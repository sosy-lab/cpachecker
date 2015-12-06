package org.sosy_lab.cpachecker.cpa.context;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.AThreadContainer;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class ContextSwitchCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithBAM, ProofChecker, ReachedSetAdjustingCPA {
  
  private AThreadContainer initialThreads;
  
  protected ContextSwitchCPA(Configuration config, LogManager pLogger, CFA cfa) throws InvalidConfigurationException {
    super("sep", "sep", new ContextSwitchTransferRelation(config, pLogger, cfa));
    assert cfa.getThreads().isPresent();
    this.initialThreads = cfa.getThreads().get();
  }
  
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ContextSwitchCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return ThreadState.getInitialState(initialThreads);
  }

  @Override
  public boolean adjustPrecision() {
    //TODO implement!!
    return true;
//    ContextSwitchTransferRelation cs = (ContextSwitchTransferRelation) getTransferRelation();
//    cs.contextSwitchBound++;
//    return true;
  }

  @Override
  public void adjustReachedSet(ReachedSet pReachedSet) {
    // No action required
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState state, CFAEdge cfaEdge, Collection<? extends AbstractState> successors)
      throws CPATransferException, InterruptedException {
      //TODO implement
    throw new UnsupportedOperationException("not impelemnted yet");
  }

  @Override
  public boolean isCoveredBy(AbstractState state, AbstractState otherState) throws CPAException, InterruptedException {
    return (getAbstractDomain().isLessOrEqual(state, otherState));
  }

  @Override
  public Reducer getReducer() {
    // TODO implement
//    throw new UnsupportedOperationException("not implemented yet!");
    return null;
  }

}
