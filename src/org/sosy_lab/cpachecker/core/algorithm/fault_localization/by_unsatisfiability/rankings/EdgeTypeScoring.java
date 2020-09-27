// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

/** Sort faults based on their contained edge types. */
public class EdgeTypeScoring implements FaultScoring {

  private double getScore(CFAEdge edge) {
    switch (edge.getEdgeType()) {
      case AssumeEdge:
        return 100d;
      case StatementEdge:
        return 50d;
      case ReturnStatementEdge:
        return 25d;
      case FunctionReturnEdge:
        // fall through
      case CallToReturnEdge:
        // fall through
      case FunctionCallEdge:
        return 12.5;
      case DeclarationEdge:
        // fall through
      case BlankEdge:
        // fall through
      default:
        return 0;
    }
  }

  @Override
  public RankInfo scoreFault(Fault fault) {
    double sum = fault.stream().mapToDouble(fc -> getScore(fc.correspondingEdge())).sum();
    return FaultInfo.rankInfo("Score based on edge type(s).", sum);
  }
}
