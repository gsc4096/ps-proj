package assinatura.externo;

import java.io.PrintStream;

/** Simula o envio de SMS: escreve na saída e guarda o último código enviado (útil nos testes). */
public class Notificador {

    private final PrintStream saida;
    private String ultimoCelular;
    private String ultimoCodigo;

    public Notificador(PrintStream saida) {
        this.saida = saida;
    }

    public void enviarCodigo(String celular, String codigo) {
        this.ultimoCelular = celular;
        this.ultimoCodigo = codigo;
        saida.println("[SMS simulado] Para " + celular + ": seu código de confirmação é " + codigo);
    }

    public String getUltimoCelular() { return ultimoCelular; }
    public String getUltimoCodigo() { return ultimoCodigo; }
}
