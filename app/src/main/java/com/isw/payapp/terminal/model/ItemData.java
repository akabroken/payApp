package com.isw.payapp.terminal.model;

import com.isw.payapp.terminal.accessors.ItemDataAccessor;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;


public class ItemData implements ItemDataAccessor {

    private int imageResource;
    private String title;

    public ItemData(int imageResource,String title){
        this.imageResource = imageResource;
        this.title =title;
    }


    @Override
    public int getImageResource() {
        return imageResource;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
