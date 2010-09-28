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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.util.symbpredabstraction.Predicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;


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

    public Set<Predicate> parsePredicates(String in) {
        long formula = mathsat.api.msat_from_msat(mmgr.getMsatEnv(), in);
        if (mathsat.api.MSAT_ERROR_TERM(formula)) {
            return null;
        }

        Set<Predicate> ret = new HashSet<Predicate>();
        Deque<Long> toProcess = new ArrayDeque<Long>();

        // We *ASSUME* that in the original msat file the formula is a
        // conjunction of (name <-> def) for each predicate. Since mathsat
        // internally translates iffs in a conjunction of implications, which
        // are in turn translated into ORs, here we look only for ORs in which
        // one of the children is a boolean variable
        toProcess.push(formula);
        while (!toProcess.isEmpty()) {
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
                }
            }
        }
        return ret;
    }
}
