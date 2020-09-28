// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.FormulaNode.FormulaNodeType;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class BooleanFormulaParser {

  /**
   * Parse a boolean formula, reduce and simplify clauses and convert it to infix notation.
   * @param formula the formula to be parsed
   * @return A syntax tree of the boolean formula
   */
  public static FormulaNode parse(BooleanFormula formula) {
    return parse(formula.toString());
  }

  /**
   * Parse the string representation of a boolean formula, reduce and simplify clauses and convert it to infix notation.
   * @param formula the formula to be parsed
   * @return A syntax tree of the boolean formula with infix string representation
   */
  public static FormulaNode parse(String formula) {
    ArrayDeque<FormulaNode> syntaxStack = new ArrayDeque<>();
    String currentString = "";
    for (int i = 0; i < formula.length(); i++) {
      currentString += formula.charAt(i);
      switch (currentString) {
        case " (":
        case "(": {
          syntaxStack.push(LiteralNode.OPEN_BRACKET);
          currentString = "";
          break;
        }
        case "`or`": {
          syntaxStack.push(new OrNode());
          currentString = "";
          break;
        }
        case "`and`": {
          syntaxStack.push(new AndNode());
          currentString = "";
          break;
        }
        case "`not`": {
          syntaxStack.push(new NotNode());
          currentString = "";
          break;
        }
        case ")": {
          evaluate(syntaxStack);
          currentString = "";
          break;
        }
        default: {
          if (currentString.startsWith("`") && currentString.endsWith("`") && currentString.length() > 1) {
            syntaxStack.push(new ExpressionNode(currentString.substring(1, currentString.length()-1)));
            currentString = "";
          }
          if (currentString.startsWith(" ") && currentString.endsWith(" ") && !currentString.isBlank()) {
            syntaxStack.push(new LiteralNode(currentString.trim()));
            currentString = " ";
          }
          if (currentString.startsWith(" ") && currentString.endsWith(")")) {
            syntaxStack.push(new LiteralNode(currentString.substring(1, currentString.length()-1)));
            evaluate(syntaxStack);
            currentString = "";
          }
        }
      }
    }
    return syntaxStack.pop();
  }

  private static void evaluate(ArrayDeque<FormulaNode> pSyntaxStack) {

    ArrayDeque<FormulaNode> reversed = new ArrayDeque<>();
    FormulaNode curr;
    while (!(curr = pSyntaxStack.pop()).equals(LiteralNode.OPEN_BRACKET)) {
      reversed.push(curr);
    }

    if (reversed.isEmpty()) {
      return;
    }

    curr = reversed.pop();
    switch (curr.getType()) {
      case ExpressionNode:
        ExpressionNode currExprNode = (ExpressionNode) curr;
        currExprNode.getOperands().addAll(reversed);
        pSyntaxStack.push(currExprNode);
        break;
      case AndNode:
        AndNode andNode = (AndNode) curr;
        evaluateAndNode(andNode, reversed, pSyntaxStack);
        break;
      case OrNode:
        OrNode orNode = (OrNode) curr;
        evaluateOrNode(orNode, reversed, pSyntaxStack);
        break;
      case NotNode:
        NotNode notNode = (NotNode) curr;
        evaluateNotNode(notNode, reversed, pSyntaxStack);
        break;
      default:
        pSyntaxStack.push(curr);
    }

  }


  private static void evaluateOrNode(OrNode orNode,
                               ArrayDeque<FormulaNode> reversed,
                               ArrayDeque<FormulaNode> pSyntaxStack) {
    FormulaNode left = reversed.pop();
    FormulaNode right = reversed.pop();

    // x or true = true
    if (LiteralNode.TRUE.equals(left) || LiteralNode.TRUE.equals(right)) {
      pSyntaxStack.push(LiteralNode.TRUE);
      return;
    }

    // false or x = x or false = x
    if (LiteralNode.FALSE.equals(left)) {
      pSyntaxStack.push(right);
      return;
    }

    if (LiteralNode.FALSE.equals(right)) {
      pSyntaxStack.push(left);
      return;
    }

    orNode.setLeft(left);
    orNode.setRight(right);

    Set<FormulaNode> orParts = new HashSet<>();
    ArrayDeque<OrNode> waitlist = new ArrayDeque<>();
    waitlist.push(orNode);

    while (!waitlist.isEmpty()) {
      OrNode curr = waitlist.pop();
      if (curr.getLeft().getType().equals(FormulaNodeType.OrNode)) {
        waitlist.push((OrNode) curr.getLeft());
      } else {
        orParts.add(curr.getLeft());
      }
      if (curr.getRight().getType().equals(FormulaNodeType.OrNode)) {
        waitlist.push((OrNode) curr.getRight());
      } else {
        orParts.add(curr.getRight());
      }
    }

    // a or true = true
    if (orParts.contains(LiteralNode.TRUE)) {
      pSyntaxStack.push(LiteralNode.TRUE);
      return;
    }

    // remove neutral element
    orParts.remove(LiteralNode.FALSE);

    if (orParts.isEmpty()) {
      pSyntaxStack.push(LiteralNode.FALSE);
      return;
    }

    if (orParts.size() == 1) {
      pSyntaxStack.push(orParts.iterator().next());
    }

    // a or not a = true
    for (FormulaNode orPart : orParts) {
      for (FormulaNode orPart2 : orParts) {
        // check both sides to prevent evaluating (not (not x))
        if (orPart.equals(new NotNode(orPart2)) || orPart2.equals(new NotNode(orPart))){
          pSyntaxStack.push(LiteralNode.TRUE);
          return;
        }
      }
    }

    // disjunction of all remaining nodes
    OrNode finalNode = new OrNode();

    for (FormulaNode orPart : orParts) {
      if (finalNode.getLeft() == null) {
        finalNode.setLeft(orPart);
      } else {
        if (finalNode.getRight() == null) {
          finalNode.setRight(orPart);
        } else {
          OrNode newFinalNode = new OrNode();
          newFinalNode.setLeft(finalNode);
          newFinalNode.setRight(orPart);
          finalNode = newFinalNode;
        }
      }
    }

    pSyntaxStack.push(finalNode);
  }

  private static void evaluateAndNode(AndNode andNode,
                               ArrayDeque<FormulaNode> reversed,
                               ArrayDeque<FormulaNode> pSyntaxStack) {
    FormulaNode left = reversed.pop();
    FormulaNode right = reversed.pop();

    // a and false = false
    if (LiteralNode.FALSE.equals(left) || LiteralNode.FALSE.equals(right)) {
      pSyntaxStack.push(LiteralNode.FALSE);
      return;
    }

    // a and true = true and a = a
    if (LiteralNode.TRUE.equals(left)) {
      pSyntaxStack.push(right);
      return;
    }

    if (LiteralNode.TRUE.equals(right)) {
      pSyntaxStack.push(left);
      return;
    }

    // find all conjunction args
    andNode.setLeft(left);
    andNode.setRight(right);

    Set<FormulaNode> andParts = new HashSet<>();
    ArrayDeque<AndNode> waitlist = new ArrayDeque<>();
    waitlist.push(andNode);

    while (!waitlist.isEmpty()) {
      AndNode curr = waitlist.pop();
      if (curr.getLeft().getType().equals(FormulaNodeType.AndNode)) {
        waitlist.push((AndNode)curr.getLeft());
      } else {
        andParts.add(curr.getLeft());
      }
      if (curr.getRight().getType().equals(FormulaNodeType.AndNode)) {
        waitlist.push((AndNode)curr.getRight());
      } else {
        andParts.add(curr.getRight());
      }
    }

    // a and false = false
    if (andParts.contains(LiteralNode.FALSE)) {
      pSyntaxStack.push(LiteralNode.FALSE);
      return;
    }

    // remove neutral element
    andParts.remove(LiteralNode.TRUE);

    if (andParts.isEmpty()) {
      pSyntaxStack.push(LiteralNode.TRUE);
      return;
    }

    if (andParts.size() == 1) {
      pSyntaxStack.push(andParts.iterator().next());
    }

    // a and not a = false
    for (FormulaNode andPart : andParts) {
      for (FormulaNode andPart2 : andParts) {
        // check both sides to prevent evaluating (not (not x))
        if (andPart.equals(new NotNode(andPart2)) || andPart2.equals(new NotNode(andPart))){
          pSyntaxStack.push(LiteralNode.FALSE);
          return;
        }
      }
    }

    // conjunction of all remaining nodes
    AndNode finalNode = new AndNode();

    for (FormulaNode andPart : andParts) {
      if (finalNode.getLeft() == null) {
        finalNode.setLeft(andPart);
      } else {
        if (finalNode.getRight() == null) {
          finalNode.setRight(andPart);
        } else {
          AndNode newFinalNode = new AndNode();
          newFinalNode.setLeft(finalNode);
          newFinalNode.setRight(andPart);
          finalNode = newFinalNode;
        }
      }
    }

    pSyntaxStack.push(finalNode);
  }

  private static void evaluateNotNode(NotNode notNode,
                               ArrayDeque<FormulaNode> reversed,
                               ArrayDeque<FormulaNode> pSyntaxStack) {
    FormulaNode top = reversed.pop();
    if (top instanceof NotNode) {
      pSyntaxStack.push(top.getSuccessors().get(0));
    } else {
      // not true = false
      if (LiteralNode.TRUE.equals(top)) {
        pSyntaxStack.push(LiteralNode.FALSE);
        // not false = true
      } else if (LiteralNode.FALSE.equals(top)) {
        pSyntaxStack.push(LiteralNode.TRUE);
        // simplify eg !(a = b) to a != b
      } else if (top.getType().equals(FormulaNodeType.ExpressionNode)) {
        ((ExpressionNode)top).negateOperator();
        pSyntaxStack.push(top);
      } else {
        notNode.setNode(top);
        pSyntaxStack.push(notNode);
      }
    }
  }

}
