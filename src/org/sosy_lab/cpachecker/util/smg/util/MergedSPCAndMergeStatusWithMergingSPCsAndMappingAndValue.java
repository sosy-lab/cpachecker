// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue {

  private final SMGValue value;
  private final MergedSPCAndMergeStatusWithMergingSPCsAndMapping
      mergedSPCAndMergeStatusWithMergingSPCsAndMapping;
  private final boolean recoverableFailure;

  private MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue(
      SMGValue pSMGValue,
      MergedSPCAndMergeStatusWithMergingSPCsAndMapping
          pMergedSPCAndMergeStatusWithMergingSPCsAndMapping,
      boolean pRecoverableFailure) {
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
        pSMGValue, pMergedSPCAndMergeStatusWithMergingSPCsAndMapping, false);
  }

  public static MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue recoverableFailure() {
    return new MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue(null, null, true);
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
    return recoverableFailure;
  }
}
