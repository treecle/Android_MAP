package treecle.map;


import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity0 extends AppCompatActivity
{
	private final int ROTATE = 90 ;
	private final int SAMPLE_SIZE = 4 ;

	//Permission variables
	static boolean ASWP_JSCRIPT    = SmartWebView.ASWP_JSCRIPT;
	static boolean ASWP_FUPLOAD    = SmartWebView.ASWP_FUPLOAD;
	static boolean ASWP_CAMUPLOAD  = SmartWebView.ASWP_CAMUPLOAD;
	static boolean ASWP_ONLYCAM		= SmartWebView.ASWP_ONLYCAM;
	static boolean ASWP_MULFILE    = SmartWebView.ASWP_MULFILE;
	static boolean ASWP_LOCATION   = SmartWebView.ASWP_LOCATION;
	static boolean ASWP_PULLFRESH	= SmartWebView.ASWP_PULLFRESH;
	static boolean ASWP_ZOOM       = SmartWebView.ASWP_ZOOM;
	static boolean ASWP_SFORM      = SmartWebView.ASWP_SFORM;
	static boolean ASWP_OFFLINE		= SmartWebView.ASWP_OFFLINE;
	static boolean ASWP_EXTURL		= SmartWebView.ASWP_EXTURL;

	//Security variables
	static boolean ASWP_CERT_VERIFICATION = SmartWebView.ASWP_CERT_VERIFICATION;

	//Configuration variables
	private static String ASWV_URL      = SmartWebView.ASWV_URL;
	private String CURR_URL				 = ASWV_URL;
	private static String ASWV_F_TYPE   = SmartWebView.ASWV_F_TYPE;

	public static String ASWV_HOST		= aswm_host(ASWV_URL);

	//Careful with these variable names if altering
	WebView asw_view;
	NotificationManager asw_notification;
	Notification asw_notification_new;

	private String asw_cam_message;
	private ValueCallback<Uri> asw_file_message;
	private ValueCallback<Uri[]> asw_file_path;
	private final static int asw_file_req = 1;

	private final static int loc_perm = 1;
	private final static int file_perm = 2;

	private SecureRandom random = new SecureRandom();

	private static final String TAG = MainActivity0.class.getSimpleName();
	private Camera camera;
	private android.R.attr lp;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (Build.VERSION.SDK_INT >= 21)
		{
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));
			Uri[] results = null;
			if (resultCode == Activity.RESULT_OK)
			{
				if (requestCode == asw_file_req)
				{
					if (null == asw_file_path)
					{
						return;
					}
					if (intent == null || intent.getData() == null)
					{
						if (asw_cam_message != null) {
							results = new Uri[]{Uri.parse(asw_cam_message)};
						}
					} else
					{
						String dataString = intent.getDataString();
						if (dataString != null) {
							results = new Uri[]{ Uri.parse(dataString) };
						} else
						{
							if(ASWP_MULFILE)
							{
								if (intent.getClipData() != null) {
									final int numSelectedFiles = intent.getClipData().getItemCount();
									results = new Uri[numSelectedFiles];
									for (int i = 0; i < numSelectedFiles; i++) {
										results[i] = intent.getClipData().getItemAt(i).getUri();
									}
								}
							}
						}
					}
				}
			}

			//이미지 촬영시 회전각도에 따라 회전시킨후 다시 저장하는 기능 - 190518 추가
			if(results != null) {
				checkRotate(results[0].getPath());

				Log.e("어디가 호출되나?", "1");
				Log.e("asw_cam_message", asw_cam_message);
				ResizeImages(asw_cam_message);


				asw_file_path.onReceiveValue(results);
				asw_file_path = null;
			}
		}
		else
		{
			if (requestCode == asw_file_req)
			{
				if (null == asw_file_message) return;
				Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();


				//이미지 촬영시 회전각도에 따라 회전시킨후 다시 저장하는 기능 - 190518 추가
				checkRotate(result.getPath());

				if(result != null) {
					asw_file_message.onReceiveValue(result);
					asw_file_message = null;
				}


			}
		}
	}

	@SuppressLint({"SetJavaScriptEnabled", "WrongViewCast"})
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

//		String sPath = "file:/storage/emulated/0/Pictures/file_2019_24_39_3349462145986765979.jpg" ;
//		Bitmap photo = BitmapFactory.decodeFile(sPath);
//		Log.e("TAG", "photo : " + photo) ;

		Log.w("READ_PERM = ",Manifest.permission.READ_EXTERNAL_STORAGE);
		Log.w("WRITE_PERM = ",Manifest.permission.WRITE_EXTERNAL_STORAGE);
		//Prevent the app from being started again when it is still alive in the background
		if (!isTaskRoot()) {
			finish();
			return;
		}

		setContentView(R.layout.activity_main);

		asw_view = findViewById(R.id.webView);

	/*	final SwipeRefreshLayout pullfresh = findViewById(R.id.pullfresh);
		if (ASWP_PULLFRESH) {
			pullfresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
				@Override
				public void onRefresh() {
					pull_fresh();
					pullfresh.setRefreshing(false);
				}
			});
			asw_view.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
				@Override
				public void onScrollChanged() {
					if (asw_view.getScrollY() == 0) {
						pullfresh.setEnabled(true);
					} else {
						pullfresh.setEnabled(false);
					}
				}
			});
		}else{
			pullfresh.setRefreshing(false);
			pullfresh.setEnabled(false);
		}*/

		//Getting basic device information
		get_info();

		//Getting GPS location of device if given permission
		if(ASWP_LOCATION && !check_permission(1)){
			ActivityCompat.requestPermissions(MainActivity0.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, loc_perm);
		}

		//Webview settings; defaults are customized for best performance
		WebSettings webSettings = asw_view.getSettings();

		if(!ASWP_OFFLINE){
			webSettings.setJavaScriptEnabled(ASWP_JSCRIPT);
		}
		webSettings.setSaveFormData(ASWP_SFORM);
		webSettings.setSupportZoom(ASWP_ZOOM);
		webSettings.setGeolocationEnabled(ASWP_LOCATION);
		webSettings.setAllowFileAccess(true);
		webSettings.setAllowFileAccessFromFileURLs(true);
		webSettings.setAllowUniversalAccessFromFileURLs(true);
		webSettings.setUseWideViewPort(true);
		webSettings.setDomStorageEnabled(true);

		webSettings.setAllowContentAccess(true);
		webSettings.setSupportMultipleWindows(false);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		webSettings.setAppCacheEnabled(true);

		asw_view.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return true;
			}
		});
		asw_view.setHapticFeedbackEnabled(false);

		asw_view.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {

				if(!check_permission(2)){
					ActivityCompat.requestPermissions(MainActivity0.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, file_perm);
				}else {
					DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

					request.setMimeType(mimeType);
					String cookies = CookieManager.getInstance().getCookie(url);
					request.addRequestHeader("cookie", cookies);
					request.addRequestHeader("User-Agent", userAgent);
					request.setDescription(getString(R.string.dl_downloading));
					request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
					request.allowScanningByMediaScanner();
					request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
					request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
					DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
					assert dm != null;
					dm.enqueue(request);
					Toast.makeText(getApplicationContext(), getString(R.string.dl_downloading2), Toast.LENGTH_LONG).show();
				}
			}
		});

		if (Build.VERSION.SDK_INT >= 21) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
			asw_view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
			webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
		} else if (Build.VERSION.SDK_INT >= 19) {
			asw_view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		}
		asw_view.setVerticalScrollBarEnabled(false);
		asw_view.setWebViewClient(new Callback());

		//Rendering the default URL
		aswm_view(ASWV_URL, false);

		asw_view.setWebChromeClient(new WebChromeClient() {
			//Handling input[type="file"] requests for android API 16+
			public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
				if(ASWP_FUPLOAD)
				{
					asw_file_message = uploadMsg;
					Intent i = new Intent(Intent.ACTION_GET_CONTENT);
					i.addCategory(Intent.CATEGORY_OPENABLE);
					i.setType(ASWV_F_TYPE);
					if(ASWP_MULFILE) {
						i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
					}
					startActivityForResult(Intent.createChooser(i, getString(R.string.fl_chooser)), asw_file_req);
				}
			}
			//Handling input[type="file"] requests for android API 21+
			public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams){
				if(check_permission(2) && check_permission(3))
				{
					if (ASWP_FUPLOAD)
					{
						if (asw_file_path != null) {
							asw_file_path.onReceiveValue(null);
						}
						asw_file_path = filePathCallback;
						Intent takePictureIntent = null;
						if (ASWP_CAMUPLOAD)
						{
							takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
							if (takePictureIntent.resolveActivity(MainActivity0.this.getPackageManager()) != null)
							{
								File photoFile = null;
								try
								{
									photoFile = create_image();
									takePictureIntent.putExtra("PhotoPath", asw_cam_message);
								} catch (Exception ex)
								{
									Log.e(TAG, "Image file creation failed", ex);
								}
								if (photoFile != null)
								{
									asw_cam_message = "file:" + photoFile.getAbsolutePath();
									takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
								}
								else
								{
									takePictureIntent = null;
								}
//								Log.e("TAG", "getPath() " +  photoFile.getPath()) ;
//								Log.e("TAG", "getParent() " +  photoFile.getParent()) ;
//								Log.e("TAG", "getName() " +  photoFile.getName()) ;
//
//								String origin = photoFile.getPath() ;
//								String change = photoFile.getParent() + "/Treecle_TEST.jpg" ;
//
//								photoFile = ResizeImages(origin, change) ;
							}
						}
						Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
						if (!ASWP_ONLYCAM) {
							contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
							contentSelectionIntent.setType(ASWV_F_TYPE);
							if (ASWP_MULFILE) {
								contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
							}
						}
						Intent[] intentArray;
						if (takePictureIntent != null) {
							intentArray = new Intent[]{takePictureIntent};
						} else {
							intentArray = new Intent[0];
						}

						Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
						chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
						chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.fl_chooser));
						chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
						startActivityForResult(chooserIntent, asw_file_req);

					}
					return true;
				}
				else
				{
					get_file();
					return false;
				}

			}


			//Getting webview rendering progress
			@Override
			public void onProgressChanged(WebView view, int p)
			{

			}

			// overload the geoLocations permissions prompt to always allow instantly as app permission was granted previously
			public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
				if(Build.VERSION.SDK_INT < 23 || check_permission(1)){
					// location permissions were granted previously so auto-approve
					callback.invoke(origin, true, false);
				} else {
					// location permissions not granted so request them
					ActivityCompat.requestPermissions(MainActivity0.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, loc_perm);
				}
			}
		});
		if (getIntent().getData() != null) {
			String path     = getIntent().getDataString();
            /*
            If you want to check or use specific directories or schemes or hosts

            Uri data        = getIntent().getData();
            String scheme   = data.getScheme();
            String host     = data.getHost();
            List<String> pr = data.getPathSegments();
            String param1   = pr.get(0);
            */
			aswm_view(path, false);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		asw_view.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		asw_view.onResume();
		//Coloring the "recent apps" tab header; doing it onResume, as an insurance
		if (Build.VERSION.SDK_INT >= 23) {
			Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
			ActivityManager.TaskDescription taskDesc;
			taskDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), bm, getColor(R.color.colorPrimary));
			MainActivity0.this.setTaskDescription(taskDesc);
		}
	}

	//Setting activity layout visibility
	private class Callback extends WebViewClient {
		public void onPageStarted(WebView view, String url, Bitmap favicon) {

		}

		public void onPageFinished(WebView view, String url) {
			findViewById(R.id.webView).setVisibility(View.VISIBLE);
		}
		//For android below API 23
		@SuppressWarnings("deprecation")
		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			Toast.makeText(getApplicationContext(), getString(R.string.went_wrong), Toast.LENGTH_SHORT).show();
			aswm_view("file:///android_asset/error.html", false);
		}

		//Overriding webview URLs
		@SuppressWarnings("deprecation")
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			CURR_URL = url;
			return url_actions(view, url);
		}

		//Overriding webview URLs for API 23+ [suggested by github.com/JakePou]
		@TargetApi(Build.VERSION_CODES.N)
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
			CURR_URL = request.getUrl().toString();
			return url_actions(view, request.getUrl().toString());
		}

		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
			if(ASWP_CERT_VERIFICATION) {
				super.onReceivedSslError(view, handler, error);
			} else {
				handler.proceed(); // Ignore SSL certificate errors
			}
		}
	}

	//Random ID creation function to help get fresh cache every-time webview reloaded
	public String random_id() {
		return new BigInteger(130, random).toString(32);
	}

	//Opening URLs inside webview with request
	void aswm_view(String url, Boolean tab) {
		if (tab) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			startActivity(intent);
		} else {
			if(url.contains("?")){ // check to see whether the url already has query parameters and handle appropriately.
				url += "&";
			} else {
				url += "?";
			}
			url += "rid="+random_id();
			asw_view.loadUrl(url);
		}
	}

	//Actions based on shouldOverrideUrlLoading
	public boolean url_actions(WebView view, String url){
		boolean a = true;
		//Show toast error if not connected to the network
		if (!ASWP_OFFLINE && !DetectConnection.isInternetAvailable(MainActivity0.this)) {
			Toast.makeText(getApplicationContext(), getString(R.string.check_connection), Toast.LENGTH_SHORT).show();

			//Use this in a hyperlink to redirect back to default URL :: href="refresh:android"
		} else if (url.startsWith("refresh:")) {
			pull_fresh();

			//Use this in a hyperlink to launch default phone dialer for specific number :: href="tel:+919876543210"
		} else if (url.startsWith("tel:")) {
			Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
			startActivity(intent);

			//Use this to open your apps page on google play store app :: href="rate:android"
		} else if (url.startsWith("rate:")) {
			final String app_package = getPackageName(); //requesting app package name from Context or Activity object
			try {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app_package)));
			} catch (ActivityNotFoundException anfe) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + app_package)));
			}

			//Sharing content from your webview to external apps :: href="share:URL" and remember to place the URL you want to share after share:___
		} else if (url.startsWith("share:")) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, view.getTitle());
			intent.putExtra(Intent.EXTRA_TEXT, view.getTitle()+"\nVisit: "+(Uri.parse(url).toString()).replace("share:",""));
			startActivity(Intent.createChooser(intent, getString(R.string.share_w_friends)));

			//Use this in a hyperlink to exit your app :: href="exit:android"
		} else if (url.startsWith("exit:")) {
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);

			//Getting location for offline files
		} else if (ASWP_EXTURL && !aswm_host(url).equals(ASWV_HOST)) {
			aswm_view(url,false);
		} else {
			a = false;
		}
		return a;
	}

	//Getting host name
	public static String aswm_host(String url){
		if (url == null || url.length() == 0) {
			return "";
		}
		int dslash = url.indexOf("//");
		if (dslash == -1) {
			dslash = 0;
		} else {
			dslash += 2;
		}
		int end = url.indexOf('/', dslash);
		end = end >= 0 ? end : url.length();
		int port = url.indexOf(':', dslash);
		end = (port > 0 && port < end) ? port : end;
		Log.w("URL Host: ",url.substring(dslash, end));
		return url.substring(dslash, end);
	}

	//Reloading current page
	public void pull_fresh(){
		aswm_view(CURR_URL,false);
	}

	//Getting device basic information
	public void get_info(){
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.setAcceptCookie(true);
		cookieManager.setCookie(ASWV_URL, "DEVICE=android");
		cookieManager.setCookie(ASWV_URL, "DEV_API=" + Build.VERSION.SDK_INT);
	}

	//Checking permission for storage and camera for writing and uploading images
	public void get_file(){
		String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

		//Checking for storage permission to write images for upload
		if (ASWP_FUPLOAD && ASWP_CAMUPLOAD && !check_permission(2) && !check_permission(3)) {
			ActivityCompat.requestPermissions(MainActivity0.this, perms, file_perm);

			//Checking for WRITE_EXTERNAL_STORAGE permission
		} else if (ASWP_FUPLOAD && !check_permission(2)) {
			ActivityCompat.requestPermissions(MainActivity0.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, file_perm);

			//Checking for CAMERA permissions
		} else if (ASWP_CAMUPLOAD && !check_permission(3)) {
			ActivityCompat.requestPermissions(MainActivity0.this, new String[]{Manifest.permission.CAMERA}, file_perm);
		}
	}



	//Checking if particular permission is given or not
	public boolean check_permission(int permission){
		switch(permission){
			case 1:
				return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

			case 2:
				return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

			case 3:
				return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

		}
		return false;
	}

	//Creating image file for upload
	private File create_image()
	{
		try {
			@SuppressLint("SimpleDateFormat")
			String file_name    = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
			String new_name     = "file_"+file_name+"_";
			File sd_directory   = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

			return File.createTempFile(new_name, ".jpg", sd_directory);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null ;
	}


	public static Bitmap rotateImage(Bitmap source, float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
				matrix, true);
	}

	public static void checkRotate(String path) {

		//파일경로를 비트맵으로 가져옴
		Bitmap bitmap = BitmapFactory.decodeFile(path);

		//방향 초기화
		int orientation = 0;

		//파일로부터 정보 가져옴
		try {
			ExifInterface ei = new ExifInterface(path);
			orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
		}
		catch(Exception e){
			return;
		}

		//화면 방향에 따른 이미지 재설정
		Matrix matrix = new Matrix();
		switch (orientation) {
			case ExifInterface.ORIENTATION_NORMAL:
				return;
			case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
				matrix.setScale(-1, 1);
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				matrix.setRotate(180);
				break;
			case ExifInterface.ORIENTATION_FLIP_VERTICAL:
				matrix.setRotate(180);
				matrix.postScale(-1, 1);
				break;
			case ExifInterface.ORIENTATION_TRANSPOSE:
				matrix.setRotate(90);
				matrix.postScale(-1, 1);
				break;
			case ExifInterface.ORIENTATION_ROTATE_90:
				matrix.setRotate(90);
				break;
			case ExifInterface.ORIENTATION_TRANSVERSE:
				matrix.setRotate(-90);
				matrix.postScale(-1, 1);
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				matrix.setRotate(-90);
				break;
			default:
				return;
		}

		//기존 파일명으로 다시 저장
		try {
			Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			bitmap.recycle();


			File file = new File(path);  //Pictures폴더 screenshot.png 파일
			FileOutputStream os = null;
			try{
				os = new FileOutputStream(file);
				bmRotated.compress(Bitmap.CompressFormat.JPEG, 90, os);   //비트맵을 PNG파일로 변환
				os.close();
			}catch (IOException e){
				e.printStackTrace();
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}




	private File ResizeImages(String sPath)
	{
		if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT) {
		}

		try
		{
			String returnFileName = sPath.replace("file:", "") ;
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = SAMPLE_SIZE;

			Bitmap photo = BitmapFactory.decodeFile(returnFileName, options);

			int width = photo.getWidth() ;
			int height = photo.getHeight() ;

			Matrix matrix = new Matrix();
			checkRotate(returnFileName);
			//matrix.postRotate(ROTATE); // 회전

			photo = Bitmap.createBitmap(photo, 0, 0, width, height, matrix, true);
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			photo.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

			File f = new File(returnFileName);
			f.createNewFile();
			FileOutputStream fo = new FileOutputStream(f);
			fo.write(bytes.toByteArray());
			fo.close();
			return f ;
		}
		catch (Exception e)
		{
			Log.e("TAG", "===============ERROR==================") ;
			e.printStackTrace();
		}
		return null ;
	}





	private Bitmap resize(Context context,Uri uri,int resize) {
		Bitmap resizeBitmap = null;

		BitmapFactory.Options options = new BitmapFactory.Options();
		try {
			BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options); // 1번

			int width = options.outWidth;
			int height = options.outHeight;
			int samplesize = 4;

			while (true) {//2번
				if (width / 4 < resize || height / 4 < resize)
					break;
				width /= 4;
				height /= 4;
				samplesize *= 4;
			}

			options.inSampleSize = samplesize;
			Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options); //3번
			resizeBitmap = bitmap;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return resizeBitmap;
	}


	//Creating custom notifications with IDs
	public void show_notification(int type, int id) {
		long when = System.currentTimeMillis();
		asw_notification = (NotificationManager) MainActivity0.this.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent i = new Intent();
		if (type == 1) {
			i.setClass(MainActivity0.this, MainActivity0.class);
		} else if (type == 2) {
			i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		} else {
			i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			i.addCategory(Intent.CATEGORY_DEFAULT);
			i.setData(Uri.parse("package:" + MainActivity0.this.getPackageName()));
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		}
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity0.this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity0.this, "");
		switch(type){
			case 1:
				builder.setTicker(getString(R.string.app_name));
				builder.setContentTitle(getString(R.string.loc_fail));
				builder.setContentText(getString(R.string.loc_fail_text));
				builder.setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.loc_fail_more)));
				builder.setVibrate(new long[]{350,350,350,350,350});
				builder.setSmallIcon(R.mipmap.ic_launcher);
				break;

			case 2:
				builder.setTicker(getString(R.string.app_name));
				builder.setContentTitle(getString(R.string.loc_perm));
				builder.setContentText(getString(R.string.loc_perm_text));
				builder.setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.loc_perm_more)));
				builder.setVibrate(new long[]{350, 700, 350, 700, 350});
				builder.setSound(alarmSound);
				builder.setSmallIcon(R.mipmap.ic_launcher);
				break;
		}
		builder.setOngoing(false);
		builder.setAutoCancel(true);
		builder.setContentIntent(pendingIntent);
		builder.setWhen(when);
		builder.setContentIntent(pendingIntent);
		asw_notification_new = builder.build();
		asw_notification.notify(id, asw_notification_new);
	}

	//Action on back key tap/click
	@Override
	public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (keyCode) {
				case KeyEvent.KEYCODE_BACK:
					if (asw_view.canGoBack()) {
						asw_view.goBack();
					} else {
						finish();
					}
					return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState ){
		super.onSaveInstanceState(outState);
		asw_view.saveState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		asw_view.restoreState(savedInstanceState);
	}



}
