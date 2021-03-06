package com.k3.rtlion.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;
import com.k3.rtlion.handler.ImageBase64;
import com.k3.rtlion.handler.JSInterface;
import com.k3.rtlion.R;
import com.k3.rtlion.view.XViewPager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

@SuppressWarnings("FieldCanBeLocal")
public class ScannerPageFrag {

    private Activity activity;
    private Context context;
    private ViewGroup viewGroup;
    private JSInterface jsInterface;
    private String hostAddr;
    private JSONObject cliArgs;
    private int minSens = 1,
                maxSens = 10,
                sensStep = 1,
                defaultSensitivity = 4,
                currentSensitivity = 4,
                centerFreq, minFreq, maxFreq,
                stepSize, selectedFrequency,
                numRead, maxRead;
    private static int refreshDuration = 800;
    public boolean viewsHidden = false;
    private boolean showGraph = false;
    private ArrayList<String> freqRes, dbRes;
    private ArrayAdapter<String> arrayAdapterRes;
    private Bitmap fftBitmap;
    private Object[] uiObjects;

    private TextView txvScannerWarning, txvScanSensitivity, txvFreqRange, txvScanPerc;
    private LinearLayout llScanner, llScanResults;
    private SwipeRefreshLayout swpScanner;
    private SeekBar sbScanSensitivity;
    private TextInputLayout tilFreqMin, tilFreqMax;
    private EditText edtxFreqMin, edtxFreqMax;
    private Button btnStartScan;
    private PhotoView imgFreqScan;
    private ListView lstScanResults;
    private View viewResSeparate;

    public ScannerPageFrag(Activity activity, ViewGroup viewGroup, JSInterface jsInterface){
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.viewGroup = viewGroup;
        this.jsInterface = jsInterface;
    }
    private void initViews(){
        txvScannerWarning = viewGroup.findViewById(R.id.txvScannerWarning);
        llScanner = viewGroup.findViewById(R.id.llScanner);
        llScanResults = viewGroup.findViewById(R.id.llScanResults);
        swpScanner = viewGroup.findViewById(R.id.swpScanner);
        sbScanSensitivity = viewGroup.findViewById(R.id.sbScanSensitivity);
        txvScanSensitivity = viewGroup.findViewById(R.id.txvScanSensitivity);
        txvScanPerc = viewGroup.findViewById(R.id.txvScanPerc);
        tilFreqMin = viewGroup.findViewById(R.id.tilFreqMin);
        tilFreqMax = viewGroup.findViewById(R.id.tilFreqMax);
        edtxFreqMin = viewGroup.findViewById(R.id.edtxFreqMin);
        edtxFreqMax = viewGroup.findViewById(R.id.edtxFreqMax);
        btnStartScan = viewGroup.findViewById(R.id.btnStartScan);
        imgFreqScan = viewGroup.findViewById(R.id.imgFreqScan);
        txvFreqRange = viewGroup.findViewById(R.id.txvFreqRange);
        lstScanResults = viewGroup.findViewById(R.id.lstScanResults);
        viewResSeparate = viewGroup.findViewById(R.id.viewResSeparate);
    }
    private void initSeekBar(){
        sbScanSensitivity.setOnSeekBarChangeListener(new sbScanSensitivity_onChange());
        sbScanSensitivity.setMax((maxSens - minSens) / sensStep);
        sbScanSensitivity.setProgress(defaultSensitivity);
    }
    public void initialize(){
        initViews();
        initSeekBar();
        txvScannerWarning.setVisibility(View.VISIBLE);
        llScanner.setVisibility(View.GONE);
        btnStartScan.setOnClickListener(new btnStartScan_onClick());
        lstScanResults.setOnTouchListener(new lstScanResults_onTouch());
        lstScanResults.setOnItemClickListener(new lstScanResults_onItemClick());
        swpScanner.setOnRefreshListener(new swpScanner_onRefresh());
        freqRes = new ArrayList<>();
        dbRes = new ArrayList<>();
    }
    public void setUIObjects(Object[] uiObjects){
        txvScannerWarning.setVisibility(View.GONE);
        llScanner.setVisibility(View.VISIBLE);
        this.uiObjects = uiObjects;
        this.hostAddr = ((MainPageFrag) uiObjects[1]).getHostAddr();
    }
    public void setCliArgs(JSONObject cliArgs){
        try {
            if(cliArgs == null)
                throw new JSONException(context.getString(R.string.invalid_args));
            this.cliArgs = cliArgs;
            centerFreq = Integer.valueOf(cliArgs.getString("freq"));
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    edtxFreqMin.setText(String.valueOf(centerFreq - (centerFreq/5)));
                    edtxFreqMax.setText(String.valueOf(centerFreq + (centerFreq/5)));
                }
            });
        }catch (JSONException e){
            e.printStackTrace();
            Toast.makeText(activity, context.getString(R.string.invalid_server_settings),
                    Toast.LENGTH_SHORT).show();
        }
    }
    private boolean checkRange(){
        try {
            minFreq = Integer.parseInt(edtxFreqMin.getText().toString());
            maxFreq = Integer.parseInt(edtxFreqMax.getText().toString());
            if (maxFreq > minFreq)
                return true;
        }catch (Exception e){
            return false;
        }
        return false;
    }
    private class lstScanResults_onTouch implements ListView.OnTouchListener{
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        }
    }
    @SuppressWarnings("ConstantConditions")
    private class lstScanResults_onItemClick implements ListView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String[] dialogOptions = context.getResources().getStringArray(R.array.freq_options);
            selectedFrequency = (int) (Double.parseDouble(freqRes.get(position)) * Math.pow(10, 6));
            AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    android.R.style.Theme_DeviceDefault_Light_Dialog);
            builder.setTitle(freqRes.get(position) + " MHz");
            builder.setItems(dialogOptions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int option) {
                    try {
                        switch (option) {
                            case 0:
                                ClipboardManager clipboard = (ClipboardManager) context.
                                        getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clipData = ClipData.newPlainText(context.
                                        getString(R.string.app_name),
                                        String.valueOf(selectedFrequency));
                                clipboard.setPrimaryClip(clipData);
                                Toast.makeText(activity, context.getString(R.string.clipboard_copy),
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                if(viewsHidden) {
                                    showGraph = true;
                                    centerFreq = maxFreq;
                                }else{
                                    showSelectedFreqGraph();
                                }
                                break;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }
    private class sbScanSensitivity_onChange implements SeekBar.OnSeekBarChangeListener{
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            ((XViewPager)uiObjects[0]).allowSwiping(false);
        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            ((XViewPager)uiObjects[0]).allowSwiping(true);
        }
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            currentSensitivity = minSens + (progress * sensStep);
            txvScanSensitivity.setText(String.valueOf(currentSensitivity));
        }
    }
    private class swpScanner_onRefresh implements SwipeRefreshLayout.OnRefreshListener{
        @Override
        public void onRefresh() {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    swpScanner.setRefreshing(false);
                    ((SettingsPageFrag) uiObjects[2]).updatedSettings = null;
                    ((SettingsPageFrag) uiObjects[2]).getArgsFromServer();
                }
            }, refreshDuration);
        }
    }
    private void hideViews(boolean state){
        if (state){
            edtxFreqMin.setVisibility(View.GONE);
            edtxFreqMax.setVisibility(View.GONE);
            tilFreqMin.setVisibility(View.GONE);
            tilFreqMax.setVisibility(View.GONE);
            txvFreqRange.setVisibility(View.VISIBLE);
            llScanResults.setVisibility(View.VISIBLE);
            viewResSeparate.setVisibility(View.VISIBLE);
            btnStartScan.setText(context.getString(R.string.stop_graph));
            viewsHidden = true;
        }else{
            edtxFreqMin.setVisibility(View.VISIBLE);
            edtxFreqMax.setVisibility(View.VISIBLE);
            tilFreqMin.setVisibility(View.VISIBLE);
            tilFreqMax.setVisibility(View.VISIBLE);
            btnStartScan.setText(context.getString(R.string.start_scan));
            viewsHidden = false;
        }
    }
    private void enableViews(boolean state) {
        edtxFreqMin.setEnabled(state);
        edtxFreqMax.setEnabled(state);
        btnStartScan.setEnabled(true);
        txvScanPerc.setText("");
    }
    private void showSelectedFreqGraph(){
        ((XViewPager)uiObjects[0]).setCurrentItem(2);
        ((GraphPageFrag)uiObjects[3]).showGraph(selectedFrequency);
    }
    private class btnStartScan_onClick implements Button.OnClickListener{
        @Override
        public void onClick(View v) {
            if (viewsHidden) {
                centerFreq = maxFreq;
            }else if(!((GraphPageFrag)uiObjects[3]).viewsHidden){
                if (checkRange()) {
                    try {
                        numRead = 0;
                        freqRes.clear();
                        dbRes.clear();
                        arrayAdapterRes = new ArrayAdapter<String>(activity,
                                android.R.layout.simple_list_item_1,
                                freqRes) {
                            @Override
                            public @NonNull
                            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                TextView txvItem = view.findViewById(android.R.id.text1);
                                txvItem.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                                txvItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                                return view;
                            }
                        };
                        lstScanResults.setAdapter(arrayAdapterRes);
                        stepSize = 2 * (int) Math.pow(10, (int) Math.log10(maxFreq - minFreq) - 1);
                        maxRead = ((maxFreq - minFreq) / stepSize);
                        enableViews(false);
                        txvFreqRange.setText(String.valueOf(minFreq) + "-" +
                                String.valueOf(maxFreq));
                        btnStartScan.setText(context.getString(R.string.graph_wait));
                        btnStartScan.setEnabled(false);
                        setDevFrequency(minFreq);
                    }catch (Exception e){
                        Toast.makeText(activity, context.getString(R.string.invalid_settings),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, context.getString(R.string.invalid_settings),
                            Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(activity, context.getString(R.string.framework_busy),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void setDevFrequency(int freq){
        try {
            if (cliArgs == null)
                throw new JSONException(context.getString(R.string.invalid_settings));
            centerFreq = freq;
            cliArgs.put("freq", centerFreq);
            jsInterface.setServerArgs(hostAddr, cliArgs.toString(),
                    new JSInterface.JSOutputInterface() {
                        @Override
                        public void onInfo(JSONObject clientInfo) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    scanFrequencyRange();
                                }
                            });
                        }

                        @Override
                        public void onArgs(JSONObject cliArgs) { }

                        @Override
                        public void onConsoleMsg(ConsoleMessage msg) { }

                        @Override
                        public void onData(String data) { }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(activity, context.getString(R.string.settings_save_error),
                    Toast.LENGTH_SHORT).show();
            enableViews(true);
        }
    }
    private void scanFrequencyRange(){
        jsInterface.getScannedValues(hostAddr, String.valueOf(currentSensitivity),
                new JSInterface.JSOutputInterface() {
            private void changeProgress(){
                numRead++;
                if(numRead < maxRead)
                    txvScanPerc.setText("[%" + String.valueOf(
                            ((numRead * 100) / maxRead)) + "]");
                else
                    txvScanPerc.setText("[%100]");
            }
            private void setGraphImage(final String data){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (data.split("[|]").length != 3 && data.split("[|]")[0].isEmpty()) {
                                throw new Exception(context.getString(R.string.graph_error));
                            } else {
                                fftBitmap = new ImageBase64().getImage(data.split("[|]")[0]);
                                if(fftBitmap == null)
                                    throw new Exception(context.getString(R.string.graph_error));
                                imgFreqScan.setImageBitmap(Bitmap.createScaledBitmap(
                                        fftBitmap,
                                        fftBitmap.getWidth() * 2,
                                        fftBitmap.getHeight() * 2, false));
                                if (!viewsHidden) {
                                    hideViews(true);
                                    btnStartScan.setEnabled(true);
                                }
                                changeProgress();
                                if (centerFreq < maxFreq) {
                                    onDataReceived(data.split("[|]")[1].trim().split(" "),
                                            data.split("[|]")[2].trim().split(" "));
                                    setDevFrequency(centerFreq + stepSize);
                                } else {
                                    hideViews(false);
                                    enableViews(true);
                                    if(showGraph){
                                        showSelectedFreqGraph();
                                        showGraph = false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(activity, context.getString(R.string.graph_error),
                                    Toast.LENGTH_SHORT).show();
                            hideViews(false);
                            enableViews(true);
                        }
                    }
                });
            }
            private void onDataReceived(final String[] freqs, final String[] dbs){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for(String freq:freqs){
                            String freqVal = String.format(Locale.US,
                                    "%.1f", Double.parseDouble(freq));
                            if(!freqRes.contains(freqVal)){
                                freqRes.add(freqVal);
                            }
                        }
                        for(String db:dbs){
                            String dbVal = String.format(Locale.US,
                                    "%.2f", Double.parseDouble(db));
                            if(!dbRes.contains(dbVal)){
                                dbRes.add(dbVal);
                            }
                        }
                        arrayAdapterRes.notifyDataSetChanged();
                    }
                });
            }
            @Override
            public void onInfo(JSONObject clientInfo) { }

            @Override
            public void onArgs(JSONObject cliArgs) { }

            @Override
            public void onConsoleMsg(ConsoleMessage msg) { }

            @Override
            public void onData(String data) {
                setGraphImage(data);
            }
        });
    }
}
