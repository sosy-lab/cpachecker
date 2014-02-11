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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.sosy_lab.common.LogManager;


/**
 * This Visitor simply extracts the AST of the JDT Parser for debug Purposes.
 */
class AstDebugg extends ASTVisitor {

  private final LogManager logger;

  public AstDebugg(LogManager logger) {
    this.logger = logger;
  }

  @Override
  public void preVisit(ASTNode problem) {
    // flags return the bitwise or of value Recovered =case 8, Malformed = case 1
    if (ASTNode.RECOVERED == (problem.getFlags() & ASTNode.RECOVERED) || ASTNode.MALFORMED == (problem.getFlags() & ASTNode.MALFORMED)) {
      logger.log(Level.WARNING, "Error " + problem.toString());
    }
  }


  public static String getTypeName(int type) {

    String name = "";
    switch (type) {


        case 81: name="ANNOTATION_TYPE_DECLARATION"; break;
        case 82: name="ANNOTATION_TYPE_MEMBER_DECLARATION"; break;
        case 1: name="ANONYMOUS_CLASS_DECLARATION"; break;
        case 2: name="ARRAY_ACCESS"; break;
        case 3: name="ARRAY_CREATION"; break;
        case 4: name="ARRAY_INITIALIZER"; break;
        case 5: name="ARRAY_TYPE"; break;
        case 6: name="ASSERT_STATEMENT"; break;
        case 7: name="ASSIGNMENT"; break;
        case 8: name="BLOCK"; break;
        case 64: name="BLOCK_COMMENT"; break;
        case 9: name="BOOLEAN_LITERAL"; break;
        case 10: name = "BREAK_STATEMENT"; break;
        case 11: name="CAST_EXPRESSION"; break;
        case 12: name="CATCH_CLAUSE"; break;
        case 13: name="CHARACTER_LITERAL"; break;
        case 14: name="CLASS_INSTANCE_CREATION"; break;
        case 15: name="COMPILATION_UNIT"; break;
        case 16: name="CONDITIONAL_EXPRESSION"; break;
        case 17: name="CONSTRUCTOR_INVOCATION"; break;
        case 18: name="CONTINUE_STATEMENT"; break;
        case 19: name="DO_STATEMENT"; break;
        case 20: name="EMPTY_STATEMENT"; break;
        case 70: name="ENHANCED_FOR_STATEMENT"; break;
         case 72: name="ENUM_CONSTANT_DECLARATION"; break;
        case 71: name="ENUM_DECLARATION"; break;
        case 21: name="EXPRESSION_STATEMENT"; break;
        case 22: name="FIELD_ACCESS"; break;
         case 23: name="FIELD_DECLARATION"; break;
         case 24: name="FOR_STATEMENT"; break;
        case 25: name="IF_STATEMENT"; break;
        case 26: name="IMPORT_DECLARATION"; break;
        case 27: name="INFIX_EXPRESSION"; break;
         case 28: name="INITIALIZER"; break;
         case 62: name="INSTANCEOF_EXPRESSION"; break;
         case 29: name="JAVADOC"; break;
         case 30: name="LABELED_STATEMENT"; break;
        case 63: name="LINE_COMMENT"; break;
         case 78: name="MARKER_ANNOTATION"; break;
        case 67: name="MEMBER_REF"; break;
         case 80: name="MEMBER_VALUE_PAIR"; break;
        case 31: name="METHOD_DECLARATION"; break;
         case 32: name="METHOD_INVOCATION"; break;
        case 68: name="METHOD_REF"; break;
        case 69: name="METHOD_REF_PARAMETER"; break;
        case 83: name="MODIFIER"; break;
         case 77: name="NORMAL_ANNOTATION"; break;
        case 33: name="NULL_LITERAL"; break;
        case 34: name="NUMBER_LITERAL"; break;
         case 35: name="PACKAGE_DECLARATION"; break;
        case 74: name="PARAMETERIZED_TYPE"; break;
        case 36: name="PARENTHESIZED_EXPRESSION"; break;
        case 37: name="POSTFIX_EXPRESSION"; break;
        case 38: name="PREFIX_EXPRESSION"; break;
        case 39: name="PRIMITIVE_TYPE"; break;
        case 40: name="QUALIFIED_NAME"; break;
        case 75: name="QUALIFIED_TYPE"; break;
        case 41: name="RETURN_STATEMENT"; break;
        case 42: name="SIMPLE_NAME"; break;
        case 43: name="SIMPLE_TYPE"; break;
        case 79: name="SINGLE_MEMBER_ANNOTATION"; break;
        case 44: name="SINGLE_VARIABLE_DECLARATION"; break;
        case 45: name="STRING_LITERAL"; break;
        case 46: name="SUPER_CONSTRUCTOR_INVOCATION"; break;
        case 47: name="SUPER_FIELD_ACCESS"; break;
        case 48: name="SUPER_METHOD_INVOCATION"; break;
        case 49: name="SWITCH_CASE"; break;
        case 50: name="SWITCH_STATEMENT"; break;
        case 51: name="SYNCHRONIZED_STATEMENT"; break;
        case 65: name="TAG_ELEMENT"; break;
        case 66: name="TEXT_ELEMENT"; break;
        case 52: name="THIS_EXPRESSION"; break;
        case 53: name="THROW_STATEMENT"; break;
        case 54: name="TRY_STATEMENT"; break;
        case 55: name="TYPE_DECLARATION"; break;
        case 56: name="TYPE_DECLARATION_STATEMENT"; break;
        case 57: name="TYPE_LITERAL"; break;
        case 73: name="TYPE_PARAMETER"; break;
        case 84: name="UNION_TYPE"; break;
        case 58: name="VARIABLE_DECLARATION_EXPRESSION"; break;
        case 59: name="VARIABLE_DECLARATION_FRAGMENT"; break;
        case 60: name="VARIABLE_DECLARATION_STATEMENT"; break;
        case 61: name="WHILE_STATEMENT"; break;
        case 76: name="WILDCARD_TYPE"; break;
    }

    return name;

  }

  @Override
  public boolean visit(MethodInvocation mI) {
    @SuppressWarnings("unused")
    IMethodBinding bind = mI.resolveMethodBinding();
    return true;
  }

 @Override
public boolean visit(ClassInstanceCreation cIC) {
  @SuppressWarnings("unused")
  IMethodBinding bind = cIC.resolveConstructorBinding();
  return true;
}

}