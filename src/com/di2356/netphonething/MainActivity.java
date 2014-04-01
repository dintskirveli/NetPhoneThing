package com.di2356.netphonething;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xbill.DNS.*;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private static final String TAG = "NETTHING";
	private EditText mEditText;
	private TextView mErrorText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mEditText = (EditText) findViewById(R.id.urlinput);
		mErrorText = (TextView) findViewById(R.id.errorText);
	}
	
	public void buttonClick(View view) {	
		//new LookupTask().execute(mEditText.getText().toString());
	    new getNumberTask().execute(mEditText.getText().toString());
	}
	
	class getNumberTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String url = "http://"+params[0]+"/.well-known/phone-number.txt";
            

            String response = "";
            try {
                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(new URI(url));
                HttpResponse execute = client.execute(httpGet);
                InputStream content = execute.getEntity().getContent();

                BufferedReader buffer = new BufferedReader(
                        new InputStreamReader(content));
                String s = "";
                while ((s = buffer.readLine()) != null) {
                    response += s;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
            return "tel="+response;
        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result.indexOf("tel=") != -1) {
                String tel = result.replace("tel=", "");
                makePhoneCall(tel);
                mErrorText.setText("");
            } else {
                mErrorText.setText("");
            }
        }
	    
	}
	
	class LookupTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... arg0) {
			String domain = arg0[0];
			Log.e(TAG, "domain: "+domain);
			 try {
				Lookup lookup = new Lookup(domain, Type.ANY);
				Resolver resolver = new SimpleResolver();
				lookup.setResolver(resolver);
			    lookup.setCache(null);
			    Record[] records = lookup.run();
			    if(lookup.getResult() == Lookup.SUCCESSFUL)
			    {
			      String responseMessage = "";
			      for (int i = 0; i < records.length; i++)
			      {
			        if(records[i] instanceof TXTRecord)
			        {
			          TXTRecord txt = (TXTRecord) records[i];
			          for(Iterator j = txt.getStrings().iterator(); j.hasNext();)
			          {
			            responseMessage += (String)j.next();
			          }
			        }
			      }

			      return responseMessage;
			    }
			    else if(lookup.getResult() == Lookup.HOST_NOT_FOUND)
			    {
			    	 Log.e(TAG, "Not found.");
			    	 return "Host not found";
			    }
			    else
			    {
			      System.err.println(lookup.getErrorString());
			      return lookup.getErrorString();
			    }
			} catch (TextParseException e) {
				return "TextParseException";
			} catch (UnknownHostException e) {
				return "UnknownHostException";
			}
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (result.indexOf("tel") != -1) {
				String tel = result.replace("tel=", "");
				makePhoneCall(tel);
				mErrorText.setText("");
			} else {
				mErrorText.setText(result);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void makePhoneCall(String number) {
	    Intent intent = new Intent(Intent.ACTION_CALL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("tel:" + number));
        getApplicationContext().startActivity(intent);
	}

}
