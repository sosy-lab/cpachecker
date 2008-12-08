/**
 * 
 */
package cpa.testgoal;

import java.util.Collection;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.CPATransferException;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

/**
 * @author holzera
 *
 */
public class TestGoalCPA implements ConfigurableProgramAnalysis {
  
  public class TestGoalMergeOperator implements MergeOperator {

    @Override
    public AbstractElement merge(AbstractElement pElement1,
                                 AbstractElement pElement2) throws CPAException {
      // no join
      return pElement2;
    }
    
  }
  
  public class TestGoalStopOperator implements StopOperator {

    @Override
    public boolean stop(AbstractElement pElement,
                        Collection<AbstractElement> pReached)
                                                             throws CPAException {
      assert(pElement != null);
      assert(pReached != null);
      
      // exists lElement in pReached with stop(pElement, lElement)?
      for (AbstractElement lElement : pReached) {
        if (stop(pElement, lElement)) {
          return true;
        }
      }
      
      return false;
    }

    @Override
    public boolean stop(AbstractElement pElement,
                        AbstractElement pReachedElement) throws CPAException {
      return mDomain.getPartialOrder().satisfiesPartialOrder(pElement, pReachedElement);
    }
    
  }
  
  public class TestGoalTransferRelation implements TransferRelation {

    @Override
    public AbstractElement getAbstractSuccessor(AbstractElement pElement,
                                                CFAEdge pCfaEdge)
                                                                 throws CPATransferException {
      return mDomain.getSuccessor(pElement, pCfaEdge);
    }

    @Override
    public List<AbstractElement> getAllAbstractSuccessors(AbstractElement pElement)
                                                                                   throws CPAException,
                                                                                   CPATransferException {
      // this method may not be called!
      assert(false);
      
      return null;
    }
    
  }
  
  private AutomatonCPADomain<CFAEdge> mDomain;
  private TestGoalMergeOperator mMergeOperator;
  private TestGoalStopOperator mStopOperator;
  private TestGoalTransferRelation mTransferRelation;
  
  public TestGoalCPA(Automaton<CFAEdge> pTestGoalAutomaton) {
    // Check for invariant: Final states will not be left once they are reached.
    for (Automaton<CFAEdge>.State lState : pTestGoalAutomaton.getFinalStates()) {
      // we ensure the property by a very strict assumption:
      assert(lState.hasUnconditionalSelfLoop());
    }
    
    mDomain = new AutomatonCPADomain<CFAEdge>(pTestGoalAutomaton);
    
    mMergeOperator = new TestGoalMergeOperator();
    mStopOperator = new TestGoalStopOperator();
    mTransferRelation = new TestGoalTransferRelation();
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getAbstractDomain()
   */
  @Override
  public AutomatonCPADomain<CFAEdge> getAbstractDomain() {
    return mDomain;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getInitialElement(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  @Override
  public AutomatonCPADomain<CFAEdge>.Element getInitialElement(CFAFunctionDefinitionNode pNode) {
    return mDomain.getInitialElement();
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getMergeOperator()
   */
  @Override
  public TestGoalMergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getStopOperator()
   */
  @Override
  public TestGoalStopOperator getStopOperator() {
    return mStopOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getTransferRelation()
   */
  @Override
  public TestGoalTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

}
