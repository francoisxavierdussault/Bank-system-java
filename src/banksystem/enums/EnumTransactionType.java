package banksystem.enums;

/**
 * Types de transactions bancaires possibles.
 */
public enum EnumTransactionType {

	/** Dépôt d'argent dans un compte. */
	DEPOSIT,

	/** Retrait d'argent d'un compte. */
	WITHDRAWAL,

	/** Virement entre deux comptes. */
	TRANSFER
}
