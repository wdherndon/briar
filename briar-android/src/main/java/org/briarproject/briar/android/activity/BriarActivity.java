package org.briarproject.briar.android.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;

import org.briarproject.briar.android.controller.BriarController;
import org.briarproject.briar.android.controller.DbController;
import org.briarproject.briar.android.controller.handler.UiResultHandler;
import org.briarproject.briar.android.login.PasswordActivity;
import org.briarproject.briar.android.panic.ExitActivity;

import java.util.logging.Logger;

import javax.inject.Inject;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

@SuppressLint("Registered")
public abstract class BriarActivity extends BaseActivity {

	public static final String KEY_STARTUP_FAILED = "briar.STARTUP_FAILED";
	public static final String GROUP_ID = "briar.GROUP_ID";
	public static final String GROUP_NAME = "briar.GROUP_NAME";

	public static final int REQUEST_PASSWORD = 1;

	private static final Logger LOG =
			Logger.getLogger(BriarActivity.class.getName());

	@Inject
	BriarController briarController;

	@Deprecated
	@Inject
	DbController dbController;

	@Override
	protected void onActivityResult(int request, int result, Intent data) {
		super.onActivityResult(request, result, data);
		if (request == REQUEST_PASSWORD) {
			if (result == RESULT_OK) briarController.startAndBindService();
			else finish();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!briarController.hasEncryptionKey() && !isFinishing()) {
			Intent i = new Intent(this, PasswordActivity.class);
			i.setFlags(FLAG_ACTIVITY_NO_ANIMATION | FLAG_ACTIVITY_SINGLE_TOP);
			startActivityForResult(i, REQUEST_PASSWORD);
		}
	}

	protected void signOut(final boolean removeFromRecentApps) {
		briarController.signOut(new UiResultHandler<Void>(this) {
			@Override
			public void onResultUi(Void result) {
				if (removeFromRecentApps) startExitActivity();
				else finishAndExit();
			}
		});
	}

	protected void signOut() {
		signOut(false);
	}

	private void startExitActivity() {
		Intent i = new Intent(this, ExitActivity.class);
		i.addFlags(FLAG_ACTIVITY_NEW_TASK
				| FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
				| FLAG_ACTIVITY_NO_ANIMATION
				| FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
	}

	private void finishAndExit() {
		if (Build.VERSION.SDK_INT >= 21) finishAndRemoveTask();
		else finish();
		LOG.info("Exiting");
		System.exit(0);
	}

	@Deprecated
	public void runOnDbThread(Runnable task) {
		dbController.runOnDbThread(task);
	}

	@Deprecated
	protected void finishOnUiThread() {
		runOnUiThreadUnlessDestroyed(new Runnable() {
			@Override
			public void run() {
				finish();
			}
		});
	}
}