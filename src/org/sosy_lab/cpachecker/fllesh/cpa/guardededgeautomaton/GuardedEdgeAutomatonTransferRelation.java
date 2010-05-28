package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.AutomatonPrettyPrinter;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.util.Automaton;

public class GuardedEdgeAutomatonTransferRelation implements TransferRelation {

  private AbstractElement mTopElement;
  private AbstractElement mBottomElement;
  private Automaton<GuardedEdgeLabel> mAutomaton;
  protected AutomatonPrettyPrinter mPrettyPrinter;
  
  public GuardedEdgeAutomatonTransferRelation(GuardedEdgeAutomatonDomain pDomain, Automaton<GuardedEdgeLabel> pAutomaton) {
    mTopElement = pDomain.getTopElement();
    mBottomElement = pDomain.getBottomElement();
    mAutomaton = pAutomaton;
    
    mPrettyPrinter = new AutomatonPrettyPrinter();
    System.out.println(mPrettyPrinter.printPretty(mAutomaton));
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
