package com.myweb.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class EmailUtil {

    private final JavaMailSender mailSender;
    private final Map<String, VerificationRecord> codes = new ConcurrentHashMap<>();

    public void sendVerificationCode(String toEmail) {
        String code = String.format("%06d", new Random().nextInt(1000000));
        codes.put(toEmail, new VerificationRecord(code, System.currentTimeMillis()));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("MyWeb 邮箱验证码");
            helper.setText("您的验证码是：" + code + "，5 分钟内有效。", false);
            mailSender.send(message);
        } catch (MessagingException e) {
            codes.remove(toEmail);
            throw new RuntimeException("验证码发送失败", e);
        }
    }

    public boolean verifyCode(String email, String code) {
        VerificationRecord record = codes.get(email);
        if (record == null) return false;
        if (System.currentTimeMillis() - record.timestamp > 300_000) {
            codes.remove(email);
            return false;
        }
        if (record.code.equals(code)) {
            codes.remove(email);
            return true;
        }
        return false;
    }

    private record VerificationRecord(String code, long timestamp) {}
}
