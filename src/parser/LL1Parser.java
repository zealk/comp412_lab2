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
import java.util.TreeMap;
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
	List<Symbol> nts;
	
	Symbol start;
	
	Map<Symbol,Set<Symbol>> first;
	Map<Symbol,Set<Symbol>> follow;
	Map<Symbol,List<Set<Symbol>>> firstp;
	
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
		printset(first);
		printset(follow);
		printFirstp();
	}

	private void parse_table() {
		// TODO Auto-generated method stub
		
	}

	private int init(String[] args) {
		p = new HashMap<Symbol, List<List<Symbol>>>();
		ts = new HashSet<Symbol>();
		nts = new ArrayList<Symbol>();
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
		
		calc_startSymbol();
		
	    calc_sets();
		
		return 0;
	}
	
	/**
	 * Calculate Start Symbol.
	 * If a non-terminal does not exists in right, its a start symbol.
	 */
	private void calc_startSymbol() {
		Set<Symbol> ss = new HashSet<Symbol>();
		ss.addAll(nts);
		for (Entry<Symbol,List<List<Symbol>>> p_e : p.entrySet()) {
			List<List<Symbol>> rhs = p_e.getValue();
			for (List<Symbol> rh : rhs) {
				for (Symbol s : rh) {
					ss.remove(s);
				}
			}
		}
		if (ss.size() != 1) {
			System.err.println("It seems to be more than one start symbol!");
			return;
		}
		start = ss.iterator().next();
		//System.out.println(start);
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
		firstp = new HashMap<Symbol,List<Set<Symbol>>>();
		for (Symbol nt : nts) {
			List<Set<Symbol>> s_fp = new ArrayList<Set<Symbol>>();
			firstp.put(nt, s_fp);
			
			List<List<Symbol>> rhs = p.get(nt);
			for (List<Symbol> rh : rhs) {
				Set<Symbol> rh_fp = new HashSet<Symbol>();
				s_fp.add(rh_fp);
				int i = 0;
				for (;i < rh.size(); i++) {
					Symbol tmp = rh.get(i);
					Set<Symbol> tmp_first = first.get(tmp);
					rh_fp.addAll(tmp_first);
					rh_fp.remove(new Symbol("Epsilon"));
					if (!tmp_first.contains(new Symbol("Epsilon"))) {
						break;
					}
				}
				if (i == rh.size()) {
					rh_fp.addAll(follow.get(nt));
				}
			}
		}
	}

	private void calc_follow() {
		follow = new HashMap<Symbol,Set<Symbol>>();
		for (Symbol nt : nts) {
			Set<Symbol> s_fw = new HashSet<Symbol>();
			follow.put(nt, s_fw);
		}
		follow.get(start).add(new Symbol());	//add EOF to follow set of start symbol
		int last_total;
		int this_total = countTotal(follow);
		Set<Symbol> trailer;
		do {
			last_total = this_total;
			for (Entry<Symbol,List<List<Symbol>>> p_e : p.entrySet()) {	//For every leftHand symbol
				Symbol lh = p_e.getKey();
				List<List<Symbol>> rhs = p_e.getValue();
				for (List<Symbol> rh : rhs) {	//For every production
					trailer = new HashSet<Symbol>();
					trailer.addAll(follow.get(lh));	//TRAILER <- FOLLOW(A)
					for (int i = rh.size()-1 ;i > -1 ;i--) {
						Symbol bi = rh.get(i);
						if (nts.contains(bi)) {	//Bi is non-terminal
							follow.get(bi).addAll(trailer); //add all in TRAILER to FOLLOW(Bi)
							if (first.get(bi).contains(new Symbol("Epsilon"))) {	//if epsilon in FIRST(bi)
								trailer.addAll(first.get(bi));
								trailer.remove(new Symbol("Epsilon"));
							} else {
								trailer.clear();
								trailer.addAll(first.get(bi));
							}
						} else {	//Bi is terminal
							trailer.clear();
							trailer.add(bi);
						}
					}	//end of every i of bi
				}	//end for every production
			}	//end of for every leftHand symbol
			this_total = countTotal(follow);
		} while (last_total < this_total);
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
		//Add terminals , initial first is its self
		for (Symbol t : ts) {
			s_f = new HashSet<Symbol>();
			s_f.add(t);
			first.put(t, s_f);
		}
		//Add non-terminals , initial first should be empty
		for (Symbol nt : nts) {
			s_f = new HashSet<Symbol>();
			first.put(nt, s_f);
		}
		//printFirst();
		int last_total;
		int this_total = countTotal(first);
		do {
			//printFirst();
			last_total = this_total;
			for (Entry<Symbol,List<List<Symbol>>> p_e :p.entrySet()) { //For each lefthand symbol
				Symbol lh = p_e.getKey();
				for (List<Symbol> rh : p_e.getValue()) { //For each production rule
					int i = 0 , k = rh.size();
					Set<Symbol> rhs = new HashSet<Symbol>();
					rhs.addAll(first.get(rh.get(0)));
					boolean prefixEpsilon = true;
					for (;i < k-1;i++) {
						if (!first.get(rh.get(i)).contains(new Symbol("Epsilon"))) {
							prefixEpsilon = false;
							break;
						}
						rhs.addAll(first.get(rh.get(i+1)));
					}
					rhs.remove(new Symbol("Epsilon"));
					if (prefixEpsilon && first.get(rh.get(k-1)).contains(new Symbol("Epsilon"))) {
						rhs.add(new Symbol("Epsilon"));
					}
					first.get(lh).addAll(rhs);
				}
			}
			this_total = countTotal(first);
		} while (last_total < this_total);
	}
	
	/**
	 * Count the total entry number of first, follow or first+.
	 * Used to determine whether it remains unchanged.
	 */
	private int countTotal(Map<Symbol,Set<Symbol>> set) {
		int total = 0;
		for (Entry<Symbol,Set<Symbol>> s : set.entrySet()) {
			Set<Symbol> f_s = s.getValue();
			total += f_s.size();
		}
		return total;
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
		for (Symbol nt : nts) {
			System.out.print(nt + " : ");
			List<List<Symbol>> rhs = p.get(nt);
			for (int i = 0 ; i < rhs.size() ;i++) {
				List<Symbol> rh = rhs.get(i);
				if (i > 0) {
					for (int x = 0 ; x <= nt.toString().length() ; x++) {
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
			for (int x = 0 ; x <= nt.toString().length() ; x++) {
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
	
	/**
	 * Print first/follow set.
	 */
	private void printset(Map<Symbol,Set<Symbol>> set) {
		if (set == first)
			System.out.println("====================First=====================");
		else if (set == follow)
			System.out.println("====================Follow====================");
		else
			return;
		for (Entry<Symbol,Set<Symbol>> f_e : set.entrySet()) {
			System.out.print(f_e.getKey() + " : ");
			Set<Symbol> f_s = f_e.getValue();
			if (f_s.isEmpty()) {
				System.out.print("Ø");
			}
			for (Symbol s : f_e.getValue()) {
				System.out.print(s + " ");
			}
			System.out.println("");
		}
	}
	
	private void printFirstp() {
		System.out.println("====================First+====================");
		for (Entry<Symbol,List<Set<Symbol>>> f_e : firstp.entrySet()) {
			Symbol lh = f_e.getKey();
			List<List<Symbol>> rhs = p.get(lh);
			List<Set<Symbol>> rhs_f = f_e.getValue();
			for (int i = 0 ; i < rhs.size() ; i++ ) {
				System.out.print(lh + " -> ");
				List<Symbol> rh = rhs.get(i);
				Set<Symbol> rh_f = rhs_f.get(i);
				for (Symbol s : rh) {
					System.out.print(s + " ");
				}
				
				System.out.print(": ");
				
				if (rh_f.size() == 0) {
					System.out.print("Ø");
				}
				for (Symbol s : rh_f) {
					System.out.print(s + " ");
				}
				System.out.println();
			}
		}
	}


}
