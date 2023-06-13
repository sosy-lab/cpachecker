// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock.effects;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.lock.AbstractLockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;

public final class CheckLockEffect extends LockEffect {

  // Temporary usage until we will exactly know the parameters
  private static final CheckLockEffect instance = new CheckLockEffect();

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
  public void effect(AbstractLockStateBuilder pBuilder) {
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
    return createEffectForId(p, isTruth, id);
  }

  @Override
  public String getAction() {
    return "Check";
  }
}
