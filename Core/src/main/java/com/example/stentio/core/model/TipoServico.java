import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Getter//lombok para nao precisa settar e dar get
@Setter//
@Table(name = "tipos_servico")
public class TipoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativo = true;


}