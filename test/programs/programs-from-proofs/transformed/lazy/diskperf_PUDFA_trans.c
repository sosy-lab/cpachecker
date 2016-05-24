extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern char __VERIFIER_nondet_char(void);
extern int __VERIFIER_nondet_int(void);
extern long __VERIFIER_nondet_long(void);
extern void *__VERIFIER_nondet_pointer(void);
void IofCompleteRequest(int Irp , int PriorityBoost );
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
int compFptr  ;
int compRegistered  ;
int lowerDriverReturn  ;
int setEventCalled  ;
int customIrp  ;
int myStatus  ;
int routine  ;
int pirp  ;
int Executive ;
int KernelMode ;
void errorFn(void);
void _BLAST_init(void);
void DiskPerfSyncFilterWithTarget(int FilterDevice , int TargetDevice );
int DiskPerfDispatchPnp(int DeviceObject , int Irp );
int DiskPerfIrpCompletion(int DeviceObject , int Irp , int Context );
int DiskPerfStartDevice(int DeviceObject , int Irp );
int DiskPerfRemoveDevice(int DeviceObject , int Irp );
int DiskPerfSendToNextDriver(int DeviceObject , int Irp );
int DiskPerfDispatchPower(int DeviceObject , int Irp );
int DiskPerfForwardIrpSynchronous(int DeviceObject , int Irp );
int DiskPerfCreate(int DeviceObject , int Irp );
int DiskPerfIoCompletion(int DeviceObject , int Irp , int Context );
int DiskPerfDeviceControl(int DeviceObject , int Irp );
int DiskPerfShutdownFlush(int DeviceObject , int Irp );
void DiskPerfUnload(int DriverObject );
int DiskPerfRegisterDevice(int DeviceObject );
void stub_driver_init(void);
int main(void);
int IoBuildDeviceIoControlRequest(int IoControlCode , int DeviceObject , int InputBuffer ,
                                  int InputBufferLength , int OutputBuffer , int OutputBufferLength ,
                                  int InternalDeviceIoControl , int Event , int IoStatusBlock );
void stubMoreProcessingRequired(void);
int IofCallDriver(int DeviceObject , int Irp );
int KeSetEvent(int Event , int Increment , int Wait );
int KeWaitForSingleObject(int Object , int WaitReason , int WaitMode , int Alertable ,
                          int Timeout );
int PoCallDriver(int DeviceObject , int Irp );
int __return_4566;
int __return_4439;
int __return_4376;
int __return_4407;
int __return_4226;
int __return_4229;
int __return_4210;
int __return_4208;
int __return_4323;
int __return_4377;
int __return_2594;
int __return_2597;
int __return_2578;
int __return_2576;
int __return_2691;
int __return_2745;
int __return_2741;
int __return_2751;
int __return_2840;
int __return_2836;
int __return_4022;
int __return_2955;
int __return_2958;
int __return_2939;
int __return_2937;
int __return_3052;
int __return_3106;
int __return_3102;
int __return_4020;
int __return_3139;
int __return_3135;
int __return_4018;
int __return_3254;
int __return_3257;
int __return_3238;
int __return_3236;
int __return_3351;
int __return_3405;
int __return_3401;
int __return_4016;
int __return_3438;
int __return_3434;
int __return_4014;
int __return_3553;
int __return_3556;
int __return_3537;
int __return_3535;
int __return_3650;
int __return_3705;
int __return_3701;
int __return_4012;
int __return_3733;
int __return_3729;
int __return_4010;
int __return_3848;
int __return_3851;
int __return_3832;
int __return_3830;
int __return_3945;
int __return_3999;
int __return_3995;
int __return_4023;
int __return_4049;
int __return_2228;
int __return_2231;
int __return_2212;
int __return_2210;
int __return_2325;
int __return_2379;
int __return_2375;
int __return_2385;
int __return_2411;
int __return_4057;
int __return_1943;
int __return_1946;
int __return_1927;
int __return_1925;
int __return_2040;
int __return_2043;
int __return_4058;
int __return_1673;
int __return_1676;
int __return_1657;
int __return_1655;
int __return_1771;
int __return_1774;
int __return_1426;
int __return_1429;
int __return_1410;
int __return_1408;
int __return_1523;
int __return_1526;
int __return_4564;
int __return_4563;
int main()
{
int d = __VERIFIER_nondet_int() ;
d = __VERIFIER_nondet_int();
int status7 = __VERIFIER_nondet_int() ;
status7 = __VERIFIER_nondet_int();
int we_should_unload = __VERIFIER_nondet_int() ;
we_should_unload = __VERIFIER_nondet_int();
int irp = __VERIFIER_nondet_int() ;
irp = __VERIFIER_nondet_int();
int pirp__IoStatus__Status ;
int irp_choice = __VERIFIER_nondet_int() ;
irp_choice = __VERIFIER_nondet_int();
int devobj = __VERIFIER_nondet_int() ;
devobj = __VERIFIER_nondet_int();
int __cil_tmp9 ;
s = 0;
UNLOADED = 0;
NP = 0;
DC = 0;
SKIP1 = 0;
SKIP2 = 0;
MPR1 = 0;
MPR3 = 0;
IPC = 0;
pended = 0;
compFptr = 0;
compRegistered = 0;
lowerDriverReturn = 0;
setEventCalled = 0;
customIrp = 0;
myStatus = 0;
routine = 0;
pirp = 0;
Executive = 0;
KernelMode = 0;
pirp = irp;
{
UNLOADED = 0;
NP = 1;
DC = 2;
SKIP1 = 3;
SKIP2 = 4;
MPR1 = 5;
MPR3 = 6;
IPC = 7;
s = UNLOADED;
pended = 0;
compFptr = 0;
compRegistered = 0;
lowerDriverReturn = 0;
setEventCalled = 0;
customIrp = 0;
}
if (status7 >= 0)
{
s = NP;
customIrp = 0;
setEventCalled = customIrp;
lowerDriverReturn = setEventCalled;
compRegistered = lowerDriverReturn;
compFptr = compRegistered;
pended = compFptr;
pirp__IoStatus__Status = 0;
myStatus = 0;
if (irp_choice == 0)
{
pirp__IoStatus__Status = -1073741637;
myStatus = -1073741637;
goto label_1250;
}
else 
{
label_1250:; 
{
s = NP;
customIrp = 0;
setEventCalled = customIrp;
lowerDriverReturn = setEventCalled;
compRegistered = lowerDriverReturn;
compFptr = compRegistered;
pended = compFptr;
}
if (status7 < 0)
{
 __return_4566 = -1;
goto label_4564;
}
else 
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
{
int __tmp_1 = devobj;
int __tmp_2 = pirp;
int DeviceObject = __tmp_1;
int Irp = __tmp_2;
myStatus = 0;
{
int __tmp_3 = Irp;
int __tmp_4 = 0;
int Irp = __tmp_3;
int PriorityBoost = __tmp_4;
if (s == NP)
{
s = DC;
goto label_4432;
}
else 
{
{
__VERIFIER_error();
}
label_4432:; 
}
 __return_4439 = 0;
}
status7 = __return_4439;
goto label_1530;
}
}
else 
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 2)
{
{
int __tmp_5 = devobj;
int __tmp_6 = pirp;
int DeviceObject = __tmp_5;
int Irp = __tmp_6;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int currentIrpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int() ;
currentIrpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int();
int currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength = __VERIFIER_nondet_int() ;
currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength = __VERIFIER_nondet_int();
int sizeof__DISK_PERFORMANCE = __VERIFIER_nondet_int() ;
sizeof__DISK_PERFORMANCE = __VERIFIER_nondet_int();
int Irp__IoStatus__Information ;
int deviceExtension__DiskCounters = __VERIFIER_nondet_int() ;
deviceExtension__DiskCounters = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
int deviceExtension__Processors = __VERIFIER_nondet_int() ;
deviceExtension__Processors = __VERIFIER_nondet_int();
int totalCounters__QueueDepth ;
int deviceExtension__QueueDepth = __VERIFIER_nondet_int() ;
deviceExtension__QueueDepth = __VERIFIER_nondet_int();
int Irp__IoStatus__Status ;
int deviceExtension ;
int currentIrpStack ;
int status5 ;
int i ;
int totalCounters ;
int diskCounters ;
int tmp ;
int __cil_tmp24 ;
int __cil_tmp25 ;
int __cil_tmp26 ;
deviceExtension = DeviceObject__DeviceExtension;
currentIrpStack = Irp__Tail__Overlay__CurrentStackLocation;
__cil_tmp24 = 32;
__cil_tmp25 = 458752;
__cil_tmp26 = 458784;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp26)
{
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength < sizeof__DISK_PERFORMANCE)
{
status5 = -1073741789;
Irp__IoStatus__Information = 0;
goto label_4382;
}
else 
{
diskCounters = deviceExtension__DiskCounters;
if (diskCounters == 0)
{
Irp__IoStatus__Status = -1073741823;
myStatus = -1073741823;
{
int __tmp_7 = Irp;
int __tmp_8 = 0;
int Irp = __tmp_7;
int PriorityBoost = __tmp_8;
if (s == NP)
{
s = DC;
goto label_4368;
}
else 
{
{
__VERIFIER_error();
}
label_4368:; 
}
 __return_4376 = -1073741823;
goto label_4377;
}
}
else 
{
totalCounters = Irp__AssociatedIrp__SystemBuffer;
i = 0;
label_4336:; 
if (i >= deviceExtension__Processors)
{
totalCounters__QueueDepth = deviceExtension__QueueDepth;
status5 = 0;
Irp__IoStatus__Information = sizeof__DISK_PERFORMANCE;
label_4382:; 
Irp__IoStatus__Status = status5;
myStatus = status5;
{
int __tmp_9 = Irp;
int __tmp_10 = 0;
int Irp = __tmp_9;
int PriorityBoost = __tmp_10;
if (s == NP)
{
s = DC;
goto label_4399;
}
else 
{
{
__VERIFIER_error();
}
label_4399:; 
}
 __return_4407 = status5;
goto label_4377;
}
}
else 
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_4336;
}
}
}
}
else 
{
int __CPAchecker_TMP_1 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_1;
int __CPAchecker_TMP_2 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_2;
{
int __tmp_11 = deviceExtension__TargetDeviceObject;
int __tmp_12 = Irp;
int DeviceObject = __tmp_11;
int Irp = __tmp_12;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_13 = DeviceObject;
int __tmp_14 = Irp;
int __tmp_15 = lcontext;
int DeviceObject = __tmp_13;
int Irp = __tmp_14;
int Context = __tmp_15;
int Event ;
Event = Context;
{
int __tmp_16 = Event;
int __tmp_17 = 0;
int __tmp_18 = 0;
int Event = __tmp_16;
int Increment = __tmp_17;
int Wait = __tmp_18;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_4226 = l;
}
 __return_4229 = -1073741802;
}
compRetStatus = __return_4229;
goto label_4214;
}
else 
{
{
int __tmp_19 = DeviceObject;
int __tmp_20 = Irp;
int __tmp_21 = lcontext;
int DeviceObject = __tmp_19;
int Irp = __tmp_20;
int Context = __tmp_21;
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int();
int Irp__IoStatus__Information = __VERIFIER_nondet_int() ;
Irp__IoStatus__Information = __VERIFIER_nondet_int();
int partitionCounters__ReadCount = __VERIFIER_nondet_int() ;
partitionCounters__ReadCount = __VERIFIER_nondet_int();
int partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int();
int difference__QuadPart = __VERIFIER_nondet_int() ;
difference__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__WriteCount = __VERIFIER_nondet_int() ;
partitionCounters__WriteCount = __VERIFIER_nondet_int();
int partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int();
int Irp__Flags = __VERIFIER_nondet_int() ;
Irp__Flags = __VERIFIER_nondet_int();
int partitionCounters__SplitCount = __VERIFIER_nondet_int() ;
partitionCounters__SplitCount = __VERIFIER_nondet_int();
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation__Control ;
int partitionCounters = __VERIFIER_nondet_int() ;
partitionCounters = __VERIFIER_nondet_int();
int queueLen = __VERIFIER_nondet_int() ;
queueLen = __VERIFIER_nondet_int();
if (partitionCounters == 0)
{
 __return_4210 = 0;
goto label_4208;
}
else 
{
if (irpStack__MajorFunction == 3)
{
partitionCounters__BytesRead__QuadPart = partitionCounters__BytesRead__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_0 = partitionCounters__ReadCount;
partitionCounters__ReadCount = partitionCounters__ReadCount + 1;
__CPAchecker_TMP_0;
partitionCounters__ReadTime__QuadPart = partitionCounters__ReadTime__QuadPart + difference__QuadPart;
goto label_4172;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_4172:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_4183;
}
else 
{
label_4183:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_4191;
}
else 
{
{
__VERIFIER_error();
}
goto label_4191;
}
}
else 
{
label_4191:; 
 __return_4208 = 0;
label_4208:; 
}
compRetStatus = __return_4208;
label_4214:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_4253;
}
else 
{
{
__VERIFIER_error();
}
label_4253:; 
}
goto label_4126;
}
}
else 
{
goto label_4126;
}
}
}
}
}
}
}
else 
{
label_4126:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4274;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4274;
}
else 
{
returnVal2 = 259;
label_4274:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4302;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4312;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4312:; 
goto label_4302;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_4302;
}
else 
{
{
__VERIFIER_error();
}
label_4302:; 
 __return_4323 = returnVal2;
}
tmp = __return_4323;
 __return_4377 = tmp;
label_4377:; 
}
status7 = __return_4377;
goto label_1530;
}
}
}
}
}
}
}
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 3)
{
{
int __tmp_22 = devobj;
int __tmp_23 = pirp;
int DeviceObject = __tmp_22;
int Irp = __tmp_23;
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int irpSp__MinorFunction = __VERIFIER_nondet_int() ;
irpSp__MinorFunction = __VERIFIER_nondet_int();
int irpSp ;
int status1 ;
int tmp ;
irpSp = Irp__Tail__Overlay__CurrentStackLocation;
if (irpSp__MinorFunction == 0)
{
{
int __tmp_24 = DeviceObject;
int __tmp_25 = Irp;
int DeviceObject = __tmp_24;
int Irp = __tmp_25;
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int Irp__IoStatus__Status ;
int deviceExtension ;
int status2 ;
deviceExtension = DeviceObject__DeviceExtension;
{
int __tmp_26 = DeviceObject;
int __tmp_27 = Irp;
int DeviceObject = __tmp_26;
int Irp = __tmp_27;
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int deviceExtension ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int status4 ;
int nextIrpSp__Control ;
int irpSp ;
int nextIrpSp ;
int irpSp__Context ;
int irpSp__Control ;
int irpSp___0 ;
long __cil_tmp15 ;
deviceExtension = DeviceObject__DeviceExtension;
irpSp = Irp__Tail__Overlay__CurrentStackLocation;
nextIrpSp = Irp__Tail__Overlay__CurrentStackLocation - 1;
nextIrpSp__Control = 0;
if (s != NP)
{
{
__VERIFIER_error();
}
goto label_2460;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
goto label_2460;
}
else 
{
compRegistered = 1;
routine = 0;
label_2460:; 
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_28 = deviceExtension__TargetDeviceObject;
int __tmp_29 = Irp;
int DeviceObject = __tmp_28;
int Irp = __tmp_29;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_30 = DeviceObject;
int __tmp_31 = Irp;
int __tmp_32 = lcontext;
int DeviceObject = __tmp_30;
int Irp = __tmp_31;
int Context = __tmp_32;
int Event ;
Event = Context;
{
int __tmp_33 = Event;
int __tmp_34 = 0;
int __tmp_35 = 0;
int Event = __tmp_33;
int Increment = __tmp_34;
int Wait = __tmp_35;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2594 = l;
}
 __return_2597 = -1073741802;
}
compRetStatus = __return_2597;
goto label_2582;
}
else 
{
{
int __tmp_36 = DeviceObject;
int __tmp_37 = Irp;
int __tmp_38 = lcontext;
int DeviceObject = __tmp_36;
int Irp = __tmp_37;
int Context = __tmp_38;
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int();
int Irp__IoStatus__Information = __VERIFIER_nondet_int() ;
Irp__IoStatus__Information = __VERIFIER_nondet_int();
int partitionCounters__ReadCount = __VERIFIER_nondet_int() ;
partitionCounters__ReadCount = __VERIFIER_nondet_int();
int partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int();
int difference__QuadPart = __VERIFIER_nondet_int() ;
difference__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__WriteCount = __VERIFIER_nondet_int() ;
partitionCounters__WriteCount = __VERIFIER_nondet_int();
int partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int();
int Irp__Flags = __VERIFIER_nondet_int() ;
Irp__Flags = __VERIFIER_nondet_int();
int partitionCounters__SplitCount = __VERIFIER_nondet_int() ;
partitionCounters__SplitCount = __VERIFIER_nondet_int();
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation__Control ;
int partitionCounters = __VERIFIER_nondet_int() ;
partitionCounters = __VERIFIER_nondet_int();
int queueLen = __VERIFIER_nondet_int() ;
queueLen = __VERIFIER_nondet_int();
if (partitionCounters == 0)
{
 __return_2578 = 0;
goto label_2576;
}
else 
{
if (irpStack__MajorFunction == 3)
{
partitionCounters__BytesRead__QuadPart = partitionCounters__BytesRead__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_0 = partitionCounters__ReadCount;
partitionCounters__ReadCount = partitionCounters__ReadCount + 1;
__CPAchecker_TMP_0;
partitionCounters__ReadTime__QuadPart = partitionCounters__ReadTime__QuadPart + difference__QuadPart;
goto label_2540;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_2540:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_2551;
}
else 
{
label_2551:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_2559;
}
else 
{
{
__VERIFIER_error();
}
goto label_2559;
}
}
else 
{
label_2559:; 
 __return_2576 = 0;
label_2576:; 
}
compRetStatus = __return_2576;
label_2582:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_2621;
}
else 
{
{
__VERIFIER_error();
}
label_2621:; 
}
goto label_2494;
}
}
else 
{
goto label_2494;
}
}
}
}
}
}
}
else 
{
label_2494:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_2642;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2642;
}
else 
{
returnVal2 = 259;
label_2642:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2670;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2680;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2680:; 
goto label_2670;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2670;
}
else 
{
{
__VERIFIER_error();
}
label_2670:; 
 __return_2691 = returnVal2;
}
status4 = __return_2691;
__cil_tmp15 = (long)status4;
if (__cil_tmp15 == 259L)
{
{
int __tmp_39 = event;
int __tmp_40 = Executive;
int __tmp_41 = KernelMode;
int __tmp_42 = 0;
int __tmp_43 = 0;
int Object = __tmp_39;
int WaitReason = __tmp_40;
int WaitMode = __tmp_41;
int Alertable = __tmp_42;
int Timeout = __tmp_43;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_2714;
}
else 
{
goto label_2704;
}
}
else 
{
label_2704:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_2714;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2714;
}
else 
{
label_2714:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2745 = 0;
goto label_2741;
}
else 
{
 __return_2741 = -1073741823;
label_2741:; 
}
status4 = myStatus;
goto label_2697;
}
}
}
}
}
else 
{
label_2697:; 
 __return_2751 = status4;
}
status2 = __return_2751;
{
int __tmp_44 = DeviceObject;
int __tmp_45 = deviceExtension__TargetDeviceObject;
int FilterDevice = __tmp_44;
int TargetDevice = __tmp_45;
int FilterDevice__Flags ;
int TargetDevice__Characteristics ;
int FilterDevice__Characteristics ;
int propFlags ;
}
{
int __tmp_46 = DeviceObject;
int DeviceObject = __tmp_46;
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int sizeof__number = __VERIFIER_nondet_int() ;
sizeof__number = __VERIFIER_nondet_int();
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int sizeof__VOLUME_NUMBER = __VERIFIER_nondet_int() ;
sizeof__VOLUME_NUMBER = __VERIFIER_nondet_int();
int volumeNumber__VolumeManagerName__0 = __VERIFIER_nondet_int() ;
volumeNumber__VolumeManagerName__0 = __VERIFIER_nondet_int();
int status6 ;
int ioStatus = __VERIFIER_nondet_int() ;
ioStatus = __VERIFIER_nondet_int();
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int deviceExtension ;
int irp ;
int number = __VERIFIER_nondet_int() ;
number = __VERIFIER_nondet_int();
int registrationFlag ;
int sizeof__MOUNTDEV_NAME = __VERIFIER_nondet_int() ;
sizeof__MOUNTDEV_NAME = __VERIFIER_nondet_int();
int output__NameLength = __VERIFIER_nondet_int() ;
output__NameLength = __VERIFIER_nondet_int();
int outputSize ;
int output = __VERIFIER_nondet_int() ;
output = __VERIFIER_nondet_int();
int volumeNumber = __VERIFIER_nondet_int() ;
volumeNumber = __VERIFIER_nondet_int();
int __cil_tmp20 ;
int __cil_tmp21 ;
int __cil_tmp22 ;
long __cil_tmp23 ;
int __cil_tmp24 ;
int __cil_tmp25 ;
int __cil_tmp26 ;
long __cil_tmp27 ;
unsigned long __cil_tmp28 ;
int __cil_tmp29 ;
int __cil_tmp30 ;
int __cil_tmp31 ;
long __cil_tmp32 ;
int __cil_tmp33 ;
int __cil_tmp34 ;
int __cil_tmp35 ;
int __cil_tmp36 ;
long __cil_tmp37 ;
int __cil_tmp38 ;
int __cil_tmp39 ;
registrationFlag = 0;
deviceExtension = DeviceObject__DeviceExtension;
__cil_tmp20 = 4224;
__cil_tmp21 = 2949120;
__cil_tmp22 = 2953344;
{
int __tmp_47 = __cil_tmp22;
int __tmp_48 = deviceExtension__TargetDeviceObject;
int __tmp_49 = 0;
int __tmp_50 = 0;
int __tmp_51 = number;
int __tmp_52 = sizeof__number;
int __tmp_53 = 0;
int __tmp_54 = event;
int __tmp_55 = ioStatus;
int IoControlCode = __tmp_47;
int DeviceObject = __tmp_48;
int InputBuffer = __tmp_49;
int InputBufferLength = __tmp_50;
int OutputBuffer = __tmp_51;
int OutputBufferLength = __tmp_52;
int InternalDeviceIoControl = __tmp_53;
int Event = __tmp_54;
int IoStatusBlock = __tmp_55;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2840 = malloc_ret;
goto label_2836;
}
else 
{
 __return_2836 = 0;
label_2836:; 
}
irp = __return_2836;
if (irp == 0)
{
 __return_4022 = -1073741670;
goto label_4023;
}
else 
{
{
int __tmp_56 = deviceExtension__TargetDeviceObject;
int __tmp_57 = irp;
int DeviceObject = __tmp_56;
int Irp = __tmp_57;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_58 = DeviceObject;
int __tmp_59 = Irp;
int __tmp_60 = lcontext;
int DeviceObject = __tmp_58;
int Irp = __tmp_59;
int Context = __tmp_60;
int Event ;
Event = Context;
{
int __tmp_61 = Event;
int __tmp_62 = 0;
int __tmp_63 = 0;
int Event = __tmp_61;
int Increment = __tmp_62;
int Wait = __tmp_63;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2955 = l;
}
 __return_2958 = -1073741802;
}
compRetStatus = __return_2958;
goto label_2943;
}
else 
{
{
int __tmp_64 = DeviceObject;
int __tmp_65 = Irp;
int __tmp_66 = lcontext;
int DeviceObject = __tmp_64;
int Irp = __tmp_65;
int Context = __tmp_66;
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int();
int Irp__IoStatus__Information = __VERIFIER_nondet_int() ;
Irp__IoStatus__Information = __VERIFIER_nondet_int();
int partitionCounters__ReadCount = __VERIFIER_nondet_int() ;
partitionCounters__ReadCount = __VERIFIER_nondet_int();
int partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int();
int difference__QuadPart = __VERIFIER_nondet_int() ;
difference__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__WriteCount = __VERIFIER_nondet_int() ;
partitionCounters__WriteCount = __VERIFIER_nondet_int();
int partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int();
int Irp__Flags = __VERIFIER_nondet_int() ;
Irp__Flags = __VERIFIER_nondet_int();
int partitionCounters__SplitCount = __VERIFIER_nondet_int() ;
partitionCounters__SplitCount = __VERIFIER_nondet_int();
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation__Control ;
int partitionCounters = __VERIFIER_nondet_int() ;
partitionCounters = __VERIFIER_nondet_int();
int queueLen = __VERIFIER_nondet_int() ;
queueLen = __VERIFIER_nondet_int();
if (partitionCounters == 0)
{
 __return_2939 = 0;
goto label_2937;
}
else 
{
if (irpStack__MajorFunction == 3)
{
partitionCounters__BytesRead__QuadPart = partitionCounters__BytesRead__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_0 = partitionCounters__ReadCount;
partitionCounters__ReadCount = partitionCounters__ReadCount + 1;
__CPAchecker_TMP_0;
partitionCounters__ReadTime__QuadPart = partitionCounters__ReadTime__QuadPart + difference__QuadPart;
goto label_2901;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_2901:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_2912;
}
else 
{
label_2912:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_2920;
}
else 
{
{
__VERIFIER_error();
}
goto label_2920;
}
}
else 
{
label_2920:; 
 __return_2937 = 0;
label_2937:; 
}
compRetStatus = __return_2937;
label_2943:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_2982;
}
else 
{
{
__VERIFIER_error();
}
label_2982:; 
}
goto label_2855;
}
}
else 
{
goto label_2855;
}
}
}
}
}
}
}
else 
{
label_2855:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3003;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3003;
}
else 
{
returnVal2 = 259;
label_3003:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3031;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3041;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3041:; 
goto label_3031;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3031;
}
else 
{
{
__VERIFIER_error();
}
label_3031:; 
 __return_3052 = returnVal2;
}
status6 = __return_3052;
__cil_tmp23 = (long)status6;
if (__cil_tmp23 == 259L)
{
{
int __tmp_67 = event;
int __tmp_68 = Executive;
int __tmp_69 = KernelMode;
int __tmp_70 = 0;
int __tmp_71 = 0;
int Object = __tmp_67;
int WaitReason = __tmp_68;
int WaitMode = __tmp_69;
int Alertable = __tmp_70;
int Timeout = __tmp_71;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_3075;
}
else 
{
goto label_3065;
}
}
else 
{
label_3065:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_3075;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3075;
}
else 
{
label_3075:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_3106 = 0;
goto label_3102;
}
else 
{
 __return_3102 = -1073741823;
label_3102:; 
}
status6 = ioStatus__Status;
goto label_3058;
}
}
}
}
}
else 
{
label_3058:; 
if (status6 < 0)
{
outputSize = sizeof__MOUNTDEV_NAME;
if (output == 0)
{
 __return_4020 = -1073741670;
goto label_4023;
}
else 
{
__cil_tmp24 = 8;
__cil_tmp25 = 5046272;
__cil_tmp26 = 5046280;
{
int __tmp_72 = __cil_tmp26;
int __tmp_73 = deviceExtension__TargetDeviceObject;
int __tmp_74 = 0;
int __tmp_75 = 0;
int __tmp_76 = output;
int __tmp_77 = outputSize;
int __tmp_78 = 0;
int __tmp_79 = event;
int __tmp_80 = ioStatus;
int IoControlCode = __tmp_72;
int DeviceObject = __tmp_73;
int InputBuffer = __tmp_74;
int InputBufferLength = __tmp_75;
int OutputBuffer = __tmp_76;
int OutputBufferLength = __tmp_77;
int InternalDeviceIoControl = __tmp_78;
int Event = __tmp_79;
int IoStatusBlock = __tmp_80;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_3139 = malloc_ret;
goto label_3135;
}
else 
{
 __return_3135 = 0;
label_3135:; 
}
irp = __return_3135;
if (irp == 0)
{
 __return_4018 = -1073741670;
goto label_4023;
}
else 
{
{
int __tmp_81 = deviceExtension__TargetDeviceObject;
int __tmp_82 = irp;
int DeviceObject = __tmp_81;
int Irp = __tmp_82;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_83 = DeviceObject;
int __tmp_84 = Irp;
int __tmp_85 = lcontext;
int DeviceObject = __tmp_83;
int Irp = __tmp_84;
int Context = __tmp_85;
int Event ;
Event = Context;
{
int __tmp_86 = Event;
int __tmp_87 = 0;
int __tmp_88 = 0;
int Event = __tmp_86;
int Increment = __tmp_87;
int Wait = __tmp_88;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_3254 = l;
}
 __return_3257 = -1073741802;
}
compRetStatus = __return_3257;
goto label_3242;
}
else 
{
{
int __tmp_89 = DeviceObject;
int __tmp_90 = Irp;
int __tmp_91 = lcontext;
int DeviceObject = __tmp_89;
int Irp = __tmp_90;
int Context = __tmp_91;
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int();
int Irp__IoStatus__Information = __VERIFIER_nondet_int() ;
Irp__IoStatus__Information = __VERIFIER_nondet_int();
int partitionCounters__ReadCount = __VERIFIER_nondet_int() ;
partitionCounters__ReadCount = __VERIFIER_nondet_int();
int partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int();
int difference__QuadPart = __VERIFIER_nondet_int() ;
difference__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__WriteCount = __VERIFIER_nondet_int() ;
partitionCounters__WriteCount = __VERIFIER_nondet_int();
int partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int();
int Irp__Flags = __VERIFIER_nondet_int() ;
Irp__Flags = __VERIFIER_nondet_int();
int partitionCounters__SplitCount = __VERIFIER_nondet_int() ;
partitionCounters__SplitCount = __VERIFIER_nondet_int();
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation__Control ;
int partitionCounters = __VERIFIER_nondet_int() ;
partitionCounters = __VERIFIER_nondet_int();
int queueLen = __VERIFIER_nondet_int() ;
queueLen = __VERIFIER_nondet_int();
if (partitionCounters == 0)
{
 __return_3238 = 0;
goto label_3236;
}
else 
{
if (irpStack__MajorFunction == 3)
{
partitionCounters__BytesRead__QuadPart = partitionCounters__BytesRead__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_0 = partitionCounters__ReadCount;
partitionCounters__ReadCount = partitionCounters__ReadCount + 1;
__CPAchecker_TMP_0;
partitionCounters__ReadTime__QuadPart = partitionCounters__ReadTime__QuadPart + difference__QuadPart;
goto label_3200;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_3200:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_3211;
}
else 
{
label_3211:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_3219;
}
else 
{
{
__VERIFIER_error();
}
goto label_3219;
}
}
else 
{
label_3219:; 
 __return_3236 = 0;
label_3236:; 
}
compRetStatus = __return_3236;
label_3242:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_3281;
}
else 
{
{
__VERIFIER_error();
}
label_3281:; 
}
goto label_3154;
}
}
else 
{
goto label_3154;
}
}
}
}
}
}
}
else 
{
label_3154:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3302;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3302;
}
else 
{
returnVal2 = 259;
label_3302:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3330;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3340;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3340:; 
goto label_3330;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3330;
}
else 
{
{
__VERIFIER_error();
}
label_3330:; 
 __return_3351 = returnVal2;
}
status6 = __return_3351;
__cil_tmp27 = (long)status6;
if (__cil_tmp27 == 259L)
{
{
int __tmp_92 = event;
int __tmp_93 = Executive;
int __tmp_94 = KernelMode;
int __tmp_95 = 0;
int __tmp_96 = 0;
int Object = __tmp_92;
int WaitReason = __tmp_93;
int WaitMode = __tmp_94;
int Alertable = __tmp_95;
int Timeout = __tmp_96;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_3374;
}
else 
{
goto label_3364;
}
}
else 
{
label_3364:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_3374;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3374;
}
else 
{
label_3374:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_3405 = 0;
goto label_3401;
}
else 
{
 __return_3401 = -1073741823;
label_3401:; 
}
status6 = ioStatus__Status;
goto label_3357;
}
}
}
}
}
else 
{
label_3357:; 
__cil_tmp28 = (unsigned long)status6;
if (__cil_tmp28 == -2147483643)
{
outputSize = sizeof__MOUNTDEV_NAME + output__NameLength;
if (output == 0)
{
 __return_4016 = -1073741670;
goto label_4023;
}
else 
{
__cil_tmp29 = 8;
__cil_tmp30 = 5046272;
__cil_tmp31 = 5046280;
{
int __tmp_97 = __cil_tmp31;
int __tmp_98 = deviceExtension__TargetDeviceObject;
int __tmp_99 = 0;
int __tmp_100 = 0;
int __tmp_101 = output;
int __tmp_102 = outputSize;
int __tmp_103 = 0;
int __tmp_104 = event;
int __tmp_105 = ioStatus;
int IoControlCode = __tmp_97;
int DeviceObject = __tmp_98;
int InputBuffer = __tmp_99;
int InputBufferLength = __tmp_100;
int OutputBuffer = __tmp_101;
int OutputBufferLength = __tmp_102;
int InternalDeviceIoControl = __tmp_103;
int Event = __tmp_104;
int IoStatusBlock = __tmp_105;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_3438 = malloc_ret;
goto label_3434;
}
else 
{
 __return_3434 = 0;
label_3434:; 
}
irp = __return_3434;
if (irp == 0)
{
 __return_4014 = -1073741670;
goto label_4023;
}
else 
{
{
int __tmp_106 = deviceExtension__TargetDeviceObject;
int __tmp_107 = irp;
int DeviceObject = __tmp_106;
int Irp = __tmp_107;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_108 = DeviceObject;
int __tmp_109 = Irp;
int __tmp_110 = lcontext;
int DeviceObject = __tmp_108;
int Irp = __tmp_109;
int Context = __tmp_110;
int Event ;
Event = Context;
{
int __tmp_111 = Event;
int __tmp_112 = 0;
int __tmp_113 = 0;
int Event = __tmp_111;
int Increment = __tmp_112;
int Wait = __tmp_113;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_3553 = l;
}
 __return_3556 = -1073741802;
}
compRetStatus = __return_3556;
goto label_3541;
}
else 
{
{
int __tmp_114 = DeviceObject;
int __tmp_115 = Irp;
int __tmp_116 = lcontext;
int DeviceObject = __tmp_114;
int Irp = __tmp_115;
int Context = __tmp_116;
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int();
int Irp__IoStatus__Information = __VERIFIER_nondet_int() ;
Irp__IoStatus__Information = __VERIFIER_nondet_int();
int partitionCounters__ReadCount = __VERIFIER_nondet_int() ;
partitionCounters__ReadCount = __VERIFIER_nondet_int();
int partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int();
int difference__QuadPart = __VERIFIER_nondet_int() ;
difference__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__WriteCount = __VERIFIER_nondet_int() ;
partitionCounters__WriteCount = __VERIFIER_nondet_int();
int partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int();
int Irp__Flags = __VERIFIER_nondet_int() ;
Irp__Flags = __VERIFIER_nondet_int();
int partitionCounters__SplitCount = __VERIFIER_nondet_int() ;
partitionCounters__SplitCount = __VERIFIER_nondet_int();
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation__Control ;
int partitionCounters = __VERIFIER_nondet_int() ;
partitionCounters = __VERIFIER_nondet_int();
int queueLen = __VERIFIER_nondet_int() ;
queueLen = __VERIFIER_nondet_int();
if (partitionCounters == 0)
{
 __return_3537 = 0;
goto label_3535;
}
else 
{
if (irpStack__MajorFunction == 3)
{
partitionCounters__BytesRead__QuadPart = partitionCounters__BytesRead__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_0 = partitionCounters__ReadCount;
partitionCounters__ReadCount = partitionCounters__ReadCount + 1;
__CPAchecker_TMP_0;
partitionCounters__ReadTime__QuadPart = partitionCounters__ReadTime__QuadPart + difference__QuadPart;
goto label_3499;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_3499:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_3510;
}
else 
{
label_3510:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_3518;
}
else 
{
{
__VERIFIER_error();
}
goto label_3518;
}
}
else 
{
label_3518:; 
 __return_3535 = 0;
label_3535:; 
}
compRetStatus = __return_3535;
label_3541:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_3580;
}
else 
{
{
__VERIFIER_error();
}
label_3580:; 
}
goto label_3453;
}
}
else 
{
goto label_3453;
}
}
}
}
}
}
}
else 
{
label_3453:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3601;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3601;
}
else 
{
returnVal2 = 259;
label_3601:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3629;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3639;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3639:; 
goto label_3629;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3629;
}
else 
{
{
__VERIFIER_error();
}
label_3629:; 
 __return_3650 = returnVal2;
}
status6 = __return_3650;
__cil_tmp32 = (long)status6;
if (__cil_tmp32 == 259L)
{
{
int __tmp_117 = event;
int __tmp_118 = Executive;
int __tmp_119 = KernelMode;
int __tmp_120 = 0;
int __tmp_121 = 0;
int Object = __tmp_117;
int WaitReason = __tmp_118;
int WaitMode = __tmp_119;
int Alertable = __tmp_120;
int Timeout = __tmp_121;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_3674;
}
else 
{
goto label_3664;
}
}
else 
{
label_3664:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_3674;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3674;
}
else 
{
label_3674:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_3705 = 0;
goto label_3701;
}
else 
{
 __return_3701 = -1073741823;
label_3701:; 
}
status6 = ioStatus__Status;
goto label_3414;
}
}
}
}
}
else 
{
goto label_3414;
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_3414:; 
if (status6 < 0)
{
 __return_4012 = status6;
goto label_4023;
}
else 
{
__cil_tmp34 = 28;
__cil_tmp35 = 5636096;
__cil_tmp36 = 5636124;
{
int __tmp_122 = __cil_tmp36;
int __tmp_123 = deviceExtension__TargetDeviceObject;
int __tmp_124 = 0;
int __tmp_125 = 0;
int __tmp_126 = volumeNumber;
int __tmp_127 = sizeof__VOLUME_NUMBER;
int __tmp_128 = 0;
int __tmp_129 = event;
int __tmp_130 = ioStatus;
int IoControlCode = __tmp_122;
int DeviceObject = __tmp_123;
int InputBuffer = __tmp_124;
int InputBufferLength = __tmp_125;
int OutputBuffer = __tmp_126;
int OutputBufferLength = __tmp_127;
int InternalDeviceIoControl = __tmp_128;
int Event = __tmp_129;
int IoStatusBlock = __tmp_130;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_3733 = malloc_ret;
goto label_3729;
}
else 
{
 __return_3729 = 0;
label_3729:; 
}
irp = __return_3729;
if (irp == 0)
{
 __return_4010 = -1073741670;
goto label_4023;
}
else 
{
{
int __tmp_131 = deviceExtension__TargetDeviceObject;
int __tmp_132 = irp;
int DeviceObject = __tmp_131;
int Irp = __tmp_132;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_133 = DeviceObject;
int __tmp_134 = Irp;
int __tmp_135 = lcontext;
int DeviceObject = __tmp_133;
int Irp = __tmp_134;
int Context = __tmp_135;
int Event ;
Event = Context;
{
int __tmp_136 = Event;
int __tmp_137 = 0;
int __tmp_138 = 0;
int Event = __tmp_136;
int Increment = __tmp_137;
int Wait = __tmp_138;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_3848 = l;
}
 __return_3851 = -1073741802;
}
compRetStatus = __return_3851;
goto label_3836;
}
else 
{
{
int __tmp_139 = DeviceObject;
int __tmp_140 = Irp;
int __tmp_141 = lcontext;
int DeviceObject = __tmp_139;
int Irp = __tmp_140;
int Context = __tmp_141;
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int();
int Irp__IoStatus__Information = __VERIFIER_nondet_int() ;
Irp__IoStatus__Information = __VERIFIER_nondet_int();
int partitionCounters__ReadCount = __VERIFIER_nondet_int() ;
partitionCounters__ReadCount = __VERIFIER_nondet_int();
int partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int();
int difference__QuadPart = __VERIFIER_nondet_int() ;
difference__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__WriteCount = __VERIFIER_nondet_int() ;
partitionCounters__WriteCount = __VERIFIER_nondet_int();
int partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int();
int Irp__Flags = __VERIFIER_nondet_int() ;
Irp__Flags = __VERIFIER_nondet_int();
int partitionCounters__SplitCount = __VERIFIER_nondet_int() ;
partitionCounters__SplitCount = __VERIFIER_nondet_int();
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation__Control ;
int partitionCounters = __VERIFIER_nondet_int() ;
partitionCounters = __VERIFIER_nondet_int();
int queueLen = __VERIFIER_nondet_int() ;
queueLen = __VERIFIER_nondet_int();
if (partitionCounters == 0)
{
 __return_3832 = 0;
goto label_3830;
}
else 
{
if (irpStack__MajorFunction == 3)
{
partitionCounters__BytesRead__QuadPart = partitionCounters__BytesRead__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_0 = partitionCounters__ReadCount;
partitionCounters__ReadCount = partitionCounters__ReadCount + 1;
__CPAchecker_TMP_0;
partitionCounters__ReadTime__QuadPart = partitionCounters__ReadTime__QuadPart + difference__QuadPart;
goto label_3794;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_3794:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_3805;
}
else 
{
label_3805:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_3813;
}
else 
{
{
__VERIFIER_error();
}
goto label_3813;
}
}
else 
{
label_3813:; 
 __return_3830 = 0;
label_3830:; 
}
compRetStatus = __return_3830;
label_3836:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_3875;
}
else 
{
{
__VERIFIER_error();
}
label_3875:; 
}
goto label_3748;
}
}
else 
{
goto label_3748;
}
}
}
}
}
}
}
else 
{
label_3748:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3896;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3896;
}
else 
{
returnVal2 = 259;
label_3896:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3924;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3934;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3934:; 
goto label_3924;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3924;
}
else 
{
{
__VERIFIER_error();
}
label_3924:; 
 __return_3945 = returnVal2;
}
status6 = __return_3945;
__cil_tmp37 = (long)status6;
if (__cil_tmp37 == 259L)
{
{
int __tmp_142 = event;
int __tmp_143 = Executive;
int __tmp_144 = KernelMode;
int __tmp_145 = 0;
int __tmp_146 = 0;
int Object = __tmp_142;
int WaitReason = __tmp_143;
int WaitMode = __tmp_144;
int Alertable = __tmp_145;
int Timeout = __tmp_146;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_3968;
}
else 
{
goto label_3958;
}
}
else 
{
label_3958:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_3968;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3968;
}
else 
{
label_3968:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_3999 = 0;
goto label_3995;
}
else 
{
 __return_3995 = -1073741823;
label_3995:; 
}
status6 = ioStatus__Status;
goto label_3951;
}
}
}
}
}
else 
{
label_3951:; 
goto label_3115;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_3115:; 
 __return_4023 = status6;
label_4023:; 
}
Irp__IoStatus__Status = status2;
myStatus = status2;
{
int __tmp_147 = Irp;
int __tmp_148 = 0;
int Irp = __tmp_147;
int PriorityBoost = __tmp_148;
if (s == NP)
{
s = DC;
goto label_4042;
}
else 
{
{
__VERIFIER_error();
}
label_4042:; 
}
 __return_4049 = status2;
}
status1 = __return_4049;
goto label_2415;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
if (irpSp__MinorFunction == 2)
{
{
int __tmp_149 = DeviceObject;
int __tmp_150 = Irp;
int DeviceObject = __tmp_149;
int Irp = __tmp_150;
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int deviceExtension__WmilibContext = __VERIFIER_nondet_int() ;
deviceExtension__WmilibContext = __VERIFIER_nondet_int();
int Irp__IoStatus__Status ;
int status3 ;
int deviceExtension ;
int wmilibContext ;
deviceExtension = DeviceObject__DeviceExtension;
wmilibContext = deviceExtension__WmilibContext;
{
int __tmp_151 = DeviceObject;
int __tmp_152 = Irp;
int DeviceObject = __tmp_151;
int Irp = __tmp_152;
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int deviceExtension ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int status4 ;
int nextIrpSp__Control ;
int irpSp ;
int nextIrpSp ;
int irpSp__Context ;
int irpSp__Control ;
int irpSp___0 ;
long __cil_tmp15 ;
deviceExtension = DeviceObject__DeviceExtension;
irpSp = Irp__Tail__Overlay__CurrentStackLocation;
nextIrpSp = Irp__Tail__Overlay__CurrentStackLocation - 1;
nextIrpSp__Control = 0;
if (s != NP)
{
{
__VERIFIER_error();
}
goto label_2094;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
goto label_2094;
}
else 
{
compRegistered = 1;
routine = 0;
label_2094:; 
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_153 = deviceExtension__TargetDeviceObject;
int __tmp_154 = Irp;
int DeviceObject = __tmp_153;
int Irp = __tmp_154;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_155 = DeviceObject;
int __tmp_156 = Irp;
int __tmp_157 = lcontext;
int DeviceObject = __tmp_155;
int Irp = __tmp_156;
int Context = __tmp_157;
int Event ;
Event = Context;
{
int __tmp_158 = Event;
int __tmp_159 = 0;
int __tmp_160 = 0;
int Event = __tmp_158;
int Increment = __tmp_159;
int Wait = __tmp_160;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2228 = l;
}
 __return_2231 = -1073741802;
}
compRetStatus = __return_2231;
goto label_2216;
}
else 
{
{
int __tmp_161 = DeviceObject;
int __tmp_162 = Irp;
int __tmp_163 = lcontext;
int DeviceObject = __tmp_161;
int Irp = __tmp_162;
int Context = __tmp_163;
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int();
int Irp__IoStatus__Information = __VERIFIER_nondet_int() ;
Irp__IoStatus__Information = __VERIFIER_nondet_int();
int partitionCounters__ReadCount = __VERIFIER_nondet_int() ;
partitionCounters__ReadCount = __VERIFIER_nondet_int();
int partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int();
int difference__QuadPart = __VERIFIER_nondet_int() ;
difference__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__WriteCount = __VERIFIER_nondet_int() ;
partitionCounters__WriteCount = __VERIFIER_nondet_int();
int partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int();
int Irp__Flags = __VERIFIER_nondet_int() ;
Irp__Flags = __VERIFIER_nondet_int();
int partitionCounters__SplitCount = __VERIFIER_nondet_int() ;
partitionCounters__SplitCount = __VERIFIER_nondet_int();
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation__Control ;
int partitionCounters = __VERIFIER_nondet_int() ;
partitionCounters = __VERIFIER_nondet_int();
int queueLen = __VERIFIER_nondet_int() ;
queueLen = __VERIFIER_nondet_int();
if (partitionCounters == 0)
{
 __return_2212 = 0;
goto label_2210;
}
else 
{
if (irpStack__MajorFunction == 3)
{
partitionCounters__BytesRead__QuadPart = partitionCounters__BytesRead__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_0 = partitionCounters__ReadCount;
partitionCounters__ReadCount = partitionCounters__ReadCount + 1;
__CPAchecker_TMP_0;
partitionCounters__ReadTime__QuadPart = partitionCounters__ReadTime__QuadPart + difference__QuadPart;
goto label_2174;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_2174:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_2185;
}
else 
{
label_2185:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_2193;
}
else 
{
{
__VERIFIER_error();
}
goto label_2193;
}
}
else 
{
label_2193:; 
 __return_2210 = 0;
label_2210:; 
}
compRetStatus = __return_2210;
label_2216:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_2255;
}
else 
{
{
__VERIFIER_error();
}
label_2255:; 
}
goto label_2128;
}
}
else 
{
goto label_2128;
}
}
}
}
}
}
}
else 
{
label_2128:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_2276;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2276;
}
else 
{
returnVal2 = 259;
label_2276:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2304;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2314;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2314:; 
goto label_2304;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2304;
}
else 
{
{
__VERIFIER_error();
}
label_2304:; 
 __return_2325 = returnVal2;
}
status4 = __return_2325;
__cil_tmp15 = (long)status4;
if (__cil_tmp15 == 259L)
{
{
int __tmp_164 = event;
int __tmp_165 = Executive;
int __tmp_166 = KernelMode;
int __tmp_167 = 0;
int __tmp_168 = 0;
int Object = __tmp_164;
int WaitReason = __tmp_165;
int WaitMode = __tmp_166;
int Alertable = __tmp_167;
int Timeout = __tmp_168;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_2348;
}
else 
{
goto label_2338;
}
}
else 
{
label_2338:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_2348;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2348;
}
else 
{
label_2348:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2379 = 0;
goto label_2375;
}
else 
{
 __return_2375 = -1073741823;
label_2375:; 
}
status4 = myStatus;
goto label_2331;
}
}
}
}
}
else 
{
label_2331:; 
 __return_2385 = status4;
}
status3 = __return_2385;
Irp__IoStatus__Status = status3;
myStatus = status3;
{
int __tmp_169 = Irp;
int __tmp_170 = 0;
int Irp = __tmp_169;
int PriorityBoost = __tmp_170;
if (s == NP)
{
s = DC;
goto label_2404;
}
else 
{
{
__VERIFIER_error();
}
label_2404:; 
}
 __return_2411 = status3;
}
status1 = __return_2411;
label_2415:; 
 __return_4057 = status1;
goto label_4058;
}
}
}
}
}
}
}
}
}
}
}
else 
{
{
int __tmp_171 = DeviceObject;
int __tmp_172 = Irp;
int DeviceObject = __tmp_171;
int Irp = __tmp_172;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int deviceExtension ;
int tmp ;
if (s == NP)
{
s = SKIP1;
goto label_1822;
}
else 
{
{
__VERIFIER_error();
}
label_1822:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
deviceExtension = DeviceObject__DeviceExtension;
{
int __tmp_173 = deviceExtension__TargetDeviceObject;
int __tmp_174 = Irp;
int DeviceObject = __tmp_173;
int Irp = __tmp_174;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_175 = DeviceObject;
int __tmp_176 = Irp;
int __tmp_177 = lcontext;
int DeviceObject = __tmp_175;
int Irp = __tmp_176;
int Context = __tmp_177;
int Event ;
Event = Context;
{
int __tmp_178 = Event;
int __tmp_179 = 0;
int __tmp_180 = 0;
int Event = __tmp_178;
int Increment = __tmp_179;
int Wait = __tmp_180;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1943 = l;
}
 __return_1946 = -1073741802;
}
compRetStatus = __return_1946;
goto label_1931;
}
else 
{
{
int __tmp_181 = DeviceObject;
int __tmp_182 = Irp;
int __tmp_183 = lcontext;
int DeviceObject = __tmp_181;
int Irp = __tmp_182;
int Context = __tmp_183;
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int();
int Irp__IoStatus__Information = __VERIFIER_nondet_int() ;
Irp__IoStatus__Information = __VERIFIER_nondet_int();
int partitionCounters__ReadCount = __VERIFIER_nondet_int() ;
partitionCounters__ReadCount = __VERIFIER_nondet_int();
int partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int();
int difference__QuadPart = __VERIFIER_nondet_int() ;
difference__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__WriteCount = __VERIFIER_nondet_int() ;
partitionCounters__WriteCount = __VERIFIER_nondet_int();
int partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int();
int Irp__Flags = __VERIFIER_nondet_int() ;
Irp__Flags = __VERIFIER_nondet_int();
int partitionCounters__SplitCount = __VERIFIER_nondet_int() ;
partitionCounters__SplitCount = __VERIFIER_nondet_int();
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation__Control ;
int partitionCounters = __VERIFIER_nondet_int() ;
partitionCounters = __VERIFIER_nondet_int();
int queueLen = __VERIFIER_nondet_int() ;
queueLen = __VERIFIER_nondet_int();
if (partitionCounters == 0)
{
 __return_1927 = 0;
goto label_1925;
}
else 
{
if (irpStack__MajorFunction == 3)
{
partitionCounters__BytesRead__QuadPart = partitionCounters__BytesRead__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_0 = partitionCounters__ReadCount;
partitionCounters__ReadCount = partitionCounters__ReadCount + 1;
__CPAchecker_TMP_0;
partitionCounters__ReadTime__QuadPart = partitionCounters__ReadTime__QuadPart + difference__QuadPart;
goto label_1889;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_1889:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_1900;
}
else 
{
label_1900:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_1908;
}
else 
{
{
__VERIFIER_error();
}
goto label_1908;
}
}
else 
{
label_1908:; 
 __return_1925 = 0;
label_1925:; 
}
compRetStatus = __return_1925;
label_1931:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_1970;
}
else 
{
{
__VERIFIER_error();
}
label_1970:; 
}
goto label_1843;
}
}
else 
{
goto label_1843;
}
}
}
}
}
}
}
else 
{
label_1843:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_1991;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1991;
}
else 
{
returnVal2 = 259;
label_1991:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2019;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2029;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2029:; 
goto label_2019;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2019;
}
else 
{
{
__VERIFIER_error();
}
label_2019:; 
 __return_2040 = returnVal2;
}
tmp = __return_2040;
 __return_2043 = tmp;
}
tmp = __return_2043;
 __return_4058 = tmp;
label_4058:; 
}
status7 = __return_4058;
goto label_1530;
}
}
}
}
}
}
}
}
}
}
else 
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 4)
{
{
int __tmp_184 = devobj;
int __tmp_185 = pirp;
int DeviceObject = __tmp_184;
int Irp = __tmp_185;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int deviceExtension ;
int tmp ;
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
deviceExtension = DeviceObject__DeviceExtension;
{
int __tmp_186 = deviceExtension__TargetDeviceObject;
int __tmp_187 = Irp;
int DeviceObject = __tmp_186;
int Irp = __tmp_187;
int compRetStatus ;
int returnVal ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_188 = DeviceObject;
int __tmp_189 = Irp;
int __tmp_190 = lcontext;
int DeviceObject = __tmp_188;
int Irp = __tmp_189;
int Context = __tmp_190;
int Event ;
Event = Context;
{
int __tmp_191 = Event;
int __tmp_192 = 0;
int __tmp_193 = 0;
int Event = __tmp_191;
int Increment = __tmp_192;
int Wait = __tmp_193;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1673 = l;
}
 __return_1676 = -1073741802;
}
compRetStatus = __return_1676;
goto label_1661;
}
else 
{
{
int __tmp_194 = DeviceObject;
int __tmp_195 = Irp;
int __tmp_196 = lcontext;
int DeviceObject = __tmp_194;
int Irp = __tmp_195;
int Context = __tmp_196;
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int();
int Irp__IoStatus__Information = __VERIFIER_nondet_int() ;
Irp__IoStatus__Information = __VERIFIER_nondet_int();
int partitionCounters__ReadCount = __VERIFIER_nondet_int() ;
partitionCounters__ReadCount = __VERIFIER_nondet_int();
int partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int();
int difference__QuadPart = __VERIFIER_nondet_int() ;
difference__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__WriteCount = __VERIFIER_nondet_int() ;
partitionCounters__WriteCount = __VERIFIER_nondet_int();
int partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int();
int Irp__Flags = __VERIFIER_nondet_int() ;
Irp__Flags = __VERIFIER_nondet_int();
int partitionCounters__SplitCount = __VERIFIER_nondet_int() ;
partitionCounters__SplitCount = __VERIFIER_nondet_int();
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation__Control ;
int partitionCounters = __VERIFIER_nondet_int() ;
partitionCounters = __VERIFIER_nondet_int();
int queueLen = __VERIFIER_nondet_int() ;
queueLen = __VERIFIER_nondet_int();
if (partitionCounters == 0)
{
 __return_1657 = 0;
goto label_1655;
}
else 
{
if (irpStack__MajorFunction == 3)
{
partitionCounters__BytesRead__QuadPart = partitionCounters__BytesRead__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_0 = partitionCounters__ReadCount;
partitionCounters__ReadCount = partitionCounters__ReadCount + 1;
__CPAchecker_TMP_0;
partitionCounters__ReadTime__QuadPart = partitionCounters__ReadTime__QuadPart + difference__QuadPart;
goto label_1619;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_1619:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_1630;
}
else 
{
label_1630:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_1638;
}
else 
{
{
__VERIFIER_error();
}
goto label_1638;
}
}
else 
{
label_1638:; 
 __return_1655 = 0;
label_1655:; 
}
compRetStatus = __return_1655;
label_1661:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_1700;
}
else 
{
{
__VERIFIER_error();
}
label_1700:; 
}
goto label_1570;
}
}
else 
{
goto label_1570;
}
}
}
}
}
}
}
else 
{
label_1570:; 
int tmp_ndt_11;
tmp_ndt_11 = __VERIFIER_nondet_int();
if (tmp_ndt_11 == 0)
{
returnVal = 0;
goto label_1721;
}
else 
{
int tmp_ndt_12;
tmp_ndt_12 = __VERIFIER_nondet_int();
if (tmp_ndt_12 == 1)
{
returnVal = -1073741823;
goto label_1721;
}
else 
{
returnVal = 259;
label_1721:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_1749;
}
else 
{
if (s == MPR1)
{
__cil_tmp8 = (long)returnVal;
if (__cil_tmp8 == 259L)
{
s = MPR3;
lowerDriverReturn = returnVal;
goto label_1760;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1760:; 
goto label_1749;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
goto label_1749;
}
else 
{
{
__VERIFIER_error();
}
label_1749:; 
 __return_1771 = returnVal;
}
tmp = __return_1771;
 __return_1774 = tmp;
}
status7 = __return_1774;
goto label_1530;
}
}
}
}
}
}
}
else 
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 12)
{
{
int __tmp_197 = devobj;
int __tmp_198 = pirp;
int DeviceObject = __tmp_197;
int Irp = __tmp_198;
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int deviceExtension ;
int tmp ;
deviceExtension = DeviceObject__DeviceExtension;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_199 = deviceExtension__TargetDeviceObject;
int __tmp_200 = Irp;
int DeviceObject = __tmp_199;
int Irp = __tmp_200;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_201 = DeviceObject;
int __tmp_202 = Irp;
int __tmp_203 = lcontext;
int DeviceObject = __tmp_201;
int Irp = __tmp_202;
int Context = __tmp_203;
int Event ;
Event = Context;
{
int __tmp_204 = Event;
int __tmp_205 = 0;
int __tmp_206 = 0;
int Event = __tmp_204;
int Increment = __tmp_205;
int Wait = __tmp_206;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1426 = l;
}
 __return_1429 = -1073741802;
}
compRetStatus = __return_1429;
goto label_1414;
}
else 
{
{
int __tmp_207 = DeviceObject;
int __tmp_208 = Irp;
int __tmp_209 = lcontext;
int DeviceObject = __tmp_207;
int Irp = __tmp_208;
int Context = __tmp_209;
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesRead__QuadPart = __VERIFIER_nondet_int();
int Irp__IoStatus__Information = __VERIFIER_nondet_int() ;
Irp__IoStatus__Information = __VERIFIER_nondet_int();
int partitionCounters__ReadCount = __VERIFIER_nondet_int() ;
partitionCounters__ReadCount = __VERIFIER_nondet_int();
int partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__ReadTime__QuadPart = __VERIFIER_nondet_int();
int difference__QuadPart = __VERIFIER_nondet_int() ;
difference__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__BytesWritten__QuadPart = __VERIFIER_nondet_int();
int partitionCounters__WriteCount = __VERIFIER_nondet_int() ;
partitionCounters__WriteCount = __VERIFIER_nondet_int();
int partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int() ;
partitionCounters__WriteTime__QuadPart = __VERIFIER_nondet_int();
int Irp__Flags = __VERIFIER_nondet_int() ;
Irp__Flags = __VERIFIER_nondet_int();
int partitionCounters__SplitCount = __VERIFIER_nondet_int() ;
partitionCounters__SplitCount = __VERIFIER_nondet_int();
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation__Control ;
int partitionCounters = __VERIFIER_nondet_int() ;
partitionCounters = __VERIFIER_nondet_int();
int queueLen = __VERIFIER_nondet_int() ;
queueLen = __VERIFIER_nondet_int();
if (partitionCounters == 0)
{
 __return_1410 = 0;
goto label_1408;
}
else 
{
if (irpStack__MajorFunction == 3)
{
partitionCounters__BytesRead__QuadPart = partitionCounters__BytesRead__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_0 = partitionCounters__ReadCount;
partitionCounters__ReadCount = partitionCounters__ReadCount + 1;
__CPAchecker_TMP_0;
partitionCounters__ReadTime__QuadPart = partitionCounters__ReadTime__QuadPart + difference__QuadPart;
goto label_1372;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_1372:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_1383;
}
else 
{
label_1383:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_1391;
}
else 
{
{
__VERIFIER_error();
}
goto label_1391;
}
}
else 
{
label_1391:; 
 __return_1408 = 0;
label_1408:; 
}
compRetStatus = __return_1408;
label_1414:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_1453;
}
else 
{
{
__VERIFIER_error();
}
label_1453:; 
}
goto label_1326;
}
}
else 
{
goto label_1326;
}
}
}
}
}
}
}
else 
{
label_1326:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_1474;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1474;
}
else 
{
returnVal2 = 259;
label_1474:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1502;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1512;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1512:; 
goto label_1502;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1502;
}
else 
{
{
__VERIFIER_error();
}
label_1502:; 
 __return_1523 = returnVal2;
}
tmp = __return_1523;
 __return_1526 = tmp;
}
status7 = __return_1526;
label_1530:; 
if (we_should_unload == 0)
{
goto label_1238;
}
else 
{
{
int __tmp_210 = d;
int DriverObject = __tmp_210;
}
goto label_1238;
}
}
}
}
}
}
}
}
else 
{
 __return_4564 = -1;
label_4564:; 
return 1;
}
}
}
}
}
}
}
}
else 
{
label_1238:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_4477;
}
else 
{
goto label_4461;
}
}
else 
{
label_4461:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_4477;
}
else 
{
goto label_4469;
}
}
else 
{
label_4469:; 
if (s != UNLOADED)
{
if (status7 != -1)
{
if (s != SKIP2)
{
if (s != IPC)
{
if (s != DC)
{
{
__VERIFIER_error();
}
goto label_4477;
}
else 
{
goto label_4485;
}
}
else 
{
goto label_4485;
}
}
else 
{
label_4485:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_4529;
}
else 
{
label_4529:; 
goto label_4477;
}
}
else 
{
if (s == DC)
{
if (status7 == 259)
{
{
__VERIFIER_error();
}
goto label_4514;
}
else 
{
label_4514:; 
goto label_4477;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_4477;
}
else 
{
goto label_4477;
}
}
}
}
}
else 
{
goto label_4477;
}
}
else 
{
label_4477:; 
 __return_4563 = status7;
goto label_4564;
}
}
}
}
}
