package assinatura.entidade;

import java.time.LocalDate;

/** Entrega programada de um pedido aprovado (data prevista e, depois, data em que foi feita). */
public class Entrega {

    private LocalDate dataPrevista;
    private LocalDate dataEntregue;

    public void criar(LocalDate dataPrevista) {
        this.dataPrevista = dataPrevista;
    }

    public void registrarEntrega(LocalDate data) {
        this.dataEntregue = data;
    }

    public LocalDate getDataPrevista() { return dataPrevista; }
    public LocalDate getDataEntregue() { return dataEntregue; }
}
