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

import java.util.Optional;
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class EdgeAnalyzer {

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final MachineModel machineModel;

  public EdgeAnalyzer(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel) {
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.machineModel = pMachineModel;
  }

  /**
   * Gets the variables involved in the given edge.
   *
   * @param pCfaEdge the edge to be analyzed.
   *
   * @return the variables involved in the given edge.
   */
  public Map<MemoryLocation, CType> getInvolvedVariableTypes(CFAEdge pCfaEdge) {
    switch (pCfaEdge.getEdgeType()) {
    case AssumeEdge: {
      AssumeEdge assumeEdge = (AssumeEdge) pCfaEdge;
      AExpression expression = assumeEdge.getExpression();
      return getInvolvedVariableTypes(expression, pCfaEdge);
    }
    case DeclarationEdge: {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) pCfaEdge;
      ADeclaration declaration = declarationEdge.getDeclaration();
      if (declaration instanceof CVariableDeclaration) {
        CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;
        MemoryLocation declaredVariable = MemoryLocation.valueOf(variableDeclaration.getQualifiedName());
        CType type = variableDeclaration.getType();
        CInitializer initializer = variableDeclaration.getInitializer();
        if (initializer == null) {
          return Collections.singletonMap(declaredVariable, type);
        }
        Map<MemoryLocation, CType> result = new HashMap<>();
        result.put(declaredVariable, type);
        result.putAll(getInvolvedVariableTypes(initializer, pCfaEdge));
        return result;
      } else if (declaration instanceof AVariableDeclaration) {
        throw new UnsupportedOperationException("Only C expressions are supported");
      } else {
        return Collections.emptyMap();
      }
    }
    case FunctionCallEdge: {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) pCfaEdge;
      Map<MemoryLocation, CType> result = new HashMap<>();

      // Extract arguments
      String callerFunctionName = pCfaEdge.getPredecessor().getFunctionName();
      for (AExpression argument : functionCallEdge.getArguments()) {
        result.putAll(getInvolvedVariableTypes(argument,
            new MemoryLocationExtractor(
                compoundIntervalManagerFactory,
                machineModel,
                callerFunctionName,
                Collections.<MemoryLocation, NumeralFormula<CompoundInterval>>emptyMap())));
      }

      // Extract formal parameters
      for (AParameterDeclaration parameter : functionCallEdge.getSuccessor().getFunctionParameters()) {
        result.putAll(getInvolvedVariableTypes(parameter, pCfaEdge));
      }

      return result;
    }
    case ReturnStatementEdge: {
      AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) pCfaEdge;
      if (returnStatementEdge.getExpression().isPresent()) {
        AExpression returnExpression = returnStatementEdge.getExpression().get();
        Map<MemoryLocation, CType> result = new HashMap<>();
            Optional<? extends AAssignment> returnAssignment = returnStatementEdge.asAssignment();
            if (returnAssignment.isPresent()) {
              result.putAll(getInvolvedVariableTypes(returnAssignment.get(), pCfaEdge));
            } else {
              Optional<? extends AVariableDeclaration> retVar =
                  returnStatementEdge.getSuccessor().getEntryNode().getReturnVariable();
              if (retVar.isPresent()) {
                CExpression idExpression =
                    new CIdExpression(
                        returnStatementEdge.getFileLocation(), (CSimpleDeclaration) retVar.get());
                result.putAll(getInvolvedVariableTypes(idExpression, pCfaEdge));
              }
        }
        result.putAll(getInvolvedVariableTypes(returnExpression, pCfaEdge));
        return result;
      }
      return Collections.emptyMap();
    }
    case StatementEdge: {
      AStatementEdge statementEdge = (AStatementEdge) pCfaEdge;
      AStatement statement = statementEdge.getStatement();
      if (statement instanceof AExpressionAssignmentStatement) {
            return getInvolvedVariableTypes((AExpressionAssignmentStatement) statement, pCfaEdge);
      } else if (statement instanceof AExpressionStatement) {
        return getInvolvedVariableTypes(((AExpressionStatement) statement).getExpression(), pCfaEdge);
      } else if (statement instanceof AFunctionCallAssignmentStatement) {
            return getInvolvedVariableTypes((AFunctionCallAssignmentStatement) statement, pCfaEdge);
      } else if (statement instanceof AFunctionCallStatement) {
        AFunctionCallStatement functionCallStatement = (AFunctionCallStatement) statement;
        Map<MemoryLocation, CType> result = new HashMap<>();
        for (AExpression expression : functionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
          result.putAll(getInvolvedVariableTypes(expression, pCfaEdge));
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
          Map<MemoryLocation, CType> result = new HashMap<>();
          Optional<? extends AVariableDeclaration> retVar = functionReturnEdge.getFunctionEntry().getReturnVariable();
          if (retVar.isPresent()) {
              AExpression idExpression =
                  new CIdExpression(
                      functionReturnEdge.getFileLocation(), (CSimpleDeclaration) retVar.get());
              idExpression =
                  ExpressionToFormulaVisitor.makeCastFromArrayToPointerIfNecessary(
                      idExpression,
                      functionCallAssignmentStatement.getLeftHandSide().getExpressionType());
              result.putAll(
                  getInvolvedVariableTypes(
                      idExpression,
                      new MemoryLocationExtractor(
                          compoundIntervalManagerFactory,
                          machineModel,
                          functionReturnEdge.getPredecessor().getFunctionName(),
                          Collections
                              .<MemoryLocation, NumeralFormula<CompoundInterval>>emptyMap())));
          }
          result.putAll(getInvolvedVariableTypes(functionCallAssignmentStatement.getLeftHandSide(), pCfaEdge));
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

  private Map<MemoryLocation, CType> getInvolvedVariableTypes(
      AAssignment pAssignment, CFAEdge pCfaEdge) {
    if (pAssignment instanceof AExpressionAssignmentStatement) {
      AExpressionAssignmentStatement expressionAssignmentStatement =
          (AExpressionAssignmentStatement) pAssignment;
      Map<MemoryLocation, CType> result = new HashMap<>();
      result.putAll(
          getInvolvedVariableTypes(expressionAssignmentStatement.getLeftHandSide(), pCfaEdge));
      result.putAll(
          getInvolvedVariableTypes(
              ExpressionToFormulaVisitor.makeCastFromArrayToPointerIfNecessary(
                  expressionAssignmentStatement.getRightHandSide(),
                  expressionAssignmentStatement.getLeftHandSide().getExpressionType()),
              pCfaEdge));
      return result;
    }
    if (pAssignment instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement functionCallAssignmentStatement =
          (AFunctionCallAssignmentStatement) pAssignment;
      Map<MemoryLocation, CType> result = new HashMap<>();
      result.putAll(
          getInvolvedVariableTypes(functionCallAssignmentStatement.getLeftHandSide(), pCfaEdge));
      AFunctionCallExpression functionCallExpression =
          functionCallAssignmentStatement.getFunctionCallExpression();
      for (AExpression expression : functionCallExpression.getParameterExpressions()) {
        result.putAll(getInvolvedVariableTypes(expression, pCfaEdge));
      }
      return result;
    }
    return Collections.emptyMap();
  }

  private Map<? extends MemoryLocation, ? extends CType> getInvolvedVariableTypes(AParameterDeclaration pParameter,
      CFAEdge pCFAEdge) {
    if (pParameter.getType() instanceof CType) {
      return Collections.singletonMap(
          new MemoryLocationExtractor(
              compoundIntervalManagerFactory,
              machineModel,
              pCFAEdge).getMemoryLocation(pParameter),
          (CType) pParameter.getType());
    }
    return Collections.emptyMap();
  }


  /**
   * Gets the variables involved in the given CInitializer.
   *
   * @param pCInitializer the CInitializer to be analyzed.
   *
   * @return the variables involved in the given CInitializer.
   */
  private Map<MemoryLocation, CType> getInvolvedVariableTypes(CInitializer pCInitializer, CFAEdge pCfaEdge) {
    if (pCInitializer instanceof CDesignatedInitializer) {
      return getInvolvedVariableTypes(((CDesignatedInitializer) pCInitializer).getRightHandSide(), pCfaEdge);
    } else if (pCInitializer instanceof CInitializerExpression) {
      return getInvolvedVariableTypes(((CInitializerExpression) pCInitializer).getExpression(), pCfaEdge);
    } else if (pCInitializer instanceof CInitializerList) {
      CInitializerList initializerList = (CInitializerList) pCInitializer;
      Map<MemoryLocation, CType> result = new HashMap<>();
      for (CInitializer initializer : initializerList.getInitializers()) {
        result.putAll(getInvolvedVariableTypes(initializer, pCfaEdge));
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
  public Map<MemoryLocation, CType> getInvolvedVariableTypes(AExpression pExpression, CFAEdge pCFAEdge) {
    return getInvolvedVariableTypes(pExpression,
        new MemoryLocationExtractor(
            compoundIntervalManagerFactory,
            machineModel,
            pCFAEdge,
            Collections.<MemoryLocation, NumeralFormula<CompoundInterval>> emptyMap())
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
  public Map<MemoryLocation, CType> getInvolvedVariableTypes(AExpression pExpression, MemoryLocationExtractor pVariableNameExtractor) {
    if (pExpression == null) { return Collections.emptyMap(); }
    if (pExpression instanceof CExpression) {
      Map<MemoryLocation, CType> result = new HashMap<>();

      for (ALeftHandSide leftHandSide : ((CExpression) pExpression).accept(LHSVisitor.INSTANCE)) {
        NumeralFormula<CompoundInterval> formula;
        try {
          ExpressionToFormulaVisitor etfv =
              new ExpressionToFormulaVisitor(
                  compoundIntervalManagerFactory, machineModel, pVariableNameExtractor);
          formula = ((CExpression) leftHandSide).accept(etfv);

          for (MemoryLocation memoryLocation : formula.accept(COLLECT_VARS_VISITOR)) {
            result.put(memoryLocation, (CType) leftHandSide.getExpressionType());
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
      CExpression operand1 = pIastBinaryExpression.getOperand1();
      CExpression operand2 = pIastBinaryExpression.getOperand2();
      operand1 =
          ExpressionToFormulaVisitor.makeCastFromArrayToPointerIfNecessary(
              operand1, pIastBinaryExpression.getCalculationType());
      operand2 =
          ExpressionToFormulaVisitor.makeCastFromArrayToPointerIfNecessary(
              operand2, pIastBinaryExpression.getCalculationType());
      return Iterables.concat(
          operand1.<Iterable<ALeftHandSide>, RuntimeException>accept(this),
          operand2.<Iterable<ALeftHandSide>, RuntimeException>accept(this));
    }

    @Override
    public Iterable<ALeftHandSide> visit(CCastExpression pIastCastExpression) {
      return ExpressionToFormulaVisitor.makeCastFromArrayToPointerIfNecessary(
              pIastCastExpression.getOperand(), pIastCastExpression.getCastType())
          .accept(this);
    }

    @Override
    public Iterable<ALeftHandSide> visit(CUnaryExpression pIastUnaryExpression) {
      CExpression operand = pIastUnaryExpression.getOperand();
      if (pIastUnaryExpression.getOperator() != UnaryOperator.AMPER) {
        operand =
            ExpressionToFormulaVisitor.makeCastFromArrayToPointerIfNecessary(
                operand, pIastUnaryExpression.getExpressionType());
      }
      return operand.accept(this);
    }
  }
}
