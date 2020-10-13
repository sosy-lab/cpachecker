// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blocks;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collections;
import com.google.common.collect.Iterables;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

/**
 * Represents a block as described in the BAM paper.
 */
public class Block {

  private final ImmutableSet<ReferencedVariable> referencedVariables;
  private ImmutableSet<String> variables; // lazy initialization
  private ImmutableSet<String> outOfScopeVariables; // lazy initialization
  private ImmutableSet<String> functions; // lazy initialization
  private final ImmutableSet<CFANode> callNodes;
  private final ImmutableSet<CFANode> returnNodes;
  private final ImmutableSet<CFANode> nodes;
  private final ImmutableSet<LockIdentifier> capturedLocks;
  private final Set<MemoryLocation> memoryLocations;

  public Block(
      Iterable<ReferencedVariable> pReferencedVariables,
      Set<CFANode> pCallNodes,
      Set<CFANode> pReturnNodes,
      Iterable<CFANode> allNodes,
      Iterable<LockIdentifier> locks,
      Iterable<MemoryLocation> locations) {

    referencedVariables = ImmutableSet.copyOf(pReferencedVariables);
    callNodes = ImmutableSortedSet.copyOf(pCallNodes);
    returnNodes = ImmutableSortedSet.copyOf(pReturnNodes);
    nodes = ImmutableSortedSet.copyOf(allNodes);
    capturedLocks = ImmutableSortedSet.copyOf(locks);
    memoryLocations = ImmutableSortedSet.copyOf(locations);
  }

  public Set<CFANode> getCallNodes() {
    return callNodes;
  }

  public CFANode getCallNode() {
    assert callNodes.size() == 1;
    return callNodes.iterator().next();
  }

  /** returns a collection of variables used in the block.
   * For soundness this must be a superset of the actually used variables. */
  @Deprecated
  // TODO unused method, potentially dangerous,
  // because dependencies between variables are potentially incomplete.
  public Set<ReferencedVariable> getReferencedVariables() {
    return referencedVariables;
  }

  public Set<LockIdentifier> getCapturedLocks() {
    return Collections.unmodifiableSet(capturedLocks);
  }

  /** returns a collection of variables used in the block.
   * For soundness this must be a superset of the actually used variables. */
  public Set<String> getVariables() {
    if (variables == null) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (ReferencedVariable referencedVar : getReferencedVariables()) {
        builder.add(referencedVar.getName());
      }
      variables = builder.build();
    }
    return variables;
  }

  /**
   * returns a collection of variables used in the block. For soundness this must be a superset of
   * the actually used variables.
   */
  public Set<String> getOutOfScopeVariables() {
    if (outOfScopeVariables == null) {
      Set<ASimpleDeclaration> declarations = new LinkedHashSet<>();
      for (CFANode node : nodes) {
        declarations.addAll(node.getOutOfScopeVariables());
        if (node instanceof FunctionExitNode) {
          declarations.addAll(((FunctionExitNode) node).getEntryNode().getFunctionParameters());
        }
        // TODO should we also handle a function return variable?
      }
      outOfScopeVariables =
          ImmutableSet
              .copyOf(Iterables.transform(declarations, ASimpleDeclaration::getQualifiedName));
    }
    return outOfScopeVariables;
  }

  /** returns a collection of function names used in the block. */
  public Set<String> getFunctions() {
    if (functions == null) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (CFANode node : getNodes()) {
        builder.add(node.getFunctionName());
      }
      functions = builder.build();
    }
    return functions;
  }

  /** get all nodes that are part of this block, including transitive function blocks. */
  public Set<CFANode> getNodes() {
    return nodes;
  }

  public boolean isReturnNode(CFANode pNode) {
    return returnNodes.contains(pNode);
  }

  public Set<CFANode> getReturnNodes() {
    return returnNodes;
  }

  public boolean isCallNode(CFANode pNode) {
    return callNodes.contains(pNode);
  }

  @Override
  public String toString() {
    return "Block " +
            "(CallNodes: " + callNodes + ") " +
            "(Nodes: " + (nodes.size() < 10 ? nodes : "[#=" + nodes.size() + "]") + ") " +
            "(ReturnNodes: " + returnNodes + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Block)) {
      return false;
    }
    Block other = (Block) o;
    // optimization: first compare the smaller collections like call- or return-nodes
    return callNodes.equals(other.callNodes)
        && returnNodes.equals(other.returnNodes)
        && nodes.equals(other.nodes)
        && referencedVariables.equals(other.referencedVariables)
        && memoryLocations.equals(other.memoryLocations);
  }

  @Override
  public int hashCode() {
    return nodes.hashCode();
  }

  public Set<MemoryLocation> getMemoryLocations() {
    return memoryLocations;
  }
}
