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
import static org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtility.buildPathFormula;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;


/**
 * This is an implementation of the Slicing Abstractions idea
 * as RefinementStrategy like in the papers:
 * "Slicing Abstractions" (doi:10.1007/978-3-540-75698-9_2)
 * "Splitting via Interpolants" (doi:10.1007/978-3-642-27940-9_13)
 */
public class SlicingAbstractionsStrategy extends RefinementStrategy {

  private class Stats implements Statistics {

    private final Timer coverTime = new Timer();
    private final Timer argUpdate = new Timer();
    private final Timer copyEdges = new Timer();
    private final Timer sliceEdges = new Timer();
    private final Timer calcReached = new Timer();

    @Override
    public String getName() {
      return "Slicing Abstractions";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      out.println("  Computing abstraction of itp:       " + impact.abstractionTime);
      out.println("  Checking whether itp is new:        " + impact.itpCheckTime);
      out.println("  Coverage checks:                    " + coverTime);
      out.println("  ARG update:                         " + argUpdate);
      out.println("    Copy edges:                       " + copyEdges);
      out.println("    Slice edges:                      " + sliceEdges);
      out.println("    Recalculate ReachedSet:           " + calcReached);
      out.println();
      out.println("Number of abstractions during refinements:  " + impact.abstractionTime.getNumberOfIntervals());

      basicRefinementStatistics.printStatistics(out, pResult, pReached);
    }
  }

  private final Stats stats = new Stats();

  private final BooleanFormulaManagerView bfmgr;
  private final PredicateAbstractionManager predAbsMgr;
  private final ImpactUtility impact;
  private final PathFormulaManager pfmgr;
  private final Solver solver;

  // During the refinement of a single path,
  // a reference to the abstraction of the last state we have seen
  // (we sometimes needs this to refer to the previous block).
  private AbstractionFormula lastAbstraction = null;
  private boolean initialSliceDone = false;

  public SlicingAbstractionsStrategy(final Configuration config, final Solver pSolver,
      final PredicateAbstractionManager pPredAbsMgr,
      final PathFormulaManager pPathFormulaManager) throws InvalidConfigurationException {
    super(pSolver);

    bfmgr = pSolver.getFormulaManager().getBooleanFormulaManager();
    predAbsMgr = pPredAbsMgr;
    impact = new ImpactUtility(config, pSolver.getFormulaManager(), pPredAbsMgr);
    pfmgr = pPathFormulaManager;
    solver = pSolver;
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

    stats.copyEdges.start();
    for (ARGState w : changedElements) {
      pReached.removeCoverageOf(w);
      if (w.getForkedChild() != null && !w.getForkedChild().isForkCompleted()) {
        SlicingAbstractionsUtility.copyEdges(w.getForkedChild(),w,pReached);
        pReached.addForkedState(w.getForkedChild(),w);
        w.getForkedChild().setForkCompleted();
      }
    }
    stats.copyEdges.stop();
    // save root of the ARG BEFORE slicing. After slicing, there might be
    // several root states, but we need the true root state for recalculateReachedSet()!
    Set<ARGState> rootStates = ARGUtils.getRootStates(pReached.asReachedSet());
    assert rootStates.size() == 1;

    stats.sliceEdges.start();
    // optimization: Slice all edges only on first iteration
    // After that we only need to slice edges of the states we split
    if (!initialSliceDone) {
      @SuppressWarnings("unchecked")
      List<ARGState> all = (List<ARGState>)(List<? extends AbstractState>)pReached.asReachedSet().asCollection().stream().
          filter(x->getPredicateState(x).isAbstractionState()).collect(Collectors.toList());
      sliceEdges(all);
      initialSliceDone = true;
    } else {
      sliceEdges(changedElements);
    }
    stats.sliceEdges.stop();

    // We do not have a tree, so this does not make sense anymore:
    //pReached.removeInfeasiblePartofARG(infeasiblePartOfART);
    // Instead we use a different method:
    stats.calcReached.start();
    pReached.recalculateReachedSet(rootStates.iterator().next());
    stats.calcReached.stop();

    stats.argUpdate.stop();

    // optimization: instead of closing all ancestors of v,
    // close only those that were strengthened during refine
    stats.coverTime.start();
    try {
      /*for (ARGState w : changedElements) {
        if (pReached.tryToCover(w)) {
          break; // all further elements are covered anyway
        }
      }*/
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

    List<ARGState> priorAbstractionStates = new ArrayList<>();
    for (ARGState currentState : allChangedStates) {
      for (ARGState s : SlicingAbstractionsUtility.calculateStartStates(currentState)) {
        if (!priorAbstractionStates.contains(s) && ! allChangedStates.contains(s)) {
          priorAbstractionStates.add(s);
        }
      }
    }
    allChangedStates.addAll(priorAbstractionStates);

    for (ARGState currentState : allChangedStates) {
      Map<ARGState, List<ARGState>> segmentMap = SlicingAbstractionsUtility.calculateOutgoingSegments(currentState);
      Map<ARGState, Boolean> infeasibleMap = new HashMap<>();
      Set<ARGState> segmentStateSet = new HashSet<>();
      for (ARGState key : segmentMap.keySet()) {
        boolean infeasible = isInfeasibleEdge(currentState, key,segmentMap.get(key));
        infeasibleMap.put(key, infeasible);
        segmentStateSet.addAll(segmentMap.get(key));
      }
      for (ARGState key : infeasibleMap.keySet()) {
        if (infeasibleMap.get(key) == false) {
          segmentStateSet.removeAll(segmentMap.get(key));
        } else {
          if (segmentMap.get(key).size()==0) {
            key.removeParent(currentState);
          }
        }
      }

      for (ARGState toRemove : segmentStateSet) {
        toRemove.removeFromARG();
      }
    }
  }

  private boolean isInfeasibleEdge(ARGState parent, ARGState child, List<ARGState> segmentList) {
    boolean infeasible = false;
    try {
      SSAMap startSSAMap = SSAMap.emptySSAMap().withDefault(1);
      BooleanFormula formula = buildPathFormula(parent, child, segmentList, startSSAMap, solver, pfmgr, true).getFormula();
      try (ProverEnvironment thmProver = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        thmProver.push(formula);
        if (thmProver.isUnsat()) {
          infeasible = true;
        } else {
          infeasible =  false;
        }
      }
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
