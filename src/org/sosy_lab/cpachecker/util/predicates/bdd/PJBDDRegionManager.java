// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.bdd;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.util.predicates.bdd.PJBDDRegion.unwrap;
import static org.sosy_lab.cpachecker.util.predicates.bdd.PJBDDRegion.wrap;

import com.google.common.primitives.ImmutableIntArray;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.BooleanFormulaVisitor;
import org.sosy_lab.pjbdd.api.Builders;
import org.sosy_lab.pjbdd.api.Builders.ParallelizationType;
import org.sosy_lab.pjbdd.api.Creator;
import org.sosy_lab.pjbdd.api.CreatorBuilder;
import org.sosy_lab.pjbdd.api.DD;

public class PJBDDRegionManager implements RegionManager {

  private final Region trueFormula;
  private final Region falseFormula;
  private final Creator bddCreator;

  public PJBDDRegionManager(Configuration pConfig) throws InvalidConfigurationException {
    BuildFromConfig buildFromConfig = new BuildFromConfig(pConfig);
    bddCreator = buildFromConfig.makeCreator();
    trueFormula = wrap(bddCreator.makeTrue());
    falseFormula = wrap(bddCreator.makeFalse());
  }

  @Override
  public boolean entails(Region f1, Region f2) {
    return bddCreator.makeImply(unwrap(f1), unwrap(f2)).isTrue();
  }

  @Override
  public Region createPredicate() {
    return wrap(bddCreator.makeVariable());
  }

  @Override
  public Region fromFormula(
      BooleanFormula pF, FormulaManagerView fmgr, Function<BooleanFormula, Region> atomToRegion) {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    if (bfmgr.isFalse(pF)) {
      return makeFalse();
    }
    if (bfmgr.isTrue(pF)) {
      return makeTrue();
    }

    try (FormulaToRegionConverter converter = new FormulaToRegionConverter(fmgr, atomToRegion)) {
      return wrap(bfmgr.visit(pF, converter));
    }
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region f) {
    DD bdd = unwrap(f);
    return Triple.of(
        wrap(bddCreator.makeIthVar(bdd.getVariable())), wrap(bdd.getHigh()), wrap(bdd.getLow()));
  }

  @Override
  public void printStatistics(PrintStream out) {
    out.print(bddCreator.getCreatorStats().prettyPrint());
  }

  @Override
  public String getVersion() {
    return bddCreator.getVersion();
  }

  @Override
  public void setVarOrder(ImmutableIntArray pOrder) {
    bddCreator.setVarOrder(pOrder.asList());
  }

  @Override
  public void reorder(VariableOrderingStrategy strategy) {
    throw new UnsupportedOperationException("dynamic reordering not yet implemented");
  }

  @Override
  public RegionBuilder builder(ShutdownNotifier pShutdownNotifier) {
    return new RegionBuilder();
  }

  @Override
  public Region makeTrue() {
    return trueFormula;
  }

  @Override
  public Region makeFalse() {
    return falseFormula;
  }

  @Override
  public Region makeNot(Region f) {
    return wrap(bddCreator.makeNot(unwrap(f)));
  }

  @Override
  public Region makeAnd(Region f1, Region f2) {
    return wrap(bddCreator.makeAnd(unwrap(f1), unwrap(f2)));
  }

  @Override
  public Region makeOr(Region f1, Region f2) {
    return wrap(bddCreator.makeOr(unwrap(f1), unwrap(f2)));
  }

  @Override
  public Region makeEqual(Region f1, Region f2) {
    return wrap(bddCreator.makeEqual(unwrap(f1), unwrap(f2)));
  }

  @Override
  public Region makeUnequal(Region f1, Region f2) {
    return wrap(bddCreator.makeUnequal(unwrap(f1), unwrap(f2)));
  }

  @Override
  public Region makeIte(Region f1, Region f2, Region f3) {
    return wrap(bddCreator.makeIte(unwrap(f1), unwrap(f2), unwrap(f3)));
  }

  @Override
  public Region makeExists(Region f1, Region... f2) {
    DD[] bddLevels = new DD[f2.length];
    IntStream.range(0, f2.length).forEach(i -> bddLevels[i] = unwrap(f2[i]));
    return wrap(bddCreator.makeExists(unwrap(f1), bddLevels));
  }

  @Override
  public Region replace(Region pRegion, List<Region> pOldPredicates, List<Region> pNewPredicates) {
    checkArgument(pOldPredicates.size() == pNewPredicates.size());
    DD bdd = unwrap(pRegion);
    for (int i = 0; i < pOldPredicates.size(); i++) {
      DD oldVar = bddCreator.makeIthVar(unwrap(pOldPredicates.get(i)).getVariable());
      DD newVar = bddCreator.makeIthVar(unwrap(pNewPredicates.get(i)).getVariable());
      bdd = bddCreator.makeReplace(bdd, oldVar, newVar);
    }
    return wrap(bdd);
  }

  @Options(prefix = "bdd.pjbdd")
  private static class BuildFromConfig {

    @Option(secure = true, description = "unique table's concurrency factor")
    @IntegerOption(min = 1)
    private int tableParallelism = 10000;

    @Option(secure = true, description = "initial variable count")
    @IntegerOption(min = 1)
    private int varCount = 100;

    @Option(secure = true, description = "increase factor for resizing tables")
    @IntegerOption(min = 1)
    private int increaseFactor = 1;

    @Option(secure = true, description = "size of the BDD cache.")
    @IntegerOption(min = 1)
    private int cacheSize = 0;

    @Option(
        secure = true,
        description =
            "Number of worker threads, Runtime.getRuntime().availableProcessors() default")
    @IntegerOption(min = 1)
    private int threads = Runtime.getRuntime().availableProcessors();

    @Option(
        secure = true,
        description =
            "Initial size of the BDD node table in percentage of available Java heap memory (only"
                + " used if initTableSize is 0).")
    private double initTableRatio = 0.001;

    @Option(
        secure = true,
        description = "Initial size of the BDD node table, use 0 for size based on initTableRatio.")
    @IntegerOption(min = 0)
    private int initTableSize = 0;

    @Option(
        secure = true,
        description =
            "Size of the BDD cache in relation to the node table size (set to 0 to use fixed BDD"
                + " cache size).")
    private double cacheRatio = 0.1;

    @Option(secure = true, description = "Use internal a int based bdd representation.")
    private boolean useInts = false;

    @Option(secure = true, description = "Use bdd chaining.")
    private boolean useChainedBDD = false;

    @Option(secure = true, description = "Disable thread safe bdd operations.")
    private boolean disableThreadSafety = false;

    private BuildFromConfig(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    private Creator makeCreator() {
      if (useInts) {
        CreatorBuilder intBuilder = Builders.intBuilder();
        resolveProperties(intBuilder);
        return intBuilder.build();
      }

      CreatorBuilder builder;
      if (useChainedBDD) {
        builder = Builders.cbddBuilder();
      } else {
        builder = Builders.bddBuilder();
      }
      resolveProperties(builder);
      return builder.build();
    }

    private void resolveProperties(CreatorBuilder pBuilder) {

      if ((initTableRatio <= 0 || initTableRatio >= 1) && initTableSize == 0) {
        initTableSize = 100000;
      }
      if (initTableSize == 0) {
        double size = Runtime.getRuntime().maxMemory() * initTableRatio / 5 / 8;
        initTableSize = (size > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) size;
      }

      if (cacheSize == 0) {
        cacheSize = (int) (initTableSize * cacheRatio);
        cacheSize = Math.max(cacheSize, 100000);
      }

      if (disableThreadSafety) {
        pBuilder.disableThreadSafety();
      }

      pBuilder
          .setParallelism(tableParallelism)
          .setVarCount(varCount)
          .setCacheSize(cacheSize)
          .setThreads(threads)
          .setTableSize(initTableSize)
          .setIncreaseFactor(increaseFactor);
      if (threads == 1) {
        pBuilder.setParallelizationType(ParallelizationType.NONE);
      }
    }
  }

  /**
   * Class for creating BDDs out of a formula. This class directly uses the BDD objects and their
   * manual reference counting, because for large formulas, the
   *
   * <p>All visit* methods from this class return methods that have not been ref'ed.
   */
  private class FormulaToRegionConverter implements BooleanFormulaVisitor<DD>, AutoCloseable {

    private final Function<BooleanFormula, Region> atomToRegion;
    private final BooleanFormulaManager bfmgr;

    private final Map<BooleanFormula, DD> cache = new HashMap<>();

    FormulaToRegionConverter(
        FormulaManagerView pFmgr, Function<BooleanFormula, Region> pAtomToRegion) {
      atomToRegion = pAtomToRegion;
      bfmgr = pFmgr.getBooleanFormulaManager();
    }

    @Override
    public void close() {
      cache.clear();
      bddCreator.shutDown();
    }

    @Override
    public DD visitConstant(boolean pB) {
      return pB ? bddCreator.makeTrue() : bddCreator.makeFalse();
    }

    @Override
    public DD visitBoundVar(BooleanFormula pBooleanFormula, int pI) {
      throw new UnsupportedOperationException("Not yet implemented"); // TODO
    }

    @Override
    public DD visitNot(BooleanFormula pBooleanFormula) {
      return bddCreator.makeNot(convert(pBooleanFormula));
    }

    @Override
    public DD visitAnd(List<BooleanFormula> pList) {
      if (pList.isEmpty()) {
        return bddCreator.makeFalse();
      }

      DD result = bddCreator.makeTrue();
      for (BooleanFormula bFormula : pList) {
        result = bddCreator.makeAnd(result, convert(bFormula));
      }
      return result;
    }

    @Override
    public DD visitOr(List<BooleanFormula> pList) {
      if (pList.isEmpty()) {
        return bddCreator.makeFalse();
      }

      DD result = bddCreator.makeFalse();
      for (BooleanFormula bFormula : pList) {
        result = bddCreator.makeOr(result, convert(bFormula));
      }
      return result;
    }

    @Override
    public DD visitXor(BooleanFormula pBooleanFormula, BooleanFormula pBooleanFormula1) {
      return bddCreator.makeXor(convert(pBooleanFormula), convert(pBooleanFormula1));
    }

    @Override
    public DD visitEquivalence(BooleanFormula pBooleanFormula, BooleanFormula pBooleanFormula1) {
      return bddCreator.makeEqual(convert(pBooleanFormula), convert(pBooleanFormula1));
    }

    @Override
    public DD visitImplication(BooleanFormula pBooleanFormula, BooleanFormula pBooleanFormula1) {
      return bddCreator.makeImply(convert(pBooleanFormula), convert(pBooleanFormula1));
    }

    @Override
    public DD visitIfThenElse(
        BooleanFormula pBooleanFormula1,
        BooleanFormula pBooleanFormula2,
        BooleanFormula pBooleanFormula3) {
      return bddCreator.makeIte(
          convert(pBooleanFormula1), convert(pBooleanFormula2), convert(pBooleanFormula3));
    }

    @Override
    public DD visitQuantifier(
        Quantifier pQuantifier,
        BooleanFormula pBooleanFormula,
        List<Formula> pList,
        BooleanFormula pBooleanFormula1) {
      throw new UnsupportedOperationException("Not yet implemented"); // TODO
    }

    @Override
    public DD visitAtom(
        BooleanFormula pBooleanFormula, FunctionDeclaration<BooleanFormula> pFunctionDeclaration) {
      return unwrap(atomToRegion.apply(pBooleanFormula));
    }

    // Convert one BooleanFormula (recursively)
    // and return a result that is also put in the cache.
    private DD convert(BooleanFormula pOperand) {
      DD operand = cache.get(pOperand);
      if (operand == null) {
        operand = bfmgr.visit(pOperand, this);
        cache.put(pOperand, operand);
      }
      return operand;
    }
  }

  private class RegionBuilder implements RegionCreator.RegionBuilder {

    private final List<DD> cubes = new ArrayList<>();
    private DD currentCube;

    @Override
    public void startNewConjunction() {
      checkState(currentCube == null);
      currentCube = bddCreator.makeTrue();
    }

    @Override
    public void addPositiveRegion(Region r) {
      checkState(currentCube != null);
      currentCube = bddCreator.makeAnd(currentCube, unwrap(r));
    }

    @Override
    public void addNegativeRegion(Region r) {
      checkState(currentCube != null);
      currentCube = bddCreator.makeAnd(currentCube, bddCreator.makeNot(unwrap(r)));
    }

    @Override
    public void finishConjunction() {
      checkState(currentCube != null);

      for (int i = 0; i < cubes.size(); i++) {
        DD cubeAtI = cubes.get(i);

        if (cubeAtI == null) {
          cubes.set(i, currentCube);
          currentCube = null;
          return;
        } else {
          currentCube = bddCreator.makeOr(currentCube, cubeAtI);
          cubes.set(i, null);
        }
      }

      if (currentCube != null) {
        cubes.add(currentCube);
        currentCube = null;
      }
    }

    @Override
    public Region getResult() {
      checkState(currentCube == null);
      if (cubes.isEmpty()) {
        return falseFormula;
      } else {

        DD[] clauses = cubes.stream().filter(bdd -> bdd != null).toArray(DD[]::new);

        DD result = bddCreator.makeFalse();

        for (DD bdd : clauses) {
          result = bddCreator.makeOr(result, bdd);
        }

        cubes.clear();

        cubes.add(result);
        return wrap(result);
      }
    }

    @Override
    public void close() {
      checkState(currentCube == null);
      cubes.clear();
    }
  }
}
