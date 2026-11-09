package com.edunex.backend.service;

import org.springframework.web.util.HtmlUtils;

final class MailTemplates {

    private MailTemplates() {
    }

    static String notification(String firstName, String title, String message, String url) {
        return """
                <div style="font-family:Segoe UI,Arial,sans-serif;background:#f1f5f9;padding:24px">
                  <div style="max-width:520px;margin:auto;background:#ffffff;border-radius:16px;padding:28px;border:1px solid #e2e8f0">
                    <p style="margin:0 0 4px;font-size:13px;color:#6366f1;font-weight:600">EduNex</p>
                    <h2 style="margin:0 0 16px;font-size:20px;color:#0f172a">%s</h2>
                    <p style="margin:0 0 12px;color:#334155">Bonjour %s,</p>
                    <p style="margin:0 0 20px;color:#334155;line-height:1.5">%s</p>
                    <a href="%s" style="display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;padding:12px 20px;border-radius:10px;font-weight:600">Ouvrir EduNex</a>
                    <p style="margin:24px 0 0;font-size:12px;color:#94a3b8">Vous recevez cet e-mail parce qu'une notification a été ajoutée à votre compte EduNex. Vous pouvez désactiver les e-mails depuis la page « Mon profil ».</p>
                  </div>
                </div>
                """.formatted(esc(title), esc(firstName), esc(message).replace("\n", "<br>"), esc(url));
    }

    static String action(String firstName, String title, String intro, String buttonLabel, String url, String footer) {
        return """
                <div style="font-family:Segoe UI,Arial,sans-serif;background:#f1f5f9;padding:24px">
                  <div style="max-width:520px;margin:auto;background:#ffffff;border-radius:16px;padding:28px;border:1px solid #e2e8f0">
                    <p style="margin:0 0 4px;font-size:13px;color:#6366f1;font-weight:600">EduNex</p>
                    <h2 style="margin:0 0 16px;font-size:20px;color:#0f172a">%s</h2>
                    <p style="margin:0 0 12px;color:#334155">Bonjour %s,</p>
                    <p style="margin:0 0 20px;color:#334155;line-height:1.5">%s</p>
                    %s
                    <p style="margin:24px 0 0;font-size:12px;color:#94a3b8">%s</p>
                  </div>
                </div>
                """.formatted(esc(title), esc(firstName), esc(intro),
                url == null ? "" : "<a href=\"" + esc(url) + "\" style=\"display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;padding:12px 20px;border-radius:10px;font-weight:600\">" + esc(buttonLabel) + "</a>",
                esc(footer));
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }
}