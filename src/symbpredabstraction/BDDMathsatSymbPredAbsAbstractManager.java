package symbpredabstraction;

import java.util.Collection;

import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.cpas.symbpredabsCPA.SymbPredAbsAbstractElement;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.LazyLogger;

/**
 * Implementation of SummaryAbstractFormulaManager that works with BDDs for
 * abstraction and MathSAT terms for concrete formulas
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class BDDMathsatSymbPredAbsAbstractManager extends
					BDDMathsatAbstractFormulaManager implements
					SymbPredAbsAbstractFormulaManager {

	public class AllSatCallbackStats extends AllSatCallback {
		public long totTime = 0;

		public AllSatCallbackStats(int bdd, long msatEnv, long absEnv) {
			super(bdd, msatEnv, absEnv);
		}

		public void callback(long[] model) {
			long start = System.currentTimeMillis();
			super.callback(model);
			long end = System.currentTimeMillis();
			totTime += (end - start);
		}
	}

	// some statistics. All times are in milliseconds
	public class Stats {
		public long abstractionMathsatTime = 0;
		public long abstractionMaxMathsatTime = 0;
		public long abstractionBddTime = 0;
		public long abstractionMaxBddTime = 0;
		public int numCallsAbstraction = 0;
		public long cexAnalysisTime = 0;
		public long cexAnalysisMaxTime = 0;
		public int numCallsCexAnalysis = 0;
		public long abstractionMathsatSolveTime = 0;
		public long abstractionMaxMathsatSolveTime = 0;
		public long cexAnalysisMathsatTime = 0;
		public long cexAnalysisMaxMathsatTime = 0;
	}
	private Stats stats;

	// TODO later
//	private Map<Pair<CFANode, CFANode>, PathFormula> 
//	abstractionCache;

	//private BDDMathsatSummaryAbstractionPrinter absPrinter = null;
	private boolean dumpHardAbstractions;

	public BDDMathsatSymbPredAbsAbstractManager() {
		super();
		stats = new Stats();
		// TODO
//		abstractionCache = 
//			new HashMap<Pair<CFANode, CFANode>, PathFormula>();
		dumpHardAbstractions = CPAMain.cpaConfig.getBooleanValue(
		"cpas.symbpredabs.mathsat.dumpHardAbstractionQueries");
	}

	public Stats getStats() { return stats; }

	// builds the SymbolicFormula corresponding to the path between "e" and
	// "succ". In the purely explicit case, this would be just the operation
	// attached to the edge connecting "e" and "succ", but in our case this is
	// actually a loop-free subgraph of the original CFA
	// TODO we don't need this 
//	private Pair<SymbolicFormula, SSAMap> buildConcreteFormula(
//			MathsatSummaryFormulaManager mgr, 
//			SummaryAbstractElement e, SummaryAbstractElement succ,
//			boolean replaceAssignments) {
//		// first, get all the paths in e that lead to succ
//		Collection<Pair<SymbolicFormula, SSAMap>> relevantPaths = 
//			new Vector<Pair<SymbolicFormula, SSAMap>>();
//		for (CFANode leaf : e.getLeaves()) {
//			for (int i = 0; i < leaf.getNumLeavingEdges(); ++i) {
//				CFAEdge edge = leaf.getLeavingEdge(i);
//				InnerCFANode s = (InnerCFANode)edge.getSuccessor();
//				if (s.getSummaryNode().equals(succ.getLocation())) {
//					// ok, this path is relevant
//					relevantPaths.add(e.getPathFormula(leaf));
//
//					LazyLogger.log(LazyLogger.DEBUG_1,
//							"FOUND RELEVANT PATH, leaf: ", 
//							leaf.getNodeNumber());
//					LazyLogger.log(LazyLogger.DEBUG_3,
//							"Formula: ", 
//							e.getPathFormula(leaf).getFirst());
//				}
//			}
//		}
//		// now, we want to create a new formula that is the OR of all the 
//		// possible paths. So we merge the SSA maps and OR the formulas
//		SSAMap ssa = new SSAMap();
//		SymbolicFormula f = mgr.makeFalse();
//		for (Pair<SymbolicFormula, SSAMap> p : relevantPaths) {
//			Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mp = 
//				mgr.mergeSSAMaps(ssa, p.getSecond(), false);
//			SymbolicFormula curf = p.getFirst();
//			if (replaceAssignments) {
//				curf = mgr.replaceAssignments((MathsatSymbolicFormula)curf);
//			}
//			f = mgr.makeAnd(f, mp.getFirst().getFirst());
//			curf = mgr.makeAnd(curf, mp.getFirst().getSecond());
//			f = mgr.makeOr(f, curf);
//			ssa = mp.getSecond();
//		}
//
//		return new Pair<SymbolicFormula, SSAMap>(f, ssa);
//	}

	// computes the abstract post from "e" to "succ"
	@Override
	public AbstractFormula buildAbstraction(SymbolicFormulaManager mgr,
			SymbPredAbsAbstractElement e, SymbPredAbsAbstractElement succ, 
			PredicateMap predicates) {
		stats.numCallsAbstraction++;
		return buildBooleanAbstraction(mgr, e, succ, predicates);
	}

	@SuppressWarnings("unchecked")
	private AbstractFormula buildBooleanAbstraction(SymbolicFormulaManager mgr,
			SymbPredAbsAbstractElement e, SymbPredAbsAbstractElement succ, 
			PredicateMap predicates) {
		// A SummaryFormulaManager for MathSAT formulas
		MathsatSymbPredAbsFormulaManager mmgr = (MathsatSymbPredAbsFormulaManager)mgr;

		long startTime = System.currentTimeMillis();

		// get the environment from the manager - this is unique, it is the
		// environment in which all terms are created
		long msatEnv = mmgr.getMsatEnv();       
		//long absEnv = mathsat.api.msat_create_env();
		long absEnv = mathsat.api.msat_create_shared_env(msatEnv);

		// first, build the concrete representation of the abstract formula of e
		// this is an abstract formula - specifically it is a bddabstractformula
		// which is basically an integer which represents it
		AbstractFormula abs = e.getAbstraction();
		// create the concrete form of the abstract formula 
		// (abstract formula is the bdd representation)
		MathsatSymbolicFormula fabs = 
			(MathsatSymbolicFormula)mmgr.instantiate(
					toConcrete(mmgr, abs), null);

		// TODO function call - handle later
//		if (isFunctionExit(e)) {
//			// we have to take the context before the function call 
//			// into account, otherwise we are not building the right 
//			// abstraction!
//			if (CPAMain.cpaConfig.getBooleanValue(
//			"cpas.symbpredabs.refinement.addWellScopedPredicates")) {
//				// but only if we are adding well-scoped predicates, otherwise 
//				// this should not be necessary
//				AbstractFormula ctx = e.topContextAbstraction();
//				MathsatSymbolicFormula fctx = 
//					(MathsatSymbolicFormula)mmgr.instantiate(
//							toConcrete(mmgr, ctx), null);
//				fabs = (MathsatSymbolicFormula)mmgr.makeAnd(fabs, fctx);
//
//				LazyLogger.log(LazyLogger.DEBUG_3, 
//						"TAKING CALLING CONTEXT INTO ACCOUNT: ", fctx);
//			} else {
//				LazyLogger.log(LazyLogger.DEBUG_3, 
//						"NOT TAKING CALLING CONTEXT INTO ACCOUNT,",
//				"as we are not using well-scoped predicates");
//			}
//		}

		// create an ssamap from concrete formula
		SSAMap absSsa = mmgr.extractSSA(fabs);

		SymbolicFormula f = null;
		SSAMap ssa = null;
		
		// TODO later
		//Pair<CFANode, CFANode> key = new Pair<CFANode, CFANode>(e.getLocationNode(), succ.getLocationNode());
		
		// TODO enable cache later
//		if (abstractionCache.containsKey(key)) {
//			PathFormula pc = abstractionCache.get(key);
//			f = pc.getSymbolicFormula();
//			ssa = pc.getSsa();
//		} else {
//			// take all outgoing edges from e to succ and OR them
//			Pair<SymbolicFormula, SSAMap> pc = 
//				buildConcreteFormula(mmgr, e, succ, false);
////			SymbolicFormula f = pc.getFirst();
////			SSAMap ssa = pc.getSecond();
//			f = pc.getFirst();
//			ssa = pc.getSecond();
//
//			pc = mmgr.shift(f, absSsa);
//			f = mmgr.replaceAssignments((MathsatSymbolicFormula)pc.getFirst());
//			ssa = pc.getSecond();

			// TODO check
			PathFormula pf = succ.getInitAbstractionSet();
			f = pf.getSymbolicFormula();
			ssa = pf.getSsa();
			
			PathFormula pc = mmgr.shift(f, absSsa);
			f = mmgr.replaceAssignments((MathsatSymbolicFormula)pc.getSymbolicFormula());
			ssa = pc.getSsa();
			
			// TODO later
			//abstractionCache.put(key, new PathFormula((MathsatSymbolicFormula)f, ssa));
//		}

		if (CPAMain.cpaConfig.getBooleanValue(
		"cpas.symbpredabs.useBitwiseAxioms")) {
			MathsatSymbolicFormula bitwiseAxioms = mmgr.getBitwiseAxioms(
					(MathsatSymbolicFormula)f);
			f = mmgr.makeAnd(f, bitwiseAxioms);

			LazyLogger.log(LazyLogger.DEBUG_3, "ADDED BITWISE AXIOMS: ", 
					bitwiseAxioms);
		}

//		long term = mathsat.api.msat_make_copy_from(
//		absEnv, ((MathsatSymbolicFormula)f).getTerm(), msatEnv);
		long term = ((MathsatSymbolicFormula)f).getTerm();
		assert(!mathsat.api.MSAT_ERROR_TERM(term));

		// build the definition of the predicates, and instantiate them
		// TODO check
		Object[] predinfo = buildPredList(mmgr, predicates.getRelevantPredicates());
		long preddef = (Long)predinfo[0];
		long[] important = (long[])predinfo[1];
		Collection<String> predvars = (Collection<String>)predinfo[2];
//		for (int i = 0; i < important.length; ++i) {
//		important[i] = mathsat.api.msat_make_copy_from(
//		absEnv, important[i], msatEnv); 
//		}

		// update the SSA map, by instantiating all the uninstantiated 
		// variables that occur in the predicates definitions (at index 1)
		for (String var : predvars) {
			if (ssa.getIndex(var) < 0) {
				ssa.setIndex(var, 1);
			}
		}

		if (CPACheckerLogger.getLevel() <= LazyLogger.DEBUG_1.intValue()) {
			StringBuffer importantStrBuf = new StringBuffer();
			for (long t : important) {
				importantStrBuf.append(mathsat.api.msat_term_repr(t));
				importantStrBuf.append(" ");
			}
			LazyLogger.log(LazyLogger.DEBUG_1,
					"IMPORTANT SYMBOLS (", important.length, "): ", 
					importantStrBuf);
		}

		// first, create the new formula corresponding to 
		// (f & edges from e to succ)
		// TODO - at the moment, we assume that all the edges connecting e and
		// succ have no statement or assertion attached (i.e. they are just
		// return edges or gotos). This might need to change in the future!!
		// (So, for now we don't need to to anything...)         

		// instantiate the definitions with the right SSA
		MathsatSymbolicFormula inst = (MathsatSymbolicFormula)mmgr.instantiate(
				new MathsatSymbolicFormula(preddef), ssa);
//		preddef = mathsat.api.msat_make_copy_from(absEnv, inst.getTerm(), 
//		msatEnv);
//		long curstate = mathsat.api.msat_make_copy_from(absEnv, fabs.getTerm(),
//		msatEnv);
		preddef = inst.getTerm();
		long curstate = fabs.getTerm();

		// the formula is (curstate & term & preddef)
		// build the formula and send it to the absEnv
		long formula = mathsat.api.msat_make_and(absEnv, 
				mathsat.api.msat_make_and(absEnv, curstate, term), preddef);
		mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_UF);
		if (CPAMain.cpaConfig.getBooleanValue(
		"cpas.symbpredabs.mathsat.useIntegers")) {
			mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LIA);
			int ok = mathsat.api.msat_set_option(absEnv, "split_eq", "true");
			assert(ok == 0);
		} else {
			mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LRA);
		}
		mathsat.api.msat_set_theory_combination(absEnv, 
				mathsat.api.MSAT_COMB_ACK);
		int ok = mathsat.api.msat_set_option(absEnv, "toplevelprop", "2");
		assert(ok == 0);


		LazyLogger.log(LazyLogger.DEBUG_3, "COMPUTING ALL-SMT ON FORMULA: ",
				new MathsatSymbolicFormula(formula));

		int absbdd = bddManager.getZero();
		AllSatCallbackStats func = 
			new AllSatCallbackStats(absbdd, msatEnv, absEnv);
		long msatSolveStartTime = System.currentTimeMillis();
		mathsat.api.msat_assert_formula(absEnv, formula);
		int numModels = mathsat.api.msat_all_sat(absEnv, important, func);
		assert(numModels != -1);
		long msatSolveEndTime = System.currentTimeMillis();

		mathsat.api.msat_destroy_env(absEnv);

		// update statistics
		long endTime = System.currentTimeMillis();
		long msatSolveTime = 
			(msatSolveEndTime - msatSolveStartTime) - func.totTime;
		long abstractionMsatTime = (endTime - startTime) - func.totTime;
		stats.abstractionMaxMathsatTime = 
			Math.max(abstractionMsatTime, stats.abstractionMaxMathsatTime);
		stats.abstractionMaxBddTime =
			Math.max(func.totTime, stats.abstractionMaxBddTime);
		stats.abstractionMathsatTime += abstractionMsatTime;
		stats.abstractionBddTime += func.totTime;
		stats.abstractionMathsatSolveTime += msatSolveTime;
		stats.abstractionMaxMathsatSolveTime = 
			Math.max(msatSolveTime, stats.abstractionMaxMathsatSolveTime);

		// TODO later
//		if (abstractionMsatTime > 1000 && dumpHardAbstractions) {
//			// we want to dump "hard" problems...
//			if (absPrinter == null) {
//				absPrinter = new BDDMathsatSummaryAbstractionPrinter(
//						msatEnv, "abs");
//			}
//			absPrinter.printMsatFormat(curstate, term, preddef, important);
//			absPrinter.printNusmvFormat(curstate, term, preddef, important);
//			absPrinter.nextNum();            
//		}

		if (numModels == -2) {
			absbdd = bddManager.getOne();
			return new BDDAbstractFormula(absbdd);
		} else {
			return new BDDAbstractFormula(func.getBDD());
		}
	}

	// TODO enable
//	@Override
//	public CounterexampleTraceInfo buildCounterexampleTrace(
//	SummaryFormulaManager mgr, 
//	Deque<SummaryAbstractElement> abstractTrace) {
//	assert(abstractTrace.size() > 1);

////	mathsat.api.msat_set_verbosity(1);
//	long startTime = System.currentTimeMillis();
//	stats.numCallsCexAnalysis++;

//	// create the DAG formula corresponding to the abstract trace. We create
//	// n formulas, one per interpolation group
//	SSAMap ssa = null;        
//	MathsatSummaryFormulaManager mmgr = (MathsatSummaryFormulaManager)mgr;

//	Vector<SymbolicFormula> f = new Vector<SymbolicFormula>();

//	LazyLogger.log(LazyLogger.DEBUG_1, "\nBUILDING COUNTEREXAMPLE TRACE\n");
//	LazyLogger.log(LazyLogger.DEBUG_1, "ABSTRACT TRACE: ", abstractTrace);

//	Object[] abstarr = abstractTrace.toArray();
//	SummaryAbstractElement cur = (SummaryAbstractElement)abstarr[0];

//	boolean theoryCombinationNeeded = false;

//	MathsatSymbolicFormula bitwiseAxioms = 
//	(MathsatSymbolicFormula)mmgr.makeTrue();

//	for (int i = 1; i < abstarr.length; ++i) {
//	SummaryAbstractElement e = (SummaryAbstractElement)abstarr[i];
//	Pair<SymbolicFormula, SSAMap> p =
//	buildConcreteFormula(mmgr, cur, e, (ssa == null));

//	SSAMap newssa = null;
//	if (ssa != null) {
//	LazyLogger.log(LazyLogger.DEBUG_3, "SHIFTING: ", p.getFirst(),
//	" WITH SSA: ", ssa);
//	p = mmgr.shift(p.getFirst(), ssa);
//	newssa = p.getSecond();
//	LazyLogger.log(LazyLogger.DEBUG_3, "RESULT: ", p.getFirst(),
//	" SSA: ", newssa);
//	newssa.update(ssa);
//	} else {
//	LazyLogger.log(LazyLogger.DEBUG_3, "INITIAL: ", p.getFirst(),
//	" SSA: ", p.getSecond());
//	newssa = p.getSecond();
//	}
//	boolean hasUf = mmgr.hasUninterpretedFunctions(
//	(MathsatSymbolicFormula)p.getFirst());
//	theoryCombinationNeeded |= hasUf;
//	f.add(p.getFirst());
//	ssa = newssa;
//	cur = e;

//	if (hasUf && CPAMain.cpaConfig.getBooleanValue(
//	"cpas.symbpredabs.useBitwiseAxioms")) {
//	MathsatSymbolicFormula a = mmgr.getBitwiseAxioms(
//	(MathsatSymbolicFormula)p.getFirst());
//	bitwiseAxioms = (MathsatSymbolicFormula)mmgr.makeAnd(
//	bitwiseAxioms, a);
//	}
//	}

//	if (CPAMain.cpaConfig.getBooleanValue(
//	"cpas.symbpredabs.useBitwiseAxioms")) {
//	LazyLogger.log(LazyLogger.DEBUG_3, "ADDING BITWISE AXIOMS TO THE ",
//	"LAST GROUP: ", bitwiseAxioms);
//	f.setElementAt(mmgr.makeAnd(f.elementAt(f.size()-1), bitwiseAxioms),
//	f.size()-1);
//	}

//	LazyLogger.log(LazyLogger.DEBUG_3,
//	"Checking feasibility of abstract trace");

//	// now f is the DAG formula which is satisfiable iff there is a 
//	// concrete counterexample
//	//
//	// create a working environment
//	long env = mathsat.api.msat_create_env();
//	long msatEnv = mmgr.getMsatEnv();
//	long[] terms = new long[f.size()];
//	for (int i = 0; i < terms.length; ++i) {
//	terms[i] = mathsat.api.msat_make_copy_from(
//	env, ((MathsatSymbolicFormula)f.elementAt(i)).getTerm(),
//	msatEnv);
//	}
//	// initialize the env and enable interpolation
//	mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
//	mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
//	if (theoryCombinationNeeded) {
//	mathsat.api.msat_set_theory_combination(env, 
//	mathsat.api.MSAT_COMB_DTC);
//	} else if (CPAMain.cpaConfig.getBooleanValue(
//	"cpas.symbpredabs.mathsat.useIntegers")) {
//	int ok = mathsat.api.msat_set_option(env, "split_eq", "true");
//	assert(ok == 0);
//	}
////	int ok = mathsat.api.msat_set_option(env, "toplevelprop", "2");
////	assert(ok == 0);

//	mathsat.api.msat_init_interpolation(env);        

//	// for each term, create an interpolation group
//	int[] groups = new int[terms.length];
//	for (int i = 0; i < groups.length; ++i) {
//	groups[i] = mathsat.api.msat_create_itp_group(env);
//	}

//	boolean shortestTrace = CPAMain.cpaConfig.getBooleanValue(
//	"cpas.symbpredabs.shortestCexTrace");

//	// then, assert the formulas
//	long res = mathsat.api.MSAT_UNKNOWN;
//	long msatSolveTimeStart = System.currentTimeMillis();
//	for (int i = 0; i < terms.length; ++i) {
//	mathsat.api.msat_set_itp_group(env, groups[i]);
//	mathsat.api.msat_assert_formula(env, terms[i]);

//	LazyLogger.log(LazyLogger.DEBUG_3,
//	"Asserting formula: ", 
//	new MathsatSymbolicFormula(terms[i]),
//	" in group: ", groups[i]);

//	if (shortestTrace && mathsat.api.msat_term_is_true(terms[i]) == 0) {
//	res = mathsat.api.msat_solve(env);
//	if (res == mathsat.api.MSAT_UNSAT) {
//	break;
//	}
//	}
//	}
//	// and check satisfiability
//	if (!shortestTrace) {
//	res = mathsat.api.msat_solve(env);
//	}
//	long msatSolveTimeEnd = System.currentTimeMillis();

//	assert(res != mathsat.api.MSAT_UNKNOWN);

//	CounterexampleTraceInfo info = null;

//	if (res == mathsat.api.MSAT_UNSAT) {
//	// the counterexample is spurious. Extract the predicates from
//	// the interpolants
//	info = new CounterexampleTraceInfo(true); 
//	boolean splitItpAtoms = CPAMain.cpaConfig.getBooleanValue(
//	"cpas.symbpredabs.refinement.splitItpAtoms");            
////	UpdateablePredicateMap pmap = new UpdateablePredicateMap();
////	info.setPredicateMap(pmap);
//	// how to partition the trace into (A, B) depends on whether
//	// there are function calls involved or not: in general, A
//	// is the trace from the entry point of the current function
//	// to the current point, and B is everything else. To implement
//	// this, we keep track of which function we are currently in.
//	Stack<Integer> entryPoints = new Stack<Integer>();
//	entryPoints.push(0);
//	for (int i = 1; i < groups.length; ++i) {
//	int start_of_a = entryPoints.peek();
//	if (!CPAMain.cpaConfig.getBooleanValue(
//	"cpas.symbpredabs.refinement.addWellScopedPredicates")) {
//	// if we don't want "well-scoped" predicates, we always
//	// cut from the beginning
//	start_of_a = 0;
//	}

//	int[] groups_of_a = new int[i-start_of_a];
//	for (int j = 0; j < groups_of_a.length; ++j) {
//	groups_of_a[j] = groups[j+start_of_a];
//	}
//	long itp = mathsat.api.msat_get_interpolant(env, groups_of_a);
//	assert(!mathsat.api.MSAT_ERROR_TERM(itp));

//	if (CPACheckerLogger.getLevel() <= 
//	LazyLogger.DEBUG_3.intValue()) {
//	StringBuffer buf = new StringBuffer();
//	for (int g : groups_of_a) {
//	buf.append(g);
//	buf.append(" ");
//	}
//	LazyLogger.log(LazyLogger.DEBUG_3, "groups_of_a: ", buf);
//	}
//	LazyLogger.log(LazyLogger.DEBUG_3,
//	"Got interpolant(", i, "): ",
//	new MathsatSymbolicFormula(itp));

//	long itpc = mathsat.api.msat_make_copy_from(msatEnv, itp, env);
//	Collection<SymbolicFormula> atoms = mmgr.extractAtoms(
//	new MathsatSymbolicFormula(itpc), true, 
//	splitItpAtoms, false);
//	Set<Predicate> preds = buildPredicates(env, msatEnv, atoms);
//	SummaryAbstractElement s1 = 
//	(SummaryAbstractElement)abstarr[i];
//	info.addPredicatesForRefinement(s1, preds);

//	// If we are entering or exiting a function, update the stack 
//	// of entry points
//	SummaryAbstractElement e = (SummaryAbstractElement)abstarr[i];
//	if (isFunctionEntry(e)) {
//	LazyLogger.log(LazyLogger.DEBUG_3,
//	"Pushing entry point, function: ",
//	e.getLocation().getInnerNode().getFunctionName());
//	entryPoints.push(i);
//	} 
//	if (isFunctionExit(e)) {
//	LazyLogger.log(LazyLogger.DEBUG_3,
//	"Popping entry point, returning from function: ",
//	e.getLocation().getInnerNode().getFunctionName());
//	entryPoints.pop();

////	SummaryAbstractElement s1 = 
////	(SummaryAbstractElement)abstarr[i];
//	//pmap.update((CFANode)s1.getLocation(), preds);
//	}                
//	}
//	} else {
//	// this is a real bug, notify the user
//	info = new CounterexampleTraceInfo(false);
//	info.setConcreteTrace(new ConcreteTraceNoInfo());
//	// TODO - reconstruct counterexample
//	// For now, we dump the asserted formula to a user-specified file
//	String cexPath = CPAMain.cpaConfig.getProperty(
//	"cpas.symbpredabs.refinement.msatCexPath");
//	if (cexPath != null) {
//	long t = mathsat.api.msat_make_true(env);
//	for (int i = 0; i < terms.length; ++i) {
//	t = mathsat.api.msat_make_and(env, t, terms[i]);
//	}
//	String msatRepr = mathsat.api.msat_to_msat(env, t);
//	try {
//	PrintWriter pw = new PrintWriter(new File(cexPath));
//	pw.println(msatRepr);
//	pw.close();
//	} catch (FileNotFoundException e) {
//	LazyLogger.log(CustomLogLevel.INFO, 
//	"Failed to save msat Counterexample to file: ",
//	cexPath);
//	}
//	}
//	}

//	mathsat.api.msat_destroy_env(env);

////	mathsat.api.msat_set_verbosity(0);

//	// update stats
//	long endTime = System.currentTimeMillis();
//	long totTime = endTime - startTime;
//	stats.cexAnalysisTime += totTime;
//	stats.cexAnalysisMaxTime = Math.max(totTime, stats.cexAnalysisMaxTime);
//	long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;
//	stats.cexAnalysisMathsatTime += msatSolveTime;
//	stats.cexAnalysisMaxMathsatTime = 
//	Math.max(msatSolveTime, stats.cexAnalysisMaxMathsatTime);

//	return info;
//	}

	// TODO functions
//	private boolean isFunctionExit(SummaryAbstractElement e) {
//	CFANode inner = e.getLocation().getInnerNode();
//	return (inner.getNumLeavingEdges() == 1 && 
//	inner.getLeavingEdge(0) instanceof ReturnEdge);
//	}

//	private boolean isFunctionEntry(SummaryAbstractElement e) {
//	CFANode inner = e.getLocation().getInnerNode();
//	return (inner.getNumEnteringEdges() > 0 &&
//	inner.getEnteringEdge(0).getPredecessor() instanceof 
//	FunctionDefinitionNode);
//	}

	// generates the predicates corresponding to the given atoms, which were
	// extracted from the interpolant
	// TODO enable
//	private Set<Predicate> buildPredicates(long srcenv, long dstenv,
//	Collection<SymbolicFormula> atoms) {
//	Set<Predicate> ret = new HashSet<Predicate>();
//	for (SymbolicFormula atom : atoms) {
//	long tt = ((MathsatSymbolicFormula)atom).getTerm();
////	String s = mathsat.api.msat_to_msat(srcenv, t);
////	System.out.println("CREATING PREDICATE:\n" + s + "\n");
////	long tt = mathsat.api.msat_from_msat(dstenv, s);
////	long tt = mathsat.api.msat_make_copy_from(dstenv, t, srcenv);
//	long d = mathsat.api.msat_declare_variable(dstenv, 
//	"\"PRED" + mathsat.api.msat_term_repr(tt) + "\"",
//	mathsat.api.MSAT_BOOL);
//	long var = mathsat.api.msat_make_variable(dstenv, d);

//	assert(!mathsat.api.MSAT_ERROR_TERM(tt));
//	assert(!mathsat.api.MSAT_ERROR_TERM(var));

//	ret.add(makePredicate(var, tt));
//	}
//	return ret;
//	}

}
