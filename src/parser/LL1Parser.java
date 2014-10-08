package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import structs.MBNFParser;
import structs.Symbol;

public class LL1Parser {
	
	/** Store the production relations */
	Map<Symbol,List<List<Symbol>>> p;
	/** Store all the terminal symbols */
	Set<Symbol> ts;
	/** Store all the non-terminal symbols*/
	Set<Symbol> nts;
	
	Map<Symbol,Set<Symbol>> first;
	Map<Symbol,Set<Symbol>> follow;
	Map<Symbol,Set<Symbol>> firstp;
	
	private static void help() {
		System.out.println("Parameters:");
		System.out.println("-h : help");
		System.out.println("-t : print parse table in YAML to std out");
		System.out.println("-s : print human readable :");
		System.out.println("	Productions,");
		System.out.println("	First sets,");
		System.out.println("	Follow sets,");
		System.out.println("	First+ sets,");
		System.out.println("-r : remove left recursive");
	}

	public static void main(String[] args) {
		for (String arg:args) {
			if (arg.equals("-h")) {
				help();
				return;
			}
		}
		LL1Parser llp = new LL1Parser();
		if (llp.init(args) > 0) {
			return;
		}
		
		for (String arg:args) {
			if (arg.equals("-r")) {
				llp.remove_leftrecursive();
			}
		}
		for (String arg:args) {
			if (arg.equals("-t")) {
				llp.parse_table();
				return;
			}
		}
		for (String arg:args) {
			if (arg.equals("-s")) {
				llp.print_sets();
			}
		}
		return;
	}

	private void print_sets() {
		// TODO Auto-generated method stub
		
	}

	private void parse_table() {
		// TODO Auto-generated method stub
		
	}

	private int init(String[] args) {
		p = new HashMap<Symbol, List<List<Symbol>>>();
		ts = new HashSet<Symbol>();
		nts = new HashSet<Symbol>();
		
		//
		String InputGrammar = "";
		int inputCount = 0;
		for (String arg:args) {
			if (!arg.startsWith("-")) {
				inputCount ++;
				InputGrammar = arg;
			}
		}
		if (inputCount != 1) {
			System.err.println(inputCount + " Input Grammar given, 1 expected.");
			return 1;
		}
		//Read
		String grammar = "";
		try {
			File file = new File(InputGrammar);
			FileReader reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);
			String line;
			while ((line=br.readLine())!=null) {
				//handle comments
				int comment_start = line.indexOf("//");
				if (comment_start > -1) {
					line = line.substring(0, comment_start);
				}
				grammar += (line + " ");
			}
			//Match
		    Matcher m_pl = Pattern.compile(MBNFParser.ProductionList).matcher(grammar);
		    //separate by production
		    while (m_pl.find()) {
		    	String production = m_pl.group(1);
		    	//System.out.println("Production:"+production);
		    	Matcher m_ps = Pattern.compile(MBNFParser.ProductionSet_S).matcher(production);
		    	if (m_ps.find()) {
		    		String lh = m_ps.group(1).trim();
		    		String rhs = m_ps.group(2);
		    		Symbol lh_s = new Symbol(lh);
		    		if (lh_s.isEpsilon) {
		    			System.err.println("LeftHand cannot be epsilon in:"+ m_ps.group());
		    			continue;
		    		}
		    		insertNT(lh_s);
		    		List<List<Symbol>> derives = new ArrayList<List<Symbol>>();
		    		p.put(lh_s, derives);
		    		//System.out.println("LH:" + m_ps.group(1));
		    		//System.out.println("RHS:" + m_ps.group(2));
		    		Matcher m_rh = Pattern.compile(MBNFParser.RightHandSide).matcher(rhs);
		    		while (m_rh.find()) {
		    			String rh = m_rh.group();
		    			List<Symbol> derive = new ArrayList<Symbol>();
		    			derives.add(derive);
		    			//System.out.println("RH:" + m_rh.group());
		    			Matcher m_eps = Pattern.compile(MBNFParser.EPSILON).matcher(rh);
		    			if (m_eps.find()) { //this rh is epsilon, only one epsilon could be accepted
		    				Symbol epsilon = new Symbol(m_eps.group());
		    				derive.add(epsilon);
		    				while (m_eps.find()) {
		    					System.err.println("Multiple epsilons in rightHand:" + rh);
		    				}
		    			} else { //this rh is not epsilon, try to match some symbols
		    				Matcher m_symbols = Pattern.compile(MBNFParser.SYMBOL).matcher(rh);
		    				while (m_symbols.find()) {
		    					Symbol symbol = new Symbol(m_symbols.group());
		    					if (symbol.isEpsilon) {
		    						System.err.println("Unexpected epsilon in rightHand:" + rh);
		    						continue;
		    					}
		    					insertT(symbol);
		    					derive.add(symbol);
		    				}
		    			}
		    		}	//End of RightHandSide match
		    	}	//End of one production match
		    }	//End of one leftHand match
		    //printAll();
		    br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return 1;
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		
	    calc_sets();
		
		return 0;
	}
	
	/**
	 * Calculate FIRST, FOLLOW, FIRST+ sets.
	 * Will be called after reading in all things.
	 */
	private void calc_sets() {
		calc_first();
		calc_follow();
		calc_firstplus();
	}

	private void calc_firstplus() {
		// TODO Auto-generated method stub
		
	}

	private void calc_follow() {
		// TODO Auto-generated method stub
		
	}

	private void calc_first() {
		first = new HashMap<Symbol,Set<Symbol>>();
		//Add epsilon and EOF
		Symbol eps = new Symbol("Epsilon");
		Set<Symbol> s_f = new HashSet<Symbol>();
		s_f.add(eps);
		first.put(eps, s_f);
		Symbol eof = new Symbol();
		s_f = new HashSet<Symbol>();
		s_f.add(eof);
		first.put(eof, s_f);
		//Add terminals
		for (Symbol terminals : ts) {
			s_f = new HashSet<Symbol>();
			
		}
		
	}

	private void insertT(Symbol srh) {
		if (nts.contains(srh))
			return;
		ts.add(srh);
	}

	private void insertNT(Symbol slh) {
		if (ts.contains(slh)) {
			ts.remove(slh);
		}
		nts.add(slh);
	}

	private void remove_leftrecursive() {
		// TODO Auto-generated method stub
		
	}

	//Below is for test.
	/**
	 * Print derive rules, terminal set, non-termial set.
	 */
	public void printAll() {
		System.out.println("====================Rules====================");
		for (Entry<Symbol,List<List<Symbol>>> entry:p.entrySet()) {
			System.out.print(entry.getKey() + " : ");
			List<List<Symbol>> rhs = entry.getValue();
			for (int i = 0 ; i < rhs.size() ;i++) {
				List<Symbol> rh = rhs.get(i);
				if (i > 0) {
					for (int x = 0 ; x <= entry.getKey().toString().length() ; x++) {
						System.out.print(" ");
					}
					System.out.print("| ");
				}
				for (int j = 0 ; j < rh.size() ; j++) {
					Symbol s = rh.get(j);
					System.out.print(s + " ");
				}
				System.out.println("");

			}
			for (int x = 0 ; x <= entry.getKey().toString().length() ; x++) {
				System.out.print(" ");
			}
			System.out.println(";");
		}
		printNT();
		printT();
	}

	/**
	 * Print terminal set.
	 */
	private void printT() {
		System.out.println("==================Terminals==================");
		for (Symbol t : ts) {
			System.out.print(t + " ");
		}
		System.out.println();
	}
	
	/**
	 * Print non-terminal set.
	 */
	private void printNT() {
		System.out.println("================Non-terminals================");
		for (Symbol nt : nts) {
			System.out.print(nt + " ");
		}
		System.out.println();
	}


}
