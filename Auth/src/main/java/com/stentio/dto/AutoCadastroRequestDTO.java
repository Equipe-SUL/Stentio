package com.stentio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * US-023 CA.1: corpo do cadastro público do profissional.
 *
 * <p>Deliberadamente <b>não</b> é o {@code UsuarioRequestDTO}: aquele tem o campo
 * {@code role}, e o {@code criar()} respeita o que vier nele. Reaproveitá-lo em rota
 * aberta deixaria qualquer pessoa da internet criar um ADMIN com um POST. Aqui o papel
 * não é entrada: é decidido no servidor.
 *
 * <p>Telefone não entra. É atributo do perfil profissional e já existe no Recurso, do
 * lado do Core; guardar uma segunda cópia aqui criaria duas verdades para o mesmo dado.
 */
public record AutoCadastroRequestDTO(

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
        String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        // Mesma exigência do cadastro feito pela equipe: conta de acesso é conta de acesso.
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, message = "Senha deve ter no mínimo 8")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[@#$%^&+=!?*]).*$",
                message = "Senha deve conter um digito e um caractere especial (como:  #, $ ou !)"
        )
        String senha
) {
}
