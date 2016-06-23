/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.speci;

import java.util.Map.Entry;
import java.util.Objects;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;

public class Automaton {

  private final int root;
  private final int sink;

  private final ImmutableMap<Integer, ImmutableList<AutomatonEdge>> edges;

  private final int nextId;

  public Automaton() {
    root = 0;
    sink = 0;
    edges = ImmutableMap.of();

    nextId = 1;
  }

  private Automaton(int root, int sink, int nextId, ImmutableMap<Integer, ImmutableList<AutomatonEdge>> edges) {
    this.root = root;
    this.sink = sink;
    this.nextId = nextId;
    this.edges = edges;
  }

  public Automaton addStatement(String statement) {

    Builder<Integer, ImmutableList<AutomatonEdge>> b = ImmutableBiMap.builder();
    b.putAll(edges);

    ImmutableList.Builder<AutomatonEdge> n = ImmutableList.builder();
    n.add(new AutomatonEdge(nextId, statement));
    b.put(sink, n.build());

    return new Automaton(root, nextId, nextId + 1, b.build());
  }

  public Automaton join(Automaton a) {

    int rebaseNum;
    Automaton base;
    Automaton top;

    if (this.nextId <= a.nextId) {
      rebaseNum = this.nextId;
      base = this;
      top = a;
    } else {
      rebaseNum = a.nextId;
      base = a;
      top = this;
    }

    int newRoot = this.nextId + a.nextId;
    int newSink = newRoot + 1;
    int newNextId = newRoot + 2;

    /*
     * join sets of edges
     */
    Builder<Integer, ImmutableList<AutomatonEdge>> b = ImmutableBiMap.builder();

    // just take edges of base
    b.putAll(base.edges);

    // rebase states and edges of top onto base and add them to the new map
    for (Entry<Integer, ImmutableList<AutomatonEdge>> e : top.edges.entrySet()) {
      ImmutableList.Builder<AutomatonEdge> n = ImmutableList.builder();
      for (AutomatonEdge e1 : e.getValue()) {
        n.add(new AutomatonEdge(e1.getSink() + rebaseNum, e1.getStatement()));
      }
      b.put(e.getKey() + rebaseNum, n.build());
    }

    /*
     * Create new root and sink states including edges
     */

    ImmutableList.Builder<AutomatonEdge> rootEdges = ImmutableList.builder();
    rootEdges.add(new AutomatonEdge(base.root, ""));
    rootEdges.add(new AutomatonEdge(top.root + rebaseNum, ""));
    b.put(newRoot, rootEdges.build());

    ImmutableList.Builder<AutomatonEdge> sinkEdge = ImmutableList.builder();
    sinkEdge.add(new AutomatonEdge(newSink, ""));
    b.put(base.sink, sinkEdge.build());
    b.put(top.sink + rebaseNum, sinkEdge.build());

    return new Automaton(newRoot, newSink, newNextId, b.build());
  }

   @Override
  public String toString() {

     StringBuilder sb = new StringBuilder();

     for (Entry e : edges.entrySet()) {
       sb.append(" (");
      sb.append(e.getKey());
      sb.append(": ");
      for (AutomatonEdge f : (ImmutableList<AutomatonEdge>) e.getValue()) {
        sb.append(f.getStatement());
        sb.append(" GOTO ");
        sb.append(f.getSink());
        sb.append(";");
      }
      sb.append(") ");
    }

     return sb.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(edges);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) { return true; }

    if (!(pObj instanceof Automaton)) { return false; }

    Automaton other = (Automaton) pObj;

    if (edges.size() != edges.size()) { return false; }

    for (Integer i : edges.keySet()) {
      if (edges.get(i).size() != other.edges.get(i).size()) {
        return false;
      } else {
        for (int j = 0; j < edges.size(); j++) {
          if (! edges.get(i).get(j).equals(other.edges.get(i).get(j))) {
            return false;
          }
        }
      }
    }

    return true;
  }

  /**
   * Returns true if this state is less or equal than the state provided as an argument.
   *
   * @param other is the state to compare to
   * @return (this <= other)
   */
  public boolean isLessOrEqual(Automaton other) {

    if (this == other) { return true; }
    if (this.sink > other.sink) { return false; }


    for (Entry<Integer, ImmutableList<AutomatonEdge>> e : edges.entrySet()) {

      if (!other.edges.containsKey(e.getKey())) {
        return false;
      } else {
        UnmodifiableIterator<AutomatonEdge> thisIt = e.getValue().iterator();
        UnmodifiableIterator<AutomatonEdge> otherIt = other.edges.get(e.getKey()).iterator();

        while (otherIt.hasNext()) {
          AutomatonEdge otherEdge = otherIt.next();
          if (!(otherEdge.getSink() > this.sink)) {
            AutomatonEdge thisEdge = thisIt.next();

            if (! thisEdge.equals(otherEdge)) {
              return false;
            }
          }
        }
      }
    }

    return true;
  }

  public int getRoot() {
    return root;
  }

  public int getSink() {
    return sink;
  }

  public AutomatonEdge getEdge(int root) {
    return edges.get(root).get(0);
  }

  public int getNextId() {
    return nextId;
  }
}
