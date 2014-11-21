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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class VariableClassification {

  private final boolean hasRelevantNonIntAddVars;

  private final Dependencies dependencies;

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

  /** Fields information doesn't take any aliasing information into account,
   *  fields are considered per type, not per composite instance */
  // Initially contains fields used in assumes and assigned to pointer dereferences,
  // then all essential fields (by propagation)
  private final Multimap<CCompositeType, String> relevantFields;

  private final Set<Partition> intBoolPartitions;
  private final Set<Partition> intEqualPartitions;
  private final Set<Partition> intAddPartitions;

  VariableClassification(boolean pHasRelevantNonIntAddVars,
      Dependencies pDependencies,
      Set<String> pIntBoolVars,
      Set<String> pIntEqualVars,
      Set<String> pIntAddVars,
      Set<String> pRelevantVariables,
      Set<String> pAddressedVariables,
      Multimap<CCompositeType, String> pRelevantFields,
      Set<Partition> pIntBoolPartitions,
      Set<Partition> pIntEqualPartitions,
      Set<Partition> pIntAddPartitions) {
    hasRelevantNonIntAddVars = pHasRelevantNonIntAddVars;
    dependencies = pDependencies;
    intBoolVars = ImmutableSet.copyOf(pIntBoolVars);
    intEqualVars = ImmutableSet.copyOf(pIntEqualVars);
    intAddVars = ImmutableSet.copyOf(pIntAddVars);
    relevantVariables = ImmutableSet.copyOf(pRelevantVariables);
    addressedVariables = ImmutableSet.copyOf(pAddressedVariables);
    relevantFields = ImmutableSetMultimap.copyOf(pRelevantFields);
    intBoolPartitions = ImmutableSet.copyOf(pIntBoolPartitions);
    intEqualPartitions = ImmutableSet.copyOf(pIntEqualPartitions);
    intAddPartitions = ImmutableSet.copyOf(pIntAddPartitions);
  }

  @VisibleForTesting
  public static VariableClassification empty() {
    return new VariableClassification(false,
        new Dependencies(),
        ImmutableSet.<String>of(),
        ImmutableSet.<String>of(),
        ImmutableSet.<String>of(),
        ImmutableSet.<String>of(),
        ImmutableSet.<String>of(),
        ImmutableSetMultimap.<CCompositeType, String>of(),
        ImmutableSet.<Partition>of(),
        ImmutableSet.<Partition>of(),
        ImmutableSet.<Partition>of()
        );
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
  public List<Partition> getPartitions() {
    return dependencies.getPartitions();
  }

  /** This function returns a partition containing all vars,
   * that are dependent from a given CFAedge. */
  public Partition getPartitionForEdge(CFAEdge edge) {
    return getPartitionForEdge(edge, 0);
  }

  /** This function returns a partition containing all vars,
   * that are dependent from a given CFAedge.
   * The index is 0 for all edges, except functionCalls,
   * where it is the position of the param.
   * For the left-hand-side of the assignment of external functionCalls use -1. */
  public Partition getPartitionForEdge(CFAEdge edge, int index) {
    return dependencies.getPartitionForEdge(edge, index);
  }

  public static String createFunctionReturnVariable(final String function) {
    return VariableClassificationBuilder.createFunctionReturnVariable(function);
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

   private Partition(Map<String, Partition> varToPartition,
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
   private void add(String var) {
     vars.add(var);
     varToPartition.put(var, this);
   }

   private void addValues(Set<BigInteger> newValues) {
     values.addAll(newValues);
   }

   void addEdge(CFAEdge edge, int index) {
     edges.put(edge, index);
     edgeToPartition.put(Pair.of(edge, index), this);
   }

   /** copies all data from other to current partition */
   private void merge(Partition other) {
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

  /** This class stores dependencies between variables.
   * It sorts vars into partitions.
   * Dependent vars are in the same partition. Partitions are independent. */
  static class Dependencies {

    /** partitions, each of them contains vars */
    private final List<Partition> partitions = Lists.newArrayList();

    /** map to get partition of a var */
    private final Map<String, Partition> varToPartition = Maps.newHashMap();

    /** table to get a partition for a edge. */
    private final Map<Pair<CFAEdge, Integer>, Partition> edgeToPartition = Maps.newHashMap();

    public List<Partition> getPartitions() {
      return partitions;
    }

    public Partition getPartitionForVar(String var) {
      return varToPartition.get(var);
    }

    public Partition getPartitionForEdge(CFAEdge edge, int index) {
      return edgeToPartition.get(Pair.of(edge, index));
    }

    /** This function creates a dependency between function1::var1 and function2::var2. */
    public void add(String var1, String var2) {

      // if both vars exists in some dependencies,
      // either ignore them or merge their partitions
      Partition partition1 = varToPartition.get(var1);
      Partition partition2 = varToPartition.get(var2);
      if (partition1 != null && partition2 != null) {

        // swap partitions, we create partitions in the order they are used
        if (partitions.lastIndexOf(partition1) > partitions.lastIndexOf(partition2)) {
          Partition tmp = partition2;
          partition2 = partition1;
          partition1 = tmp;
        }

        if (!partition1.equals(partition2)) {
          partition1.merge(partition2);
          partitions.remove(partition2);
        }

        // if only left side of dependency exists, add right side into same partition
      } else if (partition1 != null) {
        partition1.add(var2);

        // if only right side of dependency exists, add left side into same partition
      } else if (partition2 != null) {
        partition2.add(var1);

        // if none side is in any existing partition, create new partition
      } else {
        Partition partition = new Partition(varToPartition, edgeToPartition);
        partition.add(var1);
        partition.add(var2);
        partitions.add(partition);
      }
    }

    /** This function adds a group of vars to exactly one partition.
     * The values are stored in the partition.
     * The partition is "connected" with the expression.
     *
     * @param vars group of variables tobe added
     * @param values numbers, with are used in an expression together with the variables
     * @param edge where is the expression
     * @param index if an edge has several expressions, this index is the position ofthe expression
     *  */
    public void addAll(Collection<String> vars, Set<BigInteger> values,
        CFAEdge edge, int index) {
      if (vars == null || vars.isEmpty()) { return; }

      Iterator<String> iter = vars.iterator();

      // we use same varName for all other vars --> dependency
      String var = iter.next();

      // first add one single var
      addVar(var);

      // then add all other vars, they are dependent from the first var
      while (iter.hasNext()) {
        add(var, iter.next());
      }

      Partition partition = getPartitionForVar(var);
      partition.addValues(values);
      partition.addEdge(edge, index);
    }

    /** This function adds one single variable to the partitions.
     * This is the only method to create a partition with only one element. */
    public void addVar(String var) {

      // if var exists, we can ignore it, otherwise create new partition for var
      if (!varToPartition.containsKey(var)) {
        Partition partition = new Partition(varToPartition, edgeToPartition);
        partition.add(var);
        partitions.add(partition);
      }
    }

    /** This function adds all depending vars to the set, if necessary.
     * If A depends on B and A is part of the set, B is added to the set, and vice versa.
    * Example: If A is not boolean, B is not boolean. */
    public void solve(final Collection<String> vars) {
      for (Partition partition : partitions) {

        // is at least one var from the partition part of vars
        boolean isDependency = false;
        for (String var : partition.getVars()) {
          if (vars.contains(var)) {
            isDependency = true;
            break;
          }
        }

        // add all dependend vars to vars
        if (isDependency) {
          vars.addAll(partition.getVars());
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder str = new StringBuilder("[");
      for (Partition partition : partitions) {
        str.append(partition.toString() + ",\n");
      }
      str.append("]\n\n");

      //      for (Pair<CFAEdge, Integer> edge : edgeToPartition.keySet()) {
      //        str.append(edge.getFirst().getRawStatement() + " :: "
      //            + edge.getSecond() + " --> " + edgeToPartition.get(edge) + "\n");
      //      }
      return str.toString();
    }
  }
}
