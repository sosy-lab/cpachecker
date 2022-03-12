// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * This Visitor collects a graph of dependencies of partitions. They can be used for ordering
 * variables in the BDD.
 */
public class BDDPartitionOrderer {

  /** this graph contains all dependencies between partitions */
  private Multimap<Partition, Partition> graph = LinkedHashMultimap.create();

  private VariableClassification varClass;

  public BDDPartitionOrderer(CFA cfa) {
    assert cfa.getVarClassification().isPresent();
    varClass = cfa.getVarClassification().orElseThrow();

    // collect assumption, they are split-points
    CFAAssumptionCollector aCol = new CFAAssumptionCollector();
    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(cfa.getMainFunction(), aCol);
    Collection<CAssumeEdge> assumptions = aCol.getAssumptions();

    for (CAssumeEdge ass : assumptions) {
      collectDependentPartitions(ass);
    }
  }

  /**
   * This function collects some edges, that are dependent from the assumption, and puts them into
   * the graph.
   */
  private void collectDependentPartitions(CAssumeEdge assumption) {
    CFANode root = assumption.getPredecessor();
    assert root.getNumLeavingEdges() == 2 : "assumption must have 2 branches.";

    CFAEdge ass1 = root.getLeavingEdge(0);
    CFAEdge ass2 = root.getLeavingEdge(1);
    assert ass1 == assumption || ass2 == assumption;

    Partition assPartition = varClass.getPartitionForEdge(ass1);
    assert varClass.getPartitionForEdge(ass2) == assPartition;

    if (assPartition == null) {
      return;
    } // assumption is like "3==4"

    // left branch
    CFAUntilSplitCollector fCol1 = new CFAUntilSplitCollector();
    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(ass1.getSuccessor(), fCol1);
    Set<CFAEdge> reachable1 = fCol1.getEdges();

    // right branch
    CFAUntilSplitCollector fCol2 = new CFAUntilSplitCollector();
    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(ass2.getSuccessor(), fCol2);
    Set<CFAEdge> reachable2 = fCol2.getEdges();

    // get edges, that are either in left or in right branch.
    // edges, that are reachable from both branches, are independent from the assumption
    SetView<CFAEdge> distinctEdges = Sets.symmetricDifference(reachable1, reachable2);
    for (CFAEdge edge : distinctEdges) {
      if (edge instanceof FunctionCallEdge) {
        final FunctionCallEdge funcCall = (FunctionCallEdge) edge;
        for (int i = 0; i < funcCall.getArguments().size(); i++) {
          final Partition part = varClass.getPartitionForParameterOfEdge(funcCall, i);
          if (part != null) {
            graph.put(assPartition, part);
          }
        }
      } else {
        final Partition part = varClass.getPartitionForEdge(edge);
        if (part != null) {
          graph.put(assPartition, part);
        }
      }
    }
  }

  /** returns a ordered list of partitions, so that the BDD stays small. */
  public List<Partition> getOrderedPartitions() {

    // TODO use some "Minimum Linear Arrangement Algorithm"?

    Collection<Partition> partitions = new LinkedHashSet<>();

    for (Partition p : graph.keySet()) {
      addToPartitions(p, partitions);
    }

    @SuppressWarnings("JdkObsolete") // TODO consider replacing this with ArrayList or ArrayDeque
    List<Partition> orderedPartitions = new LinkedList<>(partitions);

    // add partitions, that are not dependent, in front of all other partitions
    for (Partition p : varClass.getPartitions()) {
      if (!partitions.contains(p)) {
        orderedPartitions.add(0, p);
      }
    }
    return orderedPartitions;
  }

  /** adds the father and all his children to the partitions,if not done before */
  private void addToPartitions(Partition father, Collection<Partition> partitions) {
    if (!partitions.contains(father)) {
      partitions.add(father);
      for (Partition child : graph.get(father)) {
        addToPartitions(child, partitions);
      }
    }
  }

  /**
   * This Visitor collects all edges reachable from the a node until a split-point (assumption) is
   * reached. The leaving edges of the splitpoint are cllected, too.
   */
  private static class CFAUntilSplitCollector implements CFAVisitor {

    private Set<CFAEdge> edges = new LinkedHashSet<>();

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
        leavingEdges(pNode).copyInto(edges);
        return TraversalProcess.SKIP;

      } else {
        return TraversalProcess.CONTINUE;
      }
    }
  }

  /**
   * This Visitor collects all assumptionEdges from the CFA. For each assumption only the true-edge
   * is collected.
   */
  private static class CFAAssumptionCollector extends DefaultCFAVisitor {

    private Collection<CAssumeEdge> assumptions = new ArrayList<>();

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
