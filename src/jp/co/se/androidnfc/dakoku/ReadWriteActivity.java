package jp.co.se.androidnfc.dakoku;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import jp.co.se.androidnfc.chapter03.R;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.TextView;

public class ReadWriteActivity extends BaseActivity {

	private ProgressDialog mProgressDialog = null;
	String flg = "";
	String m_idm = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_readwrite);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mNfcAdapter != null) {
			// 起動中のアクティビティが優先的にNFCを受け取れるよう設定
			Intent intent = new Intent(this, this.getClass())
					.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
			mNfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mNfcAdapter != null) {
			// アクテイビティが非表示になる際に優先的にNFCを受け取る設定を解除
			mNfcAdapter.disableForegroundDispatch(this);
		}
	}

	@SuppressLint("NewApi")
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String action = intent.getAction();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
				|| NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
				|| NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
			// idmを取得
			byte[] idm = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
			m_idm = NfcUtil.bytesToHex(idm);
			reqIdm(NfcUtil.bytesToHex(idm));
		}
	}

	public void reqIdm(String idm) {

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日HH時mm分ss秒", Locale.JAPAN);
		String dakoku_time = format.format(cal.getTime());
		Log.d("aa", dakoku_time);

		RadioGroup radioGroup = (RadioGroup) ReadWriteActivity.this.findViewById(R.id.rgroup);
		int id = radioGroup.getCheckedRadioButtonId();
		if (id == R.id.radio0) {
			flg = CommonConst.btnFlg.start;
		} else {
			flg = CommonConst.btnFlg.end;
		}

		new AsyncTask<NameValuePair, Integer, String>() {

			@Override
			protected void onPreExecute() {
				mProgressDialog = new ProgressDialog(ReadWriteActivity.this);
				mProgressDialog.setTitle("送信中");
				mProgressDialog.setIndeterminate(true);
				mProgressDialog.show();
			}

			@Override
			protected String doInBackground(NameValuePair... params) {
				try {
					HttpPost post = new HttpPost("http://54.250.182.30:8080/dakoku/");
					post.setEntity(new UrlEncodedFormEntity(Arrays.asList(params), "UTF-8"));
					AndroidHttpClient client = AndroidHttpClient.newInstance("Android UserAgent");
					String result = client.execute(post, new ResponseHandler<String>() {
						@Override
						public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
							Log.d("sample", String.valueOf(response.getStatusLine().getStatusCode()));
							Log.d("sample", response.getStatusLine().toString());
							return EntityUtils.toString(response.getEntity(), "UTF-8");
						}
					});
					Log.d("sample", result);
					client.close();
					Integer progress = 10;
					publishProgress(progress);
					return result;
				} catch (Exception e) {
					Log.w("sample", e.getMessage().toString());
					return null;
				}
			}

			@Override
			protected void onProgressUpdate(Integer... progress) {
				mProgressDialog.setProgress(progress[0]);
			}

			@Override
			protected void onPostExecute(String result) {
				Log.d("sample2", result);
				String res = result.substring(4, 6);
				if (res.equals("00")) {
					mProgressDialog.dismiss();
					TextView tvRead = (TextView) findViewById(R.id.Read);
					String msg = "";
					if (flg.equals(CommonConst.btnFlg.start)) {
						msg = "おはようございます!　";
					} else {
						msg = "お疲れさまでした!　";
					}
					tvRead.setText(msg + result.substring(12) + "さん");
				} else {
					mProgressDialog.dismiss();
					TextView tvRead = (TextView) findViewById(R.id.Read);
					tvRead.setText(m_idm + "　エラーとなりました!");
				}
			}
		}.execute(
				new BasicNameValuePair("param1", "3"),
				new BasicNameValuePair("param2", idm),
				new BasicNameValuePair("param3", dakoku_time),
				new BasicNameValuePair("param4", flg)
				);
	}
}
