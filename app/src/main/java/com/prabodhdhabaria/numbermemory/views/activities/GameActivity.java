package com.prabodhdhabaria.numbermemory.views.activities;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import com.prabodhdhabaria.numbermemory.R;
import com.prabodhdhabaria.numbermemory.databinding.ActivityGameBinding;
import com.prabodhdhabaria.numbermemory.databinding.LayoutGameItemBinding;
import com.prabodhdhabaria.numbermemory.db.AppDatabase;
import com.prabodhdhabaria.numbermemory.db.objects.LeaderboardItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * logic to play game.
 */
public class GameActivity extends AppCompatActivity {
    private final static String PLAYER_NAME = "name";
    private final static String GRID_SIZE = "grid";
    private static final int SCORE_SUBTRACT_INCORRECT = 1;
    private static final int SCORE_ADD_CORRECT = 2;
    private int mGridSize = 4;

    /**
     * intent to start the game. It takes player name .
     *
     * @param context    context
     * @param playerName name of the player
     * @return intent
     */
    public static Intent getStartIntent(Context context, String playerName) {
        Intent intent = new Intent(context, GameActivity.class);
        intent.putExtra(PLAYER_NAME, playerName);
        return intent;
    }

    /**
     * intent to start the game. It takes player name
     * and the number of columns for the grid.
     *
     * @param context    context
     * @param playerName name of the player
     * @param gridSize   columns of the square grid
     * @return intent
     */
    public static Intent getStartIntent(Context context, String playerName, int gridSize) {
        Intent intent = new Intent(context, GameActivity.class);
        intent.putExtra(PLAYER_NAME, playerName);
        intent.putExtra(GRID_SIZE, gridSize);
        return intent;
    }

    private ActivityGameBinding mBinding;
    private LeaderboardItem mItem = new LeaderboardItem();
    private int mScore = 0;
    private int mPairsIdentified = 0;
    private int mPairs = 0;


    @Override
    public void onBackPressed() {
        //finish game on back press
        endGame();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_game);

        // get player name from intent
        String playerName = getIntent().getStringExtra(PLAYER_NAME);

        // get grid size from intent.
        // set default grid size to 4.
        mGridSize = getIntent().getIntExtra(GRID_SIZE, 4);

        // set default player name to Guest
        if (TextUtils.isEmpty(playerName)) {
            playerName = "Guest";
        }

        // save player name in player data object.
        mItem.setName(playerName);

        initializeViews();
    }

    private void initializeViews() {
        //finish game on back press
        mBinding.close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endGame();
            }
        });

        // set score
        updateScoreOnUI(mItem.getScore());

        // set player name
        mBinding.playerName.setText(mItem.getName());

        // calculate total number of items for grid
        int totalItems = mGridSize * mGridSize;

        // calculate total number of pairs in the grid.
        mPairs = totalItems / 2;

        // prepare data for the grid
        ArrayList<Item> list = new ArrayList<>();
        for (int i = 0; i < mPairs; i++) {
            list.add(new Item(i + 1));
            list.add(new Item(i + 1));
        }

        // randomize data
        Collections.shuffle(list);

        // initialize layout manager and recycler view
        mBinding.list.setLayoutManager(new GridLayoutManager(this, mGridSize, GridLayoutManager.VERTICAL, false));
        mBinding.list.setHasFixedSize(true);
        mBinding.list.setAdapter(new ItemAdapter(list));
        mBinding.list.setItemAnimator(new DefaultItemAnimator());

    }

    /**
     * finish game. save player score and exit.
     */
    private void endGame() {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                // save player score in object
                mItem.setScore(mScore);

                // save player data in database.
                AppDatabase.getDatabase(GameActivity.this).leaderboardDao().insertAll(mItem);
                e.onNext(true);
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        finish();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        finish();
                    }
                });
    }

    private void playClick() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.click);
        mediaPlayer.start();
    }

    class Item {
        Item(int number) {
            this.number = number;
        }

        boolean open = false;
        boolean checked = false;
        int number;
    }

    class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemHolder> {
        private ArrayList<Item> mList;
        int mPositionItem1 = -1;
        int mPositionItem2 = -1;

        ItemAdapter(ArrayList<Item> list) {
            mList = list;
        }


        @NonNull
        @Override
        public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutGameItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.layout_game_item, parent, false);
            return new ItemHolder(binding);
        }

        private int mColorCorrectPairingBackground = 0xFF4CAF50;
        private int mColorCorrectPairingText = 0xffffffff;
        private int mTileAnimationTimeInMilliSeconds = 250;

        @Override
        public void onBindViewHolder(@NonNull final ItemHolder holder, int position) {
            final Item item = mList.get(position);

            // if item is not paired
            if (!item.checked) {
                // set initial state of grid item
                if (item.open) {

                    // animate close
                    flip(holder, item);

                } else {
                    // show closed card
                    holder.mBinding.front.animate().alpha(0).rotationYBy(180).setInterpolator(new LinearInterpolator()).setDuration(100).start();
                    holder.mBinding.back.animate().alpha(1).rotationYBy(-180).setInterpolator(new LinearInterpolator()).setDuration(100).start();
                }

                // if grid item has not been paired then
                // set click listener otherwise remove it.

                holder.mBinding.container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updatePosition(holder, item);
                    }
                });
            } else {

                // update grid item UI if it has been paired
                holder.mBinding.front.setBackgroundColor(mColorCorrectPairingBackground);
                holder.mBinding.front.setVisibility(View.VISIBLE);
                holder.mBinding.back.setVisibility(View.GONE);
                holder.mBinding.item.setTextColor(mColorCorrectPairingText);


                // remove click listener if grid item has been paired
                holder.mBinding.container.setOnClickListener(null);
            }

            // set number on grid item
            holder.mBinding.item.setText(String.valueOf(item.number));
        }

        private void flip(ItemHolder holder, Item item) {

            // close a grid item
            if (item.open) {
//                holder.mBinding.front.setVisibility(View.GONE);
//                holder.mBinding.back.setVisibility(View.VISIBLE);
                holder.mBinding.front.animate().alpha(0).rotationYBy(180).setInterpolator(new LinearInterpolator()).setDuration(mTileAnimationTimeInMilliSeconds).start();
                holder.mBinding.back.animate().alpha(1).rotationYBy(-180).setInterpolator(new LinearInterpolator()).setDuration(mTileAnimationTimeInMilliSeconds).start();
                holder.mBinding.container.animate().translationZ(0).start();
                item.open = false;
            }

            // open a grid item
            else {
//                holder.mBinding.front.setVisibility(View.VISIBLE);
//                holder.mBinding.back.setVisibility(View.GONE);

                // play click sound
                playClick();

                holder.mBinding.front.animate().alpha(1).rotationYBy(180).setInterpolator(new LinearInterpolator()).setDuration(mTileAnimationTimeInMilliSeconds).start();
                holder.mBinding.back.animate().alpha(0).rotationYBy(-180).setInterpolator(new LinearInterpolator()).setDuration(mTileAnimationTimeInMilliSeconds).start();
                holder.mBinding.container.animate().translationZ(12).start();
                item.open = true;
            }
        }

        private void updatePosition(ItemHolder holder, Item item) {
            int position = holder.getAdapterPosition();
            // check if first item is clicked upon
            if (mPositionItem1 == -1) {
                mPositionItem1 = position;

                // flip item
                flip(holder, item);

                // remove click listener
                holder.mBinding.container.setOnClickListener(null);

            }

            // check if same grid item clicked again
            else if (mPositionItem1 == position) {
                // reset item
                notifyItemChanged(position);

                // reset variables
                mPositionItem1 = -1;
                mPositionItem2 = -1;
            }
            // check if it is the second item to be clicked upon
            else if (mPositionItem2 == -1) {
                mPositionItem2 = position;

                // flip item
                flip(holder, item);

                // remove click listener
                holder.mBinding.container.setOnClickListener(null);

                Item item1 = mList.get(mPositionItem1);
                Item item2 = mList.get(mPositionItem2);

                // action for correct pairing
                if (item1.number == item2.number) {

                    // set boolean so that the
                    // grid items are kept open.
                    item1.checked = true;
                    item2.checked = true;

                    // increase score
                    mScore += SCORE_ADD_CORRECT;
                    updateScoreOnUI(mScore);

                    // increment number of pairs identified
                    mPairsIdentified++;
                }

                // action for incorrect pairing
                else {
                    // reduce score
                    mScore -= SCORE_SUBTRACT_INCORRECT;
                    updateScoreOnUI(mScore);
                }


                Completable.complete()
                        .delay(1, TimeUnit.SECONDS)
                        .subscribe(new Action() {
                            @Override
                            public void run() throws Exception {
                                // notify for UI update on open grid items
                                final int int1 = mPositionItem1;
                                final int int2 = mPositionItem2;
                                notifyItemChanged(int1);
                                notifyItemChanged(int2);

                                // reset variables
                                mPositionItem1 = -1;
                                mPositionItem2 = -1;
                            }
                        });


            }

            // end game if all pairs have been identified
            if (mPairsIdentified == mPairs) {
                Completable.complete()
                        .delay(2, TimeUnit.SECONDS)
                        .subscribe(new Action() {
                            @Override
                            public void run() throws Exception {
                                endGame();
                            }
                        });
            }
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        class ItemHolder extends RecyclerView.ViewHolder {
            LayoutGameItemBinding mBinding;

            ItemHolder(LayoutGameItemBinding binding) {
                super(binding.getRoot());
                mBinding = binding;
            }
        }
    }

    private void updateScoreOnUI(int score) {
        mBinding.score.setText(String.valueOf(score));
    }
}
