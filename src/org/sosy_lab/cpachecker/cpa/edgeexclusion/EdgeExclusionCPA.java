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
package org.sosy_lab.cpachecker.cpa.edgeexclusion;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonCPAFactory;
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

/**
 * This CPA can be used to prevent the traversal of specific CFA edges.
 */
public enum EdgeExclusionCPA implements ConfigurableProgramAnalysis {

  /**
   * The singleton instance of this CPA.
   */
  INSTANCE;

  /**
   * The merge operator used.
   */
  private static final MergeOperator MERGE_OPERATOR = new MergeJoinOperator(EdgeExclusionDomain.INSTANCE);

  /**
   * The stop operator used.
   */
  private static final StopOperator STOP_OPERATOR = new StopSepOperator(EdgeExclusionDomain.INSTANCE);

  /**
   * Gets a factory for creating EdgeExclusionCPAs.
   *
   * @return a factory for creating EdgeExclusionCPAs.
   */
  public static CPAFactory factory() {
    return SingletonCPAFactory.forInstance(INSTANCE);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    return EdgeExclusionState.TOP;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return EdgeExclusionPrecision.getEmptyPrecision();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return EdgeExclusionDomain.INSTANCE;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return EdgeExclusionTransferRelation.INSTANCE;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MERGE_OPERATOR;
  }

  @Override
  public StopOperator getStopOperator() {
    return STOP_OPERATOR;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

}
