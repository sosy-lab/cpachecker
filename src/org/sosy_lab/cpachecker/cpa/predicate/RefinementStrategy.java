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

import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.errorprone.annotations.ForOverride;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for the refinement strategy that should be used after a spurious
 * counterexample has been found and interpolants were computed.
 *
 * Instances of this interface get the path, the reached set, and the interpolants,
 * and shall update the ARG/the reached set accordingly.
 *
 * This class implements the general structure of refining a path with interpolants,
 * but delegates the actual updates to states, precisions, and ARG to its subclasses.
 */
public abstract class RefinementStrategy {

  private final StatInt differentNontrivialInterpolants = new StatInt(StatKind.SUM, "Different non-trivial interpolants along paths");
  private final StatInt equalNontrivialInterpolants = new StatInt(StatKind.SUM, "Equal non-trivial interpolants along paths");

  private final StatInt truePathPrefixStates = new StatInt(StatKind.SUM, "Length (states) of path with itp 'true'");
  private final StatInt nonTrivialPathStates = new StatInt(StatKind.SUM, "Length (states) of path with itp non-trivial itp");
  private final StatInt falsePathSuffixStates = new StatInt(StatKind.SUM, "Length (states) of path with itp 'false'");

  private final StatInt numberOfAffectedStates = new StatInt(StatKind.SUM, "Number of affected states");
  private final StatInt totalPathLengthToInfeasibility = new StatInt(StatKind.AVG, "Length of refined path (in blocks)");

  protected AbstractStatistics basicRefinementStatistics = new AbstractStatistics() {
    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      writingStatisticsTo(out)
        .put(totalPathLengthToInfeasibility)
        .put(numberOfAffectedStates)
        .put(truePathPrefixStates)
        .put(nonTrivialPathStates)
        .put(falsePathSuffixStates)
        .put(differentNontrivialInterpolants)
        .put(equalNontrivialInterpolants);
    }
  };

  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  public RefinementStrategy(Solver pSolver) {
    solver = pSolver;
    bfmgr = solver.getFormulaManager().getBooleanFormulaManager();
  }

  /**
   * @return whether previous counterexamples should be kept for comparison, such that we can
   *     determine a repeated counterexample through multiple iterations of refinements. To keep
   *     only the current counterexample, return <code>false</code>.
   */
  public boolean performRefinement(
      ARGReachedSet pReached,
      List<ARGState> abstractionStatesTrace,
      List<BooleanFormula> pInterpolants,
      boolean pRepeatedCounterexample)
      throws CPAException, InterruptedException {
    // Hook
    startRefinementOfPath();

    // The last state along the path is the target (error) state
    ARGState lastElement = abstractionStatesTrace.get(abstractionStatesTrace.size()-1);
    assert lastElement.isTarget();

    Pair<ARGState, List<ARGState>> rootOfInfeasibleArgAndChangedElements;
    try {
      rootOfInfeasibleArgAndChangedElements =
          evaluateInterpolantsOnPath(lastElement, abstractionStatesTrace, pInterpolants);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure", e);
    }

    ARGState infeasiblePartOfARG = rootOfInfeasibleArgAndChangedElements.getFirst();
    List<ARGState> changedElements = rootOfInfeasibleArgAndChangedElements.getSecond();

    // Hook
    finishRefinementOfPath(infeasiblePartOfARG, changedElements, pReached, pRepeatedCounterexample);

    // TODO find a way to uncomment this assert. In combination with
    // PredicateCPAGlobalRefiner and the PredicateAbstractionGlobalRefinementStrategy
    // this assert doesn't hold, as the updated elements are removed from the
    // reached set one step later
    // assert !pReached.asReachedSet().contains(lastElement);

    return false; // no tracking of previous counterexamples needed
  }

  // returns a pair consisting of the root of the infeasible part of the ARG and a list of all
  // changed elements
  private Pair<ARGState, List<ARGState>> evaluateInterpolantsOnPath(
      ARGState pTargetState,
      List<ARGState> abstractionStatesTrace,
      List<BooleanFormula> pInterpolants) throws SolverException, InterruptedException {

    // Skip the last element of the path, itp is always false there
    abstractionStatesTrace = abstractionStatesTrace.subList(0, abstractionStatesTrace.size()-1);
    assert pInterpolants.size() ==  abstractionStatesTrace.size();

    List<ARGState> changedElements = new ArrayList<>();
    ARGState infeasiblePartOfARG = pTargetState;
    boolean previousItpWasTrue = true;

    // Statistics on the current refinement
    int truePrefixStates = 0;
    int nonTrivialStates = 0;
    int falseSuffixStates = 0;
    int differentNontrivialItps = 0;
    int equalNontrivialItps = 0;
    int pathLengthToInfeasibility = 0;

    BooleanFormula lastItp = null;

    // Traverse the path
    for (Pair<BooleanFormula, ARGState> interpolationPoint : Pair.zipList(pInterpolants, abstractionStatesTrace)) {
      pathLengthToInfeasibility++;
      BooleanFormula itp = interpolationPoint.getFirst();
      ARGState w = interpolationPoint.getSecond();

      // ...
      if (bfmgr.isTrue(itp)) {
        // do nothing
        truePrefixStates++;
        previousItpWasTrue =  true;
        continue;
      }

      if (bfmgr.isFalse(itp)) {
        // we have reached the part of the path that is infeasible
        falseSuffixStates++;
        infeasiblePartOfARG = w;
        if (previousItpWasTrue) {
          // If the previous itp was true, and the current one is false,
          // this means that the code block between them is in itself infeasible.
          // We can add this information to the cache to speed up later sat checks.
          // PredicateAbstractState s = getPredicateState(w);
          // BooleanFormula blockFormula = s.getAbstractionFormula().getBlockFormula().getFormula();
          // solver.addUnsatisfiableFormulaToCache(blockFormula);
          // TODO disabled, because tree-interpolation returns true-false-interpolants
          // without an unsatisfiable intermediate formula
          // TODO: Move caching to InterpolationManager.buildCounterexampleTrace
        }
        break;
      }

      // Compare non-trivial interpolants along path
      if (lastItp != null) {
        if (lastItp.equals(itp)) {
          equalNontrivialItps++;
        } else {
          differentNontrivialItps++;
        }
      }
      lastItp = itp;

      nonTrivialStates++;
      previousItpWasTrue = false;

      if (!performRefinementForState(itp, w)) {
        changedElements.add(w);
      }
    }

    numberOfAffectedStates.setNextValue(changedElements.size());
    if (infeasiblePartOfARG == pTargetState) {
      pathLengthToInfeasibility++;
    }

    // Update global statistics
    truePathPrefixStates.setNextValue(truePrefixStates);
    nonTrivialPathStates.setNextValue(nonTrivialStates);
    falsePathSuffixStates.setNextValue(falseSuffixStates);
    differentNontrivialInterpolants.setNextValue(differentNontrivialItps);
    equalNontrivialInterpolants.setNextValue(equalNontrivialItps);
    totalPathLengthToInfeasibility.setNextValue(pathLengthToInfeasibility);

    return Pair.of(infeasiblePartOfARG, changedElements);
  }

  @ForOverride
  protected abstract void startRefinementOfPath();

  /**
   * Perform refinement on one state given the interpolant that was determined
   * by the solver for this state. This method is only called for states for
   * which there is a non-trivial interpolant (i.e., neither True nor False).
   * @param interpolant The interpolant.
   * @param state The state.
   * @return True if no refinement was necessary (this implies that refinement
   *          on all of the state's parents is also not necessary)
   */
  @ForOverride
  protected abstract boolean performRefinementForState(BooleanFormula interpolant, ARGState state) throws InterruptedException, SolverException;

  /**
   * Do any necessary work after one path has been refined.
   *
   * @param unreachableState The first state in the path which is infeasible (this identifies the path).
   * @param affectedStates The list of states that were affected by the refinement (ordered from root to target state).
   * @param reached The reached set.
   * @param repeatedCounterexample Whether the counterexample has been found before.
   * @throws CPAException may be thrown in subclasses
   * @throws InterruptedException may be thrown in subclasses
   */
  @ForOverride
  protected abstract void finishRefinementOfPath(
      final ARGState unreachableState,
      List<ARGState> affectedStates,
      ARGReachedSet reached,
      boolean repeatedCounterexample) throws CPAException, InterruptedException;

  public abstract Statistics getStatistics();
}
