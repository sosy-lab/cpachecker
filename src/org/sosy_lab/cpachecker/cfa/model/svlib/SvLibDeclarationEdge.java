// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public final class SvLibDeclarationEdge extends ADeclarationEdge implements SvLibCfaEdge {
  public SvLibDeclarationEdge(
      String pRawSignature,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      SvLibDeclaration pDeclaration) {
    super(pRawSignature, pFileLocation, pPredecessor, pSuccessor, pDeclaration);
  }

  @Override
  public SvLibDeclaration getDeclaration() {
    return (SvLibDeclaration) declaration;
  }
}
