// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock.effects;

import org.sosy_lab.cpachecker.cpa.lock.AbstractLockStateBuilder;

public class SaveStateLockEffect implements AbstractLockEffect {

  private static final SaveStateLockEffect instance = new SaveStateLockEffect();

  private SaveStateLockEffect() {}

  @Override
  public void effect(AbstractLockStateBuilder pBuilder) {
    pBuilder.setRestoreState();
  }

  public static SaveStateLockEffect getInstance() {
    return instance;
  }

  @Override
  public String toString() {
    return "Save state";
  }
}
