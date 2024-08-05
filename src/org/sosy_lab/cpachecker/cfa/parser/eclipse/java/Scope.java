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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JConstructorDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JConstructorType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * Provides a symbol table that maps variable and methods to their declaration (if a name is visible
 * in the current scope). Additionally, it tracks classes that still need to be parsed.
 */
class Scope {

  private static final String RETURN_VAR_NAME = "__retval__";

  // Stores all found class and reference types
  private final TypeHierarchy typeHierarchy;

  // symbolic table for  Variables and other Declarations
  private final ArrayDeque<Map<String, JSimpleDeclaration>> varsStack = new ArrayDeque<>();
  private final ArrayDeque<Map<String, JSimpleDeclaration>> varsList = new ArrayDeque<>();
  private final Deque<Map<String, JSimpleDeclaration>> varsStackWitNewNames = new ArrayDeque<>();
  private final ArrayDeque<Map<String, JSimpleDeclaration>> varsListWithNewNames =
      new ArrayDeque<>();

  // Stores all found methods and constructors
  private Map<String, JMethodDeclaration> methods;

  // Stores all found field declarations
  private Map<String, JFieldDeclaration> fields;

  // Stores current class and method
  private String currentMethodName = null;
  private Optional<JVariableDeclaration> returnVariable;

  // Stores enclosing classes
  private final Deque<JClassOrInterfaceType> classStack = new ArrayDeque<>();

  // fully Qualified main Class (not the ast name, but the real name with . instead of _)
  private final String fullyQualifiedMainClassName;

  // Track and deliver Classes that need to be parsed
  private final Queue<String> classesToBeParsed = new ConcurrentLinkedQueue<>();
  private final Queue<AnonymousClassDeclaration> localClassesToBeParsed =
      new ConcurrentLinkedQueue<>();

  private final Set<String> registeredClasses = new HashSet<>();

  private final LogManager logger;

  /**
   * Creates the Scope. It stores Information about the program as well as creating symbolic tables
   * to solve declarations.
   *
   * @param pFullyQualifiedMainClassName Name of the main Class of program. *
   * @param pTypeHierarchy Type Hierarchy of program created by {@link TypeHierachyCreator}
   * @param pLogger a logger
   */
  public Scope(
      String pFullyQualifiedMainClassName, TypeHierarchy pTypeHierarchy, LogManager pLogger) {

    fullyQualifiedMainClassName = pFullyQualifiedMainClassName;
    enterProgramScope();
    registeredClasses.add(fullyQualifiedMainClassName); // Register Main Class

    methods = pTypeHierarchy.getMethodDeclarations();
    fields = pTypeHierarchy.getFieldDeclarations();

    typeHierarchy = pTypeHierarchy;

    logger = pLogger;
  }

  private void enterProgramScope() {
    varsStack.addLast(new HashMap<>());
    varsList.addLast(varsStack.getLast());
    varsStackWitNewNames.addLast(new HashMap<>());
    varsListWithNewNames.addLast(varsStackWitNewNames.getLast());
  }

  /**
   * Returns true, iff Scope is not within a Class or method. In all other cases, returns false.
   *
   * @return true, iff Scope is not within method or Class.
   */
  public boolean isProgramScope() {
    return varsStack.size() == 1 && classStack.isEmpty();
  }

  /**
   * Returns true, if Scope is in the top-Level class and not within a method. In all other cases
   * false.
   *
   * @return true, iff Scope is within top-level class.
   */
  public boolean isTopClassScope() {
    return varsStack.size() == 1 && classStack.size() == 1;
  }

  /**
   * Is Called to indicate that a Visitor using this Scope enters a method in iterating the JDT AST:
   *
   * @param methodDef indicates method the Visitor enters.
   */
  public void enterMethod(JMethodDeclaration methodDef) {
    currentMethodName = methodDef.getOrigName();
    returnVariable = Optional.ofNullable(createFunctionReturnVariable(methodDef));

    enterBlock();
  }

  private JVariableDeclaration createFunctionReturnVariable(JMethodDeclaration pMethod) {
    FileLocation fileLocation = pMethod.getFileLocation();
    JType returnType = pMethod.getType().getReturnType();
    String qualifiedReturnVarName = createQualifiedName(RETURN_VAR_NAME);

    if (JSimpleType.getVoid().equals(returnType)) {
      return null;
    }

    return new JVariableDeclaration(
        fileLocation,
        returnType,
        RETURN_VAR_NAME,
        RETURN_VAR_NAME,
        qualifiedReturnVarName,
        null,
        false);
  }

  public Optional<JVariableDeclaration> getReturnVariable() {
    checkState(returnVariable != null);
    return returnVariable;
  }

  /**
   * Returns a fully qualified name for the given variable using the current scope information.
   *
   * @param pVariableName the simple name to create a fully qualified name of.
   * @return the fully qualified name for the given variable name, based on the current scope
   */
  public String createQualifiedName(String pVariableName) {

    /* Old way of creating qualified names - this includes the class name twice.
     * But ValueAnalysisState.MemoryLocation uses names equal to the uncommented code below.
     * As long as no need for the upper format exists, the lower one should be used to prevent
     * the need of workarounds.
     */
    // return scope.getCurrentClassType().getName()
    //    + "_" + scope.getCurrentMethodName()
    //    + "::" + var;
    return NameConverter.createQualifiedName(getCurrentMethodName(), pVariableName);
  }

  /**
   * Is Called to indicate that a Visitor using this Scope enters a class in iterating the JDT AST:
   *
   * @param enteredClassType indicates the class the Visitor enters.
   */
  public void enterClass(JClassOrInterfaceType enteredClassType) {
    if (!typeHierarchy.containsType(enteredClassType)) {
      throw new CFAGenerationRuntimeException(
          "Could not find Type for Class" + enteredClassType.getName());
    }

    classStack.push(enteredClassType);
  }

  /**
   * Indicates that the visitor using this scope leaves current class while traversing the JDT AST.
   */
  public void leaveClass() {

    if (classStack.isEmpty()) {
      throw new CFAGenerationRuntimeException(
          "Could not find enclosing class of nested class " + classStack.peek());
    }

    classStack.pop();
  }

  /**
   * Indicates that the Visitor using this scope leaves current Method while traversing the JDT AST.
   */
  public void leaveMethod() {
    checkState(!isTopClassScope());
    varsStack.removeLast();

    while (varsList.size() > varsStack.size()) {
      varsList.removeLast();
    }
    while (varsListWithNewNames.size() > varsStackWitNewNames.size()) {
      varsListWithNewNames.removeLast();
    }

    currentMethodName = null;
  }

  /** Indicates that the Visitor using this scope enters a Block while traversing the JDT AST. */
  public void enterBlock() {
    varsStack.addLast(new HashMap<>());
    varsList.addLast(varsStack.getLast());
    varsStackWitNewNames.addLast(new HashMap<>());
    varsListWithNewNames.addLast(varsStackWitNewNames.getLast());
  }

  /** Indicates that the Visitor using this scope leaves a Block while traversing the JDT AST. */
  public void leaveBlock() {
    checkState(varsStack.size() > 2);
    varsStack.removeLast();
    varsStackWitNewNames.removeLast();
  }

  /**
   * Checks if the name given in the parameters has already a declaration to which it is linked.
   *
   * @param name Given name to be checked.
   * @param origName If the name has another Identification, it can also be given with this
   *     parameter.
   * @return Returns true, if the name is already in use, else false.
   */
  public boolean variableNameInUse(String name, String origName) {
    checkNotNull(name);
    checkNotNull(origName);

    Iterator<Map<String, JSimpleDeclaration>> it = varsListWithNewNames.descendingIterator();
    while (it.hasNext()) {
      Map<String, JSimpleDeclaration> vars = it.next();

      JSimpleDeclaration binding = vars.get(name);
      if (binding != null && binding.getName().equals(name)) {
        return true;
      }
      binding = vars.get(name);
      if (binding != null && binding.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the variable declaration with the name given as parameter.
   *
   * @param name Name of the Variable which declaration is to be returned.
   * @return declaration of given name, or null, if declaration was not found.
   */
  public JSimpleDeclaration lookupVariable(String name) {
    checkNotNull(name);

    Iterator<Map<String, JSimpleDeclaration>> it = varsStack.descendingIterator();
    while (it.hasNext()) {
      Map<String, JSimpleDeclaration> vars = it.next();

      JSimpleDeclaration binding = vars.get(name);
      if (binding != null) {
        return binding;
      }
    }

    return fields.getOrDefault(name, null);
  }

  /**
   * Returns the method declaration with the name given as parameter.
   *
   * @param name name of the method which declaration is to be returned.
   * @return declaration of given name, or null, if declaration was not found.
   */
  public JMethodDeclaration lookupMethod(String name) {
    return methods.get(checkNotNull(name));
  }

  /**
   * Checks if the method declaration with the name given as parameter is already in use.
   *
   * @param name name of the method.
   * @return true, if found, else false.
   */
  public boolean isMethodRegistered(String name) {
    return methods.containsKey(checkNotNull(name));
  }

  public void registerDeclarationOfThisClass(JSimpleDeclaration declaration) {

    checkArgument(
        declaration instanceof JVariableDeclaration || declaration instanceof JParameterDeclaration,
        "Tried to register a declaration which does not define "
            + "a name in the standard namespace: %s",
        declaration);

    checkArgument(
        !(declaration instanceof JFieldDeclaration),
        "Can't register a field declaration, it has to be updated within the type Hierarchy");

    String name = declaration.getOrigName();
    assert name != null;

    Map<String, JSimpleDeclaration> vars = varsStack.getLast();
    Map<String, JSimpleDeclaration> varsWithNewNames = varsStackWitNewNames.getLast();

    if (isProgramScope()) {
      throw new CFAGenerationRuntimeException(
          "Could not find Class for Declaration " + declaration.getName(), declaration);
    }

    // multiple declarations of the same variable are disallowed
    if (vars.containsKey(declaration.getName())) {
      throw new CFAGenerationRuntimeException(
          "Variable " + name + " already declared", declaration);
    }

    vars.put(name, declaration);
    varsWithNewNames.put(declaration.getName(), declaration);
  }

  public String getCurrentMethodName() {
    return currentMethodName;
  }

  @Override
  public String toString() {
    return "Functions: " + Joiner.on(' ').join(methods.keySet());
  }

  public void registerClass(ITypeBinding classBinding) {
    String className = NameConverter.convertClassOrInterfaceToFullName(classBinding);
    String topClassName = getTopLevelClass(classBinding);

    Queue<JClassOrInterfaceType> toBeAdded = new ArrayDeque<>();

    if (!registeredClasses.contains(className)) {

      if (!registeredClasses.contains(topClassName)) {
        classesToBeParsed.add(topClassName);
      }

      registeredClasses.add(className);

    } else {
      // If top Class already added, it is unnecessary to search for subTypes
      // unless its the main Class
      if (!fullyQualifiedMainClassName.equals(className)) {
        return;
      }
    }

    // Sub Classes need to be parsed for dynamic Binding
    JClassOrInterfaceType type = typeHierarchy.getType(className);

    // TODO Check if there is a way to be more precise
    if (type == null) {
      logger.log(Level.WARNING, "Could not resolve type of ", className);
      type =
          classBinding.isInterface()
              ? JInterfaceType.createUnresolvableType()
              : JClassType.createUnresolvableType();
    }

    toBeAdded.addAll(type.getAllSubTypesOfType());

    for (JClassOrInterfaceType subClassType : toBeAdded) {

      String name = subClassType.getName();

      if (registeredClasses.add(name)) {
        classesToBeParsed.add(name);
      }
    }
  }

  private String getTopLevelClass(ITypeBinding classBinding) {

    ITypeBinding nextClass = classBinding;

    for (ITypeBinding declaringClass = nextClass.getDeclaringClass();
        declaringClass != null;
        declaringClass = nextClass.getDeclaringClass()) {
      nextClass = declaringClass;
    }

    assert nextClass.isTopLevel();

    return NameConverter.convertClassOrInterfaceToFullName(nextClass);
  }

  public String getNextClass() {
    assert !hasLocalClassPending() : "Local classes need to be parsed first!";

    if (classesToBeParsed.isEmpty()) {
      return null;
    } else {
      return classesToBeParsed.poll();
    }
  }

  public boolean hasLocalClassPending() {
    return !localClassesToBeParsed.isEmpty();
  }

  public AnonymousClassDeclaration getNextLocalClass() {
    return localClassesToBeParsed.poll();
  }

  public String getFullyQualifiedMainClassName() {
    return fullyQualifiedMainClassName;
  }

  public JClassOrInterfaceType getCurrentClassType() {
    return classStack.peek();
  }

  public Set<JFieldDeclaration> getFieldDeclarations(JClassOrInterfaceType pType) {
    return typeHierarchy.getFieldDeclarations(pType);
  }

  public Map<String, JFieldDeclaration> getStaticFieldDeclarations() {
    Map<String, JFieldDeclaration> result = new HashMap<>();

    for (JFieldDeclaration declaration : fields.values()) {
      if (declaration.isStatic()) {
        result.put(declaration.getName(), declaration);
      }
    }
    return result;
  }

  public Map<String, JFieldDeclaration> getNonStaticFieldDeclarationOfClass(
      JClassOrInterfaceType pType) {

    if (typeHierarchy.isExternType(pType)) {
      return ImmutableMap.of();
    }

    Set<JFieldDeclaration> fieldDecls = getFieldDeclarations(pType);

    for (JFieldDeclaration declaration : fieldDecls) {
      if (!declaration.isStatic()) {
        return ImmutableMap.of(declaration.getName(), declaration);
      }
    }

    return ImmutableMap.of();
  }

  public Path getFileOfCurrentType() {
    return typeHierarchy.getFileOfType(classStack.peek());
  }

  public boolean containsInterfaceType(String typeName) {
    return typeHierarchy.containsInterfaceType(typeName);
  }

  public JInterfaceType getInterfaceType(String typeName) {
    return typeHierarchy.getInterfaceType(typeName);
  }

  public boolean containsClassType(String pTypeName) {
    return typeHierarchy.containsClassType(pTypeName);
  }

  public JClassType getClassType(String pTypeName) {
    return typeHierarchy.getClassType(pTypeName);
  }

  public JInterfaceType createNewInterfaceType(ITypeBinding pTypeBinding) {
    typeHierarchy.updateTypeHierarchy(pTypeBinding);

    String newTypeName = NameConverter.convertClassOrInterfaceToFullName(pTypeBinding);

    if (containsInterfaceType(newTypeName)) {
      return getInterfaceType(newTypeName);
    } else {
      return JInterfaceType.createUnresolvableType();
    }
  }

  public JClassType createNewClassType(ITypeBinding pTypeBinding) {
    typeHierarchy.updateTypeHierarchy(pTypeBinding);

    String newTypeName = NameConverter.convertClassOrInterfaceToFullName(pTypeBinding);

    if (containsClassType(newTypeName)) {
      return getClassType(newTypeName);
    } else {
      return JClassType.createUnresolvableType();
    }
  }

  public JClassType addAnonymousClassDeclaration(AnonymousClassDeclaration pDeclaration) {
    typeHierarchy.updateTypeHierarchy(pDeclaration, getFileOfCurrentType(), logger);
    methods = typeHierarchy.getMethodDeclarations();
    fields = typeHierarchy.getFieldDeclarations();

    String pDeclarationName =
        NameConverter.convertClassOrInterfaceToFullName(pDeclaration.resolveBinding());

    localClassesToBeParsed.add(pDeclaration);
    return checkNotNull(typeHierarchy.getClassType(pDeclarationName));
  }

  public JMethodDeclaration createExternMethodDeclaration(
      JMethodType pConvertMethodType,
      String pName,
      String pSimpleName,
      VisibilityModifier pPublic,
      boolean pFinal,
      boolean pAbstract,
      boolean pStatic,
      boolean pNative,
      boolean pSynchronized,
      boolean pStrictFp,
      JClassOrInterfaceType pDeclaringClassType) {

    JMethodDeclaration decl =
        JMethodDeclaration.createExternMethodDeclaration(
            pConvertMethodType,
            pName,
            pSimpleName,
            pPublic,
            pFinal,
            pAbstract,
            pStatic,
            pNative,
            pSynchronized,
            pStrictFp,
            pDeclaringClassType);

    checkArgument(!methods.containsKey(decl.getName()));

    methods.put(decl.getName(), decl);

    return decl;
  }

  public JConstructorDeclaration createExternConstructorDeclaration(
      JConstructorType pConvertConstructorType,
      String pName,
      String pSimpleName,
      VisibilityModifier pVisibility,
      boolean pStrictFp,
      JClassType pDeclaringClassType) {

    JConstructorDeclaration decl =
        JConstructorDeclaration.createExternConstructorDeclaration(
            pConvertConstructorType,
            pName,
            pSimpleName,
            pVisibility,
            pStrictFp,
            pDeclaringClassType);

    checkArgument(!methods.containsKey(decl.getName()));

    methods.put(decl.getName(), decl);

    return decl;
  }

  public boolean isFieldRegistered(String pFieldName) {
    return fields.containsKey(pFieldName);
  }

  public JFieldDeclaration lookupField(String pFieldName) {
    checkArgument(fields.containsKey(pFieldName));
    return fields.get(pFieldName);
  }

  public JFieldDeclaration createExternFieldDeclaration(
      JType pType,
      String pName,
      String pSimpleName,
      boolean pIsFinal,
      boolean pIsStatic,
      VisibilityModifier pVisibility,
      boolean pIsVolatile,
      boolean pIsTransient) {

    JFieldDeclaration decl =
        JFieldDeclaration.createExternFieldDeclaration(
            pType, pName, pSimpleName, pIsFinal, pIsStatic, pIsTransient, pIsVolatile, pVisibility);

    checkArgument(!fields.containsKey(decl.getName()));

    fields.put(decl.getName(), decl);

    return decl;
  }

  public TypeHierarchy getTypeHierarchy() {
    return typeHierarchy;
  }
}
