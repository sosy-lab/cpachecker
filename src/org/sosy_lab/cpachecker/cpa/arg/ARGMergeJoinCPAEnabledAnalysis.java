// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARGMergeJoinCPAEnabledAnalysis implements MergeOperator {

  private final boolean deleteSubgraphAfterMerge;
  private final MergeOperator wrappedMerge;
  private final List<ARGState> toDeleteFromReached = new ArrayList<>();

  public ARGMergeJoinCPAEnabledAnalysis(
      MergeOperator pWrappedMerge, final boolean pDeleteSubgraph) {
    wrappedMerge = pWrappedMerge;
    deleteSubgraphAfterMerge = pDeleteSubgraph;
  }

  // may cause problems during refinement, relink of elements may lead to non-matching abstraction
  // formulae
  // need to compute path formulae for refinement as done if predicated analysis is enabled
  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {

    ARGState argElement1 = (ARGState) pState1;
    ARGState argElement2 = (ARGState) pState2;

    assert !argElement1.isCovered() : "Trying to merge covered element " + argElement1;

    if (!argElement2.mayCover()) {
      // elements that may not cover should also not be used for merge
      return pState2;
    }

    if (argElement1.getMergedWith() != null) {
      // element was already merged into another element, don't try to widen argElement2
      // TODO In the optimal case (if all merge & stop operators as well as the reached set
      // partitioning fit well together)
      // this case shouldn't happen, but it does sometimes (at least with
      // ExplicitCPA+FeatureVarsCPA).
      return pState2;
    }

    AbstractState wrappedState1 = argElement1.getWrappedState();
    AbstractState wrappedState2 = argElement2.getWrappedState();
    AbstractState retElement = wrappedMerge.merge(wrappedState1, wrappedState2, pPrecision);
    if (retElement.equals(wrappedState2)) {
      return pState2;
    }

    ARGState mergedElement = new ARGState(retElement, null);

    if (deleteSubgraphAfterMerge) {
      // now replace argElement2 by mergedElement in ARG
      // deleteChildren(argElement2);
      deleteChildren2(argElement2);
    }

    argElement2.replaceInARGWith(mergedElement);

    argElement1.setMergedWith(mergedElement);

    if (mergedElement.isTarget()) {
      throw new CPAEnabledAnalysisPropertyViolationException(
          "Property violated during merge", argElement1, true);
    }

    return mergedElement;
  }

  @SuppressWarnings("unused")
  private void deleteChildren(ARGState parent) {
    // assumes that covered elements are not saved in reached set
    Deque<ARGState> toProcess = new ArrayDeque<>();
    toProcess.add(parent);

    ARGState current;
    ARGState child;
    ARGState covered;

    while (!toProcess.isEmpty()) {
      current = toProcess.pop();
      toDeleteFromReached.add(current);

      // delete connection to children
      while (!current.getChildren().isEmpty()) {
        child = current.getChildren().iterator().next();
        current.deleteChild(child);

        // relink or delete child if it is not connected by another parent
        if (child.getParents().isEmpty()) {
          if (!child.getCoveredByThis().isEmpty()) {
            // relink child in ARG to parent of first covered element
            // only relink if it has a parent
            Iterator<ARGState> coveredElems = child.getCoveredByThis().iterator();
            do {
              covered = coveredElems.next();
              if (covered.getParents().isEmpty()) {
                covered = null;
              }
            } while (covered == null && coveredElems.hasNext());

            if (covered != null) {
              // remove coverage relation and relink
              covered.uncover();
              covered.replaceInARGWith(child);
            } else {
              // add child for deletion
              if (child.isCovered()) {
                child.uncover();
              }
              toProcess.add(child);
            }
          } else {
            // add child for deletion
            if (child.isCovered()) {
              child.uncover();
            }
            toProcess.add(child);
          }
        }
      }
    }
  }

  private void deleteChildren2(ARGState parent) {
    // assumes that covered elements are not saved in reached set
    Set<ARGState> subtreeNodes = getSubtreeNodes(parent);
    Set<ARGState> laterCovered = new HashSet<>();

    Deque<ARGState> toProcess = new ArrayDeque<>();
    toProcess.add(parent);

    ARGState current;
    ARGState child;
    ARGState covered;

    while (!toProcess.isEmpty()) {
      current = toProcess.pop();
      toDeleteFromReached.add(current);

      // delete connection to children
      while (!current.getChildren().isEmpty()) {
        child = current.getChildren().iterator().next();
        current.deleteChild(child);

        assert child.getParents().isEmpty();
        // relink or delete child
        if (!child.getCoveredByThis().isEmpty()) {
          // relink child in ARG to parent of first covered element
          // only relink if it has a parent
          covered = getCoveredNodeFromDifferentSubtree(subtreeNodes, child);

          if (covered != null) {
            // remove coverage relation and relink
            covered.uncover();
            covered.replaceInARGWith(child);
            subtreeNodes.removeAll(getSubtreeNodes(child));
          } else {
            // possibly later needed do not delete subtree
            laterCovered.add(child);
          }
        } else {
          // add child for deletion
          if (child.isCovered()) {
            child.uncover();
          }
          toProcess.add(child);
        }
      }
    }

    // stop deletion and find out if there are deeper children which are and may reference one of
    // their ancestor
    toProcess.addAll(laterCovered);
    while (!toProcess.isEmpty()) {
      current = toProcess.pop();

      for (ARGState c : current.getChildren()) {

        assert (c.getParents().size() == 1);
        // relink or delete child
        if (!c.getCoveredByThis().isEmpty()) {
          // relink child in ARG to parent of first covered element
          // only relink if it has a parent
          covered = getCoveredNodeFromDifferentSubtree(subtreeNodes, c);

          if (covered != null) {
            // delete edge from current to child and introduce covering
            current.deleteChild(c);
            new ARGState(c.getWrappedState(), current).setCovered(c);
            // remove coverage relation and relink
            covered.uncover();
            covered.replaceInARGWith(c);
            subtreeNodes.removeAll(getSubtreeNodes(c));

          } else {
            // possibly later needed do not delete subtree
            laterCovered.add(c);
            toProcess.add(c);
          }
        } else {
          toProcess.add(c);
        }
      }
    }

    // find out if now covered by external node
    boolean changed = true;
    while (changed) {
      changed = false;
      for (ARGState later : laterCovered) {
        if (!later.getCoveredByThis().isEmpty()) {
          covered = getCoveredNodeFromDifferentSubtree(subtreeNodes, later);
          if (covered != null) {
            // delete edge from parent and introduce covering
            assert (later.getParents().size() <= 1);
            if (later.getParents().size() == 1) {
              new ARGState(later.getWrappedState(), later.getParents().iterator().next())
                  .setCovered(later);
              later.getParents().iterator().next().deleteChild(later);
            }
            // remove coverage relation and relink
            covered.uncover();
            covered.replaceInARGWith(later);
            subtreeNodes.removeAll(getSubtreeNodes(later));

            // next iteration
            laterCovered.remove(later);
            changed = true;
            break;
          }
        }
      }
    }

    // delete rest of subtree
    toProcess.addAll(laterCovered);
    while (!toProcess.isEmpty()) {
      current = toProcess.pop();
      toDeleteFromReached.add(current);

      // delete connection to children
      while (!current.getChildren().isEmpty()) {
        child = current.getChildren().iterator().next();
        current.deleteChild(child);
        toProcess.add(child);
      }
    }
  }

  // requires that elem has at least one covered element
  private ARGState getCoveredNodeFromDifferentSubtree(Set<ARGState> subtreeNodes, ARGState elem) {
    Iterator<ARGState> coveredElems = elem.getCoveredByThis().iterator();
    ARGState covered;
    do {
      covered = coveredElems.next();
      assert covered.getCoveredByThis().isEmpty();
      if (covered.getParents().isEmpty() || subtreeNodes.contains(covered)) {
        covered = null;
      }
    } while (covered == null && coveredElems.hasNext());
    return covered;
  }

  private Set<ARGState> getSubtreeNodes(ARGState top) {
    Deque<ARGState> toProcess = new ArrayDeque<>();
    Set<ARGState> nodes = new HashSet<>();

    toProcess.push(top);
    nodes.add(top);

    while (!toProcess.isEmpty()) {
      top = toProcess.pop();
      for (ARGState child : top.getChildren()) {
        if (nodes.add(child)) {
          toProcess.push(child);
        }
      }
    }

    return nodes;
  }

  public void cleanUp(ReachedSet pReachedSet) {
    pReachedSet.removeAll(toDeleteFromReached);
    toDeleteFromReached.clear();
  }
}
