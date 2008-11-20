package cpaplugin.cpa.cpas.symbpredabs.mathsat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.TheoremProver;

public class YicesTheoremProver implements TheoremProver {

    private Map<Long, String> msatVarToYicesVar;
    private Map<Long, String> msatToYicesCache;
    private Map<String, Long> yicesPredToMsat;
    private int curVarIndex;
    private int yicesContext;
    private yices.YicesLite yicesManager;
    private MathsatSymbolicFormulaManager mmgr;
    
    private Stack<Collection<String>> declStack;
    private Set<String> globalDecls;
    
    int curLevel = 0;
    
    // restart yices every once in a while, otherwise it starts eating too 
    // much memory
    private final int MAX_NUM_YICES_CALLS = 100;
    
    public YicesTheoremProver(MathsatSymbolicFormulaManager mgr) {
        msatVarToYicesVar = new HashMap<Long, String>();
        msatToYicesCache = new HashMap<Long, String>();
        yicesPredToMsat = new HashMap<String, Long>();
        curVarIndex = 1;
        yicesManager = new yices.YicesLite();
        yicesContext = yicesManager.yicesl_mk_context();
        yicesManager.yicesl_set_verbosity((short)0);
        yicesManager.yicesl_set_output_file("/dev/null");
        //System.out.println("USING YICES VERSION: " + 
        //                   yicesManager.yicesl_version());
        mmgr = mgr;
        declStack = new Stack<Collection<String>>();
        globalDecls = new HashSet<String>();
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
                    if (mathsat.api.msat_term_is_bool_ite(term) != 0 ||
                        mathsat.api.msat_term_is_term_ite(term) != 0) {
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
                        System.out.println("UNRECOGNIZED TERM: " + 
                                mathsat.api.msat_term_repr(term));
                        System.out.flush();
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
    
    private Pair<Collection<String>, String> toYices(SymbolicFormula f) {
        return toYices((MathsatSymbolicFormula)f);
    }
    
    private void resetYices() {
        yicesManager.yicesl_del_context(yicesContext);
        yicesContext = yicesManager.yicesl_mk_context();
        yicesManager.yicesl_set_output_file("/dev/null");
        msatToYicesCache.clear();
        msatVarToYicesVar.clear();
        yicesPredToMsat.clear();
    }

    private int yicesCommand(String cmd, boolean ignoreError) {
        int ret = yicesManager.yicesl_read(yicesContext, cmd);
        if (ret == 0 && !ignoreError) {
            System.err.println("YICES ERROR: " + 
                    yicesManager.yicesl_get_last_error_message());
        }
        assert(ignoreError || ret != 0);
        return ret;
    }
    
    private int yicesCommand(String cmd) {
        return yicesCommand(cmd, false);
    }
    
    private boolean yicesInconsistent() {
        return yicesManager.yicesl_inconsistent(yicesContext) != 0;
    }
    
    private long[] parseMsatPredicates(MathsatSymbolicFormulaManager mmgr,
            Set<String> yicesPreds, Scanner s) {
        if (s.hasNextLine()) {
            String status = s.nextLine();
            if (!status.startsWith("sat")) {
                return null; // no model to extract
            }
        }
        Vector<Long> model = new Vector<Long>();
        Pattern mp = Pattern.compile("^\\(= ([a-z0-9]+) (true|false)\\) *$");
        long msatEnv = mmgr.getMsatEnv();
        while (s.hasNextLine()) {
            String l = s.nextLine();
            if (l.isEmpty()) {
                break;
            }
            Matcher match = mp.matcher(l);
            if (match.matches()) {
                String name = match.group(1);
                String value = match.group(2);
                if (yicesPreds.contains(name)) {
                    // ok, predicate found. Convert to mathsat and 
                    // add to the model
                    long msatPred = yicesPredToMsat.get(name);
                    if (value.equals("false")) {
                        msatPred = mathsat.api.msat_make_not(
                                msatEnv, msatPred);
                    }
                    model.add(msatPred);
                }
            }
        }
        long[] ret = new long[model.size()];
        for (int i = 0; i < model.size(); ++i) {
            ret[i] = model.elementAt(i);
        }
        return ret;
    }

    private Set<String> toYicesPreds(Vector<SymbolicFormula> important) {
        Set<String> ret = new HashSet<String>();
        for (SymbolicFormula f : important) {
            long pred = ((MathsatSymbolicFormula)f).getTerm();
            assert(mathsat.api.msat_term_is_boolean_var(pred) != 0);
            long d = mathsat.api.msat_term_get_decl(pred);
            assert(msatVarToYicesVar.containsKey(d));
            String name = msatVarToYicesVar.get(d);
            ret.add(name);
            yicesPredToMsat.put(name, pred);
        }
        return ret;
    }
    
    @Override
    public int allSat(SymbolicFormula f, Vector<SymbolicFormula> important,
            AllSatCallback callback) {
        // build the yices representation of the formula...
        Pair<Collection<String>, String> yicesFormula = toYices(f);
        // ...and of the important symbols
        Set<String> yicesPreds = toYicesPreds(important);

        // declare variables
        for (String decl : yicesFormula.getFirst()) {
            yicesManager.yicesl_read(yicesContext, decl);
        }
        // push one backtrack point
        yicesCommand("(push)");
        File tmpForModel = null;
        Scanner modelScanner = null;
        try {
            tmpForModel = File.createTempFile("cpachecker", "yices_allsat");
            modelScanner = new Scanner(tmpForModel);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            assert(false);
        }
        String filename = tmpForModel.getAbsolutePath();
        yicesManager.yicesl_set_output_file(filename);
        yicesCommand("(set-evidence! true)");
        
        // assert the initial formula
        yicesCommand("(assert " + yicesFormula.getSecond() + ")");

        Vector<SymbolicFormula> outModel = null;
        int numModels = 0;
        while (true) {
            yicesCommand("(check)");
            long[] model = parseMsatPredicates(mmgr, yicesPreds, modelScanner);
            if (model == null) {
                break; // context is inconsistent now
            }
            if (model.length == 0) {
                numModels = -2;
                break;
            } 
            ++numModels;
            
            // notify the callback of the new model
            if (outModel == null) {
                outModel = new Vector<SymbolicFormula>();
            }
            outModel.clear();
            outModel.ensureCapacity(model.length);
            for (long m : model) {
                outModel.add(new MathsatSymbolicFormula(m));
            }            
            callback.modelFound(outModel);
            
            // add the model as a blocking clause
            StringBuffer buf = new StringBuffer();
            for (long t : model) {
                if (mathsat.api.msat_term_is_not(t) != 0) {
                    t = mathsat.api.msat_term_get_arg(t, 0);
                    assert(mathsat.api.msat_term_is_boolean_var(t) != 0);
                    long d = mathsat.api.msat_term_get_decl(t);
                    String yv = msatVarToYicesVar.get(d);
                    assert(yv != null);
                    buf.append(yv + " ");
                } else {
                    assert(mathsat.api.msat_term_is_boolean_var(t) != 0);
                    long d = mathsat.api.msat_term_get_decl(t);
                    String yv = msatVarToYicesVar.get(d);
                    assert(yv != null);
                    buf.append("(not " + yv + ") ");
                }
            }
            if (model.length == 1) {
                yicesCommand("(assert " + buf + ")");
            } else {
                yicesCommand("(assert (or " + buf + "))");
            }
        }
        
        // restore context
        yicesManager.yicesl_set_output_file("/dev/null");
        yicesCommand("(pop)");
        yicesCommand("(set-evidence! false)");
        modelScanner.close();
        tmpForModel.delete();

        return numModels;
    }

    @Override
    public void init(int purpose) {}

    @Override
    public boolean isUnsat(SymbolicFormula f) {
        push(f);
        boolean res = yicesInconsistent();
        pop();
        return res;
    }

    @Override
    public void pop() {
        yicesCommand("(pop)");
        --curLevel;
        assert(curLevel >= 0);
        if (!declStack.empty()) {
            // yices has scoped declarations, but we want global ones: so, we
            // re-declared variables in the outer scope
            Collection<String> decls = declStack.pop();
            for (String d : decls) {
                if (!globalDecls.contains(d)) {
                    // sometimes, yices reports an error that the variable
                    // has already been declared. I wasn't able to understand
                    // why this happens (it could be a bug in Yices), so
                    // here we just ignore the error
                    yicesCommand(d, true);
                }
            }
            if (!declStack.empty()) {
                declStack.peek().addAll(decls);
            } else {
                globalDecls.addAll(decls);
            }
        }
    }

    @Override
    public void push(SymbolicFormula f) {
        yicesCommand("(push)");
        ++curLevel;
        Pair<Collection<String>, String> p = toYices(f);
        for (String d : p.getFirst()) {
            yicesCommand(d);
        }
        declStack.push(p.getFirst());
        yicesCommand("(assert " + p.getSecond() + ")");
    }

    @Override
    public void reset() {}

}
