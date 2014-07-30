package com.secandcode.testfolderdialog;


import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.secandcode.testfolderdialog.R;
import com.secandcode.*;
public class MainActivity extends Activity {

	void toMenuOnClick()
	{
		Intent intent = new Intent(this, DirectoryPicker.class);
		
		final CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox1);
		if (checkBox.isChecked()) 
			intent.putExtra(DirectoryPicker.USE_ROOT, true);
		else
			intent.putExtra(DirectoryPicker.USE_ROOT, false);
		
		final CheckBox checkBox2 = (CheckBox) findViewById(R.id.checkBox2);
		
		if (checkBox2.isChecked())
			intent.putExtra(DirectoryPicker.ONLY_DIRS, false);
		else
			intent.putExtra(DirectoryPicker.ONLY_DIRS, true);
		
		intent.putExtra(DirectoryPicker.START_DIR, "/");
    	startActivityForResult(intent, DirectoryPicker.PICK_DIRECTORY);
    	
	}
	
	
    @Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
      	if(requestCode == DirectoryPicker.PICK_DIRECTORY && resultCode == RESULT_OK) {
      		Bundle extras = data.getExtras();
      		String path = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY);
      		
      		View tv = this.findViewById(R.id.textView1);
          	((TextView)tv).setText(path);
      	}
      	if(requestCode == DirectoryPicker.PICK_DIRECTORY && resultCode == RESULT_CANCELED) {
      		
      		View tv = findViewById(R.id.textView1);
          	((TextView)tv).setText("No Folder Selected");
      	}
      }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	
		Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
  
        	  toMenuOnClick();
          }
        });
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
