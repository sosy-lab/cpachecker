// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.sampling.Sample.SampleClass;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

/** Helper class containing sampling-related utility methods. */
public class SampleUtils {

  public static Iterable<ValueAssignment> getAssignmentsWithLowestIndices(
      Iterable<ValueAssignment> model) {
    Map<String, ValueAssignment> assignmentsWithLowestIndizes = new HashMap<>();
    Splitter indexSplitter = Splitter.on("@");
    for (ValueAssignment assignment : model) {
      List<String> parts = indexSplitter.splitToList(assignment.getName());
      String qualifiedName = parts.get(0);
      if (assignmentsWithLowestIndizes.containsKey(qualifiedName)) {
        ValueAssignment current = assignmentsWithLowestIndizes.get(qualifiedName);
        int currentIndex = Integer.parseInt(indexSplitter.splitToList(current.getName()).get(1));
        int newIndex = Integer.parseInt(parts.get(1));
        if (currentIndex <= newIndex) {
          continue;
        }
      }
      assignmentsWithLowestIndizes.put(qualifiedName, assignment);
    }
    return assignmentsWithLowestIndizes.values();
  }

  /**
   * Filter the given model for relevant assignments. Relevant for sampling are only the most recent
   * variable assignments to variables in the current function.
   */
  public static Iterable<ValueAssignment> getRelevantAssignments(
      Iterable<ValueAssignment> model, CFANode pLocation) {
    Map<String, Integer> highestIndizes = new HashMap<>();
    Map<String, ValueAssignment> latestAssignments = new HashMap<>();
    for (ValueAssignment assignment : model) {
      List<String> parts = Splitter.on("@").splitToList(assignment.getName());
      String qualifiedVar = parts.get(0);
      if (!qualifiedVar.contains("::")) {
        // Current assignment is a function result
        // TODO: Can this really not be an unqualified variable?
        continue;
      }
      List<String> functionAndName = Splitter.on("::").splitToList(qualifiedVar);
      assert functionAndName.size() == 2;
      String function = functionAndName.get(0);
      if (!function.equals(pLocation.getFunctionName())) {
        continue;
      }
      String variableName = functionAndName.get(1);
      if (variableName.contains("__CPAchecker_TMP")) {
        continue;
      }
      Integer index = Integer.valueOf(parts.get(1));
      if (index < highestIndizes.getOrDefault(qualifiedVar, 0)) {
        // We are interested in the most recent values of each variable
        continue;
      }
      highestIndizes.put(qualifiedVar, index);
      latestAssignments.put(qualifiedVar, assignment);
    }
    return latestAssignments.values();
  }

  public static Sample extractSampleFromModel(
      Iterable<ValueAssignment> assignments, CFANode location, SampleClass sampleClass) {
    Map<MemoryLocation, ValueAndType> variableValues = new HashMap<>();
    for (ValueAssignment assignment : assignments) {
      List<String> parts = Splitter.on("@").splitToList(assignment.getName());
      MemoryLocation var = MemoryLocation.fromQualifiedName(parts.get(0));

      Object value = assignment.getValue();
      ValueAndType valueAndType;
      if (value instanceof BigInteger) {
        // TODO: Derive type from variable (for now we assume int)
        Number number = ((BigInteger) value).intValue();
        valueAndType = new ValueAndType(new NumericValue(number), CNumericTypes.INT);
      } else {
        throw new AssertionError(
            "Unhandled type for value assignment: " + assignment.getValue().getClass());
      }
      variableValues.put(var, valueAndType);
    }
    return new Sample(variableValues, location, null, sampleClass);
  }

  public static Sample extractSampleFromRelevantAssignments(
      Iterable<ValueAssignment> pAssignments, CFANode pLocation, SampleClass pSampleClass) {
    Iterable<ValueAssignment> relevantAssignments = getRelevantAssignments(pAssignments, pLocation);
    return extractSampleFromModel(relevantAssignments, pLocation, pSampleClass);
  }

  public static Map<MemoryLocation, ValueAndType> getValuesAndTypesFromAbstractState(
      AbstractState pState, Set<MemoryLocation> relevantVariables) {
    ValueAnalysisState valueState =
        AbstractStates.extractStateByType(pState, ValueAnalysisState.class);
    if (valueState == null) {
      return ImmutableSortedMap.of();
    }

    ImmutableSortedMap.Builder<MemoryLocation, ValueAndType> builder =
        ImmutableSortedMap.naturalOrder();
    for (MemoryLocation memoryLocation : valueState.createInterpolant().getMemoryLocations()) {
      if (relevantVariables.contains(memoryLocation)) {
        builder.put(memoryLocation, valueState.getValueAndTypeFor(memoryLocation));
      }
    }
    Map<MemoryLocation, ValueAndType> result = builder.buildOrThrow();
    return result;
  }

  public static AbstractState makeInitialStateFromSample(
      ConfigurableProgramAnalysis pCpa, Sample pSample) throws InterruptedException {
    AbstractState initialState =
        pCpa.getInitialState(pSample.getLocation(), StateSpacePartition.getDefaultPartition());

    // Initialize value analysis with values from sample
    ValueAnalysisState valueState =
        AbstractStates.extractStateByType(initialState, ValueAnalysisState.class);
    for (Entry<MemoryLocation, ValueAndType> assignment : pSample.getVariableValues().entrySet()) {
      MemoryLocation variable = assignment.getKey();
      ValueAndType valueAndType = assignment.getValue();
      valueState.assignConstant(variable, valueAndType.getValue(), valueAndType.getType());
    }

    return initialState;
  }

  public static Precision makeInitialPrecisionFromSample(
      ConfigurableProgramAnalysis pCpa, Sample pSample) throws InterruptedException {
    // Initialize precision of value analysis
    ValueAnalysisCPA valueCPA = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    if (valueCPA != null) {
      Multimap<CFANode, MemoryLocation> valuePrecisionIncrement = HashMultimap.create();
      for (MemoryLocation variable : pSample.getVariableValues().keySet()) {
        valuePrecisionIncrement.put(pSample.getLocation(), variable);
      }
      valueCPA.incrementPrecision(valuePrecisionIncrement);
    }

    return pCpa.getInitialPrecision(
        pSample.getLocation(), StateSpacePartition.getDefaultPartition());
  }

  public static FileLocation getLocationForNode(CFANode pNode) {
    Set<FileLocation> fileLocations;

    if (pNode.getNumLeavingEdges() > 0) {
      fileLocations =
          CFAUtils.leavingEdges(pNode)
              .transform(CFAEdge::getFileLocation)
              .filter(fl -> fl != null && !FileLocation.DUMMY.equals(fl))
              .toSet();
      assert fileLocations.size() < 2 : "Node location is ambiguous";
      if (!fileLocations.isEmpty()) {
        return Iterables.getOnlyElement(fileLocations);
      }
    }

    // No leaving edges or all leaving edges are missing location information
    fileLocations =
        CFAUtils.enteringEdges(pNode)
            .transform(CFAEdge::getFileLocation)
            .filter(fl -> fl != null && !FileLocation.DUMMY.equals(fl))
            .toSet();
    assert fileLocations.size() < 2 : "Node location is ambiguous";
    assert fileLocations.size() == 1 : "All edges are missing location information";
    return Iterables.getOnlyElement(fileLocations);
  }
}
