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
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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

  private static void insertCombineDefs(
      Builder pBuilder, DomFrontiers<CFANode> pFrontiers, CFANode pEntryNode) {

    CFAUtils.allEnteringEdges(pEntryNode)
        .first()
        .toJavaUtil()
        .ifPresent(
            edge -> {
              for (MemoryLocation variable : pBuilder.getDefs(edge)) {
                pBuilder.insertCombineDef(pEntryNode, variable);
              }
            });

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

  private static CFAEdge getEdge(CFANode pPredecessor, CFANode pSuccessor) {

    for (CFAEdge edge : CFAUtils.allLeavingEdges(pPredecessor)) {
      if (edge.getSuccessor().equals(pSuccessor)) {
        return edge;
      }
    }

    return null;
  }

  private static void traverseDomTree(Builder pBuilder, Builder.Frame pFrame) {

    Builder.Frame current = pFrame;

    CFAUtils.allEnteringEdges(current.node)
        .forEach(
            edge -> {
              for (MemoryLocation variable : pBuilder.getDefs(edge)) {
                pBuilder
                    .getCombineDef(pFrame.node, variable)
                    .add(new AbstractDef.VariableDef(variable, edge));
              }
            });

    pBuilder.push(current.node);

    while (true) {

      if (!current.children.isEmpty()) {

        Builder.Frame prev = current;
        current = current.children.remove();

        CFAEdge edge = getEdge(prev.node, current.node);

        if (edge != null) {
          if (edge instanceof FunctionSummaryEdge) {
            for (CFAEdge callEdge : CFAUtils.leavingEdges(prev.node)) {
              pBuilder.push(callEdge);
              pBuilder.pop(callEdge);
            }
          }
          pBuilder.push(edge);
        }

        pBuilder.push(current.node);

        if (current.children.isEmpty() && current.node.getNumLeavingEdges() == 1) {
          pBuilder.push(current.node.getLeavingEdge(0));
          pBuilder.pop(current.node.getLeavingEdge(0));
        }

      } else if (current.parent != null) {

        Builder.Frame prev = current;
        current = current.parent;

        pBuilder.pop(prev.node);

        CFAEdge edge = getEdge(current.node, prev.node);

        if (edge != null) {
          pBuilder.pop(edge);
        }

      } else {
        break;
      }
    }
  }

  static void execute(
      final FunctionEntryNode pEntryNode,
      final DependenceConsumer pDependenceConsumer,
      final UnknownPointerConsumer pUnknownPointerConsumer) {

    Builder builder = new Builder();

    Set<CFANode> nodes =
        CFATraversal.dfs()
            .ignoreFunctionCalls()
            .collectNodesReachableFromTo(pEntryNode, pEntryNode.getExitNode());

    for (CFANode node : nodes) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        Optional<DefsUses.Data> defsUses = DefsUses.getData(edge);
        if (defsUses.isPresent()) {
          builder.register(edge, defsUses.get());
        } else {
          pUnknownPointerConsumer.accept(edge);
          builder.register(edge, DefsUses.getEmptyData(edge));
        }
      }
    }

    for (CFAEdge callEdge : CFAUtils.allEnteringEdges(pEntryNode)) {
      builder.register(callEdge, DefsUses.getCallDefs((FunctionCallEdge) callEdge));
    }

    DomTree<CFANode> domTree =
        Dominance.createDomTree(
            pEntryNode,
            DependenceGraphBuilder::iterateSuccessors,
            DependenceGraphBuilder::iteratePredecessors);

    DomFrontiers<CFANode> frontiers = Dominance.createDomFrontiers(domTree);

    insertCombineDefs(builder, frontiers, pEntryNode);

    Builder.Frame root = builder.createFrames(domTree);
    traverseDomTree(builder, root);

    builder.dependences.forEach(
        (dependent, def) -> {
          for (CFAEdge edge : def.getDefEdges()) {
            pDependenceConsumer.accept(edge, dependent, def.getVariable());
          }
        });

    // flow dependence on return value (if present)
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

    private Deque<AbstractDef> getStack(MemoryLocation pVariable) {
      return stacks.computeIfAbsent(pVariable, key -> new ArrayDeque<>());
    }

    private void push(CFAEdge pEdge) {

      // edge uses: find corresponding AbstractDefs and add dependences
      for (MemoryLocation useVar : getUses(pEdge)) {
        AbstractDef def = getStack(useVar).peek();
        assert def != null
            : String.format("Variable is missing definition: %s @ %s", useVar, pEdge);
        dependences.put(pEdge, def);
      }

      // edge defs: update def stacks
      for (MemoryLocation defVar : getDefs(pEdge)) {
        getStack(defVar).push(new AbstractDef.VariableDef(defVar, pEdge));
      }

      // update successor CombineDefs
      for (AbstractDef.CombineDef combineDef : combineDefs.get(pEdge.getSuccessor())) {
        AbstractDef def = getStack(combineDef.getVariable()).peek();
        if (def != null) {
          combineDef.add(def);
        }
      }
    }

    private void push(CFANode pNode) {

      for (AbstractDef.CombineDef combineDef : combineDefs.get(pNode)) {
        getStack(combineDef.getVariable()).push(combineDef);
      }
    }

    private void pop(CFAEdge pEdge) {

      for (MemoryLocation variable : defsUses.get(pEdge).getDefs()) {
        getStack(variable).pop();
      }
    }

    private void pop(CFANode pNode) {

      for (AbstractDef.CombineDef combineDef : combineDefs.get(pNode)) {
        getStack(combineDef.getVariable()).pop();
      }
    }

    private Frame createFrames(DomTree<CFANode> pDomTree) {

      Frame[] frames = new Frame[pDomTree.getNodeCount()];
      for (int id = 0; id < pDomTree.getNodeCount(); id++) {
        frames[id] = new Frame(pDomTree.getNode(id));
      }

      for (int id = 0; id < pDomTree.getNodeCount(); id++) {
        if (pDomTree.hasParent(id)) {
          Frame current = frames[id];
          Frame parent = frames[pDomTree.getParent(id)];
          parent.children.add(current);
          current.parent = parent;
        }
      }

      return frames[frames.length - 1];
    }

    private static final class Frame {

      private final CFANode node;

      private Frame parent;
      private Queue<Frame> children;

      private Frame(CFANode pNode) {
        node = pNode;
        children = new ArrayDeque<>();
      }
    }
  }

  private abstract static class AbstractDef {

    private final MemoryLocation variable;

    private AbstractDef(MemoryLocation pVariable) {
      variable = pVariable;
    }

    final MemoryLocation getVariable() {
      return variable;
    }

    protected abstract Collection<CFAEdge> getDefEdges();

    private static final class VariableDef extends AbstractDef {

      private final CFAEdge defEdge;

      private VariableDef(MemoryLocation pVariable, CFAEdge pDefEdge) {
        super(pVariable);
        defEdge = pDefEdge;
      }

      @Override
      protected Collection<CFAEdge> getDefEdges() {
        return List.of(defEdge);
      }
    }

    private static final class CombineDef extends AbstractDef {

      private final List<AbstractDef> defs;

      private CombineDef(MemoryLocation pVariable) {
        super(pVariable);
        defs = new ArrayList<>();
      }

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
