// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.formula.parser;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.formula.parser.FormulaNode.FormulaNodeType;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class SyntaxTree {

  private FormulaNode root;

  public FormulaNode getRoot() {
    return root;
  }

  public SyntaxTree(String pFormula) {
    root = parse(pFormula);
  }
  public SyntaxTree() {
    root = LiteralNode.EMPTY;
  }

  public FormulaNode parse(BooleanFormula formula) {
    return parse(formula.toString());
  }

  public FormulaNode parse(String formula) {
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
          if (currentString.startsWith("`") && currentString.endsWith("`") && currentString.length()>1) {
            syntaxStack.push(new ExpressionNode(currentString.substring(1, currentString.length()-1)));
            currentString = "";
          }
          if (currentString.startsWith(" ") && currentString.endsWith(" ") && !currentString.isBlank()) {
            syntaxStack.push(new LiteralNode(currentString.trim()));
            currentString = " ";
          }
          if (currentString.startsWith(" ") && currentString.endsWith(")") && !currentString.isBlank()) {
            syntaxStack.push(new LiteralNode(currentString.substring(1, currentString.length()-1)));
            evaluate(syntaxStack);
            currentString = "";
          }
        }
      }
    }
    return syntaxStack.pop();
  }

  private void evaluate(ArrayDeque<FormulaNode> pSyntaxStack) {

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
        currExprNode.getOperands().addAll(reversed.stream().map(fn -> (ExpressionNode)fn).collect(
            Collectors.toList()));
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


  private void evaluateOrNode(OrNode orNode,
                               ArrayDeque<FormulaNode> reversed,
                               ArrayDeque<FormulaNode> pSyntaxStack) {
    FormulaNode left = reversed.pop();
    FormulaNode right = reversed.pop();

    if (LiteralNode.TRUE.equals(left) || LiteralNode.TRUE.equals(right)) {
      pSyntaxStack.push(LiteralNode.TRUE);
      return;
    }

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

    if (orParts.contains(LiteralNode.TRUE)) {
      pSyntaxStack.push(LiteralNode.TRUE);
      return;
    }
    orParts.remove(LiteralNode.FALSE);

    if (orParts.isEmpty()) {
      pSyntaxStack.push(LiteralNode.FALSE);
      return;
    }

    if (orParts.size() == 1) {
      pSyntaxStack.push(orParts.iterator().next());
    }

    for (FormulaNode orPart : orParts) {
      for (FormulaNode orPart2 : orParts) {
        if (orPart.equals(new NotNode(orPart2)) || orPart2.equals(new NotNode(orPart))){
          pSyntaxStack.push(LiteralNode.TRUE);
          return;
        }
      }
    }

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

  private void evaluateAndNode(AndNode andNode,
                               ArrayDeque<FormulaNode> reversed,
                               ArrayDeque<FormulaNode> pSyntaxStack) {
    FormulaNode left = reversed.pop();
    FormulaNode right = reversed.pop();

    if (LiteralNode.FALSE.equals(left) || LiteralNode.FALSE.equals(right)) {
      pSyntaxStack.push(LiteralNode.FALSE);
      return;
    }

    if (LiteralNode.TRUE.equals(left)) {
      pSyntaxStack.push(right);
      return;
    }

    if (LiteralNode.TRUE.equals(right)) {
      pSyntaxStack.push(left);
      return;
    }

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

    if (andParts.contains(LiteralNode.FALSE)) {
      pSyntaxStack.push(LiteralNode.FALSE);
      return;
    }
    andParts.remove(LiteralNode.TRUE);

    if (andParts.isEmpty()) {
      pSyntaxStack.push(LiteralNode.TRUE);
      return;
    }

    if (andParts.size() == 1) {
      pSyntaxStack.push(andParts.iterator().next());
    }

    for (FormulaNode andPart : andParts) {
      for (FormulaNode andPart2 : andParts) {
        if (andPart.equals(new NotNode(andPart2)) || andPart2.equals(new NotNode(andPart))){
          pSyntaxStack.push(LiteralNode.FALSE);
          return;
        }
      }
    }

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

  private void evaluateNotNode(NotNode notNode,
                               ArrayDeque<FormulaNode> reversed,
                               ArrayDeque<FormulaNode> pSyntaxStack) {
    FormulaNode top = reversed.pop();
    if (top instanceof NotNode) {
      pSyntaxStack.push(top.getSuccessors().get(0));
    } else {
      if (LiteralNode.TRUE.equals(top)) {
        pSyntaxStack.push(LiteralNode.FALSE);
      } else if (LiteralNode.FALSE.equals(top)) {
        pSyntaxStack.push(LiteralNode.TRUE);
      } else {
        notNode.setNode(top);
        pSyntaxStack.push(notNode);
      }
    }
  }

  @Override
  public String toString() {
    return root.toString();
  }
}
