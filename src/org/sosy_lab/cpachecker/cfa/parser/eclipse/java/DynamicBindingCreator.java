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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.util.NameConverter;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.exceptions.JParserException;

/**
 * This class models the dynamic Bindings of Java in a CFA.
 *
 */
public class DynamicBindingCreator {


  private final ASTConverter astCreator;
  private final CFABuilder cfaBuilder;

  // Data structure for handling dynamic Bindings.
  // For every instance method, with the fully Qualified Name as key of Map,
  // tracks all methods as FunctionEntryNodes and declared Class that override
  // the former method.
  //  (methodName: <packagename>_<ClassName>_<MethodName>[_<TypeOfParameter>])
  private final Map<String, List<Pair<FunctionEntryNode, JClassOrInterfaceType>>> subMethodsOfMethod = new HashMap<>();

  // Data structure for handling dynamic Bindings.
  // For every instance method, with the fully Qualified Name as key of Map,
  // tracks all found possible Bindings which are to replace it. If the runTimeType is JClassOrInterface Typ,
  // the method called is Function Entry Node.
  //  (methodName: <packagename>_<ClassName>_<MethodName>[_<TypeOfParameter>])
  private final Map<String, List<Pair<FunctionEntryNode, JClassOrInterfaceType>>> methodTypeBindingsOfMethod = new HashMap<>();


  public DynamicBindingCreator(CFABuilder builder) {

    cfaBuilder = builder;
    astCreator = builder.getAstCreator();
  }



  public void trackAndCreateDynamicBindings() throws JParserException {

    /*
     *  It starts with a map of all parsed methods while parsing the Java source Code,
     *  tracks all methods which override such a parsed method,
     *  then calculates for every runTimeType the appropriate method binding,
     *  then inserts all  in the CFA in place of the original method call.
     */
    Map<String, FunctionEntryNode> cfAs = cfaBuilder.getCFAs();



    trackOverridenMethods(cfAs);
    completeMethodBindings();

    for (String functionName : cfAs.keySet()) {
      insertBindings(cfAs.get(functionName));
    }
  }

  private void trackOverridenMethods(Map<String, FunctionEntryNode> cfAs) {

    Map<String, MethodDeclaration> allParsedMethodDeclaration = cfaBuilder.getAllParsedMethodDeclaration();

    for (String functionName : cfAs.keySet()) {
      assert allParsedMethodDeclaration.containsKey(functionName);
      // Constructors and default Constructors can't be overriden
      if (!(allParsedMethodDeclaration.get(functionName) == null  || allParsedMethodDeclaration.get(functionName).isConstructor()) ) {
        trackOverridenMethods(allParsedMethodDeclaration.get(functionName), cfAs.get(functionName));
      }
    }
  }

  private void completeMethodBindings() {

    for (String key :  subMethodsOfMethod.keySet()) {
      methodTypeBindingsOfMethod.put(key, new LinkedList<>(subMethodsOfMethod.get(key)));
    }




    Map<String, List<Pair<FunctionEntryNode, JClassOrInterfaceType>>> workMap = new HashMap<>();
    for (String key :  subMethodsOfMethod.keySet()) {
      workMap.put(key, new LinkedList<>(subMethodsOfMethod.get(key)));
    }


    Map<String, MethodDeclaration> allParsedMethodDeclaration = cfaBuilder.getAllParsedMethodDeclaration();


    for (String methodName : workMap.keySet()) {
      if (!(allParsedMethodDeclaration.get(methodName) == null  || allParsedMethodDeclaration.get(methodName).isConstructor()) ) {
        completeBindingsOfMethod(allParsedMethodDeclaration.get(methodName), methodName);
      }
    }
  }






  private void completeBindingsOfMethod(MethodDeclaration methodDeclaration,
      String methodName) {

    JClassOrInterfaceType methodDeclaringType = astCreator.convertClassOrInterfaceType(methodDeclaration.resolveBinding().getDeclaringClass());

    if (methodDeclaringType instanceof JClassType) {
      completeBindingsForDeclaringClassType((JClassType)methodDeclaringType, methodName);
    } else if (methodDeclaringType instanceof JInterfaceType) {
      completeBindingsForDeclaringInterfaceType((JInterfaceType)methodDeclaringType, methodName);
    }
  }

  private void completeBindingsForDeclaringInterfaceType(JInterfaceType methodDeclaringType, String methodName) {
    Set<JClassType> directImpementingClasses = methodDeclaringType.getKnownInterfaceImplementingClasses();


    for (JClassType implementingClasses : directImpementingClasses) {
      copmleteBindingsForClassType(implementingClasses, methodName, methodName);
    }

    Set<JInterfaceType> subInterfaces = methodDeclaringType.getAllSubInterfacesOfInterface();

    for (JInterfaceType subInterface : subInterfaces) {
      for (JClassType implementingClasses : subInterface.getKnownInterfaceImplementingClasses()) {
        copmleteBindingsForClassType(implementingClasses, methodName, methodName);
      }
    }
  }

  private void completeBindingsForDeclaringClassType(JClassType methodDeclaringType,
      String methodName) {
      Set<JClassType> directSubClasses = methodDeclaringType.getDirectSubClasses();

    for (JClassType subClass : directSubClasses) {
      copmleteBindingsForClassType(subClass, methodName, methodName);
    }
  }

  private void copmleteBindingsForClassType(JClassType classType, String methodNametoBeRegistered,
      String methodNameToRegister) {

      String nextMethodNameToRegister = updateBinding(classType, methodNametoBeRegistered, methodNameToRegister);

      Set<JClassType> directSubClasses = classType.getDirectSubClasses();
      for (JClassType subClass : directSubClasses) {
        copmleteBindingsForClassType(subClass, methodNametoBeRegistered, nextMethodNameToRegister);
      }
  }

  private String updateBinding(JClassType classType,  String methodNametoBeRegistered, String methodNameToRegister) {

      List<Pair<FunctionEntryNode, JClassOrInterfaceType>> subMethodBindings = subMethodsOfMethod.get(methodNameToRegister);
      String nextMethodNameToRegister = methodNameToRegister;

      Pair<Boolean, String> hasBindingAndBindingMethodName = typeHasBinding(classType, subMethodBindings);

      if (hasBindingAndBindingMethodName.getFirst()) {
        //Binding already exists possibly with new subMethod
        // Continue with sub method in this traversal Path
        nextMethodNameToRegister = hasBindingAndBindingMethodName.getSecond();
      } else {
           methodTypeBindingsOfMethod.get(methodNametoBeRegistered).add(Pair.of(cfaBuilder.getCFAs().get(methodNameToRegister), (JClassOrInterfaceType) classType));
      }

    return nextMethodNameToRegister;
  }



  private Pair<Boolean, String> typeHasBinding(JClassOrInterfaceType classType,
      List<Pair<FunctionEntryNode, JClassOrInterfaceType>> subMethodBindings) {

    boolean result = false;

    for (Pair<FunctionEntryNode, JClassOrInterfaceType> methodTypeBindingPair : subMethodBindings) {
      result = methodTypeBindingPair.getSecond().equals(classType);
      if (result) {
        return Pair.of(true, methodTypeBindingPair.getFirst().getFunctionName());
      }
    }
    return Pair.of(false, null);
  }

  private void insertBindings(FunctionEntryNode initialNode) throws JParserException {
    // we use a worklist algorithm
    Deque<CFANode> workList = new ArrayDeque<>();
    Set<CFANode> processed = new HashSet<>();

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
          JStatement expr = (JStatement) statement.getStatement();

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

     // We need to add all newly created nodes to processed so that the algorithm works

     JMethodInvocationExpression functionCallExpression = (JMethodInvocationExpression) functionCall.getFunctionCallExpression();
     String functionName = functionCallExpression.getFunctionNameExpression().toASTString();

    if (methodTypeBindingsOfMethod.get(functionName) != null &&    !methodTypeBindingsOfMethod.get(functionName).isEmpty()) {

      if (functionCallExpression instanceof JReferencedMethodInvocationExpression) {

        if (!((JReferencedMethodInvocationExpression) functionCallExpression).hasKnownRunTimeBinding()) {
          createMethodInvocationBindings(edge, pProcessed, functionName);
        } else {
          createOnlyReferencedMethodInvocationBinding(edge, subMethodsOfMethod.get(functionName));
        }
      } else if (!functionCallExpression.getDeclaration().isStatic() && !functionCallExpression.getDeclaration().isFinal() && !(functionCallExpression.getDeclaringType() instanceof JInterfaceType) && !((JClassType) functionCallExpression.getDeclaringType()).isFinal() && !functionCallExpression.hasKnownRunTimeBinding()) {
        createMethodInvocationBindings(edge, pProcessed, functionName);
      }
    }
  }

  private void createMethodInvocationBindings(AStatementEdge edge, Set<CFANode> pProcessed, String functionName) {
    CFANode prevNode = edge.getPredecessor();
    CFANode postConditionNode = edge.getSuccessor();

    // delete old edge
    CFACreationUtils.removeEdgeFromNodes(edge);

    // insert CallExpressions for every found Sub Method
    for (Pair<FunctionEntryNode, JClassOrInterfaceType> overridesThisMethod : methodTypeBindingsOfMethod.get(functionName)) {
      // Only insert Call Expressions if method has a Body
      if (!((JMethodDeclaration) (overridesThisMethod.getFirst().getFunctionDefinition())).isAbstract()) {
        prevNode = createBinding(edge, overridesThisMethod, prevNode, postConditionNode, pProcessed);
      }
    }

    createLastBinding(edge, prevNode, postConditionNode, pProcessed);
  }



  private void createOnlyReferencedMethodInvocationBinding(AStatementEdge edge, List<Pair<FunctionEntryNode, JClassOrInterfaceType>> subMethods) {

    FunctionEntryNode onlyFunction = null;
    Map<JClassOrInterfaceType, FunctionEntryNode> map = new HashMap<>();
    AFunctionCall oldFunctionCall = ((AFunctionCall)edge.getStatement());
    JReferencedMethodInvocationExpression oldFunctionCallExpression =
        (JReferencedMethodInvocationExpression) oldFunctionCall.getFunctionCallExpression();
    JClassType runTimeBinding = oldFunctionCallExpression.getRunTimeBinding();

    // search and copy at the same time.
    // If method can't be found with iterating the list
    // it means that we search for a super Method which
    // can be easier gotten with a map
    for (Pair<FunctionEntryNode, JClassOrInterfaceType> methodBinding : subMethods) {
      if (runTimeBinding.equals(methodBinding.getSecond())) {
        onlyFunction = methodBinding.getFirst();
        break;
      }
      map.put(methodBinding.getSecond(), methodBinding.getFirst());
    }


    if (onlyFunction == null) {

      Set<JClassType> superTypes = runTimeBinding.getAllSuperClasses();

      for (JClassType superType : superTypes) {
        if (map.containsKey(superType)) {
          onlyFunction = map.get(superType);
          break;
        }
      }
    }


    if (onlyFunction == null) {
      throw new IllegalArgumentException("No binding for function of type " + runTimeBinding.getName() + "found.");
    }

    CFANode postNode = edge.getSuccessor();
    CFANode prevNode = edge.getPredecessor();
    // delete old edge
    CFACreationUtils.removeEdgeFromNodes(edge);



    FileLocation fileloc = oldFunctionCallExpression.getFileLocation();


    JReferencedMethodInvocationExpression newFunctionCallExpression = (JReferencedMethodInvocationExpression) astCreator.convert(onlyFunction, oldFunctionCallExpression);


    JStatement newFunctionCall;

    if (oldFunctionCall instanceof JMethodInvocationAssignmentStatement) {
      JMethodInvocationAssignmentStatement oldFunctionCallAssignmentStatement =  (JMethodInvocationAssignmentStatement) oldFunctionCall;
      newFunctionCall =  new JMethodInvocationAssignmentStatement(fileloc, oldFunctionCallAssignmentStatement.getLeftHandSide(), newFunctionCallExpression);

    } else {
      assert edge.getStatement() instanceof JMethodInvocationStatement : "Statement is no Function Call";
      newFunctionCall =   new JMethodInvocationStatement(fileloc, newFunctionCallExpression);
    }


    // new FuncionCallExpressionEdge
    AStatementEdge functionCallEdge =
        new JStatementEdge(newFunctionCall.toASTString(),  newFunctionCall, edge.getFileLocation(), prevNode,
            postNode);
    CFACreationUtils.addEdgeToCFA(functionCallEdge, null);

  }

  private void createLastBinding(AStatementEdge edge, CFANode prevNode, CFANode postConditionNode, Set<CFANode> pProcessed) {

    // TODO ugly, change when a clear way is found to insert
    // functionDeclaaration which weren't parsed when called
    JMethodDeclaration decl =   cfaBuilder.getScope().lookupMethod(((AFunctionCall) edge.getStatement()).getFunctionCallExpression().getFunctionNameExpression().toASTString());

    CFANode nextPrevNode = prevNode;

    if (!decl.isAbstract() && cfaBuilder.getCFAs().containsKey(decl.getName())) {
     nextPrevNode = createBinding(edge, Pair.of(cfaBuilder.getCFAs().get(decl.getName()), decl.getDeclaringClass()), prevNode, postConditionNode, pProcessed);
    }

    //Blank edge from last unsuccessful call to postConditionNode
    BlankEdge postConditionEdge =
        new BlankEdge(edge.getRawStatement(), edge.getFileLocation(), nextPrevNode, postConditionNode, "");
    CFACreationUtils.addEdgeToCFA(postConditionEdge, null);
  }

  private CFANode createBinding(AStatementEdge edge,
      Pair<FunctionEntryNode, JClassOrInterfaceType> overridesThisMethod, CFANode prevNode, CFANode postConditionNode, Set<CFANode> pProcessed) {

    JMethodInvocationExpression oldFunctionCallExpression = (JMethodInvocationExpression) ((AFunctionCall)edge.getStatement()).getFunctionCallExpression();
    AFunctionCall functionCall = ((AFunctionCall)edge.getStatement());

    FileLocation fileloc = oldFunctionCallExpression.getFileLocation();
    String callInFunction = prevNode.getFunctionName();


    JMethodInvocationExpression newFunctionCallExpression =  astCreator.convert(overridesThisMethod.getFirst(), oldFunctionCallExpression);



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
      createConditionEdges(prevNode, successfulNode, unsuccessfulNode, overridesThisMethod.getSecond(), newFunctionCallExpression, fileloc);

      JStatement newFunctionCall;

        if (functionCall instanceof JMethodInvocationAssignmentStatement) {
          JMethodInvocationAssignmentStatement oldFunctionCallAssignmentStatement =  (JMethodInvocationAssignmentStatement) functionCall;
          // TODO Clone leftHandSide
          newFunctionCall = new JMethodInvocationAssignmentStatement(fileloc, oldFunctionCallAssignmentStatement.getLeftHandSide(), newFunctionCallExpression);

        } else {
          assert edge.getStatement() instanceof JMethodInvocationStatement : "Statement is no Function Call";
          newFunctionCall =  new JMethodInvocationStatement(fileloc, newFunctionCallExpression);
        }

        CFANode postFunctionCallNode = new CFANode(fileloc.getStartingLineNumber(),
            callInFunction);
        cfaBuilder.getCFANodes().put(callInFunction, postFunctionCallNode);
        pProcessed.add(postFunctionCallNode);

      //AStatementEdge from successful Node to  postFunctionCall location
      AStatementEdge functionCallEdge = new JStatementEdge(edge.getRawStatement(), newFunctionCall, edge.getFileLocation(), successfulNode, postFunctionCallNode);
      CFACreationUtils.addEdgeToCFA(functionCallEdge, null);

      //Blank edge from postFunctionCall location to postConditionNode
      BlankEdge postConditionEdge = new BlankEdge(edge.getRawStatement(), edge.getFileLocation(), postFunctionCallNode, postConditionNode, "");
      CFACreationUtils.addEdgeToCFA(postConditionEdge, null);

    return unsuccessfulNode;
  }

  private void createConditionEdges(CFANode prevNode, CFANode successfulNode,
      CFANode unsuccessfulNode, JClassOrInterfaceType classTypeOfNewMethodInvocation, JMethodInvocationExpression methodInvocation, FileLocation fileloc) {

        final JExpression exp = astCreator.convertClassRunTimeCompileTimeAccord(fileloc, methodInvocation, classTypeOfNewMethodInvocation);

        String rawSignature = exp.toASTString();

        // edge connecting last condition with elseNode
        final AssumeEdge JAssumeEdgeFalse = new JAssumeEdge("!(" + rawSignature + ")",
            fileloc,
            prevNode,
            unsuccessfulNode,
            exp,
            false);
       CFACreationUtils.addEdgeToCFA(JAssumeEdgeFalse, null);

        // edge connecting last condition with thenNode
        final AssumeEdge JAssumeEdgeTrue = new JAssumeEdge(rawSignature,
            fileloc,
            prevNode,
            successfulNode,
            exp,
            true);
        CFACreationUtils.addEdgeToCFA(JAssumeEdgeTrue, null);
      }


  //TODO Change the way this Methods builds the methods, so any given Path
  // through the Type tree has only to be traversed once
  private void trackOverridenMethods(MethodDeclaration  declaration, FunctionEntryNode entryNode) {

    // If toBeRegistered Method not yet tracked, it needs to be added
    // That way, even if the method is not overridden, it is tracked
    // with an empty list
    if (!subMethodsOfMethod.containsKey(entryNode.getFunctionDefinition().getName())) {
      subMethodsOfMethod.put(entryNode.getFunctionDefinition().getName(), new LinkedList<Pair<FunctionEntryNode, JClassOrInterfaceType>>());
    }

    Pair<FunctionEntryNode, JClassOrInterfaceType> toBeRegistered = getPairToBeRegistered(declaration, entryNode);
    registerForSuperClass(declaration.resolveBinding().getDeclaringClass().getSuperclass(), toBeRegistered, declaration.resolveBinding());
    registerForSuperIntefaces(declaration.resolveBinding().getDeclaringClass().getInterfaces(), toBeRegistered,  declaration.resolveBinding());

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

  // Go Recursively through all SuperClasses,
  // and their Interfaces and SuperInterfaces
  // and register that the method toBeRegistered overrides
  // this function
  private void registerForSuperClass(ITypeBinding superClass, Pair<FunctionEntryNode, JClassOrInterfaceType> toBeRegistered, IMethodBinding bindingToBeRegistered) {
    if (superClass != null) {
      IMethodBinding[] methodsBinding = superClass.getDeclaredMethods();
      for (IMethodBinding methodBinding : methodsBinding) {
        if (bindingToBeRegistered.overrides(methodBinding)) {
          registerMethod(methodBinding, toBeRegistered);
        }
      }
      registerForSuperIntefaces(superClass.getInterfaces(), toBeRegistered, bindingToBeRegistered);
      registerForSuperClass(superClass.getSuperclass(), toBeRegistered, bindingToBeRegistered);
    }
  }

  private void registerMethod(IMethodBinding overriddenMethod, Pair<FunctionEntryNode, JClassOrInterfaceType> toBeRegistered) {
   String overridenMethodName = NameConverter.convertName(overriddenMethod);

   // If Method not yet parsed, it needs to be added
   if (!subMethodsOfMethod.containsKey(overridenMethodName)) {
     subMethodsOfMethod.put(overridenMethodName, new LinkedList<Pair<FunctionEntryNode, JClassOrInterfaceType>>());
   }
     subMethodsOfMethod.get(overridenMethodName).add(toBeRegistered);
  }

  private Pair<FunctionEntryNode, JClassOrInterfaceType> getPairToBeRegistered(MethodDeclaration declaration,
      FunctionEntryNode entryNode) {

    JClassOrInterfaceType classType =  astCreator.convertClassOrInterfaceType(declaration.resolveBinding().getDeclaringClass());
    return Pair.of(entryNode, classType);
  }
}