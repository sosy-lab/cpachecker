// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.util.smg.graph.SMGNode;
import org.sosy_lab.cpachecker.util.smg.join.SMGMergeStatus;

public class MergedSPCAndMergeStatusWithMergingSPCsAndMapping {

  private final SymbolicProgramConfiguration mergedSPC;
  private final SMGMergeStatus mergeStatus;
  private final SymbolicProgramConfiguration mergingSpc1;
  private final SymbolicProgramConfiguration mergingSpc2;
  private final ImmutableMap<SMGNode, SMGNode> mapping1;
  private final ImmutableMap<SMGNode, SMGNode> mapping2;

  private MergedSPCAndMergeStatusWithMergingSPCsAndMapping(
      SymbolicProgramConfiguration pMergedSPC,
      SMGMergeStatus pMergeStatus,
      SymbolicProgramConfiguration pMergingSpc1,
      SymbolicProgramConfiguration pMergingSpc2,
      ImmutableMap<SMGNode, SMGNode> pMapping1,
      ImmutableMap<SMGNode, SMGNode> pMapping2) {
    Preconditions.checkNotNull(pMergedSPC);
    Preconditions.checkNotNull(pMergeStatus);
    Preconditions.checkNotNull(pMergingSpc1);
    Preconditions.checkNotNull(pMergingSpc2);
    Preconditions.checkNotNull(pMapping1);
    Preconditions.checkNotNull(pMapping2);
    mergedSPC = pMergedSPC;
    mergeStatus = pMergeStatus;
    mergingSpc1 = pMergingSpc1;
    mergingSpc2 = pMergingSpc2;
    mapping1 = pMapping1;
    mapping2 = pMapping2;
  }

  public static MergedSPCAndMergeStatusWithMergingSPCsAndMapping of(
      SymbolicProgramConfiguration pMergedSpc,
      SMGMergeStatus pMergeStatus,
      SymbolicProgramConfiguration pMergingSpc1,
      SymbolicProgramConfiguration pMergingSpc2,
      ImmutableMap<SMGNode, SMGNode> pMapping1,
      ImmutableMap<SMGNode, SMGNode> pMapping2) {
    return new MergedSPCAndMergeStatusWithMergingSPCsAndMapping(
        pMergedSpc, pMergeStatus, pMergingSpc1, pMergingSpc2, pMapping1, pMapping2);
  }

  public SymbolicProgramConfiguration getMergedSPC() {
    return mergedSPC;
  }

  public SMGMergeStatus getMergeStatus() {
    return mergeStatus;
  }

  public SymbolicProgramConfiguration getMergingSPC1() {
    return mergingSpc1;
  }

  public SymbolicProgramConfiguration getMergingSPC2() {
    return mergingSpc2;
  }

  public ImmutableMap<SMGNode, SMGNode> getMapping1() {
    return mapping1;
  }

  public ImmutableMap<SMGNode, SMGNode> getMapping2() {
    return mapping2;
  }
}
