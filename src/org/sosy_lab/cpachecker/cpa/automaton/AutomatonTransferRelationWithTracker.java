// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState.AutomatonUnknownState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTracker.TracingInformation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public class AutomatonTransferRelationWithTracker extends AutomatonTransferRelation {

  public AutomatonTransferRelationWithTracker(
      ControlAutomatonCPA pCpa,
      LogManager pLogger,
      MachineModel pMachineModel,
      AutomatonStatistics pStats) {
    super(pCpa, pLogger, pMachineModel, pStats);
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
  @Override
  protected ImmutableSet<AutomatonState> getFollowStates(
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

            track(state, errorState, edge, t);

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

        track(state, lSuccessor, edge, t);

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

  private void track(
      AutomatonState state,
      AutomatonState lSuccessor,
      CFAEdge edge,
      AutomatonTransition transition) {
    Automaton owningAutomaton = state.getOwningAutomaton();
    if (owningAutomaton instanceof AutomatonWithMetaData) {
      AutomatonWithMetaData metaData = (AutomatonWithMetaData) owningAutomaton;
      Map<AutomatonTransition, GraphMLTransition> data = metaData.getTransitions();
      if (data.containsKey(transition)) {
        AutomatonTracker.getInstance()
            .track(TracingInformation.of(state, lSuccessor, edge, data.get(transition)));
      }
    }
  }
}
