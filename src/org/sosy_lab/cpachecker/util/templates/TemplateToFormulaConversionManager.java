// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.templates;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.Formula;

/** Converting {@link Template} to {@link Formula}. */
public final class TemplateToFormulaConversionManager {
  private final CFA cfa;

  private static final CFAEdge dummyEdge =
      new BlankEdge(
          "",
          FileLocation.DUMMY,
          CFANode.newDummyCFANode("dummy-1"),
          CFANode.newDummyCFANode("dummy-2"),
          "Dummy Edge");

  private final Map<ToFormulaCacheKey, Formula> toFormulaCache = new HashMap<>();
  private final CBinaryExpressionBuilder expressionBuilder;

  public TemplateToFormulaConversionManager(CFA pCfa, LogManager logger) {
    cfa = pCfa;
    expressionBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
  }

  /**
   * Convert {@code template} to {@link Formula}, using {@link
   * org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap} and the {@link
   * org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet} provided
   * by {@code contextFormula}.
   *
   * @return Resulting formula.
   */
  public Formula toFormula(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      Template template,
      PathFormula contextFormula) {
    ToFormulaCacheKey key = new ToFormulaCacheKey(pfmgr, fmgr, template, contextFormula);
    Formula out = toFormulaCache.get(key);
    if (out != null) {
      return out;
    }
    Formula sum = null;
    int maxBitvectorSize = getBitvectorSize(template, pfmgr, contextFormula, fmgr);

    for (Entry<CIdExpression, Rational> entry : template.getLinearExpression()) {
      Rational coeff = entry.getValue();
      CIdExpression declaration = entry.getKey();

      final Formula item;
      try {
        Formula f = pfmgr.expressionToFormula(contextFormula, declaration, dummyEdge);
        item = normalizeLength(f, maxBitvectorSize, fmgr);
      } catch (UnrecognizedCodeException e) {
        throw new UnsupportedOperationException();
      }

      final Formula multipliedItem;
      if (coeff.equals(Rational.ZERO)) {
        continue;
      } else if (coeff.equals(Rational.NEG_ONE)) {
        multipliedItem = fmgr.makeNegate(item);
      } else if (coeff.equals(Rational.ONE)) {
        multipliedItem = item;
      } else {
        multipliedItem = fmgr.makeMultiply(item, fmgr.makeNumber(item, entry.getValue()));
      }

      if (sum == null) {
        sum = multipliedItem;
      } else {
        sum = fmgr.makePlus(sum, multipliedItem);
      }
    }
    assert sum != null;
    toFormulaCache.put(key, sum);
    return sum;
  }

  public boolean isOverflowing(Template template, Rational v) {
    CSimpleType templateType = getTemplateType(template);
    if (templateType.getType().isIntegerType()) {
      BigInteger maxValue = cfa.getMachineModel().getMaximalIntegerValue(templateType);
      BigInteger minValue = cfa.getMachineModel().getMinimalIntegerValue(templateType);

      // The bound obtained is larger than the highest representable
      // value, ignore it.
      if (v.compareTo(Rational.ofBigInteger(maxValue)) > 0
          || v.compareTo(Rational.ofBigInteger(minValue)) < 0) {
        return true;
      }
    }
    return false;
  }

  private CSimpleType getTemplateType(Template t) {
    CExpression sum = null;

    // also note: there is an overall _expression_ type.
    // Wonder how that one is computed --- it actually depends on the order of
    // the operands.
    for (Entry<CIdExpression, Rational> e : t.getLinearExpression()) {
      CIdExpression expr = e.getKey();
      if (sum == null) {
        sum = expr;
      } else {
        sum = expressionBuilder.buildBinaryExpressionUnchecked(sum, expr, BinaryOperator.PLUS);
      }
    }
    assert sum != null;
    return (CSimpleType) sum.getExpressionType();
  }

  private int getBitvectorSize(
      Template t, PathFormulaManager pfmgr, PathFormula contextFormula, FormulaManagerView fmgr) {
    int length = 0;

    // Figure out the maximum bitvector size.
    for (Entry<CIdExpression, Rational> entry : t.getLinearExpression()) {
      Formula item;
      try {
        item = pfmgr.expressionToFormula(contextFormula, entry.getKey(), dummyEdge);
      } catch (UnrecognizedCodeException e) {
        throw new UnsupportedOperationException();
      }
      if (!(item instanceof BitvectorFormula)) {
        continue;
      }
      BitvectorFormula b = (BitvectorFormula) item;
      length = Math.max(fmgr.getBitvectorFormulaManager().getLength(b), length);
    }
    return length;
  }

  private Formula normalizeLength(Formula f, int maxBitvectorSize, FormulaManagerView fmgr) {
    if (!(f instanceof BitvectorFormula)) {
      return f;
    }
    BitvectorFormula bv = (BitvectorFormula) f;
    return fmgr.getBitvectorFormulaManager()
        .extend(
            bv,
            Math.max(0, maxBitvectorSize - fmgr.getBitvectorFormulaManager().getLength(bv)),
            true);
  }

  private static class ToFormulaCacheKey {
    private final PathFormulaManager pathFormulaManager;
    private final FormulaManagerView formulaManagerView;
    private final Template template;
    private final PathFormula contextFormula;

    private ToFormulaCacheKey(
        PathFormulaManager pPathFormulaManager,
        FormulaManagerView pFormulaManagerView,
        Template pTemplate,
        PathFormula pContextFormula) {
      pathFormulaManager = pPathFormulaManager;
      formulaManagerView = pFormulaManagerView;
      template = pTemplate;
      contextFormula = pContextFormula;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (!(pO instanceof ToFormulaCacheKey)) {
        return false;
      }
      ToFormulaCacheKey that = (ToFormulaCacheKey) pO;
      return pathFormulaManager == that.pathFormulaManager
          && formulaManagerView == that.formulaManagerView
          && Objects.equals(template, that.template)
          && Objects.equals(contextFormula, that.contextFormula);
    }

    @Override
    public int hashCode() {
      return Objects.hash(pathFormulaManager, formulaManagerView, template, contextFormula);
    }

    @Override
    public String toString() {
      return "ToFormulaCacheKey{"
          + "pathFormulaManager="
          + pathFormulaManager
          + ", formulaManagerView="
          + formulaManagerView
          + ", template="
          + template
          + ", contextFormula="
          + contextFormula
          + '}';
    }
  }
}
