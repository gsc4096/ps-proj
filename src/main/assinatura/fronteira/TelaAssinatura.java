package assinatura.fronteira;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import assinatura.controle.ControladorAssinatura;
import assinatura.entidade.Assinatura;
import assinatura.entidade.Cartao;
import assinatura.entidade.Item;
import assinatura.entidade.Pedido;
import assinatura.entidade.Plano;
import assinatura.entidade.Produto;

/**
 * Fronteira em console. Os métodos de ação (informar..., selecionar..., confirmar..., desistir...)
 * apenas repassam ao controlador; os métodos exibir... mostram o resultado. O método
 * {@link #executar()} lê o teclado conforme a etapa em que o controlador está.
 */
public class TelaAssinatura {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> OPCOES_PREFERENCIA = List.of("VEGETARIANO", "SEM_LACTOSE", "SEM_GLUTEN");
    private static final List<String> ROTULOS_PREFERENCIA =
            List.of("Vegetariano", "Intolerante a lactose", "Intolerante a gluten");

    private final BufferedReader entrada;
    private final PrintStream saida;
    private ControladorAssinatura controlador;

    private List<Plano> planosExibidos = List.of();
    private String categoriaExibida = "";
    private List<Produto> produtosExibidos = List.of();

    public TelaAssinatura(BufferedReader entrada, PrintStream saida) {
        this.entrada = entrada;
        this.saida = saida;
    }

    public void setControlador(ControladorAssinatura controlador) {
        this.controlador = controlador;
    }

    // ---------- ações do assinante (repassadas ao controlador) ----------

    public void informarCelular(String celular) { controlador.informarCelular(celular); }
    public void informarCodigo(String codigo) { controlador.informarCodigo(codigo); }
    public void solicitarNovoCodigo() { controlador.solicitarNovoCodigo(); }
    public void selecionarPlano(Plano plano) { controlador.selecionarPlano(plano); }
    public void informarPreferencias(List<String> preferencias) { controlador.informarPreferencias(preferencias); }
    public void selecionarItens(String categoria, Map<Produto, Integer> escolhas) { controlador.selecionarItens(categoria, escolhas); }
    public void confirmarEndereco(String logradouro, String numero, String complemento, String cep, String cidade, String uf) {
        controlador.confirmarEndereco(logradouro, numero, complemento, cep, cidade, uf);
    }
    public void informarCartao(Cartao cartao) { controlador.informarCartao(cartao); }
    public void desistirPagamento() { controlador.desistirPagamento(); }

    // ---------- exibição ----------

    public void exibirAviso(String texto) {
        saida.println(">> " + texto);
    }

    public void exibirPlanos(List<Plano> planos) {
        planosExibidos = List.copyOf(planos);
        saida.println();
        saida.println("Planos disponiveis:");
        for (int i = 0; i < planos.size(); i++) {
            Plano p = planos.get(i);
            saida.printf("  %d) %s - %d refeicoes, %s, %s%n", i + 1, p.getNome(), p.getQtdRefeicoes(),
                    p.getPeriodicidade().toLowerCase(), moeda(p.getValor()));
        }
    }

    public void exibirProdutos(String categoria, List<Produto> produtos) {
        categoriaExibida = categoria;
        produtosExibidos = List.copyOf(produtos);
        saida.println();
        saida.println("Opcoes de " + categoria + ":");
        for (int i = 0; i < produtos.size(); i++) {
            saida.printf("  %d) %s%n", i + 1, produtos.get(i).getNome());
        }
    }

    public void exibirPedido(Pedido pedido) {
        saida.println();
        saida.println("Resumo do pedido:");
        for (Item item : pedido.getItens()) {
            saida.printf("  %dx %s (%s)%n", item.getQtde(), item.getProduto().getNome(), item.getProduto().getCategoria());
        }
    }

    public void exibirConfirmacao(Assinatura assinatura, Pedido pedido) {
        saida.println();
        saida.println("Assinatura ativa!");
        saida.println("  Protocolo: " + assinatura.getProtocolo());
        saida.println("  Plano: " + assinatura.getPlano().getNome() + " - " + moeda(assinatura.getValor()));
        saida.println("  Entrega em: " + assinatura.getEndereco());
        saida.println("  Primeira entrega prevista: " + pedido.getPrevisaoPrimeiraEntrega().format(DATA));
        for (Item item : pedido.getItens()) {
            saida.printf("  %dx %s%n", item.getQtde(), item.getProduto().getNome());
        }
    }

    // ---------- leitura do teclado ----------

    /** Repete as perguntas conforme a etapa do controlador até o fluxo terminar (ou a entrada acabar). */
    public void executar() {
        saida.println("=== Assinatura de Refeicoes ===");
        try {
            while (controlador.getEtapa() != ControladorAssinatura.Etapa.ENCERRADO) {
                switch (controlador.getEtapa()) {
                    case CELULAR -> informarCelular(ler("Celular (DDD + numero): "));
                    case CODIGO -> lerCodigo();
                    case PLANO -> lerPlano();
                    case PREFERENCIAS -> lerPreferencias();
                    case ITENS -> lerItens();
                    case ENDERECO -> lerEndereco();
                    case CARTAO -> lerCartao();
                    default -> { return; }
                }
            }
        } catch (EntradaEncerrada e) {
            saida.println();
            saida.println("Entrada encerrada.");
        }
    }

    private void lerCodigo() {
        String codigo = ler("Codigo recebido por SMS (N = novo codigo): ");
        if (codigo.equalsIgnoreCase("N")) {
            solicitarNovoCodigo();
        } else {
            informarCodigo(codigo);
        }
    }

    private void lerPlano() {
        Integer escolha = numero(ler("Escolha o plano (numero): "));
        if (escolha == null || escolha < 1 || escolha > planosExibidos.size()) {
            exibirAviso("Opcao invalida.");
            return;
        }
        selecionarPlano(planosExibidos.get(escolha - 1));
    }

    private void lerPreferencias() {
        saida.println();
        saida.println("Preferencias alimentares:");
        for (int i = 0; i < ROTULOS_PREFERENCIA.size(); i++) {
            saida.printf("  %d) %s%n", i + 1, ROTULOS_PREFERENCIA.get(i));
        }
        String resposta = ler("Numeros separados por virgula (ENTER = nenhuma): ");
        List<String> escolhidas = new ArrayList<>();
        for (String parte : resposta.split(",")) {
            if (parte.isBlank()) {
                continue;
            }
            Integer n = numero(parte);
            if (n == null || n < 1 || n > OPCOES_PREFERENCIA.size()) {
                exibirAviso("Opcao invalida: " + parte.trim());
                return;
            }
            escolhidas.add(OPCOES_PREFERENCIA.get(n - 1));
        }
        informarPreferencias(escolhidas);
    }

    private void lerItens() {
        String resposta = ler("Itens de " + categoriaExibida + " no formato: (numero do item:quantidade), separados por virgula (ENTER = pular): ");
        Map<Produto, Integer> escolhas = new LinkedHashMap<>();
        for (String parte : resposta.split(",")) {
            if (parte.isBlank()) {
                continue;
            }
            String[] par = parte.split(":");
            Integer posicao = par.length == 2 ? numero(par[0]) : null;
            Integer qtde = par.length == 2 ? numero(par[1]) : null;
            if (posicao == null || qtde == null || posicao < 1 || posicao > produtosExibidos.size()) {
                exibirAviso("Formato invalido: " + parte.trim());
                return;
            }
            escolhas.merge(produtosExibidos.get(posicao - 1), qtde, Integer::sum);
        }
        selecionarItens(categoriaExibida, escolhas);
    }

    private void lerEndereco() {
        saida.println();
        saida.println("Endereco de entrega:");
        String logradouro = ler("  Logradouro: ");
        String numero = ler("  Numero: ");
        String complemento = ler("  Complemento (opcional): ");
        String cep = ler("  CEP: ");
        String cidade = ler("  Cidade: ");
        String uf = ler("  UF: ");
        confirmarEndereco(logradouro, numero, complemento, cep, cidade, uf);
    }

    private void lerCartao() {
        String numero = ler("Numero do cartao (D = desistir): ");
        if (numero.equalsIgnoreCase("D")) {
            desistirPagamento();
            return;
        }
        String titular = ler("  Titular: ");
        String validade = ler("  Validade (MM/AA): ");
        String cvv = ler("  CVV: ");
        informarCartao(new Cartao(titular, numero, validade, cvv));
    }

    private String ler(String pergunta) {
        saida.print(pergunta);
        saida.flush();
        try {
            String linha = entrada.readLine();
            if (linha == null) {
                throw new EntradaEncerrada();
            }
            return linha.trim();
        } catch (IOException e) {
            throw new EntradaEncerrada();
        }
    }

    private static Integer numero(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String moeda(BigDecimal valor) {
        return "R$ " + valor.toPlainString().replace('.', ',');
    }

    private static class EntradaEncerrada extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
