package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.HashSet;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Optional;
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

  private final CFA cfa;
  private final LogManager logger;
  private final NumeralFormulaManagerView<
      NumeralFormula, NumeralFormula.RationalFormula> rfmgr;
  private final NumeralFormulaManagerView<NumeralFormula.IntegerFormula,
      NumeralFormula.IntegerFormula> ifmgr;
  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgrv;

  /**
   * Dummy edge required by the interface to convert the {@code CIdExpression}
   * into formula.
   */
  private final CFAEdge dummyEdge;

  private final ImmutableSet<Template> generatedTemplates;

  // Temporary variables created by CPA checker.
  private static final String TMP_VARIABLE = "__CPAchecker_TMP";
  private static final String RET_VARIABLE = "__retval__";

  private static final String ASSERT_FUNC_NAME = "assert";
  private static final String ASSERT_H_FUNC_NAME = "__assert_fail";

  public TemplateManager(
      LogManager pLogger,
      Configuration pConfig,
      CFA pCfa,
      FormulaManagerView pFormulaManagerView,
      PathFormulaManager pPfmgr
      ) throws InvalidConfigurationException{
    pConfig.inject(this, TemplateManager.class);

    pfmgr = pPfmgr;
    cfa = pCfa;
    logger = pLogger;
    rfmgr = pFormulaManagerView.getRationalFormulaManager();
    ifmgr = pFormulaManagerView.getIntegerFormulaManager();
    fmgrv = pFormulaManagerView;

    dummyEdge = new BlankEdge("",
        FileLocation.DUMMY,
        new CFANode("dummy-1"), new CFANode("dummy-2"), "Dummy Edge");
    if (generateFromAsserts) {
      generatedTemplates = ImmutableSet.copyOf(templatesFromAsserts());
    } else {
      generatedTemplates = ImmutableSet.of();
    }
    logger.log(Level.FINE, "hello");
  }


  public ImmutableSet<Template> templatesForNode(CFANode node) {
    ImmutableSet.Builder<Template> out = ImmutableSet.builder();
    LiveVariables liveVariables = cfa.getLiveVariables().get();
    Set<ASimpleDeclaration> liveVars = liveVariables.getLiveVariablesForNode(node);
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
          if (s1 == s2) { // Don't pair up the same var.
            continue;
          }
          if (!s1.getType().equals(s2.getType())) {

            // Don't pair up variables of different types.
            continue;
          }

          CSimpleType type = (CSimpleType) s1.getType();
          CIdExpression idExpression1 = new CIdExpression(
              FileLocation.DUMMY, (CSimpleDeclaration)s1
          );
          CIdExpression idExpression2 = new CIdExpression(
              FileLocation.DUMMY, (CSimpleDeclaration)s2
          );
          LinearExpression<CIdExpression> expr1 = LinearExpression.ofVariable(
              idExpression1);
          LinearExpression<CIdExpression> expr2 = LinearExpression.ofVariable(
              idExpression2);

          out.add(Template.of(expr1.add(expr2), type));
          out.add(Template.of(expr1.negate().sub(expr2), type));
          out.add(Template.of(expr1.sub(expr2), type));
          out.add(Template.of(expr2.sub(expr1), type));
        }
      }
    }

    out.addAll(generatedTemplates);

    return out.build();
  }


  /**
   * Convert {@code template} to {@link Formula}, using
   * {@link org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap} and
   * context provided by {@code contextFormula}.
   *
   * @return Resulting formula.
   */
  public Formula toFormula(Template template, PathFormula contextFormula) {
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
        multipliedItem = fmgrv.makeNegate(item);
      } else if (coeff != Rational.ONE){
        multipliedItem = fmgrv.makeMultiply(
            item, fmgrv.makeNumber(item, entry.getValue())
        );
      } else {
        multipliedItem = item;
      }

      if (sum == null) {
        sum = multipliedItem;
      } else {
        sum = fmgrv.makePlus(sum, multipliedItem);
      }
    }

    if (sum == null) {
      if (useRationals) {
        return rfmgr.makeNumber(0);
      } else {
        return ifmgr.makeNumber(0);
      }
    } else {
      return sum;
    }
  }

  public boolean shouldUseRationals(Template template) {
    if (encodeTemplatesAsRationals) {
      return true;
    }
    if (!template.linearExpression.isIntegral()) {
      return true;
    }
    switch (template.type.getType()) {
      case BOOL:
      case INT:
        return false;
      case UNSPECIFIED:
      case CHAR:
      case FLOAT:
      case DOUBLE:
        return true;
      default:
        throw new IllegalArgumentException("Unexpected type: " + template.type);
    }
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

          // Add template and its negation.
          templates.add(t);
          templates.add(
              Template.of(t.linearExpression.negate(), t.type)
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
        other.type
    );
  }
}
