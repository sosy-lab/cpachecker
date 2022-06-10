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

public abstract class AbstractSdgEdge<V> implements SdgEdge<V> {

  private final int id;
  private final SdgEdgeType type;
  private final @Nullable V cause;

  private final int hash;

  private AbstractSdgEdge(int pId, SdgEdgeType pType, @Nullable V pCause) {

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
  protected AbstractSdgEdge(SdgEdge<V> pEdge) {
    this(pEdge.getId(), pEdge.getType(), pEdge.getCause().orElse(null));
  }

  static <V> SdgEdge<V> of(int pId, SdgEdgeType pType) {
    return new AbstractSdgEdge<>(pId, pType, null) {};
  }

  static <V> SdgEdge<V> of(int pId, SdgEdgeType pType, @Nullable V pCause) {
    return new AbstractSdgEdge<>(pId, pType, pCause) {};
  }

  @Override
  public final int getId() {
    return id;
  }

  @Override
  public final SdgEdgeType getType() {
    return type;
  }

  @Override
  public boolean hasType(SdgEdgeType pEdgeType) {
    return type == pEdgeType;
  }

  @Override
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

    return id == other.getId()
        && type == other.getType()
        && Objects.equals(cause, other.getCause().orElse(null));
  }

  @Override
  public final String toString() {
    return String.format("%s[type=%s, cause=%s]", getClass().getName(), type, cause);
  }
}
