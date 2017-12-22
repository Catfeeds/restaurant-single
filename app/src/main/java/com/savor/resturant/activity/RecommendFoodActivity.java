package com.savor.resturant.activity;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.common.api.okhttp.OkHttpUtils;
import com.common.api.utils.AppUtils;
import com.common.api.utils.DensityUtil;
import com.common.api.utils.ShowMessage;
import com.savor.resturant.R;
import com.savor.resturant.adapter.RecommendFoodAdapter;
import com.savor.resturant.adapter.RoomListAdapter;
import com.savor.resturant.bean.AdvertProHistory;
import com.savor.resturant.bean.HotelBean;
import com.savor.resturant.bean.RecommendFoodAdvert;
import com.savor.resturant.bean.RecommendProHistory;
import com.savor.resturant.bean.RoomInfo;
import com.savor.resturant.bean.SmallPlatInfoBySSDP;
import com.savor.resturant.bean.SmallPlatformByGetIp;
import com.savor.resturant.bean.TvBoxSSDPInfo;
import com.savor.resturant.core.AppApi;
import com.savor.resturant.core.ResponseErrorMessage;
import com.savor.resturant.widget.LoadingDialog;
import com.savor.resturant.widget.decoration.SpacesItemDecoration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 推荐菜和宣传片共用页面
 * @author hezd
 */
public class RecommendFoodActivity extends BaseActivity implements View.OnClickListener, RecommendFoodAdapter.OnCheckStateChangeListener, RecommendFoodAdapter.OnSingleProBtnClickListener, RoomListAdapter.OnRoomItemClicklistener {
    /**单张投屏*/
    private static final int TYPE_PRO_SINGLE = 1;
    /**多张投屏*/
    private static final int TYPE_PRO_MULTI = 2;
    /**当前投屏类型*/
    private int currentProType = 0;
    private static final int COLUMN_COUNT = 2;
    private ImageView mBackBtn;
    private TextView mTitleTv;
    private TextView mProBtn;
    private RecyclerView mRecommendFoodsRlv;
    /**当前是否属于选择包间状态*/
    private boolean isSelectRommState;
    private RecyclerView mRoomListView;
    private RecommendFoodAdapter mRecommendAdapter;
    private OperationType currentType;
    private RoomListAdapter roomListAdapter;
    private RoomInfo currentRoom;
    /**单个投屏的条目*/
    private RecommendFoodAdvert currentFoodAdvert;
    private LoadingDialog mLoadingDialog;
    /**投屏发送3个请求如果错误3次认为请求失败*/
    private int erroCount;
    private ConstraintLayout mEmptyLayout;
    private TextView mEmptyHintTv;
    private List<RoomInfo> roomList;
    /**
     * 推荐菜名称列表
     */
    private static final List<String> recommendNameList = new ArrayList<String>() {
        {
            add("鲍汁扣海参");
            add("石锅海鲜拼");
            add("海鲜拼盘");
            add("江南熟醉大闸蟹");
            add("京葱山药烧海参");
            add("海鲜刺身拼盘");
            add("挪威三文鱼");
            add("功夫汤");
        }
    };

    /**
     * 推荐菜图片资源列表
     */
    private static final List<Integer> recommendResIdList = new ArrayList<Integer>() {
                {
                    add(R.drawable.baozhikouhaishen);
                    add(R.drawable.shiguohaixianpin);
                    add(R.drawable.haixianpinpan);
                    add(R.drawable.jiangnanshuzuidazhaxie);
                    add(R.drawable.jingcongshanyaoshaohaishen);
                    add(R.drawable.haixiancishenpinpan);
                    add(R.drawable.nuoweisanwenyu);
                    add(R.drawable.gongfutang);
                }
            };

    /**
     * 宣传片名称列表
     */
    private static final List<String> advertNameList = new ArrayList<String>() {
        {
            add("花家怡园");
            add("淮扬府");
            add("权茂北京菜");
            add("唐宫海鲜舫");
            add("新荣记");
            add("中发源");
        }
    };

    /**
     * 宣传片图片资源列表
     */
    private static final List<Integer> advertResIdList = new ArrayList<Integer>() {
        {
            add(R.drawable.huajiayiyuan);
            add(R.drawable.huaiyangfu);
            add(R.drawable.quanmaobeijingcai);
            add(R.drawable.tanggong);
            add(R.drawable.xinrongji);
            add(R.drawable.zhongfayuan);
        }
    };

    /**
     * 操作类型，宣传片或者推荐菜
     */
    public enum OperationType implements Serializable{
        /**宣传片*/
        TYPE_ADVERT,
        /**推荐菜*/
        TYPE_RECOMMEND_FOODS,
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend_food);

        handleIntent();
        getViews();
        setViews();
        setListeners();

//        getData();
    }

    private void handleIntent() {
        currentType = (OperationType) getIntent().getSerializableExtra("type");
    }

    private void getData() {
        showLoadingLayout();
        int hotelid = mSession.getHotelid();
        switch (currentType) {
            case TYPE_ADVERT:
                AppApi.getAdvertList(this,hotelid+"",this);
                break;
            case TYPE_RECOMMEND_FOODS:
                AppApi.getRecommendFoods(this,hotelid+"",this);
                break;
        }

    }

    @Override
    public void getViews() {
        mEmptyLayout = (ConstraintLayout) findViewById(R.id.empty_layout);
        mEmptyHintTv = (TextView) findViewById(R.id.tv_hint);
        mBackBtn = (ImageView) findViewById(R.id.iv_left);
        mTitleTv = (TextView) findViewById(R.id.tv_center);
        mProBtn = (TextView) findViewById(R.id.tv_pro);
        mRecommendFoodsRlv = (RecyclerView) findViewById(R.id.rlv_foods);
        mRoomListView = (RecyclerView) findViewById(R.id.rlv_room);
    }

    @Override
    public void setViews() {
        // 初始化标题栏
        initTitleBar();
        // 初始化内容列表
        initContentList();
        // 初始化房间列表
        initRoomList();
    }

    private void initTitleBar() {
        mTitleTv.setText("请选择投屏包间");
        TextPaint tp = mTitleTv.getPaint();
        tp.setFakeBoldText(true);
        mTitleTv.setCompoundDrawablesWithIntrinsicBounds(null,null,getResources().getDrawable(R.drawable.ico_arrow_down),null);
        mTitleTv.setCompoundDrawablePadding(DensityUtil.dip2px(this,10));
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mBackBtn.getLayoutParams();
        layoutParams.setMargins(DensityUtil.dip2px(this,15),0,0,0);

//        RoomInfo bindRoom = mSession.getBindRoom();
//        if(bindRoom!=null&&!TextUtils.isEmpty(bindRoom.getBox_name())) {
//            mTitleTv.setText(bindRoom.getBox_name());
//        }
//        currentRoom = bindRoom;
    }

    private void initContentList() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this,COLUMN_COUNT);
        gridLayoutManager.setOrientation(GridLayoutManager.VERTICAL);
        mRecommendFoodsRlv.setLayoutManager(gridLayoutManager);

        mRecommendAdapter = new RecommendFoodAdapter(this);
        mRecommendFoodsRlv.setAdapter(mRecommendAdapter);

        //添加ItemDecoration，item之间的间隔
        int leftRight = DensityUtil.dip2px(this,15);
        int topBottom = DensityUtil.dip2px(this,15);

        mRecommendFoodsRlv.addItemDecoration(new SpacesItemDecoration(leftRight, topBottom, getResources().getColor(R.color.color_eeeeee)));

        HotelBean hotelBean = mSession.getHotelBean();
        List<RecommendFoodAdvert> recommendFoodAdvertList = new ArrayList<>();
        switch (currentType) {
            case TYPE_RECOMMEND_FOODS:
                initRecoomedList(recommendFoodAdvertList);

                RecommendProHistory recommendListHistory = mSession.getRecommendListHistory();
                if(recommendListHistory!=null) {
                    List<RecommendFoodAdvert> recmmendHistoryList = recommendListHistory.getRecmmendList();
                    if(recmmendHistoryList!=null&&recmmendHistoryList.size()>0) {
//                        if(hotelBean.getHotel_id().equals(hotel_id)) {
                            for(int i = 0;i<recmmendHistoryList.size();i++) {
                                for(int j = 0;j<recommendFoodAdvertList.size();j++) {
                                    RecommendFoodAdvert historyAdvert = recmmendHistoryList.get(i);
                                    RecommendFoodAdvert foodAdvert = recommendFoodAdvertList.get(j);
                                    if(historyAdvert!=null&&foodAdvert!=null) {
                                        if(historyAdvert.getFood_name().equals(foodAdvert.getFood_name())) {
                                            foodAdvert.setSelected(true);
                                        }
                                    }
                                }
                            }
//                                        mRecommendAdapter.notifyDataSetChanged();
//                        }
                    }
                }
                break;
            case TYPE_ADVERT:
                initAdvertList(recommendFoodAdvertList);
                AdvertProHistory advertProHistory = mSession.getAdvertProHistory();
                if(advertProHistory!=null) {
//                    HotelBean hBean = advertProHistory.getHotelBean();
                    List<RecommendFoodAdvert> advertHistoryList = advertProHistory.getAdvertList();
                    if(advertHistoryList!=null&&advertHistoryList.size()>0) {
//                        String hotel_id = hBean.getHotel_id();
//                        if(hotelBean.getHotel_id().equals(hotel_id)) {
                            for(int i = 0;i<advertHistoryList.size();i++) {
                                for(int j = 0;j<recommendFoodAdvertList.size();j++) {
                                    RecommendFoodAdvert historyAdvert = advertHistoryList.get(i);
                                    RecommendFoodAdvert foodAdvert = recommendFoodAdvertList.get(j);
                                    if(historyAdvert!=null&&foodAdvert!=null) {
                                        if(historyAdvert.getName().equals(foodAdvert.getName())) {
                                            foodAdvert.setSelected(true);
                                        }
                                    }
                                }
                            }
//                                        mRecommendAdapter.notifyDataSetChanged();
                        }
//                    }
                }
                break;
        }
        mRecommendAdapter.setData(recommendFoodAdvertList,currentType);

    }

    private void initAdvertList(List<RecommendFoodAdvert> recommendFoodAdvertList) {
        for(int i = 0;i<advertNameList.size();i++) {
            String foodName = advertNameList.get(i);
            int resId = advertResIdList.get(i);
            RecommendFoodAdvert recommendFoodAdvert = new RecommendFoodAdvert();
            recommendFoodAdvert.setName(foodName);
            recommendFoodAdvert.setResId(resId);
            recommendFoodAdvertList.add(recommendFoodAdvert);
        }
    }

    private void initRecoomedList(List<RecommendFoodAdvert> recommendFoodAdvertList) {
        for(int i = 0;i<recommendNameList.size();i++) {
            String foodName = recommendNameList.get(i);
            int resId = recommendResIdList.get(i);
            RecommendFoodAdvert recommendFoodAdvert = new RecommendFoodAdvert();
            recommendFoodAdvert.setFood_name(foodName);
            recommendFoodAdvert.setResId(resId);
            recommendFoodAdvertList.add(recommendFoodAdvert);
        }
    }

    private void initRoomList() {
        //添加ItemDecoration，item之间的间隔
        int leftRight = DensityUtil.dip2px(this,15);
        int topBottom = DensityUtil.dip2px(this,15);
        GridLayoutManager roomLayoutManager = new GridLayoutManager(this,3);
        roomLayoutManager.setOrientation(GridLayoutManager.VERTICAL);
        mRoomListView.setLayoutManager(roomLayoutManager);
        roomListAdapter = new RoomListAdapter(this);
        mRoomListView.setAdapter(roomListAdapter);
        roomList = new ArrayList<>();
        TvBoxSSDPInfo tvBoxSSDPInfo = mSession.getTvBoxSSDPInfo();
        if(tvBoxSSDPInfo!=null&&!TextUtils.isEmpty(tvBoxSSDPInfo.getBoxIp())) {
            RoomInfo roomInfo = new RoomInfo();
            roomInfo.setBox_ip(tvBoxSSDPInfo.getBoxIp());
            roomInfo.setBox_name("演示版电视");
            roomInfo.setBox_mac("FCD5D900B8B6");
            roomList.add(roomInfo);
        }

        mRoomListView.addItemDecoration(new SpacesItemDecoration(leftRight, topBottom, getResources().getColor(R.color.white)));
        if(roomList !=null && roomList.size()>0) {
            roomListAdapter.setData(roomList);
        }
    }

    @Override
    public void setListeners() {
        mProBtn.setOnClickListener(this);
        mBackBtn.setOnClickListener(this);
        mTitleTv.setOnClickListener(this);
        mRecommendAdapter.setOnCheckStateChangeListener(this);
        mRecommendAdapter.setOnSingleProBtnClickListener(this);
        roomListAdapter.setOnRoomItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_left:
                if(isSelectRommState) {
                    hideRommList();
                }else {
                    finish();
                }
                break;
            case R.id.tv_center:
                if(!isSelectRommState) {
                    showRoomList();

                }

                break;
            case R.id.tv_pro:
                currentProType = TYPE_PRO_MULTI;
                startMultiPro();
                break;
        }
    }

    /**多选投屏*/
    private void startMultiPro() {
        // 1.判断是否选择包间
        if(currentRoom == null) {
            initRoomNotSelected();
            return;
        }
        // 2.开始投屏
        String nameList = getSelectedListIds(mRecommendAdapter.getData());
        showLoadingLayout();
        switch (currentType) {
            case TYPE_ADVERT:
                proAdvert(nameList);
                break;
            case TYPE_RECOMMEND_FOODS:
                proRecmmend(nameList,30+"");
                break;
        }

    }

    private void proAdvert(String videos) {
        String url = "http://"+currentRoom.getBox_ip()+":8080";
        AppApi.adverPro(this,url,videos,this);
    }

    private void proRecmmend(String nameList,String time) {
        erroCount = 0;
        String url = "http://"+currentRoom.getBox_ip()+":8080";
        AppApi.recommendPro(this,url,nameList,time,this);
    }

    private String getSelectedListIds(List<RecommendFoodAdvert> data) {
       StringBuilder sb = new StringBuilder();
       for(int i = 0;i<data.size();i++) {
           RecommendFoodAdvert foodAdvert = data.get(i);
           if(i==data.size()-1) {
               if(foodAdvert.isSelected()) {
                   switch (currentType) {
                       case TYPE_RECOMMEND_FOODS:
                           sb.append(foodAdvert.getFood_name());
                           break;
                       case TYPE_ADVERT:
                           sb.append(foodAdvert.getName());
                           break;
                   }
               }

           }else {
               if(foodAdvert.isSelected()) {
                   switch (currentType) {
                       case TYPE_RECOMMEND_FOODS:
                           sb.append(foodAdvert.getFood_name()+",");
                           break;
                       case TYPE_ADVERT:
                           sb.append(foodAdvert.getName()+",");
                           break;
                   }
               }
           }
       }

        return sb.toString();
    }

    private List<RecommendFoodAdvert> getSelectedList(List<RecommendFoodAdvert> data) {
        List<RecommendFoodAdvert> temp = new ArrayList<>();
        for(int i = 0;i<data.size();i++) {
            RecommendFoodAdvert foodAdvert = data.get(i);
            if(foodAdvert.isSelected()) {
                temp.add(foodAdvert);
            }
        }

        return temp;
    }

    private void hideRommList() {

        resetRoomList();
        if(currentRoom!=null) {
            mTitleTv.setText(currentRoom.getBox_name());
        }else {
            mTitleTv.setText("请选择投屏包间");
        }
        mTitleTv.setCompoundDrawablesWithIntrinsicBounds(null,null,getResources().getDrawable(R.drawable.ico_arrow_down),null);
        mBackBtn.setImageResource(R.drawable.back);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.actionsheet_dialog_out);
        mRoomListView.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mRoomListView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        isSelectRommState = false;
    }

    private void showRoomList() {
        mTitleTv.setText("请选择投屏包间");
        mRoomListView.setVisibility(View.VISIBLE);
        mTitleTv.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        mBackBtn.setImageResource(R.drawable.ico_close);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.actionsheet_dialog_in);
        mRoomListView.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mRoomListView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        isSelectRommState  = true;
    }

    @Override
    public void onCheckStateChange() {
        List<RecommendFoodAdvert> data = mRecommendAdapter.getData();
        if (data != null) {
            if (isSomeOneSelected(data)) {
                mProBtn.setEnabled(true);
            } else {
                mProBtn.setEnabled(false);
            }
        }
    }


    @Override
    public void onSingleProBtnClick(RecommendFoodAdvert recommendFoodAdvert) {
        currentProType = TYPE_PRO_SINGLE;
        currentFoodAdvert = recommendFoodAdvert;

        startSinglePro();
    }

    /**
     * 单个投屏
     */
    private void startSinglePro() {
        if (currentRoom == null) {
            initRoomNotSelected();
            return;
        }
        showLoadingLayout();
        TvBoxSSDPInfo tvBoxSSDPInfo = mSession.getTvBoxSSDPInfo();
        if(tvBoxSSDPInfo!=null) {
            currentRoom.setBox_ip(tvBoxSSDPInfo.getBoxIp());
        }

        switch (currentType) {
                case TYPE_RECOMMEND_FOODS:
                    proRecmmend(currentFoodAdvert.getFood_name(),60*2+"");
//                AppApi.recommendPro(this, "", currentRoom.getBox_mac(), 1000 * 60 * 2 + "", currentFoodAdvert.getFood_id(), this);
                    break;
                case TYPE_ADVERT:
                    proAdvert(currentFoodAdvert.getName());
//                AppApi.adverPro(this, "", currentRoom.getBox_mac(), currentFoodAdvert.getId(), this);
                    break;
            }

    }

    private void initRoomNotSelected() {
        ShowMessage.showToast(this, "请选择包间");
//        showRoomList();
    }

    private boolean isSomeOneSelected(List<RecommendFoodAdvert> data) {
        for (RecommendFoodAdvert food : data) {
            if (food.isSelected())
                return true;
        }
        return false;
    }

    @Override
    public void onRoomItemClick(RoomInfo roomInfo) {
        currentRoom = roomInfo;
        hideRommList();
//        switch (currentProType) {
//            case TYPE_PRO_SINGLE:
//                startSinglePro();
//                break;
//            case TYPE_PRO_MULTI:
//                startMultiPro();
//                break;
//        }
    }

    @Override
    public void onSuccess(AppApi.Action method, Object obj) {
        HotelBean hotelBean  = null;
        switch (method) {
            case GET_RECOMMEND_PRO_JSON:
            case GET_ADVERT_PRO_JSON:
                hideLoadingLayout();
                List<RecommendFoodAdvert> data = mRecommendAdapter.getData();
                List<RecommendFoodAdvert> selectedList = getSelectedList(data);
                hotelBean = mSession.getHotelBean();
                if(currentProType == TYPE_PRO_MULTI) {
                    switch (currentType) {
                        case TYPE_ADVERT:
                            AdvertProHistory advertProHistory = new AdvertProHistory();
                            advertProHistory.setAdvertList(selectedList);
                            advertProHistory.setHotelBean(hotelBean);
                            mSession.setAdvertHistory(advertProHistory);
                            break;
                        case TYPE_RECOMMEND_FOODS:
                            RecommendProHistory recommendProHistory = new RecommendProHistory();
                            recommendProHistory.setHotelBean(hotelBean);
                            recommendProHistory.setRecmmendList(selectedList);
                            mSession.setRecommendListHistory(recommendProHistory);
                            break;
                    }
                }

                ShowMessage.showToast(this,"投屏成功！");
                break;
            case GET_ADVERT_JSON:
            case GET_RECOMMEND_FOODS_JSON:
                hideLoadingLayout();
                hotelBean = mSession.getHotelBean();
                if(obj instanceof List) {
                    List<RecommendFoodAdvert> recommendFoodAdvertList = (List<RecommendFoodAdvert>) obj;
                    // 判断如果上次投屏过的要回显已选择

                    switch (currentType) {
                        case TYPE_RECOMMEND_FOODS:
                            RecommendProHistory recommendListHistory = mSession.getRecommendListHistory();
                            if(recommendListHistory!=null) {
                                HotelBean hBean = recommendListHistory.getHotelBean();
                                List<RecommendFoodAdvert> recmmendHistoryList = recommendListHistory.getRecmmendList();
                                if(hBean!=null&&recmmendHistoryList!=null&&recmmendHistoryList.size()>0) {
                                    String hotel_id = hBean.getHotel_id();
                                    if(hotelBean.getHotel_id().equals(hotel_id)) {
                                        for(int i = 0;i<recmmendHistoryList.size();i++) {
                                            for(int j = 0;j<recommendFoodAdvertList.size();j++) {
                                                RecommendFoodAdvert historyAdvert = recmmendHistoryList.get(i);
                                                RecommendFoodAdvert foodAdvert = recommendFoodAdvertList.get(j);
                                                if(historyAdvert!=null&&foodAdvert!=null) {
                                                    if(historyAdvert.getFood_id().equals(foodAdvert.getFood_id())) {
                                                        foodAdvert.setSelected(true);
                                                    }
                                                }
                                            }
                                        }
//                                        mRecommendAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                            break;
                        case TYPE_ADVERT:
                            AdvertProHistory advertProHistory = mSession.getAdvertProHistory();
                            if(advertProHistory!=null) {
                                HotelBean hBean = advertProHistory.getHotelBean();
                                List<RecommendFoodAdvert> advertHistoryList = advertProHistory.getAdvertList();
                                if(hBean!=null&&advertHistoryList!=null&&advertHistoryList.size()>0) {
                                    String hotel_id = hBean.getHotel_id();
                                    if(hotelBean.getHotel_id().equals(hotel_id)) {
                                        for(int i = 0;i<advertHistoryList.size();i++) {
                                            for(int j = 0;j<recommendFoodAdvertList.size();j++) {
                                                RecommendFoodAdvert historyAdvert = advertHistoryList.get(i);
                                                RecommendFoodAdvert foodAdvert = recommendFoodAdvertList.get(j);
                                                if(historyAdvert!=null&&foodAdvert!=null) {
                                                    if(historyAdvert.getId().equals(foodAdvert.getId())) {
                                                        foodAdvert.setSelected(true);
                                                    }
                                                }
                                            }
                                        }
//                                        mRecommendAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                            break;
                    }
                    mRecommendAdapter.setData(recommendFoodAdvertList,currentType);
                }
                break;
        }
    }

    @Override
    public void onError(AppApi.Action method, Object obj) {

        switch (method) {
            case GET_RECOMMEND_PRO_JSON:

                hideLoadingLayout();
                if(obj instanceof ResponseErrorMessage) {
                    ResponseErrorMessage message = (ResponseErrorMessage) obj;
                    int code = message.getCode();
                    String msg = message.getMessage();

                    showToast(msg);
                }else if(obj == AppApi.ERROR_TIMEOUT) {
                    showToast("网络超时，请重试");
                }else if(obj == AppApi.ERROR_NETWORK_FAILED) {
                    showToast("网络已断开，请检查");
                }
                break;
            case GET_ADVERT_PRO_JSON:

                hideLoadingLayout();
                if(obj instanceof ResponseErrorMessage) {
                    ResponseErrorMessage message = (ResponseErrorMessage) obj;
                    int code = message.getCode();
                    String msg = message.getMessage();

                    showToast(msg);
                }else if(obj == AppApi.ERROR_TIMEOUT) {
                    showToast("网络超时，请重试");
                }else if(obj == AppApi.ERROR_NETWORK_FAILED) {
                    showToast("网络已断开，请检查");
                }
                break;
            case GET_ADVERT_JSON:
                hideLoadingLayout();
                if(obj instanceof ResponseErrorMessage) {
                    ResponseErrorMessage message = (ResponseErrorMessage) obj;
                    int code = message.getCode();
                    String msg = message.getMessage();
                    if(code == 60013) {
                        mEmptyLayout.setVisibility(View.VISIBLE);
                        mEmptyHintTv.setText(msg);
                    }else {
                        showToast(msg);
                    }

                }else if(obj == AppApi.ERROR_TIMEOUT) {
                    showToast("网络超时，请重试");
                }else if(obj == AppApi.ERROR_NETWORK_FAILED) {
                    showToast("网络已断开，请检查");
                }
                break;
            case GET_RECOMMEND_FOODS_JSON:
                hideLoadingLayout();
                if(obj instanceof ResponseErrorMessage) {
                    ResponseErrorMessage message = (ResponseErrorMessage) obj;
                    int code = message.getCode();
                    String msg = message.getMessage();
                    if(code == 60007) {
                        mEmptyLayout.setVisibility(View.VISIBLE);
                        mEmptyHintTv.setText(msg);
                    }else {
                        showToast(msg);
                    }

                }else if(obj == AppApi.ERROR_TIMEOUT) {
                    showToast("网络超时，请重试");
                }else if(obj == AppApi.ERROR_NETWORK_FAILED) {
                    showToast("网络已断开，请检查");
                }
                break;
                default:
                    hideLoadingLayout();
//                    super.onError(method,obj);
                    break;
        }
    }


    @Override
    protected void onDestroy() {
        // 清楚房间选择记录
        resetRoomList();
        OkHttpUtils.getInstance().getOkHttpClient().dispatcher().cancelAll();
        super.onDestroy();
    }

    private void resetRoomList() {
        if(roomList!=null) {
            for(RoomInfo info:roomList) {
                info.setSelected(false);
            }
        }
        roomListAdapter.setData(roomList);
    }

    @Override
    public void showLoadingLayout() {
        if(mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog(this);
        }
        mLoadingDialog.show();
    }

    @Override
    public void hideLoadingLayout() {
        if(mLoadingDialog!=null) {
            mLoadingDialog.dismiss();
        }
    }

}
