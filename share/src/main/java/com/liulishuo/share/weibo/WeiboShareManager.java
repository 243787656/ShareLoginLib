package com.liulishuo.share.weibo;

import com.liulishuo.share.ShareBlock;
import com.liulishuo.share.base.share.IShareManager;
import com.liulishuo.share.base.share.ShareConstants;
import com.liulishuo.share.base.share.ShareContent;
import com.liulishuo.share.base.share.ShareStateListener;
import com.liulishuo.share.util.ShareUtil;
import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.MusicObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.utils.Utility;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.List;
import java.util.Locale;

/**
 * Created by echo on 5/18/15.
 */
public class WeiboShareManager implements IShareManager {

    public static final int WEIBO_SHARE_TYPE = 0;

    private Activity mActivity;
    
    private String mSinaAppKey;

    private String mRedirectUrl;
    
    private ShareStateListener mShareStateListener;


    /**
     * 微博分享的接口实例
     */
    private IWeiboShareAPI mSinaAPI;

    public WeiboShareManager(Activity activity) {
        mActivity = activity;
        mSinaAppKey = ShareBlock.getInstance().getWeiboAppId();
        mRedirectUrl = ShareBlock.getInstance().getWeiboRedirectUrl();
        
        if (!TextUtils.isEmpty(mSinaAppKey)) {
            // 创建微博 SDK 接口实例
            mSinaAPI = WeiboShareSDK.createWeiboAPI(activity, mSinaAppKey);
            mSinaAPI.registerApp();  // 将应用注册到微博客户端
        }
    }

    private void shareText(ShareContent shareContent) {
        //初始化微博的分享消息
        WeiboMultiMessage weiboMultiMessage = new WeiboMultiMessage();
        weiboMultiMessage.textObject = getTextObj(shareContent.getSummary());
        //初始化从第三方到微博的消息请求
        SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
        request.transaction = ShareUtil.buildTransaction("sinatext");
        request.multiMessage = weiboMultiMessage;
        allInOneShare(mActivity, request);
    }

    private void sharePicture(ShareContent shareContent) {
        WeiboMultiMessage weiboMultiMessage = new WeiboMultiMessage();
        weiboMultiMessage.imageObject = getImageObj(shareContent);
        //初始化从第三方到微博的消息请求
        SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
        request.transaction = ShareUtil.buildTransaction("sinapic");
        request.multiMessage = weiboMultiMessage;
        allInOneShare(mActivity, request);
    }

    private void shareWebPage(ShareContent shareContent) {
        WeiboMultiMessage weiboMultiMessage = new WeiboMultiMessage();
        weiboMultiMessage.textObject = getTextObj(shareContent.getSummary());
        weiboMultiMessage.imageObject = getImageObj(shareContent);
        // 初始化从第三方到微博的消息请求
        SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
        // 用transaction唯一标识一个请求
        request.transaction = ShareUtil.buildTransaction("sinawebpage");
        request.multiMessage = weiboMultiMessage;
        allInOneShare(mActivity, request);
    }

    private void shareMusic(ShareContent shareContent) {
        WeiboMultiMessage weiboMultiMessage = new WeiboMultiMessage();
        weiboMultiMessage.mediaObject = getMusicObj(shareContent);
        //初始化从第三方到微博的消息请求
        SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
        request.transaction = ShareUtil.buildTransaction("sinamusic");
        request.multiMessage = weiboMultiMessage;
        allInOneShare(mActivity, request);
    }

    /**
     * 创建文本消息对象。
     *
     * @return 文本消息对象。
     */
    private TextObject getTextObj(String text) {
        TextObject textObject = new TextObject();
        textObject.text = text;
        return textObject;
    }

    /**
     * 创建图片消息对象。
     *
     * @return 图片消息对象。
     */
    private ImageObject getImageObj(ShareContent content) {
        String imageUrl = content.getImageUrl();
        Bitmap imageBmp = content.getImageBmp();
        Bitmap shareBmp = null;
        if (imageBmp != null) {
            shareBmp = imageBmp;
        }
        
        // 尝试加载本地图片
        if (imageUrl != null) {
            if (imageUrl.indexOf("http") == 0) {
                throw new NullPointerException("imageUrl is not a file path,you should use imageBmp instead of imageUrl");
            }
            shareBmp = BitmapFactory.decodeFile(imageUrl);
            if (shareBmp == null) {
                throw new NullPointerException("imageUrl is not a file path,you should use imageBmp instead of imageUrl");
            }
        }
        
        ImageObject imageObject = new ImageObject();
        imageObject.setImageObject(shareBmp);
        return imageObject;
    }

    /**
     * 创建多媒体（音乐）消息对象。
     *
     * @return 多媒体（音乐）消息对象。
     */
    private MusicObject getMusicObj(ShareContent shareContent) {
        // 创建媒体消息
        MusicObject musicObject = new MusicObject();
        musicObject.identify = Utility.generateGUID();
        musicObject.title = shareContent.getTitle();
        musicObject.description = shareContent.getSummary();

        // 设置 Bitmap 类型的图片到视频对象里
        Bitmap bmp = BitmapFactory.decodeFile(shareContent.getImageUrl());
        musicObject.setThumbImage(bmp);
        musicObject.actionUrl = shareContent.getURL();
        musicObject.dataUrl = mRedirectUrl;
        musicObject.dataHdUrl = mRedirectUrl;
        musicObject.duration = 10;
        musicObject.defaultText = shareContent.getSummary();
        return musicObject;
    }

    private void allInOneShare(final Activity activity, SendMultiMessageToWeiboRequest request) {
        AuthInfo authInfo = new AuthInfo(activity, mSinaAppKey, mRedirectUrl, ShareBlock.getInstance().getWeiboScope());
        Oauth2AccessToken accessToken = AccessTokenKeeper.readAccessToken(activity);
        String token = "";
        if (accessToken != null) {
            token = accessToken.getToken();
        }

        mSinaAPI.sendRequest(activity, request, authInfo, token, new WeiboAuthListener() {

            @Override
            public void onWeiboException(WeiboException arg0) {
                mShareStateListener.onError(arg0.getMessage());
            }

            @Override
            public void onComplete(Bundle bundle) {
                Oauth2AccessToken newToken = Oauth2AccessToken.parseAccessToken(bundle);
                AccessTokenKeeper.writeAccessToken(activity, newToken);
                mShareStateListener.onComplete();
            }

            @Override
            public void onCancel() {
                mShareStateListener.onCancel();
            }
        });
    }

    @Override
    public void share(ShareContent shareContent, int shareType, @NonNull ShareStateListener listener) {
        if (mSinaAPI == null) {
            return;
        }
        mShareStateListener = listener;

        switch (shareContent.getShareWay()) {
            case ShareConstants.SHARE_WAY_TEXT:
                shareText(shareContent);
                break;
            case ShareConstants.SHARE_WAY_PIC:
                sharePicture(shareContent);
                break;
            case ShareConstants.SHARE_WAY_WEBPAGE:
                shareWebPage(shareContent);
                break;
            case ShareConstants.SHARE_WAY_MUSIC:
                shareMusic(shareContent);
                break;
        }
    }

    public static boolean isWeiboInstalled(@NonNull Context context) {
        PackageManager pm;
        if ((pm = context.getApplicationContext().getPackageManager()) == null) {
            return false;
        }
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        for (PackageInfo info : packages) {
            String name = info.packageName.toLowerCase(Locale.ENGLISH);
            if ("com.sina.weibo".equals(name)) {
                return true;
            }
        }
        return false;
    }
}
