// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg;

import java.util.Optional;

/**
 * Represents a single edge in a system dependence graph (SDG).
 *
 * <p>This class can be extended to get an easier to use type without type parameters.
 *
 * @param <V> The type of variables in the SDG. Variables are defined and used. Dependencies exist
 *     between defs and subsequent uses. Furthermore, formal-in/out and actual-in/out nodes exist
 *     for specific variables.
 */
public interface SdgEdge<V> {

  int getId();

  /**
   * Returns the {@link SdgEdgeType} of this edge.
   *
   * @return the {@link SdgEdgeType} of this edge
   */
  SdgEdgeType getType();

  /**
   * Returns whether this edge has the specified {@link SdgEdgeType}.
   *
   * @param pEdgeType the type to compare with the type of this edge
   * @return whether this edge has the specified {@link SdgEdgeType}
   */
  default boolean hasType(SdgEdgeType pEdgeType) {
    return getType() == pEdgeType;
  }

  /**
   * Returns the variable that represents the cause for this edge.
   *
   * <p>Variables are defined and used. Dependencies exist between defs and subsequent uses. A
   * variable causing such a dependency is called the cause for the dependency.
   *
   * @return the variable that represents the cause for this edge
   */
  Optional<V> getCause();
}
