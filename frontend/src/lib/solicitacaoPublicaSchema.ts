import { z } from "zod";
import { validarCpfOuCnpj } from "./solicitacaoPublicaService";

export const DESCRICAO_MAX = 2000;

export const MENSAGEM_IDIOMAS_IGUAIS =
  "O idioma de destino deve ser diferente do idioma de origem.";

export const solicitacaoPublicaSchema = z
  .object({
    nomeEmpresa: z
      .string()
      .trim()
      .min(1, "Nome da empresa é obrigatório")
      .max(150, "Nome da empresa deve ter no máximo 150 caracteres"),
    nomeRepresentante: z
      .string()
      .trim()
      .min(1, "Nome do representante é obrigatório")
      .max(100, "Nome do representante deve ter no máximo 100 caracteres"),
    emailRepresentante: z
      .string()
      .trim()
      .min(1, "E-mail do representante é obrigatório")
      .email("E-mail do representante inválido")
      .max(150, "E-mail do representante deve ter no máximo 150 caracteres"),
    telefone: z
      .string()
      .optional()
      .refine(
        (val) => !val || !val.trim() || /^[0-9+()\-\s]{8,20}$/.test(val.trim()),
        "Telefone inválido"
      ),
    cpfCnpj: z
      .string()
      .trim()
      .min(1, "CPF ou CNPJ é obrigatório")
      .refine((val) => validarCpfOuCnpj(val), "CPF ou CNPJ inválido"),
    categoriaProjetoId: z
      .string()
      .min(1, "Categoria do projeto é obrigatória"),
    idiomaOrigemId: z
      .string()
      .min(1, "Idioma de origem é obrigatório"),
    idiomaDestinoId: z
      .string()
      .min(1, "Idioma de destino é obrigatório"),
    descricao: z
      .string()
      .optional()
      .refine(
        (val) => !val || val.length <= DESCRICAO_MAX,
        `Descrição deve ter no máximo ${DESCRICAO_MAX} caracteres`
      ),
  })
  .refine(
    (valores) =>
      !valores.idiomaOrigemId ||
      !valores.idiomaDestinoId ||
      valores.idiomaOrigemId !== valores.idiomaDestinoId,
    {
      message: MENSAGEM_IDIOMAS_IGUAIS,
      path: ["idiomaDestinoId"],
    }
  );

export type SolicitacaoPublicaFormInput = z.infer<typeof solicitacaoPublicaSchema>;

export const VALORES_INICIAIS_PUBLICO: SolicitacaoPublicaFormInput = {
  nomeEmpresa: "",
  nomeRepresentante: "",
  emailRepresentante: "",
  telefone: "",
  cpfCnpj: "",
  categoriaProjetoId: "",
  idiomaOrigemId: "",
  idiomaDestinoId: "",
  descricao: "",
};
