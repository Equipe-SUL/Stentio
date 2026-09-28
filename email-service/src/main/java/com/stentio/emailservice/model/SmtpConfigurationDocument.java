package com.stentio.emailservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("smtp_configuration")
public class SmtpConfigurationDocument {

    @Id
    private String id;
    private String host;
    private int port;
    private String username;
    private String password;
    private String encryption;
    private String fromEmail;
    private String fromName;

    protected SmtpConfigurationDocument() {
    }

    public SmtpConfigurationDocument(
            String id,
            String host,
            int port,
            String username,
            String password,
            String encryption,
            String fromEmail,
            String fromName
    ) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.encryption = encryption;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEncryption() { return encryption; }
    public String getFromEmail() { return fromEmail; }
    public String getFromName() { return fromName; }
}