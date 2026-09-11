// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
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
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class EdgeAnalyzer {

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR =
      new CollectVarsVisitor<>();

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final MachineModel machineModel;

  public EdgeAnalyzer(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory, MachineModel pMachineModel) {
    compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    machineModel = pMachineModel;
  }

  /**
   * Gets the variables involved in the given edge.
   *
   * @param pCfaEdge the edge to be analyzed.
   * @return the variables involved in the given edge.
   */
  @SuppressWarnings(
      "MixedMutabilityReturnType") // would need lots of copying, result for short-term use only
  Map<MemoryLocation, CType> getInvolvedVariableTypes(CFAEdge pCfaEdge) {
    return switch (pCfaEdge.getEdgeType()) {
      case AssumeEdge -> {
        AssumeEdge assumeEdge = (AssumeEdge) pCfaEdge;
        AExpression expression = assumeEdge.getExpression();
        yield getInvolvedVariableTypes(expression, pCfaEdge);
      }
      case DeclarationEdge -> {
        ADeclarationEdge declarationEdge = (ADeclarationEdge) pCfaEdge;
        yield switch (declarationEdge.getDeclaration()) {
          case CVariableDeclaration variableDeclaration -> {
            MemoryLocation declaredVariable = MemoryLocation.forDeclaration(variableDeclaration);
            CType type = variableDeclaration.getType();
            CInitializer initializer = variableDeclaration.getInitializer();
            if (initializer == null) {
              yield ImmutableMap.of(declaredVariable, type);
            }

            final Map<MemoryLocation, CType> initializerVariableTypes =
                getInvolvedVariableTypes(initializer, pCfaEdge);
            if (initializerVariableTypes.containsKey(declaredVariable)) {
              // happens with "int x = x;"
              assert initializerVariableTypes.get(declaredVariable).equals(type);
              yield initializerVariableTypes;
            }
            yield ImmutableMap.<MemoryLocation, CType>builderWithExpectedSize(
                    initializerVariableTypes.size() + 1)
                .put(declaredVariable, type)
                .putAll(initializerVariableTypes)
                .buildOrThrow();
          }

          case AVariableDeclaration variableDeclaration ->
              throw new UnsupportedOperationException("Only C expressions are supported");
          default -> ImmutableMap.of();
        };
      }
      case FunctionCallEdge -> {
        FunctionCallEdge functionCallEdge = (FunctionCallEdge) pCfaEdge;
        Map<MemoryLocation, CType> result = new HashMap<>();

        // Extract arguments
        String callerFunctionName = pCfaEdge.getPredecessor().getFunctionName();
        for (AExpression argument : functionCallEdge.getArguments()) {
          result.putAll(
              getInvolvedVariableTypes(
                  argument,
                  new MemoryLocationExtractor(
                      compoundIntervalManagerFactory,
                      machineModel,
                      callerFunctionName,
                      ImmutableMap.of())));
        }

        // Extract formal parameters
        for (AParameterDeclaration parameter :
            functionCallEdge.getSuccessor().getFunctionParameters()) {
          result.putAll(getInvolvedVariableTypes(parameter));
        }

        yield result;
      }
      case ReturnStatementEdge -> {
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
          yield result;
        }
        yield ImmutableMap.of();
      }
      case StatementEdge -> {
        AStatementEdge statementEdge = (AStatementEdge) pCfaEdge;
        yield switch (statementEdge.getStatement()) {
          case AExpressionAssignmentStatement aExpressionAssignmentStatement ->
              getInvolvedVariableTypes(aExpressionAssignmentStatement, pCfaEdge);

          case AExpressionStatement aExpressionStatement ->
              getInvolvedVariableTypes(aExpressionStatement.getExpression(), pCfaEdge);

          case AFunctionCallAssignmentStatement aFunctionCallAssignmentStatement ->
              getInvolvedVariableTypes(aFunctionCallAssignmentStatement, pCfaEdge);

          case AFunctionCallStatement functionCallStatement -> {
            Map<MemoryLocation, CType> result = new HashMap<>();
            for (AExpression expression :
                functionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
              result.putAll(getInvolvedVariableTypes(expression, pCfaEdge));
            }
            yield result;
          }
          default -> ImmutableMap.of();
        };
      }
      case FunctionReturnEdge -> {
        FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) pCfaEdge;
        AFunctionCall functionCall = functionReturnEdge.getFunctionCall();
        if (functionCall
            instanceof AFunctionCallAssignmentStatement functionCallAssignmentStatement) {
          AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
          if (functionCallExpression != null) {
            Map<MemoryLocation, CType> result = new HashMap<>();
            Optional<? extends AVariableDeclaration> retVar =
                functionReturnEdge.getFunctionEntry().getReturnVariable();
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
                          ImmutableMap.of())));
            }
            result.putAll(
                getInvolvedVariableTypes(
                    functionCallAssignmentStatement.getLeftHandSide(), pCfaEdge));
            yield result;
          }
        }
        yield ImmutableMap.of();
      }
      case BlankEdge, CallToReturnEdge -> ImmutableMap.of();
    };
  }

  @SuppressWarnings(
      "MixedMutabilityReturnType") // would need lots of copying, result for short-term use only
  private Map<MemoryLocation, CType> getInvolvedVariableTypes(
      AAssignment pAssignment, CFAEdge pCfaEdge) {
    if (pAssignment instanceof AExpressionAssignmentStatement expressionAssignmentStatement) {
      Map<MemoryLocation, CType> result =
          new HashMap<>(
              getInvolvedVariableTypes(expressionAssignmentStatement.getLeftHandSide(), pCfaEdge));

      result.putAll(
          getInvolvedVariableTypes(
              ExpressionToFormulaVisitor.makeCastFromArrayToPointerIfNecessary(
                  expressionAssignmentStatement.getRightHandSide(),
                  expressionAssignmentStatement.getLeftHandSide().getExpressionType()),
              pCfaEdge));
      return result;
    }
    if (pAssignment instanceof AFunctionCallAssignmentStatement functionCallAssignmentStatement) {
      Map<MemoryLocation, CType> result =
          new HashMap<>(
              getInvolvedVariableTypes(
                  functionCallAssignmentStatement.getLeftHandSide(), pCfaEdge));

      AFunctionCallExpression functionCallExpression =
          functionCallAssignmentStatement.getFunctionCallExpression();
      for (AExpression expression : functionCallExpression.getParameterExpressions()) {
        result.putAll(getInvolvedVariableTypes(expression, pCfaEdge));
      }
      return result;
    }
    return ImmutableMap.of();
  }

  private Map<? extends MemoryLocation, ? extends CType> getInvolvedVariableTypes(
      AParameterDeclaration pParameter) {
    if (pParameter.getType() instanceof CType cType) {
      return ImmutableMap.of(MemoryLocation.forDeclaration(pParameter), cType);
    }
    return ImmutableMap.of();
  }

  /**
   * Gets the variables involved in the given CInitializer.
   *
   * @param pCInitializer the CInitializer to be analyzed.
   * @return the variables involved in the given CInitializer.
   */
  private ImmutableMap<MemoryLocation, CType> getInvolvedVariableTypes(
      CInitializer pCInitializer, CFAEdge pCfaEdge) {
    return switch (pCInitializer) {
      case CDesignatedInitializer cDesignatedInitializer ->
          getInvolvedVariableTypes(cDesignatedInitializer.getRightHandSide(), pCfaEdge);

      case CInitializerExpression cInitializerExpression ->
          getInvolvedVariableTypes(cInitializerExpression.getExpression(), pCfaEdge);

      case CInitializerList initializerList -> {
        Map<MemoryLocation, CType> result = new HashMap<>();
        for (CInitializer initializer : initializerList.getInitializers()) {
          result.putAll(getInvolvedVariableTypes(initializer, pCfaEdge));
        }
        yield ImmutableMap.copyOf(result);
      }
      default -> ImmutableMap.of();
    };
  }

  /**
   * Gets the variables involved in the given expression.
   *
   * @param pExpression the expression to be analyzed.
   * @param pCFAEdge the CFA edge to obtain the function name from, if required.
   * @return the variables involved in the given expression.
   */
  public ImmutableMap<MemoryLocation, CType> getInvolvedVariableTypes(
      AExpression pExpression, CFAEdge pCFAEdge) {
    return getInvolvedVariableTypes(
        pExpression,
        new MemoryLocationExtractor(
            compoundIntervalManagerFactory, machineModel, pCFAEdge, ImmutableMap.of()));
  }

  /**
   * Gets the variables involved in the given expression.
   *
   * @param pExpression the expression to be analyzed.
   * @param pVariableNameExtractor the variable name extractor to be used.
   * @return the variables involved in the given expression.
   */
  public ImmutableMap<MemoryLocation, CType> getInvolvedVariableTypes(
      AExpression pExpression, MemoryLocationExtractor pVariableNameExtractor) {
    if (pExpression == null) {
      return ImmutableMap.of();
    }
    if (pExpression instanceof CExpression cExpression) {
      Map<MemoryLocation, CType> result = new HashMap<>();

      for (ALeftHandSide leftHandSide : cExpression.accept(LHSVisitor.INSTANCE)) {
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

      return ImmutableMap.copyOf(result);
    } else {
      throw new UnsupportedOperationException("Only C expressions are supported");
    }
  }

  private static class LHSVisitor
      extends DefaultCExpressionVisitor<Iterable<ALeftHandSide>, NoException> {

    // we have no inner state, so we can use the same instance several times and avoid re-creating
    // it.
    private static final LHSVisitor INSTANCE = new LHSVisitor();

    @Override
    protected Iterable<ALeftHandSide> visitDefault(CExpression pExp) {
      return ImmutableSet.of();
    }

    @Override
    public Iterable<ALeftHandSide> visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
      return Collections.<ALeftHandSide>singleton(pIastArraySubscriptExpression);
    }

    @Override
    public Iterable<ALeftHandSide> visit(CFieldReference pIastFieldReference) {
      return Collections.<ALeftHandSide>singleton(pIastFieldReference);
    }

    @Override
    public Iterable<ALeftHandSide> visit(CIdExpression pIastIdExpression) {
      return Collections.<ALeftHandSide>singleton(pIastIdExpression);
    }

    @Override
    public Iterable<ALeftHandSide> visit(CPointerExpression pPointerExpression) {
      return Collections.<ALeftHandSide>singleton(pPointerExpression);
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
          operand1.<Iterable<ALeftHandSide>, NoException>accept(this),
          operand2.<Iterable<ALeftHandSide>, NoException>accept(this));
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
