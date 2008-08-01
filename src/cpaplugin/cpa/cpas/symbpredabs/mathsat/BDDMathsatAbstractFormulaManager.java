package cpaplugin.cpa.cpas.symbpredabs.mathsat;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import jdd.bdd.BDD;
import mathsat.AllSatModelCallback;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;

public class BDDMathsatAbstractFormulaManager implements AbstractFormulaManager{
    
    /**
     * callback used to build the predicate abstraction of a formula
     * @author alb
     */
    public class AllSatCallback implements AllSatModelCallback {
        private long msatEnv;
        private long absEnv;
        private int bdd;
        
        public AllSatCallback(int bdd, long msatEnv, long absEnv) {
            this.bdd = bdd;
            this.msatEnv = msatEnv;
            this.absEnv = absEnv;
        }
        
        public int getBDD() { return bdd; }

        
        public void callback(long[] model) {
            // the abstraction is created simply by taking the disjunction
            // of all the models found by msat_all_sat, and storing them 
            // in a BDD
            // first, let's create the BDD corresponding to the model
            int m = bddManager.getOne();
            bddManager.ref(m);
            for (int i = 0; i < model.length; ++i) {
                long t = mathsat.api.msat_make_copy_from(
                        msatEnv, model[i], absEnv);
                int v;
                if (mathsat.api.msat_term_is_not(t) != 0) {
                    t = mathsat.api.msat_term_get_arg(t, 0);
                    assert(msatVarToBddPredicate.containsKey(t));                    
                    v = msatVarToBddPredicate.get(t);
                    bddManager.ref(v);
                    v = bddManager.not(v);
                    bddManager.ref(v);
                } else {
                    v = msatVarToBddPredicate.get(t);
                    bddManager.ref(v);
                }
                m = bddManager.and(m, v);
                bddManager.ref(m);
            }
            // now, add the model to the bdd
            bdd = bddManager.or(bdd, m);
            bddManager.ref(bdd);
            
//            StringBuffer modelStrBuf = new StringBuffer();
//            for (int i = 0; i < model.length; ++i) {
//                modelStrBuf.append(mathsat.api.msat_term_repr(model[i]));
//                modelStrBuf.append(" ");
//            }
//            CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
//                    "GOT MODEL: " + modelStrBuf.toString());
            
        }
    }

    protected BDD bddManager;
    private static final int INITIAL_BDD_NODE_SIZE = 1000;
    private static final int INITIAL_BDD_CACHE_SIZE = 1000;

    // a predicate is just a BDD index for a variable (see BDDPredicate). Here
    // we keep the mapping BDD index -> (MathSAT variable, MathSAT atom)
    private Map<Integer, Pair<Long, Long>> bddPredicateToMsatAtom;
    // and the mapping MathSAT variable -> BDD index
    private Map<Long, Integer> msatVarToBddPredicate;
    // and MathSAT atom -> BDD index
    private Map<Long, Integer> msatAtomToBddPredicate;
    
    
    public BDDMathsatAbstractFormulaManager() {
        bddManager = new BDD(INITIAL_BDD_NODE_SIZE, INITIAL_BDD_CACHE_SIZE);
        bddPredicateToMsatAtom = new HashMap<Integer, Pair<Long, Long>>();
        msatVarToBddPredicate = new HashMap<Long, Integer>();
        msatAtomToBddPredicate = new HashMap<Long, Integer>();
    }
    
    public Predicate makePredicate(long msatVar, long msatAtom) {
        if (msatVarToBddPredicate.containsKey(msatVar)) {
            return new BDDPredicate(msatVarToBddPredicate.get(msatVar));
        } else {
            int bddVar = bddManager.createVar();
//            System.err.println("CREATED PREDICATE: bddVar: " +
//                    Integer.toString(bddVar) + "(" + 
//                    Integer.toString(bddManager.getVar(bddVar)) + ")" + 
//                    ", msatVar: " + 
//                    mathsat.api.msat_term_repr(msatVar) + 
//                    ", msatAtom: " + mathsat.api.msat_term_repr(msatAtom));
            
            bddManager.ref(bddVar);
            int var = bddManager.getVar(bddVar);
            bddPredicateToMsatAtom.put(var, 
                    new Pair<Long, Long>(msatVar, msatAtom));
            msatVarToBddPredicate.put(msatVar, bddVar);
            msatAtomToBddPredicate.put(msatAtom, bddVar);
            return new BDDPredicate(bddVar);
        }
    }

    
    public boolean entails(AbstractFormula f1, AbstractFormula f2) {
        // check entailment using BDDs: create the BDD representing 
        // the implication, and check that it is the TRUE formula
        int imp = bddManager.imp(((BDDAbstractFormula)f1).getBDD(),
                                 ((BDDAbstractFormula)f2).getBDD());
        bddManager.ref(imp);
        boolean yes = (imp == bddManager.getOne());
        bddManager.deref(imp);
        return yes;
    }
    
    protected Pair<Long, long[]> buildPredList(
            MathsatSymbolicFormulaManager mmgr, 
            Collection<Predicate> predicates) {
        long msatEnv = mmgr.getMsatEnv();
        long[] important = new long[predicates.size()];
        long preddef = mathsat.api.msat_make_true(msatEnv);
        int i = 0;
        for (Predicate p : predicates) {
            BDDPredicate bp = (BDDPredicate)p;
            int idx = bp.getBDDVar();
            int bddvar = bddManager.getVar(idx);
            long var = bddPredicateToMsatAtom.get(bddvar).getFirst();
            long def = bddPredicateToMsatAtom.get(bddvar).getSecond();
            important[i++] = var;
            // build the mathsat (var <-> def)
            long iff = mathsat.api.msat_make_iff(msatEnv, var, def);
            assert(!mathsat.api.MSAT_ERROR_TERM(iff));
            // and add it to the list of definitions
            preddef = mathsat.api.msat_make_and(msatEnv, preddef, iff);
        }
        return new Pair<Long, long[]>(preddef, important);
    }

    
    public AbstractFormula toAbstract(SymbolicFormulaManager mgr,
            SymbolicFormula f, SSAMap ssa, Collection<Predicate> predicates) {
        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        MathsatSymbolicFormula mf = (MathsatSymbolicFormula)f;
        
        long absEnv = mathsat.api.msat_create_env();
        
        //mathsat.api.msat_set_verbosity(10);
        
        long msatEnv = mmgr.getMsatEnv();
        
        long term = mathsat.api.msat_make_copy_from(absEnv, mf.getTerm(), 
                                                    msatEnv);
        assert(!mathsat.api.MSAT_ERROR_TERM(term));
        
        // build the definition of the predicates, and instantiate them
//        long[] important = new long[predicates.size()];
//        long preddef = mathsat.api.msat_make_true(msatEnv);
//        int i = 0;
//        for (Predicate p : predicates) {
//            BDDPredicate bp = (BDDPredicate)p;
//            int idx = bp.getBDDVar();
//            long var = bddPredicateToMsatAtom.get(idx).getFirst();
//            long def = bddPredicateToMsatAtom.get(idx).getSecond();
//            important[i++] = var;
//            // build the mathsat (var <-> def)
//            long iff = mathsat.api.msat_make_iff(msatEnv, var, def);
//            assert(!mathsat.api.MSAT_ERROR_TERM(iff));
//            // and add it to the list of definitions
//            preddef = mathsat.api.msat_make_and(msatEnv, preddef, iff);
//        }
        Pair<Long, long[]> predlist = buildPredList(mmgr, predicates);
        long preddef = predlist.getFirst();
        long[] important = predlist.getSecond();
        for (int i = 0; i < important.length; ++i) {
            important[i] = mathsat.api.msat_make_copy_from(
                    absEnv, important[i], msatEnv); 
        }
        // instantiate the definitions with the right SSA
        MathsatSymbolicFormula inst = (MathsatSymbolicFormula)mmgr.instantiate(
                new MathsatSymbolicFormula(preddef), ssa);
        preddef = mathsat.api.msat_make_copy_from(absEnv, inst.getTerm(), 
                                                  msatEnv);
        // build the formula and send it to the absEnv
        long formula = mathsat.api.msat_make_and(absEnv, term, preddef);
        mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_UF);
        mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LRA);
        mathsat.api.msat_set_theory_combination(absEnv, 
                                                mathsat.api.MSAT_COMB_DTC);
        mathsat.api.msat_assert_formula(absEnv, formula);

        int abs = bddManager.getZero();
        bddManager.ref(abs);
        AllSatCallback func = new AllSatCallback(abs, msatEnv, absEnv);
        int numModels = mathsat.api.msat_all_sat(absEnv, important, func);
        assert(numModels != -1);
        
        mathsat.api.msat_destroy_env(absEnv);
        
        if (numModels == -2) {
            bddManager.deref(abs);
            abs = bddManager.getOne();
            bddManager.ref(abs);
            return new BDDAbstractFormula(abs);
        } else {
            return new BDDAbstractFormula(func.getBDD());
        }
    }
    
    
    public SymbolicFormula toConcrete(SymbolicFormulaManager mgr,
            AbstractFormula af) {
        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        BDDAbstractFormula bddaf = (BDDAbstractFormula)af;
        int bdd = bddaf.getBDD();
        bddManager.ref(bdd);
        long msatEnv = mmgr.getMsatEnv();
        
        Map<Integer, Long> cache = new HashMap<Integer, Long>();
        Stack<Integer> toProcess = new Stack<Integer>();
        
        cache.put(bddManager.getOne(), mathsat.api.msat_make_true(msatEnv));
        cache.put(bddManager.getZero(), mathsat.api.msat_make_false(msatEnv));
        
        toProcess.push(new Integer(bdd));
        while (!toProcess.empty()) {
            Integer n = toProcess.peek();
            if (cache.containsKey(n)) {
                toProcess.pop();
                continue;
            }
            boolean childrenDone = true;
            long m1 = mathsat.api.MSAT_MAKE_ERROR_TERM();
            long m2 = mathsat.api.MSAT_MAKE_ERROR_TERM();
            Integer c1 = bddManager.getHigh(n);
            Integer c2 = bddManager.getLow(n);
            if (!cache.containsKey(c1)) {
                toProcess.push(c1);
                childrenDone = false;
            } else {
                m1 = cache.get(c1);
            }
            if (!cache.containsKey(c2)) {
                toProcess.push(c2);
                childrenDone = false;
            } else {
                m2 = cache.get(c2);
            }
            if (childrenDone) {
                assert(!mathsat.api.MSAT_ERROR_TERM(m1));
                assert(!mathsat.api.MSAT_ERROR_TERM(m2));

                toProcess.pop();
                Integer var = bddManager.getVar(n);
                
                assert(bddPredicateToMsatAtom.containsKey(var));
                
                long matom = bddPredicateToMsatAtom.get(var).getSecond();
                long ite = mathsat.api.msat_make_ite(msatEnv, matom, m1, m2);
                cache.put(n, ite);
            }
        }
        
        assert(cache.containsKey(bdd));
        
        bddManager.deref(bdd);
        
        return new MathsatSymbolicFormula(cache.get(bdd));
    }

    
    public boolean isFalse(AbstractFormula f) {
        return ((BDDAbstractFormula)f).getBDD() == bddManager.getZero();
    }

    
    public AbstractFormula makeTrue() {
        int t = bddManager.getOne();
        bddManager.ref(t);
        return new BDDAbstractFormula(t);
    }

}
