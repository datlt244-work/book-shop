package com.ecommerce.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Shared Email Service for all microservices.
 * Uses SMTP configuration from application properties.
 * 
 * Configuration required:
 * - spring.mail.host
 * - spring.mail.port
 * - spring.mail.username
 * - spring.mail.password
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.mail.username")
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    @Value("${app.name:Book Shop}")
    private String appName;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8088/api/v1}")
    private String backendUrl;

    /**
     * Send email verification link
     */
    @Async
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("📚 " + appName + " - Xác thực email của bạn");

            String verifyLink = backendUrl + "/auth/verify-email?token=" + token;
            String htmlContent = buildVerificationEmailHtml(fullName, verifyLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Send password reset email
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("📚 " + appName + " - Đặt lại mật khẩu");

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String htmlContent = buildPasswordResetEmailHtml(fullName, resetLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Send generic email with custom subject and content
     */
    @Async
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent to: {} with subject: {}", toEmail, subject);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildVerificationEmailHtml(String fullName, String verifyLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
                        .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 28px; }
                        .content { padding: 30px; color: #333; }
                        .content h2 { color: #667eea; }
                        .button { display: inline-block; padding: 15px 40px; margin: 20px 0; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white !important; text-decoration: none; border-radius: 25px; font-weight: bold; font-size: 16px; }
                        .button:hover { opacity: 0.9; }
                        .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }
                        .link-text { word-break: break-all; color: #667eea; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>📚 %s</h1>
                        </div>
                        <div class="content">
                            <h2>Xin chào %s! 👋</h2>
                            <p>Cảm ơn bạn đã đăng ký tài khoản tại <strong>%s</strong>.</p>
                            <p>Vui lòng click vào nút bên dưới để xác thực địa chỉ email của bạn:</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">✅ Xác Thực Email</a>
                            </p>
                            <p>Hoặc copy link sau vào trình duyệt:</p>
                            <p class="link-text">%s</p>
                            <p><strong>⏰ Lưu ý:</strong> Link này sẽ hết hạn sau 24 giờ.</p>
                            <p>Nếu bạn không đăng ký tài khoản này, vui lòng bỏ qua email này.</p>
                        </div>
                        <div class="footer">
                            <p>© 2024 %s. All rights reserved.</p>
                            <p>Email này được gửi tự động, vui lòng không reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(appName, fullName, appName, verifyLink, verifyLink, appName);
    }

    private String buildPasswordResetEmailHtml(String fullName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
                        .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); }
                        .header { background: linear-gradient(135deg, #ff6b6b 0%%, #ee5a24 100%%); color: white; padding: 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 28px; }
                        .content { padding: 30px; color: #333; }
                        .content h2 { color: #ee5a24; }
                        .button { display: inline-block; padding: 15px 40px; margin: 20px 0; background: linear-gradient(135deg, #ff6b6b 0%%, #ee5a24 100%%); color: white !important; text-decoration: none; border-radius: 25px; font-weight: bold; font-size: 16px; }
                        .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }
                        .link-text { word-break: break-all; color: #ee5a24; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>📚 %s</h1>
                        </div>
                        <div class="content">
                            <h2>Xin chào %s! 🔐</h2>
                            <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                            <p>Click vào nút bên dưới để đặt mật khẩu mới:</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">🔑 Đặt Lại Mật Khẩu</a>
                            </p>
                            <p>Hoặc copy link sau vào trình duyệt:</p>
                            <p class="link-text">%s</p>
                            <p><strong>⏰ Lưu ý:</strong> Link này sẽ hết hạn sau 1 giờ.</p>
                            <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>
                        </div>
                        <div class="footer">
                            <p>© 2024 %s. All rights reserved.</p>
                            <p>Email này được gửi tự động, vui lòng không reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(appName, fullName, resetLink, resetLink, appName);
    }
}
