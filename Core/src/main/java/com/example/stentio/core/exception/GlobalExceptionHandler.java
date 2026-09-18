package com.example.stentio.core.exception;

import com.example.stentio.core.exception.ErroResponse.CampoErro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String CONSTRAINT_EMAIL = "uk_recursos_email";
    private static final String CONSTRAINT_PAR_IDIOMA = "uk_recursos_precos_par_unidade";

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResponse> handleEmailJaCadastrado(EmailJaCadastradoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RecursoInvalidoException.class)
    public ResponseEntity<ErroResponse> handleRecursoInvalido(RecursoInvalidoException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex) {
        List<CampoErro> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(this::paraCampoErro)
                .toList();

        // Com um único erro, ele próprio vira a mensagem principal (ex.: Cenário 3 da US-04).
        String mensagem = erros.size() == 1
                ? erros.getFirst().mensagem()
                : "Existem " + erros.size() + " campos inválidos";

        return ResponseEntity.badRequest().body(ErroResponse.deValidacao(mensagem, erros));
    }

    // JSON malformado, UUID inválido ou unidade fora do enum. Os detalhes do parser não são expostos.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleCorpoIlegivel(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou com valores em formato incorreto");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'");
    }

    // Duas requisições simultâneas podem passar pela checagem do service; o banco barra a segunda.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleIntegridade(DataIntegrityViolationException ex) {
        String causa = String.valueOf(ex.getMostSpecificCause().getMessage());

        if (causa.contains(CONSTRAINT_EMAIL)) {
            return responder(HttpStatus.CONFLICT, EmailJaCadastradoException.MENSAGEM);
        }
        if (causa.contains(CONSTRAINT_PAR_IDIOMA)) {
            return responder(HttpStatus.CONFLICT, "Par de idiomas repetido para a mesma unidade de cobrança");
        }

        log.warn("Violação de integridade não mapeada", ex);
        return responder(HttpStatus.CONFLICT, "A operação conflita com dados já existentes");
    }

    // Ex.: DELETE /api/v1/recursos/{id}, que não existe por causa da RN.4.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " não é suportado nesta rota");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> handleRotaInexistente(NoResourceFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, "Rota não encontrada");
    }

    // Último recurso: registra o erro completo no log e devolve uma mensagem genérica, sem stack trace.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleInesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde");
    }

    private CampoErro paraCampoErro(FieldError erro) {
        String campo = erro.getField().replaceAll("\\[(\\d+)]", ".$1");
        // Falha de conversão (ex.: ?tipoServicoId=abc) traz uma mensagem técnica do Spring; não a expomos.
        String mensagem = erro.isBindingFailure() ? "Valor em formato inválido" : erro.getDefaultMessage();
        return new CampoErro(campo, mensagem);
    }

    private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(ErroResponse.de(status, mensagem));
    }
}
