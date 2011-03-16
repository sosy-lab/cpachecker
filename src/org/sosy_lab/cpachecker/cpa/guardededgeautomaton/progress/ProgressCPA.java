package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.progress;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonDomain;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStandardElement;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;

public class ProgressCPA implements ConfigurableProgramAnalysis {
  
  private final GuardedEdgeAutomatonDomain mDomain;
  private final GuardedEdgeAutomatonStandardElement mInitialElement;
  private final StopSepOperator mStopOperator;
  private final ProgressTransferRelation mTransferRelation;
  private final ProgressPrecisionAdjustment mPrecisionAdjustment;
  private final ProgressPrecision mInitialPrecision;
  
  public ProgressCPA(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton, Collection<NondeterministicFiniteAutomaton.State> pReachedAutomatonStates) {
    mDomain = GuardedEdgeAutomatonDomain.getInstance();
    mStopOperator = new StopSepOperator(mDomain);
    mTransferRelation = new ProgressTransferRelation(mDomain, pAutomaton, pReachedAutomatonStates);
    
    NondeterministicFiniteAutomaton.State lInitialState = pAutomaton.getInitialState();
    boolean lIsFinal = pAutomaton.getFinalStates().contains(lInitialState);
    mInitialElement = new GuardedEdgeAutomatonStandardElement(lInitialState, lIsFinal);
    
    mInitialPrecision = new ProgressPrecision(pAutomaton);
    
    mPrecisionAdjustment = new ProgressPrecisionAdjustment();
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return mTransferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return mInitialElement;
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return mInitialPrecision;
  }

}
