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
int __return_90037;
int __return_89661;
int __return_89659;
int __return_89656;
int __return_89633;
int __return_89629;
int __return_89653;
int __return_89657;
int __return_89911;
int __return_89915;
int __return_89792;
int __return_89783;
int __return_89779;
int __return_89793;
int __return_89876;
int __return_89880;
int __return_89306;
int __return_89304;
int __return_89301;
int __return_89278;
int __return_89274;
int __return_89298;
int __return_89302;
int __return_89539;
int __return_89543;
int __return_89429;
int __return_89420;
int __return_89416;
int __return_89430;
int __return_89476;
int __return_89480;
int __return_89173;
int __return_89177;
int __return_89137;
int __return_89141;
int __return_89092;
int __return_89083;
int __return_89079;
int __return_89093;
int __return_88960;
int __return_88951;
int __return_88947;
int __return_88961;
int __return_88831;
int __return_88822;
int __return_88818;
int __return_88832;
int __return_89841;
int __return_89845;
int __return_88706;
int __return_88697;
int __return_88693;
int __return_88707;
int __return_88359;
int __return_88357;
int __return_88354;
int __return_88331;
int __return_88327;
int __return_88351;
int __return_88355;
int __return_88467;
int __return_88413;
int __return_88416;
int __return_88567;
int __return_88570;
int __return_89846;
int __return_87741;
int __return_87739;
int __return_87736;
int __return_87713;
int __return_87709;
int __return_87733;
int __return_87737;
int __return_87981;
int __return_87865;
int __return_87856;
int __return_87852;
int __return_87866;
int __return_87947;
int __return_87454;
int __return_87445;
int __return_87441;
int __return_87455;
int __return_87620;
int __return_87580;
int __return_87571;
int __return_87567;
int __return_87581;
int __return_88020;
int __return_87313;
int __return_87304;
int __return_87300;
int __return_87314;
int __return_87195;
int __return_87163;
int __return_86942;
int __return_86933;
int __return_86929;
int __return_86943;
int __return_87131;
int __return_87060;
int __return_87051;
int __return_87047;
int __return_87061;
int __return_87099;
int __return_86821;
int __return_86776;
int __return_86767;
int __return_86763;
int __return_86777;
int __return_86654;
int __return_86620;
int __return_86567;
int __return_86558;
int __return_86554;
int __return_86568;
int __return_86440;
int __return_86431;
int __return_86427;
int __return_86441;
int __return_86057;
int __return_86055;
int __return_86052;
int __return_86029;
int __return_86025;
int __return_86049;
int __return_86053;
int __return_86308;
int __return_86166;
int __return_86112;
int __return_86115;
int __return_86266;
int __return_86269;
int __return_86273;
int __return_85832;
int __return_85778;
int __return_85781;
int __return_85932;
int __return_85935;
int __return_87948;
int __return_85445;
int __return_85436;
int __return_85432;
int __return_85446;
int __return_85502;
int __return_85476;
int __return_85223;
int __return_85169;
int __return_85172;
int __return_85323;
int __return_85326;
int __return_85477;
int __return_84979;
int __return_84925;
int __return_84928;
int __return_85079;
int __return_85083;
int __return_84770;
int __return_84716;
int __return_84719;
int __return_84870;
int __return_84873;
int __return_85084;
int __return_84535;
int __return_84481;
int __return_84484;
int __return_84635;
int __return_84638;
int __return_89923;
int __return_84022;
int __return_83968;
int __return_83971;
int __return_84122;
int __return_83838;
int __return_83784;
int __return_83787;
int __return_83938;
int __return_84186;
int __return_84182;
int __return_84192;
int __return_84381;
int __return_84379;
int __return_84331;
int __return_84322;
int __return_84318;
int __return_84332;
int __return_84375;
int __return_84372;
int __return_84366;
int __return_84362;
int __return_84376;
int __return_84408;
int __return_83598;
int __return_83544;
int __return_83547;
int __return_83698;
int __return_83701;
int __return_83705;
int __return_82963;
int __return_82959;
int __return_83278;
int __return_83224;
int __return_83227;
int __return_83378;
int __return_83094;
int __return_83040;
int __return_83043;
int __return_83194;
int __return_83442;
int __return_83438;
int __return_83448;
int __return_83468;
int __return_83493;
int __return_82794;
int __return_82740;
int __return_82743;
int __return_82894;
int __return_82897;
int __return_83494;
int __return_82556;
int __return_82502;
int __return_82505;
int __return_82651;
int __return_82654;
int __return_90035;
int __return_90034;
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
goto label_82416;
}
else 
{
label_82416:; 
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
 __return_90037 = -1;
goto label_90035;
}
else 
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 2)
{
{
int __tmp_1 = devobj;
int __tmp_2 = pirp;
int DeviceObject = __tmp_1;
int Irp = __tmp_2;
int deviceExtension__Active = __VERIFIER_nondet_int() ;
deviceExtension__Active = __VERIFIER_nondet_int();
int status4 ;
if (deviceExtension__Active == 2)
{
{
int __tmp_3 = DeviceObject;
int __tmp_4 = Irp;
int DeviceObject = __tmp_3;
int Irp = __tmp_4;
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
int __tmp_5 = DeviceObject;
int DeviceObject = __tmp_5;
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
 __return_89661 = 0;
goto label_89657;
}
else 
{
if (currentBuffer == 0)
{
 __return_89659 = 0;
goto label_89657;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_89656 = 0;
goto label_89657;
}
else 
{
__cil_tmp10 = (long)status5;
if (__cil_tmp10 == 259L)
{
{
int __tmp_6 = event;
int __tmp_7 = Suspended;
int __tmp_8 = KernelMode;
int __tmp_9 = 0;
int __tmp_10 = 0;
int Object = __tmp_6;
int WaitReason = __tmp_7;
int WaitMode = __tmp_8;
int Alertable = __tmp_9;
int Timeout = __tmp_10;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_89597;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_89597;
}
else 
{
{
__VERIFIER_error();
}
goto label_89597;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_89597;
}
else 
{
label_89597:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_89633 = 0;
goto label_89629;
}
else 
{
 __return_89629 = -1073741823;
label_89629:; 
}
status5 = ioStatus__Status;
goto label_89575;
}
}
}
}
else 
{
label_89575:; 
if (status5 < 0)
{
 __return_89653 = 0;
goto label_89657;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_89646;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_89646:; 
 __return_89657 = returnValue;
label_89657:; 
}
tmp = __return_89657;
if (tmp == 0)
{
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength == 0)
{
if (lastSession == 0)
{
status6 = -1073741670;
Irp__IoStatus__Information = 0;
{
int __tmp_11 = status6;
int __tmp_12 = Irp;
int __tmp_13 = deviceExtension__TargetDeviceObject;
int status = __tmp_11;
int Irp = __tmp_12;
int deviceExtension__TargetDeviceObject = __tmp_13;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_14 = Irp;
int __tmp_15 = 0;
int Irp = __tmp_14;
int PriorityBoost = __tmp_15;
if (s == NP)
{
s = DC;
goto label_89904;
}
else 
{
{
__VERIFIER_error();
}
label_89904:; 
}
 __return_89911 = status;
}
tmp___0 = __return_89911;
 __return_89915 = tmp___0;
goto label_89846;
}
}
else 
{
srb__CdbLength = 10;
cdb__CDB10__OperationCode = 38;
srb__TimeOutValue = 10;
{
int __tmp_16 = deviceExtension;
int __tmp_17 = srb;
int __tmp_18 = lastSession;
int __tmp_19 = sizeof__READ_CAPACITY_DATA;
int Extension = __tmp_16;
int Srb = __tmp_17;
int Buffer = __tmp_18;
int BufferLength = __tmp_19;
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
goto label_89719;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_89719:; 
if (irp == 0)
{
 __return_89792 = -1073741670;
goto label_89793;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_20 = event;
int __tmp_21 = Executive;
int __tmp_22 = KernelMode;
int __tmp_23 = 0;
int __tmp_24 = 0;
int Object = __tmp_20;
int WaitReason = __tmp_21;
int WaitMode = __tmp_22;
int Alertable = __tmp_23;
int Timeout = __tmp_24;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_89747;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_89747;
}
else 
{
{
__VERIFIER_error();
}
goto label_89747;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_89747;
}
else 
{
label_89747:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_89783 = 0;
goto label_89779;
}
else 
{
 __return_89779 = -1073741823;
label_89779:; 
}
status1 = ioStatus__Status;
goto label_89725;
}
}
}
}
else 
{
label_89725:; 
 __return_89793 = status1;
label_89793:; 
}
status6 = __return_89793;
if (status6 < 0)
{
Irp__IoStatus__Information = 0;
{
int __tmp_25 = status6;
int __tmp_26 = Irp;
int __tmp_27 = deviceExtension__TargetDeviceObject;
int status = __tmp_25;
int Irp = __tmp_26;
int deviceExtension__TargetDeviceObject = __tmp_27;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_28 = Irp;
int __tmp_29 = 0;
int Irp = __tmp_28;
int PriorityBoost = __tmp_29;
if (s == NP)
{
s = DC;
goto label_89869;
}
else 
{
{
__VERIFIER_error();
}
label_89869:; 
}
 __return_89876 = status;
}
tmp___1 = __return_89876;
 __return_89880 = tmp___1;
goto label_89846;
}
}
else 
{
status6 = 0;
Irp__IoStatus__Information = bytesTransfered;
if (lastSession__LogicalBlockAddress == 0)
{
goto label_89670;
}
else 
{
cdaudioDataOut__FirstTrack = 1;
cdaudioDataOut__LastTrack = 2;
goto label_89670;
}
}
}
}
}
}
}
else 
{
status6 = -1073741789;
Irp__IoStatus__Information = 0;
goto label_89670;
}
}
else 
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_89670;
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
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength == 0)
{
{
int __tmp_30 = DeviceObject;
int DeviceObject = __tmp_30;
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
 __return_89306 = 0;
goto label_89302;
}
else 
{
if (currentBuffer == 0)
{
 __return_89304 = 0;
goto label_89302;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_89301 = 0;
goto label_89302;
}
else 
{
__cil_tmp10 = (long)status5;
if (__cil_tmp10 == 259L)
{
{
int __tmp_31 = event;
int __tmp_32 = Suspended;
int __tmp_33 = KernelMode;
int __tmp_34 = 0;
int __tmp_35 = 0;
int Object = __tmp_31;
int WaitReason = __tmp_32;
int WaitMode = __tmp_33;
int Alertable = __tmp_34;
int Timeout = __tmp_35;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_89242;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_89242;
}
else 
{
{
__VERIFIER_error();
}
goto label_89242;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_89242;
}
else 
{
label_89242:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_89278 = 0;
goto label_89274;
}
else 
{
 __return_89274 = -1073741823;
label_89274:; 
}
status5 = ioStatus__Status;
goto label_89220;
}
}
}
}
else 
{
label_89220:; 
if (status5 < 0)
{
 __return_89298 = 0;
goto label_89302;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_89291;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_89291:; 
 __return_89302 = returnValue;
label_89302:; 
}
tmp___2 = __return_89302;
if (tmp___2 == 0)
{
if (Toc == 0)
{
status6 = -1073741670;
Irp__IoStatus__Information = 0;
{
int __tmp_36 = status6;
int __tmp_37 = Irp;
int __tmp_38 = deviceExtension__TargetDeviceObject;
int status = __tmp_36;
int Irp = __tmp_37;
int deviceExtension__TargetDeviceObject = __tmp_38;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_39 = Irp;
int __tmp_40 = 0;
int Irp = __tmp_39;
int PriorityBoost = __tmp_40;
if (s == NP)
{
s = DC;
goto label_89532;
}
else 
{
{
__VERIFIER_error();
}
label_89532:; 
}
 __return_89539 = status;
}
tmp___3 = __return_89539;
 __return_89543 = tmp___3;
goto label_89846;
}
}
else 
{
srb__TimeOutValue = 10;
srb__CdbLength = 10;
{
int __tmp_41 = deviceExtension;
int __tmp_42 = srb;
int __tmp_43 = Toc;
int __tmp_44 = sizeof__CDROM_TOC;
int Extension = __tmp_41;
int Srb = __tmp_42;
int Buffer = __tmp_43;
int BufferLength = __tmp_44;
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
goto label_89356;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_89356:; 
if (irp == 0)
{
 __return_89429 = -1073741670;
goto label_89430;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_45 = event;
int __tmp_46 = Executive;
int __tmp_47 = KernelMode;
int __tmp_48 = 0;
int __tmp_49 = 0;
int Object = __tmp_45;
int WaitReason = __tmp_46;
int WaitMode = __tmp_47;
int Alertable = __tmp_48;
int Timeout = __tmp_49;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_89384;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_89384;
}
else 
{
{
__VERIFIER_error();
}
goto label_89384;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_89384;
}
else 
{
label_89384:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_89420 = 0;
goto label_89416;
}
else 
{
 __return_89416 = -1073741823;
label_89416:; 
}
status1 = ioStatus__Status;
goto label_89362;
}
}
}
}
else 
{
label_89362:; 
 __return_89430 = status1;
label_89430:; 
}
status6 = __return_89430;
if (status6 >= 0)
{
__cil_tmp107 = (unsigned long)status6;
if (__cil_tmp107 != -1073741764)
{
status6 = 0;
goto label_89486;
}
else 
{
goto label_89436;
}
}
else 
{
label_89436:; 
__cil_tmp108 = (unsigned long)status6;
if (__cil_tmp108 != -1073741764)
{
Irp__IoStatus__Information = 0;
{
int __tmp_50 = status6;
int __tmp_51 = Irp;
int __tmp_52 = deviceExtension__TargetDeviceObject;
int status = __tmp_50;
int Irp = __tmp_51;
int deviceExtension__TargetDeviceObject = __tmp_52;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_53 = Irp;
int __tmp_54 = 0;
int Irp = __tmp_53;
int PriorityBoost = __tmp_54;
if (s == NP)
{
s = DC;
goto label_89469;
}
else 
{
{
__VERIFIER_error();
}
label_89469:; 
}
 __return_89476 = status;
}
tmp___4 = __return_89476;
 __return_89480 = tmp___4;
goto label_89846;
}
}
else 
{
label_89486:; 
__cil_tmp109 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp109 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_89494;
}
else 
{
tracksToReturn = tracksOnCd;
label_89494:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_89500;
}
else 
{
label_89500:; 
goto label_89670;
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
status6 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_89670;
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
status6 = -1073741789;
Irp__IoStatus__Information = 0;
goto label_89670;
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
goto label_89670;
}
else 
{
if (SubQPtr == 0)
{
status6 = -1073741670;
Irp__IoStatus__Information = 0;
{
int __tmp_55 = status6;
int __tmp_56 = Irp;
int __tmp_57 = deviceExtension__TargetDeviceObject;
int status = __tmp_55;
int Irp = __tmp_56;
int deviceExtension__TargetDeviceObject = __tmp_57;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_58 = Irp;
int __tmp_59 = 0;
int Irp = __tmp_58;
int PriorityBoost = __tmp_59;
if (s == NP)
{
s = DC;
goto label_89166;
}
else 
{
{
__VERIFIER_error();
}
label_89166:; 
}
 __return_89173 = status;
}
tmp___5 = __return_89173;
 __return_89177 = tmp___5;
goto label_89846;
}
}
else 
{
if (userPtr__Format != 1)
{
status6 = -1073741823;
Irp__IoStatus__Information = 0;
{
int __tmp_60 = status6;
int __tmp_61 = Irp;
int __tmp_62 = deviceExtension__TargetDeviceObject;
int status = __tmp_60;
int Irp = __tmp_61;
int deviceExtension__TargetDeviceObject = __tmp_62;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_63 = Irp;
int __tmp_64 = 0;
int Irp = __tmp_63;
int PriorityBoost = __tmp_64;
if (s == NP)
{
s = DC;
goto label_89130;
}
else 
{
{
__VERIFIER_error();
}
label_89130:; 
}
 __return_89137 = status;
}
tmp___6 = __return_89137;
 __return_89141 = tmp___6;
goto label_89846;
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_65 = deviceExtension;
int __tmp_66 = srb;
int __tmp_67 = SubQPtr;
int __tmp_68 = sizeof__SUB_Q_CURRENT_POSITION;
int Extension = __tmp_65;
int Srb = __tmp_66;
int Buffer = __tmp_67;
int BufferLength = __tmp_68;
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
goto label_89019;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_89019:; 
if (irp == 0)
{
 __return_89092 = -1073741670;
goto label_89093;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_69 = event;
int __tmp_70 = Executive;
int __tmp_71 = KernelMode;
int __tmp_72 = 0;
int __tmp_73 = 0;
int Object = __tmp_69;
int WaitReason = __tmp_70;
int WaitMode = __tmp_71;
int Alertable = __tmp_72;
int Timeout = __tmp_73;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_89047;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_89047;
}
else 
{
{
__VERIFIER_error();
}
goto label_89047;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_89047;
}
else 
{
label_89047:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_89083 = 0;
goto label_89079;
}
else 
{
 __return_89079 = -1073741823;
label_89079:; 
}
status1 = ioStatus__Status;
goto label_89025;
}
}
}
}
else 
{
label_89025:; 
 __return_89093 = status1;
label_89093:; 
}
status6 = __return_89093;
if (status6 >= 0)
{
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_89100;
}
else 
{
Irp__IoStatus__Information = 0;
label_89100:; 
goto label_89670;
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
goto label_89670;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_74 = deviceExtension;
int __tmp_75 = srb;
int __tmp_76 = 0;
int __tmp_77 = 0;
int Extension = __tmp_74;
int Srb = __tmp_75;
int Buffer = __tmp_76;
int BufferLength = __tmp_77;
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
goto label_88887;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_88887:; 
if (irp == 0)
{
 __return_88960 = -1073741670;
goto label_88961;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_78 = event;
int __tmp_79 = Executive;
int __tmp_80 = KernelMode;
int __tmp_81 = 0;
int __tmp_82 = 0;
int Object = __tmp_78;
int WaitReason = __tmp_79;
int WaitMode = __tmp_80;
int Alertable = __tmp_81;
int Timeout = __tmp_82;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_88915;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_88915;
}
else 
{
{
__VERIFIER_error();
}
goto label_88915;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_88915;
}
else 
{
label_88915:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_88951 = 0;
goto label_88947;
}
else 
{
 __return_88947 = -1073741823;
label_88947:; 
}
status1 = ioStatus__Status;
goto label_88893;
}
}
}
}
else 
{
label_88893:; 
 __return_88961 = status1;
label_88961:; 
}
status6 = __return_88961;
goto label_89812;
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
goto label_89670;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_83 = deviceExtension;
int __tmp_84 = srb;
int __tmp_85 = 0;
int __tmp_86 = 0;
int Extension = __tmp_83;
int Srb = __tmp_84;
int Buffer = __tmp_85;
int BufferLength = __tmp_86;
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
goto label_88758;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_88758:; 
if (irp == 0)
{
 __return_88831 = -1073741670;
goto label_88832;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_87 = event;
int __tmp_88 = Executive;
int __tmp_89 = KernelMode;
int __tmp_90 = 0;
int __tmp_91 = 0;
int Object = __tmp_87;
int WaitReason = __tmp_88;
int WaitMode = __tmp_89;
int Alertable = __tmp_90;
int Timeout = __tmp_91;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_88786;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_88786;
}
else 
{
{
__VERIFIER_error();
}
goto label_88786;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_88786;
}
else 
{
label_88786:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_88822 = 0;
goto label_88818;
}
else 
{
 __return_88818 = -1073741823;
label_88818:; 
}
status1 = ioStatus__Status;
goto label_88764;
}
}
}
}
else 
{
label_88764:; 
 __return_88832 = status1;
label_88832:; 
}
status6 = __return_88832;
label_89812:; 
{
int __tmp_92 = status6;
int __tmp_93 = Irp;
int __tmp_94 = deviceExtension__TargetDeviceObject;
int status = __tmp_92;
int Irp = __tmp_93;
int deviceExtension__TargetDeviceObject = __tmp_94;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_95 = Irp;
int __tmp_96 = 0;
int Irp = __tmp_95;
int PriorityBoost = __tmp_96;
if (s == NP)
{
s = DC;
goto label_89834;
}
else 
{
{
__VERIFIER_error();
}
label_89834:; 
}
 __return_89841 = status;
}
tmp___8 = __return_89841;
 __return_89845 = tmp___8;
goto label_89846;
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
int __tmp_97 = deviceExtension;
int __tmp_98 = srb;
int __tmp_99 = 0;
int __tmp_100 = 0;
int Extension = __tmp_97;
int Srb = __tmp_98;
int Buffer = __tmp_99;
int BufferLength = __tmp_100;
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
goto label_88633;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_88633:; 
if (irp == 0)
{
 __return_88706 = -1073741670;
goto label_88707;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_101 = event;
int __tmp_102 = Executive;
int __tmp_103 = KernelMode;
int __tmp_104 = 0;
int __tmp_105 = 0;
int Object = __tmp_101;
int WaitReason = __tmp_102;
int WaitMode = __tmp_103;
int Alertable = __tmp_104;
int Timeout = __tmp_105;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_88661;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_88661;
}
else 
{
{
__VERIFIER_error();
}
goto label_88661;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_88661;
}
else 
{
label_88661:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_88697 = 0;
goto label_88693;
}
else 
{
 __return_88693 = -1073741823;
label_88693:; 
}
status1 = ioStatus__Status;
goto label_88639;
}
}
}
}
else 
{
label_88639:; 
 __return_88707 = status1;
label_88707:; 
}
status6 = __return_88707;
goto label_89670;
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
goto label_88583;
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
label_88583:; 
goto label_88586;
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
label_88586:; 
Irp__IoStatus__Information = 0;
status6 = -1073741808;
label_89670:; 
goto label_89812;
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
int __tmp_106 = DeviceObject;
int DeviceObject = __tmp_106;
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
 __return_88359 = 0;
goto label_88355;
}
else 
{
if (currentBuffer == 0)
{
 __return_88357 = 0;
goto label_88355;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_88354 = 0;
goto label_88355;
}
else 
{
__cil_tmp10 = (long)status5;
if (__cil_tmp10 == 259L)
{
{
int __tmp_107 = event;
int __tmp_108 = Suspended;
int __tmp_109 = KernelMode;
int __tmp_110 = 0;
int __tmp_111 = 0;
int Object = __tmp_107;
int WaitReason = __tmp_108;
int WaitMode = __tmp_109;
int Alertable = __tmp_110;
int Timeout = __tmp_111;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_88295;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_88295;
}
else 
{
{
__VERIFIER_error();
}
goto label_88295;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_88295;
}
else 
{
label_88295:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_88331 = 0;
goto label_88327;
}
else 
{
 __return_88327 = -1073741823;
label_88327:; 
}
status5 = ioStatus__Status;
goto label_88273;
}
}
}
}
else 
{
label_88273:; 
if (status5 < 0)
{
 __return_88351 = 0;
goto label_88355;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_88344;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_88344:; 
 __return_88355 = returnValue;
label_88355:; 
}
goto label_88241;
}
}
}
}
}
}
}
else 
{
label_88241:; 
{
int __tmp_112 = DeviceObject;
int __tmp_113 = Irp;
int DeviceObject = __tmp_112;
int Irp = __tmp_113;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_114 = deviceExtension__TargetDeviceObject;
int __tmp_115 = Irp;
int DeviceObject = __tmp_114;
int Irp = __tmp_115;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_116 = DeviceObject;
int __tmp_117 = Irp;
int __tmp_118 = lcontext;
int DeviceObject = __tmp_116;
int Irp = __tmp_117;
int Context = __tmp_118;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_88430;
}
else 
{
{
__VERIFIER_error();
}
goto label_88430;
}
}
else 
{
label_88430:; 
if (myStatus >= 0)
{
{
int __tmp_119 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_119;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_88456;
}
else 
{
label_88456:; 
}
goto label_88448;
}
}
else 
{
label_88448:; 
 __return_88467 = myStatus;
}
compRetStatus = __return_88467;
goto label_88420;
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
int Event = __tmp_122;
{
int __tmp_123 = Event;
int __tmp_124 = 0;
int __tmp_125 = 0;
int Event = __tmp_123;
int Increment = __tmp_124;
int Wait = __tmp_125;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_88413 = l;
}
 __return_88416 = -1073741802;
}
compRetStatus = __return_88416;
label_88420:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_88491;
}
else 
{
{
__VERIFIER_error();
}
label_88491:; 
}
goto label_88398;
}
}
else 
{
goto label_88398;
}
}
}
else 
{
label_88398:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_88517;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_88517;
}
else 
{
returnVal2 = 259;
label_88517:; 
goto label_88504;
}
}
}
else 
{
returnVal2 = 259;
label_88504:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_88546;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_88556;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_88556:; 
goto label_88546;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_88546;
}
else 
{
{
__VERIFIER_error();
}
label_88546:; 
 __return_88567 = returnVal2;
}
tmp = __return_88567;
 __return_88570 = tmp;
}
tmp___7 = __return_88570;
 __return_89846 = tmp___7;
label_89846:; 
}
status4 = __return_89846;
goto label_85088;
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
if (deviceExtension__Active == 3)
{
{
int __tmp_126 = DeviceObject;
int __tmp_127 = Irp;
int DeviceObject = __tmp_126;
int Irp = __tmp_127;
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
goto label_87916;
}
else 
{
{
int __tmp_128 = DeviceObject;
int DeviceObject = __tmp_128;
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
 __return_87741 = 0;
goto label_87737;
}
else 
{
if (currentBuffer == 0)
{
 __return_87739 = 0;
goto label_87737;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_87736 = 0;
goto label_87737;
}
else 
{
__cil_tmp10 = (long)status5;
if (__cil_tmp10 == 259L)
{
{
int __tmp_129 = event;
int __tmp_130 = Suspended;
int __tmp_131 = KernelMode;
int __tmp_132 = 0;
int __tmp_133 = 0;
int Object = __tmp_129;
int WaitReason = __tmp_130;
int WaitMode = __tmp_131;
int Alertable = __tmp_132;
int Timeout = __tmp_133;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_87677;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87677;
}
else 
{
{
__VERIFIER_error();
}
goto label_87677;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87677;
}
else 
{
label_87677:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_87713 = 0;
goto label_87709;
}
else 
{
 __return_87709 = -1073741823;
label_87709:; 
}
status5 = ioStatus__Status;
goto label_87655;
}
}
}
}
else 
{
label_87655:; 
if (status5 < 0)
{
 __return_87733 = 0;
goto label_87737;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_87726;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_87726:; 
 __return_87737 = returnValue;
label_87737:; 
}
tmp = __return_87737;
if (tmp == 0)
{
if (Toc == 0)
{
status7 = -1073741670;
Irp__IoStatus__Information = 0;
__cil_tmp93 = (unsigned long)status7;
if (__cil_tmp93 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_87954;
}
else 
{
label_87954:; 
myStatus = status7;
{
int __tmp_134 = Irp;
int __tmp_135 = 0;
int Irp = __tmp_134;
int PriorityBoost = __tmp_135;
if (s == NP)
{
s = DC;
goto label_87973;
}
else 
{
{
__VERIFIER_error();
}
label_87973:; 
}
 __return_87981 = status7;
goto label_87948;
}
}
}
else 
{
srb__TimeOutValue = 10;
srb__CdbLength = 10;
{
int __tmp_136 = deviceExtension;
int __tmp_137 = srb;
int __tmp_138 = Toc;
int __tmp_139 = sizeof__CDROM_TOC;
int Extension = __tmp_136;
int Srb = __tmp_137;
int Buffer = __tmp_138;
int BufferLength = __tmp_139;
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
goto label_87792;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_87792:; 
if (irp == 0)
{
 __return_87865 = -1073741670;
goto label_87866;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_140 = event;
int __tmp_141 = Executive;
int __tmp_142 = KernelMode;
int __tmp_143 = 0;
int __tmp_144 = 0;
int Object = __tmp_140;
int WaitReason = __tmp_141;
int WaitMode = __tmp_142;
int Alertable = __tmp_143;
int Timeout = __tmp_144;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_87820;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87820;
}
else 
{
{
__VERIFIER_error();
}
goto label_87820;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87820;
}
else 
{
label_87820:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_87856 = 0;
goto label_87852;
}
else 
{
 __return_87852 = -1073741823;
label_87852:; 
}
status1 = ioStatus__Status;
goto label_87798;
}
}
}
}
else 
{
label_87798:; 
 __return_87866 = status1;
label_87866:; 
}
status7 = __return_87866;
if (status7 < 0)
{
__cil_tmp95 = (unsigned long)status7;
if (__cil_tmp95 != -1073741764)
{
__cil_tmp96 = (unsigned long)status7;
if (__cil_tmp96 != -1073741764)
{
__cil_tmp97 = (unsigned long)status7;
if (__cil_tmp97 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_87920;
}
else 
{
label_87920:; 
myStatus = status7;
{
int __tmp_145 = Irp;
int __tmp_146 = 0;
int Irp = __tmp_145;
int PriorityBoost = __tmp_146;
if (s == NP)
{
s = DC;
goto label_87939;
}
else 
{
{
__VERIFIER_error();
}
label_87939:; 
}
 __return_87947 = status7;
goto label_87948;
}
}
}
else 
{
goto label_87879;
}
}
else 
{
status7 = 0;
label_87879:; 
goto label_87873;
}
}
else 
{
status7 = 0;
label_87873:; 
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength > sizeof__CDROM_TOC)
{
bytesTransfered = sizeof__CDROM_TOC;
goto label_87891;
}
else 
{
bytesTransfered = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
label_87891:; 
__cil_tmp98 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp98 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength - TrackData__0;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_87902;
}
else 
{
tracksToReturn = tracksOnCd;
label_87902:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_87908;
}
else 
{
label_87908:; 
goto label_87916;
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
status7 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_87916;
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
goto label_87340;
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
label_87340:; 
Irp__IoStatus__Information = 0;
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_147 = deviceExtension;
int __tmp_148 = srb;
int __tmp_149 = 0;
int __tmp_150 = 0;
int Extension = __tmp_147;
int Srb = __tmp_148;
int Buffer = __tmp_149;
int BufferLength = __tmp_150;
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
goto label_87381;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_87381:; 
if (irp == 0)
{
 __return_87454 = -1073741670;
goto label_87455;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_151 = event;
int __tmp_152 = Executive;
int __tmp_153 = KernelMode;
int __tmp_154 = 0;
int __tmp_155 = 0;
int Object = __tmp_151;
int WaitReason = __tmp_152;
int WaitMode = __tmp_153;
int Alertable = __tmp_154;
int Timeout = __tmp_155;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_87409;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87409;
}
else 
{
{
__VERIFIER_error();
}
goto label_87409;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87409;
}
else 
{
label_87409:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_87445 = 0;
goto label_87441;
}
else 
{
 __return_87441 = -1073741823;
label_87441:; 
}
status1 = ioStatus__Status;
goto label_87387;
}
}
}
}
else 
{
label_87387:; 
 __return_87455 = status1;
label_87455:; 
}
status7 = __return_87455;
__cil_tmp99 = 8;
__cil_tmp100 = 16384;
__cil_tmp101 = 131072;
__cil_tmp102 = 147456;
__cil_tmp103 = 147464;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp103)
{
__cil_tmp104 = (unsigned long)status7;
if (__cil_tmp104 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_87593;
}
else 
{
label_87593:; 
myStatus = status7;
{
int __tmp_156 = Irp;
int __tmp_157 = 0;
int Irp = __tmp_156;
int PriorityBoost = __tmp_157;
if (s == NP)
{
s = DC;
goto label_87612;
}
else 
{
{
__VERIFIER_error();
}
label_87612:; 
}
 __return_87620 = status7;
goto label_87948;
}
}
}
else 
{
if (currentIrpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CDROM_PLAY_AUDIO_MSF)
{
status7 = -1073741820;
goto label_87916;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_158 = deviceExtension;
int __tmp_159 = srb;
int __tmp_160 = 0;
int __tmp_161 = 0;
int Extension = __tmp_158;
int Srb = __tmp_159;
int Buffer = __tmp_160;
int BufferLength = __tmp_161;
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
goto label_87507;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_87507:; 
if (irp == 0)
{
 __return_87580 = -1073741670;
goto label_87581;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_162 = event;
int __tmp_163 = Executive;
int __tmp_164 = KernelMode;
int __tmp_165 = 0;
int __tmp_166 = 0;
int Object = __tmp_162;
int WaitReason = __tmp_163;
int WaitMode = __tmp_164;
int Alertable = __tmp_165;
int Timeout = __tmp_166;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_87535;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87535;
}
else 
{
{
__VERIFIER_error();
}
goto label_87535;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87535;
}
else 
{
label_87535:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_87571 = 0;
goto label_87567;
}
else 
{
 __return_87567 = -1073741823;
label_87567:; 
}
status1 = ioStatus__Status;
goto label_87513;
}
}
}
}
else 
{
label_87513:; 
 __return_87581 = status1;
label_87581:; 
}
status7 = __return_87581;
label_87989:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_87993;
}
else 
{
label_87993:; 
myStatus = status7;
{
int __tmp_167 = Irp;
int __tmp_168 = 0;
int Irp = __tmp_167;
int PriorityBoost = __tmp_168;
if (s == NP)
{
s = DC;
goto label_88012;
}
else 
{
{
__VERIFIER_error();
}
label_88012:; 
}
 __return_88020 = status7;
goto label_87948;
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
goto label_87916;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_169 = deviceExtension;
int __tmp_170 = srb;
int __tmp_171 = 0;
int __tmp_172 = 0;
int Extension = __tmp_169;
int Srb = __tmp_170;
int Buffer = __tmp_171;
int BufferLength = __tmp_172;
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
goto label_87240;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_87240:; 
if (irp == 0)
{
 __return_87313 = -1073741670;
goto label_87314;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
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
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_87268;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87268;
}
else 
{
{
__VERIFIER_error();
}
goto label_87268;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87268;
}
else 
{
label_87268:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_87304 = 0;
goto label_87300;
}
else 
{
 __return_87300 = -1073741823;
label_87300:; 
}
status1 = ioStatus__Status;
goto label_87246;
}
}
}
}
else 
{
label_87246:; 
 __return_87314 = status1;
label_87314:; 
}
status7 = __return_87314;
if (status7 < 0)
{
__cil_tmp105 = (unsigned long)status7;
if (__cil_tmp105 == -1073741808)
{
status7 = -1073741803;
goto label_87319;
}
else 
{
goto label_87319;
}
}
else 
{
label_87319:; 
goto label_87916;
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
if (__cil_tmp106 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_87168;
}
else 
{
label_87168:; 
myStatus = status7;
{
int __tmp_178 = Irp;
int __tmp_179 = 0;
int Irp = __tmp_178;
int PriorityBoost = __tmp_179;
if (s == NP)
{
s = DC;
goto label_87187;
}
else 
{
{
__VERIFIER_error();
}
label_87187:; 
}
 __return_87195 = status7;
goto label_87948;
}
}
}
else 
{
if (deviceExtension__Paused == 1)
{
status7 = 0;
__cil_tmp107 = (unsigned long)status7;
if (__cil_tmp107 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_87136;
}
else 
{
label_87136:; 
myStatus = status7;
{
int __tmp_180 = Irp;
int __tmp_181 = 0;
int Irp = __tmp_180;
int PriorityBoost = __tmp_181;
if (s == NP)
{
s = DC;
goto label_87155;
}
else 
{
{
__VERIFIER_error();
}
label_87155:; 
}
 __return_87163 = status7;
goto label_87948;
}
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_182 = deviceExtension;
int __tmp_183 = srb;
int __tmp_184 = SubQPtr;
int __tmp_185 = sizeof__SUB_Q_CHANNEL_DATA;
int Extension = __tmp_182;
int Srb = __tmp_183;
int Buffer = __tmp_184;
int BufferLength = __tmp_185;
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
goto label_86869;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_86869:; 
if (irp == 0)
{
 __return_86942 = -1073741670;
goto label_86943;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_186 = event;
int __tmp_187 = Executive;
int __tmp_188 = KernelMode;
int __tmp_189 = 0;
int __tmp_190 = 0;
int Object = __tmp_186;
int WaitReason = __tmp_187;
int WaitMode = __tmp_188;
int Alertable = __tmp_189;
int Timeout = __tmp_190;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_86897;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_86897;
}
else 
{
{
__VERIFIER_error();
}
goto label_86897;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_86897;
}
else 
{
label_86897:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_86933 = 0;
goto label_86929;
}
else 
{
 __return_86929 = -1073741823;
label_86929:; 
}
status1 = ioStatus__Status;
goto label_86875;
}
}
}
}
else 
{
label_86875:; 
 __return_86943 = status1;
label_86943:; 
}
status7 = __return_86943;
if (status7 < 0)
{
__cil_tmp109 = (unsigned long)status7;
if (__cil_tmp109 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_87104;
}
else 
{
label_87104:; 
myStatus = status7;
{
int __tmp_191 = Irp;
int __tmp_192 = 0;
int Irp = __tmp_191;
int PriorityBoost = __tmp_192;
if (s == NP)
{
s = DC;
goto label_87123;
}
else 
{
{
__VERIFIER_error();
}
label_87123:; 
}
 __return_87131 = status7;
goto label_87948;
}
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_193 = deviceExtension;
int __tmp_194 = srb;
int __tmp_195 = 0;
int __tmp_196 = 0;
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
goto label_86987;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_86987:; 
if (irp == 0)
{
 __return_87060 = -1073741670;
goto label_87061;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_197 = event;
int __tmp_198 = Executive;
int __tmp_199 = KernelMode;
int __tmp_200 = 0;
int __tmp_201 = 0;
int Object = __tmp_197;
int WaitReason = __tmp_198;
int WaitMode = __tmp_199;
int Alertable = __tmp_200;
int Timeout = __tmp_201;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_87015;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87015;
}
else 
{
{
__VERIFIER_error();
}
goto label_87015;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_87015;
}
else 
{
label_87015:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_87051 = 0;
goto label_87047;
}
else 
{
 __return_87047 = -1073741823;
label_87047:; 
}
status1 = ioStatus__Status;
goto label_86993;
}
}
}
}
else 
{
label_86993:; 
 __return_87061 = status1;
label_87061:; 
}
status7 = __return_87061;
if (status7 < 0)
{
__cil_tmp111 = (unsigned long)status7;
if (__cil_tmp111 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_87072;
}
else 
{
label_87072:; 
myStatus = status7;
{
int __tmp_202 = Irp;
int __tmp_203 = 0;
int Irp = __tmp_202;
int PriorityBoost = __tmp_203;
if (s == NP)
{
s = DC;
goto label_87091;
}
else 
{
{
__VERIFIER_error();
}
label_87091:; 
}
 __return_87099 = status7;
goto label_87948;
}
}
}
else 
{
goto label_87916;
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
__cil_tmp58 = 16;
__cil_tmp59 = 16384;
__cil_tmp60 = 131072;
__cil_tmp61 = 147456;
__cil_tmp62 = 147472;
if (currentIrpStack__Parameters__DeviceIoControl__IoControlCode == __cil_tmp62)
{
Irp__IoStatus__Information = 0;
if (deviceExtension__Paused == 0)
{
status7 = -1073741823;
__cil_tmp112 = (unsigned long)status7;
if (__cil_tmp112 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_86794;
}
else 
{
label_86794:; 
myStatus = status7;
{
int __tmp_204 = Irp;
int __tmp_205 = 0;
int Irp = __tmp_204;
int PriorityBoost = __tmp_205;
if (s == NP)
{
s = DC;
goto label_86813;
}
else 
{
{
__VERIFIER_error();
}
label_86813:; 
}
 __return_86821 = status7;
goto label_87948;
}
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_206 = deviceExtension;
int __tmp_207 = srb;
int __tmp_208 = 0;
int __tmp_209 = 0;
int Extension = __tmp_206;
int Srb = __tmp_207;
int Buffer = __tmp_208;
int BufferLength = __tmp_209;
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
goto label_86703;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_86703:; 
if (irp == 0)
{
 __return_86776 = -1073741670;
goto label_86777;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_210 = event;
int __tmp_211 = Executive;
int __tmp_212 = KernelMode;
int __tmp_213 = 0;
int __tmp_214 = 0;
int Object = __tmp_210;
int WaitReason = __tmp_211;
int WaitMode = __tmp_212;
int Alertable = __tmp_213;
int Timeout = __tmp_214;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_86731;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_86731;
}
else 
{
{
__VERIFIER_error();
}
goto label_86731;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_86731;
}
else 
{
label_86731:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_86767 = 0;
goto label_86763;
}
else 
{
 __return_86763 = -1073741823;
label_86763:; 
}
status1 = ioStatus__Status;
goto label_86709;
}
}
}
}
else 
{
label_86709:; 
 __return_86777 = status1;
label_86777:; 
}
status7 = __return_86777;
if (status7 >= 0)
{
deviceExtension__PlayActive = 1;
deviceExtension__Paused = 0;
goto label_86782;
}
else 
{
label_86782:; 
goto label_87916;
}
}
}
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
goto label_87916;
}
else 
{
if (SubQPtr___0 == 0)
{
status7 = -1073741670;
Irp__IoStatus__Information = 0;
__cil_tmp113 = (unsigned long)status7;
if (__cil_tmp113 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_86627;
}
else 
{
label_86627:; 
myStatus = status7;
{
int __tmp_215 = Irp;
int __tmp_216 = 0;
int Irp = __tmp_215;
int PriorityBoost = __tmp_216;
if (s == NP)
{
s = DC;
goto label_86646;
}
else 
{
{
__VERIFIER_error();
}
label_86646:; 
}
 __return_86654 = status7;
goto label_87948;
}
}
}
else 
{
if (userPtr__Format != 1)
{
status7 = -1073741823;
Irp__IoStatus__Information = 0;
__cil_tmp114 = (unsigned long)status7;
if (__cil_tmp114 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_86593;
}
else 
{
label_86593:; 
myStatus = status7;
{
int __tmp_217 = Irp;
int __tmp_218 = 0;
int Irp = __tmp_217;
int PriorityBoost = __tmp_218;
if (s == NP)
{
s = DC;
goto label_86612;
}
else 
{
{
__VERIFIER_error();
}
label_86612:; 
}
 __return_86620 = status7;
goto label_87948;
}
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_219 = deviceExtension;
int __tmp_220 = srb;
int __tmp_221 = SubQPtr___0;
int __tmp_222 = sizeof__SUB_Q_CHANNEL_DATA;
int Extension = __tmp_219;
int Srb = __tmp_220;
int Buffer = __tmp_221;
int BufferLength = __tmp_222;
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
goto label_86494;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_86494:; 
if (irp == 0)
{
 __return_86567 = -1073741670;
goto label_86568;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_223 = event;
int __tmp_224 = Executive;
int __tmp_225 = KernelMode;
int __tmp_226 = 0;
int __tmp_227 = 0;
int Object = __tmp_223;
int WaitReason = __tmp_224;
int WaitMode = __tmp_225;
int Alertable = __tmp_226;
int Timeout = __tmp_227;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_86522;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_86522;
}
else 
{
{
__VERIFIER_error();
}
goto label_86522;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_86522;
}
else 
{
label_86522:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_86558 = 0;
goto label_86554;
}
else 
{
 __return_86554 = -1073741823;
label_86554:; 
}
status1 = ioStatus__Status;
goto label_86500;
}
}
}
}
else 
{
label_86500:; 
 __return_86568 = status1;
label_86568:; 
}
status7 = __return_86568;
if (status7 >= 0)
{
if (deviceExtension__Paused == 1)
{
deviceExtension__PlayActive = 0;
goto label_86578;
}
else 
{
label_86578:; 
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_86575;
}
}
else 
{
Irp__IoStatus__Information = 0;
label_86575:; 
goto label_87916;
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
int __tmp_228 = deviceExtension;
int __tmp_229 = srb;
int __tmp_230 = 0;
int __tmp_231 = 0;
int Extension = __tmp_228;
int Srb = __tmp_229;
int Buffer = __tmp_230;
int BufferLength = __tmp_231;
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
goto label_86367;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_86367:; 
if (irp == 0)
{
 __return_86440 = -1073741670;
goto label_86441;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_232 = event;
int __tmp_233 = Executive;
int __tmp_234 = KernelMode;
int __tmp_235 = 0;
int __tmp_236 = 0;
int Object = __tmp_232;
int WaitReason = __tmp_233;
int WaitMode = __tmp_234;
int Alertable = __tmp_235;
int Timeout = __tmp_236;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_86395;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_86395;
}
else 
{
{
__VERIFIER_error();
}
goto label_86395;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_86395;
}
else 
{
label_86395:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_86431 = 0;
goto label_86427;
}
else 
{
 __return_86427 = -1073741823;
label_86427:; 
}
status1 = ioStatus__Status;
goto label_86373;
}
}
}
}
else 
{
label_86373:; 
 __return_86441 = status1;
label_86441:; 
}
status7 = __return_86441;
goto label_87916;
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
goto label_86317;
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
label_86317:; 
goto label_86320;
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
label_86320:; 
Irp__IoStatus__Information = 0;
status7 = -1073741808;
label_87916:; 
goto label_87989;
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
int __tmp_237 = DeviceObject;
int DeviceObject = __tmp_237;
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
 __return_86057 = 0;
goto label_86053;
}
else 
{
if (currentBuffer == 0)
{
 __return_86055 = 0;
goto label_86053;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_86052 = 0;
goto label_86053;
}
else 
{
__cil_tmp10 = (long)status5;
if (__cil_tmp10 == 259L)
{
{
int __tmp_238 = event;
int __tmp_239 = Suspended;
int __tmp_240 = KernelMode;
int __tmp_241 = 0;
int __tmp_242 = 0;
int Object = __tmp_238;
int WaitReason = __tmp_239;
int WaitMode = __tmp_240;
int Alertable = __tmp_241;
int Timeout = __tmp_242;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_85993;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_85993;
}
else 
{
{
__VERIFIER_error();
}
goto label_85993;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_85993;
}
else 
{
label_85993:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_86029 = 0;
goto label_86025;
}
else 
{
 __return_86025 = -1073741823;
label_86025:; 
}
status5 = ioStatus__Status;
goto label_85971;
}
}
}
}
else 
{
label_85971:; 
if (status5 < 0)
{
 __return_86049 = 0;
goto label_86053;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_86042;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_86042:; 
 __return_86053 = returnValue;
label_86053:; 
}
tmp___1 = __return_86053;
if (tmp___1 == 1)
{
deviceExtension__PlayActive = 1;
status7 = 0;
Irp__IoStatus__Information = 0;
__cil_tmp115 = (unsigned long)status7;
if (__cil_tmp115 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_86281;
}
else 
{
label_86281:; 
myStatus = status7;
{
int __tmp_243 = Irp;
int __tmp_244 = 0;
int Irp = __tmp_243;
int PriorityBoost = __tmp_244;
if (s == NP)
{
s = DC;
goto label_86300;
}
else 
{
{
__VERIFIER_error();
}
label_86300:; 
}
 __return_86308 = status7;
goto label_87948;
}
}
}
else 
{
deviceExtension__PlayActive = 0;
{
int __tmp_245 = DeviceObject;
int __tmp_246 = Irp;
int DeviceObject = __tmp_245;
int Irp = __tmp_246;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_247 = deviceExtension__TargetDeviceObject;
int __tmp_248 = Irp;
int DeviceObject = __tmp_247;
int Irp = __tmp_248;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_249 = DeviceObject;
int __tmp_250 = Irp;
int __tmp_251 = lcontext;
int DeviceObject = __tmp_249;
int Irp = __tmp_250;
int Context = __tmp_251;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_86129;
}
else 
{
{
__VERIFIER_error();
}
goto label_86129;
}
}
else 
{
label_86129:; 
if (myStatus >= 0)
{
{
int __tmp_252 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_252;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_86155;
}
else 
{
label_86155:; 
}
goto label_86147;
}
}
else 
{
label_86147:; 
 __return_86166 = myStatus;
}
compRetStatus = __return_86166;
goto label_86119;
}
}
}
else 
{
{
int __tmp_253 = DeviceObject;
int __tmp_254 = Irp;
int __tmp_255 = lcontext;
int DeviceObject = __tmp_253;
int Irp = __tmp_254;
int Event = __tmp_255;
{
int __tmp_256 = Event;
int __tmp_257 = 0;
int __tmp_258 = 0;
int Event = __tmp_256;
int Increment = __tmp_257;
int Wait = __tmp_258;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_86112 = l;
}
 __return_86115 = -1073741802;
}
compRetStatus = __return_86115;
label_86119:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_86190;
}
else 
{
{
__VERIFIER_error();
}
label_86190:; 
}
goto label_86097;
}
}
else 
{
goto label_86097;
}
}
}
else 
{
label_86097:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_86216;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_86216;
}
else 
{
returnVal2 = 259;
label_86216:; 
goto label_86203;
}
}
}
else 
{
returnVal2 = 259;
label_86203:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_86245;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_86255;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_86255:; 
goto label_86245;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_86245;
}
else 
{
{
__VERIFIER_error();
}
label_86245:; 
 __return_86266 = returnVal2;
}
tmp = __return_86266;
 __return_86269 = tmp;
}
tmp___0 = __return_86269;
 __return_86273 = tmp___0;
goto label_87948;
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
int __tmp_259 = DeviceObject;
int __tmp_260 = Irp;
int DeviceObject = __tmp_259;
int Irp = __tmp_260;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_261 = deviceExtension__TargetDeviceObject;
int __tmp_262 = Irp;
int DeviceObject = __tmp_261;
int Irp = __tmp_262;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_263 = DeviceObject;
int __tmp_264 = Irp;
int __tmp_265 = lcontext;
int DeviceObject = __tmp_263;
int Irp = __tmp_264;
int Context = __tmp_265;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_85795;
}
else 
{
{
__VERIFIER_error();
}
goto label_85795;
}
}
else 
{
label_85795:; 
if (myStatus >= 0)
{
{
int __tmp_266 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_266;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_85821;
}
else 
{
label_85821:; 
}
goto label_85813;
}
}
else 
{
label_85813:; 
 __return_85832 = myStatus;
}
compRetStatus = __return_85832;
goto label_85785;
}
}
}
else 
{
{
int __tmp_267 = DeviceObject;
int __tmp_268 = Irp;
int __tmp_269 = lcontext;
int DeviceObject = __tmp_267;
int Irp = __tmp_268;
int Event = __tmp_269;
{
int __tmp_270 = Event;
int __tmp_271 = 0;
int __tmp_272 = 0;
int Event = __tmp_270;
int Increment = __tmp_271;
int Wait = __tmp_272;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_85778 = l;
}
 __return_85781 = -1073741802;
}
compRetStatus = __return_85781;
label_85785:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_85856;
}
else 
{
{
__VERIFIER_error();
}
label_85856:; 
}
goto label_85763;
}
}
else 
{
goto label_85763;
}
}
}
else 
{
label_85763:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_85882;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_85882;
}
else 
{
returnVal2 = 259;
label_85882:; 
goto label_85869;
}
}
}
else 
{
returnVal2 = 259;
label_85869:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_85911;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_85921;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_85921:; 
goto label_85911;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_85911;
}
else 
{
{
__VERIFIER_error();
}
label_85911:; 
 __return_85932 = returnVal2;
}
tmp = __return_85932;
 __return_85935 = tmp;
}
tmp___2 = __return_85935;
 __return_87948 = tmp___2;
label_87948:; 
}
status4 = __return_87948;
goto label_85088;
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
if (deviceExtension__Active == 1)
{
{
int __tmp_273 = DeviceObject;
int __tmp_274 = Irp;
int DeviceObject = __tmp_273;
int Irp = __tmp_274;
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
int __tmp_275 = deviceExtension;
int __tmp_276 = srb;
int __tmp_277 = 0;
int __tmp_278 = 0;
int Extension = __tmp_275;
int Srb = __tmp_276;
int Buffer = __tmp_277;
int BufferLength = __tmp_278;
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
goto label_85372;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_85372:; 
if (irp == 0)
{
 __return_85445 = -1073741670;
goto label_85446;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
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
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_85400;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_85400;
}
else 
{
{
__VERIFIER_error();
}
goto label_85400;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_85400;
}
else 
{
label_85400:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_85436 = 0;
goto label_85432;
}
else 
{
 __return_85432 = -1073741823;
label_85432:; 
}
status1 = ioStatus__Status;
goto label_85378;
}
}
}
}
else 
{
label_85378:; 
 __return_85446 = status1;
label_85446:; 
}
status8 = __return_85446;
if (status8 < 0)
{
Irp__IoStatus__Status = status8;
myStatus = status8;
{
int __tmp_284 = Irp;
int __tmp_285 = 0;
int Irp = __tmp_284;
int PriorityBoost = __tmp_285;
if (s == NP)
{
s = DC;
goto label_85494;
}
else 
{
{
__VERIFIER_error();
}
label_85494:; 
}
 __return_85502 = status8;
goto label_85477;
}
}
else 
{
Irp__IoStatus__Status = status8;
myStatus = status8;
{
int __tmp_286 = Irp;
int __tmp_287 = 0;
int Irp = __tmp_286;
int PriorityBoost = __tmp_287;
if (s == NP)
{
s = DC;
goto label_85468;
}
else 
{
{
__VERIFIER_error();
}
label_85468:; 
}
 __return_85476 = status8;
goto label_85477;
}
}
}
}
}
}
else 
{
{
int __tmp_288 = DeviceObject;
int __tmp_289 = Irp;
int DeviceObject = __tmp_288;
int Irp = __tmp_289;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_290 = deviceExtension__TargetDeviceObject;
int __tmp_291 = Irp;
int DeviceObject = __tmp_290;
int Irp = __tmp_291;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_292 = DeviceObject;
int __tmp_293 = Irp;
int __tmp_294 = lcontext;
int DeviceObject = __tmp_292;
int Irp = __tmp_293;
int Context = __tmp_294;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_85186;
}
else 
{
{
__VERIFIER_error();
}
goto label_85186;
}
}
else 
{
label_85186:; 
if (myStatus >= 0)
{
{
int __tmp_295 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_295;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_85212;
}
else 
{
label_85212:; 
}
goto label_85204;
}
}
else 
{
label_85204:; 
 __return_85223 = myStatus;
}
compRetStatus = __return_85223;
goto label_85176;
}
}
}
else 
{
{
int __tmp_296 = DeviceObject;
int __tmp_297 = Irp;
int __tmp_298 = lcontext;
int DeviceObject = __tmp_296;
int Irp = __tmp_297;
int Event = __tmp_298;
{
int __tmp_299 = Event;
int __tmp_300 = 0;
int __tmp_301 = 0;
int Event = __tmp_299;
int Increment = __tmp_300;
int Wait = __tmp_301;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_85169 = l;
}
 __return_85172 = -1073741802;
}
compRetStatus = __return_85172;
label_85176:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_85247;
}
else 
{
{
__VERIFIER_error();
}
label_85247:; 
}
goto label_85154;
}
}
else 
{
goto label_85154;
}
}
}
else 
{
label_85154:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_85273;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_85273;
}
else 
{
returnVal2 = 259;
label_85273:; 
goto label_85260;
}
}
}
else 
{
returnVal2 = 259;
label_85260:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_85302;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_85312;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_85312:; 
goto label_85302;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_85302;
}
else 
{
{
__VERIFIER_error();
}
label_85302:; 
 __return_85323 = returnVal2;
}
tmp = __return_85323;
 __return_85326 = tmp;
}
tmp = __return_85326;
 __return_85477 = tmp;
label_85477:; 
}
status4 = __return_85477;
goto label_85088;
}
}
}
}
}
}
}
else 
{
if (deviceExtension__Active == 7)
{
{
int __tmp_302 = DeviceObject;
int __tmp_303 = Irp;
int DeviceObject = __tmp_302;
int Irp = __tmp_303;
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
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
goto label_84885;
}
else 
{
compRegistered = 1;
routine = 0;
label_84885:; 
irpSp__Control = 224;
{
int __tmp_304 = deviceExtension__TargetDeviceObject;
int __tmp_305 = Irp;
int DeviceObject = __tmp_304;
int Irp = __tmp_305;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_306 = DeviceObject;
int __tmp_307 = Irp;
int __tmp_308 = lcontext;
int DeviceObject = __tmp_306;
int Irp = __tmp_307;
int Context = __tmp_308;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_84942;
}
else 
{
{
__VERIFIER_error();
}
goto label_84942;
}
}
else 
{
label_84942:; 
if (myStatus >= 0)
{
{
int __tmp_309 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_309;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_84968;
}
else 
{
label_84968:; 
}
goto label_84960;
}
}
else 
{
label_84960:; 
 __return_84979 = myStatus;
}
compRetStatus = __return_84979;
goto label_84932;
}
}
}
else 
{
{
int __tmp_310 = DeviceObject;
int __tmp_311 = Irp;
int __tmp_312 = lcontext;
int DeviceObject = __tmp_310;
int Irp = __tmp_311;
int Event = __tmp_312;
{
int __tmp_313 = Event;
int __tmp_314 = 0;
int __tmp_315 = 0;
int Event = __tmp_313;
int Increment = __tmp_314;
int Wait = __tmp_315;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_84925 = l;
}
 __return_84928 = -1073741802;
}
compRetStatus = __return_84928;
label_84932:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_85003;
}
else 
{
{
__VERIFIER_error();
}
label_85003:; 
}
goto label_84910;
}
}
else 
{
goto label_84910;
}
}
}
else 
{
label_84910:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_85029;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_85029;
}
else 
{
returnVal2 = 259;
label_85029:; 
goto label_85016;
}
}
}
else 
{
returnVal2 = 259;
label_85016:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_85058;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_85068;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_85068:; 
goto label_85058;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_85058;
}
else 
{
{
__VERIFIER_error();
}
label_85058:; 
 __return_85079 = returnVal2;
}
tmp = __return_85079;
 __return_85083 = tmp;
goto label_85084;
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
int __tmp_316 = DeviceObject;
int __tmp_317 = Irp;
int DeviceObject = __tmp_316;
int Irp = __tmp_317;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_318 = deviceExtension__TargetDeviceObject;
int __tmp_319 = Irp;
int DeviceObject = __tmp_318;
int Irp = __tmp_319;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_320 = DeviceObject;
int __tmp_321 = Irp;
int __tmp_322 = lcontext;
int DeviceObject = __tmp_320;
int Irp = __tmp_321;
int Context = __tmp_322;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_84733;
}
else 
{
{
__VERIFIER_error();
}
goto label_84733;
}
}
else 
{
label_84733:; 
if (myStatus >= 0)
{
{
int __tmp_323 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_323;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_84759;
}
else 
{
label_84759:; 
}
goto label_84751;
}
}
else 
{
label_84751:; 
 __return_84770 = myStatus;
}
compRetStatus = __return_84770;
goto label_84723;
}
}
}
else 
{
{
int __tmp_324 = DeviceObject;
int __tmp_325 = Irp;
int __tmp_326 = lcontext;
int DeviceObject = __tmp_324;
int Irp = __tmp_325;
int Event = __tmp_326;
{
int __tmp_327 = Event;
int __tmp_328 = 0;
int __tmp_329 = 0;
int Event = __tmp_327;
int Increment = __tmp_328;
int Wait = __tmp_329;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_84716 = l;
}
 __return_84719 = -1073741802;
}
compRetStatus = __return_84719;
label_84723:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_84794;
}
else 
{
{
__VERIFIER_error();
}
label_84794:; 
}
goto label_84701;
}
}
else 
{
goto label_84701;
}
}
}
else 
{
label_84701:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_84820;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_84820;
}
else 
{
returnVal2 = 259;
label_84820:; 
goto label_84807;
}
}
}
else 
{
returnVal2 = 259;
label_84807:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_84849;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_84859;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_84859:; 
goto label_84849;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_84849;
}
else 
{
{
__VERIFIER_error();
}
label_84849:; 
 __return_84870 = returnVal2;
}
tmp = __return_84870;
 __return_84873 = tmp;
}
tmp___0 = __return_84873;
 __return_85084 = tmp___0;
label_85084:; 
}
status4 = __return_85084;
label_85088:; 
goto label_84642;
}
}
}
}
}
}
}
else 
{
deviceExtension__Active = 0;
{
int __tmp_330 = DeviceObject;
int __tmp_331 = Irp;
int DeviceObject = __tmp_330;
int Irp = __tmp_331;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_332 = deviceExtension__TargetDeviceObject;
int __tmp_333 = Irp;
int DeviceObject = __tmp_332;
int Irp = __tmp_333;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_334 = DeviceObject;
int __tmp_335 = Irp;
int __tmp_336 = lcontext;
int DeviceObject = __tmp_334;
int Irp = __tmp_335;
int Context = __tmp_336;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_84498;
}
else 
{
{
__VERIFIER_error();
}
goto label_84498;
}
}
else 
{
label_84498:; 
if (myStatus >= 0)
{
{
int __tmp_337 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_337;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_84524;
}
else 
{
label_84524:; 
}
goto label_84516;
}
}
else 
{
label_84516:; 
 __return_84535 = myStatus;
}
compRetStatus = __return_84535;
goto label_84488;
}
}
}
else 
{
{
int __tmp_338 = DeviceObject;
int __tmp_339 = Irp;
int __tmp_340 = lcontext;
int DeviceObject = __tmp_338;
int Irp = __tmp_339;
int Event = __tmp_340;
{
int __tmp_341 = Event;
int __tmp_342 = 0;
int __tmp_343 = 0;
int Event = __tmp_341;
int Increment = __tmp_342;
int Wait = __tmp_343;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_84481 = l;
}
 __return_84484 = -1073741802;
}
compRetStatus = __return_84484;
label_84488:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_84559;
}
else 
{
{
__VERIFIER_error();
}
label_84559:; 
}
goto label_84466;
}
}
else 
{
goto label_84466;
}
}
}
else 
{
label_84466:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_84585;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_84585;
}
else 
{
returnVal2 = 259;
label_84585:; 
goto label_84572;
}
}
}
else 
{
returnVal2 = 259;
label_84572:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_84614;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_84624;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_84624:; 
goto label_84614;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_84614;
}
else 
{
{
__VERIFIER_error();
}
label_84614:; 
 __return_84635 = returnVal2;
}
tmp = __return_84635;
 __return_84638 = tmp;
}
status4 = __return_84638;
label_84642:; 
 __return_89923 = status4;
}
status10 = __return_89923;
goto label_82658;
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
if (tmp_ndt_2 == 3)
{
{
int __tmp_344 = devobj;
int __tmp_345 = pirp;
int DeviceObject = __tmp_344;
int Irp = __tmp_345;
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
if (irpSp__MinorFunction == 0)
{
{
int __tmp_346 = DeviceObject;
int __tmp_347 = Irp;
int DeviceObject = __tmp_346;
int Irp = __tmp_347;
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
int __tmp_348 = DeviceObject;
int __tmp_349 = Irp;
int DeviceObject = __tmp_348;
int Irp = __tmp_349;
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int status9 ;
int irpSp__Control ;
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
irpSp__Control = 224;
{
int __tmp_350 = deviceExtension__TargetDeviceObject;
int __tmp_351 = Irp;
int DeviceObject = __tmp_350;
int Irp = __tmp_351;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_352 = DeviceObject;
int __tmp_353 = Irp;
int __tmp_354 = lcontext;
int DeviceObject = __tmp_352;
int Irp = __tmp_353;
int Context = __tmp_354;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_83985;
}
else 
{
{
__VERIFIER_error();
}
goto label_83985;
}
}
else 
{
label_83985:; 
if (myStatus >= 0)
{
{
int __tmp_355 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_355;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_84011;
}
else 
{
label_84011:; 
}
goto label_84003;
}
}
else 
{
label_84003:; 
 __return_84022 = myStatus;
}
compRetStatus = __return_84022;
goto label_83975;
}
}
}
else 
{
{
int __tmp_356 = DeviceObject;
int __tmp_357 = Irp;
int __tmp_358 = lcontext;
int DeviceObject = __tmp_356;
int Irp = __tmp_357;
int Event = __tmp_358;
{
int __tmp_359 = Event;
int __tmp_360 = 0;
int __tmp_361 = 0;
int Event = __tmp_359;
int Increment = __tmp_360;
int Wait = __tmp_361;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_83968 = l;
}
 __return_83971 = -1073741802;
}
compRetStatus = __return_83971;
label_83975:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_84046;
}
else 
{
{
__VERIFIER_error();
}
label_84046:; 
}
goto label_83953;
}
}
else 
{
goto label_83953;
}
}
}
else 
{
label_83953:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_84072;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_84072;
}
else 
{
returnVal2 = 259;
label_84072:; 
goto label_84059;
}
}
}
else 
{
returnVal2 = 259;
label_84059:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_84101;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_84111;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_84111:; 
goto label_84101;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_84101;
}
else 
{
{
__VERIFIER_error();
}
label_84101:; 
 __return_84122 = returnVal2;
}
status9 = __return_84122;
goto label_83940;
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
if (routine == 0)
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
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_83801;
}
else 
{
{
__VERIFIER_error();
}
goto label_83801;
}
}
else 
{
label_83801:; 
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
goto label_83827;
}
else 
{
label_83827:; 
}
goto label_83819;
}
}
else 
{
label_83819:; 
 __return_83838 = myStatus;
}
compRetStatus = __return_83838;
goto label_83791;
}
}
}
else 
{
{
int __tmp_368 = DeviceObject;
int __tmp_369 = Irp;
int __tmp_370 = lcontext;
int DeviceObject = __tmp_368;
int Irp = __tmp_369;
int Event = __tmp_370;
{
int __tmp_371 = Event;
int __tmp_372 = 0;
int __tmp_373 = 0;
int Event = __tmp_371;
int Increment = __tmp_372;
int Wait = __tmp_373;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_83784 = l;
}
 __return_83787 = -1073741802;
}
compRetStatus = __return_83787;
label_83791:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_83862;
}
else 
{
{
__VERIFIER_error();
}
label_83862:; 
}
goto label_83769;
}
}
else 
{
goto label_83769;
}
}
}
else 
{
label_83769:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_83888;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_83888;
}
else 
{
returnVal2 = 259;
label_83888:; 
goto label_83875;
}
}
}
else 
{
returnVal2 = 259;
label_83875:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_83917;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_83927;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_83927:; 
goto label_83917;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_83917;
}
else 
{
{
__VERIFIER_error();
}
label_83917:; 
 __return_83938 = returnVal2;
}
status9 = __return_83938;
label_83940:; 
status9 = 259;
if (!(status9 == 0))
{
{
int __tmp_374 = event;
int __tmp_375 = Executive;
int __tmp_376 = KernelMode;
int __tmp_377 = 0;
int __tmp_378 = 0;
int Object = __tmp_374;
int WaitReason = __tmp_375;
int WaitMode = __tmp_376;
int Alertable = __tmp_377;
int Timeout = __tmp_378;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_84150;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_84150;
}
else 
{
{
__VERIFIER_error();
}
goto label_84150;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_84150;
}
else 
{
label_84150:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_84186 = 0;
goto label_84182;
}
else 
{
 __return_84182 = -1073741823;
label_84182:; 
}
status9 = myStatus;
goto label_84128;
}
}
}
}
else 
{
label_84128:; 
 __return_84192 = status9;
}
status2 = __return_84192;
if (status2 < 0)
{
 __return_84381 = status2;
goto label_84376;
}
else 
{
if (deviceExtension__Active == 255)
{
cdb = srb__Cdb;
inquiryDataPtr = 0;
attempt = 0;
if (inquiryDataPtr == 0)
{
deviceExtension__Active = 0;
 __return_84379 = 0;
goto label_84376;
}
else 
{
status2 = -1073741823;
label_84207:; 
if (status2 < 0)
{
tmp = attempt;
int __CPAchecker_TMP_0 = attempt;
attempt = attempt + 1;
__CPAchecker_TMP_0;
if (tmp >= 4)
{
goto label_84213;
}
else 
{
{
int __tmp_379 = deviceExtension;
int __tmp_380 = srb;
int __tmp_381 = inquiryDataPtr;
int __tmp_382 = 36;
int Extension = __tmp_379;
int Srb = __tmp_380;
int Buffer = __tmp_381;
int BufferLength = __tmp_382;
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
goto label_84258;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_84258:; 
if (irp == 0)
{
 __return_84331 = -1073741670;
goto label_84332;
}
else 
{
__cil_tmp18 = (long)status1;
if (__cil_tmp18 == 259L)
{
{
int __tmp_383 = event;
int __tmp_384 = Executive;
int __tmp_385 = KernelMode;
int __tmp_386 = 0;
int __tmp_387 = 0;
int Object = __tmp_383;
int WaitReason = __tmp_384;
int WaitMode = __tmp_385;
int Alertable = __tmp_386;
int Timeout = __tmp_387;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_84286;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_84286;
}
else 
{
{
__VERIFIER_error();
}
goto label_84286;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_84286;
}
else 
{
label_84286:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_84322 = 0;
goto label_84318;
}
else 
{
 __return_84318 = -1073741823;
label_84318:; 
}
status1 = ioStatus__Status;
goto label_84264;
}
}
}
}
else 
{
label_84264:; 
 __return_84332 = status1;
label_84332:; 
}
status2 = __return_84332;
goto label_84207;
}
}
}
}
}
else 
{
label_84213:; 
if (status2 < 0)
{
deviceExtension__Active = 0;
 __return_84375 = 0;
goto label_84376;
}
else 
{
deviceExtension__Active = 0;
goto label_84199;
}
}
}
}
else 
{
label_84199:; 
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_84372 = 0;
goto label_84376;
}
else 
{
if (status2 < 0)
{
goto label_84349;
}
else 
{
label_84349:; 
{
int __tmp_388 = deviceParameterHandle;
int Handle = __tmp_388;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_84366 = 0;
goto label_84362;
}
else 
{
 __return_84362 = -1073741823;
label_84362:; 
}
 __return_84376 = 0;
label_84376:; 
}
status3 = __return_84376;
Irp__IoStatus__Status = status3;
myStatus = status3;
{
int __tmp_389 = Irp;
int __tmp_390 = 0;
int Irp = __tmp_389;
int PriorityBoost = __tmp_390;
if (s == NP)
{
s = DC;
goto label_84400;
}
else 
{
{
__VERIFIER_error();
}
label_84400:; 
}
 __return_84408 = status3;
goto label_83494;
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
if (irpSp__MinorFunction == 22)
{
if (irpSp__Parameters__UsageNotification__Type != DeviceUsageTypePaging)
{
{
int __tmp_391 = DeviceObject;
int __tmp_392 = Irp;
int DeviceObject = __tmp_391;
int Irp = __tmp_392;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_393 = deviceExtension__TargetDeviceObject;
int __tmp_394 = Irp;
int DeviceObject = __tmp_393;
int Irp = __tmp_394;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_395 = DeviceObject;
int __tmp_396 = Irp;
int __tmp_397 = lcontext;
int DeviceObject = __tmp_395;
int Irp = __tmp_396;
int Context = __tmp_397;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_83561;
}
else 
{
{
__VERIFIER_error();
}
goto label_83561;
}
}
else 
{
label_83561:; 
if (myStatus >= 0)
{
{
int __tmp_398 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_398;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_83587;
}
else 
{
label_83587:; 
}
goto label_83579;
}
}
else 
{
label_83579:; 
 __return_83598 = myStatus;
}
compRetStatus = __return_83598;
goto label_83551;
}
}
}
else 
{
{
int __tmp_399 = DeviceObject;
int __tmp_400 = Irp;
int __tmp_401 = lcontext;
int DeviceObject = __tmp_399;
int Irp = __tmp_400;
int Event = __tmp_401;
{
int __tmp_402 = Event;
int __tmp_403 = 0;
int __tmp_404 = 0;
int Event = __tmp_402;
int Increment = __tmp_403;
int Wait = __tmp_404;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_83544 = l;
}
 __return_83547 = -1073741802;
}
compRetStatus = __return_83547;
label_83551:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_83622;
}
else 
{
{
__VERIFIER_error();
}
label_83622:; 
}
goto label_83529;
}
}
else 
{
goto label_83529;
}
}
}
else 
{
label_83529:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_83648;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_83648;
}
else 
{
returnVal2 = 259;
label_83648:; 
goto label_83635;
}
}
}
else 
{
returnVal2 = 259;
label_83635:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_83677;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_83687;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_83687:; 
goto label_83677;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_83677;
}
else 
{
{
__VERIFIER_error();
}
label_83677:; 
 __return_83698 = returnVal2;
}
tmp = __return_83698;
 __return_83701 = tmp;
}
tmp = __return_83701;
 __return_83705 = tmp;
goto label_83494;
}
}
}
}
}
}
else 
{
{
int __tmp_405 = deviceExtension__PagingPathCountEvent;
int __tmp_406 = Executive;
int __tmp_407 = KernelMode;
int __tmp_408 = 0;
int __tmp_409 = 0;
int Object = __tmp_405;
int WaitReason = __tmp_406;
int WaitMode = __tmp_407;
int Alertable = __tmp_408;
int Timeout = __tmp_409;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_82927;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_82927;
}
else 
{
{
__VERIFIER_error();
}
goto label_82927;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_82927;
}
else 
{
label_82927:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_82963 = 0;
goto label_82959;
}
else 
{
 __return_82959 = -1073741823;
label_82959:; 
}
status3 = __return_82959;
setPagable = 0;
if (irpSp__Parameters__UsageNotification__InPath == 0)
{
goto label_82974;
}
else 
{
if (deviceExtension__PagingPathCount != 1)
{
label_82974:; 
setPagable = 1;
goto label_82972;
}
else 
{
label_82972:; 
{
int __tmp_410 = DeviceObject;
int __tmp_411 = Irp;
int DeviceObject = __tmp_410;
int Irp = __tmp_411;
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int event = __VERIFIER_nondet_int() ;
event = __VERIFIER_nondet_int();
int status9 ;
int irpSp__Control ;
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
irpSp__Control = 224;
{
int __tmp_412 = deviceExtension__TargetDeviceObject;
int __tmp_413 = Irp;
int DeviceObject = __tmp_412;
int Irp = __tmp_413;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_414 = DeviceObject;
int __tmp_415 = Irp;
int __tmp_416 = lcontext;
int DeviceObject = __tmp_414;
int Irp = __tmp_415;
int Context = __tmp_416;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_83241;
}
else 
{
{
__VERIFIER_error();
}
goto label_83241;
}
}
else 
{
label_83241:; 
if (myStatus >= 0)
{
{
int __tmp_417 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_417;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_83267;
}
else 
{
label_83267:; 
}
goto label_83259;
}
}
else 
{
label_83259:; 
 __return_83278 = myStatus;
}
compRetStatus = __return_83278;
goto label_83231;
}
}
}
else 
{
{
int __tmp_418 = DeviceObject;
int __tmp_419 = Irp;
int __tmp_420 = lcontext;
int DeviceObject = __tmp_418;
int Irp = __tmp_419;
int Event = __tmp_420;
{
int __tmp_421 = Event;
int __tmp_422 = 0;
int __tmp_423 = 0;
int Event = __tmp_421;
int Increment = __tmp_422;
int Wait = __tmp_423;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_83224 = l;
}
 __return_83227 = -1073741802;
}
compRetStatus = __return_83227;
label_83231:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_83302;
}
else 
{
{
__VERIFIER_error();
}
label_83302:; 
}
goto label_83209;
}
}
else 
{
goto label_83209;
}
}
}
else 
{
label_83209:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_83328;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_83328;
}
else 
{
returnVal2 = 259;
label_83328:; 
goto label_83315;
}
}
}
else 
{
returnVal2 = 259;
label_83315:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_83357;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_83367;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_83367:; 
goto label_83357;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_83357;
}
else 
{
{
__VERIFIER_error();
}
label_83357:; 
 __return_83378 = returnVal2;
}
status9 = __return_83378;
goto label_83196;
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
int __tmp_424 = deviceExtension__TargetDeviceObject;
int __tmp_425 = Irp;
int DeviceObject = __tmp_424;
int Irp = __tmp_425;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_426 = DeviceObject;
int __tmp_427 = Irp;
int __tmp_428 = lcontext;
int DeviceObject = __tmp_426;
int Irp = __tmp_427;
int Context = __tmp_428;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_83057;
}
else 
{
{
__VERIFIER_error();
}
goto label_83057;
}
}
else 
{
label_83057:; 
if (myStatus >= 0)
{
{
int __tmp_429 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_429;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_83083;
}
else 
{
label_83083:; 
}
goto label_83075;
}
}
else 
{
label_83075:; 
 __return_83094 = myStatus;
}
compRetStatus = __return_83094;
goto label_83047;
}
}
}
else 
{
{
int __tmp_430 = DeviceObject;
int __tmp_431 = Irp;
int __tmp_432 = lcontext;
int DeviceObject = __tmp_430;
int Irp = __tmp_431;
int Event = __tmp_432;
{
int __tmp_433 = Event;
int __tmp_434 = 0;
int __tmp_435 = 0;
int Event = __tmp_433;
int Increment = __tmp_434;
int Wait = __tmp_435;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_83040 = l;
}
 __return_83043 = -1073741802;
}
compRetStatus = __return_83043;
label_83047:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_83118;
}
else 
{
{
__VERIFIER_error();
}
label_83118:; 
}
goto label_83025;
}
}
else 
{
goto label_83025;
}
}
}
else 
{
label_83025:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_83144;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_83144;
}
else 
{
returnVal2 = 259;
label_83144:; 
goto label_83131;
}
}
}
else 
{
returnVal2 = 259;
label_83131:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_83173;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_83183;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_83183:; 
goto label_83173;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_83173;
}
else 
{
{
__VERIFIER_error();
}
label_83173:; 
 __return_83194 = returnVal2;
}
status9 = __return_83194;
label_83196:; 
status9 = 259;
if (!(status9 == 0))
{
{
int __tmp_436 = event;
int __tmp_437 = Executive;
int __tmp_438 = KernelMode;
int __tmp_439 = 0;
int __tmp_440 = 0;
int Object = __tmp_436;
int WaitReason = __tmp_437;
int WaitMode = __tmp_438;
int Alertable = __tmp_439;
int Timeout = __tmp_440;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_83406;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_83406;
}
else 
{
{
__VERIFIER_error();
}
goto label_83406;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_83406;
}
else 
{
label_83406:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_83442 = 0;
goto label_83438;
}
else 
{
 __return_83438 = -1073741823;
label_83438:; 
}
status9 = myStatus;
goto label_83384;
}
}
}
}
else 
{
label_83384:; 
 __return_83448 = status9;
}
status3 = __return_83448;
if (status3 >= 0)
{
goto label_83456;
}
else 
{
if (setPagable == 1)
{
setPagable = 0;
goto label_83456;
}
else 
{
label_83456:; 
{
int __tmp_441 = deviceExtension__PagingPathCountEvent;
int __tmp_442 = 0;
int __tmp_443 = 0;
int Event = __tmp_441;
int Increment = __tmp_442;
int Wait = __tmp_443;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_83468 = l;
}
{
int __tmp_444 = Irp;
int __tmp_445 = 0;
int Irp = __tmp_444;
int PriorityBoost = __tmp_445;
if (s == NP)
{
s = DC;
goto label_83485;
}
else 
{
{
__VERIFIER_error();
}
label_83485:; 
}
 __return_83493 = status3;
goto label_83494;
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
int __tmp_446 = DeviceObject;
int __tmp_447 = Irp;
int DeviceObject = __tmp_446;
int Irp = __tmp_447;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_448 = deviceExtension__TargetDeviceObject;
int __tmp_449 = Irp;
int DeviceObject = __tmp_448;
int Irp = __tmp_449;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_450 = DeviceObject;
int __tmp_451 = Irp;
int __tmp_452 = lcontext;
int DeviceObject = __tmp_450;
int Irp = __tmp_451;
int Context = __tmp_452;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_82757;
}
else 
{
{
__VERIFIER_error();
}
goto label_82757;
}
}
else 
{
label_82757:; 
if (myStatus >= 0)
{
{
int __tmp_453 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_453;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_82783;
}
else 
{
label_82783:; 
}
goto label_82775;
}
}
else 
{
label_82775:; 
 __return_82794 = myStatus;
}
compRetStatus = __return_82794;
goto label_82747;
}
}
}
else 
{
{
int __tmp_454 = DeviceObject;
int __tmp_455 = Irp;
int __tmp_456 = lcontext;
int DeviceObject = __tmp_454;
int Irp = __tmp_455;
int Event = __tmp_456;
{
int __tmp_457 = Event;
int __tmp_458 = 0;
int __tmp_459 = 0;
int Event = __tmp_457;
int Increment = __tmp_458;
int Wait = __tmp_459;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_82740 = l;
}
 __return_82743 = -1073741802;
}
compRetStatus = __return_82743;
label_82747:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_82818;
}
else 
{
{
__VERIFIER_error();
}
label_82818:; 
}
goto label_82725;
}
}
else 
{
goto label_82725;
}
}
}
else 
{
label_82725:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_82844;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_82844;
}
else 
{
returnVal2 = 259;
label_82844:; 
goto label_82831;
}
}
}
else 
{
returnVal2 = 259;
label_82831:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_82873;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_82883;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_82883:; 
goto label_82873;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_82873;
}
else 
{
{
__VERIFIER_error();
}
label_82873:; 
 __return_82894 = returnVal2;
}
tmp = __return_82894;
 __return_82897 = tmp;
}
tmp___0 = __return_82897;
 __return_83494 = tmp___0;
label_83494:; 
}
status10 = __return_83494;
goto label_82658;
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
if (tmp_ndt_3 == 4)
{
{
int __tmp_460 = devobj;
int __tmp_461 = pirp;
int DeviceObject = __tmp_460;
int Irp = __tmp_461;
int Irp__CurrentLocation = __VERIFIER_nondet_int() ;
Irp__CurrentLocation = __VERIFIER_nondet_int();
int Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int() ;
Irp__Tail__Overlay__CurrentStackLocation = __VERIFIER_nondet_int();
int deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int() ;
deviceExtension__TargetDeviceObject = __VERIFIER_nondet_int();
int tmp ;
s = SKIP1;
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_462 = deviceExtension__TargetDeviceObject;
int __tmp_463 = Irp;
int DeviceObject = __tmp_462;
int Irp = __tmp_463;
int compRetStatus ;
int returnVal ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
long __cil_tmp8 ;
if (!(compRegistered == 0))
{
if (routine == 0)
{
{
int __tmp_464 = DeviceObject;
int __tmp_465 = Irp;
int __tmp_466 = lcontext;
int DeviceObject = __tmp_464;
int Irp = __tmp_465;
int Context = __tmp_466;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_82519;
}
else 
{
{
__VERIFIER_error();
}
goto label_82519;
}
}
else 
{
label_82519:; 
if (myStatus >= 0)
{
{
int __tmp_467 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_467;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_82545;
}
else 
{
label_82545:; 
}
goto label_82537;
}
}
else 
{
label_82537:; 
 __return_82556 = myStatus;
}
compRetStatus = __return_82556;
goto label_82509;
}
}
}
else 
{
{
int __tmp_468 = DeviceObject;
int __tmp_469 = Irp;
int __tmp_470 = lcontext;
int DeviceObject = __tmp_468;
int Irp = __tmp_469;
int Event = __tmp_470;
{
int __tmp_471 = Event;
int __tmp_472 = 0;
int __tmp_473 = 0;
int Event = __tmp_471;
int Increment = __tmp_472;
int Wait = __tmp_473;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_82502 = l;
}
 __return_82505 = -1073741802;
}
compRetStatus = __return_82505;
label_82509:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_82580;
}
else 
{
{
__VERIFIER_error();
}
label_82580:; 
}
goto label_82487;
}
}
else 
{
goto label_82487;
}
}
}
else 
{
label_82487:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal = 0;
goto label_82601;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal = -1073741823;
goto label_82601;
}
else 
{
returnVal = 259;
label_82601:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_82629;
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
goto label_82640;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_82640:; 
goto label_82629;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
goto label_82629;
}
else 
{
{
__VERIFIER_error();
}
label_82629:; 
 __return_82651 = returnVal;
}
tmp = __return_82651;
 __return_82654 = tmp;
}
status10 = __return_82654;
label_82658:; 
if (we_should_unload == 0)
{
goto label_82404;
}
else 
{
{
int __tmp_474 = d;
int DriverObject = __tmp_474;
}
goto label_82404;
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
 __return_90035 = -1;
label_90035:; 
return 1;
}
}
}
}
}
}
else 
{
label_82404:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_89961;
}
else 
{
goto label_89945;
}
}
else 
{
label_89945:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_89961;
}
else 
{
goto label_89953;
}
}
else 
{
label_89953:; 
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
goto label_89961;
}
else 
{
goto label_89969;
}
}
else 
{
goto label_89969;
}
}
else 
{
label_89969:; 
if (pended != 1)
{
if (s == DC)
{
if (status10 == 259)
{
{
__VERIFIER_error();
}
goto label_90000;
}
else 
{
label_90000:; 
goto label_89961;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_89961;
}
else 
{
goto label_89961;
}
}
}
else 
{
goto label_89961;
}
}
}
else 
{
goto label_89961;
}
}
else 
{
label_89961:; 
 __return_90034 = status10;
goto label_90035;
}
}
}
}
}
