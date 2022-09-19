// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock.effects;

import java.util.Objects;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;

public abstract class LockEffect implements AbstractLockEffect {

  protected final LockIdentifier target;

  LockEffect(LockIdentifier id) {
    target = id;
  }

  public abstract LockEffect cloneWithTarget(LockIdentifier id);

  protected abstract String getAction();

  @Override
  public String toString() {
    return getAction() + " " + target;
  }

  public LockIdentifier getAffectedLock() {
    return target;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(target, getAction());
  }

  @Override
  @SuppressWarnings("EqualsGetClass") // on purpose, case-class structure with single equals()
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    LockEffect other = (LockEffect) obj;
    return Objects.equals(target, other.target) && Objects.equals(getAction(), other.getAction());
  }
}
