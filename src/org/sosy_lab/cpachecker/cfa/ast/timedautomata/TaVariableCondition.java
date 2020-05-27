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

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class TaVariableCondition implements CExpression {

  private static final long serialVersionUID = -5222519373831903612L;

  private final List<CExpression> expressions;
  private final FileLocation fileLocation;
  private final CType type;

  /**
   * Creates a condition that represents a conjunction of expressions.
   *
   */
  public TaVariableCondition(FileLocation pFileLocation, List<CExpression> pExpressions) {
    expressions = pExpressions;
    fileLocation = pFileLocation;
    type = CNumericTypes.BOOL;
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> pV) throws X {
    if (pV instanceof TaVariableConditionVisitor) {
      var taVisitor = (TaVariableConditionVisitor<R, X>) pV;
      return taVisitor.visit(this);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CType getExpressionType() {
    return type;
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(boolean pQualified) {
    var expressionStrings =
        expressions.stream().map(expr -> expr.toASTString()).collect(Collectors.toList());
    return String.join(" AND ", expressionStrings);
  }

  @Override
  public String toParenthesizedASTString(boolean pQualified) {
    return "(" + toASTString() + ")";
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> pV) throws X {
    if (pV instanceof TaVariableConditionVisitor) {
      var taVisitor = (TaVariableConditionVisitor<R, X>) pV;
      return taVisitor.visit(this);
    }
    throw new UnsupportedOperationException();
  }

  public List<CExpression> getExpressions() {
    return ImmutableList.copyOf(expressions);
  }
}
