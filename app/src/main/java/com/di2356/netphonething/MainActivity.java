package com.di2356.netphonething;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
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

        private String getNumberOrNullFromDNS(String domain) {
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
                    //got record here, look for number..
                    if (responseMessage.contains("phone-dns")) {
                        String num = responseMessage.substring(responseMessage.indexOf("phone-dns")).split("=")[1].replaceAll("[^\\d]","");
                        Log.e("number", num);
                        return num;
                    } else {
                        return null;
                    }
                }
                else if(lookup.getResult() == Lookup.HOST_NOT_FOUND)
                {
                    Log.e(TAG, "Not found.");
                    return null;
                }
                else
                {
                    System.err.println(lookup.getErrorString());
                    return null;
                }
            } catch (TextParseException e) {
                return null;
            } catch (UnknownHostException e) {
                return null;
            }
        }

        private String getNumberOrNullFromAPI(String domain) {
            StringBuilder builder = new StringBuilder();
            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet("http://phonedns-server.herokuapp.com/"+domain+"/en_US");
            try {
                HttpResponse response = client.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                } else {
                    Log.e("readnumber", "Failed to download file");
                    return null;
                }
            } catch (Exception e) {
                return null;
            }

            //we supposedly have our JSON
            String js = builder.toString();
            try {
                JSONObject json = new JSONObject(js);
                if (json.getJSONObject("response").getBoolean("success")) {
                    return json.getJSONObject("response").getString("number");
                }
            } catch (Exception e) {
                return null;
            }
            return null;
        }

        @Override
        protected String doInBackground(String... params) {
            String domain = params[0];
            String number;
            number = getNumberOrNullFromDNS(domain);
            if(number != null) {
                return "tel="+number;
            }
            number = getNumberOrNullFromAPI(domain);
            if(number == null) {
                return "number not found";
            } else {
                return "tel="+number;
            }


        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result.indexOf("tel=") != -1) {
                String tel = result.replace("tel=", "");
                makePhoneCall(tel);
                mErrorText.setText("");
            } else {
                mErrorText.setText("Couldn't find a number for that domain.");
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
