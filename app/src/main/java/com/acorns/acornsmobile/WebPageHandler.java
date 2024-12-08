package com.acorns.acornsmobile;

import android.annotation.SuppressLint;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("SetJavaScriptEnabled")
public class WebPageHandler
{
	protected WebView view;
	protected AppCompatActivity activity;
	
	private String lessonFolder;
	
	WebPageHandler(AppCompatActivity activity, WebView view)
	{
		this.view = view;
		this.activity = activity;

        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setLoadWithOverviewMode(true);
		view.setInitialScale(1);
		view.getSettings().setSupportZoom(true);
		view.getSettings().setBuiltInZoomControls(true);
		view.getSettings().setDisplayZoomControls(false);
        view.getSettings().setUseWideViewPort(true);
        
        if (! (activity instanceof MainActivity) )
            view.setWebChromeClient(new WebChromeClient());
	}

	@SuppressWarnings("unused")
	public static void enableJavaScript(boolean enable, WebView view)
	{
		view.getSettings().setJavaScriptEnabled(true);
	}
	
	void showPage(final String url) {
		Runnable runTask = () ->
		{
			lessonFolder = url.substring(0, url.lastIndexOf("/"));
			if (lessonFolder.startsWith("file://")) lessonFolder = lessonFolder.substring(7);

			view.addJavascriptInterface(new JSInterface(activity, lessonFolder), "audioTools");
			view.getSettings().setAllowFileAccess(true);
			String oldUrl = view.getUrl();
			if (url.equals(oldUrl)) return;
			view.loadUrl(url);
		};

		if (url != null)
			activity.runOnUiThread(runTask);
	}
}
