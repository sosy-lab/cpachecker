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
package org.sosy_lab.cpachecker.cpa.apron;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import apron.Abstract0;
import apron.ApronException;

class ApronDomain implements AbstractDomain {

  private final LogManager logger;

  public ApronDomain(LogManager log) throws InvalidConfigurationException {
    logger = log;
  }

  @Override
  public boolean isLessOrEqual(AbstractState element1, AbstractState element2) {

    Map<ApronState, Set<ApronState>> covers = new HashMap<>();

    ApronState apronState1 = (ApronState) element1;
    ApronState apronState2 = (ApronState) element2;

    if (covers.containsKey(apronState2) && ((HashSet<ApronState>)(covers.get(apronState2))).contains(apronState1)) {
      return true;
    }

    try {
      return apronState1.isLessOrEquals(apronState2);
    } catch (ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
  }

  @Override
  public AbstractState join(AbstractState successor, AbstractState reached) {
    Pair<ApronState, ApronState> shrinkedStates;
    Abstract0 newApronState;
    ApronState firstState;
    try {
      shrinkedStates = getShrinkedStates((ApronState)successor, (ApronState)reached);
      firstState = shrinkedStates.getFirst();
      newApronState = firstState.getApronNativeState().joinCopy(firstState.getManager().getManager(), shrinkedStates.getSecond().getApronNativeState());

    } catch (ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }

    ApronState newState = new ApronState(newApronState,
                                         firstState.getManager(),
                                         shrinkedStates.getFirst().getIntegerVariableToIndexMap(),
                                         shrinkedStates.getFirst().getRealVariableToIndexMap(),
                                         shrinkedStates.getFirst().getVariableToTypeMap(),
                                         ((ApronState)successor).getBlock(),
                                         logger);
    if (newState.equals(reached)) {
      return reached;
    } else if (newState.equals(successor)) {
      return successor;
    } else {
      return newState;
    }
  }

  public AbstractState joinWidening(ApronState successorState, ApronState reachedState) {
    Pair<ApronState, ApronState> shrinkedStates;
    Abstract0 newApronState;
    try {
      shrinkedStates = getShrinkedStates(successorState, reachedState);
      successorState = shrinkedStates.getFirst();
      reachedState = shrinkedStates.getSecond();

      newApronState = reachedState.getApronNativeState().widening(reachedState.getManager().getManager(), successorState.getApronNativeState());

    } catch (ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }

    ApronState newState = new ApronState(newApronState,
                                         reachedState.getManager(),
                                         successorState.getIntegerVariableToIndexMap(),
                                         successorState.getRealVariableToIndexMap(),
                                         successorState.getVariableToTypeMap(),
                                         successorState.getBlock(),
                                         logger);
    if (newState.equals(successorState)) {
      return successorState;
    } else if (newState.equals(reachedState)) {
      return reachedState;
    } else {
      return newState;
    }
  }

  private Pair<ApronState, ApronState> getShrinkedStates(ApronState succ, ApronState reached) throws ApronException {
    if (succ.sizeOfVariables() > reached.sizeOfVariables()) {
      Pair<ApronState, ApronState> tmp = succ.shrinkToFittingSize(reached);
      succ = tmp.getFirst();
      reached = tmp.getSecond();
    } else {
      Pair<ApronState, ApronState> tmp = reached.shrinkToFittingSize(succ);
      succ = tmp.getSecond();
      reached = tmp.getFirst();
    }
    return Pair.of(succ, reached);
  }
}
