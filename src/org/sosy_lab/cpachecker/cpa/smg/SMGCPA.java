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

import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

enum SMGRuntimeCheck {
  FORCED(-1),
  NONE(0),
  HALF(1),
  FULL(2);

  private final int id;
  SMGRuntimeCheck(int pId) { id = pId; }
  public int getValue() { return id; }

  public boolean isFinerOrEqualThan(SMGRuntimeCheck other) {
    return id >= other.id;
  }
}

@Options(prefix="cpa.smg")
public class SMGCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SMGCPA.class);
  }

  @Option(name="runtimeCheck", description = "Sets the level of runtime checking: NONE, HALF, FULL")
  private SMGRuntimeCheck runtimeCheck = SMGRuntimeCheck.NONE;

  @Option(name="memoryErrors", description = "Determines if memory errors are target states")
  private boolean memoryErrors = true;

  @Option(name="unknownOnUndefined", description = "Emit messages when we encounter non-target undefined behavior")
  private boolean unknownOnUndefined = true;

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;

  private final MachineModel machineModel;

  private final LogManager logger;

  private SMGCPA(Configuration config, LogManager pLogger, CFA cfa) throws InvalidConfigurationException {
    config.inject(this);
    machineModel = cfa.getMachineModel();
    logger = pLogger;

    abstractDomain = new SMGDomain();
    mergeOperator = MergeSepOperator.getInstance();
    stopOperator = new StopSepOperator(abstractDomain);
    transferRelation = new SMGTransferRelation(config, logger, machineModel);

    SMGState.setRuntimeCheck(runtimeCheck);

    SMGState.setTargetMemoryErrors(memoryErrors);
    SMGState.setUnknownOnUndefined(unknownOnUndefined);
  }

  public MachineModel getMachineModel() {
    return machineModel;
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
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    SMGState initState = new SMGState(logger, machineModel);

    try {
      initState.performConsistencyCheck(SMGRuntimeCheck.FULL);
    } catch(SMGInconsistentException exc) {
      logger.log(Level.SEVERE, exc.getMessage());
    }

    CFunctionEntryNode functionNode = (CFunctionEntryNode)pNode;
    try {
      initState.addStackFrame(functionNode.getFunctionDefinition());
      initState.performConsistencyCheck(SMGRuntimeCheck.FULL);
    } catch(SMGInconsistentException exc) {
      logger.log(Level.SEVERE, exc.getMessage());
    }

    return initState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }

}
