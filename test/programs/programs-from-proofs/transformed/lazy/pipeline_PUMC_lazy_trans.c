extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int main_in1_val  ;
int main_in1_val_t  ;
int main_in1_ev  ;
int main_in1_req_up  ;
int main_in2_val  ;
int main_in2_val_t  ;
int main_in2_ev  ;
int main_in2_req_up  ;
int main_diff_val  ;
int main_diff_val_t  ;
int main_diff_ev  ;
int main_diff_req_up ;
int main_sum_val  ;
int main_sum_val_t  ;
int main_sum_ev  ;
int main_sum_req_up ;
int main_pres_val  ;
int main_pres_val_t  ;
int main_pres_ev  ;
int main_pres_req_up  ;
int main_dbl_val  ;
int main_dbl_val_t  ;
int main_dbl_ev  ;
int main_dbl_req_up  ;
int main_zero_val  ;
int main_zero_val_t  ;
int main_zero_ev  ;
int main_zero_req_up  ;
int main_clk_val  ;
int main_clk_val_t  ;
int main_clk_ev  ;
int main_clk_req_up  ;
int main_clk_pos_edge  ;
int main_clk_neg_edge  ;
int N_generate_st  ;
int N_generate_i  ;
int S1_addsub_st  ;
int S1_addsub_i  ;
int S2_presdbl_st  ;
int S2_presdbl_i  ;
int S3_zero_st  ;
int S3_zero_i  ;
int D_z  ;
int D_print_st  ;
int D_print_i  ;
void N_generate(void);
void S1_addsub(void);
void S2_presdbl(void);
void S3_zero(void);
void D_print(void);
void eval(void);
void start_simulation(void);
int main(void);
int __return_15429;
int main()
{
int count ;
int __retres2 ;
main_in1_ev = 2;
main_in1_req_up = 0;
main_in2_ev = 2;
main_in2_req_up = 0;
main_diff_ev = 2;
main_diff_req_up = 0;
main_sum_ev = 2;
main_sum_req_up = 0;
main_pres_ev = 2;
main_pres_req_up = 0;
main_dbl_ev = 2;
main_dbl_req_up = 0;
main_zero_ev = 2;
main_zero_req_up = 0;
main_clk_val = 0;
main_clk_ev = 2;
main_clk_req_up = 0;
main_clk_pos_edge = 2;
main_clk_neg_edge = 2;
count = 0;
N_generate_i = 0;
S1_addsub_i = 0;
S2_presdbl_i = 0;
S3_zero_i = 0;
D_print_i = 0;
{
int kernel_st ;
kernel_st = 0;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_6798;
}
else 
{
label_6798:; 
main_in1_req_up = 0;
goto label_6801;
}
}
else 
{
label_6801:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_6811;
}
else 
{
label_6811:; 
main_in2_req_up = 0;
goto label_6814;
}
}
else 
{
label_6814:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_6824;
}
else 
{
label_6824:; 
main_sum_req_up = 0;
goto label_6827;
}
}
else 
{
label_6827:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_6837;
}
else 
{
label_6837:; 
main_diff_req_up = 0;
goto label_6840;
}
}
else 
{
label_6840:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_6850;
}
else 
{
label_6850:; 
main_pres_req_up = 0;
goto label_6853;
}
}
else 
{
label_6853:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_6863;
}
else 
{
label_6863:; 
main_dbl_req_up = 0;
goto label_6866;
}
}
else 
{
label_6866:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_6876;
}
else 
{
label_6876:; 
main_zero_req_up = 0;
goto label_6879;
}
}
else 
{
label_6879:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
if (((int)main_clk_val) == 1)
{
main_clk_pos_edge = 0;
main_clk_neg_edge = 2;
goto label_6896;
}
else 
{
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
label_6896:; 
goto label_6898;
}
}
else 
{
label_6898:; 
main_clk_req_up = 0;
goto label_6901;
}
}
else 
{
label_6901:; 
if (((int)N_generate_i) == 1)
{
N_generate_st = 0;
goto label_6908;
}
else 
{
N_generate_st = 2;
label_6908:; 
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
goto label_6915;
}
else 
{
S1_addsub_st = 2;
label_6915:; 
if (((int)S2_presdbl_i) == 1)
{
S2_presdbl_st = 0;
goto label_6922;
}
else 
{
S2_presdbl_st = 2;
label_6922:; 
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
goto label_6929;
}
else 
{
S3_zero_st = 2;
label_6929:; 
if (((int)D_print_i) == 1)
{
D_print_st = 0;
goto label_6936;
}
else 
{
D_print_st = 2;
label_6936:; 
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_6942;
}
else 
{
label_6942:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_6948;
}
else 
{
label_6948:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_6954;
}
else 
{
label_6954:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_6960;
}
else 
{
label_6960:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_6966;
}
else 
{
label_6966:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_6972;
}
else 
{
label_6972:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_6978;
}
else 
{
label_6978:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_6984;
}
else 
{
label_6984:; 
if (((int)main_clk_pos_edge) == 0)
{
main_clk_pos_edge = 1;
goto label_6990;
}
else 
{
label_6990:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_6996;
}
else 
{
label_6996:; 
if (((int)main_clk_pos_edge) == 1)
{
N_generate_st = 0;
goto label_7002;
}
else 
{
label_7002:; 
if (((int)main_clk_pos_edge) == 1)
{
S1_addsub_st = 0;
goto label_7008;
}
else 
{
label_7008:; 
if (((int)main_clk_pos_edge) == 1)
{
S2_presdbl_st = 0;
goto label_7014;
}
else 
{
label_7014:; 
if (((int)main_clk_pos_edge) == 1)
{
S3_zero_st = 0;
goto label_7020;
}
else 
{
label_7020:; 
if (((int)main_clk_pos_edge) == 1)
{
D_print_st = 0;
goto label_7026;
}
else 
{
label_7026:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_7032;
}
else 
{
label_7032:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_7038;
}
else 
{
label_7038:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_7044;
}
else 
{
label_7044:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_7050;
}
else 
{
label_7050:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_7056;
}
else 
{
label_7056:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_7062;
}
else 
{
label_7062:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_7068;
}
else 
{
label_7068:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_7074;
}
else 
{
label_7074:; 
if (((int)main_clk_pos_edge) == 1)
{
main_clk_pos_edge = 2;
goto label_7080;
}
else 
{
label_7080:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_7086;
}
else 
{
label_7086:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
if (((int)N_generate_st) == 0)
{
goto label_7127;
}
else 
{
if (((int)S1_addsub_st) == 0)
{
goto label_7127;
}
else 
{
if (((int)S2_presdbl_st) == 0)
{
goto label_7127;
}
else 
{
if (((int)S3_zero_st) == 0)
{
goto label_7127;
}
else 
{
if (((int)D_print_st) == 0)
{
label_7127:; 
if (((int)N_generate_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_7138;
}
else 
{
label_7138:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_7149;
}
else 
{
label_7149:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
goto label_7160;
}
else 
{
label_7160:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
goto label_7171;
}
else 
{
label_7171:; 
tmp___3 = __VERIFIER_nondet_int();
return 1;
}
}
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_15147;
}
else 
{
label_15147:; 
main_in1_req_up = 0;
goto label_15150;
}
}
else 
{
label_15150:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_15160;
}
else 
{
label_15160:; 
main_in2_req_up = 0;
goto label_15163;
}
}
else 
{
label_15163:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_15173;
}
else 
{
label_15173:; 
main_sum_req_up = 0;
goto label_15176;
}
}
else 
{
label_15176:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_15186;
}
else 
{
label_15186:; 
main_diff_req_up = 0;
goto label_15189;
}
}
else 
{
label_15189:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_15199;
}
else 
{
label_15199:; 
main_pres_req_up = 0;
goto label_15202;
}
}
else 
{
label_15202:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_15212;
}
else 
{
label_15212:; 
main_dbl_req_up = 0;
goto label_15215;
}
}
else 
{
label_15215:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_15225;
}
else 
{
label_15225:; 
main_zero_req_up = 0;
goto label_15228;
}
}
else 
{
label_15228:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
if (((int)main_clk_val) == 1)
{
main_clk_pos_edge = 0;
main_clk_neg_edge = 2;
goto label_15245;
}
else 
{
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
label_15245:; 
goto label_15247;
}
}
else 
{
label_15247:; 
main_clk_req_up = 0;
goto label_15250;
}
}
else 
{
label_15250:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_15257;
}
else 
{
label_15257:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_15263;
}
else 
{
label_15263:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_15269;
}
else 
{
label_15269:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_15275;
}
else 
{
label_15275:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_15281;
}
else 
{
label_15281:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_15287;
}
else 
{
label_15287:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_15293;
}
else 
{
label_15293:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_15299;
}
else 
{
label_15299:; 
if (((int)main_clk_pos_edge) == 0)
{
main_clk_pos_edge = 1;
goto label_15305;
}
else 
{
label_15305:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_15311;
}
else 
{
label_15311:; 
if (((int)main_clk_pos_edge) == 1)
{
N_generate_st = 0;
goto label_15317;
}
else 
{
label_15317:; 
if (((int)main_clk_pos_edge) == 1)
{
S1_addsub_st = 0;
goto label_15323;
}
else 
{
label_15323:; 
if (((int)main_clk_pos_edge) == 1)
{
S2_presdbl_st = 0;
goto label_15329;
}
else 
{
label_15329:; 
if (((int)main_clk_pos_edge) == 1)
{
S3_zero_st = 0;
goto label_15335;
}
else 
{
label_15335:; 
if (((int)main_clk_pos_edge) == 1)
{
D_print_st = 0;
goto label_15341;
}
else 
{
label_15341:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_15347;
}
else 
{
label_15347:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_15353;
}
else 
{
label_15353:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_15359;
}
else 
{
label_15359:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_15365;
}
else 
{
label_15365:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_15371;
}
else 
{
label_15371:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_15377;
}
else 
{
label_15377:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_15383;
}
else 
{
label_15383:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_15389;
}
else 
{
label_15389:; 
if (((int)main_clk_pos_edge) == 1)
{
main_clk_pos_edge = 2;
goto label_15395;
}
else 
{
label_15395:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_15401;
}
else 
{
label_15401:; 
if (((int)N_generate_st) == 0)
{
return 1;
}
else 
{
if (((int)S1_addsub_st) == 0)
{
return 1;
}
else 
{
if (((int)S2_presdbl_st) == 0)
{
return 1;
}
else 
{
if (((int)S3_zero_st) == 0)
{
return 1;
}
else 
{
}
__retres2 = 0;
 __return_15429 = __retres2;
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
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
