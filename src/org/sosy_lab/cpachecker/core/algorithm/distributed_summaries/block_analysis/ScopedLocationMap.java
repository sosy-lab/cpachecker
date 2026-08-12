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
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class ScopedLocationMap {

  private final DistributedConfigurableProgramAnalysis dcpa;

  private final ImmutableMap<String, Multimap<Integer, StateAndPrecision>> entriesPerKey;

  ScopedLocationMap(DistributedConfigurableProgramAnalysis pDcpa, Set<String> pPotentialKeys) {
    dcpa = pDcpa;
    ImmutableMap.Builder<String, Multimap<Integer, StateAndPrecision>> entryBuilder =
        ImmutableMap.builder();
    pPotentialKeys.forEach(k -> entryBuilder.put(k, ArrayListMultimap.create()));
    entriesPerKey = entryBuilder.buildOrThrow();
  }

  public boolean isEmpty(String pKey) {
    return entriesPerKey.get(pKey).isEmpty();
  }

  public Collection<StateAndPrecision> getStatesForKey(String pKey) {
    return entriesPerKey.get(pKey).values();
  }

  public ImmutableList<StateAndPrecision> getStatesAndPrecisions() {
    return FluentIterable.from(entriesPerKey.keySet())
        .transformAndConcat(this::getStatesForKey)
        .toList();
  }

  public void resetStates() {
    for (Entry<String, Multimap<Integer, StateAndPrecision>> entry : entriesPerKey.entrySet()) {
      Collection<StateAndPrecision> curr = ImmutableList.copyOf(entry.getValue().values());
      clearKey(entry.getKey());
      for (StateAndPrecision state : curr) {
        entry.getValue().put(dcpa.computeProgramPointHash(state.state()), state);
      }
    }
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
