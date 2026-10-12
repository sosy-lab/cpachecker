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
  private final SMGState newState;
  private final SMGState stateFromReached;
  private final SMGMergeStatus status;

  private StatesMergedAndMergeStatus(
      SMGState newSMGState, SMGState smgStateFromReached, SMGMergeStatus pStatus) {
    Preconditions.checkNotNull(newSMGState);
    Preconditions.checkNotNull(smgStateFromReached);
    Preconditions.checkNotNull(pStatus);
    newState = newSMGState;
    stateFromReached = smgStateFromReached;
    status = pStatus;
  }

  public static StatesMergedAndMergeStatus of(
      SMGState newSMGState, SMGState smgStateFromReached, SMGMergeStatus pStatus) {
    return new StatesMergedAndMergeStatus(newSMGState, smgStateFromReached, pStatus);
  }

  /** Left state from merge, i.e. the new successor state. */
  public SMGState getNewState() {
    return newState;
  }

  /**
   * Right state from merge, i.e. the state from reached. This state should no longer be in reached
   * for a returned merged state that is distinct to this one.
   */
  public SMGState getStateFromReached() {
    return stateFromReached;
  }

  @SuppressWarnings("unused")
  public SMGMergeStatus getMergeStatus() {
    return status;
  }
}
