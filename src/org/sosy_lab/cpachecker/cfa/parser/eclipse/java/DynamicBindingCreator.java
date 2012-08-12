/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.CFileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.exceptions.ParserException;


public class DynamicBindingCreator {



  private final ASTConverter astCreator;
  private final CFABuilder cfaBuilder;

  // Data structure for handling dynamic Binding
  // Tracks all fully Qualified method names the  Method with the name as key overrides
  //  (methodName: <packagename>_<ClassName>_<MethodName>[_<TypeOfParameter>])
  private final Map<String , List<Pair<FunctionEntryNode, JClassType>>> subMethodsOfMethod = new HashMap<String, List<Pair<FunctionEntryNode, JClassType>>>();


  public DynamicBindingCreator(CFABuilder builder){

    cfaBuilder = builder;
    astCreator = builder.getAstCreator();
  }

  public Map<String , List<Pair<FunctionEntryNode, JClassType>>> getSubMethodsOfMethod() {
    return subMethodsOfMethod;
  }

  public void trackAndCreateDynamicBindings() throws ParserException {

     Map<String, MethodDeclaration> allParsedMethodDeclaration = cfaBuilder.getAllParsedMethodDeclaration();
     Map<String, FunctionEntryNode> cfAs = cfaBuilder.getCFAs();


    for(String functionName : cfAs.keySet()){
      assert allParsedMethodDeclaration.containsKey(functionName);
     trackOverridenMethods(allParsedMethodDeclaration.get(functionName), cfAs.get(functionName));
    }

    for(String functionName : cfAs.keySet()){
      insertBindings(cfAs.get(functionName));
    }

  }




  private void insertBindings(FunctionEntryNode initialNode) throws ParserException {
    // we use a worklist algorithm
    Deque<CFANode> workList = new ArrayDeque<CFANode>();
    Set<CFANode> processed = new HashSet<CFANode>();

    workList.addLast(initialNode);

    while (!workList.isEmpty()) {
      CFANode node = workList.pollFirst();
      if (!processed.add(node)) {
        // already handled
        continue;
      }

      for (CFAEdge edge : leavingEdges(node)) {
        if (edge instanceof AStatementEdge) {
          AStatementEdge statement = (AStatementEdge)edge;
          IAStatement expr = statement.getStatement();

          // if statement is of the form x = call(a,b); or call(a,b);
          if (expr instanceof AFunctionCall) {
            createBindings(statement, (AFunctionCall)expr);
          }
        }

        // if successor node is not on a different CFA, add it to the worklist
        CFANode successorNode = edge.getSuccessor();
        if (node.getFunctionName().equals(successorNode.getFunctionName())) {
          workList.add(successorNode);
        }
      }
    }
  }

  private void createBindings(AStatementEdge edge, AFunctionCall functionCall) {

     AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
    String functionName = functionCallExpression.getFunctionNameExpression().toASTString();


    if(!subMethodsOfMethod.get(functionName).isEmpty() && functionCallExpression instanceof JReferencedMethodInvocationExpression){

      CFANode prevNode = edge.getPredecessor();

      CFANode postConditionNode = edge.getSuccessor();

      // delete old edge
      CFACreationUtils.removeEdgeFromNodes(edge);

      for(Pair<FunctionEntryNode , JClassType> overridesThisMethod  : subMethodsOfMethod.get(functionName)){
       prevNode = createBinding(edge,  overridesThisMethod, prevNode, postConditionNode );
      }


      createLastBinding(edge , prevNode , postConditionNode);

    }
  }


  private void createLastBinding(AStatementEdge edge, CFANode prevNode , CFANode postConditionNode) {


    JMethodInvocationExpression oldFunctionCallExpression = (JMethodInvocationExpression) ((AFunctionCall)edge.getStatement()).getFunctionCallExpression();

    CFileLocation fileloc = oldFunctionCallExpression.getFileLocation();
    String callInFunction = prevNode.getFunctionName();

    CFANode postFunctionCallNode = new CFANode(fileloc.getStartingLineNumber(),
        callInFunction);
    cfaBuilder.getCFANodes().put(callInFunction, postFunctionCallNode);

    //TODO A Case where the function call is unknown
    // edge from prev (unsuccessful) Node to postFunctionCallNode
    AStatementEdge functionCallEdge = new AStatementEdge(edge.getRawStatement(), edge.getStatement(), edge.getLineNumber(), prevNode, postFunctionCallNode);
    CFACreationUtils.addEdgeToCFA(functionCallEdge, null);


  //Blank edge from postFunctionCall location to postConditionNode
  BlankEdge postConditionEdge = new BlankEdge(edge.getRawStatement(), edge.getLineNumber(), postFunctionCallNode, postConditionNode, "");
  CFACreationUtils.addEdgeToCFA(postConditionEdge, null);

  }

  private CFANode createBinding(AStatementEdge edge,
      Pair<FunctionEntryNode, JClassType> overridesThisMethod, CFANode prevNode, CFANode postConditionNode) {

    JMethodInvocationExpression oldFunctionCallExpression = (JMethodInvocationExpression) ((AFunctionCall)edge.getStatement()).getFunctionCallExpression();
    AFunctionCall functionCall = ((AFunctionCall)edge.getStatement());

    CFileLocation fileloc = oldFunctionCallExpression.getFileLocation();
    String callInFunction = prevNode.getFunctionName();


    JReferencedMethodInvocationExpression newFunctionCallExpression = (JReferencedMethodInvocationExpression) astCreator.convert(overridesThisMethod.getFirst(), oldFunctionCallExpression);



      // Node for successful function call
      // That is the case if Run-Time-Type equals function declaring Class Type.
      CFANode successfulNode = new CFANode(fileloc.getStartingLineNumber(),
         callInFunction);
        cfaBuilder.getCFANodes().put(callInFunction, successfulNode);

      // unsuccessfulNode if Run-Time-Type does not equals
      // function declaring Class Type,
      CFANode unsuccessfulNode = new CFANode(fileloc.getStartingLineNumber(),
          callInFunction);
      cfaBuilder.getCFANodes().put(callInFunction, unsuccessfulNode);



      // Create Condition which is  this.getClass().equals(functionClass.getClass())
      createConditionEdges(prevNode, successfulNode, unsuccessfulNode, overridesThisMethod.getSecond(), newFunctionCallExpression.getReferencedVariable(), fileloc);


      IAStatement newFunctionCall;

        if(functionCall instanceof AFunctionCallAssignmentStatement){
          AFunctionCallAssignmentStatement oldFunctionCallAssignmentStatement = (AFunctionCallAssignmentStatement) functionCall;
          // TODO Clone leftHandSide
          newFunctionCall = new AFunctionCallAssignmentStatement( fileloc, oldFunctionCallAssignmentStatement.getLeftHandSide(), newFunctionCallExpression);

        }else {
          assert edge.getStatement() instanceof AFunctionCallStatement : "Statement is no Function Call";
          newFunctionCall =  new AFunctionCallStatement(fileloc, newFunctionCallExpression );
        }

        CFANode postFunctionCallNode = new CFANode(fileloc.getStartingLineNumber(),
            callInFunction);
        cfaBuilder.getCFANodes().put(callInFunction, postFunctionCallNode);

      //AStatementEdge from successful Node to  postFunctionCall location
      AStatementEdge functionCallEdge = new AStatementEdge(edge.getRawStatement(), newFunctionCall  , edge.getLineNumber(), successfulNode, postFunctionCallNode);
      CFACreationUtils.addEdgeToCFA(functionCallEdge, null);

      //Blank edge from postFunctionCall location to postConditionNode
      BlankEdge postConditionEdge = new BlankEdge(edge.getRawStatement(), edge.getLineNumber(), postFunctionCallNode, postConditionNode, "");
      CFACreationUtils.addEdgeToCFA(postConditionEdge, null);

    return unsuccessfulNode;
  }







  private void createConditionEdges(CFANode prevNode, CFANode successfulNode,
      CFANode unsuccessfulNode , JClassType classTypeOfNewMethodInvocation , IASimpleDeclaration referencedVariable , CFileLocation fileloc) {

        final JExpression exp = astCreator.convertClassRunTimeCompileTimeAccord(fileloc ,referencedVariable, classTypeOfNewMethodInvocation);

        String rawSignature = exp.toASTString();

        // edge connecting last condition with elseNode
        final AssumeEdge JAssumeEdgeFalse = new AssumeEdge("!(" + rawSignature + ")",
            fileloc.getStartingLineNumber(),
            prevNode,
            unsuccessfulNode,
            exp,
            false);
       CFACreationUtils.addEdgeToCFA(JAssumeEdgeFalse, null);


        // edge connecting last condition with thenNode
        final AssumeEdge JAssumeEdgeTrue = new AssumeEdge(rawSignature,
            fileloc.getStartingLineNumber(),
            prevNode,
            successfulNode,
            exp,
            true);
        CFACreationUtils.addEdgeToCFA(JAssumeEdgeTrue, null);
      }






  //TODO Change the way this Methods builds the methods, so any given Path
  // through the Type tree has only to be traversed once
  private void trackOverridenMethods(MethodDeclaration  declaration , FunctionEntryNode entryNode){

    // If toBeRegistered Method not yet tracked, it needs to be added
    // That way, even if the method is not overridden, it is tracked
    // with an empty list
    if(!subMethodsOfMethod.containsKey(entryNode.getFunctionDefinition().getName())){
      subMethodsOfMethod.put(entryNode.getFunctionDefinition().getName(), new LinkedList<Pair<FunctionEntryNode,JClassType>>());
    }

    Pair<FunctionEntryNode , JClassType> toBeRegistered = getPairToBeRegistered(declaration , entryNode);
    registerForSuperClass(declaration.resolveBinding().getDeclaringClass().getSuperclass() , toBeRegistered, declaration.resolveBinding());
    registerForSuperIntefaces(declaration.resolveBinding().getDeclaringClass().getInterfaces() , toBeRegistered,  declaration.resolveBinding());

  }

  // Go Recursively through all SuperInterfaces, and register that the method toBeRegistered overrides
  // this function
  private void registerForSuperIntefaces(ITypeBinding[] interfaces, Pair<FunctionEntryNode, JClassType> toBeRegistered,
      IMethodBinding bindingToBeRegistered) {
    if (interfaces.length > 0) {
      for (ITypeBinding inface : interfaces) {
        IMethodBinding[] methodsBinding = inface.getDeclaredMethods();
        for (IMethodBinding methodBinding : methodsBinding) {
          if (bindingToBeRegistered.overrides(methodBinding)) {
            registerMethod(methodBinding, toBeRegistered);
          }
        }
        registerForSuperIntefaces(inface.getInterfaces(), toBeRegistered, bindingToBeRegistered);
      }
    }
  }

  // Go Recursively through all SuperClasses, and register that the method toBeRegistered overrides
  // this function
  private void registerForSuperClass(ITypeBinding superClass , Pair<FunctionEntryNode , JClassType> toBeRegistered, IMethodBinding bindingToBeRegistered) {
    if(superClass != null){
      IMethodBinding[] methodsBinding = superClass.getDeclaredMethods();
      for(IMethodBinding methodBinding : methodsBinding){
        if(bindingToBeRegistered.overrides(methodBinding)){
          registerMethod(methodBinding , toBeRegistered);
        }
      }
      registerForSuperClass(superClass.getSuperclass(), toBeRegistered, bindingToBeRegistered);
    }
  }

  private void registerMethod(IMethodBinding overriddenMethod , Pair<FunctionEntryNode, JClassType> toBeRegistered) {
   String overridenMethodName = astCreator.getFullyQualifiedMethodName(overriddenMethod);

   // If Method not yet parsed, it needs to be added
   if(!subMethodsOfMethod.containsKey(overridenMethodName)){
     subMethodsOfMethod.put(overridenMethodName, new LinkedList<Pair<FunctionEntryNode,JClassType>>());
   }
     subMethodsOfMethod.get(overridenMethodName).add(toBeRegistered);
  }

  private Pair<FunctionEntryNode, JClassType> getPairToBeRegistered(MethodDeclaration declaration,
      FunctionEntryNode entryNode) {

    JClassType classType =  astCreator.convertClassType(declaration.resolveBinding().getDeclaringClass());
    return Pair.of(entryNode, classType);
  }
}