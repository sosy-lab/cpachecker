/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomFrontiers;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

final class FlowDep {

  private FlowDep() {}

  /** Inserts an empty CombineDef for every function parameter. */
  private static void insertFunctionParamCombineDefs(
      Builder pBuilder, FunctionEntryNode pEntryNode) {

    // use only a single call edge to determine function parameters
    // add a CombineDef for every variable
    CFAUtils.allEnteringEdges(pEntryNode)
        .first()
        .toJavaUtil()
        .ifPresent(
            edge -> {
              for (MemoryLocation variable : pBuilder.getDefs(edge)) {
                pBuilder.insertCombineDef(pEntryNode, variable);
              }
            });
  }

  /**
   * Inserts empty CombineDefs at all nodes where different definitions of a single variable are
   * visible.
   */
  private static void insertCombineDefs(
      Builder pBuilder, DomFrontiers<CFANode> pFrontiers, FunctionEntryNode pEntryNode) {

    insertFunctionParamCombineDefs(pBuilder, pEntryNode);

    for (MemoryLocation variable : pBuilder.getVariables()) {

      Queue<CFANode> waitlist = new ArrayDeque<>();
      Set<CFANode> seen = new TreeSet<>();

      for (CFAEdge defEdge : pBuilder.getDefEdges(variable)) {
        if (!(defEdge instanceof FunctionCallEdge)) {
          waitlist.add(defEdge.getPredecessor());
        }
      }

      seen.addAll(waitlist);

      while (!waitlist.isEmpty()) {

        for (CFANode node : pFrontiers.getFrontier(waitlist.remove())) {
          if (!pBuilder.containsCombineDef(node, variable)) {

            pBuilder.insertCombineDef(node, variable);

            if (!seen.contains(node)) {
              waitlist.add(node);
              seen.add(node);
            }
          }
        }
      }
    }
  }

  /** Returns the edge between the two specified nodes. Null if such an edge does not exist. */
  private static CFAEdge getEdge(CFANode pPredecessor, CFANode pSuccessor) {

    for (CFAEdge edge : CFAUtils.allLeavingEdges(pPredecessor)) {
      if (edge.getSuccessor().equals(pSuccessor)) {
        return edge;
      }
    }

    return null;
  }

  /** Initializes function parameter CombineDefs with ConcreteDefs from all entering call edges. */
  private static void initFunctionParams(Builder pBuilder, DomTreeNode pRoot) {

    for (CFAEdge edge : CFAUtils.allEnteringEdges(pRoot.getCfaNode())) {
      for (MemoryLocation variable : pBuilder.getDefs(edge)) {
        pBuilder
            .getCombineDef(pRoot.getCfaNode(), variable)
            .add(new AbstractDef.ConcreteDef(variable, edge));
      }
    }
  }

  private static void addFlowDeps(Builder pBuilder, DomTree<CFANode> pDomTree) {

    DomTreeNode current = DomTreeNode.create(pDomTree);

    initFunctionParams(pBuilder, current);

    pBuilder.push(current.getCfaNode());

    while (true) { // traverse the dominance tree (once)

      if (current.hasNextChild()) { // any unvisited children left?

        DomTreeNode prev = current;
        current = current.nextChild();

        CFAEdge edge = getEdge(prev.getCfaNode(), current.getCfaNode());

        if (edge != null) {
          if (edge instanceof FunctionSummaryEdge) {
            for (CFAEdge callEdge : CFAUtils.leavingEdges(prev.getCfaNode())) {
              pBuilder.push(callEdge);
              pBuilder.pop(callEdge);
            }
          }
          pBuilder.push(edge);
        }

        pBuilder.push(current.getCfaNode());

        if (current.children.isEmpty() && current.getCfaNode().getNumLeavingEdges() == 1) {
          pBuilder.push(current.getCfaNode().getLeavingEdge(0));
          pBuilder.pop(current.getCfaNode().getLeavingEdge(0));
        }

      } else if (current.getParent() != null) { // has parent (is not root)?

        DomTreeNode prev = current;
        current = current.getParent();

        pBuilder.pop(prev.getCfaNode());

        CFAEdge edge = getEdge(current.getCfaNode(), prev.getCfaNode());

        if (edge != null) {
          pBuilder.pop(edge);
        }

      } else {
        break; // node has no unvisited children left and is root -> done
      }
    }
  }

  private static void addReturnValueFlowDep(
      final FunctionEntryNode pEntryNode, final DependenceConsumer pDependenceConsumer) {

    Optional<? extends AVariableDeclaration> optRetVar =
        pEntryNode.getReturnVariable().toJavaUtil();

    if (optRetVar.isPresent()) {

      MemoryLocation returnVar = MemoryLocation.valueOf(optRetVar.get().getQualifiedName());

      for (CFAEdge defEdge : CFAUtils.allEnteringEdges(pEntryNode.getExitNode())) {
        for (CFAEdge returnEdge : CFAUtils.allLeavingEdges(pEntryNode.getExitNode())) {
          pDependenceConsumer.accept(defEdge, returnEdge, returnVar);
        }
      }

      for (CFAEdge returnEdge : CFAUtils.allLeavingEdges(pEntryNode.getExitNode())) {
        CFAEdge summaryEdge = returnEdge.getSuccessor().getEnteringSummaryEdge();
        assert summaryEdge != null : "Missing summary edge for return edge: " + returnEdge;
        pDependenceConsumer.accept(returnEdge, summaryEdge, returnVar);
      }
    }
  }

  /** Adds relevant DefsUses.Data to the builder. */
  private static void initDefsUses(
      Builder pBuilder,
      FunctionEntryNode pEntryNode,
      UnknownPointerConsumer pUnknownPointerConsumer) {

    Set<CFANode> nodes =
        CFATraversal.dfs()
            .ignoreFunctionCalls()
            .collectNodesReachableFromTo(pEntryNode, pEntryNode.getExitNode());

    for (CFANode node : nodes) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        Optional<DefsUses.Data> defsUses = DefsUses.getData(edge);
        if (defsUses.isPresent()) {
          pBuilder.register(edge, defsUses.orElseThrow());
        } else {
          pUnknownPointerConsumer.accept(edge);
          pBuilder.register(edge, DefsUses.getEmptyData(edge));
        }
      }
    }

    for (CFAEdge callEdge : CFAUtils.allEnteringEdges(pEntryNode)) {
      pBuilder.register(callEdge, DefsUses.getCallDefs((FunctionCallEdge) callEdge));
    }
  }

  static void execute(
      final FunctionEntryNode pEntryNode,
      final DependenceConsumer pDependenceConsumer,
      final UnknownPointerConsumer pUnknownPointerConsumer) {

    Builder builder = new Builder();

    DomTree<CFANode> domTree =
        Dominance.createDomTree(
            pEntryNode,
            DependenceGraphBuilder::iterateSuccessors,
            DependenceGraphBuilder::iteratePredecessors);

    DomFrontiers<CFANode> frontiers = Dominance.createDomFrontiers(domTree);

    initDefsUses(builder, pEntryNode, pUnknownPointerConsumer);
    insertCombineDefs(builder, frontiers, pEntryNode);
    addFlowDeps(builder, domTree);

    builder.dependences.forEach(
        (dependent, def) -> {
          for (CFAEdge edge : def.getDefEdges()) {
            pDependenceConsumer.accept(edge, dependent, def.getVariable());
          }
        });

    addReturnValueFlowDep(pEntryNode, pDependenceConsumer);
  }

  @FunctionalInterface
  interface DependenceConsumer {

    void accept(CFAEdge pEdge, CFAEdge pDependent, MemoryLocation pCause);
  }

  @FunctionalInterface
  interface UnknownPointerConsumer {

    void accept(CFAEdge pEdge);
  }

  private static final class Builder {

    private final Multimap<MemoryLocation, CFAEdge> variables;
    private final Multimap<CFANode, AbstractDef.CombineDef> combineDefs;
    private final Map<CFAEdge, DefsUses.Data> defsUses;
    private final Map<MemoryLocation, Deque<AbstractDef>> stacks;
    private final Multimap<CFAEdge, AbstractDef> dependences;

    private Builder() {

      variables = ArrayListMultimap.create();
      combineDefs = ArrayListMultimap.create();
      defsUses = new HashMap<>();
      stacks = new HashMap<>();
      dependences = ArrayListMultimap.create();
    }

    private Set<MemoryLocation> getVariables() {
      return variables.keySet();
    }

    private Collection<CFAEdge> getDefEdges(MemoryLocation pVariable) {
      return variables.get(pVariable);
    }

    private void register(CFAEdge pEdge, DefsUses.Data pDefsUses) {

      for (MemoryLocation defVar : pDefsUses.getDefs()) {
        variables.put(defVar, pEdge);
      }

      defsUses.put(pEdge, pDefsUses);
    }

    private Set<MemoryLocation> getDefs(CFAEdge pEdge) {
      return defsUses.get(pEdge).getDefs();
    }

    private Set<MemoryLocation> getUses(CFAEdge pEdge) {
      return defsUses.get(pEdge).getUses();
    }

    private AbstractDef.CombineDef getCombineDef(CFANode pNode, MemoryLocation pVariable) {

      for (AbstractDef.CombineDef combineDef : combineDefs.get(pNode)) {
        if (combineDef.getVariable().equals(pVariable)) {
          return combineDef;
        }
      }

      return null;
    }

    private boolean containsCombineDef(CFANode pNode, MemoryLocation pVariable) {

      return getCombineDef(pNode, pVariable) != null;
    }

    private void insertCombineDef(CFANode pNode, MemoryLocation pVariable) {
      combineDefs.put(pNode, new AbstractDef.CombineDef(pVariable));
    }

    private AbstractDef.ConcreteDef getDeclaration(Deque<AbstractDef> pStack) {

      for (Iterator<AbstractDef> it = pStack.descendingIterator(); it.hasNext(); ) {
        AbstractDef def = it.next();
        if (def instanceof AbstractDef.ConcreteDef) {
          return (AbstractDef.ConcreteDef) def;
        }
      }

      return null;
    }

    private Deque<AbstractDef> getDefStack(MemoryLocation pVariable) {
      return stacks.computeIfAbsent(pVariable, key -> new ArrayDeque<>());
    }

    /**
     *
     *
     * <ol>
     *   <li>Adds dependence on defs used by the specified edge.
     *   <li>Pushes all ConcreteDefs of the specified edge onto the def-stacks.
     *   <li>Updates CombineDefs of the successor node of the specified edge.
     * </ol>
     */
    private void push(CFAEdge pEdge) {

      // edge uses: find corresponding AbstractDefs and add dependences
      for (MemoryLocation useVar : getUses(pEdge)) {
        AbstractDef def = getDefStack(useVar).peek();

        assert def != null
            : String.format("Variable is missing definition: %s @ %s", useVar, pEdge);
            
        dependences.put(pEdge, def);
      }

      // edge defs: update def stacks and add declaration dependences
      for (MemoryLocation defVar : getDefs(pEdge)) {
        Deque<AbstractDef> stack = getDefStack(defVar);
        AbstractDef.ConcreteDef declaration = getDeclaration(stack);

        stack.push(new AbstractDef.ConcreteDef(defVar, pEdge));

        if (declaration != null) {
          dependences.put(pEdge, declaration);
        }
      }

      // update successor CombineDefs
      for (AbstractDef.CombineDef combineDef : combineDefs.get(pEdge.getSuccessor())) {
        AbstractDef def = getDefStack(combineDef.getVariable()).peek();
        if (def != null) {
          combineDef.add(def);
        }
      }
    }

    /** Pushes all CombineDefs of the specified node onto the def-stacks. */
    private void push(CFANode pNode) {

      for (AbstractDef.CombineDef combineDef : combineDefs.get(pNode)) {
        getDefStack(combineDef.getVariable()).push(combineDef);
      }
    }

    /** Pops all ConcreteDefs of the specified edge from the def-stacks. */
    private void pop(CFAEdge pEdge) {

      for (MemoryLocation variable : defsUses.get(pEdge).getDefs()) {
        getDefStack(variable).pop();
      }
    }

    /** Pops all CombineDefs of the specified node from the def-stacks. */
    private void pop(CFANode pNode) {

      for (AbstractDef.CombineDef combineDef : combineDefs.get(pNode)) {
        getDefStack(combineDef.getVariable()).pop();
      }
    }
  }

  /** Traversable dominance tree. */
  private static final class DomTreeNode {

    private final CFANode cfaNode;

    private DomTreeNode parent;
    private Queue<DomTreeNode> children;

    private DomTreeNode(CFANode pCfaNode) {
      cfaNode = pCfaNode;
      children = new ArrayDeque<>();
    }

    /** Returns the root node of the traversable dominance tree. */
    private static DomTreeNode create(DomTree<CFANode> pDomTree) {

      DomTreeNode[] nodes = new DomTreeNode[pDomTree.getNodeCount()];
      for (int id = 0; id < pDomTree.getNodeCount(); id++) {
        nodes[id] = new DomTreeNode(pDomTree.getNode(id));
      }

      for (int id = 0; id < pDomTree.getNodeCount(); id++) {
        if (pDomTree.hasParent(id)) {
          DomTreeNode current = nodes[id];
          DomTreeNode parent = nodes[pDomTree.getParent(id)];
          parent.children.add(current);
          current.parent = parent;
        }
      }

      return nodes[nodes.length - 1];
    }

    private CFANode getCfaNode() {
      return cfaNode;
    }

    private DomTreeNode getParent() {
      return parent;
    }

    private boolean hasNextChild() {
      return !children.isEmpty();
    }

    private DomTreeNode nextChild() {
      return children.remove();
    }
  }

  /**
   * A class representing reachable definitions for a specific variable.
   *
   * <p>All instances are either ConcreteDefs or CombineDefs.
   */
  private abstract static class AbstractDef {

    private final MemoryLocation variable;

    private AbstractDef(MemoryLocation pVariable) {
      variable = pVariable;
    }

    /** Returns the variable for this AbstractDef. */
    final MemoryLocation getVariable() {
      return variable;
    }

    /** Returns a collection of all CFAEdges that define the variable and are reachable. */
    protected abstract Collection<CFAEdge> getDefEdges();

    /** A ConcreteDef is used for a variable defined by a CFAEdge. */
    private static final class ConcreteDef extends AbstractDef {

      private final CFAEdge defEdge;

      private ConcreteDef(MemoryLocation pVariable, CFAEdge pDefEdge) {
        super(pVariable);
        defEdge = pDefEdge;
      }

      @Override
      protected Collection<CFAEdge> getDefEdges() {
        return ImmutableList.of(defEdge);
      }
    }

    /**
     * A CombineDef is used to collect multiple reachable definitions.
     *
     * <p>A CombineDef is used at a CFANode where two branches meet and carry different definitions
     * of a variable.
     */
    private static final class CombineDef extends AbstractDef {

      private final List<AbstractDef> defs;

      private CombineDef(MemoryLocation pVariable) {
        super(pVariable);
        defs = new ArrayList<>();
      }

      /** Adds definitions to the list of reachable definitions. */
      private void add(AbstractDef pDef) {
        defs.add(pDef);
      }

      @Override
      protected Collection<CFAEdge> getDefEdges() {
        Set<CFAEdge> edges = new HashSet<>();

        for (AbstractDef def : defs) {
          edges.addAll(def.getDefEdges());
        }

        return edges;
      }
    }
  }
}
