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
package org.sosy_lab.cpachecker.cpa.constraints;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import java.util.Optional;

/**
 * Factory for {@link FormulaCreator}.
 */
public class FormulaCreatorFactory {

  private final MachineModel machineModel;
  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final FormulaEncodingOptions options;
  private final CtoFormulaTypeHandler typeHandler;

  public FormulaCreatorFactory(
      final MachineModel pMachineModel,
      final LogManager pLogger,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier
  ) throws InvalidConfigurationException {
    options = new FormulaEncodingOptions(pConfig);
    typeHandler = new CtoFormulaTypeHandler(pLogger, pMachineModel);

    machineModel = pMachineModel;
    logger = pLogger;
    config = pConfig;
    shutdownNotifier = pShutdownNotifier;
  }

  public FormulaCreator create(final FormulaManagerView pManager, final String pFunctionName) {
    CtoFormulaConverter toFormulaTransformer = new CtoFormulaConverter(
        options,
        pManager,
        machineModel,
        Optional.empty(),
        logger,
        shutdownNotifier,
        typeHandler,
        AnalysisDirection.FORWARD);
    return new FormulaCreatorUsingCConverter(pManager, toFormulaTransformer, pFunctionName);
  }
}
