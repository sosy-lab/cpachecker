extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern char __VERIFIER_nondet_char(void);
extern int __VERIFIER_nondet_int(void);
extern long __VERIFIER_nondet_long(void);
extern void *__VERIFIER_nondet_pointer(void);
void IofCompleteRequest(int Irp , int PriorityBoost );
int FloppyThread  ;
int KernelMode  ;
int Suspended  ;
int Executive  ;
int DiskController  ;
int FloppyDiskPeripheral  ;
int FlConfigCallBack  ;
int MaximumInterfaceType  ;
int MOUNTDEV_MOUNTED_DEVICE_GUID  ;
int myStatus  ;
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
int compRegistered  ;
int lowerDriverReturn  ;
int setEventCalled  ;
int customIrp  ;
int flag=0;
void errorFn(void);
void _BLAST_init(void);
int PagingReferenceCount  =    0;
int PagingMutex  =    0;
int FlAcpiConfigureFloppy(int DisketteExtension , int FdcInfo );
int FlQueueIrpToThread(int Irp , int DisketteExtension );
int FloppyPnp(int DeviceObject , int Irp );
int FloppyStartDevice(int DeviceObject , int Irp );
int FloppyPnpComplete(int DeviceObject , int Irp , int Context );
int FlFdcDeviceIo(int DeviceObject , int Ioctl , int Data );
void FloppyProcessQueuedRequests(int DisketteExtension );
void stub_driver_init(void);
int main(void);
int IoBuildDeviceIoControlRequest(int IoControlCode , int DeviceObject , int InputBuffer ,
                                  int InputBufferLength , int OutputBuffer , int OutputBufferLength ,
                                  int InternalDeviceIoControl , int Event , int IoStatusBlock );
int IoDeleteSymbolicLink(int SymbolicLinkName );
int IoQueryDeviceDescription(int BusType , int BusNumber , int ControllerType , int ControllerNumber ,
                             int PeripheralType , int PeripheralNumber , int CalloutRoutine ,
                             int Context );
int IoRegisterDeviceInterface(int PhysicalDeviceObject , int InterfaceClassGuid ,
                              int ReferenceString , int SymbolicLinkName );
int IoSetDeviceInterfaceState(int SymbolicLinkName , int Enable );
void stubMoreProcessingRequired(void);
int IofCallDriver(int DeviceObject , int Irp );
int KeSetEvent(int Event , int Increment , int Wait );
int KeWaitForSingleObject(int Object , int WaitReason , int WaitMode , int Alertable ,
                          int Timeout );
int ObReferenceObjectByHandle(int Handle , int DesiredAccess , int ObjectType , int AccessMode ,
                              int Object , int HandleInformation );
int PsCreateSystemThread(int ThreadHandle , int DesiredAccess , int ObjectAttributes ,
                         int ProcessHandle , int ClientId , int StartRoutine , int StartContext );
int ZwClose(int Handle );
int FloppyCreateClose(int DeviceObject , int Irp );
int __return_6832=0;
int __return_5290=0;
int __return_5294=0;
int __return_5086=0;
int __return_5089=0;
int __return_5292=0;
int __return_5293=0;
int __return_5291=0;
int __return_5052=0;
int __return_5056=0;
int __return_4848=0;
int __return_4851=0;
int __return_5054=0;
int __return_5055=0;
int __return_5053=0;
int __return_4814=0;
int __return_4818=0;
int __return_5359=0;
int __return_5360=0;
int __return_5361=0;
int __return_5362=0;
int __return_5410=0;
int __return_5411=0;
int __return_5722=0;
int __return_6188=0;
int __return_6189=0;
int __return_6242=0;
int __return_6243=0;
int __return_6535=0;
int __return_6115=0;
int __return_6116=0;
int __return_6172=0;
int __return_6173=0;
int __return_6226=0;
int __return_6227=0;
int __return_6536=0;
int __return_6087=0;
int __return_6204=0;
int __return_6205=0;
int __return_6258=0;
int __return_6259=0;
int __return_6537=0;
int __return_6607=0;
int __return_6534=0;
int __return_6533=0;
int __return_5641=0;
int __return_5645=0;
int __return_5710=0;
int __return_5711=0;
int __return_5712=0;
int __return_5713=0;
int __return_5720=0;
int __return_5721=0;
int __return_5437=0;
int __return_5440=0;
int __return_5643=0;
int __return_5644=0;
int __return_5642=0;
int __return_5758=0;
int __return_5759=0;
int __return_6070=0;
int __return_5989=0;
int __return_5993=0;
int __return_6058=0;
int __return_6059=0;
int __return_6060=0;
int __return_6061=0;
int __return_6068=0;
int __return_6069=0;
int __return_5785=0;
int __return_5788=0;
int __return_5991=0;
int __return_5992=0;
int __return_5990=0;
int __return_4610=0;
int __return_4613=0;
int __return_4816=0;
int __return_4817=0;
int __return_4815=0;
int __return_4227=0;
int __return_4231=0;
int __return_4480=0;
int __return_4023=0;
int __return_4026=0;
int __return_4229=0;
int __return_4230=0;
int __return_4228=0;
int __return_4465=0;
int __return_4469=0;
int __return_4261=0;
int __return_4264=0;
int __return_4467=0;
int __return_4468=0;
int __return_4466=0;
int __return_3367=0;
int __return_3283=0;
int __return_3284=0;
int __return_3363=0;
int __return_3301=0;
int __return_3302=0;
int __return_3317=0;
int __return_3318=0;
int __return_3356=0;
int __return_3357=0;
int __return_3354=0;
int __return_3368=0;
int __return_3461=0;
int __return_3462=0;
int __return_3463=0;
int __return_3464=0;
int __return_3722=0;
int __return_3726=0;
int __return_6609=0;
int __return_3518=0;
int __return_3521=0;
int __return_3724=0;
int __return_3725=0;
int __return_3723=0;
int __return_3960=0;
int __return_3964=0;
int __return_3756=0;
int __return_3759=0;
int __return_3962=0;
int __return_3963=0;
int __return_3961=0;
int __return_6608=0;
int __return_3355=0;
int __return_2974=0;
int __return_2978=0;
int __return_6611=0;
int __return_2770=0;
int __return_2773=0;
int __return_2976=0;
int __return_2977=0;
int __return_2975=0;
int __return_3212=0;
int __return_3216=0;
int __return_3008=0;
int __return_3011=0;
int __return_3214=0;
int __return_3215=0;
int __return_3213=0;
int __return_2589=0;
int __return_2593=0;
int __return_2385=0;
int __return_2388=0;
int __return_2591=0;
int __return_2592=0;
int __return_2590=0;
int __return_2351=0;
int __return_2355=0;
int __return_2147=0;
int __return_2150=0;
int __return_2353=0;
int __return_2354=0;
int __return_2352=0;
int __return_2113=0;
int __return_2117=0;
int __return_2658=0;
int __return_2659=0;
int __return_2660=0;
int __return_2661=0;
int __return_6610=0;
int __return_1909=0;
int __return_1912=0;
int __return_2115=0;
int __return_2116=0;
int __return_2114=0;
int __return_1589=0;
int __return_1593=0;
int __return_6612=0;
int __return_1385=0;
int __return_1388=0;
int __return_1591=0;
int __return_1592=0;
int __return_1590=0;
int __return_1827=0;
int __return_1831=0;
int __return_1623=0;
int __return_1626=0;
int __return_1829=0;
int __return_1830=0;
int __return_1828=0;
int __return_1014=0;
int __return_1018=0;
int __return_1281=0;
int __return_1282=0;
int __return_1318=0;
int __return_1319=0;
int __return_6615=0;
int __return_1302=0;
int __return_1303=0;
int __return_6613=0;
int __return_6614=0;
int __return_810=0;
int __return_813=0;
int __return_1016=0;
int __return_1017=0;
int __return_1015=0;
int __return_1252=0;
int __return_1256=0;
int __return_1048=0;
int __return_1051=0;
int __return_1254=0;
int __return_1255=0;
int __return_1253=0;
int __return_502=0;
int __return_506=0;
int __return_6616=0;
int __return_6828=0;
int __return_298=0;
int __return_301=0;
int __return_504=0;
int __return_505=0;
int __return_503=0;
int __return_740=0;
int __return_744=0;
int __return_536=0;
int __return_539=0;
int __return_742=0;
int __return_743=0;
int __return_741=0;
int __return_236=0;
int __return_153=0;
int __return_6826=0;
int __return_6824=0;
int __return_6822=0;
int __return_6830=0;
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
FloppyThread = 0;
KernelMode = 0;
Suspended = 0;
Executive = 0;
DiskController = 0;
FloppyDiskPeripheral = 0;
FlConfigCallBack = 0;
MaximumInterfaceType = 0;
MOUNTDEV_MOUNTED_DEVICE_GUID = 0;
myStatus = 0;
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
compRegistered = 0;
lowerDriverReturn = 0;
setEventCalled = 0;
customIrp = 0;
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
goto label_131;
}
else 
{
label_131:; 
{
s = NP;
pended = 0;
compRegistered = 0;
lowerDriverReturn = 0;
setEventCalled = 0;
customIrp = 0;
}
if (status < 0)
{
 __return_6832 = -1;
goto label_153;
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
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int Irp__IoStatus__Information ;
int Irp__IoStatus__Status ;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int disketteExtension__IsRemoved = __VERIFIER_nondet_int() ;
disketteExtension__IsRemoved = __VERIFIER_nondet_int();
int disketteExtension__IsStarted = __VERIFIER_nondet_int() ;
disketteExtension__IsStarted = __VERIFIER_nondet_int();
int disketteExtension__TargetObject = __VERIFIER_nondet_int() ;
disketteExtension__TargetObject = __VERIFIER_nondet_int();
int disketteExtension__HoldNewRequests ;
int disketteExtension__FloppyThread = __VERIFIER_nondet_int() ;
disketteExtension__FloppyThread = __VERIFIER_nondet_int();
int disketteExtension__InterfaceString__Buffer = __VERIFIER_nondet_int() ;
disketteExtension__InterfaceString__Buffer = __VERIFIER_nondet_int();
int disketteExtension__InterfaceString = __VERIFIER_nondet_int() ;
disketteExtension__InterfaceString = __VERIFIER_nondet_int();
int disketteExtension__ArcName__Length = __VERIFIER_nondet_int() ;
disketteExtension__ArcName__Length = __VERIFIER_nondet_int();
int disketteExtension__ArcName = __VERIFIER_nondet_int() ;
disketteExtension__ArcName = __VERIFIER_nondet_int();
int irpSp__MinorFunction = __VERIFIER_nondet_int() ;
irpSp__MinorFunction = __VERIFIER_nondet_int();
int IoGetConfigurationInformation__FloppyCount = __VERIFIER_nondet_int() ;
IoGetConfigurationInformation__FloppyCount = __VERIFIER_nondet_int();
int irpSp ;
int disketteExtension ;
int ntStatus ;
int doneEvent = __VERIFIER_nondet_int() ;
doneEvent = __VERIFIER_nondet_int();
int irpSp___0 ;
int nextIrpSp ;
int nextIrpSp__Control ;
int irpSp___1 ;
int irpSp__Context ;
int irpSp__Control ;
long __cil_tmp29 ;
long __cil_tmp30 ;
ntStatus = 0;
int __CPAchecker_TMP_0 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount + 1;
__CPAchecker_TMP_0;
disketteExtension = DeviceObject__DeviceExtension;
irpSp = Irp__Tail__Overlay__CurrentStackLocation;
if (disketteExtension__IsRemoved == 0)
{
if (irpSp__MinorFunction == 0)
{
{
int __tmp_3 = DeviceObject;
int __tmp_4 = Irp;
int DeviceObject = __tmp_3;
int Irp = __tmp_4;
int DeviceObject__DeviceExtension = __VERIFIER_nondet_int() ;
DeviceObject__DeviceExtension = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int Irp__IoStatus__Status ;
int disketteExtension__TargetObject = __VERIFIER_nondet_int() ;
disketteExtension__TargetObject = __VERIFIER_nondet_int();
int disketteExtension__MaxTransferSize ;
int disketteExtension__DriveType = __VERIFIER_nondet_int() ;
disketteExtension__DriveType = __VERIFIER_nondet_int();
int disketteExtension__PerpendicularMode ;
int disketteExtension__DeviceUnit ;
int disketteExtension__DriveOnValue ;
int disketteExtension__UnderlyingPDO = __VERIFIER_nondet_int() ;
disketteExtension__UnderlyingPDO = __VERIFIER_nondet_int();
int disketteExtension__InterfaceString = __VERIFIER_nondet_int() ;
disketteExtension__InterfaceString = __VERIFIER_nondet_int();
int disketteExtension__IsStarted ;
int disketteExtension__HoldNewRequests ;
int ntStatus ;
int pnpStatus ;
int doneEvent = __VERIFIER_nondet_int() ;
doneEvent = __VERIFIER_nondet_int();
int fdcInfo = __VERIFIER_nondet_int() ;
fdcInfo = __VERIFIER_nondet_int();
int fdcInfo__BufferCount ;
int fdcInfo__BufferSize ;
int fdcInfo__MaxTransferSize = __VERIFIER_nondet_int() ;
fdcInfo__MaxTransferSize = __VERIFIER_nondet_int();
int fdcInfo__AcpiBios = __VERIFIER_nondet_int() ;
fdcInfo__AcpiBios = __VERIFIER_nondet_int();
int fdcInfo__AcpiFdiSupported = __VERIFIER_nondet_int() ;
fdcInfo__AcpiFdiSupported = __VERIFIER_nondet_int();
int fdcInfo__PeripheralNumber = __VERIFIER_nondet_int() ;
fdcInfo__PeripheralNumber = __VERIFIER_nondet_int();
int fdcInfo__BusType ;
int fdcInfo__ControllerNumber = __VERIFIER_nondet_int() ;
fdcInfo__ControllerNumber = __VERIFIER_nondet_int();
int fdcInfo__UnitNumber = __VERIFIER_nondet_int() ;
fdcInfo__UnitNumber = __VERIFIER_nondet_int();
int fdcInfo__BusNumber = __VERIFIER_nondet_int() ;
fdcInfo__BusNumber = __VERIFIER_nondet_int();
int Dc ;
int Fp ;
int disketteExtension ;
int irpSp ;
int irpSp___0 ;
int nextIrpSp ;
int nextIrpSp__Control ;
int irpSp___1 ;
int irpSp__Control ;
int irpSp__Context ;
int InterfaceType ;
int KUSER_SHARED_DATA__AlternativeArchitecture_NEC98x86 = __VERIFIER_nondet_int() ;
KUSER_SHARED_DATA__AlternativeArchitecture_NEC98x86 = __VERIFIER_nondet_int();
long __cil_tmp42 ;
int __cil_tmp43 ;
int __cil_tmp44 ;
int __cil_tmp45 ;
int __cil_tmp46 ;
int __cil_tmp47 ;
int __cil_tmp48 ;
int __cil_tmp49 ;
Dc = DiskController;
Fp = FloppyDiskPeripheral;
disketteExtension = DeviceObject__DeviceExtension;
irpSp = Irp__Tail__Overlay__CurrentStackLocation;
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation;
nextIrpSp = Irp__Tail__Overlay__CurrentStackLocation - 1;
nextIrpSp__Control = 0;
flag = s - NP;
if (s != NP)
{
{
__VERIFIER_error();
}
irpSp___1 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = doneEvent;
irpSp__Control = 224;
{
int __tmp_5 = disketteExtension__TargetObject;
int __tmp_6 = Irp;
int DeviceObject = __tmp_5;
int Irp = __tmp_6;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_5185;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_5185;
}
else 
{
returnVal2 = 259;
label_5185:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5289;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5271;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5271:; 
goto label_5289;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5289:; 
 __return_5290 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_5294 = returnVal2;
}
ntStatus = __return_5290;
goto label_4820;
ntStatus = __return_5294;
goto label_4820;
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
{
int __tmp_10 = Context;
int __tmp_11 = 1;
int __tmp_12 = 0;
int Event = __tmp_10;
int Increment = __tmp_11;
int Wait = __tmp_12;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_5086 = l;
}
 __return_5089 = -1073741802;
}
compRetStatus1 = __return_5089;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_5116;
label_5116:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_5181;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_5181;
}
else 
{
returnVal2 = 259;
label_5181:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5285;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5267;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5267:; 
goto label_5285;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5285:; 
 __return_5292 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_5217:; 
 __return_5293 = returnVal2;
}
ntStatus = __return_5292;
goto label_4820;
ntStatus = __return_5293;
goto label_4820;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_5183;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_5183;
}
else 
{
returnVal2 = 259;
label_5183:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5287;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5269;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5269:; 
goto label_5287;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5287:; 
 __return_5291 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_5217;
}
ntStatus = __return_5291;
goto label_4820;
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
irpSp___1 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = doneEvent;
irpSp__Control = 224;
{
int __tmp_13 = disketteExtension__TargetObject;
int __tmp_14 = Irp;
int DeviceObject = __tmp_13;
int Irp = __tmp_14;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4947;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4947;
}
else 
{
returnVal2 = 259;
label_4947:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5051;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5033;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5033:; 
goto label_5051;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5051:; 
 __return_5052 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_5056 = returnVal2;
}
ntStatus = __return_5052;
goto label_4820;
ntStatus = __return_5056;
goto label_4820;
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
{
int __tmp_18 = Context;
int __tmp_19 = 1;
int __tmp_20 = 0;
int Event = __tmp_18;
int Increment = __tmp_19;
int Wait = __tmp_20;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_4848 = l;
}
 __return_4851 = -1073741802;
}
compRetStatus1 = __return_4851;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_4878;
label_4878:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4943;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4943;
}
else 
{
returnVal2 = 259;
label_4943:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5047;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5029;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5029:; 
goto label_5047;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5047:; 
 __return_5054 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4979:; 
 __return_5055 = returnVal2;
}
ntStatus = __return_5054;
goto label_4820;
ntStatus = __return_5055;
goto label_4820;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4945;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4945;
}
else 
{
returnVal2 = 259;
label_4945:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5049;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5031;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5031:; 
goto label_5049;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5049:; 
 __return_5053 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4979;
}
ntStatus = __return_5053;
goto label_4820;
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
irpSp___1 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = doneEvent;
irpSp__Control = 224;
{
int __tmp_21 = disketteExtension__TargetObject;
int __tmp_22 = Irp;
int DeviceObject = __tmp_21;
int Irp = __tmp_22;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4709;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4709;
}
else 
{
returnVal2 = 259;
label_4709:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4813;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4795;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4795:; 
goto label_4813;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4813:; 
 __return_4814 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_4818 = returnVal2;
}
ntStatus = __return_4814;
goto label_4820;
ntStatus = __return_4818;
label_4820:; 
__cil_tmp42 = (long)ntStatus;
if (__cil_tmp42 == 259L)
{
{
int __tmp_23 = doneEvent;
int __tmp_24 = Executive;
int __tmp_25 = KernelMode;
int __tmp_26 = 0;
int __tmp_27 = 0;
int Object = __tmp_23;
int WaitReason = __tmp_24;
int WaitMode = __tmp_25;
int Alertable = __tmp_26;
int Timeout = __tmp_27;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_5340;
}
else 
{
goto label_5317;
}
}
else 
{
label_5317:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_5340;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
 __return_5359 = 0;
goto label_5360;
}
else 
{
 __return_5360 = -1073741823;
label_5360:; 
}
ntStatus = __return_5360;
goto label_5364;
}
else 
{
label_5340:; 
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
 __return_5361 = 0;
goto label_5362;
}
else 
{
 __return_5362 = -1073741823;
label_5362:; 
}
ntStatus = __return_5362;
label_5364:; 
ntStatus = myStatus;
fdcInfo__BufferCount = 0;
fdcInfo__BufferSize = 0;
__cil_tmp43 = 3080;
__cil_tmp44 = 458752;
__cil_tmp45 = 461832;
__cil_tmp46 = 461835;
{
int __tmp_28 = disketteExtension__TargetObject;
int __tmp_29 = __cil_tmp46;
int __tmp_30 = fdcInfo;
int DeviceObject = __tmp_28;
int Ioctl = __tmp_29;
int Data = __tmp_30;
int ntStatus ;
int irp ;
int irpStack ;
int doneEvent = __VERIFIER_nondet_int() ;
doneEvent = __VERIFIER_nondet_int();
int ioStatus = __VERIFIER_nondet_int() ;
ioStatus = __VERIFIER_nondet_int();
int irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int irpStack__Parameters__DeviceIoControl__Type3InputBuffer ;
long __cil_tmp11 ;
{
int __tmp_31 = Ioctl;
int __tmp_32 = DeviceObject;
int __tmp_33 = 0;
int __tmp_34 = 0;
int __tmp_35 = 0;
int __tmp_36 = 0;
int __tmp_37 = 1;
int __tmp_38 = doneEvent;
int __tmp_39 = ioStatus;
int IoControlCode = __tmp_31;
int DeviceObject = __tmp_32;
int InputBuffer = __tmp_33;
int InputBufferLength = __tmp_34;
int OutputBuffer = __tmp_35;
int OutputBufferLength = __tmp_36;
int InternalDeviceIoControl = __tmp_37;
int Event = __tmp_38;
int IoStatusBlock = __tmp_39;
int malloc = __VERIFIER_nondet_int() ;
malloc = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
 __return_5410 = malloc;
goto label_5411;
}
else 
{
 __return_5411 = 0;
label_5411:; 
}
irp = __return_5411;
if (irp == 0)
{
 __return_5722 = -1073741670;
}
else 
{
irpStack = irp__Tail__Overlay__CurrentStackLocation - 1;
irpStack__Parameters__DeviceIoControl__Type3InputBuffer = Data;
{
int __tmp_88 = DeviceObject;
int __tmp_89 = irp;
int DeviceObject = __tmp_88;
int Irp = __tmp_89;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_5536;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_5536;
}
else 
{
returnVal2 = 259;
label_5536:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5640;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5622;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5622:; 
goto label_5640;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5640:; 
 __return_5641 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_5645 = returnVal2;
}
ntStatus = __return_5641;
goto label_5647;
ntStatus = __return_5645;
label_5647:; 
__cil_tmp11 = (long)ntStatus;
if (__cil_tmp11 == 259L)
{
{
int __tmp_90 = doneEvent;
int __tmp_91 = Suspended;
int __tmp_92 = KernelMode;
int __tmp_93 = 0;
int __tmp_94 = 0;
int Object = __tmp_90;
int WaitReason = __tmp_91;
int WaitMode = __tmp_92;
int Alertable = __tmp_93;
int Timeout = __tmp_94;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_5691;
}
else 
{
goto label_5668;
}
}
else 
{
label_5668:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_5691;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
 __return_5710 = 0;
goto label_5711;
}
else 
{
 __return_5711 = -1073741823;
label_5711:; 
}
goto label_5715;
}
else 
{
label_5691:; 
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
 __return_5712 = 0;
goto label_5713;
}
else 
{
 __return_5713 = -1073741823;
label_5713:; 
}
label_5715:; 
ntStatus = myStatus;
 __return_5720 = ntStatus;
}
ntStatus = __return_5720;
goto label_5724;
}
}
}
}
else 
{
 __return_5721 = ntStatus;
}
ntStatus = __return_5721;
goto label_5724;
}
}
}
}
}
else 
{
{
int __tmp_95 = DeviceObject;
int __tmp_96 = Irp;
int __tmp_97 = lcontext;
int DeviceObject = __tmp_95;
int Irp = __tmp_96;
int Context = __tmp_97;
{
int __tmp_98 = Context;
int __tmp_99 = 1;
int __tmp_100 = 0;
int Event = __tmp_98;
int Increment = __tmp_99;
int Wait = __tmp_100;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_5437 = l;
}
 __return_5440 = -1073741802;
}
compRetStatus1 = __return_5440;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_5467;
label_5467:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_5532;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_5532;
}
else 
{
returnVal2 = 259;
label_5532:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5636;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5618;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5618:; 
goto label_5636;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5636:; 
 __return_5643 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_5568:; 
 __return_5644 = returnVal2;
}
ntStatus = __return_5643;
goto label_5647;
ntStatus = __return_5644;
goto label_5647;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_5534;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_5534;
}
else 
{
returnVal2 = 259;
label_5534:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5638;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5620;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5620:; 
goto label_5638;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5638:; 
 __return_5642 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_5568;
}
ntStatus = __return_5642;
goto label_5647;
}
}
}
}
}
}
}
}
ntStatus = __return_5722;
label_5724:; 
if (ntStatus >= 0)
{
disketteExtension__MaxTransferSize = fdcInfo__MaxTransferSize;
if (fdcInfo__AcpiBios == 0)
{
goto label_6093;
}
else 
{
if (fdcInfo__AcpiFdiSupported == 0)
{
label_6093:; 
InterfaceType = 0;
label_6097:; 
if (InterfaceType >= MaximumInterfaceType)
{
if (ntStatus >= 0)
{
if (KUSER_SHARED_DATA__AlternativeArchitecture_NEC98x86 != 0)
{
disketteExtension__DeviceUnit = fdcInfo__UnitNumber;
goto label_6157;
}
else 
{
disketteExtension__DeviceUnit = fdcInfo__PeripheralNumber;
label_6157:; 
{
int __tmp_40 = disketteExtension__UnderlyingPDO;
int __tmp_41 = MOUNTDEV_MOUNTED_DEVICE_GUID;
int __tmp_42 = 0;
int __tmp_43 = disketteExtension__InterfaceString;
int PhysicalDeviceObject = __tmp_40;
int InterfaceClassGuid = __tmp_41;
int ReferenceString = __tmp_42;
int SymbolicLinkName = __tmp_43;
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
 __return_6188 = 0;
goto label_6189;
}
else 
{
 __return_6189 = -1073741808;
label_6189:; 
}
pnpStatus = __return_6189;
if (pnpStatus >= 0)
{
{
int __tmp_44 = disketteExtension__InterfaceString;
int __tmp_45 = 1;
int SymbolicLinkName = __tmp_44;
int Enable = __tmp_45;
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
 __return_6242 = 0;
goto label_6243;
}
else 
{
 __return_6243 = -1073741823;
label_6243:; 
}
pnpStatus = __return_6243;
disketteExtension__IsStarted = 1;
disketteExtension__HoldNewRequests = 0;
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
{
int __tmp_46 = Irp;
int __tmp_47 = 0;
int Irp = __tmp_46;
int PriorityBoost = __tmp_47;
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
goto label_6323;
goto label_6323;
}
}
}
else 
{
disketteExtension__IsStarted = 1;
disketteExtension__HoldNewRequests = 0;
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
{
int __tmp_48 = Irp;
int __tmp_49 = 0;
int Irp = __tmp_48;
int PriorityBoost = __tmp_49;
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
goto label_6323;
goto label_6323;
}
}
}
}
}
else 
{
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
{
int __tmp_50 = Irp;
int __tmp_51 = 0;
int Irp = __tmp_50;
int PriorityBoost = __tmp_51;
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
goto label_6484;
label_6484:; 
 __return_6535 = ntStatus;
}
ntStatus = __return_6535;
goto label_6539;
}
}
else 
{
fdcInfo__BusType = InterfaceType;
{
int __tmp_52 = fdcInfo__BusType;
int __tmp_53 = fdcInfo__BusNumber;
int __tmp_54 = Dc;
int __tmp_55 = fdcInfo__ControllerNumber;
int __tmp_56 = Fp;
int __tmp_57 = fdcInfo__PeripheralNumber;
int __tmp_58 = FlConfigCallBack;
int __tmp_59 = disketteExtension;
int BusType = __tmp_52;
int BusNumber = __tmp_53;
int ControllerType = __tmp_54;
int ControllerNumber = __tmp_55;
int PeripheralType = __tmp_56;
int PeripheralNumber = __tmp_57;
int CalloutRoutine = __tmp_58;
int Context = __tmp_59;
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
 __return_6115 = 0;
goto label_6116;
}
else 
{
 __return_6116 = -1073741823;
label_6116:; 
}
ntStatus = __return_6116;
if (ntStatus >= 0)
{
if (ntStatus >= 0)
{
if (KUSER_SHARED_DATA__AlternativeArchitecture_NEC98x86 != 0)
{
disketteExtension__DeviceUnit = fdcInfo__UnitNumber;
goto label_6159;
}
else 
{
disketteExtension__DeviceUnit = fdcInfo__PeripheralNumber;
label_6159:; 
{
int __tmp_60 = disketteExtension__UnderlyingPDO;
int __tmp_61 = MOUNTDEV_MOUNTED_DEVICE_GUID;
int __tmp_62 = 0;
int __tmp_63 = disketteExtension__InterfaceString;
int PhysicalDeviceObject = __tmp_60;
int InterfaceClassGuid = __tmp_61;
int ReferenceString = __tmp_62;
int SymbolicLinkName = __tmp_63;
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
 __return_6172 = 0;
goto label_6173;
}
else 
{
 __return_6173 = -1073741808;
label_6173:; 
}
pnpStatus = __return_6173;
if (pnpStatus >= 0)
{
{
int __tmp_64 = disketteExtension__InterfaceString;
int __tmp_65 = 1;
int SymbolicLinkName = __tmp_64;
int Enable = __tmp_65;
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
 __return_6226 = 0;
goto label_6227;
}
else 
{
 __return_6227 = -1073741823;
label_6227:; 
}
pnpStatus = __return_6227;
disketteExtension__IsStarted = 1;
disketteExtension__HoldNewRequests = 0;
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
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
goto label_6323;
goto label_6323;
}
}
}
else 
{
disketteExtension__IsStarted = 1;
disketteExtension__HoldNewRequests = 0;
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
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
goto label_6323;
goto label_6323;
}
}
}
}
}
else 
{
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
{
int __tmp_70 = Irp;
int __tmp_71 = 0;
int Irp = __tmp_70;
int PriorityBoost = __tmp_71;
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
goto label_6461;
label_6461:; 
 __return_6536 = ntStatus;
}
ntStatus = __return_6536;
goto label_6539;
}
}
else 
{
int __CPAchecker_TMP_0 = InterfaceType;
InterfaceType = InterfaceType + 1;
__CPAchecker_TMP_0;
goto label_6097;
}
}
}
}
else 
{
{
int __tmp_72 = disketteExtension;
int __tmp_73 = fdcInfo;
int DisketteExtension = __tmp_72;
int FdcInfo = __tmp_73;
 __return_6087 = 0;
}
ntStatus = __return_6087;
if (ntStatus >= 0)
{
if (KUSER_SHARED_DATA__AlternativeArchitecture_NEC98x86 != 0)
{
disketteExtension__DeviceUnit = fdcInfo__UnitNumber;
goto label_6155;
}
else 
{
disketteExtension__DeviceUnit = fdcInfo__PeripheralNumber;
label_6155:; 
{
int __tmp_74 = disketteExtension__UnderlyingPDO;
int __tmp_75 = MOUNTDEV_MOUNTED_DEVICE_GUID;
int __tmp_76 = 0;
int __tmp_77 = disketteExtension__InterfaceString;
int PhysicalDeviceObject = __tmp_74;
int InterfaceClassGuid = __tmp_75;
int ReferenceString = __tmp_76;
int SymbolicLinkName = __tmp_77;
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
 __return_6204 = 0;
goto label_6205;
}
else 
{
 __return_6205 = -1073741808;
label_6205:; 
}
pnpStatus = __return_6205;
if (pnpStatus >= 0)
{
{
int __tmp_78 = disketteExtension__InterfaceString;
int __tmp_79 = 1;
int SymbolicLinkName = __tmp_78;
int Enable = __tmp_79;
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
 __return_6258 = 0;
goto label_6259;
}
else 
{
 __return_6259 = -1073741823;
label_6259:; 
}
pnpStatus = __return_6259;
disketteExtension__IsStarted = 1;
disketteExtension__HoldNewRequests = 0;
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
{
int __tmp_80 = Irp;
int __tmp_81 = 0;
int Irp = __tmp_80;
int PriorityBoost = __tmp_81;
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
goto label_6323;
goto label_6323;
}
}
}
else 
{
disketteExtension__IsStarted = 1;
disketteExtension__HoldNewRequests = 0;
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
{
int __tmp_82 = Irp;
int __tmp_83 = 0;
int Irp = __tmp_82;
int PriorityBoost = __tmp_83;
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
goto label_6323;
label_6323:; 
 __return_6537 = ntStatus;
}
ntStatus = __return_6537;
label_6539:; 
int __CPAchecker_TMP_14 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount - 1;
__CPAchecker_TMP_14;
 __return_6607 = ntStatus;
}
status = __return_6607;
goto label_6618;
}
}
}
else 
{
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
{
int __tmp_84 = Irp;
int __tmp_85 = 0;
int Irp = __tmp_84;
int PriorityBoost = __tmp_85;
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
goto label_6507;
label_6507:; 
 __return_6534 = ntStatus;
}
ntStatus = __return_6534;
goto label_6539;
}
}
}
}
else 
{
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
{
int __tmp_86 = Irp;
int __tmp_87 = 0;
int Irp = __tmp_86;
int PriorityBoost = __tmp_87;
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
goto label_6530;
label_6530:; 
 __return_6533 = ntStatus;
}
ntStatus = __return_6533;
goto label_6539;
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
fdcInfo__BufferCount = 0;
fdcInfo__BufferSize = 0;
__cil_tmp43 = 3080;
__cil_tmp44 = 458752;
__cil_tmp45 = 461832;
__cil_tmp46 = 461835;
{
int __tmp_101 = disketteExtension__TargetObject;
int __tmp_102 = __cil_tmp46;
int __tmp_103 = fdcInfo;
int DeviceObject = __tmp_101;
int Ioctl = __tmp_102;
int Data = __tmp_103;
int ntStatus ;
int irp ;
int irpStack ;
int doneEvent = __VERIFIER_nondet_int() ;
doneEvent = __VERIFIER_nondet_int();
int ioStatus = __VERIFIER_nondet_int() ;
ioStatus = __VERIFIER_nondet_int();
int irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int irpStack__Parameters__DeviceIoControl__Type3InputBuffer ;
long __cil_tmp11 ;
{
int __tmp_104 = Ioctl;
int __tmp_105 = DeviceObject;
int __tmp_106 = 0;
int __tmp_107 = 0;
int __tmp_108 = 0;
int __tmp_109 = 0;
int __tmp_110 = 1;
int __tmp_111 = doneEvent;
int __tmp_112 = ioStatus;
int IoControlCode = __tmp_104;
int DeviceObject = __tmp_105;
int InputBuffer = __tmp_106;
int InputBufferLength = __tmp_107;
int OutputBuffer = __tmp_108;
int OutputBufferLength = __tmp_109;
int InternalDeviceIoControl = __tmp_110;
int Event = __tmp_111;
int IoStatusBlock = __tmp_112;
int malloc = __VERIFIER_nondet_int() ;
malloc = __VERIFIER_nondet_int();
customIrp = 1;
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
 __return_5758 = malloc;
goto label_5759;
}
else 
{
 __return_5759 = 0;
label_5759:; 
}
irp = __return_5759;
if (irp == 0)
{
 __return_6070 = -1073741670;
}
else 
{
irpStack = irp__Tail__Overlay__CurrentStackLocation - 1;
irpStack__Parameters__DeviceIoControl__Type3InputBuffer = Data;
{
int __tmp_113 = DeviceObject;
int __tmp_114 = irp;
int DeviceObject = __tmp_113;
int Irp = __tmp_114;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_5884;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_5884;
}
else 
{
returnVal2 = 259;
label_5884:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5988;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5970;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5970:; 
goto label_5988;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5988:; 
 __return_5989 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_5993 = returnVal2;
}
ntStatus = __return_5989;
goto label_5995;
ntStatus = __return_5993;
label_5995:; 
__cil_tmp11 = (long)ntStatus;
if (__cil_tmp11 == 259L)
{
{
int __tmp_115 = doneEvent;
int __tmp_116 = Suspended;
int __tmp_117 = KernelMode;
int __tmp_118 = 0;
int __tmp_119 = 0;
int Object = __tmp_115;
int WaitReason = __tmp_116;
int WaitMode = __tmp_117;
int Alertable = __tmp_118;
int Timeout = __tmp_119;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_6039;
}
else 
{
goto label_6016;
}
}
else 
{
label_6016:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_6039;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
 __return_6058 = 0;
goto label_6059;
}
else 
{
 __return_6059 = -1073741823;
label_6059:; 
}
goto label_6063;
}
else 
{
label_6039:; 
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
 __return_6060 = 0;
goto label_6061;
}
else 
{
 __return_6061 = -1073741823;
label_6061:; 
}
label_6063:; 
ntStatus = myStatus;
 __return_6068 = ntStatus;
}
ntStatus = __return_6068;
goto label_5724;
}
}
}
}
else 
{
 __return_6069 = ntStatus;
}
ntStatus = __return_6069;
goto label_5724;
}
}
}
}
}
else 
{
{
int __tmp_120 = DeviceObject;
int __tmp_121 = Irp;
int __tmp_122 = lcontext;
int DeviceObject = __tmp_120;
int Irp = __tmp_121;
int Context = __tmp_122;
{
int __tmp_123 = Context;
int __tmp_124 = 1;
int __tmp_125 = 0;
int Event = __tmp_123;
int Increment = __tmp_124;
int Wait = __tmp_125;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_5785 = l;
}
 __return_5788 = -1073741802;
}
compRetStatus1 = __return_5788;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_5815;
label_5815:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_5880;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_5880;
}
else 
{
returnVal2 = 259;
label_5880:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5984;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5966;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5966:; 
goto label_5984;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5984:; 
 __return_5991 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_5916:; 
 __return_5992 = returnVal2;
}
ntStatus = __return_5991;
goto label_5995;
ntStatus = __return_5992;
goto label_5995;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_5882;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_5882;
}
else 
{
returnVal2 = 259;
label_5882:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_5986;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_5968;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_5968:; 
goto label_5986;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_5986:; 
 __return_5990 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_5916;
}
ntStatus = __return_5990;
goto label_5995;
}
}
}
}
}
}
}
}
ntStatus = __return_6070;
goto label_5724;
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
int __tmp_126 = DeviceObject;
int __tmp_127 = Irp;
int __tmp_128 = lcontext;
int DeviceObject = __tmp_126;
int Irp = __tmp_127;
int Context = __tmp_128;
{
int __tmp_129 = Context;
int __tmp_130 = 1;
int __tmp_131 = 0;
int Event = __tmp_129;
int Increment = __tmp_130;
int Wait = __tmp_131;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_4610 = l;
}
 __return_4613 = -1073741802;
}
compRetStatus1 = __return_4613;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_4640;
label_4640:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4705;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4705;
}
else 
{
returnVal2 = 259;
label_4705:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4809;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4791;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4791:; 
goto label_4809;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4809:; 
 __return_4816 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4741:; 
 __return_4817 = returnVal2;
}
ntStatus = __return_4816;
goto label_4820;
ntStatus = __return_4817;
goto label_4820;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4707;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4707;
}
else 
{
returnVal2 = 259;
label_4707:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4811;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4793;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4793:; 
goto label_4811;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4811:; 
 __return_4815 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4741;
}
ntStatus = __return_4815;
goto label_4820;
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
if (irpSp__MinorFunction == 5)
{
goto label_3232;
}
else 
{
if (irpSp__MinorFunction == 1)
{
label_3232:; 
if (disketteExtension__IsStarted == 0)
{
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_1 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_1;
int __CPAchecker_TMP_2 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_2;
{
int __tmp_132 = disketteExtension__TargetObject;
int __tmp_133 = Irp;
int DeviceObject = __tmp_132;
int Irp = __tmp_133;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4122;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4122;
}
else 
{
returnVal2 = 259;
label_4122:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4226;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4208;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4208:; 
goto label_4226;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4226:; 
 __return_4227 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_4231 = returnVal2;
}
ntStatus = __return_4227;
goto label_4233;
ntStatus = __return_4231;
label_4233:; 
 __return_4480 = ntStatus;
}
status = __return_4480;
goto label_6618;
}
}
}
}
else 
{
{
int __tmp_134 = DeviceObject;
int __tmp_135 = Irp;
int __tmp_136 = lcontext;
int DeviceObject = __tmp_134;
int Irp = __tmp_135;
int Context = __tmp_136;
{
int __tmp_137 = Context;
int __tmp_138 = 1;
int __tmp_139 = 0;
int Event = __tmp_137;
int Increment = __tmp_138;
int Wait = __tmp_139;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_4023 = l;
}
 __return_4026 = -1073741802;
}
compRetStatus1 = __return_4026;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_4053;
label_4053:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4118;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4118;
}
else 
{
returnVal2 = 259;
label_4118:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4222;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4204;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4204:; 
goto label_4222;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4222:; 
 __return_4229 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4154:; 
 __return_4230 = returnVal2;
}
ntStatus = __return_4229;
goto label_4233;
ntStatus = __return_4230;
goto label_4233;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4120;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4120;
}
else 
{
returnVal2 = 259;
label_4120:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4224;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4206;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4206:; 
goto label_4224;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4224:; 
 __return_4228 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4154;
}
ntStatus = __return_4228;
goto label_4233;
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
int __CPAchecker_TMP_1 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_1;
int __CPAchecker_TMP_2 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_2;
{
int __tmp_140 = disketteExtension__TargetObject;
int __tmp_141 = Irp;
int DeviceObject = __tmp_140;
int Irp = __tmp_141;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4360;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4360;
}
else 
{
returnVal2 = 259;
label_4360:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4464;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4446;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4446:; 
goto label_4464;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4464:; 
 __return_4465 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_4469 = returnVal2;
}
ntStatus = __return_4465;
goto label_4233;
ntStatus = __return_4469;
goto label_4233;
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
int __tmp_144 = lcontext;
int DeviceObject = __tmp_142;
int Irp = __tmp_143;
int Context = __tmp_144;
{
int __tmp_145 = Context;
int __tmp_146 = 1;
int __tmp_147 = 0;
int Event = __tmp_145;
int Increment = __tmp_146;
int Wait = __tmp_147;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_4261 = l;
}
 __return_4264 = -1073741802;
}
compRetStatus1 = __return_4264;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_4291;
label_4291:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4356;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4356;
}
else 
{
returnVal2 = 259;
label_4356:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4460;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4442;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4442:; 
goto label_4460;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4460:; 
 __return_4467 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_4392:; 
 __return_4468 = returnVal2;
}
ntStatus = __return_4467;
goto label_4233;
ntStatus = __return_4468;
goto label_4233;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_4358;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_4358;
}
else 
{
returnVal2 = 259;
label_4358:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_4462;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_4444;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_4444:; 
goto label_4462;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_4462:; 
 __return_4466 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_4392;
}
ntStatus = __return_4466;
goto label_4233;
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
disketteExtension__HoldNewRequests = 1;
{
int __tmp_148 = Irp;
int __tmp_149 = disketteExtension;
int Irp = __tmp_148;
int DisketteExtension = __tmp_149;
int status ;
int threadHandle = __VERIFIER_nondet_int() ;
threadHandle = __VERIFIER_nondet_int();
int DisketteExtension__PoweringDown = __VERIFIER_nondet_int() ;
DisketteExtension__PoweringDown = __VERIFIER_nondet_int();
int DisketteExtension__ThreadReferenceCount = __VERIFIER_nondet_int() ;
DisketteExtension__ThreadReferenceCount = __VERIFIER_nondet_int();
int DisketteExtension__FloppyThread = __VERIFIER_nondet_int() ;
DisketteExtension__FloppyThread = __VERIFIER_nondet_int();
int Irp__IoStatus__Status ;
int Irp__IoStatus__Information ;
int Irp__Tail__Overlay__CurrentStackLocation__Control ;
int ObjAttributes = __VERIFIER_nondet_int() ;
ObjAttributes = __VERIFIER_nondet_int();
int __cil_tmp12 ;
int __cil_tmp13 ;
if (DisketteExtension__PoweringDown == 1)
{
myStatus = -1073741101;
Irp__IoStatus__Status = -1073741101;
Irp__IoStatus__Information = 0;
 __return_3367 = -1073741101;
goto label_3368;
}
else 
{
int __CPAchecker_TMP_0 = DisketteExtension__ThreadReferenceCount;
DisketteExtension__ThreadReferenceCount = DisketteExtension__ThreadReferenceCount + 1;
__CPAchecker_TMP_0;
if (DisketteExtension__ThreadReferenceCount == 0)
{
int __CPAchecker_TMP_1 = DisketteExtension__ThreadReferenceCount;
DisketteExtension__ThreadReferenceCount = DisketteExtension__ThreadReferenceCount + 1;
__CPAchecker_TMP_1;
int __CPAchecker_TMP_2 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount + 1;
__CPAchecker_TMP_2;
{
int __tmp_150 = threadHandle;
int __tmp_151 = 0;
int __tmp_152 = ObjAttributes;
int __tmp_153 = 0;
int __tmp_154 = 0;
int __tmp_155 = FloppyThread;
int __tmp_156 = DisketteExtension;
int ThreadHandle = __tmp_150;
int DesiredAccess = __tmp_151;
int ObjectAttributes = __tmp_152;
int ProcessHandle = __tmp_153;
int ClientId = __tmp_154;
int StartRoutine = __tmp_155;
int StartContext = __tmp_156;
int tmp_ndt_11;
tmp_ndt_11 = __VERIFIER_nondet_int();
if (tmp_ndt_11 == 0)
{
 __return_3283 = 0;
goto label_3284;
}
else 
{
 __return_3284 = -1073741823;
label_3284:; 
}
status = __return_3284;
if (status < 0)
{
DisketteExtension__ThreadReferenceCount = -1;
int __CPAchecker_TMP_3 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount - 1;
__CPAchecker_TMP_3;
 __return_3363 = status;
}
else 
{
{
int __tmp_157 = threadHandle;
int __tmp_158 = 1048576;
int __tmp_159 = 0;
int __tmp_160 = KernelMode;
int __tmp_161 = DisketteExtension__FloppyThread;
int __tmp_162 = 0;
int Handle = __tmp_157;
int DesiredAccess = __tmp_158;
int ObjectType = __tmp_159;
int AccessMode = __tmp_160;
int Object = __tmp_161;
int HandleInformation = __tmp_162;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_3301 = 0;
goto label_3302;
}
else 
{
 __return_3302 = -1073741823;
label_3302:; 
}
status = __return_3302;
{
int __tmp_163 = threadHandle;
int Handle = __tmp_163;
int tmp_ndt_12;
tmp_ndt_12 = __VERIFIER_nondet_int();
if (tmp_ndt_12 == 0)
{
 __return_3317 = 0;
goto label_3318;
}
else 
{
 __return_3318 = -1073741823;
label_3318:; 
}
if (status < 0)
{
 __return_3356 = status;
goto label_3357;
}
else 
{
flag = pended;
if (pended == 0)
{
pended = 1;
 __return_3357 = 259;
label_3357:; 
}
else 
{
{
__VERIFIER_error();
}
 __return_3354 = 259;
}
ntStatus = __return_3357;
goto label_3370;
ntStatus = __return_3354;
goto label_3370;
}
}
}
}
ntStatus = __return_3363;
goto label_3370;
}
}
else 
{
flag = pended;
if (pended == 0)
{
pended = 1;
 __return_3368 = 259;
label_3368:; 
}
else 
{
{
__VERIFIER_error();
}
 __return_3355 = 259;
}
ntStatus = __return_3368;
label_3370:; 
__cil_tmp29 = (long)ntStatus;
if (__cil_tmp29 == 259L)
{
{
int __tmp_164 = disketteExtension__FloppyThread;
int __tmp_165 = Executive;
int __tmp_166 = KernelMode;
int __tmp_167 = 0;
int __tmp_168 = 0;
int Object = __tmp_164;
int WaitReason = __tmp_165;
int WaitMode = __tmp_166;
int Alertable = __tmp_167;
int Timeout = __tmp_168;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_3442;
}
else 
{
goto label_3419;
}
}
else 
{
label_3419:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_3442;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
 __return_3461 = 0;
goto label_3462;
}
else 
{
 __return_3462 = -1073741823;
label_3462:; 
}
goto label_3466;
}
else 
{
label_3442:; 
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
 __return_3463 = 0;
goto label_3464;
}
else 
{
 __return_3464 = -1073741823;
label_3464:; 
}
label_3466:; 
disketteExtension__FloppyThread = 0;
Irp__IoStatus__Status = 0;
myStatus = 0;
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_3 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_3;
int __CPAchecker_TMP_4 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_4;
{
int __tmp_169 = disketteExtension__TargetObject;
int __tmp_170 = Irp;
int DeviceObject = __tmp_169;
int Irp = __tmp_170;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_3617;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_3617;
}
else 
{
returnVal2 = 259;
label_3617:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3721;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3703;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3703:; 
goto label_3721;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3721:; 
 __return_3722 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_3726 = returnVal2;
}
ntStatus = __return_3722;
goto label_3728;
ntStatus = __return_3726;
label_3728:; 
int __CPAchecker_TMP_14 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount - 1;
__CPAchecker_TMP_14;
 __return_6609 = ntStatus;
}
status = __return_6609;
goto label_6618;
}
}
}
}
else 
{
{
int __tmp_171 = DeviceObject;
int __tmp_172 = Irp;
int __tmp_173 = lcontext;
int DeviceObject = __tmp_171;
int Irp = __tmp_172;
int Context = __tmp_173;
{
int __tmp_174 = Context;
int __tmp_175 = 1;
int __tmp_176 = 0;
int Event = __tmp_174;
int Increment = __tmp_175;
int Wait = __tmp_176;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_3518 = l;
}
 __return_3521 = -1073741802;
}
compRetStatus1 = __return_3521;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_3548;
label_3548:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_3613;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_3613;
}
else 
{
returnVal2 = 259;
label_3613:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3717;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3699;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3699:; 
goto label_3717;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3717:; 
 __return_3724 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3649:; 
 __return_3725 = returnVal2;
}
ntStatus = __return_3724;
goto label_3728;
ntStatus = __return_3725;
goto label_3728;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_3615;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_3615;
}
else 
{
returnVal2 = 259;
label_3615:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3719;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3701;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3701:; 
goto label_3719;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3719:; 
 __return_3723 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3649;
}
ntStatus = __return_3723;
goto label_3728;
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
int __CPAchecker_TMP_3 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_3;
int __CPAchecker_TMP_4 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_4;
{
int __tmp_177 = disketteExtension__TargetObject;
int __tmp_178 = Irp;
int DeviceObject = __tmp_177;
int Irp = __tmp_178;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_3855;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_3855;
}
else 
{
returnVal2 = 259;
label_3855:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3959;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3941;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3941:; 
goto label_3959;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3959:; 
 __return_3960 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_3964 = returnVal2;
}
ntStatus = __return_3960;
goto label_3728;
ntStatus = __return_3964;
goto label_3728;
}
}
}
}
}
else 
{
{
int __tmp_179 = DeviceObject;
int __tmp_180 = Irp;
int __tmp_181 = lcontext;
int DeviceObject = __tmp_179;
int Irp = __tmp_180;
int Context = __tmp_181;
{
int __tmp_182 = Context;
int __tmp_183 = 1;
int __tmp_184 = 0;
int Event = __tmp_182;
int Increment = __tmp_183;
int Wait = __tmp_184;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_3756 = l;
}
 __return_3759 = -1073741802;
}
compRetStatus1 = __return_3759;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_3786;
label_3786:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_3851;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_3851;
}
else 
{
returnVal2 = 259;
label_3851:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3955;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3937;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3937:; 
goto label_3955;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3955:; 
 __return_3962 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3887:; 
 __return_3963 = returnVal2;
}
ntStatus = __return_3962;
goto label_3728;
ntStatus = __return_3963;
goto label_3728;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_3853;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_3853;
}
else 
{
returnVal2 = 259;
label_3853:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3957;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3939;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3939:; 
goto label_3957;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3957:; 
 __return_3961 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3887;
}
ntStatus = __return_3961;
goto label_3728;
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
ntStatus = -1073741823;
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
Irp__IoStatus__Information = 0;
{
int __tmp_185 = Irp;
int __tmp_186 = 0;
int Irp = __tmp_185;
int PriorityBoost = __tmp_186;
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
goto label_3406;
label_3406:; 
int __CPAchecker_TMP_14 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount - 1;
__CPAchecker_TMP_14;
 __return_6608 = ntStatus;
}
status = __return_6608;
goto label_6618;
}
ntStatus = __return_3355;
goto label_3370;
}
}
}
}
}
else 
{
if (irpSp__MinorFunction == 6)
{
goto label_1845;
}
else 
{
if (irpSp__MinorFunction == 3)
{
label_1845:; 
if (disketteExtension__IsStarted == 0)
{
Irp__IoStatus__Status = 0;
myStatus = 0;
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_5 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_5;
int __CPAchecker_TMP_6 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_6;
{
int __tmp_187 = disketteExtension__TargetObject;
int __tmp_188 = Irp;
int DeviceObject = __tmp_187;
int Irp = __tmp_188;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2869;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2869;
}
else 
{
returnVal2 = 259;
label_2869:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2973;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2955;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2955:; 
goto label_2973;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2973:; 
 __return_2974 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_2978 = returnVal2;
}
ntStatus = __return_2974;
goto label_2980;
ntStatus = __return_2978;
label_2980:; 
int __CPAchecker_TMP_14 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount - 1;
__CPAchecker_TMP_14;
 __return_6611 = ntStatus;
}
status = __return_6611;
goto label_6618;
}
}
}
}
else 
{
{
int __tmp_189 = DeviceObject;
int __tmp_190 = Irp;
int __tmp_191 = lcontext;
int DeviceObject = __tmp_189;
int Irp = __tmp_190;
int Context = __tmp_191;
{
int __tmp_192 = Context;
int __tmp_193 = 1;
int __tmp_194 = 0;
int Event = __tmp_192;
int Increment = __tmp_193;
int Wait = __tmp_194;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2770 = l;
}
 __return_2773 = -1073741802;
}
compRetStatus1 = __return_2773;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_2800;
label_2800:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2865;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2865;
}
else 
{
returnVal2 = 259;
label_2865:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2969;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2951;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2951:; 
goto label_2969;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2969:; 
 __return_2976 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2901:; 
 __return_2977 = returnVal2;
}
ntStatus = __return_2976;
goto label_2980;
ntStatus = __return_2977;
goto label_2980;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2867;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2867;
}
else 
{
returnVal2 = 259;
label_2867:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2971;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2953;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2953:; 
goto label_2971;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2971:; 
 __return_2975 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2901;
}
ntStatus = __return_2975;
goto label_2980;
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
int __CPAchecker_TMP_5 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_5;
int __CPAchecker_TMP_6 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_6;
{
int __tmp_195 = disketteExtension__TargetObject;
int __tmp_196 = Irp;
int DeviceObject = __tmp_195;
int Irp = __tmp_196;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_3107;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_3107;
}
else 
{
returnVal2 = 259;
label_3107:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3211;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3193;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3193:; 
goto label_3211;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3211:; 
 __return_3212 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_3216 = returnVal2;
}
ntStatus = __return_3212;
goto label_2980;
ntStatus = __return_3216;
goto label_2980;
}
}
}
}
}
else 
{
{
int __tmp_197 = DeviceObject;
int __tmp_198 = Irp;
int __tmp_199 = lcontext;
int DeviceObject = __tmp_197;
int Irp = __tmp_198;
int Context = __tmp_199;
{
int __tmp_200 = Context;
int __tmp_201 = 1;
int __tmp_202 = 0;
int Event = __tmp_200;
int Increment = __tmp_201;
int Wait = __tmp_202;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_3008 = l;
}
 __return_3011 = -1073741802;
}
compRetStatus1 = __return_3011;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_3038;
label_3038:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_3103;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_3103;
}
else 
{
returnVal2 = 259;
label_3103:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3207;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3189;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3189:; 
goto label_3207;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3207:; 
 __return_3214 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_3139:; 
 __return_3215 = returnVal2;
}
ntStatus = __return_3214;
goto label_2980;
ntStatus = __return_3215;
goto label_2980;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_3105;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_3105;
}
else 
{
returnVal2 = 259;
label_3105:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_3209;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_3191;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_3191:; 
goto label_3209;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_3209:; 
 __return_3213 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_3139;
}
ntStatus = __return_3213;
goto label_2980;
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
Irp__IoStatus__Status = 0;
myStatus = 0;
irpSp___0 = Irp__Tail__Overlay__CurrentStackLocation;
nextIrpSp = Irp__Tail__Overlay__CurrentStackLocation - 1;
nextIrpSp__Control = 0;
flag = s - NP;
if (s != NP)
{
{
__VERIFIER_error();
}
irpSp___1 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = doneEvent;
irpSp__Control = 224;
{
int __tmp_203 = disketteExtension__TargetObject;
int __tmp_204 = Irp;
int DeviceObject = __tmp_203;
int Irp = __tmp_204;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2484;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2484;
}
else 
{
returnVal2 = 259;
label_2484:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2588;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2570;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2570:; 
goto label_2588;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2588:; 
 __return_2589 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_2593 = returnVal2;
}
ntStatus = __return_2589;
goto label_2119;
ntStatus = __return_2593;
goto label_2119;
}
}
}
}
}
else 
{
{
int __tmp_205 = DeviceObject;
int __tmp_206 = Irp;
int __tmp_207 = lcontext;
int DeviceObject = __tmp_205;
int Irp = __tmp_206;
int Context = __tmp_207;
{
int __tmp_208 = Context;
int __tmp_209 = 1;
int __tmp_210 = 0;
int Event = __tmp_208;
int Increment = __tmp_209;
int Wait = __tmp_210;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2385 = l;
}
 __return_2388 = -1073741802;
}
compRetStatus1 = __return_2388;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_2415;
label_2415:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2480;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2480;
}
else 
{
returnVal2 = 259;
label_2480:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2584;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2566;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2566:; 
goto label_2584;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2584:; 
 __return_2591 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2516:; 
 __return_2592 = returnVal2;
}
ntStatus = __return_2591;
goto label_2119;
ntStatus = __return_2592;
goto label_2119;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2482;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2482;
}
else 
{
returnVal2 = 259;
label_2482:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2586;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2568;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2568:; 
goto label_2586;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2586:; 
 __return_2590 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2516;
}
ntStatus = __return_2590;
goto label_2119;
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
irpSp___1 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = doneEvent;
irpSp__Control = 224;
{
int __tmp_211 = disketteExtension__TargetObject;
int __tmp_212 = Irp;
int DeviceObject = __tmp_211;
int Irp = __tmp_212;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2246;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2246;
}
else 
{
returnVal2 = 259;
label_2246:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2350;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2332;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2332:; 
goto label_2350;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2350:; 
 __return_2351 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_2355 = returnVal2;
}
ntStatus = __return_2351;
goto label_2119;
ntStatus = __return_2355;
goto label_2119;
}
}
}
}
}
else 
{
{
int __tmp_213 = DeviceObject;
int __tmp_214 = Irp;
int __tmp_215 = lcontext;
int DeviceObject = __tmp_213;
int Irp = __tmp_214;
int Context = __tmp_215;
{
int __tmp_216 = Context;
int __tmp_217 = 1;
int __tmp_218 = 0;
int Event = __tmp_216;
int Increment = __tmp_217;
int Wait = __tmp_218;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_2147 = l;
}
 __return_2150 = -1073741802;
}
compRetStatus1 = __return_2150;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_2177;
label_2177:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2242;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2242;
}
else 
{
returnVal2 = 259;
label_2242:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2346;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2328;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2328:; 
goto label_2346;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2346:; 
 __return_2353 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2278:; 
 __return_2354 = returnVal2;
}
ntStatus = __return_2353;
goto label_2119;
ntStatus = __return_2354;
goto label_2119;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2244;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2244;
}
else 
{
returnVal2 = 259;
label_2244:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2348;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2330;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2330:; 
goto label_2348;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2348:; 
 __return_2352 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2278;
}
ntStatus = __return_2352;
goto label_2119;
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
irpSp___1 = Irp__Tail__Overlay__CurrentStackLocation - 1;
irpSp__Context = doneEvent;
irpSp__Control = 224;
{
int __tmp_219 = disketteExtension__TargetObject;
int __tmp_220 = Irp;
int DeviceObject = __tmp_219;
int Irp = __tmp_220;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2008;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2008;
}
else 
{
returnVal2 = 259;
label_2008:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2112;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2094;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2094:; 
goto label_2112;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2112:; 
 __return_2113 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_2117 = returnVal2;
}
ntStatus = __return_2113;
goto label_2119;
ntStatus = __return_2117;
label_2119:; 
__cil_tmp30 = (long)ntStatus;
if (__cil_tmp30 == 259L)
{
{
int __tmp_221 = doneEvent;
int __tmp_222 = Executive;
int __tmp_223 = KernelMode;
int __tmp_224 = 0;
int __tmp_225 = 0;
int Object = __tmp_221;
int WaitReason = __tmp_222;
int WaitMode = __tmp_223;
int Alertable = __tmp_224;
int Timeout = __tmp_225;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_2639;
}
else 
{
goto label_2616;
}
}
else 
{
label_2616:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_2639;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
 __return_2658 = 0;
goto label_2659;
}
else 
{
 __return_2659 = -1073741823;
label_2659:; 
}
goto label_2663;
}
else 
{
label_2639:; 
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 0)
{
 __return_2660 = 0;
goto label_2661;
}
else 
{
 __return_2661 = -1073741823;
label_2661:; 
}
label_2663:; 
ntStatus = myStatus;
disketteExtension__HoldNewRequests = 0;
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
Irp__IoStatus__Information = 0;
{
int __tmp_226 = Irp;
int __tmp_227 = 0;
int Irp = __tmp_226;
int PriorityBoost = __tmp_227;
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
goto label_2696;
label_2696:; 
int __CPAchecker_TMP_14 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount - 1;
__CPAchecker_TMP_14;
 __return_6610 = ntStatus;
}
status = __return_6610;
goto label_6618;
}
}
}
}
}
else 
{
disketteExtension__HoldNewRequests = 0;
Irp__IoStatus__Status = ntStatus;
myStatus = ntStatus;
Irp__IoStatus__Information = 0;
{
int __tmp_228 = Irp;
int __tmp_229 = 0;
int Irp = __tmp_228;
int PriorityBoost = __tmp_229;
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
goto label_2696;
goto label_2696;
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
int __tmp_230 = DeviceObject;
int __tmp_231 = Irp;
int __tmp_232 = lcontext;
int DeviceObject = __tmp_230;
int Irp = __tmp_231;
int Context = __tmp_232;
{
int __tmp_233 = Context;
int __tmp_234 = 1;
int __tmp_235 = 0;
int Event = __tmp_233;
int Increment = __tmp_234;
int Wait = __tmp_235;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1909 = l;
}
 __return_1912 = -1073741802;
}
compRetStatus1 = __return_1912;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_1939;
label_1939:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2004;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2004;
}
else 
{
returnVal2 = 259;
label_2004:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2108;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2090;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2090:; 
goto label_2108;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2108:; 
 __return_2115 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_2040:; 
 __return_2116 = returnVal2;
}
ntStatus = __return_2115;
goto label_2119;
ntStatus = __return_2116;
goto label_2119;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_2006;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_2006;
}
else 
{
returnVal2 = 259;
label_2006:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_2110;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_2092;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_2092:; 
goto label_2110;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_2110:; 
 __return_2114 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_2040;
}
ntStatus = __return_2114;
goto label_2119;
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
if (irpSp__MinorFunction == 4)
{
disketteExtension__IsStarted = 0;
Irp__IoStatus__Status = 0;
myStatus = 0;
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_7 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_7;
int __CPAchecker_TMP_8 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_8;
{
int __tmp_236 = disketteExtension__TargetObject;
int __tmp_237 = Irp;
int DeviceObject = __tmp_236;
int Irp = __tmp_237;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_1484;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_1484;
}
else 
{
returnVal2 = 259;
label_1484:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1588;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1570;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1570:; 
goto label_1588;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1588:; 
 __return_1589 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_1593 = returnVal2;
}
ntStatus = __return_1589;
goto label_1595;
ntStatus = __return_1593;
label_1595:; 
int __CPAchecker_TMP_14 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount - 1;
__CPAchecker_TMP_14;
 __return_6612 = ntStatus;
}
status = __return_6612;
goto label_6618;
}
}
}
}
else 
{
{
int __tmp_238 = DeviceObject;
int __tmp_239 = Irp;
int __tmp_240 = lcontext;
int DeviceObject = __tmp_238;
int Irp = __tmp_239;
int Context = __tmp_240;
{
int __tmp_241 = Context;
int __tmp_242 = 1;
int __tmp_243 = 0;
int Event = __tmp_241;
int Increment = __tmp_242;
int Wait = __tmp_243;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1385 = l;
}
 __return_1388 = -1073741802;
}
compRetStatus1 = __return_1388;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_1415;
label_1415:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_1480;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_1480;
}
else 
{
returnVal2 = 259;
label_1480:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1584;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1566;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1566:; 
goto label_1584;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1584:; 
 __return_1591 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1516:; 
 __return_1592 = returnVal2;
}
ntStatus = __return_1591;
goto label_1595;
ntStatus = __return_1592;
goto label_1595;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_1482;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_1482;
}
else 
{
returnVal2 = 259;
label_1482:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1586;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1568;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1568:; 
goto label_1586;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1586:; 
 __return_1590 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1516;
}
ntStatus = __return_1590;
goto label_1595;
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
int __CPAchecker_TMP_7 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_7;
int __CPAchecker_TMP_8 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_8;
{
int __tmp_244 = disketteExtension__TargetObject;
int __tmp_245 = Irp;
int DeviceObject = __tmp_244;
int Irp = __tmp_245;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_1722;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_1722;
}
else 
{
returnVal2 = 259;
label_1722:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1826;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1808;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1808:; 
goto label_1826;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1826:; 
 __return_1827 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_1831 = returnVal2;
}
ntStatus = __return_1827;
goto label_1595;
ntStatus = __return_1831;
goto label_1595;
}
}
}
}
}
else 
{
{
int __tmp_246 = DeviceObject;
int __tmp_247 = Irp;
int __tmp_248 = lcontext;
int DeviceObject = __tmp_246;
int Irp = __tmp_247;
int Context = __tmp_248;
{
int __tmp_249 = Context;
int __tmp_250 = 1;
int __tmp_251 = 0;
int Event = __tmp_249;
int Increment = __tmp_250;
int Wait = __tmp_251;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1623 = l;
}
 __return_1626 = -1073741802;
}
compRetStatus1 = __return_1626;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_1653;
label_1653:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_1718;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_1718;
}
else 
{
returnVal2 = 259;
label_1718:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1822;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1804;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1804:; 
goto label_1822;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1822:; 
 __return_1829 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1754:; 
 __return_1830 = returnVal2;
}
ntStatus = __return_1829;
goto label_1595;
ntStatus = __return_1830;
goto label_1595;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_1720;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_1720;
}
else 
{
returnVal2 = 259;
label_1720:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1824;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1806;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1806:; 
goto label_1824;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1824:; 
 __return_1828 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1754;
}
ntStatus = __return_1828;
goto label_1595;
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
if (irpSp__MinorFunction == 2)
{
disketteExtension__HoldNewRequests = 0;
disketteExtension__IsStarted = 0;
disketteExtension__IsRemoved = 1;
flag = s - NP;
if (s == NP)
{
s = SKIP1;
int __CPAchecker_TMP_9 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_9;
int __CPAchecker_TMP_10 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_10;
Irp__IoStatus__Status = 0;
myStatus = 0;
{
int __tmp_252 = disketteExtension__TargetObject;
int __tmp_253 = Irp;
int DeviceObject = __tmp_252;
int Irp = __tmp_253;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_909;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_909;
}
else 
{
returnVal2 = 259;
label_909:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1013;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_995;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_995:; 
goto label_1013;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1013:; 
 __return_1014 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_1018 = returnVal2;
}
ntStatus = __return_1014;
goto label_1020;
ntStatus = __return_1018;
label_1020:; 
if (disketteExtension__InterfaceString__Buffer != 0)
{
{
int __tmp_254 = disketteExtension__InterfaceString;
int __tmp_255 = 0;
int SymbolicLinkName = __tmp_254;
int Enable = __tmp_255;
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 0)
{
 __return_1281 = 0;
goto label_1282;
}
else 
{
 __return_1282 = -1073741823;
label_1282:; 
}
if (disketteExtension__ArcName__Length != 0)
{
{
int __tmp_256 = disketteExtension__ArcName;
int SymbolicLinkName = __tmp_256;
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
 __return_1318 = 0;
goto label_1319;
}
else 
{
 __return_1319 = -1073741823;
label_1319:; 
}
goto label_1305;
}
}
else 
{
int __CPAchecker_TMP_11 = IoGetConfigurationInformation__FloppyCount;
IoGetConfigurationInformation__FloppyCount = IoGetConfigurationInformation__FloppyCount - 1;
__CPAchecker_TMP_11;
int __CPAchecker_TMP_14 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount - 1;
__CPAchecker_TMP_14;
 __return_6615 = ntStatus;
}
status = __return_6615;
goto label_6618;
}
}
else 
{
if (disketteExtension__ArcName__Length != 0)
{
{
int __tmp_257 = disketteExtension__ArcName;
int SymbolicLinkName = __tmp_257;
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
 __return_1302 = 0;
goto label_1303;
}
else 
{
 __return_1303 = -1073741823;
label_1303:; 
}
label_1305:; 
int __CPAchecker_TMP_11 = IoGetConfigurationInformation__FloppyCount;
IoGetConfigurationInformation__FloppyCount = IoGetConfigurationInformation__FloppyCount - 1;
__CPAchecker_TMP_11;
int __CPAchecker_TMP_14 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount - 1;
__CPAchecker_TMP_14;
 __return_6613 = ntStatus;
}
status = __return_6613;
goto label_6618;
}
else 
{
int __CPAchecker_TMP_11 = IoGetConfigurationInformation__FloppyCount;
IoGetConfigurationInformation__FloppyCount = IoGetConfigurationInformation__FloppyCount - 1;
__CPAchecker_TMP_11;
int __CPAchecker_TMP_14 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount - 1;
__CPAchecker_TMP_14;
 __return_6614 = ntStatus;
}
status = __return_6614;
goto label_6618;
}
}
}
}
}
}
else 
{
{
int __tmp_258 = DeviceObject;
int __tmp_259 = Irp;
int __tmp_260 = lcontext;
int DeviceObject = __tmp_258;
int Irp = __tmp_259;
int Context = __tmp_260;
{
int __tmp_261 = Context;
int __tmp_262 = 1;
int __tmp_263 = 0;
int Event = __tmp_261;
int Increment = __tmp_262;
int Wait = __tmp_263;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_810 = l;
}
 __return_813 = -1073741802;
}
compRetStatus1 = __return_813;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_840;
label_840:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_905;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_905;
}
else 
{
returnVal2 = 259;
label_905:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1009;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_991;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_991:; 
goto label_1009;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1009:; 
 __return_1016 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_941:; 
 __return_1017 = returnVal2;
}
ntStatus = __return_1016;
goto label_1020;
ntStatus = __return_1017;
goto label_1020;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_907;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_907;
}
else 
{
returnVal2 = 259;
label_907:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1011;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_993;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_993:; 
goto label_1011;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1011:; 
 __return_1015 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_941;
}
ntStatus = __return_1015;
goto label_1020;
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
int __CPAchecker_TMP_9 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_9;
int __CPAchecker_TMP_10 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_10;
Irp__IoStatus__Status = 0;
myStatus = 0;
{
int __tmp_264 = disketteExtension__TargetObject;
int __tmp_265 = Irp;
int DeviceObject = __tmp_264;
int Irp = __tmp_265;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_1147;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_1147;
}
else 
{
returnVal2 = 259;
label_1147:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1251;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1233;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1233:; 
goto label_1251;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1251:; 
 __return_1252 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_1256 = returnVal2;
}
ntStatus = __return_1252;
goto label_1020;
ntStatus = __return_1256;
goto label_1020;
}
}
}
}
}
else 
{
{
int __tmp_266 = DeviceObject;
int __tmp_267 = Irp;
int __tmp_268 = lcontext;
int DeviceObject = __tmp_266;
int Irp = __tmp_267;
int Context = __tmp_268;
{
int __tmp_269 = Context;
int __tmp_270 = 1;
int __tmp_271 = 0;
int Event = __tmp_269;
int Increment = __tmp_270;
int Wait = __tmp_271;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_1048 = l;
}
 __return_1051 = -1073741802;
}
compRetStatus1 = __return_1051;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_1078;
label_1078:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_1143;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_1143;
}
else 
{
returnVal2 = 259;
label_1143:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1247;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1229;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1229:; 
goto label_1247;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1247:; 
 __return_1254 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_1179:; 
 __return_1255 = returnVal2;
}
ntStatus = __return_1254;
goto label_1020;
ntStatus = __return_1255;
goto label_1020;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_1145;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_1145;
}
else 
{
returnVal2 = 259;
label_1145:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_1249;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_1231;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_1231:; 
goto label_1249;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_1249:; 
 __return_1253 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_1179;
}
ntStatus = __return_1253;
goto label_1020;
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
int __CPAchecker_TMP_12 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_12;
int __CPAchecker_TMP_13 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_13;
{
int __tmp_272 = disketteExtension__TargetObject;
int __tmp_273 = Irp;
int DeviceObject = __tmp_272;
int Irp = __tmp_273;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_397;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_397;
}
else 
{
returnVal2 = 259;
label_397:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_501;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_483;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_483:; 
goto label_501;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_501:; 
 __return_502 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_506 = returnVal2;
}
ntStatus = __return_502;
goto label_508;
ntStatus = __return_506;
label_508:; 
int __CPAchecker_TMP_14 = PagingReferenceCount;
PagingReferenceCount = PagingReferenceCount - 1;
__CPAchecker_TMP_14;
 __return_6616 = ntStatus;
}
status = __return_6616;
label_6618:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_6813;
}
else 
{
goto label_6655;
}
}
else 
{
label_6655:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_6813;
}
else 
{
goto label_6671;
}
}
else 
{
label_6671:; 
if (s != UNLOADED)
{
if (status != -1)
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
goto label_6788;
}
else 
{
goto label_6707;
}
}
else 
{
goto label_6707;
}
}
else 
{
label_6707:; 
if (pended == 1)
{
if (status != 259)
{
status = 0;
goto label_6776;
}
else 
{
label_6776:; 
goto label_6813;
}
}
else 
{
if (s == DC)
{
if (status == 259)
{
{
__VERIFIER_error();
}
goto label_6752;
}
else 
{
goto label_6813;
}
}
else 
{
if (status != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_6731;
}
else 
{
goto label_6813;
}
}
}
}
}
else 
{
goto label_6813;
}
}
else 
{
label_6813:; 
status = 0;
 __return_6828 = status;
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
int __tmp_274 = DeviceObject;
int __tmp_275 = Irp;
int __tmp_276 = lcontext;
int DeviceObject = __tmp_274;
int Irp = __tmp_275;
int Context = __tmp_276;
{
int __tmp_277 = Context;
int __tmp_278 = 1;
int __tmp_279 = 0;
int Event = __tmp_277;
int Increment = __tmp_278;
int Wait = __tmp_279;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_298 = l;
}
 __return_301 = -1073741802;
}
compRetStatus1 = __return_301;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_328;
label_328:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_393;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_393;
}
else 
{
returnVal2 = 259;
label_393:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_497;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_479;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_479:; 
goto label_497;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_497:; 
 __return_504 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_429:; 
 __return_505 = returnVal2;
}
ntStatus = __return_504;
goto label_508;
ntStatus = __return_505;
goto label_508;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_395;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_395;
}
else 
{
returnVal2 = 259;
label_395:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_499;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_481;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_481:; 
goto label_499;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_499:; 
 __return_503 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_429;
}
ntStatus = __return_503;
goto label_508;
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
int __CPAchecker_TMP_12 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_12;
int __CPAchecker_TMP_13 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_13;
{
int __tmp_280 = disketteExtension__TargetObject;
int __tmp_281 = Irp;
int DeviceObject = __tmp_280;
int Irp = __tmp_281;
int returnVal2 ;
int compRetStatus1 ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
if (compRegistered == 0)
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_635;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_635;
}
else 
{
returnVal2 = 259;
label_635:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_739;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_721;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_721:; 
goto label_739;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_739:; 
 __return_740 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
 __return_744 = returnVal2;
}
ntStatus = __return_740;
goto label_508;
ntStatus = __return_744;
goto label_508;
}
}
}
}
}
else 
{
{
int __tmp_282 = DeviceObject;
int __tmp_283 = Irp;
int __tmp_284 = lcontext;
int DeviceObject = __tmp_282;
int Irp = __tmp_283;
int Context = __tmp_284;
{
int __tmp_285 = Context;
int __tmp_286 = 1;
int __tmp_287 = 0;
int Event = __tmp_285;
int Increment = __tmp_286;
int Wait = __tmp_287;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_536 = l;
}
 __return_539 = -1073741802;
}
compRetStatus1 = __return_539;
__cil_tmp7 = (unsigned long)compRetStatus1;
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
goto label_566;
label_566:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_631;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_631;
}
else 
{
returnVal2 = 259;
label_631:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_735;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_717;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_717:; 
goto label_735;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_735:; 
 __return_742 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_667:; 
 __return_743 = returnVal2;
}
ntStatus = __return_742;
goto label_508;
ntStatus = __return_743;
goto label_508;
}
}
}
}
}
}
else 
{
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
returnVal2 = 0;
goto label_633;
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 1)
{
returnVal2 = -1073741823;
goto label_633;
}
else 
{
returnVal2 = 259;
label_633:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_737;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_719;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_719:; 
goto label_737;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_737:; 
 __return_741 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_667;
}
ntStatus = __return_741;
goto label_508;
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
Irp__IoStatus__Information = 0;
Irp__IoStatus__Status = -1073741738;
myStatus = -1073741738;
{
int __tmp_288 = Irp;
int __tmp_289 = 0;
int Irp = __tmp_288;
int PriorityBoost = __tmp_289;
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
goto label_233;
label_233:; 
 __return_236 = -1073741738;
}
status = __return_236;
goto label_6618;
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
else 
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_6815;
}
else 
{
goto label_6657;
}
}
else 
{
label_6657:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_6815;
}
else 
{
goto label_6669;
}
}
else 
{
label_6669:; 
if (s != UNLOADED)
{
if (status != -1)
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
label_6788:; 
status = 0;
 __return_6826 = status;
goto label_6822;
}
else 
{
goto label_6705;
}
}
else 
{
goto label_6705;
}
}
else 
{
label_6705:; 
if (pended == 1)
{
if (status != 259)
{
status = 0;
goto label_6774;
}
else 
{
label_6774:; 
goto label_6815;
}
}
else 
{
if (s == DC)
{
if (status == 259)
{
{
__VERIFIER_error();
}
label_6752:; 
status = 0;
 __return_6824 = status;
goto label_6822;
}
else 
{
goto label_6815;
}
}
else 
{
if (status != lowerDriverReturn)
{
{
__VERIFIER_error();
}
label_6731:; 
status = 0;
 __return_6822 = status;
label_6822:; 
return 1;
}
else 
{
goto label_6815;
}
}
}
}
}
else 
{
goto label_6815;
}
}
else 
{
label_6815:; 
status = 0;
 __return_6830 = status;
goto label_6822;
}
}
}
}
}
