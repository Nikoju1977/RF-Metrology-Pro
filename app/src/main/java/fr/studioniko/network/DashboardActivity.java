package fr.studioniko.network;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintManager;
import android.webkit.*;
import android.widget.Toast;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/** Local trusted HTML only. No third-party navigation, no file URL access. */
public final class DashboardActivity extends Activity {
    private static final String PAGE="https://appassets.androidplatform.net/metrology.html";
    private static final int OPEN=401,SAVE=402;
    private WebView web; private ValueCallback<Uri[]> chooser; private byte[] pending;
    @Override public void onCreate(Bundle state){
        super.onCreate(state); web=new WebView(this);setContentView(web);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSupportMultipleWindows(false);s.setGeolocationEnabled(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !PAGE.equals(r.getUrl().toString());}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){String u=r.getUrl().toString();if(PAGE.equals(u)){try{return new WebResourceResponse("text/html","UTF-8",getAssets().open("metrology.html"));}catch(IOException ignored){return denied();}}if("https".equals(r.getUrl().getScheme())&&"speed.cloudflare.com".equals(r.getUrl().getHost())&&!r.isForMainFrame()&&(r.getUrl().getPath().equals("/__up")||r.getUrl().getPath().equals("/__down")))return null;return denied();}
            @Override public void onPageFinished(WebView v,String url){if(PAGE.equals(url))v.evaluateJavascript("window.receiveNativeRadio&&window.receiveNativeRadio("+nativeHistory().toString()+")",null);}
        });
        web.setWebChromeClient(new WebChromeClient(){@Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams p){if(chooser!=null)chooser.onReceiveValue(null);chooser=cb;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(i,OPEN);}catch(Exception e){chooser=null;cb.onReceiveValue(null);}return true;}});
        web.addJavascriptInterface(new Bridge(),"NikoNative");web.loadUrl(PAGE);
    }
    private WebResourceResponse denied(){return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}
    private Object metric(JSONObject o,String key){int x=o.optInt(key,Integer.MAX_VALUE);return x==Integer.MAX_VALUE?JSONObject.NULL:x;}
    private JSONArray nativeHistory(){JSONArray out=new JSONArray();try{JSONArray h=new JSONArray(getSharedPreferences("MainActivity",MODE_PRIVATE).getString("history","[]"));SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.ROOT);f.setTimeZone(TimeZone.getTimeZone("UTC"));for(int i=0;i<h.length();i++){JSONObject a=h.optJSONObject(i);if(a==null)continue;JSONObject r=new JSONObject();r.put("time",f.format(new Date(a.getLong("time"))));r.put("place",a.optString("place"));r.put("rat",a.optString("tech"));r.put("band",a.optString("band"));r.put("operator",a.optString("operator"));r.put("rsrp",metric(a,"rsrp"));r.put("rsrq",metric(a,"rsrq"));r.put("sinr",metric(a,"snr"));r.put("pci",metric(a,"pci"));r.put("source","modem Android · relevé enregistré");out.put(r);}}catch(Exception ignored){}return out;}
    private final class Bridge {
        @JavascriptInterface public void saveDocument(String name,String content,String type){if(name==null||!name.matches("niko-[a-zA-Z0-9._-]{1,90}")||content==null||content.length()>7000000)return;runOnUiThread(()->{if(!PAGE.equals(web.getUrl())||pending!=null)return;pending=content.getBytes(StandardCharsets.UTF_8);String mime="text/csv".equals(type)?type:"application/json";try{startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime).putExtra(Intent.EXTRA_TITLE,name),SAVE);}catch(Exception e){pending=null;toast("Export indisponible");}});}
        @JavascriptInterface public void printReport(){runOnUiThread(()->{if(!PAGE.equals(web.getUrl()))return;PrintManager pm=getSystemService(PrintManager.class);if(pm!=null)pm.print("NIKO Metrology",web.createPrintDocumentAdapter("NIKO Metrology"),null);});}
    }
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);Uri uri=res==RESULT_OK&&data!=null?data.getData():null;if(req==OPEN&&chooser!=null){chooser.onReceiveValue(uri==null?null:new Uri[]{uri});chooser=null;}if(req==SAVE){byte[] bytes=pending;pending=null;if(uri==null||bytes==null)return;try(OutputStream o=getContentResolver().openOutputStream(uri,"wt")){if(o==null)throw new IOException();o.write(bytes);toast("Document enregistré");}catch(Exception e){toast("Échec de l’export");}}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onDestroy(){if(chooser!=null)chooser.onReceiveValue(null);if(web!=null){web.removeJavascriptInterface("NikoNative");web.destroy();}super.onDestroy();}
}
