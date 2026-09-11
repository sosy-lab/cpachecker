// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.Serial;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibParameterDeclaration extends AParameterDeclaration
    implements SvLibSimpleDeclaration {
  @Serial private static final long serialVersionUID = -720428046149807846L;
  private @Nullable String qualifiedName;

  public SvLibParameterDeclaration(FileLocation pFileLocation, SvLibType pType, String pName) {
    super(pFileLocation, pType, pName);
  }

  @Override
  public SvLibType getType() {
    return (SvLibType) super.getType();
  }

  public void setQualifiedName(String pQualifiedName) {
    checkState(qualifiedName == null);
    qualifiedName = checkNotNull(pQualifiedName);
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(qualifiedName) * 31 * super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibParameterDeclaration other
        && super.equals(obj)
        && Objects.equals(qualifiedName, other.qualifiedName);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }
}
