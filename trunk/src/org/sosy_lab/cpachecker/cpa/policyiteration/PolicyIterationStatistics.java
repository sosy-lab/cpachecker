package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.templates.Template;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

public class PolicyIterationStatistics implements Statistics {

  final Multiset<TemplateUpdateEvent> templateUpdateCounter
      = HashMultiset.create();
  final Multiset<Integer> abstractMergeCounter = HashMultiset.create();
  final Multiset<Integer> updateCounter = HashMultiset.create();

  final Timer valueDeterminationTimer = new Timer();
  final Timer abstractionTimer = new Timer();
  final Timer checkSATTimer = new Timer();
  public final Timer polyhedraWideningTimer = new Timer();

  final Timer optTimer = new Timer();
  final Timer checkIndependenceTimer = new Timer();
  final Timer simplifyTimer = new Timer();

  final Timer ackermannizationTimer = new Timer();
  final Timer linearizationTimer = new Timer();

  final Timer getBoundTimer = new Timer();

  private final CFA cfa;

  private BigInteger wideningTemplatesGenerated = BigInteger.ZERO;

  public void incWideningTemplatesGenerated() {
    wideningTemplatesGenerated = wideningTemplatesGenerated.add(BigInteger.ONE);
  }

  public PolicyIterationStatistics(CFA pCFA) {
    cfa = pCFA;
  }

  @Override
  public void printStatistics(
      PrintStream out, CPAcheckerResult.Result result, ReachedSet reached) {

    printTimer(out, getBoundTimer, "getting policy bound");
    printTimer(out, valueDeterminationTimer, "value determination");
    printTimer(out, abstractionTimer, "abstraction");
    printTimer(out, optTimer, "optimization (OPT-SMT)");

    printTimer(out, checkSATTimer, "checking bad states (SMT)");

    printTimer(out, checkIndependenceTimer, "checking independence");
    printTimer(out, simplifyTimer, "simplifying formulas");
    printTimer(out, polyhedraWideningTimer, "computing polyhedra widening");
    printTimer(out, ackermannizationTimer, "performing ackermannization on policies");

    out.printf("Number of templates generated through widening: %s%n",
        wideningTemplatesGenerated);

    UpdateStats<?> updateStats = getUpdateStats(updateCounter);
    UpdateStats<?> templateUpdateStats = getUpdateStats(templateUpdateCounter);
    UpdateStats<?> mergeUpdateStats = getUpdateStats(abstractMergeCounter);

    printStats(out, updateStats, "abstractions on a given location");
    printStats(out, templateUpdateStats, "updates for given template on a given location");
    printStats(out, mergeUpdateStats, "merges of abstract states on a given location");

    out.printf("Number of loop heads: %d%n", cfa.getAllLoopHeads().get().size());
    printTimer(out, linearizationTimer, "formula linearization");
  }

  private void printStats(PrintStream out, UpdateStats<?> stats, String description) {
    out.printf("Max number of %s: %d, for object: %s%n",
        description, stats.max, stats.maxObject);
    out.printf("Min number of %s: %d, for object: %s%n",
        description, stats.min, stats.minObject);
    out.printf("Avg number of %s: %.1f%n",
        description, stats.avg);

  }

  private static class UpdateStats<T> {
    final T maxObject;
    final T minObject;
    final int max, min;
    final double avg;
    UpdateStats(int pMax, int pMin, double pAvg, T pMaxObject, T pMinObject) {
      max = pMax;
      min = pMin;
      avg = pAvg;
      maxObject = pMaxObject;
      minObject = pMinObject;
    }
  }

  private <T> UpdateStats<T> getUpdateStats(Multiset<T> updateStats) {
    if (updateStats.elementSet().isEmpty()) {
      return new UpdateStats<>(0, 0, 0, null, null);
    }
    int max = Integer.MIN_VALUE;
    int min = Integer.MAX_VALUE;
    T maxObject = null;
    T minObject = null;
    double sum = 0;
    for (T l : updateStats.elementSet()) {
      int count = updateStats.count(l);
      if (count > max) {
        max = count;
        maxObject = l;
      }
      if (count < min) {
        min = count;
        minObject = l;
      }
      sum += count;
    }
    double avg = sum / updateStats.elementSet().size();
    return new UpdateStats<>(max, min, avg, maxObject, minObject);
  }

  private void printTimer(PrintStream out, Timer t, String name) {
    out.printf("Time spent in %s: %s (Max: %s), (Avg: %s), (#intervals = %s)%n",
        name, t, t.getMaxTime().formatAs(TimeUnit.SECONDS),
        t.getAvgTime().formatAs(TimeUnit.SECONDS),
        t.getNumberOfIntervals());
  }

  @Override
  public String getName() {
    return "PolicyIterationCPA";
  }

  static final class TemplateUpdateEvent {
    final int locationID;
    final Template template;

    private TemplateUpdateEvent(int pLocationID, Template pTemplate) {
      locationID = pLocationID;
      template = pTemplate;
    }

    public static TemplateUpdateEvent of(int pLocationID, Template pTemplate) {
      return new TemplateUpdateEvent(pLocationID, pTemplate);
    }


    @Override
    public boolean equals(Object o) {
      if (!(o instanceof TemplateUpdateEvent)) {
        return false;
      }
      TemplateUpdateEvent other = (TemplateUpdateEvent) o;
      return locationID == other.locationID &&
          template.equals(other.template);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(locationID, template);
    }

    @Override
    public String toString() {
      return String.format("%s (loc=%s)", template, locationID);
    }
  }
}
