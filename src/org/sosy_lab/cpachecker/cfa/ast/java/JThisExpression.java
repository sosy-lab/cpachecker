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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;

/**
 * This Expression is used, if either the Run Time Type or Run Time Object
 * of the this (Keyword) Reference  is requested.
 * As part of a regular Expression, it denotes the Run Time Object. As Part of a
 * JRunTimeTypeEqualsType Expression, it denotes the Run Time Type.
 */
public final class JThisExpression extends AbstractExpression implements JRunTimeTypeExpression {

  public JThisExpression(FileLocation pFileLocation, JClassOrInterfaceType pType) {
    super(pFileLocation, pType);
  }

  @Override
  public JClassOrInterfaceType getExpressionType() {
    return (JClassOrInterfaceType) super.getExpressionType();
  }

  @Override
  public String toASTString() {
    return "this";
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public boolean isThisReference() {
    return true;
  }

  @Override
  public boolean isVariableReference() {
    return false;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JThisExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}