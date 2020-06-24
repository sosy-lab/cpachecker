// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Node of a dependence graph. Represents a {@link CFAEdge}. */
public interface DGNode extends Serializable {

  public boolean isUnknownPointerNode();

  public CFAEdge getCfaEdge();

  public class EdgeNode implements DGNode {
    private static final long serialVersionUID = -3772112168117340401L;

    private final CFAEdge cfaEdge;
    private final MemoryLocation cause;

    public EdgeNode(final CFAEdge pCfaEdge, @Nullable final MemoryLocation pCause) {
      checkNotNull(pCfaEdge);
      cfaEdge = pCfaEdge;
      cause = pCause;
    }

    public EdgeNode(final CFAEdge pCfaEdge) {
      this(pCfaEdge, null);
    }

    @Override
    public CFAEdge getCfaEdge() {
      return cfaEdge;
    }

    @Override
    public boolean isUnknownPointerNode() {
      return false;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (!(pO instanceof EdgeNode)) {
        return false;
      }
      EdgeNode other = (EdgeNode) pO;
      return cfaEdge.equals(other.cfaEdge) && Objects.equals(cause, other.cause);
    }

    @Override
    public int hashCode() {
      return Objects.hash(cfaEdge, cause);
    }

    @Override
    public String toString() {
      return "(" + cfaEdge + ", " + cause + ")";
    }
  }

  /** {@link DGNode} that signalizes the dependency on an unknown memory location. * */
  public enum UnknownPointerNode implements DGNode {
    INSTANCE;

    public static UnknownPointerNode getInstance() {
      return INSTANCE;
    }

    @Override
    public boolean isUnknownPointerNode() {
      return true;
    }

    @Override
    public CFAEdge getCfaEdge() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "UNK-Pointer";
    }
  }
}
