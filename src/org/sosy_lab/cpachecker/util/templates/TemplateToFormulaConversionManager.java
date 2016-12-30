/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.templates;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Converting {@link Template} to {@link Formula}.
 */
public class TemplateToFormulaConversionManager {
  private final MachineModel machineModel;

  private static final CExpression dummyExpression = CIntegerLiteralExpression.ZERO;

  private final Map<ToFormulaCacheKey, Formula> toFormulaCache =
      new HashMap<>();
  private final CBinaryExpressionBuilder expressionBuilder;

  public TemplateToFormulaConversionManager(
      MachineModel pMachineModel,
      LogManager logger) {
    machineModel = pMachineModel;
    expressionBuilder = new CBinaryExpressionBuilder(pMachineModel, logger);
  }

  /**
   * Convert {@code template} to {@link Formula}, using
   * {@link org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap} and
   * the {@link org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet}
   * provided by {@code contextFormula}.
   *
   * @return Resulting formula.
   */
  public Formula toFormula(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      Template template,
      PathFormula contextFormula) {
    ToFormulaCacheKey key =
        new ToFormulaCacheKey(pfmgr, fmgr, template, contextFormula);
    Formula out = toFormulaCache.get(key);
    if (out != null) {
      return out;
    }
    Formula sum = null;
    int maxBitvectorSize = getBitvectorSize(template, pfmgr, contextFormula,fmgr);

    for (Entry<TVariable, Rational> entry : template.getLinearExpression()) {
      Rational coeff = entry.getValue();
      TVariable variable = entry.getKey();

      Formula f = variable.toFormula(pfmgr, contextFormula);
      Formula item = normalizeLength(f, maxBitvectorSize, fmgr);

      final Formula multipliedItem;
      if (coeff.equals(Rational.ZERO)) {
        continue;
      } else if (coeff.equals(Rational.NEG_ONE)) {
        multipliedItem = fmgr.makeNegate(item);
      } else if (coeff.equals(Rational.ONE)){
        multipliedItem = item;
      } else {
        multipliedItem = fmgr.makeMultiply(
            item, fmgr.makeNumber(item, entry.getValue()));
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
      BigInteger maxValue = machineModel
          .getMaximalIntegerValue(templateType);
      BigInteger minValue = machineModel
          .getMinimalIntegerValue(templateType);

      // The bound obtained is larger than the highest representable
      // value, ignore it.
      if (v.compareTo(Rational.ofBigInteger(maxValue)) == 1
          || v.compareTo(Rational.ofBigInteger(minValue)) == -1) {
        return true;
      }
    }
    return false;
  }

  private CSimpleType getTemplateType(Template t) {
    CType out = null;
    for (Entry<TVariable, Rational> e: t.getLinearExpression()) {
      if (out == null) {
        out = e.getKey().getType();
      } else {
        out = expressionBuilder.getCalculationTypeForBinaryOperationUnchecked(
            out,
            e.getKey().getType(),
            BinaryOperator.PLUS,
            dummyExpression,
            dummyExpression
        );
      }
    }
    assert out != null;
    return (CSimpleType) out;
  }

  /**
   * Get the size of the bitvector required to represent the template {@code t},
   * as given by the size of the element requiring most bits in {@code t}.
   */
  private int getBitvectorSize(Template t,
                               PathFormulaManager pfmgr,
                               PathFormula contextFormula,
                               FormulaManagerView fmgr) {
    int length = 0;

    // Figure out the maximum bitvector size.
    for (Entry<TVariable, Rational> entry : t.getLinearExpression()) {
      Formula item = entry.getKey().toFormula(pfmgr, contextFormula);
      if (!(item instanceof BitvectorFormula)) {
        continue;
      }
      BitvectorFormula b = (BitvectorFormula) item;
      length = Math.max(
          fmgr.getBitvectorFormulaManager().getLength(b),
          length);
    }
    return length;
  }

  private Formula normalizeLength(Formula f,
                                  int maxBitvectorSize,
                                  FormulaManagerView fmgr) {
    if (!(f instanceof BitvectorFormula)) {
      return f;
    }
    BitvectorFormula bv = (BitvectorFormula) f;
    return fmgr.getBitvectorFormulaManager().extend(
        bv,
        Math.max(0,
            maxBitvectorSize - fmgr.getBitvectorFormulaManager().getLength(bv)),
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
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      ToFormulaCacheKey that = (ToFormulaCacheKey) pO;
      return pathFormulaManager == that.pathFormulaManager
          && formulaManagerView == that.formulaManagerView &&
          Objects.equals(template, that.template) &&
          Objects.equals(contextFormula, that.contextFormula);
    }

    @Override
    public int hashCode() {
      return Objects
          .hash(pathFormulaManager, formulaManagerView, template,
              contextFormula);
    }

    @Override
    public String toString() {
      return "ToFormulaCacheKey{" +
          "pathFormulaManager=" + pathFormulaManager +
          ", formulaManagerView=" + formulaManagerView +
          ", template=" + template +
          ", contextFormula=" + contextFormula +
          '}';
    }
  }
}
