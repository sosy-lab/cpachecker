/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSType;

/**
 * This class represents the boolean literal AST node type.
 *
 * <p>BooleanLiteral: true false
 */
public final class JSBooleanLiteralExpression extends ALiteralExpression
    implements JSLiteralExpression {

  private final Boolean value;

  public JSBooleanLiteralExpression(FileLocation pFileLocation, boolean pValue) {
    super(pFileLocation, JSAnyType.ANY);
    value = pValue;
  }

  @Override
  public Boolean getValue() {
    return value;
  }

  @Override
  public String toASTString() {
    if (value) {
      return "true";
    } else {
      return "false";
    }
  }

  @Override
  public JSType getExpressionType() {
    return (JSType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JSExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hashCode(value);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JSBooleanLiteralExpression) || !super.equals(obj)) {
      return false;
    }

    JSBooleanLiteralExpression other = (JSBooleanLiteralExpression) obj;

    return Objects.equals(other.value, value);
  }
}
