// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Objects;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslEnsures;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslFunctionContract;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopInvariant;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslRequires;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AcslParseException;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

public class AcslAnnotationParsingTest {

  private CSimpleType basicInt() {
    return new CSimpleType(
        CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);
  }

  private CProgramScope getCProgramScope() {
    String currentFunctionName = "f";

    CProgramScope scope =
        CProgramScope.mutableCoy(CProgramScope.empty().withFunctionScope(currentFunctionName));
    for (String var : ImmutableList.of("x", "y", "z")) {
      scope.registerDeclaration(
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.AUTO,
              basicInt(),
              var,
              var,
              var,
              null /* No initializer, we only want it for testing */));
    }
    scope.registerDeclaration(
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            new CFunctionType(basicInt(), ImmutableList.of(), false),
            currentFunctionName,
            ImmutableList.of(),
            ImmutableSet.of()));

    return scope;
  }

  private AcslScope getAcslScope() {
    AcslScope scope = AcslScope.empty();

    return scope;
  }

  @Test
  public void parseAcslAnnotationAssertionTest() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    String input = "//@ assert x == 10;";

    AcslAssertion expected = getAssertion();

    AcslAssertion parsed =
        (AcslAssertion)
            AcslParser.parseAcslComment(input, FileLocation.DUMMY, cProgramScope, getAcslScope());
    assertThat(parsed).isEqualTo(expected);
  }

  @Test
  public void parseAssertionTest() throws AcslParseException {
    String input = "assert x == 10;";

    AcslAssertion expected = getAssertion();
    AAcslAnnotation parsed =
        AcslParser.parseAcslStatement(
            input, FileLocation.DUMMY, getCProgramScope(), getAcslScope());
    assertThat(parsed).isEqualTo(expected);
  }

  @Test
  public void parseLoopInvariantTest() throws AcslParseException {
    String input = "loop invariant x <= 10;";

    AcslLoopInvariant expected = getLoopInvariant();
    AAcslAnnotation parsed =
        AcslParser.parseAcslStatement(
            input, FileLocation.DUMMY, getCProgramScope(), getAcslScope());
    assertThat(parsed).isEqualTo(expected);
  }

  @Test
  public void parseEnsuresTest() throws AcslParseException {
    String input = "ensures x <= 10;";
    AcslEnsures expected = getEnsures();
    AAcslAnnotation parsed =
        AcslParser.parseAcslStatement(
            input, FileLocation.DUMMY, getCProgramScope(), getAcslScope());
    assertThat(parsed).isEqualTo(expected);
  }

  @Test
  public void parseRequiresTest() throws AcslParseException {
    String input = "requires x == 10;";
    AcslRequires expected = getRequires();
    AAcslAnnotation parsed =
        AcslParser.parseAcslStatement(
            input, FileLocation.DUMMY, getCProgramScope(), getAcslScope());
    assertThat(parsed).isEqualTo(expected);
  }

  @Test
  public void parseFunctionContractTest() throws AcslParseException {
    String input = "/*@ requires x == 10; ensures x <= 10;*/";
    AcslFunctionContract expected = getFunctionContract();
    AAcslAnnotation parsed =
        AcslParser.parseAcslComment(input, FileLocation.DUMMY, getCProgramScope(), getAcslScope());
    assertThat(parsed).isEqualTo(expected);
  }

  @Test
  public void parseLoodAnnotationTest() throws AcslParseException {
    String input = "loop invariant x <= 10;";
    AcslLoopAnnotation expected = getLoopAnnotation();
    AAcslAnnotation parsed =
        AcslParser.parseAcslComment(input, FileLocation.DUMMY, getCProgramScope(), getAcslScope());
    assertThat(parsed).isEqualTo(expected);
  }

  private AcslAssertion getAssertion() {
    CProgramScope cProgramScope = getCProgramScope();
    return new AcslAssertion(
        FileLocation.DUMMY,
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslIdTerm(
                FileLocation.DUMMY,
                new AcslCVariableDeclaration(
                    (CVariableDeclaration)
                        Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TEN),
            AcslBinaryTermExpressionOperator.EQUALS));
  }

  private AcslLoopInvariant getLoopInvariant() {
    CProgramScope cProgramScope = getCProgramScope();
    return new AcslLoopInvariant(
        FileLocation.DUMMY,
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslIdTerm(
                FileLocation.DUMMY,
                new AcslCVariableDeclaration(
                    (CVariableDeclaration)
                        Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TEN),
            AcslBinaryTermExpressionOperator.LESS_EQUAL));
  }

  private AcslEnsures getEnsures() {
    CProgramScope cProgramScope = getCProgramScope();
    return new AcslEnsures(
        FileLocation.DUMMY,
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslIdTerm(
                FileLocation.DUMMY,
                new AcslCVariableDeclaration(
                    (CVariableDeclaration)
                        Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TEN),
            AcslBinaryTermExpressionOperator.LESS_EQUAL));
  }

  private AcslRequires getRequires() {
    CProgramScope cProgramScope = getCProgramScope();
    return new AcslRequires(
        FileLocation.DUMMY,
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslIdTerm(
                FileLocation.DUMMY,
                new AcslCVariableDeclaration(
                    (CVariableDeclaration)
                        Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TEN),
            AcslBinaryTermExpressionOperator.EQUALS));
  }

  private AcslFunctionContract getFunctionContract() {
    return new AcslFunctionContract(
        FileLocation.DUMMY,
        ImmutableSet.of(getEnsures()),
        ImmutableSet.of(),
        ImmutableSet.of(getRequires()));
  }

  private AcslLoopAnnotation getLoopAnnotation() {
    return new AcslLoopAnnotation(FileLocation.DUMMY, ImmutableSet.of(getLoopInvariant()));
  }

  @Test
  public void testStripCommentMarker() {
    String lineComment = "//@ assert a == 20;";
    String lineCommentExpected = "assert a == 20;";
    String lineCommentStripped = AcslParser.stripCommentMarker(lineComment);
    assertThat(lineCommentStripped).isEqualTo(lineCommentExpected);
  }

  @Test
  public void testStripBlockComment() {
    String blockComment =
        """
        /*@
        ensures x = 0;
        assumes /true;
        ensures !(x < 0);
        */\
        """;
    String blockCommentExpected =
        """
        ensures x = 0;
        assumes /true;
        ensures !(x < 0);
        """;

    String blockCommentStripped = AcslParser.stripCommentMarker(blockComment);
    assertThat(blockCommentStripped).isEqualTo(blockCommentExpected);
  }
}
