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
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.llvm.Module;
import org.llvm.TypeRef;
import org.llvm.Value;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * CFA builder for LLVM IR.
 * Metadata stored in the LLVM IR file is ignored.
 */
public class CFABuilder extends LlvmAstVisitor {
  // TODO: Linkage types
  // TODO: Visibility styles, i.e., default, hidden, protected
  // TODO: DLL Storage classes (do we actually need this?)
  // TODO: Thread Local Storage Model: May be important for concurrency

  // TODO: Alignment of global variables
  // TODO: Aliases (@a = %b) and IFuncs (@a = ifunc @..)

  private static final String TMP_VAR_PREFIX = "__tmp_";
  private static long tmpVarCount = 0;

  private final LogManager logger;
  private final MachineModel machineModel;

  private final LlvmTypeConverter typeConverter;

  private SortedSetMultimap<String, CFANode> cfaNodes;
  private List<Pair<ADeclaration, String>> globalDeclarations;

  public CFABuilder(final LogManager pLogger, final MachineModel pMachineModel) {
    logger = pLogger;
    machineModel = pMachineModel;

    typeConverter = new LlvmTypeConverter(pMachineModel, pLogger);

    cfaNodes = TreeMultimap.create();
    globalDeclarations = new ArrayList<>();
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
  protected CAstNode visitInstruction(final Value pItem) {
    assert pItem.isInstruction();
    pItem.dumpValue();

    int operandCount = pItem.getNumOperands();

    if (pItem.isAllocaInst()) {
      return handleAlloca(pItem);
    }

    return null;
  }

  private CAstNode handleAlloca(final Value pItem) {
    String assigned_var = pItem.getValueName();
    if (assigned_var.isEmpty()) {
      assigned_var = getTempVar();
    }

    final boolean isGlobal = pItem.isGlobalValue();
    // TODO: Support static and other storage classes
    final CStorageClass storageClass = CStorageClass.AUTO;
    final CType varType = typeConverter.getCType(pItem.getAllocatedType());

    return new CVariableDeclaration(
        getLocation(pItem),
        isGlobal,
        storageClass,
        varType,
        assigned_var,
        assigned_var,
        assigned_var,
        null);
  }

  private String getTempVar() {
    tmpVarCount++;
    return TMP_VAR_PREFIX + tmpVarCount;
  }

  private FunctionEntryNode handleFunctionDefinition(final Value pFuncDef) {
    String functionName = pFuncDef.getValueName();

    // Function type
    TypeRef functionType = pFuncDef.typeOf();
    CFunctionType cFuncType = (CFunctionType) typeConverter.getCType(functionType);

    // Parameters
    List<Value> paramVs = pFuncDef.getParams();
    List<CParameterDeclaration> parameters = new ArrayList<>(paramVs.size());
    for (Value v : paramVs) {
      String paramName = v.getValueName();
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
      String returnVarName = "__retval__";
      String scopedName = functionName + "::" + "__retval__"; // TODO: Is this needed?
      CVariableDeclaration returnVarDecl = new CVariableDeclaration(
          getLocation(pFuncDef), false, CStorageClass.AUTO, returnType, returnVarName,
          returnVarName, returnVarName, null /* no initializer */);

      returnVar = Optional.of(returnVarDecl);
    }

    return new CFunctionEntryNode(getLocation(pFuncDef), functionDeclaration, functionExit, returnVar);
  }

  @Override
  protected Behavior visitGlobalItem(final Value pItem) {
    return Behavior.CONTINUE; // Parent will iterate through the statements of the block that way
  }

  private FileLocation getLocation(final Value pItem) {
    return FileLocation.DUMMY;
  }
}
