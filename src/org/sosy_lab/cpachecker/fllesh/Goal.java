package org.sosy_lab.cpachecker.fllesh;

import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

public class Goal {
  
  private ElementaryCoveragePattern mPattern;
  private NondeterministicFiniteAutomaton<GuardedEdgeLabel> mAutomaton;
  private boolean mContainsPredicates;
  
  public Goal(ElementaryCoveragePattern pPattern, GuardedEdgeLabel pAlphaLabel, GuardedEdgeLabel pInverseAlphaLabel, GuardedEdgeLabel pOmegaLabel) {
    mPattern = pPattern;
    mAutomaton = ToGuardedAutomatonTranslator.toAutomaton(mPattern, pAlphaLabel, pInverseAlphaLabel, pOmegaLabel);
    
    mContainsPredicates = false;
    
    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : mAutomaton.getEdges()) {
      // pAutomaton only contains predicates as guards anymore (by construction)
      if (lEdge.getLabel().hasGuards()) {
        mContainsPredicates = true;
      }
    }
  }
  
  public ElementaryCoveragePattern getPattern() {
    return mPattern;
  }
  
  public NondeterministicFiniteAutomaton<GuardedEdgeLabel> getAutomaton() {
    return mAutomaton;
  }
  
  public boolean containsPredicates() {
    return mContainsPredicates;
  }
  
}
