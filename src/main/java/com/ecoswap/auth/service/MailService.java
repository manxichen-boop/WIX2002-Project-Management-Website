package com.ecoswap.auth.service;

import com.ecoswap.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text e-mail through SMTP when SPRING_MAIL_HOST is configured. Without SMTP
 * (local development) the message, including any link, is written to the log instead.
 * Delivery problems never fail the user's request.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;
    private final AppProperties props;

    public MailService(ObjectProvider<JavaMailSender> sender, AppProperties props) {
        this.sender = sender.getIfAvailable();
        this.props = props;
        if (this.sender == null) {
            log.info("SMTP is not configured (SPRING_MAIL_HOST); e-mails will be written to the log.");
        }
    }

    public boolean isSmtpConfigured() {
        return sender != null;
    }

    /** @return true if the message was handed to the mail server or logged */
    public boolean send(String to, String subject, String body) {
        if (sender == null) {
            log.info("[mail disabled] To: {} | Subject: {}\n{}", to, subject, body);
            return true;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(props.mail().from());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            return true;
        } catch (RuntimeException e) {
            log.warn("Could not send e-mail '{}' to {}: {}", subject, to, e.getMessage());
            return false;
        }
    }

    public String link(String pathAndQuery) {
        String base = props.baseUrl().endsWith("/") ? props.baseUrl().substring(0, props.baseUrl().length() - 1) : props.baseUrl();
        return base + pathAndQuery;
    }
}
