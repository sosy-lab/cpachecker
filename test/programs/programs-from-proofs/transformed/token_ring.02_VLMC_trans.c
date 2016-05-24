void t1_started();
void t2_started();
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
int E_M  =    2;
int E_1  =    2;
int E_2  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
int is_transmit2_triggered(void) ;
void immediate_notify(void) ;
int token  ;
int __VERIFIER_nondet_int()  ;
int local  ;
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
int __return_32661;
int __return_32672;
int __return_32683;
int __return_32737;
int __return_33414;
int __return_33450;
int __return_33461;
int __return_33474;
int __return_33546;
int __return_33582;
int __return_33593;
int __return_33605;
int __return_33795;
int __return_33658;
int __return_33671;
int __return_33682;
int __return_33777;
int __return_33763;
int __return_33918;
int __return_33931;
int __return_33944;
int __return_34067;
int __return_34303;
int __return_34316;
int __return_34392;
int __return_34354;
int __return_34120;
int __return_34133;
int __return_34146;
int __return_32981;
int __return_33017;
int __return_33028;
int __return_33042;
int __return_33355;
int __return_33095;
int __return_33108;
int __return_33119;
int __return_33337;
int __return_33176;
int __return_33190;
int __return_33203;
int __return_33233;
int __return_33281;
int __return_33292;
int __return_33306;
int __return_32773;
int __return_32784;
int __return_32795;
int __return_33811;
int __return_33371;
int __return_33529;
int __return_32969;
int __return_33964;
int __return_33977;
int __return_33990;
int __return_34079;
int __return_34275;
int __return_34288;
int __return_34393;
int __return_34381;
int __return_34166;
int __return_34179;
int __return_34192;
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
m_st = 0;
t1_st = 0;
t2_st = 0;
}
{
if (!(M_E == 0))
{
label_32623:; 
if (!(T1_E == 0))
{
label_32630:; 
if (!(T2_E == 0))
{
label_32637:; 
}
else 
{
T2_E = 1;
goto label_32637;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_32661 = __retres1;
}
tmp = __return_32661;
{
int __retres1 ;
__retres1 = 0;
 __return_32672 = __retres1;
}
tmp___0 = __return_32672;
{
int __retres1 ;
__retres1 = 0;
 __return_32683 = __retres1;
}
tmp___1 = __return_32683;
}
{
if (!(M_E == 1))
{
label_32694:; 
if (!(T1_E == 1))
{
label_32701:; 
if (!(T2_E == 1))
{
label_32708:; 
}
else 
{
T2_E = 2;
goto label_32708;
}
kernel_st = 1;
{
int tmp ;
label_32728:; 
{
int __retres1 ;
__retres1 = 1;
 __return_32737 = __retres1;
}
tmp = __return_32737;
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
goto label_32728;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_32953:; 
{
int __retres1 ;
__retres1 = 1;
 __return_33414 = __retres1;
}
tmp = __return_33414;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_32953;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_33062;
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
{
int __retres1 ;
__retres1 = 0;
 __return_33450 = __retres1;
}
tmp = __return_33450;
{
int __retres1 ;
__retres1 = 0;
 __return_33461 = __retres1;
}
tmp___0 = __return_33461;
{
int __retres1 ;
__retres1 = 0;
 __return_33474 = __retres1;
}
tmp___1 = __return_33474;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_33376;
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
label_32859:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_33546 = __retres1;
}
tmp = __return_33546;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_32859;
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
{
int __retres1 ;
__retres1 = 0;
 __return_33582 = __retres1;
}
tmp = __return_33582;
{
int __retres1 ;
__retres1 = 1;
 __return_33593 = __retres1;
}
tmp___0 = __return_33593;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_33605 = __retres1;
}
tmp___1 = __return_33605;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_33622:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_33795 = __retres1;
}
tmp = __return_33795;
goto label_33622;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_33145;
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
{
int __retres1 ;
__retres1 = 0;
 __return_33658 = __retres1;
}
tmp = __return_33658;
{
int __retres1 ;
__retres1 = 0;
 __return_33671 = __retres1;
}
tmp___0 = __return_33671;
{
int __retres1 ;
__retres1 = 0;
 __return_33682 = __retres1;
}
tmp___1 = __return_33682;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_33702:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_33777 = __retres1;
}
tmp = __return_33777;
goto label_33702;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
{
int __retres1 ;
__retres1 = 0;
 __return_33763 = __retres1;
}
tmp = __return_33763;
}
label_33825:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (!(M_E == 0))
{
label_33878:; 
if (!(T1_E == 0))
{
label_33885:; 
if (!(T2_E == 0))
{
label_33892:; 
}
else 
{
T2_E = 1;
goto label_33892;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_33918 = __retres1;
}
tmp = __return_33918;
{
int __retres1 ;
__retres1 = 0;
 __return_33931 = __retres1;
}
tmp___0 = __return_33931;
{
int __retres1 ;
__retres1 = 0;
 __return_33944 = __retres1;
}
tmp___1 = __return_33944;
}
{
if (!(M_E == 1))
{
label_34032:; 
if (!(T1_E == 1))
{
label_34039:; 
if (!(T2_E == 1))
{
label_34046:; 
}
else 
{
T2_E = 2;
goto label_34046;
}
{
int __retres1 ;
__retres1 = 0;
 __return_34067 = __retres1;
}
tmp = __return_34067;
if (!(tmp == 0))
{
label_34088:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_34303 = __retres1;
}
tmp = __return_34303;
if (!(tmp == 0))
{
__retres2 = 0;
label_34311:; 
 __return_34316 = __retres2;
}
else 
{
__retres2 = 1;
goto label_34311;
}
tmp___0 = __return_34316;
if (!(tmp___0 == 0))
{
}
else 
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_34354 = __retres1;
}
tmp = __return_34354;
}
goto label_33825;
}
__retres1 = 0;
 __return_34392 = __retres1;
return 1;
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
{
int __retres1 ;
__retres1 = 0;
 __return_34120 = __retres1;
}
tmp = __return_34120;
{
int __retres1 ;
__retres1 = 0;
 __return_34133 = __retres1;
}
tmp___0 = __return_34133;
{
int __retres1 ;
__retres1 = 0;
 __return_34146 = __retres1;
}
tmp___1 = __return_34146;
}
{
if (!(M_E == 1))
{
label_34234:; 
if (!(T1_E == 1))
{
label_34241:; 
if (!(T2_E == 1))
{
label_34248:; 
}
else 
{
T2_E = 2;
goto label_34248;
}
goto label_34088;
}
else 
{
T1_E = 2;
goto label_34241;
}
}
else 
{
M_E = 2;
goto label_34234;
}
}
}
}
else 
{
T1_E = 2;
goto label_34039;
}
}
else 
{
M_E = 2;
goto label_34032;
}
}
}
else 
{
T1_E = 1;
goto label_33885;
}
}
else 
{
M_E = 1;
goto label_33878;
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
label_32955:; 
{
int __retres1 ;
__retres1 = 1;
 __return_32981 = __retres1;
}
tmp = __return_32981;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
label_33062:; 
goto label_32955;
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
{
int __retres1 ;
__retres1 = 0;
 __return_33017 = __retres1;
}
tmp = __return_33017;
{
int __retres1 ;
__retres1 = 1;
 __return_33028 = __retres1;
}
tmp___0 = __return_33028;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_33042 = __retres1;
}
tmp___1 = __return_33042;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_33055:; 
label_33059:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_33145:; 
{
int __retres1 ;
__retres1 = 1;
 __return_33355 = __retres1;
}
tmp = __return_33355;
goto label_33059;
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
{
int __retres1 ;
__retres1 = 0;
 __return_33095 = __retres1;
}
tmp = __return_33095;
{
int __retres1 ;
__retres1 = 0;
 __return_33108 = __retres1;
}
tmp___0 = __return_33108;
{
int __retres1 ;
__retres1 = 1;
 __return_33119 = __retres1;
}
tmp___1 = __return_33119;
t2_st = 0;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_33140:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_33337 = __retres1;
}
tmp = __return_33337;
goto label_33140;
}
else 
{
t2_st = 1;
{
t2_started();
token = token + 1;
E_M = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 1;
 __return_33176 = __retres1;
}
tmp = __return_33176;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_33190 = __retres1;
}
tmp___0 = __return_33190;
{
int __retres1 ;
__retres1 = 0;
 __return_33203 = __retres1;
}
tmp___1 = __return_33203;
}
}
E_M = 2;
t2_pc = 1;
t2_st = 2;
}
label_33223:; 
{
int __retres1 ;
__retres1 = 1;
 __return_33233 = __retres1;
}
tmp = __return_33233;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_33223;
}
else 
{
m_st = 1;
{
if (token != (local + 2))
{
{
}
goto label_33253;
}
else 
{
label_33253:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_33281 = __retres1;
}
tmp = __return_33281;
{
int __retres1 ;
__retres1 = 1;
 __return_33292 = __retres1;
}
tmp___0 = __return_33292;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_33306 = __retres1;
}
tmp___1 = __return_33306;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_33055;
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
{
int __retres1 ;
__retres1 = 0;
 __return_32773 = __retres1;
}
tmp = __return_32773;
{
int __retres1 ;
__retres1 = 0;
 __return_32784 = __retres1;
}
tmp___0 = __return_32784;
{
int __retres1 ;
__retres1 = 0;
 __return_32795 = __retres1;
}
tmp___1 = __return_32795;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_32812:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_33811 = __retres1;
}
tmp = __return_33811;
goto label_32812;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_32954:; 
{
int __retres1 ;
__retres1 = 1;
 __return_33371 = __retres1;
}
tmp = __return_33371;
label_33376:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_32954;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_32956;
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
label_32860:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_33529 = __retres1;
}
tmp = __return_33529;
goto label_32860;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_32956:; 
{
int __retres1 ;
__retres1 = 0;
 __return_32969 = __retres1;
}
tmp = __return_32969;
}
label_33824:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (!(M_E == 0))
{
label_33847:; 
if (!(T1_E == 0))
{
label_33854:; 
if (!(T2_E == 0))
{
label_33861:; 
}
else 
{
T2_E = 1;
goto label_33861;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_33964 = __retres1;
}
tmp = __return_33964;
{
int __retres1 ;
__retres1 = 0;
 __return_33977 = __retres1;
}
tmp___0 = __return_33977;
{
int __retres1 ;
__retres1 = 0;
 __return_33990 = __retres1;
}
tmp___1 = __return_33990;
}
{
if (!(M_E == 1))
{
label_34001:; 
if (!(T1_E == 1))
{
label_34008:; 
if (!(T2_E == 1))
{
label_34015:; 
}
else 
{
T2_E = 2;
goto label_34015;
}
{
int __retres1 ;
__retres1 = 0;
 __return_34079 = __retres1;
}
tmp = __return_34079;
if (!(tmp == 0))
{
label_34089:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_34275 = __retres1;
}
tmp = __return_34275;
if (!(tmp == 0))
{
__retres2 = 0;
label_34283:; 
 __return_34288 = __retres2;
}
else 
{
__retres2 = 1;
goto label_34283;
}
tmp___0 = __return_34288;
if (!(tmp___0 == 0))
{
}
else 
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_34381 = __retres1;
}
tmp = __return_34381;
}
goto label_33824;
}
__retres1 = 0;
 __return_34393 = __retres1;
return 1;
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
{
int __retres1 ;
__retres1 = 0;
 __return_34166 = __retres1;
}
tmp = __return_34166;
{
int __retres1 ;
__retres1 = 0;
 __return_34179 = __retres1;
}
tmp___0 = __return_34179;
{
int __retres1 ;
__retres1 = 0;
 __return_34192 = __retres1;
}
tmp___1 = __return_34192;
}
{
if (!(M_E == 1))
{
label_34203:; 
if (!(T1_E == 1))
{
label_34210:; 
if (!(T2_E == 1))
{
label_34217:; 
}
else 
{
T2_E = 2;
goto label_34217;
}
goto label_34089;
}
else 
{
T1_E = 2;
goto label_34210;
}
}
else 
{
M_E = 2;
goto label_34203;
}
}
}
}
else 
{
T1_E = 2;
goto label_34008;
}
}
else 
{
M_E = 2;
goto label_34001;
}
}
}
else 
{
T1_E = 1;
goto label_33854;
}
}
else 
{
M_E = 1;
goto label_33847;
}
}
}
}
}
}
else 
{
T1_E = 2;
goto label_32701;
}
}
else 
{
M_E = 2;
goto label_32694;
}
}
}
else 
{
T1_E = 1;
goto label_32630;
}
}
else 
{
M_E = 1;
goto label_32623;
}
}
}
}
