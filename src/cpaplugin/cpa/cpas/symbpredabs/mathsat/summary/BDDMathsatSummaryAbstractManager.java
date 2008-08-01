package cpaplugin.cpa.cpas.symbpredabs.mathsat.summary;

import java.util.Collection;
import java.util.Deque;
import java.util.Vector;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.ConcreteTraceNoInfo;
import cpaplugin.cpa.cpas.symbpredabs.CounterexampleTraceInfo;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.UpdateablePredicateMap;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.BDDAbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.BDDMathsatAbstractFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.summary.InnerCFANode;
import cpaplugin.cpa.cpas.symbpredabs.summary.SummaryAbstractElement;
import cpaplugin.cpa.cpas.symbpredabs.summary.SummaryAbstractFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.summary.SummaryFormulaManager;
import cpaplugin.logging.CPACheckerLogger;

public class BDDMathsatSummaryAbstractManager extends
        BDDMathsatAbstractFormulaManager implements
        SummaryAbstractFormulaManager {

    public BDDMathsatSummaryAbstractManager() {
        super();
    }
    
    private Pair<SymbolicFormula, SSAMap> buildConcreteFormula(
            MathsatSummaryFormulaManager mgr, 
            SummaryAbstractElement e, SummaryAbstractElement succ,
            boolean replaceAssignments) {
        // first, get all the paths in e that lead to succ
        Collection<Pair<SymbolicFormula, SSAMap>> relevantPaths = 
            new Vector<Pair<SymbolicFormula, SSAMap>>();
        for (CFANode leaf : e.getLeaves()) {
            for (int i = 0; i < leaf.getNumLeavingEdges(); ++i) {
                CFAEdge edge = leaf.getLeavingEdge(i);
                InnerCFANode s = (InnerCFANode)edge.getSuccessor();
                if (s.getSummaryNode().equals(succ.getLocation())) {
                    // ok, this path is relevant
                    relevantPaths.add(e.getPathFormula(leaf));

                    LazyLogger.log(LazyLogger.DEBUG_1,
                                   "FOUND RELEVANT PATH, leaf: ", 
                                   leaf.getNodeNumber());
                    LazyLogger.log(LazyLogger.DEBUG_1,
                                   "Formula: ", 
                                   e.getPathFormula(leaf).getFirst());
                }
            }
        }
        // now, we want to create a new formula that is the OR of all the 
        // possible paths. So we merge the SSA maps and OR the formulas
        SSAMap ssa = new SSAMap();
        SymbolicFormula f = mgr.makeFalse();
        for (Pair<SymbolicFormula, SSAMap> p : relevantPaths) {
            Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mp = 
                mgr.mergeSSAMaps(ssa, p.getSecond(), false);
            SymbolicFormula curf = p.getFirst();
            if (replaceAssignments) {
                curf = mgr.replaceAssignments((MathsatSymbolicFormula)curf);
            }
            f = mgr.makeAnd(f, mp.getFirst().getFirst());
            curf = mgr.makeAnd(curf, mp.getFirst().getSecond());
            f = mgr.makeOr(f, curf);
            ssa = mp.getSecond();
        }
        
        return new Pair<SymbolicFormula, SSAMap>(f, ssa);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AbstractFormula buildAbstraction(SummaryFormulaManager mgr,
            SummaryAbstractElement e, SummaryAbstractElement succ, 
            Collection<Predicate> predicates) {
        MathsatSummaryFormulaManager mmgr = (MathsatSummaryFormulaManager)mgr;
        
        long absEnv = mathsat.api.msat_create_env();
        long msatEnv = mmgr.getMsatEnv();       
        
        // first, build the concrete representation of the abstract formula of e
        AbstractFormula abs = e.getAbstraction();
        MathsatSymbolicFormula fabs = 
            (MathsatSymbolicFormula)mmgr.instantiate(
                    toConcrete(mmgr, abs), null);
        
        SSAMap absSsa = mmgr.extractSSA(fabs);
        
        Pair<SymbolicFormula, SSAMap> pc = 
            buildConcreteFormula(mmgr, e, succ, false);
        SymbolicFormula f = pc.getFirst();
        SSAMap ssa = pc.getSecond();
        
        pc = mmgr.shift(f, absSsa);
        f = mmgr.replaceAssignments((MathsatSymbolicFormula)pc.getFirst());
        ssa = pc.getSecond();
        
        long term = mathsat.api.msat_make_copy_from(
                absEnv, ((MathsatSymbolicFormula)f).getTerm(), msatEnv);
        assert(!mathsat.api.MSAT_ERROR_TERM(term));
        
        
        // build the definition of the predicates, and instantiate them
        //Pair<Long, long[]> predlist = buildPredList(mmgr, predicates);
        Object[] predinfo = buildPredList(mmgr, predicates);
//        long preddef = predlist.getFirst();
//        long[] important = predlist.getSecond();
        long preddef = (Long)predinfo[0];
        long[] important = (long[])predinfo[1];
        Collection<String> predvars = (Collection<String>)predinfo[2];
        for (int i = 0; i < important.length; ++i) {
            important[i] = mathsat.api.msat_make_copy_from(
                    absEnv, important[i], msatEnv); 
        }
        
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
                           "IMPORTANT SYMBOLS (", important.length,
                           "): ", importantStrBuf);
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
        preddef = mathsat.api.msat_make_copy_from(absEnv, inst.getTerm(), 
                                                  msatEnv);
        long curstate = mathsat.api.msat_make_copy_from(absEnv, fabs.getTerm(),
                                                        msatEnv);
        
        // the formula is (curstate & term & preddef)
        // build the formula and send it to the absEnv
        long formula = mathsat.api.msat_make_and(absEnv, 
                mathsat.api.msat_make_and(absEnv, curstate, term), preddef);
        mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_UF);
        mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LRA);
        mathsat.api.msat_set_theory_combination(absEnv, 
                                                mathsat.api.MSAT_COMB_DTC);
        int ok = mathsat.api.msat_set_option(absEnv, "split_eq", "true");
        assert(ok == 0);
        mathsat.api.msat_assert_formula(absEnv, formula);

        LazyLogger.log(LazyLogger.DEBUG_1, "COMPUTING ALL-SMT ON FORMULA: ",
                       new MathsatSymbolicFormula(formula));

        int absbdd = bddManager.getZero();
        bddManager.ref(absbdd);
        AllSatCallback func = new AllSatCallback(absbdd, msatEnv, absEnv);
        int numModels = mathsat.api.msat_all_sat(absEnv, important, func);
        assert(numModels != -1);
        
        mathsat.api.msat_destroy_env(absEnv);

        if (numModels == -2) {
            bddManager.deref(absbdd);
            absbdd = bddManager.getOne();
            bddManager.ref(absbdd);
            return new BDDAbstractFormula(absbdd);
        } else {
            return new BDDAbstractFormula(func.getBDD());
        }
    }
    
    @Override
    public CounterexampleTraceInfo buildCounterexampleTrace(
            SummaryFormulaManager mgr, 
            Deque<SummaryAbstractElement> abstractTrace) {
        assert(abstractTrace.size() > 1);
        
//        mathsat.api.msat_set_verbosity(1);
        
        // create the DAG formula corresponding to the abstract trace. We create
        // n formulas, one per interpolation group
        SSAMap ssa = null;        
        MathsatSummaryFormulaManager mmgr = (MathsatSummaryFormulaManager)mgr;
        
        Vector<SymbolicFormula> f = new Vector<SymbolicFormula>();
        
        LazyLogger.log(LazyLogger.DEBUG_1, "\nBUILDING COUNTEREXAMPLE TRACE\n");        
        LazyLogger.log(LazyLogger.DEBUG_1, "ABSTRACT TRACE: ", abstractTrace);
        
        Object[] abstarr = abstractTrace.toArray();
        SummaryAbstractElement cur = (SummaryAbstractElement)abstarr[0];
        
        boolean theoryCombinationNeeded = false;
        
        for (int i = 1; i < abstarr.length; ++i) {
            SummaryAbstractElement e = (SummaryAbstractElement)abstarr[i];
            Pair<SymbolicFormula, SSAMap> p = 
                buildConcreteFormula(mmgr, cur, e, (ssa == null));
                        
            SSAMap newssa = null;
            if (ssa != null) {
                LazyLogger.log(LazyLogger.DEBUG_2, "SHIFTING: ", p.getFirst(),
                        " WITH SSA: ", ssa);
                p = mmgr.shift(p.getFirst(), ssa);
                newssa = p.getSecond();
                LazyLogger.log(LazyLogger.DEBUG_2, "RESULT: ", p.getFirst(),
                               " SSA: ", newssa);
                for (String var : ssa.allVariables()) {
                    if (newssa.getIndex(var) < 0) {
                        newssa.setIndex(var, ssa.getIndex(var));
                    }
                }
            } else {
                LazyLogger.log(LazyLogger.DEBUG_2, "INITIAL: ", p.getFirst(),
                               " SSA: ", p.getSecond());
                newssa = p.getSecond();
            }
            theoryCombinationNeeded |= mmgr.hasUninterpretedFunctions(
                    (MathsatSymbolicFormula)p.getFirst());
            f.add(p.getFirst());
            ssa = newssa;
            cur = e;
        }
        
        LazyLogger.log(LazyLogger.DEBUG_1,
                       "Checking feasibility of abstract trace");
        
        // now f is the DAG formula which is satisfiable iff there is a 
        // concrete counterexample
        //
        // create a working environment
        long env = mathsat.api.msat_create_env();
        long msatEnv = mmgr.getMsatEnv();
        long[] terms = new long[f.size()];
        for (int i = 0; i < terms.length; ++i) {
            terms[i] = mathsat.api.msat_make_copy_from(
                        env, ((MathsatSymbolicFormula)f.elementAt(i)).getTerm(), 
                        msatEnv);
        }
        // initialize the env and enable interpolation
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
        if (theoryCombinationNeeded) {
            mathsat.api.msat_set_theory_combination(env, 
                    mathsat.api.MSAT_COMB_DTC);
        } else {
            int ok = mathsat.api.msat_set_option(env, "split_eq", "true");
            assert(ok == 0);
        }
        mathsat.api.msat_init_interpolation(env);        
        
        // for each term, create an interpolation group
        int[] groups = new int[terms.length];
        for (int i = 0; i < groups.length; ++i) {
            groups[i] = mathsat.api.msat_create_itp_group(env);
        }
        // then, assert the formulas
        for (int i = 0; i < terms.length; ++i) {
            mathsat.api.msat_set_itp_group(env, groups[i]);
            mathsat.api.msat_assert_formula(env, terms[i]);

            LazyLogger.log(LazyLogger.DEBUG_1,
                           "Asserting formula: ", 
                           new MathsatSymbolicFormula(terms[i]),
                           " in group: ", groups[i]);
        }
        // and check satisfiability
        long res = mathsat.api.msat_solve(env);
        
        assert(res != mathsat.api.MSAT_UNKNOWN);
        
        CounterexampleTraceInfo info = null;
        
        if (res == mathsat.api.MSAT_UNSAT) {
            // the counterexample is spurious. Extract the predicates from
            // the interpolants
            info = new CounterexampleTraceInfo(true);            
            UpdateablePredicateMap pmap = new UpdateablePredicateMap();
            info.setPredicateMap(pmap);
            //Object[] abstarr = abstractTrace.toArray();
            for (int i = 1; i < groups.length; ++i) {
                int[] groups_of_a = new int[i];
                for (int j = 0; j < i; ++j) {
                    groups_of_a[j] = groups[j];
                }
                long itp = mathsat.api.msat_get_interpolant(env, groups_of_a);
                assert(!mathsat.api.MSAT_ERROR_TERM(itp));
                
                LazyLogger.log(LazyLogger.DEBUG_1,
                               "Got interpolant(", i, "): ",
                               new MathsatSymbolicFormula(itp));
                
                Collection<SymbolicFormula> atoms = mmgr.extractAtoms(
                        new MathsatSymbolicFormula(itp), true);
                Collection<Predicate> preds = 
                    buildPredicates(env, msatEnv, atoms);
                if (CPAMain.cpaConfig.getBooleanValue(
                        "cpas.symbpredabs.summary.addPredicatesGlobally")) {
                    for (Object o : abstarr) {
                        SummaryAbstractElement s = (SummaryAbstractElement)o;
                        pmap.update((CFANode)s.getLocation(), preds);
                    }
                } else {
                    SummaryAbstractElement s1 = 
                        (SummaryAbstractElement)abstarr[i];
                    pmap.update((CFANode)s1.getLocation(), preds);
//                    SummaryAbstractElement s2 = 
//                        (SummaryAbstractElement)abstarr[i+1];
//                    pmap.update((CFANode)s2.getLocation(), preds);
                }
            }
        } else {
            // this is a real bug, notify the user
            info = new CounterexampleTraceInfo(false);
            info.setConcreteTrace(new ConcreteTraceNoInfo());
            // TODO - reconstruct counterexample
        }
        
        mathsat.api.msat_destroy_env(env);
        
//        mathsat.api.msat_set_verbosity(0);
        
        return info;
    }

    private Collection<Predicate> buildPredicates(long srcenv, long dstenv,
            Collection<SymbolicFormula> atoms) {
        Collection<Predicate> ret = new Vector<Predicate>();
        for (SymbolicFormula atom : atoms) {
            long t = ((MathsatSymbolicFormula)atom).getTerm();
            long tt = mathsat.api.msat_make_copy_from(dstenv, t, srcenv);
            long d = mathsat.api.msat_declare_variable(dstenv, 
                    "\"PRED" + mathsat.api.msat_term_repr(tt) + "\"",
                    mathsat.api.MSAT_BOOL);
            long var = mathsat.api.msat_make_variable(dstenv, d);
            
            assert(!mathsat.api.MSAT_ERROR_TERM(tt));
            assert(!mathsat.api.MSAT_ERROR_TERM(var));
            
            ret.add(makePredicate(var, tt));
        }
        return ret;
    }
}
