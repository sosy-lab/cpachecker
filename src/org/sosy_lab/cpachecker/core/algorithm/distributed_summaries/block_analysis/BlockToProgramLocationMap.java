// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BlockToProgramLocationMap {

  private final DistributedConfigurableProgramAnalysis dcpa;

  private final Set<String> unreachablePredecessors;

  private final ImmutableMap<String, Multimap<Integer, StateAndPrecision>> entriesPerKey;

  BlockToProgramLocationMap(
      DistributedConfigurableProgramAnalysis pDcpa, Set<String> pPotentialKeys) {
    dcpa = pDcpa;
    ImmutableMap.Builder<String, Multimap<Integer, StateAndPrecision>> entryBuilder =
        ImmutableMap.builder();
    pPotentialKeys.forEach(k -> entryBuilder.put(k, ArrayListMultimap.create()));
    entriesPerKey = entryBuilder.buildOrThrow();
    unreachablePredecessors = new LinkedHashSet<>();
  }

  public void markUnreachable(String pKey) {
    assert entriesPerKey.containsKey(pKey);
    unreachablePredecessors.add(pKey);
  }

  public void markReachable(String pKey) {
    assert entriesPerKey.containsKey(pKey);
    unreachablePredecessors.remove(pKey);
  }

  public boolean isUnreachable() {
    return unreachablePredecessors.equals(entriesPerKey.keySet());
  }

  public boolean isAnyPredecessorTrulyEmpty() {
    return entriesPerKey.keySet().stream()
        .anyMatch(p -> isEmpty(p) && !unreachablePredecessors.contains(p));
  }

  public boolean isEmpty() {
    return entriesPerKey.keySet().stream().allMatch(this::isEmpty);
  }

  public boolean isEmpty(String pKey) {
    return entriesPerKey.get(pKey).isEmpty();
  }

  public Collection<StateAndPrecision> getStatesAndPrecisionsForKey(String pKey) {
    return entriesPerKey.get(pKey).values();
  }

  public Collection<AbstractState> getStatesForKey(String pKey) {
    return transformedImmutableListCopy(entriesPerKey.get(pKey).values(), StateAndPrecision::state);
  }

  public ImmutableList<StateAndPrecision> getStatesAndPrecisions() {
    return FluentIterable.from(entriesPerKey.keySet())
        .transformAndConcat(this::getStatesAndPrecisionsForKey)
        .toList();
  }

  public Collection<String> getKeys() {
    return entriesPerKey.keySet();
  }

  public Collection<AbstractState> getStates() {
    return transformedImmutableListCopy(getStatesAndPrecisions(), sap -> sap.state());
  }

  public void clearKey(String pKey) {
    entriesPerKey.get(pKey).clear();
  }

  public void addStateForKey(String pKey, StateAndPrecision stateAndPrecision) {
    entriesPerKey
        .get(pKey)
        .put(dcpa.computeProgramPointHash(stateAndPrecision.state()), stateAndPrecision);
  }

  public Collection<StateAndPrecision> getStateForKeyAndId(String pKey, int pHash) {
    return entriesPerKey.get(pKey).get(pHash);
  }

  public Collection<Integer> getIdsForKey(String pKey) {
    return entriesPerKey.get(pKey).keySet();
  }

  public void overwriteStatesForKey(
      String pKey, int pHash, Collection<StateAndPrecision> pStateAndPrecisions) {
    overwriteStatesForKey(
        pKey,
        ImmutableListMultimap.<Integer, StateAndPrecision>builder()
            .putAll(pHash, pStateAndPrecisions)
            .build());
  }

  public void overwriteStatesForKey(
      String pKey, Collection<StateAndPrecision> pStateAndPrecisions) {
    overwriteStatesForKey(
        pKey,
        Multimaps.index(pStateAndPrecisions, sap -> dcpa.computeProgramPointHash(sap.state())));
  }

  public void removeStatesWithIgnoreCallstackIfMorePrecise(
      String pKey, ImmutableList<@NonNull StateAndPrecision> pReceived) {
    if (!pReceived.isEmpty()
        && AbstractStates.extractStateByType(pReceived.getFirst().state(), CallstackState.class)
            == null) {
      // if callstack is not configured, ignore this function
      return;
    }
    if (pReceived.stream()
        .anyMatch(
            sap ->
                DistributedCallstackCPA.allowsAllTransfers(
                    AbstractStates.extractStateByType(sap.state(), CallstackState.class)))) {
      // if the old and the new have ignored callstack,
      // keep everything as is for the coverage check.
      return;
    }
    ImmutableSet.Builder<Integer> toRemove = ImmutableSet.builder();
    for (Entry<Integer, StateAndPrecision> entry : entriesPerKey.get(pKey).entries()) {
      if (DistributedCallstackCPA.allowsAllTransfers(
          AbstractStates.extractStateByType(entry.getValue().state(), CallstackState.class))) {
        toRemove.add(entry.getKey());
      }
    }
    for (Integer i : toRemove.build()) {
      entriesPerKey.get(pKey).removeAll(i);
    }
  }

  public void overwriteStatesForKey(String pKey, Multimap<Integer, StateAndPrecision> pStates) {
    Multimap<Integer, StateAndPrecision> idToStates = entriesPerKey.get(pKey);
    for (Integer id : pStates.keySet()) {
      idToStates.removeAll(id);
      idToStates.putAll(id, pStates.get(id));
    }
  }

  public Multimap<String, StateAndPrecision> asMultimapByKey() {
    ImmutableListMultimap.Builder<String, StateAndPrecision> statesByKey =
        ImmutableListMultimap.builder();
    for (Entry<String, Multimap<Integer, StateAndPrecision>> entry : entriesPerKey.entrySet()) {
      statesByKey.putAll(entry.getKey(), entry.getValue().values());
    }
    return statesByKey.build();
  }
}
