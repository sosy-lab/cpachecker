package org.sosy_lab.cpachecker.fshell.fql2.translators.cfa;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
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
      IASTStatement statement;
      try {
        CParser parser = CParser.Factory.getParser(null, CParser.Dialect.C99);
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
    
    return new AssumeEdge(lPredicateExpression.getRawSignature(), pNode.getLineNumber(), pNode, pNode, lPredicateExpression, true);
  }
}
