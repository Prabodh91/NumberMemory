package com.prabodhdhabaria.numbermemory.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.prabodhdhabaria.numbermemory.db.dao.LeaderboardDao;
import com.prabodhdhabaria.numbermemory.db.objects.LeaderboardItem;

/**
 * Created by Prabodh Dhabaria on 14-05-2018.
 */
@Database(entities = {LeaderboardItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract LeaderboardDao leaderboardDao();

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, AppDatabase.class, "userdatabase")
                    .allowMainThreadQueries()
                    .build();
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
