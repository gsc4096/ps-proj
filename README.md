# Assinatura de Refeições

Implementação em Java do caso de uso **Assinar Plano de Refeições**, seguindo o diagrama de classes de projeto
(fronteira, controlador, coleções, entidades e objetos externos) com persistência em CSV.

## Como rodar

Sem Maven (só JDK 17+):

```bash
mkdir -p out && javac -encoding UTF-8 -d out $(find src/main -name '*.java')
java -cp out assinatura.Main            # usa a pasta ./dados
```

Com Maven:

```bash
mvn test
mvn exec:java                           # roda assinatura.Main
```

O SMS é simulado: o código de confirmação aparece no próprio console.
Cartão de teste aprovado: `4111 1111 1111 1111`, validade `12/99`, CVV `123`.
Cartão de teste recusado: `4000 0000 0000 0002`.

## Estrutura

| Pacote | Classes | Papel |
|---|---|---|
| `fronteira` | `TelaAssinatura` | lê o teclado e exibe; repassa ações ao controlador |
| `controle` | `ControladorAssinatura` | coordena o fluxo, uma etapa por vez |
| `entidade` | `Assinante`, `Assinatura`, `Plano`, `Pedido`, `Item`, `Entrega`, `Produto`, `Endereco`, `Cartao`, `Pagamento` | dados e regras de negócio |
| `colecao` | `ColecaoAssinantes`, `ColecaoPlanos`, `ColecaoProdutos` | conjuntos de assinantes, planos e produtos |
| `externo` | `Notificador`, `GatewayOperadoraCartao`, `OperadoraCartao` | SMS e operadora simulados |
| `persistencia` | `RepositorioCsv` | leitura e gravação dos CSVs |

## Dados (pasta `dados/`)

- Entrada: `planos.csv` e `produtos.csv` (separador `;`, UTF-8, preferências separadas por `|`).
- Gerados: `assinantes`, `assinaturas`, `enderecos`, `pedidos`, `itens`, `entregas` e `pagamentos`.
- Dados de cartão **nunca** são gravados.

## Quem cria cada objeto (o controlador não cria instâncias)

| Objeto | Criado por |
|---|---|
| `Assinante` | `ColecaoAssinantes.registrar(celular)` |
| `Assinatura` | `Assinante.contratar(plano)` |
| `Pedido` | `Assinatura.gerarPedido()` |
| `Endereco` | `Assinatura.aguardarPagamento(...)` |
| `Pagamento` | `Assinatura.confirmar(codigoAutorizacao, pedido, instante)` |
| `Item` | `Pedido.substituirItens(...)` |
| `Entrega` | `Pedido.aprovar(previsao)` |

`Cartao` é só o dado digitado na tela, e a fronteira o repassa. O teste `ProjetoRegrasTest` falha se o
controlador voltar a usar `new` com alguma entidade.

## Regras de negócio

- **Bloqueio:** o 3º código inválido bloqueia o celular; o bloqueio fica gravado e vale em novas execuções.
- **Limite do plano:** em cada categoria (PRATO, ACOMPANHAMENTO, SOBREMESA) a soma das quantidades não passa do total de refeições do plano.
- **Filtro por preferência:** só aparecem produtos disponíveis que atendem **todas** as preferências marcadas.
- **Previsão de entrega:** data da confirmação + 2 dias; se cair no domingo, passa para segunda.
- **Plano inativo** não é listado.

## Premissas (ajustar se o enunciado disser outra coisa)

- O limite do plano vale **por categoria**; escolher zero itens numa categoria é permitido, mas o pedido precisa ter ao menos um item.
- Novo código **não** zera as tentativas erradas (senão daria pra burlar o bloqueio).
- O pagamento recusado pode ser repetido sem limite, até aprovar ou desistir.

## Diferenças em relação ao diagrama (atualizar o diagrama de projeto)

- Novas classes: `ColecaoAssinantes` e `Entrega` (Pedido 1 ─ 0..* Entrega).
- Assinaturas de métodos que mudaram para o controlador deixar de criar objetos:
  `Assinante.contratar(plano)`, `Assinatura.gerarPedido()`, `Assinatura.aguardarPagamento(logradouro, numero, complemento, cep, cidade, uf): boolean`,
  `Assinatura.confirmar(codigoAutorizacao, pedido, instante)`, `ControladorAssinatura.confirmarEndereco(6 campos)`,
  `ColecaoAssinantes.buscar/registrar`. `Pedido.previsaoPrimeiraEntrega` virou `Entrega.dataPrevista`.
- `ControladorAssinatura`: atributos extras `etapa`, `indiceCategoria` e getters de estado; recebe `ColecaoAssinantes` no construtor.
- `Assinatura.cancelar()`, `Pedido.cancelar()` e `Endereco.isValido()`: usados na desistência e na validação do endereço.
- `Assinante.restaurar(...)` (carregar do CSV) e `Assinante.tentativasRestantes()`.
- `RepositorioCsv`: não está no diagrama; as entidades chamam `salvar()` e ele grava.
- `TelaAssinatura.executar()`: laço de leitura do console.

## Testes (`src/test`)

- `EntidadesTest`, `OperadoraCartaoTest`, `RepositorioCsvTest`: unitários.
- `FluxoIntegracaoTest`: fluxo principal e os alternativos `FA1` (código bloqueado), `FA2` (pagamento recusado) e `FA3` (desistência). Renomeie os rótulos conforme os diagramas de sequência da equipe.

## O que o código não resolve (feedback da entrega 3: vai nos diagramas)

- Separar o diagrama de sequência do **fluxo principal** dos **fluxos alternativos**, cada um inteiro, sem segmentar.
- Reduzir e renomear os estímulos do ator para eventos significativos (hoje o controlador tem 9: `informarCelular`, `informarCodigo`,
  `solicitarNovoCodigo`, `selecionarPlano`, `informarPreferencias`, `selecionarItens`, `confirmarEndereco`, `informarCartao`, `desistirPagamento`).
- Mostrar nos diagramas as coleções/multiobjetos (`Assinantes`, `Item`, `Entrega`, `Endereco`) e os `create` feitos pelos donos da tabela acima.
- Refletir no diagrama de classes de projeto as classes e métodos novos.
