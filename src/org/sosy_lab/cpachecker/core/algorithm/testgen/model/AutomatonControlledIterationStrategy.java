/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.testgen.model;

import static java.lang.String.format;

import java.io.IOException;
import java.io.Writer;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Files.DeleteOnCloseFile;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.StartupConfig;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;


public class AutomatonControlledIterationStrategy implements TestGenIterationStrategy {

  private Configuration config;
  private CPABuilder cpaBuilder;
  private LogManager logger;
  private ShutdownNotifier shutdownNotifier;
  private CFA cfa;
  private final TestGenIterationStrategy.IterationModel model;

  public AutomatonControlledIterationStrategy(StartupConfig startupConfig, CPABuilder pCpaBuilder, CFA pCfa, IterationModel model) {
    super();
    config = startupConfig.getConfig();
    cpaBuilder = pCpaBuilder;
    logger = startupConfig.getLog();
    shutdownNotifier = startupConfig.getNotifier();
    cfa = pCfa;
    this.model = model;
  }

  @Override
  public void updateIterationModelForNextIteration(PredicatePathAnalysisResult pResult) {
    // TODO might have to reinit the reached-sets
    model.setAlgorithm(createAlgorithmForNextIteration(pResult.getTrace()));

  }

  @Override
  public IterationModel getModel() {
    return model;
  }

  @Override
  public boolean runAlgorithm() throws PredicatedAnalysisPropertyViolationException, CPAException, InterruptedException {
    return model.getAlgorithm().run(model.getLocalReached());
  }

  private Algorithm createAlgorithmForNextIteration(CounterexampleTraceInfo pNewPath) {

    // This temp file will be automatically deleted when the try block terminates.
    try (DeleteOnCloseFile automatonFile = Files.createTempFile("next_automaton", ".txt")) {

      ConfigurationBuilder builder = Configuration.builder().copyFrom(config);
      // TODO: check if we really can use multiple specification files this way
      String automatonAbsPath = automatonFile.toPath().toAbsolutePath().toString();
      String originalSpec = config.getProperty("specification");
      builder = builder.setOption("specification", format("%s,%s", automatonAbsPath, originalSpec));
      Configuration nextConfig = builder.build();


      try (Writer w = Files.openOutputFile(automatonFile.toPath())) {
        ARGUtils.producePathAutomaton(w, "nextPathAutomaton", pNewPath);
      }

      ConfigurableProgramAnalysis nextCpa = cpaBuilder.buildCPAs(cfa);

      if (model.getAlgorithm() instanceof CPAAlgorithm) {
        return CPAAlgorithm.create(nextCpa, logger, nextConfig, shutdownNotifier);
      } else {
        throw new InvalidConfigurationException("Generating a new Algorithm here only Works if the "
            + "Algorithm is a CPAAlgorithm");
      }

    } catch (IOException | InvalidConfigurationException | CPAException e) {
      // TODO: use another exception?
      throw new IllegalStateException("Unable to create the Algorithm for next Iteration", e);
    }
  }

}
