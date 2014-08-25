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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;

import com.google.common.base.Optional;

@Options(prefix="cpa.predicate")
public class PathFormulaBackwardsManagerImpl extends PathFormulaManagerImpl {

  public PathFormulaBackwardsManagerImpl(FormulaManagerView pFmgr, Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, MachineModel pMachineModel,
      Optional<VariableClassification> pVariableClassification)
          throws InvalidConfigurationException {

    super(pFmgr, pConfig, pLogger, pShutdownNotifier, pMachineModel, pVariableClassification);
  }

  public PathFormulaBackwardsManagerImpl(FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
          throws InvalidConfigurationException {

    super(pFmgr, config, pLogger, pShutdownNotifier, pCfa.getMachineModel(), pCfa.getVarClassification());
  }


  @Override
  protected CtoFormulaConverter createCtoFormulaConverter(FormulaEncodingOptions pOptions, MachineModel pMachineModel,
      Optional<VariableClassification> pVariableClassification, CtoFormulaTypeHandler pTypeHandler) {

    return super.createCtoFormulaConverter(pOptions, pMachineModel, pVariableClassification, pTypeHandler);
  }

  @Override
  protected CtoFormulaConverter createCToFormulaConverterWithPointerAliasing(
      FormulaEncodingWithPointerAliasingOptions pOptions, MachineModel pMachineModel,
      PointerTargetSetManager pPtsManager, Optional<VariableClassification> pVariableClassification,
      TypeHandlerWithPointerAliasing pAliasingTypeHandler) throws InvalidConfigurationException {

    return super.createCToFormulaConverterWithPointerAliasing(pOptions, pMachineModel, pPtsManager,
        pVariableClassification, pAliasingTypeHandler);
  }

}
