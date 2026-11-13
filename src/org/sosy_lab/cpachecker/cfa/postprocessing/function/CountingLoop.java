// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * A loop that is controlled by a counter: the counter starts with the same constant whenever the
 * loop is entered, changes by the same constant offset in every iteration, and the loop goes on as
 * long as the counter compares in a fixed way to a bound that does not change while the loop runs.
 *
 * <p>Only some forms of such loops are recognized so far (see {@link #analyze}), each restriction
 * is one check that can be relaxed when another form is needed.
 *
 * @param entryNode the node at which each iteration begins
 * @param stayInLoopEdge the branch of the condition that goes on with the next iteration
 * @param exitEdge the branch of the condition that leaves the loop
 * @param counter the local variable that controls the loop
 * @param start the value of the counter whenever the loop is entered
 * @param modificationEdge the only edge of the loop that writes the counter
 * @param offset the value that {@code modificationEdge} adds to the counter
 * @param modification whether an iteration changes the counter before or after the condition
 * @param continueOperator the comparison that has to hold for the loop to go on, with the counter
 *     as its left operand
 * @param bound the value that the counter is compared to: an integer literal, or a variable that
 *     the loop does not change
 */
public record CountingLoop(
    Loop loop,
    CFANode entryNode,
    CAssumeEdge stayInLoopEdge,
    CFAEdge exitEdge,
    CVariableDeclaration counter,
    BigInteger start,
    CFAEdge modificationEdge,
    BigInteger offset,
    CounterModification modification,
    BinaryOperator continueOperator,
    CExpression bound) {

  /** Where a loop changes its counter, relative to the point at which it checks its condition. */
  public enum CounterModification {
    /** Changed before the check, as in a {@code do while} loop that ends with its condition. */
    BEFORE_CONDITION,
    /** Changed after the check, as in a {@code while} loop that begins with its condition. */
    AFTER_CONDITION;

    static CounterModification of(boolean pAlreadyPassed) {
      return pAlreadyPassed ? BEFORE_CONDITION : AFTER_CONDITION;
    }
  }

  /** The comparisons that are supported as loop condition so far. */
  private static final ImmutableSet<BinaryOperator> SUPPORTED_CONTINUE_OPERATORS =
      ImmutableSet.of(
          BinaryOperator.LESS_THAN,
          BinaryOperator.LESS_EQUAL,
          BinaryOperator.GREATER_THAN,
          BinaryOperator.GREATER_EQUAL);

  /**
   * Analyzes whether the given loop is a counting loop.
   *
   * <p>Recognized are loops with one entry node and one exit, which is one branch of the condition.
   * The condition compares a plain local integer variable, the counter, to an integer literal or to
   * a variable that the loop does not write, with {@code <}, {@code <=}, {@code >} or {@code >=}.
   * Exactly one edge of the loop writes the counter, it adds a constant to it, and every iteration
   * passes it exactly once. Whenever the loop is entered, the counter has the same constant value.
   *
   * @param pLoop the loop to analyze
   * @param pFunctionNodes all nodes of the function that contains the loop
   * @param pLogger logs at level {@link Level#FINE} why a loop is not recognized
   * @return the counting loop, or empty if the loop does not have one of the recognized forms
   */
  public static Optional<CountingLoop> analyze(
      Loop pLoop, Collection<CFANode> pFunctionNodes, LogManager pLogger) {
    ImmutableSet<CFANode> entryNodes =
        transformedImmutableSetCopy(pLoop.getIncomingEdges(), CFAEdge::getSuccessor);
    if (entryNodes.size() != 1 || pLoop.getOutgoingEdges().size() != 1) {
      return notCounting(pLoop, pLogger, "it has more than one entry node or more than one exit");
    }
    CFANode entryNode = getOnlyElement(entryNodes);
    CFAEdge exitEdge = getOnlyElement(pLoop.getOutgoingEdges());
    CFANode conditionNode = exitEdge.getPredecessor();
    if (conditionNode.getNumLeavingEdges() != 2
        || !(exitEdge instanceof CAssumeEdge)
        || !(otherLeavingEdge(conditionNode, exitEdge) instanceof CAssumeEdge stayInLoopEdge)) {
      return notCounting(pLoop, pLogger, "it is not left via one branch of a condition");
    }
    ImmutableSet<CFAEdge> innerEdges = pLoop.getInnerLoopEdges();

    Optional<LoopCondition> parsedCondition = parseLoopCondition(stayInLoopEdge, innerEdges);
    if (parsedCondition.isEmpty()) {
      return notCounting(
          pLoop,
          pLogger,
          "its condition does not compare a variable written in the loop to a constant or to a"
              + " variable that the loop does not write: "
              + stayInLoopEdge);
    }
    LoopCondition condition = parsedCondition.orElseThrow();
    CVariableDeclaration counter = condition.counter();
    String counterName = counter.getQualifiedName();
    if (!SUPPORTED_CONTINUE_OPERATORS.contains(condition.continueOperator())) {
      return notCounting(
          pLoop, pLogger, "its condition is not an ordered comparison: " + stayInLoopEdge);
    }

    // Everything below reasons about the values of the counter and the bound, which only works if
    // nothing outside the paths we look at can change them.
    if (!isUsableLocalInteger(counter)) {
      return notCounting(pLoop, pLogger, counterName + " is not a plain local integer variable");
    }
    if (addressIsTaken(pFunctionNodes, counter)) {
      return notCounting(pLoop, pLogger, "the address of " + counterName + " is taken");
    }
    if (condition.bound() instanceof CIdExpression boundVariable
        && !isUnchangedOutsideOfFunction(boundVariable.getDeclaration(), pFunctionNodes)) {
      return notCounting(
          pLoop, pLogger, "the bound " + boundVariable + " could change while the loop runs");
    }

    // The counter has to be changed by the same offset in every iteration, so there has to be
    // exactly one edge that writes it, and it has to add a constant.
    CFAEdge modificationEdge = null;
    for (CFAEdge edge : innerEdges) {
      if (writesVariable(edge, counter)) {
        if (modificationEdge != null) {
          return notCounting(pLoop, pLogger, counterName + " is written by more than one edge");
        }
        modificationEdge = edge;
      }
    }
    if (modificationEdge == null) {
      return notCounting(pLoop, pLogger, counterName + " is never written inside the loop");
    }
    Optional<BigInteger> offset = offsetAddedBy(modificationEdge, counter);
    if (offset.isEmpty()) {
      return notCounting(
          pLoop,
          pLogger,
          counterName + " is not changed by a constant offset: " + modificationEdge);
    }

    // That single edge also has to be passed exactly once per iteration, and we need to know
    // whether it comes before or after the check of the condition.
    Optional<CounterModification> modification =
        modificationRelativeToCondition(innerEdges, entryNode, conditionNode, modificationEdge);
    if (modification.isEmpty()) {
      return notCounting(
          pLoop,
          pLogger,
          counterName + " is not written exactly once on every path through the loop");
    }

    Optional<BigInteger> start = valueBeforeLoop(innerEdges, entryNode, counter);
    if (start.isEmpty()) {
      return notCounting(
          pLoop,
          pLogger,
          counterName + " does not have a known constant value when the loop is entered");
    }

    return Optional.of(
        new CountingLoop(
            pLoop,
            entryNode,
            stayInLoopEdge,
            exitEdge,
            counter,
            start.orElseThrow(),
            modificationEdge,
            offset.orElseThrow(),
            modification.orElseThrow(),
            condition.continueOperator(),
            condition.bound()));
  }

  private static CFAEdge otherLeavingEdge(CFANode pNode, CFAEdge pEdge) {
    return pNode.getLeavingEdge(0).equals(pEdge)
        ? pNode.getLeavingEdge(1)
        : pNode.getLeavingEdge(0);
  }

  /**
   * The condition under which a loop goes on.
   *
   * @param counter the variable that the loop compares
   * @param continueOperator the comparison that has to hold for the loop to go on, with the counter
   *     as its left operand
   * @param bound the integer literal or variable that the counter is compared to
   */
  private record LoopCondition(
      CVariableDeclaration counter, BinaryOperator continueOperator, CExpression bound) {}

  /**
   * Reads the condition under which the given edge continues a loop as a comparison of a variable
   * that the loop writes, the counter, to an integer literal or to a variable that the loop does
   * not write, normalized so that the counter is the left operand. Empty for every other shape.
   */
  private static Optional<LoopCondition> parseLoopCondition(
      CAssumeEdge pStayInLoopEdge, ImmutableSet<CFAEdge> pInnerEdges) {
    // The parser rewrites a condition that is not a comparison into a comparison to zero, so that
    // for example "while (x)" arrives here as "[!(x == 0)]".
    if (!(pStayInLoopEdge.getExpression() instanceof CBinaryExpression comparison)
        || !comparison.getOperator().isLogicalOperator()) {
      return Optional.empty();
    }
    BinaryOperator operator = comparison.getOperator();
    if (!pStayInLoopEdge.getTruthAssumption()) {
      operator = operator.getOppositLogicalOperator();
    }
    CExpression counter = comparison.getOperand1();
    CExpression bound = comparison.getOperand2();
    if (!isWrittenIn(counter, pInnerEdges) && isWrittenIn(bound, pInnerEdges)) {
      counter = comparison.getOperand2();
      bound = comparison.getOperand1();
      operator = operator.getSwitchOperandsSidesLogicalOperator();
    }
    if (!(counter instanceof CIdExpression identifier)
        || !(identifier.getDeclaration() instanceof CVariableDeclaration declaration)
        || !isWrittenIn(counter, pInnerEdges)) {
      return Optional.empty();
    }
    boolean boundIsUsable =
        bound instanceof CIntegerLiteralExpression
            || (bound instanceof CIdExpression && !isWrittenIn(bound, pInnerEdges));
    if (!boundIsUsable) {
      return Optional.empty();
    }
    return Optional.of(new LoopCondition(declaration, operator, bound));
  }

  /** Whether the given expression is a variable that one of the given edges writes. */
  private static boolean isWrittenIn(CExpression pExpression, ImmutableSet<CFAEdge> pEdges) {
    if (!(pExpression instanceof CIdExpression identifier)) {
      return false;
    }
    for (CFAEdge edge : pEdges) {
      if (writesVariable(edge, identifier.getDeclaration())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Whether the given declaration is a local variable or a parameter of the function with the given
   * nodes whose address is not taken, so that only edges of this function can change it.
   */
  private static boolean isUnchangedOutsideOfFunction(
      CSimpleDeclaration pDeclaration, Collection<CFANode> pFunctionNodes) {
    boolean isLocal =
        pDeclaration instanceof CParameterDeclaration
            || (pDeclaration instanceof CVariableDeclaration variable
                && !variable.isGlobal()
                && variable.getCStorageClass() == CStorageClass.AUTO);
    return isLocal && !addressIsTaken(pFunctionNodes, pDeclaration);
  }

  /**
   * Whether the given variable is an integer that lives only in its function and that is not read
   * or written by anything else, so a global, static or volatile variable is not usable.
   */
  private static boolean isUsableLocalInteger(CVariableDeclaration pVariable) {
    return !pVariable.isGlobal()
        && pVariable.getCStorageClass() == CStorageClass.AUTO
        && pVariable.getType().getCanonicalType() instanceof CSimpleType type
        && !type.isVolatile()
        && type.getType().isIntegerType();
  }

  /**
   * Whether the address of the given variable is taken anywhere in the given nodes of its function,
   * in which case a write through a pointer could change it without us seeing an edge that writes
   * it.
   */
  private static boolean addressIsTaken(
      Collection<CFANode> pFunctionNodes, CSimpleDeclaration pVariable) {
    for (CFANode node : pFunctionNodes) {
      for (CFAEdge edge : node.getLeavingEdges()) {
        for (AAstNode astNode : CFAUtils.getAstNodesFromCfaEdge(edge)) {
          for (AAstNode subNode : CFAUtils.traverseRecursively(astNode)) {
            if (subNode instanceof CUnaryExpression unaryExpression
                && unaryExpression.getOperator() == UnaryOperator.AMPER
                && unaryExpression.getOperand() instanceof CIdExpression identifier
                && pVariable.equals(identifier.getDeclaration())) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Whether the given edge changes the value of the given variable. Only complete for variables
   * whose address is never taken, because a write through a pointer is not detected.
   */
  private static boolean writesVariable(CFAEdge pEdge, CSimpleDeclaration pVariable) {
    if (pEdge instanceof CDeclarationEdge declarationEdge) {
      return pVariable.equals(declarationEdge.getDeclaration());
    }
    return pEdge instanceof CStatementEdge statementEdge
        && statementEdge.getStatement() instanceof CAssignment assignment
        && assignment.getLeftHandSide() instanceof CIdExpression identifier
        && pVariable.equals(identifier.getDeclaration());
  }

  /**
   * The constant offset that the given edge adds to the given variable, if it is an assignment of
   * the form {@code v = v + c}, {@code v = c + v} or {@code v = v - c}. Empty for every other write
   * of the variable.
   *
   * <p>The offset only has to be a literal because the parser folds a constant expression into one
   * before we see it.
   */
  private static Optional<BigInteger> offsetAddedBy(CFAEdge pEdge, CVariableDeclaration pVariable) {
    if (!(pEdge instanceof CStatementEdge statementEdge)
        || !(statementEdge.getStatement() instanceof CExpressionAssignmentStatement assignment)
        || !(assignment.getRightHandSide() instanceof CBinaryExpression sum)) {
      return Optional.empty();
    }
    boolean counterIsLeft = isVariable(sum.getOperand1(), pVariable);
    return switch (sum.getOperator()) {
      case PLUS -> {
        if (counterIsLeft && sum.getOperand2() instanceof CIntegerLiteralExpression literal) {
          yield Optional.of(literal.getValue());
        }
        if (isVariable(sum.getOperand2(), pVariable)
            && sum.getOperand1() instanceof CIntegerLiteralExpression literal) {
          yield Optional.of(literal.getValue());
        }
        yield Optional.empty();
      }
      case MINUS -> {
        if (counterIsLeft && sum.getOperand2() instanceof CIntegerLiteralExpression literal) {
          yield Optional.of(literal.getValue().negate());
        }
        yield Optional.empty();
      }
      default -> Optional.empty();
    };
  }

  /** Whether the given expression is exactly a read of the given variable. */
  private static boolean isVariable(CExpression pExpression, CVariableDeclaration pVariable) {
    return pExpression instanceof CIdExpression identifier
        && pVariable.equals(identifier.getDeclaration());
  }

  /**
   * Where the loop changes its counter, by following its body from the entry node to the node at
   * which it checks its condition. Empty if that differs between the paths, or if an iteration does
   * not pass the modification exactly once, because then the counter does not have a value that
   * only depends on the number of iterations.
   */
  private static Optional<CounterModification> modificationRelativeToCondition(
      ImmutableSet<CFAEdge> pInnerEdges,
      CFANode pEntry,
      CFANode pCondition,
      CFAEdge pModification) {
    Map<CFANode, Boolean> passedModification = new HashMap<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    passedModification.put(pEntry, false);
    waitlist.push(pEntry);
    while (!waitlist.isEmpty()) {
      CFANode node = waitlist.pop();
      boolean before = passedModification.get(node);
      for (CFAEdge edge : node.getLeavingEdges()) {
        if (!pInnerEdges.contains(edge)) {
          continue; // the edge that leaves the loop, which ends the last iteration
        }
        boolean isModification = edge.equals(pModification);
        if (before && isModification) {
          return Optional.empty(); // an iteration that changes the counter twice
        }
        boolean after = before || isModification;
        CFANode successor = edge.getSuccessor();
        if (successor.equals(pEntry)) {
          if (!after) {
            return Optional.empty(); // an iteration that does not change the counter at all
          }
          continue; // the next iteration starts over without a modification
        }
        Boolean known = passedModification.get(successor);
        if (known == null) {
          passedModification.put(successor, after);
          waitlist.push(successor);
        } else if (known != after) {
          // Either a branch writes the counter and another does not, or a nested loop writes it.
          return Optional.empty();
        }
      }
    }
    // Absent if the condition is not even reachable without starting another iteration.
    return Optional.ofNullable(passedModification.get(pCondition)).map(CounterModification::of);
  }

  /**
   * The value that the given variable has whenever the loop is entered. Searches backwards for the
   * writes that can be the last one before the loop. Empty if one of them does not write a
   * constant, if they do not all write the same one, or if the variable can still hold whatever it
   * was declared with, which is unknown.
   */
  private static Optional<BigInteger> valueBeforeLoop(
      ImmutableSet<CFAEdge> pInnerEdges, CFANode pEntry, CVariableDeclaration pVariable) {
    // Search backwards from the node where the loop begins, stopping at each write of the variable
    // because nothing before it reaches the loop without passing it. Going through the loop itself
    // is the only other way back to that node, so leaving those edges out reaches exactly the
    // writes that can be the last one before the loop.
    EdgeCollectingCFAVisitor lastWrites =
        new EdgeCollectingCFAVisitor(
            new DefaultCFAVisitor() {
              @Override
              public TraversalProcess visitEdge(CFAEdge pEdge) {
                return pInnerEdges.contains(pEdge) || writesVariable(pEdge, pVariable)
                    ? TraversalProcess.SKIP
                    : TraversalProcess.CONTINUE;
              }
            });
    CFATraversal.dfs().backwards().traverseOnce(pEntry, lastWrites);

    Set<BigInteger> values = new HashSet<>();
    for (CFAEdge edge : lastWrites.getVisitedEdges()) {
      if (pInnerEdges.contains(edge)) {
        continue; // an edge of the loop itself, which is not a way of reaching it from outside
      }
      if (writesVariable(edge, pVariable)) {
        Optional<BigInteger> value = constantWrittenBy(edge);
        if (value.isEmpty()) {
          return Optional.empty();
        }
        values.add(value.orElseThrow());
      } else if (edge.getPredecessor().getNumEnteringEdges() == 0) {
        // We reached the start of the function, so the variable is never written before the loop.
        return Optional.empty();
      }
    }
    return values.size() == 1 ? Optional.of(getOnlyElement(values)) : Optional.empty();
  }

  /**
   * The constant that the given edge writes, if it assigns or initializes with an integer literal.
   * Empty for every other write.
   */
  private static Optional<BigInteger> constantWrittenBy(CFAEdge pEdge) {
    CExpression written = null;
    if (pEdge instanceof CDeclarationEdge declarationEdge
        && declarationEdge.getDeclaration() instanceof CVariableDeclaration declaration
        && declaration.getInitializer() instanceof CInitializerExpression initializer) {
      written = initializer.getExpression();
    } else if (pEdge instanceof CStatementEdge statementEdge
        && statementEdge.getStatement() instanceof CExpressionAssignmentStatement assignment) {
      written = assignment.getRightHandSide();
    }
    return written instanceof CIntegerLiteralExpression literal
        ? Optional.of(literal.getValue())
        : Optional.empty();
  }

  /**
   * @return always empty, so that {@link #analyze} can return the result directly
   */
  private static Optional<CountingLoop> notCounting(
      Loop pLoop, LogManager pLogger, String pReason) {
    pLogger.logf(
        Level.FINE,
        "The loop with head %s is not a counting loop because %s",
        pLoop.getLoopHeads().iterator().next(),
        pReason);
    return Optional.empty();
  }
}
