/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.PrefixProvider;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.collect.Lists;

public class PredicateBasedPrefixProvider implements PrefixProvider {
  private final LogManager logger;

  private final Solver solver;

  private final PathFormulaManager pathFormulaManager;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pSolver the solver to use
   */
  public PredicateBasedPrefixProvider(LogManager pLogger, Solver pSolver, PathFormulaManager pPathFormulaManager) {
    logger = pLogger;
    solver = pSolver;
    pathFormulaManager = pPathFormulaManager;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.predicate.PrefixProvider#getInfeasilbePrefixes(org.sosy_lab.cpachecker.cpa.arg.ARGPath)
   */
  @Override
  public List<ARGPath> getInfeasilbePrefixes(final ARGPath path) throws CPAException, InterruptedException {
    List<ARGPath> prefixes = new ArrayList<>();
    MutableARGPath currentPrefix = new MutableARGPath();

    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      PathFormula formula = pathFormulaManager.makeEmptyPathFormula();

      PathIterator iterator = path.pathIterator();
      while (iterator.hasNext()) {
        boolean isUnsat = false;
        currentPrefix.addLast(Pair.of(iterator.getAbstractState(), iterator.getOutgoingEdge()));

        try {
          formula = pathFormulaManager.makeAnd(pathFormulaManager.makeEmptyPathFormula(formula), iterator.getOutgoingEdge());

          prover.push(formula.getFormula());
          isUnsat = prover.isUnsat();
        }
        catch (SolverException e) {
          logger.logUserException(Level.WARNING, e, "Error during computation of prefixes, continuing with original error path");
          return Lists.newArrayList(path);
        }
        catch (CPATransferException e) {
          throw new CPAException("Computation of path formula for prefix failed: " + e.getMessage(), e);
        }

        // formula is unsatisfiable => path is infeasible
        if (isUnsat) {
          logger.log(Level.FINE, "found infeasible prefix: ", iterator.getOutgoingEdge(), " resulted in an unsat-formula");
          prover.pop();
          prefixes.add(currentPrefix.immutableCopy());

          currentPrefix = new MutableARGPath();
        }

        iterator.advance();
      }
    }

    // prefixes is empty => path is feasible, so add complete path
    if (prefixes.isEmpty()) {
      prefixes.add(path);
    }

    return prefixes;
  }
}
