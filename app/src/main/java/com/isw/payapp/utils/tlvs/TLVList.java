package com.isw.payapp.utils.tlvs;

import androidx.annotation.NonNull;


import com.isw.payapp.utils.BytesUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author GuoJirui.
 * @date 2021/4/29.
 * @desc
 */
public class TLVList {
    private final List<TLVElement> data = new ArrayList<TLVElement>();

    public int size() {
        return data.size();
    }

    public List<TLVElement> data(){
        return data;
    }

    /**
     * Check whether the TLV object corresponding to a TAG is included
     * @param tag tag
     * @return true：included；false：not included
     */
    public boolean contains(String tag) {
        return null != getTLV(tag);
    }


    /**
     * Gets a list of subsets TLVs
     *
     * @param tags tags
     * @return TLVList
     */
    public TLVList getTLVs(String... tags) {
        TLVList list = new TLVList();
        for (String tag : tags) {
            TLVElement data = getTLV(tag);
            if (data != null) {
                list.addTLV(data);
            }
        }
        if (list.size() == 0) {
            return null;
        }
        return list;
    }

    /**
     * Get TLV according to tag
     *
     * @param tag tag
     * @return TLVElement
     */
    public TLVElement getTLV(String tag) {
        for (TLVElement d : data) {
            if (d.getTag().equals(tag)) {
                return d;
            }
        }
        return null;
    }

    /**
     * Get TLV according to index
     *
     * @param index index
     * @return TLVElement
     */
    public TLVElement getTLV(int index) {
        return data.get(index);
    }

    /**
     * add Tlv
     *
     * @param tlv tlv
     */
    public void addTLV(TLVElement tlv) {
        if (tlv.isValid()) {
            data.add(tlv);
        } else {
            throw new IllegalArgumentException("tlv is not valid!");
        }
    }

    /**
     * Reserves specific TLVs according to TAG
     *
     * @param tags tags
     */
    public void retain(String... tags) {
        List<String> tagList = Arrays.asList(tags);
        for (int index = 0; index < data.size(); ) {
            if (!tagList.contains(data.get(index).getTag())) {
                data.remove(index);
            } else {
                index++;
            }
        }
    }

    /**
     * Delete a TLV according to Tag
     *
     * @param tag tag
     */
    public void remove(String tag) {
        for (int i = 0; i < data.size(); ) {
            if (tag.equals(data.get(i).getTag())) {
                data.remove(i);
            } else {
                i++;
            }
        }
    }

    /**
     * Delete multiple TLVs according to Tag
     *
     * @param tags tags
     */
    public void remove(String... tags) {
        List<String> tagList = Arrays.asList(tags);
        for (int i = 0; i < data.size(); ) {
            if (tagList.contains(data.get(i).getTag())) {
                data.remove(i);
            } else {
                i++;
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        if (data.isEmpty()) {
            return super.toString();
        }
        return BytesUtil.bytes2HexString(toBinary());
    }

    public byte[] toBinary() {
        byte[][] allData = new byte[data.size()][];
        for (int i = 0; i < data.size(); i++) {
            allData[i] = data.get(i).getRawData();
        }
        return BytesUtil.merge(allData);
    }
}
