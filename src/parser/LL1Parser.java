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
	List<Symbol> nts;
	
	Symbol start;
	
	Map<Symbol,Set<Symbol>> first;
	Map<Symbol,Set<Symbol>> follow;
	Map<Symbol,List<Set<Symbol>>> firstp;
	
	boolean removeLR = false;
	
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
		
		for (String arg:args) {
			if (arg.equals("-r")) {
				llp.remove_leftrecursive();
			}
		}
		
		if (llp.init(args) > 0) {
			return;
		}
		
		for (String arg:args) {
			if (arg.equals("-t")) {
				//if also has -s
				for (String arg2:args) {
					if (arg2.equals("-s")) {
						System.err.println("Parameter cannot contain both -t and -s.");
						help();
						return;
					}
				}
				llp.parse_table();
				return;
			}
		}
		for (String arg:args) {
			if (arg.equals("-s")) {
				llp.print_sets();
			}
		}
		System.err.println("Parameter should contain -t or -s.");
		help();
		return;
	}

	private void print_sets() {
		printset(first);
		printset(follow);
		printFirstp();
	}

	private void parse_table() {
		YAML_sets();
		YAML_productions();
		YAML_table();
	}

	private void YAML_table() {
		//calculate table
		Map<Symbol,Integer[]> t = new HashMap<Symbol,Integer[]>(); 
		List<Symbol> t_l = new ArrayList<Symbol>();
		t_l.addAll(ts);	//add all terminals
		t_l.add(new Symbol());	//add eof
		for (Symbol lh : nts) {
			Integer[] lh_t = new Integer[t_l.size()];
			t.put(lh, lh_t);
		}
		boolean err = false;	// is there any LL(1) collision?
		Symbol err_t = null;
		Symbol err_nt = null;
		
		int p_idx = -1;
		for (Symbol lh : nts) { //for every lefthand symbol
			Integer[] t_row = t.get(lh);
			List<Set<Symbol>> fp = firstp.get(lh);
			for (Set<Symbol> p_fp : fp) {	//for every production
				p_idx++;
				for (Symbol s : p_fp) {		//for every symbol in first p
					int s_idx = t_l.indexOf(s);
					if (t_row[s_idx] != null) {
						err = true;
						err_nt = lh;
						err_t = s;
					} else {
						t_row[s_idx] = new Integer(p_idx);
					}
				}
			}
		}
		
		//print table
		if (err) {
			System.err.println("Cannot generate LL(1) table : collision detected on: NT: "+ err_nt +" T: "+ err_t );
			return;
		}
		System.out.println("table:");
		for (Symbol lh : nts) {		
			System.out.print("  ");	//indent
			System.out.print(lh + ": ");
			System.out.print("{");
			Integer[] t_row = t.get(lh);
			for (int i = 0 ; i < t_l.size() ;i++) {
				System.out.print(t_l.get(i));
				System.out.print(": ");
				if (t_row[i]==null) {
					System.out.print("--");
				} else {
					System.out.print(t_row[i]);
				}
				if (i < t_l.size() - 1) { 
					System.out.print(", ");
				}
			}
			System.out.println(" }");
		}
	}

	private void YAML_productions() {
		System.out.println("productions:");
		int p_i = -1;
		for (Symbol lh : nts) {	//for every lefthand
			List<List<Symbol>> rhs = p.get(lh);
			for (List<Symbol> rh : rhs)	{//for every production
				p_i ++;
				System.out.print("  " + p_i + ": {" + lh + ": ");
				if (rh.get(0).isEpsilon)
					System.out.print("[]");
				else
					System.out.print(rh.toString());
				System.out.println("}");
			}
		}
		System.out.println();
	}

	private void YAML_sets() {
		System.out.print("terminals: ");
		System.out.println(ts.toString());
		
		System.out.print("non-terminals: ");
		System.out.println(nts.toString());
		
		System.out.println("eof-marker: " + new Symbol().toString());
		
		System.out.println("error-marker: --");
		
		System.out.print("start-symbol: ");
		System.out.println(start);
		System.out.println();
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
		
		if (removeLR)
			do_removeLR();
		
	    calc_sets();
		
		return 0;
	}
	
	/**
	 * Remove direct left recursive.
	 * @param lh : left hand symbol
	 */
	private void do_removeDirectLR(Symbol lh) {
		//lh should exists in nts
		List<List<Symbol>> pds = p.get(lh);
		if (pds == null)
			return;
		
		//Divide all productions by whether it is DIRECT left recursive.
		//A->Aa1 | Aa2 .. 
		List<List<Symbol>> lrpd = new ArrayList<List<Symbol>>();
		//A->b1 | b2 | ... | epsilon
		List<List<Symbol>> nlrpd = new ArrayList<List<Symbol>>();
		
		for (List<Symbol> pd : pds) {
			if (pd.get(0).equals(lh)) {
				lrpd.add(pd);
			} else {
				nlrpd.add(pd);
			}
		}
		if (lrpd.size() == 0)
			return;
			
		//Try to name the new symbol
		int suffix = 0;
		Symbol newSymbol = new Symbol(lh.lexeme + suffix);
		while (ts.contains(newSymbol) || nts.contains(newSymbol)) {
			suffix ++;
			newSymbol = new Symbol(lh.lexeme + suffix);
		}
		
		List<List<Symbol>> newpd = new ArrayList<List<Symbol>>();
		List<List<Symbol>> newnpd = new ArrayList<List<Symbol>>();
		
		for (List<Symbol> nlrp : nlrpd) {	//for A -> b1 | b2 |...
			if (nlrp.get(0).isEpsilon) {
				newpd.add(nlrp);
			} else {
				nlrp.add(newSymbol);	// b1 change to b1 A'
				newpd.add(nlrp);		// A -> b1 A'
			}
		}
		
		for (List<Symbol> lrp : lrpd) {		//for A -> A a1 | A a2 | ...
			lrp.remove(0);
			lrp.add(newSymbol);			// A -> A a1 change to A' -> a1 A' | epsilon
			newnpd.add(lrp);
		}
		List<Symbol> lrp = new ArrayList<Symbol>();
		lrp.add(new Symbol("Epsilon"));
		newnpd.add(lrp);
		
		p.remove(lh);
		p.put(lh, newpd);
		p.put(newSymbol, newnpd);
		nts.add(newSymbol);
	}
	
	private void do_removeLR() {
		
		for (int i = 0 ; i < nts.size() ;i++) {
			Symbol lh = nts.get(i);
			for (int j = 0 ; j < i ; j ++) {
				Symbol rhf = nts.get(j);
				List<List<Symbol>> pds = p.get(lh);
				List<List<Symbol>> toAdd = new ArrayList<List<Symbol>>();
				for (int p_idx = 0 ; p_idx < pds.size() ; p_idx++) {
					if (pds.get(p_idx).get(0).equals(rhf)) {	// Ai -> Aj a
						pds.get(p_idx).remove(0);
						List<List<Symbol>> rhf_ps = p.get(rhf);
						for (List<Symbol> rhf_p : rhf_ps) {		//for each Aj ->
							List<Symbol> toAddp = new ArrayList<Symbol>();
							toAddp.addAll(rhf_p);
							toAddp.addAll(pds.get(p_idx));
							toAdd.add(toAddp);
						}
						pds.remove(p_idx);
						p_idx--;
					}	//end if Ai -> Aj a
				}	//end for every production of lh(Ai)
				pds.addAll(toAdd);
				
			} //end for every j
			do_removeDirectLR(lh);
		}	// end for every i
		
		remove_unreachable();
		
	}

	/**
	 * There may be some unreachable non-terminal symbol when after
	 * eliminating left recursive. Remove them.
	 */
	private void remove_unreachable() {
		Set<Symbol> r = new HashSet<Symbol>();
		r.add(start);
		int lastcount;
		int nowcount = 1;
		do {
			lastcount = nowcount;
			Set<Symbol> toAdd = new HashSet<Symbol>();
			for (Symbol s : r) {	//for every symbol is reachable
				List<List<Symbol>> pds = p.get(s);
				for (List<Symbol> pd : pds) {
					for (Symbol rhs : pd) {
						if (nts.contains(rhs)) {
							toAdd.add(rhs);
						}
					}
				}
			}
			r.addAll(toAdd);
			nowcount = r.size();
		} while (nowcount > lastcount);
		
		for (int i = 0 ; i < nts.size() ; i++) {
			Symbol s = nts.get(i);
			if (!r.contains(s)) {
				nts.remove(i);
				i --;
				p.remove(s);
			}
		}
		
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
		removeLR = true;
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
		for (Symbol lh : nts) {
			List<List<Symbol>> rhs = p.get(lh);
			List<Set<Symbol>> rhs_f = firstp.get(lh);
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
