/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SMGFlowDependenceGraph<V extends SMGFlowDependenceVertice, E extends SMGFlowDependenceEdge<V>> {

  private final Multimap<V, E> graph;

  public SMGFlowDependenceGraph(Set<E> pGraphEdges) {

    Multimap<V, E> result = HashMultimap.create();

    pGraphEdges.forEach((E pEdge) -> {
      result.put(pEdge.getTarget(), pEdge);
    });

    graph = ImmutableSetMultimap.copyOf(result);
  }

  @Override
  public String toString() {
    return graph.toString();
  }

  public Map<V, SMGUseRange> getAllTargetsAndUseRangeOfSources(Set<V> pTargets, int curPos) {

    Map<V, SMGUseRange> result = new HashMap<>();
    Set<V> waitlist = new HashSet<>();
    Set<V> addToWaitlist = new HashSet<>();

    for (V target : pTargets) {
      int targetPos = target.getPosition();

      assert targetPos <= curPos;

      result.put(target, new SMGUseRange(targetPos, curPos));
      waitlist.add(target);
    }

    while (!waitlist.isEmpty()) {
      for (V target : waitlist) {
        // no target resolves to empty collection
        for (SMGFlowDependenceEdge<V> edge : graph.get(target)) {
          V source = edge.getSource();

          if (!result.containsKey(source)) {
            result.put(source, new SMGUseRange(source.getPosition(), target.getPosition() - 1));
            addToWaitlist.add(source);
          } else {
            SMGUseRange range = result.get(source);
            if (range.getPosUse() < target.getPosition()) {
              result.put(source, new SMGUseRange(source.getPosition(), target.getPosition() - 1));
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