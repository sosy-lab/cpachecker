// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CDeclarationEdge extends ADeclarationEdge implements CCfaEdge {

  private static final long serialVersionUID = 1085083084922071042L;

  public CDeclarationEdge(
      final String pRawSignature,
      final FileLocation pFileLocation,
      final CFANode pPredecessor,
      final CFANode pSuccessor,
      final CDeclaration pDeclaration) {

    super(pRawSignature, pFileLocation, pPredecessor, pSuccessor, pDeclaration);
  }

  @Override
  public CDeclaration getDeclaration() {
    return (CDeclaration) declaration;
  }

  @Override
  public <R, X extends Exception> R accept(CCfaEdgeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }
}
