package com.ges.boutique.transfert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_transferts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueTransfert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfert_id", nullable = false)
    @JsonIgnore
    private TransfertStock transfert;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "effectue_par")
    private String effectuePar;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    public static HistoriqueTransfert creer(TransfertStock t, String action, String description, String user) {
        HistoriqueTransfert h = new HistoriqueTransfert();
        h.transfert = t;
        h.action = action;
        h.description = description;
        h.effectuePar = user;
        h.dateAction = LocalDateTime.now();
        return h;
    }
}
