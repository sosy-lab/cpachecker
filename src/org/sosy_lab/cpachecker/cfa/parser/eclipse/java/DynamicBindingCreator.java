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
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.exceptions.ParserException;


public class DynamicBindingCreator {



  private final ASTConverter astCreator;
  private final CFABuilder cfaBuilder;
  private final TypeHierachie typeHierachie;

  // Data structure for handling dynamic Binding
  // Tracks all fully Qualified method names the  Method with the name as key overrides
  //  (methodName: <packagename>_<ClassName>_<MethodName>[_<TypeOfParameter>])
  private final Map<String , List<Pair<FunctionEntryNode, JClassOrInterfaceType>>> subMethodsOfMethod = new HashMap<String, List<Pair<FunctionEntryNode, JClassOrInterfaceType>>>();


  public DynamicBindingCreator(CFABuilder builder , TypeHierachie pTypeHierachie) {

    cfaBuilder = builder;
    astCreator = builder.getAstCreator();
    typeHierachie = pTypeHierachie;
  }

  public Map<String , List<Pair<FunctionEntryNode, JClassOrInterfaceType>>> getSubMethodsOfMethod() {
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
         // To Skip new Nodes
            createBindings(statement, (AFunctionCall)expr, processed);

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

  private void createBindings(AStatementEdge edge, AFunctionCall functionCall, Set<CFANode> pProcessed) {
     // We need to add all newly created node to processed so that the algorithm works


     AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
     String functionName = functionCallExpression.getFunctionNameExpression().toASTString();

    if(subMethodsOfMethod.get(functionName) != null &&    !subMethodsOfMethod.get(functionName).isEmpty() && functionCallExpression instanceof JReferencedMethodInvocationExpression){
      if (!((JReferencedMethodInvocationExpression) functionCallExpression).hasKnownRunTimeBinding()) {
        CFANode prevNode = edge.getPredecessor();
        CFANode postConditionNode = edge.getSuccessor();

        // delete old edge
        CFACreationUtils.removeEdgeFromNodes(edge);

        // insert CallExpressions for every found Sub Method
        for (Pair<FunctionEntryNode, JClassOrInterfaceType> overridesThisMethod : subMethodsOfMethod.get(functionName)) {
          // Only insert Call Expressions if method has a Body
          if (!((JMethodDeclaration) (overridesThisMethod.getFirst().getFunctionDefinition())).isAbstract()) {
            prevNode = createBinding(edge, overridesThisMethod, prevNode, postConditionNode, pProcessed);
          }
        }

        createLastBinding(edge, prevNode, postConditionNode, pProcessed);

      } else {
        createOnlyBinding(edge, subMethodsOfMethod.get(functionName));
      }
    }
  }

  private void createOnlyBinding(AStatementEdge edge, List<Pair<FunctionEntryNode, JClassOrInterfaceType>> subMethods) {

    FunctionEntryNode onlyFunction = null;
    Map<JClassOrInterfaceType, FunctionEntryNode > map = new HashMap<JClassOrInterfaceType, FunctionEntryNode>();
    AFunctionCall oldFunctionCall = ((AFunctionCall)edge.getStatement());
    JReferencedMethodInvocationExpression oldFunctionCallExpression =
        (JReferencedMethodInvocationExpression) oldFunctionCall.getFunctionCallExpression();
    JClassType runTimeBinding = oldFunctionCallExpression.getRunTimeBinding();

    // search and copy at the same time.
    // If method can't be found with iterating the list
    // it means that we search for a super Method which
    // can be easier gotten with a map
    for(Pair<FunctionEntryNode, JClassOrInterfaceType> methodBinding : subMethods){
      if(runTimeBinding.equals(methodBinding.getSecond())){
        onlyFunction = methodBinding.getFirst();
        break;
      }
      map.put(methodBinding.getSecond(), methodBinding.getFirst());
    }


    if(onlyFunction == null) {
      List<JClassType> superTypes = typeHierachie.getAllSuperClasses(runTimeBinding);

      for(JClassType superType : superTypes){
        if(map.containsKey(superType)) {
          onlyFunction = map.get(superType);
          break;
        }
      }
    }


    if(onlyFunction == null){
      //TODO Throw Exception;
    }

    CFANode postNode = edge.getSuccessor();
    CFANode prevNode = edge.getPredecessor();
    // delete old edge
    CFACreationUtils.removeEdgeFromNodes(edge);



    CFileLocation fileloc = oldFunctionCallExpression.getFileLocation();
    String callInFunction = prevNode.getFunctionName();


    JReferencedMethodInvocationExpression newFunctionCallExpression = (JReferencedMethodInvocationExpression) astCreator.convert(onlyFunction, oldFunctionCallExpression);


    IAStatement newFunctionCall;

    if(oldFunctionCall instanceof AFunctionCallAssignmentStatement){
      AFunctionCallAssignmentStatement oldFunctionCallAssignmentStatement = (AFunctionCallAssignmentStatement) oldFunctionCall;
      newFunctionCall = new AFunctionCallAssignmentStatement( fileloc, oldFunctionCallAssignmentStatement.getLeftHandSide(), newFunctionCallExpression);

    }else {
      assert edge.getStatement() instanceof AFunctionCallStatement : "Statement is no Function Call";
      newFunctionCall =  new AFunctionCallStatement(fileloc, newFunctionCallExpression );
    }


    // new FuncionCallExpressionEdge
    AStatementEdge functionCallEdge =
        new AStatementEdge(newFunctionCall.toASTString(),  newFunctionCall , edge.getLineNumber(), prevNode,
            postNode);
    CFACreationUtils.addEdgeToCFA(functionCallEdge, null);

  }

  private void createLastBinding(AStatementEdge edge, CFANode prevNode , CFANode postConditionNode, Set<CFANode> pProcessed) {

    // TODO ugly, change when a clear way is found to insert
    // functionDeclaaration which weren't parsed when called
    JMethodDeclaration decl =  (JMethodDeclaration) cfaBuilder.getScope().lookupFunction( ((AFunctionCall) edge.getStatement()).getFunctionCallExpression().getFunctionNameExpression().toASTString());

    if (!decl.isAbstract()) {


      JMethodInvocationExpression oldFunctionCallExpression =
          (JMethodInvocationExpression) ((AFunctionCall) edge.getStatement()).getFunctionCallExpression();

      CFileLocation fileloc = oldFunctionCallExpression.getFileLocation();
      String callInFunction = prevNode.getFunctionName();

      CFANode postFunctionCallNode = new CFANode(fileloc.getStartingLineNumber(),
          callInFunction);
      cfaBuilder.getCFANodes().put(callInFunction, postFunctionCallNode);
      pProcessed.add(postFunctionCallNode);

      //TODO A Case where the function call is unknown
      // edge from prev (unsuccessful) Node to postFunctionCallNode
      AStatementEdge functionCallEdge =
          new AStatementEdge(edge.getRawStatement(), edge.getStatement(), edge.getLineNumber(), prevNode,
              postFunctionCallNode);
      CFACreationUtils.addEdgeToCFA(functionCallEdge, null);

      //Blank edge from postFunctionCall location to postConditionNode
      BlankEdge postConditionEdge =
          new BlankEdge(edge.getRawStatement(), edge.getLineNumber(), postFunctionCallNode, postConditionNode, "");
      CFACreationUtils.addEdgeToCFA(postConditionEdge, null);

    } else {

      //Blank edge from last unsuccessful call to postConditionNode
      BlankEdge postConditionEdge =
          new BlankEdge(edge.getRawStatement(), edge.getLineNumber(), prevNode, postConditionNode, "");
      CFACreationUtils.addEdgeToCFA(postConditionEdge, null);

    }
  }

  private CFANode createBinding(AStatementEdge edge,
      Pair<FunctionEntryNode, JClassOrInterfaceType> overridesThisMethod, CFANode prevNode, CFANode postConditionNode, Set<CFANode> pProcessed) {

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
        pProcessed.add(successfulNode);

      // unsuccessfulNode if Run-Time-Type does not equals
      // function declaring Class Type,
      CFANode unsuccessfulNode = new CFANode(fileloc.getStartingLineNumber(),
          callInFunction);
      cfaBuilder.getCFANodes().put(callInFunction, unsuccessfulNode);
      pProcessed.add(unsuccessfulNode);


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
        pProcessed.add(postFunctionCallNode);

      //AStatementEdge from successful Node to  postFunctionCall location
      AStatementEdge functionCallEdge = new AStatementEdge(edge.getRawStatement(), newFunctionCall  , edge.getLineNumber(), successfulNode, postFunctionCallNode);
      CFACreationUtils.addEdgeToCFA(functionCallEdge, null);

      //Blank edge from postFunctionCall location to postConditionNode
      BlankEdge postConditionEdge = new BlankEdge(edge.getRawStatement(), edge.getLineNumber(), postFunctionCallNode, postConditionNode, "");
      CFACreationUtils.addEdgeToCFA(postConditionEdge, null);

    return unsuccessfulNode;
  }







  private void createConditionEdges(CFANode prevNode, CFANode successfulNode,
      CFANode unsuccessfulNode , JClassOrInterfaceType classTypeOfNewMethodInvocation , IASimpleDeclaration referencedVariable , CFileLocation fileloc) {

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
      subMethodsOfMethod.put(entryNode.getFunctionDefinition().getName(), new LinkedList<Pair<FunctionEntryNode,JClassOrInterfaceType>>());
    }

    Pair<FunctionEntryNode , JClassOrInterfaceType> toBeRegistered = getPairToBeRegistered(declaration , entryNode);
    registerForSuperClass(declaration.resolveBinding().getDeclaringClass().getSuperclass() , toBeRegistered, declaration.resolveBinding());
    registerForSuperIntefaces(declaration.resolveBinding().getDeclaringClass().getInterfaces() , toBeRegistered,  declaration.resolveBinding());

  }

  // Go Recursively through all SuperInterfaces, and register that the method toBeRegistered overrides
  // this function
  private void registerForSuperIntefaces(ITypeBinding[] interfaces, Pair<FunctionEntryNode, JClassOrInterfaceType> toBeRegistered,
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

  // Go Recursively through all SuperClasses, and their Interfaces and SuperInterfaces and register that the method toBeRegistered overrides
  // this function
  private void registerForSuperClass(ITypeBinding superClass , Pair<FunctionEntryNode , JClassOrInterfaceType> toBeRegistered, IMethodBinding bindingToBeRegistered) {
    if(superClass != null){
      IMethodBinding[] methodsBinding = superClass.getDeclaredMethods();
      for(IMethodBinding methodBinding : methodsBinding){
        if(bindingToBeRegistered.overrides(methodBinding)){
          registerMethod(methodBinding , toBeRegistered);
        }
      }
      registerForSuperIntefaces(superClass.getInterfaces(), toBeRegistered, bindingToBeRegistered);
      registerForSuperClass(superClass.getSuperclass(), toBeRegistered, bindingToBeRegistered);
    }
  }

  private void registerMethod(IMethodBinding overriddenMethod , Pair<FunctionEntryNode, JClassOrInterfaceType> toBeRegistered) {
   String overridenMethodName = astCreator.getFullyQualifiedMethodName(overriddenMethod);

   // If Method not yet parsed, it needs to be added
   if(!subMethodsOfMethod.containsKey(overridenMethodName)){
     subMethodsOfMethod.put(overridenMethodName, new LinkedList<Pair<FunctionEntryNode,JClassOrInterfaceType>>());
   }
     subMethodsOfMethod.get(overridenMethodName).add(toBeRegistered);
  }

  private Pair<FunctionEntryNode, JClassOrInterfaceType> getPairToBeRegistered(MethodDeclaration declaration,
      FunctionEntryNode entryNode) {

    JClassOrInterfaceType classType =  astCreator.convertClassOrInterfaceType(declaration.resolveBinding().getDeclaringClass());
    return Pair.of(entryNode, classType);
  }
}