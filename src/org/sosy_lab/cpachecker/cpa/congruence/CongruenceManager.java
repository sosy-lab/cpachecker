package org.sosy_lab.cpachecker.cpa.congruence;

import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.abe.ABEAbstractedState;
import org.sosy_lab.cpachecker.cpa.abe.ABEIntermediateState;
import org.sosy_lab.cpachecker.cpa.abe.ABEManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.cwriter.FormulaToCExpressionConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.templates.Template;
import org.sosy_lab.cpachecker.util.templates.Template.Kind;
import org.sosy_lab.cpachecker.util.templates.TemplatePrecision;
import org.sosy_lab.cpachecker.util.templates.TemplateToFormulaConversionManager;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BitvectorFormula;
import org.sosy_lab.solver.api.BitvectorFormulaManager;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.ProverEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

@Options(prefix="cpa.congruence")
public class CongruenceManager implements
                               ABEManager<CongruenceState, TemplatePrecision> {

  @Option(secure=true,
  description="Generate congruences for sums of variables "
      + "(<=> x and y have same/different evenness)")
  private boolean trackCongruenceSum = false;

  private final Solver solver;
  private final TemplateToFormulaConversionManager templateToFormulaConversionManager;
  private final BitvectorFormulaManager bvfmgr;
  private final FormulaManagerView fmgr;
  private final CongruenceStatistics statistics;
  private final PathFormulaManager pfmgr;
  private final TemplatePrecision precision;
  private final BooleanFormulaManager bfmgr;
  private final FormulaToCExpressionConverter formulaToCExpressionConverter;

  public CongruenceManager(
      Configuration config,
      Solver pSolver,
      TemplateToFormulaConversionManager pTemplateToFormulaConversionManager,
      FormulaManagerView pFmgr,
      CongruenceStatistics pStatistics,
      PathFormulaManager pPfmgr,
      LogManager logger,
      CFA cfa)
      throws InvalidConfigurationException {
    templateToFormulaConversionManager = pTemplateToFormulaConversionManager;
    config.inject(this);
    solver = pSolver;
    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    statistics = pStatistics;
    bvfmgr = fmgr.getBitvectorFormulaManager();
    pfmgr = pPfmgr;
    precision = new TemplatePrecision(logger, config, cfa,
        pTemplateToFormulaConversionManager);
    formulaToCExpressionConverter = new FormulaToCExpressionConverter(fmgr);
  }

  public CongruenceState join(
      CongruenceState a,
      CongruenceState b
  ) {
    Map<Template, Congruence> abstraction = Sets.intersection(
          a.getAbstraction().keySet(), b.getAbstraction().keySet())
        .stream()
        .filter(t -> a.getAbstraction().get(t).equals(b.getAbstraction().get(t)))
        .collect(Collectors.toMap(t -> t, t -> a.getAbstraction().get(t)));
    return new CongruenceState(
        abstraction,
        this,
        a.getPointerTargetSet(),
        a.getSSAMap(),
        b.getGeneratingState(),
        a.getNode()
    );
  }

  public boolean isLessOrEqual(CongruenceState a, CongruenceState b) {
    for (Entry<Template, Congruence> e : b) {
      Template template = e.getKey();
      Congruence congruence = e.getValue();
      Optional<Congruence> smallerCongruence = a.get(template);
      if (!smallerCongruence.isPresent()
          || smallerCongruence.get() != congruence) {
        return false;
      }
    }
    return true;
  }

  @Override
  public PrecisionAdjustmentResult performAbstraction(
      ABEIntermediateState<CongruenceState> pIntermediateState,
      TemplatePrecision precision,
      UnmodifiableReachedSet states,
      AbstractState fullState)
      throws CPATransferException, InterruptedException {
    return PrecisionAdjustmentResult.create(performAbstraction(
        pIntermediateState.getNode(),
        pIntermediateState.getPathFormula(),
        pIntermediateState.getBackpointerState().instantiate(),
        precision,
        pIntermediateState.getPathFormula().getPointerTargetSet(),
        pIntermediateState.getPathFormula().getSsa(),
        pIntermediateState
    ), precision, Action.CONTINUE);
  }

  public CongruenceState performAbstraction(
      CFANode node,
      PathFormula p,
      BooleanFormula startConstraints,
      TemplatePrecision pPrecision,
      PointerTargetSet pPointerTargetSet,
      SSAMap pSsaMap,
      ABEIntermediateState<CongruenceState> generatingState
  ) throws CPATransferException, InterruptedException {

    Map<Template, Congruence> abstraction = new HashMap<>();

    statistics.congruenceTimer.start();
    try (ProverEnvironment env = solver.newProverEnvironment()) {
      env.push(p.getFormula());
      env.push(startConstraints);

      for (Template template : pPrecision.getTemplatesForNode(node)) {
        if (!shouldUseTemplate(template)) {
          continue;
        }

        Formula formula = templateToFormulaConversionManager.toFormula(pfmgr, fmgr, template, p);

        // Test odd <=> isEven is UNSAT.
        try {
          env.push(fmgr.makeModularCongruence(formula, makeBv(bvfmgr, formula, 0), 2));
          if (env.isUnsat()) {
            abstraction.put(template, Congruence.ODD);
            continue;
          }
        } finally {
          env.pop();
        }

        // Test even <=> isOdd is UNSAT.
        try {
          env.push(
              fmgr.makeModularCongruence(formula, makeBv(bvfmgr, formula, 1), 2));
          if (env.isUnsat()) {
            abstraction.put(template, Congruence.EVEN);
          }
        } finally {
          env.pop();
        }
      }
    } catch (SolverException ex) {
      throw new CPATransferException("Solver exception: ", ex);
    } finally {
      statistics.congruenceTimer.stop();
    }

    return new CongruenceState(abstraction, this, pPointerTargetSet, pSsaMap,
        Optional.of(generatingState), node);
  }

  public BooleanFormula toFormula(
      CongruenceState state) {
    return toFormula(pfmgr, fmgr, state, new PathFormula(
        bfmgr.makeBoolean(true),
        state.getSSAMap(),
        state.getPointerTargetSet(),
        1
    ));
  }

  public BooleanFormula toFormulaUninstantiated(
      CongruenceState state,
      FormulaManagerView fmgr
      ) {
    PathFormulaManager pfmgrv;
    try {
      pfmgrv = new PathFormulaManagerImpl(
          fmgr,
          configuration,
          logManager,
          shutdownNotifier,
          cfa,
          AnalysisDirection.FORWARD
      );
    } catch (InvalidConfigurationException pE) {
      throw new UnsupportedOperationException("Could not construct path "
          + "formula manager", pE);
    }
    return fmgr.uninstantiate(
        toFormula(
            pfmgrv,
            fmgr,
            state,
            new PathFormula(
                fmgr.getBooleanFormulaManager().makeBoolean(true),
                state.getSSAMap(),
                state.getPointerTargetSet(),
                1
            )
        )
    );
  }

  public BooleanFormula toFormula(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      CongruenceState state,
      PathFormula ref) {
    Map<Template, Congruence> abstraction = state.getAbstraction();

    List<BooleanFormula> constraints = new ArrayList<>(abstraction.size());

    for (Entry<Template, Congruence> entry : abstraction.entrySet()) {
      Template template = entry.getKey();
      Congruence congruence = entry.getValue();

      Formula formula = templateToFormulaConversionManager.toFormula(pfmgr, fmgr, template, ref);
      Formula remainder;
      switch (congruence) {
        case ODD:
          remainder = makeBv(fmgr.getBitvectorFormulaManager(), formula, 1);
          break;
        case EVEN:
          remainder = makeBv(fmgr.getBitvectorFormulaManager(), formula, 0);
          break;
        default:
          throw new AssertionError("Unexpected case");
      }

      constraints.add(fmgr.makeModularCongruence(formula, remainder, 2));
    }
    return fmgr.getBooleanFormulaManager().and(constraints);
  }

  private boolean shouldUseTemplate(Template template) {
    return template.isIntegral() && (
        (template.getKind() == Kind.UPPER_BOUND)
        || (trackCongruenceSum && template.getKind() == Kind.SUM)
    );
  }

  private Formula makeBv(BitvectorFormulaManager bvfmgr, Formula other, int value) {
    return bvfmgr.makeBitvector(
        bvfmgr.getLength((BitvectorFormula) other),
        value);
  }

  @Override
  public CongruenceState getInitialState(
      CFANode node, StateSpacePartition pPartition) {
    return CongruenceState.empty(this, node);
  }

  @Override
  public TemplatePrecision getInitialPrecision(
      CFANode node, StateSpacePartition pPartition) {
    return precision;
  }


  @Override
  public Optional<ABEAbstractedState<CongruenceState>> strengthen(
      ABEAbstractedState<CongruenceState> pState,
      TemplatePrecision pPrecision,
      List<AbstractState> pOtherStates) {
    return Optional.of(pState);
  }

  @Override
  public boolean isLessOrEqual(
      ABEAbstractedState<CongruenceState> pState1,
      ABEAbstractedState<CongruenceState> pState2) {
    return isLessOrEqual(pState1.cast(), pState2.cast());
  }
}
