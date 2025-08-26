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
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.join.NodeMapping;

public class MergedSPCWithMappingsAndAddressValue {
  private final SymbolicProgramConfiguration spc;
  private final SMGValue address;
  private final NodeMapping mapping1;
  private final NodeMapping mapping2;

  private MergedSPCWithMappingsAndAddressValue(
      SymbolicProgramConfiguration pSPc,
      SMGValue pAddressValue,
      NodeMapping pMapping1,
      NodeMapping pMapping2) {
    Preconditions.checkNotNull(pSPc);
    Preconditions.checkNotNull(pAddressValue);
    Preconditions.checkNotNull(pMapping1);
    Preconditions.checkNotNull(pMapping2);
    spc = pSPc;
    address = pAddressValue;
    mapping1 = pMapping1;
    mapping2 = pMapping2;
  }

  public static MergedSPCWithMappingsAndAddressValue of(
      SymbolicProgramConfiguration pSPc,
      SMGValue pAddressValue,
      NodeMapping pMapping1,
      NodeMapping pMapping2) {
    return new MergedSPCWithMappingsAndAddressValue(pSPc, pAddressValue, pMapping1, pMapping2);
  }

  public SymbolicProgramConfiguration getMergedSPC() {
    return spc;
  }

  public SMGValue getAddressValue() {
    return address;
  }

  public NodeMapping getMapping1() {
    return mapping1;
  }

  public NodeMapping getMapping2() {
    return mapping2;
  }
}
