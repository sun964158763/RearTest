package com.jsbd.mcutest;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;

public class MainActivity extends Activity implements OnCheckedChangeListener{

	private static String TAG="MainActivity";
	private EditText edit_text;
	private Button button1;
	private Switch switch1;
	private Switch switch2;
	private Switch switch3;
	
	private byte status=0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		edit_text = (EditText) findViewById(R.id.edit_text);
		button1 = (Button) findViewById(R.id.button1);
		switch1 = (Switch) findViewById(R.id.switch1);
		switch2 = (Switch) findViewById(R.id.switch2);
		switch3 = (Switch) findViewById(R.id.switch3);
		
		
		button1.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {
					String imputstr = edit_text.getText().toString();
					if(imputstr!=null && imputstr.length()>0){
						imputstr = imputstr.toUpperCase();
						String [] strs = imputstr.split(" ");
						byte [] intlist = new byte[strs.length];
						for(int i=0;i<intlist.length;i++){
							int intvalue = Integer.parseInt(strs[i].trim(), 16);
							intlist[i] = (byte) intvalue;						
						}
						sendData(intlist.length, intlist); 
					}
				} catch (Exception e) {
					// TODO: handle exception
					Log.d(TAG, Log.getStackTraceString(e));
				}
				
				
			}
		});
		switch1.setOnCheckedChangeListener(this);
		switch2.setOnCheckedChangeListener(this);
		switch3.setOnCheckedChangeListener(this);
		
		IntentFilter filter = new IntentFilter();
		// filter.addAction(MODE_ACTION);
		filter.addAction("com.jsbd.serial.mcutoapp");
		registerReceiver(mReceiver, filter);
		
		sendData(4, (byte) 0x06,(byte) 0x00 ,(byte) 0xFF ,(byte)(0x00));
	}

	BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			byte[] data = intent.getByteArrayExtra("Data");
			onSerialData(data);
		}
	};
	
	public void onSerialData(byte[] data) {
		StringBuilder stringBuilder = new StringBuilder("");  
		for (int i = 0; i < data.length; i++) {
			int v = data[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
			stringBuilder.append(" ");
		}
		Log.d(TAG, "onSerialData stringBuilder:"+stringBuilder.toString());

		if (data[3] == 0x06 && data[5] == 0xFF) {
			int bit_0 = data[6]&0xFE;
			int bit_1 = data[6]&0xFD;
			int bit_2 = data[6]&0xFB;
			
			if (bit_0 == 1) {
				switch1.setSelected(true);
			} else {
				switch1.setSelected(false);
			}
			if (bit_1 == 1) {
				switch2.setSelected(true);
			} else {
				switch2.setSelected(false);
			}
			if (bit_2 == 1) {
				switch3.setSelected(true);
			} else {
				switch3.setSelected(false);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
		// TODO Auto-generated method stub
		if(arg0 == switch1){
			Log.d(TAG, "onCheckedChanged arg1:"+arg1);
			if(arg1){
				status = (byte) (status|0x01);
				sendData(4, (byte) 0x06,(byte) 0x00 ,(byte) 0xFF ,(byte)(status));
			}else{
				status = (byte) (status&0xFE);
				sendData(4, (byte) 0x06,(byte) 0x00 ,(byte) 0xFF ,(byte)(status));
			}
		}else if(arg0 == switch2){
			if(arg1){
				status = (byte) (status|0x02);
				sendData(4, (byte) 0x06,(byte) 0x00 ,(byte) 0xFF ,(byte)(status));
			}else{
				status = (byte) (status&0xFD);
				sendData(4, (byte) 0x06,(byte) 0x00 ,(byte) 0xFF ,(byte)(status));
			}
		}else if(arg0 == switch3){
			if(arg1){
				status = (byte) (status|0x04);
				sendData(4, (byte) 0x06,(byte) 0x00 ,(byte) 0xFF ,(byte)(status));
			}else{
				status = (byte) (status&0xFB);
				sendData(4, (byte) 0x06,(byte) 0x00 ,(byte) 0xFF ,(byte)(status));
			}
		}
		
	}
	
	
	
	
	
	
	
	
	
	
	// App向MCU发送消息，length为串口协议中的长度，arg部分不需要传入0xFF 0x66 长度及校验和
	public void sendData(int length, byte... arg) {
		try {
			byte[] data = new byte[length + 4];
			data[0] = (byte) 0xff;
			data[1] = 0x66;
			data[2] = (byte) length;
			for (int i = 0; i < length; i++) {
				data[i + 3] = arg[i];
			}
			data[length + 3] = getCmdSum(data);
			sendMcuData(data);
		} catch (Exception e) {
			// TODO: handle exception
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}

	// 包括头及校验和的完整串口数据发送给MCU
	public void sendMcuData(byte[] data) {
		Intent intent = new Intent("com.jsbd.serial.apptomcu");
		intent.putExtra("DataLen", data.length);
		intent.putExtra("Data", data);
		// Log.e(tag, "发送MCU数据");
		String datas = "McuData = ";
		for (int i = 0; i < data.length; i++) {
			datas = datas + " " + data[i];
		}
		// Log.e(tag, datas);
		sendBroadcast(intent);
	}

	// 计算串口数据的校验和
	public byte getCmdSum(byte[] data) {
		int bLen = data[2];
		byte bCheckSum = 0;
		for (int i = 0; i <= bLen; i++) {
			bCheckSum += data[2 + i];
		}
		bCheckSum = (byte) (~bCheckSum + 1);
		// Log.e(tag, "校验和 = "+bCheckSum);
		return bCheckSum;
	}
}
