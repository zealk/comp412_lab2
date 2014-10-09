package structs;

public class Symbol {
	public String lexeme;
	public boolean isEpsilon;
	public boolean isEOF;
	
	public Symbol(String lexeme) {
		this.lexeme = lexeme;
		if (lexeme.equals("Epsilon") || lexeme.equals("EPSILON") || lexeme.equals("epsilon")) {
			isEpsilon = true;
		} else {
			isEpsilon = false;
		}
		isEOF = false;
	}
	
	/**
	 * EOF init.
	 */
	public Symbol() {
		lexeme = "<EOF>";
		isEpsilon = false;
		isEOF = true;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Symbol) {
			if (isEpsilon) {
				return (((Symbol)o).isEpsilon);
			}
			if (isEOF) {
				return (((Symbol)o).isEOF);
			}
			return lexeme.equals(((Symbol)o).lexeme);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		if (isEpsilon)
			return "Epsilon".hashCode();
		if (isEOF)
			return "<EOF>".hashCode();
		return lexeme.hashCode();
	}
	
	@Override
	public String toString() {
		return lexeme;
	}
	
}
