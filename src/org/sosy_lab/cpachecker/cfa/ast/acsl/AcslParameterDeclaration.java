// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

public final class AcslParameterDeclaration extends AParameterDeclaration
    implements AcslSimpleDeclaration {

  @Serial private static final long serialVersionUID = 6290958920195598979L;
  private final String qualifiedName;

  public AcslParameterDeclaration(FileLocation pFileLocation, AcslType pType, String pName) {
    super(checkNotNull(pFileLocation), checkNotNull(pType), checkNotNull(pName));
    checkArgument(!pName.isEmpty(), "A parameter has a name");
    qualifiedName = pName;
  }

  public AcslParameterDeclaration(
      FileLocation pFileLocation, AcslType pType, String pName, String pFunctionName) {
    super(checkNotNull(pFileLocation), checkNotNull(pType), checkNotNull(pName));
    checkArgument(!pName.isEmpty(), "A parameter has a name");
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

  /**
   * Returns this parameter as C parameter declaration if it has a C type, e.g., to use the
   * parameter {@code start} of type {@code struct sll *} in the C expression {@code start->next} of
   * a predicate body (see {@link AcslCExpression}). The returned declaration has the name and
   * qualified name of this parameter.
   */
  public Optional<CParameterDeclaration> asCParameterDeclaration() {
    if (!(getType() instanceof AcslCType cType)) {
      return Optional.empty();
    }
    CParameterDeclaration cParameter =
        new CParameterDeclaration(getFileLocation(), cType.getType(), getName());
    cParameter.setQualifiedName(qualifiedName);
    return Optional.of(cParameter);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
