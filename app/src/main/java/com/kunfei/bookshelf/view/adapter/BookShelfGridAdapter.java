//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.kunfei.bookshelf.view.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.kunfei.bookshelf.DbHelper;
import com.kunfei.bookshelf.R;
import com.kunfei.bookshelf.bean.BookShelfBean;
import com.kunfei.bookshelf.help.BookshelfHelp;
import com.kunfei.bookshelf.help.ItemTouchCallback;
import com.kunfei.bookshelf.utils.theme.ThemeStore;
import com.kunfei.bookshelf.view.adapter.base.OnItemClickListenerTwo;
import com.kunfei.bookshelf.widget.BadgeView;
import com.kunfei.bookshelf.widget.RotateLoading;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class BookShelfGridAdapter extends RecyclerView.Adapter<BookShelfGridAdapter.MyViewHolder> implements BookShelfAdapter {
    private boolean isArrange;
    private List<BookShelfBean> books;
    private OnItemClickListenerTwo itemClickListener;
    private String bookshelfPx;
    private Activity activity;
    private HashSet<String> selectList = new HashSet<>();

    private ItemTouchCallback.OnItemTouchCallbackListener itemTouchCallbackListener = new ItemTouchCallback.OnItemTouchCallbackListener() {
        @Override
        public void onSwiped(int adapterPosition) {

        }

        @Override
        public boolean onMove(int srcPosition, int targetPosition) {
            BookShelfBean shelfBean = books.get(srcPosition);
            books.remove(srcPosition);
            books.add(targetPosition, shelfBean);
            notifyItemMoved(srcPosition, targetPosition);
            int start = srcPosition;
            int end = targetPosition;
            if (start > end) {
                start = targetPosition;
                end = srcPosition;
            }
            notifyItemRangeChanged(start, end - start + 1);
            return true;
        }
    };

    public BookShelfGridAdapter(Activity activity) {
        this.activity = activity;
        books = new ArrayList<>();
    }

    @Override
    public void setArrange(boolean isArrange) {
        selectList.clear();
        this.isArrange = isArrange;
        notifyDataSetChanged();
    }

    @Override
    public void selectAll() {
        if (selectList.size() == books.size()) {
            selectList.clear();
        } else {
            for (BookShelfBean bean : books) {
                selectList.add(bean.getNoteUrl());
            }
        }
        notifyDataSetChanged();
        itemClickListener.onClick(null, 0);
    }

    @Override
    public ItemTouchCallback.OnItemTouchCallbackListener getItemTouchCallbackListener() {
        return itemTouchCallbackListener;
    }

    @Override
    public void refreshBook(String noteUrl) {
        for (int i = 0; i < books.size(); i++) {
            if (Objects.equals(books.get(i).getNoteUrl(), noteUrl)) {
                notifyItemChanged(i);
            }
        }
    }

    @Override
    public int getItemCount() {
        //如果不为0，按正常的流程跑
        return books.size();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookshelf_grid, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int index) {
        BookShelfBean bookShelfBean = books.get(index);
        if (isArrange) {
            if (selectList.contains(bookShelfBean.getNoteUrl())) {
                holder.vwSelect.setBackgroundResource(R.color.ate_button_disabled_light);
            } else {
                holder.vwSelect.setBackgroundColor(Color.TRANSPARENT);
            }
            holder.vwSelect.setVisibility(View.VISIBLE);
            holder.vwSelect.setOnClickListener(v -> {
                if (selectList.contains(bookShelfBean.getNoteUrl())) {
                    selectList.remove(bookShelfBean.getNoteUrl());
                    holder.vwSelect.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    selectList.add(bookShelfBean.getNoteUrl());
                    holder.vwSelect.setBackgroundResource(R.color.ate_button_disabled_light);
                }
                itemClickListener.onClick(v, index);
            });
        } else {
            holder.vwSelect.setVisibility(View.GONE);
        }
        holder.tvName.setText(bookShelfBean.getBookInfoBean().getName());
        holder.tvName.setBackgroundColor(ThemeStore.backgroundColor(activity));
        if (!activity.isFinishing()) {
            if (TextUtils.isEmpty(bookShelfBean.getCustomCoverPath())) {
                Glide.with(activity).load(bookShelfBean.getBookInfoBean().getCoverUrl())
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop()
                        .placeholder(R.drawable.img_cover_default)
                        .into(holder.ivCover);
            } else if (bookShelfBean.getCustomCoverPath().startsWith("http")) {
                Glide.with(activity).load(bookShelfBean.getCustomCoverPath())
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop()
                        .placeholder(R.drawable.img_cover_default)
                        .into(holder.ivCover);
            } else {
                Glide.with(activity).load(new File(bookShelfBean.getCustomCoverPath()))
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop()
                        .placeholder(R.drawable.img_cover_default)
                        .into(holder.ivCover);
            }
        }

        holder.ivCover.setOnClickListener(v -> {
            if (itemClickListener != null)
                itemClickListener.onClick(v, index);
        });
        holder.tvName.setOnClickListener(view -> {
            if (itemClickListener != null) {
                itemClickListener.onLongClick(view, index);
            }
        });
        if (!Objects.equals(bookshelfPx, "2")) {
            holder.ivCover.setOnLongClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onLongClick(v, index);
                }
                return true;
            });
        } else if (bookShelfBean.getSerialNumber() != index) {
            bookShelfBean.setSerialNumber(index);
            new Thread() {
                public void run() {
                    DbHelper.getDaoSession().getBookShelfBeanDao().insertOrReplace(bookShelfBean);
                }
            }.start();
        }
        if (bookShelfBean.isLoading()) {
            holder.bvUnread.setVisibility(View.INVISIBLE);
            holder.rotateLoading.setVisibility(View.VISIBLE);
            holder.rotateLoading.start();
        } else {
            holder.bvUnread.setBadgeCount(bookShelfBean.getUnreadChapterNum());
            holder.bvUnread.setHighlight(bookShelfBean.getHasUpdate());
            holder.rotateLoading.setVisibility(View.INVISIBLE);
            holder.rotateLoading.stop();
        }
    }

    @Override
    public void setItemClickListener(OnItemClickListenerTwo itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @Override
    public synchronized void replaceAll(List<BookShelfBean> newDataS, String bookshelfPx) {
        this.bookshelfPx = bookshelfPx;
        selectList.clear();
        if (null != newDataS && newDataS.size() > 0) {
            BookshelfHelp.order(newDataS, bookshelfPx);
            books = newDataS;
        } else {
            books.clear();
        }
        notifyDataSetChanged();
        if (isArrange) {
            itemClickListener.onClick(null, 0);
        }
    }

    @Override
    public List<BookShelfBean> getBooks() {
        return books;
    }

    @Override
    public HashSet<String> getSelected() {
        return selectList;
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvName;
        BadgeView bvUnread;
        RotateLoading rotateLoading;
        View vwSelect;

        MyViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvName = itemView.findViewById(R.id.tv_name);
            bvUnread = itemView.findViewById(R.id.bv_unread);
            rotateLoading = itemView.findViewById(R.id.rl_loading);
            rotateLoading.setLoadingColor(ThemeStore.accentColor(itemView.getContext()));
            vwSelect = itemView.findViewById(R.id.vw_select);
        }
    }
}
