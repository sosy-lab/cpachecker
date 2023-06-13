// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.util.Pair;

public final class UsagePoint implements Comparable<UsagePoint> {

  private final Access access;
  private final List<CompatibleNode> compatibleNodes;
  private final Set<UsagePoint> coveredUsages;

  public UsagePoint(List<CompatibleNode> nodes, Access pAccess) {
    access = pAccess;
    coveredUsages = new TreeSet<>();
    compatibleNodes = nodes;
  }

  public boolean addCoveredUsage(UsagePoint newChild) {
    if (!coveredUsages.contains(newChild)) {

      Optional<UsagePoint> usage =
          coveredUsages.stream().filter(u -> u.covers(newChild)).findFirst();

      if (usage.isPresent()) {
        assert !usage.orElseThrow().equals(newChild);
        return usage.orElseThrow().addCoveredUsage(newChild);
      }
      return coveredUsages.add(newChild);
    }
    return false;
  }

  public Set<UsagePoint> getCoveredUsages() {
    return coveredUsages;
  }

  @Override
  public int hashCode() {
    return Objects.hash(access, compatibleNodes);
  }

  public Access getAccess() {
    return access;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    UsagePoint other = (UsagePoint) obj;
    return access == other.access && Objects.equals(compatibleNodes, other.compatibleNodes);
  }

  @Override
  public int compareTo(UsagePoint o) {
    // It is very important to compare at first the accesses, because an algorithm base on this
    // suggestion
    int result = access.compareTo(o.access);
    if (result != 0) {
      return result;
    }
    Preconditions.checkArgument(compatibleNodes.size() == o.compatibleNodes.size());
    for (int i = 0; i < compatibleNodes.size(); i++) {
      CompatibleNode currentNode = compatibleNodes.get(i);
      CompatibleNode otherNode = o.compatibleNodes.get(i);
      result = currentNode.compareTo(otherNode);
      if (result != 0) {
        return result;
      }
    }
    return result;
  }

  // TODO CompareTo? with enums
  public boolean covers(UsagePoint o) {
    // access 'write' is higher than 'read', but only for nonempty locksets
    if (access.compareTo(o.access) > 0) {
      return false;
    }

    return from(Pair.zipList(compatibleNodes, o.compatibleNodes))
        .allMatch(p -> p.getFirst().cover(p.getSecond()));
  }

  public boolean isCompatible(UsagePoint other) {
    return from(Pair.zipList(compatibleNodes, other.compatibleNodes))
        .allMatch(p -> p.getFirst().isCompatibleWith(p.getSecond()));
  }

  public boolean isEmpty() {
    return from(compatibleNodes).allMatch(CompatibleNode::hasEmptyLockSet);
  }

  @Override
  public String toString() {
    return access + ":" + compatibleNodes;
  }

  public <T extends CompatibleNode> T get(Class<T> pClass) {
    for (CompatibleNode node : compatibleNodes) {
      if (node.getClass() == pClass) {
        return pClass.cast(node);
      }
    }
    return null;
  }
}
