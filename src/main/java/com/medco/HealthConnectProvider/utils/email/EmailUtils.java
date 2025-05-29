package com.medco.HealthConnectProvider.utils.email;

import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.repository.user.UserRepository;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

public class EmailUtils {

    public static String setThymleafContext(String email, String token, UserRepository userRepository, TemplateEngine templateEngine) {
        Context context = new Context();
        context.setVariable("resetUrl", token);
        context.setVariable("header","Password Reset Request");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User Not Found...!"));

        context.setVariable("userName",user.getFirstName()+" "+user.getFatherName()+" "+user.getGrandFatherName());

        String content = templateEngine.process("reset-password.html", context);

        return content;
    }
}
