// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class MemoryManipulatingFunctionHandlingTest {

  private static final String SOLVER = "MathSAT5";
  private static final MachineModel MACHINE_MODEL = MachineModel.LINUX32;

  /** Formula type of an int variable. */
  private static final FormulaType<BitvectorFormula> INT_TYPE =
      FormulaType.getBitvectorTypeWithSize(
          MACHINE_MODEL.getSizeofInt() * MACHINE_MODEL.getSizeofCharInBits());

  /** Formula type of a char variable. */
  private static final FormulaType<BitvectorFormula> CHAR_TYPE =
      FormulaType.getBitvectorTypeWithSize(
          MACHINE_MODEL.getSizeofChar() * MACHINE_MODEL.getSizeofCharInBits());

  private Solver solver;
  private PathFormulaManager pathFormulaManager;
  private FormulaManagerView formulaManager;
  private BooleanFormulaManager booleanFormulaManager;

  private LogManager logger;
  private Configuration config;
  private ShutdownNotifier shutdownNotifier;

  @Before
  public void setUp() throws Exception {
    config =
        TestUtils.configurationForTest()
            .setOptions(
                ImmutableMap.of(
                    "solver.solver",
                    SOLVER,
                    // memset/memcpy handling is disabled by default; the tests below need it.
                    "cpa.predicate.enableMemoryAssignmentFunctions",
                    "true"))
            .build();
    shutdownNotifier = ShutdownNotifier.createDummy();
    logger = LogManager.createTestLogManager();
    solver = Solver.create(config, logger, shutdownNotifier);
    formulaManager = solver.getFormulaManager();
    booleanFormulaManager = formulaManager.getBooleanFormulaManager();
    pathFormulaManager =
        new PathFormulaManagerImpl(
            formulaManager,
            config,
            logger,
            shutdownNotifier,
            MACHINE_MODEL,
            Optional.empty(),
            AnalysisDirection.FORWARD,
            Language.C);
  }

  /**
   * Nondet-havocs the least significant byte of an int: the upper bytes must stay 0, the lowest
   * byte can be any value.
   */
  @Test
  public void testVerifierNondetMemorySingleByteOfInt() throws Exception {
    // given
    String programCode =
        """
        unsigned int x = 0;
        __VERIFIER_nondet_memory(&x, 1);
        unsigned int overLimit = (x > 255u);
        unsigned int freeByte = x & 255u;
        unsigned int upperBytes = x >> 8;
        """;

    // when
    PathFormula pf = createFormulaFromCode(programCode);

    // then ...
    BitvectorFormula overLimit = formulaForVariable(pf, "overLimit", INT_TYPE);
    BitvectorFormula freeByte = formulaForVariable(pf, "freeByte", INT_TYPE);
    BitvectorFormula upperBytes = formulaForVariable(pf, "upperBytes", INT_TYPE);

    BooleanFormula overLimitIsOne = makeEqual(overLimit, makeInt(1));
    BooleanFormula freeByteIsZero = makeEqual(freeByte, makeInt(0));
    BooleanFormula freeByteIs255 = makeEqual(freeByte, makeInt(255));
    BooleanFormula upperBytesMayBeNonZero = makeNotEqual(upperBytes, makeInt(0));

    // ... x never exceeds 255: only its lowest byte was set to a nondet value.
    assertUnsat(pf, overLimitIsOne);

    // ... the lowest byte is free to take either extreme value: 0 or 255.
    assertSat(pf, freeByteIsZero);
    assertSat(pf, freeByteIs255);

    // ... the upper 3 bytes stay pinned to their initial value: 0.
    assertUnsat(pf, upperBytesMayBeNonZero);
  }

  /**
   * Nondet-havocs 5 bytes over a 2-element {@code int} array: 1 full element ({@code arr[0]}) plus
   * a 1-byte remainder of {@code arr[1]}. Exercises the full-elements + tail-partial-element split.
   */
  @Test
  public void testVerifierNondetMemoryFullElementPlusRemainderOfArray() throws Exception {
    // given
    String programCode =
        """
        int arr[2] = {0, 0};
        __VERIFIER_nondet_memory(&arr[0], 5);
        unsigned int full = (unsigned int) arr[0];
        unsigned int overLimit = ((unsigned int) arr[1] > 255u);
        unsigned int freeByte = (unsigned int) arr[1] & 255u;
        unsigned int upperBytes = (unsigned int) arr[1] >> 8;
        """;

    // when
    PathFormula pf = createFormulaFromCode(programCode);

    // then ...
    BitvectorFormula full = formulaForVariable(pf, "full", INT_TYPE);
    BitvectorFormula overLimit = formulaForVariable(pf, "overLimit", INT_TYPE);
    BitvectorFormula freeByte = formulaForVariable(pf, "freeByte", INT_TYPE);
    BitvectorFormula upperBytes = formulaForVariable(pf, "upperBytes", INT_TYPE);

    BooleanFormula fullIsZero = makeEqual(full, makeInt(0));
    BooleanFormula fullIsMax = makeEqual(full, makeInt(0xFFFFFFFFL));
    BooleanFormula overLimitIsOne = makeEqual(overLimit, makeInt(1));
    BooleanFormula freeByteIsZero = makeEqual(freeByte, makeInt(0));
    BooleanFormula freeByteIs255 = makeEqual(freeByte, makeInt(255));
    BooleanFormula upperBytesMayBeNonZero = makeNotEqual(upperBytes, makeInt(0));

    // ... arr[1] never exceeds 255: only its lowest byte was set to a nondet value.
    assertUnsat(pf, overLimitIsOne);

    // ... arr[0] is free over its whole range
    assertSat(pf, fullIsZero);
    assertSat(pf, fullIsMax);

    // ... arr[1]'s lowest byte is free
    // to take either extreme value: 0 or 255.
    assertSat(pf, freeByteIsZero);
    assertSat(pf, freeByteIs255);

    // ... but arr[1]'s upper 3 bytes stay pinned to their initial value: 0.
    assertUnsat(pf, upperBytesMayBeNonZero);
  }

  /**
   * Nondet-havocs 5 bytes of a struct with padding between its members: covers the first member,
   * the padding, and only the first byte of the second member. Exercises byte-precise havocking
   * across struct padding.
   */
  @Test
  public void testVerifierNondetMemoryAcrossStructPadding() throws Exception {
    // given
    String programCode =
        """
        typedef struct { unsigned char a; int b; } P;
        P p = {0, 0};
        __VERIFIER_nondet_memory(&p, 5);
        unsigned char aVal = p.a;
        unsigned int overLimit = ((unsigned int) p.b > 255u);
        unsigned int freeByte = (unsigned int) p.b & 255u;
        unsigned int upperBytes = (unsigned int) p.b >> 8;
        """;

    // when
    PathFormula pf = createFormulaFromCode(programCode);

    // then ...
    BitvectorFormula aVal = formulaForVariable(pf, "aVal", CHAR_TYPE);
    BitvectorFormula overLimit = formulaForVariable(pf, "overLimit", INT_TYPE);
    BitvectorFormula freeByte = formulaForVariable(pf, "freeByte", INT_TYPE);
    BitvectorFormula upperBytes = formulaForVariable(pf, "upperBytes", INT_TYPE);

    BooleanFormula aValIsZero = makeEqual(aVal, formulaManager.makeNumber(CHAR_TYPE, 0));
    BooleanFormula aValIs255 = makeEqual(aVal, formulaManager.makeNumber(CHAR_TYPE, 255));
    BooleanFormula overLimitIsOne = makeEqual(overLimit, formulaManager.makeNumber(INT_TYPE, 1));
    BooleanFormula freeByteIsZero = makeEqual(freeByte, formulaManager.makeNumber(INT_TYPE, 0));
    BooleanFormula freeByteIs255 = makeEqual(freeByte, formulaManager.makeNumber(INT_TYPE, 255));
    BooleanFormula upperBytesMayBeNonZero =
        makeNotEqual(upperBytes, formulaManager.makeNumber(INT_TYPE, 0));

    // ... p.b never exceeds 255: only its lowest byte was set to a nondet value.
    assertUnsat(pf, overLimitIsOne);

    // ... p.a is free over its whole range
    assertSat(pf, aValIsZero);
    assertSat(pf, aValIs255);
    // ... p.b's lowest byte is free over its whole range
    assertSat(pf, freeByteIsZero);
    assertSat(pf, freeByteIs255);

    // ... p.b's upper 3 bytes stay pinned to their initial value: 0.
    assertUnsat(pf, upperBytesMayBeNonZero);
  }

  /**
   * Regression test for {@code AssignmentHandler#generatePartialAssignmentsForArrayType}'s
   * byte-repeat exemption: a {@code memset} of a whole struct must still zero an array field that
   * isn't the struct's first member. Before the fix, the field's non-zero byte offset made the
   * array-decomposition guards silently drop the assignment to it, leaving its old value in place.
   */
  @Test
  public void testMemsetSetsArrayFieldThatIsNotFirstMember1() throws Exception {
    // given
    String programCode =
        """
        typedef struct { int tag; unsigned char data[4]; } Rec;
        Rec r;
        r.tag = 0;
        r.data[0] = 0;
        r.data[1] = 0;
        r.data[2] = 42;
        r.data[3] = 0;
        memset(&r, 0, sizeof(r));
        unsigned char data2 = r.data[2];
        """;

    // when
    PathFormula pf = createFormulaFromCode(programCode);

    // then ...
    BitvectorFormula data2 = formulaForVariable(pf, "data2", CHAR_TYPE);
    BooleanFormula data2NonZero = makeNotEqual(data2, formulaManager.makeNumber(CHAR_TYPE, 0));

    // ... data[2] was overwritten by the memset: it must be 0.
    assertUnsat(pf, data2NonZero);
  }

  @Test
  public void testMemsetSetsArrayFieldThatIsNotFirstMember2() throws Exception {
    // given
    String programCode =
        """
        typedef struct { int tag; unsigned char data[4]; } Rec;
        Rec r;
        r.tag = 0;
        r.data[0] = 0;
        r.data[1] = 0;
        r.data[2] = 0;
        r.data[3] = 0;
        memset(&r, 66, sizeof(r));
        unsigned char data2 = r.data[2];
        """;

    // when
    PathFormula pf = createFormulaFromCode(programCode);

    // then ...
    BitvectorFormula data2 = formulaForVariable(pf, "data2", CHAR_TYPE);
    BooleanFormula data2NotSetByte = makeNotEqual(data2, formulaManager.makeNumber(CHAR_TYPE, 66));

    // ... data[2] must be 66.
    assertUnsat(pf, data2NotSetByte);
  }

  @Test
  public void testMemsetSetsArrayFieldThatIsNotFirstMemberAndNested() throws Exception {
    // given
    String program =
        """
        typedef struct { unsigned char data[4]; } Inner;
        typedef struct { int tag; Inner in; } Outer;
        int main() {
          Outer o;
          o.tag = 0;
          o.in.data[0] = 0;
          o.in.data[1] = 0;
          o.in.data[2] = 42;
          o.in.data[3] = 0;
          memset(&o, 21, sizeof(o));
          unsigned char data2 = o.in.data[2];
        }
        """;

    // when
    PathFormula pf = createFormulaFromProgram(program);

    // then
    BitvectorFormula data2 = formulaForVariable(pf, "data2", CHAR_TYPE);
    BooleanFormula data2Non21 = makeNotEqual(data2, formulaManager.makeNumber(CHAR_TYPE, 21));

    // ... the nested array field was reached and set
    assertUnsat(pf, data2Non21);
  }

  @Test
  public void testMemcpySameTypeArrayField() throws Exception {
    // given
    String programCode =
        """
        typedef struct { int tag; unsigned char data[4]; } Rec;
        Rec src;
        Rec dst;
        src.tag = 0;
        src.data[0] = 0;
        src.data[1] = 0;
        src.data[2] = 15;
        src.data[3] = 0;
        dst.tag = 0;
        dst.data[0] = 0;
        dst.data[1] = 0;
        dst.data[2] = 42;
        dst.data[3] = 0;
        memcpy(&dst, &src, sizeof(Rec));
        unsigned char data2 = dst.data[2];
        """;

    // when
    PathFormula pf = createFormulaFromCode(programCode);

    // then ...
    BitvectorFormula data2 = formulaForVariable(pf, "data2", CHAR_TYPE);
    BooleanFormula data2Non15 = makeNotEqual(data2, formulaManager.makeNumber(CHAR_TYPE, 15));

    // ... dst.data[2] was overwritten with src.data[2]: Must be 15.
    assertUnsat(pf, data2Non15);
  }

  private PathFormula createFormulaFromCode(String cCode) throws Exception {
    return TestCfaUtils.toPathFormula(
        TestCfaUtils.makeCfaFromFunctionBody(cCode),
        SSAMap.emptySSAMap(),
        pathFormulaManager,
        false);
  }

  /**
   * Like {@link #createFormulaFromCode}, but for a full program instead of just a function body.
   */
  private PathFormula createFormulaFromProgram(String cProgram) throws Exception {
    return TestCfaUtils.toPathFormula(
        TestCfaUtils.makeCfaFromString(cProgram), SSAMap.emptySSAMap(), pathFormulaManager, false);
  }

  /**
   * Returns the formula for the current SSA-indexed value of a plain (non-address-taken) local
   * variable of function main, as declared in code passed to {@link #createFormulaFromCode}.
   *
   * <p>This only works for variables that never have their address taken: those are encoded
   * directly as simple SSA-indexed formula variables. A variable whose address is taken (e.g. the
   * targets of {@code __VERIFIER_nondet_memory} in these tests) is instead represented via the
   * heap/pointer-target-set encoding, so its value cannot be referenced this way; read it out into
   * a plain local variable first (as the test-snippets above do) if you want to assert on it.
   */
  private <T extends Formula> T formulaForVariable(
      PathFormula pf, String name, FormulaType<T> type) {
    String qualifiedName = "main::" + name;
    int index = pf.getSsa().getIndex(qualifiedName);
    return formulaManager.makeVariable(type, qualifiedName, index);
  }

  private <T extends Formula> BooleanFormula makeEqual(T formula1, T formula2) {
    return formulaManager.makeEqual(formula1, formula2);
  }

  private <T extends Formula> BooleanFormula makeNotEqual(T formula1, T formula2) {
    return booleanFormulaManager.not(formulaManager.makeEqual(formula1, formula2));
  }

  private Formula makeInt(long value) {
    return formulaManager.makeNumber(INT_TYPE, value);
  }

  private BooleanFormula makeAnd(PathFormula pf, BooleanFormula formula) {
    return booleanFormulaManager.and(pf.getFormula(), formula);
  }

  private void assertSat(PathFormula pf, BooleanFormula formula) throws Exception {
    assertThat(solver.isUnsat(makeAnd(pf, formula))).isFalse();
  }

  private void assertUnsat(PathFormula pf, BooleanFormula formula) throws Exception {
    assertThat(solver.isUnsat(makeAnd(pf, formula))).isTrue();
  }
}
