package com.example.summit;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO) for the {@link Item} entity.
 * This interface defines methods for interacting with the Item table in the database.
 */
@Dao
public interface ItemDao {
    /**
     * Retrieves all items from the Item table.
     *
     * @return A list of all {@link Item} objects in the database.
     */
    @Query("SELECT * FROM Item")
    public List<Item> getItems();

    /**
     * Retrieves a specific item from the Item table based on its ID.
     *
     * @param id The unique identifier of the item to retrieve.
     * @return The {@link Item} object with the given ID, or null if no such item exists.
     */
    @Query("Select * FROM Item WHERE id = :id")
    public Item getItem(int id);

    /**
     * Inserts a new item into the Item table. If an item with the same primary key already exists, it will be replaced.
     *
     * @param item The {@link Item} object to be inserted or updated.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void addItem(Item item);

    /**
     * Deletes a specific item from the Item table.
     *
     * @param item The {@link Item} object to be deleted.
     */
    @Delete
    public void deleteItem(Item item);

    /**
     * Updates the summary name of a specific item in the Item table.
     *
     * @param id   The unique identifier of the item to update.
     * @param name The new summary name for the item.
     */
    @Query("UPDATE Item SET summaryName = :name WHERE id = :id")
    public void updateItemName(int id, String name); //only update name
}