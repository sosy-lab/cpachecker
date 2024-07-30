// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.hb;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cpa.hb.HappensBeforeUtils.extendMapOfSets;
import static org.sosy_lab.cpachecker.cpa.hb.HappensBeforeUtils.insertIntoMapOfLists;
import static org.sosy_lab.cpachecker.cpa.hb.HappensBeforeUtils.subtractFromMapOfSets;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record ExecutionGraph(
    Map<String, Set<MemoryEvent>> revisitableReads,
    Map<String, List<MemoryEvent>> mo,
    Map<MemoryEvent, MemoryEvent> pendingRf) {

  public static ExecutionGraph empty() {
    return new ExecutionGraph(Map.of(), Map.of(), Map.of());
  }

  /**
   * Adds a new write, by: * extending po of the thread * extending mo in every possible
   * (consistent) way * extending pendingRf for every element of the powerset of revisitable reads
   */
  Collection<ExecutionGraph> addWrite(final MemoryEvent pMemoryEvent) {
    final var ret = ImmutableList.<ExecutionGraph>builder();
    final var localMo = mo.getOrDefault(pMemoryEvent.var().getName(), List.of());
    final var visibleMo = after(localMo, lastSameWrite(pMemoryEvent));
    final var offset = localMo.size() - visibleMo.size();

    for (int i = 0; i <= visibleMo.size(); i++) {
      final var newMo =
          insertIntoMapOfLists(mo, offset + i, pMemoryEvent.var().getName(), pMemoryEvent);
      for (Set<MemoryEvent> memoryEvents :
          powerSet(revisitableReads.getOrDefault(pMemoryEvent.var().getName(), Set.of()))) {
        final var newRevisitableReads =
            subtractFromMapOfSets(revisitableReads, pMemoryEvent.var().getName(), memoryEvents);
        final var newRf = ImmutableMap.<MemoryEvent, MemoryEvent>builder(); // .putAll(pendingRf);
        memoryEvents.forEach(memoryEvent -> newRf.put(memoryEvent, pMemoryEvent));
        ret.add(new ExecutionGraph(newRevisitableReads, newMo, newRf.build()));
      }
    }
    return ret.build();
  }

  /**
   * Adding a new read that may read from any write in mo that: * is, or is mo-after the last write
   * in the same thread
   */
  Collection<ExecutionGraph> addRead(final MemoryEvent pMemoryEvent) {
    final List<ExecutionGraph> ret = new ArrayList<>();

    for (MemoryEvent memoryEvent :
        after(
            mo.getOrDefault(pMemoryEvent.var().getName(), List.of()),
            lastSameWrite(pMemoryEvent))) {
      final var newRf =
          ImmutableMap.<MemoryEvent, MemoryEvent>builder() /*.putAll(pendingRf)*/
              .put(pMemoryEvent, memoryEvent)
              .buildKeepingLast();
      ret.add(new ExecutionGraph(revisitableReads, mo, newRf));
    }
    final var newRevisitableReads =
        extendMapOfSets(revisitableReads, pMemoryEvent.var().getName(), pMemoryEvent);
    ret.add(new ExecutionGraph(newRevisitableReads, mo, pendingRf));
    return ImmutableList.copyOf(ret);
  }

  ExecutionGraph clearPending() {
    return new ExecutionGraph(revisitableReads, mo, Map.of());
  }

  private static <T> List<T> after(List<T> collection, T afterThis) {
    if (afterThis == null) {
      return collection;
    } else {
      final var indexOf = checkNotNull(collection).indexOf(afterThis);
      return collection.subList(indexOf + 1, collection.size());
    }
  }

  private static MemoryEvent lastSameWrite(MemoryEvent memEvent) {
    final var name = memEvent.var().getName();
    do {
      memEvent = memEvent.parent();
    } while (memEvent != null
        && !(memEvent.type() == MemoryEventType.WRITE && memEvent.var().getName().equals(name)));
    return memEvent;
  }

  private static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
    final var sets = ImmutableSet.<Set<T>>builder();
    if (originalSet.isEmpty()) {
      sets.add(ImmutableSet.of());
      return sets.build();
    }
    List<T> list = originalSet.stream().toList();
    T head = list.get(0);
    Set<T> rest = new LinkedHashSet<>(list.subList(1, list.size()));
    for (Set<T> set : powerSet(rest)) {
      Set<T> newSet = new LinkedHashSet<>();
      newSet.add(head);
      newSet.addAll(set);
      sets.add(newSet);
      sets.add(set);
    }
    return sets.build();
  }

  public String toDOTLabel() {
    final var rPrint = ImmutableMap.<String, Set<String>>builder();
    revisitableReads.forEach(
        (pS, pMemoryEvents) ->
            rPrint.put(
                pS,
                pMemoryEvents.stream()
                    .map((MemoryEvent pMemoryEvent) -> pMemoryEvent.var().getQualifiedName())
                    .collect(Collectors.toSet())));
    final var moPrint = ImmutableMap.<String, List<String>>builder();
    mo.forEach(
        (pS, pMemoryEvents) ->
            moPrint.put(
                pS,
                pMemoryEvents.stream()
                    .map((MemoryEvent pMemoryEvent) -> pMemoryEvent.var().getQualifiedName())
                    .toList()));
    final var rfPrint = ImmutableMap.<String, String>builder();
    pendingRf.forEach(
        (e1, e2) -> rfPrint.put(e1.var().getQualifiedName(), e2.var().getQualifiedName()));

    return "R: %s, mo: %s, pending RF: %s"
        .formatted(
            rPrint.buildKeepingLast(), moPrint.buildKeepingLast(), rfPrint.buildKeepingLast());
  }
}
