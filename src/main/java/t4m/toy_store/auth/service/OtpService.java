package t4m.toy_store.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import t4m.toy_store.auth.exception.OtpInvalidException;
import t4m.toy_store.auth.exception.OtpExpiredException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final JavaMailSender mailSender;
    private final Cache<String, OtpData> otpCache;

    @Autowired
    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.otpCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(1000).build();
    }

    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

    @Async
    public void sendOtpEmail(String email, String otp, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Your OTP for " + purpose);
            helper.setText("Your OTP is: <b>" + otp + "</b>. It expires in 5 minutes.", true);
            mailSender.send(message);
            logger.info("OTP email sent to {} for {}", email, purpose);
        } catch (MessagingException e) {
            logger.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    public void storeOtp(String email, String otp, String purpose) {
        otpCache.put(email + ":" + purpose, new OtpData(otp, System.currentTimeMillis()));
        logger.debug("Stored OTP for {}: {}", email, purpose);
    }

    public void validateOtp(String email, String otp, String purpose) {
        OtpData otpData = otpCache.getIfPresent(email + ":" + purpose);
        if (otpData == null) {
            logger.warn("OTP not found or expired for {}: {}", email, purpose);
            throw new OtpExpiredException("OTP not found or expired");
        }
        if (!otpData.otp().equals(otp)) {
            logger.warn("Invalid OTP for {}: {}", email, purpose);
            throw new OtpInvalidException("Invalid OTP");
        }
        otpCache.invalidate(email + ":" + purpose);
        logger.debug("OTP validated for {}: {}", email, purpose);
    }

    private record OtpData(String otp, long timestamp) {

    }
}