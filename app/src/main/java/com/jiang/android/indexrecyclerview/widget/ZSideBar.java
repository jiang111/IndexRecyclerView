/**
 * created by jiang, 15/12/11
 * Copyright (c) 2015, jyuesong@gmail.com All Rights Reserved.
 * *                #                                                   #
 * #                       _oo0oo_                     #
 * #                      o8888888o                    #
 * #                      88" . "88                    #
 * #                      (| -_- |)                    #
 * #                      0\  =  /0                    #
 * #                    ___/`---'\___                  #
 * #                  .' \\|     |# '.                 #
 * #                 / \\|||  :  |||# \                #
 * #                / _||||| -:- |||||- \              #
 * #               |   | \\\  -  #/ |   |              #
 * #               | \_|  ''\---/''  |_/ |             #
 * #               \  .-\__  '-'  ___/-. /             #
 * #             ___'. .'  /--.--\  `. .'___           #
 * #          ."" '<  `.___\_<|>_/___.' >' "".         #
 * #         | | :  `- \`.;`\ _ /`;.`/ - ` : | |       #
 * #         \  \ `_.   \_ __\ /__ _/   .-` /  /       #
 * #     =====`-.____`.___ \_____/___.-`___.-'=====    #
 * #                       `=---='                     #
 * #     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   #
 * #                                                   #
 * #               佛祖保佑         永无BUG              #
 * #                                                   #
 */

package com.jiang.android.indexrecyclerview.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.jiang.android.indexrecyclerview.R;


/**
 * @author 陈治谋 (513500085@qq.com)
 * @since 2016-08-16
 */
public class ZSideBar extends View
{
  private SimpleArrayMap<Integer, String> indexMap = new SimpleArrayMap<>();
  private RecyclerView recyclerView;
  private int   choose = -1;// 选中
  private Paint paint  = new Paint();

  private int offsetY;
  private int singleHeight;


  public ZSideBar(Context context)
  {
    this(context, null);
  }

  public ZSideBar(Context context, AttributeSet attrs)
  {
    this(context, attrs, 0);
  }


  public ZSideBar(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
  }

  @Override protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    ((ViewGroup) getParent()).setClipChildren(false);
  }

  public void setupWithRecycler(RecyclerView recyclerView)
  {
    this.recyclerView = recyclerView;
    final RecyclerView.Adapter adapter = recyclerView.getAdapter();
    if (adapter == null) {
      throw new IllegalArgumentException("recyclerView do not set adapter");
    }
    if (!(adapter instanceof IndexAdapter)) {
      throw new IllegalArgumentException("recyclerView adapter not implement IndexAdapter");
    }
    adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
    {
      @Override public void onChanged()
      {
        super.onChanged();
        initIndex(adapter);
      }

      @Override public void onItemRangeChanged(int positionStart, int itemCount)
      {
        super.onItemRangeChanged(positionStart, itemCount);
        initIndex(adapter);
      }

      @Override public void onItemRangeChanged(int positionStart, int itemCount, Object payload)
      {
        super.onItemRangeChanged(positionStart, itemCount, payload);
        initIndex(adapter);
      }

      @Override public void onItemRangeInserted(int positionStart, int itemCount)
      {
        super.onItemRangeInserted(positionStart, itemCount);
        initIndex(adapter);
      }

      @Override public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount)
      {
        super.onItemRangeMoved(fromPosition, toPosition, itemCount);
        initIndex(adapter);
      }

      @Override public void onItemRangeRemoved(int positionStart, int itemCount)
      {
        super.onItemRangeRemoved(positionStart, itemCount);
        initIndex(adapter);
      }
    });
    initIndex(adapter);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event)
  {
    final int action = event.getAction();
    final float y = event.getY();// 点击y坐标
    final int oldChoose = choose;
    // 点击y坐标所占总高度的比例*b数组的长度就等于点击b中的个数.
    final int c = (int) ((y - offsetY) / singleHeight);

    switch (action) {
      case MotionEvent.ACTION_UP:
        setBackgroundColor(0x00000000);
        choose = -1;
        invalidate();
        break;

      default:
        setBackgroundColor(0x66000000);
        if (oldChoose != c) {
          if (c >= 0 && c < indexMap.size()) {
            int position = indexMap.keyAt(c);
            recyclerView.getLayoutManager().scrollToPosition(position);
            choose = c;
            invalidate();
          }
        }

        break;
    }
    return true;
  }

  /**
   * 重写这个方法
   */
  protected void onDraw(Canvas canvas)
  {
    super.onDraw(canvas);

    if (indexMap.isEmpty()) return;

    int height = getHeight(); // 获取对应高度
    int width = getWidth();   // 获取对应宽度

    singleHeight = height / indexMap.size();// 获取每一个字母的高度
    int dp12 = dip2px(12);
    int dp24 = dip2px(24);
    singleHeight = singleHeight > dp24 ? dp24 : singleHeight;
    offsetY = (height - singleHeight * indexMap.size()) / 2;

    for (int i = 0; i < indexMap.size(); i++) {
      paint.setAntiAlias(true);
      paint.setTextSize(dp12);
      paint.setTypeface(Typeface.DEFAULT);
      int colorId = i == choose ? R.color.colorPrimaryDark : R.color.side_bar_text;
      paint.setColor(ContextCompat.getColor(getContext(), colorId));

      float xPos = width / 2 - paint.measureText(indexMap.get(indexMap.keyAt(i))) / 2;
      float yPos = offsetY + singleHeight * (i + 0.5F);
      if (i == choose) {
        // 选中的状态
        paint.setFakeBoldText(true);
        paint.setTextSize(dp24);
        canvas.drawText(indexMap.get(indexMap.keyAt(i)), dip2px(-56), yPos, paint);
        paint.setTextSize(dp12);
      }
      // x坐标等于中间-字符串宽度的一半.
      canvas.drawText(indexMap.get(indexMap.keyAt(i)), xPos, yPos, paint);
    }

  }

  private void initIndex(@NonNull RecyclerView.Adapter adapter)
  {
    indexMap.clear();
    for (int i = 0; i < adapter.getItemCount(); i++) {
      Indexable item = ((IndexAdapter) adapter).getItem(i);
      if (i == 0) {
        indexMap.put(i, item.getIndex());
      } else {
        Indexable preItem = ((IndexAdapter) adapter).getItem(i - 1);
        if (!preItem.getIndex().equals(item.getIndex())) {
          indexMap.put(i, item.getIndex());
        }
      }
    }

    //重绘
    invalidate();
  }

  private static int dip2px(float dpValue)
  {
    final float scale = Resources.getSystem().getDisplayMetrics().density;
    return (int) (dpValue * scale + 0.5f);
  }

}
