extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
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
int E_1  =    2;
int E_2  =    2;
int E_3  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
int is_transmit2_triggered(void) ;
int is_transmit3_triggered(void) ;
void immediate_notify(void) ;
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
int __return_634;
int __return_661;
int __return_688;
int __return_715;
int __return_816;
int __return_1188;
int __return_1215;
int __return_1242;
int __return_1269;
int __return_1030;
int __return_1057;
int __return_1084;
int __return_1111;
int __return_874;
int __return_901;
int __return_928;
int __return_955;
int __return_1424;
int __return_1451;
int __return_1478;
int __return_1505;
int __return_1595;
int __return_1805;
int __return_1817;
int __return_1830;
int __return_1628;
int __return_1655;
int __return_1682;
int __return_1709;
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
if (!(m_i == 1))
{
m_st = 2;
label_534:; 
if (!(t1_i == 1))
{
t1_st = 2;
label_543:; 
if (!(t2_i == 1))
{
t2_st = 2;
label_552:; 
if (!(t3_i == 1))
{
t3_st = 2;
label_561:; 
}
else 
{
t3_st = 0;
goto label_561;
}
{
if (!(T1_E == 0))
{
label_574:; 
if (!(T2_E == 0))
{
label_581:; 
if (!(T3_E == 0))
{
label_588:; 
if (!(E_1 == 0))
{
label_595:; 
if (!(E_2 == 0))
{
label_602:; 
if (!(E_3 == 0))
{
label_609:; 
}
else 
{
E_3 = 1;
goto label_609;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (!(m_pc == 1))
{
label_627:; 
__retres1 = 0;
 __return_634 = __retres1;
}
else 
{
goto label_627;
}
tmp = __return_634;
if (!(tmp == 0))
{
m_st = 0;
label_641:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_649:; 
__retres1 = 0;
label_657:; 
 __return_661 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_649;
}
else 
{
__retres1 = 1;
goto label_657;
}
}
tmp___0 = __return_661;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_668:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_676:; 
__retres1 = 0;
label_684:; 
 __return_688 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_676;
}
else 
{
__retres1 = 1;
goto label_684;
}
}
tmp___1 = __return_688;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_695:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_703:; 
__retres1 = 0;
label_711:; 
 __return_715 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_703;
}
else 
{
__retres1 = 1;
goto label_711;
}
}
tmp___2 = __return_715;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_722:; 
}
else 
{
goto label_722;
}
{
if (!(T1_E == 1))
{
label_733:; 
if (!(T2_E == 1))
{
label_740:; 
if (!(T3_E == 1))
{
label_747:; 
if (!(E_1 == 1))
{
label_754:; 
if (!(E_2 == 1))
{
label_761:; 
if (!(E_3 == 1))
{
label_768:; 
}
else 
{
E_3 = 2;
goto label_768;
}
label_774:; 
kernel_st = 1;
{
int tmp ;
label_782:; 
{
int __retres1 ;
if (!(m_st == 0))
{
if (!(t1_st == 0))
{
if (!(t2_st == 0))
{
if (!(t3_st == 0))
{
__retres1 = 0;
label_803:; 
 __return_816 = __retres1;
}
else 
{
__retres1 = 1;
goto label_803;
}
tmp = __return_816;
if (!(tmp == 0))
{
if (!(m_st == 0))
{
label_825:; 
if (!(t1_st == 0))
{
label_986:; 
if (!(t2_st == 0))
{
label_1144:; 
if (!(t3_st == 0))
{
label_1302:; 
goto label_782;
}
else 
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_1341;
}
else 
{
t3_st = 1;
{
if (!(t3_pc == 0))
{
if (!(t3_pc == 1))
{
label_1320:; 
goto label_1329;
}
else 
{
{
__VERIFIER_error();
}
label_1329:; 
t3_pc = 1;
t3_st = 2;
}
label_1341:; 
goto label_1302;
}
else 
{
goto label_1320;
}
}
}
}
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_1296;
}
else 
{
t2_st = 1;
{
if (!(t2_pc == 0))
{
if (!(t2_pc == 1))
{
label_1162:; 
goto label_1284;
}
else 
{
E_3 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (!(m_pc == 1))
{
label_1181:; 
__retres1 = 0;
 __return_1188 = __retres1;
}
else 
{
goto label_1181;
}
tmp = __return_1188;
if (!(tmp == 0))
{
m_st = 0;
label_1195:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_1203:; 
__retres1 = 0;
label_1211:; 
 __return_1215 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_1203;
}
else 
{
__retres1 = 1;
goto label_1211;
}
}
tmp___0 = __return_1215;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_1222:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_1230:; 
__retres1 = 0;
label_1238:; 
 __return_1242 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_1230;
}
else 
{
__retres1 = 1;
goto label_1238;
}
}
tmp___1 = __return_1242;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_1249:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_1257:; 
__retres1 = 0;
label_1265:; 
 __return_1269 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_1257;
}
else 
{
__retres1 = 1;
goto label_1265;
}
}
tmp___2 = __return_1269;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_1276:; 
}
else 
{
goto label_1276;
}
}
E_3 = 2;
label_1284:; 
t2_pc = 1;
t2_st = 2;
}
else 
{
goto label_1249;
}
label_1296:; 
goto label_1144;
}
}
else 
{
goto label_1222;
}
}
}
else 
{
goto label_1195;
}
}
}
}
}
}
else 
{
goto label_1162;
}
}
}
}
}
else 
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_1138;
}
else 
{
t1_st = 1;
{
if (!(t1_pc == 0))
{
if (!(t1_pc == 1))
{
label_1004:; 
goto label_1126;
}
else 
{
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (!(m_pc == 1))
{
label_1023:; 
__retres1 = 0;
 __return_1030 = __retres1;
}
else 
{
goto label_1023;
}
tmp = __return_1030;
if (!(tmp == 0))
{
m_st = 0;
label_1037:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_1045:; 
__retres1 = 0;
label_1053:; 
 __return_1057 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_1045;
}
else 
{
__retres1 = 1;
goto label_1053;
}
}
tmp___0 = __return_1057;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_1064:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_1072:; 
__retres1 = 0;
label_1080:; 
 __return_1084 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_1072;
}
else 
{
__retres1 = 1;
goto label_1080;
}
}
tmp___1 = __return_1084;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_1091:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_1099:; 
__retres1 = 0;
label_1107:; 
 __return_1111 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_1099;
}
else 
{
__retres1 = 1;
goto label_1107;
}
}
tmp___2 = __return_1111;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_1118:; 
}
else 
{
goto label_1118;
}
}
E_2 = 2;
label_1126:; 
t1_pc = 1;
t1_st = 2;
}
else 
{
goto label_1091;
}
label_1138:; 
goto label_986;
}
}
else 
{
goto label_1064;
}
}
}
else 
{
goto label_1037;
}
}
}
}
}
}
else 
{
goto label_1004;
}
}
}
}
}
else 
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_980;
}
else 
{
m_st = 1;
{
if (!(m_pc == 0))
{
if (!(m_pc == 1))
{
label_843:; 
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (!(m_pc == 1))
{
label_867:; 
__retres1 = 0;
 __return_874 = __retres1;
}
else 
{
goto label_867;
}
tmp = __return_874;
if (!(tmp == 0))
{
m_st = 0;
label_881:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_889:; 
__retres1 = 0;
label_897:; 
 __return_901 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_889;
}
else 
{
__retres1 = 1;
goto label_897;
}
}
tmp___0 = __return_901;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_908:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_916:; 
__retres1 = 0;
label_924:; 
 __return_928 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_916;
}
else 
{
__retres1 = 1;
goto label_924;
}
}
tmp___1 = __return_928;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_935:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_943:; 
__retres1 = 0;
label_951:; 
 __return_955 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_943;
}
else 
{
__retres1 = 1;
goto label_951;
}
}
tmp___2 = __return_955;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_962:; 
}
else 
{
goto label_962;
}
}
E_1 = 2;
goto label_846;
}
else 
{
goto label_935;
}
}
}
else 
{
goto label_908;
}
}
}
else 
{
goto label_881;
}
}
}
}
}
else 
{
label_846:; 
m_pc = 1;
m_st = 2;
}
label_980:; 
goto label_825;
}
else 
{
goto label_843;
}
}
}
}
}
else 
{
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (!(T1_E == 0))
{
label_1364:; 
if (!(T2_E == 0))
{
label_1371:; 
if (!(T3_E == 0))
{
label_1378:; 
if (!(E_1 == 0))
{
label_1385:; 
if (!(E_2 == 0))
{
label_1392:; 
if (!(E_3 == 0))
{
label_1399:; 
}
else 
{
E_3 = 1;
goto label_1399;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (!(m_pc == 1))
{
label_1417:; 
__retres1 = 0;
 __return_1424 = __retres1;
}
else 
{
goto label_1417;
}
tmp = __return_1424;
if (!(tmp == 0))
{
m_st = 0;
label_1431:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_1439:; 
__retres1 = 0;
label_1447:; 
 __return_1451 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_1439;
}
else 
{
__retres1 = 1;
goto label_1447;
}
}
tmp___0 = __return_1451;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_1458:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_1466:; 
__retres1 = 0;
label_1474:; 
 __return_1478 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_1466;
}
else 
{
__retres1 = 1;
goto label_1474;
}
}
tmp___1 = __return_1478;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_1485:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_1493:; 
__retres1 = 0;
label_1501:; 
 __return_1505 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_1493;
}
else 
{
__retres1 = 1;
goto label_1501;
}
}
tmp___2 = __return_1505;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_1512:; 
}
else 
{
goto label_1512;
}
{
if (!(T1_E == 1))
{
label_1523:; 
if (!(T2_E == 1))
{
label_1530:; 
if (!(T3_E == 1))
{
label_1537:; 
if (!(E_1 == 1))
{
label_1544:; 
if (!(E_2 == 1))
{
label_1551:; 
if (!(E_3 == 1))
{
label_1558:; 
}
else 
{
E_3 = 2;
goto label_1558;
}
{
int __retres1 ;
if (!(m_st == 0))
{
if (!(t1_st == 0))
{
if (!(t2_st == 0))
{
if (!(t3_st == 0))
{
__retres1 = 0;
label_1582:; 
 __return_1595 = __retres1;
}
else 
{
__retres1 = 1;
goto label_1582;
}
tmp = __return_1595;
if (!(tmp == 0))
{
label_1600:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
if (!(m_st == 0))
{
if (!(t1_st == 0))
{
if (!(t2_st == 0))
{
if (!(t3_st == 0))
{
__retres1 = 0;
label_1792:; 
 __return_1805 = __retres1;
}
else 
{
__retres1 = 1;
goto label_1792;
}
tmp = __return_1805;
if (!(tmp == 0))
{
__retres2 = 0;
label_1812:; 
 __return_1817 = __retres2;
}
else 
{
__retres2 = 1;
goto label_1812;
}
tmp___0 = __return_1817;
if (!(tmp___0 == 0))
{
}
else 
{
goto label_774;
}
__retres1 = 0;
 __return_1830 = __retres1;
return 1;
}
else 
{
__retres1 = 1;
goto label_1792;
}
}
else 
{
__retres1 = 1;
goto label_1792;
}
}
else 
{
__retres1 = 1;
goto label_1792;
}
}
}
}
else 
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
{
int __retres1 ;
if (!(m_pc == 1))
{
__retres1 = 0;
label_1624:; 
 __return_1628 = __retres1;
}
else 
{
__retres1 = 1;
goto label_1624;
}
tmp = __return_1628;
if (!(tmp == 0))
{
m_st = 0;
label_1635:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_1643:; 
__retres1 = 0;
label_1651:; 
 __return_1655 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_1643;
}
else 
{
__retres1 = 1;
goto label_1651;
}
}
tmp___0 = __return_1655;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_1662:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_1670:; 
__retres1 = 0;
label_1678:; 
 __return_1682 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_1670;
}
else 
{
__retres1 = 1;
goto label_1678;
}
}
tmp___1 = __return_1682;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_1689:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_1697:; 
__retres1 = 0;
label_1705:; 
 __return_1709 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_1697;
}
else 
{
__retres1 = 1;
goto label_1705;
}
}
tmp___2 = __return_1709;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_1716:; 
}
else 
{
goto label_1716;
}
{
M_E = 2;
if (!(T1_E == 1))
{
label_1728:; 
if (!(T2_E == 1))
{
label_1735:; 
if (!(T3_E == 1))
{
label_1742:; 
if (!(E_1 == 1))
{
label_1749:; 
if (!(E_2 == 1))
{
label_1756:; 
if (!(E_3 == 1))
{
label_1763:; 
}
else 
{
E_3 = 2;
goto label_1763;
}
goto label_1600;
}
else 
{
E_2 = 2;
goto label_1756;
}
}
else 
{
E_1 = 2;
goto label_1749;
}
}
else 
{
T3_E = 2;
goto label_1742;
}
}
else 
{
T2_E = 2;
goto label_1735;
}
}
else 
{
T1_E = 2;
goto label_1728;
}
}
}
}
else 
{
goto label_1689;
}
}
}
else 
{
goto label_1662;
}
}
}
else 
{
goto label_1635;
}
}
}
}
}
else 
{
__retres1 = 1;
goto label_1582;
}
}
else 
{
__retres1 = 1;
goto label_1582;
}
}
else 
{
__retres1 = 1;
goto label_1582;
}
}
}
else 
{
E_2 = 2;
goto label_1551;
}
}
else 
{
E_1 = 2;
goto label_1544;
}
}
else 
{
T3_E = 2;
goto label_1537;
}
}
else 
{
T2_E = 2;
goto label_1530;
}
}
else 
{
T1_E = 2;
goto label_1523;
}
}
}
}
else 
{
goto label_1485;
}
}
}
else 
{
goto label_1458;
}
}
}
else 
{
goto label_1431;
}
}
}
}
else 
{
E_2 = 1;
goto label_1392;
}
}
else 
{
E_1 = 1;
goto label_1385;
}
}
else 
{
T3_E = 1;
goto label_1378;
}
}
else 
{
T2_E = 1;
goto label_1371;
}
}
else 
{
T1_E = 1;
goto label_1364;
}
}
}
else 
{
__retres1 = 1;
goto label_803;
}
}
else 
{
__retres1 = 1;
goto label_803;
}
}
else 
{
__retres1 = 1;
goto label_803;
}
}
}
}
else 
{
E_2 = 2;
goto label_761;
}
}
else 
{
E_1 = 2;
goto label_754;
}
}
else 
{
T3_E = 2;
goto label_747;
}
}
else 
{
T2_E = 2;
goto label_740;
}
}
else 
{
T1_E = 2;
goto label_733;
}
}
}
}
else 
{
goto label_695;
}
}
}
else 
{
goto label_668;
}
}
}
else 
{
goto label_641;
}
}
}
}
else 
{
E_2 = 1;
goto label_602;
}
}
else 
{
E_1 = 1;
goto label_595;
}
}
else 
{
T3_E = 1;
goto label_588;
}
}
else 
{
T2_E = 1;
goto label_581;
}
}
else 
{
T1_E = 1;
goto label_574;
}
}
}
else 
{
t2_st = 0;
goto label_552;
}
}
else 
{
t1_st = 0;
goto label_543;
}
}
else 
{
m_st = 0;
goto label_534;
}
}
}
}
