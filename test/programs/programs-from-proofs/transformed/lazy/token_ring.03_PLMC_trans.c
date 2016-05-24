void t1_started();
void t2_started();
void t3_started();
void error(void);
int m_pc  =    0;
int t1_pc  =    0;
int t2_pc  =    0;
int t3_pc  =    0;
int m_st  ;
int t1_st  ;
int t2_st  ;
int t3_st  ;
int m_i  ;
int t1_i  ;
int t2_i  ;
int t3_i  ;
int M_E  =    2;
int T1_E  =    2;
int T2_E  =    2;
int T3_E  =    2;
int E_M  =    2;
int E_1  =    2;
int E_2  =    2;
int E_3  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
int is_transmit2_triggered(void) ;
int is_transmit3_triggered(void) ;
void immediate_notify(void) ;
int token  ;
int __VERIFIER_nondet_int()  ;
int local  ;
void master(void);
void transmit1(void);
void transmit2(void);
void transmit3(void);
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
int __return_467173;
int __return_467199;
int __return_467225;
int __return_467251;
int __return_467357;
int __return_467423;
int __return_467449;
int __return_467475;
int __return_467501;
int __return_468105;
int __return_477628;
int __return_477654;
int __return_477680;
int __return_477706;
int __return_478387;
int __return_478844;
int __return_478870;
int __return_478896;
int __return_478922;
int __return_479186;
int __return_479197;
int __return_480534;
int __return_480680;
int __return_480706;
int __return_480732;
int __return_480758;
int __return_480851;
int __return_480874;
int __return_472567;
int __return_470747;
int __return_477286;
int __return_477312;
int __return_477338;
int __return_477364;
int __return_478297;
int __return_474676;
int __return_470251;
int __return_474055;
int __return_471914;
int __return_476049;
int __return_468191;
int __return_477514;
int __return_477540;
int __return_477566;
int __return_477592;
int __return_478357;
int __return_468257;
int __return_468283;
int __return_468309;
int __return_468335;
int __return_468420;
int __return_468446;
int __return_468472;
int __return_468498;
int __return_468608;
int __return_468634;
int __return_468660;
int __return_468686;
int __return_468808;
int __return_468834;
int __return_468860;
int __return_468886;
int __return_468966;
int __return_469044;
int __return_469070;
int __return_469096;
int __return_469122;
int __return_469327;
int __return_469353;
int __return_469379;
int __return_469405;
int __return_469525;
int __return_469592;
int __return_469618;
int __return_469644;
int __return_469670;
int __return_469164;
int __return_469190;
int __return_469216;
int __return_469242;
int __return_469767;
int __return_477400;
int __return_477426;
int __return_477452;
int __return_477478;
int __return_478327;
int __return_478730;
int __return_478756;
int __return_478782;
int __return_478808;
int __return_479232;
int __return_479243;
int __return_480162;
int __return_480308;
int __return_480334;
int __return_480360;
int __return_480386;
int __return_480479;
int __return_469858;
int __return_469884;
int __return_469910;
int __return_469936;
int __return_470007;
int __return_470086;
int __return_470112;
int __return_470138;
int __return_470164;
int __return_472679;
int __return_472745;
int __return_472771;
int __return_472797;
int __return_472823;
int __return_472908;
int __return_472934;
int __return_472960;
int __return_472986;
int __return_473096;
int __return_473122;
int __return_473148;
int __return_473174;
int __return_473411;
int __return_476830;
int __return_476856;
int __return_476882;
int __return_476908;
int __return_478177;
int __return_478502;
int __return_478528;
int __return_478554;
int __return_478580;
int __return_479324;
int __return_479335;
int __return_479418;
int __return_479564;
int __return_479590;
int __return_479616;
int __return_479642;
int __return_479735;
int __return_480870;
int __return_473523;
int __return_473602;
int __return_473628;
int __return_473654;
int __return_473680;
int __return_473788;
int __return_473855;
int __return_473881;
int __return_473907;
int __return_473933;
int __return_470875;
int __return_477172;
int __return_477198;
int __return_477224;
int __return_477250;
int __return_478267;
int __return_470941;
int __return_470967;
int __return_470993;
int __return_471019;
int __return_471104;
int __return_471130;
int __return_471156;
int __return_471182;
int __return_471445;
int __return_477058;
int __return_477084;
int __return_477110;
int __return_477136;
int __return_478237;
int __return_478616;
int __return_478642;
int __return_478668;
int __return_478694;
int __return_479278;
int __return_479289;
int __return_479790;
int __return_479936;
int __return_479962;
int __return_479988;
int __return_480014;
int __return_480107;
int __return_480872;
int __return_471535;
int __return_476944;
int __return_476970;
int __return_476996;
int __return_477022;
int __return_478207;
int __return_471647;
int __return_471714;
int __return_471740;
int __return_471766;
int __return_471792;
int __return_474813;
int __return_474879;
int __return_474905;
int __return_474931;
int __return_474957;
int __return_475042;
int __return_475068;
int __return_475094;
int __return_475120;
int __return_475506;
int __return_475620;
int __return_475757;
int __return_475824;
int __return_475850;
int __return_475876;
int __return_475902;
int __return_470391;
int __return_470457;
int __return_470483;
int __return_470509;
int __return_470535;
int __return_474245;
int __return_474311;
int __return_474337;
int __return_474363;
int __return_474389;
int __return_472120;
int __return_472186;
int __return_472212;
int __return_472238;
int __return_472264;
int main()
{
int __retres1 ;
{
m_i = 1;
t1_i = 1;
t2_i = 1;
t3_i = 1;
}
{
int kernel_st ;
int tmp ;
int tmp___0 ;
kernel_st = 0;
{
}
{
if (m_i == 1)
{
m_st = 0;
goto label_467070;
}
else 
{
m_st = 2;
label_467070:; 
if (t1_i == 1)
{
t1_st = 0;
goto label_467077;
}
else 
{
t1_st = 2;
label_467077:; 
if (t2_i == 1)
{
t2_st = 0;
goto label_467084;
}
else 
{
t2_st = 2;
label_467084:; 
if (t3_i == 1)
{
t3_st = 0;
goto label_467091;
}
else 
{
t3_st = 2;
label_467091:; 
}
{
if (M_E == 0)
{
M_E = 1;
goto label_467103;
}
else 
{
label_467103:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_467109;
}
else 
{
label_467109:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_467115;
}
else 
{
label_467115:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_467121;
}
else 
{
label_467121:; 
if (E_M == 0)
{
E_M = 1;
goto label_467127;
}
else 
{
label_467127:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_467133;
}
else 
{
label_467133:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_467139;
}
else 
{
label_467139:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_467145;
}
else 
{
label_467145:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_467172;
}
else 
{
goto label_467167;
}
}
else 
{
label_467167:; 
__retres1 = 0;
label_467172:; 
 __return_467173 = __retres1;
}
tmp = __return_467173;
if (tmp == 0)
{
goto label_467181;
}
else 
{
m_st = 0;
label_467181:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_467198;
}
else 
{
goto label_467193;
}
}
else 
{
label_467193:; 
__retres1 = 0;
label_467198:; 
 __return_467199 = __retres1;
}
tmp___0 = __return_467199;
if (tmp___0 == 0)
{
goto label_467207;
}
else 
{
t1_st = 0;
label_467207:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_467224;
}
else 
{
goto label_467219;
}
}
else 
{
label_467219:; 
__retres1 = 0;
label_467224:; 
 __return_467225 = __retres1;
}
tmp___1 = __return_467225;
if (tmp___1 == 0)
{
goto label_467233;
}
else 
{
t2_st = 0;
label_467233:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_467250;
}
else 
{
goto label_467245;
}
}
else 
{
label_467245:; 
__retres1 = 0;
label_467250:; 
 __return_467251 = __retres1;
}
tmp___2 = __return_467251;
if (tmp___2 == 0)
{
goto label_467259;
}
else 
{
t3_st = 0;
label_467259:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_467271;
}
else 
{
label_467271:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_467277;
}
else 
{
label_467277:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_467283;
}
else 
{
label_467283:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_467289;
}
else 
{
label_467289:; 
if (E_M == 1)
{
E_M = 2;
goto label_467295;
}
else 
{
label_467295:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_467301;
}
else 
{
label_467301:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_467307;
}
else 
{
label_467307:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_467313;
}
else 
{
label_467313:; 
}
kernel_st = 1;
{
int tmp ;
label_467327:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_467356;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_467356;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_467356;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_467356;
}
else 
{
__retres1 = 0;
label_467356:; 
 __return_467357 = __retres1;
}
tmp = __return_467357;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_467526;
}
else 
{
m_st = 1;
{
if (m_pc == 0)
{
goto label_467388;
}
else 
{
if (m_pc == 1)
{
label_467390:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_467422;
}
else 
{
goto label_467417;
}
}
else 
{
label_467417:; 
__retres1 = 0;
label_467422:; 
 __return_467423 = __retres1;
}
tmp = __return_467423;
if (tmp == 0)
{
goto label_467431;
}
else 
{
m_st = 0;
label_467431:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_467448;
}
else 
{
goto label_467443;
}
}
else 
{
label_467443:; 
__retres1 = 0;
label_467448:; 
 __return_467449 = __retres1;
}
tmp___0 = __return_467449;
if (tmp___0 == 0)
{
goto label_467457;
}
else 
{
t1_st = 0;
label_467457:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_467474;
}
else 
{
goto label_467469;
}
}
else 
{
label_467469:; 
__retres1 = 0;
label_467474:; 
 __return_467475 = __retres1;
}
tmp___1 = __return_467475;
if (tmp___1 == 0)
{
goto label_467483;
}
else 
{
t2_st = 0;
label_467483:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_467500;
}
else 
{
goto label_467495;
}
}
else 
{
label_467495:; 
__retres1 = 0;
label_467500:; 
 __return_467501 = __retres1;
}
tmp___2 = __return_467501;
if (tmp___2 == 0)
{
goto label_467509;
}
else 
{
t3_st = 0;
label_467509:; 
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_467603;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_467586;
}
else 
{
label_467586:; 
t1_pc = 1;
t1_st = 2;
}
label_467595:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_467757;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_467732;
}
else 
{
label_467732:; 
t2_pc = 1;
t2_st = 2;
}
label_467741:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_468065;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_468024;
}
else 
{
label_468024:; 
t3_pc = 1;
t3_st = 2;
}
label_468033:; 
label_468075:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_468104;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_468104;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_468104;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_468104;
}
else 
{
__retres1 = 0;
label_468104:; 
 __return_468105 = __retres1;
}
tmp = __return_468105;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_468122;
}
else 
{
label_468122:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_468134;
}
else 
{
label_468134:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_468146;
}
else 
{
label_468146:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
return 1;
}
}
}
}
label_476271:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_476382;
}
else 
{
label_476382:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_476388;
}
else 
{
label_476388:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_476394;
}
else 
{
label_476394:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_476400;
}
else 
{
label_476400:; 
if (E_M == 0)
{
E_M = 1;
goto label_476406;
}
else 
{
label_476406:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_476412;
}
else 
{
label_476412:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_476418;
}
else 
{
label_476418:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_476424;
}
else 
{
label_476424:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_477627;
}
else 
{
goto label_477622;
}
}
else 
{
label_477622:; 
__retres1 = 0;
label_477627:; 
 __return_477628 = __retres1;
}
tmp = __return_477628;
if (tmp == 0)
{
goto label_477636;
}
else 
{
m_st = 0;
label_477636:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_477653;
}
else 
{
goto label_477648;
}
}
else 
{
label_477648:; 
__retres1 = 0;
label_477653:; 
 __return_477654 = __retres1;
}
tmp___0 = __return_477654;
if (tmp___0 == 0)
{
goto label_477662;
}
else 
{
t1_st = 0;
label_477662:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_477679;
}
else 
{
goto label_477674;
}
}
else 
{
label_477674:; 
__retres1 = 0;
label_477679:; 
 __return_477680 = __retres1;
}
tmp___1 = __return_477680;
if (tmp___1 == 0)
{
goto label_477688;
}
else 
{
t2_st = 0;
label_477688:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_477705;
}
else 
{
goto label_477700;
}
}
else 
{
label_477700:; 
__retres1 = 0;
label_477705:; 
 __return_477706 = __retres1;
}
tmp___2 = __return_477706;
if (tmp___2 == 0)
{
goto label_477714;
}
else 
{
t3_st = 0;
label_477714:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_477726;
}
else 
{
label_477726:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_477732;
}
else 
{
label_477732:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_477738;
}
else 
{
label_477738:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_477744;
}
else 
{
label_477744:; 
if (E_M == 1)
{
E_M = 2;
goto label_477750;
}
else 
{
label_477750:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_477756;
}
else 
{
label_477756:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_477762;
}
else 
{
label_477762:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_477768;
}
else 
{
label_477768:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_478386;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_478386;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_478386;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_478386;
}
else 
{
__retres1 = 0;
label_478386:; 
 __return_478387 = __retres1;
}
tmp = __return_478387;
kernel_st = 4;
{
M_E = 1;
}
label_478428:; 
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_478843;
}
else 
{
goto label_478838;
}
}
else 
{
label_478838:; 
__retres1 = 0;
label_478843:; 
 __return_478844 = __retres1;
}
tmp = __return_478844;
if (tmp == 0)
{
goto label_478852;
}
else 
{
m_st = 0;
label_478852:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_478869;
}
else 
{
goto label_478864;
}
}
else 
{
label_478864:; 
__retres1 = 0;
label_478869:; 
 __return_478870 = __retres1;
}
tmp___0 = __return_478870;
if (tmp___0 == 0)
{
goto label_478878;
}
else 
{
t1_st = 0;
label_478878:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_478895;
}
else 
{
goto label_478890;
}
}
else 
{
label_478890:; 
__retres1 = 0;
label_478895:; 
 __return_478896 = __retres1;
}
tmp___1 = __return_478896;
if (tmp___1 == 0)
{
goto label_478904;
}
else 
{
t2_st = 0;
label_478904:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_478921;
}
else 
{
goto label_478916;
}
}
else 
{
label_478916:; 
__retres1 = 0;
label_478921:; 
 __return_478922 = __retres1;
}
tmp___2 = __return_478922;
if (tmp___2 == 0)
{
goto label_478930;
}
else 
{
t3_st = 0;
label_478930:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_478942;
}
else 
{
label_478942:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_478948;
}
else 
{
label_478948:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_478954;
}
else 
{
label_478954:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_478960;
}
else 
{
label_478960:; 
if (E_M == 1)
{
E_M = 2;
goto label_478966;
}
else 
{
label_478966:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_478972;
}
else 
{
label_478972:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_478978;
}
else 
{
label_478978:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_478984;
}
else 
{
label_478984:; 
}
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_479185;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_479185;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_479185;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_479185;
}
else 
{
__retres1 = 0;
label_479185:; 
 __return_479186 = __retres1;
}
tmp = __return_479186;
if (tmp == 0)
{
__retres2 = 1;
goto label_479196;
}
else 
{
__retres2 = 0;
label_479196:; 
 __return_479197 = __retres2;
}
tmp___0 = __return_479197;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_480533;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_480533;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_480533;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_480533;
}
else 
{
__retres1 = 0;
label_480533:; 
 __return_480534 = __retres1;
}
tmp = __return_480534;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_480551;
}
else 
{
label_480551:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_480563;
}
else 
{
label_480563:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_480575;
}
else 
{
label_480575:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
return 1;
}
}
}
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_480610;
}
else 
{
label_480610:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_480616;
}
else 
{
label_480616:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_480622;
}
else 
{
label_480622:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_480628;
}
else 
{
label_480628:; 
if (E_M == 0)
{
E_M = 1;
goto label_480634;
}
else 
{
label_480634:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_480640;
}
else 
{
label_480640:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_480646;
}
else 
{
label_480646:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_480652;
}
else 
{
label_480652:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_480679;
}
else 
{
goto label_480674;
}
}
else 
{
label_480674:; 
__retres1 = 0;
label_480679:; 
 __return_480680 = __retres1;
}
tmp = __return_480680;
if (tmp == 0)
{
goto label_480688;
}
else 
{
m_st = 0;
label_480688:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_480705;
}
else 
{
goto label_480700;
}
}
else 
{
label_480700:; 
__retres1 = 0;
label_480705:; 
 __return_480706 = __retres1;
}
tmp___0 = __return_480706;
if (tmp___0 == 0)
{
goto label_480714;
}
else 
{
t1_st = 0;
label_480714:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_480731;
}
else 
{
goto label_480726;
}
}
else 
{
label_480726:; 
__retres1 = 0;
label_480731:; 
 __return_480732 = __retres1;
}
tmp___1 = __return_480732;
if (tmp___1 == 0)
{
goto label_480740;
}
else 
{
t2_st = 0;
label_480740:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_480757;
}
else 
{
goto label_480752;
}
}
else 
{
label_480752:; 
__retres1 = 0;
label_480757:; 
 __return_480758 = __retres1;
}
tmp___2 = __return_480758;
if (tmp___2 == 0)
{
goto label_480766;
}
else 
{
t3_st = 0;
label_480766:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_480778;
}
else 
{
label_480778:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_480784;
}
else 
{
label_480784:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_480790;
}
else 
{
label_480790:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_480796;
}
else 
{
label_480796:; 
if (E_M == 1)
{
E_M = 2;
goto label_480802;
}
else 
{
label_480802:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_480808;
}
else 
{
label_480808:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_480814;
}
else 
{
label_480814:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_480820;
}
else 
{
label_480820:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_480850;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_480850;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_480850;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_480850;
}
else 
{
__retres1 = 0;
label_480850:; 
 __return_480851 = __retres1;
}
tmp = __return_480851;
kernel_st = 4;
{
M_E = 1;
}
goto label_478428;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
}
__retres1 = 0;
 __return_480874 = __retres1;
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
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_468065:; 
label_472537:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_472566;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_472566;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_472566;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_472566;
}
else 
{
__retres1 = 0;
label_472566:; 
 __return_472567 = __retres1;
}
tmp = __return_472567;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_472584;
}
else 
{
label_472584:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_472596;
}
else 
{
label_472596:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_472608;
}
else 
{
label_472608:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_472645;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_472633;
}
else 
{
label_472633:; 
t3_pc = 1;
t3_st = 2;
}
goto label_468033;
}
}
}
else 
{
label_472645:; 
goto label_472537;
}
}
}
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
label_467757:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_468057;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_467920;
}
else 
{
label_467920:; 
t3_pc = 1;
t3_st = 2;
}
label_467929:; 
label_470717:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_470746;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_470746;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_470746;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_470746;
}
else 
{
__retres1 = 0;
label_470746:; 
 __return_470747 = __retres1;
}
tmp = __return_470747;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_470764;
}
else 
{
label_470764:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_470776;
}
else 
{
label_470776:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_470814;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_470801;
}
else 
{
label_470801:; 
t2_pc = 1;
t2_st = 2;
}
label_470810:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_470839;
}
else 
{
label_470839:; 
goto label_468075;
}
}
}
}
else 
{
label_470814:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_470837;
}
else 
{
label_470837:; 
goto label_470717;
}
}
}
}
}
label_476284:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_476544;
}
else 
{
label_476544:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_476550;
}
else 
{
label_476550:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_476556;
}
else 
{
label_476556:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_476562;
}
else 
{
label_476562:; 
if (E_M == 0)
{
E_M = 1;
goto label_476568;
}
else 
{
label_476568:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_476574;
}
else 
{
label_476574:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_476580;
}
else 
{
label_476580:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_476586;
}
else 
{
label_476586:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_477285;
}
else 
{
goto label_477280;
}
}
else 
{
label_477280:; 
__retres1 = 0;
label_477285:; 
 __return_477286 = __retres1;
}
tmp = __return_477286;
if (tmp == 0)
{
goto label_477294;
}
else 
{
m_st = 0;
label_477294:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_477311;
}
else 
{
goto label_477306;
}
}
else 
{
label_477306:; 
__retres1 = 0;
label_477311:; 
 __return_477312 = __retres1;
}
tmp___0 = __return_477312;
if (tmp___0 == 0)
{
goto label_477320;
}
else 
{
t1_st = 0;
label_477320:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_477337;
}
else 
{
goto label_477332;
}
}
else 
{
label_477332:; 
__retres1 = 0;
label_477337:; 
 __return_477338 = __retres1;
}
tmp___1 = __return_477338;
if (tmp___1 == 0)
{
goto label_477346;
}
else 
{
t2_st = 0;
label_477346:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_477363;
}
else 
{
goto label_477358;
}
}
else 
{
label_477358:; 
__retres1 = 0;
label_477363:; 
 __return_477364 = __retres1;
}
tmp___2 = __return_477364;
if (tmp___2 == 0)
{
goto label_477372;
}
else 
{
t3_st = 0;
label_477372:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_477888;
}
else 
{
label_477888:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_477894;
}
else 
{
label_477894:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_477900;
}
else 
{
label_477900:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_477906;
}
else 
{
label_477906:; 
if (E_M == 1)
{
E_M = 2;
goto label_477912;
}
else 
{
label_477912:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_477918;
}
else 
{
label_477918:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_477924;
}
else 
{
label_477924:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_477930;
}
else 
{
label_477930:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_478296;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_478296;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_478296;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_478296;
}
else 
{
__retres1 = 0;
label_478296:; 
 __return_478297 = __retres1;
}
tmp = __return_478297;
kernel_st = 4;
{
M_E = 1;
}
goto label_478428;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_468057:; 
label_474646:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_474675;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_474675;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_474675;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_474675;
}
else 
{
__retres1 = 0;
label_474675:; 
 __return_474676 = __retres1;
}
tmp = __return_474676;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_474693;
}
else 
{
label_474693:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_474705;
}
else 
{
label_474705:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_474742;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_474730;
}
else 
{
label_474730:; 
t2_pc = 1;
t2_st = 2;
}
goto label_467741;
}
}
}
else 
{
label_474742:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_474779;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_474767;
}
else 
{
label_474767:; 
t3_pc = 1;
t3_st = 2;
}
goto label_467929;
}
}
}
else 
{
label_474779:; 
goto label_474646;
}
}
}
}
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
label_467603:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_467753;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_467680;
}
else 
{
label_467680:; 
t2_pc = 1;
t2_st = 2;
}
label_467689:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_468061;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_467972;
}
else 
{
label_467972:; 
t3_pc = 1;
t3_st = 2;
}
label_467981:; 
label_470221:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_470250;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_470250;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_470250;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_470250;
}
else 
{
__retres1 = 0;
label_470250:; 
 __return_470251 = __retres1;
}
tmp = __return_470251;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_470268;
}
else 
{
label_470268:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_470306;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_470293;
}
else 
{
label_470293:; 
t1_pc = 1;
t1_st = 2;
}
label_470302:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_470331;
}
else 
{
label_470331:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_470355;
}
else 
{
label_470355:; 
goto label_468075;
}
}
}
}
}
else 
{
label_470306:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_470329;
}
else 
{
label_470329:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_470353;
}
else 
{
label_470353:; 
goto label_470221;
}
}
}
}
}
goto label_476271;
}
}
}
}
}
}
}
else 
{
label_468061:; 
label_474025:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_474054;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_474054;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_474054;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_474054;
}
else 
{
__retres1 = 0;
label_474054:; 
 __return_474055 = __retres1;
}
tmp = __return_474055;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_474072;
}
else 
{
label_474072:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_474110;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_474097;
}
else 
{
label_474097:; 
t1_pc = 1;
t1_st = 2;
}
label_474106:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_474135;
}
else 
{
label_474135:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_474209;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_474194;
}
else 
{
label_474194:; 
t3_pc = 1;
t3_st = 2;
}
goto label_468033;
}
}
}
else 
{
label_474209:; 
goto label_472537;
}
}
}
}
}
else 
{
label_474110:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_474133;
}
else 
{
label_474133:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_474207;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_474168;
}
else 
{
label_474168:; 
t3_pc = 1;
t3_st = 2;
}
goto label_467981;
}
}
}
else 
{
label_474207:; 
goto label_474025;
}
}
}
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
label_467753:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_468053;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_467868;
}
else 
{
label_467868:; 
t3_pc = 1;
t3_st = 2;
}
label_467877:; 
label_471884:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_471913;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_471913;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_471913;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_471913;
}
else 
{
__retres1 = 0;
label_471913:; 
 __return_471914 = __retres1;
}
tmp = __return_471914;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_471931;
}
else 
{
label_471931:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_471969;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_471956;
}
else 
{
label_471956:; 
t1_pc = 1;
t1_st = 2;
}
label_471965:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_472045;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_472029;
}
else 
{
label_472029:; 
t2_pc = 1;
t2_st = 2;
}
goto label_470810;
}
}
}
else 
{
label_472045:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_472080;
}
else 
{
label_472080:; 
goto label_470717;
}
}
}
}
}
else 
{
label_471969:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_472043;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_472003;
}
else 
{
label_472003:; 
t2_pc = 1;
t2_st = 2;
}
label_472012:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_472082;
}
else 
{
label_472082:; 
goto label_470221;
}
}
}
}
else 
{
label_472043:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_472078;
}
else 
{
label_472078:; 
goto label_471884;
}
}
}
}
}
goto label_476284;
}
}
}
}
}
}
}
else 
{
label_468053:; 
label_476019:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_476048;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_476048;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_476048;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_476048;
}
else 
{
__retres1 = 0;
label_476048:; 
 __return_476049 = __retres1;
}
tmp = __return_476049;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_476066;
}
else 
{
label_476066:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_476103;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_476091;
}
else 
{
label_476091:; 
t1_pc = 1;
t1_st = 2;
}
goto label_467595;
}
}
}
else 
{
label_476103:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_476140;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_476128;
}
else 
{
label_476128:; 
t2_pc = 1;
t2_st = 2;
}
goto label_467689;
}
}
}
else 
{
label_476140:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_476177;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_476165;
}
else 
{
label_476165:; 
t3_pc = 1;
t3_st = 2;
}
goto label_467877;
}
}
}
else 
{
label_476177:; 
goto label_476019;
}
}
}
}
}
}
}
}
}
}
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
label_467388:; 
goto label_467390;
}
}
}
}
}
else 
{
label_467526:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_467601;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_467560;
}
else 
{
label_467560:; 
t1_pc = 1;
t1_st = 2;
}
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_467755;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_467706;
}
else 
{
label_467706:; 
t2_pc = 1;
t2_st = 2;
}
label_467715:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_468063;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_467998;
}
else 
{
label_467998:; 
t3_pc = 1;
t3_st = 2;
}
label_468007:; 
label_468161:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_468190;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_468190;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_468190;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_468190;
}
else 
{
__retres1 = 0;
label_468190:; 
 __return_468191 = __retres1;
}
tmp = __return_468191;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_468360;
}
else 
{
m_st = 1;
{
if (m_pc == 0)
{
goto label_468222;
}
else 
{
if (m_pc == 1)
{
label_468224:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_468256;
}
else 
{
goto label_468251;
}
}
else 
{
label_468251:; 
__retres1 = 0;
label_468256:; 
 __return_468257 = __retres1;
}
tmp = __return_468257;
if (tmp == 0)
{
goto label_468265;
}
else 
{
m_st = 0;
label_468265:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_468282;
}
else 
{
goto label_468277;
}
}
else 
{
label_468277:; 
__retres1 = 0;
label_468282:; 
 __return_468283 = __retres1;
}
tmp___0 = __return_468283;
if (tmp___0 == 0)
{
goto label_468291;
}
else 
{
t1_st = 0;
label_468291:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_468308;
}
else 
{
goto label_468303;
}
}
else 
{
label_468303:; 
__retres1 = 0;
label_468308:; 
 __return_468309 = __retres1;
}
tmp___1 = __return_468309;
if (tmp___1 == 0)
{
goto label_468317;
}
else 
{
t2_st = 0;
label_468317:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_468334;
}
else 
{
goto label_468329;
}
}
else 
{
label_468329:; 
__retres1 = 0;
label_468334:; 
 __return_468335 = __retres1;
}
tmp___2 = __return_468335;
if (tmp___2 == 0)
{
goto label_468343;
}
else 
{
t3_st = 0;
label_468343:; 
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_468539;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_468419;
}
else 
{
goto label_468414;
}
}
else 
{
label_468414:; 
__retres1 = 0;
label_468419:; 
 __return_468420 = __retres1;
}
tmp = __return_468420;
if (tmp == 0)
{
goto label_468428;
}
else 
{
m_st = 0;
label_468428:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_468445;
}
else 
{
goto label_468440;
}
}
else 
{
label_468440:; 
__retres1 = 0;
label_468445:; 
 __return_468446 = __retres1;
}
tmp___0 = __return_468446;
if (tmp___0 == 0)
{
goto label_468454;
}
else 
{
t1_st = 0;
label_468454:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_468471;
}
else 
{
goto label_468466;
}
}
else 
{
label_468466:; 
__retres1 = 0;
label_468471:; 
 __return_468472 = __retres1;
}
tmp___1 = __return_468472;
if (tmp___1 == 0)
{
goto label_468480;
}
else 
{
t2_st = 0;
label_468480:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_468497;
}
else 
{
goto label_468492;
}
}
else 
{
label_468492:; 
__retres1 = 0;
label_468497:; 
 __return_468498 = __retres1;
}
tmp___2 = __return_468498;
if (tmp___2 == 0)
{
goto label_468506;
}
else 
{
t3_st = 0;
label_468506:; 
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_468532:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_468730;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_468607;
}
else 
{
goto label_468602;
}
}
else 
{
label_468602:; 
__retres1 = 0;
label_468607:; 
 __return_468608 = __retres1;
}
tmp = __return_468608;
if (tmp == 0)
{
goto label_468616;
}
else 
{
m_st = 0;
label_468616:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_468633;
}
else 
{
goto label_468628;
}
}
else 
{
label_468628:; 
__retres1 = 0;
label_468633:; 
 __return_468634 = __retres1;
}
tmp___0 = __return_468634;
if (tmp___0 == 0)
{
goto label_468642;
}
else 
{
t1_st = 0;
label_468642:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_468659;
}
else 
{
goto label_468654;
}
}
else 
{
label_468654:; 
__retres1 = 0;
label_468659:; 
 __return_468660 = __retres1;
}
tmp___1 = __return_468660;
if (tmp___1 == 0)
{
goto label_468668;
}
else 
{
t2_st = 0;
label_468668:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_468685;
}
else 
{
goto label_468680;
}
}
else 
{
label_468680:; 
__retres1 = 0;
label_468685:; 
 __return_468686 = __retres1;
}
tmp___2 = __return_468686;
if (tmp___2 == 0)
{
goto label_468694;
}
else 
{
t3_st = 0;
label_468694:; 
}
}
E_3 = 2;
t2_pc = 1;
t2_st = 2;
}
label_468720:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_468933;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
return 1;
}
else 
{
t3_started();
token = token + 1;
E_M = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_468807;
}
else 
{
goto label_468802;
}
}
else 
{
label_468802:; 
__retres1 = 0;
label_468807:; 
 __return_468808 = __retres1;
}
tmp = __return_468808;
if (tmp == 0)
{
goto label_468816;
}
else 
{
m_st = 0;
label_468816:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_468833;
}
else 
{
goto label_468828;
}
}
else 
{
label_468828:; 
__retres1 = 0;
label_468833:; 
 __return_468834 = __retres1;
}
tmp___0 = __return_468834;
if (tmp___0 == 0)
{
goto label_468842;
}
else 
{
t1_st = 0;
label_468842:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_468859;
}
else 
{
goto label_468854;
}
}
else 
{
label_468854:; 
__retres1 = 0;
label_468859:; 
 __return_468860 = __retres1;
}
tmp___1 = __return_468860;
if (tmp___1 == 0)
{
goto label_468868;
}
else 
{
t2_st = 0;
label_468868:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_468885;
}
else 
{
goto label_468880;
}
}
else 
{
label_468880:; 
__retres1 = 0;
label_468885:; 
 __return_468886 = __retres1;
}
tmp___2 = __return_468886;
if (tmp___2 == 0)
{
goto label_468894;
}
else 
{
t3_st = 0;
label_468894:; 
}
}
E_M = 2;
t3_pc = 1;
t3_st = 2;
}
label_468920:; 
label_468936:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_468965;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_468965;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_468965;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_468965;
}
else 
{
__retres1 = 0;
label_468965:; 
 __return_468966 = __retres1;
}
tmp = __return_468966;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_469267;
}
else 
{
m_st = 1;
{
if (m_pc == 0)
{
goto label_469004;
}
else 
{
if (m_pc == 1)
{
if (token != (local + 3))
{
{
}
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_469043;
}
else 
{
goto label_469038;
}
}
else 
{
label_469038:; 
__retres1 = 0;
label_469043:; 
 __return_469044 = __retres1;
}
tmp = __return_469044;
if (tmp == 0)
{
goto label_469052;
}
else 
{
m_st = 0;
label_469052:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_469069;
}
else 
{
goto label_469064;
}
}
else 
{
label_469064:; 
__retres1 = 0;
label_469069:; 
 __return_469070 = __retres1;
}
tmp___0 = __return_469070;
if (tmp___0 == 0)
{
goto label_469078;
}
else 
{
t1_st = 0;
label_469078:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_469095;
}
else 
{
goto label_469090;
}
}
else 
{
label_469090:; 
__retres1 = 0;
label_469095:; 
 __return_469096 = __retres1;
}
tmp___1 = __return_469096;
if (tmp___1 == 0)
{
goto label_469104;
}
else 
{
t2_st = 0;
label_469104:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_469121;
}
else 
{
goto label_469116;
}
}
else 
{
label_469116:; 
__retres1 = 0;
label_469121:; 
 __return_469122 = __retres1;
}
tmp___2 = __return_469122;
if (tmp___2 == 0)
{
goto label_469130;
}
else 
{
t3_st = 0;
label_469130:; 
}
}
label_469136:; 
E_1 = 2;
m_pc = 1;
m_st = 2;
}
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_469445;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_469326;
}
else 
{
goto label_469321;
}
}
else 
{
label_469321:; 
__retres1 = 0;
label_469326:; 
 __return_469327 = __retres1;
}
tmp = __return_469327;
if (tmp == 0)
{
goto label_469335;
}
else 
{
m_st = 0;
label_469335:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_469352;
}
else 
{
goto label_469347;
}
}
else 
{
label_469347:; 
__retres1 = 0;
label_469352:; 
 __return_469353 = __retres1;
}
tmp___0 = __return_469353;
if (tmp___0 == 0)
{
goto label_469361;
}
else 
{
t1_st = 0;
label_469361:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_469378;
}
else 
{
goto label_469373;
}
}
else 
{
label_469373:; 
__retres1 = 0;
label_469378:; 
 __return_469379 = __retres1;
}
tmp___1 = __return_469379;
if (tmp___1 == 0)
{
goto label_469387;
}
else 
{
t2_st = 0;
label_469387:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_469404;
}
else 
{
goto label_469399;
}
}
else 
{
label_469399:; 
__retres1 = 0;
label_469404:; 
 __return_469405 = __retres1;
}
tmp___2 = __return_469405;
if (tmp___2 == 0)
{
goto label_469413;
}
else 
{
t3_st = 0;
label_469413:; 
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
goto label_468532;
}
}
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
label_469445:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_469469;
}
else 
{
label_469469:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_469493;
}
else 
{
label_469493:; 
label_469495:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_469524;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_469524;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_469524;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_469524;
}
else 
{
__retres1 = 0;
label_469524:; 
 __return_469525 = __retres1;
}
tmp = __return_469525;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_469542;
}
else 
{
label_469542:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_469707;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_469591;
}
else 
{
goto label_469586;
}
}
else 
{
label_469586:; 
__retres1 = 0;
label_469591:; 
 __return_469592 = __retres1;
}
tmp = __return_469592;
if (tmp == 0)
{
goto label_469600;
}
else 
{
m_st = 0;
label_469600:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_469617;
}
else 
{
goto label_469612;
}
}
else 
{
label_469612:; 
__retres1 = 0;
label_469617:; 
 __return_469618 = __retres1;
}
tmp___0 = __return_469618;
if (tmp___0 == 0)
{
goto label_469626;
}
else 
{
t1_st = 0;
label_469626:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_469643;
}
else 
{
goto label_469638;
}
}
else 
{
label_469638:; 
__retres1 = 0;
label_469643:; 
 __return_469644 = __retres1;
}
tmp___1 = __return_469644;
if (tmp___1 == 0)
{
goto label_469652;
}
else 
{
t2_st = 0;
label_469652:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_469669;
}
else 
{
goto label_469664;
}
}
else 
{
label_469664:; 
__retres1 = 0;
label_469669:; 
 __return_469670 = __retres1;
}
tmp___2 = __return_469670;
if (tmp___2 == 0)
{
goto label_469678;
}
else 
{
t3_st = 0;
label_469678:; 
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
goto label_468532;
}
}
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
label_469707:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_469719;
}
else 
{
label_469719:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_469731;
}
else 
{
label_469731:; 
goto label_469495;
}
}
}
}
}
}
}
}
}
}
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
label_469006:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_469163;
}
else 
{
goto label_469158;
}
}
else 
{
label_469158:; 
__retres1 = 0;
label_469163:; 
 __return_469164 = __retres1;
}
tmp = __return_469164;
if (tmp == 0)
{
goto label_469172;
}
else 
{
m_st = 0;
label_469172:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_469189;
}
else 
{
goto label_469184;
}
}
else 
{
label_469184:; 
__retres1 = 0;
label_469189:; 
 __return_469190 = __retres1;
}
tmp___0 = __return_469190;
if (tmp___0 == 0)
{
goto label_469198;
}
else 
{
t1_st = 0;
label_469198:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_469215;
}
else 
{
goto label_469210;
}
}
else 
{
label_469210:; 
__retres1 = 0;
label_469215:; 
 __return_469216 = __retres1;
}
tmp___1 = __return_469216;
if (tmp___1 == 0)
{
goto label_469224;
}
else 
{
t2_st = 0;
label_469224:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_469241;
}
else 
{
goto label_469236;
}
}
else 
{
label_469236:; 
__retres1 = 0;
label_469241:; 
 __return_469242 = __retres1;
}
tmp___2 = __return_469242;
if (tmp___2 == 0)
{
goto label_469250;
}
else 
{
t3_st = 0;
label_469250:; 
}
}
goto label_469136;
}
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
label_469004:; 
goto label_469006;
}
}
}
}
}
else 
{
label_469267:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_469443;
}
else 
{
label_469443:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_469467;
}
else 
{
label_469467:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_469491;
}
else 
{
label_469491:; 
goto label_468936;
}
}
}
}
}
}
}
}
}
}
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
label_468933:; 
label_469737:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_469766;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_469766;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_469766;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_469766;
}
else 
{
__retres1 = 0;
label_469766:; 
 __return_469767 = __retres1;
}
tmp = __return_469767;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_469784;
}
else 
{
label_469784:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_469796;
}
else 
{
label_469796:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_469808;
}
else 
{
label_469808:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_469973;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
return 1;
}
else 
{
t3_started();
token = token + 1;
E_M = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_469857;
}
else 
{
goto label_469852;
}
}
else 
{
label_469852:; 
__retres1 = 0;
label_469857:; 
 __return_469858 = __retres1;
}
tmp = __return_469858;
if (tmp == 0)
{
goto label_469866;
}
else 
{
m_st = 0;
label_469866:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_469883;
}
else 
{
goto label_469878;
}
}
else 
{
label_469878:; 
__retres1 = 0;
label_469883:; 
 __return_469884 = __retres1;
}
tmp___0 = __return_469884;
if (tmp___0 == 0)
{
goto label_469892;
}
else 
{
t1_st = 0;
label_469892:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_469909;
}
else 
{
goto label_469904;
}
}
else 
{
label_469904:; 
__retres1 = 0;
label_469909:; 
 __return_469910 = __retres1;
}
tmp___1 = __return_469910;
if (tmp___1 == 0)
{
goto label_469918;
}
else 
{
t2_st = 0;
label_469918:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_469935;
}
else 
{
goto label_469930;
}
}
else 
{
label_469930:; 
__retres1 = 0;
label_469935:; 
 __return_469936 = __retres1;
}
tmp___2 = __return_469936;
if (tmp___2 == 0)
{
goto label_469944;
}
else 
{
t3_st = 0;
label_469944:; 
}
}
E_M = 2;
t3_pc = 1;
t3_st = 2;
}
goto label_468920;
}
}
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
label_469973:; 
goto label_469737;
}
}
}
}
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_476490;
}
else 
{
label_476490:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_476496;
}
else 
{
label_476496:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_476502;
}
else 
{
label_476502:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_476508;
}
else 
{
label_476508:; 
if (E_M == 0)
{
E_M = 1;
goto label_476514;
}
else 
{
label_476514:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_476520;
}
else 
{
label_476520:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_476526;
}
else 
{
label_476526:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_476532;
}
else 
{
label_476532:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_477399;
}
else 
{
goto label_477394;
}
}
else 
{
label_477394:; 
__retres1 = 0;
label_477399:; 
 __return_477400 = __retres1;
}
tmp = __return_477400;
if (tmp == 0)
{
goto label_477408;
}
else 
{
m_st = 0;
label_477408:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_477425;
}
else 
{
goto label_477420;
}
}
else 
{
label_477420:; 
__retres1 = 0;
label_477425:; 
 __return_477426 = __retres1;
}
tmp___0 = __return_477426;
if (tmp___0 == 0)
{
goto label_477434;
}
else 
{
t1_st = 0;
label_477434:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_477451;
}
else 
{
goto label_477446;
}
}
else 
{
label_477446:; 
__retres1 = 0;
label_477451:; 
 __return_477452 = __retres1;
}
tmp___1 = __return_477452;
if (tmp___1 == 0)
{
goto label_477460;
}
else 
{
t2_st = 0;
label_477460:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_477477;
}
else 
{
goto label_477472;
}
}
else 
{
label_477472:; 
__retres1 = 0;
label_477477:; 
 __return_477478 = __retres1;
}
tmp___2 = __return_477478;
if (tmp___2 == 0)
{
goto label_477486;
}
else 
{
t3_st = 0;
label_477486:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_477834;
}
else 
{
label_477834:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_477840;
}
else 
{
label_477840:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_477846;
}
else 
{
label_477846:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_477852;
}
else 
{
label_477852:; 
if (E_M == 1)
{
E_M = 2;
goto label_477858;
}
else 
{
label_477858:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_477864;
}
else 
{
label_477864:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_477870;
}
else 
{
label_477870:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_477876;
}
else 
{
label_477876:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_478326;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_478326;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_478326;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_478326;
}
else 
{
__retres1 = 0;
label_478326:; 
 __return_478327 = __retres1;
}
tmp = __return_478327;
kernel_st = 4;
{
M_E = 1;
}
label_478442:; 
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_478729;
}
else 
{
goto label_478724;
}
}
else 
{
label_478724:; 
__retres1 = 0;
label_478729:; 
 __return_478730 = __retres1;
}
tmp = __return_478730;
if (tmp == 0)
{
goto label_478738;
}
else 
{
m_st = 0;
label_478738:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_478755;
}
else 
{
goto label_478750;
}
}
else 
{
label_478750:; 
__retres1 = 0;
label_478755:; 
 __return_478756 = __retres1;
}
tmp___0 = __return_478756;
if (tmp___0 == 0)
{
goto label_478764;
}
else 
{
t1_st = 0;
label_478764:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_478781;
}
else 
{
goto label_478776;
}
}
else 
{
label_478776:; 
__retres1 = 0;
label_478781:; 
 __return_478782 = __retres1;
}
tmp___1 = __return_478782;
if (tmp___1 == 0)
{
goto label_478790;
}
else 
{
t2_st = 0;
label_478790:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_478807;
}
else 
{
goto label_478802;
}
}
else 
{
label_478802:; 
__retres1 = 0;
label_478807:; 
 __return_478808 = __retres1;
}
tmp___2 = __return_478808;
if (tmp___2 == 0)
{
goto label_478816;
}
else 
{
t3_st = 0;
label_478816:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_478996;
}
else 
{
label_478996:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_479002;
}
else 
{
label_479002:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_479008;
}
else 
{
label_479008:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_479014;
}
else 
{
label_479014:; 
if (E_M == 1)
{
E_M = 2;
goto label_479020;
}
else 
{
label_479020:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_479026;
}
else 
{
label_479026:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_479032;
}
else 
{
label_479032:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_479038;
}
else 
{
label_479038:; 
}
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_479231;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_479231;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_479231;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_479231;
}
else 
{
__retres1 = 0;
label_479231:; 
 __return_479232 = __retres1;
}
tmp = __return_479232;
if (tmp == 0)
{
__retres2 = 1;
goto label_479242;
}
else 
{
__retres2 = 0;
label_479242:; 
 __return_479243 = __retres2;
}
tmp___0 = __return_479243;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_480161;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_480161;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_480161;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_480161;
}
else 
{
__retres1 = 0;
label_480161:; 
 __return_480162 = __retres1;
}
tmp = __return_480162;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_480179;
}
else 
{
label_480179:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_480191;
}
else 
{
label_480191:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_480203;
}
else 
{
label_480203:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
return 1;
}
}
}
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_480238;
}
else 
{
label_480238:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_480244;
}
else 
{
label_480244:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_480250;
}
else 
{
label_480250:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_480256;
}
else 
{
label_480256:; 
if (E_M == 0)
{
E_M = 1;
goto label_480262;
}
else 
{
label_480262:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_480268;
}
else 
{
label_480268:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_480274;
}
else 
{
label_480274:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_480280;
}
else 
{
label_480280:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_480307;
}
else 
{
goto label_480302;
}
}
else 
{
label_480302:; 
__retres1 = 0;
label_480307:; 
 __return_480308 = __retres1;
}
tmp = __return_480308;
if (tmp == 0)
{
goto label_480316;
}
else 
{
m_st = 0;
label_480316:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_480333;
}
else 
{
goto label_480328;
}
}
else 
{
label_480328:; 
__retres1 = 0;
label_480333:; 
 __return_480334 = __retres1;
}
tmp___0 = __return_480334;
if (tmp___0 == 0)
{
goto label_480342;
}
else 
{
t1_st = 0;
label_480342:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_480359;
}
else 
{
goto label_480354;
}
}
else 
{
label_480354:; 
__retres1 = 0;
label_480359:; 
 __return_480360 = __retres1;
}
tmp___1 = __return_480360;
if (tmp___1 == 0)
{
goto label_480368;
}
else 
{
t2_st = 0;
label_480368:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_480385;
}
else 
{
goto label_480380;
}
}
else 
{
label_480380:; 
__retres1 = 0;
label_480385:; 
 __return_480386 = __retres1;
}
tmp___2 = __return_480386;
if (tmp___2 == 0)
{
goto label_480394;
}
else 
{
t3_st = 0;
label_480394:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_480406;
}
else 
{
label_480406:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_480412;
}
else 
{
label_480412:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_480418;
}
else 
{
label_480418:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_480424;
}
else 
{
label_480424:; 
if (E_M == 1)
{
E_M = 2;
goto label_480430;
}
else 
{
label_480430:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_480436;
}
else 
{
label_480436:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_480442;
}
else 
{
label_480442:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_480448;
}
else 
{
label_480448:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_480478;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_480478;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_480478;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_480478;
}
else 
{
__retres1 = 0;
label_480478:; 
 __return_480479 = __retres1;
}
tmp = __return_480479;
kernel_st = 4;
{
M_E = 1;
}
goto label_478442;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
}
goto label_479367;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_468730:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_468931;
}
else 
{
label_468931:; 
label_469977:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_470006;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_470006;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_470006;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_470006;
}
else 
{
__retres1 = 0;
label_470006:; 
 __return_470007 = __retres1;
}
tmp = __return_470007;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_470024;
}
else 
{
label_470024:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_470036;
}
else 
{
label_470036:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_470201;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_470085;
}
else 
{
goto label_470080;
}
}
else 
{
label_470080:; 
__retres1 = 0;
label_470085:; 
 __return_470086 = __retres1;
}
tmp = __return_470086;
if (tmp == 0)
{
goto label_470094;
}
else 
{
m_st = 0;
label_470094:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_470111;
}
else 
{
goto label_470106;
}
}
else 
{
label_470106:; 
__retres1 = 0;
label_470111:; 
 __return_470112 = __retres1;
}
tmp___0 = __return_470112;
if (tmp___0 == 0)
{
goto label_470120;
}
else 
{
t1_st = 0;
label_470120:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_470137;
}
else 
{
goto label_470132;
}
}
else 
{
label_470132:; 
__retres1 = 0;
label_470137:; 
 __return_470138 = __retres1;
}
tmp___1 = __return_470138;
if (tmp___1 == 0)
{
goto label_470146;
}
else 
{
t2_st = 0;
label_470146:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_470163;
}
else 
{
goto label_470158;
}
}
else 
{
label_470158:; 
__retres1 = 0;
label_470163:; 
 __return_470164 = __retres1;
}
tmp___2 = __return_470164;
if (tmp___2 == 0)
{
goto label_470172;
}
else 
{
t3_st = 0;
label_470172:; 
}
}
E_3 = 2;
t2_pc = 1;
t2_st = 2;
}
goto label_468720;
}
}
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
label_470201:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_470213;
}
else 
{
label_470213:; 
goto label_469977;
}
}
}
}
}
}
}
}
}
}
}
}
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
label_468539:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_468728;
}
else 
{
label_468728:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_468929;
}
else 
{
label_468929:; 
goto label_469495;
}
}
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
label_468222:; 
goto label_468224;
}
}
}
}
}
else 
{
label_468360:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_468537;
}
else 
{
label_468537:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_468726;
}
else 
{
label_468726:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_468927;
}
else 
{
label_468927:; 
goto label_468161;
}
}
}
}
}
label_476273:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_476436;
}
else 
{
label_476436:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_476442;
}
else 
{
label_476442:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_476448;
}
else 
{
label_476448:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_476454;
}
else 
{
label_476454:; 
if (E_M == 0)
{
E_M = 1;
goto label_476460;
}
else 
{
label_476460:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_476466;
}
else 
{
label_476466:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_476472;
}
else 
{
label_476472:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_476478;
}
else 
{
label_476478:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_477513;
}
else 
{
goto label_477508;
}
}
else 
{
label_477508:; 
__retres1 = 0;
label_477513:; 
 __return_477514 = __retres1;
}
tmp = __return_477514;
if (tmp == 0)
{
goto label_477522;
}
else 
{
m_st = 0;
label_477522:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_477539;
}
else 
{
goto label_477534;
}
}
else 
{
label_477534:; 
__retres1 = 0;
label_477539:; 
 __return_477540 = __retres1;
}
tmp___0 = __return_477540;
if (tmp___0 == 0)
{
goto label_477548;
}
else 
{
t1_st = 0;
label_477548:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_477565;
}
else 
{
goto label_477560;
}
}
else 
{
label_477560:; 
__retres1 = 0;
label_477565:; 
 __return_477566 = __retres1;
}
tmp___1 = __return_477566;
if (tmp___1 == 0)
{
goto label_477574;
}
else 
{
t2_st = 0;
label_477574:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_477591;
}
else 
{
goto label_477586;
}
}
else 
{
label_477586:; 
__retres1 = 0;
label_477591:; 
 __return_477592 = __retres1;
}
tmp___2 = __return_477592;
if (tmp___2 == 0)
{
goto label_477600;
}
else 
{
t3_st = 0;
label_477600:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_477780;
}
else 
{
label_477780:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_477786;
}
else 
{
label_477786:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_477792;
}
else 
{
label_477792:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_477798;
}
else 
{
label_477798:; 
if (E_M == 1)
{
E_M = 2;
goto label_477804;
}
else 
{
label_477804:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_477810;
}
else 
{
label_477810:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_477816;
}
else 
{
label_477816:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_477822;
}
else 
{
label_477822:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_478356;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_478356;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_478356;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_478356;
}
else 
{
__retres1 = 0;
label_478356:; 
 __return_478357 = __retres1;
}
tmp = __return_478357;
kernel_st = 4;
{
M_E = 1;
}
goto label_478428;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_468063:; 
label_472649:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_472678;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_472678;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_472678;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_472678;
}
else 
{
__retres1 = 0;
label_472678:; 
 __return_472679 = __retres1;
}
tmp = __return_472679;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_472848;
}
else 
{
m_st = 1;
{
if (m_pc == 0)
{
goto label_472710;
}
else 
{
if (m_pc == 1)
{
label_472712:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_472744;
}
else 
{
goto label_472739;
}
}
else 
{
label_472739:; 
__retres1 = 0;
label_472744:; 
 __return_472745 = __retres1;
}
tmp = __return_472745;
if (tmp == 0)
{
goto label_472753;
}
else 
{
m_st = 0;
label_472753:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_472770;
}
else 
{
goto label_472765;
}
}
else 
{
label_472765:; 
__retres1 = 0;
label_472770:; 
 __return_472771 = __retres1;
}
tmp___0 = __return_472771;
if (tmp___0 == 0)
{
goto label_472779;
}
else 
{
t1_st = 0;
label_472779:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_472796;
}
else 
{
goto label_472791;
}
}
else 
{
label_472791:; 
__retres1 = 0;
label_472796:; 
 __return_472797 = __retres1;
}
tmp___1 = __return_472797;
if (tmp___1 == 0)
{
goto label_472805;
}
else 
{
t2_st = 0;
label_472805:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_472822;
}
else 
{
goto label_472817;
}
}
else 
{
label_472817:; 
__retres1 = 0;
label_472822:; 
 __return_472823 = __retres1;
}
tmp___2 = __return_472823;
if (tmp___2 == 0)
{
goto label_472831;
}
else 
{
t3_st = 0;
label_472831:; 
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_473027;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_472907;
}
else 
{
goto label_472902;
}
}
else 
{
label_472902:; 
__retres1 = 0;
label_472907:; 
 __return_472908 = __retres1;
}
tmp = __return_472908;
if (tmp == 0)
{
goto label_472916;
}
else 
{
m_st = 0;
label_472916:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_472933;
}
else 
{
goto label_472928;
}
}
else 
{
label_472928:; 
__retres1 = 0;
label_472933:; 
 __return_472934 = __retres1;
}
tmp___0 = __return_472934;
if (tmp___0 == 0)
{
goto label_472942;
}
else 
{
t1_st = 0;
label_472942:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_472959;
}
else 
{
goto label_472954;
}
}
else 
{
label_472954:; 
__retres1 = 0;
label_472959:; 
 __return_472960 = __retres1;
}
tmp___1 = __return_472960;
if (tmp___1 == 0)
{
goto label_472968;
}
else 
{
t2_st = 0;
label_472968:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_472985;
}
else 
{
goto label_472980;
}
}
else 
{
label_472980:; 
__retres1 = 0;
label_472985:; 
 __return_472986 = __retres1;
}
tmp___2 = __return_472986;
if (tmp___2 == 0)
{
goto label_472994;
}
else 
{
t3_st = 0;
label_472994:; 
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_473020:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_473218;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_473095;
}
else 
{
goto label_473090;
}
}
else 
{
label_473090:; 
__retres1 = 0;
label_473095:; 
 __return_473096 = __retres1;
}
tmp = __return_473096;
if (tmp == 0)
{
goto label_473104;
}
else 
{
m_st = 0;
label_473104:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_473121;
}
else 
{
goto label_473116;
}
}
else 
{
label_473116:; 
__retres1 = 0;
label_473121:; 
 __return_473122 = __retres1;
}
tmp___0 = __return_473122;
if (tmp___0 == 0)
{
goto label_473130;
}
else 
{
t1_st = 0;
label_473130:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_473147;
}
else 
{
goto label_473142;
}
}
else 
{
label_473142:; 
__retres1 = 0;
label_473147:; 
 __return_473148 = __retres1;
}
tmp___1 = __return_473148;
if (tmp___1 == 0)
{
goto label_473156;
}
else 
{
t2_st = 0;
label_473156:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_473173;
}
else 
{
goto label_473168;
}
}
else 
{
label_473168:; 
__retres1 = 0;
label_473173:; 
 __return_473174 = __retres1;
}
tmp___2 = __return_473174;
if (tmp___2 == 0)
{
goto label_473182;
}
else 
{
t3_st = 0;
label_473182:; 
}
}
E_3 = 2;
t2_pc = 1;
t2_st = 2;
}
label_473208:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_473370;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_473346;
}
else 
{
label_473346:; 
t3_pc = 1;
t3_st = 2;
}
label_473355:; 
goto label_469737;
}
}
}
else 
{
label_473370:; 
label_473381:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_473410;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_473410;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_473410;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_473410;
}
else 
{
__retres1 = 0;
label_473410:; 
 __return_473411 = __retres1;
}
tmp = __return_473411;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_473428;
}
else 
{
label_473428:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_473440;
}
else 
{
label_473440:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_473452;
}
else 
{
label_473452:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_473489;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_473477;
}
else 
{
label_473477:; 
t3_pc = 1;
t3_st = 2;
}
goto label_473355;
}
}
}
else 
{
label_473489:; 
goto label_473381;
}
}
}
}
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_476760;
}
else 
{
label_476760:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_476766;
}
else 
{
label_476766:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_476772;
}
else 
{
label_476772:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_476778;
}
else 
{
label_476778:; 
if (E_M == 0)
{
E_M = 1;
goto label_476784;
}
else 
{
label_476784:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_476790;
}
else 
{
label_476790:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_476796;
}
else 
{
label_476796:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_476802;
}
else 
{
label_476802:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_476829;
}
else 
{
goto label_476824;
}
}
else 
{
label_476824:; 
__retres1 = 0;
label_476829:; 
 __return_476830 = __retres1;
}
tmp = __return_476830;
if (tmp == 0)
{
goto label_476838;
}
else 
{
m_st = 0;
label_476838:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_476855;
}
else 
{
goto label_476850;
}
}
else 
{
label_476850:; 
__retres1 = 0;
label_476855:; 
 __return_476856 = __retres1;
}
tmp___0 = __return_476856;
if (tmp___0 == 0)
{
goto label_476864;
}
else 
{
t1_st = 0;
label_476864:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_476881;
}
else 
{
goto label_476876;
}
}
else 
{
label_476876:; 
__retres1 = 0;
label_476881:; 
 __return_476882 = __retres1;
}
tmp___1 = __return_476882;
if (tmp___1 == 0)
{
goto label_476890;
}
else 
{
t2_st = 0;
label_476890:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_476907;
}
else 
{
goto label_476902;
}
}
else 
{
label_476902:; 
__retres1 = 0;
label_476907:; 
 __return_476908 = __retres1;
}
tmp___2 = __return_476908;
if (tmp___2 == 0)
{
goto label_476916;
}
else 
{
t3_st = 0;
label_476916:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_478104;
}
else 
{
label_478104:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_478110;
}
else 
{
label_478110:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_478116;
}
else 
{
label_478116:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_478122;
}
else 
{
label_478122:; 
if (E_M == 1)
{
E_M = 2;
goto label_478128;
}
else 
{
label_478128:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_478134;
}
else 
{
label_478134:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_478140;
}
else 
{
label_478140:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_478146;
}
else 
{
label_478146:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_478176;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_478176;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_478176;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_478176;
}
else 
{
__retres1 = 0;
label_478176:; 
 __return_478177 = __retres1;
}
tmp = __return_478177;
kernel_st = 4;
{
M_E = 1;
}
label_478477:; 
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_478501;
}
else 
{
goto label_478496;
}
}
else 
{
label_478496:; 
__retres1 = 0;
label_478501:; 
 __return_478502 = __retres1;
}
tmp = __return_478502;
if (tmp == 0)
{
goto label_478510;
}
else 
{
m_st = 0;
label_478510:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_478527;
}
else 
{
goto label_478522;
}
}
else 
{
label_478522:; 
__retres1 = 0;
label_478527:; 
 __return_478528 = __retres1;
}
tmp___0 = __return_478528;
if (tmp___0 == 0)
{
goto label_478536;
}
else 
{
t1_st = 0;
label_478536:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_478553;
}
else 
{
goto label_478548;
}
}
else 
{
label_478548:; 
__retres1 = 0;
label_478553:; 
 __return_478554 = __retres1;
}
tmp___1 = __return_478554;
if (tmp___1 == 0)
{
goto label_478562;
}
else 
{
t2_st = 0;
label_478562:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_478579;
}
else 
{
goto label_478574;
}
}
else 
{
label_478574:; 
__retres1 = 0;
label_478579:; 
 __return_478580 = __retres1;
}
tmp___2 = __return_478580;
if (tmp___2 == 0)
{
goto label_478588;
}
else 
{
t3_st = 0;
label_478588:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_479104;
}
else 
{
label_479104:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_479110;
}
else 
{
label_479110:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_479116;
}
else 
{
label_479116:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_479122;
}
else 
{
label_479122:; 
if (E_M == 1)
{
E_M = 2;
goto label_479128;
}
else 
{
label_479128:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_479134;
}
else 
{
label_479134:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_479140;
}
else 
{
label_479140:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_479146;
}
else 
{
label_479146:; 
}
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_479323;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_479323;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_479323;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_479323;
}
else 
{
__retres1 = 0;
label_479323:; 
 __return_479324 = __retres1;
}
tmp = __return_479324;
if (tmp == 0)
{
__retres2 = 1;
goto label_479334;
}
else 
{
__retres2 = 0;
label_479334:; 
 __return_479335 = __retres2;
}
tmp___0 = __return_479335;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_479417;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_479417;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_479417;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_479417;
}
else 
{
__retres1 = 0;
label_479417:; 
 __return_479418 = __retres1;
}
tmp = __return_479418;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_479435;
}
else 
{
label_479435:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_479447;
}
else 
{
label_479447:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_479459;
}
else 
{
label_479459:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
return 1;
}
}
}
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_479494;
}
else 
{
label_479494:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_479500;
}
else 
{
label_479500:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_479506;
}
else 
{
label_479506:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_479512;
}
else 
{
label_479512:; 
if (E_M == 0)
{
E_M = 1;
goto label_479518;
}
else 
{
label_479518:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_479524;
}
else 
{
label_479524:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_479530;
}
else 
{
label_479530:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_479536;
}
else 
{
label_479536:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_479563;
}
else 
{
goto label_479558;
}
}
else 
{
label_479558:; 
__retres1 = 0;
label_479563:; 
 __return_479564 = __retres1;
}
tmp = __return_479564;
if (tmp == 0)
{
goto label_479572;
}
else 
{
m_st = 0;
label_479572:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_479589;
}
else 
{
goto label_479584;
}
}
else 
{
label_479584:; 
__retres1 = 0;
label_479589:; 
 __return_479590 = __retres1;
}
tmp___0 = __return_479590;
if (tmp___0 == 0)
{
goto label_479598;
}
else 
{
t1_st = 0;
label_479598:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_479615;
}
else 
{
goto label_479610;
}
}
else 
{
label_479610:; 
__retres1 = 0;
label_479615:; 
 __return_479616 = __retres1;
}
tmp___1 = __return_479616;
if (tmp___1 == 0)
{
goto label_479624;
}
else 
{
t2_st = 0;
label_479624:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_479641;
}
else 
{
goto label_479636;
}
}
else 
{
label_479636:; 
__retres1 = 0;
label_479641:; 
 __return_479642 = __retres1;
}
tmp___2 = __return_479642;
if (tmp___2 == 0)
{
goto label_479650;
}
else 
{
t3_st = 0;
label_479650:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_479662;
}
else 
{
label_479662:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_479668;
}
else 
{
label_479668:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_479674;
}
else 
{
label_479674:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_479680;
}
else 
{
label_479680:; 
if (E_M == 1)
{
E_M = 2;
goto label_479686;
}
else 
{
label_479686:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_479692;
}
else 
{
label_479692:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_479698;
}
else 
{
label_479698:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_479704;
}
else 
{
label_479704:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_479734;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_479734;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_479734;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_479734;
}
else 
{
__retres1 = 0;
label_479734:; 
 __return_479735 = __retres1;
}
tmp = __return_479735;
kernel_st = 4;
{
M_E = 1;
}
goto label_478477;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
}
label_479367:; 
__retres1 = 0;
 __return_480870 = __retres1;
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
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_473218:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_473368;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_473320;
}
else 
{
label_473320:; 
t3_pc = 1;
t3_st = 2;
}
label_473329:; 
goto label_469977;
}
}
}
else 
{
label_473368:; 
label_473493:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_473522;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_473522;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_473522;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_473522;
}
else 
{
__retres1 = 0;
label_473522:; 
 __return_473523 = __retres1;
}
tmp = __return_473523;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_473540;
}
else 
{
label_473540:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_473552;
}
else 
{
label_473552:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_473717;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_473601;
}
else 
{
goto label_473596;
}
}
else 
{
label_473596:; 
__retres1 = 0;
label_473601:; 
 __return_473602 = __retres1;
}
tmp = __return_473602;
if (tmp == 0)
{
goto label_473610;
}
else 
{
m_st = 0;
label_473610:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_473627;
}
else 
{
goto label_473622;
}
}
else 
{
label_473622:; 
__retres1 = 0;
label_473627:; 
 __return_473628 = __retres1;
}
tmp___0 = __return_473628;
if (tmp___0 == 0)
{
goto label_473636;
}
else 
{
t1_st = 0;
label_473636:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_473653;
}
else 
{
goto label_473648;
}
}
else 
{
label_473648:; 
__retres1 = 0;
label_473653:; 
 __return_473654 = __retres1;
}
tmp___1 = __return_473654;
if (tmp___1 == 0)
{
goto label_473662;
}
else 
{
t2_st = 0;
label_473662:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_473679;
}
else 
{
goto label_473674;
}
}
else 
{
label_473674:; 
__retres1 = 0;
label_473679:; 
 __return_473680 = __retres1;
}
tmp___2 = __return_473680;
if (tmp___2 == 0)
{
goto label_473688;
}
else 
{
t3_st = 0;
label_473688:; 
}
}
E_3 = 2;
t2_pc = 1;
t2_st = 2;
}
goto label_473208;
}
}
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
label_473717:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_473754;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_473742;
}
else 
{
label_473742:; 
t3_pc = 1;
t3_st = 2;
}
goto label_473329;
}
}
}
else 
{
label_473754:; 
goto label_473493;
}
}
}
}
}
}
}
}
}
}
}
}
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
label_473027:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_473216;
}
else 
{
label_473216:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_473366;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_473294;
}
else 
{
label_473294:; 
t3_pc = 1;
t3_st = 2;
}
label_473303:; 
goto label_469495;
}
}
}
else 
{
label_473366:; 
label_473758:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_473787;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_473787;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_473787;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_473787;
}
else 
{
__retres1 = 0;
label_473787:; 
 __return_473788 = __retres1;
}
tmp = __return_473788;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_473805;
}
else 
{
label_473805:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_473970;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_473854;
}
else 
{
goto label_473849;
}
}
else 
{
label_473849:; 
__retres1 = 0;
label_473854:; 
 __return_473855 = __retres1;
}
tmp = __return_473855;
if (tmp == 0)
{
goto label_473863;
}
else 
{
m_st = 0;
label_473863:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_473880;
}
else 
{
goto label_473875;
}
}
else 
{
label_473875:; 
__retres1 = 0;
label_473880:; 
 __return_473881 = __retres1;
}
tmp___0 = __return_473881;
if (tmp___0 == 0)
{
goto label_473889;
}
else 
{
t1_st = 0;
label_473889:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_473906;
}
else 
{
goto label_473901;
}
}
else 
{
label_473901:; 
__retres1 = 0;
label_473906:; 
 __return_473907 = __retres1;
}
tmp___1 = __return_473907;
if (tmp___1 == 0)
{
goto label_473915;
}
else 
{
t2_st = 0;
label_473915:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_473932;
}
else 
{
goto label_473927;
}
}
else 
{
label_473927:; 
__retres1 = 0;
label_473932:; 
 __return_473933 = __retres1;
}
tmp___2 = __return_473933;
if (tmp___2 == 0)
{
goto label_473941;
}
else 
{
t3_st = 0;
label_473941:; 
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
goto label_473020;
}
}
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
label_473970:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_473982;
}
else 
{
label_473982:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_474019;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_474007;
}
else 
{
label_474007:; 
t3_pc = 1;
t3_st = 2;
}
goto label_473303;
}
}
}
else 
{
label_474019:; 
goto label_473758;
}
}
}
}
}
}
}
}
}
}
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
label_472710:; 
goto label_472712;
}
}
}
}
}
else 
{
label_472848:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_473025;
}
else 
{
label_473025:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_473214;
}
else 
{
label_473214:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_473364;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_473268;
}
else 
{
label_473268:; 
t3_pc = 1;
t3_st = 2;
}
goto label_468007;
}
}
}
else 
{
label_473364:; 
goto label_472649;
}
}
}
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
label_467755:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_468055;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_467894;
}
else 
{
label_467894:; 
t3_pc = 1;
t3_st = 2;
}
label_467903:; 
label_470845:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_470874;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_470874;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_470874;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_470874;
}
else 
{
__retres1 = 0;
label_470874:; 
 __return_470875 = __retres1;
}
tmp = __return_470875;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_471044;
}
else 
{
m_st = 1;
{
if (m_pc == 0)
{
goto label_470906;
}
else 
{
if (m_pc == 1)
{
label_470908:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_470940;
}
else 
{
goto label_470935;
}
}
else 
{
label_470935:; 
__retres1 = 0;
label_470940:; 
 __return_470941 = __retres1;
}
tmp = __return_470941;
if (tmp == 0)
{
goto label_470949;
}
else 
{
m_st = 0;
label_470949:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_470966;
}
else 
{
goto label_470961;
}
}
else 
{
label_470961:; 
__retres1 = 0;
label_470966:; 
 __return_470967 = __retres1;
}
tmp___0 = __return_470967;
if (tmp___0 == 0)
{
goto label_470975;
}
else 
{
t1_st = 0;
label_470975:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_470992;
}
else 
{
goto label_470987;
}
}
else 
{
label_470987:; 
__retres1 = 0;
label_470992:; 
 __return_470993 = __retres1;
}
tmp___1 = __return_470993;
if (tmp___1 == 0)
{
goto label_471001;
}
else 
{
t2_st = 0;
label_471001:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_471018;
}
else 
{
goto label_471013;
}
}
else 
{
label_471013:; 
__retres1 = 0;
label_471018:; 
 __return_471019 = __retres1;
}
tmp___2 = __return_471019;
if (tmp___2 == 0)
{
goto label_471027;
}
else 
{
t3_st = 0;
label_471027:; 
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_471223;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_471103;
}
else 
{
goto label_471098;
}
}
else 
{
label_471098:; 
__retres1 = 0;
label_471103:; 
 __return_471104 = __retres1;
}
tmp = __return_471104;
if (tmp == 0)
{
goto label_471112;
}
else 
{
m_st = 0;
label_471112:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_471129;
}
else 
{
goto label_471124;
}
}
else 
{
label_471124:; 
__retres1 = 0;
label_471129:; 
 __return_471130 = __retres1;
}
tmp___0 = __return_471130;
if (tmp___0 == 0)
{
goto label_471138;
}
else 
{
t1_st = 0;
label_471138:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_471155;
}
else 
{
goto label_471150;
}
}
else 
{
label_471150:; 
__retres1 = 0;
label_471155:; 
 __return_471156 = __retres1;
}
tmp___1 = __return_471156;
if (tmp___1 == 0)
{
goto label_471164;
}
else 
{
t2_st = 0;
label_471164:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_471181;
}
else 
{
goto label_471176;
}
}
else 
{
label_471176:; 
__retres1 = 0;
label_471181:; 
 __return_471182 = __retres1;
}
tmp___2 = __return_471182;
if (tmp___2 == 0)
{
goto label_471190;
}
else 
{
t3_st = 0;
label_471190:; 
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_471216:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_471338;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_471317;
}
else 
{
label_471317:; 
t2_pc = 1;
t2_st = 2;
}
label_471326:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_471413;
}
else 
{
label_471413:; 
label_471415:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_471444;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_471444;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_471444;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_471444;
}
else 
{
__retres1 = 0;
label_471444:; 
 __return_471445 = __retres1;
}
tmp = __return_471445;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_471462;
}
else 
{
label_471462:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_471474;
}
else 
{
label_471474:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_471486;
}
else 
{
label_471486:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
return 1;
}
}
}
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_476652;
}
else 
{
label_476652:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_476658;
}
else 
{
label_476658:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_476664;
}
else 
{
label_476664:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_476670;
}
else 
{
label_476670:; 
if (E_M == 0)
{
E_M = 1;
goto label_476676;
}
else 
{
label_476676:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_476682;
}
else 
{
label_476682:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_476688;
}
else 
{
label_476688:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_476694;
}
else 
{
label_476694:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_477057;
}
else 
{
goto label_477052;
}
}
else 
{
label_477052:; 
__retres1 = 0;
label_477057:; 
 __return_477058 = __retres1;
}
tmp = __return_477058;
if (tmp == 0)
{
goto label_477066;
}
else 
{
m_st = 0;
label_477066:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_477083;
}
else 
{
goto label_477078;
}
}
else 
{
label_477078:; 
__retres1 = 0;
label_477083:; 
 __return_477084 = __retres1;
}
tmp___0 = __return_477084;
if (tmp___0 == 0)
{
goto label_477092;
}
else 
{
t1_st = 0;
label_477092:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_477109;
}
else 
{
goto label_477104;
}
}
else 
{
label_477104:; 
__retres1 = 0;
label_477109:; 
 __return_477110 = __retres1;
}
tmp___1 = __return_477110;
if (tmp___1 == 0)
{
goto label_477118;
}
else 
{
t2_st = 0;
label_477118:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_477135;
}
else 
{
goto label_477130;
}
}
else 
{
label_477130:; 
__retres1 = 0;
label_477135:; 
 __return_477136 = __retres1;
}
tmp___2 = __return_477136;
if (tmp___2 == 0)
{
goto label_477144;
}
else 
{
t3_st = 0;
label_477144:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_477996;
}
else 
{
label_477996:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_478002;
}
else 
{
label_478002:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_478008;
}
else 
{
label_478008:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_478014;
}
else 
{
label_478014:; 
if (E_M == 1)
{
E_M = 2;
goto label_478020;
}
else 
{
label_478020:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_478026;
}
else 
{
label_478026:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_478032;
}
else 
{
label_478032:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_478038;
}
else 
{
label_478038:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_478236;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_478236;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_478236;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_478236;
}
else 
{
__retres1 = 0;
label_478236:; 
 __return_478237 = __retres1;
}
tmp = __return_478237;
kernel_st = 4;
{
M_E = 1;
}
label_478463:; 
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_478615;
}
else 
{
goto label_478610;
}
}
else 
{
label_478610:; 
__retres1 = 0;
label_478615:; 
 __return_478616 = __retres1;
}
tmp = __return_478616;
if (tmp == 0)
{
goto label_478624;
}
else 
{
m_st = 0;
label_478624:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_478641;
}
else 
{
goto label_478636;
}
}
else 
{
label_478636:; 
__retres1 = 0;
label_478641:; 
 __return_478642 = __retres1;
}
tmp___0 = __return_478642;
if (tmp___0 == 0)
{
goto label_478650;
}
else 
{
t1_st = 0;
label_478650:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_478667;
}
else 
{
goto label_478662;
}
}
else 
{
label_478662:; 
__retres1 = 0;
label_478667:; 
 __return_478668 = __retres1;
}
tmp___1 = __return_478668;
if (tmp___1 == 0)
{
goto label_478676;
}
else 
{
t2_st = 0;
label_478676:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_478693;
}
else 
{
goto label_478688;
}
}
else 
{
label_478688:; 
__retres1 = 0;
label_478693:; 
 __return_478694 = __retres1;
}
tmp___2 = __return_478694;
if (tmp___2 == 0)
{
goto label_478702;
}
else 
{
t3_st = 0;
label_478702:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_479050;
}
else 
{
label_479050:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_479056;
}
else 
{
label_479056:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_479062;
}
else 
{
label_479062:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_479068;
}
else 
{
label_479068:; 
if (E_M == 1)
{
E_M = 2;
goto label_479074;
}
else 
{
label_479074:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_479080;
}
else 
{
label_479080:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_479086;
}
else 
{
label_479086:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_479092;
}
else 
{
label_479092:; 
}
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_479277;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_479277;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_479277;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_479277;
}
else 
{
__retres1 = 0;
label_479277:; 
 __return_479278 = __retres1;
}
tmp = __return_479278;
if (tmp == 0)
{
__retres2 = 1;
goto label_479288;
}
else 
{
__retres2 = 0;
label_479288:; 
 __return_479289 = __retres2;
}
tmp___0 = __return_479289;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_479789;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_479789;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_479789;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_479789;
}
else 
{
__retres1 = 0;
label_479789:; 
 __return_479790 = __retres1;
}
tmp = __return_479790;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_479807;
}
else 
{
label_479807:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_479819;
}
else 
{
label_479819:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_479831;
}
else 
{
label_479831:; 
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
return 1;
}
}
}
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_479866;
}
else 
{
label_479866:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_479872;
}
else 
{
label_479872:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_479878;
}
else 
{
label_479878:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_479884;
}
else 
{
label_479884:; 
if (E_M == 0)
{
E_M = 1;
goto label_479890;
}
else 
{
label_479890:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_479896;
}
else 
{
label_479896:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_479902;
}
else 
{
label_479902:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_479908;
}
else 
{
label_479908:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_479935;
}
else 
{
goto label_479930;
}
}
else 
{
label_479930:; 
__retres1 = 0;
label_479935:; 
 __return_479936 = __retres1;
}
tmp = __return_479936;
if (tmp == 0)
{
goto label_479944;
}
else 
{
m_st = 0;
label_479944:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_479961;
}
else 
{
goto label_479956;
}
}
else 
{
label_479956:; 
__retres1 = 0;
label_479961:; 
 __return_479962 = __retres1;
}
tmp___0 = __return_479962;
if (tmp___0 == 0)
{
goto label_479970;
}
else 
{
t1_st = 0;
label_479970:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_479987;
}
else 
{
goto label_479982;
}
}
else 
{
label_479982:; 
__retres1 = 0;
label_479987:; 
 __return_479988 = __retres1;
}
tmp___1 = __return_479988;
if (tmp___1 == 0)
{
goto label_479996;
}
else 
{
t2_st = 0;
label_479996:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_480013;
}
else 
{
goto label_480008;
}
}
else 
{
label_480008:; 
__retres1 = 0;
label_480013:; 
 __return_480014 = __retres1;
}
tmp___2 = __return_480014;
if (tmp___2 == 0)
{
goto label_480022;
}
else 
{
t3_st = 0;
label_480022:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_480034;
}
else 
{
label_480034:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_480040;
}
else 
{
label_480040:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_480046;
}
else 
{
label_480046:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_480052;
}
else 
{
label_480052:; 
if (E_M == 1)
{
E_M = 2;
goto label_480058;
}
else 
{
label_480058:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_480064;
}
else 
{
label_480064:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_480070;
}
else 
{
label_480070:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_480076;
}
else 
{
label_480076:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_480106;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_480106;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_480106;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_480106;
}
else 
{
__retres1 = 0;
label_480106:; 
 __return_480107 = __retres1;
}
tmp = __return_480107;
kernel_st = 4;
{
M_E = 1;
}
goto label_478463;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
}
__retres1 = 0;
 __return_480872 = __retres1;
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
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_471338:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_471407;
}
else 
{
label_471407:; 
label_471505:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_471534;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_471534;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_471534;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_471534;
}
else 
{
__retres1 = 0;
label_471534:; 
 __return_471535 = __retres1;
}
tmp = __return_471535;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_471552;
}
else 
{
label_471552:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_471564;
}
else 
{
label_471564:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_471601;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_471589;
}
else 
{
label_471589:; 
t2_pc = 1;
t2_st = 2;
}
goto label_471326;
}
}
}
else 
{
label_471601:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_471613;
}
else 
{
label_471613:; 
goto label_471505;
}
}
}
}
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_476706;
}
else 
{
label_476706:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_476712;
}
else 
{
label_476712:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_476718;
}
else 
{
label_476718:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_476724;
}
else 
{
label_476724:; 
if (E_M == 0)
{
E_M = 1;
goto label_476730;
}
else 
{
label_476730:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_476736;
}
else 
{
label_476736:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_476742;
}
else 
{
label_476742:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_476748;
}
else 
{
label_476748:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_476943;
}
else 
{
goto label_476938;
}
}
else 
{
label_476938:; 
__retres1 = 0;
label_476943:; 
 __return_476944 = __retres1;
}
tmp = __return_476944;
if (tmp == 0)
{
goto label_476952;
}
else 
{
m_st = 0;
label_476952:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_476969;
}
else 
{
goto label_476964;
}
}
else 
{
label_476964:; 
__retres1 = 0;
label_476969:; 
 __return_476970 = __retres1;
}
tmp___0 = __return_476970;
if (tmp___0 == 0)
{
goto label_476978;
}
else 
{
t1_st = 0;
label_476978:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_476995;
}
else 
{
goto label_476990;
}
}
else 
{
label_476990:; 
__retres1 = 0;
label_476995:; 
 __return_476996 = __retres1;
}
tmp___1 = __return_476996;
if (tmp___1 == 0)
{
goto label_477004;
}
else 
{
t2_st = 0;
label_477004:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_477021;
}
else 
{
goto label_477016;
}
}
else 
{
label_477016:; 
__retres1 = 0;
label_477021:; 
 __return_477022 = __retres1;
}
tmp___2 = __return_477022;
if (tmp___2 == 0)
{
goto label_477030;
}
else 
{
t3_st = 0;
label_477030:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_478050;
}
else 
{
label_478050:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_478056;
}
else 
{
label_478056:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_478062;
}
else 
{
label_478062:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_478068;
}
else 
{
label_478068:; 
if (E_M == 1)
{
E_M = 2;
goto label_478074;
}
else 
{
label_478074:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_478080;
}
else 
{
label_478080:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_478086;
}
else 
{
label_478086:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_478092;
}
else 
{
label_478092:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_478206;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_478206;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_478206;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_478206;
}
else 
{
__retres1 = 0;
label_478206:; 
 __return_478207 = __retres1;
}
tmp = __return_478207;
kernel_st = 4;
{
M_E = 1;
}
goto label_478463;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_471223:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_471336;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_471291;
}
else 
{
label_471291:; 
t2_pc = 1;
t2_st = 2;
}
label_471300:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_471411;
}
else 
{
label_471411:; 
goto label_469495;
}
}
}
}
else 
{
label_471336:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_471405;
}
else 
{
label_471405:; 
label_471617:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_471646;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_471646;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_471646;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_471646;
}
else 
{
__retres1 = 0;
label_471646:; 
 __return_471647 = __retres1;
}
tmp = __return_471647;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_471664;
}
else 
{
label_471664:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_471829;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_471713;
}
else 
{
goto label_471708;
}
}
else 
{
label_471708:; 
__retres1 = 0;
label_471713:; 
 __return_471714 = __retres1;
}
tmp = __return_471714;
if (tmp == 0)
{
goto label_471722;
}
else 
{
m_st = 0;
label_471722:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_471739;
}
else 
{
goto label_471734;
}
}
else 
{
label_471734:; 
__retres1 = 0;
label_471739:; 
 __return_471740 = __retres1;
}
tmp___0 = __return_471740;
if (tmp___0 == 0)
{
goto label_471748;
}
else 
{
t1_st = 0;
label_471748:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_471765;
}
else 
{
goto label_471760;
}
}
else 
{
label_471760:; 
__retres1 = 0;
label_471765:; 
 __return_471766 = __retres1;
}
tmp___1 = __return_471766;
if (tmp___1 == 0)
{
goto label_471774;
}
else 
{
t2_st = 0;
label_471774:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_471791;
}
else 
{
goto label_471786;
}
}
else 
{
label_471786:; 
__retres1 = 0;
label_471791:; 
 __return_471792 = __retres1;
}
tmp___2 = __return_471792;
if (tmp___2 == 0)
{
goto label_471800;
}
else 
{
t3_st = 0;
label_471800:; 
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
goto label_471216;
}
}
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
label_471829:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_471866;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_471854;
}
else 
{
label_471854:; 
t2_pc = 1;
t2_st = 2;
}
goto label_471300;
}
}
}
else 
{
label_471866:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_471878;
}
else 
{
label_471878:; 
goto label_471617;
}
}
}
}
}
}
}
}
}
}
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
label_470906:; 
goto label_470908;
}
}
}
}
}
else 
{
label_471044:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_471221;
}
else 
{
label_471221:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_471334;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_471265;
}
else 
{
label_471265:; 
t2_pc = 1;
t2_st = 2;
}
label_471274:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_471409;
}
else 
{
label_471409:; 
goto label_468161;
}
}
}
}
else 
{
label_471334:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_471403;
}
else 
{
label_471403:; 
goto label_470845;
}
}
}
}
}
label_476286:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_476598;
}
else 
{
label_476598:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_476604;
}
else 
{
label_476604:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_476610;
}
else 
{
label_476610:; 
if (T3_E == 0)
{
T3_E = 1;
goto label_476616;
}
else 
{
label_476616:; 
if (E_M == 0)
{
E_M = 1;
goto label_476622;
}
else 
{
label_476622:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_476628;
}
else 
{
label_476628:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_476634;
}
else 
{
label_476634:; 
if (E_3 == 0)
{
E_3 = 1;
goto label_476640;
}
else 
{
label_476640:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_477171;
}
else 
{
goto label_477166;
}
}
else 
{
label_477166:; 
__retres1 = 0;
label_477171:; 
 __return_477172 = __retres1;
}
tmp = __return_477172;
if (tmp == 0)
{
goto label_477180;
}
else 
{
m_st = 0;
label_477180:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_477197;
}
else 
{
goto label_477192;
}
}
else 
{
label_477192:; 
__retres1 = 0;
label_477197:; 
 __return_477198 = __retres1;
}
tmp___0 = __return_477198;
if (tmp___0 == 0)
{
goto label_477206;
}
else 
{
t1_st = 0;
label_477206:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_477223;
}
else 
{
goto label_477218;
}
}
else 
{
label_477218:; 
__retres1 = 0;
label_477223:; 
 __return_477224 = __retres1;
}
tmp___1 = __return_477224;
if (tmp___1 == 0)
{
goto label_477232;
}
else 
{
t2_st = 0;
label_477232:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_477249;
}
else 
{
goto label_477244;
}
}
else 
{
label_477244:; 
__retres1 = 0;
label_477249:; 
 __return_477250 = __retres1;
}
tmp___2 = __return_477250;
if (tmp___2 == 0)
{
goto label_477258;
}
else 
{
t3_st = 0;
label_477258:; 
}
{
if (M_E == 1)
{
M_E = 2;
goto label_477942;
}
else 
{
label_477942:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_477948;
}
else 
{
label_477948:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_477954;
}
else 
{
label_477954:; 
if (T3_E == 1)
{
T3_E = 2;
goto label_477960;
}
else 
{
label_477960:; 
if (E_M == 1)
{
E_M = 2;
goto label_477966;
}
else 
{
label_477966:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_477972;
}
else 
{
label_477972:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_477978;
}
else 
{
label_477978:; 
if (E_3 == 1)
{
E_3 = 2;
goto label_477984;
}
else 
{
label_477984:; 
}
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_478266;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_478266;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_478266;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_478266;
}
else 
{
__retres1 = 0;
label_478266:; 
 __return_478267 = __retres1;
}
tmp = __return_478267;
kernel_st = 4;
{
M_E = 1;
}
goto label_478428;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_468055:; 
label_474783:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_474812;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_474812;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_474812;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_474812;
}
else 
{
__retres1 = 0;
label_474812:; 
 __return_474813 = __retres1;
}
tmp = __return_474813;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_474982;
}
else 
{
m_st = 1;
{
if (m_pc == 0)
{
goto label_474844;
}
else 
{
if (m_pc == 1)
{
label_474846:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_474878;
}
else 
{
goto label_474873;
}
}
else 
{
label_474873:; 
__retres1 = 0;
label_474878:; 
 __return_474879 = __retres1;
}
tmp = __return_474879;
if (tmp == 0)
{
goto label_474887;
}
else 
{
m_st = 0;
label_474887:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_474904;
}
else 
{
goto label_474899;
}
}
else 
{
label_474899:; 
__retres1 = 0;
label_474904:; 
 __return_474905 = __retres1;
}
tmp___0 = __return_474905;
if (tmp___0 == 0)
{
goto label_474913;
}
else 
{
t1_st = 0;
label_474913:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_474930;
}
else 
{
goto label_474925;
}
}
else 
{
label_474925:; 
__retres1 = 0;
label_474930:; 
 __return_474931 = __retres1;
}
tmp___1 = __return_474931;
if (tmp___1 == 0)
{
goto label_474939;
}
else 
{
t2_st = 0;
label_474939:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_474956;
}
else 
{
goto label_474951;
}
}
else 
{
label_474951:; 
__retres1 = 0;
label_474956:; 
 __return_474957 = __retres1;
}
tmp___2 = __return_474957;
if (tmp___2 == 0)
{
goto label_474965;
}
else 
{
t3_st = 0;
label_474965:; 
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_475161;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_475041;
}
else 
{
goto label_475036;
}
}
else 
{
label_475036:; 
__retres1 = 0;
label_475041:; 
 __return_475042 = __retres1;
}
tmp = __return_475042;
if (tmp == 0)
{
goto label_475050;
}
else 
{
m_st = 0;
label_475050:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_475067;
}
else 
{
goto label_475062;
}
}
else 
{
label_475062:; 
__retres1 = 0;
label_475067:; 
 __return_475068 = __retres1;
}
tmp___0 = __return_475068;
if (tmp___0 == 0)
{
goto label_475076;
}
else 
{
t1_st = 0;
label_475076:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_475093;
}
else 
{
goto label_475088;
}
}
else 
{
label_475088:; 
__retres1 = 0;
label_475093:; 
 __return_475094 = __retres1;
}
tmp___1 = __return_475094;
if (tmp___1 == 0)
{
goto label_475102;
}
else 
{
t2_st = 0;
label_475102:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_475119;
}
else 
{
goto label_475114;
}
}
else 
{
label_475114:; 
__retres1 = 0;
label_475119:; 
 __return_475120 = __retres1;
}
tmp___2 = __return_475120;
if (tmp___2 == 0)
{
goto label_475128;
}
else 
{
t3_st = 0;
label_475128:; 
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_475154:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_475275;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_475255;
}
else 
{
label_475255:; 
t2_pc = 1;
t2_st = 2;
}
label_475264:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_475465;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_475438;
}
else 
{
label_475438:; 
t3_pc = 1;
t3_st = 2;
}
label_475447:; 
goto label_471415;
}
}
}
else 
{
label_475465:; 
label_475476:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_475505;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_475505;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_475505;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_475505;
}
else 
{
__retres1 = 0;
label_475505:; 
 __return_475506 = __retres1;
}
tmp = __return_475506;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_475523;
}
else 
{
label_475523:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_475535;
}
else 
{
label_475535:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_475547;
}
else 
{
label_475547:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_475584;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_475572;
}
else 
{
label_475572:; 
t3_pc = 1;
t3_st = 2;
}
goto label_475447;
}
}
}
else 
{
label_475584:; 
goto label_475476;
}
}
}
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
label_475275:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_475461;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_475386;
}
else 
{
label_475386:; 
t3_pc = 1;
t3_st = 2;
}
label_475395:; 
goto label_471505;
}
}
}
else 
{
label_475461:; 
label_475590:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_475619;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_475619;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_475619;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_475619;
}
else 
{
__retres1 = 0;
label_475619:; 
 __return_475620 = __retres1;
}
tmp = __return_475620;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_475637;
}
else 
{
label_475637:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_475649;
}
else 
{
label_475649:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_475686;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_475674;
}
else 
{
label_475674:; 
t2_pc = 1;
t2_st = 2;
}
goto label_475264;
}
}
}
else 
{
label_475686:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_475723;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_475711;
}
else 
{
label_475711:; 
t3_pc = 1;
t3_st = 2;
}
goto label_475395;
}
}
}
else 
{
label_475723:; 
goto label_475590;
}
}
}
}
}
}
}
}
}
}
}
}
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
label_475161:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_475273;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_475229;
}
else 
{
label_475229:; 
t2_pc = 1;
t2_st = 2;
}
label_475238:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_475463;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_475412;
}
else 
{
label_475412:; 
t3_pc = 1;
t3_st = 2;
}
goto label_473303;
}
}
}
else 
{
label_475463:; 
goto label_473758;
}
}
}
}
else 
{
label_475273:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_475459;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_475360;
}
else 
{
label_475360:; 
t3_pc = 1;
t3_st = 2;
}
label_475369:; 
goto label_471617;
}
}
}
else 
{
label_475459:; 
label_475727:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_475756;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_475756;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_475756;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_475756;
}
else 
{
__retres1 = 0;
label_475756:; 
 __return_475757 = __retres1;
}
tmp = __return_475757;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
goto label_475774;
}
else 
{
label_475774:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_475939;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
return 1;
}
else 
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
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_475823;
}
else 
{
goto label_475818;
}
}
else 
{
label_475818:; 
__retres1 = 0;
label_475823:; 
 __return_475824 = __retres1;
}
tmp = __return_475824;
if (tmp == 0)
{
goto label_475832;
}
else 
{
m_st = 0;
label_475832:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_475849;
}
else 
{
goto label_475844;
}
}
else 
{
label_475844:; 
__retres1 = 0;
label_475849:; 
 __return_475850 = __retres1;
}
tmp___0 = __return_475850;
if (tmp___0 == 0)
{
goto label_475858;
}
else 
{
t1_st = 0;
label_475858:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_475875;
}
else 
{
goto label_475870;
}
}
else 
{
label_475870:; 
__retres1 = 0;
label_475875:; 
 __return_475876 = __retres1;
}
tmp___1 = __return_475876;
if (tmp___1 == 0)
{
goto label_475884;
}
else 
{
t2_st = 0;
label_475884:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_475901;
}
else 
{
goto label_475896;
}
}
else 
{
label_475896:; 
__retres1 = 0;
label_475901:; 
 __return_475902 = __retres1;
}
tmp___2 = __return_475902;
if (tmp___2 == 0)
{
goto label_475910;
}
else 
{
t3_st = 0;
label_475910:; 
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
goto label_475154;
}
}
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
label_475939:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_475976;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_475964;
}
else 
{
label_475964:; 
t2_pc = 1;
t2_st = 2;
}
goto label_475238;
}
}
}
else 
{
label_475976:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_476013;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_476001;
}
else 
{
label_476001:; 
t3_pc = 1;
t3_st = 2;
}
goto label_475369;
}
}
}
else 
{
label_476013:; 
goto label_475727;
}
}
}
}
}
}
}
}
}
}
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
label_474844:; 
goto label_474846;
}
}
}
}
}
else 
{
label_474982:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
goto label_475159;
}
else 
{
label_475159:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_475271;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_475203;
}
else 
{
label_475203:; 
t2_pc = 1;
t2_st = 2;
}
goto label_467715;
}
}
}
else 
{
label_475271:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_475457;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_475334;
}
else 
{
label_475334:; 
t3_pc = 1;
t3_st = 2;
}
goto label_467903;
}
}
}
else 
{
label_475457:; 
goto label_474783;
}
}
}
}
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
label_467601:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_467751;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_467654;
}
else 
{
label_467654:; 
t2_pc = 1;
t2_st = 2;
}
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_468059;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_467946;
}
else 
{
label_467946:; 
t3_pc = 1;
t3_st = 2;
}
label_467955:; 
label_470361:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_470390;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_470390;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_470390;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_470390;
}
else 
{
__retres1 = 0;
label_470390:; 
 __return_470391 = __retres1;
}
tmp = __return_470391;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_470560;
}
else 
{
m_st = 1;
{
if (m_pc == 0)
{
goto label_470422;
}
else 
{
if (m_pc == 1)
{
label_470424:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_470456;
}
else 
{
goto label_470451;
}
}
else 
{
label_470451:; 
__retres1 = 0;
label_470456:; 
 __return_470457 = __retres1;
}
tmp = __return_470457;
if (tmp == 0)
{
goto label_470465;
}
else 
{
m_st = 0;
label_470465:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_470482;
}
else 
{
goto label_470477;
}
}
else 
{
label_470477:; 
__retres1 = 0;
label_470482:; 
 __return_470483 = __retres1;
}
tmp___0 = __return_470483;
if (tmp___0 == 0)
{
goto label_470491;
}
else 
{
t1_st = 0;
label_470491:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_470508;
}
else 
{
goto label_470503;
}
}
else 
{
label_470503:; 
__retres1 = 0;
label_470508:; 
 __return_470509 = __retres1;
}
tmp___1 = __return_470509;
if (tmp___1 == 0)
{
goto label_470517;
}
else 
{
t2_st = 0;
label_470517:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_470534;
}
else 
{
goto label_470529;
}
}
else 
{
label_470529:; 
__retres1 = 0;
label_470534:; 
 __return_470535 = __retres1;
}
tmp___2 = __return_470535;
if (tmp___2 == 0)
{
goto label_470543;
}
else 
{
t3_st = 0;
label_470543:; 
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_470636;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_470620;
}
else 
{
label_470620:; 
t1_pc = 1;
t1_st = 2;
}
goto label_470302;
}
}
}
else 
{
label_470636:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_470671;
}
else 
{
label_470671:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_470707;
}
else 
{
label_470707:; 
goto label_470221;
}
}
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
label_470422:; 
goto label_470424;
}
}
}
}
}
else 
{
label_470560:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_470634;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_470594;
}
else 
{
label_470594:; 
t1_pc = 1;
t1_st = 2;
}
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_470673;
}
else 
{
label_470673:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_470709;
}
else 
{
label_470709:; 
goto label_468161;
}
}
}
}
}
else 
{
label_470634:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_470669;
}
else 
{
label_470669:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_470705;
}
else 
{
label_470705:; 
goto label_470361;
}
}
}
}
}
goto label_476273;
}
}
}
}
}
}
}
else 
{
label_468059:; 
label_474215:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_474244;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_474244;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_474244;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_474244;
}
else 
{
__retres1 = 0;
label_474244:; 
 __return_474245 = __retres1;
}
tmp = __return_474245;
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_474414;
}
else 
{
m_st = 1;
{
if (m_pc == 0)
{
goto label_474276;
}
else 
{
if (m_pc == 1)
{
label_474278:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_474310;
}
else 
{
goto label_474305;
}
}
else 
{
label_474305:; 
__retres1 = 0;
label_474310:; 
 __return_474311 = __retres1;
}
tmp = __return_474311;
if (tmp == 0)
{
goto label_474319;
}
else 
{
m_st = 0;
label_474319:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_474336;
}
else 
{
goto label_474331;
}
}
else 
{
label_474331:; 
__retres1 = 0;
label_474336:; 
 __return_474337 = __retres1;
}
tmp___0 = __return_474337;
if (tmp___0 == 0)
{
goto label_474345;
}
else 
{
t1_st = 0;
label_474345:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_474362;
}
else 
{
goto label_474357;
}
}
else 
{
label_474357:; 
__retres1 = 0;
label_474362:; 
 __return_474363 = __retres1;
}
tmp___1 = __return_474363;
if (tmp___1 == 0)
{
goto label_474371;
}
else 
{
t2_st = 0;
label_474371:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_474388;
}
else 
{
goto label_474383;
}
}
else 
{
label_474383:; 
__retres1 = 0;
label_474388:; 
 __return_474389 = __retres1;
}
tmp___2 = __return_474389;
if (tmp___2 == 0)
{
goto label_474397;
}
else 
{
t3_st = 0;
label_474397:; 
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_474490;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_474474;
}
else 
{
label_474474:; 
t1_pc = 1;
t1_st = 2;
}
goto label_474106;
}
}
}
else 
{
label_474490:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_474525;
}
else 
{
label_474525:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_474636;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_474594;
}
else 
{
label_474594:; 
t3_pc = 1;
t3_st = 2;
}
goto label_467981;
}
}
}
else 
{
label_474636:; 
goto label_474025;
}
}
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
label_474276:; 
goto label_474278;
}
}
}
}
}
else 
{
label_474414:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_474488;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_474448;
}
else 
{
label_474448:; 
t1_pc = 1;
t1_st = 2;
}
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_474527;
}
else 
{
label_474527:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_474638;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_474620;
}
else 
{
label_474620:; 
t3_pc = 1;
t3_st = 2;
}
goto label_468007;
}
}
}
else 
{
label_474638:; 
goto label_472649;
}
}
}
}
}
else 
{
label_474488:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
goto label_474523;
}
else 
{
label_474523:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_474634;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_474568;
}
else 
{
label_474568:; 
t3_pc = 1;
t3_st = 2;
}
goto label_467955;
}
}
}
else 
{
label_474634:; 
goto label_474215;
}
}
}
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
label_467751:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_468051;
}
else 
{
t3_st = 1;
{
if (t3_pc == 0)
{
goto label_467842;
}
else 
{
label_467842:; 
t3_pc = 1;
t3_st = 2;
}
label_472090:; 
{
int __retres1 ;
if (m_st == 0)
{
__retres1 = 1;
goto label_472119;
}
else 
{
if (t1_st == 0)
{
__retres1 = 1;
goto label_472119;
}
else 
{
if (t2_st == 0)
{
__retres1 = 1;
goto label_472119;
}
else 
{
if (t3_st == 0)
{
__retres1 = 1;
goto label_472119;
}
else 
{
__retres1 = 0;
label_472119:; 
 __return_472120 = __retres1;
}
tmp = __return_472120;
if (tmp == 0)
{
}
else 
{
if (m_st == 0)
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_472289;
}
else 
{
m_st = 1;
{
if (m_pc == 0)
{
goto label_472151;
}
else 
{
if (m_pc == 1)
{
label_472153:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (m_pc == 1)
{
if (E_M == 1)
{
__retres1 = 1;
goto label_472185;
}
else 
{
goto label_472180;
}
}
else 
{
label_472180:; 
__retres1 = 0;
label_472185:; 
 __return_472186 = __retres1;
}
tmp = __return_472186;
if (tmp == 0)
{
goto label_472194;
}
else 
{
m_st = 0;
label_472194:; 
{
int __retres1 ;
if (t1_pc == 1)
{
if (E_1 == 1)
{
__retres1 = 1;
goto label_472211;
}
else 
{
goto label_472206;
}
}
else 
{
label_472206:; 
__retres1 = 0;
label_472211:; 
 __return_472212 = __retres1;
}
tmp___0 = __return_472212;
if (tmp___0 == 0)
{
goto label_472220;
}
else 
{
t1_st = 0;
label_472220:; 
{
int __retres1 ;
if (t2_pc == 1)
{
if (E_2 == 1)
{
__retres1 = 1;
goto label_472237;
}
else 
{
goto label_472232;
}
}
else 
{
label_472232:; 
__retres1 = 0;
label_472237:; 
 __return_472238 = __retres1;
}
tmp___1 = __return_472238;
if (tmp___1 == 0)
{
goto label_472246;
}
else 
{
t2_st = 0;
label_472246:; 
{
int __retres1 ;
if (t3_pc == 1)
{
if (E_3 == 1)
{
__retres1 = 1;
goto label_472263;
}
else 
{
goto label_472258;
}
}
else 
{
label_472258:; 
__retres1 = 0;
label_472263:; 
 __return_472264 = __retres1;
}
tmp___2 = __return_472264;
if (tmp___2 == 0)
{
goto label_472272;
}
else 
{
t3_st = 0;
label_472272:; 
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_472365;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_472349;
}
else 
{
label_472349:; 
t1_pc = 1;
t1_st = 2;
}
goto label_471965;
}
}
}
else 
{
label_472365:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_472476;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_472433;
}
else 
{
label_472433:; 
t2_pc = 1;
t2_st = 2;
}
goto label_472012;
}
}
}
else 
{
label_472476:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_472523;
}
else 
{
label_472523:; 
goto label_471884;
}
}
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
label_472151:; 
goto label_472153;
}
}
}
}
}
else 
{
label_472289:; 
if (t1_st == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_472363;
}
else 
{
t1_st = 1;
{
if (t1_pc == 0)
{
goto label_472323;
}
else 
{
label_472323:; 
t1_pc = 1;
t1_st = 2;
}
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_472478;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_472459;
}
else 
{
label_472459:; 
t2_pc = 1;
t2_st = 2;
}
goto label_471274;
}
}
}
else 
{
label_472478:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_472525;
}
else 
{
label_472525:; 
goto label_470845;
}
}
}
}
}
else 
{
label_472363:; 
if (t2_st == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_472474;
}
else 
{
t2_st = 1;
{
if (t2_pc == 0)
{
goto label_472407;
}
else 
{
label_472407:; 
t2_pc = 1;
t2_st = 2;
}
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_472527;
}
else 
{
label_472527:; 
goto label_470361;
}
}
}
}
else 
{
label_472474:; 
if (t3_st == 0)
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
goto label_472521;
}
else 
{
label_472521:; 
goto label_472090;
}
}
}
}
}
goto label_476286;
}
}
}
}
}
}
}
else 
{
label_468051:; 
goto label_467327;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
