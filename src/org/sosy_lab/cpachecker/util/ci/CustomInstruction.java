/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;

// Note that this class is not complete yet. Most of the comments are just for me and my advisor, they will disappear later!
public class CustomInstruction{

  private final CFANode ciStartNode;
  private final Collection<CFANode> ciEndNodes;
  private final ArrayList<String> inputVariables;
  private final ArrayList<String> outputVariables;
  private final ShutdownNotifier shutdownNotifier;


  /**
   * Constructor of CustomInstruction.
   * Note that the input-/output variables have to be sorted alphabetically!
   * @param pCIStartNode CFANode
   * @param pCIEndNodes Collection of CFANode
   * @param pInputVariables ArrayList of String, represents the input variables
   * @param pOutputVariables ArrayList of String, represents the outputvariables
   * @param pShutdownNotifier ShutdownNotifier
   */
  public CustomInstruction(final CFANode pCIStartNode, final Collection<CFANode> pCIEndNodes,
      final ArrayList<String> pInputVariables, final ArrayList<String> pOutputVariables, final ShutdownNotifier pShutdownNotifier) {

      ciStartNode = pCIStartNode;
      ciEndNodes = pCIEndNodes;
      inputVariables = pInputVariables;
      outputVariables = pOutputVariables;
      shutdownNotifier = pShutdownNotifier;
  }

  /**
   * Returns the signature of the input and output variables,
   * this is a String containing all input and output variables.
   * @return String like (IV1, IV2, ... IVn -> OV1, OV2, ..., OVm)
   */
  public String getSignature() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");

    for (String variable : inputVariables) {
      sb.append(variable);
      if (!variable.equals(inputVariables.get(inputVariables.size()-1))) {
        sb.append(", ");
      }
    }

    sb.append(" -> ");

    for (String variable : outputVariables) {
      sb.append(variable);
      if (!variable.equals(outputVariables.get(outputVariables.size()-1))) {
        sb.append(", ");
      }
    }

    sb.append(")");
    return sb.toString();
  }

  /**
   * Returns the (fake!) SMT description which is a
   * conjunctions of output variables and predicates (IVj = 0) for each input variable j.
   * Note that this is prefix notation!
   * @return (and (= IV1 0) (and (= IV2 0) (and OV1 OV2)))
   */
  public String getFakeSMTDescription() {
    StringBuilder sb = new StringBuilder();
    int BracketCounter = 0;

    if (inputVariables.size() != 0) {
      String last = inputVariables.get(inputVariables.size()-1);
      for (String variable : inputVariables) {
        if (outputVariables.size()==0 && variable.equals(last)) {
          sb.append("(= ");
          sb.append(variable);
          sb.append(" 0)");
        } else {
          sb.append("(and (= ");
          sb.append(variable);
          sb.append(" 0)");
          BracketCounter++;
        }
      }
    }

    if (outputVariables.size() != 0) {

      if (outputVariables.size() == 1) {
        sb.append(" ");
        sb.append(outputVariables.get(0));

      } else {
        String lastButOne = outputVariables.get(outputVariables.size()-2);
        for (String variable : outputVariables) {

          if (variable.equals(lastButOne)) {
            sb.append("(and ");
            sb.append(variable);
            sb.append(" ");
            sb.append(outputVariables.get(outputVariables.size()-1));
            sb.append(")");
            break;

          } else {
            sb.append("(and ");
            sb.append(variable);
            BracketCounter++;
          }
        }

      }
    }

    for (int i=0; i<BracketCounter; i++) {
      sb.append(")");
    }
    return sb.toString();
  }


  /**
   * TODO
   * @param startNode
   * @return the resulting AppliedCustomInstruction
   * @throws InterruptedException due to the shutdownNotifier
   * @throws AppliedCustomInstructionParsingFailedException if the matching of the variables of ci and aci
   * is not clear, or their structure dosen't fit.
   */
  public AppliedCustomInstruction inspectAppliedCustomInstruction(CFANode startNode)
        throws InterruptedException, AppliedCustomInstructionParsingFailedException {
    HashMap<String, String> mapping = new HashMap<>();
    HashMap<String, String> swappedMapping = new HashMap<>(); // same HM as mapping, but key/value are swapped
    Collection<String> outVariables = new ArrayList<>();


    Set<CFANode> aciEndNodes = new HashSet<>();
    Set<CFANode> ciVisitedNodes = new HashSet<>();
    Set<CFANode> aciVisitedNodes = new HashSet<>();
    Queue<CFANode> ciQueue = new ArrayDeque<>();
    Queue<CFANode> aciQueue = new ArrayDeque<>();

    ciQueue.add(ciStartNode);
    ciVisitedNodes.add(ciStartNode);
    aciQueue.add(startNode);
    aciVisitedNodes.add(startNode);

    CFANode ciPred;
    CFANode aciPred;

    while (!ciQueue.isEmpty() && !ciEndNodes.equals(aciEndNodes)) {
      shutdownNotifier.shutdownIfNecessary();

      ciPred = ciQueue.poll();
      aciPred = aciQueue.poll();

      if (ciEndNodes.contains(ciPred)) {
        aciEndNodes.add(aciPred);
        continue;
      }

      for (int i=0; i<ciPred.getNumLeavingEdges(); i++) {
        // Custom Instruction
        CFAEdge ciEdge = ciPred.getLeavingEdge(i);
        CFANode ciSucc = ciEdge.getSuccessor();

        // Applied Custom Instruction
        CFAEdge aciEdge = aciPred.getLeavingEdge(i);
        CFANode aciSucc = aciEdge.getSuccessor();

        // pair.first is the variable of CI, pair.last is the variable of ACI, never the other way around!
        HashMap<String,String> currentLine = new HashMap<>();
        computeMappingOfCIandACI(ciEdge, aciEdge, mapping, swappedMapping, currentLine, outVariables);

        if (currentLine.isEmpty()) {
          throw new AppliedCustomInstructionParsingFailedException("");// TODO
        }

//        if (pair != null && !pair.getFirst().equals("") && !pair.getSecond().equals("")) {
//          if (map.containsKey(pair.getFirst())) {
//
//            // mapping is not clear: two aci variable should be mapped to the ci variable
//            if (!map.get(pair.getFirst()).equals(pair.getSecond())) {
//              throw new AppliedCustomInstructionParsingFailedException("The mapping is not clear. The ci variable "+ pair.getFirst() + " is mapped to the variable " +  map.get(pair.getFirst()) + ", but now the variable" + pair.getSecond() + " should also be mapped to this ci variable.");
//            }
//
//            // mapping is not clear: two ci variables should be mapped to the aci varialbe
//            else if (!mapSwapped.get(pair.getSecond()).equals(pair.getFirst())){
//              throw new AppliedCustomInstructionParsingFailedException("The mapping is not clear. The aci variable "+ pair.getSecond() + " is mapped to the variable " +  mapSwapped.get(pair.getSecond()) + ", but now the variable" + pair.getFirst() + " should also be mapped to this aci variable.");
//            }
//
//            // everything is fine: map doesn't contain one of these values
//            else {
//              map.put(pair.getFirst(), pair.getSecond());
//              mapSwapped.put(pair.getSecond(), pair.getFirst());
//            }

            // breadth-first-search
            if (!ciVisitedNodes.contains(ciSucc) && !aciVisitedNodes.contains(aciSucc)){
              ciQueue.add(ciSucc);
              ciVisitedNodes.add(ciSucc);
              aciQueue.add(ciSucc);
              aciVisitedNodes.add(ciSucc);
            }
//          }

          // structure doesn't fit
//          else {
//            throw new AppliedCustomInstructionParsingFailedException("The structure of the ci and aci doesn't fit.\nCI:  " + pair.getFirst() + "\nACI: " + pair.getSecond());
//          }
//        }
      }
    }

    String fakeSMT = getFakeSMTDescriptionForACI(mapping);
    //TODO SSAMap

    return new AppliedCustomInstruction(startNode, aciEndNodes);// getFakeSMTDescription(), null, pIndicesForReturnVars); // TODO indices
  }

  /**
   * Returns the (fake!) SMT description of the before mapped variables of aci, which is a
   * conjunctions of output variables and predicates (IVj = 0) for each input variable j.
   * Note that this is prefix notation, and that the variables of aci are used instead of
   * those from the ci!
   * @return (and (= IV1 0) (and (= IV2 0) (and OV1 OV2)))
   */
  private String getFakeSMTDescriptionForACI(HashMap<String,String> map) {
    StringBuilder sb = new StringBuilder();
    int BracketCounter = 0;

    if (inputVariables.size() != 0) {
      String last = inputVariables.get(inputVariables.size()-1);
      for (String variable : inputVariables) {
        if (outputVariables.size()==0 && variable.equals(last)) {
          sb.append("(= ");
          sb.append(map.get(variable));
          sb.append(" 0)");
        } else {
          sb.append("(and (= ");
          sb.append(map.get(variable));
          sb.append(" 0)");
          BracketCounter++;
        }
      }
    }

    if (outputVariables.size() != 0) {

      if (outputVariables.size() == 1) {
        sb.append(" ");
        sb.append(outputVariables.get(0));

      } else {
        String lastButOne = outputVariables.get(outputVariables.size()-2);
        for (String variable : outputVariables) {

          if (variable.equals(map.get(lastButOne))) {
            sb.append("(and ");
            sb.append(map.get(variable));
            sb.append("@1 ");
            sb.append(outputVariables.get(outputVariables.size()-1));
            sb.append(")");
            break;

          } else {
            sb.append("(and ");
            sb.append(map.get(variable));
            sb.append("@1");
            BracketCounter++;
          }
        }

      }
    }

    for (int i=0; i<BracketCounter; i++) {
      sb.append(")");
    }
    return sb.toString();
  }


  /**
   * TODO
   * @param ciEdge CFAEdge
   * @param aciEdge CFAEdge
   */
  private void computeMappingOfCIandACI(CFAEdge ciEdge, CFAEdge aciEdge,
          HashMap<String, String> map, HashMap<String, String> swappedMap, HashMap<String,String> currentLine,
          Collection<String> outVariables)
          throws AppliedCustomInstructionParsingFailedException{

    if (ciEdge.getEdgeType() != aciEdge.getEdgeType()
        || !ciEdge.getPredecessor().equals(aciEdge.getPredecessor())
        || !ciEdge.getSuccessor().equals(aciEdge.getSuccessor())) {
      throw new AppliedCustomInstructionParsingFailedException("The edgeType, pre- or successor of " + ciEdge + " and " + aciEdge + " are different.");
    }

    switch(ciEdge.getEdgeType()) {
      case BlankEdge:
        compareBlankEdge((BlankEdge) ciEdge, (BlankEdge) aciEdge, map, swappedMap, currentLine, outVariables);
        break;
      case AssumeEdge:
        compareAssumeEdge((CAssumeEdge) ciEdge, (CAssumeEdge) aciEdge, map, swappedMap, currentLine, outVariables);
        break;
      case StatementEdge:
        compareStatementEdge((CStatementEdge) ciEdge, (CStatementEdge) aciEdge, map, swappedMap, currentLine, outVariables);
        break;
      case DeclarationEdge:
        compareDeclarationEdge((CDeclarationEdge) ciEdge, (CDeclarationEdge) aciEdge, map, swappedMap, currentLine, outVariables);
        break;
      case ReturnStatementEdge:
        compareReturnStatementEdge((CReturnStatementEdge) ciEdge, (CReturnStatementEdge) aciEdge, map, swappedMap, currentLine, outVariables);
        break;
      case FunctionCallEdge:
        compareFunctionCallEdge((CFunctionCallEdge)ciEdge, (CFunctionCallEdge)aciEdge, map, swappedMap, currentLine, outVariables);
        break;
      case FunctionReturnEdge:
        compareFunctionReturnEdge((CFunctionReturnEdge) ciEdge, (CFunctionReturnEdge) aciEdge, map, swappedMap, currentLine, outVariables);
        break;
      case MultiEdge:
        compareMultiEdge((MultiEdge) ciEdge, (MultiEdge) aciEdge, map, swappedMap, currentLine, outVariables);
        break;
      case CallToReturnEdge:
        compareCallToReturnEdge(ciEdge, aciEdge, map, swappedMap, currentLine, outVariables);
        break;
    }
  }

  private void compareBlankEdge(BlankEdge ciEdge, BlankEdge aciEdge, HashMap<String,String> map, HashMap<String,String> swappedMap,
      HashMap<String,String> currentLine, Collection<String> outVariables) {
    // no additional check needed.
  }

  private void compareAssumeEdge(CAssumeEdge ciEdge, CAssumeEdge aciEdge, HashMap<String,String> map, HashMap<String,String> swappedMap,
      HashMap<String,String> currentLine, Collection<String> outVariables) throws AppliedCustomInstructionParsingFailedException{
    if (ciEdge.getTruthAssumption() != aciEdge.getTruthAssumption()) {
      throw new AppliedCustomInstructionParsingFailedException("The truthAssumption of the CAssumeEdges " + ciEdge + " and " + aciEdge + "are different!");
    }
    ciEdge.getExpression().accept(new StructureComparisonVisitor(aciEdge.getExpression(), map, swappedMap, currentLine));
  }

  private void compareStatementEdge(CStatementEdge ciEdge, CStatementEdge aciEdge, HashMap<String,String> map, HashMap<String,String> swappedMap,
      HashMap<String,String> currentLine, Collection<String> outVariables) throws AppliedCustomInstructionParsingFailedException {

    if (ciEdge.getStatement() instanceof CFunctionSummaryStatementEdge && aciEdge.getStatement() instanceof CFunctionSummaryStatementEdge) {
      CFunctionSummaryStatementEdge ciStmt = (CFunctionSummaryStatementEdge) ciEdge.getStatement();
      CFunctionSummaryStatementEdge aciStmt = (CFunctionSummaryStatementEdge) aciEdge.getStatement();

      if (!ciStmt.getFunctionName().equals(aciStmt.getFunctionName())){
        throw new AppliedCustomInstructionParsingFailedException("The functionName of the CFunctionSummaryStatementEdges " + ciEdge + " and " + aciEdge + " are different!");
      }

      compareStatementsOfStatementEdge(ciStmt.getFunctionCall(), aciStmt.getFunctionCall(), map, swappedMap, currentLine, outVariables);

    } else {
      compareStatementsOfStatementEdge(ciEdge.getStatement(), aciEdge.getStatement(), map, swappedMap, currentLine, outVariables);
    }
  }

  private void compareStatementsOfStatementEdge(CStatement ci, CStatement aci,
      HashMap<String, String> map, HashMap<String, String> swappedMap, HashMap<String, String> currentLine,
      Collection<String> outVariables) throws AppliedCustomInstructionParsingFailedException {
    if (ci instanceof CExpressionAssignmentStatement && aci instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement ciStmt = (CExpressionAssignmentStatement) ci;
      CExpressionAssignmentStatement aciStmt = (CExpressionAssignmentStatement) aci;

      // left side => output variables
      ciStmt.getLeftHandSide().accept(new StructureComparisonVisitor(aciStmt.getLeftHandSide(), map, swappedMap, currentLine));
      outVariables.addAll(currentLine.keySet());

      // right side: just proof it
      ciStmt.getRightHandSide().accept(new StructureComparisonVisitor(aciStmt.getRightHandSide(), map, swappedMap, currentLine));
    }

    else if (ci instanceof CExpressionStatement && aci instanceof CExpressionStatement) {
      CExpressionStatement ciStmt = (CExpressionStatement) ci;
      CExpressionStatement aciStmt = (CExpressionStatement) aci;
      ciStmt.getExpression().accept(new StructureComparisonVisitor(aciStmt.getExpression(), map, swappedMap, currentLine));
    }

    else if (ci instanceof CFunctionCallAssignmentStatement && aci instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement ciStmt = (CFunctionCallAssignmentStatement) ci;
      CFunctionCallAssignmentStatement aciStmt = (CFunctionCallAssignmentStatement) aci;

      if (!ciStmt.getFunctionCallExpression().getExpressionType().equals(aciStmt.getFunctionCallExpression().getExpressionType())){
        throw new AppliedCustomInstructionParsingFailedException("The expressionType of the CStatementEdges " + ci + " and " + aci + " are different!");
      }

      ciStmt.getFunctionCallExpression().getFunctionNameExpression().accept(new StructureComparisonVisitor(aciStmt.getFunctionCallExpression().getFunctionNameExpression(), map, swappedMap, currentLine));

      List<CExpression> ciList = ciStmt.getFunctionCallExpression().getParameterExpressions();
      List<CExpression> aciList = aciStmt.getFunctionCallExpression().getParameterExpressions();
      // TODO haben die Elemente der Listen die selbe Reihenfolge?
      for (int i=0; i<ciList.size(); i++) {
        ciList.get(i).accept(new StructureComparisonVisitor(aciList.get(i), map, swappedMap, currentLine));
      }

      // left side => output variables
      ciStmt.getLeftHandSide().accept(new StructureComparisonVisitor(aciStmt.getLeftHandSide(), map, swappedMap, currentLine));
      outVariables.addAll(map.keySet());
    }

    else if (ci instanceof CFunctionCallStatement && aci instanceof CFunctionCallStatement) {
      CFunctionCallStatement ciStmt = (CFunctionCallStatement) ci;
      CFunctionCallStatement aciStmt = (CFunctionCallStatement) aci;

      if (!ciStmt.getFunctionCallExpression().getExpressionType().equals(aciStmt.getFunctionCallExpression().getExpressionType())){
        throw new AppliedCustomInstructionParsingFailedException("The expressionType of the CStatementEdges " + ci + " and " + aci + " are different!");
      }

      ciStmt.getFunctionCallExpression().getFunctionNameExpression().accept(new StructureComparisonVisitor(aciStmt.getFunctionCallExpression().getFunctionNameExpression(), map, swappedMap, currentLine));

      List<CExpression> ciList = ciStmt.getFunctionCallExpression().getParameterExpressions();
      List<CExpression> aciList = aciStmt.getFunctionCallExpression().getParameterExpressions();
      // TODO haben die Elemente der Listen die selbe Reihenfolge?
      for (int i=0; i<ciList.size(); i++) {
        ciList.get(i).accept(new StructureComparisonVisitor(aciList.get(i), map, swappedMap, currentLine));
      }
    }
  }

  private void compareDeclarationEdge(CDeclarationEdge ciEdge, CDeclarationEdge aciEdge, HashMap<String,String> map, HashMap<String,String> swappedMap,
      HashMap<String,String> currentLine, Collection<String> outVariables) throws AppliedCustomInstructionParsingFailedException {

    CDeclaration ciDec = ciEdge.getDeclaration();
    CDeclaration aciDec = aciEdge.getDeclaration();

    if (!ciEdge.getDeclaration().getClass().equals(aciEdge.getDeclaration().getClass())) {
      throw new AppliedCustomInstructionParsingFailedException("The CDeclarationEdges " + ciEdge + " and " + aciEdge + " have different declarations!");
    }

    if (ciDec instanceof CVariableDeclaration) {
      if (!ciDec.getType().equals(aciDec.getType())) {
        throw new AppliedCustomInstructionParsingFailedException("The CVariableDeclaration " + ciEdge + " and " + aciEdge + " have different declaration types!");
      }
      if (!ciDec.getQualifiedName().equals(aciDec.getQualifiedName())) {
        throw new AppliedCustomInstructionParsingFailedException("The CVariableDeclaration " + ciEdge + " and " + aciEdge + " have different qualified names!");
      }

    }
  }

  private void compareReturnStatementEdge(CReturnStatementEdge ciEdge, CReturnStatementEdge aciEdge, HashMap<String,String> map, HashMap<String,String> swappedMap,
      HashMap<String,String> currentLine, Collection<String> outVariables) throws AppliedCustomInstructionParsingFailedException {

    if (!ciEdge.getExpression().isPresent() || !aciEdge.getExpression().isPresent()){
      throw new AppliedCustomInstructionParsingFailedException(""); // TODO
    }
    ciEdge.getExpression().get().accept(new StructureComparisonVisitor(aciEdge.getExpression().get(), map, swappedMap, currentLine));
  }

  private void compareFunctionCallEdge(CFunctionCallEdge ciEdge, CFunctionCallEdge aciEdge, HashMap<String,String> map, HashMap<String,String> swappedMap,
      HashMap<String,String> currentLine, Collection<String> outVariables) throws AppliedCustomInstructionParsingFailedException {

    List<CExpression> ciArguments = ciEdge.getArguments();
    List<CExpression> aciArguments = aciEdge.getArguments();
    if (ciArguments.size() != aciArguments.size()) {
      throw new AppliedCustomInstructionParsingFailedException("The amount of arguments of the FunctionCallEdges " + ciEdge + " and " + aciEdge + " are different!");
    }
    for (int i=0; i<ciArguments.size(); i++) {
      ciArguments.get(i).accept(new StructureComparisonVisitor(aciArguments.get(i), map, swappedMap, currentLine));
    }
  }

  private void compareFunctionReturnEdge(CFunctionReturnEdge ciEdge, CFunctionReturnEdge aciEdge, HashMap<String,String> map, HashMap<String,String> swappedMap,
      HashMap<String,String> currentLine, Collection<String> outVariables) {
    // no additional check needed.
  }

  private void compareMultiEdge(MultiEdge ciEdge, MultiEdge aciEdge, HashMap<String,String> map, HashMap<String,String> swappedMap,
      HashMap<String,String> currentLine, Collection<String> outVariables) {
  }

  private void compareCallToReturnEdge(CFAEdge ciEdge, CFAEdge aciEdge, HashMap<String,String> map, HashMap<String,String> swappedMap,
      HashMap<String,String> currentLine, Collection<String> outVariables) {
  }


  private class StructureComparisonVisitor implements CExpressionVisitor<String, AppliedCustomInstructionParsingFailedException>{

    CExpression aciExp;
    HashMap<String,String> mapping;
    HashMap<String,String> swappedMapping;
    HashMap<String,String> currentLine;

    public StructureComparisonVisitor(CExpression pAciExp, HashMap<String,String> pMapping, HashMap<String,String> pSwappedMapping, HashMap<String,String> pCurrentLine) {
      aciExp = pAciExp;
      this.mapping = pMapping;
      this.swappedMapping = pSwappedMapping;
      this.currentLine = pCurrentLine;
    }

    @Override
    public String visit(CArraySubscriptExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      CArraySubscriptExpression aciAExp = (CArraySubscriptExpression) aciExp;
      if (!ciExp.equals(aciAExp)) {
        throw new AppliedCustomInstructionParsingFailedException(""); // TODO
      }
      ciExp.getArrayExpression().accept(new StructureComparisonVisitor(aciAExp.getArrayExpression(), mapping, swappedMapping, currentLine));
      ciExp.getSubscriptExpression().accept(new StructureComparisonVisitor(aciAExp.getSubscriptExpression(), mapping, swappedMapping, currentLine));
      return null;
    }

    @Override
    public String visit(CFieldReference ciExp) throws AppliedCustomInstructionParsingFailedException {
      CFieldReference aciFieldRefExp = (CFieldReference) aciExp;
      if (!ciExp.getExpressionType().equals(aciFieldRefExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the FieldReference of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciFieldRefExp + " (" + aciFieldRefExp.getExpressionType() + ").");
      }
      return ciExp.getFieldName();
    }

    @Override
    public String visit(CIdExpression ciExp) throws AppliedCustomInstructionParsingFailedException {
      CIdExpression aciIdExp = (CIdExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciIdExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the IdExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciIdExp + " (" + aciIdExp.getExpressionType() + ").");
      }
      return ciExp.getName();
    }

    @Override
    public String visit(CPointerExpression ciExp) throws AppliedCustomInstructionParsingFailedException {
      CPointerExpression aciPExp = (CPointerExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciPExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CPointerExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciPExp + " (" + aciPExp.getExpressionType() + ").");
      }
      ciExp.getOperand().accept(new StructureComparisonVisitor(aciPExp.getOperand(), mapping, swappedMapping, currentLine));
      return null;
    }

    @Override
    public String visit(CComplexCastExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      CComplexCastExpression aciCExp = (CComplexCastExpression) aciExp;
      if (!ciExp.equals(aciCExp)) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CComplexCastExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciCExp + " (" + aciCExp.getExpressionType() + ").");
      }
      ciExp.getOperand().accept(new StructureComparisonVisitor(aciCExp.getOperand(), mapping, swappedMapping, currentLine));
      return null;
    }

    @Override
    public String visit(CBinaryExpression ciBinExp) throws AppliedCustomInstructionParsingFailedException {

      if (aciExp instanceof CBinaryExpression) {
        CBinaryExpression aciBinExp = (CBinaryExpression) aciExp;

        // expression types are different
        if (!ciBinExp.getExpressionType().equals(aciBinExp.getExpressionType())) {
          throw new AppliedCustomInstructionParsingFailedException("The expression type of the BinaryExpression of ci " + ciBinExp + " (" + ciBinExp.getExpressionType() + ") is not equal to the one of the aci " + aciBinExp + " (" + aciBinExp.getExpressionType() + ").");
        }

        String ciOp = ciBinExp.getOperator().getOperator();
        String aciOp = aciBinExp.getOperator().getOperator();

        // operators are different
        if (!ciOp.equals(aciOp)) {
          throw new AppliedCustomInstructionParsingFailedException("The structure doesn't fit. The operators of the ci expression " + ciBinExp  + " and aci expression " + aciBinExp + " don't fit together!");
        }

        if (ciBinExp.getOperand1() instanceof CIdExpression
            && ciBinExp.getOperand2() instanceof CIdExpression
            && ((aciBinExp.getOperand1() instanceof CIdExpression)
                || (aciBinExp.getOperand1() instanceof CIntegerLiteralExpression)
                || (aciBinExp.getOperand1() instanceof CFloatLiteralExpression)
                || (aciBinExp.getOperand1() instanceof CCharLiteralExpression)
                || (aciBinExp.getOperand1() instanceof CStringLiteralExpression)
                || (aciBinExp.getOperand1() instanceof CFieldReference))
            && ((aciBinExp.getOperand2() instanceof CIdExpression)
                || (aciBinExp.getOperand2() instanceof CIntegerLiteralExpression)
                || (aciBinExp.getOperand2() instanceof CFloatLiteralExpression)
                || (aciBinExp.getOperand2() instanceof CCharLiteralExpression)
                || (aciBinExp.getOperand2() instanceof CStringLiteralExpression)
                || (aciBinExp.getOperand2() instanceof CFieldReference))) {



          // left side
          String ciLeft = ciBinExp.getOperand1().accept(new StructureComparisonVisitor(aciBinExp.getOperand1(), mapping, swappedMapping, currentLine));
          String aciLeft = aciBinExp.getOperand1().accept(new StructureComparisonVisitor(ciBinExp.getOperand1(), mapping, swappedMapping, currentLine));

          // mapping is not clear
          if (mapping.containsKey(ciLeft) && !mapping.get(ciLeft).equals(aciLeft)) {
              throw new AppliedCustomInstructionParsingFailedException("The mapping is not clear. The ci variable "+ ciLeft + " is mapped to the variable " +  mapping.get(ciLeft) + ", but now the variable" + aciLeft + " should also be mapped to this ci variable.");
          } else if (swappedMapping.containsKey(aciLeft) && !swappedMapping.get(aciLeft).equals(ciLeft)) {
            throw new AppliedCustomInstructionParsingFailedException("The mapping is not clear. The aci variable "+ aciLeft + " is mapped to the variable " +  swappedMapping.get(aciLeft) + ", but now the variable" + ciLeft + " should also be mapped to this aci variable.");
          }
          // everything is fine with this part: mapping is clear
          else {
            mapping.put(ciLeft, aciLeft);
            swappedMapping.put(aciLeft, ciLeft);
            currentLine.put(ciLeft, aciLeft);
          }


          // right side
          String ciRight = ciBinExp.getOperand2().accept(new StructureComparisonVisitor(aciBinExp.getOperand2(), mapping, swappedMapping, currentLine));
          String aciRight = aciBinExp.getOperand2().accept(new StructureComparisonVisitor(ciBinExp.getOperand2(), mapping, swappedMapping, currentLine));

          // mapping is not clear
          if (mapping.containsKey(ciRight) && !mapping.get(ciRight).equals(aciRight)) {
            throw new AppliedCustomInstructionParsingFailedException("The mapping is not clear. The ci variable "+ ciRight + " is mapped to the variable " +  mapping.get(ciRight) + ", but now the variable" + ciRight + " should also be mapped to this ci variable.");
          } else if (swappedMapping.containsKey(aciRight) && !swappedMapping.get(aciRight).equals(ciRight)) {
            throw new AppliedCustomInstructionParsingFailedException("The mapping is not clear. The aci variable "+ aciRight + " is mapped to the variable " +  swappedMapping.get(aciRight) + ", but now the variable" + ciRight + " should also be mapped to this aci variable.");
          }
          // everything is fine with this part: mapping is clear
          else {
            mapping.put(ciRight, aciRight);
            swappedMapping.put(aciRight, ciRight);
          }
        }

      } else {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not a BinaryExpression!");
      }

      return null;
    }

    @Override
    public String visit(CCastExpression ciExp) throws AppliedCustomInstructionParsingFailedException {
      CCastExpression aciPExp = (CCastExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciPExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CCastExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciPExp + " (" + aciPExp.getExpressionType() + ").");
      }
      ciExp.getOperand().accept(new StructureComparisonVisitor(aciPExp.getOperand(), mapping, swappedMapping, currentLine));
      return null;
    }

    @Override
    public String visit(CCharLiteralExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      CCharLiteralExpression aciCharExp = (CCharLiteralExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciCharExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CharLiteralExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciCharExp + " (" + aciCharExp.getExpressionType() + ").");
      }
      return ciExp.getCharacter() + "";
    }

    @Override
    public String visit(CFloatLiteralExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      CFloatLiteralExpression aciFloatExp = (CFloatLiteralExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciFloatExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the FloatLiteralExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciFloatExp + " (" + aciFloatExp.getExpressionType() + ").");
      }
      return ciExp.getValue().toString();
    }

    @Override
    public String visit(CIntegerLiteralExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      CIntegerLiteralExpression aciIntegerLiteralExp = (CIntegerLiteralExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciIntegerLiteralExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the IntegerLiteralExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciIntegerLiteralExp + " (" + aciIntegerLiteralExp.getExpressionType() + ").");
      }
      return ciExp.getValue().toString();
    }

    @Override
    public String visit(CStringLiteralExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      CStringLiteralExpression aciStringLiteralExp = (CStringLiteralExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciStringLiteralExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the StringLiteralExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciStringLiteralExp + " (" + aciStringLiteralExp.getExpressionType() + ").");
      }
      return ciExp.getContentString();
    }

    @Override
    public String visit(CTypeIdExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      CTypeIdExpression aciIdExp = (CTypeIdExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciIdExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CTypeIdExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciIdExp + " (" + aciIdExp.getExpressionType() + ").");
      }
      return ciExp.getType().toString();
    }

    @Override
    public String visit(CUnaryExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {

      if (aciExp instanceof CUnaryExpression) {
        CUnaryExpression aciUnExp = (CUnaryExpression) aciExp;

        // expression types are different
        if (!ciExp.getExpressionType().equals(aciUnExp.getExpressionType())) {
          throw new AppliedCustomInstructionParsingFailedException("The expression type of the CUnaryExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciUnExp + " (" + aciUnExp.getExpressionType() + ").");
        }

        String ciOp = ciExp.getOperator().getOperator();
        String aciOp = aciUnExp.getOperator().getOperator();

        // operators are different
        if (!ciOp.equals(aciOp)) {
          throw new AppliedCustomInstructionParsingFailedException("The structure doesn't fit. The operators of the ci expression " + ciExp  + " and aci expression " + aciUnExp + " don't fit together!");
        }

        // TODO was gilt alles als UnaryExp "EndExp" (Pointer, AddressOfLabel, Cast ?)
        if (ciExp.getOperand() instanceof CIdExpression
//            && ((aciUnExp.getOperand() instanceof CPointerExpression))
            ) {



          // Operand
          String ciOperand = ciExp.getOperand().accept(new StructureComparisonVisitor(aciUnExp.getOperand(), mapping, swappedMapping, currentLine));
          String aciOperand = aciUnExp.getOperand().accept(new StructureComparisonVisitor(ciExp.getOperand(), mapping, swappedMapping, currentLine));

          // mapping is not clear
          if (mapping.containsKey(ciOperand) && !mapping.get(ciOperand).equals(aciOperand)) {
              throw new AppliedCustomInstructionParsingFailedException("The mapping is not clear. The ci variable "+ ciOperand + " is mapped to the variable " +  mapping.get(ciOperand) + ", but now the variable" + aciOperand + " should also be mapped to this ci variable.");
          } else if (swappedMapping.containsKey(aciOperand) && !swappedMapping.get(aciOperand).equals(ciOperand)) {
            throw new AppliedCustomInstructionParsingFailedException("The mapping is not clear. The aci variable "+ aciOperand + " is mapped to the variable " +  swappedMapping.get(aciOperand) + ", but now the variable" + ciOperand + " should also be mapped to this aci variable.");
          }
          // everything is fine with this part: mapping is clear
          else {
            mapping.put(ciOperand, aciOperand);
            swappedMapping.put(aciOperand, ciOperand);
            currentLine.put(ciOperand, aciOperand);
          }
        }

      } else {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not an UnaryExpression!");
      }

      return null;
    }

    @Override
    public String visit(CImaginaryLiteralExpression PIastLiteralExpression)
        throws AppliedCustomInstructionParsingFailedException {
      // TODO was ist das?
      return null;
    }

    @Override
    public String visit(CAddressOfLabelExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      CAddressOfLabelExpression aciAExp = (CAddressOfLabelExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciAExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CAddressOfLabelExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciAExp + " (" + aciAExp.getExpressionType() + ").");
      }
      return ciExp.getLabelName();
    }

  }
}
