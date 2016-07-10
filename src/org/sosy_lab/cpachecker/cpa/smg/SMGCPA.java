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
package org.sosy_lab.cpachecker.cpa.smg;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithConcreteCex;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGPrecision;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

import java.util.logging.Level;

@Options(prefix="cpa.smg")
public class SMGCPA implements ConfigurableProgramAnalysis, ConfigurableProgramAnalysisWithConcreteCex {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SMGCPA.class);
  }

  @Option(secure=true, name = "exportSMG.file", description = "Filename format for SMG graph dumps")
  @FileOption(Type.OUTPUT_FILE)
  private PathTemplate exportSMGFilePattern = PathTemplate.ofFormatString("smg/smg-%s.dot");

  @Option(secure=true, toUppercase=true, name = "exportSMGwhen", description = "Describes when SMG graphs should be dumped.")
  private SMGExportLevel exportSMG = SMGExportLevel.NEVER;

  public static enum SMGExportLevel {NEVER, LEAF, INTERESTING, EVERY}

  @Option(secure = true, description = "with this option enabled, heap abstraction will be enabled.")
  private boolean enableHeapAbstraction = false;

  @Option(secure=true, name="runtimeCheck", description = "Sets the level of runtime checking: NONE, HALF, FULL")
  private SMGRuntimeCheck runtimeCheck = SMGRuntimeCheck.NONE;

  @Option(secure=true, name="memoryErrors", description = "Determines if memory errors are target states")
  private boolean memoryErrors = true;

  @Option(secure=true, name="unknownOnUndefined", description = "Emit messages when we encounter non-target undefined behavior")
  private boolean unknownOnUndefined = true;

  @Option(secure=true, name="stop", toUppercase=true, values={"SEP", "NEVER", "END_BLOCK"},
      description="which stop operator to use for the SMGCPA")
  private String stopType = "SEP";

  @Option(secure = true, name = "externalAllocationSize", description = "Default size of externally allocated memory")
  private int externalAllocationSize = Integer.MAX_VALUE;

  @Option(secure = true, name = "trackPredicates", description = "Enable track predicates on SMG state")
  private boolean trackPredicates = false;

  public int getExternalAllocationSize() {
    return externalAllocationSize;
  }

  public boolean getTrackPredicates() {
    return trackPredicates;
  }

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;

  private final SMGPredicateManager smgPredicateManager;
  private final BlockOperator blockOperator;
  private final SMGPrecisionAdjustment precisionAdjustment;

  private final MachineModel machineModel;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final CFA cfa;

  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final SMGExportDotOption exportOptions;

  private SMGPrecision precision;

  private SMGCPA(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      CFA pCfa) throws InvalidConfigurationException {
    config = pConfig;
    config.inject(this);
    cfa = pCfa;
    machineModel = cfa.getMachineModel();
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    exportOptions = new SMGExportDotOption(exportSMGFilePattern, exportSMG);

    assumptionToEdgeAllocator = new AssumptionToEdgeAllocator(config, logger, machineModel);

    smgPredicateManager = new SMGPredicateManager(config, logger, pShutdownNotifier);
    blockOperator = new BlockOperator();
    pConfig.inject(blockOperator);
    blockOperator.setCFA(cfa);
    precisionAdjustment = new SMGPrecisionAdjustment(logger, exportOptions);

    abstractDomain = DelegateAbstractDomain.<SMGState> getInstance();
    mergeOperator = MergeSepOperator.getInstance();

    if(stopType.equals("END_BLOCK")) {
      stopOperator = new SMGStopOperator(abstractDomain);
    } else if (stopType.equals("NEVER")) {
      stopOperator = new StopNeverOperator();
    } else {
      stopOperator = new StopSepOperator(abstractDomain);
    }

    precision = initializePrecision();

    transferRelation =
        SMGTransferRelation.createTransferRelation(config, logger, machineModel,
            exportOptions, smgPredicateManager, blockOperator);
  }

  public void setTransferRelationToRefinment(PathTemplate pNewPathTemplate) {
    ((SMGTransferRelation) transferRelation).changeKindToRefinment();
    exportOptions.changeToRefinment(pNewPathTemplate);
  }

  public void injectRefinablePrecision() {
    // replace the full precision with an empty, refinable precision
    precision = SMGPrecision.createRefineablePrecision(precision);
  }

  public MachineModel getMachineModel() {
    return machineModel;
  }

  public SMGExportLevel getExportSMGLevel() {
    return exportSMG;
  }

  public PathTemplate getExportSMGFilePattern() {
    return exportSMGFilePattern;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  public SMGState getInitialState(CFANode pNode) {
    SMGState initState = new SMGState(logger, machineModel, memoryErrors, unknownOnUndefined,
        runtimeCheck, externalAllocationSize, trackPredicates, enableHeapAbstraction);

    try {
      initState.performConsistencyCheck(SMGRuntimeCheck.FULL);
    } catch (SMGInconsistentException exc) {
      logger.log(Level.SEVERE, exc.getMessage());
    }

    if (pNode instanceof CFunctionEntryNode) {
      CFunctionEntryNode functionNode = (CFunctionEntryNode) pNode;
      try {
        initState.addStackFrame(functionNode.getFunctionDefinition());
        initState.performConsistencyCheck(SMGRuntimeCheck.FULL);
      } catch (SMGInconsistentException exc) {
        logger.log(Level.SEVERE, exc.getMessage());
      }
    }

    return initState;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return getInitialState(pNode);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }

  private SMGPrecision initializePrecision() {
    return SMGPrecision.createStaticPrecision(enableHeapAbstraction, logger, blockOperator);
  }

  @Override
  public ConcreteStatePath createConcreteStatePath(ARGPath pPath) {

    return new SMGConcreteErrorPathAllocator(assumptionToEdgeAllocator).allocateAssignmentsToPath(pPath);
  }

  public LogManager getLogger() {
    return logger;
  }

  public Configuration getConfiguration() {
    return config;
  }

  public CFA getCFA() {
    return cfa;
  }

  public SMGPrecision getPrecision() {
    return precision;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }

  public SMGPredicateManager getPredicateManager() {
    return smgPredicateManager;
  }

  public boolean isHeapAbstractionEnabled() {
    return enableHeapAbstraction;
  }

  public BlockOperator getBlockOperator() {
    return blockOperator;
  }

  public void nextRefinment() {
    exportOptions.nextRefinment();
  }
}