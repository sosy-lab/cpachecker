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
int __return_125889;
int __return_124874;
int __return_124872;
int __return_124875;
int __return_125390;
int __return_125393;
int __return_125433;
int __return_124952;
int __return_125355;
int __return_125358;
int __return_125026;
int __return_125324;
int __return_124950;
int __return_124951;
int __return_124949;
int __return_125058;
int __return_125323;
int __return_124837;
int __return_124838;
int __return_124839;
int __return_124840;
int __return_124870;
int __return_124867;
int __return_124869;
int __return_124868;
int __return_124555;
int __return_124553;
int __return_124556;
int __return_124751;
int __return_124754;
int __return_124626;
int __return_125090;
int __return_125322;
int __return_124677;
int __return_124680;
int __return_125122;
int __return_125321;
int __return_124624;
int __return_124625;
int __return_124623;
int __return_125154;
int __return_125320;
int __return_124518;
int __return_124519;
int __return_124520;
int __return_124521;
int __return_124551;
int __return_124548;
int __return_124550;
int __return_124549;
int __return_125250;
int __return_125317;
int __return_124424;
int __return_124427;
int __return_124389;
int __return_124392;
int __return_124341;
int __return_125186;
int __return_125319;
int __return_124339;
int __return_124340;
int __return_124338;
int __return_124268;
int __return_125282;
int __return_124266;
int __return_124267;
int __return_124265;
int __return_124197;
int __return_125314;
int __return_124195;
int __return_124196;
int __return_124194;
int __return_124129;
int __return_125218;
int __return_125318;
int __return_124127;
int __return_124128;
int __return_124126;
int __return_122315;
int __return_122313;
int __return_122316;
int __return_122765;
int __return_122766;
int __return_123191;
int __return_124060;
int __return_122454;
int __return_122455;
int __return_122456;
int __return_122762;
int __return_122764;
int __return_122389;
int __return_122392;
int __return_122763;
int __return_123179;
int __return_123180;
int __return_122868;
int __return_122869;
int __return_122870;
int __return_123176;
int __return_123178;
int __return_122803;
int __return_122806;
int __return_123177;
int __return_122278;
int __return_122279;
int __return_122280;
int __return_122281;
int __return_122311;
int __return_122308;
int __return_122310;
int __return_122309;
int __return_123631;
int __return_123632;
int __return_124057;
int __return_123320;
int __return_123321;
int __return_123322;
int __return_123628;
int __return_123630;
int __return_123255;
int __return_123258;
int __return_123629;
int __return_124045;
int __return_124046;
int __return_123734;
int __return_123735;
int __return_123736;
int __return_124042;
int __return_124044;
int __return_123669;
int __return_123672;
int __return_124043;
int __return_121945;
int __return_121444;
int __return_121442;
int __return_121445;
int __return_121626;
int __return_121515;
int __return_121594;
int __return_121944;
int __return_121513;
int __return_121514;
int __return_121512;
int __return_121407;
int __return_121408;
int __return_121409;
int __return_121410;
int __return_121440;
int __return_121437;
int __return_121439;
int __return_121438;
int __return_121210;
int __return_121322;
int __return_121281;
int __return_121946;
int __return_125434;
int __return_121279;
int __return_121280;
int __return_121278;
int __return_121208;
int __return_121209;
int __return_121207;
int __return_121129;
int __return_121127;
int __return_121128;
int __return_121126;
int __return_121071;
int __return_121040;
int __return_120875;
int __return_121009;
int __return_120938;
int __return_120979;
int __return_120936;
int __return_120937;
int __return_120935;
int __return_120873;
int __return_120874;
int __return_120872;
int __return_120815;
int __return_120769;
int __return_120767;
int __return_120768;
int __return_120766;
int __return_120708;
int __return_120676;
int __return_120623;
int __return_120621;
int __return_120622;
int __return_120620;
int __return_120553;
int __return_120551;
int __return_120552;
int __return_120550;
int __return_119097;
int __return_119095;
int __return_119098;
int __return_120484;
int __return_120404;
int __return_120405;
int __return_120416;
int __return_120093;
int __return_120094;
int __return_120095;
int __return_120401;
int __return_120403;
int __return_120028;
int __return_120031;
int __return_120402;
int __return_119060;
int __return_119061;
int __return_119062;
int __return_119063;
int __return_119093;
int __return_119552;
int __return_119553;
int __return_119978;
int __return_120419;
int __return_119241;
int __return_119242;
int __return_119243;
int __return_119549;
int __return_119551;
int __return_119176;
int __return_119179;
int __return_119550;
int __return_119966;
int __return_119967;
int __return_119655;
int __return_119656;
int __return_119657;
int __return_119963;
int __return_119965;
int __return_119590;
int __return_119593;
int __return_119964;
int __return_119090;
int __return_119092;
int __return_119091;
int __return_118962;
int __return_118963;
int __return_118974;
int __return_118977;
int __return_118651;
int __return_118652;
int __return_118653;
int __return_118959;
int __return_118961;
int __return_118586;
int __return_118589;
int __return_118960;
int __return_118248;
int __return_118309;
int __return_125435;
int __return_118284;
int __return_118246;
int __return_118247;
int __return_118245;
int __return_118178;
int __return_118179;
int __return_118190;
int __return_118193;
int __return_117867;
int __return_117868;
int __return_117869;
int __return_118175;
int __return_118177;
int __return_117802;
int __return_117805;
int __return_118176;
int __return_117706;
int __return_117707;
int __return_117395;
int __return_117396;
int __return_117397;
int __return_117703;
int __return_117705;
int __return_117330;
int __return_117333;
int __return_117704;
int __return_117292;
int __return_117293;
int __return_117718;
int __return_125436;
int __return_116981;
int __return_116982;
int __return_116983;
int __return_117289;
int __return_117291;
int __return_116916;
int __return_116919;
int __return_117290;
int __return_116855;
int __return_116856;
int __return_116867;
int __return_116870;
int __return_116544;
int __return_116545;
int __return_116546;
int __return_116852;
int __return_116854;
int __return_116479;
int __return_116482;
int __return_116853;
int __return_116392;
int __return_116393;
int __return_116404;
int __return_125437;
int __return_125881;
int __return_116081;
int __return_116082;
int __return_116083;
int __return_116389;
int __return_116391;
int __return_116016;
int __return_116019;
int __return_116390;
int __return_115674;
int __return_115675;
int __return_115363;
int __return_115364;
int __return_115365;
int __return_115671;
int __return_115673;
int __return_115298;
int __return_115301;
int __return_115672;
int __return_115260;
int __return_115261;
int __return_114949;
int __return_114950;
int __return_114951;
int __return_115257;
int __return_115259;
int __return_114884;
int __return_114887;
int __return_115258;
int __return_114846;
int __return_114847;
int __return_115751;
int __return_115901;
int __return_115940;
int __return_125879;
int __return_115900;
int __return_115830;
int __return_115828;
int __return_115829;
int __return_115827;
int __return_115898;
int __return_115895;
int __return_115873;
int __return_115874;
int __return_115894;
int __return_115896;
int __return_115889;
int __return_115890;
int __return_115893;
int __return_115740;
int __return_115741;
int __return_115742;
int __return_115743;
int __return_115750;
int __return_114535;
int __return_114536;
int __return_114537;
int __return_114843;
int __return_114845;
int __return_114470;
int __return_114473;
int __return_114844;
int __return_114264;
int __return_114265;
int __return_114276;
int __return_114279;
int __return_113953;
int __return_113954;
int __return_113955;
int __return_114261;
int __return_114263;
int __return_113888;
int __return_113891;
int __return_114262;
int __return_112320;
int __return_112321;
int __return_112322;
int __return_112323;
int __return_113717;
int __return_113718;
int __return_113406;
int __return_113407;
int __return_113408;
int __return_113714;
int __return_113716;
int __return_113341;
int __return_113344;
int __return_113715;
int __return_113303;
int __return_113304;
int __return_112992;
int __return_112993;
int __return_112994;
int __return_113300;
int __return_113302;
int __return_112927;
int __return_112930;
int __return_113301;
int __return_112889;
int __return_112890;
int __return_113794;
int __return_113815;
int __return_113840;
int __return_113783;
int __return_113784;
int __return_113785;
int __return_113786;
int __return_113793;
int __return_112578;
int __return_112579;
int __return_112580;
int __return_112886;
int __return_112888;
int __return_112513;
int __return_112516;
int __return_112887;
int __return_112249;
int __return_112250;
int __return_112261;
int __return_112264;
int __return_111938;
int __return_111939;
int __return_111940;
int __return_112246;
int __return_112248;
int __return_111873;
int __return_111876;
int __return_112247;
int __return_111772;
int __return_111777;
int __return_111790;
int __return_125877;
int __return_125885;
int __return_125875;
int __return_111479;
int __return_111480;
int __return_111481;
int __return_111775;
int __return_111776;
int __return_111773;
int __return_111414;
int __return_111417;
int __return_111774;
int __return_111365;
int __return_125883;
int __return_125887;
int __return_125873;
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
goto label_111334;
}
else 
{
label_111334:; 
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
 __return_125889 = -1;
goto label_111365;
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
 __return_124874 = 0;
goto label_124875;
}
else 
{
if (currentBuffer == 0)
{
 __return_124872 = 0;
goto label_124875;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_124875 = 0;
label_124875:; 
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
goto label_124818;
}
else 
{
goto label_124795;
}
}
else 
{
label_124795:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_124818;
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
 __return_124837 = 0;
goto label_124838;
}
else 
{
 __return_124838 = -1073741823;
label_124838:; 
}
goto label_124842;
}
else 
{
label_124818:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_124839 = 0;
goto label_124840;
}
else 
{
 __return_124840 = -1073741823;
label_124840:; 
}
label_124842:; 
status5 = ioStatus__Status;
if (status5 < 0)
{
 __return_124870 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_124866;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_124866:; 
 __return_124867 = returnValue;
}
tmp = __return_124867;
goto label_124877;
}
tmp = __return_124870;
goto label_124877;
}
}
}
}
}
else 
{
if (status5 < 0)
{
 __return_124869 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_124864;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_124864:; 
 __return_124868 = returnValue;
}
tmp = __return_124868;
goto label_124877;
}
tmp = __return_124869;
goto label_124877;
}
}
tmp = __return_124875;
label_124877:; 
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
goto label_125387;
label_125387:; 
 __return_125390 = status;
}
tmp___0 = __return_125390;
 __return_125393 = tmp___0;
}
status4 = __return_125393;
label_125395:; 
 __return_125433 = status4;
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
 __return_124952 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124950 = status1;
}
status6 = __return_124952;
label_124954:; 
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
goto label_125352;
label_125352:; 
 __return_125355 = status;
}
tmp___1 = __return_125355;
 __return_125358 = tmp___1;
}
status4 = __return_125358;
goto label_125395;
}
else 
{
status6 = 0;
Irp__IoStatus__Information = bytesTransfered;
if (lastSession__LogicalBlockAddress == 0)
{
goto label_124972;
}
else 
{
cdaudioDataOut__FirstTrack = 1;
cdaudioDataOut__LastTrack = 2;
label_124972:; 
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
goto label_125023;
label_125023:; 
 __return_125026 = status;
}
tmp___8 = __return_125026;
 __return_125324 = tmp___8;
}
status4 = __return_125324;
goto label_125395;
}
}
status6 = __return_124950;
goto label_124954;
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
 __return_124951 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124949 = status1;
}
status6 = __return_124951;
goto label_124954;
status6 = __return_124949;
goto label_124954;
}
}
}
status10 = __return_125433;
goto label_125439;
}
else 
{
status6 = -1073741789;
Irp__IoStatus__Information = 0;
goto label_124896;
}
}
else 
{
status6 = -2147483631;
Irp__IoStatus__Information = 0;
label_124896:; 
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
goto label_125055;
label_125055:; 
 __return_125058 = status;
}
tmp___8 = __return_125058;
 __return_125323 = tmp___8;
}
status4 = __return_125323;
goto label_125395;
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
 __return_124555 = 0;
goto label_124556;
}
else 
{
if (currentBuffer == 0)
{
 __return_124553 = 0;
goto label_124556;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_124556 = 0;
label_124556:; 
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
goto label_124499;
}
else 
{
goto label_124476;
}
}
else 
{
label_124476:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_124499;
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
 __return_124518 = 0;
goto label_124519;
}
else 
{
 __return_124519 = -1073741823;
label_124519:; 
}
goto label_124523;
}
else 
{
label_124499:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_124520 = 0;
goto label_124521;
}
else 
{
 __return_124521 = -1073741823;
label_124521:; 
}
label_124523:; 
status5 = ioStatus__Status;
if (status5 < 0)
{
 __return_124551 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_124547;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_124547:; 
 __return_124548 = returnValue;
}
tmp___2 = __return_124548;
goto label_124558;
}
tmp___2 = __return_124551;
goto label_124558;
}
}
}
}
}
else 
{
if (status5 < 0)
{
 __return_124550 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_124545;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_124545:; 
 __return_124549 = returnValue;
}
tmp___2 = __return_124549;
goto label_124558;
}
tmp___2 = __return_124550;
goto label_124558;
}
}
tmp___2 = __return_124556;
label_124558:; 
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
goto label_124748;
label_124748:; 
 __return_124751 = status;
}
tmp___3 = __return_124751;
 __return_124754 = tmp___3;
}
status4 = __return_124754;
goto label_125395;
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
 __return_124626 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124624 = status1;
}
status6 = __return_124626;
label_124628:; 
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
goto label_124703;
}
else 
{
tracksToReturn = tracksOnCd;
label_124703:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_124715;
}
else 
{
label_124715:; 
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
goto label_125087;
label_125087:; 
 __return_125090 = status;
}
tmp___8 = __return_125090;
 __return_125322 = tmp___8;
}
status4 = __return_125322;
goto label_125395;
}
}
}
else 
{
goto label_124642;
}
}
else 
{
label_124642:; 
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
goto label_124674;
label_124674:; 
 __return_124677 = status;
}
tmp___4 = __return_124677;
 __return_124680 = tmp___4;
}
status4 = __return_124680;
goto label_125395;
}
else 
{
__cil_tmp109 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp109 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_124701;
}
else 
{
tracksToReturn = tracksOnCd;
label_124701:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_124717;
}
else 
{
label_124717:; 
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
goto label_125119;
label_125119:; 
 __return_125122 = status;
}
tmp___8 = __return_125122;
 __return_125321 = tmp___8;
}
status4 = __return_125321;
goto label_125395;
}
}
}
}
status6 = __return_124624;
goto label_124628;
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
 __return_124625 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124623 = status1;
}
status6 = __return_124625;
goto label_124628;
status6 = __return_124623;
goto label_124628;
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
goto label_125151;
label_125151:; 
 __return_125154 = status;
}
tmp___8 = __return_125154;
 __return_125320 = tmp___8;
}
status4 = __return_125320;
goto label_125395;
}
}
}
}
}
else 
{
status6 = -1073741789;
Irp__IoStatus__Information = 0;
label_124988:; 
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
goto label_125247;
label_125247:; 
 __return_125250 = status;
}
tmp___8 = __return_125250;
label_125252:; 
 __return_125317 = tmp___8;
}
status4 = __return_125317;
goto label_125395;
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
goto label_124988;
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
goto label_124421;
label_124421:; 
 __return_124424 = status;
}
tmp___5 = __return_124424;
 __return_124427 = tmp___5;
}
status4 = __return_124427;
goto label_125395;
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
goto label_124386;
label_124386:; 
 __return_124389 = status;
}
tmp___6 = __return_124389;
 __return_124392 = tmp___6;
}
status4 = __return_124392;
goto label_125395;
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
 __return_124341 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124339 = status1;
}
status6 = __return_124341;
label_124343:; 
if (status6 >= 0)
{
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_124356;
}
else 
{
Irp__IoStatus__Information = 0;
label_124356:; 
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
goto label_125183;
label_125183:; 
 __return_125186 = status;
}
tmp___8 = __return_125186;
 __return_125319 = tmp___8;
}
status4 = __return_125319;
goto label_125395;
}
status6 = __return_124339;
goto label_124343;
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
 __return_124340 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124338 = status1;
}
status6 = __return_124340;
goto label_124343;
status6 = __return_124338;
goto label_124343;
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
goto label_124988;
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
 __return_124268 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124266 = status1;
}
status6 = __return_124268;
label_124270:; 
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
goto label_125279;
label_125279:; 
 __return_125282 = status;
}
tmp___8 = __return_125282;
goto label_125252;
}
status6 = __return_124266;
goto label_124270;
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
 __return_124267 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124265 = status1;
}
status6 = __return_124267;
goto label_124270;
status6 = __return_124265;
goto label_124270;
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
goto label_124988;
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
 __return_124197 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124195 = status1;
}
status6 = __return_124197;
label_124199:; 
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
goto label_125311;
label_125311:; 
 __return_125314 = status;
}
tmp___8 = __return_125314;
goto label_125252;
}
status6 = __return_124195;
goto label_124199;
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
 __return_124196 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124194 = status1;
}
status6 = __return_124196;
goto label_124199;
status6 = __return_124194;
goto label_124199;
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
 __return_124129 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124127 = status1;
}
status6 = __return_124129;
label_124131:; 
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
goto label_125215;
label_125215:; 
 __return_125218 = status;
}
tmp___8 = __return_125218;
 __return_125318 = tmp___8;
}
status4 = __return_125318;
goto label_125395;
status6 = __return_124127;
goto label_124131;
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
 __return_124128 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_124126 = status1;
}
status6 = __return_124128;
goto label_124131;
status6 = __return_124126;
goto label_124131;
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
label_124068:; 
Irp__IoStatus__Information = 0;
label_124071:; 
status6 = -1073741808;
goto label_124988;
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
goto label_124068;
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
goto label_124071;
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
 __return_122315 = 0;
goto label_122316;
}
else 
{
if (currentBuffer == 0)
{
 __return_122313 = 0;
goto label_122316;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_122316 = 0;
label_122316:; 
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
goto label_122259;
}
else 
{
goto label_122236;
}
}
else 
{
label_122236:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_122259;
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
 __return_122278 = 0;
goto label_122279;
}
else 
{
 __return_122279 = -1073741823;
label_122279:; 
}
goto label_122283;
}
else 
{
label_122259:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_122280 = 0;
goto label_122281;
}
else 
{
 __return_122281 = -1073741823;
label_122281:; 
}
label_122283:; 
status5 = ioStatus__Status;
if (status5 < 0)
{
 __return_122311 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_122307;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_122307:; 
 __return_122308 = returnValue;
}
goto label_122318;
}
goto label_122318;
}
}
}
}
}
else 
{
if (status5 < 0)
{
 __return_122310 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_122305;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_122305:; 
 __return_122309 = returnValue;
}
goto label_122318;
}
goto label_122318;
}
}
label_122318:; 
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
goto label_122614;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_122614;
}
else 
{
returnVal2 = 259;
label_122614:; 
goto label_122626;
}
}
}
else 
{
returnVal2 = 259;
label_122626:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_122755;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_122731;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_122731:; 
goto label_122755;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_122755:; 
 __return_122765 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_122664:; 
 __return_122766 = returnVal2;
}
tmp = __return_122765;
goto label_122768;
tmp = __return_122766;
label_122768:; 
 __return_123191 = tmp;
}
tmp___7 = __return_123191;
label_123193:; 
 __return_124060 = tmp___7;
}
status4 = __return_124060;
goto label_125395;
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
goto label_122418;
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
goto label_122434;
}
else 
{
label_122434:; 
}
label_122437:; 
 __return_122454 = myStatus;
}
compRetStatus = __return_122454;
goto label_122458;
}
else 
{
 __return_122455 = myStatus;
}
compRetStatus = __return_122455;
goto label_122458;
}
}
else 
{
label_122418:; 
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
goto label_122449;
}
else 
{
label_122449:; 
}
goto label_122437;
}
}
else 
{
 __return_122456 = myStatus;
}
compRetStatus = __return_122456;
label_122458:; 
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
goto label_122491;
label_122491:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_122620;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_122620;
}
else 
{
returnVal2 = 259;
label_122620:; 
goto label_122632;
}
}
}
else 
{
returnVal2 = 259;
label_122632:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_122761;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_122737;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_122737:; 
goto label_122761;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_122761:; 
 __return_122762 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_122664;
}
tmp = __return_122762;
goto label_122768;
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
goto label_122616;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_122616;
}
else 
{
returnVal2 = 259;
label_122616:; 
goto label_122628;
}
}
}
else 
{
returnVal2 = 259;
label_122628:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_122757;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_122733;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_122733:; 
goto label_122757;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_122757:; 
 __return_122764 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_122664;
}
tmp = __return_122764;
goto label_122768;
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
 __return_122389 = l;
}
 __return_122392 = -1073741802;
}
compRetStatus = __return_122392;
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
goto label_122491;
goto label_122491;
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
goto label_122618;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_122618;
}
else 
{
returnVal2 = 259;
label_122618:; 
goto label_122630;
}
}
}
else 
{
returnVal2 = 259;
label_122630:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_122759;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_122735;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_122735:; 
goto label_122759;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_122759:; 
 __return_122763 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_122664;
}
tmp = __return_122763;
goto label_122768;
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
goto label_123028;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123028;
}
else 
{
returnVal2 = 259;
label_123028:; 
goto label_123040;
}
}
}
else 
{
returnVal2 = 259;
label_123040:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123169;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123145;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123145:; 
goto label_123169;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123169:; 
 __return_123179 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_123078:; 
 __return_123180 = returnVal2;
}
tmp = __return_123179;
goto label_122768;
tmp = __return_123180;
goto label_122768;
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
goto label_122832;
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
goto label_122848;
}
else 
{
label_122848:; 
}
label_122851:; 
 __return_122868 = myStatus;
}
compRetStatus = __return_122868;
goto label_122872;
}
else 
{
 __return_122869 = myStatus;
}
compRetStatus = __return_122869;
goto label_122872;
}
}
else 
{
label_122832:; 
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
goto label_122863;
}
else 
{
label_122863:; 
}
goto label_122851;
}
}
else 
{
 __return_122870 = myStatus;
}
compRetStatus = __return_122870;
label_122872:; 
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
goto label_122905;
label_122905:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123034;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123034;
}
else 
{
returnVal2 = 259;
label_123034:; 
goto label_123046;
}
}
}
else 
{
returnVal2 = 259;
label_123046:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123175;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123151;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123151:; 
goto label_123175;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123175:; 
 __return_123176 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123078;
}
tmp = __return_123176;
goto label_122768;
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
goto label_123030;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123030;
}
else 
{
returnVal2 = 259;
label_123030:; 
goto label_123042;
}
}
}
else 
{
returnVal2 = 259;
label_123042:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123171;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123147;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123147:; 
goto label_123171;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123171:; 
 __return_123178 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123078;
}
tmp = __return_123178;
goto label_122768;
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
 __return_122803 = l;
}
 __return_122806 = -1073741802;
}
compRetStatus = __return_122806;
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
goto label_122905;
goto label_122905;
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
goto label_123032;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123032;
}
else 
{
returnVal2 = 259;
label_123032:; 
goto label_123044;
}
}
}
else 
{
returnVal2 = 259;
label_123044:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123173;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123149;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123149:; 
goto label_123173;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123173:; 
 __return_123177 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123078;
}
tmp = __return_123177;
goto label_122768;
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
goto label_123480;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123480;
}
else 
{
returnVal2 = 259;
label_123480:; 
goto label_123492;
}
}
}
else 
{
returnVal2 = 259;
label_123492:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123621;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123597;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123597:; 
goto label_123621;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123621:; 
 __return_123631 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_123530:; 
 __return_123632 = returnVal2;
}
tmp = __return_123631;
goto label_123634;
tmp = __return_123632;
label_123634:; 
 __return_124057 = tmp;
}
tmp___7 = __return_124057;
goto label_123193;
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
goto label_123284;
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
goto label_123300;
}
else 
{
label_123300:; 
}
label_123303:; 
 __return_123320 = myStatus;
}
compRetStatus = __return_123320;
goto label_123324;
}
else 
{
 __return_123321 = myStatus;
}
compRetStatus = __return_123321;
goto label_123324;
}
}
else 
{
label_123284:; 
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
goto label_123315;
}
else 
{
label_123315:; 
}
goto label_123303;
}
}
else 
{
 __return_123322 = myStatus;
}
compRetStatus = __return_123322;
label_123324:; 
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
goto label_123357;
label_123357:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123486;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123486;
}
else 
{
returnVal2 = 259;
label_123486:; 
goto label_123498;
}
}
}
else 
{
returnVal2 = 259;
label_123498:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123627;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123603;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123603:; 
goto label_123627;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123627:; 
 __return_123628 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123530;
}
tmp = __return_123628;
goto label_123634;
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
goto label_123482;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123482;
}
else 
{
returnVal2 = 259;
label_123482:; 
goto label_123494;
}
}
}
else 
{
returnVal2 = 259;
label_123494:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123623;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123599;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123599:; 
goto label_123623;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123623:; 
 __return_123630 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123530;
}
tmp = __return_123630;
goto label_123634;
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
 __return_123255 = l;
}
 __return_123258 = -1073741802;
}
compRetStatus = __return_123258;
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
goto label_123357;
goto label_123357;
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
goto label_123484;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123484;
}
else 
{
returnVal2 = 259;
label_123484:; 
goto label_123496;
}
}
}
else 
{
returnVal2 = 259;
label_123496:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_123625;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_123601;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_123601:; 
goto label_123625;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_123625:; 
 __return_123629 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123530;
}
tmp = __return_123629;
goto label_123634;
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
goto label_123894;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123894;
}
else 
{
returnVal2 = 259;
label_123894:; 
goto label_123906;
}
}
}
else 
{
returnVal2 = 259;
label_123906:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_124035;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_124011;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_124011:; 
goto label_124035;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_124035:; 
 __return_124045 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_123944:; 
 __return_124046 = returnVal2;
}
tmp = __return_124045;
goto label_123634;
tmp = __return_124046;
goto label_123634;
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
goto label_123698;
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
goto label_123714;
}
else 
{
label_123714:; 
}
label_123717:; 
 __return_123734 = myStatus;
}
compRetStatus = __return_123734;
goto label_123738;
}
else 
{
 __return_123735 = myStatus;
}
compRetStatus = __return_123735;
goto label_123738;
}
}
else 
{
label_123698:; 
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
goto label_123729;
}
else 
{
label_123729:; 
}
goto label_123717;
}
}
else 
{
 __return_123736 = myStatus;
}
compRetStatus = __return_123736;
label_123738:; 
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
goto label_123771;
label_123771:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_123900;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123900;
}
else 
{
returnVal2 = 259;
label_123900:; 
goto label_123912;
}
}
}
else 
{
returnVal2 = 259;
label_123912:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_124041;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_124017;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_124017:; 
goto label_124041;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_124041:; 
 __return_124042 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123944;
}
tmp = __return_124042;
goto label_123634;
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
goto label_123896;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123896;
}
else 
{
returnVal2 = 259;
label_123896:; 
goto label_123908;
}
}
}
else 
{
returnVal2 = 259;
label_123908:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_124037;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_124013;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_124013:; 
goto label_124037;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_124037:; 
 __return_124044 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123944;
}
tmp = __return_124044;
goto label_123634;
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
 __return_123669 = l;
}
 __return_123672 = -1073741802;
}
compRetStatus = __return_123672;
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
goto label_123771;
goto label_123771;
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
goto label_123898;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_123898;
}
else 
{
returnVal2 = 259;
label_123898:; 
goto label_123910;
}
}
}
else 
{
returnVal2 = 259;
label_123910:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_124039;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_124015;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_124015:; 
goto label_124039;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_124039:; 
 __return_124043 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_123944;
}
tmp = __return_124043;
goto label_123634;
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
label_121644:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121697;
}
else 
{
label_121697:; 
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
goto label_121765;
label_121765:; 
 __return_121945 = status7;
}
status4 = __return_121945;
goto label_121948;
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
 __return_121444 = 0;
goto label_121445;
}
else 
{
if (currentBuffer == 0)
{
 __return_121442 = 0;
goto label_121445;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_121445 = 0;
label_121445:; 
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
goto label_121388;
}
else 
{
goto label_121365;
}
}
else 
{
label_121365:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_121388;
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
 __return_121407 = 0;
goto label_121408;
}
else 
{
 __return_121408 = -1073741823;
label_121408:; 
}
goto label_121412;
}
else 
{
label_121388:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_121409 = 0;
goto label_121410;
}
else 
{
 __return_121410 = -1073741823;
label_121410:; 
}
label_121412:; 
status5 = ioStatus__Status;
if (status5 < 0)
{
 __return_121440 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_121436;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_121436:; 
 __return_121437 = returnValue;
}
tmp = __return_121437;
goto label_121447;
}
tmp = __return_121440;
goto label_121447;
}
}
}
}
}
else 
{
if (status5 < 0)
{
 __return_121439 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_121434;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_121434:; 
 __return_121438 = returnValue;
}
tmp = __return_121438;
goto label_121447;
}
tmp = __return_121439;
goto label_121447;
}
}
tmp = __return_121445;
label_121447:; 
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
goto label_121602;
}
else 
{
label_121602:; 
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
goto label_121623;
label_121623:; 
 __return_121626 = status7;
}
status4 = __return_121626;
goto label_121948;
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
 __return_121515 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121513 = status1;
}
status7 = __return_121515;
label_121517:; 
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
goto label_121570;
}
else 
{
label_121570:; 
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
goto label_121591;
label_121591:; 
 __return_121594 = status7;
}
status4 = __return_121594;
goto label_121948;
}
}
else 
{
goto label_121537;
}
}
else 
{
status7 = 0;
label_121537:; 
goto label_121539;
}
}
else 
{
status7 = 0;
label_121539:; 
if (currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength > sizeof__CDROM_TOC)
{
bytesTransfered = sizeof__CDROM_TOC;
goto label_121546;
}
else 
{
bytesTransfered = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength;
label_121546:; 
__cil_tmp98 = cdaudioDataOut__LastTrack - cdaudioDataOut__FirstTrack;
tracksOnCd = __cil_tmp98 + 1;
tracksInBuffer = currentIrpStack__Parameters__DeviceIoControl__OutputBufferLength - TrackData__0;
if (tracksInBuffer < tracksOnCd)
{
tracksToReturn = tracksInBuffer;
goto label_121556;
}
else 
{
tracksToReturn = tracksOnCd;
label_121556:; 
if (tracksInBuffer > tracksOnCd)
{
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
goto label_121563;
}
else 
{
label_121563:; 
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
goto label_121941;
label_121941:; 
 __return_121944 = status7;
}
status4 = __return_121944;
goto label_121948;
}
}
}
}
}
status7 = __return_121513;
goto label_121517;
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
 __return_121514 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121512 = status1;
}
status7 = __return_121514;
goto label_121517;
status7 = __return_121512;
goto label_121517;
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
goto label_121711;
}
else 
{
label_121711:; 
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
goto label_121765;
goto label_121765;
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
label_121157:; 
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
 __return_121210 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121208 = status1;
}
status7 = __return_121210;
label_121212:; 
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
goto label_121298;
}
else 
{
label_121298:; 
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
goto label_121319;
label_121319:; 
 __return_121322 = status7;
}
status4 = __return_121322;
goto label_121948;
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
goto label_121709;
}
else 
{
label_121709:; 
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
goto label_121743;
goto label_121743;
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
 __return_121281 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121279 = status1;
}
status7 = __return_121281;
label_121283:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121695;
}
else 
{
label_121695:; 
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
goto label_121743;
label_121743:; 
 __return_121946 = status7;
}
status4 = __return_121946;
label_121948:; 
 __return_125434 = status4;
}
status10 = __return_125434;
goto label_125439;
status7 = __return_121279;
goto label_121283;
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
 __return_121280 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121278 = status1;
}
status7 = __return_121280;
goto label_121283;
status7 = __return_121278;
goto label_121283;
}
}
}
}
status7 = __return_121208;
goto label_121212;
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
 __return_121209 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121207 = status1;
}
status7 = __return_121209;
goto label_121212;
status7 = __return_121207;
goto label_121212;
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
goto label_121157;
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
goto label_121644;
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
 __return_121129 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121127 = status1;
}
status7 = __return_121129;
label_121131:; 
if (status7 < 0)
{
__cil_tmp105 = (unsigned long)status7;
if (__cil_tmp105 == -1073741808)
{
status7 = -1073741803;
goto label_121147;
}
else 
{
goto label_121147;
}
}
else 
{
label_121147:; 
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
goto label_121765;
goto label_121765;
}
}
}
status7 = __return_121127;
goto label_121131;
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
 __return_121128 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_121126 = status1;
}
status7 = __return_121128;
goto label_121131;
status7 = __return_121126;
goto label_121131;
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
goto label_121047;
}
else 
{
label_121047:; 
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
goto label_121068;
label_121068:; 
 __return_121071 = status7;
}
status4 = __return_121071;
goto label_121948;
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
goto label_121016;
}
else 
{
label_121016:; 
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
goto label_121037;
label_121037:; 
 __return_121040 = status7;
}
status4 = __return_121040;
goto label_121948;
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
 __return_120875 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120873 = status1;
}
status7 = __return_120875;
label_120877:; 
if (status7 < 0)
{
__cil_tmp109 = (unsigned long)status7;
if (__cil_tmp109 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_120985;
}
else 
{
label_120985:; 
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
goto label_121006;
label_121006:; 
 __return_121009 = status7;
}
status4 = __return_121009;
goto label_121948;
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
 __return_120938 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120936 = status1;
}
status7 = __return_120938;
label_120940:; 
if (status7 < 0)
{
__cil_tmp111 = (unsigned long)status7;
if (__cil_tmp111 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_120955;
}
else 
{
label_120955:; 
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
goto label_120976;
label_120976:; 
 __return_120979 = status7;
}
status4 = __return_120979;
goto label_121948;
}
}
else 
{
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
goto label_121765;
goto label_121765;
}
}
}
status7 = __return_120936;
goto label_120940;
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
 __return_120937 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120935 = status1;
}
status7 = __return_120937;
goto label_120940;
status7 = __return_120935;
goto label_120940;
}
}
}
status7 = __return_120873;
goto label_120877;
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
 __return_120874 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120872 = status1;
}
status7 = __return_120874;
goto label_120877;
status7 = __return_120872;
goto label_120877;
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
goto label_120791;
}
else 
{
label_120791:; 
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
goto label_120812;
label_120812:; 
 __return_120815 = status7;
}
status4 = __return_120815;
goto label_121948;
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
 __return_120769 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120767 = status1;
}
status7 = __return_120769;
label_120771:; 
if (status7 >= 0)
{
deviceExtension__PlayActive = 1;
deviceExtension__Paused = 0;
goto label_120783;
}
else 
{
label_120783:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121703;
}
else 
{
label_121703:; 
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
goto label_121765;
goto label_121765;
}
}
}
status7 = __return_120767;
goto label_120771;
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
 __return_120768 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120766 = status1;
}
status7 = __return_120768;
goto label_120771;
status7 = __return_120766;
goto label_120771;
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
goto label_121644;
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
goto label_120684;
}
else 
{
label_120684:; 
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
goto label_120705;
label_120705:; 
 __return_120708 = status7;
}
status4 = __return_120708;
goto label_121948;
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
goto label_120652;
}
else 
{
label_120652:; 
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
goto label_120673;
label_120673:; 
 __return_120676 = status7;
}
status4 = __return_120676;
goto label_121948;
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
 __return_120623 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120621 = status1;
}
status7 = __return_120623;
label_120625:; 
if (status7 >= 0)
{
if (deviceExtension__Paused == 1)
{
deviceExtension__PlayActive = 0;
goto label_120640;
}
else 
{
label_120640:; 
Irp__IoStatus__Information = sizeof__SUB_Q_CURRENT_POSITION;
goto label_120643;
}
}
else 
{
Irp__IoStatus__Information = 0;
label_120643:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121701;
}
else 
{
label_121701:; 
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
goto label_121765;
goto label_121765;
}
}
}
status7 = __return_120621;
goto label_120625;
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
 __return_120622 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120620 = status1;
}
status7 = __return_120622;
goto label_120625;
status7 = __return_120620;
goto label_120625;
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
 __return_120553 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120551 = status1;
}
status7 = __return_120553;
label_120555:; 
__cil_tmp116 = (unsigned long)status7;
if (__cil_tmp116 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_121699;
}
else 
{
label_121699:; 
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
goto label_121765;
goto label_121765;
}
}
status7 = __return_120551;
goto label_120555;
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
 __return_120552 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_120550 = status1;
}
status7 = __return_120552;
goto label_120555;
status7 = __return_120550;
goto label_120555;
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
label_120492:; 
Irp__IoStatus__Information = 0;
label_120495:; 
status7 = -1073741808;
goto label_121644;
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
goto label_120492;
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
goto label_120495;
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
 __return_119097 = 0;
goto label_119098;
}
else 
{
if (currentBuffer == 0)
{
 __return_119095 = 0;
goto label_119098;
}
else 
{
if (irp_CdAudioIsPlayActive == 0)
{
 __return_119098 = 0;
label_119098:; 
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
goto label_119041;
}
else 
{
goto label_119018;
}
}
else 
{
label_119018:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_119041;
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
 __return_119060 = 0;
goto label_119061;
}
else 
{
 __return_119061 = -1073741823;
label_119061:; 
}
goto label_119065;
}
else 
{
label_119041:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_119062 = 0;
goto label_119063;
}
else 
{
 __return_119063 = -1073741823;
label_119063:; 
}
label_119065:; 
status5 = ioStatus__Status;
if (status5 < 0)
{
 __return_119093 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_119089;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_119089:; 
 __return_119090 = returnValue;
}
tmp___1 = __return_119090;
goto label_119102;
}
tmp___1 = __return_119093;
label_119102:; 
if (tmp___1 == 1)
{
deviceExtension__PlayActive = 1;
status7 = 0;
Irp__IoStatus__Information = 0;
__cil_tmp115 = (unsigned long)status7;
if (__cil_tmp115 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_120437;
}
else 
{
label_120437:; 
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
goto label_120459;
goto label_120459;
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
goto label_119401;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119401;
}
else 
{
returnVal2 = 259;
label_119401:; 
goto label_119413;
}
}
}
else 
{
returnVal2 = 259;
label_119413:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119542;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119518;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119518:; 
goto label_119542;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119542:; 
 __return_119552 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_119451:; 
 __return_119553 = returnVal2;
}
tmp = __return_119552;
goto label_119555;
tmp = __return_119553;
label_119555:; 
 __return_119978 = tmp;
}
tmp___0 = __return_119978;
label_119980:; 
 __return_120419 = tmp___0;
}
status4 = __return_120419;
goto label_121948;
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
goto label_119205;
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
goto label_119221;
}
else 
{
label_119221:; 
}
label_119224:; 
 __return_119241 = myStatus;
}
compRetStatus = __return_119241;
goto label_119245;
}
else 
{
 __return_119242 = myStatus;
}
compRetStatus = __return_119242;
goto label_119245;
}
}
else 
{
label_119205:; 
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
goto label_119236;
}
else 
{
label_119236:; 
}
goto label_119224;
}
}
else 
{
 __return_119243 = myStatus;
}
compRetStatus = __return_119243;
label_119245:; 
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
goto label_119278;
label_119278:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_119407;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119407;
}
else 
{
returnVal2 = 259;
label_119407:; 
goto label_119419;
}
}
}
else 
{
returnVal2 = 259;
label_119419:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119548;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119524;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119524:; 
goto label_119548;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119548:; 
 __return_119549 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119451;
}
tmp = __return_119549;
goto label_119555;
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
goto label_119403;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119403;
}
else 
{
returnVal2 = 259;
label_119403:; 
goto label_119415;
}
}
}
else 
{
returnVal2 = 259;
label_119415:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119544;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119520;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119520:; 
goto label_119544;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119544:; 
 __return_119551 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119451;
}
tmp = __return_119551;
goto label_119555;
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
 __return_119176 = l;
}
 __return_119179 = -1073741802;
}
compRetStatus = __return_119179;
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
goto label_119278;
goto label_119278;
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
goto label_119405;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119405;
}
else 
{
returnVal2 = 259;
label_119405:; 
goto label_119417;
}
}
}
else 
{
returnVal2 = 259;
label_119417:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119546;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119522;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119522:; 
goto label_119546;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119546:; 
 __return_119550 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119451;
}
tmp = __return_119550;
goto label_119555;
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
goto label_119815;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119815;
}
else 
{
returnVal2 = 259;
label_119815:; 
goto label_119827;
}
}
}
else 
{
returnVal2 = 259;
label_119827:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119956;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119932;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119932:; 
goto label_119956;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119956:; 
 __return_119966 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_119865:; 
 __return_119967 = returnVal2;
}
tmp = __return_119966;
goto label_119555;
tmp = __return_119967;
goto label_119555;
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
goto label_119619;
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
goto label_119635;
}
else 
{
label_119635:; 
}
label_119638:; 
 __return_119655 = myStatus;
}
compRetStatus = __return_119655;
goto label_119659;
}
else 
{
 __return_119656 = myStatus;
}
compRetStatus = __return_119656;
goto label_119659;
}
}
else 
{
label_119619:; 
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
goto label_119650;
}
else 
{
label_119650:; 
}
goto label_119638;
}
}
else 
{
 __return_119657 = myStatus;
}
compRetStatus = __return_119657;
label_119659:; 
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
goto label_119692;
label_119692:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_119821;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119821;
}
else 
{
returnVal2 = 259;
label_119821:; 
goto label_119833;
}
}
}
else 
{
returnVal2 = 259;
label_119833:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119962;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119938;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119938:; 
goto label_119962;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119962:; 
 __return_119963 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119865;
}
tmp = __return_119963;
goto label_119555;
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
goto label_119817;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119817;
}
else 
{
returnVal2 = 259;
label_119817:; 
goto label_119829;
}
}
}
else 
{
returnVal2 = 259;
label_119829:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119958;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119934;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119934:; 
goto label_119958;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119958:; 
 __return_119965 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119865;
}
tmp = __return_119965;
goto label_119555;
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
 __return_119590 = l;
}
 __return_119593 = -1073741802;
}
compRetStatus = __return_119593;
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
goto label_119692;
goto label_119692;
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
goto label_119819;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_119819;
}
else 
{
returnVal2 = 259;
label_119819:; 
goto label_119831;
}
}
}
else 
{
returnVal2 = 259;
label_119831:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_119960;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_119936;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_119936:; 
goto label_119960;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_119960:; 
 __return_119964 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_119865;
}
tmp = __return_119964;
goto label_119555;
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
 __return_119092 = 0;
}
else 
{
if (currentBuffer__Header__AudioStatus == 17)
{
returnValue = 1;
goto label_119087;
}
else 
{
returnValue = 0;
deviceExtension__PlayActive = 0;
label_119087:; 
 __return_119091 = returnValue;
}
tmp___1 = __return_119091;
goto label_119100;
}
tmp___1 = __return_119092;
goto label_119100;
}
}
tmp___1 = __return_119098;
label_119100:; 
if (tmp___1 == 1)
{
deviceExtension__PlayActive = 1;
status7 = 0;
Irp__IoStatus__Information = 0;
__cil_tmp115 = (unsigned long)status7;
if (__cil_tmp115 == -2147483626)
{
Irp__IoStatus__Information = 0;
goto label_120435;
}
else 
{
label_120435:; 
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
goto label_120459;
label_120459:; 
 __return_120484 = status7;
}
status4 = __return_120484;
goto label_121948;
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
goto label_120253;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_120253;
}
else 
{
returnVal2 = 259;
label_120253:; 
goto label_120265;
}
}
}
else 
{
returnVal2 = 259;
label_120265:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_120394;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_120370;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_120370:; 
goto label_120394;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_120394:; 
 __return_120404 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_120303:; 
 __return_120405 = returnVal2;
}
tmp = __return_120404;
goto label_120407;
tmp = __return_120405;
label_120407:; 
 __return_120416 = tmp;
}
tmp___0 = __return_120416;
goto label_119980;
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
goto label_120057;
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
goto label_120073;
}
else 
{
label_120073:; 
}
label_120076:; 
 __return_120093 = myStatus;
}
compRetStatus = __return_120093;
goto label_120097;
}
else 
{
 __return_120094 = myStatus;
}
compRetStatus = __return_120094;
goto label_120097;
}
}
else 
{
label_120057:; 
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
goto label_120088;
}
else 
{
label_120088:; 
}
goto label_120076;
}
}
else 
{
 __return_120095 = myStatus;
}
compRetStatus = __return_120095;
label_120097:; 
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
goto label_120130;
label_120130:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_120259;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_120259;
}
else 
{
returnVal2 = 259;
label_120259:; 
goto label_120271;
}
}
}
else 
{
returnVal2 = 259;
label_120271:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_120400;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_120376;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_120376:; 
goto label_120400;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_120400:; 
 __return_120401 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_120303;
}
tmp = __return_120401;
goto label_120407;
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
goto label_120255;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_120255;
}
else 
{
returnVal2 = 259;
label_120255:; 
goto label_120267;
}
}
}
else 
{
returnVal2 = 259;
label_120267:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_120396;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_120372;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_120372:; 
goto label_120396;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_120396:; 
 __return_120403 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_120303;
}
tmp = __return_120403;
goto label_120407;
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
 __return_120028 = l;
}
 __return_120031 = -1073741802;
}
compRetStatus = __return_120031;
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
goto label_120130;
goto label_120130;
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
goto label_120257;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_120257;
}
else 
{
returnVal2 = 259;
label_120257:; 
goto label_120269;
}
}
}
else 
{
returnVal2 = 259;
label_120269:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_120398;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_120374;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_120374:; 
goto label_120398;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_120398:; 
 __return_120402 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_120303;
}
tmp = __return_120402;
goto label_120407;
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
goto label_118811;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118811;
}
else 
{
returnVal2 = 259;
label_118811:; 
goto label_118823;
}
}
}
else 
{
returnVal2 = 259;
label_118823:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118952;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118928;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118928:; 
goto label_118952;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118952:; 
 __return_118962 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_118861:; 
 __return_118963 = returnVal2;
}
tmp = __return_118962;
goto label_118965;
tmp = __return_118963;
label_118965:; 
 __return_118974 = tmp;
}
tmp___2 = __return_118974;
 __return_118977 = tmp___2;
}
status4 = __return_118977;
goto label_121948;
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
goto label_118615;
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
goto label_118631;
}
else 
{
label_118631:; 
}
label_118634:; 
 __return_118651 = myStatus;
}
compRetStatus = __return_118651;
goto label_118655;
}
else 
{
 __return_118652 = myStatus;
}
compRetStatus = __return_118652;
goto label_118655;
}
}
else 
{
label_118615:; 
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
goto label_118646;
}
else 
{
label_118646:; 
}
goto label_118634;
}
}
else 
{
 __return_118653 = myStatus;
}
compRetStatus = __return_118653;
label_118655:; 
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
goto label_118688;
label_118688:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_118817;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118817;
}
else 
{
returnVal2 = 259;
label_118817:; 
goto label_118829;
}
}
}
else 
{
returnVal2 = 259;
label_118829:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118958;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118934;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118934:; 
goto label_118958;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118958:; 
 __return_118959 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118861;
}
tmp = __return_118959;
goto label_118965;
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
goto label_118813;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118813;
}
else 
{
returnVal2 = 259;
label_118813:; 
goto label_118825;
}
}
}
else 
{
returnVal2 = 259;
label_118825:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118954;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118930;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118930:; 
goto label_118954;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118954:; 
 __return_118961 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118861;
}
tmp = __return_118961;
goto label_118965;
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
 __return_118586 = l;
}
 __return_118589 = -1073741802;
}
compRetStatus = __return_118589;
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
goto label_118688;
goto label_118688;
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
goto label_118815;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118815;
}
else 
{
returnVal2 = 259;
label_118815:; 
goto label_118827;
}
}
}
else 
{
returnVal2 = 259;
label_118827:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118956;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118932;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118932:; 
goto label_118956;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118956:; 
 __return_118960 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118861;
}
tmp = __return_118960;
goto label_118965;
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
 __return_118248 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_118246 = status1;
}
status8 = __return_118248;
label_118250:; 
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
goto label_118306;
label_118306:; 
 __return_118309 = status8;
}
status4 = __return_118309;
label_118311:; 
 __return_125435 = status4;
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
goto label_118281;
label_118281:; 
 __return_118284 = status8;
}
status4 = __return_118284;
goto label_118311;
}
status10 = __return_125435;
goto label_125439;
status8 = __return_118246;
goto label_118250;
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
 __return_118247 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_118245 = status1;
}
status8 = __return_118247;
goto label_118250;
status8 = __return_118245;
goto label_118250;
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
goto label_118027;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118027;
}
else 
{
returnVal2 = 259;
label_118027:; 
goto label_118039;
}
}
}
else 
{
returnVal2 = 259;
label_118039:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118168;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118144;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118144:; 
goto label_118168;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118168:; 
 __return_118178 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_118077:; 
 __return_118179 = returnVal2;
}
tmp = __return_118178;
goto label_118181;
tmp = __return_118179;
label_118181:; 
 __return_118190 = tmp;
}
tmp = __return_118190;
 __return_118193 = tmp;
}
status4 = __return_118193;
goto label_118311;
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
goto label_117831;
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
goto label_117847;
}
else 
{
label_117847:; 
}
label_117850:; 
 __return_117867 = myStatus;
}
compRetStatus = __return_117867;
goto label_117871;
}
else 
{
 __return_117868 = myStatus;
}
compRetStatus = __return_117868;
goto label_117871;
}
}
else 
{
label_117831:; 
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
goto label_117862;
}
else 
{
label_117862:; 
}
goto label_117850;
}
}
else 
{
 __return_117869 = myStatus;
}
compRetStatus = __return_117869;
label_117871:; 
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
goto label_117904;
label_117904:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_118033;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118033;
}
else 
{
returnVal2 = 259;
label_118033:; 
goto label_118045;
}
}
}
else 
{
returnVal2 = 259;
label_118045:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118174;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118150;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118150:; 
goto label_118174;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118174:; 
 __return_118175 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118077;
}
tmp = __return_118175;
goto label_118181;
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
goto label_118029;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118029;
}
else 
{
returnVal2 = 259;
label_118029:; 
goto label_118041;
}
}
}
else 
{
returnVal2 = 259;
label_118041:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118170;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118146;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118146:; 
goto label_118170;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118170:; 
 __return_118177 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118077;
}
tmp = __return_118177;
goto label_118181;
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
 __return_117802 = l;
}
 __return_117805 = -1073741802;
}
compRetStatus = __return_117805;
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
goto label_117904;
goto label_117904;
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
goto label_118031;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_118031;
}
else 
{
returnVal2 = 259;
label_118031:; 
goto label_118043;
}
}
}
else 
{
returnVal2 = 259;
label_118043:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_118172;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_118148;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_118148:; 
goto label_118172;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_118172:; 
 __return_118176 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_118077;
}
tmp = __return_118176;
goto label_118181;
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
goto label_117555;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117555;
}
else 
{
returnVal2 = 259;
label_117555:; 
goto label_117567;
}
}
}
else 
{
returnVal2 = 259;
label_117567:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117696;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117672;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117672:; 
goto label_117696;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117696:; 
 __return_117706 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_117605:; 
 __return_117707 = returnVal2;
}
tmp = __return_117706;
goto label_117295;
tmp = __return_117707;
goto label_117295;
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
goto label_117359;
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
goto label_117375;
}
else 
{
label_117375:; 
}
label_117378:; 
 __return_117395 = myStatus;
}
compRetStatus = __return_117395;
goto label_117399;
}
else 
{
 __return_117396 = myStatus;
}
compRetStatus = __return_117396;
goto label_117399;
}
}
else 
{
label_117359:; 
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
goto label_117390;
}
else 
{
label_117390:; 
}
goto label_117378;
}
}
else 
{
 __return_117397 = myStatus;
}
compRetStatus = __return_117397;
label_117399:; 
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
goto label_117432;
label_117432:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_117561;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117561;
}
else 
{
returnVal2 = 259;
label_117561:; 
goto label_117573;
}
}
}
else 
{
returnVal2 = 259;
label_117573:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117702;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117678;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117678:; 
goto label_117702;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117702:; 
 __return_117703 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117605;
}
tmp = __return_117703;
goto label_117295;
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
goto label_117557;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117557;
}
else 
{
returnVal2 = 259;
label_117557:; 
goto label_117569;
}
}
}
else 
{
returnVal2 = 259;
label_117569:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117698;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117674;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117674:; 
goto label_117698;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117698:; 
 __return_117705 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117605;
}
tmp = __return_117705;
goto label_117295;
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
 __return_117330 = l;
}
 __return_117333 = -1073741802;
}
compRetStatus = __return_117333;
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
goto label_117432;
goto label_117432;
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
goto label_117559;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117559;
}
else 
{
returnVal2 = 259;
label_117559:; 
goto label_117571;
}
}
}
else 
{
returnVal2 = 259;
label_117571:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117700;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117676;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117676:; 
goto label_117700;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117700:; 
 __return_117704 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117605;
}
tmp = __return_117704;
goto label_117295;
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
goto label_117141;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117141;
}
else 
{
returnVal2 = 259;
label_117141:; 
goto label_117153;
}
}
}
else 
{
returnVal2 = 259;
label_117153:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117282;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117258;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117258:; 
goto label_117282;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117282:; 
 __return_117292 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_117191:; 
 __return_117293 = returnVal2;
}
tmp = __return_117292;
goto label_117295;
tmp = __return_117293;
label_117295:; 
 __return_117718 = tmp;
}
status4 = __return_117718;
label_117720:; 
 __return_125436 = status4;
}
status10 = __return_125436;
goto label_125439;
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
goto label_116945;
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
goto label_116961;
}
else 
{
label_116961:; 
}
label_116964:; 
 __return_116981 = myStatus;
}
compRetStatus = __return_116981;
goto label_116985;
}
else 
{
 __return_116982 = myStatus;
}
compRetStatus = __return_116982;
goto label_116985;
}
}
else 
{
label_116945:; 
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
goto label_116976;
}
else 
{
label_116976:; 
}
goto label_116964;
}
}
else 
{
 __return_116983 = myStatus;
}
compRetStatus = __return_116983;
label_116985:; 
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
goto label_117018;
label_117018:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_117147;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117147;
}
else 
{
returnVal2 = 259;
label_117147:; 
goto label_117159;
}
}
}
else 
{
returnVal2 = 259;
label_117159:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117288;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117264;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117264:; 
goto label_117288;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117288:; 
 __return_117289 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117191;
}
tmp = __return_117289;
goto label_117295;
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
goto label_117143;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117143;
}
else 
{
returnVal2 = 259;
label_117143:; 
goto label_117155;
}
}
}
else 
{
returnVal2 = 259;
label_117155:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117284;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117260;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117260:; 
goto label_117284;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117284:; 
 __return_117291 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117191;
}
tmp = __return_117291;
goto label_117295;
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
 __return_116916 = l;
}
 __return_116919 = -1073741802;
}
compRetStatus = __return_116919;
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
goto label_117018;
goto label_117018;
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
goto label_117145;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_117145;
}
else 
{
returnVal2 = 259;
label_117145:; 
goto label_117157;
}
}
}
else 
{
returnVal2 = 259;
label_117157:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_117286;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_117262;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_117262:; 
goto label_117286;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_117286:; 
 __return_117290 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_117191;
}
tmp = __return_117290;
goto label_117295;
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
goto label_116704;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116704;
}
else 
{
returnVal2 = 259;
label_116704:; 
goto label_116716;
}
}
}
else 
{
returnVal2 = 259;
label_116716:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116845;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116821;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116821:; 
goto label_116845;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116845:; 
 __return_116855 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_116754:; 
 __return_116856 = returnVal2;
}
tmp = __return_116855;
goto label_116858;
tmp = __return_116856;
label_116858:; 
 __return_116867 = tmp;
}
tmp___0 = __return_116867;
 __return_116870 = tmp___0;
}
status4 = __return_116870;
goto label_117720;
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
goto label_116508;
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
goto label_116524;
}
else 
{
label_116524:; 
}
label_116527:; 
 __return_116544 = myStatus;
}
compRetStatus = __return_116544;
goto label_116548;
}
else 
{
 __return_116545 = myStatus;
}
compRetStatus = __return_116545;
goto label_116548;
}
}
else 
{
label_116508:; 
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
goto label_116539;
}
else 
{
label_116539:; 
}
goto label_116527;
}
}
else 
{
 __return_116546 = myStatus;
}
compRetStatus = __return_116546;
label_116548:; 
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
goto label_116581;
label_116581:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_116710;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116710;
}
else 
{
returnVal2 = 259;
label_116710:; 
goto label_116722;
}
}
}
else 
{
returnVal2 = 259;
label_116722:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116851;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116827;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116827:; 
goto label_116851;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116851:; 
 __return_116852 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116754;
}
tmp = __return_116852;
goto label_116858;
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
goto label_116706;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116706;
}
else 
{
returnVal2 = 259;
label_116706:; 
goto label_116718;
}
}
}
else 
{
returnVal2 = 259;
label_116718:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116847;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116823;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116823:; 
goto label_116847;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116847:; 
 __return_116854 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116754;
}
tmp = __return_116854;
goto label_116858;
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
 __return_116479 = l;
}
 __return_116482 = -1073741802;
}
compRetStatus = __return_116482;
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
goto label_116581;
goto label_116581;
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
goto label_116708;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116708;
}
else 
{
returnVal2 = 259;
label_116708:; 
goto label_116720;
}
}
}
else 
{
returnVal2 = 259;
label_116720:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116849;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116825;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116825:; 
goto label_116849;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116849:; 
 __return_116853 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116754;
}
tmp = __return_116853;
goto label_116858;
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
goto label_116241;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116241;
}
else 
{
returnVal2 = 259;
label_116241:; 
goto label_116253;
}
}
}
else 
{
returnVal2 = 259;
label_116253:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116382;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116358;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116358:; 
goto label_116382;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116382:; 
 __return_116392 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_116291:; 
 __return_116393 = returnVal2;
}
tmp = __return_116392;
goto label_116395;
tmp = __return_116393;
label_116395:; 
 __return_116404 = tmp;
}
status4 = __return_116404;
 __return_125437 = status4;
}
status10 = __return_125437;
label_125439:; 
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_125863;
}
else 
{
goto label_125509;
}
}
else 
{
label_125509:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_125863;
}
else 
{
goto label_125552;
}
}
else 
{
label_125552:; 
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
goto label_125796;
}
else 
{
goto label_125642;
}
}
else 
{
goto label_125642;
}
}
else 
{
label_125642:; 
if (pended != 1)
{
if (s == DC)
{
if (status10 == 259)
{
{
__VERIFIER_error();
}
goto label_125752;
}
else 
{
goto label_125863;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_125700;
}
else 
{
goto label_125863;
}
}
}
else 
{
goto label_125863;
}
}
}
else 
{
goto label_125863;
}
}
else 
{
label_125863:; 
 __return_125881 = status10;
goto label_111365;
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
goto label_125466;
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
goto label_116045;
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
goto label_116061;
}
else 
{
label_116061:; 
}
label_116064:; 
 __return_116081 = myStatus;
}
compRetStatus = __return_116081;
goto label_116085;
}
else 
{
 __return_116082 = myStatus;
}
compRetStatus = __return_116082;
goto label_116085;
}
}
else 
{
label_116045:; 
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
goto label_116076;
}
else 
{
label_116076:; 
}
goto label_116064;
}
}
else 
{
 __return_116083 = myStatus;
}
compRetStatus = __return_116083;
label_116085:; 
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
goto label_116118;
label_116118:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_116247;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116247;
}
else 
{
returnVal2 = 259;
label_116247:; 
goto label_116259;
}
}
}
else 
{
returnVal2 = 259;
label_116259:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116388;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116364;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116364:; 
goto label_116388;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116388:; 
 __return_116389 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116291;
}
tmp = __return_116389;
goto label_116395;
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
goto label_116243;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116243;
}
else 
{
returnVal2 = 259;
label_116243:; 
goto label_116255;
}
}
}
else 
{
returnVal2 = 259;
label_116255:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116384;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116360;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116360:; 
goto label_116384;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116384:; 
 __return_116391 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116291;
}
tmp = __return_116391;
goto label_116395;
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
 __return_116016 = l;
}
 __return_116019 = -1073741802;
}
compRetStatus = __return_116019;
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
goto label_116118;
goto label_116118;
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
goto label_116245;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_116245;
}
else 
{
returnVal2 = 259;
label_116245:; 
goto label_116257;
}
}
}
else 
{
returnVal2 = 259;
label_116257:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_116386;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_116362;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_116362:; 
goto label_116386;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_116386:; 
 __return_116390 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_116291;
}
tmp = __return_116390;
goto label_116395;
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
goto label_115523;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115523;
}
else 
{
returnVal2 = 259;
label_115523:; 
goto label_115535;
}
}
}
else 
{
returnVal2 = 259;
label_115535:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115664;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115640;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115640:; 
goto label_115664;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115664:; 
 __return_115674 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_115573:; 
 __return_115675 = returnVal2;
}
status9 = __return_115674;
goto label_114849;
status9 = __return_115675;
goto label_114849;
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
goto label_115327;
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
goto label_115343;
}
else 
{
label_115343:; 
}
label_115346:; 
 __return_115363 = myStatus;
}
compRetStatus = __return_115363;
goto label_115367;
}
else 
{
 __return_115364 = myStatus;
}
compRetStatus = __return_115364;
goto label_115367;
}
}
else 
{
label_115327:; 
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
goto label_115358;
}
else 
{
label_115358:; 
}
goto label_115346;
}
}
else 
{
 __return_115365 = myStatus;
}
compRetStatus = __return_115365;
label_115367:; 
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
goto label_115400;
label_115400:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_115529;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115529;
}
else 
{
returnVal2 = 259;
label_115529:; 
goto label_115541;
}
}
}
else 
{
returnVal2 = 259;
label_115541:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115670;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115646;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115646:; 
goto label_115670;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115670:; 
 __return_115671 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115573;
}
status9 = __return_115671;
goto label_114849;
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
goto label_115525;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115525;
}
else 
{
returnVal2 = 259;
label_115525:; 
goto label_115537;
}
}
}
else 
{
returnVal2 = 259;
label_115537:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115666;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115642;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115642:; 
goto label_115666;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115666:; 
 __return_115673 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115573;
}
status9 = __return_115673;
goto label_114849;
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
 __return_115298 = l;
}
 __return_115301 = -1073741802;
}
compRetStatus = __return_115301;
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
goto label_115400;
goto label_115400;
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
goto label_115527;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115527;
}
else 
{
returnVal2 = 259;
label_115527:; 
goto label_115539;
}
}
}
else 
{
returnVal2 = 259;
label_115539:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115668;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115644;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115644:; 
goto label_115668;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115668:; 
 __return_115672 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115573;
}
status9 = __return_115672;
goto label_114849;
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
goto label_115109;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115109;
}
else 
{
returnVal2 = 259;
label_115109:; 
goto label_115121;
}
}
}
else 
{
returnVal2 = 259;
label_115121:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115250;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115226;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115226:; 
goto label_115250;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115250:; 
 __return_115260 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_115159:; 
 __return_115261 = returnVal2;
}
status9 = __return_115260;
goto label_114849;
status9 = __return_115261;
goto label_114849;
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
goto label_114913;
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
goto label_114929;
}
else 
{
label_114929:; 
}
label_114932:; 
 __return_114949 = myStatus;
}
compRetStatus = __return_114949;
goto label_114953;
}
else 
{
 __return_114950 = myStatus;
}
compRetStatus = __return_114950;
goto label_114953;
}
}
else 
{
label_114913:; 
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
goto label_114944;
}
else 
{
label_114944:; 
}
goto label_114932;
}
}
else 
{
 __return_114951 = myStatus;
}
compRetStatus = __return_114951;
label_114953:; 
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
goto label_114986;
label_114986:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_115115;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115115;
}
else 
{
returnVal2 = 259;
label_115115:; 
goto label_115127;
}
}
}
else 
{
returnVal2 = 259;
label_115127:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115256;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115232;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115232:; 
goto label_115256;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115256:; 
 __return_115257 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115159;
}
status9 = __return_115257;
goto label_114849;
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
goto label_115111;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115111;
}
else 
{
returnVal2 = 259;
label_115111:; 
goto label_115123;
}
}
}
else 
{
returnVal2 = 259;
label_115123:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115252;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115228;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115228:; 
goto label_115252;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115252:; 
 __return_115259 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115159;
}
status9 = __return_115259;
goto label_114849;
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
 __return_114884 = l;
}
 __return_114887 = -1073741802;
}
compRetStatus = __return_114887;
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
goto label_114986;
goto label_114986;
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
goto label_115113;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_115113;
}
else 
{
returnVal2 = 259;
label_115113:; 
goto label_115125;
}
}
}
else 
{
returnVal2 = 259;
label_115125:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_115254;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_115230;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_115230:; 
goto label_115254;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_115254:; 
 __return_115258 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_115159;
}
status9 = __return_115258;
goto label_114849;
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
goto label_114695;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114695;
}
else 
{
returnVal2 = 259;
label_114695:; 
goto label_114707;
}
}
}
else 
{
returnVal2 = 259;
label_114707:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114836;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114812;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114812:; 
goto label_114836;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114836:; 
 __return_114846 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_114745:; 
 __return_114847 = returnVal2;
}
status9 = __return_114846;
goto label_114849;
status9 = __return_114847;
label_114849:; 
status9 = 259;
if (status9 == 0)
{
 __return_115751 = status9;
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
goto label_115721;
}
else 
{
goto label_115698;
}
}
else 
{
label_115698:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_115721;
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
 __return_115740 = 0;
goto label_115741;
}
else 
{
 __return_115741 = -1073741823;
label_115741:; 
}
goto label_115745;
}
else 
{
label_115721:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_115742 = 0;
goto label_115743;
}
else 
{
 __return_115743 = -1073741823;
label_115743:; 
}
label_115745:; 
status9 = myStatus;
 __return_115750 = status9;
}
status2 = __return_115750;
goto label_115753;
}
}
}
}
status2 = __return_115751;
label_115753:; 
if (status2 < 0)
{
 __return_115901 = status2;
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
 __return_115900 = 0;
}
else 
{
status2 = -1073741823;
label_115767:; 
if (status2 < 0)
{
tmp = attempt;
int __CPAchecker_TMP_0 = attempt;
attempt = attempt + 1;
__CPAchecker_TMP_0;
if (tmp >= 4)
{
goto label_115842;
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
 __return_115830 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_115828 = status1;
}
status2 = __return_115830;
label_115832:; 
goto label_115767;
status2 = __return_115828;
goto label_115832;
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
 __return_115829 = -1073741670;
}
else 
{
__cil_tmp18 = (long)status1;
 __return_115827 = status1;
}
status2 = __return_115829;
goto label_115832;
status2 = __return_115827;
goto label_115832;
}
}
}
}
else 
{
label_115842:; 
if (status2 < 0)
{
deviceExtension__Active = 0;
 __return_115898 = 0;
}
else 
{
deviceExtension__Active = 0;
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_115895 = 0;
}
else 
{
if (status2 < 0)
{
goto label_115860;
}
else 
{
label_115860:; 
{
int __tmp_460 = deviceParameterHandle;
int Handle = __tmp_460;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_115873 = 0;
goto label_115874;
}
else 
{
 __return_115874 = -1073741823;
label_115874:; 
}
 __return_115894 = 0;
}
status3 = __return_115894;
goto label_115903;
}
}
status3 = __return_115895;
goto label_115903;
}
status3 = __return_115898;
goto label_115903;
}
}
status3 = __return_115900;
goto label_115903;
}
else 
{
keyValue = deviceExtension__Active;
if (status2 < 0)
{
 __return_115896 = 0;
}
else 
{
if (status2 < 0)
{
goto label_115858;
}
else 
{
label_115858:; 
{
int __tmp_461 = deviceParameterHandle;
int Handle = __tmp_461;
int tmp_ndt_10;
tmp_ndt_10 = __VERIFIER_nondet_int();
if (tmp_ndt_10 == 0)
{
 __return_115889 = 0;
goto label_115890;
}
else 
{
 __return_115890 = -1073741823;
label_115890:; 
}
 __return_115893 = 0;
}
status3 = __return_115893;
goto label_115903;
}
}
status3 = __return_115896;
goto label_115903;
}
}
status3 = __return_115901;
label_115903:; 
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
goto label_115937;
label_115937:; 
 __return_115940 = status3;
}
status10 = __return_115940;
label_115942:; 
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_125865;
}
else 
{
goto label_125511;
}
}
else 
{
label_125511:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_125865;
}
else 
{
goto label_125550;
}
}
else 
{
label_125550:; 
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
goto label_125796;
}
else 
{
goto label_125640;
}
}
else 
{
goto label_125640;
}
}
else 
{
label_125640:; 
if (pended != 1)
{
if (s == DC)
{
if (status10 == 259)
{
{
__VERIFIER_error();
}
goto label_125752;
}
else 
{
goto label_125865;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_125700;
}
else 
{
goto label_125865;
}
}
}
else 
{
goto label_125865;
}
}
}
else 
{
goto label_125865;
}
}
else 
{
label_125865:; 
 __return_125879 = status10;
goto label_111365;
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
goto label_125466;
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
goto label_114499;
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
goto label_114515;
}
else 
{
label_114515:; 
}
label_114518:; 
 __return_114535 = myStatus;
}
compRetStatus = __return_114535;
goto label_114539;
}
else 
{
 __return_114536 = myStatus;
}
compRetStatus = __return_114536;
goto label_114539;
}
}
else 
{
label_114499:; 
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
goto label_114530;
}
else 
{
label_114530:; 
}
goto label_114518;
}
}
else 
{
 __return_114537 = myStatus;
}
compRetStatus = __return_114537;
label_114539:; 
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
goto label_114572;
label_114572:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_114701;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114701;
}
else 
{
returnVal2 = 259;
label_114701:; 
goto label_114713;
}
}
}
else 
{
returnVal2 = 259;
label_114713:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114842;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114818;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114818:; 
goto label_114842;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114842:; 
 __return_114843 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114745;
}
status9 = __return_114843;
goto label_114849;
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
goto label_114697;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114697;
}
else 
{
returnVal2 = 259;
label_114697:; 
goto label_114709;
}
}
}
else 
{
returnVal2 = 259;
label_114709:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114838;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114814;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114814:; 
goto label_114838;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114838:; 
 __return_114845 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114745;
}
status9 = __return_114845;
goto label_114849;
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
 __return_114470 = l;
}
 __return_114473 = -1073741802;
}
compRetStatus = __return_114473;
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
goto label_114572;
goto label_114572;
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
goto label_114699;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114699;
}
else 
{
returnVal2 = 259;
label_114699:; 
goto label_114711;
}
}
}
else 
{
returnVal2 = 259;
label_114711:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114840;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114816;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114816:; 
goto label_114840;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114840:; 
 __return_114844 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114745;
}
status9 = __return_114844;
goto label_114849;
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
goto label_114113;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114113;
}
else 
{
returnVal2 = 259;
label_114113:; 
goto label_114125;
}
}
}
else 
{
returnVal2 = 259;
label_114125:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114254;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114230;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114230:; 
goto label_114254;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114254:; 
 __return_114264 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_114163:; 
 __return_114265 = returnVal2;
}
tmp = __return_114264;
goto label_114267;
tmp = __return_114265;
label_114267:; 
 __return_114276 = tmp;
}
tmp = __return_114276;
 __return_114279 = tmp;
}
status10 = __return_114279;
goto label_115942;
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
goto label_113917;
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
goto label_113933;
}
else 
{
label_113933:; 
}
label_113936:; 
 __return_113953 = myStatus;
}
compRetStatus = __return_113953;
goto label_113957;
}
else 
{
 __return_113954 = myStatus;
}
compRetStatus = __return_113954;
goto label_113957;
}
}
else 
{
label_113917:; 
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
goto label_113948;
}
else 
{
label_113948:; 
}
goto label_113936;
}
}
else 
{
 __return_113955 = myStatus;
}
compRetStatus = __return_113955;
label_113957:; 
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
goto label_113990;
label_113990:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_114119;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114119;
}
else 
{
returnVal2 = 259;
label_114119:; 
goto label_114131;
}
}
}
else 
{
returnVal2 = 259;
label_114131:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114260;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114236;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114236:; 
goto label_114260;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114260:; 
 __return_114261 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114163;
}
tmp = __return_114261;
goto label_114267;
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
goto label_114115;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114115;
}
else 
{
returnVal2 = 259;
label_114115:; 
goto label_114127;
}
}
}
else 
{
returnVal2 = 259;
label_114127:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114256;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114232;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114232:; 
goto label_114256;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114256:; 
 __return_114263 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114163;
}
tmp = __return_114263;
goto label_114267;
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
 __return_113888 = l;
}
 __return_113891 = -1073741802;
}
compRetStatus = __return_113891;
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
goto label_113990;
goto label_113990;
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
goto label_114117;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_114117;
}
else 
{
returnVal2 = 259;
label_114117:; 
goto label_114129;
}
}
}
else 
{
returnVal2 = 259;
label_114129:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_114258;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_114234;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_114234:; 
goto label_114258;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_114258:; 
 __return_114262 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_114163;
}
tmp = __return_114262;
goto label_114267;
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
goto label_112301;
}
else 
{
goto label_112278;
}
}
else 
{
label_112278:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_112301;
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
 __return_112320 = 0;
goto label_112321;
}
else 
{
 __return_112321 = -1073741823;
label_112321:; 
}
status3 = __return_112321;
goto label_112325;
}
else 
{
label_112301:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_112322 = 0;
goto label_112323;
}
else 
{
 __return_112323 = -1073741823;
label_112323:; 
}
status3 = __return_112323;
label_112325:; 
setPagable = 0;
if (irpSp__Parameters__UsageNotification__InPath == 0)
{
goto label_112336;
}
else 
{
if (deviceExtension__PagingPathCount != 1)
{
label_112336:; 
if (status3 == status3)
{
setPagable = 1;
goto label_112343;
}
else 
{
goto label_112343;
}
}
else 
{
label_112343:; 
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
goto label_113566;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113566;
}
else 
{
returnVal2 = 259;
label_113566:; 
goto label_113578;
}
}
}
else 
{
returnVal2 = 259;
label_113578:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113707;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113683;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113683:; 
goto label_113707;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113707:; 
 __return_113717 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_113616:; 
 __return_113718 = returnVal2;
}
status9 = __return_113717;
goto label_112892;
status9 = __return_113718;
goto label_112892;
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
goto label_113370;
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
goto label_113386;
}
else 
{
label_113386:; 
}
label_113389:; 
 __return_113406 = myStatus;
}
compRetStatus = __return_113406;
goto label_113410;
}
else 
{
 __return_113407 = myStatus;
}
compRetStatus = __return_113407;
goto label_113410;
}
}
else 
{
label_113370:; 
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
goto label_113401;
}
else 
{
label_113401:; 
}
goto label_113389;
}
}
else 
{
 __return_113408 = myStatus;
}
compRetStatus = __return_113408;
label_113410:; 
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
goto label_113443;
label_113443:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_113572;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113572;
}
else 
{
returnVal2 = 259;
label_113572:; 
goto label_113584;
}
}
}
else 
{
returnVal2 = 259;
label_113584:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113713;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113689;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113689:; 
goto label_113713;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113713:; 
 __return_113714 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113616;
}
status9 = __return_113714;
goto label_112892;
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
goto label_113568;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113568;
}
else 
{
returnVal2 = 259;
label_113568:; 
goto label_113580;
}
}
}
else 
{
returnVal2 = 259;
label_113580:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113709;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113685;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113685:; 
goto label_113709;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113709:; 
 __return_113716 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113616;
}
status9 = __return_113716;
goto label_112892;
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
 __return_113341 = l;
}
 __return_113344 = -1073741802;
}
compRetStatus = __return_113344;
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
goto label_113443;
goto label_113443;
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
goto label_113570;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113570;
}
else 
{
returnVal2 = 259;
label_113570:; 
goto label_113582;
}
}
}
else 
{
returnVal2 = 259;
label_113582:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113711;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113687;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113687:; 
goto label_113711;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113711:; 
 __return_113715 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113616;
}
status9 = __return_113715;
goto label_112892;
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
goto label_113152;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113152;
}
else 
{
returnVal2 = 259;
label_113152:; 
goto label_113164;
}
}
}
else 
{
returnVal2 = 259;
label_113164:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113293;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113269;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113269:; 
goto label_113293;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113293:; 
 __return_113303 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_113202:; 
 __return_113304 = returnVal2;
}
status9 = __return_113303;
goto label_112892;
status9 = __return_113304;
goto label_112892;
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
goto label_112956;
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
goto label_112972;
}
else 
{
label_112972:; 
}
label_112975:; 
 __return_112992 = myStatus;
}
compRetStatus = __return_112992;
goto label_112996;
}
else 
{
 __return_112993 = myStatus;
}
compRetStatus = __return_112993;
goto label_112996;
}
}
else 
{
label_112956:; 
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
goto label_112987;
}
else 
{
label_112987:; 
}
goto label_112975;
}
}
else 
{
 __return_112994 = myStatus;
}
compRetStatus = __return_112994;
label_112996:; 
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
goto label_113029;
label_113029:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_113158;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113158;
}
else 
{
returnVal2 = 259;
label_113158:; 
goto label_113170;
}
}
}
else 
{
returnVal2 = 259;
label_113170:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113299;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113275;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113275:; 
goto label_113299;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113299:; 
 __return_113300 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113202;
}
status9 = __return_113300;
goto label_112892;
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
goto label_113154;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113154;
}
else 
{
returnVal2 = 259;
label_113154:; 
goto label_113166;
}
}
}
else 
{
returnVal2 = 259;
label_113166:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113295;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113271;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113271:; 
goto label_113295;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113295:; 
 __return_113302 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113202;
}
status9 = __return_113302;
goto label_112892;
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
 __return_112927 = l;
}
 __return_112930 = -1073741802;
}
compRetStatus = __return_112930;
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
goto label_113029;
goto label_113029;
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
goto label_113156;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_113156;
}
else 
{
returnVal2 = 259;
label_113156:; 
goto label_113168;
}
}
}
else 
{
returnVal2 = 259;
label_113168:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_113297;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_113273;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_113273:; 
goto label_113297;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_113297:; 
 __return_113301 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_113202;
}
status9 = __return_113301;
goto label_112892;
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
goto label_112738;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112738;
}
else 
{
returnVal2 = 259;
label_112738:; 
goto label_112750;
}
}
}
else 
{
returnVal2 = 259;
label_112750:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112879;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112855;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112855:; 
goto label_112879;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112879:; 
 __return_112889 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_112788:; 
 __return_112890 = returnVal2;
}
status9 = __return_112889;
goto label_112892;
status9 = __return_112890;
label_112892:; 
status9 = 259;
if (status9 == 0)
{
 __return_113794 = status9;
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
goto label_113764;
}
else 
{
goto label_113741;
}
}
else 
{
label_113741:; 
if (customIrp == 1)
{
s = NP;
customIrp = 0;
goto label_113764;
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
 __return_113783 = 0;
goto label_113784;
}
else 
{
 __return_113784 = -1073741823;
label_113784:; 
}
goto label_113788;
}
else 
{
label_113764:; 
int tmp_ndt_7;
tmp_ndt_7 = __VERIFIER_nondet_int();
if (tmp_ndt_7 == 0)
{
 __return_113785 = 0;
goto label_113786;
}
else 
{
 __return_113786 = -1073741823;
label_113786:; 
}
label_113788:; 
status9 = myStatus;
 __return_113793 = status9;
}
status3 = __return_113793;
goto label_113796;
}
}
}
}
status3 = __return_113794;
label_113796:; 
if (status3 >= 0)
{
goto label_113808;
}
else 
{
if (setPagable == 1)
{
setPagable = 0;
goto label_113808;
}
else 
{
label_113808:; 
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
 __return_113815 = l;
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
goto label_113837;
label_113837:; 
 __return_113840 = status3;
}
status10 = __return_113840;
goto label_115942;
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
goto label_112542;
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
goto label_112558;
}
else 
{
label_112558:; 
}
label_112561:; 
 __return_112578 = myStatus;
}
compRetStatus = __return_112578;
goto label_112582;
}
else 
{
 __return_112579 = myStatus;
}
compRetStatus = __return_112579;
goto label_112582;
}
}
else 
{
label_112542:; 
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
goto label_112573;
}
else 
{
label_112573:; 
}
goto label_112561;
}
}
else 
{
 __return_112580 = myStatus;
}
compRetStatus = __return_112580;
label_112582:; 
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
goto label_112615;
label_112615:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_112744;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112744;
}
else 
{
returnVal2 = 259;
label_112744:; 
goto label_112756;
}
}
}
else 
{
returnVal2 = 259;
label_112756:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112885;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112861;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112861:; 
goto label_112885;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112885:; 
 __return_112886 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112788;
}
status9 = __return_112886;
goto label_112892;
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
goto label_112740;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112740;
}
else 
{
returnVal2 = 259;
label_112740:; 
goto label_112752;
}
}
}
else 
{
returnVal2 = 259;
label_112752:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112881;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112857;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112857:; 
goto label_112881;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112881:; 
 __return_112888 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112788;
}
status9 = __return_112888;
goto label_112892;
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
 __return_112513 = l;
}
 __return_112516 = -1073741802;
}
compRetStatus = __return_112516;
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
goto label_112615;
goto label_112615;
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
goto label_112742;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112742;
}
else 
{
returnVal2 = 259;
label_112742:; 
goto label_112754;
}
}
}
else 
{
returnVal2 = 259;
label_112754:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112883;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112859;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112859:; 
goto label_112883;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112883:; 
 __return_112887 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112788;
}
status9 = __return_112887;
goto label_112892;
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
goto label_112098;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112098;
}
else 
{
returnVal2 = 259;
label_112098:; 
goto label_112110;
}
}
}
else 
{
returnVal2 = 259;
label_112110:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112239;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112215;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112215:; 
goto label_112239;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112239:; 
 __return_112249 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
label_112148:; 
 __return_112250 = returnVal2;
}
tmp = __return_112249;
goto label_112252;
tmp = __return_112250;
label_112252:; 
 __return_112261 = tmp;
}
tmp___0 = __return_112261;
 __return_112264 = tmp___0;
}
status10 = __return_112264;
goto label_115942;
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
goto label_111902;
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
goto label_111918;
}
else 
{
label_111918:; 
}
label_111921:; 
 __return_111938 = myStatus;
}
compRetStatus = __return_111938;
goto label_111942;
}
else 
{
 __return_111939 = myStatus;
}
compRetStatus = __return_111939;
goto label_111942;
}
}
else 
{
label_111902:; 
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
goto label_111933;
}
else 
{
label_111933:; 
}
goto label_111921;
}
}
else 
{
 __return_111940 = myStatus;
}
compRetStatus = __return_111940;
label_111942:; 
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
goto label_111975;
label_111975:; 
if (Irp__PendingReturned == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
returnVal2 = 0;
goto label_112104;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112104;
}
else 
{
returnVal2 = 259;
label_112104:; 
goto label_112116;
}
}
}
else 
{
returnVal2 = 259;
label_112116:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112245;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112221;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112221:; 
goto label_112245;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112245:; 
 __return_112246 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112148;
}
tmp = __return_112246;
goto label_112252;
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
goto label_112100;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112100;
}
else 
{
returnVal2 = 259;
label_112100:; 
goto label_112112;
}
}
}
else 
{
returnVal2 = 259;
label_112112:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112241;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112217;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112217:; 
goto label_112241;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112241:; 
 __return_112248 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112148;
}
tmp = __return_112248;
goto label_112252;
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
 __return_111873 = l;
}
 __return_111876 = -1073741802;
}
compRetStatus = __return_111876;
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
goto label_111975;
goto label_111975;
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
goto label_112102;
}
else 
{
int tmp_ndt_6;
tmp_ndt_6 = __VERIFIER_nondet_int();
if (tmp_ndt_6 == 1)
{
returnVal2 = -1073741823;
goto label_112102;
}
else 
{
returnVal2 = 259;
label_112102:; 
goto label_112114;
}
}
}
else 
{
returnVal2 = 259;
label_112114:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal2;
goto label_112243;
}
else 
{
if (s == MPR1)
{
if (returnVal2 == 259)
{
s = MPR3;
lowerDriverReturn = returnVal2;
goto label_112219;
}
else 
{
s = NP;
lowerDriverReturn = returnVal2;
label_112219:; 
goto label_112243;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal2;
label_112243:; 
 __return_112247 = returnVal2;
}
else 
{
{
__VERIFIER_error();
}
goto label_112148;
}
tmp = __return_112247;
goto label_112252;
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
goto label_111629;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal = -1073741823;
goto label_111629;
}
else 
{
returnVal = 259;
label_111629:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_111771;
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
goto label_111741;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_111741:; 
goto label_111771;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_111771:; 
 __return_111772 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
 __return_111777 = returnVal;
}
tmp = __return_111772;
goto label_111779;
tmp = __return_111777;
label_111779:; 
 __return_111790 = tmp;
}
status10 = __return_111790;
if (we_should_unload == 0)
{
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_125867;
}
else 
{
goto label_125513;
}
}
else 
{
label_125513:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_125867;
}
else 
{
goto label_125548;
}
}
else 
{
label_125548:; 
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
goto label_125796;
}
else 
{
goto label_125638;
}
}
else 
{
goto label_125638;
}
}
else 
{
label_125638:; 
if (pended != 1)
{
if (s == DC)
{
if (status10 == 259)
{
{
__VERIFIER_error();
}
goto label_125752;
}
else 
{
goto label_125867;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_125700;
}
else 
{
goto label_125867;
}
}
}
else 
{
goto label_125867;
}
}
}
else 
{
goto label_125867;
}
}
else 
{
label_125867:; 
 __return_125877 = status10;
goto label_111365;
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
label_125466:; 
if (pended == 1)
{
if (s == NP)
{
s = NP;
goto label_125869;
}
else 
{
goto label_125515;
}
}
else 
{
label_125515:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_125869;
}
else 
{
goto label_125546;
}
}
else 
{
label_125546:; 
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
goto label_125796;
}
else 
{
goto label_125636;
}
}
else 
{
goto label_125636;
}
}
else 
{
label_125636:; 
if (pended != 1)
{
if (s == DC)
{
if (status10 == 259)
{
{
__VERIFIER_error();
}
label_125752:; 
 __return_125885 = status10;
goto label_111365;
}
else 
{
goto label_125869;
}
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
goto label_125700;
}
else 
{
goto label_125869;
}
}
}
else 
{
goto label_125869;
}
}
}
else 
{
goto label_125869;
}
}
else 
{
label_125869:; 
 __return_125875 = status10;
goto label_111365;
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
goto label_111443;
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
goto label_111459;
}
else 
{
label_111459:; 
}
label_111462:; 
 __return_111479 = myStatus;
}
compRetStatus = __return_111479;
goto label_111483;
}
else 
{
 __return_111480 = myStatus;
}
compRetStatus = __return_111480;
goto label_111483;
}
}
else 
{
label_111443:; 
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
goto label_111474;
}
else 
{
label_111474:; 
}
goto label_111462;
}
}
else 
{
 __return_111481 = myStatus;
}
compRetStatus = __return_111481;
label_111483:; 
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
goto label_111516;
label_111516:; 
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal = 0;
goto label_111623;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal = -1073741823;
goto label_111623;
}
else 
{
returnVal = 259;
label_111623:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_111765;
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
goto label_111747;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_111747:; 
goto label_111765;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_111765:; 
 __return_111775 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
label_111669:; 
 __return_111776 = returnVal;
}
tmp = __return_111775;
goto label_111779;
tmp = __return_111776;
goto label_111779;
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
goto label_111627;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal = -1073741823;
goto label_111627;
}
else 
{
returnVal = 259;
label_111627:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_111769;
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
goto label_111743;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_111743:; 
goto label_111769;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_111769:; 
 __return_111773 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_111669;
}
tmp = __return_111773;
goto label_111779;
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
 __return_111414 = l;
}
 __return_111417 = -1073741802;
}
compRetStatus = __return_111417;
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
goto label_111516;
goto label_111516;
}
}
else 
{
int tmp_ndt_8;
tmp_ndt_8 = __VERIFIER_nondet_int();
if (tmp_ndt_8 == 0)
{
returnVal = 0;
goto label_111625;
}
else 
{
int tmp_ndt_9;
tmp_ndt_9 = __VERIFIER_nondet_int();
if (tmp_ndt_9 == 1)
{
returnVal = -1073741823;
goto label_111625;
}
else 
{
returnVal = 259;
label_111625:; 
if (s == NP)
{
s = IPC;
lowerDriverReturn = returnVal;
goto label_111767;
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
goto label_111745;
}
else 
{
s = NP;
lowerDriverReturn = returnVal;
label_111745:; 
goto label_111767;
}
}
else 
{
if (s == SKIP1)
{
s = SKIP2;
lowerDriverReturn = returnVal;
label_111767:; 
 __return_111774 = returnVal;
}
else 
{
{
__VERIFIER_error();
}
goto label_111669;
}
tmp = __return_111774;
goto label_111779;
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
 __return_111365 = -1;
label_111365:; 
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
goto label_125871;
}
else 
{
goto label_125517;
}
}
else 
{
label_125517:; 
if (pended == 1)
{
if (s == MPR3)
{
s = MPR3;
goto label_125871;
}
else 
{
goto label_125544;
}
}
else 
{
label_125544:; 
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
label_125796:; 
 __return_125883 = status10;
goto label_125873;
}
else 
{
goto label_125634;
}
}
else 
{
goto label_125634;
}
}
else 
{
label_125634:; 
if (pended != 1)
{
if (s == DC)
{
goto label_125871;
}
else 
{
if (status10 != lowerDriverReturn)
{
{
__VERIFIER_error();
}
label_125700:; 
 __return_125887 = status10;
goto label_125873;
}
else 
{
goto label_125871;
}
}
}
else 
{
goto label_125871;
}
}
}
else 
{
goto label_125871;
}
}
else 
{
label_125871:; 
 __return_125873 = status10;
label_125873:; 
return 1;
}
}
}
}
}
