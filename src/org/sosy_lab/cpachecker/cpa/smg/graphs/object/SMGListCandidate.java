// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

public abstract class SMGListCandidate<S> {

  private final SMGObject startObject;
  protected final MachineModel model;
  private final S shape;

  protected SMGListCandidate(SMGObject pStartObject, MachineModel pModel, S pShape) {
    startObject = pStartObject;
    model = pModel;
    shape = pShape;
  }

  public SMGObject getStartObject() {
    return startObject;
  }

  public S getShape() {
    return shape;
  }

  @Override
  public int hashCode() {
    return Objects.hash(startObject, model, shape);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SMGListCandidate<?>)) {
      return false;
    }
    SMGListCandidate<?> other = (SMGListCandidate<?>) o;
    return Objects.equals(startObject, other.startObject)
        && Objects.equals(model, other.model)
        && Objects.equals(shape, other.shape);
  }
}
