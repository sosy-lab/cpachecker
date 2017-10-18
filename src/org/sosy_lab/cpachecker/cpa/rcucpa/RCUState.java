/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.rcucpa;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.cpa.usage.UsageTreeNode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;

public class RCUState implements LatticeAbstractState<RCUState>, CompatibleState {
  private final Map<AbstractIdentifier, Set<AbstractIdentifier>> rcuRelations;
  private final Set<AbstractIdentifier> outdatedRCU;
  private final Set<AbstractIdentifier> localAgain;
  private final LockStateRCU lockState;

  RCUState(LockStateRCU pLockState, Map<AbstractIdentifier, Set<AbstractIdentifier>> pRcuRel,
                  Set<AbstractIdentifier> pOutdatedRCU, Set<AbstractIdentifier> pLocalAgain) {
    rcuRelations = pRcuRel;
    outdatedRCU = pOutdatedRCU;
    localAgain = pLocalAgain;
    lockState = pLockState;
  }

  RCUState() {
    this(new LockStateRCU(), new HashMap<>(), new HashSet<>(), new HashSet<>());
  }

  @Override
  public RCUState join(RCUState other) {
    return null;
  }

  @Override
  public boolean isLessOrEqual(RCUState other) throws CPAException, InterruptedException {
    if (!lockState.isLessOrEqual(other.lockState)) {
      return false;
    }

    Set<AbstractIdentifier> sub = new HashSet<>(rcuRelations.keySet());
    sub.retainAll(other.rcuRelations.keySet());
    if (sub.size() < rcuRelations.keySet().size()
        && sub.size() < other.rcuRelations.keySet().size()) {
      return false;
    } else {
      // TODO: ...
    }

    sub = new HashSet<>(outdatedRCU);
    sub.retainAll(other.outdatedRCU);
    if (sub.size() < outdatedRCU.size() && sub.size() < other.outdatedRCU.size()) {
      return false;
    }

    sub = new HashSet<>(localAgain);
    sub.retainAll(other.localAgain);
    if (sub.size() < localAgain.size() && sub.size() < other.localAgain.size()) {
      return false;
    }

    return true;
  }

  LockStateRCU getLockState() {
    return lockState;
  }

  void fillLocal() {
    localAgain.addAll(outdatedRCU);
    outdatedRCU.clear();
  }

  void addToOutdated(AbstractIdentifier pRcuPtr) {
    outdatedRCU.add(pRcuPtr);
    for (AbstractIdentifier id : rcuRelations.keySet()) {
      if (rcuRelations.get(id).contains(pRcuPtr)) {
        outdatedRCU.add(id);
      }
    }
  }

  void addToRelations(AbstractIdentifier pAil, AbstractIdentifier pInit) {
    if (!rcuRelations.containsKey(pAil)) {
      rcuRelations.put(pAil, new HashSet<>());
    }
    if (pInit != null) {
      rcuRelations.get(pAil).add(pInit);
    }
  }

  @Override
  public boolean isCompatibleWith(CompatibleState state) {
    Preconditions.checkArgument(state instanceof RCUState);
    return lockState.isCompatible(((RCUState) state).lockState);
  }

  @Override
  public CompatibleState prepareToStore() {
    return this;
  }

  @Override
  public UsageTreeNode getTreeNode() {
    // TODO: implement this
    return null;
  }

  @Override
  public int compareTo(CompatibleState o) {
    // TODO: implement this
    return 0;
  }

  @Override
  public String toString() {
    String result = "\nLock state: " + lockState.toString()
        + "\nRCU relations: " + rcuRelations
        + "\nOutdated RCU: " + outdatedRCU
        + "\nLocal Again: " + localAgain + '\n';
    return result;
  }

  public static RCUState copyOf(RCUState pState) {
    return new RCUState(pState.lockState, pState.rcuRelations, pState.outdatedRCU, pState.localAgain);
  }
}
