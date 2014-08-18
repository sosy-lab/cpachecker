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
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

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

  private static final String BOUND_VAR_NAME = "BOUND_[%s]_[%s]";

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

      logger.log(Level.FINE, "Creating value-determination formula for node "
          + toNode);

      for (Entry<LinearExpression, CFAEdge> incoming : policy.get(toNode).entrySet()) {

        LinearExpression template = incoming.getKey();
        CFAEdge incomingEdge = incoming.getValue();

        String templatePrefix = String.format("tmpl_[%s]_", template);

        CFANode fromNode = incomingEdge.getPredecessor();
        int toNodeNo = toNode.getNodeNumber();
        int toNodePrimeNo = toPrime(toNodeNo);

        // well I don't even get the distinction between
        // the variable-is-in-the-edge vs. variable-not-in-the-edge.
        PathFormula edgePathFormula = pathFormulaWithCustomIdx(
            incomingEdge,
            toNodeNo,
            toNodePrimeNo,
            templatePrefix
        );
        BooleanFormula edgeFormula = edgePathFormula.getFormula();

        if (!edgeFormula.equals(bfmgr.makeBoolean(true))) {
          constraints.add(edgeFormula);
        }
        logger.log(Level.FINE, "Adding edge formula: " + edgeFormula);

        if (policy.get(fromNode) == null) {
          // NOTE: nodes with no templates aren't in the policy.
          continue;
        }

        logger.log(Level.FINE, "Template set = " +
            policy.get(fromNode).keySet()
        );

        for (LinearExpression fromTemplate : policy.get(fromNode).keySet()) {

          // Add input constraints on the edge variables.
          NumeralFormula edgeInput = lcmgr.linearExpressionToFormula(
              fromTemplate,
              SSAMap.withDefault(toNodeNo),
              templatePrefix
          );

          BooleanFormula f = rfmgr.lessOrEquals(
              edgeInput,
              rfmgr.makeVariable(absDomainVarName(fromNode, fromTemplate))
          );

          constraints.add(f);
          logger.log(Level.FINE, "Adding abstract-domain constraint: "
              + f);
        }

        // if the variable does not appear in the expression i should do
        // something else.
        // however, with [getSsa()] we never know whether it does or does
        // not appear in the expression

        NumeralFormula outExpr = lcmgr.linearExpressionToFormula(
            template, edgePathFormula.getSsa(), templatePrefix
        );

        NumeralFormula out = rfmgr.makeVariable(absDomainVarName(
            toNode, template));

        BooleanFormula outConstraint = rfmgr.equal(
            outExpr, out
        );
        logger.log(Level.FINE, "Output constraint = " + outConstraint);
        constraints.add(outConstraint);


        outSSAMap.put(toNode, template, out);
      }
    }
    return new ValueDeterminationConstraint(bfmgr.and(constraints), outSSAMap);
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
      CFAEdge edge, int startIdx, int stopIdx, String customPrefix)
      throws CPATransferException, InterruptedException {

    PathFormula p = pathFormulaWithCustomStartIdx(edge, startIdx);
    SSAMap customFromIdxSSAMap = p.getSsa();

    SSAMap.SSAMapBuilder newMapBuilder = customFromIdxSSAMap.builder();

    /**
     * the issue is simple: SSA map is updated twice, hence only the latest
     * update is contained.
     *
     * basically, what I want is something else: container of a meta-information
     * about the formula.
     *
     * Is it possible to add one without changing everything all over the
     * place? a hacky alternative is to just walk a formula tree.
     */

    List<Formula> fromVars = new LinkedList<>();
    List<Formula> toVars = new LinkedList<>();

    logger.log(Level.FINE, "SSA map = " + p.getSsa() + " edge formula = "
        + p.getFormula());

    for (Entry<String, CType> entry :
                              customFromIdxSSAMap.allVariablesWithTypes()) {

      String variable = entry.getKey();
      CType type = entry.getValue();

      int oldIdx = customFromIdxSSAMap.getIndex(variable);

      int newIdx;
      if (oldIdx != startIdx) {
        newIdx = stopIdx;

        // TODO: less hacky way?
        // We are using the assumption that if the start index appears then the
        // old index MAY appear as well.
        // If it does not appear, it's fine as well.
        fromVars.add(
            rfmgr.makeVariable(formulaVarName(variable, startIdx, ""))
        );
        toVars.add(
            rfmgr.makeVariable(formulaVarName(variable, startIdx, customPrefix))
        );

      } else {
        newIdx = oldIdx;
      }

      newMapBuilder.setIndex(variable, type, newIdx);

      fromVars.add(
          rfmgr.makeVariable(formulaVarName(variable, oldIdx, ""))
      );

      toVars.add(
          rfmgr.makeVariable(
              formulaVarName(variable, newIdx, customPrefix)
          )
      );
    }

    BooleanFormula innerFormula = formulaManager.getUnsafeFormulaManager().substitute(
        p.getFormula(), fromVars, toVars
    );

    return new PathFormula(
        innerFormula,
        newMapBuilder.build(),
        p.getPointerTargetSet(),
        p.getLength());
  }

  private String formulaVarName(String variable, int idx, String namespace)
  {
    return String.format(
        "%s%s", namespace, FormulaManagerView.makeName(variable, idx)
    );
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
    return Math.max(
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

  private String absDomainVarName(CFANode node, LinearExpression template) {
    return String.format(BOUND_VAR_NAME, node.getNodeNumber(), template);
  }
}
