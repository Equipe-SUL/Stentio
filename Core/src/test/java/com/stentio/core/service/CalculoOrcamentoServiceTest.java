package com.stentio.core.service;

import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrcamentoItem;
import com.stentio.core.model.OrcamentoItemServico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculoOrcamentoServiceTest {

    private final CalculoOrcamentoService calculo = new CalculoOrcamentoService();

    @Test
    @DisplayName("CA.3: subtotal do serviço = quantidade × valor unitário")
    void calculaSubtotalDoServico() {
        assertEquals(new BigDecimal("375.00"), calculo.subtotalServico(new BigDecimal("1500"), new BigDecimal("0.25")));
        assertEquals(new BigDecimal("1.50"), calculo.subtotalServico(new BigDecimal("15"), new BigDecimal("0.10")));
    }

    @Test
    @DisplayName("Arredonda o subtotal em 2 casas com HALF_UP")
    void arredondaHalfUp() {
        // 3 × 0,3350 = 1,005 → 1,01 (HALF_UP); HALF_EVEN daria 1,00
        assertEquals(new BigDecimal("1.01"), calculo.subtotalServico(new BigDecimal("3"), new BigDecimal("0.3350")));
        // 1 × 0,0049 = 0,0049 → 0,00
        assertEquals(new BigDecimal("0.00"), calculo.subtotalServico(BigDecimal.ONE, new BigDecimal("0.0049")));
    }

    @Test
    @DisplayName("CA.4: subtotal de cada item e valor total do orçamento (exemplo da US)")
    void calculaSubtotalItemEValorTotal() {
        Orcamento orcamento = new Orcamento();
        orcamento.adicionarItem(item(1, servico("1500", "0.25"), servico("1500", "0.10")));
        orcamento.adicionarItem(item(2, servico("1500", "0.30")));

        calculo.recalcular(orcamento);

        OrcamentoItem primeiro = orcamento.getItens().get(0);
        assertEquals(new BigDecimal("375.00"), primeiro.getServicos().get(0).getSubtotal());
        assertEquals(new BigDecimal("150.00"), primeiro.getServicos().get(1).getSubtotal());
        assertEquals(new BigDecimal("525.00"), primeiro.getSubtotalItem());
        assertEquals(new BigDecimal("450.00"), orcamento.getItens().get(1).getSubtotalItem());
        assertEquals(new BigDecimal("975.00"), orcamento.getValorTotal());
    }

    @Test
    @DisplayName("Ignora subtotais preenchidos previamente e recalcula do zero")
    void ignoraSubtotalInformado() {
        OrcamentoItemServico servico = servico("10", "2.00");
        servico.setSubtotal(new BigDecimal("999.99"));
        Orcamento orcamento = new Orcamento();
        orcamento.adicionarItem(item(1, servico));

        calculo.recalcular(orcamento);

        assertEquals(new BigDecimal("20.00"), servico.getSubtotal());
        assertEquals(new BigDecimal("20.00"), orcamento.getValorTotal());
    }

    @Test
    @DisplayName("Orçamento sem itens fica com total zero")
    void orcamentoSemItens() {
        Orcamento orcamento = new Orcamento();
        calculo.recalcular(orcamento);
        assertEquals(new BigDecimal("0.00"), orcamento.getValorTotal());
    }

    private static OrcamentoItem item(int ordem, OrcamentoItemServico... servicos) {
        OrcamentoItem item = new OrcamentoItem();
        item.setOrdem(ordem);
        for (OrcamentoItemServico servico : servicos) {
            item.adicionarServico(servico);
        }
        return item;
    }

    private static OrcamentoItemServico servico(String quantidade, String valorUnitario) {
        OrcamentoItemServico servico = new OrcamentoItemServico();
        servico.setQuantidade(new BigDecimal(quantidade));
        servico.setValorUnitario(new BigDecimal(valorUnitario));
        return servico;
    }
}
