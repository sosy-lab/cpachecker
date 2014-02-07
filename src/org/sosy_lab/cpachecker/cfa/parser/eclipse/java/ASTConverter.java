/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static com.google.common.base.Preconditions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.eclipse.jdt.core.dom.ASTNode;
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
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
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
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.util.NameConverter;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JConstructorType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;


public class ASTConverter {

  private static final boolean NOT_FINAL = false;

  private static final int NO_LINE = 0;

  private static final int FIRST = 0;

  private static final int SECOND = 1;

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
    typeConverter = new ASTTypeConverter(logger, scope);
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
   * This method returns the number of Pre Side Assignments
   * of converted Statements.
   *
   * @return number of Pre Side Assignments of converted Statements
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
   * Returns the temporary Variable, which holds the
   * result of the conditional statement
   *
   * @return
   */
  public JIdExpression getConditionalTemporaryVariable() {
    return conditionalTemporaryVariable;
  }



  private static void check(boolean assertion, String msg, ASTNode astNode) throws CFAGenerationRuntimeException {
    if (!assertion) {
      throw new CFAGenerationRuntimeException(msg);
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
   * @param Method Declaration to be coverted.
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
      return new FileLocation(0, "", 0, 0, 0);
    } else if (l.getRoot().getNodeType() != ASTNode.COMPILATION_UNIT) {
      logger.log(Level.WARNING, "Can't find Placement Information for :"
          + l.toString());
      return new FileLocation(0, "", 0, 0, 0);
    }

    CompilationUnit co = (CompilationUnit) l.getRoot();

    return new FileLocation(co.getLineNumber(l.getLength() + l.getStartPosition()),
        scope.getFileOfCurrentType(), l.getLength(), l.getStartPosition(),
        co.getLineNumber(l.getStartPosition()));
  }

  /**
   * Converts a List of Field Declaration into the intern AST.
   *
   * @param field Declarations given to be transformed.
   * @return intern AST of the Field Declarations.
   */
  public List<JDeclaration> convert(FieldDeclaration fd) {

    List<JDeclaration> result = new ArrayList<>();

    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> vdfs =
        fd.fragments();

    for (VariableDeclarationFragment vdf : vdfs) {

      result.add(handleFieldDeclarationFragment(vdf, fd));
    }

    return result;
  }


  private JDeclaration handleFieldDeclarationFragment(VariableDeclarationFragment pVdf,
      FieldDeclaration pFd) {
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

    @Nullable
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
          nameAndInitializer.getName(), nameAndInitializer.getInitializer(),
          mB.isFinal());

      variableDeclarations.add(newD);
    }

    return variableDeclarations;
  }


  /**
   * Converts JDT SingleVariableDeclaration into an AST.
   *
   *
   * @param vds JDT SingleVariableDeclaration to be transformed
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

    return new JReturnStatement(getFileLocation(s), expr);
  }

/**
 * Converts a JDT Expression into the AST. This method always gives
 * side effect free Expressions back. Every Side Effect will be
 * put into a side assignment and can subsequently be fetched
 * with getNextSideAssignment().
 *
 * @param Given expression to be transformed.
 * @return an side effect free AST representing the given parameter.
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
      throw new AssertionError();
    }
  }

  /**
   * Transforms a JDT SuperConstructorInvocation into the intern AST.
   *
   * @param SuperConstructorInvocation to be transformed.
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
      params = new ArrayList<>();
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
    }

    logger.log(Level.WARNING, "Expression of typ " + AstDebugg.getTypeName(e.getNodeType()) + " not implemented");
    return null;
  }

  private JAstNode convert(SuperMethodInvocation e) {

    boolean canBeResolve = e.resolveMethodBinding() != null;

    JClassOrInterfaceType declaringClassType = null;

    if (canBeResolve) {
      declaringClassType = (JClassOrInterfaceType) convert(e.resolveMethodBinding().getDeclaringClass());
      scope.registerClass(e.resolveMethodBinding().getDeclaringClass());
    }

    @SuppressWarnings("unchecked")
    List<Expression> p = e.arguments();

    List<JExpression> params;
    if (p.size() > 0) {
      params = convert(p);
    } else {
      params = new ArrayList<>();
    }

    JExpression methodName = convertExpressionWithoutSideEffects(e.getName());

    JMethodDeclaration declaration = null;

    ModifierBean mb = null;

    if (canBeResolve) {
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

      if (canBeResolve) {
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

      if (canBeResolve) {

        JType type = miv.getDeclaringType();

        if (type instanceof JClassType) {
         miv.setRunTimeBinding((JClassType) type);
        }
      }

      return miv;
  }



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
          nameAndInitializer.getName(), nameAndInitializer.getInitializer(),
          mB.isFinal());

      variableDeclarations.add(newD);
    }

    forInitDeclarations.addAll(variableDeclarations);

    return null;
  }

  private JAstNode convert(InstanceofExpression e) {

    FileLocation fileloc = getFileLocation(e);
    JExpression leftOperand = convertExpressionWithoutSideEffects(e.getLeftOperand());
    JType type = convert(e.getRightOperand().resolveBinding());
    assert leftOperand instanceof JIdExpression : "There are other expressions for instanceOf?";
    assert type instanceof JClassOrInterfaceType : "There are other types for this expression?";

    JIdExpression referenceVariable = (JIdExpression) leftOperand;
    JClassOrInterfaceType instanceCompatible = (JClassOrInterfaceType) type;
    JRunTimeTypeEqualsType firstCond = null;



    List<JClassType> subClassTypes;

    if (instanceCompatible instanceof JInterfaceType) {

      Set<JClassType> subClassTypeSet = ((JInterfaceType) instanceCompatible).getAllKnownImplementingClassesOfInterface();

      subClassTypes =  transformSetToList(subClassTypeSet);

      if (subClassTypes.isEmpty()) {
        return new JBooleanLiteralExpression(fileloc, false);
      } else if (subClassTypes.size() == 1) {
        return convertClassRunTimeCompileTimeAccord(fileloc,
          referenceVariable, subClassTypes.get(FIRST)); }

    } else if (instanceCompatible instanceof JClassType) {

      Set<JClassType> subClassSet = ((JClassType) instanceCompatible).getAllSubTypesOfClass();

      subClassTypes = transformSetToList(subClassSet);

      firstCond = convertClassRunTimeCompileTimeAccord(fileloc, referenceVariable, instanceCompatible);
      if (subClassTypes.isEmpty()) { return firstCond; }

    } else {
      throw new AssertionError();
    }

    JBinaryExpression firstOrConnection;


    if (firstCond == null) {
      firstOrConnection =
          new JBinaryExpression(fileloc, convert(e.resolveTypeBinding()), convertClassRunTimeCompileTimeAccord(fileloc,
              referenceVariable, subClassTypes.get(FIRST)), convertClassRunTimeCompileTimeAccord(
              fileloc, referenceVariable, subClassTypes.get(SECOND)),
              JBinaryExpression.BinaryOperator.CONDITIONAL_OR);
      subClassTypes.remove(SECOND);
      subClassTypes.remove(FIRST);
    } else {
      firstOrConnection =
          new JBinaryExpression(fileloc, convert(e.resolveTypeBinding()), firstCond,
              convertClassRunTimeCompileTimeAccord(fileloc, referenceVariable,
                  subClassTypes.get(FIRST)), JBinaryExpression.BinaryOperator.CONDITIONAL_OR);
      subClassTypes.remove(FIRST);
    }

    JBinaryExpression nextConnection = firstOrConnection;

    for (JClassType subType : subClassTypes) {
      JRunTimeTypeEqualsType cond =
          convertClassRunTimeCompileTimeAccord(fileloc, referenceVariable, subType);
      nextConnection =
          new JBinaryExpression(fileloc, convert(e.resolveTypeBinding()), nextConnection, cond,
              BinaryOperator.CONDITIONAL_OR);
    }

    return nextConnection;
  }


  private List<JClassType> transformSetToList(Set<JClassType> pSubClassTypeSet) {
    List<JClassType> result = new ArrayList<>(pSubClassTypeSet.size());

    for (JClassType types : pSubClassTypeSet) {
      result.add(types);
    }

    return result;
  }

  private JRunTimeTypeEqualsType convertClassRunTimeCompileTimeAccord(
      FileLocation pFileloc, JIdExpression pDeclaration,
      JClassOrInterfaceType classType) {

    JRunTimeTypeExpression runTimeTyp =
        new JVariableRunTimeType(pFileloc, pDeclaration);

    return new JRunTimeTypeEqualsType(pFileloc,
        runTimeTyp, classType);
  }


  private JAstNode convert(ThisExpression e) {
    return new JThisExpression(getFileLocation(e),
        (JClassOrInterfaceType) convert(e.resolveTypeBinding()));
  }

  private JAstNode convert(FieldAccess e) {

    IVariableBinding fieldBinding = e.resolveFieldBinding();

    boolean canBeResolved = fieldBinding != null;

    if (canBeResolved) {
      scope.registerClass(e.resolveFieldBinding().getDeclaringClass());
    }

    JAstNode identifier = convertExpressionWithoutSideEffects(e.getName());

    if (!(identifier instanceof JIdExpression)) {
      throw new CFAGenerationRuntimeException(
        "Identifier of FieldAcces could not be preoccessed.", e);
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
        "Qualifier of FieldAcces could not be proccessed.", e);
    }

    JSimpleDeclaration decl = idExpIdentifier.getDeclaration();


    if (!(decl instanceof JFieldDeclaration)) {
      throw new CFAGenerationRuntimeException(
        "Identifier of FieldAccess no Field.", e);
    }

    return new JFieldAccess(idExpIdentifier.getFileLocation(),
        idExpIdentifier.getExpressionType(),
        idExpIdentifier.getName(), (JFieldDeclaration) decl,
        (JIdExpression) qualifier);
  }

  private JAstNode convert(ClassInstanceCreation cIC) {

    IMethodBinding binding = cIC.resolveConstructorBinding();

    boolean canBeResolved = binding != null;


    if (canBeResolved) {
      scope.registerClass(binding.getDeclaringClass());
    }

    @SuppressWarnings("unchecked")
    List<Expression> p = cIC.arguments();

    List<JExpression> params;

    if (p.size() > 0) {
      params = convert(p);

    } else {
      params = new ArrayList<>();
    }

    String name;
    String simpleName;

    if (canBeResolved) {
      name = NameConverter.convertName(binding);
      simpleName = binding.getName();
    } else {
      // If binding can't be resolved, the constructor is not parsed in all cases.
      name = cIC.toString();
      simpleName = cIC.toString();
    }

    JConstructorDeclaration declaration = (JConstructorDeclaration) scope.lookupMethod(name);

    if (declaration == null) {

      if (canBeResolved) {

        ModifierBean mb = ModifierBean.getModifiers(binding);

        declaration =
            new JConstructorDeclaration(getFileLocation(cIC),
                convertConstructorType(binding), name, simpleName,
                new LinkedList<JParameterDeclaration>(),
                mb.getVisibility(), mb.isStrictFp(),
                (JClassType) getDeclaringClassType(binding));

      } else {

        declaration =
            new JConstructorDeclaration(getFileLocation(cIC), JConstructorType.createUnresolvableConstructorType(), name,
                simpleName, new ArrayList<JParameterDeclaration>(), VisibilityModifier.NONE, false,
                (JClassType) getDeclaringClassType(binding));
      }
    }

    JExpression functionName =
        new JIdExpression(getFileLocation(cIC), convert(cIC.resolveTypeBinding()), name, declaration);
    JIdExpression idExpression = (JIdExpression) functionName;

    if (idExpression.getDeclaration() != null) {
      // clone idExpression because the declaration in it is wrong
      // (it's the declaration of an equally named variable)
      // TODO this is ugly

      functionName =
          new JIdExpression(idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
    }


    return new JClassInstanceCreation(getFileLocation(cIC),
        (JClassType) getDeclaringClassType(binding), functionName,
        params, declaration);
  }


  private JAstNode convert(ConditionalExpression e) {
    JIdExpression tmp = createTemporaryVariable(e);
    conditionalTemporaryVariable = tmp;
    conditionalExpression = e;
    return tmp;
  }

  private JAstNode convert(ArrayInitializer initializer) {

    if (initializer == null) {
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

    IBinding binding = e.resolveBinding();

    boolean canBeResolved = e.resolveBinding() != null;

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

        return convertQualifiedVariableIdentificationExpression(e, vb);
      } else {

        String name = e.getFullyQualifiedName();
        return new JIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, null);
      }
    } else {

      String name = e.getFullyQualifiedName();
      return new JIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, null);

    }
  }

  private JAstNode convertQualifiedVariableIdentificationExpression(
      QualifiedName e, IVariableBinding vb) {

    JAstNode identifier = convertExpressionWithoutSideEffects(e.getName());

    if (!(identifier instanceof JIdExpression)) {
      throw new CFAGenerationRuntimeException(
        "Identifier of FieldAcces could not be preoccessed.", e);
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
        "Qualifier of FieldAcces could not be proccessed.", e);
    }

    JSimpleDeclaration decl = idExpIdentifier.getDeclaration();


    if (!(decl instanceof JFieldDeclaration)) {
      throw new CFAGenerationRuntimeException(
        "Identifier of FieldAccess no Field.", e);
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
        new JIdExpression(oldMethodCall.getFileLocation(), new JSimpleType(JBasicType.UNSPECIFIED), name, declaration);

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

    boolean canBeResolve = mi.resolveMethodBinding() != null;

    JClassOrInterfaceType declaringClassType = null;

    if (canBeResolve) {
      declaringClassType = (JClassOrInterfaceType) convert(mi.resolveMethodBinding().getDeclaringClass());
      scope.registerClass(mi.resolveMethodBinding().getDeclaringClass());
    }

    @SuppressWarnings("unchecked")
    List<Expression> p = mi.arguments();

    List<JExpression> params;
    if (p.size() > 0) {
      params = convert(p);
    } else {
      params = new ArrayList<>();
    }

    JExpression methodName = convertExpressionWithoutSideEffects(mi.getName());

    JMethodDeclaration declaration = null;
    JExpression referencedVariableName = null;


    ModifierBean mb = null;

    if (canBeResolve) {
      mb = ModifierBean.getModifiers(mi.resolveMethodBinding());

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

      if (canBeResolve) {
        declaration = scope.createExternMethodDeclaration(
            convertMethodType(mi.resolveMethodBinding()),
            methodName.toASTString(),
            mi.resolveMethodBinding().getName(),
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
    boolean canBeResolved = e.resolveBinding() != null;

    //TODO Complete declaration by finding all Bindings
    if (canBeResolved) {

      IBinding binding = e.resolveBinding();



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
        + e.getIdentifier() + " not found. \n", e);
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

    BinaryOperator op = convert(e.getOperator());

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


  private BinaryOperator convert(Assignment.Operator op) {


    if (op.equals(Assignment.Operator.ASSIGN)) {
      return null;
    } else if (op.equals(Assignment.Operator.BIT_AND_ASSIGN)) {
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
      logger.log(Level.SEVERE, "Did not find Operator");
      return null;
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
                                            operand, convertUnaryOperator(op));
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




  private UnaryOperator convertUnaryOperator(PrefixExpression.Operator op) {

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

    BinaryOperator op = convertBinaryOperator(e.getOperator());

    JExpression rightHandSide = convertExpressionWithoutSideEffects(e.getRightOperand());

    assert rightHandSide != null;
    assert leftHandSide != null;

    JExpression binaryExpression = new JBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

    // a x b x c is being translated to (((a x b) x c) x d)
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

  private BinaryOperator convertBinaryOperator(InfixExpression.Operator op) {

    if (op.equals(InfixExpression.Operator.PLUS)) {
      return BinaryOperator.PLUS;
    } else if (op.equals(InfixExpression.Operator.MINUS)) {
      return BinaryOperator.MINUS;
    } else if (op.equals(InfixExpression.Operator.DIVIDE)) {
      return BinaryOperator.DIVIDE;
    } else if (op.equals(InfixExpression.Operator.TIMES)) {
      return BinaryOperator.MULTIPLY;
    } else if (op.equals(InfixExpression.Operator.EQUALS)) {
      return BinaryOperator.EQUALS;
    } else if (op.equals(InfixExpression.Operator.REMAINDER)) {
      return BinaryOperator.MODULO;
    } else if (op.equals(InfixExpression.Operator.CONDITIONAL_AND)) {
      return BinaryOperator.CONDITIONAL_AND;
    } else if (op.equals(InfixExpression.Operator.CONDITIONAL_OR)) {
      return BinaryOperator.CONDITIONAL_OR;
    } else if (op.equals(InfixExpression.Operator.AND)) {
      return BinaryOperator.LOGICAL_AND;
    } else if (op.equals(InfixExpression.Operator.OR)) {
      return BinaryOperator.LOGICAL_OR;
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
    } else {
      throw new CFAGenerationRuntimeException(
          "Cold not proccess Operator:" + op.toString());
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
      return new JFloatLiteralExpression(fileLoc, parseFloatLiteral(valueStr, e));

    case DOUBLE:
      return new JFloatLiteralExpression(fileLoc, parseFloatLiteral(valueStr, e));
    }
    return new JIntegerLiteralExpression(getFileLocation(e), BigInteger.valueOf(Long.parseLong(e.getToken())));
  }




  private BigDecimal parseFloatLiteral(String valueStr, NumberLiteral e) {

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
        throw new CFAGenerationRuntimeException("illegal floating point literal");
      }
    }
    return value;
  }


  private BigInteger parseIntegerLiteral(String s, ASTNode e) {
    int last = s.length() - 1;
    int bits = 32;

    if (s.charAt(last) == 'L' || s.charAt(last) == 'l') {
      last--;
      bits = 64;
    }

    s = s.substring(0, last + 1);
    BigInteger result;
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
    } catch (NumberFormatException _) {
      throw new CFAGenerationRuntimeException("invalid number");
    }
    check(result.compareTo(BigInteger.ZERO) >= 0, "invalid number", e);

    // clear the bits that don't fit in the type
    // a BigInteger with the lowest "bits" bits set to one (e. 2^32-1 or 2^64-1)
    BigInteger mask = BigInteger.ZERO.setBit(bits).subtract(BigInteger.ONE);
    result = result.and(mask);
    assert result.bitLength() <= bits;

    return result;
  }


  public JMethodInvocationAssignmentStatement getIteratorFromIterable(Expression pExpr) {

    // Get Object to be iterated
    JExpression iterable = convertExpressionWithoutSideEffects(pExpr);

    if (!(iterable instanceof JIdExpression)) {
      throw new CFAGenerationRuntimeException(pExpr.toString() + "was not correctly proccessed." , pExpr);
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

    JType type = new JSimpleType(JBasicType.BOOLEAN);

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
   *
   * @param e a Expression with a boolean type
   * @return Intern AST representing JDT Expression
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

  JObjectReferenceReturn getConstructorObjectReturn(ITypeBinding declaringClass, FileLocation constructorFileLoc) {

    assert declaringClass.isClass() || declaringClass.isEnum() : declaringClass.getName() + " is not a Class";

    FileLocation fileloc =
        new FileLocation(constructorFileLoc.getEndingLineNumber(),
            constructorFileLoc.getFileName(), NO_LINE,
            constructorFileLoc.getEndingLineNumber(),
            constructorFileLoc.getEndingLineNumber());

    JClassType objectReturnType = (JClassType) convert(declaringClass);

    return new JObjectReferenceReturn(fileloc, objectReturnType);
  }


  public JRunTimeTypeEqualsType convertClassRunTimeCompileTimeAccord(FileLocation fileloc,
      JMethodInvocationExpression methodInvocation,
      JClassOrInterfaceType classType) {

    if (methodInvocation instanceof JReferencedMethodInvocationExpression) {
      return new JRunTimeTypeEqualsType(fileloc, new JVariableRunTimeType(fileloc,
          ((JReferencedMethodInvocationExpression) methodInvocation).getReferencedVariable()),
          classType);
    } else {
      return new JRunTimeTypeEqualsType(fileloc,
          new JThisExpression(fileloc, methodInvocation.getDeclaringType()), classType);
    }
  }

  public void assignRunTimeClass(JReferencedMethodInvocationExpression methodInvocation,
      JClassInstanceCreation functionCall) {
    JClassType returnType = functionCall.getExpressionType();

    methodInvocation.setRunTimeBinding(returnType);
  }

  public JExpressionAssignmentStatement getBooleanAssign(JLeftHandSide pLeftHandSide, boolean booleanLiteral) {
    return new JExpressionAssignmentStatement(pLeftHandSide.getFileLocation(), pLeftHandSide,
        new JBooleanLiteralExpression(pLeftHandSide.getFileLocation(), booleanLiteral));
  }


  /**
   * Creates a default Constructor AST for a class represented by the class Binding.
   *
   *
   * @param classBinding represents a Class for which the Constructor be constructed.
   * @return An AST for the default Constructor.
   */
  public JMethodDeclaration createDefaultConstructor(ITypeBinding classBinding) {

    List<JType> paramTypes = new LinkedList<>();
    List<JParameterDeclaration> param = new LinkedList<>();

    FileLocation fileLoc = new FileLocation(0, "", 0, 0, 0);

    JConstructorType type = new JConstructorType((JClassType)
        convert(classBinding), paramTypes, false);

    String simpleName = classBinding.getName();

    return new JConstructorDeclaration(fileLoc, type,
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

      VisibilityModifier visibility = null;
      boolean isFinal = false;
      boolean isStatic = false;
      boolean isVolatile = false;
      boolean isTransient = false;
      boolean isNative = false;
      boolean isAbstract = false;
      boolean isStrictFp = false;
      boolean isSynchronized = false;



      // Check all possible bit constants
      for (int bitMask = 1; bitMask < 2049; bitMask = bitMask << 1) {

        // Check if n-th bit of modifiers is 1
        switch (modifiers & bitMask) {

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
        }
      }

      // If no Visibility Modifier is selected, it is None
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
            assert false : " Unkown  Modifier";

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
