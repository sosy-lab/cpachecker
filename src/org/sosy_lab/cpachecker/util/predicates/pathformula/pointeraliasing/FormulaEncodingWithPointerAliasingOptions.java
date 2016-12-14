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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;

@Options(prefix="cpa.predicate")
public class FormulaEncodingWithPointerAliasingOptions extends FormulaEncodingOptions {

  @Option(secure=true, description = "Memory allocation functions of which all parameters but the first should be ignored.")
  private ImmutableSet<String> memoryAllocationFunctionsWithSuperfluousParameters = ImmutableSet.of(
      "__kmalloc", "kmalloc", "kzalloc");

  @Option(secure=true, description = "The function used to model successful heap object allocation. " +
                        "This is only used, when pointer analysis with UFs is enabled.")
  private String successfulAllocFunctionName = "__VERIFIER_successful_alloc";

  @Option(secure=true, description = "The function used to model successful heap object allocation with zeroing. " +
                        "This is only used, when pointer analysis with UFs is enabled.")
  private String successfulZallocFunctionName = "__VERIFIER_successful_zalloc";

  @Option(secure=true, description = "Setting this to true makes memoryAllocationFunctions always return a valid pointer.")
  private boolean memoryAllocationsAlwaysSucceed = false;

  @Option(secure=true, description = "Enable the option to allow detecting the allocation type by type " +
                        "of the LHS of the assignment, e.g. char *arr = malloc(size) is detected as char[size]")
  private boolean revealAllocationTypeFromLhs = true;

  @Option(secure=true, description = "Use deferred allocation heuristic that tracks void * variables until the actual type " +
                        "of the allocation is figured out.")
  private boolean deferUntypedAllocations = true;

  @Option(secure=true, description = "Maximum size of allocations for which all structure fields are regarded always essential, " +
                        "regardless of whether they were ever really used in code.")
  private int maxPreFilledAllocationSize = 0;

  @Option(secure=true, description = "The default size in bytes for memory allocations when the value cannot be determined.")
  private int defaultAllocationSize = 4;

  @Option(
    secure = true,
    description =
        "Use the theory of arrays for heap-memory encoding. "
            + "This requires an SMT solver that is capable of the theory of arrays."
  )
  private boolean useArraysForHeap = false;

  @Option(secure=true, description = "The default length for arrays when the real length cannot be determined.")
  private int defaultArrayLength = 20;

  @Option(secure=true, description = "The maximum length for arrays (elements beyond this will be ignored). Use -1 to disable the limit.")
  @IntegerOption(min=-1)
  private int maxArrayLength = 20;

  @Option(secure=true, description = "Function that is used to free allocated memory.")
  private String memoryFreeFunctionName = "free";

  @Option(secure = true, description = "Use quantifiers when encoding heap accesses. "
      + "This requires an SMT solver that is capable of quantifiers (e.g. Z3 or PRINCESS).")
  private boolean useQuantifiersOnArrays = false;

  @Option(secure=true, description = "When a string literal initializer is encountered, initialize the contents of the char array "
                      + "with the contents of the string literal instead of just assigning a fresh non-det address "
                      + "to it")
  private boolean handleStringLiteralInitializers = false;

  @Option(secure=true, description = "If disabled, all implicitly initialized fields and elements are treated as non-dets")
  private boolean handleImplicitInitialization = true;

  @Option(secure=true, description = "Use regions for pointer analysis. "
      + "So called Burstall&Bornat (BnB) memory regions will be used for pointer analysis. "
      + "BnB regions are based not only on type, but also on structure field names. "
      + "If the field is not accessed by an address then it is placed into a separate region.")
  private boolean useMemoryRegions = false;

  public FormulaEncodingWithPointerAliasingOptions(Configuration config) throws InvalidConfigurationException {
    super(config);
    config.inject(this, FormulaEncodingWithPointerAliasingOptions.class);

    if (maxArrayLength == -1) {
      maxArrayLength = Integer.MAX_VALUE;
    }
  }

  boolean hasSuperfluousParameters(final String name) {
    return memoryAllocationFunctionsWithSuperfluousParameters.contains(name);
  }

  boolean isDynamicMemoryFunction(final String name) {
    return isSuccessfulAllocFunctionName(name)
        || isSuccessfulZallocFunctionName(name)
        || isMemoryAllocationFunction(name)
        || isMemoryAllocationFunctionWithZeroing(name)
        || isMemoryFreeFunction(name);
  }

  boolean isSuccessfulAllocFunctionName(final String name) {
    return successfulAllocFunctionName.equals(name);
  }

  boolean isSuccessfulZallocFunctionName(final String name) {
    return successfulZallocFunctionName.equals(name);
  }

  boolean isDynamicAllocVariableName(final String name) {
    return isSuccessfulAllocFunctionName(name) || isSuccessfulZallocFunctionName(name);
  }

  String getSuccessfulAllocFunctionName() {
    return successfulAllocFunctionName;
  }

  String getSuccessfulZallocFunctionName() {
    return successfulZallocFunctionName;
  }

  boolean makeMemoryAllocationsAlwaysSucceed() {
    return memoryAllocationsAlwaysSucceed;
  }

  boolean revealAllocationTypeFromLHS() {
    return revealAllocationTypeFromLhs;
  }

  boolean deferUntypedAllocations() {
    return deferUntypedAllocations;
  }

  int maxPreFilledAllocationSize() {
    return maxPreFilledAllocationSize;
  }

  int defaultAllocationSize() {
    return defaultAllocationSize;
  }

  public boolean useArraysForHeap() {
    return useArraysForHeap;
  }

  int defaultArrayLength() {
    return defaultArrayLength;
  }

  int maxArrayLength() {
    return maxArrayLength;
  }

  boolean isMemoryFreeFunction(final String name) {
    return memoryFreeFunctionName.equals(name);
  }

  public boolean useQuantifiersOnArrays() {
    return useQuantifiersOnArrays;
  }

  boolean handleStringLiteralInitializers() {
    return handleStringLiteralInitializers;
  }

  boolean handleImplicitInitialization() {
    return handleImplicitInitialization;
  }

  public boolean useMemoryRegions() {
    return useMemoryRegions;
  }
}
