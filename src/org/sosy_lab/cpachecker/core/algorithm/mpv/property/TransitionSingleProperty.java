/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm.mpv.property;

import java.util.Set;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonSafetyProperty;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition;

/*
 * This property is represented by transitions to the specific target state
 * (assertions are not supported, because they do not have a names to differentiate them).
 */
public final class TransitionSingleProperty extends AbstractSingleProperty {

  private final Set<AutomatonTransition> targetTransition;

  public TransitionSingleProperty(String pName, Set<AutomatonTransition> pTargetTransition) {
    super(pName);
    targetTransition = pTargetTransition;
  }

  @Override
  public void disableProperty() {
    for (AutomatonTransition automatonTransition : targetTransition) {
      automatonTransition.disableTransition();
    }
  }

  @Override
  public void enableProperty() {
    for (AutomatonTransition automatonTransition : targetTransition) {
      automatonTransition.enableTransition();
    }
  }

  @Override
  public boolean isTarget(AutomatonState pState) {
    if (targetTransition.contains(
        ((AutomatonSafetyProperty) pState.getViolatedProperties().toArray()[0])
            .getAutomatonTransition())) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void checkIfRelevant() {
    for (AutomatonTransition automatonTransition : targetTransition) {
      if (automatonTransition.isRelevant()) {
        relevant = true;
        return;
      }
    }
  }
}
