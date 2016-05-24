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
int routine  ;
int myStatus  ;
int pirp  ;
int Executive ;
int Suspended ;
int KernelMode ;
int DeviceUsageTypePaging ;
void errorFn(void);
void _BLAST_init(void);
int SendSrbSynchronous(int Extension , int Srb , int Buffer , int BufferLength );
int CdAudioSignalCompletion(int DeviceObject , int Irp , int Event );
int CdAudioStartDevice(int DeviceObject , int Irp );
int CdAudioPnp(int DeviceObject , int Irp );
int CdAudioDeviceControl(int DeviceObject , int Irp );
int CdAudioSendToNextDriver(int DeviceObject , int Irp );
int CdAudioIsPlayActive(int DeviceObject );
int CdAudio535DeviceControl(int DeviceObject , int Irp );
int AG_SetStatusAndReturn(int status , int Irp , int deviceExtension__TargetDeviceObject );
int CdAudio435DeviceControl(int DeviceObject , int Irp );
int CdAudioAtapiDeviceControl(int DeviceObject , int Irp );
void HpCdrProcessLastSession(int Toc );
int HPCdrCompletion(int DeviceObject , int Irp , int Context );
int CdAudioHPCdrDeviceControl(int DeviceObject , int Irp );
int CdAudioForwardIrpSynchronous(int DeviceObject , int Irp );
void CdAudioUnload(int DriverObject );
int CdAudioPower(int DeviceObject , int Irp );
void stub_driver_init(void);
int main(void);
void stubMoreProcessingRequired(void);
int IofCallDriver(int DeviceObject , int Irp );
int KeSetEvent(int Event , int Increment , int Wait );
int KeWaitForSingleObject(int Object , int WaitReason , int WaitMode , int Alertable ,
                          int Timeout );
int PoCallDriver(int DeviceObject , int Irp );
int ZwClose(int Handle );
int __return_8992;
int __return_435;
int __return_515;
int __return_597;
int __return_599;
int __return_714;
int __return_802;
int __return_804;
int __return_806;
int __return_1431;
int __return_1519;
int __return_1521;
int __return_1523;
int __return_856;
int __return_852;
int __return_1079;
int __return_1167;
int __return_1224;
int __return_1220;
int __return_1277;
int __return_1317;
int __return_1352;
int __return_934;
int __return_937;
int __return_1025;
int __return_1271;
int __return_1267;
int __return_1278;
int __return_1308;
int __return_1351;
int __return_1747;
int __return_1835;
int __return_1892;
int __return_1888;
int __return_1945;
int __return_2436;
int __return_2479;
int __return_2416;
int __return_2409;
int __return_2405;
int __return_2412;
int __return_2434;
int __return_2346;
int __return_2342;
int __return_2335;
int __return_2331;
int __return_2344;
int __return_2341;
int __return_2288;
int __return_2284;
int __return_2424;
int __return_1602;
int __return_1605;
int __return_1693;
int __return_1939;
int __return_1935;
int __return_1946;
int __return_2435;
int __return_2480;
int __return_8989;
int __return_2418;
int __return_2392;
int __return_2388;
int __return_2414;
int __return_2432;
int __return_2174;
int __return_2170;
int __return_2163;
int __return_2159;
int __return_2172;
int __return_2169;
int __return_2116;
int __return_2112;
int __return_2426;
int __return_2590;
int __return_2678;
int __return_2680;
int __return_8818;
int __return_2959;
int __return_3047;
int __return_3049;
int __return_2789;
int __return_2877;
int __return_2879;
int __return_2881;
int __return_3434;
int __return_3479;
int __return_3460;
int __return_3430;
int __return_3423;
int __return_3419;
int __return_3432;
int __return_3429;
int __return_3376;
int __return_3372;
int __return_3166;
int __return_3254;
int __return_3256;
int __return_3258;
int __return_6459;
int __return_6107;
int __return_6105;
int __return_6103;
int __return_6391;
int __return_6293;
int __return_6362;
int __return_6460;
int __return_6289;
int __return_6282;
int __return_6278;
int __return_6291;
int __return_6288;
int __return_6235;
int __return_6231;
int __return_6101;
int __return_6099;
int __return_6082;
int __return_6078;
int __return_5773;
int __return_5997;
int __return_5964;
int __return_5960;
int __return_5953;
int __return_5949;
int __return_5962;
int __return_5959;
int __return_5906;
int __return_5902;
int __return_5769;
int __return_5762;
int __return_5758;
int __return_5771;
int __return_5768;
int __return_5715;
int __return_5711;
int __return_5575;
int __return_5571;
int __return_5564;
int __return_5560;
int __return_5573;
int __return_5570;
int __return_5517;
int __return_5513;
int __return_5397;
int __return_5108;
int __return_5343;
int __return_5286;
int __return_5318;
int __return_5282;
int __return_5275;
int __return_5271;
int __return_5284;
int __return_5281;
int __return_5228;
int __return_5224;
int __return_5104;
int __return_5097;
int __return_5093;
int __return_5106;
int __return_5103;
int __return_5050;
int __return_5046;
int __return_5370;
int __return_4887;
int __return_4883;
int __return_4876;
int __return_4872;
int __return_4885;
int __return_4882;
int __return_4829;
int __return_4825;
int __return_4926;
int __return_4703;
int __return_4674;
int __return_4627;
int __return_4623;
int __return_4616;
int __return_4612;
int __return_4625;
int __return_4622;
int __return_4569;
int __return_4565;
int __return_4440;
int __return_4436;
int __return_4429;
int __return_4425;
int __return_4438;
int __return_4435;
int __return_4382;
int __return_4378;
int __return_4040;
int __return_4038;
int __return_4036;
int __return_4127;
int __return_4215;
int __return_4217;
int __return_4219;
int __return_4250;
int __return_4034;
int __return_4032;
int __return_4015;
int __return_4011;
int __return_3840;
int __return_3928;
int __return_3930;
int __return_3932;
int __return_8369;
int __return_8367;
int __return_8365;
int __return_8622;
int __return_8747;
int __return_8801;
int __return_8803;
int __return_8565;
int __return_8772;
int __return_8774;
int __return_8561;
int __return_8554;
int __return_8550;
int __return_8563;
int __return_8560;
int __return_8507;
int __return_8503;
int __return_8363;
int __return_8361;
int __return_8344;
int __return_8340;
int __return_8694;
int __return_8744;
int __return_7966;
int __return_7964;
int __return_7962;
int __return_8259;
int __return_8261;
int __return_8152;
int __return_8646;
int __return_8746;
int __return_8194;
int __return_8196;
int __return_8670;
int __return_8745;
int __return_8148;
int __return_8141;
int __return_8137;
int __return_8150;
int __return_8147;
int __return_8094;
int __return_8090;
int __return_7960;
int __return_7958;
int __return_7941;
int __return_7937;
int __return_7843;
int __return_7845;
int __return_7814;
int __return_7816;
int __return_7775;
int __return_7771;
int __return_7764;
int __return_7760;
int __return_7773;
int __return_7770;
int __return_7717;
int __return_7713;
int __return_7584;
int __return_8718;
int __return_7580;
int __return_7573;
int __return_7569;
int __return_7582;
int __return_7579;
int __return_7526;
int __return_7522;
int __return_7396;
int __return_8742;
int __return_7392;
int __return_7385;
int __return_7381;
int __return_7394;
int __return_7391;
int __return_7338;
int __return_7334;
int __return_7212;
int __return_7208;
int __return_7201;
int __return_7197;
int __return_7210;
int __return_7207;
int __return_7154;
int __return_7150;
int __return_6847;
int __return_6845;
int __return_6843;
int __return_6841;
int __return_6839;
int __return_6822;
int __return_6818;
int __return_6930;
int __return_7018;
int __return_7020;
int __return_7022;
int __return_8990;
int main()
{
int pirp__IoStatus__Status ;
int d = __VERIFIER_nondet_int() ;
d = __VERIFIER_nondet_int();
int status10 = __VERIFIER_nondet_int() ;
status10 = __VERIFIER_nondet_int();
int irp = __VERIFIER_nondet_int() ;
irp = __VERIFIER_nondet_int();
int we_should_unload = __VERIFIER_nondet_int() ;
we_should_unload = __VERIFIER_nondet_int();
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
routine = 0;
myStatus = 0;
pirp = 0;
Executive = 0;
Suspended = 5;
KernelMode = 0;
DeviceUsageTypePaging = 1;
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
if (status10 >= 0)
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
goto label_397;
}
else 
{
label_397:; 
{
s = NP;
customIrp = 0;
setEventCalled = customIrp;
lowerDriverReturn = setEventCalled;
compRegistered = lowerDriverReturn;
compFptr = compRegistered;
pended = compFptr;
}
if (status10 < 0)
{
 __return_8992 = -1;
goto label_435;
}
else 
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (!(tmp_ndt_1 == 2))
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (!(tmp_ndt_2 == 3))
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (!(tmp_ndt_3 == 4))
{
 __return_435 = -1;
label_435:; 
return 1;
}
else 
{
{
int __tmp_1 = devobj;
int __tmp_2 = pirp;
int DeviceObject = __tmp_1;
int Irp = __tmp_2;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
if (s == NP)
{
s = SKIP1;
goto label_455;
}
else 
{
{
__VERIFIER_error();
}
label_455:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_3 = deviceExtension__TargetDeviceObject;
int __tmp_4 = Irp;
int DeviceObject = __tmp_3;
int Irp = __tmp_4;
int compRetStatus ;
int returnVal ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_5 = DeviceObject;
int __tmp_6 = Irp;
int __tmp_7 = lcontext;
int DeviceObject = __tmp_5;
int Irp = __tmp_6;
int Context = __tmp_7;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_484:; 
if (myStatus >= 0)
{
{
int __tmp_8 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_8;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_507;
}
else 
{
label_507:; 
}
goto label_500;
}
}
else 
{
label_500:; 
 __return_515 = myStatus;
}
compRetStatus = __return_515;
__cil_tmp7 = (unsigned long)compRetStatus;
if (!(__cil_tmp7 == -1073741802))
{
goto label_474;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_533;
}
else 
{
{
__VERIFIER_error();
}
label_533:; 
}
goto label_474;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_484;
}
else 
{
pended = 1;
goto label_484;
}
}
}
}
else 
{
label_474:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal = 0;
goto label_554;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (!(tmp_ndt_9 == 1))
{
returnVal = 259;
label_554:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_579;
}
else 
{
if (s == MPR1)
{
__cil_tmp8 = (long)returnVal;
if (!(__cil_tmp8 == 259L))
{
s = NP;
lowerDriverReturn = returnVal;
label_589:; 
goto label_579;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal;
goto label_589;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
goto label_579;
}
else 
{
{
__VERIFIER_error();
}
label_579:; 
 __return_597 = returnVal;
}
tmp = __return_597;
 __return_599 = tmp;
}
status10 = __return_599;
label_601:; 
if (we_should_unload == 0)
{
goto label_381;
}
else 
{
{
int __tmp_9 = d;
int DriverObject = __tmp_9;
}
goto label_381;
}
}
}
else 
{
returnVal = -1073741823;
goto label_554;
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
int __tmp_10 = devobj;
int __tmp_11 = pirp;
int DeviceObject = __tmp_10;
int Irp = __tmp_11;
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int irpSp__MinorFunction = __VERIFIER_nondet_int() ;
irpSp__MinorFunction = __VERIFIER_nondet_int();
int Irp__IoStatus__Status ;
int irpSp__Parameters__UsageNotification__Type = __VERIFIER_nondet_int() ;
irpSp__Parameters__UsageNotification__Type = __VERIFIER_nondet_int();
int deviceExtension__PagingPathCountEvent = __VERIFIER_nondet_int() ;
deviceExtension__PagingPathCountEvent = __VERIFIER_nondet_int();
int irpSp__Parameters__UsageNotification__InPath = __VERIFIER_nondet_int() ;
irpSp__Parameters__UsageNotification__InPath = __VERIFIER_nondet_int();
int deviceExtension__PagingPathCount = __VERIFIER_nondet_int() ;
deviceExtension__PagingPathCount = __VERIFIER_nondet_int();
int DeviceObject__Flags ;
int irpSp ;
int status3 ;
int setPagable ;
int tmp ;
int tmp___0 ;
irpSp = Irp__Tail__Overlay__CurrentStackLocation;
status3 = -1073741637;
if (!(irpSp__MinorFunction == 0))
{
if (!(irpSp__MinorFunction == 22))
{
{
int __tmp_12 = DeviceObject;
int __tmp_13 = Irp;
int DeviceObject = __tmp_12;
int Irp = __tmp_13;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
if (s == NP)
{
s = SKIP1;
goto label_653;
}
else 
{
{
__VERIFIER_error();
}
label_653:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_14 = deviceExtension__TargetDeviceObject;
int __tmp_15 = Irp;
int DeviceObject = __tmp_14;
int Irp = __tmp_15;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_16 = DeviceObject;
int __tmp_17 = Irp;
int __tmp_18 = lcontext;
int DeviceObject = __tmp_16;
int Irp = __tmp_17;
int Context = __tmp_18;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_683:; 
if (myStatus >= 0)
{
{
int __tmp_19 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_19;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_706;
}
else 
{
label_706:; 
}
goto label_699;
}
}
else 
{
label_699:; 
 __return_714 = myStatus;
}
compRetStatus = __return_714;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_673;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_732;
}
else 
{
{
__VERIFIER_error();
}
label_732:; 
}
goto label_673;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_683;
}
else 
{
pended = 1;
goto label_683;
}
}
}
}
else 
{
label_673:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_760;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_760:; 
goto label_744;
}
else 
{
returnVal2 = -1073741823;
goto label_760;
}
}
}
else 
{
returnVal2 = 259;
label_744:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_785;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_794:; 
goto label_785;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_794;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_785;
}
else 
{
{
__VERIFIER_error();
}
label_785:; 
 __return_802 = returnVal2;
}
tmp = __return_802;
 __return_804 = tmp;
}
tmp___0 = __return_804;
 __return_806 = tmp___0;
}
status10 = __return_806;
goto label_2482;
}
}
}
}
}
}
else 
{
if (irpSp__Parameters__UsageNotification__Type != DeviceUsageTypePaging)
{
{
int __tmp_20 = DeviceObject;
int __tmp_21 = Irp;
int DeviceObject = __tmp_20;
int Irp = __tmp_21;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
if (s == NP)
{
s = SKIP1;
goto label_1370;
}
else 
{
{
__VERIFIER_error();
}
label_1370:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_22 = deviceExtension__TargetDeviceObject;
int __tmp_23 = Irp;
int DeviceObject = __tmp_22;
int Irp = __tmp_23;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_24 = DeviceObject;
int __tmp_25 = Irp;
int __tmp_26 = lcontext;
int DeviceObject = __tmp_24;
int Irp = __tmp_25;
int Context = __tmp_26;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_1400:; 
if (myStatus >= 0)
{
{
int __tmp_27 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_27;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_1423;
}
else 
{
label_1423:; 
}
goto label_1416;
}
}
else 
{
label_1416:; 
 __return_1431 = myStatus;
}
compRetStatus = __return_1431;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1390;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1449;
}
else 
{
{
__VERIFIER_error();
}
label_1449:; 
}
goto label_1390;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_1400;
}
else 
{
pended = 1;
goto label_1400;
}
}
}
}
else 
{
label_1390:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1477;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1477:; 
goto label_1461;
}
else 
{
returnVal2 = -1073741823;
goto label_1477;
}
}
}
else 
{
returnVal2 = 259;
label_1461:; 
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
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1511:; 
goto label_1502;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1511;
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
 __return_1519 = returnVal2;
}
tmp = __return_1519;
 __return_1521 = tmp;
}
tmp = __return_1521;
 __return_1523 = tmp;
}
status10 = __return_1523;
goto label_2482;
}
}
}
}
}
}
else 
{
{
int __tmp_28 = deviceExtension__PagingPathCountEvent;
int __tmp_29 = Executive;
int __tmp_30 = KernelMode;
int __tmp_31 = 0;
int __tmp_32 = 0;
int Object = __tmp_28;
int WaitReason = __tmp_29;
int WaitMode = __tmp_30;
int Alertable = __tmp_31;
int Timeout = __tmp_32;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_816;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_826;
}
}
else 
{
label_816:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_826;
}
else 
{
label_826:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_856 = 0;
goto label_852;
}
else 
{
 __return_852 = -1073741823;
label_852:; 
}
status3 = __return_852;
setPagable = 0;
if (irpSp__Parameters__UsageNotification__InPath == 0)
{
goto label_868;
}
else 
{
if (deviceExtension__PagingPathCount != 1)
{
label_868:; 
if (status3 == status3)
{
setPagable = 1;
goto label_867;
}
else 
{
goto label_867;
}
}
else 
{
label_867:; 
{
int __tmp_33 = DeviceObject;
int __tmp_34 = Irp;
int DeviceObject = __tmp_33;
int Irp = __tmp_34;
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int status9 ;
int irpSp__Control ;
if (s != NP)
{
{
__VERIFIER_error();
}
goto label_899;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
label_899:; 
irpSp__Control = 224;
{
int __tmp_35 = deviceExtension__TargetDeviceObject;
int __tmp_36 = Irp;
int DeviceObject = __tmp_35;
int Irp = __tmp_36;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_37 = DeviceObject;
int __tmp_38 = Irp;
int __tmp_39 = lcontext;
int DeviceObject = __tmp_37;
int Irp = __tmp_38;
int Context = __tmp_39;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_1048:; 
if (myStatus >= 0)
{
{
int __tmp_40 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_40;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_1071;
}
else 
{
label_1071:; 
}
goto label_1064;
}
}
else 
{
label_1064:; 
 __return_1079 = myStatus;
}
compRetStatus = __return_1079;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1038;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1097;
}
else 
{
{
__VERIFIER_error();
}
label_1097:; 
}
goto label_1038;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_1048;
}
else 
{
pended = 1;
goto label_1048;
}
}
}
}
else 
{
label_1038:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1125;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1125:; 
goto label_1109;
}
else 
{
returnVal2 = -1073741823;
goto label_1125;
}
}
}
else 
{
returnVal2 = 259;
label_1109:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1150;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1159:; 
goto label_1150;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1159;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1150;
}
else 
{
{
__VERIFIER_error();
}
label_1150:; 
 __return_1167 = returnVal2;
}
status9 = __return_1167;
status9 = 259;
if (!(status9 == 0))
{
{
int __tmp_41 = event;
int __tmp_42 = Executive;
int __tmp_43 = KernelMode;
int __tmp_44 = 0;
int __tmp_45 = 0;
int Object = __tmp_41;
int WaitReason = __tmp_42;
int WaitMode = __tmp_43;
int Alertable = __tmp_44;
int Timeout = __tmp_45;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_1184;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_1194;
}
}
else 
{
label_1184:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_1194;
}
else 
{
label_1194:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_1224 = 0;
goto label_1220;
}
else 
{
 __return_1220 = -1073741823;
label_1220:; 
}
status9 = myStatus;
goto label_1178;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_1194;
}
}
}
}
else 
{
label_1178:; 
 __return_1277 = status9;
}
status3 = __return_1277;
if (status3 >= 0)
{
goto label_1291;
}
else 
{
if (!(setPagable == 1))
{
label_1291:; 
{
int __tmp_46 = deviceExtension__PagingPathCountEvent;
int __tmp_47 = 0;
int __tmp_48 = 0;
int Event = __tmp_46;
int Increment = __tmp_47;
int Wait = __tmp_48;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1317 = l;
}
{
int __tmp_49 = Irp;
int __tmp_50 = 0;
int Irp = __tmp_49;
int PriorityBoost = __tmp_50;
if (s == NP)
{
s = DC;
goto label_1330;
}
else 
{
{
__VERIFIER_error();
}
label_1330:; 
}
 __return_1352 = status3;
}
status10 = __return_1352;
goto label_2482;
}
else 
{
setPagable = 0;
goto label_1291;
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
routine = 1;
irpSp__Control = 224;
{
int __tmp_51 = deviceExtension__TargetDeviceObject;
int __tmp_52 = Irp;
int DeviceObject = __tmp_51;
int Irp = __tmp_52;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_53 = DeviceObject;
int __tmp_54 = Irp;
int __tmp_55 = lcontext;
int DeviceObject = __tmp_53;
int Irp = __tmp_54;
int Event = __tmp_55;
{
int __tmp_56 = Event;
int __tmp_57 = 0;
int __tmp_58 = 0;
int Event = __tmp_56;
int Increment = __tmp_57;
int Wait = __tmp_58;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_934 = l;
}
 __return_937 = -1073741802;
}
compRetStatus = __return_937;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_922;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_955;
}
else 
{
{
__VERIFIER_error();
}
label_955:; 
}
goto label_922;
}
}
}
else 
{
label_922:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_983;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_983:; 
goto label_967;
}
else 
{
returnVal2 = -1073741823;
goto label_983;
}
}
}
else 
{
returnVal2 = 259;
label_967:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1008;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1017:; 
goto label_1008;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1017;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1008;
}
else 
{
{
__VERIFIER_error();
}
label_1008:; 
 __return_1025 = returnVal2;
}
status9 = __return_1025;
status9 = 259;
if (!(status9 == 0))
{
{
int __tmp_59 = event;
int __tmp_60 = Executive;
int __tmp_61 = KernelMode;
int __tmp_62 = 0;
int __tmp_63 = 0;
int Object = __tmp_59;
int WaitReason = __tmp_60;
int WaitMode = __tmp_61;
int Alertable = __tmp_62;
int Timeout = __tmp_63;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_1231;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_1241;
}
}
else 
{
label_1231:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_1241;
}
else 
{
label_1241:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_1271 = 0;
goto label_1267;
}
else 
{
 __return_1267 = -1073741823;
label_1267:; 
}
status9 = myStatus;
goto label_1175;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_1241;
}
}
}
}
else 
{
label_1175:; 
 __return_1278 = status9;
}
status3 = __return_1278;
if (status3 >= 0)
{
goto label_1292;
}
else 
{
if (!(setPagable == 1))
{
label_1292:; 
{
int __tmp_64 = deviceExtension__PagingPathCountEvent;
int __tmp_65 = 0;
int __tmp_66 = 0;
int Event = __tmp_64;
int Increment = __tmp_65;
int Wait = __tmp_66;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1308 = l;
}
{
int __tmp_67 = Irp;
int __tmp_68 = 0;
int Irp = __tmp_67;
int PriorityBoost = __tmp_68;
if (s == NP)
{
s = DC;
goto label_1346;
}
else 
{
{
__VERIFIER_error();
}
label_1346:; 
}
 __return_1351 = status3;
}
status10 = __return_1351;
goto label_2481;
}
else 
{
setPagable = 0;
goto label_1292;
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
s = NP;
customIrp = 0;
goto label_826;
}
}
}
}
}
}
else 
{
{
int __tmp_69 = DeviceObject;
int __tmp_70 = Irp;
int DeviceObject = __tmp_69;
int Irp = __tmp_70;
int deviceExtension__Active = __VERIFIER_nondet_int() ;
deviceExtension__Active = __VERIFIER_nondet_int();
int deviceExtension = __VERIFIER_nondet_int() ;
deviceExtension = __VERIFIER_nondet_int();
int status2 ;
int srb = __VERIFIER_nondet_int() ;
srb = __VERIFIER_nondet_int();
int srb__Cdb = __VERIFIER_nondet_int() ;
srb__Cdb = __VERIFIER_nondet_int();
int cdb ;
int inquiryDataPtr ;
int attempt ;
int tmp ;
int deviceParameterHandle = __VERIFIER_nondet_int() ;
deviceParameterHandle = __VERIFIER_nondet_int();
int keyValue ;
{
int __tmp_71 = DeviceObject;
int __tmp_72 = Irp;
int DeviceObject = __tmp_71;
int Irp = __tmp_72;
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int status9 ;
int irpSp__Control ;
if (s != NP)
{
{
__VERIFIER_error();
}
goto label_1567;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
label_1567:; 
irpSp__Control = 224;
{
int __tmp_73 = deviceExtension__TargetDeviceObject;
int __tmp_74 = Irp;
int DeviceObject = __tmp_73;
int Irp = __tmp_74;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_75 = DeviceObject;
int __tmp_76 = Irp;
int __tmp_77 = lcontext;
int DeviceObject = __tmp_75;
int Irp = __tmp_76;
int Context = __tmp_77;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_1716:; 
if (myStatus >= 0)
{
{
int __tmp_78 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_78;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_1739;
}
else 
{
label_1739:; 
}
goto label_1732;
}
}
else 
{
label_1732:; 
 __return_1747 = myStatus;
}
compRetStatus = __return_1747;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1706;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1765;
}
else 
{
{
__VERIFIER_error();
}
label_1765:; 
}
goto label_1706;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_1716;
}
else 
{
pended = 1;
goto label_1716;
}
}
}
}
else 
{
label_1706:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1793;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1793:; 
goto label_1777;
}
else 
{
returnVal2 = -1073741823;
goto label_1793;
}
}
}
else 
{
returnVal2 = 259;
label_1777:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1818;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1827:; 
goto label_1818;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1827;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1818;
}
else 
{
{
__VERIFIER_error();
}
label_1818:; 
 __return_1835 = returnVal2;
}
status9 = __return_1835;
status9 = 259;
if (!(status9 == 0))
{
{
int __tmp_79 = event;
int __tmp_80 = Executive;
int __tmp_81 = KernelMode;
int __tmp_82 = 0;
int __tmp_83 = 0;
int Object = __tmp_79;
int WaitReason = __tmp_80;
int WaitMode = __tmp_81;
int Alertable = __tmp_82;
int Timeout = __tmp_83;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_1852;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_1862;
}
}
else 
{
label_1852:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_1862;
}
else 
{
label_1862:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_1892 = 0;
goto label_1888;
}
else 
{
 __return_1888 = -1073741823;
label_1888:; 
}
status9 = myStatus;
goto label_1846;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_1862;
}
}
}
}
else 
{
label_1846:; 
 __return_1945 = status9;
}
status2 = __return_1945;
if (status2 < 0)
{
 __return_2436 = status2;
}
else 
{
if (!(deviceExtension__Active == 255))
{
label_1957:; 
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_2416 = 0;
goto label_2412;
}
else 
{
if (status2 < 0)
{
goto label_2372;
}
else 
{
label_2372:; 
{
int __tmp_86 = deviceParameterHandle;
int Handle = __tmp_86;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2409 = 0;
goto label_2405;
}
else 
{
 __return_2405 = -1073741823;
label_2405:; 
}
 __return_2412 = 0;
label_2412:; 
}
status3 = __return_2412;
goto label_2437;
}
}
}
else 
{
cdb = srb__Cdb;
inquiryDataPtr = 0;
attempt = 0;
if (inquiryDataPtr == 0)
{
deviceExtension__Active = 0;
 __return_2434 = 0;
goto label_2424;
}
else 
{
status2 = -1073741823;
label_1982:; 
if (status2 < 0)
{
tmp = attempt;
int __CPAchecker_TMP_0 = attempt;
attempt = attempt + 1;
__CPAchecker_TMP_0;
if (tmp >= 4)
{
goto label_1992;
}
else 
{
{
int __tmp_87 = deviceExtension;
int __tmp_88 = srb;
int __tmp_89 = inquiryDataPtr;
int __tmp_90 = 36;
int Extension = __tmp_87;
int Srb = __tmp_88;
int Buffer = __tmp_89;
int BufferLength = __tmp_90;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_2346 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2238:; 
 __return_2342 = status1;
}
else 
{
{
int __tmp_91 = event;
int __tmp_92 = Executive;
int __tmp_93 = KernelMode;
int __tmp_94 = 0;
int __tmp_95 = 0;
int Object = __tmp_91;
int WaitReason = __tmp_92;
int WaitMode = __tmp_93;
int Alertable = __tmp_94;
int Timeout = __tmp_95;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_2295;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2305;
}
}
else 
{
label_2295:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2305;
}
else 
{
label_2305:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2335 = 0;
goto label_2331;
}
else 
{
 __return_2331 = -1073741823;
label_2331:; 
}
status1 = ioStatus__Status;
goto label_2238;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2305;
}
}
}
}
status2 = __return_2342;
goto label_2347;
}
status2 = __return_2346;
label_2347:; 
goto label_1982;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_2344 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2241:; 
 __return_2341 = status1;
}
else 
{
{
int __tmp_96 = event;
int __tmp_97 = Executive;
int __tmp_98 = KernelMode;
int __tmp_99 = 0;
int __tmp_100 = 0;
int Object = __tmp_96;
int WaitReason = __tmp_97;
int WaitMode = __tmp_98;
int Alertable = __tmp_99;
int Timeout = __tmp_100;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_2248;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2258;
}
}
else 
{
label_2248:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2258;
}
else 
{
label_2258:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2288 = 0;
goto label_2284;
}
else 
{
 __return_2284 = -1073741823;
label_2284:; 
}
status1 = ioStatus__Status;
goto label_2241;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2258;
}
}
}
}
status2 = __return_2341;
goto label_2347;
}
status2 = __return_2344;
goto label_2347;
}
}
}
}
else 
{
label_1992:; 
if (status2 < 0)
{
deviceExtension__Active = 0;
 __return_2424 = 0;
label_2424:; 
}
else 
{
deviceExtension__Active = 0;
goto label_1957;
}
status3 = __return_2424;
goto label_2437;
}
}
}
}
status3 = __return_2436;
label_2437:; 
Irp__IoStatus__Status = status3;
myStatus = status3;
{
int __tmp_84 = Irp;
int __tmp_85 = 0;
int Irp = __tmp_84;
int PriorityBoost = __tmp_85;
if (s == NP)
{
s = DC;
goto label_2474;
}
else 
{
{
__VERIFIER_error();
}
label_2474:; 
}
 __return_2479 = status3;
}
status10 = __return_2479;
label_2482:; 
goto label_601;
}
}
}
}
}
}
else 
{
compRegistered = 1;
routine = 1;
irpSp__Control = 224;
{
int __tmp_101 = deviceExtension__TargetDeviceObject;
int __tmp_102 = Irp;
int DeviceObject = __tmp_101;
int Irp = __tmp_102;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_103 = DeviceObject;
int __tmp_104 = Irp;
int __tmp_105 = lcontext;
int DeviceObject = __tmp_103;
int Irp = __tmp_104;
int Event = __tmp_105;
{
int __tmp_106 = Event;
int __tmp_107 = 0;
int __tmp_108 = 0;
int Event = __tmp_106;
int Increment = __tmp_107;
int Wait = __tmp_108;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1602 = l;
}
 __return_1605 = -1073741802;
}
compRetStatus = __return_1605;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1590;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1623;
}
else 
{
{
__VERIFIER_error();
}
label_1623:; 
}
goto label_1590;
}
}
}
else 
{
label_1590:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1651;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1651:; 
goto label_1635;
}
else 
{
returnVal2 = -1073741823;
goto label_1651;
}
}
}
else 
{
returnVal2 = 259;
label_1635:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1676;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1685:; 
goto label_1676;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1685;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1676;
}
else 
{
{
__VERIFIER_error();
}
label_1676:; 
 __return_1693 = returnVal2;
}
status9 = __return_1693;
status9 = 259;
if (!(status9 == 0))
{
{
int __tmp_109 = event;
int __tmp_110 = Executive;
int __tmp_111 = KernelMode;
int __tmp_112 = 0;
int __tmp_113 = 0;
int Object = __tmp_109;
int WaitReason = __tmp_110;
int WaitMode = __tmp_111;
int Alertable = __tmp_112;
int Timeout = __tmp_113;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_1899;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_1909;
}
}
else 
{
label_1899:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_1909;
}
else 
{
label_1909:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_1939 = 0;
goto label_1935;
}
else 
{
 __return_1935 = -1073741823;
label_1935:; 
}
status9 = myStatus;
goto label_1843;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_1909;
}
}
}
}
else 
{
label_1843:; 
 __return_1946 = status9;
}
status2 = __return_1946;
if (status2 < 0)
{
 __return_2435 = status2;
}
else 
{
if (!(deviceExtension__Active == 255))
{
label_1954:; 
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_2418 = 0;
goto label_2414;
}
else 
{
if (status2 < 0)
{
goto label_2374;
}
else 
{
label_2374:; 
{
int __tmp_117 = deviceParameterHandle;
int Handle = __tmp_117;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2392 = 0;
goto label_2388;
}
else 
{
 __return_2388 = -1073741823;
label_2388:; 
}
 __return_2414 = 0;
label_2414:; 
}
status3 = __return_2414;
goto label_2438;
}
}
}
else 
{
cdb = srb__Cdb;
inquiryDataPtr = 0;
attempt = 0;
if (inquiryDataPtr == 0)
{
deviceExtension__Active = 0;
 __return_2432 = 0;
goto label_2426;
}
else 
{
status2 = -1073741823;
label_1981:; 
if (status2 < 0)
{
tmp = attempt;
int __CPAchecker_TMP_0 = attempt;
attempt = attempt + 1;
__CPAchecker_TMP_0;
if (tmp >= 4)
{
goto label_1991;
}
else 
{
{
int __tmp_118 = deviceExtension;
int __tmp_119 = srb;
int __tmp_120 = inquiryDataPtr;
int __tmp_121 = 36;
int Extension = __tmp_118;
int Srb = __tmp_119;
int Buffer = __tmp_120;
int BufferLength = __tmp_121;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_2174 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2066:; 
 __return_2170 = status1;
}
else 
{
{
int __tmp_122 = event;
int __tmp_123 = Executive;
int __tmp_124 = KernelMode;
int __tmp_125 = 0;
int __tmp_126 = 0;
int Object = __tmp_122;
int WaitReason = __tmp_123;
int WaitMode = __tmp_124;
int Alertable = __tmp_125;
int Timeout = __tmp_126;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_2123;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2133;
}
}
else 
{
label_2123:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2133;
}
else 
{
label_2133:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2163 = 0;
goto label_2159;
}
else 
{
 __return_2159 = -1073741823;
label_2159:; 
}
status1 = ioStatus__Status;
goto label_2066;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2133;
}
}
}
}
status2 = __return_2170;
goto label_2175;
}
status2 = __return_2174;
label_2175:; 
goto label_1981;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_2172 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2069:; 
 __return_2169 = status1;
}
else 
{
{
int __tmp_127 = event;
int __tmp_128 = Executive;
int __tmp_129 = KernelMode;
int __tmp_130 = 0;
int __tmp_131 = 0;
int Object = __tmp_127;
int WaitReason = __tmp_128;
int WaitMode = __tmp_129;
int Alertable = __tmp_130;
int Timeout = __tmp_131;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_2076;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2086;
}
}
else 
{
label_2076:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2086;
}
else 
{
label_2086:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2116 = 0;
goto label_2112;
}
else 
{
 __return_2112 = -1073741823;
label_2112:; 
}
status1 = ioStatus__Status;
goto label_2069;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2086;
}
}
}
}
status2 = __return_2169;
goto label_2175;
}
status2 = __return_2172;
goto label_2175;
}
}
}
}
else 
{
label_1991:; 
if (status2 < 0)
{
deviceExtension__Active = 0;
 __return_2426 = 0;
label_2426:; 
}
else 
{
deviceExtension__Active = 0;
goto label_1954;
}
status3 = __return_2426;
goto label_2438;
}
}
}
}
status3 = __return_2435;
label_2438:; 
Irp__IoStatus__Status = status3;
myStatus = status3;
{
int __tmp_114 = Irp;
int __tmp_115 = 0;
int Irp = __tmp_114;
int PriorityBoost = __tmp_115;
if (s == NP)
{
s = DC;
goto label_2458;
}
else 
{
{
__VERIFIER_error();
}
label_2458:; 
}
 __return_2480 = status3;
}
status10 = __return_2480;
label_2481:; 
if (we_should_unload == 0)
{
goto label_8841;
}
else 
{
{
int __tmp_116 = d;
int DriverObject = __tmp_116;
}
label_8841:; 
if (!(pended == 1))
{
label_8852:; 
if (!(pended == 1))
{
label_8866:; 
if (s != UNLOADED)
{
if (status10 != -1)
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
goto label_8878;
}
else 
{
goto label_8892;
}
}
else 
{
goto label_8892;
}
}
else 
{
label_8892:; 
if (pended != 1)
{
if (s == DC)
{
if (!(status10 == 259))
{
label_8938:; 
goto label_8878;
}
else 
{
{
__VERIFIER_error();
}
goto label_8938;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_8878;
}
else 
{
goto label_8878;
}
}
}
else 
{
goto label_8878;
}
}
}
else 
{
goto label_8878;
}
}
else 
{
label_8878:; 
 __return_8989 = status10;
return 1;
}
}
else 
{
if (s == MPR3)
{
s = MPR3;
goto label_8878;
}
else 
{
goto label_8866;
}
}
}
else 
{
if (s == NP)
{
s = NP;
goto label_8878;
}
else 
{
goto label_8852;
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
int __tmp_132 = devobj;
int __tmp_133 = pirp;
int DeviceObject = __tmp_132;
int Irp = __tmp_133;
int deviceExtension__Active = __VERIFIER_nondet_int() ;
deviceExtension__Active = __VERIFIER_nondet_int();
int status4 ;
if (!(deviceExtension__Active == 2))
{
if (!(deviceExtension__Active == 3))
{
if (!(deviceExtension__Active == 1))
{
if (!(deviceExtension__Active == 7))
{
deviceExtension__Active = 0;
{
int __tmp_134 = DeviceObject;
int __tmp_135 = Irp;
int DeviceObject = __tmp_134;
int Irp = __tmp_135;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
if (s == NP)
{
s = SKIP1;
goto label_2529;
}
else 
{
{
__VERIFIER_error();
}
label_2529:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_136 = deviceExtension__TargetDeviceObject;
int __tmp_137 = Irp;
int DeviceObject = __tmp_136;
int Irp = __tmp_137;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_138 = DeviceObject;
int __tmp_139 = Irp;
int __tmp_140 = lcontext;
int DeviceObject = __tmp_138;
int Irp = __tmp_139;
int Context = __tmp_140;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_2559:; 
if (myStatus >= 0)
{
{
int __tmp_141 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_141;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_2582;
}
else 
{
label_2582:; 
}
goto label_2575;
}
}
else 
{
label_2575:; 
 __return_2590 = myStatus;
}
compRetStatus = __return_2590;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2549;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_2608;
}
else 
{
{
__VERIFIER_error();
}
label_2608:; 
}
goto label_2549;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_2559;
}
else 
{
pended = 1;
goto label_2559;
}
}
}
}
else 
{
label_2549:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_2636;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_2636:; 
goto label_2620;
}
else 
{
returnVal2 = -1073741823;
goto label_2636;
}
}
}
else 
{
returnVal2 = 259;
label_2620:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2661;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_2670:; 
goto label_2661;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2670;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2661;
}
else 
{
{
__VERIFIER_error();
}
label_2661:; 
 __return_2678 = returnVal2;
}
tmp = __return_2678;
 __return_2680 = tmp;
}
status4 = __return_2680;
label_2682:; 
 __return_8818 = status4;
}
status10 = __return_8818;
goto label_601;
}
}
}
}
}
}
else 
{
{
int __tmp_142 = DeviceObject;
int __tmp_143 = Irp;
int DeviceObject = __tmp_142;
int Irp = __tmp_143;
int currentIrpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int() ;
currentIrpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int irpSp__Control ;
int tmp ;
int tmp___0 ;
int __cil_tmp8 ;
int __cil_tmp9 ;
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
__cil_tmp8 = 56;
__cil_tmp9 = 16384;
__cil_tmp10 = 131072;
__cil_tmp11 = 147456;
__cil_tmp12 = 147512;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp12)
{
if (s != NP)
{
{
__VERIFIER_error();
}
goto label_2890;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
goto label_2890;
}
else 
{
compRegistered = 1;
routine = 0;
label_2890:; 
irpSp__Control = 224;
{
int __tmp_144 = deviceExtension__TargetDeviceObject;
int __tmp_145 = Irp;
int DeviceObject = __tmp_144;
int Irp = __tmp_145;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_146 = DeviceObject;
int __tmp_147 = Irp;
int __tmp_148 = lcontext;
int DeviceObject = __tmp_146;
int Irp = __tmp_147;
int Context = __tmp_148;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_2928:; 
if (myStatus >= 0)
{
{
int __tmp_149 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_149;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_2951;
}
else 
{
label_2951:; 
}
goto label_2944;
}
}
else 
{
label_2944:; 
 __return_2959 = myStatus;
}
compRetStatus = __return_2959;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2918;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_2977;
}
else 
{
{
__VERIFIER_error();
}
label_2977:; 
}
goto label_2918;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_2928;
}
else 
{
pended = 1;
goto label_2928;
}
}
}
}
else 
{
label_2918:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_3005;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_3005:; 
goto label_2989;
}
else 
{
returnVal2 = -1073741823;
goto label_3005;
}
}
}
else 
{
returnVal2 = 259;
label_2989:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3030;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_3039:; 
goto label_3030;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3039;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3030;
}
else 
{
{
__VERIFIER_error();
}
label_3030:; 
 __return_3047 = returnVal2;
}
tmp = __return_3047;
 __return_3049 = tmp;
}
status4 = __return_3049;
label_3050:; 
label_3052:; 
goto label_2682;
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
int __tmp_150 = DeviceObject;
int __tmp_151 = Irp;
int DeviceObject = __tmp_150;
int Irp = __tmp_151;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
if (s == NP)
{
s = SKIP1;
goto label_2728;
}
else 
{
{
__VERIFIER_error();
}
label_2728:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_152 = deviceExtension__TargetDeviceObject;
int __tmp_153 = Irp;
int DeviceObject = __tmp_152;
int Irp = __tmp_153;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_154 = DeviceObject;
int __tmp_155 = Irp;
int __tmp_156 = lcontext;
int DeviceObject = __tmp_154;
int Irp = __tmp_155;
int Context = __tmp_156;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_2758:; 
if (myStatus >= 0)
{
{
int __tmp_157 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_157;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_2781;
}
else 
{
label_2781:; 
}
goto label_2774;
}
}
else 
{
label_2774:; 
 __return_2789 = myStatus;
}
compRetStatus = __return_2789;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2748;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_2807;
}
else 
{
{
__VERIFIER_error();
}
label_2807:; 
}
goto label_2748;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_2758;
}
else 
{
pended = 1;
goto label_2758;
}
}
}
}
else 
{
label_2748:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_2835;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_2835:; 
goto label_2819;
}
else 
{
returnVal2 = -1073741823;
goto label_2835;
}
}
}
else 
{
returnVal2 = 259;
label_2819:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2860;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_2869:; 
goto label_2860;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2869;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2860;
}
else 
{
{
__VERIFIER_error();
}
label_2860:; 
 __return_2877 = returnVal2;
}
tmp = __return_2877;
 __return_2879 = tmp;
}
tmp___0 = __return_2879;
 __return_2881 = tmp___0;
}
status4 = __return_2881;
goto label_3050;
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
int __tmp_158 = DeviceObject;
int __tmp_159 = Irp;
int DeviceObject = __tmp_158;
int Irp = __tmp_159;
int currentIrpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int() ;
currentIrpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int();
int Irp__IoStatus__Information ;
int deviceExtension__PlayActive ;
int srb__CdbLength ;
int srb__TimeOutValue ;
int Irp__IoStatus__Status ;
int status8 ;
int deviceExtension = __VERIFIER_nondet_int() ;
deviceExtension = __VERIFIER_nondet_int();
int srb = __VERIFIER_nondet_int() ;
srb = __VERIFIER_nondet_int();
int tmp ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
int __cil_tmp18 ;
__cil_tmp13 = 8;
__cil_tmp14 = 16384;
__cil_tmp15 = 131072;
__cil_tmp16 = 147456;
__cil_tmp17 = 147464;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp17)
{
Irp__IoStatus__Information = 0;
deviceExtension__PlayActive = 0;
srb__CdbLength = 12;
srb__TimeOutValue = 10;
{
int __tmp_160 = deviceExtension;
int __tmp_161 = srb;
int __tmp_162 = 0;
int __tmp_163 = 0;
int Extension = __tmp_160;
int Srb = __tmp_161;
int Buffer = __tmp_162;
int BufferLength = __tmp_163;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_3434 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_3326:; 
 __return_3430 = status1;
}
else 
{
{
int __tmp_168 = event;
int __tmp_169 = Executive;
int __tmp_170 = KernelMode;
int __tmp_171 = 0;
int __tmp_172 = 0;
int Object = __tmp_168;
int WaitReason = __tmp_169;
int WaitMode = __tmp_170;
int Alertable = __tmp_171;
int Timeout = __tmp_172;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_3383;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_3393;
}
}
else 
{
label_3383:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3393;
}
else 
{
label_3393:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_3423 = 0;
goto label_3419;
}
else 
{
 __return_3419 = -1073741823;
label_3419:; 
}
status1 = ioStatus__Status;
goto label_3326;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_3393;
}
}
}
}
status8 = __return_3430;
goto label_3435;
}
status8 = __return_3434;
label_3435:; 
if (status8 < 0)
{
Irp__IoStatus__Status = status8;
myStatus = status8;
{
int __tmp_164 = Irp;
int __tmp_165 = 0;
int Irp = __tmp_164;
int PriorityBoost = __tmp_165;
if (s == NP)
{
s = DC;
goto label_3474;
}
else 
{
{
__VERIFIER_error();
}
label_3474:; 
}
 __return_3479 = status8;
goto label_3460;
}
}
else 
{
Irp__IoStatus__Status = status8;
myStatus = status8;
{
int __tmp_166 = Irp;
int __tmp_167 = 0;
int Irp = __tmp_166;
int PriorityBoost = __tmp_167;
if (s == NP)
{
s = DC;
goto label_3455;
}
else 
{
{
__VERIFIER_error();
}
label_3455:; 
}
 __return_3460 = status8;
label_3460:; 
}
status4 = __return_3460;
label_3480:; 
goto label_3052;
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_3432 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_3329:; 
 __return_3429 = status1;
}
else 
{
{
int __tmp_173 = event;
int __tmp_174 = Executive;
int __tmp_175 = KernelMode;
int __tmp_176 = 0;
int __tmp_177 = 0;
int Object = __tmp_173;
int WaitReason = __tmp_174;
int WaitMode = __tmp_175;
int Alertable = __tmp_176;
int Timeout = __tmp_177;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_3336;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_3346;
}
}
else 
{
label_3336:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3346;
}
else 
{
label_3346:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_3376 = 0;
goto label_3372;
}
else 
{
 __return_3372 = -1073741823;
label_3372:; 
}
status1 = ioStatus__Status;
goto label_3329;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_3346;
}
}
}
}
status8 = __return_3429;
goto label_3435;
}
status8 = __return_3432;
goto label_3435;
}
}
}
else 
{
{
int __tmp_178 = DeviceObject;
int __tmp_179 = Irp;
int DeviceObject = __tmp_178;
int Irp = __tmp_179;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
if (s == NP)
{
s = SKIP1;
goto label_3105;
}
else 
{
{
__VERIFIER_error();
}
label_3105:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_180 = deviceExtension__TargetDeviceObject;
int __tmp_181 = Irp;
int DeviceObject = __tmp_180;
int Irp = __tmp_181;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_182 = DeviceObject;
int __tmp_183 = Irp;
int __tmp_184 = lcontext;
int DeviceObject = __tmp_182;
int Irp = __tmp_183;
int Context = __tmp_184;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_3135:; 
if (myStatus >= 0)
{
{
int __tmp_185 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_185;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_3158;
}
else 
{
label_3158:; 
}
goto label_3151;
}
}
else 
{
label_3151:; 
 __return_3166 = myStatus;
}
compRetStatus = __return_3166;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_3125;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_3184;
}
else 
{
{
__VERIFIER_error();
}
label_3184:; 
}
goto label_3125;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_3135;
}
else 
{
pended = 1;
goto label_3135;
}
}
}
}
else 
{
label_3125:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_3212;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_3212:; 
goto label_3196;
}
else 
{
returnVal2 = -1073741823;
goto label_3212;
}
}
}
else 
{
returnVal2 = 259;
label_3196:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3237;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_3246:; 
goto label_3237;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3246;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3237;
}
else 
{
{
__VERIFIER_error();
}
label_3237:; 
 __return_3254 = returnVal2;
}
tmp = __return_3254;
 __return_3256 = tmp;
}
tmp = __return_3256;
 __return_3258 = tmp;
}
status4 = __return_3258;
goto label_3480;
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
int __tmp_186 = DeviceObject;
int __tmp_187 = Irp;
int DeviceObject = __tmp_186;
int Irp = __tmp_187;
int currentIrpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int() ;
currentIrpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int();
int currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength = __VERIFIER_nondet_int() ;
currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength = __VERIFIER_nondet_int();
int currentIrpStack__Parameters__DeviceIoControl__InputBufferLength = __VERIFIER_nondet_int() ;
currentIrpStack__Parameters__DeviceIoControl__InputBufferLength = __VERIFIER_nondet_int();
int TrackData__0 = __VERIFIER_nondet_int() ;
TrackData__0 = __VERIFIER_nondet_int();
int Irp__IoStatus__Information ;
int srb__TimeOutValue ;
int srb__CdbLength ;
int sizeof__CDROM_TOC = __VERIFIER_nondet_int() ;
sizeof__CDROM_TOC = __VERIFIER_nondet_int();
int cdaudioDataOut__LastTrack = __VERIFIER_nondet_int() ;
cdaudioDataOut__LastTrack = __VERIFIER_nondet_int();
int cdaudioDataOut__FirstTrack = __VERIFIER_nondet_int() ;
cdaudioDataOut__FirstTrack = __VERIFIER_nondet_int();
int sizeof__CDROM_PLAY_AUDIO_MSF = __VERIFIER_nondet_int() ;
sizeof__CDROM_PLAY_AUDIO_MSF = __VERIFIER_nondet_int();
int sizeof__CDROM_SEEK_AUDIO_MSF = __VERIFIER_nondet_int() ;
sizeof__CDROM_SEEK_AUDIO_MSF = __VERIFIER_nondet_int();
int deviceExtension__Paused = __VERIFIER_nondet_int() ;
deviceExtension__Paused = __VERIFIER_nondet_int();
int deviceExtension__PlayActive ;
int sizeof__SUB_Q_CHANNEL_DATA = __VERIFIER_nondet_int() ;
sizeof__SUB_Q_CHANNEL_DATA = __VERIFIER_nondet_int();
int sizeof__SUB_Q_CURRENT_POSITION = __VERIFIER_nondet_int() ;
sizeof__SUB_Q_CURRENT_POSITION = __VERIFIER_nondet_int();
int deviceExtension = __VERIFIER_nondet_int() ;
deviceExtension = __VERIFIER_nondet_int();
int srb = __VERIFIER_nondet_int() ;
srb = __VERIFIER_nondet_int();
int status7 ;
int i = __VERIFIER_nondet_int() ;
i = __VERIFIER_nondet_int();
int bytesTransfered ;
int Toc = __VERIFIER_nondet_int() ;
Toc = __VERIFIER_nondet_int();
int tmp ;
int tracksToReturn ;
int tracksOnCd ;
int tracksInBuffer ;
int SubQPtr = __VERIFIER_nondet_int() ;
SubQPtr = __VERIFIER_nondet_int();
int userPtr__Format = __VERIFIER_nondet_int() ;
userPtr__Format = __VERIFIER_nondet_int();
int SubQPtr___0 = __VERIFIER_nondet_int() ;
SubQPtr___0 = __VERIFIER_nondet_int();
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int __cil_tmp35 ;
int __cil_tmp36 ;
int __cil_tmp37 ;
int __cil_tmp38 ;
int __cil_tmp39 ;
int __cil_tmp40 ;
int __cil_tmp41 ;
int __cil_tmp42 ;
int __cil_tmp43 ;
int __cil_tmp44 ;
int __cil_tmp45 ;
int __cil_tmp46 ;
int __cil_tmp47 ;
int __cil_tmp48 ;
int __cil_tmp49 ;
int __cil_tmp50 ;
int __cil_tmp51 ;
int __cil_tmp52 ;
int __cil_tmp53 ;
int __cil_tmp54 ;
int __cil_tmp55 ;
int __cil_tmp56 ;
int __cil_tmp57 ;
int __cil_tmp58 ;
int __cil_tmp59 ;
int __cil_tmp60 ;
int __cil_tmp61 ;
int __cil_tmp62 ;
int __cil_tmp63 ;
int __cil_tmp64 ;
int __cil_tmp65 ;
int __cil_tmp66 ;
int __cil_tmp67 ;
int __cil_tmp68 ;
int __cil_tmp69 ;
int __cil_tmp70 ;
int __cil_tmp71 ;
int __cil_tmp72 ;
int __cil_tmp73 ;
int __cil_tmp74 ;
int __cil_tmp75 ;
int __cil_tmp76 ;
int __cil_tmp77 ;
int __cil_tmp78 ;
int __cil_tmp79 ;
int __cil_tmp80 ;
int __cil_tmp81 ;
int __cil_tmp82 ;
int __cil_tmp83 ;
int __cil_tmp84 ;
int __cil_tmp85 ;
int __cil_tmp86 ;
int __cil_tmp87 ;
int __cil_tmp88 ;
int __cil_tmp89 ;
int __cil_tmp90 ;
int __cil_tmp91 ;
int __cil_tmp92 ;
unsigned long __cil_tmp93 ;
int __cil_tmp94 ;
unsigned long __cil_tmp95 ;
unsigned long __cil_tmp96 ;
unsigned long __cil_tmp97 ;
int __cil_tmp98 ;
int __cil_tmp99 ;
int __cil_tmp100 ;
int __cil_tmp101 ;
int __cil_tmp102 ;
int __cil_tmp103 ;
unsigned long __cil_tmp104 ;
unsigned long __cil_tmp105 ;
unsigned long __cil_tmp106 ;
unsigned long __cil_tmp107 ;
int __cil_tmp108 ;
unsigned long __cil_tmp109 ;
int __cil_tmp110 ;
unsigned long __cil_tmp111 ;
unsigned long __cil_tmp112 ;
unsigned long __cil_tmp113 ;
unsigned long __cil_tmp114 ;
unsigned long __cil_tmp115 ;
unsigned long __cil_tmp116 ;
__cil_tmp35 = 16384;
__cil_tmp36 = 131072;
__cil_tmp37 = 147456;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp37)
{
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength < TrackData__0)
{
status7 = -1073741789;
Irp__IoStatus__Information = 0;
label_6397:; 
__cil_tmp116 = (unsigned long)status7;
label_6411:; 
if (!(__cil_tmp116 == -2147483626))
{
label_6418:; 
myStatus = status7;
{
int __tmp_188 = Irp;
int __tmp_189 = 0;
int Irp = __tmp_188;
int PriorityBoost = __tmp_189;
if (s == NP)
{
s = DC;
goto label_6454;
}
else 
{
{
__VERIFIER_error();
}
label_6454:; 
}
 __return_6459 = status7;
}
status4 = __return_6459;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
label_6421:; 
goto label_6418;
}
}
else 
{
{
int __tmp_190 = DeviceObject;
int DeviceObject = __tmp_190;
int deviceExtension__PlayActive = __VERIFIER_nondet_int() ;
deviceExtension__PlayActive = __VERIFIER_nondet_int();
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int currentBuffer__Header__AudioStatus = __VERIFIER_nondet_int() ;
currentBuffer__Header__AudioStatus = __VERIFIER_nondet_int();
int irp_CdAudioIsPlayActive = __VERIFIER_nondet_int() ;
irp_CdAudioIsPlayActive = __VERIFIER_nondet_int();
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int status5 = __VERIFIER_nondet_int() ;
status5 = __VERIFIER_nondet_int();
int currentBuffer = __VERIFIER_nondet_int() ;
currentBuffer = __VERIFIER_nondet_int();
int returnValue ;
long __cil_tmp10 ;
int __cil_tmp11 ;
if (deviceExtension__PlayActive == 0)
{
 __return_6107 = 0;
goto label_6103;
}
else 
{
if (currentBuffer == 0)
{
 __return_6105 = 0;
goto label_6103;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_6103 = 0;
label_6103:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_6035:; 
if (status5 < 0)
{
 __return_6101 = 0;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_6095:; 
 __return_6099 = returnValue;
}
else 
{
returnValue = 1;
goto label_6095;
}
tmp = __return_6099;
goto label_6108;
}
tmp = __return_6101;
goto label_6108;
}
else 
{
{
int __tmp_211 = event;
int __tmp_212 = Suspended;
int __tmp_213 = KernelMode;
int __tmp_214 = 0;
int __tmp_215 = 0;
int Object = __tmp_211;
int WaitReason = __tmp_212;
int WaitMode = __tmp_213;
int Alertable = __tmp_214;
int Timeout = __tmp_215;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_6042;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6052;
}
}
else 
{
label_6042:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6052;
}
else 
{
label_6052:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6082 = 0;
goto label_6078;
}
else 
{
 __return_6078 = -1073741823;
label_6078:; 
}
status5 = ioStatus__Status;
goto label_6035;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6052;
}
}
}
}
}
tmp = __return_6103;
label_6108:; 
if (!(tmp == 0))
{
status7 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_6397;
}
else 
{
if (Toc == 0)
{
status7 = -1073741670;
Irp__IoStatus__Information = 0;
__cil_tmp93 = (unsigned long)status7;
if (!(__cil_tmp93 == -2147483626))
{
label_6369:; 
myStatus = status7;
{
int __tmp_191 = Irp;
int __tmp_192 = 0;
int Irp = __tmp_191;
int PriorityBoost = __tmp_192;
if (s == NP)
{
s = DC;
goto label_6386;
}
else 
{
{
__VERIFIER_error();
}
label_6386:; 
}
 __return_6391 = status7;
}
status4 = __return_6391;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_6369;
}
}
else 
{
srb__TimeOutValue = 10;
srb__CdbLength = 10;
{
int __tmp_193 = deviceExtension;
int __tmp_194 = srb;
int __tmp_195 = Toc;
int __tmp_196 = sizeof__CDROM_TOC;
int Extension = __tmp_193;
int Srb = __tmp_194;
int Buffer = __tmp_195;
int BufferLength = __tmp_196;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_6293 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6185:; 
 __return_6289 = status1;
}
else 
{
{
int __tmp_201 = event;
int __tmp_202 = Executive;
int __tmp_203 = KernelMode;
int __tmp_204 = 0;
int __tmp_205 = 0;
int Object = __tmp_201;
int WaitReason = __tmp_202;
int WaitMode = __tmp_203;
int Alertable = __tmp_204;
int Timeout = __tmp_205;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_6242;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6252;
}
}
else 
{
label_6242:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6252;
}
else 
{
label_6252:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6282 = 0;
goto label_6278;
}
else 
{
 __return_6278 = -1073741823;
label_6278:; 
}
status1 = ioStatus__Status;
goto label_6185;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6252;
}
}
}
}
status7 = __return_6289;
goto label_6294;
}
status7 = __return_6293;
label_6294:; 
if (status7 < 0)
{
__cil_tmp95 = (unsigned long)status7;
if (__cil_tmp95 != -1073741764)
{
__cil_tmp96 = (unsigned long)status7;
if (__cil_tmp96 != -1073741764)
{
__cil_tmp97 = (unsigned long)status7;
if (!(__cil_tmp97 == -2147483626))
{
label_6340:; 
myStatus = status7;
{
int __tmp_197 = Irp;
int __tmp_198 = 0;
int Irp = __tmp_197;
int PriorityBoost = __tmp_198;
if (s == NP)
{
s = DC;
goto label_6357;
}
else 
{
{
__VERIFIER_error();
}
label_6357:; 
}
 __return_6362 = status7;
}
status4 = __return_6362;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_6340;
}
}
else 
{
goto label_6309;
}
}
else 
{
status7 = 0;
label_6309:; 
goto label_6302;
}
}
else 
{
status7 = 0;
label_6302:; 
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength > sizeof__CDROM_TOC)
{
bytesTransfered = sizeof__CDROM_TOC;
goto label_6319;
}
else 
{
bytesTransfered = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
label_6319:; 
__cil_tmp98 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp98 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength - TrackData__0;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_6328;
}
else 
{
tracksToReturn = tracksOnCd;
label_6328:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_6332;
}
else 
{
label_6332:; 
__cil_tmp116 = (unsigned long)status7;
if (!(__cil_tmp116 == -2147483626))
{
myStatus = status7;
{
int __tmp_199 = Irp;
int __tmp_200 = 0;
int Irp = __tmp_199;
int PriorityBoost = __tmp_200;
if (s == NP)
{
s = DC;
goto label_6438;
}
else 
{
{
__VERIFIER_error();
}
label_6438:; 
}
 __return_6460 = status7;
}
status4 = __return_6460;
label_6461:; 
goto label_3052;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_6421;
}
}
}
}
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_6291 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6188:; 
 __return_6288 = status1;
}
else 
{
{
int __tmp_206 = event;
int __tmp_207 = Executive;
int __tmp_208 = KernelMode;
int __tmp_209 = 0;
int __tmp_210 = 0;
int Object = __tmp_206;
int WaitReason = __tmp_207;
int WaitMode = __tmp_208;
int Alertable = __tmp_209;
int Timeout = __tmp_210;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_6195;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6205;
}
}
else 
{
label_6195:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6205;
}
else 
{
label_6205:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6235 = 0;
goto label_6231;
}
else 
{
 __return_6231 = -1073741823;
label_6231:; 
}
status1 = ioStatus__Status;
goto label_6188;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6205;
}
}
}
}
status7 = __return_6288;
goto label_6294;
}
status7 = __return_6291;
goto label_6294;
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
__cil_tmp38 = 24;
__cil_tmp39 = 16384;
__cil_tmp40 = 131072;
__cil_tmp41 = 147456;
__cil_tmp42 = 147480;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp42)
{
Irp__IoStatus__Information = 0;
label_5599:; 
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_216 = deviceExtension;
int __tmp_217 = srb;
int __tmp_218 = 0;
int __tmp_219 = 0;
int Extension = __tmp_216;
int Srb = __tmp_217;
int Buffer = __tmp_218;
int BufferLength = __tmp_219;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_5773 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5665:; 
 __return_5769 = status1;
}
else 
{
{
int __tmp_236 = event;
int __tmp_237 = Executive;
int __tmp_238 = KernelMode;
int __tmp_239 = 0;
int __tmp_240 = 0;
int Object = __tmp_236;
int WaitReason = __tmp_237;
int WaitMode = __tmp_238;
int Alertable = __tmp_239;
int Timeout = __tmp_240;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5722;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5732;
}
}
else 
{
label_5722:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5732;
}
else 
{
label_5732:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5762 = 0;
goto label_5758;
}
else 
{
 __return_5758 = -1073741823;
label_5758:; 
}
status1 = ioStatus__Status;
goto label_5665;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5732;
}
}
}
}
status7 = __return_5769;
goto label_5774;
}
status7 = __return_5773;
label_5774:; 
__cil_tmp99 = 8;
__cil_tmp100 = 16384;
__cil_tmp101 = 131072;
__cil_tmp102 = 147456;
__cil_tmp103 = 147464;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp103)
{
__cil_tmp104 = (unsigned long)status7;
if (!(__cil_tmp104 == -2147483626))
{
label_5975:; 
myStatus = status7;
{
int __tmp_220 = Irp;
int __tmp_221 = 0;
int Irp = __tmp_220;
int PriorityBoost = __tmp_221;
if (s == NP)
{
s = DC;
goto label_5992;
}
else 
{
{
__VERIFIER_error();
}
label_5992:; 
}
 __return_5997 = status7;
}
status4 = __return_5997;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5975;
}
}
else 
{
if (currentIrpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CDROM_PLAY_AUDIO_MSF)
{
status7 = -1073741820;
goto label_6397;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_222 = deviceExtension;
int __tmp_223 = srb;
int __tmp_224 = 0;
int __tmp_225 = 0;
int Extension = __tmp_222;
int Srb = __tmp_223;
int Buffer = __tmp_224;
int BufferLength = __tmp_225;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_5964 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5856:; 
 __return_5960 = status1;
}
else 
{
{
int __tmp_226 = event;
int __tmp_227 = Executive;
int __tmp_228 = KernelMode;
int __tmp_229 = 0;
int __tmp_230 = 0;
int Object = __tmp_226;
int WaitReason = __tmp_227;
int WaitMode = __tmp_228;
int Alertable = __tmp_229;
int Timeout = __tmp_230;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5913;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5923;
}
}
else 
{
label_5913:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5923;
}
else 
{
label_5923:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5953 = 0;
goto label_5949;
}
else 
{
 __return_5949 = -1073741823;
label_5949:; 
}
status1 = ioStatus__Status;
goto label_5856;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5923;
}
}
}
}
status7 = __return_5960;
goto label_5965;
}
status7 = __return_5964;
label_5965:; 
__cil_tmp116 = (unsigned long)status7;
goto label_6411;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_5962 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5859:; 
 __return_5959 = status1;
}
else 
{
{
int __tmp_231 = event;
int __tmp_232 = Executive;
int __tmp_233 = KernelMode;
int __tmp_234 = 0;
int __tmp_235 = 0;
int Object = __tmp_231;
int WaitReason = __tmp_232;
int WaitMode = __tmp_233;
int Alertable = __tmp_234;
int Timeout = __tmp_235;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5866;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5876;
}
}
else 
{
label_5866:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5876;
}
else 
{
label_5876:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5906 = 0;
goto label_5902;
}
else 
{
 __return_5902 = -1073741823;
label_5902:; 
}
status1 = ioStatus__Status;
goto label_5859;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5876;
}
}
}
}
status7 = __return_5959;
goto label_5965;
}
status7 = __return_5962;
goto label_5965;
}
}
}
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_5771 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5668:; 
 __return_5768 = status1;
}
else 
{
{
int __tmp_241 = event;
int __tmp_242 = Executive;
int __tmp_243 = KernelMode;
int __tmp_244 = 0;
int __tmp_245 = 0;
int Object = __tmp_241;
int WaitReason = __tmp_242;
int WaitMode = __tmp_243;
int Alertable = __tmp_244;
int Timeout = __tmp_245;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5675;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5685;
}
}
else 
{
label_5675:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5685;
}
else 
{
label_5685:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5715 = 0;
goto label_5711;
}
else 
{
 __return_5711 = -1073741823;
label_5711:; 
}
status1 = ioStatus__Status;
goto label_5668;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5685;
}
}
}
}
status7 = __return_5768;
goto label_5774;
}
status7 = __return_5771;
goto label_5774;
}
}
}
else 
{
__cil_tmp43 = 8;
__cil_tmp44 = 16384;
__cil_tmp45 = 131072;
__cil_tmp46 = 147456;
__cil_tmp47 = 147464;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp47)
{
Irp__IoStatus__Information = 0;
goto label_5599;
}
else 
{
__cil_tmp48 = 4;
__cil_tmp49 = 16384;
__cil_tmp50 = 131072;
__cil_tmp51 = 147456;
__cil_tmp52 = 147460;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp52)
{
Irp__IoStatus__Information = 0;
if (currentIrpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CDROM_SEEK_AUDIO_MSF)
{
status7 = -1073741820;
goto label_6397;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_246 = deviceExtension;
int __tmp_247 = srb;
int __tmp_248 = 0;
int __tmp_249 = 0;
int Extension = __tmp_246;
int Srb = __tmp_247;
int Buffer = __tmp_248;
int BufferLength = __tmp_249;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_5575 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5467:; 
 __return_5571 = status1;
}
else 
{
{
int __tmp_250 = event;
int __tmp_251 = Executive;
int __tmp_252 = KernelMode;
int __tmp_253 = 0;
int __tmp_254 = 0;
int Object = __tmp_250;
int WaitReason = __tmp_251;
int WaitMode = __tmp_252;
int Alertable = __tmp_253;
int Timeout = __tmp_254;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5524;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5534;
}
}
else 
{
label_5524:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5534;
}
else 
{
label_5534:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5564 = 0;
goto label_5560;
}
else 
{
 __return_5560 = -1073741823;
label_5560:; 
}
status1 = ioStatus__Status;
goto label_5467;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5534;
}
}
}
}
status7 = __return_5571;
goto label_5576;
}
status7 = __return_5575;
label_5576:; 
if (status7 < 0)
{
__cil_tmp105 = (unsigned long)status7;
if (!(__cil_tmp105 == -1073741808))
{
goto label_5581;
}
else 
{
status7 = -1073741803;
goto label_5581;
}
}
else 
{
label_5581:; 
goto label_6397;
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_5573 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5470:; 
 __return_5570 = status1;
}
else 
{
{
int __tmp_255 = event;
int __tmp_256 = Executive;
int __tmp_257 = KernelMode;
int __tmp_258 = 0;
int __tmp_259 = 0;
int Object = __tmp_255;
int WaitReason = __tmp_256;
int WaitMode = __tmp_257;
int Alertable = __tmp_258;
int Timeout = __tmp_259;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5477;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5487;
}
}
else 
{
label_5477:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5487;
}
else 
{
label_5487:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5517 = 0;
goto label_5513;
}
else 
{
 __return_5513 = -1073741823;
label_5513:; 
}
status1 = ioStatus__Status;
goto label_5470;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5487;
}
}
}
}
status7 = __return_5570;
goto label_5576;
}
status7 = __return_5573;
goto label_5576;
}
}
}
}
else 
{
__cil_tmp53 = 12;
__cil_tmp54 = 16384;
__cil_tmp55 = 131072;
__cil_tmp56 = 147456;
__cil_tmp57 = 147468;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp57)
{
Irp__IoStatus__Information = 0;
if (SubQPtr == 0)
{
status7 = -1073741670;
__cil_tmp106 = (unsigned long)status7;
if (!(__cil_tmp106 == -2147483626))
{
label_5375:; 
myStatus = status7;
{
int __tmp_260 = Irp;
int __tmp_261 = 0;
int Irp = __tmp_260;
int PriorityBoost = __tmp_261;
if (s == NP)
{
s = DC;
goto label_5392;
}
else 
{
{
__VERIFIER_error();
}
label_5392:; 
}
 __return_5397 = status7;
}
status4 = __return_5397;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5375;
}
}
else 
{
if (!(deviceExtension__Paused == 1))
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_262 = deviceExtension;
int __tmp_263 = srb;
int __tmp_264 = SubQPtr;
int __tmp_265 = sizeof__SUB_Q_CHANNEL_DATA;
int Extension = __tmp_262;
int Srb = __tmp_263;
int Buffer = __tmp_264;
int BufferLength = __tmp_265;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_5108 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5000:; 
 __return_5104 = status1;
}
else 
{
{
int __tmp_284 = event;
int __tmp_285 = Executive;
int __tmp_286 = KernelMode;
int __tmp_287 = 0;
int __tmp_288 = 0;
int Object = __tmp_284;
int WaitReason = __tmp_285;
int WaitMode = __tmp_286;
int Alertable = __tmp_287;
int Timeout = __tmp_288;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5057;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5067;
}
}
else 
{
label_5057:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5067;
}
else 
{
label_5067:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5097 = 0;
goto label_5093;
}
else 
{
 __return_5093 = -1073741823;
label_5093:; 
}
status1 = ioStatus__Status;
goto label_5000;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5067;
}
}
}
}
status7 = __return_5104;
goto label_5109;
}
status7 = __return_5108;
label_5109:; 
if (status7 < 0)
{
__cil_tmp109 = (unsigned long)status7;
if (!(__cil_tmp109 == -2147483626))
{
label_5321:; 
myStatus = status7;
{
int __tmp_266 = Irp;
int __tmp_267 = 0;
int Irp = __tmp_266;
int PriorityBoost = __tmp_267;
if (s == NP)
{
s = DC;
goto label_5338;
}
else 
{
{
__VERIFIER_error();
}
label_5338:; 
}
 __return_5343 = status7;
}
status4 = __return_5343;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5321;
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_268 = deviceExtension;
int __tmp_269 = srb;
int __tmp_270 = 0;
int __tmp_271 = 0;
int Extension = __tmp_268;
int Srb = __tmp_269;
int Buffer = __tmp_270;
int BufferLength = __tmp_271;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_5286 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5178:; 
 __return_5282 = status1;
}
else 
{
{
int __tmp_274 = event;
int __tmp_275 = Executive;
int __tmp_276 = KernelMode;
int __tmp_277 = 0;
int __tmp_278 = 0;
int Object = __tmp_274;
int WaitReason = __tmp_275;
int WaitMode = __tmp_276;
int Alertable = __tmp_277;
int Timeout = __tmp_278;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5235;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5245;
}
}
else 
{
label_5235:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5245;
}
else 
{
label_5245:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5275 = 0;
goto label_5271;
}
else 
{
 __return_5271 = -1073741823;
label_5271:; 
}
status1 = ioStatus__Status;
goto label_5178;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5245;
}
}
}
}
status7 = __return_5282;
goto label_5287;
}
status7 = __return_5286;
label_5287:; 
if (status7 < 0)
{
__cil_tmp111 = (unsigned long)status7;
if (!(__cil_tmp111 == -2147483626))
{
label_5296:; 
myStatus = status7;
{
int __tmp_272 = Irp;
int __tmp_273 = 0;
int Irp = __tmp_272;
int PriorityBoost = __tmp_273;
if (s == NP)
{
s = DC;
goto label_5313;
}
else 
{
{
__VERIFIER_error();
}
label_5313:; 
}
 __return_5318 = status7;
}
status4 = __return_5318;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5296;
}
}
else 
{
goto label_6397;
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_5284 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5181:; 
 __return_5281 = status1;
}
else 
{
{
int __tmp_279 = event;
int __tmp_280 = Executive;
int __tmp_281 = KernelMode;
int __tmp_282 = 0;
int __tmp_283 = 0;
int Object = __tmp_279;
int WaitReason = __tmp_280;
int WaitMode = __tmp_281;
int Alertable = __tmp_282;
int Timeout = __tmp_283;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5188;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5198;
}
}
else 
{
label_5188:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5198;
}
else 
{
label_5198:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5228 = 0;
goto label_5224;
}
else 
{
 __return_5224 = -1073741823;
label_5224:; 
}
status1 = ioStatus__Status;
goto label_5181;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5198;
}
}
}
}
status7 = __return_5281;
goto label_5287;
}
status7 = __return_5284;
goto label_5287;
}
}
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_5106 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5003:; 
 __return_5103 = status1;
}
else 
{
{
int __tmp_289 = event;
int __tmp_290 = Executive;
int __tmp_291 = KernelMode;
int __tmp_292 = 0;
int __tmp_293 = 0;
int Object = __tmp_289;
int WaitReason = __tmp_290;
int WaitMode = __tmp_291;
int Alertable = __tmp_292;
int Timeout = __tmp_293;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5010;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5020;
}
}
else 
{
label_5010:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5020;
}
else 
{
label_5020:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5050 = 0;
goto label_5046;
}
else 
{
 __return_5046 = -1073741823;
label_5046:; 
}
status1 = ioStatus__Status;
goto label_5003;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5020;
}
}
}
}
status7 = __return_5103;
goto label_5109;
}
status7 = __return_5106;
goto label_5109;
}
}
}
else 
{
status7 = 0;
__cil_tmp107 = (unsigned long)status7;
if (!(__cil_tmp107 == -2147483626))
{
label_5348:; 
myStatus = status7;
{
int __tmp_294 = Irp;
int __tmp_295 = 0;
int Irp = __tmp_294;
int PriorityBoost = __tmp_295;
if (s == NP)
{
s = DC;
goto label_5365;
}
else 
{
{
__VERIFIER_error();
}
label_5365:; 
}
 __return_5370 = status7;
}
status4 = __return_5370;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5348;
}
}
}
}
else 
{
__cil_tmp58 = 16;
__cil_tmp59 = 16384;
__cil_tmp60 = 131072;
__cil_tmp61 = 147456;
__cil_tmp62 = 147472;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp62)
{
Irp__IoStatus__Information = 0;
if (!(deviceExtension__Paused == 0))
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_296 = deviceExtension;
int __tmp_297 = srb;
int __tmp_298 = 0;
int __tmp_299 = 0;
int Extension = __tmp_296;
int Srb = __tmp_297;
int Buffer = __tmp_298;
int BufferLength = __tmp_299;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_4887 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4779:; 
 __return_4883 = status1;
}
else 
{
{
int __tmp_300 = event;
int __tmp_301 = Executive;
int __tmp_302 = KernelMode;
int __tmp_303 = 0;
int __tmp_304 = 0;
int Object = __tmp_300;
int WaitReason = __tmp_301;
int WaitMode = __tmp_302;
int Alertable = __tmp_303;
int Timeout = __tmp_304;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4836;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4846;
}
}
else 
{
label_4836:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4846;
}
else 
{
label_4846:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4876 = 0;
goto label_4872;
}
else 
{
 __return_4872 = -1073741823;
label_4872:; 
}
status1 = ioStatus__Status;
goto label_4779;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4846;
}
}
}
}
status7 = __return_4883;
goto label_4888;
}
status7 = __return_4887;
label_4888:; 
if (status7 >= 0)
{
deviceExtension__PlayActive = 1;
deviceExtension__Paused = 0;
goto label_4893;
}
else 
{
label_4893:; 
goto label_6397;
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_4885 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4782:; 
 __return_4882 = status1;
}
else 
{
{
int __tmp_305 = event;
int __tmp_306 = Executive;
int __tmp_307 = KernelMode;
int __tmp_308 = 0;
int __tmp_309 = 0;
int Object = __tmp_305;
int WaitReason = __tmp_306;
int WaitMode = __tmp_307;
int Alertable = __tmp_308;
int Timeout = __tmp_309;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4789;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4799;
}
}
else 
{
label_4789:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4799;
}
else 
{
label_4799:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4829 = 0;
goto label_4825;
}
else 
{
 __return_4825 = -1073741823;
label_4825:; 
}
status1 = ioStatus__Status;
goto label_4782;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4799;
}
}
}
}
status7 = __return_4882;
goto label_4888;
}
status7 = __return_4885;
goto label_4888;
}
}
}
else 
{
status7 = -1073741823;
__cil_tmp112 = (unsigned long)status7;
if (!(__cil_tmp112 == -2147483626))
{
label_4904:; 
myStatus = status7;
{
int __tmp_310 = Irp;
int __tmp_311 = 0;
int Irp = __tmp_310;
int PriorityBoost = __tmp_311;
if (s == NP)
{
s = DC;
goto label_4921;
}
else 
{
{
__VERIFIER_error();
}
label_4921:; 
}
 __return_4926 = status7;
}
status4 = __return_4926;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4904;
}
}
}
else 
{
__cil_tmp63 = 44;
__cil_tmp64 = 16384;
__cil_tmp65 = 131072;
__cil_tmp66 = 147456;
__cil_tmp67 = 147500;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp67)
{
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength < sizeof__SUB_Q_CURRENT_POSITION)
{
status7 = -1073741789;
Irp__IoStatus__Information = 0;
goto label_6397;
}
else 
{
if (SubQPtr___0 == 0)
{
status7 = -1073741670;
Irp__IoStatus__Information = 0;
__cil_tmp113 = (unsigned long)status7;
if (!(__cil_tmp113 == -2147483626))
{
label_4681:; 
myStatus = status7;
{
int __tmp_312 = Irp;
int __tmp_313 = 0;
int Irp = __tmp_312;
int PriorityBoost = __tmp_313;
if (s == NP)
{
s = DC;
goto label_4698;
}
else 
{
{
__VERIFIER_error();
}
label_4698:; 
}
 __return_4703 = status7;
}
status4 = __return_4703;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4681;
}
}
else 
{
if (userPtr__Format != 1)
{
status7 = -1073741823;
Irp__IoStatus__Information = 0;
__cil_tmp114 = (unsigned long)status7;
if (!(__cil_tmp114 == -2147483626))
{
label_4652:; 
myStatus = status7;
{
int __tmp_314 = Irp;
int __tmp_315 = 0;
int Irp = __tmp_314;
int PriorityBoost = __tmp_315;
if (s == NP)
{
s = DC;
goto label_4669;
}
else 
{
{
__VERIFIER_error();
}
label_4669:; 
}
 __return_4674 = status7;
}
status4 = __return_4674;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4652;
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_316 = deviceExtension;
int __tmp_317 = srb;
int __tmp_318 = SubQPtr___0;
int __tmp_319 = sizeof__SUB_Q_CHANNEL_DATA;
int Extension = __tmp_316;
int Srb = __tmp_317;
int Buffer = __tmp_318;
int BufferLength = __tmp_319;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_4627 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4519:; 
 __return_4623 = status1;
}
else 
{
{
int __tmp_320 = event;
int __tmp_321 = Executive;
int __tmp_322 = KernelMode;
int __tmp_323 = 0;
int __tmp_324 = 0;
int Object = __tmp_320;
int WaitReason = __tmp_321;
int WaitMode = __tmp_322;
int Alertable = __tmp_323;
int Timeout = __tmp_324;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4576;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4586;
}
}
else 
{
label_4576:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4586;
}
else 
{
label_4586:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4616 = 0;
goto label_4612;
}
else 
{
 __return_4612 = -1073741823;
label_4612:; 
}
status1 = ioStatus__Status;
goto label_4519;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4586;
}
}
}
}
status7 = __return_4623;
goto label_4628;
}
status7 = __return_4627;
label_4628:; 
if (status7 >= 0)
{
if (!(deviceExtension__Paused == 1))
{
label_4638:; 
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_4636;
}
else 
{
deviceExtension__PlayActive = 0;
goto label_4638;
}
}
else 
{
Irp__IoStatus__Information = 0;
label_4636:; 
goto label_6397;
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_4625 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4522:; 
 __return_4622 = status1;
}
else 
{
{
int __tmp_325 = event;
int __tmp_326 = Executive;
int __tmp_327 = KernelMode;
int __tmp_328 = 0;
int __tmp_329 = 0;
int Object = __tmp_325;
int WaitReason = __tmp_326;
int WaitMode = __tmp_327;
int Alertable = __tmp_328;
int Timeout = __tmp_329;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4529;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4539;
}
}
else 
{
label_4529:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4539;
}
else 
{
label_4539:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4569 = 0;
goto label_4565;
}
else 
{
 __return_4565 = -1073741823;
label_4565:; 
}
status1 = ioStatus__Status;
goto label_4522;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4539;
}
}
}
}
status7 = __return_4622;
goto label_4628;
}
status7 = __return_4625;
goto label_4628;
}
}
}
}
}
}
else 
{
__cil_tmp68 = 2056;
__cil_tmp69 = 16384;
__cil_tmp70 = 131072;
__cil_tmp71 = 147456;
__cil_tmp72 = 149512;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp72)
{
Irp__IoStatus__Information = 0;
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_330 = deviceExtension;
int __tmp_331 = srb;
int __tmp_332 = 0;
int __tmp_333 = 0;
int Extension = __tmp_330;
int Srb = __tmp_331;
int Buffer = __tmp_332;
int BufferLength = __tmp_333;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_4440 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4332:; 
 __return_4436 = status1;
}
else 
{
{
int __tmp_334 = event;
int __tmp_335 = Executive;
int __tmp_336 = KernelMode;
int __tmp_337 = 0;
int __tmp_338 = 0;
int Object = __tmp_334;
int WaitReason = __tmp_335;
int WaitMode = __tmp_336;
int Alertable = __tmp_337;
int Timeout = __tmp_338;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4389;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4399;
}
}
else 
{
label_4389:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4399;
}
else 
{
label_4399:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4429 = 0;
goto label_4425;
}
else 
{
 __return_4425 = -1073741823;
label_4425:; 
}
status1 = ioStatus__Status;
goto label_4332;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4399;
}
}
}
}
status7 = __return_4436;
goto label_4441;
}
status7 = __return_4440;
label_4441:; 
goto label_6397;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_4438 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4335:; 
 __return_4435 = status1;
}
else 
{
{
int __tmp_339 = event;
int __tmp_340 = Executive;
int __tmp_341 = KernelMode;
int __tmp_342 = 0;
int __tmp_343 = 0;
int Object = __tmp_339;
int WaitReason = __tmp_340;
int WaitMode = __tmp_341;
int Alertable = __tmp_342;
int Timeout = __tmp_343;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4342;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4352;
}
}
else 
{
label_4342:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4352;
}
else 
{
label_4352:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4382 = 0;
goto label_4378;
}
else 
{
 __return_4378 = -1073741823;
label_4378:; 
}
status1 = ioStatus__Status;
goto label_4335;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4352;
}
}
}
}
status7 = __return_4435;
goto label_4441;
}
status7 = __return_4438;
goto label_4441;
}
}
}
else 
{
__cil_tmp73 = 52;
__cil_tmp74 = 16384;
__cil_tmp75 = 131072;
__cil_tmp76 = 147456;
__cil_tmp77 = 147508;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp77)
{
label_4256:; 
Irp__IoStatus__Information = 0;
label_4259:; 
status7 = -1073741808;
goto label_6397;
}
else 
{
__cil_tmp78 = 20;
__cil_tmp79 = 16384;
__cil_tmp80 = 131072;
__cil_tmp81 = 147456;
__cil_tmp82 = 147476;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp82)
{
goto label_4256;
}
else 
{
__cil_tmp83 = 40;
__cil_tmp84 = 16384;
__cil_tmp85 = 131072;
__cil_tmp86 = 147456;
__cil_tmp87 = 147496;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp87)
{
Irp__IoStatus__Information = 0;
goto label_4259;
}
else 
{
__cil_tmp88 = 2048;
__cil_tmp89 = 16384;
__cil_tmp90 = 131072;
__cil_tmp91 = 147456;
__cil_tmp92 = 149504;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp92)
{
{
int __tmp_344 = DeviceObject;
int DeviceObject = __tmp_344;
int deviceExtension__PlayActive = __VERIFIER_nondet_int() ;
deviceExtension__PlayActive = __VERIFIER_nondet_int();
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int currentBuffer__Header__AudioStatus = __VERIFIER_nondet_int() ;
currentBuffer__Header__AudioStatus = __VERIFIER_nondet_int();
int irp_CdAudioIsPlayActive = __VERIFIER_nondet_int() ;
irp_CdAudioIsPlayActive = __VERIFIER_nondet_int();
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int status5 = __VERIFIER_nondet_int() ;
status5 = __VERIFIER_nondet_int();
int currentBuffer = __VERIFIER_nondet_int() ;
currentBuffer = __VERIFIER_nondet_int();
int returnValue ;
long __cil_tmp10 ;
int __cil_tmp11 ;
if (deviceExtension__PlayActive == 0)
{
 __return_4040 = 0;
goto label_4036;
}
else 
{
if (currentBuffer == 0)
{
 __return_4038 = 0;
goto label_4036;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_4036 = 0;
label_4036:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_3968:; 
if (status5 < 0)
{
 __return_4034 = 0;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_4028:; 
 __return_4032 = returnValue;
}
else 
{
returnValue = 1;
goto label_4028;
}
tmp___1 = __return_4032;
goto label_4041;
}
tmp___1 = __return_4034;
goto label_4041;
}
else 
{
{
int __tmp_355 = event;
int __tmp_356 = Suspended;
int __tmp_357 = KernelMode;
int __tmp_358 = 0;
int __tmp_359 = 0;
int Object = __tmp_355;
int WaitReason = __tmp_356;
int WaitMode = __tmp_357;
int Alertable = __tmp_358;
int Timeout = __tmp_359;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_3975;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_3985;
}
}
else 
{
label_3975:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3985;
}
else 
{
label_3985:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4015 = 0;
goto label_4011;
}
else 
{
 __return_4011 = -1073741823;
label_4011:; 
}
status5 = ioStatus__Status;
goto label_3968;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_3985;
}
}
}
}
}
tmp___1 = __return_4036;
label_4041:; 
if (!(tmp___1 == 1))
{
deviceExtension__PlayActive = 0;
{
int __tmp_345 = DeviceObject;
int __tmp_346 = Irp;
int DeviceObject = __tmp_345;
int Irp = __tmp_346;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
if (s == NP)
{
s = SKIP1;
goto label_4066;
}
else 
{
{
__VERIFIER_error();
}
label_4066:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_347 = deviceExtension__TargetDeviceObject;
int __tmp_348 = Irp;
int DeviceObject = __tmp_347;
int Irp = __tmp_348;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_349 = DeviceObject;
int __tmp_350 = Irp;
int __tmp_351 = lcontext;
int DeviceObject = __tmp_349;
int Irp = __tmp_350;
int Context = __tmp_351;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_4096:; 
if (myStatus >= 0)
{
{
int __tmp_352 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_352;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_4119;
}
else 
{
label_4119:; 
}
goto label_4112;
}
}
else 
{
label_4112:; 
 __return_4127 = myStatus;
}
compRetStatus = __return_4127;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_4086;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_4145;
}
else 
{
{
__VERIFIER_error();
}
label_4145:; 
}
goto label_4086;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_4096;
}
else 
{
pended = 1;
goto label_4096;
}
}
}
}
else 
{
label_4086:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_4173;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_4173:; 
goto label_4157;
}
else 
{
returnVal2 = -1073741823;
goto label_4173;
}
}
}
else 
{
returnVal2 = 259;
label_4157:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4198;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_4207:; 
goto label_4198;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4207;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_4198;
}
else 
{
{
__VERIFIER_error();
}
label_4198:; 
 __return_4215 = returnVal2;
}
tmp = __return_4215;
 __return_4217 = tmp;
}
tmp___0 = __return_4217;
 __return_4219 = tmp___0;
}
status4 = __return_4219;
goto label_6461;
}
}
}
}
}
}
else 
{
deviceExtension__PlayActive = 1;
status7 = 0;
Irp__IoStatus__Information = 0;
__cil_tmp115 = (unsigned long)status7;
if (!(__cil_tmp115 == -2147483626))
{
label_4228:; 
myStatus = status7;
{
int __tmp_353 = Irp;
int __tmp_354 = 0;
int Irp = __tmp_353;
int PriorityBoost = __tmp_354;
if (s == NP)
{
s = DC;
goto label_4245;
}
else 
{
{
__VERIFIER_error();
}
label_4245:; 
}
 __return_4250 = status7;
}
status4 = __return_4250;
goto label_6461;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4228;
}
}
}
}
}
}
else 
{
{
int __tmp_360 = DeviceObject;
int __tmp_361 = Irp;
int DeviceObject = __tmp_360;
int Irp = __tmp_361;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
if (s == NP)
{
s = SKIP1;
goto label_3779;
}
else 
{
{
__VERIFIER_error();
}
label_3779:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_362 = deviceExtension__TargetDeviceObject;
int __tmp_363 = Irp;
int DeviceObject = __tmp_362;
int Irp = __tmp_363;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_364 = DeviceObject;
int __tmp_365 = Irp;
int __tmp_366 = lcontext;
int DeviceObject = __tmp_364;
int Irp = __tmp_365;
int Context = __tmp_366;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_3809:; 
if (myStatus >= 0)
{
{
int __tmp_367 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_367;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_3832;
}
else 
{
label_3832:; 
}
goto label_3825;
}
}
else 
{
label_3825:; 
 __return_3840 = myStatus;
}
compRetStatus = __return_3840;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_3799;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_3858;
}
else 
{
{
__VERIFIER_error();
}
label_3858:; 
}
goto label_3799;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_3809;
}
else 
{
pended = 1;
goto label_3809;
}
}
}
}
else 
{
label_3799:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_3886;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_3886:; 
goto label_3870;
}
else 
{
returnVal2 = -1073741823;
goto label_3886;
}
}
}
else 
{
returnVal2 = 259;
label_3870:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3911;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_3920:; 
goto label_3911;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3920;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3911;
}
else 
{
{
__VERIFIER_error();
}
label_3911:; 
 __return_3928 = returnVal2;
}
tmp = __return_3928;
 __return_3930 = tmp;
}
tmp___2 = __return_3930;
 __return_3932 = tmp___2;
}
status4 = __return_3932;
goto label_6461;
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
{
int __tmp_368 = DeviceObject;
int __tmp_369 = Irp;
int DeviceObject = __tmp_368;
int Irp = __tmp_369;
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
int srb__Cdb = __VERIFIER_nondet_int() ;
srb__Cdb = __VERIFIER_nondet_int();
int currentIrpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int() ;
currentIrpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int();
int Irp__IoStatus__Information ;
int currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength = __VERIFIER_nondet_int() ;
currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength = __VERIFIER_nondet_int();
int currentIrpStack__Parameters__DeviceIoControl__InputBufferLength = __VERIFIER_nondet_int() ;
currentIrpStack__Parameters__DeviceIoControl__InputBufferLength = __VERIFIER_nondet_int();
int srb__CdbLength ;
int cdb__CDB10__OperationCode ;
int srb__TimeOutValue ;
int sizeof__READ_CAPACITY_DATA = __VERIFIER_nondet_int() ;
sizeof__READ_CAPACITY_DATA = __VERIFIER_nondet_int();
int lastSession__LogicalBlockAddress = __VERIFIER_nondet_int() ;
lastSession__LogicalBlockAddress = __VERIFIER_nondet_int();
int cdaudioDataOut__FirstTrack = __VERIFIER_nondet_int() ;
cdaudioDataOut__FirstTrack = __VERIFIER_nondet_int();
int cdaudioDataOut__LastTrack = __VERIFIER_nondet_int() ;
cdaudioDataOut__LastTrack = __VERIFIER_nondet_int();
int sizeof__CDROM_TOC = __VERIFIER_nondet_int() ;
sizeof__CDROM_TOC = __VERIFIER_nondet_int();
int sizeof__SUB_Q_CURRENT_POSITION = __VERIFIER_nondet_int() ;
sizeof__SUB_Q_CURRENT_POSITION = __VERIFIER_nondet_int();
int userPtr__Format = __VERIFIER_nondet_int() ;
userPtr__Format = __VERIFIER_nondet_int();
int sizeof__CDROM_PLAY_AUDIO_MSF = __VERIFIER_nondet_int() ;
sizeof__CDROM_PLAY_AUDIO_MSF = __VERIFIER_nondet_int();
int inputBuffer__StartingM = __VERIFIER_nondet_int() ;
inputBuffer__StartingM = __VERIFIER_nondet_int();
int inputBuffer__EndingM = __VERIFIER_nondet_int() ;
inputBuffer__EndingM = __VERIFIER_nondet_int();
int inputBuffer__StartingS = __VERIFIER_nondet_int() ;
inputBuffer__StartingS = __VERIFIER_nondet_int();
int inputBuffer__EndingS = __VERIFIER_nondet_int() ;
inputBuffer__EndingS = __VERIFIER_nondet_int();
int inputBuffer__StartingF = __VERIFIER_nondet_int() ;
inputBuffer__StartingF = __VERIFIER_nondet_int();
int inputBuffer__EndingF = __VERIFIER_nondet_int() ;
inputBuffer__EndingF = __VERIFIER_nondet_int();
int cdb__PLAY_AUDIO_MSF__OperationCode = __VERIFIER_nondet_int() ;
cdb__PLAY_AUDIO_MSF__OperationCode = __VERIFIER_nondet_int();
int sizeof__CDROM_SEEK_AUDIO_MSF = __VERIFIER_nondet_int() ;
sizeof__CDROM_SEEK_AUDIO_MSF = __VERIFIER_nondet_int();
int currentIrpStack ;
int deviceExtension ;
int cdaudioDataOut ;
int srb = __VERIFIER_nondet_int() ;
srb = __VERIFIER_nondet_int();
int lastSession = __VERIFIER_nondet_int() ;
lastSession = __VERIFIER_nondet_int();
int cdb ;
int status6 ;
int i = __VERIFIER_nondet_int() ;
i = __VERIFIER_nondet_int();
int bytesTransfered = __VERIFIER_nondet_int() ;
bytesTransfered = __VERIFIER_nondet_int();
int Toc = __VERIFIER_nondet_int() ;
Toc = __VERIFIER_nondet_int();
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
int tmp___4 ;
int tracksToReturn ;
int tracksOnCd ;
int tracksInBuffer ;
int userPtr ;
int SubQPtr = __VERIFIER_nondet_int() ;
SubQPtr = __VERIFIER_nondet_int();
int tmp___5 ;
int tmp___6 ;
int inputBuffer ;
int inputBuffer___0 ;
int tmp___7 ;
int tmp___8 ;
int __cil_tmp58 ;
int __cil_tmp59 ;
int __cil_tmp60 ;
int __cil_tmp61 ;
int __cil_tmp62 ;
int __cil_tmp63 ;
int __cil_tmp64 ;
int __cil_tmp65 ;
int __cil_tmp66 ;
int __cil_tmp67 ;
int __cil_tmp68 ;
int __cil_tmp69 ;
int __cil_tmp70 ;
int __cil_tmp71 ;
int __cil_tmp72 ;
int __cil_tmp73 ;
int __cil_tmp74 ;
int __cil_tmp75 ;
int __cil_tmp76 ;
int __cil_tmp77 ;
int __cil_tmp78 ;
int __cil_tmp79 ;
int __cil_tmp80 ;
int __cil_tmp81 ;
int __cil_tmp82 ;
int __cil_tmp83 ;
int __cil_tmp84 ;
int __cil_tmp85 ;
int __cil_tmp86 ;
int __cil_tmp87 ;
int __cil_tmp88 ;
int __cil_tmp89 ;
int __cil_tmp90 ;
int __cil_tmp91 ;
int __cil_tmp92 ;
int __cil_tmp93 ;
int __cil_tmp94 ;
int __cil_tmp95 ;
int __cil_tmp96 ;
int __cil_tmp97 ;
int __cil_tmp98 ;
int __cil_tmp99 ;
int __cil_tmp100 ;
int __cil_tmp101 ;
int __cil_tmp102 ;
int __cil_tmp103 ;
int __cil_tmp104 ;
int __cil_tmp105 ;
int __cil_tmp106 ;
unsigned long __cil_tmp107 ;
unsigned long __cil_tmp108 ;
int __cil_tmp109 ;
int __cil_tmp110 ;
currentIrpStack = Irp__Tail__Overlay__CurrentStackLocation;
deviceExtension = DeviceObject__DeviceExtension;
cdaudioDataOut = Irp__AssociatedIrp__SystemBuffer;
cdb = srb__Cdb;
__cil_tmp58 = 56;
__cil_tmp59 = 16384;
__cil_tmp60 = 131072;
__cil_tmp61 = 147456;
__cil_tmp62 = 147512;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp62)
{
{
int __tmp_370 = DeviceObject;
int DeviceObject = __tmp_370;
int deviceExtension__PlayActive = __VERIFIER_nondet_int() ;
deviceExtension__PlayActive = __VERIFIER_nondet_int();
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int currentBuffer__Header__AudioStatus = __VERIFIER_nondet_int() ;
currentBuffer__Header__AudioStatus = __VERIFIER_nondet_int();
int irp_CdAudioIsPlayActive = __VERIFIER_nondet_int() ;
irp_CdAudioIsPlayActive = __VERIFIER_nondet_int();
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int status5 = __VERIFIER_nondet_int() ;
status5 = __VERIFIER_nondet_int();
int currentBuffer = __VERIFIER_nondet_int() ;
currentBuffer = __VERIFIER_nondet_int();
int returnValue ;
long __cil_tmp10 ;
int __cil_tmp11 ;
if (deviceExtension__PlayActive == 0)
{
 __return_8369 = 0;
goto label_8365;
}
else 
{
if (currentBuffer == 0)
{
 __return_8367 = 0;
goto label_8365;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_8365 = 0;
label_8365:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_8297:; 
if (status5 < 0)
{
 __return_8363 = 0;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_8357:; 
 __return_8361 = returnValue;
}
else 
{
returnValue = 1;
goto label_8357;
}
tmp = __return_8361;
goto label_8370;
}
tmp = __return_8363;
goto label_8370;
}
else 
{
{
int __tmp_400 = event;
int __tmp_401 = Suspended;
int __tmp_402 = KernelMode;
int __tmp_403 = 0;
int __tmp_404 = 0;
int Object = __tmp_400;
int WaitReason = __tmp_401;
int WaitMode = __tmp_402;
int Alertable = __tmp_403;
int Timeout = __tmp_404;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_8304;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_8314;
}
}
else 
{
label_8304:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_8314;
}
else 
{
label_8314:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_8344 = 0;
goto label_8340;
}
else 
{
 __return_8340 = -1073741823;
label_8340:; 
}
status5 = ioStatus__Status;
goto label_8297;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_8314;
}
}
}
}
}
tmp = __return_8365;
label_8370:; 
if (!(tmp == 0))
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
label_8380:; 
{
int __tmp_371 = status6;
int __tmp_372 = Irp;
int __tmp_373 = deviceExtension__TargetDeviceObject;
int status = __tmp_371;
int Irp = __tmp_372;
int deviceExtension__TargetDeviceObject = __tmp_373;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_374 = Irp;
int __tmp_375 = 0;
int Irp = __tmp_374;
int PriorityBoost = __tmp_375;
if (s == NP)
{
s = DC;
goto label_8617;
}
else 
{
{
__VERIFIER_error();
}
label_8617:; 
}
 __return_8622 = status;
}
tmp___8 = __return_8622;
 __return_8747 = tmp___8;
}
status4 = __return_8747;
goto label_8804;
}
else 
{
if (!(currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength == 0))
{
status6 = -1073741789;
Irp__IoStatus__Information = 0;
goto label_8380;
}
else 
{
if (lastSession == 0)
{
status6 = -1073741670;
Irp__IoStatus__Information = 0;
{
int __tmp_376 = status6;
int __tmp_377 = Irp;
int __tmp_378 = deviceExtension__TargetDeviceObject;
int status = __tmp_376;
int Irp = __tmp_377;
int deviceExtension__TargetDeviceObject = __tmp_378;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_379 = Irp;
int __tmp_380 = 0;
int Irp = __tmp_379;
int PriorityBoost = __tmp_380;
if (s == NP)
{
s = DC;
goto label_8796;
}
else 
{
{
__VERIFIER_error();
}
label_8796:; 
}
 __return_8801 = status;
}
tmp___0 = __return_8801;
 __return_8803 = tmp___0;
}
status4 = __return_8803;
label_8804:; 
goto label_3052;
}
else 
{
srb__CdbLength = 10;
cdb__CDB10__OperationCode = 38;
srb__TimeOutValue = 10;
{
int __tmp_381 = deviceExtension;
int __tmp_382 = srb;
int __tmp_383 = lastSession;
int __tmp_384 = sizeof__READ_CAPACITY_DATA;
int Extension = __tmp_381;
int Srb = __tmp_382;
int Buffer = __tmp_383;
int BufferLength = __tmp_384;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_8565 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_8457:; 
 __return_8561 = status1;
}
else 
{
{
int __tmp_390 = event;
int __tmp_391 = Executive;
int __tmp_392 = KernelMode;
int __tmp_393 = 0;
int __tmp_394 = 0;
int Object = __tmp_390;
int WaitReason = __tmp_391;
int WaitMode = __tmp_392;
int Alertable = __tmp_393;
int Timeout = __tmp_394;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_8514;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_8524;
}
}
else 
{
label_8514:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_8524;
}
else 
{
label_8524:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_8554 = 0;
goto label_8550;
}
else 
{
 __return_8550 = -1073741823;
label_8550:; 
}
status1 = ioStatus__Status;
goto label_8457;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_8524;
}
}
}
}
status6 = __return_8561;
goto label_8566;
}
status6 = __return_8565;
label_8566:; 
if (status6 < 0)
{
Irp__IoStatus__Information = 0;
{
int __tmp_385 = status6;
int __tmp_386 = Irp;
int __tmp_387 = deviceExtension__TargetDeviceObject;
int status = __tmp_385;
int Irp = __tmp_386;
int deviceExtension__TargetDeviceObject = __tmp_387;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_388 = Irp;
int __tmp_389 = 0;
int Irp = __tmp_388;
int PriorityBoost = __tmp_389;
if (s == NP)
{
s = DC;
goto label_8767;
}
else 
{
{
__VERIFIER_error();
}
label_8767:; 
}
 __return_8772 = status;
}
tmp___1 = __return_8772;
 __return_8774 = tmp___1;
}
status4 = __return_8774;
goto label_8804;
}
else 
{
status6 = 0;
Irp__IoStatus__Information = bytesTransfered;
if (lastSession__LogicalBlockAddress == 0)
{
goto label_8380;
}
else 
{
cdaudioDataOut__FirstTrack = 1;
cdaudioDataOut__LastTrack = 2;
goto label_8380;
}
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_8563 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_8460:; 
 __return_8560 = status1;
}
else 
{
{
int __tmp_395 = event;
int __tmp_396 = Executive;
int __tmp_397 = KernelMode;
int __tmp_398 = 0;
int __tmp_399 = 0;
int Object = __tmp_395;
int WaitReason = __tmp_396;
int WaitMode = __tmp_397;
int Alertable = __tmp_398;
int Timeout = __tmp_399;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_8467;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_8477;
}
}
else 
{
label_8467:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_8477;
}
else 
{
label_8477:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_8507 = 0;
goto label_8503;
}
else 
{
 __return_8503 = -1073741823;
label_8503:; 
}
status1 = ioStatus__Status;
goto label_8460;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_8477;
}
}
}
}
status6 = __return_8560;
goto label_8566;
}
status6 = __return_8563;
goto label_8566;
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
__cil_tmp63 = 16384;
__cil_tmp64 = 131072;
__cil_tmp65 = 147456;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp65)
{
if (!(currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength == 0))
{
status6 = -1073741789;
Irp__IoStatus__Information = 0;
label_7860:; 
label_8589:; 
{
int __tmp_405 = status6;
int __tmp_406 = Irp;
int __tmp_407 = deviceExtension__TargetDeviceObject;
int status = __tmp_405;
int Irp = __tmp_406;
int deviceExtension__TargetDeviceObject = __tmp_407;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_408 = Irp;
int __tmp_409 = 0;
int Irp = __tmp_408;
int PriorityBoost = __tmp_409;
if (s == NP)
{
s = DC;
goto label_8689;
}
else 
{
{
__VERIFIER_error();
}
label_8689:; 
}
 __return_8694 = status;
}
tmp___8 = __return_8694;
label_8695:; 
 __return_8744 = tmp___8;
}
status4 = __return_8744;
goto label_8804;
}
else 
{
{
int __tmp_410 = DeviceObject;
int DeviceObject = __tmp_410;
int deviceExtension__PlayActive = __VERIFIER_nondet_int() ;
deviceExtension__PlayActive = __VERIFIER_nondet_int();
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int currentBuffer__Header__AudioStatus = __VERIFIER_nondet_int() ;
currentBuffer__Header__AudioStatus = __VERIFIER_nondet_int();
int irp_CdAudioIsPlayActive = __VERIFIER_nondet_int() ;
irp_CdAudioIsPlayActive = __VERIFIER_nondet_int();
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int status5 = __VERIFIER_nondet_int() ;
status5 = __VERIFIER_nondet_int();
int currentBuffer = __VERIFIER_nondet_int() ;
currentBuffer = __VERIFIER_nondet_int();
int returnValue ;
long __cil_tmp10 ;
int __cil_tmp11 ;
if (deviceExtension__PlayActive == 0)
{
 __return_7966 = 0;
goto label_7962;
}
else 
{
if (currentBuffer == 0)
{
 __return_7964 = 0;
goto label_7962;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_7962 = 0;
label_7962:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_7894:; 
if (status5 < 0)
{
 __return_7960 = 0;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_7954:; 
 __return_7958 = returnValue;
}
else 
{
returnValue = 1;
goto label_7954;
}
tmp___2 = __return_7958;
goto label_7967;
}
tmp___2 = __return_7960;
goto label_7967;
}
else 
{
{
int __tmp_445 = event;
int __tmp_446 = Suspended;
int __tmp_447 = KernelMode;
int __tmp_448 = 0;
int __tmp_449 = 0;
int Object = __tmp_445;
int WaitReason = __tmp_446;
int WaitMode = __tmp_447;
int Alertable = __tmp_448;
int Timeout = __tmp_449;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7901;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7911;
}
}
else 
{
label_7901:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7911;
}
else 
{
label_7911:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7941 = 0;
goto label_7937;
}
else 
{
 __return_7937 = -1073741823;
label_7937:; 
}
status5 = ioStatus__Status;
goto label_7894;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7911;
}
}
}
}
}
tmp___2 = __return_7962;
label_7967:; 
if (!(tmp___2 == 0))
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_7860;
}
else 
{
if (Toc == 0)
{
status6 = -1073741670;
Irp__IoStatus__Information = 0;
{
int __tmp_411 = status6;
int __tmp_412 = Irp;
int __tmp_413 = deviceExtension__TargetDeviceObject;
int status = __tmp_411;
int Irp = __tmp_412;
int deviceExtension__TargetDeviceObject = __tmp_413;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_414 = Irp;
int __tmp_415 = 0;
int Irp = __tmp_414;
int PriorityBoost = __tmp_415;
if (s == NP)
{
s = DC;
goto label_8254;
}
else 
{
{
__VERIFIER_error();
}
label_8254:; 
}
 __return_8259 = status;
}
tmp___3 = __return_8259;
 __return_8261 = tmp___3;
}
status4 = __return_8261;
goto label_8804;
}
else 
{
srb__TimeOutValue = 10;
srb__CdbLength = 10;
{
int __tmp_416 = deviceExtension;
int __tmp_417 = srb;
int __tmp_418 = Toc;
int __tmp_419 = sizeof__CDROM_TOC;
int Extension = __tmp_416;
int Srb = __tmp_417;
int Buffer = __tmp_418;
int BufferLength = __tmp_419;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_8152 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_8044:; 
 __return_8148 = status1;
}
else 
{
{
int __tmp_435 = event;
int __tmp_436 = Executive;
int __tmp_437 = KernelMode;
int __tmp_438 = 0;
int __tmp_439 = 0;
int Object = __tmp_435;
int WaitReason = __tmp_436;
int WaitMode = __tmp_437;
int Alertable = __tmp_438;
int Timeout = __tmp_439;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_8101;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_8111;
}
}
else 
{
label_8101:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_8111;
}
else 
{
label_8111:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_8141 = 0;
goto label_8137;
}
else 
{
 __return_8137 = -1073741823;
label_8137:; 
}
status1 = ioStatus__Status;
goto label_8044;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_8111;
}
}
}
}
status6 = __return_8148;
goto label_8153;
}
status6 = __return_8152;
label_8153:; 
if (status6 >= 0)
{
__cil_tmp107 = (unsigned long)status6;
if (__cil_tmp107 != -1073741764)
{
status6 = 0;
__cil_tmp109 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp109 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_8214;
}
else 
{
tracksToReturn = tracksOnCd;
label_8214:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_8220;
}
else 
{
label_8220:; 
{
int __tmp_420 = status6;
int __tmp_421 = Irp;
int __tmp_422 = deviceExtension__TargetDeviceObject;
int status = __tmp_420;
int Irp = __tmp_421;
int deviceExtension__TargetDeviceObject = __tmp_422;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_423 = Irp;
int __tmp_424 = 0;
int Irp = __tmp_423;
int PriorityBoost = __tmp_424;
if (s == NP)
{
s = DC;
goto label_8641;
}
else 
{
{
__VERIFIER_error();
}
label_8641:; 
}
 __return_8646 = status;
}
tmp___8 = __return_8646;
 __return_8746 = tmp___8;
}
status4 = __return_8746;
goto label_8804;
}
}
}
else 
{
goto label_8159;
}
}
else 
{
label_8159:; 
__cil_tmp108 = (unsigned long)status6;
if (__cil_tmp108 != -1073741764)
{
Irp__IoStatus__Information = 0;
{
int __tmp_425 = status6;
int __tmp_426 = Irp;
int __tmp_427 = deviceExtension__TargetDeviceObject;
int status = __tmp_425;
int Irp = __tmp_426;
int deviceExtension__TargetDeviceObject = __tmp_427;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_428 = Irp;
int __tmp_429 = 0;
int Irp = __tmp_428;
int PriorityBoost = __tmp_429;
if (s == NP)
{
s = DC;
goto label_8189;
}
else 
{
{
__VERIFIER_error();
}
label_8189:; 
}
 __return_8194 = status;
}
tmp___4 = __return_8194;
 __return_8196 = tmp___4;
}
status4 = __return_8196;
goto label_8804;
}
else 
{
__cil_tmp109 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp109 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_8213;
}
else 
{
tracksToReturn = tracksOnCd;
label_8213:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_8222;
}
else 
{
label_8222:; 
{
int __tmp_430 = status6;
int __tmp_431 = Irp;
int __tmp_432 = deviceExtension__TargetDeviceObject;
int status = __tmp_430;
int Irp = __tmp_431;
int deviceExtension__TargetDeviceObject = __tmp_432;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_433 = Irp;
int __tmp_434 = 0;
int Irp = __tmp_433;
int PriorityBoost = __tmp_434;
if (s == NP)
{
s = DC;
goto label_8665;
}
else 
{
{
__VERIFIER_error();
}
label_8665:; 
}
 __return_8670 = status;
}
tmp___8 = __return_8670;
 __return_8745 = tmp___8;
}
status4 = __return_8745;
goto label_8804;
}
}
}
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_8150 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_8047:; 
 __return_8147 = status1;
}
else 
{
{
int __tmp_440 = event;
int __tmp_441 = Executive;
int __tmp_442 = KernelMode;
int __tmp_443 = 0;
int __tmp_444 = 0;
int Object = __tmp_440;
int WaitReason = __tmp_441;
int WaitMode = __tmp_442;
int Alertable = __tmp_443;
int Timeout = __tmp_444;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_8054;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_8064;
}
}
else 
{
label_8054:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_8064;
}
else 
{
label_8064:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_8094 = 0;
goto label_8090;
}
else 
{
 __return_8090 = -1073741823;
label_8090:; 
}
status1 = ioStatus__Status;
goto label_8047;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_8064;
}
}
}
}
status6 = __return_8147;
goto label_8153;
}
status6 = __return_8150;
goto label_8153;
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
__cil_tmp66 = 44;
__cil_tmp67 = 16384;
__cil_tmp68 = 131072;
__cil_tmp69 = 147456;
__cil_tmp70 = 147500;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp70)
{
userPtr = Irp__AssociatedIrp__SystemBuffer;
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength < sizeof__SUB_Q_CURRENT_POSITION)
{
status6 = -1073741789;
Irp__IoStatus__Information = 0;
goto label_8589;
}
else 
{
if (SubQPtr == 0)
{
status6 = -1073741670;
Irp__IoStatus__Information = 0;
{
int __tmp_450 = status6;
int __tmp_451 = Irp;
int __tmp_452 = deviceExtension__TargetDeviceObject;
int status = __tmp_450;
int Irp = __tmp_451;
int deviceExtension__TargetDeviceObject = __tmp_452;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_453 = Irp;
int __tmp_454 = 0;
int Irp = __tmp_453;
int PriorityBoost = __tmp_454;
if (s == NP)
{
s = DC;
goto label_7838;
}
else 
{
{
__VERIFIER_error();
}
label_7838:; 
}
 __return_7843 = status;
}
tmp___5 = __return_7843;
 __return_7845 = tmp___5;
}
status4 = __return_7845;
goto label_8804;
}
else 
{
if (userPtr__Format != 1)
{
status6 = -1073741823;
Irp__IoStatus__Information = 0;
{
int __tmp_455 = status6;
int __tmp_456 = Irp;
int __tmp_457 = deviceExtension__TargetDeviceObject;
int status = __tmp_455;
int Irp = __tmp_456;
int deviceExtension__TargetDeviceObject = __tmp_457;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_458 = Irp;
int __tmp_459 = 0;
int Irp = __tmp_458;
int PriorityBoost = __tmp_459;
if (s == NP)
{
s = DC;
goto label_7809;
}
else 
{
{
__VERIFIER_error();
}
label_7809:; 
}
 __return_7814 = status;
}
tmp___6 = __return_7814;
 __return_7816 = tmp___6;
}
status4 = __return_7816;
goto label_8804;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_460 = deviceExtension;
int __tmp_461 = srb;
int __tmp_462 = SubQPtr;
int __tmp_463 = sizeof__SUB_Q_CURRENT_POSITION;
int Extension = __tmp_460;
int Srb = __tmp_461;
int Buffer = __tmp_462;
int BufferLength = __tmp_463;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_7775 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7667:; 
 __return_7771 = status1;
}
else 
{
{
int __tmp_464 = event;
int __tmp_465 = Executive;
int __tmp_466 = KernelMode;
int __tmp_467 = 0;
int __tmp_468 = 0;
int Object = __tmp_464;
int WaitReason = __tmp_465;
int WaitMode = __tmp_466;
int Alertable = __tmp_467;
int Timeout = __tmp_468;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7724;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7734;
}
}
else 
{
label_7724:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7734;
}
else 
{
label_7734:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7764 = 0;
goto label_7760;
}
else 
{
 __return_7760 = -1073741823;
label_7760:; 
}
status1 = ioStatus__Status;
goto label_7667;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7734;
}
}
}
}
status6 = __return_7771;
goto label_7776;
}
status6 = __return_7775;
label_7776:; 
if (status6 >= 0)
{
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_7784;
}
else 
{
Irp__IoStatus__Information = 0;
label_7784:; 
goto label_8589;
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_7773 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7670:; 
 __return_7770 = status1;
}
else 
{
{
int __tmp_469 = event;
int __tmp_470 = Executive;
int __tmp_471 = KernelMode;
int __tmp_472 = 0;
int __tmp_473 = 0;
int Object = __tmp_469;
int WaitReason = __tmp_470;
int WaitMode = __tmp_471;
int Alertable = __tmp_472;
int Timeout = __tmp_473;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7677;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7687;
}
}
else 
{
label_7677:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7687;
}
else 
{
label_7687:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7717 = 0;
goto label_7713;
}
else 
{
 __return_7713 = -1073741823;
label_7713:; 
}
status1 = ioStatus__Status;
goto label_7670;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7687;
}
}
}
}
status6 = __return_7770;
goto label_7776;
}
status6 = __return_7773;
goto label_7776;
}
}
}
}
}
}
else 
{
__cil_tmp71 = 24;
__cil_tmp72 = 16384;
__cil_tmp73 = 131072;
__cil_tmp74 = 147456;
__cil_tmp75 = 147480;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp75)
{
inputBuffer = Irp__AssociatedIrp__SystemBuffer;
Irp__IoStatus__Information = 0;
if (currentIrpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CDROM_PLAY_AUDIO_MSF)
{
status6 = -1073741820;
goto label_8589;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_474 = deviceExtension;
int __tmp_475 = srb;
int __tmp_476 = 0;
int __tmp_477 = 0;
int Extension = __tmp_474;
int Srb = __tmp_475;
int Buffer = __tmp_476;
int BufferLength = __tmp_477;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_7584 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7476:; 
 __return_7580 = status1;
}
else 
{
{
int __tmp_483 = event;
int __tmp_484 = Executive;
int __tmp_485 = KernelMode;
int __tmp_486 = 0;
int __tmp_487 = 0;
int Object = __tmp_483;
int WaitReason = __tmp_484;
int WaitMode = __tmp_485;
int Alertable = __tmp_486;
int Timeout = __tmp_487;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7533;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7543;
}
}
else 
{
label_7533:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7543;
}
else 
{
label_7543:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7573 = 0;
goto label_7569;
}
else 
{
 __return_7569 = -1073741823;
label_7569:; 
}
status1 = ioStatus__Status;
goto label_7476;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7543;
}
}
}
}
status6 = __return_7580;
goto label_7585;
}
status6 = __return_7584;
label_7585:; 
{
int __tmp_478 = status6;
int __tmp_479 = Irp;
int __tmp_480 = deviceExtension__TargetDeviceObject;
int status = __tmp_478;
int Irp = __tmp_479;
int deviceExtension__TargetDeviceObject = __tmp_480;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_481 = Irp;
int __tmp_482 = 0;
int Irp = __tmp_481;
int PriorityBoost = __tmp_482;
if (s == NP)
{
s = DC;
goto label_8713;
}
else 
{
{
__VERIFIER_error();
}
label_8713:; 
}
 __return_8718 = status;
}
tmp___8 = __return_8718;
goto label_8695;
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_7582 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7479:; 
 __return_7579 = status1;
}
else 
{
{
int __tmp_488 = event;
int __tmp_489 = Executive;
int __tmp_490 = KernelMode;
int __tmp_491 = 0;
int __tmp_492 = 0;
int Object = __tmp_488;
int WaitReason = __tmp_489;
int WaitMode = __tmp_490;
int Alertable = __tmp_491;
int Timeout = __tmp_492;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7486;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7496;
}
}
else 
{
label_7486:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7496;
}
else 
{
label_7496:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7526 = 0;
goto label_7522;
}
else 
{
 __return_7522 = -1073741823;
label_7522:; 
}
status1 = ioStatus__Status;
goto label_7479;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7496;
}
}
}
}
status6 = __return_7579;
goto label_7585;
}
status6 = __return_7582;
goto label_7585;
}
}
}
}
else 
{
__cil_tmp76 = 4;
__cil_tmp77 = 16384;
__cil_tmp78 = 131072;
__cil_tmp79 = 147456;
__cil_tmp80 = 147460;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp80)
{
inputBuffer___0 = Irp__AssociatedIrp__SystemBuffer;
Irp__IoStatus__Information = 0;
if (currentIrpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CDROM_SEEK_AUDIO_MSF)
{
status6 = -1073741820;
goto label_8589;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_493 = deviceExtension;
int __tmp_494 = srb;
int __tmp_495 = 0;
int __tmp_496 = 0;
int Extension = __tmp_493;
int Srb = __tmp_494;
int Buffer = __tmp_495;
int BufferLength = __tmp_496;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_7396 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7288:; 
 __return_7392 = status1;
}
else 
{
{
int __tmp_502 = event;
int __tmp_503 = Executive;
int __tmp_504 = KernelMode;
int __tmp_505 = 0;
int __tmp_506 = 0;
int Object = __tmp_502;
int WaitReason = __tmp_503;
int WaitMode = __tmp_504;
int Alertable = __tmp_505;
int Timeout = __tmp_506;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7345;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7355;
}
}
else 
{
label_7345:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7355;
}
else 
{
label_7355:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7385 = 0;
goto label_7381;
}
else 
{
 __return_7381 = -1073741823;
label_7381:; 
}
status1 = ioStatus__Status;
goto label_7288;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7355;
}
}
}
}
status6 = __return_7392;
goto label_7397;
}
status6 = __return_7396;
label_7397:; 
{
int __tmp_497 = status6;
int __tmp_498 = Irp;
int __tmp_499 = deviceExtension__TargetDeviceObject;
int status = __tmp_497;
int Irp = __tmp_498;
int deviceExtension__TargetDeviceObject = __tmp_499;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_500 = Irp;
int __tmp_501 = 0;
int Irp = __tmp_500;
int PriorityBoost = __tmp_501;
if (s == NP)
{
s = DC;
goto label_8737;
}
else 
{
{
__VERIFIER_error();
}
label_8737:; 
}
 __return_8742 = status;
}
tmp___8 = __return_8742;
goto label_8695;
}
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_7394 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7291:; 
 __return_7391 = status1;
}
else 
{
{
int __tmp_507 = event;
int __tmp_508 = Executive;
int __tmp_509 = KernelMode;
int __tmp_510 = 0;
int __tmp_511 = 0;
int Object = __tmp_507;
int WaitReason = __tmp_508;
int WaitMode = __tmp_509;
int Alertable = __tmp_510;
int Timeout = __tmp_511;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7298;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7308;
}
}
else 
{
label_7298:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7308;
}
else 
{
label_7308:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7338 = 0;
goto label_7334;
}
else 
{
 __return_7334 = -1073741823;
label_7334:; 
}
status1 = ioStatus__Status;
goto label_7291;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7308;
}
}
}
}
status6 = __return_7391;
goto label_7397;
}
status6 = __return_7394;
goto label_7397;
}
}
}
}
else 
{
__cil_tmp81 = 2056;
__cil_tmp82 = 16384;
__cil_tmp83 = 131072;
__cil_tmp84 = 147456;
__cil_tmp85 = 149512;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp85)
{
Irp__IoStatus__Information = 0;
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_512 = deviceExtension;
int __tmp_513 = srb;
int __tmp_514 = 0;
int __tmp_515 = 0;
int Extension = __tmp_512;
int Srb = __tmp_513;
int Buffer = __tmp_514;
int BufferLength = __tmp_515;
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int ioctl ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int irp ;
int status1 = __VERIFIER_nondet_int() ;
status1 = __VERIFIER_nondet_int();
int __cil_tmp10 ;
int __cil_tmp11 ;
int __cil_tmp12 ;
int __cil_tmp13 ;
int __cil_tmp14 ;
int __cil_tmp15 ;
int __cil_tmp16 ;
int __cil_tmp17 ;
long __cil_tmp18 ;
irp = 0;
if (Buffer == 0)
{
__cil_tmp14 = 4100;
__cil_tmp15 = 49152;
__cil_tmp16 = 262144;
__cil_tmp17 = 311296;
ioctl = 315396;
if (irp == 0)
{
 __return_7212 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7104:; 
 __return_7208 = status1;
}
else 
{
{
int __tmp_516 = event;
int __tmp_517 = Executive;
int __tmp_518 = KernelMode;
int __tmp_519 = 0;
int __tmp_520 = 0;
int Object = __tmp_516;
int WaitReason = __tmp_517;
int WaitMode = __tmp_518;
int Alertable = __tmp_519;
int Timeout = __tmp_520;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7161;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7171;
}
}
else 
{
label_7161:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7171;
}
else 
{
label_7171:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7201 = 0;
goto label_7197;
}
else 
{
 __return_7197 = -1073741823;
label_7197:; 
}
status1 = ioStatus__Status;
goto label_7104;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7171;
}
}
}
}
status6 = __return_7208;
goto label_7213;
}
status6 = __return_7212;
label_7213:; 
goto label_8589;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_7210 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7107:; 
 __return_7207 = status1;
}
else 
{
{
int __tmp_521 = event;
int __tmp_522 = Executive;
int __tmp_523 = KernelMode;
int __tmp_524 = 0;
int __tmp_525 = 0;
int Object = __tmp_521;
int WaitReason = __tmp_522;
int WaitMode = __tmp_523;
int Alertable = __tmp_524;
int Timeout = __tmp_525;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7114;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7124;
}
}
else 
{
label_7114:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7124;
}
else 
{
label_7124:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7154 = 0;
goto label_7150;
}
else 
{
 __return_7150 = -1073741823;
label_7150:; 
}
status1 = ioStatus__Status;
goto label_7107;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7124;
}
}
}
}
status6 = __return_7207;
goto label_7213;
}
status6 = __return_7210;
goto label_7213;
}
}
}
else 
{
__cil_tmp86 = 52;
__cil_tmp87 = 16384;
__cil_tmp88 = 131072;
__cil_tmp89 = 147456;
__cil_tmp90 = 147508;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp90)
{
label_7028:; 
Irp__IoStatus__Information = 0;
label_7031:; 
status6 = -1073741808;
goto label_8589;
}
else 
{
__cil_tmp91 = 20;
__cil_tmp92 = 16384;
__cil_tmp93 = 131072;
__cil_tmp94 = 147456;
__cil_tmp95 = 147476;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp95)
{
goto label_7028;
}
else 
{
__cil_tmp96 = 40;
__cil_tmp97 = 16384;
__cil_tmp98 = 131072;
__cil_tmp99 = 147456;
__cil_tmp100 = 147496;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp100)
{
Irp__IoStatus__Information = 0;
goto label_7031;
}
else 
{
__cil_tmp101 = 2048;
__cil_tmp102 = 16384;
__cil_tmp103 = 131072;
__cil_tmp104 = 147456;
__cil_tmp105 = 149504;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp105)
{
{
int __tmp_526 = DeviceObject;
int DeviceObject = __tmp_526;
int deviceExtension__PlayActive = __VERIFIER_nondet_int() ;
deviceExtension__PlayActive = __VERIFIER_nondet_int();
int ioStatus__Status = __VERIFIER_nondet_int() ;
ioStatus__Status = __VERIFIER_nondet_int();
int currentBuffer__Header__AudioStatus = __VERIFIER_nondet_int() ;
currentBuffer__Header__AudioStatus = __VERIFIER_nondet_int();
int irp_CdAudioIsPlayActive = __VERIFIER_nondet_int() ;
irp_CdAudioIsPlayActive = __VERIFIER_nondet_int();
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int status5 = __VERIFIER_nondet_int() ;
status5 = __VERIFIER_nondet_int();
int currentBuffer = __VERIFIER_nondet_int() ;
currentBuffer = __VERIFIER_nondet_int();
int returnValue ;
long __cil_tmp10 ;
int __cil_tmp11 ;
if (deviceExtension__PlayActive == 0)
{
 __return_6847 = 0;
goto label_6843;
}
else 
{
if (currentBuffer == 0)
{
 __return_6845 = 0;
goto label_6843;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_6843 = 0;
label_6843:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_6775:; 
if (status5 < 0)
{
 __return_6841 = 0;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_6835:; 
 __return_6839 = returnValue;
}
else 
{
returnValue = 1;
goto label_6835;
}
goto label_6848;
}
goto label_6848;
}
else 
{
{
int __tmp_527 = event;
int __tmp_528 = Suspended;
int __tmp_529 = KernelMode;
int __tmp_530 = 0;
int __tmp_531 = 0;
int Object = __tmp_527;
int WaitReason = __tmp_528;
int WaitMode = __tmp_529;
int Alertable = __tmp_530;
int Timeout = __tmp_531;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_6782;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6792;
}
}
else 
{
label_6782:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6792;
}
else 
{
label_6792:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6822 = 0;
goto label_6818;
}
else 
{
 __return_6818 = -1073741823;
label_6818:; 
}
status5 = ioStatus__Status;
goto label_6775;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6792;
}
}
}
}
}
label_6848:; 
goto label_6739;
}
}
}
}
else 
{
label_6739:; 
{
int __tmp_532 = DeviceObject;
int __tmp_533 = Irp;
int DeviceObject = __tmp_532;
int Irp = __tmp_533;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
if (s == NP)
{
s = SKIP1;
goto label_6869;
}
else 
{
{
__VERIFIER_error();
}
label_6869:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_534 = deviceExtension__TargetDeviceObject;
int __tmp_535 = Irp;
int DeviceObject = __tmp_534;
int Irp = __tmp_535;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
{
int __tmp_536 = DeviceObject;
int __tmp_537 = Irp;
int __tmp_538 = lcontext;
int DeviceObject = __tmp_536;
int Irp = __tmp_537;
int Context = __tmp_538;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_6899:; 
if (myStatus >= 0)
{
{
int __tmp_539 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_539;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_6922;
}
else 
{
label_6922:; 
}
goto label_6915;
}
}
else 
{
label_6915:; 
 __return_6930 = myStatus;
}
compRetStatus = __return_6930;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_6889;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_6948;
}
else 
{
{
__VERIFIER_error();
}
label_6948:; 
}
goto label_6889;
}
}
}
else 
{
if (!(pended == 0))
{
{
__VERIFIER_error();
}
goto label_6899;
}
else 
{
pended = 1;
goto label_6899;
}
}
}
}
else 
{
label_6889:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_6976;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_6976:; 
goto label_6960;
}
else 
{
returnVal2 = -1073741823;
goto label_6976;
}
}
}
else 
{
returnVal2 = 259;
label_6960:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7001;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_7010:; 
goto label_7001;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7010;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_7001;
}
else 
{
{
__VERIFIER_error();
}
label_7001:; 
 __return_7018 = returnVal2;
}
tmp = __return_7018;
 __return_7020 = tmp;
}
tmp___7 = __return_7020;
 __return_7022 = tmp___7;
}
status4 = __return_7022;
goto label_8804;
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
label_381:; 
if (!(pended == 1))
{
label_8851:; 
if (!(pended == 1))
{
label_8865:; 
if (s != UNLOADED)
{
if (status10 != -1)
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
goto label_8877;
}
else 
{
goto label_8891;
}
}
else 
{
goto label_8891;
}
}
else 
{
label_8891:; 
if (pended != 1)
{
if (s == DC)
{
if (!(status10 == 259))
{
label_8941:; 
goto label_8877;
}
else 
{
{
__VERIFIER_error();
}
goto label_8941;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_8877;
}
else 
{
goto label_8877;
}
}
}
else 
{
goto label_8877;
}
}
}
else 
{
goto label_8877;
}
}
else 
{
label_8877:; 
 __return_8990 = status10;
return 1;
}
}
else 
{
if (s == MPR3)
{
s = MPR3;
goto label_8877;
}
else 
{
goto label_8865;
}
}
}
else 
{
if (s == NP)
{
s = NP;
goto label_8877;
}
else 
{
goto label_8851;
}
}
}
}
