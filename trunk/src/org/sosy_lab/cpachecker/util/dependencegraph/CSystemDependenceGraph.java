// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Represents a {@link SystemDependenceGraph} for C programs. */
public final class CSystemDependenceGraph
    extends SystemDependenceGraph<MemoryLocation, CSystemDependenceGraph.Node> {

  CSystemDependenceGraph(SystemDependenceGraph<MemoryLocation, CSystemDependenceGraph.Node> pSdg) {
    super(pSdg);
  }

  public static final class Node
      extends SystemDependenceGraph.Node<AFunctionDeclaration, CFAEdge, MemoryLocation> {

    Node(SystemDependenceGraph.Node<AFunctionDeclaration, CFAEdge, MemoryLocation> pNode) {
      super(pNode);
    }
  }

  public interface ForwardsVisitor
      extends SystemDependenceGraph.ForwardsVisitor<CSystemDependenceGraph.Node> {}

  public interface BackwardsVisitor
      extends SystemDependenceGraph.BackwardsVisitor<CSystemDependenceGraph.Node> {}
}
