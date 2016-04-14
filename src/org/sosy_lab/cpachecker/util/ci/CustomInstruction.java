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

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

// Note that this class is not complete yet. Most of the comments are just for me and my advisor, they will disappear later!
public class CustomInstruction{

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((ciEndNodes == null) ? 0 : ciEndNodes.hashCode());
    result = prime * result + ((ciStartNode == null) ? 0 : ciStartNode.hashCode());
    result = prime * result + ((inputVariables == null) ? 0 : inputVariables.hashCode());
    result = prime * result + ((outputVariables == null) ? 0 : outputVariables.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    CustomInstruction other = (CustomInstruction) obj;
    if (ciEndNodes == null) {
      if (other.ciEndNodes != null) {
        return false;
      }
    } else if (!ciEndNodes.equals(other.ciEndNodes)) {
      return false;
    }
    if (ciStartNode == null) {
      if (other.ciStartNode != null) {
        return false;
      }
    } else if (!ciStartNode.equals(other.ciStartNode)) {
      return false;
    }
    if (inputVariables == null) {
      if (other.inputVariables != null) {
        return false;
      }
    } else if (!inputVariables.equals(other.inputVariables)) {
      return false;
    }
    if (outputVariables == null) {
      if (other.outputVariables != null) {
        return false;
      }
    } else if (!outputVariables.equals(other.outputVariables)) {
      return false;
    }
    return true;
  }

  private final CFANode ciStartNode;
  private final Collection<CFANode> ciEndNodes;
  private final List<String> inputVariables;
  private final List<String> outputVariables;
  private final ShutdownNotifier shutdownNotifier;


  /**
   * Constructor of CustomInstruction.
   * Note that the input-/output variables have to be sorted alphabetically!
   * @param pCIStartNode CFANode
   * @param pCIEndNodes Collection of CFANode
   * @param pInputVariables List of String, represents the input variables
   * @param pOutputVariables List of String, represents the outputvariables
   * @param pShutdownNotifier ShutdownNotifier
   */
  public CustomInstruction(final CFANode pCIStartNode, final Collection<CFANode> pCIEndNodes,
      final List<String> pInputVariables, final List<String> pOutputVariables, final ShutdownNotifier pShutdownNotifier) {

      ciStartNode = pCIStartNode;
      ciEndNodes = pCIEndNodes;
      inputVariables = pInputVariables;
      outputVariables = pOutputVariables;
      shutdownNotifier = pShutdownNotifier;
  }

  /**
   * Returns the signature of the input and output variables,
   * this is a String containing all input and output variables.
   * @return String like (|iV1|, |iV2|, ... |iVn| -> |oV1|, |oV2|, ..., |oVm|)
   */
  public String getSignature() {
    StringBuilder sb = new StringBuilder();

    sb.append("(");
    if (inputVariables.size() > 0) {
      Joiner.on(", ").appendTo(sb, Iterables.transform(inputVariables, CIUtils.GET_SMTNAME));
    }

    sb.append(") -> (");

    if (outputVariables.size() > 0) {
      Joiner.on(", ").appendTo(sb, Iterables.transform(outputVariables, CIUtils.GET_SMTNAME_WITH_INDEX));
    }
    sb.append(")");

    return sb.toString();
  }

  /**
   * Returns the (fake!) SMT description which is a
   * conjunctions of output variables and predicates (IVj = 0) for each input variable j.
   * Note that this is prefix notation!
   * @return (define-fun aci Bool((and (= IV1 0) (and (= IV2 0) (and OV1 OV2))))
   */
  public Pair<List<String>, String> getFakeSMTDescription() {
    if (inputVariables.size() == 0 && outputVariables.size() == 0) {
      return Pair.of(Collections.<String> emptyList(),
        "(define-fun ci() Bool true)");
    }
    StringBuilder sb = new StringBuilder();
    sb.append("(define-fun ci() Bool");
    int BracketCounter = 0;

    if (inputVariables.size() != 0) {
      String last = inputVariables.get(inputVariables.size()-1);
      for (int i=0; i<inputVariables.size(); i++) {
        String variable = inputVariables.get(i);
        if (outputVariables.size()==0 && variable.equals(last)) {
          sb.append(getAssignmentOfVariableToZero(variable, false));
//          sb.append("= ");
//          sb.append(variable);
//          sb.append(" 0)");
        } else {
          sb.append("(and ");
          sb.append(getAssignmentOfVariableToZero(variable, false));
          BracketCounter++;
        }
      }
    }

    if (outputVariables.size() != 0) {
      String last = outputVariables.get(outputVariables.size()-1);
      for (int i=0; i<outputVariables.size(); i++) {
        String variable = outputVariables.get(i);
        if (variable.equals(last)) {
          sb.append(" ");
          sb.append(getAssignmentOfVariableToZero(variable, true));
//          sb.append("= ");
//          sb.append(variable);
//          sb.append("@1 0)");
        } else {
          sb.append("(and ");
          sb.append(getAssignmentOfVariableToZero(variable, true));
          BracketCounter++;
        }
      }
    }

    for (int i=0; i<BracketCounter+1; i++) { // +1 because of the Bracket of define-fun ci Bool(...)
      sb.append(")");
    }

    List<String> outputVariableList = new ArrayList<>();
    for (String var : inputVariables) {
      try {
        Integer.parseInt(var);
      } catch (NumberFormatException e) {
        outputVariableList.add("(declare-fun " + CIUtils.getSMTName(var) + " () Int)");
      }
    }
    for (String var : outputVariables) {
      outputVariableList.add("(declare-fun " + CIUtils.getSMTName(var+"@1") + " () Int)");
    }
    return Pair.of(outputVariableList, sb.toString());
  }

  /**
   * Returns String of the given variable: if it is an outputVariable (= variable@1 0), otherwise (= variable 0)
   * @param var String of variable
   * @param isOutputVariable boolean if the variable is an output variable
   */
  private String getAssignmentOfVariableToZero(final String var, final boolean isOutputVariable) {
    StringBuilder sb = new StringBuilder();

    boolean isNumber = false;
    try {
      Integer.parseInt(var);
      isNumber = true;
    } catch (NumberFormatException ex) {
    }

    sb.append("(= ");
    if (!isNumber) {
      if(isOutputVariable) {
        sb.append(CIUtils.getSMTName(var+"@1"));
      } else {
        sb.append(CIUtils.getSMTName(var));
      }
    } else{
      sb.append(var);
    }

    sb.append(" 0)");
    return sb.toString();
  }

  /**
   * Returns AppliedCustomInstruction which begins at the given aciStartNode.
   * @param aciStartNode the starting node of the instruction to be returned
   * @return the resulting AppliedCustomInstruction
   * @throws InterruptedException due to the shutdownNotifier
   * @throws AppliedCustomInstructionParsingFailedException if the matching of the variables of ci and aci
   * is not clear, or their structure dosen't fit.
   */
  public AppliedCustomInstruction inspectAppliedCustomInstruction(final CFANode aciStartNode)
        throws InterruptedException, AppliedCustomInstructionParsingFailedException {
    Map<String, String> mapping = new HashMap<>();
    Set<String> outVariables = new HashSet<>();
    Set<CFANode> aciEndNodes = new HashSet<>();
    Set<Pair<CFANode, CFANode>> visitedNodes = new HashSet<>();
    Queue<Pair<CFANode, CFANode>> queue = new ArrayDeque<>();
    Pair<CFANode, CFANode> next;

    visitedNodes.add(Pair.of(ciStartNode, aciStartNode));
    queue.add(Pair.of(ciStartNode, aciStartNode));

    CFANode ciPred;
    CFANode aciPred;

    while (!queue.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      Pair<CFANode, CFANode> nextPair = queue.poll();
      ciPred = nextPair.getFirst();
      aciPred = nextPair.getSecond();

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

        computeMappingOfCiAndAci(ciEdge, aciEdge, mapping, outVariables);

        if (ciEdge instanceof FunctionCallEdge) {
          computeMappingOfCiAndAci(((FunctionCallEdge) ciEdge).getSummaryEdge(),
              ((FunctionCallEdge) aciEdge).getSummaryEdge(), mapping, outVariables);
          next = Pair.of(((FunctionCallEdge) ciEdge).getSummaryEdge().getSuccessor(),
              ((FunctionCallEdge) aciEdge).getSummaryEdge().getSuccessor());
        } else {
          next = Pair.of(ciSucc, aciSucc);
        }

        // breadth-first-search
        if (!visitedNodes.contains(next)) {
          queue.add(next);
          visitedNodes.add(next);
        }
      }
    }

    if (ciEndNodes.size() != aciEndNodes.size()) {
      throw new AppliedCustomInstructionParsingFailedException("The amout of endNodes of ci and aci are different!");
    }

    SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
    for (String var : outVariables) {
      ssaMapBuilder.setIndex(var,new CSimpleType(false, false, CBasicType.INT, false, false, false, false, false, false, false), 1);
    }

    List<String> inVars = getVariablesOrdered(mapping, inputVariables);
    List<String> outVars = getVariablesOrdered(mapping, outputVariables);
    List<String> inVarsConst = new ArrayList<>(inputVariables.size());

    for(String ciVar: inputVariables) {
      inVarsConst.add(mapping.get(ciVar));
    }

    return new AppliedCustomInstruction(aciStartNode, aciEndNodes, inVars, outVars, inVarsConst, getFakeSMTDescriptionForACI(mapping), ssaMapBuilder.build());
  }

  private List<String> getVariablesOrdered(final Map<String, String> pMapping,
      final List<String> pVariables) {
    List<String> result = new ArrayList<>(pVariables.size());

    String aciVar;
    for (String ciVar : pVariables) {
      assert (pMapping.containsKey(ciVar));
      aciVar = pMapping.get(ciVar);
      try {
        Integer.parseInt(aciVar);
      } catch (NumberFormatException ex) {
        result.add(aciVar);
      }
    }

    return result;
  }

  /**
   * Returns the (fake!) SMT description of the before mapped variables of aci, which is a
   * conjunctions of output variables and predicates (IVj = 0) for each input variable j.
   * Note that this is prefix notation, and that the variables of aci are used instead of
   * those from the ci!
   * @return (define-fun aci Bool((and (= IV1 0) (and (= IV2 0) (and OV1 OV2))))
   */
  private Pair<List<String>, String> getFakeSMTDescriptionForACI(final Map<String,String> map) {
    if (inputVariables.size() == 0 && outputVariables.size() == 0) {
      return Pair.of(Collections.<String> emptyList(),
        "(define-fun ci() Bool true)");
    }

    StringBuilder sb = new StringBuilder();
    sb.append("(define-fun ci() Bool");
    int BracketCounter = 0;

    if (inputVariables.size() != 0) {
      String last = inputVariables.get(inputVariables.size()-1);
      for (int i=0; i<inputVariables.size(); i++) {
        String variable = inputVariables.get(i);
        if (outputVariables.size()==0 && variable.equals(last)) {
          sb.append(getAssignmentOfVariableToZero(map.get(variable), false));
        } else {
          sb.append("(and ");
          sb.append(getAssignmentOfVariableToZero(map.get(variable), false));
          BracketCounter++;
        }
      }
    }

    if (outputVariables.size() != 0) {
      String last = outputVariables.get(outputVariables.size()-1);
      for (int i=0; i<outputVariables.size(); i++) {
        String variable = outputVariables.get(i);
        if (variable.equals(last)) {
          sb.append(" ");
          sb.append(getAssignmentOfVariableToZero(map.get(variable), true));
        } else {
          sb.append("(and ");
          sb.append(getAssignmentOfVariableToZero(map.get(variable), true));
          BracketCounter++;
        }
      }
    }

    for (int i=0; i<BracketCounter+1; i++) { // +1 because of the Bracket of (define-fun ci Bool...)
      sb.append(")");
    }

    List<String> outputVariableList = new ArrayList<>();
    for (String var : inputVariables) {
      try {
        Integer.parseInt(map.get(var));
      } catch (NumberFormatException e) {
        outputVariableList.add("(declare-fun " + CIUtils.getSMTName(map.get(var))+ " () Int)");
      }
    }
    for (String var : outputVariables) {
      outputVariableList.add("(declare-fun " + CIUtils.getSMTName(map.get(var)+"@1") + " () Int)");
    }

    return Pair.of(outputVariableList, sb.toString());
  }


  /**
   * Computes the mapping of variables of the given CI and ACI.
   * That means the structure of the ci is compared to the aci's structure.
   * All variables of the CI and ACI will be mapped, except those
   * which have different types. The latter ones will throw exceptions.
   * The mapping will be stored in the given Map ciVarToAciVar.
   * @param ciEdge CFAEdge of CustomInstruction (CI)
   * @param aciEdge CFAEdge of AppliedCustomInstruction (ACI)
   * @param ciVarToAciVar Map of variables of CI and ACI
   * @param outVariables Collection of output variables
   */
  private void computeMappingOfCiAndAci(final CFAEdge ciEdge, final CFAEdge aciEdge,
      final Map<String, String> ciVarToAciVar, final Collection<String> outVariables)
          throws AppliedCustomInstructionParsingFailedException{

    if (ciEdge.getEdgeType() != aciEdge.getEdgeType()) {
      throw new AppliedCustomInstructionParsingFailedException("The edgeType of " + ciEdge + " and " + aciEdge + " are different.");
    }

    switch(ciEdge.getEdgeType()) {
      case BlankEdge:
        // no additional check needed.
        return;
      case AssumeEdge:
        compareAssumeEdge((CAssumeEdge) ciEdge, (CAssumeEdge) aciEdge, ciVarToAciVar);
        return;
      case StatementEdge:
        compareStatementEdge((CStatementEdge) ciEdge, (CStatementEdge) aciEdge, ciVarToAciVar, outVariables);
        return;
      case DeclarationEdge:
        compareDeclarationEdge((CDeclarationEdge) ciEdge, (CDeclarationEdge) aciEdge, ciVarToAciVar, outVariables);
        return;
      case ReturnStatementEdge:
        compareReturnStatementEdge((CReturnStatementEdge) ciEdge, (CReturnStatementEdge) aciEdge, ciVarToAciVar);
        return;
      case FunctionCallEdge:
        compareFunctionCallEdge((CFunctionCallEdge)ciEdge, (CFunctionCallEdge)aciEdge, ciVarToAciVar);
        return;
      case FunctionReturnEdge:
        // no additional check needed.
        return;
      case CallToReturnEdge:
        compareStatementsOfStatementEdge(((CFunctionSummaryEdge) ciEdge).getExpression(), ((CFunctionSummaryEdge) aciEdge).getExpression(), ciVarToAciVar, outVariables);
        return;
      default:
        throw new AssertionError("Unhandeled enum value in switch: " + ciEdge.getEdgeType());
    }
  }

  private void compareAssumeEdge(final CAssumeEdge ciEdge, final CAssumeEdge aciEdge,
      final Map<String,String> ciVarToAciVar) throws AppliedCustomInstructionParsingFailedException {

    if (ciEdge.getTruthAssumption() != aciEdge.getTruthAssumption()) {
      throw new AppliedCustomInstructionParsingFailedException("The truthAssumption of the CAssumeEdges " + ciEdge + " and " + aciEdge + "are different!");
    }
    ciEdge.getExpression().accept(new StructureComparisonVisitor(aciEdge.getExpression(), ciVarToAciVar));
  }

  private void compareStatementEdge(final CStatementEdge ciEdge, final CStatementEdge aciEdge,
      final Map<String,String> ciVarToAciVar, final Collection<String> outVariables) throws AppliedCustomInstructionParsingFailedException {

    if (ciEdge.getStatement() instanceof CFunctionSummaryStatementEdge && aciEdge.getStatement() instanceof CFunctionSummaryStatementEdge) {
      CFunctionSummaryStatementEdge ciStmt = (CFunctionSummaryStatementEdge) ciEdge.getStatement();
      CFunctionSummaryStatementEdge aciStmt = (CFunctionSummaryStatementEdge) aciEdge.getStatement();

      if (!ciStmt.getFunctionName().equals(aciStmt.getFunctionName())){
        throw new AppliedCustomInstructionParsingFailedException("The functionName of the CFunctionSummaryStatementEdges " + ciEdge + " and " + aciEdge + " are different!");
      }

      compareStatementsOfStatementEdge(ciStmt.getFunctionCall(), aciStmt.getFunctionCall(), ciVarToAciVar, outVariables);

    }
    compareStatementsOfStatementEdge(ciEdge.getStatement(), aciEdge.getStatement(), ciVarToAciVar, outVariables);

  }

  private void compareStatementsOfStatementEdge(final CStatement ci, final CStatement aci,
      final Map<String, String> ciVarToAciVar, final Collection<String> outVariables) throws AppliedCustomInstructionParsingFailedException {

    if (ci instanceof CExpressionAssignmentStatement && aci instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement ciStmt = (CExpressionAssignmentStatement) ci;
      CExpressionAssignmentStatement aciStmt = (CExpressionAssignmentStatement) aci;

      // left side => output variables
      Map<String,String> currentCiVarToAciVar = new HashMap<>();
      ciStmt.getLeftHandSide().accept(new StructureExtendedComparisonVisitor(aciStmt.getLeftHandSide(), ciVarToAciVar, currentCiVarToAciVar));
      outVariables.addAll(currentCiVarToAciVar.values());

      // right side: just proof it
      ciStmt.getRightHandSide().accept(new StructureExtendedComparisonVisitor(aciStmt.getRightHandSide(), ciVarToAciVar, currentCiVarToAciVar));
    }

    else if (ci instanceof CExpressionStatement && aci instanceof CExpressionStatement) {
      CExpressionStatement ciStmt = (CExpressionStatement) ci;
      CExpressionStatement aciStmt = (CExpressionStatement) aci;
      ciStmt.getExpression().accept(new StructureComparisonVisitor(aciStmt.getExpression(), ciVarToAciVar));
    }

    else if (ci instanceof CFunctionCallAssignmentStatement && aci instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement ciStmt = (CFunctionCallAssignmentStatement) ci;
      CFunctionCallAssignmentStatement aciStmt = (CFunctionCallAssignmentStatement) aci;

      // left side => output variables
      Map<String,String> currentCiVarToAciVar = new HashMap<>();
      ciStmt.getLeftHandSide().accept(new StructureExtendedComparisonVisitor(aciStmt.getLeftHandSide(), ciVarToAciVar, currentCiVarToAciVar));
      outVariables.addAll(currentCiVarToAciVar.values());

      compareFunctionCallExpressions(ciStmt.getFunctionCallExpression(), aciStmt.getFunctionCallExpression(), ciVarToAciVar);
    }

    else if (ci instanceof CFunctionCallStatement && aci instanceof CFunctionCallStatement) {
      CFunctionCallStatement ciStmt = (CFunctionCallStatement) ci;
      CFunctionCallStatement aciStmt = (CFunctionCallStatement) aci;

      compareFunctionCallExpressions(ciStmt.getFunctionCallExpression(), aciStmt.getFunctionCallExpression(), ciVarToAciVar);
    }

    else {
      throw new AppliedCustomInstructionParsingFailedException("The types of the CStatement " + ci + " and " + aci + " are different!");
    }
  }

  private void compareFunctionCallExpressions(final CFunctionCallExpression exp,
      final CFunctionCallExpression aexp, final Map<String, String> ciVarToAciVar) throws AppliedCustomInstructionParsingFailedException {
    if (!exp.getExpressionType().equals(aexp.getExpressionType())){
      throw new AppliedCustomInstructionParsingFailedException("The expressionType of the CStatementEdges " + exp + " and " + aexp + " are different!");
    }

    exp.getFunctionNameExpression().accept(
        new StructureComparisonVisitor(aexp.getFunctionNameExpression(), new HashMap<String, String>()));

    List<CExpression> ciList = exp.getParameterExpressions();
    List<CExpression> aciList = aexp.getParameterExpressions();
    for (int i=0; i<ciList.size(); i++) {
      ciList.get(i).accept(new StructureComparisonVisitor(aciList.get(i), ciVarToAciVar));
    }
  }

  private void compareDeclarationEdge(final CDeclarationEdge ciEdge, final CDeclarationEdge aciEdge,
      final Map<String,String> ciVarToAciVar, final Collection<String> outVariables)
        throws AppliedCustomInstructionParsingFailedException {

    CDeclaration ciDec = ciEdge.getDeclaration();
    CDeclaration aciDec = aciEdge.getDeclaration();

    if (ciDec instanceof CVariableDeclaration && aciDec instanceof CVariableDeclaration) {
      CVariableDeclaration ciVarDec = (CVariableDeclaration) ciDec;
      CVariableDeclaration aciVarDec = (CVariableDeclaration) aciDec;

      if (!ciVarDec.getCStorageClass().equals(aciVarDec.getCStorageClass())) {
        throw new AppliedCustomInstructionParsingFailedException("The CVariableDeclaration of ci " + ciVarDec + " and aci " + aciVarDec + " have different StorageClasses.");
      }
      if (!ciVarDec.getType().equals(aciVarDec.getType())) {
        throw new AppliedCustomInstructionParsingFailedException("The CVariableDeclaration of ci " + ciVarDec + " and aci " + aciVarDec + " have different declaration types!");
      }
      if (ciVarToAciVar.containsKey(ciVarDec.getQualifiedName())
          && !ciVarToAciVar.get(ciVarDec.getQualifiedName()).equals(aciVarDec.getQualifiedName())) {
        throw new AppliedCustomInstructionParsingFailedException(
          "The mapping is not clear. The map contains " + ciVarDec.getQualifiedName() + " with the value "
              + ciVarToAciVar.get(ciVarDec.getQualifiedName()) + ", which is different to "
              + aciVarDec.getQualifiedName() + ".");
      }

      compareInitializer(ciVarDec.getInitializer(), aciVarDec.getInitializer(), ciVarToAciVar, outVariables);
      if (ciVarDec.getInitializer() != null) {
        if (ciVarDec.getInitializer() instanceof CInitializerExpression
            || ciVarDec.getInitializer() instanceof CInitializerList) {
          outVariables.add(aciVarDec.getQualifiedName());
        } else {
          throw new AppliedCustomInstructionParsingFailedException("Unsupported initializer: "
              + ciVarDec.getInitializer());
        }
      }
      ciVarToAciVar.put(ciVarDec.getQualifiedName(), aciVarDec.getQualifiedName());
    }

    else if (ciDec instanceof CComplexTypeDeclaration && aciDec instanceof CComplexTypeDeclaration) {
      throw new AppliedCustomInstructionParsingFailedException("The code contains a CComplexTypeDeclaration, which is unsupported.");
    }
    else if (ciDec instanceof CTypeDefDeclaration && aciDec instanceof CTypeDefDeclaration) {
      throw new AppliedCustomInstructionParsingFailedException("The code contains a CTypeDefDeclaration, which is unsupported.");
    }
    else if (ciDec instanceof CFunctionDeclaration && aciDec instanceof CFunctionDeclaration) {
      throw new AppliedCustomInstructionParsingFailedException("The code contains a CFunctionDeclaration, which is unsupported.");
    } else {
      throw new AppliedCustomInstructionParsingFailedException("The declaration of the CDeclarationEdge ci " + ciDec + " and aci " + aciDec + " have different classes.");
    }
  }

  private void compareInitializer(final CInitializer ciI, final CInitializer aciI,
      final Map<String,String> ciVarToAciVar, final Collection<String> outVariables)
      throws AppliedCustomInstructionParsingFailedException {

    if (ciI == null && aciI == null) {
      // nothing to do here
    } else if (ciI == null && aciI != null) {
      throw new AppliedCustomInstructionParsingFailedException("The aci has an initializer but not the ci.");
    } else if (ciI != null && aciI == null) {
      throw new AppliedCustomInstructionParsingFailedException("The ci has an initializer but not the aci.");
    }

    else if (ciI instanceof CInitializerExpression && aciI instanceof CInitializerExpression) {
      ((CInitializerExpression) ciI).getExpression().accept(new StructureComparisonVisitor(((CInitializerExpression) aciI).getExpression(), ciVarToAciVar));
    }

    else if (ciI instanceof CDesignatedInitializer && aciI instanceof CDesignatedInitializer) {
      throw new AppliedCustomInstructionParsingFailedException("The code contains a CDesignatedInitializer, which is unsupported.");
    }

    else if (ciI instanceof CInitializerList && aciI instanceof CInitializerList) {
      List<CInitializer> ciList = ((CInitializerList) ciI).getInitializers();
      List<CInitializer> aciList = ((CInitializerList) aciI).getInitializers();

      if (ciList.size() != aciList.size()) {
        throw new AppliedCustomInstructionParsingFailedException("The CInitializerList of the Initializer of ci " + ciI + " and aci " + aciI + " have different length.");
      } else {
        for (int i=0; i<ciList.size(); i++) {
          compareInitializer(ciList.get(i), aciList.get(i), ciVarToAciVar, outVariables);
        }
      }

    } else {
      throw new AppliedCustomInstructionParsingFailedException("The CInitializer of ci " + ciI + " and aci " + aciI + " are different.");
    }
  }

  private void compareReturnStatementEdge(final CReturnStatementEdge ciEdge,
      final CReturnStatementEdge aciEdge, final Map<String,String> ciVarToAciVar)
          throws AppliedCustomInstructionParsingFailedException {

    if (ciEdge.getExpression().isPresent() && aciEdge.getExpression().isPresent()){
      ciEdge.getExpression().get().accept(new StructureComparisonVisitor(aciEdge.getExpression().get(), ciVarToAciVar));

    } else if ((!ciEdge.getExpression().isPresent() && aciEdge.getExpression().isPresent())
          ||(ciEdge.getExpression().isPresent() && !aciEdge.getExpression().isPresent()) ){
      throw new AppliedCustomInstructionParsingFailedException("The expression of the CReturnStatementEdge of ci " + ciEdge + " and aci " +  aciEdge + " is present in one of them, but not in the otherone.");
    }
  }

  private void compareFunctionCallEdge(final CFunctionCallEdge ciEdge, final CFunctionCallEdge aciEdge,
      final Map<String,String> ciVarToAciVar) throws AppliedCustomInstructionParsingFailedException {

    if(ciEdge.getSuccessor() != aciEdge.getSuccessor()) {
      throw new AppliedCustomInstructionParsingFailedException("Applied custom instruction calls different method than custom instruction.");
    }

    List<CExpression> ciArguments = ciEdge.getArguments();
    List<CExpression> aciArguments = aciEdge.getArguments();
    if (ciArguments.size() != aciArguments.size()) {
      throw new AppliedCustomInstructionParsingFailedException("The amount of arguments of the FunctionCallEdges " + ciEdge + " and " + aciEdge + " are different!");
    }
    for (int i=0; i<ciArguments.size(); i++) {
      ciArguments.get(i).accept(new StructureComparisonVisitor(aciArguments.get(i), ciVarToAciVar));
    }
  }

  CFANode getStartNode() {
    return ciStartNode;
  }

  Collection<CFANode> getEndNodes() {
    return ciEndNodes;
  }

  List<String> getInputVariables() {
    return inputVariables;
  }

  List<String> getOutputVariables() {
    return outputVariables;
  }

  private static class StructureComparisonVisitor implements CExpressionVisitor<Void, AppliedCustomInstructionParsingFailedException>{

    protected CExpression aciExp;
    protected final Map<String,String> ciVarToAciVar;

    public StructureComparisonVisitor(final CExpression pAciExp, final Map<String,String> pCiVarToAciVar) {
      aciExp = pAciExp;
      ciVarToAciVar = pCiVarToAciVar;
    }

    @Override
    public Void visit(final CArraySubscriptExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      if (!(aciExp instanceof CArraySubscriptExpression)) {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CArraySubscriptExpression, but ci is.");
      }
      CArraySubscriptExpression aciAExp = (CArraySubscriptExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of ci " + ciExp + " and aci " + aciExp + " are different.");
      }

      aciExp = aciAExp.getArrayExpression();
      ciExp.getArrayExpression().accept(this);

      aciExp = aciAExp.getSubscriptExpression();
      ciExp.getSubscriptExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(final CFieldReference ciExp) throws AppliedCustomInstructionParsingFailedException {
      if (!(aciExp instanceof CFieldReference)) {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CFieldReference, but ci is.");
      }
      CFieldReference aciFieldRefExp = (CFieldReference) aciExp;
      if (!ciExp.getExpressionType().equals(aciFieldRefExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the FieldReference of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciFieldRefExp + " (" + aciFieldRefExp.getExpressionType() + ").");
      }
      if (ciExp.isPointerDereference() != aciFieldRefExp.isPointerDereference()) {
        throw new AppliedCustomInstructionParsingFailedException("One of the ci " + ciExp + " and aci " + aciFieldRefExp + " is a pointerDereference, while the other one not.");
      }
      this.aciExp = aciFieldRefExp.getFieldOwner();
      ciExp.getFieldOwner().accept(this);
      return null;
    }

    @Override
    public Void visit(final CIdExpression ciExp) throws AppliedCustomInstructionParsingFailedException {
      if (aciExp instanceof CIdExpression) {
        CIdExpression aciIdExp = (CIdExpression) aciExp;
        if (!ciExp.getExpressionType().equals(aciIdExp.getExpressionType())) {
          throw new AppliedCustomInstructionParsingFailedException("The expression type of the IdExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciIdExp + " (" + aciIdExp.getExpressionType() + ").");
        }
        if (ciVarToAciVar.containsKey(ciExp.getDeclaration().getQualifiedName()) && !ciVarToAciVar.get(ciExp.getDeclaration().getQualifiedName()).equals(aciIdExp.getDeclaration().getQualifiedName())) {
          throw new AppliedCustomInstructionParsingFailedException("The mapping is not clear. The map contains " + ciExp.getDeclaration().getQualifiedName() + " with the value " + ciVarToAciVar.get(ciExp.getDeclaration().getQualifiedName()) + ", which is different to " + aciIdExp.getDeclaration().getQualifiedName() + ".");
        } else {
          computeMapping(ciExp.getDeclaration().getQualifiedName(), aciIdExp.getDeclaration().getQualifiedName());
        }
      }

      else if (aciExp instanceof CCharLiteralExpression) {
        throw new AppliedCustomInstructionParsingFailedException("The code contains a CCharLiteralExpression, which is unsupported.");
      }

      else if (aciExp instanceof CStringLiteralExpression) {
        throw new AppliedCustomInstructionParsingFailedException("The code contains a CStringLiteralExpression, which is unsupported.");
      }

      else if (aciExp instanceof CImaginaryLiteralExpression) {
        throw new AppliedCustomInstructionParsingFailedException("The code contains a CImaginaryLiteralExpression, which is unsupported.");
      }

      else if (aciExp instanceof CIntegerLiteralExpression) {
        compareSimpleTypes(ciExp, ((CIntegerLiteralExpression) aciExp).getValue(), (CSimpleType) aciExp.getExpressionType());
      }

      else if (aciExp instanceof CFloatLiteralExpression) {
        compareSimpleTypes(ciExp, ((CFloatLiteralExpression) aciExp).getValue(), (CSimpleType) aciExp.getExpressionType());
      } else {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + ciExp + " is not a CSimpleType.");
      }
      return null;
    }

    protected void computeMapping(final String ciString, final String aciString) {
      ciVarToAciVar.put(ciString, aciString);
    }

    private void compareSimpleTypes(final CIdExpression ciExp, final Number aciExpValue, final CSimpleType aciType) throws AppliedCustomInstructionParsingFailedException {
      if (ciExp.getExpressionType() instanceof CSimpleType) {
        CSimpleType ciST = (CSimpleType) ciExp.getExpressionType();

        if (isValidSimpleType(ciST, aciType)) {
          if (!ciVarToAciVar.containsKey(ciExp.getDeclaration().getQualifiedName())) {
            ciVarToAciVar.put(ciExp.getDeclaration().getQualifiedName(), aciExpValue.toString());
          } else if (!ciVarToAciVar.get(ciExp.getDeclaration().getQualifiedName()).equals(aciExpValue.toString())) {
            throw new AppliedCustomInstructionParsingFailedException("The mapping is not clear. The map contains " + ciExp.getDeclaration().getQualifiedName() + " with the value " + ciVarToAciVar.get(ciExp.getDeclaration().getQualifiedName()) + ", which is different to " + aciExpValue.toString() + ".");
          }
        } else {
          throw new AppliedCustomInstructionParsingFailedException("The simpleType of the ci " + ciExp + " is not a valid one.");
        }
      } else {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not a CIdExpression.");
      }
    }

    private boolean isValidSimpleType(final CSimpleType ciST, final CSimpleType pAciType) {
      if (ciST.isComplex()==pAciType.isComplex()
          && ciST.isConst() == pAciType.isConst()
          && ciST.isImaginary() == pAciType.isImaginary()
          && ciST.isLong() == pAciType.isLong()
          && ciST.isLongLong() == pAciType.isLongLong()
          && ciST.isShort() == pAciType.isShort()
          && ciST.isSigned() == pAciType.isSigned()
          && ciST.isUnsigned() == pAciType.isUnsigned()
          && ciST.isVolatile() == pAciType.isVolatile()
          && (ciST.getType().isIntegerType() || ciST.getType().isFloatingPointType())
          && ciST.isComplex() == ciST.isImaginary()
          && ciST.isImaginary() == ciST.isLong()
          && ciST.isLong() == ciST.isLongLong()
          && ciST.isLongLong() == ciST.isShort()
          && ciST.isShort() == ciST.isSigned()
          && ciST.isSigned() == ciST.isUnsigned()) {
        return true;
      }
      return false;
    }

    @Override
    public Void visit(final CPointerExpression ciExp) throws AppliedCustomInstructionParsingFailedException {
      if (!(aciExp instanceof CPointerExpression)) {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CPointerExpression, but ci is.");
      }
      CPointerExpression aciPExp = (CPointerExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciPExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CPointerExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciPExp + " (" + aciPExp.getExpressionType() + ").");
      }
      this.aciExp = aciPExp.getOperand();
      ciExp.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(final CComplexCastExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      if (!(aciExp instanceof CComplexCastExpression)) {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CComplexCastExpression, but ci is.");
      }
      CComplexCastExpression aciCExp = (CComplexCastExpression) aciExp;
      if (ciExp.isImaginaryCast() != aciCExp.isImaginaryCast()) {
        throw new AppliedCustomInstructionParsingFailedException("One of the ci " + ciExp + " and aci " + aciCExp + " is an imaginaryCast, while the other one not.");
      }
      if (ciExp.isRealCast() != aciCExp.isRealCast()) {
        throw new AppliedCustomInstructionParsingFailedException("One of the ci " + ciExp + " and aci " + aciCExp + " is a realCast, while the other one not.");
      }
      if (!ciExp.getExpressionType().equals(aciCExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CComplexCastExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciCExp + " (" + aciCExp.getExpressionType() + ").");
      }
      if (!ciExp.getType().equals(aciCExp.getType())) {
        throw new AppliedCustomInstructionParsingFailedException("The type of the CComplexCastExpression of ci " + ciExp + " (" + ciExp.getType() + ") is not equal to the one of the aci " + aciCExp + " (" + aciCExp.getType() + ").");
      }
      this.aciExp = aciCExp.getOperand();
      ciExp.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(final CBinaryExpression ciExp) throws AppliedCustomInstructionParsingFailedException {

      if (aciExp instanceof CBinaryExpression) {
        CBinaryExpression aciBinExp = (CBinaryExpression) aciExp;

        // expression types are different
        if (!ciExp.getExpressionType().equals(aciBinExp.getExpressionType())) {
          throw new AppliedCustomInstructionParsingFailedException("The expression type of the CBinaryExpression of ci " + ciExp + " is not equal to the one of the aci " + aciBinExp + ".");
        }

        // operators are different
        if (!ciExp.getOperator().getOperator().equals(aciBinExp.getOperator().getOperator())) {
          throw new AppliedCustomInstructionParsingFailedException("The operators of the CBinaryExpression the ci " + ciExp  + " and aci " + aciBinExp + " are different.");
        }

        if (!ciExp.getCalculationType().equals(aciBinExp.getCalculationType())) {
          throw new AppliedCustomInstructionParsingFailedException("The calculationType of the CBinaryExpression of ci " + ciExp + " and aci " + aciBinExp + " are different.");
        }

        aciExp = aciBinExp.getOperand1();
        ciExp.getOperand1().accept(this);

        aciExp = aciBinExp.getOperand2();
        ciExp.getOperand2().accept(this);

      } else {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CBinaryExpression, but ci is.");
      }

      return null;
    }

    @Override
    public Void visit(final CCastExpression ciExp) throws AppliedCustomInstructionParsingFailedException {
      if (!(aciExp instanceof CCastExpression)) {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CCastExpression, but ci is.");
      }
      CCastExpression aciPExp = (CCastExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciPExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CCastExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciPExp + " (" + aciPExp.getExpressionType() + ").");
      }
      this.aciExp = aciPExp.getOperand();
      ciExp.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(final CCharLiteralExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      if (!(aciExp instanceof CCharLiteralExpression)) {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CCharLiteralExpression, but ci is.");
      }
      CCharLiteralExpression aciCharExp = (CCharLiteralExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciCharExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CharLiteralExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciCharExp + " (" + aciCharExp.getExpressionType() + ").");
      }
      if (ciExp.getCharacter() != aciCharExp.getCharacter()) {
        throw new AppliedCustomInstructionParsingFailedException("The value of the CCharLiteralExpression of ci " + ciExp + " and aci " + aciCharExp + " are different.");
      }
      return null;
    }

    @Override
    public Void visit(final CFloatLiteralExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      if (!(aciExp instanceof CFloatLiteralExpression)) {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CFloatLiteralExpression, but ci is.");
      }
      CFloatLiteralExpression aciFloatExp = (CFloatLiteralExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciFloatExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the FloatLiteralExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciFloatExp + " (" + aciFloatExp.getExpressionType() + ").");
      }
      if (!ciExp.getValue().equals(aciFloatExp.getValue())) {
        throw new AppliedCustomInstructionParsingFailedException("The value of the CCharLiteralExpression of ci " + ciExp + " and aci " + aciFloatExp + " are different.");
      }
      return null;
    }

    @Override
    public Void visit(final CIntegerLiteralExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      if (!(aciExp instanceof CIntegerLiteralExpression)) {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CIntegerLiteralExpression, but ci is.");
      }
      CIntegerLiteralExpression aciIntegerLiteralExp = (CIntegerLiteralExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciIntegerLiteralExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the IntegerLiteralExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciIntegerLiteralExp + " (" + aciIntegerLiteralExp.getExpressionType() + ").");
      }
      if (!ciExp.getValue().equals(aciIntegerLiteralExp.getValue())) {
        throw new AppliedCustomInstructionParsingFailedException("The value of the CIntegerLiteralExpression of ci " + ciExp + " and aci " + aciIntegerLiteralExp + " are different.");
      }
      return null;
    }

    @Override
    public Void visit(final CStringLiteralExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      if (!(aciExp instanceof CStringLiteralExpression)) {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CStringLiteralExpression, but ci is.");
      }
      CStringLiteralExpression aciStringLiteralExp = (CStringLiteralExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciStringLiteralExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the StringLiteralExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciStringLiteralExp + " (" + aciStringLiteralExp.getExpressionType() + ").");
      }
      if (!ciExp.getValue().equals(aciStringLiteralExp.getValue())) {
        throw new AppliedCustomInstructionParsingFailedException("The value of the CIntegerLiteralExpression of ci " + ciExp + " and aci " + aciStringLiteralExp + " are different.");
      }
      return null;
    }

    @Override
    public Void visit(final CTypeIdExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      if (!(aciExp instanceof CTypeIdExpression)) {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CTypeIdExpression, but ci is.");
      }
      CTypeIdExpression aciIdExp = (CTypeIdExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciIdExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CTypeIdExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciIdExp + " (" + aciIdExp.getExpressionType() + ").");
      }
      if (!ciExp.getType().equals(aciIdExp.getType())) {
        throw new AppliedCustomInstructionParsingFailedException("The type of the CTypeIdExpression of ci " + ciExp + " (" + ciExp.getType() + ") is not equal to the one of the aci " + aciIdExp + " (" + aciIdExp.getType() + ").");
      }
      if (!ciExp.getOperator().equals(aciIdExp.getOperator())) {
        throw new AppliedCustomInstructionParsingFailedException("The operator of the CTypeIdExpression of ci " + ciExp + " (" + ciExp.getOperator() + ") is not equal to the one of the aci " + aciIdExp + " (" + aciIdExp.getOperator() + ").");
      }
      return null;
    }

    @Override
    public Void visit(final CUnaryExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {

      if (aciExp instanceof CUnaryExpression) {
        CUnaryExpression aciUnExp = (CUnaryExpression) aciExp;

        // expression types are different
        if (!ciExp.getExpressionType().equals(aciUnExp.getExpressionType())) {
          throw new AppliedCustomInstructionParsingFailedException("The expression type of the CUnaryExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciUnExp + " (" + aciUnExp.getExpressionType() + ").");
        }

        // operators are different
        if (!ciExp.getOperator().getOperator().equals(aciUnExp.getOperator().getOperator())) {
          throw new AppliedCustomInstructionParsingFailedException("The operators of the ci expression " + ciExp  + " and aci expression " + aciUnExp + " don't fit together!");
        }

        this.aciExp = aciUnExp.getOperand();
        ciExp.getOperand().accept(this);

      } else {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type UnaryExpression, but ci is.");
      }

      return null;
    }

    @Override
    public Void visit(final CImaginaryLiteralExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      throw new AppliedCustomInstructionParsingFailedException("The code contains a CImaginaryLiteralExpression, which is unsupported.");
    }

    @Override
    public Void visit(final CAddressOfLabelExpression ciExp)
        throws AppliedCustomInstructionParsingFailedException {
      if (!(aciExp instanceof CAddressOfLabelExpression)) {
        throw new AppliedCustomInstructionParsingFailedException("The aci expression " + aciExp + " is not from the type CAddressOfLabelExpression, but ci is.");
      }
      CAddressOfLabelExpression aciAExp = (CAddressOfLabelExpression) aciExp;
      if (!ciExp.getExpressionType().equals(aciAExp.getExpressionType())) {
        throw new AppliedCustomInstructionParsingFailedException("The expression type of the CAddressOfLabelExpression of ci " + ciExp + " (" + ciExp.getExpressionType() + ") is not equal to the one of the aci " + aciAExp + " (" + aciAExp.getExpressionType() + ").");
      }
      if (!ciExp.getLabelName().equals(aciAExp.getLabelName())) {
        throw new AppliedCustomInstructionParsingFailedException("The label name of the CAddressOfLabelExpression of ci " + ciExp + " and aci " + aciAExp + " are different.");
      }
      return null;
    }

  }

  private static class StructureExtendedComparisonVisitor extends StructureComparisonVisitor {

    private final Map<String,String> currentCiVarToAciVar;

    public StructureExtendedComparisonVisitor(final CExpression pAciExp, final Map<String, String> pCiVarToAciVar,
        Map<String, String> pCurrentCiVarToAciVar) {
      super(pAciExp, pCiVarToAciVar);
      currentCiVarToAciVar = pCurrentCiVarToAciVar;
    }

    @Override
    protected void computeMapping(final String ciString, final String aciString) {
      ciVarToAciVar.put(ciString, aciString);
      currentCiVarToAciVar.put(ciString, aciString);
    }

  }
}
