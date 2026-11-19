// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * We need the metadata of the choice index in SV-LIB choice edges to properly reconstruct
 * nondeterministic choices during the violation witness export. But for all the other purposes,
 * this edge is just a normal blank edge.
 */
public final class SvLibBlankChoiceEdge extends BlankEdge implements SvLibCfaEdge {
  private final int choiceIndex;

  public SvLibBlankChoiceEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      String pDescription,
      int pChoiceIndex) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pDescription);
    choiceIndex = pChoiceIndex;
  }

  public int getChoiceIndex() {
    return choiceIndex;
  }
}
