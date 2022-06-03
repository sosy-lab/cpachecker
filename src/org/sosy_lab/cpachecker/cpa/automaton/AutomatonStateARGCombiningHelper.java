// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class AutomatonStateARGCombiningHelper {

  private final Map<String, AutomatonInternalState> qualifiedAutomatonStateNameToInternalState;
  private final Map<String, Automaton> nameToAutomaton;

  public AutomatonStateARGCombiningHelper() {
    qualifiedAutomatonStateNameToInternalState = new HashMap<>();
    nameToAutomaton = new HashMap<>();
  }

  public boolean registerAutomaton(final AutomatonState pStateOfAutomata) {
    Automaton automaton = pStateOfAutomata.getOwningAutomaton();
    final String prefix = automaton.getName() + "::";
    String qualifiedName;

    if (nameToAutomaton.put(automaton.getName(), automaton) != null) {
      return false;
    }

    for (AutomatonInternalState internal : automaton.getStates()) {
      qualifiedName = prefix + internal.getName();
      if (qualifiedAutomatonStateNameToInternalState.put(qualifiedName, internal) != null) {
        return false;
      }
    }

    return true;
  }

  public AutomatonState replaceStateByStateInAutomatonOfSameInstance(final AutomatonState toReplace)
      throws CPAException {
    String qualifiedName =
        toReplace.getOwningAutomatonName() + "::" + toReplace.getInternalStateName();

    if (qualifiedAutomatonStateNameToInternalState.containsKey(qualifiedName)) {
      AutomatonTargetInformation targetInformation = null;

      if (toReplace.isTarget() && !toReplace.getTargetInformation().isEmpty()) {
        TargetInformation info = toReplace.getTargetInformation().iterator().next();
        assert info instanceof AutomatonTargetInformation;
        targetInformation = (AutomatonTargetInformation) info;
      }

      return AutomatonState.automatonStateFactory(
          toReplace.getVars(),
          qualifiedAutomatonStateNameToInternalState.get(qualifiedName),
          nameToAutomaton.get(toReplace.getOwningAutomatonName()),
          toReplace.getAssumptions(),
          toReplace.getCandidateInvariants(),
          toReplace.getMatches(),
          toReplace.getFailedMatches(),
          targetInformation,
          toReplace.isTreatingErrorsAsTarget());
    }

    throw new CPAException("Changing state failed, unknown state.");
  }

  public boolean considersAutomaton(final String pAutomatonName) {
    return nameToAutomaton.containsKey(pAutomatonName);
  }

  public static boolean endsInAssumptionTrueState(
      final AutomatonState pPredecessor, final CFAEdge pEdge, final LogManager pLogger) {
    Preconditions.checkNotNull(pPredecessor);
    Preconditions.checkArgument(!pPredecessor.getInternalState().isNonDetState());

    AutomatonExpressionArguments exprArgs =
        new AutomatonExpressionArguments(
            pPredecessor, pPredecessor.getVars(), null, pEdge, pLogger);
    try {
      for (AutomatonTransition transition : pPredecessor.getInternalState().getTransitions()) {
        exprArgs.clearTransitionVariables();
        ResultValue<Boolean> match;

        match = transition.getTrigger().eval(exprArgs);
        if (match.canNotEvaluate()) {
          return false;
        }

        if (match.getValue()) {
          if (transition.getFollowState().getName().equals("__TRUE")) {
            ResultValue<Boolean> assertionsHold = transition.assertionsHold(exprArgs);
            if (!assertionsHold.canNotEvaluate()
                && assertionsHold.getValue()
                && transition.canExecuteActionsOn(exprArgs)) {
              return true;
            }
          }
          return false;
        }
      }
    } catch (CPATransferException e) {
      return false;
    }

    return pPredecessor.getInternalState().getName().equals("__TRUE");
  }
}
