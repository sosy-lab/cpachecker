void error(void);
int m_pc  =    0;
int t1_pc  =    0;
int t2_pc  =    0;
int t3_pc  =    0;
int t4_pc  =    0;
int m_st  ;
int t1_st  ;
int t2_st  ;
int t3_st  ;
int t4_st  ;
int m_i  ;
int t1_i  ;
int t2_i  ;
int t3_i  ;
int t4_i  ;
int M_E  =    2;
int T1_E  =    2;
int T2_E  =    2;
int T3_E  =    2;
int T4_E  =    2;
int E_M  =    2;
int E_1  =    2;
int E_2  =    2;
int E_3  =    2;
int E_4  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
int is_transmit2_triggered(void) ;
int is_transmit3_triggered(void) ;
int is_transmit4_triggered(void) ;
void immediate_notify(void) ;
int token  ;
int __VERIFIER_nondet_int()  ;
int local  ;
void master(void);
void transmit1(void);
void transmit2(void);
void transmit3(void);
void transmit4(void);
void update_channels(void);
void init_threads(void);
int exists_runnable_thread(void);
void eval(void);
void fire_delta_events(void);
void reset_delta_events(void);
void activate_threads(void);
void fire_time_events(void);
void reset_time_events(void);
void init_model(void);
int stop_simulation(void);
void start_simulation(void);
int main(void);
int __return_757828;
int __return_757850;
int __return_757872;
int __return_757894;
int __return_757916;
int __return_758038;
int __return_765460;
int __return_765532;
int __return_765554;
int __return_765576;
int __return_765598;
int __return_765624;
int __return_768498;
int __return_768570;
int __return_768592;
int __return_768614;
int __return_768640;
int __return_768662;
int __return_762446;
int __return_762518;
int __return_762540;
int __return_762562;
int __return_762588;
int __return_762614;
int __return_770042;
int __return_770114;
int __return_770136;
int __return_770162;
int __return_770184;
int __return_770206;
int __return_763978;
int __return_764050;
int __return_764072;
int __return_764098;
int __return_764120;
int __return_764146;
int __return_767190;
int __return_767262;
int __return_767284;
int __return_767310;
int __return_767336;
int __return_767358;
int __return_761242;
int __return_761314;
int __return_761336;
int __return_761362;
int __return_761388;
int __return_761414;
int __return_770346;
int __return_770418;
int __return_770440;
int __return_770464;
int __return_770486;
int __return_770508;
int __return_771202;
int __return_770614;
int __return_770640;
int __return_770662;
int __return_770684;
int __return_770706;
int __return_771166;
int __return_771124;
int __return_764426;
int __return_764498;
int __return_764520;
int __return_764544;
int __return_764566;
int __return_764592;
int __return_765262;
int __return_764698;
int __return_764724;
int __return_764746;
int __return_764768;
int __return_764794;
int __return_765226;
int __return_765184;
int __return_767550;
int __return_767622;
int __return_767644;
int __return_767668;
int __return_767694;
int __return_767716;
int __return_768344;
int __return_767822;
int __return_767848;
int __return_767870;
int __return_767896;
int __return_767918;
int __return_768308;
int __return_768266;
int __return_761614;
int __return_761686;
int __return_761708;
int __return_761732;
int __return_761758;
int __return_761784;
int __return_762288;
int __return_761890;
int __return_761916;
int __return_761938;
int __return_761964;
int __return_761990;
int __return_762252;
int __return_762224;
int __return_771968;
int __return_771994;
int __return_772020;
int __return_772046;
int __return_772072;
int __return_772618;
int __return_773048;
int __return_773074;
int __return_773100;
int __return_773126;
int __return_773152;
int __return_773714;
int __return_773732;
int __return_774050;
int __return_774132;
int __return_768852;
int __return_768924;
int __return_768946;
int __return_768974;
int __return_768996;
int __return_769018;
int __return_769932;
int __return_769124;
int __return_769150;
int __return_769172;
int __return_769196;
int __return_769218;
int __return_769896;
int __return_769334;
int __return_769360;
int __return_769386;
int __return_769408;
int __return_769430;
int __return_769854;
int __return_769806;
int __return_762856;
int __return_762928;
int __return_762950;
int __return_762978;
int __return_763000;
int __return_763026;
int __return_763820;
int __return_763132;
int __return_763158;
int __return_763180;
int __return_763204;
int __return_763230;
int __return_763784;
int __return_763346;
int __return_763372;
int __return_763398;
int __return_763420;
int __return_763446;
int __return_763742;
int __return_763712;
int __return_771820;
int __return_771846;
int __return_771872;
int __return_771898;
int __return_771924;
int __return_772592;
int __return_772900;
int __return_772926;
int __return_772952;
int __return_772978;
int __return_773004;
int __return_773766;
int __return_773784;
int __return_773994;
int __return_774130;
int __return_765864;
int __return_765936;
int __return_765958;
int __return_765986;
int __return_766012;
int __return_766034;
int __return_767076;
int __return_766140;
int __return_766166;
int __return_766188;
int __return_766216;
int __return_766238;
int __return_767040;
int __return_766354;
int __return_766380;
int __return_766406;
int __return_766428;
int __return_766452;
int __return_766998;
int __return_766572;
int __return_766598;
int __return_766624;
int __return_766650;
int __return_766672;
int __return_766950;
int __return_766918;
int __return_771672;
int __return_771698;
int __return_771724;
int __return_771750;
int __return_771776;
int __return_772566;
int __return_772752;
int __return_772778;
int __return_772804;
int __return_772830;
int __return_772856;
int __return_773818;
int __return_773836;
int __return_773938;
int __return_774128;
int __return_759606;
int __return_759678;
int __return_759700;
int __return_759728;
int __return_759754;
int __return_759780;
int __return_761112;
int __return_759886;
int __return_759912;
int __return_759934;
int __return_759962;
int __return_759988;
int __return_761076;
int __return_760104;
int __return_760130;
int __return_760156;
int __return_760178;
int __return_760206;
int __return_761034;
int __return_760326;
int __return_760352;
int __return_760378;
int __return_760404;
int __return_760426;
int __return_760986;
int __return_760548;
int __return_760576;
int __return_760602;
int __return_760628;
int __return_760654;
int __return_760714;
int __return_760810;
int __return_760832;
int __return_760860;
int __return_760886;
int __return_760912;
int __return_758110;
int __return_758132;
int __return_758154;
int __return_758176;
int __return_758198;
int __return_771234;
int __return_765294;
int __return_768376;
int __return_762320;
int __return_769964;
int __return_763852;
int __return_767108;
int __return_761144;
int __return_770312;
int __return_764300;
int __return_767468;
int __return_761528;
int __return_768814;
int __return_762770;
int __return_765822;
int __return_759582;
int __return_772116;
int __return_772142;
int __return_772168;
int __return_772194;
int __return_772220;
int __return_772644;
int __return_773196;
int __return_773222;
int __return_773248;
int __return_773274;
int __return_773300;
int __return_773662;
int __return_773680;
int __return_774106;
int __return_774134;
int main()
{
int __retres1 ;
{
m_i = 1;
t1_i = 1;
t2_i = 1;
t3_i = 1;
t4_i = 1;
}
{
int kernel_st ;
int tmp ;
int tmp___0 ;
kernel_st = 0;
{
}
{
m_st = 0;
t1_st = 0;
t2_st = 0;
t3_st = 0;
t4_st = 0;
}
{
if (M_E == 0)
{
M_E = 1;
goto label_757730;
}
else 
{
label_757730:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_757740;
}
else 
{
label_757740:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_757750;
}
else 
{
label_757750:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_757760;
}
else 
{
label_757760:; 
if (T4_E == 0)
{
T4_E = 1;
goto label_757770;
}
else 
{
label_757770:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_757828 = __retres1;
}
tmp = __return_757828;
{
int __retres1 ;
__retres1 = 0;
 __return_757850 = __retres1;
}
tmp___0 = __return_757850;
{
int __retres1 ;
__retres1 = 0;
 __return_757872 = __retres1;
}
tmp___1 = __return_757872;
{
int __retres1 ;
__retres1 = 0;
 __return_757894 = __retres1;
}
tmp___2 = __return_757894;
{
int __retres1 ;
__retres1 = 0;
 __return_757916 = __retres1;
}
tmp___3 = __return_757916;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_757936;
}
else 
{
label_757936:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_757946;
}
else 
{
label_757946:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_757956;
}
else 
{
label_757956:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_757966;
}
else 
{
label_757966:; 
if (T4_E == 1)
{
T4_E = 2;
goto label_757976;
}
else 
{
label_757976:; 
}
kernel_st = 1;
{
int tmp ;
label_758020:; 
{
int __retres1 ;
__retres1 = 1;
 __return_758038 = __retres1;
}
tmp = __return_758038;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
goto label_758020;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759522:; 
{
int __retres1 ;
__retres1 = 1;
 __return_765460 = __retres1;
}
tmp = __return_765460;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_759522;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_762740;
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_764228;
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_764632;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_765532 = __retres1;
}
tmp = __return_765532;
{
int __retres1 ;
__retres1 = 0;
 __return_765554 = __retres1;
}
tmp___0 = __return_765554;
{
int __retres1 ;
__retres1 = 0;
 __return_765576 = __retres1;
}
tmp___1 = __return_765576;
{
int __retres1 ;
__retres1 = 0;
 __return_765598 = __retres1;
}
tmp___2 = __return_765598;
{
int __retres1 ;
__retres1 = 0;
 __return_765624 = __retres1;
}
tmp___3 = __return_765624;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_765304;
}
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
label_758834:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_768498 = __retres1;
}
tmp = __return_768498;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_758834;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_767440;
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_767756;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_768570 = __retres1;
}
tmp = __return_768570;
{
int __retres1 ;
__retres1 = 0;
 __return_768592 = __retres1;
}
tmp___0 = __return_768592;
{
int __retres1 ;
__retres1 = 0;
 __return_768614 = __retres1;
}
tmp___1 = __return_768614;
{
int __retres1 ;
__retres1 = 0;
 __return_768640 = __retres1;
}
tmp___2 = __return_768640;
{
int __retres1 ;
__retres1 = 0;
 __return_768662 = __retres1;
}
tmp___3 = __return_768662;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_768386;
}
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759538:; 
{
int __retres1 ;
__retres1 = 1;
 __return_762446 = __retres1;
}
tmp = __return_762446;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
label_762740:; 
goto label_759538;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_761496;
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_761824;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_762518 = __retres1;
}
tmp = __return_762518;
{
int __retres1 ;
__retres1 = 0;
 __return_762540 = __retres1;
}
tmp___0 = __return_762540;
{
int __retres1 ;
__retres1 = 0;
 __return_762562 = __retres1;
}
tmp___1 = __return_762562;
{
int __retres1 ;
__retres1 = 0;
 __return_762588 = __retres1;
}
tmp___2 = __return_762588;
{
int __retres1 ;
__retres1 = 0;
 __return_762614 = __retres1;
}
tmp___3 = __return_762614;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_762330;
}
}
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_758490:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_770042 = __retres1;
}
tmp = __return_770042;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_758490;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_769058;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_770114 = __retres1;
}
tmp = __return_770114;
{
int __retres1 ;
__retres1 = 0;
 __return_770136 = __retres1;
}
tmp___0 = __return_770136;
{
int __retres1 ;
__retres1 = 0;
 __return_770162 = __retres1;
}
tmp___1 = __return_770162;
{
int __retres1 ;
__retres1 = 0;
 __return_770184 = __retres1;
}
tmp___2 = __return_770184;
{
int __retres1 ;
__retres1 = 0;
 __return_770206 = __retres1;
}
tmp___3 = __return_770206;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_769974;
}
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759530:; 
{
int __retres1 ;
__retres1 = 1;
 __return_763978 = __retres1;
}
tmp = __return_763978;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_764228:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_759530;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_761500;
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_763066;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_764050 = __retres1;
}
tmp = __return_764050;
{
int __retres1 ;
__retres1 = 0;
 __return_764072 = __retres1;
}
tmp___0 = __return_764072;
{
int __retres1 ;
__retres1 = 0;
 __return_764098 = __retres1;
}
tmp___1 = __return_764098;
{
int __retres1 ;
__retres1 = 0;
 __return_764120 = __retres1;
}
tmp___2 = __return_764120;
{
int __retres1 ;
__retres1 = 0;
 __return_764146 = __retres1;
}
tmp___3 = __return_764146;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_763862;
}
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
label_758842:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_767190 = __retres1;
}
tmp = __return_767190;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_767440:; 
goto label_758842;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_766074;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_767262 = __retres1;
}
tmp = __return_767262;
{
int __retres1 ;
__retres1 = 0;
 __return_767284 = __retres1;
}
tmp___0 = __return_767284;
{
int __retres1 ;
__retres1 = 0;
 __return_767310 = __retres1;
}
tmp___1 = __return_767310;
{
int __retres1 ;
__retres1 = 0;
 __return_767336 = __retres1;
}
tmp___2 = __return_767336;
{
int __retres1 ;
__retres1 = 0;
 __return_767358 = __retres1;
}
tmp___3 = __return_767358;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_767118;
}
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759546:; 
{
int __retres1 ;
__retres1 = 1;
 __return_761242 = __retres1;
}
tmp = __return_761242;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_761496:; 
label_761500:; 
goto label_759546;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_759820;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_761314 = __retres1;
}
tmp = __return_761314;
{
int __retres1 ;
__retres1 = 0;
 __return_761336 = __retres1;
}
tmp___0 = __return_761336;
{
int __retres1 ;
__retres1 = 0;
 __return_761362 = __retres1;
}
tmp___1 = __return_761362;
{
int __retres1 ;
__retres1 = 0;
 __return_761388 = __retres1;
}
tmp___2 = __return_761388;
{
int __retres1 ;
__retres1 = 0;
 __return_761414 = __retres1;
}
tmp___3 = __return_761414;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_761154;
}
}
}
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
label_758318:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_770346 = __retres1;
}
tmp = __return_770346;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_758318;
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_770418 = __retres1;
}
tmp = __return_770418;
{
int __retres1 ;
__retres1 = 1;
 __return_770440 = __retres1;
}
tmp___0 = __return_770440;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_770464 = __retres1;
}
tmp___1 = __return_770464;
{
int __retres1 ;
__retres1 = 0;
 __return_770486 = __retres1;
}
tmp___2 = __return_770486;
{
int __retres1 ;
__retres1 = 0;
 __return_770508 = __retres1;
}
tmp___3 = __return_770508;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_770542:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_771202 = __retres1;
}
tmp = __return_771202;
goto label_770542;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_765154;
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_768102;
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_769268;
}
}
else 
{
t1_st = 1;
{
t1_started();
token = token + 1;
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_770614 = __retres1;
}
tmp = __return_770614;
{
int __retres1 ;
__retres1 = 0;
 __return_770640 = __retres1;
}
tmp___0 = __return_770640;
{
int __retres1 ;
__retres1 = 0;
 __return_770662 = __retres1;
}
tmp___1 = __return_770662;
{
int __retres1 ;
__retres1 = 0;
 __return_770684 = __retres1;
}
tmp___2 = __return_770684;
{
int __retres1 ;
__retres1 = 0;
 __return_770706 = __retres1;
}
tmp___3 = __return_770706;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_770746:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_771166 = __retres1;
}
tmp = __return_771166;
goto label_770746;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_765156;
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_768104;
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_770834:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_771124 = __retres1;
}
tmp = __return_771124;
goto label_770834;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_765158;
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_768106;
}
}
}
}
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759526:; 
{
int __retres1 ;
__retres1 = 1;
 __return_764426 = __retres1;
}
tmp = __return_764426;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
label_764632:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_759526;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_762172;
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_763278;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_764498 = __retres1;
}
tmp = __return_764498;
{
int __retres1 ;
__retres1 = 1;
 __return_764520 = __retres1;
}
tmp___0 = __return_764520;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_764544 = __retres1;
}
tmp___1 = __return_764544;
{
int __retres1 ;
__retres1 = 0;
 __return_764566 = __retres1;
}
tmp___2 = __return_764566;
{
int __retres1 ;
__retres1 = 0;
 __return_764592 = __retres1;
}
tmp___3 = __return_764592;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_764626:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
label_765154:; 
{
int __retres1 ;
__retres1 = 1;
 __return_765262 = __retres1;
}
tmp = __return_765262;
goto label_764626;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_762174;
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_763280;
}
}
else 
{
t1_st = 1;
{
t1_started();
token = token + 1;
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_764698 = __retres1;
}
tmp = __return_764698;
{
int __retres1 ;
__retres1 = 0;
 __return_764724 = __retres1;
}
tmp___0 = __return_764724;
{
int __retres1 ;
__retres1 = 0;
 __return_764746 = __retres1;
}
tmp___1 = __return_764746;
{
int __retres1 ;
__retres1 = 0;
 __return_764768 = __retres1;
}
tmp___2 = __return_764768;
{
int __retres1 ;
__retres1 = 0;
 __return_764794 = __retres1;
}
tmp___3 = __return_764794;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_764834:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
label_765156:; 
{
int __retres1 ;
__retres1 = 1;
 __return_765226 = __retres1;
}
tmp = __return_765226;
goto label_764834;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_762176;
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_764966:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
label_765158:; 
{
int __retres1 ;
__retres1 = 1;
 __return_765184 = __retres1;
}
tmp = __return_765184;
goto label_764966;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_762178;
}
}
}
}
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
label_758838:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_767550 = __retres1;
}
tmp = __return_767550;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
label_767756:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_758838;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_766286;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_767622 = __retres1;
}
tmp = __return_767622;
{
int __retres1 ;
__retres1 = 1;
 __return_767644 = __retres1;
}
tmp___0 = __return_767644;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_767668 = __retres1;
}
tmp___1 = __return_767668;
{
int __retres1 ;
__retres1 = 0;
 __return_767694 = __retres1;
}
tmp___2 = __return_767694;
{
int __retres1 ;
__retres1 = 0;
 __return_767716 = __retres1;
}
tmp___3 = __return_767716;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_767750:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
label_768102:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_768344 = __retres1;
}
tmp = __return_768344;
goto label_767750;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_762190;
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_766288;
}
}
else 
{
t1_st = 1;
{
t1_started();
token = token + 1;
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_767822 = __retres1;
}
tmp = __return_767822;
{
int __retres1 ;
__retres1 = 0;
 __return_767848 = __retres1;
}
tmp___0 = __return_767848;
{
int __retres1 ;
__retres1 = 0;
 __return_767870 = __retres1;
}
tmp___1 = __return_767870;
{
int __retres1 ;
__retres1 = 0;
 __return_767896 = __retres1;
}
tmp___2 = __return_767896;
{
int __retres1 ;
__retres1 = 0;
 __return_767918 = __retres1;
}
tmp___3 = __return_767918;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_767958:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
label_768104:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_768308 = __retres1;
}
tmp = __return_768308;
goto label_767958;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_762192;
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_768090:; 
label_768106:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_768266 = __retres1;
}
tmp = __return_768266;
goto label_768090;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_762194;
}
}
}
}
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759542:; 
{
int __retres1 ;
__retres1 = 1;
 __return_761614 = __retres1;
}
tmp = __return_761614;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
label_761824:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
label_762172:; 
goto label_759542;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_760036;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_761686 = __retres1;
}
tmp = __return_761686;
{
int __retres1 ;
__retres1 = 1;
 __return_761708 = __retres1;
}
tmp___0 = __return_761708;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_761732 = __retres1;
}
tmp___1 = __return_761732;
{
int __retres1 ;
__retres1 = 0;
 __return_761758 = __retres1;
}
tmp___2 = __return_761758;
{
int __retres1 ;
__retres1 = 0;
 __return_761784 = __retres1;
}
tmp___3 = __return_761784;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_761818:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
label_762174:; 
label_762190:; 
{
int __retres1 ;
__retres1 = 1;
 __return_762288 = __retres1;
}
tmp = __return_762288;
goto label_761818;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_760038;
}
}
else 
{
t1_st = 1;
{
t1_started();
token = token + 1;
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_761890 = __retres1;
}
tmp = __return_761890;
{
int __retres1 ;
__retres1 = 0;
 __return_761916 = __retres1;
}
tmp___0 = __return_761916;
{
int __retres1 ;
__retres1 = 0;
 __return_761938 = __retres1;
}
tmp___1 = __return_761938;
{
int __retres1 ;
__retres1 = 0;
 __return_761964 = __retres1;
}
tmp___2 = __return_761964;
{
int __retres1 ;
__retres1 = 0;
 __return_761990 = __retres1;
}
tmp___3 = __return_761990;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_762030:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
label_762176:; 
label_762192:; 
{
int __retres1 ;
__retres1 = 1;
 __return_762252 = __retres1;
}
tmp = __return_762252;
goto label_762030;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_762178:; 
label_762194:; 
{
int __retres1 ;
__retres1 = 0;
 __return_762224 = __retres1;
}
tmp = __return_762224;
}
label_771274:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_771414;
}
else 
{
label_771414:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_771424;
}
else 
{
label_771424:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_771434;
}
else 
{
label_771434:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_771444;
}
else 
{
label_771444:; 
if (T4_E == 0)
{
T4_E = 1;
goto label_771454;
}
else 
{
label_771454:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_771968 = __retres1;
}
tmp = __return_771968;
{
int __retres1 ;
__retres1 = 0;
 __return_771994 = __retres1;
}
tmp___0 = __return_771994;
{
int __retres1 ;
__retres1 = 0;
 __return_772020 = __retres1;
}
tmp___1 = __return_772020;
{
int __retres1 ;
__retres1 = 0;
 __return_772046 = __retres1;
}
tmp___2 = __return_772046;
{
int __retres1 ;
__retres1 = 0;
 __return_772072 = __retres1;
}
tmp___3 = __return_772072;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_772318;
}
else 
{
label_772318:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_772328;
}
else 
{
label_772328:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_772338;
}
else 
{
label_772338:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_772348;
}
else 
{
label_772348:; 
if (T4_E == 1)
{
T4_E = 2;
goto label_772358;
}
else 
{
label_772358:; 
}
{
int __retres1 ;
__retres1 = 0;
 __return_772618 = __retres1;
}
tmp = __return_772618;
if (tmp == 0)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_773048 = __retres1;
}
tmp = __return_773048;
{
int __retres1 ;
__retres1 = 0;
 __return_773074 = __retres1;
}
tmp___0 = __return_773074;
{
int __retres1 ;
__retres1 = 0;
 __return_773100 = __retres1;
}
tmp___1 = __return_773100;
{
int __retres1 ;
__retres1 = 0;
 __return_773126 = __retres1;
}
tmp___2 = __return_773126;
{
int __retres1 ;
__retres1 = 0;
 __return_773152 = __retres1;
}
tmp___3 = __return_773152;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_773398;
}
else 
{
label_773398:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_773408;
}
else 
{
label_773408:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_773418;
}
else 
{
label_773418:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_773428;
}
else 
{
label_773428:; 
if (T4_E == 1)
{
T4_E = 2;
goto label_773438;
}
else 
{
label_773438:; 
}
goto label_772668;
}
}
}
}
}
}
else 
{
label_772668:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_773714 = __retres1;
}
tmp = __return_773714;
if (tmp == 0)
{
__retres2 = 1;
goto label_773724;
}
else 
{
__retres2 = 0;
label_773724:; 
 __return_773732 = __retres2;
}
tmp___0 = __return_773732;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_774050 = __retres1;
}
tmp = __return_774050;
}
goto label_771274;
}
else 
{
}
__retres1 = 0;
 __return_774132 = __retres1;
return 1;
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
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_758494:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_768852 = __retres1;
}
tmp = __return_768852;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
label_769058:; 
goto label_758494;
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_768924 = __retres1;
}
tmp = __return_768924;
{
int __retres1 ;
__retres1 = 1;
 __return_768946 = __retres1;
}
tmp___0 = __return_768946;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_768974 = __retres1;
}
tmp___1 = __return_768974;
{
int __retres1 ;
__retres1 = 0;
 __return_768996 = __retres1;
}
tmp___2 = __return_768996;
{
int __retres1 ;
__retres1 = 0;
 __return_769018 = __retres1;
}
tmp___3 = __return_769018;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_769052:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_769268:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_769932 = __retres1;
}
tmp = __return_769932;
goto label_769052;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_763676;
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_766504;
}
}
else 
{
t1_st = 1;
{
t1_started();
token = token + 1;
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_769124 = __retres1;
}
tmp = __return_769124;
{
int __retres1 ;
__retres1 = 0;
 __return_769150 = __retres1;
}
tmp___0 = __return_769150;
{
int __retres1 ;
__retres1 = 1;
 __return_769172 = __retres1;
}
tmp___1 = __return_769172;
t2_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_769196 = __retres1;
}
tmp___2 = __return_769196;
{
int __retres1 ;
__retres1 = 0;
 __return_769218 = __retres1;
}
tmp___3 = __return_769218;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_769258:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_769896 = __retres1;
}
tmp = __return_769896;
goto label_769258;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_763678;
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_766506;
}
}
else 
{
t2_st = 1;
{
t2_started();
token = token + 1;
E_3 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_769334 = __retres1;
}
tmp = __return_769334;
{
int __retres1 ;
__retres1 = 0;
 __return_769360 = __retres1;
}
tmp___0 = __return_769360;
{
int __retres1 ;
__retres1 = 0;
 __return_769386 = __retres1;
}
tmp___1 = __return_769386;
{
int __retres1 ;
__retres1 = 0;
 __return_769408 = __retres1;
}
tmp___2 = __return_769408;
{
int __retres1 ;
__retres1 = 0;
 __return_769430 = __retres1;
}
tmp___3 = __return_769430;
}
}
E_3 = 2;
t2_pc = 1;
t2_st = 2;
}
label_769470:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_769854 = __retres1;
}
tmp = __return_769854;
goto label_769470;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_763680;
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
label_769602:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_769806 = __retres1;
}
tmp = __return_769806;
goto label_769602;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_763682;
}
}
}
}
}
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759534:; 
{
int __retres1 ;
__retres1 = 1;
 __return_762856 = __retres1;
}
tmp = __return_762856;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
label_763066:; 
label_763278:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_759534;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_760256;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_762928 = __retres1;
}
tmp = __return_762928;
{
int __retres1 ;
__retres1 = 1;
 __return_762950 = __retres1;
}
tmp___0 = __return_762950;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_762978 = __retres1;
}
tmp___1 = __return_762978;
{
int __retres1 ;
__retres1 = 0;
 __return_763000 = __retres1;
}
tmp___2 = __return_763000;
{
int __retres1 ;
__retres1 = 0;
 __return_763026 = __retres1;
}
tmp___3 = __return_763026;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_763060:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_763280:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
label_763676:; 
{
int __retres1 ;
__retres1 = 1;
 __return_763820 = __retres1;
}
tmp = __return_763820;
goto label_763060;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_760258;
}
}
else 
{
t1_st = 1;
{
t1_started();
token = token + 1;
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_763132 = __retres1;
}
tmp = __return_763132;
{
int __retres1 ;
__retres1 = 0;
 __return_763158 = __retres1;
}
tmp___0 = __return_763158;
{
int __retres1 ;
__retres1 = 1;
 __return_763180 = __retres1;
}
tmp___1 = __return_763180;
t2_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_763204 = __retres1;
}
tmp___2 = __return_763204;
{
int __retres1 ;
__retres1 = 0;
 __return_763230 = __retres1;
}
tmp___3 = __return_763230;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_763270:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
label_763678:; 
{
int __retres1 ;
__retres1 = 1;
 __return_763784 = __retres1;
}
tmp = __return_763784;
goto label_763270;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_760260;
}
}
else 
{
t2_st = 1;
{
t2_started();
token = token + 1;
E_3 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_763346 = __retres1;
}
tmp = __return_763346;
{
int __retres1 ;
__retres1 = 0;
 __return_763372 = __retres1;
}
tmp___0 = __return_763372;
{
int __retres1 ;
__retres1 = 0;
 __return_763398 = __retres1;
}
tmp___1 = __return_763398;
{
int __retres1 ;
__retres1 = 0;
 __return_763420 = __retres1;
}
tmp___2 = __return_763420;
{
int __retres1 ;
__retres1 = 0;
 __return_763446 = __retres1;
}
tmp___3 = __return_763446;
}
}
E_3 = 2;
t2_pc = 1;
t2_st = 2;
}
label_763486:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
label_763680:; 
{
int __retres1 ;
__retres1 = 1;
 __return_763742 = __retres1;
}
tmp = __return_763742;
goto label_763486;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
label_763682:; 
{
int __retres1 ;
__retres1 = 0;
 __return_763712 = __retres1;
}
tmp = __return_763712;
}
label_771276:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_771492;
}
else 
{
label_771492:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_771502;
}
else 
{
label_771502:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_771512;
}
else 
{
label_771512:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_771522;
}
else 
{
label_771522:; 
if (T4_E == 0)
{
T4_E = 1;
goto label_771532;
}
else 
{
label_771532:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_771820 = __retres1;
}
tmp = __return_771820;
{
int __retres1 ;
__retres1 = 0;
 __return_771846 = __retres1;
}
tmp___0 = __return_771846;
{
int __retres1 ;
__retres1 = 0;
 __return_771872 = __retres1;
}
tmp___1 = __return_771872;
{
int __retres1 ;
__retres1 = 0;
 __return_771898 = __retres1;
}
tmp___2 = __return_771898;
{
int __retres1 ;
__retres1 = 0;
 __return_771924 = __retres1;
}
tmp___3 = __return_771924;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_772396;
}
else 
{
label_772396:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_772406;
}
else 
{
label_772406:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_772416;
}
else 
{
label_772416:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_772426;
}
else 
{
label_772426:; 
if (T4_E == 1)
{
T4_E = 2;
goto label_772436;
}
else 
{
label_772436:; 
}
{
int __retres1 ;
__retres1 = 0;
 __return_772592 = __retres1;
}
tmp = __return_772592;
if (tmp == 0)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_772900 = __retres1;
}
tmp = __return_772900;
{
int __retres1 ;
__retres1 = 0;
 __return_772926 = __retres1;
}
tmp___0 = __return_772926;
{
int __retres1 ;
__retres1 = 0;
 __return_772952 = __retres1;
}
tmp___1 = __return_772952;
{
int __retres1 ;
__retres1 = 0;
 __return_772978 = __retres1;
}
tmp___2 = __return_772978;
{
int __retres1 ;
__retres1 = 0;
 __return_773004 = __retres1;
}
tmp___3 = __return_773004;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_773476;
}
else 
{
label_773476:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_773486;
}
else 
{
label_773486:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_773496;
}
else 
{
label_773496:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_773506;
}
else 
{
label_773506:; 
if (T4_E == 1)
{
T4_E = 2;
goto label_773516;
}
else 
{
label_773516:; 
}
goto label_772666;
}
}
}
}
}
}
else 
{
label_772666:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_773766 = __retres1;
}
tmp = __return_773766;
if (tmp == 0)
{
__retres2 = 1;
goto label_773776;
}
else 
{
__retres2 = 0;
label_773776:; 
 __return_773784 = __retres2;
}
tmp___0 = __return_773784;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_773994 = __retres1;
}
tmp = __return_773994;
}
goto label_771276;
}
else 
{
}
__retres1 = 0;
 __return_774130 = __retres1;
return 1;
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
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
label_758846:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_765864 = __retres1;
}
tmp = __return_765864;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
label_766074:; 
label_766286:; 
goto label_758846;
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_765936 = __retres1;
}
tmp = __return_765936;
{
int __retres1 ;
__retres1 = 1;
 __return_765958 = __retres1;
}
tmp___0 = __return_765958;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_765986 = __retres1;
}
tmp___1 = __return_765986;
{
int __retres1 ;
__retres1 = 0;
 __return_766012 = __retres1;
}
tmp___2 = __return_766012;
{
int __retres1 ;
__retres1 = 0;
 __return_766034 = __retres1;
}
tmp___3 = __return_766034;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_766068:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_766288:; 
label_766504:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_767076 = __retres1;
}
tmp = __return_767076;
goto label_766068;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_760482;
}
}
else 
{
t1_st = 1;
{
t1_started();
token = token + 1;
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_766140 = __retres1;
}
tmp = __return_766140;
{
int __retres1 ;
__retres1 = 0;
 __return_766166 = __retres1;
}
tmp___0 = __return_766166;
{
int __retres1 ;
__retres1 = 1;
 __return_766188 = __retres1;
}
tmp___1 = __return_766188;
t2_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_766216 = __retres1;
}
tmp___2 = __return_766216;
{
int __retres1 ;
__retres1 = 0;
 __return_766238 = __retres1;
}
tmp___3 = __return_766238;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_766278:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
label_766506:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_767040 = __retres1;
}
tmp = __return_767040;
goto label_766278;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_760484;
}
}
else 
{
t2_st = 1;
{
t2_started();
token = token + 1;
E_3 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_766354 = __retres1;
}
tmp = __return_766354;
{
int __retres1 ;
__retres1 = 0;
 __return_766380 = __retres1;
}
tmp___0 = __return_766380;
{
int __retres1 ;
__retres1 = 0;
 __return_766406 = __retres1;
}
tmp___1 = __return_766406;
{
int __retres1 ;
__retres1 = 1;
 __return_766428 = __retres1;
}
tmp___2 = __return_766428;
t3_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_766452 = __retres1;
}
tmp___3 = __return_766452;
}
}
E_3 = 2;
t2_pc = 1;
t2_st = 2;
}
label_766492:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_766998 = __retres1;
}
tmp = __return_766998;
goto label_766492;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
goto label_760486;
}
}
else 
{
t3_st = 1;
{
t3_started();
token = token + 1;
E_4 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_766572 = __retres1;
}
tmp = __return_766572;
{
int __retres1 ;
__retres1 = 0;
 __return_766598 = __retres1;
}
tmp___0 = __return_766598;
{
int __retres1 ;
__retres1 = 0;
 __return_766624 = __retres1;
}
tmp___1 = __return_766624;
{
int __retres1 ;
__retres1 = 0;
 __return_766650 = __retres1;
}
tmp___2 = __return_766650;
{
int __retres1 ;
__retres1 = 0;
 __return_766672 = __retres1;
}
tmp___3 = __return_766672;
}
}
E_4 = 2;
t3_pc = 1;
t3_st = 2;
}
label_766712:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_766950 = __retres1;
}
tmp = __return_766950;
goto label_766712;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
{
int __retres1 ;
__retres1 = 0;
 __return_766918 = __retres1;
}
tmp = __return_766918;
}
label_771278:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_771570;
}
else 
{
label_771570:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_771580;
}
else 
{
label_771580:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_771590;
}
else 
{
label_771590:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_771600;
}
else 
{
label_771600:; 
if (T4_E == 0)
{
T4_E = 1;
goto label_771610;
}
else 
{
label_771610:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_771672 = __retres1;
}
tmp = __return_771672;
{
int __retres1 ;
__retres1 = 0;
 __return_771698 = __retres1;
}
tmp___0 = __return_771698;
{
int __retres1 ;
__retres1 = 0;
 __return_771724 = __retres1;
}
tmp___1 = __return_771724;
{
int __retres1 ;
__retres1 = 0;
 __return_771750 = __retres1;
}
tmp___2 = __return_771750;
{
int __retres1 ;
__retres1 = 0;
 __return_771776 = __retres1;
}
tmp___3 = __return_771776;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_772474;
}
else 
{
label_772474:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_772484;
}
else 
{
label_772484:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_772494;
}
else 
{
label_772494:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_772504;
}
else 
{
label_772504:; 
if (T4_E == 1)
{
T4_E = 2;
goto label_772514;
}
else 
{
label_772514:; 
}
{
int __retres1 ;
__retres1 = 0;
 __return_772566 = __retres1;
}
tmp = __return_772566;
if (tmp == 0)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_772752 = __retres1;
}
tmp = __return_772752;
{
int __retres1 ;
__retres1 = 0;
 __return_772778 = __retres1;
}
tmp___0 = __return_772778;
{
int __retres1 ;
__retres1 = 0;
 __return_772804 = __retres1;
}
tmp___1 = __return_772804;
{
int __retres1 ;
__retres1 = 0;
 __return_772830 = __retres1;
}
tmp___2 = __return_772830;
{
int __retres1 ;
__retres1 = 0;
 __return_772856 = __retres1;
}
tmp___3 = __return_772856;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_773554;
}
else 
{
label_773554:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_773564;
}
else 
{
label_773564:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_773574;
}
else 
{
label_773574:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_773584;
}
else 
{
label_773584:; 
if (T4_E == 1)
{
T4_E = 2;
goto label_773594;
}
else 
{
label_773594:; 
}
goto label_772664;
}
}
}
}
}
}
else 
{
label_772664:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_773818 = __retres1;
}
tmp = __return_773818;
if (tmp == 0)
{
__retres2 = 1;
goto label_773828;
}
else 
{
__retres2 = 0;
label_773828:; 
 __return_773836 = __retres2;
}
tmp___0 = __return_773836;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_773938 = __retres1;
}
tmp = __return_773938;
}
goto label_771278;
}
else 
{
}
__retres1 = 0;
 __return_774128 = __retres1;
return 1;
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
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759550:; 
{
int __retres1 ;
__retres1 = 1;
 __return_759606 = __retres1;
}
tmp = __return_759606;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
label_759820:; 
label_760036:; 
label_760256:; 
goto label_759550;
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_759678 = __retres1;
}
tmp = __return_759678;
{
int __retres1 ;
__retres1 = 1;
 __return_759700 = __retres1;
}
tmp___0 = __return_759700;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_759728 = __retres1;
}
tmp___1 = __return_759728;
{
int __retres1 ;
__retres1 = 0;
 __return_759754 = __retres1;
}
tmp___2 = __return_759754;
{
int __retres1 ;
__retres1 = 0;
 __return_759780 = __retres1;
}
tmp___3 = __return_759780;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_759806:; 
label_759814:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_760038:; 
label_760258:; 
label_760482:; 
{
int __retres1 ;
__retres1 = 1;
 __return_761112 = __retres1;
}
tmp = __return_761112;
goto label_759814;
}
else 
{
t1_st = 1;
{
t1_started();
token = token + 1;
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_759886 = __retres1;
}
tmp = __return_759886;
{
int __retres1 ;
__retres1 = 0;
 __return_759912 = __retres1;
}
tmp___0 = __return_759912;
{
int __retres1 ;
__retres1 = 1;
 __return_759934 = __retres1;
}
tmp___1 = __return_759934;
t2_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_759962 = __retres1;
}
tmp___2 = __return_759962;
{
int __retres1 ;
__retres1 = 0;
 __return_759988 = __retres1;
}
tmp___3 = __return_759988;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_760028:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
label_760260:; 
label_760484:; 
{
int __retres1 ;
__retres1 = 1;
 __return_761076 = __retres1;
}
tmp = __return_761076;
goto label_760028;
}
else 
{
t2_st = 1;
{
t2_started();
token = token + 1;
E_3 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_760104 = __retres1;
}
tmp = __return_760104;
{
int __retres1 ;
__retres1 = 0;
 __return_760130 = __retres1;
}
tmp___0 = __return_760130;
{
int __retres1 ;
__retres1 = 0;
 __return_760156 = __retres1;
}
tmp___1 = __return_760156;
{
int __retres1 ;
__retres1 = 1;
 __return_760178 = __retres1;
}
tmp___2 = __return_760178;
t3_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_760206 = __retres1;
}
tmp___3 = __return_760206;
}
}
E_3 = 2;
t2_pc = 1;
t2_st = 2;
}
label_760246:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
label_760486:; 
{
int __retres1 ;
__retres1 = 1;
 __return_761034 = __retres1;
}
tmp = __return_761034;
goto label_760246;
}
else 
{
t3_st = 1;
{
t3_started();
token = token + 1;
E_4 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_760326 = __retres1;
}
tmp = __return_760326;
{
int __retres1 ;
__retres1 = 0;
 __return_760352 = __retres1;
}
tmp___0 = __return_760352;
{
int __retres1 ;
__retres1 = 0;
 __return_760378 = __retres1;
}
tmp___1 = __return_760378;
{
int __retres1 ;
__retres1 = 0;
 __return_760404 = __retres1;
}
tmp___2 = __return_760404;
{
int __retres1 ;
__retres1 = 1;
 __return_760426 = __retres1;
}
tmp___3 = __return_760426;
t4_st = 0;
}
}
E_4 = 2;
t3_pc = 1;
t3_st = 2;
}
label_760468:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_760986 = __retres1;
}
tmp = __return_760986;
goto label_760468;
}
else 
{
t4_st = 1;
{
t4_started();
token = token + 1;
E_M = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 1;
 __return_760548 = __retres1;
}
tmp = __return_760548;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_760576 = __retres1;
}
tmp___0 = __return_760576;
{
int __retres1 ;
__retres1 = 0;
 __return_760602 = __retres1;
}
tmp___1 = __return_760602;
{
int __retres1 ;
__retres1 = 0;
 __return_760628 = __retres1;
}
tmp___2 = __return_760628;
{
int __retres1 ;
__retres1 = 0;
 __return_760654 = __retres1;
}
tmp___3 = __return_760654;
}
}
E_M = 2;
t4_pc = 1;
t4_st = 2;
}
label_760694:; 
{
int __retres1 ;
__retres1 = 1;
 __return_760714 = __retres1;
}
tmp = __return_760714;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_760694;
}
else 
{
m_st = 1;
{
if (token != (local + 4))
{
{
}
goto label_760750;
}
else 
{
label_760750:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_760810 = __retres1;
}
tmp = __return_760810;
{
int __retres1 ;
__retres1 = 1;
 __return_760832 = __retres1;
}
tmp___0 = __return_760832;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_760860 = __retres1;
}
tmp___1 = __return_760860;
{
int __retres1 ;
__retres1 = 0;
 __return_760886 = __retres1;
}
tmp___2 = __return_760886;
{
int __retres1 ;
__retres1 = 0;
 __return_760912 = __retres1;
}
tmp___3 = __return_760912;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_759806;
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
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_758110 = __retres1;
}
tmp = __return_758110;
{
int __retres1 ;
__retres1 = 0;
 __return_758132 = __retres1;
}
tmp___0 = __return_758132;
{
int __retres1 ;
__retres1 = 0;
 __return_758154 = __retres1;
}
tmp___1 = __return_758154;
{
int __retres1 ;
__retres1 = 0;
 __return_758176 = __retres1;
}
tmp___2 = __return_758176;
{
int __retres1 ;
__retres1 = 0;
 __return_758198 = __retres1;
}
tmp___3 = __return_758198;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_758232:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_771234 = __retres1;
}
tmp = __return_771234;
goto label_758232;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759524:; 
{
int __retres1 ;
__retres1 = 1;
 __return_765294 = __retres1;
}
tmp = __return_765294;
label_765304:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_759524;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_762422;
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_763910;
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_764314;
}
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
label_758836:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_768376 = __retres1;
}
tmp = __return_768376;
label_768386:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_758836;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_767166;
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_767482;
}
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759540:; 
{
int __retres1 ;
__retres1 = 1;
 __return_762320 = __retres1;
}
tmp = __return_762320;
label_762330:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
label_762422:; 
goto label_759540;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_761204;
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_761542;
}
}
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_758492:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_769964 = __retres1;
}
tmp = __return_769964;
label_769974:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_758492;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_768828;
}
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759532:; 
{
int __retres1 ;
__retres1 = 1;
 __return_763852 = __retres1;
}
tmp = __return_763852;
label_763862:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_763910:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_759532;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_761212;
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_762784;
}
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
label_758844:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_767108 = __retres1;
}
tmp = __return_767108;
label_767118:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_767166:; 
goto label_758844;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_765836;
}
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759548:; 
{
int __retres1 ;
__retres1 = 1;
 __return_761144 = __retres1;
}
tmp = __return_761144;
label_761154:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_761204:; 
label_761212:; 
goto label_759548;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
label_761206:; 
label_761214:; 
goto label_759552;
}
}
}
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
label_758320:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_770312 = __retres1;
}
tmp = __return_770312;
goto label_758320;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759528:; 
{
int __retres1 ;
__retres1 = 1;
 __return_764300 = __retres1;
}
tmp = __return_764300;
label_764314:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_759528;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_761590;
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_762788;
}
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
label_758840:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_767468 = __retres1;
}
tmp = __return_767468;
label_767482:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_758840;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_765840;
}
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759544:; 
{
int __retres1 ;
__retres1 = 1;
 __return_761528 = __retres1;
}
tmp = __return_761528;
label_761542:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
label_761590:; 
goto label_759544;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_761206;
}
}
}
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_758496:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_768814 = __retres1;
}
tmp = __return_768814;
label_768828:; 
goto label_758496;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759536:; 
{
int __retres1 ;
__retres1 = 1;
 __return_762770 = __retres1;
}
tmp = __return_762770;
label_762784:; 
label_762788:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_759536;
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
goto label_761214;
}
}
}
else 
{
t3_st = 1;
{
t3_pc = 1;
t3_st = 2;
}
label_758848:; 
int tmp_ndt_5;
tmp_ndt_5 = __VERIFIER_nondet_int();
if (tmp_ndt_5 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_765822 = __retres1;
}
tmp = __return_765822;
label_765836:; 
label_765840:; 
goto label_758848;
}
else 
{
t4_st = 1;
{
t4_pc = 1;
t4_st = 2;
}
label_759552:; 
{
int __retres1 ;
__retres1 = 0;
 __return_759582 = __retres1;
}
tmp = __return_759582;
}
label_771272:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_771336;
}
else 
{
label_771336:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_771346;
}
else 
{
label_771346:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_771356;
}
else 
{
label_771356:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_771366;
}
else 
{
label_771366:; 
if (T4_E == 0)
{
T4_E = 1;
goto label_771376;
}
else 
{
label_771376:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_772116 = __retres1;
}
tmp = __return_772116;
{
int __retres1 ;
__retres1 = 0;
 __return_772142 = __retres1;
}
tmp___0 = __return_772142;
{
int __retres1 ;
__retres1 = 0;
 __return_772168 = __retres1;
}
tmp___1 = __return_772168;
{
int __retres1 ;
__retres1 = 0;
 __return_772194 = __retres1;
}
tmp___2 = __return_772194;
{
int __retres1 ;
__retres1 = 0;
 __return_772220 = __retres1;
}
tmp___3 = __return_772220;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_772240;
}
else 
{
label_772240:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_772250;
}
else 
{
label_772250:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_772260;
}
else 
{
label_772260:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_772270;
}
else 
{
label_772270:; 
if (T4_E == 1)
{
T4_E = 2;
goto label_772280;
}
else 
{
label_772280:; 
}
{
int __retres1 ;
__retres1 = 0;
 __return_772644 = __retres1;
}
tmp = __return_772644;
if (tmp == 0)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
{
int __retres1 ;
__retres1 = 0;
 __return_773196 = __retres1;
}
tmp = __return_773196;
{
int __retres1 ;
__retres1 = 0;
 __return_773222 = __retres1;
}
tmp___0 = __return_773222;
{
int __retres1 ;
__retres1 = 0;
 __return_773248 = __retres1;
}
tmp___1 = __return_773248;
{
int __retres1 ;
__retres1 = 0;
 __return_773274 = __retres1;
}
tmp___2 = __return_773274;
{
int __retres1 ;
__retres1 = 0;
 __return_773300 = __retres1;
}
tmp___3 = __return_773300;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_773320;
}
else 
{
label_773320:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_773330;
}
else 
{
label_773330:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_773340;
}
else 
{
label_773340:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_773350;
}
else 
{
label_773350:; 
if (T4_E == 1)
{
T4_E = 2;
goto label_773360;
}
else 
{
label_773360:; 
}
goto label_772670;
}
}
}
}
}
}
else 
{
label_772670:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_773662 = __retres1;
}
tmp = __return_773662;
if (tmp == 0)
{
__retres2 = 1;
goto label_773672;
}
else 
{
__retres2 = 0;
label_773672:; 
 __return_773680 = __retres2;
}
tmp___0 = __return_773680;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_774106 = __retres1;
}
tmp = __return_774106;
}
goto label_771272;
}
else 
{
}
__retres1 = 0;
 __return_774134 = __retres1;
return 1;
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
}
}
}
}
}
}
}
