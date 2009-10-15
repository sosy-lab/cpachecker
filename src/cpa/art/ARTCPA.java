package cpa.art;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
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
import cpa.common.interfaces.RefinableCPA;
import cpa.common.interfaces.RefinementManager;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

public class ARTCPA implements RefinableCPA {

  private AbstractDomain abstractDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;
  private RefinementManager refinementManager; 
  private RefinableCPA wrappedCPA;

  private Set<ARTElement> covered;
  private ARTElement root;

  public ARTCPA(String mergeType, String stopType, RefinableCPA cpa) throws CPAException {
    wrappedCPA = cpa;
    abstractDomain = new ARTDomain(this);
    transferRelation = new ARTTransferRelation(abstractDomain, cpa.getTransferRelation());
    precisionAdjustment = new ARTPrecisionAdjustment();
    refinementManager = new ARTRefinementManager(wrappedCPA);
    if(mergeType.equals("sep")){
      mergeOperator = new ARTMergeSep();
    }
    else if(mergeType.equals("join")){
      mergeOperator = new ARTMergeJoin(wrappedCPA);
    }
    if(stopType.equals("sep")){
      stopOperator = new ARTStopSep(abstractDomain, wrappedCPA);
    }
    else if(stopType.equals("join")){
      throw new CPAException("Location domain elements cannot be joined");
    }
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

  public void setCovered(ARTElement e1) {
    covered.add(e1);        
  }

  public Collection<ARTElement> getCovered() {
    return covered;
  }

  public void setUncovered(ARTElement e1) {
    covered.remove(e1);
  }  

  @Override
  public AbstractElement getInitialElement (CFAFunctionDefinitionNode pNode) {
    return new ARTElement(abstractDomain, (AbstractElementWithLocation)wrappedCPA.getInitialElement(pNode), 
        null);
  }

  public Precision getInitialPrecision 
  (CFAFunctionDefinitionNode pNode) {
    return new ARTPrecision(wrappedCPA.getInitialPrecision(pNode));
  }

  public RefinableCPA getWrappedCPA(){
    return wrappedCPA;
  }

  public static ConfigurableProgramAnalysis getARTCPA 
  (CFAFunctionDefinitionNode node, ConfigurableProgramAnalysis cpa) throws CPAException{
    assert(cpa instanceof RefinableCPA);
    String[] mergeTypesArray = CPAMain.cpaConfig.getPropertiesArray("analysis.mergeOperators");
    ArrayList<String> mergeTypes = new ArrayList<String>(Arrays.asList(mergeTypesArray));
    if(mergeTypes.contains("join")){
      return new ARTCPA("join", "sep", (RefinableCPA)cpa);
    }
    else{
      return new ARTCPA("sep", "sep", (RefinableCPA)cpa);
    }
  }

  @Override
  public RefinementManager getRefinementManager() {
    return refinementManager;
  }

  public ARTElement getRoot() {
    return root;
  }

  public void setRoot(ARTElement pRoot){
    root = pRoot;
  }

  public ARTElement findHighest(ARTElement pLastElem, CFANode pLoc) {
    if (root == null) return null;

    ARTElement tempRetVal = null;

    Queue<ARTElement> toProcess =
      new ArrayDeque<ARTElement>();
    toProcess.add(pLastElem);
//  System.out.println("root is " + root);
    while (!toProcess.isEmpty()) {
      ARTElement e = toProcess.remove();
      // TODO check - bottom element
      if(e.getLocationNode() == null){
        continue;
      }
      else{
        if (e.getLocationNode().equals(pLoc)) {
          tempRetVal = e;
        }
        if (e.getParent() != null) {
          toProcess.add(e.getParent());
        }
      }
    }

    if(tempRetVal == null){
      System.out.println("ERROR, NOT FOUND: " + pLoc);
      assert(false);
    }
    return tempRetVal;

  }

}
