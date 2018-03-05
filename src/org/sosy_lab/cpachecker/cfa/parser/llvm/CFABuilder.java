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
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.llvm_j.BasicBlock;
import org.sosy_lab.llvm_j.Function;
import org.sosy_lab.llvm_j.LLVMException;
import org.sosy_lab.llvm_j.Module;
import org.sosy_lab.llvm_j.TypeRef;
import org.sosy_lab.llvm_j.Value;
import org.sosy_lab.llvm_j.Value.OpCode;

/** CFA builder for LLVM IR. Metadata stored in the LLVM IR file is ignored. */
public class CFABuilder {
  // TODO: Thread Local Storage Model: May be important for concurrency
  // TODO: Aliases (@a = %b) and IFuncs (@a = ifunc @..)

  private static final String RETURN_VAR_NAME = "__retval__";
  private static final String TMP_VAR_PREFIX_LOCAL = "__t_";
  private static final String TMP_VAR_PREFIX_GLOBAL = "__tg_";

  private static final CFunctionDeclaration ABORT_FUNC_DECL =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          new CFunctionType(CVoidType.VOID, Collections.emptyList(), false),
          "abort",
          Collections.emptyList());
  private static final CExpression ABORT_FUNC_NAME =
      new CIdExpression(FileLocation.DUMMY, CVoidType.VOID, "abort", ABORT_FUNC_DECL);

  private static long tmpLocalVarCount = 0;
  private static long tmpGlobalVarCount = 0;

  private final LogManager logger;
  private final MachineModel machineModel;

  private final LlvmTypeConverter typeConverter;
  private CBinaryExpressionBuilder binaryExpressionBuilder;

  // Value address -> Variable declaration
  private final Map<Long, CSimpleDeclaration> variableDeclarations;
  // Function name -> Function declaration
  private Map<String, CFunctionDeclaration> functionDeclarations;

  // unnamed basic blocks will be named as 1,2,3,...
  private int basicBlockId;
  protected SortedMap<String, FunctionEntryNode> functions;

  protected SortedSetMultimap<String, CFANode> cfaNodes;
  protected List<Pair<ADeclaration, String>> globalDeclarations;

  public CFABuilder(final LogManager pLogger, final MachineModel pMachineModel) {
    logger = pLogger;
    machineModel = pMachineModel;

    typeConverter = new LlvmTypeConverter(pMachineModel, pLogger);

    variableDeclarations = new HashMap<>();
    functionDeclarations = new HashMap<>();

    binaryExpressionBuilder = new CBinaryExpressionBuilder(machineModel, logger);
    basicBlockId = 0;

    functions = new TreeMap<>();
    cfaNodes = TreeMultimap.create();
    globalDeclarations = new ArrayList<>();
  }

  public ParseResult build(final Module pModule, final String pFilename) throws LLVMException {
    visit(pModule, pFilename);
    List<Path> input_file = ImmutableList.of(Paths.get(pFilename));

    return new ParseResult(functions, cfaNodes, globalDeclarations, input_file);
  }

  public void visit(final Module pItem, final String pFileName) throws LLVMException {
    if (pItem.getFirstFunction() == null) {
      return;
    }

    addFunctionDeclarations(pItem, pFileName);

    /* create globals */
    iterateOverGlobals(pItem, pFileName);

    /* create CFA for all functions */
    iterateOverFunctions(pItem, pFileName);
  }

  private void addFunctionDeclarations(final Module pItem, final String pFileName)
      throws LLVMException {
    for (Value func : pItem) {
      String funcName = func.getValueName();
      assert !funcName.isEmpty();

      // XXX: may just check for generic intrinsic?
      if (funcName.startsWith("llvm.")) {
        continue;
      }

      declareFunction(func, pFileName);
    }
  }

  private void iterateOverGlobals(final Module pItem, final String pFileName) throws LLVMException {
    Value globalItem = pItem.getFirstGlobal();
    /* no globals? */
    if (globalItem == null) {
      return;
    }

    Value globalItemLast = pItem.getLastGlobal();
    assert globalItemLast != null;

    while (true) {
      ADeclaration decl = visitGlobalItem(globalItem, pFileName);

      globalDeclarations.add(Pair.of(decl, globalItem.toString()));

      /* we processed the last global variable? */
      if (globalItem.equals(globalItemLast)) {
        break;
      }

      globalItem = globalItem.getNextGlobal();
    }
  }

  protected void addNode(String funcName, CFANode nd) {
    cfaNodes.put(funcName, nd);
  }

  private void addEdge(CFAEdge edge) {
    edge.getPredecessor().addLeavingEdge(edge);
    edge.getSuccessor().addEnteringEdge(edge);
  }

  private void iterateOverFunctions(final Module pItem, final String pFileName)
      throws LLVMException {
    Function lastFunc = pItem.getLastFunction().asFunction();
    Function currFunc = null;
    do {
      if (currFunc == null) {
        currFunc = pItem.getFirstFunction().asFunction();
      } else {
        currFunc = currFunc.getNextFunction().asFunction();
      }

      if (currFunc.isDeclaration()) {
        continue;
      }

      String funcName = currFunc.getValueName();
      assert !funcName.isEmpty();

      // XXX: may just check for generic intrinsic?
      if (funcName.startsWith("llvm.")) {
        continue;
      }

      // handle the function definition
      FunctionEntryNode en = visitFunction(currFunc, pFileName);
      assert en != null;
      addNode(funcName, en);

      // create the basic blocks and instructions of the function.
      // A basic block is mapped to a pair <entry node, exit node>
      SortedMap<Integer, BasicBlockInfo> basicBlocks = new TreeMap<>();
      CLabelNode entryBB = iterateOverBasicBlocks(currFunc, en, funcName, basicBlocks, pFileName);

      // add the edge from the entry of the function to the first
      // basic block
      // BlankEdge.buildNoopEdge(en, entryBB);
      addEdge(new BlankEdge("entry", en.getFileLocation(), en, entryBB, "Function start edge"));

      // add branching between instructions
      addJumpsBetweenBasicBlocks(currFunc, basicBlocks, pFileName);

      functions.put(funcName, en);

    } while (!currFunc.equals(lastFunc));
  }

  /**
   * Iterate over basic blocks of a function.
   *
   * <p>Add a label created for every basic block to a mapping passed as an argument.
   *
   * @return the entry basic block (as a CLabelNode).
   */
  private CLabelNode iterateOverBasicBlocks(
      final Function pFunction,
      final FunctionEntryNode pEntryNode,
      final String pFuncName,
      final SortedMap<Integer, BasicBlockInfo> pBasicBlocks,
      final String pFileName)
      throws LLVMException {
    if (pFunction.countBasicBlocks() == 0) {
      return null;
    }

    CLabelNode entryBB = null;
    for (BasicBlock block : pFunction) {
      // process this basic block
      CLabelNode label = new CLabelNode(pFuncName, getBBName(block));
      addNode(pFuncName, label);
      if (entryBB == null) {
        entryBB = label;
      }

      BasicBlockInfo bbi =
          handleInstructions(pEntryNode.getExitNode(), pFuncName, block, pFileName);
      pBasicBlocks.put(block.hashCode(), new BasicBlockInfo(label, bbi.getExitNode()));

      // add an edge from label to the first node
      // of this basic block
      addEdge(
          new BlankEdge(
              "label_to_first",
              pEntryNode.getFileLocation(),
              label,
              bbi.getEntryNode(),
              "edge to first instr"));
    }

    assert entryBB != null || pBasicBlocks.isEmpty();
    return entryBB;
  }

  /** Add branching edges between first and last nodes of basic blocks. */
  private void addJumpsBetweenBasicBlocks(
      final Function pFunction,
      final SortedMap<Integer, BasicBlockInfo> pBasicBlocks,
      final String pFileName)
      throws LLVMException {
    // for every basic block, get the last instruction and
    // add edges from it to labels where it jumps
    for (BasicBlock bb : pFunction) {
      Value terminatorInst = bb.getLastInstruction();
      if (terminatorInst == null) {
        continue;
      }

      assert terminatorInst.isTerminatorInst();
      assert pBasicBlocks.containsKey(bb.hashCode());
      CFANode brNode = pBasicBlocks.get(bb.hashCode()).getExitNode();

      int succNum = terminatorInst.getNumSuccessors();
      if (succNum == 0) {
        continue;
      } else if (succNum == 1) {
        BasicBlock succ = terminatorInst.getSuccessor(0);
        CLabelNode label = (CLabelNode) pBasicBlocks.get(succ.hashCode()).getEntryNode();

        addEdge(new BlankEdge("(goto)", FileLocation.DUMMY, brNode, label, "(goto)"));
        continue;
      }

      // switch is not supported yet
      assert succNum == 2;

      // get the operands and add branching edges
      CExpression condition =
          getBranchCondition(terminatorInst, pFunction.getValueName(), pFileName);

      BasicBlock succ = terminatorInst.getSuccessor(0);
      CLabelNode label = (CLabelNode) pBasicBlocks.get(succ.hashCode()).getEntryNode();
      addEdge(
          new CAssumeEdge(
              condition.toASTString(),
              condition.getFileLocation(),
              brNode,
              label,
              condition,
              true));

      succ = terminatorInst.getSuccessor(1);
      label = (CLabelNode) pBasicBlocks.get(succ.hashCode()).getEntryNode();
      addEdge(
          new CAssumeEdge(
              condition.toASTString(),
              condition.getFileLocation(),
              brNode,
              label,
              condition,
              false));
    }
  }

  private String getBBName(BasicBlock BB) {
    Value bbValue = BB.basicBlockAsValue();
    String labelStr = bbValue.getValueName();
    if (labelStr.isEmpty()) {
      return Integer.toString(++basicBlockId);
    } else {
      return labelStr;
    }
  }

  private CFANode newNode(String funcName) {
    CFANode nd = new CFANode(funcName);
    addNode(funcName, nd);

    return nd;
  }

  /** Create a chain of nodes and edges corresponding to one basic block. */
  private BasicBlockInfo handleInstructions(
      final FunctionExitNode exitNode,
      final String funcName,
      final BasicBlock pItem,
      final String pFileName)
      throws LLVMException {
    assert pItem.getFirstInstruction() != null; // empty BB not supported

    Value lastI = pItem.getLastInstruction();
    assert lastI != null;

    CFANode prevNode = newNode(funcName);
    CFANode firstNode = prevNode;
    CFANode curNode = null;

    for (Value i : pItem) {
      if (i.isDbgInfoIntrinsic() || i.isDbgDeclareInst()) {
        continue;

      } else if (i.isSelectInst()) {
        CDeclaration decl = (CDeclaration) getAssignedVarDeclaration(i, funcName, null, pFileName);
        curNode = newNode(funcName);
        addEdge(
            new CDeclarationEdge(
                decl.toASTString(), decl.getFileLocation(), prevNode, curNode, decl));
        prevNode = curNode;

        assert i.getNumOperands() == 3
            : "Select statement doesn't have 3 operands, but " + i.getNumOperands();
        Value condition = i.getOperand(0);
        Value valueIf = i.getOperand(1);
        Value valueElse = i.getOperand(2);

        CType ifType = typeConverter.getCType(valueIf.typeOf());
        assert ifType.equals(typeConverter.getCType(valueElse.typeOf()));
        CExpression conditionC = getBranchCondition(condition, funcName, pFileName);
        CExpression trueValue = getExpression(valueIf, ifType, pFileName);
        CStatement trueAssignment =
            (CStatement) getAssignStatement(i, trueValue, funcName, pFileName).get(0);
        // we can use ifType again, since ifType == elseType for `select` instruction
        CExpression falseValue = getExpression(valueElse, ifType, pFileName);
        CStatement falseAssignment =
            (CStatement) getAssignStatement(i, falseValue, funcName, pFileName).get(0);

        CFANode trueNode = newNode(funcName);
        CFANode falseNode = newNode(funcName);
        CAssumeEdge trueBranch =
            new CAssumeEdge(
                conditionC.toASTString(),
                conditionC.getFileLocation(),
                prevNode,
                trueNode,
                conditionC,
                true);
        addEdge(trueBranch);
        CAssumeEdge falseBranch =
            new CAssumeEdge(
                conditionC.toASTString(),
                conditionC.getFileLocation(),
                prevNode,
                falseNode,
                conditionC,
                false);
        addEdge(falseBranch);

        prevNode = trueNode;
        trueNode = newNode(funcName);
        CStatementEdge trueAssign =
            new CStatementEdge(
                trueAssignment.toASTString(),
                trueAssignment,
                trueAssignment.getFileLocation(),
                prevNode,
                trueNode);
        addEdge(trueAssign);

        prevNode = falseNode;
        falseNode = newNode(funcName);
        CStatementEdge falseAssign =
            new CStatementEdge(
                falseAssignment.toASTString(),
                falseAssignment,
                falseAssignment.getFileLocation(),
                prevNode,
                falseNode);
        addEdge(falseAssign);

        curNode = newNode(funcName);
        BlankEdge trueMeet =
            new BlankEdge("", falseAssignment.getFileLocation(), trueNode, curNode, "");
        addEdge(trueMeet);

        BlankEdge falseMeet =
            new BlankEdge("", falseAssignment.getFileLocation(), falseNode, curNode, "");
        addEdge(falseMeet);

        prevNode = curNode;

      } else {

        // process this basic block
        List<CAstNode> expressions = visitInstruction(i, funcName, pFileName);
        if (expressions == null) {
          curNode = newNode(funcName);
          addEdge(new BlankEdge(i.toString(), FileLocation.DUMMY, prevNode, curNode, "noop"));
          prevNode = curNode;
          continue;
        }

        for (CAstNode expr : expressions) {
          FileLocation exprLocation = expr.getFileLocation();
          // build an edge with this expression over it
          if (expr instanceof CDeclaration) {
            curNode = newNode(funcName);
            addEdge(
                new CDeclarationEdge(
                    expr.toASTString(), exprLocation, prevNode, curNode, (CDeclaration) expr));
          } else if (expr instanceof CReturnStatement) {
            curNode = exitNode;
            addEdge(
                new CReturnStatementEdge(
                    i.toString(), (CReturnStatement) expr, exprLocation, prevNode, exitNode));
          } else if (i.isUnreachableInst()) {
            curNode = exitNode;
            addEdge(new BlankEdge(i.toString(), exprLocation, prevNode, curNode, "unreachable"));
          } else {
            curNode = newNode(funcName);
            addEdge(
                new CStatementEdge(
                    expr.toASTString() + i.toString(),
                    (CStatement) expr,
                    exprLocation,
                    prevNode,
                    curNode));
          }

          prevNode = curNode;
        }
      }
    }

    assert curNode != null;
    return new BasicBlockInfo(firstNode, curNode);
  }

  private static class BasicBlockInfo {
    private CFANode entryNode;
    private CFANode exitNode;

    public BasicBlockInfo(CFANode entry, CFANode exit) {
      entryNode = entry;
      exitNode = exit;
    }

    public CFANode getEntryNode() {
      return entryNode;
    }

    public CFANode getExitNode() {
      return exitNode;
    }

    @Override
    public String toString() {
      return "BasicBlock " + entryNode.toString() + " -> " + exitNode.toString();
    }
  }

  protected FunctionEntryNode visitFunction(final Value pItem, final String pFileName)
      throws LLVMException {
    assert pItem.isFunction();

    logger.log(Level.FINE, "Creating function: " + pItem.getValueName());

    return handleFunctionDefinition(pItem, pFileName);
  }

  private CExpression getBranchCondition(final Value pItem, String funcName, final String pFileName)
      throws LLVMException {
    CExpression condition;
    if (pItem.isConditional()) {
      Value cond = pItem.getCondition();
      CType expectedType = typeConverter.getCType(cond.typeOf());
      condition = getExpression(cond, expectedType, pFileName);
    } else {
      condition = getAssignedIdExpression(pItem, CNumericTypes.BOOL, pFileName);
    }
    try {
      return binaryExpressionBuilder.buildBinaryExpression(
          condition,
          new CIntegerLiteralExpression(
              getLocation(pItem, pFileName), CNumericTypes.BOOL, BigInteger.ONE),
          BinaryOperator.EQUALS);
    } catch (UnrecognizedCCodeException e) {
      throw new AssertionError(e.toString());
    }
  }

  private List<CAstNode> visitInstruction(
      final Value pItem, final String pFunctionName, final String pFileName) throws LLVMException {
    assert pItem.isInstruction();

    if (pItem.isAllocaInst()) {
      return handleAlloca(pItem, pFunctionName, pFileName);

    } else if (pItem.isReturnInst()) {
      return handleReturn(pItem, pFunctionName, pFileName);
    } else if (pItem.isUnreachableInst()) {
      return handleUnreachable(pItem, pFileName);

    } else if (pItem.isBinaryOperator() || pItem.isGetElementPtrInst()) {
      return handleOpCode(pItem, pFunctionName, pFileName, pItem.getOpCode());
    } else if (pItem.isUnaryInstruction()) {
      return handleUnaryOp(pItem, pFunctionName, pFileName);
    } else if (pItem.isStoreInst()) {
      return handleStore(pItem, pFunctionName, pFileName);
    } else if (pItem.isCallInst()) {
      return handleCall(pItem, pFunctionName, pFileName);
    } else if (pItem.isCmpInst()) {
      return handleCmpInst(pItem, pFunctionName, pFileName);
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

  private List<CAstNode> handleCall(
      final Value pItem, final String pCallingFunctionName, final String pFileName)
      throws LLVMException {
    assert pItem.isCallInst();
    FileLocation loc = getLocation(pItem, pFileName);
    Value calledFunction = pItem.getCalledFunction();
    CType returnType = typeConverter.getCType(pItem.typeOf());
    int argumentCount = pItem.getNumArgOperands();
    String functionName = calledFunction.getValueName();
    // May be null and that's ok - CPAchecker will handle the call as a call to a builtin function,
    // then
    CFunctionDeclaration functionDeclaration = functionDeclarations.get(functionName);

    CIdExpression functionNameExp;
    List<CExpression> parameters = new ArrayList<>(argumentCount);
    CFunctionType functionType;

    if (functionDeclaration == null) {
      logger.logf(
          Level.WARNING,
          "Declaration for function %s not found, trying to derive it.",
          functionName);
      // Try to derive a function type from the call
      List<CType> parameterTypes = new ArrayList<>(argumentCount - 1);
      for (int i = 0; i < argumentCount; i++) {
        Value functionArg = pItem.getArgOperand(i);
        assert functionArg.isConstant()
            || variableDeclarations.containsKey(functionArg.getAddress());
        CType expectedType = typeConverter.getCType(functionArg.typeOf());
        parameterTypes.add(expectedType);

        if (functionArg.isConstant()) {
          parameters.add(getConstant(functionArg, pFileName));
        } else {
          assert variableDeclarations.containsKey(functionArg.getAddress());
          parameters.add(getAssignedIdExpression(functionArg, expectedType, pFileName));
        }
      }

      functionType = new CFunctionType(returnType, parameterTypes, false);
      functionNameExp = new CIdExpression(loc, functionType, functionName, null);
    } else {
      functionNameExp =
          new CIdExpression(loc, functionDeclaration.getType(), functionName, functionDeclaration);

      List<CParameterDeclaration> parameterDeclarations = functionDeclaration.getParameters();
      // i = 1 to skip the function name, we only want to look at arguments
      for (int i = 0; i < argumentCount; i++) {
        Value functionArg = pItem.getArgOperand(i);
        CType expectedType = parameterDeclarations.get(i).getType();

        assert functionArg.isConstant()
            || variableDeclarations.containsKey(functionArg.getAddress());
        parameters.add(getExpression(functionArg, expectedType, pFileName));
      }
    }

    CFunctionCallExpression callExpression =
        new CFunctionCallExpression(
            loc, returnType, functionNameExp, parameters, functionDeclaration);

    if (returnType.equals(CVoidType.VOID)) {
      return ImmutableList.of(new CFunctionCallStatement(loc, callExpression));
    } else {
      return getAssignStatement(pItem, callExpression, pCallingFunctionName, pFileName);
    }
  }

  private List<CAstNode> handleUnreachable(final Value pItem, final String pFileName) {
    CFunctionCallExpression callExpression =
        new CFunctionCallExpression(
            getLocation(pItem, pFileName),
            CVoidType.VOID,
            ABORT_FUNC_NAME,
            Collections.emptyList(),
            ABORT_FUNC_DECL);

    return ImmutableList.of(
        new CFunctionCallStatement(getLocation(pItem, pFileName), callExpression));
  }

  private List<CAstNode> handleUnaryOp(
      final Value pItem, final String pFunctionName, final String pFileName) throws LLVMException {
    if (pItem.isLoadInst()) {
      return handleLoad(pItem, pFunctionName, pFileName);
    } else if (pItem.isCastInst()) {
      return handleCastInst(pItem, pFunctionName, pFileName);
    } else {
      throw new UnsupportedOperationException(
          "LLVM does not yet support operator with opcode " + pItem.getOpCode());
    }
  }

  private List<CAstNode> handleLoad(
      final Value pItem, final String pFunctionName, final String pFileName) throws LLVMException {
    CType expectedType = typeConverter.getCType(pItem.typeOf());
    CExpression expression = getAssignedIdExpression(pItem.getOperand(0), expectedType, pFileName);
    return getAssignStatement(pItem, expression, pFunctionName, pFileName);
  }

  private List<CAstNode> handleStore(
      final Value pItem, final String pFunctionName, final String pFileName) throws LLVMException {
    Value valueToStoreTo = pItem.getOperand(1);
    Value valueToLoad = pItem.getOperand(0);

    CType expectedType = typeConverter.getCType(valueToLoad.typeOf());
    CExpression expression = getExpression(valueToLoad, expectedType, pFileName);

    return getAssignStatement(valueToStoreTo, expression, pFunctionName, pFileName);
  }

  private List<CAstNode> handleAlloca(final Value pItem, String pFunctionName, String pFileName)
      throws LLVMException {
    // We ignore the specifics and handle alloca statements like C declarations of variables
    CSimpleDeclaration assignedVar =
        getAssignedVarDeclaration(pItem, pFunctionName, null, pFileName);
    return ImmutableList.of(assignedVar);
  }

  private List<CAstNode> handleReturn(
      final Value pItem, final String pFuncName, final String pFileName) throws LLVMException {
    Value returnVal = pItem.getReturnValue();
    Optional<CExpression> maybeExpression;
    Optional<CAssignment> maybeAssignment;
    if (returnVal == null) {
      maybeExpression = Optional.absent();
      maybeAssignment = Optional.absent();

    } else {
      CType expectedType = typeConverter.getCType(returnVal.typeOf());
      CExpression returnExp = getExpression(returnVal, expectedType, pFileName);
      maybeExpression = Optional.of(returnExp);

      CSimpleDeclaration returnVarDecl =
          getReturnVar(pFuncName, returnExp.getExpressionType(), returnExp.getFileLocation());

      CIdExpression returnVar = new CIdExpression(getLocation(returnVal, pFileName), returnVarDecl);

      CAssignment returnVarAssignment =
          new CExpressionAssignmentStatement(
              getLocation(returnVal, pFileName), returnVar, returnExp);
      maybeAssignment = Optional.of(returnVarAssignment);
    }

    return ImmutableList.of(
        new CReturnStatement(getLocation(pItem, pFileName), maybeExpression, maybeAssignment));
  }

  private String getQualifiedName(String pVarName, String pFuncName) {
    return pFuncName + "::" + pVarName;
  }

  private List<CAstNode> handleOpCode(
      final Value pItem, String pFunctionName, final String pFileName, final OpCode pOpCode)
      throws LLVMException {
    CExpression expression = createFromOpCode(pItem, pFunctionName, pFileName, pOpCode);
    return getAssignStatement(pItem, expression, pFunctionName, pFileName);
  }

  private CExpression createFromOpCode(
      final Value pItem, String pFunctionName, final String pFileName, final OpCode pOpCode)
      throws LLVMException {

    switch (pOpCode) {
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
        return createFromArithmeticOp(pItem, pOpCode, pFunctionName, pFileName);

      case GetElementPtr:
        return createGepExp(pItem, pFileName);

        // Comparison operations
      case ICmp:
      case FCmp:
        // fall through

        // Select operator
      case Select:
        // fall through

        // Sign extension/truncation operations
      case Trunc:
        // fall through
      case ZExt:
        // fall through
      case SExt:
        // fall through
      case FPToUI:
        // fall through
      case FPToSI:
        // fall through
      case UIToFP:
        // fall through
      case SIToFP:
        // fall through
      case FPTrunc:
        // fall through
      case FPExt:
        // fall through
      case PtrToInt:
        // fall through
      case IntToPtr:
        // fall through
      case BitCast:
        // fall through
      case AddrSpaceCast:
        // fall through

        // Aggregate operations
      case ExtractValue:
        // fall through
      case InsertValue:
        // fall through

      case PHI:
        // fall through

      case UserOp1:
        // fall through
      case UserOp2:
        // fall through
      case VAArg:
        // fall through

        // Vector operations
      case ExtractElement:
        // fall through
      case InsertElement:
        // fall through
      case ShuffleVector:
        // fall through

        // Concurrency-centric operations
      case Fence:
        // fall through

      case AtomicCmpXchg:
        // fall through
      case AtomicRMW:
        // fall through
      default:
        throw new UnsupportedOperationException(pOpCode.toString());
    }
  }

  private CExpression createFromArithmeticOp(
      final Value pItem, final OpCode pOpCode, final String pFunctionName, final String pFileName)
      throws LLVMException {
    final CType expressionType = typeConverter.getCType(pItem.typeOf());

    // TODO: Currently we only support flat expressions, no nested ones. Make this work
    // in the future.
    Value operand1 = pItem.getOperand(0); // First operand
    logger.log(Level.FINE, "Getting id expression for operand 1");
    CType op1type = typeConverter.getCType(operand1.typeOf());
    CExpression operand1Exp = getExpression(operand1, op1type, pFileName);
    Value operand2 = pItem.getOperand(1); // Second operand
    CType op2type = typeConverter.getCType(operand2.typeOf());
    logger.log(Level.FINE, "Getting id expression for operand 2");
    CExpression operand2Exp = getExpression(operand2, op2type, pFileName);

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

    CExpression expression =
        new CBinaryExpression(
            getLocation(pItem, pFileName),
            expressionType,
            expressionType, // calculation type is expression type in LLVM
            operand1Exp,
            operand2Exp,
            operation);

    return expression;
  }

  private CExpression getExpression(
      final Value pItem, final CType pExpectedType, final String pFileName) throws LLVMException {

    if (pItem.isConstantExpr()) {
      return createFromOpCode(pItem, "", pFileName, pItem.getConstOpCode());

    } else if (pItem.isConstant() && !pItem.isGlobalVariable()) {
      return getConstant(pItem, pFileName);

    } else {
      return getAssignedIdExpression(pItem, pExpectedType, pFileName);
    }
  }

  private CExpression getConstant(final Value pItem, final String pFileName) throws LLVMException {
    CType expectedType = typeConverter.getCType(pItem.typeOf());
    if (pItem.isConstantInt()) {
      long constantValue = pItem.constIntGetSExtValue();
      return new CIntegerLiteralExpression(
          getLocation(pItem, pFileName), expectedType, BigInteger.valueOf(constantValue));

    } else if (pItem.isConstantPointerNull()) {
      FileLocation location = getLocation(pItem, pFileName);
      return new CPointerExpression(location, expectedType, getNull(location, expectedType));

    } else if (pItem.isConstantExpr()) {
      return getExpression(pItem, expectedType, pFileName);

    } else {
      assert pItem.isConstantFP() : "Unhandled constant is not floating point constant: " + pItem;
      throw new UnsupportedOperationException("LLVM parsing does not support float constants yet");
    }
  }

  private CExpression getNull(final FileLocation pLocation, final CType pType) {
    return new CIntegerLiteralExpression(pLocation, pType, BigInteger.ZERO);
  }

  private CInitializer getConstantAggregateInitializer(
      final Value pAggregate, final String pFileName) throws LLVMException {

    int length = getLength(pAggregate);
    List<CInitializer> elementInitializers = new ArrayList<>(length);

    for (int i = 0; i < length; i++) {
      Value element = pAggregate.getElementAsConstant(i);
      assert element.isConstant() : "Value element is not a constant!";
      CInitializer elementInitializer;
      if (element.isConstantArray()) {
        elementInitializer = getConstantAggregateInitializer(element, pFileName);
      } else {
        elementInitializer =
            new CInitializerExpression(
                getLocation(element, pFileName), getConstant(element, pFileName));
      }
      elementInitializers.add(elementInitializer);
    }

    CInitializerList aggregateInitializer =
        new CInitializerList(getLocation(pAggregate, pFileName), elementInitializers);
    return aggregateInitializer;
  }

  private int getLength(Value pAggregateValue) throws LLVMException {
    CArrayType arrayType = (CArrayType) typeConverter.getCType(pAggregateValue.typeOf());
    OptionalInt maybeArrayLength = arrayType.getLengthAsInt();
    assert maybeArrayLength.isPresent() : "Constant array has non-constant length";
    return maybeArrayLength.getAsInt();
  }

  private List<CAstNode> getAssignStatement(
      final Value pAssignee,
      final CRightHandSide pAssignment,
      final String pFunctionName,
      final String pFileName)
      throws LLVMException {
    long assigneeId = pAssignee.getAddress();
    CType expectedType = pAssignment.getExpressionType();
    // Variable is already declared, so it must only be assigned the new value
    if (variableDeclarations.containsKey(assigneeId)) {
      CLeftHandSide assigneeIdExp =
          (CLeftHandSide) getAssignedIdExpression(pAssignee, expectedType, pFileName);

      CType varType = assigneeIdExp.getExpressionType();
      if (!(varType.equals(expectedType))) {
        assert expectedType instanceof CPointerType;
        assigneeIdExp =
            new CPointerExpression(getLocation(pAssignee, pFileName), varType, assigneeIdExp);
      }

      if (pAssignment instanceof CFunctionCallExpression) {
        return ImmutableList.of(
            new CFunctionCallAssignmentStatement(
                getLocation(pAssignee, pFileName),
                assigneeIdExp,
                (CFunctionCallExpression) pAssignment));

      } else {
        return ImmutableList.of(
            new CExpressionAssignmentStatement(
                getLocation(pAssignee, pFileName), assigneeIdExp, (CExpression) pAssignment));
      }

    } else { // Variable must be newly declared
      if (pAssignment instanceof CFunctionCallExpression) {
        CSimpleDeclaration assigneeDecl =
            getAssignedVarDeclaration(pAssignee, pFunctionName, null, pFileName);
        CLeftHandSide assigneeIdExp =
            (CLeftHandSide) getAssignedIdExpression(pAssignee, expectedType, pFileName);

        return ImmutableList.of(
            assigneeDecl,
            new CFunctionCallAssignmentStatement(
                getLocation(pAssignee, pFileName),
                assigneeIdExp,
                (CFunctionCallExpression) pAssignment));

      } else {
        CInitializer initializer =
            new CInitializerExpression(
                getLocation(pAssignee, pFileName), (CExpression) pAssignment);
        CSimpleDeclaration assigneeDecl =
            getAssignedVarDeclaration(pAssignee, pFunctionName, initializer, pFileName);
        return ImmutableList.of(assigneeDecl);
      }
    }
  }

  private CSimpleDeclaration getAssignedVarDeclaration(
      final Value pItem,
      final String pFunctionName,
      final CInitializer pInitializer,
      final String pFileName)
      throws LLVMException {
    final long itemId = pItem.getAddress();
    if (!variableDeclarations.containsKey(itemId)) {
      String assignedVar = getName(pItem);

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
      if (isGlobal && varType instanceof CPointerType) {
        varType = ((CPointerType) varType).getType();
      }

      CSimpleDeclaration newDecl =
          new CVariableDeclaration(
              getLocation(pItem, pFileName),
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

  private CExpression getAssignedIdExpression(
      final Value pItem, final CType pExpectedType, final String pFileName) throws LLVMException {
    logger.log(Level.FINE, "Getting var declaration for item");

    if(!variableDeclarations.containsKey(pItem.getAddress())) {
      throw new LLVMException("ID expression has no declaration: " + pItem);
    }

    CSimpleDeclaration assignedVarDeclaration = variableDeclarations.get(pItem.getAddress());
    String assignedVarName = assignedVarDeclaration.getName();
    CType expressionType = assignedVarDeclaration.getType();
    CIdExpression idExpression =
        new CIdExpression(
            getLocation(pItem, pFileName), expressionType, assignedVarName, assignedVarDeclaration);

    if (expressionType.canBeAssignedFrom(pExpectedType)) {
      return idExpression;

    } else if (pointerOf(pExpectedType, expressionType)) {
      CType typePointingTo = ((CPointerType) pExpectedType).getType().getCanonicalType();
      if (expressionType.canBeAssignedFrom(typePointingTo)
          || expressionType.equals(typePointingTo)) {
        return new CUnaryExpression(
            getLocation(pItem, pFileName), pExpectedType, idExpression, UnaryOperator.AMPER);
      } else {
        throw new AssertionError("Unhandled type structure");
      }
    } else if (expressionType instanceof CPointerType) {
      return new CPointerExpression(getLocation(pItem, pFileName), pExpectedType, idExpression);
    } else {
      throw new AssertionError("Unhandled types structure");
    }
  }

  /**
   * Returns whether the first param is a pointer of the type of the second parameter.<br>
   * Examples:
   *
   * <ul>
   *   <li>pointerOf(*int, int) -> true
   *   <li>pointerOf(**int, *int) -> true
   *   <li>pointerOf(int, int*) -> false
   *   <li>pointerOf(int, int) -> false
   * </ul>
   */
  private boolean pointerOf(CType pPotentialPointer, CType pPotentialPointee) {
    if (pPotentialPointer instanceof CPointerType) {
      return ((CPointerType) pPotentialPointer)
          .getType()
          .getCanonicalType()
          .equals(pPotentialPointee.getCanonicalType());
    } else {
      return false;
    }
  }

  private String getName(final Value pValue) {
    String name = pValue.getValueName();
    if (name.isEmpty()) {
      name = getTempVar(pValue.isGlobalValue());
    }
    return prepareName(name);
  }

  private String getTempVar(boolean pIsGlobal) {
    String var_prefix;
    String var_suffix;
    if (pIsGlobal) {
      var_prefix = TMP_VAR_PREFIX_GLOBAL;
      var_suffix = Long.toString(tmpGlobalVarCount++);
    } else {
      var_prefix = TMP_VAR_PREFIX_LOCAL;
      var_suffix = Long.toString(tmpLocalVarCount++);
    }
    return var_prefix + var_suffix;
  }

  // Converts a valid LLVM name to a valid C name, so that CPAchecker
  // can work with it without problems.
  private String prepareName(String pRawName) {
    char[] asArray = pRawName.toCharArray();
    StringBuilder newName = new StringBuilder();
    for (int i = 0; i < asArray.length; i++) {
      char curr = asArray[i];
      if (curr == '_' || Character.isAlphabetic(curr) || (i > 0 && Character.isDigit(curr))) {
        newName.append(curr);
      } else {
        if (i == 0) {
          newName.append('_');
        }
        // Represent chars that are not allowed as their number representation
        newName.append((int) curr);
      }
    }
    return newName.toString();
  }

  private void declareFunction(final Value pFuncDef, final String pFileName) throws LLVMException {
    String functionName = pFuncDef.getValueName();

    // Function type
    TypeRef functionType = pFuncDef.typeOf();
    TypeRef elemType = functionType.getElementType();
    CFunctionType cFuncType = (CFunctionType) typeConverter.getCType(elemType);

    // Parameters
    List<Value> paramVs = pFuncDef.getParams();
    List<CParameterDeclaration> parameters = new ArrayList<>(paramVs.size());
    for (Value v : paramVs) {
      String paramName = getName(v);

      CType paramType = typeConverter.getCType(v.typeOf());
      CParameterDeclaration parameter =
          new CParameterDeclaration(getLocation(v, pFileName), paramType, paramName);
      parameter.setQualifiedName(getQualifiedName(paramName, functionName));

      variableDeclarations.put(v.getAddress(), parameter);
      parameters.add(parameter);
    }

    // Function declaration, exit
    CFunctionDeclaration functionDeclaration =
        new CFunctionDeclaration(
            getLocation(pFuncDef, pFileName), cFuncType, functionName, parameters);
    functionDeclarations.put(functionName, functionDeclaration);
  }

  private FunctionEntryNode handleFunctionDefinition(final Value pFuncDef, final String pFileName)
      throws LLVMException {
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
      FileLocation returnVarLocation = getLocation(pFuncDef, pFileName);
      CVariableDeclaration returnVarDecl =
          getReturnVar(functionName, returnType, returnVarLocation);
      returnVar = Optional.of(returnVarDecl);
    }

    CFunctionDeclaration functionDeclaration = functionDeclarations.get(functionName);
    FunctionEntryNode entry =
        new CFunctionEntryNode(
            getLocation(pFuncDef, pFileName), functionDeclaration, functionExit, returnVar);
    functionExit.setEntryNode(entry);

    return entry;
  }

  private CVariableDeclaration getReturnVar(
      String pFunctionName, CType pType, FileLocation pLocation) {
    return new CVariableDeclaration(
        pLocation,
        false,
        CStorageClass.AUTO,
        pType,
        RETURN_VAR_NAME,
        RETURN_VAR_NAME,
        getQualifiedName(RETURN_VAR_NAME, pFunctionName),
        null /* no initializer */);
  }

  private CExpression createGepExp(final Value pItem, final String pFileName) throws LLVMException {

    CType baseType = typeConverter.getCType(pItem.getOperand(0).typeOf());
    Value startPointer = pItem.getOperand(0);
    assert typeConverter.getCType(startPointer.typeOf()) instanceof CPointerType
        : "Start of getelementptr is not a pointer";

    FileLocation fileLocation = getLocation(pItem, pFileName);

    CType currentType = baseType;
    CType oldType; // used to check that type was actually updated at each iteration
    CExpression currentExpression = getExpression(startPointer, currentType, pFileName);
    currentType = baseType;
    assert pItem.getNumOperands() >= 2
        : "Too few operands in GEP operation : " + pItem.getNumOperands();
    for (int i = 1; i < pItem.getNumOperands(); i++) {
      oldType = currentType;
      Value indexValue = pItem.getOperand(i);
      CExpression index = getExpression(indexValue, CNumericTypes.INT, pFileName);

      if (currentType instanceof CPointerType) {
        currentExpression =
            new CPointerExpression(
                fileLocation,
                currentType,
                new CBinaryExpression(
                    fileLocation,
                    currentType,
                    currentType,
                    currentExpression,
                    index,
                    BinaryOperator.PLUS));
        currentType = ((CPointerType) currentType).getType();

      } else if (currentType instanceof CArrayType) {
        currentExpression =
            new CArraySubscriptExpression(fileLocation, currentType, currentExpression, index);
        currentType = ((CArrayType) currentType).getType();

      } else if (currentType instanceof CCompositeType) {
        if (!(index instanceof CIntegerLiteralExpression)) {
          throw new UnsupportedOperationException(
              "GEP index to struct only allows integer " + "constant, but is " + index);
        }
        int memberIndex = ((CIntegerLiteralExpression) index).getValue().intValue();
        CCompositeTypeMemberDeclaration field =
            ((CCompositeType) currentType).getMembers().get(memberIndex);
        String fieldName = field.getName();
        currentExpression =
            new CFieldReference(fileLocation, currentType, fieldName, currentExpression, false);
        currentType = field.getType();
      }

      assert oldType != currentType : "Type didn't change in iteration: " + currentType;
    }
    return currentExpression;
  }

  private List<CAstNode> handleCmpInst(final Value pItem, String pFunctionName, String pFileName)
      throws LLVMException {
    // the only one supported now
    assert pItem.isICmpInst();
    boolean isSigned = true;

    BinaryOperator operator;
    switch (pItem.getICmpPredicate()) {
      case IntEQ:
        operator = BinaryOperator.EQUALS;
        break;
      case IntNE:
        operator = BinaryOperator.NOT_EQUALS;
        break;
      case IntUGT:
        isSigned = false;
        //$FALL-THROUGH$
      case IntSGT:
        operator = BinaryOperator.GREATER_THAN;
        break;
      case IntULT:
        isSigned = false;
        //$FALL-THROUGH$
      case IntSLT:
        operator = BinaryOperator.LESS_THAN;
        break;
      case IntULE:
        isSigned = false;
        //$FALL-THROUGH$
      case IntSLE:
        operator = BinaryOperator.LESS_EQUAL;
        break;
      case IntUGE:
        isSigned = false;
        //$FALL-THROUGH$
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
      CCastExpression op1Cast = new CCastExpression(
          getLocation(pItem, pFileName),
          typeConverter.getCType(operand1.typeOf(), isSigned),
          getExpression(operand1, op1type, pFileName));
      CCastExpression op2Cast = new CCastExpression(
          getLocation(pItem, pFileName),
          typeConverter.getCType(operand2.typeOf(), isSigned),
          getExpression(operand2, op2type, pFileName));

      CBinaryExpression cmp =
          binaryExpressionBuilder.buildBinaryExpression(op1Cast, op2Cast, operator);

      return getAssignStatement(pItem, cmp, pFunctionName, pFileName);

    } catch (UnrecognizedCCodeException e) {
      throw new UnsupportedOperationException(e.toString());
    }
  }

  private List<CAstNode> handleCastInst(final Value pItem, String pFunctionName, String pFileName)
      throws LLVMException {
    Value castOperand = pItem.getOperand(0);
    CType operandType = typeConverter.getCType(castOperand.typeOf());
    CCastExpression cast =
        new CCastExpression(
            getLocation(pItem, pFileName),
            typeConverter.getCType(pItem.typeOf()),
            getExpression(castOperand, operandType, pFileName));
    return getAssignStatement(pItem, cast, pFunctionName, pFileName);
  }

  private CDeclaration visitGlobalItem(final Value pItem, final String pFileName)
      throws LLVMException {
    assert pItem.isGlobalValue();

    CInitializer initializer;
    if (!pItem.isExternallyInitialized()) {
      Value initializerRaw = pItem.getInitializer();
      if (initializerRaw.isConstantArray()
          || initializerRaw.isConstantDataArray()
          || initializerRaw.isConstantVector()) {
        initializer = getConstantAggregateInitializer(initializerRaw, pFileName);
      } else if (initializerRaw.isConstantStruct()) {
        // TODO
        initializer = null;
      } else {
        initializer =
            new CInitializerExpression(
                getLocation(pItem, pFileName), getConstant(initializerRaw, pFileName));
      }
    } else {
      // Declaration without initialization (nondet)
      initializer = null;
    }
    return (CDeclaration) getAssignedVarDeclaration(pItem, "", initializer, pFileName);
  }

  private FileLocation getLocation(final Value pItem, final String pFileName) {
    assert pItem != null;
    return new FileLocation(pFileName, 0, 1, 0, 0);
  }
}
