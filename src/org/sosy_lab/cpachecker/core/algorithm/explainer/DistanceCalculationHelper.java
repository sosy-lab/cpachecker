// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.explainer;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This class contains various methods that are essential for
 * the calculation of the distance between 2 Executions
 */
public class DistanceCalculationHelper {

  private BooleanFormulaManagerView bfmgr;

  /**
   * For the Alignments Distance Metric
   */
  public DistanceCalculationHelper(BooleanFormulaManagerView pBooleanFormulaManagerView) {
    this.bfmgr = pBooleanFormulaManagerView;
  }

  /**
   * For the Control Flow Distance Metric
   */
  public DistanceCalculationHelper() {

  }

  /**
   * Filter the path to stop at the __Verifier__assert Node
   *
   * @return the new - clean of useless nodes - Path
   */
  public List<CFAEdge> cleanPath(ARGPath path) {
    List<CFAEdge> flow = path.getFullPath();
    List<CFAEdge> clean_flow = new ArrayList<>();

    for (int i = 0; i < flow.size(); i++) {
      if (flow.get(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        List<String> code = Splitter.onPattern("\\s*[()]\\s*").splitToList(flow.get(i).getCode());
        if (!code.isEmpty()) {
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

  /**
   * Filter the path to stop at the __Verifier__assert Node
   *
   * @return the new - clean of useless nodes - Path
   */
  public List<CFAEdge> cleanPath(List<CFAEdge> path) {
    List<CFAEdge> flow = path;
    List<CFAEdge> filteredEdges = new ArrayList<>();

    for (int i = 0; i < flow.size(); i++) {
      if (flow.get(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        List<String> code = Splitter.onPattern("\\s*[()]\\s*").splitToList(flow.get(i).getCode());
        if (!code.isEmpty()) {
          if (code.get(0).equals("__VERIFIER_assert")) {
            filteredEdges.add(flow.get(i));
            return filteredEdges;
          }
        }
      }
      filteredEdges.add(flow.get(i));
    }
    return filteredEdges;
  }

  /**
   * Convert a list of ARGPaths to a List of Lists of CFAEdges
   */
  public List<List<CFAEdge>> convertPathsToEdges(List<ARGPath> paths) {
    List<List<CFAEdge>> result = new ArrayList<>();
    for (int i = 0; i < paths.size(); i++) {
      result.add(paths.get(i).getFullPath());
    }
    return result;
  }

  /**
   * Checks if a BooleanFormula can be further splitted through "toConjunctionArgs"
   *
   * @param f the BooleanFormula
   * @return True if yes, otherwise False
   */
  public boolean isConj(BooleanFormula f) {
    Set<BooleanFormula> after = bfmgr.toConjunctionArgs(f, true);
    if (after.size() >= 2) {
      return true;
    }
    if (after.size() == 1) {
      return false;
    }
    return false;
  }


  /**
   * Checks if a BooleanFormula can be further splitted through the "toDisjunctionArgs" Method
   *
   * @param f the BooleanFormula
   * @return True, if yes, otherwise False
   */
  public boolean isDisj(BooleanFormula f) {
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
   * Splits a coupled BooleanFormula to a Set of individual BooleanFormulas
   *
   * @return the same BooleanFormula but splitted in pieces
   */
  public Set<BooleanFormula> splitPredicates(BooleanFormula form) {
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


}
