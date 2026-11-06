// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.witnesses;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CEXExportOptions;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.CounterexampleToWitness;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.LocationInvariantsToCorrectnessWitness;

/**
 * Writes the YAML witnesses of an analysis that records its proof as the states it reaches at
 * program locations, and a violation as the edge whose execution violates the specification.
 *
 * <p>Neither representation is an ARG: a recorded state is an independent snapshot without parent
 * links, and a violation has only the edge that identifies the target, not the execution that led
 * there. The standard exporters are therefore used directly, each with the data it needs, instead
 * of through {@link org.sosy_lab.cpachecker.cpa.arg.ARGStatistics}, which reads both from an ARG.
 */
public class LocationWitnessExporter {

  private final LocationInvariantsToCorrectnessWitness correctnessWitness;
  private final CounterexampleToWitness violationWitness;
  private final CEXExportOptions violationWitnessOptions;
  private final LocationStateFactory locations;
  private final LogManager logger;

  /** Whether the witness formats can describe the analyzed program at all. */
  private final boolean isC;

  public LocationWitnessExporter(
      Configuration pConfig, LogManager pLogger, Specification pSpecification, CFA pCfa)
      throws InvalidConfigurationException {
    correctnessWitness =
        new LocationInvariantsToCorrectnessWitness(pConfig, pCfa, pSpecification, pLogger);
    violationWitness = new CounterexampleToWitness(pConfig, pCfa, pSpecification, pLogger);
    violationWitnessOptions = new CEXExportOptions(pConfig);
    locations = new LocationStateFactory(pCfa, AnalysisDirection.FORWARD, pConfig);
    logger = pLogger;
    isC = pCfa.getMetadata().getInputLanguage() == Language.C;
    if (!isC) {
      pLogger.log(
          Level.WARNING, "Cannot export witnesses in YAML format for languages other than C.");
    }
  }

  /** Whether any YAML witness would be written at all. */
  public boolean isYamlWitnessExportEnabled() {
    return isCorrectnessWitnessEnabled() || isViolationWitnessEnabled();
  }

  private boolean isCorrectnessWitnessEnabled() {
    return isC && correctnessWitness.isExportEnabled();
  }

  private boolean isViolationWitnessEnabled() {
    return isC && violationWitnessOptions.hasYamlWitnessExport();
  }

  /**
   * Write the correctness witness for the given invariants.
   *
   * @param pInvariants the recorded states of each location; the disjunction of the states of a
   *     location has to hold whenever the program is at that location
   * @param pReached the reached set of the analysis, used only to report the likely quality of the
   *     witness
   */
  public void writeCorrectnessWitness(
      Multimap<CFANode, ImmutableList<ExpressionTreeReportingState>> pInvariants,
      UnmodifiableReachedSet pReached) {
    if (!isCorrectnessWitnessEnabled()) {
      return;
    }
    try {
      correctnessWitness.exportInvariants(pInvariants, pReached);
    } catch (IOException | ReportingMethodNotImplementedException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not export the YAML correctness witness of the analysis.");
    } catch (InterruptedException e) {
      logger.logUserException(Level.WARNING, e, "Could not export witness due to interruption");
    }
  }

  /**
   * Write the violation witness for an execution that violates the specification while it takes the
   * given edge. The witness holds this edge as the only step of the counterexample, because the
   * analysis does not keep the execution that led there.
   */
  public void writeViolationWitness(CFAEdge pViolatingEdge, DummyTargetState pTarget) {
    if (!isViolationWitnessEnabled()) {
      return;
    }
    ARGState root = new ARGState(locations.getState(pViolatingEdge.getPredecessor()), null);
    ARGState target =
        new ARGState(
            new CompositeState(
                ImmutableList.of(locations.getState(pViolatingEdge.getSuccessor()), pTarget)),
            root);
    CounterexampleInfo counterexample =
        CounterexampleInfo.feasibleImprecise(
            new ARGPath(ImmutableList.of(root, target), ImmutableList.of(pViolatingEdge)));
    try {
      violationWitness.export(
          counterexample,
          violationWitnessOptions.getYamlWitnessPathTemplate(),
          counterexample.getUniqueId());
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not export the YAML violation witness of the analysis.");
    }
  }
}
