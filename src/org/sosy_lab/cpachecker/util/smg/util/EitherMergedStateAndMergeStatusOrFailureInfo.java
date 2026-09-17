// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;

public class EitherMergedStateAndMergeStatusOrFailureInfo {

  private final boolean failedInPreconditions;
  // Empty optional AND failedInPreconditions == false -> merge failed due to precision loss
  private final Optional<MergedSMGStateAndMergeStatus> maybeMergedStateAndStatus;

  private EitherMergedStateAndMergeStatusOrFailureInfo(
      final boolean pFailedInPreconditions,
      final Optional<MergedSMGStateAndMergeStatus> pMaybeMergedStateAndStatus) {
    failedInPreconditions = pFailedInPreconditions;
    maybeMergedStateAndStatus = pMaybeMergedStateAndStatus;
  }

  public static EitherMergedStateAndMergeStatusOrFailureInfo ofFailedMergeDueToPrecisionLoss() {
    return new EitherMergedStateAndMergeStatusOrFailureInfo(false, Optional.empty());
  }

  public static EitherMergedStateAndMergeStatusOrFailureInfo ofFailedMergeDueToPreconditions() {
    return new EitherMergedStateAndMergeStatusOrFailureInfo(true, Optional.empty());
  }

  public static EitherMergedStateAndMergeStatusOrFailureInfo ofSuccessfulMerge(
      MergedSMGStateAndMergeStatus pMergedSMGStateAndMergeStatus) {
    return new EitherMergedStateAndMergeStatusOrFailureInfo(
        false, Optional.of(pMergedSMGStateAndMergeStatus));
  }

  public boolean failedDueToPrecisionLoss() {
    return !failedInPreconditions && maybeMergedStateAndStatus.isEmpty();
  }

  public boolean failedDueToPreconditions() {
    checkState(maybeMergedStateAndStatus.isEmpty());
    return failedInPreconditions;
  }

  public boolean isMergedSuccessfully() {
    return maybeMergedStateAndStatus.isPresent();
  }

  /**
   * Should only be used if {@link #failedDueToPrecisionLoss()} and {@link
   * #failedDueToPreconditions()} both returned false, or alternatively {@link
   * #isMergedSuccessfully()} returned true.
   */
  public MergedSMGStateAndMergeStatus getMergedStateAndMergeStatus() {
    return maybeMergedStateAndStatus.orElseThrow();
  }
}
