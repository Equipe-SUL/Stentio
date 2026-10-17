// solicitacaoSchema.ts
import { z } from "zod";

// Espelha o @Size(max = 2000) do SolicitacaoRequest no backend.
export const DESCRICAO_MAX = 2000;

export const MENSAGEM_IDIOMAS_IGUAIS =
  "O idioma de destino deve ser diferente do idioma de origem.";

export const solicitacaoSchema = z
  .object({
    clienteId: z.string().min(1, "Selecione o cliente"),
    categoriaProjetoId: z.string().min(1, "Selecione a categoria do projeto"),
    idiomaOrigemId: z.string().min(1, "Selecione o idioma de origem"),
    idiomaDestinoId: z.string().min(1, "Selecione o idioma de destino"),
    descricao: z
      .string()
      .max(DESCRICAO_MAX, `A descrição deve ter no máximo ${DESCRICAO_MAX} caracteres`),
  })
  // RN.2: origem e destino não podem ser o mesmo idioma.
  .refine(
    (valores) =>
      !valores.idiomaOrigemId ||
      !valores.idiomaDestinoId ||
      valores.idiomaOrigemId !== valores.idiomaDestinoId,
    { message: MENSAGEM_IDIOMAS_IGUAIS, path: ["idiomaDestinoId"] },
  );

export type SolicitacaoFormValues = z.infer<typeof solicitacaoSchema>;

export const VALORES_INICIAIS: SolicitacaoFormValues = {
  clienteId: "",
  categoriaProjetoId: "",
  idiomaOrigemId: "",
  idiomaDestinoId: "",
  descricao: "",
};

export const CAMPOS_DO_FORMULARIO = Object.keys(
  VALORES_INICIAIS,
) as (keyof SolicitacaoFormValues)[];