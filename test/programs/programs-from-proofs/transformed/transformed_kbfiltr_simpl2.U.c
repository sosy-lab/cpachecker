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
int __return_5337=0;
int __return_4694=0;
int __return_4697=0;
int __return_4945=0;
int __return_4948=0;
int __return_5335=0;
int __return_5331=0;
int __return_4488=0;
int __return_4491=0;
int __return_4696=0;
int __return_4695=0;
int __return_4933=0;
int __return_4936=0;
int __return_4727=0;
int __return_4730=0;
int __return_4935=0;
int __return_4934=0;
int __return_4143=0;
int __return_4146=0;
int __return_4394=0;
int __return_4397=0;
int __return_5329=0;
int __return_3937=0;
int __return_3940=0;
int __return_4145=0;
int __return_4144=0;
int __return_4382=0;
int __return_4385=0;
int __return_4176=0;
int __return_4179=0;
int __return_4384=0;
int __return_4383=0;
int __return_3678=0;
int __return_3681=0;
int __return_3472=0;
int __return_3475=0;
int __return_3680=0;
int __return_3679=0;
int __return_3439=0;
int __return_3442=0;
int __return_3233=0;
int __return_3236=0;
int __return_3441=0;
int __return_3440=0;
int __return_3200=0;
int __return_3203=0;
int __return_3745=0;
int __return_3746=0;
int __return_3747=0;
int __return_3748=0;
int __return_3837=0;
int __return_2994=0;
int __return_2997=0;
int __return_3202=0;
int __return_3201=0;
int __return_2681=0;
int __return_2684=0;
int __return_3838=0;
int __return_2475=0;
int __return_2478=0;
int __return_2683=0;
int __return_2682=0;
int __return_2920=0;
int __return_2923=0;
int __return_2714=0;
int __return_2717=0;
int __return_2922=0;
int __return_2921=0;
int __return_2172=0;
int __return_2175=0;
int __return_3839=0;
int __return_1966=0;
int __return_1969=0;
int __return_2174=0;
int __return_2173=0;
int __return_2411=0;
int __return_2414=0;
int __return_2205=0;
int __return_2208=0;
int __return_2413=0;
int __return_2412=0;
int __return_1664=0;
int __return_1667=0;
int __return_3840=0;
int __return_5327=0;
int __return_1458=0;
int __return_1461=0;
int __return_1666=0;
int __return_1665=0;
int __return_1903=0;
int __return_1906=0;
int __return_1697=0;
int __return_1700=0;
int __return_1905=0;
int __return_1904=0;
int __return_1110=0;
int __return_1113=0;
int __return_1365=0;
int __return_5325=0;
int __return_901=0;
int __return_904=0;
int __return_1112=0;
int __return_1111=0;
int __return_1353=0;
int __return_1356=0;
int __return_1144=0;
int __return_1147=0;
int __return_1355=0;
int __return_1354=0;
int __return_810=0;
int __return_5323=0;
int __return_530=0;
int __return_533=0;
int __return_781=0;
int __return_784=0;
int __return_324=0;
int __return_327=0;
int __return_532=0;
int __return_531=0;
int __return_769=0;
int __return_772=0;
int __return_563=0;
int __return_566=0;
int __return_771=0;
int __return_770=0;
int __return_153=0;
int __return_5333=0;
int __return_5321=0;
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
goto label_114;
}
else 
{
label_114:; 
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
 __return_5337 = -1;
goto label_153;
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
goto label_4423;
}
else 
{
label_4423:; 
goto label_4426;
}
}
else 
{
label_4426:; 
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
goto label_4587;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4587;
}
else 
{
returnVal2 = 259;
label_4587:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4693;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4675;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4675:; 
goto label_4693;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4693:; 
 __return_4694 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4622:; 
 __return_4697 = returnVal2;
}
tmp = __return_4694;
goto label_4699;
tmp = __return_4697;
label_4699:; 
 __return_4945 = tmp;
}
tmp = __return_4945;
 __return_4948 = tmp;
}
status = __return_4948;
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
goto label_5045;
}
}
else 
{
label_5045:; 
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
goto label_5171;
}
else 
{
goto label_5309;
}
}
else 
{
goto label_5171;
}
}
else 
{
label_5171:; 
if (pended == 1)
{
if (status != 259)
{
{
__VERIFIER_error();
}
label_5215:; 
 __return_5335 = status;
goto label_153;
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
 __return_5331 = status;
goto label_153;
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
 __return_4488 = l;
}
 __return_4491 = -1073741802;
}
compRetStatus = __return_4491;
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
goto label_4518;
label_4518:; 
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
 __return_4696 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4622;
}
tmp = __return_4696;
goto label_4699;
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
 __return_4695 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4622;
}
tmp = __return_4695;
goto label_4699;
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
goto label_4826;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4826;
}
else 
{
returnVal2 = 259;
label_4826:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4932;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4914;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4914:; 
goto label_4932;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4932:; 
 __return_4933 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4861:; 
 __return_4936 = returnVal2;
}
tmp = __return_4933;
goto label_4699;
tmp = __return_4936;
goto label_4699;
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
 __return_4727 = l;
}
 __return_4730 = -1073741802;
}
compRetStatus = __return_4730;
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
goto label_4757;
label_4757:; 
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
 __return_4935 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4861;
}
tmp = __return_4935;
goto label_4699;
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
 __return_4934 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4861;
}
tmp = __return_4934;
goto label_4699;
}
}
}
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
goto label_3872;
}
else 
{
label_3872:; 
goto label_3875;
}
}
else 
{
label_3875:; 
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
goto label_4036;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4036;
}
else 
{
returnVal2 = 259;
label_4036:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4142;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4124;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4124:; 
goto label_4142;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4142:; 
 __return_4143 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4071:; 
 __return_4146 = returnVal2;
}
tmp = __return_4143;
goto label_4148;
tmp = __return_4146;
label_4148:; 
 __return_4394 = tmp;
}
tmp = __return_4394;
 __return_4397 = tmp;
}
status = __return_4397;
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
goto label_5311;
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
goto label_5215;
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
 __return_5329 = status;
goto label_153;
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
 __return_3937 = l;
}
 __return_3940 = -1073741802;
}
compRetStatus = __return_3940;
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
goto label_3967;
label_3967:; 
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
 __return_4145 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4071;
}
tmp = __return_4145;
goto label_4148;
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
 __return_4144 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4071;
}
tmp = __return_4144;
goto label_4148;
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
goto label_4275;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_4275;
}
else 
{
returnVal2 = 259;
label_4275:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4381;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4363;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4363:; 
goto label_4381;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4381:; 
 __return_4382 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4310:; 
 __return_4385 = returnVal2;
}
tmp = __return_4382;
goto label_4148;
tmp = __return_4385;
goto label_4148;
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
 __return_4176 = l;
}
 __return_4179 = -1073741802;
}
compRetStatus = __return_4179;
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
goto label_4206;
label_4206:; 
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
 __return_4384 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4310;
}
tmp = __return_4384;
goto label_4148;
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
 __return_4383 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4310;
}
tmp = __return_4383;
goto label_4148;
}
}
}
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
goto label_3571;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3571;
}
else 
{
returnVal2 = 259;
label_3571:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3677;
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
goto label_3677;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3677:; 
 __return_3678 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3606:; 
 __return_3681 = returnVal2;
}
status = __return_3678;
goto label_3205;
status = __return_3681;
goto label_3205;
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
 __return_3472 = l;
}
 __return_3475 = -1073741802;
}
compRetStatus = __return_3475;
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
goto label_3502;
label_3502:; 
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
 __return_3680 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3606;
}
status = __return_3680;
goto label_3205;
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
 __return_3679 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3606;
}
status = __return_3679;
goto label_3205;
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
goto label_3332;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3332;
}
else 
{
returnVal2 = 259;
label_3332:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3438;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3420;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3420:; 
goto label_3438;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3438:; 
 __return_3439 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3367:; 
 __return_3442 = returnVal2;
}
status = __return_3439;
goto label_3205;
status = __return_3442;
goto label_3205;
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
 __return_3233 = l;
}
 __return_3236 = -1073741802;
}
compRetStatus = __return_3236;
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
goto label_3263;
label_3263:; 
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
 __return_3441 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3367;
}
status = __return_3441;
goto label_3205;
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
 __return_3440 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3367;
}
status = __return_3440;
goto label_3205;
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
goto label_3093;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_3093;
}
else 
{
returnVal2 = 259;
label_3093:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3199;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3181;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3181:; 
goto label_3199;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3199:; 
 __return_3200 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3128:; 
 __return_3203 = returnVal2;
}
status = __return_3200;
goto label_3205;
status = __return_3203;
label_3205:; 
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
goto label_3726;
}
else 
{
goto label_3702;
}
}
else 
{
label_3702:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_3726;
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
 __return_3745 = 0;
goto label_3746;
}
else 
{
 __return_3746 = -1073741823;
label_3746:; 
}
goto label_3750;
}
else 
{
label_3726:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
 __return_3747 = 0;
goto label_3748;
}
else 
{
 __return_3748 = -1073741823;
label_3748:; 
}
label_3750:; 
if (status >= 0)
{
if (myStatus >= 0)
{
devExt__Started = 1;
devExt__Removed = 0;
devExt__SurpriseRemoved = 0;
goto label_3775;
}
else 
{
goto label_3775;
}
}
else 
{
label_3775:; 
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
goto label_3802;
goto label_3802;
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
goto label_3802;
label_3802:; 
 __return_3837 = status;
}
status = __return_3837;
goto label_3842;
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
 __return_2994 = l;
}
 __return_2997 = -1073741802;
}
compRetStatus = __return_2997;
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
goto label_3024;
label_3024:; 
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
 __return_3202 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3128;
}
status = __return_3202;
goto label_3205;
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
 __return_3201 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3128;
}
status = __return_3201;
goto label_3205;
}
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
goto label_2574;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2574;
}
else 
{
returnVal2 = 259;
label_2574:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2680;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2662;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2662:; 
goto label_2680;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2680:; 
 __return_2681 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2609:; 
 __return_2684 = returnVal2;
}
status = __return_2681;
goto label_2686;
status = __return_2684;
label_2686:; 
 __return_3838 = status;
}
status = __return_3838;
goto label_3842;
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
 __return_2475 = l;
}
 __return_2478 = -1073741802;
}
compRetStatus = __return_2478;
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
goto label_2505;
label_2505:; 
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
 __return_2683 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2609;
}
status = __return_2683;
goto label_2686;
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
 __return_2682 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2609;
}
status = __return_2682;
goto label_2686;
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
goto label_2813;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2813;
}
else 
{
returnVal2 = 259;
label_2813:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2919;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2901;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2901:; 
goto label_2919;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2919:; 
 __return_2920 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2848:; 
 __return_2923 = returnVal2;
}
status = __return_2920;
goto label_2686;
status = __return_2923;
goto label_2686;
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
 __return_2714 = l;
}
 __return_2717 = -1073741802;
}
compRetStatus = __return_2717;
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
goto label_2744;
label_2744:; 
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
 __return_2922 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2848;
}
status = __return_2922;
goto label_2686;
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
 __return_2921 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2848;
}
status = __return_2921;
goto label_2686;
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
goto label_2065;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2065;
}
else 
{
returnVal2 = 259;
label_2065:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2171;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2153;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2153:; 
goto label_2171;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2171:; 
 __return_2172 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2100:; 
 __return_2175 = returnVal2;
}
goto label_2177;
label_2177:; 
status = 0;
 __return_3839 = status;
}
status = __return_3839;
goto label_3842;
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
 __return_1966 = l;
}
 __return_1969 = -1073741802;
}
compRetStatus = __return_1969;
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
goto label_1996;
label_1996:; 
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
 __return_2174 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2100;
}
goto label_2177;
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
 __return_2173 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2100;
}
goto label_2177;
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
goto label_2304;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_2304;
}
else 
{
returnVal2 = 259;
label_2304:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2410;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2392;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2392:; 
goto label_2410;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2410:; 
 __return_2411 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2339:; 
 __return_2414 = returnVal2;
}
goto label_2177;
goto label_2177;
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
 __return_2205 = l;
}
 __return_2208 = -1073741802;
}
compRetStatus = __return_2208;
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
goto label_2235;
label_2235:; 
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
 __return_2413 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2339;
}
goto label_2177;
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
 __return_2412 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2339;
}
goto label_2177;
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
goto label_1557;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_1557;
}
else 
{
returnVal2 = 259;
label_1557:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1663;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1645;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1645:; 
goto label_1663;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1663:; 
 __return_1664 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1592:; 
 __return_1667 = returnVal2;
}
status = __return_1664;
goto label_1669;
status = __return_1667;
label_1669:; 
 __return_3840 = status;
}
status = __return_3840;
label_3842:; 
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
goto label_5313;
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
goto label_5215;
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
 __return_5327 = status;
goto label_153;
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
 __return_1458 = l;
}
 __return_1461 = -1073741802;
}
compRetStatus = __return_1461;
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
goto label_1488;
label_1488:; 
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
 __return_1666 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1592;
}
status = __return_1666;
goto label_1669;
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
 __return_1665 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1592;
}
status = __return_1665;
goto label_1669;
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
goto label_1796;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_1796;
}
else 
{
returnVal2 = 259;
label_1796:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1902;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1884;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1884:; 
goto label_1902;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1902:; 
 __return_1903 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1831:; 
 __return_1906 = returnVal2;
}
status = __return_1903;
goto label_1669;
status = __return_1906;
goto label_1669;
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
 __return_1697 = l;
}
 __return_1700 = -1073741802;
}
compRetStatus = __return_1700;
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
goto label_1727;
label_1727:; 
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
 __return_1905 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1831;
}
status = __return_1905;
goto label_1669;
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
 __return_1904 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1831;
}
status = __return_1904;
goto label_1669;
}
}
}
}
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
goto label_844;
}
else 
{
label_844:; 
goto label_852;
}
}
else 
{
label_852:; 
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
goto label_1000;
}
else 
{
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 1)
{
returnVal = -1073741823;
goto label_1000;
}
else 
{
returnVal = 259;
label_1000:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_1109;
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
goto label_1109;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_1109:; 
 __return_1110 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
label_1035:; 
 __return_1113 = returnVal;
}
tmp = __return_1110;
goto label_1115;
tmp = __return_1113;
label_1115:; 
 __return_1365 = tmp;
}
status = __return_1365;
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
goto label_5315;
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
goto label_5215;
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
 __return_5325 = status;
goto label_153;
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
 __return_901 = l;
}
 __return_904 = -1073741802;
}
compRetStatus = __return_904;
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
goto label_931;
label_931:; 
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
goto label_1091;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1091:; 
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
 __return_1112 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_1035;
}
tmp = __return_1112;
goto label_1115;
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
goto label_1089;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1089:; 
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
 __return_1111 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_1035;
}
tmp = __return_1111;
goto label_1115;
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
goto label_1243;
}
else 
{
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 1)
{
returnVal = -1073741823;
goto label_1243;
}
else 
{
returnVal = 259;
label_1243:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_1352;
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
goto label_1352;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_1352:; 
 __return_1353 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
label_1278:; 
 __return_1356 = returnVal;
}
tmp = __return_1353;
goto label_1115;
tmp = __return_1356;
goto label_1115;
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
 __return_1144 = l;
}
 __return_1147 = -1073741802;
}
compRetStatus = __return_1147;
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
goto label_1174;
label_1174:; 
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
goto label_1334;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1334:; 
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
 __return_1355 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_1278;
}
tmp = __return_1355;
goto label_1115;
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
goto label_1332;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_1332:; 
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
 __return_1354 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_1278;
}
tmp = __return_1354;
goto label_1115;
}
}
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
goto label_258;
}
else 
{
if (irpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CONNECT_DATA)
{
status = -1073741811;
goto label_258;
}
else 
{
connectData = irpStack__Parameters__DeviceIoControl__Type3InputBuffer;
goto label_258;
}
}
}
else 
{
if (irpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp24)
{
status = -1073741822;
goto label_258;
}
else 
{
if (irpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp28)
{
if (irpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__INTERNAL_I8042_HOOK_KEYBOARD)
{
status = -1073741811;
label_258:; 
goto label_262;
}
else 
{
hookKeyboard = irpStack__Parameters__DeviceIoControl__Type3InputBuffer;
status = 0;
goto label_262;
}
}
else 
{
label_262:; 
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
goto label_807;
label_807:; 
 __return_810 = status;
}
status = __return_810;
label_812:; 
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
goto label_5317;
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
goto label_5215;
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
 __return_5323 = status;
goto label_153;
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
goto label_423;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_423;
}
else 
{
returnVal2 = 259;
label_423:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_529;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_511;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_511:; 
goto label_529;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_529:; 
 __return_530 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_458:; 
 __return_533 = returnVal2;
}
tmp = __return_530;
goto label_535;
tmp = __return_533;
label_535:; 
 __return_781 = tmp;
}
tmp = __return_781;
 __return_784 = tmp;
}
status = __return_784;
goto label_812;
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
 __return_324 = l;
}
 __return_327 = -1073741802;
}
compRetStatus = __return_327;
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
goto label_354;
label_354:; 
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
 __return_532 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_458;
}
tmp = __return_532;
goto label_535;
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
 __return_531 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_458;
}
tmp = __return_531;
goto label_535;
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
goto label_662;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
goto label_662;
}
else 
{
returnVal2 = 259;
label_662:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_768;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_750;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_750:; 
goto label_768;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_768:; 
 __return_769 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_697:; 
 __return_772 = returnVal2;
}
tmp = __return_769;
goto label_535;
tmp = __return_772;
goto label_535;
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
 __return_563 = l;
}
 __return_566 = -1073741802;
}
compRetStatus = __return_566;
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
goto label_593;
label_593:; 
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
 __return_771 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_697;
}
tmp = __return_771;
goto label_535;
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
 __return_770 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_697;
}
tmp = __return_770;
goto label_535;
}
}
}
}
}
}
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
 __return_153 = -1;
label_153:; 
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
goto label_5319;
}
else 
{
goto label_5003;
}
}
else 
{
label_5003:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_5319;
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
goto label_5319;
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
 __return_5333 = status;
goto label_5321;
}
else 
{
goto label_5319;
}
}
else 
{
goto label_5319;
}
}
}
else 
{
goto label_5319;
}
}
else 
{
label_5319:; 
 __return_5321 = status;
label_5321:; 
return 1;
}
}
}
}
}
