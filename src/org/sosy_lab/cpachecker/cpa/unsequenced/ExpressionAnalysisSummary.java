package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;

public class ExpressionAnalysisSummary {

  private final Set<SideEffectInfo> sideEffects = new HashSet<>();
  private final Set<CBinaryExpression> unsequencedBinaryExprs = new HashSet<>();
  private String originalExpressionStr = null;
  private final Map<String, Set<SideEffectInfo>> sideEffectsPerSubExprStr =
      new HashMap<>(); // tracking side effects per subexpression (by string)

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

  public Map<String, Set<SideEffectInfo>> getSideEffectsPerSubExpr() {
    return sideEffectsPerSubExprStr;
  }

  /** Add side effects for subexpressions (recorded by their AST string representation). */
  public void addSideEffectsForSubExprs(Map<String, Set<SideEffectInfo>> pSideEffectsPerSubExpr) {
    for (Map.Entry<String, Set<SideEffectInfo>> entry : pSideEffectsPerSubExpr.entrySet()) {
      String subExprStr = entry.getKey();
      Set<SideEffectInfo> effects = entry.getValue();
      if (!effects.isEmpty()) {
        sideEffectsPerSubExprStr.computeIfAbsent(subExprStr, k -> new HashSet<>()).addAll(effects);
      }
    }
  }

  @Override
  public String toString() {
    return String.format(
        "[Effects=%d, UnsequencedExprs=%d, OriginalExpr='%s']",
        sideEffects.size(),
        unsequencedBinaryExprs.size(),
        originalExpressionStr == null ? "null" : originalExpressionStr);
  }
}
