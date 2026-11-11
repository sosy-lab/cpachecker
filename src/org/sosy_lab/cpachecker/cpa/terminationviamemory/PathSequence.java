// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * An immutable sequence of CFA nodes that shares its prefix with the sequence it was extended from,
 * so that extending it takes constant time and memory.
 */
final class PathSequence {

  private static final PathSequence EMPTY = new PathSequence(null, null);

  private final @Nullable PathSequence prefix;
  private final @Nullable CFANode last;
  private final int size;
  private final int hashCode;

  private PathSequence(@Nullable PathSequence pPrefix, @Nullable CFANode pLast) {
    prefix = pPrefix;
    last = pLast;
    size = pPrefix == null ? 0 : pPrefix.size + 1;
    hashCode = pPrefix == null ? 1 : 31 * pPrefix.hashCode + pLast.hashCode();
  }

  static PathSequence empty() {
    return EMPTY;
  }

  PathSequence append(CFANode pNode) {
    return new PathSequence(this, pNode);
  }

  int size() {
    return size;
  }

  CFANode getLast() {
    checkState(size > 0);
    return last;
  }

  /** Returns the sequence of the first given number of nodes. */
  PathSequence prefix(int pLength) {
    checkArgument(0 <= pLength && pLength <= size);
    PathSequence result = this;
    while (result.size > pLength) {
      result = result.prefix;
    }
    return result;
  }

  /** Returns the nodes from the first index (inclusive) to the second one (exclusive). */
  ImmutableList<CFANode> subList(int pFrom, int pTo) {
    checkArgument(0 <= pFrom && pFrom <= pTo);
    List<CFANode> reversed = new ArrayList<>(pTo - pFrom);
    for (PathSequence s = prefix(pTo); s.size > pFrom; s = s.prefix) {
      reversed.add(s.last);
    }
    return ImmutableList.copyOf(reversed).reverse();
  }

  @Override
  public boolean equals(Object pOther) {
    if (!(pOther instanceof PathSequence other)
        || size != other.size
        || hashCode != other.hashCode) {
      return false;
    }
    PathSequence s = this;
    while (s != other) {
      if (!s.last.equals(other.last)) {
        return false;
      }
      s = s.prefix;
      other = other.prefix;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }
}
