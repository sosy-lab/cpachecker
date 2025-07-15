package org.sosy_lab.cpachecker.util.cwriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IfElement;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.SimpleStatement;

public class Block2CTranslator extends AbstractToCTranslator {

  private final AstCfaRelation astCfaRelation;
  private final CFA cfa;

  public Block2CTranslator(Configuration config, CFA cfa) throws InvalidConfigurationException {
    super(config);
    this.cfa = cfa;
    this.astCfaRelation = this.cfa.getAstCfaRelation();
  }

  public String translateBlockNode(BlockNode blockNode) throws IOException, CPAException {
    createdStatements.clear();
    functions.clear();

    globalDefinitionsSet.add("extern void abort();");

    String functionHeader =
        cfa.getFunctionHead(blockNode.getInitialLocation().getFunctionName())
            .getFunctionDefinition()
            .toASTString();
    CompoundStatement rootBlock = new CompoundStatement(null);
    // Add function definition to functions list
    functions.add(
        new Statement.FunctionDefinition(
            functionHeader.substring(0, functionHeader.length() - 1), rootBlock));

    // Add missing variable declarations
    Map<String, CType> undeclaredVars = collectUndeclaredVariables(blockNode);
    addNonDetVariableDeclarations(rootBlock, undeclaredVars);

    Set<CFANode> visited = new HashSet<>();
    traverse(blockNode.getInitialLocation(), rootBlock, blockNode, visited);

    return generateCCode();
  }

  private void traverse(
      CFANode node, CompoundStatement currentBlock, BlockNode blockNode, Set<CFANode> visited)
      throws CPAException {

    // Skip if node is not part of this block or already processed
    if (shouldSkipNode(node, blockNode, visited)) {
      return;
    }

    currentBlock = adjustBlockForMergePoint(node, currentBlock, blockNode);

    List<CFAEdge> outgoingEdges = getBlockLeavingEdges(node, blockNode);

    ControlFlowPattern pattern = classifyControlFlowPattern(outgoingEdges);
    handleControlFlowPattern(pattern, node, outgoingEdges, currentBlock, blockNode, visited);
  }

  private boolean shouldSkipNode(CFANode node, BlockNode blockNode, Set<CFANode> visited) {
    return !blockNode.getNodes().contains(node) || !visited.add(node);
  }

  private CompoundStatement adjustBlockForMergePoint(
      CFANode node, CompoundStatement currentBlock, BlockNode blockNode) {
    if (isMergePoint(node, blockNode)) {
      return currentBlock.getSurroundingBlock();
    }
    return currentBlock;
  }

  private boolean isMergePoint(CFANode node, BlockNode blockNode) {
    if (node instanceof FunctionExitNode || isLoopHead(node)) {
      return false;
    }

    int blockEnteringEdges = getBlockEnteringEdges(node, blockNode);
    return blockEnteringEdges > 1;
  }

  private enum ControlFlowPattern {
    BRANCHING, // Two assume edges (if/loop)
    SINGLE_CONDITIONAL, // One assume edge (partial conditional), only possible because of
    // decomposition
    LINEAR // Regular sequential flow
  }

  private ControlFlowPattern classifyControlFlowPattern(List<CFAEdge> outgoingEdges) {
    if (isBranchingPattern(outgoingEdges)) {
      return ControlFlowPattern.BRANCHING;
    } else if (isSingleConditionalPattern(outgoingEdges)) {
      return ControlFlowPattern.SINGLE_CONDITIONAL;
    } else {
      return ControlFlowPattern.LINEAR;
    }
  }

  private boolean isBranchingPattern(List<CFAEdge> outgoingEdges) {
    return outgoingEdges.size() == 2
        && outgoingEdges.stream().allMatch(e -> e instanceof CAssumeEdge);
  }

  private boolean isSingleConditionalPattern(List<CFAEdge> outgoingEdges) {
    return outgoingEdges.size() == 1 && outgoingEdges.get(0) instanceof CAssumeEdge;
  }

  private void handleControlFlowPattern(
      ControlFlowPattern pattern,
      CFANode node,
      List<CFAEdge> outgoingEdges,
      CompoundStatement currentBlock,
      BlockNode blockNode,
      Set<CFANode> visited)
      throws CPAException {

    switch (pattern) {
      case BRANCHING ->
          handleBranchingPattern(node, outgoingEdges, currentBlock, blockNode, visited);
      case SINGLE_CONDITIONAL ->
          handleSingleConditionalPattern(outgoingEdges.get(0), currentBlock, blockNode, visited);
      case LINEAR -> handleLinearPattern(outgoingEdges, currentBlock, blockNode, visited);
    }
  }

  private void handleBranchingPattern(
      CFANode node,
      List<CFAEdge> outgoingEdges,
      CompoundStatement currentBlock,
      BlockNode blockNode,
      Set<CFANode> visited)
      throws CPAException {

    CAssumeEdge trueEdge = (CAssumeEdge) outgoingEdges.get(0);
    CAssumeEdge falseEdge = (CAssumeEdge) outgoingEdges.get(1);

    if (!trueEdge.getTruthAssumption()) {
      CAssumeEdge temp = trueEdge;
      trueEdge = falseEdge;
      falseEdge = temp;
    }

    String condition = getConditionString(trueEdge);

    if (astCfaRelation != null) {
      if (isLoopHead(node)) {
        handleLoop(trueEdge, falseEdge, condition, currentBlock, blockNode, visited);
        return;
      }

      Optional<IfElement> ifElement = astCfaRelation.getIfStructureForConditionEdge(trueEdge);
      if (ifElement.isPresent()) {
        handleIfStatement(
            trueEdge, falseEdge, condition, ifElement.get(), currentBlock, blockNode, visited);
        return;
      }
    }

    handleSimpleConditional(trueEdge, falseEdge, condition, currentBlock, blockNode, visited);
  }

  private boolean isLoopHead(CFANode node) {
    if (astCfaRelation == null) return false;

    Optional<IterationElement> iterationElement =
        astCfaRelation.getTightestIterationStructureForNode(node);
    return iterationElement.isPresent()
        && iterationElement.get().getLoopHead().map(node::equals).orElse(false);
  }

  private void handleLoop(
      CAssumeEdge trueEdge,
      CAssumeEdge falseEdge,
      String condition,
      CompoundStatement currentBlock,
      BlockNode blockNode,
      Set<CFANode> visited)
      throws CPAException {

    CompoundStatement loopBody = new CompoundStatement(currentBlock);
    currentBlock.addStatement(new SimpleStatement("while (" + condition + ")"));
    currentBlock.addStatement(loopBody);

    traverse(trueEdge.getSuccessor(), loopBody, blockNode, visited);

    CompoundStatement afterLoop = new CompoundStatement(currentBlock);
    currentBlock.addStatement(afterLoop);
    traverse(falseEdge.getSuccessor(), afterLoop, blockNode, visited);
  }

  private void handleIfStatement(
      CAssumeEdge trueEdge,
      CAssumeEdge falseEdge,
      String condition,
      IfElement ifElement,
      CompoundStatement currentBlock,
      BlockNode blockNode,
      Set<CFANode> visited)
      throws CPAException {

    boolean hasElse = ifElement.getMaybeElseElement().isPresent();

    CompoundStatement thenBlock = new CompoundStatement(currentBlock);
    currentBlock.addStatement(new SimpleStatement("if (" + condition + ")"));
    currentBlock.addStatement(thenBlock);

    if (hasElse) {
      CompoundStatement elseBlock = new CompoundStatement(currentBlock);
      currentBlock.addStatement(new SimpleStatement("else"));
      currentBlock.addStatement(elseBlock);

      traverse(trueEdge.getSuccessor(), thenBlock, blockNode, visited);
      traverse(falseEdge.getSuccessor(), elseBlock, blockNode, visited);
    } else {
      traverse(trueEdge.getSuccessor(), thenBlock, blockNode, visited);
      traverse(falseEdge.getSuccessor(), currentBlock, blockNode, visited);
    }
  }

  private void handleSimpleConditional(
      CAssumeEdge trueEdge,
      CAssumeEdge falseEdge,
      String condition,
      CompoundStatement currentBlock,
      BlockNode blockNode,
      Set<CFANode> visited)
      throws CPAException {

    CompoundStatement thenBlock = new CompoundStatement(currentBlock);
    CompoundStatement elseBlock = new CompoundStatement(currentBlock);

    currentBlock.addStatement(new SimpleStatement("if (" + condition + ")"));
    currentBlock.addStatement(thenBlock);
    currentBlock.addStatement(new SimpleStatement("else"));
    currentBlock.addStatement(elseBlock);

    traverse(trueEdge.getSuccessor(), thenBlock, blockNode, visited);
    traverse(falseEdge.getSuccessor(), elseBlock, blockNode, visited);
  }

  private void handleSingleConditionalPattern(
      CFAEdge edge, CompoundStatement currentBlock, BlockNode blockNode, Set<CFANode> visited)
      throws CPAException {

    CAssumeEdge assumeEdge = (CAssumeEdge) edge;
    String condition = getConditionString(assumeEdge);

    CompoundStatement conditionalBlock = new CompoundStatement(currentBlock);
    currentBlock.addStatement(new SimpleStatement("if (" + condition + ")"));
    currentBlock.addStatement(conditionalBlock);

    traverse(assumeEdge.getSuccessor(), conditionalBlock, blockNode, visited);
  }

  private void handleLinearPattern(
      List<CFAEdge> outgoingEdges,
      CompoundStatement currentBlock,
      BlockNode blockNode,
      Set<CFANode> visited)
      throws CPAException {

    for (CFAEdge edge : outgoingEdges) {
      String statement = translateSimpleEdge(edge);
      if (!statement.isEmpty()) {
        currentBlock.addStatement(new SimpleStatement(statement));
      }

      traverse(edge.getSuccessor(), currentBlock, blockNode, visited);
    }
  }

  private int getBlockEnteringEdges(CFANode node, BlockNode blockNode) {
    int count = 0;
    for (CFAEdge e : CFAUtils.enteringEdges(node)) {
      if (blockNode.getEdges().contains(e)) count++;
    }
    return count;
  }

  private List<CFAEdge> getBlockLeavingEdges(CFANode node, BlockNode blockNode) {
    List<CFAEdge> result = new ArrayList<>();
    for (CFAEdge e : CFAUtils.leavingEdges(node)) {
      if (blockNode.getEdges().contains(e)) result.add(e);
    }
    return result;
  }

  private String getConditionString(CAssumeEdge edge) {
    return edge.getTruthAssumption()
        ? edge.getExpression().toASTString()
        : "!(" + edge.getExpression().toASTString() + ")";
  }

  private Map<String, CType> collectUndeclaredVariables(BlockNode blockNode) {
    // TODO: Change to not use edge analyzer, but rather a cfaedgeutils method to extract variables
    // and their types
    Map<String, CType> localTypes = new HashMap<>();
    org.sosy_lab.cpachecker.cpa.invariants.EdgeAnalyzer edgeAnalyzer =
        new org.sosy_lab.cpachecker.cpa.invariants.EdgeAnalyzer(
            org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorIntervalManagerFactory
                .forbidSignedWrapAround(),
            cfa.getMachineModel());
    for (CFAEdge edge : blockNode.getEdges()) {
      Map<org.sosy_lab.cpachecker.util.states.MemoryLocation, CType> edgeVars =
          edgeAnalyzer.getInvolvedVariableTypes(edge);
      for (Map.Entry<org.sosy_lab.cpachecker.util.states.MemoryLocation, CType> entry :
          edgeVars.entrySet()) {
        String varName = entry.getKey().getIdentifier();
        localTypes.put(varName, entry.getValue());
      }
    }
    Set<String> declaredVariables = new HashSet<>();
    for (CFAEdge edge : blockNode.getEdges()) {
      if (edge instanceof CDeclarationEdge) {
        CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
        if (decl instanceof CVariableDeclaration) {
          declaredVariables.add(((CVariableDeclaration) decl).getName());
        }
      }
    }
    localTypes.keySet().removeAll(declaredVariables);
    return localTypes;
  }

  private void addNonDetVariableDeclarations(CompoundStatement block, Map<String, CType> vars) {
    for (Map.Entry<String, CType> entry : vars.entrySet()) {
      String varName = entry.getKey();
      CType varType = entry.getValue();
      switch (varType.toString()) {
        // BOOL
        case "_Bool" -> {
          String initStmt = "_Bool " + varName + " = __VERIFIER_nondet_bool();";
          globalDefinitionsSet.add("extern _Bool __VERIFIER_nondet_bool();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "pthread_t" -> {
          String initStmt = "pthread_t " + varName + " = __VERIFIER_nondet_pthread_t();";
          globalDefinitionsSet.add("extern pthread_t __VERIFIER_nondet_pthread_t();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // Signed SHORT
        case "short" -> {
          String initStmt = "short " + varName + " = __VERIFIER_nondet_short();";
          globalDefinitionsSet.add("extern short __VERIFIER_nondet_short();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "short int" -> {
          String initStmt = "short int " + varName + " = __VERIFIER_nondet_short();";
          globalDefinitionsSet.add("extern short int __VERIFIER_nondet_short();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "signed short" -> {
          String initStmt = "signed short " + varName + " = __VERIFIER_nondet_short();";
          globalDefinitionsSet.add("extern signed short __VERIFIER_nondet_short();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // Unsigned SHORT
        case "unsigned short" -> {
          String initStmt = "unsigned short " + varName + " = __VERIFIER_nondet_ushort();";
          globalDefinitionsSet.add("extern unsigned short __VERIFIER_nondet_ushort();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "unsigned short int" -> {
          String initStmt = "unsigned short int " + varName + " = __VERIFIER_nondet_ushort();";
          globalDefinitionsSet.add("extern unsigned short int __VERIFIER_nondet_ushort();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // Signed INT
        case "int" -> {
          String initStmt = "int " + varName + " = __VERIFIER_nondet_int();";
          globalDefinitionsSet.add("extern int __VERIFIER_nondet_int();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "signed" -> {
          String initStmt = "signed " + varName + " = __VERIFIER_nondet_int();";
          globalDefinitionsSet.add("extern signed __VERIFIER_nondet_int();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "signed int" -> {
          String initStmt = "signed int " + varName + " = __VERIFIER_nondet_int();";
          globalDefinitionsSet.add("extern signed int __VERIFIER_nondet_int();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // Unsigned INT
        case "unsigned" -> {
          String initStmt = "unsigned int " + varName + " = __VERIFIER_nondet_uint();";
          globalDefinitionsSet.add("extern unsigned int __VERIFIER_nondet_uint();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "unsigned int" -> {
          String initStmt = "unsigned int " + varName + " = __VERIFIER_nondet_uint();";
          globalDefinitionsSet.add("extern unsigned int __VERIFIER_nondet_uint();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "size_t" -> {
          String initStmt = "size_t " + varName + " = __VERIFIER_nondet_size_t();";
          globalDefinitionsSet.add("extern size_t __VERIFIER_nondet_size_t();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // Unsigned ints misc
        case "u8" -> {
          String initStmt = "u8 " + varName + " = __VERIFIER_nondet_u8();";
          globalDefinitionsSet.add("extern u8 __VERIFIER_nondet_u8();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "u16" -> {
          String initStmt = "u16 " + varName + " = __VERIFIER_nondet_u16();";
          globalDefinitionsSet.add("extern u16 __VERIFIER_nondet_u16();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "u32" -> {
          String initStmt = "u32 " + varName + " = __VERIFIER_nondet_u32();";
          globalDefinitionsSet.add("extern u32 __VERIFIER_nondet_u32();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "u64" -> {
          String initStmt = "u64 " + varName + " = __VERIFIER_nondet_u64();";
          globalDefinitionsSet.add("extern u64 __VERIFIER_nondet_u64();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "u128" -> {
          String initStmt = "u128 " + varName + " = __VERIFIER_nondet_u128();";
          globalDefinitionsSet.add("extern u128 __VERIFIER_nondet_u128();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // Signeed INT128
        case "__int128" -> {
          String initStmt = "__int128_t " + varName + " = __VERIFIER_nondet_int128();";
          globalDefinitionsSet.add("extern __int128_t __VERIFIER_nondet_int128();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "signed __int128" -> {
          String initStmt = "signed __int128 " + varName + " = __VERIFIER_nondet_int128();";
          globalDefinitionsSet.add("extern signed __int128 __VERIFIER_nondet_int128();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "__int128_t" -> {
          String initStmt = "__int128_t " + varName + " = __VERIFIER_nondet_int128();";
          globalDefinitionsSet.add("extern __int128_t __VERIFIER_nondet_int128();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "signed __int128_t" -> {
          String initStmt = "signed __int128_t " + varName + " = __VERIFIER_nondet_int128();";
          globalDefinitionsSet.add("extern signed __int128_t __VERIFIER_nondet_int128();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // Unsigned INT128
        case "unsigned __int128" -> {
          String initStmt = "__uint128_t " + varName + " = __VERIFIER_nondet_uint128();";
          globalDefinitionsSet.add("extern __uint128_t __VERIFIER_nondet_uint128();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "unsigned __uint128_t" -> {
          String initStmt = "__uint128_t " + varName + " = __VERIFIER_nondet_uint128();";
          globalDefinitionsSet.add("extern __uint128_t __VERIFIER_nondet_uint128();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // Signed LONG
        case "long" -> {
          String initStmt = "long " + varName + " = __VERIFIER_nondet_long();";
          globalDefinitionsSet.add("extern long __VERIFIER_nondet_long();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "long int" -> {
          String initStmt = "long int " + varName + " = __VERIFIER_nondet_long();";
          globalDefinitionsSet.add("extern long int __VERIFIER_nondet_long();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "signed long" -> {
          String initStmt = "signed long " + varName + " = __VERIFIER_nondet_long();";
          globalDefinitionsSet.add("extern signed long __VERIFIER_nondet_long();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "signed long int" -> {
          String initStmt = "signed long int " + varName + " = __VERIFIER_nondet_long();";
          globalDefinitionsSet.add("extern signed long int __VERIFIER_nondet_long();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "long long" -> {
          String initStmt = "long long " + varName + " = __VERIFIER_nondet_longlong();";
          globalDefinitionsSet.add("extern long long __VERIFIER_nondet_longlong();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "long long int" -> {
          String initStmt = "long long int " + varName + " = __VERIFIER_nondet_longlong();";
          globalDefinitionsSet.add("extern long long int __VERIFIER_nondet_longlong();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "signed long long" -> {
          String initStmt = "signed long long " + varName + " = __VERIFIER_nondet_longlong();";
          globalDefinitionsSet.add("extern signed long long __VERIFIER_nondet_longlong();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "signed long long int" -> {
          String initStmt = "signed long long int " + varName + " = __VERIFIER_nondet_longlong();";
          globalDefinitionsSet.add("extern signed long long int __VERIFIER_nondet_longlong();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // Unsigned LONG
        case "unsigned long" -> {
          String initStmt = "unsigned long " + varName + " = __VERIFIER_nondet_ulong();";
          globalDefinitionsSet.add("extern unsigned long __VERIFIER_nondet_ulong();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "unsigned long int" -> {
          String initStmt = "unsigned long int " + varName + " = __VERIFIER_nondet_ulong();";
          globalDefinitionsSet.add("extern unsigned long int __VERIFIER_nondet_ulong();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "sector_t" -> {
          String initStmt = "sector_t " + varName + " = __VERIFIER_nondet_sector_t();";
          globalDefinitionsSet.add("extern sector_t __VERIFIER_nondet_sector_t();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // Unsigned LONG LONG
        case "unsigned long long" -> {
          String initStmt = "unsigned long long " + varName + " = __VERIFIER_nondet_ulonglong();";
          globalDefinitionsSet.add("extern unsigned long long __VERIFIER_nondet_ulonglong();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "unsigned long long int" -> {
          String initStmt =
              "unsigned long long int " + varName + " = __VERIFIER_nondet_ulonglong();";
          globalDefinitionsSet.add("extern unsigned long long int __VERIFIER_nondet_ulonglong();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // Signed LONG LONG
        case "loff_t" -> {
          String initStmt = "loff_t " + varName + " = __VERIFIER_nondet_loff_t();";
          globalDefinitionsSet.add("extern loff_t __VERIFIER_nondet_loff_t();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // CHAR
        case "char" -> {
          String initStmt = "char " + varName + " = __VERIFIER_nondet_char();";
          globalDefinitionsSet.add("extern char __VERIFIER_nondet_char();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "signed char" -> {
          String initStmt = "signed char " + varName + " = __VERIFIER_nondet_char();";
          globalDefinitionsSet.add("extern signed char __VERIFIER_nondet_char();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "unsigned char" -> {
          String initStmt = "unsigned char " + varName + " = __VERIFIER_nondet_uchar();";
          globalDefinitionsSet.add("extern unsigned char __VERIFIER_nondet_uchar();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        // FLOAT
        case "float" -> {
          String initStmt = "float " + varName + " = __VERIFIER_nondet_float();";
          globalDefinitionsSet.add("extern float __VERIFIER_nondet_float();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "double" -> {
          String initStmt = "double " + varName + " = __VERIFIER_nondet_double();";
          globalDefinitionsSet.add("extern double __VERIFIER_nondet_double();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        case "long double" -> {
          String initStmt = "long double " + varName + " = __VERIFIER_nondet_long_double();";
          globalDefinitionsSet.add("extern long double __VERIFIER_nondet_long_double();");
          block.addStatement(new SimpleStatement(initStmt));
        }
        default -> {
          block.addStatement(new SimpleStatement(varType + " " + varName + ";"));
        }
      }
    }
  }
}
