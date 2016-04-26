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

public class BnBStatementVisitor implements CStatementVisitor<Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>>, BnBException> {
  private final BnBExpressionVisitor visitor = new BnBExpressionVisitor();
  private final BnBMapMerger merger = new BnBMapMerger();

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CExpressionStatement pIastExpressionStatement) throws BnBException {
    return pIastExpressionStatement.getExpression().accept(visitor);
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
      throws BnBException {
    Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> result =
        pIastExpressionAssignmentStatement.getLeftHandSide().accept(visitor);

    Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> right =
        pIastExpressionAssignmentStatement.getRightHandSide().accept(visitor);

    return merger.mergeMaps(result, right);
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
      throws BnBException {
    Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> result = new HashMap<>();
    Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> first =
        pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(visitor);

    if (!(first == null || first.isEmpty())) {
      result.putAll(first);
    }

    for (CExpression param : pIastFunctionCallAssignmentStatement.getFunctionCallExpression().getParameterExpressions()){
      result = merger.mergeMaps(result, param.accept(visitor));
    }

    return result;
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CFunctionCallStatement pIastFunctionCallStatement) throws BnBException {
    Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> result = new HashMap<>();

    for (CExpression param : pIastFunctionCallStatement.getFunctionCallExpression().getParameterExpressions()){
      result = merger.mergeMaps(result, param.accept(visitor));
    }

    return result;
  }
}
