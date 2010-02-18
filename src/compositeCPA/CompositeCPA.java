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
package compositeCPA;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import cfa.objectmodel.CFAFunctionDefinitionNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import cpa.common.CPAchecker;
import cpa.common.CallElement;
import cpa.common.CallStack;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.CPAWrapper;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.Statistics;
import cpa.common.interfaces.StatisticsProvider;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.transferrelationmonitor.TransferRelationMonitorCPA;
import exceptions.CPAException;

public class CompositeCPA implements ConfigurableProgramAnalysis, StatisticsProvider, CPAWrapper
{
  private static class CompositeCPAFactory extends AbstractCPAFactory {

    private ImmutableList<ConfigurableProgramAnalysis> cpas = null;
    
    @Override
    public ConfigurableProgramAnalysis createInstance() throws CPAException {
      Preconditions.checkState(cpas != null, "CompositeCPA needs wrapped CPAs!");
      return createNewCompositeCPA(cpas);
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

  public static CompositeCPA createNewCompositeCPA(List<ConfigurableProgramAnalysis> pCpas) {
    ImmutableList<ConfigurableProgramAnalysis> cpas = ImmutableList.copyOf(pCpas);

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

  @SuppressWarnings("unchecked")
  public static ConfigurableProgramAnalysis getCompositeCPA() throws CPAException
  {
    String[] cpaNamesArray = CPAchecker.config.getPropertiesArray("analysis.cpas");
    String[] mergeTypesArray = CPAchecker.config.getPropertiesArray("analysis.mergeOperators");
    String[] stopTypesArray = CPAchecker.config.getPropertiesArray("analysis.stopOperators");
    String[] cpaToBeMonitored = null;
    if(CPAchecker.config.getBooleanValue("monitoringCPA.enable")){
      cpaToBeMonitored = CPAchecker.config.getPropertiesArray("monitoringCPA.cpa"); 
    }

    int sizeOfCompositeCPA = cpaNamesArray.length;
    if (0 == sizeOfCompositeCPA) throw new CPAException("Configuration option analysis.cpas is not set!");

    // The list to keep all cpas
    List<ConfigurableProgramAnalysis> cpas = new ArrayList<ConfigurableProgramAnalysis> (sizeOfCompositeCPA);

    for(int i=0; i<sizeOfCompositeCPA; i++){
      // TODO make sure that the first CPA carries location information
      // otherwise the analysis will have efficiency problems
      // -- this is currently checked when constructing a CompositeElement

      // get name of the cpa, we are getting the explicit
      // path of the representing class of this cpa
      String cpaName = cpaNamesArray[i];

      try {
        Class cls = Class.forName(cpaName);
        Class parameterTypes[] = {String.class, String.class};
        Constructor ct = cls.getConstructor(parameterTypes);
        Object argumentlist[] = {mergeTypesArray[i], stopTypesArray[i]};
        Object obj = ct.newInstance(argumentlist);
        // Convert object to CPA
        ConfigurableProgramAnalysis newCPA = (ConfigurableProgramAnalysis)obj;
        // TODO only one CPA can be monitored for now, combine more on demand and
        // monitor all later
        if(cpaToBeMonitored != null){
          if(cpaToBeMonitored[0].equals(cpaName)){
            TransferRelationMonitorCPA monitoringCPA = new TransferRelationMonitorCPA(newCPA);
            newCPA = monitoringCPA;
          }
        }
        cpas.add(newCPA);

      } catch (ClassNotFoundException e) {
        CPAchecker.logger.logException(Level.WARNING, e, "ClassNotFoundException");
      } catch (SecurityException e) {
        CPAchecker.logger.logException(Level.WARNING, e, "SecurityException");
      } catch (NoSuchMethodException e) {
        CPAchecker.logger.logException(Level.WARNING, e, "NoSuchMethodException");
      } catch (IllegalArgumentException e) {
        CPAchecker.logger.logException(Level.WARNING, e, "IllegalArgumentException");
      } catch (InstantiationException e) {
        CPAchecker.logger.logException(Level.WARNING, e, "InstantiationException");
      } catch (IllegalAccessException e) {
        CPAchecker.logger.logException(Level.WARNING, e, "IllegalAccessException");
      } catch (InvocationTargetException e) {
        CPAchecker.logger.logException(Level.WARNING, e, "InvocationTargetException");
      }
    }

    ConfigurableProgramAnalysis cpa;
    // TODO this was for efficiency but I modified the condition for it to work only with
    // summary nodes
    if (cpas.size() == 1 &&
        CPAchecker.config.getBooleanValue("analysis.noCompositeCPA")) {
      CPAchecker.logger.log(Level.FINE, "Only one analyis active, no need of a composite CPA");
      cpa = cpas.get(0);
    } else {
      CPAchecker.logger.log(Level.FINE, "CompositeCPA is built using the list of CPAs");
      cpa = CompositeCPA.createNewCompositeCPA(cpas);
    }
    return cpa;
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
  
  public List<ConfigurableProgramAnalysis> getComponentCPAs() {
    return cpas;
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
  public Iterable<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return cpas;
  }
}