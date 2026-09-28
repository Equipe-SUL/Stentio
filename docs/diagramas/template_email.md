# Modelo de documento para template de e-mail

### Esse documento descreve uma possível estrutura de templates para o sistema de templates de e-mail alinhado à estrutura do MongoDB.

## 1. Estrutura do documento

```js
TemplateDocument {
  _id: ObjectId
  code: String (Index, Único)
  version: Integer
  status: String ("ACTIVE" | "DRAFT" | "ARCHIVED")
  description: String
  category: String
  recipients: Object {
    to: Array[String]
    cc: Array[String]
    bcc: Array[String]
  }
  subject: String
  body: Object {
    html: String
    text: String
  }
  footer: Object {
    html: String
    text: String
  }
  placeholders: Array[Object] [
    {
      key: String
      description: String
      required: Boolean
      example: String
    }
  ]
  metadata: Object
  created_at: Date
  updated_at: Date
}
```

## 2. Descrição Detalhada dos Campos

| Campo | Tipo | Obrigatório | Descrição |
| :--- | :--- | :--- | :--- |
| `_id` | `ObjectId` | **Sim** | Identificador único gerado automaticamente pelo MongoDB. |
| `code` | `String` | **Sim** | Código identificador de negócio (chave de busca única, ex: `RESET_PASSWORD`). |
| `version` | `Integer` | **Sim** | Versão do template para controle de histórico de alterações. |
| `status` | `String` | **Sim** | Estado do template (`DRAFT`, `ACTIVE`, `ARCHIVED`). |
| `description` | `String` | Não | Breve explicação do propósito do template de e-mail. |
| `category` | `String` | Não | Agrupamento de negócio (ex: `AUTH`, `NOTIFICATIONS`, `FINANCIAL`). |
| `recipients.to` | `Array[String]`| Não | Endereços ou regras fixas/padrão de envio principal. |
| `recipients.cc` | `Array[String]`| Não | Endereços padrão para cópia simples. |
| `recipients.bcc` | `Array[String]`| Não | Endereços padrão para cópia oculta (auditoria/logs). |
| `subject` | `String` | **Sim** | Assunto da mensagem. Suporta placeholders. |
| `body.html` | `String` | **Sim** | Corpo do e-mail codificado em HTML para renderização rica. |
| `body.text` | `String` | Não | Versão alternativa do corpo em texto puro (fallback). |
| `footer.html` | `String` | Não | Rodapé padrão do e-mail codificado em HTML. |
| `footer.text` | `String` | Não | Versão alternativa do rodapé em texto puro. |
| `placeholders` | `Array[Object]`| **Sim** | Lista explicativa das variáveis esperadas pelo template. |
| `placeholders[].key` | `String` | **Sim** | Nome exato da variável no formato `{{nome_variavel}}`. |
| `placeholders[].description` | `String` | Não | Descrição da finalidade do placeholder. |
| `placeholders[].required` | `Boolean` | **Sim** | Indica se o parâmetro é obrigatório na interpolação. |
| `placeholders[].example` | `String` | Não | Exemplo do dado esperado para documentação e testes. |
| `metadata` | `Object` | Não | Pares chave-valor com dados operacionais/estatísticos livres. |
| `created_at` | `Date` | **Sim** | Timestamp de auditoria da data de criação do registro. |
| `updated_at` | `Date` | **Sim** | Timestamp de auditoria da última modificação do registro. |


## 3. Exemplo

```json
{
  "_id": { "$oid": "651a2b3c4d5e6f7a8b9c0d1e" },
  "code": "WELCOME_EMAIL",
  "version": 1,
  "status": "ACTIVE",
  "description": "E-mail enviado após a criação do cadastro de um novo usuário.",
  "category": "ONBOARDING",
  "recipients": {
    "to": [],
    "cc": [],
    "bcc": ["auditoria@empresa.com"]
  },
  "subject": "Bem-vindo(a), {{nome_usuario}}! Confirme sua conta",
  "body": {
    "html": "<p>Olá <strong>{{nome_usuario}}</strong>,</p><p>Sua conta foi criada com sucesso na data {{data_atual}}.</p><p>Seu número de protocolo é: <code>{{numero_protocolo}}</code>.</p><p><a href=\"{{link_acesso}}\">Clique aqui para ativar sua conta</a>.</p>",
    "text": "Olá {{nome_usuario}},\nSua conta foi criada na data {{data_atual}}.\nProtocolo: {{numero_protocolo}}.\nAcesse: {{link_acesso}}"
  },
  "footer": {
    "html": "<hr><p style=\"font-size: 12px; color: #777;\">Este e-mail é automático. Dúvidas? Contate o suporte.</p>",
    "text": "---\nEste e-mail é automático. Dúvidas? Contate o suporte."
  },
  "placeholders": [
    { "key": "{{nome_usuario}}", "description": "Nome completo do usuário", "required": true, "example": "Wesley Xavier" },
    { "key": "{{numero_protocolo}}", "description": "Identificador do registro do usuário", "required": true, "example": "PRT-2026-99" },
    { "key": "{{data_atual}}", "description": "Data de emissão do e-mail", "required": true, "example": "20/09/2026" },
    { "key": "{{link_acesso}}", "description": "URL tokenizada de ativação", "required": true, "example": "https://app.empresa.com/activate?token=xyz" }
  ],
  "created_at": { "$date": "2026-09-20T10:00:00Z" },
  "updated_at": { "$date": "2026-09-20T10:00:00Z" }
}
```