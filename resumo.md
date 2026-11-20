# Auditoria da US20 — T5

Data da verificação: 2026-11-12

## Resultado

**US20-T5 implementada e validada.** O serviço cobre as contagens do resumo, taxa de conversão com divisão por zero protegida e ordenação consolidada das pendências. O controller devolve HTTP 200 para perfis autorizados e bloqueia os demais conforme a matriz de acesso definida.

Esta conclusão é específica da T5. **A US20 como um todo ainda tem pendências**, listadas abaixo; portanto, não considerar o épico inteiro concluído somente com esta tarefa.

## Itens da T5 verificados

| Item | Resultado | Evidência |
|---|---|---|
| Contagens do resumo | Implementado: orçamentos em elaboração e aguardando aprovação, OS em planejamento e etapas vencidas. | `DashboardService.resumo()` usa métodos de contagem dos repositórios. |
| Taxa de conversão | Implementado como aprovados / (aprovados + rejeitados) × 100; retorna `0.0` quando não há decisões. | `calcularTaxaConversao()` e teste com denominador zero. |
| Ordenação de pendências | Implementada por `data_limite` crescente; valores nulos ficam no final. Há desempate determinístico por tipo e id. | `ORDENACAO_PENDENCIAS` e teste de consolidação/ordenação. |
| Cenário do resumo HTTP 200 | Implementado e testado para ADMIN e GESTOR_PROJETO, com os campos do contrato. | `DashboardControllerTest`. |
| Acesso negado | ATENDENTE e FINANCEIRO recebem 403; chamadas anônimas recebem 401. Os testes confirmam que o serviço não é invocado para essas requisições. | `SecurityConfig` e `DashboardControllerTest`. |
| Endpoints de pendências e resumo | Ambos estão protegidos por `/api/v1/dashboard/**`; somente ADMIN e GESTOR_PROJETO acessam o panorama completo, incluindo a taxa. | `SecurityConfig`. |

### Testes executados

Comando, executado com `--rerun-tasks`:

```text
./Core/gradlew -p Core test --tests com.stentio.core.service.DashboardServiceTest --tests com.stentio.core.controller.DashboardControllerTest --rerun-tasks
```

Resultado: **BUILD SUCCESSFUL**. Os testes do serviço e do controller executaram sem falhas.

## Pendências conhecidas fora da T5

- **RN.1 (entrega final):** a etapa é considerada atrasada por prazo vencido e alocação, mas o modelo atual ainda não registra se o arquivo final foi submetido. A contagem não consegue excluir etapas já entregues; precisa integrar a fonte de verdade da US-014.
- **Interface do painel (CA.1, CA.2 e CA.4):** não foi encontrada tela de dashboard com cards, lista de pendências, links navegáveis, atualização manual e auto-refresh de 60 segundos.
- **Filtro por perfil (CA.3):** o perfil `RECURSO` não existe no modelo atual. A autorização desta T4 foi definida para os perfis existentes: ADMIN e GESTOR_PROJETO acessam; ATENDENTE e FINANCEIRO não acessam.
- **Teste com PostgreSQL:** os testes de integração opt-in das projeções e da contagem precisam ser executados em ambiente com datasource configurado. Em execução anterior, o contexto JPA não obteve metadados JDBC; não foi possível validar esses testes contra o PostgreSQL neste ambiente.
- A descrição de **US20-T1 e US20-T2 é duplicada**. Ambas usam hoje `data_limite_entrega`, preenchida na alocação; `prazo_estimado_horas` é estimativa e não é usada como vencimento.

## Referências no código

- Serviço e ordenação: [DashboardService.java](./Core/src/main/java/com/stentio/core/service/DashboardService.java)
- Autorização: [SecurityConfig.java](./Core/src/main/java/com/stentio/core/config/SecurityConfig.java)
- Testes de serviço: [DashboardServiceTest.java](./Core/src/test/java/com/stentio/core/service/DashboardServiceTest.java)
- Testes HTTP/perfis: [DashboardControllerTest.java](./Core/src/test/java/com/stentio/core/controller/DashboardControllerTest.java)
- Contagem de etapa e prazo: [EtapaOrdemServicoRepository.java](./Core/src/main/java/com/stentio/core/repository/EtapaOrdemServicoRepository.java)
