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
package org.sosy_lab.cpachecker.core.defaults;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

public class VariableTrackingPrecision implements Precision {


  /**
   * the component responsible for variables that need to be tracked, according to refinement
   */
  private RefinablePrecision refinablePrecision = null;
  private VariableTrackingPrecisionOptions options;

  private final Optional<VariableClassification> varClass;

  public VariableTrackingPrecision(VariableTrackingPrecisionOptions pOptions,
      Optional<VariableClassification> vc, RefinablePrecision pRefinablePrecision)
          throws InvalidConfigurationException {

    options            = pOptions;
    varClass           = vc;
    refinablePrecision = pRefinablePrecision;
  }

  public VariableTrackingPrecision(VariableTrackingPrecisionOptions pOptions,
      Optional<VariableClassification> vc)
          throws InvalidConfigurationException {

    varClass         = vc;
    options          = pOptions;

    switch (options.getSharingStrategy()) {
    case SCOPE:
      refinablePrecision = new ScopedRefinablePrecision();
      break;
    case LOCATION:
      refinablePrecision = new LocalizedRefinablePrecision();
      break;
    default:
      throw new AssertionError("Unhandled enumerator for sharing strategy: " + options.getSharingStrategy());
    }
  }

  /**
   * This constructor is used for refining the refinable component precision with the given increment.
   *
   * @param original the value-analysis precision to refine
   * @param increment the increment to refine with
   */
  public VariableTrackingPrecision(VariableTrackingPrecision original, Multimap<CFANode, MemoryLocation> increment) {
    // refine the refinable component precision with the given increment
    refinablePrecision = original.refinablePrecision.refine(increment);

    // copy remaining fields from original
    varClass           = original.varClass;
    options            = original.options;
  }

  /**
   * This method acts as factory method for a default precision object, using the standard, i.e. empty, configuration,
   * no variable classification and a full, i.e., non-refinable precision.
   *
   * @return the default precision object
   * @throws InvalidConfigurationException
   */
  public static VariableTrackingPrecision createDefaultPrecision() throws InvalidConfigurationException {
    return new VariableTrackingPrecision(VariableTrackingPrecisionOptions.getDefaultOptions(),
        Optional.<VariableClassification>absent(),
        new VariableTrackingPrecision.FullPrecision());
  }

  /**
   * This method determines if this precision allows for abstraction, i.e., if
   * it ignores variables from some variable class, if it maintains a refinable
   * precision, or if it contains a variable blacklist.
   *
   * @return true, if this precision allows for abstraction, else false
   */
  public boolean allowsAbstraction() {
     return !options.trackBooleanVariables()
         || !options.trackIntAddVariables()
         || !options.trackIntEqualVariables()
         || !(refinablePrecision instanceof FullPrecision)
         || !options.getVariableBlacklist().toString().isEmpty();
  }

  public RefinablePrecision getRefinablePrecision() {
    return refinablePrecision;
  }

  boolean isOnBlacklist(String variable) {
    return options.getVariableBlacklist().matcher(variable).matches();
  }

  public boolean variableExceedsReachedSetThreshold(int numberOfDifferentValues) {
    return numberOfDifferentValues > options.getReachedSetThreshold();
  }

  public boolean isReachedSetThresholdActive() {
    return options.getReachedSetThreshold() > -1;
  }

  public int getSize() {
    return refinablePrecision.getSize();
  }

  @Override
  public String toString() {
    return refinablePrecision.toString();
  }

  /**
   * This method tells if the precision demands the given variable to be tracked.
   *
   * A variable is demanded to be tracked if it does not exceed a threshold (when given),
   * it is on the white-list (when not null), and is not on the black-list.
   *
   * @param variable the scoped name of the variable to check
   * @return true, if the variable has to be tracked, else false
   */
  public boolean isTracking(MemoryLocation variable) {
    boolean result = refinablePrecision.contains(variable)
            && !isOnBlacklist(variable.getIdentifier())
            && !isInIgnoredVarClass(variable);

    return result;
  }

  public boolean isTracking(MemoryLocation variable, CType type) {
    if (options.trackFloatVariables()) {
      return isTracking(variable);
    } else {
      return !(type instanceof CSimpleType
          && (((CSimpleType)type).getType() == CBasicType.FLOAT
          || ((CSimpleType)type).getType() == CBasicType.DOUBLE))
      && isTracking(variable);
    }
  }

  /** returns true, iff the variable is in an varClass, that should be ignored. */
  private boolean isInIgnoredVarClass(final MemoryLocation variable) {
    if (varClass==null || !varClass.isPresent()) { return false; }

    final boolean isBoolean = varClass.get().getIntBoolVars().contains(variable.getAsSimpleString());
    final boolean isIntEqual = varClass.get().getIntEqualVars().contains(variable.getAsSimpleString());
    final boolean isIntAdd = varClass.get().getIntAddVars().contains(variable.getAsSimpleString());

    final boolean isIgnoredBoolean = !options.trackBooleanVariables() && isBoolean;
    final boolean isIgnoredIntEqual = !options.trackIntEqualVariables() && isIntEqual;
    final boolean isIgnoredIntAdd = !options.trackIntAddVariables() && isIntAdd;

    return isIgnoredBoolean || isIgnoredIntEqual || isIgnoredIntAdd;
  }

  abstract public static class RefinablePrecision {
    public static final String DELIMITER = ", ";

    /**
     * the current location needed for checking containment
     */
    CFANode location = null;

    /**
     * This method sets the location for the refinable precision.
     *
     * @param node the location to be set
     */
    public void setLocation(CFANode node) {
      location = node;
    }

    /**
     * This method decides whether or not a variable is being tracked by this precision.
     *
     * @param variable the scoped name of the variable for which to make the decision
     * @return true, when the variable is allowed to be tracked, else false
     */
    abstract public boolean contains(MemoryLocation variable);

    /**
     * This method refines the precision with the given increment.
     *
     * @param increment the increment to refine the precision with
     * @return the refined precision
     */
    public abstract RefinablePrecision refine(Multimap<CFANode, MemoryLocation> increment);

    /**
     * This method returns the size of the refinable precision, i.e., the number of elements contained.
     * @return
     */
    abstract int getSize();

    /**
     * This method transforms the precision and writes it using the given writer.
     *
     * @param writer the write to write the precision to
     * @throws IOException
     */
    public abstract void serialize(Writer writer) throws IOException;

    /**
     * This method joins this precision with another precision
     *
     * @param otherPrecision the precision to join with
     */
    public abstract void join(RefinablePrecision otherPrecision);
  }

  public static class LocalizedRefinablePrecision extends RefinablePrecision {
    /**
     * the collection that determines which variables are tracked at a specific location - if it is null, all variables are tracked
     */
    private HashMultimap<CFANode, MemoryLocation> rawPrecision = HashMultimap.create();

    @Override
    public LocalizedRefinablePrecision refine(Multimap<CFANode, MemoryLocation> increment) {
      if (this.rawPrecision.entries().containsAll(increment.entries())) {
        return this;
      } else {
        LocalizedRefinablePrecision refinedPrecision = new LocalizedRefinablePrecision();

        refinedPrecision.rawPrecision = HashMultimap.create(rawPrecision);
        refinedPrecision.rawPrecision.putAll(increment);

        return refinedPrecision;
      }
    }

    @Override
    public boolean contains(MemoryLocation variable) {
      return rawPrecision.containsEntry(location, variable);
    }

    @Override
    public
    void serialize(Writer writer) throws IOException {
      for (CFANode currentLocation : rawPrecision.keySet()) {
        writer.write("\n" + currentLocation + ":\n");

        for (MemoryLocation variable : rawPrecision.get(currentLocation)) {
          writer.write(variable.serialize() + "\n");
        }
      }
    }

    @Override
    public void join(RefinablePrecision consolidatedPrecision) {
      assert (getClass().equals(consolidatedPrecision.getClass()));
      this.rawPrecision.putAll(((LocalizedRefinablePrecision)consolidatedPrecision).rawPrecision);
    }

    @Override
    int getSize() {
      return rawPrecision.size();
    }

    @Override
    public String toString() {
      return TreeMultimap.create(rawPrecision).toString();
    }
  }

  public static class ScopedRefinablePrecision extends RefinablePrecision {
    /**
     * the collection that determines which variables are tracked within a specific scope
     */
    private Set<MemoryLocation> rawPrecision = new HashSet<>();

    @Override
    public boolean contains(MemoryLocation variable) {
      return rawPrecision.contains(variable);
    }

    @Override
    public ScopedRefinablePrecision refine(Multimap<CFANode, MemoryLocation> increment) {
      if (this.rawPrecision.containsAll(increment.values())) {
        return this;
      } else {
        ScopedRefinablePrecision refinedPrecision = new ScopedRefinablePrecision();

        refinedPrecision.rawPrecision = new HashSet<>(rawPrecision);
        refinedPrecision.rawPrecision.addAll(increment.values());
        return refinedPrecision;
      }
    }

    @Override
    public
    void serialize(Writer writer) throws IOException {
      SortedSet<MemoryLocation> sortedPrecision = new TreeSet<>(rawPrecision);

      List<String> globals = new ArrayList<>();
      String previousScope = null;

      for (MemoryLocation variable : sortedPrecision) {
        if (variable.isOnFunctionStack()) {
          String functionName = variable.getFunctionName();
          if (!functionName.equals(previousScope)) {
            writer.write("\n" + functionName + ":\n");
          }
          writer.write(variable.serialize() + "\n");

          previousScope = functionName;
        } else {
          globals.add(variable.serialize());
        }
      }

      if (previousScope != null) {
        writer.write("\n");
      }

      writer.write("*:\n" + Joiner.on("\n").join(globals));
    }

    @Override
    public void join(RefinablePrecision consolidatedPrecision) {
      assert (getClass().equals(consolidatedPrecision.getClass()));
      this.rawPrecision.addAll(((ScopedRefinablePrecision)consolidatedPrecision).rawPrecision);
    }

    @Override
    int getSize() {
      return rawPrecision.size();
    }

    @Override
    public String toString() {
      return new TreeSet<>(rawPrecision).toString();
    }
  }

  public static class FullPrecision extends RefinablePrecision {
    @Override
    public boolean contains(MemoryLocation variable) {
      return true;
    }

    @Override
    public FullPrecision refine(Multimap<CFANode, MemoryLocation> additionalMapping) {
      return this;
    }

    @Override
    public void serialize(Writer writer) throws IOException {
      writer.write("# full precision used - nothing to show here");
    }

    @Override
    public void join(RefinablePrecision consolidatedPrecision) {
      assert (getClass().equals(consolidatedPrecision.getClass()));
    }

    @Override
    int getSize() {
      return -1;
    }
  }
}
