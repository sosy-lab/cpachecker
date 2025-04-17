// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.unsequenced.SideEffectInfo.AccessType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SideEffectGatherVisitor extends DefaultCExpressionVisitor<Set<SideEffectInfo>, UnrecognizedCodeException>
    implements CRightHandSideVisitor<Set<SideEffectInfo>, UnrecognizedCodeException> {

  private final UnseqBehaviorAnalysisState state;
  private final CFAEdge cfaEdge;
  private final AccessType accessType;
  private final LogManager logger;

  public SideEffectGatherVisitor(
      UnseqBehaviorAnalysisState pState,
      CFAEdge pCfaEdge,
      AccessType pAccessType,
      LogManager pLogger) {
    state = pState;
    cfaEdge = pCfaEdge;
    accessType = pAccessType;
    logger = pLogger;
  }

  @Override
  protected Set<SideEffectInfo> visitDefault(CExpression exp) throws UnrecognizedCodeException {
    return Collections.emptySet();
  }

  @Override
  public Set<SideEffectInfo> visit(CIdExpression idExpr) {
    Set<SideEffectInfo> sideEffects = new HashSet<>();
    // Variable access: record as READ/WRITE if global
    if (idExpr.getDeclaration() instanceof CVariableDeclaration decl) {
      String qualifiedName = decl.getQualifiedName();
      MemoryLocation loc = MemoryLocation.fromQualifiedName(qualifiedName);
      if (!loc.isOnFunctionStack()) {
          SideEffectInfo sideEffectInfo =  new SideEffectInfo(loc, accessType, cfaEdge);
          sideEffects.add(sideEffectInfo);

        logger.log(
            Level.INFO,
            String.format("Detected global %s access at %s: %s",
                accessType,
                cfaEdge.getFileLocation().toString(),
                qualifiedName)
        );
      }
    }

    return sideEffects;
  }

  @Override
  public Set<SideEffectInfo> visit(CFunctionCallExpression funCallExpr) throws UnrecognizedCodeException {
    Set<SideEffectInfo> sideEffects = new HashSet<>();

    //gather side effects for each parameter
    for(CExpression param: funCallExpr.getParameterExpressions()){
      Set<SideEffectInfo> paramEffects = param.accept(this);
      sideEffects.addAll(paramEffects);
    }

    //gather side effects inside function
    CExpression funcExpr = funCallExpr.getFunctionNameExpression();
    if (funcExpr instanceof CIdExpression idExpr) { //side effects inside foo()
      String functionName = idExpr.getName();
      if (state.getSideEffectsInFun().containsKey(functionName)) {
        sideEffects.addAll(state.getSideEffectsInFun().get(functionName));
      }
    }else {
      // TODO: handle indirect call via function pointer
      // e.g., (*fp)(), *get_ptr()()...
    }

    return sideEffects;
  }

  @Override
  public  Set<SideEffectInfo> visit(CBinaryExpression binaryExpression) throws UnrecognizedCodeException {
    Set<SideEffectInfo> sideEffects = new HashSet<>();

    Set<SideEffectInfo> leftSideEffects = binaryExpression.getOperand1().accept(this);
    sideEffects.addAll(leftSideEffects);
    Set<SideEffectInfo> rightSideEffects = binaryExpression.getOperand2().accept(this);
    sideEffects.addAll(rightSideEffects);;

    return sideEffects;
  }


  @Override
  public Set<SideEffectInfo> visit(CUnaryExpression unaryExpression) throws UnrecognizedCodeException {
    return switch (unaryExpression.getOperator()) {
      case SIZEOF, ALIGNOF -> Collections.emptySet();
      case MINUS, TILDE, AMPER -> unaryExpression.getOperand().accept(this);
      default -> throw new UnrecognizedCodeException("Unknown unary operator", cfaEdge, unaryExpression);
    };
  }

  @Override
  public Set<SideEffectInfo> visit(CFieldReference fieldReference) throws UnrecognizedCodeException {
    return fieldReference.getFieldOwner().accept(this);
  }

  @Override
  public Set<SideEffectInfo> visit(CCastExpression castExpr) throws UnrecognizedCodeException {
    return castExpr.getOperand().accept(this);
  }

  @Override
  public Set<SideEffectInfo> visit(CComplexCastExpression complexCastExpr) throws UnrecognizedCodeException {
    return complexCastExpr.getOperand().accept(this);
  }

  @Override
  public Set<SideEffectInfo> visit(CPointerExpression pointExpr) throws UnrecognizedCodeException {
    return pointExpr.getOperand().accept(this);
  }

}
