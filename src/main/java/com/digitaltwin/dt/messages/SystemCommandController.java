package com.digitaltwin.dt.messages;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemCommandController {
    private final SystemCommandService commandService;
    public SystemCommandController(SystemCommandService commandService) { this.commandService = commandService; }

    @PostMapping("/command")
    public SystemCommandService.Result command(@Valid @RequestBody CommandRequest request,
                                               Authentication authentication) {
        return commandService.execute(authentication, request.command());
    }

    public record CommandRequest(@NotBlank @Size(max = 2000) String command) { }
}
