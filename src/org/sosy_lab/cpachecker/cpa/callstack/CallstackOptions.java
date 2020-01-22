/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.callstack;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "cpa.callstack")
public class CallstackOptions {

  // set of functions that may not appear in the source code
  @Option(secure = true, description = "unsupported functions cause an exception")
  private ImmutableSet<String> unsupportedFunctions =
      ImmutableSet.of("pthread_create", "longjmp", "siglongjmp");

  @Option(secure = true, name = "depth", description = "depth of recursion bound")
  private int recursionBoundDepth = 0;

  @Option(
      secure = true,
      name = "skipRecursion",
      description =
          "Skip recursion (this is unsound)."
              + " Treat function call as a statement (the same as for functions without bodies)")
  private boolean skipRecursion = false;

  @Option(
      secure = true,
      description =
          "Skip recursion if it happens only by going via a function pointer (this is unsound)."
              + " Imprecise function pointer tracking often lead to false recursions.")
  private boolean skipFunctionPointerRecursion = false;

  @Option(
      secure = true,
      description =
          "Skip recursion if it happens only by going via a void function (this is unsound).")
  private boolean skipVoidRecursion = false;

  @Option(description = "analyse the CFA backwards", secure = true)
  private boolean traverseBackwards = false;

  @Option(
      secure = true,
      name = "domain",
      toUppercase = true,
      values = {"FLAT", "FLATPCC"},
      description =
          "which abstract domain to use for callstack cpa, typically FLAT which is faster since it uses only object equivalence")
  private String domainType = "FLAT";

  CallstackOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  public ImmutableSet<String> getUnsupportedFunctions() {
    return unsupportedFunctions;
  }

  int getRecursionBoundDepth() {
    return recursionBoundDepth;
  }

  boolean skipRecursion() {
    return skipRecursion;
  }

  boolean skipFunctionPointerRecursion() {
    return skipFunctionPointerRecursion;
  }

  boolean skipVoidRecursion() {
    return skipVoidRecursion;
  }

  boolean traverseBackwards() {
    return traverseBackwards;
  }

  String getDomainType() {
    return domainType;
  }
}
