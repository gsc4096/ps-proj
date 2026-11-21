package assinatura;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import assinatura.entidade.Cartao;
import assinatura.externo.GatewayOperadoraCartao;
import assinatura.externo.OperadoraCartao;

class OperadoraCartaoTest {

    private final OperadoraCartao operadora = new OperadoraCartao();
    private final BigDecimal valor = new BigDecimal("149.90");

    @Test
    void aprovaCartaoValido() {
        String codigo = operadora.autorizar(new Cartao("Klaus", "4111 1111 1111 1111", "12/99", "123"), valor);
        assertTrue(codigo.startsWith("AUT-"));
    }

    @Test
    void recusaCartaoDeTesteSemLimite() {
        assertEquals("", operadora.autorizar(new Cartao("Klaus", "4000000000000002", "12/99", "123"), valor));
    }

    @Test
    void recusaNumeroInvalidoCvvInvalidoOuVencido() {
        assertEquals("", operadora.autorizar(new Cartao("K", "4111111111111112", "12/99", "123"), valor));
        assertEquals("", operadora.autorizar(new Cartao("K", "4111111111111111", "12/99", "12"), valor));
        assertEquals("", operadora.autorizar(new Cartao("K", "4111111111111111", "01/20", "123"), valor));
        assertEquals("", operadora.autorizar(new Cartao("K", "4111111111111111", "13/99", "123"), valor));
        assertEquals("", operadora.autorizar(null, valor));
    }

    @Test
    void gatewayTransformaFalhaEmRecusa() {
        OperadoraCartao quebrada = new OperadoraCartao() {
            @Override
            public String autorizar(Cartao cartao, BigDecimal valor) {
                throw new RuntimeException("fora do ar");
            }
        };
        assertEquals("", new GatewayOperadoraCartao(quebrada).autorizar(new Cartao("K", "4111111111111111", "12/99", "123"), valor));
    }
}
