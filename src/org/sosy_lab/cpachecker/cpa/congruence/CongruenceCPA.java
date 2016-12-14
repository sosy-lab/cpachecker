/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.congruence;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.abe.ABECPA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.templates.TemplatePrecision;
import org.sosy_lab.cpachecker.util.templates.TemplateToFormulaConversionManager;

import java.util.Collection;

/**
 * A very simple congruence analysis.
 */
@Options(prefix="cpa.congruence")
public class CongruenceCPA
    implements ConfigurableProgramAnalysis,
               StatisticsProvider,
               AutoCloseable {

  @Option(secure=true,
      description="Cache formulas produced by path formula manager")
  private boolean useCachingPathFormulaManager = true;

  private final CongruenceStatistics statistics;
  private final ABECPA<CongruenceState, TemplatePrecision> abeCPA;
  private final Solver solver;

  public CongruenceCPA(Configuration pConfiguration,
                       LogManager pLogger,
                       ShutdownNotifier pShutdownNotifier,
                       CFA pCFA)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    solver = Solver.create(pConfiguration, pLogger, pShutdownNotifier);

    FormulaManagerView formulaManager = solver.getFormulaManager();
    PathFormulaManager pathFormulaManager = new PathFormulaManagerImpl(
        formulaManager, pConfiguration, pLogger, pShutdownNotifier, pCFA,
        AnalysisDirection.FORWARD);

    if (useCachingPathFormulaManager) {
      pathFormulaManager = new CachingPathFormulaManager(pathFormulaManager);
    }
    TemplateToFormulaConversionManager templateToFormulaConversionManager =
        new TemplateToFormulaConversionManager(pCFA, pLogger);
    statistics = new CongruenceStatistics();
    CongruenceManager congruenceManager = new CongruenceManager(
        pConfiguration, solver, templateToFormulaConversionManager,
        formulaManager, statistics, pathFormulaManager, pLogger, pCFA,
        pShutdownNotifier);
    abeCPA = new ABECPA<>(pConfiguration, pLogger, pShutdownNotifier, pCFA,
        congruenceManager, solver);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(CongruenceCPA.class);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abeCPA.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return abeCPA.getTransferRelation();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return abeCPA.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return abeCPA.getStopOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return abeCPA.getPrecisionAdjustment();
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return abeCPA.getInitialState(node, partition);
  }

  @Override
  public Precision getInitialPrecision(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return abeCPA.getInitialPrecision(node, partition);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }

  @Override
  public void close() {
    solver.close();
  }
}
