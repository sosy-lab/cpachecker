package org.sosy_lab.cpachecker.cpa.guardededgeautomaton;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
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
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;

public class GuardedEdgeAutomatonCPA implements ConfigurableProgramAnalysis {

  private final GuardedEdgeAutomatonDomain mDomain;
  private final GuardedEdgeAutomatonStandardElement mInitialElement;
  private final StopSepOperator mStopOperator;
  private final GuardedEdgeAutomatonTransferRelation mTransferRelation;

  public GuardedEdgeAutomatonCPA(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
    mDomain = GuardedEdgeAutomatonDomain.getInstance();
    mStopOperator = new StopSepOperator(mDomain);
    mTransferRelation = new GuardedEdgeAutomatonTransferRelation(mDomain, pAutomaton);

    NondeterministicFiniteAutomaton.State lInitialState = pAutomaton.getInitialState();
    boolean lIsFinal = pAutomaton.getFinalStates().contains(lInitialState);
    mInitialElement = new GuardedEdgeAutomatonStandardElement(lInitialState, lIsFinal);
  }

  public NondeterministicFiniteAutomaton<GuardedEdgeLabel> getAutomaton() {
    return mTransferRelation.getAutomaton();
  }

  @Override
  public GuardedEdgeAutomatonDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public AbstractElement getInitialElement(CFANode pNode) {
    return mInitialElement;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
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
  public GuardedEdgeAutomatonTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

  @Override
  public String toString() {
    return mTransferRelation.getAutomaton().toString();
  }

}
