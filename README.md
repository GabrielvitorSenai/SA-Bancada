# Etapa 3 — Frontend do Sistema SMART 4.0

HMI do operador (Thymeleaf + JS puro) que consome a API REST da Etapa 2.
Layout com header no topo e **pedidos em cartões** (mostrando blocos e lâminas com
as cores), Painel de Estoque (28 posições), Painel de Expedição (12 posições),
configurador dinâmico com prévia visual do produto, validações e toast.

---

## 1. Veja funcionando agora (sem backend)

Abra **`preview/preview.html`** no navegador. Usa dados simulados (mock) + os assets
reais, então funciona offline — ideal para testar e apresentar na banca.

---

## 2. Integração no projeto Spring Boot (`appsa`)

### Backend — copie para `src/main/java/com/smart/appsa/`

| Arquivo | Destino | Observação |
|---|---|---|
| `backend/dto/PosicaoEstoqueDTO.java` | `dto/` | **Novo** |
| `backend/controller/ExpedicaoController.java` | `controller/` | **Novo** — `GET /api/expedicao` |
| `backend/config/DataSeeder.java` | `config/` (crie a pasta) | **Novo, opcional** — popula 28 posições + demo |
| `backend/service/EstoqueService.java` | `service/` | **Substitui** — adiciona `mapaEstoque()` |
| `backend/controller/EstoqueController.java` | `controller/` | **Substitui** — adiciona `GET /api/estoque/posicoes` |
| `backend/service/PedidoService.java` | `service/` | **Substitui** — adiciona `enviarParaProducao()` |
| `backend/controller/PedidoController.java` | `controller/` | **Substitui** — adiciona `PUT /api/pedidos/{id}/produzir` |

> Todas as substituições apenas **acrescentam** métodos; nada existente foi removido.

### Frontend — copie para `src/main/resources/`

| Arquivo | Destino |
|---|---|
| `frontend/templates/dashboard.html` | `templates/` |
| `frontend/static/css/dashboard.css` | `static/css/` |
| `frontend/static/js/dashboard.js` | `static/js/` |
| `frontend/static/assets/` (pasta inteira) | `static/assets/` |

### Exponha a rota do dashboard

No seu `ProducaoViewController.java`, adicione:

```java
@GetMapping("/dashboard")
public String dashboard(Model model) {
    model.addAttribute("tituloPagina", "Bancada Smart 4.0 — SENAI Timbó");
    return "dashboard";
}
```

Acesse em `http://localhost:18089/dashboard`.

---

## 3. Fluxo de status do pedido

```
   POST /api/pedidos                PUT /api/pedidos/{id}/produzir       PUT /api/pedidos/{id}/status
[novo] ─────────────► (1) PENDENTE ───────────────────────► (2) EM PRODUÇÃO ──────────────────► (3) CONCLUÍDO
                                     (botão "Enviar p/ produção")        (botão "Concluir e expedir" → gera Expedição)
```

| Verbo | Endpoint | Uso |
|---|---|---|
| GET | `/api/estoque/posicoes` | **Novo** — Mapa de Estoque (28) |
| GET | `/api/expedicao` | **Novo** — Mapa de Expedição (12) |
| GET | `/api/pedidos` | Cartões de pedidos |
| POST | `/api/pedidos` | Configurador → salvar |
| PUT | `/api/pedidos/{id}/produzir` | **Novo** — pendente → produção |
| PUT | `/api/pedidos/{id}/status` | produção → concluído + expedição |

---

## 4. Requisitos do enunciado cobertos

- ✅ Grid de 28 posições, cor por bloco ou cinza se vazio, com id da posição.
- ✅ Painel de Expedição (12) com pedidos concluídos aguardando retirada.
- ✅ Pedidos em **cartões**, com blocos e lâminas e **cores destacadas no texto**.
- ✅ Filtro por status (Todos/Pendente/Produção/Concluído).
- ✅ Botão "Enviar para produção" nos pendentes (e "Concluir e expedir" em produção).
- ✅ Configurador adaptável a Simples/Duplo/Triplo + prévia visual do produto (assets).
- ✅ Lâminas com cor (6), padrão (4) e posição (Esquerda/Frente/Direita).
- ✅ Validação em tempo real: botão "Salvar" desabilitado sem ordem de produção.
- ✅ Alerta de indisponibilidade de estoque por cor.
- ✅ Toast: "Pedido cadastrado com sucesso e enviado para a fila de produção!"

---

## 5. Notas técnicas

- Cores das lâminas no texto seguem a escala do enunciado (1 vermelho … 6 branco);
  cores da tampa/bloco seguem a outra escala (1 preto, 2 vermelho, 3 azul).
- O JS tolera assets ausentes (`img.onerror`) e backend offline (fallback).
- Padrões (`rpadrao*`) só têm variantes -1/-2; a posição mapeia para a variante
  (Frente→2, demais→1). Ajuste `imgPadrao()` se não bater com a peça física.
- A execução física na bancada (CLP, assets `bancada/*_on|off|pause`) continua como
  módulo opcional fora deste escopo — o "Enviar para produção" aqui só muda o status.
