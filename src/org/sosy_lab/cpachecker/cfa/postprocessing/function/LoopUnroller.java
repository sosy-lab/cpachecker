// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.CFAReversePostorder;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class LoopUnroller {

  private final LogManager logger;

  // An unrolling creates one copy of the loop body per iteration, so a count that does not fit
  // into an int could never be built anyway.
  int maxNumberOfUnrollings = 20;

  // Nested loops multiply: the copies of a loop that we unrolled contain their own copy of every
  // loop inside it, and each of those can be unrolled again. Three nested loops of the above
  // iterations would already be 8000 copies of the innermost body, so the growth needs a limit of
  // its own.
  int maxNodesPerFunction = 100_000;

  public LoopUnroller(LogManager pLogger) {
    logger = pLogger;
  }

  public void unrollBoundedLoops(MutableCFA cfa) {
    // Unrolling a loop replaces the nodes of every loop around it, which makes the loop structure
    // we used stale. Computing it again lets the loops that contain the one we unrolled be
    // unrolled as well, so that a nest of loops comes apart from the inside out, one level per
    // round.
    while (unrollInnermostBoundedLoops(cfa)) {
      // A loop we unrolled is gone, so the one around it is one level less deep and this ends.
    }
  }

  /**
   * Unrolls every loop that contains no other loop we unroll in the same round. A loop around one
   * of them only exists as a copy afterwards, which the next round finds.
   *
   * <p>Going inwards out means that a loop is only ever copied once it is as small as we can make
   * it: unrolling the innermost loop first can even make it vanish, and the loops around it then
   * copy what is left instead of copying the loop and unrolling every copy of it.
   *
   * @return whether anything was unrolled, so that another round is worth it
   */
  private boolean unrollInnermostBoundedLoops(MutableCFA cfa) {
    LoopStructure loopStructure;
    // Loop detection needs reverse-postorder ids, which CFACreator assigns only after all
    // post-processings that modify the CFA (and again after this one).
    cfa.entryNodes().forEach(CFAReversePostorder::assignIds);
    try {
      loopStructure = LoopStructure.getLoopStructure(cfa);
    } catch (ParserException pE) {
      logger.log(Level.WARNING, "Can not parse loop structure, no unrolling done");
      return false;
    }

    boolean unrolledSomething = false;
    // A loop that contains another one also contains all of its nodes and at least its own head,
    // so this order reaches every loop before the ones that contain it. Those are stale by then
    // and canUnroll skips them until the next round sees their copies.
    for (Loop loop : innermostFirst(loopStructure.getAllLoops())) {
      // Counting the iterations of a loop we could not unroll anyway would be wasted work, and the
      // more iterations we allow the more expensive it gets.
      if (!canUnroll(cfa, loop)) {
        continue;
      }
      OptionalInt loopIterations = findExactLoopIterationCount(cfa, loop);
      if (loopIterations.isEmpty()
          || loopIterations.getAsInt() > maxNumberOfUnrollings
          || !fitsIntoTheNodeBudget(cfa, loop, loopIterations.getAsInt())) {
        continue;
      }
      unrollLoopExactly(cfa, loop, loopIterations.getAsInt());
      unrolledSomething = true;
    }
    return unrolledSomething;
  }

  /** The given loops, every one of them before the ones that contain it. */
  private static ImmutableList<Loop> innermostFirst(Collection<Loop> pLoops) {
    return ImmutableList.sortedCopyOf(
        Comparator.comparingInt(loop -> loop.getLoopNodes().size()), pLoops);
  }

  /**
   * Whether unrolling the given loop keeps its function within {@link #maxNodesPerFunction}. Logs
   * the reason if it does not.
   */
  private boolean fitsIntoTheNodeBudget(MutableCFA pCfa, Loop pLoop, int pIterations) {
    String function = pLoop.getLoopNodes().first().getFunctionName();
    long nodesAfterwards =
        (long) pCfa.getFunctionNodes(function).size()
            + (long) pIterations * pLoop.getLoopNodes().size();
    if (nodesAfterwards > maxNodesPerFunction) {
      return logGiveUpUnrolling(
          pLoop, "unrolling it would grow " + function + " to " + nodesAfterwards + " nodes");
    }
    return true;
  }

  /**
   * Unrolls the loop as often as given, replacing it by a chain of copies of its body in which this
   * loop does not occur anymore.
   *
   * <p>Unrolls under the assumption that the loop is left after the n-th visit of its entry node
   * and before the n+1-th one, and will be unsound if that is not exactly the case. All copies but
   * the last one therefore assume that the loop goes on in place of their exit condition, while the
   * last assumes it will exit the loop.
   *
   * <p>Loops nested inside the unrolled loop are copied along and stay loops.
   *
   * <p>Can only unroll loops under certain conditions given by {@link
   * LoopUnroller#canUnroll(MutableCFA, Loop)}
   *
   * @param pCfa the cfa to do the unrolling in
   * @param pLoop the loop to unroll
   * @param pIterations the number n of visits of the entry node, which is at least 1 because
   *     reaching the loop already visits that node
   */
  @VisibleForTesting
  void unrollLoopExactly(MutableCFA pCfa, Loop pLoop, int pIterations) {

    // TODO we could allow overapproximations of loop iterations and in this case keep the
    // assertions / redirect them to the end

    // We modify the CFA step by step and cannot undo that, so everything we need has to be checked
    // before we start. Callers that go on to do something else with a loop we reject check this
    // themselves, but the guard stays here so that calling this method is never destructive.
    if (!canUnroll(pCfa, pLoop)) {
      return;
    }
    checkArgument(
        pIterations >= 1,
        "cannot leave a loop before the first visit of its entry node, but got %s",
        pIterations);

    CFANode entryNode = getLoopEntry(pLoop);
    // TODO extending to multiple exits is not too complicated, but only useful if the heuristic can
    // also handle it
    CFAEdge exitEdge = getOnlyElement(pLoop.getOutgoingEdges());

    List<Map<CFANode, CFANode>> iterationNodes =
        createNodeCopies(pCfa, pLoop, entryNode, exitEdge.getPredecessor(), pIterations);
    createEdgeCopies(pLoop, entryNode, exitEdge, iterationNodes);
    redirectIncomingEdges(pLoop, iterationNodes.getFirst().get(entryNode));
    removeOriginalLoop(pCfa, pLoop, exitEdge);

    logger.logf(
        Level.FINE,
        "Unrolled loop at %s exactly %d times",
        entryNode.getFunctionName(),
        pIterations);
  }

  /**
   * Creates one copy of the nodes of the loop per iteration, in the order of the iterations. The
   * last iteration only gets the nodes before the exit, because it leaves the loop there instead of
   * running the rest of the body.
   *
   * @return for every iteration a map from each node of the loop to its copy
   */
  private static List<Map<CFANode, CFANode>> createNodeCopies(
      MutableCFA pCfa, Loop pLoop, CFANode pEntryNode, CFANode pConditionNode, int pIterations) {
    Set<CFANode> nodesOfLastIteration = nodesThatCanLeaveTheLoop(pLoop, pEntryNode, pConditionNode);

    List<Map<CFANode, CFANode>> iterationNodes = new ArrayList<>(pIterations);
    for (int iteration = 0; iteration < pIterations; iteration++) {
      boolean lastIteration = iteration + 1 == pIterations;
      Map<CFANode, CFANode> nodeCopies = new LinkedHashMap<>();
      for (CFANode node : pLoop.getLoopNodes()) {
        if (lastIteration && !nodesOfLastIteration.contains(node)) {
          continue;
        }
        CFANode copy = copyNode(node);
        if (node.isLoopStart()
            && !node.equals(pEntryNode)
            && !pLoop.getLoopHeads().contains(node)) {
          copy.setLoopStart(); // keep the flag for nested loops
        }
        pCfa.addNode(copy);
        nodeCopies.put(node, copy);
      }
      iterationNodes.add(nodeCopies);
    }
    return iterationNodes;
  }

  /**
   * Creates the edges between the node copies. Whatever started a new iteration in the original
   * loop now enters the copy of the next iteration, so that the copies form a chain. The last one
   * has no next copy and leaves the loop at the condition instead.
   */
  private void createEdgeCopies(
      Loop pLoop,
      CFANode pEntryNode,
      CFAEdge pExitEdge,
      List<Map<CFANode, CFANode>> pIterationNodes) {
    ImmutableSet<CFAEdge> innerEdges = pLoop.getInnerLoopEdges();
    ImmutableSet<CVariableDeclaration> declaredVariables = variablesDeclaredIn(pLoop);
    int iterations = pIterationNodes.size();

    for (int iteration = 0; iteration < iterations; iteration++) {
      boolean lastIteration = iteration + 1 == iterations;
      Map<CFANode, CFANode> nodeCopies = pIterationNodes.get(iteration);
      ImmutableMap<CSimpleDeclaration, CVariableDeclaration> renamedVariables =
          renameDeclarations(declaredVariables, pEntryNode.getFunctionName(), iteration);

      // Collect the edges of this copy before adding them. If one branch of an assumption is no
      // longer needed, we later replace the other with a blank edge
      Map<CFAEdge, CFANode> successors = new LinkedHashMap<>();
      for (CFAEdge edge : innerEdges) {
        if (!nodeCopies.containsKey(edge.getPredecessor())) {
          // the predecessor was left out above because it is after the exit in the last iteration
          continue;
        }
        CFANode successor;
        if (edge.getSuccessor().equals(pEntryNode)) {
          // point to the next iteration copy
          if (lastIteration) {
            continue;
          }
          successor = pIterationNodes.get(iteration + 1).get(pEntryNode);
        } else {
          successor = nodeCopies.get(edge.getSuccessor());
          if (successor == null) {
            // the successor was left out above because it is after the exit in the last iteration
            continue;
          }
        }
        successors.put(edge, successor);
      }
      if (lastIteration) {
        successors.put(pExitEdge, pExitEdge.getSuccessor());
      }

      Multiset<CFANode> branchesOfNode = HashMultiset.create();
      successors.keySet().forEach(edge -> branchesOfNode.add(edge.getPredecessor()));
      successors.forEach(
          (edge, successor) ->
              CFACreationUtils.addEdgeUnconditionallyToCFA(
                  copyEdge(
                      edge,
                      nodeCopies.get(edge.getPredecessor()),
                      successor,
                      branchesOfNode.count(edge.getPredecessor()) < 2,
                      renamedVariables)));
    }
  }

  /** Redirects everything that entered the loop to the copy of the first iteration. */
  private void redirectIncomingEdges(Loop pLoop, CFANode pFirstIterationEntry) {
    for (CFAEdge enteringEdge : pLoop.getIncomingEdges()) {
      CFACreationUtils.removeEdgeFromNodes(enteringEdge);
      // An edge that enters the loop is outside of it and cannot read what it declares.
      CFACreationUtils.addEdgeUnconditionallyToCFA(
          copyEdge(
              enteringEdge,
              enteringEdge.getPredecessor(),
              pFirstIterationEntry,
              false,
              ImmutableMap.of()));
    }
  }

  /** The variables that the given loop declares, each of which needs one copy per iteration. */
  private static ImmutableSet<CVariableDeclaration> variablesDeclaredIn(Loop pLoop) {
    ImmutableSet.Builder<CVariableDeclaration> declarations = ImmutableSet.builder();
    for (CFAEdge edge : pLoop.getInnerLoopEdges()) {
      if (edge instanceof CDeclarationEdge declarationEdge
          && declarationEdge.getDeclaration() instanceof CVariableDeclaration declaration) {
        declarations.add(declaration);
      }
    }
    return declarations.build();
  }

  /**
   * Creates the copies of the variables that the loop declares for the copy of one iteration.
   *
   * <p>A use that its copy reaches without passing the declaration keeps referring to the copy of
   * that iteration although nothing declares it there. That is sound because using such a variable
   * is undefined behavior.
   *
   * @param pFunction the function that the loop is in, which is the scope of every variable it
   *     declares because {@link #canUnroll} only allows local ones
   * @return a map from each variable of the loop to the copy that this iteration uses
   */
  private static ImmutableMap<CSimpleDeclaration, CVariableDeclaration> renameDeclarations(
      Set<CVariableDeclaration> pDeclarations, String pFunction, int pIteration) {

    // Create all of them without their initializers first, because the initializer of one variable
    // can read another variable of the same iteration.
    Map<CSimpleDeclaration, CVariableDeclaration> renamed = new LinkedHashMap<>();
    for (CVariableDeclaration declaration : pDeclarations) {
      String name = renamedVariable(declaration.getName(), pIteration);
      renamed.put(
          declaration,
          new CVariableDeclaration(
              declaration.getFileLocation(),
              declaration.isGlobal(),
              declaration.getCStorageClass(),
              declaration.getType(),
              name,
              // Keep the name that the program uses, so that exports can still point at it.
              declaration.getOrigName(),
              qualifiedNameOfLocal(pFunction, name),
              null));
    }

    ImmutableMap<CSimpleDeclaration, CVariableDeclaration> renamedVariables =
        ImmutableMap.copyOf(renamed);
    for (CVariableDeclaration declaration : pDeclarations) {
      CInitializer initializer = declaration.getInitializer();
      if (initializer != null) {
        renamedVariables
            .get(declaration)
            .addInitializer((CInitializer) substitute(initializer, renamedVariables));
      }
    }
    return renamedVariables;
  }

  /**
   * The name that a variable of the loop has in the copy of the given iteration. It starts with two
   * underscores because C reserves those for the implementation.
   */
  private static String renamedVariable(String pName, int pIteration) {
    return "__" + pName + "_" + pIteration;
  }

  /**
   * The qualified name of a local variable of the given function, built the same way as {@link
   * org.sosy_lab.cpachecker.cfa.parser.eclipse.c.FunctionScope#createQualifiedName(String, String)}
   * does it.
   *
   * <p>That method is not called directly because nothing outside of the parser uses it, and the
   * format of a qualified name is not defined anywhere else either. The rest of CPAchecker still
   * relies on it, for example {@link CFAUtils#filterVariablesOfFunction} looks up the variables of
   * a function by this prefix, so building the name the same way is what keeps the copies visible
   * to it.
   */
  private static String qualifiedNameOfLocal(String pFunction, String pName) {
    return pFunction + "::" + pName;
  }

  /**
   * Rebuilds the given node so that every read of a variable of the loop refers to the copy that
   * the iteration of the given renaming uses.
   */
  private static CAstNode substitute(
      CAstNode pNode, ImmutableMap<CSimpleDeclaration, CVariableDeclaration> pRenamedVariables) {
    if (pRenamedVariables.isEmpty()) {
      return pNode;
    }
    return pNode.accept(
        new SubstitutingCAstNodeVisitor(
            node -> {
              if (node instanceof CIdExpression identifier) {
                CVariableDeclaration renamed = pRenamedVariables.get(identifier.getDeclaration());
                if (renamed != null) {
                  return new CIdExpression(identifier.getFileLocation(), renamed);
                }
              }
              return null;
            }));
  }

  /** Removes the loop that the copies replace, which is now unreachable. */
  private static void removeOriginalLoop(MutableCFA pCfa, Loop pLoop, CFAEdge pExitEdge) {
    CFACreationUtils.removeEdgeFromNodes(pExitEdge);
    for (CFAEdge edge : pLoop.getInnerLoopEdges()) {
      CFACreationUtils.removeEdgeFromNodes(edge);
    }
    for (CFANode node : pLoop.getLoopNodes()) {
      pCfa.removeNode(node);
    }
  }

  /**
   * Whether {@link #unrollLoopExactly} can handle the given loop. Logs the reason if it cannot.
   *
   * @param pCfa the cfa that contains the loop
   * @param pLoop the loop to unroll
   */
  private boolean canUnroll(MutableCFA pCfa, Loop pLoop) {

    // Unrolling a loop replaces all of its nodes, so a loop that is nested inside a loop we already
    // unrolled does not exist anymore (and its copies are not loops in the first place).
    CFANode someLoopNode = pLoop.getLoopNodes().first();
    if (!pCfa.getFunctionNodes(someLoopNode.getFunctionName()).containsAll(pLoop.getLoopNodes())) {
      return logGiveUpUnrolling(pLoop, "skipped nested loop");
    }

    // The way we define an iteration we need a unique entry into the loop
    ImmutableSet<CFANode> entryNodes =
        transformedImmutableSetCopy(pLoop.getIncomingEdges(), CFAEdge::getSuccessor);
    if (entryNodes.size() != 1) {
      return logGiveUpUnrolling(pLoop, "it is entered at more than one node");
    }
    CFANode entryNode = getOnlyElement(entryNodes);

    if (pLoop.getOutgoingEdges().size() != 1) {
      return logGiveUpUnrolling(pLoop, "it has more than one outgoing edge");
    }
    // The single way out of the loop needs to be one branch of a condition whose other branch
    // stays inside the loop. Only then dropping the condition leaves a connected CFA behind.
    CFAEdge exitEdge = getOnlyElement(pLoop.getOutgoingEdges());
    CFANode conditionNode = exitEdge.getPredecessor();
    if (!(exitEdge instanceof AssumeEdge) || conditionNode.getNumLeavingEdges() != 2) {
      return logGiveUpUnrolling(pLoop, "the loop is not left via one branch of a condition");
    }
    CFAEdge stayInLoopEdge =
        conditionNode.getLeavingEdge(0).equals(exitEdge)
            ? conditionNode.getLeavingEdge(1)
            : conditionNode.getLeavingEdge(0);
    if (!pLoop.getLoopNodes().contains(stayInLoopEdge.getSuccessor())) {
      return logGiveUpUnrolling(pLoop, "the loop is not left via one branch of a condition");
    }

    // The last iteration keeps only the part of the body from which the loop can still be left, so
    // it has to be possible to leave the loop right after entering an iteration.
    if (!nodesThatCanLeaveTheLoop(pLoop, entryNode, conditionNode).contains(entryNode)) {
      return logGiveUpUnrolling(pLoop, "its last iteration could not leave it");
    }

    // Every copy of the body gets its own copies of the variables that the loop declares, cf.
    // renameDeclarations. That only works for a plain local variable whose type we can reuse as it
    // is: the length of a variable length array is an expression that reads another variable, and
    // renaming does not reach into types.
    for (CFAEdge edge : pLoop.getInnerLoopEdges()) {
      if (edge instanceof ADeclarationEdge declarationEdge) {
        if (!(declarationEdge.getDeclaration() instanceof CVariableDeclaration declaration)
            || declaration.isGlobal()
            || declaration.getCStorageClass() != CStorageClass.AUTO) {
          return logGiveUpUnrolling(pLoop, "it contains a declaration we cannot copy: " + edge);
        }
        if (!declaration.getType().hasKnownConstantSize()) {
          return logGiveUpUnrolling(
              pLoop, "it declares " + declaration.getQualifiedName() + " without a constant size");
        }
      }
    }

    // We must not modify the CFA and give up afterwards, so check that we can copy everything.
    for (CFANode node : pLoop.getLoopNodes()) {
      if (!isCopyableNode(node)) {
        return logGiveUpUnrolling(pLoop, "it contains a node of an unsupported kind: " + node);
      }
    }
    for (CFAEdge edge : Iterables.concat(pLoop.getInnerLoopEdges(), pLoop.getIncomingEdges())) {
      if (!isCopyableEdge(edge)) {
        return logGiveUpUnrolling(pLoop, "it contains an edge of an unsupported kind: " + edge);
      }
    }

    return true;
  }

  /**
   * The node at which the given loop is entered, which is where we consider each of its iterations
   * to begin. Only defined if {@link #canUnroll} holds for the loop.
   */
  private static CFANode getLoopEntry(Loop pLoop) {
    return getOnlyElement(pLoop.getIncomingEdges()).getSuccessor();
  }

  /**
   * Collects the nodes of the given loop from which the condition can still be reached within the
   * same iteration, i.e. without starting a new one at {@code pEntry}. These are exactly the nodes
   * that the last iteration needs, because it is the one that leaves the loop. From all other nodes
   * the loop could only continue, which the last iteration by definition does not do.
   */
  private static Set<CFANode> nodesThatCanLeaveTheLoop(
      Loop pLoop, CFANode pEntry, CFANode pCondition) {
    ImmutableSet<CFAEdge> innerEdges = pLoop.getInnerLoopEdges();
    Set<CFANode> reached = new HashSet<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    reached.add(pCondition);
    waitlist.push(pCondition);
    while (!waitlist.isEmpty()) {
      CFANode node = waitlist.pop();
      if (node.equals(pEntry)) {
        continue; // going further back would mean going through one more iteration
      }
      for (CFAEdge edge : node.getEnteringEdges()) {
        CFANode predecessor = edge.getPredecessor();
        if (innerEdges.contains(edge) && reached.add(predecessor)) {
          waitlist.push(predecessor);
        }
      }
    }
    return reached;
  }

  /**
   * @return always {@code false}, so that {@link #canUnroll} can return the result directly
   */
  @CanIgnoreReturnValue
  private boolean logGiveUpUnrolling(Loop pLoop, String pReason) {
    logger.logf(
        Level.FINE,
        "Not unrolling loop with head %s because %s",
        pLoop.getLoopHeads().iterator().next(),
        pReason);
    return false;
  }

  /**
   * Creates a copy of the given edge between the given nodes. The AST of the edge is shared with
   * the original edge except for the variables that the loop declares, which every iteration
   * creates its own copy.
   *
   * @param pNoLongerBranches whether the predecessor has no other leaving edge in this copy, in
   *     which case an {@link AssumeEdge} is converted to a {@link BlankEdge}
   * @param pRenamedVariables A mapping of the variables that are declared inside the loop to the
   *     iteration specific copies
   */
  private CFAEdge copyEdge(
      CFAEdge pEdge,
      CFANode pPredecessor,
      CFANode pSuccessor,
      boolean pNoLongerBranches,
      ImmutableMap<CSimpleDeclaration, CVariableDeclaration> pRenamedVariables) {
    String rawStatement = pEdge.getRawStatement();
    FileLocation fileLocation = pEdge.getFileLocation();

    return switch (pEdge) {
      case AssumeEdge ignored when pNoLongerBranches ->
          new BlankEdge(
              rawStatement,
              fileLocation,
              pPredecessor,
              pSuccessor,
              "unrolled: " + pEdge.getDescription());
      case CAssumeEdge edge ->
          new CAssumeEdge(
              rawStatement,
              fileLocation,
              pPredecessor,
              pSuccessor,
              (CExpression) substitute(edge.getExpression(), pRenamedVariables),
              edge.getTruthAssumption(),
              edge.isSwapped(),
              edge.isArtificialIntermediate());
      case BlankEdge ignored ->
          new BlankEdge(
              rawStatement, fileLocation, pPredecessor, pSuccessor, pEdge.getDescription());
      case CDeclarationEdge edge -> {
        CVariableDeclaration renamed = pRenamedVariables.get(edge.getDeclaration());
        yield new CDeclarationEdge(
            rawStatement,
            fileLocation,
            pPredecessor,
            pSuccessor,
            renamed != null ? renamed : edge.getDeclaration());
      }
      case CStatementEdge edge ->
          new CStatementEdge(
              rawStatement,
              (CStatement) substitute(edge.getStatement(), pRenamedVariables),
              fileLocation,
              pPredecessor,
              pSuccessor);
      default -> throw new AssertionError("unsupported edge class for unrolling: " + pEdge);
    };
  }

  /**
   * Whether {@link #copyEdge} can handle this edge, needs to be checked before modifying the CFA.
   */
  private static boolean isCopyableEdge(CFAEdge pEdge) {
    if (pEdge instanceof CFunctionSummaryStatementEdge) {
      // unrolling has to happen before the supergraph is built
      return false;
    }
    return pEdge instanceof BlankEdge
        || pEdge instanceof CAssumeEdge
        || pEdge instanceof CDeclarationEdge
        || pEdge instanceof CStatementEdge;
  }

  /**
   * Creates a copy of the given node. A label inside the loop ends up on all copies, so the
   * resulting CFA has several nodes with the same label. TODO export to C will fail
   */
  private static CFANode copyNode(CFANode pNode) {
    if (pNode instanceof CFALabelNode labelNode) {
      return new CFALabelNode(pNode.getFunction(), labelNode.getLabel());
    }
    return new CFANode(pNode.getFunction());
  }

  /**
   * Whether {@link #copyNode} can handle this node, needs to be checked before modifying the CFA.
   */
  private static boolean isCopyableNode(CFANode pNode) {
    return pNode.getClass() == CFANode.class || pNode instanceof CFALabelNode;
  }

  /**
   * Finds out with a simple heuristic the number of iterations that a loop is run.
   *
   * <p>The heuristic only recognizes loops that are counted by a single local integer variable: it
   * has to start at a constant, the loop has to be left by comparing it to a constant, and every
   * iteration has to change it by the same constant offset. Everything else, in particular a
   * counter that could overflow or that something else could write to, gives up.
   *
   * @param pCfa the cfa that contains the loop
   * @param pLoop the loop to analyze
   * @return The exact number of visits of the entry node after which the loop is left, as used by
   *     {@link #unrollLoopExactly}, or empty if it could not be determined or is not the same on
   *     every path that reaches the loop
   */
  @VisibleForTesting
  OptionalInt findExactLoopIterationCount(MutableCFA pCfa, Loop pLoop) {
    ImmutableSet<CFANode> entryNodes =
        transformedImmutableSetCopy(pLoop.getIncomingEdges(), CFAEdge::getSuccessor);
    if (entryNodes.size() != 1 || pLoop.getOutgoingEdges().size() != 1) {
      return logNoIterationCount(pLoop, "it has more than one entry node or more than one exit");
    }
    CFANode entryNode = getOnlyElement(entryNodes);
    CFAEdge exitEdge = getOnlyElement(pLoop.getOutgoingEdges());
    CFANode conditionNode = exitEdge.getPredecessor();
    if (conditionNode.getNumLeavingEdges() != 2) {
      return logNoIterationCount(pLoop, "it is not left via one branch of a condition");
    }
    // The loop goes on for exactly as long as the branch that stays inside it can be taken.
    CFAEdge stayInLoopEdge =
        conditionNode.getLeavingEdge(0).equals(exitEdge)
            ? conditionNode.getLeavingEdge(1)
            : conditionNode.getLeavingEdge(0);
    Optional<LoopCondition> parsedCondition = parseLoopCondition(stayInLoopEdge);
    if (parsedCondition.isEmpty()) {
      return logNoIterationCount(
          pLoop, "its condition does not compare a variable to a constant: " + stayInLoopEdge);
    }
    LoopCondition condition = parsedCondition.orElseThrow();
    CVariableDeclaration counter = condition.counter();
    String counterName = counter.getQualifiedName();

    // Everything below reasons about the value of the counter, which only works if nothing outside
    // the paths we look at can change it.
    Optional<CSimpleType> counterType = typeOfUsableCounter(counter);
    if (counterType.isEmpty()) {
      return logNoIterationCount(pLoop, counterName + " is not a plain local integer variable");
    }
    if (addressIsTaken(pCfa, counter, entryNode.getFunctionName())) {
      return logNoIterationCount(pLoop, "the address of " + counterName + " is taken");
    }

    // TODO when doing the simulation, we could allow multiple and more complex modifications, as
    // long as they do not depend on other variables

    // The counter has to be changed by the same offset in every iteration, so there has to be
    // exactly one edge that writes it, and it has to add a constant.
    CFAEdge modification = null;
    for (CFAEdge edge : pLoop.getInnerLoopEdges()) {
      if (writesVariable(edge, counter)) {
        if (modification != null) {
          return logNoIterationCount(pLoop, counterName + " is written by more than one edge");
        }
        modification = edge;
      }
    }
    if (modification == null) {
      return logNoIterationCount(pLoop, counterName + " is never written inside the loop");
    }
    Optional<BigInteger> offset = offsetAddedBy(modification, counter);
    if (offset.isEmpty()) {
      return logNoIterationCount(
          pLoop, counterName + " is not changed by a constant offset: " + modification);
    }

    // That single edge also has to be passed exactly once per iteration, and we need to know
    // whether it comes before or after the check of the condition.
    Optional<CounterModification> counterModification =
        modificationRelativeToCondition(pLoop, entryNode, conditionNode, modification);
    if (counterModification.isEmpty()) {
      return logNoIterationCount(
          pLoop, counterName + " is not written exactly once on every path through the loop");
    }

    Optional<BigInteger> startValue = valueBeforeLoop(pLoop, counter);
    if (startValue.isEmpty()) {
      return logNoIterationCount(
          pLoop, counterName + " does not have a known constant value when the loop is entered");
    }

    return countVisitsOfEntryNode(
        pLoop,
        condition,
        counterType.orElseThrow(),
        startValue.orElseThrow(),
        offset.orElseThrow(),
        counterModification.orElseThrow(),
        pCfa.getMachineModel());
  }

  /**
   * The condition under which a loop goes on
   *
   * @param counter the variable that the loop compares
   * @param continueOperator the comparison that has to hold for the loop to go on, with the counter
   *     as its left operand
   * @param bound the constant that the counter is compared to
   */
  private record LoopCondition(
      CVariableDeclaration counter, BinaryOperator continueOperator, BigInteger bound) {}

  /**
   * Reads the condition under which the given edge continues a loop as a comparison of a variable
   * to a constant, normalized so that the variable is the left operand. Empty for every other
   * shape, including comparisons of two variables.
   */
  private static Optional<LoopCondition> parseLoopCondition(CFAEdge pStayInLoopEdge) {
    // Only the first of these checks is an invariant of the CFA: a node that branches has two
    // leaving assume edges with opposite assumptions, which {@link CFACheck} verifies. The shape of
    // the expression is not checked anywhere, it only happens to always be a comparison because the
    // parser rewrites a condition that is not one into a comparison to zero, so that for example
    // "while (x)" arrives here as "[!(x == 0)]".
    if (!(pStayInLoopEdge instanceof CAssumeEdge assumeEdge)
        || !(assumeEdge.getExpression() instanceof CBinaryExpression comparison)
        || !comparison.getOperator().isLogicalOperator()) {
      return Optional.empty();
    }
    BinaryOperator operator = comparison.getOperator();
    if (!assumeEdge.getTruthAssumption()) {
      operator = operator.getOppositLogicalOperator();
    }
    CExpression counter = comparison.getOperand1();
    CExpression bound = comparison.getOperand2();
    if (counter instanceof CIntegerLiteralExpression && bound instanceof CIdExpression) {
      counter = comparison.getOperand2();
      bound = comparison.getOperand1();
      operator = operator.getSwitchOperandsSidesLogicalOperator();
    }
    if (counter instanceof CIdExpression identifier
        && identifier.getDeclaration() instanceof CVariableDeclaration declaration
        && bound instanceof CIntegerLiteralExpression literal) {
      return Optional.of(new LoopCondition(declaration, operator, literal.getValue()));
    }
    return Optional.empty();
  }

  /**
   * The type of the given variable, if a loop counter of this type can be tracked precisely. It has
   * to be an integer that lives only in its function and that is not read or written by anything
   * else, so a global, static or volatile variable is not usable.
   */
  private static Optional<CSimpleType> typeOfUsableCounter(CVariableDeclaration pCounter) {
    if (pCounter.isGlobal()
        || pCounter.getCStorageClass() != CStorageClass.AUTO
        || !(pCounter.getType().getCanonicalType() instanceof CSimpleType type)
        || type.isVolatile()
        || !type.getType().isIntegerType()) {
      return Optional.empty();
    }
    return Optional.of(type);
  }

  /**
   * Whether the address of the given variable is taken anywhere in the given function, in which
   * case a write through a pointer could change it without us seeing an edge that writes it.
   *
   * <p>{@link
   * org.sosy_lab.cpachecker.util.variableclassification.VariableClassification#getAddressedVariables()}
   * answers exactly this, but it is not available here: {@link
   * org.sosy_lab.cpachecker.cfa.CFACreator} builds it after all post-processings, and classifies
   * every variable of the program while we care about a single one.
   */
  private static boolean addressIsTaken(
      MutableCFA pCfa, CVariableDeclaration pVariable, String pFunction) {
    for (CFANode node : pCfa.getFunctionNodes(pFunction)) {
      for (CFAEdge edge : node.getLeavingEdges()) {
        for (AAstNode astNode : CFAUtils.getAstNodesFromCfaEdge(edge)) {
          for (AAstNode subNode : CFAUtils.traverseRecursively(astNode)) {
            // Looking at the operand itself is enough, it does not have to contain the variable
            // somewhere. A counter is an integer, so it cannot be the object that a field or a
            // subscript expression starts from, and C offers no other way to write down its
            // address because a cast, a conditional and a comma expression are not lvalues.
            // Something like &array[counter] therefore only reads it.
            if (subNode instanceof CUnaryExpression unaryExpression
                && unaryExpression.getOperator() == UnaryOperator.AMPER
                && isVariable(unaryExpression.getOperand(), pVariable)) {
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
  private static boolean writesVariable(CFAEdge pEdge, CVariableDeclaration pVariable) {
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
   * before we see it (cf. {@code simplifyConstExpressions}, which is on by default). An increment
   * by {@code sizeof(int)}, by an enum constant or by {@code 1 + 1} therefore arrives here as a
   * literal and is handled.
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
   * Where a loop changes its counter, relative to the point at which it checks its condition.
   */
  private enum CounterModification {
    /** Changed before the check, as in a {@code do while} loop that ends with its condition. */
    BEFORE_CONDITION,
    /** Changed after the check, as in a {@code while} loop that begins with its condition. */
    AFTER_CONDITION;

    static CounterModification of(boolean pAlreadyPassed) {
      return pAlreadyPassed ? BEFORE_CONDITION : AFTER_CONDITION;
    }
  }

  /**
   * Where the loop changes its counter, by following its body from the entry node to the node at
   * which it checks its condition. Empty if that differs between the paths, or if an iteration does
   * not pass the modification exactly once, because then the counter does not have a value that
   * only depends on the number of iterations.
   *
   * <p>Only one edge modifies the counter, which {@link #findExactLoopIterationCount} makes sure
   * of, so whether we already passed that one edge is all we have to carry along.
   */
  private static Optional<CounterModification> modificationRelativeToCondition(
      Loop pLoop, CFANode pEntry, CFANode pCondition, CFAEdge pModification) {
    ImmutableSet<CFAEdge> innerEdges = pLoop.getInnerLoopEdges();
    Map<CFANode, Boolean> passedModification = new HashMap<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    passedModification.put(pEntry, false);
    waitlist.push(pEntry);
    while (!waitlist.isEmpty()) {
      CFANode node = waitlist.pop();
      boolean before = passedModification.get(node);
      for (CFAEdge edge : node.getLeavingEdges()) {
        if (!innerEdges.contains(edge)) {
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
        } else if (known.booleanValue() != after) {
          // Either a branch writes the counter and another does not, or a nested loop writes it.
          return Optional.empty();
        }
      }
    }
    // Absent if the condition is not even reachable without starting another iteration, in which
    // case the loop cannot be left after a fixed number of them.
    return Optional.ofNullable(passedModification.get(pCondition))
        .map(CounterModification::of);
  }

  /**
   * The value that the given variable has whenever the given loop is entered. Searches backwards
   * for the writes that can be the last one before the loop. Empty if one of them does not write a
   * constant, if they do not all write the same one, or if the variable can still hold whatever it
   * was declared with, which is unknown.
   */
  private static Optional<BigInteger> valueBeforeLoop(Loop pLoop, CVariableDeclaration pVariable) {
    Set<BigInteger> values = new HashSet<>();
    Set<CFAEdge> visited = new HashSet<>(pLoop.getIncomingEdges());
    Deque<CFAEdge> waitlist = new ArrayDeque<>(pLoop.getIncomingEdges());
    while (!waitlist.isEmpty()) {
      CFAEdge edge = waitlist.pop();
      if (writesVariable(edge, pVariable)) {
        Optional<BigInteger> value = constantWrittenBy(edge);
        if (value.isEmpty()) {
          return Optional.empty();
        }
        values.add(value.orElseThrow());
        continue; // nothing before this write can reach the loop without passing it
      }
      CFANode predecessor = edge.getPredecessor();
      if (predecessor.getNumEnteringEdges() == 0) {
        // We reached the start of the function, so the variable is never written before the loop.
        return Optional.empty();
      }
      for (CFAEdge enteringEdge : predecessor.getEnteringEdges()) {
        if (visited.add(enteringEdge)) {
          waitlist.push(enteringEdge);
        }
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
   * Simulates the counter to find the visit of the entry node at which the loop is left, which is
   * the first one whose check of the condition fails. Empty if that does not happen within {@link
   * #maxNumberOfUnrollings} visits or if the counter would leave the range of its type before, in
   * which case the values we computed are not the ones the program produces.
   *
   * <p>TODO we could switch to a closed form, but this is more difficult to get exactly right and
   * less extensible
   *
   * <p>Simulating costs one step per visit, so a loop whose condition never fails would cost the
   * whole budget. {@link #canReachTheBound} rules those out beforehand, which keeps the cost of
   * this method proportional to the number of iterations we are about to unroll instead of to the
   * budget we allow for them.
   */
  private OptionalInt countVisitsOfEntryNode(
      Loop pLoop,
      LoopCondition pCondition,
      CSimpleType pCounterType,
      BigInteger pStart,
      BigInteger pOffset,
      CounterModification pCounterModification,
      MachineModel pMachineModel) {
    BigInteger minimum = pMachineModel.getMinimalIntegerValue(pCounterType);
    BigInteger maximum = pMachineModel.getMaximalIntegerValue(pCounterType);
    BinaryOperator continueOperator = pCondition.continueOperator();
    BigInteger bound = pCondition.bound();
    // At the n-th visit of the entry node the counter is start + (n - 1) * offset, plus one more
    // offset if the loop already changed it before it checks its condition.
    BigInteger value =
        pCounterModification == CounterModification.BEFORE_CONDITION
            ? pStart.add(pOffset)
            : pStart;

    // A condition that already fails ends the loop right away, no matter where the counter goes.
    if (isWithin(value, minimum, maximum)
        && holds(continueOperator, value, bound)
        && !canReachTheBound(continueOperator, value, bound, pOffset)) {
      return logNoIterationCount(pLoop, "the counter never reaches the bound of its condition");
    }

    for (int visits = 1; visits <= maxNumberOfUnrollings; visits++) {
      if (!isWithin(value, minimum, maximum)) {
        return logNoIterationCount(pLoop, "the counter would overflow before the loop is left");
      }
      if (!holds(continueOperator, value, bound)) {
        return OptionalInt.of(visits);
      }
      value = value.add(pOffset);
    }
    return logNoIterationCount(
        pLoop, "it is not left within " + maxNumberOfUnrollings + " iterations");
  }

  /**
   * Whether adding the offset to the given value repeatedly can ever make the condition fail, which
   * is what ends the loop. Only called for a value for which the condition still holds.
   *
   * <p>An ordered comparison needs the counter to move towards its bound, and an inequality needs
   * it to land on the bound exactly instead of stepping over it. If neither is the case, only an
   * overflow could end the loop, and we do not unroll a loop that relies on one.
   */
  private static boolean canReachTheBound(
      BinaryOperator pContinueOperator, BigInteger pValue, BigInteger pBound, BigInteger pOffset) {
    BigInteger distance = pBound.subtract(pValue);
    return switch (pContinueOperator) {
      case LESS_THAN, LESS_EQUAL -> pOffset.signum() > 0;
      case GREATER_THAN, GREATER_EQUAL -> pOffset.signum() < 0;
      case NOT_EQUALS ->
          pOffset.signum() != 0
              && distance.signum() == pOffset.signum()
              && distance.remainder(pOffset).signum() == 0;
      // The counter leaves the bound with the next step and never comes back to it.
      case EQUALS -> pOffset.signum() != 0;
      default -> throw new AssertionError("not a comparison: " + pContinueOperator);
    };
  }

  /**
   * Whether the given value is in the range of the type of the counter.
   *
   * <p>There is nothing to reuse for this. An analysis that tracks values cannot run here at all,
   * because {@link org.sosy_lab.cpachecker.cfa.CFACreator} only builds the supergraph after the
   * post-processings. {@link MachineModel#getMinimalIntegerValue} and its counterpart are the
   * primitives that everyone builds such a check from, and the other place that needs it, {@link
   * org.sosy_lab.cpachecker.cpa.value.AssigningValueVisitor}, compares against them by hand as
   * well.
   */
  private static boolean isWithin(BigInteger pValue, BigInteger pMinimum, BigInteger pMaximum) {
    return pValue.compareTo(pMinimum) >= 0 && pValue.compareTo(pMaximum) <= 0;
  }

  /** Whether comparing the two given values with the given operator yields true. */
  private static boolean holds(BinaryOperator pOperator, BigInteger pLeft, BigInteger pRight) {
    int comparison = pLeft.compareTo(pRight);
    return switch (pOperator) {
      case LESS_THAN -> comparison < 0;
      case LESS_EQUAL -> comparison <= 0;
      case GREATER_THAN -> comparison > 0;
      case GREATER_EQUAL -> comparison >= 0;
      case EQUALS -> comparison == 0;
      case NOT_EQUALS -> comparison != 0;
      default -> throw new AssertionError("not a comparison: " + pOperator);
    };
  }

  /**
   * @return always empty, so that {@link #findExactLoopIterationCount} can return the result
   *     directly
   */
  private OptionalInt logNoIterationCount(Loop pLoop, String pReason) {
    logger.logf(
        Level.FINE,
        "Cannot determine the number of iterations of the loop with head %s because %s",
        pLoop.getLoopHeads().iterator().next(),
        pReason);
    return OptionalInt.empty();
  }
}
