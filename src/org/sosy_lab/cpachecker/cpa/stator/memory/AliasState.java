package org.sosy_lab.cpachecker.cpa.stator.memory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.PathFormulaReportingState;
import org.sosy_lab.cpachecker.util.ImmutableMapMerger;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Reasons about values at concrete memory addresses.
 */
public class AliasState implements
    LatticeAbstractState<AliasState>,
    Iterable<Entry<MemorySegment, AbstractMemoryAddress>>,
    Graphable,
    PathFormulaReportingState {

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

  public AbstractMemoryAddress getCreateAssignmentOn() {
    return createAssignmentOn;
  }

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
          @Override
          public MemorySegment apply(String key, MemorySegment a, MemorySegment b) {
            Preconditions.checkState(a.equals(b));
            return a;
          }
        }
    );
  }

  /**
   * @return R-value for the associated {@code memoryBlock}
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
   * Update the r-value stored at {@code block} to include {@code rValue}
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
   * Sets the r-value stored at {@code block} to {@code rValue}
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

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  /**
   * Convert the state to the formula.
   */
  @Override
  public PathFormula getFormulaApproximation(
      FormulaManagerView fmgr, SSAMap outputMap, SSAMap inputMap) {
    return new MemoryToFormulaManager(fmgr).getFormulaApproximation(
        this, outputMap, inputMap);
  }
}
