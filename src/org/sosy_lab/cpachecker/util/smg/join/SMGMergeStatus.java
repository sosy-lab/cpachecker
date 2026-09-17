// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

/**
 * The join of two SMGs returns a status flag, arguing about the relation of the two graphs and
 * their stack frames, but excluding value interpretations.
 */
public enum SMGMergeStatus {
  /**
   * Two SMGs are considered semantically equivalent if they are structurally identical modulo
   * internal node (value/object) identifiers. Concretely, there must be a one-to-one mapping of the
   * SPCs and SMGs, preserving object attributes: kind, size, level, validity, and SLS/DLS metadata,
   * as well as all has-value edges (but not their values), points-to edges (including their
   * targets), offsets, types, target specifiers, as well as the stack frame. The equality of values
   * and or their constraints is NOT included in this! This does not generalize SMGs or checks
   * whether one SMG entails another.
   */
  EQUAL("≃"),
  /**
   * The left SMG is entailed (contained) in the right SMG, e.g. 3+ ⊏ 2+. Stack frames are equal.
   */
  LEFT_ENTAILED_IN_RIGHT("⊏"),
  /**
   * The right SMG is entailed (contained) in the left SMG, e.g. 2+ ⊐ 3+. Stack frames are equal.
   */
  RIGHT_ENTAILED_IN_LEFT("⊐"),
  /** The resulting SMG is neither EQUAL, LEFT_ENTAILED_IN_RIGHT, nor RIGHT_ENTAILED_IN_LEFT. */
  INCOMPARABLE("⋈");

  private final String symbol;

  SMGMergeStatus(String pSymbol) {
    symbol = pSymbol;
  }

  @Override
  public String toString() {
    return symbol + " (" + name() + ")";
  }

  /**
   * Table from TR "Byte-Precise Verification of Low-Level List Manipulation" [Dudka]:
   *
   * <pre>
   *        |   s2
   *        | ≃ ⊐ ⊏ ⋈
   *   -----|---------
   *      ≃ | ≃ ⊐ ⊏ ⋈
   *   s1 ⊐ | ⊐ ⊐ ⋈ ⋈
   *      ⊏ | ⊏ ⋈ ⊏ ⋈
   *      ⋈ | ⋈ ⋈ ⋈ ⋈
   * </pre>
   */
  public SMGMergeStatus updateWith(SMGMergeStatus pStatus2) {
    if (this == SMGMergeStatus.EQUAL || this == pStatus2) {
      return pStatus2;
    } else if (pStatus2 == SMGMergeStatus.EQUAL) {
      return this;
    }
    return SMGMergeStatus.INCOMPARABLE;
  }
}
