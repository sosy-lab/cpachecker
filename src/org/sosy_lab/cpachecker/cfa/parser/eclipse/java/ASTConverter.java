// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.eclipse.jdt.core.dom.ImportDeclaration;
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
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.types.java.JReferenceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

class ASTConverter {

  private static final boolean NOT_FINAL = false;

  private static final int FIRST = 0;

  private final LogManager logger;

  private Scope scope;

  private final ASTTypeConverter typeConverter;

  private List<JDeclaration> forInitDeclarations = new ArrayList<>();
  private Deque<JAstNode> preSideAssignments = new ArrayDeque<>();
  private Deque<JAstNode> postSideAssignments = new ArrayDeque<>();

  private ConditionalExpression conditionalExpression = null;
  private JIdExpression conditionalTemporaryVariable = null;

  // Temporary stores forLoopIterator
  private JIdExpression enhancedForLoopIterator;

  /** Map for unboxing of JClassTypes */
  private static final ImmutableMap<String, JBasicType> unboxingMap =
      ImmutableMap.<String, JBasicType>builder()
          .put("java.lang.Boolean", JBasicType.BOOLEAN)
          .put("java.lang.Byte", JBasicType.BYTE)
          .put("java.lang.Character", JBasicType.CHAR)
          .put("java.lang.Float", JBasicType.FLOAT)
          .put("java.lang.Integer", JBasicType.INT)
          .put("java.lang.Long", JBasicType.LONG)
          .put("java.lang.Short", JBasicType.SHORT)
          .put("java.lang.Double", JBasicType.DOUBLE)
          .put("java.lang.Void", JBasicType.VOID)
          .buildOrThrow();

  /**
   * Create a new AST Converter, which can be used to convert JDT AST Statements to CFA AST
   * Statements.
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
   * This method returns the number of Post Side Assignments of converted Statements.
   *
   * @return number of Post Side Assignments of converted Statements
   */
  public int numberOfPostSideAssignments() {
    return postSideAssignments.size();
  }

  /**
   * This method returns the number of Side Assignments of converted Statements.
   *
   * @return number of Side Assignments of converted Statements
   */
  public int numberOfSideAssignments() {
    return preSideAssignments.size() + postSideAssignments.size();
  }

  /**
   * This method returns the number of Pre Side Assignments of a converted Statements.
   *
   * @return number of Pre Side Assignments of converted Statements
   */
  public int numberOfPreSideAssignments() {
    return preSideAssignments.size();
  }

  /**
   * This method returns the next unproccessed Pre Side Assignment of converted Statements.
   *
   * @return Pre Side Assignment of converted Statements
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
   * This method returns the next Post Side Assignments of converted Statements.
   *
   * @return Post Side Assignments of converted Statement
   */
  public JAstNode getNextPostSideAssignment() {
    return postSideAssignments.removeFirst();
  }

  /** Erases the saved Conditional Expression of the last converted Statement */
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
   * Returns the latest temporary variable. This variable holds the result of the last processed
   * conditional statement.
   *
   * @return a {@link JIdExpression} representing the temporary variable of the last processed
   *     conditional statement
   */
  public JIdExpression getConditionalTemporaryVariable() {
    return conditionalTemporaryVariable;
  }

  private static void check(boolean assertion, String msg, ASTNode astNode)
      throws CFAGenerationRuntimeException {
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
   * Converts a Method Declaration of the JDT AST to a MethodDeclaration of the CFA AST
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
   * Takes a ASTNode, and tries to get Information of its Placement in the Source Code. If it
   * doesnt't find such information, returns an empty FileLocation Object.
   *
   * @param l A Code piece wrapped in an ASTNode
   * @return FileLocation with Placement Information of the Code Piece, or null if such Information
   *     could not be obtained.
   */
  public FileLocation getFileLocation(ASTNode l) {
    if (l == null) {
      return FileLocation.DUMMY;
    } else if (l.getRoot().getNodeType() != ASTNode.COMPILATION_UNIT) {
      logger.log(Level.WARNING, "Can't find Placement Information for :" + l);
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
    List<VariableDeclarationFragment> vdfs = fd.fragments();

    return transformedImmutableListCopy(vdfs, this::handleFieldDeclarationFragment);
  }

  private JDeclaration handleFieldDeclarationFragment(VariableDeclarationFragment pVdf) {
    // TODO initializer with side assignment

    NameAndInitializer nameAndInitializer = getNamesAndInitializer(pVdf);

    String fieldName = nameAndInitializer.getName();

    checkArgument(scope.isFieldRegistered(fieldName));

    JFieldDeclaration fieldDecl = scope.lookupField(fieldName);

    // update initializer (can't be constructed while generating the Declaration)
    if (!preSideAssignments.isEmpty() || !postSideAssignments.isEmpty()) {
      logger.log(
          Level.WARNING, "Sideeffects of initializer of field " + fieldName + " will be ignored");
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

      initializerExpression = new JInitializerExpression(getFileLocation(d), iniExpr);
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

    assert !mB.isAbstract() : "Local Variable has abstract modifier?";
    assert !mB.isNative() : "Local Variable has native modifier?";
    assert (mB.getVisibility() == VisibilityModifier.NONE)
        : "Local Variable has Visibility modifier?";
    assert !mB.isStatic() : "Local Variable has static modifier?";
    assert !mB.isStrictFp() : "Local Variable has strictFp modifier?";
    assert !mB.isSynchronized() : "Local Variable has synchronized modifier?";

    for (VariableDeclarationFragment vdf : variableDeclarationFragments) {

      NameAndInitializer nameAndInitializer = getNamesAndInitializer(vdf);

      String name = nameAndInitializer.getName();
      name = addCounterToName(name);
      JVariableDeclaration newD =
          new JVariableDeclaration(
              fileLoc,
              convert(type),
              name,
              nameAndInitializer.getName(),
              getQualifiedName(name),
              nameAndInitializer.getInitializer(),
              mB.isFinal());

      variableDeclarations.add(newD);
    }

    return variableDeclarations;
  }

  /**
   * Converts JDT SingleVariableDeclaration into an AST.
   *
   * @param d JDT SingleVariableDeclaration to be transformed
   * @return AST representing given Parameter
   */
  public JDeclaration convert(SingleVariableDeclaration d) {

    Type type = d.getType();

    @SuppressWarnings("unchecked")
    ModifierBean mB = ModifierBean.getModifiers(d.modifiers());

    assert !mB.isAbstract : "Local Variable has abstract modifier?";
    assert !mB.isNative : "Local Variable has native modifier?";
    assert mB.visibility == VisibilityModifier.NONE : "Local Variable has Visibility modifier?";
    assert !mB.isStatic : "Local Variable has static modifier?";
    assert !mB.isStrictFp : "Local Variable has strictFp modifier?";
    assert !mB.isSynchronized : "Local Variable has synchronized modifier?";

    JInitializerExpression initializerExpression = null;

    // If there is no Initializer, CStorageClass expects null to be given.
    if (d.getInitializer() != null) {

      JExpression iniExpr = (JExpression) convertExpressionWithSideEffects(d.getInitializer());

      initializerExpression = new JInitializerExpression(getFileLocation(d), iniExpr);
    }

    String name = d.getName().getFullyQualifiedName();

    name = addCounterToName(name);

    return new JVariableDeclaration(
        getFileLocation(d),
        convert(type),
        name,
        d.getName().getFullyQualifiedName(),
        getQualifiedName(name),
        initializerExpression,
        mB.isFinal());
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
   * Converts a JDT Expression into the AST. This method always gives side effect free Expressions
   * back. Every Side Effect will be put into a side assignment and can subsequently be fetched with
   * getNextSideAssignment().
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

    preSideAssignments.add(
        new JExpressionAssignmentStatement(node.getFileLocation(), tmp, (JExpression) node));
    return tmp;
  }

  private void addSideassignmentsForExpressionsWithoutAssignmentSideEffects(
      JAstNode node, Expression e) {

    if (e instanceof PostfixExpression) {
      postSideAssignments.add(node);
    } else {
      preSideAssignments.add(node);
    }
  }

  private JExpression addSideassignmentsForExpressionsWithoutMethodInvocationSideEffects(
      JAstNode node, Expression e) {
    JIdExpression tmp = createTemporaryVariable(e);

    preSideAssignments.add(
        new JMethodInvocationAssignmentStatement(
            node.getFileLocation(), tmp, (JMethodInvocationExpression) node));
    return tmp;
  }

  private JIdExpression createTemporaryVariable(Expression e) {
    String name = "__CPAchecker_TMP_";
    return createTemporaryVariableWithName(e, name);
  }

  private JIdExpression createTemporaryVariableWithName(Expression e, String name) {

    int i = 0;
    while (scope.variableNameInUse(name + i, name + i)) {
      i++;
    }
    name += +i;

    JVariableDeclaration decl =
        new JVariableDeclaration(
            getFileLocation(e),
            convert(e.resolveTypeBinding()),
            name,
            name,
            getQualifiedName(name),
            null,
            NOT_FINAL);

    scope.registerDeclarationOfThisClass(decl);
    preSideAssignments.add(decl);
    JIdExpression tmp =
        new JIdExpression(decl.getFileLocation(), convert(e.resolveTypeBinding()), name, decl);
    return tmp;
  }

  /**
   * Converts a JDT ExpressionStatement into a statement.
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

    final IMethodBinding binding = sCI.resolveConstructorBinding();

    if (binding != null) {
      scope.registerClass(binding.getDeclaringClass());
    }

    @SuppressWarnings("unchecked")
    List<Expression> p = sCI.arguments();
    List<JExpression> params = convert(p);

    String name;
    String simpleName;

    if (binding != null) {
      name = NameConverter.convertName(binding);
      simpleName = binding.getName();
    } else {
      // If binding can't be resolved, the constructor is not parsed in all cases.
      name = sCI.toString();
      simpleName = sCI.toString();
    }

    JConstructorDeclaration declaration = (JConstructorDeclaration) scope.lookupMethod(name);

    if (declaration == null) {

      if (binding != null) {

        ModifierBean mb = ModifierBean.getModifiers(binding);

        declaration =
            scope.createExternConstructorDeclaration(
                convertConstructorType(binding),
                name,
                simpleName,
                mb.getVisibility(),
                mb.isStrictFp(),
                (JClassType) getDeclaringClassType(binding));

      } else {
        declaration = JConstructorDeclaration.createUnresolvedConstructorDeclaration();
      }
    }

    JExpression functionName;

    if (binding != null) {
      functionName =
          new JIdExpression(
              getFileLocation(sCI), convert(binding.getReturnType()), name, declaration);
    } else {

      functionName =
          new JIdExpression(
              getFileLocation(sCI), JClassType.createUnresolvableType(), name, declaration);
    }

    JIdExpression idExpression = (JIdExpression) functionName;

    if (idExpression.getDeclaration() != null) {
      // clone idExpression because the declaration in it is wrong
      // (it's the declaration of an equally named variable)
      // TODO this is ugly

      functionName =
          new JIdExpression(
              idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
    }

    return new JMethodInvocationStatement(
        getFileLocation(sCI),
        new JSuperConstructorInvocation(
            getFileLocation(sCI),
            (JClassType) getDeclaringClassType(binding),
            functionName,
            params,
            declaration));
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

  private JConstructorType convertConstructorType(IMethodBinding pBinding, List<?> arguments) {
    Preconditions.checkArgument(pBinding.isConstructor());

    // Constructors can't be declared by Interfaces
    JClassType declaringClass = (JClassType) getDeclaringClassType(pBinding);

    return new JConstructorType(
        declaringClass, getJTypesOfParameters(arguments), pBinding.isVarargs());
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
   * Converts a JDT Expression into the intern AST. This method doesn't always return a Side effect
   * free Expression.
   *
   * @param e JDT Expression to be transformed
   * @return Intern AST of given JDT Expression
   */
  public JAstNode convertExpressionWithSideEffects(Expression e) {

    // TODO  All Expression Implementation

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
        return convertExpressionWithoutSideEffects(((ParenthesizedExpression) e).getExpression());
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
      case ASTNode.SUPER_FIELD_ACCESS:
        return convert(((SuperFieldAccess) e));
      case ASTNode.TYPE_LITERAL:
        return convert((TypeLiteral) e);
      case ASTNode.SUPER_METHOD_INVOCATION:
        return convert((SuperMethodInvocation) e);
      default:
        logger.log(
            Level.WARNING,
            "Expression of type " + ASTDebug.getTypeName(e.getNodeType()) + " not implemented");
        return null;
    }
  }

  private JAstNode convert(final SuperMethodInvocation e) {

    final IMethodBinding methodBinding = e.resolveMethodBinding();
    JClassOrInterfaceType declaringClassType = null;

    if (methodBinding != null) {
      declaringClassType = (JClassOrInterfaceType) convert(methodBinding.getDeclaringClass());
      scope.registerClass(methodBinding.getDeclaringClass());
    }

    @SuppressWarnings("unchecked")
    List<Expression> p = e.arguments();

    List<JExpression> params;
    if (!p.isEmpty()) {
      params = convert(p);
    } else {
      params = ImmutableList.of();
    }

    JExpression methodName = convertExpressionWithoutSideEffects(e.getName());

    JMethodDeclaration declaration = null;

    if (methodName instanceof JIdExpression) {
      JIdExpression idExpression = (JIdExpression) methodName;
      String name = idExpression.getName();
      declaration = scope.lookupMethod(name);

      if (idExpression.getDeclaration() != null) {
        // TODO this is ugly

        methodName =
            new JIdExpression(
                idExpression.getFileLocation(),
                idExpression.getExpressionType(),
                name,
                declaration);
      }
    }

    if (declaration == null) {

      if (methodBinding != null) {
        ModifierBean mb = ModifierBean.getModifiers(methodBinding);
        declaration =
            scope.createExternMethodDeclaration(
                convertMethodType(methodBinding),
                methodName.toASTString(),
                methodBinding.getName(),
                VisibilityModifier.PUBLIC,
                mb.isFinal(),
                mb.isAbstract(),
                mb.isStatic(),
                mb.isNative(),
                mb.isSynchronized(),
                mb.isStrictFp(),
                declaringClassType);

      } else {
        declaration = JMethodDeclaration.createUnresolvedMethodDeclaration();
      }
    }

    JMethodInvocationExpression miv =
        new JMethodInvocationExpression(
            getFileLocation(e), convert(e.resolveTypeBinding()), methodName, params, declaration);

    if (methodBinding != null) {

      JType type = miv.getDeclaringType();

      if (type instanceof JClassType) {
        miv.setRunTimeBinding((JClassType) type);
      }
    }

    return miv;
  }

  private JAstNode convert(TypeLiteral pTypeLiteral) {
    final ITypeBinding iTypeBinding = pTypeLiteral.resolveTypeBinding();
    JType jType = typeConverter.convert(iTypeBinding);
    return new JClassLiteralExpression(getFileLocation(pTypeLiteral), jType);
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
    return new JCastExpression(
        getFileLocation(e),
        convert(e.resolveTypeBinding()),
        convertExpressionWithoutSideEffects(e.getExpression()));
  }

  private JIdExpression convert(VariableDeclarationExpression vde) {

    List<JDeclaration> variableDeclarations = new ArrayList<>();

    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> variableDeclarationFragments = vde.fragments();

    FileLocation fileLoc = getFileLocation(vde);
    Type type = vde.getType();

    @SuppressWarnings("unchecked")
    ModifierBean mB = ModifierBean.getModifiers(vde.modifiers());

    assert !mB.isAbstract() : "Local Variable has abstract modifier?";
    assert !mB.isNative() : "Local Variable has native modifier?";
    assert (mB.getVisibility() == VisibilityModifier.NONE)
        : "Local Variable has Visibility modifier?";
    assert !mB.isStatic() : "Local Variable has static modifier?";
    assert !mB.isStrictFp() : "Local Variable has strictFp modifier?";
    assert !mB.isSynchronized() : "Local Variable has synchronized modifier?";

    for (VariableDeclarationFragment vdf : variableDeclarationFragments) {

      NameAndInitializer nameAndInitializer = getNamesAndInitializer(vdf);

      String name = nameAndInitializer.getName();
      name = addCounterToName(name);

      JVariableDeclaration newD =
          new JVariableDeclaration(
              fileLoc,
              convert(type),
              name,
              nameAndInitializer.getName(),
              getQualifiedName(name),
              nameAndInitializer.getInitializer(),
              mB.isFinal());

      variableDeclarations.add(newD);
    }

    forInitDeclarations.addAll(variableDeclarations);

    return null;
  }

  private String addCounterToName(String pName) {
    if (!scope.variableNameInUse(pName, pName)) {
      return pName;
    }
    int i = 0;
    String sep = "__";
    while (scope.variableNameInUse(pName + sep + i, pName)) {
      i++;
    }
    pName = pName + sep + i;
    return pName;
  }

  private JExpression convert(InstanceofExpression e) {
    FileLocation fileloc = getFileLocation(e);
    JExpression leftOperand;
    final Expression leftOperandOfExpression = e.getLeftOperand();

    leftOperand = convertExpressionWithoutSideEffects(leftOperandOfExpression);
    if (leftOperand instanceof ALiteralExpression
        || leftOperand instanceof JArraySubscriptExpression) {
      leftOperand =
          createTemporaryVariableWithName(
              leftOperandOfExpression, leftOperandOfExpression.toString() + "_");
    }
    JType typeOfRightOperand = convert(e.getRightOperand().resolveBinding());
    assert leftOperand instanceof JIdExpression : "There are other expressions for instanceOf?";
    assert (typeOfRightOperand instanceof JReferenceType)
        : "There are other types for this expression?";
    JIdExpression referenceVariableLeftOperand = (JIdExpression) leftOperand;
    JType instanceOfType = convert(e.resolveTypeBinding());

    assert instanceOfType instanceof JSimpleType;
    if (((JSimpleType) instanceOfType).getType() != JBasicType.UNSPECIFIED) {
      assert ((JSimpleType) instanceOfType).getType() == JBasicType.BOOLEAN
          : "InstanceofExpression is not always of type boolean!";
    }

    return createInstanceOfExpression(
        referenceVariableLeftOperand, (JReferenceType) typeOfRightOperand, fileloc);
  }

  /**
   * Creates an <code>instanceof</code> expression from the given parameters.
   *
   * <p>This creates an expression representing a statement of the following format:<br>
   * <code>pLeftOperand instanceof pRightOperand</code>.
   *
   * @param pLeftOperand the left operand of the <code>instanceof</code> statement
   * @param pRightOperand the right operand of the <code>instanceof</code> statement. The resulting
   *     expression will be evaluated to <code>true
   *     </code> if the the left operand's type is equal to this type or a subtype of this type
   * @param pLocation the file location of the expression
   * @return a {@link JExpression} representing an <code>instanceof</code> expression with the given
   *     parameters
   */
  JExpression createInstanceOfExpression(
      JExpression pLeftOperand, JReferenceType pRightOperand, FileLocation pLocation) {
    List<JType> allPossibleClasses;
    boolean isRightOperandArray;
    if ((pRightOperand instanceof JArrayType)) {
      isRightOperandArray = true;
      final JType elementType = ((JArrayType) pRightOperand).getElementType();
      allPossibleClasses = getSubClasses(elementType);

    } else {
      isRightOperandArray = false;
      allPossibleClasses = getSubClasses(pRightOperand);
    }
    if (pRightOperand instanceof JInterfaceType) {
      // if the given interface has no implementing classes there's no way the expression will be
      // true
      if (allPossibleClasses.isEmpty()) {
        return new JBooleanLiteralExpression(pLocation, false);
      }
    }

    return createInstanceOfDisjunction(
        pLeftOperand, allPossibleClasses, JSimpleType.getBoolean(), pLocation, isRightOperandArray);
  }

  /**
   * Returns all sub classes/implementing classes of the given class or interface. This includes the
   * given type itself, if it is a {@link JClassType}.
   *
   * @param pType the type to get all subclasses of
   * @return all sub classes/implementing classes of the given class or interface.
   */
  private List<JType> getSubClasses(JType pType) {

    // Do not return immutable list!
    if (pType instanceof JSimpleType) {
      List<JType> result = new ArrayList<>(1);
      result.add(pType);
      return result;
    }

    assert pType instanceof JInterfaceType || pType instanceof JClassType
        : "Unhandled type " + pType;

    Set<JClassType> subClassTypeSet;
    if (pType instanceof JInterfaceType) {
      subClassTypeSet = ((JInterfaceType) pType).getAllKnownImplementingClassesOfInterface();

    } else {
      JClassType classType = (JClassType) pType;

      subClassTypeSet = classType.getAllSubTypesOfClass();
      subClassTypeSet.add(classType);
    }

    return new ArrayList<>(subClassTypeSet);
  }

  private JExpression createInstanceOfDisjunction(
      JExpression pLeftOperand,
      List<JType> pConcreteTypes,
      JType pExpressionType,
      FileLocation pLocation,
      boolean isRightOperandArray) {

    final JType firstElement = pConcreteTypes.remove(FIRST);
    if (!(firstElement instanceof JClassType)) {
      if (isRightOperandArray) {
        return firstElement.equals(pLeftOperand.getExpressionType())
            ? new JBooleanLiteralExpression(pLocation, true)
            : new JBooleanLiteralExpression(pLocation, false);
      } else {
        throw new CFAGenerationRuntimeException(
            "Arguments for instance of must be reference type or null type");
      }
    }
    JExpression currentCondition;
    if (pLeftOperand instanceof JIdExpression) {
      currentCondition =
          convertClassRunTimeCompileTimeAccord(
              pLocation, (JIdExpression) pLeftOperand, (JClassType) firstElement);
    } else if (pLeftOperand instanceof JRunTimeTypeExpression) {
      currentCondition =
          new JRunTimeTypeEqualsType(
              pLeftOperand.getFileLocation(),
              (JRunTimeTypeExpression) pLeftOperand,
              (JClassType) firstElement);
    } else {
      throw new CFAGenerationRuntimeException(
          "Can only create instance of disjunction with JIdExpression or JRunTimeTypeExpression");
    }

    JRunTimeTypeEqualsType newCondition;

    for (JType currentSubType : pConcreteTypes) {
      if (pLeftOperand instanceof JIdExpression) {
        newCondition =
            convertClassRunTimeCompileTimeAccord(
                pLocation, (JIdExpression) pLeftOperand, (JClassType) currentSubType);
      } else if (pLeftOperand instanceof JRunTimeTypeExpression) {
        newCondition =
            new JRunTimeTypeEqualsType(
                pLeftOperand.getFileLocation(),
                (JRunTimeTypeExpression) pLeftOperand,
                (JClassType) currentSubType);
      } else {
        throw new CFAGenerationRuntimeException(
            "Can only create instance of disjunction with JIdExpression or JRunTimeTypeExpression");
      }
      currentCondition =
          new JBinaryExpression(
              pLocation,
              pExpressionType,
              currentCondition,
              newCondition,
              BinaryOperator.CONDITIONAL_OR);
    }

    return currentCondition;
  }

  private JRunTimeTypeEqualsType convertClassRunTimeCompileTimeAccord(
      FileLocation pFileloc, JIdExpression pDeclaration, JReferenceType pJReferenceType) {

    JRunTimeTypeExpression runTimeTyp = new JVariableRunTimeType(pFileloc, pDeclaration);

    return new JRunTimeTypeEqualsType(pFileloc, runTimeTyp, pJReferenceType);
  }

  private JAstNode convert(ThisExpression e) {
    return new JThisExpression(
        getFileLocation(e), (JClassOrInterfaceType) convert(e.resolveTypeBinding()));
  }

  private JAstNode convert(FieldAccess e) {

    if (isArrayLengthExpression(e)) {
      return createJArrayLengthExpression(e);
    }

    boolean canBeResolved = e.resolveFieldBinding() != null;

    if (canBeResolved) {
      scope.registerClass(e.resolveFieldBinding().getDeclaringClass());
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

    return new JFieldAccess(
        idExpIdentifier.getFileLocation(),
        idExpIdentifier.getExpressionType(),
        idExpIdentifier.getName(),
        (JFieldDeclaration) decl,
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

    final IMethodBinding binding = cIC.resolveConstructorBinding();

    final AnonymousClassDeclaration anonymousDeclaration = cIC.getAnonymousClassDeclaration();

    if (anonymousDeclaration != null) {
      scope.addAnonymousClassDeclaration(anonymousDeclaration);
    }

    if (binding != null) {
      scope.registerClass(binding.getDeclaringClass());
    }

    List<JExpression> params = getParameterExpressions(cIC);

    String name = getFullName(cIC);

    JConstructorDeclaration declaration = getConstructorDeclaration(cIC);

    JExpression functionName =
        new JIdExpression(
            getFileLocation(cIC), convert(cIC.resolveTypeBinding()), name, declaration);
    /*JIdExpression idExpression = (JIdExpression) functionName;


    if (idExpression.getDeclaration() != null) {
      // clone idExpression because the declaration in it is wrong
      // (it's the declaration of an equally named variable)
      // TODO this is ugly

      functionName =
          new JIdExpression(idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
    }*/

    if (declaration.getParameters().size() != params.size()) {
      throw new AssertionError(
          "Error in converting ClassInstanceCreation to JClassInstanceCreation. Amount of"
              + " parameters does not match.");
    }
    return new JClassInstanceCreation(
        getFileLocation(cIC),
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

      final JConstructorType jConstructorType =
          convertConstructorType(constructorBinding, pCIC.arguments());

      List<JParameterDeclaration> parameterDeclarations = new ArrayList<>();
      for (JType parameter : jConstructorType.getParameters()) {
        parameterDeclarations.add(
            new JParameterDeclaration(
                getFileLocation(pCIC),
                parameter,
                // TODO Naming for simple types
                parameter instanceof JClassOrInterfaceType
                    ? ((JClassOrInterfaceType) parameter).getSimpleName()
                    : ((JSimpleType) parameter).toString(),
                parameter instanceof JClassOrInterfaceType
                    ? ((JClassOrInterfaceType) parameter).getName()
                    : ((JSimpleType) parameter).toString(),
                parameter instanceof JClassType && ((JClassType) parameter).isFinal()));
      }

      return new JConstructorDeclaration(
          getFileLocation(pCIC),
          jConstructorType,
          fullName,
          simpleName,
          ImmutableList.copyOf(parameterDeclarations),
          mb.getVisibility(),
          mb.isStrictFp(),
          getDeclaringClassType(constructorBinding));
    }

    logger.logf(
        Level.FINEST,
        "No matching class for class instance creation \"%s\" in scope, trying to resolve by"
            + " imports",
        fullName);
    Set<ImportDeclaration> importDeclarations = getImportDeclarations(pCIC);

    Optional<Constructor<?>> constructorOptional;

    constructorOptional =
        matchConstructor(pCIC.getType().toString(), pCIC.arguments(), importDeclarations);

    if (constructorOptional.isPresent()) {
      JClassOrInterfaceType declaringClass = scope.getCurrentClassType();
      final JClassType jTypeFromConstructor =
          createOrFindJClassTypeFromConstructor(constructorOptional.orElseThrow());
      JConstructorType constructorType =
          new JConstructorType(
              jTypeFromConstructor,
              getJTypesOfParameters(pCIC.arguments()),
              constructorOptional.orElseThrow().isVarArgs());
      return new JConstructorDeclaration(
          getFileLocation(pCIC),
          constructorType,
          fullName,
          simpleName,
          createJParameterDeclarationsForArguments(pCIC.arguments()),
          getVisibilityModifierForConstructor(constructorOptional.orElseThrow()),
          jTypeFromConstructor.isStrictFp(),
          declaringClass);
    }

    // If nothing found
    return new JConstructorDeclaration(
        getFileLocation(pCIC),
        JConstructorType.createUnresolvableConstructorType(),
        fullName,
        simpleName,
        createJParameterDeclarationsForArguments(pCIC.arguments()),
        VisibilityModifier.NONE,
        false,
        JClassType.createUnresolvableType());
  }

  private JClassType createOrFindJClassTypeFromConstructor(final Constructor<?> pConstructor) {

    final VisibilityModifier visibilityModifierForConstructor =
        getVisibilityModifierForConstructor(pConstructor);

    Class<?> pClazz = pConstructor.getDeclaringClass();

    final TypeHierarchy typeHierarchy = scope.getTypeHierarchy();

    final JClassType jClassTypeFromClass =
        createJClassTypeFromClass(pClazz, visibilityModifierForConstructor, typeHierarchy);

    typeHierarchy.updateTypeHierarchy(jClassTypeFromClass);

    return jClassTypeFromClass;
  }

  public static JClassType createJClassTypeFromClass(
      final Class<?> pClazz,
      VisibilityModifier pVisibilityModifier,
      final TypeHierarchy pTypeHierarchy) {
    final String name = pClazz.getName();
    if (pTypeHierarchy.containsClassType(name)) {
      return pTypeHierarchy.getClassType(name);
    }

    final String simpleName = pClazz.getSimpleName();
    final Class<?> superclass = pClazz.getSuperclass();

    JClassType jTypeOfSuperClass;
    final JClassType typeOfObject = JClassType.getTypeOfObject();
    if ("java.lang.Object".equals(superclass.getName())) {
      jTypeOfSuperClass = typeOfObject;
    } else {
      final Set<JClassType> directSubClassesOfTypeObject = typeOfObject.getDirectSubClasses();
      final Optional<JClassType> superClassInTypeOfObject =
          directSubClassesOfTypeObject.stream()
              .filter(v -> v.getName().equals(superclass.getName()))
              .findFirst();
      if (superClassInTypeOfObject.isPresent()) {
        return superClassInTypeOfObject.orElseThrow();
      } else {
        jTypeOfSuperClass =
            createJClassTypeFromClass(superclass, VisibilityModifier.PUBLIC, pTypeHierarchy);
      }
    }

    final ModifierBean modifiers = ModifierBean.getModifiers(pClazz.getModifiers());
    return JClassType.valueOf(
        name,
        simpleName,
        pVisibilityModifier,
        modifiers.isFinal,
        modifiers.isAbstract,
        modifiers.isStrictFp,
        jTypeOfSuperClass,
        ImmutableSet.of());
  }

  private static Set<ImportDeclaration> getImportDeclarations(ASTNode astNode) {
    // Find CompilationUnit of class calling the constructor
    ASTNode compilationUnit = astNode.getParent();
    while (!(compilationUnit instanceof CompilationUnit)) {
      compilationUnit = compilationUnit.getParent();
    }
    // Make set of importDeclarations
    return FluentIterable.from((List<?>) ((CompilationUnit) compilationUnit).imports())
        .filter(ImportDeclaration.class)
        .toSet();
  }

  private VisibilityModifier getVisibilityModifierForConstructor(Constructor<?> pConstructor) {
    VisibilityModifier visibilityModifier;
    int i = pConstructor.toGenericString().indexOf(' ');
    String visibilityModifierString = pConstructor.toGenericString().substring(0, i);

    try {
      visibilityModifier = VisibilityModifier.valueOf(visibilityModifierString.toUpperCase());
    } catch (IllegalArgumentException ignored) {
      visibilityModifier = VisibilityModifier.NONE;
    }
    return visibilityModifier;
  }

  private static Optional<ImportDeclaration> getMatchingImportDeclaration(
      String pTypeAsString, Set<ImportDeclaration> pImportDeclarations) {
    for (ImportDeclaration importDeclaration : pImportDeclarations) {
      // case non wild card import declaration
      Pattern pattern = Pattern.compile("\\.[A-Z].*;$");
      Matcher matcher = pattern.matcher(importDeclaration.toString());
      String importedClass = "";
      while (matcher.find()) {
        importedClass =
            importDeclaration
                .toString()
                .substring(matcher.start(), matcher.end())
                .replace(".", "")
                .replace(";", "");
      }
      if (importedClass.equals(pTypeAsString)) {
        return Optional.of(importDeclaration);
      }
    }
    return Optional.empty();
  }

  private Optional<Constructor<?>> matchConstructor(
      String pClassName, List<?> pArguments, Set<ImportDeclaration> pImportDeclarations) {
    Optional<ImportDeclaration> matchingImportDeclaration =
        getMatchingImportDeclaration(pClassName, pImportDeclarations);
    Class<?> cls;
    if (!matchingImportDeclaration.isPresent()) {
      try {
        cls = Class.forName("java.lang." + pClassName);
      } catch (ClassNotFoundException pE) {
        return Optional.empty();
      }
    } else {
      try {
        cls =
            Class.forName(
                matchingImportDeclaration.orElseThrow().getName().getFullyQualifiedName());
      } catch (ClassNotFoundException pE) {
        return Optional.empty();
      }
    }
    Optional<List<Class<?>>> argumentsAsClassArray =
        convertArgumentListToClassList(pArguments, pImportDeclarations);
    if (argumentsAsClassArray.isEmpty()) {
      return Optional.empty();
    }
    for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
      boolean match = true;
      Class<?>[] parameterTypes = constructor.getParameterTypes();
      if (parameterTypes.length != argumentsAsClassArray.orElseThrow().size()) {
        continue;
      }
      for (int i = 0; i < parameterTypes.length; i++) {
        if (!parameterTypes[i].isAssignableFrom(argumentsAsClassArray.orElseThrow().get(i))) {
          match = false;
          break;
        }
      }
      if (match) {
        return Optional.of(constructor);
      }
    }
    return Optional.empty();
  }

  private Optional<List<Class<?>>> convertArgumentListToClassList(
      List<?> pArguments, Set<ImportDeclaration> pImportDeclarations) {
    List<Class<?>> result = new ArrayList<>(pArguments.size());
    for (Object argument : pArguments) {
      Optional<JSimpleDeclaration> optionalOfSimpleDeclaration =
          getJSimpleDeclarationOfArgument(argument);
      if (optionalOfSimpleDeclaration.isPresent()) {
        final JType simpleDeclarationType;
        if (argument instanceof ArrayAccess) {
          simpleDeclarationType =
              ((JArrayType) optionalOfSimpleDeclaration.orElseThrow().getType()).getElementType();
        } else {
          simpleDeclarationType = optionalOfSimpleDeclaration.orElseThrow().getType();
        }
        final Optional<Class<?>> classOfJType =
            getClassOfJType(simpleDeclarationType, pImportDeclarations);
        result.add(classOfJType.orElseThrow());
      } else if (argument instanceof Expression && !(argument instanceof InfixExpression)) {
        ITypeBinding binding = ((Expression) argument).resolveTypeBinding();
        if (binding != null) {
          final JType jType = typeConverter.convert(binding);
          result.add(getClassOfJType(jType, pImportDeclarations).orElseThrow());
        } else {
          // TODO Need better solution for Method Invocations
          return Optional.empty();
        }
      } else if (argument instanceof InfixExpression) {
        JBinaryExpression expression = (JBinaryExpression) convert((InfixExpression) argument);
        result.add(
            getClassOfJType(expression.getExpressionType(), pImportDeclarations).orElseThrow());
      } else {
        throw new AssertionError("Cannot find class of " + argument);
      }
    }
    if (pArguments.size() != result.size()) {
      throw new AssertionError("Error while converting arguments into array of classes.");
    }
    return Optional.of(ImmutableList.copyOf(result));
  }

  private Optional<JSimpleDeclaration> getJSimpleDeclarationOfArgument(final Object pArgument) {
    String argumentName;
    if (pArgument instanceof ArrayAccess) {
      argumentName = ((ArrayAccess) pArgument).getArray().toString();
    } else if (pArgument instanceof InfixExpression) {
      JExpression jExpression = convert((InfixExpression) pArgument);
      argumentName = jExpression.getExpressionType().toString();
    } else {
      argumentName = pArgument.toString();
    }
    JSimpleDeclaration simpleDeclaration = scope.lookupVariable(argumentName);
    if (simpleDeclaration != null) {
      return Optional.of(simpleDeclaration);
    }
    return Optional.empty();
  }

  @VisibleForTesting
  static Optional<Class<?>> getClassOfJType(
      JType pJType, Set<ImportDeclaration> pImportDeclarations) {
    if (pJType instanceof JSimpleType) {
      return Optional.of(getClassOfPrimitiveType((JSimpleType) pJType));
    }
    if (pJType instanceof JClassOrInterfaceType) {
      final String jTypeName = ((JClassOrInterfaceType) pJType).getName();
      Optional<ImportDeclaration> matchingImportDeclaration =
          getMatchingImportDeclaration(jTypeName, pImportDeclarations);
      Optional<Class<?>> cls = Optional.empty();
      if (matchingImportDeclaration.isPresent()) {
        try {
          cls =
              Optional.of(
                  Class.forName(
                      matchingImportDeclaration.orElseThrow().getName().getFullyQualifiedName()));
        } catch (ClassNotFoundException pE) {
          cls = Optional.empty();
        }
      }
      if (!cls.isPresent()) {
        try {
          cls = Optional.of(Class.forName(jTypeName));

        } catch (ClassNotFoundException pE) {
          cls = Optional.empty();
        }
      }
      if (!cls.isPresent()) {
        try {
          final String className = "java.lang." + jTypeName;
          cls = Optional.of(Class.forName(className));

        } catch (ClassNotFoundException pE) {

          cls = Optional.empty();
        }
      }

      if (!cls.isPresent()) {
        return cls;
      }
      return cls;
    }
    if (pJType instanceof JArrayType) {
      final JType elementTypeOfJArrayType = ((JArrayType) pJType).getElementType();
      Optional<Class<?>> typeOfArray =
          getClassOfJType(elementTypeOfJArrayType, pImportDeclarations);
      int dimensionsOfArray = ((JArrayType) pJType).getDimensions();
      Class<?> array = Array.newInstance(typeOfArray.orElseThrow(), 0).getClass();
      for (int i = 1; i < dimensionsOfArray; i++) {
        array = Array.newInstance(array, 0).getClass();
      }
      return Optional.of(array);
    }

    return Optional.empty();
  }

  @VisibleForTesting
  static Class<?> getClassOfPrimitiveType(JSimpleType pJSimpleType) {
    Class<?> cls;
    switch (pJSimpleType.getType()) {
      case BOOLEAN:
        cls = boolean.class;
        break;
      case CHAR:
        cls = char.class;
        break;
      case DOUBLE:
        cls = double.class;
        break;
      case FLOAT:
        cls = float.class;
        break;
      case INT:
        cls = int.class;
        break;
      case VOID:
        cls = void.class;
        break;
      case LONG:
        cls = long.class;
        break;
      case SHORT:
        cls = short.class;
        break;
      case BYTE:
        cls = byte.class;
        break;
      default:
        throw new AssertionError("Unknown primitive type " + pJSimpleType);
    }
    return cls;
  }

  private List<JType> getJTypesOfParameters(List<?> arguments) {
    List<JType> parameterList = new ArrayList<>();
    for (Object argument : arguments) {
      final JSimpleDeclaration jSimpleDeclaration = scope.lookupVariable(argument.toString());
      if (jSimpleDeclaration != null) {
        parameterList.add(jSimpleDeclaration.getType());
      } else if (argument instanceof Expression) {
        if (argument instanceof InfixExpression) {
          parameterList.add(convert((InfixExpression) argument).getExpressionType());
        } else {
          parameterList.add(typeConverter.convert((Expression) argument));
        }

      } else {
        throw new CFAGenerationRuntimeException("Could not process argument: " + argument + " .");
      }
    }
    return ImmutableList.copyOf(parameterList);
  }

  private List<JParameterDeclaration> createJParameterDeclarationsForArguments(List<?> arguments) {
    List<JParameterDeclaration> parameterList = new ArrayList<>();
    for (Object argument : arguments) {
      Optional<JSimpleDeclaration> simpleDeclarationOptional =
          getJSimpleDeclarationOfArgument(argument);
      if (simpleDeclarationOptional.isPresent()) {
        parameterList.add(
            convertSimpleDeclarationToParameterDeclaration(simpleDeclarationOptional.orElseThrow())
                .orElseThrow());
      } else if (argument instanceof Expression) {
        final String name;
        final String qualifiedName;
        JType jType;
        if (argument instanceof InfixExpression) {
          jType = convert((InfixExpression) argument).getExpressionType();
          if (jType instanceof JSimpleType) {
            name = ((JSimpleType) jType).toString();
            qualifiedName = ((JSimpleType) jType).toString();
          } else {
            name = ((JClassType) jType).getSimpleName();
            qualifiedName = ((JClassType) jType).getName();
          }
        } else if (argument instanceof StringLiteral) {
          jType = convert((StringLiteral) argument).getExpressionType();
          name = ((JClassType) jType).getSimpleName();
          qualifiedName = ((JClassType) jType).getName();
        } else {
          ITypeBinding binding = ((Expression) argument).resolveTypeBinding();
          jType = typeConverter.convert(binding);
          if (binding != null) {
            name = binding.getName();
            qualifiedName = binding.getQualifiedName();
          } else {
            name = jType.toString();
            qualifiedName = jType.toString();
          }
        }
        parameterList.add(
            new JParameterDeclaration(
                getFileLocation((ASTNode) argument),
                jType,
                name,
                qualifiedName,
                jType instanceof JClassType && ((JClassType) jType).isFinal()));
      } else {
        throw new CFAGenerationRuntimeException("Could not process argument: " + argument + " .");
      }
    }
    return ImmutableList.copyOf(parameterList);
  }

  private Optional<JParameterDeclaration> convertSimpleDeclarationToParameterDeclaration(
      JSimpleDeclaration js) {
    if (js instanceof JVariableDeclaration) {
      return Optional.of(
          new JParameterDeclaration(
              js.getFileLocation(),
              js.getType(),
              js.getName(),
              js.getQualifiedName(),
              ((JVariableDeclaration) js).isFinal()));
    } else if (js instanceof JParameterDeclaration) {
      return Optional.of((JParameterDeclaration) js);
    }
    throw new CFAGenerationRuntimeException(
        "Could not convert " + js.getName() + " to ParameterDeclaration");
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
        parameterDeclarations = ImmutableList.of();
        parameterTypes = ImmutableList.of();
      }

      for (JParameterDeclaration d : parameterDeclarations) {
        parameterTypes.add(d.getType());
      }

      final JConstructorType constructorType =
          new JConstructorType(returnType, parameterTypes, takesVarArgs);

      final String fullName =
          NameConverter.convertAnonymousClassConstructorName(anonymousDeclBinding, parameterTypes);
      final JClassOrInterfaceType declaringClass =
          convertClassOrInterfaceType(anonymousDeclBinding.getDeclaringClass());

      return new JConstructorDeclaration(
          fileLoc,
          constructorType,
          fullName,
          fullName,
          parameterDeclarations,
          VisibilityModifier.PRIVATE,
          isStrictFP,
          declaringClass);
    } else {
      logger.logf(
          Level.WARNING,
          "Binding for anonymous class %s can't be resolved.",
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
    return convert(p);
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

    return new JArrayInitializer(getFileLocation(initializer), initializerExpressions, type);
  }

  private JAstNode convert(ArrayCreation Ace) {

    FileLocation fileloc = getFileLocation(Ace);
    JArrayInitializer initializer =
        (JArrayInitializer) convertExpressionWithoutSideEffects(Ace.getInitializer());

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
    return new JArrayCreationExpression(fileloc, type, initializer, length);
  }

  private JAstNode convert(ArrayAccess e) {

    JExpression subscriptExpression = convertExpressionWithoutSideEffects(e.getArray());
    JExpression index = convertExpressionWithoutSideEffects(e.getIndex());

    assert subscriptExpression != null;
    assert index != null;

    return new JArraySubscriptExpression(
        getFileLocation(e), convert(e.resolveTypeBinding()), subscriptExpression, index);
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
          // TODO Prototype for enum constant expression, investigate
          return new JEnumConstantExpression(
              getFileLocation(e),
              (JClassType) convert(e.resolveTypeBinding()),
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

    return (lengthBinding != null
            && lengthBinding.getName().equals(lengthExpression)
            && isArrayType(qualifierExpression.getExpressionType()))
        || isMainArgumentArray(e, qualifierExpression);
  }

  private boolean isMainArgumentArray(QualifiedName e, JExpression qualifierExpression) {
    final IBinding lengthBinding = e.resolveBinding();

    if (qualifierExpression instanceof JIdExpression) {
      JSimpleDeclaration qualifierDecl = ((JIdExpression) qualifierExpression).getDeclaration();

      // check that no binding exists (special case for main argument array) and that
      // the given qualifier is an array and a parameter
      return lengthBinding == null
          && qualifierDecl instanceof JParameterDeclaration
          && isArrayType(qualifierDecl.getType());

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

  private JAstNode convertQualifiedVariableIdentificationExpression(QualifiedName e) {

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

    return new JFieldAccess(
        idExpIdentifier.getFileLocation(),
        idExpIdentifier.getExpressionType(),
        idExpIdentifier.getName(),
        (JFieldDeclaration) decl,
        (JIdExpression) qualifier);
  }

  /**
   * Transforms the method a method calls. This is used to solve dynamic Binding.
   *
   * @param newFunctionEntryNode The EntryNode of the new Method Call
   * @param oldMethodCall static bound call of the old Expression
   * @return a Method Call which calling the method represented by newFunctionEntryNode
   */
  public JMethodInvocationExpression convert(
      FunctionEntryNode newFunctionEntryNode, JMethodInvocationExpression oldMethodCall) {

    JMethodDeclaration declaration =
        (JMethodDeclaration) newFunctionEntryNode.getFunctionDefinition();

    String name = newFunctionEntryNode.getFunctionName();

    JIdExpression methodName =
        new JIdExpression(
            oldMethodCall.getFileLocation(), JSimpleType.getUnspecified(), name, declaration);

    if (oldMethodCall instanceof JReferencedMethodInvocationExpression) {
      return new JReferencedMethodInvocationExpression(
          oldMethodCall.getFileLocation(),
          oldMethodCall.getExpressionType(),
          methodName,
          oldMethodCall.getParameterExpressions(),
          declaration,
          ((JReferencedMethodInvocationExpression) oldMethodCall).getReferencedVariable());
    } else {
      return new JMethodInvocationExpression(
          oldMethodCall.getFileLocation(),
          oldMethodCall.getExpressionType(),
          methodName,
          oldMethodCall.getParameterExpressions(),
          declaration);
    }
  }

  private JAstNode convert(MethodInvocation mi) {

    final IMethodBinding methodBinding = mi.resolveMethodBinding();
    final ModifierBean mb = methodBinding != null ? ModifierBean.getModifiers(methodBinding) : null;
    JClassOrInterfaceType declaringClassType = null;

    if (methodBinding != null) {
      ITypeBinding declaringClass = methodBinding.getDeclaringClass();
      declaringClassType = (JClassOrInterfaceType) convert(declaringClass);
      scope.registerClass(declaringClass);
    }

    @SuppressWarnings("unchecked")
    List<Expression> p = mi.arguments();

    List<JExpression> params;
    if (!p.isEmpty()) {
      params = convert(p);
    } else {
      params = ImmutableList.of();
    }

    JExpression methodName = convertExpressionWithoutSideEffects(mi.getName());

    JMethodDeclaration declaration = null;
    JExpression referencedVariableName = null;

    if (mb != null && !mb.isStatic && mi.getExpression() != null) {
      referencedVariableName = convertExpressionWithoutSideEffects(mi.getExpression());
    }

    if (methodName instanceof JIdExpression) {
      JIdExpression idExpression = (JIdExpression) methodName;
      String name = idExpression.getName();
      declaration = scope.lookupMethod(name);

      if (idExpression.getDeclaration() != null) {
        // TODO this is ugly

        methodName =
            new JIdExpression(
                idExpression.getFileLocation(),
                idExpression.getExpressionType(),
                name,
                declaration);
      }
    }

    if (declaration == null) {

      if (methodBinding != null) {
        assert mb != null;
        declaration =
            scope.createExternMethodDeclaration(
                convertMethodType(methodBinding),
                methodName.toASTString(),
                methodBinding.getName(),
                VisibilityModifier.PUBLIC,
                mb.isFinal(),
                mb.isAbstract(),
                mb.isStatic(),
                mb.isNative(),
                mb.isSynchronized(),
                mb.isStrictFp(),
                declaringClassType);

      } else {
        declaration = JMethodDeclaration.createUnresolvedMethodDeclaration();
      }
    }

    if (!(referencedVariableName instanceof JIdExpression)) {
      return new JMethodInvocationExpression(
          getFileLocation(mi), convert(mi.resolveTypeBinding()), methodName, params, declaration);
    } else {
      return new JReferencedMethodInvocationExpression(
          getFileLocation(mi),
          convert(mi.resolveTypeBinding()),
          methodName,
          params,
          declaration,
          (JIdExpression) referencedVariableName);
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

    // TODO Complete declaration by finding all Bindings
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

    return new JIdExpression(
        getFileLocation(e), convert(e.resolveTypeBinding()), name, declaration);
  }

  private JAstNode convertSimpleVariable(SimpleName e, IVariableBinding vb) {

    if (((IVariableBinding) e.resolveBinding()).isEnumConstant()) {
      // TODO Prototype for enum constant expression, investigate
      return new JEnumConstantExpression(
          getFileLocation(e),
          (JClassType) convert(e.resolveTypeBinding()),
          NameConverter.convertName((IVariableBinding) e.resolveBinding()));
    }

    String name = NameConverter.convertName(vb);

    JSimpleDeclaration declaration = scope.lookupVariable(name);

    if (declaration == null) {
      declaration = createVariableDeclarationFromBinding(e, vb);
    }

    assert name.equals(declaration.getOrigName()) : "Created a false declaration for " + e;

    JType type = convert(e.resolveTypeBinding());

    return new JIdExpression(getFileLocation(e), type, declaration.getName(), declaration);
  }

  private JSimpleDeclaration createVariableDeclarationFromBinding(
      SimpleName e, IVariableBinding vb) {

    if (!vb.isField()) {
      throw new CFAGenerationRuntimeException(
          "Declaration of Variable " + e.getIdentifier() + " not found.", e);
    }

    String name = NameConverter.convertName(vb);
    String simpleName = vb.getName();

    JFieldDeclaration decl;

    ModifierBean mb = ModifierBean.getModifiers(vb.getModifiers());
    JType type = convert(e.resolveTypeBinding());

    decl =
        scope.createExternFieldDeclaration(
            type,
            name,
            simpleName,
            mb.isFinal(),
            mb.isStatic(),
            mb.getVisibility(),
            mb.isVolatile(),
            mb.isTransient());

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
          convertExpressionWithSideEffects(
              e.getRightHandSide()); // right-hand side may have a method call

      if (rightHandSide instanceof JExpression) {
        // a = b
        return new JExpressionAssignmentStatement(
            fileLoc, leftHandSide, (JExpression) rightHandSide);

      } else if (rightHandSide instanceof JMethodInvocationExpression) {
        // a = f()
        return new JMethodInvocationAssignmentStatement(
            fileLoc, leftHandSide, (JMethodInvocationExpression) rightHandSide);

      } else if (rightHandSide instanceof JAssignment) {

        // TODO We need the assignments to be evaluated from left to right
        // e. g x = 1;  x = ++x + x; x is 4; x = x + ++x; x is 3
        preSideAssignments.add(rightHandSide);

        return new JExpressionAssignmentStatement(
            fileLoc, leftHandSide, ((JAssignment) rightHandSide).getLeftHandSide());

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
    final String invalidTypeMsg =
        "Invalid type '" + type + "' for assignment with binary operation.";

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
      throw new CFAGenerationRuntimeException("Invalid operator " + op + " for boolean assignment");
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
      throw new CFAGenerationRuntimeException("Invalid operator " + op + " for number assignment.");
    }
  }

  private JExpression convert(BooleanLiteral e) {
    return new JBooleanLiteralExpression(getFileLocation(e), e.booleanValue());
  }

  private JAstNode convert(PrefixExpression e) {

    PrefixExpression.Operator op = e.getOperator();

    if (op.equals(PrefixExpression.Operator.INCREMENT)
        || op.equals(PrefixExpression.Operator.DECREMENT)) {

      return handlePreFixIncOrDec(e, op);
    }

    JExpression operand = convertExpressionWithoutSideEffects(e.getOperand());
    FileLocation fileLoc = getFileLocation(e);

    return new JUnaryExpression(fileLoc, convert(e.resolveTypeBinding()), operand, convert(op));
  }

  private JAstNode convert(PostfixExpression e) {
    PostfixExpression.Operator op = e.getOperator();
    return handlePostFixIncOrDec(e, op);
  }

  private JAstNode handlePostFixIncOrDec(PostfixExpression e, PostfixExpression.Operator op) {

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
    JBinaryExpression preExp = new JBinaryExpression(fileLoc, type, operand, preOne, postOp);

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
    JBinaryExpression preExp = new JBinaryExpression(fileLoc, type, operand, preOne, preOp);
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
      throw new CFAGenerationRuntimeException("Could not proccess Operator:" + op + ".");
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

    if (type.equals(JSimpleType.getUnspecified())) {
      if (op == BinaryOperator.STRING_CONCATENATION) {
        if (scope.containsClassType("java.lang.String")) {
          type = scope.getClassType("java.lang.String");
        }
        // TODO Create java.lang.String JType
        // TODO Replace with switch when adding more cases
      }
    }

    JExpression binaryExpression =
        new JBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

    // a x b x c x d is being translated to (((a x b) x c) x d)
    if (e.hasExtendedOperands()) {

      @SuppressWarnings("unchecked")
      List<Expression> extOperands = e.extendedOperands();

      for (Expression extendedOperand : extOperands) {
        binaryExpression =
            new JBinaryExpression(
                fileLoc,
                type,
                binaryExpression,
                convertExpressionWithoutSideEffects(extendedOperand),
                op);
      }
    }

    return binaryExpression;
  }

  // pType is the type of the operands of the operation
  private BinaryOperator convert(InfixExpression.Operator op, JType pOp1Type, JType pOp2Type) {
    final String invalidTypeMsg =
        "Invalid operation '" + pOp1Type + " " + op + " " + pOp2Type + "'";
    JBasicType basicTypeOp1 = null;
    JBasicType basicTypeOp2 = null;

    if (pOp1Type instanceof JSimpleType) {
      basicTypeOp1 = ((JSimpleType) pOp1Type).getType();
    }

    if (pOp2Type instanceof JSimpleType) {
      basicTypeOp2 = ((JSimpleType) pOp2Type).getType();
    }

    JBasicType jBasicType;
    if (pOp1Type instanceof JClassType && basicTypeOp2 != null) {
      jBasicType = unboxJClassType((JClassType) pOp1Type).orElse(null);
      if (jBasicType == basicTypeOp2) {
        basicTypeOp1 = jBasicType;
      }
    } else if (pOp2Type instanceof JClassType && basicTypeOp1 != null) {
      jBasicType = unboxJClassType((JClassType) pOp2Type).orElse(null);
      if (jBasicType == basicTypeOp1) {
        basicTypeOp2 = jBasicType;
      }
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

  @VisibleForTesting
  public static Optional<JBasicType> unboxJClassType(JClassType pJClassType) {
    return Optional.ofNullable(unboxingMap.getOrDefault(pJClassType.getName(), null));
  }

  private boolean isNumericCompatible(JBasicType pType) {
    return pType != null
        && (pType.isIntegerType()
            || pType.isFloatingPointType()
            || pType == JBasicType.UNSPECIFIED);
  }

  private boolean isBooleanCompatible(JBasicType pType) {
    return pType == JBasicType.BOOLEAN || pType == JBasicType.UNSPECIFIED;
  }

  private boolean isStringType(JType t) {
    return t instanceof JClassOrInterfaceType
        && ((JClassOrInterfaceType) t).getName().equals("java.lang.String");
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
    } else if (op.equals(InfixExpression.Operator.CONDITIONAL_OR)) {
      return BinaryOperator.CONDITIONAL_OR;
    } else if (op.equals(InfixExpression.Operator.CONDITIONAL_AND)) {
      return BinaryOperator.CONDITIONAL_AND;
    } else {
      throw new CFAGenerationRuntimeException("Could not process Operator: " + op);
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
      throw new CFAGenerationRuntimeException("Could not proccess Operator: " + op);
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
        if (valueStr.endsWith("L") || valueStr.endsWith("l")) {
          valueStr = valueStr.substring(0, valueStr.length() - 1);
        }
        if (valueStr.startsWith("0x")) {
          valueStr = valueStr.substring(2);
          return new JIntegerLiteralExpression(
              getFileLocation(e), BigInteger.valueOf(Long.parseLong(valueStr, 16)));
        } else {
          return new JIntegerLiteralExpression(
              getFileLocation(e), BigInteger.valueOf(Long.parseLong(valueStr)));
        }
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
      throw new CFAGenerationRuntimeException(pExpr + " was not correctly processed.", pExpr);
    }

    FileLocation fileLoc = getFileLocation(pExpr);

    // TODO correct JMethodExpression when standard Library will be supported

    List<JExpression> parameters = ImmutableList.of();

    JInterfaceType iteratorTyp = JInterfaceType.createUnresolvableType();

    JIdExpression name = new JIdExpression(fileLoc, iteratorTyp, "iterator", null);

    JReferencedMethodInvocationExpression mi =
        new JReferencedMethodInvocationExpression(
            fileLoc, iteratorTyp, name, parameters, null, (JIdExpression) iterable);

    // create Iterator Declaration
    String varName = "it_";
    int i = 0;
    while (scope.variableNameInUse(varName + i, varName + i)) {
      i++;
    }
    varName += i;

    JVariableDeclaration decl =
        new JVariableDeclaration(
            fileLoc, iteratorTyp, varName, varName, getQualifiedName(varName), null, NOT_FINAL);

    scope.registerDeclarationOfThisClass(decl);

    // Add Declaration before Assignment
    preSideAssignments.add(decl);

    enhancedForLoopIterator = new JIdExpression(decl.getFileLocation(), iteratorTyp, varName, decl);

    // Create Assignment it = x.iterators();
    return new JMethodInvocationAssignmentStatement(fileLoc, enhancedForLoopIterator, mi);
  }

  public JExpression createIteratorCondition(Expression e) {

    FileLocation fileloc = enhancedForLoopIterator.getFileLocation();

    JType type = JSimpleType.getBoolean();

    JExpression name = new JIdExpression(fileloc, type, "hasNext", null);

    List<JExpression> parameters = ImmutableList.of();

    JReferencedMethodInvocationExpression mi =
        new JReferencedMethodInvocationExpression(
            fileloc, type, name, parameters, null, enhancedForLoopIterator);

    return addSideassignmentsForExpressionsWithoutMethodInvocationSideEffects(mi, e);
  }

  public JMethodInvocationAssignmentStatement assignParameterToNextIteratorItem(
      SingleVariableDeclaration formalParameter) {

    FileLocation fileLoc = getFileLocation(formalParameter);

    JSimpleDeclaration param =
        scope.lookupVariable(NameConverter.convertName(formalParameter.resolveBinding()));

    if (param == null) {
      throw new CFAGenerationRuntimeException(
          "Formal Parameter " + formalParameter + " could not be proccessed", formalParameter);
    }

    JIdExpression paramIdExpr = new JIdExpression(fileLoc, param.getType(), param.getName(), param);

    // TODO correct JMethodExpression when standard Library will be supported

    List<JExpression> parameters = ImmutableList.of();

    JIdExpression name = new JIdExpression(fileLoc, param.getType(), "next", null);

    JReferencedMethodInvocationExpression mi =
        new JReferencedMethodInvocationExpression(
            fileLoc, param.getType(), name, parameters, null, enhancedForLoopIterator);

    enhancedForLoopIterator = null;

    return new JMethodInvocationAssignmentStatement(fileLoc, paramIdExpr, mi);
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
   * Converts a Expression into the intern AST which is required to give a boolean Type back.
   *
   * @param e an expression with a boolean type
   * @return intern AST representing JDT expression
   */
  public JExpression convertBooleanExpression(Expression e) {

    JExpression exp = convertExpressionWithoutSideEffects(e);

    if (!isBooleanExpression(exp)) {
      // TODO: Is there even such a case?
      JExpression zero = new JBooleanLiteralExpression(exp.getFileLocation(), false);
      return new JBinaryExpression(
          exp.getFileLocation(), exp.getExpressionType(), exp, zero, BinaryOperator.NOT_EQUALS);
    }

    return exp;
  }

  private static final ImmutableSet<BinaryOperator> BOOLEAN_BINARY_OPERATORS =
      Sets.immutableEnumSet(
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
   * Checks if the given Expression returns a Value of boolean Type.
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

      // TODO If parser support for Wrapper classes is implemented,
      //      We also need to check for BOOLEAN class
      JType type = e.getExpressionType();

      return type instanceof JSimpleType && ((JSimpleType) type).getType() == JBasicType.BOOLEAN;
    }
  }

  JObjectReferenceReturn getConstructorObjectReturn(ITypeBinding declaringClass) {

    assert declaringClass.isClass() || declaringClass.isEnum()
        : declaringClass.getName() + " is not a Class";

    JClassType objectReturnType = (JClassType) convert(declaringClass);

    return new JObjectReferenceReturn(FileLocation.DUMMY, objectReturnType);
  }

  public JRunTimeTypeEqualsType convertClassRunTimeCompileTimeAccord(
      FileLocation fileloc,
      JMethodInvocationExpression methodInvocation,
      JClassOrInterfaceType classType) {

    if (methodInvocation instanceof JReferencedMethodInvocationExpression) {
      JIdExpression referencedVariable =
          ((JReferencedMethodInvocationExpression) methodInvocation).getReferencedVariable();

      JRunTimeTypeExpression methodReturnType =
          new JVariableRunTimeType(fileloc, referencedVariable);

      return new JRunTimeTypeEqualsType(fileloc, methodReturnType, classType);

    } else {
      return new JRunTimeTypeEqualsType(
          fileloc, new JThisExpression(fileloc, methodInvocation.getDeclaringType()), classType);
    }
  }

  public void assignRunTimeClass(
      JReferencedMethodInvocationExpression methodInvocation, JClassInstanceCreation functionCall) {
    JClassOrInterfaceType returnType = functionCall.getExpressionType();

    methodInvocation.setRunTimeBinding(returnType);
  }

  public JExpressionAssignmentStatement getBooleanAssign(
      JLeftHandSide pLeftHandSide, boolean booleanLiteral) {
    return new JExpressionAssignmentStatement(
        pLeftHandSide.getFileLocation(),
        pLeftHandSide,
        new JBooleanLiteralExpression(pLeftHandSide.getFileLocation(), booleanLiteral));
  }

  /**
   * Creates a default Constructor AST for a class represented by the class Binding.
   *
   * @param classBinding representation of the class a constructor should be constructed for
   * @return a {@link JMethodDeclaration} representing the default constructor of the given class
   *     binding
   */
  public JMethodDeclaration createDefaultConstructor(ITypeBinding classBinding) {

    List<JType> paramTypes = ImmutableList.of();
    List<JParameterDeclaration> param = ImmutableList.of();

    JConstructorType type =
        new JConstructorType((JClassType) convert(classBinding), paramTypes, false);

    String simpleName = getSimpleName(classBinding);

    return new JConstructorDeclaration(
        FileLocation.DUMMY,
        type,
        NameConverter.convertDefaultConstructorName(classBinding),
        simpleName,
        param,
        VisibilityModifier.PUBLIC,
        false,
        type.getReturnType());
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

    public ModifierBean(
        boolean pIsFinal,
        boolean pIsStatic,
        boolean pIsVolatile,
        boolean pIsTransient,
        VisibilityModifier pVisibility,
        boolean pIsNative,
        boolean pIsAbstract,
        boolean pIsStrictFp,
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

      return new ModifierBean(
          isFinal,
          isStatic,
          isVolatile,
          isTransient,
          visibility,
          isNative,
          isAbstract,
          isStrictFp,
          isSynchronized);
    }

    public static ModifierBean getModifiers(ITypeBinding pBinding) {

      // This int value is the bit-wise or of Modifier constants
      int modifiers = pBinding.getModifiers();

      assert pBinding.isClass()
              || pBinding.isEnum()
              || pBinding.isInterface()
              || pBinding.isAnnotation()
              || pBinding.isRecovered()
          : "This type can't have modifiers";

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

      return new ModifierBean(
          isFinal,
          isStatic,
          isVolatile,
          isTransient,
          visibility,
          isNative,
          isAbstract,
          isStrictFp,
          isSynchronized);
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
