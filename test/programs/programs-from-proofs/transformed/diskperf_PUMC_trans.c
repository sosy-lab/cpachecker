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
int __return_10294;
int __return_9590;
int __return_10288;
int __return_10282;
int __return_9554;
int __return_10280;
int __return_9502;
int __return_9436;
int __return_9441;
int __return_9454;
int __return_9150;
int __return_9153;
int __return_9439;
int __return_9440;
int __return_9437;
int __return_9132;
int __return_9131;
int __return_9133;
int __return_9438;
int __return_6134;
int __return_6139;
int __return_5848;
int __return_5851;
int __return_6137;
int __return_6138;
int __return_6135;
int __return_5830;
int __return_5829;
int __return_5831;
int __return_6136;
int __return_5724;
int __return_5729;
int __return_5438;
int __return_5441;
int __return_5727;
int __return_5728;
int __return_5725;
int __return_5420;
int __return_5419;
int __return_5421;
int __return_5726;
int __return_5314;
int __return_5319;
int __return_6206;
int __return_6207;
int __return_6208;
int __return_6209;
int __return_6216;
int __return_6217;
int __return_6306;
int __return_6307;
int __return_8921;
int __return_8974;
int __return_8982;
int __return_6704;
int __return_6709;
int __return_6776;
int __return_6777;
int __return_6778;
int __return_6779;
int __return_8919;
int __return_6819;
int __return_6820;
int __return_8916;
int __return_7236;
int __return_7241;
int __return_7308;
int __return_7309;
int __return_7310;
int __return_7311;
int __return_8912;
int __return_7370;
int __return_7371;
int __return_8913;
int __return_7924;
int __return_7925;
int __return_8906;
int __return_8753;
int __return_8758;
int __return_8828;
int __return_8829;
int __return_8830;
int __return_8831;
int __return_8902;
int __return_8901;
int __return_8467;
int __return_8470;
int __return_8756;
int __return_8757;
int __return_8754;
int __return_8449;
int __return_8448;
int __return_8450;
int __return_8755;
int __return_8914;
int __return_7351;
int __return_7352;
int __return_8911;
int __return_7768;
int __return_7773;
int __return_7841;
int __return_7842;
int __return_7843;
int __return_7844;
int __return_8910;
int __return_7886;
int __return_7887;
int __return_8905;
int __return_8343;
int __return_8348;
int __return_8887;
int __return_8888;
int __return_8889;
int __return_8890;
int __return_8900;
int __return_8057;
int __return_8060;
int __return_8346;
int __return_8347;
int __return_8344;
int __return_8039;
int __return_8038;
int __return_8040;
int __return_8345;
int __return_8909;
int __return_7905;
int __return_7906;
int __return_7482;
int __return_7485;
int __return_7771;
int __return_7772;
int __return_7769;
int __return_7464;
int __return_7463;
int __return_7465;
int __return_7770;
int __return_8915;
int __return_7943;
int __return_7944;
int __return_6950;
int __return_6953;
int __return_7239;
int __return_7240;
int __return_7237;
int __return_6932;
int __return_6931;
int __return_6933;
int __return_7238;
int __return_8920;
int __return_8917;
int __return_6838;
int __return_6839;
int __return_8918;
int __return_6418;
int __return_6421;
int __return_6707;
int __return_6708;
int __return_6705;
int __return_6400;
int __return_6399;
int __return_6401;
int __return_6706;
int __return_5028;
int __return_5031;
int __return_5317;
int __return_5318;
int __return_5315;
int __return_5010;
int __return_5009;
int __return_5011;
int __return_5316;
int __return_4735;
int __return_4740;
int __return_4449;
int __return_4452;
int __return_4738;
int __return_4739;
int __return_4736;
int __return_4431;
int __return_4430;
int __return_4432;
int __return_4737;
int __return_4325;
int __return_4330;
int __return_4039;
int __return_4042;
int __return_4328;
int __return_4329;
int __return_4326;
int __return_4021;
int __return_4020;
int __return_4022;
int __return_4327;
int __return_3915;
int __return_3920;
int __return_4807;
int __return_4808;
int __return_4809;
int __return_4810;
int __return_4817;
int __return_4818;
int __return_4847;
int __return_8983;
int __return_10278;
int __return_3629;
int __return_3632;
int __return_3918;
int __return_3919;
int __return_3916;
int __return_3611;
int __return_3610;
int __return_3612;
int __return_3917;
int __return_3428;
int __return_3433;
int __return_3142;
int __return_3145;
int __return_3431;
int __return_3432;
int __return_3429;
int __return_3124;
int __return_3123;
int __return_3125;
int __return_3430;
int __return_3018;
int __return_3023;
int __return_3446;
int __return_3449;
int __return_2732;
int __return_2735;
int __return_3021;
int __return_3022;
int __return_3019;
int __return_2714;
int __return_2713;
int __return_2715;
int __return_3020;
int __return_2545;
int __return_2550;
int __return_2563;
int __return_10276;
int __return_2255;
int __return_2258;
int __return_2548;
int __return_2549;
int __return_2546;
int __return_2237;
int __return_2236;
int __return_2238;
int __return_2547;
int __return_2096;
int __return_2101;
int __return_2114;
int __return_10274;
int __return_10290;
int __return_10272;
int __return_1810;
int __return_1813;
int __return_2099;
int __return_2100;
int __return_2097;
int __return_1792;
int __return_1791;
int __return_1793;
int __return_2098;
int __return_1681;
int __return_10284;
int __return_10286;
int __return_10292;
int __return_10270;
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
goto label_1642;
}
else 
{
label_1642:; 
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
 __return_10294 = -1;
goto label_1681;
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
goto label_9587;
label_9587:; 
 __return_9590 = 0;
}
status7 = __return_9590;
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10256;
}
else 
{
goto label_9686;
}
}
else 
{
label_9686:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10256;
}
else 
{
goto label_9747;
}
}
else 
{
label_9747:; 
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
goto label_10161;
}
else 
{
goto label_9873;
}
}
else 
{
goto label_9873;
}
}
else 
{
label_9873:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
label_10087:; 
 __return_10288 = status7;
goto label_1681;
}
else 
{
goto label_10256;
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
goto label_10009;
}
else 
{
goto label_10256;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9937;
}
else 
{
goto label_10256;
}
}
}
}
}
else 
{
goto label_10256;
}
}
else 
{
label_10256:; 
 __return_10282 = status7;
goto label_1681;
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
goto label_9619;
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
goto label_9529;
label_9529:; 
 __return_9554 = status5;
}
status7 = __return_9554;
label_9556:; 
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10258;
}
else 
{
goto label_9688;
}
}
else 
{
label_9688:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10258;
}
else 
{
goto label_9745;
}
}
else 
{
label_9745:; 
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
goto label_10161;
}
else 
{
goto label_9871;
}
}
else 
{
goto label_9871;
}
}
else 
{
label_9871:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_10087;
}
else 
{
goto label_10258;
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
goto label_10009;
}
else 
{
goto label_10258;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9937;
}
else 
{
goto label_10258;
}
}
}
}
}
else 
{
goto label_10258;
}
}
else 
{
label_10258:; 
 __return_10280 = status7;
goto label_1681;
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
goto label_9619;
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
goto label_9499;
label_9499:; 
 __return_9502 = -1073741823;
}
status7 = __return_9502;
goto label_9556;
}
else 
{
totalCounters = Irp__AssociatedIrp__SystemBuffer;
i = 0;
label_9463:; 
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
goto label_9529;
goto label_9529;
}
}
else 
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_9463;
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
goto label_9297;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_9297;
}
else 
{
returnVal2 = 259;
label_9297:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_9435;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_9411;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_9411:; 
goto label_9435;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_9435:; 
 __return_9436 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_9441 = returnVal2;
}
tmp = __return_9436;
goto label_9443;
tmp = __return_9441;
label_9443:; 
 __return_9454 = tmp;
}
status7 = __return_9454;
goto label_9556;
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
 __return_9150 = l;
}
 __return_9153 = -1073741802;
}
compRetStatus = __return_9153;
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
goto label_9184;
label_9184:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_9291;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_9291;
}
else 
{
returnVal2 = 259;
label_9291:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_9429;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_9405;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_9405:; 
goto label_9429;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_9429:; 
 __return_9439 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_9337:; 
 __return_9440 = returnVal2;
}
tmp = __return_9439;
goto label_9443;
tmp = __return_9440;
goto label_9443;
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
goto label_9295;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_9295;
}
else 
{
returnVal2 = 259;
label_9295:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_9433;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_9409;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_9409:; 
goto label_9433;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_9433:; 
 __return_9437 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_9337;
}
tmp = __return_9437;
goto label_9443;
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
 __return_9132 = 0;
goto label_9133;
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
goto label_9105;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_9105:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_9113;
}
else 
{
label_9113:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_9129;
}
else 
{
{
__VERIFIER_error();
}
 __return_9131 = 0;
}
compRetStatus = __return_9131;
goto label_9135;
}
else 
{
label_9129:; 
 __return_9133 = 0;
label_9133:; 
}
compRetStatus = __return_9133;
label_9135:; 
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
goto label_9184;
goto label_9184;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_9293;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_9293;
}
else 
{
returnVal2 = 259;
label_9293:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_9431;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_9407;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_9407:; 
goto label_9431;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_9431:; 
 __return_9438 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_9337;
}
tmp = __return_9438;
goto label_9443;
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
goto label_5995;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5995;
}
else 
{
returnVal2 = 259;
label_5995:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6133;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6109;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6109:; 
goto label_6133;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6133:; 
 __return_6134 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_6139 = returnVal2;
}
status4 = __return_6134;
goto label_5321;
status4 = __return_6139;
goto label_5321;
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
 __return_5848 = l;
}
 __return_5851 = -1073741802;
}
compRetStatus = __return_5851;
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
goto label_5882;
label_5882:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5989;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5989;
}
else 
{
returnVal2 = 259;
label_5989:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6127;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6103;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6103:; 
goto label_6127;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6127:; 
 __return_6137 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_6035:; 
 __return_6138 = returnVal2;
}
status4 = __return_6137;
goto label_5321;
status4 = __return_6138;
goto label_5321;
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
goto label_5993;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5993;
}
else 
{
returnVal2 = 259;
label_5993:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6131;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6107;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6107:; 
goto label_6131;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6131:; 
 __return_6135 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_6035;
}
status4 = __return_6135;
goto label_5321;
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
 __return_5830 = 0;
goto label_5831;
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
goto label_5803;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_5803:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_5811;
}
else 
{
label_5811:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_5827;
}
else 
{
{
__VERIFIER_error();
}
 __return_5829 = 0;
}
compRetStatus = __return_5829;
goto label_5833;
}
else 
{
label_5827:; 
 __return_5831 = 0;
label_5831:; 
}
compRetStatus = __return_5831;
label_5833:; 
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
goto label_5882;
goto label_5882;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5991;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5991;
}
else 
{
returnVal2 = 259;
label_5991:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6129;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6105;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6105:; 
goto label_6129;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6129:; 
 __return_6136 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_6035;
}
status4 = __return_6136;
goto label_5321;
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
goto label_5585;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5585;
}
else 
{
returnVal2 = 259;
label_5585:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5723;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5699;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5699:; 
goto label_5723;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5723:; 
 __return_5724 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_5729 = returnVal2;
}
status4 = __return_5724;
goto label_5321;
status4 = __return_5729;
goto label_5321;
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
 __return_5438 = l;
}
 __return_5441 = -1073741802;
}
compRetStatus = __return_5441;
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
goto label_5472;
label_5472:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5579;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5579;
}
else 
{
returnVal2 = 259;
label_5579:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5717;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5693;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5693:; 
goto label_5717;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5717:; 
 __return_5727 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_5625:; 
 __return_5728 = returnVal2;
}
status4 = __return_5727;
goto label_5321;
status4 = __return_5728;
goto label_5321;
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
goto label_5583;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5583;
}
else 
{
returnVal2 = 259;
label_5583:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5721;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5697;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5697:; 
goto label_5721;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5721:; 
 __return_5725 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_5625;
}
status4 = __return_5725;
goto label_5321;
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
 __return_5420 = 0;
goto label_5421;
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
goto label_5393;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_5393:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_5401;
}
else 
{
label_5401:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_5417;
}
else 
{
{
__VERIFIER_error();
}
 __return_5419 = 0;
}
compRetStatus = __return_5419;
goto label_5423;
}
else 
{
label_5417:; 
 __return_5421 = 0;
label_5421:; 
}
compRetStatus = __return_5421;
label_5423:; 
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
goto label_5472;
goto label_5472;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5581;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5581;
}
else 
{
returnVal2 = 259;
label_5581:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5719;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5695;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5695:; 
goto label_5719;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5719:; 
 __return_5726 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_5625;
}
status4 = __return_5726;
goto label_5321;
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
goto label_5175;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5175;
}
else 
{
returnVal2 = 259;
label_5175:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5313;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5289;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5289:; 
goto label_5313;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5313:; 
 __return_5314 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_5319 = returnVal2;
}
status4 = __return_5314;
goto label_5321;
status4 = __return_5319;
label_5321:; 
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
goto label_6187;
}
else 
{
goto label_6164;
}
}
else 
{
label_6164:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_6187;
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
 __return_6206 = 0;
goto label_6207;
}
else 
{
 __return_6207 = -1073741823;
label_6207:; 
}
goto label_6211;
}
else 
{
label_6187:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_6208 = 0;
goto label_6209;
}
else 
{
 __return_6209 = -1073741823;
label_6209:; 
}
label_6211:; 
status4 = myStatus;
 __return_6216 = status4;
}
status2 = __return_6216;
goto label_6219;
}
}
}
}
else 
{
 __return_6217 = status4;
}
status2 = __return_6217;
label_6219:; 
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
 __return_6306 = malloc_ret;
goto label_6307;
}
else 
{
 __return_6307 = 0;
label_6307:; 
}
irp = __return_6307;
if (irp == 0)
{
 __return_8921 = -1073741670;
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
goto label_6565;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6565;
}
else 
{
returnVal2 = 259;
label_6565:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6703;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6679;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6679:; 
goto label_6703;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6703:; 
 __return_6704 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_6709 = returnVal2;
}
status6 = __return_6704;
goto label_6711;
status6 = __return_6709;
label_6711:; 
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
goto label_6757;
}
else 
{
goto label_6734;
}
}
else 
{
label_6734:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_6757;
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
 __return_6776 = 0;
goto label_6777;
}
else 
{
 __return_6777 = -1073741823;
label_6777:; 
}
goto label_6781;
}
else 
{
label_6757:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_6778 = 0;
goto label_6779;
}
else 
{
 __return_6779 = -1073741823;
label_6779:; 
}
label_6781:; 
status6 = ioStatus__Status;
if (status6 < 0)
{
outputSize = sizeof__MOUNTDEV_NAME;
if (output == 0)
{
 __return_8919 = -1073741670;
goto label_8920;
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
 __return_6819 = malloc_ret;
goto label_6820;
}
else 
{
 __return_6820 = 0;
label_6820:; 
}
irp = __return_6820;
label_6822:; 
if (irp == 0)
{
 __return_8916 = -1073741670;
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
goto label_7097;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7097;
}
else 
{
returnVal2 = 259;
label_7097:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7235;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7211;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7211:; 
goto label_7235;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7235:; 
 __return_7236 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_7241 = returnVal2;
}
status6 = __return_7236;
goto label_7243;
status6 = __return_7241;
label_7243:; 
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
goto label_7289;
}
else 
{
goto label_7266;
}
}
else 
{
label_7266:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_7289;
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
 __return_7308 = 0;
goto label_7309;
}
else 
{
 __return_7309 = -1073741823;
label_7309:; 
}
goto label_7313;
}
else 
{
label_7289:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_7310 = 0;
goto label_7311;
}
else 
{
 __return_7311 = -1073741823;
label_7311:; 
}
label_7313:; 
status6 = ioStatus__Status;
__cil_tmp28 = (unsigned long)status6;
if (__cil_tmp28 == -2147483643)
{
outputSize = sizeof__MOUNTDEV_NAME + output__NameLength;
if (output == 0)
{
 __return_8912 = -1073741670;
goto label_8913;
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
 __return_7370 = malloc_ret;
goto label_7371;
}
else 
{
 __return_7371 = 0;
label_7371:; 
}
irp = __return_7371;
goto label_7354;
}
}
}
else 
{
if (status6 < 0)
{
 __return_8913 = status6;
label_8913:; 
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
 __return_7924 = malloc_ret;
goto label_7925;
}
else 
{
 __return_7925 = 0;
label_7925:; 
}
irp = __return_7925;
label_7927:; 
if (irp == 0)
{
 __return_8906 = -1073741670;
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
goto label_8614;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8614;
}
else 
{
returnVal2 = 259;
label_8614:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8752;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8728;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8728:; 
goto label_8752;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8752:; 
 __return_8753 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_8758 = returnVal2;
}
status6 = __return_8753;
goto label_8760;
status6 = __return_8758;
label_8760:; 
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
goto label_8809;
}
else 
{
goto label_8786;
}
}
else 
{
label_8786:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_8809;
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
 __return_8828 = 0;
goto label_8829;
}
else 
{
 __return_8829 = -1073741823;
label_8829:; 
}
goto label_8833;
}
else 
{
label_8809:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_8830 = 0;
goto label_8831;
}
else 
{
 __return_8831 = -1073741823;
label_8831:; 
}
label_8833:; 
status6 = ioStatus__Status;
 __return_8902 = status6;
}
goto label_8923;
}
}
}
}
else 
{
 __return_8901 = status6;
}
goto label_8923;
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
 __return_8467 = l;
}
 __return_8470 = -1073741802;
}
compRetStatus = __return_8470;
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
goto label_8501;
label_8501:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_8608;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8608;
}
else 
{
returnVal2 = 259;
label_8608:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8746;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8722;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8722:; 
goto label_8746;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8746:; 
 __return_8756 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_8654:; 
 __return_8757 = returnVal2;
}
status6 = __return_8756;
goto label_8760;
status6 = __return_8757;
goto label_8760;
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
goto label_8612;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8612;
}
else 
{
returnVal2 = 259;
label_8612:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8750;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8726;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8726:; 
goto label_8750;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8750:; 
 __return_8754 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_8654;
}
status6 = __return_8754;
goto label_8760;
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
 __return_8449 = 0;
goto label_8450;
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
goto label_8422;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_8422:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_8430;
}
else 
{
label_8430:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_8446;
}
else 
{
{
__VERIFIER_error();
}
 __return_8448 = 0;
}
compRetStatus = __return_8448;
goto label_8452;
}
else 
{
label_8446:; 
 __return_8450 = 0;
label_8450:; 
}
compRetStatus = __return_8450;
label_8452:; 
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
goto label_8501;
goto label_8501;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_8610;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8610;
}
else 
{
returnVal2 = 259;
label_8610:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8748;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8724;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8724:; 
goto label_8748;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8748:; 
 __return_8755 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_8654;
}
status6 = __return_8755;
goto label_8760;
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
goto label_8923;
}
}
goto label_8923;
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
 __return_8914 = -1073741670;
goto label_8915;
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
 __return_7351 = malloc_ret;
goto label_7352;
}
else 
{
 __return_7352 = 0;
label_7352:; 
}
irp = __return_7352;
label_7354:; 
if (irp == 0)
{
 __return_8911 = -1073741670;
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
goto label_7629;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7629;
}
else 
{
returnVal2 = 259;
label_7629:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7767;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7743;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7743:; 
goto label_7767;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7767:; 
 __return_7768 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_7773 = returnVal2;
}
status6 = __return_7768;
goto label_7775;
status6 = __return_7773;
label_7775:; 
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
goto label_7822;
}
else 
{
goto label_7799;
}
}
else 
{
label_7799:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_7822;
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
 __return_7841 = 0;
goto label_7842;
}
else 
{
 __return_7842 = -1073741823;
label_7842:; 
}
goto label_7846;
}
else 
{
label_7822:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_7843 = 0;
goto label_7844;
}
else 
{
 __return_7844 = -1073741823;
label_7844:; 
}
label_7846:; 
status6 = ioStatus__Status;
if (status6 < 0)
{
 __return_8910 = status6;
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
 __return_7886 = malloc_ret;
goto label_7887;
}
else 
{
 __return_7887 = 0;
label_7887:; 
}
irp = __return_7887;
label_7889:; 
if (irp == 0)
{
 __return_8905 = -1073741670;
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
goto label_8204;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8204;
}
else 
{
returnVal2 = 259;
label_8204:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8342;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8318;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8318:; 
goto label_8342;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8342:; 
 __return_8343 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_8348 = returnVal2;
}
status6 = __return_8343;
goto label_8350;
status6 = __return_8348;
label_8350:; 
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
goto label_8868;
}
else 
{
goto label_8845;
}
}
else 
{
label_8845:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_8868;
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
 __return_8887 = 0;
goto label_8888;
}
else 
{
 __return_8888 = -1073741823;
label_8888:; 
}
goto label_8833;
}
else 
{
label_8868:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_8889 = 0;
goto label_8890;
}
else 
{
 __return_8890 = -1073741823;
label_8890:; 
}
goto label_8833;
}
}
}
}
}
else 
{
 __return_8900 = status6;
}
goto label_8923;
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
 __return_8057 = l;
}
 __return_8060 = -1073741802;
}
compRetStatus = __return_8060;
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
goto label_8091;
label_8091:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_8198;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8198;
}
else 
{
returnVal2 = 259;
label_8198:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8336;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8312;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8312:; 
goto label_8336;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8336:; 
 __return_8346 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_8244:; 
 __return_8347 = returnVal2;
}
status6 = __return_8346;
goto label_8350;
status6 = __return_8347;
goto label_8350;
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
goto label_8202;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8202;
}
else 
{
returnVal2 = 259;
label_8202:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8340;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8316;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8316:; 
goto label_8340;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8340:; 
 __return_8344 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_8244;
}
status6 = __return_8344;
goto label_8350;
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
 __return_8039 = 0;
goto label_8040;
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
goto label_8012;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_8012:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_8020;
}
else 
{
label_8020:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_8036;
}
else 
{
{
__VERIFIER_error();
}
 __return_8038 = 0;
}
compRetStatus = __return_8038;
goto label_8042;
}
else 
{
label_8036:; 
 __return_8040 = 0;
label_8040:; 
}
compRetStatus = __return_8040;
label_8042:; 
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
goto label_8091;
goto label_8091;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_8200;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_8200;
}
else 
{
returnVal2 = 259;
label_8200:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_8338;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_8314;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_8314:; 
goto label_8338;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_8338:; 
 __return_8345 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_8244;
}
status6 = __return_8345;
goto label_8350;
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
goto label_8923;
}
}
goto label_8923;
}
}
}
}
}
else 
{
if (status6 < 0)
{
 __return_8909 = status6;
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
 __return_7905 = malloc_ret;
goto label_7906;
}
else 
{
 __return_7906 = 0;
label_7906:; 
}
irp = __return_7906;
goto label_7889;
}
}
goto label_8923;
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
 __return_7482 = l;
}
 __return_7485 = -1073741802;
}
compRetStatus = __return_7485;
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
goto label_7516;
label_7516:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7623;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7623;
}
else 
{
returnVal2 = 259;
label_7623:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7761;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7737;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7737:; 
goto label_7761;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7761:; 
 __return_7771 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_7669:; 
 __return_7772 = returnVal2;
}
status6 = __return_7771;
goto label_7775;
status6 = __return_7772;
goto label_7775;
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
goto label_7627;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7627;
}
else 
{
returnVal2 = 259;
label_7627:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7765;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7741;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7741:; 
goto label_7765;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7765:; 
 __return_7769 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_7669;
}
status6 = __return_7769;
goto label_7775;
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
 __return_7464 = 0;
goto label_7465;
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
goto label_7437;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_7437:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_7445;
}
else 
{
label_7445:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_7461;
}
else 
{
{
__VERIFIER_error();
}
 __return_7463 = 0;
}
compRetStatus = __return_7463;
goto label_7467;
}
else 
{
label_7461:; 
 __return_7465 = 0;
label_7465:; 
}
compRetStatus = __return_7465;
label_7467:; 
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
goto label_7516;
goto label_7516;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7625;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7625;
}
else 
{
returnVal2 = 259;
label_7625:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7763;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7739;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7739:; 
goto label_7763;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7763:; 
 __return_7770 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_7669;
}
status6 = __return_7770;
goto label_7775;
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
goto label_8923;
}
}
}
else 
{
if (status6 < 0)
{
 __return_8915 = status6;
label_8915:; 
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
 __return_7943 = malloc_ret;
goto label_7944;
}
else 
{
 __return_7944 = 0;
label_7944:; 
}
irp = __return_7944;
goto label_7927;
}
}
goto label_8923;
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
 __return_6950 = l;
}
 __return_6953 = -1073741802;
}
compRetStatus = __return_6953;
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
goto label_6984;
label_6984:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7091;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7091;
}
else 
{
returnVal2 = 259;
label_7091:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7229;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7205;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7205:; 
goto label_7229;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7229:; 
 __return_7239 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_7137:; 
 __return_7240 = returnVal2;
}
status6 = __return_7239;
goto label_7243;
status6 = __return_7240;
goto label_7243;
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
goto label_7095;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7095;
}
else 
{
returnVal2 = 259;
label_7095:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7233;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7209;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7209:; 
goto label_7233;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7233:; 
 __return_7237 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_7137;
}
status6 = __return_7237;
goto label_7243;
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
 __return_6932 = 0;
goto label_6933;
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
goto label_6905;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_6905:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_6913;
}
else 
{
label_6913:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_6929;
}
else 
{
{
__VERIFIER_error();
}
 __return_6931 = 0;
}
compRetStatus = __return_6931;
goto label_6935;
}
else 
{
label_6929:; 
 __return_6933 = 0;
label_6933:; 
}
compRetStatus = __return_6933;
label_6935:; 
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
goto label_6984;
goto label_6984;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_7093;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_7093;
}
else 
{
returnVal2 = 259;
label_7093:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7231;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7207;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_7207:; 
goto label_7231;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_7231:; 
 __return_7238 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_7137;
}
status6 = __return_7238;
goto label_7243;
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
goto label_8923;
}
}
}
else 
{
 __return_8920 = status6;
label_8920:; 
}
goto label_8923;
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
 __return_8917 = -1073741670;
goto label_8918;
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
 __return_6838 = malloc_ret;
goto label_6839;
}
else 
{
 __return_6839 = 0;
label_6839:; 
}
irp = __return_6839;
goto label_6822;
}
}
}
else 
{
 __return_8918 = status6;
label_8918:; 
}
goto label_8923;
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
 __return_6418 = l;
}
 __return_6421 = -1073741802;
}
compRetStatus = __return_6421;
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
goto label_6452;
label_6452:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_6559;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6559;
}
else 
{
returnVal2 = 259;
label_6559:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6697;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6673;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6673:; 
goto label_6697;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6697:; 
 __return_6707 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_6605:; 
 __return_6708 = returnVal2;
}
status6 = __return_6707;
goto label_6711;
status6 = __return_6708;
goto label_6711;
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
goto label_6563;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6563;
}
else 
{
returnVal2 = 259;
label_6563:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6701;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6677;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6677:; 
goto label_6701;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6701:; 
 __return_6705 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_6605;
}
status6 = __return_6705;
goto label_6711;
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
 __return_6400 = 0;
goto label_6401;
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
goto label_6373;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_6373:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_6381;
}
else 
{
label_6381:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_6397;
}
else 
{
{
__VERIFIER_error();
}
 __return_6399 = 0;
}
compRetStatus = __return_6399;
goto label_6403;
}
else 
{
label_6397:; 
 __return_6401 = 0;
label_6401:; 
}
compRetStatus = __return_6401;
label_6403:; 
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
goto label_6452;
goto label_6452;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_6561;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_6561;
}
else 
{
returnVal2 = 259;
label_6561:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6699;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6675;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_6675:; 
goto label_6699;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_6699:; 
 __return_6706 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_6605;
}
status6 = __return_6706;
goto label_6711;
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
label_8923:; 
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
goto label_8971;
label_8971:; 
 __return_8974 = status2;
}
status1 = __return_8974;
 __return_8982 = status1;
}
status7 = __return_8982;
goto label_8985;
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
 __return_5028 = l;
}
 __return_5031 = -1073741802;
}
compRetStatus = __return_5031;
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
goto label_5062;
label_5062:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5169;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5169;
}
else 
{
returnVal2 = 259;
label_5169:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5307;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5283;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5283:; 
goto label_5307;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5307:; 
 __return_5317 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_5215:; 
 __return_5318 = returnVal2;
}
status4 = __return_5317;
goto label_5321;
status4 = __return_5318;
goto label_5321;
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
goto label_5173;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5173;
}
else 
{
returnVal2 = 259;
label_5173:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5311;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5287;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5287:; 
goto label_5311;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5311:; 
 __return_5315 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_5215;
}
status4 = __return_5315;
goto label_5321;
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
 __return_5010 = 0;
goto label_5011;
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
goto label_4983;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_4983:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_4991;
}
else 
{
label_4991:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_5007;
}
else 
{
{
__VERIFIER_error();
}
 __return_5009 = 0;
}
compRetStatus = __return_5009;
goto label_5013;
}
else 
{
label_5007:; 
 __return_5011 = 0;
label_5011:; 
}
compRetStatus = __return_5011;
label_5013:; 
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
goto label_5062;
goto label_5062;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_5171;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_5171;
}
else 
{
returnVal2 = 259;
label_5171:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5309;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5285;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5285:; 
goto label_5309;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5309:; 
 __return_5316 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_5215;
}
status4 = __return_5316;
goto label_5321;
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
goto label_4596;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4596;
}
else 
{
returnVal2 = 259;
label_4596:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4734;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4710;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4710:; 
goto label_4734;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4734:; 
 __return_4735 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_4740 = returnVal2;
}
status4 = __return_4735;
goto label_3922;
status4 = __return_4740;
goto label_3922;
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
 __return_4449 = l;
}
 __return_4452 = -1073741802;
}
compRetStatus = __return_4452;
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
goto label_4483;
label_4483:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4590;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4590;
}
else 
{
returnVal2 = 259;
label_4590:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4728;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4704;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4704:; 
goto label_4728;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4728:; 
 __return_4738 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4636:; 
 __return_4739 = returnVal2;
}
status4 = __return_4738;
goto label_3922;
status4 = __return_4739;
goto label_3922;
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
goto label_4594;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4594;
}
else 
{
returnVal2 = 259;
label_4594:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4732;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4708;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4708:; 
goto label_4732;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4732:; 
 __return_4736 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4636;
}
status4 = __return_4736;
goto label_3922;
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
 __return_4431 = 0;
goto label_4432;
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
goto label_4404;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_4404:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_4412;
}
else 
{
label_4412:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_4428;
}
else 
{
{
__VERIFIER_error();
}
 __return_4430 = 0;
}
compRetStatus = __return_4430;
goto label_4434;
}
else 
{
label_4428:; 
 __return_4432 = 0;
label_4432:; 
}
compRetStatus = __return_4432;
label_4434:; 
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
goto label_4483;
goto label_4483;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4592;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4592;
}
else 
{
returnVal2 = 259;
label_4592:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4730;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4706;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4706:; 
goto label_4730;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4730:; 
 __return_4737 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4636;
}
status4 = __return_4737;
goto label_3922;
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
goto label_4186;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4186;
}
else 
{
returnVal2 = 259;
label_4186:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4324;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4300;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4300:; 
goto label_4324;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4324:; 
 __return_4325 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_4330 = returnVal2;
}
status4 = __return_4325;
goto label_3922;
status4 = __return_4330;
goto label_3922;
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
 __return_4039 = l;
}
 __return_4042 = -1073741802;
}
compRetStatus = __return_4042;
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
goto label_4073;
label_4073:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4180;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4180;
}
else 
{
returnVal2 = 259;
label_4180:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4318;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4294;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4294:; 
goto label_4318;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4318:; 
 __return_4328 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4226:; 
 __return_4329 = returnVal2;
}
status4 = __return_4328;
goto label_3922;
status4 = __return_4329;
goto label_3922;
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
goto label_4184;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4184;
}
else 
{
returnVal2 = 259;
label_4184:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4322;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4298;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4298:; 
goto label_4322;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4322:; 
 __return_4326 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4226;
}
status4 = __return_4326;
goto label_3922;
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
 __return_4021 = 0;
goto label_4022;
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
goto label_3994;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_3994:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_4002;
}
else 
{
label_4002:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_4018;
}
else 
{
{
__VERIFIER_error();
}
 __return_4020 = 0;
}
compRetStatus = __return_4020;
goto label_4024;
}
else 
{
label_4018:; 
 __return_4022 = 0;
label_4022:; 
}
compRetStatus = __return_4022;
label_4024:; 
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
goto label_4073;
goto label_4073;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_4182;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_4182;
}
else 
{
returnVal2 = 259;
label_4182:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4320;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4296;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4296:; 
goto label_4320;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4320:; 
 __return_4327 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4226;
}
status4 = __return_4327;
goto label_3922;
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
goto label_3776;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3776;
}
else 
{
returnVal2 = 259;
label_3776:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3914;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3890;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3890:; 
goto label_3914;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3914:; 
 __return_3915 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_3920 = returnVal2;
}
status4 = __return_3915;
goto label_3922;
status4 = __return_3920;
label_3922:; 
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
goto label_4788;
}
else 
{
goto label_4765;
}
}
else 
{
label_4765:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_4788;
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
 __return_4807 = 0;
goto label_4808;
}
else 
{
 __return_4808 = -1073741823;
label_4808:; 
}
goto label_4812;
}
else 
{
label_4788:; 
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_4809 = 0;
goto label_4810;
}
else 
{
 __return_4810 = -1073741823;
label_4810:; 
}
label_4812:; 
status4 = myStatus;
 __return_4817 = status4;
}
status3 = __return_4817;
goto label_4820;
}
}
}
}
else 
{
 __return_4818 = status4;
}
status3 = __return_4818;
label_4820:; 
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
goto label_4844;
label_4844:; 
 __return_4847 = status3;
}
status1 = __return_4847;
 __return_8983 = status1;
}
status7 = __return_8983;
label_8985:; 
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10260;
}
else 
{
goto label_9690;
}
}
else 
{
label_9690:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10260;
}
else 
{
goto label_9743;
}
}
else 
{
label_9743:; 
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
goto label_10161;
}
else 
{
goto label_9869;
}
}
else 
{
goto label_9869;
}
}
else 
{
label_9869:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_10087;
}
else 
{
goto label_10260;
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
goto label_10009;
}
else 
{
goto label_10260;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9937;
}
else 
{
goto label_10260;
}
}
}
}
}
else 
{
goto label_10260;
}
}
else 
{
label_10260:; 
 __return_10278 = status7;
goto label_1681;
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
goto label_9619;
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
 __return_3629 = l;
}
 __return_3632 = -1073741802;
}
compRetStatus = __return_3632;
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
goto label_3663;
label_3663:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3770;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3770;
}
else 
{
returnVal2 = 259;
label_3770:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3908;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3884;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3884:; 
goto label_3908;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3908:; 
 __return_3918 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3816:; 
 __return_3919 = returnVal2;
}
status4 = __return_3918;
goto label_3922;
status4 = __return_3919;
goto label_3922;
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
goto label_3774;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3774;
}
else 
{
returnVal2 = 259;
label_3774:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3912;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3888;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3888:; 
goto label_3912;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3912:; 
 __return_3916 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3816;
}
status4 = __return_3916;
goto label_3922;
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
 __return_3611 = 0;
goto label_3612;
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
goto label_3584;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_3584:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_3592;
}
else 
{
label_3592:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_3608;
}
else 
{
{
__VERIFIER_error();
}
 __return_3610 = 0;
}
compRetStatus = __return_3610;
goto label_3614;
}
else 
{
label_3608:; 
 __return_3612 = 0;
label_3612:; 
}
compRetStatus = __return_3612;
label_3614:; 
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
goto label_3663;
goto label_3663;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3772;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3772;
}
else 
{
returnVal2 = 259;
label_3772:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3910;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3886;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3886:; 
goto label_3910;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3910:; 
 __return_3917 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3816;
}
status4 = __return_3917;
goto label_3922;
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
goto label_3289;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3289;
}
else 
{
returnVal2 = 259;
label_3289:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3427;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3403;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3403:; 
goto label_3427;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3427:; 
 __return_3428 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_3433 = returnVal2;
}
tmp = __return_3428;
goto label_3025;
tmp = __return_3433;
goto label_3025;
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
 __return_3142 = l;
}
 __return_3145 = -1073741802;
}
compRetStatus = __return_3145;
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
goto label_3176;
label_3176:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3283;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3283;
}
else 
{
returnVal2 = 259;
label_3283:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3421;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3397;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3397:; 
goto label_3421;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3421:; 
 __return_3431 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3329:; 
 __return_3432 = returnVal2;
}
tmp = __return_3431;
goto label_3025;
tmp = __return_3432;
goto label_3025;
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
goto label_3287;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3287;
}
else 
{
returnVal2 = 259;
label_3287:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3425;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3401;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3401:; 
goto label_3425;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3425:; 
 __return_3429 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3329;
}
tmp = __return_3429;
goto label_3025;
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
 __return_3124 = 0;
goto label_3125;
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
goto label_3097;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_3097:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_3105;
}
else 
{
label_3105:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_3121;
}
else 
{
{
__VERIFIER_error();
}
 __return_3123 = 0;
}
compRetStatus = __return_3123;
goto label_3127;
}
else 
{
label_3121:; 
 __return_3125 = 0;
label_3125:; 
}
compRetStatus = __return_3125;
label_3127:; 
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
goto label_3176;
goto label_3176;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_3285;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_3285;
}
else 
{
returnVal2 = 259;
label_3285:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3423;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3399;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3399:; 
goto label_3423;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3423:; 
 __return_3430 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3329;
}
tmp = __return_3430;
goto label_3025;
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
goto label_2879;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2879;
}
else 
{
returnVal2 = 259;
label_2879:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3017;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2993;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2993:; 
goto label_3017;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3017:; 
 __return_3018 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_3023 = returnVal2;
}
tmp = __return_3018;
goto label_3025;
tmp = __return_3023;
label_3025:; 
 __return_3446 = tmp;
}
tmp = __return_3446;
 __return_3449 = tmp;
}
status7 = __return_3449;
goto label_8985;
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
 __return_2732 = l;
}
 __return_2735 = -1073741802;
}
compRetStatus = __return_2735;
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
goto label_2766;
label_2766:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_2873;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2873;
}
else 
{
returnVal2 = 259;
label_2873:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3011;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2987;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2987:; 
goto label_3011;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3011:; 
 __return_3021 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2919:; 
 __return_3022 = returnVal2;
}
tmp = __return_3021;
goto label_3025;
tmp = __return_3022;
goto label_3025;
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
goto label_2877;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2877;
}
else 
{
returnVal2 = 259;
label_2877:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3015;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2991;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2991:; 
goto label_3015;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3015:; 
 __return_3019 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2919;
}
tmp = __return_3019;
goto label_3025;
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
 __return_2714 = 0;
goto label_2715;
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
goto label_2687;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_2687:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_2695;
}
else 
{
label_2695:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_2711;
}
else 
{
{
__VERIFIER_error();
}
 __return_2713 = 0;
}
compRetStatus = __return_2713;
goto label_2717;
}
else 
{
label_2711:; 
 __return_2715 = 0;
label_2715:; 
}
compRetStatus = __return_2715;
label_2717:; 
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
goto label_2766;
goto label_2766;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_2875;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_2875;
}
else 
{
returnVal2 = 259;
label_2875:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3013;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2989;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2989:; 
goto label_3013;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3013:; 
 __return_3020 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2919;
}
tmp = __return_3020;
goto label_3025;
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
goto label_2402;
}
else 
{
int tmp_ndt_12;
tmp_ndt_12 = __VERIFIER_nondet_int();
if (tmp_ndt_12 == 1)
{
returnVal = -1073741823;
goto label_2402;
}
else 
{
returnVal = 259;
label_2402:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_2544;
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
goto label_2514;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_2514:; 
goto label_2544;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_2544:; 
 __return_2545 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
 __return_2550 = returnVal;
}
tmp = __return_2545;
goto label_2552;
tmp = __return_2550;
label_2552:; 
 __return_2563 = tmp;
}
status7 = __return_2563;
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10262;
}
else 
{
goto label_9692;
}
}
else 
{
label_9692:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10262;
}
else 
{
goto label_9741;
}
}
else 
{
label_9741:; 
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
goto label_10161;
}
else 
{
goto label_9867;
}
}
else 
{
goto label_9867;
}
}
else 
{
label_9867:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_10087;
}
else 
{
goto label_10262;
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
goto label_10009;
}
else 
{
goto label_10262;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9937;
}
else 
{
goto label_10262;
}
}
}
}
}
else 
{
goto label_10262;
}
}
else 
{
label_10262:; 
 __return_10276 = status7;
goto label_1681;
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
goto label_9619;
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
 __return_2255 = l;
}
 __return_2258 = -1073741802;
}
compRetStatus = __return_2258;
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
goto label_2289;
label_2289:; 
int tmp_ndt_11;
tmp_ndt_11 = __VERIFIER_nondet_int();
if (tmp_ndt_11 == 0)
{
returnVal = 0;
goto label_2396;
}
else 
{
int tmp_ndt_12;
tmp_ndt_12 = __VERIFIER_nondet_int();
if (tmp_ndt_12 == 1)
{
returnVal = -1073741823;
goto label_2396;
}
else 
{
returnVal = 259;
label_2396:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_2538;
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
goto label_2520;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_2520:; 
goto label_2538;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_2538:; 
 __return_2548 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
label_2442:; 
 __return_2549 = returnVal;
}
tmp = __return_2548;
goto label_2552;
tmp = __return_2549;
goto label_2552;
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
goto label_2400;
}
else 
{
int tmp_ndt_12;
tmp_ndt_12 = __VERIFIER_nondet_int();
if (tmp_ndt_12 == 1)
{
returnVal = -1073741823;
goto label_2400;
}
else 
{
returnVal = 259;
label_2400:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_2542;
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
goto label_2516;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_2516:; 
goto label_2542;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_2542:; 
 __return_2546 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_2442;
}
tmp = __return_2546;
goto label_2552;
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
 __return_2237 = 0;
goto label_2238;
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
goto label_2210;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_2210:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_2218;
}
else 
{
label_2218:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_2234;
}
else 
{
{
__VERIFIER_error();
}
 __return_2236 = 0;
}
compRetStatus = __return_2236;
goto label_2240;
}
else 
{
label_2234:; 
 __return_2238 = 0;
label_2238:; 
}
compRetStatus = __return_2238;
label_2240:; 
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
goto label_2289;
goto label_2289;
}
}
else 
{
int tmp_ndt_11;
tmp_ndt_11 = __VERIFIER_nondet_int();
if (tmp_ndt_11 == 0)
{
returnVal = 0;
goto label_2398;
}
else 
{
int tmp_ndt_12;
tmp_ndt_12 = __VERIFIER_nondet_int();
if (tmp_ndt_12 == 1)
{
returnVal = -1073741823;
goto label_2398;
}
else 
{
returnVal = 259;
label_2398:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_2540;
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
goto label_2518;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_2518:; 
goto label_2540;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_2540:; 
 __return_2547 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_2442;
}
tmp = __return_2547;
goto label_2552;
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
goto label_1957;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1957;
}
else 
{
returnVal2 = 259;
label_1957:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2095;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2071;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2071:; 
goto label_2095;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2095:; 
 __return_2096 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_2101 = returnVal2;
}
tmp = __return_2096;
goto label_2103;
tmp = __return_2101;
label_2103:; 
 __return_2114 = tmp;
}
status7 = __return_2114;
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10264;
}
else 
{
goto label_9694;
}
}
else 
{
label_9694:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10264;
}
else 
{
goto label_9739;
}
}
else 
{
label_9739:; 
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
goto label_10161;
}
else 
{
goto label_9865;
}
}
else 
{
goto label_9865;
}
}
else 
{
label_9865:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_10087;
}
else 
{
goto label_10264;
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
goto label_10009;
}
else 
{
goto label_10264;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9937;
}
else 
{
goto label_10264;
}
}
}
}
}
else 
{
goto label_10264;
}
}
else 
{
label_10264:; 
 __return_10274 = status7;
goto label_1681;
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
label_9619:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_10266;
}
else 
{
goto label_9696;
}
}
else 
{
label_9696:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10266;
}
else 
{
goto label_9737;
}
}
else 
{
label_9737:; 
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
goto label_10161;
}
else 
{
goto label_9863;
}
}
else 
{
goto label_9863;
}
}
else 
{
label_9863:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
goto label_10087;
}
else 
{
goto label_10266;
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
label_10009:; 
 __return_10290 = status7;
goto label_1681;
}
else 
{
goto label_10266;
}
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9937;
}
else 
{
goto label_10266;
}
}
}
}
}
else 
{
goto label_10266;
}
}
else 
{
label_10266:; 
 __return_10272 = status7;
goto label_1681;
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
 __return_1810 = l;
}
 __return_1813 = -1073741802;
}
compRetStatus = __return_1813;
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
goto label_1844;
label_1844:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_1951;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1951;
}
else 
{
returnVal2 = 259;
label_1951:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2089;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2065;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2065:; 
goto label_2089;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2089:; 
 __return_2099 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1997:; 
 __return_2100 = returnVal2;
}
tmp = __return_2099;
goto label_2103;
tmp = __return_2100;
goto label_2103;
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
goto label_1955;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1955;
}
else 
{
returnVal2 = 259;
label_1955:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2093;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2069;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2069:; 
goto label_2093;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2093:; 
 __return_2097 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1997;
}
tmp = __return_2097;
goto label_2103;
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
 __return_1792 = 0;
goto label_1793;
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
goto label_1765;
}
else 
{
partitionCounters__BytesWritten__QuadPart = partitionCounters__BytesWritten__QuadPart + Irp__IoStatus__Information;
int __CPAchecker_TMP_1 = partitionCounters__WriteCount;
partitionCounters__WriteCount = partitionCounters__WriteCount + 1;
__CPAchecker_TMP_1;
partitionCounters__WriteTime__QuadPart = partitionCounters__WriteTime__QuadPart + difference__QuadPart;
label_1765:; 
if (Irp__Flags != 8)
{
int __CPAchecker_TMP_2 = partitionCounters__SplitCount;
partitionCounters__SplitCount = partitionCounters__SplitCount + 1;
__CPAchecker_TMP_2;
goto label_1773;
}
else 
{
label_1773:; 
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_1789;
}
else 
{
{
__VERIFIER_error();
}
 __return_1791 = 0;
}
compRetStatus = __return_1791;
goto label_1795;
}
else 
{
label_1789:; 
 __return_1793 = 0;
label_1793:; 
}
compRetStatus = __return_1793;
label_1795:; 
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
goto label_1844;
goto label_1844;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal2 = 0;
goto label_1953;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal2 = -1073741823;
goto label_1953;
}
else 
{
returnVal2 = 259;
label_1953:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2091;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2067;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2067:; 
goto label_2091;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2091:; 
 __return_2098 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1997;
}
tmp = __return_2098;
goto label_2103;
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
 __return_1681 = -1;
label_1681:; 
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
goto label_10268;
}
else 
{
goto label_9698;
}
}
else 
{
label_9698:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_10268;
}
else 
{
goto label_9735;
}
}
else 
{
label_9735:; 
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
label_10161:; 
 __return_10284 = status7;
goto label_10270;
}
else 
{
goto label_9861;
}
}
else 
{
goto label_9861;
}
}
else 
{
label_9861:; 
if (pended == 1)
{
if (status7 != 259)
{
{
__VERIFIER_error();
}
 __return_10286 = status7;
goto label_10270;
}
else 
{
goto label_10268;
}
}
else 
{
if (s == DC)
{
goto label_10268;
}
else 
{
if (status7 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
label_9937:; 
 __return_10292 = status7;
goto label_10270;
}
else 
{
goto label_10268;
}
}
}
}
}
else 
{
goto label_10268;
}
}
else 
{
label_10268:; 
 __return_10270 = status7;
label_10270:; 
return 1;
}
}
}
}
}
