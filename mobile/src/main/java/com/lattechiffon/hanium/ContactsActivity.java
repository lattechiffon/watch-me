package com.lattechiffon.hanium;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

//import android.content.Intent;


public class ContactsActivity extends AppCompatActivity {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String result;
    BackgroundTask task;

    ListView listView;
    ContactsListViewAdapter adapter;

    public ContactsActivity() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.activity_contacts));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new ContactsListViewAdapter();

        listView = (ListView) findViewById(R.id.listViewContacts);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(listener);
        listView.setOnItemLongClickListener(longClickListener);

        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(listView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                if (adapter.getCount() > 1) {
                                    return true;
                                }

                                return false;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    adapter.remove(adapter.getItem(position));
                                }
                            }
                        });
        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());

        pref = getSharedPreferences("UserData", Activity.MODE_PRIVATE);
        editor = pref.edit();

        task = new BackgroundTask();
        task.execute();
    }

    AdapterView.OnItemLongClickListener longClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
            ContactsListViewItem item = (ContactsListViewItem) adapterView.getItemAtPosition(position);

            String name = item.getName();
            String phone = item.getPhone();

            Toast.makeText(ContactsActivity.this, "이렇게 길게 누르면 " + name + " 님을 응급 알림 대상자에서 해제할 지의 여부를 묻습니다.", Toast.LENGTH_LONG).show();

            return true;
        }
    };

    AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            ContactsListViewItem item = (ContactsListViewItem) adapterView.getItemAtPosition(position);

            String name = item.getName();
            String phone = item.getPhone();

            Toast.makeText(ContactsActivity.this, "이렇게 한 번 누르면 그동안 " + name + " 님에게 전달된 알림 목록을 보여줍니다.", Toast.LENGTH_LONG).show();
            //Intent intent = new Intent(getContext(), UserInfoScrollingActivity.class);
            //startActivity(intent);
        }
    };

    private boolean getContactsData(String loginData) {
        try {
            JSONObject json = new JSONObject(loginData);
            Iterator<String> keys = json.keys();

            if (json.getString(keys.next()).equals("Authorized")) {

                while (keys.hasNext()) {
                    String key = keys.next();
                    adapter.addItem(json.getString(key), key);
                }

                return true;
            } else {
                adapter.addItem("등록된 전화번호가 없습니다.", "연락처가 등록되어 있는지 확인해주세요.");
                Toast.makeText(ContactsActivity.this, "소속 친구 데이터를 가져오지 못했습니다.", Toast.LENGTH_LONG).show();

                return false;
            }
        } catch (JSONException e) {
            adapter.addItem("등록된 전화번호가 없습니다.", "연락처가 등록되어 있는지 확인해주세요.");
            Toast.makeText(ContactsActivity.this, "로그인에 실패하였습니다.", Toast.LENGTH_LONG).show();

            return false;
        }
    }

    private class BackgroundTask extends AsyncTask<String, Integer, String> {
        ProgressDialog AsycDialog = new ProgressDialog(ContactsActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            AsycDialog.setMessage("잠시만 기다려주십시오.");
            AsycDialog.show();
        }

        @Override
        protected String doInBackground(String... arg0) {
            result = request("http://www.lattechiffon.com/gauss/app/load_data.php");

            return result;
        }

        @Override
        protected void onPostExecute(String a) {
            super.onPostExecute(a);
            AsycDialog.dismiss();

            if (getContactsData(result)) {
                Toast.makeText(ContactsActivity.this, "소속 친구 데이터를 성공적으로 가져왔습니다.", Toast.LENGTH_LONG).show();

                //Intent intent = new Intent(getContext(), MainActivity.class);
                //startActivity(intent);
            }

            adapter.notifyDataSetChanged();
        }
    }

    private String request(String urlStr) {
        StringBuilder json = new StringBuilder();
        String parameter = "id=" + pref.getString("id", null) + "&pw=" + pref.getString("pw", null) + "&destination=friends";

        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            if (conn != null) {
                conn.setConnectTimeout(10000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                OutputStream os = conn.getOutputStream();
                os.write(parameter.getBytes("utf-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;

                    while ((line = reader.readLine()) != null) {
                        json.append(line).append("");
                    }

                    reader.close();

                }
                conn.disconnect();
            }
        } catch (Exception e) {
            Log.e("SampleHTTP", "Exception in processing response.", e);
            e.printStackTrace();
        }

        return json.toString();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);

                break;
        }

        return true;
    }
}