package assinatura.entidade;

public class Endereco {

    private final String logradouro;
    private final String numero;
    private final String complemento;
    private final String cep;
    private final String cidade;
    private final String uf;

    public Endereco(String logradouro, String numero, String complemento, String cep, String cidade, String uf) {
        this.logradouro = limpar(logradouro);
        this.numero = limpar(numero);
        this.complemento = limpar(complemento);
        this.cep = cep == null ? "" : cep.replaceAll("\\D", "");
        this.cidade = limpar(cidade);
        this.uf = limpar(uf).toUpperCase();
    }

    private static String limpar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    /** Campos obrigatórios preenchidos, CEP com 8 dígitos e UF com 2 letras. */
    public boolean isValido() {
        return !logradouro.isEmpty() && !numero.isEmpty() && !cidade.isEmpty()
                && cep.length() == 8 && uf.matches("[A-Z]{2}");
    }

    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getComplemento() { return complemento; }
    public String getCep() { return cep; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }

    @Override
    public String toString() {
        String comp = complemento.isEmpty() ? "" : " - " + complemento;
        return logradouro + ", " + numero + comp + " - " + cidade + "/" + uf
                + " - CEP " + (cep.length() == 8 ? cep.substring(0, 5) + "-" + cep.substring(5) : cep);
    }
}
