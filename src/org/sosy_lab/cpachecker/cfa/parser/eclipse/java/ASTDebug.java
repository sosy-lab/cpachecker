// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import java.util.logging.Level;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.sosy_lab.common.log.LogManager;

/** This Visitor simply extracts the AST of the JDT Parser for debug Purposes. */
class ASTDebug extends ASTVisitor {

  private final LogManager logger;

  public ASTDebug(LogManager logger) {
    this.logger = logger;
  }

  @Override
  public void preVisit(ASTNode node) {
    if (isProblematicNode(node)) {
      logger.log(Level.WARNING, "Error in node " + node);
    }
  }

  private boolean isProblematicNode(ASTNode node) {
    int flags = node.getFlags();

    return ASTNode.RECOVERED == (flags & ASTNode.RECOVERED)
        || ASTNode.MALFORMED == (flags & ASTNode.MALFORMED);
  }

  public static String getTypeName(int type) {
    String name =
        switch (type) {
          case ASTNode.ANNOTATION_TYPE_DECLARATION -> "ANNOTATION_TYPE_DECLARATION";
          case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION -> "ANNOTATION_TYPE_MEMBER_DECLARATION";
          case ASTNode.ANONYMOUS_CLASS_DECLARATION -> "ANONYMOUS_CLASS_DECLARATION";
          case ASTNode.ARRAY_ACCESS -> "ARRAY_ACCESS";
          case ASTNode.ARRAY_CREATION -> "ARRAY_CREATION";
          case ASTNode.ARRAY_INITIALIZER -> "ARRAY_INITIALIZER";
          case ASTNode.ARRAY_TYPE -> "ARRAY_TYPE";
          case ASTNode.ASSERT_STATEMENT -> "ASSERT_STATEMENT";
          case ASTNode.ASSIGNMENT -> "ASSIGNMENT";
          case ASTNode.BLOCK -> "BLOCK";
          case ASTNode.BLOCK_COMMENT -> "BLOCK_COMMENT";
          case ASTNode.BOOLEAN_LITERAL -> "BOOLEAN_LITERAL";
          case ASTNode.BREAK_STATEMENT -> "BREAK_STATEMENT";
          case ASTNode.CAST_EXPRESSION -> "CAST_EXPRESSION";
          case ASTNode.CATCH_CLAUSE -> "CATCH_CLAUSE";
          case ASTNode.CHARACTER_LITERAL -> "CHARACTER_LITERAL";
          case ASTNode.CLASS_INSTANCE_CREATION -> "CLASS_INSTANCE_CREATION";
          case ASTNode.COMPILATION_UNIT -> "COMPILATION_UNIT";
          case ASTNode.CONDITIONAL_EXPRESSION -> "CONDITIONAL_EXPRESSION";
          case ASTNode.CONSTRUCTOR_INVOCATION -> "CONSTRUCTOR_INVOCATION";
          case ASTNode.CONTINUE_STATEMENT -> "CONTINUE_STATEMENT";
          case ASTNode.DO_STATEMENT -> "DO_STATEMENT";
          case ASTNode.EMPTY_STATEMENT -> "EMPTY_STATEMENT";
          case ASTNode.ENHANCED_FOR_STATEMENT -> "ENHANCED_FOR_STATEMENT";
          case ASTNode.ENUM_CONSTANT_DECLARATION -> "ENUM_CONSTANT_DECLARATION";
          case ASTNode.ENUM_DECLARATION -> "ENUM_DECLARATION";
          case ASTNode.EXPRESSION_STATEMENT -> "EXPRESSION_STATEMENT";
          case ASTNode.FIELD_ACCESS -> "FIELD_ACCESS";
          case ASTNode.FIELD_DECLARATION -> "FIELD_DECLARATION";
          case ASTNode.FOR_STATEMENT -> "FOR_STATEMENT";
          case ASTNode.IF_STATEMENT -> "IF_STATEMENT";
          case ASTNode.IMPORT_DECLARATION -> "IMPORT_DECLARATION";
          case ASTNode.INFIX_EXPRESSION -> "INFIX_EXPRESSION";
          case ASTNode.INITIALIZER -> "INITIALIZER";
          case ASTNode.INSTANCEOF_EXPRESSION -> "INSTANCEOF_EXPRESSION";
          case ASTNode.JAVADOC -> "JAVADOC";
          case ASTNode.LABELED_STATEMENT -> "LABELED_STATEMENT";
          case ASTNode.LINE_COMMENT -> "LINE_COMMENT";
          case ASTNode.MARKER_ANNOTATION -> "MARKER_ANNOTATION";
          case ASTNode.MEMBER_REF -> "MEMBER_REF";
          case ASTNode.MEMBER_VALUE_PAIR -> "MEMBER_VALUE_PAIR";
          case ASTNode.METHOD_DECLARATION -> "METHOD_DECLARATION";
          case ASTNode.METHOD_INVOCATION -> "METHOD_INVOCATION";
          case ASTNode.METHOD_REF -> "METHOD_REF";
          case ASTNode.METHOD_REF_PARAMETER -> "METHOD_REF_PARAMETER";
          case ASTNode.MODIFIER -> "MODIFIER";
          case ASTNode.NORMAL_ANNOTATION -> "NORMAL_ANNOTATION";
          case ASTNode.NULL_LITERAL -> "NULL_LITERAL";
          case ASTNode.NUMBER_LITERAL -> "NUMBER_LITERAL";
          case ASTNode.PACKAGE_DECLARATION -> "PACKAGE_DECLARATION";
          case ASTNode.PARAMETERIZED_TYPE -> "PARAMETERIZED_TYPE";
          case ASTNode.PARENTHESIZED_EXPRESSION -> "PARENTHESIZED_EXPRESSION";
          case ASTNode.POSTFIX_EXPRESSION -> "POSTFIX_EXPRESSION";
          case ASTNode.PREFIX_EXPRESSION -> "PREFIX_EXPRESSION";
          case ASTNode.PRIMITIVE_TYPE -> "PRIMITIVE_TYPE";
          case ASTNode.QUALIFIED_NAME -> "QUALIFIED_NAME";
          case ASTNode.QUALIFIED_TYPE -> "QUALIFIED_TYPE";
          case ASTNode.RETURN_STATEMENT -> "RETURN_STATEMENT";
          case ASTNode.SIMPLE_NAME -> "SIMPLE_NAME";
          case ASTNode.SIMPLE_TYPE -> "SIMPLE_TYPE";
          case ASTNode.SINGLE_MEMBER_ANNOTATION -> "SINGLE_MEMBER_ANNOTATION";
          case ASTNode.SINGLE_VARIABLE_DECLARATION -> "SINGLE_VARIABLE_DECLARATION";
          case ASTNode.STRING_LITERAL -> "STRING_LITERAL";
          case ASTNode.SUPER_CONSTRUCTOR_INVOCATION -> "SUPER_CONSTRUCTOR_INVOCATION";
          case ASTNode.SUPER_FIELD_ACCESS -> "SUPER_FIELD_ACCESS";
          case ASTNode.SUPER_METHOD_INVOCATION -> "SUPER_METHOD_INVOCATION";
          case ASTNode.SWITCH_CASE -> "SWITCH_CASE";
          case ASTNode.SWITCH_STATEMENT -> "SWITCH_STATEMENT";
          case ASTNode.SYNCHRONIZED_STATEMENT -> "SYNCHRONIZED_STATEMENT";
          case ASTNode.TAG_ELEMENT -> "TAG_ELEMENT";
          case ASTNode.TEXT_ELEMENT -> "TEXT_ELEMENT";
          case ASTNode.THIS_EXPRESSION -> "THIS_EXPRESSION";
          case ASTNode.THROW_STATEMENT -> "THROW_STATEMENT";
          case ASTNode.TRY_STATEMENT -> "TRY_STATEMENT";
          case ASTNode.TYPE_DECLARATION -> "TYPE_DECLARATION";
          case ASTNode.TYPE_DECLARATION_STATEMENT -> "TYPE_DECLARATION_STATEMENT";
          case ASTNode.TYPE_LITERAL -> "TYPE_LITERAL";
          case ASTNode.TYPE_PARAMETER -> "TYPE_PARAMETER";
          case ASTNode.UNION_TYPE -> "UNION_TYPE";
          case ASTNode.VARIABLE_DECLARATION_EXPRESSION -> "VARIABLE_DECLARATION_EXPRESSION";
          case ASTNode.VARIABLE_DECLARATION_FRAGMENT -> "VARIABLE_DECLARATION_FRAGMENT";
          case ASTNode.VARIABLE_DECLARATION_STATEMENT -> "VARIABLE_DECLARATION_STATEMENT";
          case ASTNode.WHILE_STATEMENT -> "WHILE_STATEMENT";
          case ASTNode.WILDCARD_TYPE -> "WILDCARD_TYPE";
          default -> "";
        };

    return name;
  }

  @Override
  public boolean visit(MethodInvocation mI) {
    mI.resolveMethodBinding();
    return true;
  }

  @Override
  public boolean visit(ClassInstanceCreation cIC) {
    cIC.resolveConstructorBinding();
    return true;
  }
}
