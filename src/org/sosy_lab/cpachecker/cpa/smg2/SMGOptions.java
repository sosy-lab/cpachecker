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
      description = "If this Option is enabled, failure of malloc" + "is simulated")
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
      ImmutableSet.of("malloc", "__kmalloc", "kmalloc", "realloc");

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
      name = "crashOnUnknown",
      description = "Crash on unknown array dereferences")
  private boolean crashOnUnknown = false;

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
  private boolean joinOnBlockEnd = true;

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
  private boolean ignoreUnknownMemoryAllocation = false;

  public enum SMGExportLevel {
    NEVER,
    LEAF,
    INTERESTING,
    EVERY
  }

  public SMGOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  public boolean isIgnoreUnknownMemoryAllocation() {
    return ignoreUnknownMemoryAllocation;
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

  public boolean crashOnUnknown() {
    return crashOnUnknown;
  }

  boolean isAssignEqualityAssumptions() {
    return assignEqualityAssumptions;
  }

  boolean isAssignSymbolicValues() {
    return assignSymbolicValues;
  }
}
