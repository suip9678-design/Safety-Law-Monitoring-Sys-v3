package com.safetylaw.monitor.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safetylaw.monitor.dto.SyncResultResponse;
import com.safetylaw.monitor.service.SyncFacade;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncFacade syncFacade;

    public SyncController(SyncFacade syncFacade) {
        this.syncFacade = syncFacade;
    }

    @PostMapping
    public SyncResultResponse run() {
        return syncFacade.run();
    }
}
