package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

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