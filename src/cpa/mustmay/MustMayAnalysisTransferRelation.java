package cpa.mustmay;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;

public class MustMayAnalysisTransferRelation implements TransferRelation {

  private TransferRelation mMustTransferRelation;
  private TransferRelation mMayTransferRelation;
  
  private MustMayAnalysisElement mBottomElement;
  
  private AbstractElement mMayBottomElement;
  
  public MustMayAnalysisTransferRelation(TransferRelation pMustTransferRelation, TransferRelation pMayTransferRelation, MustMayAnalysisElement pBottomElement) {
    assert(pMustTransferRelation != null);
    assert(pMayTransferRelation != null);
    assert(pBottomElement != null);
    
    mMustTransferRelation = pMustTransferRelation;
    mMayTransferRelation = pMayTransferRelation;
    
    mBottomElement = pBottomElement;
    mMayBottomElement = mBottomElement.getMayElement();
  }

  @Override
  // TODO: public <T extends AbstractElement> Collection<AbstractElement> getAbstractSuccessors(T pCurrentElement, Precision pPrecision, CFAEdge pCfaEdge)
  public Collection<AbstractElement> getAbstractSuccessors(AbstractElement pCurrentElement, Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException {
    
    assert(pCurrentElement != null);
    assert(pCurrentElement instanceof MustMayAnalysisElement);
    
    assert(pPrecision != null);
    assert(pPrecision instanceof MustMayAnalysisPrecision);
    
    assert(pCfaEdge != null);
    
    MustMayAnalysisElement lCurrentElement = (MustMayAnalysisElement)pCurrentElement;

    AbstractElement lCurrentMayElement = lCurrentElement.getMayElement();
    AbstractElement lCurrentMustElement = lCurrentElement.getMustElement();
    
    MustMayAnalysisPrecision lPrecision = (MustMayAnalysisPrecision)pPrecision;
    
    Collection<? extends AbstractElement> lMaySuccessors = mMayTransferRelation.getAbstractSuccessors(lCurrentMayElement, lPrecision.getMayPrecision(), pCfaEdge);
    
    HashSet<AbstractElement> lConsolidatedMaySuccessors = new HashSet<AbstractElement>();
    
    for (AbstractElement lSuccessor : lMaySuccessors) {
      if (!lSuccessor.equals(mMayBottomElement)) {
        // lSuccessor is not bottom element of may analysis
        lConsolidatedMaySuccessors.add(lSuccessor);
      }
    }
    
    if (lConsolidatedMaySuccessors.isEmpty()) {
      // if there are no may successors, then there can't be
      // must successors, thus return the empty set
      // TODO: discuss that bottom elements is not allowed to be returned
      // what says paper about this?
      return Collections.emptySet();
    }
    
    Collection<? extends AbstractElement> lMustSuccessors = mMustTransferRelation.getAbstractSuccessors(lCurrentMustElement, lPrecision.getMustPrecision(), pCfaEdge);
    
    HashSet<AbstractElement> lConsolidatedMustSuccessors = new HashSet<AbstractElement>();
    
    lConsolidatedMustSuccessors.addAll(lMustSuccessors);
    
    if (lConsolidatedMustSuccessors.isEmpty()) {
      // add bottom element for cross product generation
      lConsolidatedMustSuccessors.add(mBottomElement.getMustElement());
    }
    
    HashSet<AbstractElement> lSuccessors = new HashSet<AbstractElement>();
    
    // generate cross product
    for (AbstractElement lMaySuccessor : lConsolidatedMaySuccessors) {
      for (AbstractElement lMustSuccessor : lConsolidatedMustSuccessors) {
        // TODO: the strengthening operator of the must transfer relation has to guarantee (and establish) the subset relation of concretizations
        AbstractElement lStrengthenedMustSuccessor = mMustTransferRelation.strengthen(lMustSuccessor, Collections.singletonList((AbstractElement)lMaySuccessor), pCfaEdge, lPrecision.getMustPrecision());

        // TODO: why is null and not the passed element itself returned?
        if (lStrengthenedMustSuccessor == null) {
          lStrengthenedMustSuccessor = lMustSuccessor;
        }
        
        MustMayAnalysisElement lSuccessor = new MustMayAnalysisElement(lStrengthenedMustSuccessor, lMaySuccessor);
        
        lSuccessors.add(lSuccessor);
      }
    }
    
    return lSuccessors;
  }

  @Override
  public AbstractElement strengthen(AbstractElement pElement,
      List<AbstractElement> pOtherElements, CFAEdge pCfaEdge,
      Precision pPrecision) throws CPATransferException {
    // TODO Auto-generated method stub
    return null;
  }

}
