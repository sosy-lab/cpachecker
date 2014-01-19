int FloppyThread  ;
int KernelMode  ;
int Suspended  ;
int Executive  ;
int DiskController  ;
int FloppyDiskPeripheral  ;
int FlConfigCallBack  ;
int MaximumInterfaceType  ;
int MOUNTDEV_MOUNTED_DEVICE_GUID  ;
int myStatus  ;
int s  ;
int UNLOADED  ;
int NP  ;
int DC  ;
int SKIP1  ;
int SKIP2  ;
int MPR1  ;
int MPR3  ;
int IPC  ;
int pended  ;
int compRegistered  ;
int lowerDriverReturn  ;
int setEventCalled  ;
int customIrp  ;

int PsCreateSystemThread(int ThreadHandle, int DesiredAccess, int ObjectAttributes, int ProcessHandle, int ClientId, int StartRoutine, int StartContext);
int ObReferenceObjectByHandle(int Handle, int DesiredAccess, int ObjectType, int AccessMode, int Object, int HandleInformation);
int ZwClose(int Handle ); 
void IofCompleteRequest(int Irp , int PriorityBoost ); 
int FloppyStartDevice(int DeviceObject , int Irp ); 
int IofCallDriver(int DeviceObject , int Irp ); 
int KeWaitForSingleObject(int Object , int WaitReason , int WaitMode , int Alertable , int Timeout);
int IoSetDeviceInterfaceState(int SymbolicLinkName , int Enable ); 
int IoDeleteSymbolicLink(int SymbolicLinkName ); 
int FlFdcDeviceIo(int DeviceObject , int Ioctl , int Data ); 
int IoQueryDeviceDescription(int BusType, int BusNumber, int ControllerType, int ControllerNumber, int PeripheralType, int PeripheralNumber, int CalloutRoutine, int Context);
int IoRegisterDeviceInterface(int PhysicalDeviceObject, int InterfaceClassGuid, int ReferenceString, int SymbolicLinkName);
int KeSetEvent(int Event , int Increment , int Wait ); 
int IoBuildDeviceIoControlRequest(int IoControlCode, int DeviceObject, int InputBuffer, int InputBufferLength, int OutputBuffer, int OutputBufferLength, int IntervalDeviceIoControl, int Event, int IoStatusBlock);

void errorFn(void) 
{ 

  {
  goto ERROR;
  ERROR: 
  return;
}
}

void _BLAST_init(void) 
{ 

  {
#line 73
  UNLOADED = 0;
#line 74
  NP = 1;
#line 75
  DC = 2;
#line 76
  SKIP1 = 3;
#line 77
  SKIP2 = 4;
#line 78
  MPR1 = 5;
#line 79
  MPR3 = 6;
#line 80
  IPC = 7;
#line 81
  s = UNLOADED;
#line 82
  pended = 0;
#line 83
  compRegistered = 0;
#line 84
  lowerDriverReturn = 0;
#line 85
  setEventCalled = 0;
#line 86
  customIrp = 0;
#line 87
  return;
}
}
#line 90 "floppy_simpl3.cil.c"
int PagingReferenceCount  =    0;
#line 91 "floppy_simpl3.cil.c"
int PagingMutex  =    0;
#line 92 "floppy_simpl3.cil.c"
int FlAcpiConfigureFloppy(int DisketteExtension , int FdcInfo ) 
{ 

  {
#line 96
  return (0);
}
}
#line 99 "floppy_simpl3.cil.c"
int FlQueueIrpToThread(int Irp , int DisketteExtension ) 
{ int status ;
  int threadHandle ;
  int DisketteExtension__PoweringDown ;
  int DisketteExtension__ThreadReferenceCount ;
  int DisketteExtension__FloppyThread ;
  int Irp__IoStatus__Status ;
  int Irp__IoStatus__Information ;
  int Irp__Tail__Overlay__CurrentStackLocation__Control ;
  int ObjAttributes ;
  int __cil_tmp12 ;
  int __cil_tmp13 ;

  int __BLAST_NONDET;

  // initialization added by ah
  DisketteExtension__PoweringDown = __BLAST_NONDET;
  DisketteExtension__ThreadReferenceCount = __BLAST_NONDET;

  {
#line 111
  if (DisketteExtension__PoweringDown == 1) {
#line 112
    myStatus = -1073741101;
#line 113
    Irp__IoStatus__Status = -1073741101;
#line 114
    Irp__IoStatus__Information = 0;
#line 115
    return (-1073741101);
  }
#line 119
  DisketteExtension__ThreadReferenceCount ++;
#line 120
  if (DisketteExtension__ThreadReferenceCount == 0) {
#line 121
    DisketteExtension__ThreadReferenceCount ++;
#line 122
    PagingReferenceCount ++;
#line 123
    if (PagingReferenceCount == 1) {

    }
    {
#line 129
    status = PsCreateSystemThread(threadHandle, 0, ObjAttributes, 0, 0, FloppyThread,
                                  DisketteExtension);
    }
    {
#line 132
#line 132
    if (status < 0) {
#line 133
      DisketteExtension__ThreadReferenceCount = -1;
#line 134
      PagingReferenceCount --;
#line 135
      if (PagingReferenceCount == 0) {

      }
#line 140
      return (status);
    }
    }
    {
#line 145
    status = ObReferenceObjectByHandle(threadHandle, 1048576, 0, KernelMode, DisketteExtension__FloppyThread,
                                       0);
#line 147
    ZwClose(threadHandle);
    }
    {
#line 149
#line 149
    if (status < 0) {
#line 150
      return (status);
    }
    }
  }
#line 157
 // Irp__Tail__Overlay__CurrentStackLocation__Control |= 1;
#line 158
  if (pended == 0) {
#line 159
    pended = 1;
  } else {
    {
#line 162
    errorFn();
    }
  }
#line 165
  return (259);
}
}
#line 168 "floppy_simpl3.cil.c"
int FloppyPnp(int DeviceObject , int Irp ) 
{ int DeviceObject__DeviceExtension ;
  int Irp__Tail__Overlay__CurrentStackLocation ;
  int Irp__IoStatus__Information ;
  int Irp__IoStatus__Status ;
  int Irp__CurrentLocation ;
  int disketteExtension__IsRemoved ;
  int disketteExtension__IsStarted ;
  int disketteExtension__TargetObject ;
  int disketteExtension__HoldNewRequests ;
  int disketteExtension__FloppyThread ;
  int disketteExtension__InterfaceString__Buffer ;
  int disketteExtension__InterfaceString ;
  int disketteExtension__ArcName__Length ;
  int disketteExtension__ArcName ;
  int irpSp__MinorFunction ;
  int IoGetConfigurationInformation__FloppyCount ;
  int irpSp ;
  int disketteExtension ;
  int ntStatus ;
  int doneEvent ;
  int irpSp___0 ;
  int nextIrpSp ;
  int nextIrpSp__Control ;
  int irpSp___1 ;
  int irpSp__Context ;
  int irpSp__Control ;
  long __cil_tmp29 ;
  long __cil_tmp30 ;

  int __BLAST_NONDET;

  // initialization added by ah
  DeviceObject__DeviceExtension = __BLAST_NONDET;
  Irp__Tail__Overlay__CurrentStackLocation = __BLAST_NONDET;
  disketteExtension__IsRemoved = __BLAST_NONDET;
  disketteExtension__IsStarted = __BLAST_NONDET;
  disketteExtension__FloppyThread = __BLAST_NONDET;
  disketteExtension__InterfaceString__Buffer = __BLAST_NONDET;
  disketteExtension__ArcName__Length = __BLAST_NONDET;
  doneEvent = __BLAST_NONDET;
  irpSp__MinorFunction = __BLAST_NONDET;
  Irp__CurrentLocation = __BLAST_NONDET;
  IoGetConfigurationInformation__FloppyCount = __BLAST_NONDET;

  {
#line 197
  ntStatus = 0;
#line 198
  PagingReferenceCount ++;
#line 199
  if (PagingReferenceCount == 1) {

  }
#line 204
  disketteExtension = DeviceObject__DeviceExtension;
#line 205
  irpSp = Irp__Tail__Overlay__CurrentStackLocation;
#line 206
  if (disketteExtension__IsRemoved) {
    {
#line 208
    Irp__IoStatus__Information = 0;
#line 209
    Irp__IoStatus__Status = -1073741738;
#line 210
    myStatus = -1073741738;
#line 211
    IofCompleteRequest(Irp, 0);
    }
#line 213
    return (-1073741738);
  }
#line 217
  if (irpSp__MinorFunction == 0) {
    goto switch_0_0;
  } else {
#line 220
    if (irpSp__MinorFunction == 5) {
      goto switch_0_5;
    } else {
#line 223
      if (irpSp__MinorFunction == 1) {
        goto switch_0_5;
      } else {
#line 226
        if (irpSp__MinorFunction == 6) {
          goto switch_0_6;
        } else {
#line 229
          if (irpSp__MinorFunction == 3) {
            goto switch_0_6;
          } else {
#line 232
            if (irpSp__MinorFunction == 4) {
              goto switch_0_4;
            } else {
#line 235
              if (irpSp__MinorFunction == 2) {
                goto switch_0_2;
              } else {
                goto switch_0_default;
#line 240
                if (0) {
                  switch_0_0: 
                  {
#line 243
                  ntStatus = FloppyStartDevice(DeviceObject, Irp);
                  }
                  goto switch_0_break;
                  switch_0_5: 
#line 248
                  if (irpSp__MinorFunction == 5) {

                  }
#line 253
                  if (! disketteExtension__IsStarted) {
#line 254
                    if (s == NP) {
#line 255
                      s = SKIP1;
                    } else {
                      {
#line 258
                      errorFn();
                      }
                    }
                    {
#line 262
                    Irp__CurrentLocation ++;
#line 263
                    Irp__Tail__Overlay__CurrentStackLocation ++;
#line 264
                    ntStatus = IofCallDriver(disketteExtension__TargetObject, Irp);
                    }
#line 266
                    return (ntStatus);
                  }
                  {
#line 271
                  disketteExtension__HoldNewRequests = 1;
#line 272
                  ntStatus = FlQueueIrpToThread(Irp, disketteExtension);
                  }
                  {
#line 274
                  __cil_tmp29 = (long )ntStatus;
#line 274
                  if (__cil_tmp29 == 259L) {
                    {
#line 276
                    KeWaitForSingleObject(disketteExtension__FloppyThread, Executive,
                                          KernelMode, 0, 0);
                    }
#line 279
                    if (disketteExtension__FloppyThread != 0) {

                    }
#line 284
                    disketteExtension__FloppyThread = 0;
#line 285
                    Irp__IoStatus__Status = 0;
#line 286
                    myStatus = 0;
#line 287
                    if (s == NP) {
#line 288
                      s = SKIP1;
                    } else {
                      {
#line 291
                      errorFn();
                      }
                    }
                    {
#line 295
                    Irp__CurrentLocation ++;
#line 296
                    Irp__Tail__Overlay__CurrentStackLocation ++;
#line 297
                    ntStatus = IofCallDriver(disketteExtension__TargetObject, Irp);
                    }
                  } else {
                    {
#line 301
                    ntStatus = -1073741823;
#line 302
                    Irp__IoStatus__Status = ntStatus;
#line 303
                    myStatus = ntStatus;
#line 304
                    Irp__IoStatus__Information = 0;
#line 305
                    IofCompleteRequest(Irp, 0);
                    }
                  }
                  }
                  goto switch_0_break;
                  switch_0_6: 
#line 311
                  if (irpSp__MinorFunction == 6) {

                  }
#line 316
                  if (! disketteExtension__IsStarted) {
#line 317
                    Irp__IoStatus__Status = 0;
#line 318
                    myStatus = 0;
#line 319
                    if (s == NP) {
#line 320
                      s = SKIP1;
                    } else {
                      {
#line 323
                      errorFn();
                      }
                    }
                    {
#line 327
                    Irp__CurrentLocation ++;
#line 328
                    Irp__Tail__Overlay__CurrentStackLocation ++;
#line 329
                    ntStatus = IofCallDriver(disketteExtension__TargetObject, Irp);
                    }
                  } else {
#line 332
                    Irp__IoStatus__Status = 0;
#line 333
                    myStatus = 0;
#line 334
                    irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation;
#line 335
                    nextIrpSp = Irp__Tail__Overlay__CurrentStackLocation - 1;
#line 336
                    nextIrpSp__Control = 0;
#line 337
                    if (s != NP) {
                      {
#line 339
                      errorFn();
                      }
                    } else {
#line 342
                      if (compRegistered != 0) {
                        {
#line 344
                        errorFn();
                        }
                      } else {
#line 347
                        compRegistered = 1;
                      }
                    }
                    {
#line 351
                    irpSp___1 = Irp__Tail__Overlay__CurrentStackLocation - 1;
#line 352
                    irpSp__Context = doneEvent;
#line 353
                    irpSp__Control = 224;
#line 357
                    ntStatus = IofCallDriver(disketteExtension__TargetObject, Irp);
                    }
                    {
#line 359
                    __cil_tmp30 = (long )ntStatus;
#line 359
                    if (__cil_tmp30 == 259L) {
                      {
#line 361
                      KeWaitForSingleObject(doneEvent, Executive, KernelMode, 0, 0);
#line 362
                      ntStatus = myStatus;
                      }
                    }
                    }
                    {
#line 368
                    disketteExtension__HoldNewRequests = 0;
#line 369
                    Irp__IoStatus__Status = ntStatus;
#line 370
                    myStatus = ntStatus;
#line 371
                    Irp__IoStatus__Information = 0;
#line 372
                    IofCompleteRequest(Irp, 0);
                    }
                  }
                  goto switch_0_break;
                  switch_0_4: 
#line 377
                  disketteExtension__IsStarted = 0;
#line 378
                  Irp__IoStatus__Status = 0;
#line 379
                  myStatus = 0;
#line 380
                  if (s == NP) {
#line 381
                    s = SKIP1;
                  } else {
                    {
#line 384
                    errorFn();
                    }
                  }
                  {
#line 388
                  Irp__CurrentLocation ++;
#line 389
                  Irp__Tail__Overlay__CurrentStackLocation ++;
#line 390
                  ntStatus = IofCallDriver(disketteExtension__TargetObject, Irp);
                  }
                  goto switch_0_break;
                  switch_0_2: 
#line 394
                  disketteExtension__HoldNewRequests = 0;
#line 395
                  disketteExtension__IsStarted = 0;
#line 396
                  disketteExtension__IsRemoved = 1;
#line 397
                  if (s == NP) {
#line 398
                    s = SKIP1;
                  } else {
                    {
#line 401
                    errorFn();
                    }
                  }
                  {
#line 405
                  Irp__CurrentLocation ++;
#line 406
                  Irp__Tail__Overlay__CurrentStackLocation ++;
#line 407
                  Irp__IoStatus__Status = 0;
#line 408
                  myStatus = 0;
#line 409
                  ntStatus = IofCallDriver(disketteExtension__TargetObject, Irp);
                  }
#line 411
                  if (disketteExtension__InterfaceString__Buffer != 0) {
                    {
#line 413
                    IoSetDeviceInterfaceState(disketteExtension__InterfaceString,
                                              0);
                    }
                  }
#line 419
                  if (disketteExtension__ArcName__Length != 0) {
                    {
#line 421
                    IoDeleteSymbolicLink(disketteExtension__ArcName);
                    }
                  }
#line 426
                  IoGetConfigurationInformation__FloppyCount --;
                  goto switch_0_break;
                  switch_0_default: ;
#line 429
                  if (s == NP) {
#line 430
                    s = SKIP1;
                  } else {
                    {
#line 433
                    errorFn();
                    }
                  }
                  {
#line 437
                  Irp__CurrentLocation ++;
#line 438
                  Irp__Tail__Overlay__CurrentStackLocation ++;
#line 439
                  ntStatus = IofCallDriver(disketteExtension__TargetObject, Irp);
                  }
                } else {
                  switch_0_break: ;
                }
              }
            }
          }
        }
      }
    }
  }
#line 452
  PagingReferenceCount --;
#line 453
  if (PagingReferenceCount == 0) {

  }
#line 458
  return (ntStatus);
}
}
#line 461 "floppy_simpl3.cil.c"
int FloppyStartDevice(int DeviceObject , int Irp ) 
{ int DeviceObject__DeviceExtension ;
  int Irp__Tail__Overlay__CurrentStackLocation ;
  int Irp__IoStatus__Status ;
  int disketteExtension__TargetObject ;
  int disketteExtension__MaxTransferSize ;
  int disketteExtension__DriveType ;
  int disketteExtension__PerpendicularMode ;
  int disketteExtension__DeviceUnit ;
  int disketteExtension__DriveOnValue ;
  int disketteExtension__UnderlyingPDO ;
  int disketteExtension__InterfaceString ;
  int disketteExtension__IsStarted ;
  int disketteExtension__HoldNewRequests ;
  int ntStatus ;
  int pnpStatus ;
  int doneEvent ;
  int fdcInfo ;
  int fdcInfo__BufferCount ;
  int fdcInfo__BufferSize ;
  int fdcInfo__MaxTransferSize ;
  int fdcInfo__AcpiBios ;
  int fdcInfo__AcpiFdiSupported ;
  int fdcInfo__PeripheralNumber ;
  int fdcInfo__BusType ;
  int fdcInfo__ControllerNumber ;
  int fdcInfo__UnitNumber ;
  int fdcInfo__BusNumber ;
  int Dc ;
  int Fp ;
  int disketteExtension ;
  int irpSp ;
  int irpSp___0 ;
  int nextIrpSp ;
  int nextIrpSp__Control ;
  int irpSp___1 ;
  int irpSp__Control ;
  int irpSp__Context ;
  int InterfaceType ;
  int KUSER_SHARED_DATA__AlternativeArchitecture_NEC98x86 ;
  long __cil_tmp42 ;
  int __cil_tmp43 ;
  int __cil_tmp44 ;
  int __cil_tmp45 ;
  int __cil_tmp46 ;
  int __cil_tmp47 ;
  int __cil_tmp48 ;
  int __cil_tmp49 ;

  int __BLAST_NONDET;

  // initialization added by ah
  DeviceObject__DeviceExtension = __BLAST_NONDET;
  Irp__Tail__Overlay__CurrentStackLocation = __BLAST_NONDET;
  doneEvent = __BLAST_NONDET;
  fdcInfo = __BLAST_NONDET;
  fdcInfo__MaxTransferSize = __BLAST_NONDET;
  fdcInfo__AcpiBios = __BLAST_NONDET;
  fdcInfo__AcpiFdiSupported = __BLAST_NONDET;
  fdcInfo__PeripheralNumber = __BLAST_NONDET;
  fdcInfo__UnitNumber = __BLAST_NONDET;
  disketteExtension__DriveType = __BLAST_NONDET;
  KUSER_SHARED_DATA__AlternativeArchitecture_NEC98x86 = __BLAST_NONDET;

  {
#line 503
  Dc = DiskController;
#line 504
  Fp = FloppyDiskPeripheral;
#line 505
  disketteExtension = DeviceObject__DeviceExtension;
#line 506
  irpSp = Irp__Tail__Overlay__CurrentStackLocation;
#line 507
  irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation;
#line 508
  nextIrpSp = Irp__Tail__Overlay__CurrentStackLocation - 1;
#line 509
  nextIrpSp__Control = 0;
#line 510
  if (s != NP) {
    {
#line 512
    errorFn();
    }
  } else {
#line 515
    if (compRegistered != 0) {
      {
#line 517
      errorFn();
      }
    } else {
#line 520
      compRegistered = 1;
    }
  }
  {
#line 524
  irpSp___1 = Irp__Tail__Overlay__CurrentStackLocation - 1;
#line 525
  irpSp__Context = doneEvent;
#line 526
  irpSp__Control = 224;
#line 530
  ntStatus = IofCallDriver(disketteExtension__TargetObject, Irp);
  }
  {
#line 532
  __cil_tmp42 = (long )ntStatus;
#line 532
  if (__cil_tmp42 == 259L) {
    {
#line 534
    ntStatus = KeWaitForSingleObject(doneEvent, Executive, KernelMode, 0, 0);
#line 535
    ntStatus = myStatus;
    }
  }
  }
  {
#line 541
  fdcInfo__BufferCount = 0;
#line 542
  fdcInfo__BufferSize = 0;
#line 543
  __cil_tmp43 = 3080;
#line 543
  __cil_tmp44 = 458752;
#line 543
  __cil_tmp45 = 461832;
#line 543
  __cil_tmp46 = 461835;
#line 543
  ntStatus = FlFdcDeviceIo(disketteExtension__TargetObject, __cil_tmp46, fdcInfo);
  }
#line 546
  if (ntStatus >= 0) {
#line 547
    disketteExtension__MaxTransferSize = fdcInfo__MaxTransferSize;
#line 548
    if (fdcInfo__AcpiBios) {
#line 549
      if (fdcInfo__AcpiFdiSupported) {
        {
#line 551
        ntStatus = FlAcpiConfigureFloppy(disketteExtension, fdcInfo);
        }
#line 553
        if (disketteExtension__DriveType == 4) {
#line 554
          __cil_tmp47 = __BLAST_NONDET; // was uninf1();
#line 554
          //disketteExtension__PerpendicularMode |= __cil_tmp47;
        }
      } else {
        goto _L;
      }
    } else {
      _L: 
#line 563
      if (disketteExtension__DriveType == 4) {
#line 564
        __cil_tmp48 = __BLAST_NONDET; // was uninf1();
#line 564
        //disketteExtension__PerpendicularMode |= __cil_tmp48;
      }
#line 568
      InterfaceType = 0;
      {
#line 570
      while (1) {
        while_0_continue: /* CIL Label */ ;

#line 572
        if (InterfaceType >= MaximumInterfaceType) {
          goto while_1_break;
        }
        {
#line 578
        fdcInfo__BusType = InterfaceType;
#line 579
        ntStatus = IoQueryDeviceDescription(fdcInfo__BusType, fdcInfo__BusNumber,
                                            Dc, fdcInfo__ControllerNumber, Fp, fdcInfo__PeripheralNumber,
                                            FlConfigCallBack, disketteExtension);
        }
#line 583
        if (ntStatus >= 0) {
          goto while_1_break;
        }
#line 588
        InterfaceType ++;
      }
      while_0_break: /* CIL Label */ ;
      }
      while_1_break: ;
    }
#line 593
    if (ntStatus >= 0) {
#line 594
      if (KUSER_SHARED_DATA__AlternativeArchitecture_NEC98x86 != 0) {
#line 595
        disketteExtension__DeviceUnit = fdcInfo__UnitNumber;
#line 596
        //disketteExtension__DriveOnValue = fdcInfo__UnitNumber;
      } else {
#line 598
        disketteExtension__DeviceUnit = fdcInfo__PeripheralNumber;
#line 599
        //__cil_tmp49 = 16 << fdcInfo__PeripheralNumber;
#line 599
        //disketteExtension__DriveOnValue = fdcInfo__PeripheralNumber | __cil_tmp49;
      }
      {
#line 602
      pnpStatus = IoRegisterDeviceInterface(disketteExtension__UnderlyingPDO, MOUNTDEV_MOUNTED_DEVICE_GUID,
                                            0, disketteExtension__InterfaceString);
      }
#line 605
      if (pnpStatus >= 0) {
        {
#line 607
        pnpStatus = IoSetDeviceInterfaceState(disketteExtension__InterfaceString,
                                              1);
        }
      }
#line 613
      disketteExtension__IsStarted = 1;
#line 614
      disketteExtension__HoldNewRequests = 0;
    }
  }
  {
#line 622
  Irp__IoStatus__Status = ntStatus;
#line 623
  myStatus = ntStatus;
#line 624
  IofCompleteRequest(Irp, 0);
  }
#line 626
  return (ntStatus);
}
}
#line 629 "floppy_simpl3.cil.c"
int FloppyPnpComplete(int DeviceObject , int Irp , int Context ) 
{ 

  {
  {
#line 634
  KeSetEvent(Context, 1, 0);
  }
#line 636
  return (-1073741802);
}
}
#line 639 "floppy_simpl3.cil.c"
int FlFdcDeviceIo(int DeviceObject , int Ioctl , int Data ) 
{ int ntStatus ;
  int irp ;
  int irpStack ;
  int doneEvent ;
  int ioStatus ;
  int irp__Tail__Overlay__CurrentStackLocation ;
  int irpStack__Parameters__DeviceIoControl__Type3InputBuffer ;
  long __cil_tmp11 ;

  int __BLAST_NONDET;

  // initialization added by ah
  irp__Tail__Overlay__CurrentStackLocation = __BLAST_NONDET;

  {
  {
#line 650
  irp = IoBuildDeviceIoControlRequest(Ioctl, DeviceObject, 0, 0, 0, 0, 1, doneEvent,
                                      ioStatus);
  }
#line 653
  if (irp == 0) {
#line 654
    return (-1073741670);
  }
  {
#line 659
  irpStack = irp__Tail__Overlay__CurrentStackLocation - 1;
#line 660
  irpStack__Parameters__DeviceIoControl__Type3InputBuffer = Data;
#line 661
  ntStatus = IofCallDriver(DeviceObject, irp);
  }
  {
#line 663
  __cil_tmp11 = (long )ntStatus;
#line 663
  if (__cil_tmp11 == 259L) {
    {
#line 665
    KeWaitForSingleObject(doneEvent, Suspended, KernelMode, 0, 0);
#line 666
    ntStatus = myStatus;
    }
  }
  }
#line 671
  return (ntStatus);
}
}
#line 674 "floppy_simpl3.cil.c"
void FloppyProcessQueuedRequests(int DisketteExtension ) 
{ 

  {
#line 678
  return;
}
}
#line 681 "floppy_simpl3.cil.c"
void stub_driver_init(void) 
{ 

  {
#line 685
  s = NP;
#line 686
  pended = 0;
#line 687
  compRegistered = 0;
#line 688
  lowerDriverReturn = 0;
#line 689
  setEventCalled = 0;
#line 690
  customIrp = 0;
#line 691
  return;
}
}
#line 694 "floppy_simpl3.cil.c"
int main(void) 
{ int status ;
  int irp ;
  int pirp ;
  int pirp__IoStatus__Status ;
  int __BLAST_NONDET ;
  int irp_choice ;
  int devobj ;
  int __cil_tmp8 ;

  int tmp001;

  // initialization added by ah
  irp = __BLAST_NONDET;
  irp_choice = __BLAST_NONDET;
  tmp001 = __BLAST_NONDET;

 FloppyThread  = 0;
 KernelMode  = 0;
 Suspended  = 0;
 Executive  = 0;
 DiskController  = 0;
 FloppyDiskPeripheral  = 0;
 FlConfigCallBack  = 0;
 MaximumInterfaceType  = 0;
 MOUNTDEV_MOUNTED_DEVICE_GUID  = 0;
 myStatus  = 0;
 s  = 0;
 UNLOADED  = 0;
 NP  = 0;
 DC  = 0;
 SKIP1  = 0;
 SKIP2  = 0;
 MPR1  = 0;
 MPR3  = 0;
 IPC  = 0;
 pended  = 0;
 compRegistered  = 0;
 lowerDriverReturn  = 0;
 setEventCalled  = 0;
 customIrp  = 0;

  {
  {
#line 705
  status = 0;
#line 706
  pirp = irp;
#line 707
  _BLAST_init();
  }
#line 709
  if (status >= 0) {
#line 710
    s = NP;
#line 711
    customIrp = 0;
#line 712
    setEventCalled = customIrp;
#line 713
    lowerDriverReturn = setEventCalled;
#line 714
    compRegistered = lowerDriverReturn;
#line 715
    pended = compRegistered;
#line 716
    pirp__IoStatus__Status = 0;
#line 717
    myStatus = 0;
#line 718
    if (irp_choice == 0) {
#line 719
      pirp__IoStatus__Status = -1073741637;
#line 720
      myStatus = -1073741637;
    }
    {
#line 725
    stub_driver_init();
    }
    {
#line 727
#line 727
    if (status < 0) {
#line 728
      return (-1);
    }
    }
#line 732
    if (tmp001 == 3) {
      goto switch_2_3;
    } else {
      goto switch_2_default;
#line 737
      if (0) {
        switch_2_3: 
        {
#line 740
        status = FloppyPnp(devobj, pirp);
        }
        goto switch_2_break;
        switch_2_default: ;
#line 744
        return (-1);
      } else {
        switch_2_break: ;
      }
    }
  }
#line 753
  if (pended == 1) {
#line 754
    if (s == NP) {
#line 755
      s = NP;
    } else {
      goto _L___2;
    }
  } else {
    _L___2: 
#line 761
    if (pended == 1) {
#line 762
      if (s == MPR3) {
#line 763
        s = MPR3;
      } else {
        goto _L___1;
      }
    } else {
      _L___1: 
#line 769
      if (s != UNLOADED) {
#line 772
        if (status != -1) {
#line 775
          if (s != SKIP2) {
#line 776
            if (s != IPC) {
#line 777
              if (s != DC) {
                {
#line 779
                errorFn();
                }
              } else {
                goto _L___0;
              }
            } else {
              goto _L___0;
            }
          } else {
            _L___0: 
#line 789
            if (pended == 1) {
#line 790
              if (status != 259) {
#line 791
                status = 0;
              }
            } else {
#line 796
              if (s == DC) {
#line 797
                if (status == 259) {
                  {
#line 799
                  errorFn();
                  }
                }
              } else {
#line 805
                if (status != lowerDriverReturn) {
                  {
#line 807
                  errorFn();
                  }
                }
              }
            }
          }
        }
      }
    }
  }
#line 819
  status = 0;
#line 820
  return (status);
}
}
#line 823 "floppy_simpl3.cil.c"
int IoBuildDeviceIoControlRequest(int IoControlCode , int DeviceObject , int InputBuffer ,
                                  int InputBufferLength , int OutputBuffer , int OutputBufferLength ,
                                  int InternalDeviceIoControl , int Event , int IoStatusBlock ) 
{ int __BLAST_NONDET ;
  int malloc ;
  int tmp001;

  // initialization added by ah
  tmp001 = __BLAST_NONDET;
  malloc = __BLAST_NONDET;

  {
#line 830
  customIrp = 1;
#line 831
  if (tmp001 == 0) {
    goto switch_3_0;
  } else {
    goto switch_3_default;
#line 836
    if (0) {
      switch_3_0: 
#line 838
      return (malloc);
      switch_3_default: ;
#line 840
      return (0);
    } else {

    }
  }
}
}
#line 848 "floppy_simpl3.cil.c"
int IoDeleteSymbolicLink(int SymbolicLinkName ) 
{ int __BLAST_NONDET ;
  int tmp001;

  // initialization added by ah
  tmp001 = __BLAST_NONDET;

  {
#line 852
  if (tmp001 == 0) {
    goto switch_4_0;
  } else {
    goto switch_4_default;
#line 857
    if (0) {
      switch_4_0: 
#line 859
      return (0);
      switch_4_default: ;
#line 861
      return (-1073741823);
    } else {

    }
  }
}
}
#line 869 "floppy_simpl3.cil.c"
int IoQueryDeviceDescription(int BusType , int BusNumber , int ControllerType , int ControllerNumber ,
                             int PeripheralType , int PeripheralNumber , int CalloutRoutine ,
                             int Context ) 
{ int __BLAST_NONDET ;
  int tmp001;

  // initialization added by ah
  tmp001 = __BLAST_NONDET;

  {
#line 875
  if (tmp001 == 0) {
    goto switch_5_0;
  } else {
    goto switch_5_default;
#line 880
    if (0) {
      switch_5_0: 
#line 882
      return (0);
      switch_5_default: ;
#line 884
      return (-1073741823);
    } else {

    }
  }
}
}
#line 892 "floppy_simpl3.cil.c"
int IoRegisterDeviceInterface(int PhysicalDeviceObject , int InterfaceClassGuid ,
                              int ReferenceString , int SymbolicLinkName ) 
{ int __BLAST_NONDET ;
  int tmp001;

  // initialization added by ah
  tmp001 = __BLAST_NONDET;

  {
#line 897
  if (tmp001 == 0) {
    goto switch_6_0;
  } else {
    goto switch_6_default;
#line 902
    if (0) {
      switch_6_0: 
#line 904
      return (0);
      switch_6_default: ;
#line 906
      return (-1073741808);
    } else {

    }
  }
}
}
#line 914 "floppy_simpl3.cil.c"
int IoSetDeviceInterfaceState(int SymbolicLinkName , int Enable ) 
{ int __BLAST_NONDET ;
  int tmp001;

  // initialization added by ah
  tmp001 = __BLAST_NONDET;

  {
#line 918
  if (tmp001 == 0) {
    goto switch_7_0;
  } else {
    goto switch_7_default;
#line 923
    if (0) {
      switch_7_0: 
#line 925
      return (0);
      switch_7_default: ;
#line 927
      return (-1073741823);
    } else {

    }
  }
}
}
#line 935 "floppy_simpl3.cil.c"
void stubMoreProcessingRequired(void) 
{ 

  {
#line 939
  if (s == NP) {
#line 940
    s = MPR1;
  } else {
    {
#line 943
    errorFn();
    }
  }
#line 946
  return;
}
}
#line 949 "floppy_simpl3.cil.c"
int IofCallDriver(int DeviceObject , int Irp ) 
{ int __BLAST_NONDET ;
  int returnVal2 ;
  int compRetStatus1 ;
  int lcontext ;
  unsigned long __cil_tmp7 ;
  int tmp001;

  // initialization added by ah
  tmp001 = __BLAST_NONDET;

  {
#line 956
  if (compRegistered) {
    {
#line 958
    compRetStatus1 = FloppyPnpComplete(DeviceObject, Irp, lcontext);
    }
    {
#line 960
    __cil_tmp7 = (unsigned long )compRetStatus1;
#line 960
    if (__cil_tmp7 == -1073741802) {
      {
#line 962
      stubMoreProcessingRequired();
      }
    }
    }
  }
#line 970
  if (tmp001 == 0) {
    goto switch_8_0;
  } else {
#line 973
    if (tmp001 == 1) {
      goto switch_8_1;
    } else {
      goto switch_8_default;
#line 978
      if (0) {
        switch_8_0: 
#line 980
        returnVal2 = 0;
        goto switch_8_break;
        switch_8_1: 
#line 983
        returnVal2 = -1073741823;
        goto switch_8_break;
        switch_8_default: 
#line 986
        returnVal2 = 259;
        goto switch_8_break;
      } else {
        switch_8_break: ;
      }
    }
  }
#line 994
  if (s == NP) {
#line 995
    s = IPC;
#line 996
    lowerDriverReturn = returnVal2;
  } else {
#line 998
    if (s == MPR1) {
#line 999
      if (returnVal2 == 259) {
#line 1000
        s = MPR3;
#line 1001
        lowerDriverReturn = returnVal2;
      } else {
#line 1003
        s = NP;
#line 1004
        lowerDriverReturn = returnVal2;
      }
    } else {
#line 1007
      if (s == SKIP1) {
#line 1008
        s = SKIP2;
#line 1009
        lowerDriverReturn = returnVal2;
      } else {
        {
#line 1012
        errorFn();
        }
      }
    }
  }
#line 1017
  return (returnVal2);
}
}
#line 1020 "floppy_simpl3.cil.c"
void IofCompleteRequest(int Irp , int PriorityBoost ) 
{ 

  {
#line 1024
  if (s == NP) {
#line 1025
    s = DC;
  } else {
    {
#line 1028
    errorFn();
    }
  }
#line 1031
  return;
}
}
#line 1034 "floppy_simpl3.cil.c"
int KeSetEvent(int Event , int Increment , int Wait ) 
{ int l ;
  int __BLAST_NONDET;

  // initialization added by ah
  l = __BLAST_NONDET;

  {
#line 1038
  setEventCalled = 1;
#line 1039
  return (l);
}
}
#line 1042 "floppy_simpl3.cil.c"
int KeWaitForSingleObject(int Object , int WaitReason , int WaitMode , int Alertable ,
                          int Timeout ) 
{ int __BLAST_NONDET ;
  int tmp001;

  // initialization added by ah
  tmp001 = __BLAST_NONDET;

  {
#line 1047
  if (s == MPR3) {
#line 1048
    if (setEventCalled == 1) {
#line 1049
      s = NP;
#line 1050
      setEventCalled = 0;
    } else {
      goto _L;
    }
  } else {
    _L: 
#line 1056
    if (customIrp == 1) {
#line 1057
      s = NP;
#line 1058
      customIrp = 0;
    } else {
#line 1060
      if (s == MPR3) {
        {
#line 1062
        errorFn();
        }
      }
    }
  }
#line 1069
  if (tmp001 == 0) {
    goto switch_9_0;
  } else {
    goto switch_9_default;
#line 1074
    if (0) {
      switch_9_0: 
#line 1076
      return (0);
      switch_9_default: ;
#line 1078
      return (-1073741823);
    } else {

    }
  }
}
}
#line 1086 "floppy_simpl3.cil.c"
int ObReferenceObjectByHandle(int Handle , int DesiredAccess , int ObjectType , int AccessMode ,
                              int Object , int HandleInformation ) 
{ int __BLAST_NONDET ;
  int tmp001;

  // initialization added by ah
  tmp001 = __BLAST_NONDET;

  {
#line 1091
  if (tmp001 == 0) {
    goto switch_10_0;
  } else {
    goto switch_10_default;
#line 1096
    if (0) {
      switch_10_0: 
#line 1098
      return (0);
      switch_10_default: ;
#line 1100
      return (-1073741823);
    } else {

    }
  }
}
}
#line 1108 "floppy_simpl3.cil.c"
int PsCreateSystemThread(int ThreadHandle , int DesiredAccess , int ObjectAttributes ,
                         int ProcessHandle , int ClientId , int StartRoutine , int StartContext ) 
{ int __BLAST_NONDET ;
  int tmp001;

  // initialization added by ah
  tmp001 = __BLAST_NONDET;

  {
#line 1113
  if (tmp001 == 0) {
    goto switch_11_0;
  } else {
    goto switch_11_default;
#line 1118
    if (0) {
      switch_11_0: 
#line 1120
      return (0);
      switch_11_default: ;
#line 1122
      return (-1073741823);
    } else {

    }
  }
}
}
#line 1130 "floppy_simpl3.cil.c"
int ZwClose(int Handle ) 
{ int __BLAST_NONDET ;
  int tmp001;

  // initialization added by ah
  tmp001 = __BLAST_NONDET;

  {
#line 1134
  if (tmp001 == 0) {
    goto switch_12_0;
  } else {
    goto switch_12_default;
#line 1139
    if (0) {
      switch_12_0: 
#line 1141
      return (0);
      switch_12_default: ;
#line 1143
      return (-1073741823);
    } else {

    }
  }
}
}
#line 1151 "floppy_simpl3.cil.c"
int FloppyCreateClose(int DeviceObject , int Irp ) 
{ int Irp__IoStatus__Status ;
  int Irp__IoStatus__Information ;

  {
  {
#line 1157
  myStatus = 0;
#line 1158
  Irp__IoStatus__Status = 0;
#line 1159
  Irp__IoStatus__Information = 1;
#line 1160
  IofCompleteRequest(Irp, 0);
  }
#line 1162
  return (0);
}
}
