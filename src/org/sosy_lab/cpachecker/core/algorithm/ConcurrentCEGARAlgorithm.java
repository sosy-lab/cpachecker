/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix="rely-guarantee cegar")
public class ConcurrentCEGARAlgorithm implements ConcurrentAlgorithm,  StatisticsProvider {

  private ConcurrentAlgorithm algorithm;
  private Refiner refiner;
  private Configuration config;
  private LogManager logger;

  public ConcurrentCEGARAlgorithm(ConcurrentAlgorithm pAlgorithm, Refiner pRefiner, Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException, CPAException {
    this.algorithm = pAlgorithm;
    this.refiner = pRefiner;
    this.config = pConfig;
    this.logger = pLogger;


  }

  public ConcurrentCEGARAlgorithm(ConcurrentAlgorithm pAlgorithm, Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException, CPAException {
    this.algorithm = pAlgorithm;
    this.config = pConfig;
    this.logger = pLogger;
  }



  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    // TODO Auto-generated method stub

  }

  @Override
  public ConfigurableProgramAnalysis[] getCPAs() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean run(ReachedSet[] pReachedSets) throws CPAException,
      InterruptedException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Result getResult() {
    // TODO Auto-generated method stub
    return null;
  }



}