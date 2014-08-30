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

import java.util.logging.Level;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.sosy_lab.common.log.LogManager;


/**
 * This Visitor simply extracts the AST of the JDT Parser for debug Purposes.
 */
class AstDebugg extends ASTVisitor {

  private final LogManager logger;

  public AstDebugg(LogManager logger) {
    this.logger = logger;
  }

  @Override
  public void preVisit(ASTNode node) {
    if (isProblematicNode(node)) {
      logger.log(Level.WARNING, "Error in node " + node.toString());
    }
  }

  private boolean isProblematicNode(ASTNode node) {
    int flags = node.getFlags();

    return ASTNode.RECOVERED == (flags & ASTNode.RECOVERED)
        || ASTNode.MALFORMED == (flags & ASTNode.MALFORMED);
  }

  public static String getTypeName(int type) {
    String name;

    switch (type) {
    case ASTNode.ANNOTATION_TYPE_DECLARATION:
      name = "ANNOTATION_TYPE_DECLARATION";
      break;
    case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
      name = "ANNOTATION_TYPE_MEMBER_DECLARATION";
      break;
    case ASTNode.ANONYMOUS_CLASS_DECLARATION:
      name = "ANONYMOUS_CLASS_DECLARATION";
      break;
    case ASTNode.ARRAY_ACCESS:
      name = "ARRAY_ACCESS";
      break;
    case ASTNode.ARRAY_CREATION:
      name = "ARRAY_CREATION";
      break;
    case ASTNode.ARRAY_INITIALIZER:
      name = "ARRAY_INITIALIZER";
      break;
    case ASTNode.ARRAY_TYPE:
      name = "ARRAY_TYPE";
      break;
    case ASTNode.ASSERT_STATEMENT:
      name = "ASSERT_STATEMENT";
      break;
    case ASTNode.ASSIGNMENT:
      name = "ASSIGNMENT";
      break;
    case ASTNode.BLOCK:
      name = "BLOCK";
      break;
    case ASTNode.BLOCK_COMMENT:
      name = "BLOCK_COMMENT";
      break;
    case ASTNode.BOOLEAN_LITERAL:
      name = "BOOLEAN_LITERAL";
      break;
    case ASTNode.BREAK_STATEMENT:
      name = "BREAK_STATEMENT";
      break;
    case ASTNode.CAST_EXPRESSION:
      name = "CAST_EXPRESSION";
      break;
    case ASTNode.CATCH_CLAUSE:
      name = "CATCH_CLAUSE";
      break;
    case ASTNode.CHARACTER_LITERAL:
      name = "CHARACTER_LITERAL";
      break;
    case ASTNode.CLASS_INSTANCE_CREATION:
      name = "CLASS_INSTANCE_CREATION";
      break;
    case ASTNode.COMPILATION_UNIT:
      name = "COMPILATION_UNIT";
      break;
    case ASTNode.CONDITIONAL_EXPRESSION:
      name = "CONDITIONAL_EXPRESSION";
      break;
    case ASTNode.CONSTRUCTOR_INVOCATION:
      name = "CONSTRUCTOR_INVOCATION";
      break;
    case ASTNode.CONTINUE_STATEMENT:
      name = "CONTINUE_STATEMENT";
      break;
    case ASTNode.DO_STATEMENT:
      name = "DO_STATEMENT";
      break;
    case ASTNode.EMPTY_STATEMENT:
      name = "EMPTY_STATEMENT";
      break;
    case ASTNode.ENHANCED_FOR_STATEMENT:
      name = "ENHANCED_FOR_STATEMENT";
      break;
    case ASTNode.ENUM_CONSTANT_DECLARATION:
      name = "ENUM_CONSTANT_DECLARATION";
      break;
    case ASTNode.ENUM_DECLARATION:
      name = "ENUM_DECLARATION";
      break;
    case ASTNode.EXPRESSION_STATEMENT:
      name = "EXPRESSION_STATEMENT";
      break;
    case ASTNode.FIELD_ACCESS:
      name = "FIELD_ACCESS";
      break;
    case ASTNode.FIELD_DECLARATION:
      name = "FIELD_DECLARATION";
      break;
    case ASTNode.FOR_STATEMENT:
      name = "FOR_STATEMENT";
      break;
    case ASTNode.IF_STATEMENT:
      name = "IF_STATEMENT";
      break;
    case ASTNode.IMPORT_DECLARATION:
      name = "IMPORT_DECLARATION";
      break;
    case ASTNode.INFIX_EXPRESSION:
      name = "INFIX_EXPRESSION";
      break;
    case ASTNode.INITIALIZER:
      name = "INITIALIZER";
      break;
    case ASTNode.INSTANCEOF_EXPRESSION:
      name = "INSTANCEOF_EXPRESSION";
      break;
    case ASTNode.JAVADOC:
      name = "JAVADOC";
      break;
    case ASTNode.LABELED_STATEMENT:
      name = "LABELED_STATEMENT";
      break;
    case ASTNode.LINE_COMMENT:
      name = "LINE_COMMENT";
      break;
    case ASTNode.MARKER_ANNOTATION:
      name = "MARKER_ANNOTATION";
      break;
    case ASTNode.MEMBER_REF:
      name = "MEMBER_REF";
      break;
    case ASTNode.MEMBER_VALUE_PAIR:
      name = "MEMBER_VALUE_PAIR";
      break;
    case ASTNode.METHOD_DECLARATION:
      name = "METHOD_DECLARATION";
      break;
    case ASTNode.METHOD_INVOCATION:
      name = "METHOD_INVOCATION";
      break;
    case ASTNode.METHOD_REF:
      name = "METHOD_REF";
      break;
    case ASTNode.METHOD_REF_PARAMETER:
      name = "METHOD_REF_PARAMETER";
      break;
    case ASTNode.MODIFIER:
      name = "MODIFIER";
      break;
    case ASTNode.NORMAL_ANNOTATION:
      name = "NORMAL_ANNOTATION";
      break;
    case ASTNode.NULL_LITERAL:
      name = "NULL_LITERAL";
      break;
    case ASTNode.NUMBER_LITERAL:
      name = "NUMBER_LITERAL";
      break;
    case ASTNode.PACKAGE_DECLARATION:
      name = "PACKAGE_DECLARATION";
      break;
    case ASTNode.PARAMETERIZED_TYPE:
      name = "PARAMETERIZED_TYPE";
      break;
    case ASTNode.PARENTHESIZED_EXPRESSION:
      name = "PARENTHESIZED_EXPRESSION";
      break;
    case ASTNode.POSTFIX_EXPRESSION:
      name = "POSTFIX_EXPRESSION";
      break;
    case ASTNode.PREFIX_EXPRESSION:
      name = "PREFIX_EXPRESSION";
      break;
    case ASTNode.PRIMITIVE_TYPE:
      name = "PRIMITIVE_TYPE";
      break;
    case ASTNode.QUALIFIED_NAME:
      name = "QUALIFIED_NAME";
      break;
    case ASTNode.QUALIFIED_TYPE:
      name = "QUALIFIED_TYPE";
      break;
    case ASTNode.RETURN_STATEMENT:
      name = "RETURN_STATEMENT";
      break;
    case ASTNode.SIMPLE_NAME:
      name = "SIMPLE_NAME";
      break;
    case ASTNode.SIMPLE_TYPE:
      name = "SIMPLE_TYPE";
      break;
    case ASTNode.SINGLE_MEMBER_ANNOTATION:
      name = "SINGLE_MEMBER_ANNOTATION";
      break;
    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      name = "SINGLE_VARIABLE_DECLARATION";
      break;
    case ASTNode.STRING_LITERAL:
      name = "STRING_LITERAL";
      break;
    case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
      name = "SUPER_CONSTRUCTOR_INVOCATION";
      break;
    case ASTNode.SUPER_FIELD_ACCESS:
      name = "SUPER_FIELD_ACCESS";
      break;
    case ASTNode.SUPER_METHOD_INVOCATION:
      name = "SUPER_METHOD_INVOCATION";
      break;
    case ASTNode.SWITCH_CASE:
      name = "SWITCH_CASE";
      break;
    case ASTNode.SWITCH_STATEMENT:
      name = "SWITCH_STATEMENT";
      break;
    case ASTNode.SYNCHRONIZED_STATEMENT:
      name = "SYNCHRONIZED_STATEMENT";
      break;
    case ASTNode.TAG_ELEMENT:
      name = "TAG_ELEMENT";
      break;
    case ASTNode.TEXT_ELEMENT:
      name = "TEXT_ELEMENT";
      break;
    case ASTNode.THIS_EXPRESSION:
      name = "THIS_EXPRESSION";
      break;
    case ASTNode.THROW_STATEMENT:
      name = "THROW_STATEMENT";
      break;
    case ASTNode.TRY_STATEMENT:
      name = "TRY_STATEMENT";
      break;
    case ASTNode.TYPE_DECLARATION:
      name = "TYPE_DECLARATION";
      break;
    case ASTNode.TYPE_DECLARATION_STATEMENT:
      name = "TYPE_DECLARATION_STATEMENT";
      break;
    case ASTNode.TYPE_LITERAL:
      name = "TYPE_LITERAL";
      break;
    case ASTNode.TYPE_PARAMETER:
      name = "TYPE_PARAMETER";
      break;
    case ASTNode.UNION_TYPE:
      name = "UNION_TYPE";
      break;
    case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
      name = "VARIABLE_DECLARATION_EXPRESSION";
      break;
    case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
      name = "VARIABLE_DECLARATION_FRAGMENT";
      break;
    case ASTNode.VARIABLE_DECLARATION_STATEMENT:
      name = "VARIABLE_DECLARATION_STATEMENT";
      break;
    case ASTNode.WHILE_STATEMENT:
      name = "WHILE_STATEMENT";
      break;
    case ASTNode.WILDCARD_TYPE:
      name = "WILDCARD_TYPE";
      break;
    default:
      name = "";
      break;
    }

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
