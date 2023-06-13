// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import org.sosy_lab.cpachecker.cpa.lock.effects.AbstractLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.AcquireLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.SetLockEffect;

public class LockPrecision implements AdjustablePrecision {

  private final ImmutableSetMultimap<CFANode, LockIdentifier> precision;

  public LockPrecision() {
    precision = ImmutableSetMultimap.of();
  }

  public LockPrecision(Iterable<Entry<CFANode, LockIdentifier>> pSet) {
    precision = ImmutableSetMultimap.copyOf(pSet);
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision pOtherPrecision) {
    LockPrecision other = (LockPrecision) pOtherPrecision;
    return new LockPrecision(Iterables.concat(other.precision.entries(), precision.entries()));
  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision pOtherPrecision) {
    LockPrecision other = (LockPrecision) pOtherPrecision;
    return new LockPrecision(Sets.difference(other.precision.entries(), precision.entries()));
  }

  @Override
  public boolean isEmpty() {
    return precision.isEmpty();
  }

  public List<AbstractLockEffect> filter(CFANode pNode, List<AbstractLockEffect> pEffects) {
    Collection<LockIdentifier> ids = precision.get(pNode);
    List<AbstractLockEffect> filteredEffects = new ArrayList<>();

    for (AbstractLockEffect e : pEffects) {
      if (!(e instanceof AcquireLockEffect || e instanceof SetLockEffect)
          || ids.contains(((LockEffect) e).getAffectedLock())) {
        filteredEffects.add(e);
      }
    }
    return filteredEffects;
  }

  int getKeySize() {
    return precision.size();
  }

  String getValues() {
    return new HashSet<>(precision.values()).toString();
  }
}
