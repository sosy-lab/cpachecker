// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;

/**
 * This is a subclass of {@link CFunctionType} that is necessary during AST creation. The difference
 * is that it also stores the names of parameters, not only their types. It should not be used
 * outside the cfa package.
 */
public final class CFunctionTypeWithNames extends CFunctionType implements CType {

  private static final long serialVersionUID = -3585082910610497708L;

  @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "writeReplace() takes care of this")
  private final List<CParameterDeclaration> parameters;

  public CFunctionTypeWithNames(
      CType pReturnType, List<CParameterDeclaration> pParameters, boolean pTakesVarArgs) {

    super(
        pReturnType,
        transformedImmutableListCopy(pParameters, CParameterDeclaration::getType),
        pTakesVarArgs);

    parameters = ImmutableList.copyOf(pParameters);
  }

  public List<CParameterDeclaration> getParameterDeclarations() {
    return parameters;
  }

  @Override
  public String toString() {
    return toASTString(Strings.nullToEmpty(getName()), getParameterDeclarations());
  }

  @Override
  public String toASTString(String pDeclarator) {
    return toASTString(pDeclarator, getParameterDeclarations());
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(parameters) * 31 + super.hashCode();
  }

  /**
   * Be careful, this method compares the CType as it is to the given object, typedefs won't be
   * resolved. If you want to compare the type without having typedefs in it use
   * #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CFunctionTypeWithNames) || !super.equals(obj)) {
      return false;
    }

    CFunctionTypeWithNames other = (CFunctionTypeWithNames) obj;

    return Objects.equals(parameters, other.parameters);
  }

  private Object writeReplace() {
    // Cannot serialize parameter names, but typically this is not necessary anyway.
    return new CFunctionType(getReturnType(), getParameters(), takesVarArgs());
  }
}
