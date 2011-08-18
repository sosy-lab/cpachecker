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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class ExplicitPrecision implements Precision {
  final Pattern blackListPattern;

  final Set<String> whiteList;

  public Map<CFAEdge, Map<String, Long>> facts = new HashMap<CFAEdge, Map<String, Long>>();

  public void setFacts(Map<CFAEdge, Map<String, Long>> f)
  {
    facts = f;
  }

  public ExplicitPrecision(String variableBlacklist, Set<String> whiteList) {

    blackListPattern = Pattern.compile(variableBlacklist);

    this.whiteList = whiteList;
/*
    System.out.println("\n\ncreating new precision containing:");

    System.out.println(getWhiteListAsString());

    this.whiteList.add("CdAudio535DeviceControl::__retval__");
    this.whiteList.add("main::devobj");
    this.whiteList.add("IofCallDriver::Irp");
    this.whiteList.add("SendSrbSynchronous::status");
    this.whiteList.add("CdAudio535DeviceControl::__cil_tmp70");
    this.whiteList.add("AG_SetStatusAndReturn::__retval__");
    this.whiteList.add("CdAudio435DeviceControl::deviceExtension");
    this.whiteList.add("AG_SetStatusAndReturn::deviceExtension__TargetDeviceObject");
    this.whiteList.add("PoCallDriver::nondet_int");
    this.whiteList.add("PoCallDriver::__retval__");
    this.whiteList.add("CdAudioSendToNextDriver::DeviceObject");
    this.whiteList.add("CdAudio535DeviceControl::currentIrpStack__Parameters__DeviceIoControl__IoControlCode");
    this.whiteList.add("CdAudio535DeviceControl::sizeof__SUB_Q_CURRENT_POSITION");
    this.whiteList.add("CdAudioStartDevice::status");
    this.whiteList.add("CdAudioSendToNextDriver::Irp");
    this.whiteList.add("HPCdrCompletion::Irp");
    this.whiteList.add("CdAudio435DeviceControl::__cil_tmp52");
    this.whiteList.add("SendSrbSynchronous::__cil_tmp18");
    this.whiteList.add("CdAudioPnp::__retval__");
    this.whiteList.add("setEventCalled");
    this.whiteList.add("main::irp_choice");
    this.whiteList.add("CdAudioForwardIrpSynchronous::DeviceObject");
    this.whiteList.add("CdAudio435DeviceControl::__cil_tmp57");
    this.whiteList.add("CdAudioPower::Irp");
    this.whiteList.add("IofCallDriver::Irp__PendingReturned");
    this.whiteList.add("CdAudio535DeviceControl::DeviceObject");
    this.whiteList.add("CdAudioStartDevice::DeviceObject");
    this.whiteList.add("CdAudioForwardIrpSynchronous::deviceExtension__TargetDeviceObject");
    this.whiteList.add("CdAudioPower::tmp");
    this.whiteList.add("CdAudioStartDevice::Irp");
    this.whiteList.add("SKIP1");
    this.whiteList.add("CdAudioForwardIrpSynchronous::Irp");
    this.whiteList.add("SKIP2");
    this.whiteList.add("CdAudio435DeviceControl::__cil_tmp42");
    this.whiteList.add("CdAudio535DeviceControl::tmp___5");
    this.whiteList.add("CdAudio435DeviceControl::__cil_tmp47");
    this.whiteList.add("NP");
    this.whiteList.add("CdAudioPower::DeviceObject");
    this.whiteList.add("AG_SetStatusAndReturn::status");
    this.whiteList.add("compRegistered");
    this.whiteList.add("main::we_should_unload");
    this.whiteList.add("IofCallDriver::lcontext");
    this.whiteList.add("IofCallDriver::compRetStatus");
    this.whiteList.add("AG_SetStatusAndReturn::__cil_tmp4");
    this.whiteList.add("CdAudioSendToNextDriver::deviceExtension__TargetDeviceObject");
    this.whiteList.add("main::irp");
    this.whiteList.add("CdAudioHPCdrDeviceControl::currentIrpStack__Parameters__DeviceIoControl__IoControlCode");
    this.whiteList.add("IofCallDriver::__cil_tmp8");
    this.whiteList.add("SendSrbSynchronous::Extension");
    this.whiteList.add("IPC");
    this.whiteList.add("CdAudioDeviceControl::DeviceObject");
    this.whiteList.add("CdAudio435DeviceControl::Irp");
    this.whiteList.add("HPCdrCompletion::DeviceObject");
    this.whiteList.add("CdAudio435DeviceControl::__cil_tmp37");
    this.whiteList.add("s");
    this.whiteList.add("CdAudio435DeviceControl::__retval__");
    this.whiteList.add("CdAudio535DeviceControl::status");
    this.whiteList.add("MPR3");
    this.whiteList.add("CdAudioDeviceControl::status");
    this.whiteList.add("MPR1");
    this.whiteList.add("lowerDriverReturn");
    this.whiteList.add("SendSrbSynchronous::irp");
    this.whiteList.add("CdAudio435DeviceControl::srb");
    this.whiteList.add("IofCallDriver::__retval__");
    this.whiteList.add("SendSrbSynchronous::Buffer");
    this.whiteList.add("CdAudio535DeviceControl::Irp");
    this.whiteList.add("CdAudioPnp::DeviceObject");
    this.whiteList.add("pirp");
    this.whiteList.add("HPCdrCompletion::Context");
    this.whiteList.add("DeviceUsageTypePaging");
    this.whiteList.add("IofCallDriver::returnVal2");
    this.whiteList.add("pended");
    this.whiteList.add("HPCdrCompletion::Irp__PendingReturned");
    this.whiteList.add("CdAudioPower::__retval__");
    this.whiteList.add("CdAudioPower::deviceExtension__TargetDeviceObject");
    this.whiteList.add("customIrp");
    this.whiteList.add("CdAudioSendToNextDriver::__retval__");
    this.whiteList.add("CdAudioDeviceControl::__retval__");
    this.whiteList.add("CdAudio435DeviceControl::__cil_tmp104");
    this.whiteList.add("PoCallDriver::DeviceObject");
    this.whiteList.add("CdAudio435DeviceControl::__cil_tmp103");
    this.whiteList.add("CdAudio435DeviceControl::__cil_tmp106");
    this.whiteList.add("main::tmp_ndt_3");
    this.whiteList.add("main::tmp_ndt_2");
    this.whiteList.add("CdAudioDeviceControl::deviceExtension__Active");
    this.whiteList.add("CdAudioPnp::irpSp__MinorFunction");
    this.whiteList.add("SendSrbSynchronous::BufferLength");
    this.whiteList.add("CdAudio535DeviceControl::SubQPtr");
    this.whiteList.add("main::tmp_ndt_1");
    this.whiteList.add("IofCallDriver::DeviceObject");
    this.whiteList.add("CdAudioDeviceControl::Irp");
    this.whiteList.add("AG_SetStatusAndReturn::Irp");
    this.whiteList.add("CdAudioSendToNextDriver::tmp");
    this.whiteList.add("CdAudio435DeviceControl::SubQPtr");
    this.whiteList.add("CdAudio435DeviceControl::DeviceObject");
    this.whiteList.add("CdAudioSignalCompletion::DeviceObject");
    this.whiteList.add("CdAudioHPCdrDeviceControl::__cil_tmp12");
    this.whiteList.add("routine");
    this.whiteList.add("CdAudio435DeviceControl::status");
    this.whiteList.add("CdAudioPnp::tmp");
    this.whiteList.add("CdAudioPnp::irpSp__Parameters__UsageNotification__Type");
    this.whiteList.add("CdAudioSignalCompletion::Irp");
    this.whiteList.add("PoCallDriver::tmp_ndt_8");
    this.whiteList.add("CdAudio435DeviceControl::currentIrpStack__Parameters__DeviceIoControl__IoControlCode");
    this.whiteList.add("CdAudioPnp::Irp");
    this.whiteList.add("PoCallDriver::Irp");
    this.whiteList.add("CdAudio535DeviceControl::__cil_tmp62");
    this.whiteList.add("CdAudioForwardIrpSynchronous::status");
    this.whiteList.add("CdAudioSignalCompletion::Event");
    this.whiteList.add("SendSrbSynchronous::Srb");
    this.whiteList.add("CdAudio535DeviceControl::deviceExtension__TargetDeviceObject");
    this.whiteList.add("main::status");
    this.whiteList.add("PoCallDriver::returnVal");
    this.whiteList.add("CdAudio535DeviceControl::currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength");
    this.whiteList.add("DC");
    this.whiteList.add("CdAudio535DeviceControl::__cil_tmp65");
    this.whiteList.add("compFptr");
    this.whiteList.add("myStatus");
    this.whiteList.add("UNLOADED");
    this.whiteList.add("main::nondet_int");

*/
  }

  public ExplicitPrecision(ExplicitPrecision precision, Set<String> whiteList) {

    blackListPattern = precision.blackListPattern;

    this.whiteList = whiteList;
/*
    System.out.println("\n\ncreating new precision containing:");

    System.out.println(getWhiteListAsString());*/
  }

  private String getWhiteListAsString()
  {
    String result = "";

    if(whiteList.size() > 0)
    {
      for(String entry : whiteList)
      {
       result = result + "\n" + entry;
      }
    }
    else
      result = "nothing";

    return result;
  }

  boolean isOnBlacklist(String variable) {
    return this.blackListPattern.matcher(variable).matches();
  }

  boolean isOnWhitelist(String variable) {
    return whiteList == null || whiteList.contains(variable);
  }

  public boolean isTracking(String variable)
  {
    return isOnWhitelist(variable)
      && !blackListPattern.matcher(variable).matches();
  }

  public boolean isNotTracking(String variable)
  {
    return !isTracking(variable);
  }

  public Set<String> getWhiteList()
  {
    return whiteList;
  }

  public String getBlackListPattern()
  {
    return blackListPattern.pattern();
  }
}
