package relationalFramework;

/**
 * An enum that defines an order of arguments. The enum also defines
 * ordering/ranking of args.
 */
public enum ArgumentType {
	CONST, // A constant ('a')
	NUMBER_CONST, // A numerical constant ('8')
	GOAL_VARIABLE, // A goal variable ('?G_0')
	ACTION_VAR, // An action variable ('?A')
	NUMBER_RANGE, // A numerical range variable ('?#_5')
	NON_ACTION, // A non-action variable
	ANON; // An anonymous variable ('?')
}