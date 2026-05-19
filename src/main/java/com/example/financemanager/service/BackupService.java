package com.example.financemanager.service;

import com.example.financemanager.dto.BackupSnapshotDto;

public interface BackupService {
    BackupSnapshotDto exportSnapshot();
    void importSnapshot(BackupSnapshotDto snapshot);
}
