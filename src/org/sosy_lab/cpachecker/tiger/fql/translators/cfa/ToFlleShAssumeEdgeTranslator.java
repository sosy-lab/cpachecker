/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.tiger.fql.translators.cfa;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.tiger.fql.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.util.predicates.simpleformulas.translators.c.PredicateTranslator;

public class ToFlleShAssumeEdgeTranslator {

  private static Map<String, CExpression> mExpressionCache = new HashMap<>();

  public static CAssumeEdge translate(CFANode pNode, ECPPredicate pPredicate) {
	  assert false;

	  // TODO we have to adapt the translate method to provide a suitable code fragment!
    String lPredicateFunction = PredicateTranslator.translate(pPredicate.getPredicate());

    CExpression lPredicateExpression;

    if (mExpressionCache.containsKey(lPredicateFunction)) {
      lPredicateExpression = mExpressionCache.get(lPredicateFunction);
    }
    else {
      CAstNode statement;
      try {
    	  CParser parser = CParser.Factory.getParser(null, null, CParser.Factory.getDefaultOptions(), null);
        //CParser parser = CParser.Factory.getParser(null, CParser.Factory.getDefaultOptions());
        //statement = parser.parseSingleStatement(lPredicateFunction);
    	  statement = parser.parseSingleStatement(null);
      } catch (ParserException e) {
        throw new RuntimeException("Error during parsing C code \""
            + lPredicateFunction + "\": " + e.getMessage());
      } catch (InvalidConfigurationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		throw new RuntimeException("invalid configuration");
	}

      if (!(statement instanceof CExpressionStatement)) {
        throw new RuntimeException("Error: AST does not match the expectations");
      }

      lPredicateExpression = ((CExpressionStatement)statement).getExpression();

      mExpressionCache.put(lPredicateFunction, lPredicateExpression);
    }

    return new CAssumeEdge(lPredicateExpression.toASTString(), pNode.getLineNumber(), pNode, pNode, lPredicateExpression, true);
  }
}
