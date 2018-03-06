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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
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
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFATraversal;
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
      purgeUnreachableBlocks(funcName, basicBlocks.values());

      if (en.getExitNode().getNumEnteringEdges() == 0) {
        cfaNodes.remove(funcName, en.getExitNode());
      }

      functions.put(funcName, en);

    } while (!currFunc.equals(lastFunc));
  }

  /** Remove all unreachable blocks and their CFA nodes */
  private void purgeUnreachableBlocks(
      final String pFunctionName, final Collection<BasicBlockInfo> pBasicBlocks) {

    for (BasicBlockInfo block : pBasicBlocks) {
      if (block.entryNode.getNumEnteringEdges() == 0) {
        purgeBlock(pFunctionName, block);
      }
    }
  }

  /** Remove the block and all CFA nodes in it */
  private void purgeBlock(String pFunctionName, BasicBlockInfo pBlock) {
    Collection<CFANode> blockNodes =
        CFATraversal.dfs().collectNodesReachableFrom(pBlock.getEntryNode());
    cfaNodes.get(pFunctionName).removeAll(blockNodes);
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
      } else if (terminatorInst.isBranchInst()) {
        // get the operands and add branching edges
        CExpression condition = getBranchCondition(terminatorInst, pFileName);

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
      } else {
        assert terminatorInst.isSwitchInst()
            : "Unhandled instruction type: " + terminatorInst.getOpCode();

        Value compValue = terminatorInst.getOperand(0);
        CType compType = typeConverter.getCType(compValue.typeOf());
        CExpression comparisonLhs = getAssignedIdExpression(compValue, compType, pFileName);
        BasicBlock defaultBlock = terminatorInst.getSuccessor(0);
        CLabelNode defaultLabel =
            (CLabelNode) pBasicBlocks.get(defaultBlock.hashCode()).getEntryNode();

        CFANode currNode = brNode;
        for (int i = 1; i < succNum; i++) {
          CLabelNode label =
              (CLabelNode)
                  pBasicBlocks.get(terminatorInst.getSuccessor(i).hashCode()).getEntryNode();
          Value caseValue = terminatorInst.getOperand(2 * i);
          CExpression comparisonRhs = (CExpression) getConstant(caseValue, pFileName);

          CBinaryExpression comparisonExp =
              new CBinaryExpression(
                  comparisonLhs.getFileLocation(),
                  CNumericTypes.BOOL,
                  CNumericTypes.BOOL,
                  comparisonLhs,
                  comparisonRhs,
                  BinaryOperator.EQUALS);

          CAssumeEdge jumpEdge =
              new CAssumeEdge(
                  comparisonExp.toASTString(),
                  comparisonExp.getFileLocation(),
                  currNode,
                  label,
                  comparisonExp,
                  true);
          addEdge(jumpEdge);

          CFANode nextNode = newNode(brNode.getFunctionName());
          CAssumeEdge toNextCaseEdge =
              new CAssumeEdge(
                  comparisonExp.toASTString(),
                  comparisonExp.getFileLocation(),
                  currNode,
                  nextNode,
                  comparisonExp,
                  false);
          addEdge(toNextCaseEdge);
          currNode = nextNode;
        }
        addEdge(new BlankEdge("(goto)", FileLocation.DUMMY, currNode, defaultLabel, "(goto)"));
      }
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
        CExpression conditionC = getBranchCondition(condition, pFileName);
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
            curNode = new CFATerminationNode(funcName);
            addNode(funcName, curNode);
            addEdge(new BlankEdge(i.toString(), exprLocation, prevNode, curNode, "unreachable"));
            // don't continue in that block after an `undef` statement
            break;

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

  private CExpression getBranchCondition(final Value pItem, final String pFileName)
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

    } else if (pItem.isBinaryOperator()
        || pItem.isGetElementPtrInst()
        || pItem.isIntToPtrInst()
        || pItem.isPtrToIntInst()) {
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
      return null;
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
    CType returnType = typeConverter.getCType(pItem.typeOf());
    int argumentCount = pItem.getNumArgOperands();

    Value calledFunction = pItem.getCalledFunction();
    CFunctionDeclaration functionDeclaration = null;
    String functionName = null;
    if (calledFunction.isFunction()) {
      functionName = calledFunction.getValueName();
      functionDeclaration = functionDeclarations.get(functionName);
      if (functionDeclaration == null) {
        logger.logf(
            Level.WARNING,
            "Declaration for function %s not found, trying to derive it.",
            functionName);
      }
    }

    List<CExpression> parameters = new ArrayList<>(argumentCount);
    CFunctionType functionType;

    if (functionDeclaration == null) {
      // Try to derive a function type from the call
      List<CType> parameterTypes = new ArrayList<>(argumentCount - 1);
      for (int i = 0; i < argumentCount; i++) {
        Value functionArg = pItem.getArgOperand(i);
        assert functionArg.isConstant()
            || variableDeclarations.containsKey(functionArg.getAddress());
        CType expectedType = typeConverter.getCType(functionArg.typeOf());
        parameterTypes.add(expectedType);
      }

      functionType = new CFunctionType(returnType, parameterTypes, false);

      if (functionName != null) {
        List<CParameterDeclaration> parameterDeclarations = new ArrayList<>();
        for (CType paramType : parameterTypes) {
          parameterDeclarations.add(
              new CParameterDeclaration(FileLocation.DUMMY, paramType, getTempVar(false)));
        }
        CFunctionDeclaration derivedDeclaration =
            new CFunctionDeclaration(
                FileLocation.DUMMY, functionType, functionName, parameterDeclarations);
        functionDeclarations.put(functionName, derivedDeclaration);
      }
    } else {
      functionType = functionDeclaration.getType();
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

    CExpression functionNameExp = getExpression(calledFunction, functionType, pFileName);

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
    } else if (pItem.isExtractValueInst()) {
      return handleExtractValue(pItem, pFunctionName, pFileName);
    } else {
      throw new UnsupportedOperationException(
          "LLVM does not yet support operator with opcode " + pItem.getOpCode());
    }
  }

  private List<CAstNode> handleExtractValue(Value pItem, String pFunctionName, String pFileName)
      throws LLVMException {
    Value accessed = pItem.getOperand(0);
    CType baseType = typeConverter.getCType(accessed.typeOf());
    FileLocation fileLocation = getLocation(pItem, pFileName);

    CType currentType = baseType;
    CExpression currentExpression = getExpression(accessed, currentType, pFileName);
    for (Integer indexValue : pItem.getIndices()) {
      CIntegerLiteralExpression index =
          new CIntegerLiteralExpression(
              fileLocation, CNumericTypes.INT, BigInteger.valueOf(indexValue));

      if (currentType instanceof CArrayType) {
        currentExpression =
            new CArraySubscriptExpression(fileLocation, currentType, currentExpression, index);
        currentType = ((CArrayType) currentType).getType();

      } else if (currentType instanceof CCompositeType) {
        CCompositeTypeMemberDeclaration field =
            ((CCompositeType) currentType).getMembers().get(indexValue);
        String fieldName = field.getName();
        currentExpression =
            new CFieldReference(fileLocation, currentType, fieldName, currentExpression, false);
        currentType = field.getType();
      }
    }

    return getAssignStatement(pItem, currentExpression, pFunctionName, pFileName);
  }

  private List<CAstNode> handleLoad(
      final Value pItem, final String pFunctionName, final String pFileName) throws LLVMException {
    CType expectedType = typeConverter.getCType(pItem.typeOf());
    CExpression expression = getExpression(pItem.getOperand(0), expectedType, pFileName);
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
    CExpression expression = createFromOpCode(pItem, pFileName, pOpCode);
    return getAssignStatement(pItem, expression, pFunctionName, pFileName);
  }

  private CExpression createFromOpCode(
      final Value pItem, final String pFileName, final OpCode pOpCode) throws LLVMException {

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
        return createFromArithmeticOp(pItem, pOpCode, pFileName);

      case GetElementPtr:
        return createGepExp(pItem, pFileName);
      case BitCast:
        return createBitcast(pItem, pFileName);

      case PtrToInt:
        // fall through
      case IntToPtr:
        return new CCastExpression(getLocation(pItem, pFileName), typeConverter.getCType(pItem
            .typeOf()), getExpression(pItem.getOperand(0), typeConverter.getCType(pItem
            .getOperand(0).typeOf()), pFileName));

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

  private CExpression createBitcast(Value pItem, String pFileName) throws LLVMException {
    Value op = pItem.getOperand(0);
    CType expectedType = typeConverter.getCType(pItem.typeOf());
    CType opType = typeConverter.getCType(op.typeOf());
    if (op.isFunction()) {
      assert opType instanceof CPointerType;
      opType = ((CPointerType) opType).getType();
      return getExpression(op, opType, pFileName);
    } else {
      CExpression opToCast = getExpression(op, opType, pFileName);
      return new CCastExpression(getLocation(pItem, pFileName), expectedType, opToCast);
    }
  }

  private CExpression createFromArithmeticOp(
      final Value pItem, final OpCode pOpCode, final String pFileName) throws LLVMException {
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

    return new CBinaryExpression(
        getLocation(pItem, pFileName),
        expressionType,
        expressionType, // calculation type is expression type in LLVM
        operand1Exp,
        operand2Exp,
        operation);
  }

  private CExpression getExpression(
      final Value pItem, final CType pExpectedType, final String pFileName) throws LLVMException {

    if (pItem.isConstantExpr()) {
      return createFromOpCode(pItem, pFileName, pItem.getConstOpCode());

    } else if (pItem.isConstant() && !pItem.isGlobalVariable()) {
      return (CExpression) getConstant(pItem, pExpectedType, pFileName);

    } else {
      return getAssignedIdExpression(pItem, pExpectedType, pFileName);
    }
  }

  private CRightHandSide getConstant(final Value pItem, final String pFileName)
      throws LLVMException {
    CType expectedType = typeConverter.getCType(pItem.typeOf());
    return getConstant(pItem, expectedType, pFileName);
  }

  private CRightHandSide getConstant(final Value pItem, CType pExpectedType, final String pFileName)
      throws LLVMException {
    FileLocation location = getLocation(pItem, pFileName);
    if (pItem.isConstantInt()) {
      long constantValue = pItem.constIntGetSExtValue();
      return new CIntegerLiteralExpression(
          getLocation(pItem, pFileName), pExpectedType, BigInteger.valueOf(constantValue));

    } else if (pItem.isConstantPointerNull()) {
      return new CPointerExpression(location, pExpectedType, getNull(location, pExpectedType));

    } else if (pItem.isConstantExpr()) {
      return getExpression(pItem, pExpectedType, pFileName);

    } else if (pItem.isUndef()) {
      CType constantType = typeConverter.getCType(pItem.typeOf());
      String undefName = "__VERIFIER_undef_" + constantType.toString().replace(' ', '_');
      CSimpleDeclaration undefDecl =
          new CVariableDeclaration(
              location,
              true,
              CStorageClass.AUTO,
              pExpectedType,
              undefName,
              undefName,
              undefName,
              null);
      CExpression undefExpression = new CIdExpression(location, undefDecl);
      return undefExpression;

    } else if (pItem.isFunction()) {
      Function func = pItem.asFunction();
      CFunctionDeclaration funcDecl = functionDeclarations.get(func.getValueName());
      CType functionType = funcDecl.getType();

      CIdExpression funcId = new CIdExpression(location, funcDecl);
      if (pointerOf(pExpectedType, functionType)) {
        return new CUnaryExpression(location, pExpectedType, funcId, UnaryOperator.AMPER);
      } else {
        return funcId;
      }

    } else if (pItem.isGlobalConstant() && pItem.isGlobalVariable()) {
      return getAssignedIdExpression(pItem, pExpectedType, pFileName);
    } else {
      throw new UnsupportedOperationException("LLVM parsing does not support constant " + pItem);
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
      Value element;
      if (pAggregate.isConstantArray() || pAggregate.isConstantStruct()) {
        element = pAggregate.getOperand(i);
      } else {
        element = pAggregate.getElementAsConstant(i);
      }
      assert element.isConstant() : "Value element is not a constant!";
      CInitializer elementInitializer;
      if (isConstantArrayOrVector(element) || element.isConstantStruct()) {
        elementInitializer = getConstantAggregateInitializer(element, pFileName);
      } else if (element.isConstantAggregateZero()) {
        elementInitializer =
            getZeroInitializer(element, typeConverter.getCType(pAggregate.typeOf()), pFileName);
      } else {
        elementInitializer =
            new CInitializerExpression(
                getLocation(element, pFileName), (CExpression) getConstant(element, pFileName));
      }
      elementInitializers.add(elementInitializer);
    }

    CInitializerList aggregateInitializer =
        new CInitializerList(getLocation(pAggregate, pFileName), elementInitializers);
    return aggregateInitializer;
  }

  private CInitializer getZeroInitializer(
      final Value pForElement, final CType pExpectedType, final String pFileName) {
    FileLocation loc = getLocation(pForElement, pFileName);
    CInitializer init;
    CType canonicalType = pExpectedType.getCanonicalType();
    if (canonicalType instanceof CArrayType) {
      int length = ((CArrayType) canonicalType).getLengthAsInt().getAsInt();
      CType elementType = ((CArrayType) canonicalType).getType().getCanonicalType();
      CInitializer zeroInitializer = getZeroInitializer(pForElement, elementType, pFileName);
      List<CInitializer> initializers = Collections.nCopies(length, zeroInitializer);
      init = new CInitializerList(loc, initializers);

    } else if (canonicalType instanceof CCompositeType) {

      List<CCompositeTypeMemberDeclaration> members = ((CCompositeType) canonicalType).getMembers();
      List<CInitializer> initializers = new ArrayList<>(members.size());
      for (CCompositeTypeMemberDeclaration m : members) {
        CType memberType = m.getType();
        CInitializer memberInit = getZeroInitializer(pForElement, memberType, pFileName);
        initializers.add(memberInit);
      }

      init = new CInitializerList(loc, initializers);

    } else {
      CExpression zeroExpression;
      if (canonicalType instanceof CSimpleType) {
        CBasicType basicType = ((CSimpleType) canonicalType).getType();
        if (basicType == CBasicType.FLOAT || basicType == CBasicType.DOUBLE) {
          // use expected type for float, not canonical
          zeroExpression = new CFloatLiteralExpression(loc, pExpectedType, BigDecimal.ZERO);
        } else {
          zeroExpression = CIntegerLiteralExpression.ZERO;
        }
      } else {
        // use expected type for cast, not canonical
        zeroExpression = new CCastExpression(loc, pExpectedType, CIntegerLiteralExpression.ZERO);
      }
      init = new CInitializerExpression(loc, zeroExpression);
    }

    return init;
  }

  private int getLength(Value pAggregateValue) throws LLVMException {
    CType aggregateType = typeConverter.getCType(pAggregateValue.typeOf()).getCanonicalType();
    if (aggregateType instanceof CArrayType) {
      CArrayType arrayType = (CArrayType) typeConverter.getCType(pAggregateValue.typeOf());
      OptionalInt maybeArrayLength = arrayType.getLengthAsInt();
      assert maybeArrayLength.isPresent() : "Constant array has non-constant length";
      return maybeArrayLength.getAsInt();
    } else if (aggregateType instanceof CCompositeType) {
      return ((CCompositeType) aggregateType).getMembers().size();
    } else {
      throw new AssertionError();
    }
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

  /**
   * Returns the id expression to an already declared variable. Returns it as a cast, if necessary
   * to match the expected type.
   */
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

    if (expressionType.canBeAssignedFrom(pExpectedType)
        || expressionType instanceof CArrayType
        || expressionType instanceof CComplexType) {
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
    CExpression currentExpression = getExpression(startPointer, currentType, pFileName);
    currentType = baseType;
    assert pItem.getNumOperands() >= 2
        : "Too few operands in GEP operation : " + pItem.getNumOperands();
    for (int i = 1; i < pItem.getNumOperands(); i++) {
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
      if (isConstantArrayOrVector(initializerRaw)) {
        initializer = getConstantAggregateInitializer(initializerRaw, pFileName);
      } else if (initializerRaw.isConstantStruct()) {
        initializer = getConstantAggregateInitializer(initializerRaw, pFileName);
      } else if (initializerRaw.isConstantAggregateZero()) {
        CType expressionType = typeConverter.getCType(initializerRaw.typeOf());
        initializer = getZeroInitializer(initializerRaw, expressionType, pFileName);
      } else {
        initializer =
            new CInitializerExpression(
                getLocation(pItem, pFileName),
                (CExpression) getConstant(initializerRaw, pFileName));
      }
    } else {
      // Declaration without initialization (nondet)
      initializer = null;
    }
    return (CDeclaration) getAssignedVarDeclaration(pItem, "", initializer, pFileName);
  }

  private boolean isConstantArrayOrVector(final Value pItem) {
    return pItem.isConstantArray() || pItem.isConstantDataArray() || pItem.isConstantVector();
  }

  private FileLocation getLocation(final Value pItem, final String pFileName) {
    assert pItem != null;
    return new FileLocation(pFileName, 0, 1, 0, 0);
  }
}
