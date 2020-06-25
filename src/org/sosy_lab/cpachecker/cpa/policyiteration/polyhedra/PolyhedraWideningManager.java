package org.sosy_lab.cpachecker.cpa.policyiteration.polyhedra;

import apron.Abstract1;
import apron.Coeff;
import apron.Environment;
import apron.Lincons1;
import apron.Linexpr1;
import apron.Linterm1;
import apron.Manager;
import apron.MpqScalar;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.LinearExpression;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.policyiteration.PolicyAbstractedState;
import org.sosy_lab.cpachecker.cpa.policyiteration.PolicyBound;
import org.sosy_lab.cpachecker.cpa.policyiteration.PolicyIterationStatistics;
import org.sosy_lab.cpachecker.util.ApronManager;
import org.sosy_lab.cpachecker.util.ApronManager.AbstractDomain;
import org.sosy_lab.cpachecker.util.templates.Template;

public class PolyhedraWideningManager {

  private final Manager manager;
  private final Map<String, CIdExpression> types;
  private final PolicyIterationStatistics statistics;
  private final LogManager logger;

  public PolyhedraWideningManager(PolicyIterationStatistics pStatistics,
      LogManager pLogger) {
    statistics = pStatistics;
    logger = pLogger;
    ApronManager apronManager = new ApronManager(AbstractDomain.POLKA);

    manager = apronManager.getManager();
    types = new HashMap<>();
  }

  Manager getManager() {
    return manager;
  }

  /**
   * Generate new directions which result from the widening operation applied on two polyhedra
   * described by {@code oldState} and {@code newState}.
   */
  public Set<Template> generateWideningTemplates(
      PolicyAbstractedState oldState,
      PolicyAbstractedState newState) {

    Set<Template> allTemplates = Sets.union(oldState.getAbstraction().keySet(),
        newState.getAbstraction().keySet());
    Map<Template, Rational> oldData = Maps.transformValues(oldState.getAbstraction(),
        PolicyBound::getBound);
    Map<Template, Rational> newData = Maps.transformValues(
        newState.getAbstraction(),
        PolicyBound::getBound);

    Abstract1 widened;
    try {
      statistics.polyhedraWideningTimer.start();
      Abstract1 abs1, abs2;
      Environment env = generateEnvironment(ImmutableList.of(oldData, newData));
      abs1 = fromTemplates(env, oldData);
      abs2 = fromTemplates(env, newData);
      abs2.join(manager, abs1);
      widened = abs1.widening(manager, abs2);
    } finally {
      statistics.polyhedraWideningTimer.stop();
    }

    Map<Template, Rational> generated = toTemplates(widened);
    logger.log(Level.FINE, "Generated templates", generated);
    Set<Template> diff = Sets.difference(generated.keySet(), allTemplates);
    Set<Template> out = new HashSet<>();

    for (Template t : diff) {
      out.add(t);
      out.add(Template.of(t.getLinearExpression().negate()));
    }
    return out;
  }

  Environment generateEnvironment(List<Map<Template, Rational>> t) {
    Environment out = new Environment();
    for (Map<Template, Rational> m : t) {
      for (Template template : m.keySet()) {
        out = generateEnvironment(out, template);
      }
    }
    return out;
  }

  Map<Template, Rational> toTemplates(Abstract1 abs) {
    Map<Template, Rational> out = new HashMap<>();
    Lincons1[] values = abs.toLincons(manager);
    for (Lincons1 constraint : values) {
      // We have: t + coeff >= 0.
      Rational coeff = ofCoeff(constraint.getCst());
      Template t = ofExpression(constraint);

      // We want: -t <= coeff.
      Template tNegated = Template.of(t.getLinearExpression().negate());

      out.put(tNegated, coeff);
    }
    return out;
  }

  /**
   * Intersection of all linear constraints.
   */
  Abstract1 fromTemplates(
      Environment environment,
      Map<Template, Rational> state) {

    Lincons1[] values = new Lincons1[state.size()];
    int i = -1;
    for (Entry<Template, Rational> e : state.entrySet()) {
      Template t = e.getKey();
      Rational bound = e.getValue();
      values[++i] = fromTemplateBound(environment, t, bound);
    }
    Abstract1 out = new Abstract1(manager, environment);
    out.meet(manager, values);
    return out;
  }


  private Template ofExpression(Lincons1 expr) {
    expr.minimize();
    LinearExpression<CIdExpression> out = LinearExpression.empty();

    for (Linterm1 term : expr.getLinterms()) {
      String varName = term.getVariable();
      Rational coeff = ofCoeff(term.getCoefficient());

      out = out.add(LinearExpression.monomial(types.get(varName),  coeff));
    }

    return Template.of(out);
  }

  private Rational ofCoeff(Coeff c) {
    assert c instanceof MpqScalar;
    MpqScalar mpq = (MpqScalar) c;
    return Rational.of(mpq.get().getNum().bigIntegerValue(),
        mpq.get().getDen().bigIntegerValue());
  }

  private Lincons1 fromTemplateBound(
      Environment environment,
      Template t, Rational bound) {
    // APRON supports only ">= 0".
    // We are getting from "t <= x" to "-t + x >= 0"
    Linexpr1 expr = fromLinearExpression(
        environment,
        t.getLinearExpression().negate());
    expr.setCst(ofRational(bound));
    return new Lincons1(Lincons1.SUPEQ, expr);
  }

  private Linexpr1 fromLinearExpression(
      Environment environment,
      LinearExpression<CIdExpression> input) {
    Linexpr1 expr = new Linexpr1(environment, input.size());
    expr.setCst(ofRational(Rational.ZERO));

    for (Entry<CIdExpression, Rational> e : input) {
      CIdExpression id = e.getKey();
      Rational bound = e.getValue();

      String varName = id.getDeclaration().getQualifiedName();
      types.put(varName, id);
      expr.setCoeff(varName, ofRational(bound));
    }
    return expr;
  }


  private Environment generateEnvironment(Environment environment, Template t) {
    for (Entry<CIdExpression, Rational> e : t.getLinearExpression()) {
      CIdExpression id = e.getKey();
      String varName = id.getDeclaration().getQualifiedName();
      types.put(varName, id);
      if (!environment.hasVar(varName)) {
        if (isIntegral(id)) {
          environment = environment.add(new String[]{varName}, new String[]{});
        } else {
          environment = environment.add(new String[]{}, new String[]{varName});
        }
      }
    }
    return environment;
  }

  private MpqScalar ofRational(Rational r) {
    return new MpqScalar(r.getNum(), r.getDen());
  }

  private boolean isIntegral(CIdExpression id) {
    CSimpleType type = (CSimpleType) id.getExpressionType();
    return type.getType().isIntegerType();
  }
}
