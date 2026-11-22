package assinatura;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import assinatura.colecao.ColecaoPlanos;
import assinatura.entidade.Assinante;
import assinatura.entidade.Assinatura;
import assinatura.entidade.Endereco;
import assinatura.entidade.Pedido;
import assinatura.entidade.Plano;
import assinatura.entidade.Produto;

class EntidadesTest {

    private final Plano basico = new Plano("Basico", 5, new BigDecimal("149.90"), "SEMANAL", true);

    @Test
    void celularAceitaMascaraEExigeDddMaisNumero() {
        Assinante a = new Assinante();
        assertTrue(a.registrarCelular("(11) 91234-5678"));
        assertEquals("11912345678", a.getTelefone());
        assertFalse(new Assinante().registrarCelular("1234"));
        assertFalse(new Assinante().registrarCelular(null));
    }

    @Test
    void codigoTemSeisDigitosEValidaUmaVez() {
        Assinante a = new Assinante();
        a.registrarCelular("11912345678");
        String codigo = a.emitirCodigo();
        assertTrue(codigo.matches("\\d{6}"));
        assertEquals(Assinante.OK, a.validarCodigo(codigo));
        assertTrue(a.isCelularValidado());
        assertEquals(Assinante.INVALIDO, a.validarCodigo(codigo), "codigo ja foi consumido");
    }

    @Test
    void terceiroCodigoErradoBloqueiaOCelular() {
        Assinante a = new Assinante();
        a.registrarCelular("11912345678");
        a.emitirCodigo();
        assertEquals(Assinante.INVALIDO, a.validarCodigo("000000x"));
        assertEquals(2, a.tentativasRestantes());
        assertEquals(Assinante.INVALIDO, a.validarCodigo("111111x"));
        assertEquals(Assinante.BLOQUEADO, a.validarCodigo("222222x"));
        assertTrue(a.isCodigoBloqueado());
        assertEquals(Assinante.BLOQUEADO, a.validarCodigo("qualquer"));
        assertThrows(IllegalStateException.class, a::emitirCodigo);
    }

    @Test
    void limiteDoPlano() {
        assertTrue(basico.permiteQuantidade(1));
        assertTrue(basico.permiteQuantidade(5));
        assertFalse(basico.permiteQuantidade(6));
        assertFalse(basico.permiteQuantidade(0));
    }

    @Test
    void previsaoDeEntregaSomaDoisDiasEPulaDomingo() {
        // quarta + 2 = sexta
        assertEquals(LocalDate.of(2026, 11, 20), basico.preverPrimeiraEntrega(LocalDateTime.of(2026, 11, 18, 10, 0)));
        // sexta (13/11/2026) + 2 = domingo -> segunda
        assertEquals(LocalDate.of(2026, 11, 16), basico.preverPrimeiraEntrega(LocalDateTime.of(2026, 11, 13, 10, 0)));
    }

    @Test
    void filtroPorPreferenciaEDisponibilidade() {
        Produto risoto = new Produto("Risoto", "prato", true, List.of("VEGETARIANO", "SEM_GLUTEN"));
        Produto lasanha = new Produto("Lasanha", "PRATO", true, List.of("VEGETARIANO"));
        Produto esgotado = new Produto("Esgotado", "PRATO", false, List.of("VEGETARIANO"));

        assertTrue(risoto.atendeSelecao("PRATO", List.of("VEGETARIANO", "SEM_GLUTEN")));
        assertTrue(risoto.atendeSelecao("prato", List.of()));
        assertFalse(lasanha.atendeSelecao("PRATO", List.of("SEM_GLUTEN")));
        assertFalse(esgotado.atendeSelecao("PRATO", List.of("VEGETARIANO")));
        assertFalse(risoto.atendeSelecao("SOBREMESA", List.of()));
    }

    @Test
    void colecaoDePlanosListaSoOsAtivos() {
        Plano legado = new Plano("Legado", 8, new BigDecimal("199.90"), "SEMANAL", false);
        assertEquals(List.of(basico), new ColecaoPlanos(List.of(basico, legado)).listar());
    }

    @Test
    void enderecoValidaCamposObrigatorios() {
        assertTrue(new Endereco("Rua A", "10", "", "01302-907", "São Paulo", "sp").isValido());
        assertFalse(new Endereco("Rua A", "10", "", "0130", "São Paulo", "SP").isValido());
        assertFalse(new Endereco("", "10", "", "01302907", "São Paulo", "SP").isValido());
        assertFalse(new Endereco("Rua A", "10", "", "01302907", "São Paulo", "S").isValido());
    }

    @Test
    void pedidoValidaLimiteEPreferenciasEsubstituiItensDaCategoria() {
        Assinante assinante = new Assinante();
        assinante.registrarCelular("11912345678");
        assinante.definirPreferencias(List.of("vegetariano"));
        Assinatura assinatura = new Assinatura();
        assinatura.criar(assinante, basico);
        Pedido pedido = new Pedido();
        pedido.criar(assinatura);

        Produto lasanha = new Produto("Lasanha", "PRATO", true, List.of("VEGETARIANO"));
        Produto strogonoff = new Produto("Strogonoff", "PRATO", true, List.of());
        Produto tofu = new Produto("Tofu", "PRATO", true, List.of("VEGETARIANO"));

        assertTrue(pedido.validarSelecao("PRATO", Map.of(lasanha, 5), basico));
        assertFalse(pedido.validarSelecao("PRATO", Map.of(lasanha, 6), basico), "passou do limite");
        assertFalse(pedido.validarSelecao("PRATO", Map.of(strogonoff, 1), basico), "nao atende a preferencia");
        assertFalse(pedido.validarSelecao("PRATO", Map.of(lasanha, 0), basico), "quantidade zero");
        assertTrue(pedido.validarSelecao("PRATO", Map.of(), basico), "pular categoria eh valido");

        pedido.substituirItens("PRATO", Map.of(lasanha, 3));
        assertEquals(1, pedido.getItens().size());
        Map<Produto, Integer> nova = new LinkedHashMap<>();
        nova.put(tofu, 2);
        pedido.substituirItens("PRATO", nova);
        assertEquals(1, pedido.getItens().size());
        assertEquals("Tofu", pedido.getItens().get(0).getProduto().getNome());
    }

    @Test
    void assinaturaCriadaTemProtocoloEValorDoPlano() {
        Assinante assinante = new Assinante();
        assinante.registrarCelular("11912345678");
        Assinatura assinatura = new Assinatura();
        assinatura.criar(assinante, basico);
        assertTrue(assinatura.getProtocolo().startsWith("ASS-"));
        assertEquals(new BigDecimal("149.90"), assinatura.getValor());
        assertEquals(Assinatura.CRIADA, assinatura.getStatus());
        assertEquals(1, assinante.getAssinaturas().size());
    }
}
