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

public class MergedSPCAndMergeStatus {
  private final SymbolicProgramConfiguration spc;
  private final SMGMergeStatus status;

  private MergedSPCAndMergeStatus(SymbolicProgramConfiguration pSPc, SMGMergeStatus pStatus) {
    Preconditions.checkNotNull(pSPc);
    Preconditions.checkNotNull(pStatus);
    spc = pSPc;
    status = pStatus;
  }

  public static MergedSPCAndMergeStatus of(
      SymbolicProgramConfiguration pSPc, SMGMergeStatus pStatus) {
    return new MergedSPCAndMergeStatus(pSPc, pStatus);
  }

  public SymbolicProgramConfiguration getMergedSPC() {
    return spc;
  }

  public SMGMergeStatus getMergeStatus() {
    return status;
  }
}
