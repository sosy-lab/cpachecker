package cpa.art;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cmdline.CPAMain;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

public class ARTCPA implements ConfigurableProgramAnalysis {

  private final ARTDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final ConfigurableProgramAnalysis wrappedCPA;

  private final Set<ARTElement> covered;
  private ARTElement root;

  private ARTCPA(String mergeType, ConfigurableProgramAnalysis cpa) throws CPAException {
    wrappedCPA = cpa;
    abstractDomain = new ARTDomain(this);
    transferRelation = new ARTTransferRelation(abstractDomain, cpa.getTransferRelation());
    precisionAdjustment = new ARTPrecisionAdjustment();
    if(mergeType.equals("sep")){
      mergeOperator = new ARTMergeSep();
    } else if(mergeType.equals("join")){
      mergeOperator = new ARTMergeJoin(wrappedCPA);
    } else {
      throw new IllegalArgumentException();
    }
    stopOperator = new ARTStopSep(wrappedCPA);  
    covered = new HashSet<ARTElement>();
    root = null;
  }

  public AbstractDomain getAbstractDomain ()
  {
    return abstractDomain;
  }

  public TransferRelation getTransferRelation ()
  {
    return transferRelation;
  }

  public MergeOperator getMergeOperator ()
  {
    return mergeOperator;
  }

  public StopOperator getStopOperator ()
  {
    return stopOperator;
  }

  public PrecisionAdjustment getPrecisionAdjustment () {
    return precisionAdjustment;
  }

  protected void setCovered(ARTElement pElement, boolean pCovered) {
    if (pCovered) {
      covered.add(pElement);
    } else {
      covered.remove(pElement);
    }
  }
  
  public Collection<ARTElement> getCovered() {
    return Collections.unmodifiableCollection(covered);
  }

  public boolean isCovered(ARTElement pElement) {
    return covered.contains(pElement);
  }
  
  @Override
  public AbstractElement getInitialElement (CFAFunctionDefinitionNode pNode) {
    return new ARTElement(this, (AbstractElementWithLocation)wrappedCPA.getInitialElement(pNode), 
        null);
  }

  public Precision getInitialPrecision 
  (CFAFunctionDefinitionNode pNode) {
    return new ARTPrecision(wrappedCPA.getInitialPrecision(pNode));
  }

  public ConfigurableProgramAnalysis getWrappedCPA(){
    return wrappedCPA;
  }

  public static ConfigurableProgramAnalysis getARTCPA 
  (CFAFunctionDefinitionNode node, ConfigurableProgramAnalysis cpa) throws CPAException{
    String[] mergeTypesArray = CPAMain.cpaConfig.getPropertiesArray("analysis.mergeOperators");
    ArrayList<String> mergeTypes = new ArrayList<String>(Arrays.asList(mergeTypesArray));
    if(mergeTypes.contains("join")){
      return new ARTCPA("join", cpa);
    }
    else{
      return new ARTCPA("sep", cpa);
    }
  }

  public ARTElement getRoot() {
    return root;
  }

  public void setRoot(ARTElement pRoot){
    root = pRoot;
  }

  public ARTElement findHighest(ARTElement pLastElem, CFANode pLoc) throws CPAException {
    if (root == null) return null;

    ARTElement tempRetVal = null;
    
    Deque<ARTElement> workList = new ArrayDeque<ARTElement>();
    Set<ARTElement> handled = new HashSet<ARTElement>();

    workList.add(pLastElem);

    while (!workList.isEmpty()) {
      ARTElement currentElement = workList.removeFirst();
      if (!handled.add(currentElement)) {
        // currentElement was already handled
        continue;
      }
      // TODO check - bottom element
      if(currentElement.getLocationNode() == null) {
        assert false;
        continue;
      } else{
        if (currentElement.getLocationNode().equals(pLoc)) {
          tempRetVal = currentElement;
        }
        workList.addAll(currentElement.getParents());
      }
    }

    if (tempRetVal == null) {
      throw new CPAException("Inconsistent ART, did not find element for " + pLoc);
    }
    return tempRetVal;

  }

}
