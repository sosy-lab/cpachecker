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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
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
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitElement;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A {@link PathCondition} where the condition is based on the number of assignments (per identifier)
 * seen so far on the current path.
 */
@Options(prefix="cpa.conditions.path.assignments")
public class AssignmentsInPathCondition implements PathCondition, Statistics {

  @Option(description="maximum number of assignments (-1 for infinite)")
  @IntegerOption(min=-1)
  private int threshold = -1;

  @Option(description="whether or not to track unique assignments only")
  private boolean demandUniqueness = true;

  private int max = 0;

  private AssignmentsInPathConditionElement currentElement = null;

  private Map<String, Integer> assignmentsPerIdentifier = new HashMap<String, Integer>();

  LogManager logger;

  @Option(name = "extendedStatsFile",
      description = "file name where to put the extended stats file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File extendedStatsFile;

  public AssignmentsInPathCondition(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);

    this.logger = logger;
  }

  @Override
  public AvoidanceReportingElement getInitialElement(CFANode node) {

    AvoidanceReportingElement element = demandUniqueness ? new UniqueAssignmentsInPathConditionElement() : new AllAssignmentsInPathConditionElement();

    return element;
  }

  @Override
  public AvoidanceReportingElement getAbstractSuccessor(AbstractElement element, CFAEdge edge) {
    currentElement = (AssignmentsInPathConditionElement)element;

    if(edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      StatementEdge statementEdge = (StatementEdge)edge;

      IASTStatement statement = statementEdge.getStatement();
      if(statement instanceof IASTAssignment) {
        IASTExpression leftHandSide = ((IASTAssignment)statement).getLeftHandSide();

        String assignedVariable = getScopedVariableName(leftHandSide, edge);
        if(assignedVariable != null) {
          currentElement = currentElement.getSuccessor(assignedVariable);
        }
      }
    }

    max = Math.max(max, currentElement.maximum);

    for(Map.Entry<String, Integer> assignment : currentElement.getAssignmentCounts().entrySet()) {
      String variableName     = assignment.getKey();
      Integer currentCounter  = assignmentsPerIdentifier.containsKey(variableName) ? assignmentsPerIdentifier.get(variableName) : 0;

      currentCounter          = Math.max(currentCounter, assignment.getValue());

      assignmentsPerIdentifier.put(variableName, currentCounter);
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
    return (demandUniqueness ? "unique" : "all") + " assignments in path condition";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reachedSet) {
    out.println("Threshold value: " + threshold);
    out.println("max. number of assignments: " + max);

    if(extendedStatsFile != null) {
      try {
          StringBuilder builder = new StringBuilder();
          String newline        = System.getProperty("line.separator");

          builder.append("total numer of variable assignments of last element:");
          builder.append(newline);
          builder.append(currentElement);

          builder.append(newline);
          builder.append("max. total numer of variable assignments over all elements:");
          for(Map.Entry<String, Integer> assignment : assignmentsPerIdentifier.entrySet()) {
            builder.append(newline);
            builder.append(assignment.getKey());
            builder.append(" -> ");
            builder.append("#");
            builder.append(assignment.getValue());
        }
        Files.writeFile(extendedStatsFile, builder.toString());

      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write extended statistics to file");
      }
    }
  }

  abstract public class AssignmentsInPathConditionElement implements AbstractElement, AvoidanceReportingElement {
    /**
     * the maximal number of assignments over all variables
     */
    protected Integer maximum = 0;

    /**
     * This method creates the successor of the current element, based on the given assigned variable.
     *
     * @param assignedVariable the name of the assigned variable
     * @return the successor of the current element, based on the given assigned variable
     */
    abstract public AssignmentsInPathConditionElement getSuccessor(String assignedVariable);

    /**
     * This method returns the maximal number of assignments over all variables.
     *
     * @return the maximal amount of assignments over all variables
     */
    public Integer getMaximum() {
      return maximum;
    }

    @Override
    public Formula getReasonFormula(FormulaManager formuaManager) {
      String formula = HeuristicToFormula.getFormulaStringForHeuristic(PreventingHeuristicType.ASSIGNMENTSINPATH, maximum);
      return formuaManager.parse(formula);
    }

    @Override
    public boolean mustDumpAssumptionForAvoidance() {
      return threshold != -1 && maximum > AssignmentsInPathCondition.this.threshold;
    }

    /**
    * This method decides if the number of assignments for the given variable exceeds the given limit.
    *
    * Note, this method maybe used to check against an arbitrary limit, and must not be associated to the threshold in any form.
    *
    * @param variableName the variable to check
    * @param limit the limit to check
    * @return true, if the number of assignments for the given variable exceeds the given threshold, else false
    */
    abstract public boolean variableExceedsGivenLimit(String variableName, Integer limit);

    abstract public Map<String, Integer> getAssignmentCounts();

    @Override
    public String toString() {
      String newline = System.getProperty("line.separator");
      StringBuilder builder = new StringBuilder();

      for(Map.Entry<String, Integer> assignment : getAssignmentCounts().entrySet()) {
        builder.append(assignment.getKey());
        builder.append(" -> ");
        builder.append("#");
        builder.append(assignment.getValue());
        builder.append(newline);
      }

      return builder.toString();
    }
  }

  public class AllAssignmentsInPathConditionElement extends AssignmentsInPathConditionElement {

    /**
     * the mapping from variable name to the number of assignments to this variable
     */
    private HashMap<String, Integer> mapping = new HashMap<String, Integer>();

    /**
     * default constructor for creating the initial element
     */
    public AllAssignmentsInPathConditionElement() {}

    /**
     * copy constructor for successor computation
     *
     * @param original the original element to be copied
     */
    private AllAssignmentsInPathConditionElement(AllAssignmentsInPathConditionElement original) {
      mapping = new HashMap<String, Integer>(original.mapping);
      maximum = original.maximum;
    }

    @Override
    public AllAssignmentsInPathConditionElement getSuccessor(String assignedVariable) {
      // create a copy ...
      AllAssignmentsInPathConditionElement successor = new AllAssignmentsInPathConditionElement(this);

      // ... and update the mapping at the maximum
      Integer numberOfAssignments = mapping.containsKey(assignedVariable) ? mapping.get(assignedVariable) + 1 : 1;
      successor.mapping.put(assignedVariable, numberOfAssignments);

      successor.maximum = Math.max(successor.maximum, numberOfAssignments);

      return successor;
    }

    @Override
    public boolean variableExceedsGivenLimit(String variableName, Integer limit) {
      return mapping.containsKey(variableName) && mapping.get(variableName) >= limit;
    }

    @Override
    public Map<String, Integer> getAssignmentCounts() {
      return Collections.unmodifiableMap(mapping);
    }
  }

  public class UniqueAssignmentsInPathConditionElement extends AssignmentsInPathConditionElement {

    /**
     * the mapping from variable name to the set of assigned values to this variable
     */
    private Multimap<String, Long> mapping = HashMultimap.create();

    /**
     * the name of the variable that is being assigned
     */
    private String assignedVariable = null;

    /**
     * default constructor for creating the initial element
     */
    public UniqueAssignmentsInPathConditionElement() {}

    /**
     * copy constructor for successor computation
     *
     * @param original the original element to be copied
     */
    private UniqueAssignmentsInPathConditionElement(UniqueAssignmentsInPathConditionElement original) {
      mapping = HashMultimap.create(original.mapping);
      maximum = original.maximum;
    }

    @Override
    public UniqueAssignmentsInPathConditionElement getSuccessor(String assignedVariable) {
      // create a copy ...
      UniqueAssignmentsInPathConditionElement successor = new UniqueAssignmentsInPathConditionElement(this);

      // ... and set the later to be assigned variable
      successor.assignedVariable  = assignedVariable;

      return successor;
    }

    /**
     * This method adds an assignment for the last assigned variable, if the current value of this assigned variable in the ExplicitElement was not assigned before, i.e. if it is unique.
     *
     * @param element the ExplicitElement from which to query assignment information
     */
    public void addAssignment(ExplicitElement element) {
      if(assignedVariable == null) {
        return;
      }

      if(element.contains(assignedVariable)) {
        Long value = element.getValueFor(assignedVariable);
        if(value != null) {
          mapping.put(assignedVariable, value);

          maximum = Math.max(maximum, mapping.get(assignedVariable).size());
        }
      }
    }

    /**
     * This method decides if the number of assignments for the given variable exceeds the given limit.
     *
     * Note, this method maybe used to check against an arbitrary limit, and must not be associated to the threshold in any form.
     *
     * @param variableName the variable to check
     * @param limit the limit to check
     * @return true, if the number of assignments for the given variable exceeds the given threshold, else false
     */
    @Override
    public boolean variableExceedsGivenLimit(String variableName, Integer limit) {
      return mapping.containsKey(variableName) && mapping.get(variableName).size() >= limit;
    }

    @Override
    public Map<String, Integer> getAssignmentCounts() {
      Map<String, Integer> map = new HashMap<String, Integer>();

      for(String variableName : mapping.keys()) {
        map.put(variableName, mapping.get(variableName).size());
      }

      return Collections.unmodifiableMap(map);
    }
  }
}
