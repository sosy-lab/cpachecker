// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.path;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

/** A list of segments, each containing possibly multiple Paths for reaching the next segment */
public record ViolationWitness(ImmutableList<ImmutableSet<ImmutableList<String>>> witness) {

  public static final ViolationWitness EMPTY = new ViolationWitness(ImmutableList.of());

  public ViolationWitness addEdges(ImmutableList<CFAEdge> pImmutableList) {
    ImmutableList<String> newPath =
        FluentIterable.from(pImmutableList)
            .filter(ViolationWitness::isDecisionEdge)
            .transform(ViolationWitness::edgeToString)
            .toList();

    return new ViolationWitness(Collections3.elementAndList(ImmutableSet.of(
        newPath), witness));
  }

  public static String edgeToString(CFAEdge edge) {
    return "N" + edge.getPredecessor().getNodeNumber() + "N" + edge.getSuccessor().getNodeNumber();
  }

  public static boolean isDecisionEdge(CFAEdge edge) {
    return relevantEdge(edge)
        && edge.getPredecessor().getAllLeavingEdges().filter(ViolationWitness::relevantEdge).size()
            > 1;
  }

  private static boolean relevantEdge(CFAEdge e) {
    return !(e instanceof BlankEdge || e instanceof FunctionSummaryEdge);
  }

  public static ViolationWitness merge(Collection<ViolationWitness> toMerge) {
    Preconditions.checkNotNull(toMerge);
    Preconditions.checkArgument(!toMerge.isEmpty());

    ViolationWitness first = toMerge.iterator().next();

    if (toMerge.size() == 1) {
      return first;
    }

    ImmutableSet<ImmutableList<String>> allFirstSegmentsMerged =
        FluentIterable.from(toMerge).transformAndConcat(v -> v.witness.getFirst()).toSet();

    return new ViolationWitness(
        Collections3.elementAndList(
            allFirstSegmentsMerged, first.witness.subList(1, first.witness.size())));
  }

  public ViolationWitness transformEdges(Function<String, String> f) {
    ImmutableList.Builder<ImmutableSet<ImmutableList<String>>> newWitness =
        ImmutableList.builderWithExpectedSize(witness.size());
    for (ImmutableSet<ImmutableList<String>> segment : witness) {
      ImmutableSet.Builder<ImmutableList<String>> newSegment =
          ImmutableSet.builderWithExpectedSize(segment.size());
      for (ImmutableList<String> path : segment) {
        newSegment.add(FluentIterable.from(path).transform(f::apply).toList());
      }
      newWitness.add(newSegment.build());
    }
    return new ViolationWitness(newWitness.build());
  }

  private static final Joiner JOIN_LIST = Joiner.on(','); // join elements in inner list
  private static final Joiner JOIN_SET =
      Joiner.on('|'); // join inner-list tokens inside a set (sorted)
  private static final Joiner JOIN_TOP = Joiner.on(';'); // join set tokens for the top-level list

  private static final Splitter SPLIT_TOP = Splitter.on(';');
  private static final Splitter SPLIT_SET = Splitter.on('|');
  private static final Splitter SPLIT_LIST = Splitter.on(',');

  public String serialize() {
    List<String> setTokens = new ArrayList<>(witness.size());
    for (ImmutableSet<ImmutableList<String>> set : witness) {
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

  public static ViolationWitness deserialize(String input) {
    if (isNullOrEmpty(input)) {
      return new ViolationWitness(ImmutableList.of());
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
    return new ViolationWitness(ImmutableList.copyOf(top));
  }
}
