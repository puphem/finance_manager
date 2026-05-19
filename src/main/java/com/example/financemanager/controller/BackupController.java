package com.example.financemanager.controller;

import com.example.financemanager.dto.BackupSnapshotDto;
import com.example.financemanager.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/export")
    public ResponseEntity<BackupSnapshotDto> exportSnapshot() {
        return ResponseEntity.ok(backupService.exportSnapshot());
    }

    @PostMapping("/import")
    public ResponseEntity<Void> importSnapshot(@RequestBody BackupSnapshotDto snapshot) {
        backupService.importSnapshot(snapshot);
        return ResponseEntity.ok().build();
    }
}
