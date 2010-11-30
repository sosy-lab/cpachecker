/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;

import org.sosy_lab.cpachecker.util.symbpredabstraction.Model;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractionManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Formula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatTheoremProver.MathsatAllSatCallback;

import org.sosy_lab.common.Pair;

public class YicesTheoremProver implements TheoremProver {

    private Map<Long, String> msatVarToYicesVar;
    private Map<Long, String> msatToYicesCache;
    private Map<String, Long> yicesPredToMsat;
    private int curVarIndex;
    private int yicesContext;
    private yices.YicesLite yicesManager;
    private SymbolicFormulaManager smgr;

    private Deque<Collection<String>> declStack;
    private Set<String> globalDecls;

    int curLevel = 0;

    // TODO
    // restart yices every once in a while, otherwise it starts eating too
    // much memory
    // private final int MAX_NUM_YICES_CALLS = 100;

    public YicesTheoremProver(SymbolicFormulaManager mgr) {
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
        smgr = mgr;
        declStack = new ArrayDeque<Collection<String>>();
        globalDecls = new HashSet<String>();
    }

    // returns a pair (declarations, formula)
    private Pair<Collection<String>, String> toYices(MathsatSymbolicFormula f) {
        Deque<Long> toProcess = new ArrayDeque<Long>();
        Collection<String> decls = new ArrayList<String>();
        toProcess.push(f.getTerm());
        while (!toProcess.isEmpty()) {
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
                        StringBuilder tp = new StringBuilder();
                        tp.append("(->");
                        int arity = mathsat.api.msat_term_arity(term);
                        for (int i = 0; i < arity; i++) {
                            tp.append(" int");
                        }
                        tp.append(" int)");
                        String decl = "(define " + yicesFun + "::" + tp + ")";
                        msatVarToYicesVar.put(d, yicesFun);
                        decls.add(decl);
                    } else {
                        yicesFun = msatVarToYicesVar.get(d);
                    }
                    String s = "(" + yicesFun + " " + Joiner.on(' ').join(children) + ")";
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
                      throw new IllegalArgumentException("UNRECOGNIZED TERM: " +
                                mathsat.api.msat_term_repr(term));
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

    private Pair<Collection<String>, String> toYices(Formula f) {
        return toYices((MathsatSymbolicFormula)f);
    }

    /*
    private void resetYices() {
        yicesManager.yicesl_del_context(yicesContext);
        yicesContext = yicesManager.yicesl_mk_context();
        yicesManager.yicesl_set_output_file("/dev/null");
        msatToYicesCache.clear();
        msatVarToYicesVar.clear();
        yicesPredToMsat.clear();
    }
    */

    private int yicesCommand(String cmd, boolean ignoreError) {
        int ret = yicesManager.yicesl_read(yicesContext, cmd);
        if (ret == 0 && !ignoreError) {
          throw new IllegalStateException("YICES ERROR: " +
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

    private List<MathsatSymbolicFormula> parseMsatPredicates(Set<String> yicesPreds, Scanner s) {
        if (s.hasNextLine()) {
            String status = s.nextLine();
            if (!status.startsWith("sat")) {
                return null; // no model to extract
            }
        }
        List<MathsatSymbolicFormula> model = new ArrayList<MathsatSymbolicFormula>();
        Pattern mp = Pattern.compile("^\\(= ([a-z0-9]+) (true|false)\\) *$");
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
                    MathsatSymbolicFormula msatPred = new MathsatSymbolicFormula(yicesPredToMsat.get(name));
                    if (value.equals("false")) {
                        msatPred = (MathsatSymbolicFormula)smgr.makeNot(msatPred);
                    }
                    model.add(msatPred);
                }
            }
        }
        return model;
    }

    private Set<String> toYicesPreds(Collection<Formula> important) {
        Set<String> ret = new HashSet<String>(important.size());
        for (Formula f : important) {
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
    public AllSatResult allSat(Formula f, Collection<Formula> important,
            AbstractionManager amgr) {
        MathsatAllSatCallback callback = new MathsatAllSatCallback(amgr);
        
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
          throw new IllegalStateException(e1);
        }
        String filename = tmpForModel.getAbsolutePath();
        yicesManager.yicesl_set_output_file(filename);
        yicesCommand("(set-evidence! true)");

        // assert the initial formula
        yicesCommand("(assert " + yicesFormula.getSecond() + ")");

        while (true) {
            yicesCommand("(check)");
            List<MathsatSymbolicFormula> model = parseMsatPredicates(yicesPreds, modelScanner);
            if (model == null) {
                break; // context is inconsistent now
            }
            if (model.size() == 0) {
                callback.setInfiniteNumberOfModels();
                break;
            }

            long[] amodel = new long[model.size()];
            for (int i = 0; i < model.size(); i++) {
              amodel[i] = model.get(i).getTerm();
            }
            callback.callback(amodel);

            // add the model as a blocking clause
            StringBuilder buf = new StringBuilder();
            for (Formula m : model) {
                long t = ((MathsatSymbolicFormula)m).getTerm();
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
            if (model.size() == 1) {
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

        return callback;
    }

    @Override
    public void init() {}

    @Override
    public boolean isUnsat(Formula f) {
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
        if (!declStack.isEmpty()) {
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
            if (!declStack.isEmpty()) {
                declStack.peek().addAll(decls);
            } else {
                globalDecls.addAll(decls);
            }
        }
    }

    @Override
    public void push(Formula f) {
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

    @Override
    public Model getModel() {
      return new Model();
    }

}
