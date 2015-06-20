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
int __return_8631=0;
int __return_8630=0;
int __return_8632=0;
int __return_8633=0;
int __return_8636=0;
int __return_8637=0;
int __return_8634=0;
int __return_8635=0;
int __return_8638=0;
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
goto label_5836;
}
else 
{
label_5836:; 
main_in1_req_up = 0;
goto label_5833;
}
}
else 
{
label_5833:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_5847;
}
else 
{
label_5847:; 
main_in2_req_up = 0;
goto label_5844;
}
}
else 
{
label_5844:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_5858;
}
else 
{
label_5858:; 
main_sum_req_up = 0;
goto label_5855;
}
}
else 
{
label_5855:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_5869;
}
else 
{
label_5869:; 
main_diff_req_up = 0;
goto label_5866;
}
}
else 
{
label_5866:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_5880;
}
else 
{
label_5880:; 
main_pres_req_up = 0;
goto label_5877;
}
}
else 
{
label_5877:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_5891;
}
else 
{
label_5891:; 
main_dbl_req_up = 0;
goto label_5888;
}
}
else 
{
label_5888:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_5902;
}
else 
{
label_5902:; 
main_zero_req_up = 0;
goto label_5899;
}
}
else 
{
label_5899:; 
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_5957;
}
else 
{
label_5957:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_5977;
}
else 
{
label_5977:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_5997;
}
else 
{
label_5997:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_6017;
}
else 
{
label_6017:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_6037;
}
else 
{
label_6037:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_6057;
}
else 
{
label_6057:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_6077;
}
else 
{
label_6077:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_6097;
}
else 
{
label_6097:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_6125;
}
else 
{
label_6125:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_6185;
}
else 
{
label_6185:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_6205;
}
else 
{
label_6205:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_6225;
}
else 
{
label_6225:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_6245;
}
else 
{
label_6245:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_6265;
}
else 
{
label_6265:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_6285;
}
else 
{
label_6285:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_6305;
}
else 
{
label_6305:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_6325;
}
else 
{
label_6325:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_6353;
}
else 
{
label_6353:; 
label_6368:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_6543:; 
if (((int)S1_addsub_st) == 0)
{
goto label_6557;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_6557:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_6563;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_6583:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_6593;
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
label_6628:; 
}
label_6696:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_6777;
}
else 
{
label_6777:; 
main_in1_req_up = 0;
goto label_6744;
}
}
else 
{
label_6744:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_6876;
}
else 
{
label_6876:; 
main_in2_req_up = 0;
goto label_6843;
}
}
else 
{
label_6843:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_6975;
}
else 
{
label_6975:; 
main_sum_req_up = 0;
goto label_6942;
}
}
else 
{
label_6942:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_7074;
}
else 
{
label_7074:; 
main_diff_req_up = 0;
goto label_7041;
}
}
else 
{
label_7041:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_7173;
}
else 
{
label_7173:; 
main_pres_req_up = 0;
goto label_7140;
}
}
else 
{
label_7140:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_7272;
}
else 
{
label_7272:; 
main_dbl_req_up = 0;
goto label_7239;
}
}
else 
{
label_7239:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_7371;
}
else 
{
label_7371:; 
main_zero_req_up = 0;
goto label_7338;
}
}
else 
{
label_7338:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_7470;
}
else 
{
label_7470:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_7515;
}
else 
{
label_7515:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_7560;
}
else 
{
label_7560:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_7605;
}
else 
{
label_7605:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_7650;
}
else 
{
label_7650:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_7695;
}
else 
{
label_7695:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_7740;
}
else 
{
label_7740:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_7785;
}
else 
{
label_7785:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_7848;
}
else 
{
label_7848:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_7983;
}
else 
{
label_7983:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_8028;
}
else 
{
label_8028:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_8073;
}
else 
{
label_8073:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_8118;
}
else 
{
label_8118:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_8163;
}
else 
{
label_8163:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_8208;
}
else 
{
label_8208:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_8253;
}
else 
{
label_8253:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_8298;
}
else 
{
label_8298:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_8361;
}
else 
{
label_8361:; 
}
__retres2 = 0;
 __return_8631 = __retres2;
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
else 
{
label_6593:; 
if (((int)S3_zero_st) == 0)
{
goto label_6583;
}
else 
{
}
label_6709:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_6778;
}
else 
{
label_6778:; 
main_in1_req_up = 0;
goto label_6743;
}
}
else 
{
label_6743:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_6877;
}
else 
{
label_6877:; 
main_in2_req_up = 0;
goto label_6842;
}
}
else 
{
label_6842:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_6976;
}
else 
{
label_6976:; 
main_sum_req_up = 0;
goto label_6941;
}
}
else 
{
label_6941:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_7075;
}
else 
{
label_7075:; 
main_diff_req_up = 0;
goto label_7040;
}
}
else 
{
label_7040:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_7174;
}
else 
{
label_7174:; 
main_pres_req_up = 0;
goto label_7139;
}
}
else 
{
label_7139:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_7273;
}
else 
{
label_7273:; 
main_dbl_req_up = 0;
goto label_7238;
}
}
else 
{
label_7238:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_7372;
}
else 
{
label_7372:; 
main_zero_req_up = 0;
goto label_7337;
}
}
else 
{
label_7337:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_7471;
}
else 
{
label_7471:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_7516;
}
else 
{
label_7516:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_7561;
}
else 
{
label_7561:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_7606;
}
else 
{
label_7606:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_7651;
}
else 
{
label_7651:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_7696;
}
else 
{
label_7696:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_7741;
}
else 
{
label_7741:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_7786;
}
else 
{
label_7786:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_7849;
}
else 
{
label_7849:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_7984;
}
else 
{
label_7984:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_8029;
}
else 
{
label_8029:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_8074;
}
else 
{
label_8074:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_8119;
}
else 
{
label_8119:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_8164;
}
else 
{
label_8164:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_8209;
}
else 
{
label_8209:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_8254;
}
else 
{
label_8254:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_8299;
}
else 
{
label_8299:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_8362;
}
else 
{
label_8362:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_8563:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_8585;
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
}
goto label_6696;
}
else 
{
label_8585:; 
goto label_8563;
}
}
else 
{
}
goto label_6709;
}
}
else 
{
}
__retres2 = 0;
 __return_8630 = __retres2;
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
else 
{
label_6563:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_6592;
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
label_6629:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_6656;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_6628;
}
}
else 
{
label_6656:; 
goto label_6629;
}
}
else 
{
}
label_6650:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_6776;
}
else 
{
label_6776:; 
main_in1_req_up = 0;
goto label_6745;
}
}
else 
{
label_6745:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_6875;
}
else 
{
label_6875:; 
main_in2_req_up = 0;
goto label_6844;
}
}
else 
{
label_6844:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_6974;
}
else 
{
label_6974:; 
main_sum_req_up = 0;
goto label_6943;
}
}
else 
{
label_6943:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_7073;
}
else 
{
label_7073:; 
main_diff_req_up = 0;
goto label_7042;
}
}
else 
{
label_7042:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_7172;
}
else 
{
label_7172:; 
main_pres_req_up = 0;
goto label_7141;
}
}
else 
{
label_7141:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_7271;
}
else 
{
label_7271:; 
main_dbl_req_up = 0;
goto label_7240;
}
}
else 
{
label_7240:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_7370;
}
else 
{
label_7370:; 
main_zero_req_up = 0;
goto label_7339;
}
}
else 
{
label_7339:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_7469;
}
else 
{
label_7469:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_7514;
}
else 
{
label_7514:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_7559;
}
else 
{
label_7559:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_7604;
}
else 
{
label_7604:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_7649;
}
else 
{
label_7649:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_7694;
}
else 
{
label_7694:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_7739;
}
else 
{
label_7739:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_7784;
}
else 
{
label_7784:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_7847;
}
else 
{
label_7847:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_7982;
}
else 
{
label_7982:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_8027;
}
else 
{
label_8027:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_8072;
}
else 
{
label_8072:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_8117;
}
else 
{
label_8117:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_8162;
}
else 
{
label_8162:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_8207;
}
else 
{
label_8207:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_8252;
}
else 
{
label_8252:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_8297;
}
else 
{
label_8297:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_8360;
}
else 
{
label_8360:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_8486:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_8504;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
}
goto label_6696;
}
else 
{
label_8504:; 
goto label_8486;
}
}
else 
{
}
goto label_6650;
}
}
else 
{
}
__retres2 = 0;
 __return_8632 = __retres2;
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
else 
{
label_6592:; 
goto label_6543;
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
goto label_6775;
}
else 
{
label_6775:; 
main_in1_req_up = 0;
goto label_6746;
}
}
else 
{
label_6746:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_6874;
}
else 
{
label_6874:; 
main_in2_req_up = 0;
goto label_6845;
}
}
else 
{
label_6845:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_6973;
}
else 
{
label_6973:; 
main_sum_req_up = 0;
goto label_6944;
}
}
else 
{
label_6944:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_7072;
}
else 
{
label_7072:; 
main_diff_req_up = 0;
goto label_7043;
}
}
else 
{
label_7043:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_7171;
}
else 
{
label_7171:; 
main_pres_req_up = 0;
goto label_7142;
}
}
else 
{
label_7142:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_7270;
}
else 
{
label_7270:; 
main_dbl_req_up = 0;
goto label_7241;
}
}
else 
{
label_7241:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_7369;
}
else 
{
label_7369:; 
main_zero_req_up = 0;
goto label_7340;
}
}
else 
{
label_7340:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_7468;
}
else 
{
label_7468:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_7513;
}
else 
{
label_7513:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_7558;
}
else 
{
label_7558:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_7603;
}
else 
{
label_7603:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_7648;
}
else 
{
label_7648:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_7693;
}
else 
{
label_7693:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_7738;
}
else 
{
label_7738:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_7783;
}
else 
{
label_7783:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_7846;
}
else 
{
label_7846:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_7981;
}
else 
{
label_7981:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_8026;
}
else 
{
label_8026:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_8071;
}
else 
{
label_8071:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_8116;
}
else 
{
label_8116:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_8161;
}
else 
{
label_8161:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_8206;
}
else 
{
label_8206:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_8251;
}
else 
{
label_8251:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_8296;
}
else 
{
label_8296:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_8359;
}
else 
{
label_8359:; 
if (((int)S1_addsub_st) == 0)
{
goto label_8469;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_8469:; 
goto label_6368;
}
else 
{
}
__retres2 = 0;
 __return_8633 = __retres2;
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
else 
{
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_5959;
}
else 
{
label_5959:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_5979;
}
else 
{
label_5979:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_5999;
}
else 
{
label_5999:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_6019;
}
else 
{
label_6019:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_6039;
}
else 
{
label_6039:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_6059;
}
else 
{
label_6059:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_6079;
}
else 
{
label_6079:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_6099;
}
else 
{
label_6099:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_6127;
}
else 
{
label_6127:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_6187;
}
else 
{
label_6187:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_6207;
}
else 
{
label_6207:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_6227;
}
else 
{
label_6227:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_6247;
}
else 
{
label_6247:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_6267;
}
else 
{
label_6267:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_6287;
}
else 
{
label_6287:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_6307;
}
else 
{
label_6307:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_6327;
}
else 
{
label_6327:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_6355;
}
else 
{
label_6355:; 
label_6366:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_6407:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_6425;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_6772;
}
else 
{
label_6772:; 
main_in1_req_up = 0;
goto label_6749;
}
}
else 
{
label_6749:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_6871;
}
else 
{
label_6871:; 
main_in2_req_up = 0;
goto label_6848;
}
}
else 
{
label_6848:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_6970;
}
else 
{
label_6970:; 
main_sum_req_up = 0;
goto label_6947;
}
}
else 
{
label_6947:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_7069;
}
else 
{
label_7069:; 
main_diff_req_up = 0;
goto label_7046;
}
}
else 
{
label_7046:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_7168;
}
else 
{
label_7168:; 
main_pres_req_up = 0;
goto label_7145;
}
}
else 
{
label_7145:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_7267;
}
else 
{
label_7267:; 
main_dbl_req_up = 0;
goto label_7244;
}
}
else 
{
label_7244:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_7366;
}
else 
{
label_7366:; 
main_zero_req_up = 0;
goto label_7343;
}
}
else 
{
label_7343:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_7465;
}
else 
{
label_7465:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_7510;
}
else 
{
label_7510:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_7555;
}
else 
{
label_7555:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_7600;
}
else 
{
label_7600:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_7645;
}
else 
{
label_7645:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_7690;
}
else 
{
label_7690:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_7735;
}
else 
{
label_7735:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_7780;
}
else 
{
label_7780:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_7843;
}
else 
{
label_7843:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_7978;
}
else 
{
label_7978:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_8023;
}
else 
{
label_8023:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_8068;
}
else 
{
label_8068:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_8113;
}
else 
{
label_8113:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_8158;
}
else 
{
label_8158:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_8203;
}
else 
{
label_8203:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_8248;
}
else 
{
label_8248:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_8293;
}
else 
{
label_8293:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_8356;
}
else 
{
label_8356:; 
}
__retres2 = 0;
 __return_8636 = __retres2;
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
else 
{
label_6425:; 
goto label_6407;
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
goto label_6771;
}
else 
{
label_6771:; 
main_in1_req_up = 0;
goto label_6750;
}
}
else 
{
label_6750:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_6870;
}
else 
{
label_6870:; 
main_in2_req_up = 0;
goto label_6849;
}
}
else 
{
label_6849:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_6969;
}
else 
{
label_6969:; 
main_sum_req_up = 0;
goto label_6948;
}
}
else 
{
label_6948:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_7068;
}
else 
{
label_7068:; 
main_diff_req_up = 0;
goto label_7047;
}
}
else 
{
label_7047:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_7167;
}
else 
{
label_7167:; 
main_pres_req_up = 0;
goto label_7146;
}
}
else 
{
label_7146:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_7266;
}
else 
{
label_7266:; 
main_dbl_req_up = 0;
goto label_7245;
}
}
else 
{
label_7245:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_7365;
}
else 
{
label_7365:; 
main_zero_req_up = 0;
goto label_7344;
}
}
else 
{
label_7344:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_7464;
}
else 
{
label_7464:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_7509;
}
else 
{
label_7509:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_7554;
}
else 
{
label_7554:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_7599;
}
else 
{
label_7599:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_7644;
}
else 
{
label_7644:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_7689;
}
else 
{
label_7689:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_7734;
}
else 
{
label_7734:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_7779;
}
else 
{
label_7779:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_7842;
}
else 
{
label_7842:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_7977;
}
else 
{
label_7977:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_8022;
}
else 
{
label_8022:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_8067;
}
else 
{
label_8067:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_8112;
}
else 
{
label_8112:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_8157;
}
else 
{
label_8157:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_8202;
}
else 
{
label_8202:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_8247;
}
else 
{
label_8247:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_8292;
}
else 
{
label_8292:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_8355;
}
else 
{
label_8355:; 
if (((int)S1_addsub_st) == 0)
{
goto label_6366;
}
else 
{
}
__retres2 = 0;
 __return_8637 = __retres2;
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
else 
{
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_5958;
}
else 
{
label_5958:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_5978;
}
else 
{
label_5978:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_5998;
}
else 
{
label_5998:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_6018;
}
else 
{
label_6018:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_6038;
}
else 
{
label_6038:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_6058;
}
else 
{
label_6058:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_6078;
}
else 
{
label_6078:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_6098;
}
else 
{
label_6098:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_6126;
}
else 
{
label_6126:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_6186;
}
else 
{
label_6186:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_6206;
}
else 
{
label_6206:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_6226;
}
else 
{
label_6226:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_6246;
}
else 
{
label_6246:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_6266;
}
else 
{
label_6266:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_6286;
}
else 
{
label_6286:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_6306;
}
else 
{
label_6306:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_6326;
}
else 
{
label_6326:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_6354;
}
else 
{
label_6354:; 
label_6367:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_6478:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_6500;
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_6774;
}
else 
{
label_6774:; 
main_in1_req_up = 0;
goto label_6747;
}
}
else 
{
label_6747:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_6873;
}
else 
{
label_6873:; 
main_in2_req_up = 0;
goto label_6846;
}
}
else 
{
label_6846:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_6972;
}
else 
{
label_6972:; 
main_sum_req_up = 0;
goto label_6945;
}
}
else 
{
label_6945:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_7071;
}
else 
{
label_7071:; 
main_diff_req_up = 0;
goto label_7044;
}
}
else 
{
label_7044:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_7170;
}
else 
{
label_7170:; 
main_pres_req_up = 0;
goto label_7143;
}
}
else 
{
label_7143:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_7269;
}
else 
{
label_7269:; 
main_dbl_req_up = 0;
goto label_7242;
}
}
else 
{
label_7242:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_7368;
}
else 
{
label_7368:; 
main_zero_req_up = 0;
goto label_7341;
}
}
else 
{
label_7341:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_7467;
}
else 
{
label_7467:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_7512;
}
else 
{
label_7512:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_7557;
}
else 
{
label_7557:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_7602;
}
else 
{
label_7602:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_7647;
}
else 
{
label_7647:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_7692;
}
else 
{
label_7692:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_7737;
}
else 
{
label_7737:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_7782;
}
else 
{
label_7782:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_7845;
}
else 
{
label_7845:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_7980;
}
else 
{
label_7980:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_8025;
}
else 
{
label_8025:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_8070;
}
else 
{
label_8070:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_8115;
}
else 
{
label_8115:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_8160;
}
else 
{
label_8160:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_8205;
}
else 
{
label_8205:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_8250;
}
else 
{
label_8250:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_8295;
}
else 
{
label_8295:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_8358;
}
else 
{
label_8358:; 
}
__retres2 = 0;
 __return_8634 = __retres2;
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
else 
{
label_6500:; 
goto label_6478;
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
goto label_6773;
}
else 
{
label_6773:; 
main_in1_req_up = 0;
goto label_6748;
}
}
else 
{
label_6748:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_6872;
}
else 
{
label_6872:; 
main_in2_req_up = 0;
goto label_6847;
}
}
else 
{
label_6847:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_6971;
}
else 
{
label_6971:; 
main_sum_req_up = 0;
goto label_6946;
}
}
else 
{
label_6946:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_7070;
}
else 
{
label_7070:; 
main_diff_req_up = 0;
goto label_7045;
}
}
else 
{
label_7045:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_7169;
}
else 
{
label_7169:; 
main_pres_req_up = 0;
goto label_7144;
}
}
else 
{
label_7144:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_7268;
}
else 
{
label_7268:; 
main_dbl_req_up = 0;
goto label_7243;
}
}
else 
{
label_7243:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_7367;
}
else 
{
label_7367:; 
main_zero_req_up = 0;
goto label_7342;
}
}
else 
{
label_7342:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_7466;
}
else 
{
label_7466:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_7511;
}
else 
{
label_7511:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_7556;
}
else 
{
label_7556:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_7601;
}
else 
{
label_7601:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_7646;
}
else 
{
label_7646:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_7691;
}
else 
{
label_7691:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_7736;
}
else 
{
label_7736:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_7781;
}
else 
{
label_7781:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_7844;
}
else 
{
label_7844:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_7979;
}
else 
{
label_7979:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_8024;
}
else 
{
label_8024:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_8069;
}
else 
{
label_8069:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_8114;
}
else 
{
label_8114:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_8159;
}
else 
{
label_8159:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_8204;
}
else 
{
label_8204:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_8249;
}
else 
{
label_8249:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_8294;
}
else 
{
label_8294:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_8357;
}
else 
{
label_8357:; 
if (((int)S3_zero_st) == 0)
{
goto label_6367;
}
else 
{
}
__retres2 = 0;
 __return_8635 = __retres2;
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
else 
{
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_5960;
}
else 
{
label_5960:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_5980;
}
else 
{
label_5980:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_6000;
}
else 
{
label_6000:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_6020;
}
else 
{
label_6020:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_6040;
}
else 
{
label_6040:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_6060;
}
else 
{
label_6060:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_6080;
}
else 
{
label_6080:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_6100;
}
else 
{
label_6100:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_6128;
}
else 
{
label_6128:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_6188;
}
else 
{
label_6188:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_6208;
}
else 
{
label_6208:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_6228;
}
else 
{
label_6228:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_6248;
}
else 
{
label_6248:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_6268;
}
else 
{
label_6268:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_6288;
}
else 
{
label_6288:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_6308;
}
else 
{
label_6308:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_6328;
}
else 
{
label_6328:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_6356;
}
else 
{
label_6356:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_6770;
}
else 
{
label_6770:; 
main_in1_req_up = 0;
goto label_6751;
}
}
else 
{
label_6751:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_6869;
}
else 
{
label_6869:; 
main_in2_req_up = 0;
goto label_6850;
}
}
else 
{
label_6850:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_6968;
}
else 
{
label_6968:; 
main_sum_req_up = 0;
goto label_6949;
}
}
else 
{
label_6949:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_7067;
}
else 
{
label_7067:; 
main_diff_req_up = 0;
goto label_7048;
}
}
else 
{
label_7048:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_7166;
}
else 
{
label_7166:; 
main_pres_req_up = 0;
goto label_7147;
}
}
else 
{
label_7147:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_7265;
}
else 
{
label_7265:; 
main_dbl_req_up = 0;
goto label_7246;
}
}
else 
{
label_7246:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_7364;
}
else 
{
label_7364:; 
main_zero_req_up = 0;
goto label_7345;
}
}
else 
{
label_7345:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_7463;
}
else 
{
label_7463:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_7508;
}
else 
{
label_7508:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_7553;
}
else 
{
label_7553:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_7598;
}
else 
{
label_7598:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_7643;
}
else 
{
label_7643:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_7688;
}
else 
{
label_7688:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_7733;
}
else 
{
label_7733:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_7778;
}
else 
{
label_7778:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_7841;
}
else 
{
label_7841:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_7976;
}
else 
{
label_7976:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_8021;
}
else 
{
label_8021:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_8066;
}
else 
{
label_8066:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_8111;
}
else 
{
label_8111:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_8156;
}
else 
{
label_8156:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_8201;
}
else 
{
label_8201:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_8246;
}
else 
{
label_8246:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_8291;
}
else 
{
label_8291:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_8354;
}
else 
{
label_8354:; 
}
__retres2 = 0;
 __return_8638 = __retres2;
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
