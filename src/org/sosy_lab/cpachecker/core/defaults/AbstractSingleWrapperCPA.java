/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package org.sosy_lab.cpachecker.core.defaults;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

/**
 * Base class for CPAs which wrap exactly one other CPA.
 */
public abstract class AbstractSingleWrapperCPA implements ConfigurableProgramAnalysis, WrapperCPA, StatisticsProvider {

  protected abstract static class AbstractSingleWrapperCPAFactory extends AbstractCPAFactory {

    private ConfigurableProgramAnalysis child = null;
    
    public ConfigurableProgramAnalysis getChild() {
      Preconditions.checkState(child != null, "Child CPA object needed to create CPA!");

      return child;
    }

    @Override
    public CPAFactory setChild(ConfigurableProgramAnalysis pChild) {
      Preconditions.checkNotNull(pChild);
      Preconditions.checkState(child == null, "setChild called twice on CPAFactory");
      
      child = pChild;
      return this;
    }
  }
  
  private final ConfigurableProgramAnalysis wrappedCpa;
  
  public AbstractSingleWrapperCPA(ConfigurableProgramAnalysis pCpa) {
    Preconditions.checkNotNull(pCpa);
    
    wrappedCpa = pCpa;
  }
  
  public ConfigurableProgramAnalysis getWrappedCpa() {
    return wrappedCpa;
  }
  
  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return wrappedCpa.getInitialPrecision(pNode);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (wrappedCpa instanceof StatisticsProvider) {
      ((StatisticsProvider)wrappedCpa).collectStatistics(pStatsCollection);
    }
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(wrappedCpa.getClass())) {
      return pType.cast(wrappedCpa);
    } else if (wrappedCpa instanceof WrapperCPA) {
      return ((WrapperCPA)wrappedCpa).retrieveWrappedCpa(pType);
    } else {
      return null;
    }
  }
}