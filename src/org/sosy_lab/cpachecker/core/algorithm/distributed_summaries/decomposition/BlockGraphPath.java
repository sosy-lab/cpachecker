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

public record BlockGraphPath(ImmutableList<String> path) {

  public enum PathCase {
    SUFFIX_OR_EQUAL,
    OVERLAP,
    REAL_PREFIX,
    OTHER;
  }

  public static BlockGraphPath of(String... pathParts) {
    return new BlockGraphPath(ImmutableList.copyOf(pathParts));
  }

  public static BlockGraphPath of(Collection<String> pathParts) {
    return new BlockGraphPath(ImmutableList.copyOf(pathParts));
  }

  public boolean isSuffixOf(BlockGraphPath other) {
    if (other.path().size() < path.size()) {
      return false;
    }

    for (int i = path.size() - 1; i >= 0; i--) {
      if (!other.path().get(i).equals(path.get(i))) {
        return false;
      }
    }

    return true;
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
    // TODO Knuth Morris
    int maxOverlap = Integer.min(path.size(), other.path().size());

    for (int overlap = maxOverlap; overlap >= 1; overlap--) {
      boolean matches = true;

      for (int i = 0; i < overlap; i++) {
        if (!path.get(path.size() - overlap + i).equals(other.path().get(i))) {
          matches = false;
          break;
        }
      }

      if (matches) {
        return true;
      }
    }

    return false;
  }

  public PathCase getFirstMatchingCase(BlockGraphPath existingPath) {
    if (existingPath.isSuffixOf(this)) {
      return PathCase.SUFFIX_OR_EQUAL;
    }
    if (overlapsWith(existingPath)) {
      return PathCase.OVERLAP;
    }
    if (existingPath.isPrefixOf(this)) {
      return PathCase.REAL_PREFIX;
    }
    return PathCase.OTHER;
  }
}
