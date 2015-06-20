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
int __return_2173=0;
int __return_1521=0;
int __return_1523=0;
int __return_1935=0;
int __return_1936=0;
int __return_1937=0;
int __return_1938=0;
int __return_2027=0;
int __return_1376=0;
int __return_1379=0;
int __return_1522=0;
int __return_1696=0;
int __return_1698=0;
int __return_1551=0;
int __return_1554=0;
int __return_1697=0;
int __return_1871=0;
int __return_1873=0;
int __return_1726=0;
int __return_1729=0;
int __return_1872=0;
int __return_1133=0;
int __return_1135=0;
int __return_2028=0;
int __return_988=0;
int __return_991=0;
int __return_1134=0;
int __return_1308=0;
int __return_1310=0;
int __return_1163=0;
int __return_1166=0;
int __return_1309=0;
int __return_752=0;
int __return_754=0;
int __return_2029=0;
int __return_607=0;
int __return_610=0;
int __return_753=0;
int __return_927=0;
int __return_929=0;
int __return_782=0;
int __return_785=0;
int __return_928=0;
int __return_372=0;
int __return_374=0;
int __return_2030=0;
int __return_2171=0;
int __return_2167=0;
int __return_227=0;
int __return_230=0;
int __return_373=0;
int __return_547=0;
int __return_549=0;
int __return_402=0;
int __return_405=0;
int __return_548=0;
int __return_137=0;
int __return_2169=0;
int __return_2165=0;
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
goto label_107;
}
else 
{
label_107:; 
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
goto label_127;
}
else 
{
__cil_tmp8 = 0;
label_127:; 
if (__cil_tmp8 == 0)
{
 __return_2173 = -1;
goto label_137;
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
goto label_1449;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1449;
}
else 
{
returnVal2 = 259;
label_1449:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1520;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1508;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1508:; 
goto label_1520;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1520:; 
 __return_1521 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1475:; 
 __return_1523 = returnVal2;
}
status = __return_1521;
goto label_1525;
status = __return_1523;
label_1525:; 
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
goto label_1916;
}
else 
{
goto label_1892;
}
}
else 
{
label_1892:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_1916;
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
 __return_1935 = 0;
goto label_1936;
}
else 
{
 __return_1936 = -1073741823;
label_1936:; 
}
goto label_1940;
}
else 
{
label_1916:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
 __return_1937 = 0;
goto label_1938;
}
else 
{
 __return_1938 = -1073741823;
label_1938:; 
}
label_1940:; 
if (status >= 0)
{
if (myStatus >= 0)
{
devExt__Started = 1;
devExt__Removed = 0;
devExt__SurpriseRemoved = 0;
goto label_1965;
}
else 
{
goto label_1965;
}
}
else 
{
label_1965:; 
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
goto label_1992;
goto label_1992;
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
goto label_1992;
label_1992:; 
 __return_2027 = status;
}
status = __return_2027;
goto label_2032;
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
 __return_1376 = l;
}
 __return_1379 = -1073741802;
}
compRetStatus = __return_1379;
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
goto label_1402;
label_1402:; 
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
 __return_1522 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1475;
}
status = __return_1522;
goto label_1525;
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
goto label_1624;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1624;
}
else 
{
returnVal2 = 259;
label_1624:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1695;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1683;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1683:; 
goto label_1695;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1695:; 
 __return_1696 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1650:; 
 __return_1698 = returnVal2;
}
status = __return_1696;
goto label_1525;
status = __return_1698;
goto label_1525;
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
 __return_1551 = l;
}
 __return_1554 = -1073741802;
}
compRetStatus = __return_1554;
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
goto label_1577;
label_1577:; 
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
 __return_1697 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1650;
}
status = __return_1697;
goto label_1525;
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
goto label_1799;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1799;
}
else 
{
returnVal2 = 259;
label_1799:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1870;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1858;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1858:; 
goto label_1870;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1870:; 
 __return_1871 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1825:; 
 __return_1873 = returnVal2;
}
status = __return_1871;
goto label_1525;
status = __return_1873;
goto label_1525;
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
 __return_1726 = l;
}
 __return_1729 = -1073741802;
}
compRetStatus = __return_1729;
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
goto label_1752;
label_1752:; 
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
 __return_1872 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1825;
}
status = __return_1872;
goto label_1525;
}
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
goto label_1061;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1061;
}
else 
{
returnVal2 = 259;
label_1061:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1132;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1120;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1120:; 
goto label_1132;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1132:; 
 __return_1133 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1087:; 
 __return_1135 = returnVal2;
}
status = __return_1133;
goto label_1137;
status = __return_1135;
label_1137:; 
 __return_2028 = status;
}
status = __return_2028;
goto label_2032;
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
 __return_988 = l;
}
 __return_991 = -1073741802;
}
compRetStatus = __return_991;
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
goto label_1014;
label_1014:; 
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
 __return_1134 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1087;
}
status = __return_1134;
goto label_1137;
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
goto label_1236;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_1236;
}
else 
{
returnVal2 = 259;
label_1236:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1307;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1295;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1295:; 
goto label_1307;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1307:; 
 __return_1308 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1262:; 
 __return_1310 = returnVal2;
}
status = __return_1308;
goto label_1137;
status = __return_1310;
goto label_1137;
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
 __return_1163 = l;
}
 __return_1166 = -1073741802;
}
compRetStatus = __return_1166;
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
goto label_1189;
label_1189:; 
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
 __return_1309 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1262;
}
status = __return_1309;
goto label_1137;
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
goto label_680;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_680;
}
else 
{
returnVal2 = 259;
label_680:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_751;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_739;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_739:; 
goto label_751;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_751:; 
 __return_752 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_706:; 
 __return_754 = returnVal2;
}
goto label_756;
label_756:; 
status = 0;
 __return_2029 = status;
}
status = __return_2029;
goto label_2032;
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
 __return_607 = l;
}
 __return_610 = -1073741802;
}
compRetStatus = __return_610;
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
goto label_633;
label_633:; 
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
 __return_753 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_706;
}
goto label_756;
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
goto label_855;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_855;
}
else 
{
returnVal2 = 259;
label_855:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_926;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_914;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_914:; 
goto label_926;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_926:; 
 __return_927 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_881:; 
 __return_929 = returnVal2;
}
goto label_756;
goto label_756;
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
 __return_782 = l;
}
 __return_785 = -1073741802;
}
compRetStatus = __return_785;
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
goto label_808;
label_808:; 
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
 __return_928 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_881;
}
goto label_756;
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
goto label_300;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_300;
}
else 
{
returnVal2 = 259;
label_300:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_371;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_359;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_359:; 
goto label_371;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_371:; 
 __return_372 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_326:; 
 __return_374 = returnVal2;
}
status = __return_372;
goto label_376;
status = __return_374;
label_376:; 
 __return_2030 = status;
}
status = __return_2030;
label_2032:; 
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
goto label_2069;
}
}
else 
{
label_2069:; 
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
goto label_2111;
}
else 
{
goto label_2161;
}
}
else 
{
goto label_2111;
}
}
else 
{
label_2111:; 
if (pended == 1)
{
if (status != 259)
{
{
__VERIFIER_error();
}
 __return_2171 = status;
goto label_137;
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
 __return_2167 = status;
goto label_137;
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
 __return_227 = l;
}
 __return_230 = -1073741802;
}
compRetStatus = __return_230;
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
goto label_253;
label_253:; 
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
 __return_373 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_326;
}
status = __return_373;
goto label_376;
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
goto label_475;
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 1)
{
returnVal2 = -1073741823;
goto label_475;
}
else 
{
returnVal2 = 259;
label_475:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_546;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_534;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_534:; 
goto label_546;
}
}
else 
{
flag = s - SKIP1;
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_546:; 
 __return_547 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_501:; 
 __return_549 = returnVal2;
}
status = __return_547;
goto label_376;
status = __return_549;
goto label_376;
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
 __return_402 = l;
}
 __return_405 = -1073741802;
}
compRetStatus = __return_405;
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
goto label_428;
label_428:; 
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
 __return_548 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_501;
}
status = __return_548;
goto label_376;
}
}
}
}
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
 __return_137 = -1;
label_137:; 
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
goto label_2163;
}
else 
{
goto label_2055;
}
}
else 
{
label_2055:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_2163;
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
goto label_2163;
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
goto label_2165;
}
else 
{
goto label_2163;
}
}
else 
{
goto label_2163;
}
}
}
else 
{
goto label_2163;
}
}
else 
{
label_2163:; 
 __return_2165 = status;
label_2165:; 
return 1;
}
}
}
}
}
