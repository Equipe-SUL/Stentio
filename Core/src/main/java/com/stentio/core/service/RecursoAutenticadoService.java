package com.stentio.core.service;

import com.stentio.core.exception.AutoCadastroInvalidoException;
import com.stentio.core.model.Recurso;
import com.stentio.core.repository.RecursoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.stereotype.Service;

/**
 * Resolve o perfil de recurso de quem está falando, a partir do token.
 *
 * <p>Isolado em um serviço só porque agora há mais de um ponto de entrada do portal — ver
 * demandas, aceitar, entregar — e todos precisam responder à mesma pergunta antes de
 * qualquer outra coisa. Duplicar isso seria duplicar a fronteira de segurança da CA.7.
 */
@Service
public class RecursoAutenticadoService {

    private final RecursoRepository recursos;

    public RecursoAutenticadoService(RecursoRepository recursos) {
        this.recursos = recursos;
    }

    /** O recurso dono da sessão. Nunca aceita id vindo da requisição. */
    public Recurso resolver(UsuarioAutenticado ator) {
        if (!ator.temUsuarioId()) {
            throw new AutoCadastroInvalidoException(
                    "Sessão sem identificação de usuário. Entre novamente para acessar o portal");
        }
        return recursos.buscarPorUsuario(ator.usuarioId())
                .orElseThrow(() -> new AutoCadastroInvalidoException(
                        "Esta conta ainda não tem perfil de recurso. Conclua o cadastro para ver suas demandas"));
    }
}
