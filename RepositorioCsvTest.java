package assinatura;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import assinatura.entidade.Assinante;
import assinatura.entidade.Assinatura;
import assinatura.entidade.Pedido;
import assinatura.entidade.Plano;
import assinatura.entidade.Produto;
import assinatura.persistencia.RepositorioCsv;

class RepositorioCsvTest {

    @TempDir
    Path pasta;

    RepositorioCsv repo;

    @BeforeEach
    void preparar() throws IOException {
        Files.copy(Path.of("dados/planos.csv"), pasta.resolve("planos.csv"));
        Files.copy(Path.of("dados/produtos.csv"), pasta.resolve("produtos.csv"));
        RepositorioCsv.usar(pasta);
        repo = RepositorioCsv.padrao();
    }

    @Test
    void carregaPlanosEProdutosDosCsvs() {
        List<Plano> planos = repo.carregarPlanos();
        assertEquals(4, planos.size());
        assertEquals("Básico", planos.get(0).getNome());
        assertEquals(new BigDecimal("149.90"), planos.get(0).getValor());
        assertFalse(planos.get(3).isAtivo());

        List<Produto> produtos = repo.carregarProdutos();
        assertEquals(17, produtos.size());
        Produto risoto = produtos.stream().filter(p -> p.getNome().equals("Risoto de cogumelos")).findFirst().orElseThrow();
        assertEquals(List.of("VEGETARIANO", "SEM_GLUTEN"), risoto.getPreferenciasAtendidas());
        assertTrue(produtos.stream().anyMatch(p -> p.getNome().equals("Strogonoff de carne") && p.getPreferenciasAtendidas().isEmpty()));
    }

    @Test
    void assinanteGravadoPodeSerBuscadoPeloTelefone() {
        Assinante a = new Assinante();
        a.registrarCelular("11912345678");
        a.emitirCodigo();
        a.validarCodigo("errado");
        a.definirPreferencias(List.of("VEGETARIANO", "SEM_LACTOSE"));
        a.salvar();

        Assinante lido = repo.buscarAssinante("11912345678").orElseThrow();
        assertEquals(1, lido.getTentativasCodigo());
        assertEquals(a.getCodigoAtual(), lido.getCodigoAtual());
        assertEquals(List.of("VEGETARIANO", "SEM_LACTOSE"), lido.getPreferencias());
        assertTrue(repo.buscarAssinante("11000000000").isEmpty());
    }

    @Test
    void salvarDeNovoAtualizaEmVezDeDuplicar() throws IOException {
        Assinante a = new Assinante();
        a.registrarCelular("11912345678");
        a.salvar();
        a.definirPreferencias(List.of("SEM_GLUTEN"));
        a.salvar();
        assertEquals(2, Files.readAllLines(pasta.resolve("assinantes.csv")).size(), "cabeçalho + 1 linha");
    }

    @Test
    void assinaturaGravaTodasAsTabelasSemDadosDeCartao() throws IOException {
        Assinante assinante = new Assinante();
        assinante.registrarCelular("11912345678");
        Plano plano = repo.carregarPlanos().get(0);
        Assinatura assinatura = assinante.contratar(plano);
        Pedido pedido = assinatura.gerarPedido();
        Produto prato = repo.carregarProdutos().get(0);
        pedido.substituirItens("PRATO", Map.of(prato, 2));
        assertTrue(assinatura.aguardarPagamento("Rua \"A\"; Centro", "10", "ap 3", "01302907", "São Paulo", "SP"));
        LocalDateTime agora = LocalDateTime.of(2026, 11, 16, 12, 0);
        assinatura.confirmar("AUT-123", pedido, agora);

        String protocolo = assinatura.getProtocolo();
        for (String arquivo : List.of("assinaturas", "enderecos", "pedidos", "itens", "pagamentos", "entregas")) {
            List<String> linhas = Files.readAllLines(pasta.resolve(arquivo + ".csv"));
            assertEquals(2, linhas.size(), arquivo + ".csv deve ter cabeçalho + 1 linha");
            assertTrue(linhas.get(1).startsWith(protocolo + ";"), arquivo);
        }
        assertTrue(Files.readString(pasta.resolve("assinaturas.csv")).contains(";ATIVA"));
        assertTrue(Files.readString(pasta.resolve("pedidos.csv")).contains(";APROVADO"));
        assertTrue(Files.readString(pasta.resolve("entregas.csv")).contains(";2026-11-18;"));
        assertTrue(Files.readString(pasta.resolve("enderecos.csv")).contains("\"Rua \"\"A\"\"; Centro\""),
                "campo com ; e aspas é escapado");
        assertTrue(Files.readString(pasta.resolve("itens.csv")).contains(";Frango grelhado;PRATO;2"));
        assertTrue(Files.readString(pasta.resolve("pagamentos.csv")).contains(";149.90;2026-11-16T12:00;AUT-123"));
    }
}
