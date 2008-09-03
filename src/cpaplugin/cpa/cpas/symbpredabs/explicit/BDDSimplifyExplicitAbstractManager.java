package cpaplugin.cpa.cpas.symbpredabs.explicit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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

public class BDDSimplifyExplicitAbstractManager extends
        BDDMathsatExplicitAbstractManager {
    
    private Map<Long, String> msatVarToSimplifyVar;
    private Map<Long, String> msatToSimplifyCache;
    private int curVarIndex;
    private Process simplify;
    private BufferedReader simplifyOut;
    private PrintWriter simplifyIn;
    private PrintWriter dumpQueryWriter;
    
    public BDDSimplifyExplicitAbstractManager() {
        super();
        msatVarToSimplifyVar = new HashMap<Long, String>();
        msatToSimplifyCache = new HashMap<Long, String>();
        curVarIndex = 1;
        try {
            Runtime runtime = Runtime.getRuntime();
            simplify = runtime.exec("Simplify -nosc");
            //simplify = runtime.exec("mysimplify");
            OutputStream in = simplify.getOutputStream();
            InputStream out = simplify.getInputStream();
            simplifyOut = new BufferedReader(new InputStreamReader(out));
            simplifyIn = new PrintWriter(in);
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }
        dumpQueryWriter = null;
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.explicit.abstraction.simplifyDumpQueries")) {
            try {
                dumpQueryWriter = new PrintWriter(
                        new File("simplify_queries.txt"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                dumpQueryWriter = null;
            }
        }
    }
    
    // returns a pair (declarations, formula)
    private String toSimplify(MathsatSymbolicFormula f) {
        Stack<Long> toProcess = new Stack<Long>();
        toProcess.push(f.getTerm());
        while (!toProcess.empty()) {
            long term = toProcess.peek();
            if (msatToSimplifyCache.containsKey(term)) {
                toProcess.pop();
                continue;
            }
            boolean childrenDone = true;
            String[] children = new String[mathsat.api.msat_term_arity(term)];
            for (int i = 0; i < mathsat.api.msat_term_arity(term); ++i) {
                long c = mathsat.api.msat_term_get_arg(term, i);
                if (msatToSimplifyCache.containsKey(c)) {
                    children[i] = msatToSimplifyCache.get(c);
                } else {
                    childrenDone = false;
                    toProcess.push(c);
                }
            }
            if (childrenDone) {
                toProcess.pop();
                if (mathsat.api.msat_term_is_variable(term) != 0) {
                    long d = mathsat.api.msat_term_get_decl(term);
                    String simplifyVar = null;
                    if (!msatVarToSimplifyVar.containsKey(d)) {
                        simplifyVar = "v" + (curVarIndex++);
                        msatVarToSimplifyVar.put(d, simplifyVar);
                    } else {
                        simplifyVar = msatVarToSimplifyVar.get(d);
                    }
                    msatToSimplifyCache.put(term, simplifyVar); 
                } else if (mathsat.api.msat_term_is_uif(term) != 0) {
                    long d = mathsat.api.msat_term_get_decl(term);
                    String simplifyFun = null;
                    if (!msatVarToSimplifyVar.containsKey(d)) {
                        simplifyFun = "f" + (curVarIndex++);
                        msatVarToSimplifyVar.put(d, simplifyFun);
                    } else {
                        simplifyFun = msatVarToSimplifyVar.get(d);
                    }
                    String s = "(" + simplifyFun;
                    for (String c : children) {
                        s += " " + c;
                    }
                    s += ")";
                    msatToSimplifyCache.put(term, s);
                } else if (mathsat.api.msat_term_is_number(term) != 0) {
                    msatToSimplifyCache.put(term, 
                            mathsat.api.msat_term_repr(term));
                } else if (mathsat.api.msat_term_is_true(term) != 0) {
                    msatToSimplifyCache.put(term, "TRUE");
                } else if (mathsat.api.msat_term_is_false(term) != 0) {
                    msatToSimplifyCache.put(term, "FALSE");
                } else if (mathsat.api.msat_term_is_bool_ite(term) != 0) {
                    String s = "(AND (IMPLIES " + children[0] + " " + 
                                 children[1] + ") (IMPLIES (NOT " + 
                                 children[0] + ") " + children[2] + "))";
                    msatToSimplifyCache.put(term, s);
                } else {
                    String op = null;
                    if (mathsat.api.msat_term_is_and(term) != 0) {
                        op = "AND";
                    } else if (mathsat.api.msat_term_is_or(term) != 0) {
                        op = "OR";
                    } else if (mathsat.api.msat_term_is_not(term) != 0) {
                        op = "NOT";
                    } else if (mathsat.api.msat_term_is_implies(term) != 0) {
                        op = "IMPLIES";
                    } else if (mathsat.api.msat_term_is_iff(term) != 0) {
                        op = "IFF";
                    } else if (mathsat.api.msat_term_is_equal(term) != 0) {
                        op = "EQ";
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
                    msatToSimplifyCache.put(term, s);
                }                
            }
        }
        return msatToSimplifyCache.get(f.getTerm());
    }

    protected AbstractFormula buildCartesianAbstraction(
            SymbolicFormulaManager mgr, ExplicitAbstractElement e, 
            ExplicitAbstractElement succ, CFAEdge edge,
            Collection<Predicate> predicates) {
        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        
        long startTime = System.currentTimeMillis();

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
        
        String toPush = toSimplify((MathsatSymbolicFormula)f);
        
        long solveStartTime = System.currentTimeMillis();        
        
        if (!skipFeasibilityCheck) {
            ++stats.abstractionNumMathsatQueries;
            if (simplifyValid("(IMPLIES " + toPush + " FALSE)")) {
                if (useCache) {
                    FeasibilityCacheKey key = new FeasibilityCacheKey(fkey);
                    if (feasibilityCache.containsKey(key)) {
                        assert(feasibilityCache.get(key) == false);
                    }
                    feasibilityCache.put(key, false);
                }
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
        
        simplifyPush(toPush);
        
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
                
                toPush = toSimplify(inst);
                
                boolean isTrue = false, isFalse = false;
                // check whether this predicate has a truth value in the next 
                // state
                String predTrue = toPush;
                String predFalse = "(NOT " + predTrue + ")";
                
                ++stats.abstractionNumMathsatQueries;            
                if (simplifyValid(predTrue)) {
                    isTrue = true;
                }

                if (isTrue) {
                    long startBddTime = System.currentTimeMillis();
                    int v = bp.getBDD();
                    absbdd = bddManager.and(absbdd, v);
                    long endBddTime = System.currentTimeMillis();
                    totBddTime += (endBddTime - startBddTime);
                } else {
                    // check whether it's false...
                    ++stats.abstractionNumMathsatQueries;            
                    if (simplifyValid(predFalse)) {
                        isFalse = true;
                    }

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
                        new CartesianAbstractionCacheKey(fkey, p);
                    byte val = (byte)(isTrue ? 1 : (isFalse ? -1 : 0));
                    cartesianAbstractionCache.put(key, val);
                }
            }
        }
        long solveEndTime = System.currentTimeMillis();

        simplifyPop();
        
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
    
    private void simplifyPush(String formula) {
        String s = "(BG_PUSH " + formula + ")";
        simplifyIn.println(s);
        simplifyIn.flush();
        if (dumpQueryWriter != null) {
            dumpQueryWriter.println(s);
            dumpQueryWriter.flush();
        }
    }
    
    private void simplifyPop() {
        simplifyIn.println("(BG_POP)");
        simplifyIn.flush();
        if (dumpQueryWriter != null) {
            dumpQueryWriter.println("(BG_POP)");
            dumpQueryWriter.flush();
        }
    }
    
    private boolean simplifyValid(String formula) {
        simplifyIn.println(formula);
        simplifyIn.flush();
        if (dumpQueryWriter != null) {
            dumpQueryWriter.println(formula);
            dumpQueryWriter.flush();
        }        
        String status = null;
        try {
            status = simplifyOut.readLine();
            while (status != null && status.isEmpty()) {
                status = simplifyOut.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        assert(status != null);
        if (status.contains("Valid.")) {
            return true;
        } else if (status.contains("Invalid.")) {
            return false;
        } else {
            System.err.println("BAD ANSWER FROM SIMPLIFY: '" + status + "'");
            assert(false);
        }
        return false;
    }
}
