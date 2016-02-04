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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.InfinitePropertyBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.PropertyBudgeting;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory.OptionalAnnotation;
import org.sosy_lab.cpachecker.core.defaults.BreakOnTargetsPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.globalinfo.AutomatonInfo;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;

/**
 * This class implements an AutomatonAnalysis as described in the related Documentation.
 */
public class ControlAutomatonCPA implements ConfigurableProgramAnalysis, StatisticsProvider, ConfigurableProgramAnalysisWithBAM, ProofChecker {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ControlAutomatonCPA.class);
  }

  @Options(prefix="cpa.automaton")
  static class ControlAutomatonOptions {

    @Option(secure=true, name="dotExport",
        description="export automaton to file")
    boolean export = false;

    @Option(secure=true, name="dotExportFile",
        description="file for saving the automaton in DOT format (%s will be replaced with automaton name)")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    PathTemplate exportFile = PathTemplate.ofFormatString("%s.dot");

    @Option(secure=true, required=false,
        description="file with automaton specification for ObserverAutomatonCPA and ControlAutomatonCPA")
    @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
    Path inputFile = null;

    @Option(secure=true, description="signal the analysis to break in case the given number of error state is reached ")
    int breakOnTargetState = 1;

    @Option(secure=true, description="the maximum number of iterations performed after the initial error is found, despite the limit"
        + "given as cpa.automaton.breakOnTargetState is not yet reached")
    int extraIterationsLimit = -1;

    @Option(secure=true, description="Whether to treat automaton states with an internal error state as targets. This should be the standard use case.")
    boolean treatErrorsAsTargets = true;

    @Option(secure=true, description = "Stop in the automata transfer relation if the analysis identified one feasible path for each target state.")
    boolean stopAfterOneFeasiblePathPerProperty = false;

    @Option(secure=true, description = "Split to a state 'INACTIVE' when reaching a target state.")
    boolean splitOnTargetStatesToInactive = false;

    public ControlAutomatonOptions(Configuration pConfig) throws InvalidConfigurationException {
      Preconditions.checkNotNull(pConfig);
      pConfig.inject(this);
    }
  }

  private final ControlAutomatonOptions options;

  private final Automaton automaton;
  private final AutomatonState topState;
  private final AutomatonState bottomState;
  private final AutomatonState inactiveState;
  private final AbstractDomain automatonDomain;
  private final AutomatonPrecision initPrecision = AutomatonPrecision.emptyBlacklist();

  private final StopOperator stopOperator;
  private final MergeOperator mergeOperator;

  private final AutomatonTransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;

  private final Statistics stats = new AutomatonStatistics(this);

  @Nonnull private PropertyBudgeting budgeting;

  private final CFA cfa;
  private final LogManager logger;

  protected ControlAutomatonCPA(@OptionalAnnotation Automaton pAutomaton,
      Configuration pConfig, LogManager pLogger, CFA pCFA)
    throws InvalidConfigurationException {

    this.options = new ControlAutomatonOptions(pConfig);
    this.cfa = pCFA;
    this.logger = pLogger;

    this.topState = new AutomatonState.TOP(this);
    this.bottomState = new AutomatonState.BOTTOM(this);
    this.inactiveState = new AutomatonState.INACTIVE(this);
    this.automatonDomain = new AutomatonDomain(topState, inactiveState);
    this.stopOperator = new AutomatonStopOperator(automatonDomain);

    budgeting = InfinitePropertyBudgeting.INSTANCE;

    this.transferRelation = new AutomatonTransferRelation(this, pLogger, inactiveState, options);
    this.precisionAdjustment = composePrecisionAdjustmentOp(pConfig);
    this.mergeOperator = new AutomatonMergeOperator(pConfig, this, automatonDomain, topState);

    if (pAutomaton != null) {
      this.automaton = pAutomaton;

    } else if (options.inputFile == null) {
      throw new InvalidConfigurationException("Explicitly specified automaton CPA needs option cpa.automaton.inputFile!");

    } else {
      this.automaton = constructAutomataFromFile(pConfig, options.inputFile);
    }

    pLogger.log(Level.FINEST, "Automaton", automaton.getName(), "loaded.");

    if (options.export && options.exportFile != null) {
      try (Writer w = Files.openOutputFile(options.exportFile.getPath(automaton.getName()))) {
        automaton.writeDotFile(w);
      } catch (IOException e) {
        pLogger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
      }
    }
  }

  private Automaton constructAutomataFromFile(Configuration pConfig, Path pFile)
      throws InvalidConfigurationException {

    Scope scope = cfa.getLanguage() == Language.C
        ? new CProgramScope(cfa, logger)
        : DummyScope.getInstance();

    List<Automaton> lst = AutomatonParser.parseAutomatonFile(pFile, pConfig, logger, cfa.getMachineModel(), scope, cfa.getLanguage());

    if (lst.isEmpty()) {
      throw new InvalidConfigurationException("Could not find automata in the file " + options.inputFile.toAbsolutePath());
    } else if (lst.size() > 1) {
      throw new InvalidConfigurationException("Found " + lst.size()
          + " automata in the File " + options.inputFile.toAbsolutePath()
          + " The CPA can only handle ONE Automaton!");
    }

    return lst.get(0);
  }

  private PrecisionAdjustment composePrecisionAdjustmentOp(Configuration pConfig)
      throws InvalidConfigurationException {

    Supplier<PropertyBudgeting> supplier = new Supplier<PropertyBudgeting>() {
      @Override
      public PropertyBudgeting get() {
        return budgeting;
      }
    };

    PrecisionAdjustment result = new ControlAutomatonPrecisionAdjustment(pConfig, options,
        supplier, bottomState, inactiveState);

    if (options.breakOnTargetState > 0) {
      final int pFoundTargetLimit = options.breakOnTargetState;
      final int pExtraIterationsLimit = options.extraIterationsLimit;

      result = new BreakOnTargetsPrecisionAdjustment(result, pFoundTargetLimit, pExtraIterationsLimit);
    }

    result = new AdjustAutomatonPrecisionAdjustment(result);

    return result;
  }

  Automaton getAutomaton() {
    return this.automaton;
  }

  public void registerInAutomatonInfo(AutomatonInfo info) {
    info.register(automaton, this);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return automatonDomain;
  }

  @Override
  public AutomatonState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return AutomatonState.automatonStateFactory(automaton.getInitialVariables(), automaton.getInitialState(), this, 0, 0, null);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return initPrecision;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public AutomatonTransferRelation getTransferRelation() {
    return transferRelation ;
  }

  @Override
  public Reducer getReducer() {
    return NoOpReducer.getInstance();
  }

  public AutomatonState getBottomState() {
    return this.bottomState;
  }

  public AutomatonState getTopState() {
    return this.topState;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    return pSuccessors.equals(getTransferRelation().getAbstractSuccessorsForEdge(
        pElement, SingletonPrecision.getInstance(), pCfaEdge));
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement) throws CPAException, InterruptedException {
    return getAbstractDomain().isLessOrEqual(pElement, pOtherElement);
  }

  MachineModel getMachineModel() {
    return cfa.getMachineModel();
  }

  LogManager getLogManager() {
    return logger;
  }

  boolean isTreatingErrorsAsTargets() {
    return options.treatErrorsAsTargets;
  }

  public void setBudgeting(PropertyBudgeting pBudgeting) {
    budgeting = pBudgeting;
  }
}
