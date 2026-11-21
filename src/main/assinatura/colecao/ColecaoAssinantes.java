package assinatura.colecao;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import assinatura.entidade.Assinante;
import assinatura.persistencia.RepositorioCsv;

/** Conjunto de todos os assinantes: busca por celular e cria quem ainda não existe. */
public class ColecaoAssinantes {

    private final RepositorioCsv repositorio;
    private final Map<String, Assinante> assinantes = new HashMap<>();

    public ColecaoAssinantes(RepositorioCsv repositorio) {
        this.repositorio = repositorio;
    }

    /** Procura na coleção e, se não achar, nos CSVs. */
    public Optional<Assinante> buscar(String telefone) {
        Assinante conhecido = assinantes.get(telefone);
        if (conhecido != null) {
            return Optional.of(conhecido);
        }
        Optional<Assinante> gravado = repositorio.buscarAssinante(telefone);
        gravado.ifPresent(a -> assinantes.put(telefone, a));
        return gravado;
    }

    /** Devolve o assinante do celular (criando-o se for novo), ou vazio se o celular for inválido. */
    public Optional<Assinante> registrar(String celular) {
        Assinante novo = new Assinante();
        if (!novo.registrarCelular(celular)) {
            return Optional.empty();
        }
        Optional<Assinante> existente = buscar(novo.getTelefone());
        if (existente.isPresent()) {
            return existente;
        }
        assinantes.put(novo.getTelefone(), novo);
        return Optional.of(novo);
    }
}
