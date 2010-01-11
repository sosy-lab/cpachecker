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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cmdline.CPAMain;
import cpa.common.CallElement;
import cpa.common.CallStack;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.CPAWithStatistics;
import cpa.common.interfaces.CPAWrapper;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.transferrelationmonitor.TransferRelationMonitorCPA;
import cpaplugin.CPAStatistics;
import exceptions.CPAException;

public class CompositeCPA implements ConfigurableProgramAnalysis, CPAWithStatistics, CPAWrapper
{
  private final AbstractDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final AbstractElementWithLocation initialElement;
  private final Precision initialPrecision;

  private final List<ConfigurableProgramAnalysis> cpas;
  
  private CompositeCPA (AbstractDomain abstractDomain,
      TransferRelation transferRelation,
      MergeOperator mergeOperator,
      StopOperator stopOperator,
      PrecisionAdjustment precisionAdjustment,
      AbstractElementWithLocation initialElement,
      Precision initialPrecision,
      List<ConfigurableProgramAnalysis> cpas)
  {
    this.abstractDomain = abstractDomain;
    this.transferRelation = transferRelation;
    this.mergeOperator = mergeOperator;
    this.stopOperator = stopOperator;
    this.precisionAdjustment = precisionAdjustment;
    this.initialElement = initialElement;
    this.initialPrecision = initialPrecision;
    this.cpas = Collections.unmodifiableList(cpas);
  }

  public static CompositeCPA createNewCompositeCPA(List<ConfigurableProgramAnalysis> cpas, CFAFunctionDefinitionNode node) {

    List<AbstractDomain> domains = new ArrayList<AbstractDomain> ();
    List<TransferRelation> transferRelations = new ArrayList<TransferRelation> ();
    List<MergeOperator> mergeOperators = new ArrayList<MergeOperator> ();
    List<StopOperator> stopOperators = new ArrayList<StopOperator> ();
    List<PrecisionAdjustment> precisionAdjustments = new ArrayList<PrecisionAdjustment> ();
    List<AbstractElement> initialElements = new ArrayList<AbstractElement> ();
    List<Precision> initialPrecisions = new ArrayList<Precision> ();

    for(ConfigurableProgramAnalysis sp : cpas) {
      domains.add(sp.getAbstractDomain());
      transferRelations.add(sp.getTransferRelation());
      mergeOperators.add(sp.getMergeOperator());
      stopOperators.add(sp.getStopOperator());
      precisionAdjustments.add(sp.getPrecisionAdjustment());
      initialElements.add(sp.getInitialElement(node));
      initialPrecisions.add(sp.getInitialPrecision(node));
    }

    CompositeDomain compositeDomain = new CompositeDomain (domains);
    CompositeTransferRelation compositeTransfer = new CompositeTransferRelation (compositeDomain, transferRelations);
    CompositeMergeOperator compositeMerge = new CompositeMergeOperator (compositeDomain, mergeOperators);
    CompositeStopOperator compositeStop = new CompositeStopOperator (compositeDomain, stopOperators);
    CompositePrecisionAdjustment compositePrecisionAdjustment = new CompositePrecisionAdjustment (precisionAdjustments);
    CompositeElement initialElement = new CompositeElement (initialElements, null);
    CompositePrecision initialPrecision = new CompositePrecision (initialPrecisions);
    // set call stack
    CallStack initialCallStack = new CallStack();
    CallElement initialCallElement = new CallElement(node.getFunctionName(), node, initialElement);
    initialCallStack.push(initialCallElement);
    initialElement.setCallStack(initialCallStack);

    return new CompositeCPA(compositeDomain, compositeTransfer, compositeMerge, compositeStop,
        compositePrecisionAdjustment, initialElement, initialPrecision, cpas);
  }

  @SuppressWarnings("unchecked")
  public static ConfigurableProgramAnalysis getCompositeCPA (CFAFunctionDefinitionNode node) throws CPAException
  {
    String[] cpaNamesArray = CPAMain.cpaConfig.getPropertiesArray("analysis.cpas");
    String[] mergeTypesArray = CPAMain.cpaConfig.getPropertiesArray("analysis.mergeOperators");
    String[] stopTypesArray = CPAMain.cpaConfig.getPropertiesArray("analysis.stopOperators");
    String[] cpaToBeMonitored = null;
    if(CPAMain.cpaConfig.getBooleanValue("monitoringCPA.enable")){
      cpaToBeMonitored = CPAMain.cpaConfig.getPropertiesArray("monitoringCPA.cpa"); 
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
        CPAMain.logManager.logException(Level.WARNING, e, "ClassNotFoundException");
      } catch (SecurityException e) {
        CPAMain.logManager.logException(Level.WARNING, e, "SecurityException");
      } catch (NoSuchMethodException e) {
        CPAMain.logManager.logException(Level.WARNING, e, "NoSuchMethodException");
      } catch (IllegalArgumentException e) {
        CPAMain.logManager.logException(Level.WARNING, e, "IllegalArgumentException");
      } catch (InstantiationException e) {
        CPAMain.logManager.logException(Level.WARNING, e, "InstantiationException");
      } catch (IllegalAccessException e) {
        CPAMain.logManager.logException(Level.WARNING, e, "IllegalAccessException");
      } catch (InvocationTargetException e) {
        CPAMain.logManager.logException(Level.WARNING, e, "InvocationTargetException");
      }
    }

    ConfigurableProgramAnalysis cpa;
    // TODO this was for efficiency but I modified the condition for it to work only with
    // summary nodes
    if (cpas.size() == 1 &&
        CPAMain.cpaConfig.getBooleanValue("analysis.noCompositeCPA")) {
      CPAMain.logManager.log(Level.FINE, "Only one analyis active, no need of a composite CPA");
      cpa = cpas.get(0);
    } else {
      CPAMain.logManager.log(Level.FINE, "CompositeCPA is built using the list of CPAs");
      cpa = CompositeCPA.createNewCompositeCPA(cpas, node);
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
    return initialElement;
  }

  public Precision getInitialPrecision (CFAFunctionDefinitionNode node) {
    return initialPrecision;
  }
  
  public List<ConfigurableProgramAnalysis> getComponentCPAs() {
    return cpas;
  }

  @Override
  public void collectStatistics(Collection<CPAStatistics> pStatsCollection) {
    for (ConfigurableProgramAnalysis cpa: cpas) {
      if (cpa instanceof CPAWithStatistics) {
        ((CPAWithStatistics)cpa).collectStatistics(pStatsCollection);
      }
    }
  }
  
  @Override
  public Iterable<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return cpas;
  }
}