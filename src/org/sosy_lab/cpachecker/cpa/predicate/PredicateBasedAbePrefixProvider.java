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

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.InterpolatingProverEnvironmentWithAssumptions;

@Options(prefix="cpa.predicate.refinement")
public class PredicateBasedAbePrefixProvider {

  private final LogManager logger;

  private final Solver solver;

  private final PathFormulaManager pathFormulaManager;

  private final PrefixSelector selector;

  private final PrefixPreference prefixPreference;

  @Option(secure=true, description="Max. number of prefixes to extract")
  private int maxPrefixCount = 64;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pSolver the solver to use
   */
  public PredicateBasedAbePrefixProvider(Configuration config,
      LogManager pLogger,
      Solver pSolver,
      PathFormulaManager pPathFormulaManager,
      PrefixSelector pSelector,
      PrefixPreference pPrefixPreference) {
    try {
      config.inject(this);
    } catch (InvalidConfigurationException e) {
      pLogger.log(Level.INFO, "Invalid configuration given to " + getClass().getSimpleName() + ". Using defaults instead.");
    }

    logger = pLogger;
    solver = pSolver;
    pathFormulaManager = pPathFormulaManager;

    selector = pSelector;
    prefixPreference = pPrefixPreference;
  }

  public Pair<List<BooleanFormula>, List<BooleanFormula>> extractInfeasiblePrefixes(
      final ARGPath pPath) throws CPAException, InterruptedException {
    PredicateCPARefiner.prefixExtractionTime.start();
    List<ARGState> abstractionStates = transformPath(pPath);
    List<BooleanFormula> blockFormulas = from(abstractionStates)
        .transform(AbstractStates.toState(PredicateAbstractState.class))
        .transform(GET_BLOCK_FORMULA)
        .toList();

    List<Object> terms = new ArrayList<>(abstractionStates.size());
    List<Triple<Integer, List<BooleanFormula>, List<BooleanFormula>>> prefixes = new ArrayList<>();
    LinkedHashMap<InfeasiblePrefix, Pair<List<BooleanFormula>, List<BooleanFormula>>> infeasiblePrefixes = new LinkedHashMap<>();

    try (@SuppressWarnings("unchecked")
      InterpolatingProverEnvironmentWithAssumptions<Object> prover =
      (InterpolatingProverEnvironmentWithAssumptions<Object>)solver.newProverEnvironmentWithInterpolation()) {

      List<BooleanFormula> pathFormula = new ArrayList<>();
      PathFormula formula = pathFormulaManager.makeEmptyPathFormula();

      int currentBlockIndex = 0;

      PathIterator iterator = pPath.pathIterator();
      while (iterator.hasNext()) {
        ARGState currentState = iterator.getAbstractState();

        if(iterator.getIndex() == 0) {
          assert(isAbstractionState(currentState));
        }

        // only compute prefixes at abstraction states
        if (isAbstractionState(currentState)) {

          BooleanFormula currentBlockFormula = blockFormulas.get(currentBlockIndex);
          pathFormula.add(currentBlockFormula);

          try {
            formula = pathFormulaManager.makeAnd(makeEmpty(formula), currentBlockFormula);
            Object term = prover.push(formula.getFormula());
            terms.add(term);

            //if (iterator.getOutgoingEdge().getEdgeType() == CFAEdgeType.AssumeEdge && prover.isUnsat()) {
            if (prover.isUnsat()) {

              logger.log(Level.FINE, "found infeasible prefix, ending with edge ",
                  iterator.getOutgoingEdge(),
                  " in block # ",
                  currentBlockIndex,
                  ", that resulted in an unsat-formula");

              List<BooleanFormula> interpolantSequence = extractInterpolantSequence(terms, prover);
              List<BooleanFormula> finalPathFormula = new ArrayList<>(pathFormula);

              // add another transition to the path formula,
              // unless it is the last block (then formula would be longer than paths of abstraction states)
              if(currentBlockIndex + 1 < abstractionStates.size()) {
                finalPathFormula.add(makeTrue());
              }

              prefixes.add(Triple.of(currentBlockIndex, finalPathFormula, interpolantSequence));

              // TODO: pPath is the original, unchanged path !!!
              // But we do not care for now, just plug it in, path is irrelevant for prefix, anyway
              infeasiblePrefixes.put(InfeasiblePrefix.buildForPredicateDomain(pPath, interpolantSequence, solver.getFormulaManager()),
                  Pair.of(finalPathFormula, interpolantSequence));

              // remove reason for UNSAT from solver stack
              prover.pop();

              // replace respective term by tautology
              terms.remove(terms.size() - 1);
              formula = pathFormulaManager.makeAnd(makeEmpty(formula), makeTrue());
              terms.add(prover.push(formula.getFormula()));

              // replace failing block formula by tautology, too
              pathFormula.remove(pathFormula.size() - 1);
              pathFormula.add(makeTrue());
            }
          }

          catch (SolverException e) {
            throw new CPAException("Error during computation of prefixes: " + e.getMessage(), e);
          }

          currentBlockIndex++;

          // put hard-limit on number of prefixes
          if (prefixes.size() == maxPrefixCount) {
            break;
          }
        }

        iterator.advance();
      }
    }
    PredicateCPARefiner.prefixExtractionTime.stop();
    if (infeasiblePrefixes.isEmpty()) {
      return null;
    }

    PredicateCPARefiner.totalPrefixes.setNextValue(infeasiblePrefixes.size());

    List<InfeasiblePrefix> prefixList = new ArrayList<>();
    for (InfeasiblePrefix prefix : infeasiblePrefixes.keySet()) {
      prefixList.add(prefix);
    }

    PredicateCPARefiner.prefixSelectionTime.start();
    InfeasiblePrefix selectedPrefix = selector.selectSlicedPrefix(prefixPreference, prefixList);
    PredicateCPARefiner.prefixSelectionTime.stop();
    Pair<List<BooleanFormula>, List<BooleanFormula>> formulae = infeasiblePrefixes.get(selectedPrefix);

    return formulae;
  }

  private PathFormula makeEmpty(PathFormula formula) {
    return pathFormulaManager.makeEmptyPathFormula(formula);
  }

  private BooleanFormula makeTrue() {
    return solver.getFormulaManager().getBooleanFormulaManager().makeBoolean(true);
  }

  private BooleanFormula makeFalse() {
    return solver.getFormulaManager().getBooleanFormulaManager().makeBoolean(false);
  }

  private boolean isAbstractionState(ARGState pCurrentState) {
    return AbstractStates.toState(PredicateAbstractState.class).apply(pCurrentState).isAbstractionState();
  }

  private <T> List<BooleanFormula> extractInterpolantSequence(final List<T> pTerms,
      final InterpolatingProverEnvironmentWithAssumptions<T> pProver) throws SolverException {

    List<BooleanFormula> interpolantSequence = new ArrayList<>();

    for(int i = 1; i < pTerms.size(); i++) {
      interpolantSequence.add(pProver.getInterpolant(pTerms.subList(0, i)));
    }

    // add a final "false" interpolant
    interpolantSequence.add(makeFalse());

    return interpolantSequence;
  }
}
