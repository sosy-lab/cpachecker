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
package org.sosy_lab.cpachecker.fshell.fql2.ast.filter;

public interface FilterVisitor<T> {

  public T visit(Identity pIdentity);
  public T visit(File pFileFilter);
  public T visit(BasicBlockEntry pBasicBlockEntry);
  public T visit(ConditionEdge pConditionEdge);
  public T visit(ConditionGraph pConditionGraph);
  public T visit(DecisionEdge pDecisionEdge);
  public T visit(Line pLine);
  public T visit(FunctionCalls pCalls);
  public T visit(Column pColumn);
  public T visit(Function pFunc);
  public T visit(FunctionCall pCall);
  public T visit(FunctionEntry pEntry);
  public T visit(FunctionExit pExit);
  public T visit(Label pLabel);
  public T visit(Expression pExpression);
  public T visit(RegularExpression pRegularExpression);
  public T visit(Complement pComplement);
  public T visit(Union pUnion);
  public T visit(Compose pCompose);
  public T visit(Intersection pIntersection);
  public T visit(SetMinus pSetMinus);
  public T visit(EnclosingScopes pEnclosingScopes);
  public T visit(Predication pPredication);

}
