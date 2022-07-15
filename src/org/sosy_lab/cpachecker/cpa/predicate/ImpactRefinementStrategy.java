// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Refinement strategy similar based on the general idea of McMillan's Impact algorithm: do not
 * create a precision and remove parts from ARG, but instead directly strengthen the states in the
 * ARG using interpolants. There are different ways on how to use the interpolants, these are
 * documented in the description of {@link ImpactUtility}.
 *
 * <p>This class can be used both with and without BDDs.
 */
class ImpactRefinementStrategy extends RefinementStrategy implements StatisticsProvider {

  private class Stats implements Statistics {

    private final Timer coverTime = new Timer();
    private final Timer argUpdate = new Timer();

    @Override
    public String getName() {
      return "Impact Refiner";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      out.println("  Computing abstraction of itp:       " + impact.abstractionTime);
      out.println("  Checking whether itp is new:        " + impact.itpCheckTime);
      out.println("  Coverage checks:                    " + coverTime);
      out.println("  ARG update:                         " + argUpdate);
      out.println();
      out.println(
          "Number of abstractions during refinements:  "
              + impact.abstractionTime.getNumberOfIntervals());

      ImpactRefinementStrategy.this.printStatistics(out);
    }
  }

  private final Stats stats = new Stats();

  private final BooleanFormulaManagerView bfmgr;
  private final PredicateAbstractionManager predAbsMgr;
  private final ImpactUtility impact;

  // During the refinement of a single path,
  // a reference to the abstraction of the last state we have seen
  // (we sometimes needs this to refer to the previous block).
  private AbstractionFormula lastAbstraction = null;

  protected ImpactRefinementStrategy(
      final Configuration config,
      final Solver pSolver,
      final PredicateAbstractionManager pPredAbsMgr)
      throws InvalidConfigurationException {
    super(pSolver);

    bfmgr = pSolver.getFormulaManager().getBooleanFormulaManager();
    predAbsMgr = pPredAbsMgr;
    impact = new ImpactUtility(config, pSolver.getFormulaManager(), pPredAbsMgr);
  }

  @Override
  protected void startRefinementOfPath() {
    checkState(lastAbstraction == null);
    lastAbstraction = predAbsMgr.makeTrueAbstractionFormula(null);
  }

  /**
   * For each interpolant, we strengthen the corresponding state by conjunctively adding the
   * interpolant to its state formula. This is all implemented in {@link
   * ImpactUtility#strengthenStateWithInterpolant(BooleanFormula, ARGState, AbstractionFormula)}.
   */
  @Override
  protected boolean performRefinementForState(BooleanFormula itp, ARGState s)
      throws SolverException, InterruptedException {
    checkArgument(!bfmgr.isTrue(itp));
    checkArgument(!bfmgr.isFalse(itp));

    boolean stateChanged = impact.strengthenStateWithInterpolant(itp, s, lastAbstraction);

    // Get the abstraction formula of the current state
    // (whether changed or not) to have it ready for the next call to this method).
    lastAbstraction = getPredicateState(s).getAbstractionFormula();

    return !stateChanged; // Careful: this method requires negated return value.
  }

  /**
   * After a path was strengthened, we need to take care of the coverage relation. We also remove
   * the infeasible part from the ARG, and re-establish the coverage invariant (i.e., that states on
   * the path are either covered or cannot be covered).
   */
  @Override
  protected void finishRefinementOfPath(
      ARGState infeasiblePartOfART,
      List<ARGState> changedElements,
      ARGReachedSet pReached,
      List<ARGState> abstractionStatesTrace,
      boolean pRepeatedCounterexample)
      throws CPAException, InterruptedException {
    checkState(lastAbstraction != null);
    lastAbstraction = null;

    stats.argUpdate.start();
    Set<ARGState> alsoAffectedStates = new LinkedHashSet<>();
    for (ARGState w : changedElements) {
      alsoAffectedStates.addAll(w.getCoveredByThis());
      pReached.removeCoverageOf(w);
    }

    pReached.removeInfeasiblePartofARG(infeasiblePartOfART);
    stats.argUpdate.stop();

    // optimization: instead of closing all ancestors of v,
    // close only those that were strengthened during refine
    stats.coverTime.start();
    try {
      for (ARGState w : alsoAffectedStates) {
        if (!w.isDestroyed()) {
          pReached.tryToCover(w);
        }
      }
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
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
