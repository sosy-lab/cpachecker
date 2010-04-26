/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Sequence;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.States;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Edges;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Paths;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.ConditionalCoverage;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.BasicBlockEntry;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Compose;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.EnclosingScopes;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.FunctionCall;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.FunctionCalls;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Column;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.ConditionEdge;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.ConditionGraph;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.DecisionEdge;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.FunctionEntry;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.FunctionExit;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.File;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Function;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Identity;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Intersection;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Label;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Line;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Expression;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.RegularExpression;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Complement;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.SetMinus;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Union;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.Alternative;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.Concatenation;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.ConditionalMonitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.LowerBound;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.UpperBound;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.CIdentifier;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.NaturalNumber;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicates;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.query.Query;

public interface ASTVisitor<T> {
  // filter function expressions
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

  // predicates
  public T visit(Predicate pPredicate);
  public T visit(CIdentifier pCIdentifier);
  public T visit(NaturalNumber pNaturalNumber);
  public T visit(Predicates pPredicates);

  // coverage expressions
  public T visit(States pStates);
  public T visit(Edges pEdges);
  public T visit(Paths pPaths);
  public T visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.SetMinus pSetMinus);
  public T visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Union pUnion);
  public T visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Intersection pIntersection);
  public T visit(ConditionalCoverage pConditionalCoverage);

  public T visit(Sequence pSequence);

  // path monitor expressions
  public T visit(ConditionalMonitor pConditionalMonitor);
  public T visit(Alternative pAlternative);
  public T visit(Concatenation pConcatenation);
  public T visit(UpperBound pUpperBound);
  public T visit(LowerBound pLowerBound);

  // queries
  public T visit(Query pQuery);
}
