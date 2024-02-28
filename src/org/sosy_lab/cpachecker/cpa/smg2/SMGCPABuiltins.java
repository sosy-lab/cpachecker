// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.java_smt.api.SolverException;

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
  private static final int MALLOC_PARAMETER = 0;
  private static final int STRCMP_FIRST_PARAMETER = 0;
  private static final int STRCMP_SECOND_PARAMETER = 1;

  // TODO: Properly model printf (dereferences and stuff)
  // TODO: General modelling system for functions which do not modify state?
  private final Set<String> BUILTINS =
      Sets.newHashSet(
          "__VERIFIER_BUILTIN_PLOT",
          "memcpy",
          "memset",
          "__builtin_alloca",
          "alloca",
          "printf",
          "strcmp",
          "realloc",
          "__builtin_va_start",
          "__builtin_va_arg",
          "__builtin_va_end",
          "__builtin_va_copy");

  /**
   * Returns true if the functionName equals a built in function handleable by this class. This
   * class mostly handles memory related stuff i.e. malloc/free.
   *
   * @param functionName name of the function to check.
   * @return true for the specified names, false else.
   */
  boolean isABuiltIn(String functionName) {
    return (BUILTINS.contains(functionName)
        || isNondetBuiltin(functionName)
        || isConfigurableAllocationFunction(functionName)
        || isDeallocationFunction(functionName)
        || isExternalAllocationFunction(functionName));
  }

  private static final String NONDET_PREFIX = "__VERIFIER_nondet_";

  /**
   * Returns true if the function is some function involving nondeterministic behaviour (i.e.
   * returning a nondet int).
   *
   * @param pFunctionName name of the function to check.
   * @return true for the specified names, false else.
   */
  private boolean isNondetBuiltin(String pFunctionName) {
    return pFunctionName.startsWith(NONDET_PREFIX) || pFunctionName.equals("nondet_int");
  }

  /**
   * Checks if the input is one of the following memory allocation methods: "malloc", "__kmalloc",
   * "kmalloc" and "calloc", maybe more if they are added, and returns true if it is one of those.
   * false else.
   *
   * @param functionName name of the function to check.
   * @return true for the specified names, false else.
   */
  public boolean isConfigurableAllocationFunction(String functionName) {
    return options.getMemoryAllocationFunctions().contains(functionName)
        || options.getArrayAllocationFunctions().contains(functionName);
  }

  /**
   * True for deallocation methods supported (i.e. "free").
   *
   * @param functionName name of the function to check.
   * @return true for the specified names, false else.
   */
  public boolean isDeallocationFunction(String functionName) {
    return options.getDeallocationFunctions().contains(functionName);
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
   * Routes to the correct function call. Only handles built in functions. If no such function is
   * found this returns a unknown value.
   *
   * @param pFunctionCall {@link CFunctionCallExpression} that has been checked for all other known
   *     functions (math functions etc.) and only unknown and builtin functions for isABuiltIn() ==
   *     true are left.
   * @param functionName Name of the function.
   * @param pSmgState current {@link SMGState}.
   * @param pCfaEdge for logging/debugging.
   * @return the result of the function call and the state for it. May be an error state!
   * @throws CPATransferException in case of a critical error the SMGCPA can't handle.
   */
  public List<ValueAndSMGState> handleFunctionCall(
      CFunctionCallExpression pFunctionCall,
      String functionName,
      SMGState pSmgState,
      CFAEdge pCfaEdge)
      throws CPATransferException, SolverException, InterruptedException {
    if (isABuiltIn(functionName)) {
      if (isConfigurableAllocationFunction(functionName)) {
        return evaluateConfigurableAllocationFunction(
            pFunctionCall, functionName, pSmgState, pCfaEdge);
      } else {
        return handleBuiltinFunctionCall(pCfaEdge, pFunctionCall, functionName, pSmgState);
      }
    }
    return handleUnknownFunction(pCfaEdge, pFunctionCall, functionName, pSmgState);
  }

  /**
   * Handle a function call to a builtin function like memcpy.
   *
   * @param pCfaEdge for logging/debugging.
   * @param cFCExpression {@link CFunctionCallExpression} that leads to a non memory allocating
   *     builtin function.
   * @param calledFunctionName The name of the function to be called.
   * @param pState current {@link SMGState}.
   * @return the result of the function call and the state for it. May be an error state!
   * @throws CPATransferException in case of a critical error the SMGCPA can't handle.
   */
  public List<ValueAndSMGState> handleBuiltinFunctionCall(
      CFAEdge pCfaEdge,
      CFunctionCallExpression cFCExpression,
      String calledFunctionName,
      SMGState pState)
      throws CPATransferException, SolverException, InterruptedException {

    if (isExternalAllocationFunction(calledFunctionName)) {
      return evaluateExternalAllocation(cFCExpression, pState);
    }

    switch (calledFunctionName) {
      case "alloca":
      case "__builtin_alloca":
        return evaluateAlloca(cFCExpression, pState, pCfaEdge);

      case "memset":
        return evaluateMemset(cFCExpression, pState, pCfaEdge);

      case "memcpy":
        return evaluateMemcpy(cFCExpression, pState, pCfaEdge);

      case "strcmp":
        return evaluateStrcmp(cFCExpression, pState, pCfaEdge);

      case "__VERIFIER_BUILTIN_PLOT":
        evaluateVBPlot(cFCExpression, pState);
        // $FALL-THROUGH$
      case "printf":
        List<SMGState> checkedStates =
            checkAllParametersForValidity(pState, pCfaEdge, cFCExpression);
        return Collections3.transformedImmutableListCopy(
            checkedStates, ValueAndSMGState::ofUnknownValue);

      case "realloc":
        return evaluateRealloc(cFCExpression, pState, pCfaEdge);

      case "__builtin_va_start":
        return evaluateVaStart(cFCExpression, pCfaEdge, pState);
      case "__builtin_va_arg":
        return evaluateVaArg(cFCExpression, pCfaEdge, pState);
      case "__builtin_va_copy":
        return evaluateVaCopy(cFCExpression, pCfaEdge, pState);
      case "__builtin_va_end":
        return evaluateVaEnd(cFCExpression, pCfaEdge, pState);

      default:
        if (isNondetBuiltin(calledFunctionName)) {
          // TODO:
          return Collections.singletonList(ValueAndSMGState.ofUnknownValue(pState));
        } else {
          throw new UnsupportedOperationException(
              "Unexpected function handled as a builtin: " + calledFunctionName);
        }
    }
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
    CExpression firstArg = cFCExpression.getParameterExpressions().get(0);
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
      throws CPATransferException, SolverException, InterruptedException {
    Preconditions.checkArgument(cFCExpression.getParameterExpressions().size() == 2);
    // The first argument is the destination
    CExpression destArg = cFCExpression.getParameterExpressions().get(0);
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
    ValueAndSMGState addressAndState = addressesAndStates.get(0);
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
            sizeInBits,
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
        currentStack
            .getFunctionDefinition()
            .getParameters()
            .get(currentStack.getFunctionDefinition().getParameters().size() - 1);
    if (!paramDecl.getType().equals(secondArg.getExpressionType())) {
      // Log warning (gcc only throws a warning and it works anyway)
      logger.logf(
          Level.INFO,
          "The types of variable arguments (%s) do not match the last not "
              + "variable argument of the function (%s)",
          paramDecl.getType(),
          secondArg.getExpressionType());
    }
    BigInteger sizeInBitsPointer = evaluator.getBitSizeof(pState, firstArg);

    BigInteger sizeInBitsVarArg = evaluator.getBitSizeof(pState, secondArg);
    BigInteger overallSizeOfVarArgs =
        BigInteger.valueOf(currentStack.getVariableArguments().size()).multiply(sizeInBitsVarArg);

    ValueAndSMGState pointerAndState =
        evaluator.createHeapMemoryAndPointer(currentState, overallSizeOfVarArgs);

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
              sizeInBitsVarArg,
              varArg,
              SMGCPAExpressionEvaluator.getCanonicalType(secondArg),
              cfaEdge);
      // Unlikely that someone throws an abstracted list into a var args
      Preconditions.checkArgument(writtenStates.size() == 1);
      currentState = writtenStates.get(0);
      offset = offset.add(sizeInBitsVarArg);
    }

    return ImmutableList.of(ValueAndSMGState.ofUnknownValue(currentState));
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
      SMGState pState, CFAEdge pCfaEdge, CFunctionCallExpression cFCExpression)
      throws CPATransferException {
    // check that we can safely read all args,
    // to avoid invalid-derefs like   int * p; printf("%d", *p);
    SMGState currentState = pState;
    for (CExpression param : cFCExpression.getParameterExpressions()) {
      if (param instanceof CPointerExpression
          || param instanceof CFieldReference
          || param instanceof CArraySubscriptExpression) {
        SMGCPAValueVisitor valueVisitor =
            new SMGCPAValueVisitor(evaluator, currentState, pCfaEdge, logger, options);
        for (ValueAndSMGState valueAndState : param.accept(valueVisitor)) {
          // We only want error states
          currentState = valueAndState.getState();
        }
      }
    }
    return ImmutableList.of(currentState);
  }

  /**
   * @param pCfaEdge for logging/debugging.
   * @param cFCExpression the {@link CFunctionCallExpression} that lead to this function call.
   * @param calledFunctionName The name of the function to be called.
   * @param pState current {@link SMGState}.
   * @return a {@link List} of {@link ValueAndSMGState}s with either valid {@link Value}s and {@link
   *     SMGState}s, or unknown Values and maybe error states. Depending on the safety of the
   *     function/config.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  List<ValueAndSMGState> handleUnknownFunction(
      CFAEdge pCfaEdge,
      CFunctionCallExpression cFCExpression,
      String calledFunctionName,
      SMGState pState)
      throws CPATransferException {
    // This mostly returns unknown if it does not find a function to handle
    if (calledFunctionName.contains("pthread")) {
      throw new SMGException("Concurrency analysis not supported in this configuration.");
    }
    switch (options.getHandleUnknownFunctions()) {
      case STRICT:
        if (!isSafeFunction(calledFunctionName)) {
          throw new CPATransferException(
              String.format(
                  "Unknown function '%s' may be unsafe. See the"
                      + " cpa.smg2.SMGCPABuiltins.handleUnknownFunction()",
                  calledFunctionName));
        }
        // fallthrough for safe functions
        // $FALL-THROUGH$
      case ASSUME_SAFE:
      case ASSUME_EXTERNAL_ALLOCATED:
        List<SMGState> checkedStates =
            checkAllParametersForValidity(pState, pCfaEdge, cFCExpression);
        return Collections3.transformedImmutableListCopy(
            checkedStates, ValueAndSMGState::ofUnknownValue);
      default:
        throw new UnsupportedOperationException(
            "Unhandled function in cpa.smg2.SMGCPABuiltins.handleUnknownFunction(): "
                + options.getHandleUnknownFunctions());
    }
  }

  /**
   * Gets the size of an allocation. This needs either 1 or 2 parameters. Those are read and
   * evaluated to the size for the allocation. Might throw an exception in case of an error.
   * Currently, sizes are only calculated concretely, not symbolicly.
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

        if (!value1.isNumericValue()) {
          String infoMsg =
              "Could not determine a concrete size for a memory allocation function: "
                  + functionCall.getFunctionNameExpression();
          if (options.isAbortOnNonConcreteMemorySize()) {
            throw new UnrecognizedCodeException(infoMsg, cfaEdge);
          } else {
            logger.log(Level.INFO, infoMsg + ", in " + cfaEdge);
          }
          // Max overapproximation
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(state1));
          continue;
        }

        for (ValueAndSMGState value2AndState :
            getAllocateFunctionParameter(
                options.getMemoryArrayAllocationFunctionsElemSizeParameter(),
                functionCall,
                state1,
                cfaEdge)) {

          Value value2 = value2AndState.getValue();
          SMGState state2 = value2AndState.getState();
          if (!value2.isNumericValue()) {
            logger.log(
                Level.INFO,
                "Could not determine a concrete value for a memory allocation function: "
                    + functionName
                    + ", in: "
                    + cfaEdge);
            resultBuilder.add(ValueAndSMGState.ofUnknownValue(state2));
            continue;
          } else {
            BigInteger size =
                value1
                    .asNumericValue()
                    .bigIntegerValue()
                    .multiply(value2.asNumericValue().bigIntegerValue());
            resultBuilder.add(ValueAndSMGState.of(new NumericValue(size), state2));
          }
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
   * @param pParameterNumber the number of the paramter for the function. I.e. foo (x); x would be
   *     paramter 0.
   * @param functionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return {@link List} of {@link ValueAndSMGState}s each representing the paramteter requested.
   *     Each should be treated as a valid paramter.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private List<ValueAndSMGState> getFunctionParameterValue(
      int pParameterNumber, CFunctionCallExpression functionCall, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {

    CRightHandSide expr;
    String functionName = functionCall.getFunctionNameExpression().toASTString();
    try {
      expr = functionCall.getParameterExpressions().get(pParameterNumber);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException(
          functionName + " argument #" + pParameterNumber + " not found.", cfaEdge, functionCall);
    }

    SMGCPAValueVisitor vv = new SMGCPAValueVisitor(evaluator, pState, cfaEdge, logger, options);
    return vv.evaluate(expr, SMGCPAExpressionEvaluator.getCanonicalType(functionCall));
  }

  /**
   * Handles all allocation methods i.e. malloc Returns the pointer Value to the new memory region
   * (that may be written to 0 for the correct function i.e. calloc). This also returns a state for
   * the failure of the allocation function if the option is enabled.
   *
   * @param functionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return {@link List} of {@link ValueAndSMGState}s holding valid and invalid returns (if
   *     enabled) for the called allocation functions. The {@link Value} will not be a {@link
   *     AddressExpression}, but may be numeric 0 (leading to the 0 SMGObject). Valid {@link Value}s
   *     pointers have always offset 0. Invalid calls may have a unknown {@link Value} as a return
   *     type in case of errors a error state is set.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  List<ValueAndSMGState> evaluateConfigurableAllocationFunction(
      CFunctionCallExpression functionCall, String functionName, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {

    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();

    for (ValueAndSMGState sizeAndState : getAllocateFunctionSize(pState, cfaEdge, functionCall)) {

      Value sizeValue = sizeAndState.getValue();
      SMGState currentState = sizeAndState.getState();

      if (!sizeValue.isNumericValue()) {
        String infoMsg =
            "Could not determine a concrete size for a memory allocation function: "
                + functionCall.getFunctionNameExpression();
        if (options.isAbortOnNonConcreteMemorySize()) {
          throw new UnrecognizedCodeException(infoMsg, cfaEdge);
        } else {
          logger.log(Level.INFO, infoMsg + ", in " + cfaEdge);
        }
        if (options.isGuessSizeOfUnknownMemorySize()) {
          sizeValue = new NumericValue(options.getGuessSize());
        } else if (options.isIgnoreUnknownMemoryAllocation()) {
          // Ignore and move on
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        } else if (options.isErrorOnUnknownMemoryAllocation()) {
          // Error for CEGAR to learn the variable
          // TODO: this is bad! For truly unknown variables this also just plainly errors. Think of
          // a better way
          resultBuilder.add(
              ValueAndSMGState.ofUnknownValue(
                  currentState.withMemoryLeak(
                      "Plain memory error for malloc with symbolic size.", ImmutableList.of())));
          continue;
        } else {
          throw new AssertionError(
              "An allocation function ("
                  + functionName
                  + ") was called with a symbolic size. This is not supported"
                  + " currently by the SMG2 analysis. Try GuessSizeOfUnknownMemorySize.");
        }
      }
      // The size is always given in bytes
      BigInteger sizeInBits =
          sizeValue.asNumericValue().bigIntegerValue().multiply(BigInteger.valueOf(8));

      resultBuilder.addAll(
          handleConfigurableMemoryAllocation(functionCall, currentState, sizeInBits, cfaEdge));
    }

    return resultBuilder.build();
  }

  // malloc(size) w size in bits
  private ImmutableList<ValueAndSMGState> handleConfigurableMemoryAllocation(
      CFunctionCallExpression functionCall, SMGState pState, BigInteger sizeInBits, CFAEdge edge)
      throws SMGException, SMGSolverException {
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    SMGState currentState = pState;
    String functionName = functionCall.getFunctionNameExpression().toASTString();

    if (sizeInBits.compareTo(BigInteger.ZERO) == 0) {
      resultBuilder.add(handleAllocZero(currentState));
      return resultBuilder.build();
    }

    // Create a new memory region with the specified size and use the pointer to its beginning
    // from now on
    ValueAndSMGState addressAndState =
        evaluator.createHeapMemoryAndPointer(currentState, sizeInBits);
    Value addressToNewRegion = addressAndState.getValue();
    SMGState stateWithNewHeap = addressAndState.getState();

    if (options.getZeroingMemoryAllocation().contains(functionName)) {
      // Since this is newly created memory get(0) is fine
      stateWithNewHeap =
          stateWithNewHeap
              .writeToZero(addressToNewRegion, functionCall.getExpressionType(), edge)
              .get(0);
    }
    resultBuilder.add(ValueAndSMGState.of(addressToNewRegion, stateWithNewHeap));

    // If malloc can fail (and fails) it simply returns a pointer to 0 (C also sets errno)
    if (options.isEnableMallocFailure()) {
      // This mapping always exists
      Value addressToZero = new NumericValue(0);
      resultBuilder.add(ValueAndSMGState.of(addressToZero, currentState));
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
      return evaluator.createMallocZeroMemoryAndPointer(currentState, BigInteger.ONE);
    }
  }

  /**
   * Checks for known safe functions and returns true if the entered function name is one.
   *
   * @param calledFunctionName name of the called function to be checked.
   * @return true is the function is safe, false else.
   */
  private boolean isSafeFunction(String calledFunctionName) {
    for (String safeUnknownFunctionPattern : options.getSafeUnknownFunctions()) {
      if (Pattern.compile(safeUnknownFunctionPattern).matcher(calledFunctionName).matches()) {
        return true;
      }
    }
    return false;
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
      // The buffer is type * and has to be a AddressExpression with a not unknown value and a
      // concrete offset to be used correctly
      if (!(bufferValue instanceof AddressExpression)
          || ((AddressExpression) bufferValue).getMemoryAddress().isUnknown()
          || !((AddressExpression) bufferValue).getOffset().isNumericValue()) {
        currentState = currentState.withInvalidWrite(bufferValue);
        resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
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
                  (AddressExpression) bufferValue,
                  charValueAndSMGState.getValue(),
                  countAndState.getValue()));
        }
      }
    }

    return resultBuilder.build();
  }

  /**
   * Checks the Values such that they are useable, returns a unknown Value with error state (may be
   * non critical) if its not useable. Else it writes the char entered (int value) into the region
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
   *     AddressExpression}) and the written {@link SMGState}, or a unknown value and maybe a error
   *     state.
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
      return ValueAndSMGState.ofUnknownValue(currentState);
    }
    if (!countValue.isNumericValue()) {
      currentState =
          currentState.withInvalidWrite(
              "Symbolic count (second argument) for memset() function call not supported.",
              countValue);
      // TODO: we need to change the value behind bufferAddress to unknown as well!
      return ValueAndSMGState.ofUnknownValue(currentState);
    }

    long count = countValue.asNumericValue().longValue();

    // If the char value is unknown, we use a new symbolic value!
    Value bufferMemoryAddress = bufferAddressAndOffset.getMemoryAddress();
    BigInteger bufferOffsetInBits =
        bufferAddressAndOffset.getOffset().asNumericValue().bigIntegerValue();

    BigInteger sizeOfCharInBits = BigInteger.valueOf(machineModel.getSizeofCharInBits());

    // This precondition has to hold for the get(0) getters
    Preconditions.checkArgument(
        !currentState.getMemoryModel().pointsToZeroPlus(bufferMemoryAddress));
    if (charValue.isNumericValue()
        && charValue.asNumericValue().bigIntegerValue().equals(BigInteger.ZERO)) {
      // Create one large edge for 0 (the SMG cuts 0 edges on its own)
      currentState =
          currentState
              .writeValueTo(
                  bufferMemoryAddress,
                  bufferOffsetInBits,
                  sizeOfCharInBits.multiply(BigInteger.valueOf(count)),
                  charValue,
                  CNumericTypes.CHAR,
                  cfaEdge)
              .get(0);
    } else {
      // Write each char on its own
      for (int c = 0; c < count; c++) {
        currentState =
            currentState
                .writeValueTo(
                    bufferMemoryAddress,
                    bufferOffsetInBits.add(BigInteger.valueOf(c).multiply(sizeOfCharInBits)),
                    sizeOfCharInBits,
                    charValue,
                    CNumericTypes.CHAR,
                    cfaEdge)
                .get(0);
      }
    }
    // Since this returns the pointer of the buffer we check the offset of the AddressExpression, if
    // its 0 we can return the known pointer, else we create a new one.
    if (bufferOffsetInBits.compareTo(BigInteger.ZERO) == 0) {
      return ValueAndSMGState.of(bufferMemoryAddress, currentState);
    } else {
      ValueAndSMGState newPointerAndState =
          evaluator
              .findOrcreateNewPointer(bufferMemoryAddress, bufferOffsetInBits, currentState)
              .get(0);
      return ValueAndSMGState.of(newPointerAndState.getValue(), newPointerAndState.getState());
    }
  }

  // TODO all functions are external in C, do we even need this?
  @SuppressWarnings("unused")
  private List<ValueAndSMGState> evaluateExternalAllocation(
      CFunctionCallExpression pFunctionCall, SMGState pState) throws SMGException {
    throw new SMGException("evaluateExternalAllocation in SMGBUILTINS");
    /*
    String functionName = pFunctionCall.getFunctionNameExpression().toASTString();

    List<ValueAndSMGState> result = new ArrayList<>();


    String allocation_label =
        functionName
            + "_PARAMETERS_"
            + pFunctionCall.getParameterExpressions().size()
            + "_RETURN_"
            + pFunctionCall.getExpressionType();


    result.add(ValueAndSMGState.of(pState, pState.addExternalAllocation(allocation_label)));
    addExternalAllocation would create a pointer (points to edge) to a memory region created with
    size: options.getExternalAllocationSize(), offset: options.getExternalAllocationSize() / 2, as external
    and i don't see why

    return result;
    */
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
   *     stack of the returned {@link SMGState}s or a error info set in case of errors. The Value is
   *     either a pointer to the valid stack memory allocated or unknown. The pointer is not a
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

      if (!argumentAndState.getValue().isNumericValue()) {
        String infoMsg =
            "Could not determine a concrete size for a memory allocation function: "
                + functionCall.getFunctionNameExpression();
        if (options.isAbortOnNonConcreteMemorySize()) {
          throw new UnrecognizedCodeException(infoMsg, cfaEdge);
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
   *     and may have a error state with the error.
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  private List<ValueAndSMGState> evaluateAlloca(
      SMGState pState, Value pSizeValue, CType type, @SuppressWarnings("unused") CFAEdge cfaEdge)
      throws CPATransferException {
    // Since the size comes from getAllocateFunctionParameter we know that unknown values may be
    // replaced by guesses if enabled

    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    SMGState currentState = pState;
    if (!pSizeValue.isNumericValue()) {
      resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
    } else {
      String allocationLabel = "_ALLOCA_ID_" + U_ID_GENERATOR.getFreshId();
      ValueAndSMGState addressValueAndState =
          evaluator.createStackAllocation(
              allocationLabel,
              pSizeValue.asNumericValue().bigIntegerValue().multiply(BigInteger.valueOf(8)),
              type,
              pState);

      currentState = addressValueAndState.getState();

      resultBuilder.add(ValueAndSMGState.of(addressValueAndState.getValue(), currentState));
    }
    return resultBuilder.build();
  }

  /**
   * Evaluate the use of the C Free() method. Should recieve 1 argument that should be a address to
   * memory not yet freed (still valid). If the Value is no address, a already freed address or any
   * other form of invalid address (i.e. = 0) the return state will have a error info attached.
   *
   * @param pFunctionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return a list of {@link SMGState}s for the results of the free() invokation. These might
   *     include error states!
   * @throws CPATransferException if a critical error is encountered that the SMGCPA can't handle.
   */
  public final List<SMGState> evaluateFree(
      CFunctionCallExpression pFunctionCall, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {

    if (pFunctionCall.getParameterExpressions().size() != 1) {
      throw new UnrecognizedCodeException(
          "The function free() needs exactly 1 paramter", cfaEdge, pFunctionCall);
    }

    ImmutableList.Builder<SMGState> resultBuilder = ImmutableList.builder();
    for (ValueAndSMGState addressAndState :
        getFunctionParameterValue(0, pFunctionCall, pState, cfaEdge)) {
      Value maybeAddressValue = addressAndState.getValue();
      SMGState currentState = addressAndState.getState();

      resultBuilder.addAll(currentState.free(maybeAddressValue, pFunctionCall, cfaEdge));
    }

    return resultBuilder.build();
  }

  /**
   * @param functionCall the {@link CFunctionCallExpression} that lead to this function call.
   * @param pState current {@link SMGState}.
   * @param cfaEdge for logging/debugging.
   * @return a list of {@link ValueAndSMGState} with either the targetAddress pointer expression and
   *     the state in which the copy was successfull, or a unknown value and maybe a error state if
   *     something went wrong, i.e. invalid/read/write.
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
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        } else if (!(targetAddress instanceof AddressExpression)) {
          // The value can be unknown
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }
        AddressExpression targetAddressExpr = (AddressExpression) targetAddress;
        if (!targetAddressExpr.getOffset().isNumericValue()) {
          // Write the target region to unknown
          // TODO:
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }

        List<ValueAndSMGState> newTargetPointerAndStates =
            evaluator.findOrcreateNewPointer(
                targetAddressExpr.getMemoryAddress(),
                targetAddressExpr.getOffset().asNumericValue().bigIntegerValue(),
                currentState);
        for (ValueAndSMGState newTargetPointerAndState : newTargetPointerAndStates) {
          for (SMGStateAndOptionalSMGObjectAndOffset newTargetObjAndState :
              newTargetPointerAndState
                  .getState()
                  .dereferencePointer(newTargetPointerAndState.getValue())) {
            if (!newTargetObjAndState.hasSMGObjectAndOffset()
                || !newTargetObjAndState.getOffsetForObject().isNumericValue()) {
              continue;
            }

            evaluateMemcpySecondStep(
                newTargetObjAndState.getSMGObject(),
                newTargetObjAndState.getOffsetForObject().asNumericValue().bigIntegerValue(),
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
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }
        SMGObject targetObj = destAndState.getSMGObject();
        Value targetOffset = destAndState.getOffsetForObject();

        if (!targetOffset.isNumericValue()) {
          // Write the target region to unknown
          // TODO: Write the target region to unknown
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }
        evaluateMemcpySecondStep(
            targetObj,
            targetOffset.asNumericValue().bigIntegerValue(),
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
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        } else if (!(sourceAddress instanceof AddressExpression)) {
          // The value can be unknown
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }
        AddressExpression sourceAddressExpr = (AddressExpression) sourceAddress;
        if (!sourceAddressExpr.getOffset().isNumericValue()) {
          // Write the target region to unknown
          // TODO:
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }

        List<ValueAndSMGState> newSourcePointerAndStates =
            evaluator.findOrcreateNewPointer(
                sourceAddressExpr.getMemoryAddress(),
                sourceAddressExpr.getOffset().asNumericValue().bigIntegerValue(),
                currentState);
        for (ValueAndSMGState newSourcePointerAndState : newSourcePointerAndStates) {
          for (SMGStateAndOptionalSMGObjectAndOffset newSourceObjAndState :
              newSourcePointerAndState
                  .getState()
                  .dereferencePointer(newSourcePointerAndState.getValue())) {
            if (!newSourceObjAndState.hasSMGObjectAndOffset()
                || !newSourceObjAndState.getOffsetForObject().isNumericValue()) {
              continue;
            }
            evaluateMemcpyLastStep(
                targetObj,
                targetOffset,
                newSourceObjAndState.getSMGObject(),
                newSourceObjAndState.getOffsetForObject().asNumericValue().bigIntegerValue(),
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
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }
        SMGObject sourceObj = sourceAndState.getSMGObject();
        Value sourceOffset = sourceAndState.getOffsetForObject();

        if (!sourceOffset.isNumericValue()) {
          // Unknown offset
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }

        evaluateMemcpyLastStep(
            targetObj,
            targetOffset,
            sourceObj,
            sourceOffset.asNumericValue().bigIntegerValue(),
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

      if (!sizeValue.isNumericValue()) {
        // TODO: log instead of error? This is a limitation of the analysis that is not a
        // critical C problem.
        resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
        continue;
      }

      resultBuilder.add(
          evaluateMemcpy(
              currentState, targetObj, targetOffset, sourceObj, sourceOffset, sizeValue));
    }
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
      Value numOfBytesToCopy) {

    Preconditions.checkArgument(numOfBytesToCopy instanceof NumericValue);
    long numOfBytes = numOfBytesToCopy.asNumericValue().bigIntegerValue().longValue();
    if (numOfBytes < 0) {
      // the argument is unsigned, so we have to transform it into a postive. On most 64bit systems
      // it's an unsigned long (C99 standard)
      numOfBytes =
          Integer.toUnsignedLong(
              numOfBytesToCopy.asNumericValue().bigIntegerValue().intValueExact());
    }

    BigInteger sizeToCopyInBits =
        BigInteger.valueOf(numOfBytes)
            .multiply(BigInteger.valueOf(machineModel.getSizeofCharInBits()));

    // There can be deref errors if the size is to large
    if (sourceAddress.getSize().compareTo(sourceOffset.add(sizeToCopyInBits)) < 0) {
      return ValueAndSMGState.ofUnknownValue(pState.withInvalidRead(sourceAddress));
    } else if (targetAddress.getSize().compareTo(targetOffset.add(sizeToCopyInBits)) < 0) {
      return ValueAndSMGState.ofUnknownValue(pState.withInvalidWrite(targetAddress));
    }

    SMGState copyResultState =
        pState.copySMGObjectContentToSMGObject(
            sourceAddress, sourceOffset, targetAddress, targetOffset, sizeToCopyInBits);

    ValueAndSMGState newPointersAndStates =
        evaluator.searchOrCreatePointer(targetAddress, targetOffset, copyResultState);

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

      Value firstAddress = firstValueAndSMGState.getValue();
      // If the Value is no AddressExpression we can't work with it
      // The buffer is type * and has to be a AddressExpression with a not unknown value and a
      // concrete offset to be used correctly
      if (!(firstAddress instanceof AddressExpression)) {
        // The value can be unknown
        resultBuilder.add(ValueAndSMGState.ofUnknownValue(firstValueAndSMGState.getState()));
        continue;
      }
      AddressExpression firstAddressExpr = (AddressExpression) firstAddress;
      if (!firstAddressExpr.getOffset().isNumericValue()) {
        // Write the target region to unknown
        resultBuilder.add(ValueAndSMGState.ofUnknownValue(firstValueAndSMGState.getState()));
        continue;
      }

      for (ValueAndSMGState secondValueAndSMGState :
          getFunctionParameterValue(
              STRCMP_SECOND_PARAMETER, pFunctionCall, firstValueAndSMGState.getState(), pCfaEdge)) {
        Value secondAddress = secondValueAndSMGState.getValue();
        // If the Value is no AddressExpression we can't work with it
        // The buffer is type * and has to be a AddressExpression with a not unknown value and a
        // concrete offset to be used correctly
        if (!SMGCPAExpressionEvaluator.valueIsAddressExprOrVariableOffset(secondAddress)) {
          // Unknown addresses happen only of we don't have a memory associated
          // TODO: decide what to do here and when this happens
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(secondValueAndSMGState.getState()));
          continue;
        } else if (!(secondAddress instanceof AddressExpression)) {
          // The value can be unknown
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(secondValueAndSMGState.getState()));
          continue;
        }
        AddressExpression secondAddressExpr = (AddressExpression) secondAddress;
        if (!secondAddressExpr.getOffset().isNumericValue()) {
          // Write the target region to unknown
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(secondValueAndSMGState.getState()));
          continue;
        }

        // iterate over all chars and compare them for 'int strcmp(const char * firstStringPointer,
        // const char *secondStringPointer)' (c99 * 7.21.4.2)
        resultBuilder.add(
            evaluator.stringCompare(
                firstAddressExpr.getMemoryAddress(),
                firstAddressExpr.getOffset().asNumericValue().bigIntegerValue(),
                secondAddressExpr.getMemoryAddress(),
                secondAddressExpr.getOffset().asNumericValue().bigIntegerValue(),
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
        functionCall.getParameterExpressions().get(0).accept(valueVisitor)) {
      for (ValueAndSMGState argumentTwoAndState :
          getAllocateFunctionParameter(1, functionCall, argumentOneAndState.getState(), cfaEdge)) {

        // First arg is the ptr to the existing memory
        // Second arg is new memory size in bytes
        if (!argumentTwoAndState.getValue().isNumericValue()) {
          String infoMsg =
              "Could not determine a concrete size for a memory allocation function: "
                  + functionCall.getFunctionNameExpression();
          if (options.isAbortOnNonConcreteMemorySize()) {
            throw new UnrecognizedCodeException(infoMsg, cfaEdge);
          } else {
            logger.log(Level.INFO, infoMsg + ", in " + cfaEdge);
          }
        }

        resultBuilder.addAll(
            evaluateReallocWParameters(
                argumentTwoAndState.getState(),
                argumentOneAndState.getValue(),
                argumentTwoAndState.getValue(),
                SMGCPAExpressionEvaluator.getCanonicalType(functionCall.getExpressionType()),
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
   * @param pCanonicalReturnType canonical return type (we know its void*)
   * @param pCfaEdge current cfa edge
   * @return list of points to new memory and its states
   */
  private Collection<ValueAndSMGState> evaluateReallocWParameters(
      SMGState pState,
      Value pPtrValue,
      Value pSizeValue,
      CType pCanonicalReturnType,
      CFAEdge pCfaEdge,
      CFunctionCallExpression functionCall)
      throws SMGException, SMGSolverException {

    if (pPtrValue instanceof AddressExpression ptrAddrExpr) {
      if (!ptrAddrExpr.getOffset().isNumericValue()
          || !ptrAddrExpr.getOffset().asNumericValue().bigIntegerValue().equals(BigInteger.ZERO)) {
        throw new SMGException(
            "Realloc with pointers not pointing to their original offset not supported yet. "
                + pCfaEdge);
      }
      pPtrValue = ptrAddrExpr.getMemoryAddress();
    }

    if (!pState.getMemoryModel().isPointer(pPtrValue)) {
      // undefined beh
      return ImmutableList.of(ValueAndSMGState.of(pPtrValue, pState));
    } else if (!pSizeValue.isNumericValue()) {
      return ImmutableList.of(ValueAndSMGState.of(pPtrValue, pState));
    }

    SMGState currentState = pState;
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    BigInteger sizeInBits =
        pSizeValue.asNumericValue().bigIntegerValue().multiply(BigInteger.valueOf(8));
    // Handle (realloc(0, size) -> just malloc
    if (pPtrValue.isNumericValue()
        && pPtrValue.asNumericValue().bigIntegerValue().equals(BigInteger.ZERO)) {
      return handleConfigurableMemoryAllocation(functionCall, currentState, sizeInBits, pCfaEdge);
    }

    // Handle realloc(ptr, 0) (before C23), (C23 its just undefined beh)
    if (pSizeValue.isNumericValue() && sizeInBits.equals(BigInteger.ZERO)) {
      resultBuilder = ImmutableList.builder();
      for (SMGState freedState : currentState.free(pPtrValue, functionCall, pCfaEdge)) {
        resultBuilder.add(handleAllocZero(freedState));
      }
      return resultBuilder.build();
    }

    for (SMGStateAndOptionalSMGObjectAndOffset oldObj :
        currentState.dereferencePointer(pPtrValue)) {
      currentState = oldObj.getSMGState();
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
      // The copy is always the lesser size of the 2
      BigInteger oldSize =
          oldObj
              .getSMGObject()
              .getSize()
              .subtract(oldObj.getOffsetForObject().asNumericValue().bigIntegerValue());
      BigInteger copySizeInBits = sizeInBits;
      if (oldSize.compareTo(copySizeInBits) < 0) {
        copySizeInBits = oldSize;
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

  public Collection<SMGState> checkAllParametersForValidity(
      CFunctionCallExpression cFCExpression, SMGState pState, CFAEdge pCfaEdge)
      throws CPATransferException {
    return checkAllParametersForValidity(pState, pCfaEdge, cFCExpression);
  }
}
