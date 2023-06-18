// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.rangedconditions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CFAPath implements Comparable<CFAPath> {

  public static final CFAPath TOP = new CFATopPath();
  private static final long serialVersionUID = -7969725228233578981L;
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
    List<String> nodeNumbers = ImmutableList.copyOf(pPathString.split("\\s+"));
    return fromInts(
        pCFA, nodeNumbers.stream().map(Integer::valueOf).collect(ImmutableList.toImmutableList()));
  }

  public static CFAPath fromInts(CFA pCfa, List<Integer> ints) {
    Builder path = new Builder();
    for (Integer i : ints) {
      path.add(
          pCfa.getAllNodes().stream().filter(elem -> elem.getNodeNumber() == i).findFirst().get());
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
    if (other instanceof CFATopPath) {
      return -1;
    }
    for (int i = 0; i < size() && i < other.size(); i++) {
      if (!get(i).equals(other.get(i))) {
        return get(i).compareTo(other.get(i));
      }
    }
    return size() - other.size();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof CFAPath)) {
      return false;
    }

    CFAPath path = (CFAPath) other;
    return nodes.equals(path.nodes);
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
    private static final long serialVersionUID = -7512612535752666973L;

    @Override
    public int compareTo(CFAPath other) {
      if (other instanceof CFATopPath) {
        return 0;
      }
      return 1;
    }
  }
}
