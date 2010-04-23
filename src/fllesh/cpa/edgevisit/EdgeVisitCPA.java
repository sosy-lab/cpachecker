package fllesh.cpa.edgevisit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.defaults.FlatLatticeDomain;
import cpa.common.defaults.MergeJoinOperator;
import cpa.common.defaults.SingletonPrecision;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractQueryableElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;
import exceptions.InvalidQueryException;

/**
 * @author holzera
 * 
 * This CPA tracks which CFA edge was taken.
 * CAUTION: The used stop operator violates the over-approximation
 * criterion: It always returns true because we do not want to
 * interfere with the stop operators of other CPAs. This CPA should
 * only serve as a simple oracle for queries of the observer 
 * automaton CPA.
 * 
 */
public class EdgeVisitCPA implements ConfigurableProgramAnalysis {

  private final StopOperator mStopOperator = new StopOperator() {

    @Override
    public boolean stop(AbstractElement pElement,
        Collection<AbstractElement> pReached, Precision pPrecision)
        throws CPAException {
      // In order to not interfere with the stop operators of other
      // CPAs we always are willing to stop.
      return true;
    }

    @Override
    public boolean stop(AbstractElement pElement,
        AbstractElement pReachedElement) throws CPAException {
      // In order to not interfere with the stop operators of other
      // CPAs we always are willing to stop.
      return true;
    }
    
  };
  
  private final TransferRelation mTransferRelation = new TransferRelation() {

    @Override
    public Collection<? extends AbstractElement> getAbstractSuccessors(
        AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
        throws CPATransferException {
      
      if (pElement.equals(mDomain.getBottomElement())) {
        return Collections.emptySet();
      }
      
      return Collections.singleton(mElements.get(pCfaEdge));
    }

    @Override
    public Collection<? extends AbstractElement> strengthen(
        AbstractElement pElement, List<AbstractElement> pOtherElements,
        CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
      // We do no strengthening.
      return null;
    }
    
  };
  
  public static abstract class EdgeVisitElement implements AbstractQueryableElement {
    
    @Override
    public String getCPAName() {
      return "edgevisit";
    }

    @Override
    public boolean isError() {
      return false;
    }
    
  }
  
  public static class TopElement extends EdgeVisitElement {

    private final static TopElement mInstance = new TopElement();
    
    private TopElement() {
      
    }
    
    public static TopElement getInstance() {
      return mInstance;
    }

    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      // We do not know which edge we have taken, so we return false.
      return false;
    }
    
    @Override
    public String toString() {
      return "<EdgeVisit - Top-Element>";
    }
    
  }
  
  public static class BottomElement extends EdgeVisitElement {

    private final static BottomElement mInstance = new BottomElement();
    
    private BottomElement() {
      
    }
    
    public static BottomElement getInstance() {
      return mInstance;
    }

    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      // We do not know which edge we have taken, so we return false.
      return false;
    }
    
    @Override
    public String toString() {
      return "<EdgeVisit - Bottom-Element>";
    }
    
  }
  
  public static class NoEdgeElement extends EdgeVisitElement {
    
    private final static NoEdgeElement mInstance = new NoEdgeElement();
    
    private NoEdgeElement() {
      
    }
    
    public static NoEdgeElement getInstance() {
      return mInstance;
    }

    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      // We have not visited a CFA edge, therefore we return false.
      return false;
    }
    
    @Override
    public String toString() {
      return "<VisitEdge - No Edge>";
    }
    
  }
  
  public static class EdgeElement extends EdgeVisitElement {
    
    private Set<String> mAnnotations;
    
    private final String mId;
    
    public EdgeElement(String pId) {
      mId = pId;
      mAnnotations = Collections.emptySet();
    }

    public EdgeElement(String pId, Set<String> pAnnotations) {
      mId = pId;
      mAnnotations = pAnnotations;
    }
    
    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      if (pProperty.startsWith("E")) {
        return mId.equals(pProperty);
      }
      else {
        return mAnnotations.contains(pProperty);
      }
    }
    
    @Override
    public String toString() {
      return "<VisitEdge - " + mId + ">";
    }
  }
  
  public static class Factory extends AbstractCPAFactory {

    Annotations mAnnotations;
    
    public Factory(Annotations pAnnotations) {
      mAnnotations = pAnnotations;
    }
    
    public String getId(CFAEdge pEdge) {
      return mAnnotations.getId(pEdge);
    }
    
    @Override
    public ConfigurableProgramAnalysis createInstance() throws CPAException {
      return new EdgeVisitCPA(mAnnotations);
    }
    
  }
  
  private final FlatLatticeDomain mDomain;
  private final SingletonPrecision mPrecision;
  private final PrecisionAdjustment mPrecisionAdjustment;
  private final MergeJoinOperator mMergeOperator;
  
  private final HashMap<CFAEdge, EdgeElement> mElements;
  
  private final Annotations mAnnotations;
  
  public EdgeVisitCPA(Annotations pAnnotations) {
    mDomain = new FlatLatticeDomain(TopElement.getInstance(), BottomElement.getInstance());
    mPrecision = SingletonPrecision.getInstance();
    mPrecisionAdjustment = StaticPrecisionAdjustment.getInstance();
    mMergeOperator = new MergeJoinOperator(mDomain.getJoinOperator());
    
    mElements = new HashMap<CFAEdge, EdgeElement>();
    
    for (CFAEdge lEdge : pAnnotations.getCFAEdges()) {
      mElements.put(lEdge, new EdgeElement(pAnnotations.getId(lEdge), pAnnotations.getAnnotations(lEdge)));
    }
    
    mAnnotations = pAnnotations;
  }
  
  public void add(CFAEdge pEdge) {
    if (!mElements.containsKey(pEdge)) {
      mElements.put(pEdge, new EdgeElement(mAnnotations.getId(pEdge), mAnnotations.getAnnotations(pEdge)));
    }
  }
  
  @Override
  /*
   * We want to use our always-stop-operator.
   */
  public StopOperator getStopOperator() {
    return mStopOperator;
  }
  
  @Override
  public NoEdgeElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    // Initially, we have not visited any CFA edge.
    return NoEdgeElement.getInstance();
  }

  @Override
  public FlatLatticeDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return mPrecision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return mTransferRelation;
  }

}
