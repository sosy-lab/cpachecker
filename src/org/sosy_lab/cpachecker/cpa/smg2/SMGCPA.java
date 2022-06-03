// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.annotations.Unmaintained;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithConcreteCex;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.AdditionalInfoConverter;
import org.sosy_lab.cpachecker.cpa.smg.SMGStatistics;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.smg.exception.SMGInconsistencyException;

@Options(prefix = "cpa.smg2")
// TODO remove unmaintained annotation once all components are implemented
@Unmaintained
public class SMGCPA
    implements ConfigurableProgramAnalysis,
        ConfigurableProgramAnalysisWithConcreteCex,
        ConfigurableProgramAnalysisWithAdditionalInfo,
        StatisticsProvider {

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"SEP", "NEVER", "END_BLOCK"},
      description = "which stop operator to use for the SMGCPA")
  private String stopType = "SEP";

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = {"SEP", "JOIN"},
      description = "which merge operator to use for the SMGCPA")
  private String mergeType = "SEP";

  private final MachineModel machineModel;
  private final BlockOperator blockOperator;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final CFA cfa;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final SMGOptions options;

  private final SMGStatistics stats = new SMGStatistics();

  private SMGCPA(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    options = new SMGOptions(pConfig);

    config = pConfig;
    cfa = pCfa;
    machineModel = cfa.getMachineModel();
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    assumptionToEdgeAllocator = AssumptionToEdgeAllocator.create(config, logger, machineModel);

    blockOperator = new BlockOperator();
    pConfig.inject(blockOperator);
    blockOperator.setCFA(cfa);
  }

  public static CPAFactory factory() {
    return null;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public AdditionalInfoConverter exportAdditionalInfoConverter() {
    return new SMGAdditionalInfoConverter();
  }

  @Override
  public CFAPathWithAdditionalInfo createExtendedInfo(ARGPath pPath) {
    return new AdditionalInfoExtractor().createExtendedInfo(pPath);
  }

  @Override
  public ConcreteStatePath createConcreteStatePath(ARGPath pPath) {
    return new SMGConcreteErrorPathAllocator(assumptionToEdgeAllocator)
        .allocateAssignmentsToPath(pPath);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return DelegateAbstractDomain.<SMGState>getInstance();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new SMGTransferRelation(logger, options, machineModel, shutdownNotifier);
  }

  @Override
  public MergeOperator getMergeOperator() {
    switch (mergeType) {
      case "SEP":
        return MergeSepOperator.getInstance();
      case "JOIN":
        return new MergeJoinOperator(getAbstractDomain());
      default:
        throw new AssertionError("unknown mergetype for SMGCPA");
    }
  }

  @Override
  public StopOperator getStopOperator() {
    switch (stopType) {
        // TODO END_BLOCK
      case "NEVER":
        return StopNeverOperator.getInstance();
      case "SEP":
        return new StopSepOperator(getAbstractDomain());
      default:
        throw new AssertionError("unknown stoptype for SMGCPA");
    }
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    SMGState initState = SMGState.of(machineModel, logger, options);

    try {
      // initState.performConsistencyCheck(SMGRuntimeCheck.FULL);
    } catch (SMGInconsistencyException exc) {
      throw new AssertionError(exc);
    }

    if (pNode instanceof CFunctionEntryNode) {
      CFunctionEntryNode functionNode = (CFunctionEntryNode) pNode;
      try {
        initState = initState.opyAndAddStackFrame(functionNode.getFunctionDefinition());
        // initState.performConsistencyCheck(SMGRuntimeCheck.FULL);
      } catch (SMGInconsistencyException exc) {
        throw new AssertionError(exc);
      }
    }

    return initState;
  }
}
