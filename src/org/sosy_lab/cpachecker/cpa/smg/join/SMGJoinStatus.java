/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
  INCOMPARABLE("⋈"),
  INCOMPLETE("?");

  private final String name;

  private SMGJoinStatus(String pName) {
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
  public static SMGJoinStatus updateStatus(SMGJoinStatus pStatus1, SMGJoinStatus pStatus2) {
    if (pStatus1 == SMGJoinStatus.EQUAL || pStatus1 == pStatus2) {
      return pStatus2;
    } else if (pStatus2 == SMGJoinStatus.EQUAL) {
      return pStatus1;
    }
    return SMGJoinStatus.INCOMPARABLE;
  }
}
