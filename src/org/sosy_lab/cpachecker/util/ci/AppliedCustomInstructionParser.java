/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.io.MoreFiles;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;


public class AppliedCustomInstructionParser {

  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;
  private final CFA cfa;
  private final GlobalVarCheckVisitor visitor = new GlobalVarCheckVisitor();

  public AppliedCustomInstructionParser(final ShutdownNotifier pShutdownNotifier, final LogManager pLogger,
      final CFA pCfa) {
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    cfa = pCfa;
  }

  /**
   * Creates a CustomInstructionApplication if the file contains all required data, null if not
   * @param file Path of the file to be read
   * @param signatureFile Path of the file into which the ci signature will be written
   * @return CustomInstructionApplication
   * @throws IOException if the file doesn't contain all required data.
   */
  public CustomInstructionApplications parse(final Path file, final Path signatureFile)
      throws IOException, AppliedCustomInstructionParsingFailedException, InterruptedException {

    CustomInstruction ci = null;


    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile()), "UTF-8"))) {
      String line = br.readLine();
      if(line == null) {
        throw new AppliedCustomInstructionParsingFailedException("Empty specification. Missing at least function name for custom instruction.");
      }

      ci = readCustomInstruction(line);

      writeCustomInstructionSpecification(ci, signatureFile);

      return parseACIs(br, ci);
    }
  }

  private void writeCustomInstructionSpecification(final CustomInstruction ci,
      final Path signatureFile) throws IOException {
    try (Writer br = MoreFiles.openOutputFile(signatureFile, Charset.defaultCharset())) {
      br.write(ci.getSignature() + "\n");
      String ciString = ci.getFakeSMTDescription().getSecond();
      br.write(ciString.substring(ciString.indexOf("a")-1,ciString.length()-1) + ";");
    }
  }


  public CustomInstructionApplications parse(final CustomInstruction pCi, final Path file)
      throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile()), "UTF-8"))) {
      return parseACIs(br, pCi);
    }
  }

  private CustomInstructionApplications parseACIs(final BufferedReader br, final CustomInstruction ci)
      throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException {
    Builder<CFANode, AppliedCustomInstruction> map = new ImmutableMap.Builder<>();
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().get();

    CFANode startNode;
    AppliedCustomInstruction aci;
    String line;

    while ((line = br.readLine()) != null) {
      shutdownNotifier.shutdownIfNecessary();
      startNode = getCFANode(line, cfaInfo);
      if (startNode == null) {
        continue;
      }


      try {
        aci = ci.inspectAppliedCustomInstruction(startNode);
      } catch (InterruptedException ex) {
        throw new AppliedCustomInstructionParsingFailedException("Parsing failed because of ShutdownNotifier: "
            + ex.getMessage());
      }

      map.put(startNode, aci);
    }

    return new CustomInstructionApplications(map.build(), ci);
  }

  /**
   * Creates a new CFANode with respect to the given parameters
   * @param pNodeID String
   * @param cfaInfo CFAInfo
   * @return a new CFANode with respect to the given parameters
   * @throws AppliedCustomInstructionParsingFailedException if the node can't be created
   */
  protected CFANode getCFANode (final String pNodeID, final CFAInfo cfaInfo) throws AppliedCustomInstructionParsingFailedException{
    try{
      return cfaInfo.getNodeByNodeNumber(Integer.parseInt(pNodeID));
    } catch (NumberFormatException ex) {
      throw new AppliedCustomInstructionParsingFailedException
        ("It is not possible to parse " + pNodeID + " to an integer!", ex);
    }
  }

  /**
   * Creates a ImmutableSet out of the given String[].
   * @param pNodes String[]
   * @return Immutable Set of CFANodes out of the String[]
   */
  protected ImmutableSet<CFANode> getCFANodes (final String[] pNodes, final CFAInfo cfaInfo) throws AppliedCustomInstructionParsingFailedException {
    ImmutableSet.Builder<CFANode> builder = new ImmutableSet.Builder<>();
    for (String pNode : pNodes) {
      builder.add(getCFANode(pNode, cfaInfo));
    }
    return builder.build();
  }

  protected CustomInstruction readCustomInstruction(final String functionName)
      throws InterruptedException, AppliedCustomInstructionParsingFailedException {
    FunctionEntryNode function = cfa.getFunctionHead(functionName);

    if (function == null) {
      throw new AppliedCustomInstructionParsingFailedException("Function unknown in program");
    }

    CFANode ciStartNode = null;
    Collection<CFANode> ciEndNodes = new HashSet<>();

    Set<CFANode> visitedNodes = new HashSet<>();
    Queue<CFANode> queue = new ArrayDeque<>();

    queue.add(function);
    visitedNodes.add(function);

    CFANode pred;

    // search for CLabelNode with label "start_ci"
    while (!queue.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      pred = queue.poll();

      if (pred instanceof CLabelNode && ((CLabelNode) pred).getLabel().equals("start_ci") && pred.getFunctionName().equals(functionName)) {
        ciStartNode = pred;
        break;
      }

      // breadth-first-search
      for (CFANode succ : CFAUtils.allSuccessorsOf(pred)) {
        if (!visitedNodes.contains(succ) && succ.getFunctionName().equals(functionName)){
          queue.add(succ);
          visitedNodes.add(succ);
        }
      }
    }

    if (ciStartNode == null) {
      throw new AppliedCustomInstructionParsingFailedException("Missing label for start of custom instruction");
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

    while(!pairQueue.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      nextNode = pairQueue.poll();
      pred = nextNode.getFirst();
      predOutputVars = nextNode.getSecond();

      // pred is endNode of CI -> store pred in Collection of endNodes
      if (pred instanceof CLabelNode && ((CLabelNode)pred).getLabel().startsWith("end_ci_")) {
        for (CFANode endNode : CFAUtils.predecessorsOf(pred)) {
          ciEndNodes.add(endNode);
        }
        continue;
      }

      // search for endNodes in the subtree of pred, breadth-first search
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(pred)) {
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
          if (!noGlobalVariablesUsed(functionsWithoutGlobalVars, ((CFunctionCallEdge) leavingEdge).getSuccessor())) {
            throw new AppliedCustomInstructionParsingFailedException(
              "Function " + leavingEdge.getSuccessor().getFunctionName()
                  + " is not side effect free, uses global variables");
          }
          nextPair = Pair.of(((CFunctionCallEdge) leavingEdge).getSummaryEdge().getSuccessor(), succOutputVars);
        } else {
          nextPair = Pair.of(leavingEdge.getSuccessor(), succOutputVars);
        }

        if (visitedPairs.add(nextPair)) {
          pairQueue.add(nextPair);
        }
      }
    }

    if (usesMultiEdges) {
      logger.log(Level.WARNING, "Multi edges used in custom instruction. Results may be unreliable. Disable option cfa.useMultiEdges to get reliable results.");
    }

    if (ciEndNodes.isEmpty()) {
      throw new AppliedCustomInstructionParsingFailedException("Missing label for end of custom instruction");
    }

    List<String> outputVariablesAsList = new ArrayList<>();
    outputVariablesAsList.addAll(outputVariables);
    Collections.sort(outputVariablesAsList);

    List<String> inputVariablesAsList = new ArrayList<>();
    inputVariablesAsList.addAll(inputVariables);
    Collections.sort(inputVariablesAsList);

    return new CustomInstruction(ciStartNode, ciEndNodes, inputVariablesAsList, outputVariablesAsList, shutdownNotifier);
  }

  private void addNewInputVariables(final CFAEdge pLeavingEdge, final Set<String> pPredOutputVars,
      final Set<String> pInputVariables) {
    for(String var : getPotentialInputVariables(pLeavingEdge)) {
      if(!pPredOutputVars.contains(var)) {
        pInputVariables.add(var);
      }
    }
  }

  private Collection<String> getPotentialInputVariables(final CFAEdge pLeavingEdge) {
    if (pLeavingEdge instanceof CStatementEdge) {
      CStatement edgeStmt = ((CStatementEdge) pLeavingEdge).getStatement();

      if (edgeStmt instanceof CExpressionAssignmentStatement) {
        return CFAUtils.getVariableNamesOfExpression(
                ((CExpressionAssignmentStatement) edgeStmt).getRightHandSide())
            .toSet();
      }

      else if (edgeStmt instanceof CExpressionStatement) {
        return CFAUtils.getVariableNamesOfExpression(
                ((CExpressionStatement) edgeStmt).getExpression())
            .toSet();
      }

      else if (edgeStmt instanceof CFunctionCallStatement) {
        return getFunctionParameterInput(((CFunctionCallStatement) edgeStmt).getFunctionCallExpression());
      }

      else if (edgeStmt instanceof CFunctionCallAssignmentStatement) {
        return getFunctionParameterInput(((CFunctionCallAssignmentStatement) edgeStmt)
          .getFunctionCallExpression()); }
    }


    else if (pLeavingEdge instanceof CDeclarationEdge) {
      CDeclaration edgeDec = ((CDeclarationEdge) pLeavingEdge).getDeclaration();
      if (edgeDec instanceof CVariableDeclaration) {
        CInitializer edgeDecInit = ((CVariableDeclaration) edgeDec).getInitializer();
        if (edgeDecInit instanceof CInitializerExpression) {
          return CFAUtils.getVariableNamesOfExpression(
                  ((CInitializerExpression) edgeDecInit).getExpression())
              .toSet();
        }
      }
    }

    else if (pLeavingEdge instanceof CReturnStatementEdge) {
      Optional<CExpression> edgeExp = ((CReturnStatementEdge) pLeavingEdge).getExpression();
      if (edgeExp.isPresent()) {
        return CFAUtils.getVariableNamesOfExpression(edgeExp.get()).toSet();
      }
    }

    else if (pLeavingEdge instanceof CAssumeEdge) {
      return CFAUtils.getVariableNamesOfExpression(((CAssumeEdge) pLeavingEdge).getExpression())
          .toSet();
    }

    else if (pLeavingEdge instanceof CFunctionCallEdge) {
      return from(((CFunctionCallEdge) pLeavingEdge).getArguments())
          .transformAndConcat(CFAUtils::getVariableNamesOfExpression)
          .toSet();
    }
    return Collections.emptySet();
  }

 private Set<String> getFunctionParameterInput(final CFunctionCallExpression funCall) {
    return from(funCall.getParameterExpressions())
        .transformAndConcat(CFAUtils::getVariableNamesOfExpression)
        .toSet();
 }

  private Set<String> getOutputVariablesForSuccessorAndAddNewOutputVariables(final CFAEdge pLeavingEdge,
      final Set<String> pPredOutputVars, final Set<String> pOutputVariables) {
    Set<String> edgeOutputVariables;
    if (pLeavingEdge instanceof CStatementEdge) {
      CStatement edgeStmt = ((CStatementEdge) pLeavingEdge).getStatement();
      if (edgeStmt instanceof CExpressionAssignmentStatement) {
        edgeOutputVariables =
            CFAUtils.getVariableNamesOfExpression(
                    ((CExpressionAssignmentStatement) edgeStmt).getLeftHandSide())
                .toSet();
      }
      else if (edgeStmt instanceof CFunctionCallAssignmentStatement) {
        edgeOutputVariables = getFunctionalCallAssignmentOutputVars((CFunctionCallAssignmentStatement) edgeStmt);
      } else {
        return pPredOutputVars;
      }
    } else if (pLeavingEdge instanceof CDeclarationEdge) {
      edgeOutputVariables =
          ImmutableSet.of(((CDeclarationEdge) pLeavingEdge).getDeclaration().getQualifiedName());

    } else if (pLeavingEdge instanceof CFunctionCallEdge) {
      CFunctionCall funCall = (((CFunctionCallEdge) pLeavingEdge).getSummaryEdge().getExpression());
      if (funCall instanceof CFunctionCallAssignmentStatement) {
        edgeOutputVariables = getFunctionalCallAssignmentOutputVars((CFunctionCallAssignmentStatement) funCall);
      } else {
        edgeOutputVariables = ImmutableSet.of();
      }
    } else {
      return pPredOutputVars;
    }

    pOutputVariables.addAll(edgeOutputVariables);
    HashSet<String> returnRes = new HashSet<>(pPredOutputVars);
    returnRes.addAll(edgeOutputVariables);

    return returnRes;
  }

  private Set<String> getFunctionalCallAssignmentOutputVars(final CFunctionCallAssignmentStatement stmt) {
    return CFAUtils.getVariableNamesOfExpression(stmt.getLeftHandSide()).toSet();
  }

  private boolean noGlobalVariablesUsed(final Set<FunctionEntryNode> noGlobalVarUse, final FunctionEntryNode function) {
    Deque<CFANode> toVisit = new ArrayDeque<>();
    Collection<CFANode> visited = new HashSet<>();
    CFANode visit, successor;

    toVisit.push(function);
    visited.add(function);

    while(!toVisit.isEmpty()) {
      visit = toVisit.pop();

      if(visit instanceof FunctionExitNode) {
        continue;
      }

      if (visit instanceof FunctionEntryNode && !noGlobalVarUse.add((FunctionEntryNode) visit)) {
        continue;
      }

      for(CFAEdge leave : CFAUtils.allLeavingEdges(visit)) {
        if(containsGlobalVars(leave)) {
          return false;
        }

        successor = leave.getSuccessor();
        if(visited.add(successor)) {
          toVisit.push(successor);
        }
      }
    }

    return true;
  }

  private boolean containsGlobalVars(final CFAEdge pLeave) {
    switch (pLeave.getEdgeType()) {
    case BlankEdge:
      // no additional check needed.
      break;
    case AssumeEdge:
      return ((CAssumeEdge) pLeave).getExpression().accept(visitor);
    case StatementEdge:
      return globalVarInStatement(((CStatementEdge) pLeave).getStatement());
    case DeclarationEdge:
      if(((CDeclarationEdge) pLeave).getDeclaration() instanceof CVariableDeclaration) {
       CInitializer init = ((CVariableDeclaration) ((CDeclarationEdge) pLeave).getDeclaration()).getInitializer();
       if(init != null) {
         return init.accept(visitor);
       }
      }
      break;
    case ReturnStatementEdge:
      if (((CReturnStatementEdge) pLeave).getExpression().isPresent()) { return ((CReturnStatementEdge) pLeave)
          .getExpression().get().accept(visitor); }
      break;
    case FunctionCallEdge:
      for (CExpression exp : ((CFunctionCallEdge) pLeave).getArguments()) {
        if (exp.accept(visitor)) { return true; }
      }
      break;
    case FunctionReturnEdge:
      // no additional check needed.
      break;
    case CallToReturnEdge:
      return globalVarInStatement(((CFunctionSummaryEdge) pLeave).getExpression());
    default:
      throw new AssertionError("Unhandled enum value in switch: " + pLeave.getEdgeType());
    }
    return false;
  }

  private boolean globalVarInStatement(final CStatement statement) {
    if (statement instanceof CExpressionStatement) {
      return ((CExpressionStatement) statement).getExpression().accept(visitor);
    } else if (statement instanceof CFunctionCallStatement) {
      for (CExpression param : ((CFunctionCallStatement) statement).getFunctionCallExpression()
          .getParameterExpressions()) {
        if (param.accept(visitor)) { return true; }
      }
    } else if (statement instanceof CExpressionAssignmentStatement) {
      if (((CExpressionAssignmentStatement) statement).getLeftHandSide().accept(visitor)) { return true; }
      return ((CExpressionAssignmentStatement) statement).getRightHandSide().accept(visitor);
    } else if (statement instanceof CFunctionCallAssignmentStatement) {
      if (((CFunctionCallAssignmentStatement) statement).getLeftHandSide().accept(visitor)) { return true; }
      for (CExpression param : ((CFunctionCallAssignmentStatement) statement).getFunctionCallExpression()
          .getParameterExpressions()) {
        if (param.accept(visitor)) { return true; }
      }
    }
    return false;
  }

  private static class GlobalVarCheckVisitor extends DefaultCExpressionVisitor<Boolean, RuntimeException> implements
      CInitializerVisitor<Boolean, RuntimeException>, CDesignatorVisitor<Boolean, RuntimeException> {

    @Override
    public Boolean visit(final CArraySubscriptExpression pIastArraySubscriptExpression) throws RuntimeException {
      if (!pIastArraySubscriptExpression.getArrayExpression().accept(this)) {
        return pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
      }
      return Boolean.TRUE;
    }

    @Override
    public Boolean visit(final CFieldReference pIastFieldReference) throws RuntimeException {
      return pIastFieldReference.getFieldOwner().accept(this);
    }

    @Override
    public Boolean visit(final CIdExpression pIastIdExpression) throws RuntimeException {
      // test if global variable
      if (pIastIdExpression
          .getDeclaration()
          .getQualifiedName()
          .equals(pIastIdExpression.getDeclaration().getName())) {
        return Boolean.TRUE;
      }
      return Boolean.FALSE;
    }

    @Override
    public Boolean visit(final CPointerExpression pPointerExpression) throws RuntimeException {
      return pPointerExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(final CComplexCastExpression pComplexCastExpression) throws RuntimeException {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(final CBinaryExpression pIastBinaryExpression) throws RuntimeException {
      if (!pIastBinaryExpression.getOperand1().accept(this)) {
        return pIastBinaryExpression.getOperand2().accept(this);
      }
      return Boolean.TRUE;
    }

    @Override
    public Boolean visit(final CCastExpression pIastCastExpression) throws RuntimeException {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(final CUnaryExpression pIastUnaryExpression) throws RuntimeException {
      return pIastUnaryExpression.getOperand().accept(this);
    }

    @Override
    protected Boolean visitDefault(final CExpression pExp) throws RuntimeException {
      return Boolean.FALSE;
    }

    @Override
    public Boolean visit(final CInitializerExpression pInitializerExpression) throws RuntimeException {
      return pInitializerExpression.getExpression().accept(this);
    }

    @Override
    public Boolean visit(final CInitializerList pInitializerList) throws RuntimeException {
      for(CInitializer init : pInitializerList.getInitializers()) {
        if(init.accept(this)) {
          return Boolean.TRUE;
        }
      }
      return Boolean.FALSE;
    }

    @Override
    public Boolean visit(final CDesignatedInitializer pCStructInitializerPart) throws RuntimeException {
      for(CDesignator des : pCStructInitializerPart.getDesignators()) {
        if(des.accept(this)) {
          return Boolean.TRUE;
        }
      }
      if(pCStructInitializerPart.getRightHandSide() != null) {
        return pCStructInitializerPart.getRightHandSide().accept(this);
      }
      return Boolean.FALSE;
    }

    @Override
    public Boolean visit(final CArrayDesignator pArrayDesignator) throws RuntimeException {
      return pArrayDesignator.getSubscriptExpression().accept(this);
    }

    @Override
    public Boolean visit(final CArrayRangeDesignator pArrayRangeDesignator) throws RuntimeException {
      if(pArrayRangeDesignator.getCeilExpression().accept(this)) {
        return Boolean.TRUE;
      }
      return pArrayRangeDesignator.getFloorExpression().accept(this);
    }

    @Override
    public Boolean visit(final CFieldDesignator pFieldDesignator) throws RuntimeException {
      return Boolean.FALSE;
    }

  }

}