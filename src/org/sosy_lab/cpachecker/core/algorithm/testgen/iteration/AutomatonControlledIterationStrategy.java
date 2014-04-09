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
package org.sosy_lab.cpachecker.core.algorithm.testgen.iteration;

import static java.lang.String.format;
import static org.sosy_lab.cpachecker.core.algorithm.testgen.util.ReachedSetUtils.addReachedStatesToOtherReached;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Files.DeleteOnCloseFile;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenStatistics;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.Lists;

public class AutomatonControlledIterationStrategy extends AbstractIterationStrategy {

  private CFA cfa;
  private ConfigurableProgramAnalysis currentCPA;
  private List<Pair<AbstractState, Precision>> wrongStates;

  private int automatonCounter = 0;
  private StartupConfig startupConfig;

  // get a unique String for filenames per cpachecker instance
  private static String automatonSuffix = UUID.randomUUID().toString();
  private boolean produceDebugFiles;

  public AutomatonControlledIterationStrategy(StartupConfig startupConfig, CFA pCfa, IterationModel model,
      ReachedSetFactory pReachedSetFactory, TestGenStatistics pStats, boolean pProduceDebugFiles) {
    super(startupConfig, model, pReachedSetFactory, pStats);
    this.startupConfig = startupConfig;
    cfa = pCfa;
    produceDebugFiles = pProduceDebugFiles;
    wrongStates = Lists.newLinkedList();
  }

  @Override
  public void updateIterationModelForNextIteration(PredicatePathAnalysisResult pResult) {
    // TODO might have to reinit the reached-sets
    getModel().setAlgorithm(createAlgorithmForNextIteration(pResult));
    if (automatonCounter > 1) {
      wrongStates.add(Pair.of((AbstractState) pResult.getWrongState(),
          getLocalReached().getPrecision(pResult.getWrongState())));
    }
    ReachedSet newReached = reachedSetFactory.create();
    AbstractState initialState = getModel().getGlobalReached().getFirstState();
    CFANode initialLoc = AbstractStates.extractLocation(initialState);
    initialState = currentCPA.getInitialState(initialLoc);
    newReached.add(initialState, currentCPA.getInitialPrecision(initialLoc));
    getModel().setLocalReached(newReached);
//    for (Pair<AbstractState, Precision> wrongState : wrongStates) {
//      ReachedSetUtils.addToReachedOnly(getLocalReached(), wrongState.getFirst(), wrongState.getSecond());
//
//    }
  }

  @Override
  protected void updateReached() {
    addReachedStatesToOtherReached(getModel().getLocalReached(), getModel().getGlobalReached());
  }

  private Algorithm createAlgorithmForNextIteration(PredicatePathAnalysisResult pResult) {

    if (produceDebugFiles) {

      String outputDir;
      try {
        outputDir = new FileTypeConverter(startupConfig.getConfig()).getOutputDirectory();
      } catch (InvalidConfigurationException e1) {
        throw new IllegalStateException("Unable to create the Algorithm for next Iteration", e1);
      }

      String filename = format("next_automaton%s_%s.spc", automatonCounter++, automatonSuffix);
      Path path = Paths.get(outputDir, "automaton", filename);

      generateAutomatonFileForNextIteration(pResult, path);
      return createNewAlgorithm(path);

    } else {
      try (DeleteOnCloseFile tempFile = Files.createTempFile("next-automaton", ".spc")) {

        generateAutomatonFileForNextIteration(pResult, tempFile.toPath().toAbsolutePath());
        return createNewAlgorithm(tempFile.toPath().toAbsolutePath());

      } catch (IOException e) {
        throw new IllegalStateException("Unable to create the Algorithm for next Iteration", e);
      }
    }
  }

  private Algorithm createNewAlgorithm(Path pAutomatonPath) {
    // construct a new CPAAlgorithm with a new set of CPAs which includes an automaton CPA which guides
    // the the value analysis in the next iteration of the testgen algorithm
    try {
      Configuration lConfig =
          Configuration.builder().copyFrom(config).clearOption("analysis.algorithm.testGen")
              .setOption("EvalOnlyOnePathAutomaton.cpa.automaton.inputFile", pAutomatonPath.getAbsolutePath()).build();
      CPABuilder localBuilder =
          new CPABuilder(lConfig, logger, ShutdownNotifier.createWithParent(shutdownNotifier), reachedSetFactory);

      currentCPA = localBuilder.buildCPAs(cfa);

      if (getModel().getAlgorithm() instanceof CPAAlgorithm) {
        return CPAAlgorithm.create(currentCPA, logger, lConfig, shutdownNotifier);
      } else {
        throw new InvalidConfigurationException("Generating a new Algorithm here only Works if the "
            + "Algorithm is a CPAAlgorithm");
      }

    } catch (InvalidConfigurationException | CPAException e) {
      throw new IllegalStateException("Unable to create the Algorithm for next Iteration", e);
    }
  }

  private void generateAutomatonFileForNextIteration(PredicatePathAnalysisResult pResult, Path pFilePath)
  {
    stats.beforeAutomationFileGeneration();
    try (Writer w = Files.openOutputFile(pFilePath.toAbsolutePath(), Charset.forName("UTF8"))) {

      ARGPath argPath = pResult.getPath();
      CounterexampleInfo ci = CounterexampleInfo.feasible(argPath, pResult.getTrace().getModel());

      ARGUtils.produceTestGenPathAutomaton(w, argPath.getFirst().getFirst(), argPath.getStateSet(),
          "nextPathAutomaton", ci, true);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create the Algorithm for next Iteration", e);
    }

    stats.afterAutomatonFileGeneration();
  }

}
