package com.robot.recycler.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.robot.recycler.R;
import com.robot.recycler.adapter.TRecyclerAdapter;
import com.robot.recycler.listener.IAnimListener;
import com.robot.recycler.listener.IPullRefresh;
import com.robot.recycler.listener.IPushRefresh;

/**
 * @author xing.hu
 * @since 2018/9/13, 下午7:43
 * TRecyclerView
 *
 * pull down to refresh data, slide up to load data and the position of loading animation fixed
 *
 * the composition of TRecyclerView:
 *
 * 1)  TRecyclerView is a viewGroup-frameLayout, include  the header view and  recyclerView;
 *
 * 2)  The function of header view is showing loading animation and tips animation;
 *
 * 3)  The recycleView has a RecyclerView.Adapter: TRecyclerAdapter, the types of View in TRecyclerAdapter is
 *
 *   the footer view and normal view. RecyclerView loads normal view by the RecyclerView.Adapter which needs
 *
 *   to be implemented.
 *
 *   
 *
 *  the implementation of TRecyclerView:
 *
 *  1) When the TRecycleView is pulled over a certain distance, after releasing the hand, start to refresh the data.
 *
 *  2) When the RecycleView slides up to the last element(Footer View) is visible, start to load data.
 *
 *
 */
public class TRecyclerView extends FrameLayout{
    private int mTouchSlop;

    // whether the TRecyclerView be dragged
    private boolean mIsDrag = false;

    private HeaderHolder mHeaderHolder;


    //private float mInitX = -1f;


    private float mInitY = -1f;

    private RecyclerView mRecyclerView;

    //the header view
    private RelativeLayout mHeaderContainer;

    //TRecyclerAdapter include the footer view and normal view
    private TRecyclerAdapter mTAdapter;

    private LinearLayoutManager linearLayoutManager;

    //pull-down refresh data callback
    private IPullRefresh mPullRefresh;

    //slide-up loading data callback
    private IPushRefresh mPushRefresh;

    //the height of header view
    private int mHeaderHeight = -1;
    //the height of tip view
    private int mTipHeight = -1;

    private boolean mRefresh = false;

    private boolean mLoadMore = false;


    private Context mCtx;


    public TRecyclerView(Context context) {
        super(context);
        init(context);
    }

    public TRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);

    }

    public TRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);

    }

    public TRecyclerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
         //if the recycleView can scroll, then the TRecyclerView doesn't intercept the event.
        if(isUnIntercept()){
            return  false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsDrag = false;
                mInitY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float y = ev.getY();
                //if the distance of moving is over the touchSlop, then The TRecyclerView is dragged.
                if(y-mInitY >= mTouchSlop && !mIsDrag){
                    mIsDrag = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsDrag = false;
                break;
            default:
                break;
        }

        return mIsDrag;
    }




    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(isUnIntercept()){
            return false;
        }

        float dist = 0f;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsDrag = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float y = event.getY();
                dist = (y - mInitY)* TRecycleViewConst.PULL_DRAG_RATE;
                if(dist > 0){
                    mRecyclerView.setTranslationY(getY() + dist);
                }
                if(mIsDrag){
                    //the distance of pull can trigger off refresh
                    if(mPullRefresh != null){
                        mPullRefresh.pullRefreshEnable(dist >= mHeaderHeight);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dist = (event.getY() - mInitY) * TRecycleViewConst.PULL_DRAG_RATE;
                if(mIsDrag){
                    //if the distance of moving is over the header height ,
                    // then show the anim which moves to header position, else show the anim which moves to start position.
                     if(dist >= mHeaderHeight){
                         animToHeader();
                     }else{
                         animToStart();
                     }
                }
                mIsDrag = false;
                break;
        }
        return true;
    }

    private void init(Context ctx) {
        mCtx = ctx;
        mTouchSlop = ViewConfiguration.get(mCtx).getScaledTouchSlop();

        initView();
    }

    private void initView() {
        createHeaderView();
        addTargetView();
        linearLayoutManager = new LinearLayoutManager(mCtx);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setVerticalScrollBarEnabled(true);
        initListener();

    }

    private void initListener(){
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                //when the recycleView has scrolled to the bottom position and the state of RecycleView is IDLE, start to load data.
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (targetInBottom()) {
                      if(mPushRefresh != null){
                          mLoadMore = true;
                          mPushRefresh.loadMore();
                      }
                    }
                }

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    //create header view
    private void createHeaderView() {
        mHeaderContainer = new RelativeLayout(mCtx);
        mHeaderHeight = (int)mCtx.getResources().getDimension(R.dimen.header_height);
        mTipHeight =  (int)mCtx.getResources().getDimension(R.dimen.header_tip_height);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mHeaderHeight);
        params.gravity = Gravity.TOP;
        addView(mHeaderContainer, params);
        setHeaderView();
    }


    private  void addTargetView(){
        mRecyclerView = new RecyclerView(mCtx);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mRecyclerView, params);
    }


    private void setHeaderView(){
        mHeaderHolder = new HeaderHolder(mCtx);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, mHeaderHeight);
        mHeaderContainer.addView(mHeaderHolder.getHeaderView(), params);
        mHeaderHolder.setAnimListener(mAnimListener);

    }


    /*public void setHeaderView(View headerView) {


    }*/



    public void setAdapter(RecyclerView.Adapter adapter){
        adapter.registerAdapterDataObserver(mDataObserver);
        mTAdapter = new TRecyclerAdapter(mCtx, adapter);
        mRecyclerView.setAdapter(mTAdapter);

    }


    public void setRefresh(boolean refresh){
        mRefresh = refresh;
        if(!mRefresh){
            animToStart();
        }
    }


   public void setLoadMore(boolean loadMore){
        mLoadMore = loadMore;
   }


    //the anim which moves to start position
    private void animToStart(){
        ObjectAnimator animator = ObjectAnimator.ofFloat(mRecyclerView,"translationY",  0);
        animator.setDuration(AnimDurConst.ANIM_TO_HEADER_DUR);
        animator.start();
    }

    //the anim which moves to tip position
    private void animToTip(){
        ObjectAnimator animator = ObjectAnimator.ofFloat(mRecyclerView,"translationY",  mTipHeight);
        animator.setDuration(AnimDurConst.ANIM_TO_TIP_DUR);
        animator.start();
    }


    //the anim which moves to header position,w hen the anim is end, start to refresh data
    private void animToHeader(){
        ObjectAnimator animator = ObjectAnimator.ofFloat(mRecyclerView,"translationY",  mHeaderHeight);
        animator.addListener(mToHeaderListener);
        animator.setDuration(AnimDurConst.ANIM_TO_HEADER_DUR);
        animator.start();

    }


    private Animator.AnimatorListener mToHeaderListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            //when the anim of move to header is end, start to refresh data
            if(mPullRefresh != null){
                mRefresh = true;
                mPullRefresh.pullRefresh();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };


    public void setPullRefresh(IPullRefresh mPullRefresh) {
        this.mPullRefresh = mPullRefresh;
    }

    public void setPushRefresh(IPushRefresh mPushRefresh) {
        this.mPushRefresh = mPushRefresh;
    }


    private static class AnimDurConst {
        //the duration of anim to header  position
        private static final  int ANIM_TO_HEADER_DUR = 600;
        //the duration of anim to start position
        private static final  int ANIM_TO_START_DUR = 600;
        //the duration of anim to tip position
        private static final  int ANIM_TO_TIP_DUR = 300;
    }

    private static class TRecycleViewConst{
        private static final float PULL_DRAG_RATE = .618f;
        //the height of the Header
        //private final static int HEADER_CONTAINER_HEIGHT = 50;


    }


    //Set up an observer， when the normal adapter starts to notifyDataChanged，
    // the real adapter-TRecyclerAdapter starts to notifyDataChanged
    private RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            mTAdapter.notifyDataSetChanged();
        }


        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            mTAdapter.notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            mTAdapter.notifyItemRangeChanged(positionStart , itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mTAdapter.notifyItemRangeInserted(positionStart , itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mTAdapter.notifyItemRangeRemoved(positionStart , itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mTAdapter.notifyItemMoved(fromPosition, toPosition );
        }
    };

    //if recycleView can scroll, TRecycleView doesn't intercept the event
    private boolean isUnIntercept(){
        return  !targetInTop();

    }

    //the recycleView has scrolled to the top position
    private boolean targetInTop(){
        return  !mRecyclerView.canScrollVertically(-1) ;

    }

    //the recycleView has scrolled to the bottom position
    private boolean targetInBottom() {
        if (targetInTop()) {
            return false;
        }
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        int count = mRecyclerView.getAdapter().getItemCount();
        if (layoutManager instanceof LinearLayoutManager && count > 0) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            if (linearLayoutManager.findLastVisibleItemPosition() == count - 1) {
                return true;
            }
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
            int[] lastItems = new int[2];
            staggeredGridLayoutManager
                    .findLastCompletelyVisibleItemPositions(lastItems);
            int lastItem = Math.max(lastItems[0], lastItems[1]);
            if (lastItem == count - 1) {
                return true;
            }
        }
        return false;
    }


    public HeaderHolder getHeaderHolder(){
        return mHeaderHolder;
    }
    public FooterHolder getFooterHolder(){
        return mTAdapter.getFooterHolder();
    }

    private IAnimListener mAnimListener = new IAnimListener() {
        @Override
        public void hideAnimEnd() {
            if(mPullRefresh != null){
                mPullRefresh.pullRefreshEnd();
            }
        }
    };

    public void showRefreshTip(String text){
        mHeaderHolder.showRefreshTips(text);
        animToTip();
    }

}