/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.cpa.lock;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import org.sosy_lab.cpachecker.cpa.lock.effects.AbstractLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.AcquireLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.SetLockEffect;
import org.sosy_lab.cpachecker.util.Pair;

public class LockPrecision implements AdjustablePrecision {

  private final Multimap<CFANode, LockIdentifier> precision;

  public LockPrecision() {
    precision = ArrayListMultimap.create();
  }

  public LockPrecision(Collection<Pair<CFANode, LockIdentifier>> pSet) {
    precision = ArrayListMultimap.create();
    for (Pair<CFANode, LockIdentifier> p : pSet) {
      CFANode node = p.getFirst();
      LockIdentifier id = p.getSecond();
      if (!precision.containsEntry(node, id)) {
        precision.put(p.getFirst(), p.getSecond());
      }
    }
  }

  private LockPrecision(Multimap<CFANode, LockIdentifier> newPrecision) {
    precision = ArrayListMultimap.create(newPrecision);
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision pOtherPrecision) {
    LockPrecision other = (LockPrecision) pOtherPrecision;
    LockPrecision result = new LockPrecision(precision);
    result.precision.putAll(other.precision);
    return result;
  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision pOtherPrecision) {
    LockPrecision other = (LockPrecision) pOtherPrecision;
    LockPrecision result = new LockPrecision(precision);
    other.precision.forEach((k, v) -> result.precision.remove(k, v));
    return result;
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

}
