// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.path;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

/**
 * A list of segments, each containing possibly multiple Paths for reaching the next segment.
 *
 * <p>A segment contains multiple paths, which are represented as a list of edges where a decision
 * is made
 */
public class SegmentedPaths {

  // We store strings so that deserialize does not require a CFA to work
  final ImmutableList<ImmutableSet<ImmutableList<String>>> paths;

  private SegmentedPaths(ImmutableList<ImmutableSet<ImmutableList<String>>> pPaths) {
    paths = pPaths;
  }

  public static final SegmentedPaths EMPTY = new SegmentedPaths(ImmutableList.of());

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return obj instanceof SegmentedPaths other && Objects.equals(paths, other.paths);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(paths);
  }

  @Override
  public String toString() {
    return paths.toString();
  }

  /**
   * Adds a new segment at the front which contains only the given path. Only the edges of the path
   * where another edge could be taken are stored.
   *
   * @param pImmutableList the new path
   * @return a new {@link SegmentedPaths} with these edges at the front
   */
  public SegmentedPaths addEdgesToFront(ImmutableList<CFAEdge> pImmutableList) {
    ImmutableList<String> newPath =
        FluentIterable.from(pImmutableList)
            .filter(SegmentedPaths::isDecisionEdge)
            .transform(SegmentedPaths::edgeToString)
            .toList();

    return new SegmentedPaths(Collections3.elementAndList(ImmutableSet.of(newPath), paths));
  }

  static String edgeToString(CFAEdge edge) {
    return "N" + edge.getPredecessor().getNodeNumber() + "N" + edge.getSuccessor().getNodeNumber();
  }

  static boolean isDecisionEdge(CFAEdge edge) {
    return relevantEdge(edge)
        && edge.getPredecessor().getAllLeavingEdges().filter(SegmentedPaths::relevantEdge).size()
            > 1;
  }

  private static boolean relevantEdge(CFAEdge e) {
    return !(e instanceof BlankEdge || e instanceof FunctionSummaryEdge);
  }

  /**
   * Merges the first segments of the given collections. Assumes that all other segments are the
   * same.
   *
   * @param toMerge which paths to merge
   * @return the merged {@link SegmentedPaths}
   */
  public static SegmentedPaths merge(Collection<SegmentedPaths> toMerge) {
    Preconditions.checkNotNull(toMerge);
    Preconditions.checkArgument(!toMerge.isEmpty());

    SegmentedPaths first = toMerge.iterator().next();

    if (toMerge.size() == 1) {
      return first;
    }

    ImmutableSet<ImmutableList<String>> allFirstSegmentsMerged =
        FluentIterable.from(toMerge).transformAndConcat(v -> v.paths.getFirst()).toSet();

    return new SegmentedPaths(
        Collections3.elementAndList(
            allFirstSegmentsMerged, first.paths.subList(1, first.paths.size())));
  }

  /**
   * Returns a new {@link SegmentedPaths} where every edge that is a key in {@code oldToNew} is
   * replaced by its corresponding value. Edges that do not appear as a key in {@code oldToNew} are
   * left unchanged.
   *
   * @param oldToNew maps old edges to the new edges that should replace them
   * @return a new {@link SegmentedPaths} with the transformed edges
   */
  public SegmentedPaths transformEdges(Map<CFAEdge, CFAEdge> oldToNew) {
    Map<String, String> oldToNewStrings = new HashMap<>(oldToNew.size());
    for (Map.Entry<CFAEdge, CFAEdge> entry : oldToNew.entrySet()) {
      oldToNewStrings.put(edgeToString(entry.getKey()), edgeToString(entry.getValue()));
    }

    ImmutableList.Builder<ImmutableSet<ImmutableList<String>>> newWitness =
        ImmutableList.builderWithExpectedSize(paths.size());
    for (ImmutableSet<ImmutableList<String>> segment : paths) {
      ImmutableSet.Builder<ImmutableList<String>> newSegment =
          ImmutableSet.builderWithExpectedSize(segment.size());
      for (ImmutableList<String> path : segment) {
        newSegment.add(
            transformedImmutableListCopy(
                path, edgeString -> oldToNewStrings.getOrDefault(edgeString, edgeString)));
      }
      newWitness.add(newSegment.build());
    }
    return new SegmentedPaths(newWitness.build());
  }

  private static final Joiner JOIN_LIST = Joiner.on(','); // join elements in inner list
  private static final Joiner JOIN_SET =
      Joiner.on('|'); // join inner-list tokens inside a set (sorted)
  private static final Joiner JOIN_TOP = Joiner.on(';'); // join set tokens for the top-level list

  private static final Splitter SPLIT_TOP = Splitter.on(';');
  private static final Splitter SPLIT_SET = Splitter.on('|');
  private static final Splitter SPLIT_LIST = Splitter.on(',');

  public String serialize() {
    List<String> setTokens = new ArrayList<>(paths.size());
    for (ImmutableSet<ImmutableList<String>> set : paths) {
      if (set.isEmpty()) {
        setTokens.add(""); // represent empty set as empty token
        continue;
      }
      List<String> innerTokens = new ArrayList<>(set.size());
      for (ImmutableList<String> innerList : set) {
        if (innerList.isEmpty()) {
          innerTokens.add(""); // empty inner list -> empty token
          continue;
        }
        innerTokens.add(JOIN_LIST.join(innerList));
      }
      Collections.sort(innerTokens); // stable order for set based on content
      setTokens.add(JOIN_SET.join(innerTokens));
    }
    return JOIN_TOP.join(setTokens);
  }

  public static SegmentedPaths deserialize(String input) {
    if (isNullOrEmpty(input)) {
      return new SegmentedPaths(ImmutableList.of());
    }
    List<ImmutableSet<ImmutableList<String>>> top = new ArrayList<>();
    for (String setToken : SPLIT_TOP.splitToList(input)) {
      if (setToken.isEmpty()) {
        top.add(ImmutableSet.of());
        continue;
      }
      List<ImmutableList<String>> innerLists = new ArrayList<>();
      for (String innerToken : SPLIT_SET.splitToList(setToken)) {
        if (innerToken.isEmpty()) {
          innerLists.add(ImmutableList.of());
          continue;
        }
        innerLists.add(ImmutableList.copyOf(SPLIT_LIST.splitToList(innerToken)));
      }
      top.add(ImmutableSet.copyOf(innerLists));
    }
    return new SegmentedPaths(ImmutableList.copyOf(top));
  }
}
