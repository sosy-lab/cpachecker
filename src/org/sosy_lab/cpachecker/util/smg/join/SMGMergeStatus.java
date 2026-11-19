// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

/** The join of two SMG returns a status flag. */
public enum SMGMergeStatus {
  EQUAL("≃"),
  /** The left state is entailed (contained) in the right state, e.g. 3+ ⊏ 2+. */
  LEFT_ENTAILED_IN_RIGHT("⊏"),
  /** The right state is entailed (contained) in the left state, e.g. 2+ ⊐ 3+. */
  RIGHT_ENTAILED_IN_LEFT("⊐"),
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
