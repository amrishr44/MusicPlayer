package com.example.musicplayer.ui.equalizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.musicplayer.R;

import java.util.ArrayList;

public class EqualizerActivity extends AppCompatActivity {

    private AudioManager audioManager;
    public static Equalizer equalizer;
    private PresetReverb reverb;
    public static BassBoost bassBoost;
    public static Virtualizer virtualizer;
    private SeekBar bass_seekbar, virtualizer_seekbar, volume_seekbar;

    private SharedPreferences settings;

    String[] presets;
    ArrayList<Integer> seekbarIds;

    private LinearLayout seekbar_container, seekbar_tv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        Toolbar toolbar = findViewById(R.id.equalizer_toolbar);
        setSupportActionBar(toolbar);

        settings = getSharedPreferences("EQUALIZER", MODE_PRIVATE);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        seekbarIds = new ArrayList<>();


        final Spinner preset_spinner = findViewById(R.id.preset_spinner);
        Spinner reverb_spinner = findViewById(R.id.reverb_spinner);

        seekbar_container = findViewById(R.id.seekbar_container);
        seekbar_tv = findViewById(R.id.seekbar_tv);

        new Runnable() {
            @Override
            public void run() {

                bass_seekbar = findViewById(R.id.bass_seekbar);

                bass_seekbar.setMax(15);

                bass_seekbar.setProgress(1);
                bass_seekbar.setProgress(settings.getInt("BASS", 0));

                bass_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if (bassBoost.getStrengthSupported()){
                            bassBoost.setStrength((short) (progress*100));
                            settings.edit().putInt("BASS", progress).apply();
                            bassBoost.setEnabled(true);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


            }
        }.run();

        new Runnable() {
            @Override
            public void run() {

                virtualizer_seekbar = findViewById(R.id.virtualizer_seekbar);

                virtualizer_seekbar.setMax(15);
                virtualizer_seekbar.setProgress(1);
                virtualizer_seekbar.setProgress(settings.getInt("VIRTUALIZER", 0));

                virtualizer_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if (virtualizer.getStrengthSupported()){
                            virtualizer.setStrength((short)(progress*100));
                            settings.edit().putInt("VIRTUALIZER", progress).apply();
                            virtualizer.setEnabled(true);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


            }
        }.run();

        new Runnable() {
            @Override
            public void run() {

                volume_seekbar = findViewById(R.id.volume_seekbar_eq);

                volume_seekbar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                volume_seekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

                volume_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


            }
        }.run();


        final Switch switch_eq = findViewById(R.id.switch_eq);

        if (settings.getBoolean("ENABLED", false)) switch_eq.setChecked(true);

        switch_eq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (equalizer != null) {
                    if (switch_eq.isChecked()) {
                        equalizer.setEnabled(true);
                        settings.edit().putBoolean("ENABLED", true).apply();
                    } else {
                        equalizer.setEnabled(false);
                        settings.edit().putBoolean("ENABLED", false).apply();
                    }
                }

            }
        });


        equalizer = new Equalizer(1, 0);
        bassBoost = new BassBoost(1, 0);
        virtualizer = new Virtualizer(1, 0);
        reverb = new PresetReverb(1, 0);


        final int max = (int) equalizer.getBandLevelRange()[1];
        final int min = (int) equalizer.getBandLevelRange()[0];

        for (int i = 0; i< equalizer.getNumberOfBands(); i++){

            final int finalI = i;
            new Runnable() {
                @Override
                public void run() {

                    final VerticalSeekbar eq = new VerticalSeekbar(EqualizerActivity.this);

                    eq.setThumb(getResources().getDrawable(R.drawable.song_seekbar_thumb));
                    eq.setProgressDrawable(getResources().getDrawable(R.drawable.song_seekbar_style));


                    seekbarIds.add(finalI, View.generateViewId());

                    eq.setMax(max-min);
                    eq.setTag(finalI);
                    eq.setId((int)seekbarIds.get(finalI));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        eq.setSplitTrack(false);
                    }
                    eq.setProgress(settings.getInt(String.valueOf(finalI), (int)equalizer.getBandLevel((short) finalI) - min));

                    eq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(final SeekBar seekBar, final int progress, boolean fromUser) {

                            if (equalizer != null){

                                if (eq.getShouldChange()) {
                                    preset_spinner.setSelection(0);

                                    settings.edit().putInt(String.valueOf(seekBar.getTag()), progress).apply();

                                    if (equalizer.getEnabled()) {
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {

                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        equalizer.setBandLevel((short) ((int) seekBar.getTag()), (short) ((progress + min)));
                                                    }
                                                }.run();

                                            }
                                        }, 100);

                                    }
                                }
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    });

                    TableRow.LayoutParams lp = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
                    eq.setLayoutParams(lp);

                    String x;

                    if (equalizer.getCenterFreq((short) finalI) > 1000000){

                        x = equalizer.getCenterFreq((short) finalI)/(double)1000000 + " kHz";
                    }
                    else{
                        x = equalizer.getCenterFreq((short) finalI)/(double) 1000 + "";
                        x = x.substring(0, x.length()-2) + " Hz";
                    }


                    TextView tv = new TextView(EqualizerActivity.this);
                    tv.setText(x);
                    tv.setMaxLines(1);
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    tv.setTextColor(getResources().getColor(R.color.programmer_green));

                    TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                    tv.setGravity(Gravity.CENTER);
                    tv.setLayoutParams(params);

                    seekbar_tv.addView(tv);
                    seekbar_container.addView(eq);

                }
            }.run();

        }


        short noOfPresets = equalizer.getNumberOfPresets();

        presets = new String[noOfPresets +1];
        presets[0] = "Custom";

        for (int i = 0; i< noOfPresets; i++){
            presets[i+1] = equalizer.getPresetName((short) i);
        }


        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, presets);
        preset_spinner.setAdapter(spinnerAdapter);

        preset_spinner.setSelection(settings.getInt("PRESET", 1));

        preset_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {

                if (equalizer != null) {

                    settings.edit().putInt("PRESET", position).apply();

                    new Runnable() {
                        @Override
                        public void run() {
                            if (position != 0) {

                                equalizer.usePreset((short) (position-1));
                                for (int i = 0; i<seekbarIds.size(); i++){

                                    final int finalI = i;
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            VerticalSeekbar seekbar = findViewById((int)seekbarIds.get(finalI));
                                            seekbar.setShouldChange(false);
                                            seekbar.setProgress(1);
                                            seekbar.setProgress(equalizer.getBandLevel((short) finalI)-min);
                                            seekbar.updateThumb();
                                        }
                                    }.run();

                                }

                            }
                        }
                    }.run();

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<String> reverb_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"None", "Small Room", "Medium Room", "Large Room", "Medium Hall", "Large Hall", "Plate"});
        reverb_spinner.setAdapter(reverb_adapter);
        reverb_spinner.setSelection(settings.getInt("REVERB", 0));

        reverb_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {

                if (reverb != null){
                    new Runnable() {
                        @Override
                        public void run() {
                            reverb.setPreset((short) position);
                            reverb.setEnabled(true);
                            settings.edit().putInt("REVERB", position).apply();
                        }
                    }.run();

                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        this.finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        audioManager = null;
        reverb = null;
        bass_seekbar = virtualizer_seekbar = volume_seekbar = null;
        settings = null;
        presets = null;
        seekbarIds = null;
        seekbar_container = seekbar_tv = null;

        super.onDestroy();
    }
}
