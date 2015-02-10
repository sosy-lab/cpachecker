/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.wp;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Optional;

public class WpAbstractState implements AbstractState {

  @Nullable private final PathFormula violationPathFormula;
  @Nullable private final PathFormula healthyPathFormula;

  public WpAbstractState(PathFormula pViolationPathFormula, PathFormula pHealthyPathFormula) {
    assert pViolationPathFormula != null || pHealthyPathFormula != null;

    this.violationPathFormula = pViolationPathFormula;
    this.healthyPathFormula = pHealthyPathFormula;
  }

  public Optional<PathFormula> getHealthyPathFormula() {
    if (healthyPathFormula == null) {
      return Optional.absent();
    }
    return Optional.of(healthyPathFormula);
  }

  public Optional<PathFormula> getViolationPathFormula() {
    if (violationPathFormula == null) {
      return Optional.absent();
    }
    return Optional.of(violationPathFormula);
  }

  @Override
  public String toString() {
    return String.format("VP: %s\nHP: %s\n", violationPathFormula, healthyPathFormula);
  }

}
