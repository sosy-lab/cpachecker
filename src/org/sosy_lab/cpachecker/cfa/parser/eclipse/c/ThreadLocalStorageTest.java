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

import java.nio.file.Path;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Parsers;
import org.sosy_lab.cpachecker.cfa.parser.Parsers.EclipseCParserOptions;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/** Tests for parsing {@code __thread} and {@code _Thread_local}. */
public class ThreadLocalStorageTest {

  private static CParser parser;

  @BeforeClass
  public static void setupParser() throws InvalidConfigurationException {
    EclipseCParserOptions options = new EclipseCParserOptions();
    TestUtils.configurationForTest().build().recursiveInject(options);
    parser =
        Parsers.getCParser(
            LogManager.createTestLogManager(),
            options,
            MachineModel.LINUX64,
            ShutdownNotifier.createDummy());
  }

  private static ParseResult parse(String pCode) throws CParserException, InterruptedException {
    return parser.parseString(Path.of("dummy"), pCode);
  }

  private static CVariableDeclaration parseGlobalVariable(String pCode, String pName)
      throws CParserException, InterruptedException {
    for (var declaration : parse(pCode).globalDeclarations()) {
      ADeclaration decl = declaration.getFirst();
      if (decl instanceof CVariableDeclaration varDecl && varDecl.getOrigName().equals(pName)) {
        return varDecl;
      }
    }
    throw new AssertionError("No global variable " + pName + " in " + pCode);
  }

  @Test
  public void testThreadLocalGlobal() throws CParserException, InterruptedException {
    assertThat(parseGlobalVariable("__thread int x;", "x").hasThreadLocalStorage()).isTrue();
    assertThat(parseGlobalVariable("_Thread_local int x;", "x").hasThreadLocalStorage()).isTrue();
    assertThat(parseGlobalVariable("static _Thread_local int x;", "x").hasThreadLocalStorage())
        .isTrue();
    assertThat(parseGlobalVariable("extern __thread int x;", "x").hasThreadLocalStorage()).isTrue();
    assertThat(parseGlobalVariable("int x;", "x").hasThreadLocalStorage()).isFalse();
  }

  @Test
  public void testThreadLocalMultipleDeclarators() throws CParserException, InterruptedException {
    String code = "__thread int x, *y; int z;";
    assertThat(parseGlobalVariable(code, "x").hasThreadLocalStorage()).isTrue();
    assertThat(parseGlobalVariable(code, "y").hasThreadLocalStorage()).isTrue();
    assertThat(parseGlobalVariable(code, "z").hasThreadLocalStorage()).isFalse();
  }

  @Test
  public void testThreadLocalToASTString() throws CParserException, InterruptedException {
    for (String code : new String[] {"_Thread_local int x = 1;", "extern _Thread_local int x;"}) {
      CVariableDeclaration decl = parseGlobalVariable(code, "x");
      assertThat(decl.toASTString()).isEqualTo(code);
      // the output can be parsed again
      assertThat(parseGlobalVariable(decl.toASTString(), "x").hasThreadLocalStorage()).isTrue();
    }
    assertThat(parseGlobalVariable("__thread int x;", "x").toASTString())
        .isEqualTo("_Thread_local int x;");
    assertThat(parseGlobalVariable("int x;", "x").toASTString()).isEqualTo("int x;");
  }

  @Test
  public void testThreadLocalStaticLocal() throws CParserException, InterruptedException {
    // block-scope static variables are allowed to be thread-local
    parse("void f(void) { static _Thread_local int x; }");
  }

  @Test
  public void testThreadLocalInUnsupportedLocations() {
    for (String code :
        new String[] {
          "typedef __thread int t;",
          "__thread int f(void);",
          "__thread int f(void) { return 0; }",
          "void f(__thread int p);",
          "struct s { __thread int m; };",
          "__thread struct s { int m; };",
          "void f(void) { _Thread_local int x; }",
          "int f(void) { return sizeof(__thread int); }",
          "int x = (__thread int) 0;",
          // CDT drops the attribute here, only the check for unhandled occurrences notices it
          "int * __thread p;",
        }) {
      assertThrows(code, CParserException.class, () -> parse(code));
    }
  }
}
