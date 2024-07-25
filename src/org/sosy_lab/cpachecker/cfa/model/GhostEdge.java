// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class GhostEdge extends BlankEdge {

  public GhostEdge(CFANode pPredecessor, CFANode pSuccessor) {
    super("", FileLocation.DUMMY, pPredecessor, pSuccessor, "<<ghost-edge>>");
  }
}
