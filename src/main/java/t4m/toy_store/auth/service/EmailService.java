package t4m.toy_store.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Autowired
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendOtpEmail(String email, String otp, String action) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("T4M - Mã xác thực OTP của bạn");

            Context context = new Context();
            context.setVariable("otpCode", otp);
            context.setVariable("action", action);
            String htmlContent = templateEngine.process("email/otp-email", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            logger.info("OTP email sent to {}", email);
        } catch (MessagingException e) {
            logger.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    @Async
    public void sendWelcomeEmail(String email, String userName, String ctaLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Chào mừng " + userName + " đến với T4M!");

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("ctaLink", ctaLink);
            String htmlContent = templateEngine.process("email/welcome-email", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            logger.info("Welcome email sent to {}", email);
        } catch (MessagingException e) {
            logger.error("Failed to send welcome email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    @Async
    public void sendResetPasswordEmail(String email, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("T4M - Yêu cầu đặt lại mật khẩu");

            Context context = new Context();
            context.setVariable("resetLink", resetLink);
            String htmlContent = templateEngine.process("email/reset-password-email", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            logger.info("Reset password email sent to {}", email);
        } catch (MessagingException e) {
            logger.error("Failed to send reset password email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send reset password email", e);
        }
    }

    @Async
    public void sendThankYouEmail(String email, String ctaLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("T4M – Cảm ơn bạn đã quan tâm");

            Context context = new Context();
            context.setVariable("ctaLink", ctaLink);
            String htmlContent = templateEngine.process("email/thank-you-email", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            logger.info("Thank you email sent to {}", email);
        } catch (MessagingException e) {
            logger.error("Failed to send thank you email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send thank you email", e);
        }
    }
}