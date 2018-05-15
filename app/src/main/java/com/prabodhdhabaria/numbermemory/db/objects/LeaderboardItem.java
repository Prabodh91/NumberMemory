package com.prabodhdhabaria.numbermemory.db.objects;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by Prabodh Dhabaria on 14-05-2018.
 */
@Entity(tableName = "leaderboard")
public class LeaderboardItem {
    @PrimaryKey(autoGenerate = true)
    private int mId;

    private String mName;

    private int mScore = 0;

    public LeaderboardItem() {
    }

    public LeaderboardItem(String name, int score) {
        mName = name;
        mScore = score;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public int getScore() {
        return mScore;
    }

    public void setScore(int score) {
        mScore = score;
    }

}
