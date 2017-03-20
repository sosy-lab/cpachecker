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
package org.sosy_lab.cpachecker.cpa.lock.effects;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;
import org.sosy_lab.cpachecker.cpa.lock.LockState.LockStateBuilder;


public class CheckLockEffect extends LockEffect {

  //Temporary usage until we will exactly know the parameters
  private final static CheckLockEffect instance = new CheckLockEffect();

  final boolean isTruth;
  final int p;

  private CheckLockEffect(int t, boolean truth, LockIdentifier id) {
    super(id);
    p = t;
    isTruth = truth;
  }

  private CheckLockEffect() {
    this(0, true, null);
  }

  @Override
  public void effect(LockStateBuilder pBuilder) {
    Preconditions.checkArgument(this != instance, "Temporary instance can not effect");
    Preconditions.checkArgument(target != null, "Lock identifier must be set");
    int previousP = pBuilder.getOldState().getCounter(target);
    boolean result = ((previousP == p) == isTruth);
    if (!result) {
      pBuilder.setAsFalseState();
    }
  }

  public static CheckLockEffect getInstance() {
    return instance;
  }

  public static CheckLockEffect createEffectForId(int t, boolean truth, LockIdentifier id) {
    return new CheckLockEffect(t, truth, id);
  }

  @Override
  public CheckLockEffect cloneWithTarget(LockIdentifier id) {
    return createEffectForId(this.p, this.isTruth, id);
  }
}
