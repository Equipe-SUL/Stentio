package com.stentio.core.exception;

import com.stentio.core.controller.OrcamentoPublicoController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = OrcamentoPublicoController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrcamentoPublicoExceptionHandler {
    @ExceptionHandler(LinkOrcamentoInvalidoException.class)
    public ResponseEntity<ErroResponse> linkInvalido() {
        return erro(LinkOrcamentoInvalidoException.MENSAGEM);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
        if (ex.getBindingResult().hasFieldErrors("token")) {
            return erro(LinkOrcamentoInvalidoException.MENSAGEM);
        }
        // Não inclui token nem valor recebido em respostas públicas de erro.
        return erro("Motivo da rejeição é obrigatório e deve ter no máximo 2000 caracteres");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido() {
        return erro("Corpo da requisição inválido");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> entradaInvalida(IllegalArgumentException ex) {
        return erro(ex.getMessage());
    }

    private ResponseEntity<ErroResponse> erro(String mensagem) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .body(ErroResponse.de(HttpStatus.BAD_REQUEST, mensagem));
    }
}
