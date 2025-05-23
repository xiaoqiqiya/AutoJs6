package org.autojs.autojs.ui.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.autojs.autojs.app.OnActivityResultDelegate;
import org.autojs.autojs.tool.ImageSelector;
import org.autojs.autojs6.R;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by Stardust on Aug 22, 2017.
 */
public class EWebView extends FrameLayout implements SwipeRefreshLayout.OnRefreshListener, OnActivityResultDelegate {

    private static final List<String> IMAGE_TYPES = Arrays.asList("png", "jpg", "bmp");

    private NestedWebView mWebView;
    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public EWebView(Context context) {
        super(context);
        init();
    }

    public EWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SwipeRefreshLayout getSwipeRefreshLayout() {
        return mSwipeRefreshLayout;
    }

    private void init() {
        inflate(getContext(), R.layout.ewebview, this);

        mWebView = findViewById(R.id.web_view);
        setUpWebView();

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mProgressBar = findViewById(R.id.progress_bar);
    }

    private void setUpWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setDisplayZoomControls(false);
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.setWebChromeClient(new MyWebChromeClient());
    }

    public NestedWebView getWebView() {
        return mWebView;
    }

    public void evalJavaScript(String script) {
        mWebView.evaluateJavascript(script, null);
    }

    @SuppressLint("CheckResult")
    @Override
    public void onRefresh() {
        mWebView.reload();
        Observable.timer(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(t -> mSwipeRefreshLayout.setRefreshing(false));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* Empty body. */
    }

    protected class MyWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            mProgressBar.setProgress(newProgress);
        }

        // For Android  >= 4.1
        public void openFileChooser(ValueCallback<Uri> valueCallback,
                                    String acceptType, String capture) {
            if (acceptType == null) {
                openFileChooser(valueCallback, null);
            } else {
                openFileChooser(valueCallback, acceptType.split(","));
            }
        }

        public boolean openFileChooser(ValueCallback<Uri> valueCallback,
                                       String[] acceptType) {
            if (getContext() instanceof OnActivityResultDelegate.DelegateHost &&
                    getContext() instanceof Activity && isImageType(acceptType)) {
                chooseImage(valueCallback);
                return true;
            }
            return false;
        }

        @Override
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback,
                                         WebChromeClient.FileChooserParams fileChooserParams) {
            openFileChooser(value -> {
                if (value == null) {
                    filePathCallback.onReceiveValue(null);
                } else {
                    filePathCallback.onReceiveValue(new Uri[]{value});
                }
            }, fileChooserParams.getAcceptTypes());
            return true;
        }

    }

    private void chooseImage(ValueCallback<Uri> valueCallback) {
        DelegateHost delegateHost = ((OnActivityResultDelegate.DelegateHost) getContext());
        Mediator mediator = delegateHost.getOnActivityResultDelegateMediator();
        Activity activity = (Activity) getContext();
        new ImageSelector(activity, mediator, (selector, uri) -> valueCallback.onReceiveValue(uri))
                .disposable()
                .select();
    }

    private boolean isImageType(String[] acceptTypes) {
        if (acceptTypes == null) {
            return false;
        }
        for (String acceptType : acceptTypes) {
            for (String imageType : IMAGE_TYPES) {
                if (acceptType.contains(imageType)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mProgressBar.setProgress(0);
            mProgressBar.setVisibility(VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mProgressBar.setVisibility(GONE);
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return shouldOverrideUrlLoading(view, request.getUrl().toString());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) {
                view.loadUrl(url);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                List<ResolveInfo> intentActivities = getContext().getPackageManager().queryIntentActivities(intent, 0);
                if (intentActivities.isEmpty()) {
                    return false;
                }
                try {
                    getContext().startActivity(Intent.createChooser(intent, getResources().getString(R.string.text_open_with)));
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        }

    }

}
