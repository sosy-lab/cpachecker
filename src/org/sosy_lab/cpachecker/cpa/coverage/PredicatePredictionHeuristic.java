// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicatePredictionHeuristic {

  public static Set<BooleanFormula> predictPredicates(CFA cfa, CtoFormulaConverter converter)
      throws UnrecognizedCodeException, InterruptedException {
    Set<CFAEdge> cfaEdgeSet = extractAllEdgesFromCFA(cfa);
    Set<BooleanFormula> allPredicates = new HashSet<>();

    for (CFAEdge cfaEdge : cfaEdgeSet) {
      if (cfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        CExpression assumeExpression = ((CAssumeEdge) cfaEdge).getExpression();
        String function = cfaEdge.getSuccessor().getFunctionName();
        BooleanFormula predicate =
            converter.makePredicate(
                assumeExpression, cfaEdge, function, SSAMap.emptySSAMap().builder());
        allPredicates.add(predicate);
        // TODO: Enhance heuristic by adding further potential predicate candidates
        //  and rule predicates out which are already contained in a negated form
      }
    }
    return allPredicates;
  }

  private static Set<CFAEdge> extractAllEdgesFromCFA(CFA cfa) {
    Set<CFAEdge> cfaEdgeSet = new HashSet<>();
    for (CFANode node : cfa.getAllNodes()) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        cfaEdgeSet.add(node.getLeavingEdge(i));
      }
    }
    return cfaEdgeSet;
  }
}
