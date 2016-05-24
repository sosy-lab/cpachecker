extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int m_pc  =    0;
int t1_pc  =    0;
int t2_pc  =    0;
int m_st  ;
int t1_st  ;
int t2_st  ;
int m_i  ;
int t1_i  ;
int t2_i  ;
int M_E  =    2;
int T1_E  =    2;
int T2_E  =    2;
int E_1  =    2;
int E_2  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
int is_transmit2_triggered(void) ;
void immediate_notify(void) ;
void master(void);
void transmit1(void);
void transmit2(void);
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
int __return_496;
int __return_525;
int __return_554;
int __return_632;
int __return_702;
int __return_731;
int __return_760;
int __return_838;
int __return_867;
int __return_896;
int __return_1024;
int __return_1053;
int __return_1082;
int __return_1145;
int __return_1183;
int __return_1212;
int __return_1241;
int __return_1315;
int __return_1326;
int __return_1339;
int main()
{
int __retres1 ;
{
m_i = 1;
t1_i = 1;
t2_i = 1;
}
{
int kernel_st ;
int tmp ;
int tmp___0 ;
kernel_st = 0;
{
}
{
if (m_i < 2)
{
if (m_i > 0)
{
m_st = 0;
goto label_425;
}
else 
{
goto label_421;
}
}
else 
{
label_421:; 
m_st = 2;
label_425:; 
if (t1_i < 2)
{
if (t1_i > 0)
{
t1_st = 0;
goto label_433;
}
else 
{
goto label_429;
}
}
else 
{
label_429:; 
t1_st = 2;
label_433:; 
if (t2_i < 2)
{
if (t2_i > 0)
{
t2_st = 0;
goto label_441;
}
else 
{
goto label_437;
}
}
else 
{
label_437:; 
t2_st = 2;
label_441:; 
}
{
if (T1_E == 0)
{
T1_E = 1;
goto label_452;
}
else 
{
label_452:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_457;
}
else 
{
label_457:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_462;
}
else 
{
label_462:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_467;
}
else 
{
label_467:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (m_pc < 2)
{
if (m_pc > 0)
{
goto label_490;
}
else 
{
goto label_487;
}
}
else 
{
label_487:; 
label_490:; 
__retres1 = 0;
 __return_496 = __retres1;
}
tmp = __return_496;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_502;
}
else 
{
goto label_499;
}
}
else 
{
label_499:; 
label_502:; 
{
int __retres1 ;
if (t1_pc < 2)
{
if (t1_pc > 0)
{
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
goto label_522;
}
else 
{
goto label_516;
}
}
else 
{
label_516:; 
goto label_514;
}
}
else 
{
goto label_511;
}
}
else 
{
label_511:; 
label_514:; 
__retres1 = 0;
label_522:; 
 __return_525 = __retres1;
}
tmp___0 = __return_525;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_531;
}
else 
{
goto label_528;
}
}
else 
{
label_528:; 
label_531:; 
{
int __retres1 ;
if (t2_pc < 2)
{
if (t2_pc > 0)
{
if (E_2 < 2)
{
if (E_2 > 0)
{
__retres1 = 1;
goto label_551;
}
else 
{
goto label_545;
}
}
else 
{
label_545:; 
goto label_543;
}
}
else 
{
goto label_540;
}
}
else 
{
label_540:; 
label_543:; 
__retres1 = 0;
label_551:; 
 __return_554 = __retres1;
}
tmp___1 = __return_554;
if (tmp___1 < 1)
{
if (tmp___1 > -1)
{
t2_st = 0;
goto label_560;
}
else 
{
goto label_557;
}
}
else 
{
label_557:; 
label_560:; 
}
{
if (T1_E == 1)
{
T1_E = 2;
goto label_571;
}
else 
{
label_571:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_576;
}
else 
{
label_576:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_581;
}
else 
{
label_581:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_586;
}
else 
{
label_586:; 
}
label_591:; 
kernel_st = 1;
{
int tmp ;
int tmp_ndt_3;
int tmp_ndt_2;
int tmp_ndt_1;
label_603:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_625;
}
else 
{
goto label_612;
}
}
else 
{
label_612:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_625;
}
else 
{
goto label_616;
}
}
else 
{
label_616:; 
if (t2_st < 1)
{
if (t2_st > -1)
{
__retres1 = 1;
goto label_625;
}
else 
{
goto label_620;
}
}
else 
{
label_620:; 
__retres1 = 0;
label_625:; 
 __return_632 = __retres1;
}
tmp = __return_632;
if (tmp < 1)
{
if (tmp > -1)
{
if (m_st < 1)
{
if (m_st > -1)
{
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 < 1)
{
if (tmp_ndt_1 > -1)
{
m_st = 1;
{
if (m_pc < 1)
{
if (m_pc > -1)
{
goto label_667;
}
else 
{
goto label_659;
}
}
else 
{
label_659:; 
if (m_pc < 2)
{
if (m_pc > 0)
{
label_670:; 
m_pc = 1;
m_st = 2;
}
else 
{
goto label_663;
}
goto label_654;
}
else 
{
label_663:; 
label_667:; 
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (m_pc < 2)
{
if (m_pc > 0)
{
goto label_696;
}
else 
{
goto label_693;
}
}
else 
{
label_693:; 
label_696:; 
__retres1 = 0;
 __return_702 = __retres1;
}
tmp = __return_702;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_708;
}
else 
{
goto label_705;
}
}
else 
{
label_705:; 
label_708:; 
{
int __retres1 ;
if (t1_pc < 2)
{
if (t1_pc > 0)
{
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
goto label_728;
}
else 
{
goto label_722;
}
}
else 
{
label_722:; 
goto label_720;
}
}
else 
{
goto label_717;
}
}
else 
{
label_717:; 
label_720:; 
__retres1 = 0;
label_728:; 
 __return_731 = __retres1;
}
tmp___0 = __return_731;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_737;
}
else 
{
goto label_734;
}
}
else 
{
label_734:; 
label_737:; 
{
int __retres1 ;
if (t2_pc < 2)
{
if (t2_pc > 0)
{
if (E_2 < 2)
{
if (E_2 > 0)
{
__retres1 = 1;
goto label_757;
}
else 
{
goto label_751;
}
}
else 
{
label_751:; 
goto label_749;
}
}
else 
{
goto label_746;
}
}
else 
{
label_746:; 
label_749:; 
__retres1 = 0;
label_757:; 
 __return_760 = __retres1;
}
tmp___1 = __return_760;
if (tmp___1 < 1)
{
if (tmp___1 > -1)
{
t2_st = 0;
goto label_766;
}
else 
{
goto label_763;
}
}
else 
{
label_763:; 
label_766:; 
}
}
E_1 = 2;
goto label_670;
}
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
goto label_651;
}
}
else 
{
label_651:; 
label_654:; 
goto label_647;
}
}
else 
{
goto label_644;
}
}
else 
{
label_644:; 
label_647:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 < 1)
{
if (tmp_ndt_2 > -1)
{
t1_st = 1;
{
if (t1_pc < 1)
{
if (t1_pc > -1)
{
goto label_808;
}
else 
{
goto label_800;
}
}
else 
{
label_800:; 
if (t1_pc < 2)
{
if (t1_pc > 0)
{
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (m_pc < 2)
{
if (m_pc > 0)
{
goto label_832;
}
else 
{
goto label_829;
}
}
else 
{
label_829:; 
label_832:; 
__retres1 = 0;
 __return_838 = __retres1;
}
tmp = __return_838;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_844;
}
else 
{
goto label_841;
}
}
else 
{
label_841:; 
label_844:; 
{
int __retres1 ;
if (t1_pc < 2)
{
if (t1_pc > 0)
{
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
goto label_864;
}
else 
{
goto label_858;
}
}
else 
{
label_858:; 
goto label_856;
}
}
else 
{
goto label_853;
}
}
else 
{
label_853:; 
label_856:; 
__retres1 = 0;
label_864:; 
 __return_867 = __retres1;
}
tmp___0 = __return_867;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_873;
}
else 
{
goto label_870;
}
}
else 
{
label_870:; 
label_873:; 
{
int __retres1 ;
if (t2_pc < 2)
{
if (t2_pc > 0)
{
if (E_2 < 2)
{
if (E_2 > 0)
{
__retres1 = 1;
goto label_893;
}
else 
{
goto label_887;
}
}
else 
{
label_887:; 
goto label_885;
}
}
else 
{
goto label_882;
}
}
else 
{
label_882:; 
label_885:; 
__retres1 = 0;
label_893:; 
 __return_896 = __retres1;
}
tmp___1 = __return_896;
if (tmp___1 < 1)
{
if (tmp___1 > -1)
{
t2_st = 0;
goto label_902;
}
else 
{
goto label_899;
}
}
else 
{
label_899:; 
label_902:; 
}
}
E_2 = 2;
label_910:; 
t1_pc = 1;
t1_st = 2;
}
goto label_795;
}
}
}
}
}
}
else 
{
goto label_804;
}
}
else 
{
label_804:; 
label_808:; 
goto label_910;
}
}
}
}
else 
{
goto label_792;
}
}
else 
{
label_792:; 
label_795:; 
goto label_788;
}
}
else 
{
goto label_785;
}
}
else 
{
label_785:; 
label_788:; 
if (t2_st < 1)
{
if (t2_st > -1)
{
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 < 1)
{
if (tmp_ndt_3 > -1)
{
t2_st = 1;
{
if (t2_pc < 1)
{
if (t2_pc > -1)
{
goto label_946;
}
else 
{
goto label_938;
}
}
else 
{
label_938:; 
if (t2_pc < 2)
{
if (t2_pc > 0)
{
{
__VERIFIER_error();
}
label_955:; 
t2_pc = 1;
t2_st = 2;
}
else 
{
goto label_942;
}
goto label_933;
}
else 
{
label_942:; 
label_946:; 
goto label_955;
}
}
}
}
else 
{
goto label_930;
}
}
else 
{
label_930:; 
label_933:; 
goto label_926;
}
}
else 
{
goto label_923;
}
}
else 
{
label_923:; 
label_926:; 
goto label_603;
}
}
}
}
else 
{
goto label_635;
}
}
else 
{
label_635:; 
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (T1_E == 0)
{
T1_E = 1;
goto label_980;
}
else 
{
label_980:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_985;
}
else 
{
label_985:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_990;
}
else 
{
label_990:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_995;
}
else 
{
label_995:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (m_pc < 2)
{
if (m_pc > 0)
{
goto label_1018;
}
else 
{
goto label_1015;
}
}
else 
{
label_1015:; 
label_1018:; 
__retres1 = 0;
 __return_1024 = __retres1;
}
tmp = __return_1024;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_1030;
}
else 
{
goto label_1027;
}
}
else 
{
label_1027:; 
label_1030:; 
{
int __retres1 ;
if (t1_pc < 2)
{
if (t1_pc > 0)
{
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
goto label_1050;
}
else 
{
goto label_1044;
}
}
else 
{
label_1044:; 
goto label_1042;
}
}
else 
{
goto label_1039;
}
}
else 
{
label_1039:; 
label_1042:; 
__retres1 = 0;
label_1050:; 
 __return_1053 = __retres1;
}
tmp___0 = __return_1053;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_1059;
}
else 
{
goto label_1056;
}
}
else 
{
label_1056:; 
label_1059:; 
{
int __retres1 ;
if (t2_pc < 2)
{
if (t2_pc > 0)
{
if (E_2 < 2)
{
if (E_2 > 0)
{
__retres1 = 1;
goto label_1079;
}
else 
{
goto label_1073;
}
}
else 
{
label_1073:; 
goto label_1071;
}
}
else 
{
goto label_1068;
}
}
else 
{
label_1068:; 
label_1071:; 
__retres1 = 0;
label_1079:; 
 __return_1082 = __retres1;
}
tmp___1 = __return_1082;
if (tmp___1 < 1)
{
if (tmp___1 > -1)
{
t2_st = 0;
goto label_1088;
}
else 
{
goto label_1085;
}
}
else 
{
label_1085:; 
label_1088:; 
}
{
if (T1_E == 1)
{
T1_E = 2;
goto label_1099;
}
else 
{
label_1099:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_1104;
}
else 
{
label_1104:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_1109;
}
else 
{
label_1109:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_1114;
}
else 
{
label_1114:; 
}
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_1138;
}
else 
{
goto label_1125;
}
}
else 
{
label_1125:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_1138;
}
else 
{
goto label_1129;
}
}
else 
{
label_1129:; 
if (t2_st < 1)
{
if (t2_st > -1)
{
__retres1 = 1;
goto label_1138;
}
else 
{
goto label_1133;
}
}
else 
{
label_1133:; 
__retres1 = 0;
label_1138:; 
 __return_1145 = __retres1;
}
tmp = __return_1145;
if (tmp < 1)
{
if (tmp > -1)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (m_pc < 2)
{
if (m_pc > 0)
{
__retres1 = 1;
goto label_1180;
}
else 
{
goto label_1173;
}
}
else 
{
label_1173:; 
__retres1 = 0;
label_1180:; 
 __return_1183 = __retres1;
}
tmp = __return_1183;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_1189;
}
else 
{
goto label_1186;
}
}
else 
{
label_1186:; 
label_1189:; 
{
int __retres1 ;
if (t1_pc < 2)
{
if (t1_pc > 0)
{
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
goto label_1209;
}
else 
{
goto label_1203;
}
}
else 
{
label_1203:; 
goto label_1201;
}
}
else 
{
goto label_1198;
}
}
else 
{
label_1198:; 
label_1201:; 
__retres1 = 0;
label_1209:; 
 __return_1212 = __retres1;
}
tmp___0 = __return_1212;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_1218;
}
else 
{
goto label_1215;
}
}
else 
{
label_1215:; 
label_1218:; 
{
int __retres1 ;
if (t2_pc < 2)
{
if (t2_pc > 0)
{
if (E_2 < 2)
{
if (E_2 > 0)
{
__retres1 = 1;
goto label_1238;
}
else 
{
goto label_1232;
}
}
else 
{
label_1232:; 
goto label_1230;
}
}
else 
{
goto label_1227;
}
}
else 
{
label_1227:; 
label_1230:; 
__retres1 = 0;
label_1238:; 
 __return_1241 = __retres1;
}
tmp___1 = __return_1241;
if (tmp___1 < 1)
{
if (tmp___1 > -1)
{
t2_st = 0;
goto label_1247;
}
else 
{
goto label_1244;
}
}
else 
{
label_1244:; 
label_1247:; 
}
{
M_E = 2;
if (T1_E == 1)
{
T1_E = 2;
goto label_1261;
}
else 
{
label_1261:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_1266;
}
else 
{
label_1266:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_1271;
}
else 
{
label_1271:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_1276;
}
else 
{
label_1276:; 
}
goto label_1151;
}
}
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
goto label_1148;
}
}
else 
{
label_1148:; 
label_1151:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_1308;
}
else 
{
goto label_1295;
}
}
else 
{
label_1295:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_1308;
}
else 
{
goto label_1299;
}
}
else 
{
label_1299:; 
if (t2_st < 1)
{
if (t2_st > -1)
{
__retres1 = 1;
goto label_1308;
}
else 
{
goto label_1303;
}
}
else 
{
label_1303:; 
__retres1 = 0;
label_1308:; 
 __return_1315 = __retres1;
}
tmp = __return_1315;
if (tmp < 1)
{
if (tmp > -1)
{
__retres2 = 0;
goto label_1323;
}
else 
{
goto label_1318;
}
}
else 
{
label_1318:; 
__retres2 = 1;
label_1323:; 
 __return_1326 = __retres2;
}
tmp___0 = __return_1326;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
}
else 
{
goto label_1329;
}
__retres1 = 0;
 __return_1339 = __retres1;
return 1;
}
else 
{
label_1329:; 
goto label_591;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
