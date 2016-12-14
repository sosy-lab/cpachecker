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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNode;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JConstructorDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JObjectReferenceReturn;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JSuperConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JConstructorType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;


class ASTConverter {

  private static final boolean NOT_FINAL = false;

  private static final int FIRST = 0;

  private final LogManager logger;

  private Scope scope;

  private final ASTTypeConverter typeConverter;

  private LinkedList<JDeclaration> forInitDeclarations = new LinkedList<>();
  private LinkedList<JAstNode> preSideAssignments = new LinkedList<>();
  private LinkedList<JAstNode> postSideAssignments = new LinkedList<>();

  private ConditionalExpression conditionalExpression = null;
  private JIdExpression conditionalTemporaryVariable = null;

  // Temporary stores forLoopIterator
  private JIdExpression enhancedForLoopIterator;

  /**
   * Create a new AST Converter, which can be used to convert
   * JDT AST Statements to CFA AST Statements.
   *
   * @param pScope The symbolic table to solve e. g. names of variable to Declarations .
   * @param pLogger Logmanager to log Statusmessages or Errors
   */
  public ASTConverter(Scope pScope, LogManager pLogger) {
    scope = pScope;
    logger = pLogger;
    typeConverter = new ASTTypeConverter(scope);
  }

  /**
   * This method returns the number of Post Side Assignments
   * of converted Statements.
   *
   * @return  number of Post Side Assignments of converted Statements
   */
  public int numberOfPostSideAssignments() {
    return postSideAssignments.size();
  }

  /**
   * This method returns the number of  Side Assignments
   * of converted Statements.
   *
   * @return number of  Side Assignments of converted Statements
   */
  public int numberOfSideAssignments() {
    return preSideAssignments.size() + postSideAssignments.size();
  }

  /**
   * This method returns the number of Pre Side Assignments
   * of a converted Statements.
   *
   * @return number of Pre Side Assignments of converted Statements
   */
  public int numberOfPreSideAssignments() {
    return preSideAssignments.size();
  }

  /**
   * This method returns the next unproccessed Pre Side Assignment
   * of converted Statements.
   *
   * @return  Pre Side Assignment of converted Statements
   */
  public JAstNode getNextSideAssignment() {
    if (numberOfPreSideAssignments() > 0) {
      return preSideAssignments.removeFirst();
    } else {
      return postSideAssignments.removeFirst();
    }
  }

  /**
   * This method returns the next pre side assignment.
   *
   * @return the next pre side assignment
   */
  public JAstNode getNextPreSideAssignment() {
    return preSideAssignments.removeFirst();
  }

  /**
   * This method returns the next Post Side Assignments
   * of converted Statements.
   *
   * @return  Post Side Assignments of converted Statement
   */
  public JAstNode getNextPostSideAssignment() {
    return postSideAssignments.removeFirst();
  }



  /**
   * Erases the saved Conditional Expression of the
   * last converted Statement
   */
  public void resetConditionalExpression() {
    conditionalExpression = null;
  }

  /**
   * Returns the last Conditional Statement of converted Statements
   *
   * @return Constional Statement of converted Statements
   */
  public ConditionalExpression getConditionalExpression() {
    return conditionalExpression;
  }

  /**
   * Returns the latest temporary variable. This variable holds the
   * result of the last processed conditional statement.
   *
   * @return a {@link JIdExpression} representing the temporary variable of the last processed
   *         conditional statement
   */
  public JIdExpression getConditionalTemporaryVariable() {
    return conditionalTemporaryVariable;
  }



  private static void check(boolean assertion, String msg, ASTNode astNode) throws CFAGenerationRuntimeException {
    if (!assertion) {
      throw new CFAGenerationRuntimeException(msg, astNode);
    }
  }

/**
 * Gives back all declarations of Declaration Statement
 *
 * @return declarations of converted Statement
 */
  public List<JDeclaration> getForInitDeclaration() {
    return forInitDeclarations;
  }

  /**
   * Gives back number of declarations of converted Declaration Statement.
   *
   * @return number of declarations of converted Statement
   */
  public int numberOfForInitDeclarations() {
    return forInitDeclarations.size();
  }

  private String getQualifiedName(String var) {
    return scope.createQualifiedName(var);
  }

  private JType convert(ITypeBinding pTypeBinding) {
    return typeConverter.convert(pTypeBinding);
  }

  private JType convert(Type pType) {
    return typeConverter.convert(pType);
  }

  private JArrayType convert(ArrayType pType) {
    return (JArrayType) typeConverter.convert(pType);
  }

  public JClassOrInterfaceType convertClassOrInterfaceType(ITypeBinding pClassBinding) {
    return typeConverter.convertClassOrInterfaceType(pClassBinding);
  }

  /**
   * Converts a Method Declaration
   * of the JDT AST to a MethodDeclaration of the CFA AST
   *
   *
   * @param md method declaration to be converted.
   * @return CFA AST of Method Declaration
   */
  public JMethodDeclaration convert(final MethodDeclaration md) {

    IMethodBinding methodBinding = md.resolveBinding();

    if (methodBinding == null) {
      logger.log(Level.WARNING, "Could not resolve Binding for Method Declaration ");
      logger.log(Level.WARNING, md.getName());
      return JMethodDeclaration.createUnresolvedMethodDeclaration();
    }

    String methodName = NameConverter.convertName(methodBinding);

    checkArgument(scope.isMethodRegistered(methodName));

    // Declaration was already parsed, return declaration
    return scope.lookupMethod(methodName);

  }

  private JClassOrInterfaceType getDeclaringClassType(IMethodBinding mi) {

    JType declaringClassType = null;

    if (mi != null) {
      declaringClassType = convert(mi.getDeclaringClass());
    }

    JClassOrInterfaceType declaringClass;
    if (declaringClassType instanceof JClassOrInterfaceType) {
      declaringClass = (JClassOrInterfaceType) declaringClassType;
    } else {
      // Create a dummy if type is Unspecified
      declaringClass = JClassType.createUnresolvableType();
    }

    return declaringClass;
  }

  /**
   * Takes a ASTNode, and tries to get Information of its Placement in the
   * Source Code. If it doesnt't find such information, returns
   * an empty FileLocation Object.
   *
   *
   * @param l A Code piece wrapped in an ASTNode
   * @return FileLocation with Placement Information of the Code Piece, or null
   *          if such Information could not be obtained.
   */
  public FileLocation getFileLocation(ASTNode l) {
    if (l == null) {
      return FileLocation.DUMMY;
    } else if (l.getRoot().getNodeType() != ASTNode.COMPILATION_UNIT) {
      logger.log(Level.WARNING, "Can't find Placement Information for :"
          + l.toString());
      return FileLocation.DUMMY;
    }

    CompilationUnit co = (CompilationUnit) l.getRoot();

    return new FileLocation(
        scope.getFileOfCurrentType(),
        l.getStartPosition(),
        l.getLength(),
        co.getLineNumber(l.getStartPosition()),
        co.getLineNumber(l.getLength() + l.getStartPosition()));
  }

  /**
   * Converts a List of Field Declaration into the intern AST.
   *
   * @param fd declaration to be transformed.
   * @return intern AST of the Field Declarations.
   */
  public List<JDeclaration> convert(FieldDeclaration fd) {

    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> vdfs =
        fd.fragments();

    return transformedImmutableListCopy(vdfs, this::handleFieldDeclarationFragment);
  }


  private JDeclaration handleFieldDeclarationFragment(VariableDeclarationFragment pVdf) {
    // TODO initializer with side assignment

    NameAndInitializer nameAndInitializer = getNamesAndInitializer(pVdf);

    String fieldName = nameAndInitializer.getName();

    checkArgument(scope.isFieldRegistered(fieldName));

    JFieldDeclaration fieldDecl = scope.lookupField(fieldName);

    // update initializer (can't be constructed while generating the Declaration)
    if (preSideAssignments.size() != 0 || postSideAssignments.size() != 0) {
      logger.log(Level.WARNING, "Sideeffects of initializer of field "
          + fieldName + "will be ignored");
      preSideAssignments.clear();
      postSideAssignments.clear();
    }

    fieldDecl.updateInitializer(nameAndInitializer.getInitializer());

    return fieldDecl;
  }

  private NameAndInitializer getNamesAndInitializer(VariableDeclarationFragment d) {

    JInitializerExpression initializerExpression = null;

    // If there is no Initializer, JVariableDeclaration expects null to be given.
    if (d.getInitializer() != null) {

      JExpression iniExpr = convertExpressionWithoutSideEffects(d.getInitializer());

      initializerExpression =
          new JInitializerExpression(getFileLocation(d), iniExpr);
    }

    String name = NameConverter.convertName(d.resolveBinding());

    return new NameAndInitializer(name, initializerExpression);
  }


  private static class NameAndInitializer {

    private final String name;

    private final JInitializerExpression initializer;

    public NameAndInitializer(String pName, JInitializerExpression pInitializer) {

      checkNotNull(pName);

      name = pName;
      initializer = pInitializer;
    }

    public String getName() {
      return name;
    }

    @Nullable
    public JInitializerExpression getInitializer() {
      return initializer;
    }
  }


/**
 * Converts JDT VariableDeclarationStatement into an AST.
 *
 *
 * @param vds JDT VariableDeclarationStatement to be transformed
 * @return AST representing given Parameter
 */
  public List<JDeclaration> convert(VariableDeclarationStatement vds) {

    List<JDeclaration> variableDeclarations = new ArrayList<>();

    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> variableDeclarationFragments = vds.fragments();

    FileLocation fileLoc = getFileLocation(vds);
    Type type = vds.getType();


    @SuppressWarnings("unchecked")
    ModifierBean mB = ModifierBean.getModifiers(vds.modifiers());

    assert (!mB.isAbstract()) : "Local Variable has abstract modifier?";
    assert (!mB.isNative()) : "Local Variable has native modifier?";
    assert (mB.getVisibility() == VisibilityModifier.NONE) : "Local Variable has Visibility modifier?";
    assert (!mB.isStatic()) : "Local Variable has static modifier?";
    assert (!mB.isStrictFp()) : "Local Variable has strictFp modifier?";
    assert (!mB.isSynchronized()) : "Local Variable has synchronized modifier?";

    for (VariableDeclarationFragment vdf : variableDeclarationFragments) {

      NameAndInitializer nameAndInitializer = getNamesAndInitializer(vdf);

      JVariableDeclaration newD = new JVariableDeclaration(fileLoc,
          convert(type), nameAndInitializer.getName(),
          nameAndInitializer.getName(),
          getQualifiedName(nameAndInitializer.getName()),
          nameAndInitializer.getInitializer(),
          mB.isFinal());

      variableDeclarations.add(newD);
    }

    return variableDeclarations;
  }


  /**
   * Converts JDT SingleVariableDeclaration into an AST.
   *
   *
   * @param d JDT SingleVariableDeclaration to be transformed
   * @return AST representing given Parameter
   */
  public JDeclaration convert(SingleVariableDeclaration d) {


    Type type = d.getType();

    @SuppressWarnings("unchecked")
    ModifierBean mB = ModifierBean.getModifiers(d.modifiers());

    assert (!mB.isAbstract) : "Local Variable has abstract modifier?";
    assert (!mB.isNative) : "Local Variable has native modifier?";
    assert (mB.visibility == VisibilityModifier.NONE) : "Local Variable has Visibility modifier?";
    assert (!mB.isStatic) : "Local Variable has static modifier?";
    assert (!mB.isStrictFp) : "Local Variable has strictFp modifier?";
    assert (!mB.isSynchronized) : "Local Variable has synchronized modifier?";

    JInitializerExpression initializerExpression = null;

    // If there is no Initializer, CStorageClass expects null to be given.
    if (d.getInitializer() != null) {

      JExpression iniExpr =
          (JExpression) convertExpressionWithSideEffects(d.getInitializer());

      initializerExpression =
          new JInitializerExpression(getFileLocation(d), iniExpr);
    }

    return new JVariableDeclaration(getFileLocation(d),
        convert(type), d.getName().getFullyQualifiedName(),
        d.getName().getFullyQualifiedName(),
        getQualifiedName(d.getName().getFullyQualifiedName()),
        initializerExpression, mB.isFinal());
  }

/**
 * Converts ReturnStatement into AST.
 *
 * @param s JDT ReturnStatement to be transformed.
 * @return AST JReturnstatement representing given parameter s
 */
  public JReturnStatement convert(final ReturnStatement s) {

    JExpression expr = convertExpressionWithoutSideEffects(s.getExpression());

    return new JReturnStatement(getFileLocation(s), Optional.ofNullable(expr));
  }

/**
 * Converts a JDT Expression into the AST. This method always gives
 * side effect free Expressions back. Every Side Effect will be
 * put into a side assignment and can subsequently be fetched
 * with getNextSideAssignment().
 *
 * @param e expression to be transformed.
 * @return a side effect free AST representing the given parameter.
 */
  public JExpression convertExpressionWithoutSideEffects(Expression e) {

    JAstNode node = convertExpressionWithSideEffects(e);

    if (node instanceof JCastExpression) {
      // Sideassignment to solve cast.
      return addSideassignmentsForCasts(node, e);
    } else if (node == null || node instanceof JExpression) {
      return (JExpression) node;

    } else if ((node instanceof JMethodInvocationExpression)) {
      return addSideassignmentsForExpressionsWithoutMethodInvocationSideEffects(node, e);

    } else if (node instanceof JAssignment) {

      addSideassignmentsForExpressionsWithoutAssignmentSideEffects(node, e);

      return ((JAssignment) node).getLeftHandSide();
    } else {
      throw new AssertionError("unknown expression " + node);
    }
  }

  private JIdExpression addSideassignmentsForCasts(JAstNode node, Expression e) {
    JIdExpression tmp = createTemporaryVariable(e);


    preSideAssignments.add(new JExpressionAssignmentStatement(node.getFileLocation(),
        tmp,
         (JExpression) node));
    return tmp;
}



  private void addSideassignmentsForExpressionsWithoutAssignmentSideEffects(JAstNode node, Expression e) {

    if (e instanceof PostfixExpression) {
      postSideAssignments.add(node);
    } else {
      preSideAssignments.add(node);
    }

  }

  private JExpression addSideassignmentsForExpressionsWithoutMethodInvocationSideEffects(JAstNode node, Expression e) {
    JIdExpression tmp = createTemporaryVariable(e);

    preSideAssignments.add(new JMethodInvocationAssignmentStatement(node.getFileLocation(),
        tmp,
        (JMethodInvocationExpression) node));
    return tmp;
  }


  private JIdExpression createTemporaryVariable(Expression e) {

    String name = "__CPAchecker_TMP_";
    int i = 0;
    while (scope.variableNameInUse(name + i, name + i)) {
      i++;
    }
    name += i;

    JVariableDeclaration decl = new JVariableDeclaration(getFileLocation(e),
        convert(e.resolveTypeBinding()),
        name,
        name,
        getQualifiedName(name),
        null, NOT_FINAL);

    scope.registerDeclarationOfThisClass(decl);
    preSideAssignments.add(decl);
    JIdExpression tmp = new JIdExpression(decl.getFileLocation(),
        convert(e.resolveTypeBinding()),
        name,
        decl);
    return tmp;
  }

/**
 * Converts a  JDT ExpressionStatement into a statement.
 *
 * @param s ExpressionStatement to be transformed.
 * @return AST representing given parameter.
 */
  public JStatement convert(final ExpressionStatement s) {

    JAstNode node = convertExpressionWithSideEffects(s.getExpression());

    if (node instanceof JExpressionAssignmentStatement) {
      return (JExpressionAssignmentStatement) node;

    } else if (node instanceof JMethodInvocationAssignmentStatement) {
      return (JMethodInvocationAssignmentStatement) node;

    } else if (node instanceof JMethodInvocationExpression) {
      return new JMethodInvocationStatement(getFileLocation(s), (JMethodInvocationExpression) node);

    } else if (node instanceof JExpression) {
      return new JExpressionStatement(getFileLocation(s), (JExpression) node);

    } else {
      throw new AssertionError("Unhandled node type " + node.getClass().getCanonicalName());
    }
  }

  /**
   * Transforms a JDT SuperConstructorInvocation into the intern AST.
   *
   * @param sCI SuperConstructorInvocation to be transformed.
   * @return AST representing given parameter.
   */
  public JStatement convert(final SuperConstructorInvocation sCI) {

    IMethodBinding binding = sCI.resolveConstructorBinding();

    boolean canBeResolved = binding != null;

    if (canBeResolved) {
      scope.registerClass(binding.getDeclaringClass());
    }

    @SuppressWarnings("unchecked")
    List<Expression> p = sCI.arguments();

    List<JExpression> params;

    if (p.size() > 0) {
      params = convert(p);

    } else {
      params = Collections.emptyList();
    }

    String name;
    String simpleName;

    if (canBeResolved) {
      name = NameConverter.convertName(binding);
      simpleName = binding.getName();
    } else {
      // If binding can't be resolved, the constructor is not parsed in all cases.
      name = sCI.toString();
      simpleName = sCI.toString();
    }

    JConstructorDeclaration declaration = (JConstructorDeclaration) scope.lookupMethod(name);

    if (declaration == null) {

      if (canBeResolved) {

        ModifierBean mb = ModifierBean.getModifiers(binding);

        declaration = scope.createExternConstructorDeclaration(
            convertConstructorType(binding),
            name, simpleName, mb.getVisibility(), mb.isStrictFp(),
            (JClassType) getDeclaringClassType(binding));

      } else {
        declaration = JConstructorDeclaration.createUnresolvedConstructorDeclaration();
      }
    }

    JExpression functionName;

    if (canBeResolved) {
      functionName =
          new JIdExpression(getFileLocation(sCI), convert(binding.getReturnType()), name,
              declaration);
    } else {

      functionName =
          new JIdExpression(getFileLocation(sCI), JClassType.createUnresolvableType(),
              name, declaration);
    }

    JIdExpression idExpression = (JIdExpression) functionName;

    if (idExpression.getDeclaration() != null) {
      // clone idExpression because the declaration in it is wrong
      // (it's the declaration of an equally named variable)
      // TODO this is ugly

      functionName =
          new JIdExpression(idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
    }

    return new JMethodInvocationStatement(getFileLocation(sCI), new JSuperConstructorInvocation(getFileLocation(sCI),
        (JClassType) getDeclaringClassType(binding), functionName, params, declaration));
  }

  private JConstructorType convertConstructorType(IMethodBinding pBinding) {
    Preconditions.checkArgument(pBinding.isConstructor());

    // Constructors can't be declared by Interfaces
    JClassType declaringClass = (JClassType) getDeclaringClassType(pBinding);

    ITypeBinding[] paramBindings = pBinding.getParameterTypes();

    List<JType> paramTypes = new ArrayList<>();

    for (ITypeBinding type : paramBindings) {
      paramTypes.add(convert(type));
    }

    return new JConstructorType(declaringClass, paramTypes, pBinding.isVarargs());
  }

  private JMethodType convertMethodType(IMethodBinding pBinding) {
    Preconditions.checkArgument(!pBinding.isConstructor());

    // Constructors can't be declared by Interfaces
    JClassOrInterfaceType declaringClass = getDeclaringClassType(pBinding);

    ITypeBinding[] paramBindings = pBinding.getParameterTypes();

    List<JType> paramTypes = new ArrayList<>();

    for (ITypeBinding type : paramBindings) {
      paramTypes.add(convert(type));
    }

    return new JMethodType(declaringClass, paramTypes, pBinding.isVarargs());
  }

  /**
   * Converts a  JDT Expression into the intern AST.
   * This method doesn't always return a Side effect free
   * Expression.
   *
   * @param e JDT Expression to be transformed
   * @return Intern AST of given JDT Expression
   */
  public JAstNode convertExpressionWithSideEffects(Expression e) {

    //TODO  All Expression Implementation

    if (e == null) {
      logger.log(Level.FINE, "Expression to convert is null");
      return null;
    }

    switch (e.getNodeType()) {
    case ASTNode.ASSIGNMENT:
      return convert((Assignment) e);
    case ASTNode.INFIX_EXPRESSION:
      return convert((InfixExpression) e);
    case ASTNode.NUMBER_LITERAL:
      return convert((NumberLiteral) e);
    case ASTNode.CHARACTER_LITERAL:
      return convert((CharacterLiteral) e);
    case ASTNode.STRING_LITERAL:
      return convert((StringLiteral) e);
    case ASTNode.NULL_LITERAL:
      return convert((NullLiteral) e);
    case ASTNode.PREFIX_EXPRESSION:
      return convert((PrefixExpression) e);
    case ASTNode.POSTFIX_EXPRESSION:
      return convert((PostfixExpression) e);
    case ASTNode.QUALIFIED_NAME:
      return convert((QualifiedName) e);
    case ASTNode.BOOLEAN_LITERAL:
      return convert((BooleanLiteral) e);
    case ASTNode.FIELD_ACCESS:
      return convert((FieldAccess) e);
    case ASTNode.SIMPLE_NAME:
      return convert((SimpleName) e);
    case ASTNode.PARENTHESIZED_EXPRESSION:
      return convertExpressionWithoutSideEffects(
          ((ParenthesizedExpression) e).getExpression());
    case ASTNode.METHOD_INVOCATION:
      return convert((MethodInvocation) e);
    case ASTNode.CLASS_INSTANCE_CREATION:
      return convert((ClassInstanceCreation) e);
    case ASTNode.ARRAY_ACCESS:
      return convert((ArrayAccess) e);
    case ASTNode.ARRAY_CREATION:
      return convert((ArrayCreation) e);
    case ASTNode.ARRAY_INITIALIZER:
      return convert((ArrayInitializer) e);
    case ASTNode.CONDITIONAL_EXPRESSION:
      return convert((ConditionalExpression) e);
    case ASTNode.THIS_EXPRESSION:
      return convert((ThisExpression) e);
    case ASTNode.INSTANCEOF_EXPRESSION:
      return convert((InstanceofExpression) e);
    case ASTNode.CAST_EXPRESSION:
      return convert((CastExpression) e);
    case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
      return convert((VariableDeclarationExpression) e);
    case ASTNode.SUPER_FIELD_ACCESS :
      return convert(((SuperFieldAccess) e));
    case ASTNode.TYPE_LITERAL :
      return convert((TypeLiteral)e);
    case ASTNode.SUPER_METHOD_INVOCATION :
      return convert((SuperMethodInvocation) e);
    default:
      logger.log(Level.WARNING, "Expression of type " + AstDebugg.getTypeName(e.getNodeType()) + " not implemented");
      return null;
    }
  }

  private JAstNode convert(SuperMethodInvocation e) {

    boolean canBeResolved = e.resolveMethodBinding() != null;

    JClassOrInterfaceType declaringClassType = null;

    if (canBeResolved) {
      declaringClassType = (JClassOrInterfaceType) convert(e.resolveMethodBinding().getDeclaringClass());
      scope.registerClass(e.resolveMethodBinding().getDeclaringClass());
    }

    @SuppressWarnings("unchecked")
    List<Expression> p = e.arguments();

    List<JExpression> params;
    if (p.size() > 0) {
      params = convert(p);
    } else {
      params = Collections.emptyList();
    }

    JExpression methodName = convertExpressionWithoutSideEffects(e.getName());

    JMethodDeclaration declaration = null;

    ModifierBean mb = null;

    if (canBeResolved) {
      mb = ModifierBean.getModifiers(e.resolveMethodBinding());
    }


    if (methodName instanceof JIdExpression) {
      JIdExpression idExpression = (JIdExpression) methodName;
      String name = idExpression.getName();
      declaration = scope.lookupMethod(name);

      if (idExpression.getDeclaration() != null) {
        // TODO this is ugly

        methodName =
            new JIdExpression(idExpression.getFileLocation(),
                idExpression.getExpressionType(), name, declaration);
      }
    }

    if (declaration == null) {

      if (canBeResolved) {
        declaration = scope.createExternMethodDeclaration(
            convertMethodType(e.resolveMethodBinding()),
            methodName.toASTString(),
            e.resolveMethodBinding().getName(),
            VisibilityModifier.PUBLIC, mb.isFinal(), mb.isAbstract(),
            mb.isStatic(), mb.isNative(),
            mb.isSynchronized(), mb.isStrictFp(), declaringClassType);

      } else {
        declaration = JMethodDeclaration.createUnresolvedMethodDeclaration();
      }
    }

      JMethodInvocationExpression miv =
          new JMethodInvocationExpression(getFileLocation(e), convert(e.resolveTypeBinding()), methodName, params, declaration);

      if (canBeResolved) {

        JType type = miv.getDeclaringType();

        if (type instanceof JClassType) {
         miv.setRunTimeBinding((JClassType) type);
        }
      }

      return miv;
  }

  /**
   * @param pE the node to convert
   */
  private JAstNode convert(TypeLiteral pE) {
    throw new CFAGenerationRuntimeException("Standard Library support not yet implemented.\n"
      +  "Cannot use Type Literals which would return a class Object.");
  }



  private JAstNode convert(SuperFieldAccess e) {
    // Only used, when there is no field Access.
    // Meaning only for super.x where x is a field.

    IVariableBinding vb = e.resolveFieldBinding();

    boolean canBeResolved = vb != null;

    if (canBeResolved) {
      return convert(e.getName());
    } else {

      // If SuperFieldAccess cannot be resolved, create
      // a neutral JIDExpression
      FileLocation fileLoc = getFileLocation(e);
      JType type = convert(e.resolveTypeBinding());
      String name = e.getName().getIdentifier();

      return new JIdExpression(fileLoc, type, name, null);
    }
  }

  private JAstNode convert(CastExpression e) {
    return new JCastExpression(getFileLocation(e),
        convert(e.resolveTypeBinding()),
        convertExpressionWithoutSideEffects(e.getExpression()));
  }

  private JIdExpression convert(VariableDeclarationExpression vde) {

    List<JDeclaration> variableDeclarations = new ArrayList<>();

    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> variableDeclarationFragments =
        vde.fragments();

    FileLocation fileLoc = getFileLocation(vde);
    Type type = vde.getType();

    @SuppressWarnings("unchecked")
    ModifierBean mB = ModifierBean.getModifiers(vde.modifiers());

    assert (!mB.isAbstract()) : "Local Variable has abstract modifier?";
    assert (!mB.isNative()) : "Local Variable has native modifier?";
    assert (mB.getVisibility() == VisibilityModifier.NONE) : "Local Variable has Visibility modifier?";
    assert (!mB.isStatic()) : "Local Variable has static modifier?";
    assert (!mB.isStrictFp()) : "Local Variable has strictFp modifier?";
    assert (!mB.isSynchronized()) : "Local Variable has synchronized modifier?";

    for (VariableDeclarationFragment vdf : variableDeclarationFragments) {

      NameAndInitializer nameAndInitializer = getNamesAndInitializer(vdf);

      JVariableDeclaration newD = new JVariableDeclaration(fileLoc,
          convert(type), nameAndInitializer.getName(),
          nameAndInitializer.getName(),
          getQualifiedName(nameAndInitializer.getName()),
          nameAndInitializer.getInitializer(),
          mB.isFinal());

      variableDeclarations.add(newD);
    }

    forInitDeclarations.addAll(variableDeclarations);

    return null;
  }

  private JExpression convert(InstanceofExpression e) {

    FileLocation fileloc = getFileLocation(e);
    JExpression leftOperand = convertExpressionWithoutSideEffects(e.getLeftOperand());
    JType type = convert(e.getRightOperand().resolveBinding());
    assert leftOperand instanceof JIdExpression : "There are other expressions for instanceOf?";
    assert type instanceof JClassOrInterfaceType : "There are other types for this expression?";

    JIdExpression referenceVariable = (JIdExpression) leftOperand;
    JType instanceOfType = convert(e.resolveTypeBinding());

    assert instanceOfType instanceof JSimpleType
        && ((JSimpleType) instanceOfType).getType() == JBasicType.BOOLEAN
        : "InstanceofExpression is not always of type boolean!";

    return createInstanceOfExpression(referenceVariable, (JClassOrInterfaceType) type, fileloc);
  }

  /**
   * Creates an <code>instanceof</code> expression from the given parameters.
   *
   * <p>This creates an expression representing a statement of the following format:<br />
   * <code>pLeftOperand instanceof pClassOrInterfaceType</code>.</p>
   *
   * @param pLeftOperand the left operand of the <code>instanceof</code> statement
   * @param pClassOrInterfaceType the right operand of the <code>instanceof</code> statement. this
   *    has to be a specific class or interface. The resulting expression will be evaluated to
   *    <code>true</code> if the the left operand's type is equal to this type or a subtype of this
   *    type
   * @param pLocation the file location of the expression
   *
   * @return a {@link JExpression} representing an <code>instanceof</code> expression with the given
   *    parameters
   */
  private JExpression createInstanceOfExpression(JIdExpression pLeftOperand,
      JClassOrInterfaceType pClassOrInterfaceType, FileLocation pLocation) {

    List<JClassType> allPossibleClasses = getSubClasses(pClassOrInterfaceType);

    if (pClassOrInterfaceType instanceof JInterfaceType) {
      // if the given interface has no implementing classes there's no way the expression will be
      // true
      if (allPossibleClasses.isEmpty()) {
        return new JBooleanLiteralExpression(pLocation, false);
      }
    }

    return createInstanceOfDisjunction(pLeftOperand, allPossibleClasses,
        JSimpleType.getBoolean(), pLocation);
  }

  /**
   * Returns all sub classes/implementing classes of the given class or interface.
   * This includes the given type itself, if it is a {@link JClassType}.
   *
   * @param pType the class or interface type to get all subclasses of
   * @return all sub classes/implementing classes of the given class or interface.
   */
  private List<JClassType> getSubClasses(JClassOrInterfaceType pType) {
    Set<JClassType> subClassTypeSet;

    assert pType instanceof JInterfaceType || pType instanceof JClassType
        : "Unhandled type " + pType;

    if (pType instanceof JInterfaceType) {
      subClassTypeSet = ((JInterfaceType) pType).getAllKnownImplementingClassesOfInterface();

    } else {
      JClassType classType = (JClassType) pType;

      subClassTypeSet = classType.getAllSubTypesOfClass();
      subClassTypeSet.add(classType);
    }

    return Lists.newArrayList(subClassTypeSet);
  }

  private JExpression createInstanceOfDisjunction(JIdExpression pLeftOperand,
      List<JClassType> pConcreteClassTypes,
      JType pExpressionType,
      FileLocation pLocation) {

    JExpression currentCondition = convertClassRunTimeCompileTimeAccord(pLocation,
        pLeftOperand, pConcreteClassTypes.remove(FIRST));

    JRunTimeTypeEqualsType newCondition;

    for (JClassType currentSubType : pConcreteClassTypes) {
      newCondition = convertClassRunTimeCompileTimeAccord(pLocation, pLeftOperand, currentSubType);
      currentCondition =
          new JBinaryExpression(pLocation, pExpressionType, currentCondition, newCondition,
              BinaryOperator.CONDITIONAL_OR);
    }

    return currentCondition;
  }

  private JRunTimeTypeEqualsType convertClassRunTimeCompileTimeAccord(
      FileLocation pFileloc, JIdExpression pDeclaration, JClassOrInterfaceType pClassType) {

    JRunTimeTypeExpression runTimeTyp = new JVariableRunTimeType(pFileloc, pDeclaration);

    return new JRunTimeTypeEqualsType(pFileloc, runTimeTyp, pClassType);
  }


  private JAstNode convert(ThisExpression e) {
    return new JThisExpression(getFileLocation(e),
        (JClassOrInterfaceType) convert(e.resolveTypeBinding()));
  }

  private JAstNode convert(FieldAccess e) {

    if (isArrayLengthExpression(e)) {
      return createJArrayLengthExpression(e);
    }

    IVariableBinding fieldBinding = e.resolveFieldBinding();

    boolean canBeResolved = fieldBinding != null;

    if (canBeResolved) {
      scope.registerClass(fieldBinding.getDeclaringClass());
    }

    JAstNode identifier = convertExpressionWithoutSideEffects(e.getName());

    if (!(identifier instanceof JIdExpression)) {
      throw new CFAGenerationRuntimeException(
        "Identifier of FieldAccess could not be processed.", e);
    }

    JIdExpression idExpIdentifier = (JIdExpression) identifier;

    JAstNode qualifier = convertExpressionWithoutSideEffects(e.getExpression());

    if (qualifier instanceof JThisExpression) {
      // If only qualifier is this, we don't need
      // a JFieldAccess. It can already be identified
      // by its declaration JFieldDeclaration of idExpression
      return idExpIdentifier;
    }

    if (!(qualifier instanceof JIdExpression)) {
      throw new CFAGenerationRuntimeException(
        "Qualifier of FieldAccess could not be processed.", e);
    }

    JSimpleDeclaration decl = idExpIdentifier.getDeclaration();


    if (!(decl instanceof JFieldDeclaration)) {
      throw new CFAGenerationRuntimeException(
        "Identifier of FieldAccess does not identify a field.", e);
    }

    return new JFieldAccess(idExpIdentifier.getFileLocation(),
        idExpIdentifier.getExpressionType(),
        idExpIdentifier.getName(), (JFieldDeclaration) decl,
        (JIdExpression) qualifier);
  }

  private boolean isArrayLengthExpression(FieldAccess e) {
    return e.getExpression() instanceof ArrayAccess;
  }

  private JArrayLengthExpression createJArrayLengthExpression(FieldAccess e) {
    JExpression qualifierExpression = convertExpressionWithoutSideEffects(e.getExpression());

    return JArrayLengthExpression.getInstance(qualifierExpression, getFileLocation(e));
  }

  private JAstNode convert(ClassInstanceCreation cIC) {

    IMethodBinding binding = cIC.resolveConstructorBinding();

    boolean canBeResolved = binding != null;
    final AnonymousClassDeclaration anonymousDeclaration = cIC.getAnonymousClassDeclaration();

    if (anonymousDeclaration != null) {
      scope.addAnonymousClassDeclaration(anonymousDeclaration);
    }

    if (canBeResolved) {
      scope.registerClass(binding.getDeclaringClass());
    }

    List<JExpression> params = getParameterExpressions(cIC);

    String name = getFullName(cIC);

    JConstructorDeclaration declaration = getConstructorDeclaration(cIC);

    JExpression functionName =
        new JIdExpression(getFileLocation(cIC), convert(cIC.resolveTypeBinding()), name, declaration);
    /*JIdExpression idExpression = (JIdExpression) functionName;


    if (idExpression.getDeclaration() != null) {
      // clone idExpression because the declaration in it is wrong
      // (it's the declaration of an equally named variable)
      // TODO this is ugly

      functionName =
          new JIdExpression(idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
    }*/

    assert declaration.getParameters().size() == params.size();
    return new JClassInstanceCreation(getFileLocation(cIC),
                                      declaration.getType().getReturnType(),
                                      functionName,
                                      params,
                                      declaration);
  }

  private JConstructorDeclaration getConstructorDeclaration(ClassInstanceCreation pCIC) {
    final AnonymousClassDeclaration anonymousDecl = pCIC.getAnonymousClassDeclaration();
    String fullName = getFullName(pCIC);
    JConstructorDeclaration declaration;

    if (anonymousDecl == null) {
      declaration = (JConstructorDeclaration) scope.lookupMethod(fullName);

    } else {
      declaration = getConstructorOfAnonymousClass(anonymousDecl, pCIC.resolveConstructorBinding());
    }

    if (declaration == null) {
      declaration = getDefaultConstructor(pCIC);
    }

    return declaration;
  }

  private JConstructorDeclaration getDefaultConstructor(ClassInstanceCreation pCIC) {
    String fullName = getFullName(pCIC);
    String simpleName = getSimpleName(pCIC);
    final IMethodBinding constructorBinding = pCIC.resolveConstructorBinding();

    if (constructorBinding != null) {
      final ModifierBean mb = ModifierBean.getModifiers(constructorBinding);

      return new JConstructorDeclaration(getFileLocation(pCIC),
                                         convertConstructorType(constructorBinding),
                                         fullName,
                                         simpleName,
                                         Collections.<JParameterDeclaration>emptyList(),
                                         mb.getVisibility(),
                                         mb.isStrictFp(),
                                         getDeclaringClassType(constructorBinding));

    } else {
      return new JConstructorDeclaration(getFileLocation(pCIC),
                                         JConstructorType.createUnresolvableConstructorType(),
                                         fullName,
                                         simpleName,
                                         Collections.<JParameterDeclaration>emptyList(),
                                         VisibilityModifier.NONE,
                                         false,
                                         JClassType.createUnresolvableType());
    }
  }

  private JConstructorDeclaration getConstructorOfAnonymousClass(
      AnonymousClassDeclaration pAnonymousDecl, IMethodBinding pInstanceCreation) {

    final ITypeBinding anonymousDeclBinding = pAnonymousDecl.resolveBinding();

    if (anonymousDeclBinding != null) {

      final FileLocation fileLoc = getFileLocation(pAnonymousDecl);
      final boolean takesVarArgs = false;
      final boolean isStrictFP = false;

      final JClassOrInterfaceType returnType = convertClassOrInterfaceType(anonymousDeclBinding);

      List<JParameterDeclaration> parameterDeclarations;
      List<JType> parameterTypes;

      // no instance creation exists if direct super type is an interface
      if (pInstanceCreation != null) {
        parameterDeclarations =
            getParameterDeclarations(pInstanceCreation.getParameterTypes(), fileLoc);

        parameterTypes = new ArrayList<>(parameterDeclarations.size());

      } else {
        parameterDeclarations = Collections.emptyList();
        parameterTypes = Collections.emptyList();
      }

      for (JParameterDeclaration d : parameterDeclarations) {
        parameterTypes.add(d.getType());
      }

      final JConstructorType constructorType = new JConstructorType(returnType,
                                                                    parameterTypes,
                                                                    takesVarArgs);

      final String fullName =
          NameConverter.convertAnonymousClassConstructorName(anonymousDeclBinding, parameterTypes);
      final JClassOrInterfaceType declaringClass =
          convertClassOrInterfaceType(anonymousDeclBinding.getDeclaringClass());

      return new JConstructorDeclaration(fileLoc,
                                         constructorType,
                                         fullName,
                                         fullName,
                                         parameterDeclarations,
                                         VisibilityModifier.PRIVATE,
                                         isStrictFP,
                                         declaringClass);
    } else {
      logger.logf(Level.WARNING, "Binding for anonymous class %s can't be resolved.",
          pAnonymousDecl.toString());
      return null;
    }
  }

  private List<JParameterDeclaration> getParameterDeclarations(
      ITypeBinding[] pBindings, FileLocation pFileLoc) {

    List<JParameterDeclaration> declarations = new ArrayList<>(pBindings.length);

    for (ITypeBinding binding : pBindings) {
      declarations.add(getParameterDeclaration(binding, pFileLoc));
    }

    return declarations;
  }

  private JParameterDeclaration getParameterDeclaration(
      ITypeBinding pBinding, FileLocation pFileLoc) {

    final JType parameterType = convert(pBinding);
    final String simpleName = pBinding.getName();
    final String qualifiedName = pBinding.getQualifiedName();

    // TODO: Currently no parameter is handled as final. Find out how to find out whether a
    // parameter is final and add it instead of 'false'.
    return new JParameterDeclaration(pFileLoc, parameterType, simpleName, qualifiedName, false);
  }

  private String getFullName(ClassInstanceCreation pCIC) {
    IMethodBinding binding = pCIC.resolveConstructorBinding();

    if (binding != null) {
      return NameConverter.convertName(binding);

    } else {
      AnonymousClassDeclaration anonymousDeclaration = pCIC.getAnonymousClassDeclaration();

      if (anonymousDeclaration != null) {
        return NameConverter.convertClassOrInterfaceToFullName(
            anonymousDeclaration.resolveBinding());

      } else {
        return pCIC.toString();
      }
    }
  }

  private String getSimpleName(ClassInstanceCreation pCIC) {
    IMethodBinding binding = pCIC.resolveConstructorBinding();

    if (binding != null) {
      return getSimpleName(binding.getDeclaringClass());

    } else {
      AnonymousClassDeclaration anonymousDeclaration = pCIC.getAnonymousClassDeclaration();

      if (anonymousDeclaration != null) {
        return getSimpleName(anonymousDeclaration.resolveBinding());

      } else {
        return pCIC.toString();
      }
    }
  }

  private String getSimpleName(ITypeBinding pBinding) {
    String name = pBinding.getName();

    if (name.isEmpty()) {
      name = NameConverter.convertClassOrInterfaceToFullName(pBinding);
    }

    return name;
  }

  private List<JExpression> getParameterExpressions(ClassInstanceCreation pCIC) {
    @SuppressWarnings("unchecked")
    List<Expression> p = pCIC.arguments();

    List<JExpression> params;

    if (p.size() > 0) {
      params = convert(p);

    } else {
      params = Collections.emptyList();
    }

    return params;
  }


  private JAstNode convert(ConditionalExpression e) {
    JIdExpression tmp = createTemporaryVariable(e);
    conditionalTemporaryVariable = tmp;
    conditionalExpression = e;
    return tmp;
  }

  private JAstNode convert(ArrayInitializer initializer) {

    if (initializer == null) {
      logger.log(Level.FINE, "Array initializer to convert is null");
      return null;
    }

    JArrayType type = (JArrayType) convert(initializer.resolveTypeBinding());

    List<JExpression> initializerExpressions = new ArrayList<>();

    @SuppressWarnings("unchecked")
    List<Expression> expressions = initializer.expressions();

    for (Expression exp : expressions) {
      initializerExpressions.add(convertExpressionWithoutSideEffects(exp));
    }


    return new JArrayInitializer(getFileLocation(initializer),
        initializerExpressions, type);
  }

  private JAstNode convert(ArrayCreation Ace) {

    FileLocation fileloc = getFileLocation(Ace);
    JArrayInitializer initializer =
        (JArrayInitializer) convertExpressionWithoutSideEffects(
            Ace.getInitializer());

    JArrayType type = convert(Ace.getType());
    List<JExpression> length = new ArrayList<>(type.getDimensions());



    @SuppressWarnings("unchecked")
    List<Expression> dim = Ace.dimensions();

    if (initializer != null) {
      for (int dimension = 0; dimension < type.getDimensions(); dimension++) {
        length.add(new JIntegerLiteralExpression(fileloc, BigInteger.valueOf(dimension)));
      }
    } else {
      for (Expression exp : dim) {
        length.add(convertExpressionWithoutSideEffects(exp));
      }
    }
    return new JArrayCreationExpression(fileloc,
        type, initializer, length);
  }

  private JAstNode convert(ArrayAccess e) {

    JExpression subscriptExpression = convertExpressionWithoutSideEffects(e.getArray());
    JExpression index = convertExpressionWithoutSideEffects(e.getIndex());

    assert subscriptExpression != null;
    assert index != null;

    return new JArraySubscriptExpression(getFileLocation(e), convert(e.resolveTypeBinding()), subscriptExpression,
        index);
  }


  private JAstNode convert(QualifiedName e) {

    if (isArrayLengthExpression(e)) {
      return createJArrayLengthExpression(e);
    }

    IBinding binding = e.resolveBinding();

    boolean canBeResolved = binding != null;

    if (canBeResolved) {

      if (binding instanceof IMethodBinding) {

        String name = NameConverter.convertName((IMethodBinding) e.resolveBinding());
        return new JIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, null);

      } else if (binding instanceof IVariableBinding) {

        IVariableBinding vb = (IVariableBinding) binding;

        if (vb.isEnumConstant()) {
          //TODO Prototype for enum constant expression, investigate
          return new JEnumConstantExpression(getFileLocation(e), (JClassType) convert(e.resolveTypeBinding()),
              NameConverter.convertName((IVariableBinding) e.resolveBinding()));
        }

        return convertQualifiedVariableIdentificationExpression(e);
      } else {

        String name = e.getFullyQualifiedName();
        return new JIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, null);
      }
    } else {

      String name = e.getFullyQualifiedName();
      return new JIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, null);

    }
  }

  private boolean isArrayLengthExpression(QualifiedName e) {
    JExpression qualifierExpression = convertExpressionWithoutSideEffects(e.getQualifier());
    final String lengthExpression = "length";
    final IBinding lengthBinding = e.resolveBinding();

    return (lengthBinding != null && lengthBinding.getName().equals(lengthExpression)
            && isArrayType(qualifierExpression.getExpressionType()))
        || isMainArgumentArray(e, qualifierExpression);
  }

  private boolean isMainArgumentArray(QualifiedName e, JExpression qualifierExpression) {
    final IBinding lengthBinding = e.resolveBinding();

    if (qualifierExpression instanceof JIdExpression) {
      JSimpleDeclaration qualifierDecl = ((JIdExpression) qualifierExpression).getDeclaration();

      // check that no binding exists (special case for main argument array) and that
      // the given qualifier is an array and a parameter
      return lengthBinding == null && isArrayType(qualifierDecl.getType()) && qualifierDecl instanceof JParameterDeclaration;

    } else {
      return false;
    }

  }

  private JArrayLengthExpression createJArrayLengthExpression(QualifiedName e) {
    JExpression qualifierExpression = convertExpressionWithoutSideEffects(e.getQualifier());

    return JArrayLengthExpression.getInstance(qualifierExpression, getFileLocation(e));
  }

  private boolean isArrayType(JType pType) {
    return pType instanceof JArrayType;
  }

  private JAstNode convertQualifiedVariableIdentificationExpression(
      QualifiedName e) {

    JAstNode identifier = convertExpressionWithoutSideEffects(e.getName());

    if (!(identifier instanceof JIdExpression)) {
      throw new CFAGenerationRuntimeException(
        "Identifier of FieldAccess could not be processed.", e);
    }

    JIdExpression idExpIdentifier = (JIdExpression) identifier;

    JAstNode qualifier = convertExpressionWithoutSideEffects(e.getQualifier());

    if (qualifier instanceof JThisExpression) {
      // If only qualifier is this, we don't need
      // a JFieldAccess. It can already be identified
      // by its declaration JFieldDeclaration of idExpression
      return idExpIdentifier;
    }

    if (!(qualifier instanceof JIdExpression)) {
      throw new CFAGenerationRuntimeException(
        "Qualifier of FieldAccess could not be processed.", e);
    }

    JSimpleDeclaration decl = idExpIdentifier.getDeclaration();


    if (!(decl instanceof JFieldDeclaration)) {
      throw new CFAGenerationRuntimeException(
        "Identifier of FieldAccess does not identify a field.", e);
    }



    return new JFieldAccess(idExpIdentifier.getFileLocation(),
        idExpIdentifier.getExpressionType(),
        idExpIdentifier.getName(), (JFieldDeclaration) decl,
        (JIdExpression) qualifier);
  }

  /**
   * Transforms the method a method calls. This is used to solve dynamic Binding.
   *
   *
   * @param newFunctionEntryNode The EntryNode of the new Method Call
   * @param oldMethodCall  static bound call of the old Expression
   * @return a Method Call which calling the method represented by newFunctionEntryNode
   */
  public JMethodInvocationExpression convert(FunctionEntryNode newFunctionEntryNode,
      JMethodInvocationExpression oldMethodCall) {

    JMethodDeclaration declaration = (JMethodDeclaration) newFunctionEntryNode.getFunctionDefinition();

    String name = newFunctionEntryNode.getFunctionName();

    JIdExpression methodName =
        new JIdExpression(oldMethodCall.getFileLocation(), JSimpleType.getUnspecified(), name, declaration);

    if (oldMethodCall instanceof JReferencedMethodInvocationExpression) {
      return new JReferencedMethodInvocationExpression(oldMethodCall.getFileLocation(),
          oldMethodCall.getExpressionType(), methodName, oldMethodCall.getParameterExpressions(), declaration,
          ((JReferencedMethodInvocationExpression) oldMethodCall).getReferencedVariable());
    } else {
      return new JMethodInvocationExpression(oldMethodCall.getFileLocation(), oldMethodCall.getExpressionType(),
          methodName, oldMethodCall.getParameterExpressions(), declaration);
    }
  }


  private JAstNode convert(MethodInvocation mi) {

    IMethodBinding methodBinding = mi.resolveMethodBinding();
    boolean canBeResolved = methodBinding != null;

    JClassOrInterfaceType declaringClassType = null;

    if (canBeResolved) {
      ITypeBinding declaringClass = methodBinding.getDeclaringClass();
      declaringClassType = (JClassOrInterfaceType) convert(declaringClass);
      scope.registerClass(declaringClass);
    }

    @SuppressWarnings("unchecked")
    List<Expression> p = mi.arguments();

    List<JExpression> params;
    if (p.size() > 0) {
      params = convert(p);
    } else {
      params = Collections.emptyList();
    }

    JExpression methodName = convertExpressionWithoutSideEffects(mi.getName());

    JMethodDeclaration declaration = null;
    JExpression referencedVariableName = null;

    ModifierBean mb = null;

    if (canBeResolved) {
      mb = ModifierBean.getModifiers(methodBinding);

      if (!mb.isStatic) {
        referencedVariableName = convertExpressionWithoutSideEffects(mi.getExpression());
      }
    }


    if (methodName instanceof JIdExpression) {
      JIdExpression idExpression = (JIdExpression) methodName;
      String name = idExpression.getName();
      declaration = scope.lookupMethod(name);

      if (idExpression.getDeclaration() != null) {
        // TODO this is ugly

        methodName =
            new JIdExpression(idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
      }
    }

    if (declaration == null) {

      if (canBeResolved) {
        declaration = scope.createExternMethodDeclaration(
            convertMethodType(methodBinding),
            methodName.toASTString(),
            methodBinding.getName(),
            VisibilityModifier.PUBLIC,
            mb.isFinal(), mb.isAbstract(), mb.isStatic(), mb.isNative(),
            mb.isSynchronized(), mb.isStrictFp(), declaringClassType);

      } else {
        declaration = JMethodDeclaration.createUnresolvedMethodDeclaration();
      }
    }

    if (!(referencedVariableName instanceof JIdExpression)) {
      return new JMethodInvocationExpression(getFileLocation(mi), convert(mi.resolveTypeBinding()), methodName, params,
          declaration);
    } else {
      return new JReferencedMethodInvocationExpression(getFileLocation(mi), convert(mi.resolveTypeBinding()),
          methodName, params, declaration, (JIdExpression) referencedVariableName);
    }
  }

  private List<JExpression> convert(List<Expression> el) {

    List<JExpression> result = new ArrayList<>(el.size());
    for (Expression expression : el) {
      result.add(convertExpressionWithoutSideEffects(expression));
    }
    return result;
  }

  private JAstNode convert(SimpleName e) {

    String name = null;
    JSimpleDeclaration declaration = null;

    IBinding binding = e.resolveBinding();
    boolean canBeResolved = binding != null;

    //TODO Complete declaration by finding all Bindings
    if (canBeResolved) {
      if (binding instanceof IVariableBinding) {
        return convertSimpleVariable(e, (IVariableBinding) binding);

      } else if (binding instanceof IMethodBinding) {
        name = NameConverter.convertName((IMethodBinding) binding);
        declaration = scope.lookupMethod(name);
      } else if (binding instanceof ITypeBinding) {
        name = e.getIdentifier();
      }

    } else {
      name = e.getIdentifier();
    }

    assert name != null;


    return new JIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, declaration);
  }

  private JAstNode convertSimpleVariable(SimpleName e, IVariableBinding vb) {

    if (((IVariableBinding) e.resolveBinding()).isEnumConstant()) {
      //TODO Prototype for enum constant expression, investigate
      return new JEnumConstantExpression(getFileLocation(e),
          (JClassType) convert(e.resolveTypeBinding()),
          NameConverter.convertName((IVariableBinding) e.resolveBinding()));
    }

    String name = NameConverter.convertName(vb);

    JSimpleDeclaration declaration = scope.lookupVariable(name);

    if (declaration == null) {
      declaration = createVariableDeclarationFromBinding(e, vb);
    }

    assert name.equals(declaration.getName()) :
      "Created a false declaration for " + e.toString();

    JType type = convert(e.resolveTypeBinding());

    return new JIdExpression(getFileLocation(e), type, name, declaration);
  }

  private JSimpleDeclaration createVariableDeclarationFromBinding
      (SimpleName e, IVariableBinding vb) {

    if (!vb.isField()) {
      throw new CFAGenerationRuntimeException("Declaration of Variable "
        + e.getIdentifier() + " not found.", e);
    }

    String name = NameConverter.convertName(vb);
    String simpleName = vb.getName();

    JFieldDeclaration decl;

    ModifierBean mb = ModifierBean.getModifiers(vb.getModifiers());
    JType type = convert(e.resolveTypeBinding());

    decl = scope.createExternFieldDeclaration(type, name, simpleName,
        mb.isFinal(), mb.isStatic(), mb.getVisibility(),
        mb.isVolatile(), mb.isTransient());

    return decl;
  }

  private JAstNode convert(Assignment e) {

    FileLocation fileLoc = getFileLocation(e);
    JType type = convert(e.resolveTypeBinding());
    JLeftHandSide leftHandSide =
        (JLeftHandSide) convertExpressionWithoutSideEffects(e.getLeftHandSide());

    BinaryOperator op = convert(e.getOperator(), leftHandSide.getExpressionType());

    if (op == null) {
      // a = b
      JAstNode rightHandSide =
          convertExpressionWithSideEffects(e.getRightHandSide()); // right-hand side may have a method call

      if (rightHandSide instanceof JExpression) {
        // a = b
        return new JExpressionAssignmentStatement
            (fileLoc, leftHandSide, (JExpression) rightHandSide);

      } else if (rightHandSide instanceof JMethodInvocationExpression) {
        // a = f()
        return new JMethodInvocationAssignmentStatement(fileLoc, leftHandSide,
            (JMethodInvocationExpression) rightHandSide);

      } else if (rightHandSide instanceof JAssignment) {

        // TODO We need the assignments to be evaluated from left to right
        // e. g x = 1;  x = ++x + x; x is 4; x = x + ++x; x is 3
        preSideAssignments.add(rightHandSide);

        return new JExpressionAssignmentStatement(fileLoc, leftHandSide,
            ((JAssignment) rightHandSide).getLeftHandSide());

      } else {
        throw new CFAGenerationRuntimeException("Expression is not free of side-effects");
      }

    } else {
      // a += b etc.
      JExpression rightHandSide = convertExpressionWithoutSideEffects(e.getRightHandSide());

      // first create expression "a + b"
      JBinaryExpression exp = new JBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

      // and now the assignment
      return new JExpressionAssignmentStatement(fileLoc, leftHandSide, exp);
    }
  }

  // pType is the type of the operands of the operation
  private BinaryOperator convert(Assignment.Operator op, JType type) {
    // will be used if the type doesn't fit the operator
    final String invalidTypeMsg = "Invalid type '" + type + "' for assignment with binary operation.";

    JBasicType basicType;

    if (type instanceof JSimpleType) {
      basicType = ((JSimpleType) type).getType();
    } else {
      basicType = null;
    }

    if (op.equals(Assignment.Operator.ASSIGN)) {
      return null;

    } else if (basicType != null) {
      switch (basicType) {
      case BOOLEAN:
        return convertBooleanOperator(op); // might throw CFAGenerationRuntimeException

      case BYTE:
      case SHORT:
      case INT:
      case LONG:
      case DOUBLE:
      case FLOAT:
        return convertNumberOperator(op); // might throw CFAGenerationRuntimeException

      default:
        throw new CFAGenerationRuntimeException(invalidTypeMsg);
      }

    } else {
      throw new CFAGenerationRuntimeException(invalidTypeMsg);
    }
  }

  private BinaryOperator convertBooleanOperator(Assignment.Operator op) {
    if (op.equals(Assignment.Operator.BIT_AND_ASSIGN)) {
      return BinaryOperator.LOGICAL_AND;
    } else if (op.equals(Assignment.Operator.BIT_OR_ASSIGN)) {
      return BinaryOperator.LOGICAL_OR;
    } else if (op.equals(Assignment.Operator.BIT_XOR_ASSIGN)) {
      return BinaryOperator.LOGICAL_XOR;

    } else {
      throw new CFAGenerationRuntimeException("Invalid operator " + op
          + " for boolean assignment");
    }
  }

  private BinaryOperator convertNumberOperator(Assignment.Operator op) {
    if (op.equals(Assignment.Operator.BIT_AND_ASSIGN)) {
      return BinaryOperator.BINARY_AND;
    } else if (op.equals(Assignment.Operator.BIT_OR_ASSIGN)) {
      return BinaryOperator.BINARY_OR;
    } else if (op.equals(Assignment.Operator.BIT_XOR_ASSIGN)) {
      return BinaryOperator.BINARY_XOR;
    } else if (op.equals(Assignment.Operator.DIVIDE_ASSIGN)) {
      return BinaryOperator.DIVIDE;
    } else if (op.equals(Assignment.Operator.LEFT_SHIFT_ASSIGN)) {
      return BinaryOperator.SHIFT_LEFT;
    } else if (op.equals(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN)) {
      return BinaryOperator.SHIFT_RIGHT_SIGNED;
    } else if (op.equals(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN)) {
      return BinaryOperator.SHIFT_RIGHT_UNSIGNED;
    } else if (op.equals(Assignment.Operator.MINUS_ASSIGN)) {
      return BinaryOperator.MINUS;
    } else if (op.equals(Assignment.Operator.PLUS_ASSIGN)) {
      return BinaryOperator.PLUS;
    } else if (op.equals(Assignment.Operator.REMAINDER_ASSIGN)) {
      return BinaryOperator.MODULO;
    } else if (op.equals(Assignment.Operator.TIMES_ASSIGN)) {
      return BinaryOperator.MULTIPLY;

    } else {
      throw new CFAGenerationRuntimeException("Invalid operator " + op
          + " for number assignment.");
    }
  }

  private JExpression convert(BooleanLiteral e) {
    return new JBooleanLiteralExpression(
        getFileLocation(e), e.booleanValue());
  }


  private JAstNode convert(PrefixExpression e) {

    PrefixExpression.Operator op = e.getOperator();

    if (op.equals(PrefixExpression.Operator.INCREMENT)
        || op.equals(PrefixExpression.Operator.DECREMENT)) {

      return handlePreFixIncOrDec(e, op);
    }

    JExpression operand = convertExpressionWithoutSideEffects(e.getOperand());
    FileLocation fileLoc = getFileLocation(e);


    return new JUnaryExpression(fileLoc, convert(e.resolveTypeBinding()),
                                            operand, convert(op));
  }

  private JAstNode convert(PostfixExpression e) {
    PostfixExpression.Operator op = e.getOperator();
    return handlePostFixIncOrDec(e, op);
  }


  private JAstNode handlePostFixIncOrDec(PostfixExpression e,
                                PostfixExpression.Operator op) {

    BinaryOperator postOp = null;
    if (op.equals(PostfixExpression.Operator.INCREMENT)) {

      postOp = BinaryOperator.PLUS;
    } else if (op.equals(PostfixExpression.Operator.DECREMENT)) {
      postOp = BinaryOperator.MINUS;
    }

    assert postOp != null : "Increment/Decrement Severe Error.";

    FileLocation fileLoc = getFileLocation(e);
    JType type = convert(e.resolveTypeBinding());
    JLeftHandSide operand = (JLeftHandSide) convertExpressionWithoutSideEffects(e.getOperand());

    JExpression preOne = new JIntegerLiteralExpression(fileLoc, BigInteger.ONE);
    JBinaryExpression preExp =
        new JBinaryExpression(fileLoc, type, operand, preOne, postOp);

    return new JExpressionAssignmentStatement(fileLoc, operand, preExp);
  }


  private JAstNode handlePreFixIncOrDec(PrefixExpression e, Operator op) {

    BinaryOperator preOp = null;
    if (op.equals(PrefixExpression.Operator.INCREMENT)) {

      preOp = BinaryOperator.PLUS;
    } else if (op.equals(PrefixExpression.Operator.DECREMENT)) {
      preOp = BinaryOperator.MINUS;
    }

    FileLocation fileLoc = getFileLocation(e);
    JType type = convert(e.resolveTypeBinding());
    JLeftHandSide operand = (JLeftHandSide) convertExpressionWithoutSideEffects(e.getOperand());

    JExpression preOne = new JIntegerLiteralExpression(fileLoc, BigInteger.ONE);
    JBinaryExpression preExp = new JBinaryExpression(fileLoc, type,
        operand, preOne, preOp);
    return new JExpressionAssignmentStatement(fileLoc, operand, preExp);
  }




  private UnaryOperator convert(PrefixExpression.Operator op) {

    if (op.equals(PrefixExpression.Operator.NOT)) {
      return UnaryOperator.NOT;
    } else if (op.equals(PrefixExpression.Operator.PLUS)) {
      return UnaryOperator.PLUS;
    } else if (op.equals(PrefixExpression.Operator.COMPLEMENT)) {
      return UnaryOperator.COMPLEMENT;
    } else if (op.equals(PrefixExpression.Operator.MINUS)) {
      return UnaryOperator.MINUS;
    } else {
      throw new CFAGenerationRuntimeException(
          "Could not proccess Operator:"  + op.toString() + ".");
    }
  }


  private JExpression convert(InfixExpression e) {
    FileLocation fileLoc = getFileLocation(e);
    JType type = convert(e.resolveTypeBinding());
    JExpression leftHandSide = convertExpressionWithoutSideEffects(e.getLeftOperand());
    assert leftHandSide != null;

    JExpression rightHandSide = convertExpressionWithoutSideEffects(e.getRightOperand());
    assert rightHandSide != null;

    final JType leftHandType = leftHandSide.getExpressionType();
    final JType rightHandType = rightHandSide.getExpressionType();
    BinaryOperator op = convert(e.getOperator(), leftHandType, rightHandType);

    JExpression binaryExpression = new JBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

    // a x b x c x d is being translated to (((a x b) x c) x d)
    if (e.hasExtendedOperands()) {

      @SuppressWarnings("unchecked")
      List<Expression> extOperands = e.extendedOperands();

      for (Expression extendedOperand : extOperands) {
        binaryExpression = new JBinaryExpression(fileLoc, type, binaryExpression,
            convertExpressionWithoutSideEffects(extendedOperand), op);
      }
    }

    return binaryExpression;
  }

  // pType is the type of the operands of the operation
  private BinaryOperator convert(InfixExpression.Operator op, JType pOp1Type, JType pOp2Type) {
    final String invalidTypeMsg = "Invalid operation '" + pOp1Type + " " + op + " " + pOp2Type + "'";
    JBasicType basicTypeOp1 = null;
    JBasicType basicTypeOp2 = null;

    if (pOp1Type instanceof JSimpleType) {
      basicTypeOp1 = ((JSimpleType) pOp1Type).getType();
    }

    if (pOp2Type instanceof JSimpleType) {
      basicTypeOp2 = ((JSimpleType) pOp2Type).getType();
    }

    if (basicTypeOp1 == null || basicTypeOp2 == null) {
      if (op.equals(InfixExpression.Operator.EQUALS)) {
        return BinaryOperator.EQUALS;
      } else if (op.equals(InfixExpression.Operator.NOT_EQUALS)) {
        return BinaryOperator.NOT_EQUALS;
      } else if (op.equals(InfixExpression.Operator.PLUS)
          && (isStringType(pOp1Type) || isStringType(pOp2Type))) {
        return BinaryOperator.STRING_CONCATENATION;
      } else {
        throw new CFAGenerationRuntimeException(invalidTypeMsg);
      }
    } else if (isNumericCompatible(basicTypeOp1) && isNumericCompatible(basicTypeOp2)) {
      return convertNumericOperator(op);
    } else if (isBooleanCompatible(basicTypeOp1) && isBooleanCompatible(basicTypeOp2)) {
      return convertBooleanOperator(op);
    } else {
      throw new CFAGenerationRuntimeException(invalidTypeMsg);
    }
  }

  private boolean isNumericCompatible(JBasicType pType) {
    return pType != null
        && (pType.isIntegerType() || pType.isFloatingPointType() || pType == JBasicType.UNSPECIFIED);
  }

  private boolean isBooleanCompatible(JBasicType pType) {
    return pType == JBasicType.BOOLEAN || pType == JBasicType.UNSPECIFIED;

  }

  private boolean isStringType(JType t) {
    return t instanceof JClassOrInterfaceType
        && ((JClassOrInterfaceType)t).getName().equals("java.lang.String");
  }

  private BinaryOperator convertNumericOperator(InfixExpression.Operator op) {
    if (op.equals(InfixExpression.Operator.PLUS)) {
      return BinaryOperator.PLUS;
    } else if (op.equals(InfixExpression.Operator.MINUS)) {
      return BinaryOperator.MINUS;
    } else if (op.equals(InfixExpression.Operator.DIVIDE)) {
      return BinaryOperator.DIVIDE;
    } else if (op.equals(InfixExpression.Operator.TIMES)) {
      return BinaryOperator.MULTIPLY;
    } else if (op.equals(InfixExpression.Operator.REMAINDER)) {
      return BinaryOperator.MODULO;
    } else if (op.equals(InfixExpression.Operator.GREATER)) {
      return BinaryOperator.GREATER_THAN;
    } else if (op.equals(InfixExpression.Operator.LESS)) {
      return BinaryOperator.LESS_THAN;
    } else if (op.equals(InfixExpression.Operator.GREATER_EQUALS)) {
      return BinaryOperator.GREATER_EQUAL;
    } else if (op.equals(InfixExpression.Operator.LESS_EQUALS)) {
      return BinaryOperator.LESS_EQUAL;
    } else if (op.equals(InfixExpression.Operator.LEFT_SHIFT)) {
      return BinaryOperator.SHIFT_LEFT;
    } else if (op.equals(InfixExpression.Operator.RIGHT_SHIFT_SIGNED)) {
      return BinaryOperator.SHIFT_RIGHT_SIGNED;
    } else if (op.equals(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED)) {
      return BinaryOperator.SHIFT_RIGHT_UNSIGNED;
    } else if (op.equals(InfixExpression.Operator.NOT_EQUALS)) {
      return BinaryOperator.NOT_EQUALS;
    } else if (op.equals(InfixExpression.Operator.EQUALS)) {
      return BinaryOperator.EQUALS;
    } else if (op.equals(InfixExpression.Operator.AND)) {
        return BinaryOperator.BINARY_AND;
    } else if (op.equals(InfixExpression.Operator.OR)) {
        return BinaryOperator.BINARY_OR;
    } else if (op.equals(InfixExpression.Operator.XOR)) {
        return BinaryOperator.BINARY_XOR;
    } else {
      throw new CFAGenerationRuntimeException(
        "Could not proccess Operator: " + op.toString());
    }
  }

  private BinaryOperator convertBooleanOperator(InfixExpression.Operator op) {
    if (op.equals(InfixExpression.Operator.CONDITIONAL_AND)) {
      return BinaryOperator.CONDITIONAL_AND;
    } else if (op.equals(InfixExpression.Operator.CONDITIONAL_OR)) {
      return BinaryOperator.CONDITIONAL_OR;
    } else if (op.equals(InfixExpression.Operator.NOT_EQUALS)) {
      return BinaryOperator.NOT_EQUALS;
    } else if (op.equals(InfixExpression.Operator.EQUALS)) {
      return BinaryOperator.EQUALS;
    } else if (op.equals(InfixExpression.Operator.AND)) {
      return BinaryOperator.LOGICAL_AND;
    } else if (op.equals(InfixExpression.Operator.OR)) {
      return BinaryOperator.LOGICAL_OR;
    } else if (op.equals(InfixExpression.Operator.XOR)) {
      return BinaryOperator.LOGICAL_XOR;
    } else {
      throw new CFAGenerationRuntimeException(
          "Could not proccess Operator: " + op.toString());
    }
  }


  private JExpression convert(NumberLiteral e) {
    FileLocation fileLoc = getFileLocation(e);
    JType type = convert(e.resolveTypeBinding());
    String valueStr = e.getToken();

    JBasicType t = ((JSimpleType) type).getType();

    switch (t) {
    case INT:
      return new JIntegerLiteralExpression(fileLoc, parseIntegerLiteral(valueStr, e));
    case FLOAT:
      return new JFloatLiteralExpression(fileLoc, parseFloatLiteral(valueStr));

    case DOUBLE:
      return new JFloatLiteralExpression(fileLoc, parseFloatLiteral(valueStr));

    default:
      return new JIntegerLiteralExpression(getFileLocation(e), BigInteger.valueOf(Long.parseLong(e.getToken())));
    }
  }

  private BigDecimal parseFloatLiteral(String valueStr) {

    BigDecimal value;
    try {
      value = new BigDecimal(valueStr);
    } catch (NumberFormatException nfe1) {
      try {
        // this might be a hex floating point literal
        // BigDecimal doesn't support this, but Double does
        // TODO handle hex floating point literals that are too large for Double
        value = BigDecimal.valueOf(Double.parseDouble(valueStr));
      } catch (NumberFormatException nfe2) {
        throw new CFAGenerationRuntimeException("Illegal floating point literal", nfe2);
      }
    }
    return value;
  }


  private BigInteger parseIntegerLiteral(String s, ASTNode e) {
    assert !s.endsWith("l") && !s.endsWith("L");

    final BigInteger result;

    try {
      if (s.startsWith("0x") || s.startsWith("0X")) {
        // this should be in hex format, remove "0x" from the string
        s = s.substring(2);
        result = new BigInteger(s, 16);

      } else if (s.startsWith("0")) {
        result = new BigInteger(s, 8);

      } else if (s.startsWith("0b || 0B")) {
        result = new BigInteger(s, 2);

      } else {
        result = new BigInteger(s, 10);
      }
    } catch (NumberFormatException e1) {
      throw new CFAGenerationRuntimeException("Invalid int [" + s + "]", e1);
    }

    check(isInIntegerRange(result), "Invalid int [" + s + "]", e);

    return result;
  }

  private boolean isInIntegerRange(BigInteger value) {
    final BigInteger smallestPossibleValue = BigInteger.valueOf(Integer.MIN_VALUE);
    final BigInteger biggestPossibleValue = BigInteger.valueOf(Integer.MAX_VALUE);

    return value.compareTo(smallestPossibleValue) >= 0
        && value.compareTo(biggestPossibleValue) <= 0;
  }


  public JMethodInvocationAssignmentStatement getIteratorFromIterable(Expression pExpr) {

    // Get Object to be iterated
    JExpression iterable = convertExpressionWithoutSideEffects(pExpr);

    if (!(iterable instanceof JIdExpression)) {
      throw new CFAGenerationRuntimeException(pExpr.toString() + "was not correctly processed.",
          pExpr);
    }

    FileLocation fileLoc = getFileLocation(pExpr);

    //TODO correct JMethodExpression when standard Library will be
    //              supported

    List<JExpression> parameters = new LinkedList<>();

    JInterfaceType iteratorTyp = JInterfaceType.createUnresolvableType();

    JIdExpression name = new JIdExpression(fileLoc, iteratorTyp, "iterator", null);

    JReferencedMethodInvocationExpression mi =
        new JReferencedMethodInvocationExpression(fileLoc, iteratorTyp, name, parameters, null, (JIdExpression) iterable);


    // create Iterator Declaration
    String varName = "it_";
    int i = 0;
    while (scope.variableNameInUse(varName + i, varName + i)) {
      i++;
    }
    varName += i;

    JVariableDeclaration decl = new JVariableDeclaration(fileLoc,
        iteratorTyp,
        varName,
        varName,
        getQualifiedName(varName),
        null, NOT_FINAL);

    scope.registerDeclarationOfThisClass(decl);

    // Add Declaration before Assignment
    preSideAssignments.add(decl);


    enhancedForLoopIterator = new JIdExpression(decl.getFileLocation(),
        iteratorTyp,
        varName,
        decl);

    // Create Assignment it = x.iterators();
    return new JMethodInvocationAssignmentStatement(
        fileLoc, enhancedForLoopIterator, mi);

  }

  public JExpression createIteratorCondition(Expression e) {

    FileLocation fileloc = enhancedForLoopIterator.getFileLocation();

    JType type = JSimpleType.getBoolean();

    JExpression name = new JIdExpression(fileloc, type, "hasNext", null);

    List<JExpression> parameters = new LinkedList<>();

    JReferencedMethodInvocationExpression mi =
        new JReferencedMethodInvocationExpression(
            fileloc, type, name, parameters, null,
            enhancedForLoopIterator);

    return addSideassignmentsForExpressionsWithoutMethodInvocationSideEffects(mi, e);
  }

  public JMethodInvocationAssignmentStatement assignParameterToNextIteratorItem(SingleVariableDeclaration formalParameter) {

    FileLocation fileLoc = getFileLocation(formalParameter);

    JSimpleDeclaration param =
        scope.lookupVariable(NameConverter.convertName(
            formalParameter.resolveBinding()));

    if (param == null) {
      throw new CFAGenerationRuntimeException(
        "Formal Parameter " + formalParameter.toString()
            + " could not be proccessed", formalParameter);
      }

    JIdExpression paramIdExpr = new JIdExpression(
                                            fileLoc,
                                            param.getType(),
                                            param.getName(),
                                            param);

    //TODO correct JMethodExpression when standard Library will be
    //              supported

    List<JExpression> parameters = new LinkedList<>();

    JIdExpression name = new JIdExpression(fileLoc, param.getType(), "next", null);

    JReferencedMethodInvocationExpression mi =
        new JReferencedMethodInvocationExpression(fileLoc, param.getType(), name, parameters, null, enhancedForLoopIterator);

    enhancedForLoopIterator = null;

    return new JMethodInvocationAssignmentStatement(
        fileLoc, paramIdExpr, mi);
  }

  JStringLiteralExpression convert(StringLiteral e) {
    FileLocation fileLoc = getFileLocation(e);
    JType type = convert(e.resolveTypeBinding());
    return new JStringLiteralExpression(fileLoc, type, e.getLiteralValue());
  }

  JNullLiteralExpression convert(NullLiteral e) {
    return new JNullLiteralExpression(getFileLocation(e));
  }


  JCharLiteralExpression convert(CharacterLiteral e) {
    FileLocation fileLoc = getFileLocation(e);
    JType type = convert(e.resolveTypeBinding());
    return new JCharLiteralExpression(fileLoc, type, e.charValue());
  }

  /**
   * Converts a Expression into the intern AST which is required to
   * give a boolean Type back.
   *
   * @param e an expression with a boolean type
   * @return intern AST representing JDT expression
   */
  public JExpression convertBooleanExpression(Expression e) {

    JExpression exp = convertExpressionWithoutSideEffects(e);

    if (!isBooleanExpression(exp)) {
      // TODO: Is there even such a case?
      JExpression zero = new JBooleanLiteralExpression(exp.getFileLocation(), false);
      return new JBinaryExpression(exp.getFileLocation(), exp.getExpressionType(), exp, zero, BinaryOperator.NOT_EQUALS);
    }

    return exp;
  }

  private static final Set<BinaryOperator> BOOLEAN_BINARY_OPERATORS = ImmutableSet.of(
      BinaryOperator.EQUALS,
      BinaryOperator.NOT_EQUALS,
      BinaryOperator.GREATER_EQUAL,
      BinaryOperator.GREATER_THAN,
      BinaryOperator.LESS_EQUAL,
      BinaryOperator.LESS_THAN,
      BinaryOperator.LOGICAL_AND,
      BinaryOperator.LOGICAL_OR,
      BinaryOperator.CONDITIONAL_AND,
      BinaryOperator.CONDITIONAL_OR);

  /**
   * Checks if the given Expression returns a Value of
   *  boolean Type.
   *
   * @param e Expression to be checked
   * @return True, iff Type of Expression is boolean, else False.
   */
  public boolean isBooleanExpression(JExpression e) {
    if (e instanceof JBinaryExpression) {
      return BOOLEAN_BINARY_OPERATORS.contains(((JBinaryExpression) e).getOperator());

    } else if (e instanceof JUnaryExpression) {
      return ((JUnaryExpression) e).getOperator() == UnaryOperator.NOT;

    } else {

      //TODO If parser support for Wrapper classes is implemented,
      //      We also need to check for BOOLEAN class
      JType type = e.getExpressionType();

      return type instanceof JSimpleType
          && ((JSimpleType) type).getType() == JBasicType.BOOLEAN;
    }
  }

  JObjectReferenceReturn getConstructorObjectReturn(ITypeBinding declaringClass) {

    assert declaringClass.isClass() || declaringClass.isEnum() : declaringClass.getName() + " is not a Class";

    JClassType objectReturnType = (JClassType) convert(declaringClass);

    return new JObjectReferenceReturn(FileLocation.DUMMY, objectReturnType);
  }


  public JRunTimeTypeEqualsType convertClassRunTimeCompileTimeAccord(FileLocation fileloc,
      JMethodInvocationExpression methodInvocation,
      JClassOrInterfaceType classType) {

    if (methodInvocation instanceof JReferencedMethodInvocationExpression) {
      JIdExpression referencedVariable =
          ((JReferencedMethodInvocationExpression) methodInvocation).getReferencedVariable();

      JRunTimeTypeExpression methodReturnType = new JVariableRunTimeType(fileloc, referencedVariable);

      return new JRunTimeTypeEqualsType(fileloc, methodReturnType, classType);

    } else {
      return new JRunTimeTypeEqualsType(fileloc,
          new JThisExpression(fileloc, methodInvocation.getDeclaringType()), classType);
    }
  }

  public void assignRunTimeClass(JReferencedMethodInvocationExpression methodInvocation,
      JClassInstanceCreation functionCall) {
    JClassOrInterfaceType returnType = functionCall.getExpressionType();

    methodInvocation.setRunTimeBinding(returnType);
  }

  public JExpressionAssignmentStatement getBooleanAssign(JLeftHandSide pLeftHandSide, boolean booleanLiteral) {
    return new JExpressionAssignmentStatement(pLeftHandSide.getFileLocation(), pLeftHandSide,
        new JBooleanLiteralExpression(pLeftHandSide.getFileLocation(), booleanLiteral));
  }


  /**
   * Creates a default Constructor AST for a class represented by the class Binding.
   *
   * @param classBinding representation of the class a constructor should be constructed for
   * @return a {@link JMethodDeclaration} representing the default constructor of the given class
   *         binding
   */
  public JMethodDeclaration createDefaultConstructor(ITypeBinding classBinding) {

    List<JType> paramTypes = new LinkedList<>();
    List<JParameterDeclaration> param = new LinkedList<>();

    JConstructorType type = new JConstructorType((JClassType)
        convert(classBinding), paramTypes, false);

    String simpleName = getSimpleName(classBinding);

    return new JConstructorDeclaration(FileLocation.DUMMY, type,
        NameConverter.convertDefaultConstructorName(classBinding),
        simpleName, param, VisibilityModifier.PUBLIC, false, type.getReturnType());
  }

  static class ModifierBean {

    private final boolean isFinal;
    private final boolean isStatic;
    private final boolean isVolatile;
    private final boolean isTransient;
    private final boolean isNative;
    private final boolean isAbstract;
    private final boolean isStrictFp;
    private final boolean isSynchronized;
    private final VisibilityModifier visibility;

    public ModifierBean(boolean pIsFinal, boolean pIsStatic,
        boolean pIsVolatile, boolean pIsTransient,
        VisibilityModifier pVisibility, boolean pIsNative,
        boolean pIsAbstract, boolean pIsStrictFp,
        boolean pIsSynchronized) {

      visibility = pVisibility;
      isFinal = pIsFinal;
      isStatic = pIsStatic;
      isVolatile = pIsVolatile;
      isTransient = pIsTransient;
      isNative = pIsNative;
      isAbstract = pIsAbstract;
      isStrictFp = pIsStrictFp;
      isSynchronized = pIsSynchronized;
    }

    public static ModifierBean getModifiers(IMethodBinding imb) {
      return getModifiers(imb.getModifiers());
    }

    public static ModifierBean getModifiers(int modifiers) {

      boolean isFinal = Modifier.isFinal(modifiers);
      boolean isStatic = Modifier.isStatic(modifiers);
      boolean isVolatile = Modifier.isVolatile(modifiers);
      boolean isTransient = Modifier.isTransient(modifiers);
      boolean isNative = Modifier.isNative(modifiers);
      boolean isAbstract = Modifier.isAbstract(modifiers);
      boolean isStrictFp = Modifier.isStrictfp(modifiers);
      boolean isSynchronized = Modifier.isSynchronized(modifiers);

      VisibilityModifier visibility = null;
      if (Modifier.isPublic(modifiers)) {
        visibility = VisibilityModifier.PUBLIC;
      }
      if (Modifier.isProtected(modifiers)) {
        assert visibility == null : "Can only declare one Visibility Modifier";
        visibility = VisibilityModifier.PROTECTED;
      }
      if (Modifier.isPrivate(modifiers)) {
        assert visibility == null : "Can only declare one Visibility Modifier";
        visibility = VisibilityModifier.PRIVATE;
      }
      if (visibility == null) {
        visibility = VisibilityModifier.NONE;
      }

      return new ModifierBean(isFinal, isStatic, isVolatile,
          isTransient, visibility, isNative, isAbstract,
          isStrictFp, isSynchronized);

    }

    public static ModifierBean getModifiers(ITypeBinding pBinding) {


      // This int value is the bit-wise or of Modifier constants
      int modifiers = pBinding.getModifiers();

      assert pBinding.isClass() || pBinding.isEnum()
          || pBinding.isInterface() || pBinding.isAnnotation()
          || pBinding.isRecovered() : "This type can't have modifiers";


      return getModifiers(modifiers);
    }

    static ModifierBean getModifiers(List<IExtendedModifier> modifiers) {

      VisibilityModifier visibility = null;
      boolean isFinal = false;
      boolean isStatic = false;
      boolean isVolatile = false;
      boolean isTransient = false;
      boolean isNative = false;
      boolean isAbstract = false;
      boolean isStrictFp = false;
      boolean isSynchronized = false;

      for (IExtendedModifier modifier : modifiers) {

        if (modifier.isModifier()) {
          ModifierKeyword modifierEnum = ((Modifier) modifier).getKeyword();

          switch (modifierEnum.toFlagValue()) {

          case Modifier.FINAL:
            isFinal = true;
            break;
          case Modifier.STATIC:
            isStatic = true;
            break;
          case Modifier.VOLATILE:
            isVolatile = true;
            break;
          case Modifier.TRANSIENT:
            isTransient = true;
            break;
          case Modifier.PUBLIC:
            assert visibility == null : "Can only declare one Visibility Modifier";
            visibility = VisibilityModifier.PUBLIC;
            break;
          case Modifier.PROTECTED:
            assert visibility == null : "Can only declare one Visibility Modifier";
            visibility = VisibilityModifier.PROTECTED;
            break;
          case Modifier.NONE:
            assert visibility == null : "Can only declare one Visibility Modifier";
            visibility = VisibilityModifier.NONE;
            break;
          case Modifier.PRIVATE:
            assert visibility == null : "Can only declare one Visibility Modifier";
            visibility = VisibilityModifier.PRIVATE;
            break;
          case Modifier.NATIVE:
            isNative = true;
            break;
          case Modifier.ABSTRACT:
            isAbstract = true;
            break;
          case Modifier.STRICTFP:
            isStrictFp = true;
            break;
          case Modifier.SYNCHRONIZED:
            isSynchronized = true;
            break;

          default:
            throw new AssertionError("Unkown  Modifier");
          }
        }
      }

      // If no VisibilityModifier was given
      if (visibility == null) {
        visibility = VisibilityModifier.NONE;
      }

      return new ModifierBean(isFinal, isStatic, isVolatile,
          isTransient, visibility, isNative, isAbstract,
          isStrictFp, isSynchronized);
    }

    public VisibilityModifier getVisibility() {
      return visibility;
    }

    public boolean isFinal() {
      return isFinal;
    }

    public boolean isStatic() {
      return isStatic;
    }

    public boolean isVolatile() {
      return isVolatile;
    }

    public boolean isTransient() {
      return isTransient;
    }

    public boolean isNative() {
      return isNative;
    }

    public boolean isAbstract() {
      return isAbstract;
    }

    public boolean isStrictFp() {
      return isStrictFp;
    }

    public boolean isSynchronized() {
      return isSynchronized;
    }
  }
}
