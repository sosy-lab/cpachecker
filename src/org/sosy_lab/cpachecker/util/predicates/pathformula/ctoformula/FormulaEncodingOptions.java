/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.util.Set;
import java.util.regex.Pattern;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import com.google.common.collect.ImmutableSet;

/**
 * This class collects some configurations options for
 * the C-to-formula encoding process.
 */
@Options(prefix="cpa.predicate")
public class FormulaEncodingOptions {

  @Option(description = "Handle field access via extract and concat instead of new variables.")
  private boolean handleFieldAccess = false;

  @Option(description = "Handle pointer aliasing for pointers with unknown values "
      + "(coming from uninitialized variables or external function calls). "
      + "This is slow and provides little benefit.")
  private boolean handleNondetPointerAliasing = false;

  @Option(description="Set of functions that should be considered as giving "
    + "a non-deterministic return value. "
    + "If you specify this option, the default values are not added automatically "
    + "to the list, so you need to specify them explicitly if you need them. "
    + "Mentioning a function in this list has only an effect, if it is an "
    + "'external function', i.e., no source is given in the code for this function.")
  private Set<String> nondetFunctions = ImmutableSet.of(
      "sscanf",
      "random");

  @Option(description="Regexp pattern for functions that should be considered as giving "
    + "a non-deterministic return value (c.f. cpa.predicate.nondedFunctions)")
  private Pattern nondetFunctionsRegexp = Pattern.compile("^(__VERIFIER_)?nondet_[a-zA-Z0-9_]*");

  @Option(description="Name of an external function that will be interpreted as if the function "
     + "call would be replaced by an externally defined expression over the program variables."
     + " This will only work when all variables referenced by the dimacs file are global and declared before this function is called.")
  private String externModelFunctionName = "__VERIFIER_externModelSatisfied";

  @Option(description = "Set of functions that non-deterministically provide new memory on the heap, " +
  		                  "i.e. they can return either a valid pointer or zero.")
  private Set<String> memoryAllocationFunctions = ImmutableSet.of(
      "malloc", "__kmalloc", "kmalloc"
      );

  @Option(description = "Set of functions that non-deterministically provide new zeroed memory on the heap, " +
                        "i.e. they can return either a valid pointer or zero.")
  private Set<String> memoryAllocationFunctionsWithZeroing = ImmutableSet.of("kzalloc", "calloc");

  public FormulaEncodingOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this, FormulaEncodingOptions.class);
  }

  public boolean handleFieldAccess() {
    return handleFieldAccess;
  }

  public boolean handleNondetPointerAliasing() {
    return handleNondetPointerAliasing;
  }

  public boolean isNondetFunction(String function) {
    return nondetFunctions.contains(function)
        || nondetFunctionsRegexp.matcher(function).matches();
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
}
