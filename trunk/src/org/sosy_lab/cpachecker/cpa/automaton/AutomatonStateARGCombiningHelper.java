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
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Map;


public class AutomatonStateARGCombiningHelper {

  private final Map<String, AutomatonInternalState> qualifiedAutomatonStateNameToInternalState;
  private final Map<String, ControlAutomatonCPA> nameToCPA;

  public AutomatonStateARGCombiningHelper() {
    qualifiedAutomatonStateNameToInternalState = Maps.newHashMap();
    nameToCPA = Maps.newHashMap();
  }

  public boolean registerAutomaton(final AutomatonState pStateOfAutomata) {
    ControlAutomatonCPA automatonCPA = pStateOfAutomata.getAutomatonCPA();
    final String prefix = automatonCPA.getAutomaton().getName() + "::";
    String qualifiedName;

    if (nameToCPA.put(automatonCPA.getAutomaton().getName(), automatonCPA) != null) {
      return false;
    }

    for (AutomatonInternalState internal : automatonCPA.getAutomaton().getStates()) {
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

      if (toReplace.isTarget() && toReplace.getViolatedProperties().size() > 0) {
        Property prop = toReplace.getViolatedProperties().iterator().next();
        assert prop instanceof AutomatonSafetyProperty;
        violatedProp = (AutomatonSafetyProperty) prop;
      }

      return AutomatonState.automatonStateFactory(
          toReplace.getVars(),
          qualifiedAutomatonStateNameToInternalState.get(qualifiedName),
          nameToCPA.get(toReplace.getOwningAutomatonName()),
          toReplace.getAssumptions(),
          toReplace.getCandidateInvariants(),
          toReplace.getMatches(),
          toReplace.getFailedMatches(),
          violatedProp);
    }

    throw new CPAException("Changing state failed, unknown state.");
  }

  public boolean considersAutomaton(final String pAutomatonName) {
    return nameToCPA.containsKey(pAutomatonName);
  }

  public static boolean endsInAssumptionTrueState(final AutomatonState pPredecessor, final CFAEdge pEdge) {
    Preconditions.checkNotNull(pPredecessor);
    try {
      for (AbstractState successor : pPredecessor.getAutomatonCPA().getTransferRelation()
          .getAbstractSuccessorsForEdge(pPredecessor, SingletonPrecision.getInstance(), pEdge)) {
        if (!((AutomatonState) successor).getInternalStateName().equals("__TRUE")) {
          return false;
        }
      }
    } catch (CPATransferException e) {
      return false;
    }
    return true;
  }

}
