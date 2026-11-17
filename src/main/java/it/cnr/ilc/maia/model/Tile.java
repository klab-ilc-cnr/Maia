package it.cnr.ilc.maia.model;

import lombok.*;
import jakarta.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "tiles")
public class Tile extends TracedEntity {

    public static final String TABLE_NAME = "tiles";
    public static final String GENERATOR_NAME = TABLE_NAME + "_generator";
    public static final String SEQUENCE_NAME = TABLE_NAME + "_id_seq";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = GENERATOR_NAME)
    @SequenceGenerator(name = GENERATOR_NAME, sequenceName = SEQUENCE_NAME, allocationSize = 1)
    private Long id;

    @Column(columnDefinition = "TEXT")
    String tileConfig;

    String contentId;

    @NonNull
    @Enumerated(EnumType.STRING)
    TileType type;

    @NonNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    Workspace workspace;

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", name='" + tileConfig + '\''
                + ", note='" + contentId + '\''
                + ", layout='" + type + '\''
                + ", created=" + super.getCreated()
                + ", updated=" + super.getUpdated()
                + ", createdBy=" + super.getCreatedBy()
                + ", updatedBy=" + super.getUpdatedBy()
                + '}';
    }
}
