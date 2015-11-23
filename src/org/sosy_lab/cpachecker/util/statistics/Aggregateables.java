package org.sosy_lab.cpachecker.util.statistics;

import java.util.Set;

import org.sosy_lab.cpachecker.util.statistics.interfaces.Aggregateable;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Multiset;


public final class Aggregateables {

  private abstract static class AbstractAggregateable implements Aggregateable {

    protected long valuations = 0;

    @Override
    public long getValuations() {
      return valuations;
    }
  }

  private abstract static class AggregationNumber<TT extends Number> extends AbstractAggregateable {

    public abstract TT getSum();

    public abstract TT getMax();

    public abstract TT getMin();

    public Double getAvg() {
      if (valuations == 0) {
        return 0.0;
      }

      return  getSum().doubleValue() / valuations;
    }

    @Override
    public String toString() {
      return String.format("(valuations: %d, sum: %d, min: %d, max:%d, avg:%.3f)",
          getValuations(), getSum(), getMin(), getMax(), getAvg());
    }

    @Override
    public String[] getAttributeNames() {
      return new String[] {"min", "max", "sum", "avg"};
    }

    @Override
    public ImmutableMap<String, Object> getAttributeValueMap() {
      Builder<String, Object> result = ImmutableMap.builder();

      result.put("min", this.getMin());
      result.put("max", this.getMax());
      result.put("sum", this.getSum());
      result.put("avg", this.getAvg());

      return result.build();
    }
  }


  public static class AggregationInt extends AggregationNumber<Integer> {

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

    @Override
    public Integer getSum() {
      return sum;
    }

    @Override
    public Integer getMax() {
      return max;
    }

    @Override
    public Integer getMin() {
      return min;
    }

    public static AggregationInt neutral() {
      return new AggregationInt();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Aggregateable> T aggregateBy(T pBy) {
      assert pBy instanceof AggregationInt;

      AggregationInt result = new AggregationInt();
      AggregationInt by = (AggregationInt) pBy;

      result.valuations = this.valuations + by.valuations;
      result.sum = this.sum + by.sum;
      result.max = Math.max(this.max, by.max);
      result.min = Math.min(this.min, by.min);

      return (T) result;
    }

  }

  public static class AggregationLong extends AggregationNumber<Long> {

    private long max = Long.MIN_VALUE;
    private long min = Long.MAX_VALUE;
    private long sum = 0;


    @Override
    public Long getMax() {
      return max;
    }

    @Override
    public Long getMin() {
      return min;
    }

    @Override
    public Long getSum() {
      return sum;
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

    @SuppressWarnings("unchecked")
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

    public static AggregationSet of(Set<? extends Object> pValue) {
      AggregationSet result = new AggregationSet();
      result.valuations = 1;
      result.union.addAll(pValue);
      return result;
    }

    @Override
    public String[] getAttributeNames() {
      return new String[]{"union"};
    }

    @Override
    public ImmutableMap<String, ? extends Object> getAttributeValueMap() {
      return ImmutableMap.of("union", this.union);
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
      return String.format("(total wallsecs: %.3f, avg wallsecs: %.3f, total cpusecs: %.3f, avg cpusecs: %.3f)",
          this.wallTimeMsec.sum / 1000.0,
          this.wallTimeMsec.getAvg() / 1000.0,
          this.processCpuTimeMsec.sum / 1000.0,
          this.processCpuTimeMsec.getAvg() / 1000.0);
    }

    @Override
    public String[] getAttributeNames() {
      return new String[]{
          "walltime_millis_sum",
          "walltime_millis_max",
          "walltime_millis_avg",
          "cputime_millis_sum",
          "cputime_millis_max",
          "cputime_millis_avg"
          };
    }

    @Override
    public ImmutableMap<String, ? extends Object> getAttributeValueMap() {
      Builder<String, Object> result = ImmutableMap.builder();

      result.put("walltime_millis_sum", this.wallTimeMsec.sum);
      result.put("walltime_millis_max", this.wallTimeMsec.max);
      result.put("walltime_millis_avg", this.wallTimeMsec.getAvg());

      result.put("cputime_millis_sum", this.processCpuTimeMsec.sum);
      result.put("cputime_millis_max", this.processCpuTimeMsec.max);
      result.put("cputime_millis_avg", this.processCpuTimeMsec.getAvg());

      return result.build();
    }
  }

  public static class AggregationTime extends AbstractAggregateable {

    private long timeMillis = 0;
    private long timeMillisMax = 0;

    private AggregationTime() {
    }

    public static AggregationTime neutral() {
      AggregationTime result = new AggregationTime();
      result.valuations = 0;
      result.timeMillis = 0;
      result.timeMillisMax = 0;
      return result;
    }

    public static AggregationTime single(long pSpentTimeMillis) {
      AggregationTime result = new AggregationTime();
      result.valuations = 1;
      result.timeMillis = pSpentTimeMillis;
      result.timeMillisMax = pSpentTimeMillis;
      return result;
    }

    @Override
    public <T extends Aggregateable> T aggregateBy(T pBy) {

      assert pBy instanceof AggregationTime;

      AggregationTime by = (AggregationTime) pBy;
      AggregationTime result = new AggregationTime();

      result.valuations = this.valuations + by.valuations;
      result.timeMillis = this.timeMillis + by.timeMillis;
      result.timeMillisMax = Math.max(this.timeMillis, by.timeMillis);

      return (T) result;
    }

    public long getTimeMilisMax() {
      return timeMillisMax;
    }

    @Override
    public String toString() {
      return String.format("(wallsecs: %.3f)", this.timeMillis / 1e3);
    }

    @Override
    public String[] getAttributeNames() {
      return new String[]{"time_millis_sum", "time_millis_max"};
    }

    @Override
    public ImmutableMap<String, ? extends Object> getAttributeValueMap() {
      Builder<String, Object> result = ImmutableMap.builder();

      result.put("time_millis_sum", this.timeMillis);
      result.put("time_millis_max", this.timeMillisMax);

      return result.build();
    }
  }



}
