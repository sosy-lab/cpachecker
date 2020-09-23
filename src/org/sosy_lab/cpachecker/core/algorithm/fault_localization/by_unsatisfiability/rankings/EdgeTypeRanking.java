// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

/**
 * Sort faults based on their contained edge types.
 */
public class EdgeTypeRanking implements FaultRanking {

  @Override
  public List<Fault> rank(Set<Fault> result) {
    FaultRankingUtils.RankingResults results =
        FaultRankingUtils.rankedListFor(result, f -> f.stream().mapToDouble(fc -> getScore(fc.correspondingEdge().getEdgeType())).sum());
    double overallSum = results.getLikelihoodMap().values().stream().mapToDouble(Double::doubleValue).sum();
    for (Entry<Fault, Double> faultDoubleEntry : results.getLikelihoodMap().entrySet()) {
      faultDoubleEntry.getKey().addInfo(FaultInfo.rankInfo("Score calculated by edge type(s).", overallSum==0 ? 1d : faultDoubleEntry.getValue()/overallSum));
    }
    return results.getRankedList();
  }

  private double getScore(CFAEdgeType edgeType) {
    switch (edgeType) {
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

/*  private String readableName(CFAEdgeType type){
    StringJoiner joiner = new StringJoiner(" ");
    for (String s : Splitter.onPattern("(?=\\p{Upper})").split(type.name())) {
      joiner.add(s);
    }
    return joiner.toString();
  }*/

}
