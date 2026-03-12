// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import static org.sosy_lab.cpachecker.util.smg.join.SMGRecoverableFailure.SMGRecoverableFailureType.DELAYED_MERGE;
import static org.sosy_lab.cpachecker.util.smg.join.SMGRecoverableFailure.SMGRecoverableFailureType.LEFT_LIST_LONGER;
import static org.sosy_lab.cpachecker.util.smg.join.SMGRecoverableFailure.SMGRecoverableFailureType.RIGHT_LIST_LONGER;

import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.join.SMGRecoverableFailure;

public class MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue {

  private final SMGValue value;
  private final MergedSPCAndMergeStatusWithMergingSPCsAndMapping
      mergedSPCAndMergeStatusWithMergingSPCsAndMapping;
  private final SMGRecoverableFailure recoverableFailure;

  private MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue(
      SMGValue pSMGValue,
      MergedSPCAndMergeStatusWithMergingSPCsAndMapping
          pMergedSPCAndMergeStatusWithMergingSPCsAndMapping,
      SMGRecoverableFailure pRecoverableFailure) {
    value = pSMGValue;
    mergedSPCAndMergeStatusWithMergingSPCsAndMapping =
        pMergedSPCAndMergeStatusWithMergingSPCsAndMapping;
    recoverableFailure = pRecoverableFailure;
  }

  public static MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue of(
      SMGValue pSMGValue,
      MergedSPCAndMergeStatusWithMergingSPCsAndMapping
          pMergedSPCAndMergeStatusWithMergingSPCsAndMapping) {
    return new MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue(
        pSMGValue, pMergedSPCAndMergeStatusWithMergingSPCsAndMapping, null);
  }

  public static MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue recoverableFailure(
      SMGRecoverableFailure pRecoverableFailure) {
    return new MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue(
        null, null, pRecoverableFailure);
  }

  public SMGValue getSMGValue() {
    return value;
  }

  public MergedSPCAndMergeStatusWithMergingSPCsAndMapping
      getJoinSPCAndJoinStatusWithJoinedSPCsAndMapping() {
    return mergedSPCAndMergeStatusWithMergingSPCsAndMapping;
  }

  /** If this is true, the other inputs are NULL! */
  public boolean isRecoverableFailure() {
    return recoverableFailure != null;
  }

  public boolean isRecoverableFailureTypeDelayedMerge() {
    return recoverableFailure != null && recoverableFailure.getFailureType() == DELAYED_MERGE;
  }

  public boolean isRecoverableFailureTypeLeftListLonger() {
    return recoverableFailure != null && recoverableFailure.getFailureType() == LEFT_LIST_LONGER;
  }

  public boolean isRecoverableFailureTypeRightListLonger() {
    return recoverableFailure != null && recoverableFailure.getFailureType() == RIGHT_LIST_LONGER;
  }

  public SMGRecoverableFailure getRecoverableFailure() {
    return recoverableFailure;
  }
}
