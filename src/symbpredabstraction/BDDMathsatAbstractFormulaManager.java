package symbpredabstraction;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import mathsat.AllSatModelCallback;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;

public class BDDMathsatAbstractFormulaManager implements AbstractFormulaManager{
    
    /**
     * callback used to build the predicate abstraction of a formula
     * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
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
            for (int i = 0; i < model.length; ++i) {
                long t = mathsat.api.msat_make_copy_from(
                        msatEnv, model[i], absEnv);
                int v;
                if (mathsat.api.msat_term_is_not(t) != 0) {
                    t = mathsat.api.msat_term_get_arg(t, 0);
                    assert(msatVarToBddPredicate.containsKey(t));                    
                    v = msatVarToBddPredicate.get(t);
                    v = bddManager.not(v);
                } else {
                    v = msatVarToBddPredicate.get(t);
                }
                m = bddManager.and(m, v);
            }
            // now, add the model to the bdd
            bdd = bddManager.or(bdd, m);
            
//            StringBuffer modelStrBuf = new StringBuffer();
//            for (int i = 0; i < model.length; ++i) {
//                modelStrBuf.append(mathsat.api.msat_term_repr(model[i]));
//                modelStrBuf.append(" ");
//            }
//            CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
//                    "GOT MODEL: " + modelStrBuf.toString());
            
        }
    }

    protected JavaBDD bddManager;

    // a predicate is just a BDD index for a variable (see BDDPredicate). Here
    // we keep the mapping BDD index -> (MathSAT variable, MathSAT atom)
    private Map<Integer, Pair<Long, Long>> bddPredicateToMsatAtom;
    // and the mapping MathSAT variable -> BDD index
    private Map<Long, Integer> msatVarToBddPredicate;
    // and MathSAT atom -> BDD index
    private Map<Long, Integer> msatAtomToBddPredicate;
    
    private boolean entailsUseCache;
    private Map<Pair<AbstractFormula, AbstractFormula>, Boolean> entailsCache;
    
    
    public BDDMathsatAbstractFormulaManager() {
        //bddManager = new JDD();
        bddManager = new JavaBDD();
        bddPredicateToMsatAtom = new HashMap<Integer, Pair<Long, Long>>();
        msatVarToBddPredicate = new HashMap<Long, Integer>();
        msatAtomToBddPredicate = new HashMap<Long, Integer>();
        entailsUseCache = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useCache");
        if (entailsUseCache) {
            entailsCache =
                new HashMap<Pair<AbstractFormula, AbstractFormula>, Boolean>();
        }
    }

    /**
     * creates a BDDPredicate from the Boolean mathsat variable (msatVar) and
     * the atoms that defines it (the msatAtom)
     */
    public Predicate makePredicate(long msatVar, long msatAtom) {
        if (msatVarToBddPredicate.containsKey(msatVar)) {
            int bddVar = msatVarToBddPredicate.get(msatVar);
            int var = bddManager.getVar(bddVar);
            return new BDDPredicate(bddVar, var);
        } else {
            int bddVar = bddManager.createVar();

            LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                           "CREATED PREDICATE: bddVar: ",
                           Integer.toString(bddManager.getVar(bddVar)),  
                           ", msatAtom: ", 
                           new MathsatSymbolicFormula(msatAtom));
            
            int var = bddManager.getVar(bddVar);
            bddPredicateToMsatAtom.put(var, 
                    new Pair<Long, Long>(msatVar, msatAtom));
            msatVarToBddPredicate.put(msatVar, bddVar);
            msatAtomToBddPredicate.put(msatAtom, bddVar);
            return new BDDPredicate(bddVar, var);
        }
    }

    
    public boolean entails(AbstractFormula f1, AbstractFormula f2) {
        // check entailment using BDDs: create the BDD representing 
        // the implication, and check that it is the TRUE formula
        Pair<AbstractFormula, AbstractFormula> key = null;
        if (entailsUseCache) {
            key = new Pair<AbstractFormula, AbstractFormula>(f1, f2);
            if (entailsCache.containsKey(key)) {
                return entailsCache.get(key);
            }
        }
        int imp = bddManager.imp(((BDDAbstractFormula)f1).getBDD(),
                                 ((BDDAbstractFormula)f2).getBDD());
        boolean yes = (imp == bddManager.getOne());
        if (entailsUseCache) {
            assert(key != null);
            entailsCache.put(key, yes);
        }
        return yes;
    }
    
    public void collectVarNames(long term, Set<String> vars) {
        Stack<Long> toProcess = new Stack<Long>();
        toProcess.push(term);
        // TODO - this assumes the term is small! There is no memoizing yet!!
        while (!toProcess.empty()) {
            long t = toProcess.pop();
            if (mathsat.api.msat_term_is_variable(t) != 0) {
                vars.add(mathsat.api.msat_term_repr(t));
            } else {
                for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
                    toProcess.push(mathsat.api.msat_term_get_arg(t, i));
                }
            }
        }
    }
    
    // return value is:
    // [mathsat term for \bigwedge_preds (var <-> def),
    //  list of important terms (the names of the preds),
    //   list of variables occurring in the definitions of the preds]
    protected Object[] buildPredList(
    		MathsatSymbPredAbsFormulaManager mmgr, 
            Collection<Predicate> predicates) {
        long msatEnv = mmgr.getMsatEnv();
        long[] important = new long[predicates.size()];
        Set<String> allvars = new HashSet<String>();
        long preddef = mathsat.api.msat_make_true(msatEnv);
        int i = 0;
        for (Predicate p : predicates) {
            BDDPredicate bp = (BDDPredicate)p;
            int idx = bp.getBDD();
            int bddvar = bddManager.getVar(idx);
            long var = bddPredicateToMsatAtom.get(bddvar).getFirst();
            long def = bddPredicateToMsatAtom.get(bddvar).getSecond();
            collectVarNames(def, allvars);
            important[i++] = var;
            // build the mathsat (var <-> def)
            long iff = mathsat.api.msat_make_iff(msatEnv, var, def);
            assert(!mathsat.api.MSAT_ERROR_TERM(iff));
            // and add it to the list of definitions
            preddef = mathsat.api.msat_make_and(msatEnv, preddef, iff);
        }
        return new Object[]{preddef, important, allvars};
    }

    public Pair<MathsatSymbolicFormula, MathsatSymbolicFormula> 
      getPredicateNameAndDef(BDDPredicate p) {
        int idx = p.getBDD();
        int bddvar = bddManager.getVar(idx);
        long var = bddPredicateToMsatAtom.get(bddvar).getFirst();
        long def = bddPredicateToMsatAtom.get(bddvar).getSecond();
        return new Pair<MathsatSymbolicFormula, MathsatSymbolicFormula>(
                new MathsatSymbolicFormula(var), 
                new MathsatSymbolicFormula(def));
    }

    /**
     * This should compute the predicate abstraction of the given input
     * formula wrt. the given list of predicates. However, this method is not
     * used anymore, and should probably be considered deprecated (maybe plain
     * broken also :-). See BDDMathsatExplicitFormulaManager and
     * BDDMathsatSummaryFormulaManager for alternatives that are currently
     * used.
     */
    public AbstractFormula toAbstract(SymbolicFormulaManager mgr,
            SymbolicFormula f, SSAMap ssa, Collection<Predicate> predicates) {
    	MathsatSymbPredAbsFormulaManager mmgr = (MathsatSymbPredAbsFormulaManager)mgr;
        MathsatSymbolicFormula mf = (MathsatSymbolicFormula)f;
        
        long absEnv = mathsat.api.msat_create_env();
        
        //mathsat.api.msat_set_verbosity(10);
        
        long msatEnv = mmgr.getMsatEnv();
        
        long term = mathsat.api.msat_make_copy_from(absEnv, mf.getTerm(), 
                                                    msatEnv);
        assert(!mathsat.api.MSAT_ERROR_TERM(term));
        
        // build the definition of the predicates, and instantiate them
        Object[] predinfo = buildPredList(mmgr, predicates);
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
        // instantiate the definitions with the right SSA
        MathsatSymbolicFormula inst = (MathsatSymbolicFormula)mmgr.instantiate(
                new MathsatSymbolicFormula(preddef), ssa);
        preddef = mathsat.api.msat_make_copy_from(absEnv, inst.getTerm(), 
                                                  msatEnv);
        // build the formula and send it to the absEnv
        long formula = mathsat.api.msat_make_and(absEnv, term, preddef);
        mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_UF);
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useIntegers")) {
            mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LIA);
        } else {
            mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LRA);
        }
        mathsat.api.msat_set_theory_combination(absEnv, 
                                                mathsat.api.MSAT_COMB_DTC);
        mathsat.api.msat_assert_formula(absEnv, formula);

        int abs = bddManager.getZero();
        AllSatCallback func = new AllSatCallback(abs, msatEnv, absEnv);
        int numModels = mathsat.api.msat_all_sat(absEnv, important, func);
        assert(numModels != -1);
        
        mathsat.api.msat_destroy_env(absEnv);
        
        if (numModels == -2) {
            abs = bddManager.getOne();
            return new BDDAbstractFormula(abs);
        } else {
            return new BDDAbstractFormula(func.getBDD());
        }
    }
    

    /**
     * Given an abstract formula (which is a BDD over the predicates), build
     * its concrete representation (which is a MathSAT formula corresponding
     * to the BDD, in which each predicate is replaced with its definition)
     */
    public SymbolicFormula toConcrete(SymbolicFormulaManager mgr,
            AbstractFormula af) {
    	MathsatSymbPredAbsFormulaManager mmgr = (MathsatSymbPredAbsFormulaManager)mgr;
        BDDAbstractFormula bddaf = (BDDAbstractFormula)af;
        int bdd = bddaf.getBDD();
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
            Integer c1 = bddManager.getThen(n);
            Integer c2 = bddManager.getElse(n);
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
        
        return new MathsatSymbolicFormula(cache.get(bdd));
    }

    
    public boolean isFalse(AbstractFormula f) {
        return ((BDDAbstractFormula)f).getBDD() == bddManager.getZero();
    }

    
    public AbstractFormula makeTrue() {
        int t = bddManager.getOne();
        return new BDDAbstractFormula(t);
    }

    public JavaBDD getBddManager(){
    	return bddManager;
    }
}
