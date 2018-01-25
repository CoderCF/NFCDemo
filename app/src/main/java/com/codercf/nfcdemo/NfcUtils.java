package com.codercf.nfcdemo;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

/**
 * Created by JUNWEN on 2017/10/18.
 */

public class NfcUtils {

    /**
     * 读取NFC标签文本数据
     * @param intent
     * @return
     */
    public static String readNfcTag(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            //从标签读取数据（Parcelable对象）
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msgs[] = null;
            if (rawMsgs != null) {
                //标签可能存储了多个NdefMessage对象，一般情况下只有一个NdefMessage对象
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    //转换成NdefMessage对象
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            try {
                if (msgs != null) {
                    //程序中只考虑了1个NdefRecord对象，若是通用软件应该考虑所有的NdefRecord对象
                    NdefRecord record = msgs[0].getRecords()[0];
                    //将纯文本内容从NdefRecord对象（payload）中解析出来
                    return parseTextRecord(record);
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 解析NDEF文本数据，从第三个字节开始，后面的文本数据
     * @param ndefRecord
     * @return
     */
    public static String parseTextRecord(NdefRecord ndefRecord) {
        /**
         * 判断数据是否为NDEF格式
         *   1）TNF（类型名格式，Type Name Format）必须是NdefRecord.TNF_WELL_KNOWN。
             2）可变的长度类型必须是NdefRecord.RTD_TEXT。
            如果这两个标准同时满足，那么就为NDEF格式。
         */
        //判断TNF
        if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
            return null;
        }
        //判断可变的长度的类型
        if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
            return null;
        }
        try {
            //获得字节数组，然后进行分析
            byte[] payload = ndefRecord.getPayload();
            //下面开始NDEF文本数据第一个字节，状态字节
            //判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
            //其他位都是0，所以进行"位与"运算后就会保留最高位
            String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
            //3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
            int languageCodeLength = payload[0] & 0x3f;
            //下面开始NDEF文本数据第二个字节，语言编码
            //获得语言编码
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            //下面开始NDEF文本数据后面的字节，解析出文本
            String textRecord = new String(payload, languageCodeLength + 1,
                    payload.length - languageCodeLength - 1, textEncoding);
            return textRecord;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * 将NdefMessage对象写入标签
     *  @param context
     * @param message
     * @param tag
     * @return
     */
    public static boolean writeNfcTag(Context context, NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            //获取Ndef对象
            Ndef ndef = Ndef.get(tag);
            //判断是否为NDEF标签
            if (ndef != null) {
                //允许对标签进行IO操作
                ndef.connect();
                //判断是否支持可写
                if (!ndef.isWritable()) {
                    Toast.makeText(context, "NFC Tag是只读的！", Toast.LENGTH_SHORT).show();
                    return false;
                }
                //判断标签的容量是否够用
                if (ndef.getMaxSize() < size) {
                    Toast.makeText(context, "NFC Tag的空间不足！", Toast.LENGTH_SHORT).show();
                    return false;
                }
                //向标签写入数据
                ndef.writeNdefMessage(message);
                Toast.makeText(context, "已成功写入数据！", Toast.LENGTH_SHORT).show();
                return true;
            } else {//当我们买回来的NFC标签是没有格式化的，或者没有分区的执行此步
                //获取可以格式化和向标签写入数据NdefFormatable对象
                NdefFormatable format = NdefFormatable.get(tag);
                //向非NDEF格式或未格式化的标签写入NDEF格式数据
                //判断是否获得了NdefFormatable对象，有一些标签是只读的或者不允许格式化的
                if (format != null) {
                    try {
                        //允许对标签进行IO操作
                        format.connect();
                        //格式化并将信息写入标签
                        format.format(message);
                        Toast.makeText(context, "已成功写入数据！", Toast.LENGTH_SHORT).show();
                        return true;
                    } catch (Exception e) {
                        Toast.makeText(context, "写入NDEF格式数据失败！", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else {
                    Toast.makeText(context, "NFC标签不支持NDEF格式！", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 创建NDEF文本数据
     * @return
     */
    public static NdefRecord createNdefRecord(String text){
        //生成语言编码的字节数组，中文编码
        byte[] langBytes = Locale.CHINA.getLanguage().getBytes(Charset.forName("US-ASCII"));
        Charset utfEncoding = Charset.forName("UTF-8");
        //将文本转换为UTF-8格式
        byte[] textBytes = text.getBytes(utfEncoding);
        //设置状态字节编码最高位数为0
        int utfBit = 0;
        //定义状态字节
        char status = (char) (utfBit + langBytes.length);
        //创建存储payload的字节数组
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        //设置第一个状态字节，先将状态码转换成字节
        data[0] = (byte) status;
        //设置语言编码，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1到langBytes.length的位置
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        //设置文本字节，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1 + langBytes.length
        //到textBytes.length的位置
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        //通过字节传入NdefRecord对象
        //NdefRecord.RTD_TEXT：传入类型 读写
        NdefRecord ndefRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT, new byte[0], data);
        return ndefRecord;
    }
}
