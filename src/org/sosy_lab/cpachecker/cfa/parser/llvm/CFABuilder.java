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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
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
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

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
      // TODO
    } else if (pItem.isLoadInst()) {
      //TODO
    } else if (pItem.isStoreInst()) {
      //TODO
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
        new CSimpleType(false, false, CBasicType.INT, false, false, false, false, false, false, false),
        BigInteger.ONE
    );
    return new CExpressionStatement(getLocation(pItem), dummy_exp);
  }

  private CAstNode handleAlloca(final Value pItem, String pFunctionName) {
    // We ignore the specifics and handle alloca statements like C declarations
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
      final CType varType = typeConverter.getCType(pItem.getAllocatedType());

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

    return variableDeclarations.get(pItem);
  }

  private CAstNode handleReturn(final Value pItem, final String pFuncName) {
    Value returnVal = pItem.getReturnValue();
    Optional<CExpression> maybeExpression;
    Optional<CAssignment> maybeAssignment;
    if (returnVal == null) {
      maybeExpression = Optional.absent();
      maybeAssignment = Optional.absent();

    } else {
      CExpression returnExp = (CExpression) visitInstruction(returnVal, pFuncName);
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
        break;
      case LLVMFAdd:
        break;
      case LLVMSub:
        break;
      case LLVMFSub:
        break;
      case LLVMMul:
        break;
      case LLVMFMul:
        break;
      case LLVMUDiv:
        break;
      case LLVMSDiv:
        break;
      case LLVMFDiv:
        break;
      case LLVMURem:
        break;
      case LLVMSRem:
        break;
      case LLVMFRem:
        break;
      case LLVMShl:
        break;
      case LLVMLShr:
        break;
      case LLVMAShr:
        break;

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
        new CSimpleType(false, false, CBasicType.INT, false, false, false, false, false, false, false),
        BigInteger.ONE
    );
    return new CExpressionStatement(getLocation(pItem), dummy_exp);
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
