// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

/** This class collects some configurations options for the C-to-formula encoding process. */
@Options(prefix = "cpa.predicate")
public class FormulaEncodingOptions {

  @Option(
      secure = true,
      description = "Handle field access via extract and concat instead of new variables.")
  private boolean handleFieldAccess = false;

  @Option(
      secure = true,
      description =
          "Set of functions that should be considered as giving "
              + "a non-deterministic return value. "
              + "If you specify this option, the default values are not added automatically "
              + "to the list, so you need to specify them explicitly if you need them. "
              + "Mentioning a function in this list has only an effect, if it is an "
              + "'external function', i.e., no source is given in the code for this function.")
  private Set<String> nondetFunctions = ImmutableSet.of("sscanf", "rand", "random");

  @Option(
      secure = true,
      description =
          "Regexp pattern for functions that should be considered as giving "
              + "a non-deterministic return value (c.f. cpa.predicate.nondedFunctions)")
  private Pattern nondetFunctionsRegexp = Pattern.compile("^(__VERIFIER_)?nondet_[a-zA-Z0-9_]*");

  @Option(
      secure = true,
      description =
          "Name of an external function that will be interpreted as if the function call would be"
              + " replaced by an externally defined expression over the program variables. This"
              + " will only work when all variables referenced by the dimacs file are global and"
              + " declared before this function is called.")
  private String externModelFunctionName = "__VERIFIER_externModelSatisfied";

  @Option(
      secure = true,
      description =
          "Set of functions that non-deterministically provide new memory on the heap, "
              + "i.e. they can return either a valid pointer or zero.")
  private Set<String> memoryAllocationFunctions =
      ImmutableSet.of("malloc", "__kmalloc", "kmalloc", "alloca", "__builtin_alloca");

  @Option(
      secure = true,
      description =
          "Set of functions that non-deterministically provide new zeroed memory on the heap, "
              + "i.e. they can return either a valid pointer or zero.")
  private Set<String> memoryAllocationFunctionsWithZeroing = ImmutableSet.of("kzalloc", "calloc");

  @Option(
      secure = true,
      description =
          "Allow the given extern functions and interpret them as pure functions"
              + " although the predicate analysis does not support their semantics"
              + " and this can produce wrong results.")
  private Set<String> allowedUnsupportedFunctions = ImmutableSet.of();

  @Option(
      secure = true,
      description = "Ignore variables that are not relevant for reachability properties.")
  private boolean ignoreIrrelevantVariables = true;

  @Option(
      secure = true,
      description =
          "Do not ignore variables that could lead to an overflow (only makes sense if"
              + " ignoreIrrelevantVariables is set to true)")
  private boolean overflowVariablesAreRelevant = false;

  @Option(
      secure = true,
      description =
          "Ignore fields that are not relevant for reachability properties. This is unsound in case"
              + " fields are accessed by pointer arithmetic with hard-coded field offsets. Only"
              + " relvant if ignoreIrrelevantVariables is enabled.")
  private boolean ignoreIrrelevantFields = true;

  @Option(
      secure = true,
      description = "Whether to track values stored in variables of function-pointer type.")
  private boolean trackFunctionPointers = true;

  @Option(
      secure = true,
      description =
          "Whether to give up immediately if a very large array is encountered (heuristic, often we"
              + " would just waste time otherwise)")
  private boolean abortOnLargeArrays = true;

  @Option(
      secure = true,
      description =
          "Insert tmp-variables for parameters at function-entries. "
              + "The variables are similar to return-variables at function-exit.")
  private boolean useParameterVariables = false;

  @Option(
      secure = true,
      description =
          "Insert tmp-parameters for global variables at function-entries. "
              + "The global variables are also encoded with return-variables at function-exit.")
  private boolean useParameterVariablesForGlobals = false;

  @Option(
      secure = true,
      description =
          "Add constraints for the range of the return-value of a nondet-method. For example the"
              + " assignment 'X=nondet_int()' produces the constraint 'MIN<=X<=MAX', where MIN and"
              + " MAX are computed from the type of the method (signature, not name!).")
  private boolean addRangeConstraintsForNondet = false;

  @Option(
      secure = true,
      description =
          "Replace possible overflows with an ITE-structure, "
              + "which returns either the normal value or an UF representing the overflow.")
  private boolean encodeOverflowsWithUFs = false;

  @Option(
      secure = true,
      description =
          "For multithreaded programs this is an overapproximation of possible values of shared"
              + " variables.")
  private boolean useHavocAbstraction = false;

  public FormulaEncodingOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this, FormulaEncodingOptions.class);
  }

  public boolean handleFieldAccess() {
    return handleFieldAccess;
  }

  public boolean isNondetFunction(String function) {
    return nondetFunctions.contains(function) || nondetFunctionsRegexp.matcher(function).matches();
  }

  public boolean isMemoryAllocationFunction(String function) {
    return memoryAllocationFunctions.contains(function);
  }

  public boolean isExternModelFunction(String function) {
    return function.equals(externModelFunctionName);
  }

  public boolean isMemoryAllocationFunctionWithZeroing(final String name) {
    return memoryAllocationFunctionsWithZeroing.contains(name);
  }

  public boolean isAllowedUnsupportedFunction(final String function) {
    return allowedUnsupportedFunctions.contains(function);
  }

  public boolean ignoreIrrelevantVariables() {
    return ignoreIrrelevantVariables;
  }

  public boolean overflowVariablesAreRelevant() {
    return overflowVariablesAreRelevant;
  }

  public boolean ignoreIrrelevantFields() {
    return ignoreIrrelevantFields;
  }

  public boolean trackFunctionPointers() {
    return trackFunctionPointers;
  }

  public boolean shouldAbortOnLargeArrays() {
    return abortOnLargeArrays;
  }

  public boolean useParameterVariables() {
    return useParameterVariables;
  }

  public boolean useParameterVariablesForGlobals() {
    return useParameterVariablesForGlobals;
  }

  public boolean addRangeConstraintsForNondet() {
    return addRangeConstraintsForNondet;
  }

  public boolean encodeOverflowsWithUFs() {
    return encodeOverflowsWithUFs;
  }

  public boolean useHavocAbstraction() {
    return useHavocAbstraction;
  }
}
