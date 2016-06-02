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
package org.sosy_lab.cpachecker.util.presence.interfaces;

import org.sosy_lab.common.Appender;

public interface PresenceConditionManager {

  public PresenceCondition makeTrue();

  public PresenceCondition makeFalse();

  public PresenceCondition makeNegation(PresenceCondition pNegationOf);

  public PresenceCondition makeOr(PresenceCondition pCond1, PresenceCondition pCond2);

  public PresenceCondition makeAnd(PresenceCondition pCond1, PresenceCondition pCond2);

  public Appender dump(PresenceCondition pCond);

  public boolean checkEntails(PresenceCondition pCond1, PresenceCondition pCond2)
      throws InterruptedException;

  public boolean checkConjunction(PresenceCondition pCond1, PresenceCondition pCond2)
      throws InterruptedException;

  public boolean checkSat(PresenceCondition pCond);

  public boolean checkEqualsTrue(PresenceCondition pCond);

  public boolean checkEqualsFalse(PresenceCondition pCond);

  public PresenceCondition removeMarkerVariables(PresenceCondition pCond);

}
