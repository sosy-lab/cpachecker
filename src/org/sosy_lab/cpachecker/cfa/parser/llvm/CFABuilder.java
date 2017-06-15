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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.llvm.Module;
import org.llvm.TypeRef;
import org.llvm.Value;
import org.llvm.binding.LLVMLibrary.LLVMOpcode;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

/**
 * CFA builder for LLVM IR.
 * Metadata stored in the LLVM IR file is ignored.
 */
public class CFABuilder extends LlvmAstVisitor {
  // TODO: Thread Local Storage Model: May be important for concurrency
  // TODO: Aliases (@a = %b) and IFuncs (@a = ifunc @..)

  private static final String RETURN_VAR_NAME = "__retval__";
  private static final String TMP_VAR_PREFIX = "__tmp_";
  private static long tmpVarCount = 0;

  private final LogManager logger;
  private final MachineModel machineModel;

  private final LlvmTypeConverter typeConverter;

  private final Map<Long, CSimpleDeclaration> variableDeclarations;

  public CFABuilder(final LogManager pLogger, final MachineModel pMachineModel) {
    super(pLogger);
    logger = pLogger;
    machineModel = pMachineModel;

    typeConverter = new LlvmTypeConverter(pMachineModel, pLogger);

    variableDeclarations = new HashMap<>();
  }

  public ParseResult build(final Module pModule) {
    visit(pModule);

    return new ParseResult(functions, cfaNodes, globalDeclarations, Language.LLVM);
  }

  @Override
  protected FunctionEntryNode visitFunction(final Value pItem) {
    assert pItem.isFunction();

    logger.log(Level.INFO, "Creating function: " + pItem.getValueName());

    return handleFunctionDefinition(pItem);
  }

  @Override
  protected CAstNode visitInstruction(final Value pItem, final String pFunctionName) {
    assert pItem.isInstruction();
    pItem.dumpValue();

    if (pItem.isAllocaInst()) {
      return handleAlloca(pItem, pFunctionName);

    } else if (pItem.isReturnInst()) {
      return handleReturn(pItem, pFunctionName);
    } else if (pItem.isUnreachableInst()) {
      // TODO

    } else if (pItem.isBinaryOperator()) {
      return handleBinaryOp(pItem, pFunctionName);
    } else if (pItem.isUnaryInstruction()) {
      return handleUnaryOp(pItem, pFunctionName);
    } else if (pItem.isStoreInst()) {
      return handleStore(pItem, pFunctionName);
    } else if (pItem.isCallInst()) {
      // TODO
    } else if (pItem.isSwitchInst()) {
      throw new UnsupportedOperationException();
    } else if (pItem.isIndirectBranchInst()) {
      throw new UnsupportedOperationException();
    } else if (pItem.isInvokeInst()) {
      throw new UnsupportedOperationException();
    } else {
      throw new UnsupportedOperationException();
    }

    CExpression dummy_exp = new CIntegerLiteralExpression(
        getLocation(pItem),
        CNumericTypes.UNSIGNED_INT,
        BigInteger.ONE
    );
    return new CExpressionStatement(getLocation(pItem), dummy_exp);
  }

  private CAstNode handleUnaryOp(final Value pItem, final String pFunctionName) {
     if (pItem.isLoadInst()) {
       return handleLoad(pItem, pFunctionName);
     } else {
       throw new UnsupportedOperationException(
           "LLVM does not yet support operator with opcode " + pItem.getOpCode());
     }
  }

  private CAstNode handleLoad(final Value pItem, final String pFunctionName) {
    CIdExpression assignee = getAssignedIdExpression(pItem, pFunctionName);
    CExpression expression = getAssignedIdExpression(pItem.getOperand(0), pFunctionName);
    return new CExpressionAssignmentStatement(getLocation(pItem), assignee, expression);
  }

  private CAstNode handleStore(final Value pItem, final String pFunctionName) {
    CIdExpression assignee = getAssignedIdExpression(pItem.getOperand(1), pFunctionName);
    CExpression expression = getExpression(pItem.getOperand(0), pFunctionName);
    return new CExpressionAssignmentStatement(getLocation(pItem), assignee, expression);
  }

  private CAstNode handleAlloca(final Value pItem, String pFunctionName) {
    // We ignore the specifics and handle alloca statements like C declarations of variables
    CSimpleDeclaration assignedVar = getAssignedVarDeclaration(pItem, pFunctionName);
    return assignedVar;
  }

  private CSimpleDeclaration getAssignedVarDeclaration(
      final Value pItem,
      final String pFunctionName
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
          getQualifiedName(assignedVar, pFunctionName),
          assignedVar,
          null);
      variableDeclarations.put(itemId, newDecl);
    }

    return variableDeclarations.get(itemId);
  }

  private CAstNode handleReturn(final Value pItem, final String pFuncName) {
    Value returnVal = pItem.getReturnValue();
    Optional<CExpression> maybeExpression;
    Optional<CAssignment> maybeAssignment;
    if (returnVal == null) {
      maybeExpression = Optional.absent();
      maybeAssignment = Optional.absent();

    } else {
      CExpression returnExp = getExpression(returnVal, pFuncName);
      maybeExpression = Optional.of(returnExp);

      CSimpleDeclaration returnVarDecl = getReturnVar(pFuncName, returnExp.getExpressionType());

      CIdExpression returnVar = new CIdExpression(getLocation(returnVal), returnVarDecl);

      CAssignment returnVarAssignment =
          new CExpressionAssignmentStatement(getLocation(returnVal), returnVar, returnExp);
      maybeAssignment = Optional.of(returnVarAssignment);
    }

    return new CReturnStatement(getLocation(pItem), maybeExpression, maybeAssignment);
  }

  private String getQualifiedName(String pReturnVarName, String pFuncName) {
    return pFuncName + "::" + pReturnVarName;
  }

  private CAstNode handleBinaryOp(final Value pItem, String pFunctionName) {
    LLVMOpcode opCode =  pItem.getOpCode();

    switch (opCode) {
      // Arithmetic operations
      case LLVMAdd:
      case LLVMFAdd:
      case LLVMSub:
      case LLVMFSub:
      case LLVMMul:
      case LLVMFMul:
      case LLVMUDiv:
      case LLVMSDiv:
      case LLVMFDiv:
      case LLVMURem:
      case LLVMSRem:
      case LLVMFRem:
      case LLVMShl:
      case LLVMLShr:
      case LLVMAShr:
        return handleArithmeticOp(pItem, opCode, pFunctionName);

      // Boolean operations
      case LLVMAnd:
        break;
      case LLVMOr:
        break;
      case LLVMXor:
        break;

      // Comparison operations
      case LLVMICmp:
        break;
      case LLVMFCmp:
        break;

      // Select operator
      case LLVMSelect:
        break;

      // Sign extension/truncation operations
      case LLVMTrunc:
        break;
      case LLVMZExt:
        break;
      case LLVMSExt:
        break;
      case LLVMFPToUI:
        break;
      case LLVMFPToSI:
        break;
      case LLVMUIToFP:
        break;
      case LLVMSIToFP:
        break;
      case LLVMFPTrunc:
        break;
      case LLVMFPExt:
        break;
      case LLVMPtrToInt:
        break;
      case LLVMIntToPtr:
        break;
      case LLVMBitCast:
        break;
      case LLVMAddrSpaceCast:
        break;

      // Aggregate operations
      case LLVMExtractValue:
        break;
      case LLVMInsertValue:
        break;

      case LLVMPHI:
        break;

      case LLVMGetElementPtr:
        break;


      case LLVMUserOp1:
      case LLVMUserOp2:
      case LLVMVAArg:
      // Vector operations
      case LLVMExtractElement:
      case LLVMInsertElement:
      case LLVMShuffleVector:
      // Concurrency-centric operations
      case LLVMFence:

      case LLVMAtomicCmpXchg:
        break;
      case LLVMAtomicRMW:
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
    return new CExpressionStatement(getLocation(pItem), dummy_exp);
  }

  private CAstNode handleArithmeticOp(
      final Value pItem,
      final LLVMOpcode pOpCode,
      final String pFunctionName
  ) {
    final CType expressionType = typeConverter.getCType(pItem.typeOf());

    // TODO: Currently we only support flat expressions, no nested ones. Makes this work
    // in the future.
    Value operand1 = pItem.getOperand(0); // First operand
    logger.log(Level.INFO, "Getting id expression for operand 1");
    CExpression operand1Exp = getExpression(operand1, pFunctionName);
    Value operand2 = pItem.getOperand(1); // Second operand
    logger.log(Level.INFO, "Getting id expression for operand 2");
    CExpression operand2Exp = getExpression(operand2, pFunctionName);

    CBinaryExpression.BinaryOperator operation;
    switch (pOpCode) {
      case LLVMAdd:
      case LLVMFAdd:
        operation = BinaryOperator.PLUS;
        break;
      case LLVMSub:
      case LLVMFSub:
        operation = BinaryOperator.MINUS;
        break;
      case LLVMMul:
      case LLVMFMul:
        operation = BinaryOperator.MULTIPLY;
        break;
      case LLVMUDiv:
      case LLVMSDiv:
      case LLVMFDiv:
        // TODO: Respect unsigned and signed divide
        operation = BinaryOperator.DIVIDE;
        break;
      case LLVMURem:
      case LLVMSRem:
      case LLVMFRem:
        // TODO: Respect unsigned and signed modulo
        operation = BinaryOperator.MODULO;
        break;
      case LLVMShl: // Shift left
        operation = BinaryOperator.SHIFT_LEFT;
        break;
      case LLVMLShr: // Logical shift right
      case LLVMAShr: // arithmetic shift right
        // TODO Differentiate between logical and arithmetic shift somehow
        operation = BinaryOperator.SHIFT_RIGHT;
        break;
      default:
        throw new UnsupportedOperationException(String.valueOf(pOpCode.value()));
    }

    CExpression expression = new CBinaryExpression(
        getLocation(pItem),
        expressionType,
        expressionType, // calculation type is expression type in LLVM
        operand1Exp,
        operand2Exp,
        operation
        );

    CIdExpression assignedVar = getAssignedIdExpression(pItem, pFunctionName);

    assert expressionType.equals(assignedVar.getExpressionType())
        : "Expression returns type different from assigned variable";
    return new CExpressionAssignmentStatement(getLocation(pItem), assignedVar, expression);
  }

  private CExpression getExpression(final Value pItem, final String pFunctionName) {
    if (pItem.isConstantInt() || pItem.isConstantFP()) {
      return getConstant(pItem);
    } else {
      return getAssignedIdExpression(pItem, pFunctionName);
    }
  }

  private CExpression getConstant(final Value pItem) {
    if (pItem.isConstantInt()) {
      long constantValue = pItem.constIntGetZExtValue();
      return new CIntegerLiteralExpression(
          getLocation(pItem),
          CNumericTypes.UNSIGNED_LONG_LONG_INT,
          BigInteger.valueOf(constantValue));
    } else {
      assert pItem.isConstantFP();
      throw new UnsupportedOperationException("LLVM parsing does not support float constants yet");
    }
  }

  private CIdExpression getAssignedIdExpression(final Value pItem, final String pFunctionName) {
    logger.log(Level.INFO, "Getting var declaration for item");
    pItem.dumpValue();
    CSimpleDeclaration assignedVarDeclaration = getAssignedVarDeclaration(pItem, pFunctionName);
    String assignedVarName = assignedVarDeclaration.getName();
    CType expressionType = assignedVarDeclaration.getType();

    return new CIdExpression(
        getLocation(pItem), expressionType, assignedVarName, assignedVarDeclaration);
  }

  private String getTempVar() {
    tmpVarCount++;
    return TMP_VAR_PREFIX + tmpVarCount;
  }

  private FunctionEntryNode handleFunctionDefinition(final Value pFuncDef) {
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
    FunctionExitNode functionExit = new FunctionExitNode(functionName);

    // Return variable : The return value is written to this
    Optional<CVariableDeclaration> returnVar;
    CType returnType = cFuncType.getReturnType();
    if (returnType.equals(CVoidType.VOID)) {
      returnVar = Optional.absent();

    } else {
      CVariableDeclaration returnVarDecl = getReturnVar(functionName, returnType);
      returnVar = Optional.of(returnVarDecl);
    }

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

  @Override
  protected Behavior visitGlobalItem(final Value pItem) {
    return Behavior.CONTINUE; // Parent will iterate through the statements of the block that way
  }

  private FileLocation getLocation(final Value pItem) {
    return FileLocation.DUMMY;
  }
}
