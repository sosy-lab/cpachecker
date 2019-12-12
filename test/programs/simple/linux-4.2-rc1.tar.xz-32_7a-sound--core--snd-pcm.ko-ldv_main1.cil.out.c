struct snd_pcm_substream {
   struct snd_pcm_runtime *runtime ;
};

struct snd_pcm_runtime {
   int tstamp_mode ;
   int stop_threshold ;
};

static void update_audio_tstamp(struct snd_pcm_substream *substream) 
{ 
  struct snd_pcm_runtime *runtime ;

  runtime = substream->runtime;
  if (runtime->tstamp_mode != 1) {
    return;
  } 
  return;
}

static void xrun(struct snd_pcm_substream *substream ) 
{ 
  struct snd_pcm_runtime *runtime ;

  runtime = substream->runtime;
  if (runtime->tstamp_mode == 1) {
    return;
  } 
  ERROR:
  return;
}

void main(void) 
{ 
  struct snd_pcm_substream *var_group1 ;
  struct snd_pcm_runtime *runtime ;
  int pos ;
  int hdelta ;
  int xrun_threshold ;

  if (pos == 0xffffffffffffffffUL) {
    xrun(var_group1);
    return;
  } 
  if (runtime != 0U) {
    while (hdelta > xrun_threshold) {
    } 
  }   
  update_audio_tstamp(var_group1);
  if (runtime->stop_threshold <= 0) {
    xrun(var_group1);
    return;
  }
  return;
}
