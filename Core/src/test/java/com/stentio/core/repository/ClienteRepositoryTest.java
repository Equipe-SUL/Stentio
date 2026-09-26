package com.stentio.core.repository;

import com.stentio.core.dto.ClienteFiltro;
import com.stentio.core.model.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    @DisplayName("Deve persistir, consultar, atualizar e excluir cliente no banco de dados")
    void devePersistirConsultarAtualizarEExcluirCliente() {
        UUID empresaId = UUID.randomUUID();

        // 1. Cadastrar (Persistência)
        Cliente cliente = new Cliente(
                "Empresa Teste Persistência",
                "Representante Teste",
                "rep@empresateste.com",
                "11988887777",
                empresaId,
                "52998224725"
        );

        Cliente salvo = clienteRepository.saveAndFlush(cliente);
        assertNotNull(salvo.getId());
        assertNotNull(salvo.getDataCadastro());
        assertNotNull(salvo.getDataModificacao());

        // 2. Consultar por ID
        Optional<Cliente> encontrado = clienteRepository.findById(salvo.getId());
        assertTrue(encontrado.isPresent());
        assertEquals("Empresa Teste Persistência", encontrado.get().getNomeEmpresa());
        assertEquals("52998224725", encontrado.get().getCpfCnpj());

        // 3. Consultar por CPF/CNPJ
        Optional<Cliente> porCpf = clienteRepository.findByCpfCnpj("52998224725");
        assertTrue(porCpf.isPresent());
        assertEquals(salvo.getId(), porCpf.get().getId());

        // 4. Consultar por Empresa ID
        Page<Cliente> porEmpresa = clienteRepository.findAllByEmpresaId(empresaId, PageRequest.of(0, 10));
        assertEquals(1, porEmpresa.getTotalElements());

        // 5. Consultar com Specifications / Filtros
        ClienteFiltro filtro = new ClienteFiltro("Teste", empresaId, "52998224725", null);
        Page<Cliente> filtrados = clienteRepository.findAll(ClienteSpecifications.filtrar(filtro), PageRequest.of(0, 10));
        assertEquals(1, filtrados.getTotalElements());

        // 6. Modificar
        salvo.atualizarDados(
                "Empresa Teste Nome Modificado",
                "Representante Atualizado",
                "novo_rep@empresateste.com",
                "11977776666",
                empresaId,
                "52998224725"
        );
        Cliente atualizado = clienteRepository.saveAndFlush(salvo);
        assertEquals("Empresa Teste Nome Modificado", atualizado.getNomeEmpresa());
        assertEquals("novo_rep@empresateste.com", atualizado.getEmailRepresentante());

        // 7. Excluir
        clienteRepository.delete(atualizado);
        clienteRepository.flush();

        Optional<Cliente> excluido = clienteRepository.findById(salvo.getId());
        assertTrue(excluido.isEmpty());
    }

    @Test
    @DisplayName("Deve garantir unicidade do CPF/CNPJ no banco de dados")
    void deveGarantirUnicidadeDoCpfCnpj() {
        UUID empresa1 = UUID.randomUUID();
        UUID empresa2 = UUID.randomUUID();

        Cliente cliente1 = new Cliente(
                "Empresa Alpha",
                "Rep Alpha",
                "alpha@teste.com",
                "11999990001",
                empresa1,
                "11222333000181"
        );
        clienteRepository.saveAndFlush(cliente1);

        Cliente cliente2 = new Cliente(
                "Empresa Beta",
                "Rep Beta",
                "beta@teste.com",
                "11999990002",
                empresa2,
                "11222333000181" // mesmo CNPJ
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            clienteRepository.saveAndFlush(cliente2);
        });
    }
}
