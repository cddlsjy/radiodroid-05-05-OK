package net.programmierecke.radiodroid2.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import net.programmierecke.radiodroid2.history.TrackHistoryDao;
import net.programmierecke.radiodroid2.history.TrackHistoryEntry;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                TrackHistoryEntry.class
        },
        version = 1,
        exportSchema = false
)
@TypeConverters({DateConverter.class})
public abstract class RadioDroidDatabase extends RoomDatabase {

    private static final int NUMBER_OF_THREADS = 4;
    public static final Executor databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    private static volatile RadioDroidDatabase INSTANCE;

    public static RadioDroidDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (RadioDroidDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    RadioDroidDatabase.class, "radiodroid_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public Executor getQueryExecutor() {
        return databaseWriteExecutor;
    }

    public abstract TrackHistoryDao songHistoryDao();
}
