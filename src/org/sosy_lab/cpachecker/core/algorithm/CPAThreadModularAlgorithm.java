/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.base.Preconditions;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CompatibilityCheck;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisTM;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCovering;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationTM;
import org.sosy_lab.cpachecker.core.interfaces.WaitlistElement;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ThreadModularReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public class CPAThreadModularAlgorithm extends AbstractCPAAlgorithm {

  private static class CPAThreadModularStatistics implements Statistics {

    private Timer inferenceObjectsAdd = new Timer();
    private Timer statesAdd = new Timer();

    private int newStatesForInferenceCount = 0;
    private int inferenceObjectsStop = 0;
    private int inferenceObjectsPass = 0;
    private int inferenceObjectsMerge = 0;
    private int statesStop = 0;
    private int statesPass = 0;
    private int statesMerge = 0;
    private int compatiblePairs = 0;
    private int incompatiblePairs = 0;


    @Override
    public String getName() {
      return "CPA thread modular algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      out.println("Number of states due to inference objects:  " + newStatesForInferenceCount);
      out.println("Number of stops for inference objects:      " + inferenceObjectsStop);
      out.println("Number of passes for inference objects:     " + inferenceObjectsPass);
      out.println("Number of merges for inference objects:     " + inferenceObjectsMerge);
      out.println("Number of stops for states:                 " + statesStop);
      out.println("Number of passes for states:                " + statesPass);
      out.println("Number of merges for states:                " + statesMerge);
      out.println();
      out.println("Number of compatible pairs:                 " + compatiblePairs);
      out.println("Number of incompatible pairs:               " + incompatiblePairs);
      out.println();
      out.println("  Time for inference objects update:          " + inferenceObjectsAdd);
      out.println("  Time for states update: " + statesAdd);
    }
  }

  @Options(prefix = "cpa")
  public static class ThreadModularAlgorithmFactory {

    private final ConfigurableProgramAnalysis cpa;
    private final LogManager logger;
    private final ShutdownNotifier shutdownNotifier;

    public ThreadModularAlgorithmFactory(ConfigurableProgramAnalysis cpa, LogManager logger,
        Configuration config, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {

      config.inject(this);
      this.cpa = cpa;
      this.logger = logger;
      this.shutdownNotifier = pShutdownNotifier;

    }

    public CPAThreadModularAlgorithm newInstance() {
      return CPAThreadModularAlgorithm.create((ConfigurableProgramAnalysisTM) cpa, logger, shutdownNotifier);
    }
  }

  private final CompatibilityCheck compatibleCheck;
  private final StopOperator stopForInferenceObject;
  private final MergeOperator mergeForInferenceObject;

  private final CPAThreadModularStatistics stats = new CPAThreadModularStatistics();


  protected CPAThreadModularAlgorithm(ConfigurableProgramAnalysisTM pCpa,
      LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      ForcedCovering pForcedCovering, boolean pIsImprecise) {
    super(pCpa, pLogger, pShutdownNotifier, pForcedCovering, pIsImprecise);
    compatibleCheck = pCpa.getCompatibilityCheck();
    stopForInferenceObject = pCpa.getStopForInferenceObject();
    mergeForInferenceObject = pCpa.getMergeForInferenceObject();
  }

  public static CPAThreadModularAlgorithm create(ConfigurableProgramAnalysisTM pCpa,
      LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    return new CPAThreadModularAlgorithm(pCpa, pLogger, pShutdownNotifier, null, false);
  }

  @Override
  protected void frontier(ReachedSet pReached, AbstractState pSuccessor, Precision pPrecision) {
    if (pSuccessor instanceof InferenceObject) {
      Collection<AbstractState> states = ((ThreadModularReachedSet) pReached).getStates();

      stats.statesAdd.start();
      for (AbstractState state : states) {
        if (compatibleCheck.compatible(state, (InferenceObject) pSuccessor)) {
          stats.compatiblePairs++;
          Precision itsPrecision = pReached.getPrecision(state);
          pReached.reAddToWaitlist(new ThreadModularWaitlistElement(state, (InferenceObject) pSuccessor, itsPrecision));
        } else {
          stats.incompatiblePairs++;
        }
      }
      stats.statesAdd.stop();
    } else {
      Collection<InferenceObject> objects = ((ThreadModularReachedSet) pReached).getInferenceObjects();

      stats.inferenceObjectsAdd.start();
      for (InferenceObject object : objects) {
        if (compatibleCheck.compatible(pSuccessor, object)) {
          stats.compatiblePairs++;
          pReached.reAddToWaitlist(new ThreadModularWaitlistElement(pSuccessor, object, pPrecision));
        } else {
          stats.incompatiblePairs++;
        }
      }
      stats.inferenceObjectsAdd.stop();
    }
    pReached.add(pSuccessor, pPrecision);
  }

  @Override
  protected void update(
      ReachedSet pReachedSet,
      List<AbstractState> pToRemove,
      List<Pair<AbstractState, Precision>> pToAdd) {
    Preconditions.checkArgument(pToRemove.size() == pToAdd.size());

    for (int i = 0; i < pToRemove.size(); i++) {
      AbstractState toRemove = pToRemove.get(i);
      Pair<AbstractState, Precision> pair = pToAdd.get(i);

      pReachedSet.remove(toRemove);
      frontier(pReachedSet, pair.getFirst(), pair.getSecond());

    }
  }

  @Override
  protected Collection<Pair<? extends AbstractState, ? extends Precision>> getAbstractSuccessors(
      WaitlistElement pElement, ReachedSet rset) throws CPATransferException, InterruptedException {

    Preconditions.checkArgument(pElement instanceof ThreadModularWaitlistElement);

    AbstractState state = ((ThreadModularWaitlistElement) pElement).getState();
    Precision precision = ((ThreadModularWaitlistElement) pElement).getPrecision();
    InferenceObject object = ((ThreadModularWaitlistElement) pElement).getInferenceObject();

    Collection<Pair<AbstractState, InferenceObject>> successors;
    successors = ((TransferRelationTM) transferRelation).getAbstractSuccessors(state, object, rset, precision);

    Collection<Pair<? extends AbstractState, ? extends Precision>> result = new ArrayList<>();
    for (Pair<AbstractState, InferenceObject> tmpPair : successors) {
      result.add(Pair.of(tmpPair.getFirst(), precision));
      InferenceObject newObject = tmpPair.getSecond();
      if (object != null && newObject != EmptyInferenceObject.getInstance()) {
        result.add(Pair.of(newObject, precision));
      }
      if (object != null && object != EmptyInferenceObject.getInstance()
          && object != TauInferenceObject.getInstance()) {
        stats.newStatesForInferenceCount++;
      }
    }
    return result;
  }

  @Override
  protected boolean stop(AbstractState pSuccessor, Collection<AbstractState> pReached, Precision pSuccessorPrecision) throws CPAException, InterruptedException {
    boolean result;
    if (pSuccessor instanceof InferenceObject) {
      result = stopForInferenceObject.stop(pSuccessor, pReached, pSuccessorPrecision);
      if (result) {
        stats.inferenceObjectsStop++;
      } else {
        stats.inferenceObjectsPass++;
      }
    } else {
      result = stopOperator.stop(pSuccessor, pReached, pSuccessorPrecision);
      if (result) {
        stats.statesStop++;
      } else {
        stats.statesPass++;
      }
    }
    return result;
  }

  @Override
  protected AbstractState merge(AbstractState pSuccessor, AbstractState pReachedState, Precision pSuccessorPrecision) throws CPAException, InterruptedException {
    AbstractState result;
    if (pSuccessor instanceof InferenceObject) {
      result = mergeForInferenceObject.merge(pSuccessor, pReachedState, pSuccessorPrecision);
      if (result != pReachedState) {
        stats.inferenceObjectsMerge++;
      }
    } else {
      result = mergeOperator.merge(pSuccessor, pReachedState, pSuccessorPrecision);
      if (result != pReachedState) {
        stats.statesMerge++;
      }
    }
    return result;
  }


  @Override
  protected boolean mergeIsNotSep(AbstractState pState) {
    if (pState instanceof InferenceObject) {
      return mergeForInferenceObject != MergeSepOperator.getInstance();
    } else {
      return mergeOperator != MergeSepOperator.getInstance();
    }
  }

  @Override
  public void collectStatistics(
      Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
  }
}
