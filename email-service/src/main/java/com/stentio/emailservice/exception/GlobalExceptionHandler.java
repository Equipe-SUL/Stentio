package com.stentio.emailservice.exception;

import com.stentio.emailservice.exception.ErroResponse.CampoErro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Antes caía no handleInesperado e virava 500; o front esperava 404 para abrir o formulário vazio.
    @ExceptionHandler(EmpresaNaoCadastradaException.class)
    public ResponseEntity<ErroResponse> handleEmpresaNaoCadastrada(EmpresaNaoCadastradaException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // A empresa existe mas ainda não tem logo: 404 para o front cair no placeholder.
    @ExceptionHandler(LogoNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> handleLogoNaoEncontrada(LogoNaoEncontradaException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Regras de negócio e entradas inválidas (CNPJ, tamanho de logo, formato) respondem 400, não 500.
    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> handleRequisicaoInvalida(RequisicaoInvalidaException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Upload sem o campo "arquivo" ou sem arquivo selecionado.
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErroResponse> handleParteAusente(MissingServletRequestPartException ex) {
        return responder(HttpStatus.BAD_REQUEST, "O arquivo da logo é obrigatório");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex) {
        List<CampoErro> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(this::paraCampoErro)
                .toList();

        // Com um único erro, ele próprio vira a mensagem principal.
        String mensagem = erros.size() == 1
                ? erros.getFirst().mensagem()
                : "Existem " + erros.size() + " campos inválidos";

        return ResponseEntity.badRequest().body(ErroResponse.deValidacao(mensagem, erros));
    }

    // JSON malformado ou valores em formato incorreto. Os detalhes do parser não são expostos.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleCorpoIlegivel(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou com valores em formato incorreto");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'");
    }

    // Ex.: GET em rota que só aceita PUT, como era o caso de /api/v1/empresa/logo antes do GET existir.
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
        // Falha de conversão traz uma mensagem técnica do Spring; não a expomos.
        String mensagem = erro.isBindingFailure() ? "Valor em formato inválido" : erro.getDefaultMessage();
        return new CampoErro(campo, mensagem);
    }

    private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(ErroResponse.de(status, mensagem));
    }
}
