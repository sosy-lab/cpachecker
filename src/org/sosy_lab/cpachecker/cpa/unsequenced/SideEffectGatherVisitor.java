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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

public class SideEffectGatherVisitor extends DefaultCExpressionVisitor<Map<CFAEdge, Set<SideEffectInfo>>, UnrecognizedCodeException>
    implements CRightHandSideVisitor<Map<CFAEdge, Set<SideEffectInfo>>, UnrecognizedCodeException> {

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
  protected Map<CFAEdge, Set<SideEffectInfo>> visitDefault(CExpression exp) throws UnrecognizedCodeException {
    return Collections.emptyMap();
  }

  @Override
  public Map<CFAEdge, Set<SideEffectInfo>> visit(CIdExpression idExpr) {
    Set<SideEffectInfo> sideEffects = new HashSet<>();
    // Variable access: record as READ if global
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

    return record(sideEffects);
  }

  @Override
  public Map<CFAEdge, Set<SideEffectInfo>> visit(CFunctionCallExpression funCallExpr) throws UnrecognizedCodeException {
    Set<SideEffectInfo> sideEffects = new HashSet<>();

    //gather side effects for each parameter
    for(CExpression param: funCallExpr.getParameterExpressions()){
      Map<CFAEdge, Set<SideEffectInfo>> paramEffects = param.accept(this);
      paramEffects.values().forEach(sideEffects::addAll);
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

    return record(sideEffects) ;
  }

  @Override
  public Map<CFAEdge, Set<SideEffectInfo>> visit(CBinaryExpression binaryExpression) throws UnrecognizedCodeException {
    Set<SideEffectInfo> sideEffects = new HashSet<>();

    Map<CFAEdge, Set<SideEffectInfo>> leftSideEffects = binaryExpression.getOperand1().accept(this);
    leftSideEffects.values().forEach(sideEffects::addAll);
    Map<CFAEdge, Set<SideEffectInfo>> rightSideEffects = binaryExpression.getOperand2().accept(this);
    rightSideEffects.values().forEach(sideEffects::addAll);

    return record(sideEffects);
  }


  @Override
  public Map<CFAEdge, Set<SideEffectInfo>> visit(CUnaryExpression unaryExpression) throws UnrecognizedCodeException {
    Set<SideEffectInfo> effects = switch (unaryExpression.getOperator()) {
      case SIZEOF, ALIGNOF -> Set.of();
      case MINUS, TILDE, AMPER -> {
        Map<CFAEdge, Set<SideEffectInfo>> operandEffects = unaryExpression.getOperand().accept(this);
        Set<SideEffectInfo> collected = new HashSet<>();
        operandEffects.values().forEach(collected::addAll);
        yield collected;
      }
      default -> throw new UnrecognizedCodeException("unknown unary operator", cfaEdge, unaryExpression);
    };
    return record(effects);
  }

  @Override
  public Map<CFAEdge, Set<SideEffectInfo>> visit(CFieldReference fieldReference) throws UnrecognizedCodeException {
    return collectAndRecord(fieldReference);
  }

  @Override
  public Map<CFAEdge, Set<SideEffectInfo>> visit(CCastExpression castExpr) throws UnrecognizedCodeException {
    return collectAndRecord(castExpr);
  }

  @Override
  public Map<CFAEdge, Set<SideEffectInfo>> visit(CComplexCastExpression complexCastExpr) throws UnrecognizedCodeException {
    return collectAndRecord(complexCastExpr);
  }

  @Override
  public Map<CFAEdge, Set<SideEffectInfo>> visit(CPointerExpression pointExpr) throws UnrecognizedCodeException {
    return collectAndRecord(pointExpr);
  }

  private Map<CFAEdge, Set<SideEffectInfo>> record(Set<SideEffectInfo> effects) {
    if (effects.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<CFAEdge, Set<SideEffectInfo>> result = new HashMap<>();
    result.put(cfaEdge, effects);
    return result;
  }

  private Map<CFAEdge, Set<SideEffectInfo>> collectAndRecord(CExpression expr) throws UnrecognizedCodeException {
    Set<SideEffectInfo> sideEffects = new HashSet<>();
    Map<CFAEdge, Set<SideEffectInfo>> effects = expr.accept(this);
    effects.values().forEach(sideEffects::addAll);
    return record(sideEffects);
  }
}
