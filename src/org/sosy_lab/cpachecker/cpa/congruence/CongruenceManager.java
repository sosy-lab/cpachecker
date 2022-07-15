// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.congruence;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.abe.ABEAbstractedState;
import org.sosy_lab.cpachecker.cpa.abe.ABEIntermediateState;
import org.sosy_lab.cpachecker.cpa.abe.ABEManager;
import org.sosy_lab.cpachecker.cpa.abe.ABEState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.templates.Template;
import org.sosy_lab.cpachecker.util.templates.Template.Kind;
import org.sosy_lab.cpachecker.util.templates.TemplatePrecision;
import org.sosy_lab.cpachecker.util.templates.TemplateToFormulaConversionManager;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.congruence")
public class CongruenceManager implements ABEManager<CongruenceState, TemplatePrecision> {

  @Option(
      secure = true,
      description =
          "Generate congruences for sums of variables "
              + "(<=> x and y have same/different evenness)")
  private boolean trackCongruenceSum = false;

  private final Solver solver;
  private final TemplateToFormulaConversionManager templateToFormulaConversionManager;
  private final BitvectorFormulaManager bvfmgr;
  private final FormulaManagerView fmgr;
  private final CongruenceStatistics statistics;
  private final PathFormulaManager pfmgr;
  private final TemplatePrecision precision;
  private final Configuration configuration;
  private final CFA cfa;
  private final LogManager logManager;
  private final ShutdownNotifier shutdownNotifier;

  public CongruenceManager(
      Configuration config,
      Solver pSolver,
      TemplateToFormulaConversionManager pTemplateToFormulaConversionManager,
      FormulaManagerView pFormulaManager,
      CongruenceStatistics pStatistics,
      PathFormulaManager pPathFormulaManager,
      LogManager logger,
      CFA pCFA,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    config.inject(this);
    templateToFormulaConversionManager = pTemplateToFormulaConversionManager;
    cfa = pCFA;
    logManager = logger;
    configuration = config;
    solver = pSolver;
    fmgr = pFormulaManager;
    statistics = pStatistics;
    bvfmgr = fmgr.getBitvectorFormulaManager();
    pfmgr = pPathFormulaManager;
    precision = new TemplatePrecision(logger, config, cfa, pTemplateToFormulaConversionManager);
    shutdownNotifier = pShutdownNotifier;
  }

  public CongruenceState join(CongruenceState a, CongruenceState b) {
    Map<Template, Congruence> abstraction =
        Maps.difference(a.getAbstraction(), b.getAbstraction()).entriesInCommon();
    return new CongruenceState(
        abstraction,
        this,
        a.getPointerTargetSet(),
        a.getSSAMap(),
        b.getGeneratingState(),
        a.getNode());
  }

  @Override
  public PrecisionAdjustmentResult performAbstraction(
      ABEIntermediateState<CongruenceState> pIntermediateState,
      TemplatePrecision pPrecision,
      UnmodifiableReachedSet states,
      AbstractState fullState)
      throws CPATransferException, InterruptedException {
    return PrecisionAdjustmentResult.create(
        performAbstraction(
            pIntermediateState.getNode(),
            pIntermediateState.getPathFormula(),
            pIntermediateState.getBackpointerState().instantiate(),
            pPrecision,
            pIntermediateState.getPathFormula().getPointerTargetSet(),
            pIntermediateState.getPathFormula().getSsa(),
            pIntermediateState,
            states,
            fullState),
        pPrecision,
        Action.CONTINUE);
  }

  public CongruenceState performAbstraction(
      CFANode node,
      PathFormula p,
      BooleanFormula startConstraints,
      TemplatePrecision pPrecision,
      PointerTargetSet pPointerTargetSet,
      SSAMap pSsaMap,
      ABEIntermediateState<CongruenceState> generatingState,
      UnmodifiableReachedSet states,
      AbstractState fullState)
      throws CPATransferException, InterruptedException {

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
          env.push(
              fmgr.makeModularCongruence(
                  formula, makeBv(bvfmgr, formula, 0), 2, !template.isUnsigned()));
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
              fmgr.makeModularCongruence(
                  formula, makeBv(bvfmgr, formula, 1), 2, !template.isUnsigned()));
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

    CongruenceState out =
        new CongruenceState(
            abstraction, this, pPointerTargetSet, pSsaMap, Optional.of(generatingState), node);
    Optional<ABEAbstractedState<CongruenceState>> sibling = findSibling(states, fullState, out);
    if (sibling.isPresent()) {
      out = join(sibling.orElseThrow().cast(), out);
    }
    return out;
  }

  public BooleanFormula toFormula(CongruenceState state) {
    return toFormula(
        pfmgr,
        fmgr,
        state,
        pfmgr.makeEmptyPathFormulaWithContext(state.getSSAMap(), state.getPointerTargetSet()));
  }

  public BooleanFormula toFormulaUninstantiated(
      CongruenceState state, FormulaManagerView pFormulaManager) {
    PathFormulaManager pfmgrv;
    try {
      pfmgrv =
          new PathFormulaManagerImpl(
              pFormulaManager,
              configuration,
              logManager,
              shutdownNotifier,
              cfa,
              AnalysisDirection.FORWARD);
    } catch (InvalidConfigurationException pE) {
      throw new UnsupportedOperationException("Could not construct path " + "formula manager", pE);
    }
    return pFormulaManager.uninstantiate(
        toFormula(
            pfmgrv,
            pFormulaManager,
            state,
            pfmgrv.makeEmptyPathFormulaWithContext(
                state.getSSAMap(), state.getPointerTargetSet())));
  }

  public BooleanFormula toFormula(
      PathFormulaManager pPathFormulaManager,
      FormulaManagerView pFormulaManager,
      CongruenceState state,
      PathFormula ref) {
    Map<Template, Congruence> abstraction = state.getAbstraction();

    List<BooleanFormula> constraints = new ArrayList<>(abstraction.size());

    for (Entry<Template, Congruence> entry : abstraction.entrySet()) {
      Template template = entry.getKey();
      Congruence congruence = entry.getValue();

      Formula formula =
          templateToFormulaConversionManager.toFormula(
              pPathFormulaManager, pFormulaManager, template, ref);
      Formula remainder;
      switch (congruence) {
        case ODD:
          remainder = makeBv(pFormulaManager.getBitvectorFormulaManager(), formula, 1);
          break;
        case EVEN:
          remainder = makeBv(pFormulaManager.getBitvectorFormulaManager(), formula, 0);
          break;
        default:
          throw new AssertionError("Unexpected case");
      }

      constraints.add(
          pFormulaManager.makeModularCongruence(formula, remainder, 2, !template.isUnsigned()));
    }
    return pFormulaManager.getBooleanFormulaManager().and(constraints);
  }

  private boolean shouldUseTemplate(Template template) {
    return template.isIntegral()
        && ((template.getKind() == Kind.UPPER_BOUND)
            || (trackCongruenceSum && template.getKind() == Kind.SUM));
  }

  private Formula makeBv(BitvectorFormulaManager pBvfmgr, Formula other, int value) {
    return pBvfmgr.makeBitvector(pBvfmgr.getLength((BitvectorFormula) other), value);
  }

  @Override
  public CongruenceState getInitialState(CFANode node, StateSpacePartition pPartition) {
    return CongruenceState.empty(this, node);
  }

  @Override
  public TemplatePrecision getInitialPrecision(CFANode node, StateSpacePartition pPartition) {
    return precision;
  }

  @Override
  public Optional<ABEAbstractedState<CongruenceState>> strengthen(
      ABEAbstractedState<CongruenceState> pState,
      TemplatePrecision pPrecision,
      Iterable<AbstractState> pOtherStates) {
    return Optional.of(pState);
  }

  @Override
  public boolean isLessOrEqual(
      ABEAbstractedState<CongruenceState> pState1, ABEAbstractedState<CongruenceState> pState2) {
    return isLessOrEqual(pState1.cast(), pState2.cast());
  }

  public boolean isLessOrEqual(CongruenceState a, CongruenceState b) {
    for (Entry<Template, Congruence> e : b) {
      Template template = e.getKey();
      Congruence congruence = e.getValue();
      Optional<Congruence> smallerCongruence = a.get(template);
      if (!smallerCongruence.isPresent() || smallerCongruence.orElseThrow() != congruence) {
        return false;
      }
    }
    return true;
  }

  private Optional<ABEAbstractedState<CongruenceState>> findSibling(
      UnmodifiableReachedSet states,
      AbstractState pArgState,
      ABEAbstractedState<CongruenceState> state) {
    Set<CongruenceState> filteredSiblings =
        ImmutableSet.copyOf(
            AbstractStates.projectToType(states.getReached(pArgState), CongruenceState.class));
    if (filteredSiblings.isEmpty()) {
      return Optional.empty();
    }

    // We follow the chain of backpointers until we intersect something in the
    // same partition.
    // The chain is necessary as we might have nested loops.
    ABEState<CongruenceState> a = state;
    while (true) {
      if (a.isAbstract()) {
        ABEAbstractedState<CongruenceState> aState = a.asAbstracted();

        if (filteredSiblings.contains(aState)) {
          return Optional.of(aState);
        } else {
          if (!aState.getGeneratingState().isPresent()) {
            // Empty.
            return Optional.empty();
          }
          a = aState.getGeneratingState().orElseThrow().getBackpointerState();
        }
      } else {
        ABEIntermediateState<CongruenceState> iState = a.asIntermediate();
        a = iState.getBackpointerState();
      }
    }
  }
}
