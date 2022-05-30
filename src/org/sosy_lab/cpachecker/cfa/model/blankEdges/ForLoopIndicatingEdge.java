// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.blankEdges;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/** Class representing edges that indicate a for loop. */
public class ForLoopIndicatingEdge extends BlankEdge {

  private static final long serialVersionUID = 4618397883167225835L;

  public ForLoopIndicatingEdge(
      FileLocation pFileLocation, CFANode pPredecessor, CFANode pSuccessor) {
    super("", pFileLocation, pPredecessor, pSuccessor, "for");
  }
}
