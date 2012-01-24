/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.conditions.path;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingElement;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

/**
 * A {@link PathCondition} where the condition is based on the number of assignments (per identifier)
 * seen so far on the current path.
 */
@Options(prefix="cpa.conditions.path.assignments")
public class AssignmentsInPathCondition implements PathCondition, Statistics {

  @Option(description="maximum number of assignments (-1 for infinite)",
      name="limit")
  @IntegerOption(min=-1)
  private int threshold = -1;

  public AssignmentsInPathCondition(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public AvoidanceReportingElement getInitialElement(CFANode node) {
    return new AssignmentsInPathConditionElement();
  }

  @Override
  public AvoidanceReportingElement getAbstractSuccessor(AbstractElement element, CFAEdge edge) {
    AssignmentsInPathConditionElement currentElement = (AssignmentsInPathConditionElement)element;

    if(edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      StatementEdge statementEdge = (StatementEdge)edge;

      IASTStatement statement = statementEdge.getStatement();
      if(statement instanceof IASTAssignment) {
        IASTExpression leftHandSide = ((IASTAssignment)statement).getLeftHandSide();

        String assignedVariable = getScopedVariableName(leftHandSide, edge);
        if(assignedVariable != null) {
          return currentElement.getSuccessor(assignedVariable);
        }
      }
    }

    return currentElement;
  }

  /**
   * This method returns the scoped name of the expression (either an identifier or a field reference) which is being assigned.
   *
   * @param expression the left hand side expression of an assignment
   * @param edge the cfa edge
   * @return the scoped name of the assigned variable, or null, if neither an identifier nor a field reference where assigned
   */
  private String getScopedVariableName(IASTExpression expression, CFAEdge edge) {
    String scope = "";

    if(!isGlobalIdentifier(expression))
      scope = edge.getPredecessor().getFunctionName() + "::";

    if(expression instanceof IASTIdExpression
        || expression instanceof IASTFieldReference) {
      return scope + expression.toASTString();
    }

    return null;
  }

  /**
   * This method determines if the given expression references a global identifier.
   *
   * @param expression the expression in question
   * @return true, if the given expression references a global identifier, else false
   */
  private boolean isGlobalIdentifier(IASTExpression expression) {
    if(expression instanceof IASTIdExpression) {
      IASTIdExpression identifier       = (IASTIdExpression)expression;
      IASTSimpleDeclaration declaration = identifier.getDeclaration();

      if(declaration instanceof IASTDeclaration) {
        return ((IASTDeclaration)declaration).isGlobal();
      }
    }

    return false;
  }

  @Override
  public boolean adjustPrecision() {
    threshold *= 2;
    return true;
  }

  @Override
  public String getName() {
    return "Assignments in path condition";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reachedSet) {
    out.println("Threshold value: " + threshold);
  }


  public class AssignmentsInPathConditionElement implements AbstractElement, AvoidanceReportingElement {

    /**
     * the mapping from variable name to the number of assignments to this variable
     */
    private HashMap<String, Integer> mapping = new HashMap<String, Integer>();

    /**
     * the maximal number of assignments over all variables
     */
    private Integer maximum = 0;

    /**
     * This method creates the successor of the current element, based on the given assigned variable.
     *
     * @param assignedVariable the name of the assigned variable
     * @return the successor of the current element, based on the given assigned variable
     */
    public AssignmentsInPathConditionElement getSuccessor(String assignedVariable) {
      AssignmentsInPathConditionElement successor = new AssignmentsInPathConditionElement();

      Integer currentValue = mapping.containsKey(assignedVariable) ? mapping.get(assignedVariable) + 1 : 1;
      successor.mapping = new HashMap<String, Integer>(mapping);
      successor.mapping.put(assignedVariable, currentValue);

      maximum = Math.max(maximum, currentValue);

      return successor;
    }

    /**
     * This method returns the maximal number of assignments over all variables.
     *
     * @return the maximal amount of assignments over all variables
     */
    public Integer getMaximum() {
      return maximum;
    }

    /**
     * This method decides if the number of assignments for the given variable exceeds the given threshold.
     *
     * @param variableName the variable to check
     * @param threshold the threshold to check
     * @return true, if the number of assignments for the given variable exceeds the given threshold, else false
     */
    public boolean variableExceedsThreshold(String variableName, Integer threshold) {
      return mapping.containsKey(variableName) && mapping.get(variableName) >= threshold;
    }

    @Override
    public boolean mustDumpAssumptionForAvoidance() {
      return maximum > AssignmentsInPathCondition.this.threshold;
    }

    @Override
    public Formula getReasonFormula(FormulaManager formuaManager) {
      String formula = HeuristicToFormula.getFormulaStringForHeuristic(PreventingHeuristicType.ASSIGNMENTSINPATH, maximum);

      return formuaManager.parse(formula);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();

      for(Map.Entry<String, Integer> entry : mapping.entrySet()) {
        builder.append(entry.getKey());
        builder.append(" => ");
        builder.append(entry.getValue());
        builder.append("\n");
      }

      return builder.toString();
    }
  }
}
