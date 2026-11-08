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
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

public final class AcslParameterDeclaration extends AParameterDeclaration
    implements AcslSimpleDeclaration {

  @Serial private static final long serialVersionUID = 145675929875456789L;
  private final String qualifiedName;

  public AcslParameterDeclaration(FileLocation pFileLocation, AcslType pType, String pName) {
    super(checkNotNull(pFileLocation), checkNotNull(pType), checkNotNull(pName));
    qualifiedName = pName;
  }

  public AcslParameterDeclaration(
      FileLocation pFileLocation, AcslType pType, String pName, String pFunctionName) {
    super(checkNotNull(pFileLocation), checkNotNull(pType), checkNotNull(pName));
    qualifiedName = checkNotNull(pFunctionName) + "::" + pName;
  }

  @Override
  public <R, X extends Exception> R accept(AcslSimpleDeclarationVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public AcslType getType() {
    return (AcslType) super.getType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
