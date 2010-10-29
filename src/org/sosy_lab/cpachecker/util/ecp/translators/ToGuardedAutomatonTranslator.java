package org.sosy_lab.cpachecker.util.ecp.translators;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.util.ecp.ECPAtom;
import org.sosy_lab.cpachecker.util.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.ECPGuard;
import org.sosy_lab.cpachecker.util.ecp.ECPNodeSet;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.util.ecp.ECPRepetition;
import org.sosy_lab.cpachecker.util.ecp.ECPUnion;
import org.sosy_lab.cpachecker.util.ecp.ECPVisitor;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

public class ToGuardedAutomatonTranslator {
  
  public static NondeterministicFiniteAutomaton<GuardedEdgeLabel> toAutomaton(ElementaryCoveragePattern pPattern, GuardedEdgeLabel pAlphaLabel, GuardedEdgeLabel pInverseAlphaLabel, GuardedEdgeLabel pOmegaLabel) {
    NondeterministicFiniteAutomaton<GuardedLabel> lAutomaton1 = translate(pPattern);
    
    NondeterministicFiniteAutomaton<GuardedLabel> lAutomaton2 = removeLambdaEdges(lAutomaton1, pAlphaLabel, pOmegaLabel);
    
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton3 = removeNodeSetGuards(lAutomaton2);
    
    lAutomaton3.createEdge(lAutomaton3.getInitialState(), lAutomaton3.getInitialState(), pInverseAlphaLabel);
    
    // TODO do we need that?
    // not really --- analysis stops as soon as an omega edge is passed
    /*for (Automaton.State lFinalState : lAutomaton3.getFinalStates()) {
      lAutomaton3.createEdge(lFinalState, lFinalState, AllCFAEdgesGuardedEdgeLabel.getInstance());
    }*/
    
    return lAutomaton3;
  }
  
  public static NondeterministicFiniteAutomaton<GuardedLabel> translate(ElementaryCoveragePattern pPattern) {
    Visitor lVisitor = new Visitor();
    
    pPattern.accept(lVisitor);
    
    lVisitor.getAutomaton().addToFinalStates(lVisitor.getFinalState());
    
    return lVisitor.getAutomaton();
  }
  
  public static NondeterministicFiniteAutomaton<GuardedLabel> removeLambdaEdges(NondeterministicFiniteAutomaton<GuardedLabel> pAutomaton, GuardedEdgeLabel pAlphaLabel, GuardedEdgeLabel pOmegaLabel) {
    /** first we augment the given automaton with the alpha and omega edge */  
    // TODO move into separate (private) method
    NondeterministicFiniteAutomaton.State lNewInitialState = pAutomaton.createState();
    NondeterministicFiniteAutomaton<GuardedLabel>.Edge lInitialEdge = pAutomaton.createEdge(lNewInitialState, pAutomaton.getInitialState(), pAlphaLabel);
    pAutomaton.setInitialState(lNewInitialState);
    
    NondeterministicFiniteAutomaton.State lNewFinalState = pAutomaton.createState();
    
    for (NondeterministicFiniteAutomaton.State lFinalState : pAutomaton.getFinalStates()) {
      pAutomaton.createEdge(lFinalState, lNewFinalState, pOmegaLabel);
    }
    
    pAutomaton.setFinalStates(Collections.singleton(lNewFinalState));
    
    /** now we remove guarded lambda edges */
    
    NondeterministicFiniteAutomaton<GuardedLabel> lAutomaton = new NondeterministicFiniteAutomaton<GuardedLabel>();
    Map<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lStateMap = new HashMap<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>();
    lStateMap.put(lNewInitialState, lAutomaton.getInitialState());
    
    List<NondeterministicFiniteAutomaton<GuardedLabel>.Edge> lWorklist = new LinkedList<NondeterministicFiniteAutomaton<GuardedLabel>.Edge>();
    lWorklist.add(lInitialEdge);
    
    Set<NondeterministicFiniteAutomaton<GuardedLabel>.Edge> lReachedEdges = new HashSet<NondeterministicFiniteAutomaton<GuardedLabel>.Edge>();
    
    while (!lWorklist.isEmpty()) {
      NondeterministicFiniteAutomaton<GuardedLabel>.Edge lCurrentEdge = lWorklist.remove(0);
    
      if (lReachedEdges.contains(lCurrentEdge)) {
        continue;
      }
      
      lReachedEdges.add(lCurrentEdge);
      
      GuardedState lInitialGuardedState = new GuardedState(lCurrentEdge.getTarget(), lCurrentEdge.getLabel().getGuards());
      
      /** determine the lambda successors */
      // TODO refactor into distinguished method
      List<GuardedState> lStatesWorklist = new LinkedList<GuardedState>();
      lStatesWorklist.add(lInitialGuardedState);
      
      Set<GuardedState> lReachedStates = new HashSet<GuardedState>();
      
      while (!lStatesWorklist.isEmpty()) {
        GuardedState lCurrentState = lStatesWorklist.remove(0);
        
        boolean lIsCovered = false;
        
        for (GuardedState lGuardedState : lReachedStates) {
          if (lGuardedState.covers(lCurrentState)) {
            lIsCovered = true;
            
            break;
          }
        }
        
        if (lIsCovered) {
          continue;
        }
        
        lReachedStates.add(lCurrentState);
        
        for (NondeterministicFiniteAutomaton<GuardedLabel>.Edge lOutgoingEdge : pAutomaton.getOutgoingEdges(lCurrentState.getState())) {
          if (lOutgoingEdge.getLabel() instanceof GuardedLambdaLabel) {
            GuardedState lNewState = new GuardedState(lOutgoingEdge.getTarget(), lCurrentState, lOutgoingEdge.getLabel().getGuards());
            lStatesWorklist.add(lNewState);
          }
        }
      }
      
      NondeterministicFiniteAutomaton.State lOldSource = lCurrentEdge.getSource();
      
      if (!lStateMap.containsKey(lOldSource)) {
        lStateMap.put(lOldSource, lAutomaton.createState());
      }
      
      NondeterministicFiniteAutomaton.State lSource = lStateMap.get(lOldSource);
      
      GuardedLabel lCurrentLabel = lCurrentEdge.getLabel();
      
      GuardedEdgeLabel lEdgeLabel = (GuardedEdgeLabel)lCurrentLabel;
      
      ECPEdgeSet lCurrentEdgeSet = lEdgeLabel.getEdgeSet();
      
      for (GuardedState lReachedState : lReachedStates) {
        boolean lHasNonLambdaEdge = false;
        
        // TODO create variable for lReachedState.getState()
        
        for (NondeterministicFiniteAutomaton<GuardedLabel>.Edge lOutgoingEdge : pAutomaton.getOutgoingEdges(lReachedState.getState())) {
          if (!(lOutgoingEdge.getLabel() instanceof GuardedLambdaLabel)) {
            lHasNonLambdaEdge = true;
            
            lWorklist.add(lOutgoingEdge);
          }
        }
        
        // final state has no outgoing edges
        if (pAutomaton.getOutgoingEdges(lReachedState.getState()).isEmpty()) {
          lHasNonLambdaEdge = true;
        }
        
        if (lHasNonLambdaEdge) {
          NondeterministicFiniteAutomaton.State lOldTarget = lReachedState.getState();
          
          if (!lStateMap.containsKey(lOldTarget)) {
            lStateMap.put(lOldTarget, lAutomaton.createState());
          }
          
          NondeterministicFiniteAutomaton.State lTarget = lStateMap.get(lOldTarget);
                    
          lAutomaton.createEdge(lSource, lTarget, new GuardedEdgeLabel(lCurrentEdgeSet, lReachedState.getGuards()));
        }
      }
    }
    
    for (NondeterministicFiniteAutomaton.State lFinalState : pAutomaton.getFinalStates()) {
      lAutomaton.addToFinalStates(lStateMap.get(lFinalState));
    }
    
    return lAutomaton;
  }
  
  /**
   * 
   * @param pAutomaton Automaton that contains no lambda edges.
   * @return Automaton that is only labeled with GuardedEdgeLabel objects.
   */
  public static NondeterministicFiniteAutomaton<GuardedEdgeLabel> removeNodeSetGuards(NondeterministicFiniteAutomaton<GuardedLabel> pAutomaton) {
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = new NondeterministicFiniteAutomaton<GuardedEdgeLabel>();
    
    Map<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lStateMap = new HashMap<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>();
    lStateMap.put(pAutomaton.getInitialState(), lAutomaton.getInitialState());
    
    List<NondeterministicFiniteAutomaton<GuardedLabel>.Edge> lWorklist = new LinkedList<NondeterministicFiniteAutomaton<GuardedLabel>.Edge>();
    lWorklist.addAll(pAutomaton.getOutgoingEdges(pAutomaton.getInitialState()));
    
    Set<NondeterministicFiniteAutomaton<GuardedLabel>.Edge> lReachedEdges = new HashSet<NondeterministicFiniteAutomaton<GuardedLabel>.Edge>();
    
    while (!lWorklist.isEmpty()) {
      NondeterministicFiniteAutomaton<GuardedLabel>.Edge lCurrentEdge = lWorklist.remove(0);
    
      if (lReachedEdges.contains(lCurrentEdge)) {
        continue;
      }
      
      lReachedEdges.add(lCurrentEdge);
      
      GuardedLabel lLabel = lCurrentEdge.getLabel();
      
      if (lLabel.hasGuards()) {
        ECPNodeSet lNodeSet = null;
        
        Set<ECPGuard> lRemainingGuards = new HashSet<ECPGuard>();
        
        for (ECPGuard lGuard : lLabel.getGuards()) {
          if (lGuard instanceof ECPNodeSet) {
            if (lNodeSet == null) {
              lNodeSet = (ECPNodeSet)lGuard;
            }
            else {
              lNodeSet = lNodeSet.intersect((ECPNodeSet)lGuard);
            }
          }
          else {
            lRemainingGuards.add(lGuard);
          }
        }
        
        if (lNodeSet != null) {
          // TODO move this condition upwards
          if (!lNodeSet.isEmpty()) {
            assert(lLabel instanceof GuardedEdgeLabel);
            
            GuardedEdgeLabel lEdgeLabel = (GuardedEdgeLabel)lLabel;
            
            ECPEdgeSet lCurrentEdgeSet = lEdgeLabel.getEdgeSet();
            
            Set<CFAEdge> lRemainingCFAEdges = new HashSet<CFAEdge>();
            
            for (CFAEdge lCFAEdge : lCurrentEdgeSet) {
              if (lNodeSet.contains(lCFAEdge.getSuccessor())) {
                lRemainingCFAEdges.add(lCFAEdge);
              }
            }
            
            if (!lRemainingCFAEdges.isEmpty()) {
              ECPEdgeSet lNewEdgeSet = new ECPEdgeSet(lRemainingCFAEdges);
              
              GuardedEdgeLabel lNewGuard = new GuardedEdgeLabel(lNewEdgeSet, lRemainingGuards);
              
              // add edge
              
              NondeterministicFiniteAutomaton.State lCurrentSource = lCurrentEdge.getSource();
              NondeterministicFiniteAutomaton.State lCurrentTarget = lCurrentEdge.getTarget();
              
              if (!lStateMap.containsKey(lCurrentSource)) {
                lStateMap.put(lCurrentSource, lAutomaton.createState());
              }
              
              if (!lStateMap.containsKey(lCurrentTarget)) {
                lStateMap.put(lCurrentTarget, lAutomaton.createState());
              }
              
              NondeterministicFiniteAutomaton.State lSourceState = lStateMap.get(lCurrentSource);
              NondeterministicFiniteAutomaton.State lTargetState = lStateMap.get(lCurrentTarget);
              
              lAutomaton.createEdge(lSourceState, lTargetState, lNewGuard);
            }
          }
        }
        else {
          assert(lLabel instanceof GuardedEdgeLabel);
          
          GuardedEdgeLabel lEdgeLabel = (GuardedEdgeLabel)lLabel;
          
          if (!lEdgeLabel.getEdgeSet().isEmpty()) {
            // add edge
            NondeterministicFiniteAutomaton.State lCurrentSource = lCurrentEdge.getSource();
            NondeterministicFiniteAutomaton.State lCurrentTarget = lCurrentEdge.getTarget();
            
            if (!lStateMap.containsKey(lCurrentSource)) {
              lStateMap.put(lCurrentSource, lAutomaton.createState());
            }
            
            if (!lStateMap.containsKey(lCurrentTarget)) {
              lStateMap.put(lCurrentTarget, lAutomaton.createState());
            }
            
            NondeterministicFiniteAutomaton.State lSourceState = lStateMap.get(lCurrentSource);
            NondeterministicFiniteAutomaton.State lTargetState = lStateMap.get(lCurrentTarget);
            
            lAutomaton.createEdge(lSourceState, lTargetState, lEdgeLabel);
          }
        }
      }
      else {
        assert(lLabel instanceof GuardedEdgeLabel);
        
        GuardedEdgeLabel lEdgeLabel = (GuardedEdgeLabel)lLabel;
        
        if (!lEdgeLabel.getEdgeSet().isEmpty()) {
          // add edge
          NondeterministicFiniteAutomaton.State lCurrentSource = lCurrentEdge.getSource();
          NondeterministicFiniteAutomaton.State lCurrentTarget = lCurrentEdge.getTarget();
          
          if (!lStateMap.containsKey(lCurrentSource)) {
            lStateMap.put(lCurrentSource, lAutomaton.createState());
          }
          
          if (!lStateMap.containsKey(lCurrentTarget)) {
            lStateMap.put(lCurrentTarget, lAutomaton.createState());
          }
          
          NondeterministicFiniteAutomaton.State lSourceState = lStateMap.get(lCurrentSource);
          NondeterministicFiniteAutomaton.State lTargetState = lStateMap.get(lCurrentTarget);
          
          lAutomaton.createEdge(lSourceState, lTargetState, lEdgeLabel);
        }
      }
      
      lWorklist.addAll(pAutomaton.getOutgoingEdges(lCurrentEdge.getTarget()));
    }
    
    for (NondeterministicFiniteAutomaton.State lFinalState : pAutomaton.getFinalStates()) {
      if (lStateMap.containsKey(lFinalState)) {
        lAutomaton.addToFinalStates(lStateMap.get(lFinalState));
      }
    }
    
    return lAutomaton;
  }
  
  private static class Visitor implements ECPVisitor<Void> {

    private static final Map<ECPAtom, GuardedLabel> sLabelCache = new HashMap<ECPAtom, GuardedLabel>();
    
    private NondeterministicFiniteAutomaton<GuardedLabel> mAutomaton;

    private NondeterministicFiniteAutomaton.State mInitialState;
    private NondeterministicFiniteAutomaton.State mFinalState;
    
    public Visitor(NondeterministicFiniteAutomaton<GuardedLabel> pAutomaton) {
      mAutomaton = pAutomaton;
      setInitialState(mAutomaton.getInitialState());
      setFinalState(mAutomaton.createState());
    }
    
    public Visitor() {
      this(new NondeterministicFiniteAutomaton<GuardedLabel>());
    }
    
    public NondeterministicFiniteAutomaton<GuardedLabel> getAutomaton() {
      return mAutomaton;
    }
    
    public NondeterministicFiniteAutomaton.State getInitialState() {
      return mInitialState;
    }
    
    public NondeterministicFiniteAutomaton.State getFinalState() {
      return mFinalState;
    }
    
    public void setInitialState(NondeterministicFiniteAutomaton.State pInitialState) {
      mInitialState = pInitialState;
    }
    
    public void setFinalState(NondeterministicFiniteAutomaton.State pFinalState) {
      mFinalState = pFinalState;
    }
    
    @Override
    public Void visit(ECPEdgeSet pEdgeSet) {
      GuardedLabel lLabel = sLabelCache.get(pEdgeSet);
      
      if (lLabel == null) {
        lLabel = new GuardedEdgeLabel(pEdgeSet);
        sLabelCache.put(pEdgeSet, lLabel);
      }
      
      mAutomaton.createEdge(getInitialState(), getFinalState(), lLabel);
      
      return null;
    }

    @Override
    public Void visit(ECPNodeSet pNodeSet) {
      GuardedLabel lLabel = sLabelCache.get(pNodeSet);
      
      if (lLabel == null) {
        lLabel = new GuardedLambdaLabel(pNodeSet);
        sLabelCache.put(pNodeSet, lLabel);
      }
      
      mAutomaton.createEdge(getInitialState(), getFinalState(), lLabel);
      
      return null;
    }

    @Override
    public Void visit(ECPPredicate pPredicate) {
      GuardedLabel lLabel = sLabelCache.get(pPredicate);
      
      if (lLabel == null) {
        lLabel = new GuardedLambdaLabel(pPredicate);
        sLabelCache.put(pPredicate, lLabel);
      }
      
      mAutomaton.createEdge(getInitialState(), getFinalState(), lLabel);
      
      return null;
    }

    @Override
    public Void visit(ECPConcatenation pConcatenation) {
      if (pConcatenation.isEmpty()) {
        mAutomaton.createEdge(getInitialState(), getFinalState(), GuardedLambdaLabel.UNGUARDED_LAMBDA_LABEL);
      }
      else {
        NondeterministicFiniteAutomaton.State lTmpInitialState = getInitialState();
        NondeterministicFiniteAutomaton.State lTmpFinalState = getFinalState();
        
        for (int i = 0; i < pConcatenation.size(); i++) {
          ElementaryCoveragePattern lSubpattern = pConcatenation.get(i);
          
          if (i > 0) {
            // use final state from before
            setInitialState(getFinalState());
          }
          
          if (i == pConcatenation.size() - 1) {
            // use lTmpFinalState
            setFinalState(lTmpFinalState);
          }
          else {
            // create new final state
            setFinalState(mAutomaton.createState());
          }
          
          lSubpattern.accept(this);
        }
        
        setInitialState(lTmpInitialState);
      }
      
      return null;
    }

    @Override
    public Void visit(ECPUnion pUnion) {
      if (pUnion.isEmpty()) {
        mAutomaton.createEdge(getInitialState(), getFinalState(), GuardedLambdaLabel.UNGUARDED_LAMBDA_LABEL);
      }
      else if (pUnion.size() == 1) {
        pUnion.get(0).accept(this);
      }
      else {
        NondeterministicFiniteAutomaton.State lTmpInitialState = getInitialState();
        
        for (ElementaryCoveragePattern lSubpattern : pUnion) {
          setInitialState(mAutomaton.createState());
          
          mAutomaton.createEdge(lTmpInitialState, getInitialState(), GuardedLambdaLabel.UNGUARDED_LAMBDA_LABEL);
          
          lSubpattern.accept(this);
        }
        
        setInitialState(lTmpInitialState);
      }
      
      return null;
    }

    @Override
    public Void visit(ECPRepetition pRepetition) {
      NondeterministicFiniteAutomaton.State lTmpInitialState = getInitialState();
      NondeterministicFiniteAutomaton.State lTmpFinalState = getFinalState();
      
      mAutomaton.createEdge(lTmpInitialState, lTmpFinalState, GuardedLambdaLabel.UNGUARDED_LAMBDA_LABEL);
      
      setInitialState(mAutomaton.createState());
      setFinalState(lTmpInitialState);
      
      mAutomaton.createEdge(lTmpInitialState, getInitialState(), GuardedLambdaLabel.UNGUARDED_LAMBDA_LABEL);
      
      pRepetition.getSubpattern().accept(this);
      
      setInitialState(lTmpInitialState);
      setFinalState(lTmpFinalState);
      
      return null;
    }
    
  }
  
}
