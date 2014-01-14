/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Files.DeleteOnCloseFile;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.CounterexampleChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

@Options(prefix="counterexample.checker")
public class CounterexampleCPAChecker implements CounterexampleChecker {

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final String filename;

  private final ARGCPA cpa;

  @Option(name="config",
      description="configuration file for counterexample checks with CPAchecker")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path configFile = Paths.get("config/explicitAnalysis-no-cbmc.properties");

  public CounterexampleCPAChecker(Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA pCfa, String pFilename) throws InvalidConfigurationException {
    this.logger = logger;
    config.inject(this);
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa = pCfa;
    this.filename = pFilename;
    cpa = null;
  }

  public CounterexampleCPAChecker(Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA pCfa, String pFilename,
      ARGCPA pCpa) throws InvalidConfigurationException {
    this.logger = logger;
    config.inject(this);
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa = pCfa;
    this.filename = pFilename;
    cpa = pCpa;
  }


  @Override
  public boolean checkCounterexample(ARGState pRootState,
      ARGState pErrorState, Set<ARGState> pErrorPathStates)
      throws CPAException, InterruptedException {

    // This temp file will be automatically deleted when the try block terminates.
    try (DeleteOnCloseFile automatonFile = Files.createTempFile("automaton", ".txt")) {

      try (Writer w = Files.openOutputFile(automatonFile.toPath())) {
        ARGUtils.producePathAutomaton(w, pRootState, pErrorPathStates,
            "CounterexampleToCheck", cpa.getCounterexamples().get(pErrorState));
      }

      return checkCounterexample(pRootState, automatonFile.toPath());

    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed("Could not write path automaton to file " + e.getMessage(), e);
    }
  }

  private boolean checkCounterexample(ARGState pRootState, Path automatonFile)
      throws CPAException, InterruptedException {

    CFANode entryNode = extractLocation(pRootState);

    try {
      Configuration lConfig = Configuration.builder()
              .loadFromFile(configFile)
              .setOption("specification", automatonFile.toAbsolutePath().toString())
              .build();
      ShutdownNotifier lShutdownNotifier = ShutdownNotifier.createWithParent(shutdownNotifier);
      ResourceLimitChecker.fromConfiguration(lConfig, logger, lShutdownNotifier).start();

      CoreComponentsFactory factory = new CoreComponentsFactory(lConfig, logger, lShutdownNotifier);
      ConfigurableProgramAnalysis lCpas = factory.createCPA(cfa, null);
      Algorithm lAlgorithm = factory.createAlgorithm(lCpas, filename, cfa, null);
      ReachedSet lReached = factory.createReachedSet();
      lReached.add(lCpas.getInitialState(entryNode), lCpas.getInitialPrecision(entryNode));

      lAlgorithm.run(lReached);

      lShutdownNotifier.requestShutdown("Analysis terminated");
      CPAs.closeCpaIfPossible(lCpas, logger);
      CPAs.closeIfPossible(lAlgorithm, logger);

      // counterexample is feasible if a target state is reachable
      return from(lReached).anyMatch(IS_TARGET_STATE);

    } catch (InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed("Invalid configuration in counterexample-check config: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(e.getMessage(), e);
    } catch (InterruptedException e) {
      shutdownNotifier.shutdownIfNecessary();
      throw new CounterexampleAnalysisFailed("Counterexample check aborted", e);
    }
  }

}