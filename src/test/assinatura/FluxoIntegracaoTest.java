package assinatura;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import assinatura.colecao.ColecaoAssinantes;
import assinatura.colecao.ColecaoPlanos;
import assinatura.colecao.ColecaoProdutos;
import assinatura.controle.ControladorAssinatura;
import assinatura.controle.ControladorAssinatura.Etapa;
import assinatura.entidade.Assinatura;
import assinatura.entidade.Cartao;
import assinatura.entidade.Pedido;
import assinatura.entidade.Produto;
import assinatura.externo.GatewayOperadoraCartao;
import assinatura.externo.Notificador;
import assinatura.externo.OperadoraCartao;
import assinatura.fronteira.TelaAssinatura;
import assinatura.persistencia.RepositorioCsv;

/**
 * Testes de integração do caso de uso "Assinar Plano de Refeições", passando por
 * tela -> controlador -> entidades/coleções/objetos externos -> CSV.
 * Fluxo principal + fluxos alternativos (FA1 código bloqueado, FA2 pagamento recusado,
 * FA3 desistência). Ajuste os rótulos FA1..FA3 conforme os diagramas de sequência da equipe.
 */
class FluxoIntegracaoTest {

    static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    static final String CELULAR = "(11) 91234-5678";
    static final String TELEFONE = "11912345678";

    @TempDir
    Path pasta;

    ByteArrayOutputStream bytes;
    Notificador notificador;
    TelaAssinatura tela;
    ControladorAssinatura controlador;
    ColecaoPlanos planos;
    ColecaoProdutos produtos;

    @BeforeEach
    void preparar() throws IOException {
        Files.copy(Path.of("dados/planos.csv"), pasta.resolve("planos.csv"));
        Files.copy(Path.of("dados/produtos.csv"), pasta.resolve("produtos.csv"));
        RepositorioCsv.usar(pasta);
        novaSessao();
    }

    /** Simula abrir o sistema de novo (novo controlador e nova tela, mesmos CSVs). */
    void novaSessao() {
        bytes = new ByteArrayOutputStream();
        PrintStream saida = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        notificador = new Notificador(saida);
        tela = new TelaAssinatura(new BufferedReader(new StringReader("")), saida);
        RepositorioCsv repo = RepositorioCsv.padrao();
        planos = new ColecaoPlanos(repo.carregarPlanos());
        produtos = new ColecaoProdutos(repo.carregarProdutos());
        controlador = new ControladorAssinatura(tela, new ColecaoAssinantes(repo), planos, produtos, notificador,
                new GatewayOperadoraCartao(new OperadoraCartao()), RELOGIO);
    }

    String saida() { return bytes.toString(StandardCharsets.UTF_8); }

    String csv(String arquivo) throws IOException { return Files.readString(pasta.resolve(arquivo + ".csv")); }

    long linhasDeDados(String arquivo) throws IOException { return Files.readAllLines(pasta.resolve(arquivo + ".csv")).size() - 1; }

    // ---------- passos reutilizáveis ----------

    void ateOPlano() {
        tela.informarCelular(CELULAR);
        tela.informarCodigo(notificador.getUltimoCodigo());
    }

    void atePreferencias() {
        ateOPlano();
        tela.selecionarPlano(planos.listar().get(0)); // Básico: 5 refeições
    }

    void ateItens(List<String> preferencias) {
        atePreferencias();
        tela.informarPreferencias(preferencias);
    }

    void escolherPrimeiroDisponivel(String categoria, int qtde) {
        Produto produto = produtos.listar().stream()
                .filter(p -> p.atendeSelecao(categoria, controlador.getAssinante().getPreferencias()))
                .findFirst().orElseThrow();
        tela.selecionarItens(categoria, Map.of(produto, qtde));
    }

    void ateOEndereco() {
        ateItens(List.of());
        escolherPrimeiroDisponivel("PRATO", 2);
        escolherPrimeiroDisponivel("ACOMPANHAMENTO", 2);
        escolherPrimeiroDisponivel("SOBREMESA", 1);
    }

    void ateOPagamento() {
        ateOEndereco();
        tela.confirmarEndereco("Rua da Consolação", "930", "", "01302-907", "São Paulo", "SP");
    }

    static Cartao cartaoValido() { return new Cartao("Klaus", "4111 1111 1111 1111", "12/99", "737"); }

    static Cartao cartaoRecusado() { return new Cartao("Klaus", "4000 0000 0000 0002", "12/99", "737"); }

    // ---------- fluxo principal ----------

    @Test
    void fluxoPrincipal_ativaAssinaturaEGravaOsCsvs() throws IOException {
        ateOPagamento();
        assertEquals(Etapa.CARTAO, controlador.getEtapa());
        tela.informarCartao(cartaoValido());

        assertEquals(Etapa.ENCERRADO, controlador.getEtapa());
        assertTrue(controlador.isValidado());
        assertTrue(controlador.isAprovado());
        assertFalse(controlador.isDesistiu());

        Assinatura assinatura = controlador.getAssinatura();
        Pedido pedido = controlador.getPedido();
        assertEquals(Assinatura.ATIVA, assinatura.getStatus());
        assertEquals(Pedido.APROVADO, pedido.getStatus());
        assertEquals(LocalDate.of(2026, 11, 18), pedido.getPrevisaoPrimeiraEntrega());
        assertEquals(1, assinatura.getPagamentos().size());
        assertTrue(saida().contains("Assinatura ativa!"));
        assertTrue(saida().contains("18/11/2026"));
        assertTrue(saida().contains(assinatura.getProtocolo()));

        assertTrue(csv("assinantes").contains(TELEFONE + ";true;"));
        assertTrue(csv("assinaturas").contains(";Básico;149.90;ATIVA"));
        assertEquals(3, linhasDeDados("itens"));
        assertEquals(1, linhasDeDados("pagamentos"));
        assertTrue(csv("pedidos").contains(";APROVADO"));
        assertTrue(csv("entregas").contains(";2026-11-18;"));
        assertEquals(1, controlador.getPedido().getEntregas().size());

        for (String arquivo : List.of("assinantes", "assinaturas", "enderecos", "pedidos", "itens", "pagamentos", "entregas")) {
            assertFalse(csv(arquivo).contains("4111"), "número do cartão não pode ser gravado em " + arquivo);
            assertFalse(csv(arquivo).contains(";737"), "CVV não pode ser gravado em " + arquivo);
        }
    }

    @Test
    void preferenciasFiltramOsProdutosOferecidos() {
        ateItens(List.of("VEGETARIANO", "SEM_LACTOSE"));
        String texto = saida();
        assertTrue(texto.contains("Tofu ao molho shoyu"));
        assertFalse(texto.contains("Frango grelhado"));
        assertFalse(texto.contains("Lasanha de berinjela"), "vegetariana, mas não é sem lactose");
        assertFalse(texto.contains("Escondidinho"), "indisponível");
    }

    @Test
    void selecaoAcimaDoLimiteDoPlanoNaoAvanca() {
        ateItens(List.of());
        Produto prato = produtos.listar().get(0);
        tela.selecionarItens("PRATO", Map.of(prato, 6));
        assertEquals(Etapa.ITENS, controlador.getEtapa());
        assertTrue(saida().contains("Seleção inválida"));
        assertTrue(controlador.getPedido().getItens().isEmpty());

        tela.selecionarItens("PRATO", Map.of(prato, 5));
        assertEquals(1, controlador.getPedido().getItens().size());
    }

    @Test
    void enderecoInvalidoNaoAvanca() {
        ateOEndereco();
        tela.confirmarEndereco("Rua A", "1", "", "123", "São Paulo", "SP");
        assertEquals(Etapa.ENDERECO, controlador.getEtapa());
        assertTrue(saida().contains("Endereço inválido"));
    }

    @Test
    void acaoForaDaEtapaRecebeAviso() {
        tela.informarCodigo("123456");
        assertEquals(Etapa.CELULAR, controlador.getEtapa());
        assertTrue(saida().contains("Ação indisponível nesta etapa"));

        tela.informarCelular("12");
        assertEquals(Etapa.CELULAR, controlador.getEtapa());
        assertTrue(saida().contains("Celular inválido"));
    }

    @Test
    void novoCodigoSubstituiOAnterior() {
        tela.informarCelular(CELULAR);
        String primeiro = notificador.getUltimoCodigo();
        tela.solicitarNovoCodigo();
        String segundo = notificador.getUltimoCodigo();
        if (!primeiro.equals(segundo)) {
            tela.informarCodigo(primeiro);
            assertEquals(Etapa.CODIGO, controlador.getEtapa(), "código antigo deixou de valer");
        }
        tela.informarCodigo(segundo);
        assertEquals(Etapa.PLANO, controlador.getEtapa());
    }

    // ---------- FA1: código inválido 3 vezes ----------

    @Test
    void FA1_tresCodigosInvalidosBloqueiamOCelularInclusiveEmNovaSessao() throws IOException {
        tela.informarCelular(CELULAR);
        tela.informarCodigo("abc");
        assertTrue(saida().contains("Tentativas restantes: 2"));
        tela.informarCodigo("abc");
        assertEquals(Etapa.CODIGO, controlador.getEtapa());
        tela.informarCodigo("abc");

        assertEquals(Etapa.ENCERRADO, controlador.getEtapa());
        assertTrue(controlador.isBloqueado());
        assertFalse(controlador.isValidado());
        assertTrue(saida().contains("bloqueado"));
        assertTrue(csv("assinantes").contains(TELEFONE + ";false;;3;true;"));

        novaSessao();
        tela.informarCelular(CELULAR);
        assertEquals(Etapa.ENCERRADO, controlador.getEtapa());
        assertTrue(controlador.isBloqueado());
        assertNull(notificador.getUltimoCodigo(), "celular bloqueado não recebe SMS");
    }

    @Test
    void FA1_telaEmConsoleEncerraQuandoOCelularEBloqueado() {
        java.io.StringReader teclado = new StringReader(CELULAR + "\nabc\nabc\nabc\n");
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        TelaAssinatura console = new TelaAssinatura(new BufferedReader(teclado), out);
        new ControladorAssinatura(console, new ColecaoAssinantes(RepositorioCsv.padrao()), planos, produtos, new Notificador(out),
                new GatewayOperadoraCartao(new OperadoraCartao()), RELOGIO);
        console.executar();
        assertTrue(saida().contains("bloqueado"));
        assertTrue(saida().contains("Celular (DDD + número):"));
    }

    // ---------- FA2: pagamento recusado ----------

    @Test
    void FA2_pagamentoRecusadoPermiteTentarOutroCartao() throws IOException {
        ateOPagamento();
        tela.informarCartao(cartaoRecusado());

        assertEquals(Etapa.CARTAO, controlador.getEtapa());
        assertFalse(controlador.isAprovado());
        assertTrue(saida().contains("Pagamento recusado"));
        assertEquals(Assinatura.AGUARDANDO_PAGAMENTO, controlador.getAssinatura().getStatus());
        assertEquals(0, linhasDeDados("pagamentos"));

        tela.informarCartao(cartaoValido());
        assertEquals(Etapa.ENCERRADO, controlador.getEtapa());
        assertTrue(controlador.isAprovado());
        assertEquals(Assinatura.ATIVA, controlador.getAssinatura().getStatus());
        assertEquals(1, linhasDeDados("pagamentos"));
    }

    // ---------- FA3: desistência ----------

    @Test
    void FA3_desistenciaEncerraSemAtivarAAssinatura() throws IOException {
        ateOPagamento();
        tela.informarCartao(cartaoRecusado());
        tela.desistirPagamento();

        assertEquals(Etapa.ENCERRADO, controlador.getEtapa());
        assertTrue(controlador.isDesistiu());
        assertFalse(controlador.isAprovado());
        assertEquals(Assinatura.CANCELADA, controlador.getAssinatura().getStatus());
        assertEquals(Pedido.CANCELADO, controlador.getPedido().getStatus());
        assertTrue(saida().contains("Encerrado sem ativação"));
        assertTrue(csv("assinaturas").contains(";CANCELADA"));
        assertTrue(csv("pedidos").contains(";CANCELADO"));
        assertEquals(0, linhasDeDados("pagamentos"));
    }

    @Test
    void desistirForaDaEtapaDePagamentoRecebeAviso() {
        ateOEndereco();
        tela.desistirPagamento();
        assertEquals(Etapa.ENDERECO, controlador.getEtapa());
        assertFalse(controlador.isDesistiu());
    }

    @Test
    void naoGravaArquivosDeAssinaturaAntesDeEscolherUmPlano() throws IOException {
        ateOPlano();
        try (Stream<Path> arquivos = Files.list(pasta)) {
            List<String> nomes = arquivos.map(p -> p.getFileName().toString()).toList();
            assertTrue(nomes.contains("assinantes.csv"));
            assertFalse(nomes.contains("assinaturas.csv"));
        }
    }
}
