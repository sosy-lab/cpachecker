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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.EclipseJavaParser.JavaFileAST;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.TypeHierarchy.THTypeTable;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.exceptions.JParserException;

/**
 * This Visitor Iterates the Compilation Unit for top-level Type Declarations, converts them into
 * types, and inserts them into a type Hierarchy. Through bindings of the parser, every super type
 * is converted and inserted as well.
 */
class TypeHierachyCreator extends ASTVisitor {

  private static final boolean VISIT_CHILDREN = true;

  private static final boolean SKIP_CHILDREN = false;

  private static final int FIRST = 0;

  private final LogManager logger;
  private final THTypeTable typeTable;
  private final TypeHierachyConverter converter;

  /** FileName of File, which was parsed into the currently visited Compilation Unit. */
  private Path fileOfCU;

  /** Used for propagating errors due to wrong naming of files of classes. */
  private boolean classNameException = false;

  private String className;
  private String expectedName;

  /**
   * Creates the visitor. The created types are stored in the type table.
   *
   * @param pLogger Logger logging progress.
   * @param pTypeTable The type table of the type hierarchy, to be filled with the created types
   */
  public TypeHierachyCreator(LogManager pLogger, THTypeTable pTypeTable) {
    logger = pLogger;
    typeTable = pTypeTable;
    converter = new TypeHierachyConverter(logger, typeTable);
  }

  /**
   * Creates the visitor with the given default file name. The created types are stored in the type
   * table.
   *
   * <p>This constructor is useful when this class's visit-methods are not used through {@link
   * #createTypeHierachy}, but directly. When calling <code>createTypeHierachy</code>, the file
   * names will be set according to the parsed ASTs. Otherwise, the file name provided in this
   * method will be used.
   */
  public TypeHierachyCreator(LogManager pLogger, THTypeTable pTypeTable, Path fileName) {
    logger = pLogger;
    typeTable = pTypeTable;
    converter = new TypeHierachyConverter(logger, typeTable);
    fileOfCU = fileName;
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public void createTypeHierachy(List<JavaFileAST> pJavaProgram) throws JParserException {
    Path oldFileOfCU = fileOfCU;

    for (JavaFileAST ast : pJavaProgram) {
      fileOfCU = ast.getFile();
      CompilationUnit cu = ast.getAst();
      cu.accept(this);

      if (classNameException) {
        throw new JParserException(
            "The top-level class "
                + className
                + " is not declared within a file "
                + "with the expected filename "
                + expectedName
                + ".\n It is instead declared in "
                + fileOfCU);
      }
    }

    fileOfCU = oldFileOfCU;
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

    // TODO Add declaring class to JFielddeclaration
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
        boolean isPublic = false;
        for (Object mod : node.modifiers()) {
          if (!(mod instanceof Modifier)) {
            continue;
          }
          if (((Modifier) mod).isPublic()) {
            isPublic = true;
            break;
          }
        }

        if (isPublic) {
          String simpleName = node.getName().getIdentifier();
          String expectedFilename = simpleName + EclipseJavaParser.JAVA_SOURCE_FILE_EXTENSION;

          if (!expectedFilename.equals(fileOfCU.getFileName().toString())) {
            classNameException = true;
            expectedName = expectedFilename;
            className = simpleName;

            return SKIP_CHILDREN;
          }
        }
      }

      JClassOrInterfaceType type = converter.convertClassOrInterfaceType(typeBinding);
      typeTable.registerFileNameOfType(type, fileOfCU);
    } else {
      logger.logf(Level.WARNING, "Type %s has no binding.", node.toString());
    }

    return !classNameException;
  }

  @Override
  public boolean visit(AnonymousClassDeclaration pDeclaration) {
    final ITypeBinding classBinding = pDeclaration.resolveBinding();

    checkNotNull(classBinding);
    checkArgument(classBinding.isClass() || classBinding.isEnum() || classBinding.isInterface());

    final THTypeConverter typeConverter = new THTypeConverter(typeTable);
    final JClassOrInterfaceType classType = typeConverter.convertClassOrInterfaceType(classBinding);

    typeTable.registerFileNameOfType(classType, fileOfCU);

    return VISIT_CHILDREN;
  }

  public THTypeTable getTypeTable() {
    return typeTable;
  }
}
