package com.ianhanniballake.recipebook.auth;

import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.plus.PlusClient;
import com.ianhanniballake.recipebook.R;

/**
 * Activity class that manages user authorization
 */
public class AuthorizedActivity extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener
{
	private static final int REQUEST_ACCOUNT_RESOLUTION = 401;
	private ConnectionResult latestResult = null;
	private PlusClient plusClient = null;
	private boolean resolveOnFail = false;

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_ACCOUNT_RESOLUTION:
				if (resultCode == RESULT_OK)
				{
					resolveOnFail = true;
					plusClient.connect();
				}
				return;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onConnected()
	{
		resolveOnFail = false;
		invalidateOptionsMenu();
	}

	@Override
	public void onConnectionFailed(final ConnectionResult result)
	{
		if (result.hasResolution())
		{
			latestResult = result;
			if (resolveOnFail)
				startResolution();
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		plusClient = new PlusClient.Builder(this, this, this)
				.setVisibleActivities("http://schemas.google.com/CreateActivity")
				.setScopes(Scopes.PLUS_LOGIN, "https://www.googleapis.com/auth/drive.appdata").build();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.auth, menu);
		final MenuItem signIn = menu.findItem(R.id.sign_in);
		final SignInButton signInButton = (SignInButton) signIn.getActionView();
		if (signInButton != null)
			signInButton.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(final View v)
				{
					onSignInClicked();
				}
			});
		return true;
	}

	@Override
	public void onDisconnected()
	{
		invalidateOptionsMenu();
		plusClient.connect();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.sign_in:
				onSignInClicked();
				return true;
			case R.id.sign_out:
				plusClient.clearDefaultAccount();
				plusClient.disconnect();
				invalidateOptionsMenu();
				plusClient.connect();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		final boolean isConnected = plusClient.isConnected();
		final MenuItem signIn = menu.findItem(R.id.sign_in);
		signIn.setVisible(!isConnected);
		final MenuItem signOut = menu.findItem(R.id.sign_out);
		signOut.setVisible(isConnected);
		return true;
	}

	/**
	 * Callback for user initiated sign in process
	 */
	void onSignInClicked()
	{
		if (latestResult != null)
			startResolution();
	}

	@Override
	protected void onStart()
	{
		plusClient.connect();
		super.onStart();
	}

	private void startResolution()
	{
		try
		{
			latestResult.startResolutionForResult(this, REQUEST_ACCOUNT_RESOLUTION);
		} catch (final SendIntentException e)
		{
			Log.e(AuthorizedActivity.class.getSimpleName(), "Error resolving result " + latestResult.getErrorCode(), e);
			// Try, try again
			latestResult = null;
			plusClient.connect();
		}
	}
}