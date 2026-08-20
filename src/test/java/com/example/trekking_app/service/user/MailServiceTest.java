package com.example.trekking_app.service.user;

import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.model.ClientType;
import com.example.trekking_app.repository.TokenRepository;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MailServiceTest {

    @InjectMocks
    private MailService mailService;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private SendGrid sendGrid;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setName("Test");
        user.setEmail("test@email.com");

        ReflectionTestUtils.setField(mailService, "devSignupConfirmationUrl", "http://dev/confirm/");
        ReflectionTestUtils.setField(mailService, "webSignupConfirmationUrl", "http://web/confirm/");
        ReflectionTestUtils.setField(mailService, "mobileSignupConfirmationUrl", "http://mobile/confirm/");
        ReflectionTestUtils.setField(mailService, "devForgotPasswordResetConfirmationUrl", "http://dev/reset/");
        ReflectionTestUtils.setField(mailService, "webForgotPasswordResetConfirmationUrl", "http://web/reset/");
        ReflectionTestUtils.setField(mailService, "mobileForgotPasswordResetConfirmationUrl", "http://mobile/reset/");
        ReflectionTestUtils.setField(mailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(mailService, "sendGrid", sendGrid);
    }

    @Test
    void sendSignupConfirmationMail_Success() throws Exception {
        when(tokenRepository.findByTokenName(anyString())).thenReturn(Optional.empty());
        when(tokenRepository.save(any(Token.class))).thenAnswer(i -> i.getArgument(0));

        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(202);
        when(sendGrid.api(any())).thenReturn(mockResponse);

        mailService.sendSignupConfirmationMail(user, ClientType.WEB);

        verify(tokenRepository).save(any(Token.class));
        verify(sendGrid).api(any());
    }

    @Test
    void sendSignupConfirmationMail_SendGridFails_ThrowsMailSendException() throws Exception {
        when(tokenRepository.findByTokenName(anyString())).thenReturn(Optional.empty());
        when(tokenRepository.save(any(Token.class))).thenAnswer(i -> i.getArgument(0));

        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(400);
        when(sendGrid.api(any())).thenReturn(mockResponse);

        assertThrows(org.springframework.mail.MailSendException.class, () -> mailService.sendSignupConfirmationMail(user, ClientType.WEB));
    }

    @Test
    void sendSignupConfirmationMail_SendGridThrowsException_ThrowsMailSendException() throws Exception {
        when(tokenRepository.findByTokenName(anyString())).thenReturn(Optional.empty());
        when(tokenRepository.save(any(Token.class))).thenAnswer(i -> i.getArgument(0));
        when(sendGrid.api(any())).thenThrow(new IOException("network error"));

        assertThrows(org.springframework.mail.MailSendException.class, () -> mailService.sendSignupConfirmationMail(user, ClientType.WEB));
    }

    @Test
    void sendForgotPasswordResetMail_Success() throws Exception {
        when(tokenRepository.findByTokenName(anyString())).thenReturn(Optional.empty());
        when(tokenRepository.save(any(Token.class))).thenAnswer(i -> i.getArgument(0));

        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(202);
        when(sendGrid.api(any())).thenReturn(mockResponse);

        mailService.sendForgotPasswordResetMail(user, ClientType.WEB);

        verify(tokenRepository).save(any(Token.class));
        verify(sendGrid).api(any());
    }

    @Test
    void sendForgotPasswordResetMail_SendGridFails_ThrowsMailSendException() throws Exception {
        when(tokenRepository.findByTokenName(anyString())).thenReturn(Optional.empty());
        when(tokenRepository.save(any(Token.class))).thenAnswer(i -> i.getArgument(0));

        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(500);
        when(sendGrid.api(any())).thenReturn(mockResponse);

        assertThrows(org.springframework.mail.MailSendException.class, () -> mailService.sendForgotPasswordResetMail(user, ClientType.WEB));
    }

    @Test
    void getSignupVerificationUrl_Web_Success() {
        String url = mailService.getSignupVerificationUrl(ClientType.WEB);
        assertEquals("http://web/confirm/", url);
    }

    @Test
    void getSignupVerificationUrl_Mobile_Success() {
        String url = mailService.getSignupVerificationUrl(ClientType.MOBILE);
        assertEquals("http://mobile/confirm/", url);
    }

    @Test
    void getSignupVerificationUrl_Dev_Success() {
        String url = mailService.getSignupVerificationUrl(ClientType.DEV);
        assertEquals("http://dev/confirm/", url);
    }

    @Test
    void getSignupVerificationUrl_Undefined_FallsBackToDev() {
        ReflectionTestUtils.setField(mailService, "webSignupConfirmationUrl", "undefined");
        String url = mailService.getSignupVerificationUrl(ClientType.WEB);
        assertEquals("http://dev/confirm/", url);
    }

    @Test
    void getResetForgotPasswordUrl_Web_Success() {
        String url = mailService.getResetForgotPasswordUrl(ClientType.WEB);
        assertEquals("http://web/reset/", url);
    }

    @Test
    void getResetForgotPasswordUrl_Mobile_Success() {
        String url = mailService.getResetForgotPasswordUrl(ClientType.MOBILE);
        assertEquals("http://mobile/reset/", url);
    }

    @Test
    void getResetForgotPasswordUrl_Dev_Success() {
        String url = mailService.getResetForgotPasswordUrl(ClientType.DEV);
        assertEquals("http://dev/reset/", url);
    }

    @Test
    void getResetForgotPasswordUrl_Undefined_FallsBackToDev() {
        ReflectionTestUtils.setField(mailService, "webForgotPasswordResetConfirmationUrl", "undefined");
        String url = mailService.getResetForgotPasswordUrl(ClientType.WEB);
        assertEquals("http://dev/reset/", url);
    }

    @Test
    void generateToken_Success() {
        when(tokenRepository.findByTokenName(anyString())).thenReturn(Optional.empty());
        String token = mailService.generateToken();
        assertNotNull(token);
        assertFalse(token.isBlank());
    }


    @Test
    void buildSignupConfirmationEmail_ContainsDynamicValues() {
        String result = mailService.buildSignupConfirmationEmail(
                "Test", "http://confirm/test");
        assertTrue(result.contains("Test"));
        assertTrue(result.contains("http://confirm/test"));
        assertTrue(result.contains("Confirm Email Address"));
    }

    @Test
    void buildResetForgotPasswordConfirmationEmail_ContainsDynamicValues() {
        String result = mailService.buildResetForgotPasswordConfirmationEmail(
                "Test", "http://reset/test");
        assertTrue(result.contains("Test"));
        assertTrue(result.contains("http://reset/test"));
        assertTrue(result.contains("Reset My Password"));
    }
}