/**
 * 
 */
package cpa.scoperestriction;

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
public class ScopeRestrictionCPA implements ConfigurableProgramAnalysis {
  
  public class ScopeRestrictionMergeOperator implements MergeOperator {

    @Override
    public AbstractElement merge(AbstractElement pElement1,
                                 AbstractElement pElement2) throws CPAException {
      // no join
      return pElement2;
    }
    
  }
  
  public class ScopeRestrictionStopOperator implements StopOperator {
    
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
  
  public class ScopeRestrictionTransferRelation implements TransferRelation {

    @Override
    public AutomatonCPADomain<CFAEdge>.Element getAbstractSuccessor(AbstractElement pElement,
                                                CFAEdge pCfaEdge)
                                                                 throws CPATransferException {
      AutomatonCPADomain<CFAEdge>.Element lSuccessor = mDomain.getSuccessor(pElement, pCfaEdge);
      
      // we want a deterministic behavior
      assert(lSuccessor.isSingleton() || lSuccessor.equals(mDomain.getBottomElement()) || lSuccessor.equals(mDomain.getTopElement()));
      
      return lSuccessor;
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
  private ScopeRestrictionMergeOperator mMergeOperator;
  private ScopeRestrictionStopOperator mStopOperator;
  private ScopeRestrictionTransferRelation mTransferRelation;
  
  public ScopeRestrictionCPA(Automaton<CFAEdge> pTestGoalAutomaton) {
    // Check for invariant: No final states
    assert(pTestGoalAutomaton.getFinalStates().isEmpty());
    
    mDomain = new AutomatonCPADomain<CFAEdge>(pTestGoalAutomaton);
    
    mMergeOperator = new ScopeRestrictionMergeOperator();
    mStopOperator = new ScopeRestrictionStopOperator();
    mTransferRelation = new ScopeRestrictionTransferRelation();
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
  public ScopeRestrictionMergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getStopOperator()
   */
  @Override
  public ScopeRestrictionStopOperator getStopOperator() {
    return mStopOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getTransferRelation()
   */
  @Override
  public ScopeRestrictionTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

}
