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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
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

/**
 * All our SSA-customization code should go there.
 */
public class ValueDeterminationFormulaManager {

  /**
   * Return value datastructure for the value determination formula.
   */
  public static class ValueDeterminationConstraint {
    final BooleanFormula constraints;
    final Table<CFANode, LinearExpression, Integer> SSATemplateMap;

    ValueDeterminationConstraint(
        BooleanFormula constraints,
        Table<CFANode, LinearExpression, Integer> SSATemplateMap
        ) {
      this.constraints = constraints;
      this.SSATemplateMap = SSATemplateMap;
    }
  }

  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final NumeralFormulaManagerView<NumeralFormula, RationalFormula> rfmgr;

  private static final String BOUND_VAR_NAME = "BOUND_%s_%s";

  public ValueDeterminationFormulaManager(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      Configuration config,
      LogManager logger,
      ShutdownNotifier shutdownNotifier,
      MachineModel machineModel
  ) {
    this.pfmgr = pfmgr;
    this.fmgr = fmgr;
    this.rfmgr = fmgr.getRationalFormulaManager();
    this.bfmgr = fmgr.getBooleanFormulaManager();
  }

  private String boundVarName(CFANode node, LinearExpression template) {
    return String.format(BOUND_VAR_NAME, node, template);
  }

  private RationalFormula getBoundVar(CFANode node, LinearExpression template) {
    String varName = boundVarName(node, template);
    return rfmgr.makeVariable(varName);
  }

  /**
   * Convert a value determination problem into a single formula.
   *
   * @param policy Selected policy
   * @return {@link org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula} representing the CFA subset.
   * @throws CPATransferException
   * @throws InterruptedException
   */
  public ValueDeterminationConstraint valueDeterminationFormula(
      final Map<CFANode, Map<LinearExpression, CFAEdge>> policy
      ) throws CPATransferException, InterruptedException{

    List<BooleanFormula> constraints = new LinkedList<>();

    // Record the SSA mapping.
    Table<CFANode, LinearExpression, Integer> OutSSAMap = HashBasedTable.create();

    int globalSSACounter = 1;

    for (CFANode node : policy.keySet()) {
      for (Map.Entry<LinearExpression, CFAEdge> incoming : policy.get(node).entrySet()) {

        CFAEdge edge = incoming.getValue();

        PathFormula edgeFormula = pathFormulaWithCustomIdx(edge, globalSSACounter);

        // Add the constraint from the edge itself.
        constraints.add(edgeFormula.getFormula());

        // Add constraints from the incoming variables,
        // of the form
        //    expr <= <incoming_node_bound_for_expr>
        // Note that the incoming variables the SSA index corresponds to the
        // [globalSSACounter].
        SSAMap incomingMap = SSAMap.emptySSAMap().withDefault(globalSSACounter);
        CFANode fromNode = edge.getPredecessor();
        for (Map.Entry<LinearExpression, CFAEdge>
            fromPolicy : policy.get(fromNode).entrySet()) {
          LinearExpression fromTemplate = fromPolicy.getKey();

          RationalFormula fromTemplateVar = getBoundVar(fromNode, fromTemplate);

          RationalFormula fromTemplateF =
              linearExpressionToFormula(fromTemplate, incomingMap);
          BooleanFormula f = rfmgr.lessOrEquals(fromTemplateF, fromTemplateVar);
          constraints.add(f);
        }

        // Add constraints on the outgoing variables.
        //    template = <outgoing_expression_equal_to_template>.
        LinearExpression template = incoming.getKey();
        RationalFormula boundExpression = linearExpressionToFormula(
            template, edgeFormula.getSsa());
        BooleanFormula f = rfmgr.equal(getBoundVar(node, template), boundExpression);
        constraints.add(f);

        OutSSAMap.put(node, incoming.getKey(), globalSSACounter);

        // Incrementing a counter by two guarantees a lack of collisions.
        // NOTE: there has to be a better way.
        globalSSACounter += 2;
      }
    }

    return new ValueDeterminationConstraint(bfmgr.and(constraints), OutSSAMap);
  }

  /**
   * Wait WTF does it belong here?..
   * @param expr
   * @param ssa
   * @return
   */
  RationalFormula linearExpressionToFormula(LinearExpression expr, SSAMap ssa) {
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

  public PathFormula pathFormulaWithSSA(SSAMap map) {
    PathFormula empty = pfmgr.makeEmptyPathFormula();
    return pfmgr.makeNewPathFormula(
        empty,
        map
    );
  }

  /**
   * Creates a {@link PathFormula} with SSA indexing starting
   * from the specified value.
   * Useful for more fine-grained control over SSA indexes.
   */
  public PathFormula pathFormulaWithCustomIdx(CFAEdge edge, int ssaIdx)
      throws CPATransferException, InterruptedException {
    PathFormula empty = pfmgr.makeEmptyPathFormula();
    PathFormula emptyWithCustomSSA = pfmgr.makeNewPathFormula(
        empty,
        SSAMap.emptySSAMap().withDefault(ssaIdx));

    return pfmgr.makeAnd(emptyWithCustomSSA, edge);
  }
}
