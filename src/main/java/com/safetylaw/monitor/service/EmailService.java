package com.safetylaw.monitor.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.safetylaw.monitor.domain.LawRevision;
import com.safetylaw.monitor.dto.LawDocumentTitle;
import com.safetylaw.monitor.dto.LawRevisionRow;
import com.safetylaw.monitor.mapper.DocumentLawMappingMapper;
import com.safetylaw.monitor.mapper.LawRevisionMapper;

import jakarta.mail.internet.MimeMessage;

/**
 * 개정 감지 알림 메일.
 *
 * <p>메일 설정은 화면에서 실행 중에 바꿀 수 있으므로 발송기를 미리 만들어
 * 두지 않고 보낼 때마다 현재 설정으로 만든다.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /** 메일 서버가 응답하지 않을 때 스케줄 작업이 붙잡히지 않도록 상한을 둔다. */
    private static final String TIMEOUT_MS = "15000";

    private final SettingsService settings;
    private final LawRevisionMapper revisionMapper;
    private final DocumentLawMappingMapper mappingMapper;

    public EmailService(SettingsService settings,
                        LawRevisionMapper revisionMapper,
                        DocumentLawMappingMapper mappingMapper) {
        this.settings = settings;
        this.revisionMapper = revisionMapper;
        this.mappingMapper = mappingMapper;
    }

    /** 메일 설정이 덜 됐을 때. */
    public static class NotConfiguredException extends RuntimeException {
        public NotConfiguredException(String message) {
            super(message);
        }
    }

    private record SmtpConfig(String host, int port, boolean useTls, String user, String password,
                              String from, List<String> recipients) {

        boolean usable() {
            return host != null && !host.isBlank()
                    && from != null && !from.isBlank()
                    && !recipients.isEmpty();
        }
    }

    private SmtpConfig config() {
        Map<String, String> values = settings.getAll();
        String user = values.getOrDefault(SettingsService.SMTP_USER, "");
        String from = values.getOrDefault(SettingsService.SMTP_FROM, "");
        if (from.isBlank()) {
            from = user;
        }

        List<String> recipients = new ArrayList<>();
        for (String part : values.getOrDefault(SettingsService.ALERT_EMAILS, "").split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                recipients.add(trimmed);
            }
        }

        int port;
        try {
            String raw = values.getOrDefault(SettingsService.SMTP_PORT, "587");
            port = raw.isBlank() ? 587 : Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            port = 587;
        }

        return new SmtpConfig(
                values.getOrDefault(SettingsService.SMTP_HOST, ""),
                port,
                SettingsService.isTruthy(values.getOrDefault(SettingsService.SMTP_USE_TLS, "true")),
                user,
                values.getOrDefault(SettingsService.SMTP_PASSWORD, ""),
                from,
                recipients);
    }

    public boolean isConfigured() {
        return config().usable();
    }

    public void sendTestEmail() {
        SmtpConfig config = config();
        if (!config.usable()) {
            throw new NotConfiguredException("SMTP 서버, 발신 주소, 수신자 목록을 먼저 설정하세요.");
        }
        send(config, "[안전보건 법령 추적] 테스트 메일",
                "<p>테스트 메일입니다. 정상적으로 수신되었다면 알림 설정이 올바르게 구성된 것입니다.</p>");
    }

    /**
     * 새로 감지된 개정과 영향을 받는 사내 문서를 정리해 보낸다.
     *
     * @return 실제로 보냈으면 true
     */
    public boolean sendRevisionAlert(List<LawRevision> revisions) {
        if (revisions == null || revisions.isEmpty()) {
            return false;
        }
        SmtpConfig config = config();
        if (!config.usable()) {
            return false;
        }

        List<Long> ids = revisions.stream().map(LawRevision::getId).filter(java.util.Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return false;
        }
        List<LawRevisionRow> rows = revisionMapper.findByIds(ids);
        if (rows.isEmpty()) {
            return false;
        }

        List<Long> lawIds = rows.stream().map(LawRevisionRow::getTrackedLawId).distinct().toList();
        Map<Long, List<String>> documentsByLaw = new LinkedHashMap<>();
        for (LawDocumentTitle row : mappingMapper.findDocumentTitlesByLawIds(lawIds)) {
            documentsByLaw.computeIfAbsent(row.getTrackedLawId(), k -> new ArrayList<>())
                    .add(row.getDocumentTitle());
        }

        send(config, "[안전보건 법령 추적] 개정 " + rows.size() + "건 발생", buildHtml(rows, documentsByLaw));
        return true;
    }

    private String buildHtml(List<LawRevisionRow> rows, Map<Long, List<String>> documentsByLaw) {
        StringBuilder body = new StringBuilder();
        body.append("<h2>안전보건 법령·고시 개정 알림</h2>")
            .append("<p>새로 감지된 개정 ").append(rows.size())
            .append("건이 있습니다. 관련 회사 문서(절차서/지침서/작업표준)를 검토해 주세요.</p>")
            .append("<table style=\"border-collapse:collapse;width:100%;\"><thead><tr>");

        for (String header : List.of("법령/고시명", "구분", "공포일자", "시행일자", "영향받는 회사 문서")) {
            body.append("<th style=\"padding:8px;border:1px solid #ddd;background:#f5f5f5;\">")
                .append(escape(header)).append("</th>");
        }
        body.append("</tr></thead><tbody>");

        for (LawRevisionRow row : rows) {
            List<String> documents = documentsByLaw.getOrDefault(row.getTrackedLawId(), List.of());
            StringBuilder documentList = new StringBuilder();
            if (documents.isEmpty()) {
                documentList.append("<li>(매핑된 문서 없음)</li>");
            } else {
                for (String title : documents) {
                    documentList.append("<li>").append(escape(title)).append("</li>");
                }
            }

            body.append("<tr>")
                .append(cell(row.getTrackedLawName()))
                .append(cell(row.getTrackedLawCategory()))
                .append(cell(row.getPromulgationDate() == null ? "-" : row.getPromulgationDate()))
                .append(cell(row.getEnforcementDate() == null ? "-" : row.getEnforcementDate()))
                .append("<td style=\"padding:8px;border:1px solid #ddd;\">")
                .append("<ul style=\"margin:0;padding-left:16px;\">").append(documentList).append("</ul>")
                .append("</td>")
                .append("</tr>");
        }

        body.append("</tbody></table>");
        return body.toString();
    }

    private String cell(String value) {
        return "<td style=\"padding:8px;border:1px solid #ddd;\">" + escape(value) + "</td>";
    }

    /**
     * 메일 본문에 넣을 값은 반드시 이스케이프한다.
     *
     * <p>문서 제목은 사용자가 직접 입력하는 값이라, 그대로 넣으면 메일
     * 본문의 서식이 깨지거나 원치 않는 내용이 끼어들 수 있다.
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void send(SmtpConfig config, String subject, String html) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.host());
        sender.setPort(config.port());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        if (!config.user().isBlank()) {
            sender.setUsername(config.user());
            sender.setPassword(config.password());
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(!config.user().isBlank()));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.useTls()));
        props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
        props.put("mail.smtp.timeout", TIMEOUT_MS);
        props.put("mail.smtp.writetimeout", TIMEOUT_MS);

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(config.from());
            helper.setTo(config.recipients().toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            log.info("알림 메일을 보냈습니다. 수신자 {}명", config.recipients().size());
        } catch (Exception e) {
            throw new IllegalStateException("메일 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
