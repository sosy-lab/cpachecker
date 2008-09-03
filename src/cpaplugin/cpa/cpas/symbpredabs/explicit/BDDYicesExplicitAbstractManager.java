package cpaplugin.cpa.cpas.symbpredabs.explicit;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.BDDAbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.BDDPredicate;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatSymbolicFormulaManager;
import cpaplugin.logging.LazyLogger;

public class BDDYicesExplicitAbstractManager extends
        BDDMathsatExplicitAbstractManager {
    
    private Map<Long, String> msatVarToYicesVar;
    private Map<Long, String> msatToYicesCache;
    private int curVarIndex;
    private int yicesContext;
    private yices.YicesLite yicesManager;
    // restart yices every once in a while, otherwise it starts eating too 
    // much memory
    private final int MAX_NUM_YICES_CALLS = 10000;
    
    public BDDYicesExplicitAbstractManager() {
        super();
        msatVarToYicesVar = new HashMap<Long, String>();
        msatToYicesCache = new HashMap<Long, String>();
        curVarIndex = 1;
        yicesManager = new yices.YicesLite();
        yicesContext = yicesManager.yicesl_mk_context();
        yicesManager.yicesl_set_verbosity((short)0);
        yicesManager.yicesl_set_output_file("/dev/null");
        //System.out.println("USING YICES VERSION: " + 
        //                   yicesManager.yicesl_version());
    }
    
    // returns a pair (declarations, formula)
    private Pair<Collection<String>, String> toYices(MathsatSymbolicFormula f) {
        Stack<Long> toProcess = new Stack<Long>();
        Collection<String> decls = new Vector<String>();
        toProcess.push(f.getTerm());
        while (!toProcess.empty()) {
            long term = toProcess.peek();
            if (msatToYicesCache.containsKey(term)) {
                toProcess.pop();
                continue;
            }
            boolean childrenDone = true;
            String[] children = new String[mathsat.api.msat_term_arity(term)];
            for (int i = 0; i < mathsat.api.msat_term_arity(term); ++i) {
                long c = mathsat.api.msat_term_get_arg(term, i);
                if (msatToYicesCache.containsKey(c)) {
                    children[i] = msatToYicesCache.get(c);
                } else {
                    childrenDone = false;
                    toProcess.push(c);
                }
            }
            if (childrenDone) {
                toProcess.pop();
                if (mathsat.api.msat_term_is_variable(term) != 0) {
                    long d = mathsat.api.msat_term_get_decl(term);
                    String yicesVar = null;
                    if (!msatVarToYicesVar.containsKey(d)) {
                        yicesVar = "v" + (curVarIndex++);
                        String decl = null;
                        if (mathsat.api.msat_term_is_boolean_var(term) != 0) {
                            decl = "(define " + yicesVar + "::bool)"; 
                        } else {
                            decl = "(define " + yicesVar + "::int)";
                        }
                        msatVarToYicesVar.put(d, yicesVar);
                        decls.add(decl);
                    } else {
                        yicesVar = msatVarToYicesVar.get(d);
                    }
                    msatToYicesCache.put(term, yicesVar); 
                } else if (mathsat.api.msat_term_is_uif(term) != 0) {
                    long d = mathsat.api.msat_term_get_decl(term);
                    String yicesFun = null;
                    if (!msatVarToYicesVar.containsKey(d)) {
                        yicesFun = "f" + (curVarIndex++);
                        String tp = "(->";
                        for (int i = 0; i < mathsat.api.msat_term_arity(term);
                             ++i) {
                            tp += " int";
                        }
                        tp += " int)";
                        String decl = "(define " + yicesFun + "::" + tp + ")";
                        msatVarToYicesVar.put(d, yicesFun);
                        decls.add(decl);
                    } else {
                        yicesFun = msatVarToYicesVar.get(d);
                    }
                    String s = "(" + yicesFun;
                    for (String c : children) {
                        s += " " + c;
                    }
                    s += ")";
                    msatToYicesCache.put(term, s);
                } else if (mathsat.api.msat_term_is_number(term) != 0) {
                    msatToYicesCache.put(term, mathsat.api.msat_term_repr(term));
                } else if (mathsat.api.msat_term_is_true(term) != 0) {
                    msatToYicesCache.put(term, "true");
                } else if (mathsat.api.msat_term_is_false(term) != 0) {
                    msatToYicesCache.put(term, "false");
                } else {
                    String op = null;
                    if (mathsat.api.msat_term_is_bool_ite(term) != 0) {
                        op = "ite";
                    } else if (mathsat.api.msat_term_is_and(term) != 0) {
                        op = "and";
                    } else if (mathsat.api.msat_term_is_or(term) != 0) {
                        op = "or";
                    } else if (mathsat.api.msat_term_is_not(term) != 0) {
                        op = "not";
                    } else if (mathsat.api.msat_term_is_implies(term) != 0) {
                        op = "=>";
                    } else if (mathsat.api.msat_term_is_iff(term) != 0) {
                        op = "=";
                    } else if (mathsat.api.msat_term_is_equal(term) != 0) {
                        op = "=";
                    } else if (mathsat.api.msat_term_is_lt(term) != 0) {
                        op = "<";
                    } else if (mathsat.api.msat_term_is_leq(term) != 0) {
                        op = "<=";
                    } else if (mathsat.api.msat_term_is_gt(term) != 0) {
                        op = ">";
                    } else if (mathsat.api.msat_term_is_geq(term) != 0) {
                        op = ">=";
                    } else if (mathsat.api.msat_term_is_plus(term) != 0) {
                        op = "+";
                    } else if (mathsat.api.msat_term_is_minus(term) != 0) {
                        op = "-";
                    } else if (mathsat.api.msat_term_is_times(term) != 0) {
                        op = "*";
                    } else if (mathsat.api.msat_term_is_negate(term) != 0) {
                        op = "-";
                    } else {
                        assert(false);
                    }
                    String s = "(" + op;
                    for (String c : children) {
                        s += " "  + c;
                    }
                    s += ")";
                    msatToYicesCache.put(term, s);
                }                
            }
        }
        return new Pair<Collection<String>, String>(
                decls, msatToYicesCache.get(f.getTerm()));
    }

    protected AbstractFormula buildCartesianAbstraction(
            SymbolicFormulaManager mgr, ExplicitAbstractElement e, 
            ExplicitAbstractElement succ, CFAEdge edge,
            Collection<Predicate> predicates) {
        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        
        long startTime = System.currentTimeMillis();
        
        if (stats.abstractionNumMathsatQueries > MAX_NUM_YICES_CALLS) {
            yicesManager.yicesl_del_context(yicesContext);
            yicesContext = yicesManager.yicesl_mk_context();
            yicesManager.yicesl_set_output_file("/dev/null");
            msatToYicesCache.clear();
            msatVarToYicesVar.clear();
        }
        

        if (isFunctionExit(e)) {
            // we have to take the context before the function call 
            // into account, otherwise we are not building the right 
            // abstraction!
            assert(false); // TODO
        }
        
        Pair<SymbolicFormula, SSAMap> pc = 
            buildConcreteFormula(mmgr, e, succ, edge, false);
        SymbolicFormula f = pc.getFirst();
        SSAMap ssa = pc.getSecond();
        
        Vector<String> toDeclareAfterPop = new Vector<String>();
        
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
            FeasibilityCacheKey key = new FeasibilityCacheKey(f);
            if (feasibilityCache.containsKey(key)) {
                skipFeasibilityCheck = true;
                if (!feasibilityCache.get(key)) {
                    LazyLogger.log(LazyLogger.DEBUG_1, 
                            "CACHED INCONSITENCY FOUND");
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
        
        Pair<Collection<String>, String> toPush =
            toYices((MathsatSymbolicFormula)f);
        
        for (String decl : toPush.getFirst()) {
            yicesCommand(decl);
        }
        
        yicesCommand("(push)");
        yicesCommand("(assert " + toPush.getSecond() + ")");
        
        long solveStartTime = System.currentTimeMillis();        
        
        if (!skipFeasibilityCheck) {
            ++stats.abstractionNumMathsatQueries;
            if (yicesManager.yicesl_inconsistent(yicesContext) != 0) {
                if (useCache) {
                    FeasibilityCacheKey key = new FeasibilityCacheKey(fkey);
                    if (feasibilityCache.containsKey(key)) {
                        assert(feasibilityCache.get(key) == false);
                    }
                    feasibilityCache.put(key, false);
                }
                yicesCommand("(pop)");
                return new BDDAbstractFormula(bddManager.getZero());
            } else {
                if (useCache) {
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
        
//        if (edge instanceof AssumeEdge) {
//            LazyLogger.log(LazyLogger.DEBUG_1, 
//                    "RETURNING PREVIOUS ABSTRACTION");
//            yicesCommand("(pop)");
//            return e.getAbstraction();
//        }
        
        long totBddTime = 0;
        
        int absbdd = bddManager.getOne();

        // check whether each of the predicate is implied in the next state...
        Set<String> predvars = new HashSet<String>();
        int predIndex = -1;
        for (Predicate p : predicates) {
            ++predIndex;

            BDDPredicate bp = (BDDPredicate)p; 

            Pair<MathsatSymbolicFormula, MathsatSymbolicFormula> pi = 
                getPredicateNameAndDef(bp);
            LazyLogger.log(LazyLogger.DEBUG_1, 
                    "CHECKING VALUE OF PREDICATE: ", pi.getFirst());
            
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

                // instantiate the definition of the predicate
                MathsatSymbolicFormula inst = 
                    (MathsatSymbolicFormula)mmgr.instantiate(
                            pi.getSecond(), ssa);
                
                toPush = toYices(inst);
                
                for (String decl : toPush.getFirst()) {
                    yicesManager.yicesl_read(yicesContext, decl);
                    toDeclareAfterPop.add(decl);
                }

                boolean isTrue = false, isFalse = false;
                // check whether this predicate has a truth value in the next 
                // state
                yicesCommand("(push)");
                String predTrue = toPush.getSecond();
                String predFalse = "(not " + predTrue + ")";
                
                yicesCommand("(assert " + predFalse + ")");
                ++stats.abstractionNumMathsatQueries;            
                if (yicesInconsistent()) {
                    isTrue = true;
                }
                yicesCommand("(pop)");

                if (isTrue) {
                    LazyLogger.log(LazyLogger.DEBUG_1, "     TRUE");
                    long startBddTime = System.currentTimeMillis();
                    int v = bp.getBDD();
                    absbdd = bddManager.and(absbdd, v);
                    long endBddTime = System.currentTimeMillis();
                    totBddTime += (endBddTime - startBddTime);
                } else {
                    // check whether it's false...
                    yicesCommand("(push)");
                    yicesCommand("(assert " + predTrue + ")");
                    ++stats.abstractionNumMathsatQueries;            
                    if (yicesInconsistent()) {
                        isFalse = true;
                    }
                    yicesCommand("(pop)");

                    if (isFalse) {
                        LazyLogger.log(LazyLogger.DEBUG_1, "     FALSE");
                        long startBddTime = System.currentTimeMillis();
                        int v = bp.getBDD();
                        v = bddManager.not(v);
                        absbdd = bddManager.and(absbdd, v);
                        long endBddTime = System.currentTimeMillis();
                        totBddTime += (endBddTime - startBddTime);
                    } else {
                        LazyLogger.log(LazyLogger.DEBUG_1, "     X");
                    }
                }
                
                if (useCache) {
                    if (predVals[predIndex] != NO_VALUE) {
                        assert(isTrue ? predVals[predIndex] == 1 :
                            (isFalse ? predVals[predIndex] == -1 : 
                                predVals[predIndex] == 0));
                    }
                    CartesianAbstractionCacheKey key = 
                        new CartesianAbstractionCacheKey(fkey, p);
                    byte val = (byte)(isTrue ? 1 : (isFalse ? -1 : 0));
                    cartesianAbstractionCache.put(key, val);
                }
            }
        }
        long solveEndTime = System.currentTimeMillis();

        yicesCommand("(pop)");
        // if a variable is declared inside a push, after the pop yices will
        // undefine it. But we want our declarations to be persistent, so
        // we basically re-declare the variables here...
        for (String decl : toDeclareAfterPop) {
            yicesCommand(decl);
        }
        
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
        
        
        AbstractFormula ret = new BDDAbstractFormula(absbdd);
        LazyLogger.log(LazyLogger.DEBUG_1, "YICES, RETURNING ABSTRACTION: ",
                toConcrete(mmgr, ret));
                
        return ret;
    }
    
    private int yicesCommand(String cmd) {
        int ret = yicesManager.yicesl_read(yicesContext, cmd);
        if (ret == 0) {
            System.err.println("YICES ERROR: " + 
                    yicesManager.yicesl_get_last_error_message());
        }
        assert(ret != 0);
        return ret;
    }
    
    private boolean yicesInconsistent() {
        return yicesManager.yicesl_inconsistent(yicesContext) != 0;
    }
}
