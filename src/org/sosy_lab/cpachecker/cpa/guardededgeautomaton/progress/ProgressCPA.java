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
package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.progress;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonDomain;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStandardElement;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;

public class ProgressCPA implements ConfigurableProgramAnalysis {

  private final GuardedEdgeAutomatonDomain mDomain;
  private final GuardedEdgeAutomatonStandardElement mInitialElement;
  private final StopSepOperator mStopOperator;
  private final ProgressTransferRelation mTransferRelation;
  private final ProgressPrecisionAdjustment mPrecisionAdjustment;
  private final ProgressPrecision mInitialPrecision;

  public ProgressCPA(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
    mDomain = GuardedEdgeAutomatonDomain.getInstance();
    mStopOperator = new StopSepOperator(mDomain);
    mTransferRelation = new ProgressTransferRelation(mDomain, pAutomaton);

    NondeterministicFiniteAutomaton.State lInitialState = pAutomaton.getInitialState();
    boolean lIsFinal = pAutomaton.getFinalStates().contains(lInitialState);
    mInitialElement = new GuardedEdgeAutomatonStandardElement(lInitialState, lIsFinal);

    mInitialPrecision = new ProgressPrecision(pAutomaton);

    mPrecisionAdjustment = new ProgressPrecisionAdjustment();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return mTransferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

  @Override
  public AbstractElement getInitialElement(CFANode pNode) {
    return mInitialElement;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return mInitialPrecision;
  }

}
