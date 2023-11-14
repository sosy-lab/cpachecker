// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.cpa.smg.SMGRuntimeCheck;

@Options(prefix = "cpa.smg2")
public class SMGOptions {

  @Option(
      secure = true,
      description =
          "with this option enabled, we try to gather information on memory reads from values that"
              + " are overlapping but not exactly fitting to the read parameters. Example: int"
              + " value = 1111; char a = (char)((char[])&value)[1];")
  private boolean preciseSMGRead = true;

  @Option(
      secure = true,
      description =
          "with this option enabled, memory addresses (pointers) are transformed into a numeric"
              + " assumption upon casting the pointer to a number. This assumption can be returned"
              + " to a proper pointer by casting it back. This enables numeric operations beyond"
              + " pointer arithmetics, but loses precision for comparisons/assumptions, as the"
              + " numeric assumption is static. May be unsound!")
  private boolean castMemoryAddressesToNumeric = false;

  @Option(
      secure = true,
      description =
          "with this option enabled, a check for unreachable memory occurs whenever a function"
              + " returns, and not only at the end of the main function")
  private boolean checkForMemLeaksAtEveryFrameDrop = true;

  @Option(
      secure = true,
      description =
          "with this option enabled, memory that is not freed before the end of main is reported"
              + " as memleak even if it is reachable from local variables in main")
  private boolean handleNonFreedMemoryInMainAsMemLeak = true;

  @Option(
      secure = true,
      name = "enableMallocFail",
      description = "If this Option is enabled, failure of malloc is simulated")
  private boolean enableMallocFailure = true;

  @Option(
      secure = true,
      toUppercase = true,
      name = "handleUnknownFunctions",
      description = "Sets how unknown functions are handled.")
  private UnknownFunctionHandling handleUnknownFunctions = UnknownFunctionHandling.STRICT;

  @Option(
      secure = true,
      description =
          "Which unknown function are always considered as safe functions, "
              + "i.e., free of memory-related side-effects?")
  private ImmutableSet<String> safeUnknownFunctions = ImmutableSet.of("abort");

  public enum UnknownFunctionHandling {
    STRICT,
    ASSUME_SAFE,
    ASSUME_EXTERNAL_ALLOCATED
  }

  @Option(
      secure = true,
      toUppercase = true,
      name = "GCCZeroLengthArray",
      description = "Enable GCC extension 'Arrays of Length Zero'.")
  private boolean GCCZeroLengthArray = false;

  @Option(
      secure = true,
      name = "guessSizeOfUnknownMemorySize",
      description = "Size of memory that cannot be calculated will be guessed.")
  private boolean guessSizeOfUnknownMemorySize = false;

  @Option(
      secure = true,
      name = "memoryAllocationFunctions",
      description = "Memory allocation functions")
  private ImmutableSet<String> memoryAllocationFunctions =
      ImmutableSet.of("malloc", "__kmalloc", "kmalloc");

  @Option(
      secure = true,
      name = "guessSize",
      description = "Allocation size of memory that cannot be calculated.")
  private BigInteger guessSize = BigInteger.valueOf(2);

  @Option(
      secure = true,
      name = "memoryAllocationFunctionsSizeParameter",
      description = "Size parameter of memory allocation functions")
  private int memoryAllocationFunctionsSizeParameter = 0;

  @Option(
      secure = true,
      name = "arrayAllocationFunctions",
      description = "Array allocation functions")
  private ImmutableSet<String> arrayAllocationFunctions = ImmutableSet.of("calloc");

  @Option(
      secure = true,
      name = "memoryArrayAllocationFunctionsNumParameter",
      description = "Position of number of element parameter for array allocation functions")
  private int memoryArrayAllocationFunctionsNumParameter = 0;

  @Option(
      secure = true,
      name = "memoryArrayAllocationFunctionsElemSizeParameter",
      description = "Position of element size parameter for array allocation functions")
  private int memoryArrayAllocationFunctionsElemSizeParameter = 1;

  @Option(
      secure = true,
      name = "zeroingMemoryAllocation",
      description = "Allocation functions which set memory to zero")
  private ImmutableSet<String> zeroingMemoryAllocation = ImmutableSet.of("calloc", "kzalloc");

  @Option(secure = true, name = "deallocationFunctions", description = "Deallocation functions")
  private ImmutableSet<String> deallocationFunctions = ImmutableSet.of("free");

  @Option(
      secure = true,
      name = "externalAllocationFunction",
      description = "Functions which indicate on external allocated memory")
  private ImmutableSet<String> externalAllocationFunction = ImmutableSet.of("ext_allocation");

  @Option(
      secure = true,
      name = "externalAllocationSize",
      description = "Default size of externally allocated memory")
  private int externalAllocationSize = Integer.MAX_VALUE;

  @Option(
      secure = true,
      name = "trackPredicates",
      description = "Enable track predicates on SMG state")
  private boolean trackPredicates = false;

  private enum CheckStrategy {
    AT_ASSUME,
    AT_TARGET
  }

  @Option(
      name = "satCheckStrategy",
      description = "When to check the satisfiability of constraints")
  private CheckStrategy satCheckStrategy = CheckStrategy.AT_ASSUME;

  @Option(secure = true, description = "Whether to use subset caching", name = "cacheSubsets")
  private boolean cacheSubsets = false;

  @Option(secure = true, description = "Whether to use superset caching", name = "cacheSupersets")
  private boolean cacheSupersets = false;

  @Option(
      secure = true,
      description = "Whether to perform SAT checks only for the last added constraint",
      name = "minimalSatCheck")
  private boolean performMinimalSatCheck = true;

  @Option(
      secure = true,
      description = "Whether to perform caching of constraint satisfiability results",
      name = "cache")
  private boolean doCaching = true;

  @Option(secure = true, description = "Resolve definite assignments", name = "resolveDefinites")
  private boolean resolveDefinites = true;

  @Option(
      secure = true,
      name = "trackErrorPredicates",
      description = "Enable track predicates for possible memory safety error on SMG state")
  private boolean trackErrorPredicates = false;

  @Option(
      secure = true,
      name = "handleUnknownDereferenceAsSafe",
      description =
          "Handle unknown dereference as safe and check error based on error predicate, "
              + "depends on trackPredicates")
  private boolean handleUnknownDereferenceAsSafe = false;

  @Option(
      secure = true,
      name = "crashOnUnknownInConstraint",
      description = "Crash on unknown value when creating constraints of any form.")
  private boolean crashOnUnknownInConstraint = false;

  @Option(
      secure = true,
      description = "with this option enabled, heap abstraction will be enabled.")
  private boolean enableHeapAbstraction = false;

  @Option(
      secure = true,
      name = "memoryErrors",
      description = "Determines if memory errors are target states")
  private boolean memoryErrors = true;

  @Option(
      secure = true,
      name = "unknownOnUndefined",
      description = "Emit messages when we encounter non-target undefined behavior")
  private boolean unknownOnUndefined = true;

  @Option(
      secure = true,
      name = "runtimeCheck",
      description = "Sets the level of runtime checking: NONE, HALF, FULL")
  private SMGRuntimeCheck runtimeCheck = SMGRuntimeCheck.NONE;

  @Option(
      secure = true,
      name = "exportSMG.file",
      description = "Filename format for SMG graph dumps")
  @FileOption(Type.OUTPUT_FILE)
  private PathTemplate exportSMGFilePattern = PathTemplate.ofFormatString("smg/smg-%s.dot");

  @Option(
      secure = true,
      toUppercase = true,
      name = "exportSMGwhen",
      description = "Describes when SMG graphs should be dumped.")
  private SMGExportLevel exportSMG = SMGExportLevel.NEVER;

  @Option(
      secure = true,
      name = "allocateExternalVariables",
      description = "Allocate memory on declaration of external variable")
  private boolean allocateExternalVariables = true;

  @Option(
      secure = true,
      name = "handleIncompleteExternalVariableAsExternalAllocation",
      description =
          "Handle external variables with incomplete type (extern int array[]) as external"
              + " allocation")
  private boolean handleIncompleteExternalVariableAsExternalAllocation = false;

  @Option(
      secure = true,
      name = "joinOnBlockEnd",
      description =
          "Perform merge SMGStates by SMGJoin on ends of code block. Works with 'merge=JOIN'")
  private boolean joinOnBlockEnd = false;

  @Option(
      secure = true,
      description = "Use equality assumptions to assign values (e.g., (x == 0) => x = 0)")
  private boolean assignEqualityAssumptions = true;

  // assignSymbolicValues is needed to get the same options as the valueAnalysis as SMGs always save
  // symbolics. We could however simply retranslate every symbolic to an unknown after reads.
  @Option(
      secure = true,
      description = "Treat symbolic values as unknowns and assign new concrete values to them.")
  private boolean assignSymbolicValues = true;

  @Option(
      secure = true,
      description =
          "if there is an assumption like (x!=0), "
              + "this option sets unknown (uninitialized) variables to 1L, "
              + "when the true-branch is handled.")
  private boolean initAssumptionVars = false;

  @Option(
      secure = true,
      description = "Assume that variables used only in a boolean context are either zero or one.")
  private boolean optimizeBooleanVariables = true;

  @Option(
      secure = true,
      description =
          "If this option is enabled, a memory allocation (e.g. malloc or array declaration) for "
              + "unknown memory sizes does not abort, but also does not create any memory.")
  private UnknownMemoryAllocationHandling handleUnknownMemoryAllocation =
      UnknownMemoryAllocationHandling.STOP_ANALYSIS;

  /*
   * Ignore: ignore allocation call and overapproximate.
   * Memory_error: same as ignore but with an added memory error. (Needed in CEGAR, as else we
   * would never learn that the allocation size and or other variables are important.)
   * Stop_analysis: stops the analysis, returning unknown.
   */
  public enum UnknownMemoryAllocationHandling {
    IGNORE,
    MEMORY_ERROR,
    STOP_ANALYSIS
  }

  @Option(
      secure = true,
      description =
          "If this option is enabled, a call to malloc with value zero results in a return value "
              + "that is equal to zero. If this option is disabled, a non-zero memory section"
              + " that may not be accessed but freed is returned.")
  private boolean mallocZeroReturnsZero = false;

  public enum SMGExportLevel {
    NEVER,
    LEAF,
    INTERESTING,
    EVERY
  }

  public SMGOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  private UnknownMemoryAllocationHandling getIgnoreUnknownMemoryAllocationSetting() {
    return handleUnknownMemoryAllocation;
  }

  public boolean isIgnoreUnknownMemoryAllocation() {
    return getIgnoreUnknownMemoryAllocationSetting() == UnknownMemoryAllocationHandling.IGNORE;
  }

  public boolean isErrorOnUnknownMemoryAllocation() {
    return getIgnoreUnknownMemoryAllocationSetting()
        == UnknownMemoryAllocationHandling.MEMORY_ERROR;
  }

  public boolean isStopAnalysisOnUnknownMemoryAllocation() {
    return getIgnoreUnknownMemoryAllocationSetting()
        == UnknownMemoryAllocationHandling.STOP_ANALYSIS;
  }

  public boolean isMallocZeroReturnsZero() {
    return mallocZeroReturnsZero;
  }

  boolean isOptimizeBooleanVariables() {
    return optimizeBooleanVariables;
  }

  boolean isInitAssumptionVars() {
    return initAssumptionVars;
  }

  public boolean isCheckForMemLeaksAtEveryFrameDrop() {
    return checkForMemLeaksAtEveryFrameDrop;
  }

  public boolean isHandleNonFreedMemoryInMainAsMemLeak() {
    return handleNonFreedMemoryInMainAsMemLeak;
  }

  public boolean isEnableMallocFailure() {
    return enableMallocFailure;
  }

  public boolean isCastMemoryAddressesToNumeric() {
    return castMemoryAddressesToNumeric;
  }

  public boolean isPreciseSMGRead() {
    return preciseSMGRead;
  }

  public UnknownFunctionHandling getHandleUnknownFunctions() {
    return handleUnknownFunctions;
  }

  public ImmutableSet<String> getSafeUnknownFunctions() {
    return safeUnknownFunctions;
  }

  public boolean isGCCZeroLengthArray() {
    return GCCZeroLengthArray;
  }

  public boolean isGuessSizeOfUnknownMemorySize() {
    return guessSizeOfUnknownMemorySize;
  }

  public BigInteger getGuessSize() {
    return guessSize;
  }

  public ImmutableSet<String> getMemoryAllocationFunctions() {
    return memoryAllocationFunctions;
  }

  public int getMemoryAllocationFunctionsSizeParameter() {
    return memoryAllocationFunctionsSizeParameter;
  }

  public ImmutableSet<String> getArrayAllocationFunctions() {
    return arrayAllocationFunctions;
  }

  public int getMemoryArrayAllocationFunctionsNumParameter() {
    return memoryArrayAllocationFunctionsNumParameter;
  }

  public int getMemoryArrayAllocationFunctionsElemSizeParameter() {
    return memoryArrayAllocationFunctionsElemSizeParameter;
  }

  public ImmutableSet<String> getZeroingMemoryAllocation() {
    return zeroingMemoryAllocation;
  }

  public ImmutableSet<String> getDeallocationFunctions() {
    return deallocationFunctions;
  }

  public ImmutableSet<String> getExternalAllocationFunction() {
    return externalAllocationFunction;
  }

  public int getExternalAllocationSize() {
    return externalAllocationSize;
  }

  public boolean trackPredicates() {
    return trackPredicates;
  }

  public boolean trackErrorPredicates() {
    return trackErrorPredicates;
  }

  public boolean isHeapAbstractionEnabled() {
    return enableHeapAbstraction;
  }

  public boolean isMemoryErrorTarget() {
    return memoryErrors;
  }

  public boolean unknownOnUndefined() {
    return unknownOnUndefined;
  }

  public SMGRuntimeCheck getRuntimeCheck() {
    return runtimeCheck;
  }

  public PathTemplate getExportSMGFilePattern() {
    return exportSMGFilePattern;
  }

  public SMGExportLevel getExportSMGLevel() {
    return exportSMG;
  }

  public boolean isHandleIncompleteExternalVariableAsExternalAllocation() {
    return handleIncompleteExternalVariableAsExternalAllocation;
  }

  public boolean getAllocateExternalVariables() {
    return allocateExternalVariables;
  }

  public boolean isHandleUnknownDereferenceAsSafe() {
    return handleUnknownDereferenceAsSafe;
  }

  public boolean getJoinOnBlockEnd() {
    return joinOnBlockEnd;
  }

  public boolean crashOnUnknownInConstraint() {
    return crashOnUnknownInConstraint;
  }

  boolean isAssignEqualityAssumptions() {
    return assignEqualityAssumptions;
  }

  boolean isAssignSymbolicValues() {
    return assignSymbolicValues;
  }

  public boolean isSatCheckStrategyAtAssume() {
    return satCheckStrategy == CheckStrategy.AT_ASSUME;
  }

  public boolean isDoConstraintCaching() {
    return doCaching;
  }

  public boolean isUseConstraintCacheSupersets() {
    return cacheSupersets;
  }

  public boolean isUseConstraintCacheSubsets() {
    return cacheSubsets;
  }

  public boolean isPerformMinimalConstraintSatCheck() {
    return performMinimalSatCheck;
  }

  public boolean isResolveDefinites() {
    return resolveDefinites;
  }
}
