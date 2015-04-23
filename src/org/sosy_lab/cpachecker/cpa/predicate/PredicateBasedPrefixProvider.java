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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.PrefixProvider;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironmentWithAssumptions;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.collect.Iterables;
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

  public static Map<ARGPath, List<BooleanFormula>> prefixToItpSequences = new HashMap<>();

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.predicate.PrefixProvider#getInfeasilbePrefixes(org.sosy_lab.cpachecker.cpa.arg.ARGPath)
   */
  @Override
  public <T> List<ARGPath> extractInfeasilbePrefixes(final ARGPath path) throws CPAException, InterruptedException {
    List<ARGPath> prefixes = new ArrayList<>();
    MutableARGPath currentPrefix = new MutableARGPath();
    List<T> prefixFormulas = new ArrayList<>(path.size());

    prefixToItpSequences = new HashMap<>();

    try (@SuppressWarnings("unchecked")
      InterpolatingProverEnvironmentWithAssumptions<T> prover =
      (InterpolatingProverEnvironmentWithAssumptions<T>)solver.newProverEnvironmentWithInterpolation()) {

      PathFormula formula = pathFormulaManager.makeEmptyPathFormula();

      PathIterator iterator = path.pathIterator();
      while (iterator.hasNext()) {
        currentPrefix.addLast(Pair.of(iterator.getAbstractState(), iterator.getOutgoingEdge()));
        try {
          formula = pathFormulaManager.makeAnd(pathFormulaManager.makeEmptyPathFormula(formula), iterator.getOutgoingEdge());
          prefixFormulas.add(prover.push(formula.getFormula()));

          if (iterator.getOutgoingEdge().getEdgeType() == CFAEdgeType.AssumeEdge && prover.isUnsat()) {
            logger.log(Level.FINE, "found infeasible prefix: ", iterator.getOutgoingEdge(), " resulted in an unsat-formula");

            List<BooleanFormula> itpSeq = new ArrayList<>();
            for(int i = 1; i < prefixFormulas.size(); i++) {
              List<T> phiMinus = prefixFormulas.subList(0, i);
              BooleanFormula itp = prover.getInterpolant(phiMinus);
              itpSeq.add(itp);
            }

            // remove failing assumption from stack, formula
            prover.pop();
            prefixFormulas.remove(prefixFormulas.size() - 1);

            // add infeasible prefix
            ARGPath infeasiblePrefix = buildInfeasiblePrefix(path, currentPrefix);
            prefixes.add(infeasiblePrefix);
            prefixToItpSequences.put(infeasiblePrefix, itpSeq);

            CFAEdge noop = new BlankEdge("",
                FileLocation.DUMMY,
                currentPrefix.getLast().getSecond().getPredecessor(),
                currentPrefix.getLast().getSecond().getSuccessor(),
                "REPLACEMENT");
            formula = pathFormulaManager.makeAnd(pathFormulaManager.makeEmptyPathFormula(formula), noop);
            prefixFormulas.add(prover.push(formula.getFormula()));

            // continue with feasible prefix
            currentPrefix.replaceFinalEdgeWithBlankEdge();

            if(prefixes.size() > 50) {
              return prefixes;
            }
          }
        }
        catch (SolverException e) {
          logger.logUserException(Level.WARNING, e, "Error during computation of prefixes, continuing with original error path");
          return Lists.newArrayList(path);
        }
        catch (CPATransferException e) {
          throw new CPAException("Computation of path formula for prefix failed: " + e.getMessage(), e);
        }

        iterator.advance();
      }
    }

    return prefixes;
  }

  private ARGPath buildInfeasiblePrefix(final ARGPath path, MutableARGPath currentPrefix) {
    MutableARGPath infeasiblePrefix = new MutableARGPath();
    infeasiblePrefix.addAll(currentPrefix);

    // for interpolation, one transition after the infeasible
    // transition is needed, so we add the final (error) state
    infeasiblePrefix.add(Pair.of(Iterables.getLast(path.asStatesList()), Iterables.getLast(path.asEdgesList())));

    return infeasiblePrefix.immutableCopy();
  }
}
