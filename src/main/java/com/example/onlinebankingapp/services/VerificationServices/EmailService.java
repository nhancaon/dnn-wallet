package com.example.onlinebankingapp.services.VerificationServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmail(String toEmail, String subject, String emailBody) {
        // Create a simple mail message
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail); // Set the sender's email address
        message.setTo(toEmail); // Set the recipient's email address
        message.setSubject(subject); // Set the email subject

        // Set the email body
        message.setText(emailBody);
        // Send the email
        mailSender.send(message);
    }
}
