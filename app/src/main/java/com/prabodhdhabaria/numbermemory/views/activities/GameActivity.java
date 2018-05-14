package com.prabodhdhabaria.numbermemory.views.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.prabodhdhabaria.numbermemory.R;
import com.prabodhdhabaria.numbermemory.databinding.ActivityGameBinding;
import com.prabodhdhabaria.numbermemory.db.AppDatabase;
import com.prabodhdhabaria.numbermemory.db.objects.LeaderboardItem;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class GameActivity extends AppCompatActivity {
    private final static String PLAYER_NAME = "name";

    public static Intent getStartIntent(Context context, String playerName) {
        Intent intent = new Intent(context, GameActivity.class);
        intent.putExtra(PLAYER_NAME, playerName);
        return intent;
    }

    private ActivityGameBinding mBinding;
    private LeaderboardItem mItem = new LeaderboardItem();
    private int mScore = 0;
    private ProgressDialog mDialog;

    @Override
    public void onBackPressed() {
        endGame();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_game);

        String playerName = getIntent().getStringExtra(PLAYER_NAME);

        if (TextUtils.isEmpty(playerName)) {
            playerName = "Guest";
        }

        mItem.setName(playerName);
        initializeViews();
        setPlayerName();
        startGame();

    }

    private void startGame() {

    }

    private void setPlayerName() {

    }

    private void initializeViews() {
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Saving Data");

    }

    private void endGame() {
        mDialog.show();
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                mItem.setScore(mScore);
                AppDatabase.getDatabase(GameActivity.this).leaderboardDao().insertAll(mItem);
                e.onNext(true);
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            mDialog.dismiss();
                        }

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        mDialog.dismiss();
                        finish();
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        finish();
                    }
                });
    }

}
