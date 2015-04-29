package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@Options(prefix="cpa.stator.policy")
public class TemplateManager {
  @Option(secure=true,
      description="Generate templates for the lower bounds of each variable")
  private boolean generateLowerBound = true;

  @Option(secure=true,
      description="Generate templates for the upper bounds of each variable")
  private boolean generateUpperBound = true;

  @Option(secure=true,
      description="Generate octagon templates for all combinations of variables. ")
  private boolean generateOctagons = false;

  @Option(secure=true, description="Generate templates from assert statements")
  private boolean generateFromAsserts = true;

  @Option(secure=true,
      description="Ignore the template type and encode with a rational variable")
  private boolean encodeTemplatesAsRationals = false;

  @Option(secure=true,
    description="Generate even more templates (try coeff. 2 for each variable)")
  private boolean generateMoreTemplates = false;

  @Option(secure=true,
    description="Generate cubic constraints: templates +/- x +/- y +/- z for"
        + " every combination (x, y, z)")
  private boolean generateCube = false;

  private final CFA cfa;
  private final LogManager logger;

  /**
   * Dummy edge required by the interface to convert the {@code CIdExpression}
   * into formula.
   */
  private final CFAEdge dummyEdge;

  private final ImmutableSet<Template> generatedTemplates;

  // Temporary variables created by CPA checker.
  private static final String TMP_VARIABLE = "__CPAchecker_TMP";
  private static final String RET_VARIABLE = "__retval__";

  // todo: do not hardcode, use automaton.
  private static final String ASSERT_FUNC_NAME = "assert";
  private static final String ASSERT_H_FUNC_NAME = "__assert_fail";

  private final HashMultimap<CFANode, Template> cache = HashMultimap.create();

  public TemplateManager(
      LogManager pLogger,
      Configuration pConfig,
      CFA pCfa
      ) throws InvalidConfigurationException{
    pConfig.inject(this, TemplateManager.class);

    cfa = pCfa;
    logger = pLogger;

    dummyEdge = new BlankEdge("",
        FileLocation.DUMMY,
        new CFANode("dummy-1"), new CFANode("dummy-2"), "Dummy Edge");
    if (generateFromAsserts) {
      generatedTemplates = ImmutableSet.copyOf(templatesFromAsserts());
    } else {
      generatedTemplates = ImmutableSet.of();
    }
    logger.log(Level.FINE, "Generated templates", generatedTemplates);
  }


  public Set<Template> templatesForNode(CFANode node) {
    if (cache.containsKey(node)) {
      return cache.get(node);
    }

    ImmutableSet.Builder<Template> out = ImmutableSet.builder();
    LiveVariables liveVariables = cfa.getLiveVariables().get();
    List<ASimpleDeclaration> liveVars = ImmutableList.copyOf(
        liveVariables.getLiveVariablesForNode(node));
    for (ASimpleDeclaration s : liveVars) {

      if (!shouldProcessVariable(s)) {
        continue;
      }
      String varName = s.getQualifiedName();
      CSimpleType type = (CSimpleType) s.getType();
      CIdExpression idExpression = new CIdExpression(
          FileLocation.DUMMY, (CSimpleDeclaration)s
      );
      logger.log(Level.FINEST, "Processing variable", varName);
      if (generateUpperBound) {
        out.add(Template.of(LinearExpression.ofVariable(idExpression), type));
      }
      if (generateLowerBound) {
        out.add(Template.of(LinearExpression.ofVariable(idExpression).negate(), type));
      }
    }

    if (generateOctagons) {
      for (ASimpleDeclaration s1 : liveVars) {
        for (ASimpleDeclaration s2 : liveVars) {
          if (!shouldProcessVariable(s1)
              || !shouldProcessVariable(s2)) {
            continue;
          }
          if (s1.getQualifiedName().equals(s2.getQualifiedName())) {

            // Don't pair up the same var.
            continue;
          }
          if (!((CSimpleType)s1.getType()).getType().equals(
              ((CSimpleType)s2.getType()).getType())) {

            // Don't pair up variables of different types.
            continue;
          }

          CIdExpression idExpression1 = new CIdExpression(
              FileLocation.DUMMY, (CSimpleDeclaration)s1
          );
          CIdExpression idExpression2 = new CIdExpression(
              FileLocation.DUMMY, (CSimpleDeclaration)s2
          );

          CSimpleType type = (CSimpleType) s1.getType();
          LinearExpression<CIdExpression> expr1 = LinearExpression.ofVariable(
              idExpression1);
          LinearExpression<CIdExpression> expr2 = LinearExpression.ofVariable(
              idExpression2);

          out.addAll(genOctagonConstraints(expr1, expr2, type));

          if (generateMoreTemplates) {
            out.addAll(genOctagonConstraints(
                expr1.multByConst(Rational.ofLong(2)), expr2, type));
            out.addAll(genOctagonConstraints(expr1,
                expr2.multByConst(Rational.ofLong(2)), type));
          }
        }
      }
    }

    if (generateCube) {
      for (ASimpleDeclaration s1 : liveVars) {
        for (ASimpleDeclaration s2 : liveVars) {
          for (ASimpleDeclaration s3 : liveVars) {
            if (!shouldProcessVariable(s1)
                || !shouldProcessVariable(s2) || !shouldProcessVariable(s3)) {
              continue;
            }

            if (!s1.getType().equals(s2.getType())
                || !s2.getType().equals(s3.getType())) {

              // Don't pair up variables of different types.
              continue;
            }

            if (s1.getQualifiedName().equals(s2.getQualifiedName()) ||
                s2.getQualifiedName().equals(s3.getQualifiedName()) ||
                s3.getQualifiedName().equals(s1.getQualifiedName())) {

              // Don't pair up the same var.
              continue;
            }

            CSimpleType type = (CSimpleType) s1.getType();
            CIdExpression idExpression1 = new CIdExpression(
                FileLocation.DUMMY, (CSimpleDeclaration)s1
            );
            CIdExpression idExpression2 = new CIdExpression(
                FileLocation.DUMMY, (CSimpleDeclaration)s2
            );
            CIdExpression idExpression3 = new CIdExpression(
                FileLocation.DUMMY, (CSimpleDeclaration)s3
            );
            LinearExpression<CIdExpression> expr1 = LinearExpression.ofVariable(
                idExpression1);
            LinearExpression<CIdExpression> expr2 = LinearExpression.ofVariable(
                idExpression2);
            LinearExpression<CIdExpression> expr3 = LinearExpression.ofVariable(
                idExpression3);

            out.addAll(genCubicConstraints(expr1, expr2, expr3, type));

            if (generateMoreTemplates) {
              out.addAll(
                  genCubicConstraints(
                      expr1.multByConst(Rational.ofLong(2)), expr2, expr3, type));
              out.addAll(
                  genCubicConstraints(
                      expr1, expr2.multByConst(Rational.ofLong(2)), expr3, type));
              out.addAll(
                  genCubicConstraints(
                      expr1, expr2, expr3.multByConst(Rational.ofLong(2)), type));
            }
          }
        }
      }
    }

    out.addAll(generatedTemplates);

    cache.putAll(node, out.build());
    return cache.get(node);
  }

  private Set<Template> genOctagonConstraints(
      LinearExpression<CIdExpression> expr1,
      LinearExpression<CIdExpression> expr2,
      CSimpleType type
  ) {
    HashSet<Template> out = new HashSet<>(4);
    out.add(Template.of(expr1.add(expr2), type));
    out.add(Template.of(expr1.negate().sub(expr2), type));
    out.add(Template.of(expr1.sub(expr2), type));
    out.add(Template.of(expr2.sub(expr1), type));
    return out;
  }

  private Set<Template> genCubicConstraints(
      LinearExpression<CIdExpression> expr1,
      LinearExpression<CIdExpression> expr2,
      LinearExpression<CIdExpression> expr3,
      CSimpleType type
  ) {
    HashSet<Template> out = new HashSet<>(4);
    // No negated.
    out.add(Template.of(expr1.add(expr2).add(expr3), type));

    // 1 negated.
    out.add(Template.of(expr1.add(expr2).sub(expr3), type));
    out.add(Template.of(expr1.sub(expr2).add(expr3), type));
    out.add(Template.of(expr1.negate().add(expr2).add(expr3), type));

    // 2 Negated
    out.add(Template.of(expr1.negate().add(expr2).sub(expr3), type));
    out.add(Template.of(expr1.negate().sub(expr2).add(expr3), type));
    out.add(Template.of(expr1.sub(expr2).sub(expr3), type));

    // All negated.
    out.add(Template.of(expr1.negate().sub(expr2).sub(expr3), type));

    return out;
  }


  /**
   * Convert {@code template} to {@link Formula}, using
   * {@link org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap} and
   * context provided by {@code contextFormula}.
   *
   * @return Resulting formula.
   */
  public Formula toFormula(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      Template template,
      PathFormula contextFormula) {
    boolean useRationals = shouldUseRationals(template);
    Formula sum = null;

    for (Entry<CIdExpression, Rational> entry : template.linearExpression) {
      Rational coeff = entry.getValue();
      CIdExpression declaration = entry.getKey();

      final Formula item;
      try {
        item = pfmgr.expressionToFormula(
            contextFormula, declaration, dummyEdge);
      } catch (UnrecognizedCCodeException e) {
        throw new UnsupportedOperationException();
      }

      final Formula multipliedItem;
      if (coeff == Rational.ZERO) {
        continue;
      } else if (coeff == Rational.NEG_ONE) {
        multipliedItem = fmgr.makeNegate(item);
      } else if (coeff != Rational.ONE){
        multipliedItem = fmgr.makeMultiply(
            item, fmgr.makeNumber(item, entry.getValue())
        );
      } else {
        multipliedItem = item;
      }

      if (sum == null) {
        sum = multipliedItem;
      } else {
        sum = fmgr.makePlus(sum, multipliedItem);
      }
    }

    if (sum == null) {
      if (useRationals) {
        return fmgr.getRationalFormulaManager().makeNumber(0);
      } else {
        return fmgr.getIntegerFormulaManager().makeNumber(0);
      }
    } else {
      return sum;
    }
  }

  public boolean shouldUseRationals(Template template) {
    return encodeTemplatesAsRationals
        || !template.linearExpression.isIntegral()
        || !template.getType().getType().isIntegerType();
  }

  /**
   * Ignore temporary variables and pointers.
   */
  private boolean shouldProcessVariable(ASimpleDeclaration var) {
    return !var.getQualifiedName().contains(TMP_VARIABLE)
        && var.getType() instanceof CSimpleType
        && !var.getType().toString().contains("*")
        && !var.getQualifiedName().contains(RET_VARIABLE);

  }

  /**
   * Generate templates from the calls to assert() functions.
   */
  private Set<Template> templatesFromAsserts() {
    Set<Template> templates = new HashSet<>();

    for (CFANode node : cfa.getAllNodes()) {
      for (int edgeIdx=0; edgeIdx<node.getNumLeavingEdges(); edgeIdx++) {
        CFAEdge edge = node.getLeavingEdge(edgeIdx);
        String statement = edge.getRawStatement();
        Optional<Template> template = Optional.absent();

        // todo: use the automaton instead to derive the error conditions,
        // do not hardcode the function names.
        if (statement.contains(ASSERT_H_FUNC_NAME)
            && edge instanceof CStatementEdge) {

          for (int enteringEdgeIdx=0;
               enteringEdgeIdx<node.getNumEnteringEdges(); enteringEdgeIdx++) {
            CFAEdge enteringEdge = node.getEnteringEdge(enteringEdgeIdx);
            if (enteringEdge instanceof CAssumeEdge) {
              CAssumeEdge assumeEdge = (CAssumeEdge) enteringEdge;
              CExpression expression = assumeEdge.getExpression();

              template = recExpressionToTemplate(expression);
            }
          }

        } else if (statement.contains(ASSERT_FUNC_NAME) &&
            edge instanceof CFunctionCallEdge) {

          CFunctionCallEdge callEdge = (CFunctionCallEdge) edge;
          if (callEdge.getArguments().isEmpty()) {
            continue;
          }
          CExpression expression = callEdge.getArguments().get(0);
          template = recExpressionToTemplate(expression);
        }

        if (template.isPresent()) {
          Template t = template.get();
          if (t.linearExpression.isEmpty()) {
            continue;
          }

          // Add template and its negation.
          templates.add(t);
          templates.add(
              Template.of(t.linearExpression.negate(), t.getType())
          );
        }

      }
    }
    return templates;
  }

  private Optional<Template> recExpressionToTemplate(CExpression expression) {
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression)expression;
      CExpression operand1 = binaryExpression.getOperand1();
      CExpression operand2 = binaryExpression.getOperand2();

      CBinaryExpression.BinaryOperator operator = binaryExpression.getOperator();
      Optional<Template> templateA = recExpressionToTemplate(operand1);
      Optional<Template> templateB = recExpressionToTemplate(operand2);

      // Special handling for constants and multiplication.
      if (operator == CBinaryExpression.BinaryOperator.MULTIPLY
          && (templateA.isPresent() || templateB.isPresent())) {

        if (operand1 instanceof CIntegerLiteralExpression &&
            templateB.isPresent()) {

          return Optional.of(useCoeff(
              (CIntegerLiteralExpression) operand1, templateB.get()
          ));
        } else if (operand2 instanceof CIntegerLiteralExpression &&
            templateA.isPresent()) {

          return Optional.of(
              useCoeff((CIntegerLiteralExpression) operand2, templateA.get())
          );
        } else {
          return Optional.absent();
        }
      }

      // Otherwise just add/subtract templates.
      if (templateA.isPresent() && templateB.isPresent()
          && binaryExpression.getCalculationType() instanceof CSimpleType) {
        LinearExpression<CIdExpression> a = templateA.get().linearExpression;
        LinearExpression<CIdExpression> b = templateB.get().linearExpression;

        // Calculation type is the casting of both types to a suitable "upper"
        // type.
        CSimpleType type = (CSimpleType)binaryExpression.getCalculationType();
        Template t;
        if (operator == CBinaryExpression.BinaryOperator.PLUS) {
          t = Template.of(a.add(b), type);
        } else {
          t = Template.of(a.sub(b), type);
        }
        return Optional.of(t);
      } else {
        return Optional.absent();
      }
    } else if (expression instanceof CLiteralExpression
        && expression.getExpressionType() instanceof CSimpleType) {
      return Optional.of(Template.of(
          LinearExpression.<CIdExpression>empty(),
          (CSimpleType)(expression).getExpressionType()
      ));
    } else if (expression instanceof CIdExpression
        && expression.getExpressionType() instanceof CSimpleType) {
      CIdExpression idExpression = (CIdExpression)expression;
      return Optional.of(Template.of(
              LinearExpression.ofVariable(idExpression),
              (CSimpleType) expression.getExpressionType()));
    } else {
      return Optional.absent();
    }
  }

  private Template useCoeff(
      CIntegerLiteralExpression literal, Template other) {
    Rational coeff = Rational.ofBigInteger(literal.getValue());
    return Template.of(other.linearExpression.multByConst(coeff),
        other.getType()
    );
  }
}
