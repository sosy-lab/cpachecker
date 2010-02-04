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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import cpa.common.CPAchecker;

import symbpredabstraction.interfaces.FormulaManager;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.SymbolicFormula;


/**
 * Parses a file in msat format to extract a list of predicates. The file
 * should contain a formula that is a conjunction of
 * PREDICATE_NAME <-> PREDICATE_DEF
 * where PREDICATE_NAME is a Boolean variable, and PREDICATE_DEF is an atom
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class MathsatPredicateParser {
    private final MathsatSymbolicFormulaManager mmgr;
    private final FormulaManager mgr;

    public MathsatPredicateParser(MathsatSymbolicFormulaManager mmgr,
                                  FormulaManager mgr) {
        this.mmgr = mmgr;
        this.mgr = mgr;
    }

    public Collection<Predicate> parsePredicates(InputStream in) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuffer data = new StringBuffer();
            String line = r.readLine();
            while (line != null) {
                data.append(line);
                data.append("\n");
                line = r.readLine();
            }
            if (data.toString().trim().isEmpty()) {
              return null;
            }
            
            long msatEnv = mmgr.getMsatEnv();
            long formula = mathsat.api.msat_from_msat(msatEnv, data.toString());
            if (mathsat.api.MSAT_ERROR_TERM(formula)) {
                return null;
            }
            return parsePredicates(formula);
        } catch (IOException e) {
          CPAchecker.logger.logException(Level.WARNING, e, "");
            return null;
        }
    }

    private Collection<Predicate> parsePredicates(long formula) {
        Collection<Predicate> ret = new Vector<Predicate>();
        Stack<Long> toProcess = new Stack<Long>();

        CPAchecker.logger.log(Level.ALL, "DEBUG_3",
                       "FORMULA IS: ", new MathsatSymbolicFormula(formula));

        // We *ASSUME* that in the original msat file the formula is a
        // conjunction of (name <-> def) for each predicate. Since mathsat
        // internally translates iffs in a conjunction of implications, which
        // are in turn translated into ORs, here we look only for ORs in which
        // one of the children is a boolean variable
        toProcess.push(formula);
        while (!toProcess.empty()) {
            long t = toProcess.pop();
            if (mathsat.api.msat_term_is_and(t) != 0) {
                for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
                    toProcess.push(mathsat.api.msat_term_get_arg(t, i));
                }
            } else {
                assert(mathsat.api.msat_term_is_or(t) != 0);
                long var = mathsat.api.msat_term_get_arg(t, 0);
                long def = mathsat.api.msat_term_get_arg(t, 1);
                if (mathsat.api.msat_term_is_boolean_var(var) == 0) {
                    long tmp = var;
                    var = def;
                    def = tmp;
                }
                if (mathsat.api.msat_term_is_boolean_var(var) != 0) {
                    assert(mathsat.api.msat_term_is_not(def) != 0);
                    def = mathsat.api.msat_term_get_arg(def, 0);
                    SymbolicFormula symbVar = new MathsatSymbolicFormula(var);
                    SymbolicFormula symbDef = new MathsatSymbolicFormula(def);
                    ret.add(mgr.makePredicate(symbVar, symbDef));

                    CPAchecker.logger.log(Level.ALL, "DEBUG_1",
                                   "ADDED PREDICATE, name: ",
                                   symbVar,
                                   ", atom: ",
                                   symbDef);
                }
            }
        }
        return ret;
    }
}
