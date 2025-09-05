// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import com.google.common.base.Preconditions;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;

/** Tuple for {@link SMGHasValueEdge}s and {@link SymbolicProgramConfiguration}. */
public class SMGHasValueEdgesAndSPC {

  private final List<SMGHasValueEdge> hves;
  private final SymbolicProgramConfiguration spc;

  private SMGHasValueEdgesAndSPC(List<SMGHasValueEdge> pHves, SymbolicProgramConfiguration pSPC) {
    hves = pHves;
    spc = pSPC;
  }

  public static SMGHasValueEdgesAndSPC of(
      List<SMGHasValueEdge> pHves, SymbolicProgramConfiguration pSPC) {
    Preconditions.checkNotNull(pHves);
    Preconditions.checkArgument(!pHves.isEmpty());
    Preconditions.checkNotNull(pSPC);
    return new SMGHasValueEdgesAndSPC(pHves, pSPC);
  }

  public List<SMGHasValueEdge> getSMGHasValueEdges() {
    return hves;
  }

  public SymbolicProgramConfiguration getSPC() {
    return spc;
  }
}
