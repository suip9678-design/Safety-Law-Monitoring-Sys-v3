package com.safetylaw.monitor.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.safetylaw.monitor.dto.SettingsResponse;
import com.safetylaw.monitor.dto.SettingsUpdateRequest;
import com.safetylaw.monitor.service.EmailService;
import com.safetylaw.monitor.service.SettingsFacade;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsFacade settingsFacade;
    private final EmailService emailService;

    public SettingsController(SettingsFacade settingsFacade, EmailService emailService) {
        this.settingsFacade = settingsFacade;
        this.emailService = emailService;
    }

    @GetMapping
    public SettingsResponse get() {
        return settingsFacade.current();
    }

    @PutMapping
    public SettingsResponse update(@RequestBody SettingsUpdateRequest request) {
        return settingsFacade.update(request);
    }

    /**
     * 도움말을 이미 띄운 적 있는지.
     *
     * <p>브라우저가 아니라 서버에 기억시켜, 브라우저를 바꾸거나 캐시를 지워도
     * 설치 후 최초 한 번만 뜨게 한다.
     */
    @GetMapping("/help-shown")
    public Map<String, Boolean> helpShown() {
        return Map.of("shown", settingsFacade.helpShown());
    }

    @PostMapping("/help-shown")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markHelpShown() {
        settingsFacade.markHelpShown();
    }

    @PostMapping("/test-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void testEmail() {
        emailService.sendTestEmail();
    }
}
