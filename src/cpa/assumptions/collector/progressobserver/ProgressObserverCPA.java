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
import common.configuration.Configuration;
import common.configuration.Option;
import common.configuration.Options;

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
@Options
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
  
  @Option(name="analysis.useAssumptionCollector")
  private boolean useAssumptionCollector = false;
  
  @Option(name="assumptions.observer.heuristics", required=true)
  private String[] heuristicsNames = {};
  
  private final ProgressObserverDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;
  private final LogManager logger;
  
  private final ImmutableList<StopHeuristics<?>> enabledHeuristics;
  
  /** Return the immutable list of enables heuristics */
  public ImmutableList<StopHeuristics<?>> getEnabledHeuristics()
  {
    return enabledHeuristics;
  }
  
  private ImmutableList<StopHeuristics<?>> createEnabledHeuristics(Configuration config)
  {
    ImmutableList.Builder<StopHeuristics<?>> builder = ImmutableList.builder();
    
    for (String heuristicsName : heuristicsNames) {
      if (!heuristicsName.contains("."))
        heuristicsName = "cpa.assumptions.collector.progressobserver." + heuristicsName;
      try {
        Class<?> cls = Class.forName(heuristicsName);
        Constructor<?> constructor = cls.getConstructor(Configuration.class, LogManager.class);
        Configuration localConfig = new Configuration(config, heuristicsName);
        Object obj = constructor.newInstance(localConfig, logger);
        
        // Convert object to StopHeuristics
        StopHeuristics<?> newHeuristics = (StopHeuristics<?>)obj;
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
  
  private ProgressObserverCPA(Configuration cfg, LogManager mgr)
  {
    logger = mgr;
    
    // Check if assumption collector is enabled; if not, the analysis will
    // not be sound
    if (!useAssumptionCollector)
      logger.log(Level.WARNING, "Analysis may not be sound because ProgressObserverCPA is used without assumption collector");

    enabledHeuristics = createEnabledHeuristics(cfg);
    
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
