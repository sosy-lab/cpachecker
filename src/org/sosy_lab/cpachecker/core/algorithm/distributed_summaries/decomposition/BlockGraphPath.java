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

  /** Whether this path's elements are a prefix of {@code other}'s (equal paths count as well). */
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

  /**
   * Whether a non-empty suffix of this path matches a prefix of {@code other} (including {@code
   * other} being entirely a suffix of this path). Always true if {@code other} is empty.
   *
   * <p>E.g. {@code [A, B].overlapsWith([B, C])} and {@code [A, B].overlapsWith([B])} are both true,
   * but this is not symmetric: {@code [B, C].overlapsWith([A, B])} is false.
   *
   * <p>Implemented as a KMP search of {@code other} inside this path, reusing {@code other}'s
   * failure function to also detect a partial match running off the end of this path. Runs in
   * O(n+m), with n and m the lengths of this path and {@code other}.
   */
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

    // KMP failure function: fail[i] is the length of the longest proper prefix of
    // existingPath[0..i] that is also a suffix of it.
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

    // Standard KMP search of existingPath in newPath; j is the length of the longest prefix of
    // existingPath matched so far.
    int j = 0;
    for (int i = 0; i < n; i++) {
      while (j > 0 && !newPath.get(i).equals(existingPath.get(j))) {
        j = fail[j - 1];
      }
      if (newPath.get(i).equals(existingPath.get(j))) {
        j++;
      }
      if (j == m) {
        // Full match of existingPath ending here. If it ends exactly at the last element of
        // newPath, existingPath is a suffix of newPath: report the overlap directly, since
        // continuing to search for a later match would fall back to a shorter one (or none).
        if (i == n - 1) {
          return true;
        }
        j = fail[j - 1];
      }
    }

    // No full match of existingPath ends at the last element of newPath, but j > 0 means a
    // proper prefix of existingPath still matches a trailing suffix of newPath.
    return j > 0;
  }
}
