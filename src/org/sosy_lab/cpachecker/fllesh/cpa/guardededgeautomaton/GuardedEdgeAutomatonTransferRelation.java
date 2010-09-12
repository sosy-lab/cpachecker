package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.AutomatonPrettyPrinter;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.util.Automaton;

public class GuardedEdgeAutomatonTransferRelation implements TransferRelation {

  private final AbstractElement mTopElement;
  private final AbstractElement mBottomElement;
  private final Automaton<GuardedEdgeLabel> mAutomaton;
  protected final AutomatonPrettyPrinter mPrettyPrinter;
  
  private String mInputFunctionName;
  private Map<CallToReturnEdge, CFAEdge> mReplacedEdges;
  
  public GuardedEdgeAutomatonTransferRelation(GuardedEdgeAutomatonDomain pDomain, Automaton<GuardedEdgeLabel> pAutomaton) {
    mTopElement = pDomain.getTopElement();
    mBottomElement = pDomain.getBottomElement();
    mAutomaton = pAutomaton;
    
    mPrettyPrinter = new AutomatonPrettyPrinter();
    //System.out.println(mPrettyPrinter.printPretty(mAutomaton));
  }
  
  public GuardedEdgeAutomatonTransferRelation(GuardedEdgeAutomatonDomain pDomain, Automaton<GuardedEdgeLabel> pAutomaton, String pInputFunctionName, Map<CallToReturnEdge, CFAEdge> pReplacedEdges) {
    mTopElement = pDomain.getTopElement();
    mBottomElement = pDomain.getBottomElement();
    mAutomaton = pAutomaton;
    
    mPrettyPrinter = new AutomatonPrettyPrinter();
    System.out.println(mPrettyPrinter.printPretty(mAutomaton));
    
    if (pInputFunctionName == null) {
      throw new IllegalArgumentException();
    }
    
    mInputFunctionName = pInputFunctionName;
    mReplacedEdges = pReplacedEdges;
  }
  
  public void setInputFunctionName(String pInputFunctionName, Map<CallToReturnEdge, CFAEdge> pReplacedEdges) {
    if (pInputFunctionName == null) {
      throw new IllegalArgumentException();
    }
    
    if (pReplacedEdges == null) {
      throw new IllegalArgumentException();
    }
    
    if (mInputFunctionName != null) {
      throw new UnsupportedOperationException();
    }
    
    mInputFunctionName = pInputFunctionName;
    mReplacedEdges = pReplacedEdges;
  }
  
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    
    if (pElement.equals(mTopElement)) {
      return Collections.singleton(mTopElement);
    }
    
    if (pElement.equals(mBottomElement)) {
      return Collections.emptySet();
    }
    
    if (pElement instanceof GuardedEdgeAutomatonPredicateElement) {
      throw new IllegalArgumentException();
    }
    
    if (mInputFunctionName != null) {
      CFANode lPredecessor = pCfaEdge.getPredecessor();
      CFANode lSuccessor = pCfaEdge.getSuccessor();
      
      if (lPredecessor.getFunctionName().equals(mInputFunctionName)
          && !lSuccessor.getFunctionName().equals(mInputFunctionName)) {
        if (!pCfaEdge.getEdgeType().equals(CFAEdgeType.ReturnEdge)) {
          throw new RuntimeException();
        }
        
        // now we have to simulate one step in the automaton
        pCfaEdge = mReplacedEdges.get(lSuccessor.getEnteringSummaryEdge());
        
        if (pCfaEdge == null) {
          throw new RuntimeException();
        }
      }
      else if (lPredecessor.getFunctionName().equals(mInputFunctionName) 
          || lSuccessor.getFunctionName().equals(mInputFunctionName)) {
        return Collections.singleton(pElement);
      }
    }
    
    Set<GuardedEdgeAutomatonStateElement> lSuccessors = new HashSet<GuardedEdgeAutomatonStateElement>();
    
    GuardedEdgeAutomatonStandardElement lCurrentElement = (GuardedEdgeAutomatonStandardElement)pElement;
    
    for (Automaton<GuardedEdgeLabel>.Edge lOutgoingEdge : mAutomaton.getOutgoingEdges(lCurrentElement.getAutomatonState())) {
      GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();
      if (lLabel.contains(pCfaEdge)) {
        lSuccessors.add(GuardedEdgeAutomatonStateElement.create(lOutgoingEdge, mAutomaton, mPrettyPrinter.printPretty(lOutgoingEdge)));
      }
    }
    
    return lSuccessors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    
    if (pElement instanceof GuardedEdgeAutomatonPredicateElement) {
      return Collections.singleton(((GuardedEdgeAutomatonPredicateElement)pElement).getStandardElement());
    }
    
    return null;
  }

}
