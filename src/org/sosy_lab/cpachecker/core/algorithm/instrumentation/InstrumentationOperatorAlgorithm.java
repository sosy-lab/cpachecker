// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.InstrumentationProperty;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This algorithm instruments a CFA of program using intrumentation operator and instrumentation
 * automaton.
 *
 * <p>
 * Currently supported transformations are only no-overflow and termination to reachability.
 */
@Options (prefix = "instrumentation")
public class InstrumentationOperatorAlgorithm {
  private final CFA cfa;
  private final LogManager logger;
  private final CProgramScope cProgramScope;

  @Option(
      secure = true,
      description =
          "toggle the strategy to determine the hardcoded instrumentation automaton to be used\n"
              + "TERMINATION: transform termination to reachability\n"
              + "NOOVERFLOW: transform no-overflow to reachability")
  private InstrumentationProperty instrumentationProperty =
      InstrumentationProperty.TERMINATION;

  public InstrumentationOperatorAlgorithm(CFA pCfa, Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
    logger = pLogger;
    cProgramScope = new CProgramScope(pCfa, pLogger);
  }

  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // Output the collected CFA information into AllCFAInfos
     try (BufferedWriter writer =
        Files.newBufferedWriter(Paths.get("output/AllCFAInfos.txt"), StandardCharsets.UTF_8)) {
        StringBuilder allLoopInfos = new StringBuilder();
     } catch (IOException e) {
        logger.logException(Level.SEVERE, e, "The creation of file AllCFAInfos.txt failed!");
     }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }
}
