/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.reachedset;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Special implementation of the reached set that partitions the set by keys that
 * depend on the abstract state.
 * Which key is used for an abstract state can be changed by overriding
 * {@link #getPartitionKey(AbstractState)} in a sub-class.
 * By default, this implementation needs abstract states which implement
 * {@link Partitionable} and uses the return value of {@link Partitionable#getPartitionKey()}
 * as the key.
 *
 * Whenever the method {@link PartitionedReachedSet#getReached(AbstractState)}
 * is called (which is usually done by the CPAAlgorithm to get the candidates
 * for merging and coverage checks), it will return a subset of the set of all
 * reached states. This subset contains exactly those states, whose partition
 * key is equal to the key of the state given as a parameter.
 */
public class PartitionedReachedSet extends DefaultReachedSet {

  private final Multimap<Object, AbstractState> partitionedReached = LinkedHashMultimap.create(100, 1);

  public PartitionedReachedSet(WaitlistFactory waitlistFactory) {
    super(waitlistFactory);
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    super.add(pState, pPrecision);

    partitionedReached.put(getPartitionKey(pState), pState);
  }

  public Set<AbstractState> getReached() {
    Set<AbstractState> result = new HashSet<>();

    for (Object key : partitionedReached.keySet()) {
      result.addAll(partitionedReached.get(key));
    }
    return result;
  }

  @Override
  public void remove(AbstractState pState) {
    super.remove(pState);

    partitionedReached.remove(getPartitionKey(pState), pState);
  }

  @Override
  public void clear() {
    super.clear();

    partitionedReached.clear();
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState pState) {
    return getReachedForKey(getPartitionKey(pState));
  }

  public int getNumberOfPartitions() {
    return partitionedReached.keySet().size();
  }

  public Map.Entry<Object, Collection<AbstractState>> getMaxPartition() {
    int max = 0;
    Map.Entry<Object, Collection<AbstractState>> maxPartition = null;

    for (Map.Entry<Object, Collection<AbstractState>> partition : partitionedReached.asMap().entrySet()) {
      int size = partition.getValue().size();
      if (size > max) {
        max = partition.getValue().size();
        maxPartition = partition;
      }
    }
    return maxPartition;
  }

  protected Object getPartitionKey(AbstractState pState) {
    assert pState instanceof Partitionable : "Partitionable states necessary for PartitionedReachedSet";
    return ((Partitionable)pState).getPartitionKey();
  }

  protected Collection<AbstractState> getReachedForKey(Object key) {
    return Collections.unmodifiableCollection(partitionedReached.get(key));
  }

  protected Set<?> getKeySet() {
    return Collections.unmodifiableSet(partitionedReached.keySet());
  }
}
