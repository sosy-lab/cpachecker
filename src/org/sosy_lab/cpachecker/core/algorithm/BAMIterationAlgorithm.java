/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import java.util.ArrayList;
import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ThreadModularReachedSet;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class BAMIterationAlgorithm implements Algorithm {

  private final Algorithm algorithm;
  private final LogManager logger;
  private final BAMCPA cpa;

  public BAMIterationAlgorithm(Algorithm algorithm, ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    this.algorithm = algorithm;
    this.logger = logger;
    this.cpa = (BAMCPA) pCpa;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    assert pReachedSet instanceof ThreadModularReachedSet;

    Collection<InferenceObject> objects = from(cpa.getData().getCache().getAllCachedReachedStates()).filter(InferenceObject.class).toSet();
    AlgorithmStatus status;

    do {
      status = algorithm.run(pReachedSet);
      if (from(pReachedSet).anyMatch(IS_TARGET_STATE)) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }

      Collection<ReachedSet> reachedSets = cpa.getData().getCache().getAllCachedReachedStates();
      Collection<InferenceObject> newObjects = new ArrayList<>();

      for (ReachedSet reachedSet : reachedSets) {
        for (AbstractState state : reachedSet.asCollection()) {
          if (state instanceof InferenceObject) {
            newObjects.add((InferenceObject) state);
          } else {
            //Collection<Adju>reachedSet.getPrecisions()
          }
        }
      }
      newObjects = from(reachedSets).filter(InferenceObject.class).toSet();
      if (objects.containsAll(newObjects)) {
        //fix point
        return status;
      }

      pReachedSet.clear();
      cpa.getData().clear();
      for (InferenceObject o : newObjects) {
        pReachedSet.add(o, SingletonPrecision.getInstance());
      }
    } while (true);
  }

}
