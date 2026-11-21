package assinatura;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import assinatura.colecao.ColecaoAssinantes;
import assinatura.entidade.Assinante;
import assinatura.entidade.Assinatura;
import assinatura.entidade.Pedido;
import assinatura.entidade.Plano;
import assinatura.persistencia.RepositorioCsv;

/** Regras de projeto cobradas na disciplina. */
class ProjetoRegrasTest {

    @TempDir
    Path pasta;

    @Test
    void controladorNaoCriaInstanciasDeEntidades() throws IOException {
        String codigo = Files.readString(Path.of("src/main/java/assinatura/controle/ControladorAssinatura.java"));
        for (String classe : List.of("Assinante", "Assinatura", "Pedido", "Item", "Pagamento", "Endereco", "Entrega")) {
            assertFalse(codigo.contains("new " + classe + "("), "o controlador não pode instanciar " + classe);
        }
    }

    @Test
    void colecaoDeAssinantesCriaBuscaEReaproveita() {
        RepositorioCsv.usar(pasta);
        ColecaoAssinantes assinantes = new ColecaoAssinantes(RepositorioCsv.padrao());

        assertTrue(assinantes.registrar("123").isEmpty(), "celular inválido");
        Assinante a = assinantes.registrar("(11) 91234-5678").orElseThrow();
        assertSame(a, assinantes.registrar("11912345678").orElseThrow(), "mesmo celular, mesmo objeto");
        assertSame(a, assinantes.buscar("11912345678").orElseThrow());

        a.definirPreferencias(List.of("VEGETARIANO"));
        a.salvar();
        Assinante lido = new ColecaoAssinantes(RepositorioCsv.padrao()).buscar("11912345678").orElseThrow();
        assertEquals(List.of("VEGETARIANO"), lido.getPreferencias());
    }

    @Test
    void cadaObjetoNasceDentroDoSeuDono() {
        RepositorioCsv.usar(pasta);
        Plano plano = new Plano("Básico", 5, new BigDecimal("149.90"), "SEMANAL", true);
        Assinante assinante = new Assinante();
        assinante.registrarCelular("11912345678");

        Assinatura assinatura = assinante.contratar(plano);       // Assinante cria Assinatura
        Pedido pedido = assinatura.gerarPedido();                  // Assinatura cria Pedido
        assertTrue(assinatura.aguardarPagamento("Rua A", "1", "", "01302907", "São Paulo", "SP")); // cria Endereco
        assinatura.confirmar("AUT-1", pedido, java.time.LocalDateTime.of(2026, 11, 16, 12, 0));    // cria Pagamento

        assertSame(assinatura, assinante.getAssinaturas().get(0));
        assertSame(pedido, assinatura.getPedidos().get(0));
        assertNotNull(assinatura.getEndereco());
        assertEquals(1, assinatura.getPagamentos().size());
        assertEquals(1, pedido.getEntregas().size());              // Pedido cria Entrega
    }
}
