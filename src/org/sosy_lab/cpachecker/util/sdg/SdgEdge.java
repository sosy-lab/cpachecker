// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg;

import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a single edge in a system dependence graph (SDG).
 *
 * <p>This class can be extended to get an easier to use type without type parameters.
 *
 * @param <V> The type of variables in the SDG. Variables are defined and used. Dependencies exist
 *     between defs and subsequent uses. Furthermore, formal-in/out and actual-in/out nodes exist
 *     for specific variables.
 */
public class SdgEdge<V> {

  private final int id;
  private final SdgEdgeType type;
  private final @Nullable V cause;

  private final int hash;

  private SdgEdge(int pId, SdgEdgeType pType, @Nullable V pCause) {

    id = pId;
    type = pType;
    cause = pCause;

    hash = Objects.hash(type, cause);
  }

  /**
   * Creates a new {@link SdgEdge} instance from the specified edge.
   *
   * <p>The constructed edge is a copy of the specified edge. This non-private constructor is
   * required for subclasses of {@link SdgEdge}.
   *
   * @param pEdge a edge to create a copy of
   */
  protected SdgEdge(SdgEdge<V> pEdge) {
    this(pEdge.id, pEdge.type, pEdge.cause);
  }

  static <V> SdgEdge<V> of(int pId, SdgEdgeType pType) {
    return new SdgEdge<>(pId, pType, null);
  }

  static <V> SdgEdge<V> of(int pId, SdgEdgeType pType, @Nullable V pCause) {
    return new SdgEdge<>(pId, pType, pCause);
  }

  public int getId() {
    return id;
  }

  /**
   * Returns the {@link SdgEdgeType} of this edge.
   *
   * @return the {@link SdgEdgeType} of this edge
   */
  public final SdgEdgeType getType() {
    return type;
  }

  /**
   * Returns whether this edge has the specified {@link SdgEdgeType}.
   *
   * @param pEdgeType the type to compare with the type of this edge
   * @return whether this edge has the specified {@link SdgEdgeType}
   */
  public boolean hasType(SdgEdgeType pEdgeType) {
    return type == pEdgeType;
  }

  /**
   * Returns the variable that represents the cause for this edge.
   *
   * <p>Variables are defined and used. Dependencies exist between defs and subsequent uses. A
   * variable causing such a dependency is called the cause for the dependency.
   *
   * @return the variable that represents the cause for this edge
   */
  public final Optional<V> getCause() {
    return Optional.ofNullable(cause);
  }

  @Override
  public final int hashCode() {
    return hash;
  }

  @Override
  public final boolean equals(Object pObject) {

    if (this == pObject) {
      return true;
    }

    if (!(pObject instanceof SdgEdge)) {
      return false;
    }

    SdgEdge<?> other = (SdgEdge<?>) pObject;

    return hash == other.hash
        && id == other.id
        && type == other.type
        && Objects.equals(cause, other.cause);
  }

  @Override
  public final String toString() {
    return String.format("%s[type=%s, cause=%s]", getClass().getName(), type, cause);
  }
}
