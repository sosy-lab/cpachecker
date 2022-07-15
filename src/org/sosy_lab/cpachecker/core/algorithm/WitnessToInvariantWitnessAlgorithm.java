// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitnessFactory;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantWitnessWriter;

@Options(prefix = "invariantStore")
public class WitnessToInvariantWitnessAlgorithm implements Algorithm {

  @Option(secure = true, description = "The witness from which invariants should be generated.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path witness = Path.of("invariantwitness.yaml");

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;
  private final InvariantWitnessWriter invariantExporter;

  /**
   * Constructs a new instance of this class.
   *
   * @throws IOException If witness writer could not be instatiated
   */
  public WitnessToInvariantWitnessAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCFA,
      Specification pSpecification)
      throws InvalidConfigurationException, IOException {
    config = pConfig;
    config.inject(this);
    logger = pLogger;
    cfa = pCFA;
    shutdownNotifier = pShutdownNotifier;
    invariantExporter = InvariantWitnessWriter.getWriter(pConfig, pCFA, pSpecification, pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    Set<ExpressionTreeLocationInvariant> invariants;
    try {
      WitnessInvariantsExtractor invariantsExtractor =
          new WitnessInvariantsExtractor(config, logger, cfa, shutdownNotifier, witness);
      invariants = invariantsExtractor.extractInvariantsFromReachedSet();
    } catch (InvalidConfigurationException pE) {
      throw new CPAException(
          "Invalid Configuration while analyzing witness:\n" + pE.getMessage(), pE);
    }

    Set<InvariantWitness> invariantWitnesses = new HashSet<>();

    for (ExpressionTreeLocationInvariant invariant : invariants) {
      invariantWitnesses.addAll(
          InvariantWitnessFactory.getFactory(logger, cfa)
              .fromNodeAndInvariant(invariant.getLocation(), invariant.asExpressionTree()));
    }

    for (InvariantWitness invariantWitness : invariantWitnesses) {
      try {
        invariantExporter.exportInvariantWitness(invariantWitness);
      } catch (IOException e) {
        logger.log(Level.WARNING, "Could not write witness to file");
      }
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }
}
