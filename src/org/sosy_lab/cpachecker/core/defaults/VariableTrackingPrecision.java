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

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.errorprone.annotations.ForOverride;

public abstract class VariableTrackingPrecision implements Precision {

  /**
   * This method creates a precision which cannot be refined, all decisions about
   * the tracking of variables depend on the configuration options and the variable
   * classification.
   *
   * @param config
   * @param vc the variable classification that should be used
   * @return
   * @throws InvalidConfigurationException
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
  public static VariableTrackingPrecision joinVariableTrackingPrecisionsInReachedSet(ReachedSet reached) {
    Preconditions.checkArgument(reached != null);
    VariableTrackingPrecision joinedPrecision = null;
    for (Precision precision : reached.getPrecisions()) {
      if (precision instanceof WrapperPrecision) {
        VariableTrackingPrecision prec = ((WrapperPrecision)precision).retrieveWrappedPrecision(VariableTrackingPrecision.class);
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
   * @param config
   * @param pBaseline The precision which should be used as baseline.
   * @return
   * @throws InvalidConfigurationException
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
     return new Predicate<Precision>() {

      @Override
      public boolean apply(Precision pPrecision) {
        if (!(pPrecision instanceof VariableTrackingPrecision)) {
          return false;
        }
          return ((VariableTrackingPrecision)pPrecision).getCPAClass() == cpaClass;
      }};
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
   * @return
   */
  public abstract int getSize();

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
  public abstract VariableTrackingPrecision join(VariableTrackingPrecision otherPrecision);

  /**
   * This methods compares if this precision tracks the same variables as another precision.
   * Only precisions of the same class can track the same variables.
   *
   * @param otherPrecision the precision to compare the tracking behavior
   * @return
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

  @Options(prefix="precision")
  public static class ConfigurablePrecision extends VariableTrackingPrecision{

    @Option(secure=true, name="variableBlacklist",
        description="blacklist regex for variables that won't be tracked by the CPA using this precision")
    private Pattern variableBlacklist = Pattern.compile("");

    @Option(secure = true, name="variableWhitelist",
        description="whitelist regex for variables that will always be tracked by the CPA using this precision")
    private Pattern variableWhitelist = Pattern.compile("");

    @Option(secure=true, description = "If this option is used, booleans from the cfa are tracked.")
    private boolean trackBooleanVariables = true;

    @Option(secure=true, description = "If this option is used, variables that are only compared"
        + " for equality are tracked.")
    private boolean trackIntEqualVariables = true;

    @Option(secure=true, description = "If this option is used, variables, that are only used in"
        + " simple calculations (add, sub, lt, gt, eq) are tracked.")
    private boolean trackIntAddVariables = true;

    @Option(secure=true, description ="If this option is used, variables that have type double"
        + " or float are tracked.")
    private boolean trackFloatVariables = true;

    @Option(secure=true, description ="If this option is used, variables that are addressed"
        + " may get tracked depending on the rest of the precision. When this option"
        + " is disabled, a variable that is addressed is definitely not tracked.")
    private boolean trackAddressedVariables = true;

    @Option(secure=true, description ="If this option is used, all variables that are"
        + " of a different classification than IntAdd, IntEq and Boolean get tracked"
        + " by the precision.")
    private boolean trackVariablesBesidesEqAddBool = true;

    private Optional<VariableClassification> vc;
    private final Class<? extends ConfigurableProgramAnalysis> cpaClass;

    private ConfigurablePrecision(Configuration config, Optional<VariableClassification> pVc, Class<? extends ConfigurableProgramAnalysis> cpaClass) throws InvalidConfigurationException {
      super();
      config.inject(this);
      this.cpaClass = cpaClass;
      this.vc = pVc;
    }

    @Override
    public boolean allowsAbstraction() {
    return !trackBooleanVariables
      || !trackIntEqualVariables
      || !trackIntAddVariables
      || !trackAddressedVariables
      || !trackVariablesBesidesEqAddBool
      || !variableBlacklist.toString().isEmpty();
    }

    @Override
    public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode location) {
      if (trackFloatVariables) {
        return isTracking(pVariable);
      } else {
        return !(pType instanceof CSimpleType
                    && (((CSimpleType)pType).getType() == CBasicType.FLOAT
                    || ((CSimpleType)pType).getType() == CBasicType.DOUBLE)
                  || pType instanceof JSimpleType
                    && (((JSimpleType)pType).getType() == JBasicType.FLOAT
                    || ((JSimpleType)pType).getType() == JBasicType.DOUBLE))
                && isTracking(pVariable);
      }
    }

    private boolean isTracking(MemoryLocation pVariable) {
      return  isOnWhitelist(pVariable.getIdentifier())
              || (!isOnBlacklist(pVariable.getIdentifier())
                  && isInTrackedVarClass(pVariable.getAsSimpleString()));
    }

    private boolean isOnBlacklist(String variable) {
      return !variableBlacklist.toString().isEmpty() && variableBlacklist.matcher(variable).matches();
    }

    private boolean isOnWhitelist(String variable) {
      return !variableWhitelist.toString().isEmpty() && variableWhitelist.matcher(variable).matches();
    }

    /** returns true, iff the variable is in an varClass, that should be ignored. */
    private boolean isInTrackedVarClass(final String variableName) {
      // when there is no variable classification we cannot make any assumptions
      // about the tracking of variables and say that all variables are tracked
      if (!vc.isPresent()) {
        return true;
      }
      VariableClassification varClass = vc.get();

      final boolean varIsAddressed = varClass.getAddressedVariables().contains(variableName);

      // addressed variables do not belong to a specific type, so they have to
      // be handled extra. We want the precision to be as strict as possible,
      // therefore, when a variable is addressed but addressed variables should
      // not be tracked, we do not consider the other parts of the variable classification
      if (varIsAddressed && !trackAddressedVariables) {
        return false;


        // in this case addressed variables can at most be included in the
        // tracking variables and the rest of the variable classification is
        // the limiting factor
      } else {
        final boolean varIsBoolean = varClass.getIntBoolVars().contains(variableName);
        final boolean varIsIntEqual = varClass.getIntEqualVars().contains(variableName);
        final boolean varIsIntAdd = varClass.getIntAddVars().contains(variableName);

        // if the variable is not in a matching classification we have to check
        // if other variables should be tracked
        if (!(varIsBoolean || varIsIntAdd || varIsIntEqual)) {
          return trackVariablesBesidesEqAddBool;
        }

        final boolean isTrackedBoolean = trackBooleanVariables && varIsBoolean;
        final boolean isTrackedIntEqual = trackIntEqualVariables && varIsIntEqual;
        final boolean isTrackedIntAdd = trackIntAddVariables && varIsIntAdd;

        return isTrackedBoolean || isTrackedIntAdd || isTrackedIntEqual;
      }
    }

    @Override
    public VariableTrackingPrecision withIncrement(Multimap<CFANode, MemoryLocation> pIncrement) {
      return this;
    }

    @Override
    public void serialize(Writer writer) throws IOException {
      writer.write("# configured precision used - nothing to show here");
    }

    @Override
    public VariableTrackingPrecision join(VariableTrackingPrecision consolidatedPrecision) {
      Preconditions.checkArgument((getClass().equals(consolidatedPrecision.getClass())));
      return this;
    }

    @Override
    public int getSize() {
      return -1;
    }

    @Override
    public boolean isEmpty() {
      if (!variableWhitelist.toString().isEmpty()) { return false; }
      if (!vc.isPresent()) { return true; }
      VariableClassification varClass = vc.get();

      boolean trackSomeIntBools = trackBooleanVariables &&
          !varClass.getIntBoolVars().isEmpty();
      boolean trackSomeIntEquals = trackIntEqualVariables &&
          !varClass.getIntEqualVars().isEmpty();
      boolean trackSomeIntAdds = trackIntAddVariables &&
          !varClass.getIntAddVars().isEmpty();

      return !(trackSomeIntBools || trackSomeIntEquals || trackSomeIntAdds || trackVariablesBesidesEqAddBool);
    }

    @Override
    protected Class<? extends ConfigurableProgramAnalysis> getCPAClass() {
      return cpaClass;
    }

    @Override
    public boolean tracksTheSameVariablesAs(VariableTrackingPrecision pOtherPrecision) {
      if (pOtherPrecision.getClass().equals(getClass())) {
        ConfigurablePrecision precisionCompare = (ConfigurablePrecision) pOtherPrecision;
        if (variableBlacklist.equals(precisionCompare.variableBlacklist)
            && variableWhitelist.equals(precisionCompare.variableWhitelist)
            && trackBooleanVariables == precisionCompare.trackBooleanVariables
            && trackIntEqualVariables == precisionCompare.trackIntEqualVariables
            && trackIntAddVariables == precisionCompare.trackIntAddVariables
            && trackFloatVariables == precisionCompare.trackFloatVariables
            && trackAddressedVariables == precisionCompare.trackAddressedVariables
            && vc.isPresent() == precisionCompare.vc.isPresent()
            && vc.isPresent() && vc.get().equals(precisionCompare.vc.get())
            && cpaClass.equals(precisionCompare.cpaClass)) {
          return true;
        }
      }
      return false;
    }

  }


  public static abstract class RefinablePrecision extends VariableTrackingPrecision {

    private VariableTrackingPrecision baseline;

    private RefinablePrecision(VariableTrackingPrecision pBaseline) {
      super();
      baseline = pBaseline;
    }

    @Override
    public final boolean allowsAbstraction() {
      return true;
    }

    @Override
    public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode pLocation) {
      checkNotNull(pVariable);
      checkNotNull(pType);
      checkNotNull(pLocation);
      return baseline.isTracking(pVariable, pType, pLocation);
    }

    protected VariableTrackingPrecision getBaseline() {
      return baseline;
    }

    @Override
    protected final Class<? extends ConfigurableProgramAnalysis> getCPAClass() {
      return baseline.getCPAClass();
    }
  }

  public static class LocalizedRefinablePrecision extends RefinablePrecision {
    /**
     * the collection that determines which variables are tracked at a specific location - if it is null, all variables are tracked
     */
    private final ImmutableMultimap<CFANode, MemoryLocation> rawPrecision;


    private LocalizedRefinablePrecision(VariableTrackingPrecision pBaseline) {
      super(pBaseline);
      rawPrecision = ImmutableMultimap.of();
    }

    private LocalizedRefinablePrecision(VariableTrackingPrecision pBaseline, ImmutableMultimap<CFANode, MemoryLocation> pRawPrecision) {
      super(pBaseline);
      rawPrecision = pRawPrecision;
    }

    @Override
    public LocalizedRefinablePrecision withIncrement(Multimap<CFANode, MemoryLocation> increment) {
      if (this.rawPrecision.entries().containsAll(increment.entries())) {
        return this;
      } else {
        // sorted multimap so that we have deterministic output
        SetMultimap<CFANode, MemoryLocation> refinedPrec = TreeMultimap.create(rawPrecision);
        refinedPrec.putAll(increment);

        return new LocalizedRefinablePrecision(super.baseline, ImmutableMultimap.copyOf(refinedPrec));
      }
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
    public VariableTrackingPrecision join(VariableTrackingPrecision consolidatedPrecision) {
      checkArgument(getClass().equals(consolidatedPrecision.getClass()));
      checkArgument(super.baseline.equals(((LocalizedRefinablePrecision)consolidatedPrecision).getBaseline()));

      SetMultimap<CFANode, MemoryLocation> joinedPrec = TreeMultimap.create(rawPrecision);
      joinedPrec.putAll(((LocalizedRefinablePrecision)consolidatedPrecision).rawPrecision);
      return new LocalizedRefinablePrecision(super.baseline, ImmutableMultimap.copyOf(joinedPrec));
    }

    @Override
    public int getSize() {
      return rawPrecision.size();
    }

    @Override
    public String toString() {
      return rawPrecision.toString();
    }

    @Override
    public boolean isEmpty() {
      return rawPrecision.isEmpty();
    }

    @Override
    public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode pLocation) {
      return super.isTracking(pVariable, pType, pLocation)
              && rawPrecision.containsEntry(pLocation, pVariable);
    }

    @Override
    public boolean tracksTheSameVariablesAs(VariableTrackingPrecision pOtherPrecision) {
      if (pOtherPrecision.getClass().equals(getClass())
          && super.baseline.equals(((LocalizedRefinablePrecision) pOtherPrecision).getBaseline())
          && rawPrecision.equals(((LocalizedRefinablePrecision) pOtherPrecision).rawPrecision)) {
        return true;
      }
      return false;
    }
  }

  public static class ScopedRefinablePrecision extends RefinablePrecision {
    /**
     * the collection that determines which variables are tracked within a specific scope
     */
    private ImmutableSortedSet<MemoryLocation> rawPrecision;

    private ScopedRefinablePrecision(VariableTrackingPrecision pBaseline) {
      super(pBaseline);
      rawPrecision = ImmutableSortedSet.of();
    }

    private ScopedRefinablePrecision(VariableTrackingPrecision pBaseline, ImmutableSortedSet<MemoryLocation> pRawPrecision) {
      super(pBaseline);
      rawPrecision = pRawPrecision;
    }

    @Override
    public ScopedRefinablePrecision withIncrement(Multimap<CFANode, MemoryLocation> increment) {
      if (this.rawPrecision.containsAll(increment.values())) {
        return this;
      } else {
        SortedSet<MemoryLocation> refinedPrec = new TreeSet<>(rawPrecision);
        refinedPrec.addAll(increment.values());

        return new ScopedRefinablePrecision(super.baseline, ImmutableSortedSet.copyOf(refinedPrec));
      }
    }

    @Override
    public
    void serialize(Writer writer) throws IOException {

      List<String> globals = new ArrayList<>();
      String previousScope = null;

      for (MemoryLocation variable : rawPrecision) {
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
    public VariableTrackingPrecision join(VariableTrackingPrecision consolidatedPrecision) {
      Preconditions.checkArgument((getClass().equals(consolidatedPrecision.getClass())));
      checkArgument(super.baseline.equals(((ScopedRefinablePrecision)consolidatedPrecision).getBaseline()));

      SortedSet<MemoryLocation> joinedPrec = new TreeSet<>(rawPrecision);
      joinedPrec.addAll(((ScopedRefinablePrecision)consolidatedPrecision).rawPrecision);
      return new ScopedRefinablePrecision(super.baseline, ImmutableSortedSet.copyOf(joinedPrec));
    }

    @Override
    public int getSize() {
      return rawPrecision.size();
    }

    @Override
    public String toString() {
      return rawPrecision.toString();
    }

    @Override
    public boolean isEmpty() {
      return rawPrecision.isEmpty();
    }

    @Override
    public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode pLocation) {
      return super.isTracking(pVariable, pType, pLocation)
              && rawPrecision.contains(pVariable);
    }

    @Override
    public boolean tracksTheSameVariablesAs(VariableTrackingPrecision pOtherPrecision) {
      if (pOtherPrecision.getClass().equals(getClass())
          && super.baseline.equals(((ScopedRefinablePrecision) pOtherPrecision).getBaseline())
          && rawPrecision.equals(((ScopedRefinablePrecision) pOtherPrecision).rawPrecision)) {
        return true;
      }
      return false;
    }
  }

}
