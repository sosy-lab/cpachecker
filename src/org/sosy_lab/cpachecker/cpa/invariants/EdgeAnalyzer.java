/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.collect.Iterables;


public class EdgeAnalyzer {

  /**
   * Gets the variables involved in the given edge.
   *
   * @param pCfaEdge the edge to be analyzed.
   * @param pVariableClassification the variable classification.
   *
   * @return the variables involved in the given edge.
   */
  public static Map<String, CType> getInvolvedVariables(CFAEdge pCfaEdge) {
    switch (pCfaEdge.getEdgeType()) {
    case AssumeEdge: {
      AssumeEdge assumeEdge = (AssumeEdge) pCfaEdge;
      AExpression expression = assumeEdge.getExpression();
      return getInvolvedVariables(expression, pCfaEdge);
    }
    case MultiEdge: {
      MultiEdge multiEdge = (MultiEdge) pCfaEdge;
      Map<String, CType> result = new HashMap<>();
      for (CFAEdge edge : multiEdge) {
        result.putAll(getInvolvedVariables(edge));
      }
      return result;
    }
    case DeclarationEdge: {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) pCfaEdge;
      ADeclaration declaration = declarationEdge.getDeclaration();
      if (declaration instanceof CVariableDeclaration) {
        CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;
        String declaredVariable = variableDeclaration.getQualifiedName();
        CType type = variableDeclaration.getType();
        CInitializer initializer = variableDeclaration.getInitializer();
        if (initializer == null) {
          return Collections.singletonMap(declaredVariable, type);
        }
        Map<String, CType> result = new HashMap<>();
        result.put(declaredVariable, type);
        result.putAll(getInvolvedVariables(initializer, pCfaEdge));
        return result;
      } else if (declaration instanceof AVariableDeclaration) {
        throw new UnsupportedOperationException("Only C expressions are supported");
      } else {
        return Collections.emptyMap();
      }
    }
    case FunctionCallEdge: {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) pCfaEdge;
      Map<String, CType> result = new HashMap<>();

      // Extract arguments
      String callerFunctionName = pCfaEdge.getPredecessor().getFunctionName();
      for (AExpression argument : functionCallEdge.getArguments()) {
        result.putAll(getInvolvedVariables(argument,
            new VariableNameExtractor(
                callerFunctionName,
                Collections.<String, InvariantsFormula<CompoundInterval>>emptyMap())));
      }

      // Extract formal parameters
      for (AParameterDeclaration parameter : functionCallEdge.getSuccessor().getFunctionParameters()) {
        result.putAll(getInvolvedVariables(parameter, pCfaEdge));
      }

      return result;
    }
    case ReturnStatementEdge: {
      AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) pCfaEdge;
      if (returnStatementEdge.getExpression().isPresent()) {
        AExpression returnExpression = returnStatementEdge.getExpression().get();
        Map<String, CType> result = new HashMap<>();
        result.put(VariableNameExtractor.scope(InvariantsTransferRelation.RETURN_VARIABLE_BASE_NAME, pCfaEdge.getSuccessor().getFunctionName()),
            (CType) returnExpression.getExpressionType());
        result.putAll(getInvolvedVariables(returnExpression, pCfaEdge));
        return result;
      }
      return Collections.emptyMap();
    }
    case StatementEdge: {
      AStatementEdge statementEdge = (AStatementEdge) pCfaEdge;
      AStatement statement = statementEdge.getStatement();
      if (statement instanceof AExpressionAssignmentStatement) {
        AExpressionAssignmentStatement expressionAssignmentStatement = (AExpressionAssignmentStatement) statement;
        Map<String, CType> result = new HashMap<>();
        result.putAll(getInvolvedVariables(expressionAssignmentStatement.getLeftHandSide(), pCfaEdge));
        result.putAll(getInvolvedVariables(expressionAssignmentStatement.getRightHandSide(), pCfaEdge));
        return result;
      } else if (statement instanceof AExpressionStatement) {
        return getInvolvedVariables(((AExpressionStatement) statement).getExpression(), pCfaEdge);
      } else if (statement instanceof AFunctionCallAssignmentStatement) {
        AFunctionCallAssignmentStatement functionCallAssignmentStatement = (AFunctionCallAssignmentStatement) statement;
        Map<String, CType> result = new HashMap<>();
        result.putAll(getInvolvedVariables(functionCallAssignmentStatement.getLeftHandSide(), pCfaEdge));
        AFunctionCallExpression functionCallExpression = functionCallAssignmentStatement.getFunctionCallExpression();
        for (AExpression expression : functionCallExpression.getParameterExpressions()) {
          result.putAll(getInvolvedVariables(expression, pCfaEdge));
        }
        return result;
      } else if (statement instanceof AFunctionCallStatement) {
        AFunctionCallStatement functionCallStatement = (AFunctionCallStatement) statement;
        Map<String, CType> result = new HashMap<>();
        for (AExpression expression : functionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
          result.putAll(getInvolvedVariables(expression, pCfaEdge));
        }
        return result;
      } else {
        return Collections.emptyMap();
      }
    }
    case FunctionReturnEdge:
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) pCfaEdge;
      AFunctionCall functionCall = functionReturnEdge.getSummaryEdge().getExpression();
      if (functionCall instanceof AFunctionCallAssignmentStatement) {
        AFunctionCallAssignmentStatement functionCallAssignmentStatement =
            (AFunctionCallAssignmentStatement) functionCall;
        AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
        if (functionCallExpression != null) {
          Map<String, CType> result = new HashMap<>();
          result.put(
              VariableNameExtractor.scope(InvariantsTransferRelation.RETURN_VARIABLE_BASE_NAME, pCfaEdge.getPredecessor().getFunctionName()),
              (CType) functionCallExpression.getExpressionType());
          result.putAll(getInvolvedVariables(functionCallAssignmentStatement.getLeftHandSide(), pCfaEdge));
          return result;
        }
      }
      return Collections.emptyMap();
    case BlankEdge:
    case CallToReturnEdge:
    default:
      return Collections.emptyMap();
    }
  }


  private static Map<? extends String, ? extends CType> getInvolvedVariables(AParameterDeclaration pParameter,
      CFAEdge pCFAEdge) {
    if (pParameter.getType() instanceof CType) {
      return Collections.singletonMap(
          new VariableNameExtractor(pCFAEdge).getVarName(pParameter),
          (CType) pParameter.getType());
    }
    return Collections.emptyMap();
  }


  /**
   * Gets the variables involved in the given CInitializer.
   *
   * @param pCInitializer the CInitializer to be analyzed.
   * @param pVariableClassification the variable classification.
   *
   * @return the variables involved in the given CInitializer.
   */
  private static Map<String, CType> getInvolvedVariables(CInitializer pCInitializer, CFAEdge pCfaEdge) {
    if (pCInitializer instanceof CDesignatedInitializer) {
      return getInvolvedVariables(((CDesignatedInitializer) pCInitializer).getRightHandSide(), pCfaEdge);
    } else if (pCInitializer instanceof CInitializerExpression) {
      return getInvolvedVariables(((CInitializerExpression) pCInitializer).getExpression(), pCfaEdge);
    } else if (pCInitializer instanceof CInitializerList) {
      CInitializerList initializerList = (CInitializerList) pCInitializer;
      Map<String, CType> result = new HashMap<>();
      for (CInitializer initializer : initializerList.getInitializers()) {
        result.putAll(getInvolvedVariables(initializer, pCfaEdge));
      }
      return result;
    }
    return Collections.emptyMap();
  }

  /**
   * Gets the variables involved in the given expression.
   *
   * @param pExpression the expression to be analyzed.
   * @param pCFAEdge the CFA edge to obtain the function name from, if required.
   *
   * @return the variables involved in the given expression.
   */
  public static Map<String, CType> getInvolvedVariables(AExpression pExpression, CFAEdge pCFAEdge) {
    return getInvolvedVariables(pExpression,
        new VariableNameExtractor(
            pCFAEdge,
            Collections.<String, InvariantsFormula<CompoundInterval>> emptyMap())
    );
  }

  /**
   * Gets the variables involved in the given expression.
   *
   * @param pExpression the expression to be analyzed.
   * @param pVariableNameExtractor the variable name extractor to be used.
   *
   * @return the variables involved in the given expression.
   */
  public static Map<String, CType> getInvolvedVariables(AExpression pExpression, VariableNameExtractor pVariableNameExtractor) {
    if (pExpression == null) { return Collections.emptyMap(); }
    if (pExpression instanceof CExpression) {
      Map<String, CType> result = new HashMap<>();

      for (ALeftHandSide leftHandSide : ((CExpression) pExpression).accept(LHSVisitor.INSTANCE)) {
        InvariantsFormula<CompoundInterval> formula;
        try {
          formula = ((CExpression) leftHandSide).accept(new ExpressionToFormulaVisitor(
              pVariableNameExtractor));

          for (String variableName : formula.accept(new CollectVarsVisitor<CompoundInterval>())) {
            result.put(variableName, (CType) leftHandSide.getExpressionType());
          }
        } catch (UnrecognizedCodeException e) {
          // Don't record the variable name then
        }
      }

      return result;
    } else {
      throw new UnsupportedOperationException("Only C expressions are supported");
    }
  }


  private static class LHSVisitor
      extends DefaultCExpressionVisitor<Iterable<ALeftHandSide>, RuntimeException> {

    // we have no inner state, so we can use the same instance several times and avoid re-creating it.
    private final static LHSVisitor INSTANCE = new LHSVisitor();

    @Override
    protected Iterable<ALeftHandSide> visitDefault(CExpression pExp) {
      return Collections.emptySet();
    }

    @Override
    public Iterable<ALeftHandSide> visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
      return Collections.<ALeftHandSide> singleton(pIastArraySubscriptExpression);
    }

    @Override
    public Iterable<ALeftHandSide> visit(CFieldReference pIastFieldReference) {
      return Collections.<ALeftHandSide> singleton(pIastFieldReference);
    }

    @Override
    public Iterable<ALeftHandSide> visit(CIdExpression pIastIdExpression) {
      return Collections.<ALeftHandSide> singleton(pIastIdExpression);
    }

    @Override
    public Iterable<ALeftHandSide> visit(CPointerExpression pPointerExpression) {
      return Collections.<ALeftHandSide> singleton(pPointerExpression);
    }

    @Override
    public Iterable<ALeftHandSide> visit(CComplexCastExpression pComplexCastExpression) {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public Iterable<ALeftHandSide> visit(CBinaryExpression pIastBinaryExpression) {
      return Iterables.concat(pIastBinaryExpression.getOperand1().accept(this), pIastBinaryExpression.getOperand2()
          .accept(this));
    }

    @Override
    public Iterable<ALeftHandSide> visit(CCastExpression pIastCastExpression) {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public Iterable<ALeftHandSide> visit(CUnaryExpression pIastUnaryExpression) {
      return pIastUnaryExpression.getOperand().accept(this);
    }
  }
}
