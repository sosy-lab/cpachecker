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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.CFileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;


public class JRunTimeTypeEqualsType extends AExpression implements JExpression {

  private final JThisRunTimeType runTimeTypeExpression;
  private final JClassOrInterfaceType typeDef;

  public JRunTimeTypeEqualsType(CFileLocation pFileLocation,
      JThisRunTimeType pRunTimeTypeExpression, JClassOrInterfaceType pTypeDef) {
    super(pFileLocation, new JSimpleType(JBasicType.BOOLEAN));

    runTimeTypeExpression = pRunTimeTypeExpression;
    typeDef = pTypeDef;

    assert runTimeTypeExpression != null;
    assert typeDef != null;
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    StringBuilder astString = new StringBuilder("(");
    astString.append(runTimeTypeExpression.toASTString());
    astString.append("_equals(");
    astString.append(typeDef.getName());
    astString.append("))");
    return astString.toString();
  }
}
