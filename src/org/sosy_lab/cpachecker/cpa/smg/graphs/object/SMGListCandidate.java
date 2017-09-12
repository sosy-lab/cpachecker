/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

public abstract class SMGListCandidate<S> {

  private final SMGObject startObject;
  protected final MachineModel model;
  private final S shape;

  public SMGListCandidate(SMGObject pStartObject, MachineModel pModel, S pShape) {
    startObject = pStartObject;
    model = pModel;
    shape = pShape;
  }

  public abstract boolean hasRecursiveFields();

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