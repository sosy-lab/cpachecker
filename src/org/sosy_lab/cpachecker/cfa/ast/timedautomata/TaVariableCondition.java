/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.timedautomata;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;

public class TaVariableCondition extends AbstractExpression {

  private static final long serialVersionUID = -5222519373831903612L;

  private final List<TaVariableExpression> expressions;

  public TaVariableCondition(FileLocation pFileLocation, List<TaVariableExpression> pExpressions) {
    super(pFileLocation, null);
    expressions = pExpressions;
  }

  public TaVariableCondition(FileLocation pFileLocation, TaVariableExpression pExpression) {
    super(pFileLocation, null);
    expressions = new ArrayList<>(1);
    expressions.add(pExpression);
  }

  @Override
  public <
          R,
          R1 extends R,
          R2 extends R,
          X1 extends Exception,
          X2 extends Exception,
          V extends CExpressionVisitor<R1, X1> & JExpressionVisitor<R2, X2>>
      R accept_(V pV) throws X1, X2 {
    return null;
  }

  @Override
  public String toASTString(boolean pQualified) {
    StringBuilder sb = new StringBuilder();
    for (var expression : expressions) {
      if (sb.length() != 0) {
        sb.append(" AND ");
      }
      sb.append(expression.toASTString());
    }
    return sb.toString();
  }

  @Override
  public <
          R,
          R1 extends R,
          R2 extends R,
          X1 extends Exception,
          X2 extends Exception,
          V extends CAstNodeVisitor<R1, X1> & JAstNodeVisitor<R2, X2>>
      R accept_(V pV) throws X1, X2 {
    return null;
  }
}
