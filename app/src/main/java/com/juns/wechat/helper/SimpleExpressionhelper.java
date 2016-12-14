package com.juns.wechat.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.juns.wechat.R;
import com.juns.wechat.activity.CallVoiceBaseActivity;
import com.juns.wechat.activity.ChatActivity;
import com.juns.wechat.activity.DynamicPublishActivity;
import com.juns.wechat.activity.SendLocationActivity;
import com.juns.wechat.chat.adpter.ExpressionAdapter;
import com.juns.wechat.chat.adpter.ExpressionPagerAdapter;
import com.juns.wechat.chat.utils.SmileUtils;
import com.juns.wechat.chat.widght.ExpandGridView;
import com.juns.wechat.chat.widght.PasteEditText;
import com.juns.wechat.util.BitmapUtil;
import com.juns.wechat.util.LogUtil;
import com.juns.wechat.util.PhotoUtil;
import com.juns.wechat.util.ThreadPoolUtil;
import com.juns.wechat.util.ToastUtil;
import com.juns.wechat.view.AudioRecordButton;
import com.juns.wechat.xmpp.util.SendMessage;
import com.style.album.AlbumActivity;
import com.style.constant.FileDirectory;
import com.style.constant.Skip;
import com.style.utils.CommonUtil;
import com.style.view.CirclePageIndicator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;


/**
 * Created by 王者 on 2016/8/7.
 */
public class SimpleExpressionhelper {
    private static final String TAG = "SimpleExpressionhelper";
    private static final int EMOTICONS_COUNT = 59;
    private static final String EMOTION_NAME_DELETE = "f_emotion_del_normal";
    EditText etContent;
    View layoutRoot;
    CheckBox ivSmile;
    LinearLayout rlBottomSmile;
    LinearLayout layoutFace;
    ViewPager facePager;
    CirclePageIndicator indicator;

    private AppCompatActivity mActivity;
    Handler mHandler = new Handler();
    //屏幕高度
    private int screenHeight = 0;
    //软件盘弹起后所占高度阀值
    private int keyHeight = 0;
    private List<String> emoticonsFileNames;


    public SimpleExpressionhelper(AppCompatActivity mActivity, EditText etContent) {
        this.mActivity = mActivity;
        this.etContent = etContent;
        //不能是DecorView，DecorView不能监听layout变化
        layoutRoot = mActivity.findViewById(R.id.ll_parent);//mActivity.getWindow().getDecorView();
        rlBottomSmile = (LinearLayout) layoutRoot.findViewById(R.id.rl_bottom_smile);
        ivSmile = (CheckBox) layoutRoot.findViewById(R.id.iv_smile);
        layoutFace = (LinearLayout) layoutRoot.findViewById(R.id.layout_face);
        facePager = (ViewPager) layoutRoot.findViewById(R.id.face_pager);
        indicator = (CirclePageIndicator) layoutRoot.findViewById(R.id.indicator);

        //监听软键盘显示状态
        //获取屏幕高度
        screenHeight = mActivity.getWindowManager().getDefaultDisplay().getHeight();
        //阀值设置为屏幕高度的1/3
        keyHeight = screenHeight / 3;
        //添加layout大小发生改变监听器
        layoutRoot.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                //现在认为只要控件将Activity向上推的高度超过了1/3屏幕高，就认为软键盘弹起
                if (oldBottom != 0 && bottom != 0 && (oldBottom - bottom > keyHeight)) {
                    Log.e(TAG, "监听到软键盘弹起");
                   /* new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            faceLl.setVisibility(View.GONE);
                        }
                    }, 100);*//*
                  /*  new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (rlBottomSmile.getVisibility() == View.GONE)
                                rlBottomSmile.setVisibility(View.VISIBLE);
                        }
                    }, 200);*/
                    rlBottomSmile.setVisibility(View.VISIBLE);

                } else if (oldBottom != 0 && bottom != 0 && (bottom - oldBottom > keyHeight)) {
                    Log.e(TAG, "监听到软件盘关闭");
                    //如果是切换到表情面板而隐藏流量输入法，需要延迟判断表情面板是否显示，如果表情面板是关闭的，操作栏也关闭
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //如果表情面板是关闭的，操作栏也关闭
                            if (layoutFace.getVisibility() == View.GONE)
                                rlBottomSmile.setVisibility(View.GONE);
                        }
                    }, 200);
                }
            }
        });
        ivSmile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutFace.getVisibility() == View.GONE) {
                    //隐藏输入法，打开表情面板
                    hideSoftMouse();
                    //延迟显示，先让输入法显示
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            layoutFace.setVisibility(View.VISIBLE);
                        }
                    }, 100);
                } else {
                    //隐藏表情面板，打开输入法
                    layoutFace.setVisibility(View.GONE);
                    toggleSoftInput();
                }
            }
        });
        this.etContent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ivSmile.setChecked(false);//还原表情状态
                layoutFace.setVisibility(View.GONE);
                return false;
            }
        });
        this.etContent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent arg1) {
                // 这句话说的意思告诉父View我自己的事件我自己处理
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
    }

    public void onCreate() {
        initFace();

    }

    private void initFace() {
        emoticonsFileNames = new ArrayList<>();
        for (int x = 0; x <= EMOTICONS_COUNT; x++) {
            String filename = "f_static_0" + x;
            emoticonsFileNames.add(filename);
        }

        List<View> views = new ArrayList<>();
        View gv1 = getGridChildView(1);
        View gv2 = getGridChildView(2);
        View gv3 = getGridChildView(3);
        views.add(gv1);
        views.add(gv2);
        views.add(gv3);

        facePager.setAdapter(new ExpressionPagerAdapter(views));
        indicator.setViewPager(facePager);

    }

    /**
     * 获取表情的gridview的子view
     *
     * @param i
     * @return
     */
    private View getGridChildView(int i) {
        View view = View.inflate(mActivity, R.layout.expression_gridview, null);
        ExpandGridView gv = (ExpandGridView) view.findViewById(R.id.gridview);
        List<String> list = new ArrayList<>();
        if (i == 1) {
            List<String> list1 = emoticonsFileNames.subList(0, 20);
            list.addAll(list1);
        } else if (i == 2) {
            list.addAll(emoticonsFileNames.subList(20, 40));
        } else if (i == 3) {
            list.addAll(emoticonsFileNames.subList(40, emoticonsFileNames.size()));
        }
        list.add(EMOTION_NAME_DELETE);
        final ExpressionAdapter expressionAdapter = new ExpressionAdapter(mActivity, 1, list);
        gv.setAdapter(expressionAdapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                String filename = expressionAdapter.getItem(position);

                if (filename != EMOTION_NAME_DELETE) { // 不是删除键，显示表情
                    String fieldValue = SmileUtils.getFieldValue(filename);
                    CharSequence sequence = SmileUtils.getSmiledText(mActivity, fieldValue);
                    int index = etContent.getSelectionStart();
                    Editable edit = etContent.getEditableText();//获取EditText的文字
                    edit.insert(index, sequence);//光标所在位置插入文字
                } else { // 删除文字或者表情
                    if (!TextUtils.isEmpty(etContent.getText())) {

                        int selectionStart = etContent.getSelectionStart();// 获取光标的位置
                        if (selectionStart > 0) {
                            String body = etContent.getText()
                                    .toString();
                            String tempStr = body.substring(0,
                                    selectionStart);
                            int i = tempStr.lastIndexOf("[");// 获取最后一个表情的位置
                            if (i != -1) {
                                CharSequence cs = tempStr.substring(i,
                                        selectionStart);
                                if (SmileUtils.containsKey(cs.toString()))
                                    etContent.getEditableText()
                                            .delete(i, selectionStart);
                                else
                                    etContent.getEditableText()
                                            .delete(selectionStart - 1,
                                                    selectionStart);
                            } else {
                                etContent.getEditableText()
                                        .delete(selectionStart - 1,
                                                selectionStart);
                            }
                        }
                    }

                }

            }
        });
        return view;
    }

    //隐藏软键盘
    public void hideSoftMouse() {
        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mActivity.getCurrentFocus() != null)
            imm.hideSoftInputFromWindow(mActivity.getCurrentFocus().getWindowToken(), 0);
    }

    private void toggleSoftInput() {
        CommonUtil.toggleSoftInput(mActivity, etContent);
    }
}