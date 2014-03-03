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

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JConstructorDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.ASTConverter.ModifierBean;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.TypeHierarchy.THTypeTable;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.util.NameConverter;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JConstructorType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;

public class TypeHierachyConverter {

  public final LogManager logger;
  public final THTypeConverter typeConverter;

  public TypeHierachyConverter(LogManager pLogger, THTypeTable pTypeTable) {
    logger = pLogger;
    typeConverter = new THTypeConverter(logger, pTypeTable);
  }

  /**
   * Converts a Method Declaration
   * of the JDT AST to a MethodDeclaration of the CFA AST
   *
   * @param md The method declaration to be transformed.
   * @param pFileOfDeclaration The File this method declaration was parsed from.
   * @return The CFA AST of this method declaration.
   */
  public JMethodDeclaration convert(final MethodDeclaration md, String pFileOfDeclaration) {

    IMethodBinding methodBinding = md.resolveBinding();

    if (methodBinding == null) {
      logger.log(Level.WARNING, md.getName());
      logger.log(Level.WARNING, " can't be resolved.");
      return JMethodDeclaration.createUnresolvedMethodDeclaration();
    }

    String methodName = NameConverter.convertName(methodBinding);
    String simpleName = methodBinding.getName();

    @SuppressWarnings("unchecked")
    ModifierBean mb = ModifierBean.getModifiers(md.modifiers());

    @SuppressWarnings({ "cast", "unchecked" })
    List<JParameterDeclaration> param =
        convertParameterList(md.parameters(), pFileOfDeclaration);

    List<JType> parameterTypes = FluentIterable.from(param).transform(
        new Function<JParameterDeclaration, JType>() {
          @Override
          @Nullable
          public JType apply(@Nullable JParameterDeclaration pDecl) {
            Preconditions.checkNotNull(pDecl);
            return pDecl.getType();
          }}).toList();

    FileLocation fileLoc = convertFileLocation(md, pFileOfDeclaration);

    if (md.isConstructor()) {

      // Constructors can't be declared by Interfaces
      JClassType declaringClass = convertClassOfConstructor(md);

      JConstructorType type =
          new JConstructorType(declaringClass, parameterTypes, md.isVarargs());

      return new JConstructorDeclaration(
          fileLoc, type, methodName, simpleName,
          param, mb.getVisibility(), mb.isStrictFp(), declaringClass);

    } else {

      // A Method is also abstract if its a member of an interface
      boolean isAbstract = mb.isAbstract() ||
          md.resolveBinding().getDeclaringClass().isInterface();

      JMethodType methodType =
          new JMethodType(convert(md.getReturnType2()), parameterTypes, md.isVarargs());


      JClassOrInterfaceType declaringClass = convertDeclaringClassType(md);


      return new JMethodDeclaration(fileLoc,
          methodType, methodName, simpleName,
          param, mb.getVisibility(), mb.isFinal(),
          isAbstract, mb.isStatic(), mb.isNative(),
          mb.isSynchronized(), mb.isStrictFp(),
          declaringClass);
    }
  }

  /**
   * Converts a List of Field Declaration into the intern AST.
   *
   * @param field Declarations given to be transformed.
   * @return intern AST of the Field Declarations.
   */
  public Set<JFieldDeclaration> convert(FieldDeclaration fd, String pFileOfDeclaration) {

    Set<JFieldDeclaration> result = new HashSet<>();

    Type type = fd.getType();

    FileLocation fileLoc = convertFileLocation(fd, pFileOfDeclaration);

    @SuppressWarnings("unchecked")
    ModifierBean mB = ModifierBean.getModifiers(fd.modifiers());

    assert (!mB.isAbstract()) : "Field Variable has this modifier?";
    assert (!mB.isNative()) : "Field Variable has this modifier?";
    assert (!mB.isStrictFp()) : "Field Variable has this modifier?";
    assert (!mB.isSynchronized()) : "Field Variable has this modifier?";


    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> vdfs =
        fd.fragments();

    for (VariableDeclarationFragment vdf : vdfs) {

      result.add(handleFragment(vdf, type, fileLoc, mB));
    }

    checkArgument(!result.isEmpty());

    return result;
  }


  private JFieldDeclaration handleFragment(VariableDeclarationFragment vdf,
      Type type, FileLocation fileLoc, ModifierBean mB) {

    IVariableBinding vB = vdf.resolveBinding();

    checkNotNull(vdf, "Can't resolve binding of field declaration "
        + vdf.getName().getFullyQualifiedName());

    String qualifiedName = NameConverter.convertName(vB);
    String simpleName = vB.getName();

    JFieldDeclaration newD = new JFieldDeclaration(fileLoc,
        convert(type), qualifiedName, simpleName,
        mB.isFinal(), mB.isStatic(), mB.isTransient(),
        mB.isVolatile(), mB.getVisibility());

    return newD;
  }

  private JClassOrInterfaceType convertDeclaringClassType(MethodDeclaration md) {

    IMethodBinding methodBinding = md.resolveBinding();

    if (methodBinding == null) {
      return JClassType.createUnresolvableType();
    }

    ITypeBinding typeBinding = methodBinding.getDeclaringClass();

    if (typeBinding == null) {
      return JClassType.createUnresolvableType();
    }

    return typeConverter.convertClassOrInterfaceType(typeBinding);
  }

  private JClassType convertClassOfConstructor(MethodDeclaration md) {
    JClassOrInterfaceType type = convertDeclaringClassType(md);

    if (type instanceof JClassType) {
      return (JClassType) type;
    } else {
      return JClassType.createUnresolvableType();
    }
  }

  private List<JParameterDeclaration> convertParameterList(
      List<SingleVariableDeclaration> ps, String fileOfDeclaration) {
    List<JParameterDeclaration> paramsList = new ArrayList<>(ps.size());

    for (org.eclipse.jdt.core.dom.SingleVariableDeclaration c : ps) {
      paramsList.add(convertParameter(c, fileOfDeclaration));
    }

    return paramsList;
  }

  private JParameterDeclaration convertParameter(SingleVariableDeclaration p, String fileOfDeclaration) {

    JType type = convert(p.getType());

    ModifierBean mb = ModifierBean.getModifiers(p.getModifiers());

    String qualifiedName = p.getName().getFullyQualifiedName();

    return new JParameterDeclaration(convertFileLocation(p, fileOfDeclaration), type, qualifiedName,
        mb.isFinal());
  }

  private JType convert(Type pType) {
    return typeConverter.convert(pType);
  }

  private FileLocation convertFileLocation(ASTNode l, String fileOfDeclaration) {

    if (l == null) {
      return FileLocation.DUMMY;
    } else if (l.getRoot().getNodeType() != ASTNode.COMPILATION_UNIT) {
      logger.log(Level.WARNING, "Can't find Placement Information for :"
          + l.toString());
      return FileLocation.DUMMY;
    }

    CompilationUnit co = (CompilationUnit) l.getRoot();

    return new FileLocation(co.getLineNumber(l.getLength() + l.getStartPosition()),
        fileOfDeclaration, l.getLength(), l.getStartPosition(),
        co.getLineNumber(l.getStartPosition()));
  }

  public JClassOrInterfaceType convertClassOrInterfaceType(ITypeBinding pTypeBinding) {
    return typeConverter.convertClassOrInterfaceType(pTypeBinding);
  }
}