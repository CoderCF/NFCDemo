package com.codercf.nfcdemo;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // NFC适配器
    private NfcAdapter mNfcAdapter;
    // 传达意图
    private PendingIntent mPendingIntent;
    // 文本控件
    private TextView mTvContent;
    private EditText mEtContent;
    private String mTagText;
    private Tag mTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 控件的绑定
        mTvContent = (TextView) findViewById(R.id.promt);
        mEtContent = (EditText) findViewById(R.id.et_content);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            mTvContent.setText("设备不支持NFC！");
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            mTvContent.setText("请在系统设置中先启用NFC功能！");
            return;
        }

        //一旦截获NFC消息，就会通过PendingIntent调用窗口
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //设置处理优于所有其他NFC的处理
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            // 停止监听NFC设备是否连接
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 当前app正在前端界面运行，这个时候有intent发送过来，那么系统就会调用onNewIntent回调方法，将intent传送过来
        // 我们只需要在这里检验这个intent是否是NFC相关的intent，如果是，就调用处理方法
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            //1.获取Tag对象
            mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            //2.获取Ndef的实例
            Ndef ndef = Ndef.get(mTag);
            mTagText = ndef.getType() + "\nmaxsize:" + ndef.getMaxSize() + "bytes\n\n";
//            readNfcTag(intent);
            mTvContent.setText(mTagText);
        }
    }

    public void read(View view){
        if (mTag != null) {
            try {
                //解析Tag获取到NDEF实例
                Ndef ndef = Ndef.get(mTag);
                //打开连接
                ndef.connect();
                //获取NDEF消息
                NdefMessage message = ndef.getNdefMessage();
                String content = NfcUtils.parseTextRecord(message.getRecords()[0]);
                mTvContent.setText(mTvContent.getText().toString()+ "nfc标签内容：\n" + content+ "\n");
                //关闭连接
                ndef.close();
            } catch (Exception e) {
                Toast.makeText(this, "读取nfc异常", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "设备与nfc卡连接断开，请重新连接...", Toast.LENGTH_SHORT).show();
        }
    }
    public void write(View view){
        String s = mEtContent.getText().toString().trim();
        if("".equals(s)){
            Toast.makeText(this, "请输入写入标签内容", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mTag != null) {
            //新建NdefRecord数组，本例中数组只有一个元素
            NdefRecord[] records = { NfcUtils.createNdefRecord(mEtContent.getText().toString().trim()) };
            //新建一个NdefMessage实例
            NdefMessage ndefMessage = new NdefMessage(records);
            //开始向标签写入文本
            if (NfcUtils.writeNfcTag(this,ndefMessage, mTag)) {
                //如果成功写入文本，将mtext设为null
                mTagText = null;
                //将主窗口显示的要写入的文本清空，文本只能写入一次
                //如要继续写入，需要再次指定新的文本，否则只会读取标签中的文本
                mTvContent.setText("");
            }
        } else {
            Toast.makeText(this, "设备与nfc卡连接断开，请重新连接...", Toast.LENGTH_SHORT).show();
        }
    }
    public void delete(View view){
        try {
            delete(mTag);
        } catch (Exception e) {
            Toast.makeText(this, "删除nfc异常", Toast.LENGTH_SHORT).show();
        }
    }

    // 删除方法
    private void delete(Tag tag) throws IOException, FormatException {
        if (tag != null) {
            //新建一个里面无任何信息的NdefRecord实例
            NdefRecord nullNdefRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT, new byte[] {}, new byte[] {});
            NdefRecord[] records = { nullNdefRecord };
            NdefMessage message = new NdefMessage(records);
            // 解析TAG获取到NDEF实例
            Ndef ndef = Ndef.get(tag);
            // 打开连接
            ndef.connect();
            // 写入信息
            ndef.writeNdefMessage(message);
            // 关闭连接
            ndef.close();
            Toast.makeText(this, "删除数据成功！", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "设备与nfc卡连接断开，请重新连接...", Toast.LENGTH_SHORT).show();
        }
    }

}
