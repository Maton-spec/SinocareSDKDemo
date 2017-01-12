package sdk.sinocare.com.sinocaresdkdemo;

import android.app.Application;

import com.lidroid.xutils.util.LogcatHelper;
import com.sinocare.bluetoothle.SN_BluetoothLeConnection;

public class BleApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		SN_BluetoothLeConnection BleConnection = SN_BluetoothLeConnection.getBlueToothBleConnection(this);
		BleConnection.initBleApplicationService();
		LogcatHelper.getInstance(this).start();
	}
	
}
