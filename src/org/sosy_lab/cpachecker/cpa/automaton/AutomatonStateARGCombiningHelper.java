/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Property;
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

  public AutomatonState replaceStateByStateInAutomatonOfSameInstance(final AutomatonState toReplace) throws CPAException {
    String qualifiedName = toReplace.getOwningAutomatonName()+"::" +toReplace.getInternalStateName();

    if (qualifiedAutomatonStateNameToInternalState.containsKey(qualifiedName)) {
      AutomatonSafetyProperty violatedProp = null;

      if (toReplace.isTarget() && !toReplace.getViolatedProperties().isEmpty()) {
        Property prop = toReplace.getViolatedProperties().iterator().next();
        assert prop instanceof AutomatonSafetyProperty;
        violatedProp = (AutomatonSafetyProperty) prop;
      }

      return AutomatonState.automatonStateFactory(
          toReplace.getVars(),
          qualifiedAutomatonStateNameToInternalState.get(qualifiedName),
          nameToAutomaton.get(toReplace.getOwningAutomatonName()),
          toReplace.getAssumptions(),
          toReplace.getCandidateInvariants(),
          toReplace.getMatches(),
          toReplace.getFailedMatches(),
          violatedProp,
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
