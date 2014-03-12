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

import java.io.IOException;
import java.io.Writer;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
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
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.collect.Lists;

public class AutomatonControlledIterationStrategy implements TestGenIterationStrategy {

  private Configuration config;
  private LogManager logger;
  private ShutdownNotifier shutdownNotifier;
  private CFA cfa;
  private final TestGenIterationStrategy.IterationModel model;
  private ReachedSetFactory reachedSetFactory;
  private CPABuilder cpaBuilder;



  public AutomatonControlledIterationStrategy(StartupConfig startupConfig, CFA pCfa, IterationModel model,
      ReachedSetFactory pReachedSetFactory, CPABuilder pCpaBuilder) {
    super();
    this.reachedSetFactory = pReachedSetFactory;
    cpaBuilder = pCpaBuilder;
    config = startupConfig.getConfig();
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

      try (Writer w = Files.openOutputFile(automatonFile.toPath())) {
        ARGUtils.producePathAutomaton(w, "nextPathAutomaton", pNewPath);
      }

      ConfigurableProgramAnalysis nextCpa = cpaBuilder.buildCPAs(cfa, Lists.newArrayList(automatonFile.toPath()));

      if (model.getAlgorithm() instanceof CPAAlgorithm) {
        return CPAAlgorithm.create(nextCpa, logger, config, shutdownNotifier);
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
