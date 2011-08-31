/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class ExplicitPrecision implements Precision {
  private final Pattern blackListPattern;

  private Set<String> whiteList = null;

  public Map<CFAEdge, Map<String, Long>> facts = new HashMap<CFAEdge, Map<String, Long>>();

  public void setFacts(Map<CFAEdge, Map<String, Long>> f)
  {
    facts = f;
  }

  public ExplicitPrecision(String variableBlacklist, Set<String> whiteList) {

    blackListPattern = Pattern.compile(variableBlacklist);

    if(whiteList != null)
      this.whiteList = new HashSet<String>(whiteList);

    initConstant();
  }

  public ExplicitPrecision(ExplicitPrecision precision, Set<String> whiteList) {

    blackListPattern = precision.blackListPattern;

    if(whiteList != null)
      this.whiteList = new HashSet<String>(whiteList);

    initConstant();
  }

  boolean isOnBlacklist(String variable)
  {
    return this.blackListPattern.matcher(variable).matches();
  }

  boolean isOnWhitelist(String variable)
  {
    return whiteList == null || whiteList.contains(variable);
  }

  public boolean isTracking(String variable)
  {
    return isOnWhitelist(variable) && !blackListPattern.matcher(variable).matches();
  }

  public boolean isNotTracking(String variable)
  {
//if(!isTracking(variable)) System.out.println("decided to not track " + variable);
    return !isTracking(variable);
  }

  public Set<String> getWhiteList()
  {
    return Collections.unmodifiableSet(whiteList);
  }

  public String getBlackListPattern()
  {
    return blackListPattern.pattern();
  }

  private void initConstant()
  {
    if(true)return;
    this.whiteList = new HashSet<String>();

    // for ssh/s3_srvr.blast.06.BUG.i.cil.c
    this.whiteList.add("ssl3_accept::s->state");

/*
    // for ssh-simplified/s3_srvr_6.cil.c
    this.whiteList.add("ssl3_accept::s__state");
    this.whiteList.add("ssl3_accept::initial_state");
    this.whiteList.add("main::s");
*/

/*
    // for diskperf simple
    this.whiteList.add("lowerDriverReturn");
    this.whiteList.add("DiskPerfRegisterDevice::volumeNumber");
    this.whiteList.add("IofCallDriver::__retval__");
    this.whiteList.add("main::devobj");
    this.whiteList.add("DiskPerfRegisterDevice::output__NameLength");
    this.whiteList.add("IofCallDriver::Irp");
    this.whiteList.add("DiskPerfDeviceControl::deviceExtension__TargetDeviceObject");
    this.whiteList.add("DiskPerfCreate::DeviceObject");
    this.whiteList.add("DiskPerfDispatchPower::DeviceObject");
    this.whiteList.add("PoCallDriver::__retval__");
    this.whiteList.add("pirp");
    this.whiteList.add("DiskPerfStartDevice::Irp");
    this.whiteList.add("DiskPerfIrpCompletion::Context");
    this.whiteList.add("DiskPerfIrpCompletion::__retval__");
    this.whiteList.add("DiskPerfStartDevice::status");
    this.whiteList.add("IofCallDriver::returnVal2");
    this.whiteList.add("pended");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::__cil_tmp15");
    this.whiteList.add("DiskPerfRegisterDevice::event");
    this.whiteList.add("DiskPerfIoCompletion::Context");
    this.whiteList.add("customIrp");
    this.whiteList.add("IoBuildDeviceIoControlRequest::Event");
    this.whiteList.add("DiskPerfIrpCompletion::Irp");
    this.whiteList.add("setEventCalled");
    this.whiteList.add("DiskPerfCreate::Irp");
    this.whiteList.add("PoCallDriver::DeviceObject");
    this.whiteList.add("DiskPerfDeviceControl::DeviceObject");
    this.whiteList.add("IoBuildDeviceIoControlRequest::OutputBufferLength");
    this.whiteList.add("DiskPerfDeviceControl::Irp");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp37");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp36");
    this.whiteList.add("DiskPerfDispatchPower::__retval__");
    this.whiteList.add("IoBuildDeviceIoControlRequest::InternalDeviceIoControl");
    this.whiteList.add("IoBuildDeviceIoControlRequest::InputBuffer");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::Irp");
    this.whiteList.add("DiskPerfDispatchPower::Irp");
    this.whiteList.add("IofCallDriver::DeviceObject");
    this.whiteList.add("DiskPerfDeviceControl::status");
    this.whiteList.add("DiskPerfRegisterDevice::sizeof__number");
    this.whiteList.add("DiskPerfDispatchPower::tmp");
    this.whiteList.add("DiskPerfDeviceControl::tmp");
    this.whiteList.add("DiskPerfCreate::__retval__");
    this.whiteList.add("SKIP1");
    this.whiteList.add("SKIP2");
    this.whiteList.add("DiskPerfRegisterDevice::number");
    this.whiteList.add("DiskPerfRegisterDevice::status");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp31");
    this.whiteList.add("DiskPerfRegisterDevice::deviceExtension__TargetDeviceObject");
    this.whiteList.add("DiskPerfIoCompletion::DeviceObject");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp32");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::DeviceObject");
    this.whiteList.add("DiskPerfRegisterDevice::outputSize");
    this.whiteList.add("routine");
    this.whiteList.add("DiskPerfIoCompletion::Irp");
    this.whiteList.add("NP");
    this.whiteList.add("DiskPerfIrpCompletion::DeviceObject");
    this.whiteList.add("DiskPerfRegisterDevice::output");
    this.whiteList.add("DiskPerfRegisterDevice::irp");
    this.whiteList.add("compRegistered");
    this.whiteList.add("IofCallDriver::lcontext");
    this.whiteList.add("DiskPerfRegisterDevice::ioStatus");
    this.whiteList.add("IofCallDriver::compRetStatus");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp27");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::status");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp28");
    this.whiteList.add("main::irp");
    this.whiteList.add("IoBuildDeviceIoControlRequest::IoControlCode");
    this.whiteList.add("IoBuildDeviceIoControlRequest::OutputBuffer");
    this.whiteList.add("DiskPerfRegisterDevice::sizeof__VOLUME_NUMBER");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp23");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp26");
    this.whiteList.add("PoCallDriver::Irp");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp22");
    this.whiteList.add("IofCallDriver::__cil_tmp7");
    this.whiteList.add("DiskPerfDeviceControl::__retval__");
    this.whiteList.add("IoBuildDeviceIoControlRequest::IoStatusBlock");
    this.whiteList.add("DiskPerfDispatchPower::deviceExtension__TargetDeviceObject");
    this.whiteList.add("DiskPerfRegisterDevice::sizeof__MOUNTDEV_NAME");
    this.whiteList.add("IPC");
    this.whiteList.add("main::status");
    this.whiteList.add("PoCallDriver::returnVal");
    this.whiteList.add("DiskPerfDispatchPnp::Irp");
    this.whiteList.add("DC");
    this.whiteList.add("IoBuildDeviceIoControlRequest::InputBufferLength");
    this.whiteList.add("IoBuildDeviceIoControlRequest::DeviceObject");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::deviceExtension__TargetDeviceObject");
    this.whiteList.add("compFptr");
    this.whiteList.add("DiskPerfDispatchPnp::DeviceObject");
    this.whiteList.add("s");
    this.whiteList.add("myStatus");
    this.whiteList.add("DiskPerfStartDevice::DeviceObject");
    this.whiteList.add("UNLOADED");
    this.whiteList.add("MPR3");
    this.whiteList.add("DiskPerfRegisterDevice::ioStatus__Status");
    this.whiteList.add("MPR1");

////////////////////////// EXTENDED SET //////////////////////////////////////////////////////
    this.whiteList.add("Executive");
    this.whiteList.add("DiskPerfDeviceControl::__cil_tmp24");
    this.whiteList.add("DiskPerfDeviceControl::__cil_tmp25");
    this.whiteList.add("DiskPerfDeviceControl::__cil_tmp26");

    this.whiteList.add("DiskPerfDeviceControl::currentIrpStack");
    this.whiteList.add("DiskPerfDeviceControl::deviceExtension");
    this.whiteList.add("DiskPerfDeviceControl::diskCounters");
    this.whiteList.add("DiskPerfDeviceControl::Irp__CurrentLocation");
    this.whiteList.add("DiskPerfDeviceControl::Irp__IoStatus__Information");
    this.whiteList.add("DiskPerfDeviceControl::Irp__IoStatus__Status");
    this.whiteList.add("DiskPerfDeviceControl::Irp__Tail__Overlay__CurrentStackLocation");
    this.whiteList.add("DiskPerfDeviceControl::totalCounters");
    this.whiteList.add("DiskPerfDeviceControl::totalCounters__QueueDepth");
    this.whiteList.add("DiskPerfDispatchPnp::irpSp");

    this.whiteList.add("DiskPerfDispatchPnp::irpSp__MinorFunction");
    this.whiteList.add("DiskPerfDispatchPower::deviceExtension");
    this.whiteList.add("DiskPerfDispatchPower::Irp__CurrentLocation");
    this.whiteList.add("DiskPerfDispatchPower::Irp__Tail__Overlay__CurrentStackLocation");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::deviceExtension");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::irpSp");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::irpSp___0");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::irpSp__Context");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::irpSp__Control");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::nextIrpSp");
    this.whiteList.add("DiskPerfForwardIrpSynchronous::nextIrpSp__Control");
    this.whiteList.add("DiskPerfIrpCompletion::Event");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp20");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp21");

    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp24");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp25");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp34");
    this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp35");
    this.whiteList.add("DiskPerfRegisterDevice::deviceExtension");
    this.whiteList.add("DiskPerfRegisterDevice::registrationFlag");
    this.whiteList.add("DiskPerfRegisterDevice::volumeNumber__VolumeManagerName__0");
    this.whiteList.add("DiskPerfRemoveDevice::deviceExtension");
    this.whiteList.add("DiskPerfRemoveDevice::Irp__IoStatus__Status");
    this.whiteList.add("DiskPerfRemoveDevice::wmilibContext");
    this.whiteList.add("DiskPerfSendToNextDriver::deviceExtension");
    this.whiteList.add("DiskPerfSendToNextDriver::Irp__CurrentLocation");
    this.whiteList.add("DiskPerfSendToNextDriver::Irp__Tail__Overlay__CurrentStackLocation");
    this.whiteList.add("DiskPerfShutdownFlush::deviceExtension");
    this.whiteList.add("DiskPerfShutdownFlush::Irp__CurrentLocation");
    this.whiteList.add("DiskPerfShutdownFlush::Irp__Tail__Overlay__CurrentStackLocation");
    this.whiteList.add("DiskPerfStartDevice::deviceExtension");
    this.whiteList.add("DiskPerfStartDevice::Irp__IoStatus__Status");
    this.whiteList.add("IoBuildDeviceIoControlRequest::tmp_ndt_7");
    this.whiteList.add("IofCallDriver::tmp_ndt_8");
    this.whiteList.add("IofCallDriver::tmp_ndt_9");
    this.whiteList.add("KeWaitForSingleObject::tmp_ndt_10");
    this.whiteList.add("main::irp_choice");
    this.whiteList.add("main::pirp__IoStatus__Status");
    this.whiteList.add("main::tmp_ndt_1");
    this.whiteList.add("main::tmp_ndt_2");
    this.whiteList.add("main::tmp_ndt_3");
    this.whiteList.add("main::tmp_ndt_4");
    this.whiteList.add("main::tmp_ndt_5");
    this.whiteList.add("PoCallDriver::tmp_ndt_11");
    this.whiteList.add("PoCallDriver::tmp_ndt_12");

    // PARTY POOPER
    //this.whiteList.add("DiskPerfDeviceControl::i");
*/
  }
}
