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
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;

public final class AcslCVariableDeclaration extends AVariableDeclaration
    implements AcslDeclaration {

  @Serial private static final long serialVersionUID = -814550243801231276L;

  private CVariableDeclaration variableDeclaration;

  public AcslCVariableDeclaration(CVariableDeclaration pCVariableDeclaration) {
    super(
        pCVariableDeclaration.getFileLocation(),
        pCVariableDeclaration.isGlobal(),
        pCVariableDeclaration.getType(),
        checkNotNull(pCVariableDeclaration.getName()),
        pCVariableDeclaration.getOrigName(),
        pCVariableDeclaration.getQualifiedName(),
        pCVariableDeclaration.getInitializer());
    variableDeclaration = pCVariableDeclaration;
  }

  @Override
  public AcslType getType() {
    return new AcslCType(variableDeclaration.getType());
  }

  @Override
  public <R, X extends Exception> R accept(AcslSimpleDeclarationVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
