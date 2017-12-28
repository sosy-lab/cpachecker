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
package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;


public class CompositeInferenceObject implements InferenceObject {

  private final ImmutableList<InferenceObject> inferenceObjects;

  private CompositeInferenceObject(List<InferenceObject> elements) {
    this.inferenceObjects = ImmutableList.copyOf(elements);
  }

  public List<InferenceObject> getInferenceObjects() {
    return inferenceObjects;
  }

  public InferenceObject getInferenceObject(int i) {
    return inferenceObjects.get(i);
  }

  public Object getSize() {
    return inferenceObjects.size();
  }

  @Override
  public boolean hasEmptyAction() {
    for (InferenceObject o : inferenceObjects) {
      if (!o.hasEmptyAction()) {
        return false;
      }
    }
    return true;
  }

  public static InferenceObject create(List<InferenceObject> elements) {

    boolean notEmpty = from(elements)
        .anyMatch(s -> !s.hasEmptyAction());

    if (notEmpty) {
      return new CompositeInferenceObject(elements);
    } else {
      return EmptyInferenceObject.getInstance();
    }
  }

  @Override
  public String toString() {
    return from(inferenceObjects)
        .filter(o -> !o.hasEmptyAction())
        .join(Joiner.on(", "));
  }
}
