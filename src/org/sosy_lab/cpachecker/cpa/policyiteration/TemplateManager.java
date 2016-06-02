package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
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
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BitvectorFormula;
import org.sosy_lab.solver.api.Formula;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Options(prefix = "cpa.lpi")
public class TemplateManager {

  @Option(secure=true, description="Generate templates from assert statements")
  private boolean generateFromAsserts = true;

  @Option(secure=true, description="Generate templates from all program "
      + "statements")
  private boolean generateFromStatements = true;

  @Option(secure=true, description="Maximum size for the generated template")
  private int maxExpressionSize = 1;

  @Option(secure=true, description="Allowed coefficients in a template.")
  private Set<Rational> allowedCoefficients = ImmutableSet.of(
      Rational.NEG_ONE, Rational.ONE
  );

  @Option(secure=true,
    description="Strategy for filtering variables out of templates using "
        + "liveness")
  private VarFilteringStrategy varFiltering = VarFilteringStrategy.ALL_LIVE;

  // todo: merge with varFilteringStrategy enum.
  @Option(secure=true, description="Use only variables which appear in IntAddVars "
      + "in variable classification")
  private boolean filterIntAddVars = false;

  @Option(secure=true,
      description="Do not generate templates with threshold larger than specified."
          + " Set to '-1' for no limit.")
  private long templateConstantThreshold = 100;

  private enum VarFilteringStrategy {
    ALL_LIVE,
    ONE_LIVE,
    ALL
  }

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
  private final ImmutableSet<ASimpleDeclaration> allVariables;
  private final VariableClassification variableClassification;
  private final CBinaryExpressionBuilder expressionBuilder;

  public TemplateManager(
      LogManager pLogger,
      Configuration pConfig,
      CFA pCfa, PolicyIterationStatistics pStatistics)
        throws InvalidConfigurationException {
    pConfig.inject(this, TemplateManager.class);

    variableClassification = pCfa.getVarClassification().get();
    statistics = pStatistics;
    extraTemplates = new HashSet<>();

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

    allVariables = ImmutableSet.copyOf(
        cfa.getLiveVariables().get().getAllLiveVariables());
    expressionBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(),
        logger);
  }

  /**
   * Generate all linear expressions of size up to {@code maxExpressionSize}
   * with coefficients in {@code allowedCoefficients},
   * over the variables returned by {@link #getVarsForNode(CFANode)}.
   */
  public Set<Template> generateTemplates(final CFANode node) {

    Set<ASimpleDeclaration> varsForNode = getVarsForNode(node);
    Set<CIdExpression> vars = varsForNode.stream()
        .filter(this::shouldProcessVariable)
        .map(
            d -> new CIdExpression(FileLocation.DUMMY, (CSimpleDeclaration) d)
        ).collect(Collectors.toSet());
    int maxLength = Math.min(maxExpressionSize, vars.size());
    allowedCoefficients = allowedCoefficients.stream().filter(
        x -> !x.equals(Rational.ZERO)).collect(Collectors.toSet());

    // Copy the {@code vars} multiple times for the cartesian product.
    List<Set<CIdExpression>> lists = new ArrayList<>();
    for (int i=0; i<maxLength; i++) {
      lists.add(vars);
    }

    // All lists of size {@code maxExpressionSize}.
    Set<List<CIdExpression>> product = Sets.cartesianProduct(lists);

    // Eliminate duplicates, and ensure that all combinations are unique.
    // As a by-product, produces the expressions of all sizes less than
    // {@code maxExpressionSize} as well.
    Set<Set<CIdExpression>> combinations =
        product.stream().map(HashSet<CIdExpression>::new).collect(Collectors.toSet());

    Set<Template> returned = new HashSet<>();
    for (Set<CIdExpression> variables : combinations) {

      // For of every variable: instantiate with every coefficient.
      List<List<LinearExpression<CIdExpression>>> out =
          variables.stream().map(
              x -> allowedCoefficients.stream().map(
                  coeff -> LinearExpression.monomial(x, coeff)
              ).collect(Collectors.toList())
          ).collect(Collectors.toList());

      // Convert to a list of all possible linear expressions.
      List<LinearExpression<CIdExpression>> linearExpressions =
          Lists.cartesianProduct(out).stream().map(
              list -> list.stream().reduce(
                  LinearExpression.empty(), LinearExpression::add)
          ).collect(Collectors.toList());

      Set<Template> generated =
          filterToSameType(
              filterRedundantExpressions(linearExpressions)
          ).stream().map(Template::of).collect(Collectors.toSet());
      returned.addAll(generated);
    }

    return returned;
  }

  /**
   * Filter out the redundant expressions: that is, expressions already
   * contained in the list with a multiplier {@code >= 1}.
   */
  private List<LinearExpression<CIdExpression>> filterRedundantExpressions(
      List<LinearExpression<CIdExpression>> pLinearExpressions
  ) {
    Predicate<com.google.common.base.Optional<Rational>> existsAndMoreThanOne =
        (coeff -> coeff.isPresent() && coeff.get().compareTo(Rational.ONE) > 0);
    return pLinearExpressions.stream().filter(
            l -> !pLinearExpressions.stream().anyMatch(
                l2 -> l2 != l && existsAndMoreThanOne.test(l2.divide(l))
            )
        ).collect(Collectors.toList());
  }

  /**
   * Filter out the expressions where not all variables inside have the
   * same type.
   */
  private List<LinearExpression<CIdExpression>> filterToSameType(
      List<LinearExpression<CIdExpression>> pLinearExpressions
  ) {
    Function<CIdExpression, CBasicType> getType =
        x -> ((CSimpleType)x.getDeclaration().getType()).getType();
    return pLinearExpressions.stream().filter(
            expr -> expr.getMap().keySet().stream().allMatch(
                x -> getType.apply(x).equals(
                    getType.apply(expr.iterator().next().getKey())
                )
            )
        ).collect(Collectors.toList());
  }

  public PolicyPrecision precisionForNode(CFANode node) {
    return new PolicyPrecision(templatesForNode(node));
  }

  public Set<Template> templatesForNode(final CFANode node) {
    if (cache.containsKey(node)) {
      return cache.get(node);
    }

    Builder<Template> out = ImmutableSet.builder();
    out.addAll(extractTemplatesForNode(node)::iterator);
    out.addAll(extraTemplates);
    out.addAll(extractedFromAssertTemplates);

    out.addAll(generateTemplates(node));
    Set<Template> outBuild = out.build();

    if (varFiltering == VarFilteringStrategy.ONE_LIVE) {

      // Filter templates to make sure at least one is alive.
      outBuild = Sets.filter(outBuild, input -> shouldUseTemplate(input, node));
    }

    cache.putAll(node, outBuild);
    return cache.get(node);
  }

  private final Map<ToFormulaCacheKey, Formula> toFormulaCache =
      new HashMap<>();

  /**
   * Convert {@code template} to {@link Formula}, using
   * {@link SSAMap} and
   * the context provided by {@code contextFormula}.
   *
   * @return Resulting formula.
   */
  public Formula toFormula(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      Template template,
      PathFormula contextFormula) {
    ToFormulaCacheKey key =
        new ToFormulaCacheKey(pfmgr, fmgr, template, contextFormula);
    Formula out = toFormulaCache.get(key);
    if (out != null) {
      return out;
    }
    boolean useRationals = !template.isIntegral();
    Formula sum = null;
    int maxBitvectorSize = getBitvectorSize(template, pfmgr, contextFormula,fmgr);

    for (Entry<CIdExpression, Rational> entry : template.linearExpression) {
      Rational coeff = entry.getValue();
      CIdExpression declaration = entry.getKey();

      final Formula item;
      try {
        Formula f = pfmgr.expressionToFormula(
            contextFormula, declaration, dummyEdge);
        item = normalizeLength(f, maxBitvectorSize, fmgr);
      } catch (UnrecognizedCCodeException e) {
        throw new UnsupportedOperationException();
      }

      final Formula multipliedItem;
      if (coeff.equals(Rational.ZERO)) {
        continue;
      } else if (coeff.equals(Rational.NEG_ONE)) {
        multipliedItem = fmgr.makeNegate(item);
      } else if (coeff.equals(Rational.ONE)){
        multipliedItem = item;
      } else {
        multipliedItem = fmgr.makeMultiply(
            item, fmgr.makeNumber(item, entry.getValue()));
      }

      if (sum == null) {
        sum = multipliedItem;
      } else {
        sum = fmgr.makePlus(sum, multipliedItem);
      }
    }

    if (sum == null) {
      if (useRationals) {
        out = fmgr.getRationalFormulaManager().makeNumber(0);
      } else {
        out = fmgr.getIntegerFormulaManager().makeNumber(0);
      }
    } else {
      out = sum;
    }
    toFormulaCache.put(key, out);
    return out;
  }

  /**
   * Ignore temporary variables and pointers.
   */
  private boolean shouldProcessVariable(ASimpleDeclaration var) {
    return !var.getQualifiedName().contains(TMP_VARIABLE)
        && var.getType() instanceof CSimpleType
        && !var.getType().toString().contains("*")
        && !var.getQualifiedName().contains(RET_VARIABLE)
        && (!filterIntAddVars ||
          variableClassification.getIntAddVars().contains(var.getQualifiedName()));

  }

  /**
   * Generate templates from the calls to assert() functions.
   */
  private Set<Template> templatesFromAsserts() {
    Set<Template> templates = new HashSet<>();

    for (CFANode node : cfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        String statement = edge.getRawStatement();
        Optional<Template> template = Optional.empty();

        // todo: use the automaton instead to derive the error conditions,
        // do not hardcode the function names.
        if (statement.contains(ASSERT_H_FUNC_NAME)
            && edge instanceof CStatementEdge) {

          for (CFAEdge enteringEdge : CFAUtils.enteringEdges(node)) {
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

  private Stream<Template> extractTemplatesForNode(CFANode node) {
    return extractedTemplates.stream().filter(t -> shouldUseTemplate(t, node));
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
        out.addAll(
            extractTemplatesFromEdge(edge).stream().filter(t -> t.size() > 1)
                .collect(Collectors.toSet()));
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
      default:
        // nothing to do here
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

      BinaryOperator operator = binaryExpression.getOperator();
      Optional<Template> templateA = recExpressionToTemplate(operand1);
      Optional<Template> templateB = recExpressionToTemplate(operand2);

      // Special handling for constants and multiplication.
      if (operator == BinaryOperator.MULTIPLY
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
          return Optional.empty();
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
        if (operator == BinaryOperator.PLUS) {
          t = Template.of(a.add(b));
        } else {
          t = Template.of(a.sub(b));
        }
        return Optional.of(t);
      } else {
        return Optional.empty();
      }
    } else if (expression instanceof CLiteralExpression
        && expression.getExpressionType() instanceof CSimpleType) {
      return Optional.of(Template.of(LinearExpression.empty()));
    } else if (expression instanceof CIdExpression
        && expression.getExpressionType() instanceof CSimpleType) {
      CIdExpression idExpression = (CIdExpression)expression;
      return Optional.of(Template.of(LinearExpression.ofVariable(idExpression)));
    } else {
      return Optional.empty();
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
        logger.log(Level.INFO,
            "LPI Refinement: using templates generated with convex hull",
            generatedTemplates);
        generatedTemplates.clear();
        return true;
      }

      if (maxExpressionSize == 1) {
        logger.log(Level.INFO, "LPI Refinement: Generating octagons");
        maxExpressionSize = 2;
        return true;
      }
      if (maxExpressionSize == 2
          && !allowedCoefficients.contains(Rational.ofLong(2))) {
        logger.log(Level.INFO, "LPI Refinement: Generating more templates");
        allowedCoefficients = Sets.union(
            allowedCoefficients, ImmutableSet.of(
                Rational.ofLong(2), Rational.ofLong(-2)));
        return true;
      }
      if (maxExpressionSize == 2
          && allowedCoefficients.contains(Rational.ofLong(2))) {
        logger.log(Level.INFO, "LPI Refinement: Rich template generation strategy");
        maxExpressionSize = 3;
        return true;
      }

      return false;
    } finally {
      cache.clear();
    }
  }

  private boolean addTemplateToExtra(Template t) {
    // Do not add intervals.
    if (t.size() == 1) {
      return false;
    }
    for (Entry<CIdExpression, Rational> e : t.getLinearExpression()) {

      // Do not add templates whose coefficients are already overflowing.
      if (isOverflowing(t, e.getValue())) {
        return false;
      } else if (templateConstantThreshold != -1 &&
          e.getValue().compareTo(Rational.ofLong(templateConstantThreshold)) >= 1) {
        return false;
      }
    }

    boolean out = extraTemplates.add(t);
    if (out) {
      statistics.incWideningTemplatesGenerated();
    }
    return out;
  }

  public boolean isOverflowing(Template template, Rational v) {
    CSimpleType templateType = getTemplateType(template);
    if (templateType.getType().isIntegerType()) {
      BigInteger maxValue = cfa.getMachineModel()
          .getMaximalIntegerValue(templateType);
      BigInteger minValue = cfa.getMachineModel()
          .getMinimalIntegerValue(templateType);

      // The bound obtained is larger than the highest representable
      // value, ignore it.
      if (v.compareTo(Rational.ofBigInteger(maxValue)) == 1
          || v.compareTo(Rational.ofBigInteger(minValue)) == -1) {
        logger.log(Level.FINE, "Bound too high, ignoring",
            v);
        return true;
      }
    }
    return false;
  }

  public CSimpleType getTemplateType(Template t) {
    CExpression sum = null;

    // also note: there is an overall _expression_ type.
    // Wonder how that one is computed --- it actually depends on the order of
    // the operands.
    for (Entry<CIdExpression, Rational> e: t.getLinearExpression()) {
      CIdExpression expr = e.getKey();
      if (sum == null) {
        sum = expr;
      } else {
        sum = expressionBuilder.buildBinaryExpressionUnchecked(
            sum, expr, BinaryOperator.PLUS);
      }
    }
    assert sum != null;
    return (CSimpleType) sum.getExpressionType();
  }

  public Set<ASimpleDeclaration> getVarsForNode(CFANode node) {
    if (varFiltering == VarFilteringStrategy.ALL_LIVE) {
      return cfa.getLiveVariables().get().getLiveVariablesForNode(node).toSet();
    } else {
      return allVariables;
    }
  }

  private Formula normalizeLength(Formula f, int maxBitvectorSize,
      FormulaManagerView fmgr) {
    if (!(f instanceof BitvectorFormula)) {
      return f;
    }
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
        item = pfmgr.expressionToFormula(
            contextFormula, entry.getKey(), dummyEdge);
      } catch (UnrecognizedCCodeException e) {
        throw new UnsupportedOperationException();
      }
      if (!(item instanceof BitvectorFormula)) {
        continue;
      }
      BitvectorFormula b = (BitvectorFormula) item;
      length = Math.max(
          fmgr.getBitvectorFormulaManager().getLength(b),
          length);
    }
    return length;
  }

  private static class ToFormulaCacheKey {
    private final PathFormulaManager pathFormulaManager;
    private final FormulaManagerView formulaManagerView;
    private final Template template;
    private final PathFormula contextFormula;


    private ToFormulaCacheKey(
        PathFormulaManager pPathFormulaManager,
        FormulaManagerView pFormulaManagerView,
        Template pTemplate,
        PathFormula pContextFormula) {
      pathFormulaManager = pPathFormulaManager;
      formulaManagerView = pFormulaManagerView;
      template = pTemplate;
      contextFormula = pContextFormula;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      ToFormulaCacheKey that = (ToFormulaCacheKey) pO;
      return pathFormulaManager == that.pathFormulaManager
          && formulaManagerView == that.formulaManagerView &&
          Objects.equals(template, that.template) &&
          Objects.equals(contextFormula, that.contextFormula);
    }

    @Override
    public int hashCode() {
      return Objects
          .hash(pathFormulaManager, formulaManagerView, template,
              contextFormula);
    }

    @Override
    public String toString() {
      return "ToFormulaCacheKey{" +
          "pathFormulaManager=" + pathFormulaManager +
          ", formulaManagerView=" + formulaManagerView +
          ", template=" + template +
          ", contextFormula=" + contextFormula +
          '}';
    }
  }
}
