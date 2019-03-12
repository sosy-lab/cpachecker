/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.dependencegraph;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Node of a dependence graph. Represents a {@link CFAEdge}. */
public class DGNode implements Serializable {

  private static final long serialVersionUID = -3772112168117340401L;

  private CFAEdge cfaEdge;
  private MemoryLocation cause;

  public DGNode(final CFAEdge pCfaEdge, @Nullable final MemoryLocation pCause) {
    checkNotNull(pCfaEdge);
    cfaEdge = pCfaEdge;
    cause = pCause;
  }

  public DGNode(final CFAEdge pCfaEdge) {
    this(pCfaEdge, null);
  }

  DGNode() {}

  public CFAEdge getCfaEdge() {
    return cfaEdge;
  }

  public boolean isUnknownPointerNode() {
    return false;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    DGNode dgNode = (DGNode) pO;
    return cfaEdge.equals(dgNode.cfaEdge) && Objects.equals(cause, dgNode.cause);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cfaEdge, cause);
  }

  @Override
  public String toString() {
    return "(" + cfaEdge + ", " + cause + ")";
  }

  /** {@link DGNode} that signalizes the dependency on an unknown memory location. * */
  public static class UnknownPointerNode extends DGNode {

    private static final long serialVersionUID = 6558402143061075378L;
    private static final UnknownPointerNode INSTANCE = new UnknownPointerNode();

    private UnknownPointerNode() {}

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
    public boolean equals(Object pO) {
      return pO instanceof UnknownPointerNode;
    }

    @Override
    public int hashCode() {
      return 1;
    }

    @Override
    public String toString() {
      return "UNK-Pointer";
    }
  }
}
