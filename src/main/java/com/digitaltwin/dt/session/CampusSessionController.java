package com.digitaltwin.dt.session;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CampusSessionController {
    private final CampusIdentityService identityService;
    public CampusSessionController(CampusIdentityService identityService) { this.identityService = identityService; }

    @GetMapping("/api/session")
    public CampusIdentityService.Identity current(Authentication authentication) {
        return identityService.from(authentication);
    }
}
