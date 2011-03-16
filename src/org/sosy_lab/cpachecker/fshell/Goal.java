package org.sosy_lab.cpachecker.fshell;

import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

public class Goal {
  
  private ElementaryCoveragePattern mPattern;
  private NondeterministicFiniteAutomaton<GuardedEdgeLabel> mAutomaton;
  
  public Goal(ElementaryCoveragePattern pPattern, GuardedEdgeLabel pAlphaLabel, GuardedEdgeLabel pInverseAlphaLabel, GuardedEdgeLabel pOmegaLabel) {
    mPattern = pPattern;
    mAutomaton = ToGuardedAutomatonTranslator.toAutomaton(mPattern, pAlphaLabel, pInverseAlphaLabel, pOmegaLabel);
  }
  
  public ElementaryCoveragePattern getPattern() {
    return mPattern;
  }
  
  public NondeterministicFiniteAutomaton<GuardedEdgeLabel> getAutomaton() {
    return mAutomaton;
  }
  
}
