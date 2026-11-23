package com.stentio.core.model;

/**
 * US-024: onde está a análise do documento de um par de idiomas.
 *
 * <p>Cada valor novo aqui exige o ALTER do CHECK numa migration — o ddl-auto está em
 * validate e nunca altera constraint existente.
 */
public enum StatusCertificacao {
    /** Documento enviado, aguardando o gestor. */
    PENDENTE,
    /** O gestor conferiu: o recurso pode receber etapas deste par. */
    APROVADA,
    /** O gestor recusou com motivo; o recurso pode reenviar. */
    REJEITADA
}
