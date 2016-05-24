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
int __return_7829;
int __return_7827;
int __return_640;
int __return_722;
int __return_724;
int __return_839;
int __return_927;
int __return_929;
int __return_1478;
int __return_1557;
int __return_1645;
int __return_1647;
int __return_1649;
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
int __return_2482;
int __return_7825;
int __return_1873;
int __return_1961;
int __return_2018;
int __return_2014;
int __return_2071;
int __return_2442;
int __return_2420;
int __return_2413;
int __return_2409;
int __return_2429;
int __return_2483;
int __return_2440;
int __return_2352;
int __return_2353;
int __return_2346;
int __return_2342;
int __return_2428;
int __return_1728;
int __return_1731;
int __return_1819;
int __return_2065;
int __return_2061;
int __return_2072;
int __return_2441;
int __return_2422;
int __return_2396;
int __return_2392;
int __return_2432;
int __return_2481;
int __return_2438;
int __return_2241;
int __return_2242;
int __return_2235;
int __return_2231;
int __return_2431;
int __return_2589;
int __return_2677;
int __return_2679;
int __return_7654;
int __return_2958;
int __return_3046;
int __return_3048;
int __return_2788;
int __return_2876;
int __return_2878;
int __return_3049;
int __return_3374;
int __return_3375;
int __return_3418;
int __return_3398;
int __return_3368;
int __return_3364;
int __return_3165;
int __return_3253;
int __return_3255;
int __return_3399;
int __return_5568;
int __return_5566;
int __return_5563;
int __return_5560;
int __return_5564;
int __return_5792;
int __return_5694;
int __return_5695;
int __return_5762;
int __return_5688;
int __return_5684;
int __return_5541;
int __return_5537;
int __return_5293;
int __return_5294;
int __return_5455;
int __return_5423;
int __return_5424;
int __return_5825;
int __return_5417;
int __return_5413;
int __return_5287;
int __return_5283;
int __return_5156;
int __return_5157;
int __return_5150;
int __return_5146;
int __return_5037;
int __return_4809;
int __return_4810;
int __return_4983;
int __return_4926;
int __return_4927;
int __return_4957;
int __return_4920;
int __return_4916;
int __return_4803;
int __return_4799;
int __return_5010;
int __return_4647;
int __return_4648;
int __return_4641;
int __return_4637;
int __return_4685;
int __return_4522;
int __return_4492;
int __return_4446;
int __return_4447;
int __return_4440;
int __return_4436;
int __return_4319;
int __return_4320;
int __return_4313;
int __return_4309;
int __return_3980;
int __return_3978;
int __return_3975;
int __return_3972;
int __return_3976;
int __return_4065;
int __return_4153;
int __return_4155;
int __return_4157;
int __return_4189;
int __return_3953;
int __return_3949;
int __return_3778;
int __return_3866;
int __return_3868;
int __return_5763;
int __return_7410;
int __return_7408;
int __return_7405;
int __return_7402;
int __return_7406;
int __return_7647;
int __return_7649;
int __return_7546;
int __return_7547;
int __return_7618;
int __return_7620;
int __return_7540;
int __return_7536;
int __return_7383;
int __return_7379;
int __return_7081;
int __return_7079;
int __return_7076;
int __return_7073;
int __return_7077;
int __return_7298;
int __return_7300;
int __return_7206;
int __return_7207;
int __return_7246;
int __return_7248;
int __return_7200;
int __return_7196;
int __return_7054;
int __return_7050;
int __return_6955;
int __return_6957;
int __return_6925;
int __return_6927;
int __return_6887;
int __return_6888;
int __return_6881;
int __return_6877;
int __return_6755;
int __return_6756;
int __return_6749;
int __return_6745;
int __return_6627;
int __return_6628;
int __return_7590;
int __return_7592;
int __return_6621;
int __return_6617;
int __return_6503;
int __return_6504;
int __return_6497;
int __return_6493;
int __return_6200;
int __return_6198;
int __return_6195;
int __return_6192;
int __return_6196;
int __return_6173;
int __return_6169;
int __return_6281;
int __return_6369;
int __return_6371;
int __return_7593;
int __return_7826;
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
 __return_7829 = -1;
goto label_7827;
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
 __return_7827 = -1;
label_7827:; 
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
 __return_1478 = tmp___0;
label_1478:; 
}
status10 = __return_1478;
goto label_726;
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
goto label_1496;
}
else 
{
{
__VERIFIER_error();
}
label_1496:; 
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
label_1526:; 
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
goto label_1549;
}
else 
{
label_1549:; 
}
goto label_1542;
}
}
else 
{
label_1542:; 
 __return_1557 = myStatus;
}
compRetStatus = __return_1557;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1516;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1575;
}
else 
{
{
__VERIFIER_error();
}
label_1575:; 
}
goto label_1516;
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
goto label_1526;
}
else 
{
pended = 1;
goto label_1526;
}
}
}
}
else 
{
label_1516:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1603;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1603:; 
goto label_1587;
}
else 
{
returnVal2 = -1073741823;
goto label_1603;
}
}
}
else 
{
returnVal2 = 259;
label_1587:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1628;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1637:; 
goto label_1628;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1637;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1628;
}
else 
{
{
__VERIFIER_error();
}
label_1628:; 
 __return_1645 = returnVal2;
}
tmp = __return_1645;
 __return_1647 = tmp;
}
tmp = __return_1647;
 __return_1649 = tmp;
goto label_1478;
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
goto label_1478;
}
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
 __return_2482 = status3;
label_2482:; 
}
status10 = __return_2482;
if (we_should_unload == 0)
{
goto label_7677;
}
else 
{
{
int __tmp_69 = d;
int DriverObject = __tmp_69;
}
label_7677:; 
if (!(pended == 1))
{
label_7688:; 
if (!(pended == 1))
{
label_7702:; 
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
goto label_7714;
}
else 
{
goto label_7728;
}
}
else 
{
goto label_7728;
}
}
else 
{
label_7728:; 
if (pended != 1)
{
if (s == DC)
{
if (!(status10 == 259))
{
label_7774:; 
goto label_7714;
}
else 
{
{
__VERIFIER_error();
}
goto label_7774;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_7714;
}
else 
{
goto label_7714;
}
}
}
else 
{
goto label_7714;
}
}
}
else 
{
goto label_7714;
}
}
else 
{
label_7714:; 
 __return_7825 = status10;
return 1;
}
}
else 
{
if (s == MPR3)
{
s = MPR3;
goto label_7714;
}
else 
{
goto label_7702;
}
}
}
else 
{
if (s == NP)
{
s = NP;
goto label_7714;
}
else 
{
goto label_7688;
}
}
}
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
goto label_1693;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
label_1693:; 
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
label_1842:; 
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
goto label_1865;
}
else 
{
label_1865:; 
}
goto label_1858;
}
}
else 
{
label_1858:; 
 __return_1873 = myStatus;
}
compRetStatus = __return_1873;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1832;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1891;
}
else 
{
{
__VERIFIER_error();
}
label_1891:; 
}
goto label_1832;
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
goto label_1842;
}
else 
{
pended = 1;
goto label_1842;
}
}
}
}
else 
{
label_1832:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1919;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1919:; 
goto label_1903;
}
else 
{
returnVal2 = -1073741823;
goto label_1919;
}
}
}
else 
{
returnVal2 = 259;
label_1903:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1944;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1953:; 
goto label_1944;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1953;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1944;
}
else 
{
{
__VERIFIER_error();
}
label_1944:; 
 __return_1961 = returnVal2;
}
status9 = __return_1961;
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
goto label_1978;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_1988;
}
}
else 
{
label_1978:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_1988;
}
else 
{
label_1988:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2018 = 0;
goto label_2014;
}
else 
{
 __return_2014 = -1073741823;
label_2014:; 
}
status9 = myStatus;
goto label_1972;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_1988;
}
}
}
}
else 
{
label_1972:; 
 __return_2071 = status9;
}
status2 = __return_2071;
if (status2 < 0)
{
 __return_2442 = status2;
goto label_2429;
}
else 
{
if (!(deviceExtension__Active == 255))
{
label_2083:; 
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_2420 = 0;
goto label_2429;
}
else 
{
if (status2 < 0)
{
goto label_2376;
}
else 
{
label_2376:; 
{
int __tmp_85 = deviceParameterHandle;
int Handle = __tmp_85;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2413 = 0;
goto label_2409;
}
else 
{
 __return_2409 = -1073741823;
label_2409:; 
}
 __return_2429 = 0;
label_2429:; 
}
status3 = __return_2429;
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
goto label_2460;
}
else 
{
{
__VERIFIER_error();
}
label_2460:; 
}
 __return_2483 = status3;
goto label_1478;
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
 __return_2440 = 0;
goto label_2429;
}
else 
{
status2 = -1073741823;
label_2108:; 
if (status2 < 0)
{
tmp = attempt;
int __CPAchecker_TMP_0 = attempt;
attempt = attempt + 1;
__CPAchecker_TMP_0;
if (tmp >= 4)
{
goto label_2118;
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
goto label_2292;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_2292:; 
if (irp == 0)
{
 __return_2352 = -1073741670;
goto label_2353;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2299:; 
 __return_2353 = status1;
label_2353:; 
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
goto label_2306;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2316;
}
}
else 
{
label_2306:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2316;
}
else 
{
label_2316:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2346 = 0;
goto label_2342;
}
else 
{
 __return_2342 = -1073741823;
label_2342:; 
}
status1 = ioStatus__Status;
goto label_2299;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2316;
}
}
}
}
status2 = __return_2353;
goto label_2108;
}
}
}
}
}
else 
{
label_2118:; 
if (status2 < 0)
{
deviceExtension__Active = 0;
 __return_2428 = 0;
goto label_2429;
}
else 
{
deviceExtension__Active = 0;
goto label_2083;
}
}
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
 __return_1728 = l;
}
 __return_1731 = -1073741802;
}
compRetStatus = __return_1731;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_1716;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_1749;
}
else 
{
{
__VERIFIER_error();
}
label_1749:; 
}
goto label_1716;
}
}
}
else 
{
label_1716:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_1777;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_1777:; 
goto label_1761;
}
else 
{
returnVal2 = -1073741823;
goto label_1777;
}
}
}
else 
{
returnVal2 = 259;
label_1761:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1802;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_1811:; 
goto label_1802;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1811;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_1802;
}
else 
{
{
__VERIFIER_error();
}
label_1802:; 
 __return_1819 = returnVal2;
}
status9 = __return_1819;
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
goto label_2025;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2035;
}
}
else 
{
label_2025:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2035;
}
else 
{
label_2035:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2065 = 0;
goto label_2061;
}
else 
{
 __return_2061 = -1073741823;
label_2061:; 
}
status9 = myStatus;
goto label_1969;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2035;
}
}
}
}
else 
{
label_1969:; 
 __return_2072 = status9;
}
status2 = __return_2072;
if (status2 < 0)
{
 __return_2441 = status2;
goto label_2432;
}
else 
{
if (!(deviceExtension__Active == 255))
{
label_2080:; 
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_2422 = 0;
goto label_2432;
}
else 
{
if (status2 < 0)
{
goto label_2378;
}
else 
{
label_2378:; 
{
int __tmp_110 = deviceParameterHandle;
int Handle = __tmp_110;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_2396 = 0;
goto label_2392;
}
else 
{
 __return_2392 = -1073741823;
label_2392:; 
}
 __return_2432 = 0;
label_2432:; 
}
status3 = __return_2432;
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
goto label_2476;
}
else 
{
{
__VERIFIER_error();
}
label_2476:; 
}
 __return_2481 = status3;
goto label_2482;
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
 __return_2438 = 0;
goto label_2432;
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
goto label_2181;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_2181:; 
if (irp == 0)
{
 __return_2241 = -1073741670;
goto label_2242;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_2188:; 
 __return_2242 = status1;
label_2242:; 
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
goto label_2195;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_2205;
}
}
else 
{
label_2195:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_2205;
}
else 
{
label_2205:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_2235 = 0;
goto label_2231;
}
else 
{
 __return_2231 = -1073741823;
label_2231:; 
}
status1 = ioStatus__Status;
goto label_2188;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_2205;
}
}
}
}
status2 = __return_2242;
goto label_2107;
}
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
 __return_2431 = 0;
goto label_2432;
}
else 
{
deviceExtension__Active = 0;
goto label_2080;
}
}
}
}
}
}
}
}
}
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
goto label_2528;
}
else 
{
{
__VERIFIER_error();
}
label_2528:; 
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
label_2558:; 
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
goto label_2581;
}
else 
{
label_2581:; 
}
goto label_2574;
}
}
else 
{
label_2574:; 
 __return_2589 = myStatus;
}
compRetStatus = __return_2589;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2548;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_2607;
}
else 
{
{
__VERIFIER_error();
}
label_2607:; 
}
goto label_2548;
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
goto label_2558;
}
else 
{
pended = 1;
goto label_2558;
}
}
}
}
else 
{
label_2548:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_2635;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_2635:; 
goto label_2619;
}
else 
{
returnVal2 = -1073741823;
goto label_2635;
}
}
}
else 
{
returnVal2 = 259;
label_2619:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2660;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_2669:; 
goto label_2660;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2669;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2660;
}
else 
{
{
__VERIFIER_error();
}
label_2660:; 
 __return_2677 = returnVal2;
}
tmp = __return_2677;
 __return_2679 = tmp;
}
status4 = __return_2679;
label_2681:; 
 __return_7654 = status4;
}
status10 = __return_7654;
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
goto label_2889;
}
else 
{
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
goto label_2889;
}
else 
{
compRegistered = 1;
routine = 0;
label_2889:; 
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
label_2927:; 
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
goto label_2950;
}
else 
{
label_2950:; 
}
goto label_2943;
}
}
else 
{
label_2943:; 
 __return_2958 = myStatus;
}
compRetStatus = __return_2958;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2917;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_2976;
}
else 
{
{
__VERIFIER_error();
}
label_2976:; 
}
goto label_2917;
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
goto label_2927;
}
else 
{
pended = 1;
goto label_2927;
}
}
}
}
else 
{
label_2917:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_3004;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_3004:; 
goto label_2988;
}
else 
{
returnVal2 = -1073741823;
goto label_3004;
}
}
}
else 
{
returnVal2 = 259;
label_2988:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3029;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_3038:; 
goto label_3029;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3038;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3029;
}
else 
{
{
__VERIFIER_error();
}
label_3029:; 
 __return_3046 = returnVal2;
}
tmp = __return_3046;
 __return_3048 = tmp;
goto label_3049;
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
goto label_2727;
}
else 
{
{
__VERIFIER_error();
}
label_2727:; 
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
label_2757:; 
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
goto label_2780;
}
else 
{
label_2780:; 
}
goto label_2773;
}
}
else 
{
label_2773:; 
 __return_2788 = myStatus;
}
compRetStatus = __return_2788;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_2747;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_2806;
}
else 
{
{
__VERIFIER_error();
}
label_2806:; 
}
goto label_2747;
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
goto label_2757;
}
else 
{
pended = 1;
goto label_2757;
}
}
}
}
else 
{
label_2747:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_2834;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_2834:; 
goto label_2818;
}
else 
{
returnVal2 = -1073741823;
goto label_2834;
}
}
}
else 
{
returnVal2 = 259;
label_2818:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2859;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_2868:; 
goto label_2859;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2868;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_2859;
}
else 
{
{
__VERIFIER_error();
}
label_2859:; 
 __return_2876 = returnVal2;
}
tmp = __return_2876;
 __return_2878 = tmp;
}
tmp___0 = __return_2878;
 __return_3049 = tmp___0;
label_3049:; 
}
status4 = __return_3049;
label_3051:; 
goto label_2681;
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
goto label_3314;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_3314:; 
if (irp == 0)
{
 __return_3374 = -1073741670;
goto label_3375;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_3321:; 
 __return_3375 = status1;
label_3375:; 
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
goto label_3328;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_3338;
}
}
else 
{
label_3328:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3338;
}
else 
{
label_3338:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_3368 = 0;
goto label_3364;
}
else 
{
 __return_3364 = -1073741823;
label_3364:; 
}
status1 = ioStatus__Status;
goto label_3321;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_3338;
}
}
}
}
status8 = __return_3375;
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
goto label_3413;
}
else 
{
{
__VERIFIER_error();
}
label_3413:; 
}
 __return_3418 = status8;
goto label_3399;
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
goto label_3393;
}
else 
{
{
__VERIFIER_error();
}
label_3393:; 
}
 __return_3398 = status8;
goto label_3399;
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
goto label_3104;
}
else 
{
{
__VERIFIER_error();
}
label_3104:; 
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
label_3134:; 
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
goto label_3157;
}
else 
{
label_3157:; 
}
goto label_3150;
}
}
else 
{
label_3150:; 
 __return_3165 = myStatus;
}
compRetStatus = __return_3165;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_3124;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_3183;
}
else 
{
{
__VERIFIER_error();
}
label_3183:; 
}
goto label_3124;
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
goto label_3134;
}
else 
{
pended = 1;
goto label_3134;
}
}
}
}
else 
{
label_3124:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_3211;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_3211:; 
goto label_3195;
}
else 
{
returnVal2 = -1073741823;
goto label_3211;
}
}
}
else 
{
returnVal2 = 259;
label_3195:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3236;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_3245:; 
goto label_3236;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3245;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3236;
}
else 
{
{
__VERIFIER_error();
}
label_3236:; 
 __return_3253 = returnVal2;
}
tmp = __return_3253;
 __return_3255 = tmp;
}
tmp = __return_3255;
 __return_3399 = tmp;
label_3399:; 
}
status4 = __return_3399;
goto label_3051;
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
goto label_5737;
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
 __return_5568 = 0;
goto label_5564;
}
else 
{
if (currentBuffer == 0)
{
 __return_5566 = 0;
goto label_5564;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_5563 = 0;
goto label_5564;
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_5494:; 
if (status5 < 0)
{
 __return_5560 = 0;
goto label_5564;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_5554:; 
 __return_5564 = returnValue;
label_5564:; 
}
else 
{
returnValue = 1;
goto label_5554;
}
tmp = __return_5564;
if (!(tmp == 0))
{
status7 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_5737;
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
label_5770:; 
myStatus = status7;
{
int __tmp_174 = Irp;
int __tmp_175 = 0;
int Irp = __tmp_174;
int PriorityBoost = __tmp_175;
if (s == NP)
{
s = DC;
goto label_5787;
}
else 
{
{
__VERIFIER_error();
}
label_5787:; 
}
 __return_5792 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5770;
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
goto label_5634;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_5634:; 
if (irp == 0)
{
 __return_5694 = -1073741670;
goto label_5695;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5641:; 
 __return_5695 = status1;
label_5695:; 
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
goto label_5648;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5658;
}
}
else 
{
label_5648:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5658;
}
else 
{
label_5658:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5688 = 0;
goto label_5684;
}
else 
{
 __return_5684 = -1073741823;
label_5684:; 
}
status1 = ioStatus__Status;
goto label_5641;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5658;
}
}
}
}
status7 = __return_5695;
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
label_5740:; 
myStatus = status7;
{
int __tmp_180 = Irp;
int __tmp_181 = 0;
int Irp = __tmp_180;
int PriorityBoost = __tmp_181;
if (s == NP)
{
s = DC;
goto label_5757;
}
else 
{
{
__VERIFIER_error();
}
label_5757:; 
}
 __return_5762 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5740;
}
}
else 
{
goto label_5708;
}
}
else 
{
status7 = 0;
label_5708:; 
goto label_5701;
}
}
else 
{
status7 = 0;
label_5701:; 
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength > sizeof__CDROM_TOC)
{
bytesTransfered = sizeof__CDROM_TOC;
goto label_5718;
}
else 
{
bytesTransfered = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
label_5718:; 
__cil_tmp98 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp98 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength - TrackData__0;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_5727;
}
else 
{
tracksToReturn = tracksOnCd;
label_5727:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_5731;
}
else 
{
label_5731:; 
goto label_5737;
}
}
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
goto label_5501;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5511;
}
}
else 
{
label_5501:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5511;
}
else 
{
label_5511:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5541 = 0;
goto label_5537;
}
else 
{
 __return_5537 = -1073741823;
label_5537:; 
}
status5 = ioStatus__Status;
goto label_5494;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5511;
}
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
goto label_5178;
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
label_5178:; 
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
goto label_5233;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_5233:; 
if (irp == 0)
{
 __return_5293 = -1073741670;
goto label_5294;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5240:; 
 __return_5294 = status1;
label_5294:; 
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
goto label_5247;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5257;
}
}
else 
{
label_5247:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5257;
}
else 
{
label_5257:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5287 = 0;
goto label_5283;
}
else 
{
 __return_5283 = -1073741823;
label_5283:; 
}
status1 = ioStatus__Status;
goto label_5240;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5257;
}
}
}
}
status7 = __return_5294;
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
label_5433:; 
myStatus = status7;
{
int __tmp_196 = Irp;
int __tmp_197 = 0;
int Irp = __tmp_196;
int PriorityBoost = __tmp_197;
if (s == NP)
{
s = DC;
goto label_5450;
}
else 
{
{
__VERIFIER_error();
}
label_5450:; 
}
 __return_5455 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5433;
}
}
else 
{
if (currentIrpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CDROM_PLAY_AUDIO_MSF)
{
status7 = -1073741820;
goto label_5737;
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
goto label_5363;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_5363:; 
if (irp == 0)
{
 __return_5423 = -1073741670;
goto label_5424;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5370:; 
 __return_5424 = status1;
label_5424:; 
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
goto label_5377;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5387;
}
}
else 
{
label_5377:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5387;
}
else 
{
label_5387:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5417 = 0;
goto label_5413;
}
else 
{
 __return_5413 = -1073741823;
label_5413:; 
}
status1 = ioStatus__Status;
goto label_5370;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5387;
}
}
}
}
status7 = __return_5424;
label_5800:; 
__cil_tmp116 = (unsigned long)status7;
if (!(__cil_tmp116 == -2147483626))
{
label_5803:; 
myStatus = status7;
{
int __tmp_202 = Irp;
int __tmp_203 = 0;
int Irp = __tmp_202;
int PriorityBoost = __tmp_203;
if (s == NP)
{
s = DC;
goto label_5820;
}
else 
{
{
__VERIFIER_error();
}
label_5820:; 
}
 __return_5825 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5803;
}
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
goto label_5737;
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
goto label_5096;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_5096:; 
if (irp == 0)
{
 __return_5156 = -1073741670;
goto label_5157;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_5103:; 
 __return_5157 = status1;
label_5157:; 
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
goto label_5110;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_5120;
}
}
else 
{
label_5110:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_5120;
}
else 
{
label_5120:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_5150 = 0;
goto label_5146;
}
else 
{
 __return_5146 = -1073741823;
label_5146:; 
}
status1 = ioStatus__Status;
goto label_5103;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_5120;
}
}
}
}
status7 = __return_5157;
if (status7 < 0)
{
__cil_tmp105 = (unsigned long)status7;
if (!(__cil_tmp105 == -1073741808))
{
goto label_5160;
}
else 
{
status7 = -1073741803;
goto label_5160;
}
}
else 
{
label_5160:; 
goto label_5737;
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
label_5015:; 
myStatus = status7;
{
int __tmp_223 = Irp;
int __tmp_224 = 0;
int Irp = __tmp_223;
int PriorityBoost = __tmp_224;
if (s == NP)
{
s = DC;
goto label_5032;
}
else 
{
{
__VERIFIER_error();
}
label_5032:; 
}
 __return_5037 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_5015;
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
goto label_4749;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_4749:; 
if (irp == 0)
{
 __return_4809 = -1073741670;
goto label_4810;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4756:; 
 __return_4810 = status1;
label_4810:; 
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
goto label_4763;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4773;
}
}
else 
{
label_4763:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4773;
}
else 
{
label_4773:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4803 = 0;
goto label_4799;
}
else 
{
 __return_4799 = -1073741823;
label_4799:; 
}
status1 = ioStatus__Status;
goto label_4756;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4773;
}
}
}
}
status7 = __return_4810;
if (status7 < 0)
{
__cil_tmp109 = (unsigned long)status7;
if (!(__cil_tmp109 == -2147483626))
{
label_4961:; 
myStatus = status7;
{
int __tmp_229 = Irp;
int __tmp_230 = 0;
int Irp = __tmp_229;
int PriorityBoost = __tmp_230;
if (s == NP)
{
s = DC;
goto label_4978;
}
else 
{
{
__VERIFIER_error();
}
label_4978:; 
}
 __return_4983 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4961;
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
goto label_4866;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_4866:; 
if (irp == 0)
{
 __return_4926 = -1073741670;
goto label_4927;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4873:; 
 __return_4927 = status1;
label_4927:; 
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
goto label_4880;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4890;
}
}
else 
{
label_4880:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4890;
}
else 
{
label_4890:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4920 = 0;
goto label_4916;
}
else 
{
 __return_4916 = -1073741823;
label_4916:; 
}
status1 = ioStatus__Status;
goto label_4873;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4890;
}
}
}
}
status7 = __return_4927;
if (status7 < 0)
{
__cil_tmp111 = (unsigned long)status7;
if (!(__cil_tmp111 == -2147483626))
{
label_4935:; 
myStatus = status7;
{
int __tmp_235 = Irp;
int __tmp_236 = 0;
int Irp = __tmp_235;
int PriorityBoost = __tmp_236;
if (s == NP)
{
s = DC;
goto label_4952;
}
else 
{
{
__VERIFIER_error();
}
label_4952:; 
}
 __return_4957 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4935;
}
}
else 
{
goto label_5737;
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
label_4988:; 
myStatus = status7;
{
int __tmp_247 = Irp;
int __tmp_248 = 0;
int Irp = __tmp_247;
int PriorityBoost = __tmp_248;
if (s == NP)
{
s = DC;
goto label_5005;
}
else 
{
{
__VERIFIER_error();
}
label_5005:; 
}
 __return_5010 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4988;
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
goto label_4587;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_4587:; 
if (irp == 0)
{
 __return_4647 = -1073741670;
goto label_4648;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4594:; 
 __return_4648 = status1;
label_4648:; 
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
goto label_4601;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4611;
}
}
else 
{
label_4601:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4611;
}
else 
{
label_4611:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4641 = 0;
goto label_4637;
}
else 
{
 __return_4637 = -1073741823;
label_4637:; 
}
status1 = ioStatus__Status;
goto label_4594;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4611;
}
}
}
}
status7 = __return_4648;
if (status7 >= 0)
{
deviceExtension__PlayActive = 1;
deviceExtension__Paused = 0;
goto label_4651;
}
else 
{
label_4651:; 
goto label_5737;
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
label_4663:; 
myStatus = status7;
{
int __tmp_258 = Irp;
int __tmp_259 = 0;
int Irp = __tmp_258;
int PriorityBoost = __tmp_259;
if (s == NP)
{
s = DC;
goto label_4680;
}
else 
{
{
__VERIFIER_error();
}
label_4680:; 
}
 __return_4685 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4663;
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
goto label_5737;
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
label_4500:; 
myStatus = status7;
{
int __tmp_260 = Irp;
int __tmp_261 = 0;
int Irp = __tmp_260;
int PriorityBoost = __tmp_261;
if (s == NP)
{
s = DC;
goto label_4517;
}
else 
{
{
__VERIFIER_error();
}
label_4517:; 
}
 __return_4522 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4500;
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
label_4470:; 
myStatus = status7;
{
int __tmp_262 = Irp;
int __tmp_263 = 0;
int Irp = __tmp_262;
int PriorityBoost = __tmp_263;
if (s == NP)
{
s = DC;
goto label_4487;
}
else 
{
{
__VERIFIER_error();
}
label_4487:; 
}
 __return_4492 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4470;
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
goto label_4386;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_4386:; 
if (irp == 0)
{
 __return_4446 = -1073741670;
goto label_4447;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4393:; 
 __return_4447 = status1;
label_4447:; 
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
goto label_4400;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4410;
}
}
else 
{
label_4400:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4410;
}
else 
{
label_4410:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4440 = 0;
goto label_4436;
}
else 
{
 __return_4436 = -1073741823;
label_4436:; 
}
status1 = ioStatus__Status;
goto label_4393;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4410;
}
}
}
}
status7 = __return_4447;
if (status7 >= 0)
{
if (!(deviceExtension__Paused == 1))
{
label_4455:; 
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_4453;
}
else 
{
deviceExtension__PlayActive = 0;
goto label_4455;
}
}
else 
{
Irp__IoStatus__Information = 0;
label_4453:; 
goto label_5737;
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
goto label_4259;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_4259:; 
if (irp == 0)
{
 __return_4319 = -1073741670;
goto label_4320;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_4266:; 
 __return_4320 = status1;
label_4320:; 
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
goto label_4273;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_4283;
}
}
else 
{
label_4273:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_4283;
}
else 
{
label_4283:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_4313 = 0;
goto label_4309;
}
else 
{
 __return_4309 = -1073741823;
label_4309:; 
}
status1 = ioStatus__Status;
goto label_4266;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_4283;
}
}
}
}
status7 = __return_4320;
goto label_5737;
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
goto label_4195;
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
label_4195:; 
goto label_4197;
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
label_4197:; 
Irp__IoStatus__Information = 0;
status7 = -1073741808;
label_5737:; 
goto label_5800;
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
 __return_3980 = 0;
goto label_3976;
}
else 
{
if (currentBuffer == 0)
{
 __return_3978 = 0;
goto label_3976;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_3975 = 0;
goto label_3976;
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_3906:; 
if (status5 < 0)
{
 __return_3972 = 0;
goto label_3976;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_3966:; 
 __return_3976 = returnValue;
label_3976:; 
}
else 
{
returnValue = 1;
goto label_3966;
}
tmp___1 = __return_3976;
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
goto label_4004;
}
else 
{
{
__VERIFIER_error();
}
label_4004:; 
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
label_4034:; 
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
goto label_4057;
}
else 
{
label_4057:; 
}
goto label_4050;
}
}
else 
{
label_4050:; 
 __return_4065 = myStatus;
}
compRetStatus = __return_4065;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_4024;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_4083;
}
else 
{
{
__VERIFIER_error();
}
label_4083:; 
}
goto label_4024;
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
goto label_4034;
}
else 
{
pended = 1;
goto label_4034;
}
}
}
}
else 
{
label_4024:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_4111;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_4111:; 
goto label_4095;
}
else 
{
returnVal2 = -1073741823;
goto label_4111;
}
}
}
else 
{
returnVal2 = 259;
label_4095:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4136;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_4145:; 
goto label_4136;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4145;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_4136;
}
else 
{
{
__VERIFIER_error();
}
label_4136:; 
 __return_4153 = returnVal2;
}
tmp = __return_4153;
 __return_4155 = tmp;
}
tmp___0 = __return_4155;
 __return_4157 = tmp___0;
goto label_5763;
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
label_4167:; 
myStatus = status7;
{
int __tmp_291 = Irp;
int __tmp_292 = 0;
int Irp = __tmp_291;
int PriorityBoost = __tmp_292;
if (s == NP)
{
s = DC;
goto label_4184;
}
else 
{
{
__VERIFIER_error();
}
label_4184:; 
}
 __return_4189 = status7;
goto label_5763;
}
}
else 
{
Irp__IoStatus__Information = 0;
goto label_4167;
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
goto label_3913;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_3923;
}
}
else 
{
label_3913:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_3923;
}
else 
{
label_3923:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_3953 = 0;
goto label_3949;
}
else 
{
 __return_3949 = -1073741823;
label_3949:; 
}
status5 = ioStatus__Status;
goto label_3906;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_3923;
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
goto label_3717;
}
else 
{
{
__VERIFIER_error();
}
label_3717:; 
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
label_3747:; 
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
goto label_3770;
}
else 
{
label_3770:; 
}
goto label_3763;
}
}
else 
{
label_3763:; 
 __return_3778 = myStatus;
}
compRetStatus = __return_3778;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_3737;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_3796;
}
else 
{
{
__VERIFIER_error();
}
label_3796:; 
}
goto label_3737;
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
goto label_3747;
}
else 
{
pended = 1;
goto label_3747;
}
}
}
}
else 
{
label_3737:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_3824;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_3824:; 
goto label_3808;
}
else 
{
returnVal2 = -1073741823;
goto label_3824;
}
}
}
else 
{
returnVal2 = 259;
label_3808:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3849;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_3858:; 
goto label_3849;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3858;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_3849;
}
else 
{
{
__VERIFIER_error();
}
label_3849:; 
 __return_3866 = returnVal2;
}
tmp = __return_3866;
 __return_3868 = tmp;
}
tmp___2 = __return_3868;
 __return_5763 = tmp___2;
label_5763:; 
}
status4 = __return_5763;
goto label_3051;
}
}
}
}
}
}
}
}
}
}
}
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
 __return_7410 = 0;
goto label_7406;
}
else 
{
if (currentBuffer == 0)
{
 __return_7408 = 0;
goto label_7406;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_7405 = 0;
goto label_7406;
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_7336:; 
if (status5 < 0)
{
 __return_7402 = 0;
goto label_7406;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_7396:; 
 __return_7406 = returnValue;
label_7406:; 
}
else 
{
returnValue = 1;
goto label_7396;
}
tmp = __return_7406;
if (!(tmp == 0))
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_7420;
}
else 
{
if (!(currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength == 0))
{
status6 = -1073741789;
Irp__IoStatus__Information = 0;
goto label_7420;
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
goto label_7642;
}
else 
{
{
__VERIFIER_error();
}
label_7642:; 
}
 __return_7647 = status;
}
tmp___0 = __return_7647;
 __return_7649 = tmp___0;
goto label_7593;
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
goto label_7486;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_7486:; 
if (irp == 0)
{
 __return_7546 = -1073741670;
goto label_7547;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7493:; 
 __return_7547 = status1;
label_7547:; 
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
goto label_7500;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7510;
}
}
else 
{
label_7500:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7510;
}
else 
{
label_7510:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7540 = 0;
goto label_7536;
}
else 
{
 __return_7536 = -1073741823;
label_7536:; 
}
status1 = ioStatus__Status;
goto label_7493;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7510;
}
}
}
}
status6 = __return_7547;
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
goto label_7613;
}
else 
{
{
__VERIFIER_error();
}
label_7613:; 
}
 __return_7618 = status;
}
tmp___1 = __return_7618;
 __return_7620 = tmp___1;
goto label_7593;
}
}
else 
{
status6 = 0;
Irp__IoStatus__Information = bytesTransfered;
if (lastSession__LogicalBlockAddress == 0)
{
goto label_7420;
}
else 
{
cdaudioDataOut__FirstTrack = 1;
cdaudioDataOut__LastTrack = 2;
goto label_7420;
}
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
goto label_7343;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7353;
}
}
else 
{
label_7343:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7353;
}
else 
{
label_7353:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7383 = 0;
goto label_7379;
}
else 
{
 __return_7379 = -1073741823;
label_7379:; 
}
status5 = ioStatus__Status;
goto label_7336;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7353;
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
goto label_7420;
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
 __return_7081 = 0;
goto label_7077;
}
else 
{
if (currentBuffer == 0)
{
 __return_7079 = 0;
goto label_7077;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_7076 = 0;
goto label_7077;
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_7007:; 
if (status5 < 0)
{
 __return_7073 = 0;
goto label_7077;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_7067:; 
 __return_7077 = returnValue;
label_7077:; 
}
else 
{
returnValue = 1;
goto label_7067;
}
tmp___2 = __return_7077;
if (!(tmp___2 == 0))
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_7420;
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
goto label_7293;
}
else 
{
{
__VERIFIER_error();
}
label_7293:; 
}
 __return_7298 = status;
}
tmp___3 = __return_7298;
 __return_7300 = tmp___3;
goto label_7593;
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
goto label_7146;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_7146:; 
if (irp == 0)
{
 __return_7206 = -1073741670;
goto label_7207;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_7153:; 
 __return_7207 = status1;
label_7207:; 
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
goto label_7160;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7170;
}
}
else 
{
label_7160:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7170;
}
else 
{
label_7170:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7200 = 0;
goto label_7196;
}
else 
{
 __return_7196 = -1073741823;
label_7196:; 
}
status1 = ioStatus__Status;
goto label_7153;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7170;
}
}
}
}
status6 = __return_7207;
if (status6 >= 0)
{
__cil_tmp107 = (unsigned long)status6;
if (__cil_tmp107 != -1073741764)
{
status6 = 0;
goto label_7254;
}
else 
{
goto label_7211;
}
}
else 
{
label_7211:; 
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
goto label_7241;
}
else 
{
{
__VERIFIER_error();
}
label_7241:; 
}
 __return_7246 = status;
}
tmp___4 = __return_7246;
 __return_7248 = tmp___4;
goto label_7593;
}
}
else 
{
label_7254:; 
__cil_tmp109 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp109 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_7261;
}
else 
{
tracksToReturn = tracksOnCd;
label_7261:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_7265;
}
else 
{
label_7265:; 
goto label_7420;
}
}
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
goto label_7014;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_7024;
}
}
else 
{
label_7014:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_7024;
}
else 
{
label_7024:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_7054 = 0;
goto label_7050;
}
else 
{
 __return_7050 = -1073741823;
label_7050:; 
}
status5 = ioStatus__Status;
goto label_7007;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_7024;
}
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
goto label_7420;
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
goto label_6950;
}
else 
{
{
__VERIFIER_error();
}
label_6950:; 
}
 __return_6955 = status;
}
tmp___5 = __return_6955;
 __return_6957 = tmp___5;
goto label_7593;
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
goto label_6920;
}
else 
{
{
__VERIFIER_error();
}
label_6920:; 
}
 __return_6925 = status;
}
tmp___6 = __return_6925;
 __return_6927 = tmp___6;
goto label_7593;
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
goto label_6827;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_6827:; 
if (irp == 0)
{
 __return_6887 = -1073741670;
goto label_6888;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6834:; 
 __return_6888 = status1;
label_6888:; 
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
goto label_6841;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6851;
}
}
else 
{
label_6841:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6851;
}
else 
{
label_6851:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6881 = 0;
goto label_6877;
}
else 
{
 __return_6877 = -1073741823;
label_6877:; 
}
status1 = ioStatus__Status;
goto label_6834;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6851;
}
}
}
}
status6 = __return_6888;
if (status6 >= 0)
{
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_6894;
}
else 
{
Irp__IoStatus__Information = 0;
label_6894:; 
goto label_7420;
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
goto label_7420;
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
goto label_6695;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_6695:; 
if (irp == 0)
{
 __return_6755 = -1073741670;
goto label_6756;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6702:; 
 __return_6756 = status1;
label_6756:; 
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
goto label_6709;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6719;
}
}
else 
{
label_6709:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6719;
}
else 
{
label_6719:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6749 = 0;
goto label_6745;
}
else 
{
 __return_6745 = -1073741823;
label_6745:; 
}
status1 = ioStatus__Status;
goto label_6702;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6719;
}
}
}
}
status6 = __return_6756;
goto label_7567;
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
goto label_7420;
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
goto label_6567;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_6567:; 
if (irp == 0)
{
 __return_6627 = -1073741670;
goto label_6628;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6574:; 
 __return_6628 = status1;
label_6628:; 
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
goto label_6581;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6591;
}
}
else 
{
label_6581:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6591;
}
else 
{
label_6591:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6621 = 0;
goto label_6617;
}
else 
{
 __return_6617 = -1073741823;
label_6617:; 
}
status1 = ioStatus__Status;
goto label_6574;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6591;
}
}
}
}
status6 = __return_6628;
label_7567:; 
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
goto label_7585;
}
else 
{
{
__VERIFIER_error();
}
label_7585:; 
}
 __return_7590 = status;
}
tmp___8 = __return_7590;
 __return_7592 = tmp___8;
goto label_7593;
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
goto label_6443;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_6443:; 
if (irp == 0)
{
 __return_6503 = -1073741670;
goto label_6504;
}
else 
{
__cil_tmp18 = (long)status1;
if (!(__cil_tmp18 == 259L))
{
label_6450:; 
 __return_6504 = status1;
label_6504:; 
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
goto label_6457;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6467;
}
}
else 
{
label_6457:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6467;
}
else 
{
label_6467:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6497 = 0;
goto label_6493;
}
else 
{
 __return_6493 = -1073741823;
label_6493:; 
}
status1 = ioStatus__Status;
goto label_6450;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6467;
}
}
}
}
status6 = __return_6504;
goto label_7420;
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
goto label_6379;
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
label_6379:; 
goto label_6381;
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
label_6381:; 
Irp__IoStatus__Information = 0;
status6 = -1073741808;
label_7420:; 
goto label_7567;
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
 __return_6200 = 0;
goto label_6196;
}
else 
{
if (currentBuffer == 0)
{
 __return_6198 = 0;
goto label_6196;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_6195 = 0;
goto label_6196;
}
else 
{
__cil_tmp10 = (long)status5;
if (!(__cil_tmp10 == 259L))
{
label_6126:; 
if (status5 < 0)
{
 __return_6192 = 0;
goto label_6196;
}
else 
{
if (!(currentBuffer__Header__AudioStatus == 17))
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_6186:; 
 __return_6196 = returnValue;
label_6196:; 
}
else 
{
returnValue = 1;
goto label_6186;
}
goto label_6090;
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
goto label_6133;
}
else 
{
s = NP;
setEventCalled = 0;
goto label_6143;
}
}
else 
{
label_6133:; 
if (!(customIrp == 1))
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
goto label_6143;
}
else 
{
label_6143:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_6173 = 0;
goto label_6169;
}
else 
{
 __return_6169 = -1073741823;
label_6169:; 
}
status5 = ioStatus__Status;
goto label_6126;
}
}
else 
{
s = NP;
customIrp = 0;
goto label_6143;
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
label_6090:; 
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
goto label_6220;
}
else 
{
{
__VERIFIER_error();
}
label_6220:; 
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
label_6250:; 
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
goto label_6273;
}
else 
{
label_6273:; 
}
goto label_6266;
}
}
else 
{
label_6266:; 
 __return_6281 = myStatus;
}
compRetStatus = __return_6281;
__cil_tmp8 = (unsigned long)compRetStatus;
if (!(__cil_tmp8 == -1073741802))
{
goto label_6240;
}
else 
{
{
if (s == NP)
{
s = MPR1;
goto label_6299;
}
else 
{
{
__VERIFIER_error();
}
label_6299:; 
}
goto label_6240;
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
goto label_6250;
}
else 
{
pended = 1;
goto label_6250;
}
}
}
}
else 
{
label_6240:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_6327;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (!(tmp_ndt_6 == 1))
{
returnVal2 = 259;
label_6327:; 
goto label_6311;
}
else 
{
returnVal2 = -1073741823;
goto label_6327;
}
}
}
else 
{
returnVal2 = 259;
label_6311:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_6352;
}
else 
{
if (s == MPR1)
{
if (!(returnVal2 == 259))
{
s = NP;
lowerDriverReturn = returnVal2;
label_6361:; 
goto label_6352;
}
else 
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_6361;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_6352;
}
else 
{
{
__VERIFIER_error();
}
label_6352:; 
 __return_6369 = returnVal2;
}
tmp = __return_6369;
 __return_6371 = tmp;
}
tmp___7 = __return_6371;
 __return_7593 = tmp___7;
label_7593:; 
}
status4 = __return_7593;
goto label_3051;
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_7687:; 
if (!(pended == 1))
{
label_7701:; 
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
goto label_7713;
}
else 
{
goto label_7727;
}
}
else 
{
goto label_7727;
}
}
else 
{
label_7727:; 
if (pended != 1)
{
if (s == DC)
{
if (!(status10 == 259))
{
label_7777:; 
goto label_7713;
}
else 
{
{
__VERIFIER_error();
}
goto label_7777;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_7713;
}
else 
{
goto label_7713;
}
}
}
else 
{
goto label_7713;
}
}
}
else 
{
goto label_7713;
}
}
else 
{
label_7713:; 
 __return_7826 = status10;
goto label_7827;
}
}
else 
{
if (s == MPR3)
{
s = MPR3;
goto label_7713;
}
else 
{
goto label_7701;
}
}
}
else 
{
if (s == NP)
{
s = NP;
goto label_7713;
}
else 
{
goto label_7687;
}
}
}
}
