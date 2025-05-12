package com.example.summit;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * A Room database class for storing {@link Item} entities.
 * This class provides a singleton instance of the database.
 */
@Database(entities = {Item.class}, version = 1, exportSchema = false)
abstract public class ItemsDB extends RoomDatabase {
    private static final String LOG_TAG = ItemsDB.class.getSimpleName();
    private static final Object LOCK = new Object();
    private static final String DATABASE_NAME = "sumsDB";
    private static ItemsDB sInstance;

    /**
     * Gets the singleton instance of the ItemsDB database.
     * If an instance does not exist, a new one is created.
     *
     * @param context The application context.
     * @return The singleton instance of the ItemsDB.
     */
    public static ItemsDB getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LOCK) {
                Log.d(LOG_TAG, "Creating new database instance");
                sInstance = Room.databaseBuilder(context.getApplicationContext(),
                                ItemsDB.class, ItemsDB.DATABASE_NAME)
                        .build();
            }
        }
        Log.d(LOG_TAG, "Getting the database instance");
        return sInstance;
    }

    /**
     * Abstract method to get the Data Access Object (DAO) for the {@link Item} entity.
     * Room will generate the implementation of this method.
     *
     * @return An instance of the {@link ItemDao}.
     */
    public abstract ItemDao itemDAO();
}