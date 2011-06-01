package org.sosy_lab.cpachecker.cpa.seqcomposite;

import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class SeqCompositeCPA implements ConfigurableProgramAnalysis, StatisticsProvider, WrapperCPA {

  private List<ConfigurableProgramAnalysis> listofCPAs;
  private ConfigurableProgramAnalysis currentCPA;
  private int currentCPAIdx = 0;
  
  
  public static CPAFactory factory(){
    return new SeqCompositeCPAFactory();
  }
  
  protected SeqCompositeCPA(ImmutableList<ConfigurableProgramAnalysis> cpas){
    listofCPAs = cpas;
    currentCPA = listofCPAs.get(currentCPAIdx);
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return currentCPA.getAbstractDomain();
  }

  @Override
  public AbstractElement getInitialElement(CFANode pNode) {
    return currentCPA.getInitialElement(pNode);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return currentCPA.getInitialPrecision(pNode);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return currentCPA.getMergeOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return currentCPA.getPrecisionAdjustment();
  }

  @Override
  public StopOperator getStopOperator() {
    return currentCPA.getStopOperator();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return currentCPA.getTransferRelation();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(
      Class<T> pType) {
    return (T) currentCPA;
  }
  
  public boolean switchCPA(){
    if(currentCPAIdx == listofCPAs.size()-1) return false;
    else currentCPAIdx++;
    currentCPA = listofCPAs.get(currentCPAIdx);
    return true;
  }

  private static class SeqCompositeCPAFactory extends AbstractCPAFactory {

    private ImmutableList<ConfigurableProgramAnalysis> cpas = null;
    
    @Override
    public ConfigurableProgramAnalysis createInstance()
        throws InvalidConfigurationException, CPAException {
      Preconditions.checkState(cpas != null, "SeqCompositeCPA needs wrapped CPAs!");
      return new SeqCompositeCPA(cpas);
    }
    
  }
  
}
