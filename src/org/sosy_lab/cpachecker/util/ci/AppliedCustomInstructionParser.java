// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatorVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

public class AppliedCustomInstructionParser {

  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;
  private final CFA cfa;
  private final ImmutableMap<Integer, CFANode> numberToCFANode;
  private final GlobalVarCheckVisitor visitor = new GlobalVarCheckVisitor();

  public AppliedCustomInstructionParser(
      final ShutdownNotifier pShutdownNotifier, final LogManager pLogger, final CFA pCfa) {
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    cfa = pCfa;

    ImmutableMap.Builder<Integer, CFANode> nodeNumberToNode0 = ImmutableMap.builder();
    for (CFANode node : cfa.nodes()) {
      nodeNumberToNode0.put(node.getNodeNumber(), node);
    }
    numberToCFANode = nodeNumberToNode0.buildOrThrow();
  }

  /**
   * Creates a CustomInstructionApplication if the file contains all required data, null if not
   *
   * @param file Path of the file to be read
   * @param signatureFile Path of the file into which the ci signature will be written
   * @return CustomInstructionApplication
   * @throws IOException if the file doesn't contain all required data.
   */
  public CustomInstructionApplications parse(final Path file, final Path signatureFile)
      throws IOException, AppliedCustomInstructionParsingFailedException, InterruptedException {

    CustomInstruction ci = null;

    try (BufferedReader br = Files.newBufferedReader(file)) {
      String line = br.readLine();
      if (line == null) {
        throw new AppliedCustomInstructionParsingFailedException(
            "Empty specification. Missing at least function name for custom instruction.");
      }

      ci = readCustomInstruction(line);

      writeCustomInstructionSpecification(ci, signatureFile);

      return parseACIs(br, ci);
    }
  }

  private void writeCustomInstructionSpecification(
      final CustomInstruction ci, final Path signatureFile) throws IOException {
    try (Writer br = IO.openOutputFile(signatureFile, Charset.defaultCharset())) {
      br.write(ci.getSignature() + "\n");
      String ciString = ci.getFakeSMTDescription().getSecond();
      int index = ciString.indexOf("a");
      if (index == -1 || ciString.charAt(index - 1) != '(') {
        index = ciString.indexOf("(", ciString.indexOf("B"));
      } else {
        index--; // also write ( in front of first a
      }
      if (index != -1) {
        br.write(ciString.substring(index, ciString.length() - 1) + ";");
      }
    }
  }

  public CustomInstructionApplications parse(final CustomInstruction pCi, final Path file)
      throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException {
    try (BufferedReader br = Files.newBufferedReader(file)) {
      return parseACIs(br, pCi);
    }
  }

  private CustomInstructionApplications parseACIs(
      final BufferedReader br, final CustomInstruction ci)
      throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException {
    ImmutableMap.Builder<CFANode, AppliedCustomInstruction> map = new ImmutableMap.Builder<>();

    CFANode startNode;
    AppliedCustomInstruction aci;
    String line;

    while ((line = br.readLine()) != null) {
      shutdownNotifier.shutdownIfNecessary();
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }
      startNode = getCFANode(line);
      if (startNode == null) {
        continue;
      }

      try {
        aci = ci.inspectAppliedCustomInstruction(startNode);
      } catch (InterruptedException ex) {
        throw new AppliedCustomInstructionParsingFailedException(
            "Parsing failed because of ShutdownNotifier: " + ex.getMessage());
      }

      map.put(startNode, aci);
    }

    return new CustomInstructionApplications(map.buildOrThrow(), ci);
  }

  /**
   * Creates a new CFANode with respect to the given parameters
   *
   * @param pNodeID String
   * @return a new CFANode with respect to the given parameters
   * @throws AppliedCustomInstructionParsingFailedException if the node can't be created
   */
  protected CFANode getCFANode(final String pNodeID)
      throws AppliedCustomInstructionParsingFailedException {
    try {
      return numberToCFANode.get(Integer.parseInt(pNodeID));
    } catch (NumberFormatException ex) {
      throw new AppliedCustomInstructionParsingFailedException(
          "It is not possible to parse " + pNodeID + " to an integer!", ex);
    }
  }

  /**
   * Creates an ImmutableSet out of the given String[].
   *
   * @param pNodes String[]
   * @return Immutable Set of CFANodes out of the String[]
   */
  protected ImmutableSet<CFANode> getCFANodes(final String[] pNodes)
      throws AppliedCustomInstructionParsingFailedException {
    ImmutableSet.Builder<CFANode> builder = new ImmutableSet.Builder<>();
    for (String pNode : pNodes) {
      builder.add(getCFANode(pNode));
    }
    return builder.build();
  }

  public CustomInstruction readCustomInstruction(final String functionName)
      throws InterruptedException, AppliedCustomInstructionParsingFailedException {
    FunctionEntryNode function = cfa.getFunctionHead(functionName);

    if (function == null) {
      throw new AppliedCustomInstructionParsingFailedException("Function unknown in program");
    }

    CFANode ciStartNode = null;
    Set<CFANode> ciEndNodes = new HashSet<>();

    Set<CFANode> visitedNodes = new HashSet<>();
    Queue<CFANode> queue = new ArrayDeque<>();

    queue.add(function);
    visitedNodes.add(function);

    CFANode pred;

    // search for CFALabelNode with label "start_ci"
    while (!queue.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      pred = queue.poll();

      if (pred instanceof CFALabelNode
          && ((CFALabelNode) pred).getLabel().equals("start_ci")
          && pred.getFunctionName().equals(functionName)) {
        ciStartNode = pred;
        break;
      }

      // breadth-first-search
      for (CFANode succ : CFAUtils.allSuccessorsOf(pred)) {
        if (!visitedNodes.contains(succ) && succ.getFunctionName().equals(functionName)) {
          queue.add(succ);
          visitedNodes.add(succ);
        }
      }
    }

    if (ciStartNode == null) {
      throw new AppliedCustomInstructionParsingFailedException(
          "Missing label for start of custom instruction");
    }

    Queue<Pair<CFANode, Set<String>>> pairQueue = new ArrayDeque<>();
    Set<String> inputVariables = new HashSet<>();
    Set<String> outputVariables = new HashSet<>();
    Set<String> predOutputVars = new HashSet<>();
    Set<String> succOutputVars;
    Set<Pair<CFANode, Set<String>>> visitedPairs = new HashSet<>();
    Pair<CFANode, Set<String>> nextPair;
    Pair<CFANode, Set<String>> nextNode = Pair.of(ciStartNode, predOutputVars);
    pairQueue.add(nextNode);
    Set<FunctionEntryNode> functionsWithoutGlobalVars = new HashSet<>();
    boolean usesMultiEdges = false;

    while (!pairQueue.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      nextNode = pairQueue.poll();
      pred = nextNode.getFirst();
      predOutputVars = nextNode.getSecond();

      // pred is endNode of CI -> store pred in Collection of endNodes
      if (pred instanceof CFALabelNode && ((CFALabelNode) pred).getLabel().startsWith("end_ci_")) {
        CFAUtils.predecessorsOf(pred).copyInto(ciEndNodes);
        continue;
      }

      // search for endNodes in the subtree of pred, breadth-first search
      for (CFAEdge leavingEdge : pred.getLeavingEdges()) {
        if (leavingEdge instanceof FunctionReturnEdge) {
          continue;
        }

        // adapt output, inputvariables
        addNewInputVariables(leavingEdge, predOutputVars, inputVariables);
        succOutputVars =
            getOutputVariablesForSuccessorAndAddNewOutputVariables(
                leavingEdge, predOutputVars, outputVariables);

        // breadth-first-search within method
        if (leavingEdge instanceof FunctionCallEdge) {
          if (!noGlobalVariablesUsed(
              functionsWithoutGlobalVars, ((CFunctionCallEdge) leavingEdge).getSuccessor())) {
            throw new AppliedCustomInstructionParsingFailedException(
                "Function "
                    + leavingEdge.getSuccessor().getFunctionName()
                    + " is not side-effect free, uses global variables");
          }
          nextPair = Pair.of(((CFunctionCallEdge) leavingEdge).getReturnNode(), succOutputVars);
        } else {
          nextPair = Pair.of(leavingEdge.getSuccessor(), succOutputVars);
        }

        if (visitedPairs.add(nextPair)) {
          pairQueue.add(nextPair);
        }
      }
    }

    if (usesMultiEdges) {
      logger.log(
          Level.WARNING,
          "Multi edges used in custom instruction. Results may be unreliable. Disable option"
              + " cfa.useMultiEdges to get reliable results.");
    }

    if (ciEndNodes.isEmpty()) {
      throw new AppliedCustomInstructionParsingFailedException(
          "Missing label for end of custom instruction");
    }

    ImmutableList<String> outputVariablesAsList = ImmutableList.sortedCopyOf(outputVariables);
    ImmutableList<String> inputVariablesAsList = ImmutableList.sortedCopyOf(inputVariables);

    return new CustomInstruction(
        ciStartNode, ciEndNodes, inputVariablesAsList, outputVariablesAsList, shutdownNotifier);
  }

  private void addNewInputVariables(
      final CFAEdge pLeavingEdge,
      final Set<String> pPredOutputVars,
      final Set<String> pInputVariables) {
    for (String var : getPotentialInputVariables(pLeavingEdge)) {
      if (!pPredOutputVars.contains(var)) {
        pInputVariables.add(var);
      }
    }
  }

  private Collection<String> getPotentialInputVariables(final CFAEdge pLeavingEdge) {
    switch (pLeavingEdge) {
      case CStatementEdge cStatementEdge -> {
        CStatement edgeStmt = cStatementEdge.getStatement();
        switch (edgeStmt) {
          case CExpressionAssignmentStatement cExpressionAssignmentStatement -> {
            return CFAUtils.getVariableNamesOfExpression(
                    cExpressionAssignmentStatement.getRightHandSide())
                .toSet();
          }
          case CExpressionStatement cExpressionStatement -> {
            return CFAUtils.getVariableNamesOfExpression(cExpressionStatement.getExpression())
                .toSet();
          }
          case CFunctionCallStatement cFunctionCallStatement -> {
            return getFunctionParameterInput(cFunctionCallStatement.getFunctionCallExpression());
          }
          case CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement -> {
            return getFunctionParameterInput(
                cFunctionCallAssignmentStatement.getFunctionCallExpression());
          }
          case null /*TODO check if null is necessary*/, default -> {}
        }
      }
      case CDeclarationEdge cDeclarationEdge -> {
        CDeclaration edgeDec = cDeclarationEdge.getDeclaration();
        if (edgeDec instanceof CVariableDeclaration cVariableDeclaration) {
          CInitializer edgeDecInit = cVariableDeclaration.getInitializer();
          if (edgeDecInit instanceof CInitializerExpression cInitializerExpression) {
            return CFAUtils.getVariableNamesOfExpression(cInitializerExpression.getExpression())
                .toSet();
          }
        }
      }
      case CReturnStatementEdge cReturnStatementEdge -> {
        Optional<CExpression> edgeExp = cReturnStatementEdge.getExpression();
        if (edgeExp.isPresent()) {
          return CFAUtils.getVariableNamesOfExpression(edgeExp.orElseThrow()).toSet();
        }
      }
      case CAssumeEdge cAssumeEdge -> {
        return CFAUtils.getVariableNamesOfExpression(cAssumeEdge.getExpression()).toSet();
      }
      case CFunctionCallEdge cFunctionCallEdge -> {
        return from(cFunctionCallEdge.getArguments())
            .transformAndConcat(CFAUtils::getVariableNamesOfExpression)
            .toSet();
      }
      case null /*TODO check if null is necessary*/, default -> {}
    }
    return ImmutableSet.of();
  }

  private Set<String> getFunctionParameterInput(final CFunctionCallExpression funCall) {
    return from(funCall.getParameterExpressions())
        .transformAndConcat(CFAUtils::getVariableNamesOfExpression)
        .toSet();
  }

  private Set<String> getOutputVariablesForSuccessorAndAddNewOutputVariables(
      final CFAEdge pLeavingEdge,
      final Set<String> pPredOutputVars,
      final Set<String> pOutputVariables) {
    Set<String> edgeOutputVariables;
    switch (pLeavingEdge) {
      case CStatementEdge cStatementEdge -> {
        CStatement edgeStmt = cStatementEdge.getStatement();
        if (edgeStmt instanceof CExpressionAssignmentStatement cExpressionAssignmentStatement) {
          edgeOutputVariables =
              CFAUtils.getVariableNamesOfExpression(
                      cExpressionAssignmentStatement.getLeftHandSide())
                  .toSet();
        } else if (edgeStmt
            instanceof CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement) {
          edgeOutputVariables =
              getFunctionalCallAssignmentOutputVars(cFunctionCallAssignmentStatement);
        } else {
          return pPredOutputVars;
        }
      }
      case CDeclarationEdge cDeclarationEdge ->
          edgeOutputVariables =
              ImmutableSet.of(cDeclarationEdge.getDeclaration().getQualifiedName());
      case CFunctionCallEdge cFunctionCallEdge -> {
        CFunctionCall funCall = cFunctionCallEdge.getFunctionCall();
        if (funCall instanceof CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement) {
          edgeOutputVariables =
              getFunctionalCallAssignmentOutputVars(cFunctionCallAssignmentStatement);
        } else {
          edgeOutputVariables = ImmutableSet.of();
        }
      }
      case null /*TODO check if null is necessary*/, default -> {
        return pPredOutputVars;
      }
    }

    pOutputVariables.addAll(edgeOutputVariables);
    Set<String> returnRes = new HashSet<>(pPredOutputVars);
    returnRes.addAll(edgeOutputVariables);

    return returnRes;
  }

  private Set<String> getFunctionalCallAssignmentOutputVars(
      final CFunctionCallAssignmentStatement stmt) {
    return CFAUtils.getVariableNamesOfExpression(stmt.getLeftHandSide()).toSet();
  }

  private boolean noGlobalVariablesUsed(
      final Set<FunctionEntryNode> noGlobalVarUse, final FunctionEntryNode function) {
    Deque<CFANode> toVisit = new ArrayDeque<>();
    Collection<CFANode> visited = new HashSet<>();

    toVisit.push(function);
    visited.add(function);

    while (!toVisit.isEmpty()) {
      CFANode visit = toVisit.pop();

      if (visit instanceof FunctionExitNode) {
        continue;
      }

      if (visit instanceof FunctionEntryNode functionEntryNode
          && !noGlobalVarUse.add(functionEntryNode)) {
        continue;
      }

      for (CFAEdge leave : visit.getAllLeavingEdges()) {
        if (containsGlobalVars(leave)) {
          return false;
        }

        CFANode successor = leave.getSuccessor();
        if (visited.add(successor)) {
          toVisit.push(successor);
        }
      }
    }

    return true;
  }

  private boolean containsGlobalVars(final CFAEdge pLeave) {
    return switch (pLeave.getEdgeType()) {
      case BlankEdge -> false; // no additional check needed.

      case AssumeEdge -> ((CAssumeEdge) pLeave).getExpression().accept(visitor);

      case StatementEdge -> globalVarInStatement(((CStatementEdge) pLeave).getStatement());

      case DeclarationEdge -> {
        if (((CDeclarationEdge) pLeave).getDeclaration() instanceof CVariableDeclaration) {
          CInitializer init =
              ((CVariableDeclaration) ((CDeclarationEdge) pLeave).getDeclaration())
                  .getInitializer();
          if (init != null) {
            yield init.accept(visitor);
          }
        }
        yield false;
      }
      case ReturnStatementEdge -> {
        if (((CReturnStatementEdge) pLeave).getExpression().isPresent()) {
          yield ((CReturnStatementEdge) pLeave).getExpression().orElseThrow().accept(visitor);
        }
        yield false;
      }
      case FunctionCallEdge -> {
        for (CExpression exp : ((CFunctionCallEdge) pLeave).getArguments()) {
          if (exp.accept(visitor)) {
            yield true;
          }
        }
        yield false;
      }
      case FunctionReturnEdge -> false; // no additional check needed.

      case CallToReturnEdge ->
          globalVarInStatement(((CFunctionSummaryEdge) pLeave).getExpression());
    };
  }

  private boolean globalVarInStatement(final CStatement statement) {
    switch (statement) {
      case CExpressionStatement cExpressionStatement -> {
        return cExpressionStatement.getExpression().accept(visitor);
      }
      case CFunctionCallStatement cFunctionCallStatement -> {
        for (CExpression param :
            cFunctionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
          if (param.accept(visitor)) {
            return true;
          }
        }
      }
      case CExpressionAssignmentStatement cExpressionAssignmentStatement -> {
        if (cExpressionAssignmentStatement.getLeftHandSide().accept(visitor)) {
          return true;
        }
        return cExpressionAssignmentStatement.getRightHandSide().accept(visitor);
      }
      case CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement -> {
        if (cFunctionCallAssignmentStatement.getLeftHandSide().accept(visitor)) {
          return true;
        }
        for (CExpression param :
            cFunctionCallAssignmentStatement
                .getFunctionCallExpression()
                .getParameterExpressions()) {
          if (param.accept(visitor)) {
            return true;
          }
        }
      }
      case null /*TODO check if null is necessary*/, default -> {}
    }
    return false;
  }

  private static class GlobalVarCheckVisitor extends DefaultCExpressionVisitor<Boolean, NoException>
      implements CInitializerVisitor<Boolean, NoException>,
          CDesignatorVisitor<Boolean, RuntimeException> {

    @Override
    public Boolean visit(final CArraySubscriptExpression pIastArraySubscriptExpression) {
      if (!pIastArraySubscriptExpression.getArrayExpression().accept(this)) {
        return pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
      }
      return true;
    }

    @Override
    public Boolean visit(final CFieldReference pIastFieldReference) {
      return pIastFieldReference.getFieldOwner().accept(this);
    }

    @Override
    public Boolean visit(final CIdExpression pIastIdExpression) {
      // test if global variable
      if (pIastIdExpression
          .getDeclaration()
          .getQualifiedName()
          .equals(pIastIdExpression.getDeclaration().getName())) {
        return true;
      }
      return false;
    }

    @Override
    public Boolean visit(final CPointerExpression pPointerExpression) {
      return pPointerExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(final CComplexCastExpression pComplexCastExpression) {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(final CBinaryExpression pIastBinaryExpression) {
      if (!pIastBinaryExpression.getOperand1().accept(this)) {
        return pIastBinaryExpression.getOperand2().accept(this);
      }
      return true;
    }

    @Override
    public Boolean visit(final CCastExpression pIastCastExpression) {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(final CUnaryExpression pIastUnaryExpression) {
      return pIastUnaryExpression.getOperand().accept(this);
    }

    @Override
    protected Boolean visitDefault(final CExpression pExp) {
      return false;
    }

    @Override
    public Boolean visit(final CInitializerExpression pInitializerExpression) {
      return pInitializerExpression.getExpression().accept(this);
    }

    @Override
    public Boolean visit(final CInitializerList pInitializerList) {
      for (CInitializer init : pInitializerList.getInitializers()) {
        if (init.accept(this)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public Boolean visit(final CDesignatedInitializer pCStructInitializerPart) {
      for (CDesignator des : pCStructInitializerPart.getDesignators()) {
        if (des.accept(this)) {
          return true;
        }
      }
      if (pCStructInitializerPart.getRightHandSide() != null) {
        return pCStructInitializerPart.getRightHandSide().accept(this);
      }
      return false;
    }

    @Override
    public Boolean visit(final CArrayDesignator pArrayDesignator) {
      return pArrayDesignator.getSubscriptExpression().accept(this);
    }

    @Override
    public Boolean visit(final CArrayRangeDesignator pArrayRangeDesignator) {
      if (pArrayRangeDesignator.getCeilExpression().accept(this)) {
        return true;
      }
      return pArrayRangeDesignator.getFloorExpression().accept(this);
    }

    @Override
    public Boolean visit(final CFieldDesignator pFieldDesignator) {
      return false;
    }
  }

  public boolean isAppliedCI(final CustomInstruction pCi, final CFANode pNode) {
    try {
      return pCi.inspectAppliedCustomInstruction(pNode) != null;
    } catch (AppliedCustomInstructionParsingFailedException | InterruptedException e) {
      return false;
    }
  }
}
