/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.assumptions.collector.genericassumptions;

import org.sosy_lab.cpachecker.util.assumptions.AssumptionSymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.assumptions.MathsatInvariantSymbolicFormulaManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class GenericAssumptionsCPA implements ConfigurableProgramAnalysis {

  private static class GenericAssumptionsCPAFactory extends AbstractCPAFactory {

    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
      return new GenericAssumptionsCPA(getConfiguration(), getLogger());
    }
  }

  public static CPAFactory factory() {
    return new GenericAssumptionsCPAFactory();
  }

  private GenericAssumptionsDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;

  // Symbolic Formula Manager used to represent build invariant formulas
  private final AssumptionSymbolicFormulaManager symbolicFormulaManager;

  private GenericAssumptionsCPA(Configuration config, LogManager logger) throws InvalidConfigurationException
  {
    symbolicFormulaManager = MathsatInvariantSymbolicFormulaManager.createInstance(config, logger);

    abstractDomain = new GenericAssumptionsDomain(this);

    mergeOperator = MergeSepOperator.getInstance();
    stopOperator = StopNeverOperator.getInstance();
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();

    transferRelation = new GenericAssumptionsTransferRelation(this);
  }

  public AssumptionSymbolicFormulaManager getSymbolicFormulaManager()
  {
    return symbolicFormulaManager;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return abstractDomain.getTopElement();
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
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
