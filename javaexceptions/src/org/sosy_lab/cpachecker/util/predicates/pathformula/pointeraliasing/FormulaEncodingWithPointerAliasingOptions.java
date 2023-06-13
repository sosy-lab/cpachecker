// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;

@Options(prefix = "cpa.predicate")
public class FormulaEncodingWithPointerAliasingOptions extends FormulaEncodingOptions {

  @Option(
      secure = true,
      description =
          "Memory allocation functions of which all parameters but the first should be ignored.")
  private ImmutableSet<String> memoryAllocationFunctionsWithSuperfluousParameters =
      ImmutableSet.of("__kmalloc", "kmalloc", "kzalloc");

  @Option(
      secure = true,
      description =
          "The function used to model successful heap object allocation. "
              + "This is only used, when pointer analysis with UFs is enabled.")
  private String successfulAllocFunctionName = "__VERIFIER_successful_alloc";

  @Option(
      secure = true,
      description =
          "The function used to model successful heap object allocation with zeroing. "
              + "This is only used, when pointer analysis with UFs is enabled.")
  private String successfulZallocFunctionName = "__VERIFIER_successful_zalloc";

  @Option(
      secure = true,
      description =
          "Setting this to true makes memoryAllocationFunctions always return a valid pointer.")
  private boolean memoryAllocationsAlwaysSucceed = false;

  @Option(
      secure = true,
      description =
          "Enable the option to allow detecting the allocation type by type of the LHS of the"
              + " assignment, e.g. char *arr = malloc(size) is detected as char[size]")
  private boolean revealAllocationTypeFromLhs = true;

  @Option(
      secure = true,
      description =
          "Use deferred allocation heuristic that tracks void * variables until the actual type "
              + "of the allocation is figured out.")
  private boolean deferUntypedAllocations = true;

  @Option(
      secure = true,
      description =
          "The default size in bytes for memory allocations when the value cannot be determined.")
  private int defaultAllocationSize = 4;

  @Option(
      secure = true,
      description =
          "Use SMT arrays for encoding heap memory instead of uninterpreted function"
              + " (ignored if useByteArrayForHeap=true)."
              + " This is more precise but may lead to interpolation failures.")
  private boolean useArraysForHeap = true;

  @Option(
      secure = true,
      description =
          "Use SMT byte array for encoding heap memory instead of uninterpreted function."
              + " This is more close to c heap implementation but may be to expensive.")
  private boolean useByteArrayForHeap = false;

  @Option(secure = true, description = "The length for arrays we assume for variably-sized arrays.")
  private int defaultArrayLength = 20;

  @Option(
      secure = true,
      description =
          "The maximum length up to which bulk assignments (e.g., initialization) for arrays will"
              + " be handled. With option useArraysForHeap=false, elements beyond this bound will"
              + " be ignored completely. Use -1 to disable the limit.")
  @IntegerOption(min = -1)
  private int maxArrayLength = -1;

  @Option(secure = true, description = "Function that is used to free allocated memory.")
  private String memoryFreeFunctionName = "free";

  @Option(
      secure = true,
      description =
          "Use quantifiers when encoding heap accesses. "
              + "This requires an SMT solver that is capable of quantifiers (e.g. Z3 or PRINCESS).")
  private boolean useQuantifiersOnArrays = false;

  @Option(
      secure = true,
      description =
          "When a string literal initializer is encountered, initialize the contents of the char"
              + " array with the contents of the string literal instead of just assigning a fresh"
              + " non-det address to it")
  private boolean handleStringLiteralInitializers = false;

  @Option(
      deprecatedName = "maxPreciseStrlenSize",
      secure = true,
      description =
          "When builtin functions like memcmp/strlen/etc. are called, unroll them up to this bound."
              + "If the passed arguments are longer, the return value will be overapproximated.")
  private int maxPreciseStrFunctionSize = 100;

  @Option(
      secure = true,
      description =
          "If disabled, all implicitly initialized fields and elements are treated as non-dets")
  private boolean handleImplicitInitialization = true;

  @Option(
      secure = true,
      description =
          "Use regions for pointer analysis. So called Burstall&Bornat (BnB) memory regions will be"
              + " used for pointer analysis. BnB regions are based not only on type, but also on"
              + " structure field names. If the field is not accessed by an address then it is"
              + " placed into a separate region.")
  private boolean useMemoryRegions = false;

  @Option(secure = true, description = "Use an optimisation for constraint generation")
  private boolean useConstraintOptimization = true;

  public FormulaEncodingWithPointerAliasingOptions(Configuration config)
      throws InvalidConfigurationException {
    super(config);
    config.inject(this, FormulaEncodingWithPointerAliasingOptions.class);

    if (useByteArrayForHeap) {
      useArraysForHeap = true;
    }

    if (maxArrayLength == -1) {
      maxArrayLength = Integer.MAX_VALUE;
    }
  }

  @Override
  public boolean shouldAbortOnLargeArrays() {
    if (useArraysForHeap() || useQuantifiersOnArrays()) {
      // In this case large arrays are maybe possible to handle
      return false;
    }
    return super.shouldAbortOnLargeArrays();
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

  int defaultAllocationSize() {
    return defaultAllocationSize;
  }

  /**
   * Return whether the heap is modeled using SMT arrays (either byte-wise or word-wise). Use {@link
   * #useByteArrayForHeap()} to distinguish between the latter options.
   */
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

  int maxPreciseStrFunctionSize() {
    return maxPreciseStrFunctionSize;
  }

  boolean handleImplicitInitialization() {
    return handleImplicitInitialization;
  }

  public boolean useMemoryRegions() {
    return useMemoryRegions;
  }

  public boolean useConstraintOptimization() {
    return useConstraintOptimization;
  }

  public boolean useByteArrayForHeap() {
    return useByteArrayForHeap;
  }
}
