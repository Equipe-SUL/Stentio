package com.stentio.core.model;

/**
 * O ciclo de vida de uma etapa.
 *
 * <p>Convidar tem duas formas, e por isso dois estados: {@link #ALOCADO} é o convite
 * nominal, feito pelo gestor a uma pessoa (US-012); {@link #DISPARADA} é o convite aberto
 * a todos os elegíveis (US-013). Nos dois casos ninguém assumiu ainda — quem assume é
 * quem aceita.
 *
 * <p>US-013: acrescentar valor aqui exige recriar o CHECK de {@code etapas_os.status};
 * o bloco está em {@code schema-disparo-etapas.sql}.
 */
public enum StatusEtapaOrdemServico {

    /** US-011 CA.4: pronta para receber gente, sem ninguém convidado. */
    PENDENTE,

    /** US-011 CA.4: depende da etapa anterior terminar. */
    AGUARDANDO_ETAPA_ANTERIOR,

    /** US-012: o gestor escolheu um profissional e está esperando o aceite dele. */
    ALOCADO,

    /** US-013 CA.1: convite aberto; o primeiro elegível que aceitar leva. */
    DISPARADA,

    /** US-013 CA.3: alguém assumiu. A partir daqui a etapa está em andamento. */
    ACEITA,

    /** US-013 CA.5: o arquivo foi enviado. */
    ENTREGUE
}
