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
int __return_481;
int __return_510;
int __return_539;
int __return_617;
int __return_687;
int __return_716;
int __return_745;
int __return_823;
int __return_852;
int __return_881;
int __return_1009;
int __return_1038;
int __return_1067;
int __return_1130;
int __return_1168;
int __return_1197;
int __return_1226;
int __return_1300;
int __return_1311;
int __return_1324;
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
goto label_410;
}
else 
{
goto label_406;
}
}
else 
{
label_406:; 
m_st = 2;
label_410:; 
if (t1_i < 2)
{
if (t1_i > 0)
{
t1_st = 0;
goto label_418;
}
else 
{
goto label_414;
}
}
else 
{
label_414:; 
t1_st = 2;
label_418:; 
if (t2_i < 2)
{
if (t2_i > 0)
{
t2_st = 0;
goto label_426;
}
else 
{
goto label_422;
}
}
else 
{
label_422:; 
t2_st = 2;
label_426:; 
}
{
if (T1_E == 0)
{
T1_E = 1;
goto label_437;
}
else 
{
label_437:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_442;
}
else 
{
label_442:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_447;
}
else 
{
label_447:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_452;
}
else 
{
label_452:; 
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
goto label_475;
}
else 
{
goto label_472;
}
}
else 
{
label_472:; 
label_475:; 
__retres1 = 0;
 __return_481 = __retres1;
}
tmp = __return_481;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_487;
}
else 
{
goto label_484;
}
}
else 
{
label_484:; 
label_487:; 
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
goto label_507;
}
else 
{
goto label_501;
}
}
else 
{
label_501:; 
goto label_499;
}
}
else 
{
goto label_496;
}
}
else 
{
label_496:; 
label_499:; 
__retres1 = 0;
label_507:; 
 __return_510 = __retres1;
}
tmp___0 = __return_510;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_516;
}
else 
{
goto label_513;
}
}
else 
{
label_513:; 
label_516:; 
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
goto label_536;
}
else 
{
goto label_530;
}
}
else 
{
label_530:; 
goto label_528;
}
}
else 
{
goto label_525;
}
}
else 
{
label_525:; 
label_528:; 
__retres1 = 0;
label_536:; 
 __return_539 = __retres1;
}
tmp___1 = __return_539;
if (tmp___1 < 1)
{
if (tmp___1 > -1)
{
t2_st = 0;
goto label_545;
}
else 
{
goto label_542;
}
}
else 
{
label_542:; 
label_545:; 
}
{
if (T1_E == 1)
{
T1_E = 2;
goto label_556;
}
else 
{
label_556:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_561;
}
else 
{
label_561:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_566;
}
else 
{
label_566:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_571;
}
else 
{
label_571:; 
}
label_576:; 
kernel_st = 1;
{
int tmp ;
int tmp_ndt_3;
int tmp_ndt_2;
int tmp_ndt_1;
label_588:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_610;
}
else 
{
goto label_597;
}
}
else 
{
label_597:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_610;
}
else 
{
goto label_601;
}
}
else 
{
label_601:; 
if (t2_st < 1)
{
if (t2_st > -1)
{
__retres1 = 1;
goto label_610;
}
else 
{
goto label_605;
}
}
else 
{
label_605:; 
__retres1 = 0;
label_610:; 
 __return_617 = __retres1;
}
tmp = __return_617;
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
goto label_652;
}
else 
{
goto label_644;
}
}
else 
{
label_644:; 
if (m_pc < 2)
{
if (m_pc > 0)
{
label_655:; 
m_pc = 1;
m_st = 2;
}
else 
{
goto label_648;
}
goto label_639;
}
else 
{
label_648:; 
label_652:; 
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
goto label_681;
}
else 
{
goto label_678;
}
}
else 
{
label_678:; 
label_681:; 
__retres1 = 0;
 __return_687 = __retres1;
}
tmp = __return_687;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_693;
}
else 
{
goto label_690;
}
}
else 
{
label_690:; 
label_693:; 
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
goto label_713;
}
else 
{
goto label_707;
}
}
else 
{
label_707:; 
goto label_705;
}
}
else 
{
goto label_702;
}
}
else 
{
label_702:; 
label_705:; 
__retres1 = 0;
label_713:; 
 __return_716 = __retres1;
}
tmp___0 = __return_716;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_722;
}
else 
{
goto label_719;
}
}
else 
{
label_719:; 
label_722:; 
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
goto label_742;
}
else 
{
goto label_736;
}
}
else 
{
label_736:; 
goto label_734;
}
}
else 
{
goto label_731;
}
}
else 
{
label_731:; 
label_734:; 
__retres1 = 0;
label_742:; 
 __return_745 = __retres1;
}
tmp___1 = __return_745;
if (tmp___1 < 1)
{
if (tmp___1 > -1)
{
t2_st = 0;
goto label_751;
}
else 
{
goto label_748;
}
}
else 
{
label_748:; 
label_751:; 
}
}
E_1 = 2;
goto label_655;
}
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
goto label_636;
}
}
else 
{
label_636:; 
label_639:; 
goto label_632;
}
}
else 
{
goto label_629;
}
}
else 
{
label_629:; 
label_632:; 
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
goto label_793;
}
else 
{
goto label_785;
}
}
else 
{
label_785:; 
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
goto label_817;
}
else 
{
goto label_814;
}
}
else 
{
label_814:; 
label_817:; 
__retres1 = 0;
 __return_823 = __retres1;
}
tmp = __return_823;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_829;
}
else 
{
goto label_826;
}
}
else 
{
label_826:; 
label_829:; 
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
goto label_849;
}
else 
{
goto label_843;
}
}
else 
{
label_843:; 
goto label_841;
}
}
else 
{
goto label_838;
}
}
else 
{
label_838:; 
label_841:; 
__retres1 = 0;
label_849:; 
 __return_852 = __retres1;
}
tmp___0 = __return_852;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_858;
}
else 
{
goto label_855;
}
}
else 
{
label_855:; 
label_858:; 
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
goto label_878;
}
else 
{
goto label_872;
}
}
else 
{
label_872:; 
goto label_870;
}
}
else 
{
goto label_867;
}
}
else 
{
label_867:; 
label_870:; 
__retres1 = 0;
label_878:; 
 __return_881 = __retres1;
}
tmp___1 = __return_881;
if (tmp___1 < 1)
{
if (tmp___1 > -1)
{
t2_st = 0;
goto label_887;
}
else 
{
goto label_884;
}
}
else 
{
label_884:; 
label_887:; 
}
}
E_2 = 2;
label_895:; 
t1_pc = 1;
t1_st = 2;
}
goto label_780;
}
}
}
}
}
}
else 
{
goto label_789;
}
}
else 
{
label_789:; 
label_793:; 
goto label_895;
}
}
}
}
else 
{
goto label_777;
}
}
else 
{
label_777:; 
label_780:; 
goto label_773;
}
}
else 
{
goto label_770;
}
}
else 
{
label_770:; 
label_773:; 
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
goto label_931;
}
else 
{
goto label_923;
}
}
else 
{
label_923:; 
if (t2_pc < 2)
{
if (t2_pc > 0)
{
{
__VERIFIER_error();
}
label_940:; 
t2_pc = 1;
t2_st = 2;
}
else 
{
goto label_927;
}
goto label_918;
}
else 
{
label_927:; 
label_931:; 
goto label_940;
}
}
}
}
else 
{
goto label_915;
}
}
else 
{
label_915:; 
label_918:; 
goto label_911;
}
}
else 
{
goto label_908;
}
}
else 
{
label_908:; 
label_911:; 
goto label_588;
}
}
}
}
else 
{
goto label_620;
}
}
else 
{
label_620:; 
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (T1_E == 0)
{
T1_E = 1;
goto label_965;
}
else 
{
label_965:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_970;
}
else 
{
label_970:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_975;
}
else 
{
label_975:; 
if (E_2 == 0)
{
E_2 = 1;
goto label_980;
}
else 
{
label_980:; 
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
goto label_1003;
}
else 
{
goto label_1000;
}
}
else 
{
label_1000:; 
label_1003:; 
__retres1 = 0;
 __return_1009 = __retres1;
}
tmp = __return_1009;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_1015;
}
else 
{
goto label_1012;
}
}
else 
{
label_1012:; 
label_1015:; 
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
goto label_1035;
}
else 
{
goto label_1029;
}
}
else 
{
label_1029:; 
goto label_1027;
}
}
else 
{
goto label_1024;
}
}
else 
{
label_1024:; 
label_1027:; 
__retres1 = 0;
label_1035:; 
 __return_1038 = __retres1;
}
tmp___0 = __return_1038;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_1044;
}
else 
{
goto label_1041;
}
}
else 
{
label_1041:; 
label_1044:; 
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
goto label_1064;
}
else 
{
goto label_1058;
}
}
else 
{
label_1058:; 
goto label_1056;
}
}
else 
{
goto label_1053;
}
}
else 
{
label_1053:; 
label_1056:; 
__retres1 = 0;
label_1064:; 
 __return_1067 = __retres1;
}
tmp___1 = __return_1067;
if (tmp___1 < 1)
{
if (tmp___1 > -1)
{
t2_st = 0;
goto label_1073;
}
else 
{
goto label_1070;
}
}
else 
{
label_1070:; 
label_1073:; 
}
{
if (T1_E == 1)
{
T1_E = 2;
goto label_1084;
}
else 
{
label_1084:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_1089;
}
else 
{
label_1089:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_1094;
}
else 
{
label_1094:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_1099;
}
else 
{
label_1099:; 
}
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_1123;
}
else 
{
goto label_1110;
}
}
else 
{
label_1110:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_1123;
}
else 
{
goto label_1114;
}
}
else 
{
label_1114:; 
if (t2_st < 1)
{
if (t2_st > -1)
{
__retres1 = 1;
goto label_1123;
}
else 
{
goto label_1118;
}
}
else 
{
label_1118:; 
__retres1 = 0;
label_1123:; 
 __return_1130 = __retres1;
}
tmp = __return_1130;
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
goto label_1165;
}
else 
{
goto label_1158;
}
}
else 
{
label_1158:; 
__retres1 = 0;
label_1165:; 
 __return_1168 = __retres1;
}
tmp = __return_1168;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_1174;
}
else 
{
goto label_1171;
}
}
else 
{
label_1171:; 
label_1174:; 
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
goto label_1194;
}
else 
{
goto label_1188;
}
}
else 
{
label_1188:; 
goto label_1186;
}
}
else 
{
goto label_1183;
}
}
else 
{
label_1183:; 
label_1186:; 
__retres1 = 0;
label_1194:; 
 __return_1197 = __retres1;
}
tmp___0 = __return_1197;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_1203;
}
else 
{
goto label_1200;
}
}
else 
{
label_1200:; 
label_1203:; 
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
goto label_1223;
}
else 
{
goto label_1217;
}
}
else 
{
label_1217:; 
goto label_1215;
}
}
else 
{
goto label_1212;
}
}
else 
{
label_1212:; 
label_1215:; 
__retres1 = 0;
label_1223:; 
 __return_1226 = __retres1;
}
tmp___1 = __return_1226;
if (tmp___1 < 1)
{
if (tmp___1 > -1)
{
t2_st = 0;
goto label_1232;
}
else 
{
goto label_1229;
}
}
else 
{
label_1229:; 
label_1232:; 
}
{
M_E = 2;
if (T1_E == 1)
{
T1_E = 2;
goto label_1246;
}
else 
{
label_1246:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_1251;
}
else 
{
label_1251:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_1256;
}
else 
{
label_1256:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_1261;
}
else 
{
label_1261:; 
}
goto label_1136;
}
}
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
goto label_1133;
}
}
else 
{
label_1133:; 
label_1136:; 
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
goto label_1293;
}
else 
{
goto label_1280;
}
}
else 
{
label_1280:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_1293;
}
else 
{
goto label_1284;
}
}
else 
{
label_1284:; 
if (t2_st < 1)
{
if (t2_st > -1)
{
__retres1 = 1;
goto label_1293;
}
else 
{
goto label_1288;
}
}
else 
{
label_1288:; 
__retres1 = 0;
label_1293:; 
 __return_1300 = __retres1;
}
tmp = __return_1300;
if (tmp < 1)
{
if (tmp > -1)
{
__retres2 = 0;
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
__retres2 = 1;
label_1308:; 
 __return_1311 = __retres2;
}
tmp___0 = __return_1311;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
}
else 
{
goto label_1314;
}
__retres1 = 0;
 __return_1324 = __retres1;
return 1;
}
else 
{
label_1314:; 
goto label_576;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
