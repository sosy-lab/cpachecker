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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenStatistics;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;


public abstract class AbstractIterationStrategy implements TestGenIterationStrategy {

  protected final Configuration config;
  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  protected final ReachedSetFactory reachedSetFactory;

  private final IterationModel model;


  protected TestGenStatistics stats;

  public AbstractIterationStrategy(StartupConfig pConfig, IterationModel pModel, ReachedSetFactory pReachedSetFactory, TestGenStatistics pStats) {
    super();
    config = pConfig.getConfig();
    logger = pConfig.getLog();
    shutdownNotifier = pConfig.getShutdownNotifier();
    model = pModel;
    this.reachedSetFactory = pReachedSetFactory;
    stats = pStats;
  }

  @Override
  public void initializeModel(ReachedSet pReachedSet) {
    getModel().setGlobalReached(pReachedSet);
    ReachedSet currentReached = reachedSetFactory.create();
    getModel().setLocalReached(currentReached);
    AbstractState initialState = getGlobalReached().getFirstState();
    currentReached.add(initialState, getGlobalReached().getPrecision(initialState));


  }

  @Override
  public boolean runAlgorithm() throws PredicatedAnalysisPropertyViolationException, CPAException, InterruptedException {
    stats.beforeCpaAlgortihm();
    boolean ret = model.getAlgorithm().run(model.getLocalReached());
    stats.afterCpaAlgortihm(model.getAlgorithm());
    updateReached();
    return ret;
  }

  @Override
  public AbstractState getLastState() {
    return model.getLocalReached().getLastState();
  }

  @Override
  public IterationModel getModel() {
    return model;
  }

  protected ReachedSet getGlobalReached(){
    return getModel().getGlobalReached();
  }
  protected ReachedSet getLocalReached(){
    return getModel().getLocalReached();
  }

  protected abstract void updateReached();

}
