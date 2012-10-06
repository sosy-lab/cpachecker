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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
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
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.CFileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNode;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanzeCreation;
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
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JSuperConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisRunTimeType;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.CFAGenerationRuntimeException;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JConstructorType;
import org.sosy_lab.cpachecker.cfa.types.java.JDummyType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;


public class ASTConverter {

  private static final boolean NOT_FINAL = false;

  private static final int NO_LINE = 0;

  private static final boolean HAS_KNOWN_BINDING = true;

  private static final String DEFAULT_MAIN_FUNCTION = "main";

  private static final int FIRST = 0;

  private static final int SECOND = 1;

  private final LogManager logger;

  private Scope scope;
  private LinkedList<JDeclaration> forInitDeclarations = new LinkedList<JDeclaration>();
  private LinkedList<JAstNode> preSideAssignments = new LinkedList<JAstNode>();
  private LinkedList<JAstNode> postSideAssignments = new LinkedList<JAstNode>();
  private ConditionalExpression conditionalExpression = null;
  private JIdExpression conditionalTemporaryVariable = null;

  private TypeHierachie typeHierachie;

  public ASTConverter(Scope pScope, boolean pIgnoreCasts, LogManager pLogger, TypeHierachie pTypeHierachie) {
    scope = pScope;
    logger = pLogger;
    typeHierachie = pTypeHierachie;
  }

  public ASTConverter(LogManager pLogger) {
    logger = pLogger;
    scope = null;
  }

  public JAstNode getNextPostSideAssignment() {
    return postSideAssignments.removeFirst();
  }

  public int numberOfPostSideAssignments() {
    return postSideAssignments.size();
  }

  public int numberOfSideAssignments(){
    return preSideAssignments.size();
  }

  public JAstNode getNextSideAssignment() {
    return preSideAssignments.removeFirst();
  }

  public void resetConditionalExpression() {
    conditionalExpression = null;
  }

  public ConditionalExpression getConditionalExpression() {
    return conditionalExpression;
  }

  public JIdExpression getConditionalTemporaryVariable() {
    return conditionalTemporaryVariable;
  }

  public int numberOfPreSideAssignments() {
    return preSideAssignments.size();
  }

  private static void check(boolean assertion, String msg, ASTNode astNode) throws CFAGenerationRuntimeException {
    if (!assertion) {
      throw new CFAGenerationRuntimeException(msg);
    }
  }

  public JAstNode getNextPreSideAssignment() {
    return preSideAssignments.removeFirst();
  }

  public List<JDeclaration> getForInitDeclaration() {
    return forInitDeclarations;
  }

  public int numberOfForInitDeclarations() {
    return forInitDeclarations.size();
  }



  public JMethodDeclaration convert(final MethodDeclaration md) {

    @SuppressWarnings("unchecked")
    ModifierBean mb = ModifierBean.getModifiers(md.modifiers());

    @SuppressWarnings({ "cast", "unchecked" })
    List<JParameterDeclaration> param = convertParameterList((List<SingleVariableDeclaration>)md.parameters());


    CFileLocation fileLoc = getFileLocation(md);
    if(md.isConstructor()){
      JConstructorType type = new JConstructorType((JClassType) convert( md.resolveBinding().getDeclaringClass()), param , md.isVarargs());
      return new JConstructorDeclaration(fileLoc, type , getFullyQualifiedMethodName(md.resolveBinding()) ,   mb.getVisibility(), mb.isStrictFp);
    } else {

      // A Method is also abstract if its a member of an interface
      boolean isAbstract = mb.isAbstract() || md.resolveBinding().getDeclaringClass().isInterface();

      JMethodType declSpec = new JMethodType(convert(md.getReturnType2()) , param , md.isVarargs());
      return new JMethodDeclaration(fileLoc, declSpec, getFullyQualifiedMethodName(md.resolveBinding()), mb.getVisibility(), mb.isFinal(),
        isAbstract, mb.isStatic(), mb.isNative(), mb.isSynchronized(), mb.isStrictFp());
    }
  }



  public String getFullyQualifiedMethodName(IMethodBinding binding) {

    StringBuilder name;

    if(binding.getName().equals(DEFAULT_MAIN_FUNCTION)){
      name = new StringBuilder(binding.getName().replace('.', '_'));
    } else {
      name = new StringBuilder((binding.getDeclaringClass().getQualifiedName().replace('.', '_') + "_" + binding.getName()).replace('.', '_'));
      ITypeBinding[] parameterTypes = binding.getParameterTypes();
      String[] typeNames = new String[parameterTypes.length];
      int c = 0;
      for(ITypeBinding parameterTypeBindings : parameterTypes) {
        if(parameterTypeBindings.isRecovered() && !parameterTypeBindings.getBinaryName().equals("String") && !parameterTypeBindings.getQualifiedName().equals("java.lang.String")){
          System.out.println(parameterTypeBindings.getBinaryName());
        }

        // TODO Erase when Library in class Path
        if(parameterTypeBindings.getBinaryName().equals("String") || parameterTypeBindings.getQualifiedName().equals("java.lang.String")){
        typeNames[c] = "java_lang_String";
        } else {
        typeNames[c] = parameterTypeBindings.getQualifiedName().replace('.', '_');
        }
        c++;
      }


      if(typeNames.length > 0){
      name.append("_");
      }
      Joiner.on("_").appendTo( name , typeNames);


    }

    return name.toString();
  }

  private JType convert(Type t) {
    // TODO Not all Types implemented
    if (t.getNodeType() == ASTNode.PRIMITIVE_TYPE) {
      return convert((PrimitiveType)t);

    } else if(t.getNodeType() == ASTNode.ARRAY_TYPE){
      return convert((ArrayType)t);
    } else if(t.getNodeType() == ASTNode.QUALIFIED_TYPE ){
      return convert((QualifiedType)t);
    }else if(t.getNodeType() == ASTNode.SIMPLE_TYPE ){
      return convert((SimpleType)t);
    }else {
      return new JDummyType(t.resolveBinding().getName());
    }
  }

  private JClassType convert(QualifiedType t) {
    ITypeBinding binding = t.resolveBinding();
    ModifierBean mB = ModifierBean.getModifiers(binding);

    return new JClassType(binding.getQualifiedName().replace('.', '_'), mB.visibility, mB.isFinal, mB.isAbstract, mB.isStrictFp);
  }

  private JClassType convert(SimpleType t) {
    ITypeBinding binding = t.resolveBinding();
    ModifierBean mB = ModifierBean.getModifiers(binding);

    return new JClassType(binding.getQualifiedName().replace('.', '_'), mB.visibility, mB.isFinal, mB.isAbstract, mB.isStrictFp);
  }

  private JType convert(ITypeBinding t) {
    //TODO Needs to be completed

    if(t == null) {
      return new JSimpleType(JBasicType.UNSPECIFIED);
    } else if(t.isPrimitive()){
      return new JSimpleType( convertPrimitiveType(t.getName()));
    } else if(t.isArray()){
      return new JArrayType(  convert(t.getElementType())  , t.getDimensions());
    } else if(t.isClass()) {
      return convertClassType(t);
    } else if(t.isInterface()){
      return convertInterfaceType(t);
    } else if(t.isEnum()) {
      //TODO Implement Enums
      return new JClassType(getFullyQualifiedClassName(t), VisibilityModifier.PUBLIC, true, false, false);
    }

    assert false : "Could Not Find Type";

    return null;
  }


  public JClassOrInterfaceType convertClassOrInterfaceType(ITypeBinding t){
     assert t.isInterface() || t.isClass() ;
     return (JClassOrInterfaceType) convert(t);
  }

  public JInterfaceType convertInterfaceType(ITypeBinding t) {
    assert t.isInterface();

    ModifierBean mB = ModifierBean.getModifiers(t);
    return new JInterfaceType(t.getQualifiedName().replace('.', '_'), mB.getVisibility());
  }

  public JClassType convertClassType(ITypeBinding t) {

      assert t.isClass() ||t.isEnum();

      ModifierBean mB = ModifierBean.getModifiers(t);
      return new JClassType(getFullyQualifiedClassName(t), mB.getVisibility(), mB.isFinal, mB.isAbstract, mB.isStrictFp);
  }


  private JArrayType convert(final ArrayType t) {

    return new JArrayType(convert((t.getElementType())), t.getDimensions() );
  }

  /**
   * Takes a ASTNode, and tries to get Information of its Placement in the
   * Source Code. If it doesnt't find such information, returns null.
   *
   *
   * @param l A Code piece wrapped in an ASTNode
   * @return FileLocation with Placement Information of the Code Piece, or null
   *          if such Information could not be obtained.
   */
  public CFileLocation getFileLocation (ASTNode l) {
    if (l == null) {
      return null;
    } else if(l.getRoot().getNodeType() != ASTNode.COMPILATION_UNIT){
      logger.log(Level.WARNING, "Can't find Placement Information for :" + l.toString());
    }

    //TODO See if this works for FileHierachys
    CompilationUnit co = (CompilationUnit) l.getRoot();


    // TODO Solve File Name Problem
    return new CFileLocation(   co.getLineNumber(l.getLength() + l.getStartPosition()),
                                     "Readable.java", l.getLength(), l.getStartPosition(),
                                       co.getLineNumber(l.getStartPosition()));
  }


  private  JSimpleType convert(final PrimitiveType t) {

        PrimitiveType.Code primitiveTypeName = t.getPrimitiveTypeCode();
        return new JSimpleType(convertPrimitiveType(primitiveTypeName.toString()));
  }

  private JBasicType convertPrimitiveType(String primitiveTypeName) {

    JBasicType type;
    if(primitiveTypeName.equals("boolean") ){
      type = JBasicType.BOOLEAN;
    } else if(primitiveTypeName.equals("char")) {
      type = JBasicType.CHAR;
    } else if(primitiveTypeName.equals("double")) {
      type = JBasicType.DOUBLE;
    } else if(primitiveTypeName.equals("float")) {
      type = JBasicType.FLOAT;
    } else if(primitiveTypeName.equals("int")) {
      type = JBasicType.INT;
    } else  if(primitiveTypeName.equals("void")) {
      type = JBasicType.VOID;
    } else if(primitiveTypeName.equals("long")) {
      type = JBasicType.LONG;
    } else if(primitiveTypeName.equals("short")) {
      type = JBasicType.SHORT;
    } else if(primitiveTypeName.equals("byte")) {
      type = JBasicType.BYTE;
    } else {
      throw new CFAGenerationRuntimeException("Unknown primitive type " + primitiveTypeName);
    }

    return type;
  }

  private List<JParameterDeclaration> convertParameterList(List<SingleVariableDeclaration> ps) {
    List<JParameterDeclaration> paramsList = new ArrayList<JParameterDeclaration>(ps.size());
    for (org.eclipse.jdt.core.dom.SingleVariableDeclaration c : ps) {
        paramsList.add(convertParameter(c));
    }
    return paramsList;
  }

  private JParameterDeclaration convertParameter(SingleVariableDeclaration p) {

    JType type = convert(p.getType());

    ModifierBean mb = ModifierBean.getModifiers(p.getModifiers());

    return new JParameterDeclaration(getFileLocation(p), type, p.getName().getFullyQualifiedName().replace('.', '_'), mb.isFinal);
  }




  @SuppressWarnings("unchecked")
  public List<JDeclaration> convert(FieldDeclaration fd){

    List<JDeclaration> result = new ArrayList<JDeclaration>();

    Type type = fd.getType();

    CFileLocation fileLoc  = getFileLocation(fd);
    @SuppressWarnings("unchecked")
    ModifierBean mB = ModifierBean.getModifiers(fd.modifiers());

    assert(!mB.isAbstract) : "Field Variable has this modifier?";
    assert(!mB.isNative) : "Field Variable has this modifier?";
    assert(!mB.isStrictFp) : "Field Variable has this modifier?";
    assert(!mB.isSynchronized) : "Field Variable has this modifier?";

    for( VariableDeclarationFragment vdf  : (List<VariableDeclarationFragment>)fd.fragments()){


      Triple<String , String , JInitializerExpression> nameAndInitializer = getNamesAndInitializer(vdf);


      JInitializerExpression initializer = nameAndInitializer.getThird();

      // If Fields are initialized with methods, insert assignment when appropriate
      // (With static variables in the Beginning, with non static at Constructor start)
      if(!preSideAssignments.isEmpty() && preSideAssignments.getLast() instanceof JMethodInvocationAssignmentStatement) {
        //TODO track initializer and insert assignment when appropriate
        initializer = null;
        preSideAssignments.clear();
      }

      result.add( new JFieldDeclaration(fileLoc,
          convert(type) ,    nameAndInitializer.getFirst() ,nameAndInitializer.getSecond().replace('.', '.'),
            initializer,  mB.isFinal, mB.isStatic, mB.isTransient, mB.isVolatile , mB.getVisibility()));

    }
    return result;
}

   static class  ModifierBean {

     private final boolean isFinal;
     private final boolean isStatic;
     private final boolean isVolatile;
     private final boolean isTransient;
     private final boolean isNative;
     private final boolean isAbstract;
     private final boolean isStrictFp;
     private final boolean isSynchronized;
     private final VisibilityModifier visibility;

    public ModifierBean(boolean pIsFinal, boolean pIsStatic, boolean pIsVolatile, boolean pIsTransient,
        VisibilityModifier pVisibility , boolean pIsNative, boolean pIsAbstract, boolean pIsStrictFp,
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

    private static ModifierBean getModifiers(int modifiers){

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
      for (int bitMask = 1; bitMask < 2049; bitMask = bitMask << 1 ) {


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
            assert visibility == null :  "Can only declare one Visibility Modifier";
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
      if(visibility == null){
        visibility = VisibilityModifier.NONE;
      }

      return new ModifierBean(isFinal, isStatic, isVolatile, isTransient, visibility,
          isNative, isAbstract, isStrictFp, isSynchronized);

    }

    public static ModifierBean getModifiers(ITypeBinding pBinding) {


      // This int value is the bit-wise or of Modifier constants
      int modifiers = pBinding.getModifiers();

      assert pBinding.isClass() || pBinding.isEnum()
      || pBinding.isInterface() || pBinding.isAnnotation()
      || pBinding.isRecovered(): "This type can't have modifiers";


       return getModifiers(modifiers);
    }

    private static ModifierBean getModifiers(List<IExtendedModifier> modifiers) {

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
            assert visibility == null :  "Can only declare one Visibility Modifier";
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
      if(visibility == null){
        visibility = VisibilityModifier.NONE;
      }

      return new ModifierBean(isFinal, isStatic, isVolatile, isTransient, visibility,
          isNative, isAbstract, isStrictFp, isSynchronized);
    }

    public VisibilityModifier getVisibility(){
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

  private Triple<String , String , JInitializerExpression> getNamesAndInitializer(VariableDeclarationFragment d ) {

    JInitializerExpression initializerExpression = null;

    // If there is no Initializer, JVariableDeclaration expects null to be given.
    if(d.getInitializer() != null){
      initializerExpression = new JInitializerExpression( getFileLocation(d) ,  convertExpressionWithoutSideEffects(d.getInitializer()));
    }


    String name = getFullyQualifiedName(d.resolveBinding());

    return Triple.of( name, name, initializerExpression);
  }


  private String getFullyQualifiedName(IVariableBinding vb){
    StringBuilder name = new StringBuilder();

    // Field Variable are declared with Declaring class before Identifier
    // Classname.var
    name.append( vb.getDeclaringClass() != null ? vb.getDeclaringClass().getName() + "." : "");
    name.append(vb.getName());

    return name.toString();

  }


  @SuppressWarnings("unchecked")
  public List<JDeclaration> convert(VariableDeclarationStatement vds){


    List<JDeclaration> variableDeclarations = new ArrayList<JDeclaration>();

    @SuppressWarnings("cast")
    List<VariableDeclarationFragment> variableDeclarationFragments =
                                            (List<VariableDeclarationFragment>)vds.fragments();

    CFileLocation fileLoc = getFileLocation(vds);
    Type type = vds.getType();


    ModifierBean mB = ModifierBean.getModifiers(vds.modifiers());

   assert(!mB.isAbstract) : "Local Variable has abstract modifier?";
   assert(!mB.isNative) : "Local Variable has native modifier?";
   assert(mB.visibility == VisibilityModifier.NONE) : "Local Variable has Visibility modifier?";
   assert(!mB.isStatic) : "Local Variable has static modifier?";
   assert(!mB.isStrictFp) : "Local Variable has strictFp modifier?";
   assert(!mB.isSynchronized) : "Local Variable has synchronized modifier?";

    for(VariableDeclarationFragment vdf : variableDeclarationFragments){

      Triple<String , String , JInitializerExpression> nameAndInitializer = getNamesAndInitializer(vdf);

        variableDeclarations.add( new JVariableDeclaration(fileLoc,
            convert(type) ,    nameAndInitializer.getFirst() ,nameAndInitializer.getSecond()   , nameAndInitializer.getThird() ,  mB.isFinal));
    }

    return variableDeclarations;
  }







  public JDeclaration convert(SingleVariableDeclaration d) {


    Type type = d.getType();

    @SuppressWarnings("unchecked")
    ModifierBean mB = ModifierBean.getModifiers(d.modifiers());

    assert(!mB.isAbstract) : "Local Variable has abstract modifier?";
    assert(!mB.isNative) : "Local Variable has native modifier?";
    assert(mB.visibility == VisibilityModifier.NONE) : "Local Variable has Visibility modifier?";
    assert(!mB.isStatic) : "Local Variable has static modifier?";
    assert(!mB.isStrictFp) : "Local Variable has strictFp modifier?";
    assert(!mB.isSynchronized) : "Local Variable has synchronized modifier?";

    JInitializerExpression initializerExpression = null;

    // If there is no Initializer, CStorageClass expects null to be given.
    if (d.getInitializer() != null) {
      initializerExpression =
          new JInitializerExpression(getFileLocation(d),
               (JExpression) convertExpressionWithSideEffects(d.getInitializer()));
    }

    return new JVariableDeclaration(getFileLocation(d),
        convert(type), d.getName().getFullyQualifiedName().replace('.', '_'), d.getName().getFullyQualifiedName().replace('.', '_'), initializerExpression,
        mB.isFinal);
  }


  public JReturnStatement convert(final ReturnStatement s) {
    return new JReturnStatement(getFileLocation(s), convertExpressionWithoutSideEffects(s.getExpression()));
  }


  public JExpression convertExpressionWithoutSideEffects( Expression e) {

    JAstNode node = convertExpressionWithSideEffects(e);

    if (node == null || node instanceof JExpression) {
      return  (JExpression) node;

    } else if ((node instanceof JMethodInvocationExpression)) {
      return addSideassignmentsForExpressionsWithoutSideEffects(node, e);

    } else if (node instanceof JAssignment) {

      if(node instanceof JUnaryExpression &&  e instanceof PostfixExpression) {
      postSideAssignments.add(node);
      } else {
      preSideAssignments.add(node);
      }

      return ((JAssignment) node).getLeftHandSide();

    } else {
      throw new AssertionError("unknown expression " + node);
    }
  }

  private JExpression addSideassignmentsForExpressionsWithoutSideEffects(JAstNode node,Expression e) {
    JIdExpression tmp = createTemporaryVariable(e);

    preSideAssignments.add(new JMethodInvocationAssignmentStatement( node.getFileLocation(),
        tmp,
        (JMethodInvocationExpression) node));
    return tmp;
  }

  /**
   * creates temporary variables with increasing numbers
   */
  private JIdExpression createTemporaryVariable(Expression e) {

    String name = "__CPAchecker_TMP_";
    int i = 0;
    while(scope.variableNameInUse(name+i, name+i)){
      i++;
    }
    name += i;

    JVariableDeclaration decl = new JVariableDeclaration(getFileLocation(e),
                                               convert(e.resolveTypeBinding()),
                                               name,
                                               name,
                                               null, NOT_FINAL);

    scope.registerDeclaration(decl);
    preSideAssignments.add(decl);
    JIdExpression tmp = new JIdExpression(decl.getFileLocation(),
                                                convert(e.resolveTypeBinding()),
                                                name,
                                                decl);
    return tmp;
  }


  public JStatement convert(final ExpressionStatement s) {

    JAstNode node = convertExpressionWithSideEffects(s.getExpression());

    if (node instanceof JExpressionAssignmentStatement) {
      return (JExpressionAssignmentStatement)node;

    } else if (node instanceof JMethodInvocationAssignmentStatement) {
      return (JMethodInvocationAssignmentStatement)node;

    } else if (node instanceof JMethodInvocationExpression) {
      return new JMethodInvocationStatement( getFileLocation(s) , (JMethodInvocationExpression)node);

    } else if (node instanceof JExpression) {
      return new JExpressionStatement(  getFileLocation(s) , (JExpression) node);

    } else {
      throw new AssertionError();
    }
  }

  public JStatement convert(final SuperConstructorInvocation sCI) {

    boolean canBeResolved = sCI.resolveConstructorBinding() != null;

    if(canBeResolved){
      scope.registerClasses(sCI.resolveConstructorBinding().getDeclaringClass());
    }


    @SuppressWarnings("unchecked")
    List<Expression> p = sCI.arguments();

    List<JExpression> params;

    if (p.size() > 0) {
      params = convert(p);

    } else {
      params = new ArrayList<JExpression>();
    }

    String name;


    if(canBeResolved) {
      name = getFullyQualifiedMethodName(sCI.resolveConstructorBinding());
    } else {
      // If binding can't be resolved, the constructor is not parsed in all cases.
      name = sCI.toString().replace('.', '_');
    }

    JConstructorDeclaration declaration = (JConstructorDeclaration) scope.lookupFunction(name);

    if(declaration == null) {
      //TODO ugly, search for a way to get legitimate Declarations


      declaration = new JConstructorDeclaration(getFileLocation(sCI), new JConstructorType( new JClassType( "dummy" , VisibilityModifier.PUBLIC, false, false, false), new ArrayList<JParameterDeclaration>(), false), name, VisibilityModifier.PUBLIC, false);

    }

    //TODO Investigate if type Right

    JExpression functionName;

    if(canBeResolved) {
      functionName = new JIdExpression(getFileLocation(sCI), convert(sCI.resolveConstructorBinding().getReturnType()), name,  declaration);
    } else {
      functionName = new JIdExpression(getFileLocation(sCI), new JClassType( "dummy" , VisibilityModifier.PUBLIC, false, false, false), name,  declaration);
    }

    JIdExpression idExpression = (JIdExpression)functionName;

      if (idExpression.getDeclaration() != null) {
        // clone idExpression because the declaration in it is wrong
        // (it's the declaration of an equally named variable)
        // TODO this is ugly

        functionName = new JIdExpression(idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
      }

      JConstructorType type = null;


      type = declaration.getType();

      return new JMethodInvocationStatement( getFileLocation(sCI) , new JSuperConstructorInvocation(getFileLocation(sCI), type , functionName, params, declaration));
  }


  public JAstNode convertExpressionWithSideEffects(Expression e) {

    //TODO  All Expression Implementation

    if (e == null) {
      return null;
    }

    switch(e.getNodeType()){
       case ASTNode.ASSIGNMENT:
         return convert((Assignment)e);
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
         return convert((QualifiedName )e );
       case ASTNode.BOOLEAN_LITERAL:
         return convert((BooleanLiteral) e);
       case ASTNode.FIELD_ACCESS:
        return  convert((FieldAccess) e);
       case ASTNode.SIMPLE_NAME:
         return convert((SimpleName) e);
       case ASTNode.PARENTHESIZED_EXPRESSION:
         return convertExpressionWithoutSideEffects(((ParenthesizedExpression) e).getExpression());
       case ASTNode.METHOD_INVOCATION:
         return convert((MethodInvocation)e);
       case ASTNode.CLASS_INSTANCE_CREATION:
         return convert((ClassInstanceCreation)e);
       case ASTNode.ARRAY_ACCESS:
         return convert( (ArrayAccess) e);
       case ASTNode.ARRAY_CREATION:
         return convert( (ArrayCreation) e);
       case ASTNode.ARRAY_INITIALIZER :
         return convert((ArrayInitializer)e);
       case ASTNode.CONDITIONAL_EXPRESSION :
         return convert( (ConditionalExpression)e);
       case ASTNode.THIS_EXPRESSION:
         return convert( (ThisExpression)e);
       case ASTNode.INSTANCEOF_EXPRESSION:
         return convert((InstanceofExpression)e);
       //case ASTNode.CAST_EXPRESSION:
         //return convert((CastExpression) e);
       case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
         return convert( (VariableDeclarationExpression)e);
    }

       logger.log(Level.SEVERE, "Expression of typ "+  AstErrorChecker.getTypeName(e.getNodeType()) + " not implemented");
       return null;
  }

  private JIdExpression convert(VariableDeclarationExpression vde) {

    List<JDeclaration> variableDeclarations = new ArrayList<JDeclaration>();

    @SuppressWarnings({ "cast", "unchecked" })
    List<VariableDeclarationFragment> variableDeclarationFragments =
                                            (List<VariableDeclarationFragment>)vde.fragments();

    CFileLocation fileLoc = getFileLocation(vde);
    Type type = vde.getType();


    @SuppressWarnings("unchecked")
    ModifierBean mB = ModifierBean.getModifiers(vde.modifiers());

   assert(!mB.isAbstract) : "Local Variable has abstract modifier?";
   assert(!mB.isNative) : "Local Variable has native modifier?";
   assert(mB.visibility == VisibilityModifier.NONE) : "Local Variable has Visibility modifier?";
   assert(!mB.isStatic) : "Local Variable has static modifier?";
   assert(!mB.isStrictFp) : "Local Variable has strictFp modifier?";
   assert(!mB.isSynchronized) : "Local Variable has synchronized modifier?";

    for(VariableDeclarationFragment vdf : variableDeclarationFragments){

      Triple<String , String , JInitializerExpression> nameAndInitializer = getNamesAndInitializer(vdf);

        variableDeclarations.add( new JVariableDeclaration(fileLoc,
            convert(type) ,    nameAndInitializer.getFirst() ,nameAndInitializer.getSecond()   , nameAndInitializer.getThird() ,  mB.isFinal));
    }

    forInitDeclarations.addAll(variableDeclarations);

    return null;
  }

  //private JAstNode convert(CastExpression e) {

    //TODO Cast Expression
    //JExpression exp = convertExpressionWithoutSideEffects(e.getExpression());
    //return exp;
  //}

  private JAstNode convert(InstanceofExpression e) {

    CFileLocation fileloc = getFileLocation(e);
    JExpression leftOperand = convertExpressionWithoutSideEffects(e.getLeftOperand());
    JType type = convert(e.getRightOperand().resolveBinding());
    assert leftOperand instanceof JIdExpression : "There are other expressions for instanceOf?";
    assert type instanceof JClassOrInterfaceType : "There are other types for this expression?";


    JIdExpression referenceVariable = (JIdExpression) leftOperand;
    JClassOrInterfaceType instanceCompatible = (JClassOrInterfaceType) type;
    JRunTimeTypeEqualsType firstCond = null;



    List<JClassType> subClassTypes = null;

    if( instanceCompatible instanceof JInterfaceType) {

      subClassTypes = typeHierachie.getAllKnownImplementedClassesOfInterface((JInterfaceType)instanceCompatible);

      if(subClassTypes.isEmpty()) {
        return new JBooleanLiteralExpression(fileloc, convert(e.resolveTypeBinding()), false);
      } else if(subClassTypes.size() == 1) {
        return convertClassRunTimeCompileTimeAccord(fileloc, referenceVariable.getDeclaration(), subClassTypes.get(FIRST));
      }

    } else if(instanceCompatible instanceof JClassType){

      subClassTypes = typeHierachie.getAllSubTypesOfClass((JClassType) instanceCompatible);

      firstCond = convertClassRunTimeCompileTimeAccord(fileloc, referenceVariable.getDeclaration(), instanceCompatible);
      if(subClassTypes.isEmpty()){
        return firstCond;
      }
    }

 JBinaryExpression firstOrConnection;


   if(firstCond == null) {
     firstOrConnection = new JBinaryExpression(fileloc, convert(e.resolveTypeBinding()), convertClassRunTimeCompileTimeAccord(fileloc, referenceVariable.getDeclaration(),  subClassTypes.get(FIRST)), convertClassRunTimeCompileTimeAccord(fileloc, referenceVariable.getDeclaration(),  subClassTypes.get(SECOND)), JBinaryExpression.BinaryOperator.CONDITIONAL_OR);
     subClassTypes.remove(SECOND);
     subClassTypes.remove(FIRST);
   } else {
     firstOrConnection = new JBinaryExpression(fileloc, convert(e.resolveTypeBinding()), firstCond, convertClassRunTimeCompileTimeAccord(fileloc, referenceVariable.getDeclaration(), subClassTypes.get(FIRST)), JBinaryExpression.BinaryOperator.CONDITIONAL_OR);
     subClassTypes.remove(FIRST);
   }

   JBinaryExpression nextConnection = firstOrConnection;

    for(JClassType subType : subClassTypes) {
        JRunTimeTypeEqualsType cond = convertClassRunTimeCompileTimeAccord(fileloc, referenceVariable.getDeclaration(), subType);
        nextConnection = new JBinaryExpression(fileloc, convert(e.resolveTypeBinding()), nextConnection, cond, BinaryOperator.CONDITIONAL_OR);
    }



    return nextConnection;
  }

  // Create tmp Variable to a AbstractReferenceReturn
  private JAstNode convert(ThisExpression e) {

    //TODO Unnecessary now, change to reference this
   JIdExpression expression =   createTemporaryVariable(e);
    preSideAssignments.add(expression);

    return expression;
  }

  private JAstNode convert(FieldAccess e) {

    // JFieldAccess is no FieldAccess, but a qualified FieldAccess
    // Distinction between Fields and Variables are made through
    // Declarations JVariableDeclaration and JFieldDeclarations
    // JField Access makes the distinction between non-static
    // fields with qualifier, and the rest

    boolean canBeResolved = e.resolveFieldBinding() != null;

    if(canBeResolved){
    scope.registerClasses(e.resolveFieldBinding().getDeclaringClass());
    }
    // 'This Expression' can be ignored, is solved through resolve Binding
    // and does'nt need to be included in the cfa
    if(e.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION){
      return convertExpressionWithoutSideEffects(e.getName());
    } else {
      return convertExpressionWithoutSideEffects(e.getExpression());
    }

  }

  private JAstNode convert(ClassInstanceCreation cIC) {

    boolean canBeResolved = cIC.resolveConstructorBinding() != null;

    if(canBeResolved){
      scope.registerClasses(cIC.resolveConstructorBinding().getDeclaringClass());
    }


    @SuppressWarnings("unchecked")
    List<Expression> p = cIC.arguments();

    List<JExpression> params;

    if (p.size() > 0) {
      params = convert(p);

    } else {
      params = new ArrayList<JExpression>();
    }

    String name;


    if(canBeResolved) {
      name = getFullyQualifiedMethodName(cIC.resolveConstructorBinding());
    } else {
      // If binding can't be resolved, the constructor is not parsed in all cases.
      name = cIC.toString().replace('.', '_');
    }

    JConstructorDeclaration declaration = (JConstructorDeclaration) scope.lookupFunction(name);

    if(declaration == null) {
      //TODO ugly, search for a way to get legitimate Declarations

      declaration = new JConstructorDeclaration(getFileLocation(cIC), new JConstructorType( new JClassType( "dummy" , VisibilityModifier.PUBLIC, false, false, false), new ArrayList<JParameterDeclaration>(), false), name, VisibilityModifier.PUBLIC, false);

    }

    //TODO Investigate if type Right
    JExpression functionName = new JIdExpression(getFileLocation(cIC), convert(cIC.resolveTypeBinding()), name,  declaration);
    JIdExpression idExpression = (JIdExpression)functionName;

      if (idExpression.getDeclaration() != null) {
        // clone idExpression because the declaration in it is wrong
        // (it's the declaration of an equally named variable)
        // TODO this is ugly

        functionName = new JIdExpression(idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
      }

      JConstructorType type = null;

    //  if(declaration != null){
        type = declaration.getType();
        /*
      } else {

        boolean isVarargs = false;

        JClassType classType = null;

        if(canBeResolved){
         isVarargs =  cIC.resolveConstructorBinding().isVarargs();
         classType = (JClassType) convert(cIC.resolveTypeBinding());
        } else {
         classType = new JClassType(cIC.getType().toString().replace('.', '_'), VisibilityModifier.NONE, false, false, false);
        }



        type = new JConstructorType( classType  ,  new ArrayList<AParameterDeclaration>() ,isVarargs);
      }
      */

//TODO getDeclaration somehow (Needs to be done after everything is parsed)
    return new JClassInstanzeCreation(getFileLocation(cIC), type , functionName, params, declaration);
  }

  private JAstNode convert(ConditionalExpression e) {
    JIdExpression tmp = createTemporaryVariable(e);
    conditionalTemporaryVariable = tmp;
    conditionalExpression = e;
    return tmp;
  }




  private JAstNode convert(ArrayInitializer initializer) {

    if(initializer == null){
      return null;
    }

    JArrayType type = (JArrayType) convert(initializer.resolveTypeBinding());
    List<JExpression> initializerExpressions = new ArrayList<JExpression>();

    @SuppressWarnings("unchecked")
    List<Expression> expressions = initializer.expressions();

    for( Expression  exp : expressions){
      initializerExpressions.add( convertExpressionWithoutSideEffects(exp));
    }


    return new JArrayInitializer(getFileLocation(initializer), initializerExpressions, type);
  }

  private JAstNode convert(ArrayCreation Ace) {



    CFileLocation fileloc = getFileLocation(Ace);
    JArrayInitializer initializer = (JArrayInitializer) convertExpressionWithoutSideEffects(Ace.getInitializer());
    JArrayType type = convert(Ace.getType());
    List<JExpression> length = new ArrayList<JExpression>(type.getDimensions());



    @SuppressWarnings("unchecked")
    List<Expression> dim = Ace.dimensions();

    if(initializer != null){
      for(int dimension = 0; dimension < type.getDimensions(); dimension++){
        //TODO find way to correctly calculate size
        length.add(new JIntegerLiteralExpression(fileloc, new JSimpleType(JBasicType.INT), BigInteger.valueOf(100)));
      }
    } else {

      for(Expression exp : dim){
        length.add(convertExpressionWithoutSideEffects(exp));
      }

    }

    return new JArrayCreationExpression(fileloc ,
                                               type, initializer , length );
  }

  private JAstNode convert(ArrayAccess e) {

   JExpression subscriptExpression = convertExpressionWithoutSideEffects(e.getArray());
   JExpression index = convertExpressionWithoutSideEffects(e.getIndex());


   assert subscriptExpression    != null;
   assert index != null;

    return new JArraySubscriptExpression(getFileLocation(e), convert(e.resolveTypeBinding()), subscriptExpression , index);
  }


  private JAstNode convert(QualifiedName e) {

    String name = null;
    JSimpleDeclaration declaration = null;

    boolean canBeResolved = e.resolveBinding() != null;

    if(canBeResolved && ((IVariableBinding)e.resolveBinding()).isEnumConstant()) {
        //TODO Prototype for enum constant expression, investigate
      return new JEnumConstantExpression(getFileLocation(e), (JClassType) convert(e.resolveTypeBinding()) , getFullyQualifiedName((IVariableBinding) e.resolveBinding()));
    }

    if(e.resolveBinding() instanceof IMethodBinding ){
      name = getFullyQualifiedMethodName((IMethodBinding) e.resolveBinding());
    } else if(e.resolveBinding() instanceof IVariableBinding){

      return convertQualifiedVariableIdentificationExpression(e);

    } else {
      name = e.getFullyQualifiedName();
    }


    return new JIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, declaration);
  }




  private JAstNode convertQualifiedVariableIdentificationExpression(QualifiedName e) {

    String name = e.getFullyQualifiedName();
    IVariableBinding vb = (IVariableBinding) e.resolveBinding();
    JSimpleDeclaration declaration = scope.lookupVariable(name);

    if(declaration == null && vb.isField()) {
      // TODO If can't be Found, create declaration (ugly , needs to be changed so that only one Declaration is ever created
      ModifierBean mb = ModifierBean.getModifiers(vb.getModifiers());
      declaration = new JFieldDeclaration(getFileLocation(e), convert(e.resolveTypeBinding()), getFullyQualifiedName(vb), getFullyQualifiedName(vb), null, mb.isFinal(), mb.isStatic(), mb.isTransient(), mb.isVolatile, mb.visibility);
    } else if(declaration == null && !vb.isField()) {
      ModifierBean mb = ModifierBean.getModifiers(vb.getModifiers());
      declaration = new JVariableDeclaration(getFileLocation(e),  convert(e.resolveTypeBinding()), getFullyQualifiedName(vb), getFullyQualifiedName(vb), null, mb.isFinal());
    }



    Queue<JIdExpression> qualifier = convertQualifier(e.getQualifier());

    if(!qualifier.isEmpty()) {

      return new JFieldAccess(getFileLocation(e), convert(e.resolveTypeBinding()), name, (JFieldDeclaration) declaration, qualifier);
    }


    return new JIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, declaration);
  }

  private Queue<JIdExpression> convertQualifier(Name pQualifier) {

    Name qualifier = pQualifier;

    Queue<JIdExpression> result = new ConcurrentLinkedQueue<JIdExpression>();

    while(qualifier != null){
      JIdExpression idExp = convertQualifierToJIdExpression(qualifier);
      result.add(idExp);
      if(qualifier instanceof QualifiedName) {
        qualifier = ((QualifiedName) qualifier).getQualifier();
      } else {
        qualifier = null;
      }
    }

    return result;
  }

  private JIdExpression convertQualifierToJIdExpression(Name e) {

    String name = null;
    JSimpleDeclaration declaration = null;
    boolean canBeResolved = e.resolveBinding() != null;

    if (canBeResolved) {

      IBinding binding = e.resolveBinding();

      if (binding instanceof IVariableBinding) {
        IVariableBinding vb = (IVariableBinding) binding;

        name = getFullyQualifiedName(vb);

        declaration = scope.lookupVariable(name);

        if(declaration == null && vb.isField()) {
          // TODO If can't be Found, create declaration (ugly , needs to be changed so that only one Declaration is ever created
          ModifierBean mb = ModifierBean.getModifiers(vb.getModifiers());
          declaration = new JFieldDeclaration(getFileLocation(e), convert(e.resolveTypeBinding()), name, name, null, mb.isFinal(), mb.isStatic(), mb.isTransient(), mb.isVolatile, mb.visibility);
        } else if(declaration == null && !vb.isField()) {
          ModifierBean mb = ModifierBean.getModifiers(vb.getModifiers());
          declaration = new JVariableDeclaration(getFileLocation(e),  convert(e.resolveTypeBinding()), name, name, null, mb.isFinal());
        }

      } else if (binding instanceof IMethodBinding) {
        name = getFullyQualifiedMethodName((IMethodBinding) binding);
        declaration = scope.lookupFunction(name);
      }
    } else {
      // Can be an unresolvable Method
      name = e.getFullyQualifiedName();
    }

    assert name != null;

    return new JIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, declaration);
  }

  public JMethodInvocationExpression convert(FunctionEntryNode  newFunctionEntryNode, JMethodInvocationExpression oldFunctionCall) {

    JSimpleDeclaration declaration = (JSimpleDeclaration) newFunctionEntryNode.getFunctionDefinition();

    String name = newFunctionEntryNode.getFunctionName();

    //TODO When String Type is ready, convert to String
    JIdExpression functionName = new JIdExpression(oldFunctionCall.getFileLocation(), new JSimpleType(JBasicType.UNSPECIFIED), name, declaration);

    if(oldFunctionCall instanceof JReferencedMethodInvocationExpression ){
    return new JReferencedMethodInvocationExpression(oldFunctionCall.getFileLocation(), oldFunctionCall.getExpressionType(), functionName, oldFunctionCall.getParameterExpressions(), declaration, ((JReferencedMethodInvocationExpression)oldFunctionCall).getReferencedVariable());
    } else {
    return new JMethodInvocationExpression(oldFunctionCall.getFileLocation(), oldFunctionCall.getExpressionType(), functionName, oldFunctionCall.getParameterExpressions(), declaration);
    }
  }

  @SuppressWarnings({ "unchecked", "cast" })
  private JAstNode convert(MethodInvocation mi) {

    boolean canBeResolve = mi.resolveMethodBinding() != null;

    if(canBeResolve) {
      scope.registerClasses(mi.resolveMethodBinding().getDeclaringClass());
    }

    List<Expression> p = mi.arguments();

    List<JExpression> params;
    if (p.size() > 0) {
      params = convert((List<Expression>)p);

    } else {
      params = new ArrayList<JExpression>();
    }

    JExpression functionName = convertExpressionWithoutSideEffects(mi.getName());
    JSimpleDeclaration declaration = null;

    JExpression referencedVariableName = null;


    ModifierBean mb = null;

    if (canBeResolve) {
      mb = ModifierBean.getModifiers(mi.resolveMethodBinding());

      if (!mb.isStatic) {
        referencedVariableName = convertExpressionWithoutSideEffects(mi.getExpression());
      }
    }


    if (functionName instanceof JIdExpression) {
      JIdExpression idExpression = (JIdExpression)functionName;
      String name = idExpression.getName();
      declaration = scope.lookupFunction( name);

      if (idExpression.getDeclaration() != null) {
        // TODO this is ugly

        functionName = new JIdExpression(idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
      }
    }

    if( canBeResolve && declaration == null) {
      declaration = new JMethodDeclaration(getFileLocation(mi), new JMethodType( new JClassType( "dummy" , VisibilityModifier.PUBLIC, false, false, false), new ArrayList<JParameterDeclaration>(), false), functionName.toASTString(), VisibilityModifier.PUBLIC, mb.isFinal , mb.isAbstract, mb.isStatic ,mb.isNative ,mb.isSynchronized, mb.isStrictFp );
    }

    if(referencedVariableName == null){

    return new JMethodInvocationExpression(getFileLocation(mi), convert(mi.resolveTypeBinding()), functionName, params, declaration);
    } else {
      String variableName = convertExpressionWithoutSideEffects(mi.getExpression()).toASTString();

      JSimpleDeclaration referencedVariable = scope.lookupVariable(variableName);
      return new JReferencedMethodInvocationExpression(getFileLocation(mi), convert(mi.resolveTypeBinding()), functionName, params, declaration, referencedVariable);
    }
  }

  private List<JExpression> convert(List<Expression> el) {

    List<JExpression> result = new ArrayList<JExpression>(el.size());
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
        IVariableBinding vb = (IVariableBinding) binding;

        if(((IVariableBinding)e.resolveBinding()).isEnumConstant()) {
          //TODO Prototype for enum constant expression, investigate
          return new JEnumConstantExpression(getFileLocation(e), (JClassType) convert(e.resolveTypeBinding()) , getFullyQualifiedName((IVariableBinding) e.resolveBinding()));
        }

        name = getFullyQualifiedName(vb);


        declaration = scope.lookupVariable(name);

        if(declaration == null && vb.isField()) {
          // TODO If can't be Found, create declaration (ugly , needs to be changed so that only one Declaration is ever created
          ModifierBean mb = ModifierBean.getModifiers(vb.getModifiers());
          declaration = new JFieldDeclaration(getFileLocation(e), convert(e.resolveTypeBinding()), name, name, null, mb.isFinal(), mb.isStatic(), mb.isTransient(), mb.isVolatile, mb.visibility);
        } else if(declaration == null && !vb.isField()) {
          ModifierBean mb = ModifierBean.getModifiers(vb.getModifiers());
          declaration = new JVariableDeclaration(getFileLocation(e),  convert(e.resolveTypeBinding()), name, name, null, mb.isFinal());
        }


      } else if (binding instanceof IMethodBinding) {
        name = getFullyQualifiedMethodName((IMethodBinding) binding);
        declaration = scope.lookupFunction(name);
      } else if(binding instanceof ITypeBinding){
        name = e.getIdentifier();
      }

    } else {
      name = e.getIdentifier();
    }

    assert name != null;


    return new JIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, declaration);
  }



  private JAstNode convert(Assignment e) {

    CFileLocation fileLoc = getFileLocation(e);
    JType type = convert(e.resolveTypeBinding());
    JExpression leftHandSide = convertExpressionWithoutSideEffects(e.getLeftHandSide());

    BinaryOperator op = convert(e.getOperator());

    if (op == null) {
      // a = b
      JAstNode rightHandSide =  convertExpressionWithSideEffects(e.getRightHandSide()); // right-hand side may have a function call



      if (rightHandSide instanceof JExpression) {
        // a = b
        return new JExpressionAssignmentStatement(fileLoc, leftHandSide, (JExpression)rightHandSide);

      } else if (rightHandSide instanceof JMethodInvocationExpression) {
        // a = f()
        return new JMethodInvocationAssignmentStatement(fileLoc, leftHandSide, (JMethodInvocationExpression)rightHandSide);

      } else if(rightHandSide instanceof JAssignment) {
        preSideAssignments.add(rightHandSide);
        return new JExpressionAssignmentStatement(fileLoc, leftHandSide, ((JAssignment) rightHandSide).getLeftHandSide());
      } else {
        //TODO CFA Exception lacks ASTNode
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


    if(op.equals(Assignment.Operator.ASSIGN)){
      return null;
    } else if (op.equals(Assignment.Operator.BIT_AND_ASSIGN)) {
      return BinaryOperator.BINARY_AND;
    } else if (op.equals(Assignment.Operator.BIT_OR_ASSIGN)) {
      return BinaryOperator.BINARY_OR;
    } else if (op.equals(Assignment.Operator.BIT_XOR_ASSIGN)) {
      return BinaryOperator.BINARY_XOR;
    } else if (op.equals(Assignment.Operator.DIVIDE_ASSIGN)) {
      return BinaryOperator.DIVIDE;
    }else if (op.equals(Assignment.Operator.LEFT_SHIFT_ASSIGN)) {
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
    }else {
      logger.log(Level.SEVERE, "Did not find Operator");
      return null;
    }

  }

  private JExpression convert(BooleanLiteral e) {

    return new JBooleanLiteralExpression( getFileLocation(e), convert(e.resolveTypeBinding()) , e.booleanValue());
  }


  private JAstNode convert(PrefixExpression e) {

    PrefixExpression.Operator op = e.getOperator();

    if (op.equals(PrefixExpression.Operator.INCREMENT)
        || op.equals(PrefixExpression.Operator.DECREMENT)) {
     return handlePreFixIncOrDec(e, op);
    }

    JExpression operand = convertExpressionWithoutSideEffects(e.getOperand());
    CFileLocation fileLoc = getFileLocation(e);


    return new JUnaryExpression(fileLoc, convert(e.resolveTypeBinding()), operand, convertUnaryOperator(op));
  }

  private JAstNode convert(PostfixExpression e) {
    PostfixExpression.Operator op = e.getOperator();
    return  handlePostFixIncOrDec(e, op);
  }


  private JAstNode handlePostFixIncOrDec(PostfixExpression e, PostfixExpression.Operator op) {

    BinaryOperator postOp = null;
    if (op.equals(PostfixExpression.Operator.INCREMENT)) {

      postOp = BinaryOperator.PLUS;
    } else if (op.equals(PostfixExpression.Operator.DECREMENT)) {
      postOp = BinaryOperator.MINUS;
    }
    assert postOp != null : "Increment/Decrement Severe Error.";

    //if (e.getParent() instanceof Expression) {
      //new CFAGenerationRuntimeException("Side assignments with Increment/Decrement not yet implemented");
      //return null;
    //} else {

      CFileLocation fileLoc = getFileLocation(e);
      JType type = convert(e.resolveTypeBinding());
      JExpression operand = convertExpressionWithoutSideEffects(e.getOperand());

      JExpression preOne = new JIntegerLiteralExpression(fileLoc, type, BigInteger.ONE);
      JBinaryExpression preExp = new JBinaryExpression(fileLoc, type, operand, preOne, postOp);
      return new JExpressionAssignmentStatement(fileLoc, operand, preExp);
    }
  //}

  private JAstNode handlePreFixIncOrDec(PrefixExpression e, Operator op) {

    BinaryOperator preOp = null;
    if (op.equals(PrefixExpression.Operator.INCREMENT)) {

      preOp = BinaryOperator.PLUS;
    } else if (op.equals(PrefixExpression.Operator.DECREMENT)) {
      preOp = BinaryOperator.MINUS;
    }
    assert preOp != null : "Increment/Decrement Severe Error.";


    //if (e.getParent() instanceof Expression) {
      //new CFAGenerationRuntimeException("Side assignments with Increment/Decrement not yet implemented");
      //return null;
    //} else {

      CFileLocation fileLoc = getFileLocation(e);
      JType type = convert(e.resolveTypeBinding());
      JExpression operand = convertExpressionWithoutSideEffects(e.getOperand());

      JExpression preOne = new JIntegerLiteralExpression(fileLoc, type, BigInteger.ONE);
      JBinaryExpression preExp = new JBinaryExpression(fileLoc, type, operand, preOne, preOp);
      return new JExpressionAssignmentStatement(fileLoc, operand, preExp);
    //}
  }




private UnaryOperator convertUnaryOperator(PrefixExpression.Operator op) {

  if(op.equals(PrefixExpression.Operator.NOT)){
    return UnaryOperator.NOT;
  } else if (op.equals(PrefixExpression.Operator.PLUS)) {
    return UnaryOperator.PLUS;
  } else if (op.equals(PrefixExpression.Operator.COMPLEMENT)) {
    return UnaryOperator.COMPLEMENT;
  } else if (op.equals(PrefixExpression.Operator.MINUS)) {
    return UnaryOperator.MINUS;
  }else{
    logger.log(Level.SEVERE, "Did not find Operator");
    return null;
  }
}

  @SuppressWarnings("unchecked")
  private JExpression convert(InfixExpression e) {
    CFileLocation fileLoc = getFileLocation(e);
    JType type = convert(e.resolveTypeBinding());
    JExpression leftHandSide = convertExpressionWithoutSideEffects(e.getLeftOperand());

    BinaryOperator op =   convertBinaryOperator(e.getOperator());

    JExpression rightHandSide = convertExpressionWithoutSideEffects(e.getRightOperand());

    JExpression binaryExpression = new JBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

    //TODO Here we could translate the Idea of extended Operands
    //Maybe change tree Structure
    // into the cfa ast model
    // a x b x c is being translated to (((a x b) x c) x d)
    if (e.hasExtendedOperands()) {
      for (Expression extendedOperand : (List<Expression>) e.extendedOperands()) {
        binaryExpression = new JBinaryExpression(fileLoc, type, binaryExpression,
                                                 convertExpressionWithoutSideEffects(extendedOperand), op);
      }
    }

    return binaryExpression;
  }

  private BinaryOperator convertBinaryOperator(InfixExpression.Operator op) {


    if(op.equals(InfixExpression.Operator.PLUS)){
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
    } else if(op.equals(InfixExpression.Operator.NOT_EQUALS)){
     return BinaryOperator.NOT_EQUALS;
    } else {
      logger.log(Level.SEVERE, "Did not find Operator");
      return null;
    }

  }

  private JExpression convert(NumberLiteral e) {
    CFileLocation fileLoc = getFileLocation(e);
    JType type = convert(e.resolveTypeBinding());
    String valueStr = e.getToken();

    JBasicType t = ((JSimpleType) type).getType();

    switch(t){
    case INT:   return new JIntegerLiteralExpression(fileLoc, type, parseIntegerLiteral(valueStr, e ) );
    case FLOAT:  return new JFloatLiteralExpression(fileLoc, type, parseFloatLiteral(valueStr, e));

    case DOUBLE: return new JFloatLiteralExpression(fileLoc, type, parseFloatLiteral(valueStr, e));

    }

    return new JIntegerLiteralExpression( getFileLocation(e), convert(e.resolveTypeBinding()) , BigInteger.valueOf(Long.valueOf(e.getToken())));
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
    int last = s.length()-1;
    int bits = 32;

    if (s.charAt(last) == 'L' || s.charAt(last) == 'l' ) {
      last--;
      bits = 64;
    }

    s = s.substring(0, last+1);
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

      }else {
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


  JStringLiteralExpression convert(StringLiteral e) {
    CFileLocation fileLoc = getFileLocation(e);

    //TODO Prototype , String is in java a class Type
    JType type = new JDummyType("String");
    return new JStringLiteralExpression(fileLoc, type, e.getLiteralValue());
  }

  JNullLiteralExpression convert(NullLiteral e) {
    return new JNullLiteralExpression( getFileLocation(e));
  }


  JCharLiteralExpression convert(CharacterLiteral e) {
    CFileLocation fileLoc = getFileLocation(e);
     JType type = convert(e.resolveTypeBinding());
    return new JCharLiteralExpression(fileLoc, type, e.charValue());
  }

  public JExpression convertBooleanExpression(Expression e){

    JExpression exp = convertExpressionWithoutSideEffects(e);
    if (!isBooleanExpression(exp)) {

      // TODO: probably the type of the zero is not always correct
      JExpression zero = new JBooleanLiteralExpression(exp.getFileLocation(), (JType) exp.getExpressionType(), false);
      return new JBinaryExpression(exp.getFileLocation(), (JType) exp.getExpressionType(), exp, zero, BinaryOperator.NOT_EQUALS);
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

  public boolean isBooleanExpression(JExpression e) {
    if (e instanceof JBinaryExpression) {
      return BOOLEAN_BINARY_OPERATORS.contains(((JBinaryExpression)e).getOperator());

    } else if (e instanceof JUnaryExpression) {
      return ((JUnaryExpression) e).getOperator() == UnaryOperator.NOT;

    } else {
      return e instanceof JBooleanLiteralExpression || e instanceof JRunTimeTypeEqualsType;
    }
  }

  JReturnStatement getConstructorObjectReturn(ITypeBinding declaringClass , CFileLocation constructorFileLoc) {

    assert declaringClass.isClass() : "Method " + declaringClass.getName() + " is no Contructor";

    CFileLocation fileloc= new CFileLocation(constructorFileLoc.getEndingLineNumber(), constructorFileLoc.getFileName(), NO_LINE, constructorFileLoc.getEndingLineNumber(), constructorFileLoc.getEndingLineNumber());

    JClassType objectReturnType = (JClassType) convert(declaringClass);

    //TODO Ugly, change AStringLiteral, maybe complete new Type for Object return
    return new JObjectReferenceReturn(fileloc, new JStringLiteralExpression(fileloc, objectReturnType, objectReturnType.getName()), objectReturnType);
  }

  public JRunTimeTypeEqualsType convertClassRunTimeCompileTimeAccord( CFileLocation fileloc ,JSimpleDeclaration referencedVariable,
      JClassOrInterfaceType classType) {
    return new JRunTimeTypeEqualsType(fileloc, new JThisRunTimeType(fileloc, referencedVariable), classType );
  }

  public void assignRunTimeClass(JReferencedMethodInvocationExpression methodInvocation, JClassInstanzeCreation functionCall) {
    methodInvocation.setHasKnownRunTimeBinding(HAS_KNOWN_BINDING);

    JConstructorType constructorType = functionCall.getExpressionType();

    methodInvocation.setRunTimeBinding(constructorType.getReturnType());
  }

  public JExpressionAssignmentStatement getBooleanAssign(JExpression pLeftHandSide, boolean booleanLiteral) {
    return new JExpressionAssignmentStatement(pLeftHandSide.getFileLocation(), pLeftHandSide, new JBooleanLiteralExpression(pLeftHandSide.getFileLocation(),new JSimpleType(JBasicType.BOOLEAN), booleanLiteral));
  }

  public String getFullyQualifiedClassName(ITypeBinding classBinding) {
    return classBinding.getQualifiedName().replace('.', '_');
  }

  public JMethodDeclaration createDefaultConstructor(ITypeBinding classBinding) {

    @SuppressWarnings({ "cast", "unchecked" })
    List<JParameterDeclaration> param = new LinkedList<JParameterDeclaration>();


    // TODO File Location of Default Constructor???
    CFileLocation fileLoc = new CFileLocation(0, "", 0, 0, 0);

    JConstructorType type = new JConstructorType((JClassType) convert( classBinding), param , false);
    return new JConstructorDeclaration(fileLoc, type , getFullyQualifiedDefaultConstructorName(classBinding) , VisibilityModifier.PUBLIC  , false);
  }

  private String getFullyQualifiedDefaultConstructorName(ITypeBinding classBinding) {

    return (classBinding.getQualifiedName().replace('.', '_') + "_" + classBinding.getName()).replace('.', '_');
  }

}