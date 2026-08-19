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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * The states a block currently knows for each neighboring block, grouped by program point.
 *
 * <p>The outer key is the id of the neighboring block that sent the states; the inner key is the
 * program-point hash of a state, so that an update can replace the states of one program point
 * without discarding what is known about the others. The set of outer keys is fixed at construction
 * time: it is exactly the set of blocks that may ever send something.
 *
 * <p>Independently of the stored states, each key can be marked as reachable or unreachable, which
 * records whether that neighbor reported its block end to be unreachable. A key with no states is
 * therefore not the same as an unreachable one.
 *
 * <p>Each key also remembers which blocks the neighbor named as keeping its own report incomplete,
 * see {@link #getIncompleteSources()}.
 */
public class BlockToProgramLocationMap {

  private final DistributedConfigurableProgramAnalysis dcpa;

  private final Set<String> unreachablePredecessors;

  private final Map<String, ImmutableSet<String>> incompleteSourcesPerKey;

  private final ImmutableMap<String, Multimap<Integer, StateAndPrecision>> entriesPerKey;

  BlockToProgramLocationMap(
      DistributedConfigurableProgramAnalysis pDcpa, Set<String> pPotentialKeys) {
    dcpa = pDcpa;
    ImmutableMap.Builder<String, Multimap<Integer, StateAndPrecision>> entryBuilder =
        ImmutableMap.builder();
    pPotentialKeys.forEach(k -> entryBuilder.put(k, ArrayListMultimap.create()));
    entriesPerKey = entryBuilder.buildOrThrow();
    unreachablePredecessors = new LinkedHashSet<>();
    incompleteSourcesPerKey = new LinkedHashMap<>();
  }

  public void markUnreachable(String pKey) {
    assert entriesPerKey.containsKey(pKey);
    unreachablePredecessors.add(pKey);
  }

  /**
   * Records which blocks the given neighbor named as keeping its last report incomplete. Only the
   * latest message counts: an incompleteness that the neighbor has meanwhile resolved must not keep
   * haunting this block.
   */
  public void setIncompleteSources(String pKey, Set<String> pSources) {
    assert entriesPerKey.containsKey(pKey);
    incompleteSourcesPerKey.put(pKey, ImmutableSet.copyOf(pSources));
  }

  public void markReachable(String pKey) {
    assert entriesPerKey.containsKey(pKey);
    unreachablePredecessors.remove(pKey);
  }

  /** Whether the given neighbor reported its block end to be unreachable. */
  public boolean isMarkedUnreachable(String pKey) {
    return unreachablePredecessors.contains(pKey);
  }

  public boolean isUnreachable() {
    return unreachablePredecessors.equals(entriesPerKey.keySet());
  }

  /**
   * The blocks whose paths are missing from what is stored here: the neighbors that have not
   * reported at all, plus the blocks that the neighbors who did report named in turn.
   *
   * <p>Incompleteness has to be transitive. A block that publishes an under-approximated summary
   * hands its successor a picture that looks complete, and the successor would use it to refute
   * violation conditions that the missing paths can still reach; so every block downstream of a
   * silent one has to keep exploring speculatively from the most general state.
   *
   * <p>Naming the blocks rather than passing a flag is what makes this work in a cyclic block
   * graph. A neighbor inside the cycle reports the very block that is waiting for it, so a plain
   * flag would circle forever and no loop would ever settle. Dropping the neighbors this block has
   * itself heard from ends that: they are demonstrably no longer silent.
   */
  public ImmutableSet<String> getIncompleteSources() {
    Set<String> sources = new LinkedHashSet<>();
    for (String key : entriesPerKey.keySet()) {
      if (hasReported(key)) {
        sources.addAll(incompleteSourcesPerKey.getOrDefault(key, ImmutableSet.of()));
      } else {
        sources.add(key);
      }
    }
    entriesPerKey.keySet().stream().filter(this::hasReported).forEach(sources::remove);
    return ImmutableSet.copyOf(sources);
  }

  /** Whether the given neighbor has said anything at all, be it states or an unreachable end. */
  private boolean hasReported(String pKey) {
    return !isEmpty(pKey) || unreachablePredecessors.contains(pKey);
  }

  /** Whether anything that may enter this block is missing from the stored states. */
  public boolean isAnyPredecessorIncomplete() {
    return !getIncompleteSources().isEmpty() || isAnyPredecessorTrulyEmpty();
  }

  public boolean isAnyPredecessorTrulyEmpty() {
    return entriesPerKey.keySet().stream()
        .anyMatch(
            p ->
                (isEmpty(p) && !unreachablePredecessors.contains(p))
                    || getStatesForKey(p).stream()
                        .allMatch(
                            s ->
                                DistributedCallstackCPA.allowsAllTransfers(
                                    AbstractStates.extractStateByType(s, CallstackState.class))));
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

  public Collection<AbstractState> getStates() {
    return transformedImmutableListCopy(getStatesAndPrecisions(), StateAndPrecision::state);
  }

  public void clearKey(String pKey) {
    entriesPerKey.get(pKey).clear();
  }

  public void addStateForKey(String pKey, StateAndPrecision stateAndPrecision) {
    entriesPerKey
        .get(pKey)
        .put(dcpa.computeProgramPointHash(stateAndPrecision.state()), stateAndPrecision);
  }

  public Collection<StateAndPrecision> getStatesAndPrecisionsForKeyAndId(String pKey, int pHash) {
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

  public void overwriteStatesForKey(String pKey, Multimap<Integer, StateAndPrecision> pStates) {
    Multimap<Integer, StateAndPrecision> idToStates = entriesPerKey.get(pKey);
    for (Integer id : pStates.keySet()) {
      idToStates.removeAll(id);
      idToStates.putAll(id, pStates.get(id));
    }
  }

  public void overwriteStatesForKey(
      String pKey, Collection<StateAndPrecision> pStateAndPrecisions) {
    overwriteStatesForKey(
        pKey,
        Multimaps.index(pStateAndPrecisions, sap -> dcpa.computeProgramPointHash(sap.state())));
  }

  public Set<StateAndPrecision> getStatesAndPrecisionsPerLocation(int location) {
    return FluentIterable.from(entriesPerKey.values())
        .transformAndConcat(m -> m.get(location))
        .toSet();
  }

  public List<AbstractState> getStatesPerLocation(int location) {
    return transformedImmutableListCopy(
        getStatesAndPrecisionsPerLocation(location), StateAndPrecision::state);
  }

  public Set<Integer> getAllLocationHashes() {
    return FluentIterable.from(entriesPerKey.values()).transformAndConcat(Multimap::keySet).toSet();
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

  /**
   * A flat view keyed by sending block, dropping the program-point grouping. Used for debug output,
   * see {@code DssDebugUtils#prettyPrintPredicateAnalysisBlock}.
   */
  public Multimap<String, StateAndPrecision> asMultimapByKey() {
    ImmutableListMultimap.Builder<String, StateAndPrecision> statesByKey =
        ImmutableListMultimap.builder();
    for (Entry<String, Multimap<Integer, StateAndPrecision>> entry : entriesPerKey.entrySet()) {
      statesByKey.putAll(entry.getKey(), entry.getValue().values());
    }
    return statesByKey.build();
  }
}
