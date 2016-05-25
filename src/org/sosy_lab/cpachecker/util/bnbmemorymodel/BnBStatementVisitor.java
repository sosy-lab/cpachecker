/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.bnbmemorymodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class BnBStatementVisitor implements CStatementVisitor<Void, BnBException> {
  private final BnBExpressionVisitor visitor = new BnBExpressionVisitor();
  private final BnBMapMerger merger = new BnBMapMerger();

  private Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visitResult = new HashMap();

  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> getVisitResult() {
    return visitResult;
  }

  public void clearVisitResult() {
    visitResult.clear();
  }

  @Override
  public Void visit(CExpressionStatement pIastExpressionStatement) throws BnBException {
    visitor.clearVisitResult();

    pIastExpressionStatement.getExpression().accept(visitor);

    visitResult = merger.mergeMaps(visitResult, visitor.getVisitResult());
    return null;
  }

  @Override
  public Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) throws BnBException {
    visitor.clearVisitResult();

    pIastExpressionAssignmentStatement.getLeftHandSide().accept(visitor);
    pIastExpressionAssignmentStatement.getRightHandSide().accept(visitor);

    visitResult = merger.mergeMaps(visitResult, visitor.getVisitResult());
    return null;
  }

  @Override
  public Void visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) throws BnBException {
    visitor.clearVisitResult();

    pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(visitor);
    for (CExpression param : pIastFunctionCallAssignmentStatement.getFunctionCallExpression().getParameterExpressions()){
      param.accept(visitor);
    }

    visitResult = merger.mergeMaps(visitResult, visitor.getVisitResult());
    return null;
  }

  @Override
  public Void visit(CFunctionCallStatement pIastFunctionCallStatement) throws BnBException {
    visitor.clearVisitResult();

    for (CExpression param : pIastFunctionCallStatement.getFunctionCallExpression().getParameterExpressions()){
      param.accept(visitor);
    }

    visitResult = merger.mergeMaps(visitResult, visitor.getVisitResult());
    return null;
  }
}
