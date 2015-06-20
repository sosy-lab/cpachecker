extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern char __VERIFIER_nondet_char(void);
extern int __VERIFIER_nondet_int(void);
extern long __VERIFIER_nondet_long(void);
extern void *__VERIFIER_nondet_pointer(void);
int KernelMode  ;
int Executive  ;
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
int __return_2511;
int __return_2515;
int __return_2621;
int __return_2623;
int __return_2674;
int __return_2675;
int __return_2442;
int __return_2440;
int __return_2310;
int __return_2308;
int __return_2181;
int __return_2727;
int __return_2785;
int __return_2179;
int __return_2786;
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
goto label_1938;
}
else 
{
label_1938:; 
{
s = NP;
pended = 0;
compFptr = 0;
compRegistered = 0;
lowerDriverReturn = 0;
setEventCalled = 0;
customIrp = 0;
}
__cil_tmp8 = 1;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 3)
{
{
int __tmp_1 = devobj;
int __tmp_2 = pirp;
int DeviceObject = __tmp_1;
int Irp = __tmp_2;
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
irpSp__Control = 224;
{
int __tmp_3 = devExt__TopOfStack;
int __tmp_4 = Irp;
int DeviceObject = __tmp_3;
int Irp = __tmp_4;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
{
int __tmp_5 = DeviceObject;
int __tmp_6 = Irp;
int __tmp_7 = lcontext;
int DeviceObject = __tmp_5;
int Irp = __tmp_6;
int Context = __tmp_7;
int event ;
event = Context;
{
int __tmp_8 = event;
int __tmp_9 = 0;
int __tmp_10 = 0;
int Event = __tmp_8;
int Increment = __tmp_9;
int Wait = __tmp_10;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2511 = l;
}
 __return_2515 = -1073741802;
}
compRetStatus = __return_2515;
{
flag = s - NP;
s = MPR1;
}
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_2576;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
label_2576:; 
s = NP;
lowerDriverReturn = returnVal2;
 __return_2621 = returnVal2;
}
else 
{
returnVal2 = 259;
s = MPR3;
lowerDriverReturn = returnVal2;
 __return_2623 = returnVal2;
}
status = __return_2621;
__cil_tmp23 = (long)status;
label_2680:; 
if (status >= 0)
{
if (myStatus >= 0)
{
devExt__Started = 1;
devExt__Removed = 0;
devExt__SurpriseRemoved = 0;
goto label_2684;
}
else 
{
goto label_2684;
}
}
else 
{
label_2684:; 
Irp__IoStatus__Status = status;
myStatus = status;
Irp__IoStatus__Information = 0;
{
int __tmp_11 = Irp;
int __tmp_12 = 0;
int Irp = __tmp_11;
int PriorityBoost = __tmp_12;
s = DC;
}
goto label_2721;
}
status = __return_2623;
__cil_tmp23 = (long)status;
{
int __tmp_13 = event;
int __tmp_14 = Executive;
int __tmp_15 = KernelMode;
int __tmp_16 = 0;
int __tmp_17 = 0;
int Object = __tmp_13;
int WaitReason = __tmp_14;
int WaitMode = __tmp_15;
int Alertable = __tmp_16;
int Timeout = __tmp_17;
s = NP;
setEventCalled = 0;
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
 __return_2674 = 0;
goto label_2675;
}
else 
{
 __return_2675 = -1073741823;
label_2675:; 
}
goto label_2680;
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
int __tmp_18 = devExt__TopOfStack;
int __tmp_19 = Irp;
int DeviceObject = __tmp_18;
int Irp = __tmp_19;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_2402;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
label_2402:; 
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2442 = returnVal2;
}
else 
{
returnVal2 = 259;
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2440 = returnVal2;
}
status = __return_2442;
label_2444:; 
goto label_2721;
status = __return_2440;
goto label_2444;
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
int __tmp_20 = devExt__TopOfStack;
int __tmp_21 = Irp;
int DeviceObject = __tmp_20;
int Irp = __tmp_21;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_2270;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
label_2270:; 
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2310 = returnVal2;
}
else 
{
returnVal2 = 259;
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2308 = returnVal2;
}
label_2312:; 
status = 0;
goto label_2721;
goto label_2312;
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
int __tmp_22 = devExt__TopOfStack;
int __tmp_23 = Irp;
int DeviceObject = __tmp_22;
int Irp = __tmp_23;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_2141;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
label_2141:; 
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2181 = returnVal2;
}
else 
{
returnVal2 = 259;
flag = s - SKIP1;
s = SKIP2;
lowerDriverReturn = returnVal2;
 __return_2179 = returnVal2;
}
status = __return_2181;
label_2183:; 
label_2721:; 
 __return_2727 = status;
status = __return_2179;
goto label_2183;
}
status = __return_2727;
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
goto label_2769;
}
else 
{
goto label_2756;
}
}
else 
{
goto label_2769;
}
}
else 
{
label_2769:; 
goto label_2756;
}
}
else 
{
goto label_2756;
}
}
else 
{
label_2756:; 
 __return_2785 = status;
goto label_2786;
}
}
}
}
}
}
}
else 
{
 __return_2786 = -1;
label_2786:; 
return 1;
}
}
}
