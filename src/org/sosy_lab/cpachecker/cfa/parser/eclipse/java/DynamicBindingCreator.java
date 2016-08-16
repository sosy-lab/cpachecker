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
import java.util.SortedMap;

import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JConstructorDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
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
import org.sosy_lab.cpachecker.cfa.model.java.JMethodEntryNode;
import org.sosy_lab.cpachecker.cfa.model.java.JStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.collect.SortedSetMultimap;

/**
 * This class models the dynamic bindings of Java in a CFA.
 *
 */
class DynamicBindingCreator {


  private final ASTConverter astCreator;
  private final CFABuilder cfaBuilder;

  // Data structure for handling dynamic Bindings.
  // For every instance method, with the fully Qualified Name as key of Map,
  // tracks all methods as FunctionEntryNodes and declared Class that override
  // the former method.
  //  (methodName: <packagename>_<ClassName>_<MethodName>[_<TypeOfParameter>])
  private final Map<String, List<MethodDefinition>> subMethodsOfMethod = new HashMap<>();

  // Data structure for handling dynamic Bindings.
  // For every instance method, with the fully Qualified Name as key of Map,
  // tracks all found possible Bindings which are to replace it.
  // If the runTimeType is JClassOrInterfaceType, the method called is Function Entry Node.
  //  (methodName: <packagename>_<ClassName>_<MethodName>[_<TypeOfParameter>])
  private final Map<String, List<MethodDefinition>> methodTypeBindingsOfMethod = new HashMap<>();


  public DynamicBindingCreator(CFABuilder builder) {

    cfaBuilder = builder;
    astCreator = builder.getAstCreator();
  }



  public void trackAndCreateDynamicBindings() {

    /*
     *  It starts with a map of all parsed methods while parsing the Java source Code,
     *  tracks all methods which override such a parsed method,
     *  then calculates for every runTimeType the appropriate method binding,
     *  then inserts all in the CFA in place of the original method call.
     */
    Map<String, FunctionEntryNode> cfAs = cfaBuilder.getCFAs();

    trackOverridenMethods(cfAs);
    completeMethodBindings();

    for (Map.Entry<String, FunctionEntryNode> functionEntry : cfAs.entrySet()) {
      insertBindings(functionEntry.getValue());
    }
  }

  private void trackOverridenMethods(Map<String, FunctionEntryNode> cfAs) {

    Map<String, JMethodDeclaration> allParsedMethodDeclaration
        = cfaBuilder.getAllParsedMethodDeclaration();

    for (Map.Entry<String, FunctionEntryNode> entry : cfAs.entrySet()) {
      String functionName = entry.getKey();
      FunctionEntryNode currEntryNode = entry.getValue();
      JMethodDeclaration currMethod = allParsedMethodDeclaration.get(functionName);

      assert allParsedMethodDeclaration.containsKey(functionName);

      // Constructors and default Constructors can't be overriden
      if (!(currMethod == null || isConstructor(currMethod)) ) {
        trackOverridenMethods(currMethod, currEntryNode);
      }
    }
  }

  private boolean isConstructor(JMethodDeclaration pFunction) {
    return pFunction instanceof JConstructorDeclaration;
  }

  private void completeMethodBindings() {

    for (Map.Entry<String, List<MethodDefinition>> entry :  subMethodsOfMethod.entrySet()) {
      methodTypeBindingsOfMethod.put(entry.getKey(),
                                     new LinkedList<>(entry.getValue()));
    }

    Map<String, List<MethodDefinition>> workMap = new HashMap<>();

    for (Map.Entry<String, List<MethodDefinition>> entry : subMethodsOfMethod.entrySet()) {
      workMap.put(entry.getKey(),
                  new LinkedList<>(entry.getValue()));
    }


    Map<String, JMethodDeclaration> allParsedMethodDeclaration = cfaBuilder.getAllParsedMethodDeclaration();

    for (String methodName : workMap.keySet()) {
      JMethodDeclaration methodDeclaration = allParsedMethodDeclaration.get(methodName);
      if (methodDeclaration != null && !isConstructor(methodDeclaration)) {

        completeBindingsOfMethod(methodDeclaration, methodName);
      }
    }
  }

  private void completeBindingsOfMethod(JMethodDeclaration methodDeclaration, String methodName) {

    JClassOrInterfaceType methodDeclaringType = methodDeclaration.getDeclaringClass();

    if (methodDeclaringType instanceof JClassType) {
      completeBindingsForDeclaringClassType((JClassType)methodDeclaringType, methodName);
    } else if (methodDeclaringType instanceof JInterfaceType) {
      completeBindingsForDeclaringInterfaceType((JInterfaceType)methodDeclaringType, methodName);
    }
  }

  private void completeBindingsForDeclaringInterfaceType(JInterfaceType methodDeclaringType, String methodName) {
    Set<JClassType> directImplementingClasses = methodDeclaringType.getKnownInterfaceImplementingClasses();


    for (JClassType implementingClasses : directImplementingClasses) {
      completeBindingsForClass(implementingClasses, methodName, methodName);
    }

    Set<JInterfaceType> subInterfaces = methodDeclaringType.getAllSubInterfacesOfInterface();

    for (JInterfaceType subInterface : subInterfaces) {
      for (JClassType implementingClasses : subInterface.getKnownInterfaceImplementingClasses()) {
        completeBindingsForClass(implementingClasses, methodName, methodName);
      }
    }
  }

  private void completeBindingsForDeclaringClassType(JClassType methodDeclaringType,
      String methodName) {
      Set<JClassType> directSubClasses = methodDeclaringType.getDirectSubClasses();

    for (JClassType subClass : directSubClasses) {
      completeBindingsForClass(subClass, methodName, methodName);
    }
  }

  private void completeBindingsForClass(JClassType classType, String methodNametoBeRegistered,
      String methodNameToRegister) {

      String nextMethodNameToRegister = updateBinding(classType, methodNametoBeRegistered, methodNameToRegister);

      Set<JClassType> directSubClasses = classType.getDirectSubClasses();
      for (JClassType subClass : directSubClasses) {
        completeBindingsForClass(subClass, methodNametoBeRegistered, nextMethodNameToRegister);
      }
  }

  private String updateBinding(JClassType classType, String methodNametoBeRegistered, String methodNameToRegister) {

      List<MethodDefinition> subMethodBindings = subMethodsOfMethod.get(methodNameToRegister);
      String nextMethodNameToRegister = methodNameToRegister;

      Pair<Boolean, String> hasBindingAndBindingMethodName = typeHasBinding(classType, subMethodBindings);

      if (hasBindingAndBindingMethodName.getFirst()) {
        //Binding already exists possibly with new subMethod
        // Continue with sub method in this traversal Path
        nextMethodNameToRegister = hasBindingAndBindingMethodName.getSecond();
      } else {
        MethodDefinition methodDefinition =
            new MethodDefinition(cfaBuilder.getCFAs().get(methodNameToRegister), classType);

        methodTypeBindingsOfMethod.get(methodNametoBeRegistered)
                                  .add(methodDefinition);
      }

    return nextMethodNameToRegister;
  }



  private Pair<Boolean, String> typeHasBinding(
      JClassOrInterfaceType classType, List<MethodDefinition> subMethodBindings) {

    boolean result;

    for (MethodDefinition methodTypeBindingPair : subMethodBindings) {
      result = methodTypeBindingPair.getDefiningType().equals(classType);
      if (result) {
        return Pair.of(true, methodTypeBindingPair.getMethodEntryNode().getFunctionName());
      }
    }

    return Pair.of(false, null);
  }

  private void insertBindings(FunctionEntryNode initialNode) {
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

     JMethodInvocationExpression functionCallExpression =
         (JMethodInvocationExpression) functionCall.getFunctionCallExpression();

     String functionName = functionCallExpression.getFunctionNameExpression().toASTString();

    if (bindingForMethodExists(functionName)) {

      if (functionCallExpression instanceof JReferencedMethodInvocationExpression) {

        if (!functionCallExpression.hasKnownRunTimeBinding()) {
          createMethodInvocationBindings(edge, pProcessed, functionName);

        } else {
          createOnlyReferencedMethodInvocationBinding(edge, subMethodsOfMethod.get(functionName));
        }

      } else if (invocationDependsOnRuntimeType(functionCallExpression)) {
        createMethodInvocationBindings(edge, pProcessed, functionName);
      }
    }
  }

  private boolean bindingForMethodExists(String pMethodName) {
    return methodTypeBindingsOfMethod.get(pMethodName) != null
        && !methodTypeBindingsOfMethod.get(pMethodName).isEmpty();
  }

  private boolean invocationDependsOnRuntimeType(JMethodInvocationExpression pMethodInvocation) {
    JMethodDeclaration method = pMethodInvocation.getDeclaration();

    return !method.isStatic()
        && !method.isFinal()
        && !(pMethodInvocation.getDeclaringType() instanceof JInterfaceType)
        && !((JClassType) pMethodInvocation.getDeclaringType()).isFinal()
        && !pMethodInvocation.hasKnownRunTimeBinding();
  }

  private void createMethodInvocationBindings(AStatementEdge edge, Set<CFANode> pProcessed, String functionName) {
    CFANode prevNode = edge.getPredecessor();
    CFANode postConditionNode = edge.getSuccessor();

    // delete old edge
    CFACreationUtils.removeEdgeFromNodes(edge);

    List<MethodDefinition> typesDefiningMethod
        = methodTypeBindingsOfMethod.get(functionName);

    // insert CallExpressions for every found Sub Method
    for (MethodDefinition overridesThisMethod : typesDefiningMethod) {
      // Only insert Call Expressions if method has a Body
      if (!overridesThisMethod.isAbstract()) {
        prevNode = createBinding(edge, overridesThisMethod, prevNode, postConditionNode, pProcessed);
      }
    }

    createLastBinding(edge, prevNode, postConditionNode, pProcessed);
  }



  private void createOnlyReferencedMethodInvocationBinding(
      AStatementEdge edge, List<MethodDefinition> subMethods) {

    FunctionEntryNode onlyFunction = null;
    Map<JClassOrInterfaceType, FunctionEntryNode> map = new HashMap<>();
    AFunctionCall oldFunctionCall = ((AFunctionCall)edge.getStatement());
    JReferencedMethodInvocationExpression oldFunctionCallExpression =
        (JReferencedMethodInvocationExpression) oldFunctionCall.getFunctionCallExpression();
    JClassOrInterfaceType runTimeBinding = oldFunctionCallExpression.getRunTimeBinding();

    // search and copy at the same time.
    // If method can't be found with iterating the list
    // it means that we search for a super Method which
    // can be easier gotten with a map
    for (MethodDefinition methodBinding : subMethods) {
      JClassOrInterfaceType typeThatDefinesMethod = methodBinding.getDefiningType();
      FunctionEntryNode methodEntry = methodBinding.getMethodEntryNode();

      if (runTimeBinding.equals(typeThatDefinesMethod)) {
        onlyFunction = methodEntry;
        break;
      }

      map.put(typeThatDefinesMethod, methodEntry);
    }


    if (onlyFunction == null) {

      List<JClassOrInterfaceType> superTypes = runTimeBinding.getAllSuperTypesOfType();

      for (JClassOrInterfaceType superType : superTypes) {
        if (map.containsKey(superType)) {
          onlyFunction = map.get(superType);
          break;
        }
      }
    }


    if (onlyFunction == null) {
      throw new IllegalArgumentException("No binding for function of type "
          + runTimeBinding.getName() + " found.");
    }

    CFANode postNode = edge.getSuccessor();
    CFANode prevNode = edge.getPredecessor();
    // delete old edge
    CFACreationUtils.removeEdgeFromNodes(edge);



    FileLocation fileloc = oldFunctionCallExpression.getFileLocation();


    JReferencedMethodInvocationExpression newFunctionCallExpression =
        (JReferencedMethodInvocationExpression) astCreator.convert(onlyFunction, oldFunctionCallExpression);


    JStatement newFunctionCall;

    if (oldFunctionCall instanceof JMethodInvocationAssignmentStatement) {
      JMethodInvocationAssignmentStatement oldFunctionCallAssignmentStatement =
          (JMethodInvocationAssignmentStatement) oldFunctionCall;

      newFunctionCall =
          new JMethodInvocationAssignmentStatement(fileloc,
                                                   oldFunctionCallAssignmentStatement.getLeftHandSide(),
                                                   newFunctionCallExpression);

    } else {
      assert edge.getStatement() instanceof JMethodInvocationStatement
          : "Statement is no Function Call";

      newFunctionCall = new JMethodInvocationStatement(fileloc, newFunctionCallExpression);
    }


    // new FuncionCallExpressionEdge
    AStatementEdge functionCallEdge =
        new JStatementEdge(newFunctionCall.toASTString(),
                           newFunctionCall,
                           edge.getFileLocation(),
                           prevNode,
                           postNode);
    CFACreationUtils.addEdgeToCFA(functionCallEdge, null);

  }

  private void createLastBinding(AStatementEdge edge,
                                 CFANode prevNode,
                                 CFANode postConditionNode,
                                 Set<CFANode> pProcessed) {

    final AFunctionCallExpression functionCall =
        ((AFunctionCall) edge.getStatement()).getFunctionCallExpression();

    final AExpression functionName = functionCall.getFunctionNameExpression();

    // TODO ugly, change when a clear way is found to insert
    // functionDeclaration which weren't parsed when called
    final JMethodDeclaration decl = cfaBuilder.getScope().lookupMethod(functionName.toASTString());

    CFANode nextPrevNode = prevNode;
    SortedMap<String, FunctionEntryNode> cfas = cfaBuilder.getCFAs();
    String declarationName = decl.getName();

    if (!decl.isAbstract() && cfas.containsKey(declarationName)) {
     nextPrevNode = createBinding(edge,
                                  new MethodDefinition(cfas.get(declarationName), decl.getDeclaringClass()),
                                  prevNode,
                                  postConditionNode,
                                  pProcessed);
    }

    // Blank edge from last unsuccessful call to postConditionNode
    BlankEdge postConditionEdge =
        new BlankEdge(edge.getRawStatement(), edge.getFileLocation(), nextPrevNode, postConditionNode, "");
    CFACreationUtils.addEdgeToCFA(postConditionEdge, null);
  }

  private CFANode createBinding(AStatementEdge edge,
                                MethodDefinition overridesThisMethod,
                                CFANode prevNode,
                                CFANode postConditionNode,
                                Set<CFANode> pProcessed) {

    AFunctionCall functionCall = ((AFunctionCall) edge.getStatement());

    JMethodInvocationExpression oldFunctionCallExpression =
        (JMethodInvocationExpression) functionCall.getFunctionCallExpression();

    FileLocation fileloc = oldFunctionCallExpression.getFileLocation();
    String callInFunction = prevNode.getFunctionName();

    JMethodInvocationExpression newFunctionCallExpression =
        astCreator.convert(overridesThisMethod.getMethodEntryNode(), oldFunctionCallExpression);

    SortedSetMultimap<String, CFANode> cfas = cfaBuilder.getCFANodes();

    // Node for successful function call
    // That is the case if runtime type equals function declaring class type.
    CFANode successfulNode = new CFANode(callInFunction);
    cfas.put(callInFunction, successfulNode);
    pProcessed.add(successfulNode);

    // unsuccessfulNode if runtime type does not equal function declaring class type
    CFANode unsuccessfulNode = new CFANode(callInFunction);
    cfas.put(callInFunction, unsuccessfulNode);
    pProcessed.add(unsuccessfulNode);

    JClassOrInterfaceType definingType = overridesThisMethod.getDefiningType();

    // Create condition which represents this.getClass().equals(functionClass.getClass())
    createConditionEdges(prevNode,
                         successfulNode,
                         unsuccessfulNode,
                         definingType,
                         newFunctionCallExpression,
                         fileloc);

    JStatement newFunctionCall;

    if (functionCall instanceof JMethodInvocationAssignmentStatement) {
      JMethodInvocationAssignmentStatement oldFunctionCallAssignmentStatement =
          (JMethodInvocationAssignmentStatement) functionCall;

      // TODO Clone leftHandSide
      JLeftHandSide leftSide = oldFunctionCallAssignmentStatement.getLeftHandSide();
      newFunctionCall = new JMethodInvocationAssignmentStatement(fileloc,
                                                                 leftSide,
                                                                 newFunctionCallExpression);

    } else {
      assert edge.getStatement() instanceof JMethodInvocationStatement
          : "Statement is no Function Call";
      newFunctionCall = new JMethodInvocationStatement(fileloc, newFunctionCallExpression);
    }

    CFANode postFunctionCallNode = new CFANode(callInFunction);
    cfaBuilder.getCFANodes().put(callInFunction, postFunctionCallNode);
    pProcessed.add(postFunctionCallNode);

    //AStatementEdge from successful Node to postFunctionCall location
    AStatementEdge functionCallEdge = new JStatementEdge(edge.getRawStatement(),
                                                         newFunctionCall,
                                                         edge.getFileLocation(),
                                                         successfulNode,
                                                         postFunctionCallNode);
    CFACreationUtils.addEdgeToCFA(functionCallEdge, null);

    //Blank edge from postFunctionCall location to postConditionNode
    BlankEdge postConditionEdge = new BlankEdge(edge.getRawStatement(),
                                                edge.getFileLocation(),
                                                postFunctionCallNode,
                                                postConditionNode,
                                                "");
    CFACreationUtils.addEdgeToCFA(postConditionEdge, null);

    return unsuccessfulNode;
  }

  private void createConditionEdges(CFANode prevNode, CFANode successfulNode,
      CFANode unsuccessfulNode, JClassOrInterfaceType classTypeOfNewMethodInvocation,
      JMethodInvocationExpression methodInvocation, FileLocation fileloc) {

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
  private void trackOverridenMethods(JMethodDeclaration declaration, FunctionEntryNode entryNode) {

    String functionName = entryNode.getFunctionDefinition().getName();

    // If toBeRegistered Method not yet tracked, it needs to be added
    // That way, even if the method is not overridden, it is tracked
    // with an empty list
    if (!subMethodsOfMethod.containsKey(functionName)) {
      subMethodsOfMethod.put(functionName, new LinkedList<>());
    }

    final MethodDefinition toBeRegistered = getMethodDefinition(declaration, entryNode);
    final JClassOrInterfaceType declaringClassType = declaration.getDeclaringClass();
    final List<JClassOrInterfaceType> declaringClassesSuperTypes = declaringClassType.getAllSuperTypesOfType();

    registerForSuperClass(declaringClassesSuperTypes, toBeRegistered, declaration);
  }

  // Go Recursively through all SuperClasses,
  // and their Interfaces and SuperInterfaces
  // and register that the method toBeRegistered overrides
  // this function
  private void registerForSuperClass(
      List<JClassOrInterfaceType> pSuperClasses, MethodDefinition pToBeRegistered,
      JMethodDeclaration pBindingToBeRegistered) {

    final TypeHierarchy typeHierarchy = cfaBuilder.getScope().getTypeHierarchy();

    for (JClassOrInterfaceType t : pSuperClasses) {
      if (typeHierarchy.isExternType(t)) {
        continue;
      }

      Set<JMethodDeclaration> methods = typeHierarchy.getMethodDeclarations(t);

        for (JMethodDeclaration m : methods) {
          if (overrides(m, pBindingToBeRegistered)) {
            registerMethod(m, pToBeRegistered);
          }
        }
    }
  }

  /**
   * Returns whether the first given method overrides the second one.
   * It is assumed that the first method is declared in a subtype of the second one.
   */
  private boolean overrides(
      JMethodDeclaration pPossiblyOverwriting, JMethodDeclaration pPossiblyOverwritten) {

    final JMethodType firstType = pPossiblyOverwriting.getType();
    final JMethodType sndType = pPossiblyOverwritten.getType();

    if (!pPossiblyOverwriting.getSimpleName()
          .equals(
              pPossiblyOverwritten.getSimpleName())) {
      return false;
    }

    final JType firstReturnType = (JType) firstType.getReturnType();
    final JType sndReturnType = (JType) sndType.getReturnType();

    if (!firstReturnType.equals(sndReturnType)) {

      if (!(firstReturnType instanceof JClassOrInterfaceType
            && sndReturnType instanceof JClassOrInterfaceType)
          || !isSubType((JClassOrInterfaceType) firstReturnType, (JClassOrInterfaceType) sndReturnType)) {
        return false;
      }
    }

    final List<JType> firstParameters = firstType.getParameters();
    final List<JType> secondParameters = sndType.getParameters();

    if (firstParameters.size() != secondParameters.size()) {
      return false;
    }

    for (int i = 0; i < firstParameters.size(); i++) {
      if (!firstParameters.get(i).equals(secondParameters.get(i))) {
        return false;
      }
    }

    return true;
  }

  private boolean isSubType(JClassOrInterfaceType pPossibleSubtype, JClassOrInterfaceType pPossibleSuperType) {
     return pPossibleSubtype.getAllSuperTypesOfType().contains(pPossibleSuperType);
  }

  private void registerMethod(JMethodDeclaration pMethodDeclaration, MethodDefinition pToBeRegistered) {
   String overridenMethodName = pMethodDeclaration.getQualifiedName();

   // If Method not yet parsed, it needs to be added
   if (!subMethodsOfMethod.containsKey(overridenMethodName)) {
      subMethodsOfMethod.put(overridenMethodName, new LinkedList<>());
   }
     subMethodsOfMethod.get(overridenMethodName).add(pToBeRegistered);
  }

  private MethodDefinition getMethodDefinition(
      JMethodDeclaration declaration, FunctionEntryNode entryNode) {

    JClassOrInterfaceType classType = declaration.getDeclaringClass();

    return new MethodDefinition(entryNode, classType);
  }

  /**
   * Describes the definition of a method by a specific type.
   * The method is represented by its {@link JMethodEntryNode}, the type by its
   * {@link JClassOrInterfaceType}.
   */
  private static class MethodDefinition {
    private final FunctionEntryNode methodEntryNode;
    private final JClassOrInterfaceType definingType;

    MethodDefinition(FunctionEntryNode pMethodEntryNode, JClassOrInterfaceType pDefiningType) {
      methodEntryNode = pMethodEntryNode;
      definingType = pDefiningType;
    }

    FunctionEntryNode getMethodEntryNode() {
      return methodEntryNode;
    }

    JClassOrInterfaceType getDefiningType() {
      return definingType;
    }

    boolean isAbstract() {
      return methodEntryNode instanceof JMethodEntryNode
        && ((JMethodEntryNode) methodEntryNode).getFunctionDefinition().isAbstract();
    }
  }
}
