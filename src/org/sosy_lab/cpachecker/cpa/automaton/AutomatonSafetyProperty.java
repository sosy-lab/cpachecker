/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import javax.annotation.Nonnull;

import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;


public class AutomatonSafetyProperty implements Property {

  @Nonnull private final Automaton automaton;
  @Nonnull private final Optional<StringExpression> violationDescriptionExpression;
  @Nonnull private final String propertyViolationInstance;

  public AutomatonSafetyProperty(Automaton pAutomaton, AutomatonTransition pTransition, String pDesc) {
    this.automaton = Preconditions.checkNotNull(pAutomaton);
    this.violationDescriptionExpression = Preconditions.checkNotNull(pTransition.getViolationDescriptionExpression());
    this.propertyViolationInstance = Preconditions.checkNotNull(pDesc);
  }

  public AutomatonSafetyProperty(Automaton pAutomaton, AutomatonTransition pTransition) {
    this.automaton = Preconditions.checkNotNull(pAutomaton);
    this.violationDescriptionExpression = Preconditions.checkNotNull(pTransition.getViolationDescriptionExpression());
    this.propertyViolationInstance = "";
  }

  @Override
  public String toString() {
    return propertyViolationInstance.length() > 0
        ? propertyViolationInstance
        : automaton.getName();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + automaton.hashCode();
    result = prime * result + violationDescriptionExpression.hashCode();
    result = prime * result + propertyViolationInstance.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AutomatonSafetyProperty other = (AutomatonSafetyProperty) obj;

    if (!violationDescriptionExpression.equals(other.violationDescriptionExpression)) {
      return false;
    }

    if (!automaton.equals(other.automaton)) {
      return false;
    }

    if (!propertyViolationInstance.equals(other.propertyViolationInstance)) {
      return false;
    }

    return true;
  }

}
