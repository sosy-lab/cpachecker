// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState.AutomatonUnknownState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatIntHist;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

/**
 * The TransferRelation of this CPA determines the AbstractSuccessor of a {@link AutomatonState} and
 * strengthens an {@link AutomatonState.AutomatonUnknownState}.
 */
public class AutomatonTransferRelation implements TransferRelation {

  private final ControlAutomatonCPA cpa;
  private final LogManager logger;
  private final MachineModel machineModel;

  private final TimerWrapper totalPostTime;
  private final TimerWrapper matchTime;
  private final TimerWrapper assertionsTime;
  private final TimerWrapper actionTime;
  private final TimerWrapper totalStrengthenTime;
  private final StatIntHist automatonSuccessors;

  public AutomatonTransferRelation(
      ControlAutomatonCPA pCpa,
      LogManager pLogger,
      MachineModel pMachineModel,
      AutomatonStatistics pStats) {
    cpa = pCpa;
    logger = pLogger;
    machineModel = pMachineModel;

    totalPostTime = pStats.totalPostTime.getNewTimer();
    matchTime = pStats.matchTime.getNewTimer();
    assertionsTime = pStats.assertionsTime.getNewTimer();
    actionTime = pStats.actionTime.getNewTimer();
    totalStrengthenTime = pStats.totalStrengthenTime.getNewTimer();
    automatonSuccessors = pStats.automatonSuccessors;
  }

  @Override
  public Collection<AutomatonState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException {

    Preconditions.checkArgument(pElement instanceof AutomatonState);

    if (pElement instanceof AutomatonUnknownState) {
      // the last CFA edge could not be processed properly
      // (strengthen was not called on the AutomatonUnknownState or the strengthen operation had not
      // enough information to determine a new following state.)
      return ImmutableSet.of(cpa.getTopState());
    }

    Collection<AutomatonState> result =
        getAbstractSuccessors0((AutomatonState) pElement, pCfaEdge, pPrecision);
    automatonSuccessors.setNextValue(result.size());
    return result;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    return ImmutableSet.of(cpa.getTopState());
  }

  private Collection<AutomatonState> getAbstractSuccessors0(
      AutomatonState pElement, CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    totalPostTime.start();
    try {
      if (pElement instanceof AutomatonUnknownState) {
        // happens only inside MultiEdges,
        // here we have no chance (because strengthen is called only at the end of the edge),
        // so we just stay in the previous state
        pElement = ((AutomatonUnknownState) pElement).getPreviousState();
      }

      return getFollowStates(pElement, null, pCfaEdge, false, pPrecision);
    } finally {
      totalPostTime.stop();
    }
  }

  /**
   * Returns the <code>AutomatonStates</code> that follow this State in the ControlAutomatonCPA. If
   * the passed <code>AutomatonExpressionArguments</code> are not sufficient to determine the
   * following state this method returns a <code>AutomatonUnknownState</code> that contains this as
   * previous State. The strengthen method of the <code>AutomatonUnknownState</code> should be used
   * once enough Information is available to determine the correct following State.
   *
   * <p>If the state is a NonDet-State multiple following states may be returned. If the only
   * following state is BOTTOM an empty set is returned.
   */
  private ImmutableSet<AutomatonState> getFollowStates(
      AutomatonState state,
      List<AbstractState> otherElements,
      CFAEdge edge,
      boolean failOnUnknownMatch,
      Precision precision)
      throws CPATransferException {
    Preconditions.checkArgument(!(state instanceof AutomatonUnknownState));
    if (state == cpa.getBottomState()) {
      return ImmutableSet.of();
    }

    if (state.getInternalState().getTransitions().isEmpty()) {
      // shortcut
      return ImmutableSet.of(state);
    }

    if (precision instanceof AutomatonPrecision) {
      if (!((AutomatonPrecision) precision).isEnabled()) {
        if (state.isTarget()) {
          // do not create transition from target states
          return ImmutableSet.of();
        } else {
          // ignore disabled automaton
          return ImmutableSet.of(state);
        }
      }
    }

    ImmutableSet.Builder<AutomatonState> lSuccessors = ImmutableSet.builderWithExpectedSize(2);
    AutomatonExpressionArguments exprArgs =
        new AutomatonExpressionArguments(state, state.getVars(), otherElements, edge, logger);
    boolean edgeMatched = false;
    int failedMatches = 0;
    boolean nonDetState = state.getInternalState().isNonDetState();

    // these transitions cannot be evaluated until last, because they might have sideeffects on
    // other CPAs (dont want to execute them twice)
    // the transitionVariables have to be cached (produced during the match operation)
    // the list holds a Transition and the TransitionVariables generated during its match
    List<Pair<AutomatonTransition, Map<Integer, AAstNode>>> transitionsToBeTaken =
        new ArrayList<>(2);

    for (AutomatonTransition t : state.getInternalState().getTransitions()) {
      exprArgs.clearTransitionVariables();

      matchTime.start();
      ResultValue<Boolean> match = t.match(exprArgs);
      matchTime.stop();

      if (match.canNotEvaluate()) {
        if (failOnUnknownMatch) {
          throw new AutomatonTransferException(
              "Automaton transition condition could not be evaluated", match);
        }
        // if one transition cannot be evaluated the evaluation must be postponed until enough
        // information is available
        return ImmutableSet.of(new AutomatonUnknownState(state));
      } else {
        if (match.getValue()) {
          edgeMatched = true;
          assertionsTime.start();
          ResultValue<Boolean> assertionsHold = t.assertionsHold(exprArgs);
          assertionsTime.stop();

          if (assertionsHold.canNotEvaluate()) {
            if (failOnUnknownMatch) {
              throw new AutomatonTransferException(
                  "Automaton transition assertions could not be evaluated", assertionsHold);
            }
            // cannot yet be evaluated
            return ImmutableSet.of(new AutomatonUnknownState(state));

          } else if (assertionsHold.getValue()) {
            if (!t.canExecuteActionsOn(exprArgs)) {
              if (failOnUnknownMatch) {
                throw new AutomatonTransferException(
                    "Automaton transition action could not be executed");
              }
              // cannot yet execute, goto UnknownState
              return ImmutableSet.of(new AutomatonUnknownState(state));
            }

            // delay execution as described above
            Map<Integer, AAstNode> transitionVariables =
                ImmutableMap.copyOf(exprArgs.getTransitionVariables());
            transitionsToBeTaken.add(Pair.of(t, transitionVariables));

          } else {
            // matching transitions, but unfulfilled assertions: goto error state
            final String desc = Strings.nullToEmpty(t.getTargetInformation(exprArgs));
            AutomatonTargetInformation prop =
                new AutomatonTargetInformation(state.getOwningAutomaton(), t, desc);

            AutomatonState errorState =
                AutomatonState.automatonStateFactory(
                    ImmutableMap.of(),
                    AutomatonInternalState.ERROR,
                    state.getOwningAutomaton(),
                    0,
                    0,
                    prop,
                    state.isTreatingErrorsAsTarget());

            logger.log(
                Level.FINER,
                "Automaton going to ErrorState on edge \"" + edge.getDescription() + "\"");
            lSuccessors.add(errorState);
          }

          if (!nonDetState) {
            // not a nondet State, break on the first matching edge
            break;
          }
        } else {
          // do nothing if the edge did not match
          failedMatches++;
        }
      }
    }

    if (edgeMatched) {
      // execute Transitions
      for (Pair<AutomatonTransition, Map<Integer, AAstNode>> pair : transitionsToBeTaken) {
        // this transition will be taken. copy the variables
        AutomatonTransition t = pair.getFirst();
        Map<Integer, AAstNode> transitionVariables = pair.getSecond();
        actionTime.start();
        Map<String, AutomatonVariable> newVars = deepCloneVars(state.getVars());
        exprArgs.setAutomatonVariables(newVars);
        exprArgs.putTransitionVariables(transitionVariables);
        t.executeActions(exprArgs);
        actionTime.stop();

        AutomatonTargetInformation targetInformation = null;
        if (t.getFollowState().isTarget()) {
          final String desc = Strings.nullToEmpty(t.getTargetInformation(exprArgs));
          targetInformation = new AutomatonTargetInformation(state.getOwningAutomaton(), t, desc);
        }

        logger.log(Level.ALL, "Replace variables in automata assumptions");
        ImmutableList<AExpression> instantiatedAssumes =
            exprArgs.instantiateAssumptions(t.getAssumptions(edge, logger, machineModel));

        AutomatonState lSuccessor =
            AutomatonState.automatonStateFactory(
                newVars,
                t.getFollowState(),
                state.getOwningAutomaton(),
                instantiatedAssumes,
                t.getCandidateInvariants(),
                state.getMatches() + 1,
                state.getFailedMatches(),
                targetInformation,
                state.isTreatingErrorsAsTarget());

        if (!(lSuccessor instanceof AutomatonState.BOTTOM)) {
          lSuccessors.add(lSuccessor);
        } else {
          // add nothing
        }
      }
      return lSuccessors.build();
    } else {
      // stay in same state, no transitions to be executed here (no transition matched)
      AutomatonState stateNewCounters =
          AutomatonState.automatonStateFactory(
              state.getVars(),
              state.getInternalState(),
              state.getOwningAutomaton(),
              state.getMatches(),
              state.getFailedMatches() + failedMatches,
              null,
              state.isTreatingErrorsAsTarget());
      return ImmutableSet.of(stateNewCounters);
    }
  }

  private static Map<String, AutomatonVariable> deepCloneVars(Map<String, AutomatonVariable> pOld) {
    Map<String, AutomatonVariable> result = Maps.newHashMapWithExpectedSize(pOld.size());
    for (Entry<String, AutomatonVariable> e : pOld.entrySet()) {
      result.put(e.getKey(), e.getValue().clone());
    }
    return result;
  }

  @Override
  public Collection<AutomatonState> strengthen(
      AbstractState pElement,
      Iterable<AbstractState> pOtherElements,
      CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException {
    if (pElement instanceof AutomatonUnknownState) {
      totalStrengthenTime.start();
      Collection<AutomatonState> successors =
          strengthenAutomatonUnknownState(
              (AutomatonUnknownState) pElement, pOtherElements, pCfaEdge, pPrecision);
      totalStrengthenTime.stop();
      assert !from(successors).anyMatch(instanceOf(AutomatonUnknownState.class));
      return successors;
    }

    AutomatonState state = (AutomatonState) pElement;
    if (AutomatonGraphmlParser.WITNESS_AUTOMATON_NAME.equals(state.getOwningAutomatonName())) {
      /* In case of concurrent tasks, we need to go two steps:
       * The first step is the createThread edge of the witness.
       * The second step is the enterFunction edge of the witness.
       * As we currently only use one edge in the CFA to do both, we must execute transfer twice.
       */
      if (ThreadingTransferRelation.getCreatedThreadFunction(pCfaEdge).isPresent()) {
        Iterator<ThreadingState> possibleThreadingState =
            Iterables.filter(pOtherElements, ThreadingState.class).iterator();
        if (possibleThreadingState.hasNext()) {
          return handleThreadCreationForWitnessValidation(
              pCfaEdge, pPrecision, state, possibleThreadingState.next());
        }
      }
    }
    return ImmutableSet.of(state);
  }

  private Collection<AutomatonState> handleThreadCreationForWitnessValidation(
      CFAEdge pthreadCreateEdge,
      Precision pPrecision,
      AutomatonState state,
      ThreadingState threadingState)
      throws CPATransferException {
    ImmutableSet.Builder<AutomatonState> result = ImmutableSet.builder();
    for (CFAEdge firstEdgeOfThread : threadingState.getOutgoingEdges()) {
      if (firstEdgeOfThread.getPredecessor() instanceof FunctionEntryNode
          && firstEdgeOfThread.getPredecessor().getNumEnteringEdges() == 0) {
        assert firstEdgeOfThread instanceof BlankEdge
            : String.format(
                "unexpected type for edge '%s' of type '%s'",
                firstEdgeOfThread, firstEdgeOfThread.getClass());
        // create a complete function call for the new thread.
        // the new edge must fulfill several requirements, such that the matching succeeds:
        // - functionStart with correct location (source line, offset) of 'pthreadCreate' edge.
        // - no match on 'entry of main function'.
        // The simplest matching edge is a BlankEdge with a special description.
        CFAEdge dummyCallEdge =
            new BlankEdge(
                firstEdgeOfThread.getRawStatement(),
                pthreadCreateEdge.getFileLocation(),
                new CFANode(pthreadCreateEdge.getPredecessor().getFunction()),
                firstEdgeOfThread.getSuccessor(),
                "Function start dummy edge");
        Collection<AutomatonState> newStates =
            getAbstractSuccessorsForEdge(state, pPrecision, dummyCallEdge);

        // Assumption: "Every thread creation is directly followed by a function entry."
        // The witness automaton checks function names of CFA clones, thus the next line
        // cuts off all non-matching threads and limits the state space for the validation.
        result.addAll(from(newStates).filter(s -> !state.equals(s)));
      } else {
        result.add(state);
      }
    }
    return result.build();
  }

  /**
   * Strengthening might depend on the strengthening of other automaton states, so we do a
   * fixed-point iteration.
   */
  private Collection<AutomatonState> strengthenAutomatonUnknownState(
      AutomatonUnknownState lUnknownState,
      Iterable<AbstractState> pOtherElements,
      CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException {
    Set<List<AbstractState>> strengtheningCombinations = new LinkedHashSet<>();
    // need to use lists in set instead of iterable because the latter does not guarantee equals()
    strengtheningCombinations.add(ImmutableList.copyOf(pOtherElements));
    boolean changed = from(pOtherElements).anyMatch(instanceOf(AutomatonUnknownState.class));
    while (changed) {
      changed = false;
      Set<List<AbstractState>> newCombinations = new LinkedHashSet<>();
      for (List<AbstractState> otherStates : strengtheningCombinations) {
        Collection<List<AbstractState>> newPartialCombinations = new ArrayList<>();
        newPartialCombinations.add(new ArrayList<>());
        for (AbstractState otherState : otherStates) {
          AbstractState toAdd = otherState;
          if (otherState instanceof AutomatonUnknownState) {
            AutomatonUnknownState unknownState = (AutomatonUnknownState) otherState;

            // Compute the successors of the other unknown state
            List<AbstractState> statesOtherToCurrent = new ArrayList<>(otherStates);
            statesOtherToCurrent.remove(unknownState);
            statesOtherToCurrent.add(lUnknownState);
            Collection<? extends AbstractState> successors =
                getFollowStates(
                    unknownState.getPreviousState(),
                    statesOtherToCurrent,
                    pCfaEdge,
                    true,
                    pPrecision);

            // There might be zero or more than one successor,
            // so the list of states is multiplied with the list of successors
            Collection<List<AbstractState>> multipliedPartialCrossProduct = new ArrayList<>();
            for (List<AbstractState> newOtherStates : newPartialCombinations) {
              for (AbstractState successor : successors) {
                List<AbstractState> multipliedNewOtherStates = new ArrayList<>(newOtherStates);
                multipliedNewOtherStates.add(successor);
                multipliedPartialCrossProduct.add(multipliedNewOtherStates);
              }
            }
            newPartialCombinations = multipliedPartialCrossProduct;
          } else {
            // Not an (unknown) automaton state, so just add it at the end of each list
            for (List<AbstractState> newOtherStates : newPartialCombinations) {
              newOtherStates.add(toAdd);
            }
          }
        }
        newCombinations.addAll(newPartialCombinations);
      }
      changed = !strengtheningCombinations.equals(newCombinations);
      strengtheningCombinations = newCombinations;
    }

    // For each list of other states, do the strengthening
    Collection<AutomatonState> successors = new LinkedHashSet<>();
    for (List<AbstractState> otherStates : strengtheningCombinations) {
      successors.addAll(
          getFollowStates(
              lUnknownState.getPreviousState(), otherStates, pCfaEdge, true, pPrecision));
    }
    return successors;
  }
}
