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
int __return_7704;
int __return_7702;
int __return_515;
int __return_597;
int __return_599;
int __return_714;
int __return_802;
int __return_804;
int __return_1353;
int __return_1432;
int __return_1520;
int __return_1522;
int __return_1524;
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
int __return_2357;
int __return_7700;
int __return_1748;
int __return_1836;
int __return_1893;
int __return_1889;
int __return_1946;
int __return_2317;
int __return_2295;
int __return_2288;
int __return_2284;
int __return_2304;
int __return_2358;
int __return_2315;
int __return_2227;
int __return_2228;
int __return_2221;
int __return_2217;
int __return_2303;
int __return_1603;
int __return_1606;
int __return_1694;
int __return_1940;
int __return_1936;
int __return_1947;
int __return_2316;
int __return_2297;
int __return_2271;
int __return_2267;
int __return_2307;
int __return_2356;
int __return_2313;
int __return_2116;
int __return_2117;
int __return_2110;
int __return_2106;
int __return_2306;
int __return_2464;
int __return_2552;
int __return_2554;
int __return_7529;
int __return_2833;
int __return_2921;
int __return_2923;
int __return_2663;
int __return_2751;
int __return_2753;
int __return_2924;
int __return_3249;
int __return_3250;
int __return_3293;
int __return_3273;
int __return_3243;
int __return_3239;
int __return_3040;
int __return_3128;
int __return_3130;
int __return_3274;
int __return_5443;
int __return_5441;
int __return_5438;
int __return_5435;
int __return_5439;
int __return_5667;
int __return_5569;
int __return_5570;
int __return_5637;
int __return_5563;
int __return_5559;
int __return_5416;
int __return_5412;
int __return_5168;
int __return_5169;
int __return_5330;
int __return_5298;
int __return_5299;
int __return_5700;
int __return_5292;
int __return_5288;
int __return_5162;
int __return_5158;
int __return_5031;
int __return_5032;
int __return_5025;
int __return_5021;
int __return_4912;
int __return_4684;
int __return_4685;
int __return_4858;
int __return_4801;
int __return_4802;
int __return_4832;
int __return_4795;
int __return_4791;
int __return_4678;
int __return_4674;
int __return_4885;
int __return_4522;
int __return_4523;
int __return_4516;
int __return_4512;
int __return_4560;
int __return_4397;
int __return_4367;
int __return_4321;
int __return_4322;
int __return_4315;
int __return_4311;
int __return_4194;
int __return_4195;
int __return_4188;
int __return_4184;
int __return_3855;
int __return_3853;
int __return_3850;
int __return_3847;
int __return_3851;
int __return_3940;
int __return_4028;
int __return_4030;
int __return_4032;
int __return_4064;
int __return_3828;
int __return_3824;
int __return_3653;
int __return_3741;
int __return_3743;
int __return_5638;
int __return_7285;
int __return_7283;
int __return_7280;
int __return_7277;
int __return_7281;
int __return_7522;
int __return_7524;
int __return_7421;
int __return_7422;
int __return_7493;
int __return_7495;
int __return_7415;
int __return_7411;
int __return_7258;
int __return_7254;
int __return_6956;
int __return_6954;
int __return_6951;
int __return_6948;
int __return_6952;
int __return_7173;
int __return_7175;
int __return_7081;
int __return_7082;
int __return_7121;
int __return_7123;
int __return_7075;
int __return_7071;
int __return_6929;
int __return_6925;
int __return_6830;
int __return_6832;
int __return_6800;
int __return_6802;
int __return_6762;
int __return_6763;
int __return_6756;
int __return_6752;
int __return_6630;
int __return_6631;
int __return_6624;
int __return_6620;
int __return_6502;
int __return_6503;
int __return_7465;
int __return_7467;
int __return_6496;
int __return_6492;
int __return_6378;
int __return_6379;
int __return_6372;
int __return_6368;
int __return_6075;
int __return_6073;
int __return_6070;
int __return_6067;
int __return_6071;
int __return_6048;
int __return_6044;
int __return_6156;
int __return_6244;
int __return_6246;
int __return_7468;
int __return_7701;
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
 __return_7704 = -1;
goto label_7702;
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
 __return_7702 = -1;
label_7702:; 
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
 __return_1353 = tmp___0;
label_1353:; 
}
status10 = __return_1353;
goto label_601;
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
goto label_1371;
}
else 
{
{
__VERIFIER_error();
}
label_1371:; 
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
label_1401:; 
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
goto label_1424;
}
else 
{
label_1424:; 
}
goto label_1417;
}
}
else 
{
label_1417:; 
 __return_1432 = myStatus;
}
compRetStatus = __return_1432;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1391;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1450;
}
else 
{
{
__VERIFIER_error();
}
label_1450:; 
}
goto label_1391;
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
goto label_1401;
}
else 
{
pended = 1;
goto label_1401;
}
}
}
}
else 
{
label_1391:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1478;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1478:; 
goto label_1462;
}
else 
{
returnVal2 = -1073741823;
goto label_1478;
}
}
}
else 
{
returnVal2 = 259;
label_1462:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1503;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1512:; 
goto label_1503;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1512;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1503;
}
else 
{
{
__VERIFIER_error();
}
label_1503:; 
 __return_1520 = returnVal2;
}
tmp = __return_1520;
 __return_1522 = tmp;
}
tmp = __return_1522;
 __return_1524 = tmp;
goto label_1353;
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
goto label_1353;
}
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
 __return_2357 = status3;
label_2357:; 
}
status10 = __return_2357;
if (we_should_unload == 0)
{
goto label_7552;
}
else 
{
{
int __tmp_69 = d;
int DriverObject = __tmp_69;
}
label_7552:; 
if (!(pended == 1))
{
label_7563:; 
if (!(pended == 1))
{
label_7577:; 
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
goto label_7589;
}
else 
{
goto label_7603;
}
}
else 
{
goto label_7603;
}
}
else 
{
label_7603:; 
if (pended != 1)
{
if (s == DC)
{
if (!(status10 == 259))
{
label_7649:; 
goto label_7589;
}
else 
{
{
__VERIFIER_error();
}
goto label_7649;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_7589;
}
else 
{
goto label_7589;
}
}
}
else 
{
goto label_7589;
}
}
}
else 
{
goto label_7589;
}
}
else 
{
label_7589:; 
 __return_7700 = status10;
return 1;
}
}
else 
{
if (s == MPR3)
{
s = MPR3;
goto label_7589;
}
else 
{
goto label_7577;
}
}
}
else 
{
if (s == NP)
{
s = NP;
goto label_7589;
}
else 
{
goto label_7563;
}
}
}
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
int __tmp_70 = DeviceObject;
int __tmp_71 = Irp;
int DeviceObject = __tmp_70;
int Irp = __tmp_71;
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
int __tmp_72 = DeviceObject;
int __tmp_73 = Irp;
int DeviceObject = __tmp_72;
int Irp = __tmp_73;
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
goto label_1568;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
label_1568:; 
irpSp__Control = 224;
{
int __tmp_74 = deviceExtension__TargetDeviceObject;
int __tmp_75 = Irp;
int DeviceObject = __tmp_74;
int Irp = __tmp_75;
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
int __tmp_76 = DeviceObject;
int __tmp_77 = Irp;
int __tmp_78 = lcontext;
int DeviceObject = __tmp_76;
int Irp = __tmp_77;
int Context = __tmp_78;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_1717:; 
if (myStatus >= 0)
{
{
int __tmp_79 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_79;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_1740;
}
else 
{
label_1740:; 
}
goto label_1733;
}
}
else 
{
label_1733:; 
 __return_1748 = myStatus;
}
compRetStatus = __return_1748;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1707;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1766;
}
else 
{
{
__VERIFIER_error();
}
label_1766:; 
}
goto label_1707;
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
goto label_1717;
}
else 
{
pended = 1;
goto label_1717;
}
}
}
}
else 
{
label_1707:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1794;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1794:; 
goto label_1778;
}
else 
{
returnVal2 = -1073741823;
goto label_1794;
}
}
}
else 
{
returnVal2 = 259;
label_1778:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1819;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1828:; 
goto label_1819;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1828;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1819;
}
else 
{
{
__VERIFIER_error();
}
label_1819:; 
 __return_1836 = returnVal2;
}
status9 = __return_1836;
status9 = 259;
if (!(status9 == 0))
{
{
int __tmp_80 = event;
int __tmp_81 = Executive;
int __tmp_82 = KernelMode;
int __tmp_83 = 0;
int __tmp_84 = 0;
int Object = __tmp_80;
int WaitReason = __tmp_81;
int WaitMode = __tmp_82;
int Alertable = __tmp_83;
int Timeout = __tmp_84;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_1853;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_1863;
}
}
else 
{
label_1853:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_1863;
}
else 
{
label_1863:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_1893 = 0;
goto label_1889;
}
else 
{
 __return_1889 = -1073741823;
label_1889:; 
}
status9 = myStatus;
goto label_1847;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_1863;
}
}
}
}
else 
{
label_1847:; 
 __return_1946 = status9;
}
status2 = __return_1946;
if (status2 < 0)
{
 __return_2317 = status2;
goto label_2304;
}
else 
{
if (!(deviceExtension__Active == 255))
{
label_1958:; 
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_2295 = 0;
goto label_2304;
}
else 
{
if (status2 < 0)
{
goto label_2251;
}
else 
{
label_2251:; 
{
int __tmp_85 = deviceParameterHandle;
int Handle = __tmp_85;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2288 = 0;
goto label_2284;
}
else 
{
 __return_2284 = -1073741823;
label_2284:; 
}
 __return_2304 = 0;
label_2304:; 
}
status3 = __return_2304;
Irp__IoStatus__Status = status3;
myStatus = status3;
{
int __tmp_86 = Irp;
int __tmp_87 = 0;
int Irp = __tmp_86;
int PriorityBoost = __tmp_87;
if (s == NP)
{
s = DC;
goto label_2335;
}
else 
{
{
__VERIFIER_error();
}
label_2335:; 
}
 __return_2358 = status3;
goto label_1353;
}
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
 __return_2315 = 0;
goto label_2304;
}
else 
{
status2 = -1073741823;
label_1983:; 
if (status2 < 0)
{
tmp = attempt;
int __CPAchecker_TMP_0 = attempt;
attempt = attempt + 1;
__CPAchecker_TMP_0;
if (tmp >= 4)
{
goto label_1993;
}
else 
{
{
int __tmp_88 = deviceExtension;
int __tmp_89 = srb;
int __tmp_90 = inquiryDataPtr;
int __tmp_91 = 36;
int Extension = __tmp_88;
int Srb = __tmp_89;
int Buffer = __tmp_90;
int BufferLength = __tmp_91;
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
goto label_2167;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_2167:; 
if (irp == 0)
{
 __return_2227 = -1073741670;
goto label_2228;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2174:; 
 __return_2228 = status1;
label_2228:; 
}
else 
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
if (!(setEventCalled == 1))
{
goto label_2181;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2191;
}
}
else 
{
label_2181:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2191;
}
else 
{
label_2191:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2221 = 0;
goto label_2217;
}
else 
{
 __return_2217 = -1073741823;
label_2217:; 
}
status1 = ioStatus__Status;
goto label_2174;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2191;
}
}
}
}
status2 = __return_2228;
goto label_1983;
}
}
}
}
}
else 
{
label_1993:; 
if (status2 < 0)
{
deviceExtension__Active = 0;
 __return_2303 = 0;
goto label_2304;
}
else 
{
deviceExtension__Active = 0;
goto label_1958;
}
}
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
routine = 1;
irpSp__Control = 224;
{
int __tmp_97 = deviceExtension__TargetDeviceObject;
int __tmp_98 = Irp;
int DeviceObject = __tmp_97;
int Irp = __tmp_98;
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
int __tmp_99 = DeviceObject;
int __tmp_100 = Irp;
int __tmp_101 = lcontext;
int DeviceObject = __tmp_99;
int Irp = __tmp_100;
int Event = __tmp_101;
{
int __tmp_102 = Event;
int __tmp_103 = 0;
int __tmp_104 = 0;
int Event = __tmp_102;
int Increment = __tmp_103;
int Wait = __tmp_104;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1603 = l;
}
 __return_1606 = -1073741802;
}
compRetStatus = __return_1606;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1591;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1624;
}
else 
{
{
__VERIFIER_error();
}
label_1624:; 
}
goto label_1591;
}
}
}
else 
{
label_1591:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1652;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1652:; 
goto label_1636;
}
else 
{
returnVal2 = -1073741823;
goto label_1652;
}
}
}
else 
{
returnVal2 = 259;
label_1636:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1677;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1686:; 
goto label_1677;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1686;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1677;
}
else 
{
{
__VERIFIER_error();
}
label_1677:; 
 __return_1694 = returnVal2;
}
status9 = __return_1694;
status9 = 259;
if (!(status9 == 0))
{
{
int __tmp_105 = event;
int __tmp_106 = Executive;
int __tmp_107 = KernelMode;
int __tmp_108 = 0;
int __tmp_109 = 0;
int Object = __tmp_105;
int WaitReason = __tmp_106;
int WaitMode = __tmp_107;
int Alertable = __tmp_108;
int Timeout = __tmp_109;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_1900;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_1910;
}
}
else 
{
label_1900:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_1910;
}
else 
{
label_1910:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_1940 = 0;
goto label_1936;
}
else 
{
 __return_1936 = -1073741823;
label_1936:; 
}
status9 = myStatus;
goto label_1844;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_1910;
}
}
}
}
else 
{
label_1844:; 
 __return_1947 = status9;
}
status2 = __return_1947;
if (status2 < 0)
{
 __return_2316 = status2;
goto label_2307;
}
else 
{
if (!(deviceExtension__Active == 255))
{
label_1955:; 
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_2297 = 0;
goto label_2307;
}
else 
{
if (status2 < 0)
{
goto label_2253;
}
else 
{
label_2253:; 
{
int __tmp_110 = deviceParameterHandle;
int Handle = __tmp_110;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2271 = 0;
goto label_2267;
}
else 
{
 __return_2267 = -1073741823;
label_2267:; 
}
 __return_2307 = 0;
label_2307:; 
}
status3 = __return_2307;
Irp__IoStatus__Status = status3;
myStatus = status3;
{
int __tmp_111 = Irp;
int __tmp_112 = 0;
int Irp = __tmp_111;
int PriorityBoost = __tmp_112;
if (s == NP)
{
s = DC;
goto label_2351;
}
else 
{
{
__VERIFIER_error();
}
label_2351:; 
}
 __return_2356 = status3;
goto label_2357;
}
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
 __return_2313 = 0;
goto label_2307;
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
int __tmp_113 = deviceExtension;
int __tmp_114 = srb;
int __tmp_115 = inquiryDataPtr;
int __tmp_116 = 36;
int Extension = __tmp_113;
int Srb = __tmp_114;
int Buffer = __tmp_115;
int BufferLength = __tmp_116;
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
goto label_2056;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_2056:; 
if (irp == 0)
{
 __return_2116 = -1073741670;
goto label_2117;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2063:; 
 __return_2117 = status1;
label_2117:; 
}
else 
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
if (!(setEventCalled == 1))
{
goto label_2070;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2080;
}
}
else 
{
label_2070:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2080;
}
else 
{
label_2080:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2110 = 0;
goto label_2106;
}
else 
{
 __return_2106 = -1073741823;
label_2106:; 
}
status1 = ioStatus__Status;
goto label_2063;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2080;
}
}
}
}
status2 = __return_2117;
goto label_1982;
}
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
 __return_2306 = 0;
goto label_2307;
}
else 
{
deviceExtension__Active = 0;
goto label_1955;
}
}
}
}
}
}
}
}
}
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
int __tmp_122 = devobj;
int __tmp_123 = pirp;
int DeviceObject = __tmp_122;
int Irp = __tmp_123;
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
int __tmp_124 = DeviceObject;
int __tmp_125 = Irp;
int DeviceObject = __tmp_124;
int Irp = __tmp_125;
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
goto label_2403;
}
else 
{
{
__VERIFIER_error();
}
label_2403:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_126 = deviceExtension__TargetDeviceObject;
int __tmp_127 = Irp;
int DeviceObject = __tmp_126;
int Irp = __tmp_127;
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
int __tmp_128 = DeviceObject;
int __tmp_129 = Irp;
int __tmp_130 = lcontext;
int DeviceObject = __tmp_128;
int Irp = __tmp_129;
int Context = __tmp_130;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_2433:; 
if (myStatus >= 0)
{
{
int __tmp_131 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_131;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_2456;
}
else 
{
label_2456:; 
}
goto label_2449;
}
}
else 
{
label_2449:; 
 __return_2464 = myStatus;
}
compRetStatus = __return_2464;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2423;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_2482;
}
else 
{
{
__VERIFIER_error();
}
label_2482:; 
}
goto label_2423;
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
goto label_2433;
}
else 
{
pended = 1;
goto label_2433;
}
}
}
}
else 
{
label_2423:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_2510;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_2510:; 
goto label_2494;
}
else 
{
returnVal2 = -1073741823;
goto label_2510;
}
}
}
else 
{
returnVal2 = 259;
label_2494:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2535;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_2544:; 
goto label_2535;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2544;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2535;
}
else 
{
{
__VERIFIER_error();
}
label_2535:; 
 __return_2552 = returnVal2;
}
tmp = __return_2552;
 __return_2554 = tmp;
}
status4 = __return_2554;
label_2556:; 
 __return_7529 = status4;
}
status10 = __return_7529;
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
int __tmp_132 = DeviceObject;
int __tmp_133 = Irp;
int DeviceObject = __tmp_132;
int Irp = __tmp_133;
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
goto label_2764;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
goto label_2764;
}
else 
{
compRegistered = 1;
routine = 0;
label_2764:; 
irpSp__Control = 224;
{
int __tmp_134 = deviceExtension__TargetDeviceObject;
int __tmp_135 = Irp;
int DeviceObject = __tmp_134;
int Irp = __tmp_135;
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
int __tmp_136 = DeviceObject;
int __tmp_137 = Irp;
int __tmp_138 = lcontext;
int DeviceObject = __tmp_136;
int Irp = __tmp_137;
int Context = __tmp_138;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_2802:; 
if (myStatus >= 0)
{
{
int __tmp_139 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_139;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_2825;
}
else 
{
label_2825:; 
}
goto label_2818;
}
}
else 
{
label_2818:; 
 __return_2833 = myStatus;
}
compRetStatus = __return_2833;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2792;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_2851;
}
else 
{
{
__VERIFIER_error();
}
label_2851:; 
}
goto label_2792;
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
goto label_2802;
}
else 
{
pended = 1;
goto label_2802;
}
}
}
}
else 
{
label_2792:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_2879;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_2879:; 
goto label_2863;
}
else 
{
returnVal2 = -1073741823;
goto label_2879;
}
}
}
else 
{
returnVal2 = 259;
label_2863:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2904;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_2913:; 
goto label_2904;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2913;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2904;
}
else 
{
{
__VERIFIER_error();
}
label_2904:; 
 __return_2921 = returnVal2;
}
tmp = __return_2921;
 __return_2923 = tmp;
goto label_2924;
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
int __tmp_140 = DeviceObject;
int __tmp_141 = Irp;
int DeviceObject = __tmp_140;
int Irp = __tmp_141;
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
goto label_2602;
}
else 
{
{
__VERIFIER_error();
}
label_2602:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_142 = deviceExtension__TargetDeviceObject;
int __tmp_143 = Irp;
int DeviceObject = __tmp_142;
int Irp = __tmp_143;
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
int __tmp_144 = DeviceObject;
int __tmp_145 = Irp;
int __tmp_146 = lcontext;
int DeviceObject = __tmp_144;
int Irp = __tmp_145;
int Context = __tmp_146;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_2632:; 
if (myStatus >= 0)
{
{
int __tmp_147 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_147;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_2655;
}
else 
{
label_2655:; 
}
goto label_2648;
}
}
else 
{
label_2648:; 
 __return_2663 = myStatus;
}
compRetStatus = __return_2663;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2622;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_2681;
}
else 
{
{
__VERIFIER_error();
}
label_2681:; 
}
goto label_2622;
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
goto label_2632;
}
else 
{
pended = 1;
goto label_2632;
}
}
}
}
else 
{
label_2622:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_2709;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_2709:; 
goto label_2693;
}
else 
{
returnVal2 = -1073741823;
goto label_2709;
}
}
}
else 
{
returnVal2 = 259;
label_2693:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2734;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_2743:; 
goto label_2734;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2743;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2734;
}
else 
{
{
__VERIFIER_error();
}
label_2734:; 
 __return_2751 = returnVal2;
}
tmp = __return_2751;
 __return_2753 = tmp;
}
tmp___0 = __return_2753;
 __return_2924 = tmp___0;
label_2924:; 
}
status4 = __return_2924;
label_2926:; 
goto label_2556;
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
int __tmp_148 = DeviceObject;
int __tmp_149 = Irp;
int DeviceObject = __tmp_148;
int Irp = __tmp_149;
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
int __tmp_150 = deviceExtension;
int __tmp_151 = srb;
int __tmp_152 = 0;
int __tmp_153 = 0;
int Extension = __tmp_150;
int Srb = __tmp_151;
int Buffer = __tmp_152;
int BufferLength = __tmp_153;
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
goto label_3189;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_3189:; 
if (irp == 0)
{
 __return_3249 = -1073741670;
goto label_3250;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_3196:; 
 __return_3250 = status1;
label_3250:; 
}
else 
{
{
int __tmp_158 = event;
int __tmp_159 = Executive;
int __tmp_160 = KernelMode;
int __tmp_161 = 0;
int __tmp_162 = 0;
int Object = __tmp_158;
int WaitReason = __tmp_159;
int WaitMode = __tmp_160;
int Alertable = __tmp_161;
int Timeout = __tmp_162;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_3203;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_3213;
}
}
else 
{
label_3203:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3213;
}
else 
{
label_3213:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_3243 = 0;
goto label_3239;
}
else 
{
 __return_3239 = -1073741823;
label_3239:; 
}
status1 = ioStatus__Status;
goto label_3196;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_3213;
}
}
}
}
status8 = __return_3250;
if (status8 < 0)
{
Irp__IoStatus__Status = status8;
myStatus = status8;
{
int __tmp_154 = Irp;
int __tmp_155 = 0;
int Irp = __tmp_154;
int PriorityBoost = __tmp_155;
if (s == NP)
{
s = DC;
goto label_3288;
}
else 
{
{
__VERIFIER_error();
}
label_3288:; 
}
 __return_3293 = status8;
goto label_3274;
}
}
else 
{
Irp__IoStatus__Status = status8;
myStatus = status8;
{
int __tmp_156 = Irp;
int __tmp_157 = 0;
int Irp = __tmp_156;
int PriorityBoost = __tmp_157;
if (s == NP)
{
s = DC;
goto label_3268;
}
else 
{
{
__VERIFIER_error();
}
label_3268:; 
}
 __return_3273 = status8;
goto label_3274;
}
}
}
}
}
}
else 
{
{
int __tmp_163 = DeviceObject;
int __tmp_164 = Irp;
int DeviceObject = __tmp_163;
int Irp = __tmp_164;
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
goto label_2979;
}
else 
{
{
__VERIFIER_error();
}
label_2979:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_165 = deviceExtension__TargetDeviceObject;
int __tmp_166 = Irp;
int DeviceObject = __tmp_165;
int Irp = __tmp_166;
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
int __tmp_167 = DeviceObject;
int __tmp_168 = Irp;
int __tmp_169 = lcontext;
int DeviceObject = __tmp_167;
int Irp = __tmp_168;
int Context = __tmp_169;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_3009:; 
if (myStatus >= 0)
{
{
int __tmp_170 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_170;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_3032;
}
else 
{
label_3032:; 
}
goto label_3025;
}
}
else 
{
label_3025:; 
 __return_3040 = myStatus;
}
compRetStatus = __return_3040;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2999;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_3058;
}
else 
{
{
__VERIFIER_error();
}
label_3058:; 
}
goto label_2999;
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
goto label_3009;
}
else 
{
pended = 1;
goto label_3009;
}
}
}
}
else 
{
label_2999:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_3086;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_3086:; 
goto label_3070;
}
else 
{
returnVal2 = -1073741823;
goto label_3086;
}
}
}
else 
{
returnVal2 = 259;
label_3070:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3111;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_3120:; 
goto label_3111;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3120;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3111;
}
else 
{
{
__VERIFIER_error();
}
label_3111:; 
 __return_3128 = returnVal2;
}
tmp = __return_3128;
 __return_3130 = tmp;
}
tmp = __return_3130;
 __return_3274 = tmp;
label_3274:; 
}
status4 = __return_3274;
goto label_2926;
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
goto label_5612;
}
else 
{
{
int __tmp_173 = DeviceObject;
int DeviceObject = __tmp_173;
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
 __return_5443 = 0;
goto label_5439;
}
else 
{
if (currentBuffer == 0)
{
 __return_5441 = 0;
goto label_5439;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_5438 = 0;
goto label_5439;
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_5369:; 
if (status5 < 0)
{
 __return_5435 = 0;
goto label_5439;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_5429:; 
 __return_5439 = returnValue;
label_5439:; 
}
else 
{
returnValue = 1;
goto label_5429;
}
tmp = __return_5439;
if (!(tmp == 0))
{
status7 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_5612;
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
label_5645:; 
myStatus = status7;
{
int __tmp_174 = Irp;
int __tmp_175 = 0;
int Irp = __tmp_174;
int PriorityBoost = __tmp_175;
if (s == NP)
{
s = DC;
goto label_5662;
}
else 
{
{
__VERIFIER_error();
}
label_5662:; 
}
 __return_5667 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5645;
}
}
else 
{
srb__TimeOutValue = 10;
srb__CdbLength = 10;
{
int __tmp_176 = deviceExtension;
int __tmp_177 = srb;
int __tmp_178 = Toc;
int __tmp_179 = sizeof__CDROM_TOC;
int Extension = __tmp_176;
int Srb = __tmp_177;
int Buffer = __tmp_178;
int BufferLength = __tmp_179;
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
goto label_5509;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_5509:; 
if (irp == 0)
{
 __return_5569 = -1073741670;
goto label_5570;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5516:; 
 __return_5570 = status1;
label_5570:; 
}
else 
{
{
int __tmp_182 = event;
int __tmp_183 = Executive;
int __tmp_184 = KernelMode;
int __tmp_185 = 0;
int __tmp_186 = 0;
int Object = __tmp_182;
int WaitReason = __tmp_183;
int WaitMode = __tmp_184;
int Alertable = __tmp_185;
int Timeout = __tmp_186;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5523;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5533;
}
}
else 
{
label_5523:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5533;
}
else 
{
label_5533:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5563 = 0;
goto label_5559;
}
else 
{
 __return_5559 = -1073741823;
label_5559:; 
}
status1 = ioStatus__Status;
goto label_5516;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5533;
}
}
}
}
status7 = __return_5570;
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
label_5615:; 
myStatus = status7;
{
int __tmp_180 = Irp;
int __tmp_181 = 0;
int Irp = __tmp_180;
int PriorityBoost = __tmp_181;
if (s == NP)
{
s = DC;
goto label_5632;
}
else 
{
{
__VERIFIER_error();
}
label_5632:; 
}
 __return_5637 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5615;
}
}
else 
{
goto label_5583;
}
}
else 
{
status7 = 0;
label_5583:; 
goto label_5576;
}
}
else 
{
status7 = 0;
label_5576:; 
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength > sizeof__CDROM_TOC)
{
bytesTransfered = sizeof__CDROM_TOC;
goto label_5593;
}
else 
{
bytesTransfered = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
label_5593:; 
__cil_tmp98 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp98 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength - TrackData__0;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_5602;
}
else 
{
tracksToReturn = tracksOnCd;
label_5602:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_5606;
}
else 
{
label_5606:; 
goto label_5612;
}
}
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
int __tmp_187 = event;
int __tmp_188 = Suspended;
int __tmp_189 = KernelMode;
int __tmp_190 = 0;
int __tmp_191 = 0;
int Object = __tmp_187;
int WaitReason = __tmp_188;
int WaitMode = __tmp_189;
int Alertable = __tmp_190;
int Timeout = __tmp_191;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5376;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5386;
}
}
else 
{
label_5376:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5386;
}
else 
{
label_5386:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5416 = 0;
goto label_5412;
}
else 
{
 __return_5412 = -1073741823;
label_5412:; 
}
status5 = ioStatus__Status;
goto label_5369;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5386;
}
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
goto label_5053;
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
label_5053:; 
Irp__IoStatus__Information = 0;
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_192 = deviceExtension;
int __tmp_193 = srb;
int __tmp_194 = 0;
int __tmp_195 = 0;
int Extension = __tmp_192;
int Srb = __tmp_193;
int Buffer = __tmp_194;
int BufferLength = __tmp_195;
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
goto label_5108;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_5108:; 
if (irp == 0)
{
 __return_5168 = -1073741670;
goto label_5169;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5115:; 
 __return_5169 = status1;
label_5169:; 
}
else 
{
{
int __tmp_209 = event;
int __tmp_210 = Executive;
int __tmp_211 = KernelMode;
int __tmp_212 = 0;
int __tmp_213 = 0;
int Object = __tmp_209;
int WaitReason = __tmp_210;
int WaitMode = __tmp_211;
int Alertable = __tmp_212;
int Timeout = __tmp_213;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5122;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5132;
}
}
else 
{
label_5122:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5132;
}
else 
{
label_5132:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5162 = 0;
goto label_5158;
}
else 
{
 __return_5158 = -1073741823;
label_5158:; 
}
status1 = ioStatus__Status;
goto label_5115;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5132;
}
}
}
}
status7 = __return_5169;
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
label_5308:; 
myStatus = status7;
{
int __tmp_196 = Irp;
int __tmp_197 = 0;
int Irp = __tmp_196;
int PriorityBoost = __tmp_197;
if (s == NP)
{
s = DC;
goto label_5325;
}
else 
{
{
__VERIFIER_error();
}
label_5325:; 
}
 __return_5330 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5308;
}
}
else 
{
if (currentIrpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CDROM_PLAY_AUDIO_MSF)
{
status7 = -1073741820;
goto label_5612;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_198 = deviceExtension;
int __tmp_199 = srb;
int __tmp_200 = 0;
int __tmp_201 = 0;
int Extension = __tmp_198;
int Srb = __tmp_199;
int Buffer = __tmp_200;
int BufferLength = __tmp_201;
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
goto label_5238;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_5238:; 
if (irp == 0)
{
 __return_5298 = -1073741670;
goto label_5299;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5245:; 
 __return_5299 = status1;
label_5299:; 
}
else 
{
{
int __tmp_204 = event;
int __tmp_205 = Executive;
int __tmp_206 = KernelMode;
int __tmp_207 = 0;
int __tmp_208 = 0;
int Object = __tmp_204;
int WaitReason = __tmp_205;
int WaitMode = __tmp_206;
int Alertable = __tmp_207;
int Timeout = __tmp_208;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_5252;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5262;
}
}
else 
{
label_5252:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5262;
}
else 
{
label_5262:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5292 = 0;
goto label_5288;
}
else 
{
 __return_5288 = -1073741823;
label_5288:; 
}
status1 = ioStatus__Status;
goto label_5245;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5262;
}
}
}
}
status7 = __return_5299;
label_5675:; 
__cil_tmp116 = (unsigned long)status7;
if (!(__cil_tmp116 == -2147483626))
{
label_5678:; 
myStatus = status7;
{
int __tmp_202 = Irp;
int __tmp_203 = 0;
int Irp = __tmp_202;
int PriorityBoost = __tmp_203;
if (s == NP)
{
s = DC;
goto label_5695;
}
else 
{
{
__VERIFIER_error();
}
label_5695:; 
}
 __return_5700 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5678;
}
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
goto label_5612;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_214 = deviceExtension;
int __tmp_215 = srb;
int __tmp_216 = 0;
int __tmp_217 = 0;
int Extension = __tmp_214;
int Srb = __tmp_215;
int Buffer = __tmp_216;
int BufferLength = __tmp_217;
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
goto label_4971;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_4971:; 
if (irp == 0)
{
 __return_5031 = -1073741670;
goto label_5032;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4978:; 
 __return_5032 = status1;
label_5032:; 
}
else 
{
{
int __tmp_218 = event;
int __tmp_219 = Executive;
int __tmp_220 = KernelMode;
int __tmp_221 = 0;
int __tmp_222 = 0;
int Object = __tmp_218;
int WaitReason = __tmp_219;
int WaitMode = __tmp_220;
int Alertable = __tmp_221;
int Timeout = __tmp_222;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4985;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4995;
}
}
else 
{
label_4985:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4995;
}
else 
{
label_4995:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5025 = 0;
goto label_5021;
}
else 
{
 __return_5021 = -1073741823;
label_5021:; 
}
status1 = ioStatus__Status;
goto label_4978;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4995;
}
}
}
}
status7 = __return_5032;
if (status7 < 0)
{
__cil_tmp105 = (unsigned long)status7;
if (!(__cil_tmp105 == -1073741808))
{
goto label_5035;
}
else 
{
status7 = -1073741803;
goto label_5035;
}
}
else 
{
label_5035:; 
goto label_5612;
}
}
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
label_4890:; 
myStatus = status7;
{
int __tmp_223 = Irp;
int __tmp_224 = 0;
int Irp = __tmp_223;
int PriorityBoost = __tmp_224;
if (s == NP)
{
s = DC;
goto label_4907;
}
else 
{
{
__VERIFIER_error();
}
label_4907:; 
}
 __return_4912 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4890;
}
}
else 
{
if (!(deviceExtension__Paused == 1))
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_225 = deviceExtension;
int __tmp_226 = srb;
int __tmp_227 = SubQPtr;
int __tmp_228 = sizeof__SUB_Q_CHANNEL_DATA;
int Extension = __tmp_225;
int Srb = __tmp_226;
int Buffer = __tmp_227;
int BufferLength = __tmp_228;
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
goto label_4624;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_4624:; 
if (irp == 0)
{
 __return_4684 = -1073741670;
goto label_4685;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4631:; 
 __return_4685 = status1;
label_4685:; 
}
else 
{
{
int __tmp_242 = event;
int __tmp_243 = Executive;
int __tmp_244 = KernelMode;
int __tmp_245 = 0;
int __tmp_246 = 0;
int Object = __tmp_242;
int WaitReason = __tmp_243;
int WaitMode = __tmp_244;
int Alertable = __tmp_245;
int Timeout = __tmp_246;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4638;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4648;
}
}
else 
{
label_4638:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4648;
}
else 
{
label_4648:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4678 = 0;
goto label_4674;
}
else 
{
 __return_4674 = -1073741823;
label_4674:; 
}
status1 = ioStatus__Status;
goto label_4631;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4648;
}
}
}
}
status7 = __return_4685;
if (status7 < 0)
{
__cil_tmp109 = (unsigned long)status7;
if (!(__cil_tmp109 == -2147483626))
{
label_4836:; 
myStatus = status7;
{
int __tmp_229 = Irp;
int __tmp_230 = 0;
int Irp = __tmp_229;
int PriorityBoost = __tmp_230;
if (s == NP)
{
s = DC;
goto label_4853;
}
else 
{
{
__VERIFIER_error();
}
label_4853:; 
}
 __return_4858 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4836;
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_231 = deviceExtension;
int __tmp_232 = srb;
int __tmp_233 = 0;
int __tmp_234 = 0;
int Extension = __tmp_231;
int Srb = __tmp_232;
int Buffer = __tmp_233;
int BufferLength = __tmp_234;
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
goto label_4741;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_4741:; 
if (irp == 0)
{
 __return_4801 = -1073741670;
goto label_4802;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4748:; 
 __return_4802 = status1;
label_4802:; 
}
else 
{
{
int __tmp_237 = event;
int __tmp_238 = Executive;
int __tmp_239 = KernelMode;
int __tmp_240 = 0;
int __tmp_241 = 0;
int Object = __tmp_237;
int WaitReason = __tmp_238;
int WaitMode = __tmp_239;
int Alertable = __tmp_240;
int Timeout = __tmp_241;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4755;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4765;
}
}
else 
{
label_4755:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4765;
}
else 
{
label_4765:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4795 = 0;
goto label_4791;
}
else 
{
 __return_4791 = -1073741823;
label_4791:; 
}
status1 = ioStatus__Status;
goto label_4748;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4765;
}
}
}
}
status7 = __return_4802;
if (status7 < 0)
{
__cil_tmp111 = (unsigned long)status7;
if (!(__cil_tmp111 == -2147483626))
{
label_4810:; 
myStatus = status7;
{
int __tmp_235 = Irp;
int __tmp_236 = 0;
int Irp = __tmp_235;
int PriorityBoost = __tmp_236;
if (s == NP)
{
s = DC;
goto label_4827;
}
else 
{
{
__VERIFIER_error();
}
label_4827:; 
}
 __return_4832 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4810;
}
}
else 
{
goto label_5612;
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
status7 = 0;
__cil_tmp107 = (unsigned long)status7;
if (!(__cil_tmp107 == -2147483626))
{
label_4863:; 
myStatus = status7;
{
int __tmp_247 = Irp;
int __tmp_248 = 0;
int Irp = __tmp_247;
int PriorityBoost = __tmp_248;
if (s == NP)
{
s = DC;
goto label_4880;
}
else 
{
{
__VERIFIER_error();
}
label_4880:; 
}
 __return_4885 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4863;
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
int __tmp_249 = deviceExtension;
int __tmp_250 = srb;
int __tmp_251 = 0;
int __tmp_252 = 0;
int Extension = __tmp_249;
int Srb = __tmp_250;
int Buffer = __tmp_251;
int BufferLength = __tmp_252;
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
goto label_4462;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_4462:; 
if (irp == 0)
{
 __return_4522 = -1073741670;
goto label_4523;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4469:; 
 __return_4523 = status1;
label_4523:; 
}
else 
{
{
int __tmp_253 = event;
int __tmp_254 = Executive;
int __tmp_255 = KernelMode;
int __tmp_256 = 0;
int __tmp_257 = 0;
int Object = __tmp_253;
int WaitReason = __tmp_254;
int WaitMode = __tmp_255;
int Alertable = __tmp_256;
int Timeout = __tmp_257;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4476;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4486;
}
}
else 
{
label_4476:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4486;
}
else 
{
label_4486:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4516 = 0;
goto label_4512;
}
else 
{
 __return_4512 = -1073741823;
label_4512:; 
}
status1 = ioStatus__Status;
goto label_4469;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4486;
}
}
}
}
status7 = __return_4523;
if (status7 >= 0)
{
deviceExtension__PlayActive = 1;
deviceExtension__Paused = 0;
goto label_4526;
}
else 
{
label_4526:; 
goto label_5612;
}
}
}
}
}
else 
{
status7 = -1073741823;
__cil_tmp112 = (unsigned long)status7;
if (!(__cil_tmp112 == -2147483626))
{
label_4538:; 
myStatus = status7;
{
int __tmp_258 = Irp;
int __tmp_259 = 0;
int Irp = __tmp_258;
int PriorityBoost = __tmp_259;
if (s == NP)
{
s = DC;
goto label_4555;
}
else 
{
{
__VERIFIER_error();
}
label_4555:; 
}
 __return_4560 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4538;
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
goto label_5612;
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
label_4375:; 
myStatus = status7;
{
int __tmp_260 = Irp;
int __tmp_261 = 0;
int Irp = __tmp_260;
int PriorityBoost = __tmp_261;
if (s == NP)
{
s = DC;
goto label_4392;
}
else 
{
{
__VERIFIER_error();
}
label_4392:; 
}
 __return_4397 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4375;
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
label_4345:; 
myStatus = status7;
{
int __tmp_262 = Irp;
int __tmp_263 = 0;
int Irp = __tmp_262;
int PriorityBoost = __tmp_263;
if (s == NP)
{
s = DC;
goto label_4362;
}
else 
{
{
__VERIFIER_error();
}
label_4362:; 
}
 __return_4367 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4345;
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_264 = deviceExtension;
int __tmp_265 = srb;
int __tmp_266 = SubQPtr___0;
int __tmp_267 = sizeof__SUB_Q_CHANNEL_DATA;
int Extension = __tmp_264;
int Srb = __tmp_265;
int Buffer = __tmp_266;
int BufferLength = __tmp_267;
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
goto label_4261;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_4261:; 
if (irp == 0)
{
 __return_4321 = -1073741670;
goto label_4322;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4268:; 
 __return_4322 = status1;
label_4322:; 
}
else 
{
{
int __tmp_268 = event;
int __tmp_269 = Executive;
int __tmp_270 = KernelMode;
int __tmp_271 = 0;
int __tmp_272 = 0;
int Object = __tmp_268;
int WaitReason = __tmp_269;
int WaitMode = __tmp_270;
int Alertable = __tmp_271;
int Timeout = __tmp_272;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4275;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4285;
}
}
else 
{
label_4275:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4285;
}
else 
{
label_4285:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4315 = 0;
goto label_4311;
}
else 
{
 __return_4311 = -1073741823;
label_4311:; 
}
status1 = ioStatus__Status;
goto label_4268;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4285;
}
}
}
}
status7 = __return_4322;
if (status7 >= 0)
{
if (!(deviceExtension__Paused == 1))
{
label_4330:; 
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_4328;
}
else 
{
deviceExtension__PlayActive = 0;
goto label_4330;
}
}
else 
{
Irp__IoStatus__Information = 0;
label_4328:; 
goto label_5612;
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
int __tmp_273 = deviceExtension;
int __tmp_274 = srb;
int __tmp_275 = 0;
int __tmp_276 = 0;
int Extension = __tmp_273;
int Srb = __tmp_274;
int Buffer = __tmp_275;
int BufferLength = __tmp_276;
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
goto label_4134;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_4134:; 
if (irp == 0)
{
 __return_4194 = -1073741670;
goto label_4195;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4141:; 
 __return_4195 = status1;
label_4195:; 
}
else 
{
{
int __tmp_277 = event;
int __tmp_278 = Executive;
int __tmp_279 = KernelMode;
int __tmp_280 = 0;
int __tmp_281 = 0;
int Object = __tmp_277;
int WaitReason = __tmp_278;
int WaitMode = __tmp_279;
int Alertable = __tmp_280;
int Timeout = __tmp_281;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_4148;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4158;
}
}
else 
{
label_4148:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4158;
}
else 
{
label_4158:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4188 = 0;
goto label_4184;
}
else 
{
 __return_4184 = -1073741823;
label_4184:; 
}
status1 = ioStatus__Status;
goto label_4141;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4158;
}
}
}
}
status7 = __return_4195;
goto label_5612;
}
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
goto label_4070;
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
label_4070:; 
goto label_4072;
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
label_4072:; 
Irp__IoStatus__Information = 0;
status7 = -1073741808;
label_5612:; 
goto label_5675;
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
int __tmp_282 = DeviceObject;
int DeviceObject = __tmp_282;
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
 __return_3855 = 0;
goto label_3851;
}
else 
{
if (currentBuffer == 0)
{
 __return_3853 = 0;
goto label_3851;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_3850 = 0;
goto label_3851;
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_3781:; 
if (status5 < 0)
{
 __return_3847 = 0;
goto label_3851;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_3841:; 
 __return_3851 = returnValue;
label_3851:; 
}
else 
{
returnValue = 1;
goto label_3841;
}
tmp___1 = __return_3851;
if (!(tmp___1 == 1))
{
deviceExtension__PlayActive = 0;
{
int __tmp_283 = DeviceObject;
int __tmp_284 = Irp;
int DeviceObject = __tmp_283;
int Irp = __tmp_284;
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
goto label_3879;
}
else 
{
{
__VERIFIER_error();
}
label_3879:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_285 = deviceExtension__TargetDeviceObject;
int __tmp_286 = Irp;
int DeviceObject = __tmp_285;
int Irp = __tmp_286;
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
int __tmp_287 = DeviceObject;
int __tmp_288 = Irp;
int __tmp_289 = lcontext;
int DeviceObject = __tmp_287;
int Irp = __tmp_288;
int Context = __tmp_289;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_3909:; 
if (myStatus >= 0)
{
{
int __tmp_290 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_290;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_3932;
}
else 
{
label_3932:; 
}
goto label_3925;
}
}
else 
{
label_3925:; 
 __return_3940 = myStatus;
}
compRetStatus = __return_3940;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_3899;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_3958;
}
else 
{
{
__VERIFIER_error();
}
label_3958:; 
}
goto label_3899;
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
goto label_3909;
}
else 
{
pended = 1;
goto label_3909;
}
}
}
}
else 
{
label_3899:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_3986;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_3986:; 
goto label_3970;
}
else 
{
returnVal2 = -1073741823;
goto label_3986;
}
}
}
else 
{
returnVal2 = 259;
label_3970:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4011;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_4020:; 
goto label_4011;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4020;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_4011;
}
else 
{
{
__VERIFIER_error();
}
label_4011:; 
 __return_4028 = returnVal2;
}
tmp = __return_4028;
 __return_4030 = tmp;
}
tmp___0 = __return_4030;
 __return_4032 = tmp___0;
goto label_5638;
}
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
label_4042:; 
myStatus = status7;
{
int __tmp_291 = Irp;
int __tmp_292 = 0;
int Irp = __tmp_291;
int PriorityBoost = __tmp_292;
if (s == NP)
{
s = DC;
goto label_4059;
}
else 
{
{
__VERIFIER_error();
}
label_4059:; 
}
 __return_4064 = status7;
goto label_5638;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4042;
}
}
}
}
else 
{
{
int __tmp_293 = event;
int __tmp_294 = Suspended;
int __tmp_295 = KernelMode;
int __tmp_296 = 0;
int __tmp_297 = 0;
int Object = __tmp_293;
int WaitReason = __tmp_294;
int WaitMode = __tmp_295;
int Alertable = __tmp_296;
int Timeout = __tmp_297;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_3788;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_3798;
}
}
else 
{
label_3788:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3798;
}
else 
{
label_3798:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_3828 = 0;
goto label_3824;
}
else 
{
 __return_3824 = -1073741823;
label_3824:; 
}
status5 = ioStatus__Status;
goto label_3781;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_3798;
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
int __tmp_298 = DeviceObject;
int __tmp_299 = Irp;
int DeviceObject = __tmp_298;
int Irp = __tmp_299;
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
goto label_3592;
}
else 
{
{
__VERIFIER_error();
}
label_3592:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_300 = deviceExtension__TargetDeviceObject;
int __tmp_301 = Irp;
int DeviceObject = __tmp_300;
int Irp = __tmp_301;
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
int __tmp_302 = DeviceObject;
int __tmp_303 = Irp;
int __tmp_304 = lcontext;
int DeviceObject = __tmp_302;
int Irp = __tmp_303;
int Context = __tmp_304;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_3622:; 
if (myStatus >= 0)
{
{
int __tmp_305 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_305;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_3645;
}
else 
{
label_3645:; 
}
goto label_3638;
}
}
else 
{
label_3638:; 
 __return_3653 = myStatus;
}
compRetStatus = __return_3653;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_3612;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_3671;
}
else 
{
{
__VERIFIER_error();
}
label_3671:; 
}
goto label_3612;
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
goto label_3622;
}
else 
{
pended = 1;
goto label_3622;
}
}
}
}
else 
{
label_3612:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_3699;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_3699:; 
goto label_3683;
}
else 
{
returnVal2 = -1073741823;
goto label_3699;
}
}
}
else 
{
returnVal2 = 259;
label_3683:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3724;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_3733:; 
goto label_3724;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3733;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3724;
}
else 
{
{
__VERIFIER_error();
}
label_3724:; 
 __return_3741 = returnVal2;
}
tmp = __return_3741;
 __return_3743 = tmp;
}
tmp___2 = __return_3743;
 __return_5638 = tmp___2;
label_5638:; 
}
status4 = __return_5638;
goto label_2926;
}
}
}
}
}
}
}
}
}
}
}
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
int __tmp_306 = DeviceObject;
int __tmp_307 = Irp;
int DeviceObject = __tmp_306;
int Irp = __tmp_307;
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
int __tmp_308 = DeviceObject;
int DeviceObject = __tmp_308;
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
 __return_7285 = 0;
goto label_7281;
}
else 
{
if (currentBuffer == 0)
{
 __return_7283 = 0;
goto label_7281;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_7280 = 0;
goto label_7281;
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_7211:; 
if (status5 < 0)
{
 __return_7277 = 0;
goto label_7281;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_7271:; 
 __return_7281 = returnValue;
label_7281:; 
}
else 
{
returnValue = 1;
goto label_7271;
}
tmp = __return_7281;
if (!(tmp == 0))
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_7295;
}
else 
{
if (!(currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength == 0))
{
status6 = -1073741789;
Irp__IoStatus__Information = 0;
goto label_7295;
}
else 
{
if (lastSession == 0)
{
status6 = -1073741670;
Irp__IoStatus__Information = 0;
{
int __tmp_309 = status6;
int __tmp_310 = Irp;
int __tmp_311 = deviceExtension__TargetDeviceObject;
int status = __tmp_309;
int Irp = __tmp_310;
int deviceExtension__TargetDeviceObject = __tmp_311;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_312 = Irp;
int __tmp_313 = 0;
int Irp = __tmp_312;
int PriorityBoost = __tmp_313;
if (s == NP)
{
s = DC;
goto label_7517;
}
else 
{
{
__VERIFIER_error();
}
label_7517:; 
}
 __return_7522 = status;
}
tmp___0 = __return_7522;
 __return_7524 = tmp___0;
goto label_7468;
}
}
else 
{
srb__CdbLength = 10;
cdb__CDB10__OperationCode = 38;
srb__TimeOutValue = 10;
{
int __tmp_314 = deviceExtension;
int __tmp_315 = srb;
int __tmp_316 = lastSession;
int __tmp_317 = sizeof__READ_CAPACITY_DATA;
int Extension = __tmp_314;
int Srb = __tmp_315;
int Buffer = __tmp_316;
int BufferLength = __tmp_317;
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
goto label_7361;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_7361:; 
if (irp == 0)
{
 __return_7421 = -1073741670;
goto label_7422;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7368:; 
 __return_7422 = status1;
label_7422:; 
}
else 
{
{
int __tmp_323 = event;
int __tmp_324 = Executive;
int __tmp_325 = KernelMode;
int __tmp_326 = 0;
int __tmp_327 = 0;
int Object = __tmp_323;
int WaitReason = __tmp_324;
int WaitMode = __tmp_325;
int Alertable = __tmp_326;
int Timeout = __tmp_327;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7375;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7385;
}
}
else 
{
label_7375:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7385;
}
else 
{
label_7385:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7415 = 0;
goto label_7411;
}
else 
{
 __return_7411 = -1073741823;
label_7411:; 
}
status1 = ioStatus__Status;
goto label_7368;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7385;
}
}
}
}
status6 = __return_7422;
if (status6 < 0)
{
Irp__IoStatus__Information = 0;
{
int __tmp_318 = status6;
int __tmp_319 = Irp;
int __tmp_320 = deviceExtension__TargetDeviceObject;
int status = __tmp_318;
int Irp = __tmp_319;
int deviceExtension__TargetDeviceObject = __tmp_320;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_321 = Irp;
int __tmp_322 = 0;
int Irp = __tmp_321;
int PriorityBoost = __tmp_322;
if (s == NP)
{
s = DC;
goto label_7488;
}
else 
{
{
__VERIFIER_error();
}
label_7488:; 
}
 __return_7493 = status;
}
tmp___1 = __return_7493;
 __return_7495 = tmp___1;
goto label_7468;
}
}
else 
{
status6 = 0;
Irp__IoStatus__Information = bytesTransfered;
if (lastSession__LogicalBlockAddress == 0)
{
goto label_7295;
}
else 
{
cdaudioDataOut__FirstTrack = 1;
cdaudioDataOut__LastTrack = 2;
goto label_7295;
}
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
int __tmp_328 = event;
int __tmp_329 = Suspended;
int __tmp_330 = KernelMode;
int __tmp_331 = 0;
int __tmp_332 = 0;
int Object = __tmp_328;
int WaitReason = __tmp_329;
int WaitMode = __tmp_330;
int Alertable = __tmp_331;
int Timeout = __tmp_332;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7218;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7228;
}
}
else 
{
label_7218:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7228;
}
else 
{
label_7228:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7258 = 0;
goto label_7254;
}
else 
{
 __return_7254 = -1073741823;
label_7254:; 
}
status5 = ioStatus__Status;
goto label_7211;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7228;
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
goto label_7295;
}
else 
{
{
int __tmp_333 = DeviceObject;
int DeviceObject = __tmp_333;
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
 __return_6956 = 0;
goto label_6952;
}
else 
{
if (currentBuffer == 0)
{
 __return_6954 = 0;
goto label_6952;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_6951 = 0;
goto label_6952;
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_6882:; 
if (status5 < 0)
{
 __return_6948 = 0;
goto label_6952;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_6942:; 
 __return_6952 = returnValue;
label_6952:; 
}
else 
{
returnValue = 1;
goto label_6942;
}
tmp___2 = __return_6952;
if (!(tmp___2 == 0))
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_7295;
}
else 
{
if (Toc == 0)
{
status6 = -1073741670;
Irp__IoStatus__Information = 0;
{
int __tmp_334 = status6;
int __tmp_335 = Irp;
int __tmp_336 = deviceExtension__TargetDeviceObject;
int status = __tmp_334;
int Irp = __tmp_335;
int deviceExtension__TargetDeviceObject = __tmp_336;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_337 = Irp;
int __tmp_338 = 0;
int Irp = __tmp_337;
int PriorityBoost = __tmp_338;
if (s == NP)
{
s = DC;
goto label_7168;
}
else 
{
{
__VERIFIER_error();
}
label_7168:; 
}
 __return_7173 = status;
}
tmp___3 = __return_7173;
 __return_7175 = tmp___3;
goto label_7468;
}
}
else 
{
srb__TimeOutValue = 10;
srb__CdbLength = 10;
{
int __tmp_339 = deviceExtension;
int __tmp_340 = srb;
int __tmp_341 = Toc;
int __tmp_342 = sizeof__CDROM_TOC;
int Extension = __tmp_339;
int Srb = __tmp_340;
int Buffer = __tmp_341;
int BufferLength = __tmp_342;
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
goto label_7021;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_7021:; 
if (irp == 0)
{
 __return_7081 = -1073741670;
goto label_7082;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7028:; 
 __return_7082 = status1;
label_7082:; 
}
else 
{
{
int __tmp_348 = event;
int __tmp_349 = Executive;
int __tmp_350 = KernelMode;
int __tmp_351 = 0;
int __tmp_352 = 0;
int Object = __tmp_348;
int WaitReason = __tmp_349;
int WaitMode = __tmp_350;
int Alertable = __tmp_351;
int Timeout = __tmp_352;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_7035;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7045;
}
}
else 
{
label_7035:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7045;
}
else 
{
label_7045:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7075 = 0;
goto label_7071;
}
else 
{
 __return_7071 = -1073741823;
label_7071:; 
}
status1 = ioStatus__Status;
goto label_7028;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7045;
}
}
}
}
status6 = __return_7082;
if (status6 >= 0)
{
__cil_tmp107 = (unsigned long)status6;
if (__cil_tmp107 != -1073741764)
{
status6 = 0;
goto label_7129;
}
else 
{
goto label_7086;
}
}
else 
{
label_7086:; 
__cil_tmp108 = (unsigned long)status6;
if (__cil_tmp108 != -1073741764)
{
Irp__IoStatus__Information = 0;
{
int __tmp_343 = status6;
int __tmp_344 = Irp;
int __tmp_345 = deviceExtension__TargetDeviceObject;
int status = __tmp_343;
int Irp = __tmp_344;
int deviceExtension__TargetDeviceObject = __tmp_345;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_346 = Irp;
int __tmp_347 = 0;
int Irp = __tmp_346;
int PriorityBoost = __tmp_347;
if (s == NP)
{
s = DC;
goto label_7116;
}
else 
{
{
__VERIFIER_error();
}
label_7116:; 
}
 __return_7121 = status;
}
tmp___4 = __return_7121;
 __return_7123 = tmp___4;
goto label_7468;
}
}
else 
{
label_7129:; 
__cil_tmp109 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp109 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_7136;
}
else 
{
tracksToReturn = tracksOnCd;
label_7136:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_7140;
}
else 
{
label_7140:; 
goto label_7295;
}
}
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
int __tmp_353 = event;
int __tmp_354 = Suspended;
int __tmp_355 = KernelMode;
int __tmp_356 = 0;
int __tmp_357 = 0;
int Object = __tmp_353;
int WaitReason = __tmp_354;
int WaitMode = __tmp_355;
int Alertable = __tmp_356;
int Timeout = __tmp_357;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_6889;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6899;
}
}
else 
{
label_6889:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6899;
}
else 
{
label_6899:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6929 = 0;
goto label_6925;
}
else 
{
 __return_6925 = -1073741823;
label_6925:; 
}
status5 = ioStatus__Status;
goto label_6882;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6899;
}
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
goto label_7295;
}
else 
{
if (SubQPtr == 0)
{
status6 = -1073741670;
Irp__IoStatus__Information = 0;
{
int __tmp_358 = status6;
int __tmp_359 = Irp;
int __tmp_360 = deviceExtension__TargetDeviceObject;
int status = __tmp_358;
int Irp = __tmp_359;
int deviceExtension__TargetDeviceObject = __tmp_360;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_361 = Irp;
int __tmp_362 = 0;
int Irp = __tmp_361;
int PriorityBoost = __tmp_362;
if (s == NP)
{
s = DC;
goto label_6825;
}
else 
{
{
__VERIFIER_error();
}
label_6825:; 
}
 __return_6830 = status;
}
tmp___5 = __return_6830;
 __return_6832 = tmp___5;
goto label_7468;
}
}
else 
{
if (userPtr__Format != 1)
{
status6 = -1073741823;
Irp__IoStatus__Information = 0;
{
int __tmp_363 = status6;
int __tmp_364 = Irp;
int __tmp_365 = deviceExtension__TargetDeviceObject;
int status = __tmp_363;
int Irp = __tmp_364;
int deviceExtension__TargetDeviceObject = __tmp_365;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_366 = Irp;
int __tmp_367 = 0;
int Irp = __tmp_366;
int PriorityBoost = __tmp_367;
if (s == NP)
{
s = DC;
goto label_6795;
}
else 
{
{
__VERIFIER_error();
}
label_6795:; 
}
 __return_6800 = status;
}
tmp___6 = __return_6800;
 __return_6802 = tmp___6;
goto label_7468;
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_368 = deviceExtension;
int __tmp_369 = srb;
int __tmp_370 = SubQPtr;
int __tmp_371 = sizeof__SUB_Q_CURRENT_POSITION;
int Extension = __tmp_368;
int Srb = __tmp_369;
int Buffer = __tmp_370;
int BufferLength = __tmp_371;
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
goto label_6702;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_6702:; 
if (irp == 0)
{
 __return_6762 = -1073741670;
goto label_6763;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6709:; 
 __return_6763 = status1;
label_6763:; 
}
else 
{
{
int __tmp_372 = event;
int __tmp_373 = Executive;
int __tmp_374 = KernelMode;
int __tmp_375 = 0;
int __tmp_376 = 0;
int Object = __tmp_372;
int WaitReason = __tmp_373;
int WaitMode = __tmp_374;
int Alertable = __tmp_375;
int Timeout = __tmp_376;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_6716;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6726;
}
}
else 
{
label_6716:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6726;
}
else 
{
label_6726:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6756 = 0;
goto label_6752;
}
else 
{
 __return_6752 = -1073741823;
label_6752:; 
}
status1 = ioStatus__Status;
goto label_6709;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6726;
}
}
}
}
status6 = __return_6763;
if (status6 >= 0)
{
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_6769;
}
else 
{
Irp__IoStatus__Information = 0;
label_6769:; 
goto label_7295;
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
goto label_7295;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_377 = deviceExtension;
int __tmp_378 = srb;
int __tmp_379 = 0;
int __tmp_380 = 0;
int Extension = __tmp_377;
int Srb = __tmp_378;
int Buffer = __tmp_379;
int BufferLength = __tmp_380;
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
goto label_6570;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_6570:; 
if (irp == 0)
{
 __return_6630 = -1073741670;
goto label_6631;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6577:; 
 __return_6631 = status1;
label_6631:; 
}
else 
{
{
int __tmp_381 = event;
int __tmp_382 = Executive;
int __tmp_383 = KernelMode;
int __tmp_384 = 0;
int __tmp_385 = 0;
int Object = __tmp_381;
int WaitReason = __tmp_382;
int WaitMode = __tmp_383;
int Alertable = __tmp_384;
int Timeout = __tmp_385;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_6584;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6594;
}
}
else 
{
label_6584:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6594;
}
else 
{
label_6594:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6624 = 0;
goto label_6620;
}
else 
{
 __return_6620 = -1073741823;
label_6620:; 
}
status1 = ioStatus__Status;
goto label_6577;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6594;
}
}
}
}
status6 = __return_6631;
goto label_7442;
}
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
goto label_7295;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_386 = deviceExtension;
int __tmp_387 = srb;
int __tmp_388 = 0;
int __tmp_389 = 0;
int Extension = __tmp_386;
int Srb = __tmp_387;
int Buffer = __tmp_388;
int BufferLength = __tmp_389;
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
goto label_6442;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_6442:; 
if (irp == 0)
{
 __return_6502 = -1073741670;
goto label_6503;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6449:; 
 __return_6503 = status1;
label_6503:; 
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
goto label_6456;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6466;
}
}
else 
{
label_6456:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6466;
}
else 
{
label_6466:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6496 = 0;
goto label_6492;
}
else 
{
 __return_6492 = -1073741823;
label_6492:; 
}
status1 = ioStatus__Status;
goto label_6449;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6466;
}
}
}
}
status6 = __return_6503;
label_7442:; 
{
int __tmp_390 = status6;
int __tmp_391 = Irp;
int __tmp_392 = deviceExtension__TargetDeviceObject;
int status = __tmp_390;
int Irp = __tmp_391;
int deviceExtension__TargetDeviceObject = __tmp_392;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_393 = Irp;
int __tmp_394 = 0;
int Irp = __tmp_393;
int PriorityBoost = __tmp_394;
if (s == NP)
{
s = DC;
goto label_7460;
}
else 
{
{
__VERIFIER_error();
}
label_7460:; 
}
 __return_7465 = status;
}
tmp___8 = __return_7465;
 __return_7467 = tmp___8;
goto label_7468;
}
}
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
int __tmp_400 = deviceExtension;
int __tmp_401 = srb;
int __tmp_402 = 0;
int __tmp_403 = 0;
int Extension = __tmp_400;
int Srb = __tmp_401;
int Buffer = __tmp_402;
int BufferLength = __tmp_403;
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
goto label_6318;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_6318:; 
if (irp == 0)
{
 __return_6378 = -1073741670;
goto label_6379;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6325:; 
 __return_6379 = status1;
label_6379:; 
}
else 
{
{
int __tmp_404 = event;
int __tmp_405 = Executive;
int __tmp_406 = KernelMode;
int __tmp_407 = 0;
int __tmp_408 = 0;
int Object = __tmp_404;
int WaitReason = __tmp_405;
int WaitMode = __tmp_406;
int Alertable = __tmp_407;
int Timeout = __tmp_408;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_6332;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6342;
}
}
else 
{
label_6332:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6342;
}
else 
{
label_6342:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6372 = 0;
goto label_6368;
}
else 
{
 __return_6368 = -1073741823;
label_6368:; 
}
status1 = ioStatus__Status;
goto label_6325;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6342;
}
}
}
}
status6 = __return_6379;
goto label_7295;
}
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
goto label_6254;
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
label_6254:; 
goto label_6256;
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
label_6256:; 
Irp__IoStatus__Information = 0;
status6 = -1073741808;
label_7295:; 
goto label_7442;
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
int __tmp_409 = DeviceObject;
int DeviceObject = __tmp_409;
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
 __return_6075 = 0;
goto label_6071;
}
else 
{
if (currentBuffer == 0)
{
 __return_6073 = 0;
goto label_6071;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_6070 = 0;
goto label_6071;
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_6001:; 
if (status5 < 0)
{
 __return_6067 = 0;
goto label_6071;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_6061:; 
 __return_6071 = returnValue;
label_6071:; 
}
else 
{
returnValue = 1;
goto label_6061;
}
goto label_5965;
}
}
else 
{
{
int __tmp_410 = event;
int __tmp_411 = Suspended;
int __tmp_412 = KernelMode;
int __tmp_413 = 0;
int __tmp_414 = 0;
int Object = __tmp_410;
int WaitReason = __tmp_411;
int WaitMode = __tmp_412;
int Alertable = __tmp_413;
int Timeout = __tmp_414;
if (s == MPR3)
{
if (!(setEventCalled == 1))
{
goto label_6008;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6018;
}
}
else 
{
label_6008:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6018;
}
else 
{
label_6018:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6048 = 0;
goto label_6044;
}
else 
{
 __return_6044 = -1073741823;
label_6044:; 
}
status5 = ioStatus__Status;
goto label_6001;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6018;
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
label_5965:; 
{
int __tmp_415 = DeviceObject;
int __tmp_416 = Irp;
int DeviceObject = __tmp_415;
int Irp = __tmp_416;
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
goto label_6095;
}
else 
{
{
__VERIFIER_error();
}
label_6095:; 
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_417 = deviceExtension__TargetDeviceObject;
int __tmp_418 = Irp;
int DeviceObject = __tmp_417;
int Irp = __tmp_418;
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
int __tmp_419 = DeviceObject;
int __tmp_420 = Irp;
int __tmp_421 = lcontext;
int DeviceObject = __tmp_419;
int Irp = __tmp_420;
int Context = __tmp_421;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (Irp__PendingReturned == 0)
{
label_6125:; 
if (myStatus >= 0)
{
{
int __tmp_422 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_422;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_6148;
}
else 
{
label_6148:; 
}
goto label_6141;
}
}
else 
{
label_6141:; 
 __return_6156 = myStatus;
}
compRetStatus = __return_6156;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_6115;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_6174;
}
else 
{
{
__VERIFIER_error();
}
label_6174:; 
}
goto label_6115;
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
goto label_6125;
}
else 
{
pended = 1;
goto label_6125;
}
}
}
}
else 
{
label_6115:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_6202;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_6202:; 
goto label_6186;
}
else 
{
returnVal2 = -1073741823;
goto label_6202;
}
}
}
else 
{
returnVal2 = 259;
label_6186:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6227;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_6236:; 
goto label_6227;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6236;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_6227;
}
else 
{
{
__VERIFIER_error();
}
label_6227:; 
 __return_6244 = returnVal2;
}
tmp = __return_6244;
 __return_6246 = tmp;
}
tmp___7 = __return_6246;
 __return_7468 = tmp___7;
label_7468:; 
}
status4 = __return_7468;
goto label_2926;
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_7562:; 
if (!(pended == 1))
{
label_7576:; 
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
goto label_7588;
}
else 
{
goto label_7602;
}
}
else 
{
goto label_7602;
}
}
else 
{
label_7602:; 
if (pended != 1)
{
if (s == DC)
{
if (!(status10 == 259))
{
label_7652:; 
goto label_7588;
}
else 
{
{
__VERIFIER_error();
}
goto label_7652;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_7588;
}
else 
{
goto label_7588;
}
}
}
else 
{
goto label_7588;
}
}
}
else 
{
goto label_7588;
}
}
else 
{
label_7588:; 
 __return_7701 = status10;
goto label_7702;
}
}
else 
{
if (s == MPR3)
{
s = MPR3;
goto label_7588;
}
else 
{
goto label_7576;
}
}
}
else 
{
if (s == NP)
{
s = NP;
goto label_7588;
}
else 
{
goto label_7562;
}
}
}
}
