package assinatura.entidade;

/** Dados do cartão só existem em memória: nunca são gravados nos CSVs. */
public class Cartao {

    private final String titular;
    private final String numero;
    private final String validade;
    private final String cvv;

    public Cartao(String titular, String numero, String validade, String cvv) {
        this.titular = titular == null ? "" : titular.trim();
        this.numero = numero == null ? "" : numero.replaceAll("\\D", "");
        this.validade = validade == null ? "" : validade.trim();
        this.cvv = cvv == null ? "" : cvv.trim();
    }

    public String getTitular() { return titular; }
    public String getNumero() { return numero; }
    public String getValidade() { return validade; }
    public String getCvv() { return cvv; }

    public String numeroMascarado() {
        return numero.length() < 4 ? "****" : "**** **** **** " + numero.substring(numero.length() - 4);
    }
}
