// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.explainer;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This class contains various methods that are essential for the calculation of the distance
 * between 2 program executions
 */
public class DistanceCalculationHelper {

  private BooleanFormulaManagerView bfmgr;

  /** For the Alignments Distance Metric */
  public DistanceCalculationHelper(BooleanFormulaManagerView pBooleanFormulaManagerView) {
    bfmgr = pBooleanFormulaManagerView;
  }

  /** For the Control Flow Distance Metric */
  public DistanceCalculationHelper() {}

  /**
   * Filter the path to stop at the __Verifier__assert Node
   *
   * @return the new - clean of useless nodes - Path
   */
  public List<CFAEdge> cleanPath(ARGPath path) {
    List<CFAEdge> flow = path.getFullPath();
    return cleanPath(flow);
  }

  /**
   * Filter the path to stop at the __Verifier__assert Node
   *
   * @return the new - clean of useless nodes - Path
   */
  public List<CFAEdge> cleanPath(List<CFAEdge> path) {
    if (path == null) {
      return null;
    }
    List<CFAEdge> filteredEdges = new ArrayList<>();

    // find error line
    for (CFAEdge f : path) {
      if (f.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        List<String> code = Splitter.onPattern("\\s*[()]\\s*").splitToList(f.getCode());
        if (code.get(0).equals("__VERIFIER_assert")) {
          break;
        }
      }
      filteredEdges.add(f);
    }

    return filteredEdges;
  }

  /**
   * Convert a list of ARGPaths to a List of Lists of CFAEdges
   *
   * @param paths the paths that out to be converted to Lists of CFAEdges
   * @return the paths as List of CFAEdges
   */
  public List<List<CFAEdge>> convertPathsToEdges(List<ARGPath> paths) {
    List<List<CFAEdge>> result = new ArrayList<>();
    for (ARGPath pPath : paths) {
      result.add(pPath.getFullPath());
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
    return after.size() >= 2;
  }

  /**
   * Checks if a BooleanFormula can be further splitted through the "toDisjunctionArgs" Method
   *
   * @param f the BooleanFormula
   * @return True, if yes, otherwise False
   */
  public boolean isDisj(BooleanFormula f) {
    Set<BooleanFormula> after = bfmgr.toDisjunctionArgs(f, true);
    return after.size() >= 2;
  }

  /**
   * Splits a coupled BooleanFormula to a Set of individual BooleanFormulas
   *
   * @return the same BooleanFormula but splitted in pieces
   */
  public Set<BooleanFormula> splitPredicates(BooleanFormula form) {
    Set<BooleanFormula> result = new HashSet<>();
    Set<BooleanFormula> modulo = new HashSet<>();
    modulo.add(form);

    while (true) {
      Set<BooleanFormula> temp;
      BooleanFormula current;
      Iterator<BooleanFormula> iterator = modulo.iterator();

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

  /**
   * Find and constructs all Safe Paths Example: When the algorithm constructs the safe path:
   * [1,2,3,4,5] Condition now: [1,2,3], but "3" has 2 Children (1st: "4" and 2nd "9") Then the
   * Algorithm creates a new (copy) List: [1,2,3,4] and put this list in the waitList in order to be
   * examined later. Then adds in the original List the next child [1,2,3,4] and goes on. Terminates
   * when the Wait List is empty
   *
   * @param pStatesOnPathTo the ARGStates on the Path
   * @param root the Beginning of the Path
   * @param filterChildren is true only if we want to make sure that the nodes of the new paths are
   *     contained in the pStatesOnPathTo Collection
   * @return a List with all found safe paths
   */
  List<ARGPath> generateAllSuccessfulExecutions(
      Collection<ARGState> pStatesOnPathTo, ARGState root, boolean filterChildren) {
    if (filterChildren) {
      assert pStatesOnPathTo != null;
    }
    List<ARGPath> paths = new ArrayList<>();
    List<List<ARGState>> nodeWaitList = new ArrayList<>();
    nodeWaitList.add(new ArrayList<>());
    nodeWaitList.get(0).add(root);
    int numberOfCurrentPath = -1;
    ARGState currentNode;
    for (int i = 0; i < nodeWaitList.size(); i++) {
      numberOfCurrentPath++;

      List<ARGState> currentPath = nodeWaitList.get(numberOfCurrentPath);
      ImmutableList<ARGState> children = ImmutableList.of();
      int lastNode = nodeWaitList.get(numberOfCurrentPath).size() - 1;
      currentNode = nodeWaitList.get(numberOfCurrentPath).get(lastNode);

      if (filterChildren) {
        // for Explainer we need to consider only certain Nodes
        children = from(currentNode.getChildren()).filter(pStatesOnPathTo::contains).toList();
      } else {
        // Path Generation needs all the children
        children = ImmutableList.copyOf(currentNode.getChildren());
      }

      if (children.isEmpty()) {
        continue;
      }

      do {
        if (children.size() > 1) {
          // add new paths to nodeWaitList
          handleChildren(nodeWaitList, numberOfCurrentPath, children);
        }
        currentPath.add(children.get(0));
        currentNode = children.get(0);
        children =
            filterChildren
                ? from(currentNode.getChildren()).filter(pStatesOnPathTo::contains).toList()
                : ImmutableList.copyOf(currentNode.getChildren());
      } while (!children.isEmpty());
    }

    // create the final paths
    for (List<ARGState> nextInLineNode : nodeWaitList) {
      ARGPath targetPath = new ARGPath(nextInLineNode);
      paths.add(targetPath);
    }

    return paths;
  }

  /**
   * This method takes the children of the node that is being currently expanded and chooses what
   * child has to be expanded next while putting the extra children (if children.size() is more than
   * 1) in the wait-list
   *
   * @param nodeWaitList the wait list that contains all paths that their last node has to get
   *     further expanded
   * @param numberOfCurrentPath the index in the nodeWaitList of the current path that we expand
   * @param children the children of the node that is being expanded
   */
  private void handleChildren(
      List<List<ARGState>> nodeWaitList,
      int numberOfCurrentPath,
      ImmutableList<ARGState> children) {
    // create a new path for every new children
    IntStream.range(1, children.size())
        .forEach(
            j -> {
              List<ARGState> anotherPath = new ArrayList<>(nodeWaitList.get(numberOfCurrentPath));
              anotherPath.add(children.get(j));
              nodeWaitList.add(anotherPath);
            });
    nodeWaitList.get(numberOfCurrentPath).add(children.get(0));
  }
}
