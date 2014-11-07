/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate.synthesis.arithmethic;

import java.util.Collections;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

import scala.collection.mutable.MultiMap;


// Simple solver for LINEAR arithmetic (this can be replaced later by a library)
public class NaiveArithmethicSolver implements ExpressionSolver {

  @SuppressWarnings("unused")
  @Override
  public Set<CBinaryExpression> solve(Set<CBinaryExpression> pRelations, Set<CIdExpression> pSolveTo)
      throws SolvingFailedException {

    final MultiMap<CIdExpression, Pair<CBinaryExpression.BinaryOperator, CExpression>> resolved;

    // 1. Substitute all sub-expressions and create new relations (skolemization).
    //    Goal: IdExpression on the LHS
    // Examples:
    //    [b-c > 1000, c > 2] gets transformed to (iterations):
    //      [x1 > 1000, x1 = b-c, c > 2]
    //      [x1 > 1000, x1 = b-c, b = x1+c, c = b-x1, c > 2]
    //    [5*(f+h) > a+1+2] gets transformed to (iterations):   (NOOOOO LINEAR Arithmetic!!! Should fail!!!!!)
    //      [5*(f+h) > a+1+2]
    //      [5*(f+h) > a+x1, x1 = 1+2]
    //      [5*(f+h) > x2, x2 = a+x1, x1 = 1+2]   // The goal would be reached here...
    //  (an additional goal is needed: all variables in pSolveTo must be the only expression on a LHS or RHS)
    //      [5*x3 > x2, x3 = f+h, x2 = a+x1, x1 = 1+2]
    //      [5*x3 > x2, x3 = f+h, x3-h = f, x3-f = h, x2 = a+x1, x1 = 1+2]

    for (CIdExpression id: pSolveTo) {
      // 2. Derive expressions for the IDs in pSolveTo.
      //    This is done by inlining and the application deduction rules.
      //  Example: pSolveTo [b] for pRelations: [b-c > 1000, c > 2]
      //    [b=x1+c]
      //      can be combined with [x1 > 1000, x1 = b-c, c = b-x1, c > 2]
      //      candidates that would lead to a self-reference have to be removed
      //        --> [x1 > 1000, c > 2]
      //      symbolic deduction rules can be applied for inequalities:
      //        rules:
      //            ({a=b-c, c>x} <==> {b-a>x})
      //            ({a=b+c, c>x} <==> {a-b>x})
      //            ({a=FOO, a>x} <==> {FOO>x}) // FOO is an arbitrary (binary or unary) expression
      //        application:
      //            [b = x1+c][c > 2, x1 > 1000]
      //              <==> [b-x1 > 2][x1 > 1000]
      //              <==> [b-2 > 1000][]
    }

    return Collections.emptySet();
  }

}
