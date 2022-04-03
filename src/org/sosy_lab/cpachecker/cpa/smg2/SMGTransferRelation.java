// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGTransferRelation
    extends ForwardingTransferRelation<Collection<SMGState>, SMGState, SMGPrecision> {

  private final SMGOptions options;

  private final MachineModel machineModel;

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  private final LogManagerWithoutDuplicates logger;

  public SMGTransferRelation(
      LogManager pLogger,
      SMGOptions pOptions,
      MachineModel pMachineModel,
      ShutdownNotifier pShutdownNotifier) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    options = pOptions;
    machineModel = pMachineModel;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  protected Collection<SMGState> postProcessing(Collection<SMGState> pSuccessors, CFAEdge edge) {
    Set<CSimpleDeclaration> outOfScopeVars = edge.getSuccessor().getOutOfScopeVariables();
    return transformedImmutableSetCopy(
        pSuccessors,
        successorState -> {
          SMGState prunedState = successorState.copyAndPruneOutOfScopeVariables(outOfScopeVars);
          return checkAndSetErrorRelation(prunedState);
        });
  }

  @SuppressWarnings("unused")
  private SMGState checkAndSetErrorRelation(SMGState pPrunedState) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Set<SMGState> handleBlankEdge(BlankEdge cfaEdge) throws CPATransferException {
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      if (isEntryFunction(cfaEdge)) {
        return handleReturnEntryFunction(Collections.singleton(state));
      }
    }

    return Collections.singleton(state);
  }

  private Set<SMGState> handleReturnEntryFunction(Collection<SMGState> pSuccessors) {
    return pSuccessors.stream()
        .map(
            pState -> {
              if (options.isHandleNonFreedMemoryInMainAsMemLeak()) {
                pState = pState.dropStackFrame();
              }
              return pState.copyAndPruneUnreachable();
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  private boolean isEntryFunction(CFAEdge pCfaEdge) {
    return pCfaEdge.getSuccessor().getNumLeavingEdges() == 0;
  }

  /* (non-Javadoc)
   * Returns a collection of SMGStates that are the successors of the handled edge.
   * This method will
   * If there is no returned data, the current state is the successor state.
   * If there is returned data we assign the returned statement to the field of the state,
   * returning the successor states.
   * This assignment is further explained in its method.
   * In the case that this is an entry function, there is no function return edge,
   * meaning we have to check for memory leaks!
   * This means that every successor-state has to be checked for memory that is not freed
   * (if the option for that is enabled) and then the unreachables need to be pruned.
   * Similar to this, we need to handle leaks at any program exit point (abort, etc.).
   * TODO: how to do this?
   * Is it sufficient to check the successor states with the exception of the returned stuff?
   */
  @Override
  protected Collection<SMGState> handleReturnStatementEdge(CReturnStatementEdge returnEdge)
      throws CPATransferException {
    // First get the (SMG)Object that is returned if possible
    Optional<SMGObject> returnObjectOptional =
        state.getMemoryModel().getReturnObjectForCurrentStackFrame();
    Collection<SMGState> successors = Collections.singleton(state);
    // If there is an (SMG)Object returned, assign it to the successor state
    if (returnObjectOptional.isPresent()) {
      successors = assignStatementToField(state, returnObjectOptional.orElseThrow(), returnEdge);
    }

    // Handle entry function return (check for mem leaks)
    if (isEntryFunction(returnEdge)) {
      return handleReturnEntryFunction(successors);
    }
    return successors;
  }

  /**
   * Evaluates the value of the given expression (i.e. a return statement) and assigns the value to
   * given state at the given region.
   *
   * @param pState - The current {@link SMGState}.
   * @param pRegion - The {@link SMGObject} that is the return object on the heap of the function
   *     just returned.
   * @param pReturnEdge - The {@link CReturnStatementEdge} that models the return of the function
   *     that just returned.
   * @return A collection of {@link SMGState}s that represents the successor states.
   * @throws CPATransferException is thrown if TODO:?
   */
  @SuppressWarnings("unused")
  private Collection<SMGState> assignStatementToField(
      SMGState pState, SMGObject pRegion, CReturnStatementEdge pReturnEdge)
      throws CPATransferException {
    // If there is no concrete value use 0 as that is the C default value
    CExpression returnExp = pReturnEdge.getExpression().orElse(CIntegerLiteralExpression.ZERO);
    SMGCPAValueExpressionEvaluator valueExpressionVisitor =
        new SMGCPAValueExpressionEvaluator(machineModel, logger);
    // TODO: the rest, because this makes no sense
    return valueExpressionVisitor.evaluateValues(pState, pReturnEdge, returnExp);
  }

  @Override
  protected Collection<SMGState> handleFunctionReturnEdge(
      CFunctionReturnEdge functionReturnEdge,
      CFunctionSummaryEdge fnkCall,
      CFunctionCall summaryExpr,
      String callerFunctionName)
      throws CPATransferException {
    return null;
  }

  @Override
  protected Collection<SMGState> handleFunctionCallEdge(
      CFunctionCallEdge callEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> paramDecl,
      String calledFunctionName)
      throws CPATransferException {

    return null;
  }

  @Override
  protected void setInfo(
      AbstractState abstractState, Precision abstractPrecision, CFAEdge cfaEdge) {
    super.setInfo(abstractState, abstractPrecision, cfaEdge);
  }

  @Override
  protected Collection<SMGState> handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException, InterruptedException {
    // Assumptions are essentially all value analysis in nature. We get the values from the SMGs
    // though.
    // Assumptions are for example all comparisons like ==, !=, <.... and should always be a
    // CBinaryExpression
    return null;
  }

  @Override
  protected Collection<SMGState> handleStatementEdge(CStatementEdge pCfaEdge, CStatement cStmt)
      throws CPATransferException {

    return null;
  }

  @Override
  protected List<SMGState> handleDeclarationEdge(CDeclarationEdge edge, CDeclaration cDecl)
      throws CPATransferException {
    if (cDecl instanceof CFunctionDeclaration) {
      // TODO:
    } else if (cDecl instanceof CComplexTypeDeclaration) {
      // TODO:
    } else if (cDecl instanceof CTypeDefDeclaration) {
      // TODO:
    } else if (cDecl instanceof CVariableDeclaration) {
      return handleVariableDeclaration(state, (CVariableDeclaration) cDecl, edge);
    }
    // Fallthrough
    // TODO: log that declaration failed
    return ImmutableList.of(state);
  }

  /**
   * Creates (or re-uses) a variable for the name given. The variable is either on the stack, global
   * or externally allocated.
   *
   * @param pState current {@link SMGState}
   * @param pVarDecl declaration of the variable declared.
   * @param pEdge current CFAEdge
   * @return a new state with the variable declared and initialized.
   * @throws CPATransferException TODO
   */
  private List<SMGState> handleVariableDeclaration(
      SMGState pState, CVariableDeclaration pVarDecl, CDeclarationEdge pEdge)
      throws CPATransferException {
    // SMGCPAValueExpressionEvaluator evaluator = new SMGCPAValueExpressionEvaluator(machineModel,
    // logger);
    String varName = pVarDecl.getName();
    CType cType = SMGCPAValueExpressionEvaluator.getCanonicalType(pVarDecl);
    boolean isExtern = pVarDecl.getCStorageClass().equals(CStorageClass.EXTERN);

    if (cType.isIncomplete() && cType instanceof CElaboratedType) {
      // for incomplete types, we do not add variables.
      // we are not allowed to read or write them, dereferencing is possible.
      // example: "struct X; extern struct X var; void main() { }"
      // TODO currently we assume that only CElaboratedTypes are unimportant when incomplete.
      return ImmutableList.of(pState);
    }

    /*
     *  If the variable exists it does so because of loops etc.
     *  Invalid declarations should be already caught by the parser.
     */
    SMGState newState = pState;
    if (!pState.checkVariableExists(pState, varName)
        && (!isExtern || options.getAllocateExternalVariables())) {
      SMGCPAValueExpressionEvaluator evaluator =
          new SMGCPAValueExpressionEvaluator(machineModel, logger);
      int typeSize = evaluator.getBitSizeof(pState, cType).intValueExact();

      // Handle incomplete type of extern variables as externally allocated
      if (options.isHandleIncompleteExternalVariableAsExternalAllocation()
          && cType.isIncomplete()
          && isExtern) {
        typeSize = options.getExternalAllocationSize();
      }
      if (pVarDecl.isGlobal()) {
        newState = pState.copyAndAddGlobalVariable(typeSize, varName);
      } else {
        newState = pState.copyAndAddLocalVariable(typeSize, varName);
      }
    }

    return handleInitializerForDeclaration(newState, varName, pVarDecl, cType, pEdge);
  }

  /**
   * This method expects that there is a variable (global or otherwise) existing under the name
   * entered with the corect size allocated. This also expects that the type is correct. This method
   * will write globals to 0 and handle futher initialization of variables if necessary.
   *
   * @param pState current {@link SMGState}.
   * @param pVarName name of the variable to be initialized. This var should be present on the
   *     memory model with the correct size.
   * @param pVarDecl {@link CVariableDeclaration} for the variable.
   * @param cType {@link CType} of the variable.
   * @param pEdge {@link CDeclarationEdge} for the declaration.
   * @return a list of states with the variable initialized.
   * @throws CPATransferException if something goes wrong
   */
  private List<SMGState> handleInitializerForDeclaration(
      SMGState pState,
      String pVarName,
      CVariableDeclaration pVarDecl,
      CType cType,
      CDeclarationEdge pEdge)
      throws CPATransferException {
    CInitializer newInitializer = pVarDecl.getInitializer();
    SMGState currentState = pState;

    if (pVarDecl.isGlobal()) {
      // Global vars are always initialized to 0
      // Don't nullify extern variables
      if (pVarDecl.getCStorageClass().equals(CStorageClass.EXTERN)) {
        if (options.isHandleIncompleteExternalVariableAsExternalAllocation()) {
          currentState = currentState.setExternallyAllocatedFlag(pVarName);
        }
      } else {
        // Global variables (but not extern) without initializer are nullified in C
        currentState = currentState.writeToStackOrGlobalVariableToZero(pVarName);
      }
    }

    if (newInitializer != null) {
      return handleInitializer(
          currentState, pVarDecl, pEdge, pVarName, BigInteger.ZERO, cType, newInitializer);
    }

    return ImmutableList.of(currentState);
  }

  /*
   * Handles initializing of just declared variables. I.e. int bla = 5; This expects global vars to be already written to 0.
   */
  private List<SMGState> handleInitializer(
      SMGState pNewState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CType pLValueType,
      CInitializer pInitializer)
      throws CPATransferException {

    if (pInitializer instanceof CInitializerExpression) {
      CExpression expression = ((CInitializerExpression) pInitializer).getExpression();
      // string literal handling
      if (expression instanceof CStringLiteralExpression) {
        return handleStringInitializer(
            pNewState,
            pVarDecl,
            pEdge,
            variableName,
            pOffset,
            pLValueType,
            pInitializer.getFileLocation(),
            (CStringLiteralExpression) expression);
      } else if (expression instanceof CCastExpression) {
        // handle casting on initialization like 'char *str = (char *)"string";'
        return handleCastInitializer(
            pNewState,
            pVarDecl,
            pEdge,
            variableName,
            pOffset,
            pLValueType,
            pInitializer.getFileLocation(),
            (CCastExpression) expression);
      } else {
        return writeCExpressionToLocalOrGlobalVariable(
            pNewState, pEdge, variableName, pOffset, pLValueType, expression);
      }
    } else if (pInitializer instanceof CInitializerList) {
      CInitializerList pNewInitializer = ((CInitializerList) pInitializer);
      CType realCType = pLValueType.getCanonicalType();

      if (realCType instanceof CArrayType) {
        CArrayType arrayType = (CArrayType) realCType;
        return handleInitializerList(
            pNewState, pVarDecl, pEdge, variableName, pOffset, arrayType, pNewInitializer);

      } else if (realCType instanceof CCompositeType) {
        CCompositeType structType = (CCompositeType) realCType;
        return handleInitializerList(
            pNewState, pVarDecl, pEdge, variableName, pOffset, structType, pNewInitializer);
      }

      // Type cannot be resolved
      logger.log(
          Level.INFO,
          () ->
              String.format(
                  "Type %s cannot be resolved sufficiently to handle initializer %s",
                  realCType.toASTString(""), pNewInitializer));
      return ImmutableList.of(pNewState);

    } else if (pInitializer instanceof CDesignatedInitializer) {
      throw new AssertionError(
          "Error in handling initializer, designated Initializer "
              + pInitializer.toASTString()
              + " should not appear at this point.");

    } else {
      throw new UnrecognizedCodeException("Did not recognize Initializer", pInitializer);
    }
  }

  /*
   * Handles castings when initializing variables. I.e. = (char) 55;
   */
  private List<SMGState> handleCastInitializer(
      SMGState pNewState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CType pLValueType,
      FileLocation pFileLocation,
      CCastExpression pExpression)
      throws CPATransferException {
    CExpression expression = pExpression.getOperand();
    if (expression instanceof CStringLiteralExpression) {
      return handleStringInitializer(
          pNewState,
          pVarDecl,
          pEdge,
          variableName,
          pOffset,
          pLValueType,
          pFileLocation,
          (CStringLiteralExpression) expression);
    } else if (expression instanceof CCastExpression) {
      return handleCastInitializer(
          pNewState,
          pVarDecl,
          pEdge,
          variableName,
          pOffset,
          pLValueType,
          pFileLocation,
          (CCastExpression) expression);
    } else {
      return writeCExpressionToLocalOrGlobalVariable(
          pNewState, pEdge, variableName, pOffset, pLValueType, expression);
    }
  }

  /*
   * Handles and inits, to the variable given, the given CInitializerList initializers. In this case composite types like structs and unions.
   */
  private List<SMGState> handleInitializerList(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CCompositeType pLValueType,
      CInitializerList pNewInitializer)
      throws CPATransferException {

    int listCounter = 0;

    List<CCompositeType.CCompositeTypeMemberDeclaration> memberTypes = pLValueType.getMembers();
    // Member -> offset map
    Map<CCompositeType.CCompositeTypeMemberDeclaration, BigInteger> offsetAndPosition =
        machineModel.getAllFieldOffsetsInBits(pLValueType);

    ImmutableList.Builder<SMGState> finalStates = ImmutableList.builder();
    SMGState currentState = pState;
    // Just to be sure, should never trigger
    Preconditions.checkArgument(pNewInitializer.getInitializers().size() == memberTypes.size());
    for (CInitializer initializer : pNewInitializer.getInitializers()) {
      // TODO: this has to be checked with a test!!!!
      if (initializer instanceof CDesignatedInitializer) {
        initializer = ((CDesignatedInitializer) initializer).getRightHandSide();
      }

      CType memberType = memberTypes.get(listCounter).getType();
      // The offset is the base offset given + the current offset
      BigInteger offset = pOffset.add(offsetAndPosition.get(memberTypes.get(listCounter)));

      List<SMGState> newStates =
          handleInitializer(
              currentState, pVarDecl, pEdge, variableName, offset, memberType, initializer);

      currentState = newStates.get(newStates.size());
      finalStates.addAll(newStates);
      listCounter++;
    }

    return finalStates.build();
  }

  /*
   * Handles and inits, to the variable given, the given CInitializerList initializers. In this case arrays.
   */
  private List<SMGState> handleInitializerList(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CArrayType pLValueType,
      CInitializerList pNewInitializer)
      throws CPATransferException {

    SMGCPAValueExpressionEvaluator evaluator =
        new SMGCPAValueExpressionEvaluator(machineModel, logger);
    CType memberType = SMGCPAValueExpressionEvaluator.getCanonicalType(pLValueType.getType());
    BigInteger memberTypeSize = evaluator.getBitSizeof(pState, memberType);

    ImmutableList.Builder<SMGState> finalStates = ImmutableList.builder();
    SMGState currentState = pState;

    for (CInitializer initializer : pNewInitializer.getInitializers()) {
      // TODO: this has to be checked with a test!!!!
      if (initializer instanceof CDesignatedInitializer) {
        initializer = ((CDesignatedInitializer) initializer).getRightHandSide();
      }

      // The offset is the base offset given + the current offset
      BigInteger offset = pOffset.add(memberTypeSize);

      List<SMGState> newStates =
          handleInitializer(
              currentState, pVarDecl, pEdge, variableName, offset, memberType, initializer);

      currentState = newStates.get(newStates.size());
      finalStates.addAll(newStates);
    }

    return finalStates.build();
  }

  /*
   * Handle string literal expression initializer:
   * if a string initializer is used with a pointer:
   * - create a new memory for string expression (temporary array)
   * - call #handleInitializer for new region and string expression
   * - create pointer for new region and initialize pointer with it
   * else
   *  - create char array from string and call list init for given memory
   */
  private List<SMGState> handleStringInitializer(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CType pCurrentExpressionType,
      FileLocation pFileLocation,
      CStringLiteralExpression pExpression)
      throws CPATransferException {

    // If this is a pointer (i.e. char * name = "iAmAString";) we actually have not yet initialized
    // the memory for the String, just the pointer. So we need to create new memory for the String,
    // write the String into it, make a pointer to the beginning and save that in the char *.
    if (pCurrentExpressionType instanceof CPointerType) {
      SMGCPAValueExpressionEvaluator evaluator =
          new SMGCPAValueExpressionEvaluator(machineModel, logger);
      // create a new memory region for the string (right hand side)
      CType stringArrayType = pExpression.transformTypeToArrayType();
      String stringVarName = "_" + pExpression.getContentString() + "_STRING_LITERAL";
      // If the var exists we change the name and create a new one
      // (Don't reuse an old variable! They might be different than the new one!)
      int num = 0;
      while (pState.isGlobalVariablePresent(stringVarName + num)) {
        num++;
      }
      stringVarName += num;

      BigInteger sizeOfString = evaluator.getBitSizeof(pState, stringArrayType);
      SMGState currentState = pState.copyAndAddGlobalVariable(sizeOfString, stringVarName);
      List<SMGState> initedStates =
          transformStringToArrayAndInitialize(
              currentState,
              pVarDecl,
              pEdge,
              stringVarName,
              BigInteger.ZERO,
              pFileLocation,
              pExpression);

      ImmutableList.Builder<SMGState> stateBuilder = ImmutableList.builder();
      for (SMGState initedState : initedStates) {
        // Now create a pointer to the String memory and save that in the original variable
        ValueAndSMGState addressAndState =
            evaluator.createAddressForLocalOrGlobalVariable(stringVarName, initedState, pEdge);
        SMGState addressState = addressAndState.getState();
        stateBuilder.add(
            addressState.writeToStackOrGlobalVariable(
                variableName,
                pOffset,
                evaluator.getBitSizeof(addressState, pCurrentExpressionType),
                addressAndState.getValue()));
      }
      return stateBuilder.build();
    }

    return transformStringToArrayAndInitialize(
        pState, pVarDecl, pEdge, variableName, pOffset, pFileLocation, pExpression);
  }

  private List<SMGState> transformStringToArrayAndInitialize(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      FileLocation pFileLocation,
      CStringLiteralExpression pExpression)
      throws CPATransferException {
    // Create a char array from string and call list init
    ImmutableList.Builder<CInitializer> charArrayInitialziersBuilder = ImmutableList.builder();
    CArrayType arrayType = pExpression.transformTypeToArrayType();
    for (CCharLiteralExpression charLiteralExp : pExpression.expandStringLiteral(arrayType)) {
      charArrayInitialziersBuilder.add(new CInitializerExpression(pFileLocation, charLiteralExp));
    }
    return handleInitializerList(
        pState,
        pVarDecl,
        pEdge,
        variableName,
        pOffset,
        arrayType,
        new CInitializerList(pFileLocation, charArrayInitialziersBuilder.build()));
  }

  /**
   * (non-Javadoc)
   *
   * @see
   *     org.sosy_lab.cpachecker.core.interfaces.TransferRelation#strengthen(org.sosy_lab.cpachecker.core.interfaces.AbstractState,
   *     java.lang.Iterable, org.sosy_lab.cpachecker.cfa.model.CFAEdge,
   *     org.sosy_lab.cpachecker.core.interfaces.Precision)
   */
  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element,
      Iterable<AbstractState> elements,
      CFAEdge cfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    return null;
  }

  /*
   * Writes valueToWrite (Some CExpression that does not lead to multiple values) into the
   * variable with the name given at the offset given. The type given is used for the size.
   */
  private List<SMGState> writeCExpressionToLocalOrGlobalVariable(
      SMGState pState,
      CFAEdge cfaEdge,
      String variableName,
      BigInteger pOffsetInBits,
      CType pLFieldType,
      CRightHandSide exprToWrite)
      throws CPATransferException {
    // This can't handle types leading to multiple values!
    Preconditions.checkArgument(!(exprToWrite instanceof CStringLiteralExpression));

    SMGCPAValueExpressionEvaluator evaluator =
        new SMGCPAValueExpressionEvaluator(machineModel, logger);
    BigInteger sizeOfType = evaluator.getBitSizeof(pState, pLFieldType);
    SMGCPAValueVisitor valueVisitor = new SMGCPAValueVisitor(evaluator, pState, cfaEdge, logger);
    ImmutableList.Builder<SMGState> resultStatesBuilder = ImmutableList.builder();
    for (ValueAndSMGState valueAndState : exprToWrite.accept(valueVisitor)) {
      Value valueToAssign = valueAndState.getValue();
      SMGState currentState = valueAndState.getState();

      resultStatesBuilder.add(
          currentState.writeToStackOrGlobalVariable(
              variableName, pOffsetInBits, sizeOfType, valueToAssign));
    }
    return resultStatesBuilder.build();
  }

  /*
   * Handles any form of assignments. Takes the C/CFA arguments, transforms them into SMG arguments (reads SMG, gets SMG representatives of C values/objects or creates them if they are unknown) and then assigns them to the SMG counterparts, updating the state in the process.
   */
  @SuppressWarnings("unused")
  private List<SMGState> handleAssignment(
      SMGState pState, CFAEdge cfaEdge, CExpression lValue, CRightHandSide rValue)
      throws CPATransferException {
    /* old/ just reference
    List<SMGState> result = new ArrayList<>(4);
    LValueAssignmentVisitor visitor =
        expressionEvaluator.getLValueAssignmentVisitor(cfaEdge, pState);
    // Create ?visitor
    // For all values in lValue do:
    //  Get the SMGState, SMGValue etc.
    //  Get the correct type (bit size) for the value
    //  If the object to be assigned to ?
    for (SMGAddressAndState addressOfFieldAndState : lValue.accept(visitor)) {
      SMGAddress addressOfField = addressOfFieldAndState.getObject();
      pState = addressOfFieldAndState.getSmgState();

      CType fieldType = TypeUtils.getRealExpressionType(lValue);

      if (addressOfField.isUnknown()) {
        // Check for dereference errors in rValue
        List<SMGState> newStates =
            asSMGStateList(readValueToBeAssiged(pState.copyOf(), cfaEdge, rValue));
        newStates.forEach(smgState -> smgState.unknownWrite());
        result.addAll(newStates);
      } else {
        result.addAll(
            assignFieldToState(
                pState.copyOf(),
                cfaEdge,
                addressOfField.getObject(),
                addressOfField.getOffset().getAsLong(),
                fieldType,
                rValue));
      }
    }

    return result;
    */
    return null;
  }

  @SuppressWarnings("unused")
  public SMGState assignFieldToState(
      SMGState currentState,
      CFAEdge cfaEdge,
      SMGObject memoryOfField,
      BigInteger valueOffset,
      SMGValue pValue,
      CType rValueType)
      throws UnrecognizedCodeException {

    // TODO: getSizeof() method does not cover variable array length in C. Develop ideas for that!
    // For variable length stuff we need to read the SMG (for values) but also information from the
    // CFAEdge to determin which SMG object to read correctly! So this method needs to be
    // re-thought.

    BigInteger valueSize = machineModel.getSizeof(rValueType);

    // write values depending on the type of values
    if (valueOffset.compareTo(BigInteger.ZERO) < 0
        || memoryOfField.getSize().compareTo(valueOffset.add(valueSize)) < 0) {
      // Out of range does not mean failure just yet, it might be that
      // Log out of range info
      logOutOfRangeInformation(cfaEdge, memoryOfField, valueOffset, valueSize);

      if (memoryOfField.isZero()) {
        // Try to dereference a null pointer / all null pointers should be out of range
        return currentState.withNullPointerDereferenceWhenWriting(memoryOfField);
      } else {
        // Non null memory object but out of range write
        return currentState.withOutOfRangeWrite(memoryOfField, valueOffset, valueSize, pValue);
      }
    } else if (SMGCPAValueExpressionEvaluator.isStructOrUnionType(rValueType)) {
      // Write the struct
      // return assignStruct(currentState, memoryOfField, fieldOffset, rValueType, value, cfaEdge);
    } else {
      // Write non-struct value
      // return writeValue(currentState, memoryOfField, fieldOffset, rValueType, value, cfaEdge);
    }
    return null;
  }

  /**
   * TODO: move this. Structs get a seperate assignment method because we need to potentially copy
   * from one struct to another. TODO: Do we have to do more? They might have pointers in them.
   * (might even have methods)
   */
  @SuppressWarnings("unused")
  private SMGState assignStruct(
      SMGState pNewState,
      SMGObject pMemoryOfField,
      long pFieldOffset,
      CType pRValueType,
      SMGValue pValue,
      CFAEdge pCfaEdge)
      throws UnrecognizedCodeException {
    // If the value is a known address of a struct do:
    // Get the object for the (value address) struct
    // Write the information of the struct at the value address into the new struct at the given
    // offset/size
    // (I don't know if writeValue() is good, or a dedicated copy method would be better)

    return pNewState;
  }

  /** Logs attempts to write outside of the objects field size. */
  private void logOutOfRangeInformation(
      CFAEdge cfaEdge, SMGObject memoryOfField, BigInteger valueOffset, BigInteger valueSize) {
    // TODO: Does this work with DLS?
    logger.logf(
        Level.INFO,
        "%s, Out of range: Attempting to write %d bytes at offset %d into a field with size %d"
            + " bytes: %s",
        cfaEdge.getFileLocation(),
        valueSize,
        valueOffset,
        memoryOfField.getSize(),
        cfaEdge.getRawStatement());
  }

  /*
   * Preliminary options. Copied and modified from value CPA!
   */
  @Options(prefix = "cpa.smg2")
  public static class ValueTransferOptions {

    @Option(
        secure = true,
        description =
            "if there is an assumption like (x!=0), "
                + "this option sets unknown (uninitialized) variables to 1L, "
                + "when the true-branch is handled.")
    private boolean initAssumptionVars = false;

    @Option(
        secure = true,
        description =
            "Assume that variables used only in a boolean context are either zero or one.")
    private boolean optimizeBooleanVariables = true;

    @Option(secure = true, description = "Track or not function pointer values")
    private boolean ignoreFunctionValue = true;

    @Option(
        secure = true,
        description =
            "If 'ignoreFunctionValue' is set to true, this option allows to provide a fixed set of"
                + " values in the TestComp format. It is used for function-calls to calls of"
                + " VERIFIER_nondet_*. The file is provided via the option"
                + " functionValuesForRandom ")
    private boolean ignoreFunctionValueExceptRandom = false;

    @Option(
        secure = true,
        description =
            "Fixed set of values for function calls to VERIFIER_nondet_*. Does only work, if"
                + " ignoreFunctionValueExceptRandom is enabled ")
    @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
    private Path functionValuesForRandom = null;

    @Option(
        secure = true,
        description = "Use equality assumptions to assign values (e.g., (x == 0) => x = 0)")
    private boolean assignEqualityAssumptions = true;

    @Option(
        secure = true,
        description =
            "Allow the given extern functions and interpret them as pure functions"
                + " although the value analysis does not support their semantics"
                + " and this can produce wrong results.")
    private Set<String> allowedUnsupportedFunctions = ImmutableSet.of();

    public ValueTransferOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    boolean isInitAssumptionVars() {
      return initAssumptionVars;
    }

    boolean isAssignEqualityAssumptions() {
      return assignEqualityAssumptions;
    }

    boolean isOptimizeBooleanVariables() {
      return optimizeBooleanVariables;
    }

    boolean isIgnoreFunctionValue() {
      return ignoreFunctionValue;
    }

    public boolean isIgnoreFunctionValueExceptRandom() {
      return ignoreFunctionValueExceptRandom;
    }

    public Path getFunctionValuesForRandom() {
      return functionValuesForRandom;
    }

    boolean isAllowedUnsupportedOption(String func) {
      return allowedUnsupportedFunctions.contains(func);
    }
  }
}
