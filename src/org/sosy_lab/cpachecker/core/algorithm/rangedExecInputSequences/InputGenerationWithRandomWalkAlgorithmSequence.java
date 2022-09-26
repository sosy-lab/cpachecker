// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.rangedExecInputSequences;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.randomWalk.RandomWalkState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.inputGenWithRandomWalk")
public class InputGenerationWithRandomWalkAlgorithmSequence implements Algorithm {

  private final SequenceGenUtils utils;
  private final Algorithm algorithm;
  private final LogManager logger;
  private final CFA cfa;
  private int reties;

  @Option(secure = true, description = "Number of testcases to generate")
  private int numberOfTestcases = 2;

  @Option(secure = true, description = "Number of testcases to generate")
  private int abortAfterUnsuccessfulRetries = numberOfTestcases * 5;

  private final ConfigurableProgramAnalysis cpa;

  private final ReachedSetFactory factory;

  private final Map<RandomWalkState, ARGPath> computedPath;

  @Option(
      secure = true,
      name = "namesOfRandomFunctions",
      description =
          "List of names (or a part of the name) of functions, that return a random value")
  private ImmutableSet<String> namesOfRandomFunctions =
      ImmutableSet.of("rand", "__VERIFIER_nondet_");

  @Option(
      secure = true,
      name = "testcaseName",
      description = "Names of the files for the testcases")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testcaseName = PathTemplate.ofFormatString("testcase.%d.xml");

  public InputGenerationWithRandomWalkAlgorithmSequence(
      ConfigurableProgramAnalysis pCpa,
      Algorithm pAlgorithm,
      CFA pCfa,
      LogManager pLogger,
      Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    algorithm = pAlgorithm;
    cfa = pCfa;
    cpa = pCpa;
    logger = pLogger;

    computedPath = new HashMap<>();
    factory = new ReachedSetFactory(pConfig, pLogger);
    reties = 0;

    utils = new SequenceGenUtils(namesOfRandomFunctions, pLogger);

    if (testcaseName == null) {
      testcaseName = PathTemplate.ofFormatString("testcase.%d.xml");
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    while (reties < this.abortAfterUnsuccessfulRetries
        && computedPath.size() < this.numberOfTestcases) {

      algorithm.run(reachedSet);
      final Set<ARGPath> allPathsFromTo =
          ARGUtils.getAllPathsFromTo(
              AbstractStates.extractStateByType(reachedSet.getFirstState(), ARGState.class),
              AbstractStates.extractStateByType(reachedSet.getLastState(), ARGState.class));
      final ARGPath newPath = allPathsFromTo.stream().findFirst().orElseThrow();
      final RandomWalkState newState =
          AbstractStates.extractStateByType(newPath.getLastState(), RandomWalkState.class);
      if (!computedPath.containsKey(newState)) {
        logger.log(Level.INFO, "Generated a new testcase");
        computedPath.put(newState, newPath);
      } else {
        reties++;
      }
      reachedSet =
          factory.createAndInitialize(
              cpa, cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
    }
    List<ARGPath> generatedPaths = new ArrayList<>(computedPath.values());
    generatedPaths.sort(
        Comparator.comparing(
            pO -> AbstractStates.extractStateByType(pO.getLastState(), RandomWalkState.class)));
    generateTestcases(generatedPaths);

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private void generateTestcases(List<ARGPath> pComputedPath) throws CPAException {
    for (int i = 0; i < pComputedPath.size(); i++) {
      ARGPath path = pComputedPath.get(i);
      try {
        List<Boolean> inputs = utils.computeSequenceForLoopbound(path);
        utils.printFileToOutput(inputs, testcaseName.getPath(i));
      } catch (SolverException | IOException | InterruptedException pE) {
        throw new CPAException(Throwables.getStackTraceAsString(pE));
      }
    }
  }
}
