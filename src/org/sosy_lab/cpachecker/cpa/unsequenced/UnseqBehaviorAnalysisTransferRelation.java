// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;


import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public class UnseqBehaviorAnalysisTransferRelation
    extends ForwardingTransferRelation<
    Collection<UnseqBehaviorAnalysisState>, UnseqBehaviorAnalysisState, Precision> {

  private final LogManager logger;

  public UnseqBehaviorAnalysisTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }

  @Override
  protected Collection<UnseqBehaviorAnalysisState> handleStatementEdge(CStatementEdge cfaEdge, CStatement stat)
      throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = state;
    SideEffectGatherVisitor visitor = new SideEffectGatherVisitor(newState, cfaEdge);

    if (stat instanceof CExpressionAssignmentStatement exprAssign) {
      CExpression lhs = exprAssign.getLeftHandSide();
      CExpression rhs = exprAssign.getRightHandSide();
      recordGlobalWrite(lhs,state);

      //rhs: check if there exists unsequenced behavior and cause conflict
      if (rhs instanceof CBinaryExpression binaryExpr){ // y = f() + g()
        CBinaryExpression.BinaryOperator op = binaryExpr.getOperator();
        if (isUnsequencedBinaryOperator(op)) {
          rhs.accept(visitor);
          detectUnsequencedConflicts(binaryExpr.getOperand1(), binaryExpr.getOperand2(), newState);
        }
      }
    } else if (stat instanceof CFunctionCallAssignmentStatement funCallAssign){
      CExpression lhs = funCallAssign.getLeftHandSide();
      CFunctionCallExpression rhs = funCallAssign.getRightHandSide();

      recordGlobalWrite(lhs,state);

      if (lhs instanceof CPointerExpression pointerExpr){//*f() = g()
        pointerExpr.accept(visitor);
        rhs.accept(visitor);
        detectUnsequencedConflicts(lhs,rhs,newState);
      }
    }

    return soleSuccessor(newState);
  }

  @Override
  protected Collection<UnseqBehaviorAnalysisState> handleDeclarationEdge(
      CDeclarationEdge declarationEdge, CDeclaration declaration) throws UnrecognizedCodeException {

    if (declarationEdge.getDeclaration() instanceof CVariableDeclaration varDecl) {
      if (varDecl.getInitializer() instanceof CInitializerExpression init) {
        if (init.getExpression() instanceof CBinaryExpression binaryExpr){ // int x = f() + g()

        }
      }
    }

    return
  }

  @Override
  protected Collection<UnseqBehaviorAnalysisState> handleFunctionCallEdge(
      CFunctionCallEdge callEdge, List<CExpression> arguments, List<CParameterDeclaration> parameters, String calledFunctionName)
      throws UnrecognizedCodeException {


  }

  private void detectUnsequencedConflicts(CExpression expr1, CExpression expr2, UnseqBehaviorAnalysisState pState){
    Set<SideEffectInfo> effects1 = pState.getSideEffectsPerExpr().getOrDefault(expr1, Set.of());
    Set<SideEffectInfo> effects2 = pState.getSideEffectsPerExpr().getOrDefault(expr2, Set.of());

    for (SideEffectInfo s1 : effects1) {
      for (SideEffectInfo s2 : effects2) {
        if (conflictOnSameLocation(s1, s2)) {
          FileLocation loc = expr1.getFileLocation();
          pState.addConflict(loc, expr1);
          pState.addConflict(loc, expr2);
        }
      }
    }

  }

  private boolean conflictOnSameLocation(SideEffectInfo sideEffectInfo1, SideEffectInfo sideEffectInfo2) {
    return sideEffectInfo1.getMemoryLocation().equals(sideEffectInfo2.getMemoryLocation()) &&
        (sideEffectInfo1.isWrite() || sideEffectInfo2.isWrite());
  }

  private boolean isUnsequencedBinaryOperator(CBinaryExpression.BinaryOperator op) {
    return switch (op) {
      case BINARY_AND, BINARY_OR -> false;
      case MULTIPLY, DIVIDE, MODULO,
           PLUS, MINUS,
           SHIFT_LEFT, SHIFT_RIGHT, BINARY_XOR,
           LESS_EQUAL, LESS_THAN, GREATER_EQUAL, GREATER_THAN,
           EQUALS, NOT_EQUALS -> true;
      default -> throw new AssertionError("Unhandled operator in isUnsequencedBinaryOperator: " + op);
    };
  }

  private void recordGlobalWrite(CExpression lhs, UnseqBehaviorAnalysisState pState) {
    if (!(lhs instanceof CIdExpression idExpr)) {
      return;
    }

    CSimpleDeclaration decl = idExpr.getDeclaration();
    MemoryLocation loc = MemoryLocation.fromQualifiedName(decl.getQualifiedName());

    if (!loc.isOnFunctionStack()) {
      Set<SideEffectInfo> sideEffects = Set.of(
          new SideEffectInfo(loc, lhs.getFileLocation(), SideEffectInfo.AccessType.WRITE)
      );
      pState.addSideEffectForExpr(idExpr, sideEffects);
    }
  }

  private Collection<UnseqBehaviorAnalysisState> soleSuccessor(UnseqBehaviorAnalysisState successor) {
    return Collections.singleton(successor);
  }

}
