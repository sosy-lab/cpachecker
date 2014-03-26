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
package org.sosy_lab.cpachecker.cpa.edgeexclusion;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

import com.google.common.collect.ImmutableSet;

/**
 * Instances of this class are precisions for the edge exclusion CPA. They
 * control which edges are excluded.
 *
 * The instances of this class are immutable.
 */
public class EdgeExclusionPrecision implements Precision {

  /**
   * The empty precision does not exclude any edges.
   */
  private static final EdgeExclusionPrecision EMPTY = new EdgeExclusionPrecision(ImmutableSet.<CFAEdge>of());

  /**
   * The excluded edges.
   */
  private final ImmutableSet<CFAEdge> excludedEdges;

  /**
   * Gets the empty precision, which does not exclude any edges.
   *
   * @return the empty precision, which does not exclude any edges.
   */
  public static EdgeExclusionPrecision getEmptyPrecision() {
    return EMPTY;
  }

  /**
   * Creates a new edge exclusion precision for excluding the given edges.
   *
   * @param pExcludedEdges the edges to exclude.
   */
  private EdgeExclusionPrecision(ImmutableSet<CFAEdge> pExcludedEdges) {
    this.excludedEdges = pExcludedEdges;
  }

  /**
   * Returns a precision that excludes all edges excluded by this precision
   * plus all given edges.
   *
   * @param pAdditionalExcludedEdges the additional edges to exclude.
   *
   * @return a precision that excludes all edges excluded by this precision
   * plus all given edges.
   */
  public EdgeExclusionPrecision excludeMoreEdges(Collection<CFAEdge> pAdditionalExcludedEdges) {
    if (excludedEdges.containsAll(pAdditionalExcludedEdges)) {
      return this;
    }
    ImmutableSet.Builder<CFAEdge> setBuilder = ImmutableSet.<CFAEdge>builder();
    setBuilder.addAll(excludedEdges);
    Queue<CFAEdge> waitlist = new ArrayDeque<>(pAdditionalExcludedEdges);
    while (!waitlist.isEmpty()) {
      CFAEdge current = waitlist.poll();
      if (current instanceof MultiEdge) {
        for (CFAEdge edge : (MultiEdge) current) {
          waitlist.offer(edge);
        }
      }
      setBuilder.add(current);
    }
    return new EdgeExclusionPrecision(setBuilder.build());
  }

  /**
   * Checks if the given edge is excluded.
   *
   * @param pEdge the edge to check.
   *
   * @return {@code true} if the edge is excluded, {@code false} otherwise.
   */
  public boolean isExcluded(CFAEdge pEdge) {
    if (excludedEdges.contains(pEdge)) {
      return true;
    }
    if (pEdge instanceof MultiEdge) {
      for (CFAEdge edge : (MultiEdge) pEdge) {
        if (isExcluded(edge)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return excludedEdges.isEmpty() ? "no precision" : excludedEdges.toString();
  }

}
