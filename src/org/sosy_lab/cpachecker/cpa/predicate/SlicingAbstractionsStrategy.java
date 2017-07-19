/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.mkAbstractionState;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;


public class SlicingAbstractionsStrategy extends RefinementStrategy {

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
      out.println("Number of abstractions during refinements:  " + impact.abstractionTime.getNumberOfIntervals());

      basicRefinementStatistics.printStatistics(out, pResult, pReached);
    }
  }

  private final Stats stats = new Stats();

  private final BooleanFormulaManagerView bfmgr;
  private final PredicateAbstractionManager predAbsMgr;
  private final ImpactUtility impact;
  private final PathChecker pathChecker;

  // During the refinement of a single path,
  // a reference to the abstraction of the last state we have seen
  // (we sometimes needs this to refer to the previous block).
  private AbstractionFormula lastAbstraction = null;

  protected SlicingAbstractionsStrategy(final Configuration config, final Solver pSolver,
      final PredicateAbstractionManager pPredAbsMgr, final PathChecker pPathChecker) throws InvalidConfigurationException {
    super(pSolver);

    bfmgr = pSolver.getFormulaManager().getBooleanFormulaManager();
    predAbsMgr = pPredAbsMgr;
    impact = new ImpactUtility(config, pSolver.getFormulaManager(), pPredAbsMgr);
    pathChecker = pPathChecker;
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
      ARGState s) throws SolverException, InterruptedException {
    checkArgument(!bfmgr.isTrue(itp));
    checkArgument(!bfmgr.isFalse(itp));

    PredicateAbstractState original = getPredicateState(s);
    PredicateAbstractState copiedPredicateState = mkAbstractionState(
        original.getPathFormula(),
        original.getAbstractionFormula(),
        original.getAbstractionLocationsOnPath()
    );
    boolean stateChanged = impact.strengthenStateWithInterpolant(
                                                       itp, s, lastAbstraction);
    // we only split if the state has actually changed
    ARGState newState;
    if (stateChanged) {
      //splitting the state:
      newState = s.forkWithReplacements(Collections.singleton(copiedPredicateState));

      //Now we strengthen the splitted state with negated interpolant:
      BooleanFormula negatedItp = bfmgr.not(itp);
      impact.strengthenStateWithInterpolant(negatedItp,newState,lastAbstraction);
    }

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
      if (w.getForkedChild() != null && !w.getForkedChild().isForkCompleted()) {
        pReached.addForkedState(w.getForkedChild(),w);
        w.getForkedChild().setForkCompleted();
      }
    }
    // save root of the ARG BEFORE slicing. After slicing, there might be
    // several root states, but we need the true root state for recalculateReachedSet()!
    Set<ARGState> rootStates = ARGUtils.getRootStates(pReached.asReachedSet());
    assert rootStates.size() == 1;
    sliceEdges(changedElements);

    // We do not have a tree, so this does not make sense anymore:
    //pReached.removeInfeasiblePartofARG(infeasiblePartOfART);
    // Instead we use a different method:
    pReached.recalculateReachedSet(rootStates.iterator().next());

    stats.argUpdate.stop();

    // optimization: instead of closing all ancestors of v,
    // close only those that were strengthened during refine
    stats.coverTime.start();
    try {
      for (ARGState w : changedElements) {
        /*if (pReached.tryToCover(w)) {
          break; // all further elements are covered anyway
        }*/
      }
    } finally {
      stats.coverTime.stop();
    }
  }

  private void sliceEdges(List<ARGState> pChangedElements) {
    List<ARGState> allChangedStates = new ArrayList<>(pChangedElements.size());
    //get the corresponding forked states:
    allChangedStates = pChangedElements.stream()
        .map(x -> x.getForkedChild())
        .filter(x -> x != null)
        .collect(Collectors.toList());
    allChangedStates.addAll(pChangedElements);

    for (ARGState currentState : allChangedStates) {
      List<Pair<ARGState, ARGState>> infeasibleEdges = new ArrayList<>();

      // Check all transitions from currentState to its children:
      for (ARGState child: currentState.getChildren()) {
        if (isInfeasibleEdge(currentState,child)) {
          infeasibleEdges.add(Pair.of(currentState,child));
        }
      }

      // Check all transitions from parents of currentState to currentState:
      for (ARGState parent: currentState.getParents()) {
        if (allChangedStates.contains(parent)) {
          // we already checked for this edge above!
          continue;
        }
        if (isInfeasibleEdge(parent,currentState)) {
          infeasibleEdges.add(Pair.of(parent,currentState));
        }
      }

      // Remove the edges that have been found to be infeasible:
      for (Pair<ARGState,ARGState> statePair : infeasibleEdges) {
        ARGState parent = statePair.getFirst();
        ARGState child = statePair.getSecond();
        child.removeParent(parent);
        assert !child.getParents().contains(parent);
        assert !parent.getChildren().contains(child);
      }
    }
  }

  private boolean isInfeasibleEdge(ARGState parent, ARGState child) {
    boolean infeasible = false;
    try {
      infeasible = this.pathChecker.isInfeasibleEdge(parent,child);
    } catch (SolverException|InterruptedException|CPATransferException  e){
      // TODO: What should we do here?
    }
    return infeasible;
  }

  @Override
  public Statistics getStatistics() {
    return stats;
  }

}
