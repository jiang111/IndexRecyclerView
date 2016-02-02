package com.jiang.android.indexrecyclerview.adapter;

import android.support.v7.widget.RecyclerView;

import com.jiang.android.indexrecyclerview.model.ContactModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


/**
 * Adapter holding a list of animal names of type String. Note that each item must be unique.
 */
public abstract class AnimalsAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    private ArrayList<ContactModel.MembersEntity> items = new ArrayList<>();

    public AnimalsAdapter() {
        setHasStableIds(true);
    }

    public void add(ContactModel.MembersEntity object) {
        items.add(object);
        notifyDataSetChanged();
    }

    public void add(int index, ContactModel.MembersEntity object) {
        items.add(index, object);
        notifyDataSetChanged();
    }

    public void addAll(Collection<ContactModel.MembersEntity> collection) {
        if (collection != null) {
            items.clear();
            items.addAll(collection);
            notifyDataSetChanged();
        }
    }

    public void addAll(ContactModel.MembersEntity... items) {
        addAll(Arrays.asList(items));
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public void remove(ContactModel.MembersEntity object) {
        items.remove(object);
        notifyDataSetChanged();
    }

    public ContactModel.MembersEntity getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
