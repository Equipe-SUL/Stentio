package com.stentio.emailservice.repository;

import com.stentio.emailservice.model.ConfiguracaoEmpresaDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConfiguracaoEmpresaRepository extends MongoRepository<ConfiguracaoEmpresaDocument, String> {
}
