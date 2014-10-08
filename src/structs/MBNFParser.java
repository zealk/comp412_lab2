package structs;

public class MBNFParser {
	
	public static String SEMICOLON = ";";
	public static String DERIVES = ":";
	public static String ALSODERIVES = "\\|";
	public static String EPSILON = "(?:EPSILON|epsilon|Epsilon)";
	public static String SYMBOL = "[A-Za-z0-9]+";
	public static String SP = "\\s+";
	public static String SPP = "\\s*";
	
	public static String SymbolList = SYMBOL +"(?:"+ SP + SYMBOL + ")*";
	public static String RightHandSide = "(?:" + EPSILON + "|" + SymbolList + ")";
	public static String ProductionSet = SYMBOL + SPP + DERIVES + SPP + RightHandSide 
			+ "(?:"+ SPP + ALSODERIVES + SPP + RightHandSide + ")*";
	public static String ProductionList = SPP + "(?:" + "(" + ProductionSet + ")" + SPP + SEMICOLON + SPP + ")";
	
	//Divide Production set into 2 parts: Symbol & ( RH | RH | RH ...)
	public static String ProductionSet_S = "(" + SYMBOL + ")" + SPP + DERIVES + SPP + "(" +RightHandSide 
			+ "(?:"+ SPP + ALSODERIVES + SPP + RightHandSide + ")*)";
	
}
