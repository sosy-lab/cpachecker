package org.sosy_lab.cpachecker.cpa.stator.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Convert the alias state to formula.
 *
 * NOTE: this class might rely too much on the existing
 * {@code ctoformula} package implementation.
 */
public class MemoryToFormulaManager {
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private SSAMap.SSAMapBuilder outMapBuilder;

  public MemoryToFormulaManager(
      FormulaManagerView pFormulaManagerView) {
    fmgr = pFormulaManagerView;
    bfmgr = fmgr.getBooleanFormulaManager();
  }

  public PathFormula getFormulaApproximation(
      AliasState state, SSAMap outputMap, final SSAMap inputMap) {
    outMapBuilder = outputMap.withDefault(1).builder();

    AbstractMemoryAddress createAssignmentOn = state.getCreateAssignmentOn();

    List<BooleanFormula> lhsConstraints = new ArrayList<>();

    // Processing left-hand-side aliasing constraints.
    if (createAssignmentOn != AbstractMemoryAddress.BOTTOM) {

      // Processing *x = alias1 OR alias2 OR alias3 OR ...
      // TODO: Check for working with double/triple/etc pointers.
      Preconditions.checkState(createAssignmentOn.size() == 1);
      MemorySegment aliasFrom = createAssignmentOn.iterator().next();

      AbstractMemoryAddress aliasedTo = AbstractMemoryAddress.BOTTOM;
      for (MemorySegment aliasedFrom : createAssignmentOn) {
        aliasedTo = aliasedTo.join(state.getValuesStored(aliasedFrom));
      }

      List<BooleanFormula> lhsAliasPossibilities = new ArrayList<>();

      for (MemorySegment aliasTo : state.getValuesStored(aliasFrom)) {
        BooleanFormula constr = equal(
            makePointerVar(aliasFrom, false, outputMap),
            makeVar(aliasTo, true, outputMap));

        // If the pointer is aliased to one of the possible values,
        // everything else must stay the same under the transition.
        List<BooleanFormula> othersAreSame = new ArrayList<>();
        for (MemorySegment otherAliasTo : state.getValuesStored(aliasFrom)) {
          // Iterating over everything else, apart from what we've just processed.
          if (otherAliasTo.equals(aliasTo)) {
            continue;
          }
          othersAreSame.add(equal(
              makeVar(otherAliasTo, true, outputMap),
              makeVar(otherAliasTo, false, outputMap)
          ));
        }
        lhsAliasPossibilities.add(
            bfmgr.and(constr, bfmgr.and(othersAreSame))
        );
      }
      lhsConstraints.add(bfmgr.or(lhsAliasPossibilities));
    }

    // Processing right-hand-side aliasing constraints.
    List<BooleanFormula> rhsConstraints = new ArrayList<>();
    for (Map.Entry<MemorySegment, AbstractMemoryAddress> entry : state) {
      MemorySegment address = entry.getKey();
      AbstractMemoryAddress values = entry.getValue();
      final NumeralFormula aliasFrom = makePointerVar(address, false, inputMap);

      BooleanFormula aliasConstraints = bfmgr.or(Lists.transform(
          ImmutableList.copyOf(values),
          new Function<MemorySegment, BooleanFormula>() {
            @Override
            public BooleanFormula apply(MemorySegment aliasTo) {
              return equal(aliasFrom, makeVar(aliasTo, false, inputMap));
            }
          }
      ));
      rhsConstraints.add(aliasConstraints);
    }

    ImmutableList<BooleanFormula> outConstraintsList = ImmutableList
        .<BooleanFormula>builder()
        .addAll(lhsConstraints)
        .addAll(rhsConstraints).build();

    BooleanFormula outConstraints = bfmgr.and(outConstraintsList);

    return new PathFormula(
        outConstraints, outMapBuilder.build(), PointerTargetSet.emptyPointerTargetSet(), 1);
  }

  private BooleanFormula equal(NumeralFormula a, NumeralFormula b) {
    return fmgr.getRationalFormulaManager().equal(a, b);
  }

  private NumeralFormula makeVar(
      MemorySegment segment, boolean makeFreshVar, SSAMap map) {
    return makeVar(segment, makeFreshVar, map, false);
  }

  private NumeralFormula makePointerVar(
      MemorySegment segment,
      boolean makeFreshVar, SSAMap map) {
    return makeVar(segment, makeFreshVar, map, true);
  }

  private NumeralFormula makeVar(
      MemorySegment segment, boolean makeFreshVar, SSAMap map, boolean isPointer) {
    String varName;
    if (isPointer) {
      varName = pointerAddressToVarName(segment);
    } else {
      varName = varAddressToVarName(segment);
    }
    int ssaIndex = map.getIndex(varName);
    if (makeFreshVar) {
      ssaIndex++;
      outMapBuilder = outMapBuilder.setIndex(
          varName, segment.getDeclaration().getType(), ssaIndex);
    }

    return fmgr.makeVariable(FormulaType.RationalType, varName, ssaIndex);
  }

  private String pointerAddressToVarName(MemorySegment adr) {
    AVariableDeclaration decl = adr.getDeclaration();
    String functionName = decl.getQualifiedName().split("::")[0];
    String varName = decl.toASTString().split(" ")[1].replaceAll(";", "");
    return CtoFormulaConverter.scoped(varName, functionName);
  }

  private String varAddressToVarName(MemorySegment adr) {
    return adr.getDeclaration().getQualifiedName();
  }
}
