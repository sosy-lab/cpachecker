/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.smt;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.api.FunctionDeclarationKind;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

/**
 * Best-effort dead variables elimination with pattern matching.
 *
 * // TODO: statistics.
 */
public class DeadVariablesEliminationManager {
  private final FormulaManagerView fmgr;
  private final LogManager logger;

  public DeadVariablesEliminationManager(FormulaManagerView pFmgr, LogManager pLogger) {
    fmgr = pFmgr;
    logger = pLogger;
  }

  public PathFormula eliminateDeadVarsFixpoint(PathFormula input) {
    PathFormula before, after;
    before = input;
    while (true) {
      after = eliminateDeadVarsBestEffort(before);
      if (after.equals(before)) {
        return after;
      } else {
        before = after;
      }
    }
  }

  /**
   * Does "best-effort" for replacing dead variables: eliminate what it can eliminate,
   * keep the rest.
   *
   * <p>Based on pattern matching formulas "x@n = x@i" for some {@code i < n},
   * and replacing all occurrences of "x@i" with "x@n" if "x@n" does not occur anywhere else.
   */
  public PathFormula eliminateDeadVarsBestEffort(PathFormula input) {
    BooleanFormula constraint = input.getFormula();
    SSAMap ssa = input.getSsa();
    Map<String, Formula> vars = fmgr.getRawFormulaManager().extractVariables(constraint);

    Collection<BooleanFormula> atoms = fmgr.extractAtoms(constraint, false);
    Multimap<String, BooleanFormula> linkedAtoms = HashMultimap.create();
    Multimap<BooleanFormula, String> containedVars = HashMultimap.create();
    for (BooleanFormula atom : atoms) {
      // TODO: probably could be done faster.
      Set<String> extracted = fmgr.getRawFormulaManager().extractVariables(atom).keySet();
      containedVars.putAll(atom, extracted);
      for (String s : extracted) {
        linkedAtoms.put(s, atom);
      }
    }

    final Map<Formula, Formula> replacements = new HashMap<>();

    for (Entry<String, Formula> e : vars.entrySet()) {
      final String varNameInstantiated = e.getKey();
      Pair<String, Integer> p = FormulaManagerView.parseName(varNameInstantiated);
      final String varName = p.getFirstNotNull();
      Integer idx = p.getSecond();

      if (idx != null && ssa.getIndex(varName) > idx) {
        final String newInstantiation = FormulaManagerView.makeName(varName, ssa.getIndex(varName));

        findReplacements(
            linkedAtoms.get(newInstantiation),
            replacements,
            newInstantiation,
            varNameInstantiated
        );
        findReplacements(
            linkedAtoms.get(varNameInstantiated),
            replacements,
            newInstantiation,
            varNameInstantiated
        );
      }
    }
    if (!replacements.isEmpty()) {
      logger.log(Level.FINER, "Performing replacements", replacements);
    }
    BooleanFormula out = fmgr.getRawFormulaManager().substitute(constraint, replacements);
    return input.updateFormula(out);
  }

  /**
   * Find the possible replacements and write them to {@code replacements}
   */
  private void findReplacements(
      Collection<BooleanFormula> relevant,
      final Map<Formula, Formula> replacements,
      final String newInstantiation,
      final String varNameInstantiated) {
    if (relevant.size() == 1) {
      fmgr.visit(new DefaultFormulaVisitor<Void>() {
        @Override
        protected Void visitDefault(Formula f) {
          return null;
        }

        @Override
        public Void visitFunction(
            Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
          if (args.size() != 2
              || functionDeclaration.getKind() != FunctionDeclarationKind.EQ) {
            return null;
          }
          Formula arg1 = args.get(0);
          Formula arg2 = args.get(1);
          if (isVariable(arg1, newInstantiation) && isVariable(arg2, varNameInstantiated)) {
            replacements.put(arg2, arg1);
          } else if (isVariable(arg1, varNameInstantiated) && isVariable(arg2, newInstantiation)) {
            replacements.put(arg1, arg2);
          }

          // TODO: more complex replacements?..
          return null;
        }
      }, relevant.iterator().next());
    }
  }

  /**
   * @return Whether a given formula is a variable with a {@code expectedVarName}.
   */
  private boolean isVariable(Formula f, final String expectedVarName) {
    return fmgr.visit(new DefaultFormulaVisitor<Boolean>() {
      @Override
      protected Boolean visitDefault(Formula f) {
        return false;
      }

      @Override
      public Boolean visitFreeVariable(Formula f, String name) {
        return name.equals(expectedVarName);
      }
    }, f);
  }
}
