# appsabancada — Bancada Didática Smart 4.0

Sistema Java Spring Boot com Thymeleaf, CSS e JavaScript puro para operar a bancada didática Smart 4.0: pedidos, estoque, produção, expedição, monitoramento e comunicação com CLPs.

## Tecnologias

- Java 17
- Maven
- Spring Boot
- Spring Data JPA / Hibernate
- Thymeleaf
- MySQL
- JavaScript `fetch`, HTML e CSS responsivo

## Banco de dados local

Configuração padrão em `appsa/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dbSmart40?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=senai
spring.jpa.hibernate.ddl-auto=create
```

Durante desenvolvimento o projeto usa `ddl-auto=create` para recriar o schema e evitar conflitos com tabelas antigas. Para preservar dados depois que o schema estiver correto, troque para:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Se aparecer erro como `Unknown column 'id'`, `Unknown column 'p1_0.id'` ou `there can be only one auto column`, apague/recrie o banco antigo antes de subir a aplicação:

```bash
mysql -u root -psenai < database/resetar-banco-dbSmart40.sql
```

O Hibernate criará as tabelas automaticamente ao iniciar.

## Como rodar

```bash
cd appsa
mvn clean package -DskipTests
mvn spring-boot:run
```

A aplicação roda em:

- http://localhost:8088/
- http://localhost:8088/conexao
- http://localhost:8088/estoque
- http://localhost:8088/pedidos/novo
- http://localhost:8088/ordens
- http://localhost:8088/expedicao
- http://localhost:8088/monitoramento

## Endpoints principais

### Pedidos

- `GET /api/pedidos`
- `GET /api/pedidos/{id}`
- `POST /api/pedidos`
- `DELETE /api/pedidos/{id}`
- `PUT /api/pedidos/{id}/produzir`
- `PUT /api/pedidos/{id}/status`
- `PUT /api/pedidos/{id}/finalizar`

### Estoque

- `GET /api/estoque`
- `GET /api/estoque/posicoes`
- `POST /api/estoque`
- `DELETE /api/estoque/{posicao}`
- `PUT /api/estoque/{posicao}/limpar`

### Expedição

- `GET /api/expedicao`
- `POST /api/expedicao`
- `DELETE /api/expedicao/{posicao}`
- `PUT /api/expedicao/{posicao}/limpar`

### CLP

- `POST /api/clp/start-readings`
- `GET /api/clp/status`
- `GET /api/clp/stream/{bancada}`
- `POST /api/clp/sync-estoque`
- `POST /api/clp/sync-expedicao`
- `POST /api/clp/sync-all`

IPs padrão dos CLPs:

- Estoque: `10.74.241.10`
- Processo: `10.74.241.20`
- Montagem: `10.74.241.30`
- Expedição: `10.74.241.40`

## Fluxo mínimo de validação

1. Abrir `/pedidos/novo`.
2. Criar pedido com tampa, bloco e lâminas.
3. Conferir pedido em `/ordens`.
4. Enviar para produção.
5. Atualizar/finalizar pedido.
6. Conferir expedição.
7. Conferir estoque.
8. Verificar o console do navegador e da aplicação.

## Comandos Git básicos

```bash
git status
git add .
git commit -m "mensagem"
git push
```
