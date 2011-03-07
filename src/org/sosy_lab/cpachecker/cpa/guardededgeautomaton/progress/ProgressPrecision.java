package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.progress;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;

public class ProgressPrecision implements Precision {

   private final Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> mProgressEdges;
   
   private ProgressPrecision(Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> pProgressEdges) {
     mProgressEdges = pProgressEdges;
   }
   
   public ProgressPrecision(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
     mProgressEdges = new HashSet<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>();
     
     for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lTransition : pAutomaton.getEdges()) {
       if (lTransition.getSource() != lTransition.getTarget()) {
         mProgressEdges.add(lTransition);
       }
     }
   }
   
   public boolean isEmpty() {
     return mProgressEdges.isEmpty();
   }
   
   public boolean isProgress(NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge pTransition) {
     return mProgressEdges.contains(pTransition);
   }
  
   public ProgressPrecision remove(NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge pTransition) {
     if (mProgressEdges.contains(pTransition)) {
       ProgressPrecision lPrecision = new ProgressPrecision(new HashSet<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>(mProgressEdges));
       lPrecision.mProgressEdges.remove(pTransition);
       
       return lPrecision;
     }
     else {
       return this;
     }
   }
   
   @Override
   public int hashCode() {
     return mProgressEdges.hashCode();
   }
   
   @Override
   public boolean equals(Object pOther) {
     if (pOther == this) {
       return true;
     }
     
     if (pOther == null) {
       return false;
     }
     
     if (pOther.getClass().equals(getClass())) {
       ProgressPrecision lPrecision = (ProgressPrecision)pOther;
       
       return lPrecision.mProgressEdges.equals(mProgressEdges);
     }
     
     return false;
   }
   
   @Override
   public String toString() {
     return mProgressEdges.toString();
   }
   
}
