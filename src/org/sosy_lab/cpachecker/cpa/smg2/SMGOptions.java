// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cpa.smg2.SMGOptions.SMGMergeOptions.isFunctionExitLocation;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

@SuppressWarnings("all")
@Options(prefix = "cpa.smg2")
public class SMGOptions {

  @Option(
      secure = true,
      description =
          "Log level of unknown value usage in this CPA. E.g. due to overapproximations, unhandled"
              + " cases etc. Can be used to warn of a possible inaccurate analysis (many unknown"
              + " values do not lead to an inaccurate analysis!), help with debugging etc.")
  private Level logLevelOfUnknownValueAssumptions = Level.FINE;

  public Level getLogLevelOfUnknownValueAssumptions() {
    return logLevelOfUnknownValueAssumptions;
  }

  @Option(
      secure = true,
      description =
          "Overapproximates all C pointers that point out of bounds to their originating memory to"
              + " also point towards all other pointers when comparing pointers using (in)equality"
              + " operators (== and !=). No effect on other relational operators (i.e. <,>,<=,>=)"
              + " or pointer arithmetics.")
  private boolean overapproximatePointerArithmeticsOutOfBoundsEquality = true;

  @Option(
      secure = true,
      description =
          "Overapproximates all logical C pointer comparisons that require (unknown) address"
              + " relations that are currently not handled. E.g. casting two distinct pointers that"
              + " are not equal to integer and then comparing both against a constant integer"
              + " literal that may be equal to both (the relation between the pointers says that"
              + " they can't be both equal to the same constant integer literal).")
  private boolean overapproximateMemoryAddressRelations = true;

  enum DIRECTION {
    FORWARD,
    BACKWARD
  }

  @Option(
      secure = true,
      description =
          "The direction in which values are assigned when a concrete error path is built (i.e. for"
              + " a counterexample-check or a witness). Forward assigns concrete values only if"
              + " they are known at the location. Backward does remember possible assignments from"
              + " before and carries them over.")
  private DIRECTION errorPathConcreteValueAssignmentDirection = DIRECTION.BACKWARD;

  @Option(
      secure = true,
      description =
          "Exports concrete variable assignments for internally created CPAchecker variables (e.g."
              + " '__CPAchecker_TMP_X') if true. Note: Those internal variables are used e.g. to"
              + " safe results of sub-expressions that are split up, including assumptions, and are"
              + " always used and exported. This options only controls whether this CPA exports"
              + " known concrete values for these variables in counterexamples and witnesses.")
  private boolean exportInternalVariableAssignments = true;

  @Option(
      secure = true,
      description =
          "If true, exports concrete variable assignments along the found path towards errors for"
              + " counterexamples and violation witnesses. If false, only the found path is"
              + " exported.")
  private boolean exportVariableAssignmentsForViolations = true;

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

  public DIRECTION getErrorPathConcreteValueAssignmentDirection() {
    return errorPathConcreteValueAssignmentDirection;
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
          "If this option is enabled, a memory allocation (e.g. malloc or array declaration) for "
              + "unknown memory sizes does not abort, but also does not create any memory.")
  private UnknownMemoryAllocationHandling handleUnknownMemoryAllocation =
      UnknownMemoryAllocationHandling.STOP_ANALYSIS;

  public enum ArithmeticUndefinedBehaviorHandling {
    WARN_AND_RETURN_UNKNOWN,
    WARN_AND_RETURN_ZERO,
    WARN_AND_RETURN_ONE,
    STOP_ANALYSIS
  }

  @Option(
      secure = true,
      description =
          "Specifies the handling for all concrete arithmetic and bitwise operations resulting in"
              + " undefined behavior. Examples: divisions by zero as a result of division or"
              + " remainder operations, or bitwise shift operations with a negative second"
              + " argument, or bitwise shift operations with a second argument exceeding the width"
              + " of the first arguments type.")
  protected ArithmeticUndefinedBehaviorHandling arithmeticUndefinedBehaviorHandling =
      ArithmeticUndefinedBehaviorHandling.WARN_AND_RETURN_ZERO;

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
  private final SMGStopOptions stopOptions;

  public SMGOptions(Configuration config, @Nullable CFA cfa) throws InvalidConfigurationException {
    config.inject(this);
    ImmutableSet<CFANode> loopHeads = ImmutableSet.of();
    if (cfa != null && cfa.getAllLoopHeads().isPresent()) {
      loopHeads = cfa.getAllLoopHeads().orElseThrow();
    }
    ImmutableSet<CFANode> loopLeavingEdges = ImmutableSet.of();
    if (cfa != null && cfa.getLoopStructure().isPresent()) {
      ImmutableSet.Builder<CFANode> loopLeavingEdgesBuilder = ImmutableSet.builder();
      for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
        for (CFAEdge outgoingEdge : loop.getOutgoingEdges()) {
          loopLeavingEdgesBuilder.add(outgoingEdge.getSuccessor());
        }
      }
      loopLeavingEdges = loopLeavingEdgesBuilder.build();
    }
    abstractionOptions = new SMGAbstractionOptions(config, cfa, loopHeads);
    mergeOptions = new SMGMergeOptions(config, loopHeads, loopLeavingEdges);
    stopOptions = new SMGStopOptions(config);
  }

  public SMGMergeOptions getMergeOptions() {
    return mergeOptions;
  }

  public SMGStopOptions getStopOptions() {
    return stopOptions;
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

  public boolean overapproximateMemoryAddressRelations() {
    return overapproximateMemoryAddressRelations;
  }

  ArithmeticUndefinedBehaviorHandling getArithmeticUndefinedBehaviorHandling() {
    return arithmeticUndefinedBehaviorHandling;
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

  public boolean exportInternalVariableAssignments() {
    return exportInternalVariableAssignments;
  }

  public boolean exportVariableAssignmentsForViolations() {
    return exportVariableAssignmentsForViolations;
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

  public boolean isOverapproximatePointerArithmeticsOutOfBoundsEquality() {
    return overapproximatePointerArithmeticsOutOfBoundsEquality;
  }

  @Options(prefix = "cpa.smg2.stop")
  public static class SMGStopOptions {

    public SMGStopOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    public enum StopPolicy {
      /**
       * Tries to argue about stop by comparing the 2 states for entailment without using the merge
       * procedure and the merge-status.
       */
      // TODO: wouldn't SEP be more accurate?
      NO_MERGE,
      /**
       * Equal to NO_MERGE, except for deciding entailment for shape-abstractions, in which case the
       * merge-status of a merge of the 2 states in stop is used to argue only about shape. This is
       * more accurate for shape-abstractions than NO_MERGE.
       */
      TRY_MERGE_FOR_SHAPE_ABSTRACTION_ONLY,
      /**
       * Tries to utilize information from previous merges in merge operator to decide stop. If that
       * is not possible, falls back to the default stop check (i.e. NO_MERGE).
       */
      PREVIOUS_MERGE_INFO_ONLY_WITH_FALLBACK_TO_NO_MERGE,
      /**
       * Only utilize information from merges (by merging the 2 states and checking the
       * merge-status) to decide stop. Previous merges are not taken into account.
       */
      MERGE_EXCLUSIVLY,
      /**
       * Behaves as TRY_MERGE, except that should a previous merge status indicate that the 2 states
       * are definetly not entailed, stop takes a shortcut, returning false.
       */
      TRY_MERGE_SMART,
      /**
       * Tries to utilize information from previous merges to decide stop. If that is not possible,
       * the states are checked for stop using the merge precedure (but no merged state is used or
       * stored!). Should this check also not yield strong enough information, falls back to the
       * default stop check (i.e. NO_MERGE).
       */
      TRY_MERGE
    }

    @Option(
        secure = true,
        name = "stopOperatorPolicy",
        description =
            "Which stop policy (this always checks MergePolicy ENTAILMENT_ONLY) to apply when"
                + " deciding stop in the stop-operator:\n"
                + "NO_MERGE: Tries to argue about stop by comparing the 2 states for entailment"
                + " without using the merge procedure and the merge-status.\n"
                + "TRY_MERGE_FOR_SHAPE_ABSTRACTION_ONLY (default): Equal to NO_MERGE, except for"
                + " deciding entailment for shape-abstractions, in which case the merge-status of a"
                + " merge of the 2 states in stop is used to argue only about shape. This is more"
                + " accurate for shape-abstractions than NO_MERGE.\n"
                + "PREVIOUS_MERGE_INFO_ONLY: Tries to utilize information from previous merges in"
                + " merge operator to decide stop. If that is not possible, falls back to the"
                + " default stop check (i.e. NO_MERGE)\n"
                + "MERGE_EXCLUSIVLY: Behaves as TRY_MERGE, except that should a previous merge"
                + " status indicate that the 2 states are definetly not entailed, stop takes a"
                + " shortcut, returning false.\n"
                + "TRY_MERGE_SMART: Behaves as TRY_MERGE, except that should a previous merge"
                + " status indicate that the 2 states are definetly not entailed, stop takes a"
                + " shortcut, returning false.\n"
                + "TRY_MERGE: Tries to utilize information from previous merges to decide stop. If"
                + " that is not possible, the states are checked for stop using the merge precedure"
                + " (but no merged state is used or stored!).Should this check also not yield"
                + " strong enough information, falls back to the default stop check (i.e."
                + " NO_MERGE).")
    private StopPolicy policyForStopOperator = StopPolicy.TRY_MERGE_FOR_SHAPE_ABSTRACTION_ONLY;

    public StopPolicy getPolicyForStopOperator() {
      return policyForStopOperator;
    }
  }

  @Options(prefix = "cpa.smg2.merge")
  public static class SMGMergeOptions {

    // TODO: include value behavior in this? Or make another option for them?
    //  I am leaning towards new option.
    public enum MergePolicy {
      SEP,
      /** We merge states only if they are isomorph. No abstraction is ever performed. */
      ISOMORPHISM_ONLY,
      /**
       * We merge states only if they are isomorph, or one entails the other. No abstraction is ever
       * performed.
       */
      ENTAILMENT_ONLY,
      /**
       * We merge states only if they are isomorph, or one entails the other. Abstraction is allowed
       * within the bounds of entailment.
       */
      ENTAILMENT_WITH_ABSTRACTION,
      /**
       * We try to merge states (s1 and s2) irregardless of entailment or isomorphism, i.e. states
       * that are semantically incomparable in the form: s1 ⊆ merge_result ⊇ s2 ∧ s1 ⊉ s2 ∧ s1 ⊈ s2.
       * Merging may still fail to preserve soundness. No abstraction is performed while merging.
       */
      MERGE_WITHOUT_ABSTRACTION,
      /**
       * We try to merge states (s1 and s2) irregardless of entailment or isomorphism, i.e. states
       * that are semantically incomparable in the form: s1 ⊆ merge_result ⊇ s2 ∧ s1 ⊉ s2 ∧ s1 ⊈ s2.
       * Merging may still fail to preserve soundness. This includes abstraction while merging.
       */
      MERGE_WITH_ABSTRACTION
    }

    boolean isMergeSep() throws InvalidConfigurationException {
      if (generalMergePolicy == null) {
        throw new InvalidConfigurationException(
            "Option cpa.smg2.merge.generalPolicy may not be empty!");
      }
      return generalMergePolicy == MergePolicy.SEP
          && (mergePolicyOnLoopClosingEdges == null
              || mergePolicyOnLoopClosingEdges == MergePolicy.SEP)
          && (mergePolicyOnLoopHead == null || mergePolicyOnLoopHead == MergePolicy.SEP)
          && (mergePolicyOnFunctionExit == null || mergePolicyOnFunctionExit == MergePolicy.SEP);
    }

    @Option(
        secure = true,
        name = "generalPolicy",
        description =
            "Which merge policy to apply. This option sets a default policy that is applied on each"
                + " location. This is overridden by the following options at their specified"
                + " locations:  'cpa.smg2.merge.policyOnLoopClosing',"
                + " 'cpa.smg2.merge.policyOnFunctionExit', 'cpa.smg2.merge.policyOnLoopHead'."
                + " Setting this option, as well as 'cpa.smg2.merge.policyOnLoopClosing',"
                + " 'cpa.smg2.merge.policyOnFunctionExit' and 'cpa.smg2.merge.policyOnLoopHead' to"
                + " SEP, switches this CPA to use MERGE-SEP exclusivly.")
    private MergePolicy generalMergePolicy = MergePolicy.SEP;

    @Option(
        secure = true,
        name = "policyOnLoopClosing",
        description =
            "Which merge policy to apply when leaving a loop. This overrides the policy set by"
                + " option 'cpa.smg2.merge.generalPolicy' at loop-leaving locations if not empty."
                + " For Merge-SEP see option 'cpa.smg2.merge.generalPolicy'.")
    private MergePolicy mergePolicyOnLoopClosingEdges;

    @Option(
        secure = true,
        name = "policyOnLoopHead",
        description =
            "Which merge policy to apply on loop-heads. This overrides the policy set by option"
                + " 'cpa.smg2.merge.generalPolicy' at loop-heads if not empty. For Merge-SEP see"
                + " option 'cpa.smg2.merge.generalPolicy'.")
    private MergePolicy mergePolicyOnLoopHead;

    @Option(
        secure = true,
        name = "policyOnFunctionExit",
        description =
            "Which merge policy to apply when leaving a function. This overrides the policy set by"
                + " option 'cpa.smg2.merge.generalPolicy' at function exits if not empty. For"
                + " Merge-SEP see option 'cpa.smg2.merge.generalPolicy'.")
    private MergePolicy mergePolicyOnFunctionExit;

    @Option(
        secure = true,
        name = "exclusivelyEqualLocations",
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

    private final ImmutableSet<CFANode> loopHeads;
    private final ImmutableSet<CFANode> loopLeavingEdges;

    public SMGMergeOptions(
        Configuration config,
        ImmutableSet<CFANode> pLoopHeads,
        ImmutableSet<CFANode> pLoopLeavingEdges)
        throws InvalidConfigurationException {
      config.inject(this);
      loopHeads = pLoopHeads;
      loopLeavingEdges = pLoopLeavingEdges;
    }

    public MergePolicy getMergePolicyForLocation(LocationState location) {
      if (mergePolicyOnLoopHead != null && loopHeads.contains(location.getLocationNode())) {
        return mergePolicyOnLoopHead;
      } else if (mergePolicyOnLoopClosingEdges != null && isLoopLeavingLocation(location)) {
        return mergePolicyOnLoopClosingEdges;
      } else if (mergePolicyOnFunctionExit != null && isFunctionExitLocation(location)) {
        return mergePolicyOnFunctionExit;
      }
      checkState(
          generalMergePolicy != null, "Option cpa.smg2.merge.generalPolicy may not be empty!");
      return generalMergePolicy;
    }

    private boolean isLoopLeavingLocation(LocationState location) {
      // TODO: check whether this contains all of these edges!
      return loopLeavingEdges.contains(location);
    }

    static boolean isFunctionExitLocation(LocationState location) {
      return location.getLocationNode() instanceof FunctionExitNode;
    }

    public boolean mergeOnlyWithAbstractionPresent() {
      return mergeOnlyWithAbstractionPresent;
    }

    public boolean isOverapproximateSymbolicConstraints() {
      return overapproximateSymbolicConstraints;
    }

    public boolean isAllowSymbolicValueRenaming() {
      return false; // allowSymbolicRenamings;
    }

    public boolean isOverapproximateConcreteValues() {
      return overapproximateConcreteValues;
    }
  }

  @Options(prefix = "cpa.smg2.abstraction")
  public static class SMGAbstractionOptions {

    public enum PrecisionAdjustmentShapeAbstraction {
      NEVER,
      FUNCTION_ENTRY_ON_RECURSION,
      FUNCTION_EXIT_ON_RECURSION,
      FUNCTION_ENTRY_AND_EXIT_ON_RECURSION,
      ALWAYS_AT_LOCATIONS_SET_TO_ABSTRACT
    }

    @Option(
        secure = true,
        description =
            "Selects whether we want to use our custom SMG fix-point algorithm located in the SMG"
                + " based precision-adjustment to detect shape abstractions. Careful when combining"
                + " with merge based abstraction, as this might prevent merge from abstracting"
                + " correctly. But since the merge operator can not abstract shapes in recursive"
                + " stack frames, using any '*_ON_RECURSION' setting is safe with merge based"
                + " abstraction. ALWAYS_AT_LOCATIONS_SET_TO_ABSTRACT is only used for locations"
                + " enabled in options 'cpa.smg2.abstraction', for example 'alwaysAtLoopHeads'. All"
                + " '*ON_RECURSION' options are automatically always enabled at their described"
                + " locations.")
    private PrecisionAdjustmentShapeAbstraction usePrecisionAdjustmentForShapeAbstraction =
        PrecisionAdjustmentShapeAbstraction.ALWAYS_AT_LOCATIONS_SET_TO_ABSTRACT;

    @Option(
        secure = true,
        description =
            "If true, abstraction via precision adjustment is performed for all branching points of"
                + " the CFA.")
    private boolean alwaysAtBranch = false;

    @Option(
        secure = true,
        description =
            "If true, abstraction via precision adjustment is performed for all join points of the"
                + " CFA.")
    private boolean alwaysAtJoin = false;

    @Option(
        secure = true,
        description =
            "If true, abstraction via precision adjustment is performed for all function calls"
                + " (function entry edges in the CFA).")
    private boolean alwaysOnFunctionEntry = false;

    @Option(
        secure = true,
        description =
            "If true, abstraction via precision adjustment is performed for all function returns.")
    private boolean alwaysOnFunctionExit = false;

    @Option(
        secure = true,
        description =
            "If true, abstraction via precision adjustment is performed at loop-heads. Option ")
    private boolean alwaysAtLoopHeads = true;

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
            "The minimum list segments directly following each other, with matching values needed"
                + " to abstract them. Minimum allowed values is 2. Only works for abstraction based"
                + " on option 'usePrecisionAdjustmentForShapeAbstraction'.")
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
    private int listAbstractionMaximumIncreaseLengthThreshold = 25;

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
        description =
            "Abstraction of all detected linked lists of minimum length set by option"
                + " 'listAbstractionMinimumLengthThreshold' at all locations set to abstract. This"
                + " is independent of (list-)abstraction via merging.")
    private boolean abstractLinkedListsInPrecAdjustment = true;

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

    @Option(
        secure = true,
        description =
            "Sets behavior of the analysis when errors/exceptions are encountered when abstracting."
                + " STOP_CPACHECKER: stops CPAchecker with a RuntimeException. STOP_CURRENT: throws"
                + " a exception that stops only the CPA it is thrown in. IGNORE: does not throw"
                + " anything and continues the analysis without abstracting the state causing the"
                + " error.")
    private AbstractionErrorHandling errorHandling = AbstractionErrorHandling.IGNORE;

    public enum AbstractionErrorHandling {
      STOP_CPACHECKER,
      STOP_CURRENT,
      IGNORE
    }

    private final ImmutableSet<CFANode> loopHeads;

    public SMGAbstractionOptions(
        Configuration config, @Nullable CFA pCfa, ImmutableSet<CFANode> pLoopHeads)
        throws InvalidConfigurationException {
      config.inject(this);
      loopHeads = pLoopHeads;
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

    public boolean checkAbstractLinkedListsInPrecisionAdjustmentFor(
        LocationState loc, final SMGState currentState) {
      return switch (usePrecisionAdjustmentForShapeAbstraction) {
        case NEVER -> false;
        case FUNCTION_ENTRY_AND_EXIT_ON_RECURSION ->
            (checkAbstractOnFunctionExitFor(loc) || checkAbstractOnFunctionEntryFor(loc))
                && currentState.hasRecursionOfDepthGreaterEqual(
                    getListAbstractionMinimumLengthThreshold());
        case FUNCTION_EXIT_ON_RECURSION ->
            checkAbstractOnFunctionExitFor(loc)
                && currentState.hasRecursionOfDepthGreaterEqual(
                    getListAbstractionMinimumLengthThreshold());
        case FUNCTION_ENTRY_ON_RECURSION ->
            checkAbstractOnFunctionEntryFor(loc)
                && currentState.hasRecursionOfDepthGreaterEqual(
                    getListAbstractionMinimumLengthThreshold());
        case ALWAYS_AT_LOCATIONS_SET_TO_ABSTRACT -> checkAbstractInPrecAdjustmentFor(loc);
      };
    }

    public boolean isUsePrecAdjustmentAbstraction() {
      return usePrecisionAdjustmentForShapeAbstraction
          == PrecisionAdjustmentShapeAbstraction.ALWAYS_AT_LOCATIONS_SET_TO_ABSTRACT;
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

    /**
     * This method determines whether to abstract at each location.
     *
     * @return whether an abstraction should be computed at each location
     */
    boolean checkAbstractInPrecAdjustmentFor(LocationState location) {
      return checkAbstractAtLoopHeadFor(location)
          || checkAbstractOnFunctionExitFor(location)
          || checkAbstractOnFunctionEntryFor(location)
          || checkAbstractAtBranchingLocationFor(location)
          || checkAbstractAtJoiningLocationFor(location);
    }

    private boolean checkAbstractAtBranchingLocationFor(LocationState location) {
      return alwaysAtBranch && location.getLocationNode().getNumLeavingEdges() > 1;
    }

    private boolean checkAbstractAtJoiningLocationFor(LocationState location) {
      return alwaysAtJoin && location.getLocationNode().getNumEnteringEdges() > 1;
    }

    private boolean checkAbstractOnFunctionEntryFor(LocationState location) {
      return alwaysOnFunctionEntry
          && (location.getLocationNode() instanceof FunctionEntryNode
              || location.getLocationNode().getEnteringSummaryEdge() != null);
    }

    private boolean checkAbstractOnFunctionExitFor(LocationState location) {
      // TODO: check this for summary edges!
      return alwaysOnFunctionExit && isFunctionExitLocation(location);
    }

    private boolean checkAbstractAtLoopHeadFor(LocationState location) {
      return alwaysAtLoopHeads && loopHeads.contains(location.getLocationNode());
    }

    public AbstractionErrorHandling errorHandling() {
      return errorHandling;
    }
  }
}
