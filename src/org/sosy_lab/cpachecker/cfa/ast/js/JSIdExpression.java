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

import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSType;

/**
 * Class for expressions that represent names of declared constructs. ( e.g. variables, names of
 * methods in Invocation).
 *
 * <p>If possible, it saves a reference to the declaration this name references.
 */
public class JSIdExpression extends AIdExpression implements JSLeftHandSide {
  private static final long serialVersionUID = -1304384825170453033L;

  // TODO refactor to be either abstract or final

  public JSIdExpression(
      final FileLocation pFileLocation,
      final String pName,
      final JSSimpleDeclaration pDeclaration) {
    super(pFileLocation, JSAnyType.ANY, pName, pDeclaration);
    // TODO Refactor, so we do not need null for declaration.
    // (Insert extra classes or objects for unresolvable declarations)
    //assert pDeclaration != null;
  }

  @Override
  @Nullable
  public JSSimpleDeclaration getDeclaration() {
    return (JSSimpleDeclaration) super.getDeclaration();
  }

  @Override
  public JSType getExpressionType() {
    return (JSType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(final JSLeftHandSideVisitor<R, X> v) throws X {
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
    return this == obj || (obj instanceof JSIdExpression && super.equals(obj));
  }
}
