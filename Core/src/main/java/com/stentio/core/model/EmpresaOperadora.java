package com.stentio.core.model;

import java.util.UUID;

/**
 * O sistema opera para uma única empresa: a configuração da empresa é um
 * singleton no módulo de e-mail (documento Mongo com id fixo "default").
 *
 * <p>A configuração não expõe um UUID, mas o cliente aponta para a empresa
 * com uma coluna UUID, então este identificador é atribuído aqui. Se um dia
 * houver multiempresa, este identificador passa a vir do contexto
 * autenticado em vez de ser fixo.
 */
public final class EmpresaOperadora {

    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private EmpresaOperadora() {
    }
}
