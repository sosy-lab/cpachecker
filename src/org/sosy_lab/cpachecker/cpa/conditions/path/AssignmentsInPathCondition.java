/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A {@link PathCondition} where the condition is based on the number of assignments (per identifier)
 * seen so far on the current path.
 */
@Options(prefix="cpa.conditions.path.assignments")
public class AssignmentsInPathCondition implements PathCondition, Statistics {

  @Option(description="(soft) threshold for assignments (-1 for infinite)")
  @IntegerOption(min=-1)
  private int softThreshold = -1;

  @Option(description="(hard) threshold for assignments (-1 for infinite)")
  @IntegerOption(min=-1)
  private int hardThreshold = -1;

  /**
   * the reference to the current element
   */
  private UniqueAssignmentsInPathConditionState currentState = null;

  /**
   * the maximal number of assignments over all variables for all elements seen to far
   */
  private int maxNumberOfAssignments = 0;

  public AssignmentsInPathCondition(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public AvoidanceReportingState getInitialState(CFANode node) {
    return new UniqueAssignmentsInPathConditionState();
  }

  @Override
  public AvoidanceReportingState getAbstractSuccessor(AbstractState element, CFAEdge edge) {
    UniqueAssignmentsInPathConditionState successor = new UniqueAssignmentsInPathConditionState();
    successor.mapping = ((UniqueAssignmentsInPathConditionState)element).mapping;
    successor.maximum = ((UniqueAssignmentsInPathConditionState)element).maximum;

    currentState            = successor;
    maxNumberOfAssignments  = Math.max(maxNumberOfAssignments, currentState.getMaximum());

    return currentState;
  }

  @Override
  public boolean adjustPrecision() {
    softThreshold *= 2;
    hardThreshold *= 2;
    return true;
  }

  @Override
  public String getName() {
    return "unique assignments in path condition";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reachedSet) {
    out.println("Threshold value (soft):     " + softThreshold);
    out.println("Threshold value (hard):     " + hardThreshold);
    out.println("Max. number of assignments: " + maxNumberOfAssignments);

  }

  public class UniqueAssignmentsInPathConditionState implements AbstractState, AvoidanceReportingState {
    /**
     * the maximal number of assignments over all variables
     */
    private int maximum = 0;

    /**
     * the mapping from variable name to the set of assigned values to this variable
     */
    private Multimap<String, Long> mapping = HashMultimap.create();

    /**
     * This method returns the maximal number of assignments over all variables.
     *
     * @return the maximal amount of assignments over all variables
     */
    public int getMaximum() {
      return maximum;
    }

    @Override
    public BooleanFormula getReasonFormula(FormulaManagerView formulaManager) {
      return PreventingHeuristic.ASSIGNMENTSINPATH.getFormula(formulaManager, maximum);
    }

    @Override
    public boolean mustDumpAssumptionForAvoidance() {
      return softThreshold != -1 && maximum > AssignmentsInPathCondition.this.softThreshold;
    }

    /**
    * This method decides if the number of assignments for the given variable exceeds the soft threshold.
    *
    * @param variableName the variable to check
    * @return true, if the number of assignments for the given variable exceeds the soft threshold, else false
    */
    public boolean variableExceedsSoftThreshold(String variableName) {
      return softThreshold > -1
          && mapping.containsKey(variableName)
          && mapping.get(variableName).size() > softThreshold;
    }

    /**
    * This method decides if the number of assignments for the given variable exceeds the hard threshold.
    *
    * @param variableName the variable to check
    * @return true, if the number of assignments for the given variable exceeds the hard threshold, else false
    */
    public boolean variableExceedsHardThreshold(String variableName) {
      return hardThreshold > -1
          && mapping.containsKey(variableName)
          && mapping.get(variableName).size() > hardThreshold;
    }

    /**
     * This method updates the unique assignment information stored in this state with assignment information from an
     * explicit-value state. So, this method basically does what strengthen would do.
     *
     * @param explicitValueState the ExplicitState from which to obtain assignment information
     */
    public void updateAssignmentInformation(ExplicitState explicitValueState) {
      Multimap<String, Long> newMapping = HashMultimap.create(mapping);
      explicitValueState.addToValueMapping(newMapping);

      if(!mapping.equals(newMapping)) {
        currentState.mapping = newMapping;
        currentState.maximum = maximum;

        for (String variableName : currentState.mapping.keys()) {
          currentState.maximum = Math.max(currentState.maximum, currentState.mapping.get(variableName).size());
        }
      }
    }

    @Override
    public String toString() {
      return currentState.mapping.toString() + " [" + currentState.maximum + "]";
    }
  }
}
