package com.robot.recycle.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.robot.recycle.entity.BaseViewHolder;

import java.util.HashMap;

/**
 * @author xing.hu
 * @since 2018/9/18, 下午5:11
 */
public class TRecyclerAdapter extends RecyclerView.Adapter {
    private static final  int FOOTER_COUNT = 1;//底部View个数
    private static final int ITEM_TYPE_FOOTER = -2;   //底部刷新view类型
    private static final int ITEM_TYPE_CONTENT = -3;   //底部刷新view类型
    private Context mCtx;

    private RecyclerView.Adapter mAdapter;


    public TRecyclerAdapter(Context context, RecyclerView.Adapter adapter) {
        init(context, adapter);
    }

    private void init(Context ctx, RecyclerView.Adapter adapter){
        mCtx = ctx;
        mAdapter = adapter;
    }




    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return buildHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (isFooter(position)) {
            return;
        }
        initData(holder, position);
    }


    private RecyclerView.ViewHolder buildHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = null;
        switch (viewType) {
            case ITEM_TYPE_FOOTER:
                //holder = new BaseViewHolder(mFooterHolder.getFooterView());
                holder = new BaseViewHolder(new TextView(mCtx));
                break;
            default:
                //ITEM_TYPE_CONTENT
                holder = mAdapter.onCreateViewHolder(parent, viewType);
                break;
        }
        return holder;
    }

    private void initData(RecyclerView.ViewHolder holder, final int position) {
        final int type = getItemViewType(position);
        if (type != ITEM_TYPE_CONTENT) {
            mAdapter.onBindViewHolder(holder, position);
        }

    }


    @Override
    public int getItemViewType(int position) {
        if (isFooter(position)) {
            //底部View
            return ITEM_TYPE_FOOTER;
        } else {
            return mAdapter.getItemViewType(position);
        }
    }

    @Override
    public int getItemCount() {
        return  mAdapter.getItemCount() + FOOTER_COUNT;
    }

   /* public void setFooterHolder(RecyclerFooterHolder mFooterHolder) {
        this.mFooterHolder = mFooterHolder;
    }*/

    /**
     * 判断当前item是否是FooterView
     */
    public boolean isFooter(int position) {
        return position >= (getItemCount() - FOOTER_COUNT);
    }
}
