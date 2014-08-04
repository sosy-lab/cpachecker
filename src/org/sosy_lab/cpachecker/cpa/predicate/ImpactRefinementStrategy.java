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

import static com.google.common.base.Preconditions.*;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import java.io.PrintStream;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * Refinement strategy similar based on the general idea of McMillan's Impact
 * algorithm: do not create a precision and remove parts from ARG,
 * but instead directly strengthen the states in the ARG using interpolants.
 * There are different ways on how to use the interpolants,
 * these are documented in the description of {@link ImpactUtility}.
 *
 * This class can be used both with and without BDDs.
 */
class ImpactRefinementStrategy extends RefinementStrategy {

  private class Stats implements Statistics {

    private final Timer coverTime = new Timer();
    private final Timer argUpdate = new Timer();

    @Override
    public String getName() {
      return "Impact Refiner";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("  Computing abstraction of itp:       " + impact.abstractionTime);
      out.println("  Checking whether itp is new:        " + impact.itpCheckTime);
      out.println("  Coverage checks:                    " + coverTime);
      out.println("  ARG update:                         " + argUpdate);
      out.println();
      out.println("Number of abstractions during refinements:  " + impact.abstractionTime.getNumberOfIntervals());

      basicRefinementStatistics.printStatistics(out, pResult, pReached);
    }
  }

  private final Stats stats = new Stats();

  private final FormulaManagerView fmgr;
  private final PredicateAbstractionManager predAbsMgr;
  private final ImpactUtility impact;

  // During the refinement of a single path,
  // a reference to the abstraction of the last state we have seen
  // (we sometimes needs this to refer to the previous block).
  private AbstractionFormula lastAbstraction = null;

  protected ImpactRefinementStrategy(final Configuration config, final LogManager logger,
      final FormulaManagerView pFmgr,
      final Solver pSolver,
      final PredicateAbstractionManager pPredAbsMgr)
          throws InvalidConfigurationException, CPAException {
    super(pFmgr.getBooleanFormulaManager(), pSolver);

    fmgr = pFmgr;
    predAbsMgr = pPredAbsMgr;
    impact = new ImpactUtility(config, pFmgr, pPredAbsMgr);
  }

  @Override
  protected void startRefinementOfPath() {
    checkState(lastAbstraction == null);
    lastAbstraction = predAbsMgr.makeTrueAbstractionFormula(null);
  }

  /**
   * For each interpolant, we strengthen the corresponding state by
   * conjunctively adding the interpolant to its state formula.
   * This is all implemented in
   * {@link ImpactUtility#strengthenStateWithInterpolant(BooleanFormula, ARGState, AbstractionFormula)}.
   */
  @Override
  protected boolean performRefinementForState(BooleanFormula itp,
      ARGState s) throws InterruptedException {
    checkArgument(!fmgr.getBooleanFormulaManager().isTrue(itp));
    checkArgument(!fmgr.getBooleanFormulaManager().isFalse(itp));

    boolean stateChanged = impact.strengthenStateWithInterpolant(
                                                       itp, s, lastAbstraction);

    // Get the abstraction formula of the current state
    // (whether changed or not) to have it ready for the next call to this method).
    lastAbstraction = getPredicateState(s).getAbstractionFormula();

    return !stateChanged; // Careful: this method requires negated return value.
  }

  /**
   * After a path was strengthened, we need to take care of the coverage relation.
   * We also remove the infeasible part from the ARG,
   * and re-establish the coverage invariant (i.e., that states on the path
   * are either covered or cannot be covered).
   */
  @Override
  protected void finishRefinementOfPath(ARGState infeasiblePartOfART,
      List<ARGState> changedElements, ARGReachedSet pReached,
      boolean pRepeatedCounterexample)
      throws CPAException, InterruptedException {
    checkState(lastAbstraction != null);
    lastAbstraction = null;

    stats.argUpdate.start();
    for (ARGState w : changedElements) {
      pReached.removeCoverageOf(w);
    }

    pReached.removeInfeasiblePartofARG(infeasiblePartOfART);
    stats.argUpdate.stop();

    // optimization: instead of closing all ancestors of v,
    // close only those that were strengthened during refine
    stats.coverTime.start();
    try {
      for (ARGState w : changedElements) {
        if (pReached.tryToCover(w)) {
          break; // all further elements are covered anyway
        }
      }
    } finally {
      stats.coverTime.stop();
    }
  }

  @Override
  public Statistics getStatistics() {
    return stats;
  }
}
