// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACheck;
import org.sosy_lab.cpachecker.cfa.CFAReversePostorder;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * Tests for the parts of the unrolling that only the CFA shows, like which edges and variables the
 * copies contain.
 *
 * <p>Which loops the heuristic counts, and whether the unrolled program still behaves like the
 * original one, is checked by the verification tasks in {@value #PROGRAMS}, which {@code
 * test/test-sets/integration-loop-unrolling.xml} runs. The tests here use some of their programs.
 */
public class LoopUnrollerTest {

  private static final String PROGRAMS = "test/programs/simple/loop-unrolling/";

  private static final LogManager logger = LogManager.createTestLogManager();

  /** Builds a CFA for the given program from {@link #PROGRAMS} and returns it in a mutable form. */
  private static MutableCFA createCfa(String pProgram) throws Exception {
    // The tests below unroll explicitly, so the loops have to still be there when we start.
    CFA cfa =
        TestCfaUtils.makeCfaFromFile(
            PROGRAMS + pProgram, Map.entry("cfa.unrollBoundedLoops", "false"));
    MutableCFA mutableCfa =
        MutableCFA.copyOf(cfa, TestUtils.configurationForTest().build(), logger);
    // Loop detection needs these, they are usually assigned after all CFA post-processings.
    mutableCfa.entryNodes().forEach(CFAReversePostorder::assignIds);
    return mutableCfa;
  }

  private static LoopUnroller createUnroller() throws Exception {
    return new LoopUnroller(logger, TestUtils.configurationForTest().build());
  }

  /** The loop with the most nodes, which is the outermost one if the loops are nested. */
  private static Loop outermostLoop(MutableCFA pCfa) throws Exception {
    return LoopStructure.getLoopStructure(pCfa).getAllLoops().stream()
        .max(Comparator.comparingInt(loop -> loop.getLoopNodes().size()))
        .orElseThrow();
  }

  private static int loopCount(MutableCFA pCfa) throws Exception {
    // The copies that the unrolling created do not have these yet.
    pCfa.entryNodes().forEach(CFAReversePostorder::assignIds);
    return LoopStructure.getLoopStructure(pCfa).getCount();
  }

  /**
   * How often the given statement occurs in the CFA. Matches the statement itself instead of the
   * source code of the edge, because the source code of a labelled statement also contains its
   * label.
   */
  private static int statementCount(CFA pCfa, String pStatement) {
    return CFAUtils.allEdges(pCfa)
        .filter(AStatementEdge.class)
        .filter(edge -> edge.getStatement().toASTString().contains(pStatement))
        .size();
  }

  /** How often the given condition occurs in the CFA, no matter whether it still branches. */
  private static int conditionCount(CFA pCfa, String pCondition) {
    return CFAUtils.allEdges(pCfa)
        .filter(edge -> edge.getDescription().contains(pCondition))
        .size();
  }

  /**
   * The variables that the CFA declares for the one with the given name in the source code. Looks
   * at the name in the source code because the unrolling renames the copies.
   */
  private static ImmutableSet<ASimpleDeclaration> declarationsOf(CFA pCfa, String pSourceName) {
    return CFAUtils.allEdges(pCfa)
        .filter(ADeclarationEdge.class)
        .<ASimpleDeclaration>transform(ADeclarationEdge::getDeclaration)
        .filter(declaration -> declaration.getOrigName().equals(pSourceName))
        .toSet();
  }

  /** The variables for the one with the given name in the source code that go out of scope. */
  private static ImmutableSet<ASimpleDeclaration> outOfScopeVariablesOf(
      CFA pCfa, String pSourceName) {
    return pCfa.nodes().stream()
        .flatMap(node -> node.getOutOfScopeVariables().stream())
        .filter(variable -> variable.getOrigName().equals(pSourceName))
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Asserts that every variable that the CFA declares for the one with the given name in the source
   * code goes out of scope after its declaration and before the next one is declared, i.e. within
   * its own copy of the loop body.
   */
  private static void assertEveryCopyGoesOutOfScopeInItsIteration(CFA pCfa, String pSourceName) {
    for (ADeclarationEdge declarationEdge :
        CFAUtils.allEdges(pCfa).filter(ADeclarationEdge.class)) {
      ASimpleDeclaration declaration = declarationEdge.getDeclaration();
      if (!declaration.getOrigName().equals(pSourceName)) {
        continue;
      }
      NodeCollectingCFAVisitor reached =
          new NodeCollectingCFAVisitor(
              new DefaultCFAVisitor() {
                @Override
                public TraversalProcess visitEdge(CFAEdge pEdge) {
                  // Declaring the next copy starts the next iteration.
                  return pEdge instanceof ADeclarationEdge nextDeclaration
                          && nextDeclaration.getDeclaration().getOrigName().equals(pSourceName)
                      ? TraversalProcess.SKIP
                      : TraversalProcess.CONTINUE;
                }
              });
      CFATraversal.dfs().traverseOnce(declarationEdge.getSuccessor(), reached);
      assertWithMessage("nodes where %s goes out of scope", declaration.getQualifiedName())
          .that(
              reached.getVisitedNodes().stream()
                  .filter(node -> node.getOutOfScopeVariables().contains(declaration)))
          .isNotEmpty();
    }
  }

  /**
   * The uses of the variables for the one with the given name in the source code, each together
   * with the edge it is on. Both reads and writes count as a use.
   */
  private static ImmutableList<Map.Entry<CFAEdge, AIdExpression>> usesOf(
      CFA pCfa, String pSourceName) {
    ImmutableList.Builder<Map.Entry<CFAEdge, AIdExpression>> uses = ImmutableList.builder();
    for (CFAEdge edge : CFAUtils.allEdges(pCfa)) {
      for (AAstNode astNode : CFAUtils.getAstNodesFromCfaEdge(edge)) {
        for (AAstNode subNode : CFAUtils.traverseRecursively(astNode)) {
          if (subNode instanceof AIdExpression use
              && use.getDeclaration() != null
              && use.getDeclaration().getOrigName().equals(pSourceName)) {
            uses.add(Map.entry(edge, use));
          }
        }
      }
    }
    return uses.build();
  }

  /**
   * The declarations of the variables for the one with the given name in the source code that come
   * last before the given edge, i.e. the first ones that a backwards search from it finds on each
   * path.
   */
  private static ImmutableSet<ASimpleDeclaration> closestDeclarationsBefore(
      CFAEdge pEdge, String pSourceName) {
    Set<ASimpleDeclaration> closest = new HashSet<>();
    CFATraversal.dfs()
        .backwards()
        .traverseOnce(
            pEdge.getPredecessor(),
            new DefaultCFAVisitor() {
              @Override
              public TraversalProcess visitEdge(CFAEdge pVisited) {
                if (pVisited instanceof ADeclarationEdge declarationEdge
                    && declarationEdge.getDeclaration().getOrigName().equals(pSourceName)) {
                  closest.add(declarationEdge.getDeclaration());
                  // Anything further back is hidden by this declaration.
                  return TraversalProcess.SKIP;
                }
                return TraversalProcess.CONTINUE;
              }
            });
    return ImmutableSet.copyOf(closest);
  }

  /**
   * Asserts that every use of a variable for the one with the given name in the source code refers
   * to the declaration that comes last before it on every path. In an unrolled loop this is the one
   * of the same copy of the body, and a use of a variable that is no longer declared fails as well.
   */
  private static void assertEveryUseSeesTheClosestDeclaration(CFA pCfa, String pSourceName) {
    for (Map.Entry<CFAEdge, AIdExpression> use : usesOf(pCfa, pSourceName)) {
      assertWithMessage("closest declarations of %s before %s", pSourceName, use.getKey())
          .that(closestDeclarationsBefore(use.getKey(), pSourceName))
          .containsExactly(use.getValue().getDeclaration());
    }
  }

  /** How often the given condition still branches instead of being assumed to hold. */
  private static int branchingConditionCount(CFA pCfa, String pCondition) {
    return CFAUtils.allEdges(pCfa)
        .filter(AssumeEdge.class)
        .filter(edge -> edge.getDescription().contains(pCondition))
        .size();
  }

  /**
   * Checks that the CFA is still well-formed, in particular that it has no dead or dangling code.
   */
  private static void assertIsValidCfa(MutableCFA pCfa) {
    for (String function : pCfa.getAllFunctionNames()) {
      CFACheck.check(
          pCfa.getFunctionHead(function), pCfa.getFunctionNodes(function), pCfa.getMachineModel());
    }
  }

  /** Unrolls the outermost loop of the given program and returns the resulting CFA. */
  private static MutableCFA unrollOutermostLoop(String pProgram, int pEntryVisits)
      throws Exception {
    MutableCFA cfa = createCfa(pProgram);
    createUnroller().unrollLoopExactly(cfa, outermostLoop(cfa), pEntryVisits);
    assertIsValidCfa(cfa);
    return cfa;
  }

  @Test
  public void testWhileLoop() throws Exception {
    // The head is visited 4 times, so the body runs 3 times.
    MutableCFA cfa = unrollOutermostLoop("counted-while-safe.c", 4);

    assertThat(loopCount(cfa)).isEqualTo(0);
    assertThat(statementCount(cfa, "s = s + i")).isEqualTo(3);
    assertThat(statementCount(cfa, "i = i + 1")).isEqualTo(3);
    // One condition per visit of the head, the first three of which continue the loop.
    assertThat(conditionCount(cfa, "i < 3")).isEqualTo(4);
  }

  /** The condition of the loop is reached twice per iteration, so it has to stay a condition. */
  @Test
  public void testLoopWithSeveralChecksOfItsCondition() throws Exception {
    MutableCFA cfa = unrollOutermostLoop("shape-several-checks-of-the-condition-safe.c", 4);

    assertThat(statementCount(cfa, "s = s + 1")).isEqualTo(4);
    // Every unrolling contains the condition, but only the last one still branches on it. The
    // others assume that the loop goes on, no matter how often they reach it.
    assertThat(conditionCount(cfa, "i >= 4")).isEqualTo(5);
    assertThat(branchingConditionCount(cfa, "i >= 4")).isEqualTo(2);
    // Jumping back to the condition is a cycle that does not pass the entry node, so it does not
    // end an iteration and stays a loop in every unrolling.
    assertThat(loopCount(cfa)).isEqualTo(4);
  }

  /** A loop nested in the unrolled loop is copied along and stays a loop. */
  @Test
  public void testNestedLoopIsKept() throws Exception {
    MutableCFA cfa = unrollOutermostLoop("counted-outer-of-uncounted-inner-safe.c", 4);

    // The three unrollings that run the body each contain their own copy of the inner loop, the
    // last unrolling only leaves the outer loop and does not contain it.
    assertThat(loopCount(cfa)).isEqualTo(3);
    assertThat(statementCount(cfa, "s = s + 1")).isEqualTo(3);
    // The inner loop is untouched, so its condition still branches in each of the three copies.
    assertThat(branchingConditionCount(cfa, "j < i")).isEqualTo(6);
  }

  /**
   * Sharing one declaration between the copies would declare the same variable again in every
   * unrolling, which no C program does and which would let an uninitialized variable keep the value
   * of the copy before. So every copy of the body declares a variable of its own.
   */
  @Test
  public void testLoopThatDeclaresAVariableGivesEveryCopyItsOwn() throws Exception {
    MutableCFA cfa = unrollOutermostLoop("counted-declares-a-variable-safe.c", 4);

    assertThat(loopCount(cfa)).isEqualTo(0);
    // One for each of the three copies of the body.
    assertThat(declarationsOf(cfa, "j")).hasSize(3);
    // The reads change along with the declaration, so no copy reads the variable of another.
    assertThat(usesOf(cfa, "j")).hasSize(3);
    assertEveryUseSeesTheClosestDeclaration(cfa, "j");
  }

  /** The initializer of one variable can read another one that the same copy declares. */
  @Test
  public void testDeclarationsThatReadEachOtherAreRenamedTogether() throws Exception {
    MutableCFA cfa = unrollOutermostLoop("counted-declarations-read-each-other-safe.c", 4);

    assertThat(declarationsOf(cfa, "a")).hasSize(3);
    assertThat(declarationsOf(cfa, "b")).hasSize(3);
    // Each initializer of b reads the a of its own copy.
    assertThat(usesOf(cfa, "a")).hasSize(3);
    assertEveryUseSeesTheClosestDeclaration(cfa, "a");
    assertEveryUseSeesTheClosestDeclaration(cfa, "b");
  }

  /** A condition of the loop can read a variable that the loop declares as well. */
  @Test
  public void testConditionsThatReadADeclaredVariableAreRenamed() throws Exception {
    MutableCFA cfa = unrollOutermostLoop("counted-condition-reads-a-declared-variable-safe.c", 4);

    assertThat(loopCount(cfa)).isEqualTo(0);
    // The condition still branches in each of the three copies, and both of its edges read the
    // variable of their own copy.
    assertThat(branchingConditionCount(cfa, " > 1")).isEqualTo(6);
    assertThat(usesOf(cfa, "j")).hasSize(6);
    assertEveryUseSeesTheClosestDeclaration(cfa, "j");
  }

  /**
   * Analyses like SMG2 end the lifetime of a variable where it goes out of scope, so a pointer to
   * it dangles afterwards. Every copy has to keep that for its own variables, both at the end of
   * the body and at the end of a block nested in it.
   *
   * <p>Unrolls during CFA creation instead of using {@link #createCfa}, because {@link
   * MutableCFA#copyOf} does not keep which variables go out of scope.
   */
  @Test
  public void testDeclaredVariablesGoOutOfScopeInEveryCopy() throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromFile(
            PROGRAMS + "scope-arrays-used-in-their-iteration-safe.c",
            Map.entry("cfa.unrollBoundedLoops", "true"));

    for (String variable : ImmutableList.of("a", "b")) {
      // One per copy of the body that runs, and none of them is the variable of the original loop.
      assertThat(declarationsOf(cfa, variable)).hasSize(3);
      assertEveryCopyGoesOutOfScopeInItsIteration(cfa, variable);
      // Nothing refers to a variable that is no longer declared, like the one of the original loop.
      assertThat(declarationsOf(cfa, variable))
          .containsAtLeastElementsIn(outOfScopeVariablesOf(cfa, variable));
    }
  }

  /**
   * Unrolling a loop replaces the loops nested in it by a copy per iteration, so the loop structure
   * is computed again until nothing is left of the nest.
   */
  @Test
  public void testUnrollBoundedLoopsUnrollsANestOfCountedLoops() throws Exception {
    MutableCFA cfa = createCfa("counted-nested-safe.c");

    createUnroller().unrollBoundedLoops(cfa);

    assertIsValidCfa(cfa);
    assertThat(loopCount(cfa)).isEqualTo(0);
    // Three runs of the outer body, each of which runs the inner body twice.
    assertThat(statementCount(cfa, "s = s + 1")).isEqualTo(6);
    // Every copy of the outer body declares its own counter for the inner loop, and the unrolled
    // inner loops only use the counter of the copy they are in.
    assertThat(declarationsOf(cfa, "j")).hasSize(3);
    assertEveryUseSeesTheClosestDeclaration(cfa, "j");
  }
}
