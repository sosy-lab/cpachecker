// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.smg.SMGRuntimeCheck;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;

@Options(prefix = "cpa.smg2")
public class SMGOptions {

  private int actualConcreteValueForSymbolicOffsetsAssignmentMaximum = 0;

  @Option(
      secure = true,
      description =
          "aborts the analysis for a non-concrete (this includes symbolic values) memory allocation"
              + " of any kind.")
  private boolean abortOnNonConcreteMemorySize = true;

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
      description =
          "Sets how unknown functions are handled.\n"
              + "STRICT: Unknown functions cause a stop in the analysis, i.e. known and handled"
              + " functions are evaluated normally. \n"
              + "ASSUME_SAFE: unknown functions are assumed to be safe. No input into the function"
              + " is checked for validity and the result is a UNKNOWN value (which may itself"
              + " violate memorysafety etc.). Warning: ASSUME_SAFE can be unsound due to side"
              + " effects, the unknown return value etc.!\n"
              + "ASSUME_EXTERNAL_ALLOCATED: Input into the function is checked for validity and may"
              + " cause memory based errors. Returned values are unknown, but in a valid new memory"
              + " section that can be freed normally. Functions allocating external memory and"
              + " returning their address can be defined with option externalAllocationFunction and"
              + " externalAllocationSize.\n"
              + "Functions defined in option \"safeUnknownFunctions\" are handled equally to"
              + " ASSUME_SAFE in all cases.")
  private UnknownFunctionHandling handleUnknownFunctions =
      UnknownFunctionHandling.ASSUME_EXTERNAL_ALLOCATED;

  @Option(
      secure = true,
      description =
          "List of functions that are always considered as safe, i.e. they are not evaluated, even"
              + " if known to the analysis, nor are their inputs checked for validity. They always"
              + " return a new, unknown value and therefore overapproximate if their signature does"
              + " not return void. Using this option might be unsound, depending on the function.")
  private ImmutableSet<String> safeUnknownFunctions = ImmutableSet.of("");

  @Option(
      secure = true,
      name = "overapproximateSymbolicOffsets",
      description =
          "If this Option is enabled, all values of a memory region that is written or read with a"
              + " symbolic offset are overapproximated. I.e. when writing to a memory region, all"
              + " previous values are deleted and the memory region is overapproximated so that"
              + " only unknown values are in the memory region after the write. When reading, all"
              + " possible reads are evaluated. Can not be used at the same time as option"
              + " findConcreteValuesForSymbolicOffsets.")
  private boolean overapproximateSymbolicOffsets = false;

  @Option(
      secure = true,
      name = "findConcreteValuesForSymbolicOffsets",
      description =
          "If this Option is enabled, all symbolic offsets used when writing to memory are"
              + " evaluated into all possible concrete values by an SMT solver. This might be very"
              + " expensive, as all possible combinations of values for the symbolic offsets are"
              + " concretely evaluated. May not be used together with option"
              + " overapproximateForSymbolicWrite.")
  private boolean findConcreteValuesForSymbolicOffsets = false;

  @Option(
      secure = true,
      name = "concreteValueForSymbolicOffsetsAssignmentMaximum",
      description =
          "Maximum amount of concrete assignments before the assigning is aborted. The last offset"
              + " is then once treated as option overapproximateSymbolicOffsetsAsFallback"
              + " specifies.")
  private int concreteValueForSymbolicOffsetsAssignmentMaximum = 300;

  // TODO: make a option and implementation that allows SOME concrete values to be evaluated, and
  // then the (restricted) symbolic value is returned once a threshold is reached. The concrete
  // values chosen should be configurable, e.g. build 1 concrete value at the lowest end of the
  // value spectrum etc. This would could be used to boost the CEX that is currently unable to
  // handle symbolic offsets/memory sizes well.
  /*
    @Option(
        secure = true,
        name = "overapproximateSymbolicOffsetsAsFallback",
        description =
            "If this Option is enabled, and concreteValueForSymbolicOffsetsAssignmentMaximum reaches"
                + " its maximum, the one last not assigned offset of a memory region that is written"
                + " or read with a symbolic offset is overapproximated as specified in"
                + " findConcreteValuesForSymbolicOffsets. Otherwise, the analysis is aborted.")
    private boolean overapproximateSymbolicOffsetsAsFallback = false;
  */

  /*
  @Option(
      secure = true,
      description =
          "If this Option is enabled, all symbolic type sizes used when writing to memory (i.e. the"
              + " bit size of the type of the value written) are evaluated into all possible"
              + " concrete values by an SMT solver. This might be very expensive, as all possible"
              + " combinations of values for the symbolic values are concretely evaluated. May not"
              + " be used together with option overapproximateValuesForSymbolicTypeSize.")
  private boolean findConcreteValuesForSymbolicTypeSize = false;

  @Option(
      secure = true,
      description =
          "Maximum amount of concrete assignments before the assigning is aborted. The last offset"
              + " is then once treated as option overapproximateValuesForSymbolicTypeSize"
              + " specifies.")
  private int findConcreteValuesForSymbolicTypeSizeAssignmentMaximum = 30;
   */

  @Option(
      secure = true,
      name = "allowSymbolicVariableArrayLength",
      description = "If this Option is enabled, variable array length may be symbolic.")
  private boolean allowSymbolicVariableArrayLength = false;

  // TODO: add findConcreteValuesForSymbolicTypeSize to text!
  @Option(
      secure = true,
      description =
          "If this Option is enabled, writing with symbolic sized value types are overapproximated."
              + " I.e. the memory region affected is overapproximated, including the"
              + " value itself, to unknown.")
  private boolean overapproximateValuesForSymbolicTypeSize = false;

  public boolean isOverapproximateValuesForSymbolicTypeSize() {
    return overapproximateValuesForSymbolicTypeSize;
  }

  public boolean isOverapproximateSymbolicOffsets() {
    Preconditions.checkArgument(
        !overapproximateSymbolicOffsets || !findConcreteValuesForSymbolicOffsets);
    return overapproximateSymbolicOffsets;
  }

  public boolean isFindConcreteValuesForSymbolicOffsets() {
    Preconditions.checkArgument(
        !findConcreteValuesForSymbolicOffsets || !overapproximateSymbolicOffsets);
    return findConcreteValuesForSymbolicOffsets;
  }

  public int getConcreteValueForSymbolicOffsetsAssignmentMaximum() {
    return concreteValueForSymbolicOffsetsAssignmentMaximum;
  }

  public void incConcreteValueForSymbolicOffsetsAssignmentMaximum() throws SMGException {
    if (actualConcreteValueForSymbolicOffsetsAssignmentMaximum
        > concreteValueForSymbolicOffsetsAssignmentMaximum) {
      throw new SMGException(
          "Exceeded maximum number of concrete symbolic assignments"
              + " concreteValueForSymbolicOffsetsAssignmentMaximum = "
              + concreteValueForSymbolicOffsetsAssignmentMaximum);
    }
    actualConcreteValueForSymbolicOffsetsAssignmentMaximum++;
  }

  public void decConcreteValueForSymbolicOffsetsAssignmentMaximum() {
    actualConcreteValueForSymbolicOffsetsAssignmentMaximum--;
  }

  public boolean allowSymbolicVariableArrayLength() {
    return allowSymbolicVariableArrayLength;
  }

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

  @Option(
      secure = true,
      name = "enableZeroingOfSymbolicMemorySize",
      description =
          "If true, memory with symbolic size can be zeroed, which allows usage of zeroing"
              + " allocation functions like calloc().")
  private boolean enableZeroingOfSymbolicMemorySize = false;

  @Option(secure = true, name = "deallocationFunctions", description = "Deallocation functions")
  private ImmutableSet<String> deallocationFunctions = ImmutableSet.of("free");

  @Option(
      secure = true,
      name = "externalAllocationFunction",
      description =
          "Functions which return externally allocated memory with bit size defined by option"
              + " externalAllocationSize")
  private ImmutableSet<String> externalAllocationFunction = ImmutableSet.of("ext_allocation");

  @Option(
      secure = true,
      name = "externalAllocationSize",
      description = "Default bit size of externally allocated memory")
  private int externalAllocationSize = Integer.MAX_VALUE;

  @Option(
      secure = true,
      name = "trackPredicates",
      description = "Enable track predicates on SMG state")
  private boolean trackPredicates = true;

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
  private boolean memoryErrors = false;

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
      description = "Use equality assumptions to assign values (e.g., (x == 0) => x = 0)")
  private boolean assignEqualityAssumptions = true;

  // treatSymbolicValuesAsUnknown is needed to get the same options as the valueAnalysis as SMGs
  // always save
  // symbolics. We could however simply retranslate every symbolic to an unknown after reads.
  @Option(
      secure = true,
      description = "Treat symbolic values as unknowns and assign new concrete values to them.")
  private boolean treatSymbolicValuesAsUnknown = false;

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
          "The SV-COMP defines a list of types that are allowed to be used in __VERIFIER_nondet_X()"
              + " functions. If this option is false, only those defined in the competition are"
              + " allowed. All others throw an exception on being evaluated. For true, the type of"
              + " any __VERIFIER_nondet_X() function is simply accepted without any checks.")
  private boolean allowNondetFunctionsWithArbitraryTypes = true;

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

  @Option(
      secure = true,
      name = "canAtexitFail",
      description =
          "If this Option is enabled, C function atexit() will return a succeeding and failing"
              + " registration for each registration. Otherwise only succeeding.")
  private boolean canAtexitFail = false;

  public enum SMGExportLevel {
    NEVER,
    LEAF,
    INTERESTING,
    EVERY
  }

  private final SMGAbstractionOptions abstractionOptions;
  private final SMGMergeOptions mergeOptions;

  public SMGOptions(Configuration config, @Nullable CFA cfa) throws InvalidConfigurationException {
    config.inject(this);
    abstractionOptions = new SMGAbstractionOptions(config, cfa);
    mergeOptions = new SMGMergeOptions(config, cfa);
  }

  public SMGMergeOptions getMergeOptions() {
    return mergeOptions;
  }

  public SMGAbstractionOptions getAbstractionOptions() {
    return abstractionOptions;
  }

  public boolean canAtexitFail() {
    return canAtexitFail;
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

  public boolean allowNondetFunctionsWithArbitraryTypes() {
    return allowNondetFunctionsWithArbitraryTypes;
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

  public boolean isAbortOnNonConcreteMemorySize() {
    return abortOnNonConcreteMemorySize;
  }

  public ImmutableSet<String> getZeroingMemoryAllocation() {
    return zeroingMemoryAllocation;
  }

  public boolean isEnableZeroingOfSymbolicMemorySize() {
    return enableZeroingOfSymbolicMemorySize;
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

  public boolean crashOnUnknownInConstraint() {
    return crashOnUnknownInConstraint;
  }

  boolean isAssignEqualityAssumptions() {
    return assignEqualityAssumptions;
  }

  boolean isTreatSymbolicValuesAsUnknown() {
    return treatSymbolicValuesAsUnknown;
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

  @Options(prefix = "cpa.smg2.merge")
  public static class SMGMergeOptions {

    @Option(
        secure = true,
        description =
            "Apply merge operator based on Predators join algorithm to determine subsumtion of"
                + " abstracted lists in the stop operator more precisely. Can be costly.")
    private boolean useMergeForAbstractionDetectionInStopOperator = false;

    @Option(
        secure = true,
        name = "exclusivelyBlockEnds",
        description = "Apply merge operator only on ends of code blocks.")
    private boolean mergeOnlyOnBlockEnd = false;

    @Option(
        secure = true,
        name = "exclusivelyEqualBlockEnds",
        description =
            "Apply merge operator only on equal code block ends if true. Only applied if"
                + " exclusivelyBlockEnds=true.")
    private boolean mergeOnlyEqualBlockEnds = false;

    @Option(
        secure = true,
        name = "exclusivelyWithAbstractionPresent",
        description =
            "Apply merge operator only on states with at least one input state including an"
                + " abstracted list.")
    private boolean mergeOnlyWithAbstractionPresent = false;

    @Option(
        secure = true,
        name = "overapproximateSymbolicConstraints",
        description =
            "When true, unequal constraints on symbolic values are overapproximated when merging.")
    private boolean overapproximateSymbolicConstraints = false;

    @Option(
        secure = true,
        name = "overapproximateConcreteValues",
        description =
            "When true, concrete values can be overapproximated when merging, e.g. when merged with"
                + " a symbolic value, or another, but distinct concrete value.")
    private boolean overapproximateConcreteValues = false;

    @SuppressWarnings("unused")
    public SMGMergeOptions(Configuration config, @Nullable CFA pCfa)
        throws InvalidConfigurationException {
      config.inject(this);
    }

    public boolean mergeOnlyOnBlockEnd() {
      return mergeOnlyOnBlockEnd;
    }

    public boolean mergeOnlyEqualBlockEnds() {
      return mergeOnlyEqualBlockEnds;
    }

    public boolean mergeOnlyWithAbstractionPresent() {
      return mergeOnlyWithAbstractionPresent;
    }

    public boolean isOverapproximateSymbolicConstraints() {
      return overapproximateSymbolicConstraints;
    }

    public boolean useMergeInStop() {
      return useMergeForAbstractionDetectionInStopOperator;
    }

    public boolean isOverapproximateConcreteValues() {
      return overapproximateConcreteValues;
    }
  }

  @Options(prefix = "cpa.smg2.abstraction")
  public static class SMGAbstractionOptions {

    @Option(secure = true, description = "restrict abstraction computations to branching points")
    private boolean alwaysAtBranch = false;

    @Option(secure = true, description = "restrict abstraction computations to join points")
    private boolean alwaysAtJoin = false;

    @Option(
        secure = true,
        description = "restrict abstraction computations to function calls/returns")
    private boolean alwaysAtFunction = false;

    @Option(
        secure = true,
        description =
            "If enabled, abstraction computations at loop-heads are enabled. List abstraction has"
                + " to be enabled for this.")
    private boolean alwaysAtLoop = false;

    @Option(
        secure = true,
        description =
            "toggle liveness abstraction. Is independent of CEGAR, but dependent on the CFAs"
                + " liveness variables being tracked. Might be unsound for stack-based memory"
                + " structures like arrays.")
    private boolean doLivenessAbstraction = true;

    @Option(
        secure = true,
        description =
            "toggle memory sensitive liveness abstraction. Liveness abstraction is supposed to"
                + " simply abstract all variables away (invalidating memory) when unused, even if"
                + " there is valid outside pointers on them. With this option enabled, it is first"
                + " checked if there is a valid address still pointing to the variable before"
                + " removing it. Liveness abstraction might be unsound without this option.")
    private boolean doEnforcePointerSensitiveLiveness = true;

    @Option(
        secure = true,
        description =
            "restrict liveness abstractions to nodes with more than one entering and/or leaving"
                + " edge")
    private boolean onlyAtNonLinearCFA = false;

    @Option(
        secure = true,
        description =
            "skip abstraction computations until the given number of iterations are reached,"
                + " after that decision is based on then current level of determinism,"
                + " setting the option to -1 always performs abstraction computations")
    @IntegerOption(min = -1)
    private int iterationThreshold = -1;

    @Option(
        secure = true,
        description =
            "threshold for level of determinism, in percent, up-to which abstraction computations "
                + "are performed (and iteration threshold was reached)")
    @IntegerOption(min = 0, max = 100)
    private int determinismThreshold = 85;

    @Option(
        secure = true,
        name = "listAbstractionMinimumLengthThreshold",
        description =
            "The minimum list segments directly following each other with the same value needed to"
                + " abstract them.Minimum is 2.")
    private int listAbstractionMinimumLengthThreshold = 4;

    @Option(
        secure = true,
        name = "listAbstractionMaximumIncreaseLengthThreshold",
        description =
            "The minimum list segments that are needed for abstraction may be increased during the"
                + " analysis based on a heuristic in fixed sized loops. This is the maximum"
                + " increase that is allowed. E.g. all lists with the length given here are"
                + " abstracted in any case. If you want to prevent dynamic increase of list"
                + " abstraction min threshold set this to the same value as"
                + " listAbstractionMinimumLengthThreshold.")
    private int listAbstractionMaximumIncreaseLengthThreshold = 6;

    @Option(
        secure = true,
        name = "abstractHeapValues",
        description = "If heap values are to be abstracted based on CEGAR.")
    private boolean abstractHeapValues = false;

    @Option(
        secure = true,
        name = "abstractProgramVariables",
        description = "Abstraction of program variables via CEGAR.")
    private boolean abstractProgramVariables = false;

    @Option(
        secure = true,
        name = "abstractLinkedLists",
        description = "Abstraction of all detected linked lists at loop heads.")
    private boolean abstractLinkedLists = true;

    @Option(
        secure = true,
        name = "removeUnusedConstraints",
        description = "Periodically removes unused constraints from the state.")
    private boolean cleanUpUnusedConstraints = false;

    // TODO: the goal is to set this in a CEGAR loop one day
    @Option(
        secure = true,
        name = "abstractConcreteValuesAboveThreshold",
        description =
            "Periodically removes concrete values from the memory model and replaces them with"
                + " symbolic values. Only the newest concrete values above this threshold are"
                + " removed. For negative numbers this option is ignored. Note: 0 also removes the"
                + " null value, reducing impacting null dereference or free soundness. Currently"
                + " only supported for given value 0.")
    private int abstractConcreteValuesAboveThreshold = -1;

    private final @Nullable ImmutableSet<CFANode> loopHeads;

    public SMGAbstractionOptions(Configuration config, @Nullable CFA pCfa)
        throws InvalidConfigurationException {
      config.inject(this);

      if (alwaysAtLoop && pCfa != null && pCfa.getAllLoopHeads().isPresent()) {
        // Gather loop heads for abstraction if requested to abstract at loop heads
        loopHeads = pCfa.getAllLoopHeads().orElseThrow();
      } else {
        loopHeads = null;
      }
    }

    public boolean getCleanUpUnusedConstraints() {
      return cleanUpUnusedConstraints;
    }

    public boolean doLivenessAbstraction() {
      return doLivenessAbstraction;
    }

    public boolean abstractProgramVariables() {
      return abstractProgramVariables;
    }

    public boolean abstractLinkedLists() {
      return abstractLinkedLists;
    }

    public int getAbstractConcreteValuesAboveThreshold() {
      Preconditions.checkState(
          abstractConcreteValuesAboveThreshold <= 0,
          "Error: option cpa.smg2.abstraction.abstractConcreteValuesAboveThreshold is currently"
              + " only supported for argument 0.");
      return abstractConcreteValuesAboveThreshold;
    }

    public int getListAbstractionMinimumLengthThreshold() {
      return listAbstractionMinimumLengthThreshold;
    }

    public boolean isEnforcePointerSensitiveLiveness() {
      return doEnforcePointerSensitiveLiveness;
    }

    public int getListAbstractionMaximumIncreaseLengthThreshold() {
      return listAbstractionMaximumIncreaseLengthThreshold;
    }

    public void incListAbstractionMinimumLengthThreshold() {
      listAbstractionMinimumLengthThreshold++;
    }

    /**
     * This method determines whether to abstract at each location.
     *
     * @return whether an abstraction should be computed at each location
     */
    boolean abstractAtEachLocation() {
      return !alwaysAtBranch && !alwaysAtJoin && !alwaysAtFunction && !alwaysAtLoop;
    }

    boolean abstractAtBranch(LocationState location) {
      return alwaysAtBranch && location.getLocationNode().getNumLeavingEdges() > 1;
    }

    boolean abstractAtJoin(LocationState location) {
      return alwaysAtJoin && location.getLocationNode().getNumEnteringEdges() > 1;
    }

    public int getIterationThreshold() {
      return iterationThreshold;
    }

    public int getDeterminismThreshold() {
      return determinismThreshold;
    }

    public boolean abstractHeapValues() {
      return abstractHeapValues;
    }

    public boolean onlyAtNonLinearCFA() {
      return onlyAtNonLinearCFA;
    }

    public boolean abstractAtFunction(LocationState location) {
      return alwaysAtFunction
          && (location.getLocationNode() instanceof FunctionEntryNode
              || location.getLocationNode().getEnteringSummaryEdge() != null);
    }

    boolean abstractAtLoop(LocationState location) {
      checkState(!alwaysAtLoop || loopHeads != null);
      return alwaysAtLoop && loopHeads.contains(location.getLocationNode());
    }
  }
}
