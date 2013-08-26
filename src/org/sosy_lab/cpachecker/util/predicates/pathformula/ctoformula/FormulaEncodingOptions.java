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
class FormulaEncodingOptions {

  @Option(description = "Handle aliasing of pointers. "
      + "This adds disjunctions to the formulas, so be careful when using cartesian abstraction.")
  private boolean handlePointerAliasing = true;

  @Option(description = "Handle field access via extract and concat instead of new variables.")
  private boolean handleFieldAccess = false;

  // if true, handle lvalues as *x, &x, s.x, etc. using UIFs. If false, just
  // use variables
  @Option(name="lvalsAsUIFs",
      description="use uninterpreted functions for *, & and array access")
  private boolean lvalsAsUif = false;

  @Option(description="list of functions that should be considered as giving "
    + "a non-deterministic return value\n Only predicate analysis honors this option. "
    + "If you specify this option, the default values are not added automatically "
    + "to the list, so you need to specify them explicitly if you need them. "
    + "Mentioning a function in this list has only an effect, if it is an "
    + "'external function', i.e., no source is given in the code for this function.")
  private Set<String> nondetFunctions = ImmutableSet.of(
      "malloc", "__kmalloc", "kzalloc",
      "sscanf",
      "random");

  @Option(description="Regexp pattern for functions that should be considered as giving "
    + "a non-deterministic return value (c.f. cpa.predicate.nondedFunctions)")
  private String nondetFunctionsRegexp = "^(__VERIFIER_)?nondet_[a-z]*";
  private final Pattern nondetFunctionsPattern;

  @Option(description="Name of an external function that will be interpreted as if the function "
     + "call would be replaced by an externally defined expression over the program variables."
     + " This will only work when all variables referenced by the dimacs file are global and declared before this function is called.")
  private String externModelFunctionName = "__VERIFIER_externModelSatisfied";

  @Option(description = "list of functions that provide new memory on the heap."
    + " This is only used, when handling of pointers is enabled.")
  private Set<String> memoryAllocationFunctions = ImmutableSet.of(
      "malloc", "__kmalloc", "kzalloc"
      );


  public FormulaEncodingOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);

    nondetFunctionsPattern = Pattern.compile(nondetFunctionsRegexp);
  }

  public boolean handlePointerAliasing() {
    return handlePointerAliasing;
  }

  public boolean handleFieldAccess() {
    return handleFieldAccess;
  }

  public boolean useUifForLvals() {
    return lvalsAsUif;
  }

  public boolean isNondetFunction(String function) {
    return nondetFunctions.contains(function)
        || nondetFunctionsPattern.matcher(function).matches();
  }

  public boolean isMemoryAllocationFunction(String function) {
    return memoryAllocationFunctions.contains(function);
  }

  public boolean isExternModelFunction(String function) {
    return function.equals(externModelFunctionName);
  }
}
