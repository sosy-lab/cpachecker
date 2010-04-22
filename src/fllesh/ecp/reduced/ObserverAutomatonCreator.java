package fllesh.ecp.reduced;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ObserverAutomatonCreator implements ASTVisitor<Void> {

  private String mCurrentInitialState;
  private String mCurrentFinalState;
  private Map<Pattern, Integer> mIds;
  private Map<String, List<String>> mTransitions;
  
  private ObserverAutomatonCreator(Map<Pattern, Integer> pIds) {
    mCurrentInitialState = getInitialState(0);
    mCurrentFinalState = "ERR";
    mIds = pIds;
    mTransitions = new HashMap<String, List<String>>();
  }
  
  private String getInitialState(Integer pId) {
    return "State_" + pId.toString();
  }
  
  private List<String> getOrCreateTransitionEntry(String pState) {
    List<String> lTransitions;
    
    if (!mTransitions.containsKey(pState)) {
      lTransitions = new LinkedList<String>();
      mTransitions.put(pState, lTransitions);
    }
    else {
      lTransitions = mTransitions.get(pState);
    }
    
    return lTransitions;
  }
  
  public static void printObserverAutomaton(Pattern pPattern, String pAutomatonId, PrintStream pWriter) {
    IdCreator lIdCreator = new IdCreator();
    Map<Pattern, Integer> lIds = pPattern.accept(lIdCreator);
    
    ObserverAutomatonCreator lAutomatonCreator = new ObserverAutomatonCreator(lIds);
    pPattern.accept(lAutomatonCreator);
    lAutomatonCreator.printObserverAutomaton(pAutomatonId, pWriter);
  }
  
  private void printObserverAutomaton(String pAutomatonId, PrintStream pWriter) {
    // print header
    pWriter.println("AUTOMATON " + pAutomatonId);
    pWriter.println("INITIAL STATE " + getInitialState(0) + ";");
    
    // print transitions
    for (Entry<String, List<String>> lEntry : mTransitions.entrySet()) {
      pWriter.println("STATE " + lEntry.getKey() + ":");
      
      for (String lTransition : lEntry.getValue()) {
        pWriter.println(lTransition);
      }
    }
  }
  
  @Override
  public Void visit(Atom pAtom) {
    List<String> lTransitions = getOrCreateTransitionEntry(mCurrentInitialState);
    lTransitions.add("CHECK(edgevisit(\"" + pAtom.getIdentifier() + "\")) -> GOTO " + mCurrentFinalState + ";");
    
    return null;
  }

  @Override
  public Void visit(Concatenation pConcatenation) {
    String lTmpFinalState = mCurrentFinalState;
    
    Pattern lFirstSubpattern = pConcatenation.getFirstSubpattern();
    Pattern lSecondSubpattern = pConcatenation.getSecondSubpattern();
    
    mCurrentFinalState = getInitialState(mIds.get(pConcatenation.getFirstSubpattern()));
    lFirstSubpattern.accept(this);
    
    String lTmpInitialState = mCurrentInitialState;
    
    mCurrentInitialState = mCurrentFinalState;
    mCurrentFinalState = lTmpFinalState;
    lSecondSubpattern.accept(this);
    
    mCurrentInitialState = lTmpInitialState;
    
    return null;
  }

  @Override
  public Void visit(Repetition pRepetition) {
    
    // add P-loop
    String lTmpFinalState = mCurrentFinalState;
    mCurrentFinalState = mCurrentInitialState;
    pRepetition.getSubpattern().accept(this);
    mCurrentFinalState = lTmpFinalState;
    
    // add lambda transition
    List<String> lTransitions;
    
    if (!mTransitions.containsKey(mCurrentInitialState)) {
      lTransitions = new LinkedList<String>();
      mTransitions.put(mCurrentInitialState, lTransitions);
    }
    else {
      lTransitions = mTransitions.get(mCurrentInitialState);
    }
    
    lTransitions.add("CHECK(edgevisit(\"L\")) -> GOTO " + mCurrentFinalState + ";");
    
    return null;
  }

  @Override
  public Void visit(Union pUnion) {
    Pattern lFirstSubpattern = pUnion.getFirstSubpattern();
    Pattern lSecondSubpattern = pUnion.getSecondSubpattern();
    
    String lFirstInitialState = getInitialState(mIds.get(lFirstSubpattern));
    String lSecondInitialState = getInitialState(mIds.get(lSecondSubpattern));
    
    String lTmpInitialState = mCurrentInitialState;
    
    mCurrentInitialState = lFirstInitialState;
    lFirstSubpattern.accept(this);
    
    mCurrentInitialState = lSecondInitialState;
    lSecondSubpattern.accept(this);
    
    mCurrentInitialState = lTmpInitialState;
    
    // add lambda transitions
    List<String> lTransitions = getOrCreateTransitionEntry(mCurrentInitialState);
    lTransitions.add("CHECK(edgevisit(\"L\")) -> GOTO " + lFirstInitialState + ";");
    lTransitions.add("CHECK(edgevisit(\"L\")) -> GOTO " + lSecondInitialState + ";");
    
    return null;
  }

}
