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
int __return_4337;
int __return_4210;
int __return_4147;
int __return_4178;
int __return_3997;
int __return_4000;
int __return_3981;
int __return_3979;
int __return_4094;
int __return_4148;
int __return_2365;
int __return_2368;
int __return_2349;
int __return_2347;
int __return_2462;
int __return_2516;
int __return_2512;
int __return_2522;
int __return_2611;
int __return_2607;
int __return_3793;
int __return_2726;
int __return_2729;
int __return_2710;
int __return_2708;
int __return_2823;
int __return_2877;
int __return_2873;
int __return_3791;
int __return_2910;
int __return_2906;
int __return_3789;
int __return_3025;
int __return_3028;
int __return_3009;
int __return_3007;
int __return_3122;
int __return_3176;
int __return_3172;
int __return_3787;
int __return_3209;
int __return_3205;
int __return_3785;
int __return_3324;
int __return_3327;
int __return_3308;
int __return_3306;
int __return_3421;
int __return_3476;
int __return_3472;
int __return_3783;
int __return_3504;
int __return_3500;
int __return_3781;
int __return_3619;
int __return_3622;
int __return_3603;
int __return_3601;
int __return_3716;
int __return_3770;
int __return_3766;
int __return_3794;
int __return_3820;
int __return_1999;
int __return_2002;
int __return_1983;
int __return_1981;
int __return_2096;
int __return_2150;
int __return_2146;
int __return_2156;
int __return_2182;
int __return_3828;
int __return_1714;
int __return_1717;
int __return_1698;
int __return_1696;
int __return_1811;
int __return_1814;
int __return_3829;
int __return_1444;
int __return_1447;
int __return_1428;
int __return_1426;
int __return_1542;
int __return_1545;
int __return_1197;
int __return_1200;
int __return_1181;
int __return_1179;
int __return_1294;
int __return_1297;
int __return_4335;
int __return_4334;
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
goto label_1021;
}
else 
{
label_1021:; 
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
 __return_4337 = -1;
goto label_4335;
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
goto label_4203;
}
else 
{
{
__VERIFIER_error();
}
label_4203:; 
}
 __return_4210 = 0;
}
status7 = __return_4210;
goto label_1301;
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
goto label_4153;
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
goto label_4139;
}
else 
{
{
__VERIFIER_error();
}
label_4139:; 
}
 __return_4147 = -1073741823;
goto label_4148;
}
}
else 
{
totalCounters = Irp__AssociatedIrp__SystemBuffer;
i = 0;
label_4107:; 
if (i >= deviceExtension__Processors)
{
totalCounters__QueueDepth = deviceExtension__QueueDepth;
status5 = 0;
Irp__IoStatus__Information = sizeof__DISK_PERFORMANCE;
label_4153:; 
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
goto label_4170;
}
else 
{
{
__VERIFIER_error();
}
label_4170:; 
}
 __return_4178 = status5;
goto label_4148;
}
}
else 
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_4107;
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
 __return_3997 = l;
}
 __return_4000 = -1073741802;
}
compRetStatus = __return_4000;
goto label_3985;
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
 __return_3981 = 0;
goto label_3979;
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
goto label_3943;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_3943:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_3954;
}
else 
{
label_3954:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_3962;
}
else 
{
{
__VERIFIER_error();
}
goto label_3962;
}
}
else 
{
label_3962:; 
 __return_3979 = 0;
label_3979:; 
}
compRetStatus = __return_3979;
label_3985:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_4024;
}
else 
{
{
__VERIFIER_error();
}
label_4024:; 
}
goto label_3897;
}
}
else 
{
goto label_3897;
}
}
}
}
}
}
}
else 
{
label_3897:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4045;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4045;
}
else 
{
returnVal2 = 259;
label_4045:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4073;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4083;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4083:; 
goto label_4073;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_4073;
}
else 
{
{
__VERIFIER_error();
}
label_4073:; 
 __return_4094 = returnVal2;
}
tmp = __return_4094;
 __return_4148 = tmp;
label_4148:; 
}
status7 = __return_4148;
goto label_1301;
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
goto label_2231;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
goto label_2231;
}
else 
{
compRegistered = 1;
routine = 0;
label_2231:; 
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
 __return_2365 = l;
}
 __return_2368 = -1073741802;
}
compRetStatus = __return_2368;
goto label_2353;
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
 __return_2349 = 0;
goto label_2347;
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
goto label_2311;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_2311:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_2322;
}
else 
{
label_2322:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_2330;
}
else 
{
{
__VERIFIER_error();
}
goto label_2330;
}
}
else 
{
label_2330:; 
 __return_2347 = 0;
label_2347:; 
}
compRetStatus = __return_2347;
label_2353:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_2392;
}
else 
{
{
__VERIFIER_error();
}
label_2392:; 
}
goto label_2265;
}
}
else 
{
goto label_2265;
}
}
}
}
}
}
}
else 
{
label_2265:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_2413;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2413;
}
else 
{
returnVal2 = 259;
label_2413:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2441;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2451;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2451:; 
goto label_2441;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2441;
}
else 
{
{
__VERIFIER_error();
}
label_2441:; 
 __return_2462 = returnVal2;
}
status4 = __return_2462;
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
goto label_2485;
}
else 
{
goto label_2475;
}
}
else 
{
label_2475:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_2485;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2485;
}
else 
{
label_2485:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2516 = 0;
goto label_2512;
}
else 
{
 __return_2512 = -1073741823;
label_2512:; 
}
status4 = myStatus;
goto label_2468;
}
}
}
}
}
else 
{
label_2468:; 
 __return_2522 = status4;
}
status2 = __return_2522;
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
 __return_2611 = malloc_ret;
goto label_2607;
}
else 
{
 __return_2607 = 0;
label_2607:; 
}
irp = __return_2607;
if (irp == 0)
{
 __return_3793 = -1073741670;
goto label_3794;
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
 __return_2726 = l;
}
 __return_2729 = -1073741802;
}
compRetStatus = __return_2729;
goto label_2714;
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
 __return_2710 = 0;
goto label_2708;
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
goto label_2672;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_2672:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_2683;
}
else 
{
label_2683:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_2691;
}
else 
{
{
__VERIFIER_error();
}
goto label_2691;
}
}
else 
{
label_2691:; 
 __return_2708 = 0;
label_2708:; 
}
compRetStatus = __return_2708;
label_2714:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_2753;
}
else 
{
{
__VERIFIER_error();
}
label_2753:; 
}
goto label_2626;
}
}
else 
{
goto label_2626;
}
}
}
}
}
}
}
else 
{
label_2626:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_2774;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2774;
}
else 
{
returnVal2 = 259;
label_2774:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2802;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2812;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2812:; 
goto label_2802;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2802;
}
else 
{
{
__VERIFIER_error();
}
label_2802:; 
 __return_2823 = returnVal2;
}
status6 = __return_2823;
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
goto label_2846;
}
else 
{
goto label_2836;
}
}
else 
{
label_2836:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_2846;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2846;
}
else 
{
label_2846:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2877 = 0;
goto label_2873;
}
else 
{
 __return_2873 = -1073741823;
label_2873:; 
}
status6 = ioStatus__Status;
goto label_2829;
}
}
}
}
}
else 
{
label_2829:; 
if (status6 < 0)
{
outputSize = sizeof__MOUNTDEV_NAME;
if (output == 0)
{
 __return_3791 = -1073741670;
goto label_3794;
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
 __return_2910 = malloc_ret;
goto label_2906;
}
else 
{
 __return_2906 = 0;
label_2906:; 
}
irp = __return_2906;
if (irp == 0)
{
 __return_3789 = -1073741670;
goto label_3794;
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
 __return_3025 = l;
}
 __return_3028 = -1073741802;
}
compRetStatus = __return_3028;
goto label_3013;
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
 __return_3009 = 0;
goto label_3007;
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
goto label_2971;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_2971:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_2982;
}
else 
{
label_2982:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_2990;
}
else 
{
{
__VERIFIER_error();
}
goto label_2990;
}
}
else 
{
label_2990:; 
 __return_3007 = 0;
label_3007:; 
}
compRetStatus = __return_3007;
label_3013:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_3052;
}
else 
{
{
__VERIFIER_error();
}
label_3052:; 
}
goto label_2925;
}
}
else 
{
goto label_2925;
}
}
}
}
}
}
}
else 
{
label_2925:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3073;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3073;
}
else 
{
returnVal2 = 259;
label_3073:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3101;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3111;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3111:; 
goto label_3101;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3101;
}
else 
{
{
__VERIFIER_error();
}
label_3101:; 
 __return_3122 = returnVal2;
}
status6 = __return_3122;
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
goto label_3145;
}
else 
{
goto label_3135;
}
}
else 
{
label_3135:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_3145;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3145;
}
else 
{
label_3145:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_3176 = 0;
goto label_3172;
}
else 
{
 __return_3172 = -1073741823;
label_3172:; 
}
status6 = ioStatus__Status;
goto label_3128;
}
}
}
}
}
else 
{
label_3128:; 
__cil_tmp28 = (unsigned long)status6;
if (__cil_tmp28 == -2147483643)
{
outputSize = sizeof__MOUNTDEV_NAME + output__NameLength;
if (output == 0)
{
 __return_3787 = -1073741670;
goto label_3794;
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
 __return_3209 = malloc_ret;
goto label_3205;
}
else 
{
 __return_3205 = 0;
label_3205:; 
}
irp = __return_3205;
if (irp == 0)
{
 __return_3785 = -1073741670;
goto label_3794;
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
 __return_3324 = l;
}
 __return_3327 = -1073741802;
}
compRetStatus = __return_3327;
goto label_3312;
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
 __return_3308 = 0;
goto label_3306;
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
goto label_3270;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_3270:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_3281;
}
else 
{
label_3281:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_3289;
}
else 
{
{
__VERIFIER_error();
}
goto label_3289;
}
}
else 
{
label_3289:; 
 __return_3306 = 0;
label_3306:; 
}
compRetStatus = __return_3306;
label_3312:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_3351;
}
else 
{
{
__VERIFIER_error();
}
label_3351:; 
}
goto label_3224;
}
}
else 
{
goto label_3224;
}
}
}
}
}
}
}
else 
{
label_3224:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3372;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3372;
}
else 
{
returnVal2 = 259;
label_3372:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3400;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3410;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3410:; 
goto label_3400;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3400;
}
else 
{
{
__VERIFIER_error();
}
label_3400:; 
 __return_3421 = returnVal2;
}
status6 = __return_3421;
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
goto label_3445;
}
else 
{
goto label_3435;
}
}
else 
{
label_3435:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_3445;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3445;
}
else 
{
label_3445:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_3476 = 0;
goto label_3472;
}
else 
{
 __return_3472 = -1073741823;
label_3472:; 
}
status6 = ioStatus__Status;
goto label_3185;
}
}
}
}
}
else 
{
goto label_3185;
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
label_3185:; 
if (status6 < 0)
{
 __return_3783 = status6;
goto label_3794;
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
 __return_3504 = malloc_ret;
goto label_3500;
}
else 
{
 __return_3500 = 0;
label_3500:; 
}
irp = __return_3500;
if (irp == 0)
{
 __return_3781 = -1073741670;
goto label_3794;
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
 __return_3619 = l;
}
 __return_3622 = -1073741802;
}
compRetStatus = __return_3622;
goto label_3607;
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
 __return_3603 = 0;
goto label_3601;
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
goto label_3565;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_3565:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_3576;
}
else 
{
label_3576:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_3584;
}
else 
{
{
__VERIFIER_error();
}
goto label_3584;
}
}
else 
{
label_3584:; 
 __return_3601 = 0;
label_3601:; 
}
compRetStatus = __return_3601;
label_3607:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_3646;
}
else 
{
{
__VERIFIER_error();
}
label_3646:; 
}
goto label_3519;
}
}
else 
{
goto label_3519;
}
}
}
}
}
}
}
else 
{
label_3519:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3667;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3667;
}
else 
{
returnVal2 = 259;
label_3667:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3695;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3705;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3705:; 
goto label_3695;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3695;
}
else 
{
{
__VERIFIER_error();
}
label_3695:; 
 __return_3716 = returnVal2;
}
status6 = __return_3716;
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
goto label_3739;
}
else 
{
goto label_3729;
}
}
else 
{
label_3729:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_3739;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3739;
}
else 
{
label_3739:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_3770 = 0;
goto label_3766;
}
else 
{
 __return_3766 = -1073741823;
label_3766:; 
}
status6 = ioStatus__Status;
goto label_3722;
}
}
}
}
}
else 
{
label_3722:; 
goto label_2886;
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
label_2886:; 
 __return_3794 = status6;
label_3794:; 
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
goto label_3813;
}
else 
{
{
__VERIFIER_error();
}
label_3813:; 
}
 __return_3820 = status2;
}
status1 = __return_3820;
goto label_2186;
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
goto label_1865;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
goto label_1865;
}
else 
{
compRegistered = 1;
routine = 0;
label_1865:; 
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
 __return_1999 = l;
}
 __return_2002 = -1073741802;
}
compRetStatus = __return_2002;
goto label_1987;
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
 __return_1983 = 0;
goto label_1981;
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
goto label_1945;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_1945:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_1956;
}
else 
{
label_1956:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_1964;
}
else 
{
{
__VERIFIER_error();
}
goto label_1964;
}
}
else 
{
label_1964:; 
 __return_1981 = 0;
label_1981:; 
}
compRetStatus = __return_1981;
label_1987:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_2026;
}
else 
{
{
__VERIFIER_error();
}
label_2026:; 
}
goto label_1899;
}
}
else 
{
goto label_1899;
}
}
}
}
}
}
}
else 
{
label_1899:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_2047;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2047;
}
else 
{
returnVal2 = 259;
label_2047:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2075;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2085;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2085:; 
goto label_2075;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2075;
}
else 
{
{
__VERIFIER_error();
}
label_2075:; 
 __return_2096 = returnVal2;
}
status4 = __return_2096;
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
goto label_2119;
}
else 
{
goto label_2109;
}
}
else 
{
label_2109:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_2119;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2119;
}
else 
{
label_2119:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2150 = 0;
goto label_2146;
}
else 
{
 __return_2146 = -1073741823;
label_2146:; 
}
status4 = myStatus;
goto label_2102;
}
}
}
}
}
else 
{
label_2102:; 
 __return_2156 = status4;
}
status3 = __return_2156;
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
goto label_2175;
}
else 
{
{
__VERIFIER_error();
}
label_2175:; 
}
 __return_2182 = status3;
}
status1 = __return_2182;
label_2186:; 
 __return_3828 = status1;
goto label_3829;
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
goto label_1593;
}
else 
{
{
__VERIFIER_error();
}
label_1593:; 
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
 __return_1714 = l;
}
 __return_1717 = -1073741802;
}
compRetStatus = __return_1717;
goto label_1702;
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
 __return_1698 = 0;
goto label_1696;
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
goto label_1660;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_1660:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_1671;
}
else 
{
label_1671:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_1679;
}
else 
{
{
__VERIFIER_error();
}
goto label_1679;
}
}
else 
{
label_1679:; 
 __return_1696 = 0;
label_1696:; 
}
compRetStatus = __return_1696;
label_1702:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_1741;
}
else 
{
{
__VERIFIER_error();
}
label_1741:; 
}
goto label_1614;
}
}
else 
{
goto label_1614;
}
}
}
}
}
}
}
else 
{
label_1614:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_1762;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1762;
}
else 
{
returnVal2 = 259;
label_1762:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1790;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1800;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1800:; 
goto label_1790;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1790;
}
else 
{
{
__VERIFIER_error();
}
label_1790:; 
 __return_1811 = returnVal2;
}
tmp = __return_1811;
 __return_1814 = tmp;
}
tmp = __return_1814;
 __return_3829 = tmp;
label_3829:; 
}
status7 = __return_3829;
goto label_1301;
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
 __return_1444 = l;
}
 __return_1447 = -1073741802;
}
compRetStatus = __return_1447;
goto label_1432;
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
 __return_1428 = 0;
goto label_1426;
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
goto label_1390;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_1390:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_1401;
}
else 
{
label_1401:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_1409;
}
else 
{
{
__VERIFIER_error();
}
goto label_1409;
}
}
else 
{
label_1409:; 
 __return_1426 = 0;
label_1426:; 
}
compRetStatus = __return_1426;
label_1432:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_1471;
}
else 
{
{
__VERIFIER_error();
}
label_1471:; 
}
goto label_1341;
}
}
else 
{
goto label_1341;
}
}
}
}
}
}
}
else 
{
label_1341:; 
int tmp_ndt_11;
tmp_ndt_11 = __VERIFIER_nondet_int();
if (tmp_ndt_11 == 0)
{
returnVal = 0;
goto label_1492;
}
else 
{
int tmp_ndt_12;
tmp_ndt_12 = __VERIFIER_nondet_int();
if (tmp_ndt_12 == 1)
{
returnVal = -1073741823;
goto label_1492;
}
else 
{
returnVal = 259;
label_1492:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_1520;
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
goto label_1531;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1531:; 
goto label_1520;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
goto label_1520;
}
else 
{
{
__VERIFIER_error();
}
label_1520:; 
 __return_1542 = returnVal;
}
tmp = __return_1542;
 __return_1545 = tmp;
}
status7 = __return_1545;
goto label_1301;
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
 __return_1197 = l;
}
 __return_1200 = -1073741802;
}
compRetStatus = __return_1200;
goto label_1185;
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
 __return_1181 = 0;
goto label_1179;
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
goto label_1143;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_1143:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_1154;
}
else 
{
label_1154:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_1162;
}
else 
{
{
__VERIFIER_error();
}
goto label_1162;
}
}
else 
{
label_1162:; 
 __return_1179 = 0;
label_1179:; 
}
compRetStatus = __return_1179;
label_1185:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_1224;
}
else 
{
{
__VERIFIER_error();
}
label_1224:; 
}
goto label_1097;
}
}
else 
{
goto label_1097;
}
}
}
}
}
}
}
else 
{
label_1097:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_1245;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1245;
}
else 
{
returnVal2 = 259;
label_1245:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1273;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1283;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1283:; 
goto label_1273;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1273;
}
else 
{
{
__VERIFIER_error();
}
label_1273:; 
 __return_1294 = returnVal2;
}
tmp = __return_1294;
 __return_1297 = tmp;
}
status7 = __return_1297;
label_1301:; 
if (we_should_unload == 0)
{
goto label_1009;
}
else 
{
{
int __tmp_210 = d;
int DriverObject = __tmp_210;
}
goto label_1009;
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
 __return_4335 = -1;
label_4335:; 
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
label_1009:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_4248;
}
else 
{
goto label_4232;
}
}
else 
{
label_4232:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_4248;
}
else 
{
goto label_4240;
}
}
else 
{
label_4240:; 
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
goto label_4248;
}
else 
{
goto label_4256;
}
}
else 
{
goto label_4256;
}
}
else 
{
label_4256:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_4300;
}
else 
{
label_4300:; 
goto label_4248;
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
goto label_4285;
}
else 
{
label_4285:; 
goto label_4248;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_4248;
}
else 
{
goto label_4248;
}
}
}
}
}
else 
{
goto label_4248;
}
}
else 
{
label_4248:; 
 __return_4334 = status7;
goto label_4335;
}
}
}
}
}
