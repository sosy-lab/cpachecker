/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.fllesh.ecp.reduced;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.fllesh.util.Automaton;

public class ObserverAutomatonTranslator {

  private static class Visitor implements ASTVisitor<Void> {
    
    private Automaton<Atom> mAutomaton = new Automaton<Atom>();
    
    private Automaton<Atom>.State mCurrentInitialState = null;
    private Automaton<Atom>.State mCurrentFinalState = null;
    
    public Visitor() {
      mCurrentInitialState = mAutomaton.getInitialState();
    }
    
    public Automaton<Atom> getAutomaton() {
      return mAutomaton;
    }
    
    private Automaton<Atom>.State getInitialState() {
      return mCurrentInitialState;
    }
    
    private Automaton<Atom>.State getFinalState() {
      return  mCurrentFinalState;
    }
    
    private void unsetFinalState() {
      mCurrentFinalState = null;
    }
    
    private boolean hasFinalState() {
      return (mCurrentFinalState != null);
    }
    
    private void setFinalState(Automaton<Atom>.State pFinalState) {
      mCurrentFinalState = pFinalState;
    }
    
    private void setInitialState(Automaton<Atom>.State pInitialState) {
      mCurrentInitialState = pInitialState;
    }
    
    public static String translate(Pattern pECP, String pAutomatonName, String pAlphaEdge, String pOmegaEdge) {
      Visitor lVisitor = new Visitor();

      pECP.accept(lVisitor);
      
      Automaton<Atom> lOriginalAutomaton = lVisitor.getAutomaton();
      
      lOriginalAutomaton.addToFinalStates(lVisitor.getFinalState());
      
      Automaton<Atom> lAutomaton = lOriginalAutomaton.getLambdaFreeAutomaton();
      
      StringWriter lResult = new StringWriter();
      PrintWriter lWriter = new PrintWriter(lResult);

      Map<Automaton<Atom>.State, String> lStateNames = new HashMap<Automaton<Atom>.State, String>();
      
      /**
       * Create initial state that checks for passing of alpha edge (call of main function from wrapper)
       */
      lWriter.println("AUTOMATON " + pAutomatonName);
      lWriter.println("INITIAL STATE Init;");
      lWriter.println();
      lWriter.println("STATE Init:");
      lWriter.println("  CHECK(edgevisit(\"" + pAlphaEdge + "\")) -> GOTO " + getStateIdentifier(lAutomaton.getInitialState(), lStateNames) + ";");
      lWriter.println("  !CHECK(edgevisit(\"" + pAlphaEdge + "\")) -> GOTO Init;");
      lWriter.println();

      for (Automaton<Atom>.State lState : lAutomaton.getStates()) {
        lWriter.println("STATE NONDET " + getStateIdentifier(lState, lStateNames) + ":");

        for (Automaton<Atom>.Edge lOutgoingEdge : lAutomaton.getOutgoingEdges(lState)) {
          Automaton<Atom>.State lTarget = lOutgoingEdge.getTarget();
          lWriter.println("  CHECK(edgevisit(\"" + lOutgoingEdge.getLabel().getIdentifier() + "\")) -> GOTO " + getStateIdentifier(lTarget, lStateNames) + ";");
        }

        if (lAutomaton.getFinalStates().contains(lState)) {
          lWriter.println("  CHECK(edgevisit(\"" + pOmegaEdge + "\")) -> GOTO Accept;");
        }
        
        lWriter.println("  TRUE -> STOP;");
        
        lWriter.println();
      }
      
      // add accepting state
      lWriter.println("STATE Accept:");
      // we stay in the accepting state
      lWriter.println("  TRUE -> GOTO Accept;");
      
      lWriter.println("END AUTOMATON");
      return lResult.toString();
    }
    
    public static String getStateIdentifier(Automaton<Atom>.State pState, Map<Automaton<Atom>.State, String> pIdentifiers) {
      if (pIdentifiers.containsKey(pState)) {
        return pIdentifiers.get(pState);
      }
      
      int lNextIndex = pIdentifiers.size();
      
      String lIdentifier = "State" + lNextIndex;
      
      pIdentifiers.put(pState, lIdentifier);
      
      return lIdentifier;
    }

    @Override
    public Void visit(Atom pAtom) {
      if (!hasFinalState()) {
        Automaton<Atom>.State lFinalState = mAutomaton.createState();
        setFinalState(lFinalState);
      }
      
      mAutomaton.createEdge(getInitialState(), getFinalState(), pAtom);
      
      return null;
    }

    @Override
    public Void visit(Concatenation pConcatenation) {
      pConcatenation.getFirstSubpattern().accept(this);
      
      Automaton<Atom>.State lInitialState = getInitialState();
      Automaton<Atom>.State lFinalState = getFinalState();
      
      setInitialState(lFinalState);
      unsetFinalState();
      
      pConcatenation.getSecondSubpattern().accept(this);
      
      setInitialState(lInitialState);
      
      return null;
    }

    @Override
    public Void visit(Repetition pRepetition) {
      Automaton<Atom>.State lInitialState = getInitialState();
      
      Automaton<Atom>.State lFinalState;
      
      if (hasFinalState()) {
        lFinalState = getFinalState();
        if (!lFinalState.equals(lInitialState)) {
          mAutomaton.createLambdaEdge(lInitialState, lFinalState);
        }
      }
      else {
        lFinalState = lInitialState;
      }
      
      setFinalState(lInitialState);
      pRepetition.getSubpattern().accept(this);
      setFinalState(lFinalState);
      
      return null;
    }

    @Override
    public Void visit(Union pUnion) {
      pUnion.getFirstSubpattern().accept(this);
      pUnion.getSecondSubpattern().accept(this);
      
      return null;
    }

  }
  
  public static String translate(Pattern pECP, String pAutomatonName, String pAlphaEdge, String pOmegaEdge) {
    return Visitor.translate(pECP, pAutomatonName, pAlphaEdge, pOmegaEdge);
  }

}
