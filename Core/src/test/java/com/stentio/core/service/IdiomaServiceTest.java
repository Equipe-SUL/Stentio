package com.stentio.core.service;

import com.stentio.core.dto.IdiomaRequest;
import com.stentio.core.exception.RecursoInvalidoException;
import com.stentio.core.model.Idioma;
import com.stentio.core.repository.IdiomaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Dois idiomas com o mesmo nome apareciam iguais nos formulários ("Inglês" com en e
 * "Inglês" com en-us): o cliente escolhia um, o recurso cadastrava o outro, e o recurso
 * nunca ficava elegível para aquela solicitação.
 */
class IdiomaServiceTest {

    private final IdiomaRepository repository = mock(IdiomaRepository.class);
    private final IdiomaService service = new IdiomaService(repository);

    @Test
    @DisplayName("criar: nome já cadastrado, em qualquer caixa, é recusado")
    void criarRecusaNomeRepetido() {
        when(repository.existsByNomeIgnoreCase("inglês")).thenReturn(true);

        RecursoInvalidoException erro = assertThrows(RecursoInvalidoException.class,
                () -> service.criar(new IdiomaRequest("  inglês ", "en-us")));

        assertEquals(IdiomaService.MENSAGEM_NOME_REPETIDO, erro.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criar: código ISO já cadastrado com outra caixa é recusado")
    void criarRecusaCodigoIsoEmOutraCaixa() {
        when(repository.existsByCodigoIsoIgnoreCase("EN")).thenReturn(true);

        assertThrows(RecursoInvalidoException.class, () -> service.criar(new IdiomaRequest("English", "EN")));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criar: variante com nome próprio é aceita, sem espaços nas pontas")
    void criarAceitaVarianteComNomeDistinto() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.criar(new IdiomaRequest(" Inglês (EUA) ", " en-us "));

        ArgumentCaptor<Idioma> salvo = ArgumentCaptor.forClass(Idioma.class);
        verify(repository).save(salvo.capture());
        assertEquals("Inglês (EUA)", salvo.getValue().getNome());
        assertEquals("en-us", salvo.getValue().getCodigoIso());
    }

    @Test
    @DisplayName("editar: não pode renomear para o nome de outro idioma")
    void editarRecusaNomeDeOutroIdioma() {
        UUID id = UUID.randomUUID();
        Idioma existente = new Idioma();
        existente.setNome("Inglês (EUA)");
        existente.setCodigoIso("en-us");
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Inglês", id)).thenReturn(true);

        assertThrows(RecursoInvalidoException.class, () -> service.editar(id, new IdiomaRequest("Inglês", "en-us")));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("editar: manter o próprio nome não conta como repetido")
    void editarMantendoONome() {
        UUID id = UUID.randomUUID();
        Idioma existente = new Idioma();
        existente.setNome("Inglês");
        existente.setCodigoIso("en");
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.editar(id, new IdiomaRequest("Inglês", "en"));

        verify(repository).save(existente);
    }
}
