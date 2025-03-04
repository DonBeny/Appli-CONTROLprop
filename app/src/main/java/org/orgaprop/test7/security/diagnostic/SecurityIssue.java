package org.orgaprop.test7.security.diagnostic;

import lombok.Value;
import lombok.Builder;
import javax.annotation.Nonnull;

/**
 * Représente un problème de sécurité identifié lors d'un diagnostic.
 * Cette classe est immuable et comparable pour permettre le tri par priorité.
 */
@Value
public class SecurityIssue implements Comparable<SecurityIssue> {
    /** Niveau de sévérité du problème */
    @Nonnull SecuritySeverity severity;
    
    /** Message décrivant le problème */
    @Nonnull String message;
    
    /** Recommandation pour résoudre le problème */
    @Nonnull String recommendation;
    
    /** Priorité de traitement du problème */
    @Nonnull IssuePriority priority;
    
    /** Catégorie de sécurité concernée */
    @Nonnull SecurityCategory category;

    /**
     * Compare deux problèmes de sécurité selon leur priorité.
     * 
     * @param other Le problème à comparer
     * @return un entier négatif, zéro ou positif selon que ce problème est de priorité
     *         inférieure, égale ou supérieure à l'autre
     */
    @Override
    public int compareTo(SecurityIssue other) {
        return Integer.compare(this.priority.getValue(), other.priority.getValue());
    }
}

/**
 * Builder pour créer des instances de SecurityIssue de manière fluide.
 */
@Builder
public static class SecurityIssueBuilder {
    private SecuritySeverity severity;
    private String message;
    private String recommendation;
    private IssuePriority priority;
    private SecurityCategory category;

    /**
     * Configure la sévérité du problème.
     * 
     * @param severity Niveau de sévérité
     * @return this pour chaînage
     */
    public SecurityIssueBuilder withSeverity(SecuritySeverity severity) {
        this.severity = severity;
        return this;
    }

    /**
     * Construit l'instance de SecurityIssue après validation des champs.
     * 
     * @return Une nouvelle instance de SecurityIssue
     * @throws IllegalStateException si un champ obligatoire est manquant
     */
    public SecurityIssue build() {
        validateFields();
        return new SecurityIssue(severity, message, recommendation, priority, category);
    }

    private void validateFields() {
        if (severity == null || message == null || 
            recommendation == null || priority == null || category == null) {
            throw new IllegalStateException("Tous les champs sont obligatoires");
        }
    }
}
