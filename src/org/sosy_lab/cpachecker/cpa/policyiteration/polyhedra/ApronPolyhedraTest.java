package org.sosy_lab.cpachecker.cpa.policyiteration.polyhedra;

import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
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
import org.sosy_lab.cpachecker.cpa.policyiteration.Template;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import apron.Abstract1;
import apron.Environment;
import apron.SetUp;

public class ApronPolyhedraTest {
  static {
    SetUp.init("lib/native/x86_64-linux/apron/");
  }

  private PolyhedraWideningManager pwm;
  private LogManager logger;

  @Before public void setUp() throws InvalidConfigurationException {
    Handler h = new StreamHandler(System.out, new SimpleFormatter());
    Configuration config = TestDataTools.configurationForTest().build();
    logger = new BasicLogManager(config, h);

    PolicyIterationStatistics stats = new PolicyIterationStatistics(config);

    pwm = new PolyhedraWideningManager(stats, logger);
  }

  @Test public void test_polyhedra() {
    CIdExpression x, y;
    x = makeVar("x", CNumericTypes.INT);
    y = makeVar("y", CNumericTypes.INT);

    // Point 1: (x=0 /\ y=0).
    Map<Template, Rational> point1, point2;
    LinearExpression<CIdExpression> linX = LinearExpression.ofVariable(x);
    LinearExpression<CIdExpression> linY = LinearExpression.ofVariable(y);

    point1 = ImmutableMap.of(
      Template.of(linX), Rational.ZERO,
      Template.of(linX.negate()), Rational.ZERO,
      Template.of(linY), Rational.ZERO,
      Template.of(linY.negate()), Rational.ZERO
    );

    // Point 2: (x=1 /\ y=1).
    point2 = ImmutableMap.of(
      Template.of(linX), Rational.ONE,
      Template.of(linX.negate()), Rational.NEG_ONE,
      Template.of(linY), Rational.ONE,
      Template.of(linY.negate()), Rational.NEG_ONE
    );

    Abstract1 abs1, abs2, widened, union;

    Environment env = pwm.generateEnvironment(ImmutableList.of(point1, point2));

    abs1 = pwm.fromTemplates(env, point1);
    logger.log(Level.INFO, "Abs1 = ", abs1);

    abs2 = pwm.fromTemplates(env, point2);
    logger.log(Level.INFO, "Abs2 = ", abs2);

    union = abs1.joinCopy(pwm.getManager(), abs2);
    logger.log(Level.INFO, "Join = ", union);
    logger.log(Level.INFO, pwm.toTemplates(union));

    widened = abs1.widening(pwm.getManager(), union);
    logger.log(Level.INFO, "Widened = ", widened);
    logger.log(Level.INFO, "Widened to templates = ", pwm.toTemplates(widened));
    logger.flush();
  }

  private CIdExpression makeVar(String varName, CSimpleType type) {
    return new CIdExpression(FileLocation.DUMMY,
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            type,
            varName, varName, varName, null
        ));
  }
}
