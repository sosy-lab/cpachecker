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
int __return_9117;
int __return_560;
int __return_640;
int __return_722;
int __return_724;
int __return_839;
int __return_927;
int __return_929;
int __return_931;
int __return_1556;
int __return_1644;
int __return_1646;
int __return_1648;
int __return_981;
int __return_977;
int __return_1204;
int __return_1292;
int __return_1349;
int __return_1345;
int __return_1402;
int __return_1442;
int __return_1477;
int __return_1059;
int __return_1062;
int __return_1150;
int __return_1396;
int __return_1392;
int __return_1403;
int __return_1433;
int __return_1476;
int __return_1872;
int __return_1960;
int __return_2017;
int __return_2013;
int __return_2070;
int __return_2561;
int __return_2604;
int __return_2541;
int __return_2534;
int __return_2530;
int __return_2537;
int __return_2559;
int __return_2471;
int __return_2467;
int __return_2460;
int __return_2456;
int __return_2469;
int __return_2466;
int __return_2413;
int __return_2409;
int __return_2549;
int __return_1727;
int __return_1730;
int __return_1818;
int __return_2064;
int __return_2060;
int __return_2071;
int __return_2560;
int __return_2605;
int __return_9114;
int __return_2543;
int __return_2517;
int __return_2513;
int __return_2539;
int __return_2557;
int __return_2299;
int __return_2295;
int __return_2288;
int __return_2284;
int __return_2297;
int __return_2294;
int __return_2241;
int __return_2237;
int __return_2551;
int __return_2715;
int __return_2803;
int __return_2805;
int __return_8943;
int __return_3084;
int __return_3172;
int __return_3174;
int __return_2914;
int __return_3002;
int __return_3004;
int __return_3006;
int __return_3559;
int __return_3604;
int __return_3585;
int __return_3555;
int __return_3548;
int __return_3544;
int __return_3557;
int __return_3554;
int __return_3501;
int __return_3497;
int __return_3291;
int __return_3379;
int __return_3381;
int __return_3383;
int __return_6584;
int __return_6232;
int __return_6230;
int __return_6228;
int __return_6516;
int __return_6418;
int __return_6487;
int __return_6585;
int __return_6414;
int __return_6407;
int __return_6403;
int __return_6416;
int __return_6413;
int __return_6360;
int __return_6356;
int __return_6226;
int __return_6224;
int __return_6207;
int __return_6203;
int __return_5898;
int __return_6122;
int __return_6089;
int __return_6085;
int __return_6078;
int __return_6074;
int __return_6087;
int __return_6084;
int __return_6031;
int __return_6027;
int __return_5894;
int __return_5887;
int __return_5883;
int __return_5896;
int __return_5893;
int __return_5840;
int __return_5836;
int __return_5700;
int __return_5696;
int __return_5689;
int __return_5685;
int __return_5698;
int __return_5695;
int __return_5642;
int __return_5638;
int __return_5522;
int __return_5233;
int __return_5468;
int __return_5411;
int __return_5443;
int __return_5407;
int __return_5400;
int __return_5396;
int __return_5409;
int __return_5406;
int __return_5353;
int __return_5349;
int __return_5229;
int __return_5222;
int __return_5218;
int __return_5231;
int __return_5228;
int __return_5175;
int __return_5171;
int __return_5495;
int __return_5012;
int __return_5008;
int __return_5001;
int __return_4997;
int __return_5010;
int __return_5007;
int __return_4954;
int __return_4950;
int __return_5051;
int __return_4828;
int __return_4799;
int __return_4752;
int __return_4748;
int __return_4741;
int __return_4737;
int __return_4750;
int __return_4747;
int __return_4694;
int __return_4690;
int __return_4565;
int __return_4561;
int __return_4554;
int __return_4550;
int __return_4563;
int __return_4560;
int __return_4507;
int __return_4503;
int __return_4165;
int __return_4163;
int __return_4161;
int __return_4252;
int __return_4340;
int __return_4342;
int __return_4344;
int __return_4375;
int __return_4159;
int __return_4157;
int __return_4140;
int __return_4136;
int __return_3965;
int __return_4053;
int __return_4055;
int __return_4057;
int __return_8494;
int __return_8492;
int __return_8490;
int __return_8747;
int __return_8872;
int __return_8926;
int __return_8928;
int __return_8690;
int __return_8897;
int __return_8899;
int __return_8686;
int __return_8679;
int __return_8675;
int __return_8688;
int __return_8685;
int __return_8632;
int __return_8628;
int __return_8488;
int __return_8486;
int __return_8469;
int __return_8465;
int __return_8819;
int __return_8869;
int __return_8091;
int __return_8089;
int __return_8087;
int __return_8384;
int __return_8386;
int __return_8277;
int __return_8771;
int __return_8871;
int __return_8319;
int __return_8321;
int __return_8795;
int __return_8870;
int __return_8273;
int __return_8266;
int __return_8262;
int __return_8275;
int __return_8272;
int __return_8219;
int __return_8215;
int __return_8085;
int __return_8083;
int __return_8066;
int __return_8062;
int __return_7968;
int __return_7970;
int __return_7939;
int __return_7941;
int __return_7900;
int __return_7896;
int __return_7889;
int __return_7885;
int __return_7898;
int __return_7895;
int __return_7842;
int __return_7838;
int __return_7709;
int __return_8843;
int __return_7705;
int __return_7698;
int __return_7694;
int __return_7707;
int __return_7704;
int __return_7651;
int __return_7647;
int __return_7521;
int __return_8867;
int __return_7517;
int __return_7510;
int __return_7506;
int __return_7519;
int __return_7516;
int __return_7463;
int __return_7459;
int __return_7337;
int __return_7333;
int __return_7326;
int __return_7322;
int __return_7335;
int __return_7332;
int __return_7279;
int __return_7275;
int __return_6972;
int __return_6970;
int __return_6968;
int __return_6966;
int __return_6964;
int __return_6947;
int __return_6943;
int __return_7055;
int __return_7143;
int __return_7145;
int __return_7147;
int __return_9115;
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
goto label_522;
}
else 
{
label_522:; 
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
 __return_9117 = -1;
goto label_560;
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
 __return_560 = -1;
label_560:; 
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
goto label_580;
}
else 
{
{
__VERIFIER_error();
}
label_580:; 
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
label_609:; 
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
goto label_632;
}
else 
{
label_632:; 
}
goto label_625;
}
}
else 
{
label_625:; 
 __return_640 = myStatus;
}
compRetStatus = __return_640;
__cil_tmp7 = (unsigned long)compRetStatus;
if (!(__cil_tmp7 == -1073741802))
{
goto label_599;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_658;
}
else 
{
{
__VERIFIER_error();
}
label_658:; 
}
goto label_599;
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
goto label_609;
}
else 
{
pended = 1;
goto label_609;
}
}
}
}
else 
{
label_599:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal = 0;
goto label_679;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (!(tmp_ndt_9 == 1))
{
returnVal = 259;
label_679:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_704;
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
label_714:; 
goto label_704;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal;
goto label_714;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
goto label_704;
}
else 
{
{
__VERIFIER_error();
}
label_704:; 
 __return_722 = returnVal;
}
tmp = __return_722;
 __return_724 = tmp;
}
status10 = __return_724;
label_726:; 
if (we_should_unload == 0)
{
goto label_506;
}
else 
{
{
int __tmp_9 = d;
int DriverObject = __tmp_9;
}
goto label_506;
}
}
}
else 
{
returnVal = -1073741823;
goto label_679;
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
goto label_778;
}
else 
{
{
__VERIFIER_error();
}
label_778:; 
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
label_808:; 
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
goto label_831;
}
else 
{
label_831:; 
}
goto label_824;
}
}
else 
{
label_824:; 
 __return_839 = myStatus;
}
compRetStatus = __return_839;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_798;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_857;
}
else 
{
{
__VERIFIER_error();
}
label_857:; 
}
goto label_798;
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
goto label_808;
}
else 
{
pended = 1;
goto label_808;
}
}
}
}
else 
{
label_798:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_885;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_885:; 
goto label_869;
}
else 
{
returnVal2 = -1073741823;
goto label_885;
}
}
}
else 
{
returnVal2 = 259;
label_869:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_910;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_919:; 
goto label_910;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_919;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_910;
}
else 
{
{
__VERIFIER_error();
}
label_910:; 
 __return_927 = returnVal2;
}
tmp = __return_927;
 __return_929 = tmp;
}
tmp___0 = __return_929;
 __return_931 = tmp___0;
}
status10 = __return_931;
goto label_2607;
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
goto label_1495;
}
else 
{
{
__VERIFIER_error();
}
label_1495:; 
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
label_1525:; 
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
goto label_1548;
}
else 
{
label_1548:; 
}
goto label_1541;
}
}
else 
{
label_1541:; 
 __return_1556 = myStatus;
}
compRetStatus = __return_1556;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1515;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1574;
}
else 
{
{
__VERIFIER_error();
}
label_1574:; 
}
goto label_1515;
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
goto label_1525;
}
else 
{
pended = 1;
goto label_1525;
}
}
}
}
else 
{
label_1515:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1602;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1602:; 
goto label_1586;
}
else 
{
returnVal2 = -1073741823;
goto label_1602;
}
}
}
else 
{
returnVal2 = 259;
label_1586:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1627;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1636:; 
goto label_1627;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1636;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1627;
}
else 
{
{
__VERIFIER_error();
}
label_1627:; 
 __return_1644 = returnVal2;
}
tmp = __return_1644;
 __return_1646 = tmp;
}
tmp = __return_1646;
 __return_1648 = tmp;
}
status10 = __return_1648;
goto label_2607;
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
goto label_941;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_951;
}
}
else 
{
label_941:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_951;
}
else 
{
label_951:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_981 = 0;
goto label_977;
}
else 
{
 __return_977 = -1073741823;
label_977:; 
}
status3 = __return_977;
setPagable = 0;
if (irpSp__Parameters__UsageNotification__InPath == 0)
{
goto label_993;
}
else 
{
if (deviceExtension__PagingPathCount != 1)
{
label_993:; 
if (status3 == status3)
{
setPagable = 1;
goto label_992;
}
else 
{
goto label_992;
}
}
else 
{
label_992:; 
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
goto label_1024;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
label_1024:; 
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
label_1173:; 
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
goto label_1196;
}
else 
{
label_1196:; 
}
goto label_1189;
}
}
else 
{
label_1189:; 
 __return_1204 = myStatus;
}
compRetStatus = __return_1204;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1163;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1222;
}
else 
{
{
__VERIFIER_error();
}
label_1222:; 
}
goto label_1163;
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
goto label_1173;
}
else 
{
pended = 1;
goto label_1173;
}
}
}
}
else 
{
label_1163:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1250;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1250:; 
goto label_1234;
}
else 
{
returnVal2 = -1073741823;
goto label_1250;
}
}
}
else 
{
returnVal2 = 259;
label_1234:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1275;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1284:; 
goto label_1275;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1284;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1275;
}
else 
{
{
__VERIFIER_error();
}
label_1275:; 
 __return_1292 = returnVal2;
}
status9 = __return_1292;
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
goto label_1309;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_1319;
}
}
else 
{
label_1309:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_1319;
}
else 
{
label_1319:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_1349 = 0;
goto label_1345;
}
else 
{
 __return_1345 = -1073741823;
label_1345:; 
}
status9 = myStatus;
goto label_1303;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_1319;
}
}
}
}
else 
{
label_1303:; 
 __return_1402 = status9;
}
status3 = __return_1402;
if (status3 >= 0)
{
goto label_1416;
}
else 
{
if (!(setPagable == 1))
{
label_1416:; 
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
 __return_1442 = l;
}
{
int __tmp_49 = Irp;
int __tmp_50 = 0;
int Irp = __tmp_49;
int PriorityBoost = __tmp_50;
if (s == NP)
{
s = DC;
goto label_1455;
}
else 
{
{
__VERIFIER_error();
}
label_1455:; 
}
 __return_1477 = status3;
}
status10 = __return_1477;
goto label_2607;
}
else 
{
setPagable = 0;
goto label_1416;
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
 __return_1059 = l;
}
 __return_1062 = -1073741802;
}
compRetStatus = __return_1062;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1047;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1080;
}
else 
{
{
__VERIFIER_error();
}
label_1080:; 
}
goto label_1047;
}
}
}
else 
{
label_1047:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1108;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1108:; 
goto label_1092;
}
else 
{
returnVal2 = -1073741823;
goto label_1108;
}
}
}
else 
{
returnVal2 = 259;
label_1092:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1133;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1142:; 
goto label_1133;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1142;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1133;
}
else 
{
{
__VERIFIER_error();
}
label_1133:; 
 __return_1150 = returnVal2;
}
status9 = __return_1150;
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
goto label_1356;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_1366;
}
}
else 
{
label_1356:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_1366;
}
else 
{
label_1366:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_1396 = 0;
goto label_1392;
}
else 
{
 __return_1392 = -1073741823;
label_1392:; 
}
status9 = myStatus;
goto label_1300;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_1366;
}
}
}
}
else 
{
label_1300:; 
 __return_1403 = status9;
}
status3 = __return_1403;
if (status3 >= 0)
{
goto label_1417;
}
else 
{
if (!(setPagable == 1))
{
label_1417:; 
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
 __return_1433 = l;
}
{
int __tmp_67 = Irp;
int __tmp_68 = 0;
int Irp = __tmp_67;
int PriorityBoost = __tmp_68;
if (s == NP)
{
s = DC;
goto label_1471;
}
else 
{
{
__VERIFIER_error();
}
label_1471:; 
}
 __return_1476 = status3;
}
status10 = __return_1476;
goto label_2606;
}
else 
{
setPagable = 0;
goto label_1417;
}
}
}
}
}
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
goto label_951;
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
goto label_1692;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
label_1692:; 
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
label_1841:; 
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
goto label_1864;
}
else 
{
label_1864:; 
}
goto label_1857;
}
}
else 
{
label_1857:; 
 __return_1872 = myStatus;
}
compRetStatus = __return_1872;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1831;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1890;
}
else 
{
{
__VERIFIER_error();
}
label_1890:; 
}
goto label_1831;
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
goto label_1841;
}
else 
{
pended = 1;
goto label_1841;
}
}
}
}
else 
{
label_1831:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1918;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1918:; 
goto label_1902;
}
else 
{
returnVal2 = -1073741823;
goto label_1918;
}
}
}
else 
{
returnVal2 = 259;
label_1902:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1943;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1952:; 
goto label_1943;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1952;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1943;
}
else 
{
{
__VERIFIER_error();
}
label_1943:; 
 __return_1960 = returnVal2;
}
status9 = __return_1960;
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
goto label_1977;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_1987;
}
}
else 
{
label_1977:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_1987;
}
else 
{
label_1987:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2017 = 0;
goto label_2013;
}
else 
{
 __return_2013 = -1073741823;
label_2013:; 
}
status9 = myStatus;
goto label_1971;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_1987;
}
}
}
}
else 
{
label_1971:; 
 __return_2070 = status9;
}
status2 = __return_2070;
if (status2 < 0)
{
 __return_2561 = status2;
}
else 
{
if (!(deviceExtension__Active == 255))
{
label_2082:; 
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_2541 = 0;
goto label_2537;
}
else 
{
if (status2 < 0)
{
goto label_2497;
}
else 
{
label_2497:; 
{
int __tmp_86 = deviceParameterHandle;
int Handle = __tmp_86;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2534 = 0;
goto label_2530;
}
else 
{
 __return_2530 = -1073741823;
label_2530:; 
}
 __return_2537 = 0;
label_2537:; 
}
status3 = __return_2537;
goto label_2562;
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
 __return_2559 = 0;
goto label_2549;
}
else 
{
status2 = -1073741823;
label_2107:; 
if (status2 < 0)
{
tmp = attempt;
int __CPAchecker_TMP_0 = attempt;
attempt = attempt + 1;
__CPAchecker_TMP_0;
if (tmp >= 4)
{
goto label_2117;
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
 __return_2471 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2363:; 
 __return_2467 = status1;
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
goto label_2420;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2430;
}
}
else 
{
label_2420:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2430;
}
else 
{
label_2430:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2460 = 0;
goto label_2456;
}
else 
{
 __return_2456 = -1073741823;
label_2456:; 
}
status1 = ioStatus__Status;
goto label_2363;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2430;
}
}
}
}
status2 = __return_2467;
goto label_2472;
}
status2 = __return_2471;
label_2472:; 
goto label_2107;
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
 __return_2469 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2366:; 
 __return_2466 = status1;
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
goto label_2373;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2383;
}
}
else 
{
label_2373:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2383;
}
else 
{
label_2383:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2413 = 0;
goto label_2409;
}
else 
{
 __return_2409 = -1073741823;
label_2409:; 
}
status1 = ioStatus__Status;
goto label_2366;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2383;
}
}
}
}
status2 = __return_2466;
goto label_2472;
}
status2 = __return_2469;
goto label_2472;
}
}
}
}
else 
{
label_2117:; 
if (status2 < 0)
{
deviceExtension__Active = 0;
 __return_2549 = 0;
label_2549:; 
}
else 
{
deviceExtension__Active = 0;
goto label_2082;
}
status3 = __return_2549;
goto label_2562;
}
}
}
}
status3 = __return_2561;
label_2562:; 
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
goto label_2599;
}
else 
{
{
__VERIFIER_error();
}
label_2599:; 
}
 __return_2604 = status3;
}
status10 = __return_2604;
label_2607:; 
goto label_726;
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
 __return_1727 = l;
}
 __return_1730 = -1073741802;
}
compRetStatus = __return_1730;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1715;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1748;
}
else 
{
{
__VERIFIER_error();
}
label_1748:; 
}
goto label_1715;
}
}
}
else 
{
label_1715:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1776;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1776:; 
goto label_1760;
}
else 
{
returnVal2 = -1073741823;
goto label_1776;
}
}
}
else 
{
returnVal2 = 259;
label_1760:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1801;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1810:; 
goto label_1801;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1810;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1801;
}
else 
{
{
__VERIFIER_error();
}
label_1801:; 
 __return_1818 = returnVal2;
}
status9 = __return_1818;
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
goto label_2024;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2034;
}
}
else 
{
label_2024:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2034;
}
else 
{
label_2034:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2064 = 0;
goto label_2060;
}
else 
{
 __return_2060 = -1073741823;
label_2060:; 
}
status9 = myStatus;
goto label_1968;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2034;
}
}
}
}
else 
{
label_1968:; 
 __return_2071 = status9;
}
status2 = __return_2071;
if (status2 < 0)
{
 __return_2560 = status2;
}
else 
{
if (!(deviceExtension__Active == 255))
{
label_2079:; 
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_2543 = 0;
goto label_2539;
}
else 
{
if (status2 < 0)
{
goto label_2499;
}
else 
{
label_2499:; 
{
int __tmp_117 = deviceParameterHandle;
int Handle = __tmp_117;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2517 = 0;
goto label_2513;
}
else 
{
 __return_2513 = -1073741823;
label_2513:; 
}
 __return_2539 = 0;
label_2539:; 
}
status3 = __return_2539;
goto label_2563;
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
 __return_2557 = 0;
goto label_2551;
}
else 
{
status2 = -1073741823;
label_2106:; 
if (status2 < 0)
{
tmp = attempt;
int __CPAchecker_TMP_0 = attempt;
attempt = attempt + 1;
__CPAchecker_TMP_0;
if (tmp >= 4)
{
goto label_2116;
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
 __return_2299 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2191:; 
 __return_2295 = status1;
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
goto label_2191;
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
status2 = __return_2295;
goto label_2300;
}
status2 = __return_2299;
label_2300:; 
goto label_2106;
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
 __return_2297 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2194:; 
 __return_2294 = status1;
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
goto label_2201;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2211;
}
}
else 
{
label_2201:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2211;
}
else 
{
label_2211:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2241 = 0;
goto label_2237;
}
else 
{
 __return_2237 = -1073741823;
label_2237:; 
}
status1 = ioStatus__Status;
goto label_2194;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2211;
}
}
}
}
status2 = __return_2294;
goto label_2300;
}
status2 = __return_2297;
goto label_2300;
}
}
}
}
else 
{
label_2116:; 
if (status2 < 0)
{
deviceExtension__Active = 0;
 __return_2551 = 0;
label_2551:; 
}
else 
{
deviceExtension__Active = 0;
goto label_2079;
}
status3 = __return_2551;
goto label_2563;
}
}
}
}
status3 = __return_2560;
label_2563:; 
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
goto label_2583;
}
else 
{
{
__VERIFIER_error();
}
label_2583:; 
}
 __return_2605 = status3;
}
status10 = __return_2605;
label_2606:; 
if (we_should_unload == 0)
{
goto label_8966;
}
else 
{
{
int __tmp_116 = d;
int DriverObject = __tmp_116;
}
label_8966:; 
if (!(pended == 1))
{
label_8977:; 
if (!(pended == 1))
{
label_8991:; 
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
goto label_9003;
}
else 
{
goto label_9017;
}
}
else 
{
goto label_9017;
}
}
else 
{
label_9017:; 
if (pended != 1)
{
if (s == DC)
{
if (!(status10 == 259))
{
label_9063:; 
goto label_9003;
}
else 
{
{
__VERIFIER_error();
}
goto label_9063;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9003;
}
else 
{
goto label_9003;
}
}
}
else 
{
goto label_9003;
}
}
}
else 
{
goto label_9003;
}
}
else 
{
label_9003:; 
 __return_9114 = status10;
return 1;
}
}
else 
{
if (s == MPR3)
{
s = MPR3;
goto label_9003;
}
else 
{
goto label_8991;
}
}
}
else 
{
if (s == NP)
{
s = NP;
goto label_9003;
}
else 
{
goto label_8977;
}
}
}
}
}
}
}
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
goto label_2654;
}
else 
{
{
__VERIFIER_error();
}
label_2654:; 
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
label_2684:; 
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
goto label_2707;
}
else 
{
label_2707:; 
}
goto label_2700;
}
}
else 
{
label_2700:; 
 __return_2715 = myStatus;
}
compRetStatus = __return_2715;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2674;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_2733;
}
else 
{
{
__VERIFIER_error();
}
label_2733:; 
}
goto label_2674;
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
goto label_2684;
}
else 
{
pended = 1;
goto label_2684;
}
}
}
}
else 
{
label_2674:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_2761;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_2761:; 
goto label_2745;
}
else 
{
returnVal2 = -1073741823;
goto label_2761;
}
}
}
else 
{
returnVal2 = 259;
label_2745:; 
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
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_2795:; 
goto label_2786;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2795;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2786;
}
else 
{
{
__VERIFIER_error();
}
label_2786:; 
 __return_2803 = returnVal2;
}
tmp = __return_2803;
 __return_2805 = tmp;
}
status4 = __return_2805;
label_2807:; 
 __return_8943 = status4;
}
status10 = __return_8943;
goto label_726;
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
goto label_3015;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
goto label_3015;
}
else 
{
compRegistered = 1;
routine = 0;
label_3015:; 
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
label_3053:; 
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
goto label_3076;
}
else 
{
label_3076:; 
}
goto label_3069;
}
}
else 
{
label_3069:; 
 __return_3084 = myStatus;
}
compRetStatus = __return_3084;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_3043;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_3102;
}
else 
{
{
__VERIFIER_error();
}
label_3102:; 
}
goto label_3043;
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
goto label_3053;
}
else 
{
pended = 1;
goto label_3053;
}
}
}
}
else 
{
label_3043:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_3130;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_3130:; 
goto label_3114;
}
else 
{
returnVal2 = -1073741823;
goto label_3130;
}
}
}
else 
{
returnVal2 = 259;
label_3114:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3155;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_3164:; 
goto label_3155;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3164;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3155;
}
else 
{
{
__VERIFIER_error();
}
label_3155:; 
 __return_3172 = returnVal2;
}
tmp = __return_3172;
 __return_3174 = tmp;
}
status4 = __return_3174;
label_3175:; 
label_3177:; 
goto label_2807;
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
goto label_2853;
}
else 
{
{
__VERIFIER_error();
}
label_2853:; 
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
label_2883:; 
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
goto label_2906;
}
else 
{
label_2906:; 
}
goto label_2899;
}
}
else 
{
label_2899:; 
 __return_2914 = myStatus;
}
compRetStatus = __return_2914;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2873;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_2932;
}
else 
{
{
__VERIFIER_error();
}
label_2932:; 
}
goto label_2873;
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
goto label_2883;
}
else 
{
pended = 1;
goto label_2883;
}
}
}
}
else 
{
label_2873:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_2960;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_2960:; 
goto label_2944;
}
else 
{
returnVal2 = -1073741823;
goto label_2960;
}
}
}
else 
{
returnVal2 = 259;
label_2944:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2985;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_2994:; 
goto label_2985;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2994;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2985;
}
else 
{
{
__VERIFIER_error();
}
label_2985:; 
 __return_3002 = returnVal2;
}
tmp = __return_3002;
 __return_3004 = tmp;
}
tmp___0 = __return_3004;
 __return_3006 = tmp___0;
}
status4 = __return_3006;
goto label_3175;
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
 __return_3559 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_3451:; 
 __return_3555 = status1;
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
goto label_3508;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_3518;
}
}
else 
{
label_3508:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3518;
}
else 
{
label_3518:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_3548 = 0;
goto label_3544;
}
else 
{
 __return_3544 = -1073741823;
label_3544:; 
}
status1 = ioStatus__Status;
goto label_3451;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_3518;
}
}
}
}
status8 = __return_3555;
goto label_3560;
}
status8 = __return_3559;
label_3560:; 
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
goto label_3599;
}
else 
{
{
__VERIFIER_error();
}
label_3599:; 
}
 __return_3604 = status8;
goto label_3585;
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
goto label_3580;
}
else 
{
{
__VERIFIER_error();
}
label_3580:; 
}
 __return_3585 = status8;
label_3585:; 
}
status4 = __return_3585;
label_3605:; 
goto label_3177;
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
 __return_3557 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_3454:; 
 __return_3554 = status1;
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
goto label_3461;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_3471;
}
}
else 
{
label_3461:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3471;
}
else 
{
label_3471:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_3501 = 0;
goto label_3497;
}
else 
{
 __return_3497 = -1073741823;
label_3497:; 
}
status1 = ioStatus__Status;
goto label_3454;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_3471;
}
}
}
}
status8 = __return_3554;
goto label_3560;
}
status8 = __return_3557;
goto label_3560;
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
goto label_3230;
}
else 
{
{
__VERIFIER_error();
}
label_3230:; 
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
label_3260:; 
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
goto label_3283;
}
else 
{
label_3283:; 
}
goto label_3276;
}
}
else 
{
label_3276:; 
 __return_3291 = myStatus;
}
compRetStatus = __return_3291;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_3250;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_3309;
}
else 
{
{
__VERIFIER_error();
}
label_3309:; 
}
goto label_3250;
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
goto label_3260;
}
else 
{
pended = 1;
goto label_3260;
}
}
}
}
else 
{
label_3250:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_3337;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_3337:; 
goto label_3321;
}
else 
{
returnVal2 = -1073741823;
goto label_3337;
}
}
}
else 
{
returnVal2 = 259;
label_3321:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3362;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_3371:; 
goto label_3362;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3371;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3362;
}
else 
{
{
__VERIFIER_error();
}
label_3362:; 
 __return_3379 = returnVal2;
}
tmp = __return_3379;
 __return_3381 = tmp;
}
tmp = __return_3381;
 __return_3383 = tmp;
}
status4 = __return_3383;
goto label_3605;
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
label_6522:; 
__cil_tmp116 = (unsigned long)status7;
label_6536:; 
if (!(__cil_tmp116 == -2147483626))
{
label_6543:; 
myStatus = status7;
{
int __tmp_188 = Irp;
int __tmp_189 = 0;
int Irp = __tmp_188;
int PriorityBoost = __tmp_189;
if (s == NP)
{
s = DC;
goto label_6579;
}
else 
{
{
__VERIFIER_error();
}
label_6579:; 
}
 __return_6584 = status7;
}
status4 = __return_6584;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
label_6546:; 
goto label_6543;
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
 __return_6232 = 0;
goto label_6228;
}
else 
{
if (currentBuffer == 0)
{
 __return_6230 = 0;
goto label_6228;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_6228 = 0;
label_6228:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_6160:; 
if (status5 < 0)
{
 __return_6226 = 0;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_6220:; 
 __return_6224 = returnValue;
}
else 
{
returnValue = 1;
goto label_6220;
}
tmp = __return_6224;
goto label_6233;
}
tmp = __return_6226;
goto label_6233;
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
goto label_6167;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6177;
}
}
else 
{
label_6167:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6177;
}
else 
{
label_6177:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6207 = 0;
goto label_6203;
}
else 
{
 __return_6203 = -1073741823;
label_6203:; 
}
status5 = ioStatus__Status;
goto label_6160;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6177;
}
}
}
}
}
tmp = __return_6228;
label_6233:; 
if (!(tmp == 0))
{
status7 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_6522;
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
label_6494:; 
myStatus = status7;
{
int __tmp_191 = Irp;
int __tmp_192 = 0;
int Irp = __tmp_191;
int PriorityBoost = __tmp_192;
if (s == NP)
{
s = DC;
goto label_6511;
}
else 
{
{
__VERIFIER_error();
}
label_6511:; 
}
 __return_6516 = status7;
}
status4 = __return_6516;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_6494;
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
 __return_6418 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6310:; 
 __return_6414 = status1;
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
goto label_6367;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6377;
}
}
else 
{
label_6367:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6377;
}
else 
{
label_6377:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6407 = 0;
goto label_6403;
}
else 
{
 __return_6403 = -1073741823;
label_6403:; 
}
status1 = ioStatus__Status;
goto label_6310;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6377;
}
}
}
}
status7 = __return_6414;
goto label_6419;
}
status7 = __return_6418;
label_6419:; 
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
label_6465:; 
myStatus = status7;
{
int __tmp_197 = Irp;
int __tmp_198 = 0;
int Irp = __tmp_197;
int PriorityBoost = __tmp_198;
if (s == NP)
{
s = DC;
goto label_6482;
}
else 
{
{
__VERIFIER_error();
}
label_6482:; 
}
 __return_6487 = status7;
}
status4 = __return_6487;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_6465;
}
}
else 
{
goto label_6434;
}
}
else 
{
status7 = 0;
label_6434:; 
goto label_6427;
}
}
else 
{
status7 = 0;
label_6427:; 
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength > sizeof__CDROM_TOC)
{
bytesTransfered = sizeof__CDROM_TOC;
goto label_6444;
}
else 
{
bytesTransfered = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
label_6444:; 
__cil_tmp98 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp98 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength - TrackData__0;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_6453;
}
else 
{
tracksToReturn = tracksOnCd;
label_6453:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_6457;
}
else 
{
label_6457:; 
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
goto label_6563;
}
else 
{
{
__VERIFIER_error();
}
label_6563:; 
}
 __return_6585 = status7;
}
status4 = __return_6585;
label_6586:; 
goto label_3177;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_6546;
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
 __return_6416 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6313:; 
 __return_6413 = status1;
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
goto label_6320;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6330;
}
}
else 
{
label_6320:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6330;
}
else 
{
label_6330:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6360 = 0;
goto label_6356;
}
else 
{
 __return_6356 = -1073741823;
label_6356:; 
}
status1 = ioStatus__Status;
goto label_6313;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6330;
}
}
}
}
status7 = __return_6413;
goto label_6419;
}
status7 = __return_6416;
goto label_6419;
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
label_5724:; 
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
 __return_5898 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5790:; 
 __return_5894 = status1;
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
goto label_5847;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5857;
}
}
else 
{
label_5847:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5857;
}
else 
{
label_5857:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5887 = 0;
goto label_5883;
}
else 
{
 __return_5883 = -1073741823;
label_5883:; 
}
status1 = ioStatus__Status;
goto label_5790;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5857;
}
}
}
}
status7 = __return_5894;
goto label_5899;
}
status7 = __return_5898;
label_5899:; 
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
label_6100:; 
myStatus = status7;
{
int __tmp_220 = Irp;
int __tmp_221 = 0;
int Irp = __tmp_220;
int PriorityBoost = __tmp_221;
if (s == NP)
{
s = DC;
goto label_6117;
}
else 
{
{
__VERIFIER_error();
}
label_6117:; 
}
 __return_6122 = status7;
}
status4 = __return_6122;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_6100;
}
}
else 
{
if (currentIrpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CDROM_PLAY_AUDIO_MSF)
{
status7 = -1073741820;
goto label_6522;
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
 __return_6089 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5981:; 
 __return_6085 = status1;
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
goto label_6038;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6048;
}
}
else 
{
label_6038:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6048;
}
else 
{
label_6048:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6078 = 0;
goto label_6074;
}
else 
{
 __return_6074 = -1073741823;
label_6074:; 
}
status1 = ioStatus__Status;
goto label_5981;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6048;
}
}
}
}
status7 = __return_6085;
goto label_6090;
}
status7 = __return_6089;
label_6090:; 
__cil_tmp116 = (unsigned long)status7;
goto label_6536;
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
 __return_6087 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5984:; 
 __return_6084 = status1;
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
goto label_5991;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6001;
}
}
else 
{
label_5991:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6001;
}
else 
{
label_6001:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6031 = 0;
goto label_6027;
}
else 
{
 __return_6027 = -1073741823;
label_6027:; 
}
status1 = ioStatus__Status;
goto label_5984;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6001;
}
}
}
}
status7 = __return_6084;
goto label_6090;
}
status7 = __return_6087;
goto label_6090;
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
 __return_5896 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5793:; 
 __return_5893 = status1;
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
goto label_5800;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5810;
}
}
else 
{
label_5800:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5810;
}
else 
{
label_5810:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5840 = 0;
goto label_5836;
}
else 
{
 __return_5836 = -1073741823;
label_5836:; 
}
status1 = ioStatus__Status;
goto label_5793;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5810;
}
}
}
}
status7 = __return_5893;
goto label_5899;
}
status7 = __return_5896;
goto label_5899;
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
goto label_5724;
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
goto label_6522;
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
 __return_5700 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5592:; 
 __return_5696 = status1;
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
goto label_5649;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5659;
}
}
else 
{
label_5649:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5659;
}
else 
{
label_5659:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5689 = 0;
goto label_5685;
}
else 
{
 __return_5685 = -1073741823;
label_5685:; 
}
status1 = ioStatus__Status;
goto label_5592;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5659;
}
}
}
}
status7 = __return_5696;
goto label_5701;
}
status7 = __return_5700;
label_5701:; 
if (status7 < 0)
{
__cil_tmp105 = (unsigned long)status7;
if (!(__cil_tmp105 == -1073741808))
{
goto label_5706;
}
else 
{
status7 = -1073741803;
goto label_5706;
}
}
else 
{
label_5706:; 
goto label_6522;
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
 __return_5698 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5595:; 
 __return_5695 = status1;
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
goto label_5602;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5612;
}
}
else 
{
label_5602:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5612;
}
else 
{
label_5612:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5642 = 0;
goto label_5638;
}
else 
{
 __return_5638 = -1073741823;
label_5638:; 
}
status1 = ioStatus__Status;
goto label_5595;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5612;
}
}
}
}
status7 = __return_5695;
goto label_5701;
}
status7 = __return_5698;
goto label_5701;
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
label_5500:; 
myStatus = status7;
{
int __tmp_260 = Irp;
int __tmp_261 = 0;
int Irp = __tmp_260;
int PriorityBoost = __tmp_261;
if (s == NP)
{
s = DC;
goto label_5517;
}
else 
{
{
__VERIFIER_error();
}
label_5517:; 
}
 __return_5522 = status7;
}
status4 = __return_5522;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5500;
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
 __return_5233 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5125:; 
 __return_5229 = status1;
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
goto label_5182;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5192;
}
}
else 
{
label_5182:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5192;
}
else 
{
label_5192:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5222 = 0;
goto label_5218;
}
else 
{
 __return_5218 = -1073741823;
label_5218:; 
}
status1 = ioStatus__Status;
goto label_5125;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5192;
}
}
}
}
status7 = __return_5229;
goto label_5234;
}
status7 = __return_5233;
label_5234:; 
if (status7 < 0)
{
__cil_tmp109 = (unsigned long)status7;
if (!(__cil_tmp109 == -2147483626))
{
label_5446:; 
myStatus = status7;
{
int __tmp_266 = Irp;
int __tmp_267 = 0;
int Irp = __tmp_266;
int PriorityBoost = __tmp_267;
if (s == NP)
{
s = DC;
goto label_5463;
}
else 
{
{
__VERIFIER_error();
}
label_5463:; 
}
 __return_5468 = status7;
}
status4 = __return_5468;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5446;
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
 __return_5411 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5303:; 
 __return_5407 = status1;
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
goto label_5360;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5370;
}
}
else 
{
label_5360:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5370;
}
else 
{
label_5370:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5400 = 0;
goto label_5396;
}
else 
{
 __return_5396 = -1073741823;
label_5396:; 
}
status1 = ioStatus__Status;
goto label_5303;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5370;
}
}
}
}
status7 = __return_5407;
goto label_5412;
}
status7 = __return_5411;
label_5412:; 
if (status7 < 0)
{
__cil_tmp111 = (unsigned long)status7;
if (!(__cil_tmp111 == -2147483626))
{
label_5421:; 
myStatus = status7;
{
int __tmp_272 = Irp;
int __tmp_273 = 0;
int Irp = __tmp_272;
int PriorityBoost = __tmp_273;
if (s == NP)
{
s = DC;
goto label_5438;
}
else 
{
{
__VERIFIER_error();
}
label_5438:; 
}
 __return_5443 = status7;
}
status4 = __return_5443;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5421;
}
}
else 
{
goto label_6522;
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
 __return_5409 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5306:; 
 __return_5406 = status1;
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
goto label_5313;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5323;
}
}
else 
{
label_5313:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5323;
}
else 
{
label_5323:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5353 = 0;
goto label_5349;
}
else 
{
 __return_5349 = -1073741823;
label_5349:; 
}
status1 = ioStatus__Status;
goto label_5306;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5323;
}
}
}
}
status7 = __return_5406;
goto label_5412;
}
status7 = __return_5409;
goto label_5412;
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
 __return_5231 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5128:; 
 __return_5228 = status1;
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
goto label_5135;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5145;
}
}
else 
{
label_5135:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5145;
}
else 
{
label_5145:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5175 = 0;
goto label_5171;
}
else 
{
 __return_5171 = -1073741823;
label_5171:; 
}
status1 = ioStatus__Status;
goto label_5128;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5145;
}
}
}
}
status7 = __return_5228;
goto label_5234;
}
status7 = __return_5231;
goto label_5234;
}
}
}
else 
{
status7 = 0;
__cil_tmp107 = (unsigned long)status7;
if (!(__cil_tmp107 == -2147483626))
{
label_5473:; 
myStatus = status7;
{
int __tmp_294 = Irp;
int __tmp_295 = 0;
int Irp = __tmp_294;
int PriorityBoost = __tmp_295;
if (s == NP)
{
s = DC;
goto label_5490;
}
else 
{
{
__VERIFIER_error();
}
label_5490:; 
}
 __return_5495 = status7;
}
status4 = __return_5495;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5473;
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
 __return_5012 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4904:; 
 __return_5008 = status1;
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
goto label_4961;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4971;
}
}
else 
{
label_4961:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4971;
}
else 
{
label_4971:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5001 = 0;
goto label_4997;
}
else 
{
 __return_4997 = -1073741823;
label_4997:; 
}
status1 = ioStatus__Status;
goto label_4904;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4971;
}
}
}
}
status7 = __return_5008;
goto label_5013;
}
status7 = __return_5012;
label_5013:; 
if (status7 >= 0)
{
deviceExtension__PlayActive = 1;
deviceExtension__Paused = 0;
goto label_5018;
}
else 
{
label_5018:; 
goto label_6522;
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
 __return_5010 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4907:; 
 __return_5007 = status1;
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
goto label_4914;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4924;
}
}
else 
{
label_4914:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4924;
}
else 
{
label_4924:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4954 = 0;
goto label_4950;
}
else 
{
 __return_4950 = -1073741823;
label_4950:; 
}
status1 = ioStatus__Status;
goto label_4907;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4924;
}
}
}
}
status7 = __return_5007;
goto label_5013;
}
status7 = __return_5010;
goto label_5013;
}
}
}
else 
{
status7 = -1073741823;
__cil_tmp112 = (unsigned long)status7;
if (!(__cil_tmp112 == -2147483626))
{
label_5029:; 
myStatus = status7;
{
int __tmp_310 = Irp;
int __tmp_311 = 0;
int Irp = __tmp_310;
int PriorityBoost = __tmp_311;
if (s == NP)
{
s = DC;
goto label_5046;
}
else 
{
{
__VERIFIER_error();
}
label_5046:; 
}
 __return_5051 = status7;
}
status4 = __return_5051;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5029;
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
goto label_6522;
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
label_4806:; 
myStatus = status7;
{
int __tmp_312 = Irp;
int __tmp_313 = 0;
int Irp = __tmp_312;
int PriorityBoost = __tmp_313;
if (s == NP)
{
s = DC;
goto label_4823;
}
else 
{
{
__VERIFIER_error();
}
label_4823:; 
}
 __return_4828 = status7;
}
status4 = __return_4828;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4806;
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
label_4777:; 
myStatus = status7;
{
int __tmp_314 = Irp;
int __tmp_315 = 0;
int Irp = __tmp_314;
int PriorityBoost = __tmp_315;
if (s == NP)
{
s = DC;
goto label_4794;
}
else 
{
{
__VERIFIER_error();
}
label_4794:; 
}
 __return_4799 = status7;
}
status4 = __return_4799;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4777;
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
 __return_4752 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4644:; 
 __return_4748 = status1;
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
goto label_4701;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4711;
}
}
else 
{
label_4701:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4711;
}
else 
{
label_4711:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4741 = 0;
goto label_4737;
}
else 
{
 __return_4737 = -1073741823;
label_4737:; 
}
status1 = ioStatus__Status;
goto label_4644;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4711;
}
}
}
}
status7 = __return_4748;
goto label_4753;
}
status7 = __return_4752;
label_4753:; 
if (status7 >= 0)
{
if (!(deviceExtension__Paused == 1))
{
label_4763:; 
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_4761;
}
else 
{
deviceExtension__PlayActive = 0;
goto label_4763;
}
}
else 
{
Irp__IoStatus__Information = 0;
label_4761:; 
goto label_6522;
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
 __return_4750 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4647:; 
 __return_4747 = status1;
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
goto label_4654;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4664;
}
}
else 
{
label_4654:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4664;
}
else 
{
label_4664:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4694 = 0;
goto label_4690;
}
else 
{
 __return_4690 = -1073741823;
label_4690:; 
}
status1 = ioStatus__Status;
goto label_4647;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4664;
}
}
}
}
status7 = __return_4747;
goto label_4753;
}
status7 = __return_4750;
goto label_4753;
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
 __return_4565 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4457:; 
 __return_4561 = status1;
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
goto label_4514;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4524;
}
}
else 
{
label_4514:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4524;
}
else 
{
label_4524:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4554 = 0;
goto label_4550;
}
else 
{
 __return_4550 = -1073741823;
label_4550:; 
}
status1 = ioStatus__Status;
goto label_4457;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4524;
}
}
}
}
status7 = __return_4561;
goto label_4566;
}
status7 = __return_4565;
label_4566:; 
goto label_6522;
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
 __return_4563 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4460:; 
 __return_4560 = status1;
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
goto label_4467;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4477;
}
}
else 
{
label_4467:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4477;
}
else 
{
label_4477:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4507 = 0;
goto label_4503;
}
else 
{
 __return_4503 = -1073741823;
label_4503:; 
}
status1 = ioStatus__Status;
goto label_4460;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4477;
}
}
}
}
status7 = __return_4560;
goto label_4566;
}
status7 = __return_4563;
goto label_4566;
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
label_4381:; 
Irp__IoStatus__Information = 0;
label_4384:; 
status7 = -1073741808;
goto label_6522;
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
goto label_4381;
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
goto label_4384;
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
 __return_4165 = 0;
goto label_4161;
}
else 
{
if (currentBuffer == 0)
{
 __return_4163 = 0;
goto label_4161;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_4161 = 0;
label_4161:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_4093:; 
if (status5 < 0)
{
 __return_4159 = 0;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_4153:; 
 __return_4157 = returnValue;
}
else 
{
returnValue = 1;
goto label_4153;
}
tmp___1 = __return_4157;
goto label_4166;
}
tmp___1 = __return_4159;
goto label_4166;
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
goto label_4100;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4110;
}
}
else 
{
label_4100:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4110;
}
else 
{
label_4110:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4140 = 0;
goto label_4136;
}
else 
{
 __return_4136 = -1073741823;
label_4136:; 
}
status5 = ioStatus__Status;
goto label_4093;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4110;
}
}
}
}
}
tmp___1 = __return_4161;
label_4166:; 
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
goto label_4191;
}
else 
{
{
__VERIFIER_error();
}
label_4191:; 
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
label_4221:; 
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
goto label_4244;
}
else 
{
label_4244:; 
}
goto label_4237;
}
}
else 
{
label_4237:; 
 __return_4252 = myStatus;
}
compRetStatus = __return_4252;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_4211;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_4270;
}
else 
{
{
__VERIFIER_error();
}
label_4270:; 
}
goto label_4211;
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
goto label_4221;
}
else 
{
pended = 1;
goto label_4221;
}
}
}
}
else 
{
label_4211:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_4298;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_4298:; 
goto label_4282;
}
else 
{
returnVal2 = -1073741823;
goto label_4298;
}
}
}
else 
{
returnVal2 = 259;
label_4282:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4323;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_4332:; 
goto label_4323;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4332;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_4323;
}
else 
{
{
__VERIFIER_error();
}
label_4323:; 
 __return_4340 = returnVal2;
}
tmp = __return_4340;
 __return_4342 = tmp;
}
tmp___0 = __return_4342;
 __return_4344 = tmp___0;
}
status4 = __return_4344;
goto label_6586;
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
label_4353:; 
myStatus = status7;
{
int __tmp_353 = Irp;
int __tmp_354 = 0;
int Irp = __tmp_353;
int PriorityBoost = __tmp_354;
if (s == NP)
{
s = DC;
goto label_4370;
}
else 
{
{
__VERIFIER_error();
}
label_4370:; 
}
 __return_4375 = status7;
}
status4 = __return_4375;
goto label_6586;
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4353;
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
goto label_3904;
}
else 
{
{
__VERIFIER_error();
}
label_3904:; 
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
label_3934:; 
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
goto label_3957;
}
else 
{
label_3957:; 
}
goto label_3950;
}
}
else 
{
label_3950:; 
 __return_3965 = myStatus;
}
compRetStatus = __return_3965;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_3924;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_3983;
}
else 
{
{
__VERIFIER_error();
}
label_3983:; 
}
goto label_3924;
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
goto label_3934;
}
else 
{
pended = 1;
goto label_3934;
}
}
}
}
else 
{
label_3924:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_4011;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_4011:; 
goto label_3995;
}
else 
{
returnVal2 = -1073741823;
goto label_4011;
}
}
}
else 
{
returnVal2 = 259;
label_3995:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4036;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_4045:; 
goto label_4036;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4045;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_4036;
}
else 
{
{
__VERIFIER_error();
}
label_4036:; 
 __return_4053 = returnVal2;
}
tmp = __return_4053;
 __return_4055 = tmp;
}
tmp___2 = __return_4055;
 __return_4057 = tmp___2;
}
status4 = __return_4057;
goto label_6586;
}
}
}
}
}
}
}
}
}
}
}
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
 __return_8494 = 0;
goto label_8490;
}
else 
{
if (currentBuffer == 0)
{
 __return_8492 = 0;
goto label_8490;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_8490 = 0;
label_8490:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_8422:; 
if (status5 < 0)
{
 __return_8488 = 0;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_8482:; 
 __return_8486 = returnValue;
}
else 
{
returnValue = 1;
goto label_8482;
}
tmp = __return_8486;
goto label_8495;
}
tmp = __return_8488;
goto label_8495;
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
goto label_8429;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_8439;
}
}
else 
{
label_8429:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_8439;
}
else 
{
label_8439:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_8469 = 0;
goto label_8465;
}
else 
{
 __return_8465 = -1073741823;
label_8465:; 
}
status5 = ioStatus__Status;
goto label_8422;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_8439;
}
}
}
}
}
tmp = __return_8490;
label_8495:; 
if (!(tmp == 0))
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
label_8505:; 
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
goto label_8742;
}
else 
{
{
__VERIFIER_error();
}
label_8742:; 
}
 __return_8747 = status;
}
tmp___8 = __return_8747;
 __return_8872 = tmp___8;
}
status4 = __return_8872;
goto label_8929;
}
else 
{
if (!(currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength == 0))
{
status6 = -1073741789;
Irp__IoStatus__Information = 0;
goto label_8505;
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
goto label_8921;
}
else 
{
{
__VERIFIER_error();
}
label_8921:; 
}
 __return_8926 = status;
}
tmp___0 = __return_8926;
 __return_8928 = tmp___0;
}
status4 = __return_8928;
label_8929:; 
goto label_3177;
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
 __return_8690 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_8582:; 
 __return_8686 = status1;
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
goto label_8639;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_8649;
}
}
else 
{
label_8639:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_8649;
}
else 
{
label_8649:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_8679 = 0;
goto label_8675;
}
else 
{
 __return_8675 = -1073741823;
label_8675:; 
}
status1 = ioStatus__Status;
goto label_8582;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_8649;
}
}
}
}
status6 = __return_8686;
goto label_8691;
}
status6 = __return_8690;
label_8691:; 
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
goto label_8892;
}
else 
{
{
__VERIFIER_error();
}
label_8892:; 
}
 __return_8897 = status;
}
tmp___1 = __return_8897;
 __return_8899 = tmp___1;
}
status4 = __return_8899;
goto label_8929;
}
else 
{
status6 = 0;
Irp__IoStatus__Information = bytesTransfered;
if (lastSession__LogicalBlockAddress == 0)
{
goto label_8505;
}
else 
{
cdaudioDataOut__FirstTrack = 1;
cdaudioDataOut__LastTrack = 2;
goto label_8505;
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
 __return_8688 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_8585:; 
 __return_8685 = status1;
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
goto label_8592;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_8602;
}
}
else 
{
label_8592:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_8602;
}
else 
{
label_8602:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_8632 = 0;
goto label_8628;
}
else 
{
 __return_8628 = -1073741823;
label_8628:; 
}
status1 = ioStatus__Status;
goto label_8585;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_8602;
}
}
}
}
status6 = __return_8685;
goto label_8691;
}
status6 = __return_8688;
goto label_8691;
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
label_7985:; 
label_8714:; 
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
goto label_8814;
}
else 
{
{
__VERIFIER_error();
}
label_8814:; 
}
 __return_8819 = status;
}
tmp___8 = __return_8819;
label_8820:; 
 __return_8869 = tmp___8;
}
status4 = __return_8869;
goto label_8929;
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
 __return_8091 = 0;
goto label_8087;
}
else 
{
if (currentBuffer == 0)
{
 __return_8089 = 0;
goto label_8087;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_8087 = 0;
label_8087:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_8019:; 
if (status5 < 0)
{
 __return_8085 = 0;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_8079:; 
 __return_8083 = returnValue;
}
else 
{
returnValue = 1;
goto label_8079;
}
tmp___2 = __return_8083;
goto label_8092;
}
tmp___2 = __return_8085;
goto label_8092;
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
goto label_8026;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_8036;
}
}
else 
{
label_8026:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_8036;
}
else 
{
label_8036:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_8066 = 0;
goto label_8062;
}
else 
{
 __return_8062 = -1073741823;
label_8062:; 
}
status5 = ioStatus__Status;
goto label_8019;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_8036;
}
}
}
}
}
tmp___2 = __return_8087;
label_8092:; 
if (!(tmp___2 == 0))
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_7985;
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
goto label_8379;
}
else 
{
{
__VERIFIER_error();
}
label_8379:; 
}
 __return_8384 = status;
}
tmp___3 = __return_8384;
 __return_8386 = tmp___3;
}
status4 = __return_8386;
goto label_8929;
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
 __return_8277 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_8169:; 
 __return_8273 = status1;
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
goto label_8226;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_8236;
}
}
else 
{
label_8226:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_8236;
}
else 
{
label_8236:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_8266 = 0;
goto label_8262;
}
else 
{
 __return_8262 = -1073741823;
label_8262:; 
}
status1 = ioStatus__Status;
goto label_8169;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_8236;
}
}
}
}
status6 = __return_8273;
goto label_8278;
}
status6 = __return_8277;
label_8278:; 
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
goto label_8339;
}
else 
{
tracksToReturn = tracksOnCd;
label_8339:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_8345;
}
else 
{
label_8345:; 
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
goto label_8766;
}
else 
{
{
__VERIFIER_error();
}
label_8766:; 
}
 __return_8771 = status;
}
tmp___8 = __return_8771;
 __return_8871 = tmp___8;
}
status4 = __return_8871;
goto label_8929;
}
}
}
else 
{
goto label_8284;
}
}
else 
{
label_8284:; 
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
goto label_8314;
}
else 
{
{
__VERIFIER_error();
}
label_8314:; 
}
 __return_8319 = status;
}
tmp___4 = __return_8319;
 __return_8321 = tmp___4;
}
status4 = __return_8321;
goto label_8929;
}
else 
{
__cil_tmp109 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp109 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_8338;
}
else 
{
tracksToReturn = tracksOnCd;
label_8338:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_8347;
}
else 
{
label_8347:; 
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
goto label_8790;
}
else 
{
{
__VERIFIER_error();
}
label_8790:; 
}
 __return_8795 = status;
}
tmp___8 = __return_8795;
 __return_8870 = tmp___8;
}
status4 = __return_8870;
goto label_8929;
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
 __return_8275 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_8172:; 
 __return_8272 = status1;
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
goto label_8179;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_8189;
}
}
else 
{
label_8179:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_8189;
}
else 
{
label_8189:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_8219 = 0;
goto label_8215;
}
else 
{
 __return_8215 = -1073741823;
label_8215:; 
}
status1 = ioStatus__Status;
goto label_8172;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_8189;
}
}
}
}
status6 = __return_8272;
goto label_8278;
}
status6 = __return_8275;
goto label_8278;
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
goto label_8714;
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
goto label_7963;
}
else 
{
{
__VERIFIER_error();
}
label_7963:; 
}
 __return_7968 = status;
}
tmp___5 = __return_7968;
 __return_7970 = tmp___5;
}
status4 = __return_7970;
goto label_8929;
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
goto label_7934;
}
else 
{
{
__VERIFIER_error();
}
label_7934:; 
}
 __return_7939 = status;
}
tmp___6 = __return_7939;
 __return_7941 = tmp___6;
}
status4 = __return_7941;
goto label_8929;
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
 __return_7900 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7792:; 
 __return_7896 = status1;
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
goto label_7849;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7859;
}
}
else 
{
label_7849:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7859;
}
else 
{
label_7859:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7889 = 0;
goto label_7885;
}
else 
{
 __return_7885 = -1073741823;
label_7885:; 
}
status1 = ioStatus__Status;
goto label_7792;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7859;
}
}
}
}
status6 = __return_7896;
goto label_7901;
}
status6 = __return_7900;
label_7901:; 
if (status6 >= 0)
{
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_7909;
}
else 
{
Irp__IoStatus__Information = 0;
label_7909:; 
goto label_8714;
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
 __return_7898 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7795:; 
 __return_7895 = status1;
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
goto label_7802;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7812;
}
}
else 
{
label_7802:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7812;
}
else 
{
label_7812:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7842 = 0;
goto label_7838;
}
else 
{
 __return_7838 = -1073741823;
label_7838:; 
}
status1 = ioStatus__Status;
goto label_7795;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7812;
}
}
}
}
status6 = __return_7895;
goto label_7901;
}
status6 = __return_7898;
goto label_7901;
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
goto label_8714;
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
 __return_7709 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7601:; 
 __return_7705 = status1;
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
goto label_7658;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7668;
}
}
else 
{
label_7658:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7668;
}
else 
{
label_7668:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7698 = 0;
goto label_7694;
}
else 
{
 __return_7694 = -1073741823;
label_7694:; 
}
status1 = ioStatus__Status;
goto label_7601;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7668;
}
}
}
}
status6 = __return_7705;
goto label_7710;
}
status6 = __return_7709;
label_7710:; 
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
goto label_8838;
}
else 
{
{
__VERIFIER_error();
}
label_8838:; 
}
 __return_8843 = status;
}
tmp___8 = __return_8843;
goto label_8820;
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
 __return_7707 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7604:; 
 __return_7704 = status1;
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
goto label_7611;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7621;
}
}
else 
{
label_7611:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7621;
}
else 
{
label_7621:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7651 = 0;
goto label_7647;
}
else 
{
 __return_7647 = -1073741823;
label_7647:; 
}
status1 = ioStatus__Status;
goto label_7604;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7621;
}
}
}
}
status6 = __return_7704;
goto label_7710;
}
status6 = __return_7707;
goto label_7710;
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
goto label_8714;
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
 __return_7521 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7413:; 
 __return_7517 = status1;
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
goto label_7470;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7480;
}
}
else 
{
label_7470:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7480;
}
else 
{
label_7480:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7510 = 0;
goto label_7506;
}
else 
{
 __return_7506 = -1073741823;
label_7506:; 
}
status1 = ioStatus__Status;
goto label_7413;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7480;
}
}
}
}
status6 = __return_7517;
goto label_7522;
}
status6 = __return_7521;
label_7522:; 
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
goto label_8862;
}
else 
{
{
__VERIFIER_error();
}
label_8862:; 
}
 __return_8867 = status;
}
tmp___8 = __return_8867;
goto label_8820;
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
 __return_7519 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7416:; 
 __return_7516 = status1;
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
goto label_7423;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7433;
}
}
else 
{
label_7423:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7433;
}
else 
{
label_7433:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7463 = 0;
goto label_7459;
}
else 
{
 __return_7459 = -1073741823;
label_7459:; 
}
status1 = ioStatus__Status;
goto label_7416;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7433;
}
}
}
}
status6 = __return_7516;
goto label_7522;
}
status6 = __return_7519;
goto label_7522;
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
 __return_7337 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7229:; 
 __return_7333 = status1;
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
goto label_7286;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7296;
}
}
else 
{
label_7286:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7296;
}
else 
{
label_7296:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7326 = 0;
goto label_7322;
}
else 
{
 __return_7322 = -1073741823;
label_7322:; 
}
status1 = ioStatus__Status;
goto label_7229;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7296;
}
}
}
}
status6 = __return_7333;
goto label_7338;
}
status6 = __return_7337;
label_7338:; 
goto label_8714;
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
 __return_7335 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7232:; 
 __return_7332 = status1;
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
goto label_7239;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7249;
}
}
else 
{
label_7239:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7249;
}
else 
{
label_7249:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7279 = 0;
goto label_7275;
}
else 
{
 __return_7275 = -1073741823;
label_7275:; 
}
status1 = ioStatus__Status;
goto label_7232;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7249;
}
}
}
}
status6 = __return_7332;
goto label_7338;
}
status6 = __return_7335;
goto label_7338;
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
label_7153:; 
Irp__IoStatus__Information = 0;
label_7156:; 
status6 = -1073741808;
goto label_8714;
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
goto label_7153;
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
goto label_7156;
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
 __return_6972 = 0;
goto label_6968;
}
else 
{
if (currentBuffer == 0)
{
 __return_6970 = 0;
goto label_6968;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_6968 = 0;
label_6968:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_6900:; 
if (status5 < 0)
{
 __return_6966 = 0;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_6960:; 
 __return_6964 = returnValue;
}
else 
{
returnValue = 1;
goto label_6960;
}
goto label_6973;
}
goto label_6973;
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
goto label_6907;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6917;
}
}
else 
{
label_6907:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6917;
}
else 
{
label_6917:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6947 = 0;
goto label_6943;
}
else 
{
 __return_6943 = -1073741823;
label_6943:; 
}
status5 = ioStatus__Status;
goto label_6900;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6917;
}
}
}
}
}
label_6973:; 
goto label_6864;
}
}
}
}
else 
{
label_6864:; 
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
goto label_6994;
}
else 
{
{
__VERIFIER_error();
}
label_6994:; 
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
label_7024:; 
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
goto label_7047;
}
else 
{
label_7047:; 
}
goto label_7040;
}
}
else 
{
label_7040:; 
 __return_7055 = myStatus;
}
compRetStatus = __return_7055;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_7014;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_7073;
}
else 
{
{
__VERIFIER_error();
}
label_7073:; 
}
goto label_7014;
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
goto label_7024;
}
else 
{
pended = 1;
goto label_7024;
}
}
}
}
else 
{
label_7014:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_7101;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_7101:; 
goto label_7085;
}
else 
{
returnVal2 = -1073741823;
goto label_7101;
}
}
}
else 
{
returnVal2 = 259;
label_7085:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_7126;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_7135:; 
goto label_7126;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_7135;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_7126;
}
else 
{
{
__VERIFIER_error();
}
label_7126:; 
 __return_7143 = returnVal2;
}
tmp = __return_7143;
 __return_7145 = tmp;
}
tmp___7 = __return_7145;
 __return_7147 = tmp___7;
}
status4 = __return_7147;
goto label_8929;
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_506:; 
if (!(pended == 1))
{
label_8976:; 
if (!(pended == 1))
{
label_8990:; 
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
goto label_9002;
}
else 
{
goto label_9016;
}
}
else 
{
goto label_9016;
}
}
else 
{
label_9016:; 
if (pended != 1)
{
if (s == DC)
{
if (!(status10 == 259))
{
label_9066:; 
goto label_9002;
}
else 
{
{
__VERIFIER_error();
}
goto label_9066;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_9002;
}
else 
{
goto label_9002;
}
}
}
else 
{
goto label_9002;
}
}
}
else 
{
goto label_9002;
}
}
else 
{
label_9002:; 
 __return_9115 = status10;
return 1;
}
}
else 
{
if (s == MPR3)
{
s = MPR3;
goto label_9002;
}
else 
{
goto label_8990;
}
}
}
else 
{
if (s == NP)
{
s = NP;
goto label_9002;
}
else 
{
goto label_8976;
}
}
}
}
