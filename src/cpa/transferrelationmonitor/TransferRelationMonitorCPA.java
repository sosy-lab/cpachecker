package cpa.transferrelationmonitor;

import java.util.Collection;
import java.util.Collections;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAWithStatistics;
import cpa.common.interfaces.CPAWrapper;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpaplugin.CPAStatistics;
import exceptions.CPAException;

public class TransferRelationMonitorCPA implements ConfigurableProgramAnalysis, CPAWithStatistics, CPAWrapper {

  private final AbstractDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final ConfigurableProgramAnalysis wrappedCPA;
  
  public TransferRelationMonitorCPA(ConfigurableProgramAnalysis pCpa) throws CPAException{
    wrappedCPA = pCpa;
    abstractDomain = new TransferRelationMonitorDomain(this);
    transferRelation = new TransferRelationMonitorTransferRelation(wrappedCPA.getTransferRelation(), 
        (TransferRelationMonitorDomain)abstractDomain);
    precisionAdjustment = new TransferRelationMonitorPrecisionAdjustment();
    mergeOperator = new TransferRelationMonitorMerge(wrappedCPA);
    stopOperator = new TransferRelationMonitorStop(wrappedCPA);  
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return this.abstractDomain;
  }

  @Override
  public <AE extends AbstractElement> AE getInitialElement(
      CFAFunctionDefinitionNode pNode) {
    return (AE) new TransferRelationMonitorElement(this, (AbstractElement)wrappedCPA.getInitialElement(pNode));
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return wrappedCPA.getInitialPrecision(pNode);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return this.mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return this.precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return this.stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this.transferRelation;
  }

  @Override
  public void collectStatistics(Collection<CPAStatistics> pStatsCollection) {
    if (wrappedCPA instanceof CPAWithStatistics) {
      ((CPAWithStatistics)wrappedCPA).collectStatistics(pStatsCollection);
    }
  }

  @Override
  public Iterable<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return Collections.singletonList(wrappedCPA);
  }
}