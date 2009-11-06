/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package symbpredabstraction.mathsat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Class used to dump "hard" abstraction problems in MathSAT and NuSMV
 * formats. Used only for debugging (or for collecting interesting benchmarks)
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class BDDMathsatAbstractionPrinter {

    private long msatEnv;
    private String baseFileName;
    private int curNum;

    public BDDMathsatAbstractionPrinter(long env, String baseName) {
        msatEnv = env;
        baseFileName = baseName;
        curNum = 0;
    }

    public void nextNum() {
        ++curNum;
    }

    public void printMsatFormat(long curStateTerm, long edgeFormulaTerm,
                                long predDef, long[] importantSymbols) {
        String fileName = baseFileName + "." + curNum + ".msat";
        String importantName = fileName + ".important";
        String curStateRepr = mathsat.api.msat_to_msat(msatEnv, curStateTerm);
        String edgeFormulaRepr = mathsat.api.msat_to_msat(
                msatEnv, edgeFormulaTerm);
        String predDefRepr = mathsat.api.msat_to_msat(msatEnv, predDef);

        try {
            PrintWriter out = new PrintWriter(new File(fileName));
            out.println("#--- CURRENT STATE");
            out.println(curStateRepr);
            out.println("\n#--- SUMMARY EDGE");
            out.println(edgeFormulaRepr);
            out.println("\n#--- PRED DEFS");
            out.println(predDefRepr);
            out.close();

            out = new PrintWriter(new File(importantName));
            for (long imp : importantSymbols) {
                out.println(mathsat.api.msat_term_repr(imp));
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            assert(false);
        }
    }

    public void printNusmvFormat(long curStateTerm, long edgeFormulaTerm,
                                 long predDef, long[] importantSymbols) {
        Stack<Long> toProcess = new Stack<Long>();
//        Set<String> varDecls = new HashSet<String>();
        Set<Long> cache = new HashSet<Long>();
        Set<Long> preds = new HashSet<Long>();

        long term = mathsat.api.msat_make_and(
                msatEnv, curStateTerm, edgeFormulaTerm);

//        toProcess.push(term);
//        while (!toProcess.empty()) {
//            long t = toProcess.pop();
//            if (cache.contains(t)) {
//                continue;
//            }
//            cache.add(t);
//            if (mathsat.api.msat_term_is_boolean_var(t) != 0) {
//                assert(false);
//            } else if (mathsat.api.msat_term_is_variable(t) != 0) {
//                varDecls.add(mathsat.api.msat_term_repr(t));
//            }
//            for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
//                toProcess.push(mathsat.api.msat_term_get_arg(t, i));
//            }
//        }

        toProcess.push(predDef);
        while (!toProcess.empty()) {
            long t = toProcess.pop();
            if (cache.contains(t)) {
                continue;
            }
            cache.add(t);
            if (mathsat.api.msat_term_is_atom(t) != 0 &&
                mathsat.api.msat_term_is_boolean_var(t) == 0) {
                preds.add(t);
            }
            for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
                toProcess.push(mathsat.api.msat_term_get_arg(t, i));
            }
        }

        String fileName = baseFileName + "." + curNum + ".smv";
        try {
            PrintWriter out = new PrintWriter(new File(fileName));
            out.println("MODULE main");
            String repr = mathsat.api.msat_to_msat(msatEnv, term);
            for (String line : repr.split("\n")) {
                if (line.startsWith("VAR")) {
                    out.println(line + ";");
                } else if (line.startsWith("DEFINE")) {
                    String[] bits = line.split(" +", 5);
                    out.println("DEFINE " + bits[1] + " " + bits[4] + ";");
                } else if (line.startsWith("FORMULA")) {
                    out.println("INIT" + line.substring(7));
                } else {
                    out.println(line);
                }
            }
            out.println("\nTRANS FALSE\n");
            out.println("INVARSPEC (0 = 0)\n");
            for (long p : preds) {
                repr = mathsat.api.msat_term_repr(p);
                repr = repr.replaceAll("([a-zA-Z:_0-9]+@[0-9]+)", "\"$1\"");
                out.println("PRED " + repr);
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            assert(false);
        }

    }
}
