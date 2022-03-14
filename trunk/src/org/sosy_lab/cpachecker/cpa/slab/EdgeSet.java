// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slab;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Represents a set of @link {@link CFAEdge}s where one of the elements can be marked for later
 * retrieval.
 */
public class EdgeSet implements Serializable, Iterable<CFAEdge> {

  private static final long serialVersionUID = 1L;

  private Set<CFAEdge> edges;
  private CFAEdge selected;

  public EdgeSet(Collection<CFAEdge> edges) {
    checkArgument(!edges.isEmpty());
    this.edges = new LinkedHashSet<>(edges);
    selected = null;
  }

  public EdgeSet(EdgeSet other) {
    checkArgument(!other.edges.isEmpty());
    edges = new LinkedHashSet<>(other.edges);
  }

  /** Returns an iterator over the edges in the set. */
  @Override
  public Iterator<CFAEdge> iterator() {
    return edges.iterator();
  }

  /**
   * Removes a edge from the set of edges
   *
   * @param pEdge CFAedge that will be removed
   */
  public void removeEdge(CFAEdge pEdge) {
    edges.remove(pEdge);
    if (selected == pEdge) {
      selected = null;
    }
  }

  /**
   * Selects a edge for later retrieval by {@link EdgeSet#choose()}.
   *
   * @param pEdge the edge to be selected
   */
  public void select(CFAEdge pEdge) {
    selected = pEdge;
  }

  /**
   * Returns the (previously selected) {@link CFAEdge}. If no edge (or one that is not in the set)
   * was selected, it returns an arbitrary edge from the set. If the set is empty, it returns null.
   */
  public CFAEdge choose() {
    if (!edges.isEmpty()) {
      if (selected == null || !edges.contains(selected)) {
        return edges.iterator().next();
      } else {
        return selected;
      }
    }
    return null;
  }

  public boolean isEmpty() {
    return edges.isEmpty();
  }

  public int size() {
    return edges.size();
  }

  public void clear() {
    edges.clear();
  }

  public boolean contains(CFAEdge edge) {
    return edges.contains(edge);
  }
}
