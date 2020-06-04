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
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class AbstractDistanceMetric {

  public AbstractDistanceMetric() {

  }

  /**
   * Start Method
   * @param safePaths
   * @param counterexample
   * @return
   */
  public List<CFAEdge> startDistanceMetric(List<ARGPath> safePaths, ARGPath counterexample) {
    List<CFAEdge> ce = cleanPath(counterexample);
    List<List<CFAEdge>> paths = new ArrayList<>();
    // clean the paths from useless Statements
    for (int i = 0; i < safePaths.size(); i++) {
      paths.add(cleanPath(safePaths.get(i)));
    }

    List<CFAEdge> closestSuccessfulExecution = comparePaths(ce, paths);

    return closestSuccessfulExecution;

  }

  /**
   *
   * @param ce := Counterexample
   * @param sp := Safe paths
   * @return
   */
  private List<CFAEdge> comparePaths(List<CFAEdge> ce, List<List<CFAEdge>> sp) {
    // TODO: Make sure that the safe path list is not empty
    List<Integer> distances = new ArrayList<>();
    int weight_p = 1;
    //int weight_a = 1;
    int weight_unal = 2;

    for (int i = 0; i < sp.size(); i++) {
      // Step 1: CREATE ALIGNMENTS
      List<List<CFAEdge>> alignments = createAlignments(ce, sp.get(i));
      // Step 2: Get Differences between Predicates
      int predicate_distance = calculatePredicateDistance(alignments);
      // Step 3: Get Differences between Actions
      // did that already in the step above ? TODO: Check that
      int unalignedStates = getNumberOfUnalignedStates(alignments, sp.get(i));
      // calculate the distance
      int d = weight_p*predicate_distance + weight_unal*unalignedStates;
      distances.add(d);
    }

    //ln(distances);

    // clean distance = 0
    for (int i = 0; i < distances.size(); i++) {
      if (distances.get(i) == 0) {
        distances.remove(i);
        sp.remove(i);
      }
    }

    int index = 0;
    int min_dist = distances.get(0);
    for (int i = 0; i < distances.size(); i++) {
      if (distances.get(i) <= min_dist) {
        index = i;
        min_dist = distances.get(i);
      }
    }


    // JUST FOR ME
    List<Integer> indices = new ArrayList<>();
    for (int i = 0; i < distances.size(); i++) {
      if (distances.get(i) == min_dist && i != index) {
        indices.add(i);
      }
    }

    for (int i = 0; i < indices.size(); i++) {
   //   ln();
      ExplainTool.ExplainDeltas(ce, sp.get(indices.get(i)));
     // ln("");
    }

    // END

    return sp.get(index);
  }

  /**
   * TODO: Do I really need this as extra Function ?
   * @param alignments
   * @param safePath
   * @return
   */
  private int getNumberOfUnalignedStates(List<List<CFAEdge>> alignments, List<CFAEdge> safePath) {
    return Math.abs((alignments.get(0).size() - safePath.size()));
  }

  /**
   * Calculates the distance of the predicates
   * @param alignments
   * @return
   */
  private int calculatePredicateDistance(List<List<CFAEdge>> alignments) {
    // TODO: Make sure that the 2 lists are of the same size
    int distance = 0;
    // THE alignments List has only 2 Lists with the same size
    // TODO:
    for (int i = 0; i < alignments.get(0).size(); i++) {
      if (!alignments.get(0).get(i).getCode().equals(alignments.get(1).get(i).getCode())) {
        distance++;
      }
    }

    return distance;
  }

  /**
   * Create Alignments between CE and Safe Path
   * @param ce
   * @param safePath
   * @return
   */
  private List<List<CFAEdge>> createAlignments(List<CFAEdge> ce, List<CFAEdge> safePath) {
    // TODO: What about Loops ?
    List<CFAEdge> ce_1 = new ArrayList<>(ce);
    List<CFAEdge> safePath_1 = new ArrayList<>(safePath);
    List<CFAEdge> ce_2 = new ArrayList<>();
    List<CFAEdge> safePath_2 = new ArrayList<>();

    // MAKING ALIGNMENTS
    for (int i = 0; i < ce_1.size(); i++) {
      for (int j = 0; j < safePath_1.size(); j++) {
        if (ce_1.get(i).getPredecessor().getNodeNumber() == safePath_1.get(j).getPredecessor().getNodeNumber()) {
          ce_2.add(ce_1.get(i));
          safePath_2.add(safePath_1.get(j));
          // and delete them
          ce_1.remove(i);
          safePath_1.remove(j);
          i = i - 1;
          break;
        }
      }
    }

    List<List<CFAEdge>> result = new ArrayList<>();
    result.add(ce_2);
    result.add(safePath_2);
    return result;

  }

  /** TODO: Code Duplicate with CF_Distance_Metric
   * Filter the path to stop at the __Verifier__assert Node
   * @param path
   * @return the new - clean of useless nodes - Path
   */
  private List<CFAEdge> cleanPath(ARGPath path) {
    List<CFAEdge> flow = path.getFullPath();
    List<CFAEdge> clean_flow = new ArrayList<>();

    for (int i = 0; i < flow.size(); i++) {
      if (flow.get(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        //String[] code = flow.get(i).getCode().split("\\s*[()]\\s*");
        /*if (code.length > 0) {
          if (code[0].equals("__VERIFIER_assert")) {
            clean_flow.add(flow.get(i));
            return clean_flow;
          }
        }*/
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
