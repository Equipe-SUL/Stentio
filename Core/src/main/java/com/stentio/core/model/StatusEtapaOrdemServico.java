package com.stentio.core.model;

/**
 * US-011 (CA.4): a primeira etapa da sequência nasce pronta para alocação e as seguintes
 * ficam esperando a anterior. Os estados de execução entram com a US-012, junto com a
 * alocação do recurso — e vão exigir alterar o CHECK de etapas_os.status.
 */
public enum StatusEtapaOrdemServico {
    PENDENTE,
    AGUARDANDO_ETAPA_ANTERIOR
}
