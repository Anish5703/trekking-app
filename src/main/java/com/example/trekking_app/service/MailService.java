package com.example.trekking_app.service;

import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.repository.TokenRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
@Slf4j
@Service
public class MailService {

private final JavaMailSender mailSender;
private final TokenRepository tokenRepo;

@Value("${signup-confirmation-url}")
private String signupConfirmationUrl;

public MailService(JavaMailSender mailSender,TokenRepository tokenRepo)
{
    this.mailSender = mailSender;
    this.tokenRepo = tokenRepo;

}
@Async
public void sendHtmlMail(String to,String subject,String htmlContent) throws MessagingException
{
    try
{
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    helper.addTo(to);
    helper.setFrom("info@trekking-app.com");
    helper.setSubject(subject);
    helper.setText(htmlContent,true);
    mailSender.send(message);
}
    catch(Exception ex)
    {
        throw new MailSendException("Failed to send signup confirmation mail");
    }
}
    public String buildConfirmationEmail(String username, String confirmationLink) {
        return String.format("""
        <p>Did you just signup to Trekking-App %s ?,</p>
        <p>Click below to confirm your email:</p>
        <p><a href="%s" style="color: white; background: #28a745; padding: 10px 15px; text-decoration: none; border-radius: 5px;">Confirm Email</a></p>
        <p>If you didn't request this, ignore this email.</p>
    """, username, confirmationLink);
    }




    //Method to generate confirmation URL excluding token
    public String getConfirmationUrl(HttpServletRequest request)
    {
        return  signupConfirmationUrl;
    }

    //Method to generate random token
    public String generateToken()
    {
        String token = UUID.randomUUID().toString();
        while(tokenRepo.findByTokenName(token).isPresent())
        {
            token = UUID.randomUUID().toString();
        }
        return token;
    }
}
