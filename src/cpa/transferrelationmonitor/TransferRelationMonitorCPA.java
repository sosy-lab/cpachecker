package cpa.transferrelationmonitor;

import java.util.Collection;
import java.util.Collections;

import cfa.objectmodel.CFAFunctionDefinitionNode;

import com.google.common.base.Preconditions;
import common.configuration.Configuration;

import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.defaults.StaticPrecisionAdjustment;
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
import exceptions.InvalidConfigurationException;

public class TransferRelationMonitorCPA implements ConfigurableProgramAnalysis, StatisticsProvider, CPAWrapper {

  private static class TransferRelationMonitorCPAFactory extends AbstractCPAFactory {
    
    private ConfigurableProgramAnalysis cpa = null;

    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
      Preconditions.checkState(cpa != null, "TransferRelationMonitorCPA needs a wrapped CPA!");

      return new TransferRelationMonitorCPA(cpa, getConfiguration());
    }
    
    @Override
    public CPAFactory setChild(ConfigurableProgramAnalysis pChild) {
      Preconditions.checkNotNull(pChild);
      Preconditions.checkState(cpa == null);
      
      cpa = pChild;
      return this;
    }
  }
  
  public static CPAFactory factory() {
    return new TransferRelationMonitorCPAFactory();
  }
  
  private final AbstractDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final ConfigurableProgramAnalysis wrappedCPA;
  
  private TransferRelationMonitorCPA(ConfigurableProgramAnalysis pCpa, Configuration config) throws InvalidConfigurationException {
    wrappedCPA = pCpa;
    abstractDomain = new TransferRelationMonitorDomain(this);
    transferRelation = new TransferRelationMonitorTransferRelation(wrappedCPA, config);
    precisionAdjustment = StaticPrecisionAdjustment.getInstance(); // TODO
    mergeOperator = new TransferRelationMonitorMerge(wrappedCPA);
    stopOperator = new TransferRelationMonitorStop(wrappedCPA);  
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return this.abstractDomain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return new TransferRelationMonitorElement(this, wrappedCPA.getInitialElement(pNode));
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
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (wrappedCPA instanceof StatisticsProvider) {
      ((StatisticsProvider)wrappedCPA).collectStatistics(pStatsCollection);
    }
  }

  @Override
  public Iterable<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return Collections.singletonList(wrappedCPA);
  }
}