/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory.SpecAutomatonCompositionType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.List;

import javax.annotation.Nullable;

public class AnalysisFactory {

  private final CFA cfa;
  private final Configuration config;
  private final LogManager logger;
  private final String programDenotation;
  private final MainCPAStatistics mainStats;

  public static class Analysis {

    private final Algorithm algorithm;
    private final ConfigurableProgramAnalysis cpa;
    private final ShutdownNotifier notifier;
    private final ReachedSet reached;

    public Analysis(ShutdownNotifier pNotifier, ConfigurableProgramAnalysis pCpa,
        Algorithm pAlgorithm, ReachedSet pReached) {
      notifier = pNotifier;
      cpa = pCpa;
      algorithm = pAlgorithm;
      reached = pReached;
    }

    public Algorithm getAlgorithm() {
      return algorithm;
    }

    public ConfigurableProgramAnalysis getCpa() {
      return cpa;
    }

    public ShutdownNotifier getNotifier() {
      return notifier;
    }

    public ReachedSet getReached() {
      return reached;
    }
  }

  public AnalysisFactory(Configuration pConfig, LogManager pLogger, CFA pCfa,
      String pProgramDenotation, MainCPAStatistics pStats) {
    programDenotation = pProgramDenotation;
    config = pConfig;
    logger = pLogger;
    cfa = pCfa;
    mainStats = pStats;
  }

  public Analysis createFreshAnalysis(ShutdownManager pShutdownManager)
      throws InvalidConfigurationException, CPAException {
    return createFreshAnalysis(pShutdownManager, null);
  }

  public Analysis createFreshAnalysis(ShutdownManager pShutdownManager,
      @Nullable List<Automaton> pSpecificationAutomata)
    throws InvalidConfigurationException, CPAException {

    SpecAutomatonCompositionType speComposition = SpecAutomatonCompositionType.TARGET_SPEC;
    CoreComponentsFactory factory = new CoreComponentsFactory(config, logger, pShutdownManager.getNotifier());

    ConfigurableProgramAnalysis cpa = factory.createCPA(cfa, speComposition, pSpecificationAutomata);
    if (config.getProperty("analysis.algorithm.tiger").equals("true")) {
      GlobalInfo.getInstance().setUpInfoFromCPA(cpa, config, logger, pShutdownManager.getNotifier(), cfa);
    } else {
      GlobalInfo.getInstance().setUpInfoFromCPA(cpa);
    }

    Algorithm algorithm = factory.createAlgorithm(cpa, programDenotation, cfa, mainStats, true);

    ReachedSet reached = factory.createReachedSet();

    return new Analysis(pShutdownManager.getNotifier(), cpa, algorithm, reached);
  }
}
