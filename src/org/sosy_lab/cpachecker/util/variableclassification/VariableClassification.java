// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.util.LoopStructure;

public class VariableClassification implements Serializable {

  private static final long serialVersionUID = 1L;

  private final boolean hasRelevantNonIntAddVars;

  private final Set<String> intBoolVars;
  private final Set<String> intEqualVars;
  private final Set<String> intAddVars;
  private final Set<String> intOverflowVars;

  /**
   * These sets contain all variables even ones of array, pointer or structure types. Such variables
   * cannot be classified even as Int, so they are only kept in these sets in order not to break the
   * classification of Int variables.
   */
  // Initially contains variables used in assumes and assigned to pointer dereferences,
  // then all essential variables (by propagation)
  private final Set<String> relevantVariables;

  private final Set<String> addressedVariables;

  private final Multiset<String> assumedVariables;
  private final Multiset<String> assignedVariables;

  /**
   * Fields information doesn't take any aliasing information into account, fields are considered
   * per type, not per composite instance
   */
  // Initially contains fields used in assumes and assigned to pointer dereferences,
  // then all essential fields (by propagation)
  private final Multimap<CCompositeType, String> relevantFields;

  private final Multimap<CCompositeType, String> addressedFields;

  private final Set<Partition> partitions;
  private final Set<Partition> intBoolPartitions;
  private final Set<Partition> intEqualPartitions;
  private final Set<Partition> intAddPartitions;

  private final Table<CFAEdge, Integer, Partition> edgeToPartitions;

  VariableClassification(
      boolean pHasRelevantNonIntAddVars,
      Set<String> pIntBoolVars,
      Set<String> pIntEqualVars,
      Set<String> pIntAddVars,
      Set<String> pIntOverflowVars,
      Set<String> pRelevantVariables,
      Set<String> pAddressedVariables,
      Multimap<CCompositeType, String> pRelevantFields,
      Multimap<CCompositeType, String> pAddressedFields,
      Collection<Partition> pPartitions,
      Set<Partition> pIntBoolPartitions,
      Set<Partition> pIntEqualPartitions,
      Set<Partition> pIntAddPartitions,
      Table<CFAEdge, Integer, Partition> pEdgeToPartitions,
      Multiset<String> pAssumedVariables,
      Multiset<String> pAssignedVariables) {
    hasRelevantNonIntAddVars = pHasRelevantNonIntAddVars;
    intBoolVars = ImmutableSet.copyOf(pIntBoolVars);
    intEqualVars = ImmutableSet.copyOf(pIntEqualVars);
    intAddVars = ImmutableSet.copyOf(pIntAddVars);
    intOverflowVars = ImmutableSet.copyOf(pIntOverflowVars);
    relevantVariables = ImmutableSet.copyOf(pRelevantVariables);
    addressedVariables = ImmutableSet.copyOf(pAddressedVariables);
    relevantFields = ImmutableSetMultimap.copyOf(pRelevantFields);
    addressedFields = ImmutableSetMultimap.copyOf(pAddressedFields);
    partitions = ImmutableSet.copyOf(pPartitions);
    intBoolPartitions = ImmutableSet.copyOf(pIntBoolPartitions);
    intEqualPartitions = ImmutableSet.copyOf(pIntEqualPartitions);
    intAddPartitions = ImmutableSet.copyOf(pIntAddPartitions);
    edgeToPartitions = ImmutableTable.copyOf(pEdgeToPartitions);
    assumedVariables = ImmutableMultiset.copyOf(pAssumedVariables);
    assignedVariables = ImmutableMultiset.copyOf(pAssignedVariables);
  }

  @VisibleForTesting
  public static VariableClassification empty() {
    return new VariableClassification(
        false,
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableTable.of(),
        ImmutableMultiset.of(),
        ImmutableMultiset.of());
  }

  public boolean hasRelevantNonIntAddVars() {
    return hasRelevantNonIntAddVars;
  }

  /**
   * All variables that may be essential for reachability properties. The variables are returned as
   * a collection of scopedNames.
   *
   * <p><strong> Note: the collection includes all variables, including pointers, arrays and
   * structures, i.e. non-Int variables. </strong>
   */
  public Set<String> getRelevantVariables() {
    return relevantVariables;
  }

  /**
   * All variables that have their addresses taken somewhere in the source code. The variables are
   * returned as a collection of scopedNames.
   *
   * <p><strong> Note: the collection includes all variables, including pointers, arrays and
   * structures, i.e. non-Int variables. </strong>
   */
  public Set<String> getAddressedVariables() {
    return addressedVariables;
  }

  /**
   * All fields that may be essential for reachability properties (only fields accessed explicitly
   * through either dot (.) or arrow (->) operator count).
   *
   * @return A collection of (CCompositeType, fieldName) mappings.
   */
  public Multimap<CCompositeType, String> getRelevantFields() {
    return relevantFields;
  }

  /**
   * All fields that have their addresses taken somewhere in the source code. (only fields accessed
   * explicitly through either dot (.) or arrow (->) operator count).
   *
   * @return A collection of (CCompositeType, fieldName) mappings.
   */
  public Multimap<CCompositeType, String> getAddressedFields() {
    return addressedFields;
  }

  /**
   * This function returns a collection of scoped names. This collection contains all vars, that are
   * boolean, i.e. the value is 0 or 1.
   */
  public Set<String> getIntBoolVars() {
    return intBoolVars;
  }

  /**
   * This function returns a collection of partitions. Each partition contains only boolean vars.
   */
  public Set<Partition> getIntBoolPartitions() {
    return intBoolPartitions;
  }

  /**
   * This function returns a collection of scoped names. This collection contains all vars, that are
   * only assigned or compared for equality with integer values. There are NO mathematical
   * calculations (add, sub, mult) with these vars. This collection does not contain any variable
   * from "IntBool" or "IntAdd".
   */
  public Set<String> getIntEqualVars() {
    return intEqualVars;
  }

  /**
   * This function returns a collection of partitions. Each partition contains only vars, that are
   * only assigned or compared for equality with integer values. This collection does not contains
   * anypartition from "IntBool" or "IntAdd".
   */
  public Set<Partition> getIntEqualPartitions() {
    return intEqualPartitions;
  }

  /**
   * This function returns a collection of scoped names. This collection contains all vars, that are
   * only used in simple calculations (+, -, <, >, <=, >=, ==, !=, &, &&, |, ||, ^). This collection
   * does not contain any variable from "IntBool" or "IntEq".
   */
  public Set<String> getIntAddVars() {
    return intAddVars;
  }

  /**
   * This function returns a collection of scoped names. This collection contains all vars that are
   * used in calculations that can lead to an overflow (+, -, *, /, %, <<). This collection may
   * contain any variable from "IntBool", "IntEq" or "IntAdd".
   */
  public Set<String> getIntOverflowVars() {
    return intOverflowVars;
  }

  /**
   * This function returns a collection of partitions. Each partition contains only vars, that are
   * used in simple calculations. This collection does not contains anypartition from "IntBool" or
   * "IntEq".
   */
  public Set<Partition> getIntAddPartitions() {
    return intAddPartitions;
  }

  /**
   * This function returns a collection of partitions. A partition contains all vars, that are
   * dependent from each other.
   */
  public Set<Partition> getPartitions() {
    return partitions;
  }

  /** This method return all variables (i.e., their qualified name), that occur in an assumption. */
  public Multiset<String> getAssumedVariables() {
    return assumedVariables;
  }

  /**
   * This method return all variables (i.e., their qualified name), that occur as left-hand side in
   * an assignment.
   */
  public Multiset<String> getAssignedVariables() {
    return assignedVariables;
  }

  /**
   * This function returns a partition containing all vars, that are dependent from a given CFAEdge.
   * This method cannot be used for {@link FunctionCallEdge}s, because these have multiple
   * partitions.
   */
  public Partition getPartitionForEdge(CFAEdge edge) {
    checkArgument(
        !(edge instanceof FunctionCallEdge),
        "For FunctionCallEdges, use the specific methods because they have multiple partitions");
    return getPartitionForEdge(edge, 0);
  }

  /**
   * This method returns a partition containing all vars, that are dependent from a specific
   * function parameter at a given edge.
   */
  public Partition getPartitionForParameterOfEdge(FunctionCallEdge edge, int param) {
    Preconditions.checkElementIndex(param, edge.getArguments().size());
    return getPartitionForEdge(edge, param);
  }

  /**
   * This method returns a partition containing all vars, that are dependent from a specific
   * function return value at a given edge.
   */
  public Partition getPartitionForReturnValueOfEdge(FunctionCallEdge edge) {
    return getPartitionForEdge(edge, -1);
  }

  /**
   * This function returns a partition containing all vars, that are dependent from a given CFAedge.
   * The index is 0 for all edges, except functionCalls, where it is the position of the param. For
   * the left-hand-side of the assignment of external functionCalls use -1.
   */
  private Partition getPartitionForEdge(CFAEdge edge, int index) {
    checkNotNull(edge);
    return edgeToPartitions.get(edge, index);
  }

  /**
   * This method computes for a set of variables (qualified names) a score, which serves as rough
   * estimate how expensive tracking it might be to track these variables, e.g. variables with a
   * boolean character have a lower score than variables being used as loop counters.
   *
   * @param variableNames a collection of variables (qualified names)
   * @param loopStructure the loop structure, to identify loop-counter variables
   * @return the score for the given collection of variables
   */
  public int obtainDomainTypeScoreForVariables(
      Collection<String> variableNames,
      Optional<LoopStructure> loopStructure,
      LogManagerWithoutDuplicates logger) {
    final int BOOLEAN_VAR = 1;
    final int INTEQUAL_VAR = 2;
    final int UNKNOWN_VAR = 4;

    checkNotNull(logger);
    checkNotNull(loopStructure);
    if (variableNames.isEmpty()) {
      return UNKNOWN_VAR;
    }

    int newScore = 1;
    for (String variableName : variableNames) {
      int factor = UNKNOWN_VAR;

      if (getIntBoolVars().contains(variableName)) {
        factor = BOOLEAN_VAR;

      } else if (getIntEqualVars().contains(variableName)) {
        factor = INTEQUAL_VAR;
      }

      newScore += factor;

      if (loopStructure.isPresent()
          && loopStructure.orElseThrow().getLoopIncDecVariables().contains(variableName)) {
        return Integer.MAX_VALUE;
      }

      // check for overflow
      if (newScore < 0) {
        logger.logOnce(
            Level.WARNING,
            "Highest possible value reached in score computation."
                + " Error path prefix preference may not be applied reliably.");
        logger.logf(
            Level.FINE,
            "Overflow in score computation happened for variables %s.",
            variableNames.toString());

        return Integer.MAX_VALUE - 1;
      }
    }

    return newScore;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("\nIntBool  " + intBoolVars.size() + "\n    " + intBoolVars);
    str.append("\nIntEq  " + intEqualVars.size() + "\n    " + intEqualVars);
    str.append("\nIntAdd  " + intAddVars.size() + "\n    " + intAddVars);
    str.append("\nIntOverflow  " + intOverflowVars.size() + "\n    " + intOverflowVars);
    return str.toString();
  }

  private Object readResolve() {
    return new VariableClassification(
        hasRelevantNonIntAddVars,
        intBoolVars,
        intEqualVars,
        intAddVars,
        intOverflowVars,
        relevantVariables,
        addressedVariables,
        relevantFields,
        addressedFields,
        partitions,
        intBoolPartitions,
        intEqualPartitions,
        intAddPartitions,
        edgeToPartitions,
        assumedVariables,
        assignedVariables);
  }
}
