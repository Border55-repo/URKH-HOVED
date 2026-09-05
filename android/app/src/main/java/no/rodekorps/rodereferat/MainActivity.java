package no.rodekorps.rodereferat;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.*;
import android.widget.Toast;
import java.io.*;

public class MainActivity extends Activity {
    private WebView web;
    private PermissionRequest webPermission;
    private ValueCallback<Uri[]> fileChooser;
    private static final int AUDIO_PERMISSION = 7;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        web.addJavascriptInterface(new AndroidBridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if ("border55-repo.github.io".equals(url.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, url));
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    webPermission = request;
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
                } else request.grant(request.getResources());
            }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                fileChooser = callback;
                startActivityForResult(params.createIntent(), 8);
                return true;
            }
        });
        web.loadUrl("https://border55-repo.github.io/URKH-HOVED/");
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == AUDIO_PERMISSION && webPermission != null) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) webPermission.grant(webPermission.getResources());
            else webPermission.deny();
            webPermission = null;
        }
    }


    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == 8 && fileChooser != null) {
            fileChooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            fileChooser = null;
        }
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    public class AndroidBridge {
        @JavascriptInterface public void printPage() {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager)getSystemService(PRINT_SERVICE);
                pm.print("URKH", web.createPrintDocumentAdapter("URKH"), new PrintAttributes.Builder().build());
            });
        }

        @JavascriptInterface public void saveBase64(String dataUrl, String fileName) {
            try {
                String raw = dataUrl.substring(dataUrl.indexOf(',') + 1);
                byte[] bytes = Base64.decode(raw, Base64.DEFAULT);
                String safeName = fileName.replaceAll("[^a-zA-Z0-9æøåÆØÅ._-]", "_");
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, safeName);
                values.put(MediaStore.Downloads.MIME_TYPE, dataUrl.substring(5, dataUrl.indexOf(';')));
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/URKH");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                try (OutputStream stream = getContentResolver().openOutputStream(uri)) { stream.write(bytes); }
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Lagret i Nedlastinger/URKH: " + safeName, Toast.LENGTH_LONG).show());
            } catch (Exception ex) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Filen kunne ikke lagres.", Toast.LENGTH_LONG).show());
            }
        }
    }
}
