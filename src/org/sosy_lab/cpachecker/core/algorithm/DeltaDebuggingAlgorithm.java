/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix="dd")
public class DeltaDebuggingAlgorithm implements Algorithm {

  private final Algorithm algorithm;
  private final LogManager logger;
  private final ConfigurableProgramAnalysis pCpa;

  @Option(secure=true, description="Export auxiliary invariants used for induction.")
  private boolean test = false;

  public DeltaDebuggingAlgorithm(
      Algorithm algorithm,
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      Specification pSpecification,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      CFA cfa)
      throws InvalidConfigurationException {
    this.algorithm = algorithm;
    this.logger = logger;
    this.pCpa = pCpa;
    config.inject(this, DeltaDebuggingAlgorithm.class);

  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    try {
      readFile();
    } catch (IOException e) {
    }

    while (reachedSet.hasWaitingState()) {

      logger.log(Level.INFO, "Running");

      status = status.update(algorithm.run(reachedSet));

      assert ARGUtils.checkARG(reachedSet);


      final List<ARGState> targetStates =
          from(reachedSet)
              .transform(AbstractStates.toState(ARGState.class))
              .filter(AbstractStates.IS_TARGET_STATE)
              .toList();

      if (targetStates.size() > 0) {
        ARGState targetState = targetStates.get(0);

        ARGPath targetPath = ARGUtils.getOnePathTo(targetState);
        PathIterator it = targetPath.pathIterator();

        while (it.hasNext()) {
          it.advance();
          ARGState currentState = it.getAbstractState();

          // Get assumption from counter example
          for (AutomatonState s : AbstractStates.asIterable(currentState).filter(AutomatonState.class)) {
            boolean hasStuff = false;

            for (CExpression assume : from(s.getAssumptions()).filter(CExpression.class)) {
              hasStuff = true;
            }
            if (hasStuff) {
          }
        }
      }
    }
    return status;
  }

  private void readFile() throws IOException {
    File file = new File("src/org/sosy_lab/cpachecker/core/algorithm/assumptions.txt");
    BufferedReader br = Files.newBufferedReader(file.toPath());
    String st;

    while ((st = br.readLine()) != null) {
    }
  }
}
