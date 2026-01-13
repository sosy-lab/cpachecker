// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.BuiltinFunctions.getParameterTypeOfBuiltinPopcountFunction;
import static org.sosy_lab.cpachecker.util.StandardFunctions.isMemoryAllocatingFunction;
import static org.sosy_lab.cpachecker.util.StandardFunctions.isMemoryDeallocatingFunction;
import static org.sosy_lab.cpachecker.util.StandardFunctions.isMemoryReallocatingFunction;
import static org.sosy_lab.cpachecker.util.StandardFunctions.isStandardByteInputFunction;
import static org.sosy_lab.cpachecker.util.StandardFunctions.isStandardInputOrOutputFunction;
import static org.sosy_lab.cpachecker.util.StandardFunctions.isStandardStringInputFunction;
import static org.sosy_lab.cpachecker.util.StandardFunctions.isStandardWideCharInputFunction;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions.UnknownFunctionHandling;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.ConstraintFactory;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.SatisfiabilityAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;
import org.sosy_lab.cpachecker.util.StandardFunctions;
import org.sosy_lab.cpachecker.util.smg.SMGProveNequality;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentStack;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGCPABuiltins {

  private static final UniqueIdGenerator U_ID_GENERATOR = new UniqueIdGenerator();

  private final SMGCPAExpressionEvaluator evaluator;

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;
  private final SMGCPAExportOptions exportSMGOptions;
  private final SMGOptions options;

  public SMGCPABuiltins(
      SMGCPAExpressionEvaluator pExpressionEvaluator,
      SMGOptions pOptions,
      SMGCPAExportOptions pExportSMGOptions,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger) {
    evaluator = pExpressionEvaluator;
    machineModel = pMachineModel;
    logger = pLogger;
    exportSMGOptions = pExportSMGOptions;
    options = pOptions;
  }

  private static final int MEMSET_BUFFER_PARAMETER = 0;
  private static final int MEMSET_CHAR_PARAMETER = 1;
  private static final int MEMSET_COUNT_PARAMETER = 2;
  private static final int MEMCPY_TARGET_PARAMETER = 0;
  private static final int MEMCPY_SOURCE_PARAMETER = 1;
  private static final int MEMCPY_SIZE_PARAMETER = 2;
  private static final int MEMCMP_CMP_TARGET1_PARAMETER = 0;
  private static final int MEMCMP_CMP_TARGET2_PARAMETER = 1;
  private static final int MEMCMP_CMP_SIZE_PARAMETER = 2;
  private static final int MALLOC_PARAMETER = 0;
  private static final int STRCMP_FIRST_PARAMETER = 0;
  private static final int STRCMP_SECOND_PARAMETER = 1;

  private static final String VERIFIER_NONDET_PREFIX = "__VERIFIER_nondet_";

  /**
   * Returns true if the functionName equals a known C builtin function (either defined by the C
   * standard or extensions like GNU (GCC)) OR our internal handling for atexit
   * (__CPACHECKER_atexit_next).
   */
  boolean isABuiltIn(String functionName) {
    // __CPACHECKER_atexit_next is not a constant function, but returns a different function
    // pointer from the atexit stack every time it is being called. We model this by returning a
    // fresh variable that may point to any function in the program. The function pointer CPA,
    // which will be run in parallel, tracks the actual target of the pointer and makes sure
    // that the right function is always called.
    return BuiltinFunctions.isBuiltinFunction(functionName)
        || functionName.equals("__CPACHECKER_atexit_next")
        || StandardFunctions.C11_ALL_FUNCTIONS.contains(functionName);
  }

  /**
   * True for external allocation functions, i.e. "ext_allocation", that are supported.
   *
   * @param functionName name of the function to check.
   * @return true for the specified names, false else.
   */
  private boolean isExternalAllocationFunction(String functionName) {
    return options.getExternalAllocationFunction().contains(functionName);
  }

  /**
   * Handles functions without body like built-in C functions (either from the standard or
   * extensions like GNU (GCC)), externally allocated functions, except math functions, which should
   * be handled by the ValueVisitor. If no such function is found, handling is decided by the option
   * for unknown functions. Functions without return value return a bundled UNKNOWN value that can
   * be ignored.
   *
   * @param funCallExpr {@link CFunctionCallExpression} that has been checked for all other known
   *     functions (math functions etc.).
   * @param functionName Name of the function.
   * @param state current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return the result of the function call and the state for it, which may be an error state!
   * @throws CPATransferException in case of a critical error the SMGCPA can't handle.
   */
  public List<ValueAndSMGState> handleFunctionCallWithoutBody(
      CFunctionCallExpression funCallExpr, String functionName, SMGState state, CFAEdge cfaEdge)
      throws CPATransferException {

    if (isSafeFunction(functionName)) {
      return handleSafeFunction(functionName, state, funCallExpr, cfaEdge);
    }

    if (isCompetitionNondeterministicFunction(functionName)) {
      return handleVerifierNondetGeneratorFunction(functionName, state, cfaEdge);
    }

    if (isExternalAllocationFunction(functionName)) {
      return evaluateExternalAllocationFunction(funCallExpr, state, functionName);
    }

    return handleBuiltinFunctionCall(funCallExpr, functionName, state, cfaEdge);
  }

  /**
   * True for functions generating nondeterministic values defined by the Competition on Software
   * Verification (SV-COMP). E.g. __VERIFIER_nondet_int().
   */
  public static boolean isCompetitionNondeterministicFunction(String functionName) {
    return functionName.contains(VERIFIER_NONDET_PREFIX);
  }

  /**
   * Handles all functions generating nondeterministic values defined by the Competition on Software
   * Verification (SV-COMP). E.g. __VERIFIER_nondet_int().
   */
  private List<ValueAndSMGState> handleVerifierNondetGeneratorFunction(
      String pFunctionName, SMGState pState, CFAEdge pCfaEdge)
      throws SMGException, UnsupportedCodeException {
    // Allowed (SVCOMP26): {bool, char, int, int128, float, double, loff_t, long, longlong, pchar,
    // pthread_t, sector_t, short, size_t, u32, uchar, uint, uint128, ulong, ulonglong, unsigned,
    // ushort} (no side effects, pointer for void *, etc.).

    // TODO: __VERIFIER_nondet_memory(void *, size_t): This function initializes the given memory
    // block with arbitrary values. The first argument must be a valid pointer to the start of a
    // memory block of the given size. The second argument specifies the size of the memory to
    // initialize and must match the size of the memory block that the first argument points to. The
    // dereference of any pointer value set through this method results in undefined behavior. This
    // means that pointer values must be explicitly set through different means before they can be
    // dereferenced.

    // TODO: consider casting directly to desired type?
    //  castCValue(uncastedValueAndState.getValue(), pTargetType);
    return switch (pFunctionName.replace(VERIFIER_NONDET_PREFIX, "")) {
      case "pointer" ->
          throw new UnsupportedCodeException(
              "Function "
                  + pFunctionName
                  + " is no longer supported by SV-COMP and is not supported by this analysis",
              pCfaEdge);
      case "bool",
          "char",
          "int",
          "int128",
          "float",
          "double",
          "long",
          "longlong",
          "pchar",
          "short",
          "size_t",
          "u32",
          "uchar",
          "uint",
          "uint128",
          "ulong",
          "ulonglong",
          "unsigned",
          "ushort" ->
          ImmutableList.of(ValueAndSMGState.ofUnknownValue(pState));
      case "loff_t", "pthread_t", "sector_t" ->
          throw new SMGException(
              "Function: " + pFunctionName + " is currently unsupported in all SMG analyses");
      default ->
          throw new SMGException(
              "Unknown and unhandled function: " + pFunctionName + " at " + pCfaEdge);
    };
  }

  /**
   * Handle a function call to a builtin function like malloc(), memcpy(), atexit() etc. Will
   * delegate to the unknown function handler if no function handling is defined.
   *
   * @param cfaEdge for logging/debugging.
   * @param funCallExpr {@link CFunctionCallExpression} that leads to a non memory allocating
   *     builtin function.
   * @param functionName The name of the function to be called.
   * @param state current {@link SMGState}.
   * @return the result of the function call and the state for it, which may be an error state!
   * @throws CPATransferException in case of a critical error the SMGCPA can't handle.
   */
  private List<ValueAndSMGState> handleBuiltinFunctionCall(
      CFunctionCallExpression funCallExpr, String functionName, SMGState state, CFAEdge cfaEdge)
      throws CPATransferException {

    if (isMemoryAllocatingFunction(functionName)) {
      // malloc(), calloc() etc.
      return evaluateConfigurableAllocationFunctionWithManualMemoryCleanup(
          funCallExpr, functionName, state, cfaEdge);
    } else if (isMemoryDeallocatingFunction(functionName)) {
      // free()
      return evaluateFree(funCallExpr, state, cfaEdge);
    } else if (isMemoryReallocatingFunction(functionName)) {
      // realloc()
      return evaluateRealloc(funCallExpr, state, cfaEdge);
    }

    if (isStandardByteInputFunction(functionName)
        || isStandardWideCharInputFunction(functionName)
        || isStandardStringInputFunction(functionName)) {
      // fgets(), sscanf() etc.
      return handleInputFunctions(state, funCallExpr, functionName, cfaEdge);
    }

    if (isStandardInputOrOutputFunction(functionName)) {
      // Only output functions as input are already handled above
      return checkAllParametersForValidityAndReturnUnknownValue(
          state, cfaEdge, funCallExpr, functionName);
    }

    return switch (functionName) {
      case "__builtin_popcount", "__builtin_popcountl", "__builtin_popcountll" ->
          handlePopcount(functionName, state, funCallExpr, cfaEdge);

      case "alloca", "__builtin_alloca" -> evaluateAlloca(funCallExpr, state, cfaEdge);

      case "memset" -> evaluateMemset(funCallExpr, state, cfaEdge);

      case "memcpy" -> evaluateMemcpy(funCallExpr, state, cfaEdge);

      case "memcmp" -> evaluateMemcmp(funCallExpr, state, cfaEdge);

      case "strcmp" -> evaluateStrcmp(funCallExpr, state, cfaEdge);

      case "__VERIFIER_BUILTIN_PLOT" -> {
        evaluateVBPlot(funCallExpr, state);
        yield ImmutableList.of(ValueAndSMGState.ofUnknownValue(state));
      }

      case "__builtin_va_start" -> evaluateVaStart(funCallExpr, cfaEdge, state);

      case "__builtin_va_arg" -> evaluateVaArg(funCallExpr, cfaEdge, state);

      case "__builtin_va_copy" -> evaluateVaCopy(funCallExpr, cfaEdge, state);

      case "__builtin_va_end" -> evaluateVaEnd(funCallExpr, cfaEdge, state);

      case "atexit" -> evaluateAtExit(funCallExpr, cfaEdge, state);

      case "__CPACHECKER_atexit_next" -> evaluateAtExitNext(state);

      default -> handleUnknownFunction(cfaEdge, funCallExpr, functionName, state);
    };
  }

  /*
   * function va_end for variable function parameters.
   */
  @SuppressWarnings("unused")
  private List<ValueAndSMGState> evaluateVaEnd(
      CFunctionCallExpression cFCExpression, CFAEdge pCfaEdge, SMGState pState) {
    Preconditions.checkArgument(cFCExpression.getParameterExpressions().size() == 1);
    // The first argument is the variable to be deleted
    CIdExpression firstIdArg =
        (CIdExpression) evaluateCFunctionCallToFirstParameterForVA(cFCExpression);
    SMGState currentState =
        pState.copyAndPruneFunctionStackVariable(firstIdArg.getDeclaration().getQualifiedName());

    return ImmutableList.of(ValueAndSMGState.ofUnknownValue(currentState));
  }

  private CExpression evaluateCFunctionCallToFirstParameterForVA(
      CFunctionCallExpression cFCExpression) {
    CExpression firstArg = cFCExpression.getParameterExpressions().getFirst();
    while (firstArg instanceof CCastExpression) {
      firstArg = ((CCastExpression) firstArg).getOperand();
    }
    if (firstArg instanceof CUnaryExpression) {
      firstArg = ((CUnaryExpression) firstArg).getOperand();
    }
    Preconditions.checkArgument(firstArg instanceof CIdExpression);
    return firstArg;
  }

  /*
   * function va_copy for variable function parameters.
   */
  @SuppressWarnings("unused")
  private List<ValueAndSMGState> evaluateVaCopy(
      CFunctionCallExpression cFCExpression, CFAEdge pCfaEdge, SMGState pState)
      throws CPATransferException {
    Preconditions.checkArgument(cFCExpression.getParameterExpressions().size() == 2);
    // The first argument is the destination
    CExpression destArg = cFCExpression.getParameterExpressions().getFirst();
    Preconditions.checkArgument(destArg instanceof CIdExpression);
    CIdExpression destIdArg = (CIdExpression) destArg;
    // The second argument is the source
    CExpression srcArg = cFCExpression.getParameterExpressions().get(1);
    Preconditions.checkArgument(srcArg instanceof CIdExpression);
    CIdExpression srcIdArg = (CIdExpression) srcArg;
    // Check the types lazy
    Preconditions.checkArgument(destIdArg.getExpressionType().equals(srcIdArg.getExpressionType()));
    // The size should be equal as the types are
    BigInteger sizeInBits = evaluator.getBitSizeof(pState, srcIdArg);
    List<ValueAndSMGState> addressesAndStates =
        evaluator.readStackOrGlobalVariable(
            pState,
            srcIdArg.getName(),
            new NumericValue(BigInteger.ZERO),
            sizeInBits,
            SMGCPAExpressionEvaluator.getCanonicalType(srcIdArg));
    Preconditions.checkArgument(addressesAndStates.size() == 1);
    ValueAndSMGState addressAndState = addressesAndStates.getFirst();
    SMGState currentState = addressAndState.getState();
    if (addressAndState.getValue().isUnknown()) {
      // Critical error, should never happen
      throw new SMGException(
          "Critical error in builtin function va_copy. The source does not reflect a valid value"
              + " when read.");
    }
    currentState =
        currentState.writeToStackOrGlobalVariable(
            destIdArg.getDeclaration().getQualifiedName(),
            new NumericValue(BigInteger.ZERO),
            new NumericValue(sizeInBits),
            addressAndState.getValue(),
            destIdArg.getExpressionType(),
            pCfaEdge);
    return ImmutableList.of(ValueAndSMGState.ofUnknownValue(currentState));
  }

  /*
   * Function va_arg for variable arguments in functions.
   */
  @SuppressWarnings("unused")
  private List<ValueAndSMGState> evaluateVaArg(
      CFunctionCallExpression cFCExpression, CFAEdge pCfaEdge, SMGState pState)
      throws SMGException {
    // Preconditions.checkArgument(cFCExpression.getParameterExpressions().size() == 2);
    // The first argument is the va_list pointer CidExpression
    // CExpression srcArg = cFCExpression.getParameterExpressions().get(0);
    // Preconditions.checkArgument(srcArg instanceof CIdExpression);
    // CIdExpression srcIdArg = (CIdExpression) srcArg;
    // The second argument is the type to be read
    // CExpression typeArg = cFCExpression.getParameterExpressions().get(1);
    // Preconditions.checkArgument(srcArg instanceof CIdExpression);
    // CIdExpression srcIdArg = (CIdExpression) srcArg;
    // If the type is not compatible with the types saved in the array, behavior is undefined
    throw new SMGException(
        "Feature va_arg() not finished because our parser does not like the function.");

    // return null;
  }

  /*
   * function va_start for variable arguments in functions.
   * We assume that the given argument 1 will be the variable holding the current pointer, while the second arg gives us the type of the elements.
   */
  @SuppressWarnings("unused")
  private List<ValueAndSMGState> evaluateVaStart(
      CFunctionCallExpression cFCExpression, CFAEdge cfaEdge, SMGState pState)
      throws CPATransferException {

    SMGState currentState = pState;
    Preconditions.checkArgument(cFCExpression.getParameterExpressions().size() == 2);
    // The first argument is the target for the pointer to the array of values
    CExpression firstArg = evaluateCFunctionCallToFirstParameterForVA(cFCExpression);

    // The second argument is the type of the argument before the variable args start
    CExpression secondArg = cFCExpression.getParameterExpressions().get(1);
    StackFrame currentStack = pState.getMemoryModel().getStackFrames().peek();
    CParameterDeclaration paramDecl =
        currentStack.getFunctionDefinition().getParameters().getLast();
    if (!paramDecl.getType().equals(secondArg.getExpressionType())) {
      // Log warning (gcc only throws a warning and it works anyway)
      logger.logf(
          Level.INFO,
          "The types of variable arguments (%s) do not match the last not "
              + "variable argument of the function (%s)",
          paramDecl.getType(),
          secondArg.getExpressionType());
    }
    Value sizeInBitsPointer = new NumericValue(evaluator.getBitSizeof(pState, firstArg));

    BigInteger sizeInBitsVarArg = evaluator.getBitSizeof(pState, secondArg);
    BigInteger overallSizeOfVarArgs =
        BigInteger.valueOf(currentStack.getVariableArguments().size()).multiply(sizeInBitsVarArg);

    ValueAndSMGState pointerAndState =
        evaluator.createHeapMemoryAndPointer(currentState, new NumericValue(overallSizeOfVarArgs));

    currentState = pointerAndState.getState();
    Value address = pointerAndState.getValue();

    List<SMGStateAndOptionalSMGObjectAndOffset> targets =
        firstArg.accept(
            new SMGCPAAddressVisitor(evaluator, currentState, cfaEdge, logger, options));
    Preconditions.checkArgument(targets.size() == 1);
    for (SMGStateAndOptionalSMGObjectAndOffset target : targets) {
      // We assume that there is only 1 valid returned target
      currentState = target.getSMGState();
      if (!target.hasSMGObjectAndOffset()) {
        return ImmutableList.of(ValueAndSMGState.ofUnknownValue(currentState));
      }
      SMGObject targetObj = target.getSMGObject();
      Value offset = target.getOffsetForObject();

      currentState =
          currentState.writeValueWithChecks(
              targetObj, offset, sizeInBitsPointer, address, firstArg.getExpressionType(), cfaEdge);
    }

    BigInteger offset = BigInteger.ZERO;
    for (Value varArg : currentStack.getVariableArguments()) {
      // Fill the arra with var args
      List<SMGState> writtenStates =
          currentState.writeValueTo(
              address,
              offset,
              new NumericValue(sizeInBitsVarArg),
              varArg,
              SMGCPAExpressionEvaluator.getCanonicalType(secondArg),
              cfaEdge);
      // Unlikely that someone throws an abstracted list into a var args
      Preconditions.checkArgument(writtenStates.size() == 1);
      currentState = writtenStates.getFirst();
      offset = offset.add(sizeInBitsVarArg);
    }

    return ImmutableList.of(ValueAndSMGState.ofUnknownValue(currentState));
  }

  /*
   * The atexit function from the C standard. Returns 0 for successful registration, non-zero otherwise.
   */
  private List<ValueAndSMGState> evaluateAtExit(
      CFunctionCallExpression cFCExpression, CFAEdge cfaEdge, SMGState pState)
      throws CPATransferException {
    // Get the CExpression for the first argument
    List<CExpression> argsExpr = cFCExpression.getParameterExpressions();
    Preconditions.checkArgument(argsExpr.size() == 1);
    CExpression fpExpr = argsExpr.getFirst();

    // Evaluate the expression
    SMGCPAValueVisitor valueVisitor =
        new SMGCPAValueVisitor(evaluator, pState, cfaEdge, logger, options);
    List<ValueAndSMGState> evalStates = fpExpr.accept(valueVisitor);
    Preconditions.checkArgument(evalStates.size() == 1);

    // Get the value for the expression and the new state
    Value atExitAddressValue = evalStates.getFirst().getValue();
    SMGState newState = evalStates.getFirst().getState();

    if (atExitAddressValue instanceof AddressExpression pAddressExpression) {
      Preconditions.checkArgument(
          pAddressExpression.getOffset() instanceof NumericValue addressOffset
              && addressOffset.bigIntegerValue().equals(BigInteger.ZERO));
      atExitAddressValue = pAddressExpression.getMemoryAddress();
    }

    ImmutableList.Builder<ValueAndSMGState> retBuilder = ImmutableList.builder();
    if (options.canAtexitFail()) {
      // TODO: return non-zero symbolic for symExec
      retBuilder.add(ValueAndSMGState.of(new NumericValue(BigInteger.ONE), pState));
    }

    newState =
        newState.copyAndReplaceMemoryModel(
            newState
                .getMemoryModel()
                .copyAndReplaceAtExitStack(
                    newState.getMemoryModel().getAtExitStack().pushAndCopy(atExitAddressValue)));
    // Push the value onto the stack and update our memory model
    return retBuilder.add(ValueAndSMGState.of(new NumericValue(0), newState)).build();
  }

  /*
   * This function is added to the CFA during the atexit transformation.
   * It gets the next handler from the atexit stack or returns the null pointer if the stack is
   * empty.
   */
  private List<ValueAndSMGState> evaluateAtExitNext(SMGState pState) {
    PersistentStack<Value> atExitStack = pState.getMemoryModel().getAtExitStack();
    if (atExitStack.isEmpty()) {
      // If the stack is empty return a null pointer
      return ImmutableList.of(ValueAndSMGState.of(new NumericValue(BigInteger.ZERO), pState));
    } else {
      // Otherwise, return the next pointer from the stack
      return ImmutableList.of(
          ValueAndSMGState.of(
              atExitStack.peek(),
              pState.copyAndReplaceMemoryModel(
                  pState.getMemoryModel().copyAndReplaceAtExitStack(atExitStack.popAndCopy()))));
    }
  }

  /**
   * Checks all function parameters for invalid pointer based inputs. To be used in methods that we
   * only simulate shallowly i.e. print() and returns an unknown value.
   *
   * @param pState current {@link SMGState}.
   * @param pCfaEdge the edge from which this function call originates.
   * @param cFCExpression the function call expression.
   * @return a list of states which may include error states.
   * @throws CPATransferException in case of errors the SMGCPA can not solve.
   */
  private List<ValueAndSMGState> checkAllParametersForValidityAndReturnUnknownValue(
      SMGState pState, CFAEdge pCfaEdge, CFunctionCallExpression cFCExpression, String functionName)
      throws CPATransferException {
    return ImmutableList.of(
        ValueAndSMGState.ofUnknownValue(
            checkAllParametersForValidity(pState, pCfaEdge, cFCExpression, functionName)
                .getFirst()));
  }

  /**
   * Checks all function parameters for invalid pointer based inputs. To be used in methods that we
   * only simulate shallowly i.e. print().
   *
   * @param pState current {@link SMGState}.
   * @param pCfaEdge the edge from which this function call originates.
   * @param cFCExpression the function call expression.
   * @return a list of states which may include error states.
   * @throws CPATransferException in case of errors the SMGCPA can not solve.
   */
  private List<SMGState> checkAllParametersForValidity(
      SMGState pState, CFAEdge pCfaEdge, CFunctionCallExpression cFCExpression, String functionName)
      throws CPATransferException {
    // check that we can safely read all args,
    // to avoid invalid-derefs like   int * p; printf("%d", *p);
    SMGState currentState = pState;
    boolean isPrint = functionName.equals("printf");
    for (CExpression param : cFCExpression.getParameterExpressions()) {
      SMGCPAValueVisitor valueVisitor =
          new SMGCPAValueVisitor(evaluator, currentState, pCfaEdge, logger, options);
      if (param instanceof CPointerExpression
          || param instanceof CFieldReference
          || param instanceof CArraySubscriptExpression) {

        for (ValueAndSMGState valueAndState : param.accept(valueVisitor)) {
          // We only want error states
          currentState = valueAndState.getState();
        }
      } else if (param instanceof CIdExpression idExpr) {
        for (ValueAndSMGState valueAndState : param.accept(valueVisitor)) {
          // We want error states from dereferences etc
          currentState = valueAndState.getState();
          if (isPrint
              && idExpr.getExpressionType() instanceof CPointerType ptrType
              && ptrType.getType() instanceof CSimpleType simpleType
              && simpleType.getType().equals(CBasicType.CHAR)) {
            Value address = valueAndState.getValue();
            if (address instanceof AddressExpression addrExpr) {
              ValueAndSMGState addressTransformedAndState =
                  currentState.transformAddressExpression(addrExpr);
              address = addressTransformedAndState.getValue();
              currentState = addressTransformedAndState.getState();
            }
            if (address.isUnknown()) {
              // Deref unknown value fails always
              currentState = currentState.withUnknownPointerDereferenceWhenReading(address);
            } else {
              List<SMGStateAndOptionalSMGObjectAndOffset> deref =
                  currentState.dereferencePointer(address);
              Preconditions.checkArgument(deref.size() == 1);
              SMGStateAndOptionalSMGObjectAndOffset targetAndState = deref.getFirst();
              currentState = targetAndState.getSMGState();
              if (targetAndState.hasSMGObjectAndOffset()) {
                SMGObject derefedObj = targetAndState.getSMGObject();
                if (!currentState.getMemoryModel().isObjectValid(derefedObj)) {
                  currentState = currentState.withInvalidDerefForRead(derefedObj, pCfaEdge);
                }
              }
            }
          }
        }
      }
    }
    return ImmutableList.of(currentState);
  }

  /**
   * Checks validity of all input. Returns unknown. Overflows for all input parameters with type
   * char *, but ignores const char * (e.g. format inputs). If returnBufferPointer is false, UNKNOWN
   * is returned by the function, else the pointer of the buffer, with an exception if there are
   * multiple possible buffers. Also uses the size argument for the buffer for
   * usesBufferSizeArgument true. This method expects NO format specifier to be used in the calling
   * method!
   *
   * @param returnBufferPointer if true: tries to return the buffer pointer on success.
   * @param usesBufferSizeArgument if true: searches for ONE int argument first, and assumes that
   *     the size given here is read into the buffer (-1 for terminating null char).
   */
  private List<ValueAndSMGState> checkParamValidityWithBufferOverflowsWithReturn(
      SMGState pState,
      CFAEdge pCfaEdge,
      CFunctionCallExpression cFCExpression,
      String functionName,
      boolean returnBufferPointer,
      boolean usesBufferSizeArgument)
      throws CPATransferException {
    return checkParamValidityWithBufferOverflowsWithReturn(
        pState,
        pCfaEdge,
        cFCExpression,
        functionName,
        returnBufferPointer,
        usesBufferSizeArgument,
        Optional.empty());
  }

  /**
   * Checks validity of all input. Returns unknown. Overflows for all input parameters with type
   * char *, but ignores const char * (e.g. format inputs). If returnBufferPointer is false, UNKNOWN
   * is returned by the function, else the pointer of the buffer, with an exception if there are
   * multiple possible buffers. Also uses the size argument for the buffer for
   * usesBufferSizeArgument true.
   *
   * @param returnBufferPointer if true: tries to return the buffer pointer on success.
   * @param usesBufferSizeArgument if true: searches for ONE int argument first, and assumes that
   *     the size given here is read into the buffer (-1 for terminating null char).
   * @param formatArgumentIndex if present, index of format specifier. Else no format spec expected!
   */
  private List<ValueAndSMGState> checkParamValidityWithBufferOverflowsWithReturn(
      SMGState pState,
      CFAEdge pCfaEdge,
      CFunctionCallExpression cFCExpression,
      String functionName,
      boolean returnBufferPointer,
      boolean usesBufferSizeArgument,
      Optional<Integer> formatArgumentIndex)
      throws CPATransferException {
    // TODO: add failing w option.

    SMGState currentState = pState;
    Optional<Value> bufferWriteSizeArgument = Optional.empty();
    if (usesBufferSizeArgument) {
      for (int arg = 0; arg < cFCExpression.getParameterExpressions().size(); arg++) {
        CExpression argument = cFCExpression.getParameterExpressions().get(arg);
        CType argumentType =
            SMGCPAExpressionEvaluator.getCanonicalType(argument.getExpressionType());
        CType functionParameterType =
            SMGCPAExpressionEvaluator.getCanonicalType(
                cFCExpression.getDeclaration().getParameters().get(arg).getType());
        if (functionParameterType instanceof CSimpleType simpleType
            && simpleType.getType().equals(CBasicType.INT)) {
          List<ValueAndSMGState> sizeArgAndState =
              new SMGCPAValueVisitor(evaluator, currentState, pCfaEdge, logger, options)
                  .evaluate(argument, argumentType);

          checkState(sizeArgAndState.size() == 1);
          checkState(bufferWriteSizeArgument.isEmpty());
          currentState = sizeArgAndState.getFirst().getState();
          bufferWriteSizeArgument = Optional.of(sizeArgAndState.getFirst().getValue());
        }
      }
    }

    List<SMGState> checkedStates =
        checkAllParametersForValidity(currentState, pCfaEdge, cFCExpression, functionName);
    checkState(checkedStates.size() == 1);
    Value returnValue = UnknownValue.getInstance();
    SMGState finalState = checkedStates.getFirst();

    // Potentially the size of the input poured into any string buffer.
    // The buffer size argument might cut this down! Or there might be no format.
    Optional<Value> sizeFillingStringBuffersInBits = Optional.empty();
    // stringFormatSpecifierUsed only makes sense if formatArgumentIndex is non-empty!
    boolean stringFormatSpecifierUsed = false;

    for (int arg = 0; arg < cFCExpression.getParameterExpressions().size(); arg++) {
      CExpression argument = cFCExpression.getParameterExpressions().get(arg);
      CType argumentType = SMGCPAExpressionEvaluator.getCanonicalType(argument.getExpressionType());
      CType functionParameterType;
      if (arg < cFCExpression.getDeclaration().getParameters().size()) {
        functionParameterType =
            SMGCPAExpressionEvaluator.getCanonicalType(
                cFCExpression.getDeclaration().getParameters().get(arg).getType());
      } else {
        // Variable arguments used. Those are not declared. Usually just buffers.
        functionParameterType = argumentType;
      }

      if (usesBufferSizeArgument
          && functionParameterType instanceof CSimpleType simpleType
          && simpleType.getType().equals(CBasicType.INT)) {
        continue; // Skip size arg, we already processed it!
      }

      if (!functionParameterType.isConst()
          && functionParameterType.equals(CPointerType.POINTER_TO_CHAR)) {
        // String Buffers
        List<ValueAndSMGState> overflowBufferAndState =
            new SMGCPAValueVisitor(evaluator, finalState, pCfaEdge, logger, options)
                .evaluate(argument, argumentType);

        checkState(overflowBufferAndState.size() == 1);
        finalState = overflowBufferAndState.getFirst().getState();

        Value bufferAddress = overflowBufferAndState.getFirst().getValue();
        Value targetBufferOffsetInBits = new NumericValue(BigInteger.ZERO);
        if (bufferAddress instanceof AddressExpression addrExpr) {
          bufferAddress = addrExpr.getMemoryAddress();
          targetBufferOffsetInBits = addrExpr.getOffset();
        }

        if (!returnValue.equals(UnknownValue.getInstance()) && !isNumericZero(returnValue)) {
          // Multiple "buffers" in this function, can't return one, return unknown
          returnBufferPointer = false;
          returnValue = UnknownValue.getInstance();
        } else if (returnBufferPointer && !isNumericZero(returnValue)) {
          // Remember buffer as return value (if that's 0, we also return 0),
          //  except if its already 0, then keep the return value.
          returnValue = bufferAddress;
        }
        // TODO: add solver handling for checks

        if (!(returnValue instanceof NumericValue)
            && !returnValue.isUnknown()
            && usesBufferSizeArgument) {
          // Overflow if size not fitting and remove buffer values (i.e. make them unknown)
          checkArgument(bufferWriteSizeArgument.isPresent());
          Value bufferWriteSizeArg = bufferWriteSizeArgument.orElseThrow();

          List<SMGStateAndOptionalSMGObjectAndOffset> maybeObjects =
              finalState.dereferencePointer(returnValue);
          checkState(maybeObjects.size() == 1);
          SMGStateAndOptionalSMGObjectAndOffset maybeObjAndInfo = maybeObjects.getFirst();
          checkState(maybeObjAndInfo.hasSMGObjectAndOffset());
          finalState = maybeObjAndInfo.getSMGState();
          Value objOffsetInBits = maybeObjAndInfo.getOffsetForObject();
          SMGObject obj = maybeObjAndInfo.getSMGObject();

          if (obj.getSize() instanceof NumericValue numericObjSize
              && bufferWriteSizeArg instanceof NumericValue numericBufferWriteSizeArg
              && targetBufferOffsetInBits instanceof NumericValue numericBufferOffsetInBits
              && objOffsetInBits instanceof NumericValue numericObjOffsetInBits) {
            // copyAndRemoveAllEdgesFrom ignores targetBufferOffsetInBits currently, so it has to
            // be 0
            checkState(numericBufferOffsetInBits.bigIntegerValue().compareTo(BigInteger.ZERO) == 0);
            BigInteger bigIntBufferOffsetInBits =
                numericBufferOffsetInBits
                    .bigIntegerValue()
                    .add(numericObjOffsetInBits.bigIntegerValue());

            BigInteger charBitSize =
                BigInteger.valueOf(machineModel.getSizeofInBits(CNumericTypes.CHAR));
            BigInteger writeSizeInBits =
                numericBufferWriteSizeArg.bigIntegerValue().multiply(charBitSize);
            finalState =
                finalState.copyAndRemoveAllEdgesFrom(
                    returnValue, new NumericValue(writeSizeInBits));

            if (numericObjSize
                    .bigIntegerValue()
                    .compareTo(bigIntBufferOffsetInBits.add(writeSizeInBits))
                < 0) {
              // Overflow
              finalState = finalState.withInvalidWrite(bufferAddress);
            }

          } else {
            // Overaproximate overflow
            finalState =
                finalState.withInvalidWrite(bufferAddress).copyAndRemoveAllEdgesFrom(returnValue);
            // } else if (options.trackPredicates() && options.trackErrorPredicates()) {
            // TODO: Symex. Currently we overapproximate for symbolic values of any kind.

          }
        } else {
          // Overapproximate buffer overflow and remove edges
          checkArgument(bufferWriteSizeArgument.isEmpty());
          finalState =
              finalState.withInvalidWrite(bufferAddress).copyAndRemoveAllEdgesFrom(returnValue);
        }

      } else if (BuiltinFunctions.isFilePointer(argumentType)) {
        // STREAM. If 0, return 0.
        List<ValueAndSMGState> streamPtrAndState =
            new SMGCPAValueVisitor(evaluator, finalState, pCfaEdge, logger, options)
                .evaluate(argument, argumentType);

        checkState(streamPtrAndState.size() == 1);
        Value streamPointer = streamPtrAndState.getFirst().getValue();
        finalState = streamPtrAndState.getFirst().getState();

        if (isNumericZero(streamPointer)
            || (finalState.isPointer(streamPointer)
                && finalState.dereferencePointerWithoutMaterilization(streamPointer).isPresent()
                && finalState
                    .dereferencePointerWithoutMaterilization(streamPointer)
                    .orElseThrow()
                    .hasSMGObjectAndOffset()
                && finalState
                    .dereferencePointerWithoutMaterilization(streamPointer)
                    .orElseThrow()
                    .getSMGObject()
                    .isZero())) {
          checkState(!finalState.getMemoryModel().pointsToZeroPlus(streamPointer));
          returnValue = new NumericValue(0);
        } else if (!finalState.isPointer(streamPointer)) {
          // Ignore known and allowed file pointers that are not 0
          if (!(argument instanceof CIdExpression idExpr && idExpr.getName().equals("stdin"))) {
            // Unknown -> fail. TODO: overapproximate?
            throw new SMGException(
                "Error when handling C input function "
                    + functionName
                    + "(), can't handle unknown argument #"
                    + arg
                    + " of type "
                    + functionParameterType);
          }
        }

      } else if (functionParameterType.equals(CPointerType.POINTER_TO_CONST_CHAR)) {
        // format specifiers OR input string!
        if (formatArgumentIndex.isEmpty() || formatArgumentIndex.orElseThrow() != arg) {
          // Input string that is read from. Remember the size we can read!
          // (first argument only! Else this method is used or set-up wrongly!)
          checkArgument(arg == 0);

          List<ValueAndSMGState> inputStringBufferAndState =
              new SMGCPAValueVisitor(evaluator, finalState, pCfaEdge, logger, options)
                  .evaluate(argument, argumentType);

          checkState(inputStringBufferAndState.size() == 1);
          finalState = inputStringBufferAndState.getFirst().getState();

          Value stringBufferValue = inputStringBufferAndState.getFirst().getValue();
          Value stringBuffOffsetInBits = new NumericValue(0);
          if (stringBufferValue instanceof AddressExpression stringBuffAddrExpr) {
            stringBufferValue = stringBuffAddrExpr.getMemoryAddress();
            stringBuffOffsetInBits = stringBuffAddrExpr.getOffset();
          }

          checkArgument(!returnBufferPointer); // If fails, check and add functionality
          List<SMGStateAndOptionalSMGObjectAndOffset> maybeBufferObjects =
              finalState.dereferencePointer(stringBufferValue);
          checkState(maybeBufferObjects.size() == 1);
          SMGStateAndOptionalSMGObjectAndOffset maybeStringObjAndInfo =
              maybeBufferObjects.getFirst();
          checkState(maybeStringObjAndInfo.hasSMGObjectAndOffset());
          finalState = maybeStringObjAndInfo.getSMGState();
          Value objOffsetInBits = maybeStringObjAndInfo.getOffsetForObject();
          SMGObject stringBufferObj = maybeStringObjAndInfo.getSMGObject();
          checkState(sizeFillingStringBuffersInBits.isEmpty());

          sizeFillingStringBuffersInBits =
              Optional.of(
                  evaluator.subtractBitOffsetValues(
                      stringBufferObj.getSize(),
                      evaluator.addBitOffsetValues(
                          evaluator.addBitOffsetValues(
                              objOffsetInBits, stringBufferObj.getOffset()),
                          stringBuffOffsetInBits)));

        } else if (formatArgumentIndex.orElseThrow() == arg
            && argument instanceof CStringLiteralExpression stringArg) {
          // Format string, if there is an %s in there, we read a string. Remember where and use
          // for buffer later on
          if (stringArg.getContentWithoutNullTerminator().contains("%s")) {
            // List<String> splitSpecifiers =
            // Splitter.on('%').splitToList(stringArg.getContentWithoutNullTerminator());
            // splitSpecifiers now consists of a specifier as first char, potentially followed by
            // spaces. We might be able to make this method more precise for concrete input when
            // taking this into account.
            // for (int i = 0; i < splitSpecifiers.size(); i++) {
            // ...
            // }
            stringFormatSpecifierUsed = true;
          }

        } else {
          throw new SMGException(
              "Unexpected unhandled type argument "
                  + functionParameterType
                  + "in C input function call "
                  + functionName
                  + "() in "
                  + pCfaEdge);
        }

      } else if (argumentType instanceof CPointerType) {
        // Other buffers. Just empty them, except for char * and a previous %s specifier, those
        // can overflow if smaller than input allows, but also need to be emptied
        List<ValueAndSMGState> bufferAndState =
            new SMGCPAValueVisitor(evaluator, finalState, pCfaEdge, logger, options)
                .evaluate(argument, argumentType);

        checkState(bufferAndState.size() == 1);
        finalState = bufferAndState.getFirst().getState();
        Value bufferAddress = bufferAndState.getFirst().getValue();
        // Make buffer unknown
        if (formatArgumentIndex.isPresent() && sizeFillingStringBuffersInBits.isPresent()) {
          // If sizeFillingStringBuffersInBits is empty, means we don't know the size ->
          // overapproximate
          finalState =
              finalState.copyAndRemoveAllEdgesFrom(
                  bufferAddress, sizeFillingStringBuffersInBits.orElseThrow());
        } else {
          finalState = finalState.copyAndRemoveAllEdgesFrom(bufferAddress);
        }

        // Handle overflow for strings
        if (formatArgumentIndex.isPresent() && stringFormatSpecifierUsed) {
          // TODO: if buffersize < sizeFillingStringBuffersInBits -> overflow
          // if (sizeFillingStringBuffersInBits.isPresent()) {
          finalState = finalState.withInvalidWrite(bufferAddress);
        }
      }
    }

    return ImmutableList.of(ValueAndSMGState.of(returnValue, finalState));
  }

  // TODO: output funs:
  //  fputwc, fputws, putwc, putwchar, fwprintf, wprintf, vfwprintf, and vwprintf, fputc, fputs,
  //  printf, vfprintf, vprintf, fprintf, puts, fwrite, putc, putchar

  // TODO: int ungetc(int c, FILE *stream), wint_t ungetwc(wint_t c, FILE *stream):
  //  Push a character back onto the stream. For success, returns c,
  //  else ungetc returns EOF; ungetwc returns WEOF.

  /**
   * Handles input functions like scanf() or sscanf() by overapproximating their behavior.
   * Parameters are checked for validity and 'char *' buffers cause a buffer-overflow.
   */
  private List<ValueAndSMGState> handleInputFunctions(
      SMGState pState,
      CFunctionCallExpression functionCallExpr,
      String functionName,
      CFAEdge pCfaEdge)
      throws CPATransferException {
    // TODO: implement format based checks
    // For most buffer overflows, we actually only want to allow overflows for formats searching
    //  for %s, which is a string. The others return their type only once, e.g. 1 number, 1 char.
    return switch (functionName) {
      // TODO: getwchar, fwscanf, wscanf, vfwscanf, and vwscanf, vscanf, vfscanf, fread,
      // TODO: getchar() family
      // TODO: getch() family

      // TODO: massive overapproximations used here currently! Improve!

      // int scanf(const char * format, ...);
      // '* const char' format: format to read from file.
      // ... : additional arguments, i.e. buffers used, may overflow for certain types.
      // Returns:
      //  >0: The number of values converted and assigned successfully.
      //  0: No value was assigned.
      //  <0: Read error encountered or end-of-file (EOF) reached before any assignment was made.
      case "scanf" ->
          checkParamValidityWithBufferOverflowsWithReturn(
              pState, pCfaEdge, functionCallExpr, functionName, false, false, Optional.of(0));

      // int fscanf(FILE * stream, const char * format, ...);
      // '* FILE' stream: pointer to file.
      // '* const char' format: format to read from file.
      // ... : additional arguments are used to save parsed data, aka the buffers (!overflow for
      // certain types!)
      // Alternatives: fscanf_s and fwscanf_s
      // Alternatives with locale as 3rd argument: _fscanf_s_l and _fwscanf_s_l
      case "fscanf", "fwscanf_s", "fscanf_s" ->
          checkParamValidityWithBufferOverflowsWithReturn(
              pState, pCfaEdge, functionCallExpr, functionName, false, false, Optional.of(1));

      // int sscanf(const char * stream, const char * format, ...);
      // As fscanf() above, but with string as first arg
      case "sscanf" ->
          checkParamValidityWithBufferOverflowsWithReturn(
              pState, pCfaEdge, functionCallExpr, functionName, false, false, Optional.of(1));

      // fgetwc(FILE *stream) and fgetc(FILE *stream) -> check input, return unknown
      // getwc(FILE *stream) and getc(FILE *stream) behave the same
      // int getchar(void); reads a char and returns it.
      case "fgetwc", "fgetc", "getwc", "getc", "getchar" ->
          checkAllParametersForValidityAndReturnUnknownValue(
              pState, pCfaEdge, functionCallExpr, functionName);

      // wchar_t *fgetws(wchar_t *buffer, int n, FILE *stream)
      // char * fgets(char * buffer, int n, FILE *stream);
      // modern version of gets() with max size buffer control (with n) etc.
      // read n-1 chars from stream or until end-of-file or \n is encountered.
      // Can't overflow it seems -> check validity of input, return buffer ptr and write n unknown
      //  into buffer (nth is \0).
      // If buffer or stream is 0, return 0.
      // TODO: add option that allows this to fail (return 0)
      // Buffer should be able to hold this number -> else overflow possible!
      case "fgets", "fgetws" ->
          checkParamValidityWithBufferOverflowsWithReturn(
              pState, pCfaEdge, functionCallExpr, functionName, true, true);

      // char * gets(char *buffer);  (deprecated in C11)
      // Returns either a valid address to the read string (i.e. buffer) on success, 0 otherwise.
      case "gets" ->
          checkParamValidityWithBufferOverflowsWithReturn(
              pState, pCfaEdge, functionCallExpr, functionName, true, false);

      default ->
          throw new SMGException(
              "C function "
                  + functionName
                  + " can currently not be handled with this CPA. Origin: "
                  + functionCallExpr);
    };
  }

  /**
   * @param cfaEdge for logging/debugging.
   * @param funCallExpr the {@link CFunctionCallExpression} that lead to this function call.
   * @param functionName The name of the function to be called.
   * @param state current {@link SMGState}.
   * @return a {@link List} of {@link ValueAndSMGState}s with either valid {@link Value}s and {@link
   *     SMGState}s, or unknown Values and maybe error states. Depending on the safety of the
   *     function/config.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  List<ValueAndSMGState> handleUnknownFunction(
      CFAEdge cfaEdge, CFunctionCallExpression funCallExpr, String functionName, SMGState state)
      throws CPATransferException {

    if (isSafeFunction(functionName)) {
      return handleSafeFunction(functionName, state, funCallExpr, cfaEdge);
    }

    // This mostly returns unknown if it does not find a function to handle
    if (functionName.contains("pthread")) {
      throw new SMGException("Concurrency analysis not supported in this configuration.");
    }

    return switch (options.getHandleUnknownFunctions()) {
      case STRICT ->
          throw new CPATransferException(
              String.format(
                  "Unknown function '%s' may be unsafe and STRICT handling is enabled. See option"
                      + " cpa.smg2.SMGCPABuiltins.handleUnknownFunction for more details",
                  funCallExpr));

      case ASSUME_SAFE -> {
        logger.log(
            Level.FINE,
            "Returned unknown value for assumed to be safe unknown function " + funCallExpr,
            cfaEdge);
        yield ImmutableList.of(ValueAndSMGState.ofUnknownValue(state));
      }

      case ASSUME_EXTERNAL_ALLOCATED -> {
        ImmutableList.Builder<ValueAndSMGState> builder = ImmutableList.builder();
        for (SMGState checkedState :
            checkAllParametersForValidity(state, cfaEdge, funCallExpr, functionName)) {
          logger.log(
              Level.FINE,
              "Returned unknown value with allocated memory for unknown function " + funCallExpr,
              cfaEdge);
          builder.addAll(
              evaluateExternalAllocationFunction(funCallExpr, checkedState, functionName));
        }
        yield builder.build();
      }
    };
  }

  /**
   * Gets the size of an allocation. This needs either 1 or 2 parameters. Those are read and
   * evaluated to the size for the allocation. Might throw an exception in case of an error.
   *
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @param functionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @return a {@link List} of {@link ValueAndSMGState}s with either valid numeric sizes (in bits)
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private List<ValueAndSMGState> getAllocateFunctionSize(
      SMGState pState, CFAEdge cfaEdge, CFunctionCallExpression functionCall)
      throws CPATransferException {

    String functionName = functionCall.getFunctionNameExpression().toASTString();

    if (options.getArrayAllocationFunctions().contains(functionName)) {
      // Arrays have 2 parameters
      if (functionCall.getParameterExpressions().size() != 2) {
        throw new UnrecognizedCodeException(
            functionName + " needs 2 arguments.", cfaEdge, functionCall);
      }
      ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
      for (ValueAndSMGState value1AndState :
          getAllocateFunctionParameter(
              options.getMemoryArrayAllocationFunctionsNumParameter(),
              functionCall,
              pState,
              cfaEdge)) {

        Value value1 = value1AndState.getValue();
        SMGState state1 = value1AndState.getState();

        if (!(value1 instanceof NumericValue)) {
          String infoMsg =
              "Could not determine a concrete value for the first argument of an memory allocation"
                  + " function: "
                  + functionCall.getFunctionNameExpression();
          if (options.isAbortOnNonConcreteMemorySize()) {
            infoMsg += ", due to option abortOnNonConcreteMemorySize. At " + cfaEdge;
            throw new SMGException(infoMsg);
          } else if (options.getHandleUnknownFunctions() == UnknownFunctionHandling.STRICT) {
            infoMsg += ", due to option UnknownFunctionHandling.STRICT. At " + cfaEdge;
            throw new SMGException(infoMsg);
          } else {
            logger.log(Level.FINE, infoMsg + ", in " + cfaEdge);
          }
          if (!options.trackPredicates()) {
            // Max overapproximation
            resultBuilder.add(ValueAndSMGState.ofUnknownValue(state1));
            continue;
          }
        }

        for (ValueAndSMGState value2AndState :
            getAllocateFunctionParameter(
                options.getMemoryArrayAllocationFunctionsElemSizeParameter(),
                functionCall,
                state1,
                cfaEdge)) {

          Value value2 = value2AndState.getValue();
          SMGState state2 = value2AndState.getState();
          if (!(value2 instanceof NumericValue)) {
            logger.log(
                Level.INFO,
                "Could not determine a concrete value for the second argument of an memory"
                    + " allocation function: "
                    + functionName
                    + ", in: "
                    + cfaEdge);
            if (!options.trackPredicates()) {
              resultBuilder.add(ValueAndSMGState.ofUnknownValue(state2));
              continue;
            }
          }

          // TODO: this might be wrong (the type might be incorrect)
          Value size = evaluator.multiplyBitOffsetValues(value1, value2);

          resultBuilder.add(ValueAndSMGState.of(size, state2));
        }
      }
      return resultBuilder.build();

    } else {
      // All other allocations need 1 argument
      if (functionCall.getParameterExpressions().size() != 1) {
        throw new UnrecognizedCodeException(
            functionName + " needs 1 arguments.", cfaEdge, functionCall);
      }
      return getAllocateFunctionParameter(
          options.getMemoryAllocationFunctionsSizeParameter(), functionCall, pState, cfaEdge);
    }
  }

  /**
   * Retuns a list of Values and the states for them, the values are the returned sizes for the
   * entered pParameterNumber. This also checks that the parameter exists and throws an exception if
   * not.
   *
   * @param pParameterNumber the paramter one wants to evaluate for later usage. Starting with 0.
   * @param functionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return {@link List} of {@link ValueAndSMGState}s each representing the paramteter requested.
   *     Each should be treated as a valid paramter.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private List<ValueAndSMGState> getAllocateFunctionParameter(
      int pParameterNumber, CFunctionCallExpression functionCall, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {

    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    for (ValueAndSMGState sizeValueAndState :
        getFunctionParameterValue(pParameterNumber, functionCall, pState, cfaEdge)) {
      SMGState currentState = sizeValueAndState.getState();
      Value value = sizeValueAndState.getValue();

      // If the value is unknown and some options are enabled we may assume values
      if (value.isUnknown()) {

        if (options.isGuessSizeOfUnknownMemorySize()) {
          Value forcedValue = new NumericValue(options.getGuessSize());
          resultBuilder.add(ValueAndSMGState.of(forcedValue, currentState));
        } else {
          throw new SMGException("Unknown value in allocation function parameter.");
        }

      } else {
        resultBuilder.add(sizeValueAndState);
      }
    }

    return resultBuilder.build();
  }

  /**
   * Gets parameter pParameterNumber and checks that it exists. If not it throws an exception. This
   * means this returns a Value and not a memory location! Always check the amount of parameters
   * before using this!
   *
   * @param pParameterNumber the number of the parameter for the function. I.e. foo (x); x would be
   *     parameter 0.
   * @param functionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param parameterTypeInFun {@link CType} of the parameter.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return {@link List} of {@link ValueAndSMGState}s each representing the parameter requested.
   *     Each should be treated as a valid parameter.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private List<ValueAndSMGState> getFunctionParameterValue(
      int pParameterNumber,
      CFunctionCallExpression functionCall,
      CType parameterTypeInFun,
      SMGState pState,
      CFAEdge cfaEdge)
      throws CPATransferException {

    CExpression paramExpr;
    String functionName = functionCall.getFunctionNameExpression().toASTString();
    try {
      paramExpr = functionCall.getParameterExpressions().get(pParameterNumber);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException(
          functionName + " argument #" + pParameterNumber + " not found.", cfaEdge, functionCall);
    }

    SMGCPAValueVisitor vv = new SMGCPAValueVisitor(evaluator, pState, cfaEdge, logger, options);
    return vv.evaluate(
        paramExpr, SMGCPAExpressionEvaluator.getCanonicalType(checkNotNull(parameterTypeInFun)));
  }

  /**
   * Gets parameter pParameterNumber and checks that it exists. If not it throws an exception. This
   * means this returns a Value and not a memory location! Always check the amount of parameters
   * before using this!
   *
   * @param pParameterNumber the number of the parameter for the function. I.e. foo (x); x would be
   *     parameter 0.
   * @param functionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return {@link List} of {@link ValueAndSMGState}s each representing the parameter requested.
   *     Each should be treated as a valid parameter.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private List<ValueAndSMGState> getFunctionParameterValue(
      int pParameterNumber, CFunctionCallExpression functionCall, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {

    CFunctionDeclaration decl = functionCall.getDeclaration();
    CType parameterTypeInFun;
    if (decl != null) {
      parameterTypeInFun = decl.getParameters().get(pParameterNumber).getType().getCanonicalType();
    } else {
      parameterTypeInFun =
          functionCall
              .getParameterExpressions()
              .get(pParameterNumber)
              .getExpressionType()
              .getCanonicalType();
    }
    checkNotNull(
        parameterTypeInFun,
        "Function call parameter type null. This can happen, the type needs to be added by hand!");
    return getFunctionParameterValue(
        pParameterNumber, functionCall, parameterTypeInFun, pState, cfaEdge);
  }

  /**
   * Handles all allocation methods that need a call to free() to clean up, e.g. malloc() and
   * calloc(). Returns the pointer Value to the new memory region (that may be written to 0 for the
   * correct function i.e. calloc). This also returns a state for the failure of the allocation
   * function if the option is enabled.
   *
   * @param functionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return {@link List} of {@link ValueAndSMGState}s holding valid and invalid returns (if
   *     enabled) for the called allocation functions. The {@link Value} will not be a {@link
   *     AddressExpression}, but may be numeric 0 (leading to the 0 SMGObject). Valid {@link Value}s
   *     pointers have always offset 0. Invalid calls may have an unknown {@link Value} as a return
   *     type in case of errors an error state is set.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  List<ValueAndSMGState> evaluateConfigurableAllocationFunctionWithManualMemoryCleanup(
      CFunctionCallExpression functionCall, String functionName, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();

    for (ValueAndSMGState sizeAndState : getAllocateFunctionSize(pState, cfaEdge, functionCall)) {

      Value sizeValue = sizeAndState.getValue();
      SMGState currentState = sizeAndState.getState();

      if (!(sizeValue instanceof NumericValue) && !options.trackPredicates()) {
        String infoMsg =
            "Could not determine a concrete size for a memory allocation function in line "
                + cfaEdge.getFileLocation().getStartingLineInOrigin()
                + ": "
                + functionCall.getFunctionNameExpression();
        if (options.isAbortOnNonConcreteMemorySize()) {
          infoMsg += ", due to option abortOnNonConcreteMemorySize. At " + cfaEdge;
          throw new SMGException(infoMsg);
        } else if (options.getHandleUnknownFunctions() == UnknownFunctionHandling.STRICT) {
          infoMsg += ", due to option UnknownFunctionHandling.STRICT. At " + cfaEdge;
          throw new SMGException(infoMsg);
        } else {
          logger.log(Level.INFO, infoMsg + ", in " + cfaEdge);
        }
        if (options.isGuessSizeOfUnknownMemorySize()) {
          sizeValue = new NumericValue(options.getGuessSize());
        } else if (options.isIgnoreUnknownMemoryAllocation()) {
          // Ignore and move on
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Ignored unknown sizes memory allocation due to option ignoreUnknownMemorySetting"
                      + " in .",
                  cfaEdge));
          continue;
        } else if (options.isErrorOnUnknownMemoryAllocation()) {
          // Error for CEGAR to learn the variable
          // TODO: this is bad! For truly unknown variables this also just plainly errors. Think of
          //  a better way
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState.withMemoryLeak(
                      "Plain memory error for malloc with symbolic size.", ImmutableList.of()),
                  "Returned unknown value due to unknown size of memory allocation in ",
                  cfaEdge));
          continue;
        } else {
          throw new AssertionError(
              "An allocation function ("
                  + functionName
                  + ") was called with a symbolic size. This is not supported"
                  + " currently by the SMG2 analysis. Try GuessSizeOfUnknownMemorySize.");
        }
      }
      // The size is always given in bytes, we want bit size
      CType sizeType = functionCall.getParameterExpressions().getFirst().getExpressionType();
      if (!(sizeValue instanceof NumericValue)) {
        sizeType =
            SMGCPAExpressionEvaluator.promoteMemorySizeTypeForBitCalculation(
                functionCall.getParameterExpressions().getFirst().getExpressionType(),
                machineModel);
      }
      Value sizeInBits = evaluator.multiplyBitOffsetValues(sizeValue, BigInteger.valueOf(8));

      resultBuilder.addAll(
          handleConfigurableMemoryAllocation(
              functionCall, currentState, sizeInBits, sizeType, cfaEdge));
    }

    return resultBuilder.build();
  }

  // malloc(size) w size in bits
  private ImmutableList<ValueAndSMGState> handleConfigurableMemoryAllocation(
      CFunctionCallExpression functionCall,
      SMGState pState,
      Value sizeInBits,
      CType sizeType,
      CFAEdge edge)
      throws SMGException, SMGSolverException {
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    String functionName = functionCall.getFunctionNameExpression().toASTString();

    if (sizeInBits instanceof NumericValue numSizeInBits) {
      BigInteger numericSizeInBits = numSizeInBits.bigIntegerValue();
      if (numericSizeInBits.compareTo(BigInteger.ZERO) == 0) {
        resultBuilder.add(handleAllocZero(pState));
        return resultBuilder.build();
      }

      // Create a new memory region with the specified size and use the pointer to its beginning
      // from now on
      ValueAndSMGState addressAndState = evaluator.createHeapMemoryAndPointer(pState, sizeInBits);
      Value addressToNewRegion = addressAndState.getValue();
      SMGState stateWithNewHeap = addressAndState.getState();

      if (options.getZeroingMemoryAllocation().contains(functionName)) {
        // Since this is newly created memory get(0) is fine
        stateWithNewHeap = stateWithNewHeap.writeToZero(addressToNewRegion, edge).getFirst();
      }
      resultBuilder.add(ValueAndSMGState.of(addressToNewRegion, stateWithNewHeap));
    } else {
      if (!options.trackPredicates()) {
        // Symbolic size
        throw new SMGException(
            functionCall
                + " tried to allocate symbolic memory, which is not supported without predicate"
                + " tracking.");
      } else if (options.getHandleUnknownFunctions() == UnknownFunctionHandling.STRICT) {
        throw new SMGException(
            functionCall
                + " tried to allocate symbolic memory, which is not supported for"
                + " UnknownFunctionHandling.STRICT");
      }
      // Symbolic size allowed
      // sizeInBits is a symbolic expr with a multiplication times 8 inside
      resultBuilder.addAll(
          handleSymbolicAllocation(sizeInBits, sizeType, pState, edge, functionName));
    }

    // If malloc can fail (and fails) it simply returns a pointer to 0 (C also sets errno)
    if (options.isEnableMallocFailure()) {
      // This mapping always exists
      Value addressToZero = new NumericValue(0);
      resultBuilder.add(ValueAndSMGState.of(addressToZero, pState));
    }
    return resultBuilder.build();
  }

  private Collection<ValueAndSMGState> handleSymbolicAllocation(
      Value sizeInBits, CType sizeType, SMGState pState, CFAEdge edge, String functionName)
      throws SMGSolverException, SMGException {
    // Symbolic size allowed
    // check that the size is not 0 (or may be zero)
    // If it can be zero, we split into 2 states, one with 0, one without
    // Symbolic Execution for assumption edges, use previous state and values
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    final ConstraintFactory constraintFactory =
        ConstraintFactory.getInstance(pState, machineModel, logger, options, evaluator, edge);
    SMGState maybeZeroState = pState;

    final Constraint sizeEqZeroConstraint =
        constraintFactory.getMemorySizeInBitsEqualsZeroConstraint(sizeInBits, sizeType, pState);

    String stackFrameFunctionName = pState.getStackFrameTopFunctionName();

    // Iff SAT -> size can be zero
    SatisfiabilityAndSMGState satisfiabilityAndStateEqZero =
        evaluator.checkIsUnsatAndAddConstraint(
            sizeEqZeroConstraint, stackFrameFunctionName, maybeZeroState);
    maybeZeroState = satisfiabilityAndStateEqZero.getState();

    if (satisfiabilityAndStateEqZero.isSAT()) {
      // Create a state with the memory size == 0
      resultBuilder.add(handleAllocZero(maybeZeroState));
    }
    SMGState stateWithNewNonZeroHeap = pState;
    final Constraint sizeNotEqZeroConstraint =
        constraintFactory.getNotEqualsZeroConstraint(sizeInBits, sizeType, pState);

    // If SAT -> size can be non zero
    SatisfiabilityAndSMGState satisfiabilityAndStateNotEqZero =
        evaluator.checkIsUnsatAndAddConstraint(
            sizeNotEqZeroConstraint, stackFrameFunctionName, stateWithNewNonZeroHeap);
    stateWithNewNonZeroHeap = satisfiabilityAndStateNotEqZero.getState();

    Value addressToNewRegion;
    if (satisfiabilityAndStateNotEqZero.isSAT()) {
      // Create a state with the memory size
      ValueAndSMGState addressAndState =
          evaluator.createHeapMemoryAndPointer(stateWithNewNonZeroHeap, sizeInBits);
      addressToNewRegion = addressAndState.getValue();
      stateWithNewNonZeroHeap = addressAndState.getState();

      if (options.getZeroingMemoryAllocation().contains(functionName)) {
        // Need symbolic edges for that
        // throw new SMGException(
        //  "Zeroing allocation function with symbolic memory size is currently not supported.");

        List<SMGState> zeroedStates = stateWithNewNonZeroHeap.writeToZero(addressToNewRegion, edge);
        checkState(zeroedStates.size() == 1);
        stateWithNewNonZeroHeap = zeroedStates.getFirst();
      }

      resultBuilder.add(ValueAndSMGState.of(addressToNewRegion, stateWithNewNonZeroHeap));
    }

    return resultBuilder.build();
  }

  private ValueAndSMGState handleAllocZero(SMGState currentState) {
    // C99 says that allocation functions with argument 0 can return a null-pointer (or a valid
    // pointer that may not be dereferenced but can be freed)
    // This mapping always exists
    // SV-Comp expects a non-zero return pointer!
    if (options.isMallocZeroReturnsZero()) {
      Value addressToZero = new NumericValue(0);
      return ValueAndSMGState.of(addressToZero, currentState);
    } else {
      // Some size, does not matter
      return evaluator.createMallocZeroMemoryAndPointer(
          currentState, new NumericValue(BigInteger.ONE));
    }
  }

  /**
   * Checks for known safe functions and returns true if the entered function is one. Functions that
   * are always considered as safe are not evaluated, even if known to the analysis, nor are their
   * inputs checked for validity. They always return a new, unknown value and therefore
   * overapproximate if their signature does not return void.
   *
   * @param calledFunctionName name of the called function to be checked.
   * @return true is the function is safe, false else.
   */
  private boolean isSafeFunction(String calledFunctionName) {
    return options.getSafeUnknownFunctions().contains(calledFunctionName);
  }

  private List<ValueAndSMGState> handleSafeFunction(
      String calledFunctionName,
      SMGState pState,
      CFunctionCallExpression cFCExpression,
      CFAEdge pCfaEdge) {
    logger.log(
        Level.FINE,
        "Returned unknown value for C function "
            + calledFunctionName
            + " flagged as safe, located at "
            + cFCExpression,
        pCfaEdge);
    return ImmutableList.of(ValueAndSMGState.ofUnknownValue(pState));
  }

  /**
   * Prints the SMG in relation to the current function call.
   *
   * @param functionCall current function call __VERIFIER_BUILTIN_PLOT
   * @param currentState current {@link SMGState}.
   */
  @SuppressWarnings("unused")
  private void evaluateVBPlot(CFunctionCallExpression functionCall, SMGState currentState) {
    // String name = functionCall.getParameterExpressions().get(0).toASTString();
    if (exportSMGOptions.hasExportPath()) {
      // TODO:
      /*
      SMGUtils.dumpSMGPlot(
          logger,
          currentState,
          functionCall.toASTString(),
          exportSMGOptions.getOutputFilePath(name));
      */
    }
  }

  /**
   * Evaluate function: void * memset( void * buffer, int ch, size_t count );
   *
   * @param functionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return A {@link List} of {@link ValueAndSMGState}s with the results of the memset always in
   *     the state, this includes error states in case of failures. The Value is either unknown or
   *     the target pointer (no {@link AddressExpression}!).
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private List<ValueAndSMGState> evaluateMemset(
      CFunctionCallExpression functionCall, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {
    /*
     * void *memset(void *str, int c, size_t n) copies the character c (an unsigned char)
     * to the first n characters of the string pointed to, by the argument str.
     * This has a return value equalling the buffer!
     */

    if (functionCall.getParameterExpressions().size() != 3) {
      throw new UnrecognizedCodeException(
          functionCall.getFunctionNameExpression().toASTString() + " needs 3 arguments.",
          cfaEdge,
          functionCall);
    }

    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    // First arg
    for (ValueAndSMGState bufferAddressAndState :
        getFunctionParameterValue(MEMSET_BUFFER_PARAMETER, functionCall, pState, cfaEdge)) {
      Value bufferValue = bufferAddressAndState.getValue();
      SMGState currentState = bufferAddressAndState.getState();
      // If the Value is no AddressExpression we can't work with it
      // The buffer is type * and has to be an AddressExpression with a not unknown value and a
      // concrete offset to be used correctly
      if (!(bufferValue instanceof AddressExpression addressExpression)
          || addressExpression.getMemoryAddress().isUnknown()
          || !(addressExpression.getOffset() instanceof NumericValue)) {
        currentState = currentState.withInvalidWrite(bufferValue);
        resultBuilder.add(
            ValueAndSMGState.ofUnknownValue(
                currentState,
                "Returned unknown because of unknown target address or offset in first argument in"
                    + " function memset in",
                cfaEdge));
        continue;
      }

      // Second arg
      for (ValueAndSMGState charValueAndSMGState :
          getFunctionParameterValue(MEMSET_CHAR_PARAMETER, functionCall, currentState, cfaEdge)) {
        // Third arg
        for (ValueAndSMGState countAndState :
            getFunctionParameterValue(
                MEMSET_COUNT_PARAMETER, functionCall, charValueAndSMGState.getState(), cfaEdge)) {

          resultBuilder.add(
              evaluateMemset(
                  countAndState.getState(),
                  cfaEdge,
                  addressExpression,
                  charValueAndSMGState.getValue(),
                  countAndState.getValue()));
        }
      }
    }

    return resultBuilder.build();
  }

  /**
   * Checks the Values such that they are useable, returns an unknown Value with error state (may be
   * non critical) if it's not useable. Else it writes the char entered (int value) into the region
   * behind the given address count times. Make sure that the bufferValue is already checked and is
   * a valid AddressExpression!
   *
   * @param currentState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @param bufferAddressAndOffset {@link AddressExpression} holding the pointer and offset leading
   *     to the buffer to be written to.
   * @param charValue {@link Value} representing the character to be inserted.
   * @param countValue {@link Value} for the number of chars inserted.
   * @return {@link ValueAndSMGState} with either the address to the buffer (not a {@link
   *     AddressExpression}) and the written {@link SMGState}, or an unknown value and maybe an
   *     error state.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private ValueAndSMGState evaluateMemset(
      SMGState currentState,
      @SuppressWarnings("unused") CFAEdge cfaEdge,
      AddressExpression bufferAddressAndOffset,
      Value charValue,
      Value countValue)
      throws CPATransferException {
    // TODO: use cfaEdge for errors

    if (countValue.isUnknown()) {
      currentState =
          currentState.withInvalidWrite(
              "Invalid (Unknown) size (third argument) for memset() function call.", countValue);
      // TODO: we need to change the value behind bufferAddress to unknown as well!
      return ValueAndSMGState.ofUnknownValue(
          currentState,
          "Returned unknown because of unknown number of repetitions in third argument in function"
              + " memset in",
          cfaEdge);
    }
    if (!(countValue instanceof NumericValue numCountValue)) {
      currentState =
          currentState.withInvalidWrite(
              "Symbolic count (second argument) for memset() function call not supported.",
              countValue);
      // TODO: we need to change the value behind bufferAddress to unknown as well!
      return ValueAndSMGState.ofUnknownValue(
          currentState,
          "Returned unknown because of unknown number of repetitions in third argument in function"
              + " memset in",
          cfaEdge);
    }

    long count = numCountValue.longValue();

    // If the char value is unknown, we use a new symbolic value!
    Value bufferMemoryAddress = bufferAddressAndOffset.getMemoryAddress();
    if (!(bufferAddressAndOffset.getOffset() instanceof NumericValue bufferOffset)) {
      throw new SMGException("Can not handle non-numeric buffer offset in memset() currently");
    }
    BigInteger bufferOffsetInBits = bufferOffset.bigIntegerValue();

    BigInteger sizeOfCharInBits = BigInteger.valueOf(machineModel.getSizeofCharInBits());

    // This precondition has to hold for the get(0) getters
    Preconditions.checkArgument(
        !currentState.getMemoryModel().pointsToZeroPlus(bufferMemoryAddress));
    if (isNumericZero(charValue)) {
      // Create one large edge for 0 (the SMG cuts 0 edges on its own)
      currentState =
          currentState
              .writeValueTo(
                  bufferMemoryAddress,
                  bufferOffsetInBits,
                  new NumericValue(sizeOfCharInBits.multiply(BigInteger.valueOf(count))),
                  charValue,
                  CNumericTypes.CHAR,
                  cfaEdge)
              .getFirst();
    } else {
      // Write each char on its own
      for (int c = 0; c < count; c++) {
        currentState =
            currentState
                .writeValueTo(
                    bufferMemoryAddress,
                    bufferOffsetInBits.add(BigInteger.valueOf(c).multiply(sizeOfCharInBits)),
                    new NumericValue(sizeOfCharInBits),
                    charValue,
                    CNumericTypes.CHAR,
                    cfaEdge)
                .getFirst();
      }
    }
    // Since this returns the pointer of the buffer we check the offset of the AddressExpression, if
    // its 0 we can return the known pointer, else we create a new one.
    if (bufferOffsetInBits.equals(BigInteger.ZERO)) {
      return ValueAndSMGState.of(bufferMemoryAddress, currentState);
    } else {
      ValueAndSMGState newPointerAndState =
          evaluator
              .findOrcreateNewPointer(bufferMemoryAddress, bufferOffsetInBits, currentState)
              .getFirst();
      return ValueAndSMGState.of(newPointerAndState.getValue(), newPointerAndState.getState());
    }
  }

  private List<ValueAndSMGState> evaluateExternalAllocationFunction(
      CFunctionCallExpression pFunctionCall, SMGState pState, String calledFunctionName) {

    if (!(pFunctionCall.getExpressionType() instanceof CPointerType)
        || !isExternalAllocationFunction(calledFunctionName)) {
      // Non-allocating call, return unknown value.
      return ImmutableList.of(ValueAndSMGState.ofUnknownValue(pState));
    }
    // Some methods like fgets return pointers that were parameters of the function, we need to
    //  model those functions fully, so that they don't end up here!

    String functionName = pFunctionCall.getFunctionNameExpression().toASTString();
    Value allocationSize =
        new NumericValue(BigInteger.valueOf(options.getExternalAllocationSize()));

    // TODO line numbers are not unique when we have multiple input files!
    String extAllocationLabel =
        "_EXTERNAL_ALLOC_"
            + functionName
            + "_ID"
            + U_ID_GENERATOR.getFreshId()
            + "_Line:"
            + pFunctionCall.getFileLocation().getStartingLineNumber();

    // TODO: allocation is easy, but not all cases of handling external memory are implemented
    //  currently.
    // TODO: can this fail?
    return ImmutableList.of(
        evaluator.createExternalHeapMemoryAndPointer(pState, allocationSize, extAllocationLabel));
  }

  /**
   * The method "alloca" (or "__builtin_alloca") allocates memory from the stack. The memory is
   * automatically freed at function-exit. This is not default C, Linux uses the GNU version so we
   * stick to that. If the allocation causes stack overflow, program behavior is undefined.
   *
   * @param functionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return A {@link List} of {@link ValueAndSMGState}s with the results of the alloca call on the
   *     stack of the returned {@link SMGState}s or an error info set in case of errors. The Value
   *     is either a pointer to the valid stack memory allocated or unknown. The pointer is not a
   *     {@link AddressExpression}!
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private List<ValueAndSMGState> evaluateAlloca(
      CFunctionCallExpression functionCall, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {
    // TODO possible property violation "stack-overflow through big allocation" is not handled

    if (functionCall.getParameterExpressions().size() != 1) {
      throw new UnrecognizedCodeException(
          functionCall.getFunctionNameExpression().toASTString() + " needs 1 argument.",
          cfaEdge,
          functionCall);
    }

    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    // reuse MALLOC_PARAMETER since its just the first argument (and there is always just 1)
    for (ValueAndSMGState argumentAndState :
        getAllocateFunctionParameter(MALLOC_PARAMETER, functionCall, pState, cfaEdge)) {

      if (!(argumentAndState.getValue() instanceof NumericValue) && !options.trackPredicates()) {
        String infoMsg =
            "Could not determine a concrete size for a memory allocation function: "
                + functionCall.getFunctionNameExpression();
        if (options.isAbortOnNonConcreteMemorySize()) {
          infoMsg += ", due to option abortOnNonConcreteMemorySize. At " + cfaEdge;
          throw new SMGException(infoMsg);
        } else if (options.getHandleUnknownFunctions() == UnknownFunctionHandling.STRICT) {
          infoMsg += ", due to option UnknownFunctionHandling.STRICT. At " + cfaEdge;
          throw new SMGException(infoMsg);
        } else {
          logger.log(Level.INFO, infoMsg + ", in " + cfaEdge);
        }
      }

      resultBuilder.addAll(
          evaluateAlloca(
              argumentAndState.getState(),
              argumentAndState.getValue(),
              SMGCPAExpressionEvaluator.getCanonicalType(functionCall.getExpressionType()),
              cfaEdge));
    }

    return resultBuilder.build();
  }

  /**
   * Actual single alloca(). Declare memory region on the stack and return the pointer to its
   * beginning. This might guess the size if it is unknown based on the malloc guess size.
   *
   * @param pState current {@link SMGState}.
   * @param pSizeValue the {@link Value} holding the size of the allocation of stack memory in bits.
   * @param cfaEdge for logging/debugging.
   * @return a {@link List} of {@link ValueAndSMGState}s with the address {@link Value} (NO {@link
   *     AddressExpression}!) to the new memory on the stack. May be unknown in case of a problem
   *     and may have an error state with the error.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private List<ValueAndSMGState> evaluateAlloca(
      SMGState pState, Value pSizeValue, CType type, @SuppressWarnings("unused") CFAEdge cfaEdge)
      throws CPATransferException {
    Value sizeInBits = evaluator.multiplyBitOffsetValues(pSizeValue, BigInteger.valueOf(8));

    String allocationLabel = "_ALLOCA_ID_" + U_ID_GENERATOR.getFreshId();
    ValueAndSMGState addressValueAndState =
        evaluator.createStackAllocation(allocationLabel, sizeInBits, type, pState);

    return ImmutableList.of(
        ValueAndSMGState.of(addressValueAndState.getValue(), addressValueAndState.getState()));
  }

  /**
   * Evaluate the use of the C Free() method. Should recieve 1 argument that should be an address to
   * memory not yet freed (still valid). If the Value is no address, an already freed address or any
   * other form of invalid address (i.e. = 0) the return state will have an error info attached.
   *
   * @param pFunctionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return a list of {@link SMGState}s for the results of the free() invokation. These might
   *     include error states! The {@link Value}s returned are UNKNOWN. As the function does not
   *     return a value, this is not a problem, and they are ignored.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private ImmutableList<ValueAndSMGState> evaluateFree(
      CFunctionCallExpression pFunctionCall, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {

    if (pFunctionCall.getParameterExpressions().size() != 1) {
      throw new UnrecognizedCodeException(
          "The function free() needs exactly 1 paramter", cfaEdge, pFunctionCall);
    }

    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    for (ValueAndSMGState addressAndState :
        getFunctionParameterValue(0, pFunctionCall, pState, cfaEdge)) {
      Value maybeAddressValue = addressAndState.getValue();
      SMGState currentState = addressAndState.getState();

      if (currentState.hasMemoryErrors() && options.isMemoryErrorTarget()) {
        resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
      } else {
        resultBuilder.addAll(
            transformedImmutableListCopy(
                currentState.free(maybeAddressValue, pFunctionCall, cfaEdge),
                ValueAndSMGState::ofUnknownValue));
      }
    }

    return resultBuilder.build();
  }

  /**
   * @param functionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return a list of {@link ValueAndSMGState} with either the targetAddress pointer expression and
   *     the state in which the copy was successfull, or an unknown value and maybe an error state
   *     if something went wrong, i.e. invalid/read/write.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private List<ValueAndSMGState> evaluateMemcpy(
      CFunctionCallExpression functionCall, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {
    // TODO: partial edges are not copied, but we could!

    // evaluate function: void *memcpy(void *str1, const void *str2, size_t n)

    if (functionCall.getParameterExpressions().size() != 3) {
      throw new UnrecognizedCodeException(
          functionCall.getFunctionNameExpression().toASTString() + " needs 3 arguments.",
          cfaEdge,
          functionCall);
    }

    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();

    CExpression targetExpr = functionCall.getParameterExpressions().get(MEMCPY_TARGET_PARAMETER);

    while (targetExpr instanceof CCastExpression sourceCastExpr) {
      targetExpr = sourceCastExpr.getOperand();
    }

    if ((targetExpr instanceof CUnaryExpression unary
            && unary.getOperator().equals(UnaryOperator.AMPER))
        || SMGCPAExpressionEvaluator.getCanonicalType(targetExpr.getExpressionType())
            instanceof CPointerType) {
      // Address visitor will fail on this one
      // Retrieve via value visitor
      for (ValueAndSMGState targetAndState :
          getFunctionParameterValue(MEMCPY_TARGET_PARAMETER, functionCall, pState, cfaEdge)) {
        SMGState currentState = targetAndState.getState();

        Value targetAddress = targetAndState.getValue();
        if (!SMGCPAExpressionEvaluator.valueIsAddressExprOrVariableOffset(targetAddress)) {
          // Unknown addresses happen only of we don't have a memory associated
          // Write the target region to unknown depending on the size
          // TODO:
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown because of unknown target or offset of address of target"
                      + " argument in function memcpy in",
                  cfaEdge));
          continue;
        } else if (!(targetAddress instanceof AddressExpression)) {
          // The value can be unknown
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown because of unknown target or offset of address of target"
                      + " argument in function memcpy in",
                  cfaEdge));
          continue;
        }
        AddressExpression targetAddressExpr = (AddressExpression) targetAddress;
        if (!(targetAddressExpr.getOffset() instanceof NumericValue numTargetAddressOffset)) {
          // Write the target region to unknown
          // TODO:
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown because of unknown offset of target argument in function memcpy"
                      + " in",
                  cfaEdge));
          continue;
        }

        List<ValueAndSMGState> newTargetPointerAndStates =
            evaluator.findOrcreateNewPointer(
                targetAddressExpr.getMemoryAddress(),
                numTargetAddressOffset.bigIntegerValue(),
                currentState);
        for (ValueAndSMGState newTargetPointerAndState : newTargetPointerAndStates) {
          for (SMGStateAndOptionalSMGObjectAndOffset newTargetObjAndState :
              newTargetPointerAndState
                  .getState()
                  .dereferencePointer(newTargetPointerAndState.getValue())) {
            if (!newTargetObjAndState.hasSMGObjectAndOffset()
                || !(newTargetObjAndState.getOffsetForObject()
                    instanceof NumericValue numNewTargetObjOffset)) {
              continue;
            }

            evaluateMemcpySecondStep(
                newTargetObjAndState.getSMGObject(),
                numNewTargetObjOffset.bigIntegerValue(),
                newTargetObjAndState.getSMGState(),
                functionCall,
                cfaEdge,
                resultBuilder);
          }
        }
      }

    } else {

      // TODO: how to handle errors in the parameters here? Pull it out, catch, rethrow with
      // concrete
      // error info? (= no valid pointer/memory region found)
      for (SMGStateAndOptionalSMGObjectAndOffset destAndState :
          functionCall
              .getParameterExpressions()
              .get(MEMCPY_TARGET_PARAMETER)
              .accept(new SMGCPAAddressVisitor(evaluator, pState, cfaEdge, logger, options))) {

        SMGState currentState = destAndState.getSMGState();

        if (!destAndState.hasSMGObjectAndOffset()) {
          // Unknown addresses happen only of we don't have a memory associated
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown because of unknown target or offset of address of target"
                      + " argument in function memcpy in",
                  cfaEdge));
          continue;
        }
        SMGObject targetObj = destAndState.getSMGObject();
        Value targetOffset = destAndState.getOffsetForObject();

        if (!(targetOffset instanceof NumericValue numTargetOffset)) {
          // Write the target region to unknown
          // TODO: Write the target region to unknown
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown because of unknown offset of target argument in function memcpy"
                      + " in",
                  cfaEdge));
          continue;
        }
        evaluateMemcpySecondStep(
            targetObj,
            numTargetOffset.bigIntegerValue(),
            currentState,
            functionCall,
            cfaEdge,
            resultBuilder);
      }
    }
    return resultBuilder.build();
  }

  // Evals second and third parameters of memcpy with given first (target)
  private void evaluateMemcpySecondStep(
      SMGObject targetObj,
      BigInteger targetOffset,
      SMGState pCurrentState,
      CFunctionCallExpression functionCall,
      CFAEdge pCFAEdge,
      ImmutableList.Builder<ValueAndSMGState> resultBuilder)
      throws CPATransferException {

    CExpression sourceExpr = functionCall.getParameterExpressions().get(MEMCPY_SOURCE_PARAMETER);

    while (sourceExpr instanceof CCastExpression sourceCastExpr) {
      sourceExpr = sourceCastExpr.getOperand();
    }

    if ((sourceExpr instanceof CUnaryExpression unary
            && unary.getOperator().equals(UnaryOperator.AMPER))
        || SMGCPAExpressionEvaluator.getCanonicalType(sourceExpr.getExpressionType())
            instanceof CPointerType) {
      // Address visitor will fail on this one
      // Retrieve via value visitor
      for (ValueAndSMGState sourceAndState :
          getFunctionParameterValue(
              MEMCPY_SOURCE_PARAMETER, functionCall, pCurrentState, pCFAEdge)) {
        SMGState currentState = sourceAndState.getState();

        Value sourceAddress = sourceAndState.getValue();
        if (!SMGCPAExpressionEvaluator.valueIsAddressExprOrVariableOffset(sourceAddress)) {
          // Unknown addresses happen only of we don't have a memory associated
          // Write the target region to unknown depending on the size
          // TODO:
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown because of unknown target or offset of source argument in"
                      + " function memcpy in",
                  pCFAEdge));
          continue;
        } else if (!(sourceAddress instanceof AddressExpression)) {
          // The value can be unknown
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown because of unknown target or offset of source argument in"
                      + " function memcpy in",
                  pCFAEdge));
          continue;
        }
        AddressExpression sourceAddressExpr = (AddressExpression) sourceAddress;
        if (!(sourceAddressExpr.getOffset() instanceof NumericValue numSourceAddressExprOffset)) {
          // Write the target region to unknown
          // TODO:
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown because of unknown offset in source argument in function memcpy"
                      + " in",
                  pCFAEdge));
          continue;
        }

        List<ValueAndSMGState> newSourcePointerAndStates =
            evaluator.findOrcreateNewPointer(
                sourceAddressExpr.getMemoryAddress(),
                numSourceAddressExprOffset.bigIntegerValue(),
                currentState);
        for (ValueAndSMGState newSourcePointerAndState : newSourcePointerAndStates) {
          for (SMGStateAndOptionalSMGObjectAndOffset newSourceObjAndState :
              newSourcePointerAndState
                  .getState()
                  .dereferencePointer(newSourcePointerAndState.getValue())) {
            if (!newSourceObjAndState.hasSMGObjectAndOffset()
                || !(newSourceObjAndState.getOffsetForObject()
                    instanceof NumericValue numNewSourceObjOffset)) {
              continue;
            }
            evaluateMemcpyLastStep(
                targetObj,
                targetOffset,
                newSourceObjAndState.getSMGObject(),
                numNewSourceObjOffset.bigIntegerValue(),
                newSourceObjAndState.getSMGState(),
                functionCall,
                pCFAEdge,
                resultBuilder);
          }
        }
      }

    } else {

      for (SMGStateAndOptionalSMGObjectAndOffset sourceAndState :
          functionCall
              .getParameterExpressions()
              .get(MEMCPY_SOURCE_PARAMETER)
              .accept(
                  new SMGCPAAddressVisitor(evaluator, pCurrentState, pCFAEdge, logger, options))) {

        SMGState currentState = sourceAndState.getSMGState();
        if (!sourceAndState.hasSMGObjectAndOffset()) {
          // Unknown addresses happen only of we don't have a memory associated
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown because of unknown target or offset for source argument in"
                      + " function memcpy in",
                  pCFAEdge));
          continue;
        }
        SMGObject sourceObj = sourceAndState.getSMGObject();
        Value sourceOffset = sourceAndState.getOffsetForObject();

        if (!(sourceOffset instanceof NumericValue numSourceOffset)) {
          // Unknown offset
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState,
                  "Returned unknown because of unknown source offset in function memcpy in",
                  pCFAEdge));
          continue;
        }

        evaluateMemcpyLastStep(
            targetObj,
            targetOffset,
            sourceObj,
            numSourceOffset.bigIntegerValue(),
            currentState,
            functionCall,
            pCFAEdge,
            resultBuilder);
      }
    }
  }

  // Evals the 3rd parameter to memcpy with the first 2 given as source and target
  private void evaluateMemcpyLastStep(
      SMGObject targetObj,
      BigInteger targetOffset,
      SMGObject sourceObj,
      BigInteger sourceOffset,
      SMGState pCurrentState,
      CFunctionCallExpression functionCall,
      CFAEdge pCFAEdge,
      ImmutableList.Builder<ValueAndSMGState> resultBuilder)
      throws CPATransferException {
    for (ValueAndSMGState sizeAndState :
        getFunctionParameterValue(MEMCPY_SIZE_PARAMETER, functionCall, pCurrentState, pCFAEdge)) {

      SMGState currentState = sizeAndState.getState();
      Value sizeValue = sizeAndState.getValue();

      if (!(sizeValue instanceof NumericValue numSizeValue)) {
        // TODO: log instead of error? This is a limitation of the analysis that is not a
        // critical C problem.
        resultBuilder.add(
            ValueAndSMGState.ofUnknownValue(
                currentState,
                "Returned unknown because of unknown size of copy in function memcpy in",
                pCFAEdge));
        continue;
      }

      resultBuilder.add(
          evaluateMemcpy(
              currentState, targetObj, targetOffset, sourceObj, sourceOffset, numSizeValue));
    }
  }

  /**
   * int memcmp(const void *cmp1, const void *cmp2, size_t n) compares n bytes of cmp1 and cmp2 and
   * returns an int based on the comparison. The comparison is done lexicographically. Undef
   * behavior if either one pointer is 0 or we read beyond the sizes of either.
   *
   * @param functionCall the function call of the memcmp.
   * @param pState current SMGState.
   * @param cfaEdge current CFAEdge for debugging and error info.
   * @return 1 or 0 depending on the equality of the bytes compared
   * @throws CPATransferException in case of unhandled cases.
   */
  private List<ValueAndSMGState> evaluateMemcmp(
      CFunctionCallExpression functionCall, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {

    // TODO: add overapproximation for value analysis based on option.
    if (functionCall.getParameterExpressions().size() != 3) {
      throw new UnrecognizedCodeException(
          functionCall.getFunctionNameExpression().toASTString() + " needs 3 arguments.",
          cfaEdge,
          functionCall);
    }

    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();

    CExpression targetExpr =
        functionCall.getParameterExpressions().get(MEMCMP_CMP_TARGET1_PARAMETER);

    while (targetExpr instanceof CCastExpression sourceCastExpr) {
      targetExpr = sourceCastExpr.getOperand();
    }

    // First arg is &(something). E.g. address of an integer.
    if ((targetExpr instanceof CUnaryExpression unary
            && unary.getOperator().equals(UnaryOperator.AMPER))
        || SMGCPAExpressionEvaluator.getCanonicalType(targetExpr.getExpressionType())
            instanceof CPointerType) {
      // Get the address.
      // Address visitor will fail on this one
      // Retrieve via value visitor
      for (ValueAndSMGState targetAndState :
          getFunctionParameterValue(MEMCMP_CMP_TARGET1_PARAMETER, functionCall, pState, cfaEdge)) {
        SMGState currentState = targetAndState.getState();

        Value targetAddress = targetAndState.getValue();
        if (!SMGCPAExpressionEvaluator.valueIsAddressExprOrVariableOffset(targetAddress)) {
          // Unknown addresses happen only of we don't have a memory associated
          throw new SMGException(
              "Error when evaluating the function " + functionCall + " in " + cfaEdge);
          // resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          // continue;
        } else if (!(targetAddress instanceof AddressExpression)) {
          // The value can be unknown
          throw new SMGException(
              "Error when evaluating the function " + functionCall + " in " + cfaEdge);
          // resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          // continue;
        }
        AddressExpression targetAddressExpr = (AddressExpression) targetAddress;
        if (!(targetAddressExpr.getOffset() instanceof NumericValue targetAddressExprOffset)) {
          throw new SMGException(
              "Error when evaluating the function " + functionCall + " in " + cfaEdge);
          // resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          // continue;
        }

        List<ValueAndSMGState> newTargetPointerAndStates =
            evaluator.findOrcreateNewPointer(
                targetAddressExpr.getMemoryAddress(),
                targetAddressExprOffset.bigIntegerValue(),
                currentState);
        for (ValueAndSMGState newTargetPointerAndState : newTargetPointerAndStates) {
          for (SMGStateAndOptionalSMGObjectAndOffset newTargetObjAndState :
              newTargetPointerAndState
                  .getState()
                  .dereferencePointer(newTargetPointerAndState.getValue())) {
            if (!newTargetObjAndState.hasSMGObjectAndOffset()
                || !(newTargetObjAndState.getOffsetForObject()
                    instanceof NumericValue newTargetObjOffset)) {
              throw new SMGException(
                  "Error when evaluating the function " + functionCall + " in " + cfaEdge);
              // continue;
            }

            evaluateMemcmpSecondStep(
                newTargetObjAndState.getSMGObject(),
                newTargetObjOffset.bigIntegerValue(),
                newTargetObjAndState.getSMGState(),
                functionCall,
                cfaEdge,
                resultBuilder);
          }
        }
      }

    } else {

      // A pointer was entered as parameter, get the object.
      // TODO: how to handle errors in the parameters here? Pull it out, catch, rethrow with
      //  concrete
      // error info? (= no valid pointer/memory region found)
      for (SMGStateAndOptionalSMGObjectAndOffset destAndState :
          functionCall
              .getParameterExpressions()
              .get(MEMCMP_CMP_TARGET1_PARAMETER)
              .accept(new SMGCPAAddressVisitor(evaluator, pState, cfaEdge, logger, options))) {

        SMGState currentState = destAndState.getSMGState();

        if (!destAndState.hasSMGObjectAndOffset()) {
          // Unknown addresses happen only of we don't have a memory associated
          throw new SMGException(
              "Error when evaluating the function " + functionCall + " in " + cfaEdge);
          // resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          // continue;
        }
        SMGObject targetObj = destAndState.getSMGObject();
        Value targetOffset = destAndState.getOffsetForObject();

        if (!(targetOffset instanceof NumericValue numTargetOffset)) {
          throw new SMGException(
              "Error when evaluating the function " + functionCall + " in " + cfaEdge);
          // resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          // continue;
        }
        evaluateMemcmpSecondStep(
            targetObj,
            numTargetOffset.bigIntegerValue(),
            currentState,
            functionCall,
            cfaEdge,
            resultBuilder);
      }
    }
    return resultBuilder.build();
  }

  // Evals second and third parameters of memcmp with given first (target)
  private void evaluateMemcmpSecondStep(
      SMGObject targetObj1,
      BigInteger targetOffset1,
      SMGState pCurrentState,
      CFunctionCallExpression functionCall,
      CFAEdge pCFAEdge,
      ImmutableList.Builder<ValueAndSMGState> resultBuilder)
      throws CPATransferException {

    CExpression sourceExpr =
        functionCall.getParameterExpressions().get(MEMCMP_CMP_TARGET2_PARAMETER);

    while (sourceExpr instanceof CCastExpression sourceCastExpr) {
      sourceExpr = sourceCastExpr.getOperand();
    }

    if ((sourceExpr instanceof CUnaryExpression unary
            && unary.getOperator().equals(UnaryOperator.AMPER))
        || SMGCPAExpressionEvaluator.getCanonicalType(sourceExpr.getExpressionType())
            instanceof CPointerType) {
      // Address visitor will fail on this one
      // Retrieve via value visitor
      for (ValueAndSMGState sourceAndState :
          getFunctionParameterValue(
              MEMCMP_CMP_TARGET2_PARAMETER, functionCall, pCurrentState, pCFAEdge)) {
        SMGState currentState = sourceAndState.getState();

        Value sourceAddress = sourceAndState.getValue();
        if (!SMGCPAExpressionEvaluator.valueIsAddressExprOrVariableOffset(sourceAddress)) {
          throw new SMGException(
              "Error when evaluating the function " + functionCall + " in " + pCFAEdge);
          // resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          // continue;
        } else if (!(sourceAddress instanceof AddressExpression)) {
          throw new SMGException(
              "Error when evaluating the function " + functionCall + " in " + pCFAEdge);
          // resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          // continue;
        }
        AddressExpression sourceAddressExpr = (AddressExpression) sourceAddress;
        if (!(sourceAddressExpr.getOffset() instanceof NumericValue sourceAddressExprOffset)) {
          throw new SMGException(
              "Error when evaluating the function " + functionCall + " in " + pCFAEdge);
          // resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          // continue;
        }

        List<ValueAndSMGState> newSourcePointerAndStates =
            evaluator.findOrcreateNewPointer(
                sourceAddressExpr.getMemoryAddress(),
                sourceAddressExprOffset.bigIntegerValue(),
                currentState);
        for (ValueAndSMGState newSourcePointerAndState : newSourcePointerAndStates) {
          for (SMGStateAndOptionalSMGObjectAndOffset newTargetObj2AndState :
              newSourcePointerAndState
                  .getState()
                  .dereferencePointer(newSourcePointerAndState.getValue())) {
            if (!newTargetObj2AndState.hasSMGObjectAndOffset()
                || !(newTargetObj2AndState.getOffsetForObject()
                    instanceof NumericValue newTargetObj2Offset)) {
              throw new SMGException(
                  "Error when evaluating the function " + functionCall + " in " + pCFAEdge);
              // continue;
            }
            evaluateMemcmpLastStep(
                targetObj1,
                targetOffset1,
                newTargetObj2AndState.getSMGObject(),
                newTargetObj2Offset.bigIntegerValue(),
                newTargetObj2AndState.getSMGState(),
                functionCall,
                pCFAEdge,
                resultBuilder);
          }
        }
      }

    } else {

      for (SMGStateAndOptionalSMGObjectAndOffset sourceAndState :
          functionCall
              .getParameterExpressions()
              .get(MEMCMP_CMP_TARGET2_PARAMETER)
              .accept(
                  new SMGCPAAddressVisitor(evaluator, pCurrentState, pCFAEdge, logger, options))) {

        SMGState currentState = sourceAndState.getSMGState();
        if (!sourceAndState.hasSMGObjectAndOffset()) {
          // Unknown addresses happen only of we don't have a memory associated
          throw new SMGException(
              "Error when evaluating the function " + functionCall + " in " + pCFAEdge);
          // resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          // continue;
        }
        SMGObject targetObj2 = sourceAndState.getSMGObject();
        Value targetOffset2 = sourceAndState.getOffsetForObject();

        if (!(targetOffset2 instanceof NumericValue numTargetOffset2)) {
          // Unknown offset
          throw new SMGException(
              "Error when evaluating the function " + functionCall + " in " + pCFAEdge);
          // resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          // continue;
        }

        evaluateMemcmpLastStep(
            targetObj1,
            targetOffset1,
            targetObj2,
            numTargetOffset2.bigIntegerValue(),
            currentState,
            functionCall,
            pCFAEdge,
            resultBuilder);
      }
    }
  }

  // Evals the 3rd parameter to memcmp with the first 2 given as source and target
  private void evaluateMemcmpLastStep(
      SMGObject targetObj1,
      BigInteger targetOffset1,
      SMGObject targetObj2,
      BigInteger targetOffset2,
      SMGState pCurrentState,
      CFunctionCallExpression functionCall,
      CFAEdge pCFAEdge,
      ImmutableList.Builder<ValueAndSMGState> resultBuilder)
      throws CPATransferException {
    for (ValueAndSMGState sizeAndState :
        getFunctionParameterValue(
            MEMCMP_CMP_SIZE_PARAMETER, functionCall, pCurrentState, pCFAEdge)) {

      SMGState currentState = sizeAndState.getState();
      Value sizeValue = sizeAndState.getValue();

      if (!(sizeValue instanceof NumericValue)) {
        throw new SMGException(
            "Error when evaluating third parameter of the function "
                + functionCall
                + " in "
                + pCFAEdge);
        // resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
        // continue;
      }

      resultBuilder.add(
          evaluateMemcmpComparison(
              currentState,
              targetObj1,
              targetOffset1,
              targetObj2,
              targetOffset2,
              sizeValue,
              pCFAEdge));
    }
  }

  private ValueAndSMGState evaluateMemcmpComparison(
      SMGState pCurrentState,
      SMGObject pTargetObj1,
      BigInteger pTargetOffset1,
      SMGObject pTargetObj2,
      BigInteger pTargetOffset2,
      Value pSizeValue,
      CFAEdge pCFAEdge)
      throws SMGException, SMGSolverException {

    if (!(pSizeValue instanceof NumericValue sizeValue)) {
      throw new SMGException(
          "Size of comparison in function memcmp is not concrete and can not be handled in "
              + pCFAEdge);
    }
    if (!(pTargetObj1.getSize() instanceof NumericValue targetObj1Size)) {
      throw new SMGException(
          "Size of memory in first argument in function memcmp is not concrete and can not be"
              + " handled in "
              + pCFAEdge);
    }
    if (!(pTargetObj2.getSize() instanceof NumericValue targetObj2Size)) {
      throw new SMGException(
          "Size of memory in second argument in function memcmp is not concrete and can not be"
              + " handled in "
              + pCFAEdge);
    }

    if (pTargetObj1.isZero() || pTargetObj2.isZero()) {
      throw new SMGException("Function call memcmp has been called on 0 memory in " + pCFAEdge);
    }

    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> hvesByObjects =
        pCurrentState.getMemoryModel().getSmg().getSMGObjectsWithSMGHasValueEdges();

    SMGProveNequality nequalityCheck = new SMGProveNequality(pCurrentState);

    BigInteger numericSizeArgumentInBits =
        sizeValue.bigIntegerValue().multiply(BigInteger.valueOf(8));
    BigInteger obj1FullOffset = pTargetOffset1.add(pTargetObj1.getOffset());
    BigInteger obj2FullOffset = pTargetOffset2.add(pTargetObj2.getOffset());
    BigInteger obj1FullOffsetPlusSizeArgInBits = obj1FullOffset.add(numericSizeArgumentInBits);
    BigInteger obj2FullOffsetPlusSizeArgInBits = obj2FullOffset.add(numericSizeArgumentInBits);

    if (targetObj1Size
            .bigIntegerValue()
            .subtract(obj1FullOffset)
            .compareTo(numericSizeArgumentInBits)
        < 0) {
      throw new SMGException(
          "Size of comparison in function memcmp is larger than the given first objects size"
              + " relative to its current offset in "
              + pCFAEdge);
    } else if (targetObj2Size
            .bigIntegerValue()
            .subtract(obj2FullOffset)
            .compareTo(numericSizeArgumentInBits)
        < 0) {
      throw new SMGException(
          "Size of comparison in function memcmp is larger than the given second objects size"
              + " relative to its current offset in "
              + pCFAEdge);
    }

    if (pTargetObj1.equals(pTargetObj2) && obj1FullOffset.equals(obj2FullOffset)) {
      return ValueAndSMGState.of(new NumericValue(BigInteger.ZERO), pCurrentState);
    }

    PersistentSet<SMGHasValueEdge> allHvesObj1 = hvesByObjects.get(pTargetObj1);

    PersistentSet<SMGHasValueEdge> allHvesObj2 = hvesByObjects.get(pTargetObj2);

    if (allHvesObj1 == null || allHvesObj1.isEmpty()) {
      // If there are no edges, the values are random, so there is no order
      return ValueAndSMGState.ofUnknownValue(
          pCurrentState,
          "Returned unknown because of absence of values to compare in first argument in function"
              + " memcmp in",
          pCFAEdge);
    } else if (allHvesObj2 == null || allHvesObj2.isEmpty()) {
      // we know that there are edges in obj1 while 2 is unknown
      return ValueAndSMGState.ofUnknownValue(
          pCurrentState,
          "Returned unknown because of absence of values to compare in second argument in function"
              + " memcmp in",
          pCFAEdge);
    }

    // No Object is empty, but there might be edges in one when there are none in the other.
    // First filter out all edges not (partially) covered by the size given, then check if there are
    // edges covering the entire size argument.
    NavigableMap<BigInteger, SMGHasValueEdge> hvesObj1InSizeOrdered = new TreeMap<>();
    for (SMGHasValueEdge hve : allHvesObj1) {
      BigInteger hveOffset = hve.getOffset();
      BigInteger hveOffsetPlusSize = hveOffset.add(hve.getSizeInBits());
      // Either the hve starts in the compare area, or ends there
      if (!(hveOffset.compareTo(obj1FullOffsetPlusSizeArgInBits) >= 0
          || hveOffsetPlusSize.compareTo(obj1FullOffset) <= 0)) {
        // We normalize the offsets for the initial offset of the pointer to the object.
        hvesObj1InSizeOrdered.put(hve.getOffset().subtract(obj1FullOffset), hve);
      }
    }

    NavigableMap<BigInteger, SMGHasValueEdge> hvesObj2InSizeOrdered = new TreeMap<>();
    for (SMGHasValueEdge hve : allHvesObj2) {
      BigInteger hveOffset = hve.getOffset();
      BigInteger hveOffsetPlusSize = hveOffset.add(hve.getSizeInBits());
      if (!(hveOffset.compareTo(obj2FullOffsetPlusSizeArgInBits) >= 0
          || hveOffsetPlusSize.compareTo(obj2FullOffset) <= 0)) {
        // We normalize the offsets for the initial offset of the pointer to the object.
        hvesObj2InSizeOrdered.put(hve.getOffset().subtract(obj2FullOffset), hve);
      }
    }

    Set<BigInteger> offsetToRemoveInBoth = new HashSet<>();

    // Compare by equally sized blocks if possible first.
    for (Entry<BigInteger, SMGHasValueEdge> offsetAndHve1 : hvesObj1InSizeOrdered.entrySet()) {
      BigInteger offsetToCheck = offsetAndHve1.getKey();
      SMGHasValueEdge hve1ToCheck = offsetAndHve1.getValue();
      // We normalized the offsets for the initial offsets of the pointers to the objects.
      // So if there are equal blocks relative to the start offsets, they have the same offset now.
      SMGHasValueEdge hve2ToCheck = hvesObj2InSizeOrdered.get(offsetToCheck);

      if (hve2ToCheck == null) {
        // If both offset and offset + size are inside the area to be checked and hve2ForOffset does
        // not have an edge that overlapps with our current one, abort, return unknown
        if (offsetToCheck.compareTo(obj1FullOffset) >= 0
            && offsetToCheck
                    .add(hve1ToCheck.getSizeInBits())
                    .compareTo(obj1FullOffsetPlusSizeArgInBits)
                < 0) {
          return ValueAndSMGState.ofUnknownValue(
              pCurrentState,
              "Returned unknown for problem with compare ranges in function memcmp in",
              pCFAEdge);
        }
        // Not equal blocks, perform more detailed checks later.
        continue;
      }

      if (!hve1ToCheck.getSizeInBits().equals(hve2ToCheck.getSizeInBits())) {
        // Non-equal sized blocks, check with smaller blocks below.
        continue;
      }

      SMGValue hve1SMGValue = hve1ToCheck.hasValue();
      SMGValue hve2SMGValue = hve2ToCheck.hasValue();
      Value hve1Value =
          pCurrentState.getMemoryModel().getValueFromSMGValue(hve1SMGValue).orElseThrow();
      Value hve2Value =
          pCurrentState.getMemoryModel().getValueFromSMGValue(hve2SMGValue).orElseThrow();

      if (hve1Value instanceof NumericValue numHve1Value
          && hve2Value instanceof NumericValue numHve2Value) {
        // Lexicographical order
        int lexOrder = numHve1Value.bigIntegerValue().compareTo(numHve2Value.bigIntegerValue());
        if (lexOrder == 0) {
          // Equal. Remove the blocks, check that all blocks have been removed later.
          offsetToRemoveInBoth.add(offsetToCheck);
          continue;
        } else {
          return ValueAndSMGState.of(new NumericValue(BigInteger.valueOf(lexOrder)), pCurrentState);
        }
      }

      if (!hve1Value.equals(hve2Value)
          || nequalityCheck.proveInequality(hve1SMGValue, hve2SMGValue)) {
        // Not equal.
        // Return a new symbolic value that is not 0.
        SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
        Value notZeroValue = factory.newIdentifier(null);
        final ConstraintFactory constraintFactory =
            ConstraintFactory.getInstance(
                pCurrentState, machineModel, logger, options, evaluator, pCFAEdge);

        final Constraint notEqZeroConstraint =
            constraintFactory.getNotEqualsZeroConstraint(
                notZeroValue, CNumericTypes.INT, pCurrentState);

        return ValueAndSMGState.of(notZeroValue, pCurrentState.addConstraint(notEqZeroConstraint));

      } else {
        // Possibly equal. We will treat it as equal for now. Remove the blocks, check that all
        // blocks have been removed later.
        offsetToRemoveInBoth.add(offsetToCheck);
      }
    }

    for (BigInteger offsetToRemove : offsetToRemoveInBoth) {
      hvesObj1InSizeOrdered.remove(offsetToRemove);
      hvesObj2InSizeOrdered.remove(offsetToRemove);
    }

    if (hvesObj1InSizeOrdered.isEmpty() && hvesObj2InSizeOrdered.isEmpty()) {
      // If there is nothing left, they are equal.
      return ValueAndSMGState.of(new NumericValue(BigInteger.ZERO), pCurrentState);
    }

    // Equally sized blocks not possible, use read, as read can break down parts of edges correctly.
    // TODO: At this point we would need a solver, if we ever need it implement this case.
    //  or return UNKNOWN.
    throw new SMGException("Could not compare the memory regions given in memcmp in " + pCFAEdge);
  }

  /**
   * This expects that targetAddress and sourceAddress are valid AddressExpressions with numeric
   * offsets. Copies all values from sourceAddress to targetAddress for the size specified in BYTES.
   *
   * @param pState current {@link SMGState}.
   * @param targetAddress {@link SMGObject} for the object of the target of the copy.
   * @param targetOffset target offset
   * @param sourceAddress {@link SMGObject} for object of the source of the copy operation.
   * @param sourceOffset offset of the source obj
   * @param numOfBytesToCopy {@link Value} that should be a {@link NumericValue} holding the number
   *     of bytes copied.
   * @return {@link ValueAndSMGState} with the pointer to target and the state with the copy.
   */
  private ValueAndSMGState evaluateMemcpy(
      SMGState pState,
      SMGObject targetAddress,
      BigInteger targetOffset,
      SMGObject sourceAddress,
      BigInteger sourceOffset,
      NumericValue numOfBytesToCopy)
      throws SMGException {

    long numOfBytes = numOfBytesToCopy.bigIntegerValue().longValue();
    if (numOfBytes < 0) {
      // the argument is unsigned, so we have to transform it into a postive. On most 64bit systems
      // it's an unsigned long (C99 standard)
      numOfBytes = Integer.toUnsignedLong(numOfBytesToCopy.bigIntegerValue().intValueExact());
    }

    BigInteger sizeToCopyInBits =
        BigInteger.valueOf(numOfBytes)
            .multiply(BigInteger.valueOf(machineModel.getSizeofCharInBits()));

    Value sourceAddressSize = sourceAddress.getSize();
    Value targetAddressSize = targetAddress.getSize();

    if (!(sourceAddressSize instanceof NumericValue numSourceAddressSize)
        || !(targetAddressSize instanceof NumericValue numTargetAddressSize)) {
      throw new SMGException("Symbolic memory size in memset currently not supported.");
    }
    BigInteger numericSourceAddressSize = numSourceAddressSize.bigIntegerValue();
    BigInteger numericTargetAddressSize = numTargetAddressSize.bigIntegerValue();
    // There can be deref errors if the size is too large
    if (numericSourceAddressSize.compareTo(sourceOffset.add(sizeToCopyInBits)) < 0) {
      return ValueAndSMGState.ofUnknownValue(
          pState.withInvalidRead(sourceAddress),
          "Returned unknown for invalid copy ranges in function memcpy.");
    } else if (numericTargetAddressSize.compareTo(targetOffset.add(sizeToCopyInBits)) < 0) {
      return ValueAndSMGState.ofUnknownValue(
          pState.withInvalidWrite(targetAddress),
          "Returned unknown for invalid copy ranges in function memcpy.");
    }

    SMGState copyResultState =
        pState.copySMGObjectContentToSMGObject(
            sourceAddress,
            new NumericValue(sourceOffset),
            targetAddress,
            new NumericValue(targetOffset),
            new NumericValue(sizeToCopyInBits));

    ValueAndSMGState newPointersAndStates =
        evaluator.searchOrCreatePointer(
            targetAddress, new NumericValue(targetOffset), copyResultState);

    return ValueAndSMGState.of(newPointersAndStates.getValue(), newPointersAndStates.getState());
  }

  /**
   * Returns the result of the comparison of the String arguments of the function call: int strcmp
   * (const char* str1, const char* str2); The result is a compare similar to BigInt.
   *
   * @param pFunctionCall contains the parameters for String comparison
   * @param pState current {@link SMGState}.
   * @param pCfaEdge logging and debugging.
   */
  private List<ValueAndSMGState> evaluateStrcmp(
      CFunctionCallExpression pFunctionCall, SMGState pState, CFAEdge pCfaEdge)
      throws CPATransferException {

    if (pFunctionCall.getParameterExpressions().size() != 2) {
      throw new UnrecognizedCodeException(
          "The function strcmp needs exactly 2 arguments.", pCfaEdge);
    }

    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();

    for (ValueAndSMGState firstValueAndSMGState :
        getFunctionParameterValue(STRCMP_FIRST_PARAMETER, pFunctionCall, pState, pCfaEdge)) {

      // If the Value is no AddressExpression we can't work with it
      // The buffer is type * and has to be an AddressExpression with a not unknown value and a
      // concrete offset to be used correctly
      Value firstAddress = firstValueAndSMGState.getValue();
      NumericValue firstAddressOffset = new NumericValue(0);
      if (firstAddress instanceof AddressExpression firstAddressExpr) {
        if (!(firstAddressExpr.getOffset() instanceof NumericValue firstAddressExprOffset)) {
          // Write the target region to unknown
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  firstValueAndSMGState.getState(),
                  "Returned unknown for unknown offset in first address in function strcmp in ",
                  pCfaEdge));
          continue;
        }
        firstAddress = firstAddressExpr.getMemoryAddress();
        firstAddressOffset = firstAddressExprOffset;

      } else if (!pState.getMemoryModel().isPointer(firstAddress)) {
        // The value can be unknown
        resultBuilder.add(
            ValueAndSMGState.ofUnknownValue(
                firstValueAndSMGState.getState(),
                "Returned unknown for unknown address in first address in function strcmp in ",
                pCfaEdge));
        continue;
      }

      for (ValueAndSMGState secondValueAndSMGState :
          getFunctionParameterValue(
              STRCMP_SECOND_PARAMETER, pFunctionCall, firstValueAndSMGState.getState(), pCfaEdge)) {
        Value secondAddress = secondValueAndSMGState.getValue();
        // If the Value is no AddressExpression we can't work with it
        // The buffer is type * and has to be an AddressExpression with a not unknown value and a
        // concrete offset to be used correctly
        if (!SMGCPAExpressionEvaluator.valueIsAddressExprOrVariableOffset(secondAddress)) {
          if (options.getHandleUnknownFunctions() == UnknownFunctionHandling.STRICT) {
            throw new SMGException(
                "Function strcmp() with symbolic handling not supported with option"
                    + " UnknownFunctionHandling.STRICT enabled");
          }
          // Unknown addresses happen only of we don't have a memory associated
          // TODO: decide what to do here and when this happens
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  secondValueAndSMGState.getState(),
                  "Returned unknown for unknown target or offset in second address in function"
                      + " strcmp in ",
                  pCfaEdge));
          continue;
        } else if (!(secondAddress instanceof AddressExpression)) {
          if (options.getHandleUnknownFunctions() == UnknownFunctionHandling.STRICT) {
            throw new SMGException(
                "Function strcmp() with symbolic handling not supported with option"
                    + " UnknownFunctionHandling.STRICT enabled");
          }
          // The value can be unknown
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  secondValueAndSMGState.getState(),
                  "Returned unknown for unknown address in second address in function strcmp in ",
                  pCfaEdge));
          continue;
        }
        AddressExpression secondAddressExpr = (AddressExpression) secondAddress;
        if (!(secondAddressExpr.getOffset() instanceof NumericValue secondAddressExprOffset)) {
          if (options.getHandleUnknownFunctions() == UnknownFunctionHandling.STRICT) {
            throw new SMGException(
                "Function strcmp() with symbolic handling not supported with option"
                    + " UnknownFunctionHandling.STRICT enabled");
          }
          // Write the target region to unknown
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  secondValueAndSMGState.getState(),
                  "Returned unknown for unknown offset in second address in function strcmp in ",
                  pCfaEdge));
          continue;
        }

        // iterate over all chars and compare them for 'int strcmp(const char * firstStringPointer,
        // const char *secondStringPointer)' (c99 * 7.21.4.2)
        resultBuilder.add(
            evaluator.stringCompare(
                firstAddress,
                firstAddressOffset.bigIntegerValue(),
                secondAddressExpr.getMemoryAddress(),
                secondAddressExprOffset.bigIntegerValue(),
                secondValueAndSMGState.getState()));
      }
    }
    return resultBuilder.build();
  }

  private List<ValueAndSMGState> evaluateRealloc(
      CFunctionCallExpression functionCall, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {

    if (functionCall.getParameterExpressions().size() != 2) {
      throw new UnrecognizedCodeException(
          functionCall.getFunctionNameExpression().toASTString() + " needs 2 argument.",
          cfaEdge,
          functionCall);
    }

    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();

    SMGCPAValueVisitor valueVisitor =
        new SMGCPAValueVisitor(evaluator, pState, cfaEdge, logger, options);

    for (ValueAndSMGState argumentOneAndState :
        functionCall.getParameterExpressions().getFirst().accept(valueVisitor)) {
      for (ValueAndSMGState argumentTwoAndState :
          getAllocateFunctionParameter(1, functionCall, argumentOneAndState.getState(), cfaEdge)) {

        // First arg is the ptr to the existing memory
        // Second arg is new memory size in bytes
        if (!(argumentTwoAndState.getValue() instanceof NumericValue)) {
          String infoMsg =
              "Could not determine a concrete size for a memory allocation function: "
                  + functionCall.getFunctionNameExpression();
          if (options.isAbortOnNonConcreteMemorySize()) {
            infoMsg += ", due to option abortOnNonConcreteMemorySize. At " + cfaEdge;
            throw new SMGException(infoMsg);
          } else if (options.getHandleUnknownFunctions() == UnknownFunctionHandling.STRICT) {
            infoMsg += ", due to option UnknownFunctionHandling.STRICT. At " + cfaEdge;
            throw new SMGException(infoMsg);
          } else {
            logger.log(Level.INFO, infoMsg + ", in " + cfaEdge);
          }
        }

        resultBuilder.addAll(
            evaluateReallocWParameters(
                argumentTwoAndState.getState(),
                argumentOneAndState.getValue(),
                argumentTwoAndState.getValue(),
                cfaEdge,
                functionCall));
      }
    }

    return resultBuilder.build();
  }

  /**
   * Evaluates realloc(ptr, size); If the size is smaller than the current memory size, every
   * HV-Edge (value) outside the bounds is deleted. If the size is 0, we free the pointer and call
   * malloc(0) and return the pointer. If the ptr is null, we call malloc(size). We always return
   * new memory (new pointer to new memory) and free the old if it exists.
   *
   * @param pState current {@link SMGState}
   * @param pSizeValue size in byte
   * @param pCfaEdge current CFA edge
   * @return list of points to new memory and its states
   */
  private Collection<ValueAndSMGState> evaluateReallocWParameters(
      SMGState pState,
      Value pPtrValue,
      Value pSizeValue,
      CFAEdge pCfaEdge,
      CFunctionCallExpression functionCall)
      throws SMGException, SMGSolverException {

    if (pPtrValue instanceof AddressExpression ptrAddrExpr) {
      if (!(ptrAddrExpr.getOffset() instanceof NumericValue ptrAddrExprOffset)
          || !ptrAddrExprOffset.bigIntegerValue().equals(BigInteger.ZERO)) {
        throw new SMGException(
            "Realloc with pointers not pointing to their original offset not supported yet. "
                + pCfaEdge);
      }
      pPtrValue = ptrAddrExpr.getMemoryAddress();
    }

    Value sizeInBits;
    if (!pState.getMemoryModel().isPointer(pPtrValue)) {
      // undefined beh
      return ImmutableList.of(ValueAndSMGState.of(pPtrValue, pState));
    }

    CType sizeType = functionCall.getParameterExpressions().getFirst().getExpressionType();
    if (pSizeValue instanceof NumericValue numSizeOfValue) {
      sizeInBits =
          new NumericValue(numSizeOfValue.bigIntegerValue().multiply(BigInteger.valueOf(8)));

    } else {
      // Size symbolic
      if (options.getHandleUnknownFunctions() == UnknownFunctionHandling.STRICT) {
        throw new SMGException(
            "Can't handle symbolic parameter in C function realloc() with option"
                + " UnknownFunctionHandling.STRICT set. Location in program "
                + pCfaEdge);
      } else if (options.trackPredicates()) {
        sizeType =
            SMGCPAExpressionEvaluator.promoteMemorySizeTypeForBitCalculation(
                functionCall.getParameterExpressions().getFirst().getExpressionType(),
                machineModel);
        sizeInBits = evaluator.multiplyBitOffsetValues(pSizeValue, BigInteger.valueOf(8));

      } else {
        throw new SMGException(
            "Can't handle symbolic paramters in C function realloc() without tracking predicates at"
                + " "
                + pCfaEdge);
      }
    }

    SMGState currentState = pState;
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();

    // Handle (realloc(0, size) -> just malloc
    if (isNumericZero(pPtrValue)) {
      return handleConfigurableMemoryAllocation(
          functionCall, currentState, sizeInBits, sizeType, pCfaEdge);
    }

    // Handle realloc(ptr, 0) (before C23), (C23 its just undefined beh)
    if (sizeInBits instanceof NumericValue numSize
        && numSize.bigIntegerValue().equals(BigInteger.ZERO)) {
      resultBuilder = ImmutableList.builder();
      for (SMGState freedState : currentState.free(pPtrValue, functionCall, pCfaEdge)) {
        resultBuilder.add(handleAllocZero(freedState));
      }
      return resultBuilder.build();
    }

    for (SMGStateAndOptionalSMGObjectAndOffset oldObj :
        currentState.dereferencePointer(pPtrValue)) {
      currentState = oldObj.getSMGState();

      // The copy is always the lesser size of the 2
      Value oldSize =
          evaluator.subtractBitOffsetValues(
              oldObj.getSMGObject().getSize(), oldObj.getOffsetForObject());

      if (!(oldSize instanceof NumericValue numOldSize)
          || !(sizeInBits instanceof NumericValue numSizeInBits)) {
        throw new SMGException("Symbolic memory size in realloc() currently not supported.");
        // TODO: add STRICT function handling check once allowed!
      }
      // TODO: check that right is always larger than left, then just copy all edges into the new
      // memory

      // Malloc new memory
      ValueAndSMGState addressAndState =
          evaluator.createHeapMemoryAndPointer(currentState, sizeInBits);
      Value addressToNewRegion = addressAndState.getValue();
      currentState = addressAndState.getState();
      // New mem can not materialize, and we know the offset is 0
      SMGObject newMemory =
          currentState
              .dereferencePointerWithoutMaterilization(addressToNewRegion)
              .orElseThrow()
              .getSMGObject();

      BigInteger copySizeInBits = numSizeInBits.bigIntegerValue();
      if (numOldSize.bigIntegerValue().compareTo(copySizeInBits) < 0) {
        copySizeInBits = numOldSize.bigIntegerValue();
      }
      // free old memory
      currentState =
          currentState.copySMGObjectContentToSMGObject(
              oldObj.getSMGObject(),
              oldObj.getOffsetForObject(),
              newMemory,
              new NumericValue(BigInteger.ZERO),
              new NumericValue(copySizeInBits));
      for (SMGState freedState : currentState.free(pPtrValue, functionCall, pCfaEdge)) {
        resultBuilder.add(ValueAndSMGState.of(addressToNewRegion, freedState));
      }
    }
    return resultBuilder.build();
  }

  // TODO: strlen

  /**
   * Handle calls to __builtin_popcount, __builtin_popcountl, and __builtin_popcountll. Popcount
   * sums up all 1-bits in an unsigned int, unsigned long int or unsigned long long int number
   * given. Test C programs available at test/programs/simple/builtin_popcount*.c
   */
  private List<ValueAndSMGState> handlePopcount(
      String pFunctionName, SMGState pState, CFunctionCallExpression functionCall, CFAEdge edge)
      throws CPATransferException {

    if (functionCall.getParameterExpressions().size() != 1) {
      throw new UnrecognizedCodeException(
          "Function "
              + pFunctionName
              + " received "
              + functionCall.getParameterExpressions().size()
              + " parameters"
              + " instead of the expected "
              + 1,
          functionCall);
    }

    ImmutableList.Builder<ValueAndSMGState> result = ImmutableList.builder();
    CSimpleType paramType = getParameterTypeOfBuiltinPopcountFunction(pFunctionName);
    assert functionCall.getExpressionType() instanceof CSimpleType simpleType
        && simpleType.getCanonicalType().getType().isIntegerType()
        && simpleType.getCanonicalType().hasSignedSpecifier();
    assert paramType.hasUnsignedSpecifier();

    // Cast to unsigned target type
    for (ValueAndSMGState evaluatedParamAndState :
        getFunctionParameterValue(0, functionCall, paramType, pState, edge)) {
      Value castParamValue = evaluatedParamAndState.getValue();
      SMGState currentState = evaluatedParamAndState.getState();

      if (castParamValue instanceof NumericValue numCastParamValue) {
        BigInteger numericParam = numCastParamValue.bigIntegerValue();

        // Check that the cast function parameter is really unsigned, as defined by the function and
        // needed by Java BigInteger.bitcount() to be correct, as negative values give distinct
        // results
        verify(
            numericParam.signum() >= 0,
            "Evaluated parameter for C function %s is negative, but the function defines unsigned"
                + " parameters only",
            pFunctionName);

        result.add(ValueAndSMGState.of(new NumericValue(numericParam.bitCount()), currentState));

      } else if (options.trackPredicates() && !castParamValue.isUnknown()) {

        checkArgument(castParamValue instanceof SymbolicExpression);

        SymbolicExpression symbolicParam = (SymbolicExpression) castParamValue;
        int parameterBitSize = machineModel.getSizeofInBits(paramType);

        final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
        SymbolicExpression one = factory.asConstant(new NumericValue(1), CNumericTypes.INT);
        SymbolicExpression constraint =
            factory.binaryAnd(symbolicParam, one, CNumericTypes.INT, paramType);

        // Add up the bits one by one
        // (castParamValue >> 0) & 1 + (castParamValue >> 1) & 1 + ...
        for (int i = 1; i < parameterBitSize; i++) {
          SymbolicExpression countOfBitAtIndex =
              factory.binaryAnd(
                  factory.shiftRightUnsigned(
                      symbolicParam,
                      factory.asConstant(new NumericValue(i), CNumericTypes.INT),
                      paramType,
                      paramType),
                  one,
                  CNumericTypes.INT,
                  paramType);
          constraint =
              factory.add(constraint, countOfBitAtIndex, CNumericTypes.INT, CNumericTypes.INT);
        }

        result.add(ValueAndSMGState.of(constraint, currentState));
      } else {

        // TODO: we could associate a symbolic output with the input used to assure ==
        result.add(ValueAndSMGState.of(Value.UnknownValue.getInstance(), currentState));
      }
    }

    return result.build();
  }

  private static boolean isNumericZero(Value value) {
    return value instanceof NumericValue numValue
        && numValue.bigIntegerValue().equals(BigInteger.ZERO);
  }

  @SuppressWarnings("unused")
  private void handleDisallowedOverapproximationIfNeeded(
      CFunctionCallExpression cFCExpression, CFAEdge pCfaEdge) throws SMGException {
    if (options.getHandleUnknownFunctions() == UnknownFunctionHandling.STRICT) {
      throw new SMGException(
          "Can't handle C function "
              + cFCExpression.getFunctionNameExpression().toASTString()
              + "() with option "
              + options.getHandleUnknownFunctions()
              + " set, as this would lead to overapproximated handling. Location in program "
              + pCfaEdge);
    }
  }
}
