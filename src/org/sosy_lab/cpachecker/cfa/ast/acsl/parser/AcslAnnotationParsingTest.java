// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

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

    String annotation = AcslParser.stripCommentMarker(input);
    AcslAssertion parsed =
        (AcslAssertion)
            AcslParser.parseSingleAcslStatement(
                annotation, FileLocation.DUMMY, cProgramScope, getAcslScope());
    assert expected.equals(parsed);
  }

  @Test
  public void parseAcslCommentTest() throws AcslParseException {
    String input =
"""
/*@
assert x == 10;
loop invariant x <= 10;
ensures x <= 10;
requires x == 10;
*/\
""";
    ImmutableList<AAcslAnnotation> expected =
        ImmutableList.of(getAssertion(), getLoopInvariant(), getEnsures(), getRequires());
    ImmutableList<AAcslAnnotation> parsed =
        AcslParser.parseAcslComment(input, FileLocation.DUMMY, getCProgramScope(), getAcslScope());
    assert expected.equals(parsed);
  }

  @Test
  public void parseAssertionTest() throws AcslParseException {
    String input = "assert x == 10;";

    AcslAssertion expected = getAssertion();
    AcslAssertion parsed =
        AcslParser.parseAcslAssertion(
            input, FileLocation.DUMMY, getCProgramScope(), getAcslScope());
    assert expected.equals(parsed);
  }

  @Test
  public void parseLoopInvariantTest() throws AcslParseException {
    String input = "loop invariant x <= 10;";

    AcslLoopInvariant expected = getLoopInvariant();
    AcslLoopInvariant parsed =
        AcslParser.parseAcslLoopInvariant(
            input, FileLocation.DUMMY, getCProgramScope(), getAcslScope());
    assert expected.equals(parsed);
  }

  @Test
  public void parseEnsuresTest() throws AcslParseException {
    String input = "ensures x <= 10;";
    AcslEnsures expected = getEnsures();
    AcslEnsures parsed =
        AcslParser.parseAcslEnsures(input, FileLocation.DUMMY, getCProgramScope(), getAcslScope());
    assert expected.equals(parsed);
  }

  @Test
  public void parseRequiresTest() throws AcslParseException {
    String input = "requires x == 10;";
    AcslRequires expected = getRequires();
    AcslRequires parsed =
        AcslParser.parseAcslRequires(input, FileLocation.DUMMY, getCProgramScope(), getAcslScope());
    assert expected.equals(parsed);
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

  @Test
  public void testStripCommentMarker() {
    String lineComment = "//@ assert a == 20;";
    String lineCommentExpected = "assert a == 20;";
    String lineCommentStripped = AcslParser.stripCommentMarker(lineComment);
    assert lineCommentStripped.equals(lineCommentExpected);
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
    assert blockCommentStripped.equals(blockCommentExpected);
  }
}
