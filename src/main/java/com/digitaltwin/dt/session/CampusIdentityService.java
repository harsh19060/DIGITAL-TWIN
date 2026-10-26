package com.digitaltwin.dt.session;

import com.digitaltwin.dt.messages.ParticipantType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CampusIdentityService {

    public Identity from(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(value -> value.startsWith("ROLE_"))
                .map(value -> value.substring(5))
                .findFirst().orElse("STUDENT");
        return switch (role) {
            case "ADMIN" -> new Identity(role, "Campus admin", null, null);
            case "TEACHER" -> {
                String code = System.getenv().getOrDefault("APP_TEACHER_CODE", "ALS");
                yield new Identity(role, code, ParticipantType.TEACHER, code);
            }
            default -> {
                String code = System.getenv().getOrDefault("APP_STUDENT_CODE", "A7_44");
                yield new Identity("STUDENT", code, ParticipantType.STUDENT, code);
            }
        };
    }

    public Identity requireParticipant(Authentication authentication) {
        Identity identity = from(authentication);
        if (identity.participantType() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts can only manage timetable imports");
        }
        return identity;
    }

    public record Identity(String role, String displayName, ParticipantType participantType, String participantCode) { }
}
