package org.sosy_lab.cpachecker.cpa.policyiteration;

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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.cwriter.FormulaToCExpressionConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BitvectorFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Class responsible for converting states to formulas.
 */
public class StateFormulaConversionManager {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final PathFormulaManager pfmgr;
  private final CFA cfa;
  private final LogManager logger;
  private final CBinaryExpressionBuilder expressionBuilder;
  private final FormulaToCExpressionConverter formulaToCExpressionConverter;

  private static final CFAEdge dummyEdge = new BlankEdge("",
      FileLocation.DUMMY,
      new CFANode("dummy-1"), new CFANode("dummy-2"), "Dummy Edge");

  public StateFormulaConversionManager(
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      CFA pCfa,
      LogManager pLogger) {
    fmgr = pFmgr;
    pfmgr = pPfmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
    cfa = pCfa;
    logger = pLogger;
    expressionBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
    formulaToCExpressionConverter = new FormulaToCExpressionConverter(fmgr);
  }

  /**
   * Represent an input state as a C expression.
   *
   * <p>N.B. implementation relies on recursion and string concatenation,
   * and is consequently relatively slow.
   */
  String abstractStateToCExpression(PolicyAbstractedState abstractState) throws InterruptedException {
    BooleanFormula invariant = bfmgr.and(
        abstractStateToConstraints(fmgr, pfmgr, abstractState, false));
    BooleanFormula uninstantiated = fmgr.uninstantiate(invariant);
    return formulaToCExpressionConverter.formulaToCExpression(uninstantiated);
  }


  /**
   * Returns _instantiated_ set of constraints.
   *
   * @param attachExtraInvariant Attach the invariant supplied by other analysis.
   */
  List<BooleanFormula> abstractStateToConstraints(
      FormulaManagerView fmgrv,
      PathFormulaManager pfmgr,
      PolicyAbstractedState abstractState,
      boolean attachExtraInvariant) {

    // Returns the abstract state together with the conjoined extra invariant.
    List<BooleanFormula> constraints = new ArrayList<>();

    PathFormula inputPath = getPathFormula(abstractState, fmgrv, attachExtraInvariant);
    if (!bfmgr.isTrue(inputPath.getFormula())) {
      constraints.add(inputPath.getFormula());
    }

    if (attachExtraInvariant) {

      // Extra invariant.
      constraints.add(fmgr.instantiate(
          abstractState.getExtraInvariant(), inputPath.getSsa()));
    }

    constraints.add(abstractState.getCongruence().toFormula(
        fmgrv, pfmgr, inputPath));
    for (Entry<Template, PolicyBound> entry : abstractState) {
      Template template = entry.getKey();
      PolicyBound bound = entry.getValue();

      Formula t = toFormula(pfmgr, fmgrv, template, inputPath);

      BooleanFormula constraint = fmgrv.makeLessOrEqual(
          t, fmgrv.makeNumber(t, bound.getBound()), true);
      constraints.add(constraint);
    }
    return constraints;
  }

  public BooleanFormula getStartConstraintsWithExtraInvariant(
      PolicyIntermediateState state) {
    return bfmgr.and(abstractStateToConstraints(
        fmgr, pfmgr, state.getBackpointerState(), true));
  }

  /**
   * @return Representation of an {@code abstractState} as a
   * {@link PolicyIntermediateState}.
   */
  PolicyIntermediateState abstractStateToIntermediate(
      PolicyAbstractedState abstractState,
      boolean attachExtraInvariant) {
    CFANode node = abstractState.getNode();
    PathFormula generatingFormula = getPathFormula(abstractState,
        fmgr, attachExtraInvariant
    );

    return PolicyIntermediateState.of(node, generatingFormula, abstractState);
  }

  /**
   * @param attachExtraInvariant Whether the extra invariant should be attached.
   * @return Starting {@code PathFormula} associated with {@code abstractState}.
   * Does not include the constraints.
   */
  PathFormula getPathFormula(
      PolicyAbstractedState abstractState,
      FormulaManagerView fmgr,
      boolean attachExtraInvariant
  ) {
    BooleanFormula extraPredicate;
    if (attachExtraInvariant) {
      extraPredicate = fmgr.instantiate(abstractState.getExtraInvariant(),
          abstractState.getSSA());
    } else {
      extraPredicate = fmgr.getBooleanFormulaManager().makeBoolean(true);
    }
    return new PathFormula(extraPredicate, abstractState.getSSA(),
        abstractState.getPointerTargetSet(), 1);
  }

  private final Map<ToFormulaCacheKey, Formula> toFormulaCache =
      new HashMap<>();

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

    for (Entry<CIdExpression, Rational> entry : template.linearExpression) {
      Rational coeff = entry.getValue();
      CIdExpression declaration = entry.getKey();

      final Formula item;
      try {
        Formula f = pfmgr.expressionToFormula(
            contextFormula, declaration, dummyEdge);
        item = normalizeLength(f, maxBitvectorSize, fmgr);
      } catch (UnrecognizedCCodeException e) {
        throw new UnsupportedOperationException();
      }

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
      BigInteger maxValue = cfa.getMachineModel()
          .getMaximalIntegerValue(templateType);
      BigInteger minValue = cfa.getMachineModel()
          .getMinimalIntegerValue(templateType);

      // The bound obtained is larger than the highest representable
      // value, ignore it.
      if (v.compareTo(Rational.ofBigInteger(maxValue)) == 1
          || v.compareTo(Rational.ofBigInteger(minValue)) == -1) {
        logger.log(Level.FINE, "Bound too high, ignoring",
            v);
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
    for (Entry<CIdExpression, Rational> e: t.getLinearExpression()) {
      CIdExpression expr = e.getKey();
      if (sum == null) {
        sum = expr;
      } else {
        sum = expressionBuilder.buildBinaryExpressionUnchecked(
            sum, expr, BinaryOperator.PLUS);
      }
    }
    assert sum != null;
    return (CSimpleType) sum.getExpressionType();
  }

  private int getBitvectorSize(Template t, PathFormulaManager pfmgr,
                               PathFormula contextFormula, FormulaManagerView fmgr) {
    int length = 0;

    // Figure out the maximum bitvector size.
    for (Entry<CIdExpression, Rational> entry : t.linearExpression) {
      Formula item;
      try {
        item = pfmgr.expressionToFormula(
            contextFormula, entry.getKey(), dummyEdge);
      } catch (UnrecognizedCCodeException e) {
        throw new UnsupportedOperationException();
      }
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

  private Formula normalizeLength(Formula f, int maxBitvectorSize,
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
