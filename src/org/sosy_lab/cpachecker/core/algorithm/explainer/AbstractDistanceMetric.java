/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class AbstractDistanceMetric {

  private BooleanFormulaManagerView bfmgr;

  public AbstractDistanceMetric(BooleanFormulaManagerView pBfmgr) {
    this.bfmgr = pBfmgr;
  }

  /**
   * Start Method
   */
  public List<CFAEdge> startDistanceMetric(List<ARGPath> safePaths, ARGPath counterexample) {
    List<CFAEdge> ce = cleanPath(counterexample);
    List<List<CFAEdge>> paths = new ArrayList<>();
    // clean the paths from useless Statements
    for (int i = 0; i < safePaths.size(); i++) {
      paths.add(cleanPath(safePaths.get(i)));
    }

    List<CFAEdge> closestSuccessfulExecution =
        comparePaths(ce, paths, counterexample.asStatesList(), safePaths);

    return closestSuccessfulExecution;

  }

  /**
   * @param ce := Counterexample
   * @param sp := Safe paths
   */
  private List<CFAEdge> comparePaths(
      List<CFAEdge> ce,
      List<List<CFAEdge>> sp,
      List<ARGState> ce_states,
      List<ARGPath> pathsStates) {

    // Make sure that the safe path list is not empty
    assert sp.size() != 0;

    List<Integer> distances = new ArrayList<>();
    int weight_p = 1;
    int weight_unal = 2;


    // "sp" here stands for "Safe Path"
    for (int i = 0; i < sp.size(); i++) {
      // Step 1: CREATE ALIGNMENTS
      List<List<CFAEdge>> alignments = createAlignments(ce, sp.get(i));

      // Step 2: Get Differences between Predicates
      int predicate_distance =
          calculatePredicateDistance(alignments, ce_states, pathsStates.get(i).asStatesList());

      // Step 3: Get Differences between Actions
      List<CFAEdge> better_choice = (sp.get(i).size() > ce.size()) ? sp.get(i) : ce;
      int unalignedStates = getNumberOfUnalignedStates(alignments, better_choice);

      // calculate the distance
      int d = weight_p * predicate_distance + weight_unal * unalignedStates;
      distances.add(d);
    }


    // clean distance = 0
    for (int i = 0; i < distances.size(); i++) {
      if (distances.get(i) == 0) {
        distances.remove(i);
        sp.remove(i);
      }
    }

    // Make sure that distances is not empty
    assert distances.size() != 0;

    int index = 0;
    int min_dist = distances.get(0);
    for (int i = 0; i < distances.size(); i++) {
      if (distances.get(i) <= min_dist) {
        index = i;
        min_dist = distances.get(i);
      }
    }

    return sp.get(index);
  }

  private Set<BooleanFormula> splitPredicates(BooleanFormula form) {
    BooleanFormula current;
    Set<BooleanFormula> result = new HashSet<>();
    Set<BooleanFormula> modulo = new HashSet<>();
    modulo.add(form);
    Set<BooleanFormula> temp;
    Iterator<BooleanFormula> iterator;
    while (true) {
      iterator = modulo.iterator();

      if (iterator.hasNext()) {
        current = iterator.next();
        modulo.remove(current);
      } else {
        break;
      }

      if (isConj(current)) {
        temp = bfmgr.toConjunctionArgs(current, true);
        for (BooleanFormula f : temp) {
          if (isConj(f) || isDisj(f)) {
            modulo.add(f);
          } else {
            result.add(f);
          }
        }
      } else if (isDisj(current)) {
        temp = bfmgr.toDisjunctionArgs(current, true);
        for (BooleanFormula f : temp) {
          if (isConj(f) || isDisj(f)) {
            modulo.add(f);
          } else {
            result.add(f);
          }
        }
      }
    }

    return result;

  }

  private boolean isConj(BooleanFormula f) {
    Set<BooleanFormula> after = bfmgr.toConjunctionArgs(f, true);
    if (after.size() >= 2) {
      return true;
    }
    if (after.size() == 1) {
      return false;
    }
    return false;
  }


  private boolean isDisj(BooleanFormula f) {
    Set<BooleanFormula> after = bfmgr.toDisjunctionArgs(f, true);
    if (after.size() >= 2) {
      return true;
    }
    if (after.size() == 1) {
      return false;
    }

    return false;
  }

  /**
   * Calculate the Number of Unaligned States
   */
  private int getNumberOfUnalignedStates(List<List<CFAEdge>> alignments, List<CFAEdge> safePath) {
    return Math.abs((alignments.get(0).size() - safePath.size()));
  }

  /**
   * Calculates the distance of the predicates
   */
  private int calculatePredicateDistance(
      List<List<CFAEdge>> alignments,
      List<ARGState> ce_states,
      List<ARGState> pathsStates) {
    assert alignments.get(0).size() == alignments.get(1).size();
    int distance = 0;
    List<List<ARGState>> stateAlignments = new ArrayList<>();
    stateAlignments.add(new ArrayList<>());
    stateAlignments.add(new ArrayList<>());

    // First find the ARGStates that are mapped to the equivalent CFANode
    // and put them in a List

    // COMPUTATIONS FOR THE COUNTEREXAMPLE
    for (CFAEdge alignedEdge : alignments.get(0)) {
      for (ARGState ceState : ce_states) {
        if (alignedEdge.getPredecessor()
            .equals(AbstractStates.extractStateByType(ceState, AbstractStateWithLocation.class)
                .getLocationNode())) {
          stateAlignments.get(0).add(ceState);
          break;
        }
      }
    }

    // COMPUTATIONS FOR THE SAFE PATH
    for (CFAEdge alignedEdge : alignments.get(1)) {
      for (ARGState pathState : pathsStates) {
        if (alignedEdge.getPredecessor()
            .equals(AbstractStates.extractStateByType(pathState, AbstractStateWithLocation.class)
                .getLocationNode())) {
          stateAlignments.get(1).add(pathState);
          break;
        }
      }
    }

    assert stateAlignments.get(0).size() == stateAlignments.get(1).size();
    assert stateAlignments.get(0).size() == alignments.get(0).size();
    assert stateAlignments.get(1).size() == alignments.get(1).size();

    // THE alignments List has only 2 Lists with the same size
    for (int j = 0; j < stateAlignments.get(0).size(); j++) {
      Set<BooleanFormula> pred_a = splitPredicates(AbstractStates
          .extractStateByType(stateAlignments.get(0).get(j), PredicateAbstractState.class)
          .getAbstractionFormula().asFormula());
      Set<BooleanFormula> pred_b = splitPredicates(AbstractStates
          .extractStateByType(stateAlignments.get(1).get(j), PredicateAbstractState.class)
          .getAbstractionFormula().asFormula());

      for (BooleanFormula predicate : pred_a) {
        if (!pred_b.contains(predicate)) {
          distance++;
        }
      }
      for (BooleanFormula predicate : pred_b) {
        if (!pred_a.contains(predicate)) {
          distance++;
        }
      }
    }
    return distance;
  }

  /**
   * Create Alignments between CE and Safe Path
   */
  private List<List<CFAEdge>> createAlignments(List<CFAEdge> ce, List<CFAEdge> safePath) {
    // TODO: What about Loops ?
    List<CFAEdge> ce_1 = new ArrayList<>(ce);
    List<CFAEdge> safePath_1 = new ArrayList<>(safePath);
    List<CFAEdge> ce_2 = new ArrayList<>();
    List<CFAEdge> safePath_2 = new ArrayList<>();

    // TODO: Event - Klasse benutzen und 1 Methode fuer beide Klassen fuer ALignments haben ?
    // MAKING ALIGNMENTS
    for (int i = 0; i < ce_1.size(); i++) {
      for (int j = 0; j < safePath_1.size(); j++) {
        if (ce_1.get(i).getPredecessor().getNodeNumber() == safePath_1.get(j).getPredecessor()
            .getNodeNumber()) {
          if (ce_1.get(i).getSuccessor().getNodeNumber() != safePath_1.get(j).getSuccessor()
              .getNodeNumber()) {
            ce_2.add(ce_1.get(i));
            safePath_2.add(safePath_1.get(j));
            // remove the aligned Node
            safePath_1.remove(j);
            break;
          }
        }
      }
    }

    List<List<CFAEdge>> result = new ArrayList<>();
    result.add(ce_2);
    result.add(safePath_2);
    return result;

  }


  /**
   * TODO: Code Duplicate with CF_Distance_Metric
   * Filter the path to stop at the __Verifier__assert Node
   *
   * @return the new - clean of useless nodes - Path
   */
  private List<CFAEdge> cleanPath(ARGPath path) {
    List<CFAEdge> flow = path.getFullPath();
    List<CFAEdge> clean_flow = new ArrayList<>();

    for (int i = 0; i < flow.size(); i++) {
      if (flow.get(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        List<String> code = Splitter.onPattern("\\s*[()]\\s*").splitToList(flow.get(i).getCode());
        if (code.size() > 0) {
          if (code.get(0).equals("__VERIFIER_assert")) {
            clean_flow.add(flow.get(i));
            return clean_flow;
          }
        }
      }
      clean_flow.add(flow.get(i));
    }
    return clean_flow;
  }

}
