package com.ges.boutique.journalaudit;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JournalAuditDto {
    private Long id;
    private Long utilisateurId;
    private String utilisateurNom;
    private TypeActionAudit action;
    private String details;
    private LocalDateTime dateAction;
}
