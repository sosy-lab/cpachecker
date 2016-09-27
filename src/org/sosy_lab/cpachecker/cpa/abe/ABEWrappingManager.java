/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.abe;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Implementation of {@link ABECPA}.
 */
@Options(prefix="cpa.abe")
public class ABEWrappingManager<A extends ABEAbstractedState<A>, P extends Precision> {

  @Option(secure = true,
      description = "Where to perform abstraction")
  private AbstractionLocations abstractionLocations = AbstractionLocations.LOOPHEAD;

  @Option(secure=true, description="Check target states reachability")
  private boolean checkTargetStates = true;

  /**
   * Where an abstraction should be performed.
   */
  public enum AbstractionLocations {

    /**
     * At every node.
     */
    ALL,

    /**
     * Only at loop heads (the most sensible choice).
     */
    LOOPHEAD,

    /**
     * Whenever multiple paths are merged.
     */
    MERGE
  }

  private final ABEManager<A, P> clientManager;
  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final CFA cfa;
  private final LogManager logger;
  private final BooleanFormulaManager bfmgr;
  private final Solver solver;

  public ABEWrappingManager(
      ABEManager<A, P> pAbstractABEStatePABEManager,
      PathFormulaManager pPfmgr,
      FormulaManagerView pFmgr,
      CFA pCfa,
      LogManager pLogger,
      Solver pSolver,
      Configuration pConfiguration) throws InvalidConfigurationException {
    pConfiguration.inject(this, ABEWrappingManager.class);
    clientManager = pAbstractABEStatePABEManager;
    pfmgr = pPfmgr;
    fmgr = pFmgr;
    cfa = pCfa;
    logger = pLogger;
    solver = pSolver;
    bfmgr = fmgr.getBooleanFormulaManager();
  }

  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      ABEState<A> pState,
      CFAEdge edge) throws CPATransferException, InterruptedException {

    CFANode node = edge.getSuccessor();
    ABEIntermediateState<A> iOldState;

    if (pState.isAbstract()) {
      ABEAbstractedState<A> aState = pState.asAbstracted();
      iOldState = ABEIntermediateState.of(
          pState.getNode(),
          new PathFormula(
              bfmgr.makeTrue(),
              aState.getSSAMap(),
              aState.getPointerTargetSet(),
              1
          ),
          aState
      );
    } else {
      iOldState = pState.asIntermediate();
    }

    PathFormula outPath = pfmgr.makeAnd(iOldState.getPathFormula(), edge);
    ABEIntermediateState<A> out = ABEIntermediateState.of(
        node,
        outPath,
        iOldState.getBackpointerState());

    return Collections.singleton(out);
  }

  public Optional<? extends AbstractState> strengthen(
      ABEState<A> pState,
      P pPrecision,
      List<AbstractState> pOtherStates) {
    if (!pState.isAbstract()) {
      return Optional.of(pState);
    }
    return clientManager.strengthen(
        pState.asAbstracted(),
        pPrecision,
        pOtherStates
    );
  }

  public A getInitialState(
      CFANode pNode, StateSpacePartition pPartition
  ) {
    return clientManager.getInitialState(pNode, pPartition);
  }

  public P getInitialPrecision(
      CFANode pNode, StateSpacePartition pPartition
  ) {
    return clientManager.getInitialPrecision(pNode, pPartition);
  }

  public boolean isLessOrEqual(ABEState<A> pState1, ABEState<A> pState2) {
    if (!pState1.isAbstract()) {
      ABEIntermediateState<A> iState1 = pState1.asIntermediate();
      ABEIntermediateState<A> iState2 = pState2.asIntermediate();
      return iState1.isMergedInto(iState2)
          || (iState1.getPathFormula().getFormula().equals(iState2.getPathFormula().getFormula())
              && clientManager.isLessOrEqual(
                  iState1.getBackpointerState(), iState2.getBackpointerState()));
    } else {
      return clientManager.isLessOrEqual(
          pState1.asAbstracted(), pState2.asAbstracted()
      );
    }
  }

  public ABEState<A> merge(ABEState<A> state1, ABEState<A> state2)
      throws InterruptedException {

    Preconditions.checkState(state1.isAbstract() == state2.isAbstract(),
        "Only states with the same abstraction status should be allowed to merge");
    if (state1.isAbstract()) {

      // No merge.
      return state2;
    }

    ABEIntermediateState<A> iState1 = state1.asIntermediate();
    ABEIntermediateState<A> iState2 = state2.asIntermediate();
    Preconditions.checkState(iState1.getNode() == iState1.getNode());

    if (!iState1.getBackpointerState().equals(iState2.getBackpointerState())) {

      // Different parents: do not merge.
      return iState2;
    }

    if (iState1.isMergedInto(iState2)) {
      return iState2;
    } else if (iState2.isMergedInto(iState1)) {
      return iState1;
    }

    PathFormula mergedPath = pfmgr.makeOr(iState1.getPathFormula(),
        iState2.getPathFormula());
    ABEIntermediateState<A> out = ABEIntermediateState.of(
        iState1.getNode(),
        mergedPath,
        iState2.getBackpointerState());

    iState1.setMergedInto(out);
    iState2.setMergedInto(out);
    return out;
  }

  public Optional<PrecisionAdjustmentResult> prec(
      ABEState<A> pState,
      P pPrecision,
      UnmodifiableReachedSet pStates,
      AbstractState pFullState)
      throws CPATransferException, InterruptedException {

    Preconditions.checkState(!pState.isAbstract());
    ABEIntermediateState<A> iState = pState.asIntermediate();
    boolean hasTargetState = AbstractStates.asIterable(pFullState).anyMatch(
        AbstractStates::isTargetState
    );

    final boolean shouldPerformAbstraction = shouldPerformAbstraction(
        iState.getNode(), pFullState);

    BooleanFormula extraInvariant = extractFormula(pFullState);

    // Perform reachability checking, for property states, or before the abstractions.
    if (((hasTargetState && checkTargetStates) || shouldPerformAbstraction)
        && isUnreachable(iState, extraInvariant)) {

      logger.log(Level.INFO, "Returning bottom state");
      return Optional.empty();
    }
    if (shouldPerformAbstraction) {
      return Optional.of(clientManager.performAbstraction(
          iState, pPrecision, pStates, pFullState));
    }
    return Optional.of(
        PrecisionAdjustmentResult.create(
            iState, pPrecision, Action.CONTINUE
        )
    );
  }

  private boolean isUnreachable(
      ABEIntermediateState<A> pIState,
      BooleanFormula pExtraInvariant)
      throws CPATransferException, InterruptedException {

    BooleanFormula backpointerFormula = pIState.getBackpointerState().instantiate();
    BooleanFormula constraint = bfmgr.and(
        backpointerFormula,
        pIState.getPathFormula().getFormula(),
        fmgr.instantiate(pExtraInvariant, pIState.getPathFormula().getSsa())
    );

    try {
      return solver.isUnsat(
          bfmgr.toConjunctionArgs(constraint, true), pIState.getNode());
    } catch (SolverException e) {
      throw new CPATransferException("Failed solving", e);
    }
  }

  /**
   * @param totalState Encloses all other parallel states.
   * @return Whether to compute the abstraction when creating a new
   * state associated with <code>node</code>.
   */
  private boolean shouldPerformAbstraction(
      CFANode node,
      AbstractState totalState) {

    switch (abstractionLocations) {
      case ALL:
        return true;
      case LOOPHEAD:
        LoopstackState loopState = AbstractStates.extractStateByType(totalState,
            LoopstackState.class);

        return (cfa.getAllLoopHeads().get().contains(node)
            && (loopState == null || loopState.isLoopCounterAbstracted()));
      case MERGE:
        return node.getNumEnteringEdges() > 1;
      default:
        throw new UnsupportedOperationException("Unexpected state");
    }
  }

  private BooleanFormula extractFormula(AbstractState pFormulaState) {
    return bfmgr.and(
        AbstractStates.asIterable(pFormulaState)
            .filter(FormulaReportingState.class)
            .transform(s -> s.getFormulaApproximation(fmgr)).toList());
  }
}
