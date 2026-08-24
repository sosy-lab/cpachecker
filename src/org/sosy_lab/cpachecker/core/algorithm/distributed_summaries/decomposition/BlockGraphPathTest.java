// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class BlockGraphPathTest {

  @Test
  public void isPrefixOf_emptyIsPrefixOfAnything() {
    assertThat(BlockGraphPath.of().isPrefixOf(BlockGraphPath.of())).isTrue();
    assertThat(BlockGraphPath.of().isPrefixOf(BlockGraphPath.of("A", "B"))).isTrue();
  }

  @Test
  public void isPrefixOf_equalPathsCountAsPrefix() {
    assertThat(BlockGraphPath.of("A", "B").isPrefixOf(BlockGraphPath.of("A", "B"))).isTrue();
  }

  @Test
  public void isPrefixOf_properPrefix() {
    assertThat(BlockGraphPath.of("A").isPrefixOf(BlockGraphPath.of("A", "B"))).isTrue();
  }

  @Test
  public void isPrefixOf_falseWhenLongerThanOther() {
    assertThat(BlockGraphPath.of("A", "B").isPrefixOf(BlockGraphPath.of("A"))).isFalse();
  }

  @Test
  public void isPrefixOf_falseWhenElementsDiffer() {
    assertThat(BlockGraphPath.of("A", "C").isPrefixOf(BlockGraphPath.of("A", "B"))).isFalse();
  }

  @Test
  public void isSuffixOf_emptyIsSuffixOfAnything() {
    assertThat(BlockGraphPath.of().isSuffixOf(BlockGraphPath.of())).isTrue();
    assertThat(BlockGraphPath.of().isSuffixOf(BlockGraphPath.of("A", "B"))).isTrue();
  }

  @Test
  public void isSuffixOf_equalPathsCountAsSuffix() {
    assertThat(BlockGraphPath.of("A", "B").isSuffixOf(BlockGraphPath.of("A", "B"))).isTrue();
  }

  @Test
  public void isSuffixOf_properSuffix() {
    assertThat(BlockGraphPath.of("B").isSuffixOf(BlockGraphPath.of("A", "B"))).isTrue();
  }

  @Test
  public void isSuffixOf_falseWhenLongerThanOther() {
    assertThat(BlockGraphPath.of("A", "B").isSuffixOf(BlockGraphPath.of("B"))).isFalse();
  }

  @Test
  public void isSuffixOf_falseWhenElementsDiffer() {
    assertThat(BlockGraphPath.of("C", "B").isSuffixOf(BlockGraphPath.of("A", "B"))).isFalse();
  }

  @Test
  public void overlapsWith_emptyOtherAlwaysOverlaps() {
    assertThat(BlockGraphPath.of().overlapsWith(BlockGraphPath.of())).isTrue();
    assertThat(BlockGraphPath.of("A", "B").overlapsWith(BlockGraphPath.of())).isTrue();
  }

  @Test
  public void overlapsWith_emptyPathOnlyOverlapsWithEmptyOther() {
    assertThat(BlockGraphPath.of().overlapsWith(BlockGraphPath.of("A"))).isFalse();
  }

  @Test
  public void overlapsWith_falseWhenPathsShareNoElement() {
    assertThat(BlockGraphPath.of("A", "B").overlapsWith(BlockGraphPath.of("C", "D"))).isFalse();
  }

  @Test
  public void overlapsWith_trueWhenThisIsAPrefixOfOther() {
    // [L0] is entirely consumed as a prefix of the longer [L0, L5].
    assertThat(BlockGraphPath.of("L0").overlapsWith(BlockGraphPath.of("L0", "L5"))).isTrue();
  }

  @Test
  public void overlapsWith_trueWhenOtherIsASuffixOfThis() {
    // [B] is entirely consumed as a suffix of the longer [A, B].
    assertThat(BlockGraphPath.of("A", "B").overlapsWith(BlockGraphPath.of("B"))).isTrue();
  }

  @Test
  public void overlapsWith_trueOnBoundaryOverlapWithoutContainment() {
    // The last element of [A, B] matches the first element of [B, C]; neither path contains
    // the other.
    assertThat(BlockGraphPath.of("A", "B").overlapsWith(BlockGraphPath.of("B", "C"))).isTrue();
  }

  @Test
  public void overlapsWith_notSymmetric() {
    assertThat(BlockGraphPath.of("L0", "L5").overlapsWith(BlockGraphPath.of("L0"))).isFalse();
    assertThat(BlockGraphPath.of("B", "C").overlapsWith(BlockGraphPath.of("A", "B"))).isFalse();
    assertThat(BlockGraphPath.of("B").overlapsWith(BlockGraphPath.of("A", "B"))).isFalse();
  }

  @Test
  public void overlapsWith_selfOverlappingPatternUsesFailureFunctionCorrectly() {
    // "A" is both a prefix and a suffix of the pattern [A, B, A], so a naive search that does
    // not fall back correctly on a mismatch could miss this match.
    assertThat(BlockGraphPath.of("X", "A", "B", "A").overlapsWith(BlockGraphPath.of("A", "B", "A")))
        .isTrue();
  }

  @Test
  public void overlapsWith_boundaryMatchAfterFailedFullMatch() {
    // [A, B, A, B] fully matches the first three elements of [A, B, A, C], then falls back via
    // the failure function to a shorter, still-valid boundary match of length 2 ("A, B").
    assertThat(
            BlockGraphPath.of("A", "B", "A", "B")
                .overlapsWith(BlockGraphPath.of("A", "B", "A", "C")))
        .isTrue();
  }
}
