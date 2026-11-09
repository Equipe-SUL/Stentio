package com.stentio.core.model;

/**
 * US-012: o que a API mostra sobre uma etapa, que não é a mesma coisa que o status gravado.
 *
 * <p>A CA.1 deixa alocar uma etapa que ainda espera a anterior, e a CA.4 manda o status
 * virar {@code ALOCADO}. Se isso fosse gravado na coluna, perderíamos a única informação
 * que diz qual etapa pode começar — e a US-014, que avança a fila automaticamente, teria
 * de reconstruí-la.
 *
 * <p>Então a coluna guarda o fluxo ({@link StatusEtapaOrdemServico}) e a alocação é o
 * {@code recurso_id} preenchido. Esta é a leitura das duas coisas juntas, que é o que o
 * contrato do card descreve.
 */
public enum SituacaoEtapa {
    PENDENTE,
    AGUARDANDO_ETAPA_ANTERIOR,
    ALOCADO;

    public static SituacaoEtapa de(EtapaOrdemServico etapa) {
        if (etapa.alocada()) {
            return ALOCADO;
        }
        return switch (etapa.getStatus()) {
            case PENDENTE -> PENDENTE;
            case AGUARDANDO_ETAPA_ANTERIOR -> AGUARDANDO_ETAPA_ANTERIOR;
        };
    }
}
