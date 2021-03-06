package com.stats.chinastats;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import com.stats.chinastats.util.NetHelper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class UpdateManager {

	private String curVersion;
	private String newVersion;
	private int curVersionCode;
	private int newVersionCode;
	private String updateInfo;
	private UpdateCallback callback;
	private Context ctx;
    
	private int progress;  
	private Boolean hasNewVersion;
	private Boolean canceled;

	//��Ÿ���APK�ļ���·��
	public static final String UPDATE_DOWNURL = "http://doc.hb.stats.cn/ip/apk/sannongcensus.apk";
	//��Ÿ���APK�ļ���Ӧ�İ汾˵��·��
	public static final String UPDATE_CHECKURL = "http://doc.hb.stats.cn/ip/apk/sannongcensus-update.txt";
	public static final String UPDATE_APKNAME = "sanongcensus.apk";
	public static final String CLASSNAME= "com.stats.chinastats.LoadingActivity";
	//public static final String UPDATE_VERJSON = "ver.txt";
	public static final String UPDATE_SAVENAME = "农普助手.apk";
	private static final int UPDATE_CHECKCOMPLETED = 1;
	 private static final int UPDATE_DOWNLOADING = 2; 
	 private static final int UPDATE_DOWNLOAD_ERROR = 3; 
	 private static final int UPDATE_DOWNLOAD_COMPLETED = 4; 
	 private static final int UPDATE_DOWNLOAD_CANCELED = 5;

	 //�ӷ�����������apk����ļ���
    private String savefolder = getSdCardRootPath() + File.separator;
	public UpdateManager(Context context, UpdateCallback updateCallback) {
		ctx = context;
		callback = updateCallback;
		//savefolder = context.getFilesDir();
		canceled = false;
		getCurVersion();
	}
	
	public static String getSdCardRootPath() {
		return Environment.getExternalStorageDirectory().getAbsolutePath()
				.toString();
	}
	public String getNewVersionName()
	{
		return newVersion;
	}
	
	public String getUpdateInfo()
	{
		return updateInfo;
		
	}

	private void getCurVersion() {
		try {
			PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(
					ctx.getPackageName(), 0);
			curVersion = pInfo.versionName;
			curVersionCode = pInfo.versionCode;
		} catch (NameNotFoundException e) {
			Log.e("update", e.getMessage());
			curVersion = "1.1.1000";
			curVersionCode = 111000;
		}

	}

	public void checkUpdate() {		
		hasNewVersion = false;
		new Thread(){
			// ***************************************************************
			/**
			 * @by wainiwann add
			 * 
			 */
			@Override
			public void run() {
				Log.i("@@@@@", ">>>>>>>>>>>>>>>>>>>>>>>>>>>getServerVerCode() ");
				try {

					String verjson = NetHelper.httpStringGet(UPDATE_CHECKURL);
					Log.i("@@@@", verjson
							+ "**************************************************");
					JSONArray array = new JSONArray(verjson);

					if (array.length() > 0) {
						JSONObject obj = array.getJSONObject(0);
						try {
							newVersionCode = Integer.parseInt(obj.getString("verCode"));
							newVersion = obj.getString("verName");
							updateInfo = obj.getString("info");
							Log.i("newVerCode", newVersionCode
									+ "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
							Log.i("newVerName", newVersion
									+ "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
							if (newVersionCode > curVersionCode) {
								hasNewVersion = true;
							}
						} catch (Exception e) {
							newVersionCode = -1;
							newVersion = "";
							updateInfo = "";
							
						}
					}
				} catch (Exception e) {
					Log.e("update", e.getMessage());
				}
				updateHandler.sendEmptyMessage(UPDATE_CHECKCOMPLETED);
			};
			// ***************************************************************
		}.start();

	}

	public void update() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		
		intent.setDataAndType(
				Uri.fromFile(new File(savefolder, UPDATE_SAVENAME)),
				"application/vnd.android.package-archive");
		ctx.startActivity(intent);
		
		

	}

	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	public void downloadPackage() 
	{
		
		
		new Thread() {			
			 @Override  
		        public void run() {  
		            try {  
		                URL url = new URL(UPDATE_DOWNURL);  
		              
		                HttpURLConnection conn = (HttpURLConnection)url.openConnection();  
		                conn.connect();  
		                int length = conn.getContentLength();  
		                InputStream is = conn.getInputStream();  
		                  
		               
		                File ApkFile = new File(savefolder,UPDATE_SAVENAME);
		                
		                
		                if(ApkFile.exists())
		                {
		                	
		                	ApkFile.delete();
		                }
		                
		                
		                FileOutputStream fos = new FileOutputStream(ApkFile);  
		                 
		                int count = 0;  
		                byte buf[] = new byte[512];  
		                  
		                do{  
		                	
		                    int numread = is.read(buf);  
		                    count += numread;  
		                    progress =(int)(((float)count / length) * 100);  
		                   
		                    updateHandler.sendMessage(updateHandler.obtainMessage(UPDATE_DOWNLOADING)); 
		                    if(numread <= 0){      
		                        
		                    	updateHandler.sendEmptyMessage(UPDATE_DOWNLOAD_COMPLETED);
		                        break;  
		                    }  
		                    fos.write(buf,0,numread);  
		                }while(!canceled);  
		                if(canceled)
		                {
		                	updateHandler.sendEmptyMessage(UPDATE_DOWNLOAD_CANCELED);
		                }
		                fos.close();  
		                is.close();  
		            } catch (MalformedURLException e) {  
		                e.printStackTrace(); 
		                
		                updateHandler.sendMessage(updateHandler.obtainMessage(UPDATE_DOWNLOAD_ERROR,e.getMessage()));
		            } catch(IOException e){  
		                e.printStackTrace();  
		                
		                updateHandler.sendMessage(updateHandler.obtainMessage(UPDATE_DOWNLOAD_ERROR,e.getMessage()));
		            }  
		              
		        } 
		}.start();
	}

	public void cancelDownload()
	{
		canceled = true;
	}
	
	Handler updateHandler = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case UPDATE_CHECKCOMPLETED:
				
				callback.checkUpdateCompleted(hasNewVersion, newVersion+"\n"+updateInfo+"\n");
				break;
			case UPDATE_DOWNLOADING:
				
				callback.downloadProgressChanged(progress);
				break;
			case UPDATE_DOWNLOAD_ERROR:
				
				callback.downloadCompleted(false, msg.obj.toString());
				break;
			case UPDATE_DOWNLOAD_COMPLETED:
				
				callback.downloadCompleted(true, "");
				break;
			case UPDATE_DOWNLOAD_CANCELED:
				
				callback.downloadCanceled();
			default:
				break;
			}
		}
	};

	public interface UpdateCallback {
		public void checkUpdateCompleted(Boolean hasUpdate,
				CharSequence updateInfo);

		public void downloadProgressChanged(int progress);
		public void downloadCanceled();
		public void downloadCompleted(Boolean sucess, CharSequence errorMsg);
	}

}
