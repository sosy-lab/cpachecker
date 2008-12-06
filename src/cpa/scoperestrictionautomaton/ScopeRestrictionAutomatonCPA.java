/**
 * 
 */
package cpa.scoperestrictionautomaton;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFAEdge;
import cpa.common.CPATransferException;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TopElement;
import cpa.common.interfaces.TransferRelation;

import cpa.common.automaton.Label;
import exceptions.CPAException;

/**
 * @author holzera
 *
 * This class implements the CPA for a scope-restriction automaton. It represents
 * the automaton.
 */
public class ScopeRestrictionAutomatonCPA implements
                                         ConfigurableProgramAnalysis,
                                         StopOperator,
                                         MergeOperator,
                                         TransferRelation,
                                         AbstractDomain,
                                         JoinOperator,
                                         PartialOrder {
  
  private ScopeRestrictionAutomatonState mInitialState;
  private final BottomElementScopeRestrictionAutomatonState mBottom = new BottomElementScopeRestrictionAutomatonState(this);
  private final TopElementScopeRestrictionAutomatonState mTop = new TopElementScopeRestrictionAutomatonState(this);
  
  private Set<ScopeRestrictionAutomatonState> mFinalStates;
  
  public ScopeRestrictionAutomatonCPA(String pMergeConfiguration, String pStopConfiguration) {
    mInitialState = new ScopeRestrictionAutomatonState(this);
    mFinalStates = new HashSet<ScopeRestrictionAutomatonState>();
  }
  
  public ScopeRestrictionAutomatonState createState() {
    return new ScopeRestrictionAutomatonState(this);
  }
  
  public ScopeRestrictionAutomatonState getInitialState() {
    return mInitialState;
  }
  
  public void addTransition(ScopeRestrictionAutomatonState pQ1, ScopeRestrictionAutomatonState pQ2, Label<CFAEdge> pLabel) {
    pQ1.addTransition(pQ2, pLabel);
  }
  
  public void setFinal(ScopeRestrictionAutomatonState pState) {
    assert(pState != null);
    
    mFinalStates.add(pState);
  }
  
  public boolean isFinal(ScopeRestrictionAutomatonState pState) {
    return mFinalStates.contains(pState);
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getAbstractDomain()
   */
  @Override
  public AbstractDomain getAbstractDomain() {
    return this;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getInitialElement(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return mInitialState;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getMergeOperator()
   */
  @Override
  public MergeOperator getMergeOperator() {
    return this;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getStopOperator()
   */
  @Override
  public StopOperator getStopOperator() {
    return this;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getTransferRelation()
   */
  @Override
  public TransferRelation getTransferRelation() {
    return this;
  }

  @Override
  public <AE extends AbstractElement> boolean stop(AE pElement,
                      Collection<AE> pReached) throws CPAException {
    assert(pElement != null);
    assert(pReached != null);
    
    for (AbstractElement lElement : pReached) {
      if (stop(pElement, lElement)) {
        return true;
      }
    }
    
    return false;
  }

  @Override
  public boolean stop(AbstractElement pElement, AbstractElement pReachedElement)
                                                                                throws CPAException {
    assert(pElement != null);
    assert(pReachedElement != null);
    
    return getAbstractDomain().getPartialOrder().satisfiesPartialOrder(pElement, pReachedElement);
  }

  @Override
  public AbstractElement merge(AbstractElement pElement1,
                               AbstractElement pElement2) throws CPAException {
    return pElement2;
  }

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement pElement,
                                              CFAEdge pCfaEdge)
                                                               throws CPATransferException {
    assert(pElement != null);
    assert(pElement instanceof ScopeRestrictionAutomatonState);
    
    ScopeRestrictionAutomatonState lState = (ScopeRestrictionAutomatonState)pElement;
    
    return lState.getSuccessor(pCfaEdge);
  }

  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(AbstractElementWithLocation pElement)
                                                                                 throws CPAException,
                                                                                 CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

  @Override
  public BottomElement getBottomElement() {
    return mBottom;
  }

  @Override
  public JoinOperator getJoinOperator() {
    return this;
  }

  @Override
  public PartialOrder getPartialOrder() {
    return this;
  }

  @Override
  public TopElement getTopElement() {
    return mTop;
  }

  @Override
  public boolean isBottomElement(AbstractElement pElement) {
    return mBottom.equals(pElement);
  }

  @Override
  public AbstractElement join(AbstractElement pElement1,
                              AbstractElement pElement2) throws CPAException {
    if (satisfiesPartialOrder(pElement1, pElement2)) {
      return pElement2;
    }
    
    if (satisfiesPartialOrder(pElement2, pElement1)) {
      return pElement1;
    }
    
    return mTop;
  }

  @Override
  public boolean satisfiesPartialOrder(AbstractElement pElement1,
                                       AbstractElement pElement2)
                                                                 throws CPAException {
    if (pElement1.equals(mBottom)) {
      return true;
    }
    
    if (pElement2.equals(mTop)) {
      return true;
    }
    
    return (pElement1.equals(pElement2));
  }

}
