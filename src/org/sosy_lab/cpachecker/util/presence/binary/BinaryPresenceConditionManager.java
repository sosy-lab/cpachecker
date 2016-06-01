/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.presence.binary;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceConditionManager;

import java.io.IOException;
import java.util.Set;


public class BinaryPresenceConditionManager implements PresenceConditionManager {

  @Override
  public PresenceCondition makeTrue() {
    return new BinaryPresenceCondition(true);
  }

  @Override
  public PresenceCondition makeFalse() {
    return new BinaryPresenceCondition(false);
  }

  @Override
  public boolean checkEntails(PresenceCondition pCond1, PresenceCondition pCond2) {
    Preconditions.checkArgument(pCond1 instanceof BinaryPresenceCondition);
    Preconditions.checkArgument(pCond2 instanceof BinaryPresenceCondition);

    BinaryPresenceCondition cond1 = (BinaryPresenceCondition) pCond1;
    BinaryPresenceCondition cond2 = (BinaryPresenceCondition) pCond2;

    return !cond1.getValue() || cond2.getValue();
  }

  @Override
  public PresenceCondition makeNegation(PresenceCondition pNegationOf) {
    Preconditions.checkArgument(pNegationOf instanceof BinaryPresenceCondition);
    BinaryPresenceCondition negationOf = (BinaryPresenceCondition) pNegationOf;

    return new BinaryPresenceCondition(!negationOf.getValue());
  }

  @Override
  public PresenceCondition makeOr(PresenceCondition pCond1, PresenceCondition pCond2) {
    Preconditions.checkArgument(pCond1 instanceof BinaryPresenceCondition);
    Preconditions.checkArgument(pCond2 instanceof BinaryPresenceCondition);

    BinaryPresenceCondition cond1 = (BinaryPresenceCondition) pCond1;
    BinaryPresenceCondition cond2 = (BinaryPresenceCondition) pCond2;

    return new BinaryPresenceCondition(cond1.getValue() || cond2.getValue());
  }

  @Override
  public boolean checkConjunction(PresenceCondition pCond1, PresenceCondition pCond2)
      throws InterruptedException {
    Preconditions.checkArgument(pCond1 instanceof BinaryPresenceCondition);
    Preconditions.checkArgument(pCond2 instanceof BinaryPresenceCondition);

    BinaryPresenceCondition cond1 = (BinaryPresenceCondition) pCond1;
    BinaryPresenceCondition cond2 = (BinaryPresenceCondition) pCond2;

    return cond1.getValue() && cond2.getValue();
  }

  @Override
  public PresenceCondition makeAnd(PresenceCondition pCond1, PresenceCondition pCond2) {
    Preconditions.checkArgument(pCond1 instanceof BinaryPresenceCondition);
    Preconditions.checkArgument(pCond2 instanceof BinaryPresenceCondition);

    BinaryPresenceCondition cond1 = (BinaryPresenceCondition) pCond1;
    BinaryPresenceCondition cond2 = (BinaryPresenceCondition) pCond2;

    return new BinaryPresenceCondition(cond1.getValue() && cond2.getValue());
  }

  @Override
  public Appender dump(PresenceCondition pCond) {
    Preconditions.checkArgument(pCond instanceof BinaryPresenceCondition);
    final BinaryPresenceCondition cond = (BinaryPresenceCondition) pCond;
    return new Appender() {
      @Override
      public void appendTo(Appendable pArg0) throws IOException {
        pArg0.append(cond.toString());
      }
    };
  }

  @Override
  public boolean checkSat(PresenceCondition pCond) {
    Preconditions.checkArgument(pCond instanceof BinaryPresenceCondition);
    final BinaryPresenceCondition cond = (BinaryPresenceCondition) pCond;

    return cond.getValue();
  }

  @Override
  public boolean checkEqualsTrue(PresenceCondition pCond) {
    Preconditions.checkArgument(pCond instanceof BinaryPresenceCondition);
    final BinaryPresenceCondition cond = (BinaryPresenceCondition) pCond;

    return cond.getValue();
  }

  @Override
  public boolean checkEqualsFalse(PresenceCondition pCond) {
    Preconditions.checkArgument(pCond instanceof BinaryPresenceCondition);
    final BinaryPresenceCondition cond = (BinaryPresenceCondition) pCond;

    return !cond.getValue();
  }

  @Override
  public Set<PresenceCondition> extractPredicates(PresenceCondition pCond) {
    // TODO: implement
    throw new UnsupportedOperationException();
  }

  @Override
  public PresenceCondition makeExists(PresenceCondition pF1, PresenceCondition... pF2) {
    // TODO: implement
    throw new UnsupportedOperationException();
  }

  @Override
  public PresenceCondition removeGoalVariables(PresenceCondition pCond) {
    // TODO: implement
    throw new UnsupportedOperationException();
  }

}
