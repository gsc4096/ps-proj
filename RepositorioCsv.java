package assinatura.persistencia;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import assinatura.entidade.Assinante;
import assinatura.entidade.Assinatura;
import assinatura.entidade.Endereco;
import assinatura.entidade.Entrega;
import assinatura.entidade.Item;
import assinatura.entidade.Pagamento;
import assinatura.entidade.Pedido;
import assinatura.entidade.Plano;
import assinatura.entidade.Produto;

/**
 * Persistência em arquivos CSV (separador ";", UTF-8, primeira linha = cabeçalho).
 * Entrada: planos.csv e produtos.csv. Gerados: assinantes, assinaturas, enderecos, pedidos,
 * itens e pagamentos. Dados de cartão nunca são gravados.
 */
public class RepositorioCsv {

    private static RepositorioCsv instancia;

    private final Path diretorio;

    public RepositorioCsv(Path diretorio) {
        this.diretorio = diretorio;
        try {
            Files.createDirectories(diretorio);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static synchronized RepositorioCsv padrao() {
        if (instancia == null) {
            instancia = new RepositorioCsv(Path.of("dados"));
        }
        return instancia;
    }

    public static synchronized void usar(Path diretorio) {
        instancia = new RepositorioCsv(diretorio);
    }

    public Path getDiretorio() { return diretorio; }

    // ---------- leitura dos dados de entrada ----------

    public synchronized List<Plano> carregarPlanos() {
        List<Plano> planos = new ArrayList<>();
        for (List<String> r : ler("planos.csv")) {
            planos.add(new Plano(col(r, 0), Integer.parseInt(col(r, 1)), new BigDecimal(col(r, 2)),
                    col(r, 3), Boolean.parseBoolean(col(r, 4))));
        }
        return planos;
    }

    public synchronized List<Produto> carregarProdutos() {
        List<Produto> produtos = new ArrayList<>();
        for (List<String> r : ler("produtos.csv")) {
            produtos.add(new Produto(col(r, 0), col(r, 1), Boolean.parseBoolean(col(r, 2)), dividir(col(r, 3))));
        }
        return produtos;
    }

    public synchronized Optional<Assinante> buscarAssinante(String telefone) {
        for (List<String> r : ler("assinantes.csv")) {
            if (col(r, 0).equals(telefone)) {
                return Optional.of(Assinante.restaurar(col(r, 0), Boolean.parseBoolean(col(r, 1)), col(r, 2),
                        Integer.parseInt(col(r, 3)), Boolean.parseBoolean(col(r, 4)), dividir(col(r, 5))));
            }
        }
        return Optional.empty();
    }

    // ---------- gravação ----------

    public synchronized void salvar(Assinante a) {
        if (a.getTelefone() == null) {
            throw new IllegalStateException("Assinante sem celular registrado.");
        }
        substituir("assinantes.csv",
                "telefone;celularValidado;codigoAtual;tentativasCodigo;codigoBloqueado;preferencias",
                a.getTelefone(),
                List.of(List.of(a.getTelefone(), String.valueOf(a.isCelularValidado()),
                        a.getCodigoAtual() == null ? "" : a.getCodigoAtual(),
                        String.valueOf(a.getTentativasCodigo()), String.valueOf(a.isCodigoBloqueado()),
                        String.join("|", a.getPreferencias()))));
    }

    public synchronized void salvar(Assinatura s) {
        String protocolo = s.getProtocolo();

        substituir("assinaturas.csv", "protocolo;telefone;plano;valor;status", protocolo,
                List.of(List.of(protocolo, s.getAssinante().getTelefone(), s.getPlano().getNome(),
                        s.getValor().toPlainString(), s.getStatus())));

        List<List<String>> enderecos = new ArrayList<>();
        Endereco e = s.getEndereco();
        if (e != null) {
            enderecos.add(List.of(protocolo, e.getLogradouro(), e.getNumero(), e.getComplemento(),
                    e.getCep(), e.getCidade(), e.getUf()));
        }
        substituir("enderecos.csv", "protocolo;logradouro;numero;complemento;cep;cidade;uf", protocolo, enderecos);

        List<List<String>> pedidos = new ArrayList<>();
        List<List<String>> itens = new ArrayList<>();
        List<List<String>> entregas = new ArrayList<>();
        for (Pedido p : s.getPedidos()) {
            pedidos.add(List.of(protocolo, p.getId(), p.getStatus()));
            for (Entrega en : p.getEntregas()) {
                entregas.add(List.of(protocolo, p.getId(), en.getDataPrevista().toString(),
                        en.getDataEntregue() == null ? "" : en.getDataEntregue().toString()));
            }
            for (Item i : p.getItens()) {
                itens.add(List.of(protocolo, p.getId(), i.getProduto().getNome(),
                        i.getProduto().getCategoria(), String.valueOf(i.getQtde())));
            }
        }
        substituir("pedidos.csv", "protocolo;pedidoId;status", protocolo, pedidos);
        substituir("entregas.csv", "protocolo;pedidoId;dataPrevista;dataEntregue", protocolo, entregas);
        substituir("itens.csv", "protocolo;pedidoId;produto;categoria;qtde", protocolo, itens);

        List<List<String>> pagamentos = new ArrayList<>();
        for (Pagamento p : s.getPagamentos()) {
            pagamentos.add(List.of(protocolo, p.getValor().toPlainString(), p.getData().toString(),
                    p.getCodigoAutorizacao()));
        }
        substituir("pagamentos.csv", "protocolo;valor;data;codigoAutorizacao", protocolo, pagamentos);
    }

    // ---------- utilitários de CSV ----------

    /** Remove as linhas cuja 1ª coluna é a chave e acrescenta as novas (upsert por chave). */
    private void substituir(String arquivo, String cabecalho, String chave, List<List<String>> novas) {
        List<List<String>> linhas = ler(arquivo);
        linhas.removeIf(r -> col(r, 0).equals(chave));
        linhas.addAll(novas);
        escrever(arquivo, cabecalho, linhas);
    }

    private List<List<String>> ler(String arquivo) {
        Path caminho = diretorio.resolve(arquivo);
        List<List<String>> linhas = new ArrayList<>();
        if (!Files.exists(caminho)) {
            return linhas;
        }
        try {
            List<String> brutas = Files.readAllLines(caminho, StandardCharsets.UTF_8);
            for (int i = 1; i < brutas.size(); i++) {
                if (!brutas.get(i).isBlank()) {
                    linhas.add(dividirLinha(brutas.get(i)));
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return linhas;
    }

    private void escrever(String arquivo, String cabecalho, List<List<String>> linhas) {
        List<String> saida = new ArrayList<>();
        saida.add(cabecalho);
        for (List<String> r : linhas) {
            saida.add(String.join(";", r.stream().map(RepositorioCsv::escapar).toList()));
        }
        try {
            Path destino = diretorio.resolve(arquivo);
            Path temporario = diretorio.resolve(arquivo + ".tmp");
            Files.write(temporario, saida, StandardCharsets.UTF_8);
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    static String escapar(String valor) {
        String v = valor == null ? "" : valor.replace("\r", " ").replace("\n", " ");
        if (v.contains(";") || v.contains("\"")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    static List<String> dividirLinha(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas) {
                if (c == '"' && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else if (c == '"') {
                    entreAspas = false;
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == ';') {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos;
    }

    private static String col(List<String> linha, int indice) {
        return indice < linha.size() ? linha.get(indice).trim() : "";
    }

    private static List<String> dividir(String valor) {
        return valor.isBlank() ? List.of() : Arrays.asList(valor.split("\\|"));
    }
}
