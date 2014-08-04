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
package org.sosy_lab.cpachecker.cpa.validvars;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

// requires that function names are unique, no two functions may have the same name although they have different signature
// currently ensured by parser
public class ValidVarsCPA extends AbstractSingleWrapperCPA{

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ValidVarsCPA.class);
  }

  private final AbstractDomain domain;
  private final TransferRelation transfer;
  private final MergeOperator merge;
  private final StopOperator stop;
  private final PrecisionAdjustment adjust;

  public ValidVarsCPA(ConfigurableProgramAnalysis pCpa) {
    super(pCpa);

    domain = new ValidVarsDomain(pCpa.getAbstractDomain());
    transfer = new ValidVarsTransferRelation(pCpa.getTransferRelation());

    if (pCpa.getMergeOperator() == MergeSepOperator.getInstance()) {
      merge = MergeSepOperator.getInstance();
    } else {
      merge = new ValidVarsMergeOperator(pCpa.getMergeOperator());
    }

    stop = new StopSepOperator(domain);

    adjust = new ValidVarsPrecisionAdjustment(pCpa.getPrecisionAdjustment());
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return adjust;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    return new ValidVarsState(getWrappedCpa().getInitialState(pNode), ValidVars.initial);
  }



}
