package it.cnr.ilc.maia.model;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "workspaces")
public class Workspace extends TracedEntity {

    public static final String TABLE_NAME = "workspaces";
    public static final String GENERATOR_NAME = TABLE_NAME + "_generator";
    public static final String SEQUENCE_NAME = TABLE_NAME + "_id_seq";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = GENERATOR_NAME)
    @SequenceGenerator(name = GENERATOR_NAME, sequenceName = SEQUENCE_NAME, allocationSize = 1)
    private Long id;

    @NonNull
    private String name;

    private String note;

    @Column(columnDefinition = "TEXT")
    private String layout;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "workspace")
    private List<Tile> tiles;

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", note='" + note + '\''
                + ", layout='" + layout + '\''
                + ", created=" + super.getCreated()
                + ", updated=" + super.getUpdated()
                + ", createdBy=" + super.getCreatedBy()
                + ", updatedBy=" + super.getUpdatedBy()
                + '}';
    }
}
