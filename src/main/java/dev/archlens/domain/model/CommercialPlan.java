package dev.archlens.domain.model;

public enum CommercialPlan {

    /** Avaliação / piloto simbólico — 1 diagnóstico */
    PILOT(1, 1, 50, "Piloto"),

    /** Pacote diagnóstico padrão (R$ 8k–18k) */
    DIAGNOSTICO(3, 5, 200, "Diagnóstico"),

    /** Auditoria de portfólio (R$ 25k–40k) */
    PORTFOLIO(10, 20, 500, "Portfólio"),

    /** Uso interno do consultor — sem limite comercial */
    INTERNO(-1, -1, 500, "Interno");

    private final int maxProjects;
    private final int maxAnalysesPerMonth;
    private final int maxUploadMb;
    private final String displayName;

    CommercialPlan(int maxProjects, int maxAnalysesPerMonth, int maxUploadMb, String displayName) {
        this.maxProjects = maxProjects;
        this.maxAnalysesPerMonth = maxAnalysesPerMonth;
        this.maxUploadMb = maxUploadMb;
        this.displayName = displayName;
    }

    public int maxProjects() {
        return maxProjects;
    }

    public int maxAnalysesPerMonth() {
        return maxAnalysesPerMonth;
    }

    public int maxUploadMb() {
        return maxUploadMb;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isUnlimitedProjects() {
        return maxProjects < 0;
    }

    public boolean isUnlimitedAnalyses() {
        return maxAnalysesPerMonth < 0;
    }
}
