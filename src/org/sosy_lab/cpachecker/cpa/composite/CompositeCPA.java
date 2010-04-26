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
package org.sosy_lab.cpachecker.cpa.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.CallElement;
import org.sosy_lab.cpachecker.core.CallStack;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class CompositeCPA implements ConfigurableProgramAnalysis, StatisticsProvider, WrapperCPA
{
  private static class CompositeCPAFactory extends AbstractCPAFactory {

    private ImmutableList<ConfigurableProgramAnalysis> cpas = null;
    
    @Override
    public ConfigurableProgramAnalysis createInstance() {
      Preconditions.checkState(cpas != null, "CompositeCPA needs wrapped CPAs!");
      
      ImmutableList.Builder<AbstractDomain> domains = ImmutableList.builder();
      ImmutableList.Builder<TransferRelation> transferRelations = ImmutableList.builder();
      ImmutableList.Builder<MergeOperator> mergeOperators = ImmutableList.builder();
      ImmutableList.Builder<StopOperator> stopOperators = ImmutableList.builder();
      ImmutableList.Builder<PrecisionAdjustment> precisionAdjustments = ImmutableList.builder();
      
      for (ConfigurableProgramAnalysis sp : cpas) {
        domains.add(sp.getAbstractDomain());
        transferRelations.add(sp.getTransferRelation());
        mergeOperators.add(sp.getMergeOperator());
        stopOperators.add(sp.getStopOperator());
        precisionAdjustments.add(sp.getPrecisionAdjustment());
      }
      
      CompositeDomain compositeDomain = new CompositeDomain(domains.build());
      CompositeTransferRelation compositeTransfer = new CompositeTransferRelation(transferRelations.build());
      CompositeMergeOperator compositeMerge = new CompositeMergeOperator(mergeOperators.build());
      CompositeStopOperator compositeStop = new CompositeStopOperator(compositeDomain, stopOperators.build());
      CompositePrecisionAdjustment compositePrecisionAdjustment = new CompositePrecisionAdjustment(precisionAdjustments.build());
      
      return new CompositeCPA(compositeDomain, compositeTransfer, compositeMerge, compositeStop,
          compositePrecisionAdjustment, cpas);
    }

    @Override
    public CPAFactory setChild(ConfigurableProgramAnalysis pChild)
        throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Use CompositeCPA to wrap several CPAs!");
    }

    @Override
    public CPAFactory setChildren(List<ConfigurableProgramAnalysis> pChildren) {
      Preconditions.checkNotNull(pChildren);
      Preconditions.checkArgument(!pChildren.isEmpty());
      Preconditions.checkState(cpas == null);
      
      cpas = ImmutableList.copyOf(pChildren);
      return this;
    }
  }
  
  public static CPAFactory factory() {
    return new CompositeCPAFactory();
  }
  
  private final AbstractDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;

  private final ImmutableList<ConfigurableProgramAnalysis> cpas;
  
  private CompositeCPA (AbstractDomain abstractDomain,
      TransferRelation transferRelation,
      MergeOperator mergeOperator,
      StopOperator stopOperator,
      PrecisionAdjustment precisionAdjustment,
      ImmutableList<ConfigurableProgramAnalysis> cpas)
  {
    this.abstractDomain = abstractDomain;
    this.transferRelation = transferRelation;
    this.mergeOperator = mergeOperator;
    this.stopOperator = stopOperator;
    this.precisionAdjustment = precisionAdjustment;
    this.cpas = cpas;
  }

  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  public StopOperator getStopOperator() {
    return stopOperator;
  }

  public PrecisionAdjustment getPrecisionAdjustment () {
    return precisionAdjustment;
  }
  @Override
  public AbstractElement getInitialElement (CFAFunctionDefinitionNode node) {
    Preconditions.checkNotNull(node);
    
    ImmutableList.Builder<AbstractElement> initialElements = ImmutableList.builder();
    for (ConfigurableProgramAnalysis sp : cpas) {
      initialElements.add(sp.getInitialElement(node));
    }
    
    CompositeElement initialElement = new CompositeElement(initialElements.build(), null);
    // set call stack
    CallStack initialCallStack = new CallStack();
    CallElement initialCallElement = new CallElement(node.getFunctionName(), node, initialElement);
    initialCallStack.push(initialCallElement);
    initialElement.setCallStack(initialCallStack);
    
    return initialElement;
  }

  public Precision getInitialPrecision (CFAFunctionDefinitionNode node) {
    Preconditions.checkNotNull(node);
    
    List<Precision> initialPrecisions = new ArrayList<Precision>(cpas.size());
    for (ConfigurableProgramAnalysis sp : cpas) {
      initialPrecisions.add(sp.getInitialPrecision(node));
    }
    return new CompositePrecision(initialPrecisions);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    for (ConfigurableProgramAnalysis cpa: cpas) {
      if (cpa instanceof StatisticsProvider) {
        ((StatisticsProvider)cpa).collectStatistics(pStatsCollection);
      }
    }
  }
  
  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }
    for (ConfigurableProgramAnalysis cpa : cpas) {
      if (pType.isAssignableFrom(cpa.getClass())) {
        return pType.cast(cpa);
      } else if (cpa instanceof WrapperCPA) {
        T result = ((WrapperCPA)cpa).retrieveWrappedCpa(pType);
        if (result != null) {
          return result;
        }
      }  
    }
    return null;
  }
}