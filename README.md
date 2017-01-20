# 1. SinocareSDK说明
SinocareSDK 是三诺生物传感股份有限公司的蓝牙血糖仪连接的SDK。

## 1.1 文件说明

SinocareSDK 主要是通过lib sn_care_sdk.jar方式提供给第三发。

## 1.2 手机设备的Android系统版本和蓝牙版本要求
安稳+air版，需要SinoSDK 支持android 4.3及以上操作系统，支持蓝牙4.0，支持ble
蓝牙WL-1血糖仪（微信版）,需要SinoSDK 支持android 4.0及以上操作系统，支持蓝牙3.0
蓝牙WL-1血糖仪（直连版），需要SinoSDK 支持android 4.0及以上操作系统，支持蓝牙3.0

# 2. 集成方法
## 2.1  获得AccessKey和SecretKey
由Sinocare 提供，用于权限管理。

## 2.2 SDK接入
Libs添加蓝牙SDK jar包（sn_care_sdk.jar） ，添加编译依赖compile files('libs/sn_care_sdk.jar')

## 2.3 配置manifest
manifest的配置主要包括添加权限,代码示例如下：
    <manifest……>
    <uses-feature
        android:name="android.hardware.bluetooth_le"
        android:required="true" />

    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
   	<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/> 
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

## 2.4 权限及用途
ACCESS_NETWORK_STATE(必须)  用于检测联网方式，区分用户设备使用的是2G、3G或是WiFi
READ_PHONE_STATE(必须)   用于获取用户设备的IMEI，通过IMEI和mac来唯一的标识用户。
BLUETOOTH (必须)   用于蓝牙和设备通信所需。
BLUETOOTH_ADMIN (必须)   用于蓝牙和设备通信所需。
WRITE_EXTERNAL_STORAGE (必须)   用于底层数据缓存
ACCESS_NETWORK_STATE(必须)  用于允许应用程序联网，以便向我们的服务器端发送数据。
INTERNET(须)  用于允许应用程序联网，以便向我们的服务器端发送数据。
ACCESS_FINE_LOCATION (必须)    用于允许应用程序访问设备位置。
ACCESS_COARSE_LOCATION (必须)    用于允许应用程序访问设备位置。

## 2.5 填写服务和key
 填写服务，填写AccessKey和填写SecretKey;

    <service
       android:name="com.sinocare.bluetoothle.SN_BluetoothLeService"
       android:enabled="true" >
     <meta-data android:name="AccessKey" android:value="888"></meta-data>
    <meta-data android:name="SecretKey" android:value="888"></meta-data>
     </service>

其中AccessKey 和 SecretKey 为SDK权限访问相关的Key
服务为蓝牙相关的服务，包名固定为com.sinocare.bluetoothle.SN_BluetoothLeService 

## 2.6 接入场景
SDK 接入流程如下

### 2.6.1 鉴权过程
![](https://github.com/sinocare/SinocareSDKDemo/blob/master/uml/1.png)

### 2.6.2 建立连接及通信
![](https://github.com/sinocare/SinocareSDKDemo/blob/master/uml/2.png)
![](https://github.com/sinocare/SinocareSDKDemo/blob/master/uml/22.png)

### 2.6.3 状态返回和数据发送
![](https://github.com/sinocare/SinocareSDKDemo/blob/master/uml/3.png)


# 3.接口说明

## 3.1 初始化SDK
    initSDK(context, ProtocolVersion.WL_1,false);//不使用4.0
    // initSDK(context, ProtocolVersion.WL_1);//双模 手机支持BLE 使用BLE方式
    context 为当前activity的上下文;

## 3.2 搜索设备
    searchBlueToothDevice(SC_BlueToothSearchCallBack<BlueToothInfo> device)
         SC_BlueToothSearchCallBack<BlueToothInfo> device 为异步返回类
    实现函数 public void onBlueToothSeaching(BlueToothInfo newDevice) 
    返回搜索到的蓝牙设备信息 详细请查询API doc

## 3.3 连接
    场景：搜索到设备后，选择需要连接的设备；
     connectBlueTooth(BluetoothDevice device, SC_BlueToothCallBack callback) 
       连接蓝牙设备
    注意 :连接的时候需要提供需要连接的蓝牙设备

## 3.4 读当前测试数据
    readCurrentTestData(SC_CurrentDataCallBack<BloodSugarData> currentTestValue) 
     读当前数据(只要在插试条测试后才有效)
    SC_CurrentDataCallBack 提供了接收数据和状态的接口；详细请参照doc 

## 3.5 注册监测血糖值
    registerReceiveBloodSugarData(SC_CurrentDataCallBack<BloodSugarData> currentTestValue)
    注册监听后，当设备状态或者有测试血糖值可监听，并返回数据和状态；

## 3.6 读历史数据
    readHistoryDatas(SC_DataCallBack<java.util.ArrayList<BloodSugarData>> list) 
    读取历史数据(测试状态下无效)
    读取设备存储的历史数据（最高存储200条） 历史数据以包的形式发送，当收到数据后，
    通过onReceiveSucess(T datas, int currentPackage, int totalPackages)

    数据返回接口 currentPackage 为当前包，totalPackages为总包，每包的数据量通过
    datas.size 获取。
## 3.7 广播监听状态变化
    //广播监听SDK ACTION
	private final BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if(SN_MainHandler.ACTION_SN_CONNECTION_STATE_CHANGED.equals(action)) {
				//蓝牙连接状态变化
			}else if(SN_MainHandler.ACTION_SN_ERROR_STATE.equals(action)) {
				//错误状态变化反馈
			}else if(SN_MainHandler.ACTION_SN_MC_STATE.equals(action)) {
				//机器状态的状态变化反馈
			}
		
		}
	};

# 4. 血糖仪的错误码和状态码
## 4.1 与Sinocare设备通讯状态的枚举
	public final static int SC_BLOOD_FFLASH = 0x01;//滴血闪烁(提示请插入试条测试)
	public final static int SC_MC_TESTING = 0x02;//仪器正在测试
	public final static int SC_MC_SEND_SUCCESS= 0x03;
	public final static int SC_MC_SHUTTINGDOWN = 0x04;//仪器正在关闭
	public final static int SC_MC_SHUTDOWN = 0x05;//仪器已关机

## 4.2 蓝牙状态发生改变的枚举
	public final static int SC_MC_BLUETOOTH_CONNECTING = 0x06;//蓝牙正在连接中
	public final static int SC_MC_BLUETOOTH_CONNECT_FAILURE = 0x07;//蓝牙连接失败
	public final static int SC_MC_BLUETOOTH_DISCONNECTED = 0x08;//蓝牙连接断开连接

## 4.3 SinocareSDK错误码
	public final static int  SC_LOW_POWER = 0x01;//E-1 电力不足
	public final static int  SC_OVER_RANGED_TEMPERATURE = 0x02;//E-2 超过仪器测试温度范围
	public final static int  SC_ERROR_OPERATE = 0x03;//E-3 错误操作
	public final static int  SC_ERROR_FACTORY = 0x06;//E-6 出厂设置错误
	public final static int SC_ABLOVE_MAX_VALUE = 0x11;//HI 高于33.3 mmol/L
	public final static int SC_BELOW_LEAST_VALUE = 0x12;//LO 低于1.1
	public final static int SC_AUTH_ERROR = 0x10;//鉴权失败
	public final static int SC_BLUETOOTH_PAIR_ERROR = 0x11;//配对失败
	public final static int SC_UNDEFINED_ERROR = 0xFF;//未知错误

# 5 常见问题  
     1、问题：认证不通过
        出现问题分析： AccessKey 和 SecretKey 设置不正确。或者 当前无网络；
     2、问题: 连接不成功
        问题分析：initSDK(context, ProtocolVersion.WL_1,false);初始化 设置协议不正确
     3、问题: 蓝牙搜索到设备
        问题分析 ：蓝牙未打开
     4、其他问题：
       请联系三诺工程师；或者私信；
