// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.java;

import com.google.common.base.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class JDeclarationEdge extends ADeclarationEdge {



  private static final long serialVersionUID = -3744160417799490039L;

  public JDeclarationEdge(final String pRawSignature, final FileLocation pFileLocation,
      final CFANode pPredecessor, final CFANode pSuccessor, final JDeclaration pDeclaration) {

    super(pRawSignature, pFileLocation, pPredecessor, pSuccessor, pDeclaration);

  }

  @Override
  public JDeclaration getDeclaration() {
    return (JDeclaration) declaration;
  }

  @Override
  public Optional<JDeclaration> getRawAST() {
    return Optional.of((JDeclaration)declaration);
  }
}
