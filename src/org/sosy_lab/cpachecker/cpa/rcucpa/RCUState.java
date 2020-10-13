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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.usage.AliasInfoProvider;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.cpa.usage.LocalInfoProvider;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

public class RCUState implements LatticeAbstractState<RCUState>,
    CompatibleNode, LocalInfoProvider, AliasInfoProvider {
  private final ImmutableMultimap<AbstractIdentifier, AbstractIdentifier> rcuRelations;
  private final ImmutableSet<AbstractIdentifier> outdatedRCU;
  private final ImmutableSet<GeneralIdentifier> localAgain;
  private final LockStateRCU lockState;
  private final ImmutableMap<AbstractIdentifier, AbstractIdentifier> temporaryIds;

  RCUState(
      LockStateRCU pLockState,
      ImmutableMultimap<AbstractIdentifier, AbstractIdentifier> pRcuRel,
      ImmutableSet<AbstractIdentifier> pOutdatedRCU,
      ImmutableSet<GeneralIdentifier> pLocalAgain,
      ImmutableMap<AbstractIdentifier, AbstractIdentifier> pTmpMapping) {
    lockState = pLockState;
    rcuRelations = pRcuRel;
    outdatedRCU = pOutdatedRCU;
    localAgain = pLocalAgain;
    temporaryIds = pTmpMapping;
  }

  RCUState() {
    this(
        new LockStateRCU(),
        ImmutableMultimap.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableMap.of());
  }

  @Override
  public RCUState join(RCUState other) {
    Multimap<AbstractIdentifier, AbstractIdentifier> newRel = HashMultimap.create(rcuRelations);
    for (Entry<AbstractIdentifier, AbstractIdentifier> entry : other.rcuRelations.entries()) {
      newRel.put(entry.getKey(), entry.getValue());
    }
    Set<AbstractIdentifier> newOutdated = new TreeSet<>(this.outdatedRCU);
    newOutdated.addAll(other.outdatedRCU);

    Set<GeneralIdentifier> newLocal = new TreeSet<>(this.localAgain);
    newLocal.addAll(other.localAgain);

    Map<AbstractIdentifier, AbstractIdentifier> newTmp = new TreeMap<>(this.temporaryIds);
    for (Entry<AbstractIdentifier, AbstractIdentifier> entry : other.temporaryIds.entrySet()) {
      newTmp.putIfAbsent(entry.getKey(), entry.getValue());
    }

    LockStateRCU newLock = this.lockState.join(other.lockState);

    return new RCUState(
        newLock,
        ImmutableMultimap.copyOf(newRel),
        ImmutableSet.copyOf(newOutdated),
        ImmutableSet.copyOf(newLocal),
        ImmutableMap.copyOf(newTmp));
  }

  @Override
  public boolean isLessOrEqual(RCUState other) {
    // TODO comparing by sizes looks strange
    if (!lockState.isLessOrEqual(other.lockState)) {
      return false;
    }

    if (!other.rcuRelations.keySet().containsAll(rcuRelations.keySet())) {
      return false;
    } else {
      // TODO: ...
    }

    if (!other.outdatedRCU.containsAll(outdatedRCU)) {
      return false;
    }

    return other.localAgain.containsAll(localAgain);
  }

  RCUState fillLocal() {
    ImmutableSet<GeneralIdentifier> local =
        FluentIterable
            .concat(
                from(outdatedRCU).filter(SingleIdentifier.class)
                    .transform(SingleIdentifier::getGeneralId),
                localAgain)
            .toSet();

    return new RCUState(lockState, rcuRelations,
        ImmutableSet.of(),
        local,
        temporaryIds);
  }

  RCUState addToOutdated(AbstractIdentifier pRcuPtr) {
    Set<AbstractIdentifier> outdated = new TreeSet<>(outdatedRCU);
    outdated.add(pRcuPtr);
    for (Entry<AbstractIdentifier, AbstractIdentifier> entry : rcuRelations.entries()) {
      if (entry.getValue().equals(pRcuPtr)) {
        outdated.add(entry.getKey());
      }
    }
    return new RCUState(lockState, rcuRelations,
        ImmutableSet.copyOf(outdated),
        localAgain,
        temporaryIds);
  }

  RCUState addToRelations(AbstractIdentifier pAil, AbstractIdentifier pInit) {
    if (pInit != null) {
      Multimap<AbstractIdentifier, AbstractIdentifier> relations = LinkedListMultimap.create(rcuRelations);
      relations.put(pAil, pInit);
      return new RCUState(
          lockState,
          ImmutableMultimap.copyOf(relations),
                          outdatedRCU, localAgain, temporaryIds);
    }
    return this;
  }

  @Override
  public boolean isCompatibleWith(CompatibleState state) {
    Preconditions.checkArgument(state instanceof RCUState);
    return lockState.isCompatible(((RCUState) state).lockState);
  }

  @Override
  public int compareTo(CompatibleState o) {
    Preconditions.checkArgument(o instanceof RCUState);
    RCUState other = (RCUState) o;
    int res = lockState.compareTo(other.lockState);
    if (res != 0) {
      return res;
    }
    if (equals(o)) {
      return 0;
    }
    res = rcuRelations.size() - other.rcuRelations.size();
    if (res != 0) {
      if (rcuRelations.entries().containsAll(other.rcuRelations.entries())) {
        return 1;
      }
      if (other.rcuRelations.entries().containsAll(rcuRelations.entries())) {
        return -1;
      }
      return res;
    }
    res = outdatedRCU.size() - other.outdatedRCU.size();
    if (res != 0) {
      if (outdatedRCU.containsAll(other.outdatedRCU)) {
        return 1;
      }
      if (other.outdatedRCU.containsAll(outdatedRCU)) {
        return -1;
      }
      return res;
    }
    res = localAgain.size() - other.localAgain.size();
    if (res != 0) {
      if (localAgain.containsAll(other.localAgain)) {
        return 1;
      }
      if (other.localAgain.containsAll(localAgain)) {
        return -1;
      }
      return res;
    }
    res = temporaryIds.size() - other.temporaryIds.size();
    if (res != 0) {
      if (temporaryIds.entrySet().containsAll(other.temporaryIds.entrySet())) {
        return 1;
      }
      if (other.temporaryIds.entrySet().containsAll(temporaryIds.entrySet())) {
        return -1;
      }
      return res;
    }
    // TODO: No ideas
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    String result = "Lock state: " + lockState.toString()
        + "\nRCU relations: " + rcuRelations
        + "\nOutdated RCU: " + outdatedRCU
        + "\nLocal Again: " + localAgain
        + "\nTmp mapping: " + temporaryIds;
    return result;
  }

  public static RCUState copyOf(RCUState pState) {
    return new RCUState(
        pState.lockState,
                        pState.rcuRelations,
                        pState.outdatedRCU,
                        pState.localAgain,
                        pState.temporaryIds);
  }

  @Override
  public boolean cover(CompatibleNode node) {
    // TODO: possible optimization
    return equals(node);
  }

  @Override
  public boolean isLocal(GeneralIdentifier id) {
    if (!localAgain.isEmpty()) {
      for (int i = 0; i <= id.getDereference(); i++) {
        GeneralIdentifier gId = (GeneralIdentifier) id.cloneWithDereference(i);
        if (localAgain.contains(gId)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof RCUState)) {
      return false;
    }

    RCUState rcuState = (RCUState) pO;

    // Problems with equals of Multimap
    return Objects.equals(rcuRelations.asMap(), rcuState.rcuRelations.asMap())
        && Objects.equals(
            outdatedRCU,
            rcuState.outdatedRCU)
        && Objects.equals(localAgain, rcuState.localAgain)
        && Objects.equals(lockState, rcuState.lockState)
        && Objects.equals(temporaryIds, rcuState.temporaryIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rcuRelations, outdatedRCU, localAgain, lockState, temporaryIds);
  }

  @Override
  public Collection<AbstractIdentifier> getAllPossibleAliases(AbstractIdentifier id) {
    Set<AbstractIdentifier> result = new TreeSet<>();

    addAliasesFor(id, result);

    return result;
  }

  private void addAliasesFor(AbstractIdentifier id, Collection<AbstractIdentifier> result) {
    if (id.getDereference() > 0) {
      for (int i = 0; i <= id.getDereference(); ++i) {
        AbstractIdentifier clone = id.cloneWithDereference(i);
        if (rcuRelations.containsKey(clone)) {
          for (AbstractIdentifier alias : rcuRelations.get(clone)) {
            AbstractIdentifier clonnedAlias =
                alias.cloneWithDereference(alias.getDereference() + id.getDereference() - i);
            if (result.add(clonnedAlias)) {
              addAliasesFor(clonnedAlias, result);
            }
          }
        }
        if (rcuRelations.containsValue(clone)) {
          for (Entry<AbstractIdentifier, AbstractIdentifier> entry : rcuRelations.entries()) {
            if (entry.getValue().equals(clone)) {
              AbstractIdentifier alias = entry.getKey();
              AbstractIdentifier clonnedAlias =
                  alias.cloneWithDereference(alias.getDereference() + id.getDereference() - i);
              if (result.add(clonnedAlias)) {
                addAliasesFor(clonnedAlias, result);
              }
            }
          }
        }
      }
    }
  }

  RCUState incRCURead() {
    return new RCUState(
        lockState.incRCURead(),
        rcuRelations,
        outdatedRCU,
        localAgain,
        temporaryIds);
  }

  RCUState decRCURead() {
    return new RCUState(
        lockState.decRCURead(),
        rcuRelations,
        outdatedRCU, localAgain, temporaryIds);
  }

  RCUState markRead() {
    return new RCUState(
        lockState.markRead(),
        rcuRelations,
        outdatedRCU,
        localAgain,
        temporaryIds);
  }

  RCUState markWrite() {
    return new RCUState(lockState.markWrite(), rcuRelations,
        outdatedRCU, localAgain, temporaryIds);
  }

  RCUState clearLock() {
    return new RCUState(lockState.clearLock(), rcuRelations,
        outdatedRCU, localAgain, temporaryIds);
  }

  RCUState addTmpMapping(AbstractIdentifier tmp, AbstractIdentifier nonTmp) {
    Map<AbstractIdentifier, AbstractIdentifier> map = new TreeMap<>(temporaryIds);
    map.put(tmp, nonTmp);
    return new RCUState(lockState, rcuRelations,
        outdatedRCU,
        localAgain,
        ImmutableMap.copyOf(map));
  }

  public AbstractIdentifier getNonTemporaryId(AbstractIdentifier pId) {
    return temporaryIds.get(pId);
  }
}
