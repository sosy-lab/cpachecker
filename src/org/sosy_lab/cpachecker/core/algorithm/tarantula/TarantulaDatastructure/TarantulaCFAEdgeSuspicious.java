// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
/** Class represents the corresponding CFAEdge with its suspicious */
public class TarantulaCFAEdgeSuspicious {
  private final CFAEdge cfaEdge;
  private final double cfaEdgeSuspicious;

  public TarantulaCFAEdgeSuspicious(CFAEdge pCFAEdge, double pCfaEdgeSuspicious) {
    this.cfaEdge = pCFAEdge;
    this.cfaEdgeSuspicious = pCfaEdgeSuspicious;
  }

  public CFAEdge getCfaEdge() {
    return cfaEdge;
  }

  public double getCfaEdgeSuspicious() {
    return cfaEdgeSuspicious;
  }

  @Override
  public String toString() {
    return "TarantulaCFAEdgeSuspicious{"
        + "cfaEdge="
        + cfaEdge
        + ", suspicious="
        + cfaEdgeSuspicious
        + '}';
  }
}
