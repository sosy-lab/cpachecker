// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.Test;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACheck;
import org.sosy_lab.cpachecker.cfa.CFAReversePostorder;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class LoopUnrollerTest {

  private static final LogManager logger = LogManager.createTestLogManager();

  /** Builds a CFA for the given function body and returns it in a mutable form. */
  private static MutableCFA createCfa(String pFunctionBody) throws Exception {
    // The tests below unroll explicitly, so the loops have to still be there when we start.
    CFA cfa =
        TestCfaUtils.makeCfaFromFunctionBody(
            pFunctionBody, Map.entry("cfa.unrollBoundedLoops", "false"));
    MutableCFA mutableCfa =
        MutableCFA.copyOf(cfa, TestUtils.configurationForTest().build(), logger);
    // Loop detection needs these, they are usually assigned after all CFA post-processings.
    mutableCfa.entryNodes().forEach(CFAReversePostorder::assignIds);
    return mutableCfa;
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

  /** The qualified names of all variables that the CFA declares. */
  private static ImmutableList<String> declaredVariables(CFA pCfa) {
    return CFAUtils.allEdges(pCfa)
        .filter(ADeclarationEdge.class)
        .transform(edge -> edge.getDeclaration().getQualifiedName())
        .toList();
  }

  /** How often the given declaration occurs in the CFA. */
  private static int declarationCount(CFA pCfa, String pDeclaration) {
    return CFAUtils.allEdges(pCfa)
        .filter(ADeclarationEdge.class)
        .filter(edge -> edge.getDeclaration().toASTString().equals(pDeclaration))
        .size();
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

  /** Unrolls the outermost loop of the given function body and returns the resulting CFA. */
  private static MutableCFA unrollOutermostLoop(String pFunctionBody, int pEntryVisits)
      throws Exception {
    MutableCFA cfa = createCfa(pFunctionBody);
    new LoopUnroller(logger).unrollLoopExactly(cfa, outermostLoop(cfa), pEntryVisits);
    assertIsValidCfa(cfa);
    return cfa;
  }

  @Test
  public void testWhileLoop() throws Exception {
    // The head is visited 4 times, so the body runs 3 times.
    MutableCFA cfa =
        unrollOutermostLoop(
            """
            int i = 0;
            int s = 0;
            while (i < 3) {
              s = s + i;
              i = i + 1;
            }
            """,
            4);

    assertThat(loopCount(cfa)).isEqualTo(0);
    assertThat(statementCount(cfa, "s = s + i")).isEqualTo(3);
    assertThat(statementCount(cfa, "i = i + 1")).isEqualTo(3);
    // One condition per visit of the head, the first three of which continue the loop.
    assertThat(conditionCount(cfa, "i < 3")).isEqualTo(4);
  }

  @Test
  public void testDoWhileLoop() throws Exception {
    // The condition comes last, so the entry node is visited as often as the body runs.
    MutableCFA cfa =
        unrollOutermostLoop(
            """
            int i = 0;
            int s = 0;
            do {
              s = s + i;
              i = i + 1;
            } while (i < 3);
            """,
            3);

    assertThat(loopCount(cfa)).isEqualTo(0);
    assertThat(statementCount(cfa, "s = s + i")).isEqualTo(3);
    assertThat(conditionCount(cfa, "i < 3")).isEqualTo(3);
  }

  @Test
  public void testContinue() throws Exception {
    MutableCFA cfa =
        unrollOutermostLoop(
            """
            int i = 0;
            int s = 0;
            while (i < 3) {
              i = i + 1;
              if (i == 2) {
                continue;
              }
              s = s + i;
            }
            """,
            4);

    assertThat(loopCount(cfa)).isEqualTo(0);
    assertThat(statementCount(cfa, "i = i + 1")).isEqualTo(3);
    assertThat(statementCount(cfa, "s = s + i")).isEqualTo(3);
  }

  /**
   * The loop is entered in the middle of its body, so its exit is not where one iteration ends. The
   * last unrolling must stop at the exit instead of running the rest of the body once more.
   */
  @Test
  public void testLoopThatIsLeftInTheMiddleOfItsBody() throws Exception {
    MutableCFA cfa =
        unrollOutermostLoop(
            """
            int i = 0;
            int s = 0;
            goto middle;
            body:
              s = s + i;
            middle:
              i = i + 1;
              if (i < 3) {
                goto body;
              }
            """,
            3);

    assertThat(loopCount(cfa)).isEqualTo(0);
    // Everything before the exit runs once per visit of the entry node, everything after it once
    // less, because the last unrolling leaves the loop before reaching it.
    assertThat(statementCount(cfa, "i = i + 1")).isEqualTo(3);
    assertThat(statementCount(cfa, "s = s + i")).isEqualTo(2);
  }

  /** The condition of the loop is reached twice per iteration, so it has to stay a condition. */
  @Test
  public void testLoopWithSeveralChecksOfItsCondition() throws Exception {
    MutableCFA cfa =
        unrollOutermostLoop(
            """
            int i = 0;
            int s = 0;
            goto start;
            start:
              s = s + 1;
            check:
              if (i >= 4) {
                goto end;
              }
              i = i + 1;
              if (i == 2) {
                goto check;
              }
              goto start;
            end: ;
            """,
            4);

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
    MutableCFA cfa =
        unrollOutermostLoop(
            """
            int i = 0;
            int s = 0;
            int j = 0;
            while (i < 3) {
              j = 0;
              while (j < i) {
                s = s + 1;
                j = j + 1;
              }
              i = i + 1;
            }
            """,
            4);

    // The three unrollings that run the body each contain their own copy of the inner loop, the
    // last unrolling only leaves the outer loop and does not contain it.
    assertThat(loopCount(cfa)).isEqualTo(3);
    assertThat(statementCount(cfa, "s = s + 1")).isEqualTo(3);
    // The inner loop is untouched, so its condition still branches in each of the three copies.
    assertThat(branchingConditionCount(cfa, "j < i")).isEqualTo(6);
  }

  @Test
  public void testLoopThatRunsItsBodyOnce() throws Exception {
    MutableCFA cfa =
        unrollOutermostLoop(
            """
            int i = 2;
            int s = 0;
            while (i < 3) {
              s = s + 1;
              i = i + 1;
            }
            """,
            2);

    assertThat(loopCount(cfa)).isEqualTo(0);
    assertThat(statementCount(cfa, "s = s + 1")).isEqualTo(1);
  }

  /** A loop whose body never runs still visits its entry node once and checks its condition. */
  @Test
  public void testLoopThatNeverRunsItsBody() throws Exception {
    MutableCFA cfa =
        unrollOutermostLoop(
            """
            int i = 5;
            int s = 0;
            while (i < 3) {
              s = s + 1;
              i = i + 1;
            }
            """,
            1);

    assertThat(loopCount(cfa)).isEqualTo(0);
    assertThat(statementCount(cfa, "s = s + 1")).isEqualTo(0);
    assertThat(conditionCount(cfa, "i < 3")).isEqualTo(1);
  }

  /**
   * Every unrolling would declare the same variable again, which no C program does and which some
   * analyses do not expect, so such loops are not unrolled.
   */
  @Test
  public void testLoopThatDeclaresAVariableGivesEveryCopyItsOwn() throws Exception {
    MutableCFA cfa =
        unrollOutermostLoop(
            """
            int i = 0;
            int s = 0;
            while (i < 3) {
              int j = i + 1;
              s = s + j;
              i = i + 1;
            }
            """,
            4);

    assertThat(loopCount(cfa)).isEqualTo(0);
    // Sharing one declaration would let an uninitialized variable keep the value of the copy
    // before, so each of the three copies of the body declares its own variable.
    assertThat(declaredVariables(cfa)).containsAtLeast("main::__j_0", "main::__j_1", "main::__j_2");
    assertThat(declaredVariables(cfa)).doesNotContain("main::j");
    // The reads are renamed along with the declaration, so no copy reads the variable of another.
    assertThat(statementCount(cfa, "s = s + __j_0")).isEqualTo(1);
    assertThat(statementCount(cfa, "s = s + __j_1")).isEqualTo(1);
    assertThat(statementCount(cfa, "s = s + __j_2")).isEqualTo(1);
  }

  /** The initializer of one variable can read another one that the same copy declares. */
  @Test
  public void testDeclarationsThatReadEachOtherAreRenamedTogether() throws Exception {
    MutableCFA cfa =
        unrollOutermostLoop(
            """
            int i = 0;
            int s = 0;
            while (i < 3) {
              int a = i + 1;
              int b = a + 1;
              s = s + b;
              i = i + 1;
            }
            """,
            4);

    assertThat(declarationCount(cfa, "int __b_0 = __a_0 + 1;")).isEqualTo(1);
    assertThat(declarationCount(cfa, "int __b_2 = __a_2 + 1;")).isEqualTo(1);
    assertThat(declarationCount(cfa, "int __b_0 = __a_1 + 1;")).isEqualTo(0);
  }

  /** A condition of the loop can read a variable that the loop declares as well. */
  @Test
  public void testConditionsThatReadADeclaredVariableAreRenamed() throws Exception {
    MutableCFA cfa =
        unrollOutermostLoop(
            """
            int i = 0;
            int s = 0;
            while (i < 3) {
              int j = i + 1;
              if (j > 1) {
                s = s + 1;
              }
              i = i + 1;
            }
            """,
            4);

    assertThat(loopCount(cfa)).isEqualTo(0);
    assertThat(branchingConditionCount(cfa, "__j_0 > 1")).isEqualTo(2);
    assertThat(branchingConditionCount(cfa, "__j_2 > 1")).isEqualTo(2);
    assertThat(branchingConditionCount(cfa, "[j > 1]")).isEqualTo(0);
  }

  /**
   * The length of a variable length array is an expression that reads another variable, and it
   * lives in the type, which the renaming does not reach into.
   */
  @Test
  public void testLoopThatDeclaresAVariableLengthArrayIsNotUnrolled() throws Exception {
    assertIsNotUnrolled(
        """
        int i = 0;
        int s = 0;
        while (i < 3) {
          int n = i + 2;
          int a[n];
          a[0] = 1;
          s = s + a[0];
          i = i + 1;
        }
        """,
        4);
  }

  /** With several ways out of the loop we cannot tell which one is the exit condition. */
  @Test
  public void testLoopWithSeveralExitsIsNotUnrolled() throws Exception {
    assertIsNotUnrolled(
        """
        int i = 0;
        int s = 0;
        while (i < 3) {
          if (s > 100) {
            break;
          }
          s = s + i;
          i = i + 1;
        }
        """,
        4);
  }

  /** We would not know which unrolling the second entry belongs to. */
  @Test
  public void testLoopWithSeveralEntryNodesIsNotUnrolled() throws Exception {
    assertIsNotUnrolled(
        """
        int i = 0;
        int s = 0;
        if (s == 0) {
          goto body;
        }
        while (i < 3) {
        body:
          s = s + i;
          i = i + 1;
        }
        """,
        4);
  }

  @Test
  public void testLoopThatIsNotLeftAtAllIsNotUnrolled() throws Exception {
    assertIsNotUnrolled(
        """
        int s = 0;
        while (1) {
          s = s + 1;
        }
        """,
        4);
  }

  /** The loop is always entered, so it always visits its entry node at least once. */
  @Test
  public void testUnrollingZeroTimesIsRejected() throws Exception {
    MutableCFA cfa =
        createCfa(
            """
            int i = 0;
            while (i < 3) {
              i = i + 1;
            }
            """);
    Loop loop = outermostLoop(cfa);
    LoopUnroller unroller = new LoopUnroller(logger);

    assertThrows(IllegalArgumentException.class, () -> unroller.unrollLoopExactly(cfa, loop, 0));
  }

  /** Asserts that the loop is left untouched, so that the CFA still contains it. */
  private static void assertIsNotUnrolled(String pFunctionBody, int pEntryVisits) throws Exception {
    MutableCFA cfa = createCfa(pFunctionBody);
    int nodesBefore = cfa.nodes().size();

    new LoopUnroller(logger).unrollLoopExactly(cfa, outermostLoop(cfa), pEntryVisits);

    assertThat(cfa.nodes()).hasSize(nodesBefore);
    assertThat(loopCount(cfa)).isAtLeast(1);
  }

  /** The number of visits of its entry node after which the outermost loop is left. */
  private static OptionalInt entryVisitsOf(String pFunctionBody) throws Exception {
    MutableCFA cfa = createCfa(pFunctionBody);
    return new LoopUnroller(logger).findExactLoopIterationCount(cfa, outermostLoop(cfa));
  }

  @Test
  public void testCountsVisitsOfAWhileLoop() throws Exception {
    // The condition is checked with 0, 1, 2 and 3, and the last check leaves the loop.
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                while (i < 3) {
                  i = i + 1;
                }
                """))
        .hasValue(4);
  }

  @Test
  public void testCountsVisitsOfADoWhileLoop() throws Exception {
    // The counter is already increased when the condition is checked, so it is checked with 1, 2
    // and 3 instead.
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                do {
                  i = i + 1;
                } while (i < 3);
                """))
        .hasValue(3);
  }

  /** The entry node is in the middle of the body, so an iteration is not a run of the body. */
  @Test
  public void testCountsVisitsOfALoopThatIsLeftInTheMiddleOfItsBody() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                int s = 0;
                goto middle;
                body:
                  s = s + i;
                middle:
                  i = i + 1;
                  if (i < 3) {
                    goto body;
                  }
                """))
        .hasValue(3);
  }

  @Test
  public void testCountsALoopThatCountsDown() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 10;
                while (i > 4) {
                  i = i - 2;
                }
                """))
        .hasValue(4);
  }

  @Test
  public void testCountsALoopWithTheConstantOnTheLeft() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                while (3 > i) {
                  i = i + 1;
                }
                """))
        .hasValue(4);
  }

  @Test
  public void testCountsALoopThatCountsPastItsBound() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                while (i != 6) {
                  i = i + 2;
                }
                """))
        .hasValue(4);
  }

  @Test
  public void testCountsALoopThatNeverRunsItsBody() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 5;
                while (i < 3) {
                  i = i + 1;
                }
                """))
        .hasValue(1);
  }

  /** The counter jumps over the bound, so the loop only ends by overflowing. */
  @Test
  public void testGivesUpOnALoopThatMissesItsBound() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                while (i != 5) {
                  i = i + 2;
                }
                """))
        .isEmpty();
  }

  /** The counter moves away from the bound, so the loop only ends by overflowing. */
  @Test
  public void testGivesUpOnACounterThatMovesAwayFromItsBound() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                while (i < 3) {
                  i = i - 1;
                }
                """))
        .isEmpty();
  }

  @Test
  public void testGivesUpOnACounterThatMovesAwayFromItsBoundWhileCountingDown() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 10;
                while (i > 4) {
                  i = i + 1;
                }
                """))
        .isEmpty();
  }

  /** An inequality is never reached from the wrong side either. */
  @Test
  public void testGivesUpOnACounterThatMovesAwayFromItsInequality() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                while (i != 5) {
                  i = i - 1;
                }
                """))
        .isEmpty();
  }

  /**
   * The direction of the counter does not matter if the loop is left before it ever moves, so the
   * shortcut for a counter that moves the wrong way must not reject this.
   */
  @Test
  public void testCountsALoopThatIsLeftBeforeItsCounterMovesTheWrongWay() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 5;
                while (i < 3) {
                  i = i - 1;
                }
                """))
        .hasValue(1);
  }

  @Test
  public void testGivesUpOnALoopThatRunsTooOften() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                while (i < 1000) {
                  i = i + 1;
                }
                """))
        .isEmpty();
  }

  /** The values we compute are only the ones of the program as long as they fit into its type. */
  @Test
  public void testGivesUpOnAnOverflowingCounter() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                char c = 100;
                while (c < 126) {
                  c = c + 10;
                }
                """))
        .isEmpty();
  }

  @Test
  public void testGivesUpOnAnUninitializedCounter() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i;
                while (i < 3) {
                  i = i + 1;
                }
                """))
        .isEmpty();
  }

  @Test
  public void testGivesUpOnDifferentStartValues() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                int x = 1;
                if (x > 0) {
                  i = 1;
                }
                while (i < 3) {
                  i = i + 1;
                }
                """))
        .isEmpty();
  }

  /** Which iteration leaves the loop would depend on the values of the other variables. */
  @Test
  public void testGivesUpOnAConditionalModification() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                int x = 1;
                while (i < 3) {
                  if (x > 0) {
                    i = i + 1;
                  }
                }
                """))
        .isEmpty();
  }

  /** How often a nested loop runs would have to be known as well. */
  @Test
  public void testGivesUpOnAModificationInsideANestedLoop() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                int j = 0;
                while (i < 6) {
                  j = 0;
                  while (j < 2) {
                    i = i + 1;
                    j = j + 1;
                  }
                }
                """))
        .isEmpty();
  }

  @Test
  public void testGivesUpOnSeveralModifications() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                while (i < 8) {
                  i = i + 1;
                  i = i + 2;
                }
                """))
        .isEmpty();
  }

  /** Something we do not see could write the counter through the pointer. */
  @Test
  public void testGivesUpOnACounterWhoseAddressIsTaken() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                int *p = &i;
                while (i < 3) {
                  i = i + 1;
                }
                """))
        .isEmpty();
  }

  /** The address of an element of an array is not the address of the index. */
  @Test
  public void testCountsALoopWhoseCounterIsUsedInsideAnAddress() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int a[8];
                int i = 0;
                int *p = &a[i];
                while (i < 3) {
                  i = i + 1;
                }
                """))
        .hasValue(4);
  }

  /** The parser folds a constant expression, so the offset arrives here as a literal. */
  @Test
  public void testCountsALoopThatStepsByAConstantExpression() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                while (i < 12) {
                  i = i + sizeof(int);
                }
                """))
        .hasValue(4);
  }

  @Test
  public void testCountsALoopThatStepsByAnEnumConstant() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                enum { STEP = 3 };
                int i = 0;
                while (i < 9) {
                  i = i + STEP;
                }
                """))
        .hasValue(4);
  }

  /** The parser rewrites a condition that is not a comparison into a comparison to zero. */
  @Test
  public void testCountsALoopWhoseConditionIsNotWrittenAsAComparison() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 3;
                while (i) {
                  i = i - 1;
                }
                """))
        .hasValue(4);
  }

  /** A static variable keeps its value, so the next call of the function starts somewhere else. */
  @Test
  public void testGivesUpOnAStaticCounter() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                static int i = 0;
                while (i < 3) {
                  i = i + 1;
                }
                """))
        .isEmpty();
  }

  @Test
  public void testGivesUpOnAVariableBound() throws Exception {
    assertThat(
            entryVisitsOf(
                """
                int i = 0;
                int n = 3;
                while (i < n) {
                  i = i + 1;
                }
                """))
        .isEmpty();
  }

  /** The count that the heuristic finds has to be the one that the unrolling expects. */
  @Test
  public void testUnrollBoundedLoopsRemovesACountedLoop() throws Exception {
    MutableCFA cfa =
        createCfa(
            """
            int i = 0;
            int s = 0;
            while (i < 3) {
              s = s + i;
              i = i + 1;
            }
            """);

    new LoopUnroller(logger).unrollBoundedLoops(cfa);

    assertIsValidCfa(cfa);
    assertThat(loopCount(cfa)).isEqualTo(0);
    assertThat(statementCount(cfa, "s = s + i")).isEqualTo(3);
  }

  /**
   * Unrolling the outer loop replaces the inner one by a copy per iteration. Those only become
   * visible after the loop structure is computed again, so each of them is unrolled in a later
   * round and nothing is left of the nest.
   */
  @Test
  public void testUnrollBoundedLoopsUnrollsTheCopiesOfANestedLoop() throws Exception {
    MutableCFA cfa =
        createCfa(
            """
            int i = 0;
            int s = 0;
            while (i < 3) {
              int j = 0;
              while (j < 2) {
                s = s + 1;
                j = j + 1;
              }
              i = i + 1;
            }
            """);

    new LoopUnroller(logger).unrollBoundedLoops(cfa);

    assertIsValidCfa(cfa);
    assertThat(loopCount(cfa)).isEqualTo(0);
    // Three runs of the outer body, each of which runs the inner body twice.
    assertThat(statementCount(cfa, "s = s + 1")).isEqualTo(6);
    // Every copy of the outer body declares its own counter for the inner loop.
    assertThat(declaredVariables(cfa)).containsAtLeast("main::__j_0", "main::__j_1", "main::__j_2");
  }

  /** Nested loops multiply, so the unrolling stops before a function grows without bound. */
  @Test
  public void testUnrollBoundedLoopsKeepsAFunctionWithinItsNodeBudget() throws Exception {
    String functionBody =
        """
        int i = 0;
        int s = 0;
        while (i < 10) {
          int j = 0;
          while (j < 10) {
            s = s + 1;
            j = j + 1;
          }
          i = i + 1;
        }
        """;

    MutableCFA withoutBudget = createCfa(functionBody);
    new LoopUnroller(logger).unrollBoundedLoops(withoutBudget);

    assertIsValidCfa(withoutBudget);
    // Nothing is left of the nest, at the price of one copy of the innermost body per pair of
    // iterations of the two loops.
    assertThat(loopCount(withoutBudget)).isEqualTo(0);
    assertThat(statementCount(withoutBudget, "s = s + 1")).isEqualTo(100);

    MutableCFA withBudget = createCfa(functionBody);
    LoopUnroller unroller = new LoopUnroller(logger);
    unroller.maxNodesPerFunction = 200;
    unroller.unrollBoundedLoops(withBudget);

    assertIsValidCfa(withBudget);
    assertThat(withBudget.getFunctionNodes("main").size()).isAtMost(200);
    assertThat(loopCount(withBudget)).isAtLeast(1);
  }

  /** A loop that the heuristic cannot count has to survive the post-processing unchanged. */
  @Test
  public void testUnrollBoundedLoopsKeepsAnUncountedLoop() throws Exception {
    MutableCFA cfa =
        createCfa(
            """
            int i;
            int s = 0;
            while (i < 3) {
              s = s + i;
              i = i + 1;
            }
            """);
    int nodesBefore = cfa.nodes().size();

    new LoopUnroller(logger).unrollBoundedLoops(cfa);

    assertIsValidCfa(cfa);
    assertThat(cfa.nodes()).hasSize(nodesBefore);
    assertThat(loopCount(cfa)).isEqualTo(1);
  }
}
