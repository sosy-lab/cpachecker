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
package org.sosy_lab.cpachecker.fshell.fql2.translators.cfa;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.util.predicates.simpleformulas.translators.c.PredicateTranslator;

public class ToFlleShAssumeEdgeTranslator {

  private static Map<String, IASTExpression> mExpressionCache = new HashMap<String, IASTExpression>();

  public static AssumeEdge translate(CFANode pNode, ECPPredicate pPredicate) {
    String lPredicateFunction = PredicateTranslator.translate(pPredicate.getPredicate());

    IASTExpression lPredicateExpression;

    if (mExpressionCache.containsKey(lPredicateFunction)) {
      lPredicateExpression = mExpressionCache.get(lPredicateFunction);
    }
    else {
      IASTNode statement;
      try {
        CParser parser = CParser.Factory.getParser(null, CParser.Factory.getDefaultOptions());
        statement = parser.parseSingleStatement(lPredicateFunction);
      } catch (ParserException e) {
        throw new RuntimeException("Error during parsing C code \""
            + lPredicateFunction + "\": " + e.getMessage());
      }

      if (!(statement instanceof IASTExpressionStatement)) {
        throw new RuntimeException("Error: AST does not match the expectations");
      }

      lPredicateExpression = ((IASTExpressionStatement)statement).getExpression();

      mExpressionCache.put(lPredicateFunction, lPredicateExpression);
    }

    return new AssumeEdge(lPredicateExpression.toASTString(), pNode.getLineNumber(), pNode, pNode, lPredicateExpression, true);
  }
}
