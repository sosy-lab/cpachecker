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

public abstract class LockEffectWithId implements AbstractLockEffect {

  protected final LockIdentifier target;

  LockEffectWithId(LockIdentifier id) {
    target = id;
  }

  LockEffectWithId() {
    this(null);
  }

  protected abstract String getAction();

  @Override
  public String toString() {
    String result = getAction();
    if (target != null) {
      result += " " + target;
    }
    return result;
  }

  public LockIdentifier getAffectedLock() {
    return target;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(target, getAction());
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof LockEffectWithId)) {
      return false;
    }
    LockEffectWithId other = (LockEffectWithId) obj;
    return Objects.equals(target, other.target) &&
           Objects.equals(getAction(), other.getAction());
  }


}
