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
int __return_2171=0;
int __return_1519=0;
int __return_1521=0;
int __return_1933=0;
int __return_1934=0;
int __return_1935=0;
int __return_1936=0;
int __return_2025=0;
int __return_1374=0;
int __return_1377=0;
int __return_1520=0;
int __return_1694=0;
int __return_1696=0;
int __return_1549=0;
int __return_1552=0;
int __return_1695=0;
int __return_1869=0;
int __return_1871=0;
int __return_1724=0;
int __return_1727=0;
int __return_1870=0;
int __return_1131=0;
int __return_1133=0;
int __return_2026=0;
int __return_986=0;
int __return_989=0;
int __return_1132=0;
int __return_1306=0;
int __return_1308=0;
int __return_1161=0;
int __return_1164=0;
int __return_1307=0;
int __return_750=0;
int __return_752=0;
int __return_2027=0;
int __return_605=0;
int __return_608=0;
int __return_751=0;
int __return_925=0;
int __return_927=0;
int __return_780=0;
int __return_783=0;
int __return_926=0;
int __return_370=0;
int __return_372=0;
int __return_2028=0;
int __return_2169=0;
int __return_2165=0;
int __return_225=0;
int __return_228=0;
int __return_371=0;
int __return_545=0;
int __return_547=0;
int __return_400=0;
int __return_403=0;
int __return_546=0;
int __return_135=0;
int __return_2167=0;
int __return_2163=0;
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
goto label_105;
}
else 
{
label_105:; 
{
s = NP;
pended = 0;
compFptr = 0;
compRegistered = 0;
lowerDriverReturn = 0;
setEventCalled = 0;
customIrp = 0;
}
if (status >= 0)
{
__cil_tmp8 = 1;
goto label_125;
}
else 
{
__cil_tmp8 = 0;
label_125:; 
if (__cil_tmp8 == 0)
{
 __return_2171 = -1;
goto label_135;
}
else 
{
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
if (s != NP)
{
{
__VERIFIER_error();
}
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
if (compRegistered == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_1447;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1447;
}
else 
{
returnVal2 = 259;
label_1447:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1518;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1506;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1506:; 
goto label_1518;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1518:; 
 __return_1519 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1473:; 
 __return_1521 = returnVal2;
}
status = __return_1519;
goto label_1523;
status = __return_1521;
label_1523:; 
__cil_tmp23 = (long)status;
if (__cil_tmp23 == 259)
{
{
int __tmp_5 = event;
int __tmp_6 = Executive;
int __tmp_7 = KernelMode;
int __tmp_8 = 0;
int __tmp_9 = 0;
int Object = __tmp_5;
int WaitReason = __tmp_6;
int WaitMode = __tmp_7;
int Alertable = __tmp_8;
int Timeout = __tmp_9;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_1914;
}
else 
{
goto label_1890;
}
}
else 
{
label_1890:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_1914;
}
else 
{
flag = s - MPR3;
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
 __return_1933 = 0;
goto label_1934;
}
else 
{
 __return_1934 = -1073741823;
label_1934:; 
}
goto label_1938;
}
else 
{
label_1914:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
 __return_1935 = 0;
goto label_1936;
}
else 
{
 __return_1936 = -1073741823;
label_1936:; 
}
label_1938:; 
if (status >= 0)
{
if (myStatus >= 0)
{
devExt__Started = 1;
devExt__Removed = 0;
devExt__SurpriseRemoved = 0;
goto label_1963;
}
else 
{
goto label_1963;
}
}
else 
{
label_1963:; 
Irp__IoStatus__Status = status;
myStatus = status;
Irp__IoStatus__Information = 0;
{
int __tmp_10 = Irp;
int __tmp_11 = 0;
int Irp = __tmp_10;
int PriorityBoost = __tmp_11;
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
goto label_1990;
goto label_1990;
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
goto label_1961;
}
else 
{
goto label_1961;
}
}
else 
{
label_1961:; 
Irp__IoStatus__Status = status;
myStatus = status;
Irp__IoStatus__Information = 0;
{
int __tmp_12 = Irp;
int __tmp_13 = 0;
int Irp = __tmp_12;
int PriorityBoost = __tmp_13;
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
goto label_1990;
label_1990:; 
 __return_2025 = status;
}
status = __return_2025;
goto label_2030;
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
int __tmp_14 = DeviceObject;
int __tmp_15 = Irp;
int __tmp_16 = lcontext;
int DeviceObject = __tmp_14;
int Irp = __tmp_15;
int Context = __tmp_16;
int event ;
event = Context;
{
int __tmp_17 = event;
int __tmp_18 = 0;
int __tmp_19 = 0;
int Event = __tmp_17;
int Increment = __tmp_18;
int Wait = __tmp_19;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1374 = l;
}
 __return_1377 = -1073741802;
}
compRetStatus = __return_1377;
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
goto label_1400;
label_1400:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_1445;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1445;
}
else 
{
returnVal2 = 259;
label_1445:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1516;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1504;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1504:; 
goto label_1516;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1516:; 
 __return_1520 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1473;
}
status = __return_1520;
goto label_1523;
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
irpSp__Control = 224;
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
if (compRegistered == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_1622;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1622;
}
else 
{
returnVal2 = 259;
label_1622:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1693;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1681;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1681:; 
goto label_1693;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1693:; 
 __return_1694 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1648:; 
 __return_1696 = returnVal2;
}
status = __return_1694;
goto label_1523;
status = __return_1696;
goto label_1523;
}
}
}
}
}
else 
{
{
int __tmp_22 = DeviceObject;
int __tmp_23 = Irp;
int __tmp_24 = lcontext;
int DeviceObject = __tmp_22;
int Irp = __tmp_23;
int Context = __tmp_24;
int event ;
event = Context;
{
int __tmp_25 = event;
int __tmp_26 = 0;
int __tmp_27 = 0;
int Event = __tmp_25;
int Increment = __tmp_26;
int Wait = __tmp_27;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1549 = l;
}
 __return_1552 = -1073741802;
}
compRetStatus = __return_1552;
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
goto label_1575;
label_1575:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_1620;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1620;
}
else 
{
returnVal2 = 259;
label_1620:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1691;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1679;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1679:; 
goto label_1691;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1691:; 
 __return_1695 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1648;
}
status = __return_1695;
goto label_1523;
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
irpSp__Control = 224;
{
int __tmp_28 = devExt__TopOfStack;
int __tmp_29 = Irp;
int DeviceObject = __tmp_28;
int Irp = __tmp_29;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_1797;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1797;
}
else 
{
returnVal2 = 259;
label_1797:; 
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
goto label_1856;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1856:; 
goto label_1868;
}
}
else 
{
flag = s - SKIP1;
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
label_1823:; 
 __return_1871 = returnVal2;
}
status = __return_1869;
goto label_1523;
status = __return_1871;
goto label_1523;
}
}
}
}
}
else 
{
{
int __tmp_30 = DeviceObject;
int __tmp_31 = Irp;
int __tmp_32 = lcontext;
int DeviceObject = __tmp_30;
int Irp = __tmp_31;
int Context = __tmp_32;
int event ;
event = Context;
{
int __tmp_33 = event;
int __tmp_34 = 0;
int __tmp_35 = 0;
int Event = __tmp_33;
int Increment = __tmp_34;
int Wait = __tmp_35;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1724 = l;
}
 __return_1727 = -1073741802;
}
compRetStatus = __return_1727;
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
goto label_1750;
label_1750:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_1795;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1795;
}
else 
{
returnVal2 = 259;
label_1795:; 
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
goto label_1854;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1854:; 
goto label_1866;
}
}
else 
{
flag = s - SKIP1;
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
goto label_1823;
}
status = __return_1870;
goto label_1523;
}
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
int __tmp_36 = devExt__TopOfStack;
int __tmp_37 = Irp;
int DeviceObject = __tmp_36;
int Irp = __tmp_37;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_1059;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1059;
}
else 
{
returnVal2 = 259;
label_1059:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1130;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1118;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1118:; 
goto label_1130;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1130:; 
 __return_1131 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1085:; 
 __return_1133 = returnVal2;
}
status = __return_1131;
goto label_1135;
status = __return_1133;
label_1135:; 
 __return_2026 = status;
}
status = __return_2026;
goto label_2030;
}
}
}
}
else 
{
{
int __tmp_38 = DeviceObject;
int __tmp_39 = Irp;
int __tmp_40 = lcontext;
int DeviceObject = __tmp_38;
int Irp = __tmp_39;
int Context = __tmp_40;
int event ;
event = Context;
{
int __tmp_41 = event;
int __tmp_42 = 0;
int __tmp_43 = 0;
int Event = __tmp_41;
int Increment = __tmp_42;
int Wait = __tmp_43;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_986 = l;
}
 __return_989 = -1073741802;
}
compRetStatus = __return_989;
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
goto label_1012;
label_1012:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_1057;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1057;
}
else 
{
returnVal2 = 259;
label_1057:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1128;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1116;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1116:; 
goto label_1128;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1128:; 
 __return_1132 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1085;
}
status = __return_1132;
goto label_1135;
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
int __tmp_44 = devExt__TopOfStack;
int __tmp_45 = Irp;
int DeviceObject = __tmp_44;
int Irp = __tmp_45;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_1234;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1234;
}
else 
{
returnVal2 = 259;
label_1234:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1305;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1293;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1293:; 
goto label_1305;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1305:; 
 __return_1306 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1260:; 
 __return_1308 = returnVal2;
}
status = __return_1306;
goto label_1135;
status = __return_1308;
goto label_1135;
}
}
}
}
}
else 
{
{
int __tmp_46 = DeviceObject;
int __tmp_47 = Irp;
int __tmp_48 = lcontext;
int DeviceObject = __tmp_46;
int Irp = __tmp_47;
int Context = __tmp_48;
int event ;
event = Context;
{
int __tmp_49 = event;
int __tmp_50 = 0;
int __tmp_51 = 0;
int Event = __tmp_49;
int Increment = __tmp_50;
int Wait = __tmp_51;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1161 = l;
}
 __return_1164 = -1073741802;
}
compRetStatus = __return_1164;
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
goto label_1187;
label_1187:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_1232;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1232;
}
else 
{
returnVal2 = 259;
label_1232:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1303;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1291;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1291:; 
goto label_1303;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1303:; 
 __return_1307 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1260;
}
status = __return_1307;
goto label_1135;
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
int __tmp_52 = devExt__TopOfStack;
int __tmp_53 = Irp;
int DeviceObject = __tmp_52;
int Irp = __tmp_53;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_678;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_678;
}
else 
{
returnVal2 = 259;
label_678:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_749;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_737;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_737:; 
goto label_749;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_749:; 
 __return_750 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_704:; 
 __return_752 = returnVal2;
}
goto label_754;
label_754:; 
status = 0;
 __return_2027 = status;
}
status = __return_2027;
goto label_2030;
}
}
}
}
else 
{
{
int __tmp_54 = DeviceObject;
int __tmp_55 = Irp;
int __tmp_56 = lcontext;
int DeviceObject = __tmp_54;
int Irp = __tmp_55;
int Context = __tmp_56;
int event ;
event = Context;
{
int __tmp_57 = event;
int __tmp_58 = 0;
int __tmp_59 = 0;
int Event = __tmp_57;
int Increment = __tmp_58;
int Wait = __tmp_59;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_605 = l;
}
 __return_608 = -1073741802;
}
compRetStatus = __return_608;
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
goto label_631;
label_631:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_676;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_676;
}
else 
{
returnVal2 = 259;
label_676:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_747;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_735;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_735:; 
goto label_747;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_747:; 
 __return_751 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_704;
}
goto label_754;
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
int __tmp_60 = devExt__TopOfStack;
int __tmp_61 = Irp;
int DeviceObject = __tmp_60;
int Irp = __tmp_61;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_853;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_853;
}
else 
{
returnVal2 = 259;
label_853:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_924;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_912;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_912:; 
goto label_924;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_924:; 
 __return_925 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_879:; 
 __return_927 = returnVal2;
}
goto label_754;
goto label_754;
}
}
}
}
}
else 
{
{
int __tmp_62 = DeviceObject;
int __tmp_63 = Irp;
int __tmp_64 = lcontext;
int DeviceObject = __tmp_62;
int Irp = __tmp_63;
int Context = __tmp_64;
int event ;
event = Context;
{
int __tmp_65 = event;
int __tmp_66 = 0;
int __tmp_67 = 0;
int Event = __tmp_65;
int Increment = __tmp_66;
int Wait = __tmp_67;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_780 = l;
}
 __return_783 = -1073741802;
}
compRetStatus = __return_783;
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
goto label_806;
label_806:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_851;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_851;
}
else 
{
returnVal2 = 259;
label_851:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_922;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_910;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_910:; 
goto label_922;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_922:; 
 __return_926 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_879;
}
goto label_754;
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
int __tmp_68 = devExt__TopOfStack;
int __tmp_69 = Irp;
int DeviceObject = __tmp_68;
int Irp = __tmp_69;
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
long long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_298;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_298;
}
else 
{
returnVal2 = 259;
label_298:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_369;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_357;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_357:; 
goto label_369;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_369:; 
 __return_370 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_324:; 
 __return_372 = returnVal2;
}
status = __return_370;
goto label_374;
status = __return_372;
label_374:; 
 __return_2028 = status;
}
status = __return_2028;
label_2030:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_2159;
}
else 
{
goto label_2051;
}
}
else 
{
label_2051:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_2159;
}
else 
{
goto label_2067;
}
}
else 
{
label_2067:; 
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
goto label_2109;
}
else 
{
goto label_2159;
}
}
else 
{
goto label_2109;
}
}
else 
{
label_2109:; 
if (pended == 1)
{
if (status != 259)
{
{
__VERIFIER_error();
}
 __return_2169 = status;
goto label_135;
}
else 
{
goto label_2159;
}
}
else 
{
goto label_2159;
}
}
}
else 
{
goto label_2159;
}
}
else 
{
label_2159:; 
 __return_2165 = status;
goto label_135;
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
 __return_225 = l;
}
 __return_228 = -1073741802;
}
compRetStatus = __return_228;
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
goto label_251;
label_251:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_296;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_296;
}
else 
{
returnVal2 = 259;
label_296:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_367;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_355;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_355:; 
goto label_367;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_367:; 
 __return_371 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_324;
}
status = __return_371;
goto label_374;
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
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_473;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_473;
}
else 
{
returnVal2 = 259;
label_473:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_544;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_532;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_532:; 
goto label_544;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_544:; 
 __return_545 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_499:; 
 __return_547 = returnVal2;
}
status = __return_545;
goto label_374;
status = __return_547;
goto label_374;
}
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
 __return_400 = l;
}
 __return_403 = -1073741802;
}
compRetStatus = __return_403;
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
goto label_426;
label_426:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
returnVal2 = 0;
goto label_471;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_471;
}
else 
{
returnVal2 = 259;
label_471:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_542;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_530;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_530:; 
goto label_542;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_542:; 
 __return_546 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_499;
}
status = __return_546;
goto label_374;
}
}
}
}
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
 __return_135 = -1;
label_135:; 
return 1;
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
goto label_2161;
}
else 
{
goto label_2053;
}
}
else 
{
label_2053:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_2161;
}
else 
{
goto label_2065;
}
}
else 
{
label_2065:; 
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
goto label_2107;
}
else 
{
goto label_2161;
}
}
else 
{
goto label_2107;
}
}
else 
{
label_2107:; 
if (pended == 1)
{
if (status != 259)
{
{
__VERIFIER_error();
}
 __return_2167 = status;
goto label_2163;
}
else 
{
goto label_2161;
}
}
else 
{
goto label_2161;
}
}
}
else 
{
goto label_2161;
}
}
else 
{
label_2161:; 
 __return_2163 = status;
label_2163:; 
return 1;
}
}
}
}
}
