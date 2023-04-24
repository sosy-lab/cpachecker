// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.policyiteration.polyhedra;

import static com.google.common.truth.Truth.assertThat;

import apron.Abstract1;
import apron.ApronException;
import apron.Environment;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.LinearExpression;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cpa.policyiteration.PolicyIterationStatistics;
import org.sosy_lab.cpachecker.util.templates.Template;

public class PolyhedraWideningManagerTest {

  private PolyhedraWideningManager pwm;

  @Before
  public void setUp() {
    PolicyIterationStatistics stats = Mockito.mock(PolicyIterationStatistics.class);
    pwm = new PolyhedraWideningManager(stats, LogManager.createTestLogManager());

    try {
      pwm = new PolyhedraWideningManager(stats, LogManager.createTestLogManager());
    } catch (UnsatisfiedLinkError e) {
      // assumeNoException("missing binary dependency for old apron binary", e);
      throw new AssertionError(e);
    }
  }

  @Test
  public void test_polyhedra() throws ApronException {
    // FIXME Tests should not rely on a user manually checking log message
    // but instead use proper assertions, otherwise they are useless as regression tests.
    CIdExpression x = makeVar("x", CNumericTypes.INT);
    CIdExpression y = makeVar("y", CNumericTypes.INT);

    // Point 1: (x=0 /\ y=0).
    LinearExpression<CIdExpression> linX = LinearExpression.ofVariable(x);
    LinearExpression<CIdExpression> linY = LinearExpression.ofVariable(y);

    Map<Template, Rational> point1 =
        ImmutableMap.of(
            Template.of(linX), Rational.ZERO,
            Template.of(linX.negate()), Rational.ZERO,
            Template.of(linY), Rational.ZERO,
            Template.of(linY.negate()), Rational.ZERO);

    // Point 2: (x=1 /\ y=1).
    Map<Template, Rational> point2 =
        ImmutableMap.of(
            Template.of(linX), Rational.ONE,
            Template.of(linX.negate()), Rational.NEG_ONE,
            Template.of(linY), Rational.ONE,
            Template.of(linY.negate()), Rational.NEG_ONE);

    Environment env = pwm.generateEnvironment(ImmutableList.of(point1, point2));

    Abstract1 abs1 = pwm.fromTemplates(env, point1);
    Abstract1 abs2 = pwm.fromTemplates(env, point2);

    Abstract1 union = abs1.joinCopy(pwm.getManager(), abs2);

    Map<Template, Rational> unionMap = pwm.toTemplates(union);
    assertThat(unionMap).containsEntry(Template.of(linX), Rational.ONE);
    assertThat(unionMap).containsEntry(Template.of(linX.negate()), Rational.ZERO);
    assertThat(unionMap).containsEntry(Template.of(linX.sub(linY)), Rational.ZERO);

    Abstract1 widened = abs1.widening(pwm.getManager(), union);
    Map<Template, Rational> widenedMap = pwm.toTemplates(widened);
    assertThat(widenedMap).containsEntry(Template.of(linX.sub(linY)), Rational.ZERO);
  }

  private CIdExpression makeVar(String varName, CSimpleType type) {
    return new CIdExpression(
        FileLocation.DUMMY,
        new CVariableDeclaration(
            FileLocation.DUMMY, false, CStorageClass.AUTO, type, varName, varName, varName, null));
  }
}
