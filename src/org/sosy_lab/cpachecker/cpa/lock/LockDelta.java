// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.lock.LockState.LockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.cpa.usage.storage.Delta;

public class LockDelta extends ArrayList<LockEffect> implements Delta<CompatibleState> {

  private static final long serialVersionUID = -7138385491813901478L;

  public LockDelta(List<LockEffect> pEffects) {
    super(pEffects);
  }

  @Override
  public CompatibleState apply(CompatibleState pState) {
    LockState lockState = (LockState) pState;
    LockStateBuilder builder = lockState.builder();
    forEach(e -> e.effect(builder));
    // Possibly null in case of infeasible state
    return builder.build();
  }

  @Override
  public boolean covers(Delta<CompatibleState> pDelta) {
    LockDelta lDelta = (LockDelta) pDelta;
    return containsAll(lDelta);
  }

  @Override
  public Delta<CompatibleState> add(Delta<CompatibleState> pDelta) {
    LockDelta lDelta = (LockDelta) pDelta;
    if (lDelta.isEmpty()) {
      return this;
    } else if (isEmpty()) {
      return lDelta;
    }
    LockDelta result = new LockDelta(this);
    result.addAll(lDelta);
    return result;
  }

}
