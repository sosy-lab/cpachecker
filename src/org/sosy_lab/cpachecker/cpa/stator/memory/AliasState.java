package org.sosy_lab.cpachecker.cpa.stator.memory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.ImmutableMapMerger;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Reasons about values at concrete memory addresses.
 */
public class AliasState implements
    LatticeAbstractState<AliasState>,
    Iterable<Entry<MemorySegment, AbstractMemoryAddress>>,
    Graphable,
    FormulaReportingState {

  /**
   * Datastructure description:
   *
   * -> block is not in the map: TOP
   *
   * Note that under that representation empty map represents the TOP
   * memory state.
   */
  private final ImmutableMap<MemorySegment, AbstractMemoryAddress> data;

  /**
   * Mapping from string identifier to addresses.
   */
  private final ImmutableMap<String, MemorySegment> varMapping;

  /**
   * Set of addresses invalidated by the currently processed assignment.
   */
  private final AbstractMemoryAddress createAssignmentOn;

  private AliasState(
      ImmutableMap<MemorySegment, AbstractMemoryAddress> pData,
      ImmutableMap<String, MemorySegment> pVarMapping,
      AbstractMemoryAddress pCreateAssignmentOn) {

    data = pData;
    varMapping = pVarMapping;
    createAssignmentOn = pCreateAssignmentOn;
  }

  public static final AliasState TOP = new AliasState(
      ImmutableMap.<MemorySegment, AbstractMemoryAddress>of(),
      ImmutableMap.<String, MemorySegment>of(),
      AbstractMemoryAddress.BOTTOM);

  @Override
  public boolean isLessOrEqual(AliasState otherState) {
    for (Entry<MemorySegment, AbstractMemoryAddress> entry : data.entrySet()) {
      MemorySegment block = entry.getKey();
      AbstractMemoryAddress values = entry.getValue();
      AbstractMemoryAddress otherValues = otherState.data.get(block);

      // Null means TOP.
      if ((otherValues == null && values != AbstractMemoryAddress.TOP)
          || !values.isLessOrEqual(otherValues)) return false;
    }

    for (Entry<String, MemorySegment> entry : varMapping.entrySet()) {
      String var = entry.getKey();
      MemorySegment segment = entry.getValue();
      MemorySegment otherSegment = otherState.varMapping.get(var);
      if (otherSegment == null) {
        return false;
      }
      Preconditions.checkState(segment.equals(otherSegment));
    }
    return true;
  }


  public AliasState withCreateAssignmentOn(AbstractMemoryAddress address) {
    return new AliasState(data, varMapping, address);
  }

  @Override
  public AliasState join(AliasState otherState) {
    if (isLessOrEqual(otherState)) return otherState;

    ImmutableMap.Builder<MemorySegment, AbstractMemoryAddress> builder =
        ImmutableMap.builder();

    // NOTE: We only know information about the memory addresses
    // which occur in both states, hence we iterate over only one of the alias
    // states.
    for (Entry<MemorySegment, AbstractMemoryAddress> entry : data.entrySet()) {
      MemorySegment block = entry.getKey();
      AbstractMemoryAddress value = entry.getValue();

      /**
       * Note that the block missing from the graph represents the TOP state,
       * and TOP stays TOP under the merge.
       */
      AbstractMemoryAddress otherValue = otherState.data.get(block);
      if (otherValue != null) {
        builder.put(block, value.join(otherValue));
      }
    }
    AliasState out = new AliasState(
        builder.build(),
        withUpdate(otherState.varMapping),
        createAssignmentOn.join(otherState.createAssignmentOn)
    );
    if (out.equals(otherState)) return otherState;
    return out;
  }

  private ImmutableMap<String, MemorySegment> withUpdate(
      ImmutableMap<String, MemorySegment> memoryUpdate) {
    return ImmutableMapMerger.merge(
        varMapping,
        memoryUpdate,
        new ImmutableMapMerger.MergeFunc<String, MemorySegment>() {
          public MemorySegment apply(String key, MemorySegment a, MemorySegment b) {
            Preconditions.checkState(a.equals(b));
            return a;
          }
        }
    );
  }

  /**
   * @return R-value for the associated {@param memoryBlock}
   */
  public AbstractMemoryAddress getValuesStored(MemorySegment memoryBlock) {
    AbstractMemoryAddress values = data.get(memoryBlock);
    if (values == null) {
      return AbstractMemoryAddress.TOP;
    }
    return data.get(memoryBlock);
  }


  /** SETTERS */

  /**
   * Update the r-value stored at {@param block} to include {@param rValue}
   * @return Updated communication word.
   */
  public AliasState addValueToBlock(
      final MemorySegment block, AbstractMemoryAddress rValue) {

    if (!data.containsKey(block)) return setBlockValue(block, rValue);

    return new AliasState(
        ImmutableMapMerger.merge(
            data,
            ImmutableMap.of(block, rValue),
            new ImmutableMapMerger.MergeFunc<MemorySegment, AbstractMemoryAddress>() {
              @Override
              public AbstractMemoryAddress apply(MemorySegment key,
                  AbstractMemoryAddress oldData,
                  AbstractMemoryAddress rValue) {
                if (key.equals(block)) return oldData.join(rValue);
                return oldData;
              }
            }),
        varMapping,
        createAssignmentOn
    );
  }

  public AliasState setAddress(String identifier, MemorySegment address) {
    return new AliasState(
      data, withUpdate(ImmutableMap.of(identifier, address)), AbstractMemoryAddress.BOTTOM
    );
  }

  public MemorySegment getAddress(String identifer) {
    return varMapping.get(identifer);
  }

  /**
   * Sets the r-value stored at {@param block} to {@param rValue}
   * @return Updated communication word.
   */
  public AliasState setBlockValue(
      MemorySegment block, AbstractMemoryAddress rValue) {
    return new AliasState(
        ImmutableMap.
            <MemorySegment, AbstractMemoryAddress>builder()
            .putAll(data)
            .put(block, rValue)
            .build(),
        varMapping,
        createAssignmentOn
    );
  }

  @Override
  public Iterator<Entry<MemorySegment, AbstractMemoryAddress>> iterator() {
    return data.entrySet().iterator();
  }

  @Override
  public String toString() {
    return data.toString();
  }

  @Override
  public String toDOTLabel() {
    return toString();
  }

  /**
   * Convert the state to the formula.
   *
   * TODO: the implementation below for generating the variable name
   * is really hacky and relies on the existing CToFormulaConverter
   * implementation.
   */
  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView fmgr) {

    // TODO: move the logic to the different class....
    // Code should not live here, it also makes it difficult to debug.
    BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
    BooleanFormula lhsConstraint = bfmgr.makeBoolean(true);

    if (createAssignmentOn != AbstractMemoryAddress.BOTTOM) {
      // TODO: hack for now, let's fix later.
      Preconditions.checkState(createAssignmentOn.size() == 1);

      AbstractMemoryAddress aliasedTo = AbstractMemoryAddress.BOTTOM;
      for (MemorySegment aliasedFrom : createAssignmentOn) {
        aliasedTo = aliasedTo.join(data.get(aliasedFrom));
      }

      // Multiple alias-to options.
      lhsConstraint = null;

      MemorySegment aliasFrom = createAssignmentOn.iterator().next();

      for (MemorySegment aliasTo : data.get(aliasFrom)) {
        BooleanFormula constr = fmgr.getRationalFormulaManager().equal(
            fmgr.makeVariable(
                FormulaType.RationalType,
                pointerAddressToVarName(aliasFrom),
                2
            ),
            fmgr.makeVariable(
                FormulaType.RationalType,
                varAddressToVarName(aliasTo),
                2
            )
        );

        // If the pointer is aliased to one of the possible values,
        // everything else must stay the same under the transition.
        List<BooleanFormula> othersAreSame = new LinkedList<>();
        for (MemorySegment otherAliasTo : data.get(aliasFrom)) {
          if (otherAliasTo.equals(aliasTo)) {
            continue;
          }
          BooleanFormula otherSame = fmgr.getRationalFormulaManager().equal(
              fmgr.makeVariable(
                  FormulaType.RationalType,
                  varAddressToVarName(otherAliasTo),
                  1
              ),
              fmgr.makeVariable(
                  FormulaType.RationalType,
                  varAddressToVarName(otherAliasTo),
                  2
              )
          );
          othersAreSame.add(otherSame);
        }
        constr = bfmgr.and(constr, bfmgr.and(othersAreSame));

        if (lhsConstraint == null) {
          lhsConstraint = constr;
        } else {
          lhsConstraint = bfmgr.or(lhsConstraint, constr);
        }
      }
    }

    List<BooleanFormula> constraints = new LinkedList<>();

    for (Entry<MemorySegment, AbstractMemoryAddress> entry : this) {
      MemorySegment address = entry.getKey();
      String scopedVarName = pointerAddressToVarName(address);

      AbstractMemoryAddress values = entry.getValue();
      NumeralFormula aliasFrom = fmgr.makeVariable(
          FormulaType.RationalType,
          scopedVarName,
          1
      );

      // Ignore the pointer if it is not aliased to anything.
      if (values.size() == 0) continue;

      BooleanFormula aliases = null;
      for (MemorySegment alias : values) {
        NumeralFormula aliasTo = fmgr.makeVariable(
            FormulaType.RationalType,
            varAddressToVarName(alias),
            1
        );

        BooleanFormula constr = fmgr.getRationalFormulaManager().equal(aliasFrom, aliasTo);
        if (aliases == null) {
          aliases = constr;
        } else {
          aliases = bfmgr.or(aliases, constr);
        }
        aliases = fmgr.getBooleanFormulaManager().or(
            aliases,
            fmgr.getRationalFormulaManager().equal(aliasFrom, aliasTo)
        );
      }
      constraints.add(aliases);
    }

    BooleanFormula rhsConstraints =
        fmgr.getBooleanFormulaManager().and(constraints);

    return bfmgr.and(lhsConstraint, rhsConstraints);
  }

  private String pointerAddressToVarName(MemorySegment adr) {
    CVariableDeclaration decl = adr.getDeclaration();

    String varName = decl.toASTString().split(" ")[1].replaceAll(";", "");
    // TODO: substitute the actual function name.
    return CtoFormulaConverter.scoped(varName, "main");
  }

  private String varAddressToVarName(MemorySegment adr) {
    return adr.getDeclaration().getQualifiedName();
  }
}
