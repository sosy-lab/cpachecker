// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;


public class GlobalVarAnalysisTransferRelation
    extends ForwardingTransferRelation<GlobalVarAnalysisState, GlobalVarAnalysisState, Precision> {

  private Set<String> formalParameters;
  private final LogManager logger;


  public GlobalVarAnalysisTransferRelation(LogManager pLogger) {
    formalParameters = new HashSet<>();
    logger = pLogger;
  }

  @Override
  protected GlobalVarAnalysisState handleDeclarationEdge(CDeclarationEdge declarationEdge, CDeclaration declaration)
      throws UnrecognizedCodeException {
    Set<String> newGlobalVars = state.getGlobalVars();
    List<String> newDetectedAssignedVars = new ArrayList<>(state.getDetectedAssignedVars());

    if (declarationEdge.getDeclaration() instanceof CVariableDeclaration decl) {
      boolean isGlobal = decl.isGlobal();
      if(isGlobal){
        newGlobalVars.add(decl.getQualifiedName());
      }

      if(decl.getInitializer() instanceof CInitializerExpression init){
        if(init.getExpression() instanceof CBinaryExpression binaryExpr && isGlobalPair(binaryExpr)){ // int x = y + z
          newDetectedAssignedVars.add(decl.getQualifiedName());
        }
      }
    }

    return new GlobalVarAnalysisState(
        newGlobalVars,
        state.isValidReturn(),
        newDetectedAssignedVars
    );
  }

  @Override
  protected GlobalVarAnalysisState handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge, CFunctionCall summaryExpr, String callerFunctionName)
      throws UnrecognizedCodeException {
    logger.log(Level.INFO,
        "valid return:" + state.isValidReturn() + " summaryExpr:" + summaryExpr.toASTString()
    );
    List<String> newDetectedAssignedVars = new ArrayList<>(state.getDetectedAssignedVars());

    if(state.isValidReturn()){
      if (summaryExpr instanceof CFunctionCallAssignmentStatement fCallAssign) { //y = add() or int y = add()
        CLeftHandSide lhs = fCallAssign.getLeftHandSide();
        if (lhs instanceof CIdExpression idExpr) {
          String assignedVar = idExpr.getDeclaration().getQualifiedName();
          newDetectedAssignedVars.add(assignedVar);
        }
      }
    }

    return new GlobalVarAnalysisState(
        state.getGlobalVars(),
        state.isValidReturn(),
        newDetectedAssignedVars
    );
  }

  @Override
  protected GlobalVarAnalysisState handleStatementEdge(CStatementEdge cfaEdge, CStatement stat)
      throws UnrecognizedCodeException {
    List<String> newDetectedAssignedVars = new ArrayList<>(state.getDetectedAssignedVars());

    if (stat instanceof CExpressionAssignmentStatement exprAssign) { //y = y + z
      if (exprAssign.getRightHandSide() instanceof CBinaryExpression binaryExpr && isGlobalPair(binaryExpr)) {
        if (exprAssign.getLeftHandSide() instanceof CIdExpression idExpr ) {
          newDetectedAssignedVars.add(idExpr.getDeclaration().getQualifiedName());
        }
      }
    }

    return new GlobalVarAnalysisState(
        state.getGlobalVars(),
        false,
        newDetectedAssignedVars
    );
  }

  @Override
  protected GlobalVarAnalysisState handleReturnStatementEdge(
      CReturnStatementEdge returnEdge) throws UnrecognizedCodeException {

    boolean newValidReturn = state.isValidReturn();
    Optional<CExpression> expressionOptional = returnEdge.getExpression();

    if(expressionOptional.isPresent()) {
      CExpression returnExpression = expressionOptional.get();
      if (returnExpression instanceof CBinaryExpression returnExpr) {
        newValidReturn = isGlobalPair(returnExpr);
      }
    }

    //delete formal vars from global var set
    Set<String> newGlobalVars = new HashSet<>(state.getGlobalVars());
    newGlobalVars.removeAll(formalParameters);
    formalParameters.clear();

    return new GlobalVarAnalysisState(
        newGlobalVars,
        newValidReturn,
        state.getDetectedAssignedVars()
    );
  }

  @Override
  protected GlobalVarAnalysisState handleFunctionCallEdge(
      CFunctionCallEdge callEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName)
      throws UnrecognizedCodeException {

    Set<String> newGlobalVars = new HashSet<>(state.getGlobalVars());

    for (int i = 0; i < parameters.size(); i++) {
      CParameterDeclaration parameter = parameters.get(i);
      CExpression argument = arguments.get(i);

      if (argument instanceof CIdExpression idExpr) {
        if(state.getGlobalVars().contains(idExpr.getDeclaration().getQualifiedName())){
          newGlobalVars.add(parameter.getQualifiedName());
          formalParameters.add(parameter.getQualifiedName());
        }
      }
    }

      return new GlobalVarAnalysisState(
          newGlobalVars,
          state.isValidReturn(),
          state.getDetectedAssignedVars()
      );
  }

  private boolean isGlobalPair(CBinaryExpression binaryExpr) {
    if (binaryExpr.getOperator() == BinaryOperator.PLUS || binaryExpr.getOperator() == BinaryOperator.MINUS) {
      CExpression operand1 = binaryExpr.getOperand1();
      CExpression operand2 = binaryExpr.getOperand2();

      if (operand1 instanceof CIdExpression var1 && operand2 instanceof CIdExpression var2) {
        if (!var1.equals(var2) &&
            state.getGlobalVars().contains(var1.getDeclaration().getQualifiedName()) &&
            state.getGlobalVars().contains(var2.getDeclaration().getQualifiedName())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  protected GlobalVarAnalysisState handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthValue)
      throws UnrecognizedCodeException {
    return state;
  }


  @Override
  protected GlobalVarAnalysisState handleBlankEdge(BlankEdge cfaEdge) {
    return state;
  }

}
