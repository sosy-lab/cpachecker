// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.smg.join.SMGMergeStatus;

public class MergedSMGStateAndMergeStatus {
  private final SMGState mergedState;
  private final SMGMergeStatus status;

  private MergedSMGStateAndMergeStatus(SMGState pMergedState, SMGMergeStatus pStatus) {
    Preconditions.checkNotNull(pMergedState);
    Preconditions.checkNotNull(pStatus);
    mergedState = pMergedState;
    status = pStatus;
  }

  public static MergedSMGStateAndMergeStatus of(SMGState mergedState, SMGMergeStatus mergeStatus) {
    return new MergedSMGStateAndMergeStatus(mergedState, mergeStatus);
  }

  public SMGState getMergedSMGState() {
    return mergedState;
  }

  public SMGMergeStatus getMergeStatus() {
    return status;
  }
}
