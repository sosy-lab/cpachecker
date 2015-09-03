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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironmentWithAssumptions;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;

import com.google.common.collect.Iterables;

@Options(prefix="cpa.predicate.refinement")
public class PredicateBasedPrefixProvider implements PrefixProvider {
  @Option(secure=true, description="Max. number of prefixes to extract")
  private int maxPrefixCount = 64;

  @Option(secure=true, description="Max. length of feasible prefixes to extract from if at least one prefix was already extracted")
  private int maxPrefixLength = 1024;

  private final LogManager logger;

  private final Solver solver;

  private final PathFormulaManager pathFormulaManager;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pSolver the solver to use
   */
  public PredicateBasedPrefixProvider(Configuration config, LogManager pLogger, Solver pSolver, PathFormulaManager pPathFormulaManager) {
    try {
      config.inject(this);
    } catch (InvalidConfigurationException e) {
      pLogger.log(Level.INFO, "Invalid configuration given to " + getClass().getSimpleName() + ". Using defaults instead.");
    }

    logger = pLogger;
    solver = pSolver;
    pathFormulaManager = pPathFormulaManager;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.predicate.PrefixProvider#getInfeasiblePrefixes(org.sosy_lab.cpachecker.cpa.arg.ARGPath)
   */
  @Override
  public List<InfeasiblePrefix> extractInfeasiblePrefixes(
      final ARGPath path) throws CPAException, InterruptedException {
    List<InfeasiblePrefix> prefixes = new ArrayList<>();
    MutableARGPath feasiblePrefixPath = new MutableARGPath();
    List<Object> feasiblePrefixTerms = new ArrayList<>(path.size());

    try (@SuppressWarnings("unchecked")
      InterpolatingProverEnvironmentWithAssumptions<Object> prover =
      (InterpolatingProverEnvironmentWithAssumptions<Object>)solver.newProverEnvironmentWithInterpolation()) {

      PathFormula formula = pathFormulaManager.makeEmptyPathFormula();

      PathIterator iterator = path.pathIterator();
      while (iterator.hasNext()) {
        feasiblePrefixPath.addLast(Pair.of(iterator.getAbstractState(), iterator.getOutgoingEdge()));
        try {
          formula = pathFormulaManager.makeAnd(pathFormulaManager.makeEmptyPathFormula(formula), iterator.getOutgoingEdge());
          Object term = prover.push(formula.getFormula());
          feasiblePrefixTerms.add(term);

          if (iterator.getOutgoingEdge().getEdgeType() == CFAEdgeType.AssumeEdge && prover.isUnsat()) {
            logger.log(Level.FINE, "found infeasible prefix: ", iterator.getOutgoingEdge(), " resulted in an unsat-formula");

            List<BooleanFormula> interpolantSequence = extractInterpolantSequence(feasiblePrefixTerms, prover);

            // add infeasible prefix
            InfeasiblePrefix infeasiblePrefix = buildInfeasiblePrefix(path, feasiblePrefixPath, interpolantSequence, solver.getFormulaManager());
            prefixes.add(infeasiblePrefix);

            // remove failing operation
            Pair<ARGState, CFAEdge> failingOperation =
                removeFailingOperation(feasiblePrefixPath, feasiblePrefixTerms, prover);

            // add noop-operation
            formula = addNoopOperation(feasiblePrefixPath, feasiblePrefixTerms, prover, formula, failingOperation);

            if(prefixes.size() >= maxPrefixCount) {
              break;
            }
          }
        }

        catch (SolverException | CPATransferException e) {
          throw new CPAException("Error during computation of prefixes: " + e.getMessage(), e);
        }

        if(!prefixes.isEmpty() && feasiblePrefixPath.size() >= maxPrefixLength) {
          break;
        }

        iterator.advance();
      }
    }

    return prefixes;
  }

  private <T> List<BooleanFormula> extractInterpolantSequence(List<T> feasiblePrefixFormulas,
      InterpolatingProverEnvironmentWithAssumptions<T> prover) throws SolverException {

    List<BooleanFormula> interpolantSequence = new ArrayList<>();

    for(int i = 1; i < feasiblePrefixFormulas.size(); i++) {
      interpolantSequence.add(prover.getInterpolant(feasiblePrefixFormulas.subList(0, i)));
    }

    return interpolantSequence;
  }

  private InfeasiblePrefix buildInfeasiblePrefix(final ARGPath path,
      MutableARGPath currentPrefix,
      List<BooleanFormula> interpolantSequence,
      FormulaManagerView fmgr) {
    MutableARGPath infeasiblePrefix = new MutableARGPath();
    infeasiblePrefix.addAll(currentPrefix);

    // for interpolation/refinement to work properly with existing code,
    // add another transition after the infeasible one, also add FALSE itp
    infeasiblePrefix.add(obtainSuccessorTransition(path, currentPrefix.size()));
    interpolantSequence.add(fmgr.getBooleanFormulaManager().makeBoolean(false));

    // additionally, add final (target) state, also to satisfy requirements of existing code
    infeasiblePrefix.add(Pair.of(Iterables.getLast(path.asStatesList()), Iterables.getLast(path.asEdgesList())));

    return InfeasiblePrefix.buildForPredicateDomain(infeasiblePrefix.immutableCopy(), interpolantSequence, fmgr);
  }

  /**
   * This method returns the pair of state and edge at the given offset.
   */
  private Pair<ARGState, CFAEdge> obtainSuccessorTransition(final ARGPath path, final int offset) {
    Pair<ARGState, CFAEdge> transition = path.obtainTransitionAt(offset);
    return Pair.<ARGState, CFAEdge>of(transition.getFirst(),
        BlankEdge.buildNoopEdge(transition.getSecond().getPredecessor(), transition.getSecond().getSuccessor()));
  }

  private <T> Pair<ARGState, CFAEdge> removeFailingOperation(MutableARGPath feasiblePrefixPath,
      List<T> feasiblePrefixTerms, InterpolatingProverEnvironmentWithAssumptions<T> prover) {
    Pair<ARGState, CFAEdge> failingOperation = feasiblePrefixPath.removeLast();

    // also remove formula, term for failing assume edge from stack, formula
    prover.pop();
    feasiblePrefixTerms.remove(feasiblePrefixTerms.size() - 1);
    return failingOperation;
  }

  private <T> PathFormula addNoopOperation(MutableARGPath feasiblePrefixPath, List<T> feasiblePrefixTerms,
      InterpolatingProverEnvironmentWithAssumptions<T> prover, PathFormula formula,
      Pair<ARGState, CFAEdge> failingOperation) throws CPATransferException, InterruptedException {
    CFAEdge noopEdge = BlankEdge.buildNoopEdge(
        failingOperation.getSecond().getPredecessor(),
        failingOperation.getSecond().getSuccessor());

    feasiblePrefixPath.add(Pair.<ARGState, CFAEdge>of(failingOperation.getFirst(), noopEdge));

    formula = pathFormulaManager.makeAnd(pathFormulaManager.makeEmptyPathFormula(formula), noopEdge);
    feasiblePrefixTerms.add(prover.push(formula.getFormula()));
    return formula;
  }
}
