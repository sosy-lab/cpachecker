// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.precision.RefinablePrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithConcreteCex;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecisionAdjustment.PrecAdjustmentOptions;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecisionAdjustment.PrecAdjustmentStatistics;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation.ValueTransferOptions;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.ConstraintsStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.SymbolicValueAnalysisPrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.SymbolicValueAnalysisPrecisionAdjustment.SymbolicStatistics;
import org.sosy_lab.cpachecker.cpa.value.symbolic.SymbolicValueAssigner;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeVisitor;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.states.MemoryLocationValueHandler;

@Options(prefix = "cpa.value")
public class ValueAnalysisCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM,
        StatisticsProvider,
        ProofCheckerCPA,
        ConfigurableProgramAnalysisWithConcreteCex {

  private enum UnknownValueStrategy {
    /** This strategy discards all unknown values from the value analysis state */
    DISCARD,
    /**
     * This strategy introduces a new {@link SymbolicValue} for each unknown value. Symbolic values
     * should probably be used in conjunction with the ConstraintsCPA. Otherwise, symbolic values
     * will be created, but not evaluated.
     */
    INTRODUCE_SYMBOLIC,
  }

  @Option(secure=true, name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for ValueAnalysisCPA")
  private String mergeType = "SEP";

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"SEP", "JOIN", "NEVER", "EQUALS"},
      description = "which stop operator to use for ValueAnalysisCPA")
  private String stopType = "SEP";

  @Option(secure=true, description="get an initial precision from file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path initialPrecisionFile = null;

 @Option(
     secure = true,
     description = "build initial precision from invariants in supplied correctness witness")
 @FileOption(Type.OPTIONAL_INPUT_FILE)
 private Path initialPrecisionWitness = null;

 @Option(
     secure = true,
     toUppercase = true,
     values = {"NONE", "PRECISION_FILE", "WITNESS"},
     description = "which source to use for the initial precision"
 )
 // default need to be "PRECISION_FILE" for backwards-compatibility
 private String initialPrecisionSource = "PRECISION_FILE";

  @Option(
      secure = true,
      name = "unknownValueHandling",
      description = "Tells the value analysis how to handle unknown values.")
  private UnknownValueStrategy unknownValueStrategy = UnknownValueStrategy.DISCARD;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ValueAnalysisCPA.class);
  }

  private VariableTrackingPrecision precision;
  private final ValueAnalysisCPAStatistics statistics;
  private final StateToFormulaWriter writer;

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  private final ValueAnalysisConcreteErrorPathAllocator errorPathAllocator;

  private final MemoryLocationValueHandler unknownValueHandler;
  private final ConstraintsStrengthenOperator constraintsStrengthenOperator;
  private final ValueTransferOptions transferOptions;
  private final PrecAdjustmentOptions precisionAdjustmentOptions;
  private final PrecAdjustmentStatistics precisionAdjustmentStatistics;

  private SymbolicStatistics symbolicStats;

  private ValueAnalysisCPA(Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA cfa) throws InvalidConfigurationException {
    super(DelegateAbstractDomain.<ValueAnalysisState>getInstance(), null);
    this.config           = config;
    this.logger           = logger;
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa              = cfa;

    config.inject(this, ValueAnalysisCPA.class);

    precision           = generateInitialPrecision();
    statistics          = new ValueAnalysisCPAStatistics(this, config);
    writer = new StateToFormulaWriter(config, logger, shutdownNotifier, cfa);
    errorPathAllocator = new ValueAnalysisConcreteErrorPathAllocator(config, logger, cfa.getMachineModel());

    unknownValueHandler = createUnknownValueHandler();

    constraintsStrengthenOperator =
        new ConstraintsStrengthenOperator(config, logger);
    transferOptions = new ValueTransferOptions(config);
    precisionAdjustmentOptions = new PrecAdjustmentOptions(config, cfa);
    precisionAdjustmentStatistics = new PrecAdjustmentStatistics();
  }

  private MemoryLocationValueHandler createUnknownValueHandler()
      throws InvalidConfigurationException {
    switch (unknownValueStrategy) {
      case DISCARD:
        return new UnknownValueAssigner();
      case INTRODUCE_SYMBOLIC:
        return new SymbolicValueAssigner(config);
      default:
        throw new AssertionError("Unhandled strategy: " + unknownValueStrategy);
    }
  }

  private VariableTrackingPrecision generateInitialPrecision() throws InvalidConfigurationException {
    Objects.requireNonNull(initialPrecisionSource);

    if (initialPrecisionSource.equals("NONE")) {
      return emptyInitialPrecision();
    } else if (initialPrecisionSource.equals("PRECISION_FILE")) {
      if (initialPrecisionFile == null) {
        // silent fallback for backwards-compatibility
        return emptyInitialPrecision();
      }
      return initialPrecisionFromPrecisionFile();
    } else if (initialPrecisionSource.equals("WITNESS")) {
      if (initialPrecisionWitness == null) {
        logger.log(Level.WARNING,
            "Properties state to use initial precision from correctness witness, but no witness file was provided!");
        return emptyInitialPrecision();
      }
      return initialPrecisionFromWitness();
    } else {
      throw new AssertionError("Unknown initial precision source");
    }
  }

  private VariableTrackingPrecision emptyInitialPrecision() throws InvalidConfigurationException {
    return VariableTrackingPrecision.createStaticPrecision(config, cfa.getVarClassification(), getClass());
  }

  private VariableTrackingPrecision initialPrecisionFromPrecisionFile()
      throws InvalidConfigurationException {
    // create precision with empty, refinable component precision
    VariableTrackingPrecision initialPrecision =
        VariableTrackingPrecision.createRefineablePrecision(config, emptyInitialPrecision());

    // refine the refinable component precision with increment from file
    return initialPrecision.withIncrement(restoreMappingFromFile(cfa));
  }

  private VariableTrackingPrecision initialPrecisionFromWitness()
      throws InvalidConfigurationException {
    Objects.requireNonNull(initialPrecisionWitness);

    try {
      WitnessInvariantsExtractor invariantsExtractor =
          new WitnessInvariantsExtractor(config, logger,
              cfa, shutdownNotifier, initialPrecisionWitness);

      Set<ExpressionTreeLocationInvariant> invariants =
          invariantsExtractor.extractInvariantsFromReachedSet();

      var precIncrement = ImmutableSetMultimap.<CFANode, MemoryLocation>builder();
      for (var inv : invariants) {
        ImmutableList<MemoryLocation> memLocs = memoryLocsInExpressionTree(inv.asExpressionTree());
        precIncrement.putAll(inv.getLocation(), memLocs);
      }

      VariableTrackingPrecision prec =
          VariableTrackingPrecision.createRefineablePrecision(config, emptyInitialPrecision());
      return prec.withIncrement(precIncrement.build());
    }
    catch (CPAException e) {
      // no special handling needed
    }
    catch (InterruptedException e) {
      // returned from blocking method, no handling needed
    }

    return emptyInitialPrecision();
  }

  private ImmutableList<MemoryLocation> memoryLocsInExpressionTree(ExpressionTree<Object> expTree) {
    var memLocs = new ImmutableList.Builder<MemoryLocation>();

    expTree.accept(new ExpressionTreeVisitor<Object, Void, NoException>() {
      @Override
      public Void visit(And<Object> pAnd) throws NoException {
        for (var child : pAnd) {
          child.accept(this);
        }
        return null;
      }

      @Override
      public Void visit(Or<Object> pOr) throws NoException {
        for (var child : pOr) {
          child.accept(this);
        }
        return null;
      }

      @Override
      public Void visit(LeafExpression<Object> pLeafExpression) throws NoException {
        if (pLeafExpression.getExpression() instanceof CExpression) {
          // We only handle CExpressions for now
          var exp = (CExpression)pLeafExpression.getExpression();
          memLocs.addAll(memoryLocationsInExpression(exp));
        }
        return null;
      }

      @Override
      public Void visitTrue() throws NoException {
        // nothing to do
        return null;
      }

      @Override
      public Void visitFalse() throws NoException {
        // nothing to do
        return null;
      }
    });

    return memLocs.build();
  }

  private ImmutableList<MemoryLocation> memoryLocationsInExpression(CExpression exp) {
    var memLocs = new ImmutableList.Builder<MemoryLocation>();

    exp.accept(new CExpressionVisitor<Void, NoException>() {
      @Override
      public Void visit(CBinaryExpression pIastBinaryExpression) throws NoException {
        pIastBinaryExpression.getOperand1().accept(this);
        pIastBinaryExpression.getOperand2().accept(this);
        return null;
      }

      @Override
      public Void visit(CCastExpression pIastCastExpression) throws NoException {
        pIastCastExpression.getOperand().accept(this);
        return null;
      }

      @Override
      public Void visit(CCharLiteralExpression pIastCharLiteralExpression) throws NoException {
        // nothing to do
        return null;
      }

      @Override
      public Void visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws NoException {
        // nothing to do
        return null;
      }

      @Override
      public Void visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
          throws NoException {
        // nothing to do
        return null;
      }

      @Override
      public Void visit(CStringLiteralExpression pIastStringLiteralExpression) throws NoException {
        // nothing to do
        return null;
      }

      @Override
      public Void visit(CTypeIdExpression pIastTypeIdExpression) throws NoException {
        // nothing to do
        return null;
      }

      @Override
      public Void visit(CUnaryExpression pIastUnaryExpression) throws NoException {
        pIastUnaryExpression.getOperand().accept(this);
        return null;
      }

      @Override
      public Void visit(CImaginaryLiteralExpression PIastLiteralExpression) throws NoException {
        // nothing to do
        return null;
      }

      @Override
      public Void visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws NoException {
        // CAddressOfLabelExpression is not ISO C, but a GCC extensions
        // nothing to do
        return null;
      }

      @Override
      public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression)
          throws NoException {
        if (pIastArraySubscriptExpression.getArrayExpression() instanceof CIdExpression &&
            pIastArraySubscriptExpression.getSubscriptExpression() instanceof CIntegerLiteralExpression) {
          // the case where directly know the referenced memory location (including the offset)
          // possible future improvement: also handle cases where the subscript expression can be
          // evaluated statically to an integer
          var arrayExp = (CIdExpression)pIastArraySubscriptExpression.getArrayExpression();
          String arrayName = arrayExp.getDeclaration() != null ?
                             arrayExp.getDeclaration().getQualifiedName() :
                             arrayExp.getName();
          var indexExp = (CIntegerLiteralExpression)pIastArraySubscriptExpression.getSubscriptExpression();
          var machineModel = cfa.getMachineModel();

          try {
            var arrayType = (CArrayType)arrayExp.getExpressionType();
            CType arrayElemType = arrayType.getType();
            BigInteger arrayElemSize = machineModel.getSizeof(arrayElemType);
            long arrayOffset = arrayElemSize.longValueExact() * indexExp.asLong();
            memLocs.add(MemoryLocation.valueOf(arrayName, arrayOffset));
          } catch (ArithmeticException e) {
            logger.logException(Level.WARNING, e, "unreasonable large field offset");
          }
        } else {
          pIastArraySubscriptExpression.getArrayExpression().accept(this);
          pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
        }

        return null;
      }

      @Override
      public Void visit(CFieldReference pIastFieldReference) throws NoException {
        if (pIastFieldReference.getFieldOwner() instanceof CIdExpression) {
          var fieldOwner = (CIdExpression)pIastFieldReference.getFieldOwner();
          CType fieldOwnerType = fieldOwner.getExpressionType().getCanonicalType();
          String fieldOwnerVarName = fieldOwner.getDeclaration() != null ?
                                     fieldOwner.getDeclaration().getQualifiedName() :
                                     fieldOwner.getName();

          if (fieldOwnerType instanceof CCompositeType) {
            var structType = (CCompositeType)fieldOwnerType;
            var machineModel = cfa.getMachineModel();
            BigInteger fieldOffset = machineModel.getFieldOffsetInBits(structType, pIastFieldReference.getFieldName());
            try {
              long fOffset = fieldOffset.longValueExact();
              memLocs.add(MemoryLocation.valueOf(fieldOwnerVarName, fOffset));
            } catch (ArithmeticException e) {
              logger.logException(Level.WARNING, e, "unreasonable large field offset");
            }
          } else {
            // Field owner is of pointer type or has unknown type.
            // We only know that value analysis should track the variable fieldOwner refers to
            // directly.
            memLocs.add(MemoryLocation.valueOf(fieldOwnerVarName));
          }
        } else {
          // We do not really know which struct is accessed.
          // At least track all variables which are needed to resolve this access.
          pIastFieldReference.getFieldOwner().accept(this);
        }

        return null;
      }

      @Override
      public Void visit(CIdExpression pIastIdExpression) throws NoException {
        // The easy case, just directly track the referenced variable.
        // As it is only one simple variable, there is no offset to consider.
        String varName = pIastIdExpression.getDeclaration() != null ?
                         pIastIdExpression.getDeclaration().getQualifiedName() :
                         pIastIdExpression.getName();
        memLocs.add(MemoryLocation.valueOf(varName));

        return null;
      }

      @Override
      public Void visit(CPointerExpression pointerExpression) throws NoException {
        pointerExpression.getOperand().accept(this);
        return null;
      }

      @Override
      public Void visit(CComplexCastExpression complexCastExpression) throws NoException {
        complexCastExpression.getOperand().accept(this);
        return null;
      }
    });

    return memLocs.build();
  }

  private Multimap<CFANode, MemoryLocation> restoreMappingFromFile(CFA pCfa) {
    Multimap<CFANode, MemoryLocation> mapping = HashMultimap.create();

    List<String> contents = null;
    try {
      contents = Files.readAllLines(initialPrecisionFile, Charset.defaultCharset());
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not read precision from file named " + initialPrecisionFile);
      return mapping;
    }

    Map<Integer, CFANode> idToCfaNode = createMappingForCFANodes(pCfa);
    final Pattern CFA_NODE_PATTERN = Pattern.compile("N([0-9][0-9]*)");

    CFANode location = getDefaultLocation(idToCfaNode);
    for (String currentLine : contents) {
      if (currentLine.trim().isEmpty()) {
        continue;

      } else if(currentLine.endsWith(":")) {
        String scopeSelectors = currentLine.substring(0, currentLine.indexOf(":"));
        Matcher matcher = CFA_NODE_PATTERN.matcher(scopeSelectors);
        if (matcher.matches()) {
          location = idToCfaNode.get(Integer.parseInt(matcher.group(1)));
        }

      } else {
        mapping.put(location, MemoryLocation.valueOf(currentLine));
      }
    }

    return mapping;
  }

  private CFANode getDefaultLocation(Map<Integer, CFANode> idToCfaNode) {
    return idToCfaNode.values().iterator().next();
  }

  private Map<Integer, CFANode> createMappingForCFANodes(CFA pCfa) {
    Map<Integer, CFANode> idToNodeMap = new HashMap<>();
    for (CFANode n : pCfa.getAllNodes()) {
      idToNodeMap.put(n.getNodeNumber(), n);
    }
    return idToNodeMap;
  }

  public void injectRefinablePrecision() throws InvalidConfigurationException {

    // replace the full precision with an empty, refinable precision
    if (!(precision instanceof RefinablePrecision)) {
      precision = VariableTrackingPrecision.createRefineablePrecision(config, precision);
    }
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }

  @Override
  public ValueAnalysisTransferRelation getTransferRelation() {
    return new ValueAnalysisTransferRelation(
        logger,
        cfa,
        transferOptions,
        unknownValueHandler,
        constraintsStrengthenOperator,
        statistics);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new ValueAnalysisState(cfa.getMachineModel());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    if (unknownValueStrategy.equals(UnknownValueStrategy.INTRODUCE_SYMBOLIC)) {
      symbolicStats = new SymbolicStatistics();
      return new SymbolicValueAnalysisPrecisionAdjustment(
          statistics,
          cfa,
          precisionAdjustmentOptions,
          precisionAdjustmentStatistics,
          Preconditions.checkNotNull(symbolicStats));
    } else {
      return new ValueAnalysisPrecisionAdjustment(
          statistics, cfa, precisionAdjustmentOptions, precisionAdjustmentStatistics);
    }
  }

  public Configuration getConfiguration() {
    return config;
  }

  public LogManager getLogger() {
    return logger;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }

  public CFA getCFA() {
    return cfa;
  }

  @Override
  public Reducer getReducer() {
    return new ValueAnalysisReducer();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
    pStatsCollection.add(precisionAdjustmentStatistics);
    if (symbolicStats != null) {
      pStatsCollection.add(symbolicStats);
    }
    pStatsCollection.add(constraintsStrengthenOperator);
    writer.collectStatistics(pStatsCollection);
  }

  @Override
  public ConcreteStatePath createConcreteStatePath(ARGPath pPath) {
    return errorPathAllocator.allocateAssignmentsToPath(pPath);
  }
}