// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

/** Either status or recoverable failure is NULL! * */
public class SMGMergeStatusOrRecoverableFailure {
  private final SMGMergeStatus status;
  private final SMGRecoverableFailure recoverableFailure;

  private SMGMergeStatusOrRecoverableFailure(
      SMGMergeStatus pStatus, SMGRecoverableFailure pRecoverableFailure) {
    status = pStatus;
    recoverableFailure = pRecoverableFailure;
  }

  public static SMGMergeStatusOrRecoverableFailure of(SMGMergeStatus pStatus) {
    return new SMGMergeStatusOrRecoverableFailure(pStatus, null);
  }

  public static SMGMergeStatusOrRecoverableFailure of(SMGRecoverableFailure pRecoverableFailure) {
    return new SMGMergeStatusOrRecoverableFailure(null, pRecoverableFailure);
  }

  public boolean isRecoverableFailure() {
    return recoverableFailure != null;
  }

  public SMGRecoverableFailure getRecoverableFailure() {
    return recoverableFailure;
  }

  public SMGMergeStatus getStatus() {
    return status;
  }
}
