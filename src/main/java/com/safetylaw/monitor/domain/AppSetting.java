package com.safetylaw.monitor.domain;

/**
 * 사용자가 설정 화면에서 바꾼 값(키-값). 테이블 APP_SETTINGS.
 *
 * <p>여기 저장된 값이 application.yml 의 기본값보다 우선한다.
 * KEY/VALUE 는 Oracle 에서 혼동을 부르는 이름이라 컬럼명에
 * SETTING_ 접두어를 붙였다.
 */
public class AppSetting {

    private String settingKey;
    private String settingValue;

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }
}
