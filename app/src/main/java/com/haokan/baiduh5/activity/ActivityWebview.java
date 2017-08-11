package com.haokan.baiduh5.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mobad.feeds.BaiduNative;
import com.baidu.mobad.feeds.NativeErrorCode;
import com.baidu.mobad.feeds.NativeResponse;
import com.baidu.mobad.feeds.RequestParameters;
import com.bumptech.glide.Glide;
import com.haokan.baiduh5.R;
import com.haokan.baiduh5.util.CommonUtil;
import com.haokan.baiduh5.util.LogHelper;
import com.haokan.baiduh5.util.StatusBarUtil;
import com.haokan.baiduh5.util.ToastManager;

import java.util.List;

public class ActivityWebview extends ActivityBase implements View.OnClickListener {
    public static final String KEY_INTENT_WEB_URL = "url";
    private TextView mTitle;
    private ProgressBar mProgressHorizontal;
    private WebView mWebView;

    //分享用到的内容
    private String mWeb_Url;
    private Handler mHandler = new Handler();
    private View mTvClose;
    private RelativeLayout mAdWraper;
    private RelativeLayout mAdWraper1;
    private RelativeLayout mAdWraper2;
    private ImageView mAdimage;
    private TextView mAdTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        StatusBarUtil.setStatusBarBgColor(this, R.color.colorMainStatus);

        assignViews();
        loadData();
    }

    /**
     * 如果给的链接不是http或者https，默认认为是打开本地应用的activity
     */
    private void loadLocalApp() {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri content_url = Uri.parse(mWeb_Url);
            intent.setData(content_url);
            startActivity(intent);
        }catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        if (getIntent().getData() != null) {
            Uri uri = getIntent().getData();
            String url = uri.getQueryParameter(KEY_INTENT_WEB_URL);
            mWeb_Url = url;
//            try {
//                mWeb_Url = "http://m.levect.com/appcpudetail.html?url=" + URLEncoder.encode(url, "UTF-8");
//            } catch (Exception e) {
//                e.printStackTrace();
//                mWeb_Url = url;
//            }
        } else {
            mWeb_Url = getIntent().getStringExtra(KEY_INTENT_WEB_URL);
        }
        if (TextUtils.isEmpty(mWeb_Url)) {
            ToastManager.showShort(this, R.string.url_error);
            finish();
            return;
        }

        LogHelper.i("WebViewActivity", "loadData mweburl = " + mWeb_Url);

        if (mWeb_Url.startsWith("www")) {
            mWeb_Url = "http://" + mWeb_Url;
        }
        if (mWeb_Url.startsWith("http") || mWeb_Url.startsWith("https")) {
            mProgressHorizontal.setVisibility(View.VISIBLE);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl(mWeb_Url);
                }
            }, 300);
        } else {
            loadLocalApp();
        }

        if (mWeb_Url.contains("image?")) {
            mAdWraper = mAdWraper1;
        } else {
            mAdWraper = mAdWraper2;
        }
        mAdimage = (ImageView) mAdWraper.findViewById(R.id.image);
        mAdTitle = (TextView) mAdWraper.findViewById(R.id.titlead);

        loadBaiduAd2(true);
    }

    private void loadBaiduAd2(final boolean first) {
        if (mIsDestory || mCloadAd) {
            return;
        }

        /**
         * Step 1. 创建 BaiduNative 对象，参数分别为：
         * 上下文 context，广告位 ID，BaiduNativeNetworkListener 监听（监听广告请求的成功与失
         败）
         *  注意：请将 YOUR_AD_PALCE_ID  替换为自己的代码位 ID ，不填写无法请求到广告
         */
        BaiduNative baidu = new BaiduNative(this, "4655660",
                new BaiduNative.BaiduNativeNetworkListener() {
                @Override
                public void onNativeFail(NativeErrorCode arg0) {
                    LogHelper.d("ListViewActivity", "onNativeFail reason:" + arg0.name());
                    if (first) {
                        mAdWraper.setVisibility(View.GONE);
                    }
                }
                @Override
                public void onNativeLoad(List<NativeResponse> arg0) {
                    if (arg0 != null && arg0.size() > 0) {
                        mAdWraper.setVisibility(View.VISIBLE);
                        final NativeResponse nativeResponse = arg0.get(0);
                        String title = nativeResponse.getTitle();
                        mAdTitle.setText(title);
                        String iconUrl = nativeResponse.getIconUrl();
                        String adLogoUrl = nativeResponse.getAdLogoUrl();
                        String imageUrl = nativeResponse.getImageUrl();
                        LogHelper.d("ListViewActivity", "iconUrl = " + iconUrl + ", adLogoUrl = " + adLogoUrl
                                + ", imageUrl = " + imageUrl);

                        Glide.with(ActivityWebview.this).load(imageUrl).into(mAdimage);

                        nativeResponse.recordImpression(mAdWraper);//  警告：调用该函数来发送展现，勿漏！
                        mAdWraper.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                nativeResponse.handleClick(view);//  点击响应
                            }
                        });
                    } else {
                        if (first) {
                            mAdWraper.setVisibility(View.GONE);
                        }
                    }
                }
            });

        /**
         * Step 2. 创建requestParameters对象，并将其传给baidu.makeRequest来请求广告
         */
        RequestParameters requestParameters = new RequestParameters.Builder()
                .downloadAppConfirmPolicy(RequestParameters.DOWNLOAD_APP_CONFIRM_ALWAYS)
                .build();
        baidu.makeRequest(requestParameters);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LogHelper.i("WebViewActivity", "onAd 重新请求了");
                loadBaiduAd2(false);
            }
        }, 10000);
    }

    boolean mCloadAd = false;

//    private void loadBaiduAd(final View oldAdView) {
//        if (mIsDestory || mCloadAd) {
//            return;
//        }
//
//        mAdWraper.setVisibility(View.VISIBLE);
//        //百度信息流广告begin
//        /**
//         * Step 1. 在准备数据时，在listview广告位置创建BaiduNativeAdPlacement对象，并加入自己的数据列
//         表中
//         *  注意：请将YOUR_AD_PALCE_ID 替换为自己的广告位ID
//         */
//        BaiduNativeAdPlacement placement = new BaiduNativeAdPlacement();
//        placement.setApId("4634448");
//
//        final BaiduNativeH5AdView newAdView = BaiduNativeH5AdViewManager.getInstance()
//                .getBaiduNativeH5AdView(this, placement, R.color.bai);
//        if (newAdView.getParent() != null) {
//            ((ViewGroup) newAdView.getParent()).removeView(newAdView);
//        }
//        newAdView.setEventListener(new BaiduNativeH5AdView.BaiduNativeH5EventListner() {
//            @Override
//            public void onAdClick() {
//            }
//
//            @Override
//            public void onAdFail(String arg0) {
//                LogHelper.i("WebViewActivity", "onAdFail arg0 = " + arg0);
//                if (oldAdView == null) {
//                    mAdWraper.setVisibility(View.GONE);
//                }
//            }
//
//            @Override
//            public void onAdShow() {
//                LogHelper.i("WebViewActivity", "onAdShow");
//                if (oldAdView != null) {
//                    mAdWraper.removeView(oldAdView);
//                }
//            }
//
//            @Override
//            public void onAdDataLoaded() {
//                LogHelper.i("WebViewActivity", "onAdDataLoaded");
//            }
//        });
//
//        int width = getResources().getDisplayMetrics().widthPixels;
//        int height = width>>2;
//        RelativeLayout.LayoutParams rllp = new RelativeLayout.LayoutParams(width, height);
//        newAdView.setLayoutParams(rllp);
//
//        final RequestParameters requestParameters =
//                new RequestParameters.Builder().setWidth(width).setHeight(height).build();
//        newAdView.makeRequest(requestParameters);
//        mAdWraper.addView(newAdView, 0);
//
//
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                LogHelper.i("WebViewActivity", "onAd 重新请求了");
//                loadBaiduAd(newAdView);
//            }
//        }, 5000);
//        //百度信息流广告end
//    }

    private void assignViews() {
        ImageView back = (ImageView) findViewById(R.id.back);
        back.setOnClickListener(this);

        mTvClose = findViewById(R.id.close);
        mTvClose.setOnClickListener(this);

        mAdWraper1 = (RelativeLayout) findViewById(R.id.adwrapper1);
        mAdWraper2 = (RelativeLayout) findViewById(R.id.adwrapper2);

        mAdWraper1.findViewById(R.id.ad_close).setOnClickListener(this);
        mAdWraper2.findViewById(R.id.ad_close).setOnClickListener(this);

        mTitle = (TextView) findViewById(R.id.title);

        mProgressHorizontal = (ProgressBar) findViewById(R.id.progress_horizontal);
        mWebView = (WebView) findViewById(R.id.webView);
        initWebView();
    }

    private void initWebView() {
        mWebView.setHorizontalScrollBarEnabled(false);//水平不显示
        mWebView.setVerticalScrollBarEnabled(false); //垂直不显示

        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAppCacheEnabled(true);
//        settings.setAppCachePath(CacheManager.getWebViewAppCacheDir(getApplicationContext()).getAbsolutePath());
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAppCacheMaxSize(1024 * 1024 * 100);
        settings.setAllowFileAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setGeolocationEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);
        }

        mWebView.setDownloadListener(new DownloadListener() {//实现文件下载功能
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            //点击链接在此webView打开
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                LogHelper.i("WebViewActivity", "shouldOverrideUrlLoading mweburl = " + url);
                mWeb_Url = url;
                if (url.startsWith("http") || url.startsWith("https")) {
                    return false;
                } else {
                    loadLocalApp();
                    return true;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                LogHelper.i("WebViewActivity", "onPageStarted mweburl = " + url);
//                showLoadingLayout();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                LogHelper.i("WebViewActivity", "onPageFinished mweburl = " + url);
                String title = mWebView.getTitle();
                if (!TextUtils.isEmpty(title)) {
                    mTitle.setText(title);
                } else {
                    mTitle.setText("");
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url.contains("pos.baidu.com")) {
                    return new WebResourceResponse(null, null, null);
                }
                LogHelper.i("WebViewActivity", "shouldInterceptRequest mweburl = " + url);
                return super.shouldInterceptRequest(view, url);
            }

            //可以加载https
            @Override
            public void onReceivedSslError(WebView view, @NonNull SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress > 0 && newProgress < 90) {
                    mProgressHorizontal.setVisibility(View.VISIBLE);
                    mProgressHorizontal.setProgress(newProgress);
                } else {
                    mProgressHorizontal.setVisibility(View.GONE);
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (CommonUtil.isQuickClick()) {
            return;
        }
        int id = v.getId();
        switch (id) {
            case R.id.back:
                onBackPressed();
                break;
            case R.id.close:
                finish();
                overridePendingTransition(R.anim.activity_in_left2right, R.anim.activity_out_left2right);
                break;
            case R.id.ad_close:
                mCloadAd = true;
                if (mAdWraper != null) {
                    mAdWraper.setVisibility(View.GONE);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.activity_in_left2right, R.anim.activity_out_left2right);
        }
    }

    @Override
    public void finish() {
        LogHelper.i("WebViewActivity", "finish ----");
        //出现缩放放大按钮时切换activity会内存泄露，必须先移除所有view
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                ViewGroup view = (ViewGroup) getWindow().getDecorView();
//                view.removeAllViews();
//            }
//        }, 300);
        super.finish();
        overridePendingTransition(R.anim.activity_in_left2right, R.anim.activity_out_left2right);
    }

    @Override
    protected void onDestroy() {
        if(mWebView!=null){
            mWebView.destroy();
            mWebView.removeAllViews();
            mWebView = null;
        }
        super.onDestroy();
//        System.exit(0);
    }
}
