package com.k3.rtlion;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.design.widget.TextInputLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainPageFrag {

    private Activity activity;
    private Context context;
    private ViewGroup viewGroup;
    private TextInputLayout tilHostAddr;
    private EditText edtxHostAddr;
    private Button btnConnect;
    private String hostAddr, portNum;

    public MainPageFrag(Activity activity, ViewGroup viewGroup){
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.viewGroup = viewGroup;
    }

    private void initViews(){
        tilHostAddr = (TextInputLayout) viewGroup.findViewById(R.id.tilHostAddr);
        edtxHostAddr = (EditText) viewGroup.findViewById(R.id.edtxHostAddr);
        btnConnect = (Button) viewGroup.findViewById(R.id.btnConnect);
        edtxHostAddr.setTypeface(new SplashScreen(activity).getUbuntuMonoFont());
    }

    public void initialize(){
        initViews();
        edtxHostAddr.setOnEditorActionListener(new edtxHostAddr_onEditorAction());
        btnConnect.setOnClickListener(new btnConnect_onClick());
    }
    private class edtxHostAddr_onEditorAction implements TextView.OnEditorActionListener{
        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            if (i == EditorInfo.IME_ACTION_SEND) {
                tryConnect();
                return true;
            }
            return false;
        }
    }
    private class btnConnect_onClick implements Button.OnClickListener{
        @Override
        public void onClick(View v) {
            tryConnect();
        }
    }

    private void tryConnect(){

    }

}
