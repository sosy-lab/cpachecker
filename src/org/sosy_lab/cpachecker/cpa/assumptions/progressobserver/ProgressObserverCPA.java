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
package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Classes.ClassInstantiationException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * @author g.theoduloz
 *
 */
@Options(prefix="cpa.assumptions.progressobserver")
public class ProgressObserverCPA implements ConfigurableProgramAnalysis {

  private static class ProgressObserverCPAFactory extends AbstractCPAFactory {
    @Override
    public ConfigurableProgramAnalysis createInstance()
      throws InvalidConfigurationException
    {
      return new ProgressObserverCPA(getConfiguration(), getLogger());
    }
  }

  public static CPAFactory factory() {
    return new ProgressObserverCPAFactory();
  }

  @Option(name="heuristics", required=true)
  private String[] heuristicsNames = {};
  
  private final ProgressObserverDomain abstractDomain;
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

    Class<?>[] argsTypes = {Configuration.class, LogManager.class}; 
    for (String heuristicsName : heuristicsNames) {
      if (!heuristicsName.contains(".")) {
        heuristicsName = "org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.heuristics." + heuristicsName;
      }
      try {
        Class<?> cls = Class.forName(heuristicsName);
        Configuration localConfig = Configuration.copyWithNewPrefix(config, cls.getSimpleName());
        Object[] localArgs = {localConfig, logger};
        StopHeuristics<?> newHeuristics = Classes.createInstance(heuristicsName, null, argsTypes, localArgs, StopHeuristics.class);
        builder.add(newHeuristics);
      } catch (ClassNotFoundException e) {
        logger.logException(Level.WARNING, e, "ClassNotFoundException");
      } catch (InvocationTargetException e) {
        logger.logException(Level.WARNING, e, "InvocationTargetException");
      } catch (ClassInstantiationException e) {
        logger.logException(Level.WARNING, e, "ClassInstantiationException");
      }
    }

    return builder.build();
  }

  private ProgressObserverCPA(Configuration cfg, LogManager mgr) throws InvalidConfigurationException
  {
    logger = mgr;
    cfg.inject(this);

    enabledHeuristics = createEnabledHeuristics(cfg);

    abstractDomain = new ProgressObserverDomain();
    stopOperator = new ProgressObserverStop();
    transferRelation = new ProgressObserverTransferRelation(this);
    precisionAdjustment = new ProgressObserverPrecisionAdjustment(this);
  }

  @Override
  public ProgressObserverDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public ProgressObserverElement getInitialElement(CFAFunctionDefinitionNode node) {
    List<StopHeuristicsData> data = new ArrayList<StopHeuristicsData>(enabledHeuristics.size());
    for (StopHeuristics<? extends StopHeuristicsData> h : enabledHeuristics) {
      data.add(h.getInitialData(node));
    }
    return new ProgressObserverElement(data);
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
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
