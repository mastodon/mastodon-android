package org.joinmastodon.android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.NonNull;

import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;

import de.markusfisch.android.barcodescannerview.widget.BarcodeScannerView;

public class QrCodeScanActivity extends Activity{
	private static final int PERMISSION_RESULT=65537;
	private BarcodeScannerView scannerView;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		UiUtils.setUserPreferredTheme(this);
		setContentView(R.layout.activity_qr_scan);

		if(this.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
			requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_RESULT);
		}

		findViewById(R.id.dismiss).setOnClickListener(view -> finish());
		scannerView=findViewById(R.id.scanner);
		scannerView.setCropRatio(.75f);
		scannerView.setOnBarcodeListener(barcode -> {
			vibrate(scannerView);
			Intent result=new Intent();
			result.putExtra("barcode", barcode.getText());
			setResult(RESULT_OK, result);
			finish();
			return false;
		});
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
		if(requestCode==PERMISSION_RESULT){
			if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
				scannerView.openAsync();
			}else if(!this.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
				new M3AlertDialogBuilder(this)
						.setTitle(R.string.permission_required)
						.setMessage(R.string.camera_permission_to_scan)
						.setPositiveButton(R.string.open_settings, (dialog, which)->this.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", this.getPackageName(), null))))
						.setNegativeButton(R.string.cancel, (dialogInterface, i) -> finish())
						.setOnCancelListener(dialogInterface -> finish())
						.show();
			}
		}
	}


	private static void vibrate(View v) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
				return;
		}

		Vibrator vibrator=v.getContext().getSystemService(Vibrator.class);

		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
			vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			VibrationEffect effect=VibrationEffect.createOneShot(75L, 128);
			vibrator.vibrate(effect);
		} else {
			vibrator.vibrate(75L);
		}
	}


	@Override
	public void onResume() {
		super.onResume();
		scannerView.openAsync();
	}

	@Override
	public void onPause() {
		super.onPause();
		scannerView.close();
	}
}
