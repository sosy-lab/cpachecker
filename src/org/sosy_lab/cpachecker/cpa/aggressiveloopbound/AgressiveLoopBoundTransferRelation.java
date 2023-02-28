// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.aggressiveloopbound;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.alwaystop.AlwaysTopPrecision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

@Options(prefix = "cpa.aggressiveloopbound")
public class AgressiveLoopBoundTransferRelation extends SingleEdgeTransferRelation {

  private final ImmutableMap<CFAEdge, Loop> loopExitEdges;
  private final ImmutableMap<CFAEdge, Loop> loopContinuingEdges;
  private final ImmutableMap<CFAEdge, Loop> loopEntryEdges;

  private LogManager logger;

  @Option(
      secure = true,
      description = "threshold for unrolling loops of the program (0 is infinite)")
  private int maxLoopIterations = 0;

  @Option(
      secure = true,
      description =
          "For nested loops: Reset the counter for inner loops at each iteration of the outer loop."
              + " Meanign that, if the loobound is set to 3, the inner loop is executed 3 times in"
              + " each iteration of the outer loop (hence 9 times in total")
  private boolean useStack = true;

//  @Option(
//      secure = true,
//      description = "if there is a branch not associated with a loop, only take the right branch.")
//  private boolean useRigthPath = true;

  @Option(
      secure = true,
      description = "if there is a branch not associated with a loop, only take the left branch.")
  private boolean useLeftPathInsideLoops = true;

  private @Nullable AutomatonTransferRelation automatonTransferRelation;

  AgressiveLoopBoundTransferRelation(Configuration pConfig, CFA pCFA, LogManager pLogger)
      throws CPAException, InvalidConfigurationException {
    checkNotNull(pCFA, "CFA instance needed to create LoopBoundCPA");
    pConfig.inject(this);
    this.logger = pLogger;
    if (pCFA.getLoopStructure().isEmpty()) {
      throw new CPAException("LoopBoundCPA cannot work without loop-structure information in CFA.");
    }
    logger.logf(Level.INFO, "Computing the loopbound for value of %d", this.maxLoopIterations);

    ImmutableMap.Builder<CFAEdge, Loop> entryEdges = ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, Loop> stayInLoopEdges = ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, Loop> exitEdges = ImmutableMap.builder();
    ImmutableListMultimap.Builder<CFANode, Loop> heads = ImmutableListMultimap.builder();

    for (Loop l : pCFA.getLoopStructure().orElseThrow().getAllLoops()) {
      // function edges do not count as incoming/outgoing edges
      Stream<CFAEdge> incomingEdges =
          l.getIncomingEdges().stream()
              .filter(e -> l.getLoopHeads().contains(e.getSuccessor()))
              .filter(not(instanceOf(FunctionReturnEdge.class)));

      Stream<CFAEdge> inLoopStayingEdges =
          l.getInnerLoopEdges().stream()
              .filter(e -> l.getLoopHeads().contains(e.getPredecessor()))
              .filter(not(instanceOf(FunctionReturnEdge.class)));
      Stream<CFAEdge> outgoingEdges =
          l.getOutgoingEdges().stream().filter(not(instanceOf(FunctionCallEdge.class)));

      inLoopStayingEdges.forEach(e -> stayInLoopEdges.put(e, l));
      incomingEdges.forEach(e -> entryEdges.put(e, l));
      outgoingEdges.forEach(e -> exitEdges.put(e, l));
      l.getLoopHeads().forEach(h -> heads.put(h, l));
    }
    loopEntryEdges = entryEdges.buildOrThrow();
    loopExitEdges = exitEdges.buildOrThrow();
    loopContinuingEdges = stayInLoopEdges.buildOrThrow();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, final CFAEdge pCfaEdge)
      throws CPATransferException {

    logger.log(
        Level.FINE, String.format("Processing edge %s, current State is %s", pCfaEdge, pState));

    AgressiveLoopBoundState state = (AgressiveLoopBoundState) pState;
    state = state.copy();

    if (pCfaEdge instanceof FunctionCallEdge) {
      // such edges do never change loop status
      return Collections.singleton(state);
    }

    Loop oldLoop = loopExitEdges.get(pCfaEdge);
    if (oldLoop != null) {
      if (proceedWithLoop(
          state, oldLoop)) { // Dont exit the loop (hence dont produce a successor state
        logger.log(
            Level.FINE, String.format("Visiting edge %s, is a Loop exit, dont proceed", pCfaEdge));
        state = state.setStop(true);
        return Collections.singleton(state);
      } else {
        logger.log(
            Level.FINE, String.format("Visiting edge %s, is a Loop exit, proceed", pCfaEdge));
        state = state.exit(oldLoop);
        return Collections.singleton(state);
      }
    }

    if (useStack) {
      Loop enteredLoop = loopEntryEdges.get(pCfaEdge);
      if (enteredLoop != null) {
        state = state.addToStack(enteredLoop);
      }
    }

    if (pCfaEdge instanceof FunctionReturnEdge) {
      // Such edges may be real loop-exit edges "while () { return; }",
      // but never loop-entry edges.
      // Return here because they might be mis-classified as entry edges.
      return Collections.singleton(state);
    }

    // Check if we are at a loophead and need to enter or exit it
    Loop newLoop = loopContinuingEdges.get(pCfaEdge);

    if (newLoop != null) {
      if (proceedWithLoop(state, newLoop) && this.maxLoopIterations > 0) { // Entere the loop
        logger.log(
            Level.FINE, String.format("Visiting edge %s, is a Loop entry, proceed", pCfaEdge));
        state = state.enter(newLoop);
        return Collections.singleton(state);
      } else {
        // Dont enter the loop
        logger.log(
            Level.FINE, String.format("Visiting edge %s, is a Loop entry, dont proceed", pCfaEdge));
        // Dont need to set stop for the state, as we ignore it anyway
        return ImmutableList.of();
      }
    }

    return Collections.singleton(state);
  }

  private boolean otherEdgeLeadsToTargetStateOrIsBreak(
      AssumeEdge pAssume, Iterable<AbstractState> otherStates) {
    CFANode successor = CFAUtils.getComplimentaryAssumeEdge(pAssume).getSuccessor();
    if (successor.getNumLeavingEdges() == 1 && this.automatonTransferRelation != null) {
      CFAEdge successorEdge = successor.getLeavingEdge(0);
      if (successorEdge instanceof BlankEdge && successorEdge.getDescription().contains("break")) {
        return true;
      }
      if (successorEdge instanceof BlankEdge
          && successorEdge.getDescription().contains("Label")
          && successorEdge.getSuccessor().getNumLeavingEdges() == 1) {
        successorEdge = successorEdge.getSuccessor().getLeavingEdge(0);
      }
      return edgeLeadsToTargetState(successorEdge, otherStates);
    }
    return false;
  }

  private boolean edgeLeadsToTargetState(
      CFAEdge pSuccessorEdge, Iterable<AbstractState> pOtherStates) {
    for (AbstractState otherState : pOtherStates) {
      if (otherState instanceof AutomatonState) {
        AutomatonState other = (AutomatonState) otherState;
        try {
          Collection<AutomatonState> potentialTargetState =
              automatonTransferRelation.getAbstractSuccessorsForEdge(
                  other, AlwaysTopPrecision.INSTANCE, pSuccessorEdge);
          return potentialTargetState.stream().anyMatch(s -> s.isTarget());
        } catch (CPATransferException pE) {
          return false;
        }
      }
    }
    return false;
  }

  private boolean proceedWithLoop(AgressiveLoopBoundState pState, Loop pOldLoop) {
    return pState.getVisits(pOldLoop) < this.maxLoopIterations;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    assert state instanceof AgressiveLoopBoundState;
    AgressiveLoopBoundState loopState = (AgressiveLoopBoundState) state;
    // Do not explore target states within the loop, hence stop if the next state would be a target
    // state

    if (!((AgressiveLoopBoundState) state).getLoopStack().isEmpty()
        && pCfaEdge.getSuccessor().getNumLeavingEdges() == 1
        && edgeLeadsToTargetState(pCfaEdge.getSuccessor().getLeavingEdge(0), otherStates)) {
      logger.log(
          Level.FINE,
          String.format(
              "Stoping at edge  %s, as it would lead to a target state within the loop", pCfaEdge));
      return Collections.emptySet();
    }

    if (useLeftPathInsideLoops && pCfaEdge instanceof AssumeEdge) {
      AssumeEdge assumeState = (AssumeEdge) pCfaEdge;
      if (!loopState.getLoopStack().isEmpty()) {
        if ((assumeState.isSwapped() && assumeState.getTruthAssumption())
            || (!assumeState.isSwapped() && !assumeState.getTruthAssumption())) {

          if (!otherEdgeLeadsToTargetStateOrIsBreak(assumeState, otherStates)) {

            // Abort, as not the leftmost edge
            logger.log(
                Level.INFO,
                String.format(
                    "Visiting edge %s, is not the leftmost edge inside a loop, hence dont proceed",
                    pCfaEdge));
            return ImmutableList.of();
          }
        }
      }
    }

//    if (useRigthPath && pCfaEdge instanceof AssumeEdge && loopState.getLoopStack().isEmpty()) {
//      //    if (useRigthPath && pCfaEdge instanceof AssumeEdge) {
//      AssumeEdge assume = (AssumeEdge) pCfaEdge;
//      if ((!assume.isSwapped() && assume.getTruthAssumption())
//          || (assume.isSwapped() && !assume.getTruthAssumption())) {
//        if (false && !otherEdgeLeadsToTargetStateOrIsBreak(assume, otherStates)) {
//          // Abort, as not the rightmost edge
//          logger.log(
//              Level.FINE,
//              String.format(
//                  "Visiting edge %s, is not the rightmost edge, hence dont proceed", pCfaEdge));
//          return ImmutableList.of();
//        }
//      }
//    }
    if (((AgressiveLoopBoundState) state).isStop()) {
      logger.log(Level.FINE, String.format("Stoping at edge %s, as stop is set", pCfaEdge));
      return ImmutableList.of();
    } else {
      return Collections.singleton(state);
    }
  }

  public void setAutomatonTransferRelation(AutomatonTransferRelation pTransferRelation) {
    this.automatonTransferRelation = pTransferRelation;
  }
}
