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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.EclipseJavaParser.JavaFileAST;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.TypeHierarchy.THTypeTable;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.exceptions.JParserException;

/**
 * This Visitor Iterates the Compilation Unit for top-level Type Declarations,
 * converts them into types, and inserts them into a type Hierarchy.
 * Through bindings of the parser, every super type is converted and inserted
 * as well.
 */
public class TypeHierachyCreator extends ASTVisitor {

  private static final boolean VISIT_CHILDREN = true;

  private static final String JAVA_FILE_SUFFIX = ".java";

  private static final boolean SKIP_CHILDREN = false;

  private static final int FIRST = 0;

  @SuppressWarnings("unused")
  private final LogManager logger;
  private final THTypeTable typeTable;
  private final TypeHierachyConverter converter;

  /*
   * FileName of File, which was parsed into the currently visited Compilation Unit.
   */
  private String fileOfCU;






  /*
   * Used for propagating Errors due to wrong naming of files for classes.
   *
   */
  private boolean classNameException = false;
  private String className;
  private String expectedName;





/**
 * Creates the Visitor. The created Types are stored in the type table.
 *
 * @param pLogger Logger logging progress.
 * @param pTypeTable The type table of the type hierarchy, to be filled with the created types
 * @param pTypes Resulting Types are inserted in this map.
 * @param pTypeOfFiles Maps types to the files they were extracted from.
 */
  public TypeHierachyCreator(LogManager pLogger, THTypeTable pTypeTable) {
    logger = pLogger;
    typeTable = pTypeTable;
    converter = new TypeHierachyConverter(logger, typeTable);
  }

  public void createTypeHierachy(List<JavaFileAST> pJavaProgram) throws JParserException {

    for (JavaFileAST ast : pJavaProgram) {
      fileOfCU = ast.getFileName();
      CompilationUnit cu = ast.getAst();
      cu.accept(this);

      if (classNameException) {
        throw new JParserException(
          "The top-level class " + className + " is not declared within a file " +
              "with the expected filename " + expectedName +
              ".\n It is instead declared in " + fileOfCU);
      }
    }
  }

  @Override
  public boolean visit(MethodDeclaration pMd) {

    JMethodDeclaration decl = converter.convert(pMd, fileOfCU);
    typeTable.registerMethodDeclaration(decl);

    return SKIP_CHILDREN;
  }

  @Override
  public boolean visit(FieldDeclaration fD) {

    Set<JFieldDeclaration> decl = converter.convert(fD, fileOfCU);

    VariableDeclarationFragment vdf = (VariableDeclarationFragment) fD.fragments().get(FIRST);

    //TODO Add declaring class to JFielddeclaration
    IVariableBinding variableBinding = vdf.resolveBinding();
    checkNotNull(variableBinding);

    ITypeBinding typeBinding = variableBinding.getDeclaringClass();
    checkNotNull(typeBinding);

    JClassOrInterfaceType declaringClass = converter.convertClassOrInterfaceType(typeBinding);

    typeTable.registerFieldDeclaration(decl, declaringClass);

    return SKIP_CHILDREN;
  }

  @Override
  public boolean visit(EnumDeclaration node) {

    ITypeBinding typeBinding = node.resolveBinding();

    if (typeBinding != null) {
      JClassOrInterfaceType type = converter.convertClassOrInterfaceType(typeBinding);
      typeTable.registerFileNameOfType(type, fileOfCU);
    }

    return VISIT_CHILDREN;
  }

  @Override
  public boolean visit(TypeDeclaration node) {

    ITypeBinding typeBinding = node.resolveBinding();

    if (typeBinding != null) {

      if (typeBinding.isTopLevel()) {

        String simpleName = node.getName().getIdentifier();
        String expectedFilename = simpleName + JAVA_FILE_SUFFIX;

        if (!expectedFilename.equals(fileOfCU)) {
          classNameException = true;
          expectedName = expectedFilename;
          className = simpleName;

          return SKIP_CHILDREN;
        }
      }

      JClassOrInterfaceType type = converter.convertClassOrInterfaceType(typeBinding);
      typeTable.registerFileNameOfType(type, fileOfCU);
    }

    return !classNameException;
  }

  public THTypeTable getTypeTable() {
    return typeTable;
  }
}