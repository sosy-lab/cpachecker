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
import com.google.common.collect.FluentIterable;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.refinement.AliasInfoProvider;
import org.sosy_lab.cpachecker.cpa.usage.refinement.LocalInfoProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

public class RCUState implements LatticeAbstractState<RCUState>, CompatibleState, CompatibleNode,
                                 LocalInfoProvider, AliasInfoProvider {
  private final Multimap<AbstractIdentifier, AbstractIdentifier> rcuRelations;
  private final Set<AbstractIdentifier> outdatedRCU;
  private final Set<AbstractIdentifier> localAgain;
  private final LockStateRCU lockState;
  private final Map<AbstractIdentifier, AbstractIdentifier> tmpMapping;

  RCUState(LockStateRCU pLockState, Multimap<AbstractIdentifier, AbstractIdentifier> pRcuRel,
           Set<AbstractIdentifier> pOutdatedRCU, Set<AbstractIdentifier> pLocalAgain,
           Map<AbstractIdentifier, AbstractIdentifier> pTmpMapping) {
    lockState = LockStateRCU.copyOf(pLockState);
    rcuRelations = ImmutableMultimap.copyOf(pRcuRel);
    outdatedRCU = ImmutableSet.copyOf(pOutdatedRCU);
    localAgain = ImmutableSet.copyOf(pLocalAgain);
    tmpMapping = ImmutableMap.copyOf(pTmpMapping);
  }

  RCUState() {
    this(new LockStateRCU(), LinkedListMultimap.create(), new HashSet<>(), new HashSet<>(), new HashMap<>());
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

  RCUState fillLocal() {
    Set<AbstractIdentifier> local = new HashSet<>(localAgain);
    local.addAll(outdatedRCU);
    return new RCUState(lockState, rcuRelations,
                        ImmutableSet.of(), local, tmpMapping);
  }

  RCUState addToOutdated(AbstractIdentifier pRcuPtr) {
    Set<AbstractIdentifier> outdated = new HashSet<>(outdatedRCU);
    outdated.add(pRcuPtr);
    for (AbstractIdentifier id : rcuRelations.keySet()) {
      if (rcuRelations.get(id).contains(pRcuPtr)) {
        outdated.add(id);
      }
    }
    return new RCUState(lockState, rcuRelations,
                        outdated, localAgain, tmpMapping);
  }

  RCUState addToRelations(AbstractIdentifier pAil, AbstractIdentifier pInit) {
    if (pInit != null) {
      Multimap<AbstractIdentifier, AbstractIdentifier> relations = LinkedListMultimap.create(rcuRelations);
      relations.put(pAil, pInit);
      return new RCUState(lockState, relations,
                          outdatedRCU, localAgain, tmpMapping);
    }
    return this;
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
  public CompatibleNode getTreeNode() {
    return this;
  }

  @Override
  public int compareTo(CompatibleState o) {
    // TODO: implement this
    try {
      if (this.isLessOrEqual((RCUState) o)) {
        return 0;
      } else {
        return 1;
      }
    } catch (CPAException pE) {

    } catch (InterruptedException pE) {

    }
    return -1;
  }

  @Override
  public String toString() {
    String result = "Lock state: " + lockState.toString()
        + "\nRCU relations: " + rcuRelations
        + "\nOutdated RCU: " + outdatedRCU
        + "\nLocal Again: " + localAgain
        + "\nTmp mapping: " + tmpMapping;
    return result;
  }

  public static RCUState copyOf(RCUState pState) {
    return new RCUState(LockStateRCU.copyOf(pState.lockState),
                        LinkedListMultimap.create(pState.rcuRelations),
                        new HashSet<>(pState.outdatedRCU),
                        new HashSet<>(pState.localAgain),
                        new HashMap<>(pState.tmpMapping));
  }

  @Override
  public boolean cover(CompatibleNode node) {
    // TODO: possible optimization
    return false;
  }

  @Override
  public boolean isLocal(GeneralIdentifier id) {
    if (!localAgain.isEmpty()) {
      FluentIterable<GeneralIdentifier> genIds = from(localAgain).transform
          (AbstractIdentifier::getGeneralId);
      if (genIds.anyMatch(i -> i.equals(id))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }

    RCUState rcuState = (RCUState) pO;

    if (!rcuRelations.equals(rcuState.rcuRelations)) {
      return false;
    }
    if (!outdatedRCU.equals(rcuState.outdatedRCU)) {
      return false;
    }
    if (!localAgain.equals(rcuState.localAgain)) {
      return false;
    }
    if (!lockState.equals(rcuState.lockState)) {
      return false;
    }
    if (!tmpMapping.equals(rcuState.tmpMapping)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = rcuRelations.hashCode();
    result = 31 * result + outdatedRCU.hashCode();
    result = 31 * result + localAgain.hashCode();
    result = 31 * result + lockState.hashCode();
    result = 31 * result + tmpMapping.hashCode();
    return result;
  }

  @Override
  public Set<AbstractIdentifier> getAllPossibleIds(AbstractIdentifier id) {
    Set<AbstractIdentifier> result = new HashSet<>();

    if (id instanceof SingleIdentifier) {
      SingleIdentifier sid = (SingleIdentifier) id;
      if (sid.getDereference() > 0) {
        for (int i = 0; i <= sid.getDereference(); ++i) {
          AbstractIdentifier clone = sid.cloneWithDereference(i);
          if (rcuRelations.containsKey(clone)) {
            result.addAll(rcuRelations.get(clone));
          }
          if (rcuRelations.containsValue(clone)) {
            for (Entry<AbstractIdentifier, AbstractIdentifier> entry : rcuRelations.entries()) {
              if (entry.getValue().equals(clone)) {
                result.add(entry.getKey());
              }
            }
          }
        }
      }
    }

    return result;
  }

  @Override
  public Set<AbstractIdentifier> getUnnecessaryIds(AbstractIdentifier pIdentifier, Set<AbstractIdentifier> pSet) {
    return Collections.emptySet();
  }

  RCUState incRCURead() {
    LockStateRCU lock = LockStateRCU.copyOf(lockState);
    lock.incRCURead();
    return new RCUState(lock, rcuRelations,
                        outdatedRCU, localAgain, tmpMapping);
  }

  RCUState decRCURead() {
    LockStateRCU lock = LockStateRCU.copyOf(lockState);
    lock.decRCURead();
    return new RCUState(lock, rcuRelations,
        outdatedRCU, localAgain, tmpMapping);
  }

  RCUState markRead() {
    LockStateRCU lock = LockStateRCU.copyOf(lockState);
    lock.markRead();
    return new RCUState(lock, rcuRelations,
        ImmutableSet.copyOf(outdatedRCU), ImmutableSet.copyOf(localAgain), ImmutableMap.copyOf(tmpMapping));
  }

  RCUState markWrite() {
    LockStateRCU lock = LockStateRCU.copyOf(lockState);
    lock.markWrite();
    return new RCUState(lock, rcuRelations,
        outdatedRCU, localAgain, tmpMapping);
  }

  RCUState clearLock() {
    LockStateRCU lock = LockStateRCU.copyOf(lockState);
    lock.clearLock();
    return new RCUState(lock, rcuRelations,
        outdatedRCU, localAgain, tmpMapping);
  }

  RCUState addTmpMapping(AbstractIdentifier tmp, AbstractIdentifier nonTmp) {
    Map<AbstractIdentifier, AbstractIdentifier> map = new HashMap<>(tmpMapping);
    map.put(tmp, nonTmp);
    return new RCUState(lockState, rcuRelations,
        outdatedRCU, localAgain, ImmutableMap.copyOf(map));
  }

  public AbstractIdentifier getNonTemporaryId(AbstractIdentifier pId) {
    return tmpMapping.get(pId);
  }
}
