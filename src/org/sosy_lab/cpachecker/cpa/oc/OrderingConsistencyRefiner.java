package org.sosy_lab.cpachecker.cpa.oc;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cpa.oc.EventType.WRITE;
import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.INDEX_SEPARATOR;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class OrderingConsistencyRefiner implements Refiner {

  private final ConfigurableProgramAnalysis cpa;

  OrderingConsistencyRefiner(ConfigurableProgramAnalysis pCpa) {
    cpa = pCpa;
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return new OrderingConsistencyRefiner(pCpa);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    ExecutionData executionData = extractExecutionData(pReached);
    
    OrderingConsistencyCPA ocCPA = CPAs.retrieveCPA(cpa, OrderingConsistencyCPA.class);
    OrderingConsistencyTransferRelation transferRelation = 
        (OrderingConsistencyTransferRelation) ocCPA.getTransferRelation();
    
    try (ProverEnvironment prover = 
        transferRelation.getSolver().newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      
      FormulaEncoder encoder = new FormulaEncoder(
          transferRelation.getSolver().getFormulaManager(),
          executionData.allEvents);
      
      encodeMemoryModel(prover, encoder);
      encodeProgramOrder(prover, encoder, executionData);
      encodeTargetReachability(prover, encoder, executionData);
      
      boolean isUnsat = prover.isUnsat();
      System.err.printf("Unsat?: %s%n", isUnsat);
      return isUnsat;
      
    } catch (SolverException e) {
      throw new RuntimeException(e);
    }
  }

  private ExecutionData extractExecutionData(ReachedSet pReached) {
    Map<MemoryEvent, Set<MemoryEvent>> programOrder = new HashMap<>();
    Set<BooleanFormula> targetFormulae = new HashSet<>();
    Set<MemoryEvent> allEvents = new HashSet<>();
    
    for (AbstractState abstractState : pReached.asCollection()) {
      OrderingConsistencyState ocState =
          AbstractStates.extractStateByType(abstractState, OrderingConsistencyState.class);
      
      if (ocState != null) {
        ocState.pid().ifPresent(pid -> {
          ImmutableList<MemoryEvent> memoryEvents = 
              ImmutableList.copyOf(ocState.waitingThreads().get(pid).pMemoryEvents());
          
          buildProgramOrder(programOrder, memoryEvents);
          allEvents.addAll(memoryEvents);
          
          if (abstractState instanceof ARGState argState && argState.isTarget()) {
            targetFormulae.add(ocState.waitingThreads().get(pid).pPathFormula().getFormula());
          }
        });
      }
    }
    
    return new ExecutionData(programOrder, targetFormulae, allEvents);
  }

  private void buildProgramOrder(
      Map<MemoryEvent, Set<MemoryEvent>> programOrder, 
      ImmutableList<MemoryEvent> memoryEvents) {
    for (int i = 0; i < memoryEvents.size() - 1; i++) {
      MemoryEvent current = memoryEvents.get(i);
      MemoryEvent next = memoryEvents.get(i + 1);
      programOrder.computeIfAbsent(current, k -> new HashSet<>()).add(next);
    }
  }

  private void encodeMemoryModel(
      ProverEnvironment prover,
      FormulaEncoder encoder) throws InterruptedException {
    
    for (Entry<MemoryLocation, Set<MemoryEvent>> entry : encoder.reads.entrySet()) {
      MemoryLocation variable = entry.getKey();
      
      for (MemoryEvent read : entry.getValue()) {
        encodeReadFromRelation(prover, encoder, variable, read);
      }
    }
  }

  private void encodeReadFromRelation(
      ProverEnvironment prover,
      FormulaEncoder encoder,
      MemoryLocation variable,
      MemoryEvent read) throws InterruptedException {
    
    Set<BooleanFormula> readFromCandidates = new HashSet<>();
    Set<MemoryEvent> writesForVariable = encoder.writes.getOrDefault(variable, ImmutableSet.of());
    
    for (MemoryEvent write : writesForVariable) {
      BooleanFormula rfRelation = encoder.bmgr.makeVariable(
          "rf_%d_%d".formatted(write.id(), read.id()));
      readFromCandidates.add(rfRelation);
      
      encodeReadFromConstraints(prover, encoder, write, read, rfRelation, writesForVariable);
    }
    
    // Each read must read from exactly one write
    addConstraint(prover, 
        encoder.bmgr.implication(
            read.guard().get().getFormula(), 
            encoder.bmgr.or(readFromCandidates)));
  }

  private void encodeReadFromConstraints(
      ProverEnvironment prover,
      FormulaEncoder encoder,
      MemoryEvent write,
      MemoryEvent read,
      BooleanFormula rfRelation,
      Set<MemoryEvent> allWritesToSameVariable) throws InterruptedException {
    
    Formula writeValue = checkNotNull(encoder.cssaValues.get(write));
    Formula readValue = checkNotNull(encoder.cssaValues.get(read));
    
    // If rf holds, values must match
    addConstraint(prover, encoder.bmgr.implication(rfRelation, 
        encoder.fmgr.makeEqual(writeValue, readValue)));
    
    // If rf holds, both guards must be satisfied
    addConstraint(prover, encoder.bmgr.implication(rfRelation, 
        write.guard().get().getFormula()));
    addConstraint(prover, encoder.bmgr.implication(rfRelation, 
        read.guard().get().getFormula()));
    
    // If rf holds, write must happen before read
    addConstraint(prover, encoder.bmgr.implication(rfRelation, 
        encoder.imgr.lessThan(encoder.clocks.get(write), encoder.clocks.get(read))));
    
    // Coherence: no write can happen between the rf-related write and read
    for (MemoryEvent intermediateWrite : allWritesToSameVariable) {
      addConstraint(prover, encoder.bmgr.implication(
          encoder.bmgr.and(
              rfRelation,
              encoder.imgr.lessThan(encoder.clocks.get(write), encoder.clocks.get(intermediateWrite))),
          encoder.imgr.lessThan(encoder.clocks.get(read), encoder.clocks.get(intermediateWrite))));
    }
  }

  private void encodeProgramOrder(
      ProverEnvironment prover,
      FormulaEncoder encoder,
      ExecutionData data) throws InterruptedException {
    
    for (Entry<MemoryEvent, Set<MemoryEvent>> entry : data.programOrder.entrySet()) {
      MemoryEvent before = entry.getKey();
      for (MemoryEvent after : entry.getValue()) {
        addConstraint(prover, 
            encoder.imgr.lessThan(encoder.clocks.get(before), encoder.clocks.get(after)));
      }
    }
  }

  private void encodeTargetReachability(
      ProverEnvironment prover,
      FormulaEncoder encoder,
      ExecutionData data) throws InterruptedException {
    
    addConstraint(prover, encoder.bmgr.or(data.targetFormulae));
  }

  private void addConstraint(ProverEnvironment prover, BooleanFormula formula)
      throws InterruptedException {
    prover.addConstraint(formula);
  }

  /**
   * Holds the execution data extracted from the reached set.
   */
  private record ExecutionData(
      Map<MemoryEvent, Set<MemoryEvent>> programOrder,
      Set<BooleanFormula> targetFormulae,
      Set<MemoryEvent> allEvents) {}

  /**
   * Encodes memory events and their relationships into SMT formulas.
   */
  private static class FormulaEncoder {
    final FormulaManagerView fmgr;
    final IntegerFormulaManager imgr;
    final BooleanFormulaManager bmgr;
    
    final Map<MemoryEvent, IntegerFormula> clocks;
    final Map<MemoryLocation, Set<MemoryEvent>> writes;
    final Map<MemoryLocation, Set<MemoryEvent>> reads;
    final Map<MemoryEvent, Formula> cssaValues;

    FormulaEncoder(FormulaManagerView formulaManager, Set<MemoryEvent> allEvents) {
      this.fmgr = formulaManager;
      this.imgr = formulaManager.getIntegerFormulaManager();
      this.bmgr = formulaManager.getBooleanFormulaManager();
      
      this.clocks = new HashMap<>();
      this.writes = new HashMap<>();
      this.reads = new HashMap<>();
      this.cssaValues = new HashMap<>();
      
      initializeFormulas(allEvents);
    }

    private void initializeFormulas(Set<MemoryEvent> allEvents) {
      // Extract all CSSA variables from guards
      Map<String, Formula> allVariables = new HashMap<>();
      for (MemoryEvent event : allEvents) {
        allVariables.putAll(fmgr.extractVariables(event.guard().get().getFormula()));
      }
      
      // Create formulas for each event
      for (MemoryEvent event : allEvents) {
        // Create a clock variable for ordering
        clocks.put(event, imgr.makeVariable("clk_%d".formatted(event.id())));
        
        // Classify as read or write
        Map<MemoryLocation, Set<MemoryEvent>> targetMap = 
            event.eventType() == WRITE ? writes : reads;
        targetMap.computeIfAbsent(event.memoryLocation(), k -> new HashSet<>()).add(event);
        
        // Get the CSSA value
        int ssaIndex = event.eventType() == WRITE ? 2 : 1;
        String cssaName = event.cssaQualifiedName() + INDEX_SEPARATOR + ssaIndex;
        cssaValues.put(event, allVariables.get(cssaName));
      }
    }
  }
}
