// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
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
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
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

  private final Multimap<CType, MemoryLocation> targets = HashMultimap.create();

  public Map<MemoryLocation, CType> getInvolvedVariableTypes(
      AAssignment pAssignment, CFAEdge pCfaEdge) throws CPATransferException {
    ImmutableMap.Builder<MemoryLocation, CType> result = ImmutableMap.builder();
    if (pAssignment instanceof AExpressionAssignmentStatement) {
      AExpressionAssignmentStatement expressionAssignmentStatement =
          (AExpressionAssignmentStatement) pAssignment;
      result.putAll(
          getInvolvedVariableTypes(expressionAssignmentStatement.getLeftHandSide(), pCfaEdge));
      result.putAll(
          getInvolvedVariableTypes(
              ExpressionToFormulaVisitor.makeCastFromArrayToPointerIfNecessary(
                  expressionAssignmentStatement.getRightHandSide(),
                  expressionAssignmentStatement.getLeftHandSide().getExpressionType()),
              pCfaEdge));
    } else if (pAssignment instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement functionCallAssignmentStatement =
          (AFunctionCallAssignmentStatement) pAssignment;
      result.putAll(
          getInvolvedVariableTypes(functionCallAssignmentStatement.getLeftHandSide(), pCfaEdge));
      AFunctionCallExpression functionCallExpression =
          functionCallAssignmentStatement.getFunctionCallExpression();
      for (AExpression expression : functionCallExpression.getParameterExpressions()) {
        result.putAll(getInvolvedVariableTypes(expression, pCfaEdge));
      }
    }
    return result.build();
  }

  /**
   * Gets the variables involved in the given CInitializer.
   *
   * @param pCInitializer the CInitializer to be analyzed.
   * @return the variables involved in the given CInitializer.
   */
  public Map<MemoryLocation, CType> getInvolvedVariableTypes(
      CInitializer pCInitializer, CFAEdge pCfaEdge) throws CPATransferException {
    if (pCInitializer instanceof CDesignatedInitializer) {
      return getInvolvedVariableTypes(
          ((CDesignatedInitializer) pCInitializer).getRightHandSide(), pCfaEdge);
    } else if (pCInitializer instanceof CInitializerExpression) {
      return getInvolvedVariableTypes(
          ((CInitializerExpression) pCInitializer).getExpression(), pCfaEdge);
    } else if (pCInitializer instanceof CInitializerList) {
      CInitializerList initializerList = (CInitializerList) pCInitializer;
      Map<MemoryLocation, CType> result = new HashMap<>();
      for (CInitializer initializer : initializerList.getInitializers()) {
        result.putAll(getInvolvedVariableTypes(initializer, pCfaEdge));
      }
      return ImmutableMap.copyOf(result);
    }
    return ImmutableMap.of();
  }

  /**
   * Gets the variables involved in the given expression.
   *
   * @param pExpression the expression to be analyzed.
   * @param pEdge the CFA edge to obtain the function name from, if required.
   * @return the variables involved in the given expression.
   */
  public Map<MemoryLocation, CType> getInvolvedVariableTypes(AExpression pExpression, CFAEdge pEdge)
      throws CPATransferException {
    if (!(pExpression instanceof CExpression)) {
      return ImmutableMap.of();
    }

    CExpression expression = (CExpression) pExpression;
    try {
      return expression.accept(
          new MemoryLocationExtractingVisitor(pEdge.getSuccessor().getFunctionName()));
    } catch (UnrecognizedCodeException e) {
      throw new CPATransferException("Could not handle expression", e);
    }
  }

  /**
   * Collects the memory locations accessed by the given CFA edge and builds the corresponding
   * {@link MemoryAccess}es.
   *
   * <p>Throws CPATransferException if an unsupported function is encountered.
   */
  Set<MemoryAccess> getNewAccesses(ThreadInfo activeThreadInfo, CFAEdge edge, Set<String> locks)
      throws CPATransferException {
    String activeThread = activeThreadInfo.getThreadId();
    ImmutableMap.Builder<MemoryLocation, CType> accessedLocationBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<MemoryLocation, CType> modifiedLocationBuilder = ImmutableMap.builder();
    ImmutableSet.Builder<MemoryAccess> newAccessBuilder = ImmutableSet.builder();

    switch (edge.getEdgeType()) {
      case AssumeEdge:
        AssumeEdge assumeEdge = (AssumeEdge) edge;
        accessedLocationBuilder.putAll(
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
          OverapproximatingMemoryLocation declaredVariable =
              new OverapproximatingMemoryLocation(ImmutableSet.of(location), type);
          newAccessBuilder.add(
              new MemoryAccess(
                  activeThread,
                  declaredVariable,
                  initializer != null,
                  locks,
                  edge,
                  activeThreadInfo.getEpoch()));
          if (initializer != null) {
            if (initializer instanceof CInitializerExpression
                && isAddressAccess(((CInitializerExpression) initializer).getExpression())) {
              // TODO: Anything to do here when a new pointer is declared?
              break;
            }
            accessedLocationBuilder.putAll(getInvolvedVariableTypes(initializer, declarationEdge));
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
          accessedLocationBuilder.putAll(
              getInvolvedVariableTypes(functionCallAssignmentStatement, functionCallEdge));
        } else {
          for (AExpression argument : functionCallEdge.getArguments()) {
            accessedLocationBuilder.putAll(getInvolvedVariableTypes(argument, functionCallEdge));
          }
        }
        break;
      case ReturnStatementEdge:
        AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) edge;
        if (returnStatementEdge.getExpression().isPresent()) {
          AExpression returnExpression = returnStatementEdge.getExpression().get();
          accessedLocationBuilder.putAll(
              getInvolvedVariableTypes(returnExpression, returnStatementEdge));
        }
        break;
      case StatementEdge:
        AStatementEdge statementEdge = (AStatementEdge) edge;
        AStatement statement = statementEdge.getStatement();
        if (statement instanceof AExpressionAssignmentStatement) {
          AExpressionAssignmentStatement expressionAssignmentStatement =
              (AExpressionAssignmentStatement) statement;
          if (isAddressAccess(expressionAssignmentStatement.getRightHandSide())) {
            accessedLocationBuilder.putAll(
                getInvolvedVariableTypes(
                    expressionAssignmentStatement.getLeftHandSide(), statementEdge));
          } else {
            accessedLocationBuilder.putAll(
                getInvolvedVariableTypes(expressionAssignmentStatement, statementEdge));
          }
          modifiedLocationBuilder.putAll(
              getInvolvedVariableTypes(
                  expressionAssignmentStatement.getLeftHandSide(), statementEdge));
        } else if (statement instanceof AExpressionStatement) {
          accessedLocationBuilder.putAll(
              getInvolvedVariableTypes(
                  ((AExpressionStatement) statement).getExpression(), statementEdge));
        } else if (statement instanceof AFunctionCallAssignmentStatement) {
          AFunctionCallAssignmentStatement functionCallAssignmentStatement =
              (AFunctionCallAssignmentStatement) statement;
          functionName = getFunctionName(functionCallAssignmentStatement);
          if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
            throw new CPATransferException("DataRaceCPA does not support function " + functionName);
          }
          accessedLocationBuilder.putAll(
              getInvolvedVariableTypes(functionCallAssignmentStatement, statementEdge));
          modifiedLocationBuilder.putAll(
              getInvolvedVariableTypes(
                  functionCallAssignmentStatement.getLeftHandSide(), statementEdge));
        } else if (statement instanceof AFunctionCallStatement) {
          AFunctionCallStatement functionCallStatement = (AFunctionCallStatement) statement;
          functionName = getFunctionName(functionCallStatement);
          if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
            throw new CPATransferException("DataRaceCPA does not support function " + functionName);
          }
          for (AExpression expression :
              functionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
            accessedLocationBuilder.putAll(getInvolvedVariableTypes(expression, statementEdge));
          }
        }
        break;
      case FunctionReturnEdge:
      case BlankEdge:
      case CallToReturnEdge:
        break;
      default:
        throw new AssertionError("Unknown edge type: " + edge.getEdgeType());
    }

    Map<MemoryLocation, CType> accessedLocations = accessedLocationBuilder.buildOrThrow();
    Map<MemoryLocation, CType> modifiedLocations = modifiedLocationBuilder.buildOrThrow();
    assert accessedLocations.keySet().containsAll(modifiedLocations.keySet());

    for (Entry<MemoryLocation, CType> entry : accessedLocations.entrySet()) {
      OverapproximatingMemoryLocation possibleLocations =
          followPointers(entry.getKey(), entry.getValue());
      newAccessBuilder.add(
          new MemoryAccess(
              activeThread,
              possibleLocations,
              modifiedLocations.containsKey(entry.getKey()),
              locks,
              edge,
              activeThreadInfo.getEpoch()));
    }
    return newAccessBuilder.build();
  }

  private OverapproximatingMemoryLocation followPointers(MemoryLocation origin, CType type) {
    targets.put(type, origin);
    // TODO: e.g. data[4] can also be manipulated by data[*]
    if (!(type instanceof CPointerType)) {
      return new OverapproximatingMemoryLocation(ImmutableSet.of(origin), type);
    }
    CPointerType pointerType = (CPointerType) type;
    ImmutableSet.Builder<MemoryLocation> potentialLocations = ImmutableSet.builder();
    for (Entry<CType, MemoryLocation> entry : targets.entries()) {
      if (CTypes.areTypesCompatible(entry.getKey(), pointerType.getType())) {
        potentialLocations.add(entry.getValue());
      }
    }
    potentialLocations.add(origin);
    return new OverapproximatingMemoryLocation(potentialLocations.build(), type);
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
