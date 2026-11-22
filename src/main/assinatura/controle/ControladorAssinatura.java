package assinatura.controle;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import assinatura.colecao.ColecaoAssinantes;
import assinatura.colecao.ColecaoPlanos;
import assinatura.colecao.ColecaoProdutos;
import assinatura.entidade.Assinante;
import assinatura.entidade.Assinatura;
import assinatura.entidade.Cartao;
import assinatura.entidade.Pedido;
import assinatura.entidade.Plano;
import assinatura.entidade.Produto;
import assinatura.externo.GatewayOperadoraCartao;
import assinatura.externo.Notificador;
import assinatura.fronteira.TelaAssinatura;

/**
 * Coordena o caso de uso "Assinar Plano de Refeições": recebe as ações da tela, aciona
 * entidades, coleções e objetos externos, e devolve o resultado pela tela.
 * Cada ação só vale na sua etapa; fora dela a tela recebe um aviso.
 */
public class ControladorAssinatura {

    public enum Etapa { CELULAR, CODIGO, PLANO, PREFERENCIAS, ITENS, ENDERECO, CARTAO, ENCERRADO }

    public static final List<String> CATEGORIAS = List.of("PRATO", "ACOMPANHAMENTO", "SOBREMESA");

    private final TelaAssinatura tela;
    private final ColecaoAssinantes assinantes;
    private final ColecaoPlanos planos;
    private final ColecaoProdutos produtos;
    private final Notificador notificador;
    private final GatewayOperadoraCartao gateway;
    private final Clock relogio;

    private boolean validado;
    private boolean bloqueado;
    private boolean aprovado;
    private boolean desistiu;

    private Etapa etapa = Etapa.CELULAR;
    private int indiceCategoria;
    private Assinante assinante;
    private Assinatura assinatura;
    private Pedido pedido;

    public ControladorAssinatura(TelaAssinatura tela, ColecaoAssinantes assinantes, ColecaoPlanos planos,
                                 ColecaoProdutos produtos, Notificador notificador, GatewayOperadoraCartao gateway, Clock relogio) {
        this.tela = tela;
        this.assinantes = assinantes;
        this.planos = planos;
        this.produtos = produtos;
        this.notificador = notificador;
        this.gateway = gateway;
        this.relogio = relogio;
        tela.setControlador(this);
    }

    public void informarCelular(String celular) {
        if (!naEtapa(Etapa.CELULAR)) {
            return;
        }
        Optional<Assinante> encontrado = assinantes.registrar(celular);
        if (encontrado.isEmpty()) {
            tela.exibirAviso("Celular invalido. Informe DDD + numero.");
            return;
        }
        assinante = encontrado.get();
        if (assinante.isCodigoBloqueado()) {
            bloqueado = true;
            etapa = Etapa.ENCERRADO;
            tela.exibirAviso("Este celular esta bloqueado apos " + Assinante.MAX_TENTATIVAS
                    + " codigos invalidos. Procure o suporte.");
            return;
        }
        emitirEEnviarCodigo();
        etapa = Etapa.CODIGO;
    }

    public void informarCodigo(String codigo) {
        if (!naEtapa(Etapa.CODIGO)) {
            return;
        }
        String resultado = assinante.validarCodigo(codigo);
        assinante.salvar();
        switch (resultado) {
            case Assinante.OK -> {
                validado = true;
                etapa = Etapa.PLANO;
                tela.exibirAviso("Celular confirmado.");
                tela.exibirPlanos(planos.listar());
            }
            case Assinante.BLOQUEADO -> {
                bloqueado = true;
                etapa = Etapa.ENCERRADO;
                tela.exibirAviso("Celular bloqueado apos " + Assinante.MAX_TENTATIVAS + " codigos invalidos.");
            }
            default -> tela.exibirAviso("Codigo invalido. Tentativas restantes: " + assinante.tentativasRestantes() + ".");
        }
    }

    public void solicitarNovoCodigo() {
        if (!naEtapa(Etapa.CODIGO)) {
            return;
        }
        emitirEEnviarCodigo();
    }

    public void selecionarPlano(Plano plano) {
        if (!naEtapa(Etapa.PLANO)) {
            return;
        }
        if (plano == null || !planos.listar().contains(plano)) {
            tela.exibirAviso("Plano indisponivel.");
            return;
        }
        assinatura = assinante.contratar(plano);
        pedido = assinatura.gerarPedido();
        assinatura.salvar();
        etapa = Etapa.PREFERENCIAS;
    }

    public void informarPreferencias(List<String> preferencias) {
        if (!naEtapa(Etapa.PREFERENCIAS)) {
            return;
        }
        assinante.definirPreferencias(preferencias);
        assinante.salvar();
        indiceCategoria = 0;
        etapa = Etapa.ITENS;
        mostrarProximaCategoria();
    }

    public void selecionarItens(String categoria, Map<Produto, Integer> escolhas) {
        if (!naEtapa(Etapa.ITENS)) {
            return;
        }
        String atual = CATEGORIAS.get(indiceCategoria);
        if (categoria == null || !atual.equalsIgnoreCase(categoria.trim())) {
            tela.exibirAviso("Agora é a vez da categoria " + atual + ".");
            return;
        }
        Plano plano = assinatura.getPlano();
        if (!pedido.validarSelecao(atual, escolhas, plano)) {
            tela.exibirAviso("Selecao invalida: confira a disponibilidade, as preferencias e o limite de "
                    + plano.getQtdRefeicoes() + " unidades por categoria do plano " + plano.getNome() + ".");
            return;
        }
        pedido.substituirItens(atual, escolhas);
        assinatura.salvar();
        indiceCategoria++;
        mostrarProximaCategoria();
    }

    public void confirmarEndereco(String logradouro, String numero, String complemento,
                                  String cep, String cidade, String uf) {
        if (!naEtapa(Etapa.ENDERECO)) {
            return;
        }
        if (!assinatura.aguardarPagamento(logradouro, numero, complemento, cep, cidade, uf)) {
            tela.exibirAviso("Endereco invalido. Confira logradouro, numero, CEP (8 digitos), cidade e UF.");
            return;
        }
        assinatura.salvar();
        etapa = Etapa.CARTAO;
        tela.exibirAviso("Valor a pagar: R$ " + assinatura.getValor().toPlainString().replace('.', ',')
                + ". Informe o cartao ou desista.");
    }

    public void informarCartao(Cartao cartao) {
        if (!naEtapa(Etapa.CARTAO)) {
            return;
        }
        String codigo = gateway.autorizar(cartao, assinatura.getValor());
        if (codigo == null || codigo.isBlank()) {
            aprovado = false;
            tela.exibirAviso("Pagamento recusado pela operadora. Tente outro cartao ou desista.");
            return;
        }
        aprovado = true;
        LocalDateTime agora = LocalDateTime.now(relogio);
        assinatura.confirmar(codigo, pedido, agora);
        etapa = Etapa.ENCERRADO;
        tela.exibirConfirmacao(assinatura, pedido);
    }

    public void desistirPagamento() {
        if (!naEtapa(Etapa.CARTAO)) {
            return;
        }
        desistiu = true;
        encerrarSemAtivacao("Encerrado sem ativacao da assinatura.");
    }

    // ---------- apoio ----------

    private void emitirEEnviarCodigo() {
        String codigo = assinante.emitirCodigo();
        assinante.salvar();
        notificador.enviarCodigo(assinante.getTelefone(), codigo);
    }

    private void mostrarProximaCategoria() {
        List<String> preferencias = assinante.getPreferencias();
        while (indiceCategoria < CATEGORIAS.size()) {
            String categoria = CATEGORIAS.get(indiceCategoria);
            List<Produto> opcoes = produtos.listar().stream()
                    .filter(p -> p.atendeSelecao(categoria, preferencias)).toList();
            if (!opcoes.isEmpty()) {
                tela.exibirProdutos(categoria, opcoes);
                return;
            }
            tela.exibirAviso("Sem produtos disponiveis em " + categoria + " para as suas preferencias.");
            indiceCategoria++;
        }
        finalizarSelecao();
    }

    private void finalizarSelecao() {
        if (pedido.getItens().isEmpty()) {
            boolean haOpcoes = CATEGORIAS.stream().anyMatch(c -> produtos.listar().stream()
                    .anyMatch(p -> p.atendeSelecao(c, assinante.getPreferencias())));
            if (!haOpcoes) {
                encerrarSemAtivacao("Nenhum produto disponivel para as suas preferencias.");
                return;
            }
            tela.exibirAviso("Seu pedido esta vazio. Escolha ao menos um item.");
            indiceCategoria = 0;
            mostrarProximaCategoria();
            return;
        }
        etapa = Etapa.ENDERECO;
        tela.exibirPedido(pedido);
    }

    private void encerrarSemAtivacao(String motivo) {
        assinatura.cancelar();
        assinatura.salvar();
        etapa = Etapa.ENCERRADO;
        tela.exibirAviso(motivo);
    }

    private boolean naEtapa(Etapa esperada) {
        if (etapa != esperada) {
            tela.exibirAviso("Acao indisponivel nesta etapa.");
            return false;
        }
        return true;
    }

    public Etapa getEtapa() { return etapa; }
    public boolean isValidado() { return validado; }
    public boolean isBloqueado() { return bloqueado; }
    public boolean isAprovado() { return aprovado; }
    public boolean isDesistiu() { return desistiu; }
    public Assinante getAssinante() { return assinante; }
    public Assinatura getAssinatura() { return assinatura; }
    public Pedido getPedido() { return pedido; }
}
