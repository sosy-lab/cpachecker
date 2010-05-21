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

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Intersection;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Sequence;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.SetMinus;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.States;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Edges;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Paths;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.ConditionalCoverage;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Union;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.FilterVisitor;
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

public interface ASTVisitor<T> extends FilterVisitor<T> {

  // predicates
  public T visit(Predicate pPredicate);
  public T visit(CIdentifier pCIdentifier);
  public T visit(NaturalNumber pNaturalNumber);
  public T visit(Predicates pPredicates);

  // coverage expressions
  public T visit(States pStates);
  public T visit(Edges pEdges);
  public T visit(Paths pPaths);
  public T visit(SetMinus pSetMinus);
  public T visit(Union pUnion);
  public T visit(Intersection pIntersection);
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
