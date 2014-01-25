/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.appengine.entity;

import com.google.appengine.api.log.RequestLogs;
import com.google.appengine.api.quota.QuotaService;
import com.google.appengine.api.quota.QuotaServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

/**
 * This class represents the statistics generated when the associated job was run.
 * @see RequestLogs
 */
@Entity
public class JobStatistic {

  @Id Long id;
  @Parent Ref<Job> job;
  private double cost;
  private String host;
  private long latency;
  private long endTime;
  private long startTime;
  private long pendingTime;
  private long mCycles;

  public JobStatistic() {}

  public JobStatistic(Job job) {
    setJob(job);
  }

  public String getKey() {
    return Key.create(job.getKey(), JobStatistic.class, getId()).getString();
  }

  public Long getId() {
    return id;
  }

  public Job getJob() {
    return job.get();
  }

  public void setJob(Job pJob) {
    job = Ref.create(pJob);
  }

  /**
   * Returns the estimated cost in US Dollars.
   * @return The estimated cost
   */
  public double getCost() {
    return cost;
  }

  /**
   * Sets the estimated cost in US Dollars.
   * @param pCost The cost
   */
  public void setCost(double pCost) {
    cost = pCost;
  }

  /**
   * Returns the host that processed the job.
   * @return The host
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets the host that processed the job.
   * @param pHost The host
   */
  public void setHost(String pHost) {
    host = pHost;
  }

  /**
   * The time that was needed to process the job in microseconds.
   * @return The latency in microseconds
   */
  public long getLatency() {
    return latency;
  }

  /**
   * Sets the time needed to process the job.
   * @param pLatency The latency in microseconds
   */
  public void setLatency(long pLatency) {
    latency = pLatency;
  }

  /**
   * Returns the time when the job was done processing.
   * @return The end time in microseconds since the Unix epoch
   */
  public long getEndTime() {
    return endTime;
  }

  /**
   * Sets the time when the job was done processing.
   * @param pEndTime The end time in microseconds since the Unix epoch
   */
  public void setEndTime(long pEndTime) {
    endTime = pEndTime;
  }

  /**
   * Returns the time when the job started to be processed.
   * @return The start time in microseconds since the Unix epoch
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Sets the time when the job started to be processed.
   * @param pStartTime The start time in microseconds since the Unix epoch
   */
  public void setStartTime(long pStartTime) {
    startTime = pStartTime;
  }

  /**
   * Returns the time the job was pending before it was processed.
   * @return The pending time in microseconds
   */
  public long getPendingTime() {
    return pendingTime;
  }

  /**
   * Sets the time the job was pending before it was processed.
   * @param pPendingTime The pending time in microseconds
   */
  public void setPendingTime(long pPendingTime) {
    pendingTime = pPendingTime;
  }

  /**
   * Returns the number of machine cycles used to process the job.
   * @return The number of machine cycles
   */
  public long getMcycles() {
    return mCycles;
  }

  /**
   * Sets the number of machine cycles used to process the job.
   * @param pMCycles The machine cycles
   */
  public void setMcycles(long pMCycles) {
    mCycles = pMCycles;
  }

  /**
   * Returns the approximate number of seconds that is equivalent to the used
   * machine cycles.
   * @see QuotaService#convertMegacyclesToCpuSeconds(long)
   *
   * @return The CPU seconds used to process the job
   */
  public double getMcyclesInSeconds() {
    return QuotaServiceFactory.getQuotaService().convertMegacyclesToCpuSeconds(getMcycles());
  }
}
