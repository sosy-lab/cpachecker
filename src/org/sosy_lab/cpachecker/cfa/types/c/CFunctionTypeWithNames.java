/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.types.c;

import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;

/**
 * This is a subclass of {@link CFunctionType} that is necessary during AST
 * creation. The difference is that it also stores the names of parameters,
 * not only their types.
 * It should not be used outside the cfa package.
 */
public final class CFunctionTypeWithNames extends CFunctionType implements CType {

  private static final long serialVersionUID = -3585082910610497708L;

  @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "writeReplace() takes care of this")
  private final List<CParameterDeclaration> parameters;

  public CFunctionTypeWithNames(
      boolean pConst,
      boolean pVolatile,
      CType pReturnType,
      List<CParameterDeclaration> pParameters,
      boolean pTakesVarArgs) {

    super(
        pConst,
        pVolatile,
        pReturnType,
        FluentIterable.from(pParameters).transform(CParameterDeclaration::getType).toList(),
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
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(parameters);
    result = prime * result + super.hashCode();
    return result;
  }

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
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
    return new CFunctionType(
        isConst(), isVolatile(), getReturnType(), getParameters(), takesVarArgs());
  }
}
