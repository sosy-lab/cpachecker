// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.smg.join.SMGMergeStatus;

public class StatesMergedAndMergeStatus {

  private final SMGState leftMergeStateFromTransfer;
  private final SMGState stateFromReached;
  private final SMGMergeStatus status;

  private StatesMergedAndMergeStatus(
      SMGState newSMGState, SMGState smgStateFromReached, SMGMergeStatus pStatus) {
    Preconditions.checkNotNull(newSMGState);
    Preconditions.checkNotNull(smgStateFromReached);
    Preconditions.checkNotNull(pStatus);
    leftMergeStateFromTransfer = newSMGState;
    stateFromReached = smgStateFromReached;
    status = pStatus;
  }

  public static StatesMergedAndMergeStatus of(
      SMGState newSMGState, SMGState smgStateFromReached, SMGMergeStatus pStatus) {
    return new StatesMergedAndMergeStatus(newSMGState, smgStateFromReached, pStatus);
  }

  /**
   * Left input state e´ in merge, i.e. the new successor state that was the result of applying the
   * transfer relation.
   */
  public SMGState getLeftMergeStateFromTransfer() {
    return leftMergeStateFromTransfer;
  }

  /** Right input state e´´ used in merge, i.e. the state from reached. */
  public SMGState getStateFromReached() {
    return stateFromReached;
  }

  @SuppressWarnings("unused")
  public SMGMergeStatus getMergeStatus() {
    return status;
  }
}
