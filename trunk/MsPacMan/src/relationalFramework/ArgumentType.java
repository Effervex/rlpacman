package relationalFramework;

/**
 * An enum that defines an order of arguments. The enum also defines
 * ordering/ranking of args.
 */
public enum ArgumentType {
	CONST, // A constant ('a')
	NUMBER_CONST, // A numerical constant ('8')
	GOAL_VARIABLE, // A goal variable ('?G_0')
	ACTION_VAR, // An action variable ('?Y')
	NUMBER_RANGE, // A numerical range variable ('?#_5')
	BOUND_VAR, // A bound free variable ('?Bnd_3')
	UNBOUND_VAR, // An unbound free variable ('?Unb_2')
	ANON; // An anonymous variable ('?')
}