// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.util.smg.join.SMGMergeStatus;

public class MergingSPCsAndMergeStatus {
  SymbolicProgramConfiguration mergingSPC1;
  SymbolicProgramConfiguration mergingSPC2;
  SMGMergeStatus mergeStatus;

  private MergingSPCsAndMergeStatus(
      SymbolicProgramConfiguration pMergingSPC1,
      SymbolicProgramConfiguration pMergingSPC2,
      SMGMergeStatus pMergeStatus) {
    Preconditions.checkNotNull(pMergingSPC1);
    Preconditions.checkNotNull(pMergingSPC2);
    Preconditions.checkNotNull(pMergeStatus);
    mergingSPC1 = pMergingSPC1;
    mergingSPC2 = pMergingSPC2;
    mergeStatus = pMergeStatus;
  }

  public static MergingSPCsAndMergeStatus of(
      SymbolicProgramConfiguration pMergingSPC1,
      SymbolicProgramConfiguration pMergingSPC2,
      SMGMergeStatus pMergeStatus) {
    return new MergingSPCsAndMergeStatus(pMergingSPC1, pMergingSPC2, pMergeStatus);
  }

  public SymbolicProgramConfiguration getMergingSPC1() {
    return mergingSPC1;
  }

  public SymbolicProgramConfiguration getMergingSPC2() {
    return mergingSPC2;
  }

  public SMGMergeStatus getMergeStatus() {
    return mergeStatus;
  }
}
