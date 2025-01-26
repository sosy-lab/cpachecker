// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

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

  public SMGRecoverableFailure getRecoverableFailure() {
    return recoverableFailure;
  }
}
