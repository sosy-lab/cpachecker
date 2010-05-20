package org.sosy_lab.cpachecker.fllesh.ecp.translators;

import org.sosy_lab.cpachecker.fllesh.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPNodeSet;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPRepetition;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPUnion;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPVisitor;
import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.ecp.reduced.Automaton;

public class Translator {
  
  public static Automaton<GuardedLabel> translate(ElementaryCoveragePattern pPattern) {
    Visitor lVisitor = new Visitor();
    
    pPattern.accept(lVisitor);
    
    lVisitor.getAutomaton().addToFinalStates(lVisitor.getFinalState());
    
    return lVisitor.getAutomaton();
  }
  
  private static class Visitor implements ECPVisitor<Void> {

    private Automaton<GuardedLabel> mAutomaton;

    private Automaton<GuardedLabel>.State mInitialState;
    private Automaton<GuardedLabel>.State mFinalState;
    
    public Visitor() {
      mAutomaton = new Automaton<GuardedLabel>();
      setInitialState(mAutomaton.getInitialState());
      setFinalState(mAutomaton.createState());
    }
    
    public Automaton<GuardedLabel> getAutomaton() {
      return mAutomaton;
    }
    
    public Automaton<GuardedLabel>.State getInitialState() {
      return mInitialState;
    }
    
    public Automaton<GuardedLabel>.State getFinalState() {
      return mFinalState;
    }
    
    public void setInitialState(Automaton<GuardedLabel>.State pInitialState) {
      mInitialState = pInitialState;
    }
    
    public void setFinalState(Automaton<GuardedLabel>.State pFinalState) {
      mFinalState = pFinalState;
    }
    
    @Override
    public Void visit(ECPEdgeSet pEdgeSet) {
      mAutomaton.createEdge(getInitialState(), getFinalState(), new GuardedEdgeLabel(pEdgeSet));
      
      return null;
    }

    @Override
    public Void visit(ECPNodeSet pNodeSet) {
      mAutomaton.createEdge(getInitialState(), getFinalState(), new GuardedLambdaLabel(pNodeSet));
      
      return null;
    }

    @Override
    public Void visit(ECPPredicate pPredicate) {
      mAutomaton.createEdge(getInitialState(), getFinalState(), new GuardedLambdaLabel(pPredicate));
      
      return null;
    }

    @Override
    public Void visit(ECPConcatenation pConcatenation) {
      if (pConcatenation.isEmpty()) {
        mAutomaton.createEdge(getInitialState(), getFinalState(), new GuardedLambdaLabel());
      }
      else {
        Automaton<GuardedLabel>.State lTmpInitialState = getInitialState();
        Automaton<GuardedLabel>.State lTmpFinalState = getFinalState();
        
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
        mAutomaton.createEdge(getInitialState(), getFinalState(), new GuardedLambdaLabel());
      }
      else if (pUnion.size() == 1) {
        pUnion.get(0).accept(this);
      }
      else {
        Automaton<GuardedLabel>.State lTmpInitialState = getInitialState();
        
        for (ElementaryCoveragePattern lSubpattern : pUnion) {
          setInitialState(mAutomaton.createState());
          
          mAutomaton.createEdge(lTmpInitialState, getInitialState(), new GuardedLambdaLabel());
          
          lSubpattern.accept(this);
        }
        
        setInitialState(lTmpInitialState);
      }
      
      return null;
    }

    @Override
    public Void visit(ECPRepetition pRepetition) {
      Automaton<GuardedLabel>.State lTmpInitialState = getInitialState();
      Automaton<GuardedLabel>.State lTmpFinalState = getFinalState();
      
      mAutomaton.createEdge(lTmpInitialState, lTmpFinalState, new GuardedLambdaLabel());
      
      setInitialState(mAutomaton.createState());
      setFinalState(lTmpInitialState);
      
      mAutomaton.createEdge(lTmpInitialState, getInitialState(), new GuardedLambdaLabel());
      
      pRepetition.getSubpattern().accept(this);
      
      setInitialState(lTmpInitialState);
      setFinalState(lTmpFinalState);
      
      return null;
    }
    
  }
  
}
