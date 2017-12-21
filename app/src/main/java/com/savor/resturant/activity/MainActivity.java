package com.savor.resturant.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.common.api.http.callback.FileDownProgress;
import com.common.api.utils.AppUtils;
import com.common.api.utils.DensityUtil;
import com.common.api.utils.LogUtils;
import com.common.api.utils.ShowMessage;
import com.savor.resturant.R;
import com.savor.resturant.adapter.CategoryAdapter;
import com.savor.resturant.adapter.FunctionAdapter;
import com.savor.resturant.bean.FunctionItem;
import com.savor.resturant.bean.HotelBean;
import com.savor.resturant.bean.TvBoxSSDPInfo;
import com.savor.resturant.bean.UpgradeInfo;
import com.savor.resturant.core.AppApi;
import com.savor.resturant.presenter.SensePresenter;
import com.savor.resturant.service.ClearImageCacheService;
import com.savor.resturant.service.LocalJettyService;
import com.savor.resturant.service.SSDPService;
import com.savor.resturant.utils.ActivitiesManager;
import com.savor.resturant.utils.GlideImageLoader;
import com.savor.resturant.utils.STIDUtil;
import com.savor.resturant.utils.WifiUtil;
import com.savor.resturant.widget.HotsDialog;
import com.savor.resturant.widget.SavorDialog;
import com.savor.resturant.widget.UpgradeDialog;
import com.savor.resturant.widget.decoration.SpacesItemDecoration;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页功能操作列表
 */
public class MainActivity extends BaseActivity implements View.OnClickListener, IBindTvView, FunctionAdapter.OnNoHotelClickListener {
    public static final String SMALL_PLATFORM = "small_platform";
    /**退出投屏按钮状态，退出投屏*/
    private static final int TYPE_GO_SETTINGS = 0x1;
    /**退出投屏按钮状态，去设置*/
    private static final int TYPE_STOP_PRO = 0x2;
    /**退出投屏*/
    private static final int TYPE_LINK_TV = 0x3;
    private static final int CHECK_LINK_STATUS = 1;
    public static final String ACTION_TV_LINK = "action_tv_link";
    private TextView tv_center;
    private ImageView iv_left;
    private RecyclerView listView;
    private TextView connectTipTV;
    private TextView operationBtnTV;
    private CategoryAdapter categoryAdapter=null;
    private List<FunctionItem> mList = new ArrayList<>();
//    private BindTvPresenter mBindTvPresenter;
    private UpgradeInfo upGradeInfo;
    private UpgradeDialog mUpgradeDialog;
    private NotificationManager manager;
    private Notification notif;
    private final int NOTIFY_DOWNLOAD_FILE=10001;
    /**是否是自动检查更新，如果不是那就是手动检查提示有版本更新否则不提示*/
    private boolean ismuteUp = true;
    /**退出投屏按钮状态，当前展示退出投屏还是去设置*/
    private int mProBtnType;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            }
        }
    };
    private SmallPlatformReciver smallPlatformReciver;
    private SavorDialog mQrcodeDialog;
    private long exitTime;
    private TextView mHintTv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getViews();
        setViews();
        setListeners();
        regitsterSmallPlatformReciever();
//        upgrade();
    }

    /**
     * 注册小平台发现广播
     */
    public void regitsterSmallPlatformReciever() {
        smallPlatformReciver = new SmallPlatformReciver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SMALL_PLATFORM);
        mContext.registerReceiver(smallPlatformReciver,filter);
    }
    @Override
    public void reLink() {
    }

    @Override
    public void getViews() {
        mHintTv = (TextView) findViewById(R.id.tv_wifi_hint);
        iv_left = (ImageView) findViewById(R.id.iv_left);
        tv_center = (TextView)findViewById(R.id.tv_center);
        listView = (RecyclerView) findViewById(R.id.category_list);
        connectTipTV = (TextView)findViewById(R.id.connect_tip);
        operationBtnTV = (TextView)findViewById(R.id.operation_btn);
    }

    @Override
    public void setViews() {
        iv_left.setVisibility(View.GONE);
        tv_center.setText(getString(R.string.app_name));

        mList.clear();

        FunctionItem recommandItem = new FunctionItem();
        recommandItem.setContent("推荐菜");
        recommandItem.setResId(R.drawable.ico_recommand);
        recommandItem.setType(FunctionItem.FunctionType.TYPE_RECOMMAND_FOODS);
        mList.add(recommandItem);

        FunctionItem advertItem = new FunctionItem();
        advertItem.setContent("宣传片");
        advertItem.setResId(R.drawable.ico_xcp);
        advertItem.setType(FunctionItem.FunctionType.TYPE_ADVERT);
        mList.add(advertItem);

        FunctionItem welcomeItem = new FunctionItem();
        welcomeItem.setContent("欢迎词");
        welcomeItem.setResId(R.drawable.ico_welcom_word);
        welcomeItem.setType(FunctionItem.FunctionType.TYPE_WELCOME_WORD);
        mList.add(welcomeItem);

        FunctionItem picItem = new FunctionItem();
        picItem.setContent("照片");
        picItem.setResId(R.drawable.ico_pic);
        picItem.setType(FunctionItem.FunctionType.TYPE_PIC);
        mList.add(picItem);

        FunctionItem videoItem = new FunctionItem();
        videoItem.setContent("视频");
        videoItem.setResId(R.drawable.ico_video);
        videoItem.setType(FunctionItem.FunctionType.TYPE_VIDEO);
        mList.add(videoItem);

        FunctionAdapter mFunctionAdapter = new FunctionAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(this,2);
        layoutManager.setOrientation(GridLayoutManager.VERTICAL);
        listView.setLayoutManager(layoutManager);
        mFunctionAdapter.setData(mList);
        listView.setAdapter(mFunctionAdapter);
        mFunctionAdapter.setOnNoHotelClickListener(this);

        //添加ItemDecoration，item之间的间隔
        int leftRight = DensityUtil.dip2px(this,5);
        int topBottom = DensityUtil.dip2px(this,15);

        listView.addItemDecoration(new SpacesItemDecoration(leftRight, topBottom, getResources().getColor(R.color.color_eeeeee)));

        initWIfiHint();

    }


    @Override
    protected void onRestart() {
        super.onRestart();
        initWIfiHint();
    }

    public void initWIfiHint() {
        // 判断当前是否是酒店环境
        int hotelid = mSession.getHotelid();
        if(hotelid>0) {
            mHintTv.setText("当前连接酒楼权茂北京菜");
            mHintTv.setTextColor(getResources().getColor(R.color.color_0da606));
            mHintTv.setCompoundDrawables(null,null,null,null);
        }else {
            mHintTv.setText("请链接权茂北京菜的wifi后继续操作");
            mHintTv.setTextColor(getResources().getColor(R.color.color_e43018));
            mHintTv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ico_exe_hint),null,null,null);

        }
    }

    @Override
    public void setListeners() {
        operationBtnTV.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
        }
    }


    int msg = 0;
    @Override
    public void onSuccess(AppApi.Action method, Object obj) {
        switch (method) {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onError(AppApi.Action method, Object obj) {

        switch (method) {
            case POST_NOTIFY_TVBOX_STOP_JSON:
                if(obj == AppApi.ERROR_TIMEOUT) {
                    closeLinkingDialog();
                    ShowMessage.showToast(this,"网络超时，请重试");
                    return;
                }else if(obj == AppApi.ERROR_NETWORK_FAILED) {
                    closeLinkingDialog();
                    ShowMessage.showToast(this,"网络已断开，请检查");
                    return;
                }
                closeLinkingDialog();
                if(obj instanceof String ) {
                    String msg = (String) obj;
                    showToast(msg);
                }else {
                    String wifiName = WifiUtil.getWifiName(this);
                    ShowMessage.showToast(this,wifiName+"包间没有投屏");
                }
                break;
        }
    }

    @Override
    public void readyForQrcode() {
        if(mQrcodeDialog==null)
            mQrcodeDialog = new SavorDialog(this);
        mQrcodeDialog.show();
    }

    @Override
    public void closeQrcodeDialog() {
        if (mQrcodeDialog != null) {
            mQrcodeDialog.dismiss();
            mQrcodeDialog = null;
        }
    }

    @Override
    public void startLinkTv() {
        Intent intent = new Intent(this,LinkTvActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivityForResult(intent,0);
    }

    @Override
    public void onNoHotelClick() {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        mHintTv.startAnimation(shake);
    }

    public class SmallPlatformReciver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SensePresenter.SMALL_PLATFORM)) {
                LogUtils.d("savor:ssdp 收到小平台接受广播");
                initWIfiHint();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    ShowMessage.showToast(this,getString(R.string.confirm_exit_app));
                    exitTime = System.currentTimeMillis();
                } else {
                    exitApp();
                }
        }
        return true;
    }

    private void exitApp() {
        // 清楚图片内存缓存
        GlideImageLoader.getInstance().clearMemory(getApplicationContext());
        // 清楚activity任务栈
        ActivitiesManager.getInstance().popAllActivities();

        // 关闭jetty服务
        Intent stopIntent = new Intent(this,LocalJettyService.class);
        stopService(stopIntent);

        // 关闭发现小平台的service
        Intent stopDescoveryIntent = new Intent(this,SSDPService.class);
        stopService(stopDescoveryIntent);

        Intent intent = new Intent(this, ClearImageCacheService.class);
        intent.putExtra("path",mSession.getCompressPath());
        startService(intent);


        Process.killProcess(android.os.Process.myPid());

    }
}
