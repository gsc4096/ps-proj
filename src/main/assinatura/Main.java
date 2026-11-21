package assinatura;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;

import assinatura.colecao.ColecaoAssinantes;
import assinatura.colecao.ColecaoPlanos;
import assinatura.colecao.ColecaoProdutos;
import assinatura.controle.ControladorAssinatura;
import assinatura.externo.GatewayOperadoraCartao;
import assinatura.externo.Notificador;
import assinatura.externo.OperadoraCartao;
import assinatura.fronteira.TelaAssinatura;
import assinatura.persistencia.RepositorioCsv;

/** Uso: java -cp target/classes assinatura.Main [pasta-dos-csv]  (padrão: ./dados) */
public class Main {

    public static void main(String[] args) {
        Path pasta = Path.of(args.length > 0 ? args[0] : "dados");
        RepositorioCsv.usar(pasta);
        RepositorioCsv repositorio = RepositorioCsv.padrao();

        PrintStream saida = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        TelaAssinatura tela = new TelaAssinatura(entrada, saida);
        new ControladorAssinatura(
                tela,
                new ColecaoAssinantes(repositorio),
                new ColecaoPlanos(repositorio.carregarPlanos()),
                new ColecaoProdutos(repositorio.carregarProdutos()),
                new Notificador(saida),
                new GatewayOperadoraCartao(new OperadoraCartao()),
                Clock.systemDefaultZone());
        tela.executar();
    }
}
