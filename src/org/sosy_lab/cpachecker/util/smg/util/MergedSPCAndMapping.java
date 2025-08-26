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
import org.sosy_lab.cpachecker.util.smg.join.NodeMapping;

public class MergedSPCAndMapping {

  private final SymbolicProgramConfiguration mergedSPC;
  private final NodeMapping mapping;

  private MergedSPCAndMapping(SymbolicProgramConfiguration pMergedSPC, NodeMapping pMapping) {
    Preconditions.checkNotNull(pMergedSPC);
    Preconditions.checkNotNull(pMapping);
    mergedSPC = pMergedSPC;
    mapping = pMapping;
  }

  public static MergedSPCAndMapping of(
      SymbolicProgramConfiguration pMergedSpc, NodeMapping pMapping) {
    return new MergedSPCAndMapping(pMergedSpc, pMapping);
  }

  public SymbolicProgramConfiguration getMergedSPC() {
    return mergedSPC;
  }

  public NodeMapping getMapping() {
    return mapping;
  }
}
