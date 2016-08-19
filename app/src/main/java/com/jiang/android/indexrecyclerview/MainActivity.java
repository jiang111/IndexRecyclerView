package com.jiang.android.indexrecyclerview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jiang.android.indexrecyclerview.adapter.CommonString;
import com.jiang.android.indexrecyclerview.adapter.ContactAdapter;
import com.jiang.android.indexrecyclerview.model.ContactModel;
import com.jiang.android.indexrecyclerview.pinyin.CharacterParser;
import com.jiang.android.indexrecyclerview.pinyin.PinyinComparator;
import com.jiang.android.indexrecyclerview.widget.DividerDecoration;
import com.jiang.android.indexrecyclerview.widget.SideBar;
import com.jiang.android.indexrecyclerview.widget.TouchableRecyclerView;
import com.jiang.android.indexrecyclerview.widget.ZSideBar;
import com.jiang.android.lib.adapter.expand.StickyRecyclerHeadersDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * RecyclerView实现联系人列表
 */
public class MainActivity extends AppCompatActivity
{

  private SideBar               mSideBar;
  private ZSideBar               mZSideBar;
  private TextView              mUserDialog;
  private TouchableRecyclerView mRecyclerView;

  ContactModel mModel;
  private List<ContactModel.MembersEntity> mMembers = new ArrayList<>();
  private CharacterParser  characterParser;
  private PinyinComparator pinyinComparator;
  private ContactAdapter   mAdapter;
  private List<ContactModel.MembersEntity> mAllLists = new ArrayList<>();
  private int mPermission;


  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    getPermission();
    initView();

  }

  @Override public boolean onCreateOptionsMenu(Menu menu)
  {
    new MenuInflater(this).inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == R.id.menu_zsidebar) {
      mZSideBar.setVisibility(View.VISIBLE);
      mSideBar.setVisibility(View.GONE);
    }else {
      mZSideBar.setVisibility(View.GONE);
      mSideBar.setVisibility(View.VISIBLE);
    }
    return false;
  }

  private void getPermission()
  {
    mPermission = CommonString.PermissionCode.TEACHER;
  }


  private void initView()
  {
    characterParser = CharacterParser.getInstance();
    pinyinComparator = new PinyinComparator();
    mSideBar = (SideBar) findViewById(R.id.contact_sidebar);
    mZSideBar = (ZSideBar) findViewById(R.id.contact_zsidebar);
    mUserDialog = (TextView) findViewById(R.id.contact_dialog);
    mRecyclerView = (TouchableRecyclerView) findViewById(R.id.contact_member);
    mSideBar.setTextView(mUserDialog);


//        fillData();
    getNetData(0);


  }


  public void getNetData(final int type)
  {

    //id 已经被处理过
    String tempData = "{\"groupName\":\"中国\",\"admins\":[{\"id\":\"111221\",\"username\":\"程景瑞\",\"profession\":\"teacher\"},{\"id\":\"bfcd1feb5db2\",\"username\":\"钱黛\",\"profession\":\"teacher\"},{\"id\":\"bfcd1feb5db2\",\"username\":\"许勤颖\",\"profession\":\"teacher\"},{\"id\":\"bfcd1feb5db2\",\"username\":\"孙顺元\",\"profession\":\"teacher\"},{\"id\":\"fcd1feb5db2\",\"username\":\"朱佳\",\"profession\":\"teacher\"},{\"id\":\"bfcd1feb5db2\",\"username\":\"李茂\",\"profession\":\"teacher\"},{\"id\":\"d1feb5db2\",\"username\":\"周莺\",\"profession\":\"teacher\"},{\"id\":\"cd1feb5db2\",\"username\":\"任倩栋\",\"profession\":\"teacher\"},{\"id\":\"d1feb5db2\",\"username\":\"严庆佳\",\"profession\":\"teacher\"}],\"members\":[{\"id\":\"d1feb5db2\",\"username\":\"彭怡1\",\"profession\":\"student\"},{\"id\":\"d1feb5db2\",\"username\":\"方谦\",\"profession\":\"student\"},{\"id\":\"dd2feb5db2\",\"username\":\"谢鸣瑾\",\"profession\":\"student\"},{\"id\":\"dd2478fb5db2\",\"username\":\"孔秋\",\"profession\":\"student\"},{\"id\":\"dd24cd1feb5db2\",\"username\":\"曹莺安\",\"profession\":\"student\"},{\"id\":\"dd2478eb5db2\",\"username\":\"酆有松\",\"profession\":\"student\"},{\"id\":\"dd2478b5db2\",\"username\":\"姜莺岩\",\"profession\":\"student\"},{\"id\":\"dd2eb5db2\",\"username\":\"谢之轮\",\"profession\":\"student\"},{\"id\":\"dd2eb5db2\",\"username\":\"钱固茂\",\"profession\":\"student\"},{\"id\":\"dd2d1feb5db2\",\"username\":\"潘浩\",\"profession\":\"student\"},{\"id\":\"dd24ab5db2\",\"username\":\"花裕彪\",\"profession\":\"student\"},{\"id\":\"dd24ab5db2\",\"username\":\"史厚婉\",\"profession\":\"student\"},{\"id\":\"dd24a00d1feb5db2\",\"username\":\"陶信勤\",\"profession\":\"student\"},{\"id\":\"dd24a5db2\",\"username\":\"水天固\",\"profession\":\"student\"},{\"id\":\"dd24a5db2\",\"username\":\"柳莎婷\",\"profession\":\"student\"},{\"id\":\"dd2d1feb5db2\",\"username\":\"冯茜\",\"profession\":\"student\"},{\"id\":\"dd24a0eb5db2\",\"username\":\"吕言栋\",\"profession\":\"student\"}],\"creater\":{\"id\":\"1\",\"username\":\"褚奇清\",\"profession\":\"teacher\"}}";

    try {
      Gson gson = new GsonBuilder().create();
      mModel = gson.fromJson(tempData, ContactModel.class);
      setUI();
    } catch (Exception e) {

    }


  }

  private void setUI()
  {

    mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener()
    {

      @Override
      public void onTouchingLetterChanged(String s)
      {
        if (mAdapter != null) {
          mAdapter.closeOpenedSwipeItemLayoutWithAnim();
        }
        int position = mAdapter.getPositionForSection(s.charAt(0));
        if (position != -1) {
          mRecyclerView.getLayoutManager().scrollToPosition(position);
        }

      }
    });
    seperateLists(mModel);

    if (mAdapter == null) {
      mAdapter = new ContactAdapter(this, mAllLists, mPermission, mModel.getCreater().getId());
      int orientation = LinearLayoutManager.VERTICAL;
      final LinearLayoutManager layoutManager = new LinearLayoutManager(this, orientation, false);
      mRecyclerView.setLayoutManager(layoutManager);

      mRecyclerView.setAdapter(mAdapter);
      final StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(mAdapter);
      mRecyclerView.addItemDecoration(headersDecor);
      mRecyclerView.addItemDecoration(new DividerDecoration(this));

      //   setTouchHelper();
      mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
      {
        @Override
        public void onChanged()
        {
          headersDecor.invalidateHeaders();
        }
      });
    } else {
      mAdapter.notifyDataSetChanged();
    }
    mZSideBar.setupWithRecycler(mRecyclerView);
  }

  private void seperateLists(ContactModel mModel)
  {
    //群主
    ContactModel.CreaterEntity creatorEntity = mModel.getCreater();
    ContactModel.MembersEntity tempMember = new ContactModel.MembersEntity();
    tempMember.setUsername(creatorEntity.getUsername());
    tempMember.setId(creatorEntity.getId());
    tempMember.setProfession(creatorEntity.getProfession());
    tempMember.setSortLetters("$");

    mAllLists.add(tempMember);


    //管理员

    if (mModel.getAdmins() != null && mModel.getAdmins().size() > 0) {
      for (ContactModel.AdminsEntity e : mModel.getAdmins()) {
        ContactModel.MembersEntity eMember = new ContactModel.MembersEntity();
        eMember.setSortLetters("%");
        eMember.setProfession(e.getProfession());
        eMember.setUsername(e.getUsername());
        eMember.setId(e.getId());
        mAllLists.add(eMember);
      }
    }
    //members;
    if (mModel.getMembers() != null && mModel.getMembers().size() > 0) {
      for (int i = 0; i < mModel.getMembers().size(); i++) {
        ContactModel.MembersEntity entity = new ContactModel.MembersEntity();
        entity.setId(mModel.getMembers().get(i).getId());
        entity.setUsername(mModel.getMembers().get(i).getUsername());
        entity.setProfession(mModel.getMembers().get(i).getProfession());
        String pinyin = characterParser.getSelling(mModel.getMembers().get(i).getUsername());
        String sortString = pinyin.substring(0, 1).toUpperCase();

        if (sortString.matches("[A-Z]")) {
          entity.setSortLetters(sortString.toUpperCase());
        } else {
          entity.setSortLetters("#");
        }
        mMembers.add(entity);
      }
      Collections.sort(mMembers, pinyinComparator);
      mAllLists.addAll(mMembers);
    }


  }


  public void deleteUser(final int position)
  {
    mAdapter.remove(mAdapter.getItem(position));
    showToast("删除成功");

  }

  public void showToast(String value)
  {
    Toast.makeText(this, value, Toast.LENGTH_SHORT).show();

  }


}
