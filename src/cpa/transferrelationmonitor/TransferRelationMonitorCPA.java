package cpa.transferrelationmonitor;

import cfa.objectmodel.CFAFunctionDefinitionNode;

import common.configuration.Configuration;

import cpa.common.defaults.AbstractSingleWrapperCPA;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.InvalidConfigurationException;

public class TransferRelationMonitorCPA extends AbstractSingleWrapperCPA {

  private static class TransferRelationMonitorCPAFactory extends AbstractSingleWrapperCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
      return new TransferRelationMonitorCPA(getChild(), getConfiguration());
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
  
  private TransferRelationMonitorCPA(ConfigurableProgramAnalysis pCpa, Configuration config) throws InvalidConfigurationException {
    super(pCpa);
    abstractDomain = new TransferRelationMonitorDomain(this);
    transferRelation = new TransferRelationMonitorTransferRelation(getWrappedCpa(), config);
    precisionAdjustment = StaticPrecisionAdjustment.getInstance(); // TODO
    mergeOperator = new TransferRelationMonitorMerge(getWrappedCpa());
    stopOperator = new TransferRelationMonitorStop(getWrappedCpa());  
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return this.abstractDomain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return new TransferRelationMonitorElement(this, getWrappedCpa().getInitialElement(pNode));
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
}