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
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
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

  @Option(name = "extendedStatsFile",
      description = "file name where to put the extended stats file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File extendedStatsFile;

  /**
   * the reference to the current element
   */
  private AssignmentsInPathConditionState currentState = null;

  /**
   * the maximal number of assignments over all variables for all elements seen to far
   */
  private int maxNumberOfAssignments = 0;

  /**
   * the maximal number of assignments for each variables over all elements seen to far
   */
  private Map<String, Integer> maxNumberOfAssignmentsPerIdentifier = new HashMap<String, Integer>();

  /**
   * a reference to the logger
   */
  LogManager logger;

  public AssignmentsInPathCondition(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);

    this.logger = logger;
  }

  @Override
  public AvoidanceReportingState getInitialState(CFANode node) {

    AvoidanceReportingState element = demandUniqueness ? new UniqueAssignmentsInPathConditionState() : new AllAssignmentsInPathConditionState();

    return element;
  }

  @Override
  public AvoidanceReportingState getAbstractSuccessor(AbstractState element, CFAEdge edge) {
    currentState = (AssignmentsInPathConditionState)element;

    if(edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      CStatementEdge statementEdge = (CStatementEdge)edge;

      CStatement statement = statementEdge.getStatement();
      if(statement instanceof CAssignment) {
        CExpression leftHandSide = ((CAssignment)statement).getLeftHandSide();

        String assignedVariable = getScopedVariableName(leftHandSide, edge);
        if(assignedVariable != null) {
          currentState = currentState.getSuccessor(assignedVariable);
        }
      }
    }

    maxNumberOfAssignments = Math.max(maxNumberOfAssignments, currentState.maximum);

    for(Map.Entry<String, Integer> assignment : currentState.getAssignmentCounts().entrySet()) {
      String variableName     = assignment.getKey();
      Integer currentCounter  = maxNumberOfAssignmentsPerIdentifier.containsKey(variableName) ? maxNumberOfAssignmentsPerIdentifier.get(variableName) : 0;

      currentCounter          = Math.max(currentCounter, assignment.getValue());

      maxNumberOfAssignmentsPerIdentifier.put(variableName, currentCounter);
    }

    return currentState;
  }

  /**
   * This method returns the scoped name of the expression (either an identifier or a field reference) which is being assigned.
   *
   * @param expression the left hand side expression of an assignment
   * @param edge the cfa edge
   * @return the scoped name of the assigned variable, or null, if neither an identifier nor a field reference where assigned
   */
  private String getScopedVariableName(CExpression expression, CFAEdge edge) {
    String scope = "";

    if(!isGlobalIdentifier(expression))
      scope = edge.getPredecessor().getFunctionName() + "::";

    if(expression instanceof CIdExpression
        || expression instanceof CFieldReference) {
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
  private boolean isGlobalIdentifier(CExpression expression) {
    if(expression instanceof CIdExpression) {
      CIdExpression identifier       = (CIdExpression)expression;
      CSimpleDeclaration declaration = identifier.getDeclaration();

      if(declaration instanceof CDeclaration) {
        return ((CDeclaration)declaration).isGlobal();
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
    out.println("max. number of assignments: " + maxNumberOfAssignments);

    if(extendedStatsFile != null) {
      writeLogFile();
    }
  }

  private void writeLogFile() {
    try {
      StringBuilder builder = new StringBuilder();

      // log the last element found
      builder.append("total number of variable assignments of last element:");
      builder.append("\n");
      builder.append(currentState);

      // log the max-aggregation
      builder.append("\n");
      builder.append("\n");
      builder.append("max. total number of variable assignments over all elements:");
      builder.append("\n");
      builder.append(assignmentsAsString(maxNumberOfAssignmentsPerIdentifier));

      Files.writeFile(extendedStatsFile, builder.toString());
    } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write extended statistics to file");
    }
  }

  /**
   * This method returns a human-readable representation of an assignment map.
   *
   * @param assignments the assignment map to represent
   * @return a human-readable representation of an assignment map
   */
  private static String assignmentsAsString(Map<String, Integer> assignments) {
    StringBuilder builder = new StringBuilder();

    for(Map.Entry<String, Integer> assignment : assignments.entrySet()) {
      builder.append(assignment.getKey());
      builder.append(" -> ");
      builder.append(assignment.getValue());
      builder.append("\n");
    }

    return builder.toString();
  }

  abstract public class AssignmentsInPathConditionState implements AbstractState, AvoidanceReportingState {
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
    abstract public AssignmentsInPathConditionState getSuccessor(String assignedVariable);

    /**
     * This method returns the maximal number of assignments over all variables.
     *
     * @return the maximal amount of assignments over all variables
     */
    public Integer getMaximum() {
      return maximum;
    }

    @Override
    public Formula getReasonFormula(FormulaManager formulaManager) {
      return PreventingHeuristic.ASSIGNMENTSINPATH.getFormula(formulaManager, maximum);
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

    /**
    * This method returns the current number of assignments for the given variable.
    *
    * @param varaibleName the variable for which to get the assignment count
    * @return the current number of assignments per variable
    */
    abstract public Integer getAssignmentCount(String varaibleName);

    /**
     * This method returns the current number of assignments per variable.
     *
     * @return the current number of assignments per variable
     */
    abstract public Map<String, Integer> getAssignmentCounts();

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();

      return builder.append(assignmentsAsString(getAssignmentCounts())).toString();
    }
  }

  public class AllAssignmentsInPathConditionState extends AssignmentsInPathConditionState {

    /**
     * the mapping from variable name to the number of assignments to this variable
     */
    private HashMap<String, Integer> mapping = new HashMap<String, Integer>();

    /**
     * default constructor for creating the initial element
     */
    public AllAssignmentsInPathConditionState() {}

    /**
     * copy constructor for successor computation
     *
     * @param original the original element to be copied
     */
    private AllAssignmentsInPathConditionState(AllAssignmentsInPathConditionState original) {
      mapping = new HashMap<String, Integer>(original.mapping);
      maximum = original.maximum;
    }

    @Override
    public AllAssignmentsInPathConditionState getSuccessor(String assignedVariable) {
      // create a copy ...
      AllAssignmentsInPathConditionState successor = new AllAssignmentsInPathConditionState(this);

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
    public Integer getAssignmentCount(String variableName) {
      return mapping.get(variableName);
    }

    @Override
    public Map<String, Integer> getAssignmentCounts() {
      return Collections.unmodifiableMap(mapping);
    }
  }

  public class UniqueAssignmentsInPathConditionState extends AssignmentsInPathConditionState {

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
    public UniqueAssignmentsInPathConditionState() {}

    /**
     * copy constructor for successor computation
     *
     * @param original the original element to be copied
     */
    private UniqueAssignmentsInPathConditionState(UniqueAssignmentsInPathConditionState original) {
      mapping = HashMultimap.create(original.mapping);
      maximum = original.maximum;
    }

    @Override
    public UniqueAssignmentsInPathConditionState getSuccessor(String assignedVariable) {
      // create a copy ...
      UniqueAssignmentsInPathConditionState successor = new UniqueAssignmentsInPathConditionState(this);

      // ... and set the later to be assigned variable
      successor.assignedVariable  = assignedVariable;

      return successor;
    }

    /**
     * This method adds an assignment for the last assigned variable, if the current value of this assigned variable in the ExplicitState was not assigned before, i.e. if it is unique.
     *
     * @param element the ExplicitState from which to query assignment information
     */
    public void addAssignment(ExplicitState element) {
      if(assignedVariable == null) {
        return;
      }

      if(element.contains(assignedVariable)) {
        Long value = element.getValueFor(assignedVariable);
        if(value != null) {
          mapping.put(assignedVariable, value);

          maximum = Math.max(maximum, getAssignmentCount(assignedVariable));
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
      return mapping.containsKey(variableName) && getAssignmentCount(variableName) >= limit;
    }

    @Override
    public Integer getAssignmentCount(String variableName) {
      return mapping.get(variableName).size();
    }

    @Override
    public Map<String, Integer> getAssignmentCounts() {
      Map<String, Integer> map = new HashMap<String, Integer>();

      for(String variableName : mapping.keys()) {
        map.put(variableName, getAssignmentCount(variableName));
      }

      return Collections.unmodifiableMap(map);
    }
  }
}
