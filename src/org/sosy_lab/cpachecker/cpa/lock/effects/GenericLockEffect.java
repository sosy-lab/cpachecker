// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock.effects;

import java.util.Objects;
import java.util.function.Consumer;
import org.sosy_lab.cpachecker.cpa.lock.AbstractLockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;

public class GenericLockEffect implements AbstractLockEffect {
  public final static GenericLockEffect SAVE_STATE =
      new GenericLockEffect("Save state", AbstractLockStateBuilder::setRestoreState);
  public final static GenericLockEffect RESTORE_ALL =
      new GenericLockEffect("Resore all", AbstractLockStateBuilder::restoreAll);

  private final String name;
  private final Consumer<AbstractLockStateBuilder> action;

  private GenericLockEffect(
      String pName,
      Consumer<AbstractLockStateBuilder> pAction) {
    action = pAction;
    name = pName;
  }

  @Override
  public void effect(AbstractLockStateBuilder pBuilder) {
    action.accept(pBuilder);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof GenericLockEffect)) {
      return false;
    }
    GenericLockEffect other = (GenericLockEffect) obj;
    return Objects.equals(name, other.name);
  }

  @Override
  public GenericLockEffect applyToTarget(LockIdentifier pId) {
    return this;
  }

}
