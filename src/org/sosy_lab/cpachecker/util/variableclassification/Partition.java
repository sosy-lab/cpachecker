// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Table;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * A Partition is a Wrapper for a Collection of vars, values and edges. The Partitions are disjunct,
 * so no variable and no edge is in 2 Partitions.
 */
public class Partition implements Comparable<Partition>, Serializable {

  private static final long serialVersionUID = 1L;

  private static final transient UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  /** we use an index to track the "age" of a partition. */
  private final int index;

  private final NavigableSet<String> vars = new TreeSet<>();
  private final NavigableSet<BigInteger> values = new TreeSet<>();

  @SuppressFBWarnings("SE_BAD_FIELD")
  private final Multimap<CFAEdge, Integer> edges = HashMultimap.create();

  private final Map<String, Partition> varToPartition;

  @SuppressFBWarnings("SE_BAD_FIELD")
  private final Table<CFAEdge, Integer, Partition> edgeToPartition;

  Partition(
      Map<String, Partition> varToPartition, Table<CFAEdge, Integer, Partition> edgeToPartition) {
    this.varToPartition = checkNotNull(varToPartition);
    this.edgeToPartition = checkNotNull(edgeToPartition);
    index = idGenerator.getFreshId();
  }

  public NavigableSet<String> getVars() {
    return Collections.unmodifiableNavigableSet(vars);
  }

  public NavigableSet<BigInteger> getValues() {
    return Collections.unmodifiableNavigableSet(values);
  }

  public Multimap<CFAEdge, Integer> getEdges() {
    return Multimaps.unmodifiableMultimap(edges);
  }

  /** adds the var to the partition and also to the global set of all vars. */
  void add(String var) {
    vars.add(var);
    varToPartition.put(var, this);
  }

  void addValues(Set<BigInteger> newValues) {
    values.addAll(newValues);
  }

  void addEdge(CFAEdge edge, int pIndex) {
    edges.put(edge, pIndex);
    edgeToPartition.put(edge, pIndex, this);
  }

  /** copies all data from other to current partition */
  void merge(Partition other) {
    assert varToPartition == other.varToPartition;

    vars.addAll(other.vars);
    values.addAll(other.values);
    edges.putAll(other.edges);

    // update mapping of vars
    for (String var : other.vars) {
      varToPartition.put(var, this);
    }

    // update mapping of edges
    for (Entry<CFAEdge, Integer> edge : other.edges.entries()) {
      edgeToPartition.put(edge.getKey(), edge.getValue(), this);
    }
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Partition && index == ((Partition) other).index;
  }

  @Override
  public int hashCode() {
    return index;
  }

  @Override
  public String toString() {
    return vars + " --> " + values;
  }

  @Override
  public int compareTo(Partition other) {
    return Integer.compare(index, other.index);
  }
}
