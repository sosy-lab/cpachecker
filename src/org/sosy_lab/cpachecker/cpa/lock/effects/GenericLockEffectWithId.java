// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock.effects;

import java.util.function.BiConsumer;
import org.sosy_lab.cpachecker.cpa.lock.AbstractLockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;

public class GenericLockEffectWithId extends LockEffectWithId {

  public final static GenericLockEffectWithId RELEASE =
      new GenericLockEffectWithId("Release", (b, l) -> b.free(l), null);

  public final static GenericLockEffectWithId RESET =
      new GenericLockEffectWithId("Reset", (b, l) -> b.reset(l), null);

  public final static GenericLockEffectWithId RESTORE =
      new GenericLockEffectWithId("Restore", (b, l) -> b.restore(l), null);

  private final String name;
  private final BiConsumer<AbstractLockStateBuilder, LockIdentifier> action;

  private GenericLockEffectWithId(
      String pName,
      BiConsumer<AbstractLockStateBuilder, LockIdentifier> pAction, LockIdentifier pId) {
    super(pId);
    action = pAction;
    name = pName;
  }

  @Override
  public GenericLockEffectWithId applyToTarget(LockIdentifier id) {
    return new GenericLockEffectWithId(name, action, id);
  }

  @Override
  public void effect(AbstractLockStateBuilder pBuilder) {
    action.accept(pBuilder, target);
  }

  @Override
  protected String getAction() {
    return name;
  }
}
