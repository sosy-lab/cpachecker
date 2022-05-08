// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph.dominance;

import java.util.Objects;

/**
 * A utility class for computing dominance trees and dominance frontiers.
 *
 * <p>A dominance tree ({@link DomTree}) is created by {@link DomTree#createDomTree}. The resulting
 * tree cannot directly be used for tree traversal (e.g. depth-first search), as every node only
 * contains a reference to its parent, but no references to its children.
 *
 * <p>If references to children are required, a traversable version of the dominance tree can by
 * created by {@link DomTree#asGraph()}.
 *
 * <p>Dominance frontiers ({@link DomFrontiers}) are created by {@link
 * #createDomFrontiers(DomTree)}. A {@link DomFrontiers}-object contains the dominance frontier for
 * every node in the graph.
 *
 * <p>Implementation detail: the dominance tree and dominance frontier computation algorithms are
 * from "A Simple, Fast Dominance Algorithm" (Cooper et al.).
 */
public final class Dominance {

  private Dominance() {}

  /**
   * Creates the {@link DomFrontiers}-object that contains the dominance frontier for every node in
   * the dominance tree.
   *
   * @param <T> the node-type of the original graph.
   * @param pDomTree the {@link DomTree} (dominance tree) of the original graph.
   * @throws NullPointerException if {@code pDomTree} is {@code null}.
   * @return the created {@link DomFrontiers}-object.
   */
  public static <T> DomFrontiers<T> createDomFrontiers(DomTree<T> pDomTree) {

    Objects.requireNonNull(pDomTree, "pDomTree must not be null");

    DomFrontiers.Frontier[] frontiers = computeFrontiers(pDomTree.getInput(), pDomTree.getDoms());

    return new DomFrontiers<>(pDomTree.getInput(), frontiers);
  }

  /**
   * For more information on the algorithm, see "A Simple, Fast Dominance Algorithm" (Cooper et
   * al.).
   */
  private static DomFrontiers.Frontier[] computeFrontiers(
      final DomInput<?> pInput, final int[] pDoms) {

    DomFrontiers.Frontier[] frontiers = new DomFrontiers.Frontier[pInput.getNodeCount()];
    for (int id = 0; id < frontiers.length; id++) {
      frontiers[id] = new DomFrontiers.Frontier();
    }

    int index = 0; // index for input data (data format is specified in DomInput)
    for (int id = 0; id < pInput.getNodeCount(); id++) { // all nodes

      if (pInput.getValue(index) == DomInput.DELIMITER) { // has no predecessors?
        index++; // skip delimiter
        continue;
      }

      if (pInput.getValue(index + 1) == DomInput.DELIMITER) { // has exactly one predecessor?
        index += 2; // skip only predecessor + delimiter
        continue;
      }

      int runner;
      while ((runner = pInput.getValue(index)) != DomInput.DELIMITER) { // all predecessors of node

        while (runner != DomTree.UNDEFINED && runner != pDoms[id]) {
          frontiers[runner].add(id);
          runner = pDoms[runner];
        }

        index++; // next predecessor
      }

      index++; // skip delimiter
    }

    return frontiers;
  }
}
