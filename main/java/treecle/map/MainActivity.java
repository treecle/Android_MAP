package treecle.map;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import androidx.annotation.RequiresApi;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;

import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.common.util.IOUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.webkit.GeolocationPermissions;

import static treecle.map.RealPathUtil.getDataColumn;
import static treecle.map.RealPathUtil.isDownloadsDocument;
import static treecle.map.RealPathUtil.isExternalStorageDocument;
import static treecle.map.RealPathUtil.isMediaDocument;

public class MainActivity extends AppCompatActivity {

    Context mContext;
    WebView browser;
    SwipeRefreshLayout pullfresh;
    private final Handler handler = new Handler();

    boolean isShowRefreash = false;

    private String cacheFilePath = null;

    private static final String TYPE_IMAGE = "image/*";
    private static final int INPUT_FILE_REQUEST_CODE = 1;

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    private static String fcmToken = "";

    UploadHandler mUploadHandler;
    private Uri mCapturedImageURI = null;
    private static final int FILECHOOSER_RESULTCODE   = 2888;
    public final static int FILECHOOSER_NORMAL_REQ_CODE = 2001;
    public final static int FILECHOOSER_LOLLIPOP_REQ_CODE = 2002;
    public ValueCallback<Uri> filePathCallbackNormal;
    public ValueCallback<Uri[]> filePathCallbackLollipop;

    private Uri cameraImageUri = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        browser = ((WebView)findViewById(R.id.webView));

        ImageView iv = findViewById(R.id.ex1);
        Glide.with(this)
                .load(R.drawable.gif)

                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(iv);

        View view = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (view != null) {
                // 23 버전 이상일 때 상태바 하얀 색상에 회색 아이콘 색상을 설정
                view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }
        }else if (Build.VERSION.SDK_INT >= 21) {
            // 21 버전 이상일 때
            getWindow().setStatusBarColor(Color.BLACK);
        }

        if(getIntent().hasExtra("link")){
            setBrowser(getIntent().getStringExtra("link"));
        }
        else{
            setBrowser("https://treecle.io/map");
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {


        FirebaseApp.initializeApp(this);
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                fcmToken = instanceIdResult.getToken();
                Log.e("fcm token", fcmToken);
            }
        });
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus == false){

        }
        else {
        }
    }

    public static String getPath(final Context context, final Uri uri) {

        // DocumentProvider
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }else{
                    Toast.makeText(context, "Could not get file path. Please try again", Toast.LENGTH_SHORT).show();
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else {
                    contentUri = MediaStore.Files.getContentUri("external");
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILECHOOSER_NORMAL_REQ_CODE:
                if (resultCode == RESULT_OK) {
                    if (filePathCallbackNormal == null) return;
                    Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData(); //  onReceiveValue 로 파일을 전송한다.

                    filePathCallbackNormal.onReceiveValue(result);
                    filePathCallbackNormal = null;
                }
                break;
            case FILECHOOSER_LOLLIPOP_REQ_CODE:

                if (resultCode == RESULT_OK) {
                    if (filePathCallbackLollipop == null) return;
                    if (data == null)
                        data = new Intent();
                    if (data.getData() == null)
                        data.setData(cameraImageUri);

                    filePathCallbackLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                    filePathCallbackLollipop = null;

                } else {
                    if (filePathCallbackLollipop != null) {   //  resultCode에 RESULT_OK가 들어오지 않으면 null 처리하지 한다.(이렇게 하지 않으면 다음부터 input 태그를 클릭해도 반응하지 않음)
                        filePathCallbackLollipop.onReceiveValue(null);
                        filePathCallbackLollipop = null;
                    }

                    if (filePathCallbackNormal != null) {
                        filePathCallbackNormal.onReceiveValue(null);
                        filePathCallbackNormal = null;
                    }
                }
                break;
            default:

                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setBrowser(String url){

        //웹뷰 설정
        WebSettings settings = browser.getSettings();    //세팅 불러옴
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setUseWideViewPort(true);
        settings.setTextZoom(100);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // 혼합된 컨텐츠 허용
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);										 // Cookie 허용
            cookieManager.setAcceptThirdPartyCookies(browser, true);
        }

        //캐시사용 설정
        File dir = mContext.getCacheDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        settings.setAppCachePath(dir.getPath());
        settings.setAppCacheEnabled(true);

        String userAgent = settings.getUserAgentString();
        userAgent = userAgent.replaceAll("; webview", "");
        userAgent = userAgent.replaceAll("; wv", "");
        userAgent = userAgent.replaceAll("webview", "");
        userAgent = userAgent.replaceAll("wv", "");

        Log.e("userAgent", userAgent);
        settings.setUserAgentString(userAgent);

        browser.addJavascriptInterface(new AndroidBridge(), "native");

        //browser.setWebChromeClient(mWebChromeClient);
        browser.setWebChromeClient(new MyWebChromeClient());

        browser.setWebViewClient(mWebViewClient);

        browser.loadUrl(url);
    }



    private File resizeImage(String path){

        //수정된 이미지를 저장할 임시 폴더를 생성합니다.
        String tmpPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        tmpPath += "/treecle";
        File tmpPathFile = new File(tmpPath);
        tmpPathFile.mkdirs();


        //선택한 파일의 명칭을 가져옵니다.
        String returnFileName = path.replace("file:", "") ;


        //파일명 부분을 가져온후, 임시폴더+파일명으로 저장할 전체경로를 정합니다.
        String[] splitPath = returnFileName.split("/");
        String savaPath = tmpPath+"/"+splitPath[splitPath.length-1];
        Log.e("저장할 전체경로", savaPath);

        // 4등분한 크기로 조절된 비트맵을 생성합니다.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);


        //파일로부터 방향 가져와서 matrix 회전시킵니다.
        int orientation = 0;
        try {

            ExifInterface ei = new ExifInterface(path);
            orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            Log.e("불러온 이미지 방향", orientation+"");

            Matrix matrix = new Matrix();

            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    break;
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
                    break;
            }

            //회전된 비트맵
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            //회전된 비트맵을 파일로 저장합니다.
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

            File saveFile = new File(savaPath);
            FileOutputStream fo = new FileOutputStream(saveFile);
            fo.write(bytes.toByteArray());
            fo.close();
            return saveFile ;
        }
        catch(Exception e){
            //sendNoti("디버그용", "에러(02)-"+e.toString());
            return null;
        }

    }

    private boolean checkAppInstalled(WebView view , String url , String type){
        if(type.equals("intent")){
            return intentSchemeParser(view, url);
        } else if(type.equals("customLink")){
            return customSchemeParser(view, url);
        }
        return false;
    }

    private boolean intentSchemeParser(WebView view , String url) {
        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            if(getPackageManager().resolveActivity(intent , 0) == null){
                String pakagename = intent.getPackage();
                if(pakagename != null) {
                    Uri uri = Uri.parse("market://details?id="+pakagename);
                    intent = new Intent(Intent.ACTION_VIEW , uri);
                    startActivity(intent);
                    return true;
                }
            }
            Uri uri = Uri.parse(intent.getDataString());
            intent = new Intent(Intent.ACTION_VIEW , uri);
            startActivity(intent);
            return true;

        } catch (URISyntaxException e) {
        }
        return false;
    }

    private boolean customSchemeParser(WebView view , String url) {
        String packageName = null;
        if(url.startsWith("shinhan-sr-ansimclick://")) {        //신한 앱카드
            packageName = "com.shcard.smartpay";
        }else if(url.startsWith("mpocket.online.ansimclick://")) {  //삼성앱카드
            packageName = "kr.co.samsungcard.mpocket";
        } else if(url.startsWith("hdcardappcardansimclick://")) {       //현대안심결제
            packageName = "com.hyundaicard.appcard";
        } else if(url.startsWith("droidxantivirusweb:")){               //droidx 백신
            packageName = "net.nshc.droidxantivirus";
        } else if(url.startsWith("vguardstart://") || url.startsWith("vguardend://")){  //vguard백신
            packageName = "kr.co.shiftworks.vguardweb";
        } else if(url.startsWith("hanaansim")){         //하나외환앱카드
            packageName = "com.ilk.visa3d";
        } else if(url.startsWith("nhappcardansimclick://")) { //농협앱카드
            packageName = "nh.smart.mobilecard";
        } else if(url.startsWith("ahnlabv3mobileplus")){
            packageName = "com.ahnlab.v3mobileplus";
        } else if(url.startsWith("smartxpay-transfer://")){
            packageName = "kr.co.uplus.ecredit";
        } else if(url.startsWith("ispmobile://")){
            packageName = "kvp.jjy.MispAndroid320";
        } else if(url.startsWith("kb-acp://")){
            packageName = "com.kbcard.cxh.appcard";
        }
        else {
            return false;
        }

        Intent intent = null;
        //하드코딩된 패키지명으로 앱 설치여부를 판단하여 해당 앱 실행 또는 마켓 이동
        if(chkAppInstalled(view,packageName)){

            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                Uri uri = Uri.parse(intent.getDataString());
                intent = new Intent(Intent.ACTION_VIEW , uri);
                startActivity(intent);
                return true;
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            Uri uri = Uri.parse("market://details?id="+packageName);
            intent = new Intent(Intent.ACTION_VIEW , uri);
            startActivity(intent);
            return true;
        }

        return false;
    }

    private boolean chkAppInstalled(WebView view , String packagePath){
        boolean appInstalled = false;
        try {
            getPackageManager().getPackageInfo(packagePath, PackageManager.GET_ACTIVITIES);
            appInstalled = true;
        } catch(PackageManager.NameNotFoundException e){
            appInstalled = false;
        }
        return appInstalled;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }

    private Uri getResultUri(Intent data) {
        Uri result = null;
        if(data == null || TextUtils.isEmpty(data.getDataString())) {
            // If there is not data, then we may have taken a photo
            if(mCameraPhotoPath != null) {
                result = Uri.parse(mCameraPhotoPath);
            }
        } else {
            String filePath = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                filePath = data.getDataString();
            } else {
                filePath = "file:" + RealPathUtil.getRealPath(this, data.getData());
            }
            result = Uri.parse(filePath);
        }

        return result;
    }

    BackPressCloseHandler backPressCloseHandler = new BackPressCloseHandler(this);
    @Override
    public void onBackPressed() {
        if (browser.canGoBack()) {
            browser.goBack();
        } else {
            super.onBackPressed();
        }
     //backPressCloseHandler.onBackPressed();
    }

    class MyWebChromeClient extends WebChromeClient {
        public MyWebChromeClient() {

        }

        private String getTitleFromUrl(String url) {
            String title = url;
            try {
                URL urlObj = new URL(url);
                String host = urlObj.getHost();
                if (host != null && !host.isEmpty()) {
                    return urlObj.getProtocol() + "://" + host;
                }
                if (url.startsWith("file:")) {
                    String fileName = urlObj.getFile();
                    if (fileName != null && !fileName.isEmpty()) {
                        return fileName;
                    }
                }
            } catch (Exception e) {
                // ignore
            }

            return title;
        }

        @Override
        public boolean onJsAlert(android.webkit.WebView view, String url, String message, final JsResult result) {
            String newTitle = getTitleFromUrl(url);

            new AlertDialog.Builder(MainActivity.this).setTitle(newTitle).setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            }).setCancelable(false).create().show();
            return true;
            // return super.onJsAlert(view, url, message, result);
        }

        @Override
        public boolean onJsConfirm(android.webkit.WebView view, String url, String message, final JsResult result) {

            String newTitle = getTitleFromUrl(url);

            new AlertDialog.Builder(MainActivity.this).setTitle(newTitle).setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.cancel();
                }
            }).setCancelable(false).create().show();
            return true;

            // return super.onJsConfirm(view, url, message, result);
        }

        // 191120 for KaKaoMap Location
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            super.onGeolocationPermissionsShowPrompt(origin, callback);
            callback.invoke(origin, true, false);
        }


        // Android 4.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            mUploadHandler = new UploadHandler(new Controller());
            mUploadHandler.openFileChooser(uploadMsg, acceptType, capture);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public boolean onShowFileChooser( WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

            // Callback 초기화
            if (filePathCallbackLollipop != null) {
                filePathCallbackLollipop.onReceiveValue(null);
                filePathCallbackLollipop = null;
            }
            filePathCallbackLollipop = filePathCallback;

            boolean isCapture = fileChooserParams.isCaptureEnabled();

            runCamera(isCapture, 1);
            return true;
        }
    };
    private void runCamera(boolean _isCapture, int selectedType) {
        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File path = Environment.getExternalStorageDirectory();
        File file = new File(path, "temp.png"); // temp.png 는 카메라로 찍었을 때 저장될 파일명이므로 사용자 마음대로

        cameraImageUri = Uri.fromFile(file);
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

        if (!_isCapture) { // 선택팝업 카메라, 갤러리 둘다 띄우고 싶을 때
            Intent pickIntent = new Intent(Intent.ACTION_PICK);

            if (selectedType == 1) {
                pickIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                pickIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            }
            else if(selectedType == 2) {
                pickIntent.setType(MediaStore.Video.Media.CONTENT_TYPE);
                pickIntent.setData(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            }

            String pickTitle = "사진 가져올 방법을 선택하세요.";
            Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);

            // 카메라 intent 포함시키기..
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{intentCamera});
            startActivityForResult(chooserIntent, FILECHOOSER_LOLLIPOP_REQ_CODE);
        }
        else {// 바로 카메라 실행..
            startActivityForResult(intentCamera, FILECHOOSER_LOLLIPOP_REQ_CODE);
        }
    }

    class Controller {
        final static int FILE_SELECTED = 4;

        Activity getActivity() {
            return MainActivity.this;
        }
    }

    class UploadHandler {
        /*
         * The Object used to inform the WebView of the file to upload.
         */
        private ValueCallback<Uri> mUploadMessage;
        private String mCameraFilePath;
        private boolean mHandled;
        private boolean mCaughtActivityNotFoundException;
        private Controller mController;
        public UploadHandler(Controller controller) {
            mController = controller;
        }
        String getFilePath() {
            return mCameraFilePath;
        }
        boolean handled() {
            return mHandled;
        }
        void onResult(int resultCode, Intent intent) throws FileNotFoundException {
            if (resultCode == Activity.RESULT_CANCELED && mCaughtActivityNotFoundException) {
                // Couldn't resolve an activity, we are going to try again so skip
                // this result.
                mCaughtActivityNotFoundException = false;
                return;
            }
            Uri result = intent == null || resultCode != Activity.RESULT_OK ? null
                    : intent.getData();
            // As we ask the camera to save the result of the user taking
            // a picture, the camera application does not return anything other
            // than RESULT_OK. So we need to check whether the file we expected
            // was written to disk in the in the case that we
            // did not get an intent returned but did get a RESULT_OK. If it was,
            // we assume that this result has came back from the camera.
            if (result == null && intent == null && resultCode == Activity.RESULT_OK) {
                File cameraFile = new File(mCameraFilePath);
                if (cameraFile.exists()) {
                    result = Uri.fromFile(cameraFile);
                    // Broadcast to the media scanner that we have a new photo
                    // so it will be added into the gallery for the user.
                    mController.getActivity().sendBroadcast(
                            new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, result));
                }
            }

            if(result != null) {
                Uri albumUri = intent.getData( );
                String fileName = getFileName( albumUri );

                ParcelFileDescriptor parcelFileDescriptor = null;
                try {
                    parcelFileDescriptor = getContentResolver( ).openFileDescriptor( albumUri, "r" );
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if ( parcelFileDescriptor == null ) return;
                FileInputStream inputStream = new FileInputStream( parcelFileDescriptor.getFileDescriptor( ) );
                File cacheFile = new File( MainActivity.this.getCacheDir( ), fileName );
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream( cacheFile );
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    IOUtils.copyStream( inputStream, outputStream );
                } catch (IOException e) {
                    e.printStackTrace();
                }

                cacheFilePath = cacheFile.getAbsolutePath( );
                mUploadMessage.onReceiveValue(Uri.fromFile(new File(cacheFilePath)));

            }
            else {

                mUploadMessage.onReceiveValue(result);
            }

            mHandled = true;
            mCaughtActivityNotFoundException = false;
        }

        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            mUploadMessage = uploadMsg;
            try{
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File externalDataDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                File cameraDataDir = new File(externalDataDir.getAbsolutePath() +File.separator + "browser-photos");

                cameraDataDir.mkdirs();
                String mCameraFilePath = cameraDataDir.getAbsolutePath() + File.separator +
                       System.currentTimeMillis() + ".jpg";

                mCapturedImageURI = Uri.fromFile(new File(mCameraFilePath));

                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");

                Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[] { cameraIntent });

                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
            }
            catch(Exception e){
                Toast.makeText(getBaseContext(), "Camera Exception:"+e, Toast.LENGTH_LONG).show();
            }
        }

        // For Android < 3.0
        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg ) {
            openFileChooser(uploadMsg, "");
        }

        // For Android  > 4.1.1
        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
            openFileChooser(uploadMsg, acceptType);
        }


        public boolean onConsoleMessage(ConsoleMessage cm) {
            onConsoleMessage(cm.message(), cm.lineNumber(), cm.sourceId());
            return true;
        }
        public void onConsoleMessage(String message, int lineNumber, String sourceID) {
            Log.d("androidruntime", "www.example.com: " + message);
        }

        private void startActivity(Intent intent) {
            try {
                mController.getActivity().startActivityForResult(intent, Controller.FILE_SELECTED);
            } catch (ActivityNotFoundException e) {
                // No installed app was able to handle the intent that
                // we sent, so fallback to the default file upload control.
                try {
                    mCaughtActivityNotFoundException = true;
                    mController.getActivity().startActivityForResult(createDefaultOpenableIntent(),
                            Controller.FILE_SELECTED);
                } catch (ActivityNotFoundException e2) {
                    // Nothing can return us a file, so file upload is effectively disabled.
                    Toast.makeText(mController.getActivity(), "File Upload",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
        private Intent createDefaultOpenableIntent() {
            // Create and return a chooser with the default OPENABLE
            // actions including the camera, camcorder and sound
            // recorder where available.
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            Intent chooser = createChooserIntent(createCameraIntent(), createCamcorderIntent(),
                    createSoundRecorderIntent());
            chooser.putExtra(Intent.EXTRA_INTENT, i);
            return chooser;
        }
        private Intent createChooserIntent(Intent... intents) {
            Intent chooser = new Intent(Intent.ACTION_CHOOSER);
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents);
            chooser.putExtra(Intent.EXTRA_TITLE, "File Upload");
            return chooser;
        }
        private Intent createOpenableIntent(String type) {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType(type);
            return i;
        }

        private Intent createCameraIntent() {
            Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            File path = Environment.getExternalStorageDirectory();
            File file = new File(path, "temp.png");

            cameraImageUri = Uri.fromFile(file);
            intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

            startActivityForResult(intentCamera, FILECHOOSER_LOLLIPOP_REQ_CODE);

            return intentCamera;
            /*
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File externalDataDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM);
            File cameraDataDir = new File(externalDataDir.getAbsolutePath() +
                    File.separator + "Webbme");
            cameraDataDir.mkdirs();
            mCameraFilePath = cameraDataDir.getAbsolutePath() + File.separator +
                    System.currentTimeMillis() + ".jpg";
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(resizeImage(mCameraFilePath)));
             */
        }

        private Intent createCamcorderIntent() {
            return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        }
        private Intent createSoundRecorderIntent() {
            return new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        }
    }

    //앨범에서 선택한 사진이름 가져오기
    public String getFileName( Uri uri ) {
        Cursor cursor = getContentResolver( ).query( uri, null, null, null, null );
        try {
            if ( cursor == null ) return null;
            cursor.moveToFirst( );
            String fileName = cursor.getString( cursor.getColumnIndex( OpenableColumns.DISPLAY_NAME ) );
            cursor.close( );
            return fileName;

        } catch ( Exception e ) {
            e.printStackTrace( );
            cursor.close( );
            return null;
        }
    }

    WebViewClient mWebViewClient = new WebViewClient() {


        public static final String INTENT_PROTOCOL_START = "intent:";
        public static final String INTENT_PROTOCOL_INTENT = "#Intent;";
        public static final String INTENT_PROTOCOL_END = ";end;";
        public static final String GOOGLE_PLAY_STORE_PREFIX = "market://details?id=";

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url=request.getUrl().toString();
            Log.e("shouldOverrideUrl", url);

            if(url.startsWith("intent")){
                return checkAppInstalled(view, url, "intent");
            }else if (url != null && url.startsWith("market://")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        startActivity(intent);
                    }
                    return true;
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    return false;
                }
            } else if(url.startsWith("http://") || url.startsWith("https://")) {
                view.loadUrl(url);
                return false;
            }
            else if (url.startsWith("tel:")) {
                Intent tel = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                startActivity(tel);
                return true;
            }
            else if (url.startsWith("mailto:")) {
                String body = "Enter your Question, Enquiry or Feedback below:\n\n";
                Intent mail = new Intent(Intent.ACTION_SEND);
                mail.setType("application/octet-stream");
                mail.putExtra(Intent.EXTRA_EMAIL, new String[]{"email address"});
                mail.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                mail.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(mail);
                return true;
            }
            else {
                return checkAppInstalled(view, url , "customLink");
            }

        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.e("shouldOverrideUrl", url);


            if(url.startsWith("intent")){
                return checkAppInstalled(view, url, "intent");
            }else if (url != null && url.startsWith("market://")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        startActivity(intent);
                    }
                    return true;
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    return false;
                }
            } else if(url.startsWith("http://") || url.startsWith("https://")) {
                view.loadUrl(url);
                return false;
            }
            else if (url.startsWith("tel:")) {
                Intent tel = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                startActivity(tel);
                return true;
            }
            else if (url.startsWith("mailto:")) {
                String body = "Enter your Question, Enquiry or Feedback below:\n\n";
                Intent mail = new Intent(Intent.ACTION_SEND);
                mail.setType("application/octet-stream");
                mail.putExtra(Intent.EXTRA_EMAIL, new String[]{"email address"});
                mail.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                mail.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(mail);
                return true;
            }
            else {
                return checkAppInstalled(view, url , "customLink");
            }

        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon){
            super.onPageStarted(view, url, favicon);
            Log.e("onPageStarted", url);
        }


        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.e("onPageFinished", url);
            if(url.equals("https://treecle.io/map/")) {
                findViewById(R.id.layout_intro).setVisibility(View.GONE);
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            //showToast("에러발생"+error.toString(), mContext);
            //finish();
        }

    };

    private class AndroidBridge{

        @JavascriptInterface
        public void finishApp(){
            MainActivity.this.finish();
        }

        //토스트 출력 (기본, 짧은출력)
        @JavascriptInterface
        public void showToast(final String msg){
            handler.post(new Runnable(){
                public void run(){
                    Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        //토스트 출력 (긴 출력)
        @JavascriptInterface
        public void showToastLong(final String msg){
            handler.post(new Runnable(){
                public void run(){
                    Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                }
            });
        }

        //다른앱으로 텍스트 전달
        @JavascriptInterface
        public void sendText(final String title, final String text){
            Intent it = new Intent(Intent.ACTION_SEND);
            it.setType("text/plain");
            it.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(it, title));
        }

        //기본 버튼음 발생시키기
        @JavascriptInterface
        public void clickSound(){
            handler.post(new Runnable(){
                public void run(){
                    AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                    audioManager.playSoundEffect(SoundEffectConstants.CLICK);
                }
            });

        }

        //버전이름 조회
        @JavascriptInterface
        public String getPackageVersionName(String packageName){
            String version = null;
            try {
                PackageInfo i = mContext.getPackageManager().getPackageInfo(packageName, 0);
                version = i.versionName;
            } catch(PackageManager.NameNotFoundException e) { }

            return version;
        }

        //버전코드 조회
        @JavascriptInterface
        public String getPackageVersionCode(String packageName){
            int version = 0;
            try {
                PackageInfo i = mContext.getPackageManager().getPackageInfo(packageName, 0);
                version = i.versionCode;
            } catch(PackageManager.NameNotFoundException e) { }

            return String.valueOf(version);
        }

        //구글 플레이 스토어 어플 상세보기 페이지 링크
        @JavascriptInterface
        public void linkStore(String packageName){
            String url = "";

            PackageManager pm = getPackageManager();
            String installed = "0";
            try {
                pm.getPackageInfo("com.android.vending", PackageManager.GET_ACTIVITIES);
                url = "market://details?id="+packageName;
            } catch (PackageManager.NameNotFoundException e) {
                url = "https://market.android.com/details?id="+packageName;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);

            intent.setData(Uri.parse(url));

            mContext.startActivity(intent);
        }



        //휴대폰번호 조회
        @JavascriptInterface
        public String getLineNumber(){
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

            try {
                String number_1 = telephonyManager.getLine1Number();
                if((number_1 != null) && (number_1.indexOf("+82") > -1))
                {
                    number_1 = number_1.replace("+82", "0");
                }
                return number_1.replace("-", "");
            }
            catch (SecurityException e){
                return "";
            }
        }

        //패키지 존재확인
        @JavascriptInterface
        public String isPackageExists(String targetPackage){
            PackageManager pm = getPackageManager();
            try {
                PackageInfo info=pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                return "0";
            }
            return "1";
        }


        //fcm 토큰 조회
        @JavascriptInterface
        public String getTreecleToken(){
            return fcmToken;
        }
    }

}
