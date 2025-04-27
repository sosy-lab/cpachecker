package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;

public class ExpressionAnalysisSummary {

  private final Set<SideEffectInfo> sideEffects = new HashSet<>();
  private final Set<CBinaryExpression> unsequencedBinaryExprs = new HashSet<>();
  private String originalExpressionStr = null;

  public static ExpressionAnalysisSummary empty() {
    return new ExpressionAnalysisSummary();
  }

  public Set<SideEffectInfo> getSideEffects() {
    return sideEffects;
  }

  public Set<CBinaryExpression> getUnsequencedBinaryExprs() {
    return unsequencedBinaryExprs;
  }

  public void addSideEffect(SideEffectInfo effect) {
    sideEffects.add(effect);
  }

  public void addSideEffects(Set<SideEffectInfo> effects) {
    sideEffects.addAll(effects);
  }

  public void addUnsequencedBinaryExpr(CBinaryExpression expr) {
    unsequencedBinaryExprs.add(expr);
  }

  public void addUnsequencedBinaryExprs(Set<CBinaryExpression> exprs) {
    unsequencedBinaryExprs.addAll(exprs);
  }

  public String getOriginalExpressionStr() {
    return originalExpressionStr;
  }

  public void setOriginalExpressionStr(String exprStr) {
    this.originalExpressionStr = exprStr;
  }

  @Override
  public String toString() {
    return String.format(
        "[Effects=%d, UnsequencedExprs=%d, OriginalExpr='%s']",
        sideEffects.size(), unsequencedBinaryExprs.size(),
        originalExpressionStr == null ? "null" : originalExpressionStr);
  }
}
