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
package cpa.assumptions.collector.progressobserver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import cfa.objectmodel.CFAFunctionDefinitionNode;

import com.google.common.collect.ImmutableList;

import cpa.common.CPAConfiguration;
import cpa.common.CPAchecker;
import cpa.common.LogManager;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.SingletonPrecision;
import cpa.common.defaults.StopNeverOperator;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;

/**
 * @author g.theoduloz
 *
 */
public class ProgressObserverCPA implements ConfigurableProgramAnalysis {

  private static class ProgressObserverCPAFactory extends AbstractCPAFactory {    
    @Override
    public ConfigurableProgramAnalysis createInstance() {
      return new ProgressObserverCPA(getConfiguration(), getLogger());
    }
  }
  
  public static CPAFactory factory() {
    return new ProgressObserverCPAFactory();
  }
  
  private final ProgressObserverDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;
  private final LogManager logger;
  
  private final ImmutableList<StopHeuristics<? extends StopHeuristicsData>> enabledHeuristics;
  
  /** Return the immutable list of enables heuristics */
  public ImmutableList<StopHeuristics<? extends StopHeuristicsData>> getEnabledHeuristics()
  {
    return enabledHeuristics;
  }
  
  @SuppressWarnings("unchecked")
  private static ImmutableList<StopHeuristics<? extends StopHeuristicsData>> createEnabledHeuristics(CPAConfiguration config, LogManager logger)
  {
    String[] heuristicsNamesArray = CPAchecker.config.getPropertiesArray("assumptions.observer.heuristics");
    ImmutableList.Builder<StopHeuristics<? extends StopHeuristicsData>> builder = ImmutableList.<StopHeuristics<? extends StopHeuristicsData>>builder();
    
    for (int i = 0; i < heuristicsNamesArray.length; i++) {
      String heuristicsName = heuristicsNamesArray[i];
      if (!heuristicsName.contains("."))
        heuristicsName = "cpa.assumptions.collector.progressobserver." + heuristicsName;
      try {
        Class<?> cls = Class.forName(heuristicsName);
        Constructor<?> constructor = cls.getConstructor(CPAConfiguration.class, LogManager.class);
        CPAConfiguration localConfig = new CPAConfiguration(config, heuristicsName);
        Object obj = constructor.newInstance(localConfig, logger);
        
        // Convert object to StopHeuristics
        StopHeuristics<? extends StopHeuristicsData> newHeuristics = (StopHeuristics<? extends StopHeuristicsData>)obj;
        builder.add(newHeuristics);
      } catch (ClassNotFoundException e) {
        logger.logException(Level.WARNING, e, "ClassNotFoundException");
      } catch (SecurityException e) {
        logger.logException(Level.WARNING, e, "SecurityException");
      } catch (IllegalArgumentException e) {
        logger.logException(Level.WARNING, e, "IllegalArgumentException");
      } catch (InstantiationException e) {
        logger.logException(Level.WARNING, e, "InstantiationException");
      } catch (IllegalAccessException e) {
        logger.logException(Level.WARNING, e, "IllegalAccessException");
      } catch (NoSuchMethodException e) {
        logger.logException(Level.WARNING, e, "NoSuchMethodException");
      } catch (InvocationTargetException e) {
        logger.logException(Level.WARNING, e, "InvocationTargetException");
      }
    }
    
    return builder.build();
  }
  
  private ProgressObserverCPA(CPAConfiguration cfg, LogManager mgr)
  {
    // Check if assumption collector is enabled; if not, the analysis will
    // not be sound
    if (!CPAchecker.config.getBooleanValue("analysis.useAssumptionCollector"))
      CPAchecker.logger.log(Level.WARNING, "Analysis may not be sound because ProgressObserverCPA is used without assumption collector");

    logger = mgr;
    
    enabledHeuristics = createEnabledHeuristics(cfg, mgr);
    
    abstractDomain = new ProgressObserverDomain(this);
    mergeOperator = MergeSepOperator.getInstance();
    stopOperator = StopNeverOperator.getInstance();
    transferRelation = new ProgressObserverTransferRelation(this);
    precisionAdjustment = new ProgressObserverPrecisionAdjustment(this);
  }
  
  @Override
  public ProgressObserverDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public ProgressObserverElement getInitialElement(CFAFunctionDefinitionNode node) {
    return ProgressObserverElement.getInitial(this, node);
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
  
  public LogManager getLogger() {
    return logger;
  }

}
