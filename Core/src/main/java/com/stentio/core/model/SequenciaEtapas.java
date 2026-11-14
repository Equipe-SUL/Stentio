package com.stentio.core.model;

import com.stentio.core.exception.EtapasInvalidasException;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * US-011: as regras que valem para qualquer sequência de etapas, seja a de uma ordem de
 * serviço ou a de um modelo de workflow. Fica no domínio porque as duas pontas precisam
 * da mesma garantia — se morasse em um dos services, a outra cairia sem validação.
 */
public final class SequenciaEtapas {

    public static final int ORDEM_INICIAL = 1;

    private SequenciaEtapas() {
    }

    /**
     * RN.1: a numeração tem de ser 1, 2, 3… sem repetir e sem buraco. Um salto de 1 para 3
     * deixaria a etapa 2 órfã no meio da execução, e é o tipo de erro que só aparece quando
     * alguém tenta trabalhar nela.
     */
    public static void validarOrdens(List<Integer> ordens) {
        if (ordens == null || ordens.isEmpty()) {
            throw new EtapasInvalidasException("Informe ao menos uma etapa");
        }
        if (ordens.stream().anyMatch(o -> o == null)) {
            throw new EtapasInvalidasException("Toda etapa precisa de um número de ordem");
        }
        Set<Integer> distintas = new TreeSet<>(ordens);
        if (distintas.size() != ordens.size()) {
            throw new EtapasInvalidasException("Há números de ordem repetidos na sequência");
        }
        int esperado = ORDEM_INICIAL;
        for (Integer ordem : distintas) {
            if (ordem != esperado) {
                throw new EtapasInvalidasException(
                        "A sequência precisa começar em 1 e seguir sem lacunas; faltou a ordem " + esperado);
            }
            esperado++;
        }
    }

    /** RN.2: etapa sem prazo não dá para planejar produção, e prazo zero não é estimativa. */
    public static void validarPrazo(Integer prazoEstimadoHoras) {
        if (prazoEstimadoHoras == null || prazoEstimadoHoras <= 0) {
            throw new EtapasInvalidasException("O prazo estimado em horas deve ser maior que zero");
        }
    }

    /** CA.4: só a primeira da fila pode ser trabalhada; as demais esperam a anterior. */
    public static StatusEtapaOrdemServico statusInicial(int ordem) {
        return ordem == ORDEM_INICIAL
                ? StatusEtapaOrdemServico.PENDENTE
                : StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR;
    }
}
