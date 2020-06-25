/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

  private transient static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

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
    return Integer.compare(this.index, other.index);
  }
}
