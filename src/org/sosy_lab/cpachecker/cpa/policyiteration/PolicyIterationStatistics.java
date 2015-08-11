package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

@Options(prefix="cpa.stator.policy")
public class PolicyIterationStatistics implements Statistics {

  final Multiset<TemplateUpdateEvent> templateUpdateCounter
      = HashMultiset.create();
  final Multiset<Integer> abstractMergeCounter = HashMultiset.create();
  final Multiset<Integer> updateCounter = HashMultiset.create();

  private final Timer valueDeterminationTimer = new Timer();
  private final Timer abstractionTimer = new Timer();
  private final Timer checkSATTimer = new Timer();
  final Timer polyhedraWideningTimer = new Timer();

  final Timer optTimer = new Timer();
  final Timer checkIndependenceTimer = new Timer();
  final Timer simplifyTimer = new Timer();
  final Timer congruenceTimer = new Timer();
  final Timer comparisonTimer = new Timer();

  private BigInteger wideningTemplatesGenerated = BigInteger.ZERO;

  public void incWideningTemplatesGenerated() {
    wideningTemplatesGenerated = wideningTemplatesGenerated.add(BigInteger.ONE);
  }

  public void startCheckSATTimer() {
    checkSATTimer.start();
  }

  public void stopCheckSATTimer() {
    checkSATTimer.stop();
  }

  public void startOPTTimer() {
    optTimer.start();
  }

  public void stopOPTTimer() {
    optTimer.stop();
  }

  public void startAbstractionTimer() {
    abstractionTimer.start();
  }

  public void stopAbstractionTimer() {
    abstractionTimer.stop();
  }

  public void startValueDeterminationTimer() {
    valueDeterminationTimer.start();
  }

  public void startCongruenceTimer() {
    congruenceTimer.start();
  }

  public void stopCongruenceTimer() {
    congruenceTimer.stop();
  }

  public void stopValueDeterminationTimer() {
    valueDeterminationTimer.stop();
  }

  public void startPolyhedraWideningTimer() {
    polyhedraWideningTimer.start();
  }

  public void stopPolyhedraWideningTimer() {
    polyhedraWideningTimer.stop();
  }

  public PolicyIterationStatistics(Configuration config)
      throws InvalidConfigurationException {
    config.inject(this, PolicyIterationStatistics.class);
  }

  @Override
  public void printStatistics(
      PrintStream out, CPAcheckerResult.Result result, ReachedSet reached) {

    printTimer(out, valueDeterminationTimer, "value determination");
    out.printf("Number of val. det. calls: %d%n",
        valueDeterminationTimer.getNumberOfIntervals());
    printTimer(out, abstractionTimer, "abstraction");
    out.printf("Number of abstractions performed: %d%n",
        abstractionTimer.getNumberOfIntervals());
    printTimer(out, optTimer, "optimization (OPT-SMT)");
    out.printf("Number of optimization queries sent: %d%n",
        optTimer.getNumberOfIntervals());
    printTimer(out, checkSATTimer, "checking bad states (SMT)");
    out.printf("Number of check-SAT calls sent: %d%n",
        checkSATTimer.getNumberOfIntervals());

    printTimer(out, comparisonTimer, "comparing abstract states");

    printTimer(out, checkIndependenceTimer, "checking independence");
    printTimer(out, simplifyTimer, "simplifying formulas");
    printTimer(out, congruenceTimer, "computing congruence");
    printTimer(out, polyhedraWideningTimer, "computing polyhedra widening");

    out.printf("Number of templates generated through widening: %s%n",
        wideningTemplatesGenerated);

    UpdateStats<?> updateStats = getUpdateStats(updateCounter);
    UpdateStats<?> templateUpdateStats = getUpdateStats(templateUpdateCounter);
    UpdateStats<?> mergeUpdateStats = getUpdateStats(abstractMergeCounter);

    printStats(out, updateStats, "abstractions on a given location");
    printStats(out, templateUpdateStats, "updates for given template on a given location");
    printStats(out, mergeUpdateStats, "merges of abstract states on a given location");
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
    out.printf("Time spent in %s: %s (Max: %s), (Avg: %s)%n",
        name, t, t.getMaxTime().formatAs(TimeUnit.SECONDS),
        t.getAvgTime().formatAs(TimeUnit.SECONDS));
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
  }

}
