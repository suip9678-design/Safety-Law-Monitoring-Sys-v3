package com.safetylaw.monitor.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.safetylaw.monitor.domain.AppSetting;

public interface AppSettingMapper {

    List<AppSetting> findAll();

    AppSetting findByKey(@Param("settingKey") String settingKey);

    /** @return 갱신된 행 수. 0 이면 아직 없는 항목이므로 호출 측에서 insert 한다. */
    int updateValue(@Param("settingKey") String settingKey,
                    @Param("settingValue") String settingValue);

    void insertValue(@Param("settingKey") String settingKey,
                     @Param("settingValue") String settingValue);
}
