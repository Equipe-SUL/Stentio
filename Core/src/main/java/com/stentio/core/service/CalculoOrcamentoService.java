package com.stentio.core.service;

import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrcamentoItem;
import com.stentio.core.model.OrcamentoItemServico;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

// CA.3 e CA.4 da US-11: único ponto de cálculo do orçamento. O cálculo é sempre feito no servidor.
@Component
public class CalculoOrcamentoService {

    static final int ESCALA_MONETARIA = 2;

    // subtotal = quantidade × valor unitário, arredondado em 2 casas (HALF_UP).
    public BigDecimal subtotalServico(BigDecimal quantidade, BigDecimal valorUnitario) {
        return quantidade.multiply(valorUnitario).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    // Recalcula serviços, itens e o total, gravando os valores nas próprias entidades.
    public void recalcular(Orcamento orcamento) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrcamentoItem item : orcamento.getItens()) {
            BigDecimal subtotalItem = BigDecimal.ZERO;
            for (OrcamentoItemServico servico : item.getServicos()) {
                servico.setSubtotal(subtotalServico(servico.getQuantidade(), servico.getValorUnitario()));
                subtotalItem = subtotalItem.add(servico.getSubtotal());
            }
            item.setSubtotalItem(subtotalItem.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP));
            total = total.add(item.getSubtotalItem());
        }
        orcamento.setValorTotal(total.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP));
    }
}
