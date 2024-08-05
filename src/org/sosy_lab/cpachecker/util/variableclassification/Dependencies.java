// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * This class stores dependencies between variables. It sorts vars into partitions. Dependent vars
 * are in the same partition. Partitions are independent.
 */
class Dependencies {

  /** partitions, each of them contains vars */
  final Set<Partition> partitions = new LinkedHashSet<>();

  /** map to get partition of a var */
  private final Map<String, Partition> varToPartition = new HashMap<>();

  /** table to get a partition for a edge. */
  // we use subtype, because we might serialize the table, and FindBugs complains.
  final Table<CFAEdge, Integer, Partition> edgeToPartition = HashBasedTable.create();

  /**
   * This function returns a partition containing all vars, that are dependent with the given
   * variable.
   */
  public Partition getPartitionForVar(String var) {
    return varToPartition.get(checkNotNull(var));
  }

  /** This function creates a dependency between function1::var1 and function2::var2. */
  public void add(String var1, String var2) {

    // if both vars exists in some dependencies,
    // either ignore them or merge their partitions
    Partition partition1 = varToPartition.get(var1);
    Partition partition2 = varToPartition.get(var2);
    if (partition1 != null && partition2 != null) {

      // swap partitions, we create partitions in the order they are used
      if (partition1.compareTo(partition2) > 0) {
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

  /**
   * This function adds a group of vars to exactly one partition. The values are stored in the
   * partition. The partition is "connected" with the expression.
   *
   * @param vars group of variables tobe added
   * @param values numbers, with are used in an expression together with the variables
   * @param edge where is the expression
   * @param index if an edge has several expressions, this index is the position ofthe expression
   */
  public void addAll(
      @Nullable Collection<String> vars,
      @Nullable Set<BigInteger> values,
      CFAEdge edge,
      int index) {
    checkNotNull(edge);
    if (vars == null || vars.isEmpty()) {
      return;
    }

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

  /**
   * This function adds one single variable to the partitions. This is the only method to create a
   * partition with only one element.
   */
  public void addVar(String var) {

    // if var exists, we can ignore it, otherwise create new partition for var
    if (!varToPartition.containsKey(var)) {
      Partition partition = new Partition(varToPartition, edgeToPartition);
      partition.add(var);
      partitions.add(partition);
    }
  }

  /**
   * This function adds all depending vars to the set, if necessary. If A depends on B and A is part
   * of the set, B is added to the set, and vice versa. Example: If A is not boolean, B is not
   * boolean.
   */
  public void solve(final Set<String> vars) {
    checkNotNull(vars);
    for (Partition partition : partitions) {

      // is at least one var from the partition part of vars
      if (Iterables.any(partition.getVars(), v -> vars.contains(v))) {
        // add all dependend vars to vars
        vars.addAll(partition.getVars());
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder("[");
    Joiner.on(",\n").appendTo(str, partitions);
    str.append("]");

    //      for (Pair<CFAEdge, Integer> edge : edgeToPartition.keySet()) {
    //        str.append(edge.getFirst().getRawStatement() + " :: "
    //            + edge.getSecond() + " --> " + edgeToPartition.get(edge) + "\n");
    //      }
    return str.toString();
  }
}
