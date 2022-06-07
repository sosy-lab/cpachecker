// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg.c;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.sdg.SdgNode;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class CSdgNode extends SdgNode<AFunctionDeclaration, CFAEdge, MemoryLocation> {

  CSdgNode(SdgNode<AFunctionDeclaration, CFAEdge, MemoryLocation> pNode) {
    super(pNode);
  }
}
