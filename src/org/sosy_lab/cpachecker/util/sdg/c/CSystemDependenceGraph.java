// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg.c;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.sdg.SdgEdge;
import org.sosy_lab.cpachecker.util.sdg.SdgNode;
import org.sosy_lab.cpachecker.util.sdg.SystemDependenceGraph;
import org.sosy_lab.cpachecker.util.sdg.traversal.BackwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.ForwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Represents a {@link SystemDependenceGraph} for C programs. */
public final class CSystemDependenceGraph
    extends SystemDependenceGraph<
        MemoryLocation, CSystemDependenceGraph.Node, CSystemDependenceGraph.Edge> {

  CSystemDependenceGraph(
      SystemDependenceGraph<
              MemoryLocation, CSystemDependenceGraph.Node, CSystemDependenceGraph.Edge>
          pSdg) {
    super(pSdg);
  }

  public static final class Node extends SdgNode<AFunctionDeclaration, CFAEdge, MemoryLocation> {

    Node(SdgNode<AFunctionDeclaration, CFAEdge, MemoryLocation> pNode) {
      super(pNode);
    }
  }

  public static final class Edge extends SdgEdge<MemoryLocation> {

    Edge(SdgEdge<MemoryLocation> pEdge) {
      super(pEdge);
    }
  }

  public interface ForwardsVisitor
      extends ForwardsSdgVisitor<
          MemoryLocation, CSystemDependenceGraph.Node, CSystemDependenceGraph.Edge> {}

  public interface BackwardsVisitor
      extends BackwardsSdgVisitor<
          MemoryLocation, CSystemDependenceGraph.Node, CSystemDependenceGraph.Edge> {}
}
