package com.prabodhdhabaria.numbermemory.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.prabodhdhabaria.numbermemory.db.objects.LeaderboardItem;

import java.util.List;

/**
 * Created by Prabodh Dhabaria on 14-05-2018.
 */
@Dao
public interface LeaderboardDao {

    @Query("SELECT * FROM leaderboard order by mScore desc limit :cLimit offset :cSkip")
    List<LeaderboardItem> getSortedDataAll(int cLimit, int cSkip);

    @Insert
    void insertAll(LeaderboardItem... leaderboardItems);

}
