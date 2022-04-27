// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.congruence;

import java.util.Collection;
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

/** A very simple congruence analysis. */
@Options(prefix = "cpa.congruence")
public class CongruenceCPA
    implements ConfigurableProgramAnalysis, StatisticsProvider, AutoCloseable {

  @Option(secure = true, description = "Cache formulas produced by path formula manager")
  private boolean useCachingPathFormulaManager = true;

  private final CongruenceStatistics statistics;
  private final ABECPA<CongruenceState, TemplatePrecision> abeCPA;
  private final Solver solver;

  public CongruenceCPA(
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCFA)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    solver = Solver.create(pConfiguration, pLogger, pShutdownNotifier);

    FormulaManagerView formulaManager = solver.getFormulaManager();
    PathFormulaManager pathFormulaManager =
        new PathFormulaManagerImpl(
            formulaManager,
            pConfiguration,
            pLogger,
            pShutdownNotifier,
            pCFA,
            AnalysisDirection.FORWARD);

    if (useCachingPathFormulaManager) {
      pathFormulaManager = new CachingPathFormulaManager(pathFormulaManager);
    }
    TemplateToFormulaConversionManager templateToFormulaConversionManager =
        new TemplateToFormulaConversionManager(pCFA, pLogger);
    statistics = new CongruenceStatistics();
    CongruenceManager congruenceManager =
        new CongruenceManager(
            pConfiguration,
            solver,
            templateToFormulaConversionManager,
            formulaManager,
            statistics,
            pathFormulaManager,
            pLogger,
            pCFA,
            pShutdownNotifier);
    abeCPA =
        new ABECPA<>(pConfiguration, pLogger, pShutdownNotifier, pCFA, congruenceManager, solver);
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
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return abeCPA.getInitialState(node, partition);
  }

  @Override
  public Precision getInitialPrecision(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
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
