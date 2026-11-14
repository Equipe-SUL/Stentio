package com.stentio.core.model;

/**
 * US-012 CA.4: o que aconteceu com a etapa, para o histórico.
 *
 * <p>Começa com os dois eventos que a alocação produz. A US-013 (aceite ou recusa do
 * recurso) e a US-014 (entrega) vão acrescentar os seus — e cada valor novo aqui exige o
 * ALTER do CHECK descrito em schema-historico-etapas.sql, porque o ddl-auto nunca altera
 * um CHECK existente.
 */
public enum EventoEtapa {
    /** Primeira vez que a etapa recebeu um profissional. */
    ALOCACAO,
    /** Troca de profissional em uma etapa que já estava alocada. */
    REALOCACAO
}
