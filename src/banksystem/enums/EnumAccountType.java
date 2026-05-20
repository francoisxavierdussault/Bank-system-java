package banksystem.enums;

/**
 * Types de comptes bancaires disponibles.
 */
public enum EnumAccountType {

	/** Compte épargne avec taux d'intérêt. */
	SAVINGS,

	/** Compte chèque avec transactions mensuelles gratuites. */
	CHECKING,

	/** Compte crédit avec limite de crédit. */
	CREDIT,

	/** Compte devise — gèle des dollars américains pour une durée fixe. */
	CURRENCY,

	/** Marge de crédit renouvelable — prélèvement et remboursement flexibles. */
	LINE_OF_CREDIT
}
