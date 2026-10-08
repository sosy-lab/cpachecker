// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * Replaces memory for a single scalar that is allocated with alloca and accessed only by
 * dereferencing one pointer variable with a scalar variable, similar to mem2reg in LLVM.
 *
 * <p>A local pointer variable p is replaced if it is declared without initializer, assigned exactly
 * once by an allocation of the size of its target type, and otherwise occurs only as *p. Then the
 * declaration of p is removed, the allocation is replaced by the declaration of the scalar
 * variable, whose value is unknown like the allocated memory, and *p is replaced by the scalar
 * variable.
 */
public class ScalarAllocationReplacer {

  private static final ImmutableSet<String> ALLOCATION_FUNCTIONS =
      ImmutableSet.of("alloca", "__builtin_alloca");

  /** The prefix of the names of the scalar variables, followed by the name of the pointer. */
  public static final String SCALAR_PREFIX = "__CPAchecker_deref_";

  private final LogManager logger;
  private final MachineModel machineModel;

  public ScalarAllocationReplacer(LogManager pLogger, MachineModel pMachineModel) {
    logger = pLogger;
    machineModel = pMachineModel;
  }

  public void replaceScalarAllocations(MutableCFA pCfa) {
    for (String function : pCfa.getAllFunctionNames()) {
      replaceInFunction(ImmutableList.copyOf(pCfa.getFunctionNodes(function)));
    }
  }

  private void replaceInFunction(List<CFANode> pNodes) {
    ImmutableList<CFAEdge> edges =
        pNodes.stream()
            .flatMap(node -> node.getLeavingEdges().stream())
            .collect(ImmutableList.toImmutableList());

    // The pointer variables that may be replaced, by their qualified names
    Map<String, CVariableDeclaration> candidates = new HashMap<>();
    Map<String, CFAEdge> declarationEdges = new HashMap<>();
    Set<String> excluded = new HashSet<>();
    for (CFAEdge edge : edges) {
      if (edge instanceof CDeclarationEdge declarationEdge
          && declarationEdge.getDeclaration() instanceof CVariableDeclaration variable
          && !variable.isGlobal()
          && variable.getInitializer() == null
          && getScalarTargetType(variable).isPresent()) {
        if (declarationEdges.containsKey(variable.getQualifiedName())) {
          excluded.add(variable.getQualifiedName());
        }
        candidates.put(variable.getQualifiedName(), variable);
        declarationEdges.put(variable.getQualifiedName(), edge);
      }
    }
    if (candidates.isEmpty()) {
      return;
    }

    // Check that every candidate is allocated exactly once and otherwise only dereferenced
    Map<String, CFAEdge> allocationEdges = new HashMap<>();
    for (CFAEdge edge : edges) {
      Optional<String> allocated = getAllocatedPointer(edge, candidates);
      if (allocated.isPresent()) {
        if (allocationEdges.put(allocated.orElseThrow(), edge) != null) {
          excluded.add(allocated.orElseThrow());
        }
        continue;
      }
      if (edge.getRawAST().isPresent()) {
        Set<String> dereferenced = new HashSet<>();
        Set<String> otherwiseUsed = new HashSet<>();
        classifyUses(edge.getRawAST().orElseThrow(), candidates, dereferenced, otherwiseUsed);
        excluded.addAll(otherwiseUsed);
        if (!dereferenced.isEmpty() && !isReplaceable(edge)) {
          excluded.addAll(dereferenced);
        }
      }
    }

    Map<String, CVariableDeclaration> scalars = new HashMap<>();
    for (Map.Entry<String, CVariableDeclaration> candidate : candidates.entrySet()) {
      if (!excluded.contains(candidate.getKey())
          && allocationEdges.containsKey(candidate.getKey())) {
        CVariableDeclaration pointer = candidate.getValue();
        String name = SCALAR_PREFIX + pointer.getName();
        String function = candidate.getKey().substring(0, candidate.getKey().indexOf("::"));
        scalars.put(
            candidate.getKey(),
            new CVariableDeclaration(
                pointer.getFileLocation(),
                false,
                CStorageClass.AUTO,
                getScalarTargetType(pointer).orElseThrow(),
                name,
                SCALAR_PREFIX + pointer.getOrigName(),
                function + "::" + name,
                null));
      }
    }
    if (scalars.isEmpty()) {
      return;
    }
    logger.log(
        Level.FINE,
        "Replacing the memory of the pointers",
        scalars.keySet(),
        "by scalar variables");

    for (CFAEdge edge : edges) {
      Optional<CFAEdge> replacement = replaceEdge(edge, scalars, declarationEdges, allocationEdges);
      if (replacement.isPresent()) {
        CFACreationUtils.removeEdgeFromNodes(edge);
        CFACreationUtils.addEdgeUnconditionallyToCFA(replacement.orElseThrow());
      }
    }
  }

  /** Returns the target type of a pointer variable if it is a scalar type. */
  private static Optional<CType> getScalarTargetType(CVariableDeclaration pVariable) {
    if (pVariable.getType().getCanonicalType() instanceof CPointerType pointerType
        && pointerType.getType().getCanonicalType() instanceof CSimpleType target
        && !target.isVolatile()) {
      return Optional.of(pointerType.getType());
    }
    return Optional.empty();
  }

  /**
   * Returns the qualified name of the candidate if the edge assigns to it an allocation of the size
   * of its target type.
   */
  private Optional<String> getAllocatedPointer(
      CFAEdge pEdge, Map<String, CVariableDeclaration> pCandidates) {
    if (pEdge instanceof CStatementEdge statementEdge
        && statementEdge.getStatement() instanceof CFunctionCallAssignmentStatement assignment
        && assignment.getLeftHandSide() instanceof CIdExpression pointer
        && isCandidate(pointer, pCandidates)) {
      CFunctionCallExpression call = assignment.getRightHandSide();
      CVariableDeclaration variable = pCandidates.get(pointer.getDeclaration().getQualifiedName());
      if (call.getFunctionNameExpression() instanceof CIdExpression function
          && ALLOCATION_FUNCTIONS.contains(function.getName())
          && call.getParameterExpressions().size() == 1
          && call.getParameterExpressions().getFirst() instanceof CIntegerLiteralExpression size
          && size.getValue()
              .equals(machineModel.getSizeof(getScalarTargetType(variable).orElseThrow()))) {
        return Optional.of(pointer.getDeclaration().getQualifiedName());
      }
    }
    return Optional.empty();
  }

  /**
   * Collects the candidates that occur dereferenced, i.e., as *p, and the candidates that occur in
   * another way in the given AST node.
   */
  private static void classifyUses(
      AAstNode pAstNode,
      Map<String, CVariableDeclaration> pCandidates,
      Set<String> pDereferenced,
      Set<String> pOtherwiseUsed) {
    if (!(pAstNode instanceof CAstNode astNode)) {
      return;
    }
    // The visitor is only used to traverse the AST, the substitutes are not used
    astNode.accept(
        new SubstitutingCAstNodeVisitor(
            node -> {
              Optional<String> dereferenced = getDereferencedCandidate(node, pCandidates);
              if (dereferenced.isPresent()) {
                pDereferenced.add(dereferenced.orElseThrow());
                // Do not visit the pointer below the dereference
                return node;
              }
              if (node instanceof CIdExpression id && isCandidate(id, pCandidates)) {
                pOtherwiseUsed.add(id.getDeclaration().getQualifiedName());
              }
              return null;
            }));
  }

  private static boolean isCandidate(CIdExpression pId, Map<String, ?> pCandidates) {
    // Identifiers of undeclared functions have no declaration
    return pId.getDeclaration() != null
        && pCandidates.containsKey(pId.getDeclaration().getQualifiedName());
  }

  /**
   * Replaces the scalar variables in the given C expression by the dereferenced pointers that they
   * replace, e.g., for the export of witnesses for the original program.
   */
  public static String restorePointerDereferences(String pExpression) {
    return pExpression.replaceAll("\\b" + SCALAR_PREFIX + "(\\w+)\\b", "(*$1)");
  }

  /** Returns the qualified name of the candidate if the node is *p for a candidate p. */
  private static Optional<String> getDereferencedCandidate(
      CAstNode pNode, Map<String, ? extends CDeclaration> pCandidates) {
    if (pNode instanceof CPointerExpression dereference
        && dereference.getOperand() instanceof CIdExpression pointer
        && isCandidate(pointer, pCandidates)) {
      return Optional.of(pointer.getDeclaration().getQualifiedName());
    }
    return Optional.empty();
  }

  private static boolean isReplaceable(CFAEdge pEdge) {
    return pEdge instanceof CStatementEdge
        || pEdge instanceof CAssumeEdge
        || pEdge instanceof CDeclarationEdge
        || pEdge instanceof CReturnStatementEdge;
  }

  /** Returns the edge that replaces the given edge, if it has to be replaced. */
  private static Optional<CFAEdge> replaceEdge(
      CFAEdge pEdge,
      Map<String, CVariableDeclaration> pScalars,
      Map<String, CFAEdge> pDeclarationEdges,
      Map<String, CFAEdge> pAllocationEdges) {
    for (Map.Entry<String, CVariableDeclaration> scalar : pScalars.entrySet()) {
      if (pEdge.equals(pDeclarationEdges.get(scalar.getKey()))) {
        return Optional.of(
            new BlankEdge(
                "", pEdge.getFileLocation(), pEdge.getPredecessor(), pEdge.getSuccessor(), ""));
      }
      if (pEdge.equals(pAllocationEdges.get(scalar.getKey()))) {
        CVariableDeclaration declaration = scalar.getValue();
        return Optional.of(
            new CDeclarationEdge(
                declaration.toASTString(),
                pEdge.getFileLocation(),
                pEdge.getPredecessor(),
                pEdge.getSuccessor(),
                declaration));
      }
    }

    if (pEdge.getRawAST().isEmpty() || !(pEdge.getRawAST().orElseThrow() instanceof CAstNode)) {
      return Optional.empty();
    }
    CAstNode original = (CAstNode) pEdge.getRawAST().orElseThrow();
    CAstNode substituted =
        original.accept(
            new SubstitutingCAstNodeVisitor(
                node ->
                    getDereferencedCandidate(node, pScalars)
                        .map(
                            pointer ->
                                new CIdExpression(node.getFileLocation(), pScalars.get(pointer)))
                        .orElse(null)));
    if (substituted.equals(original)) {
      return Optional.empty();
    }

    CFANode predecessor = pEdge.getPredecessor();
    CFANode successor = pEdge.getSuccessor();
    String raw = substituted.toASTString();
    return Optional.of(
        switch (pEdge) {
          case CStatementEdge edge ->
              new CStatementEdge(
                  raw, (CStatement) substituted, edge.getFileLocation(), predecessor, successor);
          case CAssumeEdge edge ->
              new CAssumeEdge(
                  raw,
                  edge.getFileLocation(),
                  predecessor,
                  successor,
                  (CExpression) substituted,
                  edge.getTruthAssumption(),
                  edge.isSwapped(),
                  edge.isArtificialIntermediate());
          case CDeclarationEdge edge ->
              new CDeclarationEdge(
                  raw, edge.getFileLocation(), predecessor, successor, (CDeclaration) substituted);
          case CReturnStatementEdge edge ->
              new CReturnStatementEdge(
                  raw,
                  (CReturnStatement) substituted,
                  edge.getFileLocation(),
                  predecessor,
                  (FunctionExitNode) successor);
          default -> throw new AssertionError("Unexpected edge with a replaced pointer: " + pEdge);
        });
  }
}
