package cpaplugin.cpa.cpas.symbpredabs.mathsat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.TheoremProver;

public class SimplifyTheoremProver implements TheoremProver {

    private Map<Long, String> msatVarToSimplifyVar;
    private Map<Long, String> msatToSimplifyCache;
    private Map<String, Long> simplifyPredToMsat;    
    private int curVarIndex;
    private Process simplify;
    private BufferedReader simplifyOut;
    private PrintWriter simplifyIn;
    private PrintWriter dumpQueryWriter;
    private MathsatSymbolicFormulaManager mmgr;

    private Process simplifyWithCex;
    private PrintWriter simplifyWithCexIn;
    private BufferedReader simplifyWithCexOut;

    public SimplifyTheoremProver(MathsatSymbolicFormulaManager mgr) {
        msatVarToSimplifyVar = new HashMap<Long, String>();
        msatToSimplifyCache = new HashMap<Long, String>();
        simplifyPredToMsat = new HashMap<String, Long>();
        curVarIndex = 1;
        simplify = null;
        simplifyIn = null;
        simplifyOut = null;
        dumpQueryWriter = null;
        mmgr = mgr;
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
        simplifyWithCex = null;
        simplifyWithCexIn = null;
        simplifyWithCexOut = null;
    }

    @Override
    public void init(int purpose) {
        if (simplify == null) {
            initSimplify(false);
        }
    }

    @Override
    public boolean isUnsat(SymbolicFormula f) {
        return simplifyValid("(NOT " + toSimplify(f) + ")");
    }

    @Override
    public void pop() {
        simplifyPop();
    }

    @Override
    public void push(SymbolicFormula f) {
        simplifyPush(toSimplify(f));
    }

    @Override
    public void reset() {
    }

    @Override
    public int allSat(SymbolicFormula f, Vector<SymbolicFormula> important,
            AllSatCallback callback) {
        // first, initialize simplify with model generation support
        PrintWriter savedIn = simplifyIn;            
        BufferedReader savedOut = simplifyOut;
        if (simplifyWithCex == null) {
            Process savedProc = simplify;
            initSimplify(true);
            simplifyWithCex = simplify;
            simplify = savedProc;
            simplifyWithCexIn = simplifyIn;
            simplifyWithCexOut = simplifyOut;
        } 
        simplifyIn = simplifyWithCexIn;
        simplifyOut = simplifyWithCexOut;
                
        // build the Simplify representation of the formula...
        String simplifyFormula = toSimplify(f);
        // ...and of the important symbols
        Set<String> simplifyPreds = toSimplifyPreds(important);

        // remember how many pop commands to issue - one per blocking clause
        int numPopsNeeded = 0;        
        // assert the initial formula
        String impl = "(IMPLIES " + simplifyFormula + " FALSE)";
        int numModels = 0;        
        Vector<SymbolicFormula> outModel = null;
        
        while (true) {
            long[] model = simplifyGetCounterexample(impl, mmgr, simplifyPreds);
            if (model == null) {
                break; // context is inconsistent now
            }
            if (model.length == 0) {
                numModels = -2;
                break;
            } 

            if (outModel == null) {
                outModel = new Vector<SymbolicFormula>();
            }
            outModel.clear();
            outModel.ensureCapacity(model.length);
            for (long m : model) {
                outModel.add(new MathsatSymbolicFormula(m));
            }
            
            ++numModels;
            callback.modelFound(outModel);
            // add the model as a blocking clause
            StringBuffer buf = new StringBuffer();
            //buf.append("(or ");
            for (long t : model) {
                if (mathsat.api.msat_term_is_not(t) != 0) {
                    t = mathsat.api.msat_term_get_arg(t, 0);
                    assert(mathsat.api.msat_term_is_boolean_var(t) != 0);
                    long d = mathsat.api.msat_term_get_decl(t);
                    String sv = msatVarToSimplifyVar.get(d);
                    assert(sv != null);
                    buf.append(sv + " ");
                } else {
                    assert(mathsat.api.msat_term_is_boolean_var(t) != 0);
                    long d = mathsat.api.msat_term_get_decl(t);
                    String sv = msatVarToSimplifyVar.get(d);
                    assert(sv != null);
                    buf.append("(NOT " + sv + ") ");
                }
            }
            //buf.append(")");
            ++numPopsNeeded;
            if (model.length == 1) {
                simplifyPush(buf.toString());
            } else {
                simplifyPush("(OR " + buf + ")");
            }
        }

        // restore context
        for (int i = 0; i < numPopsNeeded; ++i) {
            simplifyPop();
        }
        
        // and restore the streams
        simplifyIn = savedIn;
        simplifyOut = savedOut;

        return numModels;
    }

    private void initSimplify(boolean counterexamplesNeeded) {
        String cmdline = "Simplify";
        if (!counterexamplesNeeded) {
            cmdline += " -nosc";
        }
        try {
            Runtime runtime = Runtime.getRuntime();
            simplify = runtime.exec(cmdline);
            OutputStream in = simplify.getOutputStream();
            InputStream out = simplify.getInputStream();
            simplifyOut = new BufferedReader(new InputStreamReader(out));
            simplifyIn = new PrintWriter(in);
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }
    }

    private boolean isTermIteAssignment(long term) {
        return (mathsat.api.msat_term_is_equal(term) != 0 &&
                (mathsat.api.msat_term_is_term_ite(
                        mathsat.api.msat_term_get_arg(term, 0)) != 0 ||
                        mathsat.api.msat_term_is_term_ite(
                                mathsat.api.msat_term_get_arg(
                                        term, 1)) != 0));    
    }

    private String toSimplify(SymbolicFormula f) {
        return toSimplify(((MathsatSymbolicFormula)f).getTerm());
    }

    private String toSimplify(long f) {
        Stack<Long> toProcess = new Stack<Long>();
        toProcess.push(f);
        while (!toProcess.empty()) {
            long term = toProcess.peek();
            if (msatToSimplifyCache.containsKey(term)) {
                toProcess.pop();
                continue;
            }
            boolean childrenDone = true;            
            String[] children = new String[mathsat.api.msat_term_arity(term)];
            if (isTermIteAssignment(term)) {
                System.out.println("ERROR!!: " + mathsat.api.msat_term_repr(term));
                assert(false);
                children = new String[4];
                long c1 = mathsat.api.msat_term_get_arg(term, 0);
                long c2 = mathsat.api.msat_term_get_arg(term, 1);
                if (mathsat.api.msat_term_is_term_ite(c1) != 0) {
                    long tmp = c2;
                    c2 = c1;
                    c1 = tmp;
                }
                assert(mathsat.api.msat_term_is_variable(c1) != 0);
                long[] args = new long[]{ 
                        c1, 
                        mathsat.api.msat_term_get_arg(c2, 0),
                        mathsat.api.msat_term_get_arg(c2, 1),
                        mathsat.api.msat_term_get_arg(c2, 2)
                };
                for (int i = 0; i < args.length; ++i) {
                    long c = args[i];
                    if (msatToSimplifyCache.containsKey(c1)) {
                        children[i] = msatToSimplifyCache.get(c);
                    } else {
                        childrenDone = false;
                        toProcess.push(c);
                    }
                }
            } else {
                for (int i = 0; i < mathsat.api.msat_term_arity(term); ++i) {
                    long c = mathsat.api.msat_term_get_arg(term, i);
                    if (msatToSimplifyCache.containsKey(c)) {
                        children[i] = msatToSimplifyCache.get(c);
                    } else {
                        childrenDone = false;
                        toProcess.push(c);
                    }
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
                    String num = mathsat.api.msat_term_repr(term);
                    boolean neg = num.startsWith("-");
                    if (neg) num = num.substring(1);
                    int idx = num.indexOf('/');
                    if (idx >= 0) {
                        // Simplify doesn't like fractions, we must write
                        // x/y as (/ x y)
                        num = "(/ " + num.substring(0, idx) + " " + 
                        num.substring(idx+1) + ")";
                    }
                    if (neg) {
                        num = "(- 0 " + num + ")";
                    }
                    msatToSimplifyCache.put(term, num); 
                } else if (mathsat.api.msat_term_is_true(term) != 0) {
                    msatToSimplifyCache.put(term, "TRUE");
                } else if (mathsat.api.msat_term_is_false(term) != 0) {
                    msatToSimplifyCache.put(term, "FALSE");
                } else if (mathsat.api.msat_term_is_bool_ite(term) != 0) {
                    String s = "(AND (IMPLIES " + children[0] + " " + 
                    children[1] + ") (IMPLIES (NOT " + 
                    children[0] + ") " + children[2] + "))";
                    msatToSimplifyCache.put(term, s);
                } else if (isTermIteAssignment(term)) {
                    // we have to lift the ite to the boolean level
                    String s = "(AND (IMPLIES " + children[1] + " (EQ " +
                    children[0] + " " + children[2] + ")) (IMPLIES (NOT " +
                    children[1] + ") (EQ " + children[0] + " " + 
                    children[3] + ")))";
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
                        op = "- 0";
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
        return msatToSimplifyCache.get(f);
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
            System.err.println("BAD ANSWER FROM SIMPLIFY: '" + status + "', " +
                    "FORMULA: " + formula);
            assert(false);
        }
        return false;
    }

    private long[] simplifyGetCounterexample(String formula, 
            MathsatSymbolicFormulaManager mmgr,
            Set<String> predicates) {
        simplifyIn.println(formula);
        simplifyIn.flush();
        String status = null;
        long[] ret = null;
        try {
            status = simplifyOut.readLine();
            while (status != null) {
                if (status.contains("Counterexample:")) {
                    ret = parseCex(mmgr, predicates);
                } else if (status.contains("alid.")) {
                    break;
                }
                status = simplifyOut.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        assert(status != null);
        if (status.contains("Valid.")) {
            return null;
        } else if (status.contains("Invalid.")) {
            // ok, let's parse the model
            return ret;
        } else {
            System.err.println("BAD ANSWER FROM SIMPLIFY: '" + status + "', " +
                    "FORMULA: " + formula);
            assert(false);
        }
        return null;
    }

    private long[] parseCex(MathsatSymbolicFormulaManager mmgr, 
            Set<String> predicates) {
        Vector<Long> model = new Vector<Long>();
        long msatEnv = mmgr.getMsatEnv();
        try {
            String line = simplifyOut.readLine();
            assert(line.contains("context:") || line.isEmpty());
            if (line.isEmpty()) {
                return new long[0]; // empty model means that the formula 
                // is a tautology
            }
            line = simplifyOut.readLine();
            assert(line.contains("(AND"));
            while (true) {
                // here we parse the model
                line = simplifyOut.readLine().trim();
                if (line.isEmpty()) {
                    break;
                }
                boolean negate = false;
                if (line.startsWith("(NOT ")) {
                    negate = true;
                    line = line.substring(5, line.length()-1);
                }
                if (predicates.contains(line)) {
                    long t = simplifyPredToMsat.get(line);
                    if (negate) {
                        t = mathsat.api.msat_make_not(msatEnv, t);
                    }
                    model.add(t);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }
        long[] ret = new long[model.size()];
        for (int i = 0; i < model.size(); ++i) {
            ret[i] = model.elementAt(i);
        }
        return ret;
    }

    private Set<String> toSimplifyPreds(Vector<SymbolicFormula> important) {
        Set<String> ret = new HashSet<String>();
        for (SymbolicFormula p : important) {
            long pred = ((MathsatSymbolicFormula)p).getTerm();
            assert(mathsat.api.msat_term_is_boolean_var(pred) != 0);
            long d = mathsat.api.msat_term_get_decl(pred);
            assert(msatVarToSimplifyVar.containsKey(d));
            String name = msatVarToSimplifyVar.get(d);
            ret.add(name);
            simplifyPredToMsat.put(name, pred);
        }
        return ret;
    }
}
