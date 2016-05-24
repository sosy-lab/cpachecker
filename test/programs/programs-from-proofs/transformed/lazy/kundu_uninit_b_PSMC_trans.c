extern int __VERIFIER_nondet_int();
void error(void);
void immediate_notify(void) ;
int max_loop ;
int clk ;
int num ;
int i  ;
int e  ;
int timer ;
char data_0  ;
char data_1  ;
char read_data(int i___0 );
void write_data(int i___0 , char c );
int P_1_pc;
int P_1_st  ;
int P_1_i  ;
int P_1_ev  ;
void P_1(void);
int is_P_1_triggered(void);
int P_2_pc  ;
int P_2_st  ;
int P_2_i  ;
int P_2_ev  ;
void P_2(void);
int is_P_2_triggered(void);
int C_1_pc  ;
int C_1_st  ;
int C_1_i  ;
int C_1_ev  ;
int C_1_pr  ;
void C_1(void);
int is_C_1_triggered(void);
void update_channels(void);
void init_threads(void);
int exists_runnable_thread(void);
void eval(void);
void fire_delta_events(void);
void reset_delta_events(void);
void fire_time_events(void);
void reset_time_events(void);
void activate_threads(void);
int stop_simulation(void);
void start_simulation(void);
void init_model(void);
int main(void);
int __return_82783;
int __return_82805;
int __return_82876;
int __return_82806;
int __return_82844;
int __return_82933;
char __return_83045;
char __return_83046;
char __return_83047;
int __return_87063;
int __return_87279;
char __return_87571;
char __return_87572;
char __return_87573;
int __return_87701;
int __return_87819;
char __return_87947;
char __return_87948;
char __return_87949;
char __return_87431;
char __return_87432;
char __return_87433;
int __return_87998;
char __return_88132;
char __return_88133;
char __return_88134;
int __return_88254;
char __return_88382;
char __return_88383;
char __return_88384;
char __return_88202;
char __return_88203;
char __return_88204;
char __return_87501;
char __return_87502;
char __return_87503;
char __return_87641;
char __return_87642;
char __return_87643;
char __return_87221;
char __return_87222;
char __return_87223;
int __return_88441;
char __return_88564;
char __return_88565;
char __return_88566;
int __return_88676;
char __return_88793;
char __return_88794;
char __return_88795;
char __return_88627;
char __return_88628;
char __return_88629;
int __return_83122;
int __return_83192;
int __return_83241;
int __return_83383;
int __return_83384;
int __return_83242;
int __return_83284;
int __return_83285;
int __return_83193;
int __return_83219;
int __return_83350;
int __return_83351;
int __return_83953;
char __return_84245;
char __return_84246;
char __return_84247;
int __return_84381;
char __return_84509;
char __return_84510;
char __return_84511;
char __return_84105;
char __return_84106;
char __return_84107;
int __return_84560;
char __return_84694;
char __return_84695;
char __return_84696;
int __return_84816;
char __return_84944;
char __return_84945;
char __return_84946;
char __return_84764;
char __return_84765;
char __return_84766;
char __return_84175;
char __return_84176;
char __return_84177;
char __return_84315;
char __return_84316;
char __return_84317;
char __return_83638;
char __return_83639;
char __return_83640;
int __return_83220;
int __return_83317;
int __return_84993;
char __return_85285;
char __return_85286;
char __return_85287;
int __return_85413;
char __return_85541;
char __return_85542;
char __return_85543;
char __return_85145;
char __return_85146;
char __return_85147;
int __return_85592;
char __return_85726;
char __return_85727;
char __return_85728;
int __return_85848;
char __return_85976;
char __return_85977;
char __return_85978;
char __return_85796;
char __return_85797;
char __return_85798;
char __return_85215;
char __return_85216;
char __return_85217;
char __return_85355;
char __return_85356;
char __return_85357;
char __return_83568;
char __return_83569;
char __return_83570;
int __return_83696;
int __return_83833;
int __return_86025;
int __return_86141;
int __return_83318;
int __return_86253;
int __return_86660;
int __return_86730;
int __return_86779;
int __return_86921;
int __return_86922;
int __return_86780;
int __return_86822;
int __return_86823;
int __return_86731;
int __return_86757;
int __return_86888;
int __return_86889;
int __return_86758;
int __return_86855;
int __return_86856;
int __return_86323;
int __return_86372;
int __return_86514;
int __return_86515;
int __return_86373;
int __return_86415;
int __return_86416;
int __return_86324;
int __return_86350;
int __return_86481;
int __return_86482;
int __return_86351;
int __return_86448;
int __return_86449;
int main()
{
int count ;
int __retres2 ;
num = 0;
i = 0;
clk = 0;
max_loop = 8;
e;
timer = 0;
P_1_pc = 0;
P_2_pc = 0;
C_1_pc = 0;
count = 0;
{
P_1_i = 1;
P_2_i = 1;
C_1_i = 1;
}
{
int kernel_st ;
int tmp ;
int tmp___0 ;
kernel_st = 0;
{
}
{
P_1_st = 0;
P_2_st = 0;
C_1_st = 0;
}
{
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (((int)P_1_pc) == 1)
{
goto label_82777;
}
else 
{
label_82777:; 
__retres1 = 0;
 __return_82783 = __retres1;
}
tmp = __return_82783;
{
int __retres1 ;
if (((int)P_2_pc) == 1)
{
if (((int)P_2_ev) == 1)
{
__retres1 = 1;
 __return_82805 = __retres1;
}
else 
{
goto label_82800;
}
tmp___0 = __return_82805;
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
return 1;
}
else 
{
goto label_82859;
}
}
else 
{
label_82859:; 
if (((int)C_1_pc) == 2)
{
goto label_82867;
}
else 
{
label_82867:; 
__retres1 = 0;
 __return_82876 = __retres1;
}
tmp___1 = __return_82876;
}
goto label_82887;
}
}
else 
{
label_82800:; 
__retres1 = 0;
 __return_82806 = __retres1;
}
tmp___0 = __return_82806;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
return 1;
}
else 
{
goto label_82827;
}
}
else 
{
label_82827:; 
if (((int)C_1_pc) == 2)
{
goto label_82835;
}
else 
{
label_82835:; 
__retres1 = 0;
 __return_82844 = __retres1;
}
tmp___1 = __return_82844;
}
label_82887:; 
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
label_82909:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_82932;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_82932;
}
else 
{
__retres1 = 1;
label_82932:; 
 __return_82933 = __retres1;
}
tmp___2 = __return_82933;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_82947;
}
else 
{
label_82947:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_82982;
}
else 
{
P_2_st = 1;
{
{
int __tmp_1 = num;
char __tmp_2 = 'B';
int i___0 = __tmp_1;
char c = __tmp_2;
data_0 = c;
}
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
label_82978:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_83092;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_83012;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_83017:; 
num = num - 1;
{
int __tmp_3 = num;
int i___0 = __tmp_3;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_83045 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_83046 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_83047 = __retres3;
}
c = __return_83046;
goto label_83049;
c = __return_83047;
label_83049:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_83045;
goto label_83049;
label_83060:; 
label_87039:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_87062;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_87062;
}
else 
{
__retres1 = 1;
label_87062:; 
 __return_87063 = __retres1;
}
tmp___2 = __return_87063;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_87077;
}
else 
{
label_87077:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_87677;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_4 = num;
char __tmp_5 = 'B';
int i___0 = __tmp_4;
char c = __tmp_5;
if (i___0 == 0)
{
data_0 = c;
}
else 
{
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_87119;
label_87119:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_87119;
label_87135:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_87255:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_87278;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_87278;
}
else 
{
__retres1 = 1;
label_87278:; 
 __return_87279 = __retres1;
}
tmp___2 = __return_87279;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_87293;
}
else 
{
label_87293:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_87671;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_87534;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_87543:; 
num = num - 1;
{
int __tmp_6 = num;
int i___0 = __tmp_6;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_87571 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_87572 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_87573 = __retres3;
}
c = __return_87572;
goto label_87575;
c = __return_87573;
label_87575:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_87571;
goto label_87575;
label_87589:; 
label_87677:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_87700;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_87700;
}
else 
{
__retres1 = 1;
label_87700:; 
 __return_87701 = __retres1;
}
tmp___2 = __return_87701;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_87715;
}
else 
{
label_87715:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_87776;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_7 = num;
char __tmp_8 = 'B';
int i___0 = __tmp_7;
char c = __tmp_8;
if (i___0 == 0)
{
data_0 = c;
}
else 
{
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_87757;
label_87757:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_87757;
goto label_87135;
}
}
else 
{
P_2_st = 2;
}
goto label_87133;
}
}
}
else 
{
label_87776:; 
goto label_87677;
}
}
}
}
}
}
else 
{
label_87534:; 
if (i < max_loop)
{
goto label_87543;
}
else 
{
C_1_st = 2;
}
label_87587:; 
goto label_87677;
}
}
}
}
}
else 
{
label_87671:; 
label_87795:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_87818;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_87818;
}
else 
{
__retres1 = 1;
label_87818:; 
 __return_87819 = __retres1;
}
tmp___2 = __return_87819;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_87833;
}
else 
{
label_87833:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_87888;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_9 = num;
char __tmp_10 = 'B';
int i___0 = __tmp_9;
char c = __tmp_10;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_87871;
label_87871:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_87345;
}
else 
{
P_2_st = 2;
}
goto label_87343;
}
}
}
else 
{
label_87888:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_87968;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_87910;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_87919:; 
num = num - 1;
{
int __tmp_11 = num;
int i___0 = __tmp_11;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_87947 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_87948 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_87949 = __retres3;
}
c = __return_87948;
goto label_87951;
c = __return_87949;
label_87951:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_87947;
goto label_87951;
goto label_87589;
}
}
else 
{
label_87910:; 
if (i < max_loop)
{
goto label_87919;
}
else 
{
C_1_st = 2;
}
goto label_87587;
}
}
}
}
}
else 
{
label_87968:; 
goto label_87795;
}
}
}
}
}
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_12 = num;
char __tmp_13 = 'B';
int i___0 = __tmp_12;
char c = __tmp_13;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_87331;
label_87331:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
label_87345:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_87667;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_87394;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_87403:; 
num = num - 1;
{
int __tmp_14 = num;
int i___0 = __tmp_14;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_87431 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_87432 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_87433 = __retres3;
}
c = __return_87432;
goto label_87435;
c = __return_87433;
label_87435:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_87431;
goto label_87435;
goto label_83060;
}
}
else 
{
label_87394:; 
if (i < max_loop)
{
goto label_87403;
}
else 
{
C_1_st = 2;
}
goto label_83060;
}
}
}
}
}
else 
{
label_87667:; 
label_87974:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_87997;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_87997;
}
else 
{
__retres1 = 1;
label_87997:; 
 __return_87998 = __retres1;
}
tmp___2 = __return_87998;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_88012;
}
else 
{
label_88012:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_88224;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_88095;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_88104:; 
num = num - 1;
{
int __tmp_15 = num;
int i___0 = __tmp_15;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_88132 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_88133 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_88134 = __retres3;
}
c = __return_88133;
goto label_88136;
c = __return_88134;
label_88136:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_88132;
goto label_88136;
goto label_87589;
}
}
else 
{
label_88095:; 
if (i < max_loop)
{
goto label_88104;
}
else 
{
C_1_st = 2;
}
goto label_87587;
}
}
}
}
}
else 
{
label_88224:; 
label_88230:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_88253;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_88253;
}
else 
{
__retres1 = 1;
label_88253:; 
 __return_88254 = __retres1;
}
tmp___2 = __return_88254;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_88268;
}
else 
{
label_88268:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_88323;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_16 = num;
char __tmp_17 = 'B';
int i___0 = __tmp_16;
char c = __tmp_17;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_88306;
label_88306:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_87345;
}
else 
{
P_2_st = 2;
}
goto label_87345;
}
}
}
else 
{
label_88323:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_88403;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_88345;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_88354:; 
num = num - 1;
{
int __tmp_18 = num;
int i___0 = __tmp_18;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_88382 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_88383 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_88384 = __retres3;
}
c = __return_88383;
goto label_88386;
c = __return_88384;
label_88386:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_88382;
goto label_88386;
goto label_87589;
}
}
else 
{
label_88345:; 
if (i < max_loop)
{
goto label_88354;
}
else 
{
C_1_st = 2;
}
goto label_87587;
}
}
}
}
}
else 
{
label_88403:; 
goto label_88230;
}
}
}
}
}
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_19 = num;
char __tmp_20 = 'B';
int i___0 = __tmp_19;
char c = __tmp_20;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_88050;
label_88050:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_87345;
}
else 
{
P_2_st = 2;
}
goto label_87345;
}
}
}
else 
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_88226;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_88165;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_88174:; 
num = num - 1;
{
int __tmp_21 = num;
int i___0 = __tmp_21;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_88202 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_88203 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_88204 = __retres3;
}
c = __return_88203;
goto label_88206;
c = __return_88204;
label_88206:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_88202;
goto label_88206;
goto label_83060;
}
}
else 
{
label_88165:; 
if (i < max_loop)
{
goto label_88174;
}
else 
{
C_1_st = 2;
}
goto label_83060;
}
}
}
}
}
else 
{
label_88226:; 
goto label_87974;
}
}
}
}
}
}
}
else 
{
P_2_st = 2;
}
label_87343:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_87669;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_87464;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_87473:; 
num = num - 1;
{
int __tmp_22 = num;
int i___0 = __tmp_22;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_87501 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_87502 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_87503 = __retres3;
}
c = __return_87502;
goto label_87505;
c = __return_87503;
label_87505:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_87501;
goto label_87505;
goto label_83060;
}
}
else 
{
label_87464:; 
if (i < max_loop)
{
goto label_87473;
}
else 
{
C_1_st = 2;
}
goto label_83060;
}
}
}
}
}
else 
{
label_87669:; 
goto label_87255;
}
}
}
}
else 
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_87673;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_87604;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_87613:; 
num = num - 1;
{
int __tmp_23 = num;
int i___0 = __tmp_23;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_87641 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_87642 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_87643 = __retres3;
}
c = __return_87642;
goto label_87645;
c = __return_87643;
label_87645:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_87641;
goto label_87645;
goto label_83060;
}
}
else 
{
label_87604:; 
if (i < max_loop)
{
goto label_87613;
}
else 
{
C_1_st = 2;
}
goto label_83060;
}
}
}
}
}
else 
{
label_87673:; 
goto label_87255;
}
}
}
}
}
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_87184;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_87193:; 
num = num - 1;
{
int __tmp_24 = num;
int i___0 = __tmp_24;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_87221 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_87222 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_87223 = __retres3;
}
c = __return_87222;
goto label_87225;
c = __return_87223;
label_87225:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_87221;
goto label_87225;
goto label_83060;
}
}
else 
{
label_87184:; 
if (i < max_loop)
{
goto label_87193;
}
else 
{
C_1_st = 2;
}
goto label_83060;
}
}
}
}
}
else 
{
goto label_87039;
}
}
}
else 
{
P_2_st = 2;
}
label_87133:; 
goto label_87039;
}
}
}
else 
{
goto label_87039;
}
}
}
}
}
}
else 
{
label_83012:; 
goto label_83017;
}
}
}
}
}
else 
{
label_83092:; 
label_88417:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_88440;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_88440;
}
else 
{
__retres1 = 1;
label_88440:; 
 __return_88441 = __retres1;
}
tmp___2 = __return_88441;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_88455;
}
else 
{
label_88455:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_88646;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_88531;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_88536:; 
num = num - 1;
{
int __tmp_25 = num;
int i___0 = __tmp_25;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_88564 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_88565 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_88566 = __retres3;
}
c = __return_88565;
goto label_88568;
c = __return_88566;
label_88568:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_88564;
goto label_88568;
goto label_87589;
}
}
else 
{
label_88531:; 
goto label_88536;
}
}
}
}
}
else 
{
label_88646:; 
label_88652:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_88675;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_88675;
}
else 
{
__retres1 = 1;
label_88675:; 
 __return_88676 = __retres1;
}
tmp___2 = __return_88676;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_88690;
}
else 
{
label_88690:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_88738;
}
else 
{
P_2_st = 1;
{
{
int __tmp_26 = num;
char __tmp_27 = 'B';
int i___0 = __tmp_26;
char c = __tmp_27;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_88724;
label_88724:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_82978;
}
}
}
else 
{
label_88738:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_88811;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_88760;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_88765:; 
num = num - 1;
{
int __tmp_28 = num;
int i___0 = __tmp_28;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_88793 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_88794 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_88795 = __retres3;
}
c = __return_88794;
goto label_88797;
c = __return_88795;
label_88797:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_88793;
goto label_88797;
goto label_87589;
}
}
else 
{
label_88760:; 
goto label_88765;
}
}
}
}
}
else 
{
label_88811:; 
goto label_88652;
}
}
}
}
}
}
}
else 
{
P_2_st = 1;
{
{
int __tmp_29 = num;
char __tmp_30 = 'B';
int i___0 = __tmp_29;
char c = __tmp_30;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_88489;
label_88489:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_82978;
}
}
}
else 
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_88648;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_88594;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_88599:; 
num = num - 1;
{
int __tmp_31 = num;
int i___0 = __tmp_31;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_88627 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_88628 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_88629 = __retres3;
}
c = __return_88628;
goto label_88631;
c = __return_88629;
label_88631:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_88627;
goto label_88631;
goto label_83060;
}
}
else 
{
label_88594:; 
goto label_88599;
}
}
}
}
}
else 
{
label_88648:; 
goto label_88417;
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
label_82982:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_83094;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_83075;
}
else 
{
label_83075:; 
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
}
label_83098:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_83121;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_83121;
}
else 
{
__retres1 = 1;
label_83121:; 
 __return_83122 = __retres1;
}
tmp___2 = __return_83122;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_83136;
}
else 
{
label_83136:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_83485;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_32 = num;
char __tmp_33 = 'B';
int i___0 = __tmp_32;
char c = __tmp_33;
data_0 = c;
}
num = num + 1;
timer = 0;
e = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (((int)P_1_pc) == 1)
{
if (((int)P_1_ev) == 1)
{
__retres1 = 1;
 __return_83192 = __retres1;
}
else 
{
goto label_83187;
}
tmp = __return_83192;
P_1_st = 0;
{
int __retres1 ;
if (((int)P_2_pc) == 1)
{
if (((int)P_2_ev) == 1)
{
__retres1 = 1;
 __return_83241 = __retres1;
}
else 
{
goto label_83236;
}
tmp___0 = __return_83241;
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_83382;
}
else 
{
goto label_83367;
}
}
else 
{
label_83367:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_83382:; 
 __return_83383 = __retres1;
}
else 
{
goto label_83375;
}
tmp___1 = __return_83383;
C_1_st = 0;
}
else 
{
label_83375:; 
__retres1 = 0;
 __return_83384 = __retres1;
}
label_83418:; 
tmp___1 = __return_83384;
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
goto label_83430;
}
goto label_83477;
}
else 
{
label_83236:; 
__retres1 = 0;
 __return_83242 = __retres1;
}
tmp___0 = __return_83242;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_83283;
}
else 
{
goto label_83268;
}
}
else 
{
label_83268:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_83283:; 
 __return_83284 = __retres1;
}
else 
{
goto label_83276;
}
tmp___1 = __return_83284;
C_1_st = 0;
}
else 
{
label_83276:; 
__retres1 = 0;
 __return_83285 = __retres1;
}
goto label_83422;
tmp___1 = __return_83285;
}
label_83426:; 
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
goto label_83473;
}
else 
{
label_83187:; 
__retres1 = 0;
 __return_83193 = __retres1;
}
tmp = __return_83193;
{
int __retres1 ;
if (((int)P_2_pc) == 1)
{
if (((int)P_2_ev) == 1)
{
__retres1 = 1;
 __return_83219 = __retres1;
}
else 
{
goto label_83214;
}
tmp___0 = __return_83219;
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_83349;
}
else 
{
goto label_83334;
}
}
else 
{
label_83334:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_83349:; 
 __return_83350 = __retres1;
}
else 
{
goto label_83342;
}
tmp___1 = __return_83350;
C_1_st = 0;
}
else 
{
label_83342:; 
__retres1 = 0;
 __return_83351 = __retres1;
}
goto label_83418;
tmp___1 = __return_83351;
}
label_83430:; 
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
else 
{
label_83214:; 
__retres1 = 0;
 __return_83220 = __retres1;
}
label_83473:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_83929:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_83952;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_83952;
}
else 
{
__retres1 = 1;
label_83952:; 
 __return_83953 = __retres1;
}
tmp___2 = __return_83953;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_83967;
}
else 
{
label_83967:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_84345;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_84208;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_84217:; 
num = num - 1;
{
int __tmp_34 = num;
int i___0 = __tmp_34;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_84245 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_84246 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_84247 = __retres3;
}
c = __return_84246;
goto label_84249;
c = __return_84247;
label_84249:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_84245;
goto label_84249;
label_84263:; 
goto label_83809;
}
}
else 
{
label_84208:; 
if (i < max_loop)
{
goto label_84217;
}
else 
{
C_1_st = 2;
}
label_84261:; 
goto label_83809;
}
}
}
}
}
else 
{
label_84345:; 
label_84357:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_84380;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_84380;
}
else 
{
__retres1 = 1;
label_84380:; 
 __return_84381 = __retres1;
}
tmp___2 = __return_84381;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_84395;
}
else 
{
label_84395:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_84450;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_35 = num;
char __tmp_36 = 'B';
int i___0 = __tmp_35;
char c = __tmp_36;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_84433;
label_84433:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_84019;
}
else 
{
P_2_st = 2;
}
goto label_84017;
}
}
}
else 
{
label_84450:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_84530;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_84472;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_84481:; 
num = num - 1;
{
int __tmp_37 = num;
int i___0 = __tmp_37;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_84509 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_84510 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_84511 = __retres3;
}
c = __return_84510;
goto label_84513;
c = __return_84511;
label_84513:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_84509;
goto label_84513;
goto label_84263;
}
}
else 
{
label_84472:; 
if (i < max_loop)
{
goto label_84481;
}
else 
{
C_1_st = 2;
}
goto label_84261;
}
}
}
}
}
else 
{
label_84530:; 
goto label_84357;
}
}
}
}
}
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_38 = num;
char __tmp_39 = 'B';
int i___0 = __tmp_38;
char c = __tmp_39;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_84005;
label_84005:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
label_84019:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_84341;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_84068;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_84077:; 
num = num - 1;
{
int __tmp_40 = num;
int i___0 = __tmp_40;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_84105 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_84106 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_84107 = __retres3;
}
c = __return_84106;
goto label_84109;
c = __return_84107;
label_84109:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_84105;
goto label_84109;
goto label_83586;
}
}
else 
{
label_84068:; 
if (i < max_loop)
{
goto label_84077;
}
else 
{
C_1_st = 2;
}
goto label_83584;
}
}
}
}
}
else 
{
label_84341:; 
label_84536:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_84559;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_84559;
}
else 
{
__retres1 = 1;
label_84559:; 
 __return_84560 = __retres1;
}
tmp___2 = __return_84560;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_84574;
}
else 
{
label_84574:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_84786;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_84657;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_84666:; 
num = num - 1;
{
int __tmp_41 = num;
int i___0 = __tmp_41;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_84694 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_84695 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_84696 = __retres3;
}
c = __return_84695;
goto label_84698;
c = __return_84696;
label_84698:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_84694;
goto label_84698;
goto label_84263;
}
}
else 
{
label_84657:; 
if (i < max_loop)
{
goto label_84666;
}
else 
{
C_1_st = 2;
}
goto label_84261;
}
}
}
}
}
else 
{
label_84786:; 
label_84792:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_84815;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_84815;
}
else 
{
__retres1 = 1;
label_84815:; 
 __return_84816 = __retres1;
}
tmp___2 = __return_84816;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_84830;
}
else 
{
label_84830:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_84885;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_42 = num;
char __tmp_43 = 'B';
int i___0 = __tmp_42;
char c = __tmp_43;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_84868;
label_84868:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_84019;
}
else 
{
P_2_st = 2;
}
goto label_84019;
}
}
}
else 
{
label_84885:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_84965;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_84907;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_84916:; 
num = num - 1;
{
int __tmp_44 = num;
int i___0 = __tmp_44;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_84944 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_84945 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_84946 = __retres3;
}
c = __return_84945;
goto label_84948;
c = __return_84946;
label_84948:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_84944;
goto label_84948;
goto label_84263;
}
}
else 
{
label_84907:; 
if (i < max_loop)
{
goto label_84916;
}
else 
{
C_1_st = 2;
}
goto label_84261;
}
}
}
}
}
else 
{
label_84965:; 
goto label_84792;
}
}
}
}
}
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_45 = num;
char __tmp_46 = 'B';
int i___0 = __tmp_45;
char c = __tmp_46;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_84612;
label_84612:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_84019;
}
else 
{
P_2_st = 2;
}
goto label_84019;
}
}
}
else 
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_84788;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_84727;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_84736:; 
num = num - 1;
{
int __tmp_47 = num;
int i___0 = __tmp_47;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_84764 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_84765 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_84766 = __retres3;
}
c = __return_84765;
goto label_84768;
c = __return_84766;
label_84768:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_84764;
goto label_84768;
goto label_83586;
}
}
else 
{
label_84727:; 
if (i < max_loop)
{
goto label_84736;
}
else 
{
C_1_st = 2;
}
goto label_83584;
}
}
}
}
}
else 
{
label_84788:; 
goto label_84536;
}
}
}
}
}
}
}
else 
{
P_2_st = 2;
}
label_84017:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_84343;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_84138;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_84147:; 
num = num - 1;
{
int __tmp_48 = num;
int i___0 = __tmp_48;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_84175 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_84176 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_84177 = __retres3;
}
c = __return_84176;
goto label_84179;
c = __return_84177;
label_84179:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_84175;
goto label_84179;
goto label_83586;
}
}
else 
{
label_84138:; 
if (i < max_loop)
{
goto label_84147;
}
else 
{
C_1_st = 2;
}
goto label_83584;
}
}
}
}
}
else 
{
label_84343:; 
goto label_83929;
}
}
}
}
else 
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_84347;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_84278;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_84287:; 
num = num - 1;
{
int __tmp_49 = num;
int i___0 = __tmp_49;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_84315 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_84316 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_84317 = __retres3;
}
c = __return_84316;
goto label_84319;
c = __return_84317;
label_84319:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_84315;
goto label_84319;
goto label_83586;
}
}
else 
{
label_84278:; 
if (i < max_loop)
{
goto label_84287;
}
else 
{
C_1_st = 2;
}
goto label_83584;
}
}
}
}
}
else 
{
label_84347:; 
goto label_83929;
}
}
}
}
}
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_83601;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_83610:; 
num = num - 1;
{
int __tmp_50 = num;
int i___0 = __tmp_50;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_83638 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_83639 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_83640 = __retres3;
}
c = __return_83639;
goto label_83642;
c = __return_83640;
label_83642:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_83638;
goto label_83642;
goto label_83586;
}
}
else 
{
label_83601:; 
if (i < max_loop)
{
goto label_83610;
}
else 
{
C_1_st = 2;
}
goto label_83584;
}
}
}
}
}
else 
{
goto label_83672;
}
tmp___0 = __return_83220;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_83316;
}
else 
{
goto label_83301;
}
}
else 
{
label_83301:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_83316:; 
 __return_83317 = __retres1;
}
else 
{
goto label_83309;
}
tmp___1 = __return_83317;
C_1_st = 0;
}
else 
{
label_83309:; 
__retres1 = 0;
 __return_83318 = __retres1;
}
label_83422:; 
tmp___1 = __return_83318;
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
goto label_83426;
}
label_83477:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_84969:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_84992;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_84992;
}
else 
{
__retres1 = 1;
label_84992:; 
 __return_84993 = __retres1;
}
tmp___2 = __return_84993;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_85007;
}
else 
{
label_85007:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_85383;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_85248;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_85257:; 
num = num - 1;
{
int __tmp_51 = num;
int i___0 = __tmp_51;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_85285 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_85286 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_85287 = __retres3;
}
c = __return_85286;
goto label_85289;
c = __return_85287;
label_85289:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_85285;
goto label_85289;
goto label_84263;
}
}
else 
{
label_85248:; 
if (i < max_loop)
{
goto label_85257;
}
else 
{
C_1_st = 2;
}
goto label_84261;
}
}
}
}
}
else 
{
label_85383:; 
label_85389:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_85412;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_85412;
}
else 
{
__retres1 = 1;
label_85412:; 
 __return_85413 = __retres1;
}
tmp___2 = __return_85413;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_85427;
}
else 
{
label_85427:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_85482;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_52 = num;
char __tmp_53 = 'B';
int i___0 = __tmp_52;
char c = __tmp_53;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_85465;
label_85465:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_85059;
}
else 
{
P_2_st = 2;
}
goto label_85057;
}
}
}
else 
{
label_85482:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_85562;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_85504;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_85513:; 
num = num - 1;
{
int __tmp_54 = num;
int i___0 = __tmp_54;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_85541 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_85542 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_85543 = __retres3;
}
c = __return_85542;
goto label_85545;
c = __return_85543;
label_85545:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_85541;
goto label_85545;
goto label_84263;
}
}
else 
{
label_85504:; 
if (i < max_loop)
{
goto label_85513;
}
else 
{
C_1_st = 2;
}
goto label_84261;
}
}
}
}
}
else 
{
label_85562:; 
goto label_85389;
}
}
}
}
}
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_55 = num;
char __tmp_56 = 'B';
int i___0 = __tmp_55;
char c = __tmp_56;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_85045;
label_85045:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
label_85059:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_85379;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_85108;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_85117:; 
num = num - 1;
{
int __tmp_57 = num;
int i___0 = __tmp_57;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_85145 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_85146 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_85147 = __retres3;
}
c = __return_85146;
goto label_85149;
c = __return_85147;
label_85149:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_85145;
goto label_85149;
goto label_83586;
}
}
else 
{
label_85108:; 
if (i < max_loop)
{
goto label_85117;
}
else 
{
C_1_st = 2;
}
goto label_83584;
}
}
}
}
}
else 
{
label_85379:; 
label_85568:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_85591;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_85591;
}
else 
{
__retres1 = 1;
label_85591:; 
 __return_85592 = __retres1;
}
tmp___2 = __return_85592;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_85606;
}
else 
{
label_85606:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_85818;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_85689;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_85698:; 
num = num - 1;
{
int __tmp_58 = num;
int i___0 = __tmp_58;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_85726 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_85727 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_85728 = __retres3;
}
c = __return_85727;
goto label_85730;
c = __return_85728;
label_85730:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_85726;
goto label_85730;
goto label_84263;
}
}
else 
{
label_85689:; 
if (i < max_loop)
{
goto label_85698;
}
else 
{
C_1_st = 2;
}
goto label_84261;
}
}
}
}
}
else 
{
label_85818:; 
label_85824:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_85847;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_85847;
}
else 
{
__retres1 = 1;
label_85847:; 
 __return_85848 = __retres1;
}
tmp___2 = __return_85848;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_85862;
}
else 
{
label_85862:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_85917;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_59 = num;
char __tmp_60 = 'B';
int i___0 = __tmp_59;
char c = __tmp_60;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_85900;
label_85900:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_85059;
}
else 
{
P_2_st = 2;
}
goto label_85059;
}
}
}
else 
{
label_85917:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_85997;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_85939;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_85948:; 
num = num - 1;
{
int __tmp_61 = num;
int i___0 = __tmp_61;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_85976 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_85977 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_85978 = __retres3;
}
c = __return_85977;
goto label_85980;
c = __return_85978;
label_85980:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_85976;
goto label_85980;
goto label_84263;
}
}
else 
{
label_85939:; 
if (i < max_loop)
{
goto label_85948;
}
else 
{
C_1_st = 2;
}
goto label_84261;
}
}
}
}
}
else 
{
label_85997:; 
goto label_85824;
}
}
}
}
}
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_62 = num;
char __tmp_63 = 'B';
int i___0 = __tmp_62;
char c = __tmp_63;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_85644;
label_85644:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_85059;
}
else 
{
P_2_st = 2;
}
goto label_85059;
}
}
}
else 
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_85820;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_85759;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_85768:; 
num = num - 1;
{
int __tmp_64 = num;
int i___0 = __tmp_64;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_85796 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_85797 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_85798 = __retres3;
}
c = __return_85797;
goto label_85800;
c = __return_85798;
label_85800:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_85796;
goto label_85800;
goto label_83586;
}
}
else 
{
label_85759:; 
if (i < max_loop)
{
goto label_85768;
}
else 
{
C_1_st = 2;
}
goto label_83584;
}
}
}
}
}
else 
{
label_85820:; 
goto label_85568;
}
}
}
}
}
}
}
else 
{
P_2_st = 2;
}
label_85057:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_85381;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_85178;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_85187:; 
num = num - 1;
{
int __tmp_65 = num;
int i___0 = __tmp_65;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_85215 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_85216 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_85217 = __retres3;
}
c = __return_85216;
goto label_85219;
c = __return_85217;
label_85219:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_85215;
goto label_85219;
goto label_83586;
}
}
else 
{
label_85178:; 
if (i < max_loop)
{
goto label_85187;
}
else 
{
C_1_st = 2;
}
goto label_83584;
}
}
}
}
}
else 
{
label_85381:; 
goto label_84969;
}
}
}
}
else 
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_85385;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_85318;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_85327:; 
num = num - 1;
{
int __tmp_66 = num;
int i___0 = __tmp_66;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_85355 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_85356 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_85357 = __retres3;
}
c = __return_85356;
goto label_85359;
c = __return_85357;
label_85359:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_85355;
goto label_85359;
goto label_83586;
}
}
else 
{
label_85318:; 
if (i < max_loop)
{
goto label_85327;
}
else 
{
C_1_st = 2;
}
goto label_83584;
}
}
}
}
}
else 
{
label_85385:; 
goto label_84969;
}
}
}
}
}
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_83531;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_83540:; 
num = num - 1;
{
int __tmp_67 = num;
int i___0 = __tmp_67;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_83568 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_83569 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_83570 = __retres3;
}
c = __return_83569;
goto label_83572;
c = __return_83570;
label_83572:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_83568;
goto label_83572;
label_83586:; 
label_83672:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_83695;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_83695;
}
else 
{
__retres1 = 1;
label_83695:; 
 __return_83696 = __retres1;
}
tmp___2 = __return_83696;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_83710;
}
else 
{
label_83710:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
label_83809:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_83832;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_83832;
}
else 
{
__retres1 = 1;
label_83832:; 
 __return_83833 = __retres1;
}
tmp___2 = __return_83833;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_83847;
}
else 
{
label_83847:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_83908;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_68 = num;
char __tmp_69 = 'B';
int i___0 = __tmp_68;
char c = __tmp_69;
if (i___0 == 0)
{
data_0 = c;
}
else 
{
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_83889;
label_83889:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_83889;
goto label_83473;
}
}
else 
{
P_2_st = 2;
}
goto label_83766;
}
}
}
else 
{
label_83908:; 
goto label_83809;
}
}
}
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_70 = num;
char __tmp_71 = 'B';
int i___0 = __tmp_70;
char c = __tmp_71;
if (i___0 == 0)
{
data_0 = c;
}
else 
{
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_83752;
label_83752:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_83752;
goto label_83473;
}
}
else 
{
P_2_st = 2;
}
label_83766:; 
goto label_83672;
}
}
}
else 
{
goto label_83672;
}
}
}
}
}
}
else 
{
label_83531:; 
if (i < max_loop)
{
goto label_83540;
}
else 
{
C_1_st = 2;
}
label_83584:; 
goto label_83672;
}
}
}
}
}
else 
{
label_86001:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_86024;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_86024;
}
else 
{
__retres1 = 1;
label_86024:; 
 __return_86025 = __retres1;
}
tmp___2 = __return_86025;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_86039;
}
else 
{
label_86039:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
label_86117:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_86140;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_86140;
}
else 
{
__retres1 = 1;
label_86140:; 
 __return_86141 = __retres1;
}
tmp___2 = __return_86141;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_86155;
}
else 
{
label_86155:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_86210;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_72 = num;
char __tmp_73 = 'B';
int i___0 = __tmp_72;
char c = __tmp_73;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_86193;
label_86193:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_83477;
}
else 
{
P_2_st = 2;
}
goto label_83477;
}
}
}
else 
{
label_86210:; 
goto label_86117;
}
}
}
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_74 = num;
char __tmp_75 = 'B';
int i___0 = __tmp_74;
char c = __tmp_75;
if (i___0 == 1)
{
data_1 = c;
}
else 
{
{
}
}
goto label_86077;
label_86077:; 
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
goto label_83477;
}
else 
{
P_2_st = 2;
}
goto label_83477;
}
}
}
else 
{
goto label_86001;
}
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
P_2_st = 2;
}
label_83471:; 
label_86229:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_86252;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_86252;
}
else 
{
__retres1 = 1;
label_86252:; 
 __return_86253 = __retres1;
}
tmp___2 = __return_86253;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_86267;
}
else 
{
label_86267:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
label_86636:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_86659;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_86659;
}
else 
{
__retres1 = 1;
label_86659:; 
 __return_86660 = __retres1;
}
tmp___2 = __return_86660;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_86674;
}
else 
{
label_86674:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_87020;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_76 = num;
char __tmp_77 = 'B';
int i___0 = __tmp_76;
char c = __tmp_77;
data_0 = c;
}
num = num + 1;
timer = 0;
e = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (((int)P_1_pc) == 1)
{
if (((int)P_1_ev) == 1)
{
__retres1 = 1;
 __return_86730 = __retres1;
}
else 
{
goto label_86725;
}
tmp = __return_86730;
P_1_st = 0;
{
int __retres1 ;
if (((int)P_2_pc) == 1)
{
if (((int)P_2_ev) == 1)
{
__retres1 = 1;
 __return_86779 = __retres1;
}
else 
{
goto label_86774;
}
tmp___0 = __return_86779;
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_86920;
}
else 
{
goto label_86905;
}
}
else 
{
label_86905:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_86920:; 
 __return_86921 = __retres1;
}
else 
{
goto label_86913;
}
tmp___1 = __return_86921;
C_1_st = 0;
}
else 
{
label_86913:; 
__retres1 = 0;
 __return_86922 = __retres1;
}
label_86956:; 
tmp___1 = __return_86922;
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
goto label_86968;
}
goto label_83477;
}
else 
{
label_86774:; 
__retres1 = 0;
 __return_86780 = __retres1;
}
tmp___0 = __return_86780;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_86821;
}
else 
{
goto label_86806;
}
}
else 
{
label_86806:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_86821:; 
 __return_86822 = __retres1;
}
else 
{
goto label_86814;
}
tmp___1 = __return_86822;
C_1_st = 0;
}
else 
{
label_86814:; 
__retres1 = 0;
 __return_86823 = __retres1;
}
goto label_86960;
tmp___1 = __return_86823;
}
label_86964:; 
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
goto label_83473;
}
else 
{
label_86725:; 
__retres1 = 0;
 __return_86731 = __retres1;
}
tmp = __return_86731;
{
int __retres1 ;
if (((int)P_2_pc) == 1)
{
if (((int)P_2_ev) == 1)
{
__retres1 = 1;
 __return_86757 = __retres1;
}
else 
{
goto label_86752;
}
tmp___0 = __return_86757;
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_86887;
}
else 
{
goto label_86872;
}
}
else 
{
label_86872:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_86887:; 
 __return_86888 = __retres1;
}
else 
{
goto label_86880;
}
tmp___1 = __return_86888;
C_1_st = 0;
}
else 
{
label_86880:; 
__retres1 = 0;
 __return_86889 = __retres1;
}
goto label_86956;
tmp___1 = __return_86889;
}
label_86968:; 
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
else 
{
label_86752:; 
__retres1 = 0;
 __return_86758 = __retres1;
}
goto label_83473;
tmp___0 = __return_86758;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_86854;
}
else 
{
goto label_86839;
}
}
else 
{
label_86839:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_86854:; 
 __return_86855 = __retres1;
}
else 
{
goto label_86847;
}
tmp___1 = __return_86855;
C_1_st = 0;
}
else 
{
label_86847:; 
__retres1 = 0;
 __return_86856 = __retres1;
}
label_86960:; 
tmp___1 = __return_86856;
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
goto label_86964;
}
goto label_83477;
}
}
}
}
}
else 
{
P_2_st = 2;
}
goto label_83471;
}
}
}
else 
{
label_87020:; 
goto label_86636;
}
}
}
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_78 = num;
char __tmp_79 = 'B';
int i___0 = __tmp_78;
char c = __tmp_79;
data_0 = c;
}
num = num + 1;
timer = 0;
e = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (((int)P_1_pc) == 1)
{
if (((int)P_1_ev) == 1)
{
__retres1 = 1;
 __return_86323 = __retres1;
}
else 
{
goto label_86318;
}
tmp = __return_86323;
P_1_st = 0;
{
int __retres1 ;
if (((int)P_2_pc) == 1)
{
if (((int)P_2_ev) == 1)
{
__retres1 = 1;
 __return_86372 = __retres1;
}
else 
{
goto label_86367;
}
tmp___0 = __return_86372;
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_86513;
}
else 
{
goto label_86498;
}
}
else 
{
label_86498:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_86513:; 
 __return_86514 = __retres1;
}
else 
{
goto label_86506;
}
tmp___1 = __return_86514;
C_1_st = 0;
}
else 
{
label_86506:; 
__retres1 = 0;
 __return_86515 = __retres1;
}
label_86549:; 
tmp___1 = __return_86515;
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
goto label_86561;
}
goto label_83477;
}
else 
{
label_86367:; 
__retres1 = 0;
 __return_86373 = __retres1;
}
tmp___0 = __return_86373;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_86414;
}
else 
{
goto label_86399;
}
}
else 
{
label_86399:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_86414:; 
 __return_86415 = __retres1;
}
else 
{
goto label_86407;
}
tmp___1 = __return_86415;
C_1_st = 0;
}
else 
{
label_86407:; 
__retres1 = 0;
 __return_86416 = __retres1;
}
goto label_86553;
tmp___1 = __return_86416;
}
label_86557:; 
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
goto label_83473;
}
else 
{
label_86318:; 
__retres1 = 0;
 __return_86324 = __retres1;
}
tmp = __return_86324;
{
int __retres1 ;
if (((int)P_2_pc) == 1)
{
if (((int)P_2_ev) == 1)
{
__retres1 = 1;
 __return_86350 = __retres1;
}
else 
{
goto label_86345;
}
tmp___0 = __return_86350;
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_86480;
}
else 
{
goto label_86465;
}
}
else 
{
label_86465:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_86480:; 
 __return_86481 = __retres1;
}
else 
{
goto label_86473;
}
tmp___1 = __return_86481;
C_1_st = 0;
}
else 
{
label_86473:; 
__retres1 = 0;
 __return_86482 = __retres1;
}
goto label_86549;
tmp___1 = __return_86482;
}
label_86561:; 
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
else 
{
label_86345:; 
__retres1 = 0;
 __return_86351 = __retres1;
}
goto label_83473;
tmp___0 = __return_86351;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_86447;
}
else 
{
goto label_86432;
}
}
else 
{
label_86432:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_86447:; 
 __return_86448 = __retres1;
}
else 
{
goto label_86440;
}
tmp___1 = __return_86448;
C_1_st = 0;
}
else 
{
label_86440:; 
__retres1 = 0;
 __return_86449 = __retres1;
}
label_86553:; 
tmp___1 = __return_86449;
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
goto label_86557;
}
goto label_83477;
}
}
}
}
}
else 
{
P_2_st = 2;
}
goto label_83471;
}
}
}
else 
{
goto label_86229;
}
}
}
}
}
}
}
else 
{
label_83485:; 
goto label_83098;
}
}
}
}
}
}
}
else 
{
label_83094:; 
goto label_82909;
}
}
}
}
}
}
}
}
}
}
}
}
