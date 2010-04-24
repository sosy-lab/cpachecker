package fllesh.ecp.reduced;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import common.Triple;

public class ObserverAutomatonTranslator {

  private static class Visitor implements ASTVisitor<Integer> {

    private Integer mCurrentInitialState;
    private Integer mInitialState;
    
    private Map<Integer, Set<Triple<Integer, Integer, Atom>>> mOutgoingEdges;
    private Map<Integer, Set<Triple<Integer, Integer, Atom>>> mIncomingEdges;
    
    private int mEdgeIndex;
    
    public Visitor() {
      mEdgeIndex = 0;
      mOutgoingEdges = new HashMap<Integer, Set<Triple<Integer, Integer, Atom>>>();
      mIncomingEdges = new HashMap<Integer, Set<Triple<Integer, Integer, Atom>>>();
      
      setCurrentInitialState(createState());
      
      mInitialState = getCurrentInitialState();
    }
    
    private Integer getInitialState() {
      return mInitialState;
    }
    
    private void setCurrentInitialState(Integer pState) {
      mCurrentInitialState = pState;
    }
    
    private Integer getCurrentInitialState() {
      return mCurrentInitialState;
    }
    
    private Integer createState() {
      int lNewId = mOutgoingEdges.size();
      
      mOutgoingEdges.put(lNewId, new HashSet<Triple<Integer, Integer, Atom>>());
      mIncomingEdges.put(lNewId, new HashSet<Triple<Integer, Integer, Atom>>());
      
      return lNewId;
    }
    
    private Integer createEdge(Integer pState1, Integer pState2, Atom pAtom) {
      Integer lNewId = mEdgeIndex;
      mEdgeIndex++;
      
      Triple<Integer, Integer, Atom> lEdge = new Triple<Integer, Integer, Atom>(pState1, pState2, pAtom);
      
      mOutgoingEdges.get(pState1).add(lEdge);
      mIncomingEdges.get(pState2).add(lEdge);
      
      return lNewId;
    }
    
    private Set<Triple<Integer, Integer, Atom>> getOutgoingEdges(Integer pState) {
      return mOutgoingEdges.get(pState);
    }
    
    private Set<Triple<Integer, Integer, Atom>> getIncomingEdges(Integer pState) {
      return mIncomingEdges.get(pState);
    }
    
    @Override
    public Integer visit(Atom pAtom) {
      Integer lFinalState = createState();
      
      createEdge(getCurrentInitialState(), lFinalState, pAtom);
      
      return lFinalState;
    }

    @Override
    public Integer visit(Concatenation pConcatenation) {
      Integer lFirstFinalState = pConcatenation.getFirstSubpattern().accept(this);
      
      Integer lSecondInitialState = createState();
      
      setCurrentInitialState(lSecondInitialState);
      
      Integer lSecondFinalState = pConcatenation.getSecondSubpattern().accept(this);

      for (Triple<Integer, Integer, Atom> lOutgoingEdge : getOutgoingEdges(lSecondInitialState)) {
        createEdge(lFirstFinalState, lOutgoingEdge.getSecond(), lOutgoingEdge.getThird());
      }
      
      return lSecondFinalState;
    }

    @Override
    public Integer visit(Repetition pRepetition) {
      Integer lInitialState = getCurrentInitialState();
      
      Integer lSubInitialState = this.createState();
      this.setCurrentInitialState(lSubInitialState);
      
      Integer lSubFinalState = pRepetition.getSubpattern().accept(this);
      
      for (Triple<Integer, Integer, Atom> lOutgoingEdge : getOutgoingEdges(lSubInitialState)) {
        createEdge(lInitialState, lOutgoingEdge.getSecond(), lOutgoingEdge.getThird());
      }
      
      for (Triple<Integer, Integer, Atom> lIncomingEdge : getIncomingEdges(lSubFinalState)) {
        createEdge(lIncomingEdge.getFirst(), lInitialState, lIncomingEdge.getThird());
      }
      
      return lInitialState;
    }

    @Override
    public Integer visit(Union pUnion) {
      
      Integer lInitialState = this.getCurrentInitialState();
      
      
      Integer lFirstInitialState = this.createState();
      this.setCurrentInitialState(lFirstInitialState);
      
      Integer lFirstFinalState = pUnion.getFirstSubpattern().accept(this);
      
      for (Triple<Integer, Integer, Atom> lOutgoingEdge : this.getOutgoingEdges(lFirstInitialState)) {
        this.createEdge(lInitialState, lOutgoingEdge.getSecond(), lOutgoingEdge.getThird());
      }
      
      
      Integer lSecondInitialState = this.createState();
      this.setCurrentInitialState(lSecondInitialState);
      
      Integer lSecondFinalState = pUnion.getSecondSubpattern().accept(this);
      
      for (Triple<Integer, Integer, Atom> lOutgoingEdge : this.getOutgoingEdges(lSecondInitialState)) {
        this.createEdge(lInitialState, lOutgoingEdge.getSecond(), lOutgoingEdge.getThird());
      }
      
      Integer lFinalState = this.createState();
      

      for (Triple<Integer, Integer, Atom> lIncomingEdge : this.getIncomingEdges(lFirstFinalState)) {
        this.createEdge(lIncomingEdge.getFirst(), lFinalState, lIncomingEdge.getThird());
      }
      
      for (Triple<Integer, Integer, Atom> lIncomingEdge : this.getIncomingEdges(lSecondFinalState)) {
        this.createEdge(lIncomingEdge.getFirst(), lFinalState, lIncomingEdge.getThird());
      }
      
      
      return lFinalState;
    }
    
    public void reduce(Integer pFinalState) {
      boolean changed; 
      
      do {
        changed = false;
        
        for (Integer lState : mOutgoingEdges.keySet()) {
          if (this.getOutgoingEdges(lState).size() == 0) {
            if (!lState.equals(pFinalState)) {
              mOutgoingEdges.remove(lState);
              
              for (Triple<Integer, Integer, Atom> lEdge : getIncomingEdges(lState)) {
                mOutgoingEdges.get(lEdge.getFirst()).remove(lEdge);
              }
              mIncomingEdges.remove(lState);
              
              changed = true;
              
              break;
            }
          }
          else if (this.getIncomingEdges(lState).size() == 0) {
            if (!lState.equals(this.getInitialState())) {
              mIncomingEdges.remove(lState);
              
              for (Triple<Integer, Integer, Atom> lEdge : getOutgoingEdges(lState)) {
                mIncomingEdges.get(lEdge.getSecond()).remove(lEdge);
              }
              mOutgoingEdges.remove(lState);
              
              changed = true;
              
              break;
            }
          }
        }
      }
      while (changed);
    }
    
  }
  
  public static String translate(Pattern pECP, String pAutomatonName, String pAlphaEdge, String pOmegaEdge) {
    Visitor lVisitor = new Visitor();
    
    Integer lFinalState = pECP.accept(lVisitor);
    
    StringWriter lResult = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lResult);
    
    lWriter.println("AUTOMATON " + pAutomatonName);
    lWriter.println("INITIAL STATE Init;");
    lWriter.println();
    lWriter.println("STATE Init:");
    lWriter.println("  CHECK(edgevisit(\"" + pAlphaEdge + "\")) -> GOTO State" + lVisitor.getInitialState() + ";");
    lWriter.println("  TRUE -> BOTTOM;");
    lWriter.println();
    
    lVisitor.reduce(lFinalState);
    
    Set<Integer> lProcessedStates = new HashSet<Integer>();
    LinkedList<Integer> lWorklist = new LinkedList<Integer>();
    
    lWorklist.add(lVisitor.getInitialState());
    
    while (!lWorklist.isEmpty()) {
      Integer lState = lWorklist.pop();
      
      if (lProcessedStates.contains(lState)) {
        continue;
      }

      lProcessedStates.add(lState);

      lWriter.println("STATE State" + lState + ":");
      
      for (Triple<Integer, Integer, Atom> lOutgoingEdge : lVisitor.getOutgoingEdges(lState)) {
        Integer lTarget = lOutgoingEdge.getSecond();
        
        lWorklist.addLast(lTarget);
        
        lWriter.println("  CHECK(edgevisit(\"" + lOutgoingEdge.getThird().getIdentifier() + "\")) -> GOTO " + lTarget + ";");
      }
      
      if (lState.equals(lFinalState)) {
        lWriter.println("  CHECK(edgevisit(\"" + pOmegaEdge + "\")) -> ERROR;");
      }
      
      lWriter.println("  TRUE -> BOTTOM;");
      
      lWriter.println();
    }
    
    return lResult.toString();
  }
  
}
