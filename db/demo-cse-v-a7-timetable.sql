-- Campus Twin demo data transcribed from the CSE V Semester, Section A7 timetable.
-- Source: W.E.F. 10/08/2026, odd semester, session 2026-27.
-- Repeatable: existing rooms/teachers/subjects/sections are preserved; exact schedule rows are not duplicated.
-- Parallel electives and lab batches use A7-OPT-* and A7-B* sections because the source
-- schedules them at the same time. Batch sizes (11 each) and room capacities are demo estimates.
-- MDM room/instructor and HONOR instructor are absent from the source; they use explicit placeholders.
-- Replaces only the exact seeded DBMS sample row in A7; unrelated schedule rows are preserved.

BEGIN;

INSERT INTO rooms (room_number, floor, capacity, room_type, equipment) VALUES
    ('DT106', 1, 44, 'classroom', 'Demo timetable; capacity is an estimate'),
    ('DT112', 1, 44, 'classroom', 'Demo timetable; capacity is an estimate'),
    ('DT203', 2, 44, 'classroom', 'Demo timetable; capacity is an estimate'),
    ('DT206', 2, 44, 'classroom', 'Demo timetable; capacity is an estimate'),
    ('DT212', 2, 44, 'classroom', 'Demo timetable; capacity is an estimate'),
    ('DT301', 3, 44, 'classroom', 'Demo timetable; capacity is an estimate'),
    ('DT310', 3, 44, 'classroom', 'Demo timetable; capacity is an estimate'),
    ('DT702', 7, 44, 'classroom', 'Demo timetable; capacity is an estimate'),
    ('DT805', 8, 44, 'lab', 'Demo timetable; capacity is an estimate'),
    ('DT811', 8, 44, 'lab', 'Demo timetable; capacity is an estimate'),
    ('DT103', 1, 44, 'classroom', 'Demo timetable; capacity is an estimate'),
    ('TBD-MDM', 0, 44, 'unassigned', 'Placeholder only; MDM room is not specified in the source')
ON CONFLICT (room_number) DO NOTHING;

INSERT INTO teachers (staff_code, full_name, email) VALUES
    ('ALS', 'Arslaan Sheikh', NULL),
    ('KRY', 'Kaushik Roy', NULL),
    ('SK', 'Samiha Khan', NULL),
    ('SG', 'Shraddha Ghodeswar', NULL),
    ('AS', 'Dr. Abhishek Sahu', NULL),
    ('AP', 'Anshul Pande', NULL),
    ('RR', 'Rasika Rewatkar', NULL),
    ('PK', 'Pravin Khawse', NULL),
    ('AK', 'Ayesha Khan', NULL),
    ('JN', 'J. Nandgaoli', NULL),
    ('AA', 'Ashwini Raju Agashe', NULL),
    ('DB', 'D. Borikar', NULL),
    ('BD', 'Prof. Barkha Dange', NULL),
    ('STM', 'Sameer Tembhurney', NULL),
    ('KAD', 'Komal Adhav', NULL),
    ('NK', 'Nikita Katariya', NULL),
    ('LR', 'Leena Rokde', NULL),
    ('UNASSIGNED', 'Instructor not specified in source timetable', NULL)
ON CONFLICT (staff_code) DO NOTHING;

INSERT INTO student_sections (code, student_count) VALUES
    ('A7', 44),
    ('A7-OPT-DPD', 44),
    ('A7-OPT-ECOM', 44),
    ('A7-OPT-IDPS', 44),
    ('A7-B1', 11),
    ('A7-B2', 11),
    ('A7-B3', 11),
    ('A7-B4', 11)
ON CONFLICT (code) DO NOTHING;

-- Local demo login identity for the student role.
INSERT INTO campus_students (student_code, full_name, section_id)
SELECT 'A7_44', 'A7 Demo Student', section.id
FROM student_sections section
WHERE section.code = 'A7'
ON CONFLICT (student_code) DO NOTHING;

INSERT INTO subjects (code, name) VALUES
    ('DPD', 'Distributed and Parallel Databases'),
    ('ECOM', 'E-Commerce and ERP'),
    ('IDPS', 'Intrusion Detection and Prevention System'),
    ('ML', 'Machine Learning'),
    ('SE', 'Software Engineering'),
    ('CC', 'Cloud Computing'),
    ('CC_LAB', 'Cloud Computing Lab'),
    ('ML_LAB', 'Machine Learning Lab'),
    ('OE', 'Open Elective'),
    ('MDM', 'Multidisciplinary Minor (room and instructor unassigned)'),
    ('SE_LAB', 'Software Engineering Lab'),
    ('DP', 'Design Patterns'),
    ('DP_LAB', 'Design Patterns Lab'),
    ('HONOR', 'Honors (instructor unassigned)')
ON CONFLICT (code) DO NOTHING;

-- Replace the exact old seeded demo DBMS class; keep every unrelated timetable row.
DELETE FROM timetable_entries te
USING rooms r, teachers t, student_sections s, subjects u
WHERE te.room_id = r.id AND te.teacher_id = t.id AND te.section_id = s.id AND te.subject_id = u.id
  AND r.room_number = 'DT301' AND t.staff_code = 'T001' AND s.code = 'A7' AND u.code = 'DBMS'
  AND te.day_of_week = 'MONDAY' AND te.week_pattern = 'EVERY_WEEK'
  AND te.start_time = TIME '09:00' AND te.end_time = TIME '10:00';

WITH schedule (room_number, teacher_code, section_code, subject_code, day_of_week, start_time, end_time) AS (
    VALUES
    ('DT106', 'ALS', 'A7-OPT-DPD', 'DPD', 'MONDAY', TIME '12:00', TIME '13:00'),
    ('DT212', 'KRY', 'A7-OPT-ECOM', 'ECOM', 'MONDAY', TIME '12:00', TIME '13:00'),
    ('DT203', 'SK', 'A7-OPT-IDPS', 'IDPS', 'MONDAY', TIME '12:00', TIME '13:00'),
    ('DT106', 'SG', 'A7', 'SE', 'MONDAY', TIME '13:00', TIME '14:00'),
    ('DT702', 'AS', 'A7', 'ML', 'MONDAY', TIME '15:00', TIME '16:00'),
    ('DT805', 'PK', 'A7-B3', 'CC_LAB', 'MONDAY', TIME '16:00', TIME '18:00'),
    ('DT805', 'AP', 'A7-B4', 'CC_LAB', 'MONDAY', TIME '16:00', TIME '18:00'),
    ('DT811', 'AK', 'A7-B1', 'ML_LAB', 'MONDAY', TIME '16:00', TIME '18:00'),
    ('DT811', 'AS', 'A7-B2', 'ML_LAB', 'MONDAY', TIME '16:00', TIME '18:00'),

    ('DT106', 'ALS', 'A7-OPT-DPD', 'DPD', 'TUESDAY', TIME '13:00', TIME '14:00'),
    ('DT112', 'KRY', 'A7-OPT-ECOM', 'ECOM', 'TUESDAY', TIME '13:00', TIME '14:00'),
    ('DT203', 'SK', 'A7-OPT-IDPS', 'IDPS', 'TUESDAY', TIME '13:00', TIME '14:00'),
    ('DT106', 'AP', 'A7', 'CC', 'TUESDAY', TIME '14:00', TIME '15:00'),
    ('DT106', 'AS', 'A7', 'ML', 'TUESDAY', TIME '15:00', TIME '16:00'),

    ('DT206', 'SG', 'A7', 'SE', 'WEDNESDAY', TIME '11:00', TIME '12:00'),
    ('DT805', 'JN', 'A7-B1', 'CC_LAB', 'WEDNESDAY', TIME '12:00', TIME '14:00'),
    ('DT805', 'AP', 'A7-B2', 'CC_LAB', 'WEDNESDAY', TIME '12:00', TIME '14:00'),
    ('DT811', 'AA', 'A7-B3', 'ML_LAB', 'WEDNESDAY', TIME '12:00', TIME '14:00'),
    ('DT811', 'AS', 'A7-B4', 'ML_LAB', 'WEDNESDAY', TIME '12:00', TIME '14:00'),
    ('DT301', 'AP', 'A7', 'CC', 'WEDNESDAY', TIME '15:00', TIME '16:00'),
    ('DT301', 'RR', 'A7', 'OE', 'WEDNESDAY', TIME '16:00', TIME '17:00'),

    ('TBD-MDM', 'UNASSIGNED', 'A7', 'MDM', 'THURSDAY', TIME '09:00', TIME '10:00'),
    ('DT206', 'AS', 'A7', 'ML', 'THURSDAY', TIME '10:00', TIME '11:00'),
    ('DT206', 'AP', 'A7', 'CC', 'THURSDAY', TIME '11:00', TIME '12:00'),
    ('DT212', 'SG', 'A7', 'SE', 'THURSDAY', TIME '13:00', TIME '14:00'),
    ('DT112', 'KRY', 'A7-OPT-ECOM', 'ECOM', 'THURSDAY', TIME '14:00', TIME '15:00'),
    ('DT310', 'SK', 'A7-OPT-IDPS', 'IDPS', 'THURSDAY', TIME '14:00', TIME '15:00'),

    ('TBD-MDM', 'UNASSIGNED', 'A7', 'MDM', 'FRIDAY', TIME '09:00', TIME '10:00'),
    ('DT805', 'DB', 'A7-B1', 'SE_LAB', 'FRIDAY', TIME '10:00', TIME '12:00'),
    ('DT805', 'SG', 'A7-B2', 'SE_LAB', 'FRIDAY', TIME '10:00', TIME '12:00'),
    ('DT811', 'BD', 'A7-B3', 'DP_LAB', 'FRIDAY', TIME '10:00', TIME '12:00'),
    ('DT811', 'STM', 'A7-B4', 'DP_LAB', 'FRIDAY', TIME '10:00', TIME '12:00'),
    ('DT212', 'BD', 'A7', 'DP', 'FRIDAY', TIME '13:00', TIME '14:00'),
    ('DT103', 'UNASSIGNED', 'A7', 'HONOR', 'FRIDAY', TIME '14:00', TIME '16:00'),

    ('DT103', 'ALS', 'A7', 'DPD', 'SATURDAY', TIME '08:00', TIME '09:00'),
    ('TBD-MDM', 'UNASSIGNED', 'A7', 'MDM', 'SATURDAY', TIME '09:00', TIME '10:00'),
    ('DT805', 'NK', 'A7-B3', 'SE_LAB', 'SATURDAY', TIME '10:00', TIME '12:00'),
    ('DT805', 'LR', 'A7-B4', 'SE_LAB', 'SATURDAY', TIME '10:00', TIME '12:00'),
    ('DT811', 'KAD', 'A7-B1', 'DP_LAB', 'SATURDAY', TIME '10:00', TIME '12:00'),
    ('DT811', 'BD', 'A7-B2', 'DP_LAB', 'SATURDAY', TIME '10:00', TIME '12:00'),
    ('DT103', 'UNASSIGNED', 'A7', 'HONOR', 'SATURDAY', TIME '13:00', TIME '15:00')
)
INSERT INTO timetable_entries (room_id, teacher_id, section_id, subject_id, day_of_week, week_pattern, start_time, end_time)
SELECT r.id, t.id, s.id, u.id, v.day_of_week, 'EVERY_WEEK', v.start_time, v.end_time
FROM schedule v
JOIN rooms r ON r.room_number = v.room_number
JOIN teachers t ON t.staff_code = v.teacher_code
JOIN student_sections s ON s.code = v.section_code
JOIN subjects u ON u.code = v.subject_code
WHERE NOT EXISTS (
    SELECT 1 FROM timetable_entries x
    WHERE x.room_id = r.id AND x.teacher_id = t.id AND x.section_id = s.id AND x.subject_id = u.id
      AND x.day_of_week = v.day_of_week AND x.week_pattern = 'EVERY_WEEK'
      AND x.start_time = v.start_time AND x.end_time = v.end_time
);

COMMIT;

-- Verify the imported schedule and its parallel cohort tracks.
SELECT s.code AS section, COUNT(*) AS class_entries
FROM timetable_entries te
JOIN student_sections s ON s.id = te.section_id
WHERE s.code = 'A7' OR s.code LIKE 'A7-%'
GROUP BY s.code
ORDER BY s.code;


