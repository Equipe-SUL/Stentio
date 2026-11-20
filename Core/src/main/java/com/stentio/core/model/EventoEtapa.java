package com.stentio.core.model;

/**
 * US-012 CA.4 e US-013 CA.8: o que aconteceu com a etapa, para o histórico.
 *
 * <p>Cada valor novo aqui exige o ALTER do CHECK descrito em schema-disparo-etapas.sql,
 * porque o ddl-auto nunca altera um CHECK existente.
 */
public enum EventoEtapa {
    /** Primeira vez que a etapa recebeu um profissional. */
    ALOCACAO,
    /** Troca de profissional em uma etapa que já estava alocada. */
    REALOCACAO,
    /** A etapa foi aberta a todos os elegíveis. */
    DISPARO,
    /** Um profissional assumiu a etapa. */
    ACEITE,
    /** O profissional recusou a alocação direta. */
    RECUSA,
    /** O arquivo da etapa foi entregue. */
    ENTREGA
}
