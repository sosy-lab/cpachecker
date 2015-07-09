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
import org.sosy_lab.common.rationals.LinearExpression;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

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

  @Option(secure=true, description="Generate templates from all program "
      + "statements")
  private boolean generateFromStatements = true;

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

  @Option(secure=true,
    description="Strategy for filtering variables out of templates")
  private VarFilteringStrategy varFiltering = VarFilteringStrategy.ALL_LIVE;

  private final CFA cfa;
  private final LogManager logger;

  /**
   * Dummy edge required by the interface to convert the {@code CIdExpression}
   * into formula.
   */
  private final CFAEdge dummyEdge;

  private final ImmutableSet<Template> extractedFromAssertTemplates;
  private final ImmutableSet<Template> extractedTemplates;
  private final Set<Template> extraTemplates;
  private final Set<Template> generatedTemplates;
  private final PolicyIterationStatistics statistics;

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
      CFA pCfa, PolicyIterationStatistics pStatistics)
        throws InvalidConfigurationException {
    statistics = pStatistics;
    extraTemplates = new HashSet<>();
    pConfig.inject(this, TemplateManager.class);

    cfa = pCfa;
    logger = pLogger;

    dummyEdge = new BlankEdge("",
        FileLocation.DUMMY,
        new CFANode("dummy-1"), new CFANode("dummy-2"), "Dummy Edge");
    if (generateFromAsserts) {
      extractedFromAssertTemplates = ImmutableSet.copyOf(templatesFromAsserts());
    } else {
      extractedFromAssertTemplates = ImmutableSet.of();
    }
    logger.log(Level.FINE, "Generated from assert templates",
        extractedFromAssertTemplates);

    if (generateFromStatements) {
      extractedTemplates = ImmutableSet.copyOf(extractTemplates());
    } else {
      extractedTemplates = ImmutableSet.of();
    }
    logger.log(Level.FINE, "Generated templates", extractedFromAssertTemplates);
    generatedTemplates = new HashSet<>();

    allVariables = ImmutableList.copyOf(
        cfa.getLiveVariables().get().getAllLiveVariables());
  }

  public PolicyPrecision precisionForNode(CFANode node) {
    return new PolicyPrecision(templatesForNode(node));
  }

  public Set<Template> templatesForNode(final CFANode node) {
    if (cache.containsKey(node)) {
      return cache.get(node);
    }

    ImmutableSet.Builder<Template> out = ImmutableSet.builder();
    List<ASimpleDeclaration> usedVars = ImmutableList.copyOf(getVarsForNode(node));
    out.addAll(extractTemplatesForNode(node));
    out.addAll(extraTemplates);

    for (ASimpleDeclaration s : usedVars) {
      if (!shouldProcessVariable(s)) {
        continue;
      }
      String varName = s.getQualifiedName();
      CIdExpression idExpression = new CIdExpression(
          FileLocation.DUMMY, (CSimpleDeclaration)s
      );
      logger.log(Level.FINEST, "Processing variable", varName);
      if (generateUpperBound) {
        out.add(Template.of(LinearExpression.ofVariable(idExpression)));
      }
      if (generateLowerBound) {
        out.add(Template.of(LinearExpression.ofVariable(idExpression).negate()));
      }
    }

    if (generateOctagons) {
      for (ASimpleDeclaration s1 : usedVars) {
        for (ASimpleDeclaration s2 : usedVars) {
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

          LinearExpression<CIdExpression> expr1 = LinearExpression.ofVariable(
              idExpression1);
          LinearExpression<CIdExpression> expr2 = LinearExpression.ofVariable(
              idExpression2);

          out.addAll(genOctagonConstraints(expr1, expr2));

          if (generateMoreTemplates) {
            out.addAll(genOctagonConstraints(
                expr1.multByConst(Rational.ofLong(2)), expr2));
            out.addAll(genOctagonConstraints(expr1,
                expr2.multByConst(Rational.ofLong(2))));
          }
        }
      }
    }

    if (generateCube) {
      for (ASimpleDeclaration s1 : usedVars) {
        for (ASimpleDeclaration s2 : usedVars) {
          for (ASimpleDeclaration s3 : usedVars) {
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

            CIdExpression idExpression1 = new CIdExpression(
                FileLocation.DUMMY, (CSimpleDeclaration)s1);
            CIdExpression idExpression2 = new CIdExpression(
                FileLocation.DUMMY, (CSimpleDeclaration)s2);
            CIdExpression idExpression3 = new CIdExpression(
                FileLocation.DUMMY, (CSimpleDeclaration)s3);

            LinearExpression<CIdExpression> expr1 = LinearExpression.ofVariable(
                idExpression1);
            LinearExpression<CIdExpression> expr2 = LinearExpression.ofVariable(
                idExpression2);
            LinearExpression<CIdExpression> expr3 = LinearExpression.ofVariable(
                idExpression3);

            out.addAll(genCubicConstraints(expr1, expr2, expr3));

            if (generateMoreTemplates) {
              out.addAll(
                  genCubicConstraints(
                      expr1.multByConst(Rational.ofLong(2)), expr2, expr3));
              out.addAll(
                  genCubicConstraints(
                      expr1, expr2.multByConst(Rational.ofLong(2)), expr3));
              out.addAll(
                  genCubicConstraints(
                      expr1, expr2, expr3.multByConst(Rational.ofLong(2))));
            }
          }
        }
      }
    }

    out.addAll(extractedFromAssertTemplates);
    Set<Template> outBuild = out.build();

    if (varFiltering == VarFilteringStrategy.ONE_LIVE) {

      // Filter templates to make sure at least one is alive.
      outBuild = Sets.filter(outBuild, new Predicate<Template>() {
        @Override
        public boolean apply(Template input) {
          return shouldUseTemplate(input, node);
        }
      });
    }

    cache.putAll(node, outBuild);
    return cache.get(node);
  }

  private Set<Template> genOctagonConstraints(
      LinearExpression<CIdExpression> expr1,
      LinearExpression<CIdExpression> expr2) {
    HashSet<Template> out = new HashSet<>(4);
    out.add(Template.of(expr1.add(expr2)));
    out.add(Template.of(expr1.negate().sub(expr2)));
    out.add(Template.of(expr1.sub(expr2)));
    out.add(Template.of(expr2.sub(expr1)));
    return out;
  }

  private Set<Template> genCubicConstraints(
      LinearExpression<CIdExpression> expr1,
      LinearExpression<CIdExpression> expr2,
      LinearExpression<CIdExpression> expr3) {
    HashSet<Template> out = new HashSet<>(4);
    // No negated.
    out.add(Template.of(expr1.add(expr2).add(expr3)));

    // 1 negated.
    out.add(Template.of(expr1.add(expr2).sub(expr3)));
    out.add(Template.of(expr1.sub(expr2).add(expr3)));
    out.add(Template.of(expr1.negate().add(expr2).add(expr3)));

    // 2 Negated
    out.add(Template.of(expr1.negate().add(expr2).sub(expr3)));
    out.add(Template.of(expr1.negate().sub(expr2).add(expr3)));
    out.add(Template.of(expr1.sub(expr2).sub(expr3)));

    // All negated.
    out.add(Template.of(expr1.negate().sub(expr2).sub(expr3)));

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
    int maxBitvectorSize = getBitvectorSize(template, pfmgr, contextFormula,fmgr);

    for (Entry<CIdExpression, Rational> entry : template.linearExpression) {
      Rational coeff = entry.getValue();
      CIdExpression declaration = entry.getKey();

      final Formula item;
      try {
        item = normalizeLength(pfmgr.expressionToFormula(
            contextFormula, declaration, dummyEdge),
            maxBitvectorSize, fmgr);
      } catch (UnrecognizedCCodeException e) {
        throw new UnsupportedOperationException();
      }

      final Formula multipliedItem;
      if (coeff == Rational.ZERO) {
        continue;
      } else if (coeff == Rational.NEG_ONE) {
        multipliedItem = fmgr.makeNegate(item, true);
      } else if (coeff != Rational.ONE){
        multipliedItem = fmgr.makeMultiply(
            item, fmgr.makeNumber(item, entry.getValue()), true
        );
      } else {
        multipliedItem = item;
      }

      if (sum == null) {
        sum = multipliedItem;
      } else {
        sum = fmgr.makePlus(sum, multipliedItem, true);
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
        || !template.isIntegral();
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
          templates.add(Template.of(t.linearExpression.negate()));
        }

      }
    }
    return templates;
  }

  private Set<Template> extractTemplatesForNode(CFANode node) {
    Set<Template> out = new HashSet<>();
    for (Template t : extractedTemplates) {
      if (shouldUseTemplate(t, node)) {
        out.add(t);
      }
    }
    return out;
  }

  private boolean shouldUseTemplate(Template t, CFANode node) {
    if (varFiltering == VarFilteringStrategy.ALL) {
      return true;
    }
    LiveVariables liveVariables = cfa.getLiveVariables().get();
    for (Entry<CIdExpression, Rational> e : t.linearExpression) {
      CIdExpression id = e.getKey();
      if (varFiltering == VarFilteringStrategy.ONE_LIVE &&
          !liveVariables.isVariableLive(id.getDeclaration(), node)) {
        return true;
      }
      if (varFiltering == VarFilteringStrategy.ALL_LIVE &&
          !liveVariables.isVariableLive(id.getDeclaration(), node)) {
        return false;
      }
    }
    return true;
  }

  private Set<Template> extractTemplates() {
    Set<Template> out = new HashSet<>();
    for (CFANode node : cfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.allEnteringEdges(node)) {
        for (Template t : extractTemplatesFromEdge(edge)) {
          if (t.size() > 1) {
            out.add(t);
          }
        }
      }
    }
    return out;
  }

  private Set<Template> extractTemplatesFromEdge(CFAEdge edge) {
    Set<Template> out = new HashSet<>();
    switch (edge.getEdgeType()) {
      case ReturnStatementEdge:
        CReturnStatementEdge e = (CReturnStatementEdge) edge;
        if (e.getExpression().isPresent()) {
          out.addAll(expressionToTemplate(e.getExpression().get()));
        }
        break;
      case FunctionCallEdge:
        CFunctionCallEdge callEdge = (CFunctionCallEdge) edge;
        for (CExpression arg : callEdge.getArguments()) {
          out.addAll(expressionToTemplate(arg));
        }
        break;
      case AssumeEdge:
        CAssumeEdge assumeEdge = (CAssumeEdge) edge;
        out.addAll(expressionToTemplate(assumeEdge.getExpression()));
        break;
      case StatementEdge:
        out.addAll(extractTemplatesFromStatementEdge((CStatementEdge) edge));
        break;
      case MultiEdge:
        MultiEdge multiEdge = (MultiEdge) edge;
        for (CFAEdge child : multiEdge.getEdges()) {
          out.addAll(extractTemplatesFromEdge(child));
        }
        break;
    }
    return out;
  }

  private Set<Template> extractTemplatesFromStatementEdge(CStatementEdge edge) {
    Set<Template> out = new HashSet<>();
    CStatement statement = edge.getStatement();
    if (statement instanceof CExpressionStatement) {
      out.addAll(expressionToTemplate(
          ((CExpressionStatement)statement).getExpression()));
    } else if (statement instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement assignment =
          (CExpressionAssignmentStatement)statement;
      CLeftHandSide lhs =
          assignment.getLeftHandSide();
      if (lhs instanceof CIdExpression) {
        CIdExpression id = (CIdExpression)lhs;
        out.addAll(expressionToTemplate(assignment.getRightHandSide()));
        if (!shouldProcessVariable(id.getDeclaration())) {
          return out;
        }

        Template tLhs = Template.of(LinearExpression.ofVariable(id));
        Optional<Template> x =
            recExpressionToTemplate(assignment.getRightHandSide());
        if (x.isPresent()) {
          Template tX = x.get();
          out.add(Template.of(tLhs.linearExpression.sub(tX.linearExpression)));
        }
      }
    }
    return out;

  }

  private Set<Template> expressionToTemplate(CExpression expression) {
    HashSet<Template> out = new HashSet<>(2);
    Optional<Template> t = recExpressionToTemplate(expression);
    if (!t.isPresent()) {
      return out;
    }
    out.add(t.get());
    out.add(Template.of(t.get().linearExpression.negate()));
    return out;
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
        Template t;
        if (operator == CBinaryExpression.BinaryOperator.PLUS) {
          t = Template.of(a.add(b));
        } else {
          t = Template.of(a.sub(b));
        }
        return Optional.of(t);
      } else {
        return Optional.absent();
      }
    } else if (expression instanceof CLiteralExpression
        && expression.getExpressionType() instanceof CSimpleType) {
      return Optional.of(Template.of(LinearExpression.<CIdExpression>empty()));
    } else if (expression instanceof CIdExpression
        && expression.getExpressionType() instanceof CSimpleType) {
      CIdExpression idExpression = (CIdExpression)expression;
      return Optional.of(Template.of(LinearExpression.ofVariable(idExpression)));
    } else {
      return Optional.absent();
    }
  }


  private Template useCoeff(
      CIntegerLiteralExpression literal, Template other) {
    Rational coeff = Rational.ofBigInteger(literal.getValue());
    return Template.of(other.linearExpression.multByConst(coeff));
  }

  public void addGeneratedTemplates(Set<Template> templates) {
    generatedTemplates.addAll(templates);
  }

  public boolean adjustPrecision() {
    boolean changed = false;
    for (Template t : generatedTemplates) {
      changed |= addTemplateToExtra(t);
    }
    try {
      if (changed) {
        logger.log(Level.INFO, "LPI Refinement: Using new templates",
            generatedTemplates);
        generatedTemplates.clear();
        return true;
      }
      if (!generateOctagons) {
        logger.log(Level.INFO, "LPI Refinement: Generating octagons");
        generateOctagons = true;
        return true;
      }
      if (!generateMoreTemplates) {
        logger.log(Level.INFO, "LPI Refinement: Generating more templates");
        generateMoreTemplates = true;
        return true;
      }
      if (!generateCube) {
        logger.log(Level.INFO, "LPI Refinement: Rich template generation strategy");
        generateCube = true;
        return true;
      }
      return false;
    } finally {
      cache.clear();
    }
  }

  private boolean addTemplateToExtra(Template t) {
    // Do not add intervals.
    if (t.size() == 1) return false;

    for (Template o : extraTemplates) {
      // Do not add templates which are multiples of already existing templates.
      if (o.getLinearExpression().isMultipleOf(t.getLinearExpression())) {
        return false;
      }
    }
    boolean out = extraTemplates.add(t);
    if (out) {
      statistics.incWideningTemplatesGenerated();
    }
    return out;
  }

  public Iterable<ASimpleDeclaration> getVarsForNode(CFANode node) {
    if (varFiltering == VarFilteringStrategy.ALL_LIVE) {
      return cfa.getLiveVariables().get().getLiveVariablesForNode(node);
    } else {
      return allVariables;
    }
  }

  private final ImmutableList<ASimpleDeclaration> allVariables;

  private enum VarFilteringStrategy {
    ALL_LIVE,
    ONE_LIVE,
    ALL
  }

  private Formula normalizeLength(Formula f, int maxBitvectorSize,
      FormulaManagerView fmgr) {
    if (!(f instanceof BitvectorFormula)) return f;
    BitvectorFormula bv = (BitvectorFormula) f;
    return fmgr.getBitvectorFormulaManager().extend(
        bv,
        Math.max(0,
            maxBitvectorSize - fmgr.getBitvectorFormulaManager().getLength(bv)),
        true);
  }

  private int getBitvectorSize(Template t, PathFormulaManager pfmgr,
      PathFormula contextFormula, FormulaManagerView fmgr) {
    int length = 0;

    // Figure out the maximum bitvector size.
    for (Entry<CIdExpression, Rational> entry : t.linearExpression) {
      Formula item;
      try {
        item = pfmgr.expressionToFormula(contextFormula, entry.getKey(),
            dummyEdge);
      } catch (UnrecognizedCCodeException e) {
        throw new UnsupportedOperationException();
      }
      if (!(item instanceof BitvectorFormula)) continue;
      BitvectorFormula b = (BitvectorFormula) item;
      length = Math.max(
          fmgr.getBitvectorFormulaManager().getLength(b),
          length);
    }
    return length;
  }
}
