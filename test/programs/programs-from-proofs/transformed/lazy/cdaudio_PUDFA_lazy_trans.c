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
int __return_82610;
int __return_82234;
int __return_82232;
int __return_82229;
int __return_82206;
int __return_82202;
int __return_82226;
int __return_82230;
int __return_82484;
int __return_82488;
int __return_82365;
int __return_82356;
int __return_82352;
int __return_82366;
int __return_82449;
int __return_82453;
int __return_81879;
int __return_81877;
int __return_81874;
int __return_81851;
int __return_81847;
int __return_81871;
int __return_81875;
int __return_82112;
int __return_82116;
int __return_82002;
int __return_81993;
int __return_81989;
int __return_82003;
int __return_82049;
int __return_82053;
int __return_81746;
int __return_81750;
int __return_81710;
int __return_81714;
int __return_81665;
int __return_81656;
int __return_81652;
int __return_81666;
int __return_81533;
int __return_81524;
int __return_81520;
int __return_81534;
int __return_81404;
int __return_81395;
int __return_81391;
int __return_81405;
int __return_82414;
int __return_82418;
int __return_81279;
int __return_81270;
int __return_81266;
int __return_81280;
int __return_80932;
int __return_80930;
int __return_80927;
int __return_80904;
int __return_80900;
int __return_80924;
int __return_80928;
int __return_81040;
int __return_80986;
int __return_80989;
int __return_81140;
int __return_81143;
int __return_82419;
int __return_80314;
int __return_80312;
int __return_80309;
int __return_80286;
int __return_80282;
int __return_80306;
int __return_80310;
int __return_80554;
int __return_80438;
int __return_80429;
int __return_80425;
int __return_80439;
int __return_80520;
int __return_80027;
int __return_80018;
int __return_80014;
int __return_80028;
int __return_80193;
int __return_80153;
int __return_80144;
int __return_80140;
int __return_80154;
int __return_80593;
int __return_79886;
int __return_79877;
int __return_79873;
int __return_79887;
int __return_79768;
int __return_79736;
int __return_79515;
int __return_79506;
int __return_79502;
int __return_79516;
int __return_79704;
int __return_79633;
int __return_79624;
int __return_79620;
int __return_79634;
int __return_79672;
int __return_79394;
int __return_79349;
int __return_79340;
int __return_79336;
int __return_79350;
int __return_79227;
int __return_79193;
int __return_79140;
int __return_79131;
int __return_79127;
int __return_79141;
int __return_79013;
int __return_79004;
int __return_79000;
int __return_79014;
int __return_78630;
int __return_78628;
int __return_78625;
int __return_78602;
int __return_78598;
int __return_78622;
int __return_78626;
int __return_78881;
int __return_78739;
int __return_78685;
int __return_78688;
int __return_78839;
int __return_78842;
int __return_78846;
int __return_78405;
int __return_78351;
int __return_78354;
int __return_78505;
int __return_78508;
int __return_80521;
int __return_78018;
int __return_78009;
int __return_78005;
int __return_78019;
int __return_78075;
int __return_78049;
int __return_77796;
int __return_77742;
int __return_77745;
int __return_77896;
int __return_77899;
int __return_78050;
int __return_77552;
int __return_77498;
int __return_77501;
int __return_77652;
int __return_77656;
int __return_77343;
int __return_77289;
int __return_77292;
int __return_77443;
int __return_77446;
int __return_77657;
int __return_77108;
int __return_77054;
int __return_77057;
int __return_77208;
int __return_77211;
int __return_82496;
int __return_76595;
int __return_76541;
int __return_76544;
int __return_76695;
int __return_76411;
int __return_76357;
int __return_76360;
int __return_76511;
int __return_76759;
int __return_76755;
int __return_76765;
int __return_76954;
int __return_76952;
int __return_76904;
int __return_76895;
int __return_76891;
int __return_76905;
int __return_76948;
int __return_76945;
int __return_76939;
int __return_76935;
int __return_76949;
int __return_76981;
int __return_76171;
int __return_76117;
int __return_76120;
int __return_76271;
int __return_76274;
int __return_76278;
int __return_75536;
int __return_75532;
int __return_75851;
int __return_75797;
int __return_75800;
int __return_75951;
int __return_75667;
int __return_75613;
int __return_75616;
int __return_75767;
int __return_76015;
int __return_76011;
int __return_76021;
int __return_76041;
int __return_76066;
int __return_75367;
int __return_75313;
int __return_75316;
int __return_75467;
int __return_75470;
int __return_76067;
int __return_75129;
int __return_75075;
int __return_75078;
int __return_75224;
int __return_75227;
int __return_82608;
int __return_82607;
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
goto label_74989;
}
else 
{
label_74989:; 
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
 __return_82610 = -1;
goto label_82608;
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
 __return_82234 = 0;
goto label_82230;
}
else 
{
if (currentBuffer == 0)
{
 __return_82232 = 0;
goto label_82230;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_82229 = 0;
goto label_82230;
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
goto label_82170;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_82170;
}
else 
{
{
__VERIFIER_error();
}
goto label_82170;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_82170;
}
else 
{
label_82170:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_82206 = 0;
goto label_82202;
}
else 
{
 __return_82202 = -1073741823;
label_82202:; 
}
status5 = ioStatus__Status;
goto label_82148;
}
}
}
}
else 
{
label_82148:; 
if (status5 < 0)
{
 __return_82226 = 0;
goto label_82230;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_82219;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_82219:; 
 __return_82230 = returnValue;
label_82230:; 
}
tmp = __return_82230;
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
goto label_82477;
}
else 
{
{
__VERIFIER_error();
}
label_82477:; 
}
 __return_82484 = status;
}
tmp___0 = __return_82484;
 __return_82488 = tmp___0;
goto label_82419;
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
goto label_82292;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_82292:; 
if (irp == 0)
{
 __return_82365 = -1073741670;
goto label_82366;
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
goto label_82320;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_82320;
}
else 
{
{
__VERIFIER_error();
}
goto label_82320;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_82320;
}
else 
{
label_82320:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_82356 = 0;
goto label_82352;
}
else 
{
 __return_82352 = -1073741823;
label_82352:; 
}
status1 = ioStatus__Status;
goto label_82298;
}
}
}
}
else 
{
label_82298:; 
 __return_82366 = status1;
label_82366:; 
}
status6 = __return_82366;
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
goto label_82442;
}
else 
{
{
__VERIFIER_error();
}
label_82442:; 
}
 __return_82449 = status;
}
tmp___1 = __return_82449;
 __return_82453 = tmp___1;
goto label_82419;
}
}
else 
{
status6 = 0;
Irp__IoStatus__Information = bytesTransfered;
if (lastSession__LogicalBlockAddress == 0)
{
goto label_82243;
}
else 
{
cdaudioDataOut__FirstTrack = 1;
cdaudioDataOut__LastTrack = 2;
goto label_82243;
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
goto label_82243;
}
}
else 
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
goto label_82243;
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
 __return_81879 = 0;
goto label_81875;
}
else 
{
if (currentBuffer == 0)
{
 __return_81877 = 0;
goto label_81875;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_81874 = 0;
goto label_81875;
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
goto label_81815;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81815;
}
else 
{
{
__VERIFIER_error();
}
goto label_81815;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81815;
}
else 
{
label_81815:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_81851 = 0;
goto label_81847;
}
else 
{
 __return_81847 = -1073741823;
label_81847:; 
}
status5 = ioStatus__Status;
goto label_81793;
}
}
}
}
else 
{
label_81793:; 
if (status5 < 0)
{
 __return_81871 = 0;
goto label_81875;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_81864;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_81864:; 
 __return_81875 = returnValue;
label_81875:; 
}
tmp___2 = __return_81875;
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
goto label_82105;
}
else 
{
{
__VERIFIER_error();
}
label_82105:; 
}
 __return_82112 = status;
}
tmp___3 = __return_82112;
 __return_82116 = tmp___3;
goto label_82419;
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
goto label_81929;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_81929:; 
if (irp == 0)
{
 __return_82002 = -1073741670;
goto label_82003;
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
goto label_81957;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81957;
}
else 
{
{
__VERIFIER_error();
}
goto label_81957;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81957;
}
else 
{
label_81957:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_81993 = 0;
goto label_81989;
}
else 
{
 __return_81989 = -1073741823;
label_81989:; 
}
status1 = ioStatus__Status;
goto label_81935;
}
}
}
}
else 
{
label_81935:; 
 __return_82003 = status1;
label_82003:; 
}
status6 = __return_82003;
if (status6 >= 0)
{
__cil_tmp107 = (unsigned long)status6;
if (__cil_tmp107 != -1073741764)
{
status6 = 0;
goto label_82059;
}
else 
{
goto label_82009;
}
}
else 
{
label_82009:; 
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
goto label_82042;
}
else 
{
{
__VERIFIER_error();
}
label_82042:; 
}
 __return_82049 = status;
}
tmp___4 = __return_82049;
 __return_82053 = tmp___4;
goto label_82419;
}
}
else 
{
label_82059:; 
__cil_tmp109 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp109 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_82067;
}
else 
{
tracksToReturn = tracksOnCd;
label_82067:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_82073;
}
else 
{
label_82073:; 
goto label_82243;
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
goto label_82243;
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
goto label_82243;
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
goto label_82243;
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
goto label_81739;
}
else 
{
{
__VERIFIER_error();
}
label_81739:; 
}
 __return_81746 = status;
}
tmp___5 = __return_81746;
 __return_81750 = tmp___5;
goto label_82419;
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
goto label_81703;
}
else 
{
{
__VERIFIER_error();
}
label_81703:; 
}
 __return_81710 = status;
}
tmp___6 = __return_81710;
 __return_81714 = tmp___6;
goto label_82419;
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
goto label_81592;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_81592:; 
if (irp == 0)
{
 __return_81665 = -1073741670;
goto label_81666;
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
goto label_81620;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81620;
}
else 
{
{
__VERIFIER_error();
}
goto label_81620;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81620;
}
else 
{
label_81620:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_81656 = 0;
goto label_81652;
}
else 
{
 __return_81652 = -1073741823;
label_81652:; 
}
status1 = ioStatus__Status;
goto label_81598;
}
}
}
}
else 
{
label_81598:; 
 __return_81666 = status1;
label_81666:; 
}
status6 = __return_81666;
if (status6 >= 0)
{
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_81673;
}
else 
{
Irp__IoStatus__Information = 0;
label_81673:; 
goto label_82243;
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
goto label_82243;
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
goto label_81460;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_81460:; 
if (irp == 0)
{
 __return_81533 = -1073741670;
goto label_81534;
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
goto label_81488;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81488;
}
else 
{
{
__VERIFIER_error();
}
goto label_81488;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81488;
}
else 
{
label_81488:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_81524 = 0;
goto label_81520;
}
else 
{
 __return_81520 = -1073741823;
label_81520:; 
}
status1 = ioStatus__Status;
goto label_81466;
}
}
}
}
else 
{
label_81466:; 
 __return_81534 = status1;
label_81534:; 
}
status6 = __return_81534;
goto label_82385;
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
goto label_82243;
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
goto label_81331;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_81331:; 
if (irp == 0)
{
 __return_81404 = -1073741670;
goto label_81405;
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
goto label_81359;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81359;
}
else 
{
{
__VERIFIER_error();
}
goto label_81359;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81359;
}
else 
{
label_81359:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_81395 = 0;
goto label_81391;
}
else 
{
 __return_81391 = -1073741823;
label_81391:; 
}
status1 = ioStatus__Status;
goto label_81337;
}
}
}
}
else 
{
label_81337:; 
 __return_81405 = status1;
label_81405:; 
}
status6 = __return_81405;
label_82385:; 
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
goto label_82407;
}
else 
{
{
__VERIFIER_error();
}
label_82407:; 
}
 __return_82414 = status;
}
tmp___8 = __return_82414;
 __return_82418 = tmp___8;
goto label_82419;
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
goto label_81206;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_81206:; 
if (irp == 0)
{
 __return_81279 = -1073741670;
goto label_81280;
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
goto label_81234;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81234;
}
else 
{
{
__VERIFIER_error();
}
goto label_81234;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_81234;
}
else 
{
label_81234:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_81270 = 0;
goto label_81266;
}
else 
{
 __return_81266 = -1073741823;
label_81266:; 
}
status1 = ioStatus__Status;
goto label_81212;
}
}
}
}
else 
{
label_81212:; 
 __return_81280 = status1;
label_81280:; 
}
status6 = __return_81280;
goto label_82243;
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
goto label_81156;
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
label_81156:; 
goto label_81159;
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
label_81159:; 
Irp__IoStatus__Information = 0;
status6 = -1073741808;
label_82243:; 
goto label_82385;
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
 __return_80932 = 0;
goto label_80928;
}
else 
{
if (currentBuffer == 0)
{
 __return_80930 = 0;
goto label_80928;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_80927 = 0;
goto label_80928;
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
goto label_80868;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_80868;
}
else 
{
{
__VERIFIER_error();
}
goto label_80868;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_80868;
}
else 
{
label_80868:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_80904 = 0;
goto label_80900;
}
else 
{
 __return_80900 = -1073741823;
label_80900:; 
}
status5 = ioStatus__Status;
goto label_80846;
}
}
}
}
else 
{
label_80846:; 
if (status5 < 0)
{
 __return_80924 = 0;
goto label_80928;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_80917;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_80917:; 
 __return_80928 = returnValue;
label_80928:; 
}
goto label_80814;
}
}
}
}
}
}
}
else 
{
label_80814:; 
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
goto label_81003;
}
else 
{
{
__VERIFIER_error();
}
goto label_81003;
}
}
else 
{
label_81003:; 
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
goto label_81029;
}
else 
{
label_81029:; 
}
goto label_81021;
}
}
else 
{
label_81021:; 
 __return_81040 = myStatus;
}
compRetStatus = __return_81040;
goto label_80993;
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
 __return_80986 = l;
}
 __return_80989 = -1073741802;
}
compRetStatus = __return_80989;
label_80993:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_81064;
}
else 
{
{
__VERIFIER_error();
}
label_81064:; 
}
goto label_80971;
}
}
else 
{
goto label_80971;
}
}
}
else 
{
label_80971:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_81090;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_81090;
}
else 
{
returnVal2 = 259;
label_81090:; 
goto label_81077;
}
}
}
else 
{
returnVal2 = 259;
label_81077:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_81119;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_81129;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_81129:; 
goto label_81119;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_81119;
}
else 
{
{
__VERIFIER_error();
}
label_81119:; 
 __return_81140 = returnVal2;
}
tmp = __return_81140;
 __return_81143 = tmp;
}
tmp___7 = __return_81143;
 __return_82419 = tmp___7;
label_82419:; 
}
status4 = __return_82419;
goto label_77661;
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
goto label_80489;
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
 __return_80314 = 0;
goto label_80310;
}
else 
{
if (currentBuffer == 0)
{
 __return_80312 = 0;
goto label_80310;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_80309 = 0;
goto label_80310;
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
goto label_80250;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_80250;
}
else 
{
{
__VERIFIER_error();
}
goto label_80250;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_80250;
}
else 
{
label_80250:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_80286 = 0;
goto label_80282;
}
else 
{
 __return_80282 = -1073741823;
label_80282:; 
}
status5 = ioStatus__Status;
goto label_80228;
}
}
}
}
else 
{
label_80228:; 
if (status5 < 0)
{
 __return_80306 = 0;
goto label_80310;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_80299;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_80299:; 
 __return_80310 = returnValue;
label_80310:; 
}
tmp = __return_80310;
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
goto label_80527;
}
else 
{
label_80527:; 
myStatus = status7;
{
int __tmp_134 = Irp;
int __tmp_135 = 0;
int Irp = __tmp_134;
int PriorityBoost = __tmp_135;
if (s == NP)
{
s = DC;
goto label_80546;
}
else 
{
{
__VERIFIER_error();
}
label_80546:; 
}
 __return_80554 = status7;
goto label_80521;
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
goto label_80365;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_80365:; 
if (irp == 0)
{
 __return_80438 = -1073741670;
goto label_80439;
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
goto label_80393;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_80393;
}
else 
{
{
__VERIFIER_error();
}
goto label_80393;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_80393;
}
else 
{
label_80393:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_80429 = 0;
goto label_80425;
}
else 
{
 __return_80425 = -1073741823;
label_80425:; 
}
status1 = ioStatus__Status;
goto label_80371;
}
}
}
}
else 
{
label_80371:; 
 __return_80439 = status1;
label_80439:; 
}
status7 = __return_80439;
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
goto label_80493;
}
else 
{
label_80493:; 
myStatus = status7;
{
int __tmp_145 = Irp;
int __tmp_146 = 0;
int Irp = __tmp_145;
int PriorityBoost = __tmp_146;
if (s == NP)
{
s = DC;
goto label_80512;
}
else 
{
{
__VERIFIER_error();
}
label_80512:; 
}
 __return_80520 = status7;
goto label_80521;
}
}
}
else 
{
goto label_80452;
}
}
else 
{
status7 = 0;
label_80452:; 
goto label_80446;
}
}
else 
{
status7 = 0;
label_80446:; 
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength > sizeof__CDROM_TOC)
{
bytesTransfered = sizeof__CDROM_TOC;
goto label_80464;
}
else 
{
bytesTransfered = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
label_80464:; 
__cil_tmp98 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp98 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength - TrackData__0;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_80475;
}
else 
{
tracksToReturn = tracksOnCd;
label_80475:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_80481;
}
else 
{
label_80481:; 
goto label_80489;
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
goto label_80489;
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
goto label_79913;
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
label_79913:; 
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
goto label_79954;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_79954:; 
if (irp == 0)
{
 __return_80027 = -1073741670;
goto label_80028;
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
goto label_79982;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79982;
}
else 
{
{
__VERIFIER_error();
}
goto label_79982;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79982;
}
else 
{
label_79982:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_80018 = 0;
goto label_80014;
}
else 
{
 __return_80014 = -1073741823;
label_80014:; 
}
status1 = ioStatus__Status;
goto label_79960;
}
}
}
}
else 
{
label_79960:; 
 __return_80028 = status1;
label_80028:; 
}
status7 = __return_80028;
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
goto label_80166;
}
else 
{
label_80166:; 
myStatus = status7;
{
int __tmp_156 = Irp;
int __tmp_157 = 0;
int Irp = __tmp_156;
int PriorityBoost = __tmp_157;
if (s == NP)
{
s = DC;
goto label_80185;
}
else 
{
{
__VERIFIER_error();
}
label_80185:; 
}
 __return_80193 = status7;
goto label_80521;
}
}
}
else 
{
if (currentIrpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CDROM_PLAY_AUDIO_MSF)
{
status7 = -1073741820;
goto label_80489;
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
goto label_80080;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_80080:; 
if (irp == 0)
{
 __return_80153 = -1073741670;
goto label_80154;
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
goto label_80108;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_80108;
}
else 
{
{
__VERIFIER_error();
}
goto label_80108;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_80108;
}
else 
{
label_80108:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_80144 = 0;
goto label_80140;
}
else 
{
 __return_80140 = -1073741823;
label_80140:; 
}
status1 = ioStatus__Status;
goto label_80086;
}
}
}
}
else 
{
label_80086:; 
 __return_80154 = status1;
label_80154:; 
}
status7 = __return_80154;
label_80562:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_80566;
}
else 
{
label_80566:; 
myStatus = status7;
{
int __tmp_167 = Irp;
int __tmp_168 = 0;
int Irp = __tmp_167;
int PriorityBoost = __tmp_168;
if (s == NP)
{
s = DC;
goto label_80585;
}
else 
{
{
__VERIFIER_error();
}
label_80585:; 
}
 __return_80593 = status7;
goto label_80521;
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
goto label_80489;
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
goto label_79813;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_79813:; 
if (irp == 0)
{
 __return_79886 = -1073741670;
goto label_79887;
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
goto label_79841;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79841;
}
else 
{
{
__VERIFIER_error();
}
goto label_79841;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79841;
}
else 
{
label_79841:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_79877 = 0;
goto label_79873;
}
else 
{
 __return_79873 = -1073741823;
label_79873:; 
}
status1 = ioStatus__Status;
goto label_79819;
}
}
}
}
else 
{
label_79819:; 
 __return_79887 = status1;
label_79887:; 
}
status7 = __return_79887;
if (status7 < 0)
{
__cil_tmp105 = (unsigned long)status7;
if (__cil_tmp105 == -1073741808)
{
status7 = -1073741803;
goto label_79892;
}
else 
{
goto label_79892;
}
}
else 
{
label_79892:; 
goto label_80489;
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
goto label_79741;
}
else 
{
label_79741:; 
myStatus = status7;
{
int __tmp_178 = Irp;
int __tmp_179 = 0;
int Irp = __tmp_178;
int PriorityBoost = __tmp_179;
if (s == NP)
{
s = DC;
goto label_79760;
}
else 
{
{
__VERIFIER_error();
}
label_79760:; 
}
 __return_79768 = status7;
goto label_80521;
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
goto label_79709;
}
else 
{
label_79709:; 
myStatus = status7;
{
int __tmp_180 = Irp;
int __tmp_181 = 0;
int Irp = __tmp_180;
int PriorityBoost = __tmp_181;
if (s == NP)
{
s = DC;
goto label_79728;
}
else 
{
{
__VERIFIER_error();
}
label_79728:; 
}
 __return_79736 = status7;
goto label_80521;
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
goto label_79442;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_79442:; 
if (irp == 0)
{
 __return_79515 = -1073741670;
goto label_79516;
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
goto label_79470;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79470;
}
else 
{
{
__VERIFIER_error();
}
goto label_79470;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79470;
}
else 
{
label_79470:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_79506 = 0;
goto label_79502;
}
else 
{
 __return_79502 = -1073741823;
label_79502:; 
}
status1 = ioStatus__Status;
goto label_79448;
}
}
}
}
else 
{
label_79448:; 
 __return_79516 = status1;
label_79516:; 
}
status7 = __return_79516;
if (status7 < 0)
{
__cil_tmp109 = (unsigned long)status7;
if (__cil_tmp109 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_79677;
}
else 
{
label_79677:; 
myStatus = status7;
{
int __tmp_191 = Irp;
int __tmp_192 = 0;
int Irp = __tmp_191;
int PriorityBoost = __tmp_192;
if (s == NP)
{
s = DC;
goto label_79696;
}
else 
{
{
__VERIFIER_error();
}
label_79696:; 
}
 __return_79704 = status7;
goto label_80521;
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
goto label_79560;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_79560:; 
if (irp == 0)
{
 __return_79633 = -1073741670;
goto label_79634;
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
goto label_79588;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79588;
}
else 
{
{
__VERIFIER_error();
}
goto label_79588;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79588;
}
else 
{
label_79588:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_79624 = 0;
goto label_79620;
}
else 
{
 __return_79620 = -1073741823;
label_79620:; 
}
status1 = ioStatus__Status;
goto label_79566;
}
}
}
}
else 
{
label_79566:; 
 __return_79634 = status1;
label_79634:; 
}
status7 = __return_79634;
if (status7 < 0)
{
__cil_tmp111 = (unsigned long)status7;
if (__cil_tmp111 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_79645;
}
else 
{
label_79645:; 
myStatus = status7;
{
int __tmp_202 = Irp;
int __tmp_203 = 0;
int Irp = __tmp_202;
int PriorityBoost = __tmp_203;
if (s == NP)
{
s = DC;
goto label_79664;
}
else 
{
{
__VERIFIER_error();
}
label_79664:; 
}
 __return_79672 = status7;
goto label_80521;
}
}
}
else 
{
goto label_80489;
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
goto label_79367;
}
else 
{
label_79367:; 
myStatus = status7;
{
int __tmp_204 = Irp;
int __tmp_205 = 0;
int Irp = __tmp_204;
int PriorityBoost = __tmp_205;
if (s == NP)
{
s = DC;
goto label_79386;
}
else 
{
{
__VERIFIER_error();
}
label_79386:; 
}
 __return_79394 = status7;
goto label_80521;
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
goto label_79276;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_79276:; 
if (irp == 0)
{
 __return_79349 = -1073741670;
goto label_79350;
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
goto label_79304;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79304;
}
else 
{
{
__VERIFIER_error();
}
goto label_79304;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79304;
}
else 
{
label_79304:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_79340 = 0;
goto label_79336;
}
else 
{
 __return_79336 = -1073741823;
label_79336:; 
}
status1 = ioStatus__Status;
goto label_79282;
}
}
}
}
else 
{
label_79282:; 
 __return_79350 = status1;
label_79350:; 
}
status7 = __return_79350;
if (status7 >= 0)
{
deviceExtension__PlayActive = 1;
deviceExtension__Paused = 0;
goto label_79355;
}
else 
{
label_79355:; 
goto label_80489;
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
goto label_80489;
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
goto label_79200;
}
else 
{
label_79200:; 
myStatus = status7;
{
int __tmp_215 = Irp;
int __tmp_216 = 0;
int Irp = __tmp_215;
int PriorityBoost = __tmp_216;
if (s == NP)
{
s = DC;
goto label_79219;
}
else 
{
{
__VERIFIER_error();
}
label_79219:; 
}
 __return_79227 = status7;
goto label_80521;
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
goto label_79166;
}
else 
{
label_79166:; 
myStatus = status7;
{
int __tmp_217 = Irp;
int __tmp_218 = 0;
int Irp = __tmp_217;
int PriorityBoost = __tmp_218;
if (s == NP)
{
s = DC;
goto label_79185;
}
else 
{
{
__VERIFIER_error();
}
label_79185:; 
}
 __return_79193 = status7;
goto label_80521;
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
goto label_79067;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_79067:; 
if (irp == 0)
{
 __return_79140 = -1073741670;
goto label_79141;
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
goto label_79095;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79095;
}
else 
{
{
__VERIFIER_error();
}
goto label_79095;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_79095;
}
else 
{
label_79095:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_79131 = 0;
goto label_79127;
}
else 
{
 __return_79127 = -1073741823;
label_79127:; 
}
status1 = ioStatus__Status;
goto label_79073;
}
}
}
}
else 
{
label_79073:; 
 __return_79141 = status1;
label_79141:; 
}
status7 = __return_79141;
if (status7 >= 0)
{
if (deviceExtension__Paused == 1)
{
deviceExtension__PlayActive = 0;
goto label_79151;
}
else 
{
label_79151:; 
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_79148;
}
}
else 
{
Irp__IoStatus__Information = 0;
label_79148:; 
goto label_80489;
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
goto label_78940;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_78940:; 
if (irp == 0)
{
 __return_79013 = -1073741670;
goto label_79014;
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
goto label_78968;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_78968;
}
else 
{
{
__VERIFIER_error();
}
goto label_78968;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_78968;
}
else 
{
label_78968:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_79004 = 0;
goto label_79000;
}
else 
{
 __return_79000 = -1073741823;
label_79000:; 
}
status1 = ioStatus__Status;
goto label_78946;
}
}
}
}
else 
{
label_78946:; 
 __return_79014 = status1;
label_79014:; 
}
status7 = __return_79014;
goto label_80489;
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
goto label_78890;
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
label_78890:; 
goto label_78893;
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
label_78893:; 
Irp__IoStatus__Information = 0;
status7 = -1073741808;
label_80489:; 
goto label_80562;
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
 __return_78630 = 0;
goto label_78626;
}
else 
{
if (currentBuffer == 0)
{
 __return_78628 = 0;
goto label_78626;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_78625 = 0;
goto label_78626;
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
goto label_78566;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_78566;
}
else 
{
{
__VERIFIER_error();
}
goto label_78566;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_78566;
}
else 
{
label_78566:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_78602 = 0;
goto label_78598;
}
else 
{
 __return_78598 = -1073741823;
label_78598:; 
}
status5 = ioStatus__Status;
goto label_78544;
}
}
}
}
else 
{
label_78544:; 
if (status5 < 0)
{
 __return_78622 = 0;
goto label_78626;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_78615;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_78615:; 
 __return_78626 = returnValue;
label_78626:; 
}
tmp___1 = __return_78626;
if (tmp___1 == 1)
{
deviceExtension__PlayActive = 1;
status7 = 0;
Irp__IoStatus__Information = 0;
__cil_tmp115 = (unsigned long)status7;
if (__cil_tmp115 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_78854;
}
else 
{
label_78854:; 
myStatus = status7;
{
int __tmp_243 = Irp;
int __tmp_244 = 0;
int Irp = __tmp_243;
int PriorityBoost = __tmp_244;
if (s == NP)
{
s = DC;
goto label_78873;
}
else 
{
{
__VERIFIER_error();
}
label_78873:; 
}
 __return_78881 = status7;
goto label_80521;
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
goto label_78702;
}
else 
{
{
__VERIFIER_error();
}
goto label_78702;
}
}
else 
{
label_78702:; 
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
goto label_78728;
}
else 
{
label_78728:; 
}
goto label_78720;
}
}
else 
{
label_78720:; 
 __return_78739 = myStatus;
}
compRetStatus = __return_78739;
goto label_78692;
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
 __return_78685 = l;
}
 __return_78688 = -1073741802;
}
compRetStatus = __return_78688;
label_78692:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_78763;
}
else 
{
{
__VERIFIER_error();
}
label_78763:; 
}
goto label_78670;
}
}
else 
{
goto label_78670;
}
}
}
else 
{
label_78670:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_78789;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_78789;
}
else 
{
returnVal2 = 259;
label_78789:; 
goto label_78776;
}
}
}
else 
{
returnVal2 = 259;
label_78776:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_78818;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_78828;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_78828:; 
goto label_78818;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_78818;
}
else 
{
{
__VERIFIER_error();
}
label_78818:; 
 __return_78839 = returnVal2;
}
tmp = __return_78839;
 __return_78842 = tmp;
}
tmp___0 = __return_78842;
 __return_78846 = tmp___0;
goto label_80521;
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
goto label_78368;
}
else 
{
{
__VERIFIER_error();
}
goto label_78368;
}
}
else 
{
label_78368:; 
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
goto label_78394;
}
else 
{
label_78394:; 
}
goto label_78386;
}
}
else 
{
label_78386:; 
 __return_78405 = myStatus;
}
compRetStatus = __return_78405;
goto label_78358;
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
 __return_78351 = l;
}
 __return_78354 = -1073741802;
}
compRetStatus = __return_78354;
label_78358:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_78429;
}
else 
{
{
__VERIFIER_error();
}
label_78429:; 
}
goto label_78336;
}
}
else 
{
goto label_78336;
}
}
}
else 
{
label_78336:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_78455;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_78455;
}
else 
{
returnVal2 = 259;
label_78455:; 
goto label_78442;
}
}
}
else 
{
returnVal2 = 259;
label_78442:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_78484;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_78494;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_78494:; 
goto label_78484;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_78484;
}
else 
{
{
__VERIFIER_error();
}
label_78484:; 
 __return_78505 = returnVal2;
}
tmp = __return_78505;
 __return_78508 = tmp;
}
tmp___2 = __return_78508;
 __return_80521 = tmp___2;
label_80521:; 
}
status4 = __return_80521;
goto label_77661;
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
goto label_77945;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_77945:; 
if (irp == 0)
{
 __return_78018 = -1073741670;
goto label_78019;
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
goto label_77973;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_77973;
}
else 
{
{
__VERIFIER_error();
}
goto label_77973;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_77973;
}
else 
{
label_77973:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_78009 = 0;
goto label_78005;
}
else 
{
 __return_78005 = -1073741823;
label_78005:; 
}
status1 = ioStatus__Status;
goto label_77951;
}
}
}
}
else 
{
label_77951:; 
 __return_78019 = status1;
label_78019:; 
}
status8 = __return_78019;
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
goto label_78067;
}
else 
{
{
__VERIFIER_error();
}
label_78067:; 
}
 __return_78075 = status8;
goto label_78050;
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
goto label_78041;
}
else 
{
{
__VERIFIER_error();
}
label_78041:; 
}
 __return_78049 = status8;
goto label_78050;
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
goto label_77759;
}
else 
{
{
__VERIFIER_error();
}
goto label_77759;
}
}
else 
{
label_77759:; 
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
goto label_77785;
}
else 
{
label_77785:; 
}
goto label_77777;
}
}
else 
{
label_77777:; 
 __return_77796 = myStatus;
}
compRetStatus = __return_77796;
goto label_77749;
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
 __return_77742 = l;
}
 __return_77745 = -1073741802;
}
compRetStatus = __return_77745;
label_77749:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_77820;
}
else 
{
{
__VERIFIER_error();
}
label_77820:; 
}
goto label_77727;
}
}
else 
{
goto label_77727;
}
}
}
else 
{
label_77727:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_77846;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_77846;
}
else 
{
returnVal2 = 259;
label_77846:; 
goto label_77833;
}
}
}
else 
{
returnVal2 = 259;
label_77833:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_77875;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_77885;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_77885:; 
goto label_77875;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_77875;
}
else 
{
{
__VERIFIER_error();
}
label_77875:; 
 __return_77896 = returnVal2;
}
tmp = __return_77896;
 __return_77899 = tmp;
}
tmp = __return_77899;
 __return_78050 = tmp;
label_78050:; 
}
status4 = __return_78050;
goto label_77661;
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
goto label_77458;
}
else 
{
compRegistered = 1;
routine = 0;
label_77458:; 
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
goto label_77515;
}
else 
{
{
__VERIFIER_error();
}
goto label_77515;
}
}
else 
{
label_77515:; 
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
goto label_77541;
}
else 
{
label_77541:; 
}
goto label_77533;
}
}
else 
{
label_77533:; 
 __return_77552 = myStatus;
}
compRetStatus = __return_77552;
goto label_77505;
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
 __return_77498 = l;
}
 __return_77501 = -1073741802;
}
compRetStatus = __return_77501;
label_77505:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_77576;
}
else 
{
{
__VERIFIER_error();
}
label_77576:; 
}
goto label_77483;
}
}
else 
{
goto label_77483;
}
}
}
else 
{
label_77483:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_77602;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_77602;
}
else 
{
returnVal2 = 259;
label_77602:; 
goto label_77589;
}
}
}
else 
{
returnVal2 = 259;
label_77589:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_77631;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_77641;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_77641:; 
goto label_77631;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_77631;
}
else 
{
{
__VERIFIER_error();
}
label_77631:; 
 __return_77652 = returnVal2;
}
tmp = __return_77652;
 __return_77656 = tmp;
goto label_77657;
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
goto label_77306;
}
else 
{
{
__VERIFIER_error();
}
goto label_77306;
}
}
else 
{
label_77306:; 
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
goto label_77332;
}
else 
{
label_77332:; 
}
goto label_77324;
}
}
else 
{
label_77324:; 
 __return_77343 = myStatus;
}
compRetStatus = __return_77343;
goto label_77296;
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
 __return_77289 = l;
}
 __return_77292 = -1073741802;
}
compRetStatus = __return_77292;
label_77296:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_77367;
}
else 
{
{
__VERIFIER_error();
}
label_77367:; 
}
goto label_77274;
}
}
else 
{
goto label_77274;
}
}
}
else 
{
label_77274:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_77393;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_77393;
}
else 
{
returnVal2 = 259;
label_77393:; 
goto label_77380;
}
}
}
else 
{
returnVal2 = 259;
label_77380:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_77422;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_77432;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_77432:; 
goto label_77422;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_77422;
}
else 
{
{
__VERIFIER_error();
}
label_77422:; 
 __return_77443 = returnVal2;
}
tmp = __return_77443;
 __return_77446 = tmp;
}
tmp___0 = __return_77446;
 __return_77657 = tmp___0;
label_77657:; 
}
status4 = __return_77657;
label_77661:; 
goto label_77215;
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
goto label_77071;
}
else 
{
{
__VERIFIER_error();
}
goto label_77071;
}
}
else 
{
label_77071:; 
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
goto label_77097;
}
else 
{
label_77097:; 
}
goto label_77089;
}
}
else 
{
label_77089:; 
 __return_77108 = myStatus;
}
compRetStatus = __return_77108;
goto label_77061;
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
 __return_77054 = l;
}
 __return_77057 = -1073741802;
}
compRetStatus = __return_77057;
label_77061:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_77132;
}
else 
{
{
__VERIFIER_error();
}
label_77132:; 
}
goto label_77039;
}
}
else 
{
goto label_77039;
}
}
}
else 
{
label_77039:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_77158;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_77158;
}
else 
{
returnVal2 = 259;
label_77158:; 
goto label_77145;
}
}
}
else 
{
returnVal2 = 259;
label_77145:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_77187;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_77197;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_77197:; 
goto label_77187;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_77187;
}
else 
{
{
__VERIFIER_error();
}
label_77187:; 
 __return_77208 = returnVal2;
}
tmp = __return_77208;
 __return_77211 = tmp;
}
status4 = __return_77211;
label_77215:; 
 __return_82496 = status4;
}
status10 = __return_82496;
goto label_75231;
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
goto label_76558;
}
else 
{
{
__VERIFIER_error();
}
goto label_76558;
}
}
else 
{
label_76558:; 
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
goto label_76584;
}
else 
{
label_76584:; 
}
goto label_76576;
}
}
else 
{
label_76576:; 
 __return_76595 = myStatus;
}
compRetStatus = __return_76595;
goto label_76548;
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
 __return_76541 = l;
}
 __return_76544 = -1073741802;
}
compRetStatus = __return_76544;
label_76548:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_76619;
}
else 
{
{
__VERIFIER_error();
}
label_76619:; 
}
goto label_76526;
}
}
else 
{
goto label_76526;
}
}
}
else 
{
label_76526:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_76645;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_76645;
}
else 
{
returnVal2 = 259;
label_76645:; 
goto label_76632;
}
}
}
else 
{
returnVal2 = 259;
label_76632:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_76674;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_76684;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_76684:; 
goto label_76674;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_76674;
}
else 
{
{
__VERIFIER_error();
}
label_76674:; 
 __return_76695 = returnVal2;
}
status9 = __return_76695;
goto label_76513;
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
goto label_76374;
}
else 
{
{
__VERIFIER_error();
}
goto label_76374;
}
}
else 
{
label_76374:; 
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
goto label_76400;
}
else 
{
label_76400:; 
}
goto label_76392;
}
}
else 
{
label_76392:; 
 __return_76411 = myStatus;
}
compRetStatus = __return_76411;
goto label_76364;
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
 __return_76357 = l;
}
 __return_76360 = -1073741802;
}
compRetStatus = __return_76360;
label_76364:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_76435;
}
else 
{
{
__VERIFIER_error();
}
label_76435:; 
}
goto label_76342;
}
}
else 
{
goto label_76342;
}
}
}
else 
{
label_76342:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_76461;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_76461;
}
else 
{
returnVal2 = 259;
label_76461:; 
goto label_76448;
}
}
}
else 
{
returnVal2 = 259;
label_76448:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_76490;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_76500;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_76500:; 
goto label_76490;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_76490;
}
else 
{
{
__VERIFIER_error();
}
label_76490:; 
 __return_76511 = returnVal2;
}
status9 = __return_76511;
label_76513:; 
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
goto label_76723;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_76723;
}
else 
{
{
__VERIFIER_error();
}
goto label_76723;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_76723;
}
else 
{
label_76723:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_76759 = 0;
goto label_76755;
}
else 
{
 __return_76755 = -1073741823;
label_76755:; 
}
status9 = myStatus;
goto label_76701;
}
}
}
}
else 
{
label_76701:; 
 __return_76765 = status9;
}
status2 = __return_76765;
if (status2 < 0)
{
 __return_76954 = status2;
goto label_76949;
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
 __return_76952 = 0;
goto label_76949;
}
else 
{
status2 = -1073741823;
label_76780:; 
if (status2 < 0)
{
tmp = attempt;
int __CPAchecker_TMP_0 = attempt;
attempt = attempt + 1;
__CPAchecker_TMP_0;
if (tmp >= 4)
{
goto label_76786;
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
goto label_76831;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
label_76831:; 
if (irp == 0)
{
 __return_76904 = -1073741670;
goto label_76905;
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
goto label_76859;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_76859;
}
else 
{
{
__VERIFIER_error();
}
goto label_76859;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_76859;
}
else 
{
label_76859:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_76895 = 0;
goto label_76891;
}
else 
{
 __return_76891 = -1073741823;
label_76891:; 
}
status1 = ioStatus__Status;
goto label_76837;
}
}
}
}
else 
{
label_76837:; 
 __return_76905 = status1;
label_76905:; 
}
status2 = __return_76905;
goto label_76780;
}
}
}
}
}
else 
{
label_76786:; 
if (status2 < 0)
{
deviceExtension__Active = 0;
 __return_76948 = 0;
goto label_76949;
}
else 
{
deviceExtension__Active = 0;
goto label_76772;
}
}
}
}
else 
{
label_76772:; 
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_76945 = 0;
goto label_76949;
}
else 
{
if (status2 < 0)
{
goto label_76922;
}
else 
{
label_76922:; 
{
int __tmp_388 = deviceParameterHandle;
int Handle = __tmp_388;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_76939 = 0;
goto label_76935;
}
else 
{
 __return_76935 = -1073741823;
label_76935:; 
}
 __return_76949 = 0;
label_76949:; 
}
status3 = __return_76949;
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
goto label_76973;
}
else 
{
{
__VERIFIER_error();
}
label_76973:; 
}
 __return_76981 = status3;
goto label_76067;
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
goto label_76134;
}
else 
{
{
__VERIFIER_error();
}
goto label_76134;
}
}
else 
{
label_76134:; 
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
goto label_76160;
}
else 
{
label_76160:; 
}
goto label_76152;
}
}
else 
{
label_76152:; 
 __return_76171 = myStatus;
}
compRetStatus = __return_76171;
goto label_76124;
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
 __return_76117 = l;
}
 __return_76120 = -1073741802;
}
compRetStatus = __return_76120;
label_76124:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_76195;
}
else 
{
{
__VERIFIER_error();
}
label_76195:; 
}
goto label_76102;
}
}
else 
{
goto label_76102;
}
}
}
else 
{
label_76102:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_76221;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_76221;
}
else 
{
returnVal2 = 259;
label_76221:; 
goto label_76208;
}
}
}
else 
{
returnVal2 = 259;
label_76208:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_76250;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_76260;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_76260:; 
goto label_76250;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_76250;
}
else 
{
{
__VERIFIER_error();
}
label_76250:; 
 __return_76271 = returnVal2;
}
tmp = __return_76271;
 __return_76274 = tmp;
}
tmp = __return_76274;
 __return_76278 = tmp;
goto label_76067;
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
goto label_75500;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_75500;
}
else 
{
{
__VERIFIER_error();
}
goto label_75500;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_75500;
}
else 
{
label_75500:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_75536 = 0;
goto label_75532;
}
else 
{
 __return_75532 = -1073741823;
label_75532:; 
}
status3 = __return_75532;
setPagable = 0;
if (irpSp__Parameters__UsageNotification__InPath == 0)
{
goto label_75547;
}
else 
{
if (deviceExtension__PagingPathCount != 1)
{
label_75547:; 
setPagable = 1;
goto label_75545;
}
else 
{
label_75545:; 
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
goto label_75814;
}
else 
{
{
__VERIFIER_error();
}
goto label_75814;
}
}
else 
{
label_75814:; 
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
goto label_75840;
}
else 
{
label_75840:; 
}
goto label_75832;
}
}
else 
{
label_75832:; 
 __return_75851 = myStatus;
}
compRetStatus = __return_75851;
goto label_75804;
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
 __return_75797 = l;
}
 __return_75800 = -1073741802;
}
compRetStatus = __return_75800;
label_75804:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_75875;
}
else 
{
{
__VERIFIER_error();
}
label_75875:; 
}
goto label_75782;
}
}
else 
{
goto label_75782;
}
}
}
else 
{
label_75782:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_75901;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_75901;
}
else 
{
returnVal2 = 259;
label_75901:; 
goto label_75888;
}
}
}
else 
{
returnVal2 = 259;
label_75888:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_75930;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_75940;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_75940:; 
goto label_75930;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_75930;
}
else 
{
{
__VERIFIER_error();
}
label_75930:; 
 __return_75951 = returnVal2;
}
status9 = __return_75951;
goto label_75769;
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
goto label_75630;
}
else 
{
{
__VERIFIER_error();
}
goto label_75630;
}
}
else 
{
label_75630:; 
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
goto label_75656;
}
else 
{
label_75656:; 
}
goto label_75648;
}
}
else 
{
label_75648:; 
 __return_75667 = myStatus;
}
compRetStatus = __return_75667;
goto label_75620;
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
 __return_75613 = l;
}
 __return_75616 = -1073741802;
}
compRetStatus = __return_75616;
label_75620:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_75691;
}
else 
{
{
__VERIFIER_error();
}
label_75691:; 
}
goto label_75598;
}
}
else 
{
goto label_75598;
}
}
}
else 
{
label_75598:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_75717;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_75717;
}
else 
{
returnVal2 = 259;
label_75717:; 
goto label_75704;
}
}
}
else 
{
returnVal2 = 259;
label_75704:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_75746;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_75756;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_75756:; 
goto label_75746;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_75746;
}
else 
{
{
__VERIFIER_error();
}
label_75746:; 
 __return_75767 = returnVal2;
}
status9 = __return_75767;
label_75769:; 
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
goto label_75979;
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_75979;
}
else 
{
{
__VERIFIER_error();
}
goto label_75979;
}
}
}
else 
{
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_75979;
}
else 
{
label_75979:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_76015 = 0;
goto label_76011;
}
else 
{
 __return_76011 = -1073741823;
label_76011:; 
}
status9 = myStatus;
goto label_75957;
}
}
}
}
else 
{
label_75957:; 
 __return_76021 = status9;
}
status3 = __return_76021;
if (status3 >= 0)
{
goto label_76029;
}
else 
{
if (setPagable == 1)
{
setPagable = 0;
goto label_76029;
}
else 
{
label_76029:; 
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
 __return_76041 = l;
}
{
int __tmp_444 = Irp;
int __tmp_445 = 0;
int Irp = __tmp_444;
int PriorityBoost = __tmp_445;
if (s == NP)
{
s = DC;
goto label_76058;
}
else 
{
{
__VERIFIER_error();
}
label_76058:; 
}
 __return_76066 = status3;
goto label_76067;
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
goto label_75330;
}
else 
{
{
__VERIFIER_error();
}
goto label_75330;
}
}
else 
{
label_75330:; 
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
goto label_75356;
}
else 
{
label_75356:; 
}
goto label_75348;
}
}
else 
{
label_75348:; 
 __return_75367 = myStatus;
}
compRetStatus = __return_75367;
goto label_75320;
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
 __return_75313 = l;
}
 __return_75316 = -1073741802;
}
compRetStatus = __return_75316;
label_75320:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_75391;
}
else 
{
{
__VERIFIER_error();
}
label_75391:; 
}
goto label_75298;
}
}
else 
{
goto label_75298;
}
}
}
else 
{
label_75298:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_75417;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_75417;
}
else 
{
returnVal2 = 259;
label_75417:; 
goto label_75404;
}
}
}
else 
{
returnVal2 = 259;
label_75404:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_75446;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_75456;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_75456:; 
goto label_75446;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
goto label_75446;
}
else 
{
{
__VERIFIER_error();
}
label_75446:; 
 __return_75467 = returnVal2;
}
tmp = __return_75467;
 __return_75470 = tmp;
}
tmp___0 = __return_75470;
 __return_76067 = tmp___0;
label_76067:; 
}
status10 = __return_76067;
goto label_75231;
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
goto label_75092;
}
else 
{
{
__VERIFIER_error();
}
goto label_75092;
}
}
else 
{
label_75092:; 
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
goto label_75118;
}
else 
{
label_75118:; 
}
goto label_75110;
}
}
else 
{
label_75110:; 
 __return_75129 = myStatus;
}
compRetStatus = __return_75129;
goto label_75082;
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
 __return_75075 = l;
}
 __return_75078 = -1073741802;
}
compRetStatus = __return_75078;
label_75082:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
if (s == NP)
{
s = MPR1;
goto label_75153;
}
else 
{
{
__VERIFIER_error();
}
label_75153:; 
}
goto label_75060;
}
}
else 
{
goto label_75060;
}
}
}
else 
{
label_75060:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal = 0;
goto label_75174;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal = -1073741823;
goto label_75174;
}
else 
{
returnVal = 259;
label_75174:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_75202;
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
goto label_75213;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_75213:; 
goto label_75202;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
goto label_75202;
}
else 
{
{
__VERIFIER_error();
}
label_75202:; 
 __return_75224 = returnVal;
}
tmp = __return_75224;
 __return_75227 = tmp;
}
status10 = __return_75227;
label_75231:; 
if (we_should_unload == 0)
{
goto label_74977;
}
else 
{
{
int __tmp_474 = d;
int DriverObject = __tmp_474;
}
goto label_74977;
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
 __return_82608 = -1;
label_82608:; 
return 1;
}
}
}
}
}
}
else 
{
label_74977:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_82534;
}
else 
{
goto label_82518;
}
}
else 
{
label_82518:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_82534;
}
else 
{
goto label_82526;
}
}
else 
{
label_82526:; 
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
goto label_82534;
}
else 
{
goto label_82542;
}
}
else 
{
goto label_82542;
}
}
else 
{
label_82542:; 
if (pended != 1)
{
if (s == DC)
{
if (status10 == 259)
{
{
__VERIFIER_error();
}
goto label_82573;
}
else 
{
label_82573:; 
goto label_82534;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_82534;
}
else 
{
goto label_82534;
}
}
}
else 
{
goto label_82534;
}
}
}
else 
{
goto label_82534;
}
}
else 
{
label_82534:; 
 __return_82607 = status10;
goto label_82608;
}
}
}
}
}
