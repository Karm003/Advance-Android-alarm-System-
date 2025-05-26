package com.unique.simplealarmclock.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.unique.simplealarmclock.R;
import com.unique.simplealarmclock.databinding.FragmentCreateAlarmBinding;
import com.unique.simplealarmclock.model.Alarm;
import com.unique.simplealarmclock.util.DayUtil;
import com.unique.simplealarmclock.util.TimePickerUtil;
import com.unique.simplealarmclock.viewmodel.CreateAlarmViewModel;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Random;

public class CreateAlarmFragment extends Fragment {
    FragmentCreateAlarmBinding fragmentCreateAlarmBinding;
    private CreateAlarmViewModel createAlarmViewModel;
    boolean isVibrate=false;
    String tone;
    Alarm alarm;
    Ringtone ringtone;
    private static final int QR_SCAN_REQUEST = 100;
    public CreateAlarmFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (getArguments() != null) {
            alarm= (Alarm) getArguments().getSerializable(getString(R.string.arg_alarm_obj));
        }
        createAlarmViewModel = ViewModelProviders.of(this).get(CreateAlarmViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentCreateAlarmBinding = FragmentCreateAlarmBinding.inflate(inflater,container,false);
        View v = fragmentCreateAlarmBinding.getRoot();
        tone=RingtoneManager.getActualDefaultRingtoneUri(this.getContext(), RingtoneManager.TYPE_ALARM).toString();
        ringtone = RingtoneManager.getRingtone(getContext(), Uri.parse(tone));
        fragmentCreateAlarmBinding.fragmentCreatealarmSetToneName.setText(ringtone.getTitle(getContext()));
        if(alarm!=null){
            updateAlarmInfo(alarm);
        }
        fragmentCreateAlarmBinding.fragmentCreatealarmRecurring.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    fragmentCreateAlarmBinding.fragmentCreatealarmRecurringOptions.setVisibility(View.VISIBLE);
                } else {
                    fragmentCreateAlarmBinding.fragmentCreatealarmRecurringOptions.setVisibility(View.GONE);
                }
            }
        });

        fragmentCreateAlarmBinding.fragmentCreatealarmScheduleAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(alarm!=null) {
                    updateAlarm();
                }
                else{
                    scheduleAlarm();
                }

                Navigation.findNavController(v).navigate(R.id.action_createAlarmFragment_to_alarmsListFragment);
            }
        });

        fragmentCreateAlarmBinding.fragmentCreatealarmCardSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) Uri.parse(tone));
                startActivityForResult(intent, 5);
            }
        });

        fragmentCreateAlarmBinding.fragmentCreatealarmVibrateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    isVibrate=true;
                }
                else{
                    isVibrate=false;
                }
            }
        });

        fragmentCreateAlarmBinding.fragmentCreatealarmTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                fragmentCreateAlarmBinding.fragmentCreatealarmScheduleAlarmHeading.setText(DayUtil.getDay(TimePickerUtil.getTimePickerHour(timePicker),TimePickerUtil.getTimePickerMinute(timePicker)));
            }
        });

        // Initialize snooze pickers
        NumberPicker snoozeLimitPicker = fragmentCreateAlarmBinding.snoozeLimitPicker;
        snoozeLimitPicker.setMinValue(0);
        snoozeLimitPicker.setMaxValue(5);
        snoozeLimitPicker.setValue(3); // Default value

        NumberPicker snoozeIntervalPicker = fragmentCreateAlarmBinding.snoozeIntervalPicker;
        snoozeIntervalPicker.setMinValue(1);
        snoozeIntervalPicker.setMaxValue(30);
        snoozeIntervalPicker.setValue(5); // Default value

        // Setup QR code checkbox listener
        fragmentCreateAlarmBinding.fragmentCreatealarmRequireQr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fragmentCreateAlarmBinding.qrCodeInputContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        // Setup QR code generation button
        fragmentCreateAlarmBinding.generateQrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateQRCode();
            }
        });

        // Setup scan existing QR button
        fragmentCreateAlarmBinding.scanExistingQrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanExistingQR();
            }
        });

        // Setup QR code text change listener
        fragmentCreateAlarmBinding.fragmentCreatealarmQrCode.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    generateQRCode();
                }
            }
        });

        return v;
    }

    private void scheduleAlarm() {
        String alarmTitle = getString(R.string.alarm_title);
        int alarmId = new Random().nextInt(Integer.MAX_VALUE);
        if (!fragmentCreateAlarmBinding.fragmentCreatealarmTitle.getText().toString().isEmpty()) {
            alarmTitle = fragmentCreateAlarmBinding.fragmentCreatealarmTitle.getText().toString();
        }

        NumberPicker snoozeLimitPicker = fragmentCreateAlarmBinding.snoozeLimitPicker;
        NumberPicker snoozeIntervalPicker = fragmentCreateAlarmBinding.snoozeIntervalPicker;

        Alarm alarm = new Alarm(
                alarmId,
                TimePickerUtil.getTimePickerHour(fragmentCreateAlarmBinding.fragmentCreatealarmTimePicker),
                TimePickerUtil.getTimePickerMinute(fragmentCreateAlarmBinding.fragmentCreatealarmTimePicker),
                alarmTitle,
                true,
                fragmentCreateAlarmBinding.fragmentCreatealarmRecurring.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckMon.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckTue.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckWed.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckThu.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckFri.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckSat.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckSun.isChecked(),
                tone,
                isVibrate,
                snoozeLimitPicker.getValue(),
                snoozeIntervalPicker.getValue()
        );

        // Set QR code settings
        alarm.setRequiresQR(fragmentCreateAlarmBinding.fragmentCreatealarmRequireQr.isChecked());
        if (alarm.isRequiresQR()) {
            String qrCode = fragmentCreateAlarmBinding.fragmentCreatealarmQrCode.getText().toString();
            if (!qrCode.isEmpty()) {
                alarm.setQrCode(qrCode);
            }
        }

        createAlarmViewModel.insert(alarm);
        alarm.schedule(getContext());
    }

    private void updateAlarm() {
        String alarmTitle = getString(R.string.alarm_title);
        if (!fragmentCreateAlarmBinding.fragmentCreatealarmTitle.getText().toString().isEmpty()) {
            alarmTitle = fragmentCreateAlarmBinding.fragmentCreatealarmTitle.getText().toString();
        }

        NumberPicker snoozeLimitPicker = fragmentCreateAlarmBinding.snoozeLimitPicker;
        NumberPicker snoozeIntervalPicker = fragmentCreateAlarmBinding.snoozeIntervalPicker;

        Alarm updatedAlarm = new Alarm(
                alarm.getAlarmId(),
                TimePickerUtil.getTimePickerHour(fragmentCreateAlarmBinding.fragmentCreatealarmTimePicker),
                TimePickerUtil.getTimePickerMinute(fragmentCreateAlarmBinding.fragmentCreatealarmTimePicker),
                alarmTitle,
                true,
                fragmentCreateAlarmBinding.fragmentCreatealarmRecurring.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckMon.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckTue.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckWed.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckThu.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckFri.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckSat.isChecked(),
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckSun.isChecked(),
                tone,
                isVibrate,
                snoozeLimitPicker.getValue(),
                snoozeIntervalPicker.getValue()
        );

        // Set QR code settings
        updatedAlarm.setRequiresQR(fragmentCreateAlarmBinding.fragmentCreatealarmRequireQr.isChecked());
        if (updatedAlarm.isRequiresQR()) {
            String qrCode = fragmentCreateAlarmBinding.fragmentCreatealarmQrCode.getText().toString();
            if (!qrCode.isEmpty()) {
                updatedAlarm.setQrCode(qrCode);
            }
        }

        createAlarmViewModel.update(updatedAlarm);
        updatedAlarm.schedule(getContext());
    }

    private void scanExistingQR() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan any existing QR code to use for this alarm");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null) {
            if (result.getContents() != null) {
                // Successfully scanned QR code
                String scannedText = result.getContents();
                
                // Update the text field with scanned content
                fragmentCreateAlarmBinding.fragmentCreatealarmQrCode.setText(scannedText);
                
                // Show success message with the scanned text
                Toast.makeText(getContext(), 
                    "QR code registered: " + scannedText, 
                    Toast.LENGTH_LONG).show();
                
                // Generate and show QR code preview
                generateQRCode();
                
                // Make sure QR requirement is checked and container is visible
                fragmentCreateAlarmBinding.fragmentCreatealarmRequireQr.setChecked(true);
                fragmentCreateAlarmBinding.qrCodeInputContainer.setVisibility(View.VISIBLE);
            } else {
                // User cancelled the scanning
                Toast.makeText(getContext(), "Scanning cancelled", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == 5) {
            // Handle ringtone selection
            Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            ringtone = RingtoneManager.getRingtone(getContext(), uri);
            String title = ringtone.getTitle(getContext());
            if (uri != null) {
                tone = uri.toString();
                if (title != null && !title.isEmpty())
                    fragmentCreateAlarmBinding.fragmentCreatealarmSetToneName.setText(title);
            } else {
                fragmentCreateAlarmBinding.fragmentCreatealarmSetToneName.setText("");
            }
        }
    }

    private void updateAlarmInfo(Alarm alarm) {
        fragmentCreateAlarmBinding.fragmentCreatealarmTitle.setText(alarm.getTitle());
        fragmentCreateAlarmBinding.fragmentCreatealarmTimePicker.setHour(alarm.getHour());
        fragmentCreateAlarmBinding.fragmentCreatealarmTimePicker.setMinute(alarm.getMinute());
        fragmentCreateAlarmBinding.snoozeLimitPicker.setValue(alarm.getSnoozeLimit());
        fragmentCreateAlarmBinding.snoozeIntervalPicker.setValue(alarm.getSnoozeInterval());
        if(alarm.isRecurring()){
            fragmentCreateAlarmBinding.fragmentCreatealarmRecurring.setChecked(true);
            fragmentCreateAlarmBinding.fragmentCreatealarmRecurringOptions.setVisibility(View.VISIBLE);
            if(alarm.isMonday())
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckMon.setChecked(true);
            if(alarm.isTuesday())
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckTue.setChecked(true);
            if(alarm.isWednesday())
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckWed.setChecked(true);
            if(alarm.isThursday())
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckThu.setChecked(true);
            if(alarm.isFriday())
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckFri.setChecked(true);
            if(alarm.isSaturday())
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckSat.setChecked(true);
            if(alarm.isSunday())
                fragmentCreateAlarmBinding.fragmentCreatealarmCheckSun.setChecked(true);
            tone=alarm.getTone();
            ringtone = RingtoneManager.getRingtone(getContext(), Uri.parse(tone));
            fragmentCreateAlarmBinding.fragmentCreatealarmSetToneName.setText(ringtone.getTitle(getContext()));
            if(alarm.isVibrate())
                fragmentCreateAlarmBinding.fragmentCreatealarmVibrateSwitch.setChecked(true);
        }
        
        // Set QR code info
        fragmentCreateAlarmBinding.fragmentCreatealarmRequireQr.setChecked(alarm.isRequiresQR());
        if (alarm.isRequiresQR()) {
            fragmentCreateAlarmBinding.qrCodeInputContainer.setVisibility(View.VISIBLE);
            fragmentCreateAlarmBinding.fragmentCreatealarmQrCode.setText(alarm.getQrCode());
        }
    }

    private void generateQRCode() {
        String qrText = fragmentCreateAlarmBinding.fragmentCreatealarmQrCode.getText().toString();
        if (TextUtils.isEmpty(qrText)) {
            Toast.makeText(getContext(), "Please enter text for QR code", Toast.LENGTH_SHORT).show();
            fragmentCreateAlarmBinding.qrCodeImage.setVisibility(View.GONE);
            return;
        }

        try {
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(qrText, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            fragmentCreateAlarmBinding.qrCodeImage.setImageBitmap(bitmap);
            fragmentCreateAlarmBinding.qrCodeImage.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error generating QR code", Toast.LENGTH_SHORT).show();
            fragmentCreateAlarmBinding.qrCodeImage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fragmentCreateAlarmBinding = null;
    }
}