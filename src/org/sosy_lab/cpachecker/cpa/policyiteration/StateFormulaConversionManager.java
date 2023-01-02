// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.templates.Template;
import org.sosy_lab.cpachecker.util.templates.TemplateToFormulaConversionManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

/** Class responsible for converting states to formulas. */
@Options(prefix = "cpa.lpi")
public class StateFormulaConversionManager {

  @Option(secure = true, description = "Remove redundant items when abstract values.")
  private boolean simplifyDotOutput = false;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final TemplateToFormulaConversionManager templateToFormulaConversionManager;
  private final Configuration configuration;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final PolicyDotWriter dotWriter;
  private final PathFormulaManager pfmgr;
  private final Solver solver;

  public StateFormulaConversionManager(
      FormulaManagerView pFormulaManager,
      TemplateToFormulaConversionManager pTemplateToFormulaConversionManager,
      Configuration pConfiguration,
      CFA pCfa,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      PathFormulaManager pPathFormulaManager,
      Solver pSolver)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    fmgr = pFormulaManager;
    bfmgr = pFormulaManager.getBooleanFormulaManager();
    templateToFormulaConversionManager = pTemplateToFormulaConversionManager;
    configuration = pConfiguration;
    cfa = pCfa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    pfmgr = pPathFormulaManager;
    solver = pSolver;
    dotWriter = new PolicyDotWriter();
  }

  /**
   * Returns _instantiated_ set of constraints.
   *
   * @param attachExtraInvariant Attach the invariant supplied by other analysis.
   */
  List<BooleanFormula> abstractStateToConstraints(
      FormulaManagerView fmgrv, PolicyAbstractedState abstractState, boolean attachExtraInvariant) {

    // Returns the abstract state together with the conjoined extra invariant.
    List<BooleanFormula> constraints = new ArrayList<>();

    PathFormulaManager pfmgrv;
    try {
      pfmgrv =
          new PathFormulaManagerImpl(
              fmgrv, configuration, logger, shutdownNotifier, cfa, AnalysisDirection.FORWARD);
    } catch (InvalidConfigurationException pE) {
      throw new UnsupportedOperationException("Could not construct path " + "formula manager", pE);
    }

    PathFormula inputPath = getPathFormula(abstractState, attachExtraInvariant);
    if (!fmgrv.getBooleanFormulaManager().isTrue(inputPath.getFormula())) {
      constraints.add(inputPath.getFormula());
    }

    if (attachExtraInvariant) {

      // Extra invariant.
      constraints.add(fmgrv.instantiate(abstractState.getExtraInvariant(), inputPath.getSsa()));
    }

    for (Entry<Template, PolicyBound> entry : abstractState) {
      Template template = entry.getKey();
      PolicyBound bound = entry.getValue();

      constraints.add(templateToConstraint(template, bound, pfmgrv, fmgrv, inputPath));
    }
    return constraints;
  }

  BooleanFormula templateToConstraint(
      Template template,
      PolicyBound bound,
      PathFormulaManager pfmgrv,
      FormulaManagerView fmgrv,
      PathFormula inputPath) {
    Formula t = templateToFormulaConversionManager.toFormula(pfmgrv, fmgrv, template, inputPath);
    return fmgrv.makeLessOrEqual(t, fmgrv.makeNumber(t, bound.getBound()), true);
  }

  public BooleanFormula getStartConstraintsWithExtraInvariant(PolicyIntermediateState state) {
    return bfmgr.and(abstractStateToConstraints(fmgr, state.getBackpointerState(), true));
  }

  /** Return representation of an {@code abstractState} as a {@link PolicyIntermediateState}. */
  PolicyIntermediateState abstractStateToIntermediate(
      PolicyAbstractedState abstractState, boolean attachExtraInvariant) {
    CFANode node = abstractState.getNode();
    PathFormula generatingFormula = getPathFormula(abstractState, attachExtraInvariant);

    return PolicyIntermediateState.of(node, generatingFormula, abstractState);
  }

  /**
   * Return starting {@code PathFormula} associated with {@code abstractState}. Does not include the
   * constraints.
   *
   * @param attachExtraInvariant Whether the extra invariant should be attached.
   */
  PathFormula getPathFormula(PolicyAbstractedState abstractState, boolean attachExtraInvariant) {
    PathFormula result =
        pfmgr.makeEmptyPathFormulaWithContext(
            abstractState.getSSA(), abstractState.getPointerTargetSet());
    if (attachExtraInvariant) {
      result = pfmgr.makeAnd(result, abstractState.getExtraInvariant());
    }
    return result;
  }

  public String toDOTLabel(Map<Template, PolicyBound> pAbstraction) {
    if (!simplifyDotOutput) {
      return dotWriter.toDOTLabel(pAbstraction);
    }

    PathFormula inputPath = pfmgr.makeEmptyPathFormula();

    Map<Template, BooleanFormula> templatesToConstraints =
        ImmutableMap.copyOf(
            Maps.transformEntries(
                pAbstraction,
                (key, value) -> templateToConstraint(key, value, pfmgr, fmgr, inputPath)));
    List<Template> templates = new ArrayList<>(pAbstraction.keySet());
    Set<Template> nonRedundant = new HashSet<>(templates);
    for (Template t : templates) {
      // mark redundant templates as such
      BooleanFormula constraint = templatesToConstraints.get(t);

      // if others imply the constraint, remove it.
      BooleanFormula othersConstraint =
          nonRedundant.stream()
              .filter(t2 -> t2 != t)
              .map(templatesToConstraints::get)
              .collect(bfmgr.toConjunction());

      try {
        if (solver.implies(othersConstraint, constraint)) {
          nonRedundant.remove(t);
        }
      } catch (SolverException | InterruptedException pE) {
        logger.logException(
            Level.WARNING,
            pE,
            "Failed simplifying the " + "abstraction before rendering, converting as it is.");
        simplifyDotOutput = false;
        return dotWriter.toDOTLabel(pAbstraction);
      }
    }

    Map<Template, PolicyBound> filteredAbstraction =
        Maps.filterKeys(pAbstraction, t -> nonRedundant.contains(t));
    return dotWriter.toDOTLabel(filteredAbstraction);
  }
}
