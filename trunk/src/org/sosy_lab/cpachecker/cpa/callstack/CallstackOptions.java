// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "cpa.callstack")
class CallstackOptions {

  // set of functions that may not appear in the source code
  @Option(
      secure = true,
      description = "Blacklist of extern functions that will make the analysis abort if called")
  private ImmutableSet<String> unsupportedFunctions =
      ImmutableSet.of(
          "pthread_create",
          "pthread_key_create",
          "longjmp",
          "siglongjmp",
          "__builtin_va_arg",
          "atexit");

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
          "which abstract domain to use for callstack cpa, typically FLAT which is faster since it"
              + " uses only object equivalence")
  private String domainType = "FLAT";

  CallstackOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  ImmutableSet<String> getUnsupportedFunctions() {
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
