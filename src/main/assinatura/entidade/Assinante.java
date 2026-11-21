package assinatura.entidade;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import assinatura.persistencia.RepositorioCsv;

public class Assinante {

    public static final int MAX_TENTATIVAS = 3;
    public static final String OK = "OK";
    public static final String INVALIDO = "INVALIDO";
    public static final String BLOQUEADO = "BLOQUEADO";

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private String telefone;
    private boolean celularValidado;
    private String codigoAtual;
    private int tentativasCodigo;
    private boolean codigoBloqueado;
    private List<String> preferencias = new ArrayList<>();
    private final List<Assinatura> assinaturas = new ArrayList<>();

    /** Reconstrói um assinante gravado no CSV. */
    public static Assinante restaurar(String telefone, boolean celularValidado, String codigoAtual,
                                      int tentativasCodigo, boolean codigoBloqueado, Collection<String> preferencias) {
        Assinante a = new Assinante();
        a.telefone = telefone;
        a.celularValidado = celularValidado;
        a.codigoAtual = (codigoAtual == null || codigoAtual.isBlank()) ? null : codigoAtual;
        a.tentativasCodigo = tentativasCodigo;
        a.codigoBloqueado = codigoBloqueado;
        a.preferencias = Normalizacao.lista(preferencias);
        return a;
    }

    /** Aceita DDD + número (10 ou 11 dígitos), ignorando máscara. */
    public boolean registrarCelular(String celular) {
        String digitos = celular == null ? "" : celular.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 11) {
            return false;
        }
        this.telefone = digitos;
        return true;
    }

    public String emitirCodigo() {
        if (codigoBloqueado) {
            throw new IllegalStateException("Celular bloqueado: não é possível emitir novo código.");
        }
        codigoAtual = String.format("%06d", ALEATORIO.nextInt(1_000_000));
        return codigoAtual;
    }

    /** Retorna OK, INVALIDO ou BLOQUEADO. O terceiro erro seguido bloqueia o celular. */
    public String validarCodigo(String codigo) {
        if (codigoBloqueado) {
            return BLOQUEADO;
        }
        if (codigoAtual != null && codigo != null && codigoAtual.equals(codigo.trim())) {
            celularValidado = true;
            tentativasCodigo = 0;
            codigoAtual = null;
            return OK;
        }
        tentativasCodigo++;
        if (tentativasCodigo >= MAX_TENTATIVAS) {
            codigoBloqueado = true;
            codigoAtual = null;
            return BLOQUEADO;
        }
        return INVALIDO;
    }

    /** O assinante cria a própria assinatura. */
    public Assinatura contratar(Plano plano) {
        Assinatura assinatura = new Assinatura();
        assinatura.criar(this, plano);
        return assinatura;
    }

    public void definirPreferencias(List<String> preferencias) {
        this.preferencias = Normalizacao.lista(preferencias);
    }

    public void salvar() {
        RepositorioCsv.padrao().salvar(this);
    }

    void adicionarAssinatura(Assinatura assinatura) {
        assinaturas.add(assinatura);
    }

    public int tentativasRestantes() { return Math.max(0, MAX_TENTATIVAS - tentativasCodigo); }
    public String getTelefone() { return telefone; }
    public boolean isCelularValidado() { return celularValidado; }
    public String getCodigoAtual() { return codigoAtual; }
    public int getTentativasCodigo() { return tentativasCodigo; }
    public boolean isCodigoBloqueado() { return codigoBloqueado; }
    public List<String> getPreferencias() { return Collections.unmodifiableList(preferencias); }
    public List<Assinatura> getAssinaturas() { return Collections.unmodifiableList(assinaturas); }
}
