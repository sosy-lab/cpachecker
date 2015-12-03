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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@Options(prefix="automata.properties")
class AutomatonSafetyPropertyFactory {

  public static enum PropertyGranularity {
    /** One automata definition file encodes one property. */
    BASENAME,

    /** One automaton encodes exactly one property. */
    AUTOMATON,

    /** One automaton can encode multiple properties that are distinguished based on the violating (string) expression. */
    VIOLATING_EXPRESSION
  }

  @Option(description="Granularity of safety properties that are encoded in automata.")
  private PropertyGranularity granularity = PropertyGranularity.AUTOMATON;

  private final String propertyBasename;

  public AutomatonSafetyPropertyFactory(Configuration pConfig, String pPropertyBaseName) throws InvalidConfigurationException {
    pConfig.inject(this);

    this.propertyBasename = pPropertyBaseName;
  }

  public Set<SafetyProperty> createSingleProperty() {

    Optional<Automaton> automaton;
    if (granularity == PropertyGranularity.AUTOMATON) {
      automaton = null; // Will be set delayed!
    } else {
      automaton = Optional.<Automaton>absent();
    }

    StringExpression expression = StringExpression.empty();

    return ImmutableSet.<SafetyProperty>of(
        new AutomatonSafetyProperty(propertyBasename,
            automaton,
            expression));
  }

  public Set<SafetyProperty> createSingleProperty(StringExpression pViolationExpr) {

    Optional<Automaton> automaton;
    if (granularity == PropertyGranularity.AUTOMATON) {
      automaton = null; // Will be set delayed!
    } else {
      automaton = Optional.<Automaton>absent();
    }

    StringExpression expression;
    if (granularity == PropertyGranularity.VIOLATING_EXPRESSION) {
      expression = Preconditions.checkNotNull(pViolationExpr);
    } else {
      expression = StringExpression.empty();
    }

    return ImmutableSet.<SafetyProperty>of(
        new AutomatonSafetyProperty(propertyBasename,
            automaton,
            expression));
  }

  public Set<SafetyProperty> createAssertionProperties(
      @Nullable Collection<AutomatonBoolExpr> pAssertion) {

    HashSet<SafetyProperty> result = Sets.newHashSet();

    if (pAssertion != null) {
      for (AutomatonBoolExpr expr: pAssertion) {
        result.addAll(createAssertionProperty(expr));
      }
    }

    return result;
  }

  public Set<SafetyProperty> createAssertionProperty(AutomatonBoolExpr pAssertion) {
    Optional<Automaton> automaton;
    if (granularity == PropertyGranularity.AUTOMATON) {
      automaton = null; // Will be set delayed!
    } else {
      automaton = Optional.<Automaton>absent();
    }

    AutomatonBoolExpr expression;
    if (granularity == PropertyGranularity.VIOLATING_EXPRESSION) {
      expression = Preconditions.checkNotNull(pAssertion);
    } else {
      expression = AutomatonBoolExpr.TRUE;
    }

    return ImmutableSet.<SafetyProperty>of(
        new AutomatonAssertionProperty(propertyBasename,
            automaton,
            expression));
  }

  private abstract static class AbstractSafetyProperty<E> implements SafetyProperty {

    @Nullable protected Optional<Automaton> automaton = null; // Has to be set delayed because of the parsing process!

    @Nonnull protected final String basename;

    @Nonnull protected final E violationDescriptionExpression;

    public AbstractSafetyProperty(String pPropertyBasename, Optional<Automaton> pAutomaton, E pViolationExpr) {
      this.basename = Preconditions.checkNotNull(pPropertyBasename);
      this.violationDescriptionExpression = pViolationExpr;
      this.automaton = pAutomaton;
    }

    protected abstract E createEmptyExpression();

    @Override
    public void setAutomaton(Automaton pAutomaton) {
      Preconditions.checkNotNull(pAutomaton);

      if (automaton == null) {
        automaton = Optional.of(pAutomaton);
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + basename.hashCode();
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
      AbstractSafetyProperty<?> other = (AbstractSafetyProperty<?>) obj;

      if (!violationDescriptionExpression.equals(other.violationDescriptionExpression)) {
        return false;
      }

      if (!basename.equals(other.basename)) {
        return false;
      }

      if (this.automaton == null) {
        if (other.automaton != null) {
          return false;
        }
      } else if (!automaton.equals(other.automaton)) {
        return false;
      }

      return true;
    }
  }

  public static class AutomatonSafetyProperty extends AbstractSafetyProperty<StringExpression> {

    public AutomatonSafetyProperty(String pPropertyBasename, Optional<Automaton> pAutomaton,
        StringExpression pViolationExpr) {
      super(pPropertyBasename, pAutomaton, pViolationExpr);
    }

    @Override
    public ResultValue<?> instantiate(AutomatonExpressionArguments pArgs) {
      return violationDescriptionExpression.eval(pArgs);
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append(basename.toString());
      if (automaton.isPresent()) {
        result.append(" / ");
        result.append(automaton.get().getName());
      }

      if (violationDescriptionExpression.getRawExpression().length() > 0) {
        result.append(" / ");
        result.append(violationDescriptionExpression.getRawExpression());
      }

     return result.toString();
    }

    @Override
    protected StringExpression createEmptyExpression() {
      return StringExpression.empty();
    }

  }

  public static class AutomatonAssertionProperty extends AbstractSafetyProperty<AutomatonBoolExpr> {

    public AutomatonAssertionProperty(String pPropertyBasename, Optional<Automaton> pAutomaton,
        AutomatonBoolExpr pViolationExpr) {
      super(pPropertyBasename, pAutomaton, pViolationExpr);
    }

    @Override
    public ResultValue<?> instantiate(AutomatonExpressionArguments pArgs) {
      String failureLocation;
      if (pArgs.getCfaEdge().getFileLocation() != null) {
        failureLocation = pArgs.getCfaEdge().getFileLocation().toString();
      } else {
        failureLocation = pArgs.getCfaEdge().getRawStatement();
      }

      return new ResultValue<>("Assertion failed!", failureLocation);
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append(basename.toString());
      if (automaton.isPresent()) {
        result.append(" / ");
        result.append(automaton.get().getName());
      }

      if (!violationDescriptionExpression.equals(AutomatonBoolExpr.TRUE)) {
        result.append(" / ");
        result.append(violationDescriptionExpression.toString());
      }

     return result.toString();
    }

    @Override
    protected AutomatonBoolExpr createEmptyExpression() {
      return AutomatonBoolExpr.TRUE;
    }

  }


}