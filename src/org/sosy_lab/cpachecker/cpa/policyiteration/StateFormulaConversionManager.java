package org.sosy_lab.cpachecker.cpa.policyiteration;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.cwriter.FormulaToCExpressionConverter;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.templates.Template;
import org.sosy_lab.cpachecker.util.templates.TemplateToFormulaConversionManager;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Class responsible for converting states to formulas.
 */
public class StateFormulaConversionManager {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final FormulaToCExpressionConverter formulaToCExpressionConverter;
  private final TemplateToFormulaConversionManager
      templateToFormulaConversionManager;
  private final Configuration configuration;
  private final CFA cfa;
  private final LogManager logManager;
  private final ShutdownNotifier shutdownNotifier;

  public StateFormulaConversionManager(
      FormulaManagerView pFmgr,
      TemplateToFormulaConversionManager pTemplateToFormulaConversionManager,
      Configuration pConfiguration,
      CFA pCfa,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier) {
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
    templateToFormulaConversionManager = pTemplateToFormulaConversionManager;
    formulaToCExpressionConverter = new FormulaToCExpressionConverter(fmgr);
    configuration = pConfiguration;
    cfa = pCfa;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
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
      PolicyAbstractedState abstractState,
      boolean attachExtraInvariant) {

    // Returns the abstract state together with the conjoined extra invariant.
    List<BooleanFormula> constraints = new ArrayList<>();

    PathFormulaManager pfmgrv;
    try {
      pfmgrv = new PathFormulaManagerImpl(
          fmgrv, configuration, logManager,
          shutdownNotifier,
          cfa,
          AnalysisDirection.FORWARD
      );
    } catch (InvalidConfigurationException pE) {
      throw new UnsupportedOperationException("Could not construct path "
          + "formula manager", pE);
    }

    PathFormula inputPath = getPathFormula(abstractState, fmgrv, attachExtraInvariant);
    if (!fmgrv.getBooleanFormulaManager().isTrue(inputPath.getFormula())) {
      constraints.add(inputPath.getFormula());
    }

    if (attachExtraInvariant) {

      // Extra invariant.
      constraints.add(fmgrv.instantiate(
          abstractState.getExtraInvariant(), inputPath.getSsa()));
    }

    for (Entry<Template, PolicyBound> entry : abstractState) {
      Template template = entry.getKey();
      PolicyBound bound = entry.getValue();

      Formula t = templateToFormulaConversionManager.toFormula(
          pfmgrv, fmgrv, template, inputPath);

      BooleanFormula constraint = fmgrv.makeLessOrEqual(
          t, fmgrv.makeNumber(t, bound.getBound()), true);
      constraints.add(constraint);
    }
    return constraints;
  }

  public BooleanFormula getStartConstraintsWithExtraInvariant(
      PolicyIntermediateState state) {
    return bfmgr.and(abstractStateToConstraints(
        fmgr, state.getBackpointerState(), true));
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
}
