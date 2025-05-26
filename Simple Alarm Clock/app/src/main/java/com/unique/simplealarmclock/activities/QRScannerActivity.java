package com.unique.simplealarmclock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.unique.simplealarmclock.R;

public class QRScannerActivity extends AppCompatActivity {
    private String expectedQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        // Get the expected QR code from intent
        expectedQrCode = getIntent().getStringExtra("expected_qr_code");
        if (expectedQrCode == null || expectedQrCode.isEmpty()) {
            Toast.makeText(this, "Error: No QR code set for this alarm", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Initialize QR Scanner
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan the QR Code to turn off the alarm"); 
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                // Validate scanned QR code against expected code
                if (result.getContents().equals(expectedQrCode)) {
                    Toast.makeText(this, "QR Code matched! Alarm will be dismissed.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Wrong QR Code! Please scan the correct code." + expectedQrCode, Toast.LENGTH_SHORT).show();
                    // Restart scanning
                    new IntentIntegrator(this).initiateScan();
                }
            } else {
                // User cancelled the scanning
                Toast.makeText(this, "Scanning cancelled", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent going back without scanning
        Toast.makeText(this, "Please scan the correct QR code to turn off the alarm", Toast.LENGTH_SHORT).show();
    }
} 