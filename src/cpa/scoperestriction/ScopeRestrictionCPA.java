/**
 * 
 */
package cpa.scoperestriction;

import java.util.Collection;
import java.util.List;

import common.Pair;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import exceptions.CPATransferException;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.ReachedSet;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

/**
 * @author holzera
 *
 */
public class ScopeRestrictionCPA implements ConfigurableProgramAnalysis {
  
  public class ScopeRestrictionPrecision implements Precision {
    
  }
  
  public class ScopeRestrictionMergeOperator implements MergeOperator {

    @Override
    public AbstractElement merge(AbstractElement pElement1,
                                 AbstractElement pElement2,
                                 Precision prec) throws CPAException {
      // no join
      return pElement2;
    }

    public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                             AbstractElementWithLocation pElement2,
                                             Precision prec) throws CPAException {
      throw new CPAException ("Cannot return element with location information");
    }
  }
  
  public class ScopeRestrictionStopOperator implements StopOperator {
    
    @Override
    public <AE extends AbstractElement> boolean stop(AE pElement,
                        Collection<AE> pReached, Precision prec)
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
  
  public class ScopeRestrictionPrecisionAdjustment implements PrecisionAdjustment {

    public <AE extends AbstractElement> Pair<AE, Precision> prec(
                                                                 AE pElement,
                                                                 Precision pPrecision,
                                                                 ReachedSet pElements) {
      return new Pair<AE,Precision> (pElement, pPrecision);
    }
    
  }
  
  public class ScopeRestrictionTransferRelation implements TransferRelation {

    @Override
    public AutomatonCPADomain<CFAEdge>.Element getAbstractSuccessor(AbstractElement pElement,
                                                CFAEdge pCfaEdge, Precision prec)
                                                                 throws CPATransferException {
      AutomatonCPADomain<CFAEdge>.Element lSuccessor = mDomain.getSuccessor(pElement, pCfaEdge);
      
      // we want a deterministic behavior
      assert(lSuccessor.isSingleton() || lSuccessor.equals(mDomain.getBottomElement()) || lSuccessor.equals(mDomain.getTopElement()));
      
      return lSuccessor;
    }

    @Override
    public List<AbstractElementWithLocation> getAllAbstractSuccessors(AbstractElementWithLocation pElement, Precision prec)
                                                                                   throws CPAException,
                                                                                   CPATransferException {
      // this method may not be called!
      assert(false);
      
      return null;
    }
    
  }
  
  private AutomatonCPADomain<CFAEdge> mDomain;
  private ScopeRestrictionTransferRelation mTransferRelation;
  private ScopeRestrictionMergeOperator mMergeOperator;
  private ScopeRestrictionStopOperator mStopOperator;
  private PrecisionAdjustment mPrecisionAdjustment;
  
  public ScopeRestrictionCPA(Automaton<CFAEdge> pTestGoalAutomaton) {
    // Check for invariant: No final states
    assert(pTestGoalAutomaton.getFinalStates().isEmpty());
    
    mDomain = new AutomatonCPADomain<CFAEdge>(pTestGoalAutomaton);
    mTransferRelation = new ScopeRestrictionTransferRelation();
    mMergeOperator = new ScopeRestrictionMergeOperator();
    mStopOperator = new ScopeRestrictionStopOperator();
    mPrecisionAdjustment = new ScopeRestrictionPrecisionAdjustment();
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getAbstractDomain()
   */
  @Override
  public AutomatonCPADomain<CFAEdge> getAbstractDomain() {
    return mDomain;
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getTransferRelation()
   */
  @Override
  public ScopeRestrictionTransferRelation getTransferRelation() {
    return mTransferRelation;
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
  
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getInitialElement(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  @Override
  public AutomatonCPADomain<CFAEdge>.Element getInitialElement(CFAFunctionDefinitionNode pNode) {
    return mDomain.getInitialElement();
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return new ScopeRestrictionPrecision();
  }
}
