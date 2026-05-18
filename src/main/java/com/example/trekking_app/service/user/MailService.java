package com.example.trekking_app.service;

import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.mapper.TokenMapper;
import com.example.trekking_app.repository.TokenRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

private final JavaMailSender mailSender;
private final TokenRepository tokenRepo;
private final TokenMapper tokenMapper = new TokenMapper();

@Value("${frontend.signup.confirmation.url}")
private String signupConfirmationUrl;

@Value("${frontend.reset.forgot-password.confirmation.url}")
private String forgotPasswordResetConfirmationUrl;


@Async("mailTaskExecutor")
public void sendSignupConfirmationMail(@NonNull User user) {
    try
{
    String tokenName = generateToken();
    Token token =tokenMapper.toEntity(tokenName,user, LocalDateTime.now().plusDays(1));
    Token savedToken = tokenRepo.save(token);

    String subject = "Signup Confirmation Mail";
    String confirmationLink = getSignupVerificationUrl()+savedToken.getTokenName();
    String htmlContent = buildSignupConfirmationEmail(user.getName(),confirmationLink);

    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    helper.addTo(user.getEmail());
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
@Async("mailTaskExecutor")
public void sendForgotPasswordResetMail(@NonNull User user)
{
    try {
        String tokenName = generateToken();
        Token token = tokenMapper.toEntity(tokenName, user, LocalDateTime.now().plusMinutes(5));
        Token savedToken = tokenRepo.save(token);

        String subject = "Password Reset Mail";
        String confirmationLink = getResetForgotPasswordUrl() + savedToken.getTokenName();
        String htmlContent = buildResetForgotPasswordConfirmationEmail(user.getName(), confirmationLink);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.addTo(user.getEmail());
        helper.setFrom("info@trekking-app.com");
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
    catch (Exception e)
    {
        log.error(e.getLocalizedMessage(),e.getMessage());
        throw new MailSendException("Failed to send password reset confirmation mail");
    }

}
    public String buildSignupConfirmationEmail(String username, String confirmationLink) {
        return String.format("""
        <p>Did you just signup to Trekking-App %s ?,</p>
        <p>Click below to confirm your email:</p>
        <p><a href="%s" style="color: white; background: #28a745; padding: 10px 15px; text-decoration: none; border-radius: 5px;">Confirm Email</a></p>
        <p>If you didn't request this, ignore this email.</p>
    """, username, confirmationLink);
    }

    public String buildResetForgotPasswordConfirmationEmail(String username , String resetLink)
    {
        return String.format("""
        <div style="font-family: Arial, sans-serif; line-height: 1.6;">
            <h2>Password Reset Request</h2>

            <p>Hi %s,</p>

            <p>We received a request to reset your password for your Trekking-App account.</p>

            <p>
                Click the button below to reset your password:
            </p>

            <p>
                <a href="%s"
                   style="display:inline-block;
                          color:white;
                          background:#dc3545;
                          padding:10px 15px;
                          text-decoration:none;
                          border-radius:5px;">
                    Reset Password
                </a>
            </p>

            <p><b>This link will expire in 5 minutes</b> for your security.</p>

            <p>If you did not request this, you can safely ignore this email. Your account remains secure.</p>

            <hr/>

            <small style="color: gray;">
                Trekking-App Security Team
            </small>
        </div>
    """, username, resetLink);
    }



    //Method to generate confirmation URL excluding token
    public String getSignupVerificationUrl()
    {
        return  signupConfirmationUrl;
    }

    public String getResetForgotPasswordUrl()
    {
        return forgotPasswordResetConfirmationUrl;
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
