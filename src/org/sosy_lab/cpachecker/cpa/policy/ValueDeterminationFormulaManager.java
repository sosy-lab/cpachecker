/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.policy;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.*;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * All our SSA-customization code should go there.
 */
public class ValueDeterminationFormulaManager {

  /**
   * Return value datastructure for the value determination formula.
   */
  public static class ValueDeterminationConstraint {
    final BooleanFormula constraints;
    final Table<CFANode, LinearExpression, NumeralFormula> templateFormulaMap;

    ValueDeterminationConstraint(
        BooleanFormula constraints,
        Table<CFANode, LinearExpression, NumeralFormula> templateFormulaMap
        ) {
      this.constraints = constraints;
      this.templateFormulaMap = templateFormulaMap;
    }
  }

  private final PathFormulaManager pfmgr;
  private final FormulaManager formulaManager;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final NumeralFormulaManagerView<NumeralFormula, RationalFormula> rfmgr;
  private final LogManager logger;
  private final LinearConstraintManager lcmgr;
  private final CFA cfa;

  private final int threshold;

  private static final String BOUND_VAR_NAME = "BOUND_%s_%s";

  public ValueDeterminationFormulaManager(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      Configuration config,
      LogManager logger,
      ShutdownNotifier shutdownNotifier,
      MachineModel machineModel,
      CFA cfa,
      FormulaManager rfmgr,
      LinearConstraintManager lcmgr
  ) {
    this.pfmgr = pfmgr;
    this.fmgr = fmgr;
    this.rfmgr = fmgr.getRationalFormulaManager();
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.logger = logger;
    this.cfa = cfa;
    this.formulaManager = rfmgr;
    this.lcmgr = lcmgr;

    threshold = getThreshold(cfa);
  }

  /**
   * Convert a value determination problem into a single formula.
   *
   * @param policy Selected policy
   * @return {@link org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula} representing the CFA subset.
   * @throws CPATransferException
   * @throws InterruptedException
  // NOTE: So I am doing something which seems quite silly here.
  // Iteration over all the nodes should not be required, I should iterate
  // only over the reachable ones [or better yet, the ones involved in a created
  // cycle.
  // But such an improvement can be left as a todo-item for later.
   */
  public ValueDeterminationConstraint valueDeterminationFormula(
      final Map<CFANode, Map<LinearExpression, CFAEdge>> policy
      ) throws CPATransferException, InterruptedException{

    List<BooleanFormula> constraints = new LinkedList<>();

    Table<CFANode, LinearExpression, NumeralFormula> outSSAMap = HashBasedTable.create();

    for (CFANode toNode : policy.keySet()) {
      for (Entry<LinearExpression, CFAEdge> incoming : policy.get(toNode).entrySet()) {

        LinearExpression template = incoming.getKey();
        CFAEdge incomingEdge = incoming.getValue();

        CFANode fromNode = incomingEdge.getPredecessor();
        int fromNodePrimeNo = toPrime(fromNode.getNodeNumber());
        int toNodeNo = toNode.getNodeNumber();
        int toNodePrimeNo = toPrime(toNodeNo);

        PathFormula edgeFormula = pathFormulaWithCustomIdx(
            incomingEdge,
            toNodeNo,
            toNodePrimeNo
        );

        constraints.add(edgeFormula.getFormula());

        if (policy.get(fromNode) == null) {
          // NOTE: nodes with no templates aren't in the policy.
          continue;
        }

        for (LinearExpression fromTemplate : policy.get(fromNode).keySet()) {
          RationalFormula fromTemplateUpperBound =
              linearExpressionToFormula(
                  fromTemplate, SSAMap.withDefault(fromNodePrimeNo));
          RationalFormula incomingTemplateF =  linearExpressionToFormula(fromTemplate,
                  SSAMap.withDefault(toNodeNo));

          BooleanFormula f = rfmgr.lessOrEquals(
              incomingTemplateF, fromTemplateUpperBound);

          constraints.add(f);
        }

        outSSAMap.put(
            toNode,
            template,
            lcmgr.linearExpressionToFormula(template, edgeFormula.getSsa())
        );
      }
    }
    return new ValueDeterminationConstraint(bfmgr.and(constraints), outSSAMap);
  }

  private RationalFormula linearExpressionToFormula(LinearExpression expr, SSAMap ssa) {
    RationalFormula sum = rfmgr.makeNumber(0);
    for (Map.Entry<String, ExtendedRational> monomial : expr) {
      String var = monomial.getKey();
      ExtendedRational coeff = monomial.getValue();

      RationalFormula f = rfmgr.makeVariable(FormulaManagerView.makeName(
          var, ssa.getIndex(var)));
      RationalFormula m = rfmgr.multiply(f, rfmgr.makeNumber(coeff.toString()));

      sum = rfmgr.add(sum, m);
    }
    return sum;
  }


  /**
   * Create a path formula for the edge, specifying <i>both</i> custom
   * from-index and the custom to-index.
   * E.g. for statement {@code x++}, start index set to 2 and stop index set to 1000
   * will produce:
   *
   *    x@1000 = x@2 + 1
   */
  private PathFormula pathFormulaWithCustomIdx(
      CFAEdge edge, int startIdx, int stopIdx) throws CPATransferException,
      InterruptedException {

    PathFormula p = pathFormulaWithCustomStartIdx(edge, startIdx);
    SSAMap customFromIdxSSAMap = p.getSsa();

    SSAMap.SSAMapBuilder newMapBuilder = customFromIdxSSAMap.builder();

    List<Formula> fromVars = new LinkedList<>();
    List<Formula> toVars = new LinkedList<>();

    for (Entry<String, CType> entry : customFromIdxSSAMap.allVariablesWithTypes()) {
      String variable = entry.getKey();
      CType type = entry.getValue();
      int idx = customFromIdxSSAMap.getIndex(variable);
      if (idx != startIdx) {
        fromVars.add(
            // NOTE: we are making an implicit assumption
            // here that everything is a rational.
            rfmgr.makeVariable(
                FormulaManagerView.makeName(variable, idx))
        );
        toVars.add(
            rfmgr.makeVariable(
                FormulaManagerView.makeName(variable, stopIdx)
            )
        );
        newMapBuilder.setIndex(variable, type, stopIdx);
      }
    }

    return new PathFormula(
        formulaManager.getUnsafeFormulaManager().substitute(
            p.getFormula(), fromVars, toVars
        ),
        newMapBuilder.build(),
        p.getPointerTargetSet(),
        p.getLength());
  }

  /**
   * Creates a {@link PathFormula} with SSA indexing starting
   * from the specified value.
   * E.g. for {@code x++} and starting index set to 1000 will produce:
   *
   *    x@1001 = x@1000 + 1
   */
  private PathFormula pathFormulaWithCustomStartIdx(CFAEdge edge, int startIdx)
      throws CPATransferException, InterruptedException {
    PathFormula empty = pfmgr.makeEmptyPathFormula();
    PathFormula emptyWithCustomSSA = pfmgr.makeNewPathFormula(
        empty,
        SSAMap.withDefault(startIdx));

    return pfmgr.makeAnd(emptyWithCustomSSA, edge);
  }

  /**
   * The formula encoding uses separate numbering conventions for variables
   * associated with the node "input" and the variables associated with the
   * node "output".
   * The later numbering starts with <getThreshold>.
   * The threshold is guaranteed to be a multiple of 10 and bigger than the
   * number of nodes, and at least a thousand (for readability).
   */
  private int getThreshold(CFA cfa) {
    double magnitude = Math.log10(cfa.getAllNodes().size());
    return Math.min(
        1000,
        (int) Math.pow(10, magnitude)
    );
  }

  /**
   * Convert the number from the "input" numbering convention to the "output"
   * numbering convention.
   */
  private int toPrime(int no) {
    return threshold + no;
  }
}
