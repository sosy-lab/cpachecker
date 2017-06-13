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

import ap.interpolants.StructuredPrograms.Assertion;
import com.google.common.base.Optional;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.bridj.IntValuedEnum;
import org.llvm.BasicBlock;
import org.llvm.Module;
import org.llvm.TypeRef;
import org.llvm.Value;
import org.llvm.binding.LLVMLibrary.LLVMTypeKind;
import org.matheclipse.core.reflection.system.Mod;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
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

  private final LogManager logger;
  private final MachineModel machineModel;
  private SortedMap<String, FunctionEntryNode> functions;
  private SortedSetMultimap<String, CFANode> cfaNodes;
  private List<Pair<ADeclaration, String>> globalDeclarations;

  public CFABuilder(final LogManager pLogger, final MachineModel pMachineModel) {
    logger = pLogger;
    machineModel = pMachineModel;

    functions = new TreeMap<>();
    cfaNodes = TreeMultimap.create();
    globalDeclarations = new ArrayList<>();
  }

  public ParseResult build(final Module pModule) {
    visit(pModule);

    return new ParseResult(functions, cfaNodes, globalDeclarations, Language.LLVM);
  }

  @Override
  protected Behavior visitModule(final Module pItem) {
    return Behavior.CONTINUE; // Parent will go inside the global variables and blocks that way
  }

  @Override
  protected Behavior visitGlobalItem(final BasicBlock pItem) {
    return Behavior.CONTINUE; // Parent will iterate through the statements of the block that way
  }

  @Override
  protected Behavior visitInFunction(final BasicBlock pItem) {
    return Behavior.CONTINUE; // Parent will iterate through the statements of the block that way
  }

  @Override
  protected Behavior visitInFunction(final Value pItem) {
    if (pItem.isFunction()) { // Function definition
      handleFunctionDefinition(pItem);

    } else {
      throw new AssertionError();
    }

    return Behavior.CONTINUE;
  }

  private void handleFunctionDefinition(final Value pFuncDef) {
    assert pFuncDef.isFunction();
    TypeRef functionType = pFuncDef.typeOf();
    TypeRef returnType = functionType.getReturnType();


    CType cReturnType = getCType(returnType);
    CFunctionDeclaration functionDeclaration = null;
    FunctionExitNode functionExit = null;
    Optional<CVariableDeclaration> returnVar = null;



    new CFunctionEntryNode(getLocation(pFuncDef), functionDeclaration, functionExit, returnVar);
  }

  private CType getCType(final TypeRef pReturnType) {
    CType cType = null; // FIXME
    final boolean isConst = false;
    final boolean isVolatile = false;

    IntValuedEnum<LLVMTypeKind> typeKind = pReturnType.getTypeKind();
    long tk = typeKind.value();

    if (tk == LLVMTypeKind.LLVMVoidTypeKind.value()) {
      return CVoidType.VOID;
    } else if (tk == LLVMTypeKind.LLVMIntegerTypeKind.value()) {
      int integerWidth = pReturnType.getIntTypeWidth();
      return getIntegerType(integerWidth, isConst, isVolatile);

    } else if (tk == LLVMTypeKind.LLVMFloatTypeKind.value()
        || tk == LLVMTypeKind.LLVMDoubleTypeKind.value()) {
      // TODO
    } else if (tk == LLVMTypeKind.LLVMArrayTypeKind.value()) {

    }
    return cType;
  }

  private CType getIntegerType(
      final int pIntegerWidth,
      final boolean pIsConst,
      final boolean pIsVolatile
  ) {
    final boolean isSigned = false;
    final boolean isComplex = false;
    final boolean isImaginary = false;
    final boolean isUnsigned = true;

    final boolean isLong = false;
    boolean isShort = false;
    boolean isLonglong = false;

    CBasicType basicType;

    switch (pIntegerWidth) {
      case 1:
        basicType = CBasicType.BOOL;
        break;
      case 2:
        basicType = CBasicType.INT;
        isShort = true;
        break;
      case 4:
        basicType = CBasicType.INT;
        // keep everything set to 'false' for default int
        break;
      case 8:
        basicType = CBasicType.INT;
        // We use long long since it is 8 bytes for both 32 and 64 bit machines
        isLonglong = true;
        break;
      default:
        throw new AssertionError("Unhandled integer bitwidth " + pIntegerWidth);
    }

    return new CSimpleType(
        pIsConst,
        pIsVolatile,
        basicType,
        isLong,
        isShort,
        isSigned,
        isUnsigned,
        isComplex,
        isImaginary,
        isLonglong
        );
  }

  @Override
  protected Behavior visitGlobalItem(final Value pItem) {
    return null;
  }

  public static class FunctionDefinition {

    private CFunctionDeclaration funcDecl;
    private CFunctionEntryNode entryNode;

    public FunctionDefinition(
        final CFunctionDeclaration pFuncDecl,
        final CFunctionEntryNode pEntryNode
    ) {
      funcDecl = pFuncDecl;
      entryNode = pEntryNode;
    }

    public CFunctionEntryNode getEntryNode() {
      return entryNode;
    }

    public void setEntryNode(final CFunctionEntryNode pEntryNode) {
      entryNode = pEntryNode;
    }

  }

  private FileLocation getLocation(final Value pItem) {
    return FileLocation.DUMMY;
  }
}
