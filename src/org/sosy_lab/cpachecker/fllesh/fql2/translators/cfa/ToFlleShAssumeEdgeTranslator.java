package org.sosy_lab.cpachecker.fllesh.fql2.translators.cfa;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.IASTCompoundStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDefinition;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTProblem;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fllesh.cfa.FlleShAssumeEdge;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.fllesh.fql2.translators.c.PredicateTranslator;
import org.sosy_lab.cpachecker.util.CParser;
import org.sosy_lab.cpachecker.util.CParser.Dialect;

public class ToFlleShAssumeEdgeTranslator {

  private static Map<String, IASTExpression> mExpressionCache = new HashMap<String, IASTExpression>();
  
  public static FlleShAssumeEdge translate(CFANode pNode, ECPPredicate pPredicate) {
    String lPredicateFunction = PredicateTranslator.translate(pPredicate.getPredicate());

    IASTExpression lPredicateExpression;
    
    if (mExpressionCache.containsKey(lPredicateFunction)) {
      lPredicateExpression = mExpressionCache.get(lPredicateFunction);
    }
    else {
      IASTTranslationUnit ast;
      try {
         ast = CParser.parseString(lPredicateFunction, Dialect.C99);
      } catch (CoreException e) {
        throw new RuntimeException("Error during parsing C code \""
            + lPredicateFunction + "\": " + e.getMessage());
      }

      checkForASTProblems(ast);
      
      lPredicateExpression = stripFunctionDeclaration(ast);

      mExpressionCache.put(lPredicateFunction, lPredicateExpression);
    }
    
    return new FlleShAssumeEdge(pNode, lPredicateExpression);
  }
  
  private static void checkForASTProblems(IASTNode pAST) {
    if (pAST instanceof IASTProblem) {
      throw new RuntimeException("Error during parsing C code \""
          + pAST.getRawSignature() + "\": " + ((IASTProblem)pAST).getMessage());
    } else {
      for (IASTNode n : pAST.getChildren()) {
        checkForASTProblems(n);
      }
    }
  }
  
  private static IASTExpression stripFunctionDeclaration(IASTTranslationUnit ast) {
    IASTDeclaration[] declarations = ast.getDeclarations();
    if (   declarations == null
        || declarations.length != 1
        || !(declarations[0] instanceof IASTFunctionDefinition)) {
      throw new RuntimeException("Error: AST does not match the expectations");
    }

    IASTFunctionDefinition func = (IASTFunctionDefinition)declarations[0];
    if (   !func.getDeclarator().getName().getRawSignature().equals("predicate")
        || !(func.getBody() instanceof IASTCompoundStatement)) {
      throw new RuntimeException("Error: AST does not match the expectations");
    }

    IASTStatement[] body = ((IASTCompoundStatement)func.getBody()).getStatements();
    if (!(body.length == 2 && body[1] == null || body.length == 1)) {
      throw new RuntimeException("Error: AST does not match the expectations");
    }
    
    if (!(body[0] instanceof IASTExpressionStatement)) {
      throw new RuntimeException("Error: AST does not match the expectations");
    }
    
    return ((IASTExpressionStatement)body[0]).getExpression();
  }
  
}
