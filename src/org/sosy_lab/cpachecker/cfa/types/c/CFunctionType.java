/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.collect.Iterables.transform;
import static org.sosy_lab.cpachecker.cfa.ast.c.CAstNode.TO_AST_STRING;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class CFunctionType extends AFunctionType implements CType {

  private boolean   isConst;
  private boolean   isVolatile;

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
    result = prime * result + ((getParameters() == null) ? 0 : getParameters().hashCode());
    result = prime * result + ((getReturnType() == null) ? 0 : getReturnType().hashCode());
    result = prime * result + (takesVarArgs() ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    CFunctionType other = (CFunctionType) obj;
    if (getName() == null) {
      if (other.getName() != null)
        return false;
    } else if (!getName().equals(other.getName()))
      return false;
    if (getParameters() == null) {
      if (other.getParameters() != null)
        return false;
    } else if (!getParameters().equals(other.getParameters()))
      return false;
    if (getReturnType() == null) {
      if (other.getReturnType() != null)
        return false;
    } else if (!getReturnType().equals(other.getReturnType()))
      return false;
    if (takesVarArgs() != other.takesVarArgs())
      return false;
    return true;
  }

  public CFunctionType(
      boolean pConst,
      boolean pVolatile,
      CType pReturnType,
      List<CParameterDeclaration> pParameters,
      boolean pTakesVarArgs) {

    super(pReturnType, ImmutableList.copyOf(pParameters), pTakesVarArgs );

    isConst = pConst;
    isVolatile = pVolatile;

  }

  @Override
  public CType getReturnType() {
    return (CType) super.getReturnType();
  }

  @Override
  public void setName(String pName) {
    super.setName(pName);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<CParameterDeclaration> getParameters() {
    return (List<CParameterDeclaration>) super.getParameters();
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public String toASTString(String pDeclarator) {
    StringBuilder lASTString = new StringBuilder();

    if (isConst()) {
      lASTString.append("const ");
    }
    if (isVolatile()) {
      lASTString.append("volatile ");
    }

    lASTString.append(getReturnType().toASTString(""));
    lASTString.append(" ");

    if (pDeclarator.startsWith("*")) {
      // this is a function pointer, insert parentheses
      lASTString.append("(");
      lASTString.append(pDeclarator);
      lASTString.append(")");
    } else {
      lASTString.append(pDeclarator);
    }

    lASTString.append("(");
    Joiner.on(", ").appendTo(lASTString, transform(getParameters(), TO_AST_STRING));
    if (takesVarArgs()) {
      if (!getParameters().isEmpty()) {
        lASTString.append(", ");
      }
      lASTString.append("...");
    }
    lASTString.append(")");

    return lASTString.toString();
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }
}
