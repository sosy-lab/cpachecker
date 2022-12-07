// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.APointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class MemoryAccessExtractor {

  // These functions need special handling that is not currently provided by the DataRaceCPA.
  // When one of these functions is encountered we are therefore unable to tell if a data race
  // is present or not, so the analysis is terminated. TODO: Add support for these functions
  private static final ImmutableSet<String> UNSUPPORTED_FUNCTIONS =
      ImmutableSet.of(
          "pthread_mutex_trylock",
          "pthread_rwlock_rdlock",
          "pthread_rwlock_timedrdlock",
          "pthread_rwlock_timedwrlock",
          "pthread_rwlock_wrlock");

  /**
   * Collects the memory locations accessed by the given CFA edge and builds the corresponding
   * {@link MemoryAccess}es.
   *
   * <p>Throws CPATransferException if an unsupported function is encountered.
   */
  Set<MemoryAccess> getNewAccesses(ThreadInfo activeThreadInfo, CFAEdge edge, Set<String> locks)
      throws CPATransferException {
    String activeThread = activeThreadInfo.getThreadId();
    int epoch = activeThreadInfo.getEpoch();
    Set<OverapproximatingMemoryLocation> readLocationBuilder = new HashSet<>();
    Set<OverapproximatingMemoryLocation> writeLocationBuilder = new HashSet<>();
    ImmutableSet.Builder<MemoryAccess> newAccessBuilder = ImmutableSet.builder();

    switch (edge.getEdgeType()) {
      case AssumeEdge:
        AssumeEdge assumeEdge = (AssumeEdge) edge;
        readLocationBuilder.addAll(
            getInvolvedVariableTypes(assumeEdge.getExpression(), assumeEdge));
        break;
      case DeclarationEdge:
        ADeclarationEdge declarationEdge = (ADeclarationEdge) edge;
        ADeclaration declaration = declarationEdge.getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;
          CInitializer initializer = variableDeclaration.getInitializer();
          CType type = variableDeclaration.getType();
          MemoryLocation location =
              MemoryLocation.fromQualifiedName(variableDeclaration.getQualifiedName());
          OverapproximatingMemoryLocation declaredVariable;
          if (type instanceof CPointerType) {
            declaredVariable = new OverapproximatingMemoryLocation(type);
          } else {
            declaredVariable =
                new OverapproximatingMemoryLocation(ImmutableSet.of(location), type, false, true);
          }
          newAccessBuilder.add(
              new MemoryAccess(
                  activeThread, declaredVariable, initializer != null, locks, edge, epoch));
          if (initializer != null) {
            readLocationBuilder.addAll(getInvolvedVariableTypes(initializer, declarationEdge));
          }
        }
        break;
      case FunctionCallEdge:
        FunctionCallEdge functionCallEdge = (FunctionCallEdge) edge;
        String functionName = getFunctionName(functionCallEdge.getFunctionCall());
        if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
          throw new CPATransferException("DataRaceCPA does not support function " + functionName);
        }
        if (functionCallEdge.getFunctionCall() instanceof AFunctionCallAssignmentStatement) {
          AFunctionCallAssignmentStatement functionCallAssignmentStatement =
              (AFunctionCallAssignmentStatement) functionCallEdge.getFunctionCall();
          readLocationBuilder.addAll(
              getInvolvedVariableTypes(
                  functionCallAssignmentStatement.getLeftHandSide(), functionCallEdge));
          for (AExpression expression :
              functionCallAssignmentStatement
                  .getFunctionCallExpression()
                  .getParameterExpressions()) {
            readLocationBuilder.addAll(getInvolvedVariableTypes(expression, functionCallEdge));
          }
        } else {
          for (AExpression argument : functionCallEdge.getArguments()) {
            readLocationBuilder.addAll(getInvolvedVariableTypes(argument, functionCallEdge));
          }
        }
        break;
      case ReturnStatementEdge:
        AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) edge;
        if (returnStatementEdge.getExpression().isPresent()) {
          AExpression returnExpression = returnStatementEdge.getExpression().get();
          readLocationBuilder.addAll(
              getInvolvedVariableTypes(returnExpression, returnStatementEdge));
        }
        break;
      case StatementEdge:
        AStatementEdge statementEdge = (AStatementEdge) edge;
        AStatement statement = statementEdge.getStatement();
        if (statement instanceof AExpressionAssignmentStatement) {
          AExpressionAssignmentStatement expressionAssignmentStatement =
              (AExpressionAssignmentStatement) statement;
          writeLocationBuilder.addAll(
              getInvolvedVariableTypes(
                  expressionAssignmentStatement.getLeftHandSide(), statementEdge));
          readLocationBuilder.addAll(
              getInvolvedVariableTypes(
                  expressionAssignmentStatement.getRightHandSide(), statementEdge));
        } else if (statement instanceof AExpressionStatement) {
          readLocationBuilder.addAll(
              getInvolvedVariableTypes(
                  ((AExpressionStatement) statement).getExpression(), statementEdge));
        } else if (statement instanceof AFunctionCallAssignmentStatement) {
          AFunctionCallAssignmentStatement functionCallAssignmentStatement =
              (AFunctionCallAssignmentStatement) statement;
          functionName = getFunctionName(functionCallAssignmentStatement);
          if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
            throw new CPATransferException("DataRaceCPA does not support function " + functionName);
          }
          writeLocationBuilder.addAll(
              getInvolvedVariableTypes(
                  functionCallAssignmentStatement.getLeftHandSide(), statementEdge));
          AFunctionCallExpression functionCallExpression =
              functionCallAssignmentStatement.getFunctionCallExpression();
          for (AExpression expression : functionCallExpression.getParameterExpressions()) {
            readLocationBuilder.addAll(getInvolvedVariableTypes(expression, statementEdge));
          }
        } else if (statement instanceof AFunctionCallStatement) {
          AFunctionCallStatement functionCallStatement = (AFunctionCallStatement) statement;
          functionName = getFunctionName(functionCallStatement);
          if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
            throw new CPATransferException("DataRaceCPA does not support function " + functionName);
          }
          for (AExpression expression :
              functionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
            readLocationBuilder.addAll(getInvolvedVariableTypes(expression, statementEdge));
          }
        }
        break;
      case FunctionReturnEdge:
      case BlankEdge:
      case CallToReturnEdge:
        break;
      default:
        throw new AssertionError("Unhandled edge type: " + edge.getEdgeType());
    }

    for (OverapproximatingMemoryLocation possibleLocations : readLocationBuilder) {
      newAccessBuilder.add(
          new MemoryAccess(activeThread, possibleLocations, false, locks, edge, epoch));
    }
    for (OverapproximatingMemoryLocation possibleLocations : writeLocationBuilder) {
      newAccessBuilder.add(
          new MemoryAccess(activeThread, possibleLocations, true, locks, edge, epoch));
    }
    return newAccessBuilder.build();
  }

  /**
   * Gets the variables involved in the given expression.
   *
   * @param pExpression the expression to be analyzed.
   * @param pEdge the CFA edge to obtain the function name from, if required.
   * @return the variables involved in the given expression.
   */
  private Set<OverapproximatingMemoryLocation> getInvolvedVariableTypes(
      AExpression pExpression, CFAEdge pEdge) {
    if (!(pExpression instanceof CExpression)) {
      return ImmutableSet.of();
    }
    if (isAddressAccess(pExpression)) {
      return ImmutableSet.of();
    }
    CExpression expression = (CExpression) pExpression;
    MemoryLocationExtractingVisitor visitor =
        new MemoryLocationExtractingVisitor(pEdge.getSuccessor().getFunctionName());
    return expression.accept(visitor);
  }

  private Set<OverapproximatingMemoryLocation> getInvolvedVariableTypes(
      CInitializer pCInitializer, CFAEdge pCfaEdge) {
    if (pCInitializer instanceof CDesignatedInitializer) {
      return getInvolvedVariableTypes(
          ((CDesignatedInitializer) pCInitializer).getRightHandSide(), pCfaEdge);
    } else if (pCInitializer instanceof CInitializerExpression) {
      return getInvolvedVariableTypes(
          ((CInitializerExpression) pCInitializer).getExpression(), pCfaEdge);
    } else if (pCInitializer instanceof CInitializerList) {
      ImmutableSet.Builder<OverapproximatingMemoryLocation> resultBuilder = ImmutableSet.builder();
      for (CInitializer initializer : ((CInitializerList) pCInitializer).getInitializers()) {
        resultBuilder.addAll(getInvolvedVariableTypes(initializer, pCfaEdge));
      }
      return resultBuilder.build();
    } else {
      throw new AssertionError("Unhandled C initializer:" + pCInitializer);
    }
  }

  /**
   * Check whether a given expression only accesses a memory address. This is necessary because
   * accessing only the address of a memory location is not considered a read access.
   */
  private boolean isAddressAccess(AExpression pExpression) {
    if (pExpression instanceof AUnaryExpression
        && ((AUnaryExpression) pExpression).getOperator().equals(UnaryOperator.AMPER)) {
      return true;
    }
    if (pExpression instanceof AIdExpression
        && pExpression.getExpressionType() instanceof CPointerType) {
      return true;
    }
    return false;
  }

  /**
   * Tries to determine the function name for a given AFunctionCall.
   *
   * <p>Note that it is usually possible to just look up the name from the declaration of the
   * contained function call expression but there are niche cases where this is not the case, which
   * is why this function is necessary.
   */
  private String getFunctionName(AFunctionCall pFunctionCall) {
    AFunctionCallExpression functionCallExpression = pFunctionCall.getFunctionCallExpression();
    if (functionCallExpression.getDeclaration() != null) {
      return functionCallExpression.getDeclaration().getName();
    } else {
      AExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
      if (functionNameExpression instanceof AIdExpression) {
        return ((AIdExpression) functionNameExpression).getName();
      } else if (functionNameExpression instanceof AUnaryExpression) {
        AUnaryExpression unaryFunctionNameExpression = (AUnaryExpression) functionNameExpression;
        if (unaryFunctionNameExpression.getOperand() instanceof AIdExpression) {
          return ((AIdExpression) unaryFunctionNameExpression.getOperand()).getName();
        }
      } else if (functionNameExpression instanceof APointerExpression) {
        APointerExpression pointerExpression = (APointerExpression) functionNameExpression;
        if (pointerExpression.getOperand() instanceof AIdExpression) {
          return ((AIdExpression) pointerExpression.getOperand()).getName();
        }
      }
    }
    throw new AssertionError("Unable to determine function name.");
  }
}
