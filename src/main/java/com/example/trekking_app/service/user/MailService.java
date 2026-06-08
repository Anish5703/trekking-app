package com.example.trekking_app.service.user;

import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.mapper.TokenMapper;
import com.example.trekking_app.model.ClientType;
import com.example.trekking_app.repository.TokenRepository;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class MailService {
//private final JavaMailSender mailSender;
private final SendGrid sendGrid;
private final TokenRepository tokenRepo;
private final TokenMapper tokenMapper = new TokenMapper();

@Value("${dev.signup.confirmation.url}")
private String devSignupConfirmationUrl;

@Value("${dev.reset.forgot-password.confirmation.url}")
private String devForgotPasswordResetConfirmationUrl;

    @Value("${mobile.signup.confirmation.url}")
    private String mobileSignupConfirmationUrl;

    @Value("${mobile.reset.forgot-password.confirmation.url}")
    private String mobileForgotPasswordResetConfirmationUrl;

    @Value("${web.signup.confirmation.url}")
    private String webSignupConfirmationUrl;

    @Value("${web.reset.forgot-password.confirmation.url}")
    private String webForgotPasswordResetConfirmationUrl;


    @Value("${app.mail.from}")
private String fromEmail;

public MailService(TokenRepository tokenRepo,@Value("${sendgrid.api.key}") String apiKey)
{
    this.tokenRepo = tokenRepo;
    this.sendGrid = new SendGrid(apiKey);
}

@Async("mailTaskExecutor")
public void sendSignupConfirmationMail(@NonNull User user, ClientType clientType) {
    try
{
    String tokenName = generateToken();
    Token token =tokenMapper.toEntity(tokenName,user, LocalDateTime.now().plusDays(1));
    Token savedToken = tokenRepo.save(token);

    String subject = "Signup Confirmation Mail";
    String confirmationLink = getSignupVerificationUrl(clientType)+savedToken.getTokenName();
    String htmlContent = buildSignupConfirmationEmail(user.getName(),confirmationLink);

    /*
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    helper.addTo(user.getEmail());
    helper.setFrom(fromEmail);
    helper.setSubject(subject);
    helper.setText(htmlContent,true);
    mailSender.send(message);
     */

    sendMail(user.getEmail(), subject,htmlContent);

    log.info("Signup confirmation mail send to {}",user.getEmail());
}
    catch(Exception ex)
    {
        log.error("Failed to send Mail ; {} -> {}", ex.getClass(),ex.getLocalizedMessage());
        throw new MailSendException("Failed to send signup confirmation mail");
    }
}
@Async("mailTaskExecutor")
public void sendForgotPasswordResetMail(@NonNull User user,@NonNull ClientType clientType)
{
    try {
        String tokenName = generateToken();
        Token token = tokenMapper.toEntity(tokenName, user, LocalDateTime.now().plusMinutes(5));
        Token savedToken = tokenRepo.save(token);

        String subject = "Password Reset Mail";
        String confirmationLink = getResetForgotPasswordUrl(clientType) + savedToken.getTokenName();
        String htmlContent = buildResetForgotPasswordConfirmationEmail(user.getName(), confirmationLink);
        /*
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.addTo(user.getEmail());
        helper.setFrom(fromEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);

         */
        sendMail(user.getEmail(), subject,htmlContent);
        log.info("Password reset mail send to {}",user.getEmail());
    }
    catch (Exception ex)
    {
        log.error("Failed to send Mail ; {} -> {}", ex.getClass(),ex.getLocalizedMessage());
        throw new MailSendException("Failed to send password reset confirmation mail");
    }

}
    public String buildSignupConfirmationEmail(String username, String confirmationLink) {
        return String.format("""
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Confirm your email</title>
        </head>
        <body style="margin:0; padding:0; background-color:#f4f4f4; font-family: Arial, sans-serif;">
            <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4; padding: 30px 0;">
                <tr>
                    <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden;">
    
                            <!-- Header -->
                            <tr>
                                <td style="background-color:#28a745; padding: 24px 40px;">
                                    <h1 style="margin:0; color:#ffffff; font-size:22px;">🏔️ Trekking App</h1>
                                </td>
                            </tr>

                            <!-- Body -->
                            <tr>
                                <td style="padding: 40px;">
                                    <h2 style="margin:0 0 16px; color:#333333;">Welcome, %s!</h2>
                                    <p style="margin:0 0 12px; color:#555555; font-size:15px; line-height:1.6;">
                                        Thank you for creating an account with Trekking App.
                                        Please confirm your email address to activate your account.
                                    </p>
                                    <p style="margin:0 0 24px; color:#555555; font-size:15px; line-height:1.6;">
                                        This confirmation link will expire in <strong>24 hours</strong>.
                                    </p>

                                    <!-- CTA Button -->
                                    <table cellpadding="0" cellspacing="0">
                                        <tr>
                                            <td style="background-color:#28a745; border-radius:5px;">
                                                <a href="%s"
                                                   style="display:inline-block; padding:12px 24px; color:#ffffff;
                                                          text-decoration:none; font-size:15px; font-weight:bold;">
                                                    Confirm Email Address
                                                </a>
                                            </td>
                                        </tr>
                                    </table>

                                    <p style="margin:24px 0 0; color:#888888; font-size:13px; line-height:1.6;">
                                        If the button doesn't work, copy and paste this link into your browser:<br/>
                                        <a href="%s" style="color:#28a745; word-break:break-all;">%s</a>
                                    </p>

                                    <p style="margin:24px 0 0; color:#888888; font-size:13px;">
                                        If you did not create an account with Trekking App, you can safely ignore this email.
                                        No action is required.
                                    </p>
                                </td>
                            </tr>

                            <!-- Footer -->
                            <tr>
                                <td style="background-color:#f4f4f4; padding: 24px 40px; border-top: 1px solid #eeeeee;">
                                    <p style="margin:0; color:#aaaaaa; font-size:12px; line-height:1.6;">
                                        © 2025 Trekking App. All rights reserved.<br/>
                                        123 Trail Street, Kathmandu, Nepal<br/>
                                        You are receiving this email because you registered at
                                        <a href="https://trekking-app-production.up.railway.app"
                                           style="color:#aaaaaa;">trekking-app-production.up.railway.app</a>
                                    </p>
                                </td>
                            </tr>

                        </table>
                    </td>
                </tr>
            </table>
        </body>
        </html>
    """, username, confirmationLink, confirmationLink, confirmationLink);
    }

    public String buildResetForgotPasswordConfirmationEmail(String username, String resetLink) {
        return String.format("""
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Password Reset Request</title>
        </head>
        <body style="margin:0; padding:0; background-color:#f4f4f4; font-family: Arial, sans-serif;">
            <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4; padding: 30px 0;">
                <tr>
                    <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden;">

                            <!-- Header -->
                            <tr>
                                <td style="background-color:#dc3545; padding: 24px 40px;">
                                    <h1 style="margin:0; color:#ffffff; font-size:22px;">🏔️ Trekking App</h1>
                                </td>
                            </tr>

                            <!-- Body -->
                            <tr>
                                <td style="padding: 40px;">
                                    <h2 style="margin:0 0 16px; color:#333333;">Password Reset Request</h2>
                                    <p style="margin:0 0 12px; color:#555555; font-size:15px; line-height:1.6;">
                                        Hi <strong>%s</strong>,
                                    </p>
                                    <p style="margin:0 0 12px; color:#555555; font-size:15px; line-height:1.6;">
                                        We received a request to reset the password for your Trekking App account.
                                        Click the button below to proceed.
                                    </p>

                                    <!-- Expiry Warning -->
                                    <table cellpadding="0" cellspacing="0" style="margin: 0 0 24px;">
                                        <tr>
                                            <td style="background-color:#fff3cd; border-left: 4px solid #ffc107;
                                                        padding: 12px 16px; border-radius: 4px;">
                                                <p style="margin:0; color:#856404; font-size:13px;">
                                                    ⚠️ This link will expire in <strong>5 minutes</strong> for your security.
                                                </p>
                                            </td>
                                        </tr>
                                    </table>

                                    <!-- CTA Button -->
                                    <table cellpadding="0" cellspacing="0">
                                        <tr>
                                            <td style="background-color:#dc3545; border-radius:5px;">
                                                <a href="%s"
                                                   style="display:inline-block; padding:12px 24px; color:#ffffff;
                                                          text-decoration:none; font-size:15px; font-weight:bold;">
                                                    Reset My Password
                                                </a>
                                            </td>
                                        </tr>
                                    </table>

                                    <p style="margin:24px 0 0; color:#888888; font-size:13px; line-height:1.6;">
                                        If the button doesn't work, copy and paste this link into your browser:<br/>
                                        <a href="%s" style="color:#dc3545; word-break:break-all;">%s</a>
                                    </p>

                                    <p style="margin:24px 0 0; color:#888888; font-size:13px; line-height:1.6;">
                                        If you did not request a password reset, please ignore this email.
                                        Your account remains secure and no changes have been made.
                                    </p>
                                </td>
                            </tr>

                            <!-- Footer -->
                            <tr>
                                <td style="background-color:#f4f4f4; padding: 24px 40px; border-top: 1px solid #eeeeee;">
                                    <p style="margin:0; color:#aaaaaa; font-size:12px; line-height:1.6;">
                                        © 2025 Trekking App — Security Team<br/>
                                        123 Trail Street, Kathmandu, Nepal<br/>
                                        You are receiving this email because a password reset was requested at
                                        <a href="https://trekking-app-production.up.railway.app"
                                           style="color:#aaaaaa;">trekking-app-production.up.railway.app</a>
                                    </p>
                                </td>
                            </tr>

                        </table>
                    </td>
                </tr>
            </table>
        </body>
        </html>
    """, username, resetLink, resetLink, resetLink);
    }

    private void sendMail(String toEmail, String subject, String htmlContent) throws IOException {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);

        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);
        mail.setReplyTo(new Email(fromEmail));

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sendGrid.api(request);

        if (response.getStatusCode() >= 400) {
            log.error("SendGrid error: {} - {}", response.getStatusCode(), response.getBody());
            throw new IOException("SendGrid rejected the request: " + response.getStatusCode());
        }
    }


    //Method to generate confirmation URL excluding token
    public String getSignupVerificationUrl(ClientType clientType)
    {
        String redirectUrl;
        switch (clientType)
        {
            case MOBILE -> redirectUrl = mobileSignupConfirmationUrl;
            case WEB -> redirectUrl = webSignupConfirmationUrl;
            case DEV -> redirectUrl = devSignupConfirmationUrl;
            default -> redirectUrl = devSignupConfirmationUrl;
        }

        if(redirectUrl.equals("undefined"))
            redirectUrl = devSignupConfirmationUrl;

        return redirectUrl;
    }

    public String getResetForgotPasswordUrl(ClientType clientType)
    {
        String redirectUrl;
        switch (clientType)
        {
            case MOBILE -> redirectUrl = mobileForgotPasswordResetConfirmationUrl;
            case WEB -> redirectUrl = webForgotPasswordResetConfirmationUrl;
            case DEV -> redirectUrl = devForgotPasswordResetConfirmationUrl;

            default -> redirectUrl = devForgotPasswordResetConfirmationUrl;
        }
        if(redirectUrl.equals("undefined")) redirectUrl = devForgotPasswordResetConfirmationUrl;

        return redirectUrl;
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
