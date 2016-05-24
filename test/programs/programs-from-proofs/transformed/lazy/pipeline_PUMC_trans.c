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
int __return_19109;
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
goto label_18431;
}
else 
{
label_18431:; 
main_in1_req_up = 0;
goto label_18434;
}
}
else 
{
label_18434:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_18444;
}
else 
{
label_18444:; 
main_in2_req_up = 0;
goto label_18447;
}
}
else 
{
label_18447:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_18457;
}
else 
{
label_18457:; 
main_sum_req_up = 0;
goto label_18460;
}
}
else 
{
label_18460:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18470;
}
else 
{
label_18470:; 
main_diff_req_up = 0;
goto label_18473;
}
}
else 
{
label_18473:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18483;
}
else 
{
label_18483:; 
main_pres_req_up = 0;
goto label_18486;
}
}
else 
{
label_18486:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18496;
}
else 
{
label_18496:; 
main_dbl_req_up = 0;
goto label_18499;
}
}
else 
{
label_18499:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18509;
}
else 
{
label_18509:; 
main_zero_req_up = 0;
goto label_18512;
}
}
else 
{
label_18512:; 
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
goto label_18529;
}
else 
{
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
label_18529:; 
goto label_18531;
}
}
else 
{
label_18531:; 
main_clk_req_up = 0;
goto label_18534;
}
}
else 
{
label_18534:; 
if (((int)N_generate_i) == 1)
{
N_generate_st = 0;
goto label_18541;
}
else 
{
N_generate_st = 2;
label_18541:; 
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
goto label_18548;
}
else 
{
S1_addsub_st = 2;
label_18548:; 
if (((int)S2_presdbl_i) == 1)
{
S2_presdbl_st = 0;
goto label_18555;
}
else 
{
S2_presdbl_st = 2;
label_18555:; 
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
goto label_18562;
}
else 
{
S3_zero_st = 2;
label_18562:; 
if (((int)D_print_i) == 1)
{
D_print_st = 0;
goto label_18569;
}
else 
{
D_print_st = 2;
label_18569:; 
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_18575;
}
else 
{
label_18575:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_18581;
}
else 
{
label_18581:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_18587;
}
else 
{
label_18587:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_18593;
}
else 
{
label_18593:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_18599;
}
else 
{
label_18599:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_18605;
}
else 
{
label_18605:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_18611;
}
else 
{
label_18611:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_18617;
}
else 
{
label_18617:; 
if (((int)main_clk_pos_edge) == 0)
{
main_clk_pos_edge = 1;
goto label_18623;
}
else 
{
label_18623:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_18629;
}
else 
{
label_18629:; 
if (((int)main_clk_pos_edge) == 1)
{
N_generate_st = 0;
goto label_18635;
}
else 
{
label_18635:; 
if (((int)main_clk_pos_edge) == 1)
{
S1_addsub_st = 0;
goto label_18641;
}
else 
{
label_18641:; 
if (((int)main_clk_pos_edge) == 1)
{
S2_presdbl_st = 0;
goto label_18647;
}
else 
{
label_18647:; 
if (((int)main_clk_pos_edge) == 1)
{
S3_zero_st = 0;
goto label_18653;
}
else 
{
label_18653:; 
if (((int)main_clk_pos_edge) == 1)
{
D_print_st = 0;
goto label_18659;
}
else 
{
label_18659:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_18665;
}
else 
{
label_18665:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_18671;
}
else 
{
label_18671:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_18677;
}
else 
{
label_18677:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_18683;
}
else 
{
label_18683:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_18689;
}
else 
{
label_18689:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_18695;
}
else 
{
label_18695:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_18701;
}
else 
{
label_18701:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_18707;
}
else 
{
label_18707:; 
if (((int)main_clk_pos_edge) == 1)
{
main_clk_pos_edge = 2;
goto label_18713;
}
else 
{
label_18713:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_18719;
}
else 
{
label_18719:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
if (((int)N_generate_st) == 0)
{
goto label_18760;
}
else 
{
if (((int)S1_addsub_st) == 0)
{
goto label_18760;
}
else 
{
if (((int)S2_presdbl_st) == 0)
{
goto label_18760;
}
else 
{
if (((int)S3_zero_st) == 0)
{
goto label_18760;
}
else 
{
if (((int)D_print_st) == 0)
{
label_18760:; 
if (((int)N_generate_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_18771;
}
else 
{
label_18771:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_18782;
}
else 
{
label_18782:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
goto label_18793;
}
else 
{
label_18793:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
goto label_18804;
}
else 
{
label_18804:; 
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
goto label_18827;
}
else 
{
label_18827:; 
main_in1_req_up = 0;
goto label_18830;
}
}
else 
{
label_18830:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_18840;
}
else 
{
label_18840:; 
main_in2_req_up = 0;
goto label_18843;
}
}
else 
{
label_18843:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_18853;
}
else 
{
label_18853:; 
main_sum_req_up = 0;
goto label_18856;
}
}
else 
{
label_18856:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18866;
}
else 
{
label_18866:; 
main_diff_req_up = 0;
goto label_18869;
}
}
else 
{
label_18869:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18879;
}
else 
{
label_18879:; 
main_pres_req_up = 0;
goto label_18882;
}
}
else 
{
label_18882:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18892;
}
else 
{
label_18892:; 
main_dbl_req_up = 0;
goto label_18895;
}
}
else 
{
label_18895:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18905;
}
else 
{
label_18905:; 
main_zero_req_up = 0;
goto label_18908;
}
}
else 
{
label_18908:; 
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
goto label_18925;
}
else 
{
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
label_18925:; 
goto label_18927;
}
}
else 
{
label_18927:; 
main_clk_req_up = 0;
goto label_18930;
}
}
else 
{
label_18930:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_18937;
}
else 
{
label_18937:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_18943;
}
else 
{
label_18943:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_18949;
}
else 
{
label_18949:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_18955;
}
else 
{
label_18955:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_18961;
}
else 
{
label_18961:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_18967;
}
else 
{
label_18967:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_18973;
}
else 
{
label_18973:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_18979;
}
else 
{
label_18979:; 
if (((int)main_clk_pos_edge) == 0)
{
main_clk_pos_edge = 1;
goto label_18985;
}
else 
{
label_18985:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_18991;
}
else 
{
label_18991:; 
if (((int)main_clk_pos_edge) == 1)
{
N_generate_st = 0;
goto label_18997;
}
else 
{
label_18997:; 
if (((int)main_clk_pos_edge) == 1)
{
S1_addsub_st = 0;
goto label_19003;
}
else 
{
label_19003:; 
if (((int)main_clk_pos_edge) == 1)
{
S2_presdbl_st = 0;
goto label_19009;
}
else 
{
label_19009:; 
if (((int)main_clk_pos_edge) == 1)
{
S3_zero_st = 0;
goto label_19015;
}
else 
{
label_19015:; 
if (((int)main_clk_pos_edge) == 1)
{
D_print_st = 0;
goto label_19021;
}
else 
{
label_19021:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_19027;
}
else 
{
label_19027:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_19033;
}
else 
{
label_19033:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_19039;
}
else 
{
label_19039:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_19045;
}
else 
{
label_19045:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_19051;
}
else 
{
label_19051:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_19057;
}
else 
{
label_19057:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_19063;
}
else 
{
label_19063:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_19069;
}
else 
{
label_19069:; 
if (((int)main_clk_pos_edge) == 1)
{
main_clk_pos_edge = 2;
goto label_19075;
}
else 
{
label_19075:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_19081;
}
else 
{
label_19081:; 
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
 __return_19109 = __retres2;
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
