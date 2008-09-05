package cpaplugin.cpa.cpas.symbpredabs.explicit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import cpaplugin.cfa.objectmodel.BlankEdge;
import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;
import cpaplugin.cfa.objectmodel.c.DeclarationEdge;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.BlockEdge;
import cpaplugin.cpa.cpas.symbpredabs.ConcreteTraceNoInfo;
import cpaplugin.cpa.cpas.symbpredabs.CounterexampleTraceInfo;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.UnrecognizedCFAEdgeException;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.BDDAbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.BDDMathsatAbstractFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.BDDPredicate;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatSymbolicFormulaManager;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;

/**
 * Implementation of ExplicitAbstractFormulaManager that uses BDDs for
 * AbstractFormulas and MathSAT terms for SymbolicFormulas
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class BDDMathsatExplicitAbstractManager extends
        BDDMathsatAbstractFormulaManager 
        implements ExplicitAbstractFormulaManager {
    
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
        public int abstractionNumMathsatQueries = 0;
        
        public Map<CFAEdge, Integer> edgeAbstCountMap = 
            new HashMap<CFAEdge, Integer>();
        public int abstractionNumCachedQueries; 
    }
    protected Stats stats;
    private boolean extendedStats;
    
    abstract class KeyWithTimeStamp {
        public long timeStamp;
        
        public KeyWithTimeStamp() {
            updateTimeStamp();
        }
        
        public void updateTimeStamp() {
            timeStamp = System.currentTimeMillis();
        }
    }
    
    class CartesianAbstractionCacheKey extends KeyWithTimeStamp {
        SymbolicFormula formula;
        Predicate pred;
        
        public CartesianAbstractionCacheKey(SymbolicFormula f, Predicate p) {
            super();
            formula = f;
            pred = p;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof CartesianAbstractionCacheKey) {
                CartesianAbstractionCacheKey c = 
                    (CartesianAbstractionCacheKey)o;
                return formula.equals(c.formula) && pred.equals(c.pred);
            } else {
                return false;
            }
        }
        
        public int hashCode() {
            return formula.hashCode() ^ pred.hashCode();
        }
    }
    
    class BooleanAbstractionCacheKey extends KeyWithTimeStamp {
        SymbolicFormula formula;
        Collection<Predicate> predList;
        
        public BooleanAbstractionCacheKey(SymbolicFormula f, 
                Collection<Predicate> preds) {
            super();
            formula = f;
            predList = preds;
        }
        
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof BooleanAbstractionCacheKey) {
                BooleanAbstractionCacheKey c =
                    (BooleanAbstractionCacheKey)o;
                return formula.equals(c.formula) && predList.equals(c.predList);
            } else {
                return false;
            }
        }
        
        public int hashCode() {
            return formula.hashCode() ^ predList.hashCode();
        }
    }
    
    class FeasibilityCacheKey extends KeyWithTimeStamp {
        SymbolicFormula f;
        
        public FeasibilityCacheKey(SymbolicFormula fm) {
            super();
            f = fm;
        }
        
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof FeasibilityCacheKey) {
                return f.equals(((FeasibilityCacheKey)o).f);
            }
            return false;
        }
        
        public int hashCode() {
            return f.hashCode();
        }       
    }
    
    class TimeStampCache<Key extends KeyWithTimeStamp, Value> 
        extends HashMap<Key, Value> {
        /**
         * default value
         */
        private static final long serialVersionUID = 1L;
        private int maxSize;
        
        class TimeStampComparator implements Comparator<KeyWithTimeStamp> {
            @Override
            public int compare(KeyWithTimeStamp arg0, KeyWithTimeStamp arg1) {
                long r = arg0.timeStamp - arg1.timeStamp;
                return r < 0 ? -1 : (r > 0 ? 1 : 0);
            }
        }
        
        private TimeStampComparator cmp;
        
        public TimeStampCache(int maxSize) {
            super();
            this.maxSize = maxSize;
            cmp = new TimeStampComparator();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Value get(Object o) {
            Key key = (Key)o;
            key.updateTimeStamp();
            return super.get(key);
        }
        
        @Override
        public Value put(Key key, Value value) {
            key.updateTimeStamp();
            compact();
            return super.put(key, value);
        }
        
        private void compact() {
            if (size() > maxSize) {
                // find the half oldest entries, and get rid of them...
                KeyWithTimeStamp[] keys = keySet().toArray(
                        new KeyWithTimeStamp[0]);
                Arrays.sort(keys, cmp);
                for (int i = 0; i < keys.length/2; ++i) {
                    remove(keys[i]);
                }
            }
        }
    }
    
    // cache for cartesian abstraction queries. For each predicate, the values
    // are -1: predicate is false, 0: predicate is don't care, 
    // 1: predicate is true
    protected TimeStampCache<CartesianAbstractionCacheKey, Byte> 
        cartesianAbstractionCache;
    protected TimeStampCache<FeasibilityCacheKey, Boolean> feasibilityCache;
    protected TimeStampCache<BooleanAbstractionCacheKey, AbstractFormula>
        booleanAbstractionCache;
    protected boolean useCache;
    
    public BDDMathsatExplicitAbstractManager() {
        super();
        stats = new Stats();
        useCache = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useCache");
        if (useCache) {
            final int MAX_CACHE_SIZE = 100000;
            cartesianAbstractionCache = 
                new TimeStampCache<CartesianAbstractionCacheKey, Byte>(
                        MAX_CACHE_SIZE);
            feasibilityCache = 
                new TimeStampCache<FeasibilityCacheKey, Boolean>(
                        MAX_CACHE_SIZE);
            booleanAbstractionCache = 
                new TimeStampCache<BooleanAbstractionCacheKey, 
                                   AbstractFormula>(MAX_CACHE_SIZE);
        }
        extendedStats = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.explicit.extendedStats");
    }
    
    public Stats getStats() { return stats; }

    // computes the formula corresponding to executing the operation attached
    // to the given edge, starting from the data region encoded by the
    // abstraction at "e"
    protected Pair<SymbolicFormula, SSAMap> buildConcreteFormula(
            MathsatSymbolicFormulaManager mgr, 
            ExplicitAbstractElement e, ExplicitAbstractElement succ,
            CFAEdge edge, boolean replaceAssignments) {
        
        assert(edge.getSuccessor().equals(succ.getLocation()));
       
        AbstractFormula abs = e.getAbstraction();
        MathsatSymbolicFormula fabs = 
            (MathsatSymbolicFormula)mgr.instantiate(
                    toConcrete(mgr, abs), null);        
        SSAMap ssa = mgr.extractSSA(fabs);
        Pair<SymbolicFormula, SSAMap> p = null;
        try {
            p = mgr.makeAnd(fabs, edge, ssa, true, false);
        } catch (UnrecognizedCFAEdgeException e1) {
            e1.printStackTrace();
            System.exit(1);
        }
        
        return p;
    }

    @Override
    public AbstractFormula buildAbstraction(SymbolicFormulaManager mgr,
            ExplicitAbstractElement e, ExplicitAbstractElement succ,
            CFAEdge edge, Collection<Predicate> predicates) {
        stats.numCallsAbstraction++;
        if (extendedStats) {
            int n = 0;
            if (stats.edgeAbstCountMap.containsKey(edge)) {
                n = stats.edgeAbstCountMap.get(edge);
            }
            stats.edgeAbstCountMap.put(edge, n+1);
        }
        
        if (!(succ.getLocation() instanceof CFAErrorNode)) {
            if (edge instanceof BlankEdge || edge instanceof DeclarationEdge //||
//                    (predicates.size() == 0 && 
//                            ((BDDAbstractFormula)e.getAbstraction()).getBDD() == 
//                                bddManager.getOne())) {
                    ) {
                LazyLogger.log(LazyLogger.DEBUG_1, 
                        "SKIPPING ABSTRACTION CHECK, e: ", e, ", SUCC:", succ,
                        ", edge: ", edge);
                return e.getAbstraction();
            }
        }
                
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.abstraction.cartesian")) {
            return buildCartesianAbstraction(mgr, e, succ, edge, predicates);
        } else {
            return buildBooleanAbstraction(mgr, e, succ, edge, predicates);
        }
    }

    // precise predicate abstraction, using All-SMT algorithm
    @SuppressWarnings("unchecked")
    private AbstractFormula buildBooleanAbstraction(SymbolicFormulaManager mgr,
            ExplicitAbstractElement e, ExplicitAbstractElement succ,
            CFAEdge edge, Collection<Predicate> predicates) {
        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        
        long startTime = System.currentTimeMillis();
        
        long msatEnv = mmgr.getMsatEnv();       
        long absEnv = mathsat.api.msat_create_shared_env(msatEnv);
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
                mathsat.api.MSAT_COMB_DTC);


        // first, build the concrete representation of the abstract formula of e
        //        AbstractFormula abs = e.getAbstraction();
        //        MathsatSymbolicFormula fabs = 
        //            (MathsatSymbolicFormula)mmgr.instantiate(
        //                    toConcrete(mmgr, abs), null);

        if (isFunctionExit(e)) {
            // we have to take the context before the function call 
            // into account, otherwise we are not building the right 
            // abstraction!
            assert(false); // TODO
            //            if (CPAMain.cpaConfig.getBooleanValue(
            //                    "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
            //                // but only if we are adding well-scoped predicates, otherwise 
            //                // this should not be necessary
            //                AbstractFormula ctx = e.topContextAbstraction();
            //                MathsatSymbolicFormula fctx = 
            //                    (MathsatSymbolicFormula)mmgr.instantiate(
            //                            toConcrete(mmgr, ctx), null);
            //                fabs = (MathsatSymbolicFormula)mmgr.makeAnd(fabs, fctx);
            //
            //                LazyLogger.log(LazyLogger.DEBUG_3, 
            //                        "TAKING CALLING CONTEXT INTO ACCOUNT: ", fctx);
            //            } else {
            //                LazyLogger.log(LazyLogger.DEBUG_3, 
            //                        "NOT TAKING CALLING CONTEXT INTO ACCOUNT,",
            //                        "as we are not using well-scoped predicates");
            //            }
        }

        //        SSAMap absSsa = mmgr.extractSSA(fabs);

        Pair<SymbolicFormula, SSAMap> pc = 
            buildConcreteFormula(mmgr, e, succ, edge, false);
        SymbolicFormula f = pc.getFirst();
        SSAMap ssa = pc.getSecond();

        //        pc = mmgr.shift(f, absSsa);
        f = mmgr.replaceAssignments((MathsatSymbolicFormula)pc.getFirst());
        //        ssa = pc.getSecond();
        
        BooleanAbstractionCacheKey key = null;
        if (useCache) {
            key = new BooleanAbstractionCacheKey(f, predicates);
            if (booleanAbstractionCache.containsKey(key)) {
                mathsat.api.msat_destroy_env(absEnv);
                ++stats.abstractionNumCachedQueries;
                return booleanAbstractionCache.get(key);
            }
        }

        if (CPAMain.cpaConfig.getBooleanValue(
        "cpas.symbpredabs.useBitwiseAxioms")) {
            MathsatSymbolicFormula bitwiseAxioms = mmgr.getBitwiseAxioms(
                    (MathsatSymbolicFormula)f);
            f = mmgr.makeAnd(f, bitwiseAxioms);

            LazyLogger.log(LazyLogger.DEBUG_3, "ADDED BITWISE AXIOMS: ", 
                    bitwiseAxioms);
        }
//        long term = mathsat.api.msat_make_copy_from(
//                absEnv, ((MathsatSymbolicFormula)f).getTerm(), msatEnv);
        long term = ((MathsatSymbolicFormula)f).getTerm();
        assert(!mathsat.api.MSAT_ERROR_TERM(term));

        // build the definition of the predicates, and instantiate them
        Object[] predinfo = buildPredList(mmgr, predicates);
        long preddef = (Long)predinfo[0];
        long[] important = (long[])predinfo[1];
        Collection<String> predvars = (Collection<String>)predinfo[2];
//              for (int i = 0; i < important.length; ++i) {
//                  important[i] = mathsat.api.msat_make_copy_from(
//                          absEnv, important[i], msatEnv); 
//              }

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
//        preddef = mathsat.api.msat_make_copy_from(absEnv, inst.getTerm(), 
//                msatEnv);
        preddef = inst.getTerm();

        // the formula is (curstate & term & preddef)
        // build the formula and send it to the absEnv
        long formula = mathsat.api.msat_make_and(absEnv, term, preddef);
//        mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_UF);
//        if (CPAMain.cpaConfig.getBooleanValue(
//        "cpas.symbpredabs.mathsat.useIntegers")) {
//            mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LIA);
//            int ok = mathsat.api.msat_set_option(absEnv, "split_eq", "true");
//            assert(ok == 0);
//        } else {
//            mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LRA);
//        }
//        mathsat.api.msat_set_theory_combination(absEnv, 
//                mathsat.api.MSAT_COMB_ACK);
//        int ok = mathsat.api.msat_set_option(absEnv, "toplevelprop", "2");
//        assert(ok == 0);


        LazyLogger.log(LazyLogger.DEBUG_1, "COMPUTING ALL-SMT ON FORMULA: ",
                new MathsatSymbolicFormula(formula));
        
        ++stats.abstractionNumMathsatQueries;

        int absbdd = bddManager.getZero();
        AllSatCallbackStats func = 
            new AllSatCallbackStats(absbdd, msatEnv, absEnv);
        long libmsatStartTime = System.currentTimeMillis();
        mathsat.api.msat_assert_formula(absEnv, formula);
        int numModels = mathsat.api.msat_all_sat(absEnv, important, func);
        assert(numModels != -1);
        long libmsatEndTime = System.currentTimeMillis();

        mathsat.api.msat_destroy_env(absEnv);

        // update statistics
        long endTime = System.currentTimeMillis();
        long libmsatTime = (libmsatEndTime - libmsatStartTime) - func.totTime;
        long msatTime = (endTime - startTime) - func.totTime;
        stats.abstractionMaxMathsatTime = 
            Math.max(msatTime, stats.abstractionMaxMathsatTime);
        stats.abstractionMaxBddTime =
            Math.max(func.totTime, stats.abstractionMaxBddTime);
        stats.abstractionMathsatTime += msatTime;
        stats.abstractionBddTime += func.totTime;
        stats.abstractionMathsatSolveTime += libmsatTime;
        stats.abstractionMaxMathsatSolveTime = 
            Math.max(libmsatTime, stats.abstractionMaxMathsatSolveTime);

        AbstractFormula ret = null;
        if (numModels == -2) {
            absbdd = bddManager.getOne();
            ret = new BDDAbstractFormula(absbdd);
        } else {
            ret = new BDDAbstractFormula(func.getBDD());
        }
        if (useCache) {
            booleanAbstractionCache.put(key, ret);
        }
        return ret;
    }

    // cartesian abstraction
    protected AbstractFormula buildCartesianAbstraction(
            SymbolicFormulaManager mgr, ExplicitAbstractElement e, 
            ExplicitAbstractElement succ, CFAEdge edge,
            Collection<Predicate> predicates) {
        long startTime = System.currentTimeMillis();

        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        
        long msatEnv = mmgr.getMsatEnv();       
        long absEnv =  mathsat.api.msat_create_shared_env(msatEnv);
        //long absEnv = mathsat.api.msat_create_env();
//        if (absEnv == 0) {
//            absEnv = mathsat.api.msat_create_shared_env(msatEnv);
//        }
        //mathsat.api.msat_reset_env(absEnv);
        mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_UF);
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useIntegers")) {
            mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LIA);
            int ok = mathsat.api.msat_set_option(
                    absEnv, "split_eq", "false");
            assert(ok == 0);
        } else {
            mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LRA);
        }
        mathsat.api.msat_set_theory_combination(absEnv, 
                mathsat.api.MSAT_COMB_DTC);
        // disable static learning. For small problems, this is just overhead
        mathsat.api.msat_set_option(absEnv, "sl", "0");
        
        
        if (isFunctionExit(e)) {
            // we have to take the context before the function call 
            // into account, otherwise we are not building the right 
            // abstraction!
            assert(false); // TODO
//            if (CPAMain.cpaConfig.getBooleanValue(
//                    "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
//                // but only if we are adding well-scoped predicates, otherwise 
//                // this should not be necessary
//                AbstractFormula ctx = e.topContextAbstraction();
//                MathsatSymbolicFormula fctx = 
//                    (MathsatSymbolicFormula)mmgr.instantiate(
//                            toConcrete(mmgr, ctx), null);
//                fabs = (MathsatSymbolicFormula)mmgr.makeAnd(fabs, fctx);
//
//                LazyLogger.log(LazyLogger.DEBUG_3, 
//                        "TAKING CALLING CONTEXT INTO ACCOUNT: ", fctx);
//            } else {
//                LazyLogger.log(LazyLogger.DEBUG_3, 
//                        "NOT TAKING CALLING CONTEXT INTO ACCOUNT,",
//                        "as we are not using well-scoped predicates");
//            }
        }
        
        Pair<SymbolicFormula, SSAMap> pc = 
            buildConcreteFormula(mmgr, e, succ, edge, false);
        SymbolicFormula f = pc.getFirst();
        SSAMap ssa = pc.getSecond();
        
        f = mmgr.replaceAssignments((MathsatSymbolicFormula)pc.getFirst());
        SymbolicFormula fkey = f;
        
        byte[] predVals = null;
        final byte NO_VALUE = -2;
        if (useCache) {
            predVals = new byte[predicates.size()];
            int predIndex = -1;
            for (Predicate p : predicates) {
                ++predIndex;
                CartesianAbstractionCacheKey key = 
                    new CartesianAbstractionCacheKey(f, p);
                if (cartesianAbstractionCache.containsKey(key)) {
                    predVals[predIndex] = cartesianAbstractionCache.get(key);
                } else {
                    predVals[predIndex] = NO_VALUE;
                }
            }
        }
        
        boolean skipFeasibilityCheck = false;
        if (useCache) {
//            Pair<AbstractFormula, CFAEdge> key = 
//                new Pair<AbstractFormula, CFAEdge>(e.getAbstraction(), edge);
            FeasibilityCacheKey key = new FeasibilityCacheKey(f);
            if (feasibilityCache.containsKey(key)) {
                skipFeasibilityCheck = true;
                if (!feasibilityCache.get(key)) {
                    // abstract post leads to false, we can return immediately
                    return new BDDAbstractFormula(bddManager.getZero());
                }
            }
        }        


        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.useBitwiseAxioms")) {
            MathsatSymbolicFormula bitwiseAxioms = mmgr.getBitwiseAxioms(
                    (MathsatSymbolicFormula)f);
            f = mmgr.makeAnd(f, bitwiseAxioms);

            LazyLogger.log(LazyLogger.DEBUG_3, "ADDED BITWISE AXIOMS: ", 
                    bitwiseAxioms);
        }
        long term = ((MathsatSymbolicFormula)f).getTerm();
//        term = mathsat.api.msat_make_copy_from(
//                absEnv, ((MathsatSymbolicFormula)f).getTerm(), msatEnv);
        assert(!mathsat.api.MSAT_ERROR_TERM(term));
        
        long solveStartTime = System.currentTimeMillis();        
        mathsat.api.msat_assert_formula(absEnv, term);    
        
        if (!skipFeasibilityCheck) {
            ++stats.abstractionNumMathsatQueries;
            if (mathsat.api.msat_solve(absEnv) == mathsat.api.MSAT_UNSAT) {
                mathsat.api.msat_destroy_env(absEnv);
                if (useCache) {
//                    Pair<AbstractFormula, CFAEdge> key = 
//                        new Pair<AbstractFormula, CFAEdge>(
//                                e.getAbstraction(), edge);
                    FeasibilityCacheKey key = new FeasibilityCacheKey(fkey);
                    if (feasibilityCache.containsKey(key)) {
                        assert(feasibilityCache.get(key) == false);
                    }
                    feasibilityCache.put(key, false);
                }
                return new BDDAbstractFormula(bddManager.getZero());
            } else {
                if (useCache) {
//                    Pair<AbstractFormula, CFAEdge> key = 
//                        new Pair<AbstractFormula, CFAEdge>(
//                                e.getAbstraction(), edge);
                    FeasibilityCacheKey key = new FeasibilityCacheKey(fkey);
                    if (feasibilityCache.containsKey(key)) {
                        assert(feasibilityCache.get(key) == true);
                    }
                    feasibilityCache.put(key, true);
                }
            }
        } else {
            ++stats.abstractionNumCachedQueries;
        }
        
        long totBddTime = 0;
        
        int absbdd = bddManager.getOne();

        // check whether each of the predicate is implied in the next state...
        Set<String> predvars = new HashSet<String>();
        int predIndex = -1;
        for (Predicate p : predicates) {
            ++predIndex;
            BDDPredicate bp = (BDDPredicate)p; 
            if (useCache && predVals[predIndex] != NO_VALUE) {
                long startBddTime = System.currentTimeMillis();
                int v = bp.getBDD();
                if (predVals[predIndex] == -1) { // pred is false
                    v = bddManager.not(v);
                    absbdd = bddManager.and(absbdd, v);
                } else if (predVals[predIndex] == 1) { // pred is true
                    absbdd = bddManager.and(absbdd, v);
                }
                long endBddTime = System.currentTimeMillis();
                totBddTime += (endBddTime - startBddTime);
                ++stats.abstractionNumCachedQueries;
            } else {
                Pair<MathsatSymbolicFormula, MathsatSymbolicFormula> pi = 
                    getPredicateNameAndDef(bp);

                // update the SSA map, by instantiating all the uninstantiated 
                // variables that occur in the predicates definitions
                // (at index 1)
                predvars.clear();
                collectVarNames(pi.getSecond().getTerm(), predvars);
                for (String var : predvars) {
                    if (ssa.getIndex(var) < 0) {
                        ssa.setIndex(var, 1);
                    }
                }

                LazyLogger.log(LazyLogger.DEBUG_1, 
                        "CHECKING VALUE OF PREDICATE: ", pi.getFirst());

                // instantiate the definition of the predicate
                MathsatSymbolicFormula inst = 
                    (MathsatSymbolicFormula)mmgr.instantiate(
                            pi.getSecond(), ssa);

                boolean isTrue = false, isFalse = false;
                // check whether this predicate has a truth value in the next 
                // state
                long predTrue = inst.getTerm();
//                predTrue = mathsat.api.msat_make_copy_from(
//                        absEnv, inst.getTerm(), msatEnv);
                long predFalse = mathsat.api.msat_make_not(absEnv, predTrue);

                int ok = mathsat.api.msat_push_backtrack_point(absEnv);
                assert(ok == 0);
                mathsat.api.msat_assert_formula(absEnv, predFalse);
                long res = mathsat.api.msat_solve(absEnv);
                assert(res != mathsat.api.MSAT_UNKNOWN);
                ++stats.abstractionNumMathsatQueries;            
                if (res == mathsat.api.MSAT_UNSAT) {
                    isTrue = true;
                }
                mathsat.api.msat_pop_backtrack_point(absEnv);

                if (isTrue) {
                    long startBddTime = System.currentTimeMillis();
                    int v = bp.getBDD();
                    absbdd = bddManager.and(absbdd, v);
                    long endBddTime = System.currentTimeMillis();
                    totBddTime += (endBddTime - startBddTime);
                } else {
                    // check whether it's false...
                    ok = mathsat.api.msat_push_backtrack_point(absEnv);
                    assert(ok == 0);
                    mathsat.api.msat_assert_formula(absEnv, predTrue);
                    res = mathsat.api.msat_solve(absEnv);
                    ++stats.abstractionNumMathsatQueries;                
                    assert(res != mathsat.api.MSAT_UNKNOWN);
                    if (res == mathsat.api.MSAT_UNSAT) {
                        isFalse = true;
                    }
                    mathsat.api.msat_pop_backtrack_point(absEnv);

                    if (isFalse) {
                        long startBddTime = System.currentTimeMillis();
                        int v = bp.getBDD();
                        v = bddManager.not(v);
                        absbdd = bddManager.and(absbdd, v);
                        long endBddTime = System.currentTimeMillis();
                        totBddTime += (endBddTime - startBddTime);
                    }
                }
                
                if (useCache) {
                    if (predVals[predIndex] != NO_VALUE) {
                        assert(isTrue ? predVals[predIndex] == 1 :
                            (isFalse ? predVals[predIndex] == -1 : 
                                predVals[predIndex] == 0));
                    }
                    CartesianAbstractionCacheKey key = 
//                        new CartesianAbstractionCacheKey(
//                                e.getAbstraction(), edge, p);
                        new CartesianAbstractionCacheKey(fkey, p);
                    byte val = (byte)(isTrue ? 1 : (isFalse ? -1 : 0));
                    cartesianAbstractionCache.put(key, val);
                }
            }
        }
        long solveEndTime = System.currentTimeMillis();

        mathsat.api.msat_destroy_env(absEnv);
        
        // update statistics
        long endTime = System.currentTimeMillis();
        long solveTime = (solveEndTime - solveStartTime) - totBddTime;
        long msatTime = (endTime - startTime) - totBddTime;
        stats.abstractionMaxMathsatTime = 
            Math.max(msatTime, stats.abstractionMaxMathsatTime);
        stats.abstractionMaxBddTime =
            Math.max(totBddTime, stats.abstractionMaxBddTime);
        stats.abstractionMathsatTime += msatTime;
        stats.abstractionBddTime += totBddTime;
        stats.abstractionMathsatSolveTime += solveTime;
        stats.abstractionMaxMathsatSolveTime = 
            Math.max(solveTime, stats.abstractionMaxMathsatSolveTime);

        return new BDDAbstractFormula(absbdd);
    }
            

    // counterexample analysis
    @Override
    public CounterexampleTraceInfo buildCounterexampleTrace(
            SymbolicFormulaManager mgr,
            Deque<ExplicitAbstractElement> abstractTrace) {
        assert(abstractTrace.size() > 1);
        
//        mathsat.api.msat_set_verbosity(1);
        long startTime = System.currentTimeMillis();
        stats.numCallsCexAnalysis++;
        
        // create the DAG formula corresponding to the abstract trace. We create
        // n formulas, one per interpolation group
        SSAMap ssa = new SSAMap();        
        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        
        Vector<SymbolicFormula> f = new Vector<SymbolicFormula>();
        Vector<Boolean> checkHere = new Vector<Boolean>();
        
        LazyLogger.log(LazyLogger.DEBUG_1, "\nBUILDING COUNTEREXAMPLE TRACE\n");
        LazyLogger.log(LazyLogger.DEBUG_1, "ABSTRACT TRACE: ", abstractTrace);
        
        Object[] abstarr = abstractTrace.toArray();
        ExplicitAbstractElement cur = (ExplicitAbstractElement)abstarr[0];
        
        boolean theoryCombinationNeeded = false;
        
        MathsatSymbolicFormula bitwiseAxioms = 
            (MathsatSymbolicFormula)mmgr.makeTrue();
        
        for (int i = 1; i < abstarr.length; ++i) {
            ExplicitAbstractElement e = (ExplicitAbstractElement)abstarr[i];
            CFAEdge found = null;
            for (int j = 0; j < e.getLocation().getNumEnteringEdges(); ++j) {
                CFAEdge edge = e.getLocation().getEnteringEdge(j);
                if (edge.getPredecessor().equals(cur.getLocation())) {
                    found = edge;
                    break;
                }
            }
            assert(found != null);
            Pair<SymbolicFormula, SSAMap> p = null;
            try {
                p = mmgr.makeAnd(mmgr.makeTrue(), found, ssa, false, false);
                checkHere.add(found instanceof AssumeEdge || 
                              found instanceof BlockEdge);
            } catch (UnrecognizedCFAEdgeException e1) {
                e1.printStackTrace();
                System.exit(1);
            }
            
            SSAMap newssa = null;
            SymbolicFormula fm = null;
            if (false) {//i != 1) {
                LazyLogger.log(LazyLogger.DEBUG_3, "SHIFTING: ", p.getFirst(),
                        " WITH SSA: ", ssa);
                p = mmgr.shift(p.getFirst(), ssa);
                newssa = p.getSecond();
                LazyLogger.log(LazyLogger.DEBUG_3, "RESULT: ", p.getFirst(),
                               " SSA: ", newssa);
                newssa.update(ssa);
                fm = p.getFirst();
            } else {
                fm = mmgr.replaceAssignments(
                        (MathsatSymbolicFormula)p.getFirst());
                LazyLogger.log(LazyLogger.DEBUG_3, "INITIAL: ", fm,
                               " SSA: ", p.getSecond());
                newssa = p.getSecond();
            }
            boolean hasUf = mmgr.hasUninterpretedFunctions(
                    (MathsatSymbolicFormula)fm);
            theoryCombinationNeeded |= hasUf;
            f.add(fm);
            ssa = newssa;
            cur = e;
            
            if (hasUf && CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.useBitwiseAxioms")) {
                MathsatSymbolicFormula a = mmgr.getBitwiseAxioms(
                        (MathsatSymbolicFormula)p.getFirst());
                bitwiseAxioms = (MathsatSymbolicFormula)mmgr.makeAnd(
                        bitwiseAxioms, a);
            }
        }
        
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.useBitwiseAxioms")) {
            LazyLogger.log(LazyLogger.DEBUG_3, "ADDING BITWISE AXIOMS TO THE ",
                    "LAST GROUP: ", bitwiseAxioms);
            f.setElementAt(mmgr.makeAnd(f.elementAt(f.size()-1), bitwiseAxioms),
                    f.size()-1);
        }
        
        LazyLogger.log(LazyLogger.DEBUG_3,
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
        if (theoryCombinationNeeded) {
            mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
            mathsat.api.msat_set_theory_combination(env, 
                    mathsat.api.MSAT_COMB_DTC);
        } else {
            mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
            if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useIntegers")) {
                int ok = mathsat.api.msat_set_option(env, "split_eq", "true");
                assert(ok == 0);
//                int ok = mathsat.api.msat_set_option(env, "la_itp_mode", "new");
//                assert(ok == 0);
            }
        }
        // turn off static learning, for trivial problems it's just overhead
        mathsat.api.msat_set_option(env, "sl", "0");
        
        mathsat.api.msat_init_interpolation(env);        
        
        // for each term, create an interpolation group
        int[] groups = new int[terms.length];
        for (int i = 0; i < groups.length; ++i) {
            groups[i] = mathsat.api.msat_create_itp_group(env);
        }
        // then, assert the formulas
        long res = mathsat.api.MSAT_UNKNOWN;

        boolean shortestTrace = CPAMain.cpaConfig.getBooleanValue(
            "cpas.symbpredabs.shortestCexTrace");
        boolean suffixTrace = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.shortestCexTraceUseSuffix");
        
//        long msatSolveTimeStart = System.currentTimeMillis();
//        for (int i = 0; i < terms.length; ++i) {
//            mathsat.api.msat_set_itp_group(env, groups[i]);
//            mathsat.api.msat_assert_formula(env, terms[i]);
//
//            LazyLogger.log(LazyLogger.DEBUG_1,
//                           "Asserting formula: ", 
//                           new MathsatSymbolicFormula(terms[i]),
//                           " in group: ", groups[i]);
//
//            // if shortestTrace is true, we try to find the minimal infeasible
//            // prefix of the trace
//            if (shortestTrace && checkHere.elementAt(i)) {
//                res = mathsat.api.msat_solve(env);
//                assert(res != mathsat.api.MSAT_UNKNOWN);
//                if (res == mathsat.api.MSAT_UNSAT) {
//                    break;
//                }
//            }
//        }
//        // and check satisfiability
//        if (!shortestTrace) {
//            res = mathsat.api.msat_solve(env);
//        }
        long msatSolveTimeStart = System.currentTimeMillis();
        for (int i = suffixTrace ? terms.length-1 : 0; 
             suffixTrace ? i >= 0 : i < terms.length;
             i = (suffixTrace ? i-1 : i+1)) {
            mathsat.api.msat_set_itp_group(env, groups[i]);
            mathsat.api.msat_assert_formula(env, terms[i]);

            LazyLogger.log(LazyLogger.DEBUG_1,
                           "Asserting formula: ", 
                           new MathsatSymbolicFormula(terms[i]),
                           " in group: ", groups[i]);

            // if shortestTrace is true, we try to find the minimal infeasible
            // prefix of the trace
            if (shortestTrace && true) {//checkHere.elementAt(i)) {
                res = mathsat.api.msat_solve(env);
                assert(res != mathsat.api.MSAT_UNKNOWN);
                if (res == mathsat.api.MSAT_UNSAT) {
                    LazyLogger.log(LazyLogger.DEBUG_1,
                                   "TRACE INCONSISTENT AFTER group: ", 
                                   groups[i]);
                    break;
                }
            }
        }
        // and check satisfiability
        if (!shortestTrace) {
            res = mathsat.api.msat_solve(env);
        }
        long msatSolveTimeEnd = System.currentTimeMillis();
        
        assert(res != mathsat.api.MSAT_UNKNOWN);
        
        CounterexampleTraceInfo info = null;
                
        if (res == mathsat.api.MSAT_UNSAT) {
            boolean useBlastWay = CPAMain.cpaConfig.getBooleanValue(
            "cpas.symbpredabs.refinement.useBlastWay");
            Set<Predicate> allPreds = null;
            if (useBlastWay) allPreds = new HashSet<Predicate>();
            int firstIndexBlastWay = -1, lastIndexBlastWay = -1;

            // the counterexample is spurious. Extract the predicates from
            // the interpolants
            info = new CounterexampleTraceInfo(true);
            boolean splitItpAtoms = CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.refinement.splitItpAtoms");
//            UpdateablePredicateMap pmap = new UpdateablePredicateMap();
//            info.setPredicateMap(pmap);
            // how to partition the trace into (A, B) depends on whether
            // there are function calls involved or not: in general, A
            // is the trace from the entry point of the current function
            // to the current point, and B is everything else. To implement
            // this, we keep track of which function we are currently in.
            Stack<Integer> entryPoints = new Stack<Integer>();
            entryPoints.push(0);
            for (int i = 1; i < groups.length; ++i) {
                int start_of_a = entryPoints.peek();
                if (!CPAMain.cpaConfig.getBooleanValue(
                       "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
                    // if we don't want "well-scoped" predicates, we always
                    // cut from the beginning
                    start_of_a = 0;
                }
                		
                int[] groups_of_a = new int[i-start_of_a];
                for (int j = 0; j < groups_of_a.length; ++j) {
                    groups_of_a[j] = groups[j+start_of_a];
                }
                long itp = mathsat.api.msat_get_interpolant(env, groups_of_a);
                assert(!mathsat.api.MSAT_ERROR_TERM(itp));
                
                if (firstIndexBlastWay < 0 &&
                    mathsat.api.msat_term_is_true(itp) == 0) {
                    firstIndexBlastWay = i-1;
                } 
                if (lastIndexBlastWay < 0 &&
                    mathsat.api.msat_term_is_false(itp) != 0) {
                    lastIndexBlastWay = i;
                }
                
                if (CPACheckerLogger.getLevel() <= 
                    LazyLogger.DEBUG_1.intValue()) {
                    StringBuffer buf = new StringBuffer();
                    for (int g : groups_of_a) {
                        buf.append(g);
                        buf.append(" ");
                    }
                    LazyLogger.log(LazyLogger.DEBUG_1, "groups_of_a: ", buf);
                }
                LazyLogger.log(LazyLogger.DEBUG_1,
                               "Got interpolant(", i, "): ",
                               new MathsatSymbolicFormula(itp), " LOCATION: ",
                               ((ExplicitAbstractElement)
                                       abstarr[i-1]).getLocation());
                
                long itpc = mathsat.api.msat_make_copy_from(msatEnv, itp, env);
                boolean nonAtomic = CPAMain.cpaConfig.getBooleanValue(
                        "cpas.symbpredabs.abstraction.explicit." +
                        "nonAtomicPredicates"); 
                Collection<SymbolicFormula> atoms = mmgr.extractAtoms(
                            new MathsatSymbolicFormula(itpc), true, 
                            splitItpAtoms, nonAtomic);
                Set<Predicate> preds = 
                    buildPredicates(env, msatEnv, atoms);
                if (useBlastWay) {
                    allPreds.addAll(preds);
                } else {
                    if (CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.refinement.addPredicatesGlobally")) {
                        for (Object o : abstarr) {
                            ExplicitAbstractElement s = 
                                (ExplicitAbstractElement)o;
                            info.addPredicatesForRefinement(s, preds);
                        }
                    } else {
                        ExplicitAbstractElement s1 = 
                            (ExplicitAbstractElement)abstarr[i-1];
                        info.addPredicatesForRefinement(s1, preds);
                    }
                }
                
                // If we are entering or exiting a function, update the stack 
                // of entry points
                ExplicitAbstractElement e = (ExplicitAbstractElement)abstarr[i];
                if (isFunctionEntry(e)) {
                    LazyLogger.log(LazyLogger.DEBUG_3,
                            "Pushing entry point, function: ",
                            e.getLocation().getFunctionName());
                    entryPoints.push(i);
                } 
                if (isFunctionExit(e)) {
                    LazyLogger.log(LazyLogger.DEBUG_3,
                            "Popping entry point, returning from function: ",
                            e.getLocation().getFunctionName());
                    entryPoints.pop();
                }                
            }
            if (useBlastWay) {
                assert(firstIndexBlastWay >= 0);
                assert(lastIndexBlastWay >= 0);
                for (int i = firstIndexBlastWay; i < lastIndexBlastWay; ++i) {
                    ExplicitAbstractElement s1 = 
                        (ExplicitAbstractElement)abstarr[i];
                    info.addPredicatesForRefinement(s1, allPreds);                    
                }
            }
        } else {
            // this is a real bug, notify the user
            info = new CounterexampleTraceInfo(false);
            info.setConcreteTrace(new ConcreteTraceNoInfo());
            // TODO - reconstruct counterexample
            // For now, we dump the asserted formula to a user-specified file
            String cexPath = CPAMain.cpaConfig.getProperty(
                    "cpas.symbpredabs.refinement.msatCexPath");
            if (cexPath != null) {
                long t = mathsat.api.msat_make_true(env);
                for (int i = 0; i < terms.length; ++i) {
                    t = mathsat.api.msat_make_and(env, t, terms[i]);
                }
                String msatRepr = mathsat.api.msat_to_msat(env, t);
                try {
                    PrintWriter pw = new PrintWriter(new File(cexPath));
                    pw.println(msatRepr);
                    pw.close();
                } catch (FileNotFoundException e) {
                    LazyLogger.log(CustomLogLevel.INFO, 
                            "Failed to save msat Counterexample to file: ",
                            cexPath);
                }
            }
        }
        
        mathsat.api.msat_destroy_env(env);
        
//        mathsat.api.msat_set_verbosity(0);
        
        // update stats
        long endTime = System.currentTimeMillis();
        long totTime = endTime - startTime;
        stats.cexAnalysisTime += totTime;
        stats.cexAnalysisMaxTime = Math.max(totTime, stats.cexAnalysisMaxTime);
        long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;
        stats.cexAnalysisMathsatTime += msatSolveTime;
        stats.cexAnalysisMaxMathsatTime = 
            Math.max(msatSolveTime, stats.cexAnalysisMaxMathsatTime);
        
        return info;
    }

    protected boolean isFunctionExit(ExplicitAbstractElement e) {
        return false; // TODO
//        CFANode inner = e.getLocation();
//        return (inner.getNumLeavingEdges() == 1 && 
//                inner.getLeavingEdge(0) instanceof ReturnEdge);
    }

    protected boolean isFunctionEntry(ExplicitAbstractElement e) {
        return false; // TODO
//        CFANode inner = e.getLocation();
//        return (inner.getNumEnteringEdges() > 0 &&
//                inner.getEnteringEdge(0).getPredecessor() instanceof 
//                FunctionDefinitionNode);
    }

    private Set<Predicate> buildPredicates(long srcenv, long dstenv,
            Collection<SymbolicFormula> atoms) {
        Set<Predicate> ret = new HashSet<Predicate>();
        for (SymbolicFormula atom : atoms) {
            long tt = ((MathsatSymbolicFormula)atom).getTerm();
            assert(!mathsat.api.MSAT_ERROR_TERM(tt));

            String repr = mathsat.api.msat_term_is_atom(tt) != 0 ? 
                          mathsat.api.msat_term_repr(tt) : 
                          ("#" + mathsat.api.msat_term_id(tt));
            long d = mathsat.api.msat_declare_variable(dstenv, 
                    "\"PRED" + repr + "\"",
                    mathsat.api.MSAT_BOOL);
            long var = mathsat.api.msat_make_variable(dstenv, d);            
            assert(!mathsat.api.MSAT_ERROR_TERM(var));
            
            ret.add(makePredicate(var, tt));
        }
        return ret;
    }
    
}
