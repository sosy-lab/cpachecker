/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.common;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Abstract class that encapsulates the registration of an MBean with the
 * {@link MBeanServer}.
 * Exceptions that occur are swallowed and logged.
 * 
 * This class is not thread-safe.
 */
public class AbstractMBean {
  
  private static final MBeanServer mbs = getMBeanServer();
  
  private static MBeanServer getMBeanServer() {
    try {
      // wrap this call in method so that an exception does not prevent the
      // whole program from continuing
      return ManagementFactory.getPlatformMBeanServer();
    } catch (SecurityException e) {
      // ignore exception because we cannot handle it here
      return null;
    }
  }
  
  private final String name;
  private ObjectName oname = null;
  private final LogManager logger;

  public AbstractMBean(String name, LogManager logger) {
    this.logger = logger;
    this.name = name;
  }
    
  /**
   * Register this instance at the platform MBeanServer.
   * Swallows all checked exceptions that might occur and logs them.
   */
  public void register() {
    if (mbs != null) {
      try {
        oname = new ObjectName(name);
        mbs.registerMBean(this, oname);
      } catch (JMException e) {
        oname = null;
        logger.logException(Level.WARNING, e, "Error during registration of management interface");
      }
    } else {
      logger.log(Level.WARNING, "Cannot register management interface");
    }
  }
  
  /**
   * Unregister this instance.
   * May be called even if registration was not successful (does nothing in this case).
   */
  public void unregister() {
    if (oname != null) {
      try {
        mbs.unregisterMBean(oname);
      } catch (JMException e) {
        logger.logException(Level.WARNING, e, "Error during unregistration of management interface");
      }
    }
  }
}
