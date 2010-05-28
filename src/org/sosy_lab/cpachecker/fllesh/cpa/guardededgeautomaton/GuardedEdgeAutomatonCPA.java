package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.util.Automaton;

public class GuardedEdgeAutomatonCPA implements ConfigurableProgramAnalysis {

  private GuardedEdgeAutomatonDomain mDomain;
  private GuardedEdgeAutomatonStandardElement mInitialElement;
  private StopSepOperator mStopOperator;
  private GuardedEdgeAutomatonTransferRelation mTransferRelation;
  
  public GuardedEdgeAutomatonCPA(Automaton<GuardedEdgeLabel> pAutomaton) {
    mDomain = new GuardedEdgeAutomatonDomain();
    mStopOperator = new StopSepOperator(mDomain.getPartialOrder());
    mTransferRelation = new GuardedEdgeAutomatonTransferRelation(mDomain, pAutomaton);
    
    Automaton<GuardedEdgeLabel>.State lInitialState = pAutomaton.getInitialState();
    boolean lIsFinal = pAutomaton.getFinalStates().contains(lInitialState);
    mInitialElement = new GuardedEdgeAutomatonStandardElement(lInitialState, lIsFinal, mTransferRelation.mPrettyPrinter.printPretty(lInitialState));
  }
  
  @Override
  public GuardedEdgeAutomatonDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return mInitialElement;
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
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return mTransferRelation;
  }

}
