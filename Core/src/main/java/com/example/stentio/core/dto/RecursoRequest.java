package com.example.stentio.core.dto;

import com.example.stentio.core.model.UnidadeCobranca;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Corpo do POST e do PUT. O "ativo" fica de fora de propósito: status só muda pelo PATCH.
public record RecursoRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo 150 caracteres")
        String email,

        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{8,20}$", message = "Telefone inválido")
        String telefone,

        @NotEmpty(message = "Informe ao menos um tipo de serviço")
        @Size(max = 20, message = "Informe no máximo 20 tipos de serviço")
        List<@NotNull(message = "Tipo de serviço inválido") UUID> tiposServicoIds,

        @NotEmpty(message = "Informe ao menos um par de idioma e valor")
        @Size(max = 50, message = "Informe no máximo 50 pares de idioma")
        List<@NotNull(message = "Par de idioma inválido") @Valid PrecoRequest> precos
) {

    public record PrecoRequest(

            @NotNull(message = "Idioma de origem é obrigatório")
            UUID idiomaOrigemId,

            @NotNull(message = "Idioma de destino é obrigatório")
            UUID idiomaDestinoId,

            @NotNull(message = "Unidade de cobrança é obrigatória")
            UnidadeCobranca unidade,

            @NotNull(message = "Valor é obrigatório")
            @Positive(message = "O valor deve ser maior que zero")
            @Digits(integer = 8, fraction = 4, message = "Valor deve ter no máximo 8 dígitos inteiros e 4 decimais")
            BigDecimal valor
    ) {
    }
}
