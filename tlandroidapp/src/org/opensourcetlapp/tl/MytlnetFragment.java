package org.opensourcetlapp.tl;

import java.io.IOException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class MytlnetFragment extends Fragment implements Runnable {
	private Button loginButton;
	private EditText usernameEditText;
	private EditText passwordEditText;
	private ProgressDialog progressDialog;
	private TLHandler handler;
	private Context context;
	private Button postsButton;
	private Button pmButton;
	private CheckBox rememberMeCheckBox;
	private DBHelper db;
	private MytlnetFragment instance;

    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig);
        //---code to redraw your activity here---
        //...
    }
    
    

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.mytlnet,container,false);
		
		db = new DBHelper(getActivity());

		loginButton = (Button) view.findViewById(R.id.loginButton);
		usernameEditText = (EditText) view.findViewById(R.id.username);
		passwordEditText = (EditText) view.findViewById(R.id.password);
		context = getActivity();
		setLoginListner();
		
		instance = this;

		pmButton = (Button)view.findViewById(R.id.pmButton);
		pmButton.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent().setClass(context,
						ShowMyPM.class);
				startActivity(intent);
			}
		});
		
		postsButton = (Button) view.findViewById(R.id.viewPostsButton);
		postsButton.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent().setClass(context,
						MyPosts.class);
				startActivity(intent);
			}
		});

		rememberMeCheckBox = (CheckBox) view.findViewById(R.id.rememberMeCheckBox);
		rememberMeCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CheckBox checkBox = (CheckBox) v;
				if (checkBox.isChecked()) {
					db.insertUser(usernameEditText.getText().toString(),
							passwordEditText.getText().toString());
				} else {
					db.deleteUser();
				}
			}
		});
		
		Button settingsButton = (Button) view.findViewById(R.id.settingsButton);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent().setClass(context, Settings.class));
			}
		});

		Cursor userCursor = db.getUser();
		userCursor.moveToFirst();
		if (!userCursor.isAfterLast()) {
			String username = userCursor.getString(userCursor
					.getColumnIndex("username"));
			String password = userCursor.getString(userCursor
					.getColumnIndex("password"));
			int valid = userCursor.getInt(userCursor.getColumnIndex("valid"));

			rememberMeCheckBox.setChecked(true);

			usernameEditText.setText(username);
			passwordEditText.setText(password);

			if (valid == 1) {
				loginButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
						KeyEvent.KEYCODE_DPAD_CENTER));
				loginButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
						KeyEvent.KEYCODE_DPAD_CENTER));
			}
		}

		usernameEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (rememberMeCheckBox.isChecked()) {
					db.insertUser(usernameEditText.getText().toString(),
							passwordEditText.getText().toString());
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub

			}
		});

		passwordEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (rememberMeCheckBox.isChecked()) {
					db.insertUser(usernameEditText.getText().toString(),
							passwordEditText.getText().toString());
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub

			}
		});
		userCursor.close();
		
		return view;
	}

	private void setLoginListner() {
		loginButton.setText("Login");
		Typeface tf = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
		loginButton.setTypeface(tf);
		loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setLoggedIn();
				progressDialog = ProgressDialog.show(getActivity(), null,
						"Logging in...", true, true);
				handler = new LoginHandler(progressDialog, context);
				new Thread((Runnable) instance).start();
			}
		});
	}

	private void setLogoutListener() {
		loginButton.setText("Logout");
		loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TLLib.logout();
				setLoggedOut();
				setLoginListner();
			}
		});
	}
	
	private void setLoggedIn(){
		usernameEditText.setEnabled(false);
		passwordEditText.setEnabled(false);
		pmButton.setEnabled(true);
		postsButton.setEnabled(true);	
	}
	
	private void setLoggedOut(){
		usernameEditText.setEnabled(true);
		passwordEditText.setEnabled(true);
		postsButton.setEnabled(false);
		pmButton.setEnabled(false);
		loginButton.setText("Login");
	}

	private class LoginHandler extends TLHandler {
		public LoginHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY) {
				progressDialog.dismiss();
			} else if (msg.what == 1) {
				setLogoutListener();
			} else if (msg.what == 2) {
				setLoggedOut();
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						context);
				alertDialogBuilder.setTitle("Login Error");
				alertDialogBuilder
						.setMessage("Invalid username and/or password.");
				alertDialogBuilder.setPositiveButton("Okay", null);
				alertDialogBuilder.show();

			} else if (msg.what == 3) {
				setLoggedOut();

			} else {
				super.handleMessage(msg);
			}
		}
	}



	@Override
	public void run() {
		try {
			if (TLLib.login(usernameEditText.getText().toString(),
					passwordEditText.getText().toString(), handler, context)) {
				db.validateUser();
				handler.sendEmptyMessage(1);
			} else {
				db.invalidateUser();
				handler.sendEmptyMessage(2);
			}
		} catch (IOException e) {
			handler.sendEmptyMessage(3);
			handler.progressStatus = TLHandler.PROGRESS_NETWORK_DOWN;
		}
		handler.sendEmptyMessage(0);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu,MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.show_forums_menu, menu);
	}
}
