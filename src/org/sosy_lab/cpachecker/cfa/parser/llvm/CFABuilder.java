/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.llvm;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.llvm.Module;
import org.llvm.TypeRef;
import org.llvm.Value;
import org.llvm.Value.OpCode;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 * CFA builder for LLVM IR.
 * Metadata stored in the LLVM IR file is ignored.
 */
public class CFABuilder extends LlvmAstVisitor {
  // TODO: Thread Local Storage Model: May be important for concurrency
  // TODO: Aliases (@a = %b) and IFuncs (@a = ifunc @..)

  private static final String RETURN_VAR_NAME = "__retval__";
  private static final String TMP_VAR_PREFIX = "__tmp_";

  private static final CFunctionDeclaration ABORT_FUNC_DECL = new CFunctionDeclaration(
          FileLocation.DUMMY,
          new CFunctionType(
              false,
              false,
              CVoidType.VOID,
              Collections.emptyList(),
              false),
      "abort",
          Collections.emptyList()
  );
  private static final CExpression ABORT_FUNC_NAME =
      new CIdExpression(FileLocation.DUMMY, CVoidType.VOID, "abort", ABORT_FUNC_DECL);

  private static long tmpVarCount = 0;

  private final LogManager logger;
  private final MachineModel machineModel;

  private final LlvmTypeConverter typeConverter;
  private CBinaryExpressionBuilder binaryExpressionBuilder;

  // Value address -> Variable declaration
  private final Map<Long, CSimpleDeclaration> variableDeclarations;
  // Function name -> Function declaration
  private Map<String, CFunctionDeclaration> functionDeclarations;

  public CFABuilder(final LogManager pLogger, final MachineModel pMachineModel) {
    logger = pLogger;
    machineModel = pMachineModel;

    typeConverter = new LlvmTypeConverter(pMachineModel, pLogger);

    variableDeclarations = new HashMap<>();
    functionDeclarations = new HashMap<>();

    binaryExpressionBuilder = new CBinaryExpressionBuilder(machineModel, logger);
  }

  public ParseResult build(final Module pModule, final String pFilename) {
    visit(pModule);
    List<Path> input_file = ImmutableList.of(Paths.get(pFilename));

    return new ParseResult(functions, cfaNodes, globalDeclarations, input_file);
  }

  @Override
  protected FunctionEntryNode visitFunction(final Value pItem) {
    assert pItem.isFunction();

    logger.log(Level.FINE, "Creating function: " + pItem.getValueName());

    return handleFunctionDefinition(pItem);
  }

  @Override
  protected CExpression getBranchCondition(final Value pItem, String funcName) {
    Value cond = pItem.getCondition();
    try {
      CType expectedType = typeConverter.getCType(cond.typeOf());
      return binaryExpressionBuilder.buildBinaryExpression(
        getExpression(cond, expectedType),
        new CIntegerLiteralExpression(
            getLocation(pItem),
            CNumericTypes.BOOL,
            BigInteger.ONE),
        BinaryOperator.EQUALS);
    } catch (UnrecognizedCCodeException e) {
        throw new AssertionError(e.toString());
    }
  }

  @Override
  protected List<CAstNode> visitInstruction(final Value pItem, final String pFunctionName) {
    assert pItem.isInstruction();

    if (pItem.isAllocaInst()) {
      return handleAlloca(pItem, pFunctionName);

    } else if (pItem.isReturnInst()) {
      return handleReturn(pItem, pFunctionName);
    } else if (pItem.isUnreachableInst()) {
      return handleUnreachable(pItem);

    } else if (pItem.isBinaryOperator()) {
      return handleBinaryOp(pItem, pFunctionName);
    } else if (pItem.isUnaryInstruction()) {
      return handleUnaryOp(pItem, pFunctionName);
    } else if (pItem.isStoreInst()) {
      return handleStore(pItem, pFunctionName);
    } else if (pItem.isCallInst()) {
      return handleCall(pItem, pFunctionName);
    } else if (pItem.isCmpInst()) {
      return handleCmpInst(pItem, pFunctionName);
    } else if (pItem.isGetElementPtrInst()) {
      return handleGEP();
    } else if (pItem.isSwitchInst()) {

      throw new UnsupportedOperationException();
    } else if (pItem.isIndirectBranchInst()) {
      throw new UnsupportedOperationException();
    } else if (pItem.isBranchInst()) {
      return null;
    } else if (pItem.isPHINode()) {
      // TODO!
      throw new UnsupportedOperationException();
    } else if (pItem.isInvokeInst()) {
      throw new UnsupportedOperationException();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private List<CAstNode> handleCall(final Value pItem, final String pCallingFunctionName) {
    assert pItem.isCallInst();
    FileLocation loc = getLocation(pItem);
    CType returnType = typeConverter.getCType(pItem.typeOf());
    Value calledFunction = pItem.getOperand(0);
    int argumentCount = pItem.getNumOperands();

    String functionName = calledFunction.getValueName();
    // May be null and that's ok - CPAchecker will handle the call as a call to a builtin function,
    // then
    CFunctionDeclaration functionDeclaration = functionDeclarations.get(functionName);

    CIdExpression functionNameExp;
    List<CExpression> parameters = new ArrayList<>(argumentCount);
    if (functionDeclaration == null) {
      // Try to derive a function type from the call
      List<CType> parameterTypes = new ArrayList<>(argumentCount-1);
      for (int i = 1; i < argumentCount; i++) {
        Value functionArg = pItem.getOperand(i);
        assert functionArg.isConstant() || variableDeclarations.containsKey(functionArg.getAddress());
        CType expectedType = typeConverter.getCType(functionArg.typeOf());
        parameterTypes.add(expectedType);
        parameters.add(getAssignedIdExpression(functionArg, expectedType));
      }

      CFunctionType functionType =
          new CFunctionType(false, false, returnType, parameterTypes, false);
      functionNameExp = new CIdExpression(loc, functionType, functionName, null);
    } else {
      functionNameExp =
          new CIdExpression(loc, functionDeclaration.getType(), functionName, functionDeclaration);

      List<CParameterDeclaration> parameterDeclarations = functionDeclaration.getParameters();
      // i = 1 to skip the function name, we only want to look at arguments
      for (int i = 1; i < argumentCount; i++) {
        Value functionArg = pItem.getOperand(i);
        // Parameter declarations start at 0, not 1, so we have to subtract 1 again
        CType expectedType = parameterDeclarations.get(i - 1).getType();

        assert
            functionArg.isConstant() || variableDeclarations.containsKey(functionArg.getAddress());
        parameters.add(getAssignedIdExpression(functionArg, expectedType));
      }
    }

    CFunctionCallExpression callExpression = new CFunctionCallExpression(loc, returnType,
        functionNameExp, parameters, functionDeclaration);

    if (returnType.equals(CVoidType.VOID)) {
      return ImmutableList.of(new CFunctionCallStatement(loc, callExpression));
    } else {
      return getAssignStatement(pItem, callExpression, pCallingFunctionName);
    }
  }

  private List<CAstNode> handleUnreachable(final Value pItem) {
    CFunctionCallExpression callExpression =
        new CFunctionCallExpression(getLocation(pItem), CVoidType.VOID, ABORT_FUNC_NAME,
            Collections.emptyList(), ABORT_FUNC_DECL);

    return ImmutableList.of(new CFunctionCallStatement(getLocation(pItem), callExpression));
  }

  private List<CAstNode> handleUnaryOp(final Value pItem, final String pFunctionName) {
     if (pItem.isLoadInst()) {
       return handleLoad(pItem, pFunctionName);
     } else if (pItem.isCastInst()) {
       return handleCastInst(pItem, pFunctionName);
     } else {
       throw new UnsupportedOperationException(
           "LLVM does not yet support operator with opcode " + pItem.getOpCode());
     }
  }

  private List<CAstNode> handleLoad(final Value pItem, final String pFunctionName) {
    CType expectedType = typeConverter.getCType(pItem.typeOf());
    CExpression expression = getAssignedIdExpression(pItem.getOperand(0), expectedType);
    return getAssignStatement(pItem, expression, pFunctionName);
  }

  private List<CAstNode> handleStore(final Value pItem, final String pFunctionName) {
    Value valueToStoreTo = pItem.getOperand(1);
    Value valueToLoad = pItem.getOperand(0);

    CType expectedType = typeConverter.getCType(valueToLoad.typeOf());
    CExpression expression = getExpression(valueToLoad, expectedType);

    return getAssignStatement(valueToStoreTo, expression, pFunctionName);
  }

  private List<CAstNode> handleAlloca(final Value pItem, String pFunctionName) {
    // We ignore the specifics and handle alloca statements like C declarations of variables
    CSimpleDeclaration assignedVar = getAssignedVarDeclaration(pItem, pFunctionName, null);
    return ImmutableList.of(assignedVar);
  }

  private List<CAstNode> handleReturn(final Value pItem, final String pFuncName) {
    Value returnVal = pItem.getReturnValue();
    Optional<CExpression> maybeExpression;
    Optional<CAssignment> maybeAssignment;
    if (returnVal == null) {
      maybeExpression = Optional.absent();
      maybeAssignment = Optional.absent();

    } else {
      CType expectedType = typeConverter.getCType(returnVal.typeOf());
      CExpression returnExp = getExpression(returnVal, expectedType);
      maybeExpression = Optional.of(returnExp);

      CSimpleDeclaration returnVarDecl = getReturnVar(pFuncName, returnExp.getExpressionType());

      CIdExpression returnVar = new CIdExpression(getLocation(returnVal), returnVarDecl);

      CAssignment returnVarAssignment =
          new CExpressionAssignmentStatement(getLocation(returnVal), returnVar, returnExp);
      maybeAssignment = Optional.of(returnVarAssignment);
    }

    return ImmutableList.of(
        new CReturnStatement(getLocation(pItem), maybeExpression, maybeAssignment));
  }

  private String getQualifiedName(String pReturnVarName, String pFuncName) {
    return pFuncName + "::" + pReturnVarName;
  }

  private List<CAstNode> handleBinaryOp(final Value pItem, String pFunctionName) {
    OpCode opCode = pItem.getOpCode();

    switch (opCode) {
      // Arithmetic operations
      case Add:
      case FAdd:
      case Sub:
      case FSub:
      case Mul:
      case FMul:
      case UDiv:
      case SDiv:
      case FDiv:
      case URem:
      case SRem:
      case FRem:
      case Shl:
      case LShr:
      case AShr:
      case And:
      case Or:
      case Xor:
        return handleArithmeticOp(pItem, opCode, pFunctionName);

      // Comparison operations
      case ICmp:
        break;
      case FCmp:
        break;

      // Select operator
      case Select:
        break;

      // Sign extension/truncation operations
      case Trunc:
        break;
      case ZExt:
        break;
      case SExt:
        break;
      case FPToUI:
        break;
      case FPToSI:
        break;
      case UIToFP:
        break;
      case SIToFP:
        break;
      case FPTrunc:
        break;
      case FPExt:
        break;
      case PtrToInt:
        break;
      case IntToPtr:
        break;
      case BitCast:
        break;
      case AddrSpaceCast:
        break;

      // Aggregate operations
      case ExtractValue:
        break;
      case InsertValue:
        break;

      case PHI:
        break;

      case GetElementPtr:
        break;


      case UserOp1:
      case UserOp2:
      case VAArg:
      // Vector operations
      case ExtractElement:
      case InsertElement:
      case ShuffleVector:
      // Concurrency-centric operations
      case Fence:

      case AtomicCmpXchg:
        break;
      case AtomicRMW:
        break;
      default:
        throw new UnsupportedOperationException(opCode.toString());
    }
    CExpression dummy_exp = new CIntegerLiteralExpression(
        getLocation(pItem),
        new CSimpleType(false, false, CBasicType.INT,
            false, false, false, false,
            false, false, false),
        BigInteger.ONE
    );
    return ImmutableList.of(new CExpressionStatement(getLocation(pItem), dummy_exp));
  }

  private List<CAstNode> handleArithmeticOp(
      final Value pItem,
      final OpCode pOpCode,
      final String pFunctionName
  ) {
    final CType expressionType = typeConverter.getCType(pItem.typeOf());

    // TODO: Currently we only support flat expressions, no nested ones. Makes this work
    // in the future.
    Value operand1 = pItem.getOperand(0); // First operand
    logger.log(Level.FINE, "Getting id expression for operand 1");
    CType op1type = typeConverter.getCType(operand1.typeOf());
    CExpression operand1Exp = getExpression(operand1, op1type);
    Value operand2 = pItem.getOperand(1); // Second operand
    CType op2type = typeConverter.getCType(operand2.typeOf());
    logger.log(Level.FINE, "Getting id expression for operand 2");
    CExpression operand2Exp = getExpression(operand2, op2type);

    CBinaryExpression.BinaryOperator operation;
    switch (pOpCode) {
      case Add:
      case FAdd:
        operation = BinaryOperator.PLUS;
        break;
      case Sub:
      case FSub:
        operation = BinaryOperator.MINUS;
        break;
      case Mul:
      case FMul:
        operation = BinaryOperator.MULTIPLY;
        break;
      case UDiv:
      case SDiv:
      case FDiv:
        // TODO: Respect unsigned and signed divide
        operation = BinaryOperator.DIVIDE;
        break;
      case URem:
      case SRem:
      case FRem:
        // TODO: Respect unsigned and signed modulo
        operation = BinaryOperator.MODULO;
        break;
      case Shl: // Shift left
        operation = BinaryOperator.SHIFT_LEFT;
        break;
      case LShr: // Logical shift right
      case AShr: // arithmetic shift right
        // TODO Differentiate between logical and arithmetic shift somehow
        operation = BinaryOperator.SHIFT_RIGHT;
        break;
      case And:
        operation = BinaryOperator.BINARY_AND;
        break;
      case Or:
        operation = BinaryOperator.BINARY_OR;
        break;
      case Xor:
        operation = BinaryOperator.BINARY_XOR;
        break;
      default:
        throw new AssertionError("Unhandled operation " + pOpCode);
    }

    CExpression expression = new CBinaryExpression(
        getLocation(pItem),
        expressionType,
        expressionType, // calculation type is expression type in LLVM
        operand1Exp,
        operand2Exp,
        operation
        );

    return getAssignStatement(pItem, expression, pFunctionName);
  }

  private CExpression getExpression(
      final Value pItem, final CType pExpectedType) {
    if (pItem.isConstantInt() || pItem.isConstantFP()) {
      return getConstant(pItem);
    } else {
      return getAssignedIdExpression(pItem, pExpectedType);
    }
  }

  private CExpression getConstant(final Value pItem) {
    CType expectedType = typeConverter.getCType(pItem.typeOf());
    if (pItem.isConstantInt()) {
      long constantValue = pItem.constIntGetSExtValue();
      return new CIntegerLiteralExpression(
          getLocation(pItem),
          expectedType,
          BigInteger.valueOf(constantValue));
    } else {
      assert pItem.isConstantFP();
      throw new UnsupportedOperationException("LLVM parsing does not support float constants yet");
    }
  }

  private List<CAstNode> getAssignStatement(
      final Value pAssignee,
      final CRightHandSide pAssignment,
      final String pFunctionName
  ) {
    long assigneeId = pAssignee.getAddress();
    CType expectedType = pAssignment.getExpressionType();
    // Variable is already declared, so it must only be assigned the new value
    if (variableDeclarations.containsKey(assigneeId)) {
      CLeftHandSide assigneeIdExp = (CLeftHandSide) getAssignedIdExpression(pAssignee, expectedType);

      CType varType = assigneeIdExp.getExpressionType();
      if (!(varType.equals(expectedType))) {
        assert expectedType instanceof CPointerType;
        assigneeIdExp = new CPointerExpression(getLocation(pAssignee), varType, assigneeIdExp);
      }

      if (pAssignment instanceof CFunctionCallExpression) {
        return ImmutableList.of(new CFunctionCallAssignmentStatement(
            getLocation(pAssignee),
            assigneeIdExp,
            (CFunctionCallExpression) pAssignment
        ));

      } else {
        return ImmutableList.of(new CExpressionAssignmentStatement(
            getLocation(pAssignee),
            assigneeIdExp,
            (CExpression) pAssignment));
      }

    } else {  // Variable must be newly declared
      if (pAssignment instanceof CFunctionCallExpression) {
        CSimpleDeclaration assigneeDecl =
            getAssignedVarDeclaration(pAssignee, pFunctionName, null);
        CLeftHandSide assigneeIdExp =
            (CLeftHandSide) getAssignedIdExpression(pAssignee, expectedType);

        return ImmutableList.of(
            assigneeDecl,
            new CFunctionCallAssignmentStatement(
              getLocation(pAssignee),
              assigneeIdExp,
              (CFunctionCallExpression) pAssignment)
        );

      } else {
        CInitializer initializer =
            new CInitializerExpression(getLocation(pAssignee), (CExpression) pAssignment);
        CSimpleDeclaration assigneeDecl =
            getAssignedVarDeclaration(pAssignee, pFunctionName, initializer);
        return ImmutableList.of(assigneeDecl);
      }
    }
  }

  private CSimpleDeclaration getAssignedVarDeclaration(
      final Value pItem,
      final String pFunctionName,
      final CInitializer pInitializer
  ) {
    final long itemId = pItem.getAddress();
    if (!variableDeclarations.containsKey(itemId)) {
      String assignedVar = pItem.getValueName();

      if (assignedVar.isEmpty()) {
        assignedVar = getTempVar();
      }

      final boolean isGlobal = pItem.isGlobalValue();
      // TODO: Support static and other storage classes
      final CStorageClass storageClass = CStorageClass.AUTO;
      CType varType;
      // We handle alloca not like malloc, which returns a pointer, but as a general
      // variable declaration. Consider that here by using the allocated type, not the
      // pointer of that type alloca returns.
      if (pItem.isAllocaInst()) {
        varType = typeConverter.getCType(pItem.getAllocatedType());
      } else {
        varType = typeConverter.getCType(pItem.typeOf());
      }

      CSimpleDeclaration newDecl = new CVariableDeclaration(
          getLocation(pItem),
          isGlobal,
          storageClass,
          varType,
          assignedVar,
          assignedVar,
          getQualifiedName(assignedVar, pFunctionName),
          pInitializer);
      assert !variableDeclarations.containsKey(itemId);
      variableDeclarations.put(itemId, newDecl);
    }

    return variableDeclarations.get(itemId);
  }


  private CExpression getAssignedIdExpression(final Value pItem, final CType pExpectedType) {
    logger.log(Level.FINE, "Getting var declaration for item");
    assert variableDeclarations.containsKey(pItem.getAddress())
        : "ID expression has no declaration!";
    CSimpleDeclaration assignedVarDeclaration =
        variableDeclarations.get(pItem.getAddress());
    String assignedVarName = assignedVarDeclaration.getName();
    CType expressionType = assignedVarDeclaration.getType();
    CIdExpression idExpression = new CIdExpression(
        getLocation(pItem), expressionType, assignedVarName, assignedVarDeclaration);

    if (pExpectedType.equals(expressionType)) {
      return idExpression;

    } else if (pointerOf(pExpectedType, expressionType)) {
      CType typePointingTo = ((CPointerType) pExpectedType).getType();
      if (typePointingTo.equals(expressionType)) {
        return new CUnaryExpression(
            getLocation(pItem), pExpectedType, idExpression, UnaryOperator.AMPER);
      } else {
        throw new AssertionError("Unhandled type structure");
      }
    } else if (expressionType instanceof CPointerType) {
      return new CPointerExpression(getLocation(pItem), pExpectedType, idExpression);
    } else {
      throw new AssertionError("Unhandled types structure");
    }
  }

  /**
   * Returns whether the first param is a pointer of the type of the second parameter.<br />
   * Examples:
   * <ul>
   *   <li>pointerOf(*int, int) -> true</li>
   *  <li>pointerOf(**int, *int) -> true</li>
   *  <li>pointerOf(int, int*) -> false</li>
   *  <li>pointerOf(int, int) -> false</li>
   *</ul>
   **/
  private boolean pointerOf(CType pPotentialPointer, CType pPotentialPointee) {
    if (pPotentialPointer instanceof CPointerType) {
      return ((CPointerType) pPotentialPointer).getType().equals(pPotentialPointee);
    } else {
      return false;
    }
  }

  private String getTempVar() {
    tmpVarCount++;
    return TMP_VAR_PREFIX + tmpVarCount;
  }

  @Override
  protected void declareFunction(final Value pFuncDef) {
    String functionName = pFuncDef.getValueName();

    // Function type
    TypeRef functionType = pFuncDef.typeOf();
    TypeRef elemType = functionType.getElementType();
    CFunctionType cFuncType = (CFunctionType) typeConverter.getCType(elemType);

    // Parameters
    List<Value> paramVs = pFuncDef.getParams();
    List<CParameterDeclaration> parameters = new ArrayList<>(paramVs.size());
    int unnamed_value = 1;
    for (Value v : paramVs) {
      String paramName = v.getValueName();
      if (paramName.isEmpty()) {
        paramName = Integer.toString(++unnamed_value);
      }

      CType paramType = typeConverter.getCType(v.typeOf());
      CParameterDeclaration parameter = new CParameterDeclaration(FileLocation.DUMMY, paramType, paramName);
      parameters.add(parameter);
    }

    // Function declaration, exit
    CFunctionDeclaration functionDeclaration = new CFunctionDeclaration(
        getLocation(pFuncDef),
        cFuncType,
        functionName,
        parameters);
    functionDeclarations.put(functionName, functionDeclaration);
  }

  private FunctionEntryNode handleFunctionDefinition(final Value pFuncDef) {
    assert !pFuncDef.isDeclaration();

    String functionName = pFuncDef.getValueName();
    FunctionExitNode functionExit = new FunctionExitNode(functionName);
    addNode(functionName, functionExit);

    // Function type
    TypeRef functionType = pFuncDef.typeOf();
    TypeRef elemType = functionType.getElementType();
    CFunctionType cFuncType = (CFunctionType) typeConverter.getCType(elemType);

    // Return variable : The return value is written to this
    Optional<CVariableDeclaration> returnVar;
    CType returnType = cFuncType.getReturnType();
    if (returnType.equals(CVoidType.VOID)) {
      returnVar = Optional.absent();

    } else {
      CVariableDeclaration returnVarDecl = getReturnVar(functionName, returnType);
      returnVar = Optional.of(returnVarDecl);
    }

    CFunctionDeclaration functionDeclaration = functionDeclarations.get(functionName);
    FunctionEntryNode entry = new CFunctionEntryNode(
      getLocation(pFuncDef), functionDeclaration,
      functionExit, returnVar);
    functionExit.setEntryNode(entry);

    return entry;
  }

  private CVariableDeclaration getReturnVar(String pFunctionName, CType pType) {
    return new CVariableDeclaration(
        FileLocation.DUMMY, false, CStorageClass.AUTO, pType, RETURN_VAR_NAME,
        RETURN_VAR_NAME, getQualifiedName(RETURN_VAR_NAME, pFunctionName), null /* no initializer */);
  }

  private List<CAstNode> handleGEP() {
      return null;
      //return getAssignStatement(pItem, ptrexpr, pFunctionName);
  }

  private List<CAstNode> handleCmpInst(final Value pItem, String pFunctionName) {
    // the only one supported now
    assert pItem.isICmpInst();

    BinaryOperator operator = null;
    switch (pItem.getICmpPredicate()) {
        case IntEQ:
            operator = BinaryOperator.EQUALS;
            break;
        case IntNE:
            operator = BinaryOperator.NOT_EQUALS;
            break;
        case IntUGT:
        case IntSGT:
            operator = BinaryOperator.GREATER_THAN;
            break;
        case IntULT:
        case IntSLT:
            operator = BinaryOperator.LESS_THAN;
            break;
        case IntULE:
        case IntSLE:
            operator = BinaryOperator.LESS_EQUAL;
            break;
        case IntUGE:
        case IntSGE:
            operator = BinaryOperator.GREATER_EQUAL;
            break;
        default:
            throw new UnsupportedOperationException("Unsupported predicate");
    }

    assert operator != null;
    Value operand1 = pItem.getOperand(0);
    Value operand2 = pItem.getOperand(1);
    CType op1type = typeConverter.getCType(operand1.typeOf());
    CType op2type = typeConverter.getCType(operand2.typeOf());
    try {
      CBinaryExpression cmp = binaryExpressionBuilder.buildBinaryExpression(
        getExpression(operand1, op1type),
        getExpression(operand2, op2type),
        operator);

      return getAssignStatement(pItem, cmp, pFunctionName);

    } catch (UnrecognizedCCodeException e) {
        throw new UnsupportedOperationException(e.toString());
    }
  }

  private List<CAstNode> handleCastInst(final Value pItem, String pFunctionName) {
    Value castOperand = pItem.getOperand(0);
    CType operandType = typeConverter.getCType(castOperand.typeOf());
    CCastExpression cast = new CCastExpression(getLocation(pItem),
                                               typeConverter.getCType(pItem.typeOf()),
                                               getExpression(castOperand, operandType));
    return getAssignStatement(pItem, cast, pFunctionName);
  }

  @Override
  protected ADeclaration visitGlobalItem(final Value pItem) {
    /*
    assert !pItem.isExternallyInitialized();

    // now we handle only simple initializers
    Value initializer = pItem.getInitializer();
    return getConstant(initializer);
    */
    return null;
  }

  private FileLocation getLocation(final Value pItem) {
    assert pItem != null;
    return FileLocation.DUMMY;
  }
}
