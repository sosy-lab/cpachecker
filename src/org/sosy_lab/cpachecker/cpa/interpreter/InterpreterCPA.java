/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interpreter;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class InterpreterCPA implements ConfigurableProgramAnalysis {

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;
  private int[] mInitialValuesForNondeterministicAssignments;

  public InterpreterCPA(int[] pInitialValuesForNondeterministicAssignments) {
    this(pInitialValuesForNondeterministicAssignments, false);
  }

  public InterpreterCPA(int[] pInitialValuesForNondeterministicAssignments, boolean pExtendInputs) {
    if (pInitialValuesForNondeterministicAssignments == null) {
      throw new IllegalArgumentException();
    }

    InterpreterDomain lDomain = new InterpreterDomain ();
    MergeOperator lMergeOp = MergeSepOperator.getInstance();
    StopOperator lStopOp = StopNeverOperator.getInstance();

    TransferRelation lTransferRelation;

    if (pExtendInputs) {
      lTransferRelation = new InterpreterInputExtendingTransferRelation();
    }
    else {
      lTransferRelation = new InterpreterTransferRelation();
    }

    this.abstractDomain = lDomain;
    this.mergeOperator = lMergeOp;
    this.stopOperator = lStopOp;
    this.transferRelation = lTransferRelation;
    this.precisionAdjustment = StaticPrecisionAdjustment.getInstance();

    this.mInitialValuesForNondeterministicAssignments = pInitialValuesForNondeterministicAssignments;
  }

  @Override
  public AbstractDomain getAbstractDomain ()
  {
    return abstractDomain;
  }

  @Override
  public MergeOperator getMergeOperator ()
  {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator ()
  {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation ()
  {
    return transferRelation;
  }

  @Override
  public AbstractElement getInitialElement (CFANode node)
  {
    return new InterpreterElement(mInitialValuesForNondeterministicAssignments);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

}
