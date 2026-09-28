package com.stentio.emailservice.repository;

import com.stentio.emailservice.model.SmtpConfigurationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SmtpConfigurationRepository extends MongoRepository<SmtpConfigurationDocument, String> {
}