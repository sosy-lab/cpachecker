// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.getJoinResultTarget;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class MemoryAccessExtractor {

  private final Set<String> addressedVariables;
  private final SetMultimap<MemoryLocation, MemoryLocation> pointerTargets;

  MemoryAccessExtractor(
      Set<String> pAddressedVariables,
      SetMultimap<MemoryLocation, MemoryLocation> pPointerTargets) {
    addressedVariables = pAddressedVariables;
    pointerTargets = pPointerTargets;
  }

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
      case AssumeEdge -> {
        AssumeEdge assumeEdge = (AssumeEdge) edge;
        readLocationBuilder.addAll(
            getInvolvedVariableTypes(assumeEdge.getExpression(), assumeEdge));
      }
      case DeclarationEdge -> {
        ADeclarationEdge declarationEdge = (ADeclarationEdge) edge;
        ADeclaration declaration = declarationEdge.getDeclaration();
        if (declaration instanceof CVariableDeclaration variableDeclaration) {
          CInitializer initializer = variableDeclaration.getInitializer();
          newAccessBuilder.add(
              new MemoryAccess(
                  activeThread,
                  getDeclaredLocation(variableDeclaration),
                  initializer != null,
                  locks,
                  edge,
                  epoch));
          if (initializer != null) {
            readLocationBuilder.addAll(getInvolvedVariableTypes(initializer, declarationEdge));
          }
        }
      }
      case FunctionCallEdge -> {
        FunctionCallEdge functionCallEdge = (FunctionCallEdge) edge;
        String functionName = getFunctionName(functionCallEdge.getFunctionCall());
        if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
          throw new CPATransferException("DataRaceCPA does not support function " + functionName);
        }
        if (functionCallEdge.getFunctionCall()
            instanceof AFunctionCallAssignmentStatement functionCallAssignmentStatement) {

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
      }
      case ReturnStatementEdge -> {
        AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) edge;
        if (returnStatementEdge.getExpression().isPresent()) {
          AExpression returnExpression = returnStatementEdge.getExpression().get();
          readLocationBuilder.addAll(
              getInvolvedVariableTypes(returnExpression, returnStatementEdge));
        }
      }
      case StatementEdge -> {
        AStatementEdge statementEdge = (AStatementEdge) edge;
        switch (statementEdge.getStatement()) {
          case AExpressionAssignmentStatement expressionAssignmentStatement -> {
            addLeftHandSide(
                expressionAssignmentStatement.getLeftHandSide(),
                statementEdge,
                writeLocationBuilder,
                readLocationBuilder);
            readLocationBuilder.addAll(
                getInvolvedVariableTypes(
                    expressionAssignmentStatement.getRightHandSide(), statementEdge));
          }
          case AExpressionStatement aExpressionStatement ->
              readLocationBuilder.addAll(
                  getInvolvedVariableTypes(aExpressionStatement.getExpression(), statementEdge));
          case AFunctionCallAssignmentStatement functionCallAssignmentStatement -> {
            String functionName = getFunctionName(functionCallAssignmentStatement);
            if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
              throw new CPATransferException(
                  "DataRaceCPA does not support function " + functionName);
            }
            addLeftHandSide(
                functionCallAssignmentStatement.getLeftHandSide(),
                statementEdge,
                writeLocationBuilder,
                readLocationBuilder);
            AFunctionCallExpression functionCallExpression =
                functionCallAssignmentStatement.getFunctionCallExpression();
            for (AExpression expression : functionCallExpression.getParameterExpressions()) {
              readLocationBuilder.addAll(getInvolvedVariableTypes(expression, statementEdge));
            }
            addJoinResult(
                functionCallExpression, statementEdge, writeLocationBuilder, readLocationBuilder);
          }
          case AFunctionCallStatement functionCallStatement -> {
            String functionName = getFunctionName(functionCallStatement);
            if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
              throw new CPATransferException(
                  "DataRaceCPA does not support function " + functionName);
            }
            for (AExpression expression :
                functionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
              readLocationBuilder.addAll(getInvolvedVariableTypes(expression, statementEdge));
            }
            addJoinResult(
                functionCallStatement.getFunctionCallExpression(),
                statementEdge,
                writeLocationBuilder,
                readLocationBuilder);
          }
          default -> {}
        }
      }
      case FunctionReturnEdge -> {
        FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) edge;
        if (functionReturnEdge.getSummaryEdge().getExpression()
            instanceof AFunctionCallAssignmentStatement functionCallAssignmentStatement) {
          addLeftHandSide(
              functionCallAssignmentStatement.getLeftHandSide(),
              functionReturnEdge,
              writeLocationBuilder,
              readLocationBuilder);
        }
      }
      case BlankEdge, CallToReturnEdge -> {}
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
   * Returns the locations that the given edge may write, including declared variables and
   * parameters. The address of an access that depends on them may differ after the edge.
   */
  Set<OverapproximatingMemoryLocation> getWrittenLocations(CFAEdge pEdge) {
    return switch (pEdge) {
      case ADeclarationEdge declarationEdge
          when declarationEdge.getDeclaration() instanceof CVariableDeclaration declaration ->
          ImmutableSet.of(getDeclaredLocation(declaration));
      case FunctionCallEdge functionCallEdge ->
          FluentIterable.from(functionCallEdge.getSuccessor().getFunctionParameters())
              .filter(CParameterDeclaration.class)
              .transform(this::getDeclaredLocation)
              .toSet();
      case FunctionReturnEdge functionReturnEdge
          when functionReturnEdge.getSummaryEdge().getExpression()
              instanceof AFunctionCallAssignmentStatement assignment ->
          getWrittenLocation(assignment.getLeftHandSide(), pEdge);
      case AStatementEdge statementEdge
          when statementEdge.getStatement() instanceof AAssignment assignment ->
          getWrittenLocation(assignment.getLeftHandSide(), pEdge);
      default -> ImmutableSet.of();
    };
  }

  private Set<OverapproximatingMemoryLocation> getWrittenLocation(
      AExpression pLvalue, CFAEdge pEdge) {
    if (!(pLvalue instanceof CExpression lvalue)) {
      return ImmutableSet.of();
    }
    return ImmutableSet.of(newVisitor(pEdge).getMemoryLocation(lvalue));
  }

  private OverapproximatingMemoryLocation getDeclaredLocation(CSimpleDeclaration pDeclaration) {
    CType type = pDeclaration.getType();
    return new OverapproximatingMemoryLocation(
        ImmutableSet.of(MemoryLocation.fromQualifiedName(pDeclaration.getQualifiedName())),
        type,
        false,
        true,
        type.getCanonicalType() instanceof CArrayType
            || addressedVariables.contains(pDeclaration.getQualifiedName()),
        new CIdExpression(pDeclaration.getFileLocation(), pDeclaration));
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
    if (!(pExpression instanceof CExpression expression)) {
      return ImmutableSet.of();
    }

    MemoryLocationExtractingVisitor visitor = newVisitor(pEdge);
    return expression.accept(visitor);
  }

  private Set<OverapproximatingMemoryLocation> getInvolvedVariableTypes(
      CInitializer pCInitializer, CFAEdge pCfaEdge) {
    return switch (pCInitializer) {
      case CDesignatedInitializer cDesignatedInitializer ->
          getInvolvedVariableTypes(cDesignatedInitializer.getRightHandSide(), pCfaEdge);

      case CInitializerExpression cInitializerExpression ->
          getInvolvedVariableTypes(cInitializerExpression.getExpression(), pCfaEdge);

      case CInitializerList cInitializerList -> {
        ImmutableSet.Builder<OverapproximatingMemoryLocation> resultBuilder =
            ImmutableSet.builder();
        for (CInitializer initializer : cInitializerList.getInitializers()) {
          resultBuilder.addAll(getInvolvedVariableTypes(initializer, pCfaEdge));
        }
        yield resultBuilder.build();
      }
      default -> throw new AssertionError("Unhandled C initializer:" + pCInitializer);
    };
  }

  /** Adds the location written by the given left-hand side and the reads of its address. */
  private void addLeftHandSide(
      AExpression pLeftHandSide,
      CFAEdge pEdge,
      Set<OverapproximatingMemoryLocation> pWrites,
      Set<OverapproximatingMemoryLocation> pReads) {
    if (pLeftHandSide instanceof CExpression leftHandSide) {
      MemoryLocationExtractingVisitor visitor = newVisitor(pEdge);
      pWrites.add(visitor.getMemoryLocation(leftHandSide));
      pReads.addAll(visitor.getAddressReads(leftHandSide));
    }
  }

  /** pthread_join(t, p) writes the result of thread t to *p. */
  private void addJoinResult(
      AFunctionCallExpression pCall,
      CFAEdge pEdge,
      Set<OverapproximatingMemoryLocation> pWrites,
      Set<OverapproximatingMemoryLocation> pReads) {
    getJoinResultTarget(pCall).ifPresent(target -> addLeftHandSide(target, pEdge, pWrites, pReads));
  }

  private MemoryLocationExtractingVisitor newVisitor(CFAEdge pEdge) {
    // The expressions of a function call belong to the caller
    CFANode node =
        pEdge instanceof FunctionCallEdge ? pEdge.getPredecessor() : pEdge.getSuccessor();
    return new MemoryLocationExtractingVisitor(
        node.getFunctionName(), addressedVariables, pointerTargets);
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
      if (functionNameExpression instanceof AIdExpression aIdExpression) {
        return aIdExpression.getName();
      } else if (functionNameExpression instanceof AUnaryExpression unaryFunctionNameExpression) {
        if (unaryFunctionNameExpression.getOperand() instanceof AIdExpression aIdExpression) {
          return aIdExpression.getName();
        }
      } else if ((functionNameExpression instanceof APointerExpression pointerExpression)
          && (pointerExpression.getOperand() instanceof AIdExpression aIdExpression)) {
        return aIdExpression.getName();
      }
    }
    throw new AssertionError("Unable to determine function name.");
  }
}
