// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class ADeclarationEdge extends AbstractCFAEdge {

  private static final long serialVersionUID = 3691647301334179318L;
  protected final ADeclaration declaration;

  protected ADeclarationEdge(
      final String pRawSignature,
      final FileLocation pFileLocation,
      final CFANode pPredecessor,
      final CFANode pSuccessor,
      final ADeclaration pDeclaration) {

    super(pRawSignature, pFileLocation, pPredecessor, pSuccessor);
    declaration = pDeclaration;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.DeclarationEdge;
  }

  public ADeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public Optional<AAstNode> getRawAST() {
    return Optional.of(declaration);
  }

  @Override
  public String getCode() {
    return declaration.toASTString();
  }
}
