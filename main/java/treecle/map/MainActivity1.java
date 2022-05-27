package treecle.map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity1 extends FragmentActivity
{
    private final String Default_URL = "https://treecle.io/map" ;
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int FILECHOOSER_RESULTCODE = 1;
    private static final String TAG = MainActivity1.class.getSimpleName();
    private WebView webView;
    private WebSettings webSettings;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null)
            {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK)
            {
                if (data == null)
                {
                    if (mCameraPhotoPath != null)
                    {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                }
                else
                {
                    String dataString = data.getDataString();

                    if (dataString != null)
                    {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }
        else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
        {
            if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null)
            {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            if (requestCode == FILECHOOSER_RESULTCODE)
            {
                if (this.mUploadMessage == null)
                {
                    return;
                }

                Uri result = null;

                try
                {
                    if (resultCode != RESULT_OK)
                    {
                        result = null;
                    }
                    else
                    {
                        result = data == null ? mCapturedImageURI : data.getData();
                    }
                }
                catch (Exception e)
                {
                    Toast.makeText(getApplicationContext(), "activity :" + e, Toast.LENGTH_LONG).show();
                }
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webView);
        webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAllowFileAccess(true);
        webView.setWebViewClient(new Client());
        webView.setWebChromeClient(new ChromeClient());
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        if (Build.VERSION.SDK_INT >= 19)
        {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        else if(Build.VERSION.SDK_INT >=11 && Build.VERSION.SDK_INT < 19)
        {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        webView.loadUrl(Default_URL);


    }

    private File createImageFile() throws IOException
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(imageFileName,".jpg", storageDir);
//TODO 리사이징
        return imageFile;
    }

    public class ChromeClient extends WebChromeClient
    {
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams)
        {
            if (mFilePathCallback != null)
            {
                mFilePathCallback.onReceiveValue(null);
            }

            mFilePathCallback = filePath;
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null)
            {
                File photoFile = null;
                try
                {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                }
                catch (IOException ex)
                {
                    Log.e(TAG, "Unable to create Image File", ex);
                }

                if (photoFile != null)
                {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                }
                else
                {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");
            Intent[] intentArray;

            if (takePictureIntent != null)
            {
                intentArray = new Intent[]{takePictureIntent};
            }
            else
            {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

            return true;
        }


        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType)
        {
            mUploadMessage = uploadMsg;
            File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AndroidExampleFolder");
            if (!imageStorageDir.exists())
            {
                imageStorageDir.mkdirs();
            }

            File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
            mCapturedImageURI = Uri.fromFile(file);

            final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");

            Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[] { captureIntent });
            startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
        }

        public void openFileChooser(ValueCallback<Uri> uploadMsg)
        {
            openFileChooser(uploadMsg, "");
        }

        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)
        {
            openFileChooser(uploadMsg, acceptType);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (event.getAction() != KeyEvent.ACTION_DOWN)
        {
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if (webView.canGoBack())
            {
                webView.goBack();
            }
            else
            {
                onBackPressed();
            }
            return true;
        }
        return false;
    }

    public class Client extends WebViewClient
    {
        ProgressDialog progressDialog;

        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            if (url.contains("mailto:"))
            {
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

                return true;
            }
            else
            {
                view.loadUrl(url);
                return true;
            }
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon)
        {
            if (progressDialog == null)
            {
                progressDialog = new ProgressDialog(MainActivity1.this);
                progressDialog.setMessage("Loading...");
                progressDialog.show();
            }
        }

        public void onPageFinished(WebView view, String url)
        {
            try
            {
                if (progressDialog.isShowing())
                {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
            }
        }
    }


}