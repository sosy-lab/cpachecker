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
package org.sosy_lab.cpachecker.cpa.conditions.path;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

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
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.Value;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
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
  public AvoidanceReportingState getAbstractSuccessor(AbstractState pElement, CFAEdge pEdge) {
    UniqueAssignmentsInPathConditionState current   = (UniqueAssignmentsInPathConditionState)pElement;

    UniqueAssignmentsInPathConditionState successor = new UniqueAssignmentsInPathConditionState(current.maximum,
        HashMultimap.create(current.mapping));

    maxNumberOfAssignments  = Math.max(maxNumberOfAssignments, successor.getMaximum());

    return successor;
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
    private int maximum;

    private UniqueAssignmentsInPathConditionState() {
      this(0, HashMultimap.<MemoryLocation, Value>create());
    }

    public UniqueAssignmentsInPathConditionState(int pMaximum, Multimap<MemoryLocation, Value> pMapping) {
      maximum = pMaximum;
      mapping = pMapping;
    }

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
    * This method decides if the number of assignments for the given variable would exceed the soft threshold, taking
    * into account the current assignment from the given explicit-value state.
    *
    * @param state the current assignment in form of a explicit-value state
    * @param memoryLocation the variable to check
    * @return true, if the number of assignments for the given variable would exceed the soft threshold, else false
    */
    public boolean wouldExceedSoftThreshold(ValueAnalysisState state, MemoryLocation memoryLocation) {
      if(softThreshold == -1) {
        return false;
      }

      Set<Value> values = new HashSet<>(mapping.get(memoryLocation));
      values.add(state.getValueFor(memoryLocation));

      return values.size() > softThreshold;
    }

    /**
    * This method decides if the number of assignments for the given memory location exceeds the soft threshold.
    *
    * @param memoryLocation the memory location to check
    * @return true, if the number of assignments for the given memory location exceeds the soft threshold, else false
    */
    public boolean exceedsSoftThreshold(MemoryLocation memoryLocation) {
      return softThreshold > -1
          && mapping.containsKey(memoryLocation)
          && mapping.get(memoryLocation).size() > softThreshold;
    }

    /**
     * the mapping from variable name to the set of assigned values to this variable
     */
    private Multimap<MemoryLocation, Value> mapping = HashMultimap.create();

    /*
    * This method decides if the number of assignments for the given variable would exceed the hard threshold, taking
    * into account the current assignment from the given explicit-value state.
    *
    * @param state the current assignment in form of a explicit-value state
    * @param memoryLocation the variable to check
    * @return true, if the number of assignments for the given variable would exceed the hard threshold, else false
    */
    public boolean wouldExceedHardThreshold(ValueAnalysisState state, MemoryLocation memoryLocation) {
      if(hardThreshold == -1) {
        return false;
      }

      Set<Value> values = new HashSet<>(mapping.get(memoryLocation));
      values.add(state.getValueFor(memoryLocation));

      return values.size() > hardThreshold;
    }

    /**
    * This method decides if the number of assignments for the given memory location exceeds the hard threshold.
    *
    * @param memoryLocation the memory location to check
    * @return true, if the number of assignments for the given memory location exceeds the hard threshold, else false
    */
    public boolean exceedsHardThreshold(MemoryLocation memoryLocation) {
      return hardThreshold > -1
          && mapping.containsKey(memoryLocation)
          && mapping.get(memoryLocation).size() > hardThreshold;
    }

    /**
     * This method updates this state's unique assignment information with an assignment of the given memory location.
     *
     * @param memoryLocation the memory location for which to set assignment information
     */
    public void updateAssignmentInformation(MemoryLocation memoryLocation, Value value) {
      mapping.put(memoryLocation, value);
      maximum = Math.max(maximum, mapping.get(memoryLocation).size());
    }

    @Override
    public String toString() {
      return mapping.toString() + " [max: " + maximum + "]";
    }
  }
}