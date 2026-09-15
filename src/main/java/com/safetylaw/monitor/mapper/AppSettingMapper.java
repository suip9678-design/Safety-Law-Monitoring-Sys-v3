package com.safetylaw.monitor.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.safetylaw.monitor.domain.AppSetting;

public interface AppSettingMapper {

    List<AppSetting> findAll();

    AppSetting findByKey(@Param("settingKey") String settingKey);

    /** 있으면 갱신, 없으면 추가한다. */
    void upsert(@Param("settingKey") String settingKey,
                @Param("settingValue") String settingValue);
}
