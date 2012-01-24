/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ecp.translators;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.ECPGuard;
import org.sosy_lab.cpachecker.util.ecp.ECPNodeSet;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;

public class AutomatonPrettyPrinter {

  public static String print(NondeterministicFiniteAutomaton<? extends GuardedLabel> pAutomaton) {
    AutomatonPrettyPrinter lPrettyPrinter = new AutomatonPrettyPrinter();

    return lPrettyPrinter.printPretty(pAutomaton);
  }

  private Map<NondeterministicFiniteAutomaton.State, String> mStateIds;
  private Map<ECPEdgeSet, String> mEdgeSetIds;
  private Map<ECPNodeSet, String> mNodeSetIds;
  private Visitor mVisitor;

  public AutomatonPrettyPrinter() {
    mStateIds = new HashMap<NondeterministicFiniteAutomaton.State, String>();
    mEdgeSetIds = new HashMap<ECPEdgeSet, String>();
    mNodeSetIds = new HashMap<ECPNodeSet, String>();
    mVisitor = new Visitor();
  }

  private String getId(NondeterministicFiniteAutomaton.State pState) {
    if (!mStateIds.containsKey(pState)) {
      mStateIds.put(pState, "S" + mStateIds.size());
    }

    return mStateIds.get(pState);
  }

  private String getId(ECPEdgeSet pEdgeSet) {
    if (!mEdgeSetIds.containsKey(pEdgeSet)) {
      mEdgeSetIds.put(pEdgeSet, "E" + mEdgeSetIds.size());
    }

    return mEdgeSetIds.get(pEdgeSet);
  }

  private String getId(ECPNodeSet pNodeSet) {
    if (!mNodeSetIds.containsKey(pNodeSet)) {
      mNodeSetIds.put(pNodeSet, "N" + mNodeSetIds.size());
    }

    return mNodeSetIds.get(pNodeSet);
  }

  public String printPretty(NondeterministicFiniteAutomaton<? extends GuardedLabel>.Edge pEdge) {
    return printPretty(pEdge.getSource()) + " -[" + pEdge.getLabel().accept(mVisitor) + "]> " + printPretty(pEdge.getTarget());
  }

  public String printPretty(NondeterministicFiniteAutomaton.State pState) {
    return getId(pState);
  }

  public String printPretty(NondeterministicFiniteAutomaton<? extends GuardedLabel> pAutomaton) {
    StringBuffer lBuffer = new StringBuffer();

    boolean lIsFirst = true;

    lBuffer.append("States: {");
    for (NondeterministicFiniteAutomaton.State lState : pAutomaton.getStates()) {
      if (lIsFirst) {
        lIsFirst = false;
      }
      else {
        lBuffer.append(", ");
      }
      lBuffer.append(getId(lState));
    }
    lBuffer.append("}\n");

    lBuffer.append("Initial State: ");
    lBuffer.append(getId(pAutomaton.getInitialState()));
    lBuffer.append("\n");

    lBuffer.append("Final States: {");

    lIsFirst = true;

    for (NondeterministicFiniteAutomaton.State lFinalState : pAutomaton.getFinalStates()) {
      if (lIsFirst) {
        lIsFirst = false;
      }
      else {
        lBuffer.append(", ");
      }
      lBuffer.append(getId(lFinalState));
    }
    lBuffer.append("}\n");



    StringBuffer lTmpBuffer = new StringBuffer();

    for (NondeterministicFiniteAutomaton<? extends GuardedLabel>.Edge lEdge : pAutomaton.getEdges()) {
      //lTmpBuffer.append(getId(lEdge.getSource()) + " -[" + lEdge.getLabel().accept(mVisitor) + "]> " + getId(lEdge.getTarget()));
      lTmpBuffer.append(printPretty(lEdge));
      lTmpBuffer.append("\n");
    }

    for (Map.Entry<ECPNodeSet, String> lEntry : mNodeSetIds.entrySet()) {
      lBuffer.append(lEntry.getValue() + ": " + lEntry.getKey().toString());
      lBuffer.append("\n");
    }

    for (Map.Entry<ECPEdgeSet, String> lEntry : mEdgeSetIds.entrySet()) {
      lBuffer.append(lEntry.getValue() + ": " + lEntry.getKey().toString());
      lBuffer.append("\n");
    }

    lBuffer.append(lTmpBuffer);

    return lBuffer.toString();
  }

  private class Visitor implements GuardedLabelVisitor<String> {
    @Override
    public String visit(GuardedLambdaLabel pLabel) {
      if (pLabel.hasGuards()) {

        StringBuffer lGuardBuffer = new StringBuffer();

        lGuardBuffer.append("[");

        boolean lIsFirst = true;

        for (ECPGuard lGuard : pLabel.getGuards()) {
          if (lIsFirst) {
            lIsFirst = false;
          }
          else {
            lGuardBuffer.append(", ");
          }

          if (lGuard instanceof ECPPredicate) {
            lGuardBuffer.append(lGuard.toString());
          }
          else {
            assert(lGuard instanceof ECPNodeSet);

            ECPNodeSet lNodeSet = (ECPNodeSet)lGuard;

            lGuardBuffer.append(getId(lNodeSet));
          }
        }

        lGuardBuffer.append("]");

        return "Lambda " + lGuardBuffer.toString();
      }
      else {
        return "Lambda";
      }
    }

    @Override
    public String visit(GuardedEdgeLabel pLabel) {

      if (pLabel instanceof InverseGuardedEdgeLabel) {
        return visitInverseGuardedEdgeLabel((InverseGuardedEdgeLabel)pLabel);
      }

      if (pLabel instanceof AllCFAEdgesGuardedEdgeLabel) {
        return "TRUE";
      }

      if (pLabel.hasGuards()) {
        StringBuffer lGuardBuffer = new StringBuffer();

        lGuardBuffer.append("[");

        boolean lIsFirst = true;

        for (ECPGuard lGuard : pLabel.getGuards()) {
          if (lIsFirst) {
            lIsFirst = false;
          }
          else {
            lGuardBuffer.append(", ");
          }

          if (lGuard instanceof ECPPredicate) {
            lGuardBuffer.append(lGuard.toString());
          }
          else {
            assert(lGuard instanceof ECPNodeSet);

            ECPNodeSet lNodeSet = (ECPNodeSet)lGuard;

            lGuardBuffer.append(getId(lNodeSet));
          }
        }

        lGuardBuffer.append("]");

        return getId(pLabel.getEdgeSet()) + " " + lGuardBuffer.toString();
      }
      else {
        return getId(pLabel.getEdgeSet());
      }
    }

    private String visitInverseGuardedEdgeLabel(InverseGuardedEdgeLabel pLabel) {
      return ("!" + pLabel.getInvertedLabel().accept(this));
    }
  }
}
