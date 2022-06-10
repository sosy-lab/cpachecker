// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg.c;

import org.sosy_lab.cpachecker.util.sdg.AbstractSystemDependenceGraph;
import org.sosy_lab.cpachecker.util.sdg.SystemDependenceGraph;
import org.sosy_lab.cpachecker.util.sdg.traversal.BackwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.ForwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Represents a {@link SystemDependenceGraph} for C programs. */
public final class CSystemDependenceGraph
    extends AbstractSystemDependenceGraph<MemoryLocation, CSdgNode, CSdgEdge> {

  CSystemDependenceGraph(AbstractSystemDependenceGraph<MemoryLocation, CSdgNode, CSdgEdge> pSdg) {
    super(pSdg);
  }

  public interface ForwardsVisitor extends ForwardsSdgVisitor<MemoryLocation, CSdgNode, CSdgEdge> {}

  public interface BackwardsVisitor
      extends BackwardsSdgVisitor<MemoryLocation, CSdgNode, CSdgEdge> {}
}
