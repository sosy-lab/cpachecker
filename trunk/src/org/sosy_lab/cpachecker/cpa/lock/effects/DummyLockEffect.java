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

public class DummyLockEffect implements AbstractLockEffect {

  private static final DummyLockEffect instance = new DummyLockEffect();

  private DummyLockEffect() {}

  @Override
  public void effect(AbstractLockStateBuilder pBuilder) {
    Preconditions.checkArgument(false, "DummyEffect can not effect anything");
  }

  public static DummyLockEffect getInstance() {
    return instance;
  }
}
