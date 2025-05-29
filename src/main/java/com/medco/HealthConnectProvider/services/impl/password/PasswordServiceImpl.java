package com.medco.HealthConnectProvider.services.impl.password;

import com.medco.HealthConnectProvider.entity.token.PasswordResetToken;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.repository.token.PasswordResetTokenRepository;
import com.medco.HealthConnectProvider.repository.user.UserRepository;
import com.medco.HealthConnectProvider.services.password.PasswordService;
import com.medco.HealthConnectProvider.ui.request.auth.password.ForgotPasswordRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.ResetPasswordRequest;
import com.medco.HealthConnectProvider.utils.email.EmailUtils;
import com.medco.HealthConnectProvider.utils.password.ExpiryHandler;
import com.medco.HealthConnectProvider.utils.password.RandomNumberGenerator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Optional;

@Service
public class PasswordServiceImpl implements PasswordService {

    private UserRepository userRepository;

    private PasswordResetTokenRepository resetTokenRepository;

    private PasswordEncoder passwordEncoder;

    private TemplateEngine templateEngine;

    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.username.mail}")
    private String customSenderName;


    public PasswordServiceImpl(UserRepository userRepository, PasswordResetTokenRepository resetTokenRepository, PasswordEncoder passwordEncoder, TemplateEngine templateEngine, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.templateEngine = templateEngine;
        this.mailSender = mailSender;
    }

    @Override
    public ResponseEntity<?> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User With The Provided Email Not Found"));

        String token = RandomNumberGenerator.generateSixDigitNumber();
        var passwordToken = createPasswordResetTokenForUser(user, token);

        sendEmailToUser(request.getEmail(),token);

        return ResponseEntity.ok("an email containing a code is sent to your inbox. please get the random number and fill before it expires at: "+ passwordToken.getExpiryDate());
    }

    @Override
    public ResponseEntity<?> resetPassword(ResetPasswordRequest resetPassword) {

        User user = validatePasswordResetToken(resetPassword.getPasswordResetCode());

        if(!resetPassword.getConfirmPassword().equals(resetPassword.getNewPassword()))
            throw new BadRequestException("New Password And Confirm Password Doesn't match");

        user.setPassword(passwordEncoder.encode(resetPassword.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok("Password Reset Operation is Successful.");
    }

    public User validatePasswordResetToken(String token) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Can't find reset token"));

        if (resetToken.getExpiryDate().before(new Date())) {
            throw new BadRequestException("Please Try To Reset Your password again.");
        }
        return (userRepository.findByUserUuid(resetToken.getUserUuid())
                .orElse(null));
    }


    private void sendEmailToUser(String email, String token) {

        String content = EmailUtils.setThymleafContext(email, token, userRepository, templateEngine);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(message, true);
        } catch (MessagingException e) {
            throw new RuntimeException(e.getMessage());
        }
        try {
            helper.setSubject("Your Password Reset Request: "+ token);
        } catch (MessagingException e) {
            throw new RuntimeException(e.getMessage());
        }

        try {
            helper.setFrom(username,customSenderName);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        try {
            helper.setTo(email);
        } catch (MessagingException e) {
            throw new RuntimeException(e.getMessage());
        }
        try {
            helper.setText(content, true);
        } catch (MessagingException e) {
            throw new RuntimeException(e.getMessage());
        }

        mailSender.send(message);
    }

    private PasswordResetToken createPasswordResetTokenForUser(User user, String token) {
        var passwordToken = new PasswordResetToken();

        passwordToken.setToken(token);
        passwordToken.setUserUuid(user.getUserUuid());
        passwordToken.setExpiryDate(ExpiryHandler.calculateExpiryDate());

        return  resetTokenRepository.save(passwordToken);
    }

}
