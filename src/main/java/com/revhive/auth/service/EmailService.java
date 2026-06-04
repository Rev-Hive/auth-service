package com.revhive.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    @Autowired
    private JavaMailSender javaMailSender;
    public void sendResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset Password OTP");
        message.setText("Your password reset OTP is: " + token + "\nIt is valid for 15 minutes.");

        javaMailSender.send(message);
    }

        public void sendOtp(String email, String otp) {

            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setTo(email);
            message.setSubject("RevHive Email Verification");
            message.setText("Your OTP is: " + otp);

            javaMailSender.send(message);
        }


}
