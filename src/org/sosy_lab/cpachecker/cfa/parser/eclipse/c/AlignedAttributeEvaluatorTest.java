// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import org.eclipse.cdt.core.dom.ast.IASTAttribute;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.parser.Parsers.EclipseCParserOptions;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * Tests for {@link AlignedAttributeEvaluator}. The tokens are created by CDT, and the arguments are
 * the forms used in SV-Benchmarks.
 */
public class AlignedAttributeEvaluatorTest {

  private static final ImmutableMap<String, CType> TYPEDEFS =
      ImmutableMap.of(
          "kernel_ulong_t", CNumericTypes.UNSIGNED_LONG_INT, "__u32", CNumericTypes.UNSIGNED_INT);

  private static EclipseCdtWrapper cdt;

  @BeforeClass
  public static void setupParser() throws InvalidConfigurationException {
    EclipseCParserOptions options = new EclipseCParserOptions();
    TestUtils.configurationForTest().build().recursiveInject(options);
    cdt = new EclipseCdtWrapper(options, MachineModel.LINUX64, ShutdownNotifier.createDummy());
  }

  private static int evaluate(MachineModel pMachineModel, String pArgument)
      throws CoreException, InterruptedException {
    Path file = Path.of("test.c");
    String code = "int x __attribute__((aligned(" + pArgument + ")));";
    IASTTranslationUnit ast = cdt.getASTTranslationUnit(cdt.wrapCode(file, code));
    IASTSimpleDeclaration declaration = (IASTSimpleDeclaration) ast.getDeclarations()[0];
    IASTAttribute attribute = declaration.getDeclarators()[0].getAttributes()[0];
    CSourceOriginMapping sourceOriginMapping = new CSourceOriginMapping();
    sourceOriginMapping.addFileInformation(file, code);
    AlignedAttributeEvaluator evaluator =
        new AlignedAttributeEvaluator(
            pMachineModel,
            new ParseContext(Functions.identity(), sourceOriginMapping),
            TYPEDEFS::get);
    return evaluator
        .evaluate(AlignedAttributeEvaluator.getTokens(attribute.getArgumentClause()), attribute)
        .intValueExact();
  }

  private static int evaluate(String pArgument) throws CoreException, InterruptedException {
    return evaluate(MachineModel.LINUX64, pArgument);
  }

  @Test
  public void testIntegerLiterals() throws CoreException, InterruptedException {
    assertThat(evaluate("16")).isEqualTo(16);
    assertThat(evaluate("0x10")).isEqualTo(16);
    assertThat(evaluate("(4 * 0x400ULL)")).isEqualTo(4096);
  }

  @Test
  public void testOperators() throws CoreException, InterruptedException {
    assertThat(evaluate("(1) << (6)")).isEqualTo(64);
    assertThat(evaluate("1 << 12")).isEqualTo(4096);
    assertThat(evaluate("256 >> 4")).isEqualTo(16);
    assertThat(evaluate("64/8")).isEqualTo(8);
    assertThat(evaluate("19 % 11")).isEqualTo(8);
    assertThat(evaluate("20 - 4 - 8")).isEqualTo(8);
  }

  @Test
  public void testPrecedence() throws CoreException, InterruptedException {
    assertThat(evaluate("2 + 3 * 2")).isEqualTo(8);
    assertThat(evaluate("(1 + 1) * 4")).isEqualTo(8);
    assertThat(evaluate("1 << 2 + 1")).isEqualTo(8);
    assertThat(evaluate("(1 << 2) + 4")).isEqualTo(8);
  }

  @Test
  public void testSizeof() throws CoreException, InterruptedException {
    assertThat(evaluate("sizeof(long)")).isEqualTo(8);
    assertThat(evaluate("sizeof(unsigned long )")).isEqualTo(8);
    assertThat(evaluate("(2) * (sizeof(unsigned long ))")).isEqualTo(16);
    assertThat(evaluate("sizeof(void *)")).isEqualTo(8);
    assertThat(evaluate("sizeof(short int)")).isEqualTo(2);
  }

  @Test
  public void testAlignof() throws CoreException, InterruptedException {
    assertThat(evaluate("__alignof__(long long)")).isEqualTo(8);
    assertThat(evaluate("__alignof__(long double)")).isEqualTo(16);
    assertThat(evaluate("__alignof__(unsigned long long )")).isEqualTo(8);
    assertThat(evaluate("__alignof(__float128)")).isEqualTo(16);
    assertThat(evaluate("(__alignof__ (struct sockaddr *))")).isEqualTo(8);
  }

  @Test
  public void testTypedefNames() throws CoreException, InterruptedException {
    assertThat(evaluate("sizeof(kernel_ulong_t)")).isEqualTo(8);
    assertThat(evaluate("2 * sizeof(kernel_ulong_t)")).isEqualTo(16);
    assertThat(evaluate("sizeof(__u32 )")).isEqualTo(4);
  }

  @Test
  public void testMachineModel() throws CoreException, InterruptedException {
    assertThat(evaluate(MachineModel.LINUX32, "2 * sizeof(unsigned long)")).isEqualTo(8);
    assertThat(evaluate(MachineModel.LINUX32, "sizeof(void *)")).isEqualTo(4);
    assertThat(evaluate(MachineModel.LINUX32, "sizeof(kernel_ulong_t)")).isEqualTo(4);
  }

  @Test
  public void testUnsupported() {
    for (String argument :
        ImmutableList.of(
            "x", "-1", "1 2", "1 +", "sizeof long", "sizeof(x)", "sizeof(struct S)", "sizeof(1)")) {
      assertThrows(argument, CFAGenerationRuntimeException.class, () -> evaluate(argument));
    }
  }
}
