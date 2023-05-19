// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock.effects;

import org.sosy_lab.cpachecker.cpa.lock.AbstractLockStateBuilder;

public class RestoreAllLockEffect implements AbstractLockEffect {

  private static final RestoreAllLockEffect instance = new RestoreAllLockEffect();

  private RestoreAllLockEffect() {}

  @Override
  public void effect(AbstractLockStateBuilder pBuilder) {
    pBuilder.restoreAll();
  }

  public static RestoreAllLockEffect getInstance() {
    return instance;
  }
}
