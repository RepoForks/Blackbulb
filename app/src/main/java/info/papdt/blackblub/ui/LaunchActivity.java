package info.papdt.blackblub.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import info.papdt.blackblub.C;
import info.papdt.blackblub.R;
import info.papdt.blackblub.services.MaskService;
import info.papdt.blackblub.utils.Settings;

public class LaunchActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

	private MessageReceiver mReceiver;

	private DiscreteSeekBar mSeekbar;
	private MaterialAnimatedSwitch mSwitch;

	private PopupMenu popupMenu;

	private boolean isRunning = false;
	private Settings mSettings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mSettings = Settings.getInstance(getApplicationContext());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				getWindow().setStatusBarColor(Color.TRANSPARENT);
				getWindow().setNavigationBarColor(Color.TRANSPARENT);
			}
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);

		// A foolish method to check if it was enabled. Sometimes it may get a wrong result.
		isRunning = mSettings.getBoolean(Settings.KEY_ALIVE, false);

		mSwitch = (MaterialAnimatedSwitch) findViewById(R.id.toggle);
		if (isRunning) {
			// If I don't use postDelayed, Switch will cause a NPE because its animator wasn't initialized.
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					mSwitch.toggle();
				}
			}, 200);
		}
		mSwitch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(boolean b) {
				if (b) {
					Intent intent = new Intent(LaunchActivity.this, MaskService.class);
					intent.putExtra(C.EXTRA_ACTION, C.ACTION_START);
					intent.putExtra(C.EXTRA_BRIGHTNESS, mSeekbar.getProgress());
					startService(intent);
					isRunning = true;
				} else {
					Intent intent = new Intent(LaunchActivity.this, MaskService.class);
					intent.putExtra(C.EXTRA_ACTION, C.ACTION_STOP);
					stopService(intent);
					isRunning = false;
				}
			}
		});

		mSeekbar = (DiscreteSeekBar) findViewById(R.id.seek_bar);
		mSeekbar.setProgress(mSettings.getInt(Settings.KEY_BRIGHTNESS, 50));
		mSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
			@Override
			public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
				if (isRunning) {
					Intent intent = new Intent(LaunchActivity.this, MaskService.class);
					intent.putExtra(C.EXTRA_ACTION, C.ACTION_UPDATE);
					intent.putExtra(C.EXTRA_BRIGHTNESS, mSeekbar.getProgress());
					startService(intent);
				}
			}

			@Override
			public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

			}
		});

		ImageButton menuBtn = (ImageButton) findViewById(R.id.btn_menu);
		popupMenu = new PopupMenu(this, menuBtn);
		popupMenu.getMenuInflater().inflate(R.menu.menu_settings, popupMenu.getMenu());
		popupMenu.getMenu()
				.findItem(R.id.action_overlay_system)
				.setChecked(mSettings.getBoolean(Settings.KEY_OVERLAY_SYSTEM, false));
		popupMenu.setOnMenuItemClickListener(this);
		menuBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				popupMenu.show();
			}
		});
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			menuBtn.setOnTouchListener(popupMenu.getDragToOpenListener());
		}

		FrameLayout rootLayout = (FrameLayout) findViewById(R.id.root_layout);
		rootLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				finish();
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		if (mReceiver == null) {
			mReceiver = new MessageReceiver();
		}
		IntentFilter filter = new IntentFilter();
		filter.addAction(LaunchActivity.class.getCanonicalName());
		registerReceiver(mReceiver, filter);
	}

	@Override
	public void onPause() {
		super.onPause();
		mSettings.putInt(Settings.KEY_BRIGHTNESS, mSeekbar.getProgress());
	}

	@Override
	public void onStop() {
		super.onStop();
		unregisterReceiver(mReceiver);
	}

	@Override
	public boolean onMenuItemClick(final MenuItem menuItem) {
		int id = menuItem.getItemId();
		if (id == R.id.action_about) {
			new AlertDialog.Builder(this)
					.setView(R.layout.dialog_about)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							// Do nothing....
						}
					})
					.show();
			return true;
		} else if (id == R.id.action_overlay_system) {
			if (menuItem.isChecked()) {
				mSettings.putBoolean(Settings.KEY_OVERLAY_SYSTEM, false);
				menuItem.setChecked(false);
			} else {
				new AlertDialog.Builder(this)
						.setTitle(R.string.dialog_overlay_enable_title)
						.setMessage(R.string.dialog_overlay_enable_message)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								mSettings.putBoolean(Settings.KEY_OVERLAY_SYSTEM, true);
								menuItem.setChecked(true);
							}
						})
						.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								// Do nothing....
							}
						})
						.show();
			}
			return true;
		}
		return false;
	}

	private class MessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int eventId = intent.getIntExtra(C.EXTRA_EVENT_ID, -1);
			switch (eventId) {
				case C.EVENT_CANNOT_START:
					// Receive a error from MaskService
					mSettings.putBoolean(Settings.KEY_ALIVE, false);
					isRunning = false;
					try {
						mSwitch.toggle();
						Toast.makeText(
								LaunchActivity.this,
								R.string.mask_fail_to_start,
								Toast.LENGTH_LONG
						).show();
					} finally {

					}
					break;
			}
		}

	}

}