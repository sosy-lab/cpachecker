// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.rangedconditions;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CFAPath implements Comparable<CFAPath> {

  public static final CFAPath TOP = new CFATopPath();
  private final ImmutableList<CFANode> nodes;

  public CFAPath() {
    nodes = ImmutableList.of();
  }

  public CFAPath(Collection<CFANode> pPath) {
    nodes = ImmutableList.copyOf(pPath);
  }

  public static CFAPath append(CFAPath pPath, CFANode pNode) {
    Builder builder = new Builder();
    builder.addAll(pPath.nodes);
    builder.add(pNode);
    return builder.build();
  }

  public static CFAPath fromString(CFA pCFA, String pPathString) {
    List<String> nodeNumbers = ImmutableList.copyOf(Splitter.on("\\s+").split(pPathString));
    return fromInts(
        pCFA, nodeNumbers.stream().map(Integer::valueOf).collect(ImmutableList.toImmutableList()));
  }

  public static CFAPath fromInts(CFA pCfa, List<Integer> ints) {
    Builder path = new Builder();
    for (Integer i : ints) {
      path.add(
          pCfa.nodes().stream()
              .filter(elem -> elem.getNodeNumber() == i)
              .findFirst()
              .orElseThrow());
    }
    return path.build();
  }

  @Override
  public String toString() {
    return nodes.stream().map(elem -> elem.toString()).collect(Collectors.joining());
  }

  public Set<CFAPath> getPrefixes() {
    ImmutableSet.Builder<CFAPath> resultSetBuilder = new ImmutableSet.Builder<>();
    for (int i = 1; i <= size(); i++) {
      resultSetBuilder.add(new CFAPath(nodes.subList(0, i)));
    }
    return resultSetBuilder.build();
  }

  public int size() {
    return nodes.size();
  }

  public CFANode get(int i) {
    return nodes.get(i);
  }

  public CFANode getLast() {
    return get(size() - 1);
  }

  @Override
  public int compareTo(CFAPath other) {
    // implement path ordering

    // this not instanceof CFATopPath because subclass overwrites compareTo
    // hence, CFATopPath always larger than this
    if (other instanceof CFATopPath) {
      return -1;
    }

    // compare path until shorter path ends, or path deviates
    for (int i = 0; i < size() && i < other.size(); i++) {
      if (!get(i).equals(other.get(i))) {
        // paths deviate, path with smaller node number is smaller
        return get(i).compareTo(other.get(i));
      }
    }

    // shorter path is a prefix of longer path
    // thus, shorter path is smaller
    return Integer.compare(size(), other.size());
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }

    return other instanceof CFAPath && nodes.equals(((CFAPath) other).nodes);
  }

  @Override
  public int hashCode() {
    return nodes.hashCode();
  }

  public Collection<CFANode> subList(int start, int end) {
    return nodes.subList(start, end);
  }

  public Builder asBuilder() {
    return new Builder().addAll(this.nodes);
  }

  public static class Builder {
    private final ImmutableList.Builder<CFANode> builder = new ImmutableList.Builder<>();

    public Builder add(CFANode element) {
      builder.add(element);
      return this;
    }

    public Builder addAll(Iterable<? extends CFANode> elements) {
      builder.addAll(elements);
      return this;
    }

    public CFAPath build() {
      return new CFAPath(builder.build());
    }
  }

  private static class CFATopPath extends CFAPath {

    @Override
    public int compareTo(CFAPath other) {
      // according to ordering no CFAPath can be larger than top path
      // two top paths are always equal
      if (other instanceof CFATopPath) {
        return 0;
      }
      return 1;
    }
  }
}
