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

    //initConstant();
  }

  public ExplicitPrecision(ExplicitPrecision precision, Set<String> whiteList) {

    blackListPattern = precision.blackListPattern;

    if(whiteList != null)
      this.whiteList = new HashSet<String>(whiteList);

    //initConstant();
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
    this.whiteList.add("lowerDriverReturn");
        this.whiteList.add("IofCallDriver::__retval__");
        this.whiteList.add("main::devobj");
        this.whiteList.add("IofCallDriver::Irp");
        this.whiteList.add("DiskPerfDeviceControl::deviceExtension__TargetDeviceObject");
        this.whiteList.add("DiskPerfForwardIrpSynchronous::__retval__");
        this.whiteList.add("DiskPerfDispatchPower::DeviceObject");
        this.whiteList.add("PoCallDriver::__retval__");
        this.whiteList.add("pirp");
        this.whiteList.add("DiskPerfIrpCompletion::Context");
        this.whiteList.add("DiskPerfStartDevice::Irp");
        this.whiteList.add("DiskPerfIrpCompletion::__retval__");
        this.whiteList.add("IofCallDriver::returnVal2");
        this.whiteList.add("pended");
        this.whiteList.add("DiskPerfStartDevice::status");
        this.whiteList.add("DiskPerfForwardIrpSynchronous::__cil_tmp15");
        this.whiteList.add("DiskPerfRegisterDevice::event");
        this.whiteList.add("customIrp");
        this.whiteList.add("IoBuildDeviceIoControlRequest::Event");
        this.whiteList.add("DiskPerfIrpCompletion::Irp");
        this.whiteList.add("DiskPerfDispatchPnp::__retval__");
        this.whiteList.add("setEventCalled");
        this.whiteList.add("PoCallDriver::DeviceObject");
        this.whiteList.add("DiskPerfDeviceControl::DeviceObject");
        this.whiteList.add("DiskPerfDeviceControl::Irp");
        this.whiteList.add("IoBuildDeviceIoControlRequest::OutputBufferLength");
        this.whiteList.add("DiskPerfDispatchPower::__retval__");
        this.whiteList.add("IoBuildDeviceIoControlRequest::InternalDeviceIoControl");
        this.whiteList.add("DiskPerfDispatchPower::Irp");
        this.whiteList.add("DiskPerfForwardIrpSynchronous::Irp");
        this.whiteList.add("IoBuildDeviceIoControlRequest::InputBuffer");
        this.whiteList.add("DiskPerfDispatchPower::tmp");
        this.whiteList.add("IofCallDriver::DeviceObject");
        this.whiteList.add("DiskPerfDeviceControl::status");
        this.whiteList.add("DiskPerfRegisterDevice::sizeof__number");
        this.whiteList.add("DiskPerfDeviceControl::tmp");
        this.whiteList.add("SKIP1");
        this.whiteList.add("SKIP2");
        this.whiteList.add("DiskPerfRegisterDevice::number");
        this.whiteList.add("DiskPerfRegisterDevice::status");
        this.whiteList.add("DiskPerfRegisterDevice::deviceExtension__TargetDeviceObject");
        this.whiteList.add("DiskPerfRemoveDevice::status");
        this.whiteList.add("DiskPerfForwardIrpSynchronous::DeviceObject");
        this.whiteList.add("DiskPerfRegisterDevice::outputSize");
        this.whiteList.add("DiskPerfDispatchPnp::status");
        this.whiteList.add("routine");
        this.whiteList.add("DiskPerfIrpCompletion::DeviceObject");
        this.whiteList.add("NP");
        this.whiteList.add("DiskPerfRegisterDevice::irp");
        this.whiteList.add("DiskPerfRegisterDevice::output");
        this.whiteList.add("DiskPerfRemoveDevice::Irp");
        this.whiteList.add("compRegistered");
        this.whiteList.add("IofCallDriver::lcontext");
        this.whiteList.add("IofCallDriver::compRetStatus");
        this.whiteList.add("DiskPerfRegisterDevice::ioStatus");
        this.whiteList.add("DiskPerfForwardIrpSynchronous::status");
        this.whiteList.add("main::irp");
        this.whiteList.add("IoBuildDeviceIoControlRequest::IoControlCode");
        this.whiteList.add("IoBuildDeviceIoControlRequest::OutputBuffer");
        this.whiteList.add("DiskPerfRemoveDevice::__retval__");
        this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp23");
        this.whiteList.add("PoCallDriver::Irp");
        this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp26");
        this.whiteList.add("IofCallDriver::__cil_tmp7");
        this.whiteList.add("DiskPerfRegisterDevice::__cil_tmp22");
        this.whiteList.add("DiskPerfDispatchPower::deviceExtension__TargetDeviceObject");
        this.whiteList.add("DiskPerfDeviceControl::__retval__");
        this.whiteList.add("IoBuildDeviceIoControlRequest::IoStatusBlock");
        this.whiteList.add("DiskPerfRegisterDevice::sizeof__MOUNTDEV_NAME");
        this.whiteList.add("IPC");
        this.whiteList.add("PoCallDriver::returnVal");
        this.whiteList.add("main::status");
        this.whiteList.add("DiskPerfDispatchPnp::Irp");
        this.whiteList.add("DC");
        this.whiteList.add("IoBuildDeviceIoControlRequest::InputBufferLength");
        this.whiteList.add("IoBuildDeviceIoControlRequest::DeviceObject");
        this.whiteList.add("compFptr");
        this.whiteList.add("DiskPerfForwardIrpSynchronous::deviceExtension__TargetDeviceObject");
        this.whiteList.add("DiskPerfDispatchPnp::DeviceObject");
        this.whiteList.add("DiskPerfRemoveDevice::DeviceObject");
        this.whiteList.add("s");
        this.whiteList.add("myStatus");
        this.whiteList.add("DiskPerfStartDevice::DeviceObject");
        this.whiteList.add("UNLOADED");
        this.whiteList.add("MPR3");
        this.whiteList.add("MPR1");
  }
}
