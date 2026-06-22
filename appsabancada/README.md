# appsabancada

Projeto Spring Boot com interface web para acompanhamento e operação de uma linha didática 4.0.

## Como executar

1. Abra o projeto na pasta raiz `appsabancada`.
2. Confira se o MySQL está ativo.
3. Confirme a conexão local com o banco:
   - Banco: `dbSmart40`
   - Usuário: `root`
   - Senha: `senai`
4. Rode a classe `AppsabancadaApplication`.
5. Acesse: `http://localhost:8088`.

## Configuração do banco

A configuração principal está em:

- `src/main/resources/application.properties`

Também existe uma configuração externa opcional em:

- `config/db-config.properties`

Se o arquivo externo não for encontrado, a aplicação continua usando o `application.properties`.

## Criar o banco manualmente

Caso o MySQL não crie o banco automaticamente, execute no MySQL Workbench:

```sql
CREATE DATABASE IF NOT EXISTS dbSmart40 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

O Hibernate está configurado com `spring.jpa.hibernate.ddl-auto=update`, então as tabelas são criadas/atualizadas automaticamente ao iniciar a aplicação.
