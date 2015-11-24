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

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;


class AutomatonSafetyProperty implements SafetyProperty {

  public static enum PropertyGranularity {
    /** One automaton encodes exactly one property. */
    AUTOMATON,

    /** One automaton can encode multiple properties that are distinguished based on the violating (string) expression. */
    VIOLATING_EXPRESSION
  }

  @Options(prefix="automata.properties")
  public static class AutomatonSafetyPropertyFactory {

    @Option(description="Granularity of safety properties that are encoded in automata.")
    private PropertyGranularity granularity = PropertyGranularity.AUTOMATON;

    public AutomatonSafetyPropertyFactory(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public Set<SafetyProperty> createSingleProperty() {

      return ImmutableSet.<SafetyProperty>of(new AutomatonSafetyProperty());
    }

    public Set<SafetyProperty> createSingleProperty(StringExpression pViolationExpr) {
      if (granularity == PropertyGranularity.VIOLATING_EXPRESSION) {
        return ImmutableSet.<SafetyProperty>of(new AutomatonSafetyProperty(pViolationExpr));
      }

      return ImmutableSet.<SafetyProperty>of(new AutomatonSafetyProperty());
    }

  }

  @Nullable private Automaton automaton; // Has to be set delayed because of the parsing process!
  @Nonnull private final StringExpression violationDescriptionExpression;

  public AutomatonSafetyProperty(StringExpression pViolationExpr) {
    this.automaton = null;
    this.violationDescriptionExpression = Preconditions.checkNotNull(pViolationExpr);
  }

  public AutomatonSafetyProperty(Automaton pAutomaton) {
    this.automaton = pAutomaton;
    this.violationDescriptionExpression = StringExpression.empty();
  }

  public AutomatonSafetyProperty() {
    this.automaton = null;
    this.violationDescriptionExpression = StringExpression.empty();
  }

  @Override
  public void setAutomaton(Automaton pAutomaton) {
    automaton = Preconditions.checkNotNull(pAutomaton);
  }

  public StringExpression getViolationDescriptionExpression() {
    return violationDescriptionExpression;
  }

  @Override
  public ResultValue<?> instantiate(AutomatonExpressionArguments pArgs) {
    return violationDescriptionExpression.eval(pArgs);
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(automaton.getName());

    if (violationDescriptionExpression.getRawExpression().length() > 0) {
      result.append(" / ");
      result.append(violationDescriptionExpression.getRawExpression());
    }

   return result.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (automaton == null ? 0 : automaton.hashCode());
    result = prime * result + violationDescriptionExpression.hashCode();
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

    if (automaton == null) {
      if (other.automaton != null) {
        return false;
      }
    } else if (!automaton.equals(other.automaton)) {
      return false;
    }

    return true;
  }

}
