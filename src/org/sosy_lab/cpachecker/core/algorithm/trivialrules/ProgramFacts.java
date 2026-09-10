// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;

/**
 * The facts about a program that the trivial rules are built on. Every fact is computed at most
 * once, and only if a rule asks for it.
 *
 * <p>All facts are properties of the CFA, so they hold for every execution of the program. Facts
 * that over-approximate the executions (such as {@link #reachableEdges()}) may be used by a rule
 * that proves a proposition, but not by a rule that refutes one; the only fact that describes
 * executions precisely is {@link #chain()}.
 */
final class ProgramFacts {

  /**
   * Functions without a body that are known not to allocate memory, not to create a thread, not to
   * access the memory of the program in a way it did not ask for, and not to call a function of the
   * program. A call of any other function without a body makes the rules abstain, because such a
   * function can do anything.
   */
  private static final ImmutableSet<String> KNOWN_HARMLESS_FUNCTIONS =
      ImmutableSet.<String>builder()
          .addAll(org.sosy_lab.cpachecker.util.StandardFunctions.C11_MATH_H_FUNCTIONS)
          .addAll(org.sosy_lab.cpachecker.util.StandardFunctions.C11_FENV_H_FUNCTIONS)
          .add("abort")
          .add("exit")
          .add("_Exit")
          .add("quick_exit")
          .add("__assert_fail")
          .add("__assert")
          .add("assert")
          .add("reach_error")
          .add("__VERIFIER_error")
          .add("__VERIFIER_assume")
          // Output functions do not touch memory that the program did not hand them, and SV-COMP
          // models the functions of the library abstractly anyway.
          .add("printf")
          .add("fprintf")
          .add("puts")
          .add("putchar")
          .add("fputc")
          .add("fputs")
          .add("perror")
          .build();

  /**
   * The prefix of the SV-COMP functions that return an unconstrained value without side effects.
   */
  private static final String NONDET_FUNCTION_PREFIX = "__VERIFIER_nondet";

  /**
   * Functions without a body that do not return, i.e., that end the execution. This is the same set
   * that {@code config/specification/sv-comp-terminatingfunctions.spc} tells the analyses about.
   */
  private static final ImmutableSet<String> FUNCTIONS_THAT_DO_NOT_RETURN =
      ImmutableSet.of("abort", "exit", "_Exit", "__assert_fail", "__VERIFIER_error", "reach_error");

  /** Why {@link #chain()} ends where it ends. */
  enum ChainEnd {
    /** The next location has more than one possible successor, so the execution is not forced. */
    BRANCHING,
    /** The chain reaches a location it has already visited, so the execution never ends. */
    REPEATED_LOCATION,
    /** The chain reaches a location without successors, i.e., the end of the program. */
    PROGRAM_END,
    /** The chain reaches a call of a function that does not return. */
    PROGRAM_EXIT,
    /** The chain reaches a call of a function whose behavior we do not know. */
    UNKNOWN_CALL,
  }

  /**
   * The sequence of edges that every execution of the program starts with, because every location
   * on it has exactly one possible successor.
   *
   * @param edges the edges in the order in which they are executed
   * @param end why the sequence ends where it ends
   */
  record Chain(ImmutableList<CFAEdge> edges, ChainEnd end) {}

  /**
   * The nodes and edges of the CFA that an execution of the program can reach.
   *
   * @param nodes the locations that an execution can reach
   * @param edges the edges that an execution can execute
   * @param usesFunctionAddress whether the program uses a function as a value, which means that a
   *     function without a body can call it although the CFA has no edge for that call
   */
  private record Reachable(
      ImmutableSet<CFANode> nodes, ImmutableSet<CFAEdge> edges, boolean usesFunctionAddress) {}

  private final CFA cfa;
  private final Specification specification;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final Supplier<ImmutableMap<String, BigInteger>> constants =
      Suppliers.memoize(this::computeConstants);
  private final Supplier<RangeEstimator> ranges =
      Suppliers.memoize(() -> new RangeEstimator(machineModel(), constants()));
  private final Supplier<Reachable> reachable = Suppliers.memoize(this::computeReachable);
  private final Supplier<ImmutableSet<String>> unknownFunctions =
      Suppliers.memoize(this::computeUnknownFunctions);
  private final Supplier<Chain> chain = Suppliers.memoize(this::computeChain);
  private final Supplier<Optional<ImmutableSet<CFANode>>> targetLocations =
      Suppliers.memoize(this::computeTargetLocations);
  private final Supplier<ImmutableSet<CFANode>> recursionNodes =
      Suppliers.memoize(this::computeRecursionNodes);

  ProgramFacts(
      CFA pCfa,
      Specification pSpecification,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    cfa = pCfa;
    specification = pSpecification;
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  CFA cfa() {
    return cfa;
  }

  LogManager logger() {
    return logger;
  }

  MachineModel machineModel() {
    return cfa.getMachineModel();
  }

  /**
   * The values of the variables that hold the same value in every execution: a variable with static
   * storage duration that is initialized with a constant, is never assigned, never has its address
   * taken, and is not volatile. Local variables without static storage duration are not included,
   * because a {@code goto} can jump over their initialization.
   */
  ImmutableMap<String, BigInteger> constants() {
    return constants.get();
  }

  RangeEstimator ranges() {
    return ranges.get();
  }

  /**
   * The edges that an execution of the program can reach. Edges of functions that are never called
   * are not included, and neither are edges behind a condition that is never true or behind a call
   * of a function that does not return.
   */
  ImmutableSet<CFAEdge> reachableEdges() {
    return reachable.get().edges();
  }

  /**
   * The locations that an execution of the program can reach, cf. {@link #reachableEdges()}. The
   * location behind a call of a function that does not return is included, because a specification
   * automaton reports a violation at the location behind the edge it matches, and that edge can be
   * the call of the error function itself.
   */
  ImmutableSet<CFANode> reachableNodes() {
    return reachable.get().nodes();
  }

  /**
   * Whether the program uses the address of a function. A function without a body can call such a
   * function, and the CFA contains no edge for that call, so a rule that argues about the reachable
   * part of the program has to abstain.
   */
  boolean usesFunctionAddress() {
    return reachable.get().usesFunctionAddress();
  }

  /**
   * The functions without a body that the program calls and that we know nothing about. A rule that
   * argues about what the program does has to abstain if this set is not empty, because such a
   * function can allocate memory, create a thread, run forever, or call a function of the program.
   */
  ImmutableSet<String> unknownFunctions() {
    return unknownFunctions.get();
  }

  /** The sequence of edges that every execution starts with, cf. {@link Chain}. */
  Chain chain() {
    return chain.get();
  }

  /**
   * The locations at which the specification automata can report a violation, or an empty {@link
   * Optional} if they could not be computed. An empty set means that no automaton of the
   * specification can ever match, which proves the specification for every proposition that is
   * given by an automaton.
   */
  Optional<ImmutableSet<CFANode>> targetLocations() {
    return targetLocations.get();
  }

  Optional<LoopStructure> loopStructure() {
    return cfa.getLoopStructure();
  }

  /** The locations that are part of a cycle in the call graph, i.e., of a recursion. */
  ImmutableSet<CFANode> recursionNodes() {
    return recursionNodes.get();
  }

  /**
   * Whether the given option of the CFA construction is explicitly set to the given value.
   *
   * <p>A rule whose argument depends on such an option must not rely on the default value: the CFA
   * is constructed once for the whole analysis, and the options of a nested configuration are
   * ignored (cf. {@link org.sosy_lab.cpachecker.core.algorithm.NestingAlgorithm}), so the CFA that
   * a rule sees is the one that the surrounding configuration asked for.
   */
  @SuppressWarnings("deprecation") // we deliberately look at the option of another component
  boolean isOptionSetTo(String pOption, boolean pValue) {
    String value = config.getProperty(pOption);
    return value != null && Boolean.parseBoolean(value.trim()) == pValue;
  }

  /**
   * Whether the CFA contains a location that {@code cfa.removeTrivialLoops} created: that option
   * replaces a loop that consists of blank edges with the end of the program, which is not valid
   * for the termination property.
   */
  boolean hasReplacedTrivialLoop() {
    for (CFANode node : reachableNodes()) {
      if (node instanceof CFATerminationNode) {
        for (CFAEdge edge : node.getEnteringEdges()) {
          if (edge instanceof BlankEdge) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Whether the program is a C program. A rule that reads the AST of the program has to abstain
   * otherwise, because it only knows the AST of C.
   */
  boolean isCProgram() {
    return cfa.getLanguage() == Language.C;
  }

  boolean isDefinedFunction(String pName) {
    return cfa.getAllFunctionNames().contains(pName);
  }

  /** All AST nodes of the given edge, including the nested ones. */
  static FluentIterable<CAstNode> astNodes(CFAEdge pEdge) {
    return from(CFAUtils.getAstNodesFromCfaEdge(pEdge))
        .filter(CAstNode.class)
        .transformAndConcat(node -> CFAUtils.traverseRecursively(node));
  }

  /** The call of the given edge, if the edge calls a function without a body. */
  static @Nullable AFunctionCall callWithoutBody(CFAEdge pEdge) {
    if (pEdge instanceof AStatementEdge statementEdge
        && statementEdge.getStatement() instanceof AFunctionCall call) {
      // A call of a function with a body is a FunctionCallEdge together with a summary edge, so a
      // statement edge with a call means that we do not have the body.
      return call;
    }
    return null;
  }

  /** The name of the function that the given edge calls without knowing its body. */
  static @Nullable String nameOfCallWithoutBody(CFAEdge pEdge) {
    AFunctionCall call = callWithoutBody(pEdge);
    if (call == null) {
      return null;
    }
    AExpression name = call.getFunctionCallExpression().getFunctionNameExpression();
    return name instanceof AIdExpression id ? id.getName() : null;
  }

  /** Whether the given edge calls a function that ends the execution. */
  boolean neverReturns(CFAEdge pEdge) {
    String name = nameOfCallWithoutBody(pEdge);
    return name != null && !isDefinedFunction(name) && FUNCTIONS_THAT_DO_NOT_RETURN.contains(name);
  }

  /** Whether the given edge calls a function without a body that we know nothing about. */
  boolean callsUnknownFunction(CFAEdge pEdge) {
    AFunctionCall call = callWithoutBody(pEdge);
    if (call == null) {
      return false;
    }
    String name = nameOfCallWithoutBody(pEdge);
    if (name == null) {
      // A call through a function pointer that the CFA construction could not resolve.
      return true;
    }
    return !isDefinedFunction(name) && !isKnownHarmless(name);
  }

  private static boolean isKnownHarmless(String pName) {
    return KNOWN_HARMLESS_FUNCTIONS.contains(pName) || pName.startsWith(NONDET_FUNCTION_PREFIX);
  }

  /**
   * Whether the given assume edge cannot be taken by any execution, because its condition is
   * constant.
   */
  boolean cannotBeTaken(CFAEdge pEdge) {
    if (!(pEdge instanceof CAssumeEdge assumeEdge)) {
      return false;
    }
    CExpression condition = assumeEdge.getExpression();
    return assumeEdge.getTruthAssumption()
        ? ranges().isAlwaysFalse(condition)
        : ranges().isAlwaysTrue(condition);
  }

  private Reachable computeReachable() {
    Set<CFANode> nodes = new LinkedHashSet<>();
    Set<CFANode> explored = new HashSet<>();
    Set<CFAEdge> edges = new LinkedHashSet<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    boolean functionAddressTaken = false;

    nodes.add(cfa.getMainFunction());
    explored.add(cfa.getMainFunction());
    waitlist.push(cfa.getMainFunction());
    while (!waitlist.isEmpty()) {
      CFANode node = waitlist.pop();
      for (CFAEdge edge : node.getAllLeavingEdges()) {
        if (cannotBeTaken(edge)) {
          continue;
        }
        edges.add(edge);
        functionAddressTaken = functionAddressTaken || takesFunctionAddress(edge);
        CFANode successor = edge.getSuccessor();
        nodes.add(successor);
        if (!neverReturns(edge) && explored.add(successor)) {
          waitlist.push(successor);
        }
      }
    }

    if (functionAddressTaken) {
      // A function whose address is taken can be called by a function without a body, and such a
      // call is not part of the CFA. We therefore consider the whole program as reachable.
      logger.log(
          Level.FINE,
          "The program uses the address of a function, so all functions are considered reachable.");
      return new Reachable(ImmutableSet.copyOf(cfa.nodes()), CFAUtils.allEdges(cfa).toSet(), true);
    }
    return new Reachable(ImmutableSet.copyOf(nodes), ImmutableSet.copyOf(edges), false);
  }

  /** Whether the given edge uses a function as a value instead of calling it. */
  private static boolean takesFunctionAddress(CFAEdge pEdge) {
    Set<CAstNode> namesOfCalls = Sets.newIdentityHashSet();
    for (CAstNode node : astNodes(pEdge)) {
      if (node instanceof CFunctionCallExpression call) {
        namesOfCalls.add(call.getFunctionNameExpression());
      }
    }
    for (CAstNode node : astNodes(pEdge)) {
      if (node instanceof CIdExpression id
          && id.getDeclaration() instanceof CFunctionDeclaration
          && !namesOfCalls.contains(node)) {
        return true;
      }
    }
    return false;
  }

  private ImmutableSet<String> computeUnknownFunctions() {
    ImmutableSet.Builder<String> result = ImmutableSet.builder();
    for (CFAEdge edge : reachableEdges()) {
      if (callsUnknownFunction(edge)) {
        String name = nameOfCallWithoutBody(edge);
        result.add(name == null ? "call through a function pointer" : name);
      }
    }
    return result.build();
  }

  private Chain computeChain() {
    List<CFAEdge> edges = new ArrayList<>();
    Set<CFANode> visited = new HashSet<>();
    CFANode node = cfa.getMainFunction();
    while (true) {
      if (!visited.add(node)) {
        // Every location so far had exactly one possible successor, so the execution really walks
        // along this sequence of edges, and it repeats itself from here on.
        return new Chain(ImmutableList.copyOf(edges), ChainEnd.REPEATED_LOCATION);
      }
      // We follow the call edge of a call instead of its summary edge, i.e., we walk into the
      // called function. If it has several call sites, its exit node has several leaving edges and
      // the chain ends there.
      List<CFAEdge> possible = new ArrayList<>(1);
      for (CFAEdge edge : node.getLeavingEdges()) {
        if (!cannotBeTaken(edge)) {
          possible.add(edge);
        }
      }
      if (possible.size() != 1) {
        return new Chain(
            ImmutableList.copyOf(edges),
            possible.isEmpty() ? ChainEnd.PROGRAM_END : ChainEnd.BRANCHING);
      }
      CFAEdge edge = possible.get(0);
      edges.add(edge);
      if (neverReturns(edge)) {
        return new Chain(ImmutableList.copyOf(edges), ChainEnd.PROGRAM_EXIT);
      }
      if (callsUnknownFunction(edge)) {
        // The function might not return, so we do not know that the next edge is executed.
        return new Chain(ImmutableList.copyOf(edges), ChainEnd.UNKNOWN_CALL);
      }
      node = edge.getSuccessor();
    }
  }

  private Optional<ImmutableSet<CFANode>> computeTargetLocations() {
    ImmutableSet<CFANode> locations =
        new TargetLocationProviderImpl(shutdownNotifier, logger, cfa)
            .tryGetAutomatonTargetLocations(cfa.getMainFunction(), specification);
    if (locations.size() >= cfa.nodes().size()) {
      // The provider returns all nodes of the CFA if it could not compute the target locations.
      return Optional.empty();
    }
    return Optional.of(locations);
  }

  private ImmutableSet<CFANode> computeRecursionNodes() {
    ImmutableSet.Builder<CFANode> result = ImmutableSet.builder();
    for (Loop recursion : LoopStructure.getRecursions(cfa)) {
      result.addAll(recursion.getLoopNodes());
    }
    return result.build();
  }

  private ImmutableMap<String, BigInteger> computeConstants() {
    // A variable is constant if it is declared exactly once with a constant initializer and none of
    // the rejected operations is applied to it anywhere in the program, so we look at the whole
    // CFA and not only at the reachable part of it.
    Map<String, BigInteger> candidates = new HashMap<>();
    Set<String> rejected = new HashSet<>();
    RangeEstimator literals = new RangeEstimator(machineModel(), ImmutableMap.of());

    for (CFAEdge edge : CFAUtils.allEdges(cfa)) {
      for (CAstNode node : astNodes(edge)) {
        if (node instanceof CVariableDeclaration declaration) {
          String name = declaration.getQualifiedName();
          BigInteger value = constantInitializer(declaration, literals);
          if (value == null || candidates.containsKey(name)) {
            rejected.add(name);
          } else {
            candidates.put(name, value);
          }

        } else if (node instanceof CAssignment assignment) {
          if (assignment.getLeftHandSide() instanceof CIdExpression id
              && id.getDeclaration() != null) {
            rejected.add(id.getDeclaration().getQualifiedName());
          }

        } else if (node instanceof CUnaryExpression unary
            && unary.getOperator() == UnaryOperator.AMPER
            && unary.getOperand() instanceof CIdExpression id
            && id.getDeclaration() != null) {
          // The variable can be assigned through the pointer, also by a function without a body.
          rejected.add(id.getDeclaration().getQualifiedName());
        }
      }
    }

    ImmutableMap.Builder<String, BigInteger> result = ImmutableMap.builder();
    for (Map.Entry<String, BigInteger> candidate : candidates.entrySet()) {
      if (!rejected.contains(candidate.getKey())) {
        result.put(candidate);
      }
    }
    return result.buildOrThrow();
  }

  /**
   * The value that the given declaration gives its variable in every execution, or {@code null} if
   * there is no such value.
   */
  private static @Nullable BigInteger constantInitializer(
      CVariableDeclaration pDeclaration, RangeEstimator pLiterals) {
    if (!pDeclaration.isGlobal() && pDeclaration.getCStorageClass() != CStorageClass.STATIC) {
      // A local variable is initialized when its declaration is executed, and a goto can jump over
      // that declaration.
      return null;
    }
    if (pDeclaration.getCStorageClass() == CStorageClass.EXTERN) {
      // The definition of the variable, and thus its value, is not part of the program.
      return null;
    }
    if (pDeclaration.getType().isVolatile()
        || pLiterals.rangeOfType(pDeclaration.getType()) == null) {
      return null;
    }
    if (!(pDeclaration.getInitializer() instanceof CInitializerExpression initializer)) {
      return null;
    }
    IntegerRange range = pLiterals.rangeOf(initializer.getExpression());
    return range == null ? null : range.exactValue();
  }
}
