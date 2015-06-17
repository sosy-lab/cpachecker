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
package org.sosy_lab.cpachecker.core.algorithm.precondition;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import java.util.List;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.partitioning.PartitioningCPA.PartitionState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

public final class PreconditionHelper {

  private final FormulaManagerView mgrv;
  private final BooleanFormulaManagerView bmgr;
  private final PathFormulaManager pfmBwd;

  public PreconditionHelper(FormulaManagerView pMgrv, Configuration pConfig,
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
          throws InvalidConfigurationException {

    this.mgrv = pMgrv;
    this.bmgr = pMgrv.getBooleanFormulaManager();
    this.pfmBwd = new PathFormulaManagerImpl(mgrv, pConfig, pLogger, pShutdownNotifier, pCfa, AnalysisDirection.BACKWARD);
  }

  public static PreconditionPartition spacePartitionToPreconditionPartition(StateSpacePartition pPartition) {
    return pPartition.getPartitionKey().equals(CPAchecker.InitialStatesFor.TARGET)
        ? PreconditionPartition.VIOLATING
        : PreconditionPartition.VALID;
  }

  public static boolean isStateFromPartition(AbstractState pState, PreconditionPartition pP) {
    PartitionState state = AbstractStates.extractStateByType(pState, PartitionState.class);
    StateSpacePartition partition = state.getStateSpacePartition();
    return spacePartitionToPreconditionPartition(partition).equals(pP);
  }

  public static final Predicate<AbstractState> IS_FROM_VIOLATING_PARTITION = new Predicate<AbstractState>() {
    @Override
    public boolean apply(AbstractState pArg0) {
      return isStateFromPartition(pArg0, PreconditionPartition.VIOLATING);
    }
  };

  public static final Predicate<AbstractState> IS_FROM_VALID_PARTITION = new Predicate<AbstractState>() {
    @Override
    public boolean apply(AbstractState pArg0) {
      return isStateFromPartition(pArg0, PreconditionPartition.VALID);
    }
  };

  static final Function<PredicateAbstractState, PathFormula> GET_BLOCK_FORMULA
    = new Function<PredicateAbstractState, PathFormula>() {
      @Override
      public PathFormula apply(PredicateAbstractState e) {
        assert e.isAbstractionState();
        return e.getAbstractionFormula().getBlockFormula();
      }
    };

  static List<ARGState> transformPath(ARGPath pPath) {
    List<ARGState> result = from(pPath.asStatesList())
      .skip(1)
      .filter(Predicates.compose(PredicateAbstractState.FILTER_ABSTRACTION_STATES,
                                 toState(PredicateAbstractState.class)))
      .toList();

//    assert from(result).allMatch(new Predicate<ARGState>() {
//      @Override
//      public boolean apply(ARGState pInput) {
//        boolean correct = pInput.getChildren().size() <= 1;
//        assert correct : "PreconditionWriter expects abstraction states to have only one children, but this state has more:" + pInput;
//        return correct;
//      }
//    });

    assert pPath.getLastState() == result.get(result.size()-1);
    return result;
  }

  public BooleanFormula uninstanciatePathFormula(PathFormula pPf)
      throws SolverException, InterruptedException {

    return mgrv.uninstantiate(
        mgrv.eliminateDeadVariables(pPf.getFormula(), pPf.getSsa()));
  }

  /**
   * Compute the precondition for a given path.
   *
   * ASSUMPTION:
   *    The first element of ARGPath.asEdgesList() is the edge on the error location.
   *
   * This code can later also be used to compute postconditions.
   *
   * @param pPathToEntryLocation
   * @return
   * @throws InterruptedException
   * @throws CPATransferException
   * @throws SolverException
   */
  public BooleanFormula getPreconditionOfPath(
      final ARGPath pPathToEntryLocation,
      final PathPosition pTraceValWpPos)
    throws CPATransferException, SolverException, InterruptedException {

    return getPreconditionOfPath(pPathToEntryLocation, pTraceValWpPos, true);
  }

  public PathFormula computePathformulaForArbitraryTrace(
      final ARGPath pAbstractPathToEntryLocation,
      final PathPosition pWpPos)
    throws CPATransferException, InterruptedException {

    Preconditions.checkNotNull(pAbstractPathToEntryLocation);
    Preconditions.checkNotNull(pWpPos);

    PathFormula pf = pfmBwd.makeEmptyPathFormula();

    PathIterator it = pAbstractPathToEntryLocation.pathIterator();

    while (it.hasNext()) {
      final CFAEdge t = it.getOutgoingEdge();
      final PathPosition currentPos = it.getPosition();
      it.advance();

      if (t == null) {
        continue;
      }

      if (pWpPos.equals(currentPos)) {
        break;
      }

      if (t.getEdgeType() != CFAEdgeType.BlankEdge) {
        pf = pfmBwd.makeAnd(pf, t); // BACKWARDS!!!!
      }

    }

    return pf;
  }

  public BooleanFormula getPreconditionOfPath(
      final ARGPath pAbstractPathToEntryLocation,
      final PathPosition pWpPos,
      final boolean uninstanciate)
    throws CPATransferException, InterruptedException, SolverException {

    Preconditions.checkNotNull(pAbstractPathToEntryLocation);
    Preconditions.checkNotNull(pWpPos);

    PathFormula pf = computePathformulaForArbitraryTrace(pAbstractPathToEntryLocation, pWpPos);

    return uninstanciate
        ? uninstanciatePathFormula(pf)
        : pf.getFormula();
  }

  public BooleanFormula getPreconditionFromReached(
      final ReachedSet pReached,
      final PreconditionPartition pPartition,
      final CFANode pSpecificTargetLocation) {

    Preconditions.checkNotNull(pReached);
    Preconditions.checkNotNull(pPartition);

    List<BooleanFormula> abstractions = getAbstractionsOnLocationFromReached(
        pReached, pPartition, pSpecificTargetLocation);

    return mgrv.simplify(bmgr.or(abstractions));
  }

  private PredicateAbstractState getTargetAbstractionState(
      final ARGPath pPathToEntryLocation,
      final CFANode pTargetLocation) {

    Preconditions.checkNotNull(pTargetLocation);
    Preconditions.checkNotNull(pPathToEntryLocation);

    final List<ARGState> relevantStates = from(pPathToEntryLocation.asStatesList())
        .skip(1)
        .filter(Predicates.compose(
            PredicateAbstractState.FILTER_ABSTRACTION_STATES,
            toState(PredicateAbstractState.class)))
        .filter(Predicates.compose(
            equalTo(pTargetLocation),
            AbstractStates.EXTRACT_LOCATION))
        .toList();

    Verify.verify(relevantStates.size() == 1);

    return AbstractStates.extractStateByType(relevantStates.get(0), PredicateAbstractState.class);
  }

  private List<BooleanFormula> getAbstractionsOnLocationFromReached(
      final ReachedSet pReached,
      final PreconditionPartition pPartition,
      final CFANode pTargetLocation) {

    Preconditions.checkNotNull(pTargetLocation);
    Preconditions.checkNotNull(pPartition);
    Preconditions.checkNotNull(pReached);

    List<BooleanFormula> result = Lists.newArrayList();

    // Also for backwards analysis can exist multiple target states (for the same CFA location)
    FluentIterable<AbstractState> targetStates = from(pReached)
        .filter(Predicates.compose(
            PredicateAbstractState.FILTER_ABSTRACTION_STATES,
            toState(PredicateAbstractState.class)))
        .filter(Predicates.compose(
            equalTo(pTargetLocation),
            AbstractStates.EXTRACT_LOCATION));

    for (AbstractState s: targetStates) {
      if (isStateFromPartition(s, pPartition)) {

        final ARGState target = (ARGState) s;
        final ARGPath pathToEntryLocation = ARGUtils.getOnePathTo(target); // BACKWARDS analysis: target = entry location

        Verify.verify(pathToEntryLocation != null, "The abstract target-state must be on an abstract path!");

        final PredicateAbstractState state = getTargetAbstractionState(pathToEntryLocation, pTargetLocation);

        // The last abstraction state before the target location contains the negation of the WP
        result.add(mgrv.uninstantiate(state.getAbstractionFormula().asFormula()));
      }
    }

    return result;
  }

}
