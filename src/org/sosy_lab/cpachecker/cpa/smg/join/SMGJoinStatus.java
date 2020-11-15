// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

/**
 * The join of two SMG returns a status flag.
 *
 * <p>We extend the definition with an additional status 'INCOMPLETE'.
 */
public enum SMGJoinStatus {
  EQUAL("≃"),
  LEFT_ENTAIL("⊏"),
  RIGHT_ENTAIL("⊐"),
  INCOMPARABLE("⋈");

  private final String name;

  SMGJoinStatus(String pName) {
    name = pName;
  }

  @Override
  public String toString() {
    return name;
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
  // TODO handle "INCOMPLETE"?
  public SMGJoinStatus updateWith(SMGJoinStatus pStatus2) {
    if (this == SMGJoinStatus.EQUAL || this == pStatus2) {
      return pStatus2;
    } else if (pStatus2 == SMGJoinStatus.EQUAL) {
      return this;
    }
    return SMGJoinStatus.INCOMPARABLE;
  }
}
