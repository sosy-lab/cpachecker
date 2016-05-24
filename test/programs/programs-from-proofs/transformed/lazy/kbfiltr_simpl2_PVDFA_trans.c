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
int __return_2843;
int __return_2848;
int __return_2851;
int __return_2842;
int __return_2720;
int __return_2725;
int __return_2728;
int __return_2719;
int __return_2462;
int __return_2465;
int __return_2533;
int __return_2534;
int __return_2567;
int __return_2568;
int __return_2419;
int __return_2418;
int __return_2341;
int __return_2340;
int __return_2265;
int __return_2604;
int __return_2264;
int __return_2148;
int __return_2151;
int __return_2046;
int __return_2023;
int __return_2028;
int __return_2047;
int __return_2895;
int __return_2022;
int __return_2896;
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
goto label_1792;
}
else 
{
label_1792:; 
{
s = NP;
pended = 0;
compFptr = 0;
compRegistered = 0;
lowerDriverReturn = 0;
setEventCalled = 0;
customIrp = 0;
}
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
goto label_2758;
}
else 
{
label_2758:; 
goto label_2762;
}
}
else 
{
label_2762:; 
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
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2821;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
label_2821:; 
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2843 = returnVal2;
}
else 
{
returnVal2 = 259;
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2842 = returnVal2;
}
tmp = __return_2843;
label_2845:; 
 __return_2848 = tmp;
tmp = __return_2842;
goto label_2845;
}
tmp = __return_2848;
 __return_2851 = tmp;
}
status = __return_2851;
goto label_2856;
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
int __tmp_7 = devobj;
int __tmp_8 = pirp;
int DeviceObject = __tmp_7;
int Irp = __tmp_8;
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
goto label_2635;
}
else 
{
label_2635:; 
goto label_2639;
}
}
else 
{
label_2639:; 
Irp__IoStatus__Status = status;
myStatus = status;
{
int __tmp_9 = DeviceObject;
int __tmp_10 = Irp;
int DeviceObject = __tmp_9;
int Irp = __tmp_10;
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
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_11 = DeviceObject__DeviceExtension__TopOfStack;
int __tmp_12 = Irp;
int DeviceObject = __tmp_11;
int Irp = __tmp_12;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2698;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
label_2698:; 
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2720 = returnVal2;
}
else 
{
returnVal2 = 259;
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2719 = returnVal2;
}
tmp = __return_2720;
label_2722:; 
 __return_2725 = tmp;
tmp = __return_2719;
goto label_2722;
}
tmp = __return_2725;
 __return_2728 = tmp;
}
status = __return_2728;
goto label_2856;
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
int __tmp_13 = devobj;
int __tmp_14 = pirp;
int DeviceObject = __tmp_13;
int Irp = __tmp_14;
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
flag = compRegistered;
compRegistered = 1;
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = event;
irpSp__Control = 224;
{
int __tmp_15 = devExt__TopOfStack;
int __tmp_16 = Irp;
int DeviceObject = __tmp_15;
int Irp = __tmp_16;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
{
int __tmp_17 = DeviceObject;
int __tmp_18 = Irp;
int __tmp_19 = lcontext;
int DeviceObject = __tmp_17;
int Irp = __tmp_18;
int Context = __tmp_19;
int event ;
event = Context;
{
int __tmp_20 = event;
int __tmp_21 = 0;
int __tmp_22 = 0;
int Event = __tmp_20;
int Increment = __tmp_21;
int Wait = __tmp_22;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2462 = l;
}
 __return_2465 = -1073741802;
}
compRetStatus = __return_2465;
__cil_tmp7 = (long long)compRetStatus;
{
flag = s - NP;
s = MPR1;
}
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2506;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
label_2506:; 
s = NP;
lowerDriverReturn = returnVal2;
 __return_2533 = returnVal2;
}
else 
{
returnVal2 = 259;
s = MPR3;
lowerDriverReturn = returnVal2;
 __return_2534 = returnVal2;
}
status = __return_2533;
__cil_tmp23 = (long)status;
label_2573:; 
if (status >= 0)
{
if (myStatus >= 0)
{
devExt__Started = 1;
devExt__Removed = 0;
devExt__SurpriseRemoved = 0;
goto label_2576;
}
else 
{
goto label_2576;
}
}
else 
{
label_2576:; 
Irp__IoStatus__Status = status;
myStatus = status;
Irp__IoStatus__Information = 0;
{
int __tmp_23 = Irp;
int __tmp_24 = 0;
int Irp = __tmp_23;
int PriorityBoost = __tmp_24;
s = DC;
}
goto label_2601;
}
status = __return_2534;
__cil_tmp23 = (long)status;
{
int __tmp_25 = event;
int __tmp_26 = Executive;
int __tmp_27 = KernelMode;
int __tmp_28 = 0;
int __tmp_29 = 0;
int Object = __tmp_25;
int WaitReason = __tmp_26;
int WaitMode = __tmp_27;
int Alertable = __tmp_28;
int Timeout = __tmp_29;
s = NP;
setEventCalled = 0;
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
 __return_2567 = 0;
goto label_2568;
}
else 
{
 __return_2568 = -1073741823;
label_2568:; 
}
goto label_2573;
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
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_30 = devExt__TopOfStack;
int __tmp_31 = Irp;
int DeviceObject = __tmp_30;
int Irp = __tmp_31;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2397;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
label_2397:; 
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2419 = returnVal2;
}
else 
{
returnVal2 = 259;
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2418 = returnVal2;
}
status = __return_2419;
label_2421:; 
goto label_2601;
status = __return_2418;
goto label_2421;
}
}
}
else 
{
if (irpStack__MinorFunction == 2)
{
devExt__Removed = 1;
flag = s - NP;
s = SKIP1;
int __CPAchecker_TMP_2 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_2;
int __CPAchecker_TMP_3 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_3;
{
int __tmp_32 = devExt__TopOfStack;
int __tmp_33 = Irp;
int DeviceObject = __tmp_32;
int Irp = __tmp_33;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2319;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
label_2319:; 
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2341 = returnVal2;
}
else 
{
returnVal2 = 259;
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2340 = returnVal2;
}
label_2343:; 
status = 0;
goto label_2601;
goto label_2343;
}
}
}
else 
{
flag = s - NP;
s = SKIP1;
int __CPAchecker_TMP_4 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_4;
int __CPAchecker_TMP_5 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_5;
{
int __tmp_34 = devExt__TopOfStack;
int __tmp_35 = Irp;
int DeviceObject = __tmp_34;
int Irp = __tmp_35;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2243;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
label_2243:; 
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2265 = returnVal2;
}
else 
{
returnVal2 = 259;
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2264 = returnVal2;
}
status = __return_2265;
label_2267:; 
label_2601:; 
 __return_2604 = status;
status = __return_2264;
goto label_2267;
}
status = __return_2604;
goto label_2856;
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
int __tmp_36 = devobj;
int __tmp_37 = pirp;
int DeviceObject = __tmp_36;
int Irp = __tmp_37;
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
goto label_2079;
}
else 
{
label_2079:; 
goto label_2074;
}
}
else 
{
label_2074:; 
flag = s - NP;
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_38 = devExt__TopOfStack;
int __tmp_39 = Irp;
int DeviceObject = __tmp_38;
int Irp = __tmp_39;
int compRetStatus ;
int returnVal ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
long __cil_tmp8 ;
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
returnVal = 0;
goto label_2137;
}
else 
{
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 1)
{
returnVal = -1073741823;
goto label_2137;
}
else 
{
returnVal = 259;
label_2137:; 
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal;
 __return_2148 = returnVal;
}
tmp = __return_2148;
 __return_2151 = tmp;
}
status = __return_2151;
goto label_2856;
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
int __tmp_40 = devobj;
int __tmp_41 = pirp;
int DeviceObject = __tmp_40;
int Irp = __tmp_41;
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
goto label_1918;
}
else 
{
if (irpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CONNECT_DATA)
{
status = -1073741811;
goto label_1918;
}
else 
{
connectData = irpStack__Parameters__DeviceIoControl__Type3InputBuffer;
goto label_1918;
}
}
}
else 
{
if (irpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp24)
{
status = -1073741822;
goto label_1918;
}
else 
{
if (irpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp28)
{
if (irpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__INTERNAL_I8042_HOOK_KEYBOARD)
{
status = -1073741811;
goto label_1918;
}
else 
{
hookKeyboard = irpStack__Parameters__DeviceIoControl__Type3InputBuffer;
status = 0;
label_1918:; 
goto label_1942;
}
}
else 
{
label_1942:; 
if (status < 0)
{
Irp__IoStatus__Status = status;
myStatus = status;
{
int __tmp_42 = Irp;
int __tmp_43 = 0;
int Irp = __tmp_42;
int PriorityBoost = __tmp_43;
s = DC;
}
 __return_2046 = status;
goto label_2047;
}
else 
{
{
int __tmp_44 = DeviceObject;
int __tmp_45 = Irp;
int DeviceObject = __tmp_44;
int Irp = __tmp_45;
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
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_46 = DeviceObject__DeviceExtension__TopOfStack;
int __tmp_47 = Irp;
int DeviceObject = __tmp_46;
int Irp = __tmp_47;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
returnVal2 = 0;
goto label_2001;
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 1)
{
returnVal2 = -1073741823;
label_2001:; 
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2023 = returnVal2;
}
else 
{
returnVal2 = 259;
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2022 = returnVal2;
}
tmp = __return_2023;
label_2025:; 
 __return_2028 = tmp;
tmp = __return_2022;
goto label_2025;
}
tmp = __return_2028;
 __return_2047 = tmp;
label_2047:; 
}
status = __return_2047;
label_2856:; 
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
goto label_2883;
}
else 
{
goto label_2874;
}
}
else 
{
goto label_2883;
}
}
else 
{
label_2883:; 
goto label_2874;
}
}
else 
{
goto label_2874;
}
}
else 
{
label_2874:; 
 __return_2895 = status;
goto label_2896;
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
 __return_2896 = -1;
label_2896:; 
return 1;
}
}
}
}
}
}
}
