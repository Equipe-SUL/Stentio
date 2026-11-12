# Importação ORM — versão atual

O contrato antigo que gravava pela API do Core foi substituído nesta versão.
As instruções, regras e o roteiro de teste atuais estão em [../README.md](../README.md).

Rotas atuais: `POST /upload-csv/validar` e `POST /upload-csv`.
CSV: `ativo;e-mail;nome;telefone;tipo_servico;descricao_servico`.
Persistência: uma transação envolvendo recursos, tipos de serviço e vínculos.
