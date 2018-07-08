/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.js;

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSType;

/**
 * This Expression is used, if either the Run Time Type or Run Time Object of the this (Keyword)
 * Reference is requested. As part of a regular Expression, it denotes the Run Time Object. As Part
 * of a JRunTimeTypeEqualsType Expression, it denotes the Run Time Type.
 */
public final class JSThisExpression extends AbstractExpression implements JSExpression {

  private static final long serialVersionUID = -3327127448924110155L;

  public JSThisExpression(FileLocation pFileLocation) {
    super(pFileLocation, JSAnyType.ANY);
  }

  @Override
  public JSType getExpressionType() {
    return (JSType) super.getExpressionType();
  }

  @Override
  public String toASTString(final boolean pQualified) {
    return "this"; // TODO consider pQualified
  }

  @Override
  public <R, X extends Exception> R accept(final JSExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    final int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    return this == obj || (obj instanceof JSThisExpression && super.equals(obj));
  }
}
