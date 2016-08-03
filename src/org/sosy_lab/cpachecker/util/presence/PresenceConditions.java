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
package org.sosy_lab.cpachecker.util.presence;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithPresenceCondition;
import org.sosy_lab.cpachecker.cpa.bdd.BDDState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceConditionManager;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Nullable;

public class PresenceConditions {

  public static PresenceConditionManager manager() {
    return GlobalInfo.getInstance().getPresenceConditionManager();
  }

  public static PresenceCondition composeRemainingPresenceConditions(Set<Goal> pTestGoalsToBeProcessed,
      TestSuite testsuite) {

    PresenceCondition presenceCondition = manager().makeFalse();
    for (Goal goal : pTestGoalsToBeProcessed) {
      presenceCondition = manager().makeOr(presenceCondition, testsuite.getRemainingPresenceCondition(goal));
    }

    return presenceCondition;
  }

  public static PresenceCondition orTrue(Optional<PresenceCondition> pPc) {
    if (pPc == null || !pPc.isPresent()) {
      return manager().makeTrue();
    }
    return pPc.get();
  }

  public static PresenceCondition orTrue(@Nullable PresenceCondition pPc) {
    if (pPc == null) {
      return manager().makeTrue();
    }
    return pPc;
  }

  public static PresenceCondition orFalse(Optional<PresenceCondition> pPc) {
    if (pPc == null || !pPc.isPresent()) {
      return manager().makeFalse();
    }
    return pPc.get();
  }

  public static PresenceCondition orFalse(@Nullable PresenceCondition pPc) {
    if (pPc == null) {
      return manager().makeFalse();
    }
    return pPc;
  }

  public static Appender dump(final PresenceCondition pPc) {
    return new Appender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        manager().dump(pPc).appendTo(pAppendable);
      }

      @Override
      public String toString() {
        StringBuilder result = new StringBuilder();
        try {
          manager().dump(pPc).appendTo(result);
        } catch (IOException pE) {
          return "EXCEPTION";
        }
        return result.toString();
      }
    };
  }

}
