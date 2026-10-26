package com.digitaltwin.dt.timetable;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TimetableImportService {
    private static final List<String> HEADERS = List.of(
            "roomnumber", "teachercode", "sectioncode", "subjectcode",
            "dayofweek", "weekpattern", "starttime", "endtime");
    private static final int MAX_ROWS = 5000;

    private final TimetableEntryRepository entryRepository;
    private final TimetableService timetableService;

    public TimetableImportService(TimetableEntryRepository entryRepository, TimetableService timetableService) {
        this.entryRepository = entryRepository;
        this.timetableService = timetableService;
    }

    public byte[] createTemplate() {
        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Timetable");
            var header = sheet.createRow(0);
            String[] labels = {"roomNumber", "teacherCode", "sectionCode", "subjectCode", "dayOfWeek", "weekPattern", "startTime", "endTime"};
            for (int i = 0; i < labels.length; i++) header.createCell(i).setCellValue(labels[i]);
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create timetable template", ex);
        }
    }
    @Transactional
    public ImportResult replaceBaseSchedule(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Upload a non-empty .xlsx timetable file");
        }
        List<ImportedRow> rows = parse(file);
        if (rows.isEmpty()) throw badRequest("The workbook contains no timetable entries");

        entryRepository.deleteAllInBatch();
        for (ImportedRow row : rows) {
            try {
                timetableService.create(row.request());
            } catch (ResponseStatusException ex) {
                String detail = ex.getReason() == null ? "Invalid timetable entry" : ex.getReason();
                throw new ResponseStatusException(ex.getStatusCode(), "Excel row " + row.rowNumber() + ": " + detail, ex);
            }
        }
        return new ImportResult(rows.size(), "Base timetable replaced successfully");
    }

    private List<ImportedRow> parse(MultipartFile file) {
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) throw badRequest("Workbook has no sheets");
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw badRequest("The first sheet must start with the required header row");
            List<String> actualHeaders = new ArrayList<>();
            for (int i = 0; i < HEADERS.size(); i++) {
                actualHeaders.add(normalize(cellText(header.getCell(i), formatter)));
            }
            if (!HEADERS.equals(actualHeaders)) {
                throw badRequest("Header row must be: roomNumber, teacherCode, sectionCode, subjectCode, dayOfWeek, weekPattern, startTime, endTime");
            }

            List<ImportedRow> rows = new ArrayList<>();
            for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isBlank(row, formatter)) continue;
                if (rows.size() >= MAX_ROWS) throw badRequest("Workbook cannot contain more than " + MAX_ROWS + " timetable rows");
                int rowNumber = i + 1;
                try {
                    String weekText = cellText(row.getCell(5), formatter);
                    WeekPattern weekPattern = weekText.isBlank() ? WeekPattern.EVERY_WEEK
                            : WeekPattern.valueOf(weekText.trim().toUpperCase(Locale.ROOT));
                    var request = new TimetableService.CreateTimetableEntryRequest(
                            required(row, 0, formatter, "roomNumber"),
                            required(row, 1, formatter, "teacherCode"),
                            required(row, 2, formatter, "sectionCode"),
                            required(row, 3, formatter, "subjectCode"),
                            DayOfWeek.valueOf(required(row, 4, formatter, "dayOfWeek").toUpperCase(Locale.ROOT)),
                            weekPattern,
                            parseTime(row.getCell(6), formatter),
                            parseTime(row.getCell(7), formatter));
                    rows.add(new ImportedRow(rowNumber, request));
                } catch (IllegalArgumentException ex) {
                    throw badRequest("Excel row " + rowNumber + ": invalid weekday, week pattern, or time value");
                }
            }
            return rows;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read timetable workbook", ex);
        }
    }

    private String required(Row row, int column, DataFormatter formatter, String name) {
        String value = cellText(row.getCell(column), formatter).trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Missing " + name);
        return value;
    }

    private LocalTime parseTime(Cell cell, DataFormatter formatter) {
        String value = cellText(cell, formatter).trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Missing time");
        for (DateTimeFormatter pattern : List.of(DateTimeFormatter.ofPattern("H:mm", Locale.ROOT),
                DateTimeFormatter.ofPattern("h:mm a", Locale.ROOT))) {
            try { return LocalTime.parse(value.toUpperCase(Locale.ROOT), pattern); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("Time must look like 09:00 or 2:00 PM");
    }

    private String cellText(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private boolean isBlank(Row row, DataFormatter formatter) {
        if (row == null) return true;
        for (int i = 0; i < HEADERS.size(); i++) if (!cellText(row.getCell(i), formatter).isBlank()) return false;
        return true;
    }

    private String normalize(String value) { return value.replaceAll("[\\s_]+", "").toLowerCase(Locale.ROOT); }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    private record ImportedRow(int rowNumber, TimetableService.CreateTimetableEntryRequest request) { }
    public record ImportResult(int importedEntries, String message) { }
}
