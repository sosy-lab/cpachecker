// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SMGUseGraph<V extends SMGUseVertice, E extends SMGUseGraphEdge<V>> {

  private final Multimap<V, E> graph = HashMultimap.create();

  public void addEdge(E edge) {
    graph.put(edge.getSource(), edge);
  }

  @Override
  public String toString() {
    return graph.toString();
  }

  public Map<V, SMGUseRange> getAllTargetsAndUseRangeOfSources(Set<V> pSources, int curPos) {

    Map<V, SMGUseRange> result = new HashMap<>();
    Set<V> waitlist = new HashSet<>();
    Set<V> addToWaitlist = new HashSet<>();

    for (V source : pSources) {
      int srcPos = source.getPosition();

      assert srcPos <= curPos;

      result.put(source, new SMGUseRange(srcPos, curPos));
      waitlist.add(source);
    }

    while (!waitlist.isEmpty()) {
      for (V source : waitlist) {
        // no target resolves to empty collection
        for (SMGUseGraphEdge<V> edge : graph.get(source)) {
          V target = edge.getTarget();

          if (!result.containsKey(target)) {
            result.put(target, new SMGUseRange(target.getPosition(), source.getPosition()));
            addToWaitlist.add(target);
          } else {
            SMGUseRange range = result.get(target);
            if (range.getPosUse() < source.getPosition()) {
              result.put(target, new SMGUseRange(target.getPosition(), source.getPosition()));
            }
          }
        }
      }

      waitlist.clear();
      waitlist.addAll(addToWaitlist);
      addToWaitlist.clear();
    }

    return result;
  }

  public static class SMGUseRange {

    private final int posStart;
    private final int posUse;

    public SMGUseRange(int pStart, int pUse) {
      posStart = pStart;
      posUse = pUse;
    }

    public int getPosUse() {
      return posUse;
    }

    public int getPosStart() {
      return posStart;
    }

    @Override
    public String toString() {
      return "SMGUseRange [posStart=" + posStart + ", posUse=" + posUse + "]";
    }
  }
}
