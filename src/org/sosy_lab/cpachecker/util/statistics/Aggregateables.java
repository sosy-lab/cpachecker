package org.sosy_lab.cpachecker.util.statistics;

import java.util.Set;

import org.sosy_lab.cpachecker.util.statistics.interfaces.Aggregateable;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;


public final class Aggregateables {

  private abstract static class AbstractAggregateable implements Aggregateable {
    protected long valuations = 0;

    @Override
    public long getValuations() {
      return valuations;
    }
  }

  public static class AggregationInt extends AbstractAggregateable {

    private int max = Integer.MIN_VALUE;
    private int min = Integer.MAX_VALUE;
    private int sum = 0;

    public static AggregationInt single(int pValue) {
      AggregationInt result = new AggregationInt();
      result.valuations = 1;
      result.max = pValue;
      result.min = pValue;
      result.sum = pValue;
      return result;
    }

    public int getSum() {
      return sum;
    }

    public int getMax() {
      return max;
    }

    public int getMin() {
      return min;
    }

    @Override
    public Aggregateable aggregateBy(Aggregateable pBy) {
      assert pBy instanceof AggregationInt;

      AggregationInt result = new AggregationInt();
      AggregationInt by = (AggregationInt) pBy;

      result.valuations = this.valuations + by.valuations;
      result.sum = this.sum + by.sum;
      result.max = Math.max(this.max, by.max);
      result.min = Math.min(this.min, by.min);

      return result;
    }

    public double getAvg() {
      if (valuations == 0) {
        return 0.0;
      }

      return sum / (double) valuations;
    }

    @Override
    public String toString() {
      return String.format("(valuations: %d, sum: %d, min: %d, max:%d, avg:%.3f)", valuations, sum, min, max, getAvg());
    }

    public static Aggregateable neutral() {
      return new AggregationInt();
    }
  }

  public static class AggregationLong extends AbstractAggregateable {

    private long max = Long.MIN_VALUE;
    private long min = Long.MAX_VALUE;
    private long sum = 0;

    @Override
    public <T extends Aggregateable> T aggregateBy(T pBy) {
      assert pBy instanceof AggregationLong;

      AggregationLong result = new AggregationLong();
      AggregationLong by = (AggregationLong) pBy;

      result.valuations = this.valuations + by.valuations;
      result.sum = this.sum + by.sum;
      result.max = Math.max(this.max, by.max);
      result.min = Math.min(this.min, by.min);

      return (T) result;
    }

    public long getMax() {
      return max;
    }

    public long getMin() {
      return min;
    }

    public long getSum() {
      return sum;
    }

    public double getAvg() {
      if (valuations == 0) {
        return 0.0;
      }

      return (double) sum / (double) valuations;
    }

    @Override
    public String toString() {
      return String.format("(valuations: %d, sum: %d, min: %d, max:%d, avg:%.3f)", valuations, sum, min, max, getAvg());
    }

    public static AggregationLong single(long pValue) {
      AggregationLong result = new AggregationLong();
      result.valuations = 1;
      result.max = pValue;
      result.min = pValue;
      result.sum = pValue;
      return result;
    }

    public static AggregationLong neutral() {
      return new AggregationLong();
    }
  }

  public static class AggregationSet extends AbstractAggregateable {

    private Multiset<Object> union = HashMultiset.create();

    @Override
    public <T extends Aggregateable> T aggregateBy(T pBy) {
      assert pBy instanceof AggregationSet;

      AggregationSet by = (AggregationSet) pBy;
      AggregationSet result = new AggregationSet();
      result.valuations = this.valuations + by.valuations;
      result.union.addAll(this.union);
      result.union.addAll(by.union);

      return (T) result;
    }

    @Override
    public String toString() {
      return union.toString();
    }

    public static AggregationSet single(Object pValue) {
      AggregationSet result = new AggregationSet();
      result.valuations = 1;
      result.union.add(pValue);
      return result;
    }

    public static AggregationSet of(Set<Object> pValue) {
      AggregationSet result = new AggregationSet();
      result.valuations = 1;
      result.union.addAll(pValue);
      return result;
    }
  }

  public static class AggregationMilliSecPair extends AbstractAggregateable {

    private AggregationLong processCpuTimeMsec = null;
    private AggregationLong wallTimeMsec = null;

    private AggregationMilliSecPair() {
    }

    public AggregationLong getProcessCpuTimeMsec() {
      return processCpuTimeMsec;
    }

    public AggregationLong getWallTimeMsec() {
      return wallTimeMsec;
    }

    public static AggregationMilliSecPair neutral() {
      AggregationMilliSecPair result = new AggregationMilliSecPair();
      result.processCpuTimeMsec = AggregationLong.neutral();
      result.wallTimeMsec = AggregationLong.neutral();
      return result;
    }

    public static AggregationMilliSecPair single(long pSpentCpuTimeMSecs, long pSpentWallTimeMSecs) {
      AggregationMilliSecPair result = new AggregationMilliSecPair();
      result.valuations = 1;
      result.processCpuTimeMsec = AggregationLong.single(pSpentCpuTimeMSecs);
      result.wallTimeMsec = AggregationLong.single(pSpentWallTimeMSecs);
      return result;
    }

    @Override
    public <T extends Aggregateable> T aggregateBy(T pBy) {

      assert pBy instanceof AggregationMilliSecPair;

      AggregationMilliSecPair by = (AggregationMilliSecPair) pBy;
      AggregationMilliSecPair result = new AggregationMilliSecPair();

      result.valuations = this.valuations + by.valuations;
      result.processCpuTimeMsec = this.processCpuTimeMsec.aggregateBy(by.processCpuTimeMsec);
      result.wallTimeMsec = this.wallTimeMsec.aggregateBy(by.wallTimeMsec);

      return (T) result;
    }

    @Override
    public String toString() {
      return String.format("(wallsecs: %.3f, cpusecs: %.3f)",
          this.wallTimeMsec.sum / 1000.0, this.processCpuTimeMsec.sum / 1000.0);
    }
  }

  public static class AggregationTime extends AbstractAggregateable {

    private long timeNanos = 0;

    private AggregationTime() {
    }

    public static AggregationTime neutral() {
      AggregationTime result = new AggregationTime();
      result.valuations = 0;
      result.timeNanos = 0;
      return result;
    }

    public static AggregationTime single(long pSpentTimeNanos) {
      AggregationTime result = new AggregationTime();
      result.valuations = 0;
      result.timeNanos = pSpentTimeNanos;
      return result;
    }

    @Override
    public <T extends Aggregateable> T aggregateBy(T pBy) {

      assert pBy instanceof AggregationTime;

      AggregationTime by = (AggregationTime) pBy;
      AggregationTime result = new AggregationTime();

      result.valuations = this.valuations + by.valuations;
      result.timeNanos = this.timeNanos + by.timeNanos;

      return (T) result;
    }

    @Override
    public String toString() {
      return String.format("(wallsecs: %.3f)", this.timeNanos / 1e9);
    }
  }



}
