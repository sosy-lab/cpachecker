// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static com.google.common.collect.Iterables.getOnlyElement;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.KnownFunctionBehavior;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CountingLoop.CounterModification;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * Finds the functions of a program that behave like a well-known function, see {@link
 * KnownFunctionBehavior}. This is a read-only analysis of the CFA of each single function, it has
 * to run before the supergraph is built.
 *
 * <p>So far, it recognizes implementations of {@code memset} that set the bytes one by one in a
 * counting loop, and functions that only forward their parameters to such an implementation.
 * Everything else is rejected, so every recognized function really has the described behavior.
 */
public final class KnownFunctionBehaviorDetector {

  /** Functions without a body that may be called by a function without changing its behavior. */
  private static final ImmutableSet<String> TERMINATING_FUNCTIONS =
      ImmutableSet.of("abort", "exit", "_Exit", "__assert_fail", "__VERIFIER_assume");

  private final MutableCFA cfa;
  private final LoopStructure loopStructure;
  private final MachineModel machineModel;
  private final LogManager logger;

  private KnownFunctionBehaviorDetector(
      MutableCFA pCfa, LoopStructure pLoopStructure, LogManager pLogger) {
    cfa = pCfa;
    loopStructure = pLoopStructure;
    machineModel = pCfa.getMachineModel();
    logger = pLogger;
  }

  /**
   * Finds the functions of the given CFA that behave like a well-known function.
   *
   * @param pCfa the CFA, in which each function is still a separate graph
   * @param pLoopStructure the loop structure of the CFA
   * @param pLogger logs each recognized function at {@link Level#INFO}, and at {@link Level#FINE}
   *     why some functions that look similar to one are not recognized
   * @return the recognized functions with their behavior
   */
  public static ImmutableMap<String, KnownFunctionBehavior> detect(
      MutableCFA pCfa, LoopStructure pLoopStructure, LogManager pLogger) {
    return new KnownFunctionBehaviorDetector(pCfa, pLoopStructure, pLogger).detect();
  }

  private ImmutableMap<String, KnownFunctionBehavior> detect() {
    Map<String, KnownFunctionBehavior> detected = new LinkedHashMap<>();
    Map<String, String> rejected = new LinkedHashMap<>();
    List<CFunctionEntryNode> candidates = new ArrayList<>();

    for (String function : cfa.getAllFunctionNames()) {
      if (!(cfa.getFunctionHead(function) instanceof CFunctionEntryNode entry)) {
        continue;
      }
      Optional<String> signatureProblem = whyNotMemsetSignature(entry);
      if (signatureProblem.isPresent()) {
        if (looksLikeMemset(function)) {
          rejected.put(function, signatureProblem.orElseThrow());
        }
        continue;
      }
      Optional<String> loopProblem = whyNotByteLoopMemset(entry);
      if (loopProblem.isEmpty()) {
        detected.put(function, KnownFunctionBehavior.MEMSET);
        logDetected(entry, "byte loop");
      } else {
        // Only report functions that look like a memset, everything else would be noise.
        if (looksLikeMemset(function) || loopStructure.getLoopsForFunction(function).size() == 1) {
          rejected.put(function, loopProblem.orElseThrow());
        }
        candidates.add(entry);
      }
    }

    // Wrappers can only be recognized once the function that they call is known, so repeat until
    // nothing changes.
    boolean changed = true;
    while (changed) {
      changed = false;
      for (CFunctionEntryNode entry : candidates) {
        String function = entry.getFunctionName();
        if (detected.containsKey(function)) {
          continue;
        }
        WrapperCheck check = checkWrapper(entry, detected);
        if (check.problem() == null) {
          detected.put(function, KnownFunctionBehavior.MEMSET);
          logDetected(entry, "wrapper of " + check.callee());
          changed = true;
        } else if (looksLikeMemset(function)
            && loopStructure.getLoopsForFunction(function).isEmpty()) {
          // without a loop, why it is not a wrapper is more useful than why it is no byte loop
          rejected.put(function, check.problem());
        }
      }
    }

    for (Map.Entry<String, String> rejection : rejected.entrySet()) {
      if (!detected.containsKey(rejection.getKey())) {
        logger.logf(
            Level.FINE,
            "Known function behavior: function %s is not recognized as MEMSET implementation"
                + " because %s",
            rejection.getKey(),
            rejection.getValue());
      }
    }
    return ImmutableMap.copyOf(detected);
  }

  private void logDetected(CFunctionEntryNode pEntry, String pReason) {
    logger.logf(
        Level.INFO,
        "Known function behavior: function %s (%s) is a MEMSET implementation (%s)",
        pEntry.getFunctionName(),
        pEntry.getFileLocation(),
        pReason);
  }

  private static boolean looksLikeMemset(String pFunction) {
    return Ascii.toLowerCase(pFunction).contains("memset");
  }

  /**
   * Checks the parameters {@code (s, c, n)} and the return type of a memset implementation: a
   * pointer, an integer, and an unsigned integer, and the function returns a pointer or nothing.
   * The count has to be unsigned, because otherwise a negative count would not mean the same as for
   * {@code memset}, which takes a {@code size_t}.
   */
  private Optional<String> whyNotMemsetSignature(CFunctionEntryNode pEntry) {
    ImmutableList<CParameterDeclaration> parameters = pEntry.getFunctionParameters();
    if (parameters.size() != 3) {
      return Optional.of("it does not have three parameters");
    }
    if (!(parameters.get(0).getType().getCanonicalType() instanceof CPointerType)) {
      return Optional.of("its first parameter is not a pointer");
    }
    if (!(parameters.get(1).getType().getCanonicalType() instanceof CSimpleType valueType)
        || !valueType.getType().isIntegerType()) {
      return Optional.of("its second parameter is not an integer");
    }
    if (!(parameters.get(2).getType().getCanonicalType() instanceof CSimpleType countType)
        || !countType.getType().isIntegerType()
        || machineModel.isSigned(countType)) {
      return Optional.of("its third parameter is not an unsigned integer");
    }
    CType returnType = pEntry.getFunctionDefinition().getType().getReturnType().getCanonicalType();
    if (!(returnType instanceof CVoidType) && !(returnType instanceof CPointerType)) {
      return Optional.of("it returns neither a pointer nor nothing");
    }
    return Optional.empty();
  }

  /**
   * Checks whether the given function sets the bytes one by one: {@code for (i = 0; i < n; i++)
   * q[i] = c;}, where {@code q} is the pointer parameter {@code s} or a local variable that holds
   * it, maybe cast to a pointer to {@code char}. Besides that, the function may only check
   * conditions, write its own local variables, call functions that only check conditions or stop
   * the program, and return {@code s} or nothing.
   */
  private Optional<String> whyNotByteLoopMemset(CFunctionEntryNode pEntry) {
    String function = pEntry.getFunctionName();
    Collection<CFANode> nodes = cfa.getFunctionNodes(function);
    ImmutableList<CParameterDeclaration> parameters = pEntry.getFunctionParameters();
    CParameterDeclaration pointer = parameters.get(0);
    CParameterDeclaration value = parameters.get(1);
    CParameterDeclaration count = parameters.get(2);

    ImmutableCollection<Loop> loops = loopStructure.getLoopsForFunction(function);
    if (loops.size() != 1) {
      return Optional.of("it does not contain exactly one loop");
    }
    Loop loop = getOnlyElement(loops);
    Optional<CountingLoop> analyzed = CountingLoop.analyze(loop, nodes, logger);
    if (analyzed.isEmpty()) {
      return Optional.of("its loop is not a counting loop");
    }
    CountingLoop countingLoop = analyzed.orElseThrow();
    if (!countingLoop.start().equals(BigInteger.ZERO)
        || !countingLoop.offset().equals(BigInteger.ONE)
        || countingLoop.modification() != CounterModification.AFTER_CONDITION
        || countingLoop.continueOperator() != BinaryOperator.LESS_THAN
        || !isVariable(countingLoop.bound(), count)) {
      return Optional.of("its loop does not count from 0 to the third parameter");
    }
    if (!counterCoversCount(countingLoop.counter(), count)) {
      return Optional.of("the counter of its loop cannot hold every value of the count");
    }

    // Every edge has to be harmless, except for the one that writes the bytes.
    CFAEdge write = null;
    for (CFANode node : nodes) {
      for (CFAEdge edge : node.getLeavingEdges()) {
        if (writesParameter(edge, parameters)) {
          return Optional.of("it changes one of its parameters: " + edge);
        }
        if (isMemoryWrite(edge)) {
          if (write != null) {
            return Optional.of("it writes memory more than once: " + edge);
          }
          write = edge;
        } else if (!isHarmless(edge) && !isLocalAssignment(edge) && !isCheckingCall(edge)) {
          return Optional.of("it contains an operation that is not allowed: " + edge);
        }
        if (edge instanceof CReturnStatementEdge returnEdge
            && !returnsNothingOrParameter(returnEdge, pointer)) {
          return Optional.of("it returns something else than its first parameter: " + edge);
        }
      }
    }
    if (write == null) {
      return Optional.of("it does not write memory");
    }
    if (!loop.getInnerLoopEdges().contains(write)) {
      return Optional.of("it writes memory outside of its loop: " + write);
    }
    Optional<String> writeProblem =
        whyNotByteWrite(write, countingLoop, pointer, value, nodes, loop.getInnerLoopEdges());
    if (writeProblem.isPresent()) {
      return writeProblem;
    }
    if (!isWrittenInEveryIterationBeforeTheCounter(loop, countingLoop, write)) {
      return Optional.of(
          "its loop does not write exactly once per iteration, before changing the counter");
    }
    if (canReturnAvoiding(
        pEntry, edge -> loop.getIncomingEdges().contains(edge) || impliesZero(edge, count))) {
      return Optional.of("it can return without running its loop although the count is not 0");
    }
    return Optional.empty();
  }

  /**
   * Checks that the given edge is {@code q[i] = c} or {@code *(q + i) = c} with a byte-sized
   * target, the counter {@code i}, the value parameter {@code c} (maybe cast), and a pointer {@code
   * q} that is the pointer parameter or a local variable that is set to it once before the loop.
   */
  private Optional<String> whyNotByteWrite(
      CFAEdge pWrite,
      CountingLoop pLoop,
      CParameterDeclaration pPointer,
      CParameterDeclaration pValue,
      Collection<CFANode> pNodes,
      ImmutableSet<CFAEdge> pLoopEdges) {
    if (!(pWrite instanceof CStatementEdge statementEdge)
        || !(statementEdge.getStatement() instanceof CAssignment assignment)
        || assignment instanceof CFunctionCall) {
      return Optional.of("its memory write is not a plain assignment: " + pWrite);
    }
    CLeftHandSide target = assignment.getLeftHandSide();
    if (!(target.getExpressionType().getCanonicalType() instanceof CSimpleType targetType)
        || targetType.getType() != CBasicType.CHAR) {
      return Optional.of("its memory write does not write a char: " + pWrite);
    }
    if (!(assignment.getRightHandSide() instanceof CExpression written)
        || !isVariable(withoutCasts(written), pValue)) {
      return Optional.of("its memory write does not write the second parameter: " + pWrite);
    }

    @Nullable CExpression base = null;
    if (target instanceof CArraySubscriptExpression subscript
        && isVariable(withoutCasts(subscript.getSubscriptExpression()), pLoop.counter())) {
      base = withoutCasts(subscript.getArrayExpression());
    } else if (target instanceof CPointerExpression dereference
        && withoutCasts(dereference.getOperand()) instanceof CBinaryExpression sum
        && sum.getOperator() == BinaryOperator.PLUS) {
      if (isVariable(withoutCasts(sum.getOperand2()), pLoop.counter())) {
        base = withoutCasts(sum.getOperand1());
      } else if (isVariable(withoutCasts(sum.getOperand1()), pLoop.counter())) {
        base = withoutCasts(sum.getOperand2());
      }
    }
    if (!(base instanceof CIdExpression baseVariable)) {
      return Optional.of("its memory write is not indexed by the counter: " + pWrite);
    }
    if (isVariable(baseVariable, pPointer)) {
      return Optional.empty();
    }
    if (!(baseVariable.getDeclaration() instanceof CVariableDeclaration baseDeclaration)
        || !isPlainLocal(baseDeclaration)
        || addressIsTaken(pNodes, baseDeclaration)) {
      return Optional.of("its memory write does not go through the first parameter: " + pWrite);
    }
    List<CFAEdge> baseWrites = new ArrayList<>();
    for (CFANode node : pNodes) {
      for (CFAEdge edge : node.getLeavingEdges()) {
        if (assignsValue(edge, baseDeclaration)) {
          baseWrites.add(edge);
        }
      }
    }
    if (baseWrites.size() != 1
        || pLoopEdges.contains(baseWrites.getFirst())
        || !(valueWrittenBy(baseWrites.getFirst()) instanceof CExpression baseValue)
        || !isVariable(withoutCasts(baseValue), pPointer)) {
      return Optional.of(
          "the pointer that it writes through is not set to the first parameter exactly once"
              + " before the loop: "
              + pWrite);
    }
    return Optional.empty();
  }

  /**
   * Whether every iteration of the loop executes the given write once, after the check of the
   * condition and before the change of the counter, so that it writes at every index from the start
   * of the counter to the one before the bound. Only loops whose body does not branch are accepted.
   */
  private static boolean isWrittenInEveryIterationBeforeTheCounter(
      Loop pLoop, CountingLoop pCountingLoop, CFAEdge pWrite) {
    CFANode conditionNode = pCountingLoop.exitEdge().getPredecessor();
    for (CFANode node : pLoop.getLoopNodes()) {
      if (!node.equals(conditionNode) && node.getNumLeavingEdges() != 1) {
        return false;
      }
    }
    boolean writeSeen = false;
    CFAEdge edge = pCountingLoop.stayInLoopEdge();
    CFANode node = edge.getSuccessor();
    while (!node.equals(pCountingLoop.entryNode())) {
      if (node.equals(conditionNode) || node.getNumLeavingEdges() != 1) {
        return false;
      }
      edge = node.getLeavingEdge(0);
      if (edge.equals(pWrite)) {
        writeSeen = true;
      } else if (edge.equals(pCountingLoop.modificationEdge())) {
        return writeSeen;
      }
      node = edge.getSuccessor();
    }
    return false;
  }

  private boolean counterCoversCount(CVariableDeclaration pCounter, CParameterDeclaration pCount) {
    return pCounter.getType().getCanonicalType() instanceof CSimpleType counterType
        && pCount.getType().getCanonicalType() instanceof CSimpleType countType
        && machineModel.getMinimalIntegerValue(counterType).signum() <= 0
        && machineModel
                .getMaximalIntegerValue(counterType)
                .compareTo(machineModel.getMaximalIntegerValue(countType))
            >= 0;
  }

  /** The result of checking whether a function is a wrapper of a known memset implementation. */
  private record WrapperCheck(@Nullable String callee, @Nullable String problem) {}

  /**
   * Checks whether the given function only calls a known memset implementation with its own
   * parameters {@code (s, c, n)} in this order, and returns its result, {@code s}, or nothing.
   * Besides that, it may only check conditions, write its own local variables, and call functions
   * that only check conditions or stop the program.
   */
  private WrapperCheck checkWrapper(
      CFunctionEntryNode pEntry, Map<String, KnownFunctionBehavior> pKnown) {
    String function = pEntry.getFunctionName();
    Collection<CFANode> nodes = cfa.getFunctionNodes(function);
    ImmutableList<CParameterDeclaration> parameters = pEntry.getFunctionParameters();

    if (!loopStructure.getLoopsForFunction(function).isEmpty()) {
      return new WrapperCheck(null, "it is not a wrapper, because it contains a loop");
    }
    CFAEdge call = null;
    CFunctionCall callStatement = null;
    for (CFANode node : nodes) {
      for (CFAEdge edge : node.getLeavingEdges()) {
        if (writesParameter(edge, parameters)) {
          return new WrapperCheck(null, "it changes one of its parameters: " + edge);
        }
        if (isHarmless(edge) || isLocalAssignment(edge) || isCheckingCall(edge)) {
          continue;
        }
        if (edge instanceof CStatementEdge statementEdge
            && statementEdge.getStatement() instanceof CFunctionCall functionCall
            && (functionCall instanceof CFunctionCallStatement
                || (functionCall instanceof CFunctionCallAssignmentStatement assignment
                    && isLocalVariable(assignment.getLeftHandSide())))) {
          if (call != null) {
            return new WrapperCheck(null, "it is not a wrapper, because it calls more functions");
          }
          call = edge;
          callStatement = functionCall;
          continue;
        }
        return new WrapperCheck(
            null, "it is not a wrapper, because it contains another operation: " + edge);
      }
    }
    if (call == null) {
      return new WrapperCheck(null, "it is not a wrapper, because it calls no function");
    }

    Optional<String> callee = CFAUtils.getFunctionCallName(call);
    if (callee.isEmpty() || !pKnown.containsKey(callee.orElseThrow())) {
      return new WrapperCheck(
          null,
          "it calls "
              + callee.orElse("a function pointer")
              + ", which is not known to be a MEMSET implementation");
    }
    List<CExpression> arguments =
        callStatement.getFunctionCallExpression().getParameterExpressions();
    if (arguments.size() != 3
        || !isVariable(withoutCasts(arguments.get(0)), parameters.get(0))
        || !isVariable(withoutCasts(arguments.get(1)), parameters.get(1))
        || !isVariable(withoutCasts(arguments.get(2)), parameters.get(2))) {
      return new WrapperCheck(
          null, "it does not pass its own parameters in the same order to " + callee.orElseThrow());
    }

    @Nullable CSimpleDeclaration result =
        callStatement instanceof CFunctionCallAssignmentStatement assignment
                && assignment.getLeftHandSide() instanceof CIdExpression resultVariable
            ? resultVariable.getDeclaration()
            : null;
    for (CFANode node : nodes) {
      for (CFAEdge edge : node.getLeavingEdges()) {
        if (result != null && !edge.equals(call) && assignsValue(edge, result)) {
          return new WrapperCheck(null, "it changes the result of " + callee.orElseThrow());
        }
        if (edge instanceof CReturnStatementEdge returnEdge
            && !returnsNothingOrParameter(returnEdge, parameters.get(0))
            && !(result != null
                && returnEdge.getExpression().isPresent()
                && isVariable(withoutCasts(returnEdge.getExpression().orElseThrow()), result))) {
          return new WrapperCheck(
              null, "it returns something else than its first parameter or the result: " + edge);
        }
      }
    }
    final CFAEdge finalCall = call;
    if (canReturnAvoiding(pEntry, edge -> edge.equals(finalCall))) {
      return new WrapperCheck(null, "it can return without calling " + callee.orElseThrow());
    }
    return new WrapperCheck(callee.orElseThrow(), null);
  }

  /** Whether the function can return normally without passing one of the given edges. */
  private static boolean canReturnAvoiding(FunctionEntryNode pEntry, Predicate<CFAEdge> pAvoided) {
    Optional<FunctionExitNode> exit = pEntry.getExitNode();
    if (exit.isEmpty()) {
      return false; // the function never returns
    }
    NodeCollectingCFAVisitor reached =
        new NodeCollectingCFAVisitor(
            new DefaultCFAVisitor() {
              @Override
              public TraversalProcess visitEdge(CFAEdge pEdge) {
                return pAvoided.test(pEdge) ? TraversalProcess.SKIP : TraversalProcess.CONTINUE;
              }
            });
    CFATraversal.dfs().traverseOnce(pEntry, reached);
    return reached.getVisitedNodes().contains(exit.orElseThrow());
  }

  /**
   * Whether the given edge only continues if the given unsigned variable is 0, e.g. {@code [n ==
   * 0]} or {@code [!(n > 0)]}.
   */
  private static boolean impliesZero(CFAEdge pEdge, CParameterDeclaration pCount) {
    if (!(pEdge instanceof CAssumeEdge assumeEdge)
        || !(assumeEdge.getExpression() instanceof CBinaryExpression comparison)
        || !comparison.getOperator().isLogicalOperator()) {
      return false;
    }
    BinaryOperator operator = comparison.getOperator();
    if (!assumeEdge.getTruthAssumption()) {
      operator = operator.getOppositLogicalOperator();
    }
    CExpression variable = withoutCasts(comparison.getOperand1());
    CExpression constant = withoutCasts(comparison.getOperand2());
    if (variable instanceof CIntegerLiteralExpression) {
      variable = withoutCasts(comparison.getOperand2());
      constant = withoutCasts(comparison.getOperand1());
      operator = operator.getSwitchOperandsSidesLogicalOperator();
    }
    return isVariable(variable, pCount)
        && constant instanceof CIntegerLiteralExpression literal
        && literal.getValue().signum() == 0
        && (operator == BinaryOperator.EQUALS || operator == BinaryOperator.LESS_EQUAL);
  }

  /** Edges that neither write memory nor variables nor call a function. */
  private static boolean isHarmless(CFAEdge pEdge) {
    return pEdge instanceof BlankEdge
        || pEdge instanceof CAssumeEdge
        || pEdge instanceof CReturnStatementEdge
        || (pEdge instanceof CDeclarationEdge declarationEdge
            && !(declarationEdge.getDeclaration() instanceof CVariableDeclaration variable
                && variable.isGlobal()))
        || (pEdge instanceof CStatementEdge statementEdge
            && statementEdge.getStatement() instanceof CExpressionStatement);
  }

  /** An assignment of an expression to a local variable of the function. */
  private static boolean isLocalAssignment(CFAEdge pEdge) {
    return pEdge instanceof CStatementEdge statementEdge
        && statementEdge.getStatement() instanceof CAssignment assignment
        && !(assignment instanceof CFunctionCall)
        && isLocalVariable(assignment.getLeftHandSide());
  }

  /** An assignment whose target is not a local variable of the function. */
  private static boolean isMemoryWrite(CFAEdge pEdge) {
    return pEdge instanceof CStatementEdge statementEdge
        && statementEdge.getStatement() instanceof CAssignment assignment
        && !isLocalVariable(assignment.getLeftHandSide());
  }

  /**
   * A call without result to a function that only checks a condition or stops the program, such as
   * {@code assume_abort_if_not(n == 0 || s)}.
   */
  private boolean isCheckingCall(CFAEdge pEdge) {
    if (!(pEdge instanceof CStatementEdge statementEdge)
        || !(statementEdge.getStatement() instanceof CFunctionCallStatement)) {
      return false;
    }
    Optional<String> callee = CFAUtils.getFunctionCallName(pEdge);
    if (callee.isEmpty()) {
      return false;
    }
    if (isTerminatingFunction(callee.orElseThrow())) {
      return true;
    }
    if (!cfa.getAllFunctionNames().contains(callee.orElseThrow())) {
      return false;
    }
    // A function with a body, which may only check conditions and call terminating functions.
    for (CFANode node : cfa.getFunctionNodes(callee.orElseThrow())) {
      for (CFAEdge edge : node.getLeavingEdges()) {
        boolean allowed =
            isHarmless(edge)
                || isLocalAssignment(edge)
                || (edge instanceof CStatementEdge callEdge
                    && callEdge.getStatement() instanceof CFunctionCallStatement
                    && CFAUtils.getFunctionCallName(callEdge)
                        .filter(KnownFunctionBehaviorDetector::isTerminatingFunction)
                        .isPresent());
        if (!allowed) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean isTerminatingFunction(String pFunction) {
    return TERMINATING_FUNCTIONS.contains(pFunction);
  }

  private static boolean isLocalVariable(CLeftHandSide pLeftHandSide) {
    return pLeftHandSide instanceof CIdExpression identifier
        && (identifier.getDeclaration() instanceof CParameterDeclaration
            || (identifier.getDeclaration() instanceof CVariableDeclaration variable
                && isPlainLocal(variable)));
  }

  private static boolean isPlainLocal(CVariableDeclaration pVariable) {
    return !pVariable.isGlobal() && pVariable.getCStorageClass() == CStorageClass.AUTO;
  }

  private static boolean writesParameter(
      CFAEdge pEdge, ImmutableList<CParameterDeclaration> pParameters) {
    for (CParameterDeclaration parameter : pParameters) {
      if (writesVariable(pEdge, parameter)) {
        return true;
      }
    }
    return false;
  }

  /** Whether the given edge declares or assigns the given variable. */
  private static boolean writesVariable(CFAEdge pEdge, CSimpleDeclaration pVariable) {
    if (pEdge instanceof CDeclarationEdge declarationEdge) {
      return pVariable.equals(declarationEdge.getDeclaration());
    }
    return pEdge instanceof CStatementEdge statementEdge
        && statementEdge.getStatement() instanceof CAssignment assignment
        && isVariable(assignment.getLeftHandSide(), pVariable);
  }

  /**
   * Whether the given edge gives the given variable a value, i.e., assigns it or declares it with
   * an initializer. A declaration without initializer does not count.
   */
  private static boolean assignsValue(CFAEdge pEdge, CSimpleDeclaration pVariable) {
    return writesVariable(pEdge, pVariable) && valueWrittenBy(pEdge) != null;
  }

  /** The value that the given edge writes into a variable, if it has one. */
  private static @Nullable CRightHandSide valueWrittenBy(CFAEdge pEdge) {
    if (pEdge instanceof CDeclarationEdge declarationEdge
        && declarationEdge.getDeclaration() instanceof CVariableDeclaration declaration
        && declaration.getInitializer() instanceof CInitializerExpression initializer) {
      return initializer.getExpression();
    }
    if (pEdge instanceof CStatementEdge statementEdge
        && statementEdge.getStatement() instanceof CAssignment assignment) {
      return assignment.getRightHandSide();
    }
    return null;
  }

  private static boolean returnsNothingOrParameter(
      CReturnStatementEdge pEdge, CParameterDeclaration pPointer) {
    return pEdge.getExpression().isEmpty()
        || isVariable(withoutCasts(pEdge.getExpression().orElseThrow()), pPointer);
  }

  /** Whether the address of the given variable is taken anywhere in the given nodes. */
  private static boolean addressIsTaken(Collection<CFANode> pNodes, CSimpleDeclaration pVariable) {
    for (CFANode node : pNodes) {
      for (CFAEdge edge : node.getLeavingEdges()) {
        for (AAstNode astNode : CFAUtils.getAstNodesFromCfaEdge(edge)) {
          for (AAstNode subNode : CFAUtils.traverseRecursively(astNode)) {
            if (subNode instanceof CUnaryExpression unaryExpression
                && unaryExpression.getOperator() == UnaryOperator.AMPER
                && isVariable(withoutCasts(unaryExpression.getOperand()), pVariable)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private static boolean isVariable(CExpression pExpression, CSimpleDeclaration pVariable) {
    return pExpression instanceof CIdExpression identifier
        && pVariable.equals(identifier.getDeclaration());
  }

  private static CExpression withoutCasts(CExpression pExpression) {
    CExpression expression = pExpression;
    while (expression instanceof CCastExpression cast) {
      expression = cast.getOperand();
    }
    return expression;
  }
}
