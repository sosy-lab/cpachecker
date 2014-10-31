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
package org.sosy_lab.cpachecker.cpa.predicate.synthesis;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;

import com.google.common.base.Optional;


public abstract class AbstractPrecisionSynthesis implements AbstractionInstanceSynthesis {

  protected final LogManager logger;
  protected final FormulaManager rawFmgr;
  protected final AbstractionManager amgr;
  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bfmgr;
  protected final CFA cfa;
  protected final CtoFormulaConverter converter;
  protected final RelationView relview;

  public AbstractPrecisionSynthesis(Configuration pConfig, LogManager pLogger,
      FormulaManagerView pFmgr, Optional<VariableClassification> pVariableClassification,
      FormulaManager pRawFmgr, AbstractionManager pAmgr,
      MachineModel pMachineModel, ShutdownNotifier pShutdownNotifier, CFA pCfa, RelationView pRelView,
      AnalysisDirection pDirection)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = pLogger;
    fmgr = pFmgr;
    rawFmgr = pRawFmgr;
    amgr = pAmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
    cfa = pCfa;
    relview = pRelView;

    FormulaEncodingOptions options = new FormulaEncodingOptions(pConfig);
    CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(pLogger, options, pMachineModel, pFmgr);

    converter = new CtoFormulaConverter(
        options, fmgr,
        pMachineModel, pVariableClassification,
        logger, pShutdownNotifier,
        typeHandler, pDirection);
  }
}
