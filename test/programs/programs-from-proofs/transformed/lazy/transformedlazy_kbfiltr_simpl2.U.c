extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern char __VERIFIER_nondet_char(void);
extern int __VERIFIER_nondet_int(void);
extern long __VERIFIER_nondet_long(void);
extern void *__VERIFIER_nondet_pointer(void);
int KernelMode  ;
int Executive  ;
int DevicePowerState ;
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
int flag=0;
void stub_driver_init(void);
void _BLAST_init(void);
void IofCompleteRequest(int, int);
void errorFn(void);
int KbFilter_PnP(int DeviceObject , int Irp );
int main(void);
void stubMoreProcessingRequired(void);
int IofCallDriver(int DeviceObject , int Irp );
void IofCompleteRequest(int Irp , int PriorityBoost );
int KeSetEvent(int Event , int Increment , int Wait );
int KeWaitForSingleObject(int Object , int WaitReason , int WaitMode , int Alertable ,
                          int Timeout );
int KbFilter_Complete(int DeviceObject , int Irp , int Context );
int KbFilter_CreateClose(int DeviceObject , int Irp );
int KbFilter_DispatchPassThrough(int DeviceObject , int Irp );
int KbFilter_Power(int DeviceObject , int Irp );
int PoCallDriver(int DeviceObject , int Irp );
int KbFilter_InternIoCtl(int DeviceObject , int Irp );
int __return_5335=0;
int __return_4692=0;
int __return_4695=0;
int __return_4943=0;
int __return_4946=0;
int __return_5333=0;
int __return_5329=0;
int __return_4486=0;
int __return_4489=0;
int __return_4694=0;
int __return_4693=0;
int __return_4931=0;
int __return_4934=0;
int __return_4725=0;
int __return_4728=0;
int __return_4933=0;
int __return_4932=0;
int __return_4141=0;
int __return_4144=0;
int __return_4392=0;
int __return_4395=0;
int __return_5327=0;
int __return_3935=0;
int __return_3938=0;
int __return_4143=0;
int __return_4142=0;
int __return_4380=0;
int __return_4383=0;
int __return_4174=0;
int __return_4177=0;
int __return_4382=0;
int __return_4381=0;
int __return_3676=0;
int __return_3679=0;
int __return_3470=0;
int __return_3473=0;
int __return_3678=0;
int __return_3677=0;
int __return_3437=0;
int __return_3440=0;
int __return_3231=0;
int __return_3234=0;
int __return_3439=0;
int __return_3438=0;
int __return_3198=0;
int __return_3201=0;
int __return_3743=0;
int __return_3744=0;
int __return_3745=0;
int __return_3746=0;
int __return_3835=0;
int __return_2992=0;
int __return_2995=0;
int __return_3200=0;
int __return_3199=0;
int __return_2679=0;
int __return_2682=0;
int __return_3836=0;
int __return_2473=0;
int __return_2476=0;
int __return_2681=0;
int __return_2680=0;
int __return_2918=0;
int __return_2921=0;
int __return_2712=0;
int __return_2715=0;
int __return_2920=0;
int __return_2919=0;
int __return_2170=0;
int __return_2173=0;
int __return_3837=0;
int __return_1964=0;
int __return_1967=0;
int __return_2172=0;
int __return_2171=0;
int __return_2409=0;
int __return_2412=0;
int __return_2203=0;
int __return_2206=0;
int __return_2411=0;
int __return_2410=0;
int __return_1662=0;
int __return_1665=0;
int __return_3838=0;
int __return_5325=0;
int __return_1456=0;
int __return_1459=0;
int __return_1664=0;
int __return_1663=0;
int __return_1901=0;
int __return_1904=0;
int __return_1695=0;
int __return_1698=0;
int __return_1903=0;
int __return_1902=0;
int __return_1108=0;
int __return_1111=0;
int __return_1363=0;
int __return_5323=0;
int __return_899=0;
int __return_902=0;
int __return_1110=0;
int __return_1109=0;
int __return_1351=0;
int __return_1354=0;
int __return_1142=0;
int __return_1145=0;
int __return_1353=0;
int __return_1352=0;
int __return_808=0;
int __return_5321=0;
int __return_528=0;
int __return_531=0;
int __return_779=0;
int __return_782=0;
int __return_322=0;
int __return_325=0;
int __return_530=0;
int __return_529=0;
int __return_767=0;
int __return_770=0;
int __return_561=0;
int __return_564=0;
int __return_769=0;
int __return_768=0;
int __return_151=0;
int __return_5331=0;
int __return_5319=0;
int main()
{
int status ;
int irp = __VERIFIER_nondet_int() ;
irp = __VERIFIER_nondet_int();
int pirp ;
int pirp__IoStatus__Status ;
int irp_choice = __VERIFIER_nondet_int() ;
irp_choice = __VERIFIER_nondet_int();
int devobj = __VERIFIER_nondet_int() ;
devobj = __VERIFIER_nondet_int();
int __cil_tmp8 ;
KernelMode = 0;
Executive = 0;
DevicePowerState = 1;
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
status = 0;
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
if (status >= 0)
{
s = NP;
customIrp = 0;
setEventCalled = customIrp;
lowerDriverReturn = setEventCalled;
compRegistered = lowerDriverReturn;
pended = compRegistered;
pirp__IoStatus__Status = 0;
myStatus = 0;
if (irp_choice == 0)
{
pirp__IoStatus__Status = -1073741637;
myStatus = -1073741637;
goto label_112;
}
else 
{
label_112:; 
{
s = NP;
pended = 0;
compFptr = 0;
compRegistered = 0;
lowerDriverReturn = 0;
setEventCalled = 0;
customIrp = 0;
}
if (status < 0)
{
 __return_5335 = -1;
goto label_151;
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
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int devExt__UpperConnectData__ClassService = __VERIFIER_nondet_int() ;
devExt__UpperConnectData__ClassService = __VERIFIER_nondet_int();
int Irp__IoStatus__Status ;
int status ;
int tmp ;
status = myStatus;
if (irpStack__MajorFunction == 0)
{
if (devExt__UpperConnectData__ClassService == 0)
{
status = -1073741436;
goto label_4421;
}
else 
{
label_4421:; 
goto label_4424;
}
}
else 
{
label_4424:; 
Irp__IoStatus__Status = status;
myStatus = status;
{
int __tmp_3 = DeviceObject;
int __tmp_4 = Irp;
int DeviceObject = __tmp_3;
int Irp = __tmp_4;
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int DeviceObject__DeviceExtension__TopOfStack = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension__TopOfStack = __VERIFIER_nondet_int();
int irpStack ;
int tmp ;
irpStack = Irp__Tail__Overlay__CurrentStackLocation;
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_5 = DeviceObject__DeviceExtension__TopOfStack;
int __tmp_6 = Irp;
int DeviceObject = __tmp_5;
int Irp = __tmp_6;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4585;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4585;
}
else 
{
returnVal2 = 259;
label_4585:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4691;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4673;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4673:; 
goto label_4691;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4691:; 
 __return_4692 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4620:; 
 __return_4695 = returnVal2;
}
tmp = __return_4692;
goto label_4697;
tmp = __return_4695;
label_4697:; 
 __return_4943 = tmp;
}
tmp = __return_4943;
 __return_4946 = tmp;
}
status = __return_4946;
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_5307;
}
else 
{
goto label_4991;
}
}
else 
{
label_4991:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_5307;
}
else 
{
goto label_5043;
}
}
else 
{
label_5043:; 
if (s != UNLOADED)
{
if (status != -1)
{
if (s != SKIP2)
{
if (s != IPC)
{
if (s == DC)
{
goto label_5169;
}
else 
{
goto label_5307;
}
}
else 
{
goto label_5169;
}
}
else 
{
label_5169:; 
if (pended == 1)
{
if (status != 259)
{
{
__VERIFIER_error();
}
label_5213:; 
 __return_5333 = status;
goto label_151;
}
else 
{
goto label_5307;
}
}
else 
{
goto label_5307;
}
}
}
else 
{
goto label_5307;
}
}
else 
{
label_5307:; 
 __return_5329 = status;
goto label_151;
}
}
}
}
}
}
else 
{
{
int __tmp_7 = DeviceObject;
int __tmp_8 = Irp;
int __tmp_9 = lcontext;
int DeviceObject = __tmp_7;
int Irp = __tmp_8;
int Context = __tmp_9;
int event ;
event = Context;
{
int __tmp_10 = event;
int __tmp_11 = 0;
int __tmp_12 = 0;
int Event = __tmp_10;
int Increment = __tmp_11;
int Wait = __tmp_12;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_4486 = l;
}
 __return_4489 = -1073741802;
}
compRetStatus = __return_4489;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_4516;
label_4516:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4581;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4581;
}
else 
{
returnVal2 = 259;
label_4581:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4687;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4669;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4669:; 
goto label_4687;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4687:; 
 __return_4694 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4620;
}
tmp = __return_4694;
goto label_4697;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4583;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4583;
}
else 
{
returnVal2 = 259;
label_4583:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4689;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4671;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4671:; 
goto label_4689;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4689:; 
 __return_4693 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4620;
}
tmp = __return_4693;
goto label_4697;
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
{
int __tmp_13 = DeviceObject__DeviceExtension__TopOfStack;
int __tmp_14 = Irp;
int DeviceObject = __tmp_13;
int Irp = __tmp_14;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4824;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4824;
}
else 
{
returnVal2 = 259;
label_4824:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4930;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4912;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4912:; 
goto label_4930;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4930:; 
 __return_4931 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4859:; 
 __return_4934 = returnVal2;
}
tmp = __return_4931;
goto label_4697;
tmp = __return_4934;
goto label_4697;
}
}
}
}
}
else 
{
{
int __tmp_15 = DeviceObject;
int __tmp_16 = Irp;
int __tmp_17 = lcontext;
int DeviceObject = __tmp_15;
int Irp = __tmp_16;
int Context = __tmp_17;
int event ;
event = Context;
{
int __tmp_18 = event;
int __tmp_19 = 0;
int __tmp_20 = 0;
int Event = __tmp_18;
int Increment = __tmp_19;
int Wait = __tmp_20;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_4725 = l;
}
 __return_4728 = -1073741802;
}
compRetStatus = __return_4728;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_4755;
label_4755:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4820;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4820;
}
else 
{
returnVal2 = 259;
label_4820:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4926;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4908;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4908:; 
goto label_4926;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4926:; 
 __return_4933 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4859;
}
tmp = __return_4933;
goto label_4697;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4822;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4822;
}
else 
{
returnVal2 = 259;
label_4822:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4928;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4910;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4910:; 
goto label_4928;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4928:; 
 __return_4932 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4859;
}
tmp = __return_4932;
goto label_4697;
}
}
}
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
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 1)
{
{
int __tmp_21 = devobj;
int __tmp_22 = pirp;
int DeviceObject = __tmp_21;
int Irp = __tmp_22;
int irpStack__MajorFunction = __VERIFIER_nondet_int() ;
irpStack__MajorFunction = __VERIFIER_nondet_int();
int devExt__UpperConnectData__ClassService = __VERIFIER_nondet_int() ;
devExt__UpperConnectData__ClassService = __VERIFIER_nondet_int();
int Irp__IoStatus__Status ;
int status ;
int tmp ;
status = myStatus;
if (irpStack__MajorFunction == 0)
{
if (devExt__UpperConnectData__ClassService == 0)
{
status = -1073741436;
goto label_3870;
}
else 
{
label_3870:; 
goto label_3873;
}
}
else 
{
label_3873:; 
Irp__IoStatus__Status = status;
myStatus = status;
{
int __tmp_23 = DeviceObject;
int __tmp_24 = Irp;
int DeviceObject = __tmp_23;
int Irp = __tmp_24;
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int DeviceObject__DeviceExtension__TopOfStack = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension__TopOfStack = __VERIFIER_nondet_int();
int irpStack ;
int tmp ;
irpStack = Irp__Tail__Overlay__CurrentStackLocation;
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_25 = DeviceObject__DeviceExtension__TopOfStack;
int __tmp_26 = Irp;
int DeviceObject = __tmp_25;
int Irp = __tmp_26;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4034;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4034;
}
else 
{
returnVal2 = 259;
label_4034:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4140;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4122;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4122:; 
goto label_4140;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4140:; 
 __return_4141 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4069:; 
 __return_4144 = returnVal2;
}
tmp = __return_4141;
goto label_4146;
tmp = __return_4144;
label_4146:; 
 __return_4392 = tmp;
}
tmp = __return_4392;
 __return_4395 = tmp;
}
status = __return_4395;
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_5309;
}
else 
{
goto label_4993;
}
}
else 
{
label_4993:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_5309;
}
else 
{
goto label_5041;
}
}
else 
{
label_5041:; 
if (s != UNLOADED)
{
if (status != -1)
{
if (s != SKIP2)
{
if (s != IPC)
{
if (s == DC)
{
goto label_5167;
}
else 
{
goto label_5309;
}
}
else 
{
goto label_5167;
}
}
else 
{
label_5167:; 
if (pended == 1)
{
if (status != 259)
{
{
__VERIFIER_error();
}
goto label_5213;
}
else 
{
goto label_5309;
}
}
else 
{
goto label_5309;
}
}
}
else 
{
goto label_5309;
}
}
else 
{
label_5309:; 
 __return_5327 = status;
goto label_151;
}
}
}
}
}
}
else 
{
{
int __tmp_27 = DeviceObject;
int __tmp_28 = Irp;
int __tmp_29 = lcontext;
int DeviceObject = __tmp_27;
int Irp = __tmp_28;
int Context = __tmp_29;
int event ;
event = Context;
{
int __tmp_30 = event;
int __tmp_31 = 0;
int __tmp_32 = 0;
int Event = __tmp_30;
int Increment = __tmp_31;
int Wait = __tmp_32;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_3935 = l;
}
 __return_3938 = -1073741802;
}
compRetStatus = __return_3938;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_3965;
label_3965:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4030;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4030;
}
else 
{
returnVal2 = 259;
label_4030:; 
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
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4118;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4118:; 
goto label_4136;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4136:; 
 __return_4143 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4069;
}
tmp = __return_4143;
goto label_4146;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4032;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4032;
}
else 
{
returnVal2 = 259;
label_4032:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4138;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4120;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4120:; 
goto label_4138;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4138:; 
 __return_4142 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4069;
}
tmp = __return_4142;
goto label_4146;
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
{
int __tmp_33 = DeviceObject__DeviceExtension__TopOfStack;
int __tmp_34 = Irp;
int DeviceObject = __tmp_33;
int Irp = __tmp_34;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4273;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4273;
}
else 
{
returnVal2 = 259;
label_4273:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4379;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4361;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4361:; 
goto label_4379;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4379:; 
 __return_4380 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4308:; 
 __return_4383 = returnVal2;
}
tmp = __return_4380;
goto label_4146;
tmp = __return_4383;
goto label_4146;
}
}
}
}
}
else 
{
{
int __tmp_35 = DeviceObject;
int __tmp_36 = Irp;
int __tmp_37 = lcontext;
int DeviceObject = __tmp_35;
int Irp = __tmp_36;
int Context = __tmp_37;
int event ;
event = Context;
{
int __tmp_38 = event;
int __tmp_39 = 0;
int __tmp_40 = 0;
int Event = __tmp_38;
int Increment = __tmp_39;
int Wait = __tmp_40;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_4174 = l;
}
 __return_4177 = -1073741802;
}
compRetStatus = __return_4177;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_4204;
label_4204:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4269;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4269;
}
else 
{
returnVal2 = 259;
label_4269:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4375;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4357;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4357:; 
goto label_4375;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4375:; 
 __return_4382 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4308;
}
tmp = __return_4382;
goto label_4146;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_4271;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4271;
}
else 
{
returnVal2 = 259;
label_4271:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4377;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4359;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4359:; 
goto label_4377;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4377:; 
 __return_4381 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4308;
}
tmp = __return_4381;
goto label_4146;
}
}
}
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
int __tmp_41 = devobj;
int __tmp_42 = pirp;
int DeviceObject = __tmp_41;
int Irp = __tmp_42;
int devExt ;
int irpStack ;
int status ;
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int irpStack__MinorFunction = __VERIFIER_nondet_int() ;
irpStack__MinorFunction = __VERIFIER_nondet_int();
int devExt__TopOfStack = __VERIFIER_nondet_int() ;
devExt__TopOfStack = __VERIFIER_nondet_int();
int devExt__Started ;
int devExt__Removed ;
int devExt__SurpriseRemoved ;
int Irp__IoStatus__Status ;
int Irp__IoStatus__Information ;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int irpSp ;
int nextIrpSp ;
int nextIrpSp__Control ;
int irpSp___0 ;
int irpSp__Context ;
int irpSp__Control ;
long __cil_tmp23 ;
status = 0;
devExt = DeviceObject__DeviceExtension;
irpStack = Irp__Tail__Overlay__CurrentStackLocation;
if (irpStack__MinorFunction == 0)
{
irpSp = Irp__Tail__Overlay__CurrentStackLocation;
nextIrpSp = Irp__Tail__Overlay__CurrentStackLocation - 1;
nextIrpSp__Control = 0;
flag = s - NP;
if (s != NP)
{
{
__VERIFIER_error();
}
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_43 = devExt__TopOfStack;
int __tmp_44 = Irp;
int DeviceObject = __tmp_43;
int Irp = __tmp_44;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_3569;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3569;
}
else 
{
returnVal2 = 259;
label_3569:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3675;
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
goto label_3675;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3675:; 
 __return_3676 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3604:; 
 __return_3679 = returnVal2;
}
status = __return_3676;
goto label_3203;
status = __return_3679;
goto label_3203;
}
}
}
}
}
else 
{
{
int __tmp_45 = DeviceObject;
int __tmp_46 = Irp;
int __tmp_47 = lcontext;
int DeviceObject = __tmp_45;
int Irp = __tmp_46;
int Context = __tmp_47;
int event ;
event = Context;
{
int __tmp_48 = event;
int __tmp_49 = 0;
int __tmp_50 = 0;
int Event = __tmp_48;
int Increment = __tmp_49;
int Wait = __tmp_50;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_3470 = l;
}
 __return_3473 = -1073741802;
}
compRetStatus = __return_3473;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_3500;
label_3500:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_3565;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3565;
}
else 
{
returnVal2 = 259;
label_3565:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3671;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3653;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3653:; 
goto label_3671;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3671:; 
 __return_3678 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3604;
}
status = __return_3678;
goto label_3203;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_3567;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3567;
}
else 
{
returnVal2 = 259;
label_3567:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3673;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3655;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3655:; 
goto label_3673;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3673:; 
 __return_3677 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3604;
}
status = __return_3677;
goto label_3203;
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
flag = compRegistered;
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_51 = devExt__TopOfStack;
int __tmp_52 = Irp;
int DeviceObject = __tmp_51;
int Irp = __tmp_52;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_3330;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3330;
}
else 
{
returnVal2 = 259;
label_3330:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3436;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3418;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3418:; 
goto label_3436;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3436:; 
 __return_3437 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3365:; 
 __return_3440 = returnVal2;
}
status = __return_3437;
goto label_3203;
status = __return_3440;
goto label_3203;
}
}
}
}
}
else 
{
{
int __tmp_53 = DeviceObject;
int __tmp_54 = Irp;
int __tmp_55 = lcontext;
int DeviceObject = __tmp_53;
int Irp = __tmp_54;
int Context = __tmp_55;
int event ;
event = Context;
{
int __tmp_56 = event;
int __tmp_57 = 0;
int __tmp_58 = 0;
int Event = __tmp_56;
int Increment = __tmp_57;
int Wait = __tmp_58;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_3231 = l;
}
 __return_3234 = -1073741802;
}
compRetStatus = __return_3234;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_3261;
label_3261:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_3326;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3326;
}
else 
{
returnVal2 = 259;
label_3326:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3432;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3414;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3414:; 
goto label_3432;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3432:; 
 __return_3439 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3365;
}
status = __return_3439;
goto label_3203;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_3328;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3328;
}
else 
{
returnVal2 = 259;
label_3328:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3434;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3416;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3416:; 
goto label_3434;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3434:; 
 __return_3438 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3365;
}
status = __return_3438;
goto label_3203;
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
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_59 = devExt__TopOfStack;
int __tmp_60 = Irp;
int DeviceObject = __tmp_59;
int Irp = __tmp_60;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_3091;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3091;
}
else 
{
returnVal2 = 259;
label_3091:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3197;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3179;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3179:; 
goto label_3197;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3197:; 
 __return_3198 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3126:; 
 __return_3201 = returnVal2;
}
status = __return_3198;
goto label_3203;
status = __return_3201;
label_3203:; 
__cil_tmp23 = (long)status;
if (__cil_tmp23 == 259)
{
{
int __tmp_61 = event;
int __tmp_62 = Executive;
int __tmp_63 = KernelMode;
int __tmp_64 = 0;
int __tmp_65 = 0;
int Object = __tmp_61;
int WaitReason = __tmp_62;
int WaitMode = __tmp_63;
int Alertable = __tmp_64;
int Timeout = __tmp_65;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_3724;
}
else 
{
goto label_3700;
}
}
else 
{
label_3700:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_3724;
}
else 
{
flag = s - MPR3;
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
 __return_3743 = 0;
goto label_3744;
}
else 
{
 __return_3744 = -1073741823;
label_3744:; 
}
goto label_3748;
}
else 
{
label_3724:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
 __return_3745 = 0;
goto label_3746;
}
else 
{
 __return_3746 = -1073741823;
label_3746:; 
}
label_3748:; 
if (status >= 0)
{
if (myStatus >= 0)
{
devExt__Started = 1;
devExt__Removed = 0;
devExt__SurpriseRemoved = 0;
goto label_3773;
}
else 
{
goto label_3773;
}
}
else 
{
label_3773:; 
Irp__IoStatus__Status = status;
myStatus = status;
Irp__IoStatus__Information = 0;
{
int __tmp_66 = Irp;
int __tmp_67 = 0;
int Irp = __tmp_66;
int PriorityBoost = __tmp_67;
flag = s - NP;
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
goto label_3800;
goto label_3800;
}
}
}
}
}
}
}
else 
{
if (status >= 0)
{
if (myStatus >= 0)
{
devExt__Started = 1;
devExt__Removed = 0;
devExt__SurpriseRemoved = 0;
goto label_3771;
}
else 
{
goto label_3771;
}
}
else 
{
label_3771:; 
Irp__IoStatus__Status = status;
myStatus = status;
Irp__IoStatus__Information = 0;
{
int __tmp_68 = Irp;
int __tmp_69 = 0;
int Irp = __tmp_68;
int PriorityBoost = __tmp_69;
flag = s - NP;
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
goto label_3800;
label_3800:; 
 __return_3835 = status;
}
status = __return_3835;
goto label_3840;
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
int __tmp_70 = DeviceObject;
int __tmp_71 = Irp;
int __tmp_72 = lcontext;
int DeviceObject = __tmp_70;
int Irp = __tmp_71;
int Context = __tmp_72;
int event ;
event = Context;
{
int __tmp_73 = event;
int __tmp_74 = 0;
int __tmp_75 = 0;
int Event = __tmp_73;
int Increment = __tmp_74;
int Wait = __tmp_75;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2992 = l;
}
 __return_2995 = -1073741802;
}
compRetStatus = __return_2995;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_3022;
label_3022:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_3087;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3087;
}
else 
{
returnVal2 = 259;
label_3087:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3193;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3175;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3175:; 
goto label_3193;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3193:; 
 __return_3200 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3126;
}
status = __return_3200;
goto label_3203;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_3089;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3089;
}
else 
{
returnVal2 = 259;
label_3089:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3195;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3177;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3177:; 
goto label_3195;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3195:; 
 __return_3199 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3126;
}
status = __return_3199;
goto label_3203;
}
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
if (irpStack__MinorFunction == 23)
{
devExt__SurpriseRemoved = 1;
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_76 = devExt__TopOfStack;
int __tmp_77 = Irp;
int DeviceObject = __tmp_76;
int Irp = __tmp_77;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2572;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2572;
}
else 
{
returnVal2 = 259;
label_2572:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2678;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2660;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2660:; 
goto label_2678;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2678:; 
 __return_2679 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2607:; 
 __return_2682 = returnVal2;
}
status = __return_2679;
goto label_2684;
status = __return_2682;
label_2684:; 
 __return_3836 = status;
}
status = __return_3836;
goto label_3840;
}
}
}
}
else 
{
{
int __tmp_78 = DeviceObject;
int __tmp_79 = Irp;
int __tmp_80 = lcontext;
int DeviceObject = __tmp_78;
int Irp = __tmp_79;
int Context = __tmp_80;
int event ;
event = Context;
{
int __tmp_81 = event;
int __tmp_82 = 0;
int __tmp_83 = 0;
int Event = __tmp_81;
int Increment = __tmp_82;
int Wait = __tmp_83;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2473 = l;
}
 __return_2476 = -1073741802;
}
compRetStatus = __return_2476;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_2503;
label_2503:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2568;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2568;
}
else 
{
returnVal2 = 259;
label_2568:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2674;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2656;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2656:; 
goto label_2674;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2674:; 
 __return_2681 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2607;
}
status = __return_2681;
goto label_2684;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2570;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2570;
}
else 
{
returnVal2 = 259;
label_2570:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2676;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2658;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2658:; 
goto label_2676;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2676:; 
 __return_2680 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2607;
}
status = __return_2680;
goto label_2684;
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
{
int __tmp_84 = devExt__TopOfStack;
int __tmp_85 = Irp;
int DeviceObject = __tmp_84;
int Irp = __tmp_85;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2811;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2811;
}
else 
{
returnVal2 = 259;
label_2811:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2917;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2899;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2899:; 
goto label_2917;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2917:; 
 __return_2918 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2846:; 
 __return_2921 = returnVal2;
}
status = __return_2918;
goto label_2684;
status = __return_2921;
goto label_2684;
}
}
}
}
}
else 
{
{
int __tmp_86 = DeviceObject;
int __tmp_87 = Irp;
int __tmp_88 = lcontext;
int DeviceObject = __tmp_86;
int Irp = __tmp_87;
int Context = __tmp_88;
int event ;
event = Context;
{
int __tmp_89 = event;
int __tmp_90 = 0;
int __tmp_91 = 0;
int Event = __tmp_89;
int Increment = __tmp_90;
int Wait = __tmp_91;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2712 = l;
}
 __return_2715 = -1073741802;
}
compRetStatus = __return_2715;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_2742;
label_2742:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2807;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2807;
}
else 
{
returnVal2 = 259;
label_2807:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2913;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2895;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2895:; 
goto label_2913;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2913:; 
 __return_2920 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2846;
}
status = __return_2920;
goto label_2684;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2809;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2809;
}
else 
{
returnVal2 = 259;
label_2809:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2915;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2897;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2897:; 
goto label_2915;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2915:; 
 __return_2919 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2846;
}
status = __return_2919;
goto label_2684;
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
if (irpStack__MinorFunction == 2)
{
devExt__Removed = 1;
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_2 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_2;
int __CPAchecker_TMP_3 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_3;
{
int __tmp_92 = devExt__TopOfStack;
int __tmp_93 = Irp;
int DeviceObject = __tmp_92;
int Irp = __tmp_93;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2063;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2063;
}
else 
{
returnVal2 = 259;
label_2063:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2169;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2151;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2151:; 
goto label_2169;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2169:; 
 __return_2170 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2098:; 
 __return_2173 = returnVal2;
}
goto label_2175;
label_2175:; 
status = 0;
 __return_3837 = status;
}
status = __return_3837;
goto label_3840;
}
}
}
}
else 
{
{
int __tmp_94 = DeviceObject;
int __tmp_95 = Irp;
int __tmp_96 = lcontext;
int DeviceObject = __tmp_94;
int Irp = __tmp_95;
int Context = __tmp_96;
int event ;
event = Context;
{
int __tmp_97 = event;
int __tmp_98 = 0;
int __tmp_99 = 0;
int Event = __tmp_97;
int Increment = __tmp_98;
int Wait = __tmp_99;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1964 = l;
}
 __return_1967 = -1073741802;
}
compRetStatus = __return_1967;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_1994;
label_1994:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2059;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2059;
}
else 
{
returnVal2 = 259;
label_2059:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2165;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2147;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2147:; 
goto label_2165;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2165:; 
 __return_2172 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2098;
}
goto label_2175;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2061;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2061;
}
else 
{
returnVal2 = 259;
label_2061:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2167;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2149;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2149:; 
goto label_2167;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2167:; 
 __return_2171 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2098;
}
goto label_2175;
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
int __CPAchecker_TMP_2 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_2;
int __CPAchecker_TMP_3 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_3;
{
int __tmp_100 = devExt__TopOfStack;
int __tmp_101 = Irp;
int DeviceObject = __tmp_100;
int Irp = __tmp_101;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2302;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2302;
}
else 
{
returnVal2 = 259;
label_2302:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2408;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2390;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2390:; 
goto label_2408;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2408:; 
 __return_2409 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2337:; 
 __return_2412 = returnVal2;
}
goto label_2175;
goto label_2175;
}
}
}
}
}
else 
{
{
int __tmp_102 = DeviceObject;
int __tmp_103 = Irp;
int __tmp_104 = lcontext;
int DeviceObject = __tmp_102;
int Irp = __tmp_103;
int Context = __tmp_104;
int event ;
event = Context;
{
int __tmp_105 = event;
int __tmp_106 = 0;
int __tmp_107 = 0;
int Event = __tmp_105;
int Increment = __tmp_106;
int Wait = __tmp_107;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2203 = l;
}
 __return_2206 = -1073741802;
}
compRetStatus = __return_2206;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_2233;
label_2233:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2298;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2298;
}
else 
{
returnVal2 = 259;
label_2298:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2404;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2386;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2386:; 
goto label_2404;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2404:; 
 __return_2411 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2337;
}
goto label_2175;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2300;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2300;
}
else 
{
returnVal2 = 259;
label_2300:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2406;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2388;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2388:; 
goto label_2406;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2406:; 
 __return_2410 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2337;
}
goto label_2175;
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
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_4 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_4;
int __CPAchecker_TMP_5 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_5;
{
int __tmp_108 = devExt__TopOfStack;
int __tmp_109 = Irp;
int DeviceObject = __tmp_108;
int Irp = __tmp_109;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_1555;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_1555;
}
else 
{
returnVal2 = 259;
label_1555:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1661;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1643;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1643:; 
goto label_1661;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1661:; 
 __return_1662 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1590:; 
 __return_1665 = returnVal2;
}
status = __return_1662;
goto label_1667;
status = __return_1665;
label_1667:; 
 __return_3838 = status;
}
status = __return_3838;
label_3840:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_5311;
}
else 
{
goto label_4995;
}
}
else 
{
label_4995:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_5311;
}
else 
{
goto label_5039;
}
}
else 
{
label_5039:; 
if (s != UNLOADED)
{
if (status != -1)
{
if (s != SKIP2)
{
if (s != IPC)
{
if (s == DC)
{
goto label_5165;
}
else 
{
goto label_5311;
}
}
else 
{
goto label_5165;
}
}
else 
{
label_5165:; 
if (pended == 1)
{
if (status != 259)
{
{
__VERIFIER_error();
}
goto label_5213;
}
else 
{
goto label_5311;
}
}
else 
{
goto label_5311;
}
}
}
else 
{
goto label_5311;
}
}
else 
{
label_5311:; 
 __return_5325 = status;
goto label_151;
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
int __tmp_110 = DeviceObject;
int __tmp_111 = Irp;
int __tmp_112 = lcontext;
int DeviceObject = __tmp_110;
int Irp = __tmp_111;
int Context = __tmp_112;
int event ;
event = Context;
{
int __tmp_113 = event;
int __tmp_114 = 0;
int __tmp_115 = 0;
int Event = __tmp_113;
int Increment = __tmp_114;
int Wait = __tmp_115;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1456 = l;
}
 __return_1459 = -1073741802;
}
compRetStatus = __return_1459;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_1486;
label_1486:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_1551;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_1551;
}
else 
{
returnVal2 = 259;
label_1551:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1657;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1639;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1639:; 
goto label_1657;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1657:; 
 __return_1664 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1590;
}
status = __return_1664;
goto label_1667;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_1553;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_1553;
}
else 
{
returnVal2 = 259;
label_1553:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1659;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1641;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1641:; 
goto label_1659;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1659:; 
 __return_1663 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1590;
}
status = __return_1663;
goto label_1667;
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
int __CPAchecker_TMP_4 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_4;
int __CPAchecker_TMP_5 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_5;
{
int __tmp_116 = devExt__TopOfStack;
int __tmp_117 = Irp;
int DeviceObject = __tmp_116;
int Irp = __tmp_117;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_1794;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_1794;
}
else 
{
returnVal2 = 259;
label_1794:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1900;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1882;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1882:; 
goto label_1900;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1900:; 
 __return_1901 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1829:; 
 __return_1904 = returnVal2;
}
status = __return_1901;
goto label_1667;
status = __return_1904;
goto label_1667;
}
}
}
}
}
else 
{
{
int __tmp_118 = DeviceObject;
int __tmp_119 = Irp;
int __tmp_120 = lcontext;
int DeviceObject = __tmp_118;
int Irp = __tmp_119;
int Context = __tmp_120;
int event ;
event = Context;
{
int __tmp_121 = event;
int __tmp_122 = 0;
int __tmp_123 = 0;
int Event = __tmp_121;
int Increment = __tmp_122;
int Wait = __tmp_123;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1695 = l;
}
 __return_1698 = -1073741802;
}
compRetStatus = __return_1698;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_1725;
label_1725:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_1790;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_1790;
}
else 
{
returnVal2 = 259;
label_1790:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1896;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1878;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1878:; 
goto label_1896;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1896:; 
 __return_1903 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1829;
}
status = __return_1903;
goto label_1667;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_1792;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_1792;
}
else 
{
returnVal2 = 259;
label_1792:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1898;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1880;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1880:; 
goto label_1898;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1898:; 
 __return_1902 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1829;
}
status = __return_1902;
goto label_1667;
}
}
}
}
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
int __tmp_124 = devobj;
int __tmp_125 = pirp;
int DeviceObject = __tmp_124;
int Irp = __tmp_125;
int irpStack__MinorFunction = __VERIFIER_nondet_int() ;
irpStack__MinorFunction = __VERIFIER_nondet_int();
int devExt__DeviceState ;
int powerState__DeviceState = __VERIFIER_nondet_int() ;
powerState__DeviceState = __VERIFIER_nondet_int();
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int devExt__TopOfStack = __VERIFIER_nondet_int() ;
devExt__TopOfStack = __VERIFIER_nondet_int();
int powerType = __VERIFIER_nondet_int() ;
powerType = __VERIFIER_nondet_int();
int tmp ;
if (irpStack__MinorFunction == 2)
{
if (powerType == DevicePowerState)
{
devExt__DeviceState = powerState__DeviceState;
goto label_842;
}
else 
{
label_842:; 
goto label_850;
}
}
else 
{
label_850:; 
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_126 = devExt__TopOfStack;
int __tmp_127 = Irp;
int DeviceObject = __tmp_126;
int Irp = __tmp_127;
int compRetStatus ;
int returnVal ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
long __cil_tmp8 ;
if (compRegistered == 0)
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
returnVal = 0;
goto label_998;
}
else 
{
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 1)
{
returnVal = -1073741823;
goto label_998;
}
else 
{
returnVal = 259;
label_998:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_1107;
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
goto label_1085;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1085:; 
goto label_1107;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_1107:; 
 __return_1108 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
label_1033:; 
 __return_1111 = returnVal;
}
tmp = __return_1108;
goto label_1113;
tmp = __return_1111;
label_1113:; 
 __return_1363 = tmp;
}
status = __return_1363;
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_5313;
}
else 
{
goto label_4997;
}
}
else 
{
label_4997:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_5313;
}
else 
{
goto label_5037;
}
}
else 
{
label_5037:; 
if (s != UNLOADED)
{
if (status != -1)
{
if (s != SKIP2)
{
if (s != IPC)
{
if (s == DC)
{
goto label_5163;
}
else 
{
goto label_5313;
}
}
else 
{
goto label_5163;
}
}
else 
{
label_5163:; 
if (pended == 1)
{
if (status != 259)
{
{
__VERIFIER_error();
}
goto label_5213;
}
else 
{
goto label_5313;
}
}
else 
{
goto label_5313;
}
}
}
else 
{
goto label_5313;
}
}
else 
{
label_5313:; 
 __return_5323 = status;
goto label_151;
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
int __tmp_128 = DeviceObject;
int __tmp_129 = Irp;
int __tmp_130 = lcontext;
int DeviceObject = __tmp_128;
int Irp = __tmp_129;
int Context = __tmp_130;
int event ;
event = Context;
{
int __tmp_131 = event;
int __tmp_132 = 0;
int __tmp_133 = 0;
int Event = __tmp_131;
int Increment = __tmp_132;
int Wait = __tmp_133;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_899 = l;
}
 __return_902 = -1073741802;
}
compRetStatus = __return_902;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_929;
label_929:; 
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
returnVal = 0;
goto label_994;
}
else 
{
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 1)
{
returnVal = -1073741823;
goto label_994;
}
else 
{
returnVal = 259;
label_994:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_1103;
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
goto label_1089;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1089:; 
goto label_1103;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_1103:; 
 __return_1110 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_1033;
}
tmp = __return_1110;
goto label_1113;
}
}
}
}
}
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
returnVal = 0;
goto label_996;
}
else 
{
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 1)
{
returnVal = -1073741823;
goto label_996;
}
else 
{
returnVal = 259;
label_996:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_1105;
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
goto label_1087;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1087:; 
goto label_1105;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_1105:; 
 __return_1109 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_1033;
}
tmp = __return_1109;
goto label_1113;
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
{
int __tmp_134 = devExt__TopOfStack;
int __tmp_135 = Irp;
int DeviceObject = __tmp_134;
int Irp = __tmp_135;
int compRetStatus ;
int returnVal ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
long __cil_tmp8 ;
if (compRegistered == 0)
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
returnVal = 0;
goto label_1241;
}
else 
{
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 1)
{
returnVal = -1073741823;
goto label_1241;
}
else 
{
returnVal = 259;
label_1241:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_1350;
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
goto label_1328;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1328:; 
goto label_1350;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_1350:; 
 __return_1351 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
label_1276:; 
 __return_1354 = returnVal;
}
tmp = __return_1351;
goto label_1113;
tmp = __return_1354;
goto label_1113;
}
}
}
}
}
else 
{
{
int __tmp_136 = DeviceObject;
int __tmp_137 = Irp;
int __tmp_138 = lcontext;
int DeviceObject = __tmp_136;
int Irp = __tmp_137;
int Context = __tmp_138;
int event ;
event = Context;
{
int __tmp_139 = event;
int __tmp_140 = 0;
int __tmp_141 = 0;
int Event = __tmp_139;
int Increment = __tmp_140;
int Wait = __tmp_141;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1142 = l;
}
 __return_1145 = -1073741802;
}
compRetStatus = __return_1145;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_1172;
label_1172:; 
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
returnVal = 0;
goto label_1237;
}
else 
{
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 1)
{
returnVal = -1073741823;
goto label_1237;
}
else 
{
returnVal = 259;
label_1237:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_1346;
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
goto label_1332;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1332:; 
goto label_1346;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_1346:; 
 __return_1353 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_1276;
}
tmp = __return_1353;
goto label_1113;
}
}
}
}
}
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
returnVal = 0;
goto label_1239;
}
else 
{
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 1)
{
returnVal = -1073741823;
goto label_1239;
}
else 
{
returnVal = 259;
label_1239:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_1348;
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
goto label_1330;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1330:; 
goto label_1348;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_1348:; 
 __return_1352 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_1276;
}
tmp = __return_1352;
goto label_1113;
}
}
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
if (tmp_ndt_5 == 8)
{
{
int __tmp_142 = devobj;
int __tmp_143 = pirp;
int DeviceObject = __tmp_142;
int Irp = __tmp_143;
int Irp__IoStatus__Information ;
int irpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int() ;
irpStack__Parameters__DeviceIoControl__IoControlCode = __VERIFIER_nondet_int();
int devExt__UpperConnectData__ClassService = __VERIFIER_nondet_int() ;
devExt__UpperConnectData__ClassService = __VERIFIER_nondet_int();
int irpStack__Parameters__DeviceIoControl__InputBufferLength = __VERIFIER_nondet_int() ;
irpStack__Parameters__DeviceIoControl__InputBufferLength = __VERIFIER_nondet_int();
int sizeof__CONNECT_DATA = __VERIFIER_nondet_int() ;
sizeof__CONNECT_DATA = __VERIFIER_nondet_int();
int irpStack__Parameters__DeviceIoControl__Type3InputBuffer = __VERIFIER_nondet_int() ;
irpStack__Parameters__DeviceIoControl__Type3InputBuffer = __VERIFIER_nondet_int();
int sizeof__INTERNAL_I8042_HOOK_KEYBOARD = __VERIFIER_nondet_int() ;
sizeof__INTERNAL_I8042_HOOK_KEYBOARD = __VERIFIER_nondet_int();
int hookKeyboard__InitializationRoutine = __VERIFIER_nondet_int() ;
hookKeyboard__InitializationRoutine = __VERIFIER_nondet_int();
int hookKeyboard__IsrRoutine = __VERIFIER_nondet_int() ;
hookKeyboard__IsrRoutine = __VERIFIER_nondet_int();
int Irp__IoStatus__Status ;
int hookKeyboard ;
int connectData ;
int status ;
int tmp ;
int __cil_tmp17 ;
int __cil_tmp18 ;
int __cil_tmp19 ;
int __cil_tmp20 = __VERIFIER_nondet_int() ;
__cil_tmp20 = __VERIFIER_nondet_int();
int __cil_tmp21 ;
int __cil_tmp22 ;
int __cil_tmp23 ;
int __cil_tmp24 = __VERIFIER_nondet_int() ;
__cil_tmp24 = __VERIFIER_nondet_int();
int __cil_tmp25 ;
int __cil_tmp26 ;
int __cil_tmp27 ;
int __cil_tmp28 = __VERIFIER_nondet_int() ;
__cil_tmp28 = __VERIFIER_nondet_int();
int __cil_tmp29 = __VERIFIER_nondet_int() ;
__cil_tmp29 = __VERIFIER_nondet_int();
int __cil_tmp30 ;
int __cil_tmp31 ;
int __cil_tmp32 = __VERIFIER_nondet_int() ;
__cil_tmp32 = __VERIFIER_nondet_int();
int __cil_tmp33 ;
int __cil_tmp34 ;
int __cil_tmp35 = __VERIFIER_nondet_int() ;
__cil_tmp35 = __VERIFIER_nondet_int();
int __cil_tmp36 ;
int __cil_tmp37 ;
int __cil_tmp38 = __VERIFIER_nondet_int() ;
__cil_tmp38 = __VERIFIER_nondet_int();
int __cil_tmp39 ;
int __cil_tmp40 ;
int __cil_tmp41 = __VERIFIER_nondet_int() ;
__cil_tmp41 = __VERIFIER_nondet_int();
int __cil_tmp42 ;
int __cil_tmp43 ;
int __cil_tmp44 = __VERIFIER_nondet_int() ;
__cil_tmp44 = __VERIFIER_nondet_int();
int __cil_tmp45 ;
status = 0;
Irp__IoStatus__Information = 0;
if (irpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp20)
{
if (devExt__UpperConnectData__ClassService != 0)
{
status = -1073741757;
goto label_256;
}
else 
{
if (irpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CONNECT_DATA)
{
status = -1073741811;
goto label_256;
}
else 
{
connectData = irpStack__Parameters__DeviceIoControl__Type3InputBuffer;
goto label_256;
}
}
}
else 
{
if (irpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp24)
{
status = -1073741822;
goto label_256;
}
else 
{
if (irpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp28)
{
if (irpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__INTERNAL_I8042_HOOK_KEYBOARD)
{
status = -1073741811;
label_256:; 
goto label_260;
}
else 
{
hookKeyboard = irpStack__Parameters__DeviceIoControl__Type3InputBuffer;
status = 0;
goto label_260;
}
}
else 
{
label_260:; 
if (status < 0)
{
Irp__IoStatus__Status = status;
myStatus = status;
{
int __tmp_144 = Irp;
int __tmp_145 = 0;
int Irp = __tmp_144;
int PriorityBoost = __tmp_145;
flag = s - NP;
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
goto label_805;
label_805:; 
 __return_808 = status;
}
status = __return_808;
label_810:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_5315;
}
else 
{
goto label_4999;
}
}
else 
{
label_4999:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_5315;
}
else 
{
goto label_5035;
}
}
else 
{
label_5035:; 
if (s != UNLOADED)
{
if (status != -1)
{
if (s != SKIP2)
{
if (s != IPC)
{
if (s == DC)
{
goto label_5161;
}
else 
{
goto label_5315;
}
}
else 
{
goto label_5161;
}
}
else 
{
label_5161:; 
if (pended == 1)
{
if (status != 259)
{
{
__VERIFIER_error();
}
goto label_5213;
}
else 
{
goto label_5315;
}
}
else 
{
goto label_5315;
}
}
}
else 
{
goto label_5315;
}
}
else 
{
label_5315:; 
 __return_5321 = status;
goto label_151;
}
}
}
}
else 
{
{
int __tmp_146 = DeviceObject;
int __tmp_147 = Irp;
int DeviceObject = __tmp_146;
int Irp = __tmp_147;
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int DeviceObject__DeviceExtension__TopOfStack = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension__TopOfStack = __VERIFIER_nondet_int();
int irpStack ;
int tmp ;
irpStack = Irp__Tail__Overlay__CurrentStackLocation;
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_148 = DeviceObject__DeviceExtension__TopOfStack;
int __tmp_149 = Irp;
int DeviceObject = __tmp_148;
int Irp = __tmp_149;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_421;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_421;
}
else 
{
returnVal2 = 259;
label_421:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_527;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_509;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_509:; 
goto label_527;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_527:; 
 __return_528 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_456:; 
 __return_531 = returnVal2;
}
tmp = __return_528;
goto label_533;
tmp = __return_531;
label_533:; 
 __return_779 = tmp;
}
tmp = __return_779;
 __return_782 = tmp;
}
status = __return_782;
goto label_810;
}
}
}
else 
{
{
int __tmp_150 = DeviceObject;
int __tmp_151 = Irp;
int __tmp_152 = lcontext;
int DeviceObject = __tmp_150;
int Irp = __tmp_151;
int Context = __tmp_152;
int event ;
event = Context;
{
int __tmp_153 = event;
int __tmp_154 = 0;
int __tmp_155 = 0;
int Event = __tmp_153;
int Increment = __tmp_154;
int Wait = __tmp_155;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_322 = l;
}
 __return_325 = -1073741802;
}
compRetStatus = __return_325;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_352;
label_352:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_417;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_417;
}
else 
{
returnVal2 = 259;
label_417:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_523;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_505;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_505:; 
goto label_523;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_523:; 
 __return_530 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_456;
}
tmp = __return_530;
goto label_533;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_419;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_419;
}
else 
{
returnVal2 = 259;
label_419:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_525;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_507;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_507:; 
goto label_525;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_525:; 
 __return_529 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_456;
}
tmp = __return_529;
goto label_533;
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
{
int __tmp_156 = DeviceObject__DeviceExtension__TopOfStack;
int __tmp_157 = Irp;
int DeviceObject = __tmp_156;
int Irp = __tmp_157;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_660;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_660;
}
else 
{
returnVal2 = 259;
label_660:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_766;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_748;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_748:; 
goto label_766;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_766:; 
 __return_767 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_695:; 
 __return_770 = returnVal2;
}
tmp = __return_767;
goto label_533;
tmp = __return_770;
goto label_533;
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
int __tmp_160 = lcontext;
int DeviceObject = __tmp_158;
int Irp = __tmp_159;
int Context = __tmp_160;
int event ;
event = Context;
{
int __tmp_161 = event;
int __tmp_162 = 0;
int __tmp_163 = 0;
int Event = __tmp_161;
int Increment = __tmp_162;
int Wait = __tmp_163;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_561 = l;
}
 __return_564 = -1073741802;
}
compRetStatus = __return_564;
__cil_tmp7 = (long long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
flag = s - NP;
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
goto label_591;
label_591:; 
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_656;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_656;
}
else 
{
returnVal2 = 259;
label_656:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_762;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_744;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_744:; 
goto label_762;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_762:; 
 __return_769 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_695;
}
tmp = __return_769;
goto label_533;
}
}
}
}
}
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_658;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_658;
}
else 
{
returnVal2 = 259;
label_658:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_764;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_746;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_746:; 
goto label_764;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_764:; 
 __return_768 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_695;
}
tmp = __return_768;
goto label_533;
}
}
}
}
}
}
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
 __return_151 = -1;
label_151:; 
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
goto label_5317;
}
else 
{
goto label_5001;
}
}
else 
{
label_5001:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_5317;
}
else 
{
goto label_5033;
}
}
else 
{
label_5033:; 
if (s != UNLOADED)
{
if (status != -1)
{
if (s != SKIP2)
{
if (s != IPC)
{
if (s == DC)
{
goto label_5159;
}
else 
{
goto label_5317;
}
}
else 
{
goto label_5159;
}
}
else 
{
label_5159:; 
if (pended == 1)
{
if (status != 259)
{
{
__VERIFIER_error();
}
 __return_5331 = status;
goto label_5319;
}
else 
{
goto label_5317;
}
}
else 
{
goto label_5317;
}
}
}
else 
{
goto label_5317;
}
}
else 
{
label_5317:; 
 __return_5319 = status;
label_5319:; 
return 1;
}
}
}
}
}
