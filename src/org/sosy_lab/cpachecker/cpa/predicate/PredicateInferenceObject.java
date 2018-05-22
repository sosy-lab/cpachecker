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
package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Preconditions;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateInferenceObject implements InferenceObject {

  private final Set<CAssignment> edgeFormulas;
  private final BooleanFormula abstraction;

  public PredicateInferenceObject(Set<CAssignment> f, BooleanFormula a) {
    Preconditions.checkNotNull(f);
    Preconditions.checkNotNull(a);

    edgeFormulas = f;
    abstraction = a;
  }

  public Set<CAssignment> getAction() {
    return edgeFormulas;
  }

  public BooleanFormula getGuard() {
    return abstraction;
  }

  @Override
  public boolean hasEmptyAction() {
    return false;
  }

  @Override
  public String toString() {
    return edgeFormulas.toString();
  }
}
