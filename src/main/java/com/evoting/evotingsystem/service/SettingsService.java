package com.evoting.evotingsystem.service;

import com.evoting.evotingsystem.entity.SystemSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final AdminPanelService adminPanelService;

    @Transactional(readOnly = true)
    public List<SystemSetting> getSettings() {
        return adminPanelService.getSettings();
    }

    @Transactional
    public void saveSetting(String key, String value) {
        adminPanelService.saveSetting(key, value);
    }
}
