// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;

public record BlockGraphPath(ImmutableList<String> path) {

  public enum PathCase {
    SUFFIX_OR_EQUAL,
    OVERLAP,
    REVERSE_OVERLAP,
    REAL_PREFIX,
    OTHER
  }

  public static BlockGraphPath of(String... pathParts) {
    return new BlockGraphPath(ImmutableList.copyOf(pathParts));
  }

  public static BlockGraphPath of(Collection<String> pathParts) {
    return new BlockGraphPath(ImmutableList.copyOf(pathParts));
  }

  public boolean isSuffixOf(BlockGraphPath other) {
    return BlockGraphPath.of(this.path().reverse())
        .isPrefixOf(BlockGraphPath.of(other.path().reverse()));
  }

  public boolean isPrefixOf(BlockGraphPath other) {
    if (other.path().size() < path.size()) {
      return false;
    }

    for (int i = 0; i < path.size(); i++) {
      if (!other.path().get(i).equals(path.get(i))) {
        return false;
      }
    }

    return true;
  }

  public boolean overlapsWith(BlockGraphPath other) {
    List<String> newPath = path;
    List<String> existingPath = other.path();
    int n = newPath.size();
    int m = existingPath.size();
    if (m == 0) {
      return true;
    }
    if (n == 0) {
      return false;
    }

    int[] fail = new int[m];
    for (int i = 1, j = 0; i < m; i++) {
      while (j > 0 && !existingPath.get(i).equals(existingPath.get(j))) {
        j = fail[j - 1];
      }
      if (existingPath.get(i).equals(existingPath.get(j))) {
        j++;
      }
      fail[i] = j;
    }

    int j = 0;
    for (int i = 0; i < n; i++) {
      while (j > 0 && !newPath.get(i).equals(existingPath.get(j))) {
        j = fail[j - 1];
      }
      if (newPath.get(i).equals(existingPath.get(j))) {
        j++;
      }
      if (j == m) {
        if (i == n - 1) {
          return true;
        }
        j = fail[j - 1];
      }
    }

    return j > 0;
  }

  public PathCase getFirstMatchingCase(BlockGraphPath existingPath) {
    if (existingPath.isSuffixOf(this)) {
      return PathCase.SUFFIX_OR_EQUAL;
    }
    if (existingPath.isPrefixOf(this)) {
      return PathCase.REAL_PREFIX;
    }
    if (overlapsWith(existingPath)) {
      return PathCase.OVERLAP;
    }
    if (existingPath.overlapsWith(this)) {
      return PathCase.REVERSE_OVERLAP;
    }
    return PathCase.OTHER;
  }
}
