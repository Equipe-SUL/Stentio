package com.stentio.core.service;

import com.stentio.core.dto.LogAuditoriaFiltro;
import com.stentio.core.exception.AuditoriaFiltroInvalidoException;
import com.stentio.core.repository.LogAuditoriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// US-019 T6: validação dos parâmetros e ordenação da consulta (CA.2).
class LogAuditoriaServiceTest {

    private static final LogAuditoriaFiltro SEM_FILTRO = new LogAuditoriaFiltro(null, null, null, null, null, null);

    private final LogAuditoriaRepository logs = mock(LogAuditoriaRepository.class);
    private final LogAuditoriaService service = new LogAuditoriaService(logs);

    @Test
    @DisplayName("CA.2: pagina em base 1, mais recente primeiro e id como desempate")
    void ordenaDoMaisRecente() {
        when(logs.findAll(any(), any(Pageable.class))).thenReturn(Page.empty());

        service.listar(SEM_FILTRO, 3, 25);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(logs).findAll(any(), captor.capture());
        Pageable paginacao = captor.getValue();
        assertEquals(2, paginacao.getPageNumber());
        assertEquals(25, paginacao.getPageSize());
        assertEquals(Sort.by(Sort.Order.desc("dataHora"), Sort.Order.desc("id")), paginacao.getSort());
    }

    @Test
    @DisplayName("page menor que 1 é recusado")
    void paginaInvalida() {
        assertThrows(AuditoriaFiltroInvalidoException.class, () -> service.listar(SEM_FILTRO, 0, 20));
        verify(logs, never()).findAll(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("size fora de 1..100 é recusado")
    void tamanhoInvalido() {
        assertThrows(AuditoriaFiltroInvalidoException.class, () -> service.listar(SEM_FILTRO, 1, 0));
        assertThrows(AuditoriaFiltroInvalidoException.class, () -> service.listar(SEM_FILTRO, 1, 101));
    }

    @Test
    @DisplayName("data_inicio depois de data_fim é recusado")
    void intervaloInvertido() {
        LogAuditoriaFiltro filtro = new LogAuditoriaFiltro(null, null, null,
                Instant.parse("2026-11-20T00:00:00Z"), Instant.parse("2026-11-19T00:00:00Z"), null);

        assertThrows(AuditoriaFiltroInvalidoException.class, () -> service.listar(filtro, 1, 20));
    }
}
