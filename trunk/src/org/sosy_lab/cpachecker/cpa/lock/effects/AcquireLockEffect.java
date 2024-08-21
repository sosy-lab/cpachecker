// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock.effects;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.cpa.lock.AbstractLockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;

public class AcquireLockEffect extends LockEffect {

  private static final class StopAcquireLockEffect extends AcquireLockEffect {

    private StopAcquireLockEffect(LockIdentifier id, int t) {
      super(id, t);
    }

    @Override
    public void effect(AbstractLockStateBuilder pBuilder) {
      Preconditions.checkArgument(target != null, "Lock identifier must be set");
      int previousP = pBuilder.getOldState().getCounter(target);
      if (maxRecursiveCounter > previousP) {
        pBuilder.add(target);
      } else {
        pBuilder.setAsFalseState();
      }
    }

    @Override
    public AcquireLockEffect cloneWithTarget(LockIdentifier id) {
      return createEffectForId(id, maxRecursiveCounter, true);
    }
  }

  private static final AcquireLockEffect instance = new AcquireLockEffect();

  private static final NavigableMap<LockIdentifier, AcquireLockEffect> AcquireLockEffectMap =
      new TreeMap<>();

  protected final int maxRecursiveCounter;

  private AcquireLockEffect(LockIdentifier id, int t) {
    super(id);
    maxRecursiveCounter = t;
  }

  private AcquireLockEffect() {
    this(null, Integer.MAX_VALUE);
  }

  @Override
  public void effect(AbstractLockStateBuilder pBuilder) {
    Preconditions.checkArgument(target != null, "Lock identifier must be set");
    int previousP = pBuilder.getOldState().getCounter(target);
    if (maxRecursiveCounter > previousP) {
      pBuilder.add(target);
    }
  }

  public static AcquireLockEffect getInstance() {
    return instance;
  }

  public static AcquireLockEffect createEffectForId(LockIdentifier id, int counter, boolean stop) {
    AcquireLockEffect result;
    if (AcquireLockEffectMap.containsKey(id)) {
      result = AcquireLockEffectMap.get(id);
      checkArgument(result.maxRecursiveCounter == counter, "Recursive counter differs for %s", id);
    } else {
      if (stop) {
        result = new StopAcquireLockEffect(id, counter);
      } else {
        result = new AcquireLockEffect(id, counter);
      }
      AcquireLockEffectMap.put(id, result);
    }
    return result;
  }

  public static AcquireLockEffect createEffectForId(LockIdentifier id) {
    if (AcquireLockEffectMap.containsKey(id)) {
      return AcquireLockEffectMap.get(id);
    } else {
      // Means that the lock is set ('lock = 1'), not store it
      return new AcquireLockEffect(id, Integer.MAX_VALUE);
    }
  }

  @Override
  public AcquireLockEffect cloneWithTarget(LockIdentifier id) {
    return createEffectForId(id, maxRecursiveCounter, false);
  }

  @Override
  public String getAction() {
    return "Acquire";
  }
}
