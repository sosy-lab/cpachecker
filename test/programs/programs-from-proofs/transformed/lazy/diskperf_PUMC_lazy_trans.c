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
int __return_10067;
int __return_9363;
int __return_10061;
int __return_10055;
int __return_9327;
int __return_10053;
int __return_9275;
int __return_9209;
int __return_9214;
int __return_9227;
int __return_8923;
int __return_8926;
int __return_9212;
int __return_9213;
int __return_9210;
int __return_8905;
int __return_8904;
int __return_8906;
int __return_9211;
int __return_5907;
int __return_5912;
int __return_5621;
int __return_5624;
int __return_5910;
int __return_5911;
int __return_5908;
int __return_5603;
int __return_5602;
int __return_5604;
int __return_5909;
int __return_5497;
int __return_5502;
int __return_5211;
int __return_5214;
int __return_5500;
int __return_5501;
int __return_5498;
int __return_5193;
int __return_5192;
int __return_5194;
int __return_5499;
int __return_5087;
int __return_5092;
int __return_5979;
int __return_5980;
int __return_5981;
int __return_5982;
int __return_5989;
int __return_5990;
int __return_6079;
int __return_6080;
int __return_8694;
int __return_8747;
int __return_8755;
int __return_6477;
int __return_6482;
int __return_6549;
int __return_6550;
int __return_6551;
int __return_6552;
int __return_8692;
int __return_6592;
int __return_6593;
int __return_8689;
int __return_7009;
int __return_7014;
int __return_7081;
int __return_7082;
int __return_7083;
int __return_7084;
int __return_8685;
int __return_7143;
int __return_7144;
int __return_8686;
int __return_7697;
int __return_7698;
int __return_8679;
int __return_8526;
int __return_8531;
int __return_8601;
int __return_8602;
int __return_8603;
int __return_8604;
int __return_8675;
int __return_8674;
int __return_8240;
int __return_8243;
int __return_8529;
int __return_8530;
int __return_8527;
int __return_8222;
int __return_8221;
int __return_8223;
int __return_8528;
int __return_8687;
int __return_7124;
int __return_7125;
int __return_8684;
int __return_7541;
int __return_7546;
int __return_7614;
int __return_7615;
int __return_7616;
int __return_7617;
int __return_8683;
int __return_7659;
int __return_7660;
int __return_8678;
int __return_8116;
int __return_8121;
int __return_8660;
int __return_8661;
int __return_8662;
int __return_8663;
int __return_8673;
int __return_7830;
int __return_7833;
int __return_8119;
int __return_8120;
int __return_8117;
int __return_7812;
int __return_7811;
int __return_7813;
int __return_8118;
int __return_8682;
int __return_7678;
int __return_7679;
int __return_7255;
int __return_7258;
int __return_7544;
int __return_7545;
int __return_7542;
int __return_7237;
int __return_7236;
int __return_7238;
int __return_7543;
int __return_8688;
int __return_7716;
int __return_7717;
int __return_6723;
int __return_6726;
int __return_7012;
int __return_7013;
int __return_7010;
int __return_6705;
int __return_6704;
int __return_6706;
int __return_7011;
int __return_8693;
int __return_8690;
int __return_6611;
int __return_6612;
int __return_8691;
int __return_6191;
int __return_6194;
int __return_6480;
int __return_6481;
int __return_6478;
int __return_6173;
int __return_6172;
int __return_6174;
int __return_6479;
int __return_4801;
int __return_4804;
int __return_5090;
int __return_5091;
int __return_5088;
int __return_4783;
int __return_4782;
int __return_4784;
int __return_5089;
int __return_4508;
int __return_4513;
int __return_4222;
int __return_4225;
int __return_4511;
int __return_4512;
int __return_4509;
int __return_4204;
int __return_4203;
int __return_4205;
int __return_4510;
int __return_4098;
int __return_4103;
int __return_3812;
int __return_3815;
int __return_4101;
int __return_4102;
int __return_4099;
int __return_3794;
int __return_3793;
int __return_3795;
int __return_4100;
int __return_3688;
int __return_3693;
int __return_4580;
int __return_4581;
int __return_4582;
int __return_4583;
int __return_4590;
int __return_4591;
int __return_4620;
int __return_8756;
int __return_10051;
int __return_3402;
int __return_3405;
int __return_3691;
int __return_3692;
int __return_3689;
int __return_3384;
int __return_3383;
int __return_3385;
int __return_3690;
int __return_3201;
int __return_3206;
int __return_2915;
int __return_2918;
int __return_3204;
int __return_3205;
int __return_3202;
int __return_2897;
int __return_2896;
int __return_2898;
int __return_3203;
int __return_2791;
int __return_2796;
int __return_3219;
int __return_3222;
int __return_2505;
int __return_2508;
int __return_2794;
int __return_2795;
int __return_2792;
int __return_2487;
int __return_2486;
int __return_2488;
int __return_2793;
int __return_2318;
int __return_2323;
int __return_2336;
int __return_10049;
int __return_2028;
int __return_2031;
int __return_2321;
int __return_2322;
int __return_2319;
int __return_2010;
int __return_2009;
int __return_2011;
int __return_2320;
int __return_1869;
int __return_1874;
int __return_1887;
int __return_10047;
int __return_10063;
int __return_10045;
int __return_1583;
int __return_1586;
int __return_1872;
int __return_1873;
int __return_1870;
int __return_1565;
int __return_1564;
int __return_1566;
int __return_1871;
int __return_1454;
int __return_10057;
int __return_10059;
int __return_10065;
int __return_10043;
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
goto label_1415;
}
else 
{
label_1415:; 
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
 __return_10067 = -1;
goto label_1454;
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
}
else 
{
{
__VERIFIER_error();
}
}
goto label_9360;
label_9360:; 
 __return_9363 = 0;
}
status7 = __return_9363;
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10029;
}
else 
{
goto label_9459;
}
}
else 
{
label_9459:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10029;
}
else 
{
goto label_9520;
}
}
else 
{
label_9520:; 
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
goto label_9934;
}
else 
{
goto label_9646;
}
}
else 
{
goto label_9646;
}
}
else 
{
label_9646:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
label_9860:; 
 __return_10061 = status7;
goto label_1454;
}
else 
{
goto label_10029;
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
goto label_9782;
}
else 
{
goto label_10029;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9710;
}
else 
{
goto label_10029;
}
}
}
}
}
else 
{
goto label_10029;
}
}
else 
{
label_10029:; 
 __return_10055 = status7;
goto label_1454;
}
}
}
}
else 
{
{
int __tmp_5 = d;
int DriverObject = __tmp_5;
}
goto label_9392;
}
}
}
else 
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 2)
{
{
int __tmp_6 = devobj;
int __tmp_7 = pirp;
int DeviceObject = __tmp_6;
int Irp = __tmp_7;
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
Irp__IoStatus__Status = status5;
myStatus = status5;
{
int __tmp_8 = Irp;
int __tmp_9 = 0;
int Irp = __tmp_8;
int PriorityBoost = __tmp_9;
if (s == NP)
{
s = DC;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_9302;
label_9302:; 
 __return_9327 = status5;
}
status7 = __return_9327;
label_9329:; 
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10031;
}
else 
{
goto label_9461;
}
}
else 
{
label_9461:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10031;
}
else 
{
goto label_9518;
}
}
else 
{
label_9518:; 
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
goto label_9934;
}
else 
{
goto label_9644;
}
}
else 
{
goto label_9644;
}
}
else 
{
label_9644:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_9860;
}
else 
{
goto label_10031;
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
goto label_9782;
}
else 
{
goto label_10031;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9710;
}
else 
{
goto label_10031;
}
}
}
}
}
else 
{
goto label_10031;
}
}
else 
{
label_10031:; 
 __return_10053 = status7;
goto label_1454;
}
}
}
}
else 
{
{
int __tmp_10 = d;
int DriverObject = __tmp_10;
}
goto label_9392;
}
}
else 
{
diskCounters = deviceExtension__DiskCounters;
if (diskCounters == 0)
{
Irp__IoStatus__Status = -1073741823;
myStatus = -1073741823;
{
int __tmp_11 = Irp;
int __tmp_12 = 0;
int Irp = __tmp_11;
int PriorityBoost = __tmp_12;
if (s == NP)
{
s = DC;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_9272;
label_9272:; 
 __return_9275 = -1073741823;
}
status7 = __return_9275;
goto label_9329;
}
else 
{
totalCounters = Irp__AssociatedIrp__SystemBuffer;
i = 0;
label_9236:; 
if (i >= deviceExtension__Processors)
{
totalCounters__QueueDepth = deviceExtension__QueueDepth;
status5 = 0;
Irp__IoStatus__Information = sizeof__DISK_PERFORMANCE;
Irp__IoStatus__Status = status5;
myStatus = status5;
{
int __tmp_13 = Irp;
int __tmp_14 = 0;
int Irp = __tmp_13;
int PriorityBoost = __tmp_14;
if (s == NP)
{
s = DC;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_9302;
goto label_9302;
}
}
else 
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_9236;
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
int __tmp_15 = deviceExtension__TargetDeviceObject;
int __tmp_16 = Irp;
int DeviceObject = __tmp_15;
int Irp = __tmp_16;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_9070;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_9070;
}
else 
{
returnVal2 = 259;
label_9070:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_9208;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_9184;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_9184:; 
goto label_9208;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_9208:; 
 __return_9209 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_9214 = returnVal2;
}
tmp = __return_9209;
goto label_9216;
tmp = __return_9214;
label_9216:; 
 __return_9227 = tmp;
}
status7 = __return_9227;
goto label_9329;
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_17 = DeviceObject;
int __tmp_18 = Irp;
int __tmp_19 = lcontext;
int DeviceObject = __tmp_17;
int Irp = __tmp_18;
int Context = __tmp_19;
int Event ;
Event = Context;
{
int __tmp_20 = Event;
int __tmp_21 = 0;
int __tmp_22 = 0;
int Event = __tmp_20;
int Increment = __tmp_21;
int Wait = __tmp_22;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_8923 = l;
}
 __return_8926 = -1073741802;
}
compRetStatus = __return_8926;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_8957;
label_8957:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_9064;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_9064;
}
else 
{
returnVal2 = 259;
label_9064:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_9202;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_9178;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_9178:; 
goto label_9202;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_9202:; 
 __return_9212 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_9110:; 
 __return_9213 = returnVal2;
}
tmp = __return_9212;
goto label_9216;
tmp = __return_9213;
goto label_9216;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_9068;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_9068;
}
else 
{
returnVal2 = 259;
label_9068:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_9206;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_9182;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_9182:; 
goto label_9206;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_9206:; 
 __return_9210 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_9110;
}
tmp = __return_9210;
goto label_9216;
}
}
}
}
}
}
else 
{
{
int __tmp_23 = DeviceObject;
int __tmp_24 = Irp;
int __tmp_25 = lcontext;
int DeviceObject = __tmp_23;
int Irp = __tmp_24;
int Context = __tmp_25;
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
 __return_8905 = 0;
goto label_8906;
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
goto label_8878;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_8878:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_8886;
}
else 
{
label_8886:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_8902;
}
else 
{
{
__VERIFIER_error();
}
 __return_8904 = 0;
}
compRetStatus = __return_8904;
goto label_8908;
}
else 
{
label_8902:; 
 __return_8906 = 0;
label_8906:; 
}
compRetStatus = __return_8906;
label_8908:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_8957;
goto label_8957;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_9066;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_9066;
}
else 
{
returnVal2 = 259;
label_9066:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_9204;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_9180;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_9180:; 
goto label_9204;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_9204:; 
 __return_9211 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_9110;
}
tmp = __return_9211;
goto label_9216;
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
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 3)
{
{
int __tmp_26 = devobj;
int __tmp_27 = pirp;
int DeviceObject = __tmp_26;
int Irp = __tmp_27;
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
int __tmp_28 = DeviceObject;
int __tmp_29 = Irp;
int DeviceObject = __tmp_28;
int Irp = __tmp_29;
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int Irp__IoStatus__Status ;
int deviceExtension ;
int status2 ;
deviceExtension = DeviceObject__DeviceExtension;
{
int __tmp_30 = DeviceObject;
int __tmp_31 = Irp;
int DeviceObject = __tmp_30;
int Irp = __tmp_31;
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
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_32 = deviceExtension__TargetDeviceObject;
int __tmp_33 = Irp;
int DeviceObject = __tmp_32;
int Irp = __tmp_33;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5768;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5768;
}
else 
{
returnVal2 = 259;
label_5768:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5906;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5882;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5882:; 
goto label_5906;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5906:; 
 __return_5907 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_5912 = returnVal2;
}
status4 = __return_5907;
goto label_5094;
status4 = __return_5912;
goto label_5094;
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_34 = DeviceObject;
int __tmp_35 = Irp;
int __tmp_36 = lcontext;
int DeviceObject = __tmp_34;
int Irp = __tmp_35;
int Context = __tmp_36;
int Event ;
Event = Context;
{
int __tmp_37 = Event;
int __tmp_38 = 0;
int __tmp_39 = 0;
int Event = __tmp_37;
int Increment = __tmp_38;
int Wait = __tmp_39;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_5621 = l;
}
 __return_5624 = -1073741802;
}
compRetStatus = __return_5624;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_5655;
label_5655:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5762;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5762;
}
else 
{
returnVal2 = 259;
label_5762:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5900;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5876;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5876:; 
goto label_5900;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5900:; 
 __return_5910 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_5808:; 
 __return_5911 = returnVal2;
}
status4 = __return_5910;
goto label_5094;
status4 = __return_5911;
goto label_5094;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5766;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5766;
}
else 
{
returnVal2 = 259;
label_5766:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5904;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5880;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5880:; 
goto label_5904;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5904:; 
 __return_5908 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_5808;
}
status4 = __return_5908;
goto label_5094;
}
}
}
}
}
}
else 
{
{
int __tmp_40 = DeviceObject;
int __tmp_41 = Irp;
int __tmp_42 = lcontext;
int DeviceObject = __tmp_40;
int Irp = __tmp_41;
int Context = __tmp_42;
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
 __return_5603 = 0;
goto label_5604;
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
goto label_5576;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_5576:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_5584;
}
else 
{
label_5584:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_5600;
}
else 
{
{
__VERIFIER_error();
}
 __return_5602 = 0;
}
compRetStatus = __return_5602;
goto label_5606;
}
else 
{
label_5600:; 
 __return_5604 = 0;
label_5604:; 
}
compRetStatus = __return_5604;
label_5606:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_5655;
goto label_5655;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5764;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5764;
}
else 
{
returnVal2 = 259;
label_5764:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5902;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5878;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5878:; 
goto label_5902;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5902:; 
 __return_5909 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_5808;
}
status4 = __return_5909;
goto label_5094;
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
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_43 = deviceExtension__TargetDeviceObject;
int __tmp_44 = Irp;
int DeviceObject = __tmp_43;
int Irp = __tmp_44;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5358;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5358;
}
else 
{
returnVal2 = 259;
label_5358:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5496;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5472;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5472:; 
goto label_5496;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5496:; 
 __return_5497 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_5502 = returnVal2;
}
status4 = __return_5497;
goto label_5094;
status4 = __return_5502;
goto label_5094;
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_45 = DeviceObject;
int __tmp_46 = Irp;
int __tmp_47 = lcontext;
int DeviceObject = __tmp_45;
int Irp = __tmp_46;
int Context = __tmp_47;
int Event ;
Event = Context;
{
int __tmp_48 = Event;
int __tmp_49 = 0;
int __tmp_50 = 0;
int Event = __tmp_48;
int Increment = __tmp_49;
int Wait = __tmp_50;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_5211 = l;
}
 __return_5214 = -1073741802;
}
compRetStatus = __return_5214;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_5245;
label_5245:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5352;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5352;
}
else 
{
returnVal2 = 259;
label_5352:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5490;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5466;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5466:; 
goto label_5490;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5490:; 
 __return_5500 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_5398:; 
 __return_5501 = returnVal2;
}
status4 = __return_5500;
goto label_5094;
status4 = __return_5501;
goto label_5094;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5356;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5356;
}
else 
{
returnVal2 = 259;
label_5356:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5494;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5470;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5470:; 
goto label_5494;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5494:; 
 __return_5498 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_5398;
}
status4 = __return_5498;
goto label_5094;
}
}
}
}
}
}
else 
{
{
int __tmp_51 = DeviceObject;
int __tmp_52 = Irp;
int __tmp_53 = lcontext;
int DeviceObject = __tmp_51;
int Irp = __tmp_52;
int Context = __tmp_53;
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
 __return_5193 = 0;
goto label_5194;
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
goto label_5166;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_5166:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_5174;
}
else 
{
label_5174:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_5190;
}
else 
{
{
__VERIFIER_error();
}
 __return_5192 = 0;
}
compRetStatus = __return_5192;
goto label_5196;
}
else 
{
label_5190:; 
 __return_5194 = 0;
label_5194:; 
}
compRetStatus = __return_5194;
label_5196:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_5245;
goto label_5245;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5354;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5354;
}
else 
{
returnVal2 = 259;
label_5354:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5492;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5468;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5468:; 
goto label_5492;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5492:; 
 __return_5499 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_5398;
}
status4 = __return_5499;
goto label_5094;
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
compRegistered = 1;
routine = 0;
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_54 = deviceExtension__TargetDeviceObject;
int __tmp_55 = Irp;
int DeviceObject = __tmp_54;
int Irp = __tmp_55;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4948;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4948;
}
else 
{
returnVal2 = 259;
label_4948:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5086;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5062;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5062:; 
goto label_5086;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5086:; 
 __return_5087 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_5092 = returnVal2;
}
status4 = __return_5087;
goto label_5094;
status4 = __return_5092;
label_5094:; 
__cil_tmp15 = (long)status4;
if (__cil_tmp15 == 259L)
{
{
int __tmp_56 = event;
int __tmp_57 = Executive;
int __tmp_58 = KernelMode;
int __tmp_59 = 0;
int __tmp_60 = 0;
int Object = __tmp_56;
int WaitReason = __tmp_57;
int WaitMode = __tmp_58;
int Alertable = __tmp_59;
int Timeout = __tmp_60;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_5960;
}
else 
{
goto label_5937;
}
}
else 
{
label_5937:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_5960;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_5979 = 0;
goto label_5980;
}
else 
{
 __return_5980 = -1073741823;
label_5980:; 
}
goto label_5984;
}
else 
{
label_5960:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_5981 = 0;
goto label_5982;
}
else 
{
 __return_5982 = -1073741823;
label_5982:; 
}
label_5984:; 
status4 = myStatus;
 __return_5989 = status4;
}
status2 = __return_5989;
goto label_5992;
}
}
}
}
else 
{
 __return_5990 = status4;
}
status2 = __return_5990;
label_5992:; 
{
int __tmp_61 = DeviceObject;
int __tmp_62 = deviceExtension__TargetDeviceObject;
int FilterDevice = __tmp_61;
int TargetDevice = __tmp_62;
int FilterDevice__Flags ;
int TargetDevice__Characteristics ;
int FilterDevice__Characteristics ;
int propFlags ;
}
{
int __tmp_63 = DeviceObject;
int DeviceObject = __tmp_63;
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
int __tmp_64 = __cil_tmp22;
int __tmp_65 = deviceExtension__TargetDeviceObject;
int __tmp_66 = 0;
int __tmp_67 = 0;
int __tmp_68 = number;
int __tmp_69 = sizeof__number;
int __tmp_70 = 0;
int __tmp_71 = event;
int __tmp_72 = ioStatus;
int IoControlCode = __tmp_64;
int DeviceObject = __tmp_65;
int InputBuffer = __tmp_66;
int InputBufferLength = __tmp_67;
int OutputBuffer = __tmp_68;
int OutputBufferLength = __tmp_69;
int InternalDeviceIoControl = __tmp_70;
int Event = __tmp_71;
int IoStatusBlock = __tmp_72;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6079 = malloc_ret;
goto label_6080;
}
else 
{
 __return_6080 = 0;
label_6080:; 
}
irp = __return_6080;
if (irp == 0)
{
 __return_8694 = -1073741670;
}
else 
{
{
int __tmp_75 = deviceExtension__TargetDeviceObject;
int __tmp_76 = irp;
int DeviceObject = __tmp_75;
int Irp = __tmp_76;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_6338;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6338;
}
else 
{
returnVal2 = 259;
label_6338:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6476;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6452;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6452:; 
goto label_6476;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6476:; 
 __return_6477 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_6482 = returnVal2;
}
status6 = __return_6477;
goto label_6484;
status6 = __return_6482;
label_6484:; 
__cil_tmp23 = (long)status6;
if (__cil_tmp23 == 259L)
{
{
int __tmp_77 = event;
int __tmp_78 = Executive;
int __tmp_79 = KernelMode;
int __tmp_80 = 0;
int __tmp_81 = 0;
int Object = __tmp_77;
int WaitReason = __tmp_78;
int WaitMode = __tmp_79;
int Alertable = __tmp_80;
int Timeout = __tmp_81;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_6530;
}
else 
{
goto label_6507;
}
}
else 
{
label_6507:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_6530;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_6549 = 0;
goto label_6550;
}
else 
{
 __return_6550 = -1073741823;
label_6550:; 
}
goto label_6554;
}
else 
{
label_6530:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_6551 = 0;
goto label_6552;
}
else 
{
 __return_6552 = -1073741823;
label_6552:; 
}
label_6554:; 
status6 = ioStatus__Status;
if (status6 < 0)
{
outputSize = sizeof__MOUNTDEV_NAME;
if (output == 0)
{
 __return_8692 = -1073741670;
goto label_8693;
}
else 
{
__cil_tmp24 = 8;
__cil_tmp25 = 5046272;
__cil_tmp26 = 5046280;
{
int __tmp_82 = __cil_tmp26;
int __tmp_83 = deviceExtension__TargetDeviceObject;
int __tmp_84 = 0;
int __tmp_85 = 0;
int __tmp_86 = output;
int __tmp_87 = outputSize;
int __tmp_88 = 0;
int __tmp_89 = event;
int __tmp_90 = ioStatus;
int IoControlCode = __tmp_82;
int DeviceObject = __tmp_83;
int InputBuffer = __tmp_84;
int InputBufferLength = __tmp_85;
int OutputBuffer = __tmp_86;
int OutputBufferLength = __tmp_87;
int InternalDeviceIoControl = __tmp_88;
int Event = __tmp_89;
int IoStatusBlock = __tmp_90;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6592 = malloc_ret;
goto label_6593;
}
else 
{
 __return_6593 = 0;
label_6593:; 
}
irp = __return_6593;
label_6595:; 
if (irp == 0)
{
 __return_8689 = -1073741670;
}
else 
{
{
int __tmp_91 = deviceExtension__TargetDeviceObject;
int __tmp_92 = irp;
int DeviceObject = __tmp_91;
int Irp = __tmp_92;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_6870;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6870;
}
else 
{
returnVal2 = 259;
label_6870:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7008;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6984;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6984:; 
goto label_7008;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7008:; 
 __return_7009 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_7014 = returnVal2;
}
status6 = __return_7009;
goto label_7016;
status6 = __return_7014;
label_7016:; 
__cil_tmp27 = (long)status6;
if (__cil_tmp27 == 259L)
{
{
int __tmp_93 = event;
int __tmp_94 = Executive;
int __tmp_95 = KernelMode;
int __tmp_96 = 0;
int __tmp_97 = 0;
int Object = __tmp_93;
int WaitReason = __tmp_94;
int WaitMode = __tmp_95;
int Alertable = __tmp_96;
int Timeout = __tmp_97;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_7062;
}
else 
{
goto label_7039;
}
}
else 
{
label_7039:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_7062;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_7081 = 0;
goto label_7082;
}
else 
{
 __return_7082 = -1073741823;
label_7082:; 
}
goto label_7086;
}
else 
{
label_7062:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_7083 = 0;
goto label_7084;
}
else 
{
 __return_7084 = -1073741823;
label_7084:; 
}
label_7086:; 
status6 = ioStatus__Status;
__cil_tmp28 = (unsigned long)status6;
if (__cil_tmp28 == -2147483643)
{
outputSize = sizeof__MOUNTDEV_NAME + output__NameLength;
if (output == 0)
{
 __return_8685 = -1073741670;
goto label_8686;
}
else 
{
__cil_tmp29 = 8;
__cil_tmp30 = 5046272;
__cil_tmp31 = 5046280;
{
int __tmp_98 = __cil_tmp31;
int __tmp_99 = deviceExtension__TargetDeviceObject;
int __tmp_100 = 0;
int __tmp_101 = 0;
int __tmp_102 = output;
int __tmp_103 = outputSize;
int __tmp_104 = 0;
int __tmp_105 = event;
int __tmp_106 = ioStatus;
int IoControlCode = __tmp_98;
int DeviceObject = __tmp_99;
int InputBuffer = __tmp_100;
int InputBufferLength = __tmp_101;
int OutputBuffer = __tmp_102;
int OutputBufferLength = __tmp_103;
int InternalDeviceIoControl = __tmp_104;
int Event = __tmp_105;
int IoStatusBlock = __tmp_106;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7143 = malloc_ret;
goto label_7144;
}
else 
{
 __return_7144 = 0;
label_7144:; 
}
irp = __return_7144;
goto label_7127;
}
}
}
else 
{
if (status6 < 0)
{
 __return_8686 = status6;
label_8686:; 
}
else 
{
__cil_tmp34 = 28;
__cil_tmp35 = 5636096;
__cil_tmp36 = 5636124;
{
int __tmp_107 = __cil_tmp36;
int __tmp_108 = deviceExtension__TargetDeviceObject;
int __tmp_109 = 0;
int __tmp_110 = 0;
int __tmp_111 = volumeNumber;
int __tmp_112 = sizeof__VOLUME_NUMBER;
int __tmp_113 = 0;
int __tmp_114 = event;
int __tmp_115 = ioStatus;
int IoControlCode = __tmp_107;
int DeviceObject = __tmp_108;
int InputBuffer = __tmp_109;
int InputBufferLength = __tmp_110;
int OutputBuffer = __tmp_111;
int OutputBufferLength = __tmp_112;
int InternalDeviceIoControl = __tmp_113;
int Event = __tmp_114;
int IoStatusBlock = __tmp_115;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7697 = malloc_ret;
goto label_7698;
}
else 
{
 __return_7698 = 0;
label_7698:; 
}
irp = __return_7698;
label_7700:; 
if (irp == 0)
{
 __return_8679 = -1073741670;
}
else 
{
{
int __tmp_116 = deviceExtension__TargetDeviceObject;
int __tmp_117 = irp;
int DeviceObject = __tmp_116;
int Irp = __tmp_117;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_8387;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8387;
}
else 
{
returnVal2 = 259;
label_8387:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8525;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8501;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8501:; 
goto label_8525;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8525:; 
 __return_8526 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_8531 = returnVal2;
}
status6 = __return_8526;
goto label_8533;
status6 = __return_8531;
label_8533:; 
__cil_tmp37 = (long)status6;
if (__cil_tmp37 == 259L)
{
{
int __tmp_118 = event;
int __tmp_119 = Executive;
int __tmp_120 = KernelMode;
int __tmp_121 = 0;
int __tmp_122 = 0;
int Object = __tmp_118;
int WaitReason = __tmp_119;
int WaitMode = __tmp_120;
int Alertable = __tmp_121;
int Timeout = __tmp_122;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_8582;
}
else 
{
goto label_8559;
}
}
else 
{
label_8559:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_8582;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_8601 = 0;
goto label_8602;
}
else 
{
 __return_8602 = -1073741823;
label_8602:; 
}
goto label_8606;
}
else 
{
label_8582:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_8603 = 0;
goto label_8604;
}
else 
{
 __return_8604 = -1073741823;
label_8604:; 
}
label_8606:; 
status6 = ioStatus__Status;
 __return_8675 = status6;
}
goto label_8696;
}
}
}
}
else 
{
 __return_8674 = status6;
}
goto label_8696;
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_123 = DeviceObject;
int __tmp_124 = Irp;
int __tmp_125 = lcontext;
int DeviceObject = __tmp_123;
int Irp = __tmp_124;
int Context = __tmp_125;
int Event ;
Event = Context;
{
int __tmp_126 = Event;
int __tmp_127 = 0;
int __tmp_128 = 0;
int Event = __tmp_126;
int Increment = __tmp_127;
int Wait = __tmp_128;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_8240 = l;
}
 __return_8243 = -1073741802;
}
compRetStatus = __return_8243;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_8274;
label_8274:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_8381;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8381;
}
else 
{
returnVal2 = 259;
label_8381:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8519;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8495;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8495:; 
goto label_8519;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8519:; 
 __return_8529 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_8427:; 
 __return_8530 = returnVal2;
}
status6 = __return_8529;
goto label_8533;
status6 = __return_8530;
goto label_8533;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_8385;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8385;
}
else 
{
returnVal2 = 259;
label_8385:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8523;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8499;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8499:; 
goto label_8523;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8523:; 
 __return_8527 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_8427;
}
status6 = __return_8527;
goto label_8533;
}
}
}
}
}
}
else 
{
{
int __tmp_129 = DeviceObject;
int __tmp_130 = Irp;
int __tmp_131 = lcontext;
int DeviceObject = __tmp_129;
int Irp = __tmp_130;
int Context = __tmp_131;
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
 __return_8222 = 0;
goto label_8223;
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
goto label_8195;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_8195:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_8203;
}
else 
{
label_8203:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_8219;
}
else 
{
{
__VERIFIER_error();
}
 __return_8221 = 0;
}
compRetStatus = __return_8221;
goto label_8225;
}
else 
{
label_8219:; 
 __return_8223 = 0;
label_8223:; 
}
compRetStatus = __return_8223;
label_8225:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_8274;
goto label_8274;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_8383;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8383;
}
else 
{
returnVal2 = 259;
label_8383:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8521;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8497;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8497:; 
goto label_8521;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8521:; 
 __return_8528 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_8427;
}
status6 = __return_8528;
goto label_8533;
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
goto label_8696;
}
}
goto label_8696;
}
}
}
}
}
}
else 
{
__cil_tmp28 = (unsigned long)status6;
if (__cil_tmp28 == -2147483643)
{
outputSize = sizeof__MOUNTDEV_NAME + output__NameLength;
if (output == 0)
{
 __return_8687 = -1073741670;
goto label_8688;
}
else 
{
__cil_tmp29 = 8;
__cil_tmp30 = 5046272;
__cil_tmp31 = 5046280;
{
int __tmp_132 = __cil_tmp31;
int __tmp_133 = deviceExtension__TargetDeviceObject;
int __tmp_134 = 0;
int __tmp_135 = 0;
int __tmp_136 = output;
int __tmp_137 = outputSize;
int __tmp_138 = 0;
int __tmp_139 = event;
int __tmp_140 = ioStatus;
int IoControlCode = __tmp_132;
int DeviceObject = __tmp_133;
int InputBuffer = __tmp_134;
int InputBufferLength = __tmp_135;
int OutputBuffer = __tmp_136;
int OutputBufferLength = __tmp_137;
int InternalDeviceIoControl = __tmp_138;
int Event = __tmp_139;
int IoStatusBlock = __tmp_140;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7124 = malloc_ret;
goto label_7125;
}
else 
{
 __return_7125 = 0;
label_7125:; 
}
irp = __return_7125;
label_7127:; 
if (irp == 0)
{
 __return_8684 = -1073741670;
}
else 
{
{
int __tmp_141 = deviceExtension__TargetDeviceObject;
int __tmp_142 = irp;
int DeviceObject = __tmp_141;
int Irp = __tmp_142;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7402;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7402;
}
else 
{
returnVal2 = 259;
label_7402:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7540;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7516;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7516:; 
goto label_7540;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7540:; 
 __return_7541 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_7546 = returnVal2;
}
status6 = __return_7541;
goto label_7548;
status6 = __return_7546;
label_7548:; 
__cil_tmp32 = (long)status6;
if (__cil_tmp32 == 259L)
{
{
int __tmp_143 = event;
int __tmp_144 = Executive;
int __tmp_145 = KernelMode;
int __tmp_146 = 0;
int __tmp_147 = 0;
int Object = __tmp_143;
int WaitReason = __tmp_144;
int WaitMode = __tmp_145;
int Alertable = __tmp_146;
int Timeout = __tmp_147;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_7595;
}
else 
{
goto label_7572;
}
}
else 
{
label_7572:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_7595;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_7614 = 0;
goto label_7615;
}
else 
{
 __return_7615 = -1073741823;
label_7615:; 
}
goto label_7619;
}
else 
{
label_7595:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_7616 = 0;
goto label_7617;
}
else 
{
 __return_7617 = -1073741823;
label_7617:; 
}
label_7619:; 
status6 = ioStatus__Status;
if (status6 < 0)
{
 __return_8683 = status6;
}
else 
{
__cil_tmp34 = 28;
__cil_tmp35 = 5636096;
__cil_tmp36 = 5636124;
{
int __tmp_148 = __cil_tmp36;
int __tmp_149 = deviceExtension__TargetDeviceObject;
int __tmp_150 = 0;
int __tmp_151 = 0;
int __tmp_152 = volumeNumber;
int __tmp_153 = sizeof__VOLUME_NUMBER;
int __tmp_154 = 0;
int __tmp_155 = event;
int __tmp_156 = ioStatus;
int IoControlCode = __tmp_148;
int DeviceObject = __tmp_149;
int InputBuffer = __tmp_150;
int InputBufferLength = __tmp_151;
int OutputBuffer = __tmp_152;
int OutputBufferLength = __tmp_153;
int InternalDeviceIoControl = __tmp_154;
int Event = __tmp_155;
int IoStatusBlock = __tmp_156;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7659 = malloc_ret;
goto label_7660;
}
else 
{
 __return_7660 = 0;
label_7660:; 
}
irp = __return_7660;
label_7662:; 
if (irp == 0)
{
 __return_8678 = -1073741670;
}
else 
{
{
int __tmp_157 = deviceExtension__TargetDeviceObject;
int __tmp_158 = irp;
int DeviceObject = __tmp_157;
int Irp = __tmp_158;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7977;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7977;
}
else 
{
returnVal2 = 259;
label_7977:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8115;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8091;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8091:; 
goto label_8115;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8115:; 
 __return_8116 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_8121 = returnVal2;
}
status6 = __return_8116;
goto label_8123;
status6 = __return_8121;
label_8123:; 
__cil_tmp37 = (long)status6;
if (__cil_tmp37 == 259L)
{
{
int __tmp_159 = event;
int __tmp_160 = Executive;
int __tmp_161 = KernelMode;
int __tmp_162 = 0;
int __tmp_163 = 0;
int Object = __tmp_159;
int WaitReason = __tmp_160;
int WaitMode = __tmp_161;
int Alertable = __tmp_162;
int Timeout = __tmp_163;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_8641;
}
else 
{
goto label_8618;
}
}
else 
{
label_8618:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_8641;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_8660 = 0;
goto label_8661;
}
else 
{
 __return_8661 = -1073741823;
label_8661:; 
}
goto label_8606;
}
else 
{
label_8641:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_8662 = 0;
goto label_8663;
}
else 
{
 __return_8663 = -1073741823;
label_8663:; 
}
goto label_8606;
}
}
}
}
}
else 
{
 __return_8673 = status6;
}
goto label_8696;
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_164 = DeviceObject;
int __tmp_165 = Irp;
int __tmp_166 = lcontext;
int DeviceObject = __tmp_164;
int Irp = __tmp_165;
int Context = __tmp_166;
int Event ;
Event = Context;
{
int __tmp_167 = Event;
int __tmp_168 = 0;
int __tmp_169 = 0;
int Event = __tmp_167;
int Increment = __tmp_168;
int Wait = __tmp_169;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_7830 = l;
}
 __return_7833 = -1073741802;
}
compRetStatus = __return_7833;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_7864;
label_7864:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7971;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7971;
}
else 
{
returnVal2 = 259;
label_7971:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8109;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8085;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8085:; 
goto label_8109;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8109:; 
 __return_8119 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_8017:; 
 __return_8120 = returnVal2;
}
status6 = __return_8119;
goto label_8123;
status6 = __return_8120;
goto label_8123;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7975;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7975;
}
else 
{
returnVal2 = 259;
label_7975:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8113;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8089;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8089:; 
goto label_8113;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8113:; 
 __return_8117 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_8017;
}
status6 = __return_8117;
goto label_8123;
}
}
}
}
}
}
else 
{
{
int __tmp_170 = DeviceObject;
int __tmp_171 = Irp;
int __tmp_172 = lcontext;
int DeviceObject = __tmp_170;
int Irp = __tmp_171;
int Context = __tmp_172;
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
 __return_7812 = 0;
goto label_7813;
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
goto label_7785;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_7785:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_7793;
}
else 
{
label_7793:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_7809;
}
else 
{
{
__VERIFIER_error();
}
 __return_7811 = 0;
}
compRetStatus = __return_7811;
goto label_7815;
}
else 
{
label_7809:; 
 __return_7813 = 0;
label_7813:; 
}
compRetStatus = __return_7813;
label_7815:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_7864;
goto label_7864;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7973;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7973;
}
else 
{
returnVal2 = 259;
label_7973:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8111;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8087;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8087:; 
goto label_8111;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8111:; 
 __return_8118 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_8017;
}
status6 = __return_8118;
goto label_8123;
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
goto label_8696;
}
}
goto label_8696;
}
}
}
}
}
else 
{
if (status6 < 0)
{
 __return_8682 = status6;
}
else 
{
__cil_tmp34 = 28;
__cil_tmp35 = 5636096;
__cil_tmp36 = 5636124;
{
int __tmp_173 = __cil_tmp36;
int __tmp_174 = deviceExtension__TargetDeviceObject;
int __tmp_175 = 0;
int __tmp_176 = 0;
int __tmp_177 = volumeNumber;
int __tmp_178 = sizeof__VOLUME_NUMBER;
int __tmp_179 = 0;
int __tmp_180 = event;
int __tmp_181 = ioStatus;
int IoControlCode = __tmp_173;
int DeviceObject = __tmp_174;
int InputBuffer = __tmp_175;
int InputBufferLength = __tmp_176;
int OutputBuffer = __tmp_177;
int OutputBufferLength = __tmp_178;
int InternalDeviceIoControl = __tmp_179;
int Event = __tmp_180;
int IoStatusBlock = __tmp_181;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7678 = malloc_ret;
goto label_7679;
}
else 
{
 __return_7679 = 0;
label_7679:; 
}
irp = __return_7679;
goto label_7662;
}
}
goto label_8696;
}
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_182 = DeviceObject;
int __tmp_183 = Irp;
int __tmp_184 = lcontext;
int DeviceObject = __tmp_182;
int Irp = __tmp_183;
int Context = __tmp_184;
int Event ;
Event = Context;
{
int __tmp_185 = Event;
int __tmp_186 = 0;
int __tmp_187 = 0;
int Event = __tmp_185;
int Increment = __tmp_186;
int Wait = __tmp_187;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_7255 = l;
}
 __return_7258 = -1073741802;
}
compRetStatus = __return_7258;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_7289;
label_7289:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7396;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7396;
}
else 
{
returnVal2 = 259;
label_7396:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7534;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7510;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7510:; 
goto label_7534;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7534:; 
 __return_7544 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_7442:; 
 __return_7545 = returnVal2;
}
status6 = __return_7544;
goto label_7548;
status6 = __return_7545;
goto label_7548;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7400;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7400;
}
else 
{
returnVal2 = 259;
label_7400:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7538;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7514;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7514:; 
goto label_7538;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7538:; 
 __return_7542 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_7442;
}
status6 = __return_7542;
goto label_7548;
}
}
}
}
}
}
else 
{
{
int __tmp_188 = DeviceObject;
int __tmp_189 = Irp;
int __tmp_190 = lcontext;
int DeviceObject = __tmp_188;
int Irp = __tmp_189;
int Context = __tmp_190;
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
 __return_7237 = 0;
goto label_7238;
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
goto label_7210;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_7210:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_7218;
}
else 
{
label_7218:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_7234;
}
else 
{
{
__VERIFIER_error();
}
 __return_7236 = 0;
}
compRetStatus = __return_7236;
goto label_7240;
}
else 
{
label_7234:; 
 __return_7238 = 0;
label_7238:; 
}
compRetStatus = __return_7238;
label_7240:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_7289;
goto label_7289;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7398;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7398;
}
else 
{
returnVal2 = 259;
label_7398:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7536;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7512;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7512:; 
goto label_7536;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7536:; 
 __return_7543 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_7442;
}
status6 = __return_7543;
goto label_7548;
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
goto label_8696;
}
}
}
else 
{
if (status6 < 0)
{
 __return_8688 = status6;
label_8688:; 
}
else 
{
__cil_tmp34 = 28;
__cil_tmp35 = 5636096;
__cil_tmp36 = 5636124;
{
int __tmp_191 = __cil_tmp36;
int __tmp_192 = deviceExtension__TargetDeviceObject;
int __tmp_193 = 0;
int __tmp_194 = 0;
int __tmp_195 = volumeNumber;
int __tmp_196 = sizeof__VOLUME_NUMBER;
int __tmp_197 = 0;
int __tmp_198 = event;
int __tmp_199 = ioStatus;
int IoControlCode = __tmp_191;
int DeviceObject = __tmp_192;
int InputBuffer = __tmp_193;
int InputBufferLength = __tmp_194;
int OutputBuffer = __tmp_195;
int OutputBufferLength = __tmp_196;
int InternalDeviceIoControl = __tmp_197;
int Event = __tmp_198;
int IoStatusBlock = __tmp_199;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7716 = malloc_ret;
goto label_7717;
}
else 
{
 __return_7717 = 0;
label_7717:; 
}
irp = __return_7717;
goto label_7700;
}
}
goto label_8696;
}
}
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_200 = DeviceObject;
int __tmp_201 = Irp;
int __tmp_202 = lcontext;
int DeviceObject = __tmp_200;
int Irp = __tmp_201;
int Context = __tmp_202;
int Event ;
Event = Context;
{
int __tmp_203 = Event;
int __tmp_204 = 0;
int __tmp_205 = 0;
int Event = __tmp_203;
int Increment = __tmp_204;
int Wait = __tmp_205;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_6723 = l;
}
 __return_6726 = -1073741802;
}
compRetStatus = __return_6726;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_6757;
label_6757:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_6864;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6864;
}
else 
{
returnVal2 = 259;
label_6864:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7002;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6978;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6978:; 
goto label_7002;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7002:; 
 __return_7012 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_6910:; 
 __return_7013 = returnVal2;
}
status6 = __return_7012;
goto label_7016;
status6 = __return_7013;
goto label_7016;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_6868;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6868;
}
else 
{
returnVal2 = 259;
label_6868:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7006;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6982;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6982:; 
goto label_7006;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7006:; 
 __return_7010 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_6910;
}
status6 = __return_7010;
goto label_7016;
}
}
}
}
}
}
else 
{
{
int __tmp_206 = DeviceObject;
int __tmp_207 = Irp;
int __tmp_208 = lcontext;
int DeviceObject = __tmp_206;
int Irp = __tmp_207;
int Context = __tmp_208;
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
 __return_6705 = 0;
goto label_6706;
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
goto label_6678;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_6678:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_6686;
}
else 
{
label_6686:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_6702;
}
else 
{
{
__VERIFIER_error();
}
 __return_6704 = 0;
}
compRetStatus = __return_6704;
goto label_6708;
}
else 
{
label_6702:; 
 __return_6706 = 0;
label_6706:; 
}
compRetStatus = __return_6706;
label_6708:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_6757;
goto label_6757;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_6866;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6866;
}
else 
{
returnVal2 = 259;
label_6866:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7004;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6980;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6980:; 
goto label_7004;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7004:; 
 __return_7011 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_6910;
}
status6 = __return_7011;
goto label_7016;
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
goto label_8696;
}
}
}
else 
{
 __return_8693 = status6;
label_8693:; 
}
goto label_8696;
}
}
}
}
}
else 
{
if (status6 < 0)
{
outputSize = sizeof__MOUNTDEV_NAME;
if (output == 0)
{
 __return_8690 = -1073741670;
goto label_8691;
}
else 
{
__cil_tmp24 = 8;
__cil_tmp25 = 5046272;
__cil_tmp26 = 5046280;
{
int __tmp_209 = __cil_tmp26;
int __tmp_210 = deviceExtension__TargetDeviceObject;
int __tmp_211 = 0;
int __tmp_212 = 0;
int __tmp_213 = output;
int __tmp_214 = outputSize;
int __tmp_215 = 0;
int __tmp_216 = event;
int __tmp_217 = ioStatus;
int IoControlCode = __tmp_209;
int DeviceObject = __tmp_210;
int InputBuffer = __tmp_211;
int InputBufferLength = __tmp_212;
int OutputBuffer = __tmp_213;
int OutputBufferLength = __tmp_214;
int InternalDeviceIoControl = __tmp_215;
int Event = __tmp_216;
int IoStatusBlock = __tmp_217;
int malloc_ret = __VERIFIER_nondet_int() ;
malloc_ret = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6611 = malloc_ret;
goto label_6612;
}
else 
{
 __return_6612 = 0;
label_6612:; 
}
irp = __return_6612;
goto label_6595;
}
}
}
else 
{
 __return_8691 = status6;
label_8691:; 
}
goto label_8696;
}
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_218 = DeviceObject;
int __tmp_219 = Irp;
int __tmp_220 = lcontext;
int DeviceObject = __tmp_218;
int Irp = __tmp_219;
int Context = __tmp_220;
int Event ;
Event = Context;
{
int __tmp_221 = Event;
int __tmp_222 = 0;
int __tmp_223 = 0;
int Event = __tmp_221;
int Increment = __tmp_222;
int Wait = __tmp_223;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_6191 = l;
}
 __return_6194 = -1073741802;
}
compRetStatus = __return_6194;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_6225;
label_6225:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_6332;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6332;
}
else 
{
returnVal2 = 259;
label_6332:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6470;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6446;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6446:; 
goto label_6470;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6470:; 
 __return_6480 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_6378:; 
 __return_6481 = returnVal2;
}
status6 = __return_6480;
goto label_6484;
status6 = __return_6481;
goto label_6484;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_6336;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6336;
}
else 
{
returnVal2 = 259;
label_6336:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6474;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6450;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6450:; 
goto label_6474;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6474:; 
 __return_6478 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_6378;
}
status6 = __return_6478;
goto label_6484;
}
}
}
}
}
}
else 
{
{
int __tmp_224 = DeviceObject;
int __tmp_225 = Irp;
int __tmp_226 = lcontext;
int DeviceObject = __tmp_224;
int Irp = __tmp_225;
int Context = __tmp_226;
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
 __return_6173 = 0;
goto label_6174;
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
goto label_6146;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_6146:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_6154;
}
else 
{
label_6154:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_6170;
}
else 
{
{
__VERIFIER_error();
}
 __return_6172 = 0;
}
compRetStatus = __return_6172;
goto label_6176;
}
else 
{
label_6170:; 
 __return_6174 = 0;
label_6174:; 
}
compRetStatus = __return_6174;
label_6176:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_6225;
goto label_6225;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_6334;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6334;
}
else 
{
returnVal2 = 259;
label_6334:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6472;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6448;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6448:; 
goto label_6472;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6472:; 
 __return_6479 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_6378;
}
status6 = __return_6479;
goto label_6484;
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
label_8696:; 
Irp__IoStatus__Status = status2;
myStatus = status2;
{
int __tmp_73 = Irp;
int __tmp_74 = 0;
int Irp = __tmp_73;
int PriorityBoost = __tmp_74;
if (s == NP)
{
s = DC;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_8744;
label_8744:; 
 __return_8747 = status2;
}
status1 = __return_8747;
 __return_8755 = status1;
}
status7 = __return_8755;
goto label_8758;
}
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_227 = DeviceObject;
int __tmp_228 = Irp;
int __tmp_229 = lcontext;
int DeviceObject = __tmp_227;
int Irp = __tmp_228;
int Context = __tmp_229;
int Event ;
Event = Context;
{
int __tmp_230 = Event;
int __tmp_231 = 0;
int __tmp_232 = 0;
int Event = __tmp_230;
int Increment = __tmp_231;
int Wait = __tmp_232;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_4801 = l;
}
 __return_4804 = -1073741802;
}
compRetStatus = __return_4804;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_4835;
label_4835:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4942;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4942;
}
else 
{
returnVal2 = 259;
label_4942:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5080;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5056;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5056:; 
goto label_5080;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5080:; 
 __return_5090 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4988:; 
 __return_5091 = returnVal2;
}
status4 = __return_5090;
goto label_5094;
status4 = __return_5091;
goto label_5094;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4946;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4946;
}
else 
{
returnVal2 = 259;
label_4946:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5084;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5060;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5060:; 
goto label_5084;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5084:; 
 __return_5088 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4988;
}
status4 = __return_5088;
goto label_5094;
}
}
}
}
}
}
else 
{
{
int __tmp_233 = DeviceObject;
int __tmp_234 = Irp;
int __tmp_235 = lcontext;
int DeviceObject = __tmp_233;
int Irp = __tmp_234;
int Context = __tmp_235;
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
 __return_4783 = 0;
goto label_4784;
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
goto label_4756;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_4756:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_4764;
}
else 
{
label_4764:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_4780;
}
else 
{
{
__VERIFIER_error();
}
 __return_4782 = 0;
}
compRetStatus = __return_4782;
goto label_4786;
}
else 
{
label_4780:; 
 __return_4784 = 0;
label_4784:; 
}
compRetStatus = __return_4784;
label_4786:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_4835;
goto label_4835;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4944;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4944;
}
else 
{
returnVal2 = 259;
label_4944:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5082;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5058;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5058:; 
goto label_5082;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5082:; 
 __return_5089 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4988;
}
status4 = __return_5089;
goto label_5094;
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
int __tmp_236 = DeviceObject;
int __tmp_237 = Irp;
int DeviceObject = __tmp_236;
int Irp = __tmp_237;
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
int __tmp_238 = DeviceObject;
int __tmp_239 = Irp;
int DeviceObject = __tmp_238;
int Irp = __tmp_239;
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
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_240 = deviceExtension__TargetDeviceObject;
int __tmp_241 = Irp;
int DeviceObject = __tmp_240;
int Irp = __tmp_241;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4369;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4369;
}
else 
{
returnVal2 = 259;
label_4369:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4507;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4483;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4483:; 
goto label_4507;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4507:; 
 __return_4508 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_4513 = returnVal2;
}
status4 = __return_4508;
goto label_3695;
status4 = __return_4513;
goto label_3695;
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_242 = DeviceObject;
int __tmp_243 = Irp;
int __tmp_244 = lcontext;
int DeviceObject = __tmp_242;
int Irp = __tmp_243;
int Context = __tmp_244;
int Event ;
Event = Context;
{
int __tmp_245 = Event;
int __tmp_246 = 0;
int __tmp_247 = 0;
int Event = __tmp_245;
int Increment = __tmp_246;
int Wait = __tmp_247;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_4222 = l;
}
 __return_4225 = -1073741802;
}
compRetStatus = __return_4225;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_4256;
label_4256:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4363;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4363;
}
else 
{
returnVal2 = 259;
label_4363:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4501;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4477;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4477:; 
goto label_4501;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4501:; 
 __return_4511 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4409:; 
 __return_4512 = returnVal2;
}
status4 = __return_4511;
goto label_3695;
status4 = __return_4512;
goto label_3695;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4367;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4367;
}
else 
{
returnVal2 = 259;
label_4367:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4505;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4481;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4481:; 
goto label_4505;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4505:; 
 __return_4509 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4409;
}
status4 = __return_4509;
goto label_3695;
}
}
}
}
}
}
else 
{
{
int __tmp_248 = DeviceObject;
int __tmp_249 = Irp;
int __tmp_250 = lcontext;
int DeviceObject = __tmp_248;
int Irp = __tmp_249;
int Context = __tmp_250;
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
 __return_4204 = 0;
goto label_4205;
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
goto label_4177;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_4177:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_4185;
}
else 
{
label_4185:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_4201;
}
else 
{
{
__VERIFIER_error();
}
 __return_4203 = 0;
}
compRetStatus = __return_4203;
goto label_4207;
}
else 
{
label_4201:; 
 __return_4205 = 0;
label_4205:; 
}
compRetStatus = __return_4205;
label_4207:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_4256;
goto label_4256;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4365;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4365;
}
else 
{
returnVal2 = 259;
label_4365:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4503;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4479;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4479:; 
goto label_4503;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4503:; 
 __return_4510 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4409;
}
status4 = __return_4510;
goto label_3695;
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
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_251 = deviceExtension__TargetDeviceObject;
int __tmp_252 = Irp;
int DeviceObject = __tmp_251;
int Irp = __tmp_252;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3959;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3959;
}
else 
{
returnVal2 = 259;
label_3959:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4097;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4073;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4073:; 
goto label_4097;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4097:; 
 __return_4098 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_4103 = returnVal2;
}
status4 = __return_4098;
goto label_3695;
status4 = __return_4103;
goto label_3695;
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_253 = DeviceObject;
int __tmp_254 = Irp;
int __tmp_255 = lcontext;
int DeviceObject = __tmp_253;
int Irp = __tmp_254;
int Context = __tmp_255;
int Event ;
Event = Context;
{
int __tmp_256 = Event;
int __tmp_257 = 0;
int __tmp_258 = 0;
int Event = __tmp_256;
int Increment = __tmp_257;
int Wait = __tmp_258;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_3812 = l;
}
 __return_3815 = -1073741802;
}
compRetStatus = __return_3815;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_3846;
label_3846:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3953;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3953;
}
else 
{
returnVal2 = 259;
label_3953:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4091;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4067;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4067:; 
goto label_4091;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4091:; 
 __return_4101 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3999:; 
 __return_4102 = returnVal2;
}
status4 = __return_4101;
goto label_3695;
status4 = __return_4102;
goto label_3695;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3957;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3957;
}
else 
{
returnVal2 = 259;
label_3957:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4095;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4071;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4071:; 
goto label_4095;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4095:; 
 __return_4099 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3999;
}
status4 = __return_4099;
goto label_3695;
}
}
}
}
}
}
else 
{
{
int __tmp_259 = DeviceObject;
int __tmp_260 = Irp;
int __tmp_261 = lcontext;
int DeviceObject = __tmp_259;
int Irp = __tmp_260;
int Context = __tmp_261;
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
 __return_3794 = 0;
goto label_3795;
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
goto label_3767;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_3767:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_3775;
}
else 
{
label_3775:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_3791;
}
else 
{
{
__VERIFIER_error();
}
 __return_3793 = 0;
}
compRetStatus = __return_3793;
goto label_3797;
}
else 
{
label_3791:; 
 __return_3795 = 0;
label_3795:; 
}
compRetStatus = __return_3795;
label_3797:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_3846;
goto label_3846;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3955;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3955;
}
else 
{
returnVal2 = 259;
label_3955:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4093;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4069;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4069:; 
goto label_4093;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4093:; 
 __return_4100 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3999;
}
status4 = __return_4100;
goto label_3695;
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
compRegistered = 1;
routine = 0;
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_262 = deviceExtension__TargetDeviceObject;
int __tmp_263 = Irp;
int DeviceObject = __tmp_262;
int Irp = __tmp_263;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3549;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3549;
}
else 
{
returnVal2 = 259;
label_3549:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3687;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3663;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3663:; 
goto label_3687;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3687:; 
 __return_3688 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_3693 = returnVal2;
}
status4 = __return_3688;
goto label_3695;
status4 = __return_3693;
label_3695:; 
__cil_tmp15 = (long)status4;
if (__cil_tmp15 == 259L)
{
{
int __tmp_264 = event;
int __tmp_265 = Executive;
int __tmp_266 = KernelMode;
int __tmp_267 = 0;
int __tmp_268 = 0;
int Object = __tmp_264;
int WaitReason = __tmp_265;
int WaitMode = __tmp_266;
int Alertable = __tmp_267;
int Timeout = __tmp_268;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_4561;
}
else 
{
goto label_4538;
}
}
else 
{
label_4538:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_4561;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_4580 = 0;
goto label_4581;
}
else 
{
 __return_4581 = -1073741823;
label_4581:; 
}
goto label_4585;
}
else 
{
label_4561:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_4582 = 0;
goto label_4583;
}
else 
{
 __return_4583 = -1073741823;
label_4583:; 
}
label_4585:; 
status4 = myStatus;
 __return_4590 = status4;
}
status3 = __return_4590;
goto label_4593;
}
}
}
}
else 
{
 __return_4591 = status4;
}
status3 = __return_4591;
label_4593:; 
Irp__IoStatus__Status = status3;
myStatus = status3;
{
int __tmp_269 = Irp;
int __tmp_270 = 0;
int Irp = __tmp_269;
int PriorityBoost = __tmp_270;
if (s == NP)
{
s = DC;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_4617;
label_4617:; 
 __return_4620 = status3;
}
status1 = __return_4620;
 __return_8756 = status1;
}
status7 = __return_8756;
label_8758:; 
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10033;
}
else 
{
goto label_9463;
}
}
else 
{
label_9463:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10033;
}
else 
{
goto label_9516;
}
}
else 
{
label_9516:; 
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
goto label_9934;
}
else 
{
goto label_9642;
}
}
else 
{
goto label_9642;
}
}
else 
{
label_9642:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_9860;
}
else 
{
goto label_10033;
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
goto label_9782;
}
else 
{
goto label_10033;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9710;
}
else 
{
goto label_10033;
}
}
}
}
}
else 
{
goto label_10033;
}
}
else 
{
label_10033:; 
 __return_10051 = status7;
goto label_1454;
}
}
}
}
else 
{
{
int __tmp_271 = d;
int DriverObject = __tmp_271;
}
goto label_9392;
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_272 = DeviceObject;
int __tmp_273 = Irp;
int __tmp_274 = lcontext;
int DeviceObject = __tmp_272;
int Irp = __tmp_273;
int Context = __tmp_274;
int Event ;
Event = Context;
{
int __tmp_275 = Event;
int __tmp_276 = 0;
int __tmp_277 = 0;
int Event = __tmp_275;
int Increment = __tmp_276;
int Wait = __tmp_277;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_3402 = l;
}
 __return_3405 = -1073741802;
}
compRetStatus = __return_3405;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_3436;
label_3436:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3543;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3543;
}
else 
{
returnVal2 = 259;
label_3543:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3681;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3657;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3657:; 
goto label_3681;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3681:; 
 __return_3691 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3589:; 
 __return_3692 = returnVal2;
}
status4 = __return_3691;
goto label_3695;
status4 = __return_3692;
goto label_3695;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3547;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3547;
}
else 
{
returnVal2 = 259;
label_3547:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3685;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3661;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3661:; 
goto label_3685;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3685:; 
 __return_3689 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3589;
}
status4 = __return_3689;
goto label_3695;
}
}
}
}
}
}
else 
{
{
int __tmp_278 = DeviceObject;
int __tmp_279 = Irp;
int __tmp_280 = lcontext;
int DeviceObject = __tmp_278;
int Irp = __tmp_279;
int Context = __tmp_280;
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
 __return_3384 = 0;
goto label_3385;
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
goto label_3357;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_3357:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_3365;
}
else 
{
label_3365:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_3381;
}
else 
{
{
__VERIFIER_error();
}
 __return_3383 = 0;
}
compRetStatus = __return_3383;
goto label_3387;
}
else 
{
label_3381:; 
 __return_3385 = 0;
label_3385:; 
}
compRetStatus = __return_3385;
label_3387:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_3436;
goto label_3436;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3545;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3545;
}
else 
{
returnVal2 = 259;
label_3545:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3683;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3659;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3659:; 
goto label_3683;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3683:; 
 __return_3690 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3589;
}
status4 = __return_3690;
goto label_3695;
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
{
int __tmp_281 = DeviceObject;
int __tmp_282 = Irp;
int DeviceObject = __tmp_281;
int Irp = __tmp_282;
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
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
deviceExtension = DeviceObject__DeviceExtension;
{
int __tmp_283 = deviceExtension__TargetDeviceObject;
int __tmp_284 = Irp;
int DeviceObject = __tmp_283;
int Irp = __tmp_284;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3062;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3062;
}
else 
{
returnVal2 = 259;
label_3062:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3200;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3176;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3176:; 
goto label_3200;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3200:; 
 __return_3201 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_3206 = returnVal2;
}
tmp = __return_3201;
goto label_2798;
tmp = __return_3206;
goto label_2798;
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_285 = DeviceObject;
int __tmp_286 = Irp;
int __tmp_287 = lcontext;
int DeviceObject = __tmp_285;
int Irp = __tmp_286;
int Context = __tmp_287;
int Event ;
Event = Context;
{
int __tmp_288 = Event;
int __tmp_289 = 0;
int __tmp_290 = 0;
int Event = __tmp_288;
int Increment = __tmp_289;
int Wait = __tmp_290;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2915 = l;
}
 __return_2918 = -1073741802;
}
compRetStatus = __return_2918;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_2949;
label_2949:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3056;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3056;
}
else 
{
returnVal2 = 259;
label_3056:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3194;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3170;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3170:; 
goto label_3194;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3194:; 
 __return_3204 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3102:; 
 __return_3205 = returnVal2;
}
tmp = __return_3204;
goto label_2798;
tmp = __return_3205;
goto label_2798;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3060;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3060;
}
else 
{
returnVal2 = 259;
label_3060:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3198;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3174;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3174:; 
goto label_3198;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3198:; 
 __return_3202 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3102;
}
tmp = __return_3202;
goto label_2798;
}
}
}
}
}
}
else 
{
{
int __tmp_291 = DeviceObject;
int __tmp_292 = Irp;
int __tmp_293 = lcontext;
int DeviceObject = __tmp_291;
int Irp = __tmp_292;
int Context = __tmp_293;
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
 __return_2897 = 0;
goto label_2898;
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
goto label_2870;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_2870:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_2878;
}
else 
{
label_2878:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_2894;
}
else 
{
{
__VERIFIER_error();
}
 __return_2896 = 0;
}
compRetStatus = __return_2896;
goto label_2900;
}
else 
{
label_2894:; 
 __return_2898 = 0;
label_2898:; 
}
compRetStatus = __return_2898;
label_2900:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_2949;
goto label_2949;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3058;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3058;
}
else 
{
returnVal2 = 259;
label_3058:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3196;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3172;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3172:; 
goto label_3196;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3196:; 
 __return_3203 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3102;
}
tmp = __return_3203;
goto label_2798;
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
{
__VERIFIER_error();
}
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
deviceExtension = DeviceObject__DeviceExtension;
{
int __tmp_294 = deviceExtension__TargetDeviceObject;
int __tmp_295 = Irp;
int DeviceObject = __tmp_294;
int Irp = __tmp_295;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_2652;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2652;
}
else 
{
returnVal2 = 259;
label_2652:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2790;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2766;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2766:; 
goto label_2790;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2790:; 
 __return_2791 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_2796 = returnVal2;
}
tmp = __return_2791;
goto label_2798;
tmp = __return_2796;
label_2798:; 
 __return_3219 = tmp;
}
tmp = __return_3219;
 __return_3222 = tmp;
}
status7 = __return_3222;
goto label_8758;
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_296 = DeviceObject;
int __tmp_297 = Irp;
int __tmp_298 = lcontext;
int DeviceObject = __tmp_296;
int Irp = __tmp_297;
int Context = __tmp_298;
int Event ;
Event = Context;
{
int __tmp_299 = Event;
int __tmp_300 = 0;
int __tmp_301 = 0;
int Event = __tmp_299;
int Increment = __tmp_300;
int Wait = __tmp_301;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2505 = l;
}
 __return_2508 = -1073741802;
}
compRetStatus = __return_2508;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_2539;
label_2539:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_2646;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2646;
}
else 
{
returnVal2 = 259;
label_2646:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2784;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2760;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2760:; 
goto label_2784;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2784:; 
 __return_2794 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2692:; 
 __return_2795 = returnVal2;
}
tmp = __return_2794;
goto label_2798;
tmp = __return_2795;
goto label_2798;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_2650;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2650;
}
else 
{
returnVal2 = 259;
label_2650:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2788;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2764;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2764:; 
goto label_2788;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2788:; 
 __return_2792 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2692;
}
tmp = __return_2792;
goto label_2798;
}
}
}
}
}
}
else 
{
{
int __tmp_302 = DeviceObject;
int __tmp_303 = Irp;
int __tmp_304 = lcontext;
int DeviceObject = __tmp_302;
int Irp = __tmp_303;
int Context = __tmp_304;
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
 __return_2487 = 0;
goto label_2488;
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
goto label_2460;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_2460:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_2468;
}
else 
{
label_2468:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_2484;
}
else 
{
{
__VERIFIER_error();
}
 __return_2486 = 0;
}
compRetStatus = __return_2486;
goto label_2490;
}
else 
{
label_2484:; 
 __return_2488 = 0;
label_2488:; 
}
compRetStatus = __return_2488;
label_2490:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_2539;
goto label_2539;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_2648;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2648;
}
else 
{
returnVal2 = 259;
label_2648:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2786;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2762;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2762:; 
goto label_2786;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2786:; 
 __return_2793 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2692;
}
tmp = __return_2793;
goto label_2798;
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
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 4)
{
{
int __tmp_305 = devobj;
int __tmp_306 = pirp;
int DeviceObject = __tmp_305;
int Irp = __tmp_306;
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
int __tmp_307 = deviceExtension__TargetDeviceObject;
int __tmp_308 = Irp;
int DeviceObject = __tmp_307;
int Irp = __tmp_308;
int compRetStatus ;
int returnVal ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
long __cil_tmp8 ;
if (compRegistered == 0)
{
int tmp_ndt_11;
tmp_ndt_11 = __VERIFIER_nondet_int();
if (tmp_ndt_11 == 0)
{
returnVal = 0;
goto label_2175;
}
else 
{
int tmp_ndt_12;
tmp_ndt_12 = __VERIFIER_nondet_int();
if (tmp_ndt_12 == 1)
{
returnVal = -1073741823;
goto label_2175;
}
else 
{
returnVal = 259;
label_2175:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_2317;
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
goto label_2287;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_2287:; 
goto label_2317;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_2317:; 
 __return_2318 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
 __return_2323 = returnVal;
}
tmp = __return_2318;
goto label_2325;
tmp = __return_2323;
label_2325:; 
 __return_2336 = tmp;
}
status7 = __return_2336;
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10035;
}
else 
{
goto label_9465;
}
}
else 
{
label_9465:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10035;
}
else 
{
goto label_9514;
}
}
else 
{
label_9514:; 
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
goto label_9934;
}
else 
{
goto label_9640;
}
}
else 
{
goto label_9640;
}
}
else 
{
label_9640:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_9860;
}
else 
{
goto label_10035;
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
goto label_9782;
}
else 
{
goto label_10035;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9710;
}
else 
{
goto label_10035;
}
}
}
}
}
else 
{
goto label_10035;
}
}
else 
{
label_10035:; 
 __return_10049 = status7;
goto label_1454;
}
}
}
}
else 
{
{
int __tmp_309 = d;
int DriverObject = __tmp_309;
}
goto label_9392;
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_310 = DeviceObject;
int __tmp_311 = Irp;
int __tmp_312 = lcontext;
int DeviceObject = __tmp_310;
int Irp = __tmp_311;
int Context = __tmp_312;
int Event ;
Event = Context;
{
int __tmp_313 = Event;
int __tmp_314 = 0;
int __tmp_315 = 0;
int Event = __tmp_313;
int Increment = __tmp_314;
int Wait = __tmp_315;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2028 = l;
}
 __return_2031 = -1073741802;
}
compRetStatus = __return_2031;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_2062;
label_2062:; 
int tmp_ndt_11;
tmp_ndt_11 = __VERIFIER_nondet_int();
if (tmp_ndt_11 == 0)
{
returnVal = 0;
goto label_2169;
}
else 
{
int tmp_ndt_12;
tmp_ndt_12 = __VERIFIER_nondet_int();
if (tmp_ndt_12 == 1)
{
returnVal = -1073741823;
goto label_2169;
}
else 
{
returnVal = 259;
label_2169:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_2311;
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
goto label_2293;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_2293:; 
goto label_2311;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_2311:; 
 __return_2321 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
label_2215:; 
 __return_2322 = returnVal;
}
tmp = __return_2321;
goto label_2325;
tmp = __return_2322;
goto label_2325;
}
}
}
}
}
}
else 
{
int tmp_ndt_11;
tmp_ndt_11 = __VERIFIER_nondet_int();
if (tmp_ndt_11 == 0)
{
returnVal = 0;
goto label_2173;
}
else 
{
int tmp_ndt_12;
tmp_ndt_12 = __VERIFIER_nondet_int();
if (tmp_ndt_12 == 1)
{
returnVal = -1073741823;
goto label_2173;
}
else 
{
returnVal = 259;
label_2173:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_2315;
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
goto label_2289;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_2289:; 
goto label_2315;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_2315:; 
 __return_2319 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_2215;
}
tmp = __return_2319;
goto label_2325;
}
}
}
}
}
}
else 
{
{
int __tmp_316 = DeviceObject;
int __tmp_317 = Irp;
int __tmp_318 = lcontext;
int DeviceObject = __tmp_316;
int Irp = __tmp_317;
int Context = __tmp_318;
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
 __return_2010 = 0;
goto label_2011;
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
goto label_1983;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_1983:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_1991;
}
else 
{
label_1991:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_2007;
}
else 
{
{
__VERIFIER_error();
}
 __return_2009 = 0;
}
compRetStatus = __return_2009;
goto label_2013;
}
else 
{
label_2007:; 
 __return_2011 = 0;
label_2011:; 
}
compRetStatus = __return_2011;
label_2013:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_2062;
goto label_2062;
}
}
else 
{
int tmp_ndt_11;
tmp_ndt_11 = __VERIFIER_nondet_int();
if (tmp_ndt_11 == 0)
{
returnVal = 0;
goto label_2171;
}
else 
{
int tmp_ndt_12;
tmp_ndt_12 = __VERIFIER_nondet_int();
if (tmp_ndt_12 == 1)
{
returnVal = -1073741823;
goto label_2171;
}
else 
{
returnVal = 259;
label_2171:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_2313;
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
goto label_2291;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_2291:; 
goto label_2313;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_2313:; 
 __return_2320 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_2215;
}
tmp = __return_2320;
goto label_2325;
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
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 12)
{
{
int __tmp_319 = devobj;
int __tmp_320 = pirp;
int DeviceObject = __tmp_319;
int Irp = __tmp_320;
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
int __tmp_321 = deviceExtension__TargetDeviceObject;
int __tmp_322 = Irp;
int DeviceObject = __tmp_321;
int Irp = __tmp_322;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_1730;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1730;
}
else 
{
returnVal2 = 259;
label_1730:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1868;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1844;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1844:; 
goto label_1868;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1868:; 
 __return_1869 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_1874 = returnVal2;
}
tmp = __return_1869;
goto label_1876;
tmp = __return_1874;
label_1876:; 
 __return_1887 = tmp;
}
status7 = __return_1887;
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10037;
}
else 
{
goto label_9467;
}
}
else 
{
label_9467:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10037;
}
else 
{
goto label_9512;
}
}
else 
{
label_9512:; 
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
goto label_9934;
}
else 
{
goto label_9638;
}
}
else 
{
goto label_9638;
}
}
else 
{
label_9638:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_9860;
}
else 
{
goto label_10037;
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
goto label_9782;
}
else 
{
goto label_10037;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9710;
}
else 
{
goto label_10037;
}
}
}
}
}
else 
{
goto label_10037;
}
}
else 
{
label_10037:; 
 __return_10047 = status7;
goto label_1454;
}
}
}
}
else 
{
{
int __tmp_323 = d;
int DriverObject = __tmp_323;
}
label_9392:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10039;
}
else 
{
goto label_9469;
}
}
else 
{
label_9469:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10039;
}
else 
{
goto label_9510;
}
}
else 
{
label_9510:; 
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
goto label_9934;
}
else 
{
goto label_9636;
}
}
else 
{
goto label_9636;
}
}
else 
{
label_9636:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_9860;
}
else 
{
goto label_10039;
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
label_9782:; 
 __return_10063 = status7;
goto label_1454;
}
else 
{
goto label_10039;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9710;
}
else 
{
goto label_10039;
}
}
}
}
}
else 
{
goto label_10039;
}
}
else 
{
label_10039:; 
 __return_10045 = status7;
goto label_1454;
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
if (routine == 0)
{
{
int __tmp_324 = DeviceObject;
int __tmp_325 = Irp;
int __tmp_326 = lcontext;
int DeviceObject = __tmp_324;
int Irp = __tmp_325;
int Context = __tmp_326;
int Event ;
Event = Context;
{
int __tmp_327 = Event;
int __tmp_328 = 0;
int __tmp_329 = 0;
int Event = __tmp_327;
int Increment = __tmp_328;
int Wait = __tmp_329;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1583 = l;
}
 __return_1586 = -1073741802;
}
compRetStatus = __return_1586;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1617;
label_1617:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_1724;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1724;
}
else 
{
returnVal2 = 259;
label_1724:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1862;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1838;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1838:; 
goto label_1862;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1862:; 
 __return_1872 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1770:; 
 __return_1873 = returnVal2;
}
tmp = __return_1872;
goto label_1876;
tmp = __return_1873;
goto label_1876;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_1728;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1728;
}
else 
{
returnVal2 = 259;
label_1728:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1866;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1842;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1842:; 
goto label_1866;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1866:; 
 __return_1870 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1770;
}
tmp = __return_1870;
goto label_1876;
}
}
}
}
}
}
else 
{
{
int __tmp_330 = DeviceObject;
int __tmp_331 = Irp;
int __tmp_332 = lcontext;
int DeviceObject = __tmp_330;
int Irp = __tmp_331;
int Context = __tmp_332;
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
 __return_1565 = 0;
goto label_1566;
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
goto label_1538;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_1538:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_1546;
}
else 
{
label_1546:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_1562;
}
else 
{
{
__VERIFIER_error();
}
 __return_1564 = 0;
}
compRetStatus = __return_1564;
goto label_1568;
}
else 
{
label_1562:; 
 __return_1566 = 0;
label_1566:; 
}
compRetStatus = __return_1566;
label_1568:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1617;
goto label_1617;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_1726;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1726;
}
else 
{
returnVal2 = 259;
label_1726:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1864;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1840;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1840:; 
goto label_1864;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1864:; 
 __return_1871 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1770;
}
tmp = __return_1871;
goto label_1876;
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
 __return_1454 = -1;
label_1454:; 
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
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10041;
}
else 
{
goto label_9471;
}
}
else 
{
label_9471:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10041;
}
else 
{
goto label_9508;
}
}
else 
{
label_9508:; 
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
label_9934:; 
 __return_10057 = status7;
goto label_10043;
}
else 
{
goto label_9634;
}
}
else 
{
goto label_9634;
}
}
else 
{
label_9634:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
 __return_10059 = status7;
goto label_10043;
}
else 
{
goto label_10041;
}
}
else 
{
if (s == DC)
{
goto label_10041;
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
label_9710:; 
 __return_10065 = status7;
goto label_10043;
}
else 
{
goto label_10041;
}
}
}
}
}
else 
{
goto label_10041;
}
}
else 
{
label_10041:; 
 __return_10043 = status7;
label_10043:; 
return 1;
}
}
}
}
}
