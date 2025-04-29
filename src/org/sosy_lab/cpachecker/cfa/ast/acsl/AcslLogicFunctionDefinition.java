// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslLogicFunctionDefinition extends AcslLogicDefinition {

  @Serial private static final long serialVersionUID = 145671239875456789L;

  public AcslLogicFunctionDefinition(
      FileLocation pFileLocation, AcslFunctionDeclaration pDeclaration, AcslTerm pBody) {
    super(pFileLocation, pDeclaration, pBody);
    checkNotNull(pFileLocation);
    checkNotNull(pDeclaration);
    checkNotNull(pBody);
  }

  @Override
  public AcslFunctionDeclaration getDeclaration() {
    return (AcslFunctionDeclaration) super.getDeclaration();
  }

  @Override
  public AcslTerm getBody() {
    return (AcslTerm) super.getBody();
  }

  @Override
  public <R, X extends Exception> R accept(AcslLogicDefinitionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return getDeclaration().typedDeclarationString()
        + " = "
        + getBody().toASTString(pAAstNodeRepresentation);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return getDeclaration().typedDeclarationString()
        + " = "
        + getBody().toParenthesizedASTString(pAAstNodeRepresentation);
  }
}
