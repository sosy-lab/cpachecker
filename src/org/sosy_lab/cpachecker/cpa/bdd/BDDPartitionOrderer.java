/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bdd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.VariableClassification.Partition;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/** This Visitor collects a graph of dependencies of partitions.
 * They can be used for ordering variables in the BDD. */
public class BDDPartitionOrderer {

  /** this graph contains all dependencies between partitions */
  private Multimap<Partition, Partition> graph = HashMultimap.create();

  private VariableClassification varClass;

  public BDDPartitionOrderer(CFA cfa) {
    assert cfa.getVarClassification().isPresent();
    this.varClass = cfa.getVarClassification().get();

    // collect assumption, they are split-points
    CFAAssumptionCollector aCol = new CFAAssumptionCollector();
    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(cfa.getMainFunction(), aCol);
    Collection<CAssumeEdge> assumptions = aCol.getAssumptions();

    for (CAssumeEdge ass : assumptions) {
      collectDependentPartitions(ass);
    }
  }

  /** This function collects some edges, that are dependent from the assumption,
   * and puts them into the graph. */
  private void collectDependentPartitions(CAssumeEdge assumption) {
    CFANode root = assumption.getPredecessor();
    assert root.getNumLeavingEdges() == 2 : "assumption must have 2 branches.";

    CFAEdge ass1 = root.getLeavingEdge(0);
    CFAEdge ass2 = root.getLeavingEdge(1);
    assert ass1 == assumption || ass2 == assumption;

    Partition assPartition = varClass.getPartitionForEdge(ass1);
    assert varClass.getPartitionForEdge(ass2) == assPartition;

    // left branch
    CFAUntilSplitCollector fCol1 = new CFAUntilSplitCollector();
    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(ass1.getSuccessor(), fCol1);
    Set<CFAEdge> reachable1 = fCol1.getEdges();

    // right branch
    CFAUntilSplitCollector fCol2 = new CFAUntilSplitCollector();
    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(ass2.getSuccessor(), fCol2);
    Set<CFAEdge> reachable2 = fCol2.getEdges();

    // get distinct edges, they are either in left or in right branch
    SetView<CFAEdge> distinctEdges = Sets.symmetricDifference(reachable1, reachable2);
    for (CFAEdge edge : distinctEdges) {
      Partition part = varClass.getPartitionForEdge(edge);
      if (part != null) {
        graph.put(assPartition, part);
      }
    }
  }

  /** returns a ordered list of partitions, so that the BDD stays small. */
  public Partition[] getOrderedPartitions() {

    // TODO use some "Minimum Linear Arrangement Algorithm"?

    Collection<Partition> partitions = Sets.newLinkedHashSet();

    for (Partition p : graph.keySet()) {
      if (!partitions.contains(p)) {
        partitions.add(p);
      }
      for (Partition child : graph.get(p)) {
        if (!partitions.contains(child)) {
          partitions.add(child);
        }
      }
    }

    // add partitions, that are not dependent
    for (Partition p : varClass.getPartitions()) {
      if (!partitions.contains(p)) {
        partitions.add(p);
      }
    }
    return partitions.toArray(new Partition[0]);
  }

  /** This Visitor collects all edges reachable from the a node
   * until a split-point is reached. */
  private class CFAUntilSplitCollector implements CFAVisitor {

    private Set<CFAEdge> edges = new LinkedHashSet<CFAEdge>();

    public Set<CFAEdge> getEdges() {
      return edges;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {
      edges.add(pEdge);
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      int numChildren = pNode.getNumLeavingEdges();
      if (numChildren > 1) { // split-point
        for (int i = 0; i < numChildren; i++) {
          edges.add(pNode.getLeavingEdge(i));
        }
        return TraversalProcess.SKIP;

      } else {
        return TraversalProcess.CONTINUE;
      }
    }
  }


  /** This Visitor collects all assumptionEdges from the CFA.
   * For each assumption only the true-edge is collected. */
  private class CFAAssumptionCollector extends DefaultCFAVisitor {

    private Collection<CAssumeEdge> assumptions = new ArrayList<CAssumeEdge>();

    public Collection<CAssumeEdge> getAssumptions() {
      return assumptions;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {

      // graph splits into branches
      if (pEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        CAssumeEdge assumption = (CAssumeEdge) pEdge;
        if (assumption.getTruthAssumption()) { // true-branch
          assumptions.add(assumption);
        }
      }

      return TraversalProcess.CONTINUE;
    }
  }
}
