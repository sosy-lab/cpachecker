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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectorVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;


public class AppliedCustomInstructionParser {

  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  public AppliedCustomInstructionParser(final ShutdownNotifier pShutdownNotifier, final CFA pCfa) {
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;
  }

  /**
   * Creates a CustomInstructionApplication if the file contains all required data, null if not
   * @param file Path of the file to be read
   * @return CustomInstructionApplication
   * @throws IOException if the file doesn't contain all required data.
   * @throws AppliedCustomInstructionParsingFailedException
   * @throws InterruptedException
   */
  public CustomInstructionApplications parse (Path file)
      throws IOException, AppliedCustomInstructionParsingFailedException, InterruptedException {

    Builder<CFANode, AppliedCustomInstruction> map = new ImmutableMap.Builder<>();
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().get();

    CFANode startNode;
    CustomInstruction ci = null;
    AppliedCustomInstruction aci;


    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile()), "UTF-8"))) {
      String line = br.readLine();
      ci = readCustomInstruction(line);

      while ((line = br.readLine()) != null) {
        shutdownNotifier.shutdownIfNecessary();

        if ((line = br.readLine()) == null) {
          throw new AppliedCustomInstructionParsingFailedException("Wrong format, specification of end nodes not found. Expect that a custom instruction is specified in two lines. First line contains the start node and second line the end nodes.");
        }

        startNode = getCFANode(line, cfaInfo);

        try {
          aci = ci.inspectAppliedCustomInstruction(startNode);
        } catch (InterruptedException ex) {
          throw new AppliedCustomInstructionParsingFailedException("Parsing failed because of ShutdownNotifier: " + ex.getMessage());
        }

        map.put(startNode, aci);
      }
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
  public CFANode getCFANode (String pNodeID, CFAInfo cfaInfo) throws AppliedCustomInstructionParsingFailedException{
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
   * @throws AppliedCustomInstructionParsingFailedException
   */
  public ImmutableSet<CFANode> getCFANodes (String[] pNodes, CFAInfo cfaInfo) throws AppliedCustomInstructionParsingFailedException {
    ImmutableSet.Builder<CFANode> builder = new ImmutableSet.Builder<>();
    for (int i=0; i<pNodes.length; i++) {
      builder.add(getCFANode(pNodes[i], cfaInfo));
    }
    return builder.build();
  }

  public CustomInstruction readCustomInstruction(String functionName)
      throws InterruptedException, AppliedCustomInstructionParsingFailedException {
    FunctionEntryNode function = cfa.getFunctionHead(functionName);

    CFANode ciStartNode = null;
    Collection<CFANode> ciEndNodes = new HashSet<>();
    Set<String> inputVariables = new HashSet<>();
    Set<String> outputVariables = new HashSet<>();

    Set<CFANode> visitedNodes = new HashSet<>();
    Queue<CFANode> queue = new ArrayDeque<>();

    queue.add(function);
    visitedNodes.add(function);

    CFANode pred;

    // search for CLabelNode with label "start_ci"
    while (!queue.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      pred = queue.poll();

      if (pred instanceof CLabelNode && ((CLabelNode) pred).getLabel().equals("start_ci")) {
        ciStartNode = pred;
        break;
      }

      // breadth-first-search
      for (CFANode succ : CFAUtils.successorsOf(pred)) {
        if (!visitedNodes.contains(succ)){
          queue.add(succ);
          visitedNodes.add(succ);
        }
      }
    }

    Queue<Pair<CFANode, Set<String>>> pairQueue = new ArrayDeque<>();
    CFANode succ;
    Set<String> predOutputVars = new HashSet<>();
    Set<String> succOutputVars;
    Set<Pair<CFANode, Set<String>>> visitedPairs = new HashSet<>();
    Pair<CFANode, Set<String>> nextPair;
    Pair<CFANode, Set<String>> nextNode = Pair.of(ciStartNode, predOutputVars);
    pairQueue.add(nextNode);

    while(!pairQueue.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      nextNode = pairQueue.poll();
      pred = nextNode.getFirst();
      predOutputVars = nextNode.getSecond();

      // pred is endNode of CI -> store pred in Collection of endNodes
      if (pred instanceof CLabelNode && ((CLabelNode)pred).getLabel().startsWith("end_ci_")) {
        ciEndNodes.add(pred);
      }

      // search for endNodes in the subtree of pred, breadth-first search
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(pred)) {
        if (leavingEdge instanceof MultiEdge) {
          succOutputVars = predOutputVars;
          for (CFAEdge innerEdge : ((MultiEdge) leavingEdge).getEdges()) {
            // adapt output, inputvariables
            addNewInputVariables(innerEdge, succOutputVars, inputVariables);
            succOutputVars =
                getOutputVariablesForSuccessorAndAddNewOutputVariables(innerEdge, succOutputVars, outputVariables);
          }
        } else {
          // adapt output, inputvariables
          addNewInputVariables(leavingEdge, predOutputVars, inputVariables);
          succOutputVars =
              getOutputVariablesForSuccessorAndAddNewOutputVariables(leavingEdge, predOutputVars, outputVariables);
        }

        // breadth-first-search
        succ = leavingEdge.getSuccessor();
        nextPair = Pair.of(succ, succOutputVars);
        if (!visitedPairs.contains(nextPair)) {
          pairQueue.add(nextPair);
          visitedPairs.add(nextPair);
        }
      }
    }

    List<String> outputVariablesAsList = new ArrayList<>();
    outputVariablesAsList.addAll(outputVariables);
    Collections.sort(outputVariablesAsList);

    List<String> inputVariablesAsList = new ArrayList<>();
    inputVariablesAsList.addAll(inputVariables);
    Collections.sort(inputVariablesAsList);

    return new CustomInstruction(ciStartNode, ciEndNodes, inputVariablesAsList, outputVariablesAsList, shutdownNotifier);
  }

  private void addNewInputVariables(CFAEdge pLeavingEdge, Set<String> pPredOutputVars, Set<String> pInputVariables) {
    for(String var : getPotentialInputVariables(pLeavingEdge)) {
      if(!pPredOutputVars.contains(var)) {
        pInputVariables.add(var);
      }
    }
  }

  private Collection<String> getPotentialInputVariables(CFAEdge pLeavingEdge) {
    if (pLeavingEdge.getEdgeType() == CFAEdgeType.StatementEdge) {

      if (pLeavingEdge instanceof CStatementEdge) {
        CStatement edgeStmt = ((CStatementEdge) pLeavingEdge).getStatement();

        if (edgeStmt instanceof CExpressionAssignmentStatement) {
          return CIdExpressionCollectorVisitor.getVariablesOfExpression(((CExpressionAssignmentStatement) edgeStmt)
              .getRightHandSide());
        }

        else if (edgeStmt instanceof CExpressionStatement) {
          return CIdExpressionCollectorVisitor.getVariablesOfExpression(((CExpressionStatement) edgeStmt)
              .getExpression());
        }

        else if (edgeStmt instanceof CFunctionCallStatement) {
          Set<String> edgeInputVariables = new HashSet<>();
          for (CExpression exp : ((CFunctionCallStatement) edgeStmt).getFunctionCallExpression()
              .getParameterExpressions()) {
            edgeInputVariables.addAll(CIdExpressionCollectorVisitor.getVariablesOfExpression(exp));
          }
          return edgeInputVariables;
        }
        else if (edgeStmt instanceof CFunctionCallAssignmentStatement) {
          return CIdExpressionCollectorVisitor.getVariablesOfExpression(((CFunctionCallAssignmentStatement) edgeStmt)
                  .getRightHandSide().getFunctionNameExpression());
        }
      }
    }
      /**
       * TODO: für alle Kantentypen: Welche noch?!
       * - CDeclarationEdge + CDeclaration CVariableDeclaration -> all vars in initializer  become potential input vars
       * - CReturnStatementEdge: if getExpression isPresent -> all vars in expression  become potential input vars
       * - CAssumeEdge: all vars in expression  become potential input vars
       * - CFunctionCallEdge: all vars in getArgumentExpressions become potential input vars
       */
    // else
    return Collections.emptySet();
  }



  private Set<String> getOutputVariablesForSuccessorAndAddNewOutputVariables(CFAEdge pLeavingEdge,
      Set<String> pPredOutputVars, Set<String> pOutputVariables) {
    Set<String> edgeOutputVariables = null;
    if (pLeavingEdge instanceof CStatementEdge) {
      CStatement edgeStmt = ((CStatementEdge) pLeavingEdge).getStatement();
      if (edgeStmt instanceof CExpressionAssignmentStatement) {
        edgeOutputVariables =
            CIdExpressionCollectorVisitor.getVariablesOfExpression(((CExpressionAssignmentStatement) edgeStmt)
                .getLeftHandSide());
      }
      else if (edgeStmt instanceof CFunctionCallAssignmentStatement) {
        edgeOutputVariables =
            CIdExpressionCollectorVisitor.getVariablesOfExpression(((CFunctionCallAssignmentStatement) edgeStmt)
                .getLeftHandSide());
      } else {
        return pPredOutputVars;
      }
    } else if (pLeavingEdge instanceof CDeclarationEdge) {
      // TODO if pLeavingedge  CDeclarationEdge --> getQualifiedVariablename --> edgeOutputVariables variable
    } else {
      return pPredOutputVars;
    }

    pOutputVariables.addAll(edgeOutputVariables);
    HashSet<String> returnRes = new HashSet<>(pPredOutputVars);
    returnRes.addAll(edgeOutputVariables);

    return returnRes;
  }

}