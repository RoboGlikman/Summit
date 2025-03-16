package com.example.summit;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ItemDao {
    @Query("SELECT * FROM Item")
    public List<Item> getItems();

    @Query("Select * FROM Item WHERE id = :id")
    public Item getItem(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void addItem(Item item);

    @Delete
    public void deleteItem(Item item);

    @Query("UPDATE Item SET summaryName = :name WHERE id = :id")
    public void updateItemName(int id, String name); //only update name
}
