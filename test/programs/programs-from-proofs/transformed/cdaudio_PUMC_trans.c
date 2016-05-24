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
int __return_125899;
int __return_124884;
int __return_124882;
int __return_124885;
int __return_125400;
int __return_125403;
int __return_125443;
int __return_124962;
int __return_125365;
int __return_125368;
int __return_125036;
int __return_125334;
int __return_124960;
int __return_124961;
int __return_124959;
int __return_125068;
int __return_125333;
int __return_124847;
int __return_124848;
int __return_124849;
int __return_124850;
int __return_124880;
int __return_124877;
int __return_124879;
int __return_124878;
int __return_124565;
int __return_124563;
int __return_124566;
int __return_124761;
int __return_124764;
int __return_124636;
int __return_125100;
int __return_125332;
int __return_124687;
int __return_124690;
int __return_125132;
int __return_125331;
int __return_124634;
int __return_124635;
int __return_124633;
int __return_125164;
int __return_125330;
int __return_124528;
int __return_124529;
int __return_124530;
int __return_124531;
int __return_124561;
int __return_124558;
int __return_124560;
int __return_124559;
int __return_125260;
int __return_125327;
int __return_124434;
int __return_124437;
int __return_124399;
int __return_124402;
int __return_124351;
int __return_125196;
int __return_125329;
int __return_124349;
int __return_124350;
int __return_124348;
int __return_124278;
int __return_125292;
int __return_124276;
int __return_124277;
int __return_124275;
int __return_124207;
int __return_125324;
int __return_124205;
int __return_124206;
int __return_124204;
int __return_124139;
int __return_125228;
int __return_125328;
int __return_124137;
int __return_124138;
int __return_124136;
int __return_122325;
int __return_122323;
int __return_122326;
int __return_122775;
int __return_122776;
int __return_123201;
int __return_124070;
int __return_122464;
int __return_122465;
int __return_122466;
int __return_122772;
int __return_122774;
int __return_122399;
int __return_122402;
int __return_122773;
int __return_123189;
int __return_123190;
int __return_122878;
int __return_122879;
int __return_122880;
int __return_123186;
int __return_123188;
int __return_122813;
int __return_122816;
int __return_123187;
int __return_122288;
int __return_122289;
int __return_122290;
int __return_122291;
int __return_122321;
int __return_122318;
int __return_122320;
int __return_122319;
int __return_123641;
int __return_123642;
int __return_124067;
int __return_123330;
int __return_123331;
int __return_123332;
int __return_123638;
int __return_123640;
int __return_123265;
int __return_123268;
int __return_123639;
int __return_124055;
int __return_124056;
int __return_123744;
int __return_123745;
int __return_123746;
int __return_124052;
int __return_124054;
int __return_123679;
int __return_123682;
int __return_124053;
int __return_121955;
int __return_121454;
int __return_121452;
int __return_121455;
int __return_121636;
int __return_121525;
int __return_121604;
int __return_121954;
int __return_121523;
int __return_121524;
int __return_121522;
int __return_121417;
int __return_121418;
int __return_121419;
int __return_121420;
int __return_121450;
int __return_121447;
int __return_121449;
int __return_121448;
int __return_121220;
int __return_121332;
int __return_121291;
int __return_121956;
int __return_125444;
int __return_121289;
int __return_121290;
int __return_121288;
int __return_121218;
int __return_121219;
int __return_121217;
int __return_121139;
int __return_121137;
int __return_121138;
int __return_121136;
int __return_121081;
int __return_121050;
int __return_120885;
int __return_121019;
int __return_120948;
int __return_120989;
int __return_120946;
int __return_120947;
int __return_120945;
int __return_120883;
int __return_120884;
int __return_120882;
int __return_120825;
int __return_120779;
int __return_120777;
int __return_120778;
int __return_120776;
int __return_120718;
int __return_120686;
int __return_120633;
int __return_120631;
int __return_120632;
int __return_120630;
int __return_120563;
int __return_120561;
int __return_120562;
int __return_120560;
int __return_119107;
int __return_119105;
int __return_119108;
int __return_120494;
int __return_120414;
int __return_120415;
int __return_120426;
int __return_120103;
int __return_120104;
int __return_120105;
int __return_120411;
int __return_120413;
int __return_120038;
int __return_120041;
int __return_120412;
int __return_119070;
int __return_119071;
int __return_119072;
int __return_119073;
int __return_119103;
int __return_119562;
int __return_119563;
int __return_119988;
int __return_120429;
int __return_119251;
int __return_119252;
int __return_119253;
int __return_119559;
int __return_119561;
int __return_119186;
int __return_119189;
int __return_119560;
int __return_119976;
int __return_119977;
int __return_119665;
int __return_119666;
int __return_119667;
int __return_119973;
int __return_119975;
int __return_119600;
int __return_119603;
int __return_119974;
int __return_119100;
int __return_119102;
int __return_119101;
int __return_118972;
int __return_118973;
int __return_118984;
int __return_118987;
int __return_118661;
int __return_118662;
int __return_118663;
int __return_118969;
int __return_118971;
int __return_118596;
int __return_118599;
int __return_118970;
int __return_118258;
int __return_118319;
int __return_125445;
int __return_118294;
int __return_118256;
int __return_118257;
int __return_118255;
int __return_118188;
int __return_118189;
int __return_118200;
int __return_118203;
int __return_117877;
int __return_117878;
int __return_117879;
int __return_118185;
int __return_118187;
int __return_117812;
int __return_117815;
int __return_118186;
int __return_117716;
int __return_117717;
int __return_117405;
int __return_117406;
int __return_117407;
int __return_117713;
int __return_117715;
int __return_117340;
int __return_117343;
int __return_117714;
int __return_117302;
int __return_117303;
int __return_117728;
int __return_125446;
int __return_116991;
int __return_116992;
int __return_116993;
int __return_117299;
int __return_117301;
int __return_116926;
int __return_116929;
int __return_117300;
int __return_116865;
int __return_116866;
int __return_116877;
int __return_116880;
int __return_116554;
int __return_116555;
int __return_116556;
int __return_116862;
int __return_116864;
int __return_116489;
int __return_116492;
int __return_116863;
int __return_116402;
int __return_116403;
int __return_116414;
int __return_125447;
int __return_125891;
int __return_116091;
int __return_116092;
int __return_116093;
int __return_116399;
int __return_116401;
int __return_116026;
int __return_116029;
int __return_116400;
int __return_115684;
int __return_115685;
int __return_115373;
int __return_115374;
int __return_115375;
int __return_115681;
int __return_115683;
int __return_115308;
int __return_115311;
int __return_115682;
int __return_115270;
int __return_115271;
int __return_114959;
int __return_114960;
int __return_114961;
int __return_115267;
int __return_115269;
int __return_114894;
int __return_114897;
int __return_115268;
int __return_114856;
int __return_114857;
int __return_115761;
int __return_115911;
int __return_115950;
int __return_125889;
int __return_115910;
int __return_115840;
int __return_115838;
int __return_115839;
int __return_115837;
int __return_115908;
int __return_115905;
int __return_115883;
int __return_115884;
int __return_115904;
int __return_115906;
int __return_115899;
int __return_115900;
int __return_115903;
int __return_115750;
int __return_115751;
int __return_115752;
int __return_115753;
int __return_115760;
int __return_114545;
int __return_114546;
int __return_114547;
int __return_114853;
int __return_114855;
int __return_114480;
int __return_114483;
int __return_114854;
int __return_114380;
int __return_114381;
int __return_114392;
int __return_114395;
int __return_114069;
int __return_114070;
int __return_114071;
int __return_114377;
int __return_114379;
int __return_114004;
int __return_114007;
int __return_114378;
int __return_112542;
int __return_112543;
int __return_112544;
int __return_112545;
int __return_113833;
int __return_113834;
int __return_113522;
int __return_113523;
int __return_113524;
int __return_113830;
int __return_113832;
int __return_113457;
int __return_113460;
int __return_113831;
int __return_113419;
int __return_113420;
int __return_113108;
int __return_113109;
int __return_113110;
int __return_113416;
int __return_113418;
int __return_113043;
int __return_113046;
int __return_113417;
int __return_113005;
int __return_113006;
int __return_113910;
int __return_113931;
int __return_113956;
int __return_113899;
int __return_113900;
int __return_113901;
int __return_113902;
int __return_113909;
int __return_112694;
int __return_112695;
int __return_112696;
int __return_113002;
int __return_113004;
int __return_112629;
int __return_112632;
int __return_113003;
int __return_112471;
int __return_112472;
int __return_112483;
int __return_112486;
int __return_112160;
int __return_112161;
int __return_112162;
int __return_112468;
int __return_112470;
int __return_112095;
int __return_112098;
int __return_112469;
int __return_111994;
int __return_111999;
int __return_112012;
int __return_125887;
int __return_125895;
int __return_125885;
int __return_111701;
int __return_111702;
int __return_111703;
int __return_111997;
int __return_111998;
int __return_111995;
int __return_111636;
int __return_111639;
int __return_111996;
int __return_111587;
int __return_125893;
int __return_125897;
int __return_125883;
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
goto label_111556;
}
else 
{
label_111556:; 
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
 __return_125899 = -1;
goto label_111587;
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
 __return_124884 = 0;
goto label_124885;
}
else 
{
if (currentBuffer == 0)
{
 __return_124882 = 0;
goto label_124885;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_124885 = 0;
label_124885:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (__cil_tmp10 == 259L)
{
{
int __tmp_30 = event;
int __tmp_31 = Suspended;
int __tmp_32 = KernelMode;
int __tmp_33 = 0;
int __tmp_34 = 0;
int Object = __tmp_30;
int WaitReason = __tmp_31;
int WaitMode = __tmp_32;
int Alertable = __tmp_33;
int Timeout = __tmp_34;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_124828;
}
else 
{
goto label_124805;
}
}
else 
{
label_124805:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_124828;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_124847 = 0;
goto label_124848;
}
else 
{
 __return_124848 = -1073741823;
label_124848:; 
}
goto label_124852;
}
else 
{
label_124828:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_124849 = 0;
goto label_124850;
}
else 
{
 __return_124850 = -1073741823;
label_124850:; 
}
label_124852:; 
status5 = ioStatus__Status;
if (status5 < 0)
{
 __return_124880 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_124876;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_124876:; 
 __return_124877 = returnValue;
}
tmp = __return_124877;
goto label_124887;
}
tmp = __return_124880;
goto label_124887;
}
}
}
}
}
else 
{
if (status5 < 0)
{
 __return_124879 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_124874;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_124874:; 
 __return_124878 = returnValue;
}
tmp = __return_124878;
goto label_124887;
}
tmp = __return_124879;
goto label_124887;
}
}
tmp = __return_124885;
label_124887:; 
if (tmp == 0)
{
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength == 0)
{
if (lastSession == 0)
{
status6 = -1073741670;
Irp__IoStatus__Information = 0;
{
int __tmp_6 = status6;
int __tmp_7 = Irp;
int __tmp_8 = deviceExtension__TargetDeviceObject;
int status = __tmp_6;
int Irp = __tmp_7;
int deviceExtension__TargetDeviceObject = __tmp_8;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_9 = Irp;
int __tmp_10 = 0;
int Irp = __tmp_9;
int PriorityBoost = __tmp_10;
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
goto label_125397;
label_125397:; 
 __return_125400 = status;
}
tmp___0 = __return_125400;
 __return_125403 = tmp___0;
}
status4 = __return_125403;
label_125405:; 
 __return_125443 = status4;
}
else 
{
srb__CdbLength = 10;
cdb__CDB10__OperationCode = 38;
srb__TimeOutValue = 10;
{
int __tmp_11 = deviceExtension;
int __tmp_12 = srb;
int __tmp_13 = lastSession;
int __tmp_14 = sizeof__READ_CAPACITY_DATA;
int Extension = __tmp_11;
int Srb = __tmp_12;
int Buffer = __tmp_13;
int BufferLength = __tmp_14;
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
if (irp == 0)
{
 __return_124962 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124960 = status1;
}
status6 = __return_124962;
label_124964:; 
if (status6 < 0)
{
Irp__IoStatus__Information = 0;
{
int __tmp_15 = status6;
int __tmp_16 = Irp;
int __tmp_17 = deviceExtension__TargetDeviceObject;
int status = __tmp_15;
int Irp = __tmp_16;
int deviceExtension__TargetDeviceObject = __tmp_17;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_18 = Irp;
int __tmp_19 = 0;
int Irp = __tmp_18;
int PriorityBoost = __tmp_19;
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
goto label_125362;
label_125362:; 
 __return_125365 = status;
}
tmp___1 = __return_125365;
 __return_125368 = tmp___1;
}
status4 = __return_125368;
goto label_125405;
}
else 
{
status6 = 0;
Irp__IoStatus__Information = bytesTransfered;
if (lastSession__LogicalBlockAddress == 0)
{
goto label_124982;
}
else 
{
cdaudioDataOut__FirstTrack = 1;
cdaudioDataOut__LastTrack = 2;
label_124982:; 
{
int __tmp_20 = status6;
int __tmp_21 = Irp;
int __tmp_22 = deviceExtension__TargetDeviceObject;
int status = __tmp_20;
int Irp = __tmp_21;
int deviceExtension__TargetDeviceObject = __tmp_22;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_23 = Irp;
int __tmp_24 = 0;
int Irp = __tmp_23;
int PriorityBoost = __tmp_24;
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
goto label_125033;
label_125033:; 
 __return_125036 = status;
}
tmp___8 = __return_125036;
 __return_125334 = tmp___8;
}
status4 = __return_125334;
goto label_125405;
}
}
status6 = __return_124960;
goto label_124964;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_124961 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124959 = status1;
}
status6 = __return_124961;
goto label_124964;
status6 = __return_124959;
goto label_124964;
}
}
}
status10 = __return_125443;
goto label_125449;
}
else 
{
status6 = -1073741789;
Irp__IoStatus__Information = 0;
goto label_124906;
}
}
else 
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
label_124906:; 
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
}
else 
{
{
__VERIFIER_error();
}
}
goto label_125065;
label_125065:; 
 __return_125068 = status;
}
tmp___8 = __return_125068;
 __return_125333 = tmp___8;
}
status4 = __return_125333;
goto label_125405;
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
int __tmp_35 = DeviceObject;
int DeviceObject = __tmp_35;
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
 __return_124565 = 0;
goto label_124566;
}
else 
{
if (currentBuffer == 0)
{
 __return_124563 = 0;
goto label_124566;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_124566 = 0;
label_124566:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (__cil_tmp10 == 259L)
{
{
int __tmp_65 = event;
int __tmp_66 = Suspended;
int __tmp_67 = KernelMode;
int __tmp_68 = 0;
int __tmp_69 = 0;
int Object = __tmp_65;
int WaitReason = __tmp_66;
int WaitMode = __tmp_67;
int Alertable = __tmp_68;
int Timeout = __tmp_69;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_124509;
}
else 
{
goto label_124486;
}
}
else 
{
label_124486:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_124509;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_124528 = 0;
goto label_124529;
}
else 
{
 __return_124529 = -1073741823;
label_124529:; 
}
goto label_124533;
}
else 
{
label_124509:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_124530 = 0;
goto label_124531;
}
else 
{
 __return_124531 = -1073741823;
label_124531:; 
}
label_124533:; 
status5 = ioStatus__Status;
if (status5 < 0)
{
 __return_124561 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_124557;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_124557:; 
 __return_124558 = returnValue;
}
tmp___2 = __return_124558;
goto label_124568;
}
tmp___2 = __return_124561;
goto label_124568;
}
}
}
}
}
else 
{
if (status5 < 0)
{
 __return_124560 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_124555;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_124555:; 
 __return_124559 = returnValue;
}
tmp___2 = __return_124559;
goto label_124568;
}
tmp___2 = __return_124560;
goto label_124568;
}
}
tmp___2 = __return_124566;
label_124568:; 
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
}
else 
{
{
__VERIFIER_error();
}
}
goto label_124758;
label_124758:; 
 __return_124761 = status;
}
tmp___3 = __return_124761;
 __return_124764 = tmp___3;
}
status4 = __return_124764;
goto label_125405;
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
if (irp == 0)
{
 __return_124636 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124634 = status1;
}
status6 = __return_124636;
label_124638:; 
if (status6 >= 0)
{
__cil_tmp107 = (unsigned long)status6;
if (__cil_tmp107 != -1073741764)
{
status6 = 0;
__cil_tmp109 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp109 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_124713;
}
else 
{
tracksToReturn = tracksOnCd;
label_124713:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_124725;
}
else 
{
label_124725:; 
{
int __tmp_45 = status6;
int __tmp_46 = Irp;
int __tmp_47 = deviceExtension__TargetDeviceObject;
int status = __tmp_45;
int Irp = __tmp_46;
int deviceExtension__TargetDeviceObject = __tmp_47;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_48 = Irp;
int __tmp_49 = 0;
int Irp = __tmp_48;
int PriorityBoost = __tmp_49;
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
goto label_125097;
label_125097:; 
 __return_125100 = status;
}
tmp___8 = __return_125100;
 __return_125332 = tmp___8;
}
status4 = __return_125332;
goto label_125405;
}
}
}
else 
{
goto label_124652;
}
}
else 
{
label_124652:; 
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
}
else 
{
{
__VERIFIER_error();
}
}
goto label_124684;
label_124684:; 
 __return_124687 = status;
}
tmp___4 = __return_124687;
 __return_124690 = tmp___4;
}
status4 = __return_124690;
goto label_125405;
}
else 
{
__cil_tmp109 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp109 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_124711;
}
else 
{
tracksToReturn = tracksOnCd;
label_124711:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_124727;
}
else 
{
label_124727:; 
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
}
else 
{
{
__VERIFIER_error();
}
}
goto label_125129;
label_125129:; 
 __return_125132 = status;
}
tmp___8 = __return_125132;
 __return_125331 = tmp___8;
}
status4 = __return_125331;
goto label_125405;
}
}
}
}
status6 = __return_124634;
goto label_124638;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_124635 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124633 = status1;
}
status6 = __return_124635;
goto label_124638;
status6 = __return_124633;
goto label_124638;
}
}
}
}
else 
{
status6 = -2147483631;
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
}
else 
{
{
__VERIFIER_error();
}
}
goto label_125161;
label_125161:; 
 __return_125164 = status;
}
tmp___8 = __return_125164;
 __return_125330 = tmp___8;
}
status4 = __return_125330;
goto label_125405;
}
}
}
}
}
else 
{
status6 = -1073741789;
Irp__IoStatus__Information = 0;
label_124998:; 
{
int __tmp_70 = status6;
int __tmp_71 = Irp;
int __tmp_72 = deviceExtension__TargetDeviceObject;
int status = __tmp_70;
int Irp = __tmp_71;
int deviceExtension__TargetDeviceObject = __tmp_72;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_73 = Irp;
int __tmp_74 = 0;
int Irp = __tmp_73;
int PriorityBoost = __tmp_74;
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
goto label_125257;
label_125257:; 
 __return_125260 = status;
}
tmp___8 = __return_125260;
label_125262:; 
 __return_125327 = tmp___8;
}
status4 = __return_125327;
goto label_125405;
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
goto label_124998;
}
else 
{
if (SubQPtr == 0)
{
status6 = -1073741670;
Irp__IoStatus__Information = 0;
{
int __tmp_75 = status6;
int __tmp_76 = Irp;
int __tmp_77 = deviceExtension__TargetDeviceObject;
int status = __tmp_75;
int Irp = __tmp_76;
int deviceExtension__TargetDeviceObject = __tmp_77;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_78 = Irp;
int __tmp_79 = 0;
int Irp = __tmp_78;
int PriorityBoost = __tmp_79;
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
goto label_124431;
label_124431:; 
 __return_124434 = status;
}
tmp___5 = __return_124434;
 __return_124437 = tmp___5;
}
status4 = __return_124437;
goto label_125405;
}
else 
{
if (userPtr__Format != 1)
{
status6 = -1073741823;
Irp__IoStatus__Information = 0;
{
int __tmp_80 = status6;
int __tmp_81 = Irp;
int __tmp_82 = deviceExtension__TargetDeviceObject;
int status = __tmp_80;
int Irp = __tmp_81;
int deviceExtension__TargetDeviceObject = __tmp_82;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_83 = Irp;
int __tmp_84 = 0;
int Irp = __tmp_83;
int PriorityBoost = __tmp_84;
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
goto label_124396;
label_124396:; 
 __return_124399 = status;
}
tmp___6 = __return_124399;
 __return_124402 = tmp___6;
}
status4 = __return_124402;
goto label_125405;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_85 = deviceExtension;
int __tmp_86 = srb;
int __tmp_87 = SubQPtr;
int __tmp_88 = sizeof__SUB_Q_CURRENT_POSITION;
int Extension = __tmp_85;
int Srb = __tmp_86;
int Buffer = __tmp_87;
int BufferLength = __tmp_88;
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
if (irp == 0)
{
 __return_124351 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124349 = status1;
}
status6 = __return_124351;
label_124353:; 
if (status6 >= 0)
{
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_124366;
}
else 
{
Irp__IoStatus__Information = 0;
label_124366:; 
{
int __tmp_89 = status6;
int __tmp_90 = Irp;
int __tmp_91 = deviceExtension__TargetDeviceObject;
int status = __tmp_89;
int Irp = __tmp_90;
int deviceExtension__TargetDeviceObject = __tmp_91;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_92 = Irp;
int __tmp_93 = 0;
int Irp = __tmp_92;
int PriorityBoost = __tmp_93;
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
goto label_125193;
label_125193:; 
 __return_125196 = status;
}
tmp___8 = __return_125196;
 __return_125329 = tmp___8;
}
status4 = __return_125329;
goto label_125405;
}
status6 = __return_124349;
goto label_124353;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_124350 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124348 = status1;
}
status6 = __return_124350;
goto label_124353;
status6 = __return_124348;
goto label_124353;
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
goto label_124998;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_94 = deviceExtension;
int __tmp_95 = srb;
int __tmp_96 = 0;
int __tmp_97 = 0;
int Extension = __tmp_94;
int Srb = __tmp_95;
int Buffer = __tmp_96;
int BufferLength = __tmp_97;
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
if (irp == 0)
{
 __return_124278 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124276 = status1;
}
status6 = __return_124278;
label_124280:; 
{
int __tmp_98 = status6;
int __tmp_99 = Irp;
int __tmp_100 = deviceExtension__TargetDeviceObject;
int status = __tmp_98;
int Irp = __tmp_99;
int deviceExtension__TargetDeviceObject = __tmp_100;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_101 = Irp;
int __tmp_102 = 0;
int Irp = __tmp_101;
int PriorityBoost = __tmp_102;
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
goto label_125289;
label_125289:; 
 __return_125292 = status;
}
tmp___8 = __return_125292;
goto label_125262;
}
status6 = __return_124276;
goto label_124280;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_124277 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124275 = status1;
}
status6 = __return_124277;
goto label_124280;
status6 = __return_124275;
goto label_124280;
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
goto label_124998;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_103 = deviceExtension;
int __tmp_104 = srb;
int __tmp_105 = 0;
int __tmp_106 = 0;
int Extension = __tmp_103;
int Srb = __tmp_104;
int Buffer = __tmp_105;
int BufferLength = __tmp_106;
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
if (irp == 0)
{
 __return_124207 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124205 = status1;
}
status6 = __return_124207;
label_124209:; 
{
int __tmp_107 = status6;
int __tmp_108 = Irp;
int __tmp_109 = deviceExtension__TargetDeviceObject;
int status = __tmp_107;
int Irp = __tmp_108;
int deviceExtension__TargetDeviceObject = __tmp_109;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_110 = Irp;
int __tmp_111 = 0;
int Irp = __tmp_110;
int PriorityBoost = __tmp_111;
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
goto label_125321;
label_125321:; 
 __return_125324 = status;
}
tmp___8 = __return_125324;
goto label_125262;
}
status6 = __return_124205;
goto label_124209;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_124206 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124204 = status1;
}
status6 = __return_124206;
goto label_124209;
status6 = __return_124204;
goto label_124209;
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
int __tmp_112 = deviceExtension;
int __tmp_113 = srb;
int __tmp_114 = 0;
int __tmp_115 = 0;
int Extension = __tmp_112;
int Srb = __tmp_113;
int Buffer = __tmp_114;
int BufferLength = __tmp_115;
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
if (irp == 0)
{
 __return_124139 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124137 = status1;
}
status6 = __return_124139;
label_124141:; 
{
int __tmp_116 = status6;
int __tmp_117 = Irp;
int __tmp_118 = deviceExtension__TargetDeviceObject;
int status = __tmp_116;
int Irp = __tmp_117;
int deviceExtension__TargetDeviceObject = __tmp_118;
unsigned long __cil_tmp4 ;
__cil_tmp4 = (unsigned long)status;
myStatus = status;
{
int __tmp_119 = Irp;
int __tmp_120 = 0;
int Irp = __tmp_119;
int PriorityBoost = __tmp_120;
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
goto label_125225;
label_125225:; 
 __return_125228 = status;
}
tmp___8 = __return_125228;
 __return_125328 = tmp___8;
}
status4 = __return_125328;
goto label_125405;
status6 = __return_124137;
goto label_124141;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_124138 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124136 = status1;
}
status6 = __return_124138;
goto label_124141;
status6 = __return_124136;
goto label_124141;
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
label_124078:; 
Irp__IoStatus__Information = 0;
label_124081:; 
status6 = -1073741808;
goto label_124998;
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
goto label_124078;
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
Irp__IoStatus__Information = 0;
goto label_124081;
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
int __tmp_121 = DeviceObject;
int DeviceObject = __tmp_121;
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
 __return_122325 = 0;
goto label_122326;
}
else 
{
if (currentBuffer == 0)
{
 __return_122323 = 0;
goto label_122326;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_122326 = 0;
label_122326:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (__cil_tmp10 == 259L)
{
{
int __tmp_150 = event;
int __tmp_151 = Suspended;
int __tmp_152 = KernelMode;
int __tmp_153 = 0;
int __tmp_154 = 0;
int Object = __tmp_150;
int WaitReason = __tmp_151;
int WaitMode = __tmp_152;
int Alertable = __tmp_153;
int Timeout = __tmp_154;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_122269;
}
else 
{
goto label_122246;
}
}
else 
{
label_122246:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_122269;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_122288 = 0;
goto label_122289;
}
else 
{
 __return_122289 = -1073741823;
label_122289:; 
}
goto label_122293;
}
else 
{
label_122269:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_122290 = 0;
goto label_122291;
}
else 
{
 __return_122291 = -1073741823;
label_122291:; 
}
label_122293:; 
status5 = ioStatus__Status;
if (status5 < 0)
{
 __return_122321 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_122317;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_122317:; 
 __return_122318 = returnValue;
}
goto label_122328;
}
goto label_122328;
}
}
}
}
}
else 
{
if (status5 < 0)
{
 __return_122320 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_122315;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_122315:; 
 __return_122319 = returnValue;
}
goto label_122328;
}
goto label_122328;
}
}
label_122328:; 
{
int __tmp_122 = DeviceObject;
int __tmp_123 = Irp;
int DeviceObject = __tmp_122;
int Irp = __tmp_123;
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
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_124 = deviceExtension__TargetDeviceObject;
int __tmp_125 = Irp;
int DeviceObject = __tmp_124;
int Irp = __tmp_125;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_122624;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_122624;
}
else 
{
returnVal2 = 259;
label_122624:; 
goto label_122636;
}
}
}
else 
{
returnVal2 = 259;
label_122636:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_122765;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_122741;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_122741:; 
goto label_122765;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_122765:; 
 __return_122775 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_122674:; 
 __return_122776 = returnVal2;
}
tmp = __return_122775;
goto label_122778;
tmp = __return_122776;
label_122778:; 
 __return_123201 = tmp;
}
tmp___7 = __return_123201;
label_123203:; 
 __return_124070 = tmp___7;
}
status4 = __return_124070;
goto label_125405;
}
}
else 
{
if (routine == 0)
{
{
int __tmp_126 = DeviceObject;
int __tmp_127 = Irp;
int __tmp_128 = lcontext;
int DeviceObject = __tmp_126;
int Irp = __tmp_127;
int Context = __tmp_128;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_122428;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_129 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_129;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_122444;
}
else 
{
label_122444:; 
}
label_122447:; 
 __return_122464 = myStatus;
}
compRetStatus = __return_122464;
goto label_122468;
}
else 
{
 __return_122465 = myStatus;
}
compRetStatus = __return_122465;
goto label_122468;
}
}
else 
{
label_122428:; 
if (myStatus >= 0)
{
{
int __tmp_130 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_130;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_122459;
}
else 
{
label_122459:; 
}
goto label_122447;
}
}
else 
{
 __return_122466 = myStatus;
}
compRetStatus = __return_122466;
label_122468:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_122501;
label_122501:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_122630;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_122630;
}
else 
{
returnVal2 = 259;
label_122630:; 
goto label_122642;
}
}
}
else 
{
returnVal2 = 259;
label_122642:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_122771;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_122747;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_122747:; 
goto label_122771;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_122771:; 
 __return_122772 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_122674;
}
tmp = __return_122772;
goto label_122778;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_122626;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_122626;
}
else 
{
returnVal2 = 259;
label_122626:; 
goto label_122638;
}
}
}
else 
{
returnVal2 = 259;
label_122638:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_122767;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_122743;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_122743:; 
goto label_122767;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_122767:; 
 __return_122774 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_122674;
}
tmp = __return_122774;
goto label_122778;
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
int __tmp_131 = DeviceObject;
int __tmp_132 = Irp;
int __tmp_133 = lcontext;
int DeviceObject = __tmp_131;
int Irp = __tmp_132;
int Event = __tmp_133;
{
int __tmp_134 = Event;
int __tmp_135 = 0;
int __tmp_136 = 0;
int Event = __tmp_134;
int Increment = __tmp_135;
int Wait = __tmp_136;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_122399 = l;
}
 __return_122402 = -1073741802;
}
compRetStatus = __return_122402;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_122501;
goto label_122501;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_122628;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_122628;
}
else 
{
returnVal2 = 259;
label_122628:; 
goto label_122640;
}
}
}
else 
{
returnVal2 = 259;
label_122640:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_122769;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_122745;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_122745:; 
goto label_122769;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_122769:; 
 __return_122773 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_122674;
}
tmp = __return_122773;
goto label_122778;
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
int __tmp_137 = deviceExtension__TargetDeviceObject;
int __tmp_138 = Irp;
int DeviceObject = __tmp_137;
int Irp = __tmp_138;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123038;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123038;
}
else 
{
returnVal2 = 259;
label_123038:; 
goto label_123050;
}
}
}
else 
{
returnVal2 = 259;
label_123050:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123179;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123155;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123155:; 
goto label_123179;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123179:; 
 __return_123189 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_123088:; 
 __return_123190 = returnVal2;
}
tmp = __return_123189;
goto label_122778;
tmp = __return_123190;
goto label_122778;
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_139 = DeviceObject;
int __tmp_140 = Irp;
int __tmp_141 = lcontext;
int DeviceObject = __tmp_139;
int Irp = __tmp_140;
int Context = __tmp_141;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_122842;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_142 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_142;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_122858;
}
else 
{
label_122858:; 
}
label_122861:; 
 __return_122878 = myStatus;
}
compRetStatus = __return_122878;
goto label_122882;
}
else 
{
 __return_122879 = myStatus;
}
compRetStatus = __return_122879;
goto label_122882;
}
}
else 
{
label_122842:; 
if (myStatus >= 0)
{
{
int __tmp_143 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_143;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_122873;
}
else 
{
label_122873:; 
}
goto label_122861;
}
}
else 
{
 __return_122880 = myStatus;
}
compRetStatus = __return_122880;
label_122882:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_122915;
label_122915:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123044;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123044;
}
else 
{
returnVal2 = 259;
label_123044:; 
goto label_123056;
}
}
}
else 
{
returnVal2 = 259;
label_123056:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123185;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123161;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123161:; 
goto label_123185;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123185:; 
 __return_123186 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123088;
}
tmp = __return_123186;
goto label_122778;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123040;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123040;
}
else 
{
returnVal2 = 259;
label_123040:; 
goto label_123052;
}
}
}
else 
{
returnVal2 = 259;
label_123052:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123181;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123157;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123157:; 
goto label_123181;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123181:; 
 __return_123188 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123088;
}
tmp = __return_123188;
goto label_122778;
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
int __tmp_144 = DeviceObject;
int __tmp_145 = Irp;
int __tmp_146 = lcontext;
int DeviceObject = __tmp_144;
int Irp = __tmp_145;
int Event = __tmp_146;
{
int __tmp_147 = Event;
int __tmp_148 = 0;
int __tmp_149 = 0;
int Event = __tmp_147;
int Increment = __tmp_148;
int Wait = __tmp_149;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_122813 = l;
}
 __return_122816 = -1073741802;
}
compRetStatus = __return_122816;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_122915;
goto label_122915;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123042;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123042;
}
else 
{
returnVal2 = 259;
label_123042:; 
goto label_123054;
}
}
}
else 
{
returnVal2 = 259;
label_123054:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123183;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123159;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123159:; 
goto label_123183;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123183:; 
 __return_123187 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123088;
}
tmp = __return_123187;
goto label_122778;
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
int __tmp_155 = DeviceObject;
int __tmp_156 = Irp;
int DeviceObject = __tmp_155;
int Irp = __tmp_156;
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
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_157 = deviceExtension__TargetDeviceObject;
int __tmp_158 = Irp;
int DeviceObject = __tmp_157;
int Irp = __tmp_158;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123490;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123490;
}
else 
{
returnVal2 = 259;
label_123490:; 
goto label_123502;
}
}
}
else 
{
returnVal2 = 259;
label_123502:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123631;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123607;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123607:; 
goto label_123631;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123631:; 
 __return_123641 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_123540:; 
 __return_123642 = returnVal2;
}
tmp = __return_123641;
goto label_123644;
tmp = __return_123642;
label_123644:; 
 __return_124067 = tmp;
}
tmp___7 = __return_124067;
goto label_123203;
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_159 = DeviceObject;
int __tmp_160 = Irp;
int __tmp_161 = lcontext;
int DeviceObject = __tmp_159;
int Irp = __tmp_160;
int Context = __tmp_161;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_123294;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_162 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_162;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_123310;
}
else 
{
label_123310:; 
}
label_123313:; 
 __return_123330 = myStatus;
}
compRetStatus = __return_123330;
goto label_123334;
}
else 
{
 __return_123331 = myStatus;
}
compRetStatus = __return_123331;
goto label_123334;
}
}
else 
{
label_123294:; 
if (myStatus >= 0)
{
{
int __tmp_163 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_163;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_123325;
}
else 
{
label_123325:; 
}
goto label_123313;
}
}
else 
{
 __return_123332 = myStatus;
}
compRetStatus = __return_123332;
label_123334:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_123367;
label_123367:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123496;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123496;
}
else 
{
returnVal2 = 259;
label_123496:; 
goto label_123508;
}
}
}
else 
{
returnVal2 = 259;
label_123508:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123637;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123613;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123613:; 
goto label_123637;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123637:; 
 __return_123638 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123540;
}
tmp = __return_123638;
goto label_123644;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123492;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123492;
}
else 
{
returnVal2 = 259;
label_123492:; 
goto label_123504;
}
}
}
else 
{
returnVal2 = 259;
label_123504:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123633;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123609;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123609:; 
goto label_123633;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123633:; 
 __return_123640 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123540;
}
tmp = __return_123640;
goto label_123644;
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
int __tmp_164 = DeviceObject;
int __tmp_165 = Irp;
int __tmp_166 = lcontext;
int DeviceObject = __tmp_164;
int Irp = __tmp_165;
int Event = __tmp_166;
{
int __tmp_167 = Event;
int __tmp_168 = 0;
int __tmp_169 = 0;
int Event = __tmp_167;
int Increment = __tmp_168;
int Wait = __tmp_169;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_123265 = l;
}
 __return_123268 = -1073741802;
}
compRetStatus = __return_123268;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_123367;
goto label_123367;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123494;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123494;
}
else 
{
returnVal2 = 259;
label_123494:; 
goto label_123506;
}
}
}
else 
{
returnVal2 = 259;
label_123506:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123635;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123611;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123611:; 
goto label_123635;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123635:; 
 __return_123639 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123540;
}
tmp = __return_123639;
goto label_123644;
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
int __tmp_170 = deviceExtension__TargetDeviceObject;
int __tmp_171 = Irp;
int DeviceObject = __tmp_170;
int Irp = __tmp_171;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123904;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123904;
}
else 
{
returnVal2 = 259;
label_123904:; 
goto label_123916;
}
}
}
else 
{
returnVal2 = 259;
label_123916:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_124045;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_124021;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_124021:; 
goto label_124045;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_124045:; 
 __return_124055 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_123954:; 
 __return_124056 = returnVal2;
}
tmp = __return_124055;
goto label_123644;
tmp = __return_124056;
goto label_123644;
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_172 = DeviceObject;
int __tmp_173 = Irp;
int __tmp_174 = lcontext;
int DeviceObject = __tmp_172;
int Irp = __tmp_173;
int Context = __tmp_174;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_123708;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_175 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_175;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_123724;
}
else 
{
label_123724:; 
}
label_123727:; 
 __return_123744 = myStatus;
}
compRetStatus = __return_123744;
goto label_123748;
}
else 
{
 __return_123745 = myStatus;
}
compRetStatus = __return_123745;
goto label_123748;
}
}
else 
{
label_123708:; 
if (myStatus >= 0)
{
{
int __tmp_176 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_176;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_123739;
}
else 
{
label_123739:; 
}
goto label_123727;
}
}
else 
{
 __return_123746 = myStatus;
}
compRetStatus = __return_123746;
label_123748:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_123781;
label_123781:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123910;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123910;
}
else 
{
returnVal2 = 259;
label_123910:; 
goto label_123922;
}
}
}
else 
{
returnVal2 = 259;
label_123922:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_124051;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_124027;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_124027:; 
goto label_124051;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_124051:; 
 __return_124052 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123954;
}
tmp = __return_124052;
goto label_123644;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123906;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123906;
}
else 
{
returnVal2 = 259;
label_123906:; 
goto label_123918;
}
}
}
else 
{
returnVal2 = 259;
label_123918:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_124047;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_124023;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_124023:; 
goto label_124047;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_124047:; 
 __return_124054 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123954;
}
tmp = __return_124054;
goto label_123644;
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
int __tmp_177 = DeviceObject;
int __tmp_178 = Irp;
int __tmp_179 = lcontext;
int DeviceObject = __tmp_177;
int Irp = __tmp_178;
int Event = __tmp_179;
{
int __tmp_180 = Event;
int __tmp_181 = 0;
int __tmp_182 = 0;
int Event = __tmp_180;
int Increment = __tmp_181;
int Wait = __tmp_182;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_123679 = l;
}
 __return_123682 = -1073741802;
}
compRetStatus = __return_123682;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_123781;
goto label_123781;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123908;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123908;
}
else 
{
returnVal2 = 259;
label_123908:; 
goto label_123920;
}
}
}
else 
{
returnVal2 = 259;
label_123920:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_124049;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_124025;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_124025:; 
goto label_124049;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_124049:; 
 __return_124053 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123954;
}
tmp = __return_124053;
goto label_123644;
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
if (deviceExtension__Active == 3)
{
{
int __tmp_183 = DeviceObject;
int __tmp_184 = Irp;
int DeviceObject = __tmp_183;
int Irp = __tmp_184;
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
label_121654:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121707;
}
else 
{
label_121707:; 
myStatus = status7;
{
int __tmp_185 = Irp;
int __tmp_186 = 0;
int Irp = __tmp_185;
int PriorityBoost = __tmp_186;
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
goto label_121775;
label_121775:; 
 __return_121955 = status7;
}
status4 = __return_121955;
goto label_121958;
}
}
else 
{
{
int __tmp_187 = DeviceObject;
int DeviceObject = __tmp_187;
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
 __return_121454 = 0;
goto label_121455;
}
else 
{
if (currentBuffer == 0)
{
 __return_121452 = 0;
goto label_121455;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_121455 = 0;
label_121455:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (__cil_tmp10 == 259L)
{
{
int __tmp_200 = event;
int __tmp_201 = Suspended;
int __tmp_202 = KernelMode;
int __tmp_203 = 0;
int __tmp_204 = 0;
int Object = __tmp_200;
int WaitReason = __tmp_201;
int WaitMode = __tmp_202;
int Alertable = __tmp_203;
int Timeout = __tmp_204;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_121398;
}
else 
{
goto label_121375;
}
}
else 
{
label_121375:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_121398;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_121417 = 0;
goto label_121418;
}
else 
{
 __return_121418 = -1073741823;
label_121418:; 
}
goto label_121422;
}
else 
{
label_121398:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_121419 = 0;
goto label_121420;
}
else 
{
 __return_121420 = -1073741823;
label_121420:; 
}
label_121422:; 
status5 = ioStatus__Status;
if (status5 < 0)
{
 __return_121450 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_121446;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_121446:; 
 __return_121447 = returnValue;
}
tmp = __return_121447;
goto label_121457;
}
tmp = __return_121450;
goto label_121457;
}
}
}
}
}
else 
{
if (status5 < 0)
{
 __return_121449 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_121444;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_121444:; 
 __return_121448 = returnValue;
}
tmp = __return_121448;
goto label_121457;
}
tmp = __return_121449;
goto label_121457;
}
}
tmp = __return_121455;
label_121457:; 
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
goto label_121612;
}
else 
{
label_121612:; 
myStatus = status7;
{
int __tmp_188 = Irp;
int __tmp_189 = 0;
int Irp = __tmp_188;
int PriorityBoost = __tmp_189;
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
goto label_121633;
label_121633:; 
 __return_121636 = status7;
}
status4 = __return_121636;
goto label_121958;
}
}
else 
{
srb__TimeOutValue = 10;
srb__CdbLength = 10;
{
int __tmp_190 = deviceExtension;
int __tmp_191 = srb;
int __tmp_192 = Toc;
int __tmp_193 = sizeof__CDROM_TOC;
int Extension = __tmp_190;
int Srb = __tmp_191;
int Buffer = __tmp_192;
int BufferLength = __tmp_193;
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
if (irp == 0)
{
 __return_121525 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121523 = status1;
}
status7 = __return_121525;
label_121527:; 
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
goto label_121580;
}
else 
{
label_121580:; 
myStatus = status7;
{
int __tmp_194 = Irp;
int __tmp_195 = 0;
int Irp = __tmp_194;
int PriorityBoost = __tmp_195;
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
goto label_121601;
label_121601:; 
 __return_121604 = status7;
}
status4 = __return_121604;
goto label_121958;
}
}
else 
{
goto label_121547;
}
}
else 
{
status7 = 0;
label_121547:; 
goto label_121549;
}
}
else 
{
status7 = 0;
label_121549:; 
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength > sizeof__CDROM_TOC)
{
bytesTransfered = sizeof__CDROM_TOC;
goto label_121556;
}
else 
{
bytesTransfered = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
label_121556:; 
__cil_tmp98 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp98 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength - TrackData__0;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_121566;
}
else 
{
tracksToReturn = tracksOnCd;
label_121566:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_121573;
}
else 
{
label_121573:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121723;
}
else 
{
label_121723:; 
myStatus = status7;
{
int __tmp_196 = Irp;
int __tmp_197 = 0;
int Irp = __tmp_196;
int PriorityBoost = __tmp_197;
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
goto label_121951;
label_121951:; 
 __return_121954 = status7;
}
status4 = __return_121954;
goto label_121958;
}
}
}
}
}
status7 = __return_121523;
goto label_121527;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_121524 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121522 = status1;
}
status7 = __return_121524;
goto label_121527;
status7 = __return_121522;
goto label_121527;
}
}
}
}
else 
{
status7 = -2147483631;
Irp__IoStatus__Information = 0;
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121721;
}
else 
{
label_121721:; 
myStatus = status7;
{
int __tmp_198 = Irp;
int __tmp_199 = 0;
int Irp = __tmp_198;
int PriorityBoost = __tmp_199;
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
goto label_121775;
goto label_121775;
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
Irp__IoStatus__Information = 0;
label_121167:; 
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_205 = deviceExtension;
int __tmp_206 = srb;
int __tmp_207 = 0;
int __tmp_208 = 0;
int Extension = __tmp_205;
int Srb = __tmp_206;
int Buffer = __tmp_207;
int BufferLength = __tmp_208;
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
if (irp == 0)
{
 __return_121220 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121218 = status1;
}
status7 = __return_121220;
label_121222:; 
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
goto label_121308;
}
else 
{
label_121308:; 
myStatus = status7;
{
int __tmp_209 = Irp;
int __tmp_210 = 0;
int Irp = __tmp_209;
int PriorityBoost = __tmp_210;
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
goto label_121329;
label_121329:; 
 __return_121332 = status7;
}
status4 = __return_121332;
goto label_121958;
}
}
else 
{
if (currentIrpStack__Parameters__DeviceIoControl__InputBufferLength < sizeof__CDROM_PLAY_AUDIO_MSF)
{
status7 = -1073741820;
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121719;
}
else 
{
label_121719:; 
myStatus = status7;
{
int __tmp_211 = Irp;
int __tmp_212 = 0;
int Irp = __tmp_211;
int PriorityBoost = __tmp_212;
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
goto label_121753;
goto label_121753;
}
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_213 = deviceExtension;
int __tmp_214 = srb;
int __tmp_215 = 0;
int __tmp_216 = 0;
int Extension = __tmp_213;
int Srb = __tmp_214;
int Buffer = __tmp_215;
int BufferLength = __tmp_216;
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
if (irp == 0)
{
 __return_121291 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121289 = status1;
}
status7 = __return_121291;
label_121293:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121705;
}
else 
{
label_121705:; 
myStatus = status7;
{
int __tmp_217 = Irp;
int __tmp_218 = 0;
int Irp = __tmp_217;
int PriorityBoost = __tmp_218;
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
goto label_121753;
label_121753:; 
 __return_121956 = status7;
}
status4 = __return_121956;
label_121958:; 
 __return_125444 = status4;
}
status10 = __return_125444;
goto label_125449;
status7 = __return_121289;
goto label_121293;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_121290 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121288 = status1;
}
status7 = __return_121290;
goto label_121293;
status7 = __return_121288;
goto label_121293;
}
}
}
}
status7 = __return_121218;
goto label_121222;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_121219 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121217 = status1;
}
status7 = __return_121219;
goto label_121222;
status7 = __return_121217;
goto label_121222;
}
}
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
Irp__IoStatus__Information = 0;
goto label_121167;
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
goto label_121654;
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_219 = deviceExtension;
int __tmp_220 = srb;
int __tmp_221 = 0;
int __tmp_222 = 0;
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
if (irp == 0)
{
 __return_121139 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121137 = status1;
}
status7 = __return_121139;
label_121141:; 
if (status7 < 0)
{
__cil_tmp105 = (unsigned long)status7;
if (__cil_tmp105 == -1073741808)
{
status7 = -1073741803;
goto label_121157;
}
else 
{
goto label_121157;
}
}
else 
{
label_121157:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121717;
}
else 
{
label_121717:; 
myStatus = status7;
{
int __tmp_223 = Irp;
int __tmp_224 = 0;
int Irp = __tmp_223;
int PriorityBoost = __tmp_224;
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
goto label_121775;
goto label_121775;
}
}
}
status7 = __return_121137;
goto label_121141;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_121138 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121136 = status1;
}
status7 = __return_121138;
goto label_121141;
status7 = __return_121136;
goto label_121141;
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
goto label_121057;
}
else 
{
label_121057:; 
myStatus = status7;
{
int __tmp_225 = Irp;
int __tmp_226 = 0;
int Irp = __tmp_225;
int PriorityBoost = __tmp_226;
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
goto label_121078;
label_121078:; 
 __return_121081 = status7;
}
status4 = __return_121081;
goto label_121958;
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
goto label_121026;
}
else 
{
label_121026:; 
myStatus = status7;
{
int __tmp_227 = Irp;
int __tmp_228 = 0;
int Irp = __tmp_227;
int PriorityBoost = __tmp_228;
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
goto label_121047;
label_121047:; 
 __return_121050 = status7;
}
status4 = __return_121050;
goto label_121958;
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_229 = deviceExtension;
int __tmp_230 = srb;
int __tmp_231 = SubQPtr;
int __tmp_232 = sizeof__SUB_Q_CHANNEL_DATA;
int Extension = __tmp_229;
int Srb = __tmp_230;
int Buffer = __tmp_231;
int BufferLength = __tmp_232;
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
if (irp == 0)
{
 __return_120885 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120883 = status1;
}
status7 = __return_120885;
label_120887:; 
if (status7 < 0)
{
__cil_tmp109 = (unsigned long)status7;
if (__cil_tmp109 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_120995;
}
else 
{
label_120995:; 
myStatus = status7;
{
int __tmp_233 = Irp;
int __tmp_234 = 0;
int Irp = __tmp_233;
int PriorityBoost = __tmp_234;
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
goto label_121016;
label_121016:; 
 __return_121019 = status7;
}
status4 = __return_121019;
goto label_121958;
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_235 = deviceExtension;
int __tmp_236 = srb;
int __tmp_237 = 0;
int __tmp_238 = 0;
int Extension = __tmp_235;
int Srb = __tmp_236;
int Buffer = __tmp_237;
int BufferLength = __tmp_238;
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
if (irp == 0)
{
 __return_120948 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120946 = status1;
}
status7 = __return_120948;
label_120950:; 
if (status7 < 0)
{
__cil_tmp111 = (unsigned long)status7;
if (__cil_tmp111 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_120965;
}
else 
{
label_120965:; 
myStatus = status7;
{
int __tmp_239 = Irp;
int __tmp_240 = 0;
int Irp = __tmp_239;
int PriorityBoost = __tmp_240;
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
goto label_120986;
label_120986:; 
 __return_120989 = status7;
}
status4 = __return_120989;
goto label_121958;
}
}
else 
{
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121715;
}
else 
{
label_121715:; 
myStatus = status7;
{
int __tmp_241 = Irp;
int __tmp_242 = 0;
int Irp = __tmp_241;
int PriorityBoost = __tmp_242;
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
goto label_121775;
goto label_121775;
}
}
}
status7 = __return_120946;
goto label_120950;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_120947 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120945 = status1;
}
status7 = __return_120947;
goto label_120950;
status7 = __return_120945;
goto label_120950;
}
}
}
status7 = __return_120883;
goto label_120887;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_120884 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120882 = status1;
}
status7 = __return_120884;
goto label_120887;
status7 = __return_120882;
goto label_120887;
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
goto label_120801;
}
else 
{
label_120801:; 
myStatus = status7;
{
int __tmp_243 = Irp;
int __tmp_244 = 0;
int Irp = __tmp_243;
int PriorityBoost = __tmp_244;
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
goto label_120822;
label_120822:; 
 __return_120825 = status7;
}
status4 = __return_120825;
goto label_121958;
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_245 = deviceExtension;
int __tmp_246 = srb;
int __tmp_247 = 0;
int __tmp_248 = 0;
int Extension = __tmp_245;
int Srb = __tmp_246;
int Buffer = __tmp_247;
int BufferLength = __tmp_248;
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
if (irp == 0)
{
 __return_120779 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120777 = status1;
}
status7 = __return_120779;
label_120781:; 
if (status7 >= 0)
{
deviceExtension__PlayActive = 1;
deviceExtension__Paused = 0;
goto label_120793;
}
else 
{
label_120793:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121713;
}
else 
{
label_121713:; 
myStatus = status7;
{
int __tmp_249 = Irp;
int __tmp_250 = 0;
int Irp = __tmp_249;
int PriorityBoost = __tmp_250;
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
goto label_121775;
goto label_121775;
}
}
}
status7 = __return_120777;
goto label_120781;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_120778 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120776 = status1;
}
status7 = __return_120778;
goto label_120781;
status7 = __return_120776;
goto label_120781;
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
goto label_121654;
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
goto label_120694;
}
else 
{
label_120694:; 
myStatus = status7;
{
int __tmp_251 = Irp;
int __tmp_252 = 0;
int Irp = __tmp_251;
int PriorityBoost = __tmp_252;
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
goto label_120715;
label_120715:; 
 __return_120718 = status7;
}
status4 = __return_120718;
goto label_121958;
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
goto label_120662;
}
else 
{
label_120662:; 
myStatus = status7;
{
int __tmp_253 = Irp;
int __tmp_254 = 0;
int Irp = __tmp_253;
int PriorityBoost = __tmp_254;
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
goto label_120683;
label_120683:; 
 __return_120686 = status7;
}
status4 = __return_120686;
goto label_121958;
}
}
else 
{
srb__CdbLength = 10;
srb__TimeOutValue = 10;
{
int __tmp_255 = deviceExtension;
int __tmp_256 = srb;
int __tmp_257 = SubQPtr___0;
int __tmp_258 = sizeof__SUB_Q_CHANNEL_DATA;
int Extension = __tmp_255;
int Srb = __tmp_256;
int Buffer = __tmp_257;
int BufferLength = __tmp_258;
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
if (irp == 0)
{
 __return_120633 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120631 = status1;
}
status7 = __return_120633;
label_120635:; 
if (status7 >= 0)
{
if (deviceExtension__Paused == 1)
{
deviceExtension__PlayActive = 0;
goto label_120650;
}
else 
{
label_120650:; 
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_120653;
}
}
else 
{
Irp__IoStatus__Information = 0;
label_120653:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121711;
}
else 
{
label_121711:; 
myStatus = status7;
{
int __tmp_259 = Irp;
int __tmp_260 = 0;
int Irp = __tmp_259;
int PriorityBoost = __tmp_260;
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
goto label_121775;
goto label_121775;
}
}
}
status7 = __return_120631;
goto label_120635;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_120632 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120630 = status1;
}
status7 = __return_120632;
goto label_120635;
status7 = __return_120630;
goto label_120635;
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
int __tmp_261 = deviceExtension;
int __tmp_262 = srb;
int __tmp_263 = 0;
int __tmp_264 = 0;
int Extension = __tmp_261;
int Srb = __tmp_262;
int Buffer = __tmp_263;
int BufferLength = __tmp_264;
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
if (irp == 0)
{
 __return_120563 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120561 = status1;
}
status7 = __return_120563;
label_120565:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121709;
}
else 
{
label_121709:; 
myStatus = status7;
{
int __tmp_265 = Irp;
int __tmp_266 = 0;
int Irp = __tmp_265;
int PriorityBoost = __tmp_266;
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
goto label_121775;
goto label_121775;
}
}
status7 = __return_120561;
goto label_120565;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_120562 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120560 = status1;
}
status7 = __return_120562;
goto label_120565;
status7 = __return_120560;
goto label_120565;
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
label_120502:; 
Irp__IoStatus__Information = 0;
label_120505:; 
status7 = -1073741808;
goto label_121654;
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
goto label_120502;
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
Irp__IoStatus__Information = 0;
goto label_120505;
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
int __tmp_267 = DeviceObject;
int DeviceObject = __tmp_267;
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
 __return_119107 = 0;
goto label_119108;
}
else 
{
if (currentBuffer == 0)
{
 __return_119105 = 0;
goto label_119108;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_119108 = 0;
label_119108:; 
}
else 
{
__cil_tmp10 = (long)status5;
if (__cil_tmp10 == 259L)
{
{
int __tmp_285 = event;
int __tmp_286 = Suspended;
int __tmp_287 = KernelMode;
int __tmp_288 = 0;
int __tmp_289 = 0;
int Object = __tmp_285;
int WaitReason = __tmp_286;
int WaitMode = __tmp_287;
int Alertable = __tmp_288;
int Timeout = __tmp_289;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_119051;
}
else 
{
goto label_119028;
}
}
else 
{
label_119028:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_119051;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_119070 = 0;
goto label_119071;
}
else 
{
 __return_119071 = -1073741823;
label_119071:; 
}
goto label_119075;
}
else 
{
label_119051:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_119072 = 0;
goto label_119073;
}
else 
{
 __return_119073 = -1073741823;
label_119073:; 
}
label_119075:; 
status5 = ioStatus__Status;
if (status5 < 0)
{
 __return_119103 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_119099;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_119099:; 
 __return_119100 = returnValue;
}
tmp___1 = __return_119100;
goto label_119112;
}
tmp___1 = __return_119103;
label_119112:; 
if (tmp___1 == 1)
{
deviceExtension__PlayActive = 1;
status7 = 0;
Irp__IoStatus__Information = 0;
__cil_tmp115 = (unsigned long)status7;
if (__cil_tmp115 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_120447;
}
else 
{
label_120447:; 
myStatus = status7;
{
int __tmp_290 = Irp;
int __tmp_291 = 0;
int Irp = __tmp_290;
int PriorityBoost = __tmp_291;
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
goto label_120469;
goto label_120469;
}
}
}
else 
{
deviceExtension__PlayActive = 0;
{
int __tmp_292 = DeviceObject;
int __tmp_293 = Irp;
int DeviceObject = __tmp_292;
int Irp = __tmp_293;
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
int __CPAchecker_TMP_0 = Irp__CurrentLocation;
Irp__CurrentLocation = Irp__CurrentLocation + 1;
__CPAchecker_TMP_0;
int __CPAchecker_TMP_1 = Irp__Tail__Overlay__CurrentStackLocation;
Irp__Tail__Overlay__CurrentStackLocation = Irp__Tail__Overlay__CurrentStackLocation + 1;
__CPAchecker_TMP_1;
{
int __tmp_294 = deviceExtension__TargetDeviceObject;
int __tmp_295 = Irp;
int DeviceObject = __tmp_294;
int Irp = __tmp_295;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_119411;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119411;
}
else 
{
returnVal2 = 259;
label_119411:; 
goto label_119423;
}
}
}
else 
{
returnVal2 = 259;
label_119423:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119552;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119528;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119528:; 
goto label_119552;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119552:; 
 __return_119562 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_119461:; 
 __return_119563 = returnVal2;
}
tmp = __return_119562;
goto label_119565;
tmp = __return_119563;
label_119565:; 
 __return_119988 = tmp;
}
tmp___0 = __return_119988;
label_119990:; 
 __return_120429 = tmp___0;
}
status4 = __return_120429;
goto label_121958;
}
}
else 
{
if (routine == 0)
{
{
int __tmp_296 = DeviceObject;
int __tmp_297 = Irp;
int __tmp_298 = lcontext;
int DeviceObject = __tmp_296;
int Irp = __tmp_297;
int Context = __tmp_298;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_119215;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_299 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_299;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_119231;
}
else 
{
label_119231:; 
}
label_119234:; 
 __return_119251 = myStatus;
}
compRetStatus = __return_119251;
goto label_119255;
}
else 
{
 __return_119252 = myStatus;
}
compRetStatus = __return_119252;
goto label_119255;
}
}
else 
{
label_119215:; 
if (myStatus >= 0)
{
{
int __tmp_300 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_300;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_119246;
}
else 
{
label_119246:; 
}
goto label_119234;
}
}
else 
{
 __return_119253 = myStatus;
}
compRetStatus = __return_119253;
label_119255:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_119288;
label_119288:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_119417;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119417;
}
else 
{
returnVal2 = 259;
label_119417:; 
goto label_119429;
}
}
}
else 
{
returnVal2 = 259;
label_119429:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119558;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119534;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119534:; 
goto label_119558;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119558:; 
 __return_119559 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119461;
}
tmp = __return_119559;
goto label_119565;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_119413;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119413;
}
else 
{
returnVal2 = 259;
label_119413:; 
goto label_119425;
}
}
}
else 
{
returnVal2 = 259;
label_119425:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119554;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119530;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119530:; 
goto label_119554;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119554:; 
 __return_119561 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119461;
}
tmp = __return_119561;
goto label_119565;
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
int __tmp_301 = DeviceObject;
int __tmp_302 = Irp;
int __tmp_303 = lcontext;
int DeviceObject = __tmp_301;
int Irp = __tmp_302;
int Event = __tmp_303;
{
int __tmp_304 = Event;
int __tmp_305 = 0;
int __tmp_306 = 0;
int Event = __tmp_304;
int Increment = __tmp_305;
int Wait = __tmp_306;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_119186 = l;
}
 __return_119189 = -1073741802;
}
compRetStatus = __return_119189;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_119288;
goto label_119288;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_119415;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119415;
}
else 
{
returnVal2 = 259;
label_119415:; 
goto label_119427;
}
}
}
else 
{
returnVal2 = 259;
label_119427:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119556;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119532;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119532:; 
goto label_119556;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119556:; 
 __return_119560 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119461;
}
tmp = __return_119560;
goto label_119565;
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
int __tmp_307 = deviceExtension__TargetDeviceObject;
int __tmp_308 = Irp;
int DeviceObject = __tmp_307;
int Irp = __tmp_308;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_119825;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119825;
}
else 
{
returnVal2 = 259;
label_119825:; 
goto label_119837;
}
}
}
else 
{
returnVal2 = 259;
label_119837:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119966;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119942;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119942:; 
goto label_119966;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119966:; 
 __return_119976 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_119875:; 
 __return_119977 = returnVal2;
}
tmp = __return_119976;
goto label_119565;
tmp = __return_119977;
goto label_119565;
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_309 = DeviceObject;
int __tmp_310 = Irp;
int __tmp_311 = lcontext;
int DeviceObject = __tmp_309;
int Irp = __tmp_310;
int Context = __tmp_311;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_119629;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_312 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_312;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_119645;
}
else 
{
label_119645:; 
}
label_119648:; 
 __return_119665 = myStatus;
}
compRetStatus = __return_119665;
goto label_119669;
}
else 
{
 __return_119666 = myStatus;
}
compRetStatus = __return_119666;
goto label_119669;
}
}
else 
{
label_119629:; 
if (myStatus >= 0)
{
{
int __tmp_313 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_313;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_119660;
}
else 
{
label_119660:; 
}
goto label_119648;
}
}
else 
{
 __return_119667 = myStatus;
}
compRetStatus = __return_119667;
label_119669:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_119702;
label_119702:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_119831;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119831;
}
else 
{
returnVal2 = 259;
label_119831:; 
goto label_119843;
}
}
}
else 
{
returnVal2 = 259;
label_119843:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119972;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119948;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119948:; 
goto label_119972;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119972:; 
 __return_119973 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119875;
}
tmp = __return_119973;
goto label_119565;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_119827;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119827;
}
else 
{
returnVal2 = 259;
label_119827:; 
goto label_119839;
}
}
}
else 
{
returnVal2 = 259;
label_119839:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119968;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119944;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119944:; 
goto label_119968;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119968:; 
 __return_119975 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119875;
}
tmp = __return_119975;
goto label_119565;
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
int __tmp_314 = DeviceObject;
int __tmp_315 = Irp;
int __tmp_316 = lcontext;
int DeviceObject = __tmp_314;
int Irp = __tmp_315;
int Event = __tmp_316;
{
int __tmp_317 = Event;
int __tmp_318 = 0;
int __tmp_319 = 0;
int Event = __tmp_317;
int Increment = __tmp_318;
int Wait = __tmp_319;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_119600 = l;
}
 __return_119603 = -1073741802;
}
compRetStatus = __return_119603;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_119702;
goto label_119702;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_119829;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119829;
}
else 
{
returnVal2 = 259;
label_119829:; 
goto label_119841;
}
}
}
else 
{
returnVal2 = 259;
label_119841:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119970;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119946;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119946:; 
goto label_119970;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119970:; 
 __return_119974 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119875;
}
tmp = __return_119974;
goto label_119565;
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
if (status5 < 0)
{
 __return_119102 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_119097;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_119097:; 
 __return_119101 = returnValue;
}
tmp___1 = __return_119101;
goto label_119110;
}
tmp___1 = __return_119102;
goto label_119110;
}
}
tmp___1 = __return_119108;
label_119110:; 
if (tmp___1 == 1)
{
deviceExtension__PlayActive = 1;
status7 = 0;
Irp__IoStatus__Information = 0;
__cil_tmp115 = (unsigned long)status7;
if (__cil_tmp115 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_120445;
}
else 
{
label_120445:; 
myStatus = status7;
{
int __tmp_268 = Irp;
int __tmp_269 = 0;
int Irp = __tmp_268;
int PriorityBoost = __tmp_269;
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
goto label_120469;
label_120469:; 
 __return_120494 = status7;
}
status4 = __return_120494;
goto label_121958;
}
}
else 
{
deviceExtension__PlayActive = 0;
{
int __tmp_270 = DeviceObject;
int __tmp_271 = Irp;
int DeviceObject = __tmp_270;
int Irp = __tmp_271;
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
int __tmp_272 = deviceExtension__TargetDeviceObject;
int __tmp_273 = Irp;
int DeviceObject = __tmp_272;
int Irp = __tmp_273;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_120263;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_120263;
}
else 
{
returnVal2 = 259;
label_120263:; 
goto label_120275;
}
}
}
else 
{
returnVal2 = 259;
label_120275:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_120404;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_120380;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_120380:; 
goto label_120404;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_120404:; 
 __return_120414 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_120313:; 
 __return_120415 = returnVal2;
}
tmp = __return_120414;
goto label_120417;
tmp = __return_120415;
label_120417:; 
 __return_120426 = tmp;
}
tmp___0 = __return_120426;
goto label_119990;
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_274 = DeviceObject;
int __tmp_275 = Irp;
int __tmp_276 = lcontext;
int DeviceObject = __tmp_274;
int Irp = __tmp_275;
int Context = __tmp_276;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_120067;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_277 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_277;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_120083;
}
else 
{
label_120083:; 
}
label_120086:; 
 __return_120103 = myStatus;
}
compRetStatus = __return_120103;
goto label_120107;
}
else 
{
 __return_120104 = myStatus;
}
compRetStatus = __return_120104;
goto label_120107;
}
}
else 
{
label_120067:; 
if (myStatus >= 0)
{
{
int __tmp_278 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_278;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_120098;
}
else 
{
label_120098:; 
}
goto label_120086;
}
}
else 
{
 __return_120105 = myStatus;
}
compRetStatus = __return_120105;
label_120107:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_120140;
label_120140:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_120269;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_120269;
}
else 
{
returnVal2 = 259;
label_120269:; 
goto label_120281;
}
}
}
else 
{
returnVal2 = 259;
label_120281:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_120410;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_120386;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_120386:; 
goto label_120410;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_120410:; 
 __return_120411 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_120313;
}
tmp = __return_120411;
goto label_120417;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_120265;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_120265;
}
else 
{
returnVal2 = 259;
label_120265:; 
goto label_120277;
}
}
}
else 
{
returnVal2 = 259;
label_120277:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_120406;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_120382;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_120382:; 
goto label_120406;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_120406:; 
 __return_120413 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_120313;
}
tmp = __return_120413;
goto label_120417;
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
int __tmp_279 = DeviceObject;
int __tmp_280 = Irp;
int __tmp_281 = lcontext;
int DeviceObject = __tmp_279;
int Irp = __tmp_280;
int Event = __tmp_281;
{
int __tmp_282 = Event;
int __tmp_283 = 0;
int __tmp_284 = 0;
int Event = __tmp_282;
int Increment = __tmp_283;
int Wait = __tmp_284;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_120038 = l;
}
 __return_120041 = -1073741802;
}
compRetStatus = __return_120041;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_120140;
goto label_120140;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_120267;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_120267;
}
else 
{
returnVal2 = 259;
label_120267:; 
goto label_120279;
}
}
}
else 
{
returnVal2 = 259;
label_120279:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_120408;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_120384;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_120384:; 
goto label_120408;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_120408:; 
 __return_120412 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_120313;
}
tmp = __return_120412;
goto label_120417;
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
int __tmp_320 = DeviceObject;
int __tmp_321 = Irp;
int DeviceObject = __tmp_320;
int Irp = __tmp_321;
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
int __tmp_322 = deviceExtension__TargetDeviceObject;
int __tmp_323 = Irp;
int DeviceObject = __tmp_322;
int Irp = __tmp_323;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_118821;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118821;
}
else 
{
returnVal2 = 259;
label_118821:; 
goto label_118833;
}
}
}
else 
{
returnVal2 = 259;
label_118833:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118962;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118938;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118938:; 
goto label_118962;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118962:; 
 __return_118972 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_118871:; 
 __return_118973 = returnVal2;
}
tmp = __return_118972;
goto label_118975;
tmp = __return_118973;
label_118975:; 
 __return_118984 = tmp;
}
tmp___2 = __return_118984;
 __return_118987 = tmp___2;
}
status4 = __return_118987;
goto label_121958;
}
}
else 
{
if (routine == 0)
{
{
int __tmp_324 = DeviceObject;
int __tmp_325 = Irp;
int __tmp_326 = lcontext;
int DeviceObject = __tmp_324;
int Irp = __tmp_325;
int Context = __tmp_326;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_118625;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_327 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_327;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_118641;
}
else 
{
label_118641:; 
}
label_118644:; 
 __return_118661 = myStatus;
}
compRetStatus = __return_118661;
goto label_118665;
}
else 
{
 __return_118662 = myStatus;
}
compRetStatus = __return_118662;
goto label_118665;
}
}
else 
{
label_118625:; 
if (myStatus >= 0)
{
{
int __tmp_328 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_328;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_118656;
}
else 
{
label_118656:; 
}
goto label_118644;
}
}
else 
{
 __return_118663 = myStatus;
}
compRetStatus = __return_118663;
label_118665:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_118698;
label_118698:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_118827;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118827;
}
else 
{
returnVal2 = 259;
label_118827:; 
goto label_118839;
}
}
}
else 
{
returnVal2 = 259;
label_118839:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118968;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118944;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118944:; 
goto label_118968;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118968:; 
 __return_118969 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118871;
}
tmp = __return_118969;
goto label_118975;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_118823;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118823;
}
else 
{
returnVal2 = 259;
label_118823:; 
goto label_118835;
}
}
}
else 
{
returnVal2 = 259;
label_118835:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118964;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118940;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118940:; 
goto label_118964;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118964:; 
 __return_118971 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118871;
}
tmp = __return_118971;
goto label_118975;
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
int __tmp_329 = DeviceObject;
int __tmp_330 = Irp;
int __tmp_331 = lcontext;
int DeviceObject = __tmp_329;
int Irp = __tmp_330;
int Event = __tmp_331;
{
int __tmp_332 = Event;
int __tmp_333 = 0;
int __tmp_334 = 0;
int Event = __tmp_332;
int Increment = __tmp_333;
int Wait = __tmp_334;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_118596 = l;
}
 __return_118599 = -1073741802;
}
compRetStatus = __return_118599;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_118698;
goto label_118698;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_118825;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118825;
}
else 
{
returnVal2 = 259;
label_118825:; 
goto label_118837;
}
}
}
else 
{
returnVal2 = 259;
label_118837:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118966;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118942;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118942:; 
goto label_118966;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118966:; 
 __return_118970 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118871;
}
tmp = __return_118970;
goto label_118975;
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
if (deviceExtension__Active == 1)
{
{
int __tmp_335 = DeviceObject;
int __tmp_336 = Irp;
int DeviceObject = __tmp_335;
int Irp = __tmp_336;
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
int __tmp_337 = deviceExtension;
int __tmp_338 = srb;
int __tmp_339 = 0;
int __tmp_340 = 0;
int Extension = __tmp_337;
int Srb = __tmp_338;
int Buffer = __tmp_339;
int BufferLength = __tmp_340;
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
if (irp == 0)
{
 __return_118258 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_118256 = status1;
}
status8 = __return_118258;
label_118260:; 
if (status8 < 0)
{
Irp__IoStatus__Status = status8;
myStatus = status8;
{
int __tmp_341 = Irp;
int __tmp_342 = 0;
int Irp = __tmp_341;
int PriorityBoost = __tmp_342;
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
goto label_118316;
label_118316:; 
 __return_118319 = status8;
}
status4 = __return_118319;
label_118321:; 
 __return_125445 = status4;
}
else 
{
Irp__IoStatus__Status = status8;
myStatus = status8;
{
int __tmp_343 = Irp;
int __tmp_344 = 0;
int Irp = __tmp_343;
int PriorityBoost = __tmp_344;
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
goto label_118291;
label_118291:; 
 __return_118294 = status8;
}
status4 = __return_118294;
goto label_118321;
}
status10 = __return_125445;
goto label_125449;
status8 = __return_118256;
goto label_118260;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_118257 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_118255 = status1;
}
status8 = __return_118257;
goto label_118260;
status8 = __return_118255;
goto label_118260;
}
}
}
else 
{
{
int __tmp_345 = DeviceObject;
int __tmp_346 = Irp;
int DeviceObject = __tmp_345;
int Irp = __tmp_346;
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
int __tmp_347 = deviceExtension__TargetDeviceObject;
int __tmp_348 = Irp;
int DeviceObject = __tmp_347;
int Irp = __tmp_348;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_118037;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118037;
}
else 
{
returnVal2 = 259;
label_118037:; 
goto label_118049;
}
}
}
else 
{
returnVal2 = 259;
label_118049:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118178;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118154;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118154:; 
goto label_118178;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118178:; 
 __return_118188 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_118087:; 
 __return_118189 = returnVal2;
}
tmp = __return_118188;
goto label_118191;
tmp = __return_118189;
label_118191:; 
 __return_118200 = tmp;
}
tmp = __return_118200;
 __return_118203 = tmp;
}
status4 = __return_118203;
goto label_118321;
}
}
else 
{
if (routine == 0)
{
{
int __tmp_349 = DeviceObject;
int __tmp_350 = Irp;
int __tmp_351 = lcontext;
int DeviceObject = __tmp_349;
int Irp = __tmp_350;
int Context = __tmp_351;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_117841;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_352 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_352;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_117857;
}
else 
{
label_117857:; 
}
label_117860:; 
 __return_117877 = myStatus;
}
compRetStatus = __return_117877;
goto label_117881;
}
else 
{
 __return_117878 = myStatus;
}
compRetStatus = __return_117878;
goto label_117881;
}
}
else 
{
label_117841:; 
if (myStatus >= 0)
{
{
int __tmp_353 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_353;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_117872;
}
else 
{
label_117872:; 
}
goto label_117860;
}
}
else 
{
 __return_117879 = myStatus;
}
compRetStatus = __return_117879;
label_117881:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_117914;
label_117914:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_118043;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118043;
}
else 
{
returnVal2 = 259;
label_118043:; 
goto label_118055;
}
}
}
else 
{
returnVal2 = 259;
label_118055:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118184;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118160;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118160:; 
goto label_118184;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118184:; 
 __return_118185 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118087;
}
tmp = __return_118185;
goto label_118191;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_118039;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118039;
}
else 
{
returnVal2 = 259;
label_118039:; 
goto label_118051;
}
}
}
else 
{
returnVal2 = 259;
label_118051:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118180;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118156;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118156:; 
goto label_118180;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118180:; 
 __return_118187 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118087;
}
tmp = __return_118187;
goto label_118191;
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
int __tmp_354 = DeviceObject;
int __tmp_355 = Irp;
int __tmp_356 = lcontext;
int DeviceObject = __tmp_354;
int Irp = __tmp_355;
int Event = __tmp_356;
{
int __tmp_357 = Event;
int __tmp_358 = 0;
int __tmp_359 = 0;
int Event = __tmp_357;
int Increment = __tmp_358;
int Wait = __tmp_359;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_117812 = l;
}
 __return_117815 = -1073741802;
}
compRetStatus = __return_117815;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_117914;
goto label_117914;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_118041;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118041;
}
else 
{
returnVal2 = 259;
label_118041:; 
goto label_118053;
}
}
}
else 
{
returnVal2 = 259;
label_118053:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118182;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118158;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118158:; 
goto label_118182;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118182:; 
 __return_118186 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118087;
}
tmp = __return_118186;
goto label_118191;
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
if (deviceExtension__Active == 7)
{
{
int __tmp_360 = DeviceObject;
int __tmp_361 = Irp;
int DeviceObject = __tmp_360;
int Irp = __tmp_361;
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
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_117565;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117565;
}
else 
{
returnVal2 = 259;
label_117565:; 
goto label_117577;
}
}
}
else 
{
returnVal2 = 259;
label_117577:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117706;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117682;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117682:; 
goto label_117706;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117706:; 
 __return_117716 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_117615:; 
 __return_117717 = returnVal2;
}
tmp = __return_117716;
goto label_117305;
tmp = __return_117717;
goto label_117305;
}
}
}
}
else 
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
goto label_117369;
}
else 
{
{
__VERIFIER_error();
}
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
goto label_117385;
}
else 
{
label_117385:; 
}
label_117388:; 
 __return_117405 = myStatus;
}
compRetStatus = __return_117405;
goto label_117409;
}
else 
{
 __return_117406 = myStatus;
}
compRetStatus = __return_117406;
goto label_117409;
}
}
else 
{
label_117369:; 
if (myStatus >= 0)
{
{
int __tmp_368 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_368;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_117400;
}
else 
{
label_117400:; 
}
goto label_117388;
}
}
else 
{
 __return_117407 = myStatus;
}
compRetStatus = __return_117407;
label_117409:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_117442;
label_117442:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_117571;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117571;
}
else 
{
returnVal2 = 259;
label_117571:; 
goto label_117583;
}
}
}
else 
{
returnVal2 = 259;
label_117583:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117712;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117688;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117688:; 
goto label_117712;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117712:; 
 __return_117713 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117615;
}
tmp = __return_117713;
goto label_117305;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_117567;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117567;
}
else 
{
returnVal2 = 259;
label_117567:; 
goto label_117579;
}
}
}
else 
{
returnVal2 = 259;
label_117579:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117708;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117684;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117684:; 
goto label_117708;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117708:; 
 __return_117715 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117615;
}
tmp = __return_117715;
goto label_117305;
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
int __tmp_369 = DeviceObject;
int __tmp_370 = Irp;
int __tmp_371 = lcontext;
int DeviceObject = __tmp_369;
int Irp = __tmp_370;
int Event = __tmp_371;
{
int __tmp_372 = Event;
int __tmp_373 = 0;
int __tmp_374 = 0;
int Event = __tmp_372;
int Increment = __tmp_373;
int Wait = __tmp_374;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_117340 = l;
}
 __return_117343 = -1073741802;
}
compRetStatus = __return_117343;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_117442;
goto label_117442;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_117569;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117569;
}
else 
{
returnVal2 = 259;
label_117569:; 
goto label_117581;
}
}
}
else 
{
returnVal2 = 259;
label_117581:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117710;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117686;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117686:; 
goto label_117710;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117710:; 
 __return_117714 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117615;
}
tmp = __return_117714;
goto label_117305;
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
routine = 0;
irpSp__Control = 224;
{
int __tmp_375 = deviceExtension__TargetDeviceObject;
int __tmp_376 = Irp;
int DeviceObject = __tmp_375;
int Irp = __tmp_376;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_117151;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117151;
}
else 
{
returnVal2 = 259;
label_117151:; 
goto label_117163;
}
}
}
else 
{
returnVal2 = 259;
label_117163:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117292;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117268;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117268:; 
goto label_117292;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117292:; 
 __return_117302 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_117201:; 
 __return_117303 = returnVal2;
}
tmp = __return_117302;
goto label_117305;
tmp = __return_117303;
label_117305:; 
 __return_117728 = tmp;
}
status4 = __return_117728;
label_117730:; 
 __return_125446 = status4;
}
status10 = __return_125446;
goto label_125449;
}
}
else 
{
if (routine == 0)
{
{
int __tmp_377 = DeviceObject;
int __tmp_378 = Irp;
int __tmp_379 = lcontext;
int DeviceObject = __tmp_377;
int Irp = __tmp_378;
int Context = __tmp_379;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_116955;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_380 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_380;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_116971;
}
else 
{
label_116971:; 
}
label_116974:; 
 __return_116991 = myStatus;
}
compRetStatus = __return_116991;
goto label_116995;
}
else 
{
 __return_116992 = myStatus;
}
compRetStatus = __return_116992;
goto label_116995;
}
}
else 
{
label_116955:; 
if (myStatus >= 0)
{
{
int __tmp_381 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_381;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_116986;
}
else 
{
label_116986:; 
}
goto label_116974;
}
}
else 
{
 __return_116993 = myStatus;
}
compRetStatus = __return_116993;
label_116995:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_117028;
label_117028:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_117157;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117157;
}
else 
{
returnVal2 = 259;
label_117157:; 
goto label_117169;
}
}
}
else 
{
returnVal2 = 259;
label_117169:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117298;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117274;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117274:; 
goto label_117298;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117298:; 
 __return_117299 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117201;
}
tmp = __return_117299;
goto label_117305;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_117153;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117153;
}
else 
{
returnVal2 = 259;
label_117153:; 
goto label_117165;
}
}
}
else 
{
returnVal2 = 259;
label_117165:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117294;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117270;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117270:; 
goto label_117294;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117294:; 
 __return_117301 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117201;
}
tmp = __return_117301;
goto label_117305;
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
int __tmp_382 = DeviceObject;
int __tmp_383 = Irp;
int __tmp_384 = lcontext;
int DeviceObject = __tmp_382;
int Irp = __tmp_383;
int Event = __tmp_384;
{
int __tmp_385 = Event;
int __tmp_386 = 0;
int __tmp_387 = 0;
int Event = __tmp_385;
int Increment = __tmp_386;
int Wait = __tmp_387;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_116926 = l;
}
 __return_116929 = -1073741802;
}
compRetStatus = __return_116929;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_117028;
goto label_117028;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_117155;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117155;
}
else 
{
returnVal2 = 259;
label_117155:; 
goto label_117167;
}
}
}
else 
{
returnVal2 = 259;
label_117167:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117296;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117272;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117272:; 
goto label_117296;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117296:; 
 __return_117300 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117201;
}
tmp = __return_117300;
goto label_117305;
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
int __tmp_388 = DeviceObject;
int __tmp_389 = Irp;
int DeviceObject = __tmp_388;
int Irp = __tmp_389;
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
int __tmp_390 = deviceExtension__TargetDeviceObject;
int __tmp_391 = Irp;
int DeviceObject = __tmp_390;
int Irp = __tmp_391;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_116714;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116714;
}
else 
{
returnVal2 = 259;
label_116714:; 
goto label_116726;
}
}
}
else 
{
returnVal2 = 259;
label_116726:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116855;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116831;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116831:; 
goto label_116855;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116855:; 
 __return_116865 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_116764:; 
 __return_116866 = returnVal2;
}
tmp = __return_116865;
goto label_116868;
tmp = __return_116866;
label_116868:; 
 __return_116877 = tmp;
}
tmp___0 = __return_116877;
 __return_116880 = tmp___0;
}
status4 = __return_116880;
goto label_117730;
}
}
else 
{
if (routine == 0)
{
{
int __tmp_392 = DeviceObject;
int __tmp_393 = Irp;
int __tmp_394 = lcontext;
int DeviceObject = __tmp_392;
int Irp = __tmp_393;
int Context = __tmp_394;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_116518;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_395 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_395;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_116534;
}
else 
{
label_116534:; 
}
label_116537:; 
 __return_116554 = myStatus;
}
compRetStatus = __return_116554;
goto label_116558;
}
else 
{
 __return_116555 = myStatus;
}
compRetStatus = __return_116555;
goto label_116558;
}
}
else 
{
label_116518:; 
if (myStatus >= 0)
{
{
int __tmp_396 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_396;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_116549;
}
else 
{
label_116549:; 
}
goto label_116537;
}
}
else 
{
 __return_116556 = myStatus;
}
compRetStatus = __return_116556;
label_116558:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_116591;
label_116591:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_116720;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116720;
}
else 
{
returnVal2 = 259;
label_116720:; 
goto label_116732;
}
}
}
else 
{
returnVal2 = 259;
label_116732:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116861;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116837;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116837:; 
goto label_116861;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116861:; 
 __return_116862 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116764;
}
tmp = __return_116862;
goto label_116868;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_116716;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116716;
}
else 
{
returnVal2 = 259;
label_116716:; 
goto label_116728;
}
}
}
else 
{
returnVal2 = 259;
label_116728:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116857;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116833;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116833:; 
goto label_116857;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116857:; 
 __return_116864 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116764;
}
tmp = __return_116864;
goto label_116868;
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
int __tmp_397 = DeviceObject;
int __tmp_398 = Irp;
int __tmp_399 = lcontext;
int DeviceObject = __tmp_397;
int Irp = __tmp_398;
int Event = __tmp_399;
{
int __tmp_400 = Event;
int __tmp_401 = 0;
int __tmp_402 = 0;
int Event = __tmp_400;
int Increment = __tmp_401;
int Wait = __tmp_402;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_116489 = l;
}
 __return_116492 = -1073741802;
}
compRetStatus = __return_116492;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_116591;
goto label_116591;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_116718;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116718;
}
else 
{
returnVal2 = 259;
label_116718:; 
goto label_116730;
}
}
}
else 
{
returnVal2 = 259;
label_116730:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116859;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116835;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116835:; 
goto label_116859;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116859:; 
 __return_116863 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116764;
}
tmp = __return_116863;
goto label_116868;
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
deviceExtension__Active = 0;
{
int __tmp_403 = DeviceObject;
int __tmp_404 = Irp;
int DeviceObject = __tmp_403;
int Irp = __tmp_404;
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
int __tmp_405 = deviceExtension__TargetDeviceObject;
int __tmp_406 = Irp;
int DeviceObject = __tmp_405;
int Irp = __tmp_406;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_116251;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116251;
}
else 
{
returnVal2 = 259;
label_116251:; 
goto label_116263;
}
}
}
else 
{
returnVal2 = 259;
label_116263:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116392;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116368;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116368:; 
goto label_116392;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116392:; 
 __return_116402 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_116301:; 
 __return_116403 = returnVal2;
}
tmp = __return_116402;
goto label_116405;
tmp = __return_116403;
label_116405:; 
 __return_116414 = tmp;
}
status4 = __return_116414;
 __return_125447 = status4;
}
status10 = __return_125447;
label_125449:; 
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_125873;
}
else 
{
goto label_125519;
}
}
else 
{
label_125519:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_125873;
}
else 
{
goto label_125562;
}
}
else 
{
label_125562:; 
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
goto label_125806;
}
else 
{
goto label_125652;
}
}
else 
{
goto label_125652;
}
}
else 
{
label_125652:; 
if (pended != 1)
{
if (s == DC)
{
if (status10 == 259)
{
{
__VERIFIER_error();
}
goto label_125762;
}
else 
{
goto label_125873;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_125710;
}
else 
{
goto label_125873;
}
}
}
else 
{
goto label_125873;
}
}
}
else 
{
goto label_125873;
}
}
else 
{
label_125873:; 
 __return_125891 = status10;
goto label_111587;
}
}
}
}
else 
{
{
int __tmp_407 = d;
int DriverObject = __tmp_407;
}
goto label_125476;
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_408 = DeviceObject;
int __tmp_409 = Irp;
int __tmp_410 = lcontext;
int DeviceObject = __tmp_408;
int Irp = __tmp_409;
int Context = __tmp_410;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_116055;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_411 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_411;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_116071;
}
else 
{
label_116071:; 
}
label_116074:; 
 __return_116091 = myStatus;
}
compRetStatus = __return_116091;
goto label_116095;
}
else 
{
 __return_116092 = myStatus;
}
compRetStatus = __return_116092;
goto label_116095;
}
}
else 
{
label_116055:; 
if (myStatus >= 0)
{
{
int __tmp_412 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_412;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_116086;
}
else 
{
label_116086:; 
}
goto label_116074;
}
}
else 
{
 __return_116093 = myStatus;
}
compRetStatus = __return_116093;
label_116095:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_116128;
label_116128:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_116257;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116257;
}
else 
{
returnVal2 = 259;
label_116257:; 
goto label_116269;
}
}
}
else 
{
returnVal2 = 259;
label_116269:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116398;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116374;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116374:; 
goto label_116398;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116398:; 
 __return_116399 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116301;
}
tmp = __return_116399;
goto label_116405;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_116253;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116253;
}
else 
{
returnVal2 = 259;
label_116253:; 
goto label_116265;
}
}
}
else 
{
returnVal2 = 259;
label_116265:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116394;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116370;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116370:; 
goto label_116394;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116394:; 
 __return_116401 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116301;
}
tmp = __return_116401;
goto label_116405;
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
int __tmp_413 = DeviceObject;
int __tmp_414 = Irp;
int __tmp_415 = lcontext;
int DeviceObject = __tmp_413;
int Irp = __tmp_414;
int Event = __tmp_415;
{
int __tmp_416 = Event;
int __tmp_417 = 0;
int __tmp_418 = 0;
int Event = __tmp_416;
int Increment = __tmp_417;
int Wait = __tmp_418;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_116026 = l;
}
 __return_116029 = -1073741802;
}
compRetStatus = __return_116029;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_116128;
goto label_116128;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_116255;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116255;
}
else 
{
returnVal2 = 259;
label_116255:; 
goto label_116267;
}
}
}
else 
{
returnVal2 = 259;
label_116267:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116396;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116372;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116372:; 
goto label_116396;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116396:; 
 __return_116400 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116301;
}
tmp = __return_116400;
goto label_116405;
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
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 3)
{
{
int __tmp_419 = devobj;
int __tmp_420 = pirp;
int DeviceObject = __tmp_419;
int Irp = __tmp_420;
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
int __tmp_421 = DeviceObject;
int __tmp_422 = Irp;
int DeviceObject = __tmp_421;
int Irp = __tmp_422;
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
int __tmp_423 = DeviceObject;
int __tmp_424 = Irp;
int DeviceObject = __tmp_423;
int Irp = __tmp_424;
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
irpSp__Control = 224;
{
int __tmp_425 = deviceExtension__TargetDeviceObject;
int __tmp_426 = Irp;
int DeviceObject = __tmp_425;
int Irp = __tmp_426;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_115533;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115533;
}
else 
{
returnVal2 = 259;
label_115533:; 
goto label_115545;
}
}
}
else 
{
returnVal2 = 259;
label_115545:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115674;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115650;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115650:; 
goto label_115674;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115674:; 
 __return_115684 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_115583:; 
 __return_115685 = returnVal2;
}
status9 = __return_115684;
goto label_114859;
status9 = __return_115685;
goto label_114859;
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_427 = DeviceObject;
int __tmp_428 = Irp;
int __tmp_429 = lcontext;
int DeviceObject = __tmp_427;
int Irp = __tmp_428;
int Context = __tmp_429;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_115337;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_430 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_430;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_115353;
}
else 
{
label_115353:; 
}
label_115356:; 
 __return_115373 = myStatus;
}
compRetStatus = __return_115373;
goto label_115377;
}
else 
{
 __return_115374 = myStatus;
}
compRetStatus = __return_115374;
goto label_115377;
}
}
else 
{
label_115337:; 
if (myStatus >= 0)
{
{
int __tmp_431 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_431;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_115368;
}
else 
{
label_115368:; 
}
goto label_115356;
}
}
else 
{
 __return_115375 = myStatus;
}
compRetStatus = __return_115375;
label_115377:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_115410;
label_115410:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_115539;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115539;
}
else 
{
returnVal2 = 259;
label_115539:; 
goto label_115551;
}
}
}
else 
{
returnVal2 = 259;
label_115551:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115680;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115656;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115656:; 
goto label_115680;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115680:; 
 __return_115681 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115583;
}
status9 = __return_115681;
goto label_114859;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_115535;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115535;
}
else 
{
returnVal2 = 259;
label_115535:; 
goto label_115547;
}
}
}
else 
{
returnVal2 = 259;
label_115547:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115676;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115652;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115652:; 
goto label_115676;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115676:; 
 __return_115683 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115583;
}
status9 = __return_115683;
goto label_114859;
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
int __tmp_432 = DeviceObject;
int __tmp_433 = Irp;
int __tmp_434 = lcontext;
int DeviceObject = __tmp_432;
int Irp = __tmp_433;
int Event = __tmp_434;
{
int __tmp_435 = Event;
int __tmp_436 = 0;
int __tmp_437 = 0;
int Event = __tmp_435;
int Increment = __tmp_436;
int Wait = __tmp_437;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_115308 = l;
}
 __return_115311 = -1073741802;
}
compRetStatus = __return_115311;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_115410;
goto label_115410;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_115537;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115537;
}
else 
{
returnVal2 = 259;
label_115537:; 
goto label_115549;
}
}
}
else 
{
returnVal2 = 259;
label_115549:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115678;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115654;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115654:; 
goto label_115678;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115678:; 
 __return_115682 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115583;
}
status9 = __return_115682;
goto label_114859;
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
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
irpSp__Control = 224;
{
int __tmp_438 = deviceExtension__TargetDeviceObject;
int __tmp_439 = Irp;
int DeviceObject = __tmp_438;
int Irp = __tmp_439;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_115119;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115119;
}
else 
{
returnVal2 = 259;
label_115119:; 
goto label_115131;
}
}
}
else 
{
returnVal2 = 259;
label_115131:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115260;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115236;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115236:; 
goto label_115260;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115260:; 
 __return_115270 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_115169:; 
 __return_115271 = returnVal2;
}
status9 = __return_115270;
goto label_114859;
status9 = __return_115271;
goto label_114859;
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_440 = DeviceObject;
int __tmp_441 = Irp;
int __tmp_442 = lcontext;
int DeviceObject = __tmp_440;
int Irp = __tmp_441;
int Context = __tmp_442;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_114923;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_443 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_443;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_114939;
}
else 
{
label_114939:; 
}
label_114942:; 
 __return_114959 = myStatus;
}
compRetStatus = __return_114959;
goto label_114963;
}
else 
{
 __return_114960 = myStatus;
}
compRetStatus = __return_114960;
goto label_114963;
}
}
else 
{
label_114923:; 
if (myStatus >= 0)
{
{
int __tmp_444 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_444;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_114954;
}
else 
{
label_114954:; 
}
goto label_114942;
}
}
else 
{
 __return_114961 = myStatus;
}
compRetStatus = __return_114961;
label_114963:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_114996;
label_114996:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_115125;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115125;
}
else 
{
returnVal2 = 259;
label_115125:; 
goto label_115137;
}
}
}
else 
{
returnVal2 = 259;
label_115137:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115266;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115242;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115242:; 
goto label_115266;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115266:; 
 __return_115267 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115169;
}
status9 = __return_115267;
goto label_114859;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_115121;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115121;
}
else 
{
returnVal2 = 259;
label_115121:; 
goto label_115133;
}
}
}
else 
{
returnVal2 = 259;
label_115133:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115262;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115238;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115238:; 
goto label_115262;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115262:; 
 __return_115269 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115169;
}
status9 = __return_115269;
goto label_114859;
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
int __tmp_445 = DeviceObject;
int __tmp_446 = Irp;
int __tmp_447 = lcontext;
int DeviceObject = __tmp_445;
int Irp = __tmp_446;
int Event = __tmp_447;
{
int __tmp_448 = Event;
int __tmp_449 = 0;
int __tmp_450 = 0;
int Event = __tmp_448;
int Increment = __tmp_449;
int Wait = __tmp_450;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_114894 = l;
}
 __return_114897 = -1073741802;
}
compRetStatus = __return_114897;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_114996;
goto label_114996;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_115123;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115123;
}
else 
{
returnVal2 = 259;
label_115123:; 
goto label_115135;
}
}
}
else 
{
returnVal2 = 259;
label_115135:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115264;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115240;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115240:; 
goto label_115264;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115264:; 
 __return_115268 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115169;
}
status9 = __return_115268;
goto label_114859;
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
int __tmp_451 = deviceExtension__TargetDeviceObject;
int __tmp_452 = Irp;
int DeviceObject = __tmp_451;
int Irp = __tmp_452;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_114705;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114705;
}
else 
{
returnVal2 = 259;
label_114705:; 
goto label_114717;
}
}
}
else 
{
returnVal2 = 259;
label_114717:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114846;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114822;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114822:; 
goto label_114846;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114846:; 
 __return_114856 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_114755:; 
 __return_114857 = returnVal2;
}
status9 = __return_114856;
goto label_114859;
status9 = __return_114857;
label_114859:; 
status9 = 259;
if (status9 == 0)
{
 __return_115761 = status9;
}
else 
{
{
int __tmp_462 = event;
int __tmp_463 = Executive;
int __tmp_464 = KernelMode;
int __tmp_465 = 0;
int __tmp_466 = 0;
int Object = __tmp_462;
int WaitReason = __tmp_463;
int WaitMode = __tmp_464;
int Alertable = __tmp_465;
int Timeout = __tmp_466;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_115731;
}
else 
{
goto label_115708;
}
}
else 
{
label_115708:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_115731;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_115750 = 0;
goto label_115751;
}
else 
{
 __return_115751 = -1073741823;
label_115751:; 
}
goto label_115755;
}
else 
{
label_115731:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_115752 = 0;
goto label_115753;
}
else 
{
 __return_115753 = -1073741823;
label_115753:; 
}
label_115755:; 
status9 = myStatus;
 __return_115760 = status9;
}
status2 = __return_115760;
goto label_115763;
}
}
}
}
status2 = __return_115761;
label_115763:; 
if (status2 < 0)
{
 __return_115911 = status2;
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
 __return_115910 = 0;
}
else 
{
status2 = -1073741823;
label_115777:; 
if (status2 < 0)
{
tmp = attempt;
int __CPAchecker_TMP_0 = attempt;
attempt = attempt + 1;
__CPAchecker_TMP_0;
if (tmp >= 4)
{
goto label_115852;
}
else 
{
{
int __tmp_456 = deviceExtension;
int __tmp_457 = srb;
int __tmp_458 = inquiryDataPtr;
int __tmp_459 = 36;
int Extension = __tmp_456;
int Srb = __tmp_457;
int Buffer = __tmp_458;
int BufferLength = __tmp_459;
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
if (irp == 0)
{
 __return_115840 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_115838 = status1;
}
status2 = __return_115840;
label_115842:; 
goto label_115777;
status2 = __return_115838;
goto label_115842;
}
else 
{
__cil_tmp10 = 4116;
__cil_tmp11 = 49152;
__cil_tmp12 = 262144;
__cil_tmp13 = 311296;
ioctl = 315412;
if (irp == 0)
{
 __return_115839 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_115837 = status1;
}
status2 = __return_115839;
goto label_115842;
status2 = __return_115837;
goto label_115842;
}
}
}
}
else 
{
label_115852:; 
if (status2 < 0)
{
deviceExtension__Active = 0;
 __return_115908 = 0;
}
else 
{
deviceExtension__Active = 0;
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_115905 = 0;
}
else 
{
if (status2 < 0)
{
goto label_115870;
}
else 
{
label_115870:; 
{
int __tmp_460 = deviceParameterHandle;
int Handle = __tmp_460;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_115883 = 0;
goto label_115884;
}
else 
{
 __return_115884 = -1073741823;
label_115884:; 
}
 __return_115904 = 0;
}
status3 = __return_115904;
goto label_115913;
}
}
status3 = __return_115905;
goto label_115913;
}
status3 = __return_115908;
goto label_115913;
}
}
status3 = __return_115910;
goto label_115913;
}
else 
{
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_115906 = 0;
}
else 
{
if (status2 < 0)
{
goto label_115868;
}
else 
{
label_115868:; 
{
int __tmp_461 = deviceParameterHandle;
int Handle = __tmp_461;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_115899 = 0;
goto label_115900;
}
else 
{
 __return_115900 = -1073741823;
label_115900:; 
}
 __return_115903 = 0;
}
status3 = __return_115903;
goto label_115913;
}
}
status3 = __return_115906;
goto label_115913;
}
}
status3 = __return_115911;
label_115913:; 
Irp__IoStatus__Status = status3;
myStatus = status3;
{
int __tmp_453 = Irp;
int __tmp_454 = 0;
int Irp = __tmp_453;
int PriorityBoost = __tmp_454;
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
goto label_115947;
label_115947:; 
 __return_115950 = status3;
}
status10 = __return_115950;
label_115952:; 
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_125875;
}
else 
{
goto label_125521;
}
}
else 
{
label_125521:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_125875;
}
else 
{
goto label_125560;
}
}
else 
{
label_125560:; 
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
goto label_125806;
}
else 
{
goto label_125650;
}
}
else 
{
goto label_125650;
}
}
else 
{
label_125650:; 
if (pended != 1)
{
if (s == DC)
{
if (status10 == 259)
{
{
__VERIFIER_error();
}
goto label_125762;
}
else 
{
goto label_125875;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_125710;
}
else 
{
goto label_125875;
}
}
}
else 
{
goto label_125875;
}
}
}
else 
{
goto label_125875;
}
}
else 
{
label_125875:; 
 __return_125889 = status10;
goto label_111587;
}
}
}
}
else 
{
{
int __tmp_455 = d;
int DriverObject = __tmp_455;
}
goto label_125476;
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_467 = DeviceObject;
int __tmp_468 = Irp;
int __tmp_469 = lcontext;
int DeviceObject = __tmp_467;
int Irp = __tmp_468;
int Context = __tmp_469;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_114509;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_470 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_470;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_114525;
}
else 
{
label_114525:; 
}
label_114528:; 
 __return_114545 = myStatus;
}
compRetStatus = __return_114545;
goto label_114549;
}
else 
{
 __return_114546 = myStatus;
}
compRetStatus = __return_114546;
goto label_114549;
}
}
else 
{
label_114509:; 
if (myStatus >= 0)
{
{
int __tmp_471 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_471;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_114540;
}
else 
{
label_114540:; 
}
goto label_114528;
}
}
else 
{
 __return_114547 = myStatus;
}
compRetStatus = __return_114547;
label_114549:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_114582;
label_114582:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_114711;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114711;
}
else 
{
returnVal2 = 259;
label_114711:; 
goto label_114723;
}
}
}
else 
{
returnVal2 = 259;
label_114723:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114852;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114828;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114828:; 
goto label_114852;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114852:; 
 __return_114853 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114755;
}
status9 = __return_114853;
goto label_114859;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_114707;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114707;
}
else 
{
returnVal2 = 259;
label_114707:; 
goto label_114719;
}
}
}
else 
{
returnVal2 = 259;
label_114719:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114848;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114824;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114824:; 
goto label_114848;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114848:; 
 __return_114855 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114755;
}
status9 = __return_114855;
goto label_114859;
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
int __tmp_472 = DeviceObject;
int __tmp_473 = Irp;
int __tmp_474 = lcontext;
int DeviceObject = __tmp_472;
int Irp = __tmp_473;
int Event = __tmp_474;
{
int __tmp_475 = Event;
int __tmp_476 = 0;
int __tmp_477 = 0;
int Event = __tmp_475;
int Increment = __tmp_476;
int Wait = __tmp_477;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_114480 = l;
}
 __return_114483 = -1073741802;
}
compRetStatus = __return_114483;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_114582;
goto label_114582;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_114709;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114709;
}
else 
{
returnVal2 = 259;
label_114709:; 
goto label_114721;
}
}
}
else 
{
returnVal2 = 259;
label_114721:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114850;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114826;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114826:; 
goto label_114850;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114850:; 
 __return_114854 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114755;
}
status9 = __return_114854;
goto label_114859;
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
int __tmp_478 = DeviceObject;
int __tmp_479 = Irp;
int DeviceObject = __tmp_478;
int Irp = __tmp_479;
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
int __tmp_480 = deviceExtension__TargetDeviceObject;
int __tmp_481 = Irp;
int DeviceObject = __tmp_480;
int Irp = __tmp_481;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_114229;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114229;
}
else 
{
returnVal2 = 259;
label_114229:; 
goto label_114241;
}
}
}
else 
{
returnVal2 = 259;
label_114241:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114370;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114346;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114346:; 
goto label_114370;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114370:; 
 __return_114380 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_114279:; 
 __return_114381 = returnVal2;
}
tmp = __return_114380;
goto label_114383;
tmp = __return_114381;
label_114383:; 
 __return_114392 = tmp;
}
tmp = __return_114392;
 __return_114395 = tmp;
}
status10 = __return_114395;
goto label_115952;
}
}
else 
{
if (routine == 0)
{
{
int __tmp_482 = DeviceObject;
int __tmp_483 = Irp;
int __tmp_484 = lcontext;
int DeviceObject = __tmp_482;
int Irp = __tmp_483;
int Context = __tmp_484;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_114033;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_485 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_485;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_114049;
}
else 
{
label_114049:; 
}
label_114052:; 
 __return_114069 = myStatus;
}
compRetStatus = __return_114069;
goto label_114073;
}
else 
{
 __return_114070 = myStatus;
}
compRetStatus = __return_114070;
goto label_114073;
}
}
else 
{
label_114033:; 
if (myStatus >= 0)
{
{
int __tmp_486 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_486;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_114064;
}
else 
{
label_114064:; 
}
goto label_114052;
}
}
else 
{
 __return_114071 = myStatus;
}
compRetStatus = __return_114071;
label_114073:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_114106;
label_114106:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_114235;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114235;
}
else 
{
returnVal2 = 259;
label_114235:; 
goto label_114247;
}
}
}
else 
{
returnVal2 = 259;
label_114247:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114376;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114352;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114352:; 
goto label_114376;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114376:; 
 __return_114377 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114279;
}
tmp = __return_114377;
goto label_114383;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_114231;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114231;
}
else 
{
returnVal2 = 259;
label_114231:; 
goto label_114243;
}
}
}
else 
{
returnVal2 = 259;
label_114243:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114372;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114348;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114348:; 
goto label_114372;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114372:; 
 __return_114379 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114279;
}
tmp = __return_114379;
goto label_114383;
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
int __tmp_487 = DeviceObject;
int __tmp_488 = Irp;
int __tmp_489 = lcontext;
int DeviceObject = __tmp_487;
int Irp = __tmp_488;
int Event = __tmp_489;
{
int __tmp_490 = Event;
int __tmp_491 = 0;
int __tmp_492 = 0;
int Event = __tmp_490;
int Increment = __tmp_491;
int Wait = __tmp_492;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_114004 = l;
}
 __return_114007 = -1073741802;
}
compRetStatus = __return_114007;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_114106;
goto label_114106;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_114233;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114233;
}
else 
{
returnVal2 = 259;
label_114233:; 
goto label_114245;
}
}
}
else 
{
returnVal2 = 259;
label_114245:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114374;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114350;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114350:; 
goto label_114374;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114374:; 
 __return_114378 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114279;
}
tmp = __return_114378;
goto label_114383;
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
int __tmp_493 = deviceExtension__PagingPathCountEvent;
int __tmp_494 = Executive;
int __tmp_495 = KernelMode;
int __tmp_496 = 0;
int __tmp_497 = 0;
int Object = __tmp_493;
int WaitReason = __tmp_494;
int WaitMode = __tmp_495;
int Alertable = __tmp_496;
int Timeout = __tmp_497;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_112523;
}
else 
{
goto label_112500;
}
}
else 
{
label_112500:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_112523;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_112542 = 0;
goto label_112543;
}
else 
{
 __return_112543 = -1073741823;
label_112543:; 
}
status3 = __return_112543;
goto label_112547;
}
else 
{
label_112523:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_112544 = 0;
goto label_112545;
}
else 
{
 __return_112545 = -1073741823;
label_112545:; 
}
status3 = __return_112545;
label_112547:; 
setPagable = 0;
if (irpSp__Parameters__UsageNotification__InPath == 0)
{
goto label_112558;
}
else 
{
if (deviceExtension__PagingPathCount != 1)
{
label_112558:; 
if (status3 == status3)
{
setPagable = 1;
goto label_112565;
}
else 
{
goto label_112565;
}
}
else 
{
label_112565:; 
{
int __tmp_498 = DeviceObject;
int __tmp_499 = Irp;
int DeviceObject = __tmp_498;
int Irp = __tmp_499;
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
irpSp__Control = 224;
{
int __tmp_500 = deviceExtension__TargetDeviceObject;
int __tmp_501 = Irp;
int DeviceObject = __tmp_500;
int Irp = __tmp_501;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_113682;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113682;
}
else 
{
returnVal2 = 259;
label_113682:; 
goto label_113694;
}
}
}
else 
{
returnVal2 = 259;
label_113694:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113823;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113799;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113799:; 
goto label_113823;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113823:; 
 __return_113833 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_113732:; 
 __return_113834 = returnVal2;
}
status9 = __return_113833;
goto label_113008;
status9 = __return_113834;
goto label_113008;
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_502 = DeviceObject;
int __tmp_503 = Irp;
int __tmp_504 = lcontext;
int DeviceObject = __tmp_502;
int Irp = __tmp_503;
int Context = __tmp_504;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_113486;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_505 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_505;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_113502;
}
else 
{
label_113502:; 
}
label_113505:; 
 __return_113522 = myStatus;
}
compRetStatus = __return_113522;
goto label_113526;
}
else 
{
 __return_113523 = myStatus;
}
compRetStatus = __return_113523;
goto label_113526;
}
}
else 
{
label_113486:; 
if (myStatus >= 0)
{
{
int __tmp_506 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_506;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_113517;
}
else 
{
label_113517:; 
}
goto label_113505;
}
}
else 
{
 __return_113524 = myStatus;
}
compRetStatus = __return_113524;
label_113526:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_113559;
label_113559:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_113688;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113688;
}
else 
{
returnVal2 = 259;
label_113688:; 
goto label_113700;
}
}
}
else 
{
returnVal2 = 259;
label_113700:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113829;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113805;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113805:; 
goto label_113829;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113829:; 
 __return_113830 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113732;
}
status9 = __return_113830;
goto label_113008;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_113684;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113684;
}
else 
{
returnVal2 = 259;
label_113684:; 
goto label_113696;
}
}
}
else 
{
returnVal2 = 259;
label_113696:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113825;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113801;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113801:; 
goto label_113825;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113825:; 
 __return_113832 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113732;
}
status9 = __return_113832;
goto label_113008;
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
int __tmp_507 = DeviceObject;
int __tmp_508 = Irp;
int __tmp_509 = lcontext;
int DeviceObject = __tmp_507;
int Irp = __tmp_508;
int Event = __tmp_509;
{
int __tmp_510 = Event;
int __tmp_511 = 0;
int __tmp_512 = 0;
int Event = __tmp_510;
int Increment = __tmp_511;
int Wait = __tmp_512;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_113457 = l;
}
 __return_113460 = -1073741802;
}
compRetStatus = __return_113460;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_113559;
goto label_113559;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_113686;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113686;
}
else 
{
returnVal2 = 259;
label_113686:; 
goto label_113698;
}
}
}
else 
{
returnVal2 = 259;
label_113698:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113827;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113803;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113803:; 
goto label_113827;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113827:; 
 __return_113831 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113732;
}
status9 = __return_113831;
goto label_113008;
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
if (compRegistered != 0)
{
{
__VERIFIER_error();
}
irpSp__Control = 224;
{
int __tmp_513 = deviceExtension__TargetDeviceObject;
int __tmp_514 = Irp;
int DeviceObject = __tmp_513;
int Irp = __tmp_514;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_113268;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113268;
}
else 
{
returnVal2 = 259;
label_113268:; 
goto label_113280;
}
}
}
else 
{
returnVal2 = 259;
label_113280:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113409;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113385;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113385:; 
goto label_113409;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113409:; 
 __return_113419 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_113318:; 
 __return_113420 = returnVal2;
}
status9 = __return_113419;
goto label_113008;
status9 = __return_113420;
goto label_113008;
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_515 = DeviceObject;
int __tmp_516 = Irp;
int __tmp_517 = lcontext;
int DeviceObject = __tmp_515;
int Irp = __tmp_516;
int Context = __tmp_517;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_113072;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_518 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_518;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_113088;
}
else 
{
label_113088:; 
}
label_113091:; 
 __return_113108 = myStatus;
}
compRetStatus = __return_113108;
goto label_113112;
}
else 
{
 __return_113109 = myStatus;
}
compRetStatus = __return_113109;
goto label_113112;
}
}
else 
{
label_113072:; 
if (myStatus >= 0)
{
{
int __tmp_519 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_519;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_113103;
}
else 
{
label_113103:; 
}
goto label_113091;
}
}
else 
{
 __return_113110 = myStatus;
}
compRetStatus = __return_113110;
label_113112:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_113145;
label_113145:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_113274;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113274;
}
else 
{
returnVal2 = 259;
label_113274:; 
goto label_113286;
}
}
}
else 
{
returnVal2 = 259;
label_113286:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113415;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113391;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113391:; 
goto label_113415;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113415:; 
 __return_113416 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113318;
}
status9 = __return_113416;
goto label_113008;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_113270;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113270;
}
else 
{
returnVal2 = 259;
label_113270:; 
goto label_113282;
}
}
}
else 
{
returnVal2 = 259;
label_113282:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113411;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113387;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113387:; 
goto label_113411;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113411:; 
 __return_113418 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113318;
}
status9 = __return_113418;
goto label_113008;
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
int __tmp_520 = DeviceObject;
int __tmp_521 = Irp;
int __tmp_522 = lcontext;
int DeviceObject = __tmp_520;
int Irp = __tmp_521;
int Event = __tmp_522;
{
int __tmp_523 = Event;
int __tmp_524 = 0;
int __tmp_525 = 0;
int Event = __tmp_523;
int Increment = __tmp_524;
int Wait = __tmp_525;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_113043 = l;
}
 __return_113046 = -1073741802;
}
compRetStatus = __return_113046;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_113145;
goto label_113145;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_113272;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113272;
}
else 
{
returnVal2 = 259;
label_113272:; 
goto label_113284;
}
}
}
else 
{
returnVal2 = 259;
label_113284:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113413;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113389;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113389:; 
goto label_113413;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113413:; 
 __return_113417 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113318;
}
status9 = __return_113417;
goto label_113008;
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
int __tmp_526 = deviceExtension__TargetDeviceObject;
int __tmp_527 = Irp;
int DeviceObject = __tmp_526;
int Irp = __tmp_527;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_112854;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112854;
}
else 
{
returnVal2 = 259;
label_112854:; 
goto label_112866;
}
}
}
else 
{
returnVal2 = 259;
label_112866:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112995;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112971;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112971:; 
goto label_112995;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112995:; 
 __return_113005 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_112904:; 
 __return_113006 = returnVal2;
}
status9 = __return_113005;
goto label_113008;
status9 = __return_113006;
label_113008:; 
status9 = 259;
if (status9 == 0)
{
 __return_113910 = status9;
}
else 
{
{
int __tmp_533 = event;
int __tmp_534 = Executive;
int __tmp_535 = KernelMode;
int __tmp_536 = 0;
int __tmp_537 = 0;
int Object = __tmp_533;
int WaitReason = __tmp_534;
int WaitMode = __tmp_535;
int Alertable = __tmp_536;
int Timeout = __tmp_537;
if (s == MPR3)
{
if (setEventCalled == 1)
{
s = NP;
setEventCalled = 0;
goto label_113880;
}
else 
{
goto label_113857;
}
}
else 
{
label_113857:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_113880;
}
else 
{
if (s == MPR3)
{
{
__VERIFIER_error();
}
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_113899 = 0;
goto label_113900;
}
else 
{
 __return_113900 = -1073741823;
label_113900:; 
}
goto label_113904;
}
else 
{
label_113880:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_113901 = 0;
goto label_113902;
}
else 
{
 __return_113902 = -1073741823;
label_113902:; 
}
label_113904:; 
status9 = myStatus;
 __return_113909 = status9;
}
status3 = __return_113909;
goto label_113912;
}
}
}
}
status3 = __return_113910;
label_113912:; 
if (status3 >= 0)
{
goto label_113924;
}
else 
{
if (setPagable == 1)
{
setPagable = 0;
goto label_113924;
}
else 
{
label_113924:; 
{
int __tmp_528 = deviceExtension__PagingPathCountEvent;
int __tmp_529 = 0;
int __tmp_530 = 0;
int Event = __tmp_528;
int Increment = __tmp_529;
int Wait = __tmp_530;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_113931 = l;
}
{
int __tmp_531 = Irp;
int __tmp_532 = 0;
int Irp = __tmp_531;
int PriorityBoost = __tmp_532;
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
goto label_113953;
label_113953:; 
 __return_113956 = status3;
}
status10 = __return_113956;
goto label_115952;
}
}
}
}
}
}
else 
{
if (routine == 0)
{
{
int __tmp_538 = DeviceObject;
int __tmp_539 = Irp;
int __tmp_540 = lcontext;
int DeviceObject = __tmp_538;
int Irp = __tmp_539;
int Context = __tmp_540;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_112658;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_541 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_541;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_112674;
}
else 
{
label_112674:; 
}
label_112677:; 
 __return_112694 = myStatus;
}
compRetStatus = __return_112694;
goto label_112698;
}
else 
{
 __return_112695 = myStatus;
}
compRetStatus = __return_112695;
goto label_112698;
}
}
else 
{
label_112658:; 
if (myStatus >= 0)
{
{
int __tmp_542 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_542;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_112689;
}
else 
{
label_112689:; 
}
goto label_112677;
}
}
else 
{
 __return_112696 = myStatus;
}
compRetStatus = __return_112696;
label_112698:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_112731;
label_112731:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_112860;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112860;
}
else 
{
returnVal2 = 259;
label_112860:; 
goto label_112872;
}
}
}
else 
{
returnVal2 = 259;
label_112872:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113001;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112977;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112977:; 
goto label_113001;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113001:; 
 __return_113002 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112904;
}
status9 = __return_113002;
goto label_113008;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_112856;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112856;
}
else 
{
returnVal2 = 259;
label_112856:; 
goto label_112868;
}
}
}
else 
{
returnVal2 = 259;
label_112868:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112997;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112973;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112973:; 
goto label_112997;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112997:; 
 __return_113004 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112904;
}
status9 = __return_113004;
goto label_113008;
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
int __tmp_543 = DeviceObject;
int __tmp_544 = Irp;
int __tmp_545 = lcontext;
int DeviceObject = __tmp_543;
int Irp = __tmp_544;
int Event = __tmp_545;
{
int __tmp_546 = Event;
int __tmp_547 = 0;
int __tmp_548 = 0;
int Event = __tmp_546;
int Increment = __tmp_547;
int Wait = __tmp_548;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_112629 = l;
}
 __return_112632 = -1073741802;
}
compRetStatus = __return_112632;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_112731;
goto label_112731;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_112858;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112858;
}
else 
{
returnVal2 = 259;
label_112858:; 
goto label_112870;
}
}
}
else 
{
returnVal2 = 259;
label_112870:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112999;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112975;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112975:; 
goto label_112999;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112999:; 
 __return_113003 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112904;
}
status9 = __return_113003;
goto label_113008;
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
int __tmp_549 = DeviceObject;
int __tmp_550 = Irp;
int DeviceObject = __tmp_549;
int Irp = __tmp_550;
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
int __tmp_551 = deviceExtension__TargetDeviceObject;
int __tmp_552 = Irp;
int DeviceObject = __tmp_551;
int Irp = __tmp_552;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int returnVal2 ;
int compRetStatus ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp8 ;
if (compRegistered == 0)
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_112320;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112320;
}
else 
{
returnVal2 = 259;
label_112320:; 
goto label_112332;
}
}
}
else 
{
returnVal2 = 259;
label_112332:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112461;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112437;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112437:; 
goto label_112461;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112461:; 
 __return_112471 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_112370:; 
 __return_112472 = returnVal2;
}
tmp = __return_112471;
goto label_112474;
tmp = __return_112472;
label_112474:; 
 __return_112483 = tmp;
}
tmp___0 = __return_112483;
 __return_112486 = tmp___0;
}
status10 = __return_112486;
goto label_115952;
}
}
else 
{
if (routine == 0)
{
{
int __tmp_553 = DeviceObject;
int __tmp_554 = Irp;
int __tmp_555 = lcontext;
int DeviceObject = __tmp_553;
int Irp = __tmp_554;
int Context = __tmp_555;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_112124;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_556 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_556;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_112140;
}
else 
{
label_112140:; 
}
label_112143:; 
 __return_112160 = myStatus;
}
compRetStatus = __return_112160;
goto label_112164;
}
else 
{
 __return_112161 = myStatus;
}
compRetStatus = __return_112161;
goto label_112164;
}
}
else 
{
label_112124:; 
if (myStatus >= 0)
{
{
int __tmp_557 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_557;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_112155;
}
else 
{
label_112155:; 
}
goto label_112143;
}
}
else 
{
 __return_112162 = myStatus;
}
compRetStatus = __return_112162;
label_112164:; 
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_112197;
label_112197:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_112326;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112326;
}
else 
{
returnVal2 = 259;
label_112326:; 
goto label_112338;
}
}
}
else 
{
returnVal2 = 259;
label_112338:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112467;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112443;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112443:; 
goto label_112467;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112467:; 
 __return_112468 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112370;
}
tmp = __return_112468;
goto label_112474;
}
}
}
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_112322;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112322;
}
else 
{
returnVal2 = 259;
label_112322:; 
goto label_112334;
}
}
}
else 
{
returnVal2 = 259;
label_112334:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112463;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112439;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112439:; 
goto label_112463;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112463:; 
 __return_112470 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112370;
}
tmp = __return_112470;
goto label_112474;
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
int __tmp_558 = DeviceObject;
int __tmp_559 = Irp;
int __tmp_560 = lcontext;
int DeviceObject = __tmp_558;
int Irp = __tmp_559;
int Event = __tmp_560;
{
int __tmp_561 = Event;
int __tmp_562 = 0;
int __tmp_563 = 0;
int Event = __tmp_561;
int Increment = __tmp_562;
int Wait = __tmp_563;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_112095 = l;
}
 __return_112098 = -1073741802;
}
compRetStatus = __return_112098;
__cil_tmp8 = (unsigned long)compRetStatus;
if (__cil_tmp8 == -1073741802)
{
{
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
goto label_112197;
goto label_112197;
}
}
else 
{
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_112324;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112324;
}
else 
{
returnVal2 = 259;
label_112324:; 
goto label_112336;
}
}
}
else 
{
returnVal2 = 259;
label_112336:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112465;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112441;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112441:; 
goto label_112465;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112465:; 
 __return_112469 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112370;
}
tmp = __return_112469;
goto label_112474;
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
if (tmp_ndt_3 == 4)
{
{
int __tmp_564 = devobj;
int __tmp_565 = pirp;
int DeviceObject = __tmp_564;
int Irp = __tmp_565;
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
int __tmp_566 = deviceExtension__TargetDeviceObject;
int __tmp_567 = Irp;
int DeviceObject = __tmp_566;
int Irp = __tmp_567;
int compRetStatus ;
int returnVal ;
int lcontext = __VERIFIER_nondet_int() ;
lcontext = __VERIFIER_nondet_int();
unsigned long __cil_tmp7 ;
long __cil_tmp8 ;
if (compRegistered == 0)
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal = 0;
goto label_111851;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal = -1073741823;
goto label_111851;
}
else 
{
returnVal = 259;
label_111851:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_111993;
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
goto label_111963;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_111963:; 
goto label_111993;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_111993:; 
 __return_111994 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
 __return_111999 = returnVal;
}
tmp = __return_111994;
goto label_112001;
tmp = __return_111999;
label_112001:; 
 __return_112012 = tmp;
}
status10 = __return_112012;
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_125877;
}
else 
{
goto label_125523;
}
}
else 
{
label_125523:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_125877;
}
else 
{
goto label_125558;
}
}
else 
{
label_125558:; 
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
goto label_125806;
}
else 
{
goto label_125648;
}
}
else 
{
goto label_125648;
}
}
else 
{
label_125648:; 
if (pended != 1)
{
if (s == DC)
{
if (status10 == 259)
{
{
__VERIFIER_error();
}
goto label_125762;
}
else 
{
goto label_125877;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_125710;
}
else 
{
goto label_125877;
}
}
}
else 
{
goto label_125877;
}
}
}
else 
{
goto label_125877;
}
}
else 
{
label_125877:; 
 __return_125887 = status10;
goto label_111587;
}
}
}
}
else 
{
{
int __tmp_568 = d;
int DriverObject = __tmp_568;
}
label_125476:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_125879;
}
else 
{
goto label_125525;
}
}
else 
{
label_125525:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_125879;
}
else 
{
goto label_125556;
}
}
else 
{
label_125556:; 
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
goto label_125806;
}
else 
{
goto label_125646;
}
}
else 
{
goto label_125646;
}
}
else 
{
label_125646:; 
if (pended != 1)
{
if (s == DC)
{
if (status10 == 259)
{
{
__VERIFIER_error();
}
label_125762:; 
 __return_125895 = status10;
goto label_111587;
}
else 
{
goto label_125879;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_125710;
}
else 
{
goto label_125879;
}
}
}
else 
{
goto label_125879;
}
}
}
else 
{
goto label_125879;
}
}
else 
{
label_125879:; 
 __return_125885 = status10;
goto label_111587;
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
if (routine == 0)
{
{
int __tmp_569 = DeviceObject;
int __tmp_570 = Irp;
int __tmp_571 = lcontext;
int DeviceObject = __tmp_569;
int Irp = __tmp_570;
int Context = __tmp_571;
int Irp__PendingReturned = __VERIFIER_nondet_int() ;
Irp__PendingReturned = __VERIFIER_nondet_int();
int Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int() ;
Irp__AssociatedIrp__SystemBuffer = __VERIFIER_nondet_int();
if (!(Irp__PendingReturned == 0))
{
if (pended == 0)
{
pended = 1;
goto label_111665;
}
else 
{
{
__VERIFIER_error();
}
if (myStatus >= 0)
{
{
int __tmp_572 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_572;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_111681;
}
else 
{
label_111681:; 
}
label_111684:; 
 __return_111701 = myStatus;
}
compRetStatus = __return_111701;
goto label_111705;
}
else 
{
 __return_111702 = myStatus;
}
compRetStatus = __return_111702;
goto label_111705;
}
}
else 
{
label_111665:; 
if (myStatus >= 0)
{
{
int __tmp_573 = Irp__AssociatedIrp__SystemBuffer;
int Toc = __tmp_573;
int index = __VERIFIER_nondet_int() ;
index = __VERIFIER_nondet_int();
if (!(index == 0))
{
int __CPAchecker_TMP_0 = index;
index = index - 1;
__CPAchecker_TMP_0;
goto label_111696;
}
else 
{
label_111696:; 
}
goto label_111684;
}
}
else 
{
 __return_111703 = myStatus;
}
compRetStatus = __return_111703;
label_111705:; 
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
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
goto label_111738;
label_111738:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal = 0;
goto label_111845;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal = -1073741823;
goto label_111845;
}
else 
{
returnVal = 259;
label_111845:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_111987;
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
goto label_111969;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_111969:; 
goto label_111987;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_111987:; 
 __return_111997 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
label_111891:; 
 __return_111998 = returnVal;
}
tmp = __return_111997;
goto label_112001;
tmp = __return_111998;
goto label_112001;
}
}
}
}
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal = 0;
goto label_111849;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal = -1073741823;
goto label_111849;
}
else 
{
returnVal = 259;
label_111849:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_111991;
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
goto label_111965;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_111965:; 
goto label_111991;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_111991:; 
 __return_111995 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_111891;
}
tmp = __return_111995;
goto label_112001;
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
int __tmp_574 = DeviceObject;
int __tmp_575 = Irp;
int __tmp_576 = lcontext;
int DeviceObject = __tmp_574;
int Irp = __tmp_575;
int Event = __tmp_576;
{
int __tmp_577 = Event;
int __tmp_578 = 0;
int __tmp_579 = 0;
int Event = __tmp_577;
int Increment = __tmp_578;
int Wait = __tmp_579;
int l = __VERIFIER_nondet_int() ;
l = __VERIFIER_nondet_int();
setEventCalled = 1;
 __return_111636 = l;
}
 __return_111639 = -1073741802;
}
compRetStatus = __return_111639;
__cil_tmp7 = (unsigned long)compRetStatus;
if (__cil_tmp7 == -1073741802)
{
{
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
goto label_111738;
goto label_111738;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal = 0;
goto label_111847;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal = -1073741823;
goto label_111847;
}
else 
{
returnVal = 259;
label_111847:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_111989;
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
goto label_111967;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_111967:; 
goto label_111989;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_111989:; 
 __return_111996 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_111891;
}
tmp = __return_111996;
goto label_112001;
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
 __return_111587 = -1;
label_111587:; 
return 1;
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
goto label_125881;
}
else 
{
goto label_125527;
}
}
else 
{
label_125527:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_125881;
}
else 
{
goto label_125554;
}
}
else 
{
label_125554:; 
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
label_125806:; 
 __return_125893 = status10;
goto label_125883;
}
else 
{
goto label_125644;
}
}
else 
{
goto label_125644;
}
}
else 
{
label_125644:; 
if (pended != 1)
{
if (s == DC)
{
goto label_125881;
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
label_125710:; 
 __return_125897 = status10;
goto label_125883;
}
else 
{
goto label_125881;
}
}
}
else 
{
goto label_125881;
}
}
}
else 
{
goto label_125881;
}
}
else 
{
label_125881:; 
 __return_125883 = status10;
label_125883:; 
return 1;
}
}
}
}
}
