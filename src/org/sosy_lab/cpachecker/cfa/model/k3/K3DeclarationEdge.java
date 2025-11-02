// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.k3;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Declaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public final class K3DeclarationEdge extends ADeclarationEdge implements K3CfaEdge {
  public K3DeclarationEdge(
      String pRawSignature,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      K3Declaration pDeclaration) {
    super(pRawSignature, pFileLocation, pPredecessor, pSuccessor, pDeclaration);
  }

  @Override
  public K3Declaration getDeclaration() {
    return (K3Declaration) declaration;
  }
}
