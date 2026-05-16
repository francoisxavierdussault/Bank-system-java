package banksystem.enums;

/**
 * États du cycle de vie d'un compte bancaire.
 */
public enum EnumAccountStatus {
    /** Opérations normales — dépôts, retraits et virements autorisés. */
    ACTIVE,
    /** Temporairement bloqué — aucune transaction jusqu'à la levée par un gestionnaire. */
    FROZEN,
    /** Fermé définitivement — aucune transaction autorisée. */
    CLOSED
}
