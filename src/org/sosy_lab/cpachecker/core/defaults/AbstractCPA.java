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
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * This is an abstract class for building CPAs. It uses the flat lattice domain
 * if no other domain is given, and the standard implementations for merge-(sep|join)
 * and stop-sep.
 */
public abstract class AbstractCPA implements ConfigurableProgramAnalysis {

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;

  protected AbstractCPA(String mergeType, String stopType, TransferRelation transfer) {
    this(mergeType, stopType, new FlatLatticeDomain(), transfer);
  }

  protected AbstractCPA(String mergeType, String stopType, AbstractDomain domain, TransferRelation transfer) {
    this.abstractDomain = domain;

    if (mergeType.equalsIgnoreCase("join")) {
      mergeOperator = new MergeJoinOperator(abstractDomain);
    } else {
      assert mergeType.equalsIgnoreCase("sep");
      mergeOperator = MergeSepOperator.getInstance();
    }

    assert stopType.equalsIgnoreCase("sep");
    stopOperator = new StopSepOperator(abstractDomain);

    this.transferRelation = transfer;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }
}
