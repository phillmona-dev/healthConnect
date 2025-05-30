package com.medco.HealthConnectProvider.services.impl.mail;

import com.medco.HealthConnectProvider.services.mail.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void sendWelcomeEmail(String to, String firstName, String password, String payerName, String loginUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Welcome to HealthConnect Provider System");

            // If loginUrl is not provided, use the default frontend URL
            if (loginUrl == null || loginUrl.isEmpty()) {
                loginUrl = frontendUrl + "/login";
            }

            String emailContent =
                    "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                            "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>" +
                            "<div style='text-align: center; margin-bottom: 20px;'>" +
                            "<h2 style='color: #0066cc;'>Welcome to HealthConnect Provider System</h2>" +
                            "</div>" +
                            "<p>Dear " + firstName + ",</p>" +
                            "<p>Your account has been created as an administrator for <strong>" + payerName + "</strong>.</p>" +
                            "<p>Please use the following credentials to log in:</p>" +
                            "<div style='background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;'>" +
                            "<p><strong>Email:</strong> " + to + "</p>" +
                            "<p><strong>Temporary Password:</strong> " + password + "</p>" +
                            "</div>" +
                            "<p><strong>Important:</strong> For security reasons, please change your password after your first login.</p>" +
                            "<div style='text-align: center; margin: 25px 0;'>" +
                            "<a href='" + loginUrl + "' style='background-color: #0066cc; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Login to Your Account</a>" +
                            "</div>" +
                            "<p>If the button above doesn't work, copy and paste this URL into your browser:</p>" +
                            "<p style='word-break: break-all;'><a href='" + loginUrl + "'>" + loginUrl + "</a></p>" +
                            "<hr style='border: none; border-top: 1px solid #ddd; margin: 20px 0;'>" +
                            "<p>If you have any questions, please contact our support team.</p>" +
                            "<p>Best regards,<br/>HealthConnect Provider Team</p>" +
                            "</div></body></html>";

            helper.setText(emailContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }
}
