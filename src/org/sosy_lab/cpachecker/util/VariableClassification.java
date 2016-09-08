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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

public class VariableClassification {

  private final boolean hasRelevantNonIntAddVars;

  private final Set<String> intBoolVars;
  private final Set<String> intEqualVars;
  private final Set<String> intAddVars;

  /** These sets contain all variables even ones of array, pointer or structure types.
   *  Such variables cannot be classified even as Int, so they are only kept in these sets in order
   *  not to break the classification of Int variables.*/
  // Initially contains variables used in assumes and assigned to pointer dereferences,
  // then all essential variables (by propagation)
  private final Set<String> relevantVariables;
  private final Set<String> addressedVariables;

  private final Multiset<String> assumedVariables;
  private final Multiset<String> assignedVariables;

  /** Fields information doesn't take any aliasing information into account,
   *  fields are considered per type, not per composite instance */
  // Initially contains fields used in assumes and assigned to pointer dereferences,
  // then all essential fields (by propagation)
  private final Multimap<CCompositeType, String> relevantFields;

  private final Multimap<CCompositeType, String> addressedFields;

  private final Set<Partition> partitions;
  private final Set<Partition> intBoolPartitions;
  private final Set<Partition> intEqualPartitions;
  private final Set<Partition> intAddPartitions;

  private final Map<Pair<CFAEdge, Integer>, Partition> edgeToPartitions;

  private final LogManagerWithoutDuplicates logger;

  VariableClassification(boolean pHasRelevantNonIntAddVars,
      Set<String> pIntBoolVars,
      Set<String> pIntEqualVars,
      Set<String> pIntAddVars,
      Set<String> pRelevantVariables,
      Set<String> pAddressedVariables,
      Multimap<CCompositeType, String> pRelevantFields,
      Multimap<CCompositeType, String> pAddressedFields,
      Collection<Partition> pPartitions,
      Set<Partition> pIntBoolPartitions,
      Set<Partition> pIntEqualPartitions,
      Set<Partition> pIntAddPartitions,
      Map<Pair<CFAEdge, Integer>, Partition> pEdgeToPartitions,
      Multiset<String> pAssumedVariables,
      Multiset<String> pAssignedVariables,
    LogManager pLogger) {
    hasRelevantNonIntAddVars = pHasRelevantNonIntAddVars;
    intBoolVars = ImmutableSet.copyOf(pIntBoolVars);
    intEqualVars = ImmutableSet.copyOf(pIntEqualVars);
    intAddVars = ImmutableSet.copyOf(pIntAddVars);
    relevantVariables = ImmutableSet.copyOf(pRelevantVariables);
    addressedVariables = ImmutableSet.copyOf(pAddressedVariables);
    relevantFields = ImmutableSetMultimap.copyOf(pRelevantFields);
    addressedFields = ImmutableSetMultimap.copyOf(pAddressedFields);
    partitions = ImmutableSet.copyOf(pPartitions);
    intBoolPartitions = ImmutableSet.copyOf(pIntBoolPartitions);
    intEqualPartitions = ImmutableSet.copyOf(pIntEqualPartitions);
    intAddPartitions = ImmutableSet.copyOf(pIntAddPartitions);
    edgeToPartitions = ImmutableMap.copyOf(pEdgeToPartitions);
    assumedVariables = ImmutableMultiset.copyOf(pAssumedVariables);
    assignedVariables = ImmutableMultiset.copyOf(pAssignedVariables);
    logger = new LogManagerWithoutDuplicates(pLogger);
  }

  @VisibleForTesting
  public static VariableClassification empty(LogManager pLogger) {
    return new VariableClassification(false,
        ImmutableSet.<String>of(),
        ImmutableSet.<String>of(),
        ImmutableSet.<String>of(),
        ImmutableSet.<String>of(),
        ImmutableSet.<String>of(),
        ImmutableSetMultimap.<CCompositeType, String>of(),
        ImmutableSetMultimap.<CCompositeType, String>of(),
        ImmutableSet.<Partition>of(),
        ImmutableSet.<Partition>of(),
        ImmutableSet.<Partition>of(),
        ImmutableSet.<Partition>of(),
        ImmutableMap.<Pair<CFAEdge, Integer>, Partition>of(),
        ImmutableMultiset.<String>of(),
        ImmutableMultiset.<String>of(),
        pLogger);
  }

  public boolean hasRelevantNonIntAddVars() {
    return hasRelevantNonIntAddVars;
  }

  /**
   * All variables that may be essential for reachability properties.
   * The variables are returned as a collection of scopedNames.
   * <p>
   * <strong>
   * Note: the collection includes all variables, including pointers, arrays and structures, i.e.
   *       non-Int variables.
   * </strong>
   * </p>
   */
  public Set<String> getRelevantVariables() {
    return relevantVariables;
  }

  /**
   * All variables that have their addresses taken somewhere in the source code.
   * The variables are returned as a collection of scopedNames.
   * <p>
   * <strong>
   * Note: the collection includes all variables, including pointers, arrays and structures, i.e.
   *       non-Int variables.
   * </strong>
   * </p>
   */
  public Set<String> getAddressedVariables() {
    return addressedVariables;
  }

  /**
   * All fields that may be essential for reachability properties
   * (only fields accessed explicitly through either dot (.) or arrow (->) operator count).
   *
   * @return A collection of (CCompositeType, fieldName) mappings.
   */
  public Multimap<CCompositeType, String> getRelevantFields() {
    return relevantFields;
  }

  /**
   * All fields that have their addresses taken somewhere in the source code.
   * (only fields accessed explicitly through either dot (.) or arrow (->) operator count).
   *
   * @return A collection of (CCompositeType, fieldName) mappings.
   */
  public Multimap<CCompositeType, String> getAddressedFields() {
    return addressedFields;
  }

  /** This function returns a collection of scoped names.
   * This collection contains all vars, that are boolean,
   * i.e. the value is 0 or 1. */
  public Set<String> getIntBoolVars() {
    return intBoolVars;
  }

  /** This function returns a collection of partitions.
   * Each partition contains only boolean vars. */
  public Set<Partition> getIntBoolPartitions() {
    return intBoolPartitions;
  }

  /** This function returns a collection of scoped names.
   * This collection contains all vars, that are only assigned or compared
   * for equality with integer values.
   * There are NO mathematical calculations (add, sub, mult) with these vars.
   * This collection does not contain any variable from "IntBool" or "IntAdd". */
  public Set<String> getIntEqualVars() {
    return intEqualVars;
  }

  /** This function returns a collection of partitions.
   * Each partition contains only vars,
   * that are only assigned or compared for equality with integer values.
   * This collection does not contains anypartition from "IntBool" or "IntAdd". */
  public Set<Partition> getIntEqualPartitions() {
    return intEqualPartitions;
  }

  /** This function returns a collection of scoped names.
   * This collection contains all vars, that are only used in simple calculations
   * (+, -, <, >, <=, >=, ==, !=, &, &&, |, ||, ^).
   * This collection does not contain any variable from "IntBool" or "IntEq". */
  public Set<String> getIntAddVars() {
    return intAddVars;
  }

  /** This function returns a collection of partitions.
   * Each partition contains only vars, that are used in simple calculations.
   * This collection does not contains anypartition from "IntBool" or "IntEq". */
  public Set<Partition> getIntAddPartitions() {
    return intAddPartitions;
  }

  /** This function returns a collection of partitions.
   * A partition contains all vars, that are dependent from each other. */
  public Set<Partition> getPartitions() {
    return partitions;
  }

  /**
   * This method return all variables (i.e., their qualified name), that occur in an assumption.
   */
  public Multiset<String> getAssumedVariables() {
    return assumedVariables;
  }

  /**
   * This method return all variables (i.e., their qualified name), that occur
   * as left-hand side in an assignment.
   */
  public Multiset<String> getAssignedVariables() {
    return assignedVariables;
  }

  /**
   * This function returns a partition containing all vars,
   * that are dependent from a given CFAEdge.
   * This method cannot be used for {@link FunctionCallEdge}s,
   * because these have multiple partitions.
   */
  public Partition getPartitionForEdge(CFAEdge edge) {
    checkArgument(!(edge instanceof FunctionCallEdge),
        "For FunctionCallEdges, use the specific methods because they have multiple partitions");
    return getPartitionForEdge(edge, 0);
  }

  /**
   * This method returns a partition containing all vars,
   * that are dependent from a specific function parameter at a given edge.
   */
  public Partition getPartitionForParameterOfEdge(FunctionCallEdge edge, int param) {
    Preconditions.checkElementIndex(param, edge.getArguments().size());
    return getPartitionForEdge(edge, param);
  }

  /**
   * This method returns a partition containing all vars,
   * that are dependent from a specific function return value at a given edge.
   */
  public Partition getPartitionForReturnValueOfEdge(FunctionCallEdge edge) {
    return getPartitionForEdge(edge, -1);
  }

  /** This function returns a partition containing all vars,
   * that are dependent from a given CFAedge.
   * The index is 0 for all edges, except functionCalls,
   * where it is the position of the param.
   * For the left-hand-side of the assignment of external functionCalls use -1. */
  private Partition getPartitionForEdge(CFAEdge edge, int index) {
    return edgeToPartitions.get(Pair.of(edge, index));
  }

  /**
   * This method computes for a set of variables (qualified names) a score,
   * which serves as rough estimate how expensive tracking it might be to
   * track these variables, e.g. variables with a boolean character have a
   * lower score than variables being used as loop counters.
   *
   * @param variableNames a collection of variables (qualified names)
   * @param loopStructure the loop structure, to identify loop-counter variables
   * @return the score for the given collection of variables
   */
  public int obtainDomainTypeScoreForVariables(Collection<String> variableNames,
      Optional<LoopStructure> loopStructure) {
    final int BOOLEAN_VAR   = 2;
    final int INTEQUAL_VAR  = 4;
    final int UNKNOWN_VAR   = 16;

    if(variableNames.isEmpty()) {
      return UNKNOWN_VAR;
    }

    int newScore = 1;
    int oldScore = newScore;
    for (String variableName : variableNames) {
      int factor = UNKNOWN_VAR;

      if (getIntBoolVars().contains(variableName)) {
        factor = BOOLEAN_VAR;

      } else if (getIntEqualVars().contains(variableName)) {
        factor = INTEQUAL_VAR;
      }

      newScore = newScore * factor;

      if (loopStructure.isPresent()
          && loopStructure.get().getLoopIncDecVariables().contains(variableName)) {
        return Integer.MAX_VALUE;
      }

      // check for overflow
      if(newScore < oldScore) {
        logger.logOnce(Level.WARNING,
            "Highest possible value reached in score computation."
                + " Error path prefix preference may not be applied reliably.");
        logger.logf(Level.FINE,
            "Overflow in score computation happened for variables %s.",
            variableNames.toString());

        return Integer.MAX_VALUE - 1;
      }
      oldScore = newScore;
    }

    return newScore;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("\nIntBool  " + intBoolVars.size() + "\n    " + intBoolVars);
    str.append("\nIntEq  " + intEqualVars.size() + "\n    " + intEqualVars);
    str.append("\nIntAdd  " + intAddVars.size() + "\n    " + intAddVars);
    return str.toString();
  }

  /** A Partition is a Wrapper for a Collection of vars, values and edges.
  * The Partitions are disjunct, so no variable and no edge is in 2 Partitions. */
  public static class Partition {

   private final Set<String> vars = new HashSet<>();
   private final Set<BigInteger> values = Sets.newTreeSet();
   private final Multimap<CFAEdge, Integer> edges = HashMultimap.create();

   private final Map<String, Partition> varToPartition;
   private final Map<Pair<CFAEdge, Integer>, Partition> edgeToPartition;

   Partition(Map<String, Partition> varToPartition,
       Map<Pair<CFAEdge, Integer>, Partition> edgeToPartition) {
     this.varToPartition = varToPartition;
     this.edgeToPartition = edgeToPartition;
   }

   public Set<String> getVars() {
     return vars;
   }

   public Set<BigInteger> getValues() {
     return values;
   }

   public Multimap<CFAEdge, Integer> getEdges() {
     return edges;
   }

   /** adds the var to the partition and also to the global set of all vars. */
   void add(String var) {
     vars.add(var);
     varToPartition.put(var, this);
   }

   void addValues(Set<BigInteger> newValues) {
     values.addAll(newValues);
   }

   void addEdge(CFAEdge edge, int index) {
     edges.put(edge, index);
     edgeToPartition.put(Pair.of(edge, index), this);
   }

   /** copies all data from other to current partition */
   void merge(Partition other) {
     assert this.varToPartition == other.varToPartition;

     this.vars.addAll(other.vars);
     this.values.addAll(other.values);
     this.edges.putAll(other.edges);

     // update mapping of vars
     for (String var : other.vars) {
       varToPartition.put(var, this);
     }

     // update mapping of edges
     for (Entry<CFAEdge, Integer> edge : other.edges.entries()) {
       edgeToPartition.put(Pair.of(edge.getKey(), edge.getValue()), this);
     }
   }

   @Override
   public boolean equals(Object other) {
     if (other instanceof Partition) {
       Partition p = (Partition) other;
       return this.vars == p.vars;
     } else {
       return false;
     }
   }

   @Override
   public int hashCode() {
     return vars.hashCode();
   }

   @Override
   public String toString() {
     return vars.toString() + " --> " + Arrays.toString(values.toArray());
   }
 }
}
