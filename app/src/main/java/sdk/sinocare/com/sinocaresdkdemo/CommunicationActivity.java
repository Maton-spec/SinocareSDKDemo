package sdk.sinocare.com.sinocaresdkdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.lidroid.xutils.util.LogUtils;
import com.sinocare.Impl.SC_BlueToothCallBack;
import com.sinocare.Impl.SC_BlueToothSearchCallBack;
import com.sinocare.Impl.SC_CmdCallBack;
import com.sinocare.Impl.SC_CurrentDataCallBack;
import com.sinocare.Impl.SC_DataCallBack;
import com.sinocare.Impl.SC_TimeSetCmdCallBack;
import com.sinocare.domain.BloodSugarData;
import com.sinocare.domain.BlueToothInfo;
import com.sinocare.handler.SN_MainHandler;
import com.sinocare.status.SC_DataStatusUpdate;
import com.sinocare.status.SC_ErrorStatus;
import com.sinocare.utils.LogUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class CommunicationActivity extends Activity{

	public static final int REFRESH = 1001;
	private ListView mListView;
	private ArrayList<deviceListItem>list;
	private MsgListAdapter mAdapter;
	private Context mContext = null;
	private SN_MainHandler Sn_MainHandler = null;
	private Button searchButton, upgradeButton, commandButton, disconnectButton, clearButton;
	private PopupWindow popupWindow;
	private int screenWidth;
	private int screenHeight;
	private BluetoothDevice device;

	//private BluetoothAdapter mBtAdapter = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hud_main);

		initActivity();
		registerReceiver(mBtReceiver, makeIntentFilter());

	}

	private void initActivity() {
		mContext = this;
		Intent intent = getIntent();
		if(intent!=null) {
			Bundle bundle= intent.getExtras();
			if(bundle!=null)
				device =bundle.getParcelable("device");
		}

		list = new ArrayList<deviceListItem>();
		mAdapter = new MsgListAdapter(this, list);
		mListView = (ListView) findViewById(R.id.list);
		mListView.setAdapter(mAdapter);
		//mListView.setOnItemClickListener(this);
		mListView.setFastScrollEnabled(true);

		//Sn_MainHandler.open(mContext, mBtAdapter,this);
		Sn_MainHandler = SN_MainHandler.getBlueToothInstance();

		Sn_MainHandler.connectBlueTooth(device, new SC_BlueToothCallBack() {

			@Override
			public void onConnectFeedBack(int result) {
				// TODO Auto-generated method stub

			}

		});

		Sn_MainHandler.registerReceiveBloodSugarData(new SC_CurrentDataCallBack<BloodSugarData>() {

			@Override
			public void onStatusChange(int status) {
				// TODO Auto-generated method stub
				if(status==SC_DataStatusUpdate.SC_BLOOD_FFLASH)
					list.add(new deviceListItem("请插入试条测试！", false));
				else if(status==SC_DataStatusUpdate.SC_MC_TESTING)
					list.add(new deviceListItem("正在测试，请稍后！", false));
				else if(status==SC_DataStatusUpdate.SC_MC_SHUTTINGDOWN)
					list.add(new deviceListItem("正在关机！", false));
				else if(status==SC_DataStatusUpdate.SC_MC_SHUTDOWN)
					list.add(new deviceListItem("已关机！", false));

				loadHandler.sendEmptyMessage(REFRESH);
			}

			@Override
			public void onReceiveSyncData(BloodSugarData datas) {

			}

			@Override
			public void onReceiveSucess(BloodSugarData datas) {
				// TODO Auto-generated method stub
				float v =datas.getBloodSugarValue();
				Date date = datas.getCreatTime();
				float t = datas.getTemperature();
				list.add(new deviceListItem("测试结果："+v+"mmol/l,"+"时间："
						+date.toLocaleString()+"当前温度："+t+"°", false));
				loadHandler.sendEmptyMessage(REFRESH);
			}
		});

		commandButton = (Button) findViewById(R.id.bt_command);
		commandButton.setOnClickListener(commandButtonClickListener);
		disconnectButton = (Button) findViewById(R.id.bt_disconnect);
		disconnectButton.setOnClickListener(disconnectButtonClickListener);
		clearButton = (Button) findViewById(R.id.bt_clear);
		clearButton.setOnClickListener(clearButtonClickListener);

		if(!Sn_MainHandler.isConnected()) {
			setActivityInIdleState();
		} else {
			setActivityInConnectedState();
		}

	}

	//未连接状态界面刷新
	private void setActivityInIdleState() {
		commandButton.setVisibility(View.GONE);
		//searchButton.setVisibility(View.VISIBLE);
		disconnectButton.setVisibility(View.GONE);
		clearButton.setVisibility(View.GONE);
		if(Sn_MainHandler.isSearching()) {
			//searchButton.setText("停止搜索");
		} else {
			//searchButton.setText("搜索/连接");
		}
	}

	//连接状态界面刷新
	private void setActivityInConnectedState() {
		//searchButton.setVisibility(View.GONE);
		commandButton.setVisibility(View.VISIBLE);
		disconnectButton.setVisibility(View.VISIBLE);
		clearButton.setVisibility(View.VISIBLE);
	}

	private OnClickListener disconnectButtonClickListener = new OnClickListener() {
		@SuppressWarnings("static-access")
		@Override
		public void onClick(View arg0) {
			Sn_MainHandler.disconnectDevice();
			//setActivityInIdleState();
			finish();

		}
	};

	private OnClickListener clearButtonClickListener = new OnClickListener() {
		@SuppressWarnings("static-access")
		@Override
		public void onClick(View arg0) {
			list.clear();
			loadHandler.sendEmptyMessage(REFRESH);
		}
	};

	private OnClickListener commandButtonClickListener = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if (!Sn_MainHandler.isConnected()) {
				//Hud_Display.toast("设备未连接，请先建立连接再发送命令！");
			} else {
				getPopupWindow();
				popupWindow.showAtLocation(commandButton, Gravity.CENTER, 0, 0);
			}
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		// 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
		if (!Sn_MainHandler.isBlueToothEnable()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, 3);
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//作为SDK时销毁Activity不关闭Handler，保持蓝牙通信不断开
		//导航软件请在主进程退出时调用close()方法关闭蓝牙通信模块
		//Sn_MainHandler.close();
		Sn_MainHandler.disconnectDevice();
		unregisterReceiver(mBtReceiver);
	}


	//创建PopupWindow
	@SuppressLint("InflateParams")
	@SuppressWarnings("deprecation")
	protected void initPopuptWindow() {
		// TODO Auto-generated method stub
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenWidth =dm.widthPixels;
		screenHeight =dm.heightPixels;

		View popupWindow_view = getLayoutInflater().inflate(R.layout.hud_cmd_dialog, null,false);

		popupWindow = new PopupWindow(popupWindow_view, screenWidth/2, screenHeight/2, true);

		popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

		popupWindow.update();
		popupWindow.setTouchable(true);
		// 设置允许在外点击消失
		popupWindow.setOutsideTouchable(true);
		popupWindow.setBackgroundDrawable(new BitmapDrawable());

		// Button connectButton = (Button)popupWindow_view.findViewById(R.id.btn_commad_1);
		Button currentButton = (Button)popupWindow_view.findViewById(R.id.btn_commad_2);
		Button historyButton = (Button)popupWindow_view.findViewById(R.id.btn_commad_3);
		// Button timeSettingButton = (Button)popupWindow_view.findViewById(R.id.btn_commad_4);
		// Button getIDButton = (Button)popupWindow_view.findViewById(R.id.btn_commad_5);
		Button clearDatasButton = (Button)popupWindow_view.findViewById(R.id.btn_commad_6);
		// Button modifyCorrectButton = (Button)popupWindow_view.findViewById(R.id.btn_commad_7);
		Button shutdowmButton = (Button)popupWindow_view.findViewById(R.id.btn_commad_8);

		Button firstRecordButton = (Button)popupWindow_view.findViewById(R.id.btn_commad_9);
		Button setTime = (Button)popupWindow_view.findViewById(R.id.btn_commad_10);
		Button allRecordsButton = (Button)popupWindow_view.findViewById(R.id.btn_commad_11);
		View layMenu = popupWindow_view.findViewById(R.id.cmd_dialog);
		layMenu.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK)
					popupWindow.dismiss();
				return false;
			}

		});
/*        connectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
			try {
				SCDataBaseUtils.getDbUtils().dropDb();
			} catch (DbException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
		});*/

		firstRecordButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Sn_MainHandler.requestFirstRecord();
			}
		});

		setTime.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {

				Calendar now = Calendar.getInstance();
				Date date = now.getTime();
				LogUtil.log("setTime",date.toLocaleString());
				Sn_MainHandler.setMCTime(date, new SC_TimeSetCmdCallBack() {
					@Override
					public void onTimeSetCmdFeedback(Date date) {
						LogUtil.log("setTime feedback",date.toLocaleString());
					}
				});
			}
		});

		allRecordsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Sn_MainHandler.requestAllRecord();
			}
		});

		currentButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Sn_MainHandler.readCurrentTestData(new SC_CurrentDataCallBack<BloodSugarData>() {

					@Override
					public void onStatusChange(int status) {
						// TODO Auto-generated method stub
						if(status==SC_DataStatusUpdate.SC_BLOOD_FFLASH)
							list.add(new deviceListItem("请插入试条测试！", false));
						else if(status==SC_DataStatusUpdate.SC_MC_TESTING)
							list.add(new deviceListItem("正在测试，请稍后！", false));
						else if(status==SC_DataStatusUpdate.SC_MC_SHUTTINGDOWN)
							list.add(new deviceListItem("正在关机！", false));
						else if(status==SC_DataStatusUpdate.SC_MC_SHUTDOWN)
							list.add(new deviceListItem("已关机！", false));
						loadHandler.sendEmptyMessage(REFRESH);
					}

					@Override
					public void onReceiveSyncData(BloodSugarData datas) {

					}

					@Override
					public void onReceiveSucess(BloodSugarData datas) {
						// TODO Auto-generated method stub
						float v =datas.getBloodSugarValue();
						Date date = datas.getCreatTime();
						float t = datas.getTemperature();
						list.add(new deviceListItem("测试结果："+v+"mmol/l,"+"时间："
								+date.toLocaleString()+"当前温度："+t+"°", false));
						loadHandler.sendEmptyMessage(REFRESH);
					}
				});
			}
		});
		historyButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Sn_MainHandler.readHistoryDatas(new SC_DataCallBack<ArrayList<BloodSugarData>>() {

					@Override
					public void onStatusChange(int status) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onReceiveSucess(
							ArrayList<BloodSugarData> datas,
							int currentPackage, int totalPackages) {
						// TODO Auto-generated method stub

						if(currentPackage==1)
							list.add(new deviceListItem("历史结果：", false));

						if(datas.size()==0)
							list.add(new deviceListItem("无历史数据！", false));

						for(int i=0;i<datas.size();i++) {
							BloodSugarData data = datas.get(i);
							float v =data.getBloodSugarValue();
							Date date = data.getCreatTime();
							//float t = data.getTemperature();
							list.add(new deviceListItem("时间："+date.toLocaleString()+","+v+"mmol/l", false));
						}
						loadHandler.sendEmptyMessage(REFRESH);
					}
				});
			}
		});
        /*timeSettingButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				//SN_MainHandler.setMCTime(date);
				SCDataBaseUtils.findAllDatas();

				//SCDataBaseUtils.test();
			}
		});
        getIDButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				SCDataBaseUtils.deleteAll();
			}
		});*/

		clearDatasButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Sn_MainHandler.clearHistory(new SC_CmdCallBack() {

					@Override
					public void onCmdFeedback(int result) {
						// TODO Auto-generated method stub
						if(result==1)
							list.add(new deviceListItem("清除历史成功！", false));
						else
							list.add(new deviceListItem("清除失败！", false));
						loadHandler.sendEmptyMessage(REFRESH);
					}
				});
			}
		});
/*        modifyCorrectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Sn_MainHandler.modifyCode((byte) 203);
			}
		});*/
		shutdowmButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				//Sn_MainHandler.shutDownMC();
			}
		});



	}

	//获取PopupWindow实例
	private void getPopupWindow() {

		if(null != popupWindow) {
			popupWindow.dismiss();
			return;
		}else {
			initPopuptWindow();
		}
	}

	public class deviceListItem {
		String message;
		boolean isSiri;

		public deviceListItem(String msg, boolean siri) {
			message = msg;
			isSiri = siri;
		}
	}

	//广播监听SDK ACTION
	private final BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if(SN_MainHandler.ACTION_SN_CONNECTION_STATE_CHANGED.equals(action)) {
				//D.log("ACTION_HUD_CONNECTION_STATE_CHANGED");
				if(Sn_MainHandler.isConnected()) {
					setActivityInConnectedState();
				} else if(Sn_MainHandler.isIdleState()||Sn_MainHandler.isDisconnecting()) {
					setActivityInIdleState();
				} else if(Sn_MainHandler.isSearching()) {
				}
			}else if(SN_MainHandler.ACTION_SN_ERROR_STATE.equals(action)) {
				Bundle bundle =intent.getExtras();
				int errorStatus = bundle.getInt(SN_MainHandler.EXTRA_ERROR_STATUS);
				if(errorStatus==SC_ErrorStatus.SC_OVER_RANGED_TEMPERATURE)
					list.add(new deviceListItem("错误：超过仪器测试温度范围！", false));
				else if(errorStatus==SC_ErrorStatus.SC_AUTH_ERROR)
					list.add(new deviceListItem("错误：认证失败！", false));
				else if(errorStatus==SC_ErrorStatus.SC_ERROR_OPERATE)
					list.add(new deviceListItem("错误操作！！", false));
				else if(errorStatus==SC_ErrorStatus.SC_ABLOVE_MAX_VALUE)
					list.add(new deviceListItem("错误：测试结果大于33.3mmol/L！", false));
				else if(errorStatus==SC_ErrorStatus.SC_BELOW_LEAST_VALUE)
					list.add(new deviceListItem("错误：测试结果小于1.1mmol/L！", false));
				else if(errorStatus==SC_ErrorStatus.SC_LOW_POWER)
					list.add(new deviceListItem("错误：电力不足！", false));
				else if(errorStatus==SC_ErrorStatus.SC_UNDEFINED_ERROR)
					list.add(new deviceListItem("未知错误！", false));
				else if(errorStatus==6)
					list.add(new deviceListItem("E-6", false));
				loadHandler.sendEmptyMessage(REFRESH);
			}else if(SN_MainHandler.ACTION_SN_MC_STATE.equals(action)) {
				Bundle bundle =intent.getExtras();
				int MCStatus = bundle.getInt(SN_MainHandler.EXTRA_MC_STATUS);
			}

		}
	};

	private IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(SN_MainHandler.ACTION_SN_CONNECTION_STATE_CHANGED);
		intentFilter.addAction(SN_MainHandler.ACTION_SN_ERROR_STATE);
		intentFilter.addAction(SN_MainHandler.ACTION_SN_MC_STATE);
		return intentFilter;
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		if(Sn_MainHandler.isConnected())
			Sn_MainHandler.disconnectDevice();
	}
	//主线程中的handler
	private Handler loadHandler = new Handler()
	{
		/**
		 * 接受子线程传递的消息机制
		 */
		@Override
		public void handleMessage(Message msg)
		{
			super.handleMessage(msg);
			int what = msg.what;

			switch (what)
			{
				case REFRESH:
				{


					mAdapter.notifyDataSetChanged();
					mListView.setSelection(list.size());

					break;
				}

			}
		}

	} ;
}
