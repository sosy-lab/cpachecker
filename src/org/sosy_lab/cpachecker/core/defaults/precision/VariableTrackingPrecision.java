/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.defaults.precision;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;
import com.google.errorprone.annotations.ForOverride;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

public abstract class VariableTrackingPrecision implements Precision {

  /**
   * This method creates a precision which cannot be refined, all decisions about
   * the tracking of variables depend on the configuration options and the variable
   * classification.
   */
  public static VariableTrackingPrecision createStaticPrecision(Configuration config, Optional<VariableClassification> vc, Class<? extends ConfigurableProgramAnalysis> cpaClass)
          throws InvalidConfigurationException {
    return new ConfigurablePrecision(config, vc, cpaClass);
  }

  /**
   * This method iterates of every state of the reached set and joins their respective precision into one map.
   *
   * @param reached the set of reached states
   * @return the join over precisions of states in the reached set
   */
  public static VariableTrackingPrecision joinVariableTrackingPrecisionsInReachedSet(UnmodifiableReachedSet reached) {
    Preconditions.checkArgument(reached != null);
    VariableTrackingPrecision joinedPrecision = null;
    for (Precision precision : reached.getPrecisions()) {
      VariableTrackingPrecision prec =
          Precisions.extractPrecisionByType(precision, VariableTrackingPrecision.class);
      if (prec != null) {
        if (joinedPrecision == null) {
          joinedPrecision = prec;
        } else {
          joinedPrecision = joinedPrecision.join(prec);
        }
      }
    }
    return joinedPrecision;
  }

  /**
   * This method creates a refinable precision. The baseline should usually be
   * a static precision, where the most configuration options are handled.
   *
   * @param pBaseline The precision which should be used as baseline.
   */
  public static VariableTrackingPrecision createRefineablePrecision(Configuration config, VariableTrackingPrecision pBaseline) throws InvalidConfigurationException {
    Preconditions.checkNotNull(pBaseline);
    RefinablePrecisionOptions options = new RefinablePrecisionOptions(config);
    switch (options.sharing) {
    case LOCATION:
      return new LocalizedRefinablePrecision(pBaseline);
    case SCOPE:
      return new ScopedRefinablePrecision(pBaseline);
      default:
        throw new AssertionError("Unhandled case in switch statement");
    }
  }

  public static Predicate<Precision> isMatchingCPAClass(final Class<? extends ConfigurableProgramAnalysis> cpaClass) {
     return pPrecision -> pPrecision instanceof VariableTrackingPrecision
         && ((VariableTrackingPrecision) pPrecision).getCPAClass() == cpaClass;
  }

  /**
   * This method determines if this precision allows for abstraction, i.e., if
   * it ignores variables from some variable class, if it maintains a refinable
   * precision, or if it contains a variable blacklist.
   *
   * @return true, if this precision allows for abstraction, else false
   */
  public abstract boolean allowsAbstraction();

  /**
   * This method tells if the precision demands the given variable to be tracked.
   *
   * A variable is demanded to be tracked if
   * it is on the white-list (when not null), and is not on the black-list.
   *
   * @param variable the scoped name of the variable to check
   * @param pType the type of the variable, necessary for checking if the variable
   *              should be handled (necessary for floats / doubles)
   * @param location the location of the variable
   * @return true, if the variable has to be tracked, else false
   */
  public abstract boolean isTracking(MemoryLocation variable, Type pType, CFANode location);

  /**
   * This method refines the precision with the given increment.
   *
   * @param increment the increment to refine the precision with
   * @return the refined precision
   */
  public abstract VariableTrackingPrecision withIncrement(Multimap<CFANode, MemoryLocation> increment);

  /**
   * This method returns the size of the refinable precision, i.e., the number of elements contained.
   */
  public abstract int getSize();

  /**
   * This method transforms the precision and writes it using the given writer.
   *
   * @param writer the write to write the precision to
   */
  public abstract void serialize(Writer writer) throws IOException;

  /**
   * This method joins this precision with another precision
   *
   * @param otherPrecision the precision to join with
   */
  public abstract VariableTrackingPrecision join(VariableTrackingPrecision otherPrecision);

  /**
   * This methods compares if this precision tracks the same variables as another precision.
   * Only precisions of the same class can track the same variables.
   *
   * @param otherPrecision the precision to compare the tracking behavior
   */
  public abstract boolean tracksTheSameVariablesAs(VariableTrackingPrecision otherPrecision);

  /**
   * This method checks if the caller precision is empty, thus there is
   * no variable that should be tracked.
   *
   * @return indicates whether there are variables that should be tracked or not
   */
  public abstract boolean isEmpty();

  /**
   * This method returns the CPA class to which this Precision belongs. This way
   * more CPAs can have a VariableTrackingPrecision without interfering with
   * each other.
   *
   * @return the owner CPA of this precision
   */
  @ForOverride
  protected abstract Class<? extends ConfigurableProgramAnalysis> getCPAClass();

  @Override
  abstract public boolean equals(Object other);

  @Override
  abstract public int hashCode();

  @Options(prefix="precision")
  private static class RefinablePrecisionOptions {

    enum Sharing {
      SCOPE,
      LOCATION
    }

    @Option(secure=true, description = "whether to track relevant variables only at the exact "
        + "program location (sharing=location), or within their respective"
        + " (function-/global-) scope (sharing=scoped).")
    private Sharing sharing = Sharing.SCOPE;

    private RefinablePrecisionOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }
  }
}
