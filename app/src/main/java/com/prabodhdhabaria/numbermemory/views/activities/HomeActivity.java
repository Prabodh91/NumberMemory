package com.prabodhdhabaria.numbermemory.views.activities;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.prabodhdhabaria.numbermemory.R;
import com.prabodhdhabaria.numbermemory.databinding.ActivityHomeBinding;
import com.prabodhdhabaria.numbermemory.databinding.LayoutLeaderboardItemBinding;
import com.prabodhdhabaria.numbermemory.db.AppDatabase;
import com.prabodhdhabaria.numbermemory.db.objects.LeaderboardItem;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class HomeActivity extends AppCompatActivity {
    public static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        return intent;
    }

    ActivityHomeBinding mBinding;
    private int mSkip = 0, mLimit = 10;
    private ItemAdapter mAdapter;
    private RecyclerView.OnScrollListener mOnScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_home);

        initializeViews();
    }

    private void initializeViews() {

        // start game
        mBinding.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(GameActivity.getStartIntent(HomeActivity.this, ""));
            }
        });


        // initialize layout manager and recycler view for leaderboard
        final LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        mBinding.list.setLayoutManager(manager);
        mBinding.list.setHasFixedSize(true);
        mBinding.list.setItemAnimator(new DefaultItemAnimator());

        // initialize scroll listener to load next batch of leaderboard data
        mOnScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    if (manager.findLastVisibleItemPosition() == mSkip - 1) {
                        loadLeaderboard(mLimit, mSkip);
                    }
                }
            }
        };

        mAdapter = new ItemAdapter(new ArrayList<LeaderboardItem>());
        mBinding.list.setAdapter(mAdapter);
    }

    /**
     * load leader board data in batches and show in recycler view
     *
     * @param limit
     * @param skip
     */
    private void loadLeaderboard(final int limit, final int skip) {

        // clear all on scroll listeners from
        // recycler view so that there is no repetition of data.
        mBinding.list.clearOnScrollListeners();

        Observable.create(new ObservableOnSubscribe<List<LeaderboardItem>>() {
            @Override
            public void subscribe(ObservableEmitter<List<LeaderboardItem>> e) throws Exception {
                // get data from database
                List<LeaderboardItem> list = AppDatabase.getDatabase(HomeActivity.this).leaderboardDao().getSortedDataAll(limit, skip);
                if (list != null) {
                    e.onNext(list);
                }
                e.onComplete();

            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<LeaderboardItem>>() {
                    @Override
                    public void accept(List<LeaderboardItem> leaderboardItems) throws Exception {
                        // increment skip
                        mSkip += leaderboardItems.size();

                        // add items to adapter
                        mAdapter.addItems(leaderboardItems);

                        // if mSkip is 0 , then there is no data
                        // in database , so hide recycler view

                        if (mSkip == 0) {
                            mBinding.list.clearOnScrollListeners();
                            mBinding.list.setVisibility(View.GONE);
                        }

                        // if number of items fetched in this batch is
                        // less then limit then it means there is
                        // no more data. so we clear the listeners on recycler view.
                        else if (leaderboardItems.size() < limit) {
                            mBinding.list.clearOnScrollListeners();
                            mBinding.list.setVisibility(View.VISIBLE);

                        }

                        // otherwise add the scroll listener
                        // and make recycler view visible.
                        else {
                            mBinding.list.addOnScrollListener(mOnScrollListener);
                            mBinding.list.setVisibility(View.VISIBLE);

                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {

                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // refresh leaderboard data
        refreshLeaderboardData();
    }

    /**
     * refresh leaderboard data
     */
    private void refreshLeaderboardData() {
        // reset skip to load data from start
        mSkip = 0;

        // clear adapter
        mAdapter.clear();

        // load leaderboard data
        loadLeaderboard(mLimit, mSkip);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // destroy database instance
        AppDatabase.destroyInstance();
    }

    class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemHolder> {
        private List<LeaderboardItem> mList;

        public ItemAdapter(List<LeaderboardItem> list) {
            mList = list;
        }

        @NonNull
        @Override
        public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutLeaderboardItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.layout_leaderboard_item, parent, false);
            return new ItemHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
            LeaderboardItem item = mList.get(position);
            // set name
            holder.mBinding.name.setText(item.getName());

            // set score
            holder.mBinding.score.setText(String.valueOf(item.getScore()));
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        public void addItems(List<LeaderboardItem> leaderboardItems) {
            mList.addAll(leaderboardItems);
            notifyDataSetChanged();
        }

        public void clear() {
            mList.clear();
            notifyDataSetChanged();
        }

        class ItemHolder extends RecyclerView.ViewHolder {
            private LayoutLeaderboardItemBinding mBinding;

            public ItemHolder(LayoutLeaderboardItemBinding binding) {
                super(binding.getRoot());
                mBinding = binding;

            }
        }
    }
}
