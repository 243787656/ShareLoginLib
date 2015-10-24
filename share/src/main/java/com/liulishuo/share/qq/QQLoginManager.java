package com.liulishuo.share.qq;

import com.liulishuo.share.ShareBlock;
import com.liulishuo.share.base.login.GetUserListener;
import com.liulishuo.share.base.login.ILoginManager;
import com.liulishuo.share.base.login.LoginListener;
import com.liulishuo.share.base.Constants;
import com.liulishuo.share.util.HttpUtil;
import com.tencent.connect.UserInfo;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.HashMap;

/**
 * Created by echo on 5/19/15.
 */
public class QQLoginManager implements ILoginManager {

    private Activity mActivity;

    private Tencent mTencent;

    private LoginUiListener mLoginUiListener;

    public QQLoginManager(Activity activity) {
        mActivity = activity;
        String appId = ShareBlock.getInstance().QQAppId;
        if (!TextUtils.isEmpty(appId)) {
            mTencent = Tencent.createInstance(appId, activity.getApplicationContext());
        }
    }

    @Override
    public void login(final @NonNull LoginListener loginListener) {
        if (!mTencent.isSessionValid()) {
            mLoginUiListener = new LoginUiListener(loginListener);
            mTencent.login(mActivity, ShareBlock.getInstance().QQScope, mLoginUiListener);
        } else {
            mTencent.logout(mActivity);
        }
    }

    private class LoginUiListener implements IUiListener {

        final private LoginListener mLoginListener;

        private LoginUiListener(LoginListener loginListener) {
            mLoginListener = loginListener;
        }

        @Override
        public void onComplete(Object object) {
            JSONObject jsonObject = (JSONObject) object; // qq_json
            initOpenidAndToken(jsonObject); // 初始化id和access token
            mLoginListener.onSuccess(mTencent.getOpenId(), mTencent.getAccessToken(), mTencent.getExpiresIn(), jsonObject.toString());
        }

        @Override
        public void onError(UiError uiError) {
            mLoginListener.onError(uiError.errorCode + " - " + uiError.errorMessage + " - " + uiError.errorDetail);
        }

        @Override
        public void onCancel() {
            mLoginListener.onCancel();
        }
    }

    private void initOpenidAndToken(@NonNull JSONObject jsonObject) {
        try {
            String openId = jsonObject.getString(com.tencent.connect.common.Constants.PARAM_OPEN_ID);
            String token = jsonObject.getString(com.tencent.connect.common.Constants.PARAM_ACCESS_TOKEN);
            String expires = jsonObject.getString(com.tencent.connect.common.Constants.PARAM_EXPIRES_IN);
            if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expires) && !TextUtils.isEmpty(openId)) {
                mTencent.setAccessToken(token, expires);
                mTencent.setOpenId(openId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getUserInfo(final @NonNull GetUserListener listener) {
        UserInfo info = new UserInfo(mActivity, mTencent.getQQToken());
        // 执行获取用户信息的操作
        info.getUserInfo(new IUiListener() {
            @Override
            public void onComplete(Object object) {
                try {
                    JSONObject jsonObject = (JSONObject) object;
                    HashMap<String, String> userInfoHashMap = new HashMap<>();
                    userInfoHashMap.put(Constants.PARAMS_NICK_NAME, jsonObject.getString("nickname"));
                    userInfoHashMap.put(Constants.PARAMS_SEX, jsonObject.getString("gender"));
                    userInfoHashMap.put(Constants.PARAMS_IMAGEURL, jsonObject.getString("figureurl_qq_2"));
                    userInfoHashMap.put(Constants.PARAMS_USERID, mTencent.getOpenId());
                    listener.onComplete(userInfoHashMap);
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.onError("userInfo data error");
                }
            }

            @Override
            public void onError(UiError uiError) {
                listener.onError(uiError.errorCode + " - " + uiError.errorMessage + " - " + uiError.errorDetail);
            }

            @Override
            public void onCancel() {
                listener.onCancel();
            }
        });
    }

    /**
     * @see "http://wiki.open.qq.com/wiki/website/get_simple_userinfo"
     */
    @Override
    public void getUserInfo(final String accessToken, final String uid, final @NonNull GetUserListener listener) {
        StringBuilder builder = new StringBuilder()
                .append("https://graph.qq.com/user/get_simple_userinfo")
                .append("?access_token=").append(accessToken)
                .append("&oauth_consumer_key=").append(ShareBlock.getInstance().QQAppId)
                .append("&openid=").append(uid)
                .append("&format=json");
        
        HttpUtil.doGetAsyn(builder.toString(), new HttpUtil.CallBack() {
            @Override
            public void onRequestComplete(String result) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    HashMap<String, String> userInfoHashMap = new HashMap<>();
                    userInfoHashMap.put(Constants.PARAMS_NICK_NAME, jsonObject.getString("nickname"));
                    userInfoHashMap.put(Constants.PARAMS_SEX, jsonObject.getString("gender"));
                    userInfoHashMap.put(Constants.PARAMS_IMAGEURL, jsonObject.getString("figureurl_qq_1"));
                    userInfoHashMap.put(Constants.PARAMS_USERID, uid);

                    listener.onComplete(userInfoHashMap);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onError("user data parse error");
                }
            }

            @Override
            public void onError() {
                listener.onError("get user data error : {network error}");
            }
        });
    }

    public void handlerOnActivityResult(int requestCode, int resultCode, Intent data) {
        if (mLoginUiListener != null) {
            Tencent.onActivityResultData(requestCode, resultCode, data, mLoginUiListener);
        }
    }

    public Tencent getTencent() {
        return mTencent;
    }
}


