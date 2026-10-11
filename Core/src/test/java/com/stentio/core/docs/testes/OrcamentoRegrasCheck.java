import com.stentio.core.exception.LinkOrcamentoInvalidoException;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.service.TokenOrcamento;
import com.stentio.core.service.ValidadeLinkOrcamento;
import java.time.Instant;
import java.util.*;

// Executa a lógica real sem dependências Spring; complementa, não substitui, a suíte Gradle.
public class OrcamentoRegrasCheck {
    private static int checks;
    public static void main(String[] args) {
        Instant agora = Instant.parse("2026-10-10T17:00:00Z");
        String token = "6bbd9908-4783-4991-a4f9-b0fc1807115a";
        String hash = TokenOrcamento.hash(token);
        check(hash.length() == 64, "Hash SHA-256");
        check(hash.equals(TokenOrcamento.hash(token.toUpperCase(Locale.ROOT))), "UUID maiúsculo normalizado");
        check(!hash.contains(token), "Hash não contém segredo bruto");
        check(!hash.equals(TokenOrcamento.hash("6bbd9908-4783-4991-a4f9-b0fc1807115b")), "Tokens diferentes");
        for (String invalido : new String[]{null, "", " ", "abc", "1-1-1-1-1", " " + token,
                token + " ", token.replace("-", ""), token.substring(0, 35), token + "0"}) {
            invalido(() -> TokenOrcamento.hash(invalido));
        }
        HashSet<UUID> gerados = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            UUID gerado = TokenOrcamento.gerar();
            check(gerado.version() == 4 && gerado.variant() == 2, "Geração UUIDv4");
            check(gerados.add(gerado), "Amostra sem repetição");
            check(TokenOrcamento.hash(gerado.toString()).matches("[0-9a-f]{64}"), "Hash canônico");
        }
        ValidadeLinkOrcamento.validar(StatusOrcamento.AGUARDANDO_APROVACAO, agora.plusNanos(1), null, agora);
        checks++;
        for (StatusOrcamento status : StatusOrcamento.values()) {
            if (status != StatusOrcamento.AGUARDANDO_APROVACAO)
                invalido(() -> ValidadeLinkOrcamento.validar(status, agora.plusSeconds(10), null, agora));
        }
        invalido(() -> ValidadeLinkOrcamento.validar(null, agora.plusSeconds(10), null, agora));
        invalido(() -> ValidadeLinkOrcamento.validar(StatusOrcamento.AGUARDANDO_APROVACAO, null, null, agora));
        invalido(() -> ValidadeLinkOrcamento.validar(StatusOrcamento.AGUARDANDO_APROVACAO, agora, null, agora));
        invalido(() -> ValidadeLinkOrcamento.validar(StatusOrcamento.AGUARDANDO_APROVACAO, agora.minusNanos(1), null, agora));
        invalido(() -> ValidadeLinkOrcamento.validar(StatusOrcamento.AGUARDANDO_APROVACAO,
                agora.plusSeconds(10), agora.minusSeconds(1), agora));
        System.out.println("PASS: " + checks + " verificações da lógica real de token, status, uso e expiração.");
    }
    private static void check(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
        checks++;
    }
    private static void invalido(Runnable acao) {
        try {
            acao.run();
        } catch (LinkOrcamentoInvalidoException ex) {
            check(ex.getMessage().equals("Link inválido, expirado ou já utilizado"), "Erro genérico");
            return;
        }
        throw new AssertionError("Deveria invalidar o link");
    }
}
