// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.rangedconditions;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CFAPath extends ArrayList<CFANode> implements Comparable<CFAPath> {

  private static final long serialVersionUID = -7969725228233578981L;

  public static final CFAPath TOP = new CFATopPath();

  public CFAPath() {
    super();
  }

  public CFAPath(Collection<CFANode> pPath) {
    super();
    addAll(pPath);
  }

  public static CFAPath fromString(CFA pCFA, String pPathString) {
    List<String> nodeNumbers = ImmutableList.copyOf(pPathString.split("\\s+"));
    return fromInts(
        pCFA, nodeNumbers.stream().map(Integer::valueOf).collect(ImmutableList.toImmutableList()));
  }

  public static CFAPath fromInts(CFA pCfa, List<Integer> ints) {
    CFAPath result = new CFAPath();
    for (Integer i : ints) {
      result.add(
          pCfa.getAllNodes().stream().filter(elem -> elem.getNodeNumber() == i).findFirst().get());
    }
    return result;
  }

  @Override
  public String toString() {
    return stream().map(elem -> elem.toString()).collect(Collectors.joining());
  }

  public Set<CFAPath> getPrefixes() {
    Set<CFAPath> resultSet = new HashSet<>();

    for (int i = 1; i <= size(); i++) {
      resultSet.add(new CFAPath(subList(0, i)));
    }

    return resultSet;
  }

  public CFANode getLast() {
    return get(size() - 1);
  }

  @Override
  public int compareTo(CFAPath other) {
    for (int i = 0; i < size() && i < other.size(); i++) {
      if (!get(i).equals(other.get(i))) {
        return get(i).compareTo(other.get(i));
      }
    }
    return size() - other.size();
  }

  public CFAPath copy() {
    return new CFAPath(this);
  }

  private static class CFATopPath extends CFAPath {
    private static final long serialVersionUID = -7512612535752666973L;
  }
}
